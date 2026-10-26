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

/**
 * Surbrillance des claims : seuls les <b>bords extérieurs</b> d'un territoire sont dessinés (pas les limites entre deux chunks d'une même
 * guilde), un point tous les 4 blocs, dans un rayon de 2 chunks, une fois par seconde, pendant une minute. Quelques dizaines de
 * particules au lieu de plusieurs milliers : le client ne rame plus et la bande passante reste raisonnable.
 */
public class ClaimVisualizerManager {
   private static final int RADIUS = 2;
   private static final int STEP = 4;
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
         ChatUtil.sendMessage(player, "&a✔ Bordures des territoires proches affichées en surbrillance !");
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
               if (this.ticks > 60) {
                  this.cancel();
                  ClaimVisualizerManager.this.activeVisualizers.remove(player.getUniqueId());
                  ChatUtil.sendActionbar(player, "&7Surbrillance des claims terminée.");
               } else {
                  ClaimVisualizerManager.this.renderNearbyClaims(player);
               }
            }
         }
      }).runTaskTimer(this.plugin, 0L, 20L);
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
      if (world == null) {
         return;
      }

      int px = pLoc.getBlockX() >> 4;
      int pz = pLoc.getBlockZ() >> 4;
      double y = pLoc.getY() + 1.0;
      Guild playerGuild = this.plugin.getGuildManager().getGuildByPlayer(player.getUniqueId());
      for (int cx = px - RADIUS; cx <= px + RADIUS; cx++) {
         for (int cz = pz - RADIUS; cz <= pz + RADIUS; cz++) {
            Guild owner = this.plugin.getGuildManager().getGuildByChunk(new ChunkKey(world.getName(), cx, cz));
            if (owner == null) {
               continue;
            }

            DustOptions dust = playerGuild != null && playerGuild.getId().equals(owner.getId()) ? DUST_OWN : DUST_ENEMY;
            int minX = cx << 4;
            int minZ = cz << 4;
            // Un bord n'est dessiné que s'il touche un chunk qui n'est pas de la même guilde.
            if (this.isBorder(world, owner, cx - 1, cz)) {
               this.edge(player, dust, minX, y, minZ, 0, 1);
            }

            if (this.isBorder(world, owner, cx + 1, cz)) {
               this.edge(player, dust, minX + 16, y, minZ, 0, 1);
            }

            if (this.isBorder(world, owner, cx, cz - 1)) {
               this.edge(player, dust, minX, y, minZ, 1, 0);
            }

            if (this.isBorder(world, owner, cx, cz + 1)) {
               this.edge(player, dust, minX, y, minZ + 16, 1, 0);
            }
         }
      }
   }

   private boolean isBorder(World world, Guild owner, int cx, int cz) {
      Guild neighbour = this.plugin.getGuildManager().getGuildByChunk(new ChunkKey(world.getName(), cx, cz));
      return neighbour == null || !neighbour.getId().equals(owner.getId());
   }

   /** Un point tous les 4 blocs le long d'un bord de 16 blocs ({@code dx}/{@code dz} : direction du bord). */
   private void edge(Player player, DustOptions dust, double x, double y, double z, int dx, int dz) {
      for (int i = 0; i <= 16; i += STEP) {
         player.spawnParticle(Particle.DUST, x + dx * i, y, z + dz * i, 1, 0.0, 0.0, 0.0, 0.0, dust);
      }
   }
}
