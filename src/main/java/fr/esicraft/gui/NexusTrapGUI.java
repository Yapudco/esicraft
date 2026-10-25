package fr.esicraft.gui;

import fr.esicraft.EsicraftPlugin;
import fr.esicraft.gui.holder.NexusTrapHolder;
import fr.esicraft.guild.Guild;
import fr.esicraft.guild.GuildSpecialization;
import fr.esicraft.nexus.Nexus;
import fr.esicraft.nexus.NexusTrapType;
import fr.esicraft.util.ChatUtil;
import fr.esicraft.util.ItemBuilder;
import java.util.ArrayList;
import java.util.List;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;

public class NexusTrapGUI {
   public static final String TITLE_RAW = "Défenses & Pièges du Nexus";

   public static void open(Player player, EsicraftPlugin plugin) {
      Guild guild = plugin.getGuildManager().getGuildByPlayer(player.getUniqueId());
      if (guild != null && guild.hasNexus()) {
         Nexus nexus = guild.getNexus();
         Inventory inv = Bukkit.createInventory(new NexusTrapHolder(), 27, ChatUtil.component("&8&lDéfenses & Pièges du Nexus"));
         ItemStack glass = new ItemBuilder(Material.GRAY_STAINED_GLASS_PANE).name(" ").build();

         for (int i = 0; i < 27; i++) {
            inv.setItem(i, glass);
         }

         inv.setItem(
            4,
            new ItemBuilder(Material.CRYING_OBSIDIAN)
               .name("&6&lCœur de Guilde : &e" + guild.getName())
               .lore(
                  "&7Santé : &a" + nexus.getCurrentHealth() + " &7/ &a" + nexus.getMaxHealth() + " PV",
                  "&7Banque de Guilde : &6" + String.format("%.2f", guild.getBankBalance()) + " pièces",
                  "&7Spécialisation : " + guild.getSpecialization().getDisplayName(),
                  guild.getSpecialization() == GuildSpecialization.UNDERGROUND_INDUSTRY
                     ? "&a⚡ Réduction Spéciale Industrie : &6-50% sur tous les pièges !"
                     : "",
                  "&8(Les pièges restent armés jusqu'à la fin de la prochaine horde)"
               )
               .build()
         );
         int[] slots = new int[]{11, 12, 13, 14, 15};
         NexusTrapType[] traps = NexusTrapType.values();

         for (int i = 0; i < traps.length && i < slots.length; i++) {
            NexusTrapType trap = traps[i];
            boolean active = nexus.hasTrap(trap);
            double cost = trap.getBaseCost();
            if (guild.getSpecialization() == GuildSpecialization.UNDERGROUND_INDUSTRY) {
               cost *= 0.5;
            }

            List<String> lore = new ArrayList<>();
            lore.add(trap.getDescription());
            lore.add("");
            lore.add("&7Statut : " + (active ? "&a✔ ACTIF &7(Armé pour la prochaine horde)" : "&c✖ INACTIF &7(Non armé)"));
            lore.add("&7Coût d'armement : &6" + String.format("%.2f", cost) + " pièces");
            lore.add("");
            if (active) {
               lore.add("&a✔ Piège actuellement armé et fonctionnel !");
               lore.add("&7(Sera consommé et à racheter après la prochaine attaque)");
            } else {
               lore.add("&e▶ Cliquez pour acheter et armer ce piège !");
               lore.add("&8(Priorité débit : Banque de guilde puis solde personnel)");
            }

            ItemBuilder builder = new ItemBuilder(trap.getIcon()).name((active ? "&a" : "&e") + trap.getDisplayName()).lore(lore);
            if (active) {
               builder.glow();
            }

            inv.setItem(slots[i], builder.build());
         }

         player.openInventory(inv);
      } else {
         ChatUtil.sendMessage(player, "&cVotre guilde ne possède pas de Nexus actif. Posez-le avec &e/guild nexus place&c.");
      }
   }
}
