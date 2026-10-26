package fr.esicraft.market;

import fr.esicraft.EsicraftPlugin;
import fr.esicraft.data.PlayerData;
import fr.esicraft.guild.Guild;
import fr.esicraft.guild.GuildSpecialization;
import fr.esicraft.util.ChatUtil;
import java.util.Collection;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import org.bukkit.Bukkit;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

public class MarketManager {
   private final EsicraftPlugin plugin;
   private final Map<String, MarketListing> listings = new ConcurrentHashMap<>();

   public MarketManager(EsicraftPlugin plugin) {
      this.plugin = plugin;
   }

   public Collection<MarketListing> getListings() {
      return this.listings.values();
   }

   public MarketListing getListing(String id) {
      return this.listings.get(id);
   }

   public void registerListing(MarketListing listing) {
      if (listing != null) {
         this.listings.put(listing.getId(), listing);
      }
   }

   public void clearAll() {
      this.listings.clear();
   }

   public long countListingsOf(UUID seller) {
      return this.listings.values().stream().filter(l -> l.getSellerUuid().equals(seller)).count();
   }

   /** Clic sur une annonce : achat, ou annulation si c'est la sienne (l'objet est rendu au vendeur). */
   public boolean buyOrCancelListing(Player player, String listingId) {
      MarketListing listing = listingId != null ? this.listings.get(listingId) : null;
      if (listing != null && listing.getSellerUuid().equals(player.getUniqueId())) {
         if (player.getInventory().firstEmpty() == -1) {
            ChatUtil.sendMessage(player, "&cVotre inventaire est plein : libérez de la place pour récupérer l'objet.");
            return false;
         } else if (this.listings.remove(listingId, listing)) {
            this.plugin.getDataManager().deleteMarketListing(listingId);
            player.getInventory().addItem(new ItemStack[]{listing.getItemStack()});
            ChatUtil.sendMessage(player, "&aAnnonce annulée : l'objet a été rendu à votre inventaire.");
            ChatUtil.playSound(player, Sound.ENTITY_ITEM_PICKUP, 1.0F, 1.0F);
            return true;
         } else {
            return false;
         }
      } else {
         return this.buyListing(player, listingId);
      }
   }

   public boolean createListing(Player seller, ItemStack item, double price) {
      int maxListings = this.plugin.getConfig().getInt("market.max-listings-per-player", 10);
      if (seller != null && this.countListingsOf(seller.getUniqueId()) >= (long)maxListings) {
         ChatUtil.sendMessage(seller, "&c❌ Vous avez déjà &e" + maxListings + " &cannonces actives : annulez-en une (clic sur votre annonce dans /ah) avant d'en créer une autre.");
         return false;
      } else if (seller != null && item != null && !item.getType().isAir() && !(price <= 0.0) && !Double.isNaN(price) && !Double.isInfinite(price)) {
         String id;
         do {
            id = UUID.randomUUID().toString();
         } while (this.listings.containsKey(id));
         ItemStack itemToSell = item.clone();
         seller.getInventory().setItemInMainHand(null);
         MarketListing listing = new MarketListing(id, seller.getUniqueId(), seller.getName(), itemToSell, price);
         this.listings.put(id, listing);
         this.plugin.getDataManager().saveMarketListing(listing);
         ChatUtil.sendMessage(seller, "&aVotre objet &e" + itemToSell.getType().name() + " &aa été mis en vente pour &6" + price + " pièces &a!");
         ChatUtil.playSound(seller, Sound.ENTITY_VILLAGER_YES, 1.0F, 1.2F);
         return true;
      } else {
         return false;
      }
   }

   public boolean buyListing(Player buyer, String listingId) {
      if (buyer != null && this.plugin.getCombatManager().denySpending(buyer)) {
         return false;
      }

      if (buyer != null && listingId != null) {
         MarketListing listing = this.listings.remove(listingId);
         if (listing == null) {
            ChatUtil.sendMessage(buyer, "&cCet article n'est plus disponible.");
            return false;
         } else if (listing.getSellerUuid().equals(buyer.getUniqueId())) {
            this.listings.put(listingId, listing);
            ChatUtil.sendMessage(buyer, "&cVous ne pouvez pas acheter votre propre article !");
            return false;
         } else {
            PlayerData buyerData = this.plugin.getDataManager().getPlayerData(buyer.getUniqueId());
            if (buyerData == null || buyerData.getCoins() < listing.getPrice()) {
               this.listings.put(listingId, listing);
               ChatUtil.sendMessage(buyer, "&cVous n'avez pas assez d'argent pour acheter cet article (&e" + listing.getPrice() + " pièces &crequises).");
               return false;
            } else if (buyer.getInventory().firstEmpty() == -1) {
               this.listings.put(listingId, listing);
               ChatUtil.sendMessage(buyer, "&cVotre inventaire est plein !");
               return false;
            } else {
               this.plugin.getDataManager().deleteMarketListingSync(listingId);
               buyerData.removeCoins(listing.getPrice());
               buyer.getInventory().addItem(new ItemStack[]{listing.getItemStack()});
               this.plugin.getDataManager().savePlayer(buyerData);
               double baseTaxRate = this.plugin.getConfig().getDouble("market.tax-percentage", 5.0) / 100.0;
               Guild sellerGuild = this.plugin.getGuildManager().getGuildByPlayer(listing.getSellerUuid());
               boolean isMerchantSyndicate = sellerGuild != null && sellerGuild.getSpecialization() == GuildSpecialization.MERCHANT_SYNDICATE;
               double effectiveTaxRate = isMerchantSyndicate || this.plugin.getCourt().is(fr.esicraft.game.CourtManager.Rule.FREE_MARKET) ? 0.0 : baseTaxRate;
               double sellerEarnings = Math.min(listing.getPrice(), listing.getPrice() * (1.0 - effectiveTaxRate));

               this.plugin.getDataManager().creditPlayer(listing.getSellerUuid(), sellerEarnings);
               Player onlineSeller = Bukkit.getPlayer(listing.getSellerUuid());
               if (onlineSeller != null && onlineSeller.isOnline()) {
                  ChatUtil.sendMessage(
                     onlineSeller,
                     "&aVotre article a été acheté par &e" + buyer.getName() + " &apour &6" + String.format("%.2f", sellerEarnings) + " pièces &a!"
                  );
                  ChatUtil.playSound(onlineSeller, Sound.ENTITY_PLAYER_LEVELUP, 1.0F, 1.5F);
               }

               ChatUtil.sendMessage(buyer, "&aAchat réussi pour &6" + listing.getPrice() + " pièces &a!");
               ChatUtil.playSound(buyer, Sound.ENTITY_VILLAGER_CELEBRATE, 1.0F, 1.0F);
               return true;
            }
         }
      } else {
         return false;
      }
   }
}
