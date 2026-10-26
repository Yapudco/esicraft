package fr.esicraft.game;

import fr.esicraft.EsicraftPlugin;
import fr.esicraft.combat.CombatManager;
import fr.esicraft.util.ChatUtil;
import java.time.DayOfWeek;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.Predicate;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.PotionSplashEvent;

/**
 * Trêve hebdomadaire : une heure sans PvP le vendredi et le samedi soir (jours, heure et durée réglables dans {@code truce.*}).
 * Les duels d'honneur en sont exemptés ; la règle « Trêve prolongée » du Tribunal ajoute le jeudi et double la durée.
 */
public class TruceManager implements Listener {
   private static final java.util.Set<org.bukkit.potion.PotionEffectType> HARMFUL = java.util.Set.of(org.bukkit.potion.PotionEffectType.INSTANT_DAMAGE, org.bukkit.potion.PotionEffectType.POISON,
      org.bukkit.potion.PotionEffectType.WITHER, org.bukkit.potion.PotionEffectType.WEAKNESS, org.bukkit.potion.PotionEffectType.SLOWNESS, org.bukkit.potion.PotionEffectType.MINING_FATIGUE,
      org.bukkit.potion.PotionEffectType.BLINDNESS, org.bukkit.potion.PotionEffectType.NAUSEA, org.bukkit.potion.PotionEffectType.HUNGER, org.bukkit.potion.PotionEffectType.LEVITATION);

   private final EsicraftPlugin plugin;
   private final List<Predicate<Player>> exemptions = new CopyOnWriteArrayList<>();
   private boolean wasTruce;

   public TruceManager(EsicraftPlugin plugin) {
      this.plugin = plugin;
   }

   public void start() {
      Bukkit.getScheduler().runTaskTimer(this.plugin, this::tick, 200L, 400L);
   }

   /** Un joueur exempté (en duel, par exemple) peut combattre pendant la trêve. */
   public void addExemption(Predicate<Player> exemption) {
      this.exemptions.add(exemption);
   }

   private List<DayOfWeek> days() {
      List<DayOfWeek> days = new ArrayList<>();
      for (String name : this.plugin.getConfig().getStringList("truce.days")) {
         try {
            days.add(DayOfWeek.valueOf(name.trim().toUpperCase(Locale.ROOT)));
         } catch (IllegalArgumentException ignored) {
         }
      }

      if (this.plugin.getCourt() != null && this.plugin.getCourt().is(CourtManager.Rule.LONG_TRUCE) && !days.contains(DayOfWeek.THURSDAY)) {
         days.add(DayOfWeek.THURSDAY);
      }

      return days;
   }

   private int minutes() {
      int base = Math.max(1, this.plugin.getConfig().getInt("truce.duration-minutes", 60));
      return this.plugin.getCourt() != null && this.plugin.getCourt().is(CourtManager.Rule.LONG_TRUCE) ? base * 2 : base;
   }

   private LocalTime startTime() {
      return LocalTime.of(Math.max(0, Math.min(23, this.plugin.getConfig().getInt("truce.start-hour", 21))), Math.max(0, Math.min(59, this.plugin.getConfig().getInt("truce.start-minute", 0))));
   }

   private volatile boolean cachedTruce;
   private volatile long cachedAt;

   /** La trêve est-elle en cours à cet instant ? (résultat gardé une seconde : cette méthode est appelée à chaque coup entre joueurs). */
   public boolean isTruce() {
      long now = System.currentTimeMillis();
      if (now - this.cachedAt >= 1000L) {
         this.cachedTruce = this.isTruceAt(LocalDateTime.now());
         this.cachedAt = now;
      }

      return this.cachedTruce;
   }

   public boolean isTruceAt(LocalDateTime at) {
      if (!this.plugin.getConfig().getBoolean("truce.enabled", true) || !this.days().contains(at.getDayOfWeek())) {
         return false;
      }

      LocalDateTime start = at.toLocalDate().atTime(this.startTime());
      return !at.isBefore(start) && at.isBefore(start.plusMinutes(this.minutes()));
   }

   /** Une plage horaire [début, fin[ chevauche-t-elle la trêve ? (les sièges ne peuvent pas la recouvrir.) */
   public boolean overlapsTruce(LocalDateTime from, LocalDateTime to) {
      if (!this.plugin.getConfig().getBoolean("truce.enabled", true) || !this.days().contains(from.getDayOfWeek())) {
         return false;
      }

      LocalDateTime start = from.toLocalDate().atTime(this.startTime());
      LocalDateTime end = start.plusMinutes(this.minutes());
      return from.isBefore(end) && to.isAfter(start);
   }

   /** Vrai (avec message) si la trêve interdit l'action : on ne frappe ni Nexus, ni convoi, on ne lance pas de horde, on ne pille pas. */
   public boolean denyHostile(Player player) {
      if (!this.isTruce() || this.plugin.bypassCooldowns(player)) {
         return false;
      }

      ChatUtil.sendActionbar(player, "&a🕊 Trêve : aucune attaque (Nexus, raids, convois) n'est permise pour l'instant.");
      return true;
   }

   private void tick() {
      boolean now = this.isTruce();
      if (now != this.wasTruce) {
         this.wasTruce = now;
         if (now) {
            ChatUtil.broadcast("&a🕊 &lLa trêve commence ! &7Plus aucun PvP pendant &e" + this.minutes() + " minutes&7 : profitez-en pour commercer et construire.");
         } else {
            ChatUtil.broadcast("&c⚔ &lLa trêve est terminée. &7Le PvP est de nouveau permis.");
         }
      }
   }

   private boolean exempt(Player p) {
      for (Predicate<Player> e : this.exemptions) {
         if (e.test(p)) {
            return true;
         }
      }

      return false;
   }

   @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
   public void onPvp(EntityDamageByEntityEvent event) {
      if (!(event.getEntity() instanceof Player victim) || !this.isTruce()) {
         return;
      }

      Player attacker = CombatManager.attackerOf(event.getDamager());
      if (attacker == null || attacker.equals(victim) || this.exempt(attacker) && this.exempt(victim)) {
         return;
      }

      event.setCancelled(true);
      ChatUtil.sendActionbar(attacker, "&a🕊 Trêve : le PvP est suspendu pour l'instant.");
   }

   @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
   public void onSplash(PotionSplashEvent event) {
      if (!this.isTruce() || !(event.getPotion().getShooter() instanceof Player thrower)) {
         return;
      }

      boolean harmful = false;
      for (org.bukkit.potion.PotionEffect e : event.getPotion().getEffects()) {
         if (HARMFUL.contains(e.getType())) {
            harmful = true;
         }
      }

      if (!harmful) {
         return;
      }

      for (var target : event.getAffectedEntities()) {
         if (target instanceof Player victim && !victim.equals(thrower) && !(this.exempt(thrower) && this.exempt(victim))) {
            event.setIntensity(victim, 0.0);
         }
      }
   }
}
