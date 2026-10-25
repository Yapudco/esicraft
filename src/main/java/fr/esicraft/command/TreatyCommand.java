package fr.esicraft.command;

import fr.esicraft.EsicraftPlugin;
import fr.esicraft.guild.Guild;
import fr.esicraft.treaty.Treaty;
import fr.esicraft.treaty.TreatyGui;
import fr.esicraft.treaty.TreatyManager;
import fr.esicraft.util.ChatUtil;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

/** /traite : ouvre le menu des traités de sa guilde (tous les membres peuvent consulter, les officiers agissent). */
public class TreatyCommand implements CommandExecutor {
   private final EsicraftPlugin plugin;

   public TreatyCommand(EsicraftPlugin plugin) {
      this.plugin = plugin;
   }

   public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
      if (!(sender instanceof Player player)) {
         ChatUtil.sendMessage(sender, "&cCommande réservée aux joueurs.");
         return true;
      }

      Guild guild = this.plugin.getGuildManager().getGuildByPlayer(player.getUniqueId());
      if (guild == null) {
         ChatUtil.sendMessage(player, "&cVous devez appartenir à une guilde pour consulter les traités.");
         return true;
      }

      TreatyManager tm = this.plugin.getTreatyManager();
      if (args.length > 0 && args[0].equalsIgnoreCase("liste")) {
         if (tm.treatiesOf(guild.getId()).isEmpty()) {
            ChatUtil.sendMessage(player, "&7Aucun traité en vigueur.");
         }

         for (Treaty t : tm.treatiesOf(guild.getId())) {
            Guild other = this.plugin.getGuildManager().getGuildById(t.other(guild.getId()));
            ChatUtil.sendMessage(player, "&d" + t.getType().getLabel() + " &7avec &f" + (other != null ? other.getName() : "?")
               + (t.getExpiresAt() > 0L ? " &7(" + TreatyManager.formatDuration(t.getExpiresAt() - System.currentTimeMillis()) + ")" : ""));
         }

         return true;
      }

      TreatyGui.openMain(this.plugin, player, guild);
      return true;
   }
}
