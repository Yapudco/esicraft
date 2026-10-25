package fr.esicraft.economy;

import fr.esicraft.EsicraftPlugin;
import fr.esicraft.fun.SeasonManager;
import fr.esicraft.guild.Guild;
import fr.esicraft.util.ChatUtil;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import org.bukkit.Bukkit;

/**
 * Équilibrage entre guildes.
 * <ul>
 *   <li><b>Revanche</b> : la guilde qui perd un raid obtient des tarifs réduits (24 h) pour attaquer son vainqueur.</li>
 *   <li><b>Rattrapage</b> : les guildes sous la médiane de la saison touchent un bonus sur les primes de monstres,
 *       d'autant plus faible qu'elles se rapprochent de la médiane.</li>
 * </ul>
 */
public class BalanceManager {
   /** Revanche en cours : le perdant (« loser ») peut attaquer le vainqueur à prix réduit jusqu'à {@code until}. */
   public record Revenge(String loserId, String winnerId, long until) {
   }

   private final EsicraftPlugin plugin;
   private final Map<String, Long> revenge = new ConcurrentHashMap<>();
   private volatile double cachedMedian;
   private volatile long medianAt;

   public BalanceManager(EsicraftPlugin plugin) {
      this.plugin = plugin;
   }

   private static String key(String loserId, String winnerId) {
      return loserId + ">" + winnerId;
   }

   public void init() {
      this.plugin.getDataManager().getStorage().exec("CREATE TABLE IF NOT EXISTS revenge (loser VARCHAR(36), winner VARCHAR(36), until BIGINT, PRIMARY KEY (loser, winner));");
      long now = System.currentTimeMillis();
      for (Object[] row : this.plugin.getDataManager().getStorage().select("SELECT loser, winner, until FROM revenge;", 3)) {
         long until = ((Number)row[2]).longValue();
         if (until > now) {
            this.revenge.put(key((String)row[0], (String)row[1]), until);
         }
      }
   }

   public void clear() {
      this.revenge.clear();
   }

   // ------------------------------------------------------------------ revanche

   private boolean revengeEnabled() {
      return this.plugin.getConfig().getBoolean("revenge.enabled", true);
   }

   /** Enregistre la défaite d'une guilde face à un raid de {@code winner}. */
   public void recordRaidLoss(Guild loser, Guild winner) {
      if (!this.revengeEnabled() || loser == null || winner == null || loser.getId().equals(winner.getId())) {
         return;
      }

      long until = System.currentTimeMillis() + this.plugin.getConfig().getLong("revenge.hours", 24L) * 3600000L;
      this.revenge.put(key(loser.getId(), winner.getId()), until);
      String loserId = loser.getId();
      String winnerId = winner.getId();
      Bukkit.getScheduler().runTaskAsynchronously(this.plugin, () -> this.plugin.getDataManager().getStorage().exec(
         "INSERT INTO revenge (loser, winner, until) VALUES (?, ?, ?) ON CONFLICT(loser, winner) DO UPDATE SET until = excluded.until;", loserId, winnerId, until));
      int percent = (int)Math.round(this.plugin.getConfig().getDouble("revenge.discount", 0.5) * 100.0);
      ChatUtil.broadcastGuild(loser, "&6🔥 &lREVANCHE ! &eVotre guilde a perdu ce raid : &a-" + percent + " % &esur les hordes contre &6" + winner.getName()
         + " &ependant &a" + this.plugin.getConfig().getLong("revenge.hours", 24L) + " h&e.");
   }

   /** Multiplicateur de prix d'une horde de {@code attacker} contre {@code target} (1.0 sans revanche). */
   public double raidPriceFactor(Guild attacker, Guild target) {
      if (!this.revengeEnabled() || attacker == null || target == null) {
         return 1.0;
      }

      Long until = this.revenge.get(key(attacker.getId(), target.getId()));
      if (until == null || until <= System.currentTimeMillis()) {
         return 1.0;
      }

      return Math.max(0.0, 1.0 - this.plugin.getConfig().getDouble("revenge.discount", 0.5));
   }

   public boolean hasRevenge(Guild attacker, Guild target) {
      return this.raidPriceFactor(attacker, target) < 1.0;
   }

   /** Revanches encore valables de cette guilde (pour le site et les menus). */
   public List<Revenge> revengesOf(String loserId) {
      long now = System.currentTimeMillis();
      List<Revenge> list = new ArrayList<>();
      for (Map.Entry<String, Long> e : this.revenge.entrySet()) {
         if (e.getValue() > now && e.getKey().startsWith(loserId + ">")) {
            list.add(new Revenge(loserId, e.getKey().substring(loserId.length() + 1), e.getValue()));
         }
      }

      return list;
   }

   // ------------------------------------------------------------------ rattrapage

   private double median() {
      long now = System.currentTimeMillis();
      if (now - this.medianAt > 30000L) {
         List<Long> scores = new ArrayList<>();
         SeasonManager season = this.plugin.getSeasonManager();
         for (Guild g : this.plugin.getGuildManager().getAllGuilds()) {
            scores.add(season.score(g));
         }

         scores.sort(Long::compare);
         double m = 0.0;
         if (!scores.isEmpty()) {
            int n = scores.size();
            m = n % 2 == 1 ? scores.get(n / 2) : (scores.get(n / 2 - 1) + scores.get(n / 2)) / 2.0;
         }

         this.cachedMedian = m;
         this.medianAt = now;
      }

      return this.cachedMedian;
   }

   /** Bonus de rattrapage en pourcentage (0 si la guilde est au-dessus de la médiane ou s'il y a moins de 4 guildes). */
   public int catchUpPercent(Guild guild) {
      if (guild == null || !this.plugin.getConfig().getBoolean("catchup.enabled", true) || this.plugin.getGuildManager().getAllGuilds().size() < 4) {
         return 0;
      }

      double median = this.median();
      if (median <= 0.0) {
         return 0;
      }

      double gap = (median - this.plugin.getSeasonManager().score(guild)) / median;
      if (gap <= 0.0) {
         return 0;
      }

      double max = this.plugin.getConfig().getDouble("catchup.max-bonus-percent", 25.0);
      return (int)Math.round(Math.min(max, max * Math.min(1.0, gap)));
   }

   public double catchUpMultiplier(Guild guild) {
      return 1.0 + this.catchUpPercent(guild) / 100.0;
   }
}
