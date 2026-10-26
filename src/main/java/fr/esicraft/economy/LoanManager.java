package fr.esicraft.economy;

import fr.esicraft.EsicraftPlugin;
import fr.esicraft.guild.Guild;
import fr.esicraft.guild.GuildRole;
import fr.esicraft.treaty.Treaty;
import fr.esicraft.util.ChatUtil;
import fr.esicraft.util.ChunkKey;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import org.bukkit.Bukkit;
import org.bukkit.Sound;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;

/**
 * Banque de guilde : prêts entre guildes liées par un traité, avec intérêts, et coffre commun entre partenaires.
 * Une dette impayée (échéance dépassée) interdit toute revendication et fait saisir un chunk par jour.
 */
public class LoanManager implements Listener, CommandExecutor, TabCompleter {
   public static final String ACTIVE = "ACTIVE";
   public static final String OVERDUE = "OVERDUE";
   public static final String REPAID = "REPAID";

   public static final class Loan {
      final String id;
      final String lenderId;
      final String borrowerId;
      final double principal;
      volatile double owed;
      final long createdAt;
      final long dueAt;
      volatile String status;
      volatile long lastSeizure;

      Loan(String id, String lenderId, String borrowerId, double principal, double owed, long createdAt, long dueAt, String status, long lastSeizure) {
         this.id = id;
         this.lenderId = lenderId;
         this.borrowerId = borrowerId;
         this.principal = principal;
         this.owed = owed;
         this.createdAt = createdAt;
         this.dueAt = dueAt;
         this.status = status;
         this.lastSeizure = lastSeizure;
      }

      public String lenderId() {
         return this.lenderId;
      }

      public String borrowerId() {
         return this.borrowerId;
      }

      public double owed() {
         return this.owed;
      }

      public long dueAt() {
         return this.dueAt;
      }

      public String status() {
         return this.status;
      }
   }

   private record Proposal(String lenderId, String borrowerId, double amount, long expiresAt) {
   }

   private static final class ChestHolder implements InventoryHolder {
      final String pair;
      Inventory inventory;

      ChestHolder(String pair) {
         this.pair = pair;
      }

      public @NotNull Inventory getInventory() {
         return this.inventory;
      }
   }

   private final EsicraftPlugin plugin;
   private final Map<String, Loan> loans = new ConcurrentHashMap<>();
   private final List<Proposal> proposals = new CopyOnWriteArrayList<>();
   private final Map<String, Inventory> chests = new ConcurrentHashMap<>();
   // Un seul thread, dans l'ordre : un ancien instantané ne peut plus écraser un plus récent (dupe au redémarrage).
   private final java.util.concurrent.ExecutorService chestIo = java.util.concurrent.Executors.newSingleThreadExecutor(r -> {
      Thread t = new Thread(r, "esicraft-chest-io");
      t.setDaemon(false);
      return t;
   });

   private void saveChestAsync(String key, Inventory inv) {
      String data = Base64.getEncoder().encodeToString(ItemStack.serializeItemsAsBytes(inv.getContents()));
      this.chestIo.execute(() -> this.plugin.getDataManager().getStorage().exec(
         "INSERT INTO shared_chests (pair, data) VALUES (?, ?) ON CONFLICT(pair) DO UPDATE SET data = excluded.data;", key, data));
   }

   public LoanManager(EsicraftPlugin plugin) {
      this.plugin = plugin;
   }

   // ------------------------------------------------------------------ cycle de vie

   public void init() {
      var db = this.plugin.getDataManager().getStorage();
      db.exec("CREATE TABLE IF NOT EXISTS loans (id VARCHAR(40) PRIMARY KEY, lender VARCHAR(36), borrower VARCHAR(36), principal DOUBLE, owed DOUBLE, created BIGINT, due BIGINT, status VARCHAR(10), last_seizure BIGINT);");
      db.exec("CREATE TABLE IF NOT EXISTS shared_chests (pair VARCHAR(80) PRIMARY KEY, data TEXT);");
      for (Object[] r : db.select("SELECT id, lender, borrower, principal, owed, created, due, status, last_seizure FROM loans WHERE status != 'REPAID';", 9)) {
         this.loans.put((String)r[0], new Loan((String)r[0], (String)r[1], (String)r[2], ((Number)r[3]).doubleValue(), ((Number)r[4]).doubleValue(),
            ((Number)r[5]).longValue(), ((Number)r[6]).longValue(), (String)r[7], ((Number)r[8]).longValue()));
      }

      Bukkit.getScheduler().runTaskTimer(this.plugin, this::tick, 1200L, 6000L);
      // Un coffre ouvert est enregistré toutes les 30 s : un plantage ne perd plus que quelques secondes.
      Bukkit.getScheduler().runTaskTimer(this.plugin, () -> {
         for (Map.Entry<String, Inventory> e : this.chests.entrySet()) {
            if (!e.getValue().getViewers().isEmpty()) {
               this.saveChestAsync(e.getKey(), e.getValue());
            }
         }
      }, 600L, 600L);
   }

   public void clear() {
      this.loans.clear();
      this.proposals.clear();
      this.chests.clear();
   }

   /** Enregistre tous les coffres communs (arrêt du serveur). */
   public void shutdown() {
      for (Map.Entry<String, Inventory> e : this.chests.entrySet()) {
         this.saveChestAsync(e.getKey(), e.getValue());
      }

      this.chestIo.shutdown();
      try {
         this.chestIo.awaitTermination(10L, java.util.concurrent.TimeUnit.SECONDS);
      } catch (InterruptedException ignored) {
         Thread.currentThread().interrupt();
      }
   }

   private boolean enabled() {
      return this.plugin.getConfig().getBoolean("loans.enabled", true);
   }

   // ------------------------------------------------------------------ requêtes

   /** Vrai si la guilde doit encore de l'argent (elle ne peut alors pas se dissoudre pour effacer sa dette). */
   public boolean hasDebt(String borrowerId) {
      for (Loan l : this.loans.values()) {
         if (l.borrowerId.equals(borrowerId) && l.owed > 0.01) {
            return true;
         }
      }

      return false;
   }

   /** Dette imposée (indemnité de rupture ou tribut impayé faute de fonds) : le créancier la récupère comme un prêt arrivé à échéance dans {@code days} jours. */
   public void addForcedDebt(Guild lender, Guild borrower, double amount, int days) {
      if (lender == null || borrower == null || amount < 1.0 || lender.getId().equals(borrower.getId())) {
         return;
      }

      long now = System.currentTimeMillis();
      Loan loan = new Loan(UUID.randomUUID().toString(), lender.getId(), borrower.getId(), Math.ceil(amount), Math.ceil(amount), now, now + days * 86400000L, ACTIVE, 0L);
      this.loans.put(loan.id, loan);
      this.persist(loan);
      ChatUtil.broadcastGuild(borrower, "&c⚠ Fonds insuffisants : une dette de &e" + money(loan.owed) + " pièces &cenvers &6" + lender.getName() + " &cest enregistrée (à payer sous " + days + " jours, &e/banque dettes&c).");
      ChatUtil.broadcastGuild(lender, "&e💰 &6" + borrower.getName() + " &evous doit &6" + money(loan.owed) + " pièces &e(banque insuffisante) : dette enregistrée.");
   }

   public boolean hasOverdue(String borrowerId) {
      for (Loan l : this.loans.values()) {
         if (l.borrowerId.equals(borrowerId) && OVERDUE.equals(l.status)) {
            return true;
         }
      }

      return false;
   }

   public List<Loan> loansOf(String guildId) {
      List<Loan> list = new ArrayList<>();
      for (Loan l : this.loans.values()) {
         if (l.lenderId.equals(guildId) || l.borrowerId.equals(guildId)) {
            list.add(l);
         }
      }

      list.sort((a, b) -> Long.compare(a.createdAt, b.createdAt));
      return list;
   }

   private double debtOf(String borrowerId) {
      double sum = 0.0;
      for (Loan l : this.loans.values()) {
         if (l.borrowerId.equals(borrowerId)) {
            sum += l.owed;
         }
      }

      return sum;
   }

   private static String pairKey(String a, String b) {
      return a.compareTo(b) < 0 ? a + "|" + b : b + "|" + a;
   }

   private boolean linked(Guild a, Guild b) {
      return this.plugin.getTreatyManager().between(a.getId(), b.getId()) != null;
   }

   private boolean isOfficer(Guild g, Player p) {
      return g != null && g.hasRoleAtLeast(p.getUniqueId(), GuildRole.OFFICER);
   }

   private static String money(double v) {
      return String.format(Locale.ROOT, "%.0f", v);
   }

   private static String duration(long ms) {
      return fr.esicraft.treaty.TreatyManager.formatDuration(ms);
   }

   // ------------------------------------------------------------------ échéances et saisies

   private void persist(Loan l) {
      Bukkit.getScheduler().runTaskAsynchronously(this.plugin, () -> this.plugin.getDataManager().getStorage().exec(
         "INSERT INTO loans (id, lender, borrower, principal, owed, created, due, status, last_seizure) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?) "
            + "ON CONFLICT(id) DO UPDATE SET owed = excluded.owed, status = excluded.status, last_seizure = excluded.last_seizure;",
         l.id, l.lenderId, l.borrowerId, l.principal, l.owed, l.createdAt, l.dueAt, l.status, l.lastSeizure));
   }

   private void tick() {
      this.proposals.removeIf(p -> p.expiresAt() <= System.currentTimeMillis());
      long now = System.currentTimeMillis();
      long seizureMs = this.plugin.getConfig().getLong("loans.seizure-hours", 24L) * 3600000L;
      double value = this.plugin.getConfig().getDouble("loans.seizure-value", 300.0);
      for (Loan l : new ArrayList<>(this.loans.values())) {
         Guild lender = this.plugin.getGuildManager().getGuildById(l.lenderId);
         Guild borrower = this.plugin.getGuildManager().getGuildById(l.borrowerId);
         if (borrower == null || lender == null) {
            this.loans.remove(l.id);
            l.status = REPAID;
            this.persist(l);
            continue;
         }

         if (ACTIVE.equals(l.status) && l.dueAt <= now) {
            l.status = OVERDUE;
            l.lastSeizure = now;
            this.persist(l);
            ChatUtil.broadcastGuild(borrower, "&4⚠ &cDette impayée envers &6" + lender.getName() + " &c(&e" + money(l.owed) + " pièces&c) : revendications bloquées, un chunk saisi toutes les "
               + this.plugin.getConfig().getLong("loans.seizure-hours", 24L) + " h ! &e/banque rembourser " + lender.getName());
            ChatUtil.broadcastGuild(lender, "&e⚠ &6" + borrower.getName() + " &en'a pas remboursé son prêt à temps : ses territoires sont désormais saisissables.");
            this.plugin.getChronicle().log(fr.esicraft.life.Chronicle.Kind.CONTRACT, borrower.getName() + " ne rembourse pas sa dette envers " + lender.getName() + ".", lender.getId(), borrower.getId());
         } else if (OVERDUE.equals(l.status) && now - l.lastSeizure >= seizureMs / 4L && borrower.getBankBalance() >= 1.0) {
            // Prélèvement forcé sur la banque de l'emprunteur : vider sa banque ne suffit pas à échapper à la dette.
            double take = Math.min(l.owed, Math.floor(borrower.getBankBalance()));
            if (take >= 1.0 && borrower.withdrawBank(take)) {
               lender.depositBank(take);
               l.owed = Math.max(0.0, l.owed - take);
               this.plugin.getDataManager().saveGuild(borrower);
               this.plugin.getDataManager().saveGuild(lender);
               ChatUtil.broadcastGuild(borrower, "&4💸 &cPrélèvement forcé de &e" + money(take) + " pièces &csur votre banque (dette envers &6" + lender.getName() + "&c).");
               ChatUtil.broadcastGuild(lender, "&a💰 Prélèvement forcé : &e+" + money(take) + " pièces &asur la banque de &6" + borrower.getName() + "&a.");
               if (l.owed <= 0.01) {
                  this.settle(l, "soldée par prélèvement");
                  continue;
               }

               this.persist(l);
            }
         } else if (OVERDUE.equals(l.status) && now - l.lastSeizure >= seizureMs) {
            ChunkKey seized = this.plugin.getGuildManager().seizeOneChunk(borrower);
            l.lastSeizure = now;
            if (seized != null) {
               l.owed = Math.max(0.0, l.owed - value);
               ChatUtil.broadcastGuild(borrower, "&4🏴 &cUn chunk a été saisi pour dette impayée envers &6" + lender.getName() + " &c! La dette baisse de &e" + money(value) + "&c (reste &e" + money(l.owed) + "&c).");
               ChatUtil.broadcastGuild(lender, "&e🏴 Un chunk de &6" + borrower.getName() + " &eest saisi pour dette : &a-" + money(value) + " &esur ce qu'elle vous doit.");
               if (l.owed <= 0.01) {
                  this.settle(l, "soldée par saisies");
                  continue;
               }
            } else {
               ChatUtil.broadcastGuild(borrower, "&cDette impayée envers &6" + lender.getName() + " &c: remboursez avant qu'on ne vous prenne vos terres !");
            }

            this.persist(l);
         }
      }
   }

   private void settle(Loan l, String how) {
      l.status = REPAID;
      this.loans.remove(l.id);
      this.persist(l);
      Guild lender = this.plugin.getGuildManager().getGuildById(l.lenderId);
      Guild borrower = this.plugin.getGuildManager().getGuildById(l.borrowerId);
      if (lender != null && borrower != null) {
         ChatUtil.broadcastGuild(lender, "&a💰 Le prêt à &6" + borrower.getName() + " est " + how + ".");
         ChatUtil.broadcastGuild(borrower, "&a💰 Votre dette envers &6" + lender.getName() + " est " + how + ".");
      }
   }

   // ------------------------------------------------------------------ commande

   public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
      if (!(sender instanceof Player player)) {
         sender.sendMessage(ChatUtil.color("&cSeul un joueur peut exécuter cette commande."));
         return true;
      }

      if (!this.enabled()) {
         ChatUtil.sendMessage(player, "&cLa banque de guilde est désactivée.");
         return true;
      }

      Guild guild = this.plugin.getGuildManager().getGuildByPlayer(player.getUniqueId());
      if (guild == null) {
         ChatUtil.sendMessage(player, "&cVous devez appartenir à une guilde.");
         return true;
      }

      String sub = args.length == 0 ? "aide" : args[0].toLowerCase(Locale.ROOT);
      switch (sub) {
         case "pret", "prêt" -> this.propose(player, guild, args);
         case "accepter" -> this.accept(player, guild, args);
         case "refuser" -> this.refuse(player, guild, args);
         case "dettes", "dette", "prets" -> this.debts(player, guild);
         case "rembourser" -> this.repay(player, guild, args);
         case "pardonner" -> this.forgive(player, guild, args);
         case "coffre" -> this.openChest(player, guild, args);
         default -> this.help(player);
      }

      return true;
   }

   private void help(Player p) {
      ChatUtil.sendMessage(p, "&6🏦 Banque de guilde (entre guildes liées par un traité)");
      ChatUtil.sendRaw(p, " &e/banque pret <guilde> <montant> &7: proposer un prêt (intérêts " + money(this.plugin.getConfig().getDouble("loans.interest-percent", 10.0))
         + " %, " + this.plugin.getConfig().getLong("loans.days", 5L) + " jours)");
      ChatUtil.sendRaw(p, " &e/banque accepter|refuser <guilde> &7: répondre à une proposition");
      ChatUtil.sendRaw(p, " &e/banque dettes &7: prêts en cours · &e/banque rembourser <guilde> [montant] &7· &e/banque pardonner <guilde>");
      ChatUtil.sendRaw(p, " &e/banque coffre <guilde> &7: coffre commun avec un partenaire");
      ChatUtil.sendRaw(p, " &cUne dette impayée bloque les revendications et fait saisir un chunk par jour.");
   }

   private void propose(Player p, Guild lender, String[] args) {
      if (!this.isOfficer(lender, p)) {
         ChatUtil.sendMessage(p, "&cRéservé aux officiers et au chef de guilde.");
         return;
      }

      Guild borrower = args.length > 1 ? this.plugin.getGuildManager().getGuildByName(args[1]) : null;
      double amount;
      try {
         amount = Math.floor(Double.parseDouble(args.length > 2 ? args[2] : ""));
      } catch (NumberFormatException e) {
         ChatUtil.sendMessage(p, "&cUsage : &e/banque pret <guilde> <montant>");
         return;
      }

      double min = this.plugin.getConfig().getDouble("loans.min", 100.0);
      double max = this.plugin.getConfig().getDouble("loans.max", 20000.0);
      if (borrower == null || borrower.getId().equals(lender.getId())) {
         ChatUtil.sendMessage(p, "&c❌ Guilde introuvable.");
      } else if (!this.linked(lender, borrower)) {
         ChatUtil.sendMessage(p, "&c❌ Les prêts ne sont possibles qu'entre guildes liées par un traité (&e/traite&c).");
      } else if (amount < min || amount > max) {
         ChatUtil.sendMessage(p, "&c❌ Montant entre &e" + money(min) + " &cet &e" + money(max) + "&c.");
      } else if (lender.getBankBalance() < amount) {
         ChatUtil.sendMessage(p, "&c❌ La banque de votre guilde ne contient pas &e" + money(amount) + " &cpièces.");
      } else if (amount > this.plugin.getConfig().getDouble("loans.max-per-claim", 1500.0) * borrower.getClaimedChunks().size()) {
         ChatUtil.sendMessage(p, "&c❌ Cette guilde ne peut pas garantir un tel prêt : au plus &e" + money(this.plugin.getConfig().getDouble("loans.max-per-claim", 1500.0))
            + " &cpièces par chunk revendiqué (&e" + money(this.plugin.getConfig().getDouble("loans.max-per-claim", 1500.0) * borrower.getClaimedChunks().size()) + " &cici).");
      } else if (this.hasOverdue(borrower.getId())) {
         ChatUtil.sendMessage(p, "&c❌ &6" + borrower.getName() + " &caccuse déjà un retard de paiement.");
      } else if (this.debtOf(borrower.getId()) + amount > this.plugin.getConfig().getDouble("loans.max-total-debt", 30000.0)) {
         ChatUtil.sendMessage(p, "&c❌ Cette guilde serait trop endettée.");
      } else {
         this.proposals.removeIf(x -> x.lenderId().equals(lender.getId()) && x.borrowerId().equals(borrower.getId()));
         this.proposals.add(new Proposal(lender.getId(), borrower.getId(), amount, System.currentTimeMillis() + 600000L));
         double interest = this.plugin.getConfig().getDouble("loans.interest-percent", 10.0);
         ChatUtil.sendMessage(p, "&a🏦 Offre envoyée à &6" + borrower.getName() + "&a.");
         ChatUtil.broadcastGuild(borrower, "&6🏦 &e" + lender.getName() + " &6vous propose un prêt de &e" + money(amount) + " pièces &6(à rendre &e" + money(amount * (1 + interest / 100.0))
            + "&6 sous " + this.plugin.getConfig().getLong("loans.days", 5L) + " jours). &e/banque accepter " + lender.getName() + " &6ou &erefuser&6 (10 min).");
      }
   }

   private Proposal findProposal(Guild borrower, String lenderName) {
      Guild lender = lenderName == null ? null : this.plugin.getGuildManager().getGuildByName(lenderName);
      if (lender == null) {
         return null;
      }

      for (Proposal pr : this.proposals) {
         if (pr.lenderId().equals(lender.getId()) && pr.borrowerId().equals(borrower.getId()) && pr.expiresAt() > System.currentTimeMillis()) {
            return pr;
         }
      }

      return null;
   }

   private void refuse(Player p, Guild borrower, String[] args) {
      Proposal pr = this.findProposal(borrower, args.length > 1 ? args[1] : null);
      if (pr == null || !this.isOfficer(borrower, p)) {
         ChatUtil.sendMessage(p, "&cAucune offre valable (ou vous n'êtes pas officier).");
         return;
      }

      this.proposals.remove(pr);
      ChatUtil.sendMessage(p, "&eOffre refusée.");
      Guild lender = this.plugin.getGuildManager().getGuildById(pr.lenderId());
      if (lender != null) {
         ChatUtil.broadcastGuild(lender, "&c🏦 " + borrower.getName() + " a refusé votre offre de prêt.");
      }
   }

   private void accept(Player p, Guild borrower, String[] args) {
      Proposal pr = this.findProposal(borrower, args.length > 1 ? args[1] : null);
      if (pr == null || !this.isOfficer(borrower, p)) {
         ChatUtil.sendMessage(p, "&cAucune offre valable (ou vous n'êtes pas officier).");
         return;
      }

      Guild lender = this.plugin.getGuildManager().getGuildById(pr.lenderId());
      if (lender == null || !this.proposals.remove(pr)) {
         ChatUtil.sendMessage(p, "&cCette offre n'est plus valable.");
         return;
      }

      if (!this.linked(lender, borrower)) {
         ChatUtil.sendMessage(p, "&cLe traité qui vous lie a pris fin : prêt impossible.");
         return;
      }

      if (!lender.withdrawBank(pr.amount())) {
         ChatUtil.sendMessage(p, "&cLa banque du prêteur ne peut plus financer ce prêt.");
         return;
      }

      borrower.depositBank(pr.amount());
      long now = System.currentTimeMillis();
      double interest = this.plugin.getConfig().getDouble("loans.interest-percent", 10.0);
      Loan loan = new Loan(UUID.randomUUID().toString(), lender.getId(), borrower.getId(), pr.amount(), Math.ceil(pr.amount() * (1 + interest / 100.0)), now,
         now + this.plugin.getConfig().getLong("loans.days", 5L) * 86400000L, ACTIVE, 0L);
      this.loans.put(loan.id, loan);
      this.persist(loan);
      this.plugin.getDataManager().saveGuild(lender);
      this.plugin.getDataManager().saveGuild(borrower);
      ChatUtil.broadcastGuild(borrower, "&a🏦 Prêt accepté : &6+" + money(pr.amount()) + " pièces &adans la banque. À rendre : &e" + money(loan.owed) + " &asous " + duration(loan.dueAt - now) + ".");
      ChatUtil.broadcastGuild(lender, "&a🏦 &6" + borrower.getName() + " &aa accepté votre prêt de " + money(pr.amount()) + " (retour attendu : " + money(loan.owed) + ").");
   }

   private void debts(Player p, Guild guild) {
      List<Loan> mine = this.loansOf(guild.getId());
      if (mine.isEmpty()) {
         ChatUtil.sendMessage(p, "&7Aucun prêt en cours pour votre guilde.");
         return;
      }

      ChatUtil.sendMessage(p, "&6🏦 Prêts de " + guild.getName() + " :");
      for (Loan l : mine) {
         boolean borrowing = l.borrowerId.equals(guild.getId());
         Guild other = this.plugin.getGuildManager().getGuildById(borrowing ? l.lenderId : l.borrowerId);
         String name = other != null ? other.getName() : "?";
         long left = l.dueAt - System.currentTimeMillis();
         ChatUtil.sendRaw(p, (borrowing ? " &c▼ Dette envers &6" : " &a▲ Prêt à &6") + name + " &7: &e" + money(l.owed) + " &7pièces, "
            + (OVERDUE.equals(l.status) ? "&4EN RETARD" : "&7échéance dans &f" + duration(left)));
      }
   }

   private void repay(Player p, Guild borrower, String[] args) {
      if (!this.isOfficer(borrower, p)) {
         ChatUtil.sendMessage(p, "&cRéservé aux officiers et au chef de guilde.");
         return;
      }

      Guild lender = args.length > 1 ? this.plugin.getGuildManager().getGuildByName(args[1]) : null;
      Loan loan = null;
      if (lender != null) {
         for (Loan l : this.loans.values()) {
            if (l.lenderId.equals(lender.getId()) && l.borrowerId.equals(borrower.getId()) && (loan == null || l.createdAt < loan.createdAt)) {
               loan = l;
            }
         }
      }

      if (loan == null) {
         ChatUtil.sendMessage(p, "&cAucune dette envers cette guilde. &e/banque dettes");
         return;
      }

      double amount = loan.owed;
      if (args.length > 2) {
         try {
            amount = Math.min(loan.owed, Math.floor(Double.parseDouble(args[2])));
         } catch (NumberFormatException e) {
            ChatUtil.sendMessage(p, "&cUsage : &e/banque rembourser <guilde> [montant]");
            return;
         }
      }

      amount = Math.min(amount, Math.floor(borrower.getBankBalance()));
      if (amount < 1.0 || !borrower.withdrawBank(amount)) {
         ChatUtil.sendMessage(p, "&c❌ La banque de votre guilde est vide ou le montant est invalide.");
         return;
      }

      lender.depositBank(amount);
      loan.owed = Math.max(0.0, loan.owed - amount);
      this.plugin.getDataManager().saveGuild(lender);
      this.plugin.getDataManager().saveGuild(borrower);
      ChatUtil.broadcastGuild(lender, "&a💰 &6" + borrower.getName() + " &avous rembourse &e" + money(amount) + " &apièces.");
      if (loan.owed <= 0.01) {
         this.settle(loan, "intégralement remboursé");
      } else {
         ChatUtil.broadcastGuild(borrower, "&a💰 Remboursement de &e" + money(amount) + "&a. Reste dû : &e" + money(loan.owed) + "&a.");
         if (OVERDUE.equals(loan.status) && loan.dueAt > System.currentTimeMillis()) {
            loan.status = ACTIVE;
         }

         this.persist(loan);
      }
   }

   private void forgive(Player p, Guild lender, String[] args) {
      if (!this.isOfficer(lender, p)) {
         ChatUtil.sendMessage(p, "&cRéservé aux officiers et au chef de guilde.");
         return;
      }

      Guild borrower = args.length > 1 ? this.plugin.getGuildManager().getGuildByName(args[1]) : null;
      boolean any = false;
      if (borrower != null) {
         for (Loan l : new ArrayList<>(this.loans.values())) {
            if (l.lenderId.equals(lender.getId()) && l.borrowerId.equals(borrower.getId())) {
               this.settle(l, "effacé par le prêteur");
               any = true;
            }
         }
      }

      if (!any) {
         ChatUtil.sendMessage(p, "&cAucun prêt à effacer pour cette guilde.");
      }
   }

   // ------------------------------------------------------------------ coffre commun

   private void openChest(Player p, Guild guild, String[] args) {
      Guild other = args.length > 1 ? this.plugin.getGuildManager().getGuildByName(args[1]) : null;
      if (other == null || other.getId().equals(guild.getId())) {
         ChatUtil.sendMessage(p, "&cUsage : &e/banque coffre <guilde partenaire>");
         return;
      }

      Treaty t = this.plugin.getTreatyManager().between(guild.getId(), other.getId());
      if (t == null) {
         ChatUtil.sendMessage(p, "&c❌ Il faut un traité avec &6" + other.getName() + " &cpour partager un coffre.");
         return;
      }

      if (!guild.hasRoleAtLeast(p.getUniqueId(), GuildRole.MEMBER)) {
         ChatUtil.sendMessage(p, "&c❌ Le coffre commun est réservé aux membres confirmés (pas aux recrues).");
         return;
      }

      String key = pairKey(guild.getId(), other.getId());
      Inventory inv = this.chests.computeIfAbsent(key, k -> this.loadChest(k, guild.getName(), other.getName()));
      p.openInventory(inv);
      ChatUtil.playSound(p, Sound.BLOCK_CHEST_OPEN, 1.0F, 1.0F);
   }

   private Inventory loadChest(String key, String nameA, String nameB) {
      ChestHolder holder = new ChestHolder(key);
      Inventory inv = Bukkit.createInventory(holder, 54, ChatUtil.component("&8» &6Coffre commun · " + nameA + " & " + nameB));
      holder.inventory = inv;
      List<Object[]> rows = this.plugin.getDataManager().getStorage().select("SELECT data FROM shared_chests WHERE pair = ?;", 1, key);
      if (!rows.isEmpty() && rows.get(0)[0] != null) {
         try {
            ItemStack[] items = ItemStack.deserializeItemsFromBytes(Base64.getDecoder().decode((String)rows.get(0)[0]));
            for (int i = 0; i < items.length && i < inv.getSize(); i++) {
               inv.setItem(i, items[i]);
            }
         } catch (RuntimeException e) {
            this.plugin.getLogger().warning("Coffre commun illisible (" + key + ") : " + e.getMessage());
         }
      }

      return inv;
   }

   @EventHandler
   public void onClose(InventoryCloseEvent event) {
      if (event.getInventory().getHolder() instanceof ChestHolder holder) {
         this.saveChestAsync(holder.pair, event.getInventory());
      }
   }

   public List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command command, @NotNull String alias, @NotNull String[] args) {
      List<String> out = new ArrayList<>();
      if (args.length == 1) {
         for (String s : List.of("pret", "accepter", "refuser", "dettes", "rembourser", "pardonner", "coffre")) {
            if (s.startsWith(args[0].toLowerCase(Locale.ROOT))) {
               out.add(s);
            }
         }
      } else if (args.length == 2) {
         for (Guild g : this.plugin.getGuildManager().getAllGuilds()) {
            if (g.getName().toLowerCase(Locale.ROOT).startsWith(args[1].toLowerCase(Locale.ROOT))) {
               out.add(g.getName());
            }
         }
      }

      return out;
   }
}
