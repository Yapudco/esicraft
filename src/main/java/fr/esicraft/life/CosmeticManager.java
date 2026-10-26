package fr.esicraft.life;

import fr.esicraft.EsicraftPlugin;
import fr.esicraft.data.PlayerData;
import fr.esicraft.util.ChatUtil;
import fr.esicraft.util.ItemBuilder;
import java.time.Duration;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import net.kyori.adventure.title.Title;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.Particle;
import org.bukkit.Registry;
import org.bukkit.Sound;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Item;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityPickupItemEvent;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.jetbrains.annotations.NotNull;

/**
 * Cosmétiques, en trois emplacements : <b>aura</b> (particules autour de soi), <b>traînée</b> (particules derrière ses pas) et
 * <b>effet de victoire</b> (explosion de particules quand on tue un joueur). Un cosmétique de chaque emplacement au plus est équipé.
 * Trois façons d'en obtenir : <b>un succès</b> (un aura chacun), <b>une quête</b> (compteurs de jeu : contrats accomplis, monstres,
 * trajets en train, victoires PvP…) ou <b>l'achat</b> auprès du serveur (pièces personnelles). Un aura secret s'y ajoute.
 */
public class CosmeticManager implements Listener, CommandExecutor {
   private static final String KEY_DIAMOND = "cosmetic:diamond:";
   private static final String KEY_BOUGHT = "cos_buy:";
   private static final String DIAMOND_AURA = "aura:DIAMOND";
   private static final int PER_PAGE = 36;

   /** Emplacement d'un cosmétique ; {@code key} : préfixe de stockage de ce qui est équipé. */
   enum Slot {
      AURA("cos_aura:", "Auras", Material.BLAZE_POWDER), TRAIL("cos_trail:", "Traînées", Material.FEATHER), BURST("cos_burst:", "Effets de victoire", Material.FIREWORK_ROCKET);

      final String key;
      final String label;
      final Material icon;

      Slot(String key, String label, Material icon) {
         this.key = key;
         this.label = label;
         this.icon = icon;
      }
   }

   private enum Kind {
      ACHIEVEMENT, QUEST, SHOP, SECRET
   }

   /** {@code key} : nom du succès, ou statistique de la quête ; {@code amount} : seuil de la quête ou prix en pièces. */
   private record Cosmetic(String id, Slot slot, String name, Material icon, Particle particle, int count, Kind kind, String key, long amount, String hint) {
   }

   private final EsicraftPlugin plugin;
   private final Map<String, Cosmetic> catalog = new LinkedHashMap<>();
   private final Map<Slot, List<Cosmetic>> bySlot = new EnumMap<>(Slot.class);
   private final Set<UUID> diamond = ConcurrentHashMap.newKeySet();
   private final Map<UUID, Set<String>> bought = new ConcurrentHashMap<>();
   private final Map<Slot, Map<UUID, String>> equipped = new EnumMap<>(Slot.class);
   private final Map<UUID, Location> lastSeen = new ConcurrentHashMap<>();
   private int tick;

   private static final class Holder implements InventoryHolder {
      Inventory inv;
      Slot slot = Slot.AURA;
      int page;
      String pendingBuy;

      public @NotNull Inventory getInventory() {
         return this.inv;
      }
   }

   public CosmeticManager(EsicraftPlugin plugin) {
      this.plugin = plugin;
   }

   // ------------------------------------------------------------------ catalogue

   private Particle particle(String key) {
      Particle p = Registry.PARTICLE_TYPE.get(NamespacedKey.minecraft(key));
      return p != null ? p : Particle.END_ROD;
   }

   private void add(Slot slot, String id, String name, Material icon, String particle, int count, Kind kind, String key, long amount, String hint) {
      Cosmetic c = new Cosmetic(slot.name().toLowerCase(Locale.ROOT) + ":" + id, slot, name, icon, this.particle(particle), count, kind, key, amount, hint);
      this.catalog.put(c.id(), c);
      this.bySlot.computeIfAbsent(slot, s -> new ArrayList<>()).add(c);
   }

   private void buildCatalog() {
      // Un aura par succès.
      String[] keys = {"happy_villager", "heart", "note", "crit", "flame", "soul_fire_flame", "totem_of_undying", "firework", "enchant", "snowflake", "cherry_leaves",
         "electric_spark", "glow", "sculk_soul", "witch", "soul", "ash", "wax_on", "composter", "scrape", "small_flame", "bubble_pop", "campfire_cosy_smoke", "cloud",
         "spore_blossom_air", "dripping_obsidian_tear"};
      Material[] icons = {Material.EMERALD, Material.PINK_DYE, Material.NOTE_BLOCK, Material.IRON_SWORD, Material.BLAZE_POWDER, Material.SOUL_LANTERN, Material.TOTEM_OF_UNDYING,
         Material.FIREWORK_STAR, Material.ENCHANTED_BOOK, Material.SNOWBALL, Material.CHERRY_LEAVES, Material.LIGHTNING_ROD, Material.GLOW_INK_SAC, Material.ECHO_SHARD,
         Material.FERMENTED_SPIDER_EYE, Material.SOUL_SAND, Material.GUNPOWDER, Material.HONEYCOMB, Material.COMPOSTER, Material.COPPER_INGOT, Material.TORCH,
         Material.PRISMARINE_SHARD, Material.CAMPFIRE, Material.WHITE_WOOL, Material.SPORE_BLOSSOM, Material.CRYING_OBSIDIAN};
      Achievement[] all = Achievement.values();
      for (int i = 0; i < all.length; i++) {
         this.add(Slot.AURA, all[i].name(), "Aura : " + all[i].getTitle(), icons[i % icons.length], keys[i % keys.length], 2, Kind.ACHIEVEMENT, all[i].name(), 0L,
            "Succès « " + all[i].getTitle() + " » : " + all[i].getDescription());
      }

      this.add(Slot.AURA, "DIAMOND", "Aura : Diamant", Material.DIAMOND, "end_rod", 2, Kind.SECRET, "diamond", 0L, "Un secret à découvrir…");

      // Quêtes : compteurs de jeu (aura / traînée / effet de victoire).
      String contracts = AchievementManager.STAT_CONTRACTS;
      String monsters = AchievementManager.STAT_MONSTERS;
      String trips = AchievementManager.STAT_TRIPS;
      String helps = AchievementManager.STAT_HELPS;
      String kills = AchievementManager.STAT_PVP_KILLS;
      String defenses = AchievementManager.STAT_DEFENSES;
      String raids = AchievementManager.STAT_RAIDS;
      this.add(Slot.AURA, "q_contracts10", "Aura : Travailleur acharné", Material.IRON_PICKAXE, "wax_on", 2, Kind.QUEST, contracts, 10L, "Accomplir 10 contrats du jour");
      this.add(Slot.AURA, "q_monsters250", "Aura : Chasseur de monstres", Material.BOW, "ash", 3, Kind.QUEST, monsters, 250L, "Éliminer 250 monstres");
      this.add(Slot.AURA, "q_trips20", "Aura : Cheminot", Material.MINECART, "cloud", 3, Kind.QUEST, trips, 20L, "Prendre le train 20 fois");
      this.add(Slot.AURA, "q_helps5", "Aura : Bon voisin", Material.GLOW_BERRIES, "glow", 2, Kind.QUEST, helps, 5L, "Répondre à 5 appels à l'aide");
      this.add(Slot.AURA, "q_defenses5", "Aura : Rempart", Material.SHIELD, "small_flame", 2, Kind.QUEST, defenses, 5L, "Repousser 5 hordes");
      this.add(Slot.TRAIL, "q_trips25", "Traînée : Nuages de vapeur", Material.WHITE_STAINED_GLASS, "cloud", 2, Kind.QUEST, trips, 25L, "Prendre le train 25 fois");
      this.add(Slot.TRAIL, "q_monsters1000", "Traînée : Sillage sanglant", Material.NETHERITE_SWORD, "crit", 3, Kind.QUEST, monsters, 1000L, "Éliminer 1 000 monstres");
      this.add(Slot.TRAIL, "q_helps10", "Traînée : Cœurs d'allié", Material.PINK_TULIP, "heart", 1, Kind.QUEST, helps, 10L, "Répondre à 10 appels à l'aide");
      this.add(Slot.TRAIL, "q_defenses10", "Traînée : Braises du Rempart", Material.MAGMA_BLOCK, "flame", 2, Kind.QUEST, defenses, 10L, "Repousser 10 hordes");
      this.add(Slot.TRAIL, "q_contracts50", "Traînée : Poussière d'or", Material.GOLD_NUGGET, "wax_on", 3, Kind.QUEST, contracts, 50L, "Accomplir 50 contrats du jour");
      this.add(Slot.BURST, "q_kills5", "Victoire : Feu d'artifice", Material.FIREWORK_ROCKET, "firework", 40, Kind.QUEST, kills, 5L, "Tuer 5 joueurs d'autres guildes");
      this.add(Slot.BURST, "q_kills25", "Victoire : Âmes libérées", Material.SOUL_TORCH, "soul", 40, Kind.QUEST, kills, 25L, "Tuer 25 joueurs d'autres guildes");
      this.add(Slot.BURST, "q_kills100", "Victoire : Éruption", Material.LAVA_BUCKET, "lava", 40, Kind.QUEST, kills, 100L, "Tuer 100 joueurs d'autres guildes");
      this.add(Slot.BURST, "q_raids3", "Victoire : Fanfare", Material.GOAT_HORN, "note", 30, Kind.QUEST, raids, 3L, "Lancer 3 hordes");
      this.add(Slot.BURST, "q_defenses20", "Victoire : Éclat du Rempart", Material.TOTEM_OF_UNDYING, "totem_of_undying", 40, Kind.QUEST, defenses, 20L, "Repousser 20 hordes");

      // Boutique du serveur (pièces personnelles).
      this.add(Slot.AURA, "s_cherry", "Aura : Cerisier en fleur", Material.CHERRY_SAPLING, "cherry_leaves", 2, Kind.SHOP, "", 4000L, "");
      this.add(Slot.AURA, "s_enchant", "Aura : Ensorcelée", Material.ENCHANTING_TABLE, "enchant", 4, Kind.SHOP, "", 6000L, "");
      this.add(Slot.AURA, "s_electric", "Aura : Électrique", Material.COPPER_BLOCK, "electric_spark", 2, Kind.SHOP, "", 9000L, "");
      this.add(Slot.AURA, "s_soulfire", "Aura : Flamme d'âme", Material.SOUL_CAMPFIRE, "soul_fire_flame", 2, Kind.SHOP, "", 12000L, "");
      this.add(Slot.AURA, "s_hearts", "Aura : Cœurs", Material.RED_DYE, "heart", 1, Kind.SHOP, "", 15000L, "");
      this.add(Slot.TRAIL, "s_endrod", "Traînée : Étincelles", Material.END_ROD, "end_rod", 2, Kind.SHOP, "", 3000L, "");
      this.add(Slot.TRAIL, "s_note", "Traînée : Notes de musique", Material.JUKEBOX, "note", 1, Kind.SHOP, "", 4000L, "");
      this.add(Slot.TRAIL, "s_cherry", "Traînée : Pétales", Material.PINK_PETALS, "cherry_leaves", 3, Kind.SHOP, "", 5000L, "");
      this.add(Slot.TRAIL, "s_soul", "Traînée : Âmes errantes", Material.SOUL_SOIL, "soul", 2, Kind.SHOP, "", 8000L, "");
      this.add(Slot.TRAIL, "s_electric", "Traînée : Étincelles électriques", Material.LIGHTNING_ROD, "electric_spark", 3, Kind.SHOP, "", 10000L, "");
      this.add(Slot.BURST, "s_smoke", "Victoire : Fumée noire", Material.COAL_BLOCK, "large_smoke", 30, Kind.SHOP, "", 3000L, "");
      this.add(Slot.BURST, "s_confetti", "Victoire : Confettis", Material.EMERALD, "happy_villager", 40, Kind.SHOP, "", 4000L, "");
      this.add(Slot.BURST, "s_hearts", "Victoire : Pluie de cœurs", Material.PINK_DYE, "heart", 25, Kind.SHOP, "", 6000L, "");
      this.add(Slot.BURST, "s_totem", "Victoire : Totem", Material.TOTEM_OF_UNDYING, "totem_of_undying", 50, Kind.SHOP, "", 9000L, "");
      this.add(Slot.BURST, "s_electric", "Victoire : Tempête électrique", Material.LIGHTNING_ROD, "electric_spark", 60, Kind.SHOP, "", 12000L, "");
   }

   public void init() {
      this.buildCatalog();
      for (Slot s : Slot.values()) {
         this.equipped.put(s, new ConcurrentHashMap<>());
      }

      var db = this.plugin.getDataManager().getStorage();
      for (Object[] row : db.select("SELECT k, v FROM kv WHERE k LIKE 'cosmetic:diamond:%' OR k LIKE 'cos_aura:%' OR k LIKE 'cos_trail:%' OR k LIKE 'cos_burst:%' OR k LIKE 'cos_buy:%';", 2)) {
         String k = (String)row[0];
         String v = (String)row[1];
         try {
            if (k.startsWith(KEY_DIAMOND)) {
               this.diamond.add(UUID.fromString(k.substring(KEY_DIAMOND.length())));
            } else if (k.startsWith(KEY_BOUGHT)) {
               Set<String> set = ConcurrentHashMap.newKeySet();
               for (String id : v.split(",")) {
                  if (this.catalog.containsKey(id)) {
                     set.add(id);
                  }
               }

               this.bought.put(UUID.fromString(k.substring(KEY_BOUGHT.length())), set);
            } else {
               for (Slot s : Slot.values()) {
                  if (k.startsWith(s.key) && this.catalog.containsKey(v)) {
                     this.equipped.get(s).put(UUID.fromString(k.substring(s.key.length())), v);
                  }
               }
            }
         } catch (IllegalArgumentException ignored) {
         }
      }

      Bukkit.getScheduler().runTaskTimer(this.plugin, this::tickEffects, 40L, 5L);
   }

   // ------------------------------------------------------------------ état

   private boolean owns(UUID uuid, Cosmetic c) {
      return switch (c.kind()) {
         case ACHIEVEMENT -> {
            Achievement a = Achievement.parse(c.key());
            yield a != null && this.plugin.getAchievements().has(uuid, a);
         }
         case QUEST -> this.plugin.getAchievements().stat(uuid, c.key()) >= c.amount();
         case SHOP -> this.bought.getOrDefault(uuid, Set.of()).contains(c.id());
         case SECRET -> this.diamond.contains(uuid);
      };
   }

   private int ownedCount(UUID uuid) {
      int n = 0;
      for (Cosmetic c : this.catalog.values()) {
         n += this.owns(uuid, c) ? 1 : 0;
      }

      return n;
   }

   /** Particules des cosmétiques équipés (joueurs EN LIGNE seulement) : aura toutes les 10 ticks, traînée (si le joueur s'est déplacé) toutes les 5 ticks. */
   private void tickEffects() {
      this.tick++;
      boolean auraTurn = this.tick % 2 == 0;
      for (Player p : Bukkit.getOnlinePlayers()) {
         UUID uuid = p.getUniqueId();
         String auraId = auraTurn ? this.equipped.get(Slot.AURA).get(uuid) : null;
         String trailId = this.equipped.get(Slot.TRAIL).get(uuid);
         if (auraId == null && trailId == null) {
            continue;
         }

         if (this.visible(uuid) == null) {
            continue;
         }

         Cosmetic aura = auraId == null ? null : this.catalog.get(auraId);
         if (aura != null && this.owns(uuid, aura)) {
            p.getWorld().spawnParticle(aura.particle(), p.getLocation().add(0.0, 1.0, 0.0), aura.count(), 0.45, 0.7, 0.45, 0.01);
         }

         Cosmetic trail = trailId == null ? null : this.catalog.get(trailId);
         if (trail != null && this.owns(uuid, trail)) {
            Location now = p.getLocation();
            Location before = this.lastSeen.put(uuid, now);
            if (before != null && before.getWorld() == now.getWorld() && before.distanceSquared(now) > 0.04) {
               p.getWorld().spawnParticle(trail.particle(), now.clone().add(0.0, 0.15, 0.0), trail.count(), 0.15, 0.05, 0.15, 0.01);
            }
         }
      }
   }

   @EventHandler
   public void onQuit(org.bukkit.event.player.PlayerQuitEvent event) {
      this.lastSeen.remove(event.getPlayer().getUniqueId());
   }

   private Player visible(UUID uuid) {
      Player p = Bukkit.getPlayer(uuid);
      return p != null && !p.isDead() && p.getGameMode() != org.bukkit.GameMode.SPECTATOR && this.plugin.getDisguiseManager().get(uuid) == null ? p : null;
   }

   @EventHandler(priority = EventPriority.MONITOR)
   public void onKill(PlayerDeathEvent event) {
      Player killer = event.getEntity().getKiller();
      if (killer == null || killer.equals(event.getEntity())) {
         return;
      }

      String id = this.equipped.get(Slot.BURST).get(killer.getUniqueId());
      Cosmetic c = id == null ? null : this.catalog.get(id);
      if (c != null && this.owns(killer.getUniqueId(), c) && this.visible(killer.getUniqueId()) != null) {
         Location at = event.getEntity().getLocation().add(0.0, 1.0, 0.0);
         at.getWorld().spawnParticle(c.particle(), at, c.count(), 0.5, 0.8, 0.5, 0.08);
         at.getWorld().playSound(at, Sound.ENTITY_FIREWORK_ROCKET_BLAST, 0.8F, 1.2F);
      }
   }

   private void save(String key, String value) {
      Bukkit.getScheduler().runTaskAsynchronously(this.plugin, () -> {
         if (value == null) {
            this.plugin.getDataManager().getStorage().exec("DELETE FROM kv WHERE k = ?;", key);
         } else {
            this.plugin.getDataManager().getStorage().exec("INSERT INTO kv (k, v) VALUES (?, ?) ON CONFLICT(k) DO UPDATE SET v = excluded.v;", key, value);
         }
      });
   }

   private void equip(Player p, Cosmetic c) {
      UUID uuid = p.getUniqueId();
      Map<UUID, String> slot = this.equipped.get(c.slot());
      if (c.id().equals(slot.get(uuid))) {
         slot.remove(uuid);
         this.save(c.slot().key + uuid, null);
         ChatUtil.sendMessage(p, "&7Cosmétique retiré.");
      } else {
         slot.put(uuid, c.id());
         this.save(c.slot().key + uuid, c.id());
         ChatUtil.sendMessage(p, "&a✔ Équipé : &f" + c.name());
         ChatUtil.playSound(p, Sound.ENTITY_PLAYER_LEVELUP, 0.6F, 1.8F);
      }
   }

   private void clearAll(UUID uuid) {
      for (Slot s : Slot.values()) {
         if (this.equipped.get(s).remove(uuid) != null) {
            this.save(s.key + uuid, null);
         }
      }
   }

   private void buy(Player p, Cosmetic c) {
      if (this.plugin.getCombatManager().denySpending(p)) {
         return;
      }

      long price = Math.max(0L, Math.round(c.amount() * this.plugin.getConfig().getDouble("cosmetics.price-multiplier", 1.0)));
      PlayerData data = this.plugin.getDataManager().getPlayerData(p.getUniqueId());
      if (data == null || !data.removeCoins((double)price)) {
         ChatUtil.sendMessage(p, "&c❌ Il vous faut &6" + price + " pièces &cpour acheter ce cosmétique.");
         ChatUtil.playSound(p, Sound.ENTITY_VILLAGER_NO, 1.0F, 1.0F);
         return;
      }

      this.plugin.getDataManager().savePlayer(data);
      Set<String> set = this.bought.computeIfAbsent(p.getUniqueId(), k -> ConcurrentHashMap.newKeySet());
      set.add(c.id());
      this.save(KEY_BOUGHT + p.getUniqueId(), String.join(",", set));
      ChatUtil.sendMessage(p, "&a✔ Acheté pour &6" + price + " pièces &a: &f" + c.name() + "&a. Cliquez dessus pour l'équiper.");
      ChatUtil.playSound(p, Sound.ENTITY_PLAYER_LEVELUP, 1.0F, 1.4F);
   }

   /** Annonce l'aura gagné avec un succès. */
   public void onAchievement(Player player, Achievement a) {
      ChatUtil.sendMessage(player, "&b🎁 Cosmétique débloqué : &eAura " + a.getTitle() + "&b. &7/cosmetique pour l'équiper.");
   }

   // ------------------------------------------------------------------ easter egg du diamant

   @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
   public void onPickup(EntityPickupItemEvent event) {
      if (!(event.getEntity() instanceof Player receiver)) {
         return;
      }

      Item dropped = event.getItem();
      UUID thrower = dropped.getThrower();
      if (thrower == null || thrower.equals(receiver.getUniqueId()) || dropped.getItemStack().getType() != Material.DIAMOND || this.diamond.contains(thrower)) {
         return;
      }

      Player giver = Bukkit.getPlayer(thrower);
      if (giver == null || !this.diamond.add(thrower)) {
         return;
      }

      this.save(KEY_DIAMOND + thrower, "1");
      if (!this.equipped.get(Slot.AURA).containsKey(thrower)) {
         this.equipped.get(Slot.AURA).put(thrower, DIAMOND_AURA);
         this.save(Slot.AURA.key + thrower, DIAMOND_AURA);
      }

      giver.showTitle(Title.title(ChatUtil.component("&b&l💎 Diamonds to you !"), ChatUtil.component("&7Cosmétique débloqué : &bAura de diamant &7(/cosmetique)"),
         Title.Times.times(Duration.ofMillis(300L), Duration.ofMillis(3000L), Duration.ofMillis(800L))));
      ChatUtil.sendMessage(giver, "&b💎 &lDiamonds to you ! &7Votre générosité est récompensée : vous débloquez l'&bAura de diamant&7 (équipée). &e/cosmetique &7pour la changer.");
      ChatUtil.playSound(giver, Sound.ENTITY_PLAYER_LEVELUP, 1.0F, 2.0F);
      ChatUtil.sendMessage(receiver, "&b💎 &e" + giver.getName() + " &7vous offre un diamant !");
      giver.getWorld().spawnParticle(Particle.END_ROD, giver.getLocation().add(0.0, 1.0, 0.0), 40, 0.5, 0.8, 0.5, 0.05);
   }

   // ------------------------------------------------------------------ menu

   private void open(Player p, Slot slot, int page) {
      Holder holder = new Holder();
      holder.slot = slot;
      holder.page = page;
      UUID uuid = p.getUniqueId();
      Inventory inv = Bukkit.createInventory(holder, 54, ChatUtil.component("&8» &b✨ Cosmétiques &7(" + this.ownedCount(uuid) + "/" + this.catalog.size() + ")"));
      holder.inv = inv;
      int[] tabSlots = {1, 3, 5};
      for (int i = 0; i < Slot.values().length; i++) {
         Slot s = Slot.values()[i];
         inv.setItem(tabSlots[i], new ItemBuilder(s.icon).name((s == slot ? "&a▶ " : "&f") + s.label).lore("&7Cliquez pour afficher cette catégorie.").glow(s == slot).build());
      }

      inv.setItem(7, new ItemBuilder(Material.BARRIER).name("&cTout retirer").lore("&7Retire l'aura, la traînée et l'effet équipés.").build());
      List<Cosmetic> list = this.bySlot.getOrDefault(slot, List.of());
      String equippedId = this.equipped.get(slot).get(uuid);
      for (int i = 0; i < PER_PAGE; i++) {
         int idx = page * PER_PAGE + i;
         if (idx >= list.size()) {
            break;
         }

         inv.setItem(9 + i, this.item(uuid, list.get(idx), list.get(idx).id().equals(equippedId), holder));
      }

      int pages = Math.max(1, (list.size() + PER_PAGE - 1) / PER_PAGE);
      if (page > 0) {
         inv.setItem(45, new ItemBuilder(Material.ARROW).name("&e◀ Page précédente").build());
      }

      if (page + 1 < pages) {
         inv.setItem(53, new ItemBuilder(Material.ARROW).name("&ePage suivante ▶").build());
      }

      inv.setItem(49, new ItemBuilder(Material.PAPER).name("&7Page " + (page + 1) + " / " + pages).lore("&8Succès : un aura chacun", "&8Quêtes : objectifs de jeu", "&8Boutique : pièces").build());
      p.openInventory(inv);
   }

   private org.bukkit.inventory.ItemStack item(UUID uuid, Cosmetic c, boolean equipped, Holder holder) {
      if (this.owns(uuid, c)) {
         return new ItemBuilder(c.icon()).name("&b" + c.name()).lore(equipped ? "&a✔ Équipé &7(clic pour retirer)" : "&e➤ Clic pour équiper").glow(equipped).build();
      }

      List<String> lore = new ArrayList<>();
      switch (c.kind()) {
         case ACHIEVEMENT -> {
            lore.add("&8Verrouillé");
            lore.add("&7" + c.hint());
         }
         case QUEST -> {
            long have = Math.min(c.amount(), this.plugin.getAchievements().stat(uuid, c.key()));
            lore.add("&8Quête");
            lore.add("&7" + c.hint());
            lore.add("&7Progression : &f" + have + " / " + c.amount());
         }
         case SHOP -> {
            long price = Math.max(0L, Math.round(c.amount() * this.plugin.getConfig().getDouble("cosmetics.price-multiplier", 1.0)));
            boolean confirm = c.id().equals(holder.pendingBuy);
            lore.add("&8Boutique du serveur");
            lore.add("&7Prix : &6" + price + " pièces");
            lore.add(confirm ? "&c➤ Cliquez encore pour confirmer l'achat" : "&e➤ Clic pour acheter");
         }
         case SECRET -> lore.add("&7" + c.hint());
      }

      Material shown = c.kind() == Kind.SHOP ? c.icon() : Material.GRAY_DYE;
      String title = c.kind() == Kind.SECRET ? "&7??? Aura secrète" : (c.kind() == Kind.SHOP ? "&6" + c.name() : "&7" + c.name());
      return new ItemBuilder(shown).name(title).lore(lore).build();
   }

   @EventHandler
   public void onClick(InventoryClickEvent event) {
      if (!(event.getInventory().getHolder() instanceof Holder holder) || !(event.getWhoClicked() instanceof Player p)) {
         return;
      }

      event.setCancelled(true);
      if (event.getClickedInventory() != event.getInventory()) {
         return;
      }

      int slot = event.getSlot();
      UUID uuid = p.getUniqueId();
      if (slot == 1 || slot == 3 || slot == 5) {
         this.open(p, Slot.values()[(slot - 1) / 2], 0);
      } else if (slot == 7) {
         this.clearAll(uuid);
         ChatUtil.sendMessage(p, "&7Cosmétiques retirés.");
         this.open(p, holder.slot, holder.page);
      } else if (slot == 45 && holder.page > 0) {
         this.open(p, holder.slot, holder.page - 1);
      } else if (slot == 53) {
         this.open(p, holder.slot, holder.page + 1);
      } else if (slot >= 9 && slot < 9 + PER_PAGE) {
         List<Cosmetic> list = this.bySlot.getOrDefault(holder.slot, List.of());
         int idx = holder.page * PER_PAGE + (slot - 9);
         if (idx >= list.size()) {
            return;
         }

         Cosmetic c = list.get(idx);
         if (this.owns(uuid, c)) {
            this.equip(p, c);
            this.open(p, holder.slot, holder.page);
         } else if (c.kind() == Kind.SHOP) {
            if (c.id().equals(holder.pendingBuy)) {
               this.buy(p, c);
               this.open(p, holder.slot, holder.page);
            } else {
               holder.pendingBuy = c.id();
               this.open(p, holder.slot, holder.page);
               if (p.getOpenInventory().getTopInventory().getHolder() instanceof Holder fresh) {
                  fresh.pendingBuy = c.id();
                  fresh.inv.setItem(slot, this.item(uuid, c, false, fresh));
               }
            }
         } else {
            ChatUtil.sendMessage(p, "&7Verrouillé : &f" + c.hint());
         }
      }
   }

   public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
      if (!(sender instanceof Player player)) {
         sender.sendMessage(ChatUtil.color("&cSeul un joueur peut exécuter cette commande."));
         return true;
      }

      if (args.length > 0 && args[0].equalsIgnoreCase("off")) {
         this.clearAll(player.getUniqueId());
         ChatUtil.sendMessage(player, "&7Cosmétiques retirés.");
         return true;
      }

      this.open(player, Slot.AURA, 0);
      return true;
   }
}
