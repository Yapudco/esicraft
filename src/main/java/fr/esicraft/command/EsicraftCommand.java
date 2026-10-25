package fr.esicraft.command;

import fr.esicraft.EsicraftPlugin;
import fr.esicraft.util.ChatUtil;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;

public class EsicraftCommand implements CommandExecutor {
   private final EsicraftPlugin plugin;

   public EsicraftCommand(EsicraftPlugin plugin) {
      this.plugin = plugin;
   }

   public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
      if (args.length <= 0 || !args[0].equalsIgnoreCase("reload")) {
         ChatUtil.sendRaw(sender, "&8&m--------------------------------------------------");
         ChatUtil.sendRaw(sender, "  &6&l⚔️ ESICRAFT-PLUGIN &7- &eSurvie Stratégique v1.0.0");
         ChatUtil.sendRaw(sender, "  &7Développé pour des parties épiques à 50+ joueurs.");
         ChatUtil.sendRaw(sender, "");
         ChatUtil.sendRaw(sender, "  &e/race &7: Choisir ou consulter sa classe & métiers");
         ChatUtil.sendRaw(sender, "  &e/guild &7: Gestion des guildes, spécialisations & claims");
         ChatUtil.sendRaw(sender, "  &e/nexus &7: Statut & placement du cœur de guilde");
         ChatUtil.sendRaw(sender, "  &e/age &7: Progression technologique de guilde & dons");
         ChatUtil.sendRaw(sender, "  &e/ah &7: Marché central & contrats de hordes");
         ChatUtil.sendRaw(sender, "  &e/guide &7: Obtenir le livre de règles officiel");
         ChatUtil.sendRaw(sender, "  &c/money &7: (Admin) Gérer l'argent des joueurs");
         ChatUtil.sendRaw(sender, "&8&m--------------------------------------------------");
         return true;
      } else if (!sender.hasPermission("esicraft.admin")) {
         ChatUtil.sendMessage(sender, "&cPermission insuffisante.");
         return true;
      } else {
         this.plugin.reloadConfig();
         this.plugin.getAgeManager().loadConfig();
         ChatUtil.sendMessage(sender, "&aConfiguration de &6Esicraft-Plugin &arechargée avec succès !");
         return true;
      }
   }
}
