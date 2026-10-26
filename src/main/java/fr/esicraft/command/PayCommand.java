package fr.esicraft.command;

import fr.esicraft.EsicraftPlugin;
import fr.esicraft.data.PlayerData;
import fr.esicraft.util.ChatUtil;
import java.util.ArrayList;
import java.util.List;
import org.bukkit.Bukkit;
import org.bukkit.Sound;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;

public class PayCommand implements CommandExecutor, TabCompleter {
   private final EsicraftPlugin plugin;

   public PayCommand(EsicraftPlugin plugin) {
      this.plugin = plugin;
   }

   public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
      if (sender instanceof Player player) {
         if (args.length < 2) {
            ChatUtil.sendMessage(player, "&cUtilisation : &e/pay <joueur> <montant>");
            return true;
         } else {
            String targetName = args[0];
            Player target = Bukkit.getPlayerExact(targetName);
            if (target != null && target.isOnline()) {
               if (target.getUniqueId().equals(player.getUniqueId())) {
                  ChatUtil.sendMessage(player, "&cVous ne pouvez pas vous envoyer de l'argent à vous-même !");
                  return true;
               } else {
                  double amount;
                  try {
                     amount = Double.parseDouble(args[1].replace(',', '.'));
                     if (amount <= 0.0 || Double.isNaN(amount) || Double.isInfinite(amount)) {
                        throw new NumberFormatException();
                     }
                  } catch (NumberFormatException var12) {
                     ChatUtil.sendMessage(player, "&cMontant invalide. Exemple : &e/pay " + target.getName() + " 50");
                     return true;
                  }

                  PlayerData senderData = this.plugin.getDataManager().getPlayerData(player.getUniqueId());
                  if (senderData == null) {
                     ChatUtil.sendMessage(player, "&cErreur lors du chargement de vos données économiques.");
                     return true;
                  } else if (senderData.getCoins() < amount) {
                     ChatUtil.sendMessage(player, "&cSolde insuffisant ! Vous disposez de &6" + String.format("%.2f", senderData.getCoins()) + " pièces&c.");
                     ChatUtil.playSound(player, Sound.ENTITY_VILLAGER_NO, 1.0F, 1.0F);
                     return true;
                  } else {
                     if (this.plugin.getCombatManager().denySpending(player)) {
                        return true;
                     }

                     PlayerData targetData = this.plugin.getDataManager().getPlayerData(target.getUniqueId());
                     if (targetData == null) {
                        targetData = this.plugin.getDataManager().loadPlayer(target);
                     }

                     if (targetData == null) {
                        ChatUtil.sendMessage(player, "&cErreur lors du chargement des données du destinataire.");
                        return true;
                     } else {
                        senderData.removeCoins(amount);
                        targetData.addCoins(amount);
                        this.plugin.getDataManager().savePlayer(senderData);
                        this.plugin.getDataManager().savePlayer(targetData);
                        ChatUtil.sendMessage(
                           player,
                           "&aVous avez envoyé &6"
                              + String.format("%.2f", amount)
                              + " pièces &aà &e"
                              + target.getName()
                              + " &a(Nouveau solde : &6"
                              + String.format("%.2f", senderData.getCoins())
                              + "&a)."
                        );
                        ChatUtil.playSound(player, Sound.ENTITY_EXPERIENCE_ORB_PICKUP, 1.0F, 1.2F);
                        ChatUtil.sendMessage(
                           target,
                           "&aVous avez reçu &6"
                              + String.format("%.2f", amount)
                              + " pièces &ade la part de &e"
                              + player.getName()
                              + " &a! (Nouveau solde : &6"
                              + String.format("%.2f", targetData.getCoins())
                              + "&a)."
                        );
                        ChatUtil.playSound(target, Sound.ENTITY_PLAYER_LEVELUP, 1.0F, 1.5F);
                        return true;
                     }
                  }
               }
            } else {
               ChatUtil.sendMessage(player, "&cLe joueur &e" + targetName + " &cn'est pas connecté ou introuvable.");
               return true;
            }
         }
      } else {
         ChatUtil.sendMessage(sender, "&cSeul un joueur en jeu peut effectuer un transfert d'argent.");
         return true;
      }
   }

   public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
      List<String> completions = new ArrayList<>();
      if (args.length == 1) {
         String prefix = args[0].toLowerCase();

         for (Player p : Bukkit.getOnlinePlayers()) {
            if ((!(sender instanceof Player sp) || !p.getUniqueId().equals(sp.getUniqueId())) && p.getName().toLowerCase().startsWith(prefix)) {
               completions.add(p.getName());
            }
         }
      } else if (args.length == 2) {
         String prefix = args[1].toLowerCase();

         for (String val : List.of("10", "50", "100", "500", "1000")) {
            if (val.startsWith(prefix)) {
               completions.add(val);
            }
         }
      }

      return completions;
   }
}
