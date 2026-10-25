package fr.esicraft.treaty;

import fr.esicraft.EsicraftPlugin;
import fr.esicraft.guild.Guild;
import org.bukkit.entity.Player;
import org.bukkit.entity.Projectile;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;

/** Pas de dégâts entre joueurs de guildes liées par une alliance ou une vassalité. */
public class AlliedProtectionListener implements Listener {
   private final EsicraftPlugin plugin;

   public AlliedProtectionListener(EsicraftPlugin plugin) {
      this.plugin = plugin;
   }

   @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
   public void onDamage(EntityDamageByEntityEvent event) {
      if (!(event.getEntity() instanceof Player victim) || !this.plugin.getConfig().getBoolean("treaties.protect-allies-pvp", true)) {
         return;
      }

      Player attacker = null;
      if (event.getDamager() instanceof Player p) {
         attacker = p;
      } else if (event.getDamager() instanceof Projectile proj && proj.getShooter() instanceof Player p) {
         attacker = p;
      }

      if (attacker == null || attacker.equals(victim)) {
         return;
      }

      Guild a = this.plugin.getGuildManager().getGuildByPlayer(attacker.getUniqueId());
      Guild b = this.plugin.getGuildManager().getGuildByPlayer(victim.getUniqueId());
      if (a != null && b != null && !a.getId().equals(b.getId()) && this.plugin.getTreatyManager().areFriends(a.getId(), b.getId())) {
         event.setCancelled(true);
      }
   }
}
