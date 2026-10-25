package fr.esicraft.rail;

import fr.esicraft.EsicraftPlugin;
import fr.esicraft.gui.holder.BaseGUIHolder;
import fr.esicraft.guild.Guild;
import fr.esicraft.util.ChatUtil;
import fr.esicraft.util.ItemBuilder;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;

/** Interfaces du réseau ferroviaire : menu voyageur (/station) et gestion de guilde (/rail). */
public final class RailGui {
   public static final int PAGE_SIZE = 45;

   private RailGui() {
   }

   // ------------------------------------------------------------------ menu voyageur

   public static final class TravelHolder extends BaseGUIHolder {
      public final RailSearch.Result result;
      public final List<RailSearch.Reached> trips;
      public int page;

      public TravelHolder(RailSearch.Result result, List<RailSearch.Reached> trips) {
         this.result = result;
         this.trips = trips;
      }
   }

   public static void openTravel(EsicraftPlugin plugin, Player player, RailSearch.Result result) {
      List<RailSearch.Reached> trips = new ArrayList<>(result.reached());
      trips.sort(Comparator.comparingInt(RailSearch.Reached::steps));
      TravelHolder holder = new TravelHolder(result, trips);
      renderTravel(plugin, player, holder);
   }

   public static void renderTravel(EsicraftPlugin plugin, Player player, TravelHolder holder) {
      Inventory inv = Bukkit.createInventory(holder, 54, ChatUtil.component("&8» &6🚆 Gare de " + holder.result.origin().getName()));
      int from = holder.page * PAGE_SIZE;
      Guild traveler = plugin.getGuildManager().getGuildByPlayer(player.getUniqueId());
      for (int i = 0; i < PAGE_SIZE && from + i < holder.trips.size(); i++) {
         RailSearch.Reached trip = holder.trips.get(from + i);
         inv.setItem(i, travelItem(plugin, player, holder.result.origin(), trip, traveler));
      }

      int pages = Math.max(1, (holder.trips.size() + PAGE_SIZE - 1) / PAGE_SIZE);
      if (holder.page > 0) {
         inv.setItem(45, new ItemBuilder(Material.ARROW).name("&e◀ Page précédente").build());
      }

      if (holder.page + 1 < pages) {
         inv.setItem(53, new ItemBuilder(Material.ARROW).name("&ePage suivante ▶").build());
      }

      List<String> info = new ArrayList<>();
      info.add("&7Destinations accessibles : &f" + holder.trips.size());
      info.add("&7Tarif de base : &6" + String.format(Locale.ROOT, "%.2f", plugin.getConfig().getDouble("rail.base-fare", 1.0)) + " pièce(s)");
      info.add("&7Chaque guilde dont vous traversez le territoire");
      info.add("&7peut prélever un &6péage&7 (gratuit pour ses alliées).");
      if (!holder.result.blockedBy().isEmpty()) {
         info.add("");
         info.add("&cLignes coupées par : &f" + String.join(", ", holder.result.blockedBy()));
      }

      if (holder.result.truncated()) {
         info.add("");
         info.add("&eRéseau très vaste : recherche limitée.");
      }

      inv.setItem(49, new ItemBuilder(Material.MINECART).name("&6&lRéseau ferroviaire").lore(info).build());
      if (holder.trips.isEmpty()) {
         inv.setItem(22, new ItemBuilder(Material.BARRIER).name("&cAucune gare reliée").lore(
            "&7Aucune autre gare n'est reliée par une ligne de rails",
            "&7continue à celle-ci (ou les guildes traversées",
            "&7vous interdisent le passage).").build());
      }

      player.openInventory(inv);
   }

   private static ItemStack travelItem(EsicraftPlugin plugin, Player player, Station origin, RailSearch.Reached trip, Guild traveler) {
      Station dest = trip.station();
      Guild owner = plugin.getRailManager().guildOf(dest);
      RailManager.Quote quote = plugin.getRailManager().quote(player, trip);
      double dx = dest.getSignX() - origin.getSignX();
      double dz = dest.getSignZ() - origin.getSignZ();
      List<String> lore = new ArrayList<>();
      lore.add("&7Guilde : &f" + (owner != null ? owner.getName() : "—"));
      lore.add("&7Distance : &f" + Math.round(Math.sqrt(dx * dx + dz * dz)) + " blocs &8(" + trip.steps() + " rails)");
      lore.add("");
      if (quote == null) {
         lore.add("&c❌ Passage interdit sur cette ligne");
         return new ItemBuilder(Material.BARRIER).name("&c" + dest.getName()).lore(lore).build();
      }

      lore.add("&7Tarif de gare : &6" + String.format(Locale.ROOT, "%.2f", quote.fare()));
      for (Map.Entry<Guild, Double> toll : quote.tolls().entrySet()) {
         lore.add("&7Péage &f" + toll.getKey().getName() + "&7 : &6" + String.format(Locale.ROOT, "%.2f", toll.getValue()));
      }

      for (Guild free : quote.freePass().keySet()) {
         lore.add("&7Péage &f" + free.getName() + "&7 : &aGratuit (alliée)");
      }

      lore.add("");
      lore.add("&eTotal : &6" + String.format(Locale.ROOT, "%.2f", quote.total()) + " pièces");
      lore.add("&a▶ Clic pour voyager");
      return new ItemBuilder(Material.MINECART).name("&e" + dest.getName()).lore(lore).build();
   }

   // ------------------------------------------------------------------ gestion de guilde

   public enum Kind { MAIN, RELATIONS }

   public static final class ManageHolder extends BaseGUIHolder {
      public final Kind kind;
      public final String guildId;
      public int page;
      public final List<Station> stationSlots = new ArrayList<>();
      public final List<Guild> guildSlots = new ArrayList<>();

      public ManageHolder(Kind kind, String guildId) {
         this.kind = kind;
         this.guildId = guildId;
      }
   }

   public static void openMain(EsicraftPlugin plugin, Player player, Guild guild) {
      ManageHolder holder = new ManageHolder(Kind.MAIN, guild.getId());
      Inventory inv = Bukkit.createInventory(holder, 54, ChatUtil.component("&8» &6🚆 Réseau de " + guild.getName()));
      double toll = plugin.getRailManager().getToll(guild.getId());
      inv.setItem(10, new ItemBuilder(Material.RED_CONCRETE).name("&c-10").build());
      inv.setItem(11, new ItemBuilder(Material.ORANGE_CONCRETE).name("&6-1").build());
      inv.setItem(13, new ItemBuilder(Material.GOLD_INGOT).name("&6&lPéage : " + String.format(Locale.ROOT, "%.0f", toll) + " pièces").lore(
         "&7Prélevé sur chaque voyageur (hors alliés)",
         "&7dont la ligne traverse vos territoires.",
         "&7Versé directement à la banque de guilde.",
         "&8Maximum : " + String.format(Locale.ROOT, "%.0f", plugin.getConfig().getDouble("rail.max-toll", 500.0))).build());
      inv.setItem(15, new ItemBuilder(Material.LIME_CONCRETE).name("&a+1").build());
      inv.setItem(16, new ItemBuilder(Material.GREEN_CONCRETE).name("&2+10").build());
      inv.setItem(31, new ItemBuilder(Material.WRITABLE_BOOK).name("&b&lRelations entre guildes").lore(
         "&7Rendez une guilde &aalliée &7(passage gratuit)",
         "&7ou &cbannie &7(vos terres lui sont fermées :",
         "&7ses lignes qui les traversent sont coupées).",
         "",
         "&eClic pour gérer").build());
      List<Station> own = plugin.getRailManager().stationsOf(guild.getId());
      int[] slots = {28, 29, 30, 32, 33, 34, 37, 38, 39, 40, 41, 42, 43};
      for (int i = 0; i < own.size() && i < slots.length; i++) {
         Station s = own.get(i);
         holder.stationSlots.add(s);
         inv.setItem(slots[i], new ItemBuilder(Material.MINECART).name("&e" + s.getName()).lore(
            "&7Monde : &f" + s.getWorld(),
            "&7Panneau : &f" + s.getSignX() + " " + s.getSignY() + " " + s.getSignZ(),
            "",
            "&cMaj + clic : démanteler la gare").build());
      }

      inv.setItem(49, new ItemBuilder(Material.OAK_SIGN).name("&7Créer une gare").lore(
         "&7Posez un panneau avec :",
         "&f[Gare]",
         "&fNom de la gare",
         "&7à moins de 2 blocs d'un rail.",
         "&8Coût : " + String.format(Locale.ROOT, "%.0f", plugin.getConfig().getDouble("rail.station-creation-cost", 500.0)) + " pièces (banque)").build());
      player.openInventory(inv);
   }

   public static void openRelations(EsicraftPlugin plugin, Player player, Guild guild, int page) {
      ManageHolder holder = new ManageHolder(Kind.RELATIONS, guild.getId());
      holder.page = page;
      Inventory inv = Bukkit.createInventory(holder, 54, ChatUtil.component("&8» &bRelations ferroviaires"));
      List<Guild> others = new ArrayList<>();
      for (Guild g : plugin.getGuildManager().getAllGuilds()) {
         if (!g.getId().equals(guild.getId())) {
            others.add(g);
         }
      }

      others.sort((a, b) -> a.getName().compareToIgnoreCase(b.getName()));
      int from = page * PAGE_SIZE;
      for (int i = 0; i < PAGE_SIZE && from + i < others.size(); i++) {
         Guild other = others.get(from + i);
         holder.guildSlots.add(other);
         Relation r = plugin.getRailManager().getRelation(guild.getId(), other.getId());
         boolean locked = plugin.getRailManager().isTreatyLocked(guild.getId(), other.getId());
         inv.setItem(i, new ItemBuilder(r.getIcon()).name("&f" + other.getName()).lore(
            "&7Relation : " + r.getLabel(),
            "&8" + r.getDescription(),
            "",
            locked ? "&d🔒 Imposée par un traité (/traite)" : "&eClic pour changer (Normale → Alliée → Bannie)").build());
      }

      int pages = Math.max(1, (others.size() + PAGE_SIZE - 1) / PAGE_SIZE);
      if (page > 0) {
         inv.setItem(45, new ItemBuilder(Material.ARROW).name("&e◀ Page précédente").build());
      }

      if (page + 1 < pages) {
         inv.setItem(53, new ItemBuilder(Material.ARROW).name("&ePage suivante ▶").build());
      }

      inv.setItem(49, new ItemBuilder(Material.BARRIER).name("&cRetour").build());
      if (others.isEmpty()) {
         inv.setItem(22, new ItemBuilder(Material.PAPER).name("&7Aucune autre guilde pour le moment").build());
      }

      player.openInventory(inv);
   }
}
