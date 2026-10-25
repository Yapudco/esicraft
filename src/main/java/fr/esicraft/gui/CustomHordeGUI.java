package fr.esicraft.gui;

import fr.esicraft.EsicraftPlugin;
import fr.esicraft.gui.holder.CustomHordeHolder;
import fr.esicraft.guild.Guild;
import fr.esicraft.guild.GuildRole;
import fr.esicraft.guild.GuildSpecialization;
import fr.esicraft.raid.CustomHordeUnit;
import fr.esicraft.util.ChatUtil;
import fr.esicraft.util.ItemBuilder;
import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.Map.Entry;
import java.util.concurrent.ConcurrentHashMap;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;

public class CustomHordeGUI {
   public static final String TITLE = "Créateur de Horde Personnalisée";
   public static final double BREACHER_COST = 2500.0;
   public static final double LAVA_RESISTANCE_COST = 1500.0;
   private static final Map<UUID, Map<CustomHordeUnit, Integer>> drafts = new ConcurrentHashMap<>();
   private static final Map<UUID, Boolean> breachersToggle = new ConcurrentHashMap<>();
   private static final Map<UUID, Boolean> lavaResistanceToggle = new ConcurrentHashMap<>();

   public static Map<CustomHordeUnit, Integer> getDraft(UUID uuid) {
      return drafts.computeIfAbsent(uuid, k -> Collections.synchronizedMap(new EnumMap<>(CustomHordeUnit.class)));
   }

   public static void clearDraft(UUID uuid) {
      drafts.remove(uuid);
   }

   public static boolean isBreachersEnabled(UUID uuid) {
      return breachersToggle.getOrDefault(uuid, false);
   }

   public static void toggleBreachers(UUID uuid) {
      breachersToggle.put(uuid, !isBreachersEnabled(uuid));
   }

   public static boolean isLavaResistanceEnabled(UUID uuid) {
      return lavaResistanceToggle.getOrDefault(uuid, false);
   }

   public static void toggleLavaResistance(UUID uuid) {
      lavaResistanceToggle.put(uuid, !isLavaResistanceEnabled(uuid));
   }

   public static int getTotalMobCount(Map<CustomHordeUnit, Integer> draft) {
      synchronized (draft) {
         return draft.values().stream().mapToInt(Integer::intValue).sum();
      }
   }

   public static double getTotalCost(Map<CustomHordeUnit, Integer> draft, GuildSpecialization spec, boolean isBreachers, boolean isLavaResist) {
      double cost = 0.0;
      synchronized (draft) {
         for (Entry<CustomHordeUnit, Integer> entry : draft.entrySet()) {
            cost += entry.getKey().getCostPerUnit() * (double)entry.getValue().intValue();
         }
      }

      if (isBreachers) {
         cost += 2500.0;
      }

      if (isLavaResist) {
         cost += 1500.0;
      }

      if (spec == GuildSpecialization.WARLORD) {
         cost *= 0.7;
      } else if (spec == GuildSpecialization.MERCHANT_SYNDICATE) {
         cost *= 1.3;
      }

      return cost;
   }

   public static double getTotalCost(Map<CustomHordeUnit, Integer> draft, boolean isMerchant, boolean isBreachers, boolean isLavaResist) {
      return getTotalCost(draft, isMerchant ? GuildSpecialization.MERCHANT_SYNDICATE : GuildSpecialization.NONE, isBreachers, isLavaResist);
   }

   public static int getTotalDamage(Map<CustomHordeUnit, Integer> draft) {
      int dmg = 0;
      synchronized (draft) {
         for (Entry<CustomHordeUnit, Integer> entry : draft.entrySet()) {
            dmg += entry.getKey().getDamageToNexus() * entry.getValue();
         }

         return dmg;
      }
   }

   public static void open(Player player, EsicraftPlugin plugin) {
      Guild guild = plugin.getGuildManager().getGuildByPlayer(player.getUniqueId());
      if (guild == null) {
         ChatUtil.sendMessage(player, "&c❌ Vous devez faire partie d'une guilde pour créer et lancer des hordes sur-mesure !");
         ChatUtil.playSound(player, Sound.ENTITY_VILLAGER_NO, 1.0F, 1.0F);
      } else if (!guild.getRole(player.getUniqueId()).isAtLeast(GuildRole.OFFICER)) {
         ChatUtil.sendMessage(player, "&c❌ Seuls les Officiers et le Chef de guilde ont l'autorisation de créer et lancer des hordes !");
         ChatUtil.playSound(player, Sound.ENTITY_VILLAGER_NO, 1.0F, 1.0F);
      } else {
         Inventory inv = Bukkit.createInventory(new CustomHordeHolder(), 54, ChatUtil.component("&8» &4Créateur de Horde Sur-Mesure"));
         ItemStack filler = new ItemBuilder(Material.GRAY_STAINED_GLASS_PANE).name(" ").build();

         for (int i = 0; i < 54; i++) {
            inv.setItem(i, filler);
         }

         GuildSpecialization spec = guild.getSpecialization();
         Map<CustomHordeUnit, Integer> draft = getDraft(player.getUniqueId());
         boolean breachers = isBreachersEnabled(player.getUniqueId());
         boolean lavaResist = isLavaResistanceEnabled(player.getUniqueId());
         int totalUnits = getTotalMobCount(draft);
         double totalCost = getTotalCost(draft, spec, breachers, lavaResist);
         int totalDamage = getTotalDamage(draft);
         String specTag = "";
         if (spec == GuildSpecialization.WARLORD) {
            specTag = " &a(-30% Légion)";
         } else if (spec == GuildSpecialization.MERCHANT_SYNDICATE) {
            specTag = " &c(+30% Syndicat)";
         }

         inv.setItem(
            4,
            new ItemBuilder(Material.NETHER_STAR)
               .name("&6&lComposition de votre Horde Personnalisée")
               .lore(
                  "&7Guilde : &6" + guild.getName() + " &8[&e" + guild.getTag() + "&8]",
                  "&7Banque de Guilde : &a" + String.format("%.2f", guild.getBankBalance()) + " pièces",
                  "",
                  "&c• Total Monstres : &e" + totalUnits + " &7(Illimité)",
                  "&c• Dégâts Nexus cumulés : &e" + totalDamage + " PV",
                  "&6• Prix Total (Banque Guilde) : &e" + String.format("%.2f", totalCost) + " pièces" + specTag,
                  breachers ? "&c• Sapeurs Explosifs : &aACTIVÉS (+2 500 pièces)" : "&7• Sapeurs Explosifs : &cDÉSACTIVÉS",
                  lavaResist ? "&6• Résistance Lave & Feu : &aACTIVÉE (+1 500 pièces)" : "&7• Résistance Lave & Feu : &cDÉSACTIVÉE",
                  "",
                  "&7Ajustez les effectifs ci-dessous avec les clics."
               )
               .build()
         );
         CustomHordeUnit[] units = CustomHordeUnit.values();
         int[] slots = new int[]{19, 20, 21, 22, 23, 24, 25, 26};

         for (int i = 0; i < units.length && i < slots.length; i++) {
            CustomHordeUnit unit = units[i];
            int count;
            synchronized (draft) {
               count = draft.getOrDefault(unit, 0);
            }

            List<String> lore = new ArrayList<>();
            lore.add(unit.getDescription());
            lore.add("");
            lore.add("&6• Coût unitaire : &e" + String.format("%.2f", unit.getCostPerUnit()) + " pièces");
            lore.add("&c• Dégâts Nexus : &e" + unit.getDamageToNexus() + " PV / unité");
            lore.add("");
            lore.add("&e• Quantité actuelle : " + (count > 0 ? "&a&l" + count : "&70"));
            lore.add("&7(Sous-total : &6" + String.format("%.2f", (double)count * unit.getCostPerUnit()) + " pièces&7)");
            lore.add("");
            lore.add("&a[Clic Gauche] &7: +1 unité &8(+5 avec Shift)");
            lore.add("&c[Clic Droit] &7: -1 unité &8(-5 avec Shift)");
            ItemBuilder builder = new ItemBuilder(unit.getIcon()).name(unit.getDisplayName() + (count > 0 ? " &8[&a" + count + "&8]" : "")).lore(lore);
            if (count > 0) {
               builder.glow();
               builder.amount(Math.min(64, Math.max(1, count)));
            }

            inv.setItem(slots[i], builder.build());
         }

         inv.setItem(45, new ItemBuilder(Material.ARROW).name("&c⬅ Retour aux Contrats Standards").build());
         inv.setItem(
            47, new ItemBuilder(Material.BARRIER).name("&c❌ Réinitialiser la composition").lore("&7Remet le compte de toutes les unités à zéro.").build()
         );
         double breacherPrice = spec == GuildSpecialization.WARLORD ? 1750.0 : (spec == GuildSpecialization.MERCHANT_SYNDICATE ? 3250.0 : 2500.0);
         inv.setItem(
            49,
            new ItemBuilder(Material.TNT)
               .name(breachers ? "&c&l\ud83d\udca3 Sapeurs Explosifs : &a&lACTIVÉS" : "&7\ud83d\udca3 Sapeurs Explosifs : &c&lDÉSACTIVÉS")
               .lore(
                  "&7Permet aux monstres bloqués par des murs ou des tranchées",
                  "&7d'exploser pour créer une brèche vers le Nexus ennemi.",
                  "",
                  "&6• Surcoût : &e+" + String.format("%.2f", breacherPrice) + " pièces",
                  "",
                  "&e➤ Cliquez pour activer/désactiver cette option !"
               )
               .glow(breachers)
               .build()
         );
         double lavaPrice = spec == GuildSpecialization.WARLORD ? 1050.0 : (spec == GuildSpecialization.MERCHANT_SYNDICATE ? 1950.0 : 1500.0);
         inv.setItem(
            50,
            new ItemBuilder(Material.LAVA_BUCKET)
               .name(lavaResist ? "&6&l\ud83d\udd25 Ignifugation (Lave & Feu) : &a&lACTIVÉE" : "&7\ud83d\udd25 Ignifugation (Lave & Feu) : &c&lDÉSACTIVÉE")
               .lore(
                  "&7Immunise totalement vos monstres contre la lave et les flammes.",
                  "&7Ils traversent les douves de lave protectrices sans aucun dégât !",
                  "",
                  "&6• Surcoût : &e+" + String.format("%.2f", lavaPrice) + " pièces",
                  "",
                  "&e➤ Cliquez pour activer/désactiver cette option !"
               )
               .glow(lavaResist)
               .build()
         );
         if (totalUnits > 0) {
            boolean hasFunds = guild.getBankBalance() >= totalCost;
            inv.setItem(
               53,
               new ItemBuilder(hasFunds ? Material.EMERALD_BLOCK : Material.GOLD_BLOCK)
                  .name("&a&l⚔️ Lancer l'Invasion Personnalisée")
                  .lore(
                     "&7Unités prêtes : &e" + totalUnits + " monstres",
                     "&7Coût total : &6" + String.format("%.2f", totalCost) + " pièces",
                     "&7Banque de Guilde : &a" + String.format("%.2f", guild.getBankBalance()) + " pièces",
                     "",
                     hasFunds ? "&e➤ Cliquez pour sélectionner la guilde cible !" : "&c❌ Fonds insuffisants dans la banque de guilde !"
                  )
                  .glow(hasFunds)
                  .build()
            );
         } else {
            inv.setItem(53, new ItemBuilder(Material.REDSTONE_BLOCK).name("&c⚔️ Composition Vide").lore("&7Ajoutez au moins 1 unité pour attaquer.").build());
         }

         player.openInventory(inv);
      }
   }
}
