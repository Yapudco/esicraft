package fr.esicraft.util;

import fr.esicraft.EsicraftPlugin;
import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

/**
 * Langue par joueur (/english). Le jeu est écrit en français : chaque texte envoyé à un joueur anglophone
 * passe par un dictionnaire de fragments FR → EN ({@code lang/en.tsv}), le plus long fragment gagnant.
 * Les parties dynamiques (noms, nombres) ne sont pas dans le dictionnaire et restent intactes.
 */
public final class Lang {
   private static final String KEY_PREFIX = "lang_en:";
   private static final Set<UUID> ENGLISH = ConcurrentHashMap.newKeySet();
   /** Fragments français regroupés par première lettre, du plus long au plus court. */
   private static final Map<Character, List<String[]>> INDEX = new HashMap<>();
   private static int entries;
   private static EsicraftPlugin plugin;

   private Lang() {
   }

   public static void init(EsicraftPlugin pl) {
      plugin = pl;
      loadDictionary();
      for (Object[] row : pl.getDataManager().getStorage().select("SELECT k FROM kv WHERE k LIKE '" + KEY_PREFIX + "%';", 1)) {
         try {
            ENGLISH.add(UUID.fromString(((String)row[0]).substring(KEY_PREFIX.length())));
         } catch (IllegalArgumentException ignored) {
         }
      }
   }

   private static void loadDictionary() {
      INDEX.clear();
      entries = 0;
      try (InputStream in = Lang.class.getResourceAsStream("/lang/en.tsv")) {
         if (in == null) {
            return;
         }

         BufferedReader reader = new BufferedReader(new InputStreamReader(in, StandardCharsets.UTF_8));
         String line;
         while ((line = reader.readLine()) != null) {
            int tab = line.indexOf('\t');
            if (tab > 0 && !line.startsWith("#")) {
               String fr = unescape(line.substring(0, tab));
               String en = unescape(line.substring(tab + 1));
               if (!fr.isEmpty()) {
                  INDEX.computeIfAbsent(fr.charAt(0), c -> new ArrayList<>()).add(new String[]{fr, en});
                  entries++;
               }
            }
         }
      } catch (Exception e) {
         if (plugin != null) {
            plugin.getLogger().warning("Dictionnaire anglais illisible : " + e.getMessage());
         }
      }

      for (List<String[]> list : INDEX.values()) {
         list.sort(Comparator.comparingInt((String[] e) -> e[0].length()).reversed());
      }
   }

   private static String unescape(String s) {
      return s.indexOf('\\') < 0 ? s : s.replace("\\n", "\n").replace("\\t", "\t").replace("\\\\", "\\");
   }

   public static int dictionarySize() {
      return entries;
   }

   public static boolean isEnglish(UUID uuid) {
      return uuid != null && ENGLISH.contains(uuid);
   }

   public static boolean isEnglish(CommandSender sender) {
      return sender instanceof Player p && ENGLISH.contains(p.getUniqueId());
   }

   public static void setEnglish(Player player, boolean english) {
      UUID uuid = player.getUniqueId();
      boolean changed = english ? ENGLISH.add(uuid) : ENGLISH.remove(uuid);
      if (changed && plugin != null) {
         Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
            if (english) {
               plugin.getDataManager().getStorage().exec("INSERT INTO kv (k, v) VALUES (?, '1') ON CONFLICT(k) DO NOTHING;", KEY_PREFIX + uuid);
            } else {
               plugin.getDataManager().getStorage().exec("DELETE FROM kv WHERE k = ?;", KEY_PREFIX + uuid);
            }
         });
      }
   }

   /** Traduit un texte (codes couleur & conservés) en anglais, quel que soit le destinataire. */
   public static String toEnglish(String text) {
      if (text == null || text.isEmpty() || INDEX.isEmpty()) {
         return text;
      }

      // Les mêmes textes reviennent sans cesse (tableau de bord toutes les 2 s, menus) : mémoire des traductions déjà faites.
      String cached = CACHE.get(text);
      if (cached != null) {
         return cached;
      }

      String translated = translate(text);
      if (text.length() <= 400) {
         if (CACHE.size() > 4000) {
            CACHE.clear();
         }

         CACHE.put(text, translated);
      }

      return translated;
   }

   private static final Map<String, String> CACHE = new ConcurrentHashMap<>();

   private static String translate(String text) {

      StringBuilder out = null;
      int last = 0;
      int i = 0;
      int n = text.length();
      while (i < n) {
         List<String[]> candidates = INDEX.get(text.charAt(i));
         String[] match = null;
         if (candidates != null) {
            for (String[] entry : candidates) {
               if (text.startsWith(entry[0], i)) {
                  match = entry;
                  break;
               }
            }
         }

         if (match == null) {
            i++;
         } else {
            if (out == null) {
               out = new StringBuilder(n + 16);
            }

            out.append(text, last, i).append(match[1]);
            i += match[0].length();
            last = i;
         }
      }

      if (out == null) {
         return text;
      }

      return out.append(text, last, n).toString();
   }

   /** Texte à destination de {@code viewer} : traduit seulement s'il est anglophone. */
   public static String tr(CommandSender viewer, String text) {
      return isEnglish(viewer) ? toEnglish(text) : text;
   }

   public static Component tr(CommandSender viewer, Component component) {
      if (component == null || !isEnglish(viewer)) {
         return component;
      }

      return ChatUtil.component(toEnglish(ChatUtil.toLegacy(component)));
   }
}
