package fr.esicraft.command;

import fr.esicraft.EsicraftPlugin;
import fr.esicraft.rail.RailGui;
import fr.esicraft.rail.RailManager;
import fr.esicraft.rail.Station;
import fr.esicraft.util.ChatUtil;
import org.bukkit.Location;
import org.bukkit.Tag;
import org.bukkit.World;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

/** /station : ouvre le menu de voyage de la gare où se trouve le joueur. */
public class StationCommand implements CommandExecutor {
   private static final double RADIUS = 8.0;
   private final EsicraftPlugin plugin;

   public StationCommand(EsicraftPlugin plugin) {
      this.plugin = plugin;
   }

   public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
      if (!(sender instanceof Player player)) {
         ChatUtil.sendMessage(sender, "&cCommande réservée aux joueurs.");
         return true;
      }

      if (!this.plugin.getConfig().getBoolean("rail.enabled", true)) {
         ChatUtil.sendMessage(player, "&cLe réseau ferroviaire est désactivé.");
         return true;
      }

      if (args.length > 0 && args[0].equalsIgnoreCase("fret")) {
         String sub = args.length > 1 ? args[1].toLowerCase() : "";
         switch (sub) {
            case "confirmer" -> this.plugin.getFreight().confirm(player);
            case "annuler" -> this.plugin.getFreight().cancel(player);
            case "recuperer", "récupérer" -> this.plugin.getFreight().openBox(player);
            default -> ChatUtil.sendMessage(player, "&6🚂 Fret : Maj + clic sur une destination dans &e/station &6pour charger un wagonnet (Âge 2 minimum). &e/station fret recuperer &6récupère les livraisons de la gare.");
         }

         return true;
      }

      RailManager rails = this.plugin.getRailManager();
      Station origin = rails.nearestStation(player.getLocation(), RADIUS);
      if (origin == null) {
         ChatUtil.sendMessage(player, "&c🚉 Rendez-vous dans une gare (à moins de " + (int)RADIUS + " blocs d'un panneau &f[Gare]&c) pour voyager.");
         return true;
      }

      World world = player.getWorld();
      Location sign = new Location(world, origin.getSignX(), origin.getSignY(), origin.getSignZ());
      if (!Tag.SIGNS.isTagged(sign.getBlock().getType())) {
         rails.removeStation(origin);
         ChatUtil.sendMessage(player, "&cCette gare a perdu son panneau et a été supprimée du réseau.");
         return true;
      }

      RailManager.SearchStatus status = rails.search(player, origin, result -> {
         if (player.isOnline()) {
            RailGui.openTravel(this.plugin, player, result);
         }
      });
      switch (status) {
         case STARTED -> ChatUtil.sendActionbar(player, "&e🚆 Recherche des lignes en cours…");
         case COOLDOWN -> ChatUtil.sendMessage(player, "&7Patientez quelques secondes avant de relancer la recherche.");
         case BUSY -> ChatUtil.sendMessage(player, "&eLe réseau est très sollicité, réessayez dans un instant.");
         default -> {
         }
      }

      return true;
   }
}
