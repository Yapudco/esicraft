package fr.esicraft.command;

import fr.esicraft.util.ChatUtil;
import fr.esicraft.util.Lang;
import org.bukkit.Sound;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

/** /english : passe le jeu en anglais pour soi (menus, messages, tableau de bord, livre) ; /francais revient au français. */
public class EnglishCommand implements CommandExecutor {
   public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
      if (!(sender instanceof Player player)) {
         ChatUtil.sendMessage(sender, "&cCommande réservée aux joueurs.");
         return true;
      }

      boolean wantEnglish;
      if (command.getName().equalsIgnoreCase("francais")) {
         wantEnglish = false;
      } else if (args.length > 0 && (args[0].equalsIgnoreCase("off") || args[0].equalsIgnoreCase("fr"))) {
         wantEnglish = false;
      } else if (args.length > 0 && args[0].equalsIgnoreCase("on")) {
         wantEnglish = true;
      } else {
         wantEnglish = !Lang.isEnglish(player);
      }

      Lang.setEnglish(player, wantEnglish);
      if (wantEnglish) {
         ChatUtil.sendMessage(player, "&aThe game is now in &6English&a. Type &e/english&a again (or &e/francais&a) to go back to French. Open a menu again to see it translated; &e/guide&a gives you the English rule book.");
      } else {
         ChatUtil.sendMessage(player, "&aLe jeu est de nouveau en &6français&a. Tape &e/english&a pour repasser en anglais.");
      }

      ChatUtil.playSound(player, Sound.ENTITY_EXPERIENCE_ORB_PICKUP, 1.0F, 1.2F);
      return true;
   }
}
