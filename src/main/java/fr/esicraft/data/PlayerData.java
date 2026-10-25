package fr.esicraft.data;

import fr.esicraft.guild.GuildRole;
import fr.esicraft.race.Race;
import java.util.UUID;

public class PlayerData {
   private final UUID uniqueId;
   private String playerName;
   private Race race;
   private String guildId;
   private GuildRole guildRole;
   private double coins;
   private long lastRaceChangeTimestamp;
   private int raceChangesCount;
   private boolean hasJoinedGuildBefore;
   private transient long sessionLoginTime;

   public PlayerData(UUID uniqueId, String playerName) {
      this.uniqueId = uniqueId;
      this.playerName = playerName;
      this.race = Race.NONE;
      this.guildId = null;
      this.guildRole = null;
      this.coins = 100.0;
      this.lastRaceChangeTimestamp = 0L;
      this.raceChangesCount = 0;
      this.hasJoinedGuildBefore = false;
      this.sessionLoginTime = System.currentTimeMillis();
   }

   public UUID getUniqueId() {
      return this.uniqueId;
   }

   public String getPlayerName() {
      return this.playerName;
   }

   public void setPlayerName(String playerName) {
      this.playerName = playerName;
   }

   public Race getRace() {
      return this.race != null ? this.race : Race.NONE;
   }

   public void setRace(Race race) {
      this.race = race;
      this.lastRaceChangeTimestamp = System.currentTimeMillis();
   }

   public String getGuildId() {
      return this.guildId;
   }

   public void setGuildId(String guildId) {
      this.guildId = guildId;
   }

   public boolean hasGuild() {
      return this.guildId != null && !this.guildId.trim().isEmpty();
   }

   public GuildRole getGuildRole() {
      return this.guildRole;
   }

   public void setGuildRole(GuildRole guildRole) {
      this.guildRole = guildRole;
   }

   public double getCoins() {
      return this.coins;
   }

   public void setCoins(double coins) {
      if (!Double.isNaN(coins) && !Double.isInfinite(coins)) {
         this.coins = Math.max(0.0, coins);
      }
   }

   public void addCoins(double amount) {
      if (!(amount <= 0.0) && !Double.isNaN(amount) && !Double.isInfinite(amount)) {
         this.coins += amount;
      }
   }

   public boolean removeCoins(double amount) {
      if (amount <= 0.0 || Double.isNaN(amount) || Double.isInfinite(amount)) {
         return false;
      } else if (this.coins >= amount) {
         this.coins -= amount;
         return true;
      } else {
         return false;
      }
   }

   public long getLastRaceChangeTimestamp() {
      return this.lastRaceChangeTimestamp;
   }

   public void setLastRaceChangeTimestamp(long lastRaceChangeTimestamp) {
      this.lastRaceChangeTimestamp = lastRaceChangeTimestamp;
   }

   public int getRaceChangesCount() {
      return this.raceChangesCount;
   }

   public void setRaceChangesCount(int raceChangesCount) {
      this.raceChangesCount = Math.max(0, raceChangesCount);
   }

   public void incrementRaceChangesCount() {
      this.raceChangesCount++;
   }

   public boolean hasJoinedGuildBefore() {
      return this.hasJoinedGuildBefore;
   }

   public void setHasJoinedGuildBefore(boolean hasJoinedGuildBefore) {
      this.hasJoinedGuildBefore = hasJoinedGuildBefore;
   }

   public long getSessionLoginTime() {
      return this.sessionLoginTime > 0L ? this.sessionLoginTime : System.currentTimeMillis();
   }

   public void setSessionLoginTime(long sessionLoginTime) {
      this.sessionLoginTime = sessionLoginTime;
   }
}
