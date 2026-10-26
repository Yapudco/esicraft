package fr.esicraft.life;

import fr.esicraft.EsicraftPlugin;
import fr.esicraft.util.ChatUtil;
import fr.esicraft.util.ItemBuilder;
import java.time.Duration;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import net.kyori.adventure.title.Title;
import org.bukkit.Bukkit;
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
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;

/**
 * Cosmétiques : <b>chaque succès débloque un aura (particules autour de soi) et un titre (affiché dans le tchat)</b>, équipables
 * depuis {@code /cosmetique}. Un aura et un titre au plus sont équipés à la fois. Un aura secret s'ajoute à ceux des succès
 * (easter egg : la première fois qu'on donne un diamant à un autre joueur).
 */
public class CosmeticManager implements Listener, CommandExecutor {
   private static final String KEY_DIAMOND = "cosmetic:diamond:";
   private static final String KEY_AURA = "cos_aura:";
   private static final String KEY_TITLE = "cos_title:";
   private static final String DIAMOND = "DIAMOND";

   /** Particule de chaque aura, dans l'ordre des succès (clés Minecraft ; repli : étincelles). */
   private static final String[] PARTICLE_KEYS = {"happy_villager", "heart", "note", "crit", "flame", "soul_fire_flame", "totem_of_undying", "firework", "enchant", "snowflake",
      "cherry_leaves", "electric_spark", "glow", "sculk_soul", "witch", "soul", "ash", "wax_on", "composter", "scrape", "small_flame", "bubble_pop", "campfire_cosy_smoke", "cloud",
      "spore_blossom_air", "dripping_obsidian_tear"};
   private static final Material[] AURA_ICONS = {Material.EMERALD, Material.PINK_DYE, Material.NOTE_BLOCK, Material.IRON_SWORD, Material.BLAZE_POWDER, Material.SOUL_LANTERN,
      Material.TOTEM_OF_UNDYING, Material.FIREWORK_STAR, Material.ENCHANTED_BOOK, Material.SNOWBALL, Material.CHERRY_LEAVES, Material.LIGHTNING_ROD, Material.GLOW_INK_SAC,
      Material.ECHO_SHARD, Material.FERMENTED_SPIDER_EYE, Material.SOUL_SAND, Material.GUNPOWDER, Material.HONEYCOMB, Material.COMPOSTER, Material.COPPER_INGOT,
      Material.TORCH, Material.PRISMARINE_SHARD, Material.CAMPFIRE, Material.WHITE_WOOL, Material.SPORE_BLOSSOM, Material.CRYING_OBSIDIAN};

   private final EsicraftPlugin plugin;
   private final Set<UUID> diamond = ConcurrentHashMap.newKeySet();
   private final Map<UUID, String> aura = new ConcurrentHashMap<>();
   private final Map<UUID, String> title = new ConcurrentHashMap<>();
   private final Map<String, Particle> particles = new LinkedHashMap<>();
   private final Achievement[] achievements = Achievement.values();

   private static final class Holder implements InventoryHolder {
      Inventory inv;

      public @NotNull Inventory getInventory() {
         return this.inv;
      }
   }

   public CosmeticManager(EsicraftPlugin plugin) {
      this.plugin = plugin;
   }

   public void init() {
      for (int i = 0; i < this.achievements.length; i++) {
         this.particles.put(this.auraId(this.achievements[i]), this.particle(PARTICLE_KEYS[i % PARTICLE_KEYS.length]));
      }

      this.particles.put("aura:" + DIAMOND, Particle.END_ROD);
      var db = this.plugin.getDataManager().getStorage();
      for (Object[] row : db.select("SELECT k, v FROM kv WHERE k LIKE 'cosmetic:diamond:%' OR k LIKE 'cos_aura:%' OR k LIKE 'cos_title:%';", 2)) {
         String k = (String)row[0];
         try {
            if (k.startsWith(KEY_DIAMOND)) {
               this.diamond.add(UUID.fromString(k.substring(KEY_DIAMOND.length())));
            } else if (k.startsWith(KEY_AURA)) {
               this.aura.put(UUID.fromString(k.substring(KEY_AURA.length())), (String)row[1]);
            } else if (k.startsWith(KEY_TITLE)) {
               this.title.put(UUID.fromString(k.substring(KEY_TITLE.length())), (String)row[1]);
            }
         } catch (IllegalArgumentException ignored) {
         }
      }

      Bukkit.getScheduler().runTaskTimer(this.plugin, this::tickAuras, 40L, 10L);
   }

   private Particle particle(String key) {
      Particle p = Registry.PARTICLE_TYPE.get(NamespacedKey.minecraft(key));
      return p != null ? p : Particle.END_ROD;
   }

   private String auraId(Achievement a) {
      return "aura:" + a.name();
   }

   private String titleId(Achievement a) {
      return "title:" + a.name();
   }

   // ------------------------------------------------------------------ état

   private boolean owns(UUID uuid, String id) {
      if (id.equals("aura:" + DIAMOND)) {
         return this.diamond.contains(uuid);
      }

      Achievement a = Achievement.parse(id.substring(id.indexOf(':') + 1));
      return a != null && this.plugin.getAchievements().has(uuid, a);
   }

   /** Titre équipé, prêt pour le tchat (« &e⚔ Chef de horde »), ou null. */
   public String titleOf(UUID uuid) {
      String id = this.title.get(uuid);
      if (id == null || !this.owns(uuid, id)) {
         return null;
      }

      Achievement a = Achievement.parse(id.substring(id.indexOf(':') + 1));
      return a == null ? null : "&e" + a.getIcon() + " " + a.getTitle();
   }

   private void tickAuras() {
      for (Map.Entry<UUID, String> e : this.aura.entrySet()) {
         Player p = Bukkit.getPlayer(e.getKey());
         Particle particle = this.particles.get(e.getValue());
         if (p != null && particle != null && !p.isDead() && p.getGameMode() != org.bukkit.GameMode.SPECTATOR && this.plugin.getDisguiseManager().get(e.getKey()) == null
            && this.owns(e.getKey(), e.getValue())) {
            p.getWorld().spawnParticle(particle, p.getLocation().add(0.0, 1.0, 0.0), 2, 0.45, 0.7, 0.45, 0.01);
         }
      }
   }

   private void save(String prefix, UUID uuid, String value) {
      String key = prefix + uuid;
      Bukkit.getScheduler().runTaskAsynchronously(this.plugin, () -> {
         if (value == null) {
            this.plugin.getDataManager().getStorage().exec("DELETE FROM kv WHERE k = ?;", key);
         } else {
            this.plugin.getDataManager().getStorage().exec("INSERT INTO kv (k, v) VALUES (?, ?) ON CONFLICT(k) DO UPDATE SET v = excluded.v;", key, value);
         }
      });
   }

   private void equip(Player p, String id) {
      UUID uuid = p.getUniqueId();
      boolean isAura = id.startsWith("aura:");
      Map<UUID, String> slot = isAura ? this.aura : this.title;
      if (id.equals(slot.get(uuid))) {
         slot.remove(uuid);
         this.save(isAura ? KEY_AURA : KEY_TITLE, uuid, null);
         ChatUtil.sendMessage(p, "&7Cosmétique retiré.");
      } else {
         slot.put(uuid, id);
         this.save(isAura ? KEY_AURA : KEY_TITLE, uuid, id);
         ChatUtil.sendMessage(p, "&a✔ Cosmétique équipé.");
         ChatUtil.playSound(p, Sound.ENTITY_PLAYER_LEVELUP, 0.6F, 1.8F);
      }
   }

   /** Annonce les cosmétiques gagnés avec un succès. */
   public void onAchievement(Player player, Achievement a) {
      ChatUtil.sendMessage(player, "&b🎁 Cosmétiques débloqués : aura &e" + a.getTitle() + " &bet titre &e" + a.getIcon() + " " + a.getTitle() + "&b. &7/cosmetique pour les équiper.");
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

      this.save(KEY_DIAMOND, thrower, "1");
      if (!this.aura.containsKey(thrower)) {
         this.aura.put(thrower, "aura:" + DIAMOND);
         this.save(KEY_AURA, thrower, "aura:" + DIAMOND);
      }

      giver.showTitle(Title.title(ChatUtil.component("&b&l💎 Diamonds to you !"), ChatUtil.component("&7Cosmétique débloqué : &bAura de diamant &7(/cosmetique)"),
         Title.Times.times(Duration.ofMillis(300L), Duration.ofMillis(3000L), Duration.ofMillis(800L))));
      ChatUtil.sendMessage(giver, "&b💎 &lDiamonds to you ! &7Votre générosité est récompensée : vous débloquez l'&bAura de diamant&7 (équipée). &e/cosmetique &7pour la changer.");
      ChatUtil.playSound(giver, Sound.ENTITY_PLAYER_LEVELUP, 1.0F, 2.0F);
      ChatUtil.sendMessage(receiver, "&b💎 &e" + giver.getName() + " &7vous offre un diamant !");
      giver.getWorld().spawnParticle(Particle.END_ROD, giver.getLocation().add(0.0, 1.0, 0.0), 40, 0.5, 0.8, 0.5, 0.05);
   }

   // ------------------------------------------------------------------ menu

   private int slotOfAura(int i) {
      return i;
   }

   private void open(Player p) {
      Holder holder = new Holder();
      UUID uuid = p.getUniqueId();
      int ownedCount = this.ownedTitles(uuid).size();
      Inventory inv = Bukkit.createInventory(holder, 54, ChatUtil.component("&8» &b✨ Cosmétiques &7(" + ownedCount + "/" + this.achievements.length + ")"));
      holder.inv = inv;
      String equippedAura = this.aura.get(uuid);
      String equippedTitle = this.title.get(uuid);
      for (int i = 0; i < this.achievements.length; i++) {
         Achievement a = this.achievements[i];
         String id = this.auraId(a);
         boolean owned = this.plugin.getAchievements().has(uuid, a);
         inv.setItem(this.slotOfAura(i), this.item(owned, AURA_ICONS[i % AURA_ICONS.length], "&b✨ Aura : " + a.getTitle(), a, id.equals(equippedAura)));
         inv.setItem(27 + i, this.item(this.plugin.getAchievements().has(uuid, a), Material.NAME_TAG, "&e" + a.getIcon() + " Titre : " + a.getTitle(), a, this.titleId(a).equals(equippedTitle)));
      }

      boolean diamondOwned = this.diamond.contains(uuid);
      inv.setItem(26, new ItemBuilder(diamondOwned ? Material.DIAMOND : Material.GRAY_DYE).name(diamondOwned ? "&b✨ Aura : Diamant" : "&7??? Aura secrète")
         .lore(diamondOwned ? List.of("&7Offerte pour votre générosité.", "", "&e➤ Clic pour équiper") : List.of("&7Un secret à découvrir…")).glow(("aura:" + DIAMOND).equals(equippedAura)).build());
      inv.setItem(53, new ItemBuilder(Material.BARRIER).name("&cTout retirer").lore("&7Retire l'aura et le titre équipés.", "", "&8Chaque succès débloque un aura (haut)", "&8et un titre (bas).").build());
      p.openInventory(inv);
   }

   private ItemStack item(boolean owned, Material icon, String name, Achievement a, boolean equipped) {
      if (!owned) {
         return new ItemBuilder(Material.GRAY_DYE).name("&7??? " + ChatUtil.color(name).replaceAll("§.", "")).lore("&8Verrouillé", "&7Succès : &f" + a.getTitle(), "&7" + a.getDescription()).build();
      }

      return new ItemBuilder(icon).name(name).lore("&7Succès : &f" + a.getTitle(), "", equipped ? "&a✔ Équipé &7(clic pour retirer)" : "&e➤ Clic pour équiper").glow(equipped).build();
   }

   @EventHandler
   public void onClick(InventoryClickEvent event) {
      if (!(event.getInventory().getHolder() instanceof Holder) || !(event.getWhoClicked() instanceof Player p)) {
         return;
      }

      event.setCancelled(true);
      if (event.getClickedInventory() != event.getInventory()) {
         return;
      }

      int slot = event.getSlot();
      UUID uuid = p.getUniqueId();
      if (slot == 53) {
         this.aura.remove(uuid);
         this.title.remove(uuid);
         this.save(KEY_AURA, uuid, null);
         this.save(KEY_TITLE, uuid, null);
         ChatUtil.sendMessage(p, "&7Cosmétiques retirés.");
      } else if (slot == 26) {
         if (this.diamond.contains(uuid)) {
            this.equip(p, "aura:" + DIAMOND);
         }
      } else if (slot < this.achievements.length) {
         if (this.plugin.getAchievements().has(uuid, this.achievements[slot])) {
            this.equip(p, this.auraId(this.achievements[slot]));
         }
      } else if (slot >= 27 && slot - 27 < this.achievements.length) {
         if (this.plugin.getAchievements().has(uuid, this.achievements[slot - 27])) {
            this.equip(p, this.titleId(this.achievements[slot - 27]));
         }
      } else {
         return;
      }

      this.open(p);
   }

   public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
      if (!(sender instanceof Player player)) {
         sender.sendMessage(ChatUtil.color("&cSeul un joueur peut exécuter cette commande."));
         return true;
      }

      if (args.length > 0 && args[0].equalsIgnoreCase("off")) {
         this.aura.remove(player.getUniqueId());
         this.title.remove(player.getUniqueId());
         this.save(KEY_AURA, player.getUniqueId(), null);
         this.save(KEY_TITLE, player.getUniqueId(), null);
         ChatUtil.sendMessage(player, "&7Cosmétiques retirés.");
         return true;
      }

      this.open(player);
      return true;
   }

   public List<String> ownedTitles(UUID uuid) {
      List<String> list = new ArrayList<>();
      for (Achievement a : this.achievements) {
         if (this.plugin.getAchievements().has(uuid, a)) {
            list.add(a.getTitle());
         }
      }

      return list;
   }
}
