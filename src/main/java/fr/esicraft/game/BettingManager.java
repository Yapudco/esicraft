package fr.esicraft.game;

import fr.esicraft.EsicraftPlugin;
import fr.esicraft.data.PlayerData;
import fr.esicraft.guild.Guild;
import fr.esicraft.util.ChatUtil;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Predicate;
import org.bukkit.Bukkit;
import org.bukkit.Sound;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

/**
 * Paris des spectateurs : {@code /pari <camp> <montant>} pendant les premières minutes d'un raid ou d'un duel. Le camp vainqueur
 * se partage la cagnotte (moins {@code betting.tax-percent} % prélevés par le serveur) ; sans adversaire ou en cas d'annulation, les mises sont remboursées.
 */
public class BettingManager implements CommandExecutor, TabCompleter {
   private static final class Pool {
      final String key;
      final String title;
      final String[] sides;
      final String[][] aliases;
      final long closesAt;
      final Predicate<UUID> excluded;
      final List<Map<UUID, Double>> stakes = new ArrayList<>();

      Pool(String key, String title, String[] sides, String[][] aliases, long closesAt, Predicate<UUID> excluded) {
         this.key = key;
         this.title = title;
         this.sides = sides;
         this.aliases = aliases;
         this.closesAt = closesAt;
         this.excluded = excluded;
         for (int i = 0; i < sides.length; i++) {
            this.stakes.add(new LinkedHashMap<>());
         }
      }

      double total(int side) {
         return this.stakes.get(side).values().stream().mapToDouble(Double::doubleValue).sum();
      }
   }

   private final EsicraftPlugin plugin;
   private final Map<String, Pool> pools = new ConcurrentHashMap<>();

   public BettingManager(EsicraftPlugin plugin) {
      this.plugin = plugin;
   }

   private int windowSeconds() {
      return Math.max(10, this.plugin.getConfig().getInt("betting.window-seconds", 120));
   }

   // ------------------------------------------------------------------ ouverture / règlement

   private void open(String key, String title, String[] sides, String[][] aliases, Predicate<UUID> excluded) {
      if (!this.plugin.getConfig().getBoolean("betting.enabled", true)) {
         return;
      }

      this.pools.put(key, new Pool(key, title, sides, aliases, System.currentTimeMillis() + this.windowSeconds() * 1000L, excluded));
      ChatUtil.broadcast("&6🎲 &e&lParis ouverts &7— " + title + " &7: &e/pari <" + String.join("|", sides) + "> <montant> &7(" + this.windowSeconds() + " s, réservé aux spectateurs).");
   }

   /** Un raid commence : on parie sur la guilde attaquante ou la guilde défensive. */
   public void openRaid(Guild attacker, Guild target) {
      if (attacker == null || target == null) {
         return;
      }

      String a = attacker.getName();
      String d = target.getName();
      this.open("raid:" + target.getId(), "Raid de &c" + a + " &7contre &a" + d, new String[]{"attaquante", "defensive"},
         new String[][]{{"attaquante", "attaquant", "attaque", a.toLowerCase(Locale.ROOT)}, {"defensive", "defense", "défensive", "défense", d.toLowerCase(Locale.ROOT)}},
         id -> attacker.isMember(id) || target.isMember(id));
   }

   public void settleRaid(String targetGuildId, boolean defended) {
      this.settle("raid:" + targetGuildId, defended ? 1 : 0);
   }

   public void cancelRaid(String targetGuildId) {
      this.cancel("raid:" + targetGuildId);
   }

   /** Un duel est lancé : on parie sur l'un des deux duellistes. */
   public void openDuel(String duelId, Player a, Player b) {
      this.open("duel:" + duelId, "Duel &e" + a.getName() + " &7contre &e" + b.getName(), new String[]{a.getName(), b.getName()},
         new String[][]{{a.getName().toLowerCase(Locale.ROOT)}, {b.getName().toLowerCase(Locale.ROOT)}}, id -> id.equals(a.getUniqueId()) || id.equals(b.getUniqueId()));
   }

   public void settleDuel(String duelId, int winnerIndex) {
      this.settle("duel:" + duelId, winnerIndex);
   }

   public void cancelDuel(String duelId) {
      this.cancel("duel:" + duelId);
   }

   private void settle(String key, int winner) {
      Pool pool = this.pools.remove(key);
      if (pool == null) {
         return;
      }

      double winners = pool.total(winner);
      double all = 0.0;
      for (int i = 0; i < pool.sides.length; i++) {
         all += pool.total(i);
      }

      if (all <= 0.0) {
         return;
      }

      if (winners <= 0.0 || winners >= all) {
         this.refund(pool, "personne n'a parié contre le camp vainqueur");
         return;
      }

      double tax = Math.max(0.0, Math.min(50.0, this.plugin.getConfig().getDouble("betting.tax-percent", 5.0))) / 100.0;
      double payoutPool = all * (1.0 - tax);
      for (Map.Entry<UUID, Double> e : pool.stakes.get(winner).entrySet()) {
         double gain = Math.floor(payoutPool * e.getValue() / winners);
         this.plugin.getDataManager().creditPlayer(e.getKey(), gain);
         Player p = Bukkit.getPlayer(e.getKey());
         if (p != null) {
            ChatUtil.sendMessage(p, "&a🎲 Pari gagnant sur &e" + pool.sides[winner] + " &a! Vous touchez &6" + (long)gain + " pièces &7(mise " + (long)(double)e.getValue() + ").");
            ChatUtil.playSound(p, Sound.ENTITY_PLAYER_LEVELUP, 1.0F, 1.4F);
         }
      }

      ChatUtil.broadcast("&6🎲 Paris clos : &e" + pool.sides[winner] + " &7l'emporte. Cagnotte de &6" + (long)all + " pièces &7(taxe serveur " + (int)Math.round(tax * 100) + " %).");
   }

   private void cancel(String key) {
      Pool pool = this.pools.remove(key);
      if (pool != null) {
         this.refund(pool, "annulé");
      }
   }

   private void refund(Pool pool, String why) {
      boolean any = false;
      for (Map<UUID, Double> side : pool.stakes) {
         for (Map.Entry<UUID, Double> e : side.entrySet()) {
            any = true;
            this.plugin.getDataManager().creditPlayer(e.getKey(), e.getValue());
            Player p = Bukkit.getPlayer(e.getKey());
            if (p != null) {
               ChatUtil.sendMessage(p, "&e🎲 Pari remboursé (" + why + ") : &6" + (long)(double)e.getValue() + " pièces&7.");
            }
         }
      }

      if (any) {
         ChatUtil.broadcast("&6🎲 Paris remboursés (" + why + ").");
      }
   }

   /** À l'arrêt du serveur : toutes les mises en cours sont rendues. */
   public void shutdown() {
      for (Pool pool : List.copyOf(this.pools.values())) {
         this.refund(pool, "arrêt du serveur");
      }

      this.pools.clear();
   }

   public void clear() {
      this.pools.clear();
   }

   // ------------------------------------------------------------------ commande

   public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
      if (!(sender instanceof Player player)) {
         sender.sendMessage(ChatUtil.color("&cSeul un joueur peut exécuter cette commande."));
         return true;
      }

      long now = System.currentTimeMillis();
      this.pools.values().removeIf(p -> p.closesAt <= now && p.stakes.stream().allMatch(Map::isEmpty));
      if (args.length < 2) {
         ChatUtil.sendMessage(player, "&6🎲 Paris en cours :");
         boolean any = false;
         for (Pool p : this.pools.values()) {
            any = true;
            long left = Math.max(0L, (p.closesAt - now) / 1000L);
            StringBuilder sb = new StringBuilder(" &8• " + p.title + " &7— ");
            for (int i = 0; i < p.sides.length; i++) {
               sb.append("&e").append(p.sides[i]).append(" &7(").append((long)p.total(i)).append(")  ");
            }

            sb.append(left > 0 ? "&8[" + left + " s]" : "&8[clos]");
            ChatUtil.sendRaw(player, sb.toString());
         }

         if (!any) {
            ChatUtil.sendRaw(player, " &7Aucun pari ouvert. Ils s'ouvrent au début d'un raid ou d'un duel.");
         }

         ChatUtil.sendRaw(player, "&7Usage : &e/pari <camp> <montant>");
         return true;
      }

      double amount;
      try {
         amount = Double.parseDouble(args[args.length - 1].replace(',', '.'));
      } catch (NumberFormatException e) {
         ChatUtil.sendMessage(player, "&c❌ Montant invalide.");
         return true;
      }

      String camp = String.join(" ", java.util.Arrays.copyOf(args, args.length - 1)).toLowerCase(Locale.ROOT);
      Pool found = null;
      int side = -1;
      int matches = 0;
      for (Pool p : this.pools.values()) {
         for (int i = 0; i < p.sides.length; i++) {
            for (String alias : p.aliases[i]) {
               if (alias.equals(camp) || p.sides[i].equalsIgnoreCase(camp)) {
                  found = p;
                  side = i;
                  matches++;
                  break;
               }
            }
         }
      }

      if (found == null) {
         ChatUtil.sendMessage(player, "&c❌ Aucun pari ouvert pour « " + camp + " ». Tapez &e/pari &cpour voir la liste.");
         return true;
      }

      if (matches > 1) {
         ChatUtil.sendMessage(player, "&c❌ Plusieurs paris correspondent : précisez le nom de la guilde ou du joueur.");
         return true;
      }

      if (found.closesAt <= now) {
         ChatUtil.sendMessage(player, "&c❌ Les paris sont clos pour cette bataille.");
         return true;
      }

      if (found.excluded.test(player.getUniqueId())) {
         ChatUtil.sendMessage(player, "&c❌ Vous êtes impliqué dans ce conflit : seuls les spectateurs peuvent parier.");
         return true;
      }

      double min = Math.max(1.0, this.plugin.getConfig().getDouble("betting.min", 10.0));
      double max = Math.max(min, this.plugin.getConfig().getDouble("betting.max", 5000.0));
      if (Double.isNaN(amount) || Double.isInfinite(amount) || amount < min || amount > max) {
         ChatUtil.sendMessage(player, "&c❌ La mise doit être comprise entre &e" + (long)min + " &cet &e" + (long)max + " &cpièces.");
         return true;
      }

      amount = Math.floor(amount);
      if (this.plugin.getCombatManager().denySpending(player)) {
         return true;
      }

      for (Map<UUID, Double> s : found.stakes) {
         if (s != found.stakes.get(side) && s.containsKey(player.getUniqueId())) {
            ChatUtil.sendMessage(player, "&c❌ Vous avez déjà parié sur l'autre camp.");
            return true;
         }
      }

      PlayerData data = this.plugin.getDataManager().getPlayerData(player.getUniqueId());
      if (data == null || !data.removeCoins(amount)) {
         ChatUtil.sendMessage(player, "&c❌ Vous n'avez pas assez de pièces.");
         return true;
      }

      this.plugin.getDataManager().savePlayer(data);
      found.stakes.get(side).merge(player.getUniqueId(), amount, Double::sum);
      ChatUtil.sendMessage(player, "&a🎲 Vous avez parié &6" + (long)amount + " pièces &asur &e" + found.sides[side] + "&a.");
      return true;
   }

   public List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command command, @NotNull String alias, @NotNull String[] args) {
      List<String> out = new ArrayList<>();
      if (args.length == 1) {
         String prefix = args[0].toLowerCase(Locale.ROOT);
         for (Pool p : this.pools.values()) {
            for (String side : p.sides) {
               if (side.toLowerCase(Locale.ROOT).startsWith(prefix)) {
                  out.add(side);
               }
            }
         }
      }

      return out;
   }
}
