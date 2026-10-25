package fr.esicraft.life;

/** Les quatre saisons naturelles, chacune avec ses effets sur le monde. */
public enum NatureSeason {
   SPRING("🌸", "Printemps", "&a", new String[]{"Les cultures poussent plus vite (+50 %)."}),
   SUMMER("☀", "Été", "&e", new String[]{"+20 % d'expérience pour tous."}),
   AUTUMN("🍂", "Automne", "&6", new String[]{"Récoltes abondantes : une chance sur deux de récolte bonus."}),
   WINTER("❄", "Hiver", "&b", new String[]{"Les cultures poussent deux fois moins vite.", "Primes de monstres +25 % (la chasse nourrit)."});

   private final String icon;
   private final String label;
   private final String color;
   private final String[] effects;

   NatureSeason(String icon, String label, String color, String[] effects) {
      this.icon = icon;
      this.label = label;
      this.color = color;
      this.effects = effects;
   }

   public String getIcon() {
      return this.icon;
   }

   public String getLabel() {
      return this.label;
   }

   public String getColor() {
      return this.color;
   }

   public String[] getEffects() {
      return this.effects;
   }

   public String display() {
      return this.icon + " " + this.label;
   }
}
