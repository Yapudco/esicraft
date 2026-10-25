package fr.esicraft.command;

import fr.esicraft.EsicraftPlugin;
import fr.esicraft.data.PlayerData;
import fr.esicraft.util.ChatUtil;
import java.util.Map.Entry;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;

public class MoneyCommand implements CommandExecutor {
   private final EsicraftPlugin plugin;

   public MoneyCommand(EsicraftPlugin plugin) {
      this.plugin = plugin;
   }

   public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
      if (!sender.hasPermission("esicraft.admin")) {
         if (args.length == 0 && sender instanceof Player self) {
            PlayerData own = this.plugin.getDataManager().getPlayerData(self.getUniqueId());
            if (own != null) {
               ChatUtil.sendMessage(self, "&6\ud83d\udcb0 Votre solde : &e" + String.format("%.2f", own.getCoins()) + " pièces");
               return true;
            }
         }

         ChatUtil.sendMessage(sender, "&cSeul &e/money &cest disponible pour les joueurs (les autres sous-commandes sont réservées aux administrateurs).");
         return true;
      } else if (args.length == 0) {
         this.sendHelp(sender);
         return true;
      } else {
         String sub = args[0].toLowerCase();
         if (sub.equals("rectify")) {
            if (args.length >= 2 && args[1].equalsIgnoreCase("reset")) {
               if (args.length >= 3 && !args[2].equalsIgnoreCase("all")) {
                  OfflinePlayer target = Bukkit.getOfflinePlayerIfCached(args[2]);
                  if (target != null) {
                     this.plugin.getAntiFarmManager().resetPlayerStats(target.getUniqueId());
                     ChatUtil.sendMessage(sender, "&aStatistiques anti-farm de &e" + target.getName() + " &aréinitialisées.");
                     return true;
                  }

                  ChatUtil.sendMessage(sender, "&cJoueur introuvable : aucune statistique réinitialisée. (Utilisez &e/money rectify reset all &cpour tout remettre à zéro.)");
                  return true;
               }

               this.plugin.getAntiFarmManager().resetAllStats();
               ChatUtil.sendMessage(sender, "&aToutes les statistiques d'éliminations individuelles ont été réinitialisées.");
               return true;
            } else if (args.length >= 2 && (args[1].equalsIgnoreCase("info") || args[1].equalsIgnoreCase("status"))) {
               OfflinePlayer target = (OfflinePlayer)(args.length >= 3 ? Bukkit.getOfflinePlayer(args[2]) : (sender instanceof Player p ? p : null));
               if (target != null && target.getUniqueId() != null) {
                  double factor = this.plugin.getAntiFarmManager().getRectifyFactor();
                  int total = this.plugin.getAntiFarmManager().getPlayerTotalKills(target.getUniqueId());
                  ChatUtil.sendRaw(sender, "&8&m--------------------------------------------------");
                  ChatUtil.sendRaw(sender, "  &6&l⚖️ ANTI-FARM INDIVIDUEL DE : &e" + (target.getName() != null ? target.getName() : "Inconnu"));
                  ChatUtil.sendRaw(sender, "  &7Facteur serveur : &e" + String.format("%.2f", factor));
                  ChatUtil.sendRaw(sender, "  &7Total de ses éliminations : &f" + total + " monstres");
                  ChatUtil.sendRaw(sender, "");
                  ChatUtil.sendRaw(sender, "  &eRépartition & Multiplicateurs appliqués à ce joueur :");
                  if (total == 0) {
                     ChatUtil.sendRaw(sender, "   &7Ce joueur n'a encore éliminé aucun monstre.");
                  } else {
                     for (Entry<EntityType, Integer> entry : this.plugin.getAntiFarmManager().getPlayerMobCounts(target.getUniqueId()).entrySet()) {
                        double share = this.plugin.getAntiFarmManager().getPlayerShare(target.getUniqueId(), entry.getKey()) * 100.0;
                        double mult = this.plugin.getAntiFarmManager().getMultiplier(target.getUniqueId(), entry.getKey());
                        ChatUtil.sendRaw(
                           sender,
                           "   &8• &f"
                              + entry.getKey().name()
                              + " &8» &e"
                              + entry.getValue()
                              + " kills &7("
                              + String.format("%.1f", share)
                              + "%) &8➔ &aMult: &6x"
                              + String.format("%.3f", mult)
                        );
                     }
                  }

                  ChatUtil.sendRaw(sender, "");
                  ChatUtil.sendRaw(sender, "  &7Pour changer le facteur global : &e/money rectify <valeur>");
                  ChatUtil.sendRaw(sender, "  &7Pour réinitialiser : &e/money rectify reset [joueur/all]");
                  ChatUtil.sendRaw(sender, "&8&m--------------------------------------------------");
                  return true;
               } else {
                  ChatUtil.sendMessage(sender, "&cUtilisation : /money rectify status <joueur>");
                  return true;
               }
            } else {
               try {
                  double factor = Double.parseDouble(args[1].replace(',', '.'));
                  if (Double.isNaN(factor) || Double.isInfinite(factor)) {
                     throw new NumberFormatException();
                  }

                  this.plugin.getAntiFarmManager().setRectifyFactor(factor);
                  ChatUtil.sendMessage(sender, "&aFacteur de rectification anti-farm défini à &6" + String.format("%.2f", factor) + " &a!");
                  ChatUtil.sendMessage(sender, "&7Chaque joueur verra ses gains réduits individuellement selon sa propre proportion de kills.");
               } catch (NumberFormatException var16) {
                  ChatUtil.sendMessage(sender, "&cValeur invalide. Exemple : /money rectify 1.0 ou /money rectify status <joueur>");
               }

               return true;
            }
         } else if (!sub.equals("top") && !sub.equals("leaderboard")) {
            if (args.length < 2) {
               this.sendHelp(sender);
               return true;
            } else {
               String targetName = args[1];
               OfflinePlayer target = Bukkit.getOfflinePlayerIfCached(targetName);
               if (target == null) {
                  ChatUtil.sendMessage(sender, "&cJoueur introuvable (jamais connecté ou nom incorrect).");
                  return true;
               } else {
                  PlayerData data = this.plugin.getDataManager().getPlayerData(target.getUniqueId());
                  if (data == null && target.isOnline()) {
                     data = this.plugin.getDataManager().loadPlayer(target.getPlayer());
                  }

                  if (data == null) {
                     ChatUtil.sendMessage(sender, "&cImpossible de charger les données du joueur &e" + targetName + "&c.");
                     return true;
                  } else {
                     switch (sub) {
                        case "get":
                        case "see":
                        case "show":
                           ChatUtil.sendMessage(
                              sender, "&eSolde de &6" + data.getPlayerName() + " &e: &a" + String.format("%.2f", data.getCoins()) + " pièces&e."
                           );
                           break;
                        case "set":
                           if (args.length < 3) {
                              ChatUtil.sendMessage(sender, "&cUtilisation : /money set <joueur> <montant>");
                              return true;
                           }

                           try {
                              double amountxx = Double.parseDouble(args[2].replace(',', '.'));
                              data.setCoins(amountxx);
                              this.plugin.getDataManager().savePlayer(data);
                              ChatUtil.sendMessage(
                                 sender, "&aLe solde de &e" + data.getPlayerName() + " &aa été défini à &6" + String.format("%.2f", amountxx) + " pièces&a."
                              );
                              if (target.isOnline() && target.getPlayer() != null) {
                                 ChatUtil.sendMessage(
                                    target.getPlayer(), "&eUn administrateur a défini votre solde à &6" + String.format("%.2f", amountxx) + " pièces&e."
                                 );
                              }
                           } catch (NumberFormatException var19) {
                              ChatUtil.sendMessage(sender, "&cMontant invalide.");
                           }
                           break;
                        case "give":
                        case "add":
                           if (args.length < 3) {
                              ChatUtil.sendMessage(sender, "&cUtilisation : /money give <joueur> <montant>");
                              return true;
                           }

                           try {
                              double amountx = Double.parseDouble(args[2].replace(',', '.'));
                              if (amountx <= 0.0) {
                                 throw new NumberFormatException();
                              }

                              data.addCoins(amountx);
                              this.plugin.getDataManager().savePlayer(data);
                              ChatUtil.sendMessage(
                                 sender,
                                 "&aAjouté &6"
                                    + String.format("%.2f", amountx)
                                    + " pièces &aau solde de &e"
                                    + data.getPlayerName()
                                    + " &a(Nouveau solde : &6"
                                    + String.format("%.2f", data.getCoins())
                                    + "&a)."
                              );
                              if (target.isOnline() && target.getPlayer() != null) {
                                 ChatUtil.sendMessage(
                                    target.getPlayer(), "&aVous avez reçu &6" + String.format("%.2f", amountx) + " pièces &ad'un administrateur !"
                                 );
                              }
                           } catch (NumberFormatException var18) {
                              ChatUtil.sendMessage(sender, "&cMontant invalide.");
                           }
                           break;
                        case "take":
                        case "remove":
                           if (args.length < 3) {
                              ChatUtil.sendMessage(sender, "&cUtilisation : /money take <joueur> <montant>");
                              return true;
                           }

                           try {
                              double amount = Double.parseDouble(args[2].replace(',', '.'));
                              if (amount <= 0.0) {
                                 throw new NumberFormatException();
                              }

                              data.removeCoins(amount);
                              this.plugin.getDataManager().savePlayer(data);
                              ChatUtil.sendMessage(
                                 sender,
                                 "&eRetiré &6"
                                    + String.format("%.2f", amount)
                                    + " pièces &eau solde de &e"
                                    + data.getPlayerName()
                                    + " &e(Nouveau solde : &6"
                                    + String.format("%.2f", data.getCoins())
                                    + "&e)."
                              );
                              if (target.isOnline() && target.getPlayer() != null) {
                                 ChatUtil.sendMessage(target.getPlayer(), "&cUn administrateur vous a retiré &6" + String.format("%.2f", amount) + " pièces&c.");
                              }
                           } catch (NumberFormatException var17) {
                              ChatUtil.sendMessage(sender, "&cMontant invalide.");
                           }
                           break;
                        default:
                           this.sendHelp(sender);
                     }

                     return true;
                  }
               }
            }
         } else {
            return new BaltopCommand(this.plugin).onCommand(sender, command, label, args);
         }
      }
   }

   private void sendHelp(CommandSender sender) {
      ChatUtil.sendRaw(sender, "&8&m----------------------------------------");
      ChatUtil.sendRaw(sender, "  &6&lAdministration Économique (/money) :");
      ChatUtil.sendRaw(sender, "  &e/money get <joueur> &7: Voir le solde d'un joueur");
      ChatUtil.sendRaw(sender, "  &e/money set <joueur> <montant> &7: Définir le solde");
      ChatUtil.sendRaw(sender, "  &e/money give <joueur> <montant> &7: Donner de l'argent");
      ChatUtil.sendRaw(sender, "  &e/money take <joueur> <montant> &7: Retirer de l'argent");
      ChatUtil.sendRaw(sender, "  &e/money rectify <facteur> &7: Définir facteur anti-farm");
      ChatUtil.sendRaw(sender, "  &e/money rectify status <joueur> &7: Voir stats d'un joueur");
      ChatUtil.sendRaw(sender, "  &e/money top &7: Voir le classement des riches");
      ChatUtil.sendRaw(sender, "&8&m----------------------------------------");
   }
}
