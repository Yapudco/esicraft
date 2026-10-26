package fr.esicraft.game;

import fr.esicraft.EsicraftPlugin;
import fr.esicraft.guild.Guild;
import fr.esicraft.util.ChatUtil;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.event.ClickEvent;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

/**
 * Tribunal des guildes : chaque lundi, les membres de guilde votent avec {@code /vote} la règle qui s'appliquera à toute la semaine suivante
 * (du mardi au lundi). Chaque guilde pèse une voix (son choix = l'option la plus votée par ses membres), l'égalité se départage au nombre de votants.
 */
public class CourtManager implements CommandExecutor {
   public enum Rule {
      TOLL_TAX("Taxe de passage", "Les péages ferroviaires sont doublés : les guildes qui possèdent des rails s'enrichissent."),
      BUILD_BONUS("Bonus de construction", "Le coût des nouveaux claims est réduit de 25 %."),
      FREE_MARKET("Marché libre", "L'Hôtel des Ventes ne prélève plus aucune taxe, pour tout le monde."),
      BOUNTY_BOOM("Chasse ouverte", "Les primes de chasse aux monstres sont majorées de 25 %."),
      LONG_TRUCE("Trêve prolongée", "La trêve hebdomadaire sans PvP commence aussi le jeudi soir et dure deux fois plus longtemps.");

      final String label;
      final String effect;

      Rule(String label, String effect) {
         this.label = label;
         this.effect = effect;
      }

      public String label() {
         return this.label;
      }

      public String effect() {
         return this.effect;
      }
   }

   private static final String K_ACTIVE = "court_active";
   private static final String K_FINALIZED = "court_finalized";
   private static final String K_ANNOUNCED = "court_announced";

   private final EsicraftPlugin plugin;
   /** Votes de la semaine de vote en cours : joueur → règle. */
   private final Map<UUID, Rule> votes = new ConcurrentHashMap<>();
   private volatile Rule active;
   private volatile String finalizedWeek = "";
   private volatile String announcedWeek = "";
   private volatile String loadedWeek = "";

   public CourtManager(EsicraftPlugin plugin) {
      this.plugin = plugin;
   }

   public void init() {
      var db = this.plugin.getDataManager().getStorage();
      db.exec("CREATE TABLE IF NOT EXISTS court_votes (week VARCHAR(12), uuid VARCHAR(36), rule VARCHAR(24), PRIMARY KEY (week, uuid));");
      String saved = this.kvGet(K_ACTIVE);
      this.active = saved == null || saved.isEmpty() ? null : parse(saved);
      this.finalizedWeek = this.nullToEmpty(this.kvGet(K_FINALIZED));
      this.announcedWeek = this.nullToEmpty(this.kvGet(K_ANNOUNCED));
      this.reloadVotes(this.voteWeek());
      Bukkit.getScheduler().runTaskTimer(this.plugin, this::tick, 400L, 1200L);
   }

   private String nullToEmpty(String s) {
      return s == null ? "" : s;
   }

   private static Rule parse(String name) {
      try {
         return Rule.valueOf(name);
      } catch (IllegalArgumentException e) {
         return null;
      }
   }

   private String kvGet(String key) {
      for (Object[] row : this.plugin.getDataManager().getStorage().select("SELECT v FROM kv WHERE k = ?;", 1, key)) {
         return String.valueOf(row[0]);
      }

      return null;
   }

   private void kvSet(String key, String value) {
      Runnable write = () -> this.plugin.getDataManager().getStorage().exec("INSERT INTO kv (k, v) VALUES (?, ?) ON CONFLICT(k) DO UPDATE SET v = excluded.v;", key, value);
      if (this.plugin.isEnabled()) {
         Bukkit.getScheduler().runTaskAsynchronously(this.plugin, write);
      } else {
         write.run();
      }
   }

   public void clear() {
      this.votes.clear();
      this.active = null;
      this.finalizedWeek = "";
      this.announcedWeek = "";
      this.loadedWeek = "";
   }

   /** Règle en vigueur (null = aucune). */
   public Rule getActive() {
      return this.active;
   }

   public boolean is(Rule rule) {
      return this.active == rule;
   }

   // ------------------------------------------------------------------ semaines

   private static LocalDate mondayOf(LocalDate day) {
      return day.minusDays(day.getDayOfWeek().getValue() - 1L);
   }

   /** Clé de la semaine de vote (le lundi le plus récent). */
   private String voteWeek() {
      return mondayOf(LocalDate.now()).toString();
   }

   private boolean votingOpen() {
      return LocalDate.now().getDayOfWeek() == DayOfWeek.MONDAY;
   }

   private void reloadVotes(String week) {
      this.votes.clear();
      for (Object[] row : this.plugin.getDataManager().getStorage().select("SELECT uuid, rule FROM court_votes WHERE week = ?;", 2, week)) {
         Rule r = parse(String.valueOf(row[1]));
         try {
            if (r != null) {
               this.votes.put(UUID.fromString(String.valueOf(row[0])), r);
            }
         } catch (IllegalArgumentException ignored) {
         }
      }

      this.loadedWeek = week;
   }

   private void tick() {
      String week = this.voteWeek();
      if (!week.equals(this.loadedWeek)) {
         this.reloadVotes(week);
      }

      if (this.votingOpen()) {
         if (!week.equals(this.announcedWeek)) {
            this.announcedWeek = week;
            this.kvSet(K_ANNOUNCED, week);
            ChatUtil.broadcast("&6⚖ &e&lTribunal des guildes &7— c'est lundi ! Votez la règle de la semaine avec &e/vote&7 (jusqu'à minuit).");
         }
      } else if (!week.equals(this.finalizedWeek)) {
         this.finalizeWeek(week);
      }
   }

   /** Dépouille le scrutin du lundi {@code week} et met en vigueur la règle élue jusqu'au lundi suivant. */
   private void finalizeWeek(String week) {
      this.finalizedWeek = week;
      this.kvSet(K_FINALIZED, week);
      Rule winner = this.tally();
      this.active = winner;
      this.kvSet(K_ACTIVE, winner == null ? "" : winner.name());
      if (winner == null) {
         ChatUtil.broadcast("&6⚖ &7Le Tribunal n'a rendu aucun verdict (personne n'a voté) : aucune règle spéciale cette semaine.");
      } else {
         ChatUtil.broadcast("&6⚖ &e&lVerdict du Tribunal &7: &6" + winner.label + " &7— " + winner.effect);
         this.plugin.getSeasonManager().addHistory(fr.esicraft.life.Chronicle.Kind.EVENT, "⚖ Le Tribunal des guildes a voté : " + winner.label + ".", null, null);
      }
   }

   private Rule tally() {
      // Choix de chaque guilde = règle la plus votée par ses membres.
      Map<String, Map<Rule, Integer>> perGuild = new HashMap<>();
      Map<Rule, Integer> voters = new HashMap<>();
      for (Map.Entry<UUID, Rule> e : this.votes.entrySet()) {
         Guild g = this.plugin.getGuildManager().getGuildByPlayer(e.getKey());
         if (g == null) {
            continue;
         }

         perGuild.computeIfAbsent(g.getId(), k -> new HashMap<>()).merge(e.getValue(), 1, Integer::sum);
         voters.merge(e.getValue(), 1, Integer::sum);
      }

      Map<Rule, Integer> guildVotes = new HashMap<>();
      for (Map<Rule, Integer> counts : perGuild.values()) {
         Rule best = null;
         for (Rule r : Rule.values()) {
            if (counts.containsKey(r) && (best == null || counts.get(r) > counts.get(best))) {
               best = r;
            }
         }

         if (best != null) {
            guildVotes.merge(best, 1, Integer::sum);
         }
      }

      Rule winner = null;
      for (Rule r : Rule.values()) {
         if (!guildVotes.containsKey(r)) {
            continue;
         }

         if (winner == null || guildVotes.get(r) > guildVotes.get(winner)
            || guildVotes.get(r).equals(guildVotes.get(winner)) && voters.getOrDefault(r, 0) > voters.getOrDefault(winner, 0)) {
            winner = r;
         }
      }

      return winner;
   }

   /** Décompte actuel : règle → nombre de votants. */
   public Map<Rule, Integer> counts() {
      Map<Rule, Integer> counts = new java.util.EnumMap<>(Rule.class);
      for (Rule r : this.votes.values()) {
         counts.merge(r, 1, Integer::sum);
      }

      return counts;
   }

   // ------------------------------------------------------------------ commande

   public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
      if (!(sender instanceof Player player)) {
         sender.sendMessage(ChatUtil.color("&cSeul un joueur peut exécuter cette commande."));
         return true;
      }

      Rule[] rules = Rule.values();
      if (args.length == 0) {
         this.show(player, rules);
         return true;
      }

      int n;
      try {
         n = Integer.parseInt(args[0]);
      } catch (NumberFormatException e) {
         ChatUtil.sendMessage(player, "&cUsage : &e/vote <numéro de la règle>");
         return true;
      }

      if (n < 1 || n > rules.length) {
         ChatUtil.sendMessage(player, "&c❌ Choisissez un numéro entre 1 et " + rules.length + ".");
         return true;
      }

      if (!this.votingOpen() && !this.plugin.bypassCooldowns(player)) {
         ChatUtil.sendMessage(player, "&c❌ Le Tribunal ne siège que le lundi.");
         return true;
      }

      Guild guild = this.plugin.getGuildManager().getGuildByPlayer(player.getUniqueId());
      if (guild == null) {
         ChatUtil.sendMessage(player, "&c❌ Seuls les membres d'une guilde peuvent voter.");
         return true;
      }

      Rule rule = rules[n - 1];
      String week = this.voteWeek();
      this.votes.put(player.getUniqueId(), rule);
      UUID id = player.getUniqueId();
      Bukkit.getScheduler().runTaskAsynchronously(this.plugin, () -> this.plugin.getDataManager().getStorage().exec(
         "INSERT INTO court_votes (week, uuid, rule) VALUES (?, ?, ?) ON CONFLICT(week, uuid) DO UPDATE SET rule = excluded.rule;", week, id.toString(), rule.name()));
      ChatUtil.sendMessage(player, "&a⚖ Vote enregistré pour &e" + rule.label + "&a au nom de &e" + guild.getName() + "&a (vous pouvez changer d'avis jusqu'à minuit).");
      return true;
   }

   private void show(Player player, Rule[] rules) {
      ChatUtil.sendRaw(player, "&6⚖ &e&lTribunal des guildes");
      Rule act = this.active;
      ChatUtil.sendRaw(player, "&7Règle en vigueur : " + (act == null ? "&8aucune" : "&6" + act.label + " &7— " + act.effect));
      if (!this.votingOpen()) {
         ChatUtil.sendRaw(player, "&7Le scrutin s'ouvre le &elundi&7. Revenez alors voter la règle de la semaine suivante !");
         return;
      }

      ChatUtil.sendRaw(player, "&7Scrutin ouvert jusqu'à minuit — cliquez pour voter :");
      Map<Rule, Integer> counts = this.counts();
      Rule mine = this.votes.get(player.getUniqueId());
      List<Component> lines = new ArrayList<>();
      for (int i = 0; i < rules.length; i++) {
         Rule r = rules[i];
         Component line = Component.text(" " + (i + 1) + ". ", NamedTextColor.GOLD)
            .append(Component.text(r.label, r == mine ? NamedTextColor.GREEN : NamedTextColor.YELLOW))
            .append(Component.text(" (" + counts.getOrDefault(r, 0) + ") ", NamedTextColor.GRAY))
            .append(Component.text("— " + r.effect, NamedTextColor.DARK_GRAY))
            .clickEvent(ClickEvent.runCommand("/vote " + (i + 1)));
         lines.add(line);
      }

      for (Component line : lines) {
         player.sendMessage(line);
      }
   }
}
