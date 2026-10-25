package fr.esicraft.command;

import fr.esicraft.EsicraftPlugin;
import fr.esicraft.fun.ContractManager;
import fr.esicraft.race.Race;
import fr.esicraft.util.ChatUtil;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class ContractCommand implements CommandExecutor {
   private final EsicraftPlugin plugin;

   public ContractCommand(EsicraftPlugin plugin) {
      this.plugin = plugin;
   }

   public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
      if (!(sender instanceof Player player)) {
         ChatUtil.sendMessage(sender, "&cCommande réservée aux joueurs.");
         return true;
      }

      if (this.plugin.getRaceManager().getRace(player) == Race.NONE) {
         ChatUtil.sendMessage(player, "&cChoisissez d'abord un métier avec &e/race &cpour recevoir des contrats !");
         return true;
      }

      ContractManager.DayState state = this.plugin.getContractManager().getState(player);
      ChatUtil.sendRaw(player, "&8&m--------------------------------------------------");
      ChatUtil.sendRaw(player, "  &6&l📜 CONTRATS DU JOUR &7(" + state.completedCount() + "/" + ContractManager.CONTRACTS_PER_DAY + ")");
      ChatUtil.sendRaw(player, "");
      for (int i = 0; i < ContractManager.CONTRACTS_PER_DAY; i++) {
         ContractManager.Contract c = state.getContracts().get(i);
         int done = state.getProgress(i);
         int filled = (int)Math.round(10.0 * done / c.target());
         String bar = "&a" + "█".repeat(filled) + "&8" + "█".repeat(10 - filled);
         String mark = state.isClaimed(i) ? "&a✔" : "&e•";
         ChatUtil.sendRaw(player, "  " + mark + " &f" + c.type().describe(c.target()) + " &8» &6" + String.format("%.0f", c.reward()) + " pièces");
         ChatUtil.sendRaw(player, "     " + bar + " &7" + done + "/" + c.target());
      }

      ChatUtil.sendRaw(player, "");
      ChatUtil.sendRaw(player, "  &7Les 3 contrats accomplis : &6+400 pièces &7de bonus. Renouvellement à minuit.");
      ChatUtil.sendRaw(player, "&8&m--------------------------------------------------");
      return true;
   }
}
