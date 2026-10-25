package fr.esicraft.command;

import fr.esicraft.EsicraftPlugin;
import fr.esicraft.util.ChatUtil;
import java.util.List;
import java.util.Map.Entry;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;

public class BaltopCommand implements CommandExecutor {
   private final EsicraftPlugin plugin;

   public BaltopCommand(EsicraftPlugin plugin) {
      this.plugin = plugin;
   }

   public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
      Bukkit.getScheduler().runTaskAsynchronously(this.plugin, () -> {
         List<Entry<String, Double>> top = this.plugin.getDataManager().getTopBalances(10);
         ChatUtil.sendRaw(sender, "&8&m--------------------------------------------------");
         ChatUtil.sendRaw(sender, "  &6&l\ud83c\udfc6 CLASSEMENT DES JOUEURS LES PLUS RICHES &7(Top 10)");
         ChatUtil.sendRaw(sender, "");
         if (top.isEmpty()) {
            ChatUtil.sendRaw(sender, "  &7Aucune donnée économique disponible pour le moment.");
         } else {
            int rank = 1;

            for (Entry<String, Double> entry : top) {
               String medal = switch (rank) {
                  case 1 -> "&e\ud83e\udd47 &l#1";
                  case 2 -> "&f\ud83e\udd48 &l#2";
                  case 3 -> "&6\ud83e\udd49 &l#3";
                  default -> "&7  #" + rank;
               };
               ChatUtil.sendRaw(sender, "  " + medal + " &f" + entry.getKey() + " &8» &a" + String.format("%,.2f", entry.getValue()) + " pièces");
               rank++;
            }
         }

         ChatUtil.sendRaw(sender, "&8&m--------------------------------------------------");
      });
      return true;
   }
}
