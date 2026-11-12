package fr.esicraft.game;

import fr.esicraft.EsicraftPlugin;
import fr.esicraft.guild.Guild;
import fr.esicraft.util.ChatUtil;
import fr.esicraft.util.ChunkKey;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ThreadLocalRandom;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.entity.WanderingTrader;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.world.EntitiesLoadEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.MerchantRecipe;
import org.bukkit.inventory.meta.EnchantmentStorageMeta;
import org.jetbrains.annotations.NotNull;

/**
 * Marchand itinérant de guilde : chaque jour, une guilde tirée au sort (Nexus posé, quelques territoires) reçoit un marchand rare sur ses terres.
 * Tout le monde peut venir le voir : cela fait passer des étrangers sur le territoire de la guilde hôte (diplomatie, embuscades, sauf-conduits).
 * La guilde hôte touche une petite commission à chaque achat d'un visiteur étranger ; ses propres membres achètent sans commission.
 */
public class GuildMerchantManager implements CommandExecutor, TabCompleter, Listener {
   public static final String TAG = "esicraft_guildmerchant";
   private static final String KV_STATE = "gmerchant:state";
   private static final String KV_LAST_DAY = "gmerchant:day";
   private static final String KV_LAST_GUILD = "gmerchant:last_guild";

   private final EsicraftPlugin plugin;
   private final Map<UUID, Integer> tradesToday = new ConcurrentHashMap<>();
   private WanderingTrader trader;
   private String hostGuildId;
   private Location spot;
   private long endsAt;
   private String lastDay = "";
   private String lastGuild = "";

   public GuildMerchantManager(EsicraftPlugin plugin) {
      this.plugin = plugin;
   }

   /** Entité marchande « spéciale » (marché noir ou marchand de guilde) : ouverte à tous, sans condition d'Âge. */
   public static boolean isSpecialTrader(Object entity) {
      return entity instanceof Entity e && (e.getScoreboardTags().contains(TAG) || e.getScoreboardTags().contains(fr.esicraft.fun.EventManager.TRADER_TAG));
   }

   private boolean enabled() {
      return this.plugin.getConfig().getBoolean("guild-merchant.enabled", true);
   }

   public void init() {
      this.lastDay = this.kv(KV_LAST_DAY, "");
      this.lastGuild = this.kv(KV_LAST_GUILD, "");
      String state = this.kv(KV_STATE, "");
      String[] p = state.split("\\|");
      if (p.length == 6) {
         try {
            long ends = Long.parseLong(p[5]);
            World w = Bukkit.getWorld(p[1]);
            if (ends > System.currentTimeMillis() && w != null && this.plugin.getGuildManager().getGuildById(p[0]) != null) {
               this.hostGuildId = p[0];
               this.spot = new Location(w, Double.parseDouble(p[2]), Double.parseDouble(p[3]), Double.parseDouble(p[4]));
               this.endsAt = ends;
               Bukkit.getScheduler().runTaskLater(this.plugin, this::spawnEntity, 40L);
            }
         } catch (NumberFormatException ignored) {
         }
      }

      Bukkit.getScheduler().runTaskTimer(this.plugin, this::tick, 20L * 30L, 20L * 60L);
   }

   public void clear() {
      this.despawn();
      this.hostGuildId = null;
      this.spot = null;
      this.tradesToday.clear();
   }

   public void shutdown() {
      if (this.trader != null && this.trader.isValid()) {
         this.trader.remove();
      }
   }

   private String kv(String key, String def) {
      for (Object[] row : this.plugin.getDataManager().getStorage().select("SELECT v FROM kv WHERE k = ?;", 1, key)) {
         return String.valueOf(row[0]);
      }

      return def;
   }

   private void setKv(String key, String value) {
      this.plugin.getDataManager().getStorage().exec("INSERT INTO kv (k, v) VALUES (?, ?) ON CONFLICT(k) DO UPDATE SET v = excluded.v;", key, value);
   }

   // ------------------------------------------------------------------ cycle quotidien

   private void tick() {
      if (this.hostGuildId != null && System.currentTimeMillis() >= this.endsAt) {
         this.leave("&7🐫 Le marchand itinérant a plié bagage. Il reviendra demain chez une autre guilde.");
      }

      if (this.hostGuildId != null) {
         Guild host = this.plugin.getGuildManager().getGuildById(this.hostGuildId);
         if (host == null || this.spot == null || this.plugin.getGuildManager().getGuildByChunk(ChunkKey.from(this.spot.getChunk())) != host) {
            this.leave("&7🐫 Le marchand itinérant a fui : les terres qui l'accueillaient ont changé de mains.");
         } else if (this.trader == null || !this.trader.isValid()) {
            if (this.spot.isChunkLoaded()) {
               this.spawnEntity();
            }
         }
      }

      if (!this.enabled()) {
         return;
      }

      String today = LocalDate.now().toString();
      int hour = Math.max(0, Math.min(23, this.plugin.getConfig().getInt("guild-merchant.hour", 14)));
      if (!today.equals(this.lastDay) && LocalTime.now().getHour() >= hour && this.hostGuildId == null) {
         this.lastDay = today;
         this.setKv(KV_LAST_DAY, today);
         this.tradesToday.clear();
         this.arrive(null);
      }
   }

   /** Fait venir le marchand chez une guilde (tirée au sort si {@code forced} est nul). Renvoie la guilde choisie. */
   public Guild arrive(Guild forced) {
      Guild host = forced != null ? forced : this.draw();
      if (host == null) {
         return null;
      }

      Location loc = this.findSpot(host);
      if (loc == null) {
         return null;
      }

      this.despawn();
      this.hostGuildId = host.getId();
      this.spot = loc;
      this.endsAt = System.currentTimeMillis() + Math.max(1, this.plugin.getConfig().getInt("guild-merchant.duration-hours", 6)) * 3600000L;
      this.lastGuild = host.getId();
      this.setKv(KV_LAST_GUILD, host.getId());
      this.saveState();
      this.spawnEntity();
      ChatUtil.broadcast("&6&l🐫 Un marchand itinérant s'est installé chez &e" + host.getName() + " &6&l! &7Il vend des objets rares jusqu'à &e"
         + java.time.format.DateTimeFormatter.ofPattern("HH'h'mm").format(java.time.Instant.ofEpochMilli(this.endsAt).atZone(java.time.ZoneId.systemDefault()))
         + "&7, près de &e" + loc.getBlockX() + ", " + loc.getBlockZ() + "&7 (&e/marchand&7). Allez le voir… poliment.");
      ChatUtil.broadcastGuild(host, "&6🐫 Le marchand itinérant s'installe sur vos terres : vous touchez une commission sur chaque achat d'un visiteur étranger. Accueillez-les bien !");
      this.plugin.getChronicle().log(fr.esicraft.life.Chronicle.Kind.EVENT, "Un marchand itinérant s'est installé chez " + host.getName() + ".");
      return host;
   }

   /** Tirage : guildes établies avec un Nexus, au moins quelques territoires et un membre en ligne ; jamais deux jours de suite la même. */
   private Guild draw() {
      int minClaims = Math.max(1, this.plugin.getConfig().getInt("guild-merchant.min-claims", 4));
      List<Guild> pool = new ArrayList<>();
      for (Guild g : this.plugin.getGuildManager().getAllGuilds()) {
         if (g.hasNexus() && g.getClaimedChunks().size() >= minClaims && !g.getId().equals(this.lastGuild) && this.hasOnlineMember(g)) {
            pool.add(g);
         }
      }

      if (pool.isEmpty()) {
         for (Guild g : this.plugin.getGuildManager().getAllGuilds()) {
            if (g.hasNexus() && g.getClaimedChunks().size() >= minClaims) {
               pool.add(g);
            }
         }
      }

      return pool.isEmpty() ? null : pool.get(ThreadLocalRandom.current().nextInt(pool.size()));
   }

   private boolean hasOnlineMember(Guild g) {
      for (UUID id : g.getMembers().keySet()) {
         if (Bukkit.getPlayer(id) != null) {
            return true;
         }
      }

      return false;
   }

   /** Un emplacement à quelques blocs du Nexus, dans un chunk de la guilde, au sol. */
   private Location findSpot(Guild host) {
      if (host.getNexus() == null || host.getNexus().getLocation() == null || host.getNexus().getLocation().getWorld() == null) {
         return null;
      }

      Location base = host.getNexus().getLocation();
      World w = base.getWorld();
      int[][] offsets = {{4, 4}, {-4, 4}, {4, -4}, {-4, -4}, {6, 0}, {0, 6}, {-6, 0}, {0, -6}, {2, 2}};
      for (int[] o : offsets) {
         int x = base.getBlockX() + o[0];
         int z = base.getBlockZ() + o[1];
         if (this.plugin.getGuildManager().getGuildByChunk(new ChunkKey(w.getName(), x >> 4, z >> 4)) != host) {
            continue;
         }

         Block ground = w.getHighestBlockAt(x, z);
         if (!ground.isLiquid() && ground.getType().isSolid()) {
            return ground.getLocation().add(0.5, 1.0, 0.5);
         }
      }

      return base.clone().add(0.5, 1.0, 2.5);
   }

   private void saveState() {
      if (this.hostGuildId == null || this.spot == null) {
         this.setKv(KV_STATE, "");
         return;
      }

      this.setKv(KV_STATE, this.hostGuildId + "|" + this.spot.getWorld().getName() + "|" + this.spot.getX() + "|" + this.spot.getY() + "|" + this.spot.getZ() + "|" + this.endsAt);
   }

   private void spawnEntity() {
      if (this.spot == null || this.spot.getWorld() == null) {
         return;
      }

      if (this.trader != null && this.trader.isValid()) {
         return;
      }

      Guild host = this.plugin.getGuildManager().getGuildById(this.hostGuildId);
      WanderingTrader npc = (WanderingTrader)this.spot.getWorld().spawnEntity(this.spot, EntityType.WANDERING_TRADER);
      npc.customName(ChatUtil.component("&6&l🐫 Marchand itinérant" + (host != null ? " &7(chez " + host.getName() + ")" : "")));
      npc.setCustomNameVisible(true);
      npc.setInvulnerable(true);
      npc.setPersistent(true);
      npc.setRemoveWhenFarAway(false);
      npc.setAI(false);
      npc.setSilent(false);
      npc.setDespawnDelay(0);
      npc.addScoreboardTag(TAG);
      npc.setRecipes(this.offers());
      this.trader = npc;
   }

   private void despawn() {
      if (this.trader != null && this.trader.isValid()) {
         this.trader.remove();
      }

      this.trader = null;
   }

   private void leave(String message) {
      this.despawn();
      this.hostGuildId = null;
      this.spot = null;
      this.saveState();
      ChatUtil.broadcast(message);
   }

   // ------------------------------------------------------------------ offres

   private List<MerchantRecipe> offers() {
      List<MerchantRecipe> all = new ArrayList<>();
      all.add(this.recipe(this.book(Enchantment.MENDING, 1), 24, Material.EMERALD, 8));
      all.add(this.recipe(this.book(Enchantment.UNBREAKING, 3), 16, Material.EMERALD, 0));
      all.add(this.recipe(this.book(Enchantment.EFFICIENCY, 4), 20, Material.EMERALD, 0));
      all.add(this.recipe(this.book(Enchantment.FORTUNE, 3), 16, Material.DIAMOND, 0));
      all.add(this.recipe(new ItemStack(Material.TRIDENT), 24, Material.DIAMOND, 0));
      all.add(this.recipe(new ItemStack(Material.HEART_OF_THE_SEA), 32, Material.EMERALD, 0));
      all.add(this.recipe(new ItemStack(Material.NETHERITE_UPGRADE_SMITHING_TEMPLATE), 16, Material.DIAMOND, 0));
      all.add(this.recipe(new ItemStack(Material.SNIFFER_EGG), 32, Material.EMERALD, 0));
      all.add(this.recipe(new ItemStack(Material.SADDLE), 12, Material.EMERALD, 0));
      all.add(this.recipe(new ItemStack(Material.NAME_TAG, 2), 10, Material.EMERALD, 0));
      all.add(this.recipe(new ItemStack(Material.EXPERIENCE_BOTTLE, 32), 24, Material.EMERALD, 0));
      all.add(this.recipe(new ItemStack(Material.ANCIENT_DEBRIS, 2), 24, Material.DIAMOND, 0));
      all.add(this.recipe(new ItemStack(Material.SHULKER_SHELL, 2), 40, Material.EMERALD, 0));
      all.add(this.recipe(new ItemStack(Material.GOLDEN_APPLE, 4), 20, Material.EMERALD, 0));
      Collections.shuffle(all);
      int count = Math.max(1, Math.min(all.size(), this.plugin.getConfig().getInt("guild-merchant.offers", 5)));
      return new ArrayList<>(all.subList(0, count));
   }

   private ItemStack book(Enchantment ench, int level) {
      ItemStack book = new ItemStack(Material.ENCHANTED_BOOK);
      EnchantmentStorageMeta meta = (EnchantmentStorageMeta)book.getItemMeta();
      meta.addStoredEnchant(ench, level, true);
      book.setItemMeta(meta);
      return book;
   }

   private MerchantRecipe recipe(ItemStack result, int price, Material currency, int extraEmeralds) {
      int uses = Math.max(1, this.plugin.getConfig().getInt("guild-merchant.stock-per-offer", 3));
      MerchantRecipe r = new MerchantRecipe(result, uses);
      r.addIngredient(new ItemStack(currency, Math.min(64, price)));
      if (extraEmeralds > 0) {
         r.addIngredient(new ItemStack(currency == Material.EMERALD ? Material.DIAMOND : Material.EMERALD, extraEmeralds));
      }

      r.setExperienceReward(false);
      r.setIgnoreDiscounts(true);
      return r;
   }

   @EventHandler(ignoreCancelled = true)
   public void onTrade(io.papermc.paper.event.player.PlayerTradeEvent event) {
      if (!(event.getVillager() instanceof WanderingTrader npc) || !npc.getScoreboardTags().contains(TAG)) {
         return;
      }

      Player buyer = event.getPlayer();
      int max = Math.max(1, this.plugin.getConfig().getInt("guild-merchant.trades-per-player", 2));
      int done = this.tradesToday.getOrDefault(buyer.getUniqueId(), 0);
      if (done >= max) {
         event.setCancelled(true);
         ChatUtil.sendMessage(buyer, "&6🐫 &7« Vous m'avez déjà assez acheté aujourd'hui, l'ami. Laissez-en aux autres. »");
         return;
      }

      this.tradesToday.put(buyer.getUniqueId(), done + 1);
      Guild host = this.plugin.getGuildManager().getGuildById(this.hostGuildId);
      if (host == null) {
         return;
      }

      if (!host.isMember(buyer.getUniqueId())) {
         double commission = Math.max(0.0, this.plugin.getConfig().getDouble("guild-merchant.host-commission", 75.0));
         if (commission > 0.0) {
            host.depositBank(commission);
            this.plugin.getDataManager().saveGuild(host);
         }

         ChatUtil.broadcastGuild(host, "&6🐫 &e" + buyer.getName() + " &7est venu acheter au marchand sur vos terres" + (commission > 0.0 ? " &8(&6+" + (long)commission + " pièces &7en banque&8)" : "") + "&7.");
      }

      ChatUtil.playSound(buyer, Sound.ENTITY_WANDERING_TRADER_YES, 1.0F, 1.0F);
   }

   /** Nettoie un marchand orphelin (redémarrage, double apparition). */
   @EventHandler
   public void onEntitiesLoad(EntitiesLoadEvent event) {
      for (Entity entity : event.getEntities()) {
         if (entity.getScoreboardTags().contains(TAG) && entity != this.trader) {
            if (this.trader == null && this.hostGuildId != null && this.spot != null && entity instanceof WanderingTrader wt && entity.getWorld().equals(this.spot.getWorld())
               && entity.getLocation().distanceSquared(this.spot) < 9.0) {
               this.trader = wt;
            } else {
               entity.remove();
            }
         }
      }
   }

   // ------------------------------------------------------------------ commande

   public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
      if (args.length >= 1 && sender.hasPermission("esicraft.admin") && (args[0].equalsIgnoreCase("forcer") || args[0].equalsIgnoreCase("force"))) {
         Guild g = args.length >= 2 ? this.plugin.getGuildManager().getGuildByName(args[1]) : null;
         if (args.length >= 2 && g == null) {
            ChatUtil.sendMessage(sender, "&cGuilde introuvable.");
            return true;
         }

         this.tradesToday.clear();
         Guild host = this.arrive(g);
         ChatUtil.sendMessage(sender, host != null ? "&aMarchand envoyé chez " + host.getName() + "." : "&cAucune guilde éligible (Nexus et territoires requis).");
         return true;
      }

      if (args.length >= 1 && sender.hasPermission("esicraft.admin") && args[0].equalsIgnoreCase("partir")) {
         if (this.hostGuildId != null) {
            this.leave("&7🐫 Le marchand itinérant est reparti plus tôt que prévu.");
         }

         return true;
      }

      Guild host = this.hostGuildId != null ? this.plugin.getGuildManager().getGuildById(this.hostGuildId) : null;
      if (host == null || this.spot == null) {
         int hour = this.plugin.getConfig().getInt("guild-merchant.hour", 14);
         ChatUtil.sendMessage(sender, "&6🐫 Aucun marchand itinérant en ce moment. &7Il s'installe chaque jour vers &e" + hour + "h &7chez une guilde tirée au sort (Nexus et au moins "
            + this.plugin.getConfig().getInt("guild-merchant.min-claims", 4) + " territoires).");
         return true;
      }

      long min = Math.max(0L, (this.endsAt - System.currentTimeMillis()) / 60000L);
      ChatUtil.sendMessage(sender, "&6🐫 Le marchand itinérant est chez &e" + host.getName() + " &6en &e" + this.spot.getBlockX() + ", " + this.spot.getBlockY() + ", " + this.spot.getBlockZ()
         + " &7(" + this.spot.getWorld().getName() + ")&6. Il repart dans &e" + min / 60L + " h " + min % 60L + " min&6. &7Achats limités à "
         + this.plugin.getConfig().getInt("guild-merchant.trades-per-player", 2) + " par joueur et par jour.");
      return true;
   }

   public List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command command, @NotNull String alias, @NotNull String[] args) {
      if (!sender.hasPermission("esicraft.admin")) {
         return List.of();
      }

      if (args.length == 1) {
         return List.of("forcer", "partir");
      }

      if (args.length == 2 && args[0].toLowerCase(Locale.ROOT).startsWith("forc")) {
         List<String> names = new ArrayList<>();
         for (Guild g : this.plugin.getGuildManager().getAllGuilds()) {
            names.add(g.getName());
         }

         return names;
      }

      return List.of();
   }
}
