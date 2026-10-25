package fr.esicraft.command;

import fr.esicraft.EsicraftPlugin;
import fr.esicraft.gui.RaceSelectGUI;
import fr.esicraft.gui.RaceShowGUI;
import fr.esicraft.race.Race;
import fr.esicraft.util.ChatUtil;
import java.util.ArrayList;
import java.util.List;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;

public class RaceCommand implements CommandExecutor, TabCompleter {
   private final EsicraftPlugin plugin;

   public RaceCommand(EsicraftPlugin plugin) {
      this.plugin = plugin;
   }

   public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
      if (args.length >= 1 && args[0].equalsIgnoreCase("show")) {
         if (sender instanceof Player player) {
            if (args.length >= 2) {
               Player target = Bukkit.getPlayer(args[1]);
               if (target == null) {
                  ChatUtil.sendMessage(player, "&cJoueur &e" + args[1] + " &cintrouvable ou hors-ligne.");
                  return true;
               } else {
                  RaceShowGUI.open(player, target, this.plugin);
                  return true;
               }
            } else {
               RaceShowGUI.open(player, this.plugin);
               return true;
            }
         } else {
            ChatUtil.sendMessage(sender, "&cCette commande doit être exécutée par un joueur.");
            return true;
         }
      } else if (args.length >= 3 && args[0].equalsIgnoreCase("set")) {
         if (!sender.hasPermission("esicraft.admin")) {
            ChatUtil.sendMessage(sender, "&cPermission insuffisante.");
            return true;
         } else {
            Player target = Bukkit.getPlayer(args[1]);
            if (target == null) {
               ChatUtil.sendMessage(sender, "&cJoueur introuvable.");
               return true;
            } else {
               try {
                  Race race = Race.valueOf(args[2].toUpperCase());
                  this.plugin.getRaceManager().setRace(target, race, true);
                  ChatUtil.sendMessage(sender, "&aLa classe de &e" + target.getName() + " &aa été définie sur &6" + race.getDisplayName());
               } catch (IllegalArgumentException var7) {
                  ChatUtil.sendMessage(sender, "&cClasse invalide. Classes disponibles : DWARF, SYLVAN, BLACKSMITH, RANGER, ARCANIST, ENGINEER, NONE");
               }

               return true;
            }
         }
      } else if (sender instanceof Player playerx) {
         RaceSelectGUI.open(playerx, this.plugin);
         return true;
      } else {
         ChatUtil.sendMessage(sender, "&cCette commande doit être exécutée par un joueur.");
         return true;
      }
   }

   public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
      List<String> completions = new ArrayList<>();
      if (args.length == 1) {
         if ("show".startsWith(args[0].toLowerCase())) {
            completions.add("show");
         }

         if (sender.hasPermission("esicraft.admin") && "set".startsWith(args[0].toLowerCase())) {
            completions.add("set");
         }
      } else if (args.length == 2 && args[0].equalsIgnoreCase("show")) {
         for (Player p : Bukkit.getOnlinePlayers()) {
            if (p.getName().toLowerCase().startsWith(args[1].toLowerCase())) {
               completions.add(p.getName());
            }
         }
      } else if (args.length == 2 && args[0].equalsIgnoreCase("set") && sender.hasPermission("esicraft.admin")) {
         for (Player px : Bukkit.getOnlinePlayers()) {
            if (px.getName().toLowerCase().startsWith(args[1].toLowerCase())) {
               completions.add(px.getName());
            }
         }
      } else if (args.length == 3 && args[0].equalsIgnoreCase("set") && sender.hasPermission("esicraft.admin")) {
         for (Race r : Race.values()) {
            if (r.name().toLowerCase().startsWith(args[2].toLowerCase())) {
               completions.add(r.name());
            }
         }
      }

      return completions;
   }
}
