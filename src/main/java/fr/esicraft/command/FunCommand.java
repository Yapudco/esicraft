package fr.esicraft.command;

import fr.esicraft.EsicraftPlugin;
import fr.esicraft.fun.EventManager;
import fr.esicraft.util.ChatUtil;
import java.util.ArrayList;
import java.util.List;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;

/** /hud (tableau de bord) et /evenement (administration des événements serveur). */
public class FunCommand implements CommandExecutor, TabCompleter {
   private final EsicraftPlugin plugin;

   public FunCommand(EsicraftPlugin plugin) {
      this.plugin = plugin;
   }

   public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
      if (command.getName().equalsIgnoreCase("hud")) {
         if (!(sender instanceof Player player)) {
            ChatUtil.sendMessage(sender, "&cCommande réservée aux joueurs.");
            return true;
         }

         boolean on = this.plugin.getHudManager().toggle(player);
         ChatUtil.sendMessage(player, on ? "&aTableau de bord activé. &7(/hud pour le masquer)" : "&7Tableau de bord masqué. &7(/hud pour le réafficher)");
         return true;
      }

      if (!sender.hasPermission("esicraft.admin")) {
         ChatUtil.sendMessage(sender, "&cPermission refusée.");
         return true;
      }

      EventManager events = this.plugin.getEventManager();
      if (args.length == 0 || args[0].equalsIgnoreCase("status")) {
         for (String line : events.describe()) {
            ChatUtil.sendMessage(sender, "&e" + line);
         }

         ChatUtil.sendMessage(sender, "&7Usage : /evenement <lune|meteores|marchand|boss|stop|status>");
         return true;
      }

      EventManager.Type type = switch (args[0].toLowerCase()) {
         case "lune", "moon" -> EventManager.Type.BLOOD_MOON;
         case "meteores", "meteors" -> EventManager.Type.METEOR_SHOWER;
         case "marchand", "market" -> EventManager.Type.BLACK_MARKET;
         case "boss" -> EventManager.Type.WORLD_BOSS;
         default -> null;
      };
      if (args[0].equalsIgnoreCase("stop")) {
         events.stop();
         ChatUtil.sendMessage(sender, "&aÉvénement arrêté.");
      } else if (type == null) {
         ChatUtil.sendMessage(sender, "&cÉvénement inconnu. Usage : /evenement <lune|meteores|marchand|boss|stop|status>");
      } else if (events.launch(type)) {
         ChatUtil.sendMessage(sender, "&aÉvénement lancé : " + type.getLabel());
      } else {
         ChatUtil.sendMessage(sender, "&cImpossible de lancer l'événement (déjà un événement actif, aucun joueur ou aucun emplacement valide).");
      }

      return true;
   }

   public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
      List<String> out = new ArrayList<>();
      if (command.getName().equalsIgnoreCase("evenement") && args.length == 1) {
         for (String s : List.of("lune", "meteores", "marchand", "boss", "stop", "status")) {
            if (s.startsWith(args[0].toLowerCase())) {
               out.add(s);
            }
         }
      }

      return out;
   }
}
