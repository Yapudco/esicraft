package fr.esicraft.gui;

import fr.esicraft.EsicraftPlugin;
import fr.esicraft.gui.holder.GuildSpecHolder;
import fr.esicraft.guild.Guild;
import fr.esicraft.guild.GuildSpecialization;
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

public class GuildSpecGUI {
   public static final String TITLE = "Spécialisations de Guilde";

   public static void open(Player player, EsicraftPlugin plugin) {
      Guild guild = plugin.getGuildManager().getGuildByPlayer(player.getUniqueId());
      if (guild != null) {
         Inventory inv = Bukkit.createInventory(new GuildSpecHolder(), 45, ChatUtil.component("&8» &6Spécialisations de Guilde"));
         ItemStack filler = new ItemBuilder(Material.GRAY_STAINED_GLASS_PANE).name(" ").build();

         for (int i = 0; i < 45; i++) {
            inv.setItem(i, filler);
         }

         GuildSpecialization currentSpec = guild.getSpecialization();
         GuildSpecialization[] specs = new GuildSpecialization[]{
            GuildSpecialization.WARLORD,
            GuildSpecialization.UNDERGROUND_INDUSTRY,
            GuildSpecialization.DRUIDIC_ENCLAVE,
            GuildSpecialization.MERCHANT_SYNDICATE,
            GuildSpecialization.ARCANE_CONCLAVE,
            GuildSpecialization.LONE_RANGER
         };
         int[] slots = new int[]{19, 20, 21, 22, 23, 24};

         for (int i = 0; i < specs.length; i++) {
            GuildSpecialization spec = specs[i];
            boolean selected = spec == currentSpec;
            List<String> lore = new ArrayList<>();
            lore.add("&7" + spec.getDescription());
            lore.add("");
            lore.add("&a&lBonus de Guilde :");

            for (String perk : spec.getPerks()) {
               lore.add(perk);
            }

            lore.add("");
            lore.add("&c&lContraintes & Quotas :");

            for (String restriction : spec.getRestrictions()) {
               lore.add(restriction);
            }

            lore.add("");
            if (selected) {
               lore.add("&a✔ &lSpécialisation Active");
            } else {
               lore.add("&e➤ Cliquez pour adopter cette spécialisation !");
            }

            ItemBuilder builder = new ItemBuilder(spec.getIcon()).name(spec.getDisplayName()).lore(lore).hideAllFlags();
            if (selected) {
               builder.glow();
            }

            inv.setItem(slots[i], builder.build());
         }

         boolean canChange = guild.canChangeSpecialization();
         List<String> infoLore = new ArrayList<>();
         infoLore.add("&7La spécialisation confère de puissants atouts collectifs");
         infoLore.add("&7mais impose des contraintes de composition raciale.");
         infoLore.add("");
         infoLore.add("&eSpécialisation actuelle : &a" + currentSpec.getDisplayName());
         infoLore.add("");
         if (currentSpec == GuildSpecialization.NONE) {
            infoLore.add("&a✨ Choix initial disponible !");
         } else if (canChange) {
            infoLore.add("&a✨ Changement disponible : &eDÉBLOQUÉ");
            infoLore.add("&7(Un nouveau membre a rejoint la guilde !)");
         } else {
            infoLore.add("&c\ud83d\udd12 Changement verrouillé : &cNÉCESSITE UN NOUVEAU JOUEUR");
            infoLore.add("&7Un &enouveau joueur historique &7doit rejoindre la guilde");
            infoLore.add("&7pour débloquer un changement de spécialisation.");
         }

         inv.setItem(4, new ItemBuilder(Material.NETHER_STAR).name("&6&lOrientation Stratégique").lore(infoLore).build());
         inv.setItem(36, new ItemBuilder(Material.ARROW).name("&c⬅ Retour").build());
         player.openInventory(inv);
         ChatUtil.playSound(player, Sound.BLOCK_CHEST_OPEN, 1.0F, 1.2F);
      }
   }
}
