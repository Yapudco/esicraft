package fr.esicraft.combat;

import fr.esicraft.EsicraftPlugin;
import fr.esicraft.guild.Guild;
import fr.esicraft.util.ChatUtil;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import org.bukkit.Bukkit;
import org.bukkit.entity.AreaEffectCloud;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.entity.Projectile;
import org.bukkit.entity.Tameable;
import org.bukkit.entity.TNTPrimed;
import org.bukkit.entity.ThrownPotion;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.entity.PotionSplashEvent;
import org.bukkit.event.player.PlayerCommandPreprocessEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.player.PlayerTeleportEvent;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

/**
 * Combat PvP : un joueur qui frappe ou est frappé par un autre joueur (coup, projectile, TNT, loup apprivoisé, potion, nuage) est
 * « en combat » quelques secondes. Il ne peut alors ni quitter vers le hub, ni se téléporter, ni cacher son argent ou ses objets
 * (commandes bloquées) ; se déconnecter pendant le combat est puni (mort sur place, objets lâchés), y compris par un kick.
 */
public class CombatManager implements Listener {
   private static final Set<PotionEffectType> HARMFUL = Set.of(PotionEffectType.INSTANT_DAMAGE, PotionEffectType.POISON, PotionEffectType.WITHER, PotionEffectType.WEAKNESS,
      PotionEffectType.SLOWNESS, PotionEffectType.MINING_FATIGUE, PotionEffectType.BLINDNESS, PotionEffectType.NAUSEA, PotionEffectType.HUNGER, PotionEffectType.LEVITATION);

   private final EsicraftPlugin plugin;
   private final Map<UUID, Long> taggedUntil = new ConcurrentHashMap<>();

   public CombatManager(EsicraftPlugin plugin) {
      this.plugin = plugin;
   }

   public void start() {
      Bukkit.getScheduler().runTaskTimer(this.plugin, this::tick, 10L, 10L);
   }

   private boolean enabled() {
      return this.plugin.getConfig().getBoolean("combat.enabled", true);
   }

   private long durationMs() {
      return this.plugin.getConfig().getLong("combat.seconds", 15L) * 1000L;
   }

   public boolean isInCombat(Player player) {
      Long until = this.taggedUntil.get(player.getUniqueId());
      return this.enabled() && until != null && until > System.currentTimeMillis();
   }

   public long remainingSeconds(Player player) {
      Long until = this.taggedUntil.get(player.getUniqueId());
      return until == null ? 0L : Math.max(0L, (until - System.currentTimeMillis() + 999L) / 1000L);
   }

   /** Joueur à l'origine d'un dégât, même indirect : projectile, TNT allumée, loup apprivoisé, nuage de potion. Null si aucun. */
   public static Player attackerOf(Entity damager) {
      if (damager instanceof Player p) {
         return p;
      }

      if (damager instanceof Projectile proj && proj.getShooter() instanceof Player p) {
         return p;
      }

      if (damager instanceof TNTPrimed tnt && tnt.getSource() instanceof Player p) {
         return p;
      }

      if (damager instanceof Tameable pet && pet.getOwner() instanceof Player p) {
         return p;
      }

      return damager instanceof AreaEffectCloud cloud && cloud.getSource() instanceof Player p ? p : null;
   }

   private void tag(Player player) {
      if (this.taggedUntil.put(player.getUniqueId(), System.currentTimeMillis() + this.durationMs()) == null) {
         ChatUtil.sendMessage(player, "&c⚔ Vous êtes en combat ! &7Téléportation, /hub, argent et coffres sont bloqués, et se déconnecter est puni (&e"
            + this.plugin.getConfig().getLong("combat.seconds", 15L) + " s&7).");
      }
   }

   private void tick() {
      long now = System.currentTimeMillis();
      Iterator<Map.Entry<UUID, Long>> it = this.taggedUntil.entrySet().iterator();
      while (it.hasNext()) {
         Map.Entry<UUID, Long> e = it.next();
         Player p = Bukkit.getPlayer(e.getKey());
         if (p == null) {
            it.remove();
         } else if (e.getValue() <= now) {
            it.remove();
            ChatUtil.sendActionbar(p, "&a✔ Vous n'êtes plus en combat.");
         } else {
            ChatUtil.sendActionbar(p, "&c⚔ En combat : &e" + ((e.getValue() - now + 999L) / 1000L) + " s");
         }
      }
   }

   private boolean sameGuild(Player a, Player b) {
      Guild ga = this.plugin.getGuildManager().getGuildByPlayer(a.getUniqueId());
      Guild gb = this.plugin.getGuildManager().getGuildByPlayer(b.getUniqueId());
      return ga != null && gb != null && ga.getId().equals(gb.getId());
   }

   @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
   public void onDamage(EntityDamageByEntityEvent event) {
      if (!this.enabled() || !(event.getEntity() instanceof Player victim)) {
         return;
      }

      Player attacker = attackerOf(event.getDamager());
      if (attacker == null || attacker.equals(victim) || this.sameGuild(attacker, victim)) {
         return;
      }

      this.tag(attacker);
      this.tag(victim);
   }

   @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
   public void onSplash(PotionSplashEvent event) {
      if (!this.enabled() || !(event.getPotion() instanceof ThrownPotion potion) || !(potion.getShooter() instanceof Player thrower)) {
         return;
      }

      boolean harmful = false;
      for (PotionEffect e : potion.getEffects()) {
         if (HARMFUL.contains(e.getType())) {
            harmful = true;
            break;
         }
      }

      if (!harmful) {
         return;
      }

      for (var target : event.getAffectedEntities()) {
         if (target instanceof Player victim && !victim.equals(thrower) && !this.sameGuild(thrower, victim) && event.getIntensity(victim) > 0.0) {
            this.tag(thrower);
            this.tag(victim);
         }
      }
   }

   @EventHandler
   public void onDeath(PlayerDeathEvent event) {
      this.taggedUntil.remove(event.getEntity().getUniqueId());
   }

   // ------------------------------------------------------------------ restrictions pendant le combat

   @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
   public void onCommand(PlayerCommandPreprocessEvent event) {
      Player p = event.getPlayer();
      if (!this.isInCombat(p)) {
         return;
      }

      String[] parts = event.getMessage().trim().toLowerCase(Locale.ROOT).split("\\s+");
      String label = parts[0].startsWith("/") ? parts[0].substring(1) : parts[0];
      int colon = label.indexOf(':');
      if (colon >= 0) {
         label = label.substring(colon + 1);
      }

      List<String> blocked = this.plugin.getConfig().getStringList("combat.blocked-commands");
      boolean block = blocked.contains(label);
      if (!block && List.of("guild", "g", "guilde", "faction").contains(label) && parts.length > 1) {
         block = this.plugin.getConfig().getStringList("combat.blocked-guild-subcommands").contains(parts[1]);
      }

      if (block) {
         event.setCancelled(true);
         ChatUtil.sendMessage(p, "&c❌ Commande impossible en plein combat (&e" + this.remainingSeconds(p) + " s&c).");
         ChatUtil.playSound(p, org.bukkit.Sound.ENTITY_VILLAGER_NO, 1.0F, 1.0F);
      }
   }

   @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
   public void onTeleport(PlayerTeleportEvent event) {
      PlayerTeleportEvent.TeleportCause cause = event.getCause();
      if ((cause == PlayerTeleportEvent.TeleportCause.COMMAND || cause == PlayerTeleportEvent.TeleportCause.PLUGIN) && this.isInCombat(event.getPlayer())) {
         event.setCancelled(true);
         ChatUtil.sendMessage(event.getPlayer(), "&c❌ Téléportation impossible en plein combat.");
      }
   }

   /** Le kick n'exempte plus : se faire expulser exprès (spam, double connexion) ne sauve plus d'une sanction. Seul l'arrêt du serveur est excusé. */
   @EventHandler(priority = EventPriority.HIGHEST)
   public void onQuit(PlayerQuitEvent event) {
      Player player = event.getPlayer();
      boolean punish = this.isInCombat(player) && !Bukkit.isStopping() && !player.isDead();
      this.taggedUntil.remove(player.getUniqueId());
      if (punish) {
         ChatUtil.broadcast("&4&l☠ &c" + player.getName() + " &7s'est déconnecté en plein combat et est mort sur place !");
         player.setHealth(0.0);
      }
   }
}
