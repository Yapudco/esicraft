package fr.esicraft.command;

import fr.esicraft.EsicraftPlugin;
import fr.esicraft.guild.Guild;
import fr.esicraft.guild.GuildRole;
import fr.esicraft.rail.RailGui;
import fr.esicraft.util.ChatUtil;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

/** /rail : gestion du réseau ferroviaire de la guilde (péage, relations, gares). Réservé aux officiers. */
public class RailCommand implements CommandExecutor {
   private final EsicraftPlugin plugin;

   public RailCommand(EsicraftPlugin plugin) {
      this.plugin = plugin;
   }

   public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
      if (!(sender instanceof Player player)) {
         ChatUtil.sendMessage(sender, "&cCommande réservée aux joueurs.");
         return true;
      }

      Guild guild = this.plugin.getGuildManager().getGuildByPlayer(player.getUniqueId());
      if (guild == null) {
         ChatUtil.sendMessage(player, "&cVous devez appartenir à une guilde pour gérer un réseau ferroviaire.");
      } else if (!guild.hasRoleAtLeast(player.getUniqueId(), GuildRole.OFFICER)) {
         ChatUtil.sendMessage(player, "&cRéservé aux officiers et au chef de guilde.");
      } else {
         RailGui.openMain(this.plugin, player, guild);
      }

      return true;
   }
}
