package fr.esicraft.command;

import fr.esicraft.EsicraftPlugin;
import fr.esicraft.gui.AgeProgressGUI;
import fr.esicraft.util.ChatUtil;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

public class AgeCommand implements CommandExecutor {
   private final EsicraftPlugin plugin;

   public AgeCommand(EsicraftPlugin plugin) {
      this.plugin = plugin;
   }

   public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
      if (sender instanceof Player player) {
         if (args.length > 0 && args[0].equalsIgnoreCase("deposit")) {
            ItemStack inHand = player.getInventory().getItemInMainHand();
            if (inHand.getType().isAir()) {
               ChatUtil.sendMessage(player, "&cVous devez tenir l'objet que vous souhaitez donner en main principale.");
               return true;
            } else {
               this.plugin.getAgeManager().depositItemForGuild(player, inHand);
               return true;
            }
         } else {
            AgeProgressGUI.open(player, this.plugin);
            return true;
         }
      } else {
         ChatUtil.sendMessage(sender, "&cCommande réservée aux joueurs.");
         return true;
      }
   }
}
