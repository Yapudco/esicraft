package fr.esicraft.nexus;

import fr.esicraft.EsicraftPlugin;
import fr.esicraft.guild.Guild;
import fr.esicraft.util.ChatUtil;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ThreadLocalRandom;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Mob;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitTask;

/**
 * Roulette russe du Nexus ({@code /nexus gamble}) : le chef de guilde, face au Nexus, tente un coup de poker ultime.
 * Pile : +1 000 PV max et 2 000 pièces à la banque. Face : −1 500 PV et une mini-horde de pillards surgit sans préavis (le Nexus survit toujours à 1 PV).
 */
public class NexusGamble {
   private final EsicraftPlugin plugin;
   /** Chef ayant demandé confirmation → échéance. */
   private final Map<UUID, Long> pending = new ConcurrentHashMap<>();

   public NexusGamble(EsicraftPlugin plugin) {
      this.plugin = plugin;
   }

   private String key(Guild guild) {
      return "gamble:" + guild.getId();
   }

   private long lastGamble(Guild guild) {
      for (Object[] row : this.plugin.getDataManager().getStorage().select("SELECT v FROM kv WHERE k = ?;", 1, this.key(guild))) {
         try {
            return Long.parseLong(String.valueOf(row[0]));
         } catch (NumberFormatException ignored) {
         }
      }

      return 0L;
   }

   public void attempt(Player player, Guild guild) {
      if (!guild.getLeaderUuid().equals(player.getUniqueId())) {
         ChatUtil.sendMessage(player, "&c❌ Seul le chef de guilde peut tenter la roulette russe du Nexus.");
         return;
      }

      Nexus nexus = guild.getNexus();
      Location loc = nexus == null ? null : nexus.getLocation();
      if (nexus == null || !nexus.isAlive() || loc == null || loc.getWorld() == null) {
         ChatUtil.sendMessage(player, "&c❌ Votre guilde n'a pas de Nexus actif.");
         return;
      }

      if (!player.getWorld().equals(loc.getWorld()) || player.getLocation().distanceSquared(loc) > 36.0) {
         ChatUtil.sendMessage(player, "&c❌ Placez-vous face au Nexus (à moins de 6 blocs).");
         return;
      }

      if (this.plugin.getRaidManager().isGuildUnderRaid(guild.getId())) {
         ChatUtil.sendMessage(player, "&c❌ Impossible pendant un raid.");
         return;
      }

      long cooldownMs = this.plugin.getConfig().getLong("nexus.gamble.cooldown-hours", 24L) * 3600000L;
      long left = this.lastGamble(guild) + cooldownMs - System.currentTimeMillis();
      if (left > 0 && !this.plugin.bypassCooldowns(player)) {
         ChatUtil.sendMessage(player, "&c❌ Le Nexus a déjà tenté sa chance : revenez dans &e" + (left / 60000L) + " min&c.");
         return;
      }

      long now = System.currentTimeMillis();
      Long ask = this.pending.get(player.getUniqueId());
      if (ask == null || ask < now) {
         this.pending.put(player.getUniqueId(), now + 15000L);
         ChatUtil.sendMessage(player, "&4&l☠ ROULETTE RUSSE &c: 50 % de chance de gagner &a+1000 PV max &cet &62000 pièces&c, 50 % de perdre &41500 PV &cet de subir une horde de pillards. Retapez &e/nexus gamble &cdans 15 s pour confirmer.");
         return;
      }

      this.pending.remove(player.getUniqueId());
      String key = this.key(guild);
      Bukkit.getScheduler().runTaskAsynchronously(this.plugin, () -> this.plugin.getDataManager().getStorage().exec(
         "INSERT INTO kv (k, v) VALUES (?, ?) ON CONFLICT(k) DO UPDATE SET v = excluded.v;", key, String.valueOf(System.currentTimeMillis())));
      ChatUtil.broadcast("&4&l☠ " + player.getName() + " &c fait tourner la roulette russe du Nexus de &6" + guild.getName() + " &c…");
      loc.getWorld().playSound(loc, Sound.BLOCK_BEACON_DEACTIVATE, 2.0F, 0.6F);
      Bukkit.getScheduler().runTaskLater(this.plugin, () -> this.resolve(player, guild, nexus, loc), 40L);
   }

   private void resolve(Player player, Guild guild, Nexus nexus, Location loc) {
      if (!nexus.isAlive() || !guild.hasNexus()) {
         return;
      }

      if (ThreadLocalRandom.current().nextBoolean()) {
         int gainHp = this.plugin.getConfig().getInt("nexus.gamble.win-max-hp", 1000);
         double coins = this.plugin.getConfig().getDouble("nexus.gamble.win-coins", 2000.0);
         nexus.setMaxHealth(nexus.getMaxHealth() + gainHp);
         nexus.heal(gainHp);
         guild.depositBank(coins);
         this.plugin.getDataManager().saveGuild(guild);
         this.plugin.getNexusManager().updateHologram(guild);
         loc.getWorld().spawnParticle(Particle.TOTEM_OF_UNDYING, loc.clone().add(0.5, 1.0, 0.5), 80, 1.0, 1.0, 1.0, 0.3);
         loc.getWorld().playSound(loc, Sound.UI_TOAST_CHALLENGE_COMPLETE, 2.0F, 1.0F);
         this.plugin.getAchievements().grant(player.getUniqueId(), fr.esicraft.life.Achievement.NEXUS_GAMBLER);
         ChatUtil.broadcast("&a&l✔ LA CHANCE SOURIT À &6" + guild.getName() + " &a: Nexus &2+" + gainHp + " PV max&a et &6+" + (long)coins + " pièces &adans la banque !");
      } else {
         int loss = this.plugin.getConfig().getInt("nexus.gamble.lose-hp", 1500);
         nexus.setCurrentHealth(Math.max(1, nexus.getCurrentHealth() - loss));
         this.plugin.getDataManager().saveGuild(guild);
         this.plugin.getNexusManager().updateHologram(guild);
         loc.getWorld().spawnParticle(Particle.EXPLOSION_EMITTER, loc.clone().add(0.5, 1.0, 0.5), 3, 1.0, 0.5, 1.0, 0.0);
         loc.getWorld().playSound(loc, Sound.ENTITY_GENERIC_EXPLODE, 3.0F, 0.6F);
         ChatUtil.broadcast("&4&l✖ LA ROULETTE A TOURNÉ CONTRE &6" + guild.getName() + " &4: Nexus &c-" + loss + " PV&4 et une horde de pillards surgit !");
         this.spawnHorde(loc);
      }
   }

   private void spawnHorde(Location nexusLoc) {
      java.util.List<Mob> horde = new java.util.ArrayList<>();
      int count = Math.max(1, this.plugin.getConfig().getInt("nexus.gamble.horde-size", 6));
      for (int i = 0; i < count; i++) {
         double angle = Math.PI * 2 * i / count;
         Location s = nexusLoc.clone().add(Math.cos(angle) * 4.0, 1.0, Math.sin(angle) * 4.0);
         EntityType type = i == 0 ? EntityType.VINDICATOR : EntityType.PILLAGER;
         Mob mob = (Mob)nexusLoc.getWorld().spawnEntity(s, type);
         mob.setRemoveWhenFarAway(true);
         mob.setPersistent(false);
         horde.add(mob);
      }

      // La horde se dissipe au bout de 5 minutes si elle n'a pas été abattue.
      BukkitTask[] holder = new BukkitTask[1];
      holder[0] = Bukkit.getScheduler().runTaskLater(this.plugin, () -> {
         for (Mob m : horde) {
            if (m.isValid()) {
               m.remove();
            }
         }
      }, 6000L);
   }

   public void clear() {
      this.pending.clear();
   }
}
