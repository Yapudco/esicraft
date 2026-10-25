package fr.esicraft.web;

import java.util.List;

/** Instantané immuable des données publiques : le serveur HTTP ne lit jamais directement les objets du jeu. */
public record WebSnapshot(long generatedAt, String season, long daysLeft, String event, int onlineCount, int maxPlayers,
                          List<GuildView> guilds, List<OnlineView> online, List<RichView> rich, List<String> history) {

   public record MemberView(String name, String role, String race, boolean online) {
   }

   public record GuildView(String id, String name, String tag, String specialization, String age, int ageLevel, List<MemberView> members,
                           int claims, Double bank, long score, int repelled, int conquests, Integer nexusPercent, List<String> treaties,
                           boolean champion) {
   }

   public record OnlineView(String name, String race, String guildId, String guildName, String age, Double coins) {
   }

   public record RichView(String name, double coins) {
   }

   public static WebSnapshot empty() {
      return new WebSnapshot(0L, "-", 0L, null, 0, 0, List.of(), List.of(), List.of(), List.of());
   }

   public GuildView guild(String id) {
      for (GuildView g : this.guilds) {
         if (g.id().equals(id)) {
            return g;
         }
      }

      return null;
   }
}
