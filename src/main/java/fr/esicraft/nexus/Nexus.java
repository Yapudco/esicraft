package fr.esicraft.nexus;

import java.util.Collections;
import java.util.EnumSet;
import java.util.Set;
import org.bukkit.Location;

public class Nexus {
   private final String guildId;
   private Location location;
   private int currentHealth;
   private int maxHealth;
   private boolean alive;
   private final Set<NexusTrapType> activeTraps = EnumSet.noneOf(NexusTrapType.class);

   public Nexus(String guildId, Location location, int maxHealth) {
      this.guildId = guildId;
      this.location = location;
      this.maxHealth = maxHealth;
      this.currentHealth = maxHealth;
      this.alive = true;
   }

   public Set<NexusTrapType> getActiveTraps() {
      return Collections.unmodifiableSet(this.activeTraps);
   }

   public boolean hasTrap(NexusTrapType trap) {
      return trap == null ? false : this.activeTraps.contains(trap);
   }

   public void addTrap(NexusTrapType trap) {
      if (trap != null) {
         this.activeTraps.add(trap);
      }
   }

   public void removeTrap(NexusTrapType trap) {
      if (trap != null) {
         this.activeTraps.remove(trap);
      }
   }

   public void clearTraps() {
      this.activeTraps.clear();
   }

   public String getGuildId() {
      return this.guildId;
   }

   public Location getLocation() {
      return this.location;
   }

   public void setLocation(Location location) {
      this.location = location;
   }

   public int getCurrentHealth() {
      return this.currentHealth;
   }

   public void setCurrentHealth(int currentHealth) {
      this.currentHealth = Math.max(0, Math.min(currentHealth, this.maxHealth));
      if (this.currentHealth == 0) {
         this.alive = false;
      }
   }

   public int getMaxHealth() {
      return this.maxHealth;
   }

   public void setMaxHealth(int maxHealth) {
      this.maxHealth = maxHealth;
   }

   public boolean isAlive() {
      return this.alive;
   }

   public void setAlive(boolean alive) {
      this.alive = alive;
   }

   public void damage(int amount) {
      this.setCurrentHealth(this.currentHealth - amount);
   }

   public void heal(int amount) {
      this.setCurrentHealth(this.currentHealth + amount);
      if (this.currentHealth > 0) {
         this.alive = true;
      }
   }

   public double getHealthPercentage() {
      return this.maxHealth <= 0 ? 0.0 : (double)this.currentHealth / (double)this.maxHealth * 100.0;
   }
}
