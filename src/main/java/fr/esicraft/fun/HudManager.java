package fr.esicraft.fun;

import fr.esicraft.EsicraftPlugin;
import fr.esicraft.data.PlayerData;
import fr.esicraft.guild.Guild;
import fr.esicraft.util.ChatUtil;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.scoreboard.Criteria;
import org.bukkit.scoreboard.DisplaySlot;
import org.bukkit.scoreboard.Objective;
import org.bukkit.scoreboard.Scoreboard;

/** Tableau de bord latéral personnel (guilde, Âge, Nexus, raid, contrats, solde), activable avec /hud. */
public class HudManager {
   private final EsicraftPlugin plugin;
   private final Map<UUID, Scoreboard> boards = new ConcurrentHashMap<>();
   private final Set<UUID> disabled = new HashSet<>();

   public HudManager(EsicraftPlugin plugin) {
      this.plugin = plugin;
   }

   public void start() {
      for (Object[] row : this.plugin.getDataManager().getStorage().select("SELECT k FROM kv WHERE k LIKE 'hud_off:%';", 1)) {
         try {
            this.disabled.add(UUID.fromString(((String)row[0]).substring("hud_off:".length())));
         } catch (IllegalArgumentException ignored) {
         }
      }

      Bukkit.getScheduler().runTaskTimer(this.plugin, this::refreshAll, 40L, 40L);
   }

   public boolean isEnabled(UUID uuid) {
      return !this.disabled.contains(uuid);
   }

   /** Bascule le HUD du joueur ; renvoie le nouvel état (vrai = affiché). */
   public boolean toggle(Player player) {
      UUID uuid = player.getUniqueId();
      boolean nowEnabled;
      if (this.disabled.remove(uuid)) {
         nowEnabled = true;
         Bukkit.getScheduler().runTaskAsynchronously(this.plugin, () -> this.plugin.getDataManager().getStorage().exec("DELETE FROM kv WHERE k = ?;", "hud_off:" + uuid));
         this.update(player);
      } else {
         nowEnabled = false;
         this.disabled.add(uuid);
         Bukkit.getScheduler().runTaskAsynchronously(this.plugin, () -> this.plugin.getDataManager().getStorage().exec(
            "INSERT INTO kv (k, v) VALUES (?, '1') ON CONFLICT(k) DO NOTHING;", "hud_off:" + uuid));
         this.hide(player);
      }

      return nowEnabled;
   }

   public void onQuit(Player player) {
      this.boards.remove(player.getUniqueId());
   }

   private void hide(Player player) {
      this.boards.remove(player.getUniqueId());
      player.setScoreboard(Bukkit.getScoreboardManager().getMainScoreboard());
   }

   private void refreshAll() {
      for (Player player : Bukkit.getOnlinePlayers()) {
         if (this.isEnabled(player.getUniqueId())) {
            this.update(player);
         }
      }
   }

   private void update(Player player) {
      Scoreboard board = this.boards.get(player.getUniqueId());
      if (board == null) {
         board = Bukkit.getScoreboardManager().getNewScoreboard();
         board.registerNewObjective("esicraft", Criteria.DUMMY, ChatUtil.component("&6&lESICRAFT"));
         board.getObjective("esicraft").setDisplaySlot(DisplaySlot.SIDEBAR);
         this.boards.put(player.getUniqueId(), board);
         player.setScoreboard(board);
      } else if (player.getScoreboard() != board) {
         player.setScoreboard(board);
      }

      Objective objective = board.getObjective("esicraft");
      if (objective == null) {
         return;
      }

      List<String> lines = this.buildLines(player);
      for (String entry : new ArrayList<>(board.getEntries())) {
         board.resetScores(entry);
      }

      int score = lines.size();
      for (String line : lines) {
         objective.getScore(line).setScore(score--);
      }
   }

   private List<String> buildLines(Player player) {
      List<String> raw = new ArrayList<>();
      Guild guild = this.plugin.getGuildManager().getGuildByPlayer(player.getUniqueId());
      PlayerData data = this.plugin.getDataManager().getPlayerData(player.getUniqueId());
      raw.add("&8&m------------");
      raw.add("&eGuilde: &f" + (guild != null ? guild.getTag() : "aucune"));
      raw.add("&eÂge: &f" + this.plugin.getAgeManager().getPlayerAge(player).getDisplayName());
      if (guild != null && guild.hasNexus()) {
         int pct = (int)Math.round(guild.getNexus().getHealthPercentage());
         raw.add("&eNexus: " + (pct > 50 ? "&a" : pct > 20 ? "&6" : "&c") + pct + "%");
         if (this.plugin.getRaidManager().isGuildUnderRaid(guild.getId())) {
            raw.add("&cRaid: EN COURS !");
         } else {
            long cd = this.plugin.getRaidManager().getCooldownRemainingMs(guild);
            raw.add(cd > 0 ? "&aProtégé: " + (cd / 60000L) + " min" : "&7Raid: aucun");
         }
      }

      ContractManager.DayState contracts = this.plugin.getContractManager().getState(player);
      raw.add("&eContrats: &f" + contracts.completedCount() + "/" + ContractManager.CONTRACTS_PER_DAY);
      if (data != null) {
         raw.add("&eSolde: &6" + String.format("%,.0f", data.getCoins()));
      }

      EventManager events = this.plugin.getEventManager();
      if (events.getActive() != null) {
         raw.add("&d" + events.getActive().getLabel());
      }

      // Les entrées d'un scoreboard doivent être uniques : on suffixe d'un code couleur invisible si besoin.
      List<String> unique = new ArrayList<>();
      Set<String> seen = new HashSet<>();
      for (String line : raw) {
         String colored = ChatUtil.color(line);
         while (!seen.add(colored)) {
            colored += "§r";
         }

         unique.add(colored);
      }

      return unique;
   }
}
