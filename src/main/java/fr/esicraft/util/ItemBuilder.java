package fr.esicraft.util;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import net.kyori.adventure.text.Component;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.OfflinePlayer;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.SkullMeta;
import org.bukkit.persistence.PersistentDataType;

public class ItemBuilder {
   private final ItemStack item;
   private final ItemMeta meta;

   public ItemBuilder(Material material) {
      this(material, 1);
   }

   public ItemBuilder(Material material, int amount) {
      this.item = new ItemStack(material, Math.max(1, amount));
      this.meta = this.item.getItemMeta();
   }

   public ItemBuilder(ItemStack itemStack) {
      this.item = itemStack.clone();
      this.meta = this.item.getItemMeta();
   }

   public ItemBuilder amount(int amount) {
      this.item.setAmount(Math.max(1, amount));
      return this;
   }

   public ItemBuilder name(String name) {
      if (this.meta != null && name != null) {
         this.meta.displayName(ChatUtil.component(name));
      }

      return this;
   }

   public ItemBuilder lore(String... lines) {
      return this.lore(Arrays.asList(lines));
   }

   public ItemBuilder lore(List<String> lines) {
      if (this.meta != null && lines != null) {
         List<Component> componentLore = new ArrayList<>();

         for (String line : lines) {
            componentLore.add(ChatUtil.component(line));
         }

         this.meta.lore(componentLore);
      }

      return this;
   }

   public ItemBuilder addLoreLine(String line) {
      if (this.meta != null && line != null) {
         List<Component> currentLore = this.meta.lore();
         ArrayList var3;
         if (currentLore == null) {
            var3 = new ArrayList();
         } else {
            var3 = new ArrayList<>(currentLore);
         }

         var3.add(ChatUtil.component(line));
         this.meta.lore(var3);
      }

      return this;
   }

   public ItemBuilder enchant(Enchantment enchantment, int level) {
      if (this.meta != null) {
         this.meta.addEnchant(enchantment, level, true);
      }

      return this;
   }

   public ItemBuilder flag(ItemFlag... flags) {
      if (this.meta != null) {
         this.meta.addItemFlags(flags);
      }

      return this;
   }

   public ItemBuilder glow() {
      if (this.meta != null) {
         try {
            Method method = this.meta.getClass().getMethod("setEnchantmentGlintOverride", Boolean.class);
            method.invoke(this.meta, true);
            return this;
         } catch (Throwable var4) {
            try {
               Method methodx = this.meta.getClass().getMethod("setEnchantmentGlintOverride", boolean.class);
               methodx.invoke(this.meta, true);
               return this;
            } catch (Throwable var3) {
               try {
                  Enchantment ench = Enchantment.getByKey(NamespacedKey.minecraft("unbreaking"));
                  if (ench == null) {
                     ench = Enchantment.DURABILITY;
                  }

                  if (ench != null) {
                     this.meta.addEnchant(ench, 1, true);
                     this.meta.addItemFlags(new ItemFlag[]{ItemFlag.HIDE_ENCHANTS});
                  }
               } catch (Throwable var2) {
               }
            }
         }
      }

      return this;
   }

   public ItemBuilder glow(boolean condition) {
      if (condition) {
         this.glow();
      }

      return this;
   }

   public ItemBuilder hideAllFlags() {
      if (this.meta != null) {
         this.meta.addItemFlags(ItemFlag.values());
      }

      return this;
   }

   public ItemBuilder customModelData(int data) {
      if (this.meta != null) {
         this.meta.setCustomModelData(data);
      }

      return this;
   }

   public ItemBuilder skullOwner(OfflinePlayer player) {
      if (this.meta instanceof SkullMeta skullMeta && player != null) {
         skullMeta.setOwningPlayer(player);
      }

      return this;
   }

   public <T, Z> ItemBuilder pdc(NamespacedKey key, PersistentDataType<T, Z> type, Z value) {
      if (this.meta != null && key != null && type != null && value != null) {
         this.meta.getPersistentDataContainer().set(key, type, value);
      }

      return this;
   }

   public ItemStack build() {
      if (this.meta != null) {
         this.item.setItemMeta(this.meta);
      }

      return this.item;
   }
}
