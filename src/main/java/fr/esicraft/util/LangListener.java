package fr.esicraft.util;

import fr.esicraft.EsicraftPlugin;
import java.util.ArrayList;
import java.util.List;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryOpenEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

/** Traduit à l'ouverture (et après chaque clic) les menus du plugin pour les joueurs anglophones. */
public class LangListener implements Listener {
   private final EsicraftPlugin plugin;

   public LangListener(EsicraftPlugin plugin) {
      this.plugin = plugin;
   }

   private boolean isPluginMenu(Inventory inventory) {
      InventoryHolder holder = inventory.getHolder();
      return holder != null && holder.getClass().getName().startsWith("fr.esicraft");
   }

   @EventHandler(priority = EventPriority.MONITOR)
   public void onOpen(InventoryOpenEvent event) {
      if (event.getPlayer() instanceof Player player && Lang.isEnglish(player) && this.isPluginMenu(event.getInventory())) {
         translateMenu(player, event.getInventory());
         Bukkit.getScheduler().runTask(this.plugin, () -> {
            translateMenu(player, player.getOpenInventory().getTopInventory());
            translateTitle(player);
         });
      }
   }

   @EventHandler(priority = EventPriority.MONITOR)
   public void onClick(InventoryClickEvent event) {
      if (event.getWhoClicked() instanceof Player player && Lang.isEnglish(player) && this.isPluginMenu(event.getView().getTopInventory())) {
         Bukkit.getScheduler().runTask(this.plugin, () -> {
            Inventory top = player.getOpenInventory().getTopInventory();
            if (this.isPluginMenu(top)) {
               translateMenu(player, top);
            }
         });
      }
   }

   /** Le titre du menu ne peut être changé qu'une fois la fenêtre ouverte. */
   private static void translateTitle(Player player) {
      String legacy = ChatUtil.toLegacy(player.getOpenInventory().title());
      String english = Lang.toEnglish(legacy);
      if (!english.equals(legacy)) {
         player.getOpenInventory().setTitle(ChatUtil.color(english));
      }
   }

   static void translateMenu(Player player, Inventory inventory) {
      for (int slot = 0; slot < inventory.getSize(); slot++) {
         ItemStack item = inventory.getItem(slot);
         if (item != null && item.hasItemMeta()) {
            ItemMeta meta = item.getItemMeta();
            boolean changed = false;
            Component name = meta.displayName();
            if (name != null) {
               Component translated = translate(name);
               if (translated != null) {
                  meta.displayName(translated);
                  changed = true;
               }
            }

            List<Component> lore = meta.lore();
            if (lore != null) {
               List<Component> out = new ArrayList<>(lore.size());
               boolean loreChanged = false;
               for (Component line : lore) {
                  Component translated = translate(line);
                  out.add(translated != null ? translated : line);
                  loreChanged |= translated != null;
               }

               if (loreChanged) {
                  meta.lore(out);
                  changed = true;
               }
            }

            if (changed) {
               item.setItemMeta(meta);
            }
         }
      }
   }

   /** Renvoie le composant traduit, ou null s'il n'y a rien à changer. */
   private static Component translate(Component original) {
      String legacy = ChatUtil.toLegacy(original);
      String english = Lang.toEnglish(legacy);
      if (english.equals(legacy)) {
         return null;
      }

      Component result = ChatUtil.component(english);
      if (original.decoration(TextDecoration.ITALIC) == TextDecoration.State.FALSE) {
         result = result.decoration(TextDecoration.ITALIC, false);
      }

      return result;
   }
}
