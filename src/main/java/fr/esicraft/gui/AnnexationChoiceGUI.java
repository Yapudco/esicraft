package fr.esicraft.gui;

import fr.esicraft.EsicraftPlugin;
import fr.esicraft.gui.holder.AnnexationChoiceHolder;
import fr.esicraft.guild.Guild;
import fr.esicraft.util.ChatUtil;
import fr.esicraft.util.ItemBuilder;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;

public class AnnexationChoiceGUI {
   public static final String TITLE = "Choix d'Allégeance de Guilde";

   public static void open(Player player, Guild victorGuild, EsicraftPlugin plugin) {
      Inventory inv = Bukkit.createInventory(new AnnexationChoiceHolder(), 27, ChatUtil.component("&8» &4Vaincu : Choix d'Allégeance"));
      ItemStack filler = new ItemBuilder(Material.BLACK_STAINED_GLASS_PANE).name(" ").build();

      for (int i = 0; i < 27; i++) {
         inv.setItem(i, filler);
      }

      inv.setItem(
         4,
         new ItemBuilder(Material.SHIELD)
            .name("&4&lVOTRE GUILDE A ÉTÉ VAINCUE !")
            .lore(
               "&7Votre Nexus a été détruit par &6" + victorGuild.getName() + "&7.",
               "&7Tous les territoires ont été annexés par le vainqueur.",
               "",
               "&eFaites votre choix d'avenir :"
            )
            .build()
      );
      inv.setItem(
         11,
         new ItemBuilder(Material.EMERALD_BLOCK)
            .name("&a&l\ud83d\udfe2 REJOINDRE LA GUILDE VAINQUEUR")
            .lore(
               "&7Prêtez allégeance à &6" + victorGuild.getName() + "&7.",
               "&7Vous deviendrez membre à part entière de leur guilde.",
               "&7Vous conserverez l'accès aux territoires annexés.",
               "",
               "&a➤ Cliquez pour prêter allégeance !"
            )
            .glow()
            .build()
      );
      inv.setItem(
         15,
         new ItemBuilder(Material.REDSTONE_BLOCK)
            .name("&c&l\ud83d\udd34 REFUSER ET RESTER INDÉPENDANT")
            .lore(
               "&7Refusez de servir la guilde vainqueur.",
               "&7Vous redeviendrez un joueur sans guilde (nomade).",
               "&7Vous pourrez fonder ou rejoindre une autre guilde plus tard.",
               "",
               "&c➤ Cliquez pour refuser l'annexion !"
            )
            .build()
      );
      player.openInventory(inv);
      ChatUtil.playSound(player, Sound.ENTITY_WITHER_DEATH, 0.7F, 0.8F);
   }
}
