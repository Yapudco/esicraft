package fr.esicraft.life;

import com.destroystokyo.paper.profile.PlayerProfile;
import fr.esicraft.EsicraftPlugin;
import fr.esicraft.data.PlayerData;
import fr.esicraft.guild.Guild;
import fr.esicraft.util.ChatUtil;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ThreadLocalRandom;
import java.util.function.Consumer;
import java.util.regex.Pattern;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.scoreboard.Scoreboard;
import org.bukkit.scoreboard.Team;

/**
 * Déguisement : pendant quelques minutes, un joueur change de <b>skin et de pseudo</b>.
 * <ul>
 *   <li>{@code /deguisement <guilde>} : il se fait passer pour un membre de cette guilde (skin d'un de ses membres, pseudonyme inventé,
 *       étiquette de guilde) ; les pièges et le golem de cette guilde l'ignorent.</li>
 *   <li>{@code /deguisement <Pseudo>} : il prend le skin et le pseudo de n'importe quel compte Minecraft (pas forcément du serveur) ;
 *       si ce joueur est membre d'une autre guilde du serveur, il emprunte aussi son identité de guilde.</li>
 * </ul>
 * Le skin est appliqué par le profil du joueur (rafraîchi chez tous les autres) et restauré quand le déguisement tombe.
 * Infliger des dégâts le démasque aussitôt ; les protections de territoire restent actives.
 */
public class DisguiseManager implements Listener {
   private static final String TEAM = "esi_masque";
   private static final Pattern NAME = Pattern.compile("[A-Za-z0-9_]{3,16}");
   private static final String[] ALIASES = {"Ombre", "Corbeau", "Passant", "Voyageur", "Marchand", "Pelerin", "Eclaireur", "Vagabond"};

   /** {@code guildId} / {@code tag} / {@code guildName} vides : déguisement purement cosmétique (compte sans guilde sur le serveur). */
   public record Disguise(String guildId, String tag, String guildName, String fakeName, long until) {
      public boolean hasGuild() {
         return !this.guildId.isEmpty();
      }
   }

   private final EsicraftPlugin plugin;
   private final Map<UUID, Disguise> active = new ConcurrentHashMap<>();
   private final Map<UUID, Long> cooldowns = new ConcurrentHashMap<>();
   /** Profil d'origine (nom et skin réels), rendu au joueur quand le déguisement tombe. */
   private final Map<UUID, PlayerProfile> originals = new ConcurrentHashMap<>();
   /** Repli si le changement de profil échoue : nom réel caché au-dessus de la tête. */
   private final Map<UUID, String> hiddenNames = new ConcurrentHashMap<>();
   private final Set<UUID> pending = ConcurrentHashMap.newKeySet();

   public DisguiseManager(EsicraftPlugin plugin) {
      this.plugin = plugin;
   }

   public void init() {
      Bukkit.getScheduler().runTaskTimer(this.plugin, this::expire, 20L, 20L);
   }

   /** Restaure les vrais profils (arrêt ou rechargement du plugin). */
   public void shutdown() {
      for (UUID uuid : new ArrayList<>(this.active.keySet())) {
         Player p = Bukkit.getPlayer(uuid);
         if (p != null) {
            this.reveal(p, false);
         }
      }
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
      return d != null && d.hasGuild() && d.guildId().equals(guildId);
   }

   /** Nom réel d'un joueur déguisé (le nom du profil change avec le skin). */
   public String realName(Player player) {
      PlayerProfile original = this.originals.get(player.getUniqueId());
      return original != null && original.getName() != null ? original.getName() : player.getName();
   }

   // ------------------------------------------------------------------ mise en place

   /**
    * Lance un déguisement ({@code arg} : nom de guilde ou pseudo Minecraft). Le skin est récupéré chez Mojang en tâche de fond ;
    * {@code done} reçoit un message d'erreur, ou null quand le déguisement est en place. Les pièces ne sont débitées qu'au succès.
    */
   public void disguise(Player player, String arg, Consumer<String> done) {
      if (!this.plugin.getConfig().getBoolean("life.disguise.enabled", true)) {
         done.accept("&cLe déguisement est désactivé sur ce serveur.");
         return;
      }

      UUID uuid = player.getUniqueId();
      if (this.get(uuid) != null) {
         done.accept("&cVous êtes déjà déguisé (&e/deguisement off&c pour l'enlever).");
         return;
      }

      if (this.pending.contains(uuid)) {
         done.accept("&7Déguisement en préparation…");
         return;
      }

      long now = System.currentTimeMillis();
      Long cd = this.cooldowns.get(uuid);
      if (cd != null && cd > now) {
         done.accept("&cVotre prochain déguisement sera prêt dans &e" + ((cd - now) / 60000L + 1L) + " min&c.");
         return;
      }

      if (this.plugin.getCombatManager().isInCombat(player)) {
         done.accept("&cImpossible de s'acheter un déguisement en plein combat.");
         return;
      }

      PlayerData data = this.plugin.getDataManager().getPlayerData(uuid);
      long cost = this.cost();
      if (data == null || data.getCoins() < cost) {
         done.accept("&cIl vous faut &6" + cost + " pièces &cpour acheter un déguisement.");
         return;
      }

      Guild guildTarget = this.plugin.getGuildManager().getGuildByName(arg);
      Guild identity;
      String skinName;
      UUID skinUuid = null;
      String fakeName;
      if (guildTarget != null) {
         if (guildTarget.isMember(uuid)) {
            done.accept("&cVous ne pouvez pas vous déguiser en membre de votre propre guilde.");
            return;
         }

         List<UUID> members = new ArrayList<>(guildTarget.getMembers().keySet());
         skinUuid = members.get(ThreadLocalRandom.current().nextInt(members.size()));
         skinName = Bukkit.getOfflinePlayer(skinUuid).getName();
         fakeName = this.pseudonym();
         identity = guildTarget;
      } else if (NAME.matcher(arg).matches()) {
         if (arg.equalsIgnoreCase(player.getName()) || arg.equalsIgnoreCase(this.realName(player))) {
            done.accept("&cVous ne pouvez pas vous déguiser en vous-même.");
            return;
         }

         if (Bukkit.getPlayerExact(arg) != null) {
            done.accept("&cCe joueur est connecté : deux joueurs du même nom trahiraient la ruse.");
            return;
         }

         skinName = arg;
         fakeName = arg;
         identity = null;
         OfflinePlayer known = Bukkit.getOfflinePlayerIfCached(arg);
         if (known != null) {
            Guild g = this.plugin.getGuildManager().getGuildByPlayer(known.getUniqueId());
            if (g != null && !g.isMember(uuid)) {
               identity = g;
               fakeName = known.getName() != null ? known.getName() : arg;
            }
         }
      } else {
         done.accept("&cGuilde ou pseudo Minecraft introuvable : &e" + arg);
         return;
      }

      PlayerProfile wanted = skinUuid != null && skinName != null ? Bukkit.createProfile(skinUuid, skinName) : Bukkit.createProfile(skinName != null ? skinName : fakeName);
      this.pending.add(uuid);
      Guild finalIdentity = identity;
      String finalFake = fakeName;
      boolean requireSkin = guildTarget == null;
      ChatUtil.sendMessage(player, "&7🎭 Récupération du skin en cours…");
      wanted.update().whenComplete((fetched, error) -> Bukkit.getScheduler().runTask(this.plugin, () -> {
         this.pending.remove(uuid);
         if (!player.isOnline()) {
            return;
         }

         boolean hasSkin = error == null && fetched != null && fetched.hasTextures();
         if (requireSkin && !hasSkin) {
            done.accept("&cSkin introuvable pour &e" + arg + " &c(ce compte Minecraft n'existe pas ou Mojang ne répond pas). Aucune pièce débitée.");
            return;
         }

         if (this.get(uuid) != null) {
            done.accept("&cVous êtes déjà déguisé.");
            return;
         }

         PlayerData fresh = this.plugin.getDataManager().getPlayerData(uuid);
         if (fresh == null || !fresh.removeCoins(cost)) {
            done.accept("&cIl vous faut &6" + cost + " pièces &cpour acheter un déguisement.");
            return;
         }

         this.plugin.getDataManager().savePlayer(fresh);
         long until = System.currentTimeMillis() + this.durationMs();
         Disguise d = finalIdentity != null
            ? new Disguise(finalIdentity.getId(), finalIdentity.getTag(), finalIdentity.getName(), finalFake, until)
            : new Disguise("", "", "", finalFake, until);
         this.active.put(uuid, d);
         this.cooldowns.put(uuid, System.currentTimeMillis() + this.cooldownMs());
         this.apply(player, d, hasSkin ? fetched : null);
         this.plugin.getAchievements().grant(uuid, Achievement.SPY);
         player.getWorld().spawnParticle(Particle.SMOKE, player.getLocation().add(0.0, 1.0, 0.0), 30, 0.3, 0.6, 0.3, 0.02);
         ChatUtil.playSound(player, Sound.ITEM_ARMOR_EQUIP_LEATHER, 1.0F, 0.8F);
         done.accept(null);
      }));
   }

   private String pseudonym() {
      String fake;
      int tries = 0;
      do {
         fake = ALIASES[ThreadLocalRandom.current().nextInt(ALIASES.length)] + "_" + (100 + ThreadLocalRandom.current().nextInt(900));
         tries++;
      } while (Bukkit.getOfflinePlayerIfCached(fake) != null && tries < 20);
      return fake;
   }

   /** Change le profil (skin et nom, rafraîchi chez tous les joueurs) ; en cas d'échec, repli sur le simple masquage du nom. */
   private void apply(Player player, Disguise d, PlayerProfile skinSource) {
      UUID uuid = player.getUniqueId();
      PlayerProfile original = player.getPlayerProfile();
      this.originals.put(uuid, original);
      boolean swapped = false;
      try {
         PlayerProfile fake = Bukkit.createProfile(uuid, d.fakeName());
         if (skinSource != null) {
            fake.setProperties(skinSource.getProperties());
         }

         player.setPlayerProfile(fake);
         swapped = true;
      } catch (RuntimeException e) {
         this.plugin.getLogger().warning("Déguisement : changement de profil impossible (" + e.getMessage() + "), repli sur le masquage du nom.");
      }

      player.displayName(ChatUtil.component("&f" + d.fakeName()));
      player.playerListName(ChatUtil.component((d.hasGuild() ? "&8[&6" + d.tag() + "&8] &f" : "&8[&7Sans Guilde&8] &f") + d.fakeName()));
      if (!swapped) {
         String real = original.getName() != null ? original.getName() : player.getName();
         this.hiddenNames.put(uuid, real);
         for (Player viewer : Bukkit.getOnlinePlayers()) {
            this.hideName(viewer.getScoreboard(), real, true);
         }

         this.hideName(Bukkit.getScoreboardManager().getMainScoreboard(), real, true);
      }
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

   /** Applique les noms masqués (repli) à un tableau de bord tout juste créé (HUD). */
   public void syncBoard(Scoreboard board) {
      for (String hidden : this.hiddenNames.values()) {
         this.hideName(board, hidden, true);
      }
   }

   // ------------------------------------------------------------------ fin du déguisement

   /** Fait tomber le déguisement : profil réel (nom et skin) restauré et rafraîchi chez tous les joueurs. */
   public void reveal(Player player, boolean exposed) {
      Disguise d = this.active.remove(player.getUniqueId());
      if (d == null) {
         return;
      }

      PlayerProfile original = this.originals.remove(player.getUniqueId());
      String hidden = this.hiddenNames.remove(player.getUniqueId());
      if (original != null && player.isOnline()) {
         try {
            player.setPlayerProfile(original);
         } catch (RuntimeException e) {
            this.plugin.getLogger().warning("Déguisement : profil d'origine non restauré (" + e.getMessage() + ")");
         }
      }

      player.displayName(null);
      this.plugin.getGuildManager().updatePlayerTab(player);
      if (hidden != null) {
         for (Player viewer : Bukkit.getOnlinePlayers()) {
            this.hideName(viewer.getScoreboard(), hidden, false);
         }

         this.hideName(Bukkit.getScoreboardManager().getMainScoreboard(), hidden, false);
      }

      if (exposed) {
         ChatUtil.sendMessage(player, "&c🎭 Vous avez été démasqué !");
         Guild target = d.hasGuild() ? this.plugin.getGuildManager().getGuildById(d.guildId()) : null;
         if (target != null) {
            ChatUtil.broadcastGuild(target, "&c🎭 Espion démasqué : &e" + player.getName() + " &cse faisait passer pour &e" + d.fakeName() + " &c!");
         }
      } else if (player.isOnline()) {
         ChatUtil.sendMessage(player, "&7🎭 Votre déguisement est tombé.");
      }
   }

   /** Déconnexion : le profil se réinitialise de lui-même à la reconnexion, on oublie simplement l'état. */
   private void forget(Player player) {
      this.active.remove(player.getUniqueId());
      this.originals.remove(player.getUniqueId());
      String hidden = this.hiddenNames.remove(player.getUniqueId());
      if (hidden != null) {
         for (Player viewer : Bukkit.getOnlinePlayers()) {
            this.hideName(viewer.getScoreboard(), hidden, false);
         }

         this.hideName(Bukkit.getScoreboardManager().getMainScoreboard(), hidden, false);
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
               this.originals.remove(e.getKey());
               this.hiddenNames.remove(e.getKey());
            }
         }
      }
   }

   @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
   public void onDamage(EntityDamageByEntityEvent event) {
      Player attacker = fr.esicraft.combat.CombatManager.attackerOf(event.getDamager());
      if (attacker != null && this.get(attacker.getUniqueId()) != null) {
         this.reveal(attacker, true);
      }
   }

   /** Les messages de mort ne trahissent pas le vrai nom d'un espion (victime ou tueur). */
   @EventHandler(priority = EventPriority.HIGH)
   public void onDeathMessage(org.bukkit.event.entity.PlayerDeathEvent event) {
      net.kyori.adventure.text.Component message = event.deathMessage();
      if (message == null) {
         return;
      }

      for (Player p : new Player[]{event.getEntity(), event.getEntity().getKiller()}) {
         Disguise d = p != null ? this.get(p.getUniqueId()) : null;
         if (d != null) {
            message = message.replaceText(net.kyori.adventure.text.TextReplacementConfig.builder().matchLiteral(this.realName(p)).replacement(d.fakeName()).build());
         }
      }

      event.deathMessage(message);
   }

   @EventHandler
   public void onQuit(PlayerQuitEvent event) {
      this.pending.remove(event.getPlayer().getUniqueId());
      if (this.active.containsKey(event.getPlayer().getUniqueId())) {
         this.forget(event.getPlayer());
      }
   }

   @EventHandler
   public void onJoin(PlayerJoinEvent event) {
      this.syncBoard(event.getPlayer().getScoreboard());
   }
}
