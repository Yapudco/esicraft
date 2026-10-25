package fr.esicraft.gui;

import fr.esicraft.EsicraftPlugin;
import fr.esicraft.data.PlayerData;
import fr.esicraft.gui.holder.MarketMainHolder;
import fr.esicraft.market.MarketListing;
import fr.esicraft.util.ChatUtil;
import fr.esicraft.util.ItemBuilder;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;

public class MarketMainGUI {
   public static final String TITLE = "Hôtel des Ventes Central";
   public static final NamespacedKey LISTING_KEY = new NamespacedKey(EsicraftPlugin.getInstance(), "market_listing_id");

   public static void open(Player player, EsicraftPlugin plugin) {
      open(player, plugin, 1);
   }

   public static void open(Player player, EsicraftPlugin plugin, int requestedPage) {
      int[] itemSlots = new int[]{10, 11, 12, 13, 14, 15, 16, 19, 20, 21, 22, 23, 24, 25, 28, 29, 30, 31, 32, 33, 34, 37, 38, 39, 40, 41, 42, 43};
      List<MarketListing> sorted = new ArrayList<>(plugin.getMarketManager().getListings());
      sorted.sort(Comparator.comparingLong(MarketListing::getCreatedAt).thenComparing(MarketListing::getId));
      int totalPages = Math.max(1, (sorted.size() + itemSlots.length - 1) / itemSlots.length);
      int page = Math.min(Math.max(1, requestedPage), totalPages);
      Inventory inv = Bukkit.createInventory(new MarketMainHolder(page), 54, ChatUtil.component("&8» &6Hôtel des Ventes Central &7(" + page + "/" + totalPages + ")"));
      ItemStack filler = new ItemBuilder(Material.GRAY_STAINED_GLASS_PANE).name(" ").build();

      for (int i = 0; i < 9; i++) {
         inv.setItem(i, filler);
      }

      for (int i = 45; i < 54; i++) {
         inv.setItem(i, filler);
      }

      inv.setItem(9, filler);
      inv.setItem(17, filler);
      inv.setItem(18, filler);
      inv.setItem(26, filler);
      inv.setItem(27, filler);
      inv.setItem(35, filler);
      inv.setItem(36, filler);
      inv.setItem(44, filler);
      PlayerData data = plugin.getDataManager().getPlayerData(player.getUniqueId());
      double balance = data != null ? data.getCoins() : 0.0;
      inv.setItem(
         4,
         new ItemBuilder(Material.GOLD_INGOT)
            .name("&6&lVotre Solde : &e" + String.format("%.2f", balance) + " pièces")
            .lore("&7Pour mettre en vente un objet :", "&e/ah sell <prix> &7avec l'objet en main.")
            .glow()
            .build()
      );
      inv.setItem(
         49,
         new ItemBuilder(Material.WITHER_SKELETON_SKULL)
            .name("&4&l⚔️ Contrats de Hordes & Mercenaires")
            .lore("&7Achetez une armée de monstres", "&7pour assaillir le Nexus d'une guilde rivale !", "", "&e➤ Cliquez pour ouvrir les contrats d'invasion !")
            .glow()
            .build()
      );
      if (page > 1) {
         inv.setItem(45, new ItemBuilder(Material.ARROW).name("&e◀ Page précédente").build());
      }

      if (page < totalPages) {
         inv.setItem(53, new ItemBuilder(Material.ARROW).name("&ePage suivante ▶").build());
      }

      int index = 0;
      int startIndex = (page - 1) * itemSlots.length;

      for (int li = startIndex; li < sorted.size() && index < itemSlots.length; li++) {
         MarketListing listing = sorted.get(li);

         ItemStack displayItem = listing.getItemStack().clone();
         ItemBuilder b = new ItemBuilder(displayItem);
         b.addLoreLine("");
         b.addLoreLine("&7Vendeur : &e" + listing.getSellerName());
         b.addLoreLine("&7Prix : &6" + String.format("%.2f", listing.getPrice()) + " pièces");
         if (listing.getSellerUuid().equals(player.getUniqueId())) {
            b.addLoreLine("&c➤ Votre annonce : cliquez pour l'ANNULER et récupérer l'objet.");
         } else {
            b.addLoreLine("&e➤ Cliquez pour ACHETER cet objet !");
         }
         ItemStack finalItem = b.build();
         ItemMeta meta = finalItem.getItemMeta();
         if (meta != null) {
            meta.getPersistentDataContainer().set(LISTING_KEY, PersistentDataType.STRING, listing.getId());
            finalItem.setItemMeta(meta);
         }

         inv.setItem(itemSlots[index], finalItem);
         index++;
      }

      player.openInventory(inv);
      ChatUtil.playSound(player, Sound.BLOCK_CHEST_OPEN, 1.0F, 1.2F);
   }
}
