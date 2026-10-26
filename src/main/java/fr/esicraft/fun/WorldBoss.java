package fr.esicraft.fun;

import fr.esicraft.EsicraftPlugin;
import fr.esicraft.guild.Guild;
import fr.esicraft.race.Race;
import fr.esicraft.util.ChatUtil;
import java.time.Duration;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;
import net.kyori.adventure.title.Title;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.World;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeInstance;
import org.bukkit.boss.BarColor;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Mob;
import org.bukkit.entity.Player;
import org.bukkit.entity.Projectile;
import org.bukkit.entity.Vex;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.util.Vector;

/**
 * Boss mondial : un boss différent chaque semaine, dont les PV et les dégâts s'adaptent au nombre de joueurs proches, annoncé par un compte à rebours
 * (titre, cor, direction et distance), à trois phases (gardes spectraux, onde de choc, enragement), avec des coups de zone télégraphiés par un cercle
 * de particules, un mode héroïque débloqué après une première victoire et un bonus pour la guilde du vainqueur gravé sur le site.
 */
public class WorldBoss implements Listener {
   public static final String BOSS_TAG = "esicraft_worldboss";
   public static final String GUARD_TAG = "esicraft_bossguard";
   private static final String WINS_KEY = "boss_wins";

   /** Les trois boss de la rotation hebdomadaire. */
   public enum Kind {
      ASH_LORD("&4&l☠ Seigneur des Cendres", "Seigneur des Cendres", EntityType.WITHER_SKELETON, Race.DWARF, Particle.FLAME, "Maître de la forge : les Mineurs (Nains) lui font +25 % de dégâts."),
      FOREST_GUARDIAN("&2&l🌲 Gardien de la Forêt", "Gardien de la Forêt", EntityType.RAVAGER, Race.RANGER, Particle.SPORE_BLOSSOM_AIR, "Bête sylvestre : les Rôdeurs lui font +25 % de dégâts."),
      ICE_QUEEN("&b&l❄ Reine des Glaces", "Reine des Glaces", EntityType.STRAY, Race.BLACKSMITH, Particle.SNOWFLAKE, "Souveraine du gel : les Forgerons lui font +25 % de dégâts.");

      final String display;
      final String plain;
      final EntityType type;
      final Race favored;
      final Particle ring;
      final String blurb;

      Kind(String display, String plain, EntityType type, Race favored, Particle ring, String blurb) {
         this.display = display;
         this.plain = plain;
         this.type = type;
         this.favored = favored;
         this.ring = ring;
         this.blurb = blurb;
      }

      public String plain() {
         return this.plain;
      }
   }

   private enum TelegraphType { SLAM, SHOCK }

   private static final class Telegraph {
      final Location center;
      final double radius;
      final TelegraphType type;
      int ticksLeft;

      Telegraph(Location center, double radius, TelegraphType type, int ticks) {
         this.center = center;
         this.radius = radius;
         this.type = type;
         this.ticksLeft = ticks;
      }
   }

   private final EsicraftPlugin plugin;
   private final EventManager events;
   private Kind kind;
   private boolean heroic;
   private Location spawnPoint;
   private long spawnAt;
   private boolean countdown;
   private LivingEntity boss;
   private int phase;
   private double baseAttack = 10.0;
   private int fightTicks;
   private long nextSlamAt;
   private long nextShockAt;
   private int lastReminder = -1;
   private final Map<UUID, Double> damage = new HashMap<>();
   private final List<Telegraph> telegraphs = new ArrayList<>();
   private final List<Entity> guards = new ArrayList<>();

   public WorldBoss(EsicraftPlugin plugin, EventManager events) {
      this.plugin = plugin;
      this.events = events;
   }

   public void init() {
      this.plugin.getDataManager().getStorage().exec("CREATE TABLE IF NOT EXISTS boss_wins (ts BIGINT, boss VARCHAR(40), heroic INT, guild_id VARCHAR(64), guild_name VARCHAR(64), top_player VARCHAR(40), fighters INT);");
      Bukkit.getPluginManager().registerEvents(this, this.plugin);
   }

   public boolean isBoss(Entity entity) {
      return entity != null && this.boss != null && entity.getUniqueId().equals(this.boss.getUniqueId());
   }

   public boolean isActiveFight() {
      return this.boss != null && this.boss.isValid();
   }

   // ------------------------------------------------------------------ configuration & état persistant

   private int cfgInt(String key, int def) {
      return this.plugin.getConfig().getInt("events.boss." + key, def);
   }

   private double cfgDouble(String key, double def) {
      return this.plugin.getConfig().getDouble("events.boss." + key, def);
   }

   private int wins() {
      for (Object[] row : this.plugin.getDataManager().getStorage().select("SELECT v FROM kv WHERE k = ?;", 1, WINS_KEY)) {
         try {
            return Integer.parseInt(String.valueOf(row[0]));
         } catch (NumberFormatException ignored) {
         }
      }

      return 0;
   }

   private void setWins(int value) {
      Runnable write = () -> this.plugin.getDataManager().getStorage().exec("INSERT INTO kv (k, v) VALUES (?, ?) ON CONFLICT(k) DO UPDATE SET v = excluded.v;", WINS_KEY, String.valueOf(value));
      if (this.plugin.isEnabled()) {
         Bukkit.getScheduler().runTaskAsynchronously(this.plugin, write);
      } else {
         write.run();
      }
   }

   /** Le boss de la semaine (rotation sur le numéro de semaine ISO). */
   private Kind weeklyKind() {
      int week = java.time.LocalDate.now().get(java.time.temporal.IsoFields.WEEK_OF_WEEK_BASED_YEAR);
      return Kind.values()[Math.floorMod(week, Kind.values().length)];
   }

   // ------------------------------------------------------------------ annonce & apparition

   private static String direction(Location from, Location to) {
      double dx = to.getX() - from.getX();
      double dz = to.getZ() - from.getZ();
      double bearing = Math.toDegrees(Math.atan2(dx, -dz));
      if (bearing < 0) {
         bearing += 360.0;
      }

      String[] names = {"Nord", "Nord-Est", "Est", "Sud-Est", "Sud", "Sud-Ouest", "Ouest", "Nord-Ouest"};
      return names[(int)Math.round(bearing / 45.0) % 8];
   }

   private void announce(boolean first) {
      int secondsLeft = (int)Math.max(0L, (this.spawnAt - System.currentTimeMillis()) / 1000L);
      for (Player p : Bukkit.getOnlinePlayers()) {
         String where;
         if (!p.getWorld().equals(this.spawnPoint.getWorld())) {
            where = "dans le monde « " + this.spawnPoint.getWorld().getName() + " »";
         } else {
            where = "à &e" + (int)p.getLocation().distance(this.spawnPoint) + " blocs &7vers le &e" + direction(p.getLocation(), this.spawnPoint);
         }

         if (first) {
            p.showTitle(Title.title(ChatUtil.component(this.kind.display + (this.heroic ? " &6★ HÉROÏQUE" : "")), ChatUtil.component("&7s'éveille dans &e" + secondsLeft + " s &7— " + where),
               Title.Times.times(Duration.ofMillis(400), Duration.ofSeconds(5), Duration.ofMillis(800))));
            ChatUtil.playSound(p, Sound.EVENT_RAID_HORN, 3.0F, 1.0F);
         }

         ChatUtil.sendMessage(p, "&c⚠ " + this.kind.display + (this.heroic ? " &6★ HÉROÏQUE" : "") + " &7s'éveille dans &e" + secondsLeft + " s &7— " + where + "&7.");
      }
   }

   /** Prépare l'apparition : choisit le lieu et le boss de la semaine, lance le compte à rebours. */
   public boolean start(List<Player> players, Location loc) {
      this.kind = this.weeklyKind();
      this.heroic = this.wins() > 0 && ThreadLocalRandom.current().nextDouble() < this.cfgDouble("heroic-chance", 0.5);
      this.spawnPoint = loc;
      int countdownSeconds = Math.max(5, this.cfgInt("countdown-seconds", 120));
      this.spawnAt = System.currentTimeMillis() + countdownSeconds * 1000L;
      this.countdown = true;
      this.lastReminder = -1;
      this.damage.clear();
      this.telegraphs.clear();
      this.phase = 0;
      this.events.setEndsAt(this.spawnAt + 15 * 60000L);
      this.events.openBar(this.kind.display + (this.heroic ? " &6★" : "") + " &c— éveil imminent", BarColor.PURPLE);
      this.announce(true);
      this.plugin.getChronicle().log(fr.esicraft.life.Chronicle.Kind.EVENT, this.kind.plain + (this.heroic ? " (héroïque)" : "") + " va s'éveiller dans " + countdownSeconds + " s.");
      this.events.runTicker(10, this::tick);
      return true;
   }

   private int nearbyPlayers(Location loc) {
      int n = 0;
      for (Player p : loc.getWorld().getPlayers()) {
         if (p.getGameMode() != org.bukkit.GameMode.SPECTATOR && p.getLocation().distanceSquared(loc) <= 150.0 * 150.0) {
            n++;
         }
      }

      return Math.max(1, n);
   }

   private void setAttr(LivingEntity entity, Attribute attribute, double value) {
      AttributeInstance instance = entity.getAttribute(attribute);
      if (instance != null) {
         instance.setBaseValue(value);
      }
   }

   private void spawnBoss() {
      this.countdown = false;
      Location loc = this.spawnPoint;
      World world = loc.getWorld();
      world.getChunkAt(loc);
      int n = this.nearbyPlayers(loc);
      double hp = Math.max(this.cfgDouble("min-hp", 400.0), Math.min(this.cfgDouble("max-hp", 1000.0), this.cfgDouble("hp-per-player", 300.0) * n));
      double attack = Math.max(8.0, Math.min(this.cfgDouble("max-attack", 22.0), 8.0 + 2.0 * n));
      if (this.heroic) {
         hp *= this.cfgDouble("heroic-hp-multiplier", 1.5);
         attack *= this.cfgDouble("heroic-damage-multiplier", 1.3);
      }

      hp = Math.min(1000.0, hp);
      this.baseAttack = attack;
      LivingEntity mob = (LivingEntity)world.spawnEntity(loc, this.kind.type);
      mob.customName(ChatUtil.component(this.kind.display + (this.heroic ? " &6★" : "")));
      mob.setCustomNameVisible(true);
      mob.setPersistent(true);
      mob.setRemoveWhenFarAway(false);
      mob.setGlowing(true);
      mob.addScoreboardTag(BOSS_TAG);
      mob.addPotionEffect(new PotionEffect(PotionEffectType.FIRE_RESISTANCE, PotionEffect.INFINITE_DURATION, 0, false, false));
      this.setAttr(mob, Attribute.MAX_HEALTH, hp);
      mob.setHealth(hp);
      this.setAttr(mob, Attribute.ATTACK_DAMAGE, attack);
      this.setAttr(mob, Attribute.MOVEMENT_SPEED, 0.30);
      this.setAttr(mob, Attribute.FOLLOW_RANGE, 48.0);
      this.setAttr(mob, Attribute.KNOCKBACK_RESISTANCE, 0.8);
      if (mob.getEquipment() != null && this.kind != Kind.FOREST_GUARDIAN) {
         mob.getEquipment().setItemInMainHand(new ItemStack(this.kind == Kind.ICE_QUEEN ? Material.BOW : Material.NETHERITE_SWORD));
         mob.getEquipment().setItemInMainHandDropChance(0.0F);
      }

      this.boss = mob;
      this.phase = 0;
      this.fightTicks = 0;
      this.nextSlamAt = System.currentTimeMillis() + 6000L;
      this.nextShockAt = Long.MAX_VALUE;
      this.events.setBar(this.kind.display + (this.heroic ? " &6★" : "") + " &c— " + (int)hp + " PV", BarColor.RED);
      ChatUtil.broadcast(this.kind.display + (this.heroic ? " &6★ HÉROÏQUE" : "") + " &7s'est éveillé ! (&c" + (int)hp + " PV&7, &e" + n + " joueur(s) à proximité&7) &8— " + this.kind.blurb + " &7Cagnotte : &6"
         + (int)this.pool() + " pièces &7pour ceux qui infligent au moins " + this.cfgInt("min-share-percent", 10) + " % des dégâts, 15 minutes pour le terrasser !");
      for (Player p : Bukkit.getOnlinePlayers()) {
         ChatUtil.playSound(p, Sound.ENTITY_WITHER_SPAWN, 0.6F, 1.2F);
      }
   }

   private double pool() {
      return Math.max(0.0, this.cfgDouble("coin-pool", 1000.0)) * (this.heroic ? this.cfgDouble("heroic-pool-multiplier", 1.5) : 1.0);
   }

   // ------------------------------------------------------------------ boucle de combat

   private void tick() {
      if (this.countdown) {
         long left = this.spawnAt - System.currentTimeMillis();
         if (left <= 0L) {
            this.spawnBoss();
            return;
         }

         this.events.setBarProgress(1.0 - (double)left / (Math.max(5, this.cfgInt("countdown-seconds", 120)) * 1000.0));
         this.events.setBarTitle(this.kind.display + (this.heroic ? " &6★" : "") + " &c— éveil dans &e" + (left / 1000L) + " s");
         if (left < 31000L && this.lastReminder != 0) {
            this.lastReminder = 0;
            this.announce(false);
         }

         return;
      }

      if (this.boss == null || !this.boss.isValid()) {
         return;
      }

      this.fightTicks += 10;
      double max = this.boss.getAttribute(Attribute.MAX_HEALTH).getValue();
      double ratio = Math.max(0.0, Math.min(1.0, this.boss.getHealth() / max));
      this.events.setBarProgress(ratio);
      int wanted = ratio <= 0.25 ? 3 : ratio <= 0.5 ? 2 : ratio <= 0.75 ? 1 : 0;
      while (this.phase < wanted) {
         this.enterPhase(++this.phase);
      }

      long now = System.currentTimeMillis();
      if (now >= this.nextSlamAt) {
         this.scheduleSlam();
         long base = this.phase >= 3 ? 4500L : this.phase == 2 ? 6000L : this.phase == 1 ? 7500L : 9000L;
         this.nextSlamAt = now + (long)(base * (this.heroic ? 0.8 : 1.0));
      }

      if (now >= this.nextShockAt) {
         this.telegraphs.add(new Telegraph(this.boss.getLocation(), 11.0, TelegraphType.SHOCK, 60));
         this.boss.getWorld().playSound(this.boss.getLocation(), Sound.ENTITY_WARDEN_SONIC_CHARGE, 2.0F, 1.0F);
         this.nextShockAt = now + (this.phase >= 3 ? 14000L : 22000L);
      }

      this.processTelegraphs();
   }

   private void enterPhase(int newPhase) {
      Location at = this.boss.getLocation();
      World world = at.getWorld();
      switch (newPhase) {
         case 1 -> {
            this.events.setBarColor(BarColor.YELLOW);
            ChatUtil.broadcast(this.kind.display + " &e« Mes gardes, à moi ! » &7— des gardes spectraux surgissent !");
            int count = Math.min(6, 2 + this.nearbyPlayers(at) / 2);
            for (int i = 0; i < count; i++) {
               Location s = at.clone().add(ThreadLocalRandom.current().nextDouble(-3, 3), 1.5, ThreadLocalRandom.current().nextDouble(-3, 3));
               Vex vex = (Vex)world.spawnEntity(s, EntityType.VEX);
               vex.customName(ChatUtil.component("&7Garde spectral"));
               vex.addScoreboardTag(GUARD_TAG);
               vex.setLifeTicks(20 * 90);
               this.setAttr(vex, Attribute.ATTACK_DAMAGE, Math.max(3.0, this.baseAttack * 0.4));
               this.guards.add(vex);
            }

            world.playSound(at, Sound.ENTITY_EVOKER_PREPARE_SUMMON, 3.0F, 0.8F);
         }
         case 2 -> {
            this.events.setBarColor(BarColor.PURPLE);
            ChatUtil.broadcast(this.kind.display + " &d« Tremblez ! » &7— une onde de choc se prépare, écartez-vous !");
            this.nextShockAt = System.currentTimeMillis() + 2000L;
         }
         default -> {
            this.events.setBarColor(BarColor.WHITE);
            ChatUtil.broadcast(this.kind.display + " &f&lest ENRAGÉ ! &7Plus rapide, plus violent, ses attaques s'enchaînent !");
            this.setAttr(this.boss, Attribute.MOVEMENT_SPEED, 0.40);
            this.setAttr(this.boss, Attribute.ATTACK_DAMAGE, this.baseAttack * 1.3);
            world.playSound(at, Sound.ENTITY_ENDER_DRAGON_GROWL, 3.0F, 0.7F);
         }
      }
   }

   private void scheduleSlam() {
      List<Player> near = new ArrayList<>();
      for (Player p : this.boss.getWorld().getPlayers()) {
         if (p.getGameMode() != org.bukkit.GameMode.SPECTATOR && p.getLocation().distanceSquared(this.boss.getLocation()) <= 28.0 * 28.0) {
            near.add(p);
         }
      }

      Location center = near.isEmpty() ? this.boss.getLocation() : near.get(ThreadLocalRandom.current().nextInt(near.size())).getLocation();
      this.telegraphs.add(new Telegraph(center.clone(), 4.0, TelegraphType.SLAM, 60));
      center.getWorld().playSound(center, Sound.ENTITY_BLAZE_SHOOT, 2.0F, 0.6F);
   }

   private void processTelegraphs() {
      Iterator<Telegraph> it = this.telegraphs.iterator();
      while (it.hasNext()) {
         Telegraph t = it.next();
         t.ticksLeft -= 10;
         World world = t.center.getWorld();
         int points = (int)Math.max(24, t.radius * 5);
         Particle ring = t.type == TelegraphType.SHOCK ? Particle.CRIT : this.kind.ring;
         for (int i = 0; i < points; i++) {
            double a = Math.PI * 2 * i / points;
            world.spawnParticle(ring, t.center.getX() + Math.cos(a) * t.radius, t.center.getY() + 0.15, t.center.getZ() + Math.sin(a) * t.radius, 1, 0, 0, 0, 0);
         }

         if (t.ticksLeft > 0) {
            continue;
         }

         it.remove();
         this.detonate(t);
      }
   }

   private void detonate(Telegraph t) {
      World world = t.center.getWorld();
      double dmg = Math.max(4.0, this.baseAttack * (t.type == TelegraphType.SHOCK ? 0.6 : 0.9));
      world.spawnParticle(t.type == TelegraphType.SHOCK ? Particle.EXPLOSION_EMITTER : Particle.EXPLOSION, t.center, t.type == TelegraphType.SHOCK ? 2 : 5, t.radius / 3, 0.3, t.radius / 3, 0.0);
      world.playSound(t.center, Sound.ENTITY_GENERIC_EXPLODE, 2.5F, 0.8F);
      for (Player p : world.getPlayers()) {
         Location pl = p.getLocation();
         double dx = pl.getX() - t.center.getX();
         double dz = pl.getZ() - t.center.getZ();
         if (p.getGameMode() == org.bukkit.GameMode.SPECTATOR || dx * dx + dz * dz > t.radius * t.radius || Math.abs(pl.getY() - t.center.getY()) > 4.0) {
            continue;
         }

         p.damage(dmg, this.boss);
         if (t.type == TelegraphType.SHOCK) {
            Vector away = new Vector(dx, 0, dz);
            away = away.lengthSquared() < 0.01 ? new Vector(0, 0, 1) : away.normalize();
            p.setVelocity(away.multiply(1.6).setY(0.7));
         } else {
            switch (this.kind) {
               case ASH_LORD -> p.setFireTicks(80);
               case FOREST_GUARDIAN -> p.addPotionEffect(new PotionEffect(PotionEffectType.POISON, 100, 1));
               default -> {
                  p.addPotionEffect(new PotionEffect(PotionEffectType.SLOWNESS, 100, 2));
                  p.setFreezeTicks(220);
               }
            }
         }
      }
   }

   // ------------------------------------------------------------------ dégâts, mort

   @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
   public void onBossHit(EntityDamageByEntityEvent event) {
      if (!this.isBoss(event.getEntity())) {
         return;
      }

      Entity damager = event.getDamager();
      if (damager instanceof Projectile projectile && projectile.getShooter() instanceof Entity shooter) {
         damager = shooter;
      }

      if (damager instanceof Player player) {
         var data = this.plugin.getDataManager().getPlayerData(player.getUniqueId());
         if (data != null && data.getRace() == this.kind.favored) {
            event.setDamage(event.getDamage() * (1.0 + this.cfgDouble("race-bonus", 0.25)));
         }
      }
   }

   @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
   public void onBossDamaged(EntityDamageByEntityEvent event) {
      if (!this.isBoss(event.getEntity())) {
         return;
      }

      Entity damager = event.getDamager();
      if (damager instanceof Projectile projectile && projectile.getShooter() instanceof Entity shooter) {
         damager = shooter;
      }

      if (damager instanceof Player player && event.getEntity() instanceof LivingEntity living) {
         // Seuls les PV réellement retirés comptent (pas l'excédent du coup fatal).
         this.damage.merge(player.getUniqueId(), Math.min(event.getFinalDamage(), Math.max(0.0, living.getHealth())), Double::sum);
      }
   }

   @EventHandler(priority = EventPriority.MONITOR)
   public void onDeath(EntityDeathEvent event) {
      if (event.getEntity().getScoreboardTags().contains(GUARD_TAG)) {
         event.getDrops().clear();
         event.setDroppedExp(0);
         return;
      }

      if (!this.isBoss(event.getEntity())) {
         return;
      }

      event.getDrops().clear();
      event.setDroppedExp(Math.max(0, this.cfgInt("xp", 500)));
      this.victory(event);
      Bukkit.getScheduler().runTask(this.plugin, this.events::stop);
   }

   private String nameOf(UUID id) {
      Player p = Bukkit.getPlayer(id);
      String n = p != null ? p.getName() : Bukkit.getOfflinePlayer(id).getName();
      return n != null ? n : "?";
   }

   private void victory(EntityDeathEvent event) {
      double total = this.damage.values().stream().mapToDouble(Double::doubleValue).sum();
      UUID top = this.damage.entrySet().stream().max(Map.Entry.comparingByValue()).map(Map.Entry::getKey).orElse(null);
      ChatUtil.broadcast(this.kind.display + (this.heroic ? " &6★" : "") + " &6&lest tombé ! &eHonneur aux combattants :");
      if (total > 0) {
         // Seuls ceux qui ont réellement participé (au moins X % des dégâts) touchent la cagnotte, avec un plafond par joueur.
         double minShare = Math.max(0.0, this.cfgInt("min-share-percent", 10) / 100.0);
         double cap = Math.max(0.0, this.cfgDouble("max-per-player", 400.0)) * (this.heroic ? 1.5 : 1.0);
         List<Map.Entry<UUID, Double>> sorted = new ArrayList<>();
         double qualified = 0.0;
         for (Map.Entry<UUID, Double> entry : this.damage.entrySet()) {
            if (entry.getValue() / total >= minShare) {
               sorted.add(entry);
               qualified += entry.getValue();
            }
         }

         sorted.sort(Map.Entry.<UUID, Double>comparingByValue().reversed());
         int shown = 0;
         for (Map.Entry<UUID, Double> entry : sorted) {
            double reward = Math.min(cap, Math.round(this.pool() * entry.getValue() / qualified));
            this.plugin.getDataManager().creditPlayer(entry.getKey(), reward);
            if (shown++ < 5) {
               ChatUtil.broadcast("  &f" + this.nameOf(entry.getKey()) + " &8» &c" + String.format("%.0f", entry.getValue()) + " dégâts &8(&6+" + String.format("%.0f", reward) + " pièces&8)");
            }
         }
      }

      if (top == null) {
         return;
      }

      Player topPlayer = Bukkit.getPlayer(top);
      String topName = this.nameOf(top);
      int diamonds = Math.max(0, this.cfgInt("top-diamonds", 3)) * (this.heroic ? 2 : 1);
      boolean star = ThreadLocalRandom.current().nextDouble() < (this.heroic ? this.cfgDouble("heroic-star-chance", 0.35) : this.cfgDouble("top-star-chance", 0.2));
      List<ItemStack> loot = new ArrayList<>();
      if (diamonds > 0) {
         loot.add(new ItemStack(Material.DIAMOND, diamonds));
      }

      if (star) {
         loot.add(new ItemStack(Material.NETHER_STAR, 1));
      }

      if (topPlayer != null) {
         for (ItemStack left : topPlayer.getInventory().addItem(loot.toArray(new ItemStack[0])).values()) {
            topPlayer.getWorld().dropItem(topPlayer.getLocation(), left, dropped -> dropped.setOwner(top));
         }
      } else {
         event.getDrops().addAll(loot);
      }

      ChatUtil.broadcast("&7Le plus grand pourfendeur, &e" + topName + "&7, empoche " + (diamonds > 0 ? "&b" + diamonds + " diamants" : "") + (star ? (diamonds > 0 ? " &7et une " : "une ") + "&fÉtoile du Nether" : "") + "&7 !");
      Guild guild = this.plugin.getGuildManager().getGuildByPlayer(top);
      String guildId = guild != null ? guild.getId() : "";
      String guildName = guild != null ? guild.getName() : "";
      if (guild != null) {
         double bonus = Math.max(0.0, this.cfgDouble("guild-bonus", 500.0)) * (this.heroic ? 1.5 : 1.0);
         if (bonus > 0.0) {
            guild.depositBank(bonus);
            this.plugin.getDataManager().saveGuild(guild);
            ChatUtil.broadcast("&6🏰 La guilde &e" + guild.getName() + " &6remporte &e" + (int)bonus + " pièces &6dans sa banque et son nom est gravé dans le Panthéon du site !");
         }
      }

      long ts = System.currentTimeMillis();
      int fighters = this.damage.size();
      int heroicFlag = this.heroic ? 1 : 0;
      String bossName = this.kind.plain;
      Bukkit.getScheduler().runTaskAsynchronously(this.plugin, () -> this.plugin.getDataManager().getStorage().exec(
         "INSERT INTO boss_wins (ts, boss, heroic, guild_id, guild_name, top_player, fighters) VALUES (?, ?, ?, ?, ?, ?, ?);", ts, bossName, heroicFlag, guildId, guildName, topName, fighters));
      this.setWins(this.wins() + 1);
      this.plugin.getSeasonManager().addHistory(fr.esicraft.life.Chronicle.Kind.EVENT, "☠ " + this.kind.plain + (this.heroic ? " (héroïque)" : "") + " a été terrassé (meilleur combattant : " + topName + (guild != null ? ", guilde " + guild.getName() : "") + ").", null, null);
      this.plugin.getAchievements().grant(top, fr.esicraft.life.Achievement.SLAYER);
      if (this.heroic) {
         this.plugin.getAchievements().grant(top, fr.esicraft.life.Achievement.HEROIC_SLAYER);
      }
   }

   // ------------------------------------------------------------------ fin de combat

   /** Le temps imparti est écoulé. */
   public void onTimeout() {
      if (this.countdown) {
         ChatUtil.broadcast("&7☠ Le boss ne s'est jamais éveillé…");
      } else if (this.kind != null) {
         ChatUtil.broadcast("&7☠ " + this.kind.plain + " s'est évanoui… personne ne l'a terrassé.");
      }
   }

   /** Retire le boss, ses gardes et les cercles en cours. */
   public void cleanup() {
      if (this.boss != null && this.boss.isValid()) {
         this.boss.remove();
      }

      for (Entity g : this.guards) {
         if (g.isValid()) {
            g.remove();
         }
      }

      this.guards.clear();
      this.telegraphs.clear();
      this.damage.clear();
      this.boss = null;
      this.countdown = false;
   }

   public void clearOrphans(Entity entity) {
      if ((entity.getScoreboardTags().contains(BOSS_TAG) || entity.getScoreboardTags().contains(GUARD_TAG)) && entity != this.boss && !this.guards.contains(entity)) {
         entity.remove();
      }
   }
}
