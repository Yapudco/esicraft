package fr.esicraft.gui;

import fr.esicraft.EsicraftPlugin;
import fr.esicraft.gui.holder.GuildMainHolder;
import fr.esicraft.guild.Guild;
import fr.esicraft.guild.GuildRole;
import fr.esicraft.nexus.Nexus;
import fr.esicraft.util.ChatUtil;
import fr.esicraft.util.ItemBuilder;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.Map.Entry;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;

public class GuildMainGUI {
   public static final String TITLE = "Gestion de Guilde";

   public static void open(Player player, EsicraftPlugin plugin) {
      Guild guild = plugin.getGuildManager().getGuildByPlayer(player.getUniqueId());
      if (guild == null) {
         ChatUtil.sendMessage(player, "&cVous n'avez pas de guilde. Utilisez &e/guild create <nom> <tag> &cpour en fonder une !");
      } else {
         Inventory inv = Bukkit.createInventory(new GuildMainHolder(), 45, ChatUtil.component("&8» &6Gestion de Guilde : " + guild.getName()));
         ItemStack filler = new ItemBuilder(Material.GRAY_STAINED_GLASS_PANE).name(" ").build();

         for (int i = 0; i < 45; i++) {
            inv.setItem(i, filler);
         }

         GuildRole role = guild.getRole(player.getUniqueId()) != null ? guild.getRole(player.getUniqueId()) : GuildRole.RECRUIT;
         inv.setItem(
            13,
            new ItemBuilder(Material.BEACON)
               .name("&6&l" + guild.getName() + " &8[&e" + guild.getTag() + "&8]")
               .lore(
                  "&7Chef : &e" + Bukkit.getOfflinePlayer(guild.getLeaderUuid()).getName(),
                  "&7Votre rôle : &a" + role.getDisplayName(),
                  "&7Âge technologique : " + guild.getCurrentAge().getDisplayName() + " &7(Niveau " + guild.getCurrentAge().getLevel() + "/6)",
                  "&7Spécialisation : " + guild.getSpecialization().getDisplayName(),
                  "&7Membres : &f" + guild.getMembers().size(),
                  "&7Territoires Claimés : &e" + guild.getClaimedChunks().size() + " chunks",
                  "&7Banque : &6" + String.format("%.2f", guild.getBankBalance()) + " pièces"
               )
               .glow()
               .build()
         );
         inv.setItem(
            20,
            new ItemBuilder(guild.getSpecialization().getIcon())
               .name("&e&lSpécialisation de Guilde")
               .lore("&7Orientation actuelle : " + guild.getSpecialization().getDisplayName(), "", "&e➤ Cliquez pour modifier la spécialisation.")
               .build()
         );
         Nexus nexus = guild.getNexus();
         String nexusStatus = nexus != null && nexus.isAlive()
            ? "&aActif (" + nexus.getCurrentHealth() + "/" + nexus.getMaxHealth() + " PV)"
            : "&cDétruit / Inexistant";
         inv.setItem(
            22,
            new ItemBuilder(Material.CRYING_OBSIDIAN)
               .name("&5&lCœur de Guilde (Nexus)")
               .lore(
                  "&7État : " + nexusStatus,
                  "&7Emplacement : "
                     + (
                        nexus != null && nexus.getLocation() != null
                           ? "&fX: " + nexus.getLocation().getBlockX() + ", Y: " + nexus.getLocation().getBlockY() + ", Z: " + nexus.getLocation().getBlockZ()
                           : "&cNon placé"
                     ),
                  "",
                  nexus != null && nexus.isAlive()
                     ? "&e➤ Cliquez pour ouvrir le menu des pièges défensifs !"
                     : "&e➤ Utilisez &6/guild nexus place &epour poser le Nexus dans votre claim."
               )
               .build()
         );
         List<String> memberLore = new ArrayList<>();
         memberLore.add("&7Effectif actuel de la guilde :");

         for (Entry<UUID, GuildRole> entry : guild.getMembers().entrySet()) {
            String pName = Bukkit.getOfflinePlayer(entry.getKey()).getName();
            memberLore.add("&8• &e" + (pName != null ? pName : "Inconnu") + " &7- " + entry.getValue().getDisplayName());
         }

         inv.setItem(24, new ItemBuilder(Material.PLAYER_HEAD).name("&b&lMembres de la Guilde").lore(memberLore).build());
         inv.setItem(
            31,
            new ItemBuilder(Material.GRASS_BLOCK)
               .name("&a&lTerritoires & Claims")
               .lore(
                  "&7Chunks occupés : &e" + guild.getClaimedChunks().size() + " chunks",
                  "",
                  "&e➤ Cliquez pour afficher/masquer la bordure visuelle des claims !",
                  "&7(/guild claim pour revendiquer le chunk actuel)"
               )
               .build()
         );
         player.openInventory(inv);
         ChatUtil.playSound(player, Sound.BLOCK_CHEST_OPEN, 1.0F, 1.2F);
      }
   }
}
