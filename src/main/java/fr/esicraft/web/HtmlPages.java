package fr.esicraft.web;

import java.util.List;
import java.util.Locale;

/** Génération HTML / JSON à partir d'un instantané. Tout texte venant du jeu est échappé. */
final class HtmlPages {
   private static final String CSS = ":root{--bg:#12141a;--card:#1b1f29;--line:#2a3040;--tx:#e8eaf0;--mut:#8b93a7;--acc:#f5b942}"
      + "@media(prefers-color-scheme:light){:root{--bg:#f3f4f7;--card:#fff;--line:#dde1ea;--tx:#1b1f29;--mut:#5d6679;--acc:#b9770e}}"
      + "*{box-sizing:border-box}body{margin:0;background:var(--bg);color:var(--tx);font:16px/1.5 system-ui,Segoe UI,sans-serif}"
      + "header{border-bottom:1px solid var(--line);background:var(--card)}"
      + ".w{max-width:1000px;margin:0 auto;padding:0 16px}"
      + "nav{display:flex;gap:18px;align-items:center;padding:14px 0;flex-wrap:wrap}nav b{color:var(--acc);font-size:1.2rem;margin-right:auto}"
      + "a{color:var(--acc);text-decoration:none}a:hover{text-decoration:underline}"
      + "h1{font-size:1.6rem;margin:24px 0 8px}h2{font-size:1.15rem;margin:28px 0 10px}"
      + ".grid{display:grid;grid-template-columns:repeat(auto-fit,minmax(170px,1fr));gap:12px;margin:16px 0}"
      + ".card{background:var(--card);border:1px solid var(--line);border-radius:10px;padding:14px}"
      + ".card .n{font-size:1.7rem;font-weight:700}.card .l{color:var(--mut);font-size:.85rem}"
      + "table{width:100%;border-collapse:collapse;background:var(--card);border:1px solid var(--line);border-radius:10px;overflow:hidden}"
      + "th,td{padding:9px 12px;text-align:left;border-bottom:1px solid var(--line)}th{color:var(--mut);font-weight:600;font-size:.8rem;text-transform:uppercase}"
      + "tr:last-child td{border-bottom:0}.r{text-align:right}.mut{color:var(--mut)}.tag{background:var(--line);border-radius:5px;padding:1px 7px;font-size:.8rem}"
      + ".on{color:#4ade80}.scroll{overflow-x:auto}footer{color:var(--mut);font-size:.8rem;padding:30px 0}"
      + "ul.h{list-style:none;padding:0;margin:0}ul.h li{padding:7px 0;border-bottom:1px solid var(--line)}";

   private HtmlPages() {
   }

   static String esc(String s) {
      if (s == null) {
         return "";
      }

      StringBuilder sb = new StringBuilder(s.length() + 8);
      for (int i = 0; i < s.length(); i++) {
         char c = s.charAt(i);
         switch (c) {
            case '&' -> sb.append("&amp;");
            case '<' -> sb.append("&lt;");
            case '>' -> sb.append("&gt;");
            case '"' -> sb.append("&quot;");
            case '\'' -> sb.append("&#39;");
            default -> sb.append(c);
         }
      }

      return sb.toString();
   }

   static String plain(String s) {
      return s == null ? "" : s.replaceAll("[&§][0-9a-fk-orA-FK-OR]", "");
   }

   private static String money(double v) {
      return String.format(Locale.FRANCE, "%,.0f", v).replace(' ', ' ').replace(' ', ' ');
   }

   static String page(String serverName, String title, String body, WebSnapshot snap) {
      String time = snap.generatedAt() == 0L ? "-" : java.time.format.DateTimeFormatter.ofPattern("dd/MM HH:mm:ss")
         .withZone(java.time.ZoneId.of("Europe/Paris")).format(java.time.Instant.ofEpochMilli(snap.generatedAt()));
      return "<!doctype html><html lang=\"fr\"><head><meta charset=\"utf-8\"><meta name=\"viewport\" content=\"width=device-width,initial-scale=1\">"
         + "<title>" + esc(title) + " · " + esc(serverName) + "</title><style>" + CSS + "</style></head><body>"
         + "<header><div class=\"w\"><nav><b>" + esc(serverName) + "</b><a href=\"/\">Accueil</a><a href=\"/guildes\">Guildes</a><a href=\"/joueurs\">Joueurs</a>"
         + "<a href=\"/api/stats.json\">API</a></nav></div></header><main class=\"w\">" + body
         + "<footer>Données mises à jour à " + esc(time) + " · lecture seule</footer></main></body></html>";
   }

   private static String stat(String n, String label) {
      return "<div class=\"card\"><div class=\"n\">" + esc(n) + "</div><div class=\"l\">" + esc(label) + "</div></div>";
   }

   static String home(String serverName, WebSnapshot s) {
      StringBuilder b = new StringBuilder("<h1>" + esc(serverName) + "</h1><div class=\"grid\">");
      b.append(stat(s.onlineCount() + " / " + s.maxPlayers(), "Joueurs en ligne"));
      b.append(stat(String.valueOf(s.guilds().size()), "Guildes"));
      b.append(stat(s.season(), "Saison (" + s.daysLeft() + " j restants)"));
      if (s.event() != null) {
         b.append(stat(plain(s.event()), "Événement en cours"));
      }

      b.append("</div><h2>🏆 Top guildes</h2>").append(guildTable(s.guilds().stream().limit(5).toList(), false));
      b.append("<h2>🟢 En ligne</h2>").append(onlineTable(s));
      b.append("<h2>📜 Histoire récente</h2>");
      if (s.history().isEmpty()) {
         b.append("<p class=\"mut\">Rien à signaler pour le moment.</p>");
      } else {
         b.append("<ul class=\"h\">");
         for (String line : s.history()) {
            b.append("<li>").append(esc(plain(line))).append("</li>");
         }

         b.append("</ul>");
      }

      return b.toString();
   }

   static String guildTable(List<WebSnapshot.GuildView> guilds, boolean full) {
      if (guilds.isEmpty()) {
         return "<p class=\"mut\">Aucune guilde pour le moment.</p>";
      }

      StringBuilder b = new StringBuilder("<div class=\"scroll\"><table><tr><th>#</th><th>Guilde</th><th>Âge</th><th class=\"r\">Membres</th><th class=\"r\">Terres</th>");
      if (full) {
         b.append("<th>Spécialisation</th>");
      }

      b.append("<th class=\"r\">Score</th></tr>");
      int rank = 1;
      for (WebSnapshot.GuildView g : guilds) {
         b.append("<tr><td>").append(rank++).append("</td><td><a href=\"/guilde/").append(esc(g.id())).append("\">").append(esc(g.name()))
            .append("</a> <span class=\"tag\">").append(esc(g.tag())).append("</span>").append(g.champion() ? " 👑" : "").append("</td><td>")
            .append(esc(g.age())).append("</td><td class=\"r\">").append(g.members().size()).append("</td><td class=\"r\">").append(g.claims()).append("</td>");
         if (full) {
            b.append("<td>").append(esc(g.specialization())).append("</td>");
         }

         b.append("<td class=\"r\">").append(g.score()).append("</td></tr>");
      }

      return b.append("</table></div>").toString();
   }

   static String onlineTable(WebSnapshot s) {
      if (s.online().isEmpty()) {
         return "<p class=\"mut\">Personne en ligne.</p>";
      }

      boolean coins = s.online().get(0).coins() != null;
      StringBuilder b = new StringBuilder("<div class=\"scroll\"><table><tr><th>Joueur</th><th>Race</th><th>Guilde</th><th>Âge</th>" + (coins ? "<th class=\"r\">Pièces</th>" : "") + "</tr>");
      for (WebSnapshot.OnlineView p : s.online()) {
         b.append("<tr><td class=\"on\">● ").append(esc(p.name())).append("</td><td>").append(esc(p.race())).append("</td><td>");
         if (p.guildId() != null) {
            b.append("<a href=\"/guilde/").append(esc(p.guildId())).append("\">").append(esc(p.guildName())).append("</a>");
         } else {
            b.append("<span class=\"mut\">aucune</span>");
         }

         b.append("</td><td>").append(esc(p.age())).append("</td>");
         if (coins) {
            b.append("<td class=\"r\">").append(money(p.coins())).append("</td>");
         }

         b.append("</tr>");
      }

      return b.append("</table></div>").toString();
   }

   static String guilds(WebSnapshot s) {
      return "<h1>Guildes</h1>" + guildTable(s.guilds(), true);
   }

   static String players(WebSnapshot s) {
      StringBuilder b = new StringBuilder("<h1>Joueurs</h1><h2>🟢 En ligne</h2>").append(onlineTable(s));
      if (!s.rich().isEmpty()) {
         b.append("<h2>💰 Les plus riches</h2><div class=\"scroll\"><table><tr><th>#</th><th>Joueur</th><th class=\"r\">Pièces</th></tr>");
         int rank = 1;
         for (WebSnapshot.RichView r : s.rich()) {
            b.append("<tr><td>").append(rank++).append("</td><td>").append(esc(r.name())).append("</td><td class=\"r\">").append(money(r.coins())).append("</td></tr>");
         }

         b.append("</table></div>");
      }

      return b.toString();
   }

   static String guild(WebSnapshot.GuildView g) {
      StringBuilder b = new StringBuilder("<h1>" + esc(g.name()) + " <span class=\"tag\">" + esc(g.tag()) + "</span>" + (g.champion() ? " 👑" : "") + "</h1>");
      b.append("<p class=\"mut\">").append(esc(g.specialization())).append(" · ").append(esc(g.age())).append("</p><div class=\"grid\">");
      b.append(stat(String.valueOf(g.score()), "Score de saison"));
      b.append(stat(String.valueOf(g.members().size()), "Membres"));
      b.append(stat(String.valueOf(g.claims()), "Chunks claim"));
      if (g.bank() != null) {
         b.append(stat(money(g.bank()), "Banque"));
      }

      b.append(stat(String.valueOf(g.repelled()), "Raids repoussés"));
      b.append(stat(String.valueOf(g.conquests()), "Conquêtes"));
      if (g.nexusPercent() != null) {
         b.append(stat(g.nexusPercent() + " %", "Nexus"));
      }

      b.append("</div>");
      if (!g.treaties().isEmpty()) {
         b.append("<h2>📜 Traités</h2><ul class=\"h\">");
         for (String t : g.treaties()) {
            b.append("<li>").append(esc(t)).append("</li>");
         }

         b.append("</ul>");
      }

      b.append("<h2>Membres</h2><div class=\"scroll\"><table><tr><th>Joueur</th><th>Rôle</th><th>Race</th></tr>");
      for (WebSnapshot.MemberView m : g.members()) {
         b.append("<tr><td>").append(m.online() ? "<span class=\"on\">●</span> " : "<span class=\"mut\">○</span> ").append(esc(m.name())).append("</td><td>")
            .append(esc(m.role())).append("</td><td>").append(m.race() != null ? esc(m.race()) : "<span class=\"mut\">-</span>").append("</td></tr>");
      }

      return b.append("</table></div>").toString();
   }

   static String notFound() {
      return "<h1>Page introuvable</h1><p><a href=\"/\">Retour à l'accueil</a></p>";
   }

   // ------------------------------------------------------------------ JSON

   static String json(String s) {
      if (s == null) {
         return "null";
      }

      StringBuilder sb = new StringBuilder("\"");
      for (int i = 0; i < s.length(); i++) {
         char c = s.charAt(i);
         if (c == '"' || c == '\\') {
            sb.append('\\').append(c);
         } else if (c < 0x20) {
            sb.append(String.format("\\u%04x", (int)c));
         } else {
            sb.append(c);
         }
      }

      return sb.append('"').toString();
   }

   static String statsJson(WebSnapshot s) {
      StringBuilder b = new StringBuilder("{");
      b.append("\"generatedAt\":").append(s.generatedAt()).append(",\"season\":").append(json(s.season())).append(",\"daysLeft\":").append(s.daysLeft());
      b.append(",\"event\":").append(json(s.event() == null ? null : plain(s.event())));
      b.append(",\"players\":{\"online\":").append(s.onlineCount()).append(",\"max\":").append(s.maxPlayers()).append(",\"list\":[");
      for (int i = 0; i < s.online().size(); i++) {
         WebSnapshot.OnlineView p = s.online().get(i);
         b.append(i > 0 ? "," : "").append("{\"name\":").append(json(p.name())).append(",\"race\":").append(json(p.race()))
            .append(",\"guild\":").append(json(p.guildName())).append(",\"age\":").append(json(p.age())).append("}");
      }

      b.append("]},\"guilds\":[");
      for (int i = 0; i < s.guilds().size(); i++) {
         WebSnapshot.GuildView g = s.guilds().get(i);
         b.append(i > 0 ? "," : "").append("{\"id\":").append(json(g.id())).append(",\"name\":").append(json(g.name())).append(",\"tag\":").append(json(g.tag()))
            .append(",\"specialization\":").append(json(g.specialization())).append(",\"age\":").append(json(g.age())).append(",\"members\":").append(g.members().size())
            .append(",\"claims\":").append(g.claims()).append(",\"score\":").append(g.score()).append(",\"repelled\":").append(g.repelled())
            .append(",\"conquests\":").append(g.conquests()).append(",\"champion\":").append(g.champion());
         if (g.bank() != null) {
            b.append(",\"bank\":").append(Math.round(g.bank()));
         }

         b.append("}");
      }

      return b.append("]}").toString();
   }
}
