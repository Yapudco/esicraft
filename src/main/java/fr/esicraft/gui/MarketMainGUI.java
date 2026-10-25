package fr.esicraft.gui;

import fr.esicraft.EsicraftPlugin;
import fr.esicraft.data.PlayerData;
import fr.esicraft.gui.holder.MarketMainHolder;
import fr.esicraft.market.MarketListing;
import fr.esicraft.util.ChatUtil;
import fr.esicraft.util.ItemBuilder;
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
      Inventory inv = Bukkit.createInventory(new MarketMainHolder(), 54, ChatUtil.component("&8» &6Hôtel des Ventes Central"));
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
      int[] itemSlots = new int[]{10, 11, 12, 13, 14, 15, 16, 19, 20, 21, 22, 23, 24, 25, 28, 29, 30, 31, 32, 33, 34, 37, 38, 39, 40, 41, 42, 43};
      int index = 0;

      for (MarketListing listing : plugin.getMarketManager().getListings()) {
         if (index >= itemSlots.length) {
            break;
         }

         ItemStack displayItem = listing.getItemStack().clone();
         ItemBuilder b = new ItemBuilder(displayItem);
         b.addLoreLine("");
         b.addLoreLine("&7Vendeur : &e" + listing.getSellerName());
         b.addLoreLine("&7Prix : &6" + String.format("%.2f", listing.getPrice()) + " pièces");
         b.addLoreLine("&e➤ Cliquez pour ACHETER cet objet !");
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
