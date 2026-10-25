package fr.esicraft.command;

import fr.esicraft.util.BookUtil;
import fr.esicraft.util.ChatUtil;
import org.bukkit.Sound;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

public class GuideCommand implements CommandExecutor {
   public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
      if (sender instanceof Player player) {
         ItemStack book = BookUtil.createGuideBook(fr.esicraft.util.Lang.isEnglish(player));
            player.getInventory().addItem(new ItemStack[]{book}).values().forEach(left -> player.getWorld().dropItemNaturally(player.getLocation(), left));
            player.openBook(book);
         ChatUtil.sendMessage(player, "&aUn exemplaire du &6Guide Esicraft &aa été ajouté à votre inventaire !");
         ChatUtil.playSound(player, Sound.ITEM_BOOK_PAGE_TURN, 1.0F, 1.2F);
         return true;
      } else {
         ChatUtil.sendMessage(sender, "&cCommande réservée aux joueurs.");
         return true;
      }
   }
}
