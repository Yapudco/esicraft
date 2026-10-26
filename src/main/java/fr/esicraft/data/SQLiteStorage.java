package fr.esicraft.data;

import fr.esicraft.EsicraftPlugin;
import fr.esicraft.age.AgeType;
import fr.esicraft.guild.Guild;
import fr.esicraft.guild.GuildRole;
import fr.esicraft.guild.GuildSpecialization;
import fr.esicraft.market.MarketListing;
import fr.esicraft.nexus.Nexus;
import fr.esicraft.race.Race;
import fr.esicraft.util.ChunkKey;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.nio.charset.StandardCharsets;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.Base64;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.AbstractMap.SimpleEntry;
import java.util.Map.Entry;
import java.util.logging.Level;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.inventory.ItemStack;
import org.bukkit.util.io.BukkitObjectInputStream;

public class SQLiteStorage {
   private final EsicraftPlugin plugin;
   private final File dbFile;
   private Connection connection;
   /** Seconde connexion, en lecture seule (mode WAL) : les lectures ne font plus la queue derrière les sauvegardes. */
   private Connection readConnection;
   private final Object readLock = new Object();

   public SQLiteStorage(EsicraftPlugin plugin) {
      this.plugin = plugin;
      this.dbFile = new File(plugin.getDataFolder(), "esicraft.db");
   }

   public synchronized void init() {
      try {
         if (!this.plugin.getDataFolder().exists()) {
            this.plugin.getDataFolder().mkdirs();
         }

         Class.forName("org.sqlite.JDBC");
         this.connection = DriverManager.getConnection("jdbc:sqlite:" + this.dbFile.getAbsolutePath());
         this.applyPragmas();
         this.createTables();
         this.runMigrations();
         this.readConnection = DriverManager.getConnection("jdbc:sqlite:" + this.dbFile.getAbsolutePath());
         try (Statement st = this.readConnection.createStatement()) {
            st.execute("PRAGMA busy_timeout=5000;");
            st.execute("PRAGMA query_only=ON;");
         }
         this.plugin.getLogger().info("Base de données SQLite initialisée avec succès.");
      } catch (Exception var2) {
         this.plugin.getLogger().log(Level.SEVERE, "Impossible d'initialiser SQLite !", (Throwable)var2);
      }
   }

   private void applyPragmas() throws SQLException {
      try (Statement st = this.connection.createStatement()) {
         st.execute("PRAGMA journal_mode=WAL;");
         st.execute("PRAGMA synchronous=NORMAL;");
         st.execute("PRAGMA busy_timeout=5000;");
         st.execute("PRAGMA cache_size=-4000;");
      }
   }

   private void createTables() throws SQLException {
      try (Statement statement = this.connection.createStatement()) {
         statement.executeUpdate(
            "    CREATE TABLE IF NOT EXISTS players (\n        uuid VARCHAR(36) PRIMARY KEY,\n        name VARCHAR(32),\n        race VARCHAR(32),\n        guild_id VARCHAR(36),\n        guild_role VARCHAR(32),\n        coins DOUBLE,\n        last_race_change BIGINT,\n        race_changes_count INT DEFAULT 0,\n        has_joined_guild_before INT DEFAULT 0\n    );\n"
         );
         statement.executeUpdate(
            "    CREATE TABLE IF NOT EXISTS guilds (\n        id VARCHAR(36) PRIMARY KEY,\n        name VARCHAR(32),\n        tag VARCHAR(8),\n        leader_uuid VARCHAR(36),\n        specialization VARCHAR(32),\n        bank_balance DOUBLE,\n        home_world VARCHAR(64),\n        home_x DOUBLE,\n        home_y DOUBLE,\n        home_z DOUBLE,\n        home_yaw FLOAT,\n        home_pitch FLOAT,\n        nexus_world VARCHAR(64),\n        nexus_x DOUBLE,\n        nexus_y DOUBLE,\n        nexus_z DOUBLE,\n        nexus_health INT,\n        nexus_max_health INT,\n        vulnerable_until BIGINT,\n        current_age INT,\n        age_donations TEXT,\n        created_at BIGINT,\n        can_change_spec INT DEFAULT 1,\n        historical_members TEXT DEFAULT ''\n    );\n"
         );
         statement.executeUpdate(
            "    CREATE TABLE IF NOT EXISTS guild_members (\n        guild_id VARCHAR(36),\n        player_uuid VARCHAR(36),\n        role VARCHAR(32),\n        PRIMARY KEY (guild_id, player_uuid)\n    );\n"
         );
         statement.executeUpdate(
            "    CREATE TABLE IF NOT EXISTS guild_claims (\n        guild_id VARCHAR(36),\n        world VARCHAR(64),\n        chunk_x INT,\n        chunk_z INT,\n        PRIMARY KEY (world, chunk_x, chunk_z)\n    );\n"
         );
         statement.executeUpdate(
            "    CREATE TABLE IF NOT EXISTS server_age (\n        key VARCHAR(32) PRIMARY KEY,\n        current_age INT,\n        donations_data TEXT\n    );\n"
         );
         statement.executeUpdate(
            "    CREATE TABLE IF NOT EXISTS market_listings (\n        id VARCHAR(36) PRIMARY KEY,\n        seller_uuid VARCHAR(36),\n        seller_name VARCHAR(32),\n        item_data TEXT,\n        price DOUBLE,\n        created_at BIGINT\n    );\n"
         );
         statement.executeUpdate("CREATE TABLE IF NOT EXISTS raid_cooldowns (guild_id VARCHAR(36) PRIMARY KEY, last_raid BIGINT);");
         statement.executeUpdate("CREATE TABLE IF NOT EXISTS contract_progress (uuid VARCHAR(36), day VARCHAR(10), idx INT, progress INT, claimed INT DEFAULT 0, PRIMARY KEY (uuid, day, idx));");
         statement.executeUpdate("CREATE TABLE IF NOT EXISTS guild_stats (guild_id VARCHAR(36) PRIMARY KEY, repelled INT DEFAULT 0, conquests INT DEFAULT 0);");
         statement.executeUpdate("CREATE TABLE IF NOT EXISTS history (id INTEGER PRIMARY KEY AUTOINCREMENT, ts BIGINT, text TEXT);");
         statement.executeUpdate("CREATE TABLE IF NOT EXISTS kv (k VARCHAR(64) PRIMARY KEY, v TEXT);");
         statement.executeUpdate("CREATE TABLE IF NOT EXISTS stations (id VARCHAR(36) PRIMARY KEY, name VARCHAR(32), guild_id VARCHAR(36), world VARCHAR(64), sx INT, sy INT, sz INT, rx INT, ry INT, rz INT);");
         statement.executeUpdate("CREATE TABLE IF NOT EXISTS rail_policy (guild_id VARCHAR(36) PRIMARY KEY, toll DOUBLE);");
         statement.executeUpdate("CREATE TABLE IF NOT EXISTS rail_relations (owner_id VARCHAR(36), other_id VARCHAR(36), relation VARCHAR(16), PRIMARY KEY (owner_id, other_id));");
         statement.executeUpdate("CREATE TABLE IF NOT EXISTS treaties (id VARCHAR(36) PRIMARY KEY, a_id VARCHAR(36), b_id VARCHAR(36), type VARCHAR(16), suzerain_id VARCHAR(36), created_at BIGINT, expires_at BIGINT, last_tribute BIGINT);");
         statement.executeUpdate("CREATE TABLE IF NOT EXISTS treaty_grace (breaker_id VARCHAR(36), victim_id VARCHAR(36), until BIGINT, PRIMARY KEY (breaker_id, victim_id));");
      }
   }

   private void runMigrations() throws SQLException {
      try (Statement st = this.connection.createStatement()) {
         st.executeUpdate("CREATE TABLE IF NOT EXISTS schema_version (version INT PRIMARY KEY);");
      }

      int currentVersion = 0;

      try (
         Statement st = this.connection.createStatement();
         ResultSet rs = st.executeQuery("SELECT MAX(version) AS version FROM schema_version;");
      ) {
         if (rs.next()) {
            currentVersion = rs.getInt("version");
         }
      }

      if (currentVersion < 2) {
         this.applyMigration("ALTER TABLE players ADD COLUMN race_changes_count INT DEFAULT 0;");
         currentVersion = 2;
      }

      if (currentVersion < 3) {
         this.applyMigration("ALTER TABLE players ADD COLUMN has_joined_guild_before INT DEFAULT 0;");
         currentVersion = 3;
      }

      if (currentVersion < 4) {
         this.applyMigration("ALTER TABLE guilds ADD COLUMN can_change_spec INT DEFAULT 1;");
         this.applyMigration("ALTER TABLE guilds ADD COLUMN historical_members TEXT DEFAULT '';");
         currentVersion = 4;
      }

      if (currentVersion < 5) {
         this.applyMigration("ALTER TABLE guilds ADD COLUMN nexus_traps TEXT DEFAULT '';");
         currentVersion = 5;
      }

      if (currentVersion < 6) {
         // v6 : cooldowns de raid persistants (table raid_cooldowns, créée par createTables).
         currentVersion = 6;
      }

      try (Statement cleanup = this.connection.createStatement()) {
         cleanup.executeUpdate("DELETE FROM schema_version WHERE version <> " + currentVersion + ";");
      }

      try (PreparedStatement ps = this.connection
            .prepareStatement("INSERT INTO schema_version (version) VALUES (?) ON CONFLICT(version) DO UPDATE SET version = excluded.version;")) {
         ps.setInt(1, currentVersion);
         ps.executeUpdate();
      }
   }

   private void applyMigration(String sql) {
      try (Statement st = this.connection.createStatement()) {
         st.executeUpdate(sql);
      } catch (SQLException var7) {
         if (!var7.getMessage().toLowerCase().contains("duplicate column")) {
            this.plugin.getLogger().log(Level.WARNING, "Erreur migration DB (" + sql + ") : " + var7.getMessage());
         }
      }
   }

   public synchronized void savePlayerData(PlayerData data) {
      if (data != null && this.connection != null) {
         String sql = "    INSERT INTO players (uuid, name, race, guild_id, guild_role, coins, last_race_change, race_changes_count, has_joined_guild_before)\n    VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)\n    ON CONFLICT(uuid) DO UPDATE SET\n        name = excluded.name,\n        race = excluded.race,\n        guild_id = excluded.guild_id,\n        guild_role = excluded.guild_role,\n        coins = excluded.coins,\n        last_race_change = excluded.last_race_change,\n        race_changes_count = excluded.race_changes_count,\n        has_joined_guild_before = excluded.has_joined_guild_before;\n";

         try (PreparedStatement ps = this.connection.prepareStatement(sql)) {
            ps.setString(1, data.getUniqueId().toString());
            ps.setString(2, data.getPlayerName());
            ps.setString(3, data.getRace().name());
            ps.setString(4, data.getGuildId());
            ps.setString(5, data.getGuildRole() != null ? data.getGuildRole().name() : null);
            ps.setDouble(6, data.getCoins());
            ps.setLong(7, data.getLastRaceChangeTimestamp());
            ps.setInt(8, data.getRaceChangesCount());
            ps.setInt(9, data.hasJoinedGuildBefore() ? 1 : 0);
            ps.executeUpdate();
         } catch (SQLException var8) {
            this.plugin.getLogger().log(Level.SEVERE, "Erreur sauvegarde joueur: " + data.getPlayerName(), (Throwable)var8);
         }
      }
   }

   /**
    * Exécute plusieurs écritures dans UNE seule transaction (un seul fsync au lieu d'un par ligne) : la sauvegarde
    * périodique de tous les joueurs et guildes devient bien moins coûteuse. En cas d'erreur, tout est annulé proprement.
    */
   public synchronized void runInTransaction(Runnable writes) {
      if (this.connection == null) {
         writes.run();
         return;
      }

      boolean previous = true;
      try {
         previous = this.connection.getAutoCommit();
         this.connection.setAutoCommit(false);
         writes.run();
         this.connection.commit();
      } catch (SQLException | RuntimeException e) {
         try {
            this.connection.rollback();
         } catch (SQLException ignored) {
         }

         this.plugin.getLogger().log(Level.SEVERE, "Erreur lors de la sauvegarde groupée (annulée)", e);
      } finally {
         try {
            this.connection.setAutoCommit(previous);
         } catch (SQLException ignored) {
         }
      }
   }

   public synchronized PlayerData loadPlayerData(UUID uuid, String playerName) {
      if (this.connection == null) {
         return new PlayerData(uuid, playerName);
      } else {
         String sql = "SELECT * FROM players WHERE uuid = ?;";

         try {
            PlayerData var8;
            try (PreparedStatement ps = this.connection.prepareStatement(sql)) {
               ps.setString(1, uuid.toString());

               try (ResultSet rs = ps.executeQuery()) {
                  if (!rs.next()) {
                     return new PlayerData(uuid, playerName);
                  }

                  PlayerData data = new PlayerData(uuid, rs.getString("name"));

                  try {
                     data.setRace(Race.valueOf(rs.getString("race")));
                  } catch (Exception var14) {
                  }

                  data.setGuildId(rs.getString("guild_id"));
                  String roleStr = rs.getString("guild_role");
                  if (roleStr != null) {
                     try {
                        data.setGuildRole(GuildRole.valueOf(roleStr));
                     } catch (Exception var13) {
                     }
                  }

                  data.setCoins(rs.getDouble("coins"));
                  data.setLastRaceChangeTimestamp(rs.getLong("last_race_change"));

                  try {
                     data.setRaceChangesCount(rs.getInt("race_changes_count"));
                  } catch (Exception var12) {
                  }

                  try {
                     data.setHasJoinedGuildBefore(rs.getInt("has_joined_guild_before") == 1);
                  } catch (Exception var11) {
                  }

                  var8 = data;
               }
            }

            return var8;
         } catch (SQLException var17) {
            this.plugin.getLogger().log(Level.SEVERE, "Erreur chargement joueur: " + uuid, (Throwable)var17);
            return new PlayerData(uuid, playerName);
         }
      }
   }

   public synchronized void saveGuild(Guild guild) {
      if (guild != null && this.connection != null) {
         String sql = "    INSERT INTO guilds (\n        id, name, tag, leader_uuid, specialization, bank_balance,\n        home_world, home_x, home_y, home_z, home_yaw, home_pitch,\n        nexus_world, nexus_x, nexus_y, nexus_z, nexus_health, nexus_max_health, vulnerable_until,\n        current_age, age_donations, created_at, can_change_spec, historical_members, nexus_traps\n    ) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)\n    ON CONFLICT(id) DO UPDATE SET\n        name = excluded.name,\n        tag = excluded.tag,\n        leader_uuid = excluded.leader_uuid,\n        specialization = excluded.specialization,\n        bank_balance = excluded.bank_balance,\n        home_world = excluded.home_world,\n        home_x = excluded.home_x,\n        home_y = excluded.home_y,\n        home_z = excluded.home_z,\n        home_yaw = excluded.home_yaw,\n        home_pitch = excluded.home_pitch,\n        nexus_world = excluded.nexus_world,\n        nexus_x = excluded.nexus_x,\n        nexus_y = excluded.nexus_y,\n        nexus_z = excluded.nexus_z,\n        nexus_health = excluded.nexus_health,\n        nexus_max_health = excluded.nexus_max_health,\n        vulnerable_until = excluded.vulnerable_until,\n        current_age = excluded.current_age,\n        age_donations = excluded.age_donations,\n        created_at = excluded.created_at,\n        can_change_spec = excluded.can_change_spec,\n        historical_members = excluded.historical_members,\n        nexus_traps = excluded.nexus_traps;\n";
         StringBuilder donationsSb = new StringBuilder();

         for (Entry<Material, Integer> entry : guild.getAgeDonations().entrySet()) {
            donationsSb.append(entry.getKey().name()).append(":").append(entry.getValue()).append(";");
         }

         StringBuilder histSb = new StringBuilder();

         for (UUID u : guild.getHistoricalMembers()) {
            histSb.append(u.toString()).append(";");
         }

         try (PreparedStatement ps = this.connection.prepareStatement(sql)) {
            ps.setString(1, guild.getId());
            ps.setString(2, guild.getName());
            ps.setString(3, guild.getTag());
            ps.setString(4, guild.getLeaderUuid().toString());
            ps.setString(5, guild.getSpecialization().name());
            ps.setDouble(6, guild.getBankBalance());
            Location home = guild.getHomeLocation();
            if (home != null && home.getWorld() != null) {
               ps.setString(7, home.getWorld().getName());
               ps.setDouble(8, home.getX());
               ps.setDouble(9, home.getY());
               ps.setDouble(10, home.getZ());
               ps.setFloat(11, home.getYaw());
               ps.setFloat(12, home.getPitch());
            } else {
               ps.setNull(7, 12);
               ps.setNull(8, 8);
               ps.setNull(9, 8);
               ps.setNull(10, 8);
               ps.setNull(11, 6);
               ps.setNull(12, 6);
            }

            Nexus nexus = guild.getNexus();
            if (nexus != null && nexus.getLocation() != null && nexus.getLocation().getWorld() != null) {
               ps.setString(13, nexus.getLocation().getWorld().getName());
               ps.setDouble(14, nexus.getLocation().getX());
               ps.setDouble(15, nexus.getLocation().getY());
               ps.setDouble(16, nexus.getLocation().getZ());
               ps.setInt(17, nexus.getCurrentHealth());
               ps.setInt(18, nexus.getMaxHealth());
            } else {
               ps.setNull(13, 12);
               ps.setNull(14, 8);
               ps.setNull(15, 8);
               ps.setNull(16, 8);
               ps.setNull(17, 4);
               ps.setNull(18, 4);
            }

            ps.setLong(19, guild.getVulnerableUntil());
            ps.setInt(20, guild.getCurrentAge().getLevel());
            ps.setString(21, donationsSb.toString());
            ps.setLong(22, guild.getCreatedAt());
            ps.setInt(23, guild.canChangeSpecialization() ? 1 : 0);
            ps.setString(24, histSb.toString());
            StringBuilder trapsSb = new StringBuilder();
            if (nexus != null) {
               for (fr.esicraft.nexus.NexusTrapType trap : nexus.getActiveTraps()) {
                  trapsSb.append(trap.name()).append(";");
               }
            }

            ps.setString(25, trapsSb.toString());
            ps.executeUpdate();
            this.saveGuildMembers(guild);
            this.saveGuildClaims(guild);
         } catch (SQLException var10) {
            this.plugin.getLogger().log(Level.SEVERE, "Erreur sauvegarde guilde: " + guild.getName(), (Throwable)var10);
         }
      }
   }

   private void saveGuildMembers(Guild guild) throws SQLException {
      if (guild.isMembersDirty()) {
         String deleteSql = "DELETE FROM guild_members WHERE guild_id = ?;";

         try (PreparedStatement ps = this.connection.prepareStatement(deleteSql)) {
            ps.setString(1, guild.getId());
            ps.executeUpdate();
         }

         String insertSql = "INSERT OR REPLACE INTO guild_members (guild_id, player_uuid, role) VALUES (?, ?, ?);";

         try (PreparedStatement ps = this.connection.prepareStatement(insertSql)) {
            for (Entry<UUID, GuildRole> entry : guild.getMembers().entrySet()) {
               ps.setString(1, guild.getId());
               ps.setString(2, entry.getKey().toString());
               ps.setString(3, entry.getValue().name());
               ps.addBatch();
            }

            ps.executeBatch();
         }

         guild.setMembersDirty(false);
      }
   }

   private void saveGuildClaims(Guild guild) throws SQLException {
      if (guild.isClaimsDirty()) {
         String deleteSql = "DELETE FROM guild_claims WHERE guild_id = ?;";

         try (PreparedStatement ps = this.connection.prepareStatement(deleteSql)) {
            ps.setString(1, guild.getId());
            ps.executeUpdate();
         }

         String insertSql = "INSERT OR REPLACE INTO guild_claims (guild_id, world, chunk_x, chunk_z) VALUES (?, ?, ?, ?);";

         try (PreparedStatement ps = this.connection.prepareStatement(insertSql)) {
            for (ChunkKey ck : guild.getClaimedChunks()) {
               ps.setString(1, guild.getId());
               ps.setString(2, ck.getWorldName());
               ps.setInt(3, ck.getX());
               ps.setInt(4, ck.getZ());
               ps.addBatch();
            }

            ps.executeBatch();
         }

         guild.setClaimsDirty(false);
      }
   }

   public synchronized void deleteGuild(String guildId) {
      if (guildId != null && this.connection != null) {
         String sql = "DELETE FROM guilds WHERE id = ?;";

         try (PreparedStatement ps = this.connection.prepareStatement(sql)) {
            ps.setString(1, guildId);
            ps.executeUpdate();
         } catch (SQLException var14) {
            this.plugin.getLogger().log(Level.SEVERE, "Erreur suppression guilde: " + guildId, (Throwable)var14);
         }

         try (PreparedStatement ps = this.connection.prepareStatement("DELETE FROM guild_members WHERE guild_id = ?;")) {
            ps.setString(1, guildId);
            ps.executeUpdate();
         } catch (SQLException var12) {
            this.plugin.getLogger().log(Level.SEVERE, "Erreur suppression membres: " + guildId, (Throwable)var12);
         }

         try (PreparedStatement ps = this.connection.prepareStatement("DELETE FROM guild_claims WHERE guild_id = ?;")) {
            ps.setString(1, guildId);
            ps.executeUpdate();
         } catch (SQLException var10) {
            this.plugin.getLogger().log(Level.SEVERE, "Erreur suppression claims: " + guildId, (Throwable)var10);
         }
      }
   }

   public synchronized Map<String, Guild> loadAllGuilds() {
      Map<String, Guild> guilds = new HashMap<>();
      if (this.connection == null) {
         return guilds;
      } else {
         String sql = "SELECT * FROM guilds;";

         try (
            Statement statement = this.connection.createStatement();
            ResultSet rs = statement.executeQuery(sql);
         ) {
            while (rs.next()) {
               String id = rs.getString("id");
               String name = rs.getString("name");
               String tag = rs.getString("tag");
               UUID leaderUuid = UUID.fromString(rs.getString("leader_uuid"));
               Guild guild = new Guild(id, name, tag, leaderUuid);

               try {
                  guild.setSpecialization(GuildSpecialization.valueOf(rs.getString("specialization")));
               } catch (Exception var27) {
               }

               guild.setBankBalance(rs.getDouble("bank_balance"));
               guild.setVulnerableUntil(rs.getLong("vulnerable_until"));
               guild.setCreatedAt(rs.getLong("created_at"));

               try {
                  guild.setCanChangeSpecialization(rs.getInt("can_change_spec") == 1);
               } catch (Exception var26) {
               }

               try {
                  String histStr = rs.getString("historical_members");
                  if (histStr != null && !histStr.isEmpty()) {
                     for (String uStr : histStr.split(";")) {
                        if (!uStr.trim().isEmpty()) {
                           guild.addHistoricalMember(UUID.fromString(uStr.trim()));
                        }
                     }
                  }
               } catch (Exception var34) {
               }

               int ageLevel = rs.getInt("current_age");
               if (ageLevel >= 1) {
                  guild.setCurrentAge(AgeType.fromLevel(ageLevel));
               }

               String donationsData = rs.getString("age_donations");
               if (donationsData != null && !donationsData.isEmpty()) {
                  for (String entry : donationsData.split(";")) {
                     String[] parts = entry.split(":");
                     if (parts.length == 2) {
                        try {
                           Material m = Material.matchMaterial(parts[0]);
                           int amount = Integer.parseInt(parts[1]);
                           if (m != null) {
                              guild.addDonation(m, amount);
                           }
                        } catch (Exception var25) {
                        }
                     }
                  }
               }

               String homeWorld = rs.getString("home_world");
               if (homeWorld != null) {
                  World w = Bukkit.getWorld(homeWorld);
                  if (w != null) {
                     guild.setHomeLocation(
                        new Location(
                           w, rs.getDouble("home_x"), rs.getDouble("home_y"), rs.getDouble("home_z"), rs.getFloat("home_yaw"), rs.getFloat("home_pitch")
                        )
                     );
                  }
               }

               String nexusWorld = rs.getString("nexus_world");
               if (nexusWorld != null) {
                  World nw = Bukkit.getWorld(nexusWorld);
                  if (nw != null) {
                     Location loc = new Location(nw, rs.getDouble("nexus_x"), rs.getDouble("nexus_y"), rs.getDouble("nexus_z"));
                     int maxHealth = rs.getInt("nexus_max_health");
                     Nexus nexus = new Nexus(id, loc, maxHealth);
                     nexus.setCurrentHealth(rs.getInt("nexus_health"));

                     try {
                        String trapsStr = rs.getString("nexus_traps");
                        if (trapsStr != null && !trapsStr.isEmpty()) {
                           for (String trapName : trapsStr.split(";")) {
                              if (!trapName.trim().isEmpty()) {
                                 nexus.addTrap(fr.esicraft.nexus.NexusTrapType.valueOf(trapName.trim()));
                              }
                           }
                        }
                     } catch (Exception var40) {
                     }

                     guild.setNexus(nexus);
                  }
               }

               guilds.put(id, guild);
            }
         } catch (SQLException var37) {
            this.plugin.getLogger().log(Level.SEVERE, "Erreur chargement des guildes", (Throwable)var37);
         }

         String membersSql = "SELECT * FROM guild_members;";

         try (
            Statement statement = this.connection.createStatement();
            ResultSet rs = statement.executeQuery(membersSql);
         ) {
            while (rs.next()) {
               String guildId = rs.getString("guild_id");
               Guild guild = guilds.get(guildId);
               if (guild != null) {
                  UUID pUuid = UUID.fromString(rs.getString("player_uuid"));
                  GuildRole role = GuildRole.valueOf(rs.getString("role"));
                  guild.addMember(pUuid, role);
               }
            }
         } catch (SQLException var33) {
            this.plugin.getLogger().log(Level.SEVERE, "Erreur chargement membres de guildes", (Throwable)var33);
         }

         String claimsSql = "SELECT * FROM guild_claims;";

         try (
            Statement statement = this.connection.createStatement();
            ResultSet rs = statement.executeQuery(claimsSql);
         ) {
            while (rs.next()) {
               String guildId = rs.getString("guild_id");
               Guild guild = guilds.get(guildId);
               if (guild != null) {
                  ChunkKey ck = new ChunkKey(rs.getString("world"), rs.getInt("chunk_x"), rs.getInt("chunk_z"));
                  guild.addClaim(ck);
               }
            }
         } catch (SQLException var30) {
            this.plugin.getLogger().log(Level.SEVERE, "Erreur chargement claims de guildes", (Throwable)var30);
         }

         return guilds;
      }
   }

   public synchronized List<Entry<String, Double>> getTopBalances(int limit) {
      List<Entry<String, Double>> results = new ArrayList<>();
      if (this.connection == null) {
         return results;
      } else {
         String sql = "SELECT name, coins FROM players ORDER BY coins DESC LIMIT ?;";

         try (PreparedStatement ps = this.connection.prepareStatement(sql)) {
            ps.setInt(1, Math.max(1, limit));

            try (ResultSet rs = ps.executeQuery()) {
               while (rs.next()) {
                  String name = rs.getString("name");
                  double coins = rs.getDouble("coins");
                  if (name != null) {
                     results.add(new SimpleEntry<>(name, coins));
                  }
               }
            }
         } catch (SQLException var13) {
            this.plugin.getLogger().log(Level.SEVERE, "Erreur lors de la récupération du baltop", (Throwable)var13);
         }

         return results;
      }
   }

   public synchronized List<UUID> getTopPlayerUuids(int limit) {
      List<UUID> results = new ArrayList<>();
      if (this.connection == null) {
         return results;
      } else {
         String sql = "SELECT uuid FROM players ORDER BY coins DESC LIMIT ?;";

         try (PreparedStatement ps = this.connection.prepareStatement(sql)) {
            ps.setInt(1, Math.max(1, limit));

            try (ResultSet rs = ps.executeQuery()) {
               while (rs.next()) {
                  String uuidStr = rs.getString("uuid");
                  if (uuidStr != null) {
                     try {
                        results.add(UUID.fromString(uuidStr));
                     } catch (IllegalArgumentException var10) {
                     }
                  }
               }
            }
         } catch (SQLException var13) {
            this.plugin.getLogger().log(Level.SEVERE, "Erreur lors de la récupération du top UUIDs", (Throwable)var13);
         }

         return results;
      }
   }

   public synchronized double getTotalPlayersCoins() {
      if (this.connection == null) {
         return 0.0;
      } else {
         String sql = "SELECT SUM(coins) FROM players;";

         try {
            double var4;
            try (
               Statement statement = this.connection.createStatement();
               ResultSet rs = statement.executeQuery(sql);
            ) {
               if (!rs.next()) {
                  return 0.0;
               }

               var4 = rs.getDouble(1);
            }

            return var4;
         } catch (SQLException var10) {
            this.plugin.getLogger().log(Level.SEVERE, "Erreur lors du calcul de la masse monétaire des joueurs", (Throwable)var10);
            return 0.0;
         }
      }
   }

   public synchronized void resetAllGameData() {
      if (this.connection != null) {
         try (Statement statement = this.connection.createStatement()) {
            statement.executeUpdate("DELETE FROM guilds;");
            statement.executeUpdate("DELETE FROM guild_members;");
            statement.executeUpdate("DELETE FROM guild_claims;");
            statement.executeUpdate("DELETE FROM market_listings;");
            statement.executeUpdate("DELETE FROM server_age;");
            statement.executeUpdate("DELETE FROM raid_cooldowns;");
            statement.executeUpdate("DELETE FROM guild_stats;");
            statement.executeUpdate("DELETE FROM history;");
            for (String table : new String[]{"journal", "raid_log", "pvp_log", "pvp_events", "rivalry_reset", "guild_history", "age_log", "guild_contract", "revenge", "loans", "shared_chests", "mentoring", "achievements", "player_stats", "contract_progress", "reputation", "reputation_thanks",
               "boss_wins", "court_votes", "taverns", "nexus_tech", "freight_convoys", "freight_boxes", "sieges", "siege_history", "stock_listings", "stock_positions", "ransom_cells", "web_ballots", "web_titles"}) {
               try {
                  statement.executeUpdate("DELETE FROM " + table + ";");
               } catch (SQLException ignored) {
                  // table pas encore créée
               }
            }

            // Données gardées en clé/valeur : cosmétiques (équipés, achetés, secrets), délais de spécialisation, pics de membres, saison et championne.
            // Seules les préférences de langue des joueurs survivent à un reset.
            statement.executeUpdate("DELETE FROM kv WHERE k LIKE 'cosmetic%' OR k LIKE 'cos_aura:%' OR k LIKE 'cos_trail:%' OR k LIKE 'cos_burst:%' OR k LIKE 'cos_buy:%' OR k LIKE 'cos_title:%'"
               + " OR k LIKE 'speccd:%' OR k LIKE 'peak:%' OR k = 'season' OR k = 'season_champion' OR k LIKE 'court_%' OR k = 'boss_wins' OR k LIKE 'gamble:%' OR k LIKE 'gamble_bonus:%' OR k LIKE 'nexusmove:%';");
            statement.executeUpdate("DELETE FROM stations;");
            statement.executeUpdate("DELETE FROM rail_policy;");
            statement.executeUpdate("DELETE FROM rail_relations;");
            statement.executeUpdate("DELETE FROM treaties;");
            statement.executeUpdate("DELETE FROM treaty_grace;");
            statement.executeUpdate("UPDATE players SET race = 'NONE', guild_id = NULL, guild_role = NULL, coins = 100.0, race_changes_count = 0;");
            this.plugin.getLogger().info("Toutes les données de jeu ont été réinitialisées en base de données.");
         } catch (SQLException var6) {
            this.plugin.getLogger().log(Level.SEVERE, "Erreur lors du reset complet du jeu", (Throwable)var6);
         }
      }
   }

   /** Écriture SQL générique (contrats, statistiques, historique, kv). Les erreurs sont journalisées, jamais propagées. */
   public synchronized void exec(String sql, Object... params) {
      if (this.connection != null) {
         try (PreparedStatement ps = this.connection.prepareStatement(sql)) {
            for (int i = 0; i < params.length; i++) {
               ps.setObject(i + 1, params[i]);
            }

            ps.executeUpdate();
         } catch (SQLException e) {
            this.plugin.getLogger().log(Level.WARNING, "Erreur SQL (" + sql + ") : " + e.getMessage());
         }
      }
   }

   /** Lecture SQL générique : renvoie les lignes sous forme de tableaux de {@code columns} colonnes. */
   public List<Object[]> select(String sql, int columns, Object... params) {
      List<Object[]> rows = new java.util.ArrayList<>();
      Connection source = this.readConnection != null ? this.readConnection : this.connection;
      if (source != null) {
         synchronized (this.readLock) {
         try (PreparedStatement ps = source.prepareStatement(sql)) {
            for (int i = 0; i < params.length; i++) {
               ps.setObject(i + 1, params[i]);
            }

            try (ResultSet rs = ps.executeQuery()) {
               while (rs.next()) {
                  Object[] row = new Object[columns];
                  for (int c = 0; c < columns; c++) {
                     row[c] = rs.getObject(c + 1);
                  }

                  rows.add(row);
               }
            }
         } catch (SQLException e) {
            this.plugin.getLogger().log(Level.WARNING, "Erreur SQL (" + sql + ") : " + e.getMessage());
         }
         }
      }

      return rows;
   }

   public synchronized void saveRaidCooldown(String guildId, long lastRaidMs) {
      if (this.connection != null) {
         try (PreparedStatement ps = this.connection.prepareStatement(
            "INSERT INTO raid_cooldowns (guild_id, last_raid) VALUES (?, ?) ON CONFLICT(guild_id) DO UPDATE SET last_raid = excluded.last_raid;")) {
            ps.setString(1, guildId);
            ps.setLong(2, lastRaidMs);
            ps.executeUpdate();
         } catch (SQLException e) {
            this.plugin.getLogger().log(Level.WARNING, "Erreur sauvegarde cooldown de raid", (Throwable)e);
         }
      }
   }

   public synchronized void deleteRaidCooldown(String guildId) {
      if (this.connection != null) {
         try (PreparedStatement ps = this.connection.prepareStatement("DELETE FROM raid_cooldowns WHERE guild_id = ?;")) {
            ps.setString(1, guildId);
            ps.executeUpdate();
         } catch (SQLException e) {
            this.plugin.getLogger().log(Level.WARNING, "Erreur suppression cooldown de raid", (Throwable)e);
         }
      }
   }

   public synchronized void clearRaidCooldowns() {
      if (this.connection != null) {
         try (Statement st = this.connection.createStatement()) {
            st.executeUpdate("DELETE FROM raid_cooldowns;");
         } catch (SQLException e) {
            this.plugin.getLogger().log(Level.WARNING, "Erreur reset cooldowns de raid", (Throwable)e);
         }
      }
   }

   public synchronized Map<String, Long> loadRaidCooldowns() {
      Map<String, Long> result = new java.util.HashMap<>();
      if (this.connection != null) {
         try (Statement st = this.connection.createStatement(); ResultSet rs = st.executeQuery("SELECT guild_id, last_raid FROM raid_cooldowns;")) {
            while (rs.next()) {
               result.put(rs.getString("guild_id"), rs.getLong("last_raid"));
            }
         } catch (SQLException e) {
            this.plugin.getLogger().log(Level.WARNING, "Erreur chargement cooldowns de raid", (Throwable)e);
         }
      }

      return result;
   }

   public synchronized void updateOfflinePlayerBalance(UUID uuid, double amountToAdd) {
      if (uuid != null && this.connection != null) {
         String sql = "UPDATE players SET coins = coins + ? WHERE uuid = ?;";

         try (PreparedStatement ps = this.connection.prepareStatement(sql)) {
            ps.setDouble(1, amountToAdd);
            ps.setString(2, uuid.toString());
            ps.executeUpdate();
         } catch (SQLException var10) {
            this.plugin.getLogger().log(Level.SEVERE, "Erreur mise à jour solde offline: " + uuid, (Throwable)var10);
         }
      }
   }

   public synchronized void saveMarketListing(MarketListing listing) {
      if (listing != null && this.connection != null) {
         String sql = "    INSERT INTO market_listings (id, seller_uuid, seller_name, item_data, price, created_at)\n    VALUES (?, ?, ?, ?, ?, ?)\n    ON CONFLICT(id) DO UPDATE SET\n        price = excluded.price;\n";

         try (PreparedStatement ps = this.connection.prepareStatement(sql)) {
            ps.setString(1, listing.getId());
            ps.setString(2, listing.getSellerUuid().toString());
            ps.setString(3, listing.getSellerName());
            ps.setString(4, itemToBase64(listing.getItemStack()));
            ps.setDouble(5, listing.getPrice());
            ps.setLong(6, listing.getCreatedAt());
            ps.executeUpdate();
         } catch (SQLException var8) {
            this.plugin.getLogger().log(Level.SEVERE, "Erreur sauvegarde listing marché: " + listing.getId(), (Throwable)var8);
         }
      }
   }

   public synchronized void deleteMarketListing(String listingId) {
      if (listingId != null && this.connection != null) {
         String sql = "DELETE FROM market_listings WHERE id = ?;";

         try (PreparedStatement ps = this.connection.prepareStatement(sql)) {
            ps.setString(1, listingId);
            ps.executeUpdate();
         } catch (SQLException var8) {
            this.plugin.getLogger().log(Level.SEVERE, "Erreur suppression listing marché: " + listingId, (Throwable)var8);
         }
      }
   }

   public synchronized Map<String, MarketListing> loadAllMarketListings() {
      Map<String, MarketListing> listings = new HashMap<>();
      if (this.connection == null) {
         return listings;
      } else {
         String sql = "SELECT * FROM market_listings;";

         try (
            Statement statement = this.connection.createStatement();
            ResultSet rs = statement.executeQuery(sql);
         ) {
            while (rs.next()) {
               String id = rs.getString("id");
               UUID sellerUuid = UUID.fromString(rs.getString("seller_uuid"));
               String sellerName = rs.getString("seller_name");
               String itemData = rs.getString("item_data");
               double price = rs.getDouble("price");
               long createdAt = rs.getLong("created_at");
               ItemStack item = itemFromBase64(itemData);
               if (item != null) {
                  listings.put(id, new MarketListing(id, sellerUuid, sellerName, item, price, createdAt));
               }
            }
         } catch (SQLException var18) {
            this.plugin.getLogger().log(Level.SEVERE, "Erreur chargement des listings du marché", (Throwable)var18);
         }

         return listings;
      }
   }

   public static String itemToBase64(ItemStack item) {
      if (item == null) {
         return "";
      } else {
         try {
            YamlConfiguration config = new YamlConfiguration();
            config.set("item", item);
            return Base64.getEncoder().encodeToString(config.saveToString().getBytes(StandardCharsets.UTF_8));
         } catch (Exception var2) {
            return "";
         }
      }
   }

   public static ItemStack itemFromBase64(String data) {
      if (data != null && !data.isEmpty()) {
         try {
            String yaml = new String(Base64.getDecoder().decode(data), StandardCharsets.UTF_8);
            YamlConfiguration config = new YamlConfiguration();
            config.loadFromString(yaml);
            return config.getItemStack("item");
         } catch (Exception var11) {
            try {
               ItemStack var4;
               try (ByteArrayInputStream inputStream = new ByteArrayInputStream(Base64.getDecoder().decode(data))) {
                  BukkitObjectInputStream dataInput = new BukkitObjectInputStream(inputStream);

                  try {
                     var4 = (ItemStack)dataInput.readObject();
                  } catch (Throwable var8) {
                     try {
                        dataInput.close();
                     } catch (Throwable var7) {
                        var8.addSuppressed(var7);
                     }

                     throw var8;
                  }

                  dataInput.close();
               }

               return var4;
            } catch (Exception var10) {
               return null;
            }
         }
      } else {
         return null;
      }
   }

   public synchronized void close() {
      synchronized (this.readLock) {
         if (this.readConnection != null) {
            try {
               this.readConnection.close();
            } catch (SQLException ignored) {
            }

            this.readConnection = null;
         }
      }

      if (this.connection != null) {
         try {
            this.connection.close();
         } catch (SQLException var2) {
         }
      }
   }
}
