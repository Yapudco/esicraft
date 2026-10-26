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
   ),
   SHULKER_SHOT("&d🟪 Tir de Shulker", Material.SHULKER_SHELL, 0.0, "&7Projectile guidé : dégâts et lévitation sur l'ennemi le plus proche (toutes les 6 s)."),
   WARDEN_SHOT("&3👁 Tir du Warden", Material.ECHO_SHARD, 0.0, "&7Onde sonique dévastatrice (10 dégâts, repoussée) toutes les 15 s."),
   EVOKER_SHOT("&5🪄 Tir d'Évocateur", Material.TOTEM_OF_UNDYING, 0.0, "&7Crocs magiques : 6 dégâts de zone sur la cible toutes les 9 s."),
   GUARDIAN_SHOT("&b🐡 Tir de Gardien", Material.PRISMARINE_CRYSTALS, 0.0, "&7Rayon aquatique : dégâts et lenteur toutes les 4 s."),
   ELDER_GUARDIAN_SHOT("&9🌊 Malédiction de l'Ancien", Material.SPONGE, 0.0, "&7Fatigue de minage III et dégâts à tous les ennemis proches toutes les 25 s."),
   BLAZE_SHOT("&6🔥 Tir de Blaze", Material.BLAZE_ROD, 0.0, "&7Jusqu'à 3 boules de feu enflammant les ennemis toutes les 5 s."),
   GHAST_SHOT("&f👻 Tir de Ghast", Material.GHAST_TEAR, 0.0, "&7Boule de feu explosive de zone (8 dégâts, sans casser de blocs) toutes les 14 s."),
   BEE_SWARM("&e🐝 Essaim d'abeilles", Material.HONEYCOMB, 0.0, "&7Invoque jusqu'à 6 abeilles enragées qui piquent les ennemis (respawn toutes les 30 s).");

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
