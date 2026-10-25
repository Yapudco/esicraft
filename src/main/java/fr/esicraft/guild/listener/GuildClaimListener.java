package fr.esicraft.guild.listener;

import fr.esicraft.EsicraftPlugin;
import fr.esicraft.guild.Guild;
import fr.esicraft.guild.GuildSpecialization;
import fr.esicraft.util.ChatUtil;
import fr.esicraft.util.ChunkKey;
import java.util.EnumSet;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.block.Block;
import org.bukkit.entity.Animals;
import org.bukkit.entity.ArmorStand;
import org.bukkit.entity.EnderCrystal;
import org.bukkit.entity.EnderPearl;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.entity.Projectile;
import org.bukkit.entity.Tameable;
import org.bukkit.entity.Vehicle;
import org.bukkit.entity.Villager;
import org.bukkit.entity.minecart.ExplosiveMinecart;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockBurnEvent;
import org.bukkit.event.block.BlockDispenseEvent;
import org.bukkit.event.block.BlockExplodeEvent;
import org.bukkit.event.block.BlockFromToEvent;
import org.bukkit.event.block.BlockIgniteEvent;
import org.bukkit.event.block.BlockPistonExtendEvent;
import org.bukkit.event.block.BlockPistonRetractEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.entity.EntityChangeBlockEvent;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityExplodeEvent;
import org.bukkit.event.entity.ExplosionPrimeEvent;
import org.bukkit.event.entity.ProjectileLaunchEvent;
import org.bukkit.event.hanging.HangingBreakByEntityEvent;
import org.bukkit.event.player.PlayerBucketEmptyEvent;
import org.bukkit.event.player.PlayerBucketFillEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.event.player.PlayerTeleportEvent;
import org.bukkit.event.player.PlayerTeleportEvent.TeleportCause;
import org.bukkit.event.vehicle.VehicleCreateEvent;
import org.bukkit.event.vehicle.VehicleMoveEvent;
import org.bukkit.inventory.ItemStack;

public class GuildClaimListener implements Listener {
   private final EsicraftPlugin plugin;
   private static final EnumSet<Material> CONTAINER_MATERIALS = EnumSet.noneOf(Material.class);

   public static boolean isChestMaterial(Material mat) {
      if (mat == null) {
         return false;
      } else {
         String name = mat.name();
         return mat == Material.CHEST || mat == Material.TRAPPED_CHEST || mat == Material.BARREL || name.endsWith("SHULKER_BOX") || mat == Material.ENDER_CHEST;
      }
   }

   public GuildClaimListener(EsicraftPlugin plugin) {
      this.plugin = plugin;
   }

   @EventHandler(
      priority = EventPriority.HIGH,
      ignoreCancelled = true
   )
   public void onBlockBreak(BlockBreakEvent event) {
      Player player = event.getPlayer();
      if (!player.hasPermission("esicraft.admin")) {
         Block block = event.getBlock();
         ChunkKey chunkKey = new ChunkKey(block.getWorld().getName(), block.getX() >> 4, block.getZ() >> 4);
         Guild guild = this.protectedGuildAt(chunkKey);
         if (guild != null && !guild.isMember(player.getUniqueId())) {
            event.setCancelled(true);
            ChatUtil.sendActionbar(player, "&c❌ Vous ne pouvez pas casser de blocs sur le territoire de &6" + guild.getName() + " &c!");
            ChatUtil.playSound(player, Sound.ENTITY_VILLAGER_NO, 0.8F, 1.2F);
         }
      }
   }

   @EventHandler(
      priority = EventPriority.HIGH,
      ignoreCancelled = true
   )
   public void onBlockPlace(BlockPlaceEvent event) {
      Player player = event.getPlayer();
      if (!player.hasPermission("esicraft.admin")) {
         Block block = event.getBlock();
         ChunkKey chunkKey = new ChunkKey(block.getWorld().getName(), block.getX() >> 4, block.getZ() >> 4);
         Guild guild = this.protectedGuildAt(chunkKey);
         if (guild != null
            && guild.isMember(player.getUniqueId())
            && guild.getSpecialization() == GuildSpecialization.LONE_RANGER
            && isChestMaterial(block.getType())) {
            event.setCancelled(true);
            ChatUtil.sendActionbar(player, "&c❌ La spécialisation Rôdeur Solitaire interdit d'avoir des coffres dans ses claims !");
            ChatUtil.sendMessage(player, "&c❌ Vous ne pouvez pas poser de coffres ou conteneurs de stockage sur votre territoire de Rôdeur Solitaire !");
            ChatUtil.playSound(player, Sound.ENTITY_VILLAGER_NO, 1.0F, 1.0F);
         } else {
            if (guild != null && !guild.isMember(player.getUniqueId())) {
               event.setCancelled(true);
               if (block.getType() == Material.TNT) {
                  ChatUtil.sendMessage(player, "&c❌ La pose de TNT est strictement interdite sur le territoire d'une autre guilde !");
               } else {
                  ChatUtil.sendActionbar(player, "&c❌ Vous ne pouvez pas poser de blocs sur le territoire de &6" + guild.getName() + " &c!");
               }

               ChatUtil.playSound(player, Sound.ENTITY_VILLAGER_NO, 1.0F, 1.0F);
            }
         }
      }
   }

   @EventHandler(
      priority = EventPriority.HIGH,
      ignoreCancelled = true
   )
   public void onBucketEmpty(PlayerBucketEmptyEvent event) {
      Player player = event.getPlayer();
      if (!player.hasPermission("esicraft.admin")) {
         Block block = event.getBlockClicked().getRelative(event.getBlockFace());
         ChunkKey chunkKey = new ChunkKey(block.getWorld().getName(), block.getX() >> 4, block.getZ() >> 4);
         Guild guild = this.protectedGuildAt(chunkKey);
         if (guild != null && !guild.isMember(player.getUniqueId())) {
            event.setCancelled(true);
            ChatUtil.sendActionbar(player, "&c❌ Vous ne pouvez pas vider de seau sur le territoire de &6" + guild.getName() + " &c!");
            ChatUtil.playSound(player, Sound.ENTITY_VILLAGER_NO, 1.0F, 1.0F);
         }
      }
   }

   @EventHandler(
      priority = EventPriority.HIGH,
      ignoreCancelled = true
   )
   public void onBucketFill(PlayerBucketFillEvent event) {
      Player player = event.getPlayer();
      if (!player.hasPermission("esicraft.admin")) {
         Block block = event.getBlockClicked();
         ChunkKey chunkKey = new ChunkKey(block.getWorld().getName(), block.getX() >> 4, block.getZ() >> 4);
         Guild guild = this.protectedGuildAt(chunkKey);
         if (guild != null && !guild.isMember(player.getUniqueId())) {
            event.setCancelled(true);
            ChatUtil.sendActionbar(player, "&c❌ Vous ne pouvez pas remplir de seau sur le territoire de &6" + guild.getName() + " &c!");
            ChatUtil.playSound(player, Sound.ENTITY_VILLAGER_NO, 1.0F, 1.0F);
         }
      }
   }

   @EventHandler(
      priority = EventPriority.HIGH,
      ignoreCancelled = true
   )
   public void onEntityDamage(EntityDamageByEntityEvent event) {
      Player player = this.resolvePlayer(event.getDamager());
      if (player != null) {
         if (!player.hasPermission("esicraft.admin")) {
            Entity entity = event.getEntity();
            ChunkKey chunkKey = new ChunkKey(entity.getWorld().getName(), entity.getLocation().getBlockX() >> 4, entity.getLocation().getBlockZ() >> 4);
            Guild guild = this.plugin.getGuildManager().getGuildByChunk(chunkKey);
            if (guild != null && !guild.isMember(player.getUniqueId())) {
               if (entity instanceof EnderCrystal) {
                  event.setCancelled(true);
                  ChatUtil.sendMessage(player, "&c❌ L'utilisation de Cristaux de l'End est interdite sur le territoire d'une autre guilde !");
                  ChatUtil.playSound(player, Sound.ENTITY_VILLAGER_NO, 1.0F, 1.0F);
                  return;
               }

               if (entity.getType() == EntityType.TNT_MINECART || entity instanceof ExplosiveMinecart) {
                  event.setCancelled(true);
                  entity.remove();
                  ChatUtil.sendMessage(player, "&c❌ L'utilisation de Minecarts avec TNT est strictement interdite sur le territoire d'une autre guilde !");
                  ChatUtil.playSound(player, Sound.ENTITY_VILLAGER_NO, 1.0F, 1.0F);
                  return;
               }

               if (entity instanceof Animals || entity instanceof Tameable || entity instanceof Villager || entity instanceof ArmorStand) {
                  event.setCancelled(true);
                  ChatUtil.sendActionbar(player, "&c❌ Vous ne pouvez pas attaquer les animaux/entités sur le territoire de &6" + guild.getName());
               }
            }
         }
      }
   }

   @EventHandler(
      priority = EventPriority.HIGH,
      ignoreCancelled = true
   )
   public void onHangingBreak(HangingBreakByEntityEvent event) {
      Player player = this.resolvePlayer(event.getRemover());
      if (player != null) {
         if (!player.hasPermission("esicraft.admin")) {
            Entity hanging = event.getEntity();
            ChunkKey chunkKey = new ChunkKey(hanging.getWorld().getName(), hanging.getLocation().getBlockX() >> 4, hanging.getLocation().getBlockZ() >> 4);
            Guild guild = this.plugin.getGuildManager().getGuildByChunk(chunkKey);
            if (guild != null && !guild.isMember(player.getUniqueId())) {
               event.setCancelled(true);
               ChatUtil.sendActionbar(player, "&c❌ Territoire protégé de la guilde &6" + guild.getName());
            }
         }
      }
   }

   @EventHandler(
      priority = EventPriority.HIGH,
      ignoreCancelled = true
   )
   public void onPlayerInteract(PlayerInteractEvent event) {
      Player player = event.getPlayer();
      if (!player.hasPermission("esicraft.admin")) {
         ItemStack item = event.getItem();
         Location playerLoc = player.getLocation();
         ChunkKey playerChunk = ChunkKey.from(playerLoc.getChunk());
         Guild playerGuildClaim = this.plugin.getGuildManager().getGuildByChunk(playerChunk);
         if (item != null) {
            Material type = item.getType();
            if (type == Material.ENDER_PEARL && playerGuildClaim != null && !playerGuildClaim.isMember(player.getUniqueId())) {
               event.setCancelled(true);
               ChatUtil.sendMessage(player, "&c❌ L'utilisation des Ender Pearls est interdite sur le territoire d'une autre guilde !");
               ChatUtil.playSound(player, Sound.ENTITY_VILLAGER_NO, 1.0F, 1.0F);
               return;
            }

            if (type == Material.END_CRYSTAL) {
               Location targetLoc = event.getClickedBlock() != null ? event.getClickedBlock().getLocation() : playerLoc;
               ChunkKey targetChunk = ChunkKey.from(targetLoc.getChunk());
               Guild targetGuildClaim = this.plugin.getGuildManager().getGuildByChunk(targetChunk);
               if (targetGuildClaim != null && !targetGuildClaim.isMember(player.getUniqueId())) {
                  event.setCancelled(true);
                  ChatUtil.sendMessage(player, "&c❌ L'utilisation de Cristaux de l'End est interdite sur le territoire d'une autre guilde !");
                  ChatUtil.playSound(player, Sound.ENTITY_VILLAGER_NO, 1.0F, 1.0F);
                  return;
               }
            }

            if (type == Material.TNT_MINECART) {
               Location targetLoc = event.getClickedBlock() != null ? event.getClickedBlock().getLocation() : playerLoc;
               ChunkKey targetChunk = ChunkKey.from(targetLoc.getChunk());
               Guild targetGuildClaim = this.plugin.getGuildManager().getGuildByChunk(targetChunk);
               if (targetGuildClaim != null && !targetGuildClaim.isMember(player.getUniqueId())) {
                  event.setCancelled(true);
                  ChatUtil.sendMessage(player, "&c❌ L'utilisation de Minecarts avec TNT est strictement interdite sur le territoire d'une autre guilde !");
                  ChatUtil.playSound(player, Sound.ENTITY_VILLAGER_NO, 1.0F, 1.0F);
                  return;
               }
            }
         }

         if (event.getClickedBlock() != null) {
            Block block = event.getClickedBlock();
            ChunkKey chunkKey = new ChunkKey(block.getWorld().getName(), block.getX() >> 4, block.getZ() >> 4);
            Guild guild = this.plugin.getGuildManager().getGuildByChunk(chunkKey);
            if (guild != null && !guild.isMember(player.getUniqueId())) {
               if (this.isContainerBlock(block)) {
                  event.setCancelled(true);
                  ChatUtil.sendMessage(
                     player, "&c❌ Accès refusé : Seuls les membres de &e" + guild.getName() + " &cpeuvent ouvrir les coffres et conteneurs de ce territoire !"
                  );
                  ChatUtil.playSound(player, Sound.ENTITY_VILLAGER_NO, 1.0F, 1.0F);
                  return;
               }

               event.setCancelled(true);
               ChatUtil.sendActionbar(player, "&c❌ Territoire protégé de la guilde &6" + guild.getName());
            }
         }
      }
   }

   @EventHandler(
      priority = EventPriority.HIGH,
      ignoreCancelled = true
   )
   public void onProjectileLaunch(ProjectileLaunchEvent event) {
      if (event.getEntity() instanceof EnderPearl pearl && pearl.getShooter() instanceof Player player) {
         if (player.hasPermission("esicraft.admin")) {
            return;
         }

         ChunkKey currentChunk = ChunkKey.from(player.getLocation().getChunk());
         Guild guild = this.plugin.getGuildManager().getGuildByChunk(currentChunk);
         if (guild != null && !guild.isMember(player.getUniqueId())) {
            event.setCancelled(true);
            ChatUtil.sendMessage(player, "&c❌ L'utilisation des Ender Pearls est interdite sur le territoire d'une autre guilde !");
            ChatUtil.playSound(player, Sound.ENTITY_VILLAGER_NO, 1.0F, 1.0F);
         }
      }
   }

   @EventHandler(
      priority = EventPriority.HIGH,
      ignoreCancelled = true
   )
   public void onPlayerTeleport(PlayerTeleportEvent event) {
      if (event.getCause() == TeleportCause.ENDER_PEARL || event.getCause() == TeleportCause.CHORUS_FRUIT) {
         Player player = event.getPlayer();
         if (player.hasPermission("esicraft.admin")) {
            return;
         }

         ChunkKey toChunk = ChunkKey.from(event.getTo().getChunk());
         Guild targetGuild = this.plugin.getGuildManager().getGuildByChunk(toChunk);
         if (targetGuild != null && !targetGuild.isMember(player.getUniqueId())) {
            event.setCancelled(true);
            ChatUtil.sendMessage(
               player, "&c❌ Vous ne pouvez pas vous téléporter (Ender Pearl / Fruit de Chorus) dans le territoire de la guilde &6" + targetGuild.getName() + " &c!"
            );
            ChatUtil.playSound(player, Sound.ENTITY_VILLAGER_NO, 1.0F, 1.0F);
         }
      }
   }


   /** Guilde protégeant ce chunk : null si le territoire est vulnérable (Nexus détruit il y a moins de 30 min). */
   private Guild protectedGuildAt(ChunkKey key) {
      Guild g = this.plugin.getGuildManager().getGuildByChunk(key);
      return g != null && g.getVulnerableUntil() > System.currentTimeMillis() ? null : g;
   }

   private Player resolvePlayer(Entity source) {
      if (source instanceof Player p) {
         return p;
      } else {
         return source instanceof Projectile projectile && projectile.getShooter() instanceof Player shooter ? shooter : null;
      }
   }

   private String claimOwnerId(Block block) {
      Guild g = this.plugin.getGuildManager().getGuildByChunk(new ChunkKey(block.getWorld().getName(), block.getX() >> 4, block.getZ() >> 4));
      return g != null ? g.getId() : null;
   }

   private boolean isRaided(String guildId) {
      return guildId != null && this.plugin.getRaidManager().isGuildUnderRaid(guildId);
   }

   @EventHandler(
      priority = EventPriority.HIGH,
      ignoreCancelled = true
   )
   public void onPistonExtend(BlockPistonExtendEvent event) {
      this.checkPiston(event.getBlock(), event.getBlocks(), event.getDirection(), event);
   }

   @EventHandler(
      priority = EventPriority.HIGH,
      ignoreCancelled = true
   )
   public void onPistonRetract(BlockPistonRetractEvent event) {
      this.checkPiston(event.getBlock(), event.getBlocks(), event.getDirection(), event);
   }

   private void checkPiston(Block piston, java.util.List<Block> moved, org.bukkit.block.BlockFace direction, org.bukkit.event.Cancellable event) {
      String pistonOwner = this.claimOwnerId(piston);
      for (Block b : moved) {
         String from = this.claimOwnerId(b);
         String to = this.claimOwnerId(b.getRelative(direction));
         if (from != null && !from.equals(pistonOwner) || to != null && !to.equals(pistonOwner)) {
            event.setCancelled(true);
            return;
         }
      }

      String front = this.claimOwnerId(piston.getRelative(direction));
      if (front != null && !front.equals(pistonOwner)) {
         event.setCancelled(true);
      }
   }

   @EventHandler(
      priority = EventPriority.HIGH,
      ignoreCancelled = true
   )
   public void onLiquidFlow(BlockFromToEvent event) {
      Block from = event.getBlock();
      Block to = event.getToBlock();
      if (from.getX() >> 4 != to.getX() >> 4 || from.getZ() >> 4 != to.getZ() >> 4) {
         String toOwner = this.claimOwnerId(to);
         if (toOwner != null && !toOwner.equals(this.claimOwnerId(from)) && !this.isRaided(toOwner)) {
            event.setCancelled(true);
         }
      }
   }

   @EventHandler(
      priority = EventPriority.HIGH,
      ignoreCancelled = true
   )
   public void onBlockIgnite(BlockIgniteEvent event) {
      String owner = this.claimOwnerId(event.getBlock());
      if (owner != null) {
         if (event.getPlayer() != null) {
            Guild g = this.plugin.getGuildManager().getGuildById(owner);
            if (g != null && !g.isMember(event.getPlayer().getUniqueId()) && !event.getPlayer().hasPermission("esicraft.admin")) {
               event.setCancelled(true);
            }
         } else if (event.getIgnitingBlock() != null) {
            String srcOwner = this.claimOwnerId(event.getIgnitingBlock());
            if (!owner.equals(srcOwner) && !this.isRaided(owner)) {
               event.setCancelled(true);
            }
         }
      }
   }

   @EventHandler(
      priority = EventPriority.HIGH,
      ignoreCancelled = true
   )
   public void onBlockBurn(BlockBurnEvent event) {
      String owner = this.claimOwnerId(event.getBlock());
      if (owner != null && !this.isRaided(owner)) {
         event.setCancelled(true);
      }
   }

   @EventHandler(
      priority = EventPriority.HIGH,
      ignoreCancelled = true
   )
   public void onMobGrief(EntityChangeBlockEvent event) {
      EntityType type = event.getEntityType();
      if (type == EntityType.ENDERMAN || type == EntityType.WITHER || type == EntityType.RAVAGER || type == EntityType.SILVERFISH) {
         String owner = this.claimOwnerId(event.getBlock());
         if (owner != null && !this.isRaided(owner)) {
            event.setCancelled(true);
         }
      }
   }

   private boolean isContainerBlock(Block block) {
      return block == null ? false : CONTAINER_MATERIALS.contains(block.getType());
   }

   @EventHandler(
      priority = EventPriority.MONITOR,
      ignoreCancelled = true
   )
   public void onPlayerMove(PlayerMoveEvent event) {
      int fromX = event.getFrom().getBlockX() >> 4;
      int fromZ = event.getFrom().getBlockZ() >> 4;
      int toX = event.getTo().getBlockX() >> 4;
      int toZ = event.getTo().getBlockZ() >> 4;
      if (fromX != toX || fromZ != toZ) {
         Player player = event.getPlayer();
         ChunkKey fromKey = new ChunkKey(event.getFrom().getWorld().getName(), fromX, fromZ);
         ChunkKey toKey = new ChunkKey(event.getTo().getWorld().getName(), toX, toZ);
         Guild fromGuild = this.plugin.getGuildManager().getGuildByChunk(fromKey);
         Guild toGuild = this.plugin.getGuildManager().getGuildByChunk(toKey);
         if (fromGuild != toGuild) {
            if (toGuild != null) {
               ChatUtil.sendActionbar(
                  player,
                  "&8[&6Territoire&8] &a" + toGuild.getName() + " &8[&e" + toGuild.getTag() + "&8] &7- Spé: " + toGuild.getSpecialization().getDisplayName()
               );
            } else {
               ChatUtil.sendActionbar(player, "&8[&7Terres Sauvages&8] &7Zone Neutre");
            }
         }
      }
   }

   @EventHandler(
      priority = EventPriority.HIGH,
      ignoreCancelled = true
   )
   public void onVehicleCreate(VehicleCreateEvent event) {
      Vehicle vehicle = event.getVehicle();
      if (vehicle.getType() == EntityType.TNT_MINECART || vehicle instanceof ExplosiveMinecart) {
         ChunkKey chunkKey = ChunkKey.from(vehicle.getLocation().getChunk());
         Guild guild = this.plugin.getGuildManager().getGuildByChunk(chunkKey);
         if (guild != null) {
            vehicle.remove();
         }
      }
   }

   @EventHandler(
      priority = EventPriority.HIGH,
      ignoreCancelled = true
   )
   public void onVehicleMove(VehicleMoveEvent event) {
      Vehicle vehicle = event.getVehicle();
      if (vehicle.getType() == EntityType.TNT_MINECART || vehicle instanceof ExplosiveMinecart) {
         int fromX = event.getFrom().getBlockX() >> 4;
         int fromZ = event.getFrom().getBlockZ() >> 4;
         int toX = event.getTo().getBlockX() >> 4;
         int toZ = event.getTo().getBlockZ() >> 4;
         if (fromX != toX || fromZ != toZ) {
            ChunkKey toChunk = new ChunkKey(event.getTo().getWorld().getName(), toX, toZ);
            Guild guild = this.plugin.getGuildManager().getGuildByChunk(toChunk);
            if (guild != null) {
               vehicle.remove();
            }
         }
      }
   }

   @EventHandler(
      priority = EventPriority.HIGH,
      ignoreCancelled = true
   )
   public void onExplosionPrime(ExplosionPrimeEvent event) {
      if (event.getEntity() != null && (event.getEntity().getType() == EntityType.TNT_MINECART || event.getEntity() instanceof ExplosiveMinecart)) {
         ChunkKey chunkKey = ChunkKey.from(event.getEntity().getLocation().getChunk());
         Guild guild = this.plugin.getGuildManager().getGuildByChunk(chunkKey);
         if (guild != null) {
            event.setCancelled(true);
            event.getEntity().remove();
         }
      }
   }

   @EventHandler(
      priority = EventPriority.HIGH,
      ignoreCancelled = true
   )
   public void onEntityExplode(EntityExplodeEvent event) {
      event.blockList().removeIf(block -> {
         ChunkKey chunkKey = ChunkKey.from(block.getChunk());
         Guild guild = this.protectedGuildAt(chunkKey);
         return guild != null && !this.plugin.getRaidManager().isGuildUnderRaid(guild.getId());
      });
   }

   @EventHandler(
      priority = EventPriority.HIGH,
      ignoreCancelled = true
   )
   public void onBlockExplode(BlockExplodeEvent event) {
      event.blockList().removeIf(block -> {
         ChunkKey chunkKey = ChunkKey.from(block.getChunk());
         Guild guild = this.protectedGuildAt(chunkKey);
         return guild != null && !this.plugin.getRaidManager().isGuildUnderRaid(guild.getId());
      });
   }

   @EventHandler(
      priority = EventPriority.HIGH,
      ignoreCancelled = true
   )
   public void onBlockDispense(BlockDispenseEvent event) {
      if (event.getItem().getType() == Material.TNT_MINECART) {
         ChunkKey chunkKey = ChunkKey.from(event.getBlock().getLocation().getChunk());
         Guild guild = this.plugin.getGuildManager().getGuildByChunk(chunkKey);
         if (guild != null) {
            event.setCancelled(true);
         }
      }
   }

   static {
      for (Material m : Material.values()) {
         if (m.isBlock()) {
            String name = m.name();
            if (name.contains("CHEST")
               || name.contains("BARREL")
               || name.contains("SHULKER")
               || name.contains("FURNACE")
               || name.contains("SMOKER")
               || name.contains("HOPPER")
               || name.contains("DISPENSER")
               || name.contains("DROPPER")
               || name.contains("BREWING_STAND")
               || name.contains("CAMPFIRE")
               || name.contains("LECTERN")
               || name.contains("ANVIL")
               || name.contains("ENCHANTING_TABLE")
               || name.contains("JUKEBOX")
               || name.contains("CRAFTER")) {
               CONTAINER_MATERIALS.add(m);
            }
         }
      }
   }
}
