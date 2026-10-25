package fr.esicraft.gui;

import fr.esicraft.EsicraftPlugin;
import fr.esicraft.data.PlayerData;
import fr.esicraft.gui.holder.RaceSelectHolder;
import fr.esicraft.race.Race;
import fr.esicraft.util.ChatUtil;
import fr.esicraft.util.ItemBuilder;
import java.util.ArrayList;
import java.util.List;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;

public class RaceSelectGUI {
   public static final String TITLE = "Choix de Classe & Métier";

   public static void open(Player player, EsicraftPlugin plugin) {
      Inventory inv = Bukkit.createInventory(new RaceSelectHolder(), 45, ChatUtil.component("&8» &6Choix de Classe & Métier"));
      ItemStack filler = new ItemBuilder(Material.GRAY_STAINED_GLASS_PANE).name(" ").build();

      for (int i = 0; i < 45; i++) {
         inv.setItem(i, filler);
      }

      Race currentRace = plugin.getRaceManager().getRace(player);
      Race[] races = new Race[]{Race.DWARF, Race.SYLVAN, Race.BLACKSMITH, Race.RANGER, Race.ARCANIST, Race.ENGINEER};
      int[] slots = new int[]{19, 21, 23, 25, 29, 33};

      for (int i = 0; i < races.length; i++) {
         Race r = races[i];
         boolean selected = r == currentRace;
         List<String> lore = new ArrayList<>();
         lore.add("&7" + r.getDescription());
         lore.add("");
         lore.add("&e&lCrafts Exclusifs :");

         for (String craft : r.getExclusiveCrafts()) {
            lore.add(craft);
         }

         lore.add("");
         lore.add("&6&lAvantages & Malus :");

         for (String perk : r.getPerksAndDrawbacks()) {
            lore.add(perk);
         }

         lore.add("");
         if (selected) {
            lore.add("&a✔ &lVotre Métier Actuel");
         } else if (currentRace == Race.NONE) {
            lore.add("&a✨ &lPremière sélection GRATUITE !");
            lore.add("&7(Puis 3 reconversions gratuites supplémentaires)");
            lore.add("&e➤ Cliquez pour choisir ce métier !");
         } else {
            PlayerData pData = plugin.getDataManager().getPlayerData(player.getUniqueId());
            int changesDone = pData != null ? pData.getRaceChangesCount() : 0;
            int remainingFree = Math.max(0, 3 - changesDone);
            if (remainingFree > 0) {
               lore.add("&a✨ &lReconversion GRATUITE &7(&e" + remainingFree + " restante(s)&7)");
            } else {
               lore.add("&6• Coût de reconversion : &e10 000 pièces &7(3 gratuits épuisés)");
            }

            lore.add("&e➤ Cliquez pour changer de métier !");
         }

         ItemBuilder builder = new ItemBuilder(r.getIcon()).name(r.getDisplayName()).lore(lore).hideAllFlags();
         if (selected) {
            builder.glow();
         }

         inv.setItem(slots[i], builder.build());
      }

      PlayerData pData = plugin.getDataManager().getPlayerData(player.getUniqueId());
      double coins = pData != null ? pData.getCoins() : 0.0;
      int changesDone = pData != null ? pData.getRaceChangesCount() : 0;
      int remainingFree = Math.max(0, 3 - changesDone);
      inv.setItem(
         4,
         new ItemBuilder(Material.BOOK)
            .name("&6&lSystème de Métiers Esicraft")
            .lore(
               "&7Chaque métier débloque des crafts uniques",
               "&7et des compétences passives adaptées à votre jeu.",
               "",
               "&eMétier actuel : &a" + currentRace.getDisplayName(),
               "&eVotre solde : &6" + String.format("%.2f", coins) + " pièces",
               currentRace == Race.NONE
                  ? "&a✨ Première sélection gratuite !"
                  : (
                     remainingFree > 0
                        ? "&a✨ Changements gratuits restants : &e" + remainingFree + "/3"
                        : "&c⚠ Changements gratuits épuisés (Coût : 10 000 pièces)"
                  )
            )
            .build()
      );
      player.openInventory(inv);
      ChatUtil.playSound(player, Sound.BLOCK_CHEST_OPEN, 1.0F, 1.2F);
   }
}
