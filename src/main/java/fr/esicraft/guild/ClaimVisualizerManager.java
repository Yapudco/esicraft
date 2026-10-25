package fr.esicraft.guild;

import fr.esicraft.EsicraftPlugin;
import fr.esicraft.util.ChatUtil;
import fr.esicraft.util.ChunkKey;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import org.bukkit.Color;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.World;
import org.bukkit.Particle.DustOptions;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scheduler.BukkitTask;

public class ClaimVisualizerManager {
   private final EsicraftPlugin plugin;
   private final Map<UUID, BukkitTask> activeVisualizers = new ConcurrentHashMap<>();
   private static final DustOptions DUST_OWN = new DustOptions(Color.fromRGB(50, 255, 50), 1.3F);
   private static final DustOptions DUST_ENEMY = new DustOptions(Color.fromRGB(255, 40, 40), 1.3F);

   public ClaimVisualizerManager(EsicraftPlugin plugin) {
      this.plugin = plugin;
   }

   public void toggleVisualizer(Player player) {
      UUID uuid = player.getUniqueId();
      if (this.activeVisualizers.containsKey(uuid)) {
         this.stopVisualizer(player);
         ChatUtil.sendActionbar(player, "&c✖ Surbrillance des claims désactivée.");
         ChatUtil.sendMessage(player, "&cSurbrillance des chunks claimés désactivée.");
      } else {
         this.startVisualizer(player);
         ChatUtil.sendActionbar(player, "&a✨ Surbrillance des claims activée !");
         ChatUtil.sendMessage(player, "&a✔ Bordures des chunks claimés affichées en surbrillance !");
         ChatUtil.sendMessage(player, "&7• &aVert &7= Votre Guilde  |  &cRouge &7= Guildes Ennemies");
         ChatUtil.sendMessage(player, "&7(Tapez à nouveau &e/guild claim show &7pour désactiver)");
      }
   }

   public void startVisualizer(final Player player) {
      this.stopVisualizer(player);
      BukkitTask task = (new BukkitRunnable() {
         int ticks = 0;

         public void run() {
            if (!player.isOnline()) {
               this.cancel();
               ClaimVisualizerManager.this.activeVisualizers.remove(player.getUniqueId());
            } else {
               this.ticks++;
               if (this.ticks > 120) {
                  this.cancel();
                  ClaimVisualizerManager.this.activeVisualizers.remove(player.getUniqueId());
                  ChatUtil.sendActionbar(player, "&7Surbrillance des claims terminée.");
               } else {
                  ClaimVisualizerManager.this.renderNearbyClaims(player);
               }
            }
         }
      }).runTaskTimer(this.plugin, 0L, 10L);
      this.activeVisualizers.put(player.getUniqueId(), task);
   }

   public void stopVisualizer(Player player) {
      BukkitTask task = this.activeVisualizers.remove(player.getUniqueId());
      if (task != null) {
         task.cancel();
      }
   }

   private void renderNearbyClaims(Player player) {
      Location pLoc = player.getLocation();
      World world = pLoc.getWorld();
      if (world != null) {
         int playerChunkX = pLoc.getBlockX() >> 4;
         int playerChunkZ = pLoc.getBlockZ() >> 4;
         double pY = pLoc.getY();
         Guild playerGuild = this.plugin.getGuildManager().getGuildByPlayer(player.getUniqueId());

         for (int cx = playerChunkX - 3; cx <= playerChunkX + 3; cx++) {
            for (int cz = playerChunkZ - 3; cz <= playerChunkZ + 3; cz++) {
               ChunkKey key = new ChunkKey(world.getName(), cx, cz);
               Guild chunkOwner = this.plugin.getGuildManager().getGuildByChunk(key);
               if (chunkOwner != null) {
                  boolean isOwnGuild = playerGuild != null && playerGuild.getId().equals(chunkOwner.getId());
                  this.drawChunkBorders(player, world, cx, cz, pY, isOwnGuild);
               }
            }
         }
      }
   }

   private void drawChunkBorders(Player player, World world, int chunkX, int chunkZ, double baseY, boolean isOwn) {
      int minX = chunkX << 4;
      int maxX = minX + 16;
      int minZ = chunkZ << 4;
      int maxZ = minZ + 16;
      DustOptions dust = isOwn ? DUST_OWN : DUST_ENEMY;
      double step = 2.0;

      for (double x = (double)minX; x <= (double)maxX; x += step) {
         this.spawnParticle(player, world, dust, x, baseY + 0.2, (double)minZ);
         this.spawnParticle(player, world, dust, x, baseY + 0.2, (double)maxZ);
         this.spawnParticle(player, world, dust, x, baseY + 1.2, (double)minZ);
         this.spawnParticle(player, world, dust, x, baseY + 1.2, (double)maxZ);
      }

      for (double z = (double)minZ; z <= (double)maxZ; z += step) {
         this.spawnParticle(player, world, dust, (double)minX, baseY + 0.2, z);
         this.spawnParticle(player, world, dust, (double)maxX, baseY + 0.2, z);
         this.spawnParticle(player, world, dust, (double)minX, baseY + 1.2, z);
         this.spawnParticle(player, world, dust, (double)maxX, baseY + 1.2, z);
      }

      for (double y = baseY - 2.0; y <= baseY + 6.0; y++) {
         this.spawnParticle(player, world, dust, (double)minX, y, (double)minZ);
         this.spawnParticle(player, world, dust, (double)maxX, y, (double)minZ);
         this.spawnParticle(player, world, dust, (double)minX, y, (double)maxZ);
         this.spawnParticle(player, world, dust, (double)maxX, y, (double)maxZ);
      }
   }

   private void spawnParticle(Player player, World world, DustOptions dust, double x, double y, double z) {
      player.spawnParticle(Particle.REDSTONE, x, y, z, 1, 0.0, 0.0, 0.0, 0.0, dust);
   }
}
