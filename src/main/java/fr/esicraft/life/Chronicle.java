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

   public void pvpKill(String killerGuild, String victimGuild) {
      if (killerGuild == null || victimGuild == null || killerGuild.equals(victimGuild)) {
         return;
      }

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
      for (String table : new String[]{"journal", "raid_log", "pvp_log", "guild_history", "age_log"}) {
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

   public List<Rivalry> rivalries() {
      java.util.Map<String, int[]> map = new java.util.HashMap<>();
      for (Object[] r : this.db().select("SELECT attacker_id, target_id, COUNT(*) FROM raid_log WHERE outcome = 'launched' AND attacker_id IS NOT NULL GROUP BY attacker_id, target_id;", 3)) {
         map.computeIfAbsent(r[0] + ">" + r[1], k -> new int[2])[0] = (int)num(r[2]);
      }

      for (Object[] r : this.db().select("SELECT killer_guild, victim_guild, kills FROM pvp_log;", 3)) {
         map.computeIfAbsent(r[0] + ">" + r[1], k -> new int[2])[1] = (int)num(r[2]);
      }

      List<Rivalry> list = new ArrayList<>();
      for (var e : map.entrySet()) {
         String[] ids = e.getKey().split(">", 2);
         list.add(new Rivalry(ids[0], ids[1], e.getValue()[0], e.getValue()[1]));
      }

      return list;
   }
}
