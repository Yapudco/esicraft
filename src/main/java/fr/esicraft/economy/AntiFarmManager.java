package fr.esicraft.economy;

import fr.esicraft.EsicraftPlugin;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import org.bukkit.entity.EntityType;

public class AntiFarmManager {
   private final EsicraftPlugin plugin;
   private double rectifyFactor = 1.0;
   private final Map<UUID, Map<EntityType, Integer>> playerMobKillCounts = new ConcurrentHashMap<>();
   private final Map<UUID, AtomicInteger> playerTotalKillsCache = new ConcurrentHashMap<>();

   public AntiFarmManager(EsicraftPlugin plugin) {
      this.plugin = plugin;
      this.rectifyFactor = plugin.getConfig().getDouble("economy.rectify-factor", 1.0);
   }

   public double getRectifyFactor() {
      return this.rectifyFactor;
   }

   public void setRectifyFactor(double factor) {
      this.rectifyFactor = Math.max(0.0, factor);
      this.plugin.getConfig().set("economy.rectify-factor", this.rectifyFactor);
      this.plugin.saveConfig();
   }

   public void registerKill(UUID playerUuid, EntityType type) {
      if (playerUuid != null && type != null) {
         this.playerMobKillCounts.computeIfAbsent(playerUuid, k -> new ConcurrentHashMap<>()).merge(type, 1, Integer::sum);
         this.playerTotalKillsCache.computeIfAbsent(playerUuid, k -> new AtomicInteger(0)).incrementAndGet();
      }
   }

   private final Map<UUID, double[]> hourly = new ConcurrentHashMap<>();

   /** Plafond de primes de monstres par heure et par joueur (économie.hourly-bounty-cap) : renvoie la part encore autorisée. */
   public double capHourlyEarning(UUID playerUuid, double amount) {
      double cap = this.plugin.getConfig().getDouble("economy.hourly-bounty-cap", 4000.0);
      if (cap <= 0.0 || playerUuid == null) {
         return amount;
      }

      long now = System.currentTimeMillis();
      double[] w = this.hourly.computeIfAbsent(playerUuid, k -> new double[]{now, 0.0});
      synchronized (w) {
         if (now - (long)w[0] >= 3600000L) {
            w[0] = now;
            w[1] = 0.0;
         }

         double allowed = Math.max(0.0, Math.min(amount, cap - w[1]));
         w[1] += allowed;
         return allowed;
      }
   }

   public int getPlayerTotalKills(UUID playerUuid) {
      AtomicInteger cached = this.playerTotalKillsCache.get(playerUuid);
      return cached != null ? cached.get() : 0;
   }

   public int getPlayerKills(UUID playerUuid, EntityType type) {
      Map<EntityType, Integer> counts = this.playerMobKillCounts.get(playerUuid);
      return counts != null ? counts.getOrDefault(type, 0) : 0;
   }

   public double getPlayerShare(UUID playerUuid, EntityType type) {
      int total = this.getPlayerTotalKills(playerUuid);
      return total <= 0 ? 0.0 : (double)this.getPlayerKills(playerUuid, type) / (double)total;
   }

   public double getMultiplier(UUID playerUuid, EntityType type) {
      if (this.rectifyFactor <= 0.0) {
         return 1.0;
      } else {
         int total = this.getPlayerTotalKills(playerUuid);
         if (total < 10) {
            return 1.0;
         } else {
            double share = this.getPlayerShare(playerUuid, type);
            double mult = 1.0 - share * this.rectifyFactor;
            return Math.max(0.005, mult);
         }
      }
   }

   public void resetPlayerStats(UUID playerUuid) {
      this.playerMobKillCounts.remove(playerUuid);
      this.playerTotalKillsCache.remove(playerUuid);
   }

   public void resetAllStats() {
      this.playerMobKillCounts.clear();
      this.playerTotalKillsCache.clear();
   }

   public void resetStats() {
      this.resetAllStats();
   }

   public Map<EntityType, Integer> getPlayerMobCounts(UUID playerUuid) {
      return this.playerMobKillCounts.getOrDefault(playerUuid, Map.of());
   }

   public int getServerTotalKills() {
      int total = 0;

      for (AtomicInteger count : this.playerTotalKillsCache.values()) {
         total += count.get();
      }

      return total;
   }
}
