package fr.esicraft.guild;

public enum GuildRole {
   RECRUIT("&7Recrue", 0),
   MEMBER("&aMembre", 1),
   OFFICER("&eOfficier", 2),
   LEADER("&6Chef", 3);

   private final String displayName;
   private final int level;

   private GuildRole(String displayName, int level) {
      this.displayName = displayName;
      this.level = level;
   }

   public String getDisplayName() {
      return this.displayName;
   }

   public int getLevel() {
      return this.level;
   }

   public boolean isAtLeast(GuildRole role) {
      return this.level >= role.level;
   }
}
