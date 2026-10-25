package fr.esicraft.gui;

import fr.esicraft.EsicraftPlugin;
import fr.esicraft.age.AgeType;
import fr.esicraft.gui.holder.AgeProgressHolder;
import fr.esicraft.guild.Guild;
import fr.esicraft.util.ChatUtil;
import fr.esicraft.util.ItemBuilder;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;

public class AgeProgressGUI {
   public static final String TITLE = "Âge de Guilde : Progression";

   public static void open(Player player, EsicraftPlugin plugin) {
      Guild guild = plugin.getGuildManager().getGuildByPlayer(player.getUniqueId());
      if (guild == null) {
         ChatUtil.sendMessage(player, "&cVous devez faire partie d'une guilde pour faire progresser les Âges (&e/guild create &cou &e/guild join&c).");
      } else {
         Inventory inv = Bukkit.createInventory(new AgeProgressHolder(), 54, ChatUtil.component("&8» &6Âge de Guilde : Progression"));
         ItemStack filler = new ItemBuilder(Material.GRAY_STAINED_GLASS_PANE).name(" ").build();

         for (int i = 0; i < 54; i++) {
            inv.setItem(i, filler);
         }

         AgeType currentAge = guild.getCurrentAge();
         AgeType nextAge = currentAge.getNextAge();
         int memberCount = Math.max(1, guild.getMembers().size());
         double mult = plugin.getAgeManager().getGuildMultiplier(guild);
         int multPct = (int)Math.round(mult * 100.0);
         List<String> statusLore = new ArrayList<>();
         statusLore.add("&7Niveau actuel : &e" + currentAge.getLevel() + "/6");
         statusLore.add("&7" + currentAge.getDescription());
         statusLore.add("");
         statusLore.add("&7Membres dans la guilde : &e" + memberCount);
         if (nextAge == AgeType.SUPREMACY) {
            statusLore.add("&7Échelle d'objectifs : &e100% &7(Quotas de reliques fixes)");
         } else if (memberCount > 1) {
            statusLore.add("&7Échelle d'objectifs : &6" + multPct + "% &7(+30%/j. jusqu'à 5, +50%/j. dès 6)");
         } else {
            statusLore.add("&7Échelle d'objectifs : &a100% (Base solo)");
         }

         statusLore.add("");
         statusLore.add(nextAge != null ? "&eProchain Âge : &a" + nextAge.getDisplayName() : "&a&lFÉLICITATIONS ! &7Âge Suprême Maximal Atteint !");
         inv.setItem(
            13,
            new ItemBuilder(Material.NETHER_STAR).name("&6&lGuilde " + guild.getName() + " : " + currentAge.getDisplayName()).lore(statusLore).glow().build()
         );
         AgeType[] allAges = AgeType.values();
         int[] ageSlots = new int[]{10, 11, 12, 14, 15, 16};

         for (int i = 0; i < allAges.length && i < ageSlots.length; i++) {
            AgeType a = allAges[i];
            boolean unlocked = a.getLevel() <= currentAge.getLevel();
            boolean isCurrent = a == currentAge;

            Material mat = switch (a) {
               case RUSTIC -> Material.OAK_LOG;
               case METALLURGY -> Material.IRON_INGOT;
               case ALCHEMY -> Material.BREWING_STAND;
               case TOTAL_WAR -> Material.DIAMOND_SWORD;
               case MYTHIC -> Material.NETHERITE_INGOT;
               case SUPREMACY -> Material.COMMAND_BLOCK;
            };
            ItemBuilder b = new ItemBuilder(mat)
               .name((unlocked ? "&a✔ " : "&c\ud83d\udd12 ") + a.getDisplayName())
               .lore("&7" + a.getDescription(), "", isCurrent ? "&e&l➤ ÂGE ACTUEL DE VOTRE GUILDE" : (unlocked ? "&aDébloqué" : "&cVerrouillé"));
            if (isCurrent) {
               b.glow();
            }

            inv.setItem(ageSlots[i], b.build());
         }

         if (nextAge != null) {
            Map<Material, Integer> reqs = plugin.getAgeManager().getRequirementsForGuild(guild, nextAge);
            List<Material> reqMats = new ArrayList<>(reqs.keySet());
            int count = reqMats.size();
            int[] allAvailableSlots = new int[]{19, 20, 21, 22, 23, 24, 25, 28, 29, 30, 31, 32, 33, 34, 37, 38, 39, 40, 41, 42, 43, 46, 47, 48, 49, 50, 51, 52};
            int[] targetSlots;
            if (count <= 7) {
               if (count == 1) {
                  targetSlots = new int[]{40};
               } else if (count == 2) {
                  targetSlots = new int[]{39, 41};
               } else if (count == 3) {
                  targetSlots = new int[]{38, 40, 42};
               } else if (count == 4) {
                  targetSlots = new int[]{38, 39, 41, 42};
               } else if (count == 5) {
                  targetSlots = new int[]{38, 39, 40, 41, 42};
               } else if (count == 6) {
                  targetSlots = new int[]{37, 38, 39, 41, 42, 43};
               } else {
                  targetSlots = new int[]{37, 38, 39, 40, 41, 42, 43};
               }
            } else {
               targetSlots = new int[Math.min(count, allAvailableSlots.length)];
               System.arraycopy(allAvailableSlots, 0, targetSlots, 0, targetSlots.length);
            }

            for (int i = 0; i < count && i < targetSlots.length; i++) {
               Material reqMat = reqMats.get(i);
               int requiredAmount = reqs.get(reqMat);
               int donatedAmount = guild.getDonatedAmount(reqMat);
               int pct = (int)((double)donatedAmount / (double)Math.max(1, requiredAmount) * 100.0);
               boolean completed = donatedAmount >= requiredAmount;
               String progressBar = getProgressBar(donatedAmount, requiredAmount);
               inv.setItem(
                  targetSlots[i],
                  new ItemBuilder(reqMat)
                     .name((completed ? "&a✔ &e" : "&e") + formatMaterialName(reqMat))
                     .lore(
                        "&7Progression : &f" + donatedAmount + " &7/ &6" + requiredAmount + " &7(" + Math.min(100, pct) + "%)",
                        progressBar,
                        "",
                        completed ? "&a✔ Objectif complété pour ce matériau !" : "&e➤ Cliquez avec cet item dans votre inventaire pour faire un don !"
                     )
                     .glow(completed)
                     .build()
               );
            }
         }

         player.openInventory(inv);
         ChatUtil.playSound(player, Sound.BLOCK_CHEST_OPEN, 1.0F, 1.2F);
      }
   }

   public static String formatMaterialName(Material mat) {
      if (mat == null) {
         return "Matériau";
      } else {
         String name = mat.name();
         if (name.startsWith("MUSIC_DISC_")) {
            return "Disque de Musique (" + name.replace("MUSIC_DISC_", "") + ")";
         } else if (name.endsWith("_SMITHING_TEMPLATE")) {
            return "Modèle de Forge (" + name.replace("_SMITHING_TEMPLATE", "").replace('_', ' ') + ")";
         } else if (name.endsWith("_POTTERY_SHERD")) {
            return "Tesson de Poterie (" + name.replace("_POTTERY_SHERD", "").replace('_', ' ') + ")";
         } else {
            return switch (mat) {
               case COPPER_INGOT -> "Lingots de Cuivre";
               case COPPER_BLOCK -> "Blocs de Cuivre";
               case LEATHER -> "Cuir (Tannerie)";
               case RAW_IRON -> "Fer Brut";
               case OAK_LOG -> "Bûches de Chêne";
               case WHEAT -> "Blé (Récolte Agricole)";
               case CARROT -> "Carottes (Maraîchage)";
               case FEATHER -> "Plumes (Élevage & Archerie)";
               case POTATO -> "Pommes de Terre (Rations)";
               case SUGAR_CANE -> "Cannes à Sucre (Papeterie & Alchimie)";
               case PUMPKIN -> "Citrouilles";
               case MELON_SLICE -> "Tranches de Pastèque";
               case BEETROOT -> "Betteraves (Agriculture)";
               case LAPIS_LAZULI -> "Lapis-Lazuli (Minage Magique)";
               case QUARTZ -> "Quartz du Nether";
               case BLAZE_ROD -> "Bâtons de Blaze (Forteresse)";
               case MAGMA_CREAM -> "Crème de Magma";
               case GHAST_TEAR -> "Larmes de Ghast";
               case GLOWSTONE_DUST -> "Poussière de Lumite (Glowstone)";
               case SPIDER_EYE -> "Yeux d'Araignée (Alchimie)";
               case IRON_INGOT -> "Lingots de Fer";
               case IRON_BLOCK -> "Blocs de Fer (Blindage Lourd)";
               case GOLD_INGOT -> "Lingots d'Or";
               case GOLD_BLOCK -> "Blocs d'Or Massif";
               case NETHER_WART -> "Verrues du Nether";
               case DIAMOND -> "Diamants";
               case DIAMOND_BLOCK -> "Blocs de Diamant";
               case EMERALD -> "Émeraudes (Trésor)";
               case EMERALD_BLOCK -> "Blocs d'Émeraude";
               case OBSIDIAN -> "Obsidienne";
               case CRYING_OBSIDIAN -> "Obsidienne Pleureuse";
               case ENDER_PEARL -> "Perles de l'End";
               case GOLDEN_CARROT -> "Carottes Dorées (Rations d'Élite)";
               case GOLDEN_APPLE -> "Pommes Dorées (Rations de Siège)";
               case HAY_BLOCK -> "Bottes de Foin (Fourrage)";
               case HONEY_BOTTLE -> "Bouteilles de Miel (Apiculture)";
               case SLIME_BALL -> "Boules de Slime";
               case REDSTONE -> "Poudre de Redstone";
               case REDSTONE_BLOCK -> "Blocs de Redstone";
               case REPEATER -> "Répéteurs de Redstone";
               case COMPARATOR -> "Comparateurs de Redstone";
               case PISTON -> "Pistons";
               case STICKY_PISTON -> "Pistons Collants";
               case DISPENSER -> "Distributeurs (Artillerie de Siège)";
               case OBSERVER -> "Observateurs";
               case HOPPER -> "Entonnoirs Industriels";
               case TARGET -> "Cibles de Redstone";
               case GUNPOWDER -> "Poudre à Canon (Munitions)";
               case TNT -> "Blocs de TNT (Armement de Siège)";
               case FIREWORK_ROCKET -> "Feux d'Artifice (Munitions Balistiques)";
               case ANVIL -> "Enclumes de Forge de Guerre";
               case ELYTRA -> "Élytres (Ailes de Vol)";
               case NETHERITE_INGOT -> "Lingots de Netherite Purs";
               case NETHERITE_SCRAP -> "Fragments de Netherite";
               case NETHERITE_BLOCK -> "Blocs de Netherite Massifs";
               case NETHER_STAR -> "Étoiles du Nether";
               case BEACON -> "Balises (Phares Célestes)";
               case TOTEM_OF_UNDYING -> "Totems d'Immortalité";
               case ECHO_SHARD -> "Éclats d'Écho (Cités Anciennes)";
               case SCULK_CATALYST -> "Catalyseurs de Sculk (Deep Dark)";
               case SCULK_SHRIEKER -> "Hurleurs de Sculk (Deep Dark)";
               case SCULK_SENSOR -> "Capteurs de Sculk";
               case DRAGON_BREATH -> "Souffles de Dragon";
               case ENCHANTED_GOLDEN_APPLE -> "Pommes Dorées Enchantées (Récolte Sacrée)";
               case CHORUS_FRUIT -> "Fruits de Chorus (Flore de l'End)";
               case POPPED_CHORUS_FRUIT -> "Fruits de Chorus Éclatés";
               case SHULKER_SHELL -> "Carapaces de Shulker";
               case END_STONE -> "Pierre de l'End";
               case PURPUR_BLOCK -> "Blocs de Purpur";
               case NETHERITE_HOE -> "Houe en Netherite (Mending + Unbreaking 3 + Fortune 3)";
               case DRAGON_EGG -> "Œuf d'Ender Dragon (Relique Légendaire)";
               case DEAD_BUSH -> "Buisson Mort Sacré";
               case GOAT_HORN -> "Cornes de Chèvre";
               case WITHER_SKELETON_SKULL -> "Crâne de Wither Squelette";
               case ZOMBIE_HEAD -> "Tête de Zombie";
               case SKELETON_SKULL -> "Crâne de Squelette";
               case DRAGON_HEAD -> "Tête d'Ender Dragon";
               case CREEPER_HEAD -> "Tête de Creeper";
               case PIGLIN_HEAD -> "Tête de Piglin";
               default -> mat.name().replace('_', ' ');
            };
         }
      }
   }

   private static String getProgressBar(int current, int max) {
      double pct = (double)current / (double)Math.max(1, max);
      int total = 10;
      int green = (int)(pct * (double)total);
      return "&a" + "■".repeat(Math.max(0, Math.min(total, green))) + "&7" + "■".repeat(Math.max(0, total - green));
   }
}
