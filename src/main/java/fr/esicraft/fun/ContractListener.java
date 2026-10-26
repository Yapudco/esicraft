package fr.esicraft.fun;

import fr.esicraft.EsicraftPlugin;
import fr.esicraft.util.PlacedBlockTracker;
import java.util.EnumSet;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.data.Ageable;
import org.bukkit.entity.Enemy;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.entity.Projectile;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.enchantment.EnchantItemEvent;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.event.inventory.FurnaceExtractEvent;
import org.bukkit.event.player.PlayerQuitEvent;

/** Alimente les contrats quotidiens à partir des actions de jeu. */
public class ContractListener implements Listener {
   private static final EnumSet<Material> CROPS = EnumSet.of(
      Material.WHEAT, Material.CARROTS, Material.POTATOES, Material.BEETROOTS, Material.NETHER_WART
   );
   private static final EnumSet<Material> REDSTONE_PARTS = EnumSet.of(
      Material.PISTON, Material.STICKY_PISTON, Material.REPEATER, Material.COMPARATOR,
      Material.OBSERVER, Material.HOPPER, Material.DISPENSER, Material.DROPPER
   );

   private final EsicraftPlugin plugin;
   private final PlacedBlockTracker placedTracker;
   /** Délai minimal entre deux redstones comptées (poser puis casser en boucle ne rapporte plus). */
   private final java.util.Map<java.util.UUID, Long> lastRedstone = new java.util.concurrent.ConcurrentHashMap<>();
   // L'état « bloc posé par un joueur » est lu AVANT que le listener de métiers ne le retire (HIGHEST), puis exploité au MONITOR.
   private BlockBreakEvent lastBreak;
   private boolean lastBreakWasPlaced;

   public ContractListener(EsicraftPlugin plugin) {
      this.plugin = plugin;
      this.placedTracker = PlacedBlockTracker.shared(plugin);
   }

   private static boolean isOre(Material mat) {
      return mat.name().endsWith("_ORE") || mat == Material.ANCIENT_DEBRIS;
   }

   @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
   public void onBreakPre(BlockBreakEvent event) {
      this.lastBreak = event;
      this.lastBreakWasPlaced = this.placedTracker.isPlaced(event.getBlock());
   }

   @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
   public void onBreak(BlockBreakEvent event) {
      Player player = event.getPlayer();
      Block block = event.getBlock();
      boolean wasPlaced = this.lastBreak == event && this.lastBreakWasPlaced;
      this.lastBreak = null;
      Material mat = block.getType();
      if (isOre(mat) && !wasPlaced) {
         this.plugin.getContractManager().addProgress(player, ContractType.MINE_ORE, 1);
      } else if (CROPS.contains(mat) && block.getBlockData() instanceof Ageable ageable && ageable.getAge() >= ageable.getMaximumAge()) {
         this.plugin.getContractManager().addProgress(player, ContractType.HARVEST, 1);
      }
   }

   @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
   public void onPlace(BlockPlaceEvent event) {
      Block block = event.getBlockPlaced();
      if (REDSTONE_PARTS.contains(block.getType())) {
         long packed = ((long)block.getX() & 0x3FFFFFFL) << 38 | ((long)block.getZ() & 0x3FFFFFFL) << 12 | ((long)(block.getY() + 2048) & 0xFFFL);
         long nowMs = System.currentTimeMillis();
         Long last = this.lastRedstone.get(event.getPlayer().getUniqueId());
         if (last != null && nowMs - last < 2000L) {
            return;
         }

         this.lastRedstone.put(event.getPlayer().getUniqueId(), nowMs);
         if (this.plugin.getContractManager().markPositionOnce(event.getPlayer(), packed)) {
            this.plugin.getContractManager().addProgress(event.getPlayer(), ContractType.BUILD_REDSTONE, 1);
         }
      }
   }

   @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
   public void onKill(EntityDeathEvent event) {
      LivingEntity entity = event.getEntity();
      Player killer = entity.getKiller();
      if (killer == null || !(entity instanceof Enemy)) {
         return;
      }

      // Les kills trop répétitifs sur un même type (ferme) ne comptent plus.
      if (this.plugin.getAntiFarmManager().getMultiplier(killer.getUniqueId(), entity.getType()) < 0.7) {
         return;
      }

      this.plugin.getContractManager().addProgress(killer, ContractType.KILL, 1);
      if (this.plugin.getAchievements().addStat(killer.getUniqueId(), fr.esicraft.life.AchievementManager.STAT_MONSTERS, 1L) >= 500L) {
         this.plugin.getAchievements().grant(killer.getUniqueId(), fr.esicraft.life.Achievement.HUNTER);
      }
      if (entity.getLastDamageCause() instanceof EntityDamageByEntityEvent byEntity && byEntity.getDamager() instanceof Projectile) {
         this.plugin.getContractManager().addProgress(killer, ContractType.KILL_RANGED, 1);
      }
   }

   @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
   public void onSmelt(FurnaceExtractEvent event) {
      if (event.getItemType().name().endsWith("_INGOT") && event.getItemAmount() > 0) {
         this.plugin.getContractManager().addProgress(event.getPlayer(), ContractType.SMELT, event.getItemAmount());
      }
   }

   @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
   public void onEnchant(EnchantItemEvent event) {
      if (event.getExpLevelCost() >= 2) {
         this.plugin.getContractManager().addProgress(event.getEnchanter(), ContractType.ENCHANT, 1);
      }
   }

   @EventHandler
   public void onQuit(PlayerQuitEvent event) {
      this.plugin.getContractManager().onQuit(event.getPlayer().getUniqueId());
      this.plugin.getHudManager().onQuit(event.getPlayer());
   }
}
