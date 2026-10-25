package fr.esicraft.command;

import fr.esicraft.EsicraftPlugin;
import fr.esicraft.race.Race;
import fr.esicraft.util.ChatUtil;
import fr.esicraft.util.Lang;
import java.time.Duration;
import net.kyori.adventure.title.Title;
import net.kyori.adventure.title.Title.Times;
import org.bukkit.Bukkit;
import org.bukkit.Sound;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.potion.PotionEffect;

public class GameCommand implements CommandExecutor {
   private final EsicraftPlugin plugin;

   public GameCommand(EsicraftPlugin plugin) {
      this.plugin = plugin;
   }

   public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
      if (!sender.hasPermission("esicraft.admin")) {
         ChatUtil.sendMessage(sender, "&cCette commande est strictement réservée aux administrateurs.");
         return true;
      } else if (args.length != 0 && args[0].equalsIgnoreCase("reset")) {
         if (args.length >= 2 && args[1].equalsIgnoreCase("confirm")) {
            this.plugin.getDataManager().resetGame();

            for (Player p : Bukkit.getOnlinePlayers()) {
               for (PotionEffect effect : p.getActivePotionEffects()) {
                  p.removePotionEffect(effect.getType());
               }

               p.setFireTicks(0);
               this.plugin.getRaceManager().applyRaceAttributes(p, Race.NONE);
               p.setHealth(20.0);
               p.setFoodLevel(20);
               p.setSaturation(20.0F);
               p.showTitle(
                  Title.title(
                     ChatUtil.component(Lang.tr(p, "&4&lRESET GLOBAL !")),
                     ChatUtil.component(Lang.tr(p, "&cEffets, guildes et monde réinitialisés")),
                     Times.times(Duration.ofMillis(500L), Duration.ofMillis(4000L), Duration.ofMillis(1000L))
                  )
               );
               ChatUtil.playSound(p, Sound.UI_TOAST_CHALLENGE_COMPLETE, 1.0F, 0.5F);
            }

            ChatUtil.broadcast("&m--------------------------------------------------");
            ChatUtil.broadcast("&4&l\ud83d\udca5 RÉINITIALISATION GLOBALE DU SERVEUR ! \ud83d\udca5");
            ChatUtil.broadcast("&cToutes les guildes, territoires, cœurs de Nexus et fortunes ont été réinitialisés !");
            ChatUtil.broadcast("&eChaque joueur repart avec 100 pièces et peut refonder une nouvelle dynastie (&e/guild&e, &e/race&e) !");
            ChatUtil.broadcast("&m--------------------------------------------------");
            ChatUtil.sendMessage(sender, "&aLe jeu a été réinitialisé avec succès.");
            return true;
         } else {
            ChatUtil.sendRaw(sender, "&8&m--------------------------------------------------");
            ChatUtil.sendRaw(sender, "  &4&l⚠️ ATTENTION : DESTRUCTION TOTALE DES DONNÉES !");
            ChatUtil.sendRaw(sender, "  &cCette action va supprimer DÉFINITIVEMENT :");
            ChatUtil.sendRaw(sender, "   &8• &cToutes les guildes et leurs territoires (claims)");
            ChatUtil.sendRaw(sender, "   &8• &cTous les Nexus et améliorations");
            ChatUtil.sendRaw(sender, "   &8• &cToutes les fortunes et banques (reset à 100 pièces)");
            ChatUtil.sendRaw(sender, "   &8• &cTous les items en vente à l'Hôtel des Ventes");
            ChatUtil.sendRaw(sender, "");
            ChatUtil.sendRaw(sender, "  &ePour confirmer le reset global, tapez :");
            ChatUtil.sendRaw(sender, "  &4&l➤ &c/game reset confirm");
            ChatUtil.sendRaw(sender, "&8&m--------------------------------------------------");
            return true;
         }
      } else {
         ChatUtil.sendRaw(sender, "&8&m----------------------------------------");
         ChatUtil.sendRaw(sender, "  &4&lAdministration du Jeu (/game) :");
         ChatUtil.sendRaw(sender, "  &c/game reset confirm &7: Réinitialise totalement le jeu (Guildes, Argent, Nexus, Claims)");
         ChatUtil.sendRaw(sender, "&8&m----------------------------------------");
         return true;
      }
   }
}
