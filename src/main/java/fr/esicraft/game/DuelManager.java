package fr.esicraft.game;

import fr.esicraft.EsicraftPlugin;
import fr.esicraft.data.PlayerData;
import fr.esicraft.util.ChatUtil;
import java.util.ArrayList;
import java.util.Base64;
import java.util.Collection;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.World;
import org.bukkit.attribute.Attribute;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.player.PlayerCommandPreprocessEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.player.PlayerRespawnEvent;
import org.bukkit.event.player.PlayerTeleportEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.potion.PotionEffect;
import org.jetbrains.annotations.NotNull;

/**
 * Duels d'honneur ({@code /duel <joueur> [mise]}) : arène neutre dans le ciel, inventaire et état sauvegardés puis rendus à la fin, aucune mort réelle,
 * aucune rivalité de guilde ni agression diplomatique. Le vainqueur remporte la mise de l'adversaire ; les spectateurs peuvent parier ({@code /pari}).
 */
public class DuelManager implements CommandExecutor, TabCompleter, Listener {
   private static final class Challenge {
      final UUID from;
      final double stake;
      final long expires;

      Challenge(UUID from, double stake, long expires) {
         this.from = from;
         this.stake = stake;
         this.expires = expires;
      }
   }

   private static final class Duel {
      final String id = Long.toString(System.nanoTime(), 36);
      final Player a;
      final Player b;
      final double stake;
      long startedAt;
      boolean live;
      boolean over;

      Duel(Player a, Player b, double stake) {
         this.a = a;
         this.b = b;
         this.stake = stake;
      }

      boolean has(Player p) {
         return this.a.equals(p) || this.b.equals(p);
      }

      Player other(Player p) {
         return this.a.equals(p) ? this.b : this.a;
      }
   }

   private final EsicraftPlugin plugin;
   private final Map<UUID, Challenge> challenges = new ConcurrentHashMap<>();
   private final Map<UUID, Duel> duels = new ConcurrentHashMap<>();
   private boolean teleporting;

   public DuelManager(EsicraftPlugin plugin) {
      this.plugin = plugin;
   }

   public void init() {
      this.plugin.getTruce().addExemption(this::isDueling);
      Bukkit.getScheduler().runTaskTimer(this.plugin, this::tick, 20L, 20L);
   }

   public boolean isDueling(Player p) {
      return p != null && this.duels.containsKey(p.getUniqueId());
   }

   /** Les deux joueurs s'affrontent-ils en duel ? (aucune conséquence diplomatique, ni marque de combat). */
   public boolean inDuelWith(Player a, Player b) {
      Duel d = a == null ? null : this.duels.get(a.getUniqueId());
      return d != null && b != null && d.has(b);
   }

   private double minStake() {
      return Math.max(0.0, this.plugin.getConfig().getDouble("duels.min-stake", 0.0));
   }

   // ------------------------------------------------------------------ commande

   public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
      if (!(sender instanceof Player player)) {
         sender.sendMessage(ChatUtil.color("&cSeul un joueur peut exécuter cette commande."));
         return true;
      }

      if (!this.plugin.getConfig().getBoolean("duels.enabled", true)) {
         ChatUtil.sendMessage(player, "&cLes duels sont désactivés.");
         return true;
      }

      if (args.length == 0) {
         ChatUtil.sendMessage(player, "&6⚔ Duels d'honneur : &e/duel <joueur> [mise]&7, puis &e/duel accepter &7ou &e/duel refuser&7. Inventaire sauvegardé, aucune mort, aucune rivalité.");
         return true;
      }

      String sub = args[0].toLowerCase(Locale.ROOT);
      if (sub.equals("accepter") || sub.equals("accept")) {
         this.accept(player);
      } else if (sub.equals("refuser") || sub.equals("deny")) {
         Challenge c = this.challenges.remove(player.getUniqueId());
         ChatUtil.sendMessage(player, c == null ? "&cAucun défi en attente." : "&7Défi refusé.");
         if (c != null) {
            ChatUtil.sendMessage(Bukkit.getPlayer(c.from), "&e" + player.getName() + " &7a refusé votre défi.");
         }
      } else {
         this.challenge(player, args);
      }

      return true;
   }

   private boolean busy(Player p) {
      return this.duels.containsKey(p.getUniqueId());
   }

   private void challenge(Player player, String[] args) {
      Player target = Bukkit.getPlayerExact(args[0]);
      if (target == null || target.equals(player)) {
         ChatUtil.sendMessage(player, "&c❌ Joueur introuvable (il doit être connecté, et différent de vous).");
         return;
      }

      double stake = 0.0;
      if (args.length > 1) {
         try {
            stake = Math.floor(Double.parseDouble(args[1].replace(',', '.')));
         } catch (NumberFormatException e) {
            ChatUtil.sendMessage(player, "&c❌ Mise invalide.");
            return;
         }
      }

      double max = Math.max(0.0, this.plugin.getConfig().getDouble("duels.max-stake", 10000.0));
      if (Double.isNaN(stake) || stake < Math.max(0.0, this.minStake()) || stake > max) {
         ChatUtil.sendMessage(player, "&c❌ La mise doit être comprise entre &e" + (long)this.minStake() + " &cet &e" + (long)max + " &cpièces.");
         return;
      }

      if (this.busy(player) || this.busy(target)) {
         ChatUtil.sendMessage(player, "&c❌ L'un de vous est déjà en duel.");
         return;
      }

      if (this.plugin.getCombatManager().isInCombat(player) || this.plugin.getCombatManager().isInCombat(target)) {
         ChatUtil.sendMessage(player, "&c❌ Impossible en plein combat.");
         return;
      }

      this.challenges.put(target.getUniqueId(), new Challenge(player.getUniqueId(), stake, System.currentTimeMillis() + 60000L));
      String stakeText = stake > 0 ? " avec une mise de &6" + (long)stake + " pièces" : " sans mise";
      ChatUtil.sendMessage(player, "&6⚔ Défi envoyé à &e" + target.getName() + "&6" + stakeText + "&6 (60 s).");
      ChatUtil.sendMessage(target, "&6⚔ &e" + player.getName() + " &6vous défie en duel" + stakeText + "&6 ! &e/duel accepter &7ou &e/duel refuser");
      ChatUtil.playSound(target, Sound.BLOCK_NOTE_BLOCK_BELL, 1.0F, 1.2F);
   }

   private void accept(Player player) {
      Challenge c = this.challenges.remove(player.getUniqueId());
      if (c == null || c.expires < System.currentTimeMillis()) {
         ChatUtil.sendMessage(player, "&c❌ Aucun défi en attente (ou il a expiré).");
         return;
      }

      Player challenger = Bukkit.getPlayer(c.from);
      if (challenger == null || this.busy(challenger) || this.busy(player)) {
         ChatUtil.sendMessage(player, "&c❌ Le duel n'est plus possible.");
         return;
      }

      if (this.plugin.getCombatManager().isInCombat(player) || this.plugin.getCombatManager().isInCombat(challenger)) {
         ChatUtil.sendMessage(player, "&c❌ Impossible en plein combat.");
         return;
      }

      PlayerData da = this.plugin.getDataManager().getPlayerData(challenger.getUniqueId());
      PlayerData db = this.plugin.getDataManager().getPlayerData(player.getUniqueId());
      if (da == null || db == null || da.getCoins() < c.stake || db.getCoins() < c.stake) {
         ChatUtil.sendMessage(player, "&c❌ L'un des deux duellistes n'a pas assez de pièces pour la mise.");
         ChatUtil.sendMessage(challenger, "&c❌ L'un des deux duellistes n'a pas assez de pièces pour la mise.");
         return;
      }

      Location arena = this.arenaCenter();
      if (arena == null) {
         ChatUtil.sendMessage(player, "&c❌ Aucune arène disponible.");
         return;
      }

      if (c.stake > 0) {
         da.removeCoins(c.stake);
         db.removeCoins(c.stake);
         this.plugin.getDataManager().savePlayer(da);
         this.plugin.getDataManager().savePlayer(db);
      }

      Duel duel = new Duel(challenger, player, c.stake);
      this.duels.put(challenger.getUniqueId(), duel);
      this.duels.put(player.getUniqueId(), duel);
      this.snapshot(challenger);
      this.snapshot(player);
      this.buildArena(arena);
      this.teleporting = true;
      try {
         challenger.teleport(arena.clone().add(-9.5, 1, 0.5));
         player.teleport(arena.clone().add(10.5, 1, 0.5));
      } finally {
         this.teleporting = false;
      }

      for (Player p : List.of(challenger, player)) {
         p.setHealth(p.getAttribute(Attribute.MAX_HEALTH).getValue());
         p.setFoodLevel(20);
         p.setSaturation(20f);
         p.setFireTicks(0);
         p.setGameMode(GameMode.SURVIVAL);
         for (PotionEffect e : new ArrayList<>(p.getActivePotionEffects())) {
            p.removePotionEffect(e.getType());
         }
      }

      ChatUtil.broadcast("&6⚔ &e" + challenger.getName() + " &7et &e" + player.getName() + " &7s'affrontent en duel d'honneur" + (c.stake > 0 ? " (mise : &6" + (long)c.stake + " pièces&7)" : "") + " !");
      this.plugin.getBetting().openDuel(duel.id, challenger, player);
      duel.startedAt = System.currentTimeMillis() + 5000L;
      for (int i = 5; i >= 1; i--) {
         int n = i;
         Bukkit.getScheduler().runTaskLater(this.plugin, () -> {
            if (!duel.over) {
               for (Player p : List.of(duel.a, duel.b)) {
                  ChatUtil.sendActionbar(p, "&6⚔ Combat dans &e" + n + " &6…");
                  ChatUtil.playSound(p, Sound.BLOCK_NOTE_BLOCK_HAT, 1.0F, 1.0F);
               }
            }
         }, (5 - i) * 20L);
      }

      Bukkit.getScheduler().runTaskLater(this.plugin, () -> {
         if (!duel.over) {
            duel.live = true;
            for (Player p : List.of(duel.a, duel.b)) {
               ChatUtil.sendActionbar(p, "&c&l⚔ COMBATTEZ !");
               ChatUtil.playSound(p, Sound.ENTITY_ENDER_DRAGON_GROWL, 0.6F, 1.5F);
            }
         }
      }, 100L);
   }

   // ------------------------------------------------------------------ arène

   private Location arenaCenter() {
      World world = null;
      String name = this.plugin.getConfig().getString("duels.arena.world", "");
      if (name != null && !name.isEmpty()) {
         world = Bukkit.getWorld(name);
      }

      if (world == null) {
         for (World w : Bukkit.getWorlds()) {
            if (w.getEnvironment() == World.Environment.NORMAL) {
               world = w;
               break;
            }
         }
      }

      if (world == null) {
         return null;
      }

      int x = this.plugin.getConfig().getInt("duels.arena.x", Integer.MIN_VALUE);
      int z = this.plugin.getConfig().getInt("duels.arena.z", Integer.MIN_VALUE);
      if (x == Integer.MIN_VALUE || z == Integer.MIN_VALUE) {
         x = world.getSpawnLocation().getBlockX();
         z = world.getSpawnLocation().getBlockZ();
      }

      int y = Math.min(world.getMaxHeight() - 20, this.plugin.getConfig().getInt("duels.arena.y", 280));
      return new Location(world, x + 0.5, y, z + 0.5);
   }

   /** Plateforme éclairée de 31 × 31 entourée de barrières (construite une seule fois). */
   private void buildArena(Location c) {
      World w = c.getWorld();
      int cx = c.getBlockX();
      int cy = c.getBlockY();
      int cz = c.getBlockZ();
      if (w.getBlockAt(cx, cy, cz).getType() == Material.POLISHED_BLACKSTONE_BRICKS || w.getBlockAt(cx, cy, cz).getType() == Material.SEA_LANTERN) {
         return;
      }

      int r = 15;
      for (int x = -r; x <= r; x++) {
         for (int z = -r; z <= r; z++) {
            w.getChunkAt((cx + x) >> 4, (cz + z) >> 4);
            w.getBlockAt(cx + x, cy, cz + z).setType((x + z) % 2 == 0 ? Material.SEA_LANTERN : Material.POLISHED_BLACKSTONE_BRICKS, false);
            for (int h = 1; h <= 10; h++) {
               boolean wall = Math.abs(x) == r || Math.abs(z) == r;
               w.getBlockAt(cx + x, cy + h, cz + z).setType(wall || h == 10 ? Material.BARRIER : Material.AIR, false);
            }
         }
      }
   }

   // ------------------------------------------------------------------ sauvegarde de l'état

   private static String encodeLoc(Location l) {
      return l.getWorld().getName() + "," + l.getX() + "," + l.getY() + "," + l.getZ() + "," + l.getYaw() + "," + l.getPitch();
   }

   private static Location decodeLoc(String s) {
      String[] p = s.split(",");
      World w = Bukkit.getWorld(p[0]);
      return w == null ? null : new Location(w, Double.parseDouble(p[1]), Double.parseDouble(p[2]), Double.parseDouble(p[3]), Float.parseFloat(p[4]), Float.parseFloat(p[5]));
   }

   private void snapshot(Player p) {
      byte[] inv = ItemStack.serializeItemsAsBytes(p.getInventory().getContents());
      String value = Base64.getEncoder().encodeToString(inv) + ";" + encodeLoc(p.getLocation()) + ";" + p.getLevel() + ";" + p.getExp() + ";" + p.getHealth() + ";" + p.getFoodLevel() + ";" + p.getSaturation();
      this.plugin.getDataManager().getStorage().exec("INSERT INTO kv (k, v) VALUES (?, ?) ON CONFLICT(k) DO UPDATE SET v = excluded.v;", "duel_snap:" + p.getUniqueId(), value);
      p.getInventory().clear();
   }

   /** Rend l'inventaire et l'état d'avant-duel ; renvoie la position d'origine. */
   private boolean restore(Player p) {
      String key = "duel_snap:" + p.getUniqueId();
      List<Object[]> rows = this.plugin.getDataManager().getStorage().select("SELECT v FROM kv WHERE k = ?;", 1, key);
      if (rows.isEmpty()) {
         return false;
      }

      try {
         String[] parts = String.valueOf(rows.get(0)[0]).split(";");
         ItemStack[] items = ItemStack.deserializeItemsFromBytes(Base64.getDecoder().decode(parts[0]));
         p.getInventory().clear();
         p.getInventory().setContents(items);
         p.setLevel(Integer.parseInt(parts[2]));
         p.setExp(Float.parseFloat(parts[3]));
         double max = p.getAttribute(Attribute.MAX_HEALTH).getValue();
         p.setHealth(Math.max(1.0, Math.min(max, Double.parseDouble(parts[4]))));
         p.setFoodLevel(Integer.parseInt(parts[5]));
         p.setSaturation(Float.parseFloat(parts[6]));
         for (PotionEffect e : new ArrayList<>(p.getActivePotionEffects())) {
            p.removePotionEffect(e.getType());
         }

         Location origin = decodeLoc(parts[1]);
         this.teleporting = true;
         try {
            if (origin != null) {
               p.teleport(origin);
            }
         } finally {
            this.teleporting = false;
         }
      } catch (RuntimeException e) {
         this.plugin.getLogger().warning("Restauration du duel impossible pour " + p.getName() + " : " + e.getMessage());
      }

      this.plugin.getDataManager().getStorage().exec("DELETE FROM kv WHERE k = ?;", key);
      return true;
   }

   // ------------------------------------------------------------------ fin

   private void finish(Duel duel, Player winner, String reason) {
      if (duel.over) {
         return;
      }

      duel.over = true;
      this.duels.remove(duel.a.getUniqueId());
      this.duels.remove(duel.b.getUniqueId());
      if (winner == null) {
         if (duel.stake > 0) {
            this.plugin.getDataManager().creditPlayer(duel.a.getUniqueId(), duel.stake);
            this.plugin.getDataManager().creditPlayer(duel.b.getUniqueId(), duel.stake);
         }

         this.plugin.getBetting().cancelDuel(duel.id);
         ChatUtil.broadcast("&6⚔ Duel entre &e" + duel.a.getName() + " &6et &e" + duel.b.getName() + " : &7match nul (" + reason + "), mises remboursées.");
      } else {
         Player loser = duel.other(winner);
         if (duel.stake > 0) {
            this.plugin.getDataManager().creditPlayer(winner.getUniqueId(), duel.stake * 2);
         }

         this.plugin.getBetting().settleDuel(duel.id, winner.equals(duel.a) ? 0 : 1);
         ChatUtil.broadcast("&6⚔ &e" + winner.getName() + " &6remporte son duel d'honneur contre &e" + loser.getName() + (duel.stake > 0 ? " &7et empoche &6" + (long)(duel.stake * 2) + " pièces" : "") + " &7(" + reason + ") !");
         if (this.plugin.getAchievements().addStat(winner.getUniqueId(), fr.esicraft.life.AchievementManager.STAT_DUELS, 1L) >= 10L) {
            this.plugin.getAchievements().grant(winner.getUniqueId(), fr.esicraft.life.Achievement.DUEL_MASTER);
         }
      }

      for (Player p : List.of(duel.a, duel.b)) {
         if (p.isOnline() && !p.isDead()) {
            this.restore(p);
         }
      }
   }

   private void tick() {
      long now = System.currentTimeMillis();
      for (Duel d : new ArrayList<>(this.duels.values())) {
         if (!d.over && d.live && now - d.startedAt > this.plugin.getConfig().getLong("duels.max-seconds", 300L) * 1000L) {
            this.finish(d, null, "temps écoulé");
         }
      }

      this.challenges.values().removeIf(c -> c.expires < now);
   }

   public void shutdown() {
      for (Duel d : new ArrayList<>(this.duels.values())) {
         this.finish(d, null, "arrêt du serveur");
      }
   }

   public void clear() {
      this.challenges.clear();
   }

   // ------------------------------------------------------------------ événements

   /** Aucun coup mortel en duel : le coup fatal met fin au combat sans tuer personne. */
   @EventHandler(priority = EventPriority.HIGHEST)
   public void onDamage(EntityDamageEvent event) {
      if (!(event.getEntity() instanceof Player victim)) {
         return;
      }

      Duel duel = this.duels.get(victim.getUniqueId());
      if (duel == null || duel.over) {
         return;
      }

      boolean fromOpponent = false;
      if (event instanceof EntityDamageByEntityEvent byEntity) {
         Player attacker = fr.esicraft.combat.CombatManager.attackerOf(byEntity.getDamager());
         fromOpponent = attacker != null && duel.has(attacker) && !attacker.equals(victim);
         if (!fromOpponent) {
            event.setCancelled(true);
            return;
         }
      }

      if (!duel.live) {
         event.setCancelled(true);
         return;
      }

      if (victim.getHealth() - event.getFinalDamage() <= 0.0) {
         event.setCancelled(true);
         this.finish(duel, duel.other(victim), "coup décisif");
      }
   }

   @EventHandler(priority = EventPriority.HIGHEST)
   public void onDeath(PlayerDeathEvent event) {
      Player dead = event.getEntity();
      Duel duel = this.duels.get(dead.getUniqueId());
      if (duel == null) {
         return;
      }

      event.setKeepInventory(true);
      event.setKeepLevel(true);
      event.getDrops().clear();
      event.setDroppedExp(0);
      this.finish(duel, duel.other(dead), "chute");
   }

   @EventHandler
   public void onRespawn(PlayerRespawnEvent event) {
      Player p = event.getPlayer();
      List<Object[]> rows = this.plugin.getDataManager().getStorage().select("SELECT v FROM kv WHERE k = ?;", 1, "duel_snap:" + p.getUniqueId());
      if (!rows.isEmpty()) {
         try {
            Location origin = decodeLoc(String.valueOf(rows.get(0)[0]).split(";")[1]);
            if (origin != null) {
               event.setRespawnLocation(origin);
            }
         } catch (RuntimeException ignored) {
         }

         Bukkit.getScheduler().runTask(this.plugin, () -> this.restore(p));
      }
   }

   @EventHandler
   public void onQuit(PlayerQuitEvent event) {
      Player p = event.getPlayer();
      this.challenges.remove(p.getUniqueId());
      Duel duel = this.duels.get(p.getUniqueId());
      if (duel != null && !duel.over) {
         // Abandon : l'adversaire gagne ; l'inventaire du fuyard lui est rendu à sa prochaine connexion.
         this.finish(duel, duel.other(p), "abandon de " + p.getName());
      }
   }

   /** Après un crash ou un abandon, l'inventaire sauvegardé est rendu à la reconnexion. */
   @EventHandler
   public void onJoin(PlayerJoinEvent event) {
      Player p = event.getPlayer();
      if (!this.duels.containsKey(p.getUniqueId())) {
         Bukkit.getScheduler().runTaskLater(this.plugin, () -> {
            if (p.isOnline() && this.restore(p)) {
               ChatUtil.sendMessage(p, "&a⚔ Votre inventaire d'avant-duel vous a été rendu.");
            }
         }, 20L);
      }
   }

   @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
   public void onTeleport(PlayerTeleportEvent event) {
      if (!this.teleporting && this.isDueling(event.getPlayer())) {
         event.setCancelled(true);
      }
   }

   @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
   public void onCommand(PlayerCommandPreprocessEvent event) {
      if (!this.isDueling(event.getPlayer())) {
         return;
      }

      String msg = event.getMessage().toLowerCase(Locale.ROOT).trim();
      if (!msg.startsWith("/duel") && !event.getPlayer().isOp()) {
         event.setCancelled(true);
         ChatUtil.sendMessage(event.getPlayer(), "&c❌ Aucune commande pendant un duel.");
      }
   }

   public List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command command, @NotNull String alias, @NotNull String[] args) {
      List<String> out = new ArrayList<>();
      if (args.length == 1) {
         String prefix = args[0].toLowerCase(Locale.ROOT);
         for (String s : List.of("accepter", "refuser")) {
            if (s.startsWith(prefix)) {
               out.add(s);
            }
         }

         for (Player p : Bukkit.getOnlinePlayers()) {
            if (!p.equals(sender) && p.getName().toLowerCase(Locale.ROOT).startsWith(prefix)) {
               out.add(p.getName());
            }
         }
      }

      return out;
   }

   public Collection<Player> duelists() {
      List<Player> out = new ArrayList<>();
      for (Duel d : this.duels.values()) {
         out.add(d.a);
         out.add(d.b);
      }

      return out;
   }
}
