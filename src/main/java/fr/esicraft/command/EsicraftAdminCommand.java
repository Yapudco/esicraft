package fr.esicraft.command;

import fr.esicraft.EsicraftPlugin;
import fr.esicraft.age.AgeType;
import fr.esicraft.data.PlayerData;
import fr.esicraft.guild.Guild;
import fr.esicraft.nexus.Nexus;
import fr.esicraft.raid.RaidType;
import fr.esicraft.util.ChatUtil;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class EsicraftAdminCommand implements CommandExecutor {
   private final EsicraftPlugin plugin;

   public EsicraftAdminCommand(EsicraftPlugin plugin) {
      this.plugin = plugin;
   }

   public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
      if (!sender.hasPermission("esicraft.admin")) {
         ChatUtil.sendMessage(sender, "&cPermission insuffisante.");
         return true;
      } else if (args.length == 0) {
         this.sendAdminHelp(sender);
         return true;
      } else {
         String sub = args[0].toLowerCase();
         switch (sub) {
            case "setage": {
               if (args.length < 3) {
                  ChatUtil.sendMessage(sender, "&cUtilisation : /esicraftadmin setage <guilde> <1-5>");
                  return true;
               }

               Guild target = this.plugin.getGuildManager().getGuildByName(args[1]);
               if (target == null) {
                  ChatUtil.sendMessage(sender, "&cGuilde introuvable.");
                  return true;
               }

               try {
                  int level = Integer.parseInt(args[2]);
                  AgeType age = AgeType.fromLevel(level);
                  target.setCurrentAge(age);
                  target.clearDonations();
                  this.plugin.getDataManager().saveGuild(target);
                  ChatUtil.broadcast("&6&l[Admin] &eL'Âge de la guilde &6" + target.getName() + " &ea été défini sur : " + age.getDisplayName());
               } catch (NumberFormatException var13) {
                  ChatUtil.sendMessage(sender, "&cNiveau d'âge invalide (1 à 5).");
               }
               break;
            }
            case "spawnraid": {
               if (args.length < 3) {
                  ChatUtil.sendMessage(sender, "&cUtilisation : /esicraftadmin spawnraid <guilde> <TYPE_RAID>");
                  return true;
               }

               Guild target = this.plugin.getGuildManager().getGuildByName(args[1]);
               if (target == null) {
                  ChatUtil.sendMessage(sender, "&cGuilde introuvable.");
                  return true;
               }

               try {
                  RaidType type = RaidType.valueOf(args[2].toUpperCase());
                  if (this.plugin.getRaidManager().launchAdminRaid(target, type)) {
                     ChatUtil.sendMessage(sender, "&aRaid forcé lancé avec succès sur la guilde &e" + target.getName());
                  } else {
                     ChatUtil.sendMessage(sender, "&cLe raid n'a pas pu être lancé (pas de Nexus, aucun défenseur sur son territoire, raid déjà en cours, cooldown ou /pvp désactivé).");
                  }
               } catch (IllegalArgumentException var12) {
                  ChatUtil.sendMessage(sender, "&cType de raid invalide. Types : ARMORED_ZOMBIES, INCENDIARY_SKELETONS, TOXIC_SPIDERS, PILLAGER_SIEGE, NETHER_DEMONS, PHANTOM_SWARM, CREEPER_BLITZ, ENDER_INCURSION");
               }
               break;
            }
            case "givecoins": {
               if (args.length < 3) {
                  ChatUtil.sendMessage(sender, "&cUtilisation : /esicraftadmin givecoins <joueur> <montant>");
                  return true;
               }

               Player target = Bukkit.getPlayer(args[1]);
               if (target == null) {
                  ChatUtil.sendMessage(sender, "&cJoueur introuvable.");
                  return true;
               }

               try {
                  double amount = Double.parseDouble(args[2].replace(',', '.'));
                  if (!(amount <= 0.0) && !Double.isNaN(amount) && !Double.isInfinite(amount)) {
                     PlayerData data = this.plugin.getDataManager().getPlayerData(target.getUniqueId());
                     if (data != null) {
                        data.addCoins(amount);
                        this.plugin.getDataManager().savePlayer(data);
                        ChatUtil.sendMessage(sender, "&aAjouté &6" + String.format("%.2f", amount) + " pièces &aau joueur &e" + target.getName());
                        ChatUtil.sendMessage(target, "&aVous avez reçu &6" + String.format("%.2f", amount) + " pièces &ad'un administrateur !");
                     }
                     break;
                  }

                  ChatUtil.sendMessage(sender, "&cLe montant doit être un nombre positif.");
                  return true;
               } catch (NumberFormatException var14) {
                  ChatUtil.sendMessage(sender, "&cMontant invalide.");
                  break;
               }
            }
            case "forcenexus": {
               if (args.length < 2) {
                  ChatUtil.sendMessage(sender, "&cUtilisation : /esicraftadmin forcenexus <guilde>");
                  return true;
               }

               Guild target = this.plugin.getGuildManager().getGuildByName(args[1]);
               if (target == null || !target.hasNexus()) {
                  ChatUtil.sendMessage(sender, "&cGuilde ou Nexus introuvable.");
                  return true;
               }

               Nexus nexus = target.getNexus();
               nexus.setCurrentHealth(nexus.getMaxHealth());
               nexus.setAlive(true);
               target.setVulnerableUntil(0L);
               this.plugin.getNexusManager().updateHologram(target);
               this.plugin.getDataManager().saveGuild(target);
               ChatUtil.sendMessage(sender, "&aLe Nexus de &e" + target.getName() + " &aa été restauré à 100% de ses PV !");
               break;
            }
            default:
               this.sendAdminHelp(sender);
         }

         return true;
      }
   }

   private void sendAdminHelp(CommandSender sender) {
      ChatUtil.sendRaw(sender, "&8&m----------------------------------------");
      ChatUtil.sendRaw(sender, "  &c&lCommandes Administration Esicraft :");
      ChatUtil.sendRaw(sender, "  &e/esicraftadmin setage <1-6> &7: Définir l'âge");
      ChatUtil.sendRaw(sender, "  &e/esicraftadmin spawnraid <guilde> <type> &7: Lancer un raid");
      ChatUtil.sendRaw(sender, "  &e/esicraftadmin givecoins <joueur> <montant> &7: Donner de l'argent");
      ChatUtil.sendRaw(sender, "  &e/esicraftadmin forcenexus <guilde> &7: Réparer un Nexus");
      ChatUtil.sendRaw(sender, "&8&m----------------------------------------");
   }
}
