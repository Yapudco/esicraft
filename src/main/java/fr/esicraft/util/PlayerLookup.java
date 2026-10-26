package fr.esicraft.util;

import fr.esicraft.EsicraftPlugin;
import java.util.Locale;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

/**
 * Recherche d'un joueur connecté par son nom saisi dans une commande : nom exact, puis début du nom <b>réel</b>.
 * Contrairement à {@link Bukkit#getPlayer(String)}, un joueur déguisé n'est jamais trouvé par son faux nom (le nom du profil change avec le skin).
 */
public final class PlayerLookup {
   private PlayerLookup() {
   }

   public static Player find(String name) {
      if (name == null || name.isEmpty()) {
         return null;
      }

      Player exact = Bukkit.getPlayerExact(name);
      if (exact != null) {
         return exact;
      }

      EsicraftPlugin plugin = EsicraftPlugin.getInstance();
      String lower = name.toLowerCase(Locale.ROOT);
      Player best = null;
      for (Player p : Bukkit.getOnlinePlayers()) {
         String real = plugin != null && plugin.getDisguiseManager() != null ? plugin.getDisguiseManager().realName(p) : p.getName();
         if (real.toLowerCase(Locale.ROOT).startsWith(lower) && (best == null || real.length() < plugin.getDisguiseManager().realName(best).length())) {
            best = p;
         }
      }

      return best;
   }
}
