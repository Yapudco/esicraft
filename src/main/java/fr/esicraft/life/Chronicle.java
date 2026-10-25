package fr.esicraft.life;

import fr.esicraft.EsicraftPlugin;
import fr.esicraft.data.SQLiteStorage;
import fr.esicraft.guild.Guild;
import java.util.ArrayList;
import java.util.List;
import org.bukkit.Bukkit;

/**
 * Journal de guerre et mémoire du serveur : fil d'actualité (raids, traités, Âges, événements…), journal des raids et des
 * duels entre guildes (rivalités), courbes d'évolution horaires des guildes et dates de passage des Âges.
 * Toutes les écritures sont asynchrones ; les lectures sont faites par le site web depuis un thread asynchrone.
 */
public class Chronicle {
   public enum Kind {
      RAID("⚔"), DEFENSE("🛡"), CONQUEST("💥"), TREATY("📜"), BETRAYAL("💔"), AGE("⏳"), EVENT("🎉"), SEASON("👑"), GUILD("🏰"),
      ACHIEVEMENT("🏅"), HELP("🆘"), NATURE("🍂"), CONTRACT("🧩");

      private final String icon;

      Kind(String icon) {
         this.icon = icon;
      }

      public String getIcon() {
         return this.icon;
      }

      public static Kind parse(String s) {
         try {
            return Kind.valueOf(s);
         } catch (RuntimeException e) {
            return SEASON;
         }
      }
   }

   public record Entry(long ts, Kind kind, String text, String guildA, String guildB) {
   }

   /** Point d'une courbe d'évolution. */
   public record Point(long ts, long score, double bank, int claims, int members, int age) {
   }

   /** Passage d'un Âge par une guilde. */
   public record AgeMark(String guildId, String guildName, int age, long ts) {
   }

   /** Nombre de raids lancés et de joueurs tués entre deux guildes (a = attaquant / tueur). */
   public record Rivalry(String a, String b, int raids, int kills) {
   }

   private static final long HISTORY_RETENTION_MS = 90L * 24L * 3600000L;
   private final EsicraftPlugin plugin;

   public Chronicle(EsicraftPlugin plugin) {
      this.plugin = plugin;
   }

   private SQLiteStorage db() {
      return this.plugin.getDataManager().getStorage();
   }

   public void init() {
      this.db().exec("CREATE TABLE IF NOT EXISTS journal (id INTEGER PRIMARY KEY AUTOINCREMENT, ts BIGINT, kind VARCHAR(16), text TEXT, guild_a VARCHAR(36), guild_b VARCHAR(36));");
      this.db().exec("CREATE TABLE IF NOT EXISTS raid_log (id INTEGER PRIMARY KEY AUTOINCREMENT, ts BIGINT, attacker_id VARCHAR(36), target_id VARCHAR(36), outcome VARCHAR(12));");
      this.db().exec("CREATE TABLE IF NOT EXISTS pvp_log (killer_guild VARCHAR(36), victim_guild VARCHAR(36), kills INT DEFAULT 0, PRIMARY KEY (killer_guild, victim_guild));");
      this.db().exec("CREATE TABLE IF NOT EXISTS pvp_events (ts BIGINT, killer_guild VARCHAR(36), victim_guild VARCHAR(36));");
      this.db().exec("CREATE TABLE IF NOT EXISTS rivalry_reset (pair VARCHAR(80) PRIMARY KEY, ts BIGINT);");
      this.db().exec("CREATE TABLE IF NOT EXISTS guild_history (ts BIGINT, guild_id VARCHAR(36), score BIGINT, bank DOUBLE, claims INT, members INT, age INT);");
      this.db().exec("CREATE INDEX IF NOT EXISTS idx_guild_history ON guild_history (guild_id, ts);");
      this.db().exec("CREATE TABLE IF NOT EXISTS age_log (guild_id VARCHAR(36), guild_name VARCHAR(32), age INT, ts BIGINT, PRIMARY KEY (guild_id, age));");
      // Les guildes existantes reçoivent au moins leur Âge 1 (date de création) et leur Âge actuel.
      for (Guild g : this.plugin.getGuildManager().getAllGuilds()) {
         this.db().exec("INSERT OR IGNORE INTO age_log (guild_id, guild_name, age, ts) VALUES (?, ?, 1, ?);", g.getId(), g.getName(), g.getCreatedAt());
         if (g.getCurrentAge().getLevel() > 1) {
            this.db().exec("INSERT OR IGNORE INTO age_log (guild_id, guild_name, age, ts) VALUES (?, ?, ?, ?);", g.getId(), g.getName(),
               g.getCurrentAge().getLevel(), System.currentTimeMillis());
         }
      }

      // Un point de courbe par heure (le premier deux minutes après le démarrage).
      Bukkit.getScheduler().runTaskTimer(this.plugin, this::snapshotGuilds, 2400L, 72000L);
   }

   static String strip(String s) {
      return s == null ? "" : s.replaceAll("[&§]#[0-9a-fA-F]{6}", "").replaceAll("[&§][0-9a-fk-orxA-FK-ORX]", "").trim();
   }

   private void async(Runnable r) {
      if (this.plugin.isEnabled()) {
         Bukkit.getScheduler().runTaskAsynchronously(this.plugin, r);
      } else {
         r.run();
      }
   }

   // ------------------------------------------------------------------ écritures

   public void log(Kind kind, String text, String guildA, String guildB) {
      long now = System.currentTimeMillis();
      String clean = strip(text);
      this.async(() -> this.db().exec("INSERT INTO journal (ts, kind, text, guild_a, guild_b) VALUES (?, ?, ?, ?, ?);", now, kind.name(), clean, guildA, guildB));
   }

   public void log(Kind kind, String text) {
      this.log(kind, text, null, null);
   }

   public void raid(String attackerId, String targetId, String outcome) {
      long now = System.currentTimeMillis();
      this.async(() -> this.db().exec("INSERT INTO raid_log (ts, attacker_id, target_id, outcome) VALUES (?, ?, ?, ?);", now, attackerId, targetId, outcome));
   }

   /** Rivalité ouverte par une agression contre une guilde liée par un pacte (comptée comme une hostilité dans les rivalités). */
   public void rivalry(String aggressorId, String victimId) {
      this.raid(aggressorId, victimId, "betrayal");
   }

   public void pvpKill(String killerGuild, String victimGuild) {
      if (killerGuild == null || victimGuild == null || killerGuild.equals(victimGuild)) {
         return;
      }

      long now = System.currentTimeMillis();
      this.async(() -> this.db().exec("INSERT INTO pvp_events (ts, killer_guild, victim_guild) VALUES (?, ?, ?);", now, killerGuild, victimGuild));
      this.async(() -> this.db().exec("INSERT INTO pvp_log (killer_guild, victim_guild, kills) VALUES (?, ?, 1) "
         + "ON CONFLICT(killer_guild, victim_guild) DO UPDATE SET kills = kills + 1;", killerGuild, victimGuild));
   }

   public void ageReached(Guild guild, int age) {
      long now = System.currentTimeMillis();
      String id = guild.getId();
      String name = guild.getName();
      this.async(() -> this.db().exec("INSERT OR IGNORE INTO age_log (guild_id, guild_name, age, ts) VALUES (?, ?, ?, ?);", id, name, age, now));
   }

   private void snapshotGuilds() {
      long now = System.currentTimeMillis();
      List<Object[]> rows = new ArrayList<>();
      for (Guild g : this.plugin.getGuildManager().getAllGuilds()) {
         rows.add(new Object[]{now, g.getId(), this.plugin.getSeasonManager().score(g), g.getBankBalance(), g.getClaimedChunks().size(), g.getMembers().size(),
            g.getCurrentAge().getLevel()});
      }

      this.async(() -> this.db().runInTransaction(() -> {
         for (Object[] r : rows) {
            this.db().exec("INSERT INTO guild_history (ts, guild_id, score, bank, claims, members, age) VALUES (?, ?, ?, ?, ?, ?, ?);", r);
         }

         this.db().exec("DELETE FROM guild_history WHERE ts < ?;", now - HISTORY_RETENTION_MS);
         this.db().exec("DELETE FROM journal WHERE id NOT IN (SELECT id FROM journal ORDER BY id DESC LIMIT 2000);");
      }));
   }

   /** Données effacées par /game reset. */
   public void clear() {
      for (String table : new String[]{"journal", "raid_log", "pvp_log", "pvp_events", "rivalry_reset", "guild_history", "age_log"}) {
         this.db().exec("DELETE FROM " + table + ";");
      }
   }

   // ------------------------------------------------------------------ lectures (threads asynchrones)

   private static long num(Object o) {
      return o instanceof Number n ? n.longValue() : 0L;
   }

   public List<Entry> recent(int limit) {
      List<Entry> list = new ArrayList<>();
      for (Object[] r : this.db().select("SELECT ts, kind, text, guild_a, guild_b FROM journal ORDER BY id DESC LIMIT ?;", 5, limit)) {
         list.add(new Entry(num(r[0]), Kind.parse((String)r[1]), (String)r[2], (String)r[3], (String)r[4]));
      }

      return list;
   }

   public List<Point> history(String guildId) {
      List<Point> list = new ArrayList<>();
      for (Object[] r : this.db().select("SELECT ts, score, bank, claims, members, age FROM guild_history WHERE guild_id = ? ORDER BY ts;", 6, guildId)) {
         list.add(new Point(num(r[0]), num(r[1]), r[2] instanceof Number n ? n.doubleValue() : 0.0, (int)num(r[3]), (int)num(r[4]), (int)num(r[5])));
      }

      return list;
   }

   public List<AgeMark> ageMarks() {
      List<AgeMark> list = new ArrayList<>();
      for (Object[] r : this.db().select("SELECT guild_id, guild_name, age, ts FROM age_log ORDER BY ts;", 4)) {
         list.add(new AgeMark((String)r[0], (String)r[1], (int)num(r[2]), num(r[3])));
      }

      return list;
   }

   private static String pairKey(String a, String b) {
      return a.compareTo(b) < 0 ? a + "|" + b : b + "|" + a;
   }

   /** Un traité (alliance, pacte, vassalité) signé entre deux guildes efface leur rivalité passée. */
   public void resetRivalry(String guildA, String guildB) {
      long now = System.currentTimeMillis();
      String key = pairKey(guildA, guildB);
      this.async(() -> this.db().exec("INSERT INTO rivalry_reset (pair, ts) VALUES (?, ?) ON CONFLICT(pair) DO UPDATE SET ts = excluded.ts;", key, now));
   }

   /** Poids d'un fait d'hostilité : divisé par deux tous les {@code rivalry.half-life-days} jours. */
   private double weight(long ts, long now) {
      double halfLifeMs = Math.max(0.1, this.plugin.getConfig().getDouble("rivalry.half-life-days", 7.0)) * 86400000.0;
      return Math.pow(0.5, Math.max(0L, now - ts) / halfLifeMs);
   }

   /** Rivalités entre guildes : raids, trahisons et morts qui s'estompent avec le temps et disparaissent à la signature d'un traité. */
   public List<Rivalry> rivalries() {
      long now = System.currentTimeMillis();
      java.util.Map<String, Long> resets = new java.util.HashMap<>();
      for (Object[] r : this.db().select("SELECT pair, ts FROM rivalry_reset;", 2)) {
         resets.put((String)r[0], num(r[1]));
      }

      java.util.Map<String, double[]> map = new java.util.HashMap<>();
      for (Object[] r : this.db().select("SELECT ts, attacker_id, target_id FROM raid_log WHERE outcome IN ('launched', 'betrayal') AND attacker_id IS NOT NULL;", 3)) {
         this.accumulate(map, resets, num(r[0]), (String)r[1], (String)r[2], 0, now);
      }

      for (Object[] r : this.db().select("SELECT ts, killer_guild, victim_guild FROM pvp_events;", 3)) {
         this.accumulate(map, resets, num(r[0]), (String)r[1], (String)r[2], 1, now);
      }

      List<Rivalry> list = new ArrayList<>();
      for (var e : map.entrySet()) {
         int raids = (int)Math.round(e.getValue()[0]);
         int kills = (int)Math.round(e.getValue()[1]);
         if (raids > 0 || kills > 0) {
            String[] ids = e.getKey().split(">", 2);
            list.add(new Rivalry(ids[0], ids[1], raids, kills));
         }
      }

      return list;
   }

   private void accumulate(java.util.Map<String, double[]> map, java.util.Map<String, Long> resets, long ts, String a, String b, int slot, long now) {
      if (a == null || b == null) {
         return;
      }

      Long reset = resets.get(pairKey(a, b));
      if (reset != null && ts <= reset) {
         return;
      }

      map.computeIfAbsent(a + ">" + b, k -> new double[2])[slot] += this.weight(ts, now);
   }
}
