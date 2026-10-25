package fr.esicraft.raid;

import org.bukkit.Material;
import org.bukkit.entity.EntityType;

public enum RaidType {
   ARMORED_ZOMBIES(
      "&2\ud83e\udddf Horde de Zombies Cuirassés",
      Material.ZOMBIE_HEAD,
      "&7Zombies équipés de casques et plastrons en fer ciblant le Nexus.",
      EntityType.ZOMBIE,
      24,
      2500.0,
      100
   ),
   INCENDIARY_SKELETONS(
      "&f\ud83c\udff9 Légion Squelette Incendiaire",
      Material.SKELETON_SKULL,
      "&7Squelettes aux flèches enflammées pour incendier les défenses.",
      EntityType.SKELETON,
      20,
      3500.0,
      140
   ),
   TOXIC_SPIDERS(
      "&c\ud83d\udd77️ Essaim d'Araignées Venimeuses",
      Material.SPIDER_EYE,
      "&7Araignées venimeuses rapides sautant par-dessus les murailles.",
      EntityType.CAVE_SPIDER,
      30,
      3000.0,
      80
   ),
   PILLAGER_SIEGE(
      "&4\ud83d\udc7a Siège de Pillards & Ravageurs",
      Material.CROSSBOW,
      "&7Attaque de siège lourde avec Ravageurs et Vindicators d'élite.",
      EntityType.PILLAGER,
      18,
      7500.0,
      320
   ),
   NETHER_DEMONS(
      "&8\ud83d\udd25 Fléau Démoniaque du Nether",
      Material.WITHER_SKELETON_SKULL,
      "&7Squelettes du Wither et Démons ardents armés d'épées en pierre maudite.",
      EntityType.WITHER_SKELETON,
      16,
      6000.0,
      240
   ),
   PHANTOM_SWARM(
      "&9\ud83e\udd87 Volée de Monstres Aériens",
      Material.PHANTOM_MEMBRANE,
      "&7Nuée de Phantoms agressifs fondant depuis les cieux sur le Nexus.",
      EntityType.PHANTOM,
      18,
      5000.0,
      180
   ),
   CREEPER_BLITZ(
      "&a\ud83d\udca5 Commando Kamikaze Express",
      Material.CREEPER_HEAD,
      "&7Creepers survoltés dotés d'une vitesse accrue fonçant vers le cœur.",
      EntityType.CREEPER,
      12,
      5500.0,
      280
   ),
   ENDER_INCURSION(
      "&5\ud83d\udd2e Incursion de l'End",
      Material.ENDER_PEARL,
      "&7Endermen d'élite infligeant de lourds dégâts d'annihilation.",
      EntityType.ENDERMAN,
      10,
      9000.0,
      360
   );

   private final String displayName;
   private final Material icon;
   private final String description;
   private final EntityType primaryMobType;
   private final int mobCount;
   private final double basePrice;
   private final int damageToNexus;

   private RaidType(String displayName, Material icon, String description, EntityType primaryMobType, int mobCount, double basePrice, int damageToNexus) {
      this.displayName = displayName;
      this.icon = icon;
      this.description = description;
      this.primaryMobType = primaryMobType;
      this.mobCount = mobCount;
      this.basePrice = basePrice;
      this.damageToNexus = damageToNexus;
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

   public EntityType getPrimaryMobType() {
      return this.primaryMobType;
   }

   public int getMobCount() {
      return this.mobCount;
   }

   public double getBasePrice() {
      return this.basePrice;
   }

   public int getDamageToNexus() {
      return this.damageToNexus;
   }
}
