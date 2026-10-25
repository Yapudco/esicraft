package fr.esicraft.fun;

import fr.esicraft.EsicraftPlugin;
import fr.esicraft.guild.Guild;
import fr.esicraft.util.ChatUtil;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import org.bukkit.Bukkit;

/**
 * Classement de saison (mensuel) et mémorial des conquêtes.
 * Score d'une guilde = territoire + Âge + richesse + raids repoussés + conquêtes + membres.
 */
public class SeasonManager {
   private static final ZoneId ZONE = ZoneId.of("Europe/Paris");
   private static final double CHAMPION_BANK_REWARD = 5000.0;
   private static final double CHAMPION_MEMBER_REWARD = 500.0;
   private static final DateTimeFormatter DATE_FORMAT = DateTimeFormatter.ofPattern("dd/MM HH:mm").withZone(ZONE);

   public record Entry(Guild guild, long score) {
   }

   private final EsicraftPlugin plugin;
   private final Map<String, int[]> stats = new ConcurrentHashMap<>();
   private volatile String championGuildId;

   public SeasonManager(EsicraftPlugin plugin) {
      this.plugin = plugin;
   }

   /** Charge les statistiques puis vérifie le changement de saison (appelé après l'ouverture de la base). */
   public void init() {
      for (Object[] row : this.plugin.getDataManager().getStorage().select("SELECT guild_id, repelled, conquests FROM guild_stats;", 3)) {
         this.stats.put((String)row[0], new int[]{((Number)row[1]).intValue(), ((Number)row[2]).intValue()});
      }

      List<Object[]> champion = this.plugin.getDataManager().getStorage().select("SELECT v FROM kv WHERE k = 'season_champion';", 1);
      if (!champion.isEmpty()) {
         this.championGuildId = (String)champion.get(0)[0];
      }

      this.checkSeason();
      // Vérification toutes les 10 minutes (changement de mois).
      Bukkit.getScheduler().runTaskTimer(this.plugin, this::checkSeason, 12000L, 12000L);
   }

   public static String currentSeason() {
      return YearMonth.now(ZONE).toString();
   }

   public static long daysLeft() {
      LocalDate today = LocalDate.now(ZONE);
      return YearMonth.from(today).atEndOfMonth().toEpochDay() - today.toEpochDay() + 1;
   }

   public boolean isChampion(String guildId) {
      return guildId != null && guildId.equals(this.championGuildId);
   }

   public int getRepelled(String guildId) {
      return this.statsOf(guildId)[0];
   }

   public int getConquests(String guildId) {
      return this.statsOf(guildId)[1];
   }

   private int[] statsOf(String guildId) {
      return this.stats.computeIfAbsent(guildId, k -> new int[2]);
   }

   private void saveStats(String guildId) {
      int[] s = this.statsOf(guildId);
      int repelled = s[0];
      int conquests = s[1];
      Bukkit.getScheduler().runTaskAsynchronously(this.plugin, () -> this.plugin.getDataManager().getStorage().exec(
         "INSERT INTO guild_stats (guild_id, repelled, conquests) VALUES (?, ?, ?) "
            + "ON CONFLICT(guild_id) DO UPDATE SET repelled = excluded.repelled, conquests = excluded.conquests;",
         guildId, repelled, conquests));
   }

   public void recordRepelled(Guild guild) {
      if (guild != null) {
         this.statsOf(guild.getId())[0]++;
         this.saveStats(guild.getId());
         this.addHistory("🛡 " + guild.getName() + " a repoussé une invasion de monstres.");
      }
   }

   public void recordConquest(Guild winner, Guild loser) {
      if (winner != null) {
         this.statsOf(winner.getId())[1]++;
         this.saveStats(winner.getId());
         this.addHistory("⚔ " + winner.getName() + " a détruit le Nexus de " + (loser != null ? loser.getName() : "?") + " et annexé ses terres.");
      }
   }

   public void addHistory(String text) {
      long now = System.currentTimeMillis();
      if (Bukkit.getServer() != null && this.plugin.isEnabled()) {
         Bukkit.getScheduler().runTaskAsynchronously(this.plugin, () -> this.plugin.getDataManager().getStorage().exec(
            "INSERT INTO history (ts, text) VALUES (?, ?);", now, text));
      } else {
         this.plugin.getDataManager().getStorage().exec("INSERT INTO history (ts, text) VALUES (?, ?);", now, text);
      }
   }

   public List<String> getHistory(int limit) {
      List<String> lines = new ArrayList<>();
      for (Object[] row : this.plugin.getDataManager().getStorage().select("SELECT ts, text FROM history ORDER BY id DESC LIMIT ?;", 2, limit)) {
         lines.add(DATE_FORMAT.format(java.time.Instant.ofEpochMilli(((Number)row[0]).longValue())) + " &8» &f" + row[1]);
      }

      return lines;
   }

   public long score(Guild guild) {
      int[] s = this.statsOf(guild.getId());
      double score = guild.getClaimedChunks().size() * 5.0
         + guild.getCurrentAge().getLevel() * 200.0
         + guild.getBankBalance() / 50.0
         + s[0] * 40.0
         + s[1] * 150.0
         + guild.getMembers().size() * 10.0
         + (guild.hasNexus() ? 50.0 : 0.0);
      return Math.round(score);
   }

   public List<Entry> ranking() {
      List<Entry> list = new ArrayList<>();
      for (Guild guild : this.plugin.getGuildManager().getAllGuilds()) {
         list.add(new Entry(guild, this.score(guild)));
      }

      list.sort(Comparator.comparingLong(Entry::score).reversed());
      return list;
   }

   private void checkSeason() {
      String current = currentSeason();
      List<Object[]> rows = this.plugin.getDataManager().getStorage().select("SELECT v FROM kv WHERE k = 'season';", 1);
      String stored = rows.isEmpty() ? null : (String)rows.get(0)[0];
      if (current.equals(stored)) {
         return;
      }

      if (stored != null) {
         this.finalizeSeason(stored);
      }

      this.plugin.getDataManager().getStorage().exec("INSERT INTO kv (k, v) VALUES ('season', ?) ON CONFLICT(k) DO UPDATE SET v = excluded.v;", current);
   }

   private void finalizeSeason(String endedSeason) {
      List<Entry> ranking = this.ranking();
      if (ranking.isEmpty()) {
         return;
      }

      Guild winner = ranking.get(0).guild();
      this.championGuildId = winner.getId();
      this.plugin.getDataManager().getStorage().exec(
         "INSERT INTO kv (k, v) VALUES ('season_champion', ?) ON CONFLICT(k) DO UPDATE SET v = excluded.v;", winner.getId());
      winner.depositBank(CHAMPION_BANK_REWARD);
      this.plugin.getDataManager().saveGuild(winner);
      for (UUID member : winner.getMembers().keySet()) {
         this.plugin.getDataManager().creditPlayer(member, CHAMPION_MEMBER_REWARD);
      }

      ChatUtil.broadcast("&6&l👑 FIN DE SAISON " + endedSeason + " ! &eLa guilde &6&l" + winner.getName() + " &eest sacrée championne avec &f" + ranking.get(0).score() + " points &e!");
      ChatUtil.broadcast("&7Récompense : &6" + String.format("%.0f", CHAMPION_BANK_REWARD) + " pièces en banque de guilde &7et &6" + String.format("%.0f", CHAMPION_MEMBER_REWARD) + " pièces &7par membre. Une nouvelle saison commence !");
      this.addHistory("👑 " + winner.getName() + " remporte la saison " + endedSeason + ".");
      this.stats.clear();
      this.plugin.getDataManager().getStorage().exec("DELETE FROM guild_stats;");
   }
}
