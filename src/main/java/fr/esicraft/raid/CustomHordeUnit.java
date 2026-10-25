package fr.esicraft.raid;

import org.bukkit.Material;
import org.bukkit.entity.EntityType;

public enum CustomHordeUnit {
   ZOMBIE("&2Zombie Cuirassé", Material.ZOMBIE_HEAD, EntityType.ZOMBIE, 150.0, 60, "&7Fantassin lourd équipé d'une armure en fer."),
   SKELETON("&fSquelette Archer", Material.SKELETON_SKULL, EntityType.SKELETON, 200.0, 72, "&7Tireur d'élite à distance harcelant les défenses."),
   SPIDER("&cAraignée Venimeuse", Material.SPIDER_EYE, EntityType.CAVE_SPIDER, 220.0, 64, "&7Créature rapide franchissant les obstacles et empoisonnant."),
   PILLAGER("&7Pillard Arbalétrier", Material.CROSSBOW, EntityType.PILLAGER, 300.0, 100, "&7Tireur perçant avec des tirs d'arbalète lourds."),
   VINDICATOR("&4Vindicateur Brutal", Material.IRON_AXE, EntityType.VINDICATOR, 450.0, 140, "&7Guerrier d'assaut fendant les armures à la hache."),
   WITCH("&5Sorcière Alchimiste", Material.POTION, EntityType.WITCH, 550.0, 160, "&7Lanceuse de potions nocives (poison, lenteur, dégâts instantanés)."),
   WITHER_SKELETON(
      "&8Squelette du Wither",
      Material.WITHER_SKELETON_SKULL,
      EntityType.WITHER_SKELETON,
      600.0,
      200,
      "&7Démon du Nether infligeant l'effet de décomposition Wither."
   ),
   RAVAGER("&6Ravageur Monstrueux", Material.SADDLE, EntityType.RAVAGER, 2000.0, 480, "&7Bête colossale de siège écrasant tout sur son passage.");

   private final String displayName;
   private final Material icon;
   private final EntityType entityType;
   private final double costPerUnit;
   private final int damageToNexus;
   private final String description;

   private CustomHordeUnit(String displayName, Material icon, EntityType entityType, double costPerUnit, int damageToNexus, String description) {
      this.displayName = displayName;
      this.icon = icon;
      this.entityType = entityType;
      this.costPerUnit = costPerUnit;
      this.damageToNexus = damageToNexus;
      this.description = description;
   }

   public String getDisplayName() {
      return this.displayName;
   }

   public Material getIcon() {
      return this.icon;
   }

   public EntityType getEntityType() {
      return this.entityType;
   }

   public double getCostPerUnit() {
      return this.costPerUnit;
   }

   public int getDamageToNexus() {
      return this.damageToNexus;
   }

   public String getDescription() {
      return this.description;
   }
}
