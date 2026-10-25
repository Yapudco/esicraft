package fr.esicraft.nexus;

import org.bukkit.Material;

public enum NexusTrapType {
   INFERNAL_FLAMES(
      "&c\ud83d\udd25 Brasier Infernal", Material.BLAZE_POWDER, 250.0, "&7Enflamme et inflige des brûlures continues aux ennemis à moins de 8 blocs."
   ),
   SLOWDOWN_WEB("&7\ud83d\udd78️ Toile d'Entravement", Material.COBWEB, 200.0, "&7Inflige Lenteur III et Cécité aux assaillants approchant du Nexus."),
   LIGHTNING_STRIKE("&e⚡ Foudre de Riposte", Material.LIGHTNING_ROD, 350.0, "&7Foudroie violemment quiconque ose frapper le Nexus (+15 dégâts réels)."),
   POISON_MIST(
      "&2\ud83e\uddea Brume Toxique", Material.DRAGON_BREATH, 300.0, "&7Émet un nuage toxique (Poison II) asphyxiant les pillards et monstres de horde."
   ),
   GUARDIAN_GOLEM(
      "&6\ud83e\udd16 Automate Gardien (Golem)", Material.IRON_BLOCK, 500.0, "&7Invoque un Golem de fer blindé loyal attaquant férocement tous les ennemis."
   );

   private final String displayName;
   private final Material icon;
   private final double baseCost;
   private final String description;

   private NexusTrapType(String displayName, Material icon, double baseCost, String description) {
      this.displayName = displayName;
      this.icon = icon;
      this.baseCost = baseCost;
      this.description = description;
   }

   public String getDisplayName() {
      return this.displayName;
   }

   public Material getIcon() {
      return this.icon;
   }

   public double getBaseCost() {
      return this.baseCost;
   }

   public String getDescription() {
      return this.description;
   }
}
