package fr.esicraft.command;

import fr.esicraft.EsicraftPlugin;
import fr.esicraft.util.ChatUtil;
import java.util.ArrayList;
import java.util.List;
import org.bukkit.Sound;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;

public class PvPCommand implements CommandExecutor, TabCompleter {
   private final EsicraftPlugin plugin;

   public PvPCommand(EsicraftPlugin plugin) {
      this.plugin = plugin;
   }

   public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
      if (!sender.hasPermission("esicraft.admin") && !sender.isOp()) {
         ChatUtil.sendMessage(sender, "&cVous n'avez pas la permission d'exécuter cette commande.");
         return true;
      } else {
         boolean newState;
         if (args.length > 0) {
            String arg = args[0].toLowerCase();
            if (arg.equals("on") || arg.equals("enable") || arg.equals("activer") || arg.equals("true")) {
               newState = true;
            } else if (!arg.equals("off") && !arg.equals("disable") && !arg.equals("desactiver") && !arg.equals("false")) {
               newState = !this.plugin.isNexusDestructionEnabled();
            } else {
               newState = false;
            }
         } else {
            newState = !this.plugin.isNexusDestructionEnabled();
         }

         this.plugin.setNexusDestructionEnabled(newState);
         if (newState) {
            ChatUtil.broadcast("&8&m--------------------------------------------------");
            ChatUtil.broadcast("&c&l⚔️ DESTRUCTION DES NEXUS : &a&lACTIVÉE &7!");
            ChatUtil.broadcast("&eLes Nexus de guilde peuvent désormais être attaqués et détruits !");
            ChatUtil.broadcast("&8&m--------------------------------------------------");
         } else {
            ChatUtil.broadcast("&8&m--------------------------------------------------");
            ChatUtil.broadcast("&c&l\ud83d\udee1️ DESTRUCTION DES NEXUS : &c&lDÉSACTIVÉE &7!");
            ChatUtil.broadcast("&eLes Nexus de guilde sont temporairement invulnérables à la destruction.");
            ChatUtil.broadcast("&8&m--------------------------------------------------");
         }

         if (sender instanceof Player p) {
            ChatUtil.playSound(p, Sound.UI_TOAST_CHALLENGE_COMPLETE, 1.0F, 1.2F);
         }

         return true;
      }
   }

   public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
      if (args.length == 1) {
         List<String> options = List.of("on", "off", "toggle");
         List<String> results = new ArrayList<>();

         for (String opt : options) {
            if (opt.startsWith(args[0].toLowerCase())) {
               results.add(opt);
            }
         }

         return results;
      } else {
         return List.of();
      }
   }
}
