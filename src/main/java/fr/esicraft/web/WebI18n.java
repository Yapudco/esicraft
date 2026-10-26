package fr.esicraft.web;

import fr.esicraft.util.Lang;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Version anglaise du site : les pages sont rendues en français puis traduites avec le dictionnaire de {@code /english}
 * ({@code lang/en.tsv}). Seuls les textes affichés et quelques attributs (title, aria-label, alt, placeholder, content) sont
 * traduits ; styles, scripts et adresses restent intacts. Les liens internes gardent {@code ?lang=en}.
 */
final class WebI18n {
   /** Marqueur remplacé dans la barre de navigation par le bouton de langue. */
   static final String MARKER = "<!--LANG-->";

   private static final Pattern TOKEN = Pattern.compile(
      "(?s)<style>.*?</style>|<script[^>]*>.*?</script>|>([^<>]+)<|\\b(title|aria-label|placeholder|alt|content)=\"([^\"]*)\"|href=\"(/[^\"]*)\"");

   private WebI18n() {
   }

   static String french(String html) {
      return html.replace(MARKER, "<a class=\"nl lang\" data-lang=\"en\" href=\"?lang=en\" hreflang=\"en\" title=\"English\">EN</a>");
   }

   /**
    * Noms de guildes et de joueurs (échappés en HTML), jamais traduits même s'ils contiennent un mot du dictionnaire.
    * Indexés par première lettre : la recherche dans un texte coûte quelques comparaisons, pas un parcours de tous les noms.
    */
   static final class NameIndex {
      private final java.util.Map<Character, String[]> byFirst;

      private NameIndex(java.util.Map<Character, String[]> byFirst) {
         this.byFirst = byFirst;
      }

      static NameIndex of(java.util.Collection<String> names) {
         java.util.Map<Character, java.util.List<String>> tmp = new java.util.HashMap<>();
         for (String n : new java.util.HashSet<>(names)) {
            if (n != null && n.length() >= 3) {
               tmp.computeIfAbsent(n.charAt(0), k -> new java.util.ArrayList<>()).add(n);
            }
         }

         java.util.Map<Character, String[]> out = new java.util.HashMap<>();
         for (var e : tmp.entrySet()) {
            e.getValue().sort((x, y) -> Integer.compare(y.length(), x.length()));
            out.put(e.getKey(), e.getValue().toArray(new String[0]));
         }

         return new NameIndex(out);
      }
   }

   static String english(String html, NameIndex names) {
      String page = html.replace(MARKER, "<a class=\"nl lang\" data-lang=\"fr\" href=\"?lang=fr\" hreflang=\"fr\" title=\"Français\">FR</a>").replace("<html lang=\"fr\">", "<html lang=\"en\">");
      Matcher m = TOKEN.matcher(page);
      StringBuilder out = new StringBuilder(page.length() + 256);
      while (m.find()) {
         String rep;
         if (m.group(1) != null) {
            rep = ">" + translate(m.group(1), names) + "<";
         } else if (m.group(3) != null) {
            rep = m.group(2) + "=\"" + translate(m.group(3), names) + "\"";
         } else if (m.group(4) != null) {
            rep = "href=\"" + link(m.group(4)) + "\"";
         } else {
            rep = m.group(0);
         }

         m.appendReplacement(out, Matcher.quoteReplacement(rep));
      }

      m.appendTail(out);
      return out.toString();
   }

   private static String translate(String text, NameIndex names) {
      if (text.isBlank()) {
         return text;
      }

      StringBuilder out = null;
      int last = 0;
      int i = 0;
      int n = text.length();
      while (i < n) {
         String[] candidates = names.byFirst.get(text.charAt(i));
         String hit = null;
         if (candidates != null) {
            for (String c : candidates) {
               if (text.startsWith(c, i)) {
                  hit = c;
                  break;
               }
            }
         }

         if (hit == null) {
            i++;
            continue;
         }

         if (out == null) {
            out = new StringBuilder(n + 16);
         }

         out.append(translateSegment(text.substring(last, i))).append(hit);
         i += hit.length();
         last = i;
      }

      return out == null ? translateSegment(text) : out.append(translateSegment(text.substring(last))).toString();
   }

   private static String translateSegment(String text) {
      if (text.isEmpty() || text.isBlank()) {
         return text;
      }

      String raw = text.replace("&#39;", "'");
      String translated = date(Lang.toEnglish(raw));
      return translated.equals(raw) ? text : translated;
   }

   private static final Pattern DATE = Pattern.compile("\\b(lundi|mardi|mercredi|jeudi|vendredi|samedi|dimanche) (\\d{1,2}) (janvier|février|mars|avril|mai|juin|juillet|août|septembre|octobre|novembre|décembre)\\b");
   private static final String[] DAYS = {"Monday", "Tuesday", "Wednesday", "Thursday", "Friday", "Saturday", "Sunday"};
   private static final String[] FR_DAYS = {"lundi", "mardi", "mercredi", "jeudi", "vendredi", "samedi", "dimanche"};
   private static final String[] MONTHS = {"January", "February", "March", "April", "May", "June", "July", "August", "September", "October", "November", "December"};
   private static final String[] FR_MONTHS = {"janvier", "février", "mars", "avril", "mai", "juin", "juillet", "août", "septembre", "octobre", "novembre", "décembre"};

   /** « samedi 25 octobre » → « Saturday, October 25 » (les noms de jours et de mois ne passent pas par le dictionnaire : « mai » est dans « mais »). */
   private static String date(String text) {
      Matcher m = DATE.matcher(text);
      StringBuilder out = new StringBuilder();
      while (m.find()) {
         String day = m.group(1);
         String month = m.group(3);
         for (int i = 0; i < FR_DAYS.length; i++) {
            if (FR_DAYS[i].equals(day)) {
               day = DAYS[i];
            }
         }

         for (int i = 0; i < FR_MONTHS.length; i++) {
            if (FR_MONTHS[i].equals(month)) {
               month = MONTHS[i];
            }
         }

         m.appendReplacement(out, Matcher.quoteReplacement(day + ", " + month + " " + m.group(2)));
      }

      m.appendTail(out);
      return out.toString();
   }

   private static String link(String href) {
      if (href.startsWith("/assets") || href.startsWith("/api") || href.contains("lang=")) {
         return href;
      }

      int hash = href.indexOf('#');
      String base = hash >= 0 ? href.substring(0, hash) : href;
      String frag = hash >= 0 ? href.substring(hash) : "";
      return base + (base.contains("?") ? "&amp;" : "?") + "lang=en" + frag;
   }
}
