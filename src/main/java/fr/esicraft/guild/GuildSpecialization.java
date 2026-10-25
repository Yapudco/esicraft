package fr.esicraft.guild;

import fr.esicraft.race.Race;
import java.util.List;
import java.util.Map;
import org.bukkit.Material;

public enum GuildSpecialization {
   NONE(
      "&7Non Spécialisée",
      Material.SHIELD,
      "&7Aucune orientation choisie pour la guilde.",
      List.of("&7Aucun bonus de claim."),
      List.of("&7Aucune contrainte de composition.")
   ),
   WARLORD(
      "&c⚔️ Légion Guerrière",
      Material.NETHERITE_SWORD,
      "&7Domination martiale & conquête.",
      List.of("&a+ Force 1 & Résistance 1 en claim", "&a+ Hordes mercenaires à -30%", "&a+ +30% dégâts vs monstres/hordes", "&a+ Pillage aux âges supérieurs"),
      List.of("&c- Max 1 Nain et 1 Sylvain", "&c- Min 50% Rôdeurs / Forgerons", "&c- -25% récoltes & minerais")
   ),
   UNDERGROUND_INDUSTRY(
      "&6⛏️ Industrie Souterraine",
      Material.RAW_IRON_BLOCK,
      "&7Extraction industrielle lourde.",
      List.of("&a+ Haste 2 en claim sous Y=30", "&a+ Minerais bruts x1.5", "&a+ Pièges & Golems Nexus à -50%"),
      List.of("&c- Max 1 Rôdeur", "&c- Potions niveau 2 interdites", "&c- +20% dégâts monstres magiques")
   ),
   DRUIDIC_ENCLAVE(
      "&a\ud83c\udf43 Enclave Champêtre",
      Material.HAY_BLOCK,
      "&7Harmonie et autosuffisance.",
      List.of(
         "&a+ Santé max : +2 cœurs (+4 PV)",
         "&a+ Régen 1 & Saturation en claim",
         "&a+ Double récolte champs & animaux",
         "&a+ Bois & pierres décoratives x2",
         "&a+ Reproduction animale x3"
      ),
      List.of("&c- Pas d'Ingénieur autorisant TNT", "&c- Max 1 Forgeron (Armures fer max)", "&c- Armures Netherite interdites")
   ),
   MERCHANT_SYNDICATE(
      "&e\ud83d\udcb0 Syndicat Marchand",
      Material.GOLD_BLOCK,
      "&7Monopole économique & commerce.",
      List.of("&a+ Commerce exclusif Villageois", "&a+ Primes monstres +20% (+10% clan)", "&a+ Aucune taxe à l'Hôtel des Ventes", "&a+ Dividendes de guilde"),
      List.of("&c- 0 Arcaniste autorisé", "&c- Hordes mercenaires à +30%", "&c- Aucun bonus de combat", "&c- Nexus : -20% PV max")
   ),
   ARCANE_CONCLAVE(
      "&d\ud83d\udd2e Conclave Arcanique",
      Material.ENCHANTING_TABLE,
      "&7Magie et arcanes défensives.",
      List.of(
         "&a+ Nexus : +25% PV max",
         "&a+ Enchantements & enclumes : -50% XP",
         "&a+ Potions améliorées (+1 tier)",
         "&a+ Enchants surpuissants (IV➔V, V➔VI)",
         "&a+ Rayon magique de Nexus (1 cœur / 2 s)",
         "&a+ -50% dégâts magiques subis"
      ),
      List.of("&c- Santé max : -2 cœurs (-4 PV)", "&c- Coût des claims : +50%", "&c- Max 1 Forgeron et 1 Nain", "&c- Perte des enchants V-VI si quit")
   ),
   LONE_RANGER(
      "&2\ud83c\udff9 Rôdeur Solitaire",
      Material.BOW,
      "&7Voie solitaire de chasseur d'élite.",
      List.of("&a+ Vitesse 1 permanent", "&a+ Armures Netherite dès l'Âge 3", "&a+ Enchants & Livres dès l'Âge 3"),
      List.of(
         "&c- Strictement 1 seul membre (Rôdeur)",
         "&c- N'avoir jamais rejoint d'autre guilde",
         "&c- Aucun coffre dans les claims",
         "&c- Nexus réduit à 2 500 PV max"
      )
   );

   private final String displayName;
   private final Material icon;
   private final String description;
   private final List<String> perks;
   private final List<String> restrictions;

   private GuildSpecialization(String displayName, Material icon, String description, List<String> perks, List<String> restrictions) {
      this.displayName = displayName;
      this.icon = icon;
      this.description = description;
      this.perks = perks;
      this.restrictions = restrictions;
   }

   public String getDisplayName() {
      return this.displayName;
   }

   public Material getIcon() {
      return this.icon;
   }

   public String getDescription() {
      return this.description;
   }

   public List<String> getPerks() {
      return this.perks;
   }

   public List<String> getRestrictions() {
      return this.restrictions;
   }

   public boolean validateComposition(Map<Race, Integer> raceCounts, int totalMembers) {
      if (this == NONE) {
         return true;
      } else if (totalMembers <= 0) {
         return true;
      } else {
         int dwarfCount = raceCounts.getOrDefault(Race.DWARF, 0);
         int sylvanCount = raceCounts.getOrDefault(Race.SYLVAN, 0);
         int blacksmithCount = raceCounts.getOrDefault(Race.BLACKSMITH, 0);
         int rangerCount = raceCounts.getOrDefault(Race.RANGER, 0);
         int engineerCount = raceCounts.getOrDefault(Race.ENGINEER, 0);
         int arcanistCount = raceCounts.getOrDefault(Race.ARCANIST, 0);
         switch (this) {
            case WARLORD:
               if (dwarfCount > 1 || sylvanCount > 1) {
                  return false;
               }

               // Min 50% de Rôdeurs / Forgerons (dès 2 membres)
               if (totalMembers >= 2 && (rangerCount + blacksmithCount) * 2 < totalMembers) {
                  return false;
               }
               break;
            case UNDERGROUND_INDUSTRY:
               if (rangerCount > 1) {
                  return false;
               }
               break;
            case DRUIDIC_ENCLAVE:
               if (engineerCount > 0 || blacksmithCount > 1) {
                  return false;
               }
               break;
            case MERCHANT_SYNDICATE:
               if (arcanistCount > 0) {
                  return false;
               }
               break;
            case ARCANE_CONCLAVE:
               if (blacksmithCount > 1 || dwarfCount > 1) {
                  return false;
               }
               break;
            case LONE_RANGER:
               if (totalMembers > 1 || rangerCount != 1) {
                  return false;
               }
         }

         return true;
      }
   }
}
