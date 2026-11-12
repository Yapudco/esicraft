package fr.esicraft.economy;

import fr.esicraft.EsicraftPlugin;
import fr.esicraft.gui.holder.BaseGUIHolder;
import fr.esicraft.guild.Guild;
import fr.esicraft.util.ChatUtil;
import fr.esicraft.util.ItemBuilder;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;

import static fr.esicraft.economy.LoanManager.money;

/**
 * Menu de la banque de guilde ({@code /banque}) : offres reçues et envoyées, dettes, créances, proposition d'un prêt avec choix du montant
 * et du <b>taux d'intérêt</b>, et accès aux coffres communs. Toutes les actions passent par {@link LoanManager}, qui fait les mêmes vérifications
 * que les commandes.
 */
final class LoanGui implements Listener {
   private enum Mode {
      MAIN, LEND_TO, CHEST_WITH, OFFER
   }

   private static final class Holder extends BaseGUIHolder {
      final Mode mode;
      final String guildId;
      final Map<Integer, Object> actions = new HashMap<>();
      String targetId;
      double amount;
      double rate;

      Holder(Mode mode, String guildId) {
         this.mode = mode;
         this.guildId = guildId;
      }
   }

   private static final int[] OFFERS_IN = {10, 11, 12, 13, 14, 15, 16};
   private static final int[] DEBTS = {19, 20, 21, 22, 23, 24, 25};
   private static final int[] CREDITS = {28, 29, 30, 31, 32, 33, 34};
   private static final int[] OFFERS_OUT = {37, 38, 39, 40, 41, 42, 43};

   private final EsicraftPlugin plugin;
   private final LoanManager loans;

   LoanGui(EsicraftPlugin plugin, LoanManager loans) {
      this.plugin = plugin;
      this.loans = loans;
   }

   private static ItemStack glass() {
      return new ItemBuilder(Material.GRAY_STAINED_GLASS_PANE).name(" ").build();
   }

   private static void fill(Inventory inv) {
      ItemStack g = glass();
      for (int i = 0; i < inv.getSize(); i++) {
         inv.setItem(i, g);
      }
   }

   private String nameOf(String guildId) {
      Guild g = this.plugin.getGuildManager().getGuildById(guildId);
      return g != null ? g.getName() : "?";
   }

   private static String left(long ms) {
      return fr.esicraft.treaty.TreatyManager.formatDuration(Math.max(0L, ms));
   }

   // ------------------------------------------------------------------ menu principal

   void openMain(Player player, Guild guild) {
      Holder h = new Holder(Mode.MAIN, guild.getId());
      Inventory inv = Bukkit.createInventory(h, 54, ChatUtil.component("&8» &6🏦 Banque de " + guild.getName()));
      fill(inv);
      inv.setItem(4, new ItemBuilder(Material.GOLD_BLOCK).name("&6&lBanque de " + guild.getName())
         .lore("&7Fonds : &e" + money(guild.getBankBalance()) + " pièces", "", "&7Prêts entre guildes liées par un &etraité&7.", "&7Taux d'intérêt libre, de &e" + money(this.loans.minRate())
            + " &7à &e" + money(this.loans.maxRate()) + " %&7, à rendre sous &e" + this.loans.loanDays() + " jours&7.", "&cUne dette en retard bloque les revendications", "&cet fait saisir un chunk par jour.")
         .build());

      inv.setItem(9, new ItemBuilder(Material.WRITABLE_BOOK).name("&e&lOffres reçues").lore("&7Clic gauche : accepter", "&7Clic droit : refuser").build());
      List<LoanManager.Proposal> in = this.loans.proposalsFor(guild.getId());
      for (int i = 0; i < in.size() && i < OFFERS_IN.length; i++) {
         LoanManager.Proposal p = in.get(i);
         inv.setItem(OFFERS_IN[i], new ItemBuilder(Material.PAPER).name("&ePrêt de &6" + this.nameOf(p.lenderId()))
            .lore("&7Montant : &a+" + money(p.amount()) + " pièces", "&7Taux : &e" + money(p.rate()) + " %", "&7À rendre : &c" + money(p.toRepay()) + " pièces &7sous " + this.loans.loanDays() + " jours",
               "&7Expire dans &f" + left(p.expiresAt() - System.currentTimeMillis()), "", "&a➤ Clic gauche : accepter", "&c➤ Clic droit : refuser")
            .build());
         h.actions.put(OFFERS_IN[i], p);
      }

      inv.setItem(18, new ItemBuilder(Material.RED_DYE).name("&c&lNos dettes").lore("&7Clic gauche : tout rembourser (selon la banque)", "&7Clic droit : rembourser 1 000 pièces").build());
      inv.setItem(27, new ItemBuilder(Material.LIME_DYE).name("&a&lNos créances").lore("&7Ce que les autres guildes nous doivent.", "&7Maj + clic droit : effacer la dette (chef)").build());
      int d = 0;
      int c = 0;
      long now = System.currentTimeMillis();
      for (LoanManager.Loan l : this.loans.loansOf(guild.getId())) {
         boolean overdue = LoanManager.OVERDUE.equals(l.status);
         String due = overdue ? "&4&lEN RETARD" : "&7Échéance dans &f" + left(l.dueAt - now);
         if (l.borrowerId.equals(guild.getId()) && d < DEBTS.length) {
            inv.setItem(DEBTS[d], new ItemBuilder(overdue ? Material.REDSTONE_BLOCK : Material.RED_CONCRETE).name("&cDette envers &6" + this.nameOf(l.lenderId))
               .lore("&7Emprunté : &f" + money(l.principal), "&7Reste dû : &c" + money(l.owed) + " pièces", due, "", "&a➤ Clic gauche : tout rembourser", "&e➤ Clic droit : rembourser 1 000").build());
            h.actions.put(DEBTS[d++], l);
         } else if (l.lenderId.equals(guild.getId()) && c < CREDITS.length) {
            inv.setItem(CREDITS[c], new ItemBuilder(overdue ? Material.GOLD_BLOCK : Material.EMERALD).name("&a" + this.nameOf(l.borrowerId) + " &anous doit")
               .lore("&7Prêté : &f" + money(l.principal), "&7Reste dû : &a" + money(l.owed) + " pièces", due, "", "&c➤ Maj + clic droit : effacer la dette (chef)").build());
            h.actions.put(CREDITS[c++], l);
         }
      }

      inv.setItem(36, new ItemBuilder(Material.MAP).name("&b&lOffres envoyées").lore("&7Cliquez sur une offre pour la retirer.").build());
      List<LoanManager.Proposal> out = this.loans.proposalsFrom(guild.getId());
      for (int i = 0; i < out.size() && i < OFFERS_OUT.length; i++) {
         LoanManager.Proposal p = out.get(i);
         inv.setItem(OFFERS_OUT[i], new ItemBuilder(Material.FILLED_MAP).name("&bOffre à &6" + this.nameOf(p.borrowerId()))
            .lore("&7Montant : &f" + money(p.amount()) + " pièces", "&7Taux : &e" + money(p.rate()) + " %", "&7Retour attendu : &a" + money(p.toRepay()),
               "&7Expire dans &f" + left(p.expiresAt() - System.currentTimeMillis()), "", "&c➤ Cliquez pour retirer l'offre").build());
         h.actions.put(OFFERS_OUT[i], p);
      }

      inv.setItem(48, new ItemBuilder(Material.GOLD_INGOT).name("&a&lProposer un prêt").lore("&7Choisissez une guilde partenaire,", "&7le montant et le taux d'intérêt.", "&8(officiers et chef)").glow().build());
      inv.setItem(50, new ItemBuilder(Material.CHEST).name("&6&lCoffres communs").lore("&7Un coffre partagé avec chaque", "&7guilde liée par un traité.").build());
      inv.setItem(53, new ItemBuilder(Material.BARRIER).name("&cFermer").build());
      player.openInventory(inv);
   }

   // ------------------------------------------------------------------ choix d'un partenaire

   void openPartners(Player player, Guild guild, boolean lend) {
      Holder h = new Holder(lend ? Mode.LEND_TO : Mode.CHEST_WITH, guild.getId());
      Inventory inv = Bukkit.createInventory(h, 54, ChatUtil.component(lend ? "&8» &6Prêter à quelle guilde ?" : "&8» &6Coffre commun avec…"));
      fill(inv);
      List<Guild> partners = this.loans.partners(guild);
      if (partners.isEmpty()) {
         inv.setItem(22, new ItemBuilder(Material.BARRIER).name("&cAucune guilde partenaire").lore("&7Il faut un traité (&e/traite&7) pour", "&7prêter ou partager un coffre.").build());
      }

      for (int i = 0; i < partners.size() && i < 36; i++) {
         Guild g = partners.get(i);
         ItemBuilder b = new ItemBuilder(Material.WHITE_BANNER).name("&6" + g.getName() + " &7[" + g.getTag() + "]");
         if (lend) {
            double cap = this.plugin.getConfig().getDouble("loans.max-per-claim", 1500.0) * g.getClaimedChunks().size();
            b.lore("&7Territoires : &f" + g.getClaimedChunks().size(), "&7Prêt maximal garanti : &e" + money(Math.min(cap, this.loans.maxAmount())) + " pièces",
               this.loans.hasOverdue(g.getId()) ? "&4En retard de paiement : prêt impossible" : "&a➤ Cliquez pour préparer un prêt");
         } else {
            b.lore("&a➤ Cliquez pour ouvrir le coffre commun");
         }

         inv.setItem(9 + i, b.build());
         h.actions.put(9 + i, g.getId());
      }

      inv.setItem(45, new ItemBuilder(Material.ARROW).name("&e◀ Retour").build());
      player.openInventory(inv);
   }

   // ------------------------------------------------------------------ préparation d'une offre

   private double maxFor(Guild lender, Guild borrower) {
      double cap = this.plugin.getConfig().getDouble("loans.max-per-claim", 1500.0) * borrower.getClaimedChunks().size();
      return Math.max(0.0, Math.min(Math.min(this.loans.maxAmount(), cap), Math.floor(lender.getBankBalance())));
   }

   private void openOffer(Player player, Guild lender, Guild borrower, double amount, double rate) {
      Holder h = new Holder(Mode.OFFER, lender.getId());
      h.targetId = borrower.getId();
      double max = this.maxFor(lender, borrower);
      double min = this.loans.minAmount();
      h.amount = max < min ? min : Math.max(min, Math.min(max, amount));
      h.rate = this.loans.clampRate(rate);
      Inventory inv = Bukkit.createInventory(h, 54, ChatUtil.component("&8» &6Prêt à " + borrower.getName()));
      fill(inv);
      double repay = Math.ceil(h.amount * (1.0 + h.rate / 100.0));
      inv.setItem(4, new ItemBuilder(Material.WHITE_BANNER).name("&6" + borrower.getName())
         .lore("&7Notre banque : &e" + money(lender.getBankBalance()) + " pièces", "&7Prêt possible : &e" + money(min) + " &7à &e" + money(max) + " pièces",
            "&8(limité par notre banque, " + money(this.plugin.getConfig().getDouble("loans.max-per-claim", 1500.0)) + " par territoire", "&8de l'emprunteur et le plafond du serveur)")
         .build());

      inv.setItem(9, new ItemBuilder(Material.GOLD_NUGGET).name("&e&lMontant").build());
      inv.setItem(10, new ItemBuilder(Material.RED_STAINED_GLASS_PANE).name("&c-1 000").build());
      inv.setItem(11, new ItemBuilder(Material.ORANGE_STAINED_GLASS_PANE).name("&6-100").build());
      inv.setItem(13, new ItemBuilder(Material.GOLD_INGOT).name("&e&l" + money(h.amount) + " pièces").lore("&7Somme versée à " + borrower.getName()).build());
      inv.setItem(15, new ItemBuilder(Material.LIME_STAINED_GLASS_PANE).name("&a+100").build());
      inv.setItem(16, new ItemBuilder(Material.GREEN_STAINED_GLASS_PANE).name("&2+1 000").build());
      inv.setItem(17, new ItemBuilder(Material.GOLD_BLOCK).name("&6Maximum").lore("&e" + money(max) + " pièces").build());

      inv.setItem(27, new ItemBuilder(Material.EXPERIENCE_BOTTLE).name("&b&lTaux d'intérêt").lore("&7De &e" + money(this.loans.minRate()) + " &7à &e" + money(this.loans.maxRate()) + " %").build());
      inv.setItem(28, new ItemBuilder(Material.RED_STAINED_GLASS_PANE).name("&c-5 %").build());
      inv.setItem(29, new ItemBuilder(Material.ORANGE_STAINED_GLASS_PANE).name("&6-1 %").build());
      inv.setItem(31, new ItemBuilder(Material.PAPER).name("&b&l" + money(h.rate) + " %").lore("&7Intérêts : &a+" + money(repay - h.amount) + " pièces", "&7À rendre : &e" + money(repay) + " pièces",
         "&7Délai : &f" + this.loans.loanDays() + " jours").build());
      inv.setItem(33, new ItemBuilder(Material.LIME_STAINED_GLASS_PANE).name("&a+1 %").build());
      inv.setItem(34, new ItemBuilder(Material.GREEN_STAINED_GLASS_PANE).name("&2+5 %").build());
      inv.setItem(35, new ItemBuilder(Material.BOOK).name("&7Taux par défaut").lore("&e" + money(this.loans.defaultRate()) + " %").build());

      inv.setItem(45, new ItemBuilder(Material.ARROW).name("&e◀ Retour").build());
      inv.setItem(49, new ItemBuilder(Material.LIME_CONCRETE).name("&a&lENVOYER L'OFFRE")
         .lore("&7Prêt de &e" + money(h.amount) + " pièces &7à &e" + money(h.rate) + " %", "&7" + borrower.getName() + " rendra &e" + money(repay) + " pièces", "&7sous " + this.loans.loanDays() + " jours.",
            "&8L'argent ne quitte notre banque", "&8que si l'offre est acceptée (10 min).")
         .glow().build());
      player.openInventory(inv);
   }

   // ------------------------------------------------------------------ clics

   @EventHandler(priority = EventPriority.HIGHEST)
   public void onClick(InventoryClickEvent event) {
      if (!(event.getInventory().getHolder() instanceof Holder h) || !(event.getWhoClicked() instanceof Player p)) {
         return;
      }

      event.setCancelled(true);
      if (event.getClickedInventory() != event.getView().getTopInventory()) {
         return;
      }

      Guild guild = this.plugin.getGuildManager().getGuildByPlayer(p.getUniqueId());
      if (guild == null || !guild.getId().equals(h.guildId)) {
         p.closeInventory();
         return;
      }

      int slot = event.getRawSlot();
      ClickType click = event.getClick();
      Object action = h.actions.get(slot);
      switch (h.mode) {
         case MAIN -> {
            if (slot == 53) {
               p.closeInventory();
               return;
            }

            if (slot == 48) {
               this.openPartners(p, guild, true);
               return;
            }

            if (slot == 50) {
               this.openPartners(p, guild, false);
               return;
            }

            if (action instanceof LoanManager.Proposal pr) {
               if (pr.borrowerId().equals(guild.getId())) {
                  if (click.isRightClick()) {
                     this.loans.refuseProposal(p, guild, pr);
                  } else {
                     this.loans.acceptProposal(p, guild, pr);
                  }
               } else if (this.loans.isOfficer(guild, p)) {
                  this.loans.withdrawProposal(pr);
                  ChatUtil.sendMessage(p, "&7Offre retirée.");
               } else {
                  ChatUtil.sendMessage(p, "&cRéservé aux officiers et au chef de guilde.");
               }
            } else if (action instanceof LoanManager.Loan l) {
               if (l.borrowerId.equals(guild.getId())) {
                  this.loans.repayLoan(p, guild, l, click.isRightClick() ? 1000.0 : l.owed);
               } else if (click == ClickType.SHIFT_RIGHT) {
                  this.loans.forgiveLoan(p, guild, l);
               } else {
                  return;
               }
            } else {
               return;
            }

            ChatUtil.playSound(p, Sound.UI_BUTTON_CLICK, 1.0F, 1.0F);
            this.openMain(p, guild);
         }
         case LEND_TO, CHEST_WITH -> {
            if (slot == 45) {
               this.openMain(p, guild);
               return;
            }

            Guild other = action instanceof String id ? this.plugin.getGuildManager().getGuildById(id) : null;
            if (other == null) {
               return;
            }

            if (h.mode == Mode.CHEST_WITH) {
               p.closeInventory();
               this.loans.openChestWith(p, guild, other);
            } else if (!this.loans.isOfficer(guild, p)) {
               ChatUtil.sendMessage(p, "&cRéservé aux officiers et au chef de guilde.");
            } else {
               this.openOffer(p, guild, other, 1000.0, this.loans.defaultRate());
            }
         }
         case OFFER -> {
            Guild borrower = this.plugin.getGuildManager().getGuildById(h.targetId);
            if (borrower == null) {
               this.openMain(p, guild);
               return;
            }

            if (slot == 45) {
               this.openPartners(p, guild, true);
               return;
            }

            double amount = h.amount;
            double rate = h.rate;
            switch (slot) {
               case 10 -> amount -= 1000;
               case 11 -> amount -= 100;
               case 15 -> amount += 100;
               case 16 -> amount += 1000;
               case 17 -> amount = this.maxFor(guild, borrower);
               case 28 -> rate -= 5;
               case 29 -> rate -= 1;
               case 33 -> rate += 1;
               case 34 -> rate += 5;
               case 35 -> rate = this.loans.defaultRate();
               case 49 -> {
                  if (this.loans.offer(p, guild, borrower, h.amount, h.rate)) {
                     ChatUtil.playSound(p, Sound.ENTITY_VILLAGER_YES, 1.0F, 1.2F);
                     this.openMain(p, guild);
                  } else {
                     ChatUtil.playSound(p, Sound.ENTITY_VILLAGER_NO, 1.0F, 1.0F);
                  }

                  return;
               }
               default -> {
                  return;
               }
            }

            ChatUtil.playSound(p, Sound.UI_BUTTON_CLICK, 1.0F, 1.2F);
            this.openOffer(p, guild, borrower, amount, rate);
         }
      }
   }

   @EventHandler(priority = EventPriority.HIGHEST)
   public void onDrag(InventoryDragEvent event) {
      if (event.getInventory().getHolder() instanceof Holder) {
         event.setCancelled(true);
      }
   }
}
