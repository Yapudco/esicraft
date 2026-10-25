package fr.esicraft.treaty;

/** Un traité conclu entre deux guildes. {@code suzerainId} n'est renseigné que pour une vassalité. */
public final class Treaty {
   private final String id;
   private final String aId;
   private final String bId;
   private final TreatyType type;
   private final String suzerainId;
   private final long createdAt;
   private final long expiresAt;
   private long lastTribute;

   public Treaty(String id, String aId, String bId, TreatyType type, String suzerainId, long createdAt, long expiresAt, long lastTribute) {
      this.id = id;
      this.aId = aId;
      this.bId = bId;
      this.type = type;
      this.suzerainId = suzerainId;
      this.createdAt = createdAt;
      this.expiresAt = expiresAt;
      this.lastTribute = lastTribute;
   }

   public String getId() {
      return this.id;
   }

   public String getAId() {
      return this.aId;
   }

   public String getBId() {
      return this.bId;
   }

   public TreatyType getType() {
      return this.type;
   }

   public String getSuzerainId() {
      return this.suzerainId;
   }

   public long getCreatedAt() {
      return this.createdAt;
   }

   public long getExpiresAt() {
      return this.expiresAt;
   }

   public long getLastTribute() {
      return this.lastTribute;
   }

   public void setLastTribute(long lastTribute) {
      this.lastTribute = lastTribute;
   }

   public boolean involves(String guildId) {
      return this.aId.equals(guildId) || this.bId.equals(guildId);
   }

   public String other(String guildId) {
      return this.aId.equals(guildId) ? this.bId : this.aId;
   }

   public boolean isSuzerain(String guildId) {
      return this.suzerainId != null && this.suzerainId.equals(guildId);
   }
}
