package fr.esicraft.nexus;

import org.bukkit.Material;

public enum NexusTrapType {
   INFERNAL_FLAMES(
      "&c\ud83d\udd25 Brasier Infernal", Material.BLAZE_POWDER, 250.0, "&7Enflamme les intrus et monstres à moins de 8 blocs (1 cœur toutes les 2 s + brûlure)."
   ),
   SLOWDOWN_WEB("&7\ud83d\udd78️ Toile d'Entravement", Material.COBWEB, 200.0, "&7Inflige Lenteur II aux assaillants à moins de 10 blocs du Nexus."),
   LIGHTNING_STRIKE("&e⚡ Foudre de Riposte", Material.LIGHTNING_ROD, 350.0, "&7Foudroie quiconque frappe le Nexus (4 cœurs, au plus un éclair toutes les 5 s par joueur)."),
   POISON_MIST(
      "&2\ud83e\uddea Brume Toxique", Material.DRAGON_BREATH, 300.0, "&7Émet un nuage toxique (Poison I) à moins de 8 blocs ; le poison ne tue jamais, il affaiblit."
   ),
   GUARDIAN_GOLEM(
      "&6\ud83e\udd16 Automate Gardien (Golem)", Material.IRON_BLOCK, 500.0, "&7Invoque un Golem de fer qui attaque les ennemis ; reconstruit 5 min après sa mort."
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
