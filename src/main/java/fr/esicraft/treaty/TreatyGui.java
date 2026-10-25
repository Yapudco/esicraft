package fr.esicraft.treaty;

import fr.esicraft.EsicraftPlugin;
import fr.esicraft.gui.holder.BaseGUIHolder;
import fr.esicraft.guild.Guild;
import fr.esicraft.util.ChatUtil;
import fr.esicraft.util.ItemBuilder;
import java.util.ArrayList;
import java.util.List;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;

/** Menus /traite : vue d'ensemble, choix du partenaire, choix du type. */
public final class TreatyGui {
   public static final int PAGE_SIZE = 45;

   public enum Kind { MAIN, PICK_GUILD, PICK_TYPE }

   public static final class Holder extends BaseGUIHolder {
      public final Kind kind;
      public final String guildId;
      public int page;
      public String targetId;
      public final List<Guild> guildSlots = new ArrayList<>();
      public final List<Treaty> treatySlots = new ArrayList<>();
      public final List<Proposal> receivedSlots = new ArrayList<>();
      public final List<Proposal> sentSlots = new ArrayList<>();

      public Holder(Kind kind, String guildId) {
         this.kind = kind;
         this.guildId = guildId;
      }
   }

   private TreatyGui() {
   }

   public static void openMain(EsicraftPlugin plugin, Player player, Guild guild) {
      TreatyManager tm = plugin.getTreatyManager();
      Holder holder = new Holder(Kind.MAIN, guild.getId());
      Inventory inv = Bukkit.createInventory(holder, 54, ChatUtil.component("&8» &dTraités de &f" + guild.getName()));
      int slot = 0;
      for (Treaty t : tm.treatiesOf(guild.getId())) {
         if (slot >= 18) {
            break;
         }

         Guild other = plugin.getGuildManager().getGuildById(t.other(guild.getId()));
         String role = t.getType() == TreatyType.VASSALAGE ? (t.isSuzerain(guild.getId()) ? "&6Vous êtes suzerain" : "&6Vous êtes vassal") : null;
         List<String> lore = new ArrayList<>();
         lore.add("&7Avec : &f" + (other != null ? other.getName() : "?"));
         if (role != null) {
            lore.add(role);
         }

         lore.add(t.getExpiresAt() > 0L ? "&7Expire dans : &e" + TreatyManager.formatDuration(t.getExpiresAt() - System.currentTimeMillis()) : "&7Durée : &eillimitée");
         lore.addAll(t.getType().getEffects());
         lore.add("");
         boolean betrayal = tm.isBetrayal(t, guild);
         lore.add(betrayal ? "&cShift + clic : ROMPRE (indemnité " + String.format("%.0f", tm.indemnity(guild)) + " pièces + préavis)" : "&eShift + clic : mettre fin au traité");
         holder.treatySlots.add(t);
         inv.setItem(slot++, new ItemBuilder(t.getType().getIcon()).name("&d" + t.getType().getLabel()).lore(lore).build());
      }

      if (holder.treatySlots.isEmpty()) {
         inv.setItem(4, new ItemBuilder(Material.GRAY_STAINED_GLASS_PANE).name("&7Aucun traité en vigueur").build());
      }

      int rslot = 27;
      for (Proposal p : tm.receivedBy(guild.getId())) {
         if (rslot >= 36) {
            break;
         }

         Guild from = plugin.getGuildManager().getGuildById(p.fromId());
         if (from == null) {
            continue;
         }

         List<String> lore = new ArrayList<>();
         lore.add("&7De : &f" + from.getName());
         if (p.type() == TreatyType.VASSALAGE) {
            lore.add(p.suzerainId().equals(from.getId()) ? "&6" + from.getName() + " suzerain, vous vassal" : "&6Vous suzerain, " + from.getName() + " vassal");
         }

         lore.addAll(p.type().getEffects());
         lore.add("");
         lore.add("&aClic gauche : accepter");
         lore.add("&cClic droit : refuser");
         holder.receivedSlots.add(p);
         inv.setItem(rslot++, new ItemBuilder(p.type().getIcon()).name("&a📩 " + p.type().getLabel()).lore(lore).glow().build());
      }

      int sslot = 36;
      for (Proposal p : tm.sentBy(guild.getId())) {
         if (sslot >= 45) {
            break;
         }

         Guild to = plugin.getGuildManager().getGuildById(p.toId());
         if (to == null) {
            continue;
         }

         holder.sentSlots.add(p);
         inv.setItem(sslot++, new ItemBuilder(Material.WRITABLE_BOOK).name("&e📤 " + p.type().getLabel() + " → " + to.getName())
            .lore("&7En attente de réponse.", "&7Expire dans : &e" + TreatyManager.formatDuration(p.expiresAt() - System.currentTimeMillis()), "", "&cClic : annuler").build());
      }

      inv.setItem(49, new ItemBuilder(Material.WRITABLE_BOOK).name("&a➕ Proposer un traité")
         .lore("&7Alliance, pacte de non-agression", "&7ou vassalité avec une autre guilde.").build());
      inv.setItem(53, new ItemBuilder(Material.BOOK).name("&bComment ça marche ?").lore(
         "&dAlliance &7: pas de raid, rails gratuits, pas de PvP.",
         "&dPacte &7: pas de raid pendant la durée du pacte.",
         "&dVassalité &7: tribut quotidien au suzerain + alliance.",
         "",
         "&cRompre un traité = indemnité + préavis avant de",
         "&cpouvoir attaquer l'ancien partenaire.").build());
      player.openInventory(inv);
   }

   public static void openPickGuild(EsicraftPlugin plugin, Player player, Guild guild, int page) {
      Holder holder = new Holder(Kind.PICK_GUILD, guild.getId());
      holder.page = page;
      Inventory inv = Bukkit.createInventory(holder, 54, ChatUtil.component("&8» &dAvec quelle guilde ?"));
      List<Guild> others = new ArrayList<>();
      for (Guild g : plugin.getGuildManager().getAllGuilds()) {
         if (!g.getId().equals(guild.getId()) && plugin.getTreatyManager().between(guild.getId(), g.getId()) == null) {
            others.add(g);
         }
      }

      others.sort((a, b) -> a.getName().compareToIgnoreCase(b.getName()));
      int from = page * PAGE_SIZE;
      for (int i = 0; i < PAGE_SIZE && from + i < others.size(); i++) {
         Guild other = others.get(from + i);
         holder.guildSlots.add(other);
         inv.setItem(i, new ItemBuilder(Material.WHITE_BANNER).name("&f" + other.getName()).lore("&7Âge : &f" + other.getCurrentAge().getLevel(), "", "&eClic pour choisir").build());
      }

      if (page > 0) {
         inv.setItem(45, new ItemBuilder(Material.ARROW).name("&e◀ Page précédente").build());
      }

      if ((page + 1) * PAGE_SIZE < others.size()) {
         inv.setItem(53, new ItemBuilder(Material.ARROW).name("&ePage suivante ▶").build());
      }

      inv.setItem(49, new ItemBuilder(Material.BARRIER).name("&cRetour").build());
      if (others.isEmpty()) {
         inv.setItem(22, new ItemBuilder(Material.PAPER).name("&7Aucune guilde disponible").build());
      }

      player.openInventory(inv);
   }

   public static void openPickType(EsicraftPlugin plugin, Player player, Guild guild, Guild target) {
      Holder holder = new Holder(Kind.PICK_TYPE, guild.getId());
      holder.targetId = target.getId();
      Inventory inv = Bukkit.createInventory(holder, 27, ChatUtil.component("&8» &dTraité avec &f" + target.getName()));
      long days = plugin.getConfig().getLong("treaties.pact-days", 7L);
      inv.setItem(10, new ItemBuilder(TreatyType.ALLIANCE.getIcon()).name("&dAlliance").lore(prefix(TreatyType.ALLIANCE, "&7Durée : &eillimitée", "", "&eClic : proposer")).build());
      inv.setItem(12, new ItemBuilder(TreatyType.PACT.getIcon()).name("&dPacte de non-agression").lore(prefix(TreatyType.PACT, "&7Durée : &e" + days + " jours", "", "&eClic : proposer")).build());
      inv.setItem(14, new ItemBuilder(TreatyType.VASSALAGE.getIcon()).name("&dVous êtes SUZERAIN").lore(prefix(TreatyType.VASSALAGE,
         "&7" + target.getName() + " devient votre vassal.", "&7Tribut : &e" + plugin.getConfig().getDouble("treaties.vassal-tribute-percent", 5.0) + "% &7de sa banque / jour", "", "&eClic : proposer")).build());
      inv.setItem(16, new ItemBuilder(Material.IRON_BLOCK).name("&dVous êtes VASSAL").lore(prefix(TreatyType.VASSALAGE,
         "&7Vous prêtez allégeance à &f" + target.getName() + "&7.", "&7Vous lui versez un tribut quotidien.", "", "&eClic : proposer")).build());
      inv.setItem(22, new ItemBuilder(Material.BARRIER).name("&cRetour").build());
      player.openInventory(inv);
   }

   private static List<String> prefix(TreatyType type, String... extra) {
      List<String> lines = new ArrayList<>(type.getEffects());
      lines.addAll(List.of(extra));
      return lines;
   }
}
