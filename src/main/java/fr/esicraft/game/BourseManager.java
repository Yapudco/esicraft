package fr.esicraft.game;

import fr.esicraft.EsicraftPlugin;
import fr.esicraft.data.PlayerData;
import fr.esicraft.guild.Guild;
import fr.esicraft.util.ChatUtil;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ThreadLocalRandom;
import org.bukkit.Bukkit;
import org.bukkit.Sound;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

/**
 * Actions de guilde ({@code /bourse}) : à partir de l'Âge 4, une guilde ouvre son capital. Les joueurs extérieurs achètent des parts ; à intervalle régulier,
 * une part des bénéfices de la guilde (augmentation de sa banque : ventes, primes de raids, dons) est versée aux actionnaires au prorata de leurs parts.
 */
public class BourseManager implements CommandExecutor, TabCompleter {
   /** Décomposition de la valeur fondamentale d'une guilde (ce que le marché « croit » qu'elle vaut). */
   public record Fund(double equity, int age, int members, int chunks, double avgRep, long season, int techLevel, int defenses, double nexusPct, int diversity,
                      double ageMult, double repMult, double healthMult, double debtMult, double marketCap, double perShare) {
   }

   /** Ligne de cote (lecture seule) pour l'affichage et le site. */
   public record Quote(String guildId, String guildName, double price, double change24, int free, int total, int holders, Fund fund, List<Double> spark) {
   }

   private static final class Listing {
      volatile double price = 10.0;
      volatile double change24;
      volatile Fund fund;
      final String guildId;
      int total;
      int treasury;
      double snapshot;
      long lastDividend;
      final Map<UUID, Integer> holders = new ConcurrentHashMap<>();
      /** Prix payé au total par chaque actionnaire pour ses parts, et date de son dernier achat. */
      final Map<UUID, Double> costs = new ConcurrentHashMap<>();
      final Map<UUID, Long> since = new ConcurrentHashMap<>();

      Listing(String guildId, int total, int treasury, double snapshot, long lastDividend) {
         this.guildId = guildId;
         this.total = total;
         this.treasury = treasury;
         this.snapshot = snapshot;
         this.lastDividend = lastDividend;
      }

      int outstanding() {
         return this.holders.values().stream().mapToInt(Integer::intValue).sum();
      }
   }

   private final EsicraftPlugin plugin;
   private final Map<String, Listing> listings = new ConcurrentHashMap<>();

   public BourseManager(EsicraftPlugin plugin) {
      this.plugin = plugin;
   }

   public void init() {
      var db = this.plugin.getDataManager().getStorage();
      db.exec("CREATE TABLE IF NOT EXISTS stock_listings (guild_id VARCHAR(64) PRIMARY KEY, total INT, treasury INT, snapshot REAL, last_div BIGINT);");
      db.exec("CREATE TABLE IF NOT EXISTS stock_positions (guild_id VARCHAR(64), uuid VARCHAR(36), shares INT, cost REAL, since BIGINT, PRIMARY KEY (guild_id, uuid));");
      // Migration de l'ancienne table (sans prix de revient) : coût estimé au prix plancher.
      db.exec("INSERT OR IGNORE INTO stock_positions (guild_id, uuid, shares, cost, since) SELECT guild_id, uuid, shares, shares * 10.0, 0 FROM stock_holdings;");
      db.exec("DROP TABLE IF EXISTS stock_holdings;");
      for (Object[] row : db.select("SELECT guild_id, total, treasury, snapshot, last_div FROM stock_listings;", 5)) {
         this.listings.put(String.valueOf(row[0]), new Listing(String.valueOf(row[0]), ((Number)row[1]).intValue(), ((Number)row[2]).intValue(), ((Number)row[3]).doubleValue(), ((Number)row[4]).longValue()));
      }

      for (Object[] row : db.select("SELECT guild_id, uuid, shares, cost, since FROM stock_positions;", 5)) {
         Listing l = this.listings.get(String.valueOf(row[0]));
         try {
            if (l != null) {
               UUID id = UUID.fromString(String.valueOf(row[1]));
               l.holders.put(id, ((Number)row[2]).intValue());
               l.costs.put(id, ((Number)row[3]).doubleValue());
               l.since.put(id, ((Number)row[4]).longValue());
            }
         } catch (IllegalArgumentException ignored) {
         }
      }

      db.exec("CREATE TABLE IF NOT EXISTS stock_market (guild_id VARCHAR(64) PRIMARY KEY, price REAL);");
      db.exec("CREATE TABLE IF NOT EXISTS stock_history (guild_id VARCHAR(64), ts BIGINT, price REAL);");
      db.exec("CREATE INDEX IF NOT EXISTS idx_stock_history ON stock_history (guild_id, ts);");
      java.util.Set<String> priced = new java.util.HashSet<>();
      for (Object[] row : db.select("SELECT guild_id, price FROM stock_market;", 2)) {
         Listing l = this.listings.get(String.valueOf(row[0]));
         if (l != null) {
            l.price = ((Number)row[1]).doubleValue();
            priced.add(l.guildId);
         }
      }

      // Une guilde déjà cotée avant l'arrivée du marché démarre à sa valeur fondamentale.
      for (Listing l : this.listings.values()) {
         Guild g = this.plugin.getGuildManager().getGuildById(l.guildId);
         if (g != null && !priced.contains(l.guildId)) {
            l.fund = this.fundamentals(g, l);
            l.price = l.fund.perShare();
         }
      }

      long updateTicks = Math.max(1L, this.plugin.getConfig().getLong("bourse.update-minutes", 10L)) * 1200L;
      Bukkit.getScheduler().runTaskTimer(this.plugin, this::updateMarket, 200L, updateTicks);
      long period = Math.max(1L, this.plugin.getConfig().getLong("bourse.dividend-minutes", 360L)) * 1200L;
      Bukkit.getScheduler().runTaskTimer(this.plugin, this::dividends, period, Math.min(period, 6000L));
   }

   public void clear() {
      this.listings.clear();
      this.mood = 0.0;
   }

   // ------------------------------------------------------------------ le marché : valeur fondamentale, humeur, cours

   /** Humeur générale du marché (−0,2 à +0,2) : une marche aléatoire qui revient vers zéro et tire tous les cours dans le même sens. */
   private volatile double mood;

   private double weight(String key, double def) {
      return this.plugin.getConfig().getDouble("bourse.weights." + key, def);
   }

   /**
    * Valeur fondamentale : bien plus que les capitaux propres. Elle additionne la richesse (banque moins dettes), le territoire, les effectifs, la
    * réputation de ses membres, les résultats de la saison, la force du Nexus (niveau, défenses, vie) et la diversité des métiers, puis la module par
    * l'Âge de la guilde, la réputation moyenne (plus ses joueurs sont honorés, plus le multiplicateur monte), l'état du Nexus et d'éventuels impayés.
    */
   private Fund fundamentals(Guild g, Listing l) {
      double equity = Math.max(0.0, this.equity(g));
      int members = Math.max(1, g.getMembers().size());
      int chunks = g.getClaimedChunks().size();
      double sumRep = 0.0;
      for (UUID m : g.getMembers().keySet()) {
         sumRep += this.plugin.getReputation().get(m);
      }

      double avgRep = sumRep / members;
      long season = this.plugin.getSeasonManager().score(g);
      int age = g.getCurrentAge().getLevel();
      int tech = this.plugin.getNexusTech().level(g.getId());
      int defenses = this.plugin.getNexusTech().unlockedCount(g);
      double pct = g.hasNexus() && g.getNexus().isAlive() ? Math.max(0.0, Math.min(1.0, g.getNexus().getHealthPercentage() / 100.0)) : 0.0;
      int diversity = this.plugin.getSynergy().diversity(g);
      double base = equity * this.weight("equity", 1.0) + age * this.weight("age", 2000.0) + chunks * this.weight("chunk", 150.0) + members * this.weight("member", 300.0)
         + sumRep * this.weight("reputation", 40.0) + season * this.weight("season", 2.0) + (tech * this.weight("tech-level", 400.0) + defenses * this.weight("defense", 150.0)) * pct
         + diversity * this.weight("diversity", 250.0);
      double ageMult = 0.8 + 0.1 * age;
      double repMult = 1.0 + Math.min(0.5, avgRep / 100.0);
      double healthMult = g.hasNexus() && g.getNexus().isAlive() ? 0.6 + 0.4 * pct : 0.3;
      double debtMult = this.plugin.getLoans().hasOverdue(g.getId()) ? 0.85 : 1.0;
      double cap = base * ageMult * repMult * healthMult * debtMult * (1.0 + this.mood);
      double floor = Math.max(1.0, this.plugin.getConfig().getDouble("bourse.floor-price", 5.0));
      double perShare = Math.round((floor + cap / Math.max(1, l.total)) * 100.0) / 100.0;
      return new Fund(equity, age, members, chunks, avgRep, season, tech, defenses, pct, diversity, ageMult, repMult, healthMult, debtMult, cap, perShare);
   }

   private double minPrice() {
      return Math.max(1.0, this.plugin.getConfig().getDouble("bourse.min-price", 1.0));
   }

   private void savePrice(Listing l) {
      double p = l.price;
      this.async(() -> this.plugin.getDataManager().getStorage().exec("INSERT INTO stock_market (guild_id, price) VALUES (?, ?) ON CONFLICT(guild_id) DO UPDATE SET price = excluded.price;", l.guildId, p));
   }

   /** Mise à jour périodique : le cours est attiré par la valeur fondamentale (vitesse réglable) et bruité par la volatilité ; l'humeur du marché évolue. */
   private void updateMarket() {
      this.mood = Math.max(-0.2, Math.min(0.2, this.mood * 0.92 + ThreadLocalRandom.current().nextGaussian() * this.plugin.getConfig().getDouble("bourse.mood-volatility", 0.025)));
      double speed = Math.max(0.01, Math.min(1.0, this.plugin.getConfig().getDouble("bourse.reversion-speed", 0.15)));
      double vol = Math.max(0.0, this.plugin.getConfig().getDouble("bourse.volatility", 0.02));
      long now = System.currentTimeMillis();
      var db = this.plugin.getDataManager().getStorage();
      for (Listing l : this.listings.values()) {
         Guild g = this.plugin.getGuildManager().getGuildById(l.guildId);
         if (g == null) {
            continue;
         }

         Fund f = this.fundamentals(g, l);
         l.fund = f;
         double p = l.price;
         p += speed * (f.perShare() - p) + p * vol * ThreadLocalRandom.current().nextGaussian();
         l.price = Math.round(Math.max(this.minPrice(), p) * 100.0) / 100.0;
         double price = l.price;
         String id = l.guildId;
         this.async(() -> {
            db.exec("INSERT INTO stock_history (guild_id, ts, price) VALUES (?, ?, ?);", id, now, price);
            db.exec("DELETE FROM stock_history WHERE guild_id = ? AND ts < ?;", id, now - 3L * 86400000L);
         });
         this.savePrice(l);
         var rows = db.select("SELECT price FROM stock_history WHERE guild_id = ? AND ts <= ? ORDER BY ts DESC LIMIT 1;", 1, id, now - 86400000L);
         if (rows.isEmpty()) {
            rows = db.select("SELECT price FROM stock_history WHERE guild_id = ? ORDER BY ts ASC LIMIT 1;", 1, id);
         }

         double ref = rows.isEmpty() ? price : ((Number)rows.get(0)[0]).doubleValue();
         l.change24 = ref <= 0.0 ? 0.0 : (price - ref) / ref;
      }
   }

   /**
    * Choc de cours lié à un événement de la guilde (horde repoussée, siège, Nexus détruit…) : le prix bouge aussitôt, avant même la prochaine mise à jour.
    */
   public void shock(String guildId, double pct, String reason) {
      Listing l = guildId == null ? null : this.listings.get(guildId);
      if (l == null || pct == 0.0) {
         return;
      }

      double before = l.price;
      l.price = Math.round(Math.max(this.minPrice(), before * (1.0 + pct)) * 100.0) / 100.0;
      this.savePrice(l);
      Guild g = this.plugin.getGuildManager().getGuildById(guildId);
      for (UUID holder : l.holders.keySet()) {
         Player p = Bukkit.getPlayer(holder);
         if (p != null && g != null) {
            ChatUtil.sendMessage(p, (pct > 0 ? "&a📈 " : "&c📉 ") + "&e" + g.getName() + " &7: " + (pct > 0 ? "+" : "") + Math.round(pct * 100) + " % (" + reason + ") — cours &6" + String.format(Locale.ROOT, "%.2f", l.price));
         }
      }
   }

   /** Cotes actuelles (copie pour le site et les commandes). */
   public List<Quote> quotes() {
      List<Quote> out = new ArrayList<>();
      var db = this.plugin.getDataManager().getStorage();
      for (Listing l : this.listings.values()) {
         Guild g = this.plugin.getGuildManager().getGuildById(l.guildId);
         if (g == null) {
            continue;
         }

         List<Double> spark = new ArrayList<>();
         for (Object[] r : db.select("SELECT price FROM stock_history WHERE guild_id = ? ORDER BY ts DESC LIMIT 36;", 1, l.guildId)) {
            spark.add(0, ((Number)r[0]).doubleValue());
         }

         out.add(new Quote(l.guildId, g.getName(), l.price, l.change24, l.treasury, l.total, l.holders.size(), l.fund, spark));
      }

      out.sort((a, b) -> Double.compare(b.price() * b.total(), a.price() * a.total()));
      return out;
   }

   public double moodValue() {
      return this.mood;
   }

   private void saveListing(Listing l) {
      Runnable w = () -> this.plugin.getDataManager().getStorage().exec(
         "INSERT INTO stock_listings (guild_id, total, treasury, snapshot, last_div) VALUES (?, ?, ?, ?, ?) ON CONFLICT(guild_id) DO UPDATE SET total = excluded.total, treasury = excluded.treasury, snapshot = excluded.snapshot, last_div = excluded.last_div;",
         l.guildId, l.total, l.treasury, l.snapshot, l.lastDividend);
      this.async(w);
   }

   private void saveHolding(String guildId, UUID uuid, int shares, double cost, long since) {
      this.async(() -> {
         var db = this.plugin.getDataManager().getStorage();
         if (shares <= 0) {
            db.exec("DELETE FROM stock_positions WHERE guild_id = ? AND uuid = ?;", guildId, uuid.toString());
         } else {
            db.exec("INSERT INTO stock_positions (guild_id, uuid, shares, cost, since) VALUES (?, ?, ?, ?, ?) ON CONFLICT(guild_id, uuid) DO UPDATE SET shares = excluded.shares, cost = excluded.cost, since = excluded.since;",
               guildId, uuid.toString(), shares, cost, since);
         }
      });
   }

   /** Capitaux propres de la guilde : banque moins ses dettes nettes. Un prêt reçu n'est donc jamais un bénéfice distribuable. */
   private double equity(Guild g) {
      return g.getBankBalance() - this.plugin.getLoans().netDebt(g.getId());
   }

   private void async(Runnable r) {
      if (this.plugin.isEnabled()) {
         Bukkit.getScheduler().runTaskAsynchronously(this.plugin, r);
      } else {
         r.run();
      }
   }

   /** Cours d'une part sur le marché. */
   public double price(Guild guild, Listing l) {
      return l.price;
   }

   /** Une guilde dissoute disparaît de la cote (ses actions ne valent plus rien). */
   public void onGuildDisbanded(String guildId) {
      if (this.listings.remove(guildId) != null) {
         this.async(() -> {
            var db = this.plugin.getDataManager().getStorage();
            db.exec("DELETE FROM stock_listings WHERE guild_id = ?;", guildId);
            db.exec("DELETE FROM stock_positions WHERE guild_id = ?;", guildId);
            db.exec("DELETE FROM stock_market WHERE guild_id = ?;", guildId);
            db.exec("DELETE FROM stock_history WHERE guild_id = ?;", guildId);
         });
      }
   }

   // ------------------------------------------------------------------ dividendes

   private void dividends() {
      long now = System.currentTimeMillis();
      long every = Math.max(1L, this.plugin.getConfig().getLong("bourse.dividend-minutes", 360L)) * 60000L;
      double percent = Math.max(0.0, Math.min(90.0, this.plugin.getConfig().getDouble("bourse.dividend-percent", 20.0))) / 100.0;
      for (Listing l : this.listings.values()) {
         if (now - l.lastDividend < every) {
            continue;
         }

         Guild g = this.plugin.getGuildManager().getGuildById(l.guildId);
         if (g == null) {
            continue;
         }

         double profit = this.equity(g) - l.snapshot;
         int outstanding = l.outstanding();
         if (profit > 0.0 && outstanding > 0) {
            double pool = Math.floor(profit * percent);
            if (pool > 0.0 && g.withdrawBank(pool)) {
               double perShare = pool / l.total;
               for (Map.Entry<UUID, Integer> h : l.holders.entrySet()) {
                  double due = Math.floor(perShare * h.getValue());
                  if (due > 0.0) {
                     this.plugin.getDataManager().creditPlayer(h.getKey(), due);
                     Player p = Bukkit.getPlayer(h.getKey());
                     if (p != null) {
                        ChatUtil.sendMessage(p, "&6📈 Dividende de &e" + g.getName() + " &6: &a+" + (long)due + " pièces &7pour vos " + h.getValue() + " part(s).");
                     }
                  }
               }

               this.plugin.getDataManager().saveGuild(g);
               ChatUtil.broadcastGuild(g, "&6📈 Dividendes versés à vos actionnaires : &e" + (long)pool + " pièces &6(bénéfice de la période : " + (long)profit + ").");
            }
         }

         l.snapshot = this.equity(g);
         l.lastDividend = now;
         this.saveListing(l);
      }
   }

   // ------------------------------------------------------------------ commande

   public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
      if (!(sender instanceof Player player)) {
         sender.sendMessage(ChatUtil.color("&cSeul un joueur peut exécuter cette commande."));
         return true;
      }

      if (!this.plugin.getConfig().getBoolean("bourse.enabled", true)) {
         ChatUtil.sendMessage(player, "&cLa bourse est fermée.");
         return true;
      }

      String sub = args.length == 0 ? "liste" : args[0].toLowerCase(Locale.ROOT);
      switch (sub) {
         case "ouvrir" -> this.open(player);
         case "fermer" -> this.close(player);
         case "acheter" -> this.buy(player, args);
         case "vendre" -> this.sell(player, args);
         case "portefeuille", "wallet" -> this.wallet(player);
         case "info" -> this.info(player, args);
         default -> this.list(player);
      }

      return true;
   }

   private static String arrow(double change) {
      return change > 0.0005 ? "&a▲ +" + String.format(Locale.ROOT, "%.1f", change * 100) + " %" : change < -0.0005 ? "&c▼ " + String.format(Locale.ROOT, "%.1f", change * 100) + " %" : "&7■ 0,0 %";
   }

   private void list(Player player) {
      String humeur = this.mood > 0.05 ? "&aoptimiste" : this.mood < -0.05 ? "&cpessimiste" : "&7neutre";
      ChatUtil.sendMessage(player, "&6📈 Bourse des guildes &7— marché " + humeur + " &7(" + (this.mood >= 0 ? "+" : "") + String.format(Locale.ROOT, "%.1f", this.mood * 100) + " %)");
      boolean any = false;
      for (Quote q : this.quotes()) {
         any = true;
         ChatUtil.sendRaw(player, " &8• &e" + q.guildName() + " &7— &6" + String.format(Locale.ROOT, "%.2f", q.price()) + " " + arrow(q.change24()) + " &7| disponibles &a" + q.free() + "&7/" + q.total() + " &7| actionnaires " + q.holders());
      }

      if (!any) {
         ChatUtil.sendRaw(player, " &7Aucune guilde cotée. Dès l'&eÂge 4&7, un chef de guilde peut ouvrir le capital avec &e/bourse ouvrir&7.");
      }

      ChatUtil.sendRaw(player, "&7/bourse info <guilde> &8| &7/bourse acheter <guilde> <parts> &8| &7/bourse vendre <guilde> <parts> &8| &7/bourse portefeuille");
   }

   private void info(Player player, String[] args) {
      Guild g = args.length > 1 ? this.plugin.getGuildManager().getGuildByName(String.join(" ", java.util.Arrays.copyOfRange(args, 1, args.length))) : this.plugin.getGuildManager().getGuildByPlayer(player.getUniqueId());
      Listing l = g == null ? null : this.listings.get(g.getId());
      if (l == null) {
         ChatUtil.sendMessage(player, "&c❌ Cette guilde n'est pas cotée. Usage : &e/bourse info <guilde>");
         return;
      }

      Fund f = l.fund != null ? l.fund : this.fundamentals(g, l);
      ChatUtil.sendMessage(player, "&6📊 &e" + g.getName() + " &7— cours &6" + String.format(Locale.ROOT, "%.2f", l.price) + " " + arrow(l.change24) + " &7(valeur fondamentale &e" + String.format(Locale.ROOT, "%.2f", f.perShare()) + "&7)");
      ChatUtil.sendRaw(player, " &7Richesse : capitaux propres &6" + (long)f.equity() + " &7· territoire &f" + f.chunks() + " chunks &7· effectif &f" + f.members());
      ChatUtil.sendRaw(player, " &7Réputation moyenne : &c❤" + String.format(Locale.ROOT, "%.1f", f.avgRep()) + " &7(multiplicateur ×" + String.format(Locale.ROOT, "%.2f", f.repMult()) + ")");
      ChatUtil.sendRaw(player, " &7Âge " + f.age() + " (×" + String.format(Locale.ROOT, "%.1f", f.ageMult()) + ") · saison &f" + f.season() + " pts &7· métiers &f" + f.diversity() + "/6");
      ChatUtil.sendRaw(player, " &7Nexus : niveau &d" + f.techLevel() + " &7· défenses &f" + f.defenses() + " &7· vie &f" + (int)Math.round(f.nexusPct() * 100) + " % &7(×" + String.format(Locale.ROOT, "%.2f", f.healthMult()) + ")"
         + (f.debtMult() < 1.0 ? " &c· impayés (×0,85)" : ""));
      ChatUtil.sendRaw(player, " &7Capitalisation estimée : &6" + (long)f.marketCap() + " pièces &7· le cours se rapproche de la valeur fondamentale, avec l'humeur du marché, des chocs (raids, sièges) et vos ordres.");
   }

   private void open(Player player) {
      Guild g = this.plugin.getGuildManager().getGuildByPlayer(player.getUniqueId());
      if (g == null || !g.getLeaderUuid().equals(player.getUniqueId())) {
         ChatUtil.sendMessage(player, "&c❌ Seul le chef de guilde peut ouvrir le capital.");
         return;
      }

      int minAge = this.plugin.getConfig().getInt("bourse.min-age", 4);
      if (g.getCurrentAge().getLevel() < minAge) {
         ChatUtil.sendMessage(player, "&c❌ L'ouverture du capital exige l'Âge " + minAge + ".");
         return;
      }

      if (this.listings.containsKey(g.getId())) {
         ChatUtil.sendMessage(player, "&cVotre guilde est déjà cotée.");
         return;
      }

      int total = Math.max(10, this.plugin.getConfig().getInt("bourse.total-shares", 1000));
      Listing l = new Listing(g.getId(), total, total, this.equity(g), System.currentTimeMillis());
      l.fund = this.fundamentals(g, l);
      l.price = l.fund.perShare();
      this.listings.put(g.getId(), l);
      this.saveListing(l);
      this.savePrice(l);
      ChatUtil.broadcast("&6📈 &e" + g.getName() + " &6entre en bourse ! &7" + total + " parts disponibles, cours initial &6" + String.format(Locale.ROOT, "%.2f", l.price) + " &7(/bourse).");
   }

   private void close(Player player) {
      Guild g = this.plugin.getGuildManager().getGuildByPlayer(player.getUniqueId());
      Listing l = g == null ? null : this.listings.get(g.getId());
      if (l == null || !g.getLeaderUuid().equals(player.getUniqueId())) {
         ChatUtil.sendMessage(player, "&c❌ Votre guilde n'est pas cotée, ou vous n'en êtes pas le chef.");
         return;
      }

      if (l.outstanding() > 0) {
         ChatUtil.sendMessage(player, "&c❌ Des actionnaires détiennent encore " + l.outstanding() + " part(s) : ils doivent d'abord vendre.");
         return;
      }

      this.onGuildDisbanded(g.getId());
      ChatUtil.sendMessage(player, "&7Votre guilde quitte la bourse.");
   }

   private Guild guildArg(Player player, String[] args) {
      if (args.length < 3) {
         ChatUtil.sendMessage(player, "&cUsage : &e/bourse " + args[0] + " <guilde> <parts>");
         return null;
      }

      Guild g = this.plugin.getGuildManager().getGuildByName(args[1]);
      if (g == null || !this.listings.containsKey(g.getId())) {
         ChatUtil.sendMessage(player, "&c❌ Cette guilde n'est pas cotée en bourse.");
         return null;
      }

      return g;
   }

   private int shares(Player player, String raw) {
      try {
         int n = Integer.parseInt(raw);
         if (n >= 1 && n <= 100000) {
            return n;
         }
      } catch (NumberFormatException ignored) {
      }

      ChatUtil.sendMessage(player, "&c❌ Nombre de parts invalide.");
      return -1;
   }

   private void buy(Player player, String[] args) {
      Guild g = this.guildArg(player, args);
      int n = g == null ? -1 : this.shares(player, args[2]);
      if (g == null || n < 0) {
         return;
      }

      Listing l = this.listings.get(g.getId());
      if (g.isMember(player.getUniqueId())) {
         ChatUtil.sendMessage(player, "&c❌ Seuls les joueurs extérieurs peuvent acheter des parts de votre guilde.");
         return;
      }

      if (this.plugin.getCombatManager().denySpending(player)) {
         return;
      }

      int perPlayerMax = Math.max(1, this.plugin.getConfig().getInt("bourse.max-shares-per-player", 100));
      int owned = l.holders.getOrDefault(player.getUniqueId(), 0);
      int maxPublic = (int)(l.total * Math.max(0.0, Math.min(0.9, this.plugin.getConfig().getDouble("bourse.max-public-percent", 49.0) / 100.0)));
      if (n > l.treasury || owned + n > perPlayerMax || l.outstanding() + n > maxPublic) {
         ChatUtil.sendMessage(player, "&c❌ Achat impossible : parts disponibles " + l.treasury + ", plafond par joueur " + perPlayerMax + " (vous en avez " + owned + "), le public ne peut détenir que "
            + maxPublic + " parts au total.");
         return;
      }

      double price = l.price;
      double cost = Math.ceil(price * n);
      PlayerData data = this.plugin.getDataManager().getPlayerData(player.getUniqueId());
      if (data == null || !data.removeCoins(cost)) {
         ChatUtil.sendMessage(player, "&c❌ Il vous faut &6" + (long)cost + " pièces&c.");
         return;
      }

      this.plugin.getDataManager().savePlayer(data);
      g.depositBank(cost);
      // Le capital apporté ne compte pas comme un bénéfice distribuable.
      l.snapshot += cost;
      this.plugin.getDataManager().saveGuild(g);
      l.treasury -= n;
      l.holders.merge(player.getUniqueId(), n, Integer::sum);
      l.costs.merge(player.getUniqueId(), cost, Double::sum);
      long bought = System.currentTimeMillis();
      l.since.put(player.getUniqueId(), bought);
      // Une demande forte fait monter le cours : acheter 10 % du capital le relève de 8 % (bourse.impact).
      l.price = Math.round(l.price * (1.0 + this.plugin.getConfig().getDouble("bourse.impact", 0.8) * n / Math.max(1, l.total)) * 100.0) / 100.0;
      this.savePrice(l);
      this.saveListing(l);
      this.saveHolding(g.getId(), player.getUniqueId(), l.holders.get(player.getUniqueId()), l.costs.get(player.getUniqueId()), bought);
      this.plugin.getAchievements().grant(player.getUniqueId(), fr.esicraft.life.Achievement.SHAREHOLDER);
      ChatUtil.sendMessage(player, "&a📈 Vous achetez &e" + n + " part(s) de " + g.getName() + " &apour &6" + (long)cost + " pièces&a.");
      ChatUtil.broadcastGuild(g, "&6📈 &e" + player.getName() + " &6investit &e" + (long)cost + " pièces &6dans votre guilde (" + n + " parts).");
      ChatUtil.playSound(player, Sound.ENTITY_EXPERIENCE_ORB_PICKUP, 1.0F, 1.0F);
   }

   private void sell(Player player, String[] args) {
      Guild g = this.guildArg(player, args);
      int n = g == null ? -1 : this.shares(player, args[2]);
      if (g == null || n < 0) {
         return;
      }

      Listing l = this.listings.get(g.getId());
      int owned = l.holders.getOrDefault(player.getUniqueId(), 0);
      if (owned < n) {
         ChatUtil.sendMessage(player, "&c❌ Vous ne possédez que " + owned + " part(s) de cette guilde.");
         return;
      }

      long holdMs = this.plugin.getConfig().getLong("bourse.min-hold-hours", 24L) * 3600000L;
      long boughtAt = l.since.getOrDefault(player.getUniqueId(), 0L);
      if (System.currentTimeMillis() - boughtAt < holdMs && !this.plugin.bypassCooldowns(player)) {
         ChatUtil.sendMessage(player, "&c❌ Vous devez garder vos parts au moins &e" + (holdMs / 3600000L) + " h&c après le dernier achat avant de les revendre.");
         return;
      }

      // Revente à la guilde : 90 % du cours, et jamais plus de 1,5 fois le prix de revient (une guilde ne peut pas vider sa banque vers un complice).
      double avgCost = l.costs.getOrDefault(player.getUniqueId(), 0.0) / Math.max(1, owned);
      double marketValue = l.price * n * Math.max(0.1, Math.min(1.0, this.plugin.getConfig().getDouble("bourse.buyback-ratio", 0.95)));
      double proceeds = Math.floor(Math.min(marketValue, avgCost * n * Math.max(1.0, this.plugin.getConfig().getDouble("bourse.max-gain-ratio", 3.0))));
      if (!g.withdrawBank(proceeds)) {
         ChatUtil.sendMessage(player, "&c❌ La banque de la guilde ne peut pas racheter vos parts pour l'instant (" + (long)proceeds + " pièces nécessaires).");
         return;
      }

      l.snapshot = Math.min(l.snapshot, this.equity(g));
      this.plugin.getDataManager().saveGuild(g);
      this.plugin.getDataManager().creditPlayer(player.getUniqueId(), proceeds);
      int left = owned - n;
      double remainingCost = l.costs.getOrDefault(player.getUniqueId(), 0.0) - avgCost * n;
      if (left <= 0) {
         l.holders.remove(player.getUniqueId());
         l.costs.remove(player.getUniqueId());
         l.since.remove(player.getUniqueId());
      } else {
         l.holders.put(player.getUniqueId(), left);
         l.costs.put(player.getUniqueId(), Math.max(0.0, remainingCost));
      }

      l.treasury += n;
      // L'offre fait baisser le cours.
      l.price = Math.round(Math.max(this.minPrice(), l.price * (1.0 - this.plugin.getConfig().getDouble("bourse.impact", 0.8) * n / Math.max(1, l.total))) * 100.0) / 100.0;
      this.savePrice(l);
      this.saveListing(l);
      this.saveHolding(g.getId(), player.getUniqueId(), left, Math.max(0.0, remainingCost), boughtAt);
      ChatUtil.sendMessage(player, "&a📈 Vous revendez &e" + n + " part(s) de " + g.getName() + " &apour &6" + (long)proceeds + " pièces&a.");
   }

   private void wallet(Player player) {
      ChatUtil.sendMessage(player, "&6📈 Votre portefeuille :");
      boolean any = false;
      double total = 0.0;
      for (Listing l : this.listings.values()) {
         int owned = l.holders.getOrDefault(player.getUniqueId(), 0);
         Guild g = this.plugin.getGuildManager().getGuildById(l.guildId);
         if (owned > 0 && g != null) {
            any = true;
            double value = l.price * owned;
            total += value;
            ChatUtil.sendRaw(player, " &8• &e" + g.getName() + " &7: &f" + owned + " part(s) &7≈ &6" + (long)value + " pièces");
         }
      }

      ChatUtil.sendRaw(player, any ? " &7Valeur totale : &6" + (long)total + " pièces" : " &7Vous ne possédez aucune action.");
   }

   /** Valeur en bourse des actions d'un joueur (pour le classement de richesse éventuel). */
   public double portfolioValue(UUID uuid) {
      double total = 0.0;
      for (Listing l : this.listings.values()) {
         Guild g = this.plugin.getGuildManager().getGuildById(l.guildId);
         if (g != null) {
            total += l.price * l.holders.getOrDefault(uuid, 0);
         }
      }

      return total;
   }

   public List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command command, @NotNull String alias, @NotNull String[] args) {
      List<String> out = new ArrayList<>();
      if (args.length == 1) {
         out.addAll(List.of("liste", "info", "ouvrir", "fermer", "acheter", "vendre", "portefeuille"));
      } else if (args.length == 2 && (args[0].equalsIgnoreCase("acheter") || args[0].equalsIgnoreCase("vendre") || args[0].equalsIgnoreCase("info"))) {
         for (String id : this.listings.keySet()) {
            Guild g = this.plugin.getGuildManager().getGuildById(id);
            if (g != null) {
               out.add(g.getName());
            }
         }
      }

      return out;
   }
}
