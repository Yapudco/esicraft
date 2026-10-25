package fr.esicraft.web;

import fr.esicraft.life.Chronicle;
import java.util.List;
import java.util.Map;

/** Données publiques supplémentaires (journal, raids, diplomatie, courbes, Âges, succès, profils), capturées avec l'instantané. */
record WebExtra(List<JournalView> journal, List<RaidView> raids, List<EdgeView> edges, Map<String, List<Chronicle.Point>> curves,
                List<Chronicle.AgeMark> ages, List<AchievementView> achievements, List<UnlockView> recentUnlocks,
                Map<String, List<TrophyView>> trophies, List<ProfileView> profiles, NatureView nature, Map<String, Integer> diversity,
                int knownPlayers) {

   record JournalView(long ts, String icon, String kind, String text, String guildA, String guildB) {
   }

   record RaidView(String targetId, String targetName, String attackerName, String type, int mobsLeft, int mobsTotal, long startedAt,
                   int defenders, Integer nexusPercent) {
   }

   /** kind : ALLIANCE, PACT, VASSALAGE ou RIVALRY (poids = 3 × raids + joueurs tués). */
   record EdgeView(String a, String b, String kind, int weight, int raids, int kills) {
   }

   record AchievementView(String id, String icon, String title, String description, int holders, double percent, boolean rare) {
   }

   record UnlockView(long ts, String player, String icon, String title, boolean rare) {
   }

   record TrophyView(String icon, String title, String detail) {
   }

   record ProfileView(String key, String name, String race, String guildId, String guildName, String role, String age, Double coins,
                      boolean online, List<String> achievements, Map<String, Long> stats) {
   }

   record NatureView(String icon, String label, long daysLeft, List<String> effects, String next) {
   }

   static WebExtra empty() {
      return new WebExtra(List.of(), List.of(), List.of(), Map.of(), List.of(), List.of(), List.of(), Map.of(), List.of(), null, Map.of(), 1);
   }

   ProfileView profile(String key) {
      for (ProfileView p : this.profiles) {
         if (p.key().equals(key)) {
            return p;
         }
      }

      return null;
   }
}
