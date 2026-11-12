package fr.esicraft.market;

import fr.esicraft.EsicraftPlugin;
import fr.esicraft.data.PlayerData;
import fr.esicraft.gui.holder.BaseGUIHolder;
import fr.esicraft.guild.Guild;
import fr.esicraft.guild.GuildSpecialization;
import fr.esicraft.util.ChatUtil;
import fr.esicraft.util.ItemBuilder;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;

/**
 * Commandes d'achat de l'Hôtel des Ventes : « j'achète 64 diamants à 30 pièces l'unité ». L'acheteur bloque la somme à la création ;
 * n'importe quel joueur peut livrer tout ou partie de la commande et il est payé aussitôt (taxe du marché déduite). Les objets livrés attendent
 * l'acheteur, qui les retire quand il veut. Annuler rend l'argent de la partie non livrée.
 * <p>
 * Seuls les objets « nus » sont acceptés (sans nom, enchantement ni usure) : une commande de diamants ne se remplit pas avec n'importe quoi.
 */
public class BuyOrderManager implements Listener {
   public static final class BuyOrder {
      final String id;
      final UUID buyer;
      final String buyerName;
      final Material material;
      final int total;
      int remaining;
      int pending;
      final double unitPrice;
      final long createdAt;

      BuyOrder(String id, UUID buyer, String buyerName, Material material, int total, int remaining, int pending, double unitPrice, long createdAt) {
         this.id = id;
         this.buyer = buyer;
         this.buyerName = buyerName;
         this.material = material;
         this.total = total;
         this.remaining = remaining;
         this.pending = pending;
         this.unitPrice = unitPrice;
         this.createdAt = createdAt;
      }

      public Material material() {
         return this.material;
      }

      public int remaining() {
         return this.remaining;
      }

      public double unitPrice() {
         return this.unitPrice;
      }

      public String buyerName() {
         return this.buyerName;
      }
   }

   static final class OrdersHolder extends BaseGUIHolder {
      final int page;
      final List<String> slots = new ArrayList<>();

      OrdersHolder(int page) {
         this.page = page;
      }
   }

   private static final int MAX_QTY = 2304;
   private final EsicraftPlugin plugin;
   private final Map<String, BuyOrder> orders = new ConcurrentHashMap<>();

   public BuyOrderManager(EsicraftPlugin plugin) {
      this.plugin = plugin;
   }

   public void init() {
      var db = this.plugin.getDataManager().getStorage();
      db.exec("CREATE TABLE IF NOT EXISTS market_orders (id VARCHAR(64) PRIMARY KEY, buyer VARCHAR(36), buyer_name VARCHAR(32), material VARCHAR(64), total INT, remaining INT, pending INT, unit_price REAL, created_at BIGINT);");
      for (Object[] r : db.select("SELECT id, buyer, buyer_name, material, total, remaining, pending, unit_price, created_at FROM market_orders;", 9)) {
         try {
            Material m = Material.valueOf(String.valueOf(r[3]));
            this.orders.put(String.valueOf(r[0]), new BuyOrder(String.valueOf(r[0]), UUID.fromString(String.valueOf(r[1])), String.valueOf(r[2]), m, ((Number)r[4]).intValue(),
               ((Number)r[5]).intValue(), ((Number)r[6]).intValue(), ((Number)r[7]).doubleValue(), ((Number)r[8]).longValue()));
         } catch (RuntimeException ignored) {
         }
      }
   }

   public void clear() {
      this.orders.clear();
   }

   public List<BuyOrder> open() {
      List<BuyOrder> out = new ArrayList<>();
      for (BuyOrder o : this.orders.values()) {
         if (o.remaining > 0) {
            out.add(o);
         }
      }

      out.sort(Comparator.comparingDouble((BuyOrder o) -> -o.unitPrice).thenComparingLong(o -> o.createdAt));
      return out;
   }

   private void save(BuyOrder o) {
      var db = this.plugin.getDataManager().getStorage();
      if (o.remaining <= 0 && o.pending <= 0) {
         this.orders.remove(o.id);
         db.exec("DELETE FROM market_orders WHERE id = ?;", o.id);
      } else {
         db.exec("INSERT OR REPLACE INTO market_orders (id, buyer, buyer_name, material, total, remaining, pending, unit_price, created_at) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?);",
            o.id, o.buyer.toString(), o.buyerName, o.material.name(), o.total, o.remaining, o.pending, o.unitPrice, o.createdAt);
      }
   }

   private static String name(Material m) {
      return m.name().toLowerCase(Locale.ROOT).replace('_', ' ');
   }

   private static String money(double v) {
      return v == Math.floor(v) ? String.valueOf((long)v) : String.format(Locale.ROOT, "%.2f", v);
   }

   // ------------------------------------------------------------------ création

   /** {@code /ah commande [matériau] <quantité> <prix unitaire>} : le matériau par défaut est celui de l'objet tenu en main. */
   public void create(Player player, String[] args) {
      Material mat;
      String qtyArg;
      String priceArg;
      if (args.length >= 4) {
         mat = Material.matchMaterial(args[1]);
         qtyArg = args[2];
         priceArg = args[3];
      } else if (args.length == 3) {
         mat = player.getInventory().getItemInMainHand().getType();
         qtyArg = args[1];
         priceArg = args[2];
      } else {
         ChatUtil.sendMessage(player, "&cUtilisation : &e/ah commande <quantité> <prix unitaire> &7(objet en main) &cou &e/ah commande <matériau> <quantité> <prix unitaire>");
         return;
      }

      if (mat == null || mat.isAir() || !mat.isItem()) {
         ChatUtil.sendMessage(player, "&c❌ Matériau inconnu : tenez l'objet voulu en main, ou donnez son nom anglais (ex. &ediamond&c).");
         return;
      }

      int qty;
      double price;
      try {
         qty = Integer.parseInt(qtyArg);
         price = Double.parseDouble(priceArg.replace(',', '.'));
      } catch (NumberFormatException e) {
         ChatUtil.sendMessage(player, "&c❌ Quantité ou prix invalide.");
         return;
      }

      if (qty < 1 || qty > MAX_QTY) {
         ChatUtil.sendMessage(player, "&c❌ La quantité doit être comprise entre 1 et " + MAX_QTY + ".");
         return;
      }

      price = Math.floor(price * 100.0) / 100.0;
      if (Double.isNaN(price) || price < 0.01 || price > 100000.0) {
         ChatUtil.sendMessage(player, "&c❌ Le prix unitaire doit être compris entre 0,01 et 100 000 pièces.");
         return;
      }

      int max = Math.max(1, this.plugin.getConfig().getInt("market.max-buy-orders-per-player", 5));
      long mine = this.orders.values().stream().filter(o -> o.buyer.equals(player.getUniqueId()) && o.remaining > 0).count();
      if (mine >= max) {
         ChatUtil.sendMessage(player, "&c❌ Vous avez déjà &e" + max + " &ccommandes d'achat ouvertes (&e/ah commandes&c pour en annuler).");
         return;
      }

      if (this.plugin.getCombatManager().denySpending(player)) {
         return;
      }

      double escrow = Math.round(qty * price * 100.0) / 100.0;
      PlayerData data = this.plugin.getDataManager().getPlayerData(player.getUniqueId());
      if (data == null || !data.removeCoins(escrow)) {
         ChatUtil.sendMessage(player, "&c❌ Il faut bloquer &6" + money(escrow) + " pièces &cpour cette commande (quantité × prix).");
         return;
      }

      this.plugin.getDataManager().savePlayer(data);
      String id = UUID.randomUUID().toString();
      BuyOrder o = new BuyOrder(id, player.getUniqueId(), player.getName(), mat, qty, qty, 0, price, System.currentTimeMillis());
      this.orders.put(id, o);
      this.save(o);
      ChatUtil.sendMessage(player, "&a✔ Commande d'achat publiée : &e" + qty + " × " + name(mat) + " &aà &6" + money(price) + " pièces &al'unité (&6" + money(escrow) + " &abloquées).");
      ChatUtil.playSound(player, Sound.ENTITY_VILLAGER_YES, 1.0F, 1.2F);
      ChatUtil.broadcast("&6🛒 &e" + player.getName() + " &7achète &e" + qty + " × " + name(mat) + " &7à &6" + money(price) + " pièces &7l'unité — livrez avec &e/ah commandes&7.");
      this.plugin.getTutorial().complete(player, fr.esicraft.life.TutorialManager.Step.MARKET);
   }

   // ------------------------------------------------------------------ livraison, retrait, annulation

   private void deliver(Player seller, BuyOrder o) {
      if (o.buyer.equals(seller.getUniqueId())) {
         return;
      }

      if (o.remaining <= 0) {
         ChatUtil.sendMessage(seller, "&cCette commande est déjà remplie.");
         return;
      }

      if (this.plugin.getCombatManager().denyItems(seller)) {
         return;
      }

      ItemStack plain = new ItemStack(o.material);
      ItemStack[] contents = seller.getInventory().getStorageContents();
      int have = 0;
      for (ItemStack it : contents) {
         if (it != null && it.isSimilar(plain)) {
            have += it.getAmount();
         }
      }

      int qty = Math.min(have, o.remaining);
      if (qty <= 0) {
         ChatUtil.sendMessage(seller, "&c❌ Vous n'avez aucun &e" + name(o.material) + " &c« nu » (sans nom, enchantement ni usure) dans votre inventaire.");
         return;
      }

      int left = qty;
      for (int i = 0; i < contents.length && left > 0; i++) {
         ItemStack it = contents[i];
         if (it != null && it.isSimilar(plain)) {
            int take = Math.min(left, it.getAmount());
            it.setAmount(it.getAmount() - take);
            contents[i] = it.getAmount() <= 0 ? null : it;
            left -= take;
         }
      }

      seller.getInventory().setStorageContents(contents);
      o.remaining -= qty;
      o.pending += qty;
      this.save(o);
      double gross = qty * o.unitPrice;
      double taxRate = this.plugin.getConfig().getDouble("market.tax-percentage", 5.0) / 100.0;
      Guild sellerGuild = this.plugin.getGuildManager().getGuildByPlayer(seller.getUniqueId());
      if (sellerGuild != null && sellerGuild.getSpecialization() == GuildSpecialization.MERCHANT_SYNDICATE || this.plugin.getCourt().is(fr.esicraft.game.CourtManager.Rule.FREE_MARKET)) {
         taxRate = 0.0;
      }

      double net = Math.floor(gross * (1.0 - taxRate) * 100.0) / 100.0;
      this.plugin.getDataManager().creditPlayer(seller.getUniqueId(), net);
      ChatUtil.sendMessage(seller, "&a✔ Vous livrez &e" + qty + " × " + name(o.material) + " &aà " + o.buyerName + " : &6+" + money(net) + " pièces&a.");
      ChatUtil.playSound(seller, Sound.ENTITY_EXPERIENCE_ORB_PICKUP, 1.0F, 1.0F);
      Player buyer = Bukkit.getPlayer(o.buyer);
      if (buyer != null) {
         ChatUtil.sendMessage(buyer, "&6🛒 &e" + seller.getName() + " &7vous a livré &e" + qty + " × " + name(o.material) + "&7 : retirez-les dans &e/ah commandes&7.");
      }

      this.plugin.getTutorial().complete(seller, fr.esicraft.life.TutorialManager.Step.MARKET);
   }

   private void collect(Player buyer, BuyOrder o) {
      if (o.pending <= 0) {
         return;
      }

      List<ItemStack> stacks = new ArrayList<>();
      int left = o.pending;
      int max = o.material.getMaxStackSize();
      while (left > 0) {
         int n = Math.min(max, left);
         stacks.add(new ItemStack(o.material, n));
         left -= n;
      }

      int back = 0;
      for (ItemStack rest : buyer.getInventory().addItem(stacks.toArray(new ItemStack[0])).values()) {
         back += rest.getAmount();
      }

      int got = o.pending - back;
      o.pending = back;
      this.save(o);
      if (got > 0) {
         ChatUtil.sendMessage(buyer, "&a✔ Vous retirez &e" + got + " × " + name(o.material) + "&a." + (back > 0 ? " &7(" + back + " attendent encore : inventaire plein)" : ""));
         ChatUtil.playSound(buyer, Sound.ENTITY_ITEM_PICKUP, 1.0F, 1.0F);
      } else {
         ChatUtil.sendMessage(buyer, "&c❌ Inventaire plein.");
      }
   }

   private void cancel(Player buyer, BuyOrder o) {
      if (o.remaining <= 0) {
         return;
      }

      double refund = Math.round(o.remaining * o.unitPrice * 100.0) / 100.0;
      o.remaining = 0;
      this.save(o);
      this.plugin.getDataManager().creditPlayer(buyer.getUniqueId(), refund);
      ChatUtil.sendMessage(buyer, "&7Commande annulée : &6" + money(refund) + " pièces &7vous sont rendues." + (o.pending > 0 ? " Pensez à retirer les objets déjà livrés." : ""));
   }

   // ------------------------------------------------------------------ interface

   public void openGui(Player player, int requestedPage) {
      List<BuyOrder> list = new ArrayList<>(this.open());
      // Les commandes du joueur (y compris annulées mais avec des objets à retirer) en tête.
      List<BuyOrder> mine = new ArrayList<>();
      for (BuyOrder o : this.orders.values()) {
         if (o.buyer.equals(player.getUniqueId())) {
            mine.add(o);
         }
      }

      list.removeAll(mine);
      mine.sort(Comparator.comparingLong(o -> o.createdAt));
      mine.addAll(list);
      int per = 45;
      int pages = Math.max(1, (mine.size() + per - 1) / per);
      int page = Math.max(1, Math.min(requestedPage, pages));
      OrdersHolder holder = new OrdersHolder(page);
      Inventory inv = Bukkit.createInventory(holder, 54, ChatUtil.component("&8» &6Commandes d'achat &7(" + page + "/" + pages + ")"));
      for (int i = (page - 1) * per, slot = 0; i < mine.size() && slot < per; i++, slot++) {
         BuyOrder o = mine.get(i);
         holder.slots.add(o.id);
         boolean own = o.buyer.equals(player.getUniqueId());
         ItemBuilder b = new ItemBuilder(o.material, Math.max(1, Math.min(64, Math.max(o.remaining, o.pending))))
            .name((own ? "&a" : "&e") + name(o.material))
            .addLoreLine("&7Acheteur : &f" + o.buyerName)
            .addLoreLine("&7Prix unitaire : &6" + money(o.unitPrice) + " pièces")
            .addLoreLine("&7Reste à livrer : &e" + o.remaining + " &7/ " + o.total);
         if (own) {
            b.addLoreLine("&7À retirer : &a" + o.pending);
            b.addLoreLine("");
            b.addLoreLine("&a➤ Clic gauche : retirer les objets livrés");
            if (o.remaining > 0) {
               b.addLoreLine("&c➤ Clic droit : annuler (remboursement de la partie non livrée)");
            }
         } else {
            b.addLoreLine("");
            b.addLoreLine("&e➤ Cliquez pour LIVRER ce que vous avez (payé " + money(o.unitPrice) + " l'unité, taxe déduite)");
         }

         inv.setItem(slot, b.build());
      }

      ItemStack glass = new ItemBuilder(Material.GRAY_STAINED_GLASS_PANE).name(" ").build();
      for (int i = 45; i < 54; i++) {
         inv.setItem(i, glass);
      }

      if (page > 1) {
         inv.setItem(45, new ItemBuilder(Material.ARROW).name("&e◀ Page précédente").build());
      }

      if (page < pages) {
         inv.setItem(53, new ItemBuilder(Material.ARROW).name("&ePage suivante ▶").build());
      }

      inv.setItem(48, new ItemBuilder(Material.CHEST).name("&6◀ Retour à l'Hôtel des Ventes").build());
      inv.setItem(50, new ItemBuilder(Material.WRITABLE_BOOK).name("&e&lPasser une commande").lore("&7Tenez l'objet voulu en main, puis :", "&e/ah commande <quantité> <prix unitaire>",
         "&7ou : &e/ah commande <matériau> <quantité> <prix>", "&7La somme est bloquée jusqu'à livraison ou annulation.").build());
      player.openInventory(inv);
   }

   @EventHandler(priority = EventPriority.HIGHEST)
   public void onClick(InventoryClickEvent event) {
      if (!(event.getInventory().getHolder() instanceof OrdersHolder holder) || !(event.getWhoClicked() instanceof Player player)) {
         return;
      }

      event.setCancelled(true);
      if (event.getClickedInventory() != event.getView().getTopInventory()) {
         return;
      }

      int slot = event.getRawSlot();
      if (slot == 45 && holder.page > 1) {
         this.openGui(player, holder.page - 1);
         return;
      }

      if (slot == 53) {
         this.openGui(player, holder.page + 1);
         return;
      }

      if (slot == 48) {
         fr.esicraft.gui.MarketMainGUI.open(player, this.plugin);
         return;
      }

      if (slot < 0 || slot >= holder.slots.size()) {
         return;
      }

      BuyOrder o = this.orders.get(holder.slots.get(slot));
      if (o == null) {
         ChatUtil.sendMessage(player, "&cCette commande n'existe plus.");
      } else if (o.buyer.equals(player.getUniqueId())) {
         if (event.isRightClick()) {
            this.cancel(player, o);
         } else {
            this.collect(player, o);
         }
      } else {
         this.deliver(player, o);
      }

      this.openGui(player, holder.page);
   }

   @EventHandler(priority = EventPriority.HIGHEST)
   public void onDrag(InventoryDragEvent event) {
      if (event.getInventory().getHolder() instanceof OrdersHolder) {
         event.setCancelled(true);
      }
   }
}
