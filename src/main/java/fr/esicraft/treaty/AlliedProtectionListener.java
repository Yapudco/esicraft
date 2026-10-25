package fr.esicraft.treaty;

import fr.esicraft.EsicraftPlugin;
import fr.esicraft.guild.Guild;
import java.util.Set;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.entity.ThrownPotion;
import org.bukkit.event.entity.AreaEffectCloudApplyEvent;
import org.bukkit.event.entity.PotionSplashEvent;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.entity.Projectile;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;

/** Pas de dégâts (coups, projectiles, potions nocives jetées ou persistantes) entre joueurs de guildes liées par une alliance ou une vassalité. */
public class AlliedProtectionListener implements Listener {
   private final EsicraftPlugin plugin;

   public AlliedProtectionListener(EsicraftPlugin plugin) {
      this.plugin = plugin;
   }

   private static final Set<PotionEffectType> HARMFUL = Set.of(PotionEffectType.INSTANT_DAMAGE, PotionEffectType.POISON, PotionEffectType.WITHER,
      PotionEffectType.WEAKNESS, PotionEffectType.SLOWNESS, PotionEffectType.MINING_FATIGUE, PotionEffectType.BLINDNESS, PotionEffectType.NAUSEA,
      PotionEffectType.HUNGER, PotionEffectType.LEVITATION, PotionEffectType.DARKNESS, PotionEffectType.UNLUCK);

   private boolean allied(Player a, Player b) {
      if (a == null || b == null || a.equals(b) || !this.plugin.getConfig().getBoolean("treaties.protect-allies-pvp", true)) {
         return false;
      }

      Guild ga = this.plugin.getGuildManager().getGuildByPlayer(a.getUniqueId());
      Guild gb = this.plugin.getGuildManager().getGuildByPlayer(b.getUniqueId());
      return ga != null && gb != null && !ga.getId().equals(gb.getId()) && this.plugin.getTreatyManager().areFriends(ga.getId(), gb.getId());
   }

   private static boolean harmful(Iterable<PotionEffect> effects) {
      for (PotionEffect e : effects) {
         if (HARMFUL.contains(e.getType())) {
            return true;
         }
      }

      return false;
   }

   @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
   public void onSplash(PotionSplashEvent event) {
      ThrownPotion potion = event.getPotion();
      if (potion.getShooter() instanceof Player thrower && harmful(potion.getEffects())) {
         for (LivingEntity target : event.getAffectedEntities()) {
            if (target instanceof Player victim && this.allied(thrower, victim)) {
               event.setIntensity(victim, 0.0);
            }
         }
      }
   }

   @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
   public void onCloud(AreaEffectCloudApplyEvent event) {
      var cloud = event.getEntity();
      boolean bad = harmful(cloud.getCustomEffects()) || cloud.getBasePotionType() != null && harmful(cloud.getBasePotionType().getPotionEffects());
      if (bad && cloud.getSource() instanceof Player thrower) {
         event.getAffectedEntities().removeIf(e -> e instanceof Player victim && this.allied(thrower, victim));
      }
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
