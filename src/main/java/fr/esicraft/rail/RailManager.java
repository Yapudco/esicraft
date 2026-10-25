package fr.esicraft.rail;

import fr.esicraft.EsicraftPlugin;
import fr.esicraft.data.PlayerData;
import fr.esicraft.guild.Guild;
import fr.esicraft.util.ChatUtil;
import fr.esicraft.util.ChunkKey;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.World;
import org.bukkit.entity.Player;

/**
 * Réseau ferroviaire inter-guildes : gares (panneaux [Gare]), péages et relations entre guildes.
 * Un voyage téléporte le joueur d'une gare à une autre gare reliée par un chemin de rails continu ;
 * il paie le tarif de la gare d'arrivée + le péage de chaque guilde dont le territoire est traversé.
 */
public class RailManager {
   public record Quote(double fare, Map<Guild, Double> tolls, Map<Guild, Boolean> freePass, double total) {
   }

   private static final long RESULT_TTL_MS = 60000L;
   private static final int MAX_CONCURRENT_SEARCHES = 3;

   private final EsicraftPlugin plugin;
   private final Map<String, Station> stations = new ConcurrentHashMap<>();
   private final Map<String, Station> bySign = new ConcurrentHashMap<>();
   private final Map<String, Double> tolls = new ConcurrentHashMap<>();
   private final Map<String, Map<String, Relation>> relations = new ConcurrentHashMap<>();
   private final Map<String, RailSearch.Result> cache = new ConcurrentHashMap<>();
   private final Map<UUID, Long> searchCooldown = new ConcurrentHashMap<>();
   private int activeSearches;

   public RailManager(EsicraftPlugin plugin) {
      this.plugin = plugin;
   }

   // ------------------------------------------------------------------ persistance

   public void load() {
      for (Object[] row : this.plugin.getDataManager().getStorage().select(
         "SELECT id, name, guild_id, world, sx, sy, sz, rx, ry, rz FROM stations;", 10)) {
         Station s = new Station((String)row[0], (String)row[1], (String)row[2], (String)row[3],
            ((Number)row[4]).intValue(), ((Number)row[5]).intValue(), ((Number)row[6]).intValue(),
            ((Number)row[7]).intValue(), ((Number)row[8]).intValue(), ((Number)row[9]).intValue());
         this.index(s);
      }

      for (Object[] row : this.plugin.getDataManager().getStorage().select("SELECT guild_id, toll FROM rail_policy;", 2)) {
         this.tolls.put((String)row[0], ((Number)row[1]).doubleValue());
      }

      for (Object[] row : this.plugin.getDataManager().getStorage().select("SELECT owner_id, other_id, relation FROM rail_relations;", 3)) {
         try {
            this.relations.computeIfAbsent((String)row[0], k -> new ConcurrentHashMap<>()).put((String)row[1], Relation.valueOf((String)row[2]));
         } catch (IllegalArgumentException ignored) {
         }
      }

      this.plugin.getLogger().info("Réseau ferroviaire : " + this.stations.size() + " gare(s) chargée(s).");
   }

   public void clearAll() {
      this.stations.clear();
      this.bySign.clear();
      this.tolls.clear();
      this.relations.clear();
      this.cache.clear();
   }

   private void index(Station s) {
      this.stations.put(s.getId(), s);
      this.bySign.put(s.signKey(), s);
   }

   private void async(Runnable r) {
      if (this.plugin.isEnabled()) {
         Bukkit.getScheduler().runTaskAsynchronously(this.plugin, r);
      } else {
         r.run();
      }
   }

   // ------------------------------------------------------------------ gares

   public Station createStation(String name, Guild guild, Location sign, Location rail) {
      Station s = new Station(UUID.randomUUID().toString(), name, guild.getId(), sign.getWorld().getName(),
         sign.getBlockX(), sign.getBlockY(), sign.getBlockZ(), rail.getBlockX(), rail.getBlockY(), rail.getBlockZ());
      this.index(s);
      this.cache.clear();
      this.async(() -> this.plugin.getDataManager().getStorage().exec(
         "INSERT INTO stations (id, name, guild_id, world, sx, sy, sz, rx, ry, rz) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?);",
         s.getId(), s.getName(), s.getGuildId(), s.getWorld(), s.getSignX(), s.getSignY(), s.getSignZ(), s.getRailX(), s.getRailY(), s.getRailZ()));
      return s;
   }

   public void removeStation(Station s) {
      this.stations.remove(s.getId());
      this.bySign.remove(s.signKey());
      this.cache.clear();
      this.async(() -> this.plugin.getDataManager().getStorage().exec("DELETE FROM stations WHERE id = ?;", s.getId()));
   }

   public Station stationAtSign(org.bukkit.block.Block block) {
      return this.bySign.get(block.getWorld().getName() + ":" + RailSearch.pack(block.getX(), block.getY(), block.getZ()));
   }

   public boolean nameTaken(String name) {
      for (Station s : this.stations.values()) {
         if (s.getName().equalsIgnoreCase(name)) {
            return true;
         }
      }

      return false;
   }

   public List<Station> stationsOf(String guildId) {
      List<Station> list = new ArrayList<>();
      for (Station s : this.stations.values()) {
         if (s.getGuildId().equals(guildId)) {
            list.add(s);
         }
      }

      list.sort((a, b) -> a.getName().compareToIgnoreCase(b.getName()));
      return list;
   }

   public List<Station> stationsInWorld(String world) {
      List<Station> list = new ArrayList<>();
      for (Station s : this.stations.values()) {
         if (s.getWorld().equals(world)) {
            list.add(s);
         }
      }

      return list;
   }

   public Station nearestStation(Location loc, double radius) {
      Station best = null;
      double bestDist = radius * radius;
      for (Station s : this.stations.values()) {
         if (!s.getWorld().equals(loc.getWorld().getName())) {
            continue;
         }

         double dx = s.getSignX() + 0.5 - loc.getX();
         double dy = s.getSignY() + 0.5 - loc.getY();
         double dz = s.getSignZ() + 0.5 - loc.getZ();
         double d = dx * dx + dy * dy + dz * dz;
         if (d <= bestDist) {
            bestDist = d;
            best = s;
         }
      }

      return best;
   }

   public Location arrivalOf(Station s) {
      World w = Bukkit.getWorld(s.getWorld());
      return w == null ? null : new Location(w, s.getRailX() + 0.5, s.getRailY() + 0.2, s.getRailZ() + 0.5);
   }

   public Guild guildOf(Station s) {
      return this.plugin.getGuildManager().getGuildById(s.getGuildId());
   }

   // ------------------------------------------------------------------ péages et relations

   public double getToll(String guildId) {
      Double t = this.tolls.get(guildId);
      return t != null ? t : this.plugin.getConfig().getDouble("rail.default-toll", 2.0);
   }

   public void setToll(String guildId, double toll) {
      double clamped = Math.max(0.0, Math.min(this.plugin.getConfig().getDouble("rail.max-toll", 500.0), toll));
      this.tolls.put(guildId, clamped);
      this.cache.clear();
      this.async(() -> this.plugin.getDataManager().getStorage().exec(
         "INSERT INTO rail_policy (guild_id, toll) VALUES (?, ?) ON CONFLICT(guild_id) DO UPDATE SET toll = excluded.toll;", guildId, clamped));
   }

   public void invalidateCache() {
      this.cache.clear();
   }

   /** Vrai si un traité (alliance / vassalité) impose le passage gratuit : la relation manuelle est alors verrouillée. */
   public boolean isTreatyLocked(String ownerGuildId, String otherGuildId) {
      return this.plugin.getTreatyManager() != null && this.plugin.getTreatyManager().areFriends(ownerGuildId, otherGuildId);
   }

   public Relation getRelation(String ownerGuildId, String otherGuildId) {
      if (otherGuildId == null) {
         return Relation.NORMAL;
      }

      if (this.plugin.getTreatyManager() != null) {
         Relation forced = this.plugin.getTreatyManager().railOverride(ownerGuildId, otherGuildId);
         if (forced != null) {
            return forced;
         }
      }

      Map<String, Relation> map = this.relations.get(ownerGuildId);
      Relation r = map != null ? map.get(otherGuildId) : null;
      return r != null ? r : Relation.NORMAL;
   }

   public void setRelation(String ownerGuildId, String otherGuildId, Relation relation) {
      this.cache.clear();
      if (relation == Relation.NORMAL) {
         Map<String, Relation> map = this.relations.get(ownerGuildId);
         if (map != null) {
            map.remove(otherGuildId);
         }

         this.async(() -> this.plugin.getDataManager().getStorage().exec("DELETE FROM rail_relations WHERE owner_id = ? AND other_id = ?;", ownerGuildId, otherGuildId));
      } else {
         this.relations.computeIfAbsent(ownerGuildId, k -> new ConcurrentHashMap<>()).put(otherGuildId, relation);
         this.async(() -> this.plugin.getDataManager().getStorage().exec(
            "INSERT INTO rail_relations (owner_id, other_id, relation) VALUES (?, ?, ?) "
               + "ON CONFLICT(owner_id, other_id) DO UPDATE SET relation = excluded.relation;", ownerGuildId, otherGuildId, relation.name()));
      }
   }

   public boolean isBanned(Guild owner, Guild traveler) {
      return traveler != null && !owner.getId().equals(traveler.getId()) && this.getRelation(owner.getId(), traveler.getId()) == Relation.BANNED;
   }

   public Guild ownerOfChunk(String world, int cx, int cz) {
      return this.plugin.getGuildManager().getGuildByChunk(new ChunkKey(world, cx, cz));
   }

   // ------------------------------------------------------------------ recherche et voyage

   public enum SearchStatus { STARTED, CACHED, COOLDOWN, BUSY }

   /** Lance (ou réutilise) la recherche de lignes depuis une gare. Le callback est appelé sur le thread principal. */
   public SearchStatus search(Player player, Station origin, Consumer<RailSearch.Result> callback) {
      Guild traveler = this.plugin.getGuildManager().getGuildByPlayer(player.getUniqueId());
      String cacheKey = origin.getId() + "|" + (traveler != null ? traveler.getId() : "-");
      RailSearch.Result cached = this.cache.get(cacheKey);
      if (cached != null && System.currentTimeMillis() - cached.finishedAt() < RESULT_TTL_MS) {
         callback.accept(cached);
         return SearchStatus.CACHED;
      }

      long now = System.currentTimeMillis();
      Long last = this.searchCooldown.get(player.getUniqueId());
      if (last != null && now - last < 4000L) {
         return SearchStatus.COOLDOWN;
      }

      if (this.activeSearches >= MAX_CONCURRENT_SEARCHES) {
         return SearchStatus.BUSY;
      }

      World world = Bukkit.getWorld(origin.getWorld());
      if (world == null) {
         return SearchStatus.BUSY;
      }

      this.searchCooldown.put(player.getUniqueId(), now);
      this.activeSearches++;
      new RailSearch(this.plugin, this, world, origin, traveler, result -> {
         this.activeSearches--;
         this.cache.put(cacheKey, result);
         callback.accept(result);
      }).begin();
      return SearchStatus.STARTED;
   }

   /** Calcule le prix d'un trajet avec les péages et relations actuels (renvoie null si une guilde banni désormais le voyageur). */
   public Quote quote(Player player, RailSearch.Reached trip) {
      Guild traveler = this.plugin.getGuildManager().getGuildByPlayer(player.getUniqueId());
      Map<Guild, Double> tollMap = new LinkedHashMap<>();
      Map<Guild, Boolean> free = new LinkedHashMap<>();
      for (ChunkKey chunk : trip.chunks()) {
         Guild owner = this.plugin.getGuildManager().getGuildByChunk(chunk);
         if (owner == null || tollMap.containsKey(owner) || free.containsKey(owner)) {
            continue;
         }

         if (traveler != null && owner.getId().equals(traveler.getId())) {
            continue;
         }

         if (this.isBanned(owner, traveler)) {
            return null;
         }

         if (traveler != null && this.getRelation(owner.getId(), traveler.getId()) == Relation.FREE) {
            free.put(owner, true);
         } else {
            double toll = this.getToll(owner.getId());
            if (toll > 0.0) {
               tollMap.put(owner, toll);
            }
         }
      }

      double fare = this.plugin.getConfig().getDouble("rail.base-fare", 1.0);
      double total = fare;
      for (double t : tollMap.values()) {
         total += t;
      }

      return new Quote(fare, tollMap, free, total);
   }

   /** Débite le joueur, verse péages et tarif aux banques de guilde, puis téléporte. */
   public void travel(Player player, RailSearch.Reached trip) {
      Station dest = trip.station();
      Location arrival = this.arrivalOf(dest);
      if (arrival == null || !this.stations.containsKey(dest.getId())) {
         ChatUtil.sendMessage(player, "&cCette gare n'existe plus.");
         return;
      }

      RailManager.Quote quote = this.quote(player, trip);
      if (quote == null) {
         ChatUtil.sendMessage(player, "&c❌ Une guilde traversée par cette ligne vous interdit désormais le passage.");
         return;
      }

      PlayerData data = this.plugin.getDataManager().getPlayerData(player.getUniqueId());
      if (data == null || !data.removeCoins(quote.total())) {
         ChatUtil.sendMessage(player, "&c❌ Il vous faut &6" + String.format(Locale.ROOT, "%.2f", quote.total()) + " pièces &cpour ce trajet.");
         ChatUtil.playSound(player, Sound.ENTITY_VILLAGER_NO, 1.0F, 1.0F);
         return;
      }

      Location from = player.getLocation();
      if (player.isInsideVehicle()) {
         player.leaveVehicle();
      }

      arrival.setYaw(from.getYaw());
      arrival.setPitch(from.getPitch());
      player.teleportAsync(arrival).thenAccept(ok -> Bukkit.getScheduler().runTask(this.plugin, () -> {
         if (ok) {
         for (Map.Entry<Guild, Double> toll : quote.tolls().entrySet()) {
            toll.getKey().depositBank(toll.getValue());
            this.plugin.getDataManager().saveGuild(toll.getKey());
            ChatUtil.broadcastGuild(toll.getKey(), "&6🚆 Péage perçu : &e+" + String.format(Locale.ROOT, "%.2f", toll.getValue()) + " pièces &7(passage de " + player.getName() + ")");
         }

         Guild destGuild = this.guildOf(dest);
         if (destGuild != null) {
            destGuild.depositBank(quote.fare());
            this.plugin.getDataManager().saveGuild(destGuild);
         }

            ChatUtil.sendMessage(player, "&a🚆 Bon voyage ! Arrivée à &e" + dest.getName() + " &7(-" + String.format(Locale.ROOT, "%.2f", quote.total()) + " pièces)");
            player.getWorld().spawnParticle(Particle.CLOUD, arrival, 25, 0.6, 0.3, 0.6, 0.02);
            ChatUtil.playSound(player, Sound.ENTITY_MINECART_RIDING, 1.0F, 1.2F);
         } else {
            data.addCoins(quote.total());
            ChatUtil.sendMessage(player, "&cLe voyage a échoué, vous avez été remboursé.");
         }
      }));
   }
}
