package fr.esicraft.race;

import java.util.List;
import org.bukkit.Material;

public enum Race {
   NONE(
      "&7Sans Métier",
      Material.LEATHER_HELMET,
      "&7Aucune spécialisation choisie.",
      List.of("&c• Impossible de miner ou casser des blocs sans métier !"),
      List.of("&c- Choisissez un métier avec /race pour jouer."),
      20.0
   ),
   DWARF(
      "&6Mineur (Nain)",
      Material.DIAMOND_PICKAXE,
      "&7Extraction et excavation souterraine.",
      List.of("&e• Pioches (Fer A1, Dia A2, Neth A3)", "&e• Pelles (Dia A2, Neth A3)"),
      List.of("&a+ Haste 2 & Night Vision (Y<=0)", "&a+ 15% double minage", "&c- Lenteur 1 en plein air (Y>60)", "&c- Ni armes ni armures fer/dia"),
      20.0
   ),
   SYLVAN(
      "&aSylvain (Elfe)",
      Material.WHEAT,
      "&7Agriculture et élevage accéléré.",
      List.of("&e• Houes (Fer A1, Dia A2, Neth A3)", "&e• Poudre d'os (A1-A2)", "&e• Pommes Dorées (A1-A3)"),
      List.of("&a+ Aura Régen 1 (4 blocs)", "&a+ Récolte clic-droit", "&c- -20% dégâts au corps-à-corps"),
      20.0
   ),
   BLACKSMITH(
      "&cForgeron de Guerre",
      Material.ANVIL,
      "&7Forge lourde, armures et épées.",
      List.of("&e• Armures & Épées fer (Âge 1)", "&e• Armures & Épées diamant (Âge 2)", "&e• Armures Netherite (Âges 3-4)", "&e• Enclumes & Hauts-fourneaux"),
      List.of("&a+ Résistance au Feu permanente", "&a+ +35% lingots fondus", "&a+ -50% coût XP sur l'enclume", "&c- Faim accrue (+25%)"),
      20.0
   ),
   RANGER(
      "&2Rôdeur & Chasseur",
      Material.BOW,
      "&7Tir à l'arc et traque à distance.",
      List.of("&e• Arcs & Arbalètes (Âges 1-3)"),
      List.of("&a+ Flèches de Poison 1", "&a+ +25% dégâts à distance", "&a+ Vitesse 1 en forêt/plaine", "&c- Lenteur si plastron lourd"),
      18.0
   ),
   ARCANIST(
      "&dArcaniste & Alchimiste",
      Material.BREWING_STAND,
      "&7Magie, potions et enchantements.",
      List.of("&e• Enchants & Livres (Âges 1-3)", "&e• Splash Potions exclusives", "&e• Alambics (Âges 1-2)", "&e• Pommes Dorées (Âges 1-3)"),
      List.of("&a+ +30% d'XP reçue", "&a+ Durée potions +25%", "&a+ -20% dégâts magiques", "&c- +15% dégâts en mêlée subis"),
      16.0
   ),
   ENGINEER(
      "&6Ingénieur & Bâtisseur",
      Material.REDSTONE,
      "&7Redstone, explosifs et pièges.",
      List.of("&e• Pistons, Répéteurs, Comparateurs", "&e• Observateurs, Entonnoirs", "&e• Distributeurs & TNT (Âges 1-3)"),
      List.of("&a+ -40% dégâts d'explosions", "&a+ Maîtrise exclusive redstone/TNT", "&c- -20% dégâts corps-à-corps"),
      20.0
   );

   private final String displayName;
   private final Material icon;
   private final String description;
   private final List<String> exclusiveCrafts;
   private final List<String> perksAndDrawbacks;
   private final double baseHealth;

   private Race(String displayName, Material icon, String description, List<String> exclusiveCrafts, List<String> perksAndDrawbacks, double baseHealth) {
      this.displayName = displayName;
      this.icon = icon;
      this.description = description;
      this.exclusiveCrafts = exclusiveCrafts;
      this.perksAndDrawbacks = perksAndDrawbacks;
      this.baseHealth = baseHealth;
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

   public List<String> getExclusiveCrafts() {
      return this.exclusiveCrafts;
   }

   public List<String> getPerksAndDrawbacks() {
      return this.perksAndDrawbacks;
   }

   public double getBaseHealth() {
      return this.baseHealth;
   }
}
