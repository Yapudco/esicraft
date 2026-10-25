package fr.esicraft.combat;

import fr.esicraft.EsicraftPlugin;
import fr.esicraft.guild.Guild;
import fr.esicraft.util.ChatUtil;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.entity.Projectile;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.player.PlayerKickEvent;
import org.bukkit.event.player.PlayerQuitEvent;

/**
 * Combat PvP : un joueur qui frappe ou est frappé par un autre joueur est « en combat » quelques secondes.
 * Il ne peut pas utiliser /hub, et se déconnecter pendant le combat est puni (mort sur place, objets lâchés).
 */
public class CombatManager implements Listener {
   private final EsicraftPlugin plugin;
   private final Map<UUID, Long> taggedUntil = new ConcurrentHashMap<>();
   private final Set<UUID> kicked = ConcurrentHashMap.newKeySet();

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

   private void tag(Player player) {
      if (this.taggedUntil.put(player.getUniqueId(), System.currentTimeMillis() + this.durationMs()) == null) {
         ChatUtil.sendMessage(player, "&c⚔ Vous êtes en combat ! &7/hub est bloqué et se déconnecter est puni (&e" + this.plugin.getConfig().getLong("combat.seconds", 15L) + " s&7).");
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

   @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
   public void onDamage(EntityDamageByEntityEvent event) {
      if (!this.enabled() || !(event.getEntity() instanceof Player victim)) {
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
      if (a != null && b != null && a.getId().equals(b.getId())) {
         return;
      }

      this.tag(attacker);
      this.tag(victim);
   }

   @EventHandler
   public void onDeath(PlayerDeathEvent event) {
      this.taggedUntil.remove(event.getEntity().getUniqueId());
   }

   @EventHandler
   public void onKick(PlayerKickEvent event) {
      // Un kick (admin, redémarrage) n'est pas une fuite du joueur.
      this.kicked.add(event.getPlayer().getUniqueId());
   }

   @EventHandler(priority = EventPriority.HIGHEST)
   public void onQuit(PlayerQuitEvent event) {
      Player player = event.getPlayer();
      boolean wasKicked = this.kicked.remove(player.getUniqueId());
      boolean punish = this.isInCombat(player) && !wasKicked && !Bukkit.isStopping() && !player.isDead();
      this.taggedUntil.remove(player.getUniqueId());
      if (punish) {
         ChatUtil.broadcast("&4&l☠ &c" + player.getName() + " &7s'est déconnecté en plein combat et est mort sur place !");
         player.setHealth(0.0);
      }
   }
}
