package fr.esicraft.life;

/** Succès débloquables. {@code notable} : annoncé dans le journal de guerre quel que soit son taux d'obtention. */
public enum Achievement {
   FIRST_JOB("🎭", "Vocation", "Choisir son premier métier", false),
   FOUNDER("🏰", "Fondateur", "Fonder une guilde", false),
   RECRUIT("🤝", "Frères d'armes", "Rejoindre une guilde", false),
   FIRST_CLAIM("🗺", "Premier arpent", "Revendiquer un territoire", false),
   NEXUS("💎", "Cœur battant", "Poser le Nexus de sa guilde", false),
   AGE_2("⚙", "Métallurgiste", "Atteindre l'Âge 2 avec sa guilde", false),
   AGE_4("🔥", "Seigneur de guerre", "Atteindre l'Âge 4 avec sa guilde", false),
   AGE_6("👑", "Suprématie", "Atteindre l'Âge 6 avec sa guilde", true),
   RAIDER("💀", "Chef de horde", "Lancer une horde contre une autre guilde", false),
   DEFENDER("🛡", "Rempart", "Repousser une horde en défendant son territoire", false),
   CONQUEROR("💥", "Conquérant", "Détruire le Nexus d'une autre guilde", true),
   DIPLOMAT("📜", "Diplomate", "Signer un traité", false),
   TRAITOR("💔", "Traître", "Rompre un traité", true),
   TRAVELER("🚆", "Voyageur", "Prendre le train", false),
   STATIONMASTER("🚉", "Chef de gare", "Construire une gare", false),
   CONTRACTOR("📋", "Travailleur", "Accomplir les 3 contrats du jour", false),
   TEAMWORK("🧩", "Esprit d'équipe", "Participer à un contrat de guilde accompli", false),
   HUNTER("🏹", "Chasseur", "Éliminer 500 monstres", false),
   DUELIST("🗡", "Duelliste", "Tuer un joueur d'une autre guilde", false),
   SLAYER("☠", "Tueur de boss", "Être le meilleur combattant contre le Seigneur des Cendres", true),
   RICH("💰", "Fortuné", "Posséder 10 000 pièces", false),
   TYCOON("🏦", "Magnat", "Posséder 100 000 pièces", true),
   SPY("🕵", "Maître espion", "Se déguiser en membre d'une autre guilde", false),
   SAVIOR("🆘", "Sauveur", "Répondre à l'appel à l'aide d'un allié et repousser la horde", false),
   FOUR_SEASONS("🍂", "Quatre saisons", "Jouer pendant les quatre saisons naturelles", false),
   CHAMPION("🏆", "Champion", "Gagner une saison avec sa guilde", true);

   private final String icon;
   private final String title;
   private final String description;
   private final boolean notable;

   Achievement(String icon, String title, String description, boolean notable) {
      this.icon = icon;
      this.title = title;
      this.description = description;
      this.notable = notable;
   }

   public String getIcon() {
      return this.icon;
   }

   public String getTitle() {
      return this.title;
   }

   public String getDescription() {
      return this.description;
   }

   public boolean isNotable() {
      return this.notable;
   }

   public static Achievement parse(String id) {
      try {
         return Achievement.valueOf(id);
      } catch (RuntimeException e) {
         return null;
      }
   }
}
