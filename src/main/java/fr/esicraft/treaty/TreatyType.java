package fr.esicraft.treaty;

import java.util.List;
import org.bukkit.Material;

/** Types de traités entre guildes. */
public enum TreatyType {
   ALLIANCE("Alliance", Material.EMERALD,
      List.of("&7Aucun raid entre les deux guildes.", "&7Passage ferroviaire &agratuit &7sur les terres de l'autre.", "&7Pas de dégâts entre joueurs alliés.")),
   PACT("Pacte de non-agression", Material.PAPER,
      List.of("&7Aucun raid entre les deux guildes", "&7pendant la durée du pacte.")),
   VASSALAGE("Vassalité", Material.GOLD_BLOCK,
      List.of("&7Aucun raid entre suzerain et vassal.", "&7Le vassal verse un &6tribut quotidien &7au suzerain.", "&7Passage ferroviaire gratuit et pas de dégâts entre joueurs."));

   private final String label;
   private final Material icon;
   private final List<String> effects;

   TreatyType(String label, Material icon, List<String> effects) {
      this.label = label;
      this.icon = icon;
      this.effects = effects;
   }

   public String getLabel() {
      return this.label;
   }

   public Material getIcon() {
      return this.icon;
   }

   public List<String> getEffects() {
      return this.effects;
   }

   /** Alliance et vassalité unissent les joueurs (pas de dégâts, passage gratuit). */
   public boolean isFriendly() {
      return this == ALLIANCE || this == VASSALAGE;
   }
}
