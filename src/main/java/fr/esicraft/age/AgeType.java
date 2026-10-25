package fr.esicraft.age;

public enum AgeType {
   RUSTIC(1, "&6Âge Rustique", "&7Outils bois/pierre uniquement. Débuts de la civilisation (Nether & End verrouillés).", 0),
   METALLURGY(2, "&fÂge de la Métallurgie", "&7Outils en fer pour tous, diamant réservé aux métiers spécialisés. Accès au Nether.", 1),
   ALCHEMY(3, "&dÂge de l'Alchimie", "&7Diamant pour tous, Netherite réservée aux métiers spécialisés. Accès à l'End.", 2),
   TOTAL_WAR(4, "&bÂge de Guerre Totale", "&7Batailles de siège lourdes, alambics et redstone avancée pour tous.", 3),
   MYTHIC(5, "&5Âge Mythique & Apex", "&7Technologies ultimes et démocratisation de la Netherite pour tous.", 4),
   SUPREMACY(6, "&eÂge de Suprématie", "&7Suprématie totale, reliques sacrées et maîtrise absolue de l'univers.", 5);

   private final int level;
   private final String displayName;
   private final String description;
   private final int index;

   private AgeType(int level, String displayName, String description, int index) {
      this.level = level;
      this.displayName = displayName;
      this.description = description;
      this.index = index;
   }

   public int getLevel() {
      return this.level;
   }

   public String getDisplayName() {
      return this.displayName;
   }

   public String getDescription() {
      return this.description;
   }

   public int getIndex() {
      return this.index;
   }

   public int getMaxClaims() {
      return switch (this) {
         case RUSTIC -> 16;
         case METALLURGY -> 32;
         case ALCHEMY -> 64;
         case TOTAL_WAR -> 128;
         case MYTHIC, SUPREMACY -> Integer.MAX_VALUE;
      };
   }

   public static AgeType fromLevel(int level) {
      for (AgeType type : values()) {
         if (type.getLevel() == level) {
            return type;
         }
      }

      return RUSTIC;
   }

   public AgeType getNextAge() {
      return this == SUPREMACY ? null : fromLevel(this.level + 1);
   }
}
