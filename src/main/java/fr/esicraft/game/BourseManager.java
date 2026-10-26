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
   private static final class Listing {
      final String guildId;
      int total;
      int treasury;
      double snapshot;
      long lastDividend;
      final Map<UUID, Integer> holders = new ConcurrentHashMap<>();

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
      db.exec("CREATE TABLE IF NOT EXISTS stock_holdings (guild_id VARCHAR(64), uuid VARCHAR(36), shares INT, PRIMARY KEY (guild_id, uuid));");
      for (Object[] row : db.select("SELECT guild_id, total, treasury, snapshot, last_div FROM stock_listings;", 5)) {
         this.listings.put(String.valueOf(row[0]), new Listing(String.valueOf(row[0]), ((Number)row[1]).intValue(), ((Number)row[2]).intValue(), ((Number)row[3]).doubleValue(), ((Number)row[4]).longValue()));
      }

      for (Object[] row : db.select("SELECT guild_id, uuid, shares FROM stock_holdings;", 3)) {
         Listing l = this.listings.get(String.valueOf(row[0]));
         try {
            if (l != null) {
               l.holders.put(UUID.fromString(String.valueOf(row[1])), ((Number)row[2]).intValue());
            }
         } catch (IllegalArgumentException ignored) {
         }
      }

      long period = Math.max(1L, this.plugin.getConfig().getLong("bourse.dividend-minutes", 360L)) * 1200L;
      Bukkit.getScheduler().runTaskTimer(this.plugin, this::dividends, period, Math.min(period, 6000L));
   }

   public void clear() {
      this.listings.clear();
   }

   private void saveListing(Listing l) {
      Runnable w = () -> this.plugin.getDataManager().getStorage().exec(
         "INSERT INTO stock_listings (guild_id, total, treasury, snapshot, last_div) VALUES (?, ?, ?, ?, ?) ON CONFLICT(guild_id) DO UPDATE SET total = excluded.total, treasury = excluded.treasury, snapshot = excluded.snapshot, last_div = excluded.last_div;",
         l.guildId, l.total, l.treasury, l.snapshot, l.lastDividend);
      this.async(w);
   }

   private void saveHolding(String guildId, UUID uuid, int shares) {
      this.async(() -> {
         var db = this.plugin.getDataManager().getStorage();
         if (shares <= 0) {
            db.exec("DELETE FROM stock_holdings WHERE guild_id = ? AND uuid = ?;", guildId, uuid.toString());
         } else {
            db.exec("INSERT INTO stock_holdings (guild_id, uuid, shares) VALUES (?, ?, ?) ON CONFLICT(guild_id, uuid) DO UPDATE SET shares = excluded.shares;", guildId, uuid.toString(), shares);
         }
      });
   }

   private void async(Runnable r) {
      if (this.plugin.isEnabled()) {
         Bukkit.getScheduler().runTaskAsynchronously(this.plugin, r);
      } else {
         r.run();
      }
   }

   /** Cours d'une part : prix plancher + banque de la guilde répartie sur l'ensemble des parts. */
   public double price(Guild guild, Listing l) {
      double floor = Math.max(1.0, this.plugin.getConfig().getDouble("bourse.floor-price", 10.0));
      return Math.round((floor + guild.getBankBalance() / Math.max(1, l.total)) * 100.0) / 100.0;
   }

   /** Une guilde dissoute disparaît de la cote (ses actions ne valent plus rien). */
   public void onGuildDisbanded(String guildId) {
      if (this.listings.remove(guildId) != null) {
         this.async(() -> {
            var db = this.plugin.getDataManager().getStorage();
            db.exec("DELETE FROM stock_listings WHERE guild_id = ?;", guildId);
            db.exec("DELETE FROM stock_holdings WHERE guild_id = ?;", guildId);
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

         double profit = g.getBankBalance() - l.snapshot;
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

         l.snapshot = g.getBankBalance();
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
         default -> this.list(player);
      }

      return true;
   }

   private void list(Player player) {
      ChatUtil.sendMessage(player, "&6📈 Bourse des guildes :");
      boolean any = false;
      for (Listing l : this.listings.values()) {
         Guild g = this.plugin.getGuildManager().getGuildById(l.guildId);
         if (g != null) {
            any = true;
            ChatUtil.sendRaw(player, " &8• &e" + g.getName() + " &7— cours &6" + String.format(Locale.ROOT, "%.2f", this.price(g, l)) + " &7| disponibles &a" + l.treasury + "&7/" + l.total
               + " &7| banque " + (long)g.getBankBalance());
         }
      }

      if (!any) {
         ChatUtil.sendRaw(player, " &7Aucune guilde cotée. Dès l'&eÂge 4&7, un chef de guilde peut ouvrir le capital avec &e/bourse ouvrir&7.");
      }

      ChatUtil.sendRaw(player, "&7/bourse acheter <guilde> <parts> &8| &7/bourse vendre <guilde> <parts> &8| &7/bourse portefeuille");
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
      Listing l = new Listing(g.getId(), total, total, g.getBankBalance(), System.currentTimeMillis());
      this.listings.put(g.getId(), l);
      this.saveListing(l);
      ChatUtil.broadcast("&6📈 &e" + g.getName() + " &6entre en bourse ! &7" + total + " parts disponibles, cours initial &6" + String.format(Locale.ROOT, "%.2f", this.price(g, l)) + " &7(/bourse).");
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

      double price = this.price(g, l);
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
      this.saveListing(l);
      this.saveHolding(g.getId(), player.getUniqueId(), l.holders.get(player.getUniqueId()));
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

      double proceeds = Math.floor(this.price(g, l) * n * Math.max(0.1, Math.min(1.0, this.plugin.getConfig().getDouble("bourse.buyback-ratio", 0.9))));
      if (!g.withdrawBank(proceeds)) {
         ChatUtil.sendMessage(player, "&c❌ La banque de la guilde ne peut pas racheter vos parts pour l'instant (" + (long)proceeds + " pièces nécessaires).");
         return;
      }

      l.snapshot = Math.min(l.snapshot, g.getBankBalance());
      this.plugin.getDataManager().saveGuild(g);
      this.plugin.getDataManager().creditPlayer(player.getUniqueId(), proceeds);
      int left = owned - n;
      if (left <= 0) {
         l.holders.remove(player.getUniqueId());
      } else {
         l.holders.put(player.getUniqueId(), left);
      }

      l.treasury += n;
      this.saveListing(l);
      this.saveHolding(g.getId(), player.getUniqueId(), left);
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
            double value = this.price(g, l) * owned;
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
            total += this.price(g, l) * l.holders.getOrDefault(uuid, 0);
         }
      }

      return total;
   }

   public List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command command, @NotNull String alias, @NotNull String[] args) {
      List<String> out = new ArrayList<>();
      if (args.length == 1) {
         out.addAll(List.of("liste", "ouvrir", "fermer", "acheter", "vendre", "portefeuille"));
      } else if (args.length == 2 && (args[0].equalsIgnoreCase("acheter") || args[0].equalsIgnoreCase("vendre"))) {
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
