package fr.esicraft.social;

import fr.esicraft.EsicraftPlugin;
import fr.esicraft.guild.Guild;
import fr.esicraft.race.Race;
import fr.esicraft.util.ChatUtil;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.Sound;
import org.bukkit.Statistic;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

/**
 * Parrainage : un joueur avancé guide une recrue ({@code /parrain <joueur>}) et reçoit une récompense quand elle franchit
 * des jalons : premier métier choisi, premier territoire revendiqué par sa guilde.
 */
public class MentorManager implements CommandExecutor, TabCompleter {
   private record Offer(UUID mentor, long expiresAt) {
   }

   /** Lien de parrainage ; {@code raceDone} / {@code claimDone} : jalons déjà récompensés. */
   private static final class Bond {
      final UUID recruit;
      final UUID mentor;
      final long since;
      volatile boolean raceDone;
      volatile boolean claimDone;

      Bond(UUID recruit, UUID mentor, long since, boolean raceDone, boolean claimDone) {
         this.recruit = recruit;
         this.mentor = mentor;
         this.since = since;
         this.raceDone = raceDone;
         this.claimDone = claimDone;
      }
   }

   private final EsicraftPlugin plugin;
   private final Map<UUID, Bond> bonds = new ConcurrentHashMap<>();
   private final Map<UUID, Offer> offers = new ConcurrentHashMap<>();

   public MentorManager(EsicraftPlugin plugin) {
      this.plugin = plugin;
   }

   public void init() {
      var db = this.plugin.getDataManager().getStorage();
      db.exec("CREATE TABLE IF NOT EXISTS mentoring (recruit VARCHAR(36) PRIMARY KEY, mentor VARCHAR(36), since BIGINT, race_done INT DEFAULT 0, claim_done INT DEFAULT 0);");
      for (Object[] row : db.select("SELECT recruit, mentor, since, race_done, claim_done FROM mentoring;", 5)) {
         try {
            UUID recruit = UUID.fromString((String)row[0]);
            this.bonds.put(recruit, new Bond(recruit, UUID.fromString((String)row[1]), ((Number)row[2]).longValue(), ((Number)row[3]).intValue() != 0, ((Number)row[4]).intValue() != 0));
         } catch (IllegalArgumentException ignored) {
         }
      }

      Bukkit.getScheduler().runTaskTimer(this.plugin, this::tick, 400L, 400L);
   }

   public void clear() {
      this.bonds.clear();
      this.offers.clear();
   }

   private boolean enabled() {
      return this.plugin.getConfig().getBoolean("mentoring.enabled", true);
   }

   private double reward(String key, double def) {
      return this.plugin.getConfig().getDouble("mentoring.rewards." + key, def);
   }

   private List<Bond> recruitsOf(UUID mentor) {
      List<Bond> list = new ArrayList<>();
      for (Bond b : this.bonds.values()) {
         if (b.mentor.equals(mentor)) {
            list.add(b);
         }
      }

      return list;
   }

   private String nameOf(UUID uuid) {
      OfflinePlayer op = Bukkit.getOfflinePlayer(uuid);
      return op.getName() != null ? op.getName() : "?";
   }

   private boolean isAdvanced(Player p) {
      if (this.plugin.getRaceManager().getRace(p) == Race.NONE || this.plugin.getGuildManager().getGuildByPlayer(p.getUniqueId()) == null) {
         return false;
      }

      long minHours = this.plugin.getConfig().getLong("mentoring.mentor-min-hours", 5L);
      return p.getStatistic(Statistic.PLAY_ONE_MINUTE) / 72000L >= minHours;
   }

   private boolean isRecruit(Player p) {
      long maxDays = this.plugin.getConfig().getLong("mentoring.recruit-max-days", 14L);
      boolean recent = p.getFirstPlayed() <= 0L || System.currentTimeMillis() - p.getFirstPlayed() <= maxDays * 86400000L;
      Guild g = this.plugin.getGuildManager().getGuildByPlayer(p.getUniqueId());
      boolean raceLeft = this.plugin.getRaceManager().getRace(p) == Race.NONE;
      boolean claimLeft = g == null || g.getClaimedChunks().isEmpty();
      return recent && (raceLeft || claimLeft);
   }

   // ------------------------------------------------------------------ jalons

   private void tick() {
      if (!this.enabled()) {
         return;
      }

      for (Bond b : this.bonds.values()) {
         Player recruit = Bukkit.getPlayer(b.recruit);
         if (recruit == null) {
            continue;
         }

         if (!b.raceDone && this.plugin.getRaceManager().getRace(recruit) != Race.NONE) {
            b.raceDone = true;
            this.pay(b, "race", "premier métier", this.reward("first-job-mentor", 200.0), this.reward("first-job-recruit", 50.0), recruit);
         }

         Guild g = this.plugin.getGuildManager().getGuildByPlayer(b.recruit);
         if (!b.claimDone && g != null && !g.getClaimedChunks().isEmpty()) {
            b.claimDone = true;
            this.pay(b, "claim", "premier territoire", this.reward("first-claim-mentor", 300.0), this.reward("first-claim-recruit", 100.0), recruit);
         }
      }
   }

   private void pay(Bond b, String column, String milestone, double mentorGain, double recruitGain, Player recruit) {
      String col = column.equals("race") ? "race_done" : "claim_done";
      Bukkit.getScheduler().runTaskAsynchronously(this.plugin, () -> this.plugin.getDataManager().getStorage()
         .exec("UPDATE mentoring SET " + col + " = 1 WHERE recruit = ?;", b.recruit.toString()));
      this.plugin.getDataManager().creditPlayer(b.mentor, mentorGain);
      this.plugin.getDataManager().creditPlayer(b.recruit, recruitGain);
      ChatUtil.sendMessage(recruit, "&a🎓 Jalon atteint : &e" + milestone + " &a! Vous recevez &6+" + String.format(Locale.ROOT, "%.0f", recruitGain)
         + " pièces &aet votre parrain &e" + this.nameOf(b.mentor) + " &a" + String.format(Locale.ROOT, "%.0f", mentorGain) + ".");
      Player mentor = Bukkit.getPlayer(b.mentor);
      if (mentor != null) {
         ChatUtil.sendMessage(mentor, "&a🎓 Votre recrue &e" + recruit.getName() + " &aa atteint le jalon &e" + milestone + " &a: &6+"
            + String.format(Locale.ROOT, "%.0f", mentorGain) + " pièces&a !");
         ChatUtil.playSound(mentor, Sound.ENTITY_PLAYER_LEVELUP, 1.0F, 1.3F);
      }

      ChatUtil.playSound(recruit, Sound.ENTITY_PLAYER_LEVELUP, 1.0F, 1.3F);
   }

   // ------------------------------------------------------------------ commande

   public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
      if (!(sender instanceof Player player)) {
         sender.sendMessage(ChatUtil.color("&cSeul un joueur peut exécuter cette commande."));
         return true;
      }

      if (!this.enabled()) {
         ChatUtil.sendMessage(player, "&cLe parrainage est désactivé.");
         return true;
      }

      if (args.length == 0) {
         this.status(player);
         return true;
      }

      switch (args[0].toLowerCase(Locale.ROOT)) {
         case "accepter" -> this.accept(player);
         case "refuser" -> {
            if (this.offers.remove(player.getUniqueId()) != null) {
               ChatUtil.sendMessage(player, "&eProposition de parrainage refusée.");
            } else {
               ChatUtil.sendMessage(player, "&cAucune proposition en attente.");
            }
         }
         case "aide", "help" -> this.help(player);
         default -> this.propose(player, args[0]);
      }

      return true;
   }

   private void help(Player player) {
      ChatUtil.sendMessage(player, "&6🎓 Parrainage");
      ChatUtil.sendRaw(player, " &e/parrain <joueur> &7: proposer de parrainer une recrue (joueur avancé).");
      ChatUtil.sendRaw(player, " &e/parrain accepter | refuser &7: répondre à une proposition.");
      ChatUtil.sendRaw(player, " &7Le parrain gagne &6" + String.format(Locale.ROOT, "%.0f", this.reward("first-job-mentor", 200.0)) + " &7pièces au premier métier de la recrue et &6"
         + String.format(Locale.ROOT, "%.0f", this.reward("first-claim-mentor", 300.0)) + " &7à son premier territoire.");
   }

   private void status(Player player) {
      Bond own = this.bonds.get(player.getUniqueId());
      if (own != null) {
         ChatUtil.sendMessage(player, "&6🎓 Votre parrain : &e" + this.nameOf(own.mentor));
         ChatUtil.sendRaw(player, " " + (own.raceDone ? "&a✔" : "&8✖") + " &7Premier métier   " + (own.claimDone ? "&a✔" : "&8✖") + " &7Premier territoire");
      }

      List<Bond> mine = this.recruitsOf(player.getUniqueId());
      if (!mine.isEmpty()) {
         ChatUtil.sendMessage(player, "&6🎓 Vos recrues :");
         for (Bond b : mine) {
            ChatUtil.sendRaw(player, " &e" + this.nameOf(b.recruit) + " &7- " + (b.raceDone ? "&a✔" : "&8✖") + " &7métier  " + (b.claimDone ? "&a✔" : "&8✖") + " &7territoire");
         }
      }

      if (own == null && mine.isEmpty()) {
         this.help(player);
      }
   }

   private void propose(Player mentor, String targetName) {
      Player recruit = Bukkit.getPlayerExact(targetName);
      if (recruit == null) {
         ChatUtil.sendMessage(mentor, "&c❌ Joueur introuvable ou hors ligne : &e" + targetName);
         return;
      }

      if (recruit.equals(mentor)) {
         ChatUtil.sendMessage(mentor, "&c❌ Vous ne pouvez pas vous parrainer vous-même.");
         return;
      }

      if (!this.isAdvanced(mentor)) {
         ChatUtil.sendMessage(mentor, "&c❌ Pour parrainer, il faut avoir choisi un métier, appartenir à une guilde et avoir joué au moins &e"
            + this.plugin.getConfig().getLong("mentoring.mentor-min-hours", 5L) + " h&c.");
         return;
      }

      int max = this.plugin.getConfig().getInt("mentoring.max-recruits", 5);
      if (this.recruitsOf(mentor.getUniqueId()).size() >= max) {
         ChatUtil.sendMessage(mentor, "&c❌ Vous avez déjà &e" + max + " &crecrues.");
         return;
      }

      if (this.bonds.containsKey(recruit.getUniqueId()) || this.bonds.containsKey(mentor.getUniqueId()) && this.bonds.get(mentor.getUniqueId()).mentor.equals(recruit.getUniqueId())) {
         ChatUtil.sendMessage(mentor, "&c❌ Ce joueur a déjà un parrain.");
         return;
      }

      if (!this.isRecruit(recruit)) {
         ChatUtil.sendMessage(mentor, "&c❌ &e" + recruit.getName() + " &cn'est pas une recrue (trop ancien, ou déjà métier choisi et territoire en main).");
         return;
      }

      this.offers.put(recruit.getUniqueId(), new Offer(mentor.getUniqueId(), System.currentTimeMillis() + 120000L));
      ChatUtil.sendMessage(mentor, "&a🎓 Proposition envoyée à &e" + recruit.getName() + "&a.");
      ChatUtil.sendMessage(recruit, "&6🎓 &e" + mentor.getName() + " &6vous propose de devenir votre parrain. Il vous guide et est récompensé quand vous progressez. "
         + "&e/parrain accepter &6ou &e/parrain refuser &6(2 min).");
   }

   private void accept(Player recruit) {
      Offer offer = this.offers.remove(recruit.getUniqueId());
      if (offer == null || offer.expiresAt() < System.currentTimeMillis()) {
         ChatUtil.sendMessage(recruit, "&cAucune proposition de parrainage valable.");
         return;
      }

      boolean raceDone = this.plugin.getRaceManager().getRace(recruit) != Race.NONE;
      Guild g = this.plugin.getGuildManager().getGuildByPlayer(recruit.getUniqueId());
      boolean claimDone = g != null && !g.getClaimedChunks().isEmpty();
      Bond bond = new Bond(recruit.getUniqueId(), offer.mentor(), System.currentTimeMillis(), raceDone, claimDone);
      this.bonds.put(recruit.getUniqueId(), bond);
      Bukkit.getScheduler().runTaskAsynchronously(this.plugin, () -> this.plugin.getDataManager().getStorage().exec(
         "INSERT OR REPLACE INTO mentoring (recruit, mentor, since, race_done, claim_done) VALUES (?, ?, ?, ?, ?);",
         bond.recruit.toString(), bond.mentor.toString(), bond.since, raceDone ? 1 : 0, claimDone ? 1 : 0));
      ChatUtil.sendMessage(recruit, "&a🎓 Vous êtes maintenant guidé par &e" + this.nameOf(offer.mentor()) + "&a. Bon courage !");
      Player mentor = Bukkit.getPlayer(offer.mentor());
      if (mentor != null) {
         ChatUtil.sendMessage(mentor, "&a🎓 &e" + recruit.getName() + " &aest désormais votre recrue.");
      }
   }

   public List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command command, @NotNull String alias, @NotNull String[] args) {
      List<String> out = new ArrayList<>();
      if (args.length == 1) {
         String prefix = args[0].toLowerCase(Locale.ROOT);
         for (String s : List.of("accepter", "refuser", "aide")) {
            if (s.startsWith(prefix)) {
               out.add(s);
            }
         }

         for (Player p : Bukkit.getOnlinePlayers()) {
            if (!p.equals(sender) && p.getName().toLowerCase(Locale.ROOT).startsWith(prefix)) {
               out.add(p.getName());
            }
         }
      }

      return out;
   }
}
