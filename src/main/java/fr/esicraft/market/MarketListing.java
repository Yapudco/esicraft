package fr.esicraft.market;

import java.util.UUID;
import org.bukkit.inventory.ItemStack;

public class MarketListing {
   private final String id;
   private final UUID sellerUuid;
   private final String sellerName;
   private final ItemStack itemStack;
   private final double price;
   private final long createdAt;

   public MarketListing(String id, UUID sellerUuid, String sellerName, ItemStack itemStack, double price) {
      this(id, sellerUuid, sellerName, itemStack, price, System.currentTimeMillis());
   }

   public MarketListing(String id, UUID sellerUuid, String sellerName, ItemStack itemStack, double price, long createdAt) {
      this.id = id;
      this.sellerUuid = sellerUuid;
      this.sellerName = sellerName;
      this.itemStack = itemStack;
      this.price = price;
      this.createdAt = createdAt;
   }

   public String getId() {
      return this.id;
   }

   public UUID getSellerUuid() {
      return this.sellerUuid;
   }

   public String getSellerName() {
      return this.sellerName;
   }

   public ItemStack getItemStack() {
      return this.itemStack;
   }

   public double getPrice() {
      return this.price;
   }

   public long getCreatedAt() {
      return this.createdAt;
   }
}
