package fr.esicraft.command;

import fr.esicraft.EsicraftPlugin;
import fr.esicraft.gui.MarketMainGUI;
import fr.esicraft.gui.MercenaryHordeGUI;
import fr.esicraft.util.ChatUtil;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

public class MarketCommand implements CommandExecutor {
   private final EsicraftPlugin plugin;

   public MarketCommand(EsicraftPlugin plugin) {
      this.plugin = plugin;
   }

   public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
      if (!(sender instanceof Player player)) {
         ChatUtil.sendMessage(sender, "&cCommande réservée aux joueurs.");
         return true;
      } else {
         if (args.length > 0) {
            if (args[0].equalsIgnoreCase("sell")) {
               if (args.length < 2) {
                  ChatUtil.sendMessage(player, "&cUtilisation : &e/ah sell <prix>");
                  return true;
               }

               try {
                  double price = Double.parseDouble(args[1]);
                  if (price < 1.0 || price > 1000000.0 || Double.isNaN(price) || Double.isInfinite(price)) {
                     ChatUtil.sendMessage(player, "&cLe prix doit être compris entre &e1.00 &cet &e1 000 000.00 &cpièces.");
                     return true;
                  }

                  ItemStack inHand = player.getInventory().getItemInMainHand();
                  if (inHand.getType().isAir()) {
                     ChatUtil.sendMessage(player, "&cVous devez tenir l'objet à vendre dans votre main principale.");
                     return true;
                  }

                  this.plugin.getMarketManager().createListing(player, inHand, price);
               } catch (NumberFormatException var9) {
                  ChatUtil.sendMessage(player, "&cMontant invalide.");
               }

               return true;
            }

            if (args[0].equalsIgnoreCase("raids") || args[0].equalsIgnoreCase("hordes")) {
               MercenaryHordeGUI.open(player, this.plugin);
               return true;
            }
         }

         MarketMainGUI.open(player, this.plugin);
         return true;
      }
   }
}
