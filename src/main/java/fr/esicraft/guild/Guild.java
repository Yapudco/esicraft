package fr.esicraft.guild;

import fr.esicraft.age.AgeType;
import fr.esicraft.nexus.Nexus;
import fr.esicraft.util.ChunkKey;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import org.bukkit.Location;
import org.bukkit.Material;

public class Guild {
   private final String id;
   private String name;
   private String tag;
   private UUID leaderUuid;
   private GuildSpecialization specialization;
   private final Map<UUID, GuildRole> members = new ConcurrentHashMap<>();
   private final Set<UUID> historicalMembers = ConcurrentHashMap.newKeySet();
   private final Set<ChunkKey> claimedChunks = ConcurrentHashMap.newKeySet();
   private double bankBalance;
   private Location homeLocation;
   private Nexus nexus;
   private long vulnerableUntil;
   private AgeType currentAge;
   private final Map<Material, Integer> ageDonations = new ConcurrentHashMap<>();
   private long createdAt;
   private boolean canChangeSpecialization;
   private volatile int peakMembersThisAge = 1;
   private volatile boolean membersDirty = false;
   private volatile boolean claimsDirty = false;

   public Guild(String id, String name, String tag, UUID leaderUuid) {
      this.id = id;
      this.name = name;
      this.tag = tag;
      this.leaderUuid = leaderUuid;
      this.specialization = GuildSpecialization.NONE;
      this.members.put(leaderUuid, GuildRole.LEADER);
      this.historicalMembers.add(leaderUuid);
      this.bankBalance = 0.0;
      this.homeLocation = null;
      this.nexus = null;
      this.vulnerableUntil = 0L;
      this.currentAge = AgeType.RUSTIC;
      this.createdAt = System.currentTimeMillis();
      this.canChangeSpecialization = true;
   }

   public String getId() {
      return this.id;
   }

   public String getName() {
      return this.name;
   }

   public void setName(String name) {
      this.name = name;
   }

   public String getTag() {
      return this.tag;
   }

   public void setTag(String tag) {
      this.tag = tag;
   }

   public UUID getLeaderUuid() {
      return this.leaderUuid;
   }

   public void setLeaderUuid(UUID leaderUuid) {
      this.leaderUuid = leaderUuid;
   }

   public GuildSpecialization getSpecialization() {
      return this.specialization != null ? this.specialization : GuildSpecialization.NONE;
   }

   public void setSpecialization(GuildSpecialization specialization) {
      this.specialization = specialization;
   }

   public Map<UUID, GuildRole> getMembers() {
      return this.members;
   }

   public boolean isMember(UUID uuid) {
      return this.members.containsKey(uuid);
   }

   /** Renvoie null pour un non-membre (ne JAMAIS supposer un rang par défaut). */
   public GuildRole getRole(UUID uuid) {
      return this.members.get(uuid);
   }

   public boolean hasRoleAtLeast(UUID uuid, GuildRole minimum) {
      GuildRole role = this.members.get(uuid);
      return role != null && role.isAtLeast(minimum);
   }

   public void addMember(UUID uuid, GuildRole role) {
      this.members.put(uuid, role);
      this.peakMembersThisAge = Math.max(this.peakMembersThisAge, this.members.size());
      this.membersDirty = true;
   }

   /** Effectif utilisé pour les quotas d'Âge : le pic de l'Âge en cours (empêche de faire sortir ses membres avant de déposer). */
   public int getEffectiveMemberCount() {
      this.peakMembersThisAge = Math.max(this.peakMembersThisAge, this.members.size());
      return Math.max(1, this.peakMembersThisAge);
   }

   public void removeMember(UUID uuid) {
      this.members.remove(uuid);
      this.membersDirty = true;
   }

   public boolean isMembersDirty() {
      return this.membersDirty;
   }

   public void setMembersDirty(boolean membersDirty) {
      this.membersDirty = membersDirty;
   }

   public Set<ChunkKey> getClaimedChunks() {
      return this.claimedChunks;
   }

   public void addClaim(ChunkKey chunkKey) {
      this.claimedChunks.add(chunkKey);
      this.claimsDirty = true;
   }

   public void removeClaim(ChunkKey chunkKey) {
      this.claimedChunks.remove(chunkKey);
      this.claimsDirty = true;
   }

   public boolean isClaimsDirty() {
      return this.claimsDirty;
   }

   public void setClaimsDirty(boolean claimsDirty) {
      this.claimsDirty = claimsDirty;
   }

   public boolean isClaimed(ChunkKey chunkKey) {
      return this.claimedChunks.contains(chunkKey);
   }

   public synchronized double getBankBalance() {
      return this.bankBalance;
   }

   public synchronized void setBankBalance(double bankBalance) {
      if (Double.isNaN(bankBalance) || Double.isInfinite(bankBalance)) {
         return;
      }
      this.bankBalance = Math.max(0.0, bankBalance);
   }

   public synchronized void depositBank(double amount) {
      if (amount <= 0.0 || Double.isNaN(amount) || Double.isInfinite(amount)) {
         return;
      }
      this.bankBalance += amount;
   }

   public synchronized boolean withdrawBank(double amount) {
      if (amount <= 0.0 || Double.isNaN(amount) || Double.isInfinite(amount)) {
         return false;
      }
      if (this.bankBalance >= amount) {
         this.bankBalance -= amount;
         return true;
      } else {
         return false;
      }
   }

   public Location getHomeLocation() {
      return this.homeLocation;
   }

   public void setHomeLocation(Location homeLocation) {
      this.homeLocation = homeLocation;
   }

   public Nexus getNexus() {
      return this.nexus;
   }

   public void setNexus(Nexus nexus) {
      this.nexus = nexus;
   }

   public boolean hasNexus() {
      return this.nexus != null && this.nexus.getLocation() != null;
   }

   public long getVulnerableUntil() {
      return this.vulnerableUntil;
   }

   public void setVulnerableUntil(long vulnerableUntil) {
      this.vulnerableUntil = vulnerableUntil;
   }

   public boolean isVulnerable() {
      return System.currentTimeMillis() < this.vulnerableUntil;
   }

   public AgeType getCurrentAge() {
      return this.currentAge != null ? this.currentAge : AgeType.RUSTIC;
   }

   public void setCurrentAge(AgeType currentAge) {
      this.currentAge = currentAge;
   }

   public Map<Material, Integer> getAgeDonations() {
      return this.ageDonations;
   }

   public int getDonatedAmount(Material material) {
      return this.ageDonations.getOrDefault(material, 0);
   }

   public void addDonation(Material material, int amount) {
      this.ageDonations.put(material, this.getDonatedAmount(material) + amount);
   }

   public void clearDonations() {
      this.ageDonations.clear();
      this.peakMembersThisAge = Math.max(1, this.members.size());
   }

   public long getCreatedAt() {
      return this.createdAt;
   }

   public void setCreatedAt(long createdAt) {
      this.createdAt = createdAt;
   }

   public Set<UUID> getHistoricalMembers() {
      return this.historicalMembers;
   }

   public boolean isHistoricalMember(UUID uuid) {
      return this.historicalMembers.contains(uuid);
   }

   public void addHistoricalMember(UUID uuid) {
      this.historicalMembers.add(uuid);
   }

   public boolean canChangeSpecialization() {
      return this.canChangeSpecialization;
   }

   public void setCanChangeSpecialization(boolean canChangeSpecialization) {
      this.canChangeSpecialization = canChangeSpecialization;
   }
}
