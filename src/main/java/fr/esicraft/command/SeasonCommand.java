package fr.esicraft.command;

import fr.esicraft.EsicraftPlugin;
import fr.esicraft.fun.SeasonManager;
import fr.esicraft.guild.Guild;
import fr.esicraft.util.ChatUtil;
import java.util.List;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

/** /classement : classement de saison des guildes ; /histoire : mémorial des conquêtes. */
public class SeasonCommand implements CommandExecutor {
   private final EsicraftPlugin plugin;

   public SeasonCommand(EsicraftPlugin plugin) {
      this.plugin = plugin;
   }

   public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
      if (command.getName().equalsIgnoreCase("histoire")) {
         Bukkit.getScheduler().runTaskAsynchronously(this.plugin, () -> this.showHistory(sender));
      } else {
         this.showRanking(sender);
      }

      return true;
   }

   private void showRanking(CommandSender sender) {
      List<SeasonManager.Entry> ranking = this.plugin.getSeasonManager().ranking();
      ChatUtil.sendRaw(sender, "&8&m--------------------------------------------------");
      ChatUtil.sendRaw(sender, "  &6&l🏆 CLASSEMENT DE SAISON &7" + SeasonManager.currentSeason() + " &8(fin dans " + SeasonManager.daysLeft() + " j)");
      ChatUtil.sendRaw(sender, "");
      if (ranking.isEmpty()) {
         ChatUtil.sendRaw(sender, "  &7Aucune guilde pour le moment.");
      }

      Guild own = sender instanceof Player p ? this.plugin.getGuildManager().getGuildByPlayer(p.getUniqueId()) : null;
      int rank = 1;
      for (SeasonManager.Entry entry : ranking) {
         String medal = switch (rank) {
            case 1 -> "&e🥇 &l#1";
            case 2 -> "&f🥈 &l#2";
            case 3 -> "&6🥉 &l#3";
            default -> "&7  #" + rank;
         };
         String crown = this.plugin.getSeasonManager().isChampion(entry.guild().getId()) ? " &6👑" : "";
         boolean mine = own != null && own.getId().equals(entry.guild().getId());
         if (rank <= 10 || mine) {
            ChatUtil.sendRaw(sender, "  " + medal + " &f" + entry.guild().getName() + crown + " &8» &a" + entry.score() + " pts" + (mine ? " &e← vous" : ""));
         }

         rank++;
      }

      ChatUtil.sendRaw(sender, "");
      ChatUtil.sendRaw(sender, "  &7Points : territoire, Âge, richesse, membres, raids repoussés et conquêtes.");
      ChatUtil.sendRaw(sender, "  &7Le champion reçoit &65 000 pièces &7en banque, &6500 &7par membre et la couronne &6👑&7.");
      ChatUtil.sendRaw(sender, "&8&m--------------------------------------------------");
   }

   private void showHistory(CommandSender sender) {
      List<String> lines = this.plugin.getSeasonManager().getHistory(10);
      ChatUtil.sendRaw(sender, "&8&m--------------------------------------------------");
      ChatUtil.sendRaw(sender, "  &6&l📖 MÉMORIAL DES CONQUÊTES &7(10 derniers événements)");
      ChatUtil.sendRaw(sender, "");
      if (lines.isEmpty()) {
         ChatUtil.sendRaw(sender, "  &7Rien à signaler… pour l'instant.");
      }

      for (String line : lines) {
         ChatUtil.sendRaw(sender, "  &7" + line);
      }

      ChatUtil.sendRaw(sender, "&8&m--------------------------------------------------");
   }
}
