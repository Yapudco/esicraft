package fr.esicraft.gui.holder;

public class MarketMainHolder extends BaseGUIHolder {
   private final int page;

   public MarketMainHolder() {
      this(1);
   }

   public MarketMainHolder(int page) {
      this.page = Math.max(1, page);
   }

   public int getPage() {
      return this.page;
   }
}
