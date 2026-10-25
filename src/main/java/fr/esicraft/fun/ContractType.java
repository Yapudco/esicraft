package fr.esicraft.fun;

/** Types de contrats quotidiens. {@code perUnit} est la prime versée par unité accomplie. */
public enum ContractType {
   MINE_ORE("Extraire {n} minerais", 4.0, 40, 80),
   HARVEST("Récolter {n} cultures mûres", 1.5, 80, 150),
   KILL("Éliminer {n} monstres", 8.0, 15, 30),
   KILL_RANGED("Éliminer {n} monstres à distance", 10.0, 12, 24),
   SMELT("Fondre {n} lingots", 4.0, 40, 90),
   ENCHANT("Enchanter {n} objets", 60.0, 3, 6),
   BUILD_REDSTONE("Poser {n} composants de redstone", 3.0, 40, 80);

   private final String template;
   private final double perUnit;
   private final int minTarget;
   private final int maxTarget;

   ContractType(String template, double perUnit, int minTarget, int maxTarget) {
      this.template = template;
      this.perUnit = perUnit;
      this.minTarget = minTarget;
      this.maxTarget = maxTarget;
   }

   public String describe(int target) {
      return this.template.replace("{n}", String.valueOf(target));
   }

   public double getPerUnit() {
      return this.perUnit;
   }

   public int getMinTarget() {
      return this.minTarget;
   }

   public int getMaxTarget() {
      return this.maxTarget;
   }
}
