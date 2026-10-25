package fr.esicraft.command;

import fr.esicraft.EsicraftPlugin;
import fr.esicraft.gui.NexusTrapGUI;
import fr.esicraft.guild.Guild;
import fr.esicraft.guild.GuildRole;
import fr.esicraft.nexus.Nexus;
import fr.esicraft.util.ChatUtil;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class NexusCommand implements CommandExecutor {
   private final EsicraftPlugin plugin;

   public NexusCommand(EsicraftPlugin plugin) {
      this.plugin = plugin;
   }

   public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
      if (sender instanceof Player player) {
         Guild guild = this.plugin.getGuildManager().getGuildByPlayer(player.getUniqueId());
         if (guild == null) {
            ChatUtil.sendMessage(player, "&cVous n'appartenez à aucune guilde.");
            return true;
         } else if (args.length <= 0 || !args[0].equalsIgnoreCase("place")) {
            Nexus nexus = guild.getNexus();
            if (nexus != null && nexus.isAlive()) {
               if (args.length > 0 && args[0].equalsIgnoreCase("info")) {
                  ChatUtil.sendRaw(player, "&8&m----------------------------------------");
                  ChatUtil.sendRaw(player, "  &5&lCœur de Guilde (Nexus) :");
                  ChatUtil.sendRaw(
                     player,
                     "  &7Points de Vie : &a"
                        + nexus.getCurrentHealth()
                        + " &7/ &a"
                        + nexus.getMaxHealth()
                        + " PV ("
                        + String.format("%.1f", nexus.getHealthPercentage())
                        + "%)"
                  );
                  ChatUtil.sendRaw(
                     player,
                     "  &7Position : &f"
                        + (
                           nexus.getLocation() != null
                              ? "X=" + nexus.getLocation().getBlockX() + ", Y=" + nexus.getLocation().getBlockY() + ", Z=" + nexus.getLocation().getBlockZ()
                              : "Inconnue"
                        )
                  );
                  ChatUtil.sendRaw(player, "  &7Spécialisation : " + guild.getSpecialization().getDisplayName());
                  ChatUtil.sendRaw(player, "  &7Pièges armés : &a" + nexus.getActiveTraps().size() + " / 5 actifs");
                  ChatUtil.sendRaw(player, "&8&m----------------------------------------");
                  return true;
               } else {
                  NexusTrapGUI.open(player, this.plugin);
                  return true;
               }
            } else {
               ChatUtil.sendMessage(player, "&cVotre guilde ne possède pas de Nexus actif. Posez-le avec &e/guild nexus place &cdans votre claim.");
               return true;
            }
         } else if (!guild.getRole(player.getUniqueId()).isAtLeast(GuildRole.OFFICER)) {
            ChatUtil.sendMessage(player, "&cSeuls les chefs et officiers peuvent poser le Nexus.");
            return true;
         } else {
            this.plugin.getNexusManager().placeNexus(guild, player.getLocation(), player);
            return true;
         }
      } else {
         ChatUtil.sendMessage(sender, "&cCommande réservée aux joueurs.");
         return true;
      }
   }
}
