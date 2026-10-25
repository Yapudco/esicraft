package fr.esicraft.data;

import fr.esicraft.EsicraftPlugin;
import fr.esicraft.guild.Guild;
import fr.esicraft.market.MarketListing;
import fr.esicraft.race.Race;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.Map.Entry;
import java.util.concurrent.ConcurrentHashMap;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.potion.PotionEffect;

public class DataManager {
   private final EsicraftPlugin plugin;
   private final SQLiteStorage storage;
   private final Map<UUID, PlayerData> players = new ConcurrentHashMap<>();
   private volatile double cachedServerWealth = 0.0;
   // Toutes les écritures de comptes joueurs passent par UN SEUL thread : l'ordre sauvegarde-quit / crédit hors-ligne est garanti (pas de perte d'argent).
   private final java.util.concurrent.ExecutorService playerIo = java.util.concurrent.Executors.newSingleThreadExecutor(r -> {
      Thread t = new Thread(r, "esicraft-player-io");
      t.setDaemon(false);
      return t;
   });

   public DataManager(EsicraftPlugin plugin) {
      this.plugin = plugin;
      this.storage = new SQLiteStorage(plugin);
   }

   public void init() {
      this.storage.init();
      Map<String, Guild> loadedGuilds = this.storage.loadAllGuilds();

      for (Guild g : loadedGuilds.values()) {
         this.plugin.getGuildManager().registerGuild(g);
      }

      Map<String, MarketListing> loadedListings = this.storage.loadAllMarketListings();

      for (MarketListing listing : loadedListings.values()) {
         this.plugin.getMarketManager().registerListing(listing);
      }

      this.plugin.getRaidManager().loadCooldowns();
      this.plugin.getLogger().info("Marché initialisé : " + loadedListings.size() + " annonce(s) chargée(s).");
      int interval = this.plugin.getConfig().getInt("storage.save-interval-seconds", 180) * 20;
      Bukkit.getScheduler().runTaskTimerAsynchronously(this.plugin, this::saveAll, (long)interval, (long)interval);
   }

   public PlayerData getPlayerData(UUID uuid) {
      return this.players.get(uuid);
   }

   public PlayerData preloadPlayer(UUID uuid, String name) {
      PlayerData data = this.storage.loadPlayerData(uuid, name);
      this.players.put(uuid, data);
      return data;
   }

   public PlayerData loadPlayer(Player player) {
      PlayerData data = this.players.get(player.getUniqueId());
      if (data == null) {
         data = this.storage.loadPlayerData(player.getUniqueId(), player.getName());
         this.players.put(player.getUniqueId(), data);
      }

      return data;
   }

   public void unloadPlayer(UUID uuid) {
      PlayerData data = this.players.remove(uuid);
      if (data != null) {
         this.playerIo.execute(() -> this.storage.savePlayerData(data));
      }
   }

   public void savePlayer(PlayerData data) {
      if (data != null) {
         this.playerIo.execute(() -> this.storage.savePlayerData(data));
      }
   }

   public void saveGuild(Guild guild) {
      if (guild != null) {
         Bukkit.getScheduler().runTaskAsynchronously(this.plugin, () -> this.storage.saveGuild(guild));
      }
   }

   public void deleteGuild(String guildId) {
      Bukkit.getScheduler().runTaskAsynchronously(this.plugin, () -> this.storage.deleteGuild(guildId));
   }

   public void saveMarketListing(MarketListing listing) {
      if (listing != null) {
         Bukkit.getScheduler().runTaskAsynchronously(this.plugin, () -> this.storage.saveMarketListing(listing));
      }
   }

   public void deleteMarketListing(String listingId) {
      if (listingId != null) {
         Bukkit.getScheduler().runTaskAsynchronously(this.plugin, () -> this.storage.deleteMarketListing(listingId));
      }
   }

   public void deleteMarketListingSync(String listingId) {
      if (listingId != null) {
         this.storage.deleteMarketListing(listingId);
      }
   }

   public void creditPlayer(UUID uuid, double amount) {
      PlayerData onlineData = this.players.get(uuid);
      if (onlineData != null) {
         onlineData.addCoins(amount);
         this.savePlayer(onlineData);
      } else {
         this.playerIo.execute(() -> this.storage.updateOfflinePlayerBalance(uuid, amount));
      }
   }

   public SQLiteStorage getStorage() {
      return this.storage;
   }

   public List<Entry<String, Double>> getTopBalances(int limit) {
      return this.storage.getTopBalances(limit);
   }

   public List<UUID> getTopPlayerUuids(int limit) {
      return this.storage.getTopPlayerUuids(limit);
   }

   public double getTotalServerWealth() {
      return this.cachedServerWealth;
   }

   private void refreshServerWealthCache() {
      double playersCoins = this.storage.getTotalPlayersCoins();
      double guildsCoins = 0.0;

      for (Guild guild : this.plugin.getGuildManager().getAllGuilds()) {
         guildsCoins += guild.getBankBalance();
      }

      this.cachedServerWealth = playersCoins + guildsCoins;
   }

   public void resetGame() {
      for (PlayerData pData : this.players.values()) {
         pData.setCoins(100.0);
         pData.setRace(Race.NONE);
         pData.setGuildId(null);
         pData.setGuildRole(null);
         pData.resetRaceChangesCount();
      }

      for (Guild guild : this.plugin.getGuildManager().getAllGuilds()) {
         if (guild.hasNexus() && guild.getNexus().getLocation() != null) {
            Location loc = guild.getNexus().getLocation();
            if (loc.getWorld() != null && loc.getBlock().getType() == Material.CRYING_OBSIDIAN) {
               loc.getBlock().setType(Material.AIR);
            }
         }
      }

      for (Player p : Bukkit.getOnlinePlayers()) {
         for (PotionEffect effect : p.getActivePotionEffects()) {
            p.removePotionEffect(effect.getType());
         }
      }

      this.plugin.getNexusManager().removeAllHolograms();
      this.plugin.getNexusManager().clearIndex();
      this.plugin.getGuildManager().clearAll();
      this.plugin.getMarketManager().clearAll();
      this.plugin.getRailManager().clearAll();
      this.plugin.getTreatyManager().clearAll();
      this.plugin.getAntiFarmManager().resetStats();
      this.storage.resetAllGameData();
   }

   public void saveAll() {
      this.storage.runInTransaction(() -> {
         for (PlayerData data : this.players.values()) {
            this.storage.savePlayerData(data);
         }

         for (Guild guild : this.plugin.getGuildManager().getAllGuilds()) {
            this.storage.saveGuild(guild);
         }
      });
      this.refreshServerWealthCache();
   }

   public void close() {
      this.saveAll();
      this.playerIo.shutdown();

      try {
         if (!this.playerIo.awaitTermination(15L, java.util.concurrent.TimeUnit.SECONDS)) {
            this.plugin.getLogger().warning("Écritures joueurs encore en attente après 15 s.");
         }
      } catch (InterruptedException var2) {
         Thread.currentThread().interrupt();
      }

      this.storage.close();
   }
}
