package fr.esicraft.chat;

import fr.esicraft.EsicraftPlugin;
import fr.esicraft.util.ChatUtil;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerCommandPreprocessEvent;
import org.bukkit.event.player.PlayerCommandSendEvent;

/**
 * Messages privés désactivés : {@code /msg} et ses équivalents ({@code /tell}, {@code /w}, {@code /r}…, y compris avec un préfixe
 * {@code minecraft:} ou {@code essentials:}) sont refusés et retirés de l'auto-complétion. Liste réglable : {@code chat.blocked-commands}.
 */
public class PrivateMessageBlocker implements Listener {
   private static final List<String> DEFAULTS = List.of("msg", "tell", "w", "whisper", "m", "t", "pm", "dm", "r", "reply", "emsg", "etell", "ewhisper", "er", "ereply", "teammsg", "tm");
   private final EsicraftPlugin plugin;

   public PrivateMessageBlocker(EsicraftPlugin plugin) {
      this.plugin = plugin;
   }

   private Set<String> blocked() {
      List<String> list = this.plugin.getConfig().isList("chat.blocked-commands") ? this.plugin.getConfig().getStringList("chat.blocked-commands") : DEFAULTS;
      Set<String> out = new HashSet<>();
      for (String s : list) {
         out.add(s.toLowerCase(Locale.ROOT).replace("/", "").trim());
      }

      return out;
   }

   /** Nom de commande sans « / » ni préfixe d'espace de noms (minecraft:msg → msg). */
   private static String base(String label) {
      String l = label.toLowerCase(Locale.ROOT);
      int colon = l.indexOf(':');
      return colon >= 0 ? l.substring(colon + 1) : l;
   }

   @EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true)
   public void onCommand(PlayerCommandPreprocessEvent event) {
      String msg = event.getMessage();
      if (msg.length() < 2) {
         return;
      }

      int space = msg.indexOf(' ');
      String label = msg.substring(1, space < 0 ? msg.length() : space);
      if (this.blocked().contains(base(label))) {
         event.setCancelled(true);
         ChatUtil.sendMessage(event.getPlayer(), "&c❌ Les messages privés sont désactivés sur ce serveur : parlez dans le chat public ou celui de votre guilde.");
      }
   }

   @EventHandler
   public void onTabList(PlayerCommandSendEvent event) {
      Set<String> blocked = this.blocked();
      event.getCommands().removeIf(c -> blocked.contains(base(c)));
   }
}
