package fr.esicraft.rail;

import org.bukkit.Material;

/** Relation d'une guilde (propriétaire du territoire) envers une autre guilde pour le passage sur ses rails. */
public enum Relation {
   NORMAL("&fNormale", "Le péage de votre guilde s'applique.", Material.WHITE_BANNER),
   FREE("&aAlliée (gratuit)", "Passage gratuit sur vos terres.", Material.LIME_BANNER),
   BANNED("&cBannie", "Passage interdit : les lignes qui traversent vos terres sont coupées.", Material.RED_BANNER);

   private final String label;
   private final String description;
   private final Material icon;

   Relation(String label, String description, Material icon) {
      this.label = label;
      this.description = description;
      this.icon = icon;
   }

   public String getLabel() {
      return this.label;
   }

   public String getDescription() {
      return this.description;
   }

   public Material getIcon() {
      return this.icon;
   }

   public Relation next() {
      Relation[] all = values();
      return all[(this.ordinal() + 1) % all.length];
   }
}
