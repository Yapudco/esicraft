package fr.esicraft.gui.holder;

import fr.esicraft.raid.RaidType;

public class RaidTargetHolder extends BaseGUIHolder {
   private final RaidType raidType;
   private final boolean isCustom;

   public RaidTargetHolder(RaidType raidType, boolean isCustom) {
      this.raidType = raidType;
      this.isCustom = isCustom;
   }

   public RaidType getRaidType() {
      return this.raidType;
   }

   public boolean isCustom() {
      return this.isCustom;
   }
}
