package fr.esicraft.life;

import fr.esicraft.EsicraftPlugin;
import fr.esicraft.data.PlayerData;
import fr.esicraft.guild.Guild;
import fr.esicraft.util.ChatUtil;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ThreadLocalRandom;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.entity.Projectile;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.scoreboard.Scoreboard;
import org.bukkit.scoreboard.Team;

/**
 * Déguisement : pendant quelques minutes, un joueur se fait passer pour un membre d'une autre guilde (nom et étiquette de guilde
 * dans le tchat et la liste des joueurs, nom au-dessus de la tête masqué). Les pièges et le golem de cette guilde l'ignorent,
 * mais les protections de territoire restent actives. Infliger des dégâts le démasque aussitôt.
 */
public class DisguiseManager implements Listener {
   private static final String TEAM = "esi_masque";

   public record Disguise(String guildId, String tag, String guildName, String fakeName, long until) {
   }

   private final EsicraftPlugin plugin;
   private final Map<UUID, Disguise> active = new ConcurrentHashMap<>();
   private final Map<UUID, Long> cooldowns = new ConcurrentHashMap<>();

   public DisguiseManager(EsicraftPlugin plugin) {
      this.plugin = plugin;
   }

   public void init() {
      Bukkit.getScheduler().runTaskTimer(this.plugin, this::expire, 20L, 20L);
   }

   private long cost() {
      return Math.max(0L, this.plugin.getConfig().getLong("life.disguise.cost", 750L));
   }

   private long durationMs() {
      return Math.max(30L, this.plugin.getConfig().getLong("life.disguise.duration-seconds", 300L)) * 1000L;
   }

   private long cooldownMs() {
      return Math.max(0L, this.plugin.getConfig().getLong("life.disguise.cooldown-minutes", 30L)) * 60000L;
   }

   public Disguise get(UUID uuid) {
      Disguise d = this.active.get(uuid);
      return d != null && d.until() > System.currentTimeMillis() ? d : null;
   }

   public boolean isDisguisedAs(UUID uuid, String guildId) {
      Disguise d = this.get(uuid);
      return d != null && d.guildId().equals(guildId);
   }

   /** Renvoie un message d'erreur, ou null si le déguisement est en place. */
   public String disguise(Player player, Guild target) {
      if (!this.plugin.getConfig().getBoolean("life.disguise.enabled", true)) {
         return "&cLe déguisement est désactivé sur ce serveur.";
      }

      if (target == null) {
         return "&cGuilde introuvable.";
      }

      if (target.isMember(player.getUniqueId())) {
         return "&cVous ne pouvez pas vous déguiser en membre de votre propre guilde.";
      }

      if (this.get(player.getUniqueId()) != null) {
         return "&cVous êtes déjà déguisé (&e/deguisement off&c pour l'enlever).";
      }

      long now = System.currentTimeMillis();
      Long cd = this.cooldowns.get(player.getUniqueId());
      if (cd != null && cd > now) {
         return "&cVotre prochain déguisement sera prêt dans &e" + ((cd - now) / 60000L + 1L) + " min&c.";
      }

      PlayerData data = this.plugin.getDataManager().getPlayerData(player.getUniqueId());
      long cost = this.cost();
      if (data == null || !data.removeCoins(cost)) {
         return "&cIl vous faut &6" + cost + " pièces &cpour acheter un déguisement.";
      }

      // Emprunte le nom d'un membre hors ligne (sinon garde le sien) : deux joueurs du même nom en ligne trahiraient la ruse.
      List<String> candidates = new ArrayList<>();
      for (UUID member : target.getMembers().keySet()) {
         OfflinePlayer op = Bukkit.getOfflinePlayer(member);
         if (!op.isOnline() && op.getName() != null) {
            candidates.add(op.getName());
         }
      }

      String fake = candidates.isEmpty() ? player.getName() : candidates.get(ThreadLocalRandom.current().nextInt(candidates.size()));
      Disguise d = new Disguise(target.getId(), target.getTag(), target.getName(), fake, now + this.durationMs());
      this.active.put(player.getUniqueId(), d);
      this.cooldowns.put(player.getUniqueId(), now + this.cooldownMs());
      this.apply(player, d);
      this.plugin.getAchievements().grant(player.getUniqueId(), Achievement.SPY);
      player.getWorld().spawnParticle(Particle.SMOKE, player.getLocation().add(0.0, 1.0, 0.0), 30, 0.3, 0.6, 0.3, 0.02);
      ChatUtil.playSound(player, Sound.ITEM_ARMOR_EQUIP_LEATHER, 1.0F, 0.8F);
      return null;
   }

   private void apply(Player player, Disguise d) {
      player.displayName(ChatUtil.component("&f" + d.fakeName()));
      player.playerListName(ChatUtil.component("&8[&6" + d.tag() + "&8] &f" + d.fakeName()));
      for (Player viewer : Bukkit.getOnlinePlayers()) {
         this.hideName(viewer.getScoreboard(), player.getName(), true);
      }

      this.hideName(Bukkit.getScoreboardManager().getMainScoreboard(), player.getName(), true);
   }

   private void hideName(Scoreboard board, String entry, boolean hide) {
      if (board == null) {
         return;
      }

      Team team = board.getTeam(TEAM);
      if (hide) {
         if (team == null) {
            team = board.registerNewTeam(TEAM);
            team.setOption(Team.Option.NAME_TAG_VISIBILITY, Team.OptionStatus.NEVER);
         }

         team.addEntry(entry);
      } else if (team != null) {
         team.removeEntry(entry);
      }
   }

   /** Applique les noms masqués à un tableau de bord tout juste créé (HUD). */
   public void syncBoard(Scoreboard board) {
      for (UUID uuid : this.active.keySet()) {
         Player p = Bukkit.getPlayer(uuid);
         if (p != null && this.get(uuid) != null) {
            this.hideName(board, p.getName(), true);
         }
      }
   }

   public void reveal(Player player, boolean exposed) {
      Disguise d = this.active.remove(player.getUniqueId());
      if (d == null) {
         return;
      }

      player.displayName(null);
      this.plugin.getGuildManager().updatePlayerTab(player);
      for (Player viewer : Bukkit.getOnlinePlayers()) {
         this.hideName(viewer.getScoreboard(), player.getName(), false);
      }

      this.hideName(Bukkit.getScoreboardManager().getMainScoreboard(), player.getName(), false);
      if (exposed) {
         ChatUtil.sendMessage(player, "&c🎭 Vous avez été démasqué !");
         Guild target = this.plugin.getGuildManager().getGuildById(d.guildId());
         if (target != null) {
            ChatUtil.broadcastGuild(target, "&c🎭 Espion démasqué : &e" + player.getName() + " &cse faisait passer pour &e" + d.fakeName() + " &c!");
         }
      } else if (player.isOnline()) {
         ChatUtil.sendMessage(player, "&7🎭 Votre déguisement est tombé.");
      }
   }

   private void expire() {
      long now = System.currentTimeMillis();
      for (Map.Entry<UUID, Disguise> e : this.active.entrySet()) {
         if (e.getValue().until() <= now) {
            Player p = Bukkit.getPlayer(e.getKey());
            if (p != null) {
               this.reveal(p, false);
            } else {
               this.active.remove(e.getKey());
            }
         }
      }
   }

   @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
   public void onDamage(EntityDamageByEntityEvent event) {
      Entity damager = event.getDamager();
      Player attacker = damager instanceof Player p ? p : damager instanceof Projectile proj && proj.getShooter() instanceof Player shooter ? shooter : null;
      if (attacker != null && this.get(attacker.getUniqueId()) != null) {
         this.reveal(attacker, true);
      }
   }

   @EventHandler
   public void onQuit(PlayerQuitEvent event) {
      if (this.active.containsKey(event.getPlayer().getUniqueId())) {
         this.reveal(event.getPlayer(), false);
      }
   }

   @EventHandler
   public void onJoin(PlayerJoinEvent event) {
      this.syncBoard(event.getPlayer().getScoreboard());
   }
}
