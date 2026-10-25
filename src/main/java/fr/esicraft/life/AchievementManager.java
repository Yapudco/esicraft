package fr.esicraft.life;

import fr.esicraft.EsicraftPlugin;
import fr.esicraft.data.PlayerData;
import fr.esicraft.guild.Guild;
import fr.esicraft.util.ChatUtil;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.Sound;
import org.bukkit.entity.Player;

/**
 * Succès des joueurs (table {@code achievements}) et compteurs de statistiques ({@code player_stats}).
 * Tout est gardé en mémoire (serveur entre amis : quelques centaines de lignes au plus) et écrit en asynchrone.
 * Un succès obtenu par moins de 10 % des joueurs est « rare » et annoncé dans le journal de guerre.
 */
public class AchievementManager {
   public static final double RARE_THRESHOLD = 10.0;
   public static final String STAT_MONSTERS = "monsters";
   public static final String STAT_PVP_KILLS = "pvp_kills";
   public static final String STAT_PVP_DEATHS = "pvp_deaths";
   public static final String STAT_RAIDS = "raids_launched";
   public static final String STAT_DEFENSES = "raids_repelled";
   public static final String STAT_CONTRACTS = "contracts";
   public static final String STAT_TRIPS = "trips";
   public static final String STAT_HELPS = "helps";
   public static final String STAT_SEASONS = "seasons_seen";

   private final EsicraftPlugin plugin;
   private final Map<UUID, Map<Achievement, Long>> unlocked = new ConcurrentHashMap<>();
   private final Map<Achievement, Integer> holders = new ConcurrentHashMap<>();
   private final Map<UUID, Map<String, Long>> stats = new ConcurrentHashMap<>();
   private final Set<String> dirtyStats = ConcurrentHashMap.newKeySet();
   private volatile int knownPlayers = 1;

   public AchievementManager(EsicraftPlugin plugin) {
      this.plugin = plugin;
   }

   public void init() {
      var db = this.plugin.getDataManager().getStorage();
      db.exec("CREATE TABLE IF NOT EXISTS achievements (uuid VARCHAR(36), id VARCHAR(32), ts BIGINT, PRIMARY KEY (uuid, id));");
      db.exec("CREATE TABLE IF NOT EXISTS player_stats (uuid VARCHAR(36), k VARCHAR(32), v BIGINT, PRIMARY KEY (uuid, k));");
      for (Object[] r : db.select("SELECT uuid, id, ts FROM achievements;", 3)) {
         Achievement a = Achievement.parse((String)r[1]);
         if (a != null) {
            try {
               this.unlocked.computeIfAbsent(UUID.fromString((String)r[0]), k -> new ConcurrentHashMap<>()).put(a, ((Number)r[2]).longValue());
               this.holders.merge(a, 1, Integer::sum);
            } catch (IllegalArgumentException ignored) {
            }
         }
      }

      for (Object[] r : db.select("SELECT uuid, k, v FROM player_stats;", 3)) {
         try {
            this.stats.computeIfAbsent(UUID.fromString((String)r[0]), k -> new ConcurrentHashMap<>()).put((String)r[1], ((Number)r[2]).longValue());
         } catch (IllegalArgumentException ignored) {
         }
      }

      this.refreshKnownPlayers();
      Bukkit.getScheduler().runTaskTimerAsynchronously(this.plugin, this::flush, 1200L, 1200L);
      Bukkit.getScheduler().runTaskTimer(this.plugin, this::checkWealth, 1200L, 1200L);
   }

   public void refreshKnownPlayers() {
      var rows = this.plugin.getDataManager().getStorage().select("SELECT COUNT(*) FROM players;", 1);
      this.knownPlayers = rows.isEmpty() ? 1 : Math.max(1, ((Number)rows.get(0)[0]).intValue());
   }

   // ------------------------------------------------------------------ succès

   public boolean has(UUID uuid, Achievement a) {
      Map<Achievement, Long> map = this.unlocked.get(uuid);
      return map != null && map.containsKey(a);
   }

   public Map<Achievement, Long> of(UUID uuid) {
      Map<Achievement, Long> map = this.unlocked.get(uuid);
      return map == null ? Map.of() : Map.copyOf(map);
   }

   public int holders(Achievement a) {
      return this.holders.getOrDefault(a, 0);
   }

   public int knownPlayers() {
      return this.knownPlayers;
   }

   public double percent(Achievement a) {
      return Math.min(100.0, this.holders(a) * 100.0 / Math.max(1, this.knownPlayers));
   }

   public boolean isRare(Achievement a) {
      return this.percent(a) < RARE_THRESHOLD;
   }

   public void grant(UUID uuid, Achievement a) {
      if (uuid == null || a == null) {
         return;
      }

      long now = System.currentTimeMillis();
      if (this.unlocked.computeIfAbsent(uuid, k -> new ConcurrentHashMap<>()).putIfAbsent(a, now) != null) {
         return;
      }

      int count = this.holders.merge(a, 1, Integer::sum);
      String id = uuid.toString();
      Bukkit.getScheduler().runTaskAsynchronously(this.plugin, () -> this.plugin.getDataManager().getStorage().exec(
         "INSERT OR IGNORE INTO achievements (uuid, id, ts) VALUES (?, ?, ?);", id, a.name(), now));
      OfflinePlayer op = Bukkit.getOfflinePlayer(uuid);
      Player online = op.getPlayer();
      double pct = Math.min(100.0, count * 100.0 / Math.max(1, this.knownPlayers));
      boolean rare = pct < RARE_THRESHOLD;
      if (online != null) {
         ChatUtil.sendMessage(online, "&6🏅 Succès débloqué : &e" + a.getIcon() + " " + a.getTitle() + " &7— " + a.getDescription()
            + (rare ? " &d(rare : " + String.format(java.util.Locale.ROOT, "%.0f", pct) + " % des joueurs)" : ""));
         ChatUtil.playSound(online, Sound.UI_TOAST_CHALLENGE_COMPLETE, 0.8F, 1.3F);
      }

      if (a.isNotable() || rare && count <= 3) {
         Guild g = this.plugin.getGuildManager().getGuildByPlayer(uuid);
         this.plugin.getChronicle().log(Chronicle.Kind.ACHIEVEMENT, (op.getName() != null ? op.getName() : "?") + " débloque le succès « " + a.getTitle() + " ».",
            g != null ? g.getId() : null, null);
      }
   }

   public void grantGuild(Guild guild, Achievement a, boolean onlineOnly) {
      if (guild == null) {
         return;
      }

      for (UUID member : guild.getMembers().keySet()) {
         if (!onlineOnly || Bukkit.getPlayer(member) != null) {
            this.grant(member, a);
         }
      }
   }

   public record Unlock(UUID uuid, Achievement achievement, long ts) {
   }

   /** Derniers succès débloqués, tous joueurs confondus (pour le site web). */
   public java.util.List<Unlock> recentUnlocks(int limit) {
      java.util.List<Unlock> list = new java.util.ArrayList<>();
      for (Map.Entry<UUID, Map<Achievement, Long>> e : this.unlocked.entrySet()) {
         for (Map.Entry<Achievement, Long> a : e.getValue().entrySet()) {
            list.add(new Unlock(e.getKey(), a.getKey(), a.getValue()));
         }
      }

      list.sort((x, y) -> Long.compare(y.ts(), x.ts()));
      return list.size() > limit ? new java.util.ArrayList<>(list.subList(0, limit)) : list;
   }

   // ------------------------------------------------------------------ statistiques

   public long stat(UUID uuid, String key) {
      Map<String, Long> map = this.stats.get(uuid);
      return map == null ? 0L : map.getOrDefault(key, 0L);
   }

   public Map<String, Long> statsOf(UUID uuid) {
      Map<String, Long> map = this.stats.get(uuid);
      return map == null ? Map.of() : new HashMap<>(map);
   }

   public long addStat(UUID uuid, String key, long delta) {
      long value = this.stats.computeIfAbsent(uuid, k -> new ConcurrentHashMap<>()).merge(key, delta, Long::sum);
      this.dirtyStats.add(uuid + "|" + key);
      return value;
   }

   /** Ajoute un bit (ex. saisons vécues) ; renvoie le masque obtenu. */
   public long orStat(UUID uuid, String key, long bits) {
      long value = this.stats.computeIfAbsent(uuid, k -> new ConcurrentHashMap<>()).merge(key, bits, (a, b) -> a | b);
      this.dirtyStats.add(uuid + "|" + key);
      return value;
   }

   private void flush() {
      if (this.dirtyStats.isEmpty()) {
         return;
      }

      var db = this.plugin.getDataManager().getStorage();
      var keys = new java.util.ArrayList<>(this.dirtyStats);
      this.dirtyStats.removeAll(keys);
      db.runInTransaction(() -> {
         for (String k : keys) {
            int sep = k.indexOf('|');
            try {
               UUID uuid = UUID.fromString(k.substring(0, sep));
               String key = k.substring(sep + 1);
               db.exec("INSERT INTO player_stats (uuid, k, v) VALUES (?, ?, ?) ON CONFLICT(uuid, k) DO UPDATE SET v = excluded.v;", uuid.toString(), key,
                  this.stat(uuid, key));
            } catch (RuntimeException ignored) {
            }
         }
      });
   }

   /** Appelé à l'arrêt du plugin. */
   public void shutdown() {
      this.flush();
   }

   private void checkWealth() {
      for (Player p : Bukkit.getOnlinePlayers()) {
         PlayerData data = this.plugin.getDataManager().getPlayerData(p.getUniqueId());
         if (data != null) {
            if (data.getCoins() >= 10000.0) {
               this.grant(p.getUniqueId(), Achievement.RICH);
            }

            if (data.getCoins() >= 100000.0) {
               this.grant(p.getUniqueId(), Achievement.TYCOON);
            }
         }
      }
   }
}
