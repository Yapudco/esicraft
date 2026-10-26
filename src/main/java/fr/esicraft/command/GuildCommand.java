package fr.esicraft.command;

import fr.esicraft.EsicraftPlugin;
import fr.esicraft.data.PlayerData;
import fr.esicraft.gui.GuildMainGUI;
import fr.esicraft.gui.GuildSpecGUI;
import fr.esicraft.guild.Guild;
import fr.esicraft.guild.GuildRole;
import fr.esicraft.util.ChatUtil;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.Sound;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;

public class GuildCommand implements CommandExecutor, TabCompleter {
   private final EsicraftPlugin plugin;

   public GuildCommand(EsicraftPlugin plugin) {
      this.plugin = plugin;
   }

   public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
      if (!(sender instanceof Player player)) {
         if (args.length > 1 && args[0].equalsIgnoreCase("cooldown") && args[1].equalsIgnoreCase("reset")) {
            if (args.length > 2) {
               String guildName = args[2];
               Guild targetGuild = this.plugin.getGuildManager().getGuildByName(guildName);
               if (targetGuild != null) {
                  this.plugin.getRaidManager().stopRaid(targetGuild.getId());
                  this.plugin.getRaidManager().resetCooldown(targetGuild.getId());
                  ChatUtil.sendMessage(sender, "&a✔ Le cooldown et les hordes pour la guilde " + targetGuild.getName() + " ont été réinitialisés !");
               } else {
                  ChatUtil.sendMessage(sender, "&cGuilde introuvable (" + guildName + ").");
               }
            } else {
               this.plugin.getRaidManager().stopAllRaids();
               this.plugin.getRaidManager().resetAllCooldowns();
               ChatUtil.sendMessage(sender, "&a✔ Tous les cooldowns de hordes et les raids ont été réinitialisés pour TOUTES les guildes !");
            }

            return true;
         } else {
            ChatUtil.sendMessage(sender, "&cCette commande est réservée aux joueurs.");
            return true;
         }
      } else if (args.length == 0) {
         Guild guild = this.plugin.getGuildManager().getGuildByPlayer(player.getUniqueId());
         if (guild != null) {
            GuildMainGUI.open(player, this.plugin);
         } else {
            this.sendHelp(player);
         }

         return true;
      } else {
         String sub = args[0].toLowerCase();
         switch (sub) {
            case "create": {
               if (args.length < 3) {
                  ChatUtil.sendMessage(player, "&cUtilisation : &e/guild create <nom> <tag>");
                  return true;
               }

               String name = args[1];
               String tag = args[2];
               if (tag.length() > 6) {
                  ChatUtil.sendMessage(player, "&cLe tag ne doit pas dépasser 6 caractères.");
                  return true;
               }

               this.plugin.getGuildManager().createGuild(player, name, tag);
               break;
            }
            case "invite": {
               if (args.length < 2) {
                  ChatUtil.sendMessage(player, "&cUtilisation : &e/guild invite <joueur>");
                  return true;
               }

               Guild guild = this.plugin.getGuildManager().getGuildByPlayer(player.getUniqueId());
               if (guild == null) {
                  ChatUtil.sendMessage(player, "&cVous n'avez pas de guilde.");
                  return true;
               }

               if (!guild.hasRoleAtLeast(player.getUniqueId(), GuildRole.OFFICER)) {
                  ChatUtil.sendMessage(player, "&cSeuls les officiers et chefs peuvent inviter des joueurs.");
                  return true;
               }

               Player target = Bukkit.getPlayer(args[1]);
               if (target == null) {
                  ChatUtil.sendMessage(player, "&cJoueur introuvable ou hors-ligne.");
                  return true;
               }

               if (this.plugin.getGuildManager().invitePlayer(guild, target, player)) {
                  ChatUtil.sendMessage(player, "&aInvitation envoyée à &e" + target.getName() + " &a!");
               }
               break;
            }
            case "join": {
               if (args.length < 2) {
                  ChatUtil.sendMessage(player, "&cUtilisation : &e/guild join <nom_guilde>");
                  return true;
               }

               Guild targetGuild = this.plugin.getGuildManager().getGuildByName(args[1]);
               if (targetGuild == null) {
                  ChatUtil.sendMessage(player, "&cGuilde introuvable.");
                  return true;
               }

               if (!this.plugin.getGuildManager().hasInvite(player, targetGuild)) {
                  ChatUtil.sendMessage(player, "&cVous n'avez pas d'invitation en attente pour cette guilde.");
                  return true;
               }

               this.plugin.getGuildManager().acceptInvite(player, targetGuild);
               break;
            }
            case "leave":
            case "quit":
            case "sortir":
            case "exit": {
               Guild guild = this.plugin.getGuildManager().getGuildByPlayer(player.getUniqueId());
               if (guild == null) {
                  ChatUtil.sendMessage(player, "&cVous n'avez pas de guilde.");
                  return true;
               }

               this.plugin.getGuildManager().leaveGuild(guild, player);
               break;
            }
            case "disband":
            case "dissoudre": {
               Guild guild = this.plugin.getGuildManager().getGuildByPlayer(player.getUniqueId());
               if (guild == null) {
                  ChatUtil.sendMessage(player, "&cVous n'avez pas de guilde.");
                  return true;
               }

               if (!guild.getLeaderUuid().equals(player.getUniqueId())) {
                  ChatUtil.sendMessage(player, "&cSeul le chef de guilde peut dissoudre la guilde.");
                  return true;
               }

               if (!this.plugin.getGuildManager().canDisbandNow(guild, player)) {
                  return true;
               }

               this.plugin.getGuildManager().disbandGuild(guild);
               break;
            }
            case "kick":
            case "expulser": {
               if (args.length < 2) {
                  ChatUtil.sendMessage(player, "&cUtilisation : /guild kick <joueur>");
                  return true;
               }

               Guild guild = this.plugin.getGuildManager().getGuildByPlayer(player.getUniqueId());
               if (guild == null) {
                  ChatUtil.sendMessage(player, "&cVous n'avez pas de guilde.");
                  return true;
               }

               OfflinePlayer target = Bukkit.getOfflinePlayerIfCached(args[1]);
               if (target == null) {
                  ChatUtil.sendMessage(player, "&cJoueur introuvable (jamais connecté ou nom incorrect).");
                  return true;
               }

               this.plugin.getGuildManager().kickPlayer(guild, player, target);
               break;
            }
            case "promote":
            case "promouvoir": {
               if (args.length < 2) {
                  ChatUtil.sendMessage(player, "&cUtilisation : &e/guild promote <joueur> [officier|membre]");
                  return true;
               }

               Guild guild = this.plugin.getGuildManager().getGuildByPlayer(player.getUniqueId());
               if (guild == null) {
                  ChatUtil.sendMessage(player, "&cVous n'avez pas de guilde.");
                  return true;
               }

               OfflinePlayer target = Bukkit.getOfflinePlayerIfCached(args[1]);
               if (target == null) {
                  ChatUtil.sendMessage(player, "&cJoueur introuvable (jamais connecté ou nom incorrect).");
                  return true;
               }

               GuildRole specificRole = null;
               if (args.length > 2) {
                  String rStr = args[2].toLowerCase();
                  if (rStr.startsWith("off") || rStr.startsWith("officier") || rStr.startsWith("officer")) {
                     specificRole = GuildRole.OFFICER;
                  } else if (rStr.startsWith("mem") || rStr.startsWith("joueur") || rStr.startsWith("membre") || rStr.startsWith("simple")) {
                     specificRole = GuildRole.MEMBER;
                  } else if (rStr.startsWith("rec") || rStr.startsWith("recrue")) {
                     specificRole = GuildRole.RECRUIT;
                  }
               }

               this.plugin.getGuildManager().promoteOrDemotePlayer(guild, player, target, specificRole);
               break;
            }
            case "demote":
            case "retrograder": {
               if (args.length < 2) {
                  ChatUtil.sendMessage(player, "&cUtilisation : &e/guild demote <joueur>");
                  return true;
               }

               Guild guild = this.plugin.getGuildManager().getGuildByPlayer(player.getUniqueId());
               if (guild == null) {
                  ChatUtil.sendMessage(player, "&cVous n'avez pas de guilde.");
                  return true;
               }

               OfflinePlayer target = Bukkit.getOfflinePlayerIfCached(args[1]);
               if (target == null) {
                  ChatUtil.sendMessage(player, "&cJoueur introuvable (jamais connecté ou nom incorrect).");
                  return true;
               }

               GuildRole currentTargetRole = guild.getRole(target.getUniqueId());
               if (currentTargetRole == null) {
                  ChatUtil.sendMessage(player, "&cCe joueur ne fait pas partie de votre guilde.");
                  return true;
               }

               GuildRole lowerRole = currentTargetRole == GuildRole.OFFICER ? GuildRole.MEMBER : GuildRole.RECRUIT;
               if (currentTargetRole == GuildRole.LEADER) {
                  ChatUtil.sendMessage(player, "&cLe chef ne peut pas être rétrogradé (utilisez &e/guild transfer&c).");
                  return true;
               }

               if (currentTargetRole == GuildRole.RECRUIT) {
                  ChatUtil.sendMessage(player, "&cCe joueur est déjà au rang le plus bas.");
                  return true;
               }

               this.plugin.getGuildManager().promoteOrDemotePlayer(guild, player, target, lowerRole);
               break;
            }
            case "transfer":
            case "transferer": {
               if (args.length < 2) {
                  ChatUtil.sendMessage(player, "&cUtilisation : /guild transfer <joueur>");
                  return true;
               }

               Guild guild = this.plugin.getGuildManager().getGuildByPlayer(player.getUniqueId());
               if (guild == null) {
                  ChatUtil.sendMessage(player, "&cVous n'avez pas de guilde.");
                  return true;
               }

               Player target = Bukkit.getPlayer(args[1]);
               if (target == null || !target.isOnline()) {
                  ChatUtil.sendMessage(player, "&cJoueur introuvable ou hors ligne.");
                  return true;
               }

               this.plugin.getGuildManager().transferLeadership(guild, player, target);
               break;
            }
            case "claim": {
               if (args.length > 1 && (args[1].equalsIgnoreCase("show") || args[1].equalsIgnoreCase("view") || args[1].equalsIgnoreCase("borders"))) {
                  this.plugin.getClaimVisualizerManager().toggleVisualizer(player);
                  return true;
               }

               Guild guild = this.plugin.getGuildManager().getGuildByPlayer(player.getUniqueId());
               if (guild == null) {
                  ChatUtil.sendMessage(player, "&cVous n'avez pas de guilde.");
                  return true;
               }

               if (!guild.hasRoleAtLeast(player.getUniqueId(), GuildRole.OFFICER)) {
                  ChatUtil.sendMessage(player, "&cSeuls les officiers et chefs peuvent claim des territoires.");
                  return true;
               }

               this.plugin.getGuildManager().claimChunk(guild, player);
               break;
            }
            case "show":
            case "showclaims":
            case "borders": {
               this.plugin.getClaimVisualizerManager().toggleVisualizer(player);
               break;
            }
            case "unclaim": {
               Guild guild = this.plugin.getGuildManager().getGuildByPlayer(player.getUniqueId());
               if (guild == null) {
                  ChatUtil.sendMessage(player, "&cVous n'avez pas de guilde.");
                  return true;
               }

               if (!guild.hasRoleAtLeast(player.getUniqueId(), GuildRole.OFFICER)) {
                  ChatUtil.sendMessage(player, "&cSeuls les officiers et chefs peuvent unclaim.");
                  return true;
               }

               this.plugin.getGuildManager().unclaimChunk(guild, player);
               break;
            }
            case "spec": {
               Guild guild = this.plugin.getGuildManager().getGuildByPlayer(player.getUniqueId());
               if (guild == null) {
                  ChatUtil.sendMessage(player, "&cVous n'avez pas de guilde.");
                  return true;
               }

               GuildSpecGUI.open(player, this.plugin);
               break;
            }
            case "nexus": {
               Guild guild = this.plugin.getGuildManager().getGuildByPlayer(player.getUniqueId());
               if (guild == null) {
                  ChatUtil.sendMessage(player, "&cVous n'avez pas de guilde.");
                  return true;
               }

               if (args.length > 1 && args[1].equalsIgnoreCase("place")) {
                  if (!guild.hasRoleAtLeast(player.getUniqueId(), GuildRole.OFFICER)) {
                     ChatUtil.sendMessage(player, "&cSeul le chef ou un officier peut poser le Nexus.");
                     return true;
                  }

                  this.plugin.getNexusManager().placeNexus(guild, player.getLocation(), player);
               } else {
                  ChatUtil.sendMessage(
                     player,
                     "&eNexus de votre guilde : "
                        + (
                           guild.hasNexus()
                              ? "&aActif (" + guild.getNexus().getCurrentHealth() + "/" + guild.getNexus().getMaxHealth() + " PV)"
                              : "&cNon posé. Faites /guild nexus place"
                        )
                  );
               }
               break;
            }
            case "deposit":
            case "bank": {
               Guild guild = this.plugin.getGuildManager().getGuildByPlayer(player.getUniqueId());
               if (guild == null) {
                  ChatUtil.sendMessage(player, "&cVous n'avez pas de guilde.");
                  return true;
               }

               if (args.length < 2) {
                  ChatUtil.sendMessage(player, "&eSolde de la banque de guilde : &6" + String.format("%.2f", guild.getBankBalance()) + " pièces.");
                  return true;
               }

               try {
                  double amount = Double.parseDouble(args[1]);
                  if (amount <= 0.0) {
                     throw new NumberFormatException();
                  }

                  PlayerData pData = this.plugin.getDataManager().getPlayerData(player.getUniqueId());
                  if (pData != null && pData.getCoins() >= amount) {
                     pData.removeCoins(amount);
                     guild.depositBank(amount);
                     this.plugin.getDataManager().savePlayerAndGuild(pData, guild);
                     ChatUtil.sendMessage(player, "&aDépôt de &6" + amount + " pièces &aeffectué dans la banque de guilde !");
                  } else {
                     ChatUtil.sendMessage(player, "&cVous n'avez pas assez d'argent sur vous.");
                  }
               } catch (NumberFormatException var15) {
                  ChatUtil.sendMessage(player, "&cMontant invalide.");
               }
               break;
            }
            case "info": {
               Guild guild = args.length > 1
                  ? this.plugin.getGuildManager().getGuildByName(args[1])
                  : this.plugin.getGuildManager().getGuildByPlayer(player.getUniqueId());
               if (guild == null) {
                  ChatUtil.sendMessage(player, "&cGuilde introuvable.");
                  return true;
               }

               OfflinePlayer leader = Bukkit.getOfflinePlayer(guild.getLeaderUuid());
               ChatUtil.sendRaw(player, "&8&m----------------------------------------");
               ChatUtil.sendRaw(player, "&6&lGuilde : &e" + guild.getName() + " &8[&6" + guild.getTag() + "&8]");
               ChatUtil.sendRaw(player, "&7Chef : &f" + (leader.getName() != null ? leader.getName() : "Inconnu"));
               ChatUtil.sendRaw(player, "&7Spécialisation : " + guild.getSpecialization().getDisplayName());
               ChatUtil.sendRaw(player, "&7Membres : &a" + guild.getMembers().size());
               int maxClaims = this.plugin.getGuildManager().getMaxClaims(guild);
               String maxStr = maxClaims == Integer.MAX_VALUE ? "Illimité" : String.valueOf(maxClaims);
               double nextClaimCost = this.plugin.getGuildManager().calculateNextClaimCost(guild);
               ChatUtil.sendRaw(
                  player,
                  "&7Territoires : &e"
                     + guild.getClaimedChunks().size()
                     + "/"
                     + maxStr
                     + " chunks &8(Prochain claim : &6"
                     + String.format("%.2f", nextClaimCost)
                     + " pièces&8)"
               );
               ChatUtil.sendRaw(player, "&7Banque de Guilde : &a" + String.format("%.2f", guild.getBankBalance()) + " pièces");
               for (String line : this.plugin.getSynergy().describe(guild)) {
                  ChatUtil.sendRaw(player, line);
               }

               ChatUtil.sendRaw(
                  player,
                  "&7Nexus : " + (guild.hasNexus() ? "&a" + guild.getNexus().getCurrentHealth() + "/" + guild.getNexus().getMaxHealth() + " PV" : "&cNon posé")
               );
               ChatUtil.sendRaw(player, "&8&m----------------------------------------");
               break;
            }
            case "annex": {
               if (args.length < 2) {
                  ChatUtil.sendMessage(player, "&cUtilisation : &e/guild annex <join|decline>");
                  return true;
               }

               String action = args[1].toLowerCase();
               if (action.equals("join") || action.equals("rejoindre") || action.equals("accept")) {
                  this.plugin.getGuildManager().acceptAnnexation(player);
               } else if (!action.equals("decline") && !action.equals("refuser") && !action.equals("leave")) {
                  ChatUtil.sendMessage(player, "&cAction invalide. Utilisez &e/guild annex join &cou &e/guild annex decline&c.");
               } else {
                  this.plugin.getGuildManager().declineAnnexation(player);
               }
               break;
            }
            case "cooldown": {
               if (!player.hasPermission("esicraft.admin") && !player.isOp()) {
                  ChatUtil.sendMessage(player, "&cVous n'avez pas la permission d'utiliser cette commande admin.");
                  return true;
               }

               if (args.length > 1 && args[1].equalsIgnoreCase("reset")) {
                  if (args.length > 2) {
                     String guildName = args[2];
                     Guild targetGuild = this.plugin.getGuildManager().getGuildByName(guildName);
                     if (targetGuild != null) {
                        this.plugin.getRaidManager().stopRaid(targetGuild.getId());
                        this.plugin.getRaidManager().resetCooldown(targetGuild.getId());
                        ChatUtil.sendMessage(
                           player, "&a✔ Le cooldown et les hordes actives pour la guilde &e" + targetGuild.getName() + " &aont été réinitialisés !"
                        );
                     } else {
                        ChatUtil.sendMessage(player, "&cGuilde introuvable (&e" + guildName + "&c).");
                     }
                  } else {
                     this.plugin.getRaidManager().stopAllRaids();
                     this.plugin.getRaidManager().resetAllCooldowns();
                     ChatUtil.sendMessage(player, "&a✔ Tous les cooldowns de hordes et les raids en cours ont été réinitialisés pour TOUTES les guildes !");
                  }

                  ChatUtil.playSound(player, Sound.UI_TOAST_CHALLENGE_COMPLETE, 1.0F, 1.5F);
               } else {
                  ChatUtil.sendMessage(player, "&cUtilisation : &e/guild cooldown reset [nom_guilde]");
               }
               break;
            }
            default:
               this.sendHelp(player);
         }

         return true;
      }
   }

   public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
      List<String> completions = new ArrayList<>();
      if (args.length == 1) {
         List<String> subs = new ArrayList<>(
            List.of(
               "create",
               "invite",
               "join",
               "leave",
               "disband",
               "kick",
               "promote",
               "demote",
               "transfer",
               "claim",
               "unclaim",
               "show",
               "spec",
               "nexus",
               "bank",
               "info",
               "annex"
            )
         );
         if (sender.hasPermission("esicraft.admin") || sender.isOp()) {
            subs.add("cooldown");
         }

         for (String s : subs) {
            if (s.toLowerCase().startsWith(args[0].toLowerCase())) {
               completions.add(s);
            }
         }
      } else if (args.length == 2
         && (
            args[0].equalsIgnoreCase("promote")
               || args[0].equalsIgnoreCase("demote")
               || args[0].equalsIgnoreCase("kick")
               || args[0].equalsIgnoreCase("transfer")
         )) {
         if (sender instanceof Player p) {
            Guild guild = this.plugin.getGuildManager().getGuildByPlayer(p.getUniqueId());
            if (guild != null) {
               for (UUID memberUuid : guild.getMembers().keySet()) {
                  if (!memberUuid.equals(p.getUniqueId())) {
                     OfflinePlayer offP = Bukkit.getOfflinePlayer(memberUuid);
                     if (offP.getName() != null && offP.getName().toLowerCase().startsWith(args[1].toLowerCase())) {
                        completions.add(offP.getName());
                     }
                  }
               }
            }
         }
      } else if (args.length == 3 && args[0].equalsIgnoreCase("promote")) {
         for (String r : List.of("officier", "membre", "recrue")) {
            if (r.startsWith(args[2].toLowerCase())) {
               completions.add(r);
            }
         }
      } else if (args.length == 2 && args[0].equalsIgnoreCase("annex")) {
         for (String opt : List.of("join", "decline")) {
            if (opt.startsWith(args[1].toLowerCase())) {
               completions.add(opt);
            }
         }
      } else if (args.length == 2 && args[0].equalsIgnoreCase("cooldown")) {
         if ("reset".startsWith(args[1].toLowerCase())) {
            completions.add("reset");
         }
      } else if (args.length == 3 && args[0].equalsIgnoreCase("cooldown") && args[1].equalsIgnoreCase("reset")) {
         for (Guild g : this.plugin.getGuildManager().getAllGuilds()) {
            if (g.getName().toLowerCase().startsWith(args[2].toLowerCase())) {
               completions.add(g.getName());
            }
         }
      }

      return completions;
   }

   private void sendHelp(Player player) {
      ChatUtil.sendRaw(player, "&8&m----------------------------------------");
      ChatUtil.sendRaw(player, "  &6&lCommandes de Guilde Esicraft :");
      ChatUtil.sendRaw(player, "  &e/guild create <nom> <tag> &7: Fonder une guilde");
      ChatUtil.sendRaw(player, "  &e/guild invite <joueur> &7: Inviter un membre");
      ChatUtil.sendRaw(player, "  &e/guild join <guilde> &7: Accepter une invitation");
      ChatUtil.sendRaw(player, "  &e/guild leave &7: Quitter votre guilde actuelle");
      ChatUtil.sendRaw(player, "  &e/guild disband &7: Dissoudre votre guilde (Chef)");
      ChatUtil.sendRaw(player, "  &e/guild kick <joueur> &7: Expulser un membre");
      ChatUtil.sendRaw(player, "  &e/guild promote <joueur> &7: Promouvoir Officier / rétrograder");
      ChatUtil.sendRaw(player, "  &e/guild demote <joueur> &7: Rétrograder au rang de Membre");
      ChatUtil.sendRaw(player, "  &e/guild transfer <joueur> &7: Transférer le leadership");
      ChatUtil.sendRaw(player, "  &e/guild claim &7: Revendiquer le chunk actuel");
      ChatUtil.sendRaw(player, "  &e/guild claim show &7: Afficher les claims en surbrillance");
      ChatUtil.sendRaw(player, "  &e/guild unclaim &7: Abandonner le chunk actuel");
      ChatUtil.sendRaw(player, "  &e/guild spec &7: Choisir l'orientation de guilde");
      ChatUtil.sendRaw(player, "  &e/guild nexus place &7: Placer le Nexus protecteur");
      ChatUtil.sendRaw(player, "  &e/guild bank <montant> &7: Déposer dans la banque");
      ChatUtil.sendRaw(player, "  &e/guild info [nom] &7: Voir les détails d'une guilde");
      if (player.hasPermission("esicraft.admin") || player.isOp()) {
         ChatUtil.sendRaw(player, "  &c/guild cooldown reset [nom] &7: (Admin) Reset cooldown & hordes");
      }

      ChatUtil.sendRaw(player, "&8&m----------------------------------------");
   }
}
