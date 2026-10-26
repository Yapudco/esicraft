package fr.esicraft.nexus;

import fr.esicraft.EsicraftPlugin;
import fr.esicraft.guild.Guild;
import fr.esicraft.util.ChatUtil;
import fr.esicraft.util.ChunkKey;
import java.util.Iterator;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockBurnEvent;
import org.bukkit.event.block.BlockDispenseEvent;
import org.bukkit.event.block.BlockExplodeEvent;
import org.bukkit.event.block.BlockFormEvent;
import org.bukkit.event.block.BlockFromToEvent;
import org.bukkit.event.block.BlockPistonExtendEvent;
import org.bukkit.event.block.BlockPistonRetractEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.entity.EntityExplodeEvent;
import org.bukkit.event.player.PlayerBucketEmptyEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.ItemStack;

public class NexusListener implements Listener {
   private final EsicraftPlugin plugin;
   private final Map<UUID, Long> lastAttackTime = new ConcurrentHashMap<>();
   private static final Map<Material, String> FORBIDDEN_IN_NEXUS_CHUNK = Map.of(
      Material.OBSIDIAN, "L'Obsidienne", Material.CRYING_OBSIDIAN, "L'Obsidienne Pleureuse", Material.LAVA, "La lave", Material.WATER, "L'eau"
   );
   private static final Map<Material, String> FORBIDDEN_BUCKETS = Map.of(Material.LAVA_BUCKET, "La lave", Material.WATER_BUCKET, "L'eau");

   public NexusListener(EsicraftPlugin plugin) {
      this.plugin = plugin;
   }

   /** L'eau est permise autour du Nexus d'une guilde Atlante (son Nexus doit être immergé) ; lave et obsidienne restent interdites. */
   private boolean atlanteanWater(Location loc) {
      if (loc.getWorld() == null) {
         return false;
      }

      Guild g = this.plugin.getGuildManager().getGuildByChunk(new fr.esicraft.util.ChunkKey(loc.getWorld().getName(), loc.getBlockX() >> 4, loc.getBlockZ() >> 4));
      return g != null && g.getSpecialization() == fr.esicraft.guild.GuildSpecialization.ATLANTEAN;
   }

   private Guild getNexusGuild(Location loc) {
      return this.plugin.getNexusManager().getGuildByNexusLocation(loc);
   }

   @EventHandler(
      priority = EventPriority.HIGHEST
   )
   public void onBlockBreak(BlockBreakEvent event) {
      Guild guild = this.getNexusGuild(event.getBlock().getLocation());
      if (guild != null) {
         Player player = event.getPlayer();
         if (player.getGameMode() == GameMode.CREATIVE && (player.hasPermission("esicraft.admin") || player.isOp())) {
            this.plugin.getNexusManager().removeNexus(guild);
            // Nouveau délai de grâce d'1 h pour reposer un Nexus avant la dissolution automatique
            guild.setCreatedAt(System.currentTimeMillis());
            this.plugin.getDataManager().saveGuild(guild);
            ChatUtil.sendMessage(
               player, "&e[Admin] Nexus de la guilde &6" + guild.getName() + " &cretiré (désindexé, hologramme supprimé). La guilde dispose d'1 h pour en reposer un."
            );
         } else if (this.plugin.getSiege().pickaxeAttack(player, guild)) {
            event.setCancelled(true);
         } else {
            event.setCancelled(true);
            ChatUtil.sendActionbar(player, "&c\ud83d\udee1️ Le Nexus est indestructible à la pioche ! Frappez-le avec une arme.");
         }
      }
   }

   @EventHandler(
      priority = EventPriority.HIGH,
      ignoreCancelled = true
   )
   public void onBlockPlace(BlockPlaceEvent event) {
      Location loc = event.getBlock().getLocation();
      if (this.plugin.getNexusManager().isNexusChunk(loc)) {
         Material type = event.getBlock().getType();
         String forbiddenName = FORBIDDEN_IN_NEXUS_CHUNK.get(type);
         if (forbiddenName != null && !(type == Material.WATER && this.atlanteanWater(loc))) {
            event.setCancelled(true);
            ChatUtil.sendMessage(event.getPlayer(), "&c❌ " + forbiddenName + " est strictement interdite dans le périmètre du Nexus (16 blocs) !");
            ChatUtil.playSound(event.getPlayer(), Sound.ENTITY_VILLAGER_NO, 1.0F, 1.0F);
         }
      }
   }

   @EventHandler(
      priority = EventPriority.HIGH,
      ignoreCancelled = true
   )
   public void onBucketEmpty(PlayerBucketEmptyEvent event) {
      Location targetLoc = event.getBlockClicked().getRelative(event.getBlockFace()).getLocation();
      if (this.plugin.getNexusManager().isNexusChunk(targetLoc)) {
         String forbiddenName = FORBIDDEN_BUCKETS.get(event.getBucket());
         if (forbiddenName != null && !(event.getBucket() == Material.WATER_BUCKET && this.atlanteanWater(targetLoc))) {
            event.setCancelled(true);
            ChatUtil.sendMessage(event.getPlayer(), "&c❌ " + forbiddenName + " est strictement interdite dans le périmètre du Nexus (16 blocs) !");
            ChatUtil.playSound(event.getPlayer(), Sound.ENTITY_VILLAGER_NO, 1.0F, 1.0F);
         }
      }
   }

   @EventHandler(
      priority = EventPriority.HIGH,
      ignoreCancelled = true
   )
   public void onLavaFlow(BlockFromToEvent event) {
      Material type = event.getBlock().getType();
      if ((type == Material.LAVA || type == Material.WATER) && this.plugin.getNexusManager().isNexusChunk(event.getToBlock().getLocation())
         && !(type == Material.WATER && this.atlanteanWater(event.getToBlock().getLocation()))) {
         event.setCancelled(true);
      }
   }

   @EventHandler(
      priority = EventPriority.HIGH,
      ignoreCancelled = true
   )
   public void onBlockForm(BlockFormEvent event) {
      Material formed = event.getNewState().getType();
      if ((formed == Material.OBSIDIAN || formed == Material.CRYING_OBSIDIAN) && this.plugin.getNexusManager().isNexusChunk(event.getBlock().getLocation())) {
         event.setCancelled(true);
      }
   }

   @EventHandler(
      priority = EventPriority.HIGH,
      ignoreCancelled = true
   )
   public void onDispenseLava(BlockDispenseEvent event) {
      Material itemType = event.getItem().getType();
      if ((itemType == Material.LAVA_BUCKET || itemType == Material.WATER_BUCKET && !this.atlanteanWater(event.getBlock().getLocation()))
         && this.plugin.getNexusManager().isNexusChunk(event.getBlock().getLocation())
         )
       {
         event.setCancelled(true);
      }
   }

   @EventHandler(
      priority = EventPriority.HIGH
   )
   public void onPlayerRightClickNexus(PlayerInteractEvent event) {
      if (event.getAction() == Action.RIGHT_CLICK_BLOCK) {
         Block block = event.getClickedBlock();
         if (block != null) {
            Guild guild = this.getNexusGuild(block.getLocation());
            if (guild != null && guild.hasNexus()) {
               Player player = event.getPlayer();
               ItemStack handItem = player.getInventory().getItemInMainHand();
               if (handItem.getType() == Material.GOLDEN_APPLE || handItem.getType() == Material.ENCHANTED_GOLDEN_APPLE) {
                  event.setCancelled(true);
                  if (!guild.isMember(player.getUniqueId())) {
                     ChatUtil.sendMessage(player, "&c❌ Seuls les membres de la guilde peuvent soigner leur Nexus !");
                     return;
                  }
                  boolean healed = this.plugin.getNexusManager().healWithGoldenApple(guild, player, handItem);
                  if (healed && player.getGameMode() != GameMode.CREATIVE) {
                     handItem.setAmount(handItem.getAmount() - 1);
                  }
               }
            }
         }
      }
   }

   @EventHandler(
      priority = EventPriority.HIGH
   )
   public void onPlayerAttackNexus(PlayerInteractEvent event) {
      if (event.getAction() == Action.LEFT_CLICK_BLOCK) {
         Block block = event.getClickedBlock();
         if (block != null) {
            Guild guild = this.getNexusGuild(block.getLocation());
            if (guild != null && guild.hasNexus()) {
               Player attacker = event.getPlayer();
               if (attacker.getGameMode() != GameMode.CREATIVE) {
                  if (!this.plugin.isNexusDestructionEnabled()) {
                     ChatUtil.sendActionbar(attacker, "&c\ud83d\udee1️ La destruction des Nexus est temporairement désactivée par un admin (/pvp) !");
                  } else if (this.plugin.getTruce().denyHostile(attacker)) {
                     return;
                  } else if (guild.isMember(attacker.getUniqueId())) {
                     ChatUtil.sendActionbar(attacker, "&cVous ne pouvez pas attaquer le Nexus de votre propre guilde !");
                  } else {
                     long now = System.currentTimeMillis();
                     if (this.lastAttackTime.size() > 50) {
                        this.lastAttackTime.entrySet().removeIf(entry -> now - entry.getValue() > 10000L);
                     }

                     long lastHit = this.lastAttackTime.getOrDefault(attacker.getUniqueId(), 0L);
                     if (now - lastHit >= 500L) {
                        this.lastAttackTime.put(attacker.getUniqueId(), now);
                        Location center = guild.getNexus().getLocation().clone().add(0.5, 0.5, 0.5);
                        // Portée et ligne de vue vérifiées côté serveur : un client modifié ne frappe pas le Nexus à travers les murs.
                        if (attacker.getWorld() != center.getWorld() || attacker.getEyeLocation().distanceSquared(center) > 49.0 || !this.canSee(attacker, block)) {
                           ChatUtil.sendActionbar(attacker, "&c❌ Vous devez voir le Nexus pour le frapper.");
                           return;
                        }

                        {
                           double factor = 1.0;
                           if (!this.isDefenderPresent(guild) && !this.plugin.getSiege().isSiegeAttacker(attacker, guild)) {
                              // Plus d'immunité totale quand tous les défenseurs sont absents (hors ligne ou loin des claims) : les dégâts sont réduits.
                              factor = this.plugin.getConfig().getDouble("nexus.undefended-damage-factor", 0.25);
                              ChatUtil.sendActionbar(attacker, "&e\ud83d\udee1️ Aucun défenseur présent : le Nexus encaisse &6" + Math.round(factor * 100.0) + " % &edes dégâts.");
                           }

                           ItemStack weapon = attacker.getInventory().getItemInMainHand();
                           int damage = Math.max(1, (int)Math.round(this.getWeaponDamage(weapon.getType()) * factor * this.plugin.getSiege().multiplierFor(attacker, guild)));
                           this.plugin.getDisguiseManager().reveal(attacker, true);
                           if (this.plugin.getTreatyManager().blocksRecruitAggression(this.plugin.getGuildManager().getGuildByPlayer(attacker.getUniqueId()), guild, attacker)) {
                              ChatUtil.sendActionbar(attacker, "&c❌ Une recrue ne peut pas attaquer le Nexus d'une guilde liée à la sienne par un traité.");
                              return;
                           }

                           this.plugin.getTreatyManager().onAggression(this.plugin.getGuildManager().getGuildByPlayer(attacker.getUniqueId()), guild, attacker);
                           this.plugin.getNexusManager().damageNexus(guild, damage, attacker.getName(), this.plugin.getGuildManager().getGuildByPlayer(attacker.getUniqueId()), attacker);
                           ChatUtil.sendActionbar(attacker, "&c⚔️ Vous avez infligé &e" + damage + " dégâts &cau Nexus ennemi !");
                        }
                     }
                  }
               }
            }
         }
      }
   }

   /** Ligne de vue entre l'œil du joueur et le Nexus : aucun autre bloc plein ne doit s'interposer. */
   private boolean canSee(Player attacker, Block nexusBlock) {
      Location eye = attacker.getEyeLocation();
      Location target = nexusBlock.getLocation().add(0.5, 0.5, 0.5);
      org.bukkit.util.Vector dir = target.toVector().subtract(eye.toVector());
      double dist = dir.length();
      if (dist < 0.01) {
         return true;
      }

      org.bukkit.util.RayTraceResult hit = eye.getWorld().rayTraceBlocks(eye, dir.normalize(), dist + 0.5, org.bukkit.FluidCollisionMode.NEVER, true);
      return hit == null || hit.getHitBlock() == null || hit.getHitBlock().equals(nexusBlock);
   }

   private boolean isDefenderPresent(Guild guild) {
      Location nexusLoc = guild.getNexus().getLocation();

      for (UUID memberUuid : guild.getMembers().keySet()) {
         Player member = Bukkit.getPlayer(memberUuid);
         if (member != null && member.isOnline()) {
            Location pLoc = member.getLocation();
            ChunkKey pChunk = ChunkKey.from(pLoc.getChunk());
            if (guild.isClaimed(pChunk)) {
               return true;
            }

            if (nexusLoc != null
               && nexusLoc.getWorld() != null
               && pLoc.getWorld() != null
               && nexusLoc.getWorld().getName().equals(pLoc.getWorld().getName())
               && pLoc.distanceSquared(nexusLoc) <= 6400.0) {
               return true;
            }
         }
      }

      return false;
   }

   private int getWeaponDamage(Material mat) {
      String name = mat.name();
      if (name.equals("NETHERITE_SWORD")) {
         return 25;
      } else if (name.equals("DIAMOND_SWORD")) {
         return 20;
      } else if (name.equals("IRON_SWORD")) {
         return 14;
      } else if (name.equals("STONE_SWORD")) {
         return 8;
      } else if (name.equals("WOODEN_SWORD") || name.equals("GOLDEN_SWORD")) {
         return 6;
      } else {
         return name.endsWith("_AXE") ? 12 : 4;
      }
   }

   @EventHandler(
      priority = EventPriority.HIGHEST,
      ignoreCancelled = true
   )
   public void onEntityExplode(EntityExplodeEvent event) {
      Iterator<Block> iterator = event.blockList().iterator();

      while (iterator.hasNext()) {
         Block b = iterator.next();
         if (this.getNexusGuild(b.getLocation()) != null) {
            iterator.remove();
         }
      }
   }

   @EventHandler(
      priority = EventPriority.HIGHEST,
      ignoreCancelled = true
   )
   public void onBlockExplode(BlockExplodeEvent event) {
      Iterator<Block> iterator = event.blockList().iterator();

      while (iterator.hasNext()) {
         Block b = iterator.next();
         if (this.getNexusGuild(b.getLocation()) != null) {
            iterator.remove();
         }
      }
   }

   @EventHandler(
      priority = EventPriority.HIGHEST,
      ignoreCancelled = true
   )
   public void onPistonExtend(BlockPistonExtendEvent event) {
      for (Block b : event.getBlocks()) {
         if (this.getNexusGuild(b.getLocation()) != null || this.getNexusGuild(b.getRelative(event.getDirection()).getLocation()) != null) {
            event.setCancelled(true);
            return;
         }
      }
      if (this.getNexusGuild(event.getBlock().getRelative(event.getDirection()).getLocation()) != null) {
         event.setCancelled(true);
      }
   }

   @EventHandler(
      priority = EventPriority.HIGHEST,
      ignoreCancelled = true
   )
   public void onPistonRetract(BlockPistonRetractEvent event) {
      for (Block b : event.getBlocks()) {
         if (this.getNexusGuild(b.getLocation()) != null) {
            event.setCancelled(true);
            return;
         }
      }
   }

   @EventHandler(
      priority = EventPriority.HIGHEST,
      ignoreCancelled = true
   )
   public void onBlockBurn(BlockBurnEvent event) {
      if (this.getNexusGuild(event.getBlock().getLocation()) != null) {
         event.setCancelled(true);
      }
   }

   /** Frapper son propre golem gardien ne le met pas en colère : il ne cible jamais un membre de sa guilde (ni un allié). */
   @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
   public void onGuardianTarget(org.bukkit.event.entity.EntityTargetLivingEntityEvent event) {
      if (event.getEntity() instanceof org.bukkit.entity.Mob golem && event.getTarget() instanceof Player player) {
         String guildId = NexusManager.guardianGuildId(golem);
         if (guildId != null && this.plugin.getNexusManager().isGuardianFriend(guildId, player)) {
            event.setCancelled(true);
         }
      }
   }

   /** Filet de sécurité : même déjà en colère, le golem gardien ne blesse pas un membre de sa guilde. */
   @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
   public void onGuardianHit(org.bukkit.event.entity.EntityDamageByEntityEvent event) {
      if (event.getDamager() instanceof org.bukkit.entity.Mob golem && event.getEntity() instanceof Player player) {
         String guildId = NexusManager.guardianGuildId(golem);
         if (guildId != null && this.plugin.getNexusManager().isGuardianFriend(guildId, player)) {
            event.setCancelled(true);
            golem.setTarget(null);
         }
      }
   }
}
