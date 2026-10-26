package fr.esicraft.age.listener;

import fr.esicraft.EsicraftPlugin;
import fr.esicraft.age.AgeType;
import fr.esicraft.util.ChatUtil;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.World;
import org.bukkit.World.Environment;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerChangedWorldEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerPortalEvent;
import org.bukkit.event.player.PlayerTeleportEvent;
import org.bukkit.util.Vector;

public class AgeDimensionListener implements Listener {
   private final EsicraftPlugin plugin;

   public AgeDimensionListener(EsicraftPlugin plugin) {
      this.plugin = plugin;
   }

   @EventHandler(
      priority = EventPriority.HIGHEST,
      ignoreCancelled = true
   )
   public void onPlayerPortal(PlayerPortalEvent event) {
      Player player = event.getPlayer();
      if (!player.hasPermission("esicraft.admin")) {
         Location to = event.getTo();
         if (to != null && to.getWorld() != null) {
            Environment env = to.getWorld().getEnvironment();
            int playerAgeLevel = this.plugin.getAgeManager().getPlayerAge(player).getLevel();
            if (env == Environment.NETHER && playerAgeLevel < 2) {
               event.setCancelled(true);
               ChatUtil.sendMessage(
                  player,
                  "&c❌ L'accès au Nether est réservé aux joueurs ayant atteint l'&eÂge 2 (&f" + AgeType.METALLURGY.getDisplayName() + "&e) &cou supérieur !"
               );
               ChatUtil.sendActionbar(player, "&c❌ Accès au Nether verrouillé (Requis : Âge 2 - Métallurgie)");
               ChatUtil.playSound(player, Sound.ENTITY_VILLAGER_NO, 1.0F, 1.0F);
               this.ejectFromPortal(player);
            } else if (env == Environment.THE_END && playerAgeLevel < 3) {
               event.setCancelled(true);
               ChatUtil.sendMessage(
                  player, "&c❌ L'accès à l'End est réservé aux joueurs ayant atteint l'&eÂge 3 (&d" + AgeType.ALCHEMY.getDisplayName() + "&e) &cou supérieur !"
               );
               ChatUtil.sendActionbar(player, "&c❌ Accès à l'End verrouillé (Requis : Âge 3 - Alchimie)");
               ChatUtil.playSound(player, Sound.ENTITY_VILLAGER_NO, 1.0F, 1.0F);
               this.ejectFromPortal(player);
            }
         }
      }
   }

   @EventHandler(
      priority = EventPriority.HIGHEST,
      ignoreCancelled = true
   )
   public void onPlayerTeleport(PlayerTeleportEvent event) {
      Player player = event.getPlayer();
      if (!player.hasPermission("esicraft.admin")) {
         Location to = event.getTo();
         if (to != null && to.getWorld() != null) {
            Environment env = to.getWorld().getEnvironment();
            int playerAgeLevel = this.plugin.getAgeManager().getPlayerAge(player).getLevel();
            if (env == Environment.NETHER && playerAgeLevel < 2) {
               event.setCancelled(true);
               ChatUtil.sendMessage(
                  player,
                  "&c❌ Téléportation vers le Nether annulée ! Votre guilde doit atteindre l'&eÂge 2 (&f" + AgeType.METALLURGY.getDisplayName() + "&e)&c."
               );
               ChatUtil.playSound(player, Sound.ENTITY_VILLAGER_NO, 1.0F, 1.0F);
            } else if (env == Environment.THE_END && playerAgeLevel < 3) {
               event.setCancelled(true);
               ChatUtil.sendMessage(
                  player, "&c❌ Téléportation vers l'End annulée ! Votre guilde doit atteindre l'&eÂge 3 (&d" + AgeType.ALCHEMY.getDisplayName() + "&e)&c."
               );
               ChatUtil.playSound(player, Sound.ENTITY_VILLAGER_NO, 1.0F, 1.0F);
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
         if (event.getAction() == Action.RIGHT_CLICK_BLOCK) {
            if (event.getClickedBlock() != null) {
               Material clickedType = event.getClickedBlock().getType();
               int playerAgeLevel = this.plugin.getAgeManager().getPlayerAge(player).getLevel();
               if (clickedType == Material.OBSIDIAN && event.getItem() != null) {
                  Material itemType = event.getItem().getType();
                  if ((itemType == Material.FLINT_AND_STEEL || itemType == Material.FIRE_CHARGE) && playerAgeLevel < 2) {
                     event.setCancelled(true);
                     ChatUtil.sendMessage(
                        player, "&c❌ Vous devez atteindre l'&eÂge 2 (&f" + AgeType.METALLURGY.getDisplayName() + "&e) &cpour allumer un portail du Nether !"
                     );
                     ChatUtil.sendActionbar(player, "&c❌ Portail du Nether verrouillé (Requis : Âge 2)");
                     ChatUtil.playSound(player, Sound.ENTITY_VILLAGER_NO, 1.0F, 1.0F);
                  }
               }

               if (clickedType == Material.END_PORTAL_FRAME && event.getItem() != null && event.getItem().getType() == Material.ENDER_EYE && playerAgeLevel < 3
                  )
                {
                  event.setCancelled(true);
                  ChatUtil.sendMessage(
                     player, "&c❌ Vous devez atteindre l'&eÂge 3 (&d" + AgeType.ALCHEMY.getDisplayName() + "&e) &cpour activer le portail de l'End !"
                  );
                  ChatUtil.sendActionbar(player, "&c❌ Portail de l'End verrouillé (Requis : Âge 3)");
                  ChatUtil.playSound(player, Sound.ENTITY_VILLAGER_NO, 1.0F, 1.0F);
               }
            }
         }
      }
   }

   @EventHandler
   public void onPlayerJoin(PlayerJoinEvent event) {
      Player joining = event.getPlayer();
      // La position du joueur est réécrite pendant le chargement initial : on retente après quelques ticks.
      Bukkit.getScheduler().runTaskLater(this.plugin, () -> this.checkAndExpelIfUnderage(joining), 10L);
      Bukkit.getScheduler().runTaskLater(this.plugin, () -> this.checkAndExpelIfUnderage(joining), 60L);
   }

   @EventHandler
   public void onWorldChange(PlayerChangedWorldEvent event) {
      Player moved = event.getPlayer();
      Bukkit.getScheduler().runTask(this.plugin, () -> this.checkAndExpelIfUnderage(moved));
   }

   /** Appelé aussi quand un joueur quitte ou est expulsé de sa guilde : on ne garde pas le Nether / l'End d'un Âge « emprunté ». */
   public void checkAndExpelIfUnderage(Player player) {
      if (player != null && player.isOnline()) {
         if (!player.hasPermission("esicraft.admin")) {
            World world = player.getWorld();
            Environment env = world.getEnvironment();
            int playerAgeLevel = this.plugin.getAgeManager().getPlayerAge(player).getLevel();
            if (env == Environment.NETHER && playerAgeLevel < 2) {
               this.teleportToOverworldSpawn(player, "&c⚠ Vous avez été expulsé du Nether car votre guilde n'a pas encore atteint l'Âge 2 !");
            } else if (env == Environment.THE_END && playerAgeLevel < 3) {
               this.teleportToOverworldSpawn(player, "&c⚠ Vous avez été expulsé de l'End car votre guilde n'a pas encore atteint l'Âge 3 !");
            }
         }
      }
   }

   private void teleportToOverworldSpawn(Player player, String message) {
      World normalWorld = Bukkit.getWorlds()
         .stream()
         .filter(w -> w.getEnvironment() == Environment.NORMAL)
         .findFirst()
         .orElse((World)Bukkit.getWorlds().get(0));
      if (normalWorld != null) {
         Location spawnLoc = normalWorld.getSpawnLocation();
         player.teleport(spawnLoc);
         ChatUtil.sendMessage(player, message);
         ChatUtil.playSound(player, Sound.ENTITY_ENDERMAN_TELEPORT, 1.0F, 1.0F);
      }
   }

   private void ejectFromPortal(Player player) {
      Vector velocity = player.getLocation().getDirection().multiply(-0.8).setY(0.2);
      player.setVelocity(velocity);
   }
}
