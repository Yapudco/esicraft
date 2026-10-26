package fr.esicraft.life;

import fr.esicraft.EsicraftPlugin;
import fr.esicraft.util.ChatUtil;
import java.time.Duration;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import net.kyori.adventure.title.Title;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Item;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityPickupItemEvent;
import org.jetbrains.annotations.NotNull;

/**
 * Cosmétiques. Easter egg : la première fois qu'on donne un diamant à un autre joueur (on le lâche et il le ramasse),
 * on débloque l'« Aura de diamant » (étincelles autour de soi). {@code /cosmetique} l'active ou la désactive.
 */
public class CosmeticManager implements Listener, CommandExecutor {
   private static final String KEY_UNLOCKED = "cosmetic:diamond:";
   private static final String KEY_HIDDEN = "cosmetic_off:diamond:";

   private final EsicraftPlugin plugin;
   private final Set<UUID> unlocked = ConcurrentHashMap.newKeySet();
   private final Set<UUID> hidden = ConcurrentHashMap.newKeySet();

   public CosmeticManager(EsicraftPlugin plugin) {
      this.plugin = plugin;
   }

   public void init() {
      var db = this.plugin.getDataManager().getStorage();
      for (Object[] row : db.select("SELECT k FROM kv WHERE k LIKE 'cosmetic:diamond:%';", 1)) {
         this.parse((String)row[0], KEY_UNLOCKED, this.unlocked);
      }

      for (Object[] row : db.select("SELECT k FROM kv WHERE k LIKE 'cosmetic_off:diamond:%';", 1)) {
         this.parse((String)row[0], KEY_HIDDEN, this.hidden);
      }

      Bukkit.getScheduler().runTaskTimer(this.plugin, this::aura, 40L, 10L);
   }

   private void parse(String key, String prefix, Set<UUID> into) {
      try {
         into.add(UUID.fromString(key.substring(prefix.length())));
      } catch (IllegalArgumentException ignored) {
      }
   }

   public boolean hasDiamondAura(UUID uuid) {
      return this.unlocked.contains(uuid);
   }

   private void aura() {
      for (UUID uuid : this.unlocked) {
         Player p = Bukkit.getPlayer(uuid);
         if (p != null && !this.hidden.contains(uuid) && !p.isDead() && p.getGameMode() != org.bukkit.GameMode.SPECTATOR && this.plugin.getDisguiseManager().get(uuid) == null) {
            p.getWorld().spawnParticle(Particle.END_ROD, p.getLocation().add(0.0, 1.0, 0.0), 2, 0.45, 0.7, 0.45, 0.01);
         }
      }
   }

   private void setFlag(String key, boolean on) {
      Bukkit.getScheduler().runTaskAsynchronously(this.plugin, () -> {
         if (on) {
            this.plugin.getDataManager().getStorage().exec("INSERT INTO kv (k, v) VALUES (?, '1') ON CONFLICT(k) DO NOTHING;", key);
         } else {
            this.plugin.getDataManager().getStorage().exec("DELETE FROM kv WHERE k = ?;", key);
         }
      });
   }

   @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
   public void onPickup(EntityPickupItemEvent event) {
      if (!(event.getEntity() instanceof Player receiver)) {
         return;
      }

      Item dropped = event.getItem();
      UUID thrower = dropped.getThrower();
      if (thrower == null || thrower.equals(receiver.getUniqueId()) || dropped.getItemStack().getType() != Material.DIAMOND || this.unlocked.contains(thrower)) {
         return;
      }

      Player giver = Bukkit.getPlayer(thrower);
      if (giver == null || !this.unlocked.add(thrower)) {
         return;
      }

      this.setFlag(KEY_UNLOCKED + thrower, true);
      giver.showTitle(Title.title(ChatUtil.component("&b&l💎 Diamonds to you !"), ChatUtil.component("&7Cosmétique débloqué : &bAura de diamant &7(/cosmetique)"),
         Title.Times.times(Duration.ofMillis(300L), Duration.ofMillis(3000L), Duration.ofMillis(800L))));
      ChatUtil.sendMessage(giver, "&b💎 &lDiamonds to you ! &7Votre générosité est récompensée : vous débloquez l'&bAura de diamant&7. &e/cosmetique &7pour l'activer ou la cacher.");
      ChatUtil.playSound(giver, Sound.ENTITY_PLAYER_LEVELUP, 1.0F, 2.0F);
      ChatUtil.sendMessage(receiver, "&b💎 &e" + giver.getName() + " &7vous offre un diamant !");
      giver.getWorld().spawnParticle(Particle.END_ROD, giver.getLocation().add(0.0, 1.0, 0.0), 40, 0.5, 0.8, 0.5, 0.05);
   }

   public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
      if (!(sender instanceof Player player)) {
         sender.sendMessage(ChatUtil.color("&cSeul un joueur peut exécuter cette commande."));
         return true;
      }

      if (!this.unlocked.contains(player.getUniqueId())) {
         ChatUtil.sendMessage(player, "&7Vous n'avez débloqué aucun cosmétique. Il y a des secrets à découvrir… (indice : la générosité paie)");
         return true;
      }

      UUID uuid = player.getUniqueId();
      boolean wantOn = args.length > 0 ? args[0].equalsIgnoreCase("on") : this.hidden.contains(uuid);
      if (wantOn) {
         this.hidden.remove(uuid);
         this.setFlag(KEY_HIDDEN + uuid, false);
         ChatUtil.sendMessage(player, "&b💎 Aura de diamant &aactivée&b.");
      } else {
         this.hidden.add(uuid);
         this.setFlag(KEY_HIDDEN + uuid, true);
         ChatUtil.sendMessage(player, "&b💎 Aura de diamant &cdésactivée&b.");
      }

      return true;
   }
}
