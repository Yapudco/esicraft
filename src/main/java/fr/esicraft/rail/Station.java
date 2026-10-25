package fr.esicraft.rail;

/** Une gare : un panneau [Gare] posé à côté d'un rail (le « rail d'ancrage »). */
public final class Station {
   private final String id;
   private final String name;
   private final String guildId;
   private final String world;
   private final int signX;
   private final int signY;
   private final int signZ;
   private final int railX;
   private final int railY;
   private final int railZ;

   public Station(String id, String name, String guildId, String world, int signX, int signY, int signZ, int railX, int railY, int railZ) {
      this.id = id;
      this.name = name;
      this.guildId = guildId;
      this.world = world;
      this.signX = signX;
      this.signY = signY;
      this.signZ = signZ;
      this.railX = railX;
      this.railY = railY;
      this.railZ = railZ;
   }

   public String getId() {
      return this.id;
   }

   public String getName() {
      return this.name;
   }

   public String getGuildId() {
      return this.guildId;
   }

   public String getWorld() {
      return this.world;
   }

   public int getSignX() {
      return this.signX;
   }

   public int getSignY() {
      return this.signY;
   }

   public int getSignZ() {
      return this.signZ;
   }

   public int getRailX() {
      return this.railX;
   }

   public int getRailY() {
      return this.railY;
   }

   public int getRailZ() {
      return this.railZ;
   }

   public long railKey() {
      return RailSearch.pack(this.railX, this.railY, this.railZ);
   }

   public String signKey() {
      return this.world + ":" + RailSearch.pack(this.signX, this.signY, this.signZ);
   }
}
