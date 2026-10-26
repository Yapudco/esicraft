package fr.esicraft.social;

import fr.esicraft.EsicraftPlugin;
import fr.esicraft.util.ChatUtil;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.Sound;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

/**
 * Réputation : {@code /merci <joueur>} offre un point d'honneur (une fois par jour et par personne).
 * Les points sont affichés dans le tab, sur le profil du site et au classement des plus honorables.
 */
public class ReputationManager implements CommandExecutor, TabCompleter {
   private final EsicraftPlugin plugin;
   private final Map<UUID, Integer> points = new ConcurrentHashMap<>();
   /** « giver|receiver|jour » déjà remerciés : l'ajout est atomique, donc 20 /merci dans le même tick ne donnent qu'un point. */
   private final java.util.Set<String> thankedToday = ConcurrentHashMap.newKeySet();
   private final Map<String, Integer> givenToday = new ConcurrentHashMap<>();

   public ReputationManager(EsicraftPlugin plugin) {
      this.plugin = plugin;
   }

   public void init() {
      var db = this.plugin.getDataManager().getStorage();
      db.exec("CREATE TABLE IF NOT EXISTS reputation (uuid VARCHAR(36) PRIMARY KEY, points INT DEFAULT 0);");
      db.exec("CREATE TABLE IF NOT EXISTS reputation_thanks (giver VARCHAR(36), receiver VARCHAR(36), day BIGINT, PRIMARY KEY (giver, receiver, day));");
      for (Object[] row : db.select("SELECT uuid, points FROM reputation;", 2)) {
         try {
            this.points.put(UUID.fromString((String)row[0]), ((Number)row[1]).intValue());
         } catch (IllegalArgumentException ignored) {
         }
      }

      db.exec("DELETE FROM reputation_thanks WHERE day < ?;", LocalDate.now().toEpochDay() - 2);
      long today = LocalDate.now().toEpochDay();
      for (Object[] row : db.select("SELECT giver, receiver FROM reputation_thanks WHERE day = ?;", 2, today)) {
         this.thankedToday.add(row[0] + "|" + row[1] + "|" + today);
         this.givenToday.merge(row[0] + "|" + today, 1, Integer::sum);
      }
   }

   public void clear() {
      this.points.clear();
      this.thankedToday.clear();
      this.givenToday.clear();
   }

   public int get(UUID uuid) {
      return uuid == null ? 0 : this.points.getOrDefault(uuid, 0);
   }

   /** Joueurs les plus honorables (UUID, points), du plus au moins. */
   public List<Map.Entry<UUID, Integer>> top(int limit) {
      List<Map.Entry<UUID, Integer>> list = new ArrayList<>();
      for (Map.Entry<UUID, Integer> e : this.points.entrySet()) {
         if (e.getValue() > 0) {
            list.add(Map.entry(e.getKey(), e.getValue()));
         }
      }

      list.sort((a, b) -> Integer.compare(b.getValue(), a.getValue()));
      return list.size() > limit ? list.subList(0, limit) : list;
   }

   /** Suffixe affiché dans le tab (vide sans point). */
   public String tabSuffix(UUID uuid) {
      int p = this.get(uuid);
      return p > 0 ? " &c❤" + p : "";
   }


   public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
      if (!(sender instanceof Player player)) {
         sender.sendMessage(ChatUtil.color("&cSeul un joueur peut exécuter cette commande."));
         return true;
      }

      if (!this.plugin.getConfig().getBoolean("reputation.enabled", true)) {
         ChatUtil.sendMessage(player, "&cLa réputation est désactivée.");
         return true;
      }

      if (args.length == 0) {
         ChatUtil.sendMessage(player, "&6❤ Votre réputation : &c" + this.get(player.getUniqueId()) + " &7point(s) d'honneur.");
         ChatUtil.sendMessage(player, "&7Remerciez un joueur serviable avec &e/merci <joueur>&7 (une fois par jour et par personne).");
         int rank = 1;
         for (Map.Entry<UUID, Integer> e : this.top(3)) {
            OfflinePlayer op = Bukkit.getOfflinePlayer(e.getKey());
            ChatUtil.sendRaw(player, " &8#" + rank++ + " &f" + (op.getName() != null ? op.getName() : "?") + " &c❤" + e.getValue());
         }

         return true;
      }

      OfflinePlayer target = Bukkit.getPlayerExact(args[0]);
      if (target == null) {
         target = Bukkit.getOfflinePlayerIfCached(args[0]);
      }

      if (target == null || !target.hasPlayedBefore() && !target.isOnline()) {
         ChatUtil.sendMessage(player, "&c❌ Joueur introuvable : &e" + args[0]);
         return true;
      }

      UUID giver = player.getUniqueId();
      UUID receiver = target.getUniqueId();
      if (giver.equals(receiver)) {
         ChatUtil.sendMessage(player, "&c❌ On ne se remercie pas soi-même !");
         return true;
      }

      long day = LocalDate.now().toEpochDay();
      int perDay = this.plugin.getConfig().getInt("reputation.max-thanks-per-day", 5);
      String giverDay = giver + "|" + day;
      boolean bypass = this.plugin.bypassCooldowns(player);
      if (!bypass && this.givenToday.getOrDefault(giverDay, 0) >= perDay) {
         ChatUtil.sendMessage(player, "&c❌ Vous avez déjà remercié &e" + perDay + " &cjoueurs aujourd'hui. Revenez demain !");
         ChatUtil.playSound(player, Sound.ENTITY_VILLAGER_NO, 1.0F, 1.0F);
         return true;
      }

      if (!this.thankedToday.add(giver + "|" + receiver + "|" + day) && !bypass) {
         ChatUtil.sendMessage(player, "&c❌ Vous avez déjà remercié &e" + target.getName() + " &caujourd'hui. Revenez demain !");
         ChatUtil.playSound(player, Sound.ENTITY_VILLAGER_NO, 1.0F, 1.0F);
         return true;
      }

      this.givenToday.merge(giverDay, 1, Integer::sum);

      this.points.merge(receiver, 1, Integer::sum);
      int total = this.points.get(receiver);
      Bukkit.getScheduler().runTaskAsynchronously(this.plugin, () -> {
         var db = this.plugin.getDataManager().getStorage();
         db.exec("INSERT OR IGNORE INTO reputation_thanks (giver, receiver, day) VALUES (?, ?, ?);", giver.toString(), receiver.toString(), day);
         db.exec("INSERT INTO reputation (uuid, points) VALUES (?, ?) ON CONFLICT(uuid) DO UPDATE SET points = excluded.points;", receiver.toString(), total);
      });
      ChatUtil.sendMessage(player, "&a❤ Vous avez remercié &e" + target.getName() + " &a! Il possède maintenant &c" + total + " &apoint(s) d'honneur.");
      ChatUtil.playSound(player, Sound.ENTITY_PLAYER_LEVELUP, 0.7F, 1.6F);
      Player online = target.getPlayer();
      if (online != null) {
         ChatUtil.sendMessage(online, "&c❤ &e" + player.getName() + " &avous remercie ! Réputation : &c" + total + "&a.");
         ChatUtil.playSound(online, Sound.ENTITY_EXPERIENCE_ORB_PICKUP, 1.0F, 1.2F);
         this.plugin.getGuildManager().updatePlayerTab(online);
      }

      return true;
   }

   public List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command command, @NotNull String alias, @NotNull String[] args) {
      List<String> names = new ArrayList<>();
      if (args.length == 1) {
         String prefix = args[0].toLowerCase(Locale.ROOT);
         for (Player p : Bukkit.getOnlinePlayers()) {
            if (!p.equals(sender) && p.getName().toLowerCase(Locale.ROOT).startsWith(prefix)) {
               names.add(p.getName());
            }
         }
      }

      return names;
   }
}
