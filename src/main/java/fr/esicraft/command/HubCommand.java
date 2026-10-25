package fr.esicraft.command;

import com.google.common.io.ByteArrayDataOutput;
import com.google.common.io.ByteStreams;
import fr.esicraft.EsicraftPlugin;
import fr.esicraft.util.ChatUtil;
import org.bukkit.Sound;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

public class HubCommand implements CommandExecutor {
   private final EsicraftPlugin plugin;

   public HubCommand(EsicraftPlugin plugin) {
      this.plugin = plugin;
   }

   public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
      if (sender instanceof Player player) {
         ChatUtil.sendMessage(player, "&aConnexion au Hub en cours...");
         ChatUtil.playSound(player, Sound.ENTITY_ENDERMAN_TELEPORT, 1.0F, 1.2F);

         try {
            ByteArrayDataOutput out = ByteStreams.newDataOutput();
            out.writeUTF("Connect");
            out.writeUTF("hub");
            player.sendPluginMessage(this.plugin, "BungeeCord", out.toByteArray());
         } catch (Exception var7) {
         }

         player.performCommand("server hub");
         return true;
      } else {
         sender.sendMessage(ChatUtil.color("&cSeul un joueur peut exécuter cette commande."));
         return true;
      }
   }
}
