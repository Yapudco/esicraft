package fr.esicraft.life;

import fr.esicraft.EsicraftPlugin;
import fr.esicraft.util.ChatUtil;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ThreadLocalRandom;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.World;
import org.bukkit.block.data.Ageable;
import org.bukkit.block.data.BlockData;
import org.bukkit.entity.Item;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockDropItemEvent;
import org.bukkit.event.block.BlockGrowEvent;
import org.bukkit.event.player.PlayerExpChangeEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.inventory.ItemStack;

/**
 * Saisons naturelles : printemps, été, automne, hiver, chacune durant {@code life.nature.season-days} jours (14 par défaut).
 * Le cycle est calculé à partir d'une date de départ conservée en base : un redémarrage ne le décale pas.
 */
public class NatureSeasonManager implements Listener {
   private static final Set<Material> CROPS = EnumSet.of(Material.WHEAT, Material.CARROTS, Material.POTATOES, Material.BEETROOTS, Material.MELON_STEM,
      Material.PUMPKIN_STEM, Material.SWEET_BERRY_BUSH, Material.COCOA, Material.TORCHFLOWER_CROP, Material.PITCHER_CROP);

   private final EsicraftPlugin plugin;
   private long startMs;
   private long seasonMs;
   private boolean enabled;
   private NatureSeason announced;

   public NatureSeasonManager(EsicraftPlugin plugin) {
      this.plugin = plugin;
   }

   public void init() {
      this.enabled = this.plugin.getConfig().getBoolean("life.nature.enabled", true);
      this.seasonMs = Math.max(1L, this.plugin.getConfig().getLong("life.nature.season-days", 14L)) * 86400000L;
      var db = this.plugin.getDataManager().getStorage();
      List<Object[]> rows = db.select("SELECT v FROM kv WHERE k = 'nature_start';", 1);
      long start = 0L;
      if (!rows.isEmpty()) {
         try {
            start = Long.parseLong((String)rows.get(0)[0]);
         } catch (NumberFormatException ignored) {
         }
      }

      if (start <= 0L) {
         start = System.currentTimeMillis();
         db.exec("INSERT INTO kv (k, v) VALUES ('nature_start', ?) ON CONFLICT(k) DO UPDATE SET v = excluded.v;", String.valueOf(start));
      }

      this.startMs = start;
      this.announced = this.current();
      Bukkit.getScheduler().runTaskTimer(this.plugin, this::tick, 6000L, 6000L);
   }

   public boolean isEnabled() {
      return this.enabled;
   }

   public NatureSeason current() {
      long elapsed = Math.max(0L, System.currentTimeMillis() - this.startMs);
      return NatureSeason.values()[(int)((elapsed / this.seasonMs) % 4L)];
   }

   public NatureSeason next() {
      return NatureSeason.values()[(this.current().ordinal() + 1) % 4];
   }

   /** Temps restant avant la saison suivante (ms). */
   public long remainingMs() {
      long elapsed = Math.max(0L, System.currentTimeMillis() - this.startMs);
      return this.seasonMs - elapsed % this.seasonMs;
   }

   public long remainingDays() {
      return (long)Math.ceil(this.remainingMs() / 86400000.0);
   }

   /** Multiplicateur des primes de monstres dû à la saison. */
   public double bountyMultiplier() {
      return this.enabled && this.current() == NatureSeason.WINTER ? 1.25 : 1.0;
   }

   private void tick() {
      if (!this.enabled) {
         return;
      }

      NatureSeason now = this.current();
      if (now != this.announced) {
         this.announced = now;
         ChatUtil.broadcast(now.getColor() + "&l" + now.getIcon() + " " + now.getLabel().toUpperCase(java.util.Locale.ROOT) + " &7— la saison change !");
         for (String effect : now.getEffects()) {
            ChatUtil.broadcast("&7• " + effect);
         }

         for (Player p : Bukkit.getOnlinePlayers()) {
            ChatUtil.playSound(p, Sound.BLOCK_AMETHYST_BLOCK_CHIME, 1.0F, 1.0F);
            this.markSeen(p);
         }

         this.plugin.getChronicle().log(Chronicle.Kind.NATURE, now.display() + " commence.");
      }
   }

   private void markSeen(Player p) {
      long mask = this.plugin.getAchievements().orStat(p.getUniqueId(), AchievementManager.STAT_SEASONS, 1L << this.current().ordinal());
      if (mask == 15L) {
         this.plugin.getAchievements().grant(p.getUniqueId(), Achievement.FOUR_SEASONS);
      }
   }

   @EventHandler
   public void onJoin(PlayerJoinEvent event) {
      if (this.enabled) {
         this.markSeen(event.getPlayer());
      }
   }

   private static boolean overworld(World world) {
      return world != null && world.getEnvironment() == World.Environment.NORMAL;
   }

   @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
   public void onGrow(BlockGrowEvent event) {
      if (!this.enabled || !CROPS.contains(event.getBlock().getType()) || !overworld(event.getBlock().getWorld())) {
         return;
      }

      NatureSeason season = this.current();
      if (season == NatureSeason.WINTER && ThreadLocalRandom.current().nextBoolean()) {
         event.setCancelled(true);
      } else if (season == NatureSeason.SPRING && ThreadLocalRandom.current().nextBoolean()) {
         BlockData data = event.getNewState().getBlockData();
         if (data instanceof Ageable ageable && ageable.getAge() < ageable.getMaximumAge()) {
            ageable.setAge(ageable.getAge() + 1);
            event.getNewState().setBlockData(ageable);
         }
      }
   }

   @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
   public void onHarvest(BlockDropItemEvent event) {
      if (!this.enabled || this.current() != NatureSeason.AUTUMN || !overworld(event.getBlock().getWorld())) {
         return;
      }

      BlockData data = event.getBlockState().getBlockData();
      if (!CROPS.contains(event.getBlockState().getType()) || !(data instanceof Ageable ageable) || ageable.getAge() < ageable.getMaximumAge()) {
         return;
      }

      if (!event.getItems().isEmpty() && ThreadLocalRandom.current().nextBoolean()) {
         Item first = event.getItems().get(0);
         ItemStack bonus = first.getItemStack().clone();
         bonus.setAmount(1);
         event.getBlock().getWorld().dropItemNaturally(event.getBlock().getLocation().add(0.5, 0.5, 0.5), bonus);
      }
   }

   @EventHandler(priority = EventPriority.HIGH)
   public void onExp(PlayerExpChangeEvent event) {
      if (this.enabled && this.current() == NatureSeason.SUMMER && event.getAmount() > 0) {
         double extra = event.getAmount() * 0.2;
         int add = (int)extra + (ThreadLocalRandom.current().nextDouble() < extra - Math.floor(extra) ? 1 : 0);
         event.setAmount(event.getAmount() + add);
      }
   }
}
