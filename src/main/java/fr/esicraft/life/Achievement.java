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
   CHAMPION("🏆", "Champion", "Gagner une saison avec sa guilde", true),
   CLAIMS_16("🗺", "Arpenteur", "Posséder 16 chunks revendiqués avec sa guilde", false),
   CLAIMS_32("🗺", "Cartographe", "Posséder 32 chunks revendiqués avec sa guilde", false),
   CLAIMS_64("🗺", "Bâtisseur de frontières", "Posséder 64 chunks revendiqués avec sa guilde", false),
   CLAIMS_128("🗺", "Seigneur foncier", "Posséder 128 chunks revendiqués avec sa guilde", true),
   CLAIMS_256("🗺", "Baron des terres", "Posséder 256 chunks revendiqués avec sa guilde", true),
   CLAIMS_512("🗺", "Duc des provinces", "Posséder 512 chunks revendiqués avec sa guilde", true),
   CLAIMS_1024("🗺", "Empereur des horizons", "Posséder 1024 chunks revendiqués avec sa guilde", true),
   CLAIMS_2048("🗺", "Maître du monde", "Posséder 2048 chunks revendiqués avec sa guilde", true),
   HEROIC_SLAYER("☠", "Légende héroïque", "Être le meilleur combattant contre un boss héroïque", true),
   DUEL_MASTER("🗡", "Maître d'armes", "Remporter 10 duels d'honneur", false),
   NEXUS_GAMBLER("🎰", "Tout pour le tout", "Gagner la roulette russe du Nexus", true),
   TECH_MASTER("🔬", "Archi-technicien", "Débloquer toutes les défenses de l'arbre technologique du Nexus", true),
   SIEGE_VICTOR("🏴", "Maître du siège", "Remporter une guerre formelle (siège)", true),
   SHAREHOLDER("📈", "Actionnaire", "Posséder des parts d'une guilde cotée en bourse", false),
   FREIGHT_RUNNER("🚂", "Roi du fret", "Livrer un train de fret à destination", false),
   CONVOY_RAIDER("🏹", "Pilleur de convois", "Piller le train de fret d'une autre guilde", true),
   CHAMPION_TAVERN("🍺", "Tavernier", "Déclarer une taverne (zone neutre)", false);

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
