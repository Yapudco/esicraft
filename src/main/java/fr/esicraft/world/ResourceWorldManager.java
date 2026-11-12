package fr.esicraft.world;

import fr.esicraft.EsicraftPlugin;
import fr.esicraft.util.ChatUtil;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.DayOfWeek;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.TemporalAdjusters;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ThreadLocalRandom;
import java.util.stream.Stream;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.World;
import org.bukkit.WorldCreator;
import org.bukkit.block.Block;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityPortalEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerPortalEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.jetbrains.annotations.NotNull;

/**
 * Monde de ressources : un monde minier séparé, remis à neuf chaque semaine (jour et heure réglables). On y trouve du minerai frais et du PvP
 * hors de tout territoire (aucune revendication n'y est possible), ce qui préserve le monde principal des cratères de minage.
 * <p>
 * Avant chaque remise à zéro, les joueurs présents sont prévenus puis renvoyés là d'où ils venaient ; un joueur déconnecté dans l'ancien monde
 * est ramené au point d'apparition du monde principal à sa reconnexion.
 */
public class ResourceWorldManager implements CommandExecutor, TabCompleter, Listener {
   private static final String KV_RESET = "resworld:last_reset";
   private static final String KV_EPOCH = "resworld:epoch";
   private static final String KV_LEFT = "resworld:left:";
   private static final int[] WARN_MINUTES = {60, 30, 10, 5, 1};

   private final EsicraftPlugin plugin;
   private final Map<UUID, Location> returnPoints = new ConcurrentHashMap<>();
   private final Map<UUID, Long> lastTeleport = new ConcurrentHashMap<>();
   private final java.util.Set<UUID> searching = ConcurrentHashMap.newKeySet();
   private long lastReset;
   private long epoch;
   private int lastWarned = Integer.MAX_VALUE;
   private boolean resetting;

   public ResourceWorldManager(EsicraftPlugin plugin) {
      this.plugin = plugin;
   }

   // ------------------------------------------------------------------ réglages

   public boolean enabled() {
      return this.plugin.getConfig().getBoolean("resource-world.enabled", true);
   }

   public String worldName() {
      return this.plugin.getConfig().getString("resource-world.name", "esicraft_ressources");
   }

   public World world() {
      return Bukkit.getWorld(this.worldName());
   }

   public boolean isResourceWorld(World world) {
      return world != null && world.getName().equalsIgnoreCase(this.worldName());
   }

   public boolean isResourceWorld(String worldName) {
      return worldName != null && worldName.equalsIgnoreCase(this.worldName());
   }

   private int borderSize() {
      return Math.max(500, this.plugin.getConfig().getInt("resource-world.border-size", 6000));
   }

   private World mainWorld() {
      return Bukkit.getWorlds().get(0);
   }

   // ------------------------------------------------------------------ démarrage

   public void init() {
      this.lastReset = this.readLong(KV_RESET, 0L);
      this.epoch = this.readLong(KV_EPOCH, 0L);
      if (!this.enabled()) {
         return;
      }

      Bukkit.getScheduler().runTask(this.plugin, () -> {
         if (this.lastReset == 0L || System.currentTimeMillis() >= this.nextReset()) {
            // Jamais créé, ou la date de remise à zéro est passée pendant que le serveur était éteint.
            this.reset("première ouverture ou remise à zéro manquée");
         } else {
            this.load();
         }
      });
      Bukkit.getScheduler().runTaskTimer(this.plugin, this::tick, 20L * 60L, 20L * 60L);
   }

   private long readLong(String key, long def) {
      for (Object[] row : this.plugin.getDataManager().getStorage().select("SELECT v FROM kv WHERE k = ?;", 1, key)) {
         try {
            return Long.parseLong(String.valueOf(row[0]));
         } catch (NumberFormatException ignored) {
         }
      }

      return def;
   }

   private void writeKv(String key, String value) {
      this.plugin.getDataManager().getStorage().exec("INSERT INTO kv (k, v) VALUES (?, ?) ON CONFLICT(k) DO UPDATE SET v = excluded.v;", key, value);
   }

   private World load() {
      World w = this.world();
      if (w != null) {
         return w;
      }

      w = new WorldCreator(this.worldName()).environment(World.Environment.NORMAL).createWorld();
      if (w != null) {
         this.prepare(w);
      }

      return w;
   }

   private void prepare(World w) {
      w.getWorldBorder().setCenter(0.0, 0.0);
      w.getWorldBorder().setSize(this.borderSize());
   }

   // ------------------------------------------------------------------ calendrier

   /** Prochaine remise à zéro : le premier jour/heure configuré strictement après la dernière. */
   public long nextReset() {
      DayOfWeek day;
      try {
         day = DayOfWeek.valueOf(this.plugin.getConfig().getString("resource-world.reset-day", "MONDAY").toUpperCase(Locale.ROOT));
      } catch (IllegalArgumentException e) {
         day = DayOfWeek.MONDAY;
      }

      int hour = Math.max(0, Math.min(23, this.plugin.getConfig().getInt("resource-world.reset-hour", 5)));
      ZonedDateTime from = Instant.ofEpochMilli(Math.max(this.lastReset, 1L)).atZone(ZoneId.systemDefault());
      ZonedDateTime next = from.with(TemporalAdjusters.nextOrSame(day)).withHour(hour).withMinute(0).withSecond(0).withNano(0);
      if (!next.isAfter(from)) {
         next = next.plusWeeks(1);
      }

      return next.toInstant().toEpochMilli();
   }

   private void tick() {
      if (!this.enabled() || this.resetting) {
         return;
      }

      long left = this.nextReset() - System.currentTimeMillis();
      if (left <= 0L) {
         this.reset("remise à zéro hebdomadaire");
         return;
      }

      int minutes = (int)Math.ceil(left / 60000.0);
      for (int warn : WARN_MINUTES) {
         if (minutes <= warn && this.lastWarned > warn) {
            this.lastWarned = warn;
            World w = this.world();
            String msg = "&6⛏ Le monde de ressources sera remis à neuf dans &e" + warn + " minute" + (warn > 1 ? "s" : "") + "&6 : remontez avec &e/ressources quitter&6 !";
            if (warn <= 10) {
               ChatUtil.broadcast(msg);
            } else if (w != null) {
               for (Player p : w.getPlayers()) {
                  ChatUtil.sendMessage(p, msg);
               }
            }

            break;
         }
      }
   }

   // ------------------------------------------------------------------ remise à zéro

   public void reset(String reason) {
      if (this.resetting) {
         return;
      }

      this.resetting = true;
      World old = this.world();
      if (old == null) {
         this.regenerate(reason, 0);
         return;
      }

      for (Player p : old.getPlayers()) {
         ChatUtil.sendMessage(p, "&6⛏ Le monde de ressources se referme : retour à la surface !");
         this.sendBack(p);
      }

      // Les téléportations sont asynchrones : on laisse quelques secondes aux joueurs pour quitter le monde avant de le décharger.
      Bukkit.getScheduler().runTaskLater(this.plugin, () -> this.regenerate(reason, 0), 60L);
   }

   private void regenerate(String reason, int attempt) {
      World old = this.world();
      if (old != null) {
         for (Player p : old.getPlayers()) {
            p.teleport(this.mainWorld().getSpawnLocation());
         }

         if (!Bukkit.unloadWorld(old, false)) {
            if (attempt < 5) {
               Bukkit.getScheduler().runTaskLater(this.plugin, () -> this.regenerate(reason, attempt + 1), 100L);
            } else {
               this.plugin.getLogger().warning("Monde de ressources : déchargement impossible, nouvelle tentative à la prochaine vérification.");
               this.resetting = false;
            }

            return;
         }
      }

      File folder = new File(Bukkit.getWorldContainer(), this.worldName());
      Bukkit.getScheduler().runTaskAsynchronously(this.plugin, () -> {
         deleteRecursively(folder.toPath());
         Bukkit.getScheduler().runTask(this.plugin, () -> {
            World fresh = new WorldCreator(this.worldName()).environment(World.Environment.NORMAL).seed(ThreadLocalRandom.current().nextLong()).createWorld();
            if (fresh != null) {
               this.prepare(fresh);
            }

            this.lastReset = System.currentTimeMillis();
            this.epoch++;
            this.lastWarned = Integer.MAX_VALUE;
            this.writeKv(KV_RESET, String.valueOf(this.lastReset));
            this.writeKv(KV_EPOCH, String.valueOf(this.epoch));
            this.resetting = false;
            this.plugin.getLogger().info("Monde de ressources régénéré (" + reason + ").");
            ChatUtil.broadcast("&6&l⛏ Le monde de ressources a été remis à neuf ! &7Minerai frais et terres sauvages : &e/ressources&7. Prochaine remise à zéro : &e" + this.formatNext() + "&7.");
         });
      });
   }

   private static void deleteRecursively(Path root) {
      if (!Files.exists(root)) {
         return;
      }

      try (Stream<Path> walk = Files.walk(root)) {
         walk.sorted(Comparator.reverseOrder()).forEach(p -> {
            try {
               Files.deleteIfExists(p);
            } catch (IOException ignored) {
            }
         });
      } catch (IOException ignored) {
      }
   }

   public String formatNext() {
      return DateTimeFormatter.ofPattern("EEEE d MMMM 'à' HH'h'", Locale.FRENCH).format(Instant.ofEpochMilli(this.nextReset()).atZone(ZoneId.systemDefault()));
   }

   // ------------------------------------------------------------------ allers-retours

   private void sendBack(Player p) {
      Location back = this.returnPoints.remove(p.getUniqueId());
      if (back == null || back.getWorld() == null || this.isResourceWorld(back.getWorld())) {
         Location bed = p.getRespawnLocation();
         back = bed != null && !this.isResourceWorld(bed.getWorld()) ? bed : this.mainWorld().getSpawnLocation();
      }

      p.teleportAsync(back);
   }

   private void goDown(Player p) {
      if (!this.enabled() || this.resetting) {
         ChatUtil.sendMessage(p, "&c❌ Le monde de ressources est fermé pour le moment.");
         return;
      }

      if (this.isResourceWorld(p.getWorld())) {
         ChatUtil.sendMessage(p, "&7Vous êtes déjà dans le monde de ressources (&e/ressources quitter&7 pour remonter).");
         return;
      }

      if (this.plugin.getCombatManager().isInCombat(p) || this.plugin.getDuels().isDueling(p) || this.plugin.getRansom().isCaptive(p.getUniqueId())) {
         ChatUtil.sendMessage(p, "&c❌ Impossible de partir maintenant (combat, duel ou captivité).");
         return;
      }

      long cd = Math.max(0, this.plugin.getConfig().getInt("resource-world.teleport-cooldown-seconds", 300)) * 1000L;
      Long last = this.lastTeleport.get(p.getUniqueId());
      if (last != null && System.currentTimeMillis() - last < cd && !this.plugin.bypassCooldowns(p)) {
         ChatUtil.sendMessage(p, "&c❌ Patientez encore &e" + (cd - (System.currentTimeMillis() - last)) / 1000L + " s &cavant de redescendre.");
         return;
      }

      World w = this.load();
      if (w == null) {
         ChatUtil.sendMessage(p, "&c❌ Le monde de ressources n'a pas pu être chargé.");
         return;
      }

      if (!this.searching.add(p.getUniqueId())) {
         return;
      }

      ChatUtil.sendMessage(p, "&6⛏ Recherche d'un point d'arrivée sûr…");
      this.findSpot(p, w, 0);
   }

   private void findSpot(Player p, World w, int attempt) {
      if (attempt >= 12 || !p.isOnline()) {
         this.searching.remove(p.getUniqueId());
         if (p.isOnline()) {
            ChatUtil.sendMessage(p, "&c❌ Aucun point d'arrivée sûr trouvé, réessayez.");
         }

         return;
      }

      int half = this.borderSize() / 2 - 64;
      int x = ThreadLocalRandom.current().nextInt(-half, half);
      int z = ThreadLocalRandom.current().nextInt(-half, half);
      w.getChunkAtAsync(x >> 4, z >> 4).thenAccept(chunk -> {
         Block top = w.getHighestBlockAt(x, z);
         Material t = top.getType();
         if (top.isLiquid() || t == Material.MAGMA_BLOCK || t == Material.CACTUS || t == Material.POWDER_SNOW || !t.isSolid() && !t.name().endsWith("LEAVES")) {
            this.findSpot(p, w, attempt + 1);
            return;
         }

         this.searching.remove(p.getUniqueId());
         if (!this.isResourceWorld(p.getWorld())) {
            this.returnPoints.put(p.getUniqueId(), p.getLocation());
         }

         this.lastTeleport.put(p.getUniqueId(), System.currentTimeMillis());
         p.teleportAsync(top.getLocation().add(0.5, 1.0, 0.5)).thenAccept(ok -> {
            if (ok) {
               ChatUtil.sendMessage(p, "&6⛏ Bienvenue dans le monde de ressources ! &7Pas de territoires ici : le PvP est libre. Remise à zéro : &e" + this.formatNext() + "&7.");
               ChatUtil.playSound(p, Sound.ENTITY_ENDERMAN_TELEPORT, 1.0F, 0.8F);
               this.plugin.getTutorial().complete(p, fr.esicraft.life.TutorialManager.Step.RESOURCE_WORLD);
            }
         });
      });
   }

   private void goUp(Player p) {
      if (!this.isResourceWorld(p.getWorld())) {
         ChatUtil.sendMessage(p, "&7Vous n'êtes pas dans le monde de ressources.");
         return;
      }

      if (this.plugin.getCombatManager().isInCombat(p)) {
         ChatUtil.sendMessage(p, "&c❌ Pas de fuite en plein combat !");
         return;
      }

      ChatUtil.sendMessage(p, "&6⛏ Retour à la surface…");
      this.sendBack(p);
   }

   // ------------------------------------------------------------------ commande

   public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
      String sub = args.length == 0 ? "aller" : args[0].toLowerCase(Locale.ROOT);
      if (sub.equals("reset")) {
         if (!sender.hasPermission("esicraft.admin")) {
            ChatUtil.sendMessage(sender, "&c❌ Réservé aux administrateurs.");
         } else {
            ChatUtil.sendMessage(sender, "&6Remise à zéro du monde de ressources lancée.");
            this.reset("remise à zéro manuelle");
         }

         return true;
      }

      if (sub.equals("info")) {
         ChatUtil.sendMessage(sender, "&6⛏ Monde de ressources : minerai frais, PvP libre, aucune revendication. Bordure : &e" + this.borderSize() + " blocs&6. Prochaine remise à zéro : &e" + this.formatNext() + "&6.");
         return true;
      }

      if (!(sender instanceof Player p)) {
         ChatUtil.sendMessage(sender, "&cCommande réservée aux joueurs.");
         return true;
      }

      switch (sub) {
         case "quitter", "retour", "sortir", "leave" -> this.goUp(p);
         default -> this.goDown(p);
      }

      return true;
   }

   public List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command command, @NotNull String alias, @NotNull String[] args) {
      if (args.length != 1) {
         return List.of();
      }

      return sender.hasPermission("esicraft.admin") ? List.of("aller", "quitter", "info", "reset") : List.of("aller", "quitter", "info");
   }

   // ------------------------------------------------------------------ événements

   /** Pas de portails depuis le monde de ressources : il n'a ni Nether ni End à lui, et ne doit pas servir de raccourci. */
   @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
   public void onPortal(PlayerPortalEvent event) {
      if (this.isResourceWorld(event.getFrom().getWorld())) {
         event.setCancelled(true);
         ChatUtil.sendActionbar(event.getPlayer(), "&6⛏ Les portails ne fonctionnent pas dans le monde de ressources.");
      }
   }

   @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
   public void onEntityPortal(EntityPortalEvent event) {
      if (this.isResourceWorld(event.getFrom().getWorld())) {
         event.setCancelled(true);
      }
   }

   @EventHandler
   public void onQuit(PlayerQuitEvent event) {
      Player p = event.getPlayer();
      this.searching.remove(p.getUniqueId());
      if (this.isResourceWorld(p.getWorld())) {
         this.writeKv(KV_LEFT + p.getUniqueId(), String.valueOf(this.epoch));
      }
   }

   /** Un joueur parti dans un monde de ressources qui a été remis à zéro depuis revient au point d'apparition du monde principal. */
   @EventHandler
   public void onJoin(PlayerJoinEvent event) {
      Player p = event.getPlayer();
      String key = KV_LEFT + p.getUniqueId();
      long leftEpoch = this.readLong(key, -1L);
      if (leftEpoch < 0L) {
         return;
      }

      this.plugin.getDataManager().getStorage().exec("DELETE FROM kv WHERE k = ?;", key);
      if (leftEpoch != this.epoch) {
         Bukkit.getScheduler().runTask(this.plugin, () -> {
            Location bed = p.getRespawnLocation();
            p.teleportAsync(bed != null && !this.isResourceWorld(bed.getWorld()) ? bed : this.mainWorld().getSpawnLocation());
            ChatUtil.sendMessage(p, "&6⛏ Le monde de ressources a été remis à neuf pendant votre absence : vous voici de retour à la surface.");
         });
      }
   }
}
