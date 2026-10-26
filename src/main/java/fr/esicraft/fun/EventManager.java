package fr.esicraft.fun;

import fr.esicraft.EsicraftPlugin;
import fr.esicraft.util.ChatUtil;
import fr.esicraft.util.ChunkKey;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;
import org.bukkit.Bukkit;
import org.bukkit.HeightMap;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.World;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeInstance;
import org.bukkit.block.Block;
import org.bukkit.boss.BarColor;
import org.bukkit.boss.BarStyle;
import org.bukkit.boss.BossBar;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Monster;
import org.bukkit.entity.Player;
import org.bukkit.entity.Projectile;
import org.bukkit.entity.WanderingTrader;
import org.bukkit.entity.WitherSkeleton;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.CreatureSpawnEvent;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.world.EntitiesLoadEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.MerchantRecipe;
import org.bukkit.scheduler.BukkitTask;

/**
 * Événements serveur aléatoires : Lune de Sang, Pluie de Météorites, Marchand du Marché Noir et Boss Mondial.
 * Un seul événement est actif à la fois ; un nouvel événement se déclenche tous les 1 à 2 jours (configurable, échéance conservée après un redémarrage).
 */
public class EventManager implements Listener {
   public enum Type {
      BLOOD_MOON("🌑 Lune de Sang"),
      METEOR_SHOWER("☄ Pluie de Météorites"),
      BLACK_MARKET("🧙 Marché Noir"),
      WORLD_BOSS("☠ Boss Mondial");

      private final String label;

      Type(String label) {
         this.label = label;
      }

      public String getLabel() {
         return this.label;
      }
   }

   public static final String TRADER_TAG = "esicraft_blackmarket";
   private final EsicraftPlugin plugin;
   private Type active;
   private long endsAt;
   private long nextEventAt;
   private BossBar bar;
   private BukkitTask ticker;
   private BukkitTask scheduler;
   private final WorldBoss worldBoss;
   private Entity trader;
   /** Joueurs qui ont déjà fait leur unique échange avec le marchand de l'événement en cours. */
   private final java.util.Set<UUID> tradedPlayers = java.util.concurrent.ConcurrentHashMap.newKeySet();
   private boolean cloning;

   public EventManager(EsicraftPlugin plugin) {
      this.plugin = plugin;
      this.worldBoss = new WorldBoss(plugin, this);
   }

   public WorldBoss getWorldBoss() {
      return this.worldBoss;
   }

   private static final String NEXT_EVENT_KEY = "event_next_at";

   public void start() {
      this.worldBoss.init();
      // L'échéance est conservée en base : redémarrer le serveur ne remet pas le compteur à zéro (ni ne déclenche un événement de plus).
      long saved = 0L;
      for (Object[] row : this.plugin.getDataManager().getStorage().select("SELECT v FROM kv WHERE k = ?;", 1, NEXT_EVENT_KEY)) {
         try {
            saved = Long.parseLong((String)row[0]);
         } catch (NumberFormatException ignored) {
         }
      }

      long now = System.currentTimeMillis();
      int maxMinutes = this.plugin.getConfig().getInt("events.max-interval-minutes", 2880);
      if (saved > now && saved - now <= maxMinutes * 60000L) {
         this.nextEventAt = saved;
      } else {
         this.scheduleNext(1440, 2880);
      }

      // Vérification chaque minute : lance un événement aléatoire quand l'échéance est atteinte.
      this.scheduler = Bukkit.getScheduler().runTaskTimer(this.plugin, () -> {
         if (this.active == null && this.plugin.getConfig().getBoolean("events.enabled", true) && System.currentTimeMillis() >= this.nextEventAt
            && !Bukkit.getOnlinePlayers().isEmpty()) {
            Type[] types = Type.values();
            this.launch(types[ThreadLocalRandom.current().nextInt(types.length)]);
         }
      }, 1200L, 1200L);
   }

   private void scheduleNext(int minMinutes, int maxMinutes) {
      int min = this.plugin.getConfig().getInt("events.min-interval-minutes", minMinutes);
      int max = Math.max(min, this.plugin.getConfig().getInt("events.max-interval-minutes", maxMinutes));
      this.nextEventAt = System.currentTimeMillis() + (long)ThreadLocalRandom.current().nextInt(min, max + 1) * 60000L;
      long at = this.nextEventAt;
      if (this.plugin.isEnabled()) {
         Bukkit.getScheduler().runTaskAsynchronously(this.plugin, () -> this.plugin.getDataManager().getStorage().exec(
            "INSERT INTO kv (k, v) VALUES (?, ?) ON CONFLICT(k) DO UPDATE SET v = excluded.v;", NEXT_EVENT_KEY, String.valueOf(at)));
      }
   }

   public Type getActive() {
      return this.active;
   }

   public double getBountyMultiplier() {
      double mult = this.active == Type.BLOOD_MOON ? 2.0 : 1.0;
      return this.plugin.getCourt().is(fr.esicraft.game.CourtManager.Rule.BOUNTY_BOOM) ? mult * 1.25 : mult;
   }

   public long getRemainingSeconds() {
      return this.active == null ? 0L : Math.max(0L, (this.endsAt - System.currentTimeMillis()) / 1000L);
   }

   // ------------------------------------------------------------------ cycle de vie

   public boolean launch(Type type) {
      if (this.active != null) {
         return false;
      }

      List<Player> players = new ArrayList<>(Bukkit.getOnlinePlayers());
      if (players.isEmpty()) {
         return false;
      }

      World overworld = this.overworld();
      if (overworld == null) {
         return false;
      }

      boolean started = switch (type) {
         case BLOOD_MOON -> this.startBloodMoon(overworld);
         case METEOR_SHOWER -> this.startMeteors();
         case BLACK_MARKET -> this.startBlackMarket(players);
         case WORLD_BOSS -> this.startBoss(players);
      };
      if (!started) {
         return false;
      }

      this.active = type;
      this.plugin.getSeasonManager().addHistory(type.getLabel() + " s'est déclenché sur le serveur.");
      for (Player p : players) {
         this.showBar(p);
         ChatUtil.playSound(p, Sound.ENTITY_WITHER_SPAWN, 0.6F, 1.4F);
      }

      return true;
   }

   public void stop() {
      if (this.active == null) {
         return;
      }

      if (this.ticker != null) {
         this.ticker.cancel();
         this.ticker = null;
      }

      this.worldBoss.cleanup();
      if (this.trader != null && this.trader.isValid()) {
         this.trader.remove();
      }

      this.trader = null;
      this.tradedPlayers.clear();
      if (this.bar != null) {
         this.bar.removeAll();
         this.bar = null;
      }

      this.active = null;
      this.scheduleNext(1440, 2880);
   }

   /** À l'arrêt du plugin : nettoie boss, marchand et barre. */
   public void shutdown() {
      this.stop();
      if (this.scheduler != null) {
         this.scheduler.cancel();
      }
   }

   private World overworld() {
      for (World w : Bukkit.getWorlds()) {
         if (w.getEnvironment() == World.Environment.NORMAL) {
            return w;
         }
      }

      return null;
   }

   private void showBar(Player p) {
      if (this.bar != null) {
         this.bar.addPlayer(p);
      }
   }

   void setEndsAt(long ms) {
      this.endsAt = ms;
   }

   void openBar(String title, BarColor color) {
      this.createBar(title, color);
   }

   void setBar(String title, BarColor color) {
      if (this.bar != null) {
         this.bar.setTitle(ChatUtil.color(title));
         this.bar.setColor(color);
      }
   }

   void setBarTitle(String title) {
      if (this.bar != null) {
         this.bar.setTitle(ChatUtil.color(title));
      }
   }

   void setBarColor(BarColor color) {
      if (this.bar != null) {
         this.bar.setColor(color);
      }
   }

   void setBarProgress(double progress) {
      if (this.bar != null) {
         this.bar.setProgress(Math.max(0.0, Math.min(1.0, progress)));
      }
   }

   private void createBar(String title, BarColor color) {
      this.bar = Bukkit.createBossBar(ChatUtil.color(title), color, BarStyle.SEGMENTED_10);
      this.bar.setProgress(1.0);
   }

   void runTicker(int periodTicks, Runnable perTick) {
      this.ticker = Bukkit.getScheduler().runTaskTimer(this.plugin, () -> {
         if (this.active == null) {
            return;
         }

         if (System.currentTimeMillis() >= this.endsAt) {
            this.onTimeout();
            return;
         }

         perTick.run();
      }, periodTicks, periodTicks);
   }

   private void onTimeout() {
      Type ended = this.active;
      switch (ended) {
         case BLOOD_MOON -> ChatUtil.broadcast("&7🌑 La Lune de Sang s'efface… l'aube revient.");
         case METEOR_SHOWER -> ChatUtil.broadcast("&7☄ La pluie de météorites est terminée. Fouillez les cratères restants !");
         case BLACK_MARKET -> ChatUtil.broadcast("&7🧙 Le Marchand du Marché Noir a plié bagage et disparu dans la nuit.");
         case WORLD_BOSS -> this.worldBoss.onTimeout();
      }

      this.stop();
   }

   // ------------------------------------------------------------------ outils de placement

   private boolean inClaim(Location loc) {
      return loc.getWorld() != null
         && this.plugin.getGuildManager().getGuildByChunk(new ChunkKey(loc.getWorld().getName(), loc.getBlockX() >> 4, loc.getBlockZ() >> 4)) != null;
   }

   /** Cherche un point en surface, hors territoire de guilde, à 30-90 blocs d'un joueur (chunk déjà chargé). */
   Location findSurfaceNear(Player around, int minDist, int maxDist) {
      World world = around.getWorld();
      if (world.getEnvironment() != World.Environment.NORMAL) {
         return null;
      }

      for (int attempt = 0; attempt < 12; attempt++) {
         double angle = ThreadLocalRandom.current().nextDouble(Math.PI * 2);
         double dist = ThreadLocalRandom.current().nextDouble(minDist, maxDist);
         int x = (int)(around.getLocation().getX() + Math.cos(angle) * dist);
         int z = (int)(around.getLocation().getZ() + Math.sin(angle) * dist);
         if (!world.isChunkLoaded(x >> 4, z >> 4)) {
            continue;
         }

         Block ground = world.getHighestBlockAt(x, z, HeightMap.MOTION_BLOCKING_NO_LEAVES);
         if (!ground.getType().isSolid() || ground.isLiquid()) {
            continue;
         }

         Location loc = ground.getLocation().add(0.5, 1.0, 0.5);
         if (!this.inClaim(loc)) {
            return loc;
         }
      }

      return null;
   }

   private static String rough(Location loc) {
      return "X≈" + (Math.round(loc.getBlockX() / 20.0) * 20) + " Z≈" + (Math.round(loc.getBlockZ() / 20.0) * 20);
   }

   // ------------------------------------------------------------------ Lune de Sang

   private boolean startBloodMoon(World world) {
      this.endsAt = System.currentTimeMillis() + 10 * 60000L;
      this.createBar("&4&l🌑 LUNE DE SANG &c— Primes x2, monstres en nombre !", BarColor.RED);
      if (world.getTime() < 13000L || world.getTime() > 23000L) {
         world.setTime(13500L);
      }

      ChatUtil.broadcast("&4&l🌑 LA LUNE DE SANG SE LÈVE ! &cPendant 10 minutes, les monstres sont plus nombreux, mais toutes les &6primes de chasse sont doublées&c !");
      this.plugin.getChronicle().log(fr.esicraft.life.Chronicle.Kind.EVENT, "La Lune de Sang se lève : primes de chasse doublées pendant 10 minutes.");
      this.runTicker(100, () -> this.bar.setProgress(Math.max(0.0, Math.min(1.0, (double)(this.endsAt - System.currentTimeMillis()) / 600000.0))));
      return true;
   }

   @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
   public void onNaturalSpawn(CreatureSpawnEvent event) {
      if (this.active != Type.BLOOD_MOON || this.cloning || event.getSpawnReason() != CreatureSpawnEvent.SpawnReason.NATURAL) {
         return;
      }

      if (event.getEntity() instanceof Monster && ThreadLocalRandom.current().nextDouble() < this.plugin.getConfig().getDouble("events.blood-moon-extra-spawn-chance", 0.25)) {
         this.cloning = true;
         try {
            Location loc = event.getLocation();
            loc.getWorld().spawnEntity(loc, event.getEntityType(), CreatureSpawnEvent.SpawnReason.CUSTOM);
         } finally {
            this.cloning = false;
         }
      }
   }

   // ------------------------------------------------------------------ Pluie de Météorites

   private boolean startMeteors() {
      this.endsAt = System.currentTimeMillis() + 3 * 60000L;
      this.createBar("&6&l☄ PLUIE DE MÉTÉORITES &e— Cherchez les cratères !", BarColor.YELLOW);
      ChatUtil.broadcast("&6&l☄ UNE PLUIE DE MÉTÉORITES S'ABAT SUR LE MONDE ! &eDes blocs de minerai rares tombent près des joueurs, hors des territoires de guilde. Soyez les premiers !");
      this.plugin.getChronicle().log(fr.esicraft.life.Chronicle.Kind.EVENT, "Une pluie de météorites s'abat sur le monde.");
      this.runTicker(300, () -> {
         this.bar.setProgress(Math.max(0.0, Math.min(1.0, (double)(this.endsAt - System.currentTimeMillis()) / 180000.0)));
         List<Player> players = new ArrayList<>(Bukkit.getOnlinePlayers());
         if (!players.isEmpty()) {
            this.dropMeteor(players.get(ThreadLocalRandom.current().nextInt(players.size())));
         }
      });
      return true;
   }

   private Material meteorOre() {
      double r = ThreadLocalRandom.current().nextDouble();
      if (r < 0.03) {
         return Material.ANCIENT_DEBRIS;
      } else if (r < 0.15) {
         return Material.DIAMOND_ORE;
      } else if (r < 0.40) {
         return Material.GOLD_ORE;
      } else if (r < 0.70) {
         return Material.IRON_ORE;
      } else {
         return Material.COPPER_ORE;
      }
   }

   private void dropMeteor(Player near) {
      Location impact = this.findSurfaceNear(near, 30, 80);
      if (impact == null) {
         return;
      }

      World world = impact.getWorld();
      int cluster = 3 + ThreadLocalRandom.current().nextInt(3);
      for (int i = 0; i < cluster; i++) {
         int dx = i == 0 ? 0 : ThreadLocalRandom.current().nextInt(-1, 2);
         int dz = i == 0 ? 0 : ThreadLocalRandom.current().nextInt(-1, 2);
         Block ground = world.getHighestBlockAt(impact.getBlockX() + dx, impact.getBlockZ() + dz, HeightMap.MOTION_BLOCKING_NO_LEAVES);
         if (ground.getType().isSolid() && !ground.isLiquid() && !this.inClaim(ground.getLocation())) {
            // Posé sans BlockPlaceEvent : le bloc n'est pas « posé par un joueur », les bonus de minage s'appliquent.
            ground.setType(this.meteorOre(), false);
         }
      }

      world.spawnParticle(Particle.EXPLOSION, impact, 3, 1.0, 0.5, 1.0, 0.0);
      world.spawnParticle(Particle.FLAME, impact, 60, 1.5, 0.5, 1.5, 0.05);
      world.playSound(impact, Sound.ENTITY_GENERIC_EXPLODE, 3.0F, 0.6F);
      ChatUtil.broadcast("&6☄ Une météorite s'est écrasée près de &e" + rough(impact) + " &6!");
   }

   // ------------------------------------------------------------------ Marché Noir

   private boolean startBlackMarket(List<Player> players) {
      Location loc = null;
      for (int i = 0; i < 6 && loc == null; i++) {
         loc = this.findSurfaceNear(players.get(ThreadLocalRandom.current().nextInt(players.size())), 40, 100);
      }

      if (loc == null) {
         return false;
      }

      WanderingTrader npc = (WanderingTrader)loc.getWorld().spawnEntity(loc, EntityType.WANDERING_TRADER);
      npc.customName(ChatUtil.component("&5&l🧙 Marchand du Marché Noir"));
      npc.setCustomNameVisible(true);
      npc.setInvulnerable(true);
      npc.setPersistent(true);
      npc.setRemoveWhenFarAway(false);
      npc.setGlowing(true);
      npc.addScoreboardTag(TRADER_TAG);
      this.tradedPlayers.clear();
      npc.setRecipes(this.blackMarketRecipes());
      this.trader = npc;
      this.endsAt = System.currentTimeMillis() + 5 * 60000L;
      this.createBar("&5&l🧙 MARCHÉ NOIR &d— Un marchand rôde près de " + rough(loc), BarColor.PURPLE);
      ChatUtil.broadcast("&5&l🧙 UN MARCHAND DU MARCHÉ NOIR EST APPARU près de &d" + rough(loc) + " &5! &7Il échange des objets rares contre des &bdiamants&7 — pendant 5 minutes seulement.");
      this.plugin.getChronicle().log(fr.esicraft.life.Chronicle.Kind.EVENT, "Le Marchand du Marché Noir est apparu pour 5 minutes.");
      this.runTicker(100, () -> this.bar.setProgress(Math.max(0.0, Math.min(1.0, (double)(this.endsAt - System.currentTimeMillis()) / 300000.0))));
      return true;
   }

   /**
    * Offres du Marché noir : très chères (de 32 à 128 diamants) et <b>verrouillées après un seul achat</b> (un exemplaire de chaque offre,
    * pour tout le serveur). Seules {@code events.black-market.offers} offres tirées au hasard sont proposées à chaque apparition.
    */
   private List<MerchantRecipe> blackMarketRecipes() {
      List<MerchantRecipe> all = new ArrayList<>();
      all.add(this.recipe(new ItemStack(Material.ENCHANTED_GOLDEN_APPLE, 1), 96, 0, null));
      all.add(this.recipe(new ItemStack(Material.TOTEM_OF_UNDYING, 1), 48, 0, null));
      all.add(this.recipe(new ItemStack(Material.SHULKER_SHELL, 2), 32, 0, null));
      all.add(this.recipe(new ItemStack(Material.MUSIC_DISC_PIGSTEP, 1), 64, 0, null));
      all.add(this.recipe(new ItemStack(Material.GOAT_HORN, 1), 40, 0, null));
      all.add(this.recipe(new ItemStack(Material.ELYTRA, 1), 64, 2, Material.NETHERITE_INGOT));
      java.util.Collections.shuffle(all);
      int offers = Math.max(1, Math.min(all.size(), this.plugin.getConfig().getInt("events.black-market.offers", 3)));
      return new ArrayList<>(all.subList(0, offers));
   }

   private MerchantRecipe recipe(ItemStack result, int diamonds, int extraAmount, Material extra) {
      double mult = Math.max(0.1, this.plugin.getConfig().getDouble("events.black-market.price-multiplier", 1.0));
      int total = Math.max(1, (int)Math.round(diamonds * mult));
      MerchantRecipe r = new MerchantRecipe(result, 1);
      // Deux emplacements d'ingrédients de 64 au plus : le prix en diamants peut les occuper tous les deux.
      r.addIngredient(new ItemStack(Material.DIAMOND, Math.min(64, total)));
      if (total > 64) {
         r.addIngredient(new ItemStack(Material.DIAMOND, Math.min(64, total - 64)));
      } else if (extra != null && extraAmount > 0) {
         r.addIngredient(new ItemStack(extra, extraAmount));
      }

      r.setExperienceReward(false);
      r.setIgnoreDiscounts(true);
      return r;
   }

   /** Un seul échange par joueur et par apparition du marchand (en plus du verrouillage de chaque offre après un achat). */
   @EventHandler(ignoreCancelled = true)
   public void onBlackMarketTrade(io.papermc.paper.event.player.PlayerTradeEvent event) {
      if (!(event.getVillager() instanceof WanderingTrader npc) || !npc.getScoreboardTags().contains(TRADER_TAG)) {
         return;
      }

      if (!this.tradedPlayers.add(event.getPlayer().getUniqueId())) {
         event.setCancelled(true);
         ChatUtil.sendMessage(event.getPlayer(), "&5🧙 &7« Un seul achat par client, voyageur. Laissez-en aux autres. »");
         return;
      }

      ChatUtil.sendMessage(event.getPlayer(), "&5🧙 &7« Marché conclu. Cette offre est désormais épuisée, et vous n'achèterez plus rien ici. »");
   }

   /** Supprime les marchands et boss orphelins après un redémarrage (les événements ne survivent pas à l'arrêt). */
   @EventHandler
   public void onEntitiesLoad(EntitiesLoadEvent event) {
      for (Entity entity : event.getEntities()) {
         if (entity.getScoreboardTags().contains(TRADER_TAG) && entity != this.trader) {
            entity.remove();
         }

         this.worldBoss.clearOrphans(entity);
      }
   }

   // ------------------------------------------------------------------ Boss Mondial

   private boolean startBoss(List<Player> players) {
      Location loc = null;
      for (int i = 0; i < 6 && loc == null; i++) {
         loc = this.findSurfaceNear(players.get(ThreadLocalRandom.current().nextInt(players.size())), 40, 90);
      }

      return loc != null && this.worldBoss.start(players, loc);
   }

   @EventHandler
   public void onJoin(PlayerJoinEvent event) {
      this.showBar(event.getPlayer());
   }

   public List<String> describe() {
      List<String> lines = new ArrayList<>();
      if (this.active == null) {
         long mins = Math.max(0L, (this.nextEventAt - System.currentTimeMillis()) / 60000L);
         lines.add("Aucun événement en cours. Prochain tirage dans ~" + mins + " min.");
      } else {
         lines.add("Événement actif : " + this.active.getLabel() + " (" + this.getRemainingSeconds() + " s restantes)");
      }

      return lines;
   }
}
