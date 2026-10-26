package fr.esicraft.nexus;

import fr.esicraft.EsicraftPlugin;
import fr.esicraft.gui.holder.BaseGUIHolder;
import fr.esicraft.guild.Guild;
import fr.esicraft.guild.GuildRole;
import fr.esicraft.guild.GuildSpecialization;
import fr.esicraft.life.Achievement;
import fr.esicraft.util.ChatUtil;
import fr.esicraft.util.ChunkKey;
import fr.esicraft.util.ItemBuilder;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.EnumSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ThreadLocalRandom;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.World;
import org.bukkit.entity.Bee;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

/**
 * Arbre technologique du Nexus : le Nexus gagne de l'expérience à chaque horde repoussée ou ressource sacrifiée ({@code /nexus sacrifice}).
 * Chaque défense se débloque (définitivement) en sacrifiant des objets, en atteignant le niveau requis, en ayant débloqué sa défense parente
 * et, pour les défenses « vivantes », en ayant ramené puis tué le mob correspondant dans le chunk du Nexus.
 */
public class NexusTech implements Listener {
   /** Expérience cumulée nécessaire pour chaque niveau du Nexus (niveau 1 à 6). */
   private static final long[] LEVEL_XP = {0L, 300L, 900L, 2000L, 4500L, 9000L};

   public record Node(NexusTrapType type, int level, Map<Material, Integer> cost, EntityType kill, NexusTrapType parent, int slot) {
   }

   private static final Map<Material, Integer> XP_VALUES = new EnumMap<>(Material.class);

   static {
      Object[][] values = {
         {Material.COAL, 1}, {Material.COPPER_INGOT, 1}, {Material.STRING, 1}, {Material.BONE, 1}, {Material.REDSTONE, 1}, {Material.FLINT, 1}, {Material.LAPIS_LAZULI, 2}, {Material.GUNPOWDER, 2},
         {Material.QUARTZ, 2}, {Material.IRON_INGOT, 3}, {Material.AMETHYST_SHARD, 3}, {Material.PRISMARINE_SHARD, 4}, {Material.GOLD_INGOT, 6}, {Material.OBSIDIAN, 10}, {Material.EMERALD, 15},
         {Material.ENDER_PEARL, 15}, {Material.BLAZE_ROD, 20}, {Material.DIAMOND, 25}, {Material.GHAST_TEAR, 60}, {Material.SHULKER_SHELL, 80}, {Material.ANCIENT_DEBRIS, 100},
         {Material.ECHO_SHARD, 120}, {Material.NETHERITE_INGOT, 200}, {Material.NETHER_STAR, 500},
         {Material.IRON_BLOCK, 27}, {Material.GOLD_BLOCK, 54}, {Material.DIAMOND_BLOCK, 225}, {Material.EMERALD_BLOCK, 135}, {Material.COAL_BLOCK, 9}, {Material.NETHERITE_BLOCK, 1800}};
      for (Object[] v : values) {
         XP_VALUES.put((Material)v[0], (Integer)v[1]);
      }
   }

   private static final Map<NexusTrapType, Node> NODES = new LinkedHashMap<>();

   private static void node(NexusTrapType type, int level, EntityType kill, NexusTrapType parent, int slot, Object... cost) {
      Map<Material, Integer> items = new EnumMap<>(Material.class);
      for (int i = 0; i < cost.length; i += 2) {
         items.put((Material)cost[i], (Integer)cost[i + 1]);
      }

      NODES.put(type, new Node(type, level, items, kill, parent, slot));
   }

   static {
      node(NexusTrapType.INFERNAL_FLAMES, 1, null, null, 1, Material.COAL, 64, Material.FLINT, 16);
      node(NexusTrapType.SLOWDOWN_WEB, 1, null, null, 4, Material.STRING, 128, Material.SLIME_BALL, 16);
      node(NexusTrapType.LIGHTNING_STRIKE, 2, null, NexusTrapType.INFERNAL_FLAMES, 10, Material.COPPER_INGOT, 96, Material.REDSTONE, 64);
      node(NexusTrapType.GUARDIAN_GOLEM, 2, EntityType.IRON_GOLEM, null, 13, Material.IRON_BLOCK, 8, Material.REDSTONE, 32);
      node(NexusTrapType.POISON_MIST, 2, null, NexusTrapType.SLOWDOWN_WEB, 16, Material.SPIDER_EYE, 48, Material.SLIME_BALL, 16);
      node(NexusTrapType.BLAZE_SHOT, 3, EntityType.BLAZE, NexusTrapType.INFERNAL_FLAMES, 19, Material.BLAZE_ROD, 24, Material.GOLD_INGOT, 32);
      node(NexusTrapType.GUARDIAN_SHOT, 3, EntityType.GUARDIAN, NexusTrapType.LIGHTNING_STRIKE, 22, Material.PRISMARINE_SHARD, 64, Material.PRISMARINE_CRYSTALS, 32);
      node(NexusTrapType.BEE_SWARM, 3, EntityType.BEE, NexusTrapType.POISON_MIST, 25, Material.HONEYCOMB, 32, Material.HONEY_BOTTLE, 8);
      node(NexusTrapType.EVOKER_SHOT, 4, EntityType.EVOKER, NexusTrapType.GUARDIAN_GOLEM, 28, Material.EMERALD, 64, Material.TOTEM_OF_UNDYING, 1);
      node(NexusTrapType.SHULKER_SHOT, 4, EntityType.SHULKER, NexusTrapType.GUARDIAN_SHOT, 31, Material.SHULKER_SHELL, 8, Material.ENDER_PEARL, 16);
      node(NexusTrapType.GHAST_SHOT, 5, EntityType.GHAST, NexusTrapType.BLAZE_SHOT, 37, Material.GHAST_TEAR, 8, Material.GUNPOWDER, 128);
      node(NexusTrapType.WARDEN_SHOT, 5, EntityType.WARDEN, NexusTrapType.EVOKER_SHOT, 40, Material.ECHO_SHARD, 8, Material.SCULK_CATALYST, 4);
      node(NexusTrapType.ELDER_GUARDIAN_SHOT, 6, EntityType.ELDER_GUARDIAN, NexusTrapType.GUARDIAN_SHOT, 49, Material.PRISMARINE_BRICKS, 128, Material.NAUTILUS_SHELL, 8);
   }

   /** Cadence de tir (ms) de chaque défense active. */
   private static long cooldownMs(NexusTrapType type) {
      return switch (type) {
         case SHULKER_SHOT -> 6000L;
         case WARDEN_SHOT -> 15000L;
         case EVOKER_SHOT -> 9000L;
         case GUARDIAN_SHOT -> 4000L;
         case ELDER_GUARDIAN_SHOT -> 25000L;
         case BLAZE_SHOT -> 5000L;
         case GHAST_SHOT -> 14000L;
         case BEE_SWARM -> 30000L;
         default -> Long.MAX_VALUE;
      };
   }

   private static final class State {
      long xp;
      final Set<EntityType> trophies = EnumSet.noneOf(EntityType.class);
      long dayKey;
      long dayXp;
   }

   static final class TechHolder extends BaseGUIHolder {
      String guildId;
   }

   private final EsicraftPlugin plugin;
   private final Map<String, State> states = new ConcurrentHashMap<>();
   private final Map<String, Long> nextShot = new ConcurrentHashMap<>();

   public NexusTech(EsicraftPlugin plugin) {
      this.plugin = plugin;
   }

   public void init() {
      var db = this.plugin.getDataManager().getStorage();
      db.exec("CREATE TABLE IF NOT EXISTS nexus_tech (guild_id VARCHAR(64) PRIMARY KEY, xp BIGINT DEFAULT 0, trophies TEXT DEFAULT '', day_key BIGINT DEFAULT 0, day_xp BIGINT DEFAULT 0);");
      for (Object[] row : db.select("SELECT guild_id, xp, trophies, day_key, day_xp FROM nexus_tech;", 5)) {
         State s = new State();
         s.xp = ((Number)row[1]).longValue();
         s.dayKey = ((Number)row[3]).longValue();
         s.dayXp = ((Number)row[4]).longValue();
         for (String t : String.valueOf(row[2]).split(",")) {
            try {
               if (!t.isBlank()) {
                  s.trophies.add(EntityType.valueOf(t.trim()));
               }
            } catch (IllegalArgumentException ignored) {
            }
         }

         this.states.put(String.valueOf(row[0]), s);
      }
   }

   public void clear() {
      this.states.clear();
      this.nextShot.clear();
   }

   private State state(String guildId) {
      return this.states.computeIfAbsent(guildId, k -> new State());
   }

   private void save(String guildId) {
      State s = this.state(guildId);
      StringBuilder sb = new StringBuilder();
      for (EntityType t : s.trophies) {
         sb.append(t.name()).append(',');
      }

      long xp = s.xp;
      long dk = s.dayKey;
      long dx = s.dayXp;
      String trophies = sb.toString();
      Runnable write = () -> this.plugin.getDataManager().getStorage().exec(
         "INSERT INTO nexus_tech (guild_id, xp, trophies, day_key, day_xp) VALUES (?, ?, ?, ?, ?) ON CONFLICT(guild_id) DO UPDATE SET xp = excluded.xp, trophies = excluded.trophies, day_key = excluded.day_key, day_xp = excluded.day_xp;",
         guildId, xp, trophies, dk, dx);
      if (this.plugin.isEnabled()) {
         Bukkit.getScheduler().runTaskAsynchronously(this.plugin, write);
      } else {
         write.run();
      }
   }

   // ------------------------------------------------------------------ niveau & expérience

   public long xp(String guildId) {
      return this.state(guildId).xp;
   }

   public int level(String guildId) {
      long xp = this.xp(guildId);
      int level = 1;
      for (int i = 1; i < LEVEL_XP.length; i++) {
         if (xp >= LEVEL_XP[i]) {
            level = i + 1;
         }
      }

      return level;
   }

   public int maxLevel() {
      return LEVEL_XP.length;
   }

   public Set<EntityType> trophies(String guildId) {
      return java.util.Collections.unmodifiableSet(this.state(guildId).trophies);
   }

   public int unlockedCount(Guild guild) {
      int n = 0;
      if (guild.hasNexus()) {
         for (NexusTrapType t : NODES.keySet()) {
            if (guild.getNexus().hasTrap(t)) {
               n++;
            }
         }
      }

      return n;
   }

   public static int totalNodes() {
      return NODES.size();
   }

   public void addXp(Guild guild, long amount, String why) {
      if (guild == null || amount <= 0L) {
         return;
      }

      State s = this.state(guild.getId());
      int before = this.level(guild.getId());
      s.xp += amount;
      int after = this.level(guild.getId());
      this.save(guild.getId());
      if (after > before) {
         ChatUtil.broadcastGuild(guild, "&5✨ Le Nexus de votre guilde passe au &d&lniveau " + after + " &5(" + why + ") — de nouvelles défenses sont disponibles avec &e/nexus&5 !");
         if (guild.hasNexus() && guild.getNexus().getLocation() != null) {
            Location l = guild.getNexus().getLocation();
            l.getWorld().playSound(l, Sound.BLOCK_BEACON_POWER_SELECT, 2.0F, 1.2F);
            l.getWorld().spawnParticle(Particle.END_ROD, l.clone().add(0.5, 1.0, 0.5), 60, 1.0, 1.0, 1.0, 0.1);
         }
      }
   }

   /** Une horde est repoussée : le Nexus s'enrichit de l'expérience du combat. */
   public void onRaidRepelled(Guild guild) {
      this.addXp(guild, Math.max(0L, this.plugin.getConfig().getLong("nexus.tech.xp-per-repel", 200L)), "horde repoussée");
   }

   // ------------------------------------------------------------------ sacrifice

   public void sacrifice(Player player, Guild guild) {
      Nexus nexus = guild.getNexus();
      if (nexus == null || !nexus.isAlive() || nexus.getLocation() == null) {
         ChatUtil.sendMessage(player, "&c❌ Votre guilde n'a pas de Nexus actif.");
         return;
      }

      Location loc = nexus.getLocation();
      if (!player.getWorld().equals(loc.getWorld()) || player.getLocation().distanceSquared(loc) > 100.0) {
         ChatUtil.sendMessage(player, "&c❌ Approchez-vous du Nexus (moins de 10 blocs) pour y sacrifier des ressources.");
         return;
      }

      ItemStack hand = player.getInventory().getItemInMainHand();
      Integer value = XP_VALUES.get(hand.getType());
      if (hand.getType().isAir() || value == null || hand.hasItemMeta() && hand.getItemMeta().hasEnchants()) {
         ChatUtil.sendMessage(player, "&c❌ Tenez en main une ressource brute que le Nexus accepte (charbon, fer, or, diamant, émeraude, netherite, perle, bâton de blaze…).");
         return;
      }

      State s = this.state(guild.getId());
      long today = java.time.LocalDate.now().toEpochDay();
      if (s.dayKey != today) {
         s.dayKey = today;
         s.dayXp = 0L;
      }

      long cap = Math.max(0L, this.plugin.getConfig().getLong("nexus.tech.daily-xp-cap", 3000L));
      long room = cap - s.dayXp;
      if (room <= 0L && !this.plugin.bypassCooldowns(player)) {
         ChatUtil.sendMessage(player, "&c❌ Le Nexus est rassasié pour aujourd'hui (plafond de " + cap + " XP par jour).");
         return;
      }

      int amount = hand.getAmount();
      if (!this.plugin.bypassCooldowns(player)) {
         amount = (int)Math.max(1L, Math.min(amount, room / value));
      }

      long gained = (long)amount * value;
      if (gained <= 0L) {
         ChatUtil.sendMessage(player, "&c❌ Le Nexus est rassasié pour aujourd'hui.");
         return;
      }

      hand.setAmount(hand.getAmount() - amount);
      player.getInventory().setItemInMainHand(hand.getAmount() <= 0 ? null : hand);
      s.dayXp += gained;
      loc.getWorld().spawnParticle(Particle.PORTAL, loc.clone().add(0.5, 1.0, 0.5), 40, 0.5, 0.5, 0.5, 0.8);
      loc.getWorld().playSound(loc, Sound.BLOCK_ENCHANTMENT_TABLE_USE, 1.5F, 1.0F);
      ChatUtil.sendMessage(player, "&5✨ Vous sacrifiez &e" + amount + "× " + hand.getType().name().toLowerCase(Locale.ROOT).replace('_', ' ') + " &5au Nexus : &d+" + gained + " XP&5.");
      this.addXp(guild, gained, "sacrifice de " + player.getName());
   }

   // ------------------------------------------------------------------ butin de chasse (mob ramené et tué dans le chunk du Nexus)

   @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
   public void onMobKilled(EntityDeathEvent event) {
      Player killer = event.getEntity().getKiller();
      if (killer == null) {
         return;
      }

      EntityType type = event.getEntityType();
      boolean wanted = false;
      for (Node n : NODES.values()) {
         if (n.kill() == type) {
            wanted = true;
            break;
         }
      }

      if (!wanted) {
         return;
      }

      Guild guild = this.plugin.getGuildManager().getGuildByPlayer(killer.getUniqueId());
      if (guild == null || !guild.hasNexus() || guild.getNexus().getLocation() == null) {
         return;
      }

      Location where = event.getEntity().getLocation();
      Location nexus = guild.getNexus().getLocation();
      if (where.getWorld() == null || !where.getWorld().equals(nexus.getWorld()) || where.getBlockX() >> 4 != nexus.getBlockX() >> 4 || where.getBlockZ() >> 4 != nexus.getBlockZ() >> 4) {
         return;
      }

      State s = this.state(guild.getId());
      if (s.trophies.add(type)) {
         this.save(guild.getId());
         ChatUtil.broadcastGuild(guild, "&5🏆 &e" + killer.getName() + " &5a terrassé &d" + type.name().toLowerCase(Locale.ROOT).replace('_', ' ') + " &5dans le chunk du Nexus : sa défense peut être débloquée !");
      }
   }

   // ------------------------------------------------------------------ interface

   private double costMultiplier(Guild guild) {
      double m = Math.max(0.1, this.plugin.getConfig().getDouble("nexus.tech.cost-multiplier", 1.0));
      if (guild.getSpecialization() == GuildSpecialization.UNDERGROUND_INDUSTRY) {
         m *= 0.5;
      }

      return m * this.plugin.getSynergy().trapCostMultiplier(guild);
   }

   private static int count(Player p, Material m) {
      int total = 0;
      for (ItemStack it : p.getInventory().getContents()) {
         if (it != null && it.getType() == m && !it.hasItemMeta()) {
            total += it.getAmount();
         }
      }

      return total;
   }

   private static String pretty(Material m) {
      return m.name().toLowerCase(Locale.ROOT).replace('_', ' ');
   }

   public void openGui(Player player) {
      Guild guild = this.plugin.getGuildManager().getGuildByPlayer(player.getUniqueId());
      if (guild == null || !guild.hasNexus()) {
         ChatUtil.sendMessage(player, "&cVotre guilde ne possède pas de Nexus actif.");
         return;
      }

      TechHolder holder = new TechHolder();
      holder.guildId = guild.getId();
      Inventory inv = Bukkit.createInventory(holder, 54, ChatUtil.component("&8&l🔬 Arbre technologique du Nexus"));
      this.render(inv, player, guild);
      player.openInventory(inv);
   }

   private void render(Inventory inv, Player player, Guild guild) {
      ItemStack glass = new ItemBuilder(Material.GRAY_STAINED_GLASS_PANE).name(" ").build();
      for (int i = 0; i < 54; i++) {
         inv.setItem(i, glass);
      }

      Nexus nexus = guild.getNexus();
      int level = this.level(guild.getId());
      long xp = this.xp(guild.getId());
      String next = level >= LEVEL_XP.length ? "&aNiveau maximal" : "&7Prochain niveau : &e" + LEVEL_XP[level] + " XP";
      inv.setItem(53, new ItemBuilder(Material.EXPERIENCE_BOTTLE).name("&d&lNexus niveau " + level + " &7(" + xp + " XP)").lore(next, "",
         "&7Gagnez de l'XP en &erepoussant des hordes", "&7et en sacrifiant des ressources :", "&e/nexus sacrifice &7(objet en main, près du Nexus).").build());
      inv.setItem(45, new ItemBuilder(Material.CRYING_OBSIDIAN).name("&6&lCœur de Guilde : &e" + guild.getName())
         .lore("&7Santé : &a" + nexus.getCurrentHealth() + " &7/ &a" + nexus.getMaxHealth() + " PV", "&7Défenses débloquées : &a" + this.unlockedCount(guild) + " &7/ &a" + totalNodes(),
            "", "&7Les défenses débloquées sont &apermanentes&7.", "&7Les défenses « vivantes » exigent d'avoir tué", "&7le mob dans le chunk du Nexus.").build());

      double mult = this.costMultiplier(guild);
      for (Node n : NODES.values()) {
         boolean unlocked = nexus.hasTrap(n.type());
         boolean levelOk = level >= n.level();
         boolean parentOk = n.parent() == null || nexus.hasTrap(n.parent());
         boolean killOk = n.kill() == null || this.state(guild.getId()).trophies.contains(n.kill());
         List<String> lore = new ArrayList<>();
         lore.add(n.type().getDescription());
         lore.add("");
         lore.add((levelOk ? "&a✔" : "&c✘") + " &7Niveau de Nexus requis : &e" + n.level());
         if (n.parent() != null) {
            lore.add((parentOk ? "&a✔" : "&c✘") + " &7Défense requise : " + n.parent().getDisplayName());
         }

         if (n.kill() != null) {
            lore.add((killOk ? "&a✔" : "&c✘") + " &7Tuer un(e) &e" + n.kill().name().toLowerCase(Locale.ROOT).replace('_', ' ') + " &7dans le chunk du Nexus");
         }

         lore.add("&7Objets à sacrifier :");
         for (Map.Entry<Material, Integer> c : n.cost().entrySet()) {
            int need = (int)Math.max(1, Math.round(c.getValue() * mult));
            int have = count(player, c.getKey());
            lore.add("  " + (have >= need ? "&a" : "&c") + have + "&7/&e" + need + " &f" + pretty(c.getKey()));
         }

         lore.add("");
         if (unlocked) {
            lore.add("&a&l✔ DÉBLOQUÉE (permanente)");
         } else if (levelOk && parentOk && killOk) {
            lore.add("&e➤ Cliquez pour sacrifier et débloquer !");
         } else {
            lore.add("&c✘ Conditions non remplies");
         }

         ItemBuilder b = new ItemBuilder(unlocked ? n.type().getIcon() : levelOk && parentOk && killOk ? n.type().getIcon() : Material.BARRIER).name(n.type().getDisplayName()).lore(lore).hideAllFlags();
         if (unlocked) {
            b.glow();
         }

         inv.setItem(n.slot(), b.build());
      }
   }

   @EventHandler(priority = EventPriority.HIGHEST)
   public void onClick(InventoryClickEvent event) {
      if (!(event.getWhoClicked() instanceof Player player) || !(event.getInventory().getHolder() instanceof TechHolder holder)) {
         return;
      }

      event.setCancelled(true);
      if (event.getClickedInventory() != event.getView().getTopInventory()) {
         return;
      }

      Guild guild = this.plugin.getGuildManager().getGuildById(holder.guildId);
      if (guild == null || !guild.hasNexus() || !guild.isMember(player.getUniqueId())) {
         player.closeInventory();
         return;
      }

      Node node = null;
      for (Node n : NODES.values()) {
         if (n.slot() == event.getRawSlot()) {
            node = n;
         }
      }

      if (node == null) {
         return;
      }

      if (!guild.hasRoleAtLeast(player.getUniqueId(), GuildRole.OFFICER)) {
         ChatUtil.sendMessage(player, "&cSeuls les officiers et chefs peuvent débloquer des défenses.");
         ChatUtil.playSound(player, Sound.ENTITY_VILLAGER_NO, 1.0F, 1.0F);
         return;
      }

      this.tryUnlock(player, guild, node);
      this.render(event.getInventory(), player, guild);
   }

   private void tryUnlock(Player player, Guild guild, Node n) {
      Nexus nexus = guild.getNexus();
      if (nexus.hasTrap(n.type())) {
         ChatUtil.sendMessage(player, "&aCette défense est déjà débloquée.");
         return;
      }

      if (this.level(guild.getId()) < n.level()) {
         ChatUtil.sendMessage(player, "&c❌ Niveau de Nexus insuffisant (niveau &e" + n.level() + " &crequis).");
         return;
      }

      if (n.parent() != null && !nexus.hasTrap(n.parent())) {
         ChatUtil.sendMessage(player, "&c❌ Débloquez d'abord : " + n.parent().getDisplayName());
         return;
      }

      if (n.kill() != null && !this.state(guild.getId()).trophies.contains(n.kill())) {
         ChatUtil.sendMessage(player, "&c❌ Il faut d'abord tuer ce mob dans le chunk du Nexus : &e" + n.kill().name().toLowerCase(Locale.ROOT).replace('_', ' ') + "&c.");
         return;
      }

      double mult = this.costMultiplier(guild);
      Map<Material, Integer> need = new EnumMap<>(Material.class);
      for (Map.Entry<Material, Integer> c : n.cost().entrySet()) {
         int amount = (int)Math.max(1, Math.round(c.getValue() * mult));
         if (count(player, c.getKey()) < amount) {
            ChatUtil.sendMessage(player, "&c❌ Il vous manque des objets à sacrifier : &e" + amount + "× " + pretty(c.getKey()) + "&c.");
            ChatUtil.playSound(player, Sound.ENTITY_VILLAGER_NO, 1.0F, 1.0F);
            return;
         }

         need.put(c.getKey(), amount);
      }

      for (Map.Entry<Material, Integer> e : need.entrySet()) {
         int left = e.getValue();
         for (ItemStack it : player.getInventory().getContents()) {
            if (left <= 0) {
               break;
            }

            if (it != null && it.getType() == e.getKey() && !it.hasItemMeta()) {
               int take = Math.min(left, it.getAmount());
               it.setAmount(it.getAmount() - take);
               left -= take;
            }
         }
      }

      nexus.addTrap(n.type());
      if (n.type() == NexusTrapType.GUARDIAN_GOLEM && nexus.getLocation() != null && nexus.getLocation().getWorld() != null) {
         this.plugin.getNexusManager().removeGuardians(guild, nexus.getLocation());
         this.plugin.getNexusManager().spawnGuardian(guild, nexus.getLocation());
      }

      this.plugin.getDataManager().saveGuild(guild);
      ChatUtil.playSound(player, Sound.ENTITY_PLAYER_LEVELUP, 1.0F, 1.0F);
      ChatUtil.broadcastGuild(guild, "&5🔬 &e" + player.getName() + " &5débloque pour le Nexus : " + n.type().getDisplayName() + " &5!");
      if (this.unlockedCount(guild) >= totalNodes()) {
         this.plugin.getAchievements().grantGuild(guild, Achievement.TECH_MASTER, false);
      }
   }

   // ------------------------------------------------------------------ tirs automatiques

   private static void beam(Location from, Location to, Particle particle, int points) {
      World w = from.getWorld();
      for (int i = 0; i <= points; i++) {
         double t = (double)i / points;
         w.spawnParticle(particle, from.getX() + (to.getX() - from.getX()) * t, from.getY() + (to.getY() - from.getY()) * t, from.getZ() + (to.getZ() - from.getZ()) * t, 1, 0, 0, 0, 0);
      }
   }

   private double shotMultiplier() {
      return Math.max(0.0, this.plugin.getConfig().getDouble("nexus.tech.shot-damage-multiplier", 1.0));
   }

   private boolean ready(Guild guild, NexusTrapType type, long now) {
      String key = guild.getId() + ":" + type.name();
      Long next = this.nextShot.get(key);
      if (next != null && now < next) {
         return false;
      }

      this.nextShot.put(key, now + cooldownMs(type));
      return true;
   }

   /** Appelé par la boucle du Nexus : fait feu avec chaque défense « tir » débloquée sur les ennemis proches. */
   public void tickDefenses(Guild guild, Nexus nexus, Location loc, List<LivingEntity> enemies) {
      if (enemies.isEmpty()) {
         return;
      }

      long now = System.currentTimeMillis();
      Location muzzle = loc.clone().add(0.5, 1.8, 0.5);
      LivingEntity target = null;
      double best = Double.MAX_VALUE;
      for (LivingEntity e : enemies) {
         double d = e.getLocation().distanceSquared(loc);
         if (d < best) {
            best = d;
            target = e;
         }
      }

      if (target == null) {
         return;
      }

      World w = loc.getWorld();
      double dm = this.shotMultiplier();
      Location tl = target.getLocation().add(0, 1.0, 0);
      if (nexus.hasTrap(NexusTrapType.SHULKER_SHOT) && this.ready(guild, NexusTrapType.SHULKER_SHOT, now)) {
         beam(muzzle, tl, Particle.END_ROD, 14);
         target.damage(4.0 * dm);
         target.addPotionEffect(new PotionEffect(PotionEffectType.LEVITATION, 50, 0, false, true, true));
         w.playSound(loc, Sound.ENTITY_SHULKER_SHOOT, 1.5F, 1.0F);
      }

      if (nexus.hasTrap(NexusTrapType.WARDEN_SHOT) && this.ready(guild, NexusTrapType.WARDEN_SHOT, now)) {
         beam(muzzle, tl, Particle.SONIC_BOOM, 5);
         target.damage(10.0 * dm);
         target.setVelocity(target.getLocation().toVector().subtract(loc.toVector()).setY(0.3).normalize().multiply(0.9));
         w.playSound(loc, Sound.ENTITY_WARDEN_SONIC_BOOM, 2.0F, 1.0F);
      }

      if (nexus.hasTrap(NexusTrapType.EVOKER_SHOT) && this.ready(guild, NexusTrapType.EVOKER_SHOT, now)) {
         int steps = 6;
         for (int i = 1; i <= steps; i++) {
            double t = (double)i / steps;
            Location p = muzzle.clone().add(tl.clone().subtract(muzzle).toVector().multiply(t));
            p.setY(Math.max(w.getHighestBlockYAt(p) + 1, p.getY() - 1.5));
            Bukkit.getScheduler().runTaskLater(this.plugin, () -> w.spawnParticle(Particle.WITCH, p, 12, 0.3, 0.6, 0.3, 0.05), i * 2L);
         }

         final LivingEntity victim = target;
         Bukkit.getScheduler().runTaskLater(this.plugin, () -> {
            for (LivingEntity e : enemies) {
               if (e.isValid() && e.getLocation().distanceSquared(victim.getLocation()) <= 9.0) {
                  e.damage(6.0 * dm);
               }
            }
         }, steps * 2L);
         w.playSound(loc, Sound.ENTITY_EVOKER_PREPARE_ATTACK, 1.5F, 1.0F);
      }

      if (nexus.hasTrap(NexusTrapType.GUARDIAN_SHOT) && this.ready(guild, NexusTrapType.GUARDIAN_SHOT, now)) {
         beam(muzzle, tl, Particle.BUBBLE_POP, 16);
         target.damage(3.0 * dm);
         target.addPotionEffect(new PotionEffect(PotionEffectType.SLOWNESS, 60, 0, false, true, true));
         w.playSound(loc, Sound.ENTITY_GUARDIAN_ATTACK, 1.2F, 1.2F);
      }

      if (nexus.hasTrap(NexusTrapType.ELDER_GUARDIAN_SHOT) && this.ready(guild, NexusTrapType.ELDER_GUARDIAN_SHOT, now)) {
         for (LivingEntity e : enemies) {
            e.damage(4.0 * dm);
            e.addPotionEffect(new PotionEffect(PotionEffectType.MINING_FATIGUE, 200, 2, false, true, true));
            if (e instanceof Player p) {
               p.spawnParticle(Particle.ELDER_GUARDIAN, p.getLocation(), 1);
            }
         }

         w.playSound(loc, Sound.ENTITY_ELDER_GUARDIAN_CURSE, 2.0F, 1.0F);
      }

      if (nexus.hasTrap(NexusTrapType.BLAZE_SHOT) && this.ready(guild, NexusTrapType.BLAZE_SHOT, now)) {
         int shots = Math.min(3, enemies.size());
         for (int i = 0; i < shots; i++) {
            LivingEntity e = enemies.get(i);
            beam(muzzle, e.getLocation().add(0, 1.0, 0), Particle.FLAME, 12);
            e.damage(3.0 * dm);
            e.setFireTicks(80);
         }

         w.playSound(loc, Sound.ENTITY_BLAZE_SHOOT, 1.5F, 1.0F);
      }

      if (nexus.hasTrap(NexusTrapType.GHAST_SHOT) && this.ready(guild, NexusTrapType.GHAST_SHOT, now)) {
         beam(muzzle, tl, Particle.LARGE_SMOKE, 16);
         w.spawnParticle(Particle.EXPLOSION, tl, 3, 0.8, 0.5, 0.8, 0.0);
         w.playSound(tl, Sound.ENTITY_GENERIC_EXPLODE, 2.0F, 1.0F);
         for (LivingEntity e : enemies) {
            if (e.getLocation().distanceSquared(target.getLocation()) <= 16.0) {
               e.damage(8.0 * dm);
               e.setFireTicks(60);
            }
         }
      }

      if (nexus.hasTrap(NexusTrapType.BEE_SWARM) && this.ready(guild, NexusTrapType.BEE_SWARM, now)) {
         String tag = NexusManager.guardianTag(guild);
         int existing = 0;
         for (Entity e : w.getNearbyEntities(loc, 24, 24, 24)) {
            if (e instanceof Bee && e.getScoreboardTags().contains(tag)) {
               existing++;
            }
         }

         for (int i = existing; i < 6; i++) {
            Location s = loc.clone().add(ThreadLocalRandom.current().nextDouble(-2, 2), 2.0, ThreadLocalRandom.current().nextDouble(-2, 2));
            LivingEntity victim = target;
            w.spawn(s, Bee.class, bee -> {
               bee.addScoreboardTag(tag);
               bee.customName(ChatUtil.component("&6🐝 Abeille du Nexus"));
               bee.setAnger(600);
               bee.setCannotEnterHiveTicks(Integer.MAX_VALUE);
               bee.setTarget(victim);
               bee.setPersistent(false);
               Bukkit.getScheduler().runTaskLater(this.plugin, () -> {
                  if (bee.isValid()) {
                     bee.remove();
                  }
               }, 20L * 60);
            });
         }
      }
   }
}
