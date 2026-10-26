package fr.esicraft.raid;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import org.bukkit.Location;

public class ActiveRaid {
   private final String id;
   private final String attackerGuildId;
   private final String attackerGuildName;
   private final String targetGuildId;
   private final RaidType raidType;
   private final String customRaidName;
   private final Location targetNexusLocation;
   private final boolean explosiveBreachers;
   private final boolean lavaImmunity;
   private final List<UUID> aliveMobUuids = new CopyOnWriteArrayList<>();
   private final Map<UUID, Integer> mobDamageMap = new ConcurrentHashMap<>();
   private final long startTime;
   private final java.util.concurrent.atomic.AtomicInteger vanished = new java.util.concurrent.atomic.AtomicInteger();

   public ActiveRaid(
      String id,
      String attackerGuildId,
      String attackerGuildName,
      String targetGuildId,
      RaidType raidType,
      boolean explosiveBreachers,
      boolean lavaImmunity,
      Location targetNexusLocation
   ) {
      this(
         id,
         attackerGuildId,
         attackerGuildName,
         targetGuildId,
         raidType,
         raidType != null ? raidType.getDisplayName() : "Horde Noire",
         explosiveBreachers,
         lavaImmunity,
         targetNexusLocation
      );
   }

   public ActiveRaid(
      String id,
      String attackerGuildId,
      String attackerGuildName,
      String targetGuildId,
      RaidType raidType,
      String customRaidName,
      boolean explosiveBreachers,
      boolean lavaImmunity,
      Location targetNexusLocation
   ) {
      this.id = id;
      this.attackerGuildId = attackerGuildId;
      this.attackerGuildName = attackerGuildName;
      this.targetGuildId = targetGuildId;
      this.raidType = raidType;
      this.customRaidName = customRaidName;
      this.explosiveBreachers = explosiveBreachers;
      this.lavaImmunity = lavaImmunity;
      this.targetNexusLocation = targetNexusLocation;
      this.startTime = System.currentTimeMillis();
   }

   public void markVanished() {
      this.vanished.incrementAndGet();
   }

   public int getVanished() {
      return this.vanished.get();
   }

   public String getId() {
      return this.id;
   }

   public String getAttackerGuildId() {
      return this.attackerGuildId;
   }

   public String getAttackerGuildName() {
      return this.attackerGuildName;
   }

   public String getTargetGuildId() {
      return this.targetGuildId;
   }

   public RaidType getRaidType() {
      return this.raidType;
   }

   public String getRaidDisplayName() {
      return this.customRaidName != null ? this.customRaidName : (this.raidType != null ? this.raidType.getDisplayName() : "Horde Personnalisée");
   }

   public boolean hasExplosiveBreachers() {
      return this.explosiveBreachers;
   }

   public boolean hasLavaImmunity() {
      return this.lavaImmunity;
   }

   public Location getTargetNexusLocation() {
      return this.targetNexusLocation;
   }

   public List<UUID> getAliveMobUuids() {
      return this.aliveMobUuids;
   }

   private int totalMobs;

   public int getTotalMobs() {
      return this.totalMobs;
   }

   public long getStartTime() {
      return this.startTime;
   }

   public void addMob(UUID uuid, int damageToNexus) {
      this.totalMobs++;
      this.aliveMobUuids.add(uuid);
      this.mobDamageMap.put(uuid, damageToNexus);
   }

   public int getMobDamage(UUID uuid) {
      return this.mobDamageMap.getOrDefault(uuid, this.raidType != null ? this.raidType.getDamageToNexus() : 25);
   }

   public void removeMob(UUID uuid) {
      this.aliveMobUuids.remove(uuid);
      this.mobDamageMap.remove(uuid);
   }

   public boolean isFinished() {
      return this.aliveMobUuids.isEmpty() || System.currentTimeMillis() - this.startTime > 600000L;
   }
}
