package fr.esicraft.gui;

import fr.esicraft.EsicraftPlugin;
import fr.esicraft.gui.holder.MercenaryHordeHolder;
import fr.esicraft.gui.holder.RaidTargetHolder;
import fr.esicraft.guild.Guild;
import fr.esicraft.guild.GuildRole;
import fr.esicraft.guild.GuildSpecialization;
import fr.esicraft.raid.RaidType;
import fr.esicraft.util.ChatUtil;
import fr.esicraft.util.ItemBuilder;
import java.util.ArrayList;
import java.util.List;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.persistence.PersistentDataType;

public class MercenaryHordeGUI {
   public static final String TITLE = "Contrats de Hordes Noires";
   public static final NamespacedKey TARGET_GUILD_KEY = new NamespacedKey(EsicraftPlugin.getInstance(), "raid_target_guild_id");

   public static void open(Player player, EsicraftPlugin plugin) {
      Guild playerGuild = plugin.getGuildManager().getGuildByPlayer(player.getUniqueId());
      if (playerGuild == null) {
         ChatUtil.sendMessage(player, "&c❌ Vous devez faire partie d'une guilde pour recruter et lancer des hordes de mercenaires !");
         ChatUtil.playSound(player, Sound.ENTITY_VILLAGER_NO, 1.0F, 1.0F);
      } else if (!playerGuild.getRole(player.getUniqueId()).isAtLeast(GuildRole.OFFICER)) {
         ChatUtil.sendMessage(player, "&c❌ Seuls les Officiers et le Chef de guilde ont l'autorisation de recruter et lancer des hordes !");
         ChatUtil.playSound(player, Sound.ENTITY_VILLAGER_NO, 1.0F, 1.0F);
      } else {
         Inventory inv = Bukkit.createInventory(new MercenaryHordeHolder(), 54, ChatUtil.component("&8» &4Contrats de Hordes Noires"));
         ItemStack filler = new ItemBuilder(Material.RED_STAINED_GLASS_PANE).name(" ").build();

         for (int i = 0; i < 54; i++) {
            inv.setItem(i, filler);
         }

         GuildSpecialization spec = playerGuild.getSpecialization();
         double specMultiplier = spec == GuildSpecialization.WARLORD ? 0.7 : (spec == GuildSpecialization.MERCHANT_SYNDICATE ? 1.3 : 1.0);
         boolean breachers = CustomHordeGUI.isBreachersEnabled(player.getUniqueId());
         boolean lavaResist = CustomHordeGUI.isLavaResistanceEnabled(player.getUniqueId());
         double breacherCost = 2500.0 * specMultiplier;
         double lavaCost = 1500.0 * specMultiplier;
         String specNote = "";
         if (spec == GuildSpecialization.WARLORD) {
            specNote = "&a⚡ Réduction Légion Guerrière : &6-30% sur tous les contrats !";
         } else if (spec == GuildSpecialization.MERCHANT_SYNDICATE) {
            specNote = "&c⚠️ Malus Syndicat Marchand : &c+30% sur tous les contrats !";
         }

         inv.setItem(
            4,
            new ItemBuilder(Material.WITHER_SKELETON_SKULL)
               .name("&4&lMarché Noir : Contrats de Mercenaires")
               .lore(
                  "&7Guilde : &6" + playerGuild.getName() + " &8[&e" + playerGuild.getTag() + "&8]",
                  "&7Banque de Guilde : &a" + String.format("%.2f", playerGuild.getBankBalance()) + " pièces",
                  "",
                  "&7Commanditez des attaques de monstres payées par la guilde.",
                  "&c(Réservé aux Officiers & Chefs de guilde)",
                  specNote
               )
               .build()
         );
         RaidType[] types = RaidType.values();
         int[] slots = new int[]{19, 20, 21, 22, 23, 24, 25, 26};

         for (int i = 0; i < types.length && i < slots.length; i++) {
            RaidType type = types[i];
            double price = type.getBasePrice() * specMultiplier;
            if (breachers) {
               price += breacherCost;
            }

            if (lavaResist) {
               price += lavaCost;
            }

            List<String> lore = new ArrayList<>();
            lore.add(type.getDescription());
            lore.add("");
            lore.add("&c• Effectif : &e" + type.getMobCount() + " monstres");
            lore.add("&c• Dégâts Nexus / impact : &e" + type.getDamageToNexus() + " PV");
            lore.add(
               "&6• Prix (Banque Guilde) : &e"
                  + String.format("%.2f", price)
                  + " pièces"
                  + (spec == GuildSpecialization.WARLORD ? " &a(-30% Légion)" : (spec == GuildSpecialization.MERCHANT_SYNDICATE ? " &c(+30% Syndicat)" : ""))
            );
            if (breachers) {
               lore.add("&c• Sapeurs Explosifs : &aINCLUS (+" + String.format("%.0f", breacherCost) + " pièces)");
            }

            if (lavaResist) {
               lore.add("&6• Résistance Lave & Feu : &aINCLUSE (+" + String.format("%.0f", lavaCost) + " pièces)");
            }

            lore.add("");
            if (playerGuild.getBankBalance() >= price) {
               lore.add("&a✔ Fonds disponibles en banque de guilde.");
               lore.add("&e➤ Cliquez pour choisir la guilde cible !");
            } else {
               lore.add("&c❌ Fonds insuffisants en banque de guilde !");
               lore.add("&7(Déposez avec &e/guild bank <montant>&7)");
            }

            inv.setItem(slots[i], new ItemBuilder(type.getIcon()).name(type.getDisplayName()).lore(lore).glow(playerGuild.getBankBalance() >= price).build());
         }

         inv.setItem(
            40,
            new ItemBuilder(Material.ANVIL)
               .name("&6&l\ud83d\udee0️ Créer une Horde Personnalisée")
               .lore(
                  "&7Composez vous-même votre armée monstre unité par unité.",
                  "&7Choisissez parmi 8 classes de monstres (Zombies, Ravageurs, Sorcières...).",
                  "&7Payé directement par la banque de guilde !",
                  "",
                  "&e➤ Cliquez pour ouvrir le Créateur de Horde !"
               )
               .glow()
               .build()
         );
         inv.setItem(
            48,
            new ItemBuilder(Material.TNT)
               .name(breachers ? "&c&l\ud83d\udca3 Sapeurs Explosifs : &a&lACTIVÉS" : "&7\ud83d\udca3 Sapeurs Explosifs : &c&lDÉSACTIVÉS")
               .lore(
                  "&7Permet aux monstres bloqués par des murs ou des tranchées",
                  "&7d'exploser pour créer une brèche vers le Nexus ennemi.",
                  "",
                  "&6• Surcoût : &e+" + String.format("%.2f", breacherCost) + " pièces",
                  "",
                  "&e➤ Cliquez pour activer/désactiver cette option !"
               )
               .glow(breachers)
               .build()
         );
         inv.setItem(
            50,
            new ItemBuilder(Material.LAVA_BUCKET)
               .name(lavaResist ? "&6&l\ud83d\udd25 Ignifugation (Lave & Feu) : &a&lACTIVÉE" : "&7\ud83d\udd25 Ignifugation (Lave & Feu) : &c&lDÉSACTIVÉE")
               .lore(
                  "&7Immunise totalement vos monstres contre la lave et les flammes.",
                  "&7Ils traversent les douves de lave protectrices sans aucun dégât !",
                  "",
                  "&6• Surcoût : &e+" + String.format("%.2f", lavaCost) + " pièces",
                  "",
                  "&e➤ Cliquez pour activer/désactiver cette option !"
               )
               .glow(lavaResist)
               .build()
         );
         inv.setItem(45, new ItemBuilder(Material.ARROW).name("&c⬅ Retour à l'Hôtel des Ventes").build());
         player.openInventory(inv);
         ChatUtil.playSound(player, Sound.BLOCK_ANVIL_USE, 0.8F, 0.7F);
      }
   }

   public static void openTargetSelector(Player player, RaidType raidType, EsicraftPlugin plugin) {
      Guild playerGuild = plugin.getGuildManager().getGuildByPlayer(player.getUniqueId());
      if (playerGuild == null) {
         ChatUtil.sendMessage(player, "&c❌ Vous devez faire partie d'une guilde pour lancer un raid !");
      } else if (!playerGuild.getRole(player.getUniqueId()).isAtLeast(GuildRole.OFFICER)) {
         ChatUtil.sendMessage(player, "&c❌ Seuls les Officiers et le Chef de guilde peuvent lancer des hordes de mercenaires !");
         ChatUtil.playSound(player, Sound.ENTITY_VILLAGER_NO, 1.0F, 1.0F);
      } else {
         String targetTitle = raidType != null ? "&8» &cCible : " + raidType.name() : "&8» &cCible : Horde Personnalisée";
         Inventory inv = Bukkit.createInventory(new RaidTargetHolder(raidType, raidType == null), 54, ChatUtil.component(targetTitle));
         ItemStack filler = new ItemBuilder(Material.GRAY_STAINED_GLASS_PANE).name(" ").build();

         for (int i = 0; i < 9; i++) {
            inv.setItem(i, filler);
         }

         for (int i = 45; i < 54; i++) {
            inv.setItem(i, filler);
         }

         int slot = 9;

         for (Guild guild : plugin.getGuildManager().getAllGuilds()) {
            if (!guild.getId().equals(playerGuild.getId()) && guild.hasNexus()) {
               boolean isHegemony = plugin.getGuildManager().isEconomicHegemony(guild);
               List<String> lore = new ArrayList<>();
               lore.add("&7Chef : &e" + Bukkit.getOfflinePlayer(guild.getLeaderUuid()).getName());
               lore.add(
                  "&7Nexus : &a" + (guild.getNexus() != null ? guild.getNexus().getCurrentHealth() + "/" + guild.getNexus().getMaxHealth() + " PV" : "Inconnu")
               );
               lore.add("&7Banque : &a" + String.format("%.2f", guild.getBankBalance()) + " pièces");
               if (isHegemony) {
                  lore.add("");
                  lore.add("&6&l⭐ HÉGÉMONIE ÉCONOMIQUE &e(Cible Prioritaire) :");
                  lore.add("&a✔ Coût de raid : &6-50% (Moitié prix !)");
                  lore.add("&a✔ Cooldown de raid : &e2 minutes seulement !");
               }

               lore.add("");
               lore.add("&4➤ Cliquez pour LANCER L'INVASION sur cette guilde !");
               inv.setItem(
                  slot,
                  new ItemBuilder(Material.SHIELD)
                     .name((isHegemony ? "&6⭐ &c⚔️ " : "&c⚔️ ") + guild.getName() + " &8[&e" + guild.getTag() + "&8]")
                     .lore(lore)
                     .pdc(TARGET_GUILD_KEY, PersistentDataType.STRING, guild.getId())
                     .glow(isHegemony)
                     .build()
               );
               if (++slot >= 45) {
                  break;
               }
            }
         }

         inv.setItem(45, new ItemBuilder(Material.ARROW).name("&c⬅ Retour").build());
         player.openInventory(inv);
      }
   }
}
