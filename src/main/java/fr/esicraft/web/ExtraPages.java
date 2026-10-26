package fr.esicraft.web;

import static fr.esicraft.web.HtmlPages.avatar;
import static fr.esicraft.web.HtmlPages.playerAvatar;
import static fr.esicraft.web.HtmlPages.empty;
import static fr.esicraft.web.HtmlPages.esc;
import static fr.esicraft.web.HtmlPages.hue;
import static fr.esicraft.web.HtmlPages.json;
import static fr.esicraft.web.HtmlPages.money;
import static fr.esicraft.web.HtmlPages.stat;

import fr.esicraft.life.Chronicle;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.function.ToDoubleFunction;

/**
 * Pages ajoutées au site : journal de guerre, raids en direct, réseau diplomatique, musée des trophées et succès, courbes
 * d'évolution, frise des Âges, profils de joueurs et guide en ligne. Graphiques en SVG généré côté serveur (aucune bibliothèque).
 */
final class ExtraPages {
   private static final ZoneId ZONE = ZoneId.of("Europe/Paris");
   private static final DateTimeFormatter DAY = DateTimeFormatter.ofPattern("dd/MM").withZone(ZONE);
   private static final DateTimeFormatter DAY_TIME = DateTimeFormatter.ofPattern("dd/MM HH:mm").withZone(ZONE);
   private static final DateTimeFormatter TIME = DateTimeFormatter.ofPattern("HH:mm").withZone(ZONE);
   private static final DateTimeFormatter FULL_DAY = DateTimeFormatter.ofPattern("EEEE d MMMM", Locale.FRENCH).withZone(ZONE);

   /** Libellés des catégories du journal de guerre (ordre d'affichage des filtres). */
   static final Map<String, String> KINDS = new LinkedHashMap<>();

   static {
      KINDS.put("RAID", "⚔ Raids");
      KINDS.put("DEFENSE", "🛡 Défenses");
      KINDS.put("CONQUEST", "💥 Conquêtes");
      KINDS.put("TREATY", "📜 Traités");
      KINDS.put("BETRAYAL", "💔 Trahisons");
      KINDS.put("AGE", "⏳ Âges");
      KINDS.put("EVENT", "🎉 Événements");
      KINDS.put("HELP", "🆘 Renforts");
      KINDS.put("CONTRACT", "🧩 Contrats");
      KINDS.put("ACHIEVEMENT", "🏅 Succès");
      KINDS.put("GUILD", "🏰 Guildes");
      KINDS.put("NATURE", "🍂 Saisons");
      KINDS.put("SEASON", "👑 Classement");
   }

   private static final Map<String, String> STAT_LABELS = new LinkedHashMap<>();

   static {
      STAT_LABELS.put("monsters", "Monstres éliminés");
      STAT_LABELS.put("pvp_kills", "Joueurs vaincus");
      STAT_LABELS.put("pvp_deaths", "Défaites en duel");
      STAT_LABELS.put("raids_launched", "Hordes lancées");
      STAT_LABELS.put("raids_repelled", "Hordes repoussées");
      STAT_LABELS.put("contracts", "Contrats accomplis");
      STAT_LABELS.put("trips", "Voyages en train");
      STAT_LABELS.put("helps", "Renforts envoyés");
   }

   private ExtraPages() {
   }

   // ------------------------------------------------------------------ utilitaires

   static String color(int h) {
      return "hsl(" + h + " 72% 58%)";
   }

   static String profileLink(String name) {
      return "/joueur/" + esc(name.toLowerCase(Locale.ROOT));
   }

   private static String ago(long ts, long now) {
      long min = Math.max(0L, (now - ts) / 60000L);
      if (min < 1) {
         return "à l'instant";
      } else if (min < 60) {
         return "il y a " + min + " min";
      } else if (min < 1440) {
         return "il y a " + min / 60 + " h";
      }

      return "le " + DAY.format(Instant.ofEpochMilli(ts));
   }

   private static String duration(long ms) {
      long s = Math.max(0L, ms / 1000L);
      return s >= 60 ? s / 60 + " min " + String.format("%02d", s % 60) + " s" : s + " s";
   }

   private static String hero(String title, String text) {
      return "<section class=\"hero\"><h1>" + esc(title) + "</h1><p>" + esc(text) + "</p></section>";
   }

   private static String guildName(WebSnapshot s, String id) {
      WebSnapshot.GuildView g = id != null ? s.guild(id) : null;
      return g != null ? g.name() : null;
   }

   // ------------------------------------------------------------------ graphiques SVG

   /** Série de points (horodatage, valeur) pour un graphique en courbes. */
   record Series(String name, int hue, List<double[]> points) {
   }

   static String lineChart(List<Series> series, String unit) {
      double minT = Double.MAX_VALUE;
      double maxT = -Double.MAX_VALUE;
      double maxV = 1.0;
      int count = 0;
      for (Series se : series) {
         for (double[] p : se.points()) {
            minT = Math.min(minT, p[0]);
            maxT = Math.max(maxT, p[0]);
            maxV = Math.max(maxV, p[1]);
            count++;
         }
      }

      if (count < 2) {
         return empty("Pas encore assez de données : un point est enregistré chaque heure.");
      }

      if (maxT <= minT) {
         maxT = minT + 3600000.0;
      }

      double niceMax = niceCeil(maxV);
      int w = 640;
      int h = 230;
      int left = 52;
      int right = 14;
      int top = 14;
      int bottom = 26;
      double pw = w - left - right;
      double ph = h - top - bottom;
      StringBuilder b = new StringBuilder("<svg class=\"chart\" viewBox=\"0 0 " + w + " " + h + "\" role=\"img\" aria-label=\"Graphique\">");
      for (int i = 0; i <= 4; i++) {
         double y = top + ph - ph * i / 4.0;
         b.append("<line class=\"gl\" x1=\"").append(left).append("\" x2=\"").append(w - right).append("\" y1=\"").append(fmt(y)).append("\" y2=\"").append(fmt(y)).append("\"/>");
         b.append("<text x=\"").append(left - 8).append("\" y=\"").append(fmt(y + 4)).append("\" text-anchor=\"end\">").append(esc(shortNumber(niceMax * i / 4.0))).append("</text>");
      }

      b.append("<text x=\"").append(left).append("\" y=\"").append(h - 6).append("\">").append(DAY_TIME.format(Instant.ofEpochMilli((long)minT))).append("</text>");
      b.append("<text x=\"").append(w - right).append("\" y=\"").append(h - 6).append("\" text-anchor=\"end\">").append(DAY_TIME.format(Instant.ofEpochMilli((long)maxT)))
         .append("</text>");
      for (Series se : series) {
         if (se.points().isEmpty()) {
            continue;
         }

         StringBuilder path = new StringBuilder();
         double lx = 0;
         double ly = 0;
         for (double[] p : se.points()) {
            lx = left + pw * (p[0] - minT) / (maxT - minT);
            ly = top + ph - ph * Math.max(0.0, p[1]) / niceMax;
            path.append(path.length() == 0 ? "M" : "L").append(fmt(lx)).append(' ').append(fmt(ly));
         }

         String c = color(se.hue());
         b.append("<path d=\"").append(path).append("\" fill=\"none\" stroke=\"").append(c).append("\" stroke-width=\"2.5\" stroke-linejoin=\"round\" stroke-linecap=\"round\"/>");
         b.append("<circle cx=\"").append(fmt(lx)).append("\" cy=\"").append(fmt(ly)).append("\" r=\"4\" fill=\"").append(c).append("\"><title>").append(esc(se.name()))
            .append(" : ").append(esc(shortNumber(se.points().get(se.points().size() - 1)[1]))).append(unit.isEmpty() ? "" : " " + esc(unit)).append("</title></circle>");
      }

      b.append("</svg>");
      if (series.size() > 1) {
         b.append("<div class=\"legend\">");
         for (Series se : series) {
            b.append("<span class=\"chip\"><i class=\"sw\" style=\"background:").append(color(se.hue())).append("\"></i>").append(esc(se.name())).append("</span>");
         }

         b.append("</div>");
      }

      return b.toString();
   }

   private static double niceCeil(double v) {
      double exp = Math.pow(10, Math.floor(Math.log10(Math.max(1.0, v))));
      for (double m : new double[]{1, 2, 2.5, 5, 10}) {
         if (m * exp >= v) {
            return m * exp;
         }
      }

      return 10 * exp;
   }

   static String shortNumber(double v) {
      if (v >= 1_000_000) {
         return String.format(Locale.FRANCE, "%.1f M", v / 1_000_000);
      } else if (v >= 10_000) {
         return String.format(Locale.FRANCE, "%.0f k", v / 1000);
      } else if (v >= 1000) {
         return String.format(Locale.FRANCE, "%.1f k", v / 1000);
      }

      return String.format(Locale.FRANCE, "%.0f", v);
   }

   private static String fmt(double v) {
      return String.format(Locale.ROOT, "%.1f", v);
   }

   private static List<double[]> points(List<Chronicle.Point> list, ToDoubleFunction<Chronicle.Point> f) {
      List<double[]> out = new ArrayList<>(list.size());
      for (Chronicle.Point p : list) {
         out.add(new double[]{p.ts(), f.applyAsDouble(p)});
      }

      return out;
   }

   // ------------------------------------------------------------------ accueil (blocs ajoutés)

   static String homeBanner(WebExtra x) {
      StringBuilder b = new StringBuilder();
      if (!x.raids().isEmpty()) {
         b.append("<a class=\"row alert\" href=\"/raids\"><span class=\"rank\">⚔</span><div class=\"grow\"><div class=\"t\">")
            .append(x.raids().size()).append(x.raids().size() > 1 ? " raids en cours" : " raid en cours").append("</div><div class=\"s mut\">");
         List<String> targets = new ArrayList<>();
         for (WebExtra.RaidView r : x.raids()) {
            targets.add(r.targetName());
         }

         b.append(esc(String.join(", ", targets))).append(" sous les assauts d'une horde</div></div><span class=\"chip bad\">Suivre en direct →</span></a>");
      }

      return b.toString();
   }

   static String natureCard(WebExtra x) {
      WebExtra.NatureView n = x.nature();
      if (n == null) {
         return "";
      }

      StringBuilder b = new StringBuilder("<div class=\"card nature\"><div class=\"gh\"><span class=\"big\">").append(esc(n.icon())).append("</span><div><b>")
         .append(esc(n.label())).append("</b> <span class=\"mut\">· encore ").append(n.daysLeft()).append(n.daysLeft() > 1 ? " jours" : " jour").append(" · puis ")
         .append(esc(n.next())).append("</span><div class=\"mut\">");
      b.append(esc(String.join(" ", n.effects()))).append("</div></div></div></div>");
      return b.toString();
   }

   static String journalList(WebSnapshot s, List<WebExtra.JournalView> entries, long now, boolean grouped) {
      if (entries.isEmpty()) {
         return empty("Rien à signaler pour le moment.");
      }

      StringBuilder b = new StringBuilder("<div class=\"tl\">");
      String lastDay = null;
      for (WebExtra.JournalView e : entries) {
         if (grouped) {
            String day = FULL_DAY.format(Instant.ofEpochMilli(e.ts()));
            if (!day.equals(lastDay)) {
               b.append("<div class=\"day\" data-day>").append(esc(day)).append("</div>");
               lastDay = day;
            }
         }

         b.append("<div class=\"ev\" data-k=\"").append(esc(e.kind())).append("\"><time>").append(esc(grouped ? TIME.format(Instant.ofEpochMilli(e.ts())) : ago(e.ts(), now)))
            .append("</time>").append(esc(e.icon())).append(' ').append(esc(e.text()));
         String a = guildName(s, e.guildA());
         String c = guildName(s, e.guildB());
         if (a != null || c != null) {
            b.append("<div class=\"chips\">");
            if (a != null) {
               b.append("<a class=\"chip\" href=\"/guilde/").append(esc(e.guildA())).append("\">").append(esc(a)).append("</a>");
            }

            if (c != null) {
               b.append("<a class=\"chip\" href=\"/guilde/").append(esc(e.guildB())).append("\">").append(esc(c)).append("</a>");
            }

            b.append("</div>");
         }

         b.append("</div>");
      }

      return b.append("</div>").toString();
   }

   // ------------------------------------------------------------------ journal de guerre

   static String journal(WebSnapshot s, WebExtra x) {
      StringBuilder b = new StringBuilder(hero("📰 Journal de guerre", "Tout ce qui fait l'histoire du serveur : raids, conquêtes, traités, trahisons, Âges, événements."));
      Set<String> present = new HashSet<>();
      for (WebExtra.JournalView e : x.journal()) {
         present.add(e.kind());
      }

      b.append("<div class=\"filters\" role=\"group\" aria-label=\"Filtrer\"><button class=\"chip fb on\" data-f=\"\">Tout</button>");
      for (Map.Entry<String, String> k : KINDS.entrySet()) {
         if (present.contains(k.getKey())) {
            b.append("<button class=\"chip fb\" data-f=\"").append(k.getKey()).append("\">").append(esc(k.getValue())).append("</button>");
         }
      }

      b.append("</div>").append(journalList(s, x.journal(), s.generatedAt(), true));
      return b.toString();
   }

   // ------------------------------------------------------------------ raids en direct

   static String raids(WebSnapshot s, WebExtra x) {
      StringBuilder b = new StringBuilder("<section class=\"hero\"><span class=\"live\"><span class=\"dot\"").append(x.raids().isEmpty() ? "" : " style=\"background:#f87171\"")
         .append("></span>").append(x.raids().isEmpty() ? "Aucun raid" : x.raids().size() + (x.raids().size() > 1 ? " raids en cours" : " raid en cours"))
         .append("</span><h1>⚔ Raids en direct</h1><p>Les hordes qui assiègent un Nexus en ce moment. La page se met à jour toute seule ; aucune position n'est publiée.</p></section>");
      if (x.raids().isEmpty()) {
         b.append(empty("Aucune horde n'attaque pour le moment. Les derniers raids sont dans le journal de guerre."));
      }

      for (WebExtra.RaidView r : x.raids()) {
         int pct = r.mobsTotal() > 0 ? (int)Math.round(100.0 * (r.mobsTotal() - r.mobsLeft()) / r.mobsTotal()) : 0;
         b.append("<div class=\"card raid\"><div class=\"gh\">").append(avatar(r.targetName(), r.targetId(), "", true)).append("<div class=\"grow\"><div class=\"t\">")
            .append("<a href=\"/guilde/").append(esc(r.targetId())).append("\">").append(esc(r.targetName())).append("</a> assiégée</div><div class=\"chips\">")
            .append("<span class=\"chip bad\">").append(esc(r.type())).append("</span><span class=\"chip\">par ").append(esc(r.attackerName())).append("</span>")
            .append("<span class=\"chip\">⏱ ").append(esc(duration(s.generatedAt() - r.startedAt()))).append("</span>")
            .append(r.defenders() >= 0 ? "<span class=\"chip " + (r.defenders() > 0 ? "ok" : "bad") + "\">🛡 " + r.defenders() + (r.defenders() > 1 ? " défenseurs" : " défenseur") + " sur place</span>" : "");
         if (r.nexusPercent() != null) {
            b.append("<span class=\"chip acc\">💎 Nexus ").append(r.nexusPercent()).append(" %</span>");
         }

         b.append("</div></div></div><div class=\"mut\" style=\"margin-top:12px\">Monstres éliminés : <b>").append(r.mobsTotal() - r.mobsLeft()).append(" / ").append(r.mobsTotal())
            .append("</b></div><div class=\"bar\"><i style=\"width:").append(Math.max(2, pct)).append("%\"></i></div></div>");
      }

      List<WebExtra.JournalView> recent = new ArrayList<>();
      for (WebExtra.JournalView e : x.journal()) {
         if (e.kind().equals("RAID") || e.kind().equals("DEFENSE") || e.kind().equals("CONQUEST") || e.kind().equals("HELP")) {
            recent.add(e);
         }

         if (recent.size() >= 10) {
            break;
         }
      }

      b.append("<h2>📜 Derniers combats</h2>").append(journalList(s, recent, s.generatedAt(), false));
      return b.toString();
   }

   // ------------------------------------------------------------------ réseau diplomatique

   static String diplomacy(WebSnapshot s, WebExtra x) {
      StringBuilder b = new StringBuilder(hero("🕸 Réseau diplomatique", "Alliances, pactes, vassalités et rivalités entre les guildes. Les rivalités comptent les raids lancés et les joueurs tués."));
      List<WebSnapshot.GuildView> nodes = s.guilds().stream().limit(14).toList();
      if (nodes.isEmpty()) {
         return b.append(empty("Aucune guilde pour le moment.")).toString();
      }

      Map<String, double[]> pos = new HashMap<>();
      double cx = 220;
      double cy = 210;
      double radius = nodes.size() == 1 ? 0 : 150;
      for (int i = 0; i < nodes.size(); i++) {
         double angle = -Math.PI / 2 + 2 * Math.PI * i / nodes.size();
         pos.put(nodes.get(i).id(), new double[]{cx + radius * Math.cos(angle), cy + radius * Math.sin(angle)});
      }

      long topScore = Math.max(1L, nodes.get(0).score());
      java.util.Map<String, Double> radii = new HashMap<>();
      for (WebSnapshot.GuildView g : nodes) {
         radii.put(g.id(), 15 + 11.0 * g.score() / topScore);
      }

      StringBuilder svg = new StringBuilder("<svg class=\"net\" viewBox=\"0 0 440 430\" role=\"img\" aria-label=\"Réseau des guildes\">"
         + "<defs><marker id=\"arrv\" viewBox=\"0 0 10 10\" refX=\"9\" refY=\"5\" markerWidth=\"6\" markerHeight=\"6\" orient=\"auto-start-reverse\"><path d=\"M0 0L10 5L0 10z\" fill=\"#c084fc\"/></marker></defs>"
         + "<g id=\"netg\">");
      int rivalries = 0;
      int treaties = 0;
      for (WebExtra.EdgeView e : x.edges()) {
         double[] a = pos.get(e.a());
         double[] c = pos.get(e.b());
         if (a == null || c == null) {
            continue;
         }

         String style = switch (e.kind()) {
            case "ALLIANCE" -> "stroke=\"#34d399\" stroke-width=\"3.5\"";
            case "PACT" -> "stroke=\"#60a5fa\" stroke-width=\"2.5\" stroke-dasharray=\"7 5\"";
            case "VASSALAGE" -> "stroke=\"#c084fc\" stroke-width=\"3.5\" stroke-dasharray=\"2 5\" stroke-linecap=\"round\"";
            default -> "stroke=\"#f87171\" stroke-opacity=\".75\" stroke-width=\"" + fmt(1.5 + Math.min(7.0, e.weight() / 3.0)) + "\"";
         };
         if (e.kind().equals("RIVALRY")) {
            rivalries++;
         } else {
            treaties++;
         }

         String marker = "";
         double x1 = a[0];
         double y1 = a[1];
         double x2 = c[0];
         double y2 = c[1];
         if (e.kind().equals("VASSALAGE") && !e.suzerain().isEmpty()) {
            // Flèche du vassal vers son suzerain (allégeance), arrêtée au bord du cercle du suzerain.
            boolean aIsSuzerain = e.suzerain().equals(e.a());
            double[] from = aIsSuzerain ? c : a;
            double[] to = aIsSuzerain ? a : c;
            double rTo = radii.getOrDefault(aIsSuzerain ? e.a() : e.b(), 18.0) + 4;
            double dx = to[0] - from[0];
            double dy = to[1] - from[1];
            double len = Math.max(1.0, Math.hypot(dx, dy));
            x1 = from[0];
            y1 = from[1];
            x2 = to[0] - dx / len * rTo;
            y2 = to[1] - dy / len * rTo;
            marker = " marker-end=\"url(#arrv)\"";
         }

         svg.append("<line class=\"edge\" data-k=\"").append(e.kind()).append("\" data-a=\"").append(esc(e.a())).append("\" data-b=\"").append(esc(e.b()))
            .append("\" x1=\"").append(fmt(x1)).append("\" y1=\"").append(fmt(y1)).append("\" x2=\"").append(fmt(x2)).append("\" y2=\"").append(fmt(y2))
            .append("\" ").append(style).append(marker).append("><title>").append(esc(edgeLabel(s, e))).append("</title></line>");
      }

      for (WebSnapshot.GuildView g : nodes) {
         double[] p = pos.get(g.id());
         double r = 15 + 11.0 * g.score() / topScore;
         svg.append("<a class=\"node\" data-id=\"").append(esc(g.id())).append("\" href=\"/guilde/").append(esc(g.id())).append("\"><circle cx=\"").append(fmt(p[0])).append("\" cy=\"").append(fmt(p[1])).append("\" r=\"").append(fmt(r))
            .append("\" fill=\"").append(color(hue(g.id()))).append("\" stroke=\"var(--bg)\" stroke-width=\"3\"><title>").append(esc(g.name())).append("</title></circle>")
            .append("<text x=\"").append(fmt(p[0])).append("\" y=\"").append(fmt(p[1] + 4)).append("\" text-anchor=\"middle\" class=\"tag\">").append(esc(g.tag()))
            .append("</text><text x=\"").append(fmt(p[0])).append("\" y=\"").append(fmt(p[1] + r + 14)).append("\" text-anchor=\"middle\">").append(esc(g.name()))
            .append("</text></a>");
      }

      svg.append("</g></svg>");
      StringBuilder focus = new StringBuilder("<select id=\"netfocus\" class=\"nsel\" aria-label=\"Guilde à mettre en avant\"><option value=\"\">Toutes les guildes</option>");
      for (WebSnapshot.GuildView g : nodes) {
         focus.append("<option value=\"").append(esc(g.id())).append("\">").append(esc(g.name())).append("</option>");
      }

      focus.append("</select>");
      b.append("<div class=\"card\"><div class=\"nettools\" data-js hidden>").append(focus)
         .append("<span class=\"zb\"><button type=\"button\" class=\"chip\" data-z=\"out\" aria-label=\"Dézoomer\">−</button>")
         .append("<button type=\"button\" class=\"chip\" data-z=\"in\" aria-label=\"Zoomer\">+</button>")
         .append("<button type=\"button\" class=\"chip\" data-z=\"reset\">Recentrer</button></span></div>")
         .append(svg).append("<div class=\"legend\" style=\"justify-content:center\">")
         .append("<button type=\"button\" class=\"chip nk on\" data-nk=\"ALLIANCE\"><i class=\"sw\" style=\"background:#34d399\"></i>Alliance</button>")
         .append("<button type=\"button\" class=\"chip nk on\" data-nk=\"PACT\"><i class=\"sw\" style=\"background:#60a5fa\"></i>Pacte de non-agression</button>")
         .append("<button type=\"button\" class=\"chip nk on\" data-nk=\"VASSALAGE\"><i class=\"sw\" style=\"background:#c084fc\"></i>Vassalité (flèche : vassal → suzerain)</button>")
         .append("<button type=\"button\" class=\"chip nk on\" data-nk=\"RIVALRY\"><i class=\"sw\" style=\"background:#f87171\"></i>Rivalité (épaisseur = intensité)</button></div>")
         .append("<p class=\"mut\" style=\"text-align:center;font-size:.8rem;margin:8px 0 0\">Molette ou boutons pour zoomer, glisser pour déplacer. Cliquez un type pour le masquer ou l'afficher.</p></div>");
      if (treaties == 0 && rivalries == 0) {
         b.append("<p class=\"mut\" style=\"text-align:center\">Aucun traité ni rivalité pour le moment : le réseau se dessinera au fil des alliances et des raids.</p>");
      }

      // Classement des rivalités + carte de chaleur.
      List<WebExtra.EdgeView> rivals = new ArrayList<>();
      for (WebExtra.EdgeView e : x.edges()) {
         if (e.kind().equals("RIVALRY")) {
            rivals.add(e);
         }
      }

      rivals.sort(Comparator.comparingInt(WebExtra.EdgeView::weight).reversed());
      b.append("<div class=\"two\"><div><h2>🔥 Rivalités</h2>");
      if (rivals.isEmpty()) {
         b.append(empty("Aucun raid ni duel entre guildes pour l'instant."));
      } else {
         b.append("<div class=\"list\">");
         for (WebExtra.EdgeView e : rivals.stream().limit(10).toList()) {
            b.append("<div class=\"row\"><span class=\"rank\">⚔</span><div class=\"grow\"><div class=\"t\">").append(esc(guildName(s, e.a()))).append(" <span class=\"mut\">contre</span> ")
               .append(esc(guildName(s, e.b()))).append("</div><div class=\"chips\"><span class=\"chip bad\">").append(e.raids()).append(e.raids() > 1 ? " raids" : " raid")
               .append("</span><span class=\"chip\">").append(e.kills()).append(e.kills() > 1 ? " joueurs tués" : " joueur tué").append("</span></div></div></div>");
         }

         b.append("</div>");
      }

      b.append("</div><div><h2>📜 Traités en vigueur</h2>");
      List<WebExtra.EdgeView> pacts = new ArrayList<>();
      for (WebExtra.EdgeView e : x.edges()) {
         if (!e.kind().equals("RIVALRY") && guildName(s, e.a()) != null && guildName(s, e.b()) != null) {
            pacts.add(e);
         }
      }

      if (pacts.isEmpty()) {
         b.append(empty("Aucun traité signé."));
      } else {
         b.append("<div class=\"list\">");
         for (WebExtra.EdgeView e : pacts) {
            String icon = switch (e.kind()) {
               case "ALLIANCE" -> "🤝";
               case "PACT" -> "📜";
               default -> "👑";
            };
            b.append("<div class=\"row\"><span class=\"rank\">").append(icon).append("</span><div class=\"grow\"><div class=\"t\">").append(esc(guildName(s, e.a())))
               .append(" &amp; ").append(esc(guildName(s, e.b()))).append("</div><div class=\"s mut\">").append(esc(kindLabel(e.kind()))).append("</div></div></div>");
         }

         b.append("</div>");
      }

      b.append("</div></div>");
      b.append(heatmap(s, rivals));
      return b.toString();
   }

   private static String kindLabel(String kind) {
      return switch (kind) {
         case "ALLIANCE" -> "Alliance";
         case "PACT" -> "Pacte de non-agression";
         case "VASSALAGE" -> "Vassalité";
         default -> "Rivalité";
      };
   }

   private static String edgeLabel(WebSnapshot s, WebExtra.EdgeView e) {
      String base = guildName(s, e.a()) + " – " + guildName(s, e.b()) + " : " + kindLabel(e.kind());
      return e.kind().equals("RIVALRY") ? base + " (" + e.raids() + " raids, " + e.kills() + " joueurs tués)" : base;
   }

   private static String heatmap(WebSnapshot s, List<WebExtra.EdgeView> rivals) {
      if (rivals.isEmpty()) {
         return "";
      }

      List<String> ids = new ArrayList<>();
      for (WebExtra.EdgeView e : rivals) {
         for (String id : new String[]{e.a(), e.b()}) {
            if (!ids.contains(id) && ids.size() < 10) {
               ids.add(id);
            }
         }
      }

      Map<String, WebExtra.EdgeView> byPair = new HashMap<>();
      int max = 1;
      for (WebExtra.EdgeView e : rivals) {
         byPair.put(e.a() + "|" + e.b(), e);
         byPair.put(e.b() + "|" + e.a(), e);
         max = Math.max(max, e.weight());
      }

      StringBuilder b = new StringBuilder("<h2>🌡 Carte des rivalités <small>raids · joueurs tués</small></h2><div class=\"card scroll\"><table class=\"heat\"><tr><th></th>");
      for (String id : ids) {
         WebSnapshot.GuildView g = s.guild(id);
         b.append("<th>").append(esc(g != null ? g.tag() : "?")).append("</th>");
      }

      b.append("</tr>");
      for (String row : ids) {
         WebSnapshot.GuildView g = s.guild(row);
         b.append("<tr><th>").append(esc(g != null ? g.tag() : "?")).append("</th>");
         for (String col : ids) {
            WebExtra.EdgeView e = row.equals(col) ? null : byPair.get(row + "|" + col);
            if (e == null) {
               b.append("<td class=\"mut\">").append(row.equals(col) ? "—" : "·").append("</td>");
            } else {
               double a = 0.15 + 0.75 * e.weight() / max;
               b.append("<td style=\"background:rgba(248,113,113,").append(fmt(a)).append(")\" title=\"").append(esc(edgeLabel(s, e))).append("\">").append(e.raids())
                  .append(" · ").append(e.kills()).append("</td>");
            }
         }

         b.append("</tr>");
      }

      return b.append("</table></div>").toString();
   }

   // ------------------------------------------------------------------ musée des trophées et succès

   static String museum(WebSnapshot s, WebExtra x) {
      StringBuilder b = new StringBuilder(hero("🏛 Musée des trophées", "Les exploits des guildes et les succès des joueurs. Un succès obtenu par moins de 10 % des joueurs est rare."));
      b.append("<h2>🏆 Galerie des guildes</h2>");
      boolean any = false;
      StringBuilder gal = new StringBuilder("<div class=\"people\">");
      for (WebSnapshot.GuildView g : s.guilds()) {
         List<WebExtra.TrophyView> t = x.trophies().getOrDefault(g.id(), List.of());
         if (t.isEmpty()) {
            continue;
         }

         any = true;
         gal.append("<a class=\"card trophy\" href=\"/guilde/").append(esc(g.id())).append("\"><div class=\"gh\">").append(avatar(g.tag(), g.id(), "sm", true))
            .append("<b>").append(esc(g.name())).append("</b></div><div class=\"chips\" style=\"margin-top:10px\">");
         for (WebExtra.TrophyView tv : t) {
            gal.append("<span class=\"chip acc\" title=\"").append(esc(tv.detail())).append("\">").append(esc(tv.icon())).append(' ').append(esc(tv.title())).append("</span>");
         }

         gal.append("</div></a>");
      }

      gal.append("</div>");
      b.append(any ? gal : empty("Aucun trophée pour l'instant : Âges avancés, conquêtes, hordes repoussées et titres de champion apparaîtront ici."));
      List<WebExtra.AchievementView> rare = x.achievements().stream().filter(a -> a.rare() && a.holders() > 0).toList();
      if (!rare.isEmpty()) {
         b.append("<h2>💜 Succès rares obtenus</h2><div class=\"achs\">");
         for (WebExtra.AchievementView a : rare) {
            b.append(achievementCard(a, true));
         }

         b.append("</div>");
      }

      b.append("<div class=\"two\"><div><h2>🏅 Tous les succès <small>").append(x.achievements().size()).append("</small></h2><div class=\"achs\">");
      List<WebExtra.AchievementView> sorted = new ArrayList<>(x.achievements());
      sorted.sort(Comparator.comparingDouble(WebExtra.AchievementView::percent));
      for (WebExtra.AchievementView a : sorted) {
         b.append(achievementCard(a, a.holders() > 0));
      }

      b.append("</div></div><div><h2>✨ Derniers débloqués</h2>");
      if (x.recentUnlocks().isEmpty()) {
         b.append(empty("Personne n'a encore débloqué de succès."));
      } else {
         b.append("<div class=\"tl\">");
         for (WebExtra.UnlockView u : x.recentUnlocks()) {
            b.append("<div class=\"ev\"><time>").append(esc(ago(u.ts(), s.generatedAt()))).append("</time><a href=\"").append(profileLink(u.player())).append("\"><b>")
               .append(esc(u.player())).append("</b></a> débloque ").append(esc(u.icon())).append(' ').append(esc(u.title()))
               .append(u.rare() ? " <span class=\"chip rare\">rare</span>" : "").append("</div>");
         }

         b.append("</div>");
      }

      return b.append("</div></div>").toString();
   }

   private static String achievementCard(WebExtra.AchievementView a, boolean unlocked) {
      return "<div class=\"ach" + (unlocked ? "" : " lock") + "\"><span class=\"ic\">" + esc(a.icon()) + "</span><div class=\"grow\"><div class=\"t\">" + esc(a.title())
         + (a.rare() && a.holders() > 0 ? " <span class=\"chip rare\">rare</span>" : "") + "</div><div class=\"s mut\">" + esc(a.description()) + "</div><div class=\"bar\"><i style=\"width:"
         + Math.max(2, (int)Math.round(a.percent())) + "%\"></i></div><div class=\"s mut\">" + a.holders() + (a.holders() > 1 ? " joueurs" : " joueur") + " · "
         + String.format(Locale.FRANCE, "%.0f", a.percent()) + " %</div></div></div>";
   }

   // ------------------------------------------------------------------ courbes d'évolution

   static String evolution(WebSnapshot s, WebExtra x) {
      StringBuilder b = new StringBuilder(hero("📈 Courbes d'évolution", "Le score de saison des meilleures guildes, relevé chaque heure (90 jours d'historique)."));
      List<Series> score = new ArrayList<>();
      List<Series> claims = new ArrayList<>();
      for (WebSnapshot.GuildView g : s.guilds().stream().limit(8).toList()) {
         List<Chronicle.Point> pts = x.curves().getOrDefault(g.id(), List.of());
         score.add(new Series(g.name(), hue(g.id()), points(pts, p -> p.score())));
         claims.add(new Series(g.name(), hue(g.id()), points(pts, p -> p.claims())));
      }

      b.append("<h2>🏆 Score de saison</h2><div class=\"card\">").append(lineChart(score, "points")).append("</div>");
      b.append("<h2>🗺 Territoire <small>chunks revendiqués</small></h2><div class=\"card\">").append(lineChart(claims, "chunks")).append("</div>");
      return b.toString();
   }

   static String guildCurves(WebSnapshot.GuildView g, WebExtra x) {
      List<Chronicle.Point> pts = x.curves().getOrDefault(g.id(), List.of());
      int h = hue(g.id());
      StringBuilder b = new StringBuilder("<h2>📈 Évolution</h2><div class=\"duo\"><div class=\"card\"><b>Score de saison</b>")
         .append(lineChart(List.of(new Series("Score", h, points(pts, p -> p.score()))), "points")).append("</div>");
      if (g.bank() != null) {
         b.append("<div class=\"card\"><b>Banque</b>").append(lineChart(List.of(new Series("Banque", (h + 50) % 360, points(pts, p -> p.bank()))), "pièces")).append("</div>");
      } else {
         b.append("<div class=\"card\"><b>Membres</b>").append(lineChart(List.of(new Series("Membres", (h + 50) % 360, points(pts, p -> p.members()))), "membres")).append("</div>");
      }

      return b.append("</div>").toString();
   }

   // ------------------------------------------------------------------ frise des Âges

   static String ages(WebSnapshot s, WebExtra x) {
      StringBuilder b = new StringBuilder(hero("⏳ Frise des Âges", "La course aux Âges sur un vrai axe de temps : à quel moment chaque guilde a franchi chaque Âge, et ce qu'il lui manque pour le suivant."));
      return b.append(agesTimeline(s, x, null)).toString();
   }

   private static final String[] AGE_SHORT = {"Rustique", "Métallurgie", "Alchimie", "Guerre Totale", "Mythique", "Suprématie"};
   private static final long[] TICK_STEPS = {3600000L, 7200000L, 21600000L, 43200000L, 86400000L, 172800000L, 604800000L, 1209600000L, 2592000000L, 7776000000L};

   /**
    * Frise des Âges : courbe en escalier par guilde sur un axe de temps proportionnel (abscisse = date, ordonnée = Âge).
    * Les guildes se masquent ou s'affichent une à une (script), et chaque guilde a un menu déroulant listant les
    * prérequis du prochain Âge, déposés ou non.
    */
   static String agesTimeline(WebSnapshot s, WebExtra x, String onlyGuild) {
      Map<String, java.util.TreeMap<Integer, Long>> byGuild = new LinkedHashMap<>();
      for (WebSnapshot.GuildView g : s.guilds()) {
         if (onlyGuild == null || g.id().equals(onlyGuild)) {
            byGuild.put(g.id(), new java.util.TreeMap<>());
         }
      }

      long min = Long.MAX_VALUE;
      for (Chronicle.AgeMark m : x.ages()) {
         java.util.TreeMap<Integer, Long> reached = byGuild.get(m.guildId());
         if (reached != null && m.age() >= 1 && m.age() <= 6) {
            reached.merge(m.age(), m.ts(), Math::min);
            min = Math.min(min, m.ts());
         }
      }

      byGuild.values().removeIf(Map::isEmpty);
      if (byGuild.isEmpty()) {
         return empty("Aucun passage d'Âge enregistré pour le moment.");
      }

      long now = Math.max(s.generatedAt(), min + 3600000L);
      List<String> ids = new ArrayList<>(byGuild.keySet());
      ids.sort((a, c) -> {
         int byAge = Integer.compare(s.guild(c).ageLevel(), s.guild(a).ageLevel());
         return byAge != 0 ? byAge : s.guild(a).name().compareToIgnoreCase(s.guild(c).name());
      });
      if (ids.size() > 30) {
         ids = ids.subList(0, 30);
      }

      int w = 840;
      int left = 112;
      int right = 28;
      int top = 20;
      int rowH = 46;
      int bottom = 44;
      int h = top + 5 * rowH + 14 + bottom;
      double pw = w - left - right - 24;
      double x0 = left + 12;
      long span = now - min;
      long step = TICK_STEPS[TICK_STEPS.length - 1];
      for (long candidate : TICK_STEPS) {
         if (span / candidate <= 7L) {
            step = candidate;
            break;
         }
      }

      StringBuilder svg = new StringBuilder("<div class=\"card scroll\"><svg class=\"chart frise\" viewBox=\"0 0 " + w + " " + h + "\" role=\"img\" aria-label=\"Frise des Âges\">");
      for (int a = 1; a <= 6; a++) {
         double y = top + (6 - a) * rowH + 14;
         svg.append("<line class=\"gl\" x1=\"").append(left).append("\" x2=\"").append(w - right).append("\" y1=\"").append(fmt(y)).append("\" y2=\"").append(fmt(y)).append("\"/>")
            .append("<text x=\"").append(left - 10).append("\" y=\"").append(fmt(y - 2)).append("\" text-anchor=\"end\" class=\"lbl\">Âge ").append(a).append("</text>")
            .append("<text x=\"").append(left - 10).append("\" y=\"").append(fmt(y + 11)).append("\" text-anchor=\"end\">").append(AGE_SHORT[a - 1]).append("</text>");
      }

      long offsetMs = ZONE.getRules().getOffset(Instant.ofEpochMilli(min)).getTotalSeconds() * 1000L;
      long firstTick = ((min + offsetMs + step - 1) / step) * step - offsetMs;
      DateTimeFormatter tickFmt = step < 86400000L ? DAY_TIME : DAY;
      double axisY = top + 5 * rowH + 14;
      for (long t = firstTick; t <= now; t += step) {
         double tx = x0 + pw * (t - min) / (double)(now - min);
         svg.append("<line class=\"gl\" x1=\"").append(fmt(tx)).append("\" x2=\"").append(fmt(tx)).append("\" y1=\"").append(top).append("\" y2=\"").append(fmt(axisY + 5)).append("\" stroke-dasharray=\"2 4\"/>")
            .append("<text x=\"").append(fmt(tx)).append("\" y=\"").append(fmt(axisY + 20)).append("\" text-anchor=\"middle\">").append(tickFmt.format(Instant.ofEpochMilli(t))).append("</text>");
      }

      double nowX = x0 + pw;
      svg.append("<line x1=\"").append(fmt(nowX)).append("\" x2=\"").append(fmt(nowX)).append("\" y1=\"").append(top).append("\" y2=\"").append(fmt(axisY + 5))
         .append("\" stroke=\"var(--acc)\" stroke-width=\"1.5\"/><text x=\"").append(fmt(nowX)).append("\" y=\"").append(fmt(axisY + 36)).append("\" text-anchor=\"end\" class=\"lbl\">maintenant</text>");

      int gi = 0;
      for (String id : ids) {
         WebSnapshot.GuildView g = s.guild(id);
         java.util.TreeMap<Integer, Long> reached = byGuild.get(id);
         String c = color(hue(id));
         double dy = ((gi++ % 7) - 3) * 1.6;
         List<Map.Entry<Integer, Long>> pts = new ArrayList<>(reached.entrySet());
         pts.sort(Map.Entry.comparingByValue());
         StringBuilder d = new StringBuilder();
         double lastY = 0;
         for (int i = 0; i < pts.size(); i++) {
            double px = x0 + pw * (pts.get(i).getValue() - min) / (double)(now - min);
            double py = top + (6 - pts.get(i).getKey()) * rowH + 14 + dy;
            d.append(i == 0 ? "M" : "H").append(fmt(px)).append(i == 0 ? " " : " V").append(fmt(py));
            lastY = py;
         }

         d.append(" H").append(fmt(nowX));
         svg.append("<g class=\"fg\" data-gid=\"").append(esc(id)).append("\"><path d=\"").append(d).append("\" fill=\"none\" stroke=\"").append(c)
            .append("\" stroke-width=\"2.5\" stroke-linejoin=\"round\" stroke-opacity=\".85\"/>");
         int top1 = reached.lastKey();
         for (Map.Entry<Integer, Long> e : pts) {
            double px = x0 + pw * (e.getValue() - min) / (double)(now - min);
            double py = top + (6 - e.getKey()) * rowH + 14 + dy;
            Long prev = reached.get(e.getKey() - 1);
            String delay = prev == null ? "" : " (" + fr.esicraft.treaty.TreatyManager.formatDuration(e.getValue() - prev) + " après l'Âge " + (e.getKey() - 1) + ")";
            svg.append("<circle cx=\"").append(fmt(px)).append("\" cy=\"").append(fmt(py)).append("\" r=\"").append(e.getKey() == top1 ? 7 : 5).append("\" fill=\"").append(c)
               .append("\" stroke=\"").append(e.getKey() == top1 ? "var(--tx)" : "var(--bg)").append("\" stroke-width=\"2\"><title>").append(esc(g.name())).append(" : Âge ").append(e.getKey())
               .append(" (").append(AGE_SHORT[e.getKey() - 1]).append(") le ").append(DAY_TIME.format(Instant.ofEpochMilli(e.getValue()))).append(esc(delay)).append("</title></circle>");
         }

         svg.append("</g>");
      }

      svg.append("</svg></div>");
      StringBuilder b = new StringBuilder();
      if (onlyGuild == null && ids.size() > 1) {
         b.append("<div class=\"legend\" style=\"margin:0 0 10px\" data-js hidden><button type=\"button\" class=\"chip\" data-all=\"hide\">Tout masquer</button><button type=\"button\" class=\"chip\" data-all=\"show\">Tout afficher</button>");
         for (String id : ids) {
            b.append("<button type=\"button\" class=\"chip gk on\" data-gid=\"").append(esc(id)).append("\"><i class=\"sw\" style=\"background:").append(color(hue(id))).append("\"></i>")
               .append(esc(s.guild(id).name())).append("</button>");
         }

         b.append("</div>");
      }

      b.append(svg).append("<p class=\"mut\" style=\"font-size:.8rem;margin:8px 0 0\">Abscisse : le temps réel (les écarts sont proportionnels). Ordonnée : l'Âge atteint. Chaque marche est un passage d'Âge ; le gros point est l'Âge actuel. Survolez un point pour sa date et le délai depuis l'Âge précédent.</p>");
      b.append("<h2>📋 Prérequis du prochain Âge</h2><div class=\"fdl\">");
      for (String id : ids) {
         WebSnapshot.GuildView g = s.guild(id);
         WebExtra.AgeProgress prog = x.ageProgress().get(id);
         java.util.TreeMap<Integer, Long> reached = byGuild.get(id);
         int pct = 100;
         if (prog != null && !prog.reqs().isEmpty()) {
            double sum = 0;
            for (WebExtra.ReqView r : prog.reqs()) {
               sum += r.required() <= 0 ? 1.0 : Math.min(1.0, r.donated() / (double)r.required());
            }

            pct = (int)Math.round(100.0 * sum / prog.reqs().size());
         }

         b.append("<details class=\"fd card\" data-gid=\"").append(esc(id)).append("\"><summary><i class=\"sw\" style=\"background:").append(color(hue(id))).append("\"></i><b>")
            .append(esc(g.name())).append("</b><span class=\"mut\"> Âge ").append(g.ageLevel()).append(" · ").append(AGE_SHORT[Math.max(0, Math.min(5, g.ageLevel() - 1))]).append("</span>");
         if (prog != null) {
            b.append("<span class=\"chip ").append(pct >= 100 ? "ok" : "acc").append("\">→ Âge ").append(prog.nextLevel()).append(" : ").append(pct).append(" %</span>");
         } else {
            b.append("<span class=\"chip ok\">👑 Âge maximal</span>");
         }

         b.append("</summary><div class=\"fdb\">");
         if (prog != null) {
            b.append("<div class=\"s mut\">Pour atteindre l'Âge ").append(prog.nextLevel()).append(" (").append(AGE_SHORT[prog.nextLevel() - 1]).append("), la guilde doit déposer (<b>/age</b>) :</div><div class=\"reqs\">");
            for (WebExtra.ReqView r : prog.reqs()) {
               boolean done = r.donated() >= r.required();
               int rp = r.required() <= 0 ? 100 : (int)Math.min(100L, Math.round(100.0 * r.donated() / r.required()));
               b.append("<div class=\"req ").append(done ? "ok" : "ko").append("\"><span class=\"ck\">").append(done ? "✔" : "✖").append("</span><span class=\"rn\">").append(esc(r.item()))
                  .append("</span><span class=\"rv\">").append(money(r.donated())).append(" / ").append(money(r.required())).append("</span><div class=\"bar\"><i style=\"width:").append(Math.max(2, rp)).append("%\"></i></div></div>");
            }

            b.append("</div><div class=\"s mut\" style=\"margin-top:6px\">Les quantités augmentent avec la taille de la guilde (").append(prog.members()).append(" membre(s) comptés).</div>");
         } else {
            b.append("<div class=\"s mut\">Cette guilde a atteint le dernier Âge.</div>");
         }

         b.append("<div class=\"chips\" style=\"margin-top:8px\">");
         for (Map.Entry<Integer, Long> e : reached.entrySet()) {
            b.append("<span class=\"chip\">Âge ").append(e.getKey()).append(" · ").append(DAY_TIME.format(Instant.ofEpochMilli(e.getValue()))).append("</span>");
         }

         b.append("</div></div></details>");
      }

      return b.append("</div>").toString();
   }

   // ------------------------------------------------------------------ fiche de guilde (blocs ajoutés)

   static String guildExtra(WebSnapshot s, WebSnapshot.GuildView g, WebExtra x) {
      StringBuilder b = new StringBuilder();
      int d = x.diversity().getOrDefault(g.id(), 0);
      b.append("<h2>🧩 Diversité des métiers <small>").append(d).append(" / 6</small></h2><div class=\"card\"><div class=\"bar\" style=\"margin:0 0 12px\"><i style=\"width:")
         .append(Math.max(3, d * 100 / 6)).append("%\"></i></div><div class=\"chips\">")
         .append(tier(d >= 3, "3 métiers : primes de monstres +10 %")).append(tier(d >= 4, "4 métiers : terres -10 %"))
         .append(tier(d >= 5, "5 métiers : pièges -15 %")).append(tier(d >= 6, "6 métiers : contrats de guilde ×1,5")).append("</div></div>");
      List<WebExtra.TrophyView> t = x.trophies().getOrDefault(g.id(), List.of());
      if (!t.isEmpty()) {
         b.append("<h2>🏆 Trophées</h2><div class=\"people\">");
         for (WebExtra.TrophyView tv : t) {
            b.append("<div class=\"ach\"><span class=\"ic\">").append(esc(tv.icon())).append("</span><div class=\"grow\"><div class=\"t\">").append(esc(tv.title()))
               .append("</div><div class=\"s mut\">").append(esc(tv.detail())).append("</div></div></div>");
         }

         b.append("</div>");
      }

      b.append(guildCurves(g, x));
      b.append("<h2>⏳ Âges franchis</h2>").append(agesTimeline(s, x, g.id()));
      List<WebExtra.JournalView> mine = new ArrayList<>();
      for (WebExtra.JournalView e : x.journal()) {
         if (g.id().equals(e.guildA()) || g.id().equals(e.guildB())) {
            mine.add(e);
         }

         if (mine.size() >= 12) {
            break;
         }
      }

      b.append("<h2>📰 Dans le journal</h2>").append(journalList(s, mine, s.generatedAt(), false));
      return b.toString();
   }

   private static String tier(boolean on, String label) {
      return "<span class=\"chip" + (on ? " ok" : "") + "\">" + (on ? "✔ " : "✖ ") + esc(label) + "</span>";
   }

   // ------------------------------------------------------------------ profils de joueurs

   static String directory(WebExtra x) {
      if (x.profiles().isEmpty()) {
         return "";
      }

      StringBuilder b = new StringBuilder("<h2>📇 Tous les joueurs <small>").append(x.profiles().size()).append("</small></h2><div class=\"people\">");
      for (WebExtra.ProfileView p : x.profiles()) {
         b.append("<a class=\"person row\" href=\"").append(profileLink(p.name())).append("\">").append(playerAvatar(p.name(), "sm")).append("<div class=\"grow\"><div class=\"t\">")
            .append(esc(p.name())).append("</div><div class=\"s\">").append(esc(p.race())).append(" · ").append(p.guildName() != null ? esc(p.guildName()) : "sans guilde")
            .append("</div></div>").append(p.reputation() > 0 ? "<span class=\"chip bad\">❤ " + p.reputation() + "</span>" : "").append("<span class=\"chip\">🏅 ").append(p.achievements().size()).append("</span></a>");
      }

      return b.append("</div>").toString();
   }

   static String profile(WebSnapshot s, WebExtra.ProfileView p, WebExtra x) {
      StringBuilder b = new StringBuilder("<section class=\"hero\"><div class=\"gh\">").append(playerAvatar(p.name(), "lg")).append("<div><h1>").append(esc(p.name()))
         .append("</h1><div class=\"chips\"><span class=\"chip acc\">").append(esc(p.race())).append("</span>");
      if (p.guildId() != null) {
         b.append("<a class=\"chip\" href=\"/guilde/").append(esc(p.guildId())).append("\">🏰 ").append(esc(p.guildName())).append(p.role() != null ? " · " + esc(p.role()) : "")
            .append("</a>");
      } else {
         b.append("<span class=\"chip\">Sans guilde</span>");
      }

      b.append("<span class=\"chip\">").append(esc(p.age())).append("</span>").append(p.reputation() > 0 ? "<span class=\"chip bad\">❤ " + p.reputation() + " honneur</span>" : "").append(p.online() ? "<span class=\"chip ok\">● en ligne</span>" : "").append("</div></div></div></section>");
      b.append("<div class=\"grid\" style=\"margin-top:16px\">").append(stat("🏅", p.achievements().size() + " / " + x.achievements().size(), "Succès")).append(stat("❤", String.valueOf(p.reputation()), "Réputation"));
      if (p.coins() != null) {
         b.append(stat("💰", money(p.coins()), "Pièces"));
      }

      for (Map.Entry<String, String> e : STAT_LABELS.entrySet()) {
         long v = p.stats().getOrDefault(e.getKey(), 0L);
         if (v > 0) {
            b.append(stat(icon(e.getKey()), money(v), e.getValue()));
         }
      }

      long seasons = Long.bitCount(p.stats().getOrDefault("seasons_seen", 0L));
      if (seasons > 0) {
         b.append(stat("🍂", seasons + " / 4", "Saisons vécues"));
      }

      b.append("</div><h2>🏅 Succès</h2><div class=\"achs\">");
      Set<String> owned = new HashSet<>(p.achievements());
      List<WebExtra.AchievementView> list = new ArrayList<>(x.achievements());
      list.sort(Comparator.comparing((WebExtra.AchievementView a) -> !owned.contains(a.id())).thenComparingDouble(WebExtra.AchievementView::percent));
      for (WebExtra.AchievementView a : list) {
         b.append(achievementCard(a, owned.contains(a.id())));
      }

      b.append("</div><p style=\"margin-top:26px\"><a class=\"nl\" href=\"/joueurs\">← Tous les joueurs</a></p>");
      return b.toString();
   }

   private static String icon(String key) {
      return switch (key) {
         case "monsters" -> "🧟";
         case "pvp_kills" -> "🗡";
         case "pvp_deaths" -> "💀";
         case "raids_launched" -> "⚔";
         case "raids_repelled" -> "🛡";
         case "contracts" -> "📋";
         case "trips" -> "🚆";
         case "helps" -> "🆘";
         default -> "•";
      };
   }

   // ------------------------------------------------------------------ guide en ligne

   private static String gs(String id, String title, String body) {
      return "<section class=\"card gs\" data-s id=\"" + id + "\"><h3>" + title + "</h3>" + body + "</section>";
   }

   static String guide(String serverName) {
      StringBuilder b = new StringBuilder("<section class=\"hero\"><h1>📖 Guide du joueur</h1><p>Toutes les règles d'" + esc(serverName)
         + ". Tapez un mot pour filtrer : « Nexus », « traité », « train »…</p></section>");
      b.append("<input id=\"q\" class=\"search\" type=\"search\" placeholder=\"Rechercher dans le guide…\" autocomplete=\"off\" aria-label=\"Rechercher dans le guide\">");
      b.append("<div id=\"none\" class=\"empty\" hidden>Aucune section ne correspond à cette recherche.</div><div class=\"guide\">");
      b.append(gs("demarrer", "🚀 Bien démarrer", "<ol><li><b>/race</b> : choisissez votre métier. Sans métier, impossible de casser un bloc.</li>"
         + "<li>Rejoignez une guilde (<b>/guild join &lt;nom&gt;</b>) ou fondez la vôtre : <b>/guild create &lt;nom&gt; &lt;tag&gt;</b> (500 pièces).</li>"
         + "<li>Revendiquez un terrain (<b>/guild claim</b>) puis posez le Nexus (<b>/guild nexus place</b>), dans l'heure qui suit la création.</li>"
         + "<li>Gagnez des pièces : monstres, contrats du jour (<b>/contrat</b>), Hôtel des Ventes (<b>/ah</b>).</li>"
         + "<li>Donnez des ressources dans <b>/age</b> pour faire progresser votre guilde.</li></ol><p><b>/guide</b> donne le livre en jeu, <b>/hud</b> le tableau de bord, <b>/english</b> passe le jeu en anglais.</p>"));
      b.append(gs("metiers", "🎭 Les métiers", "<ul><li><b>Nain</b> (mineur) : Hâte II et vision nocturne sous la couche 0, 15 % de minerais doublés ; lent en plein soleil, pas d'armes ni d'armures en métal.</li>"
         + "<li><b>Sylvain</b> (fermier) : régénération pour lui et sa guilde à 4 blocs, récolte d'un clic droit ; -20 % de dégâts au corps à corps.</li>"
         + "<li><b>Forgeron</b> : seul à forger armes, armures et enclumes au début ; résistance au feu, 35 % de lingots doublés ; a faim plus vite.</li>"
         + "<li><b>Rôdeur</b> (archer) : flèches empoisonnées, +25 % à distance, rapide en forêt ; lent avec un plastron lourd.</li>"
         + "<li><b>Arcaniste</b> (mage) : enchantements et potions, +30 % d'XP ; +15 % de dégâts subis au corps à corps.</li>"
         + "<li><b>Ingénieur</b> : redstone, pistons et TNT, -40 % de dégâts d'explosion ; -20 % au corps à corps.</li></ul>"
         + "<p>Les exclusivités disparaissent avec les Âges. Changer de métier : 3 fois gratuitement après le premier choix, puis 10 000 pièces, 24 h d'attente.</p>"));
      b.append(gs("guildes", "🏰 Guildes et territoire", "<p>10 joueurs maximum. <b>Chef</b> : tout ; <b>officiers</b> : invitations, terres, Nexus, pièges, raids, traités, gares ; <b>membres</b> : banque, dons, défense.</p>"
         + "<p>Les chunks revendiqués doivent se toucher. Prix : 100 pièces puis +25 par chunk. Maximum : 16 (Âge 1), 32, 64, 128, puis illimité à l'Âge 5.</p>"
         + "<p>Chez une autre guilde : impossible de casser, poser, ouvrir les coffres ; TNT, perles, pistons, lave et eau venues de l'extérieur sont bloqués. Pas de seau de lave à moins de 5 blocs d'un joueur ennemi.</p>"));
      b.append(gs("diversite", "🧩 Diversité des métiers", "<p>Plus une guilde réunit de métiers différents, plus elle est forte :</p><ul><li><b>3 métiers</b> : primes de monstres +10 %</li>"
         + "<li><b>4 métiers</b> : nouvelles terres 10 % moins chères</li><li><b>5 métiers</b> : pièges du Nexus 15 % moins chers</li>"
         + "<li><b>6 métiers</b> : récompenses des contrats de guilde ×1,5</li></ul><p>Votre niveau s'affiche dans <b>/guild info</b> et sur la fiche de guilde.</p>"));
      b.append(gs("specialisations", "🌟 Spécialisations", "<p>Choisies par le chef dans <b>/guild spec</b> : Légion Guerrière (combat), Industrie Souterraine (minage), Enclave Champêtre (nature), "
         + "Syndicat Marchand (commerce), Conclave Arcanique (magie), Rôdeur Solitaire (seul). Chacune donne des bonus et impose une composition de métiers. "
         + "Après le premier choix, on ne peut en changer qu'à l'arrivée d'un nouveau joueur.</p>"));
      b.append(gs("nexus", "💎 Le Nexus et ses pièges", "<p>Le cœur de la guilde : s'il est détruit, la guilde perd ses terres et sa banque. Pas d'eau, de lave ni d'obsidienne à moins de 16 blocs. "
         + "Une pomme dorée jetée dessus le soigne (+500 PV, +2 500 si enchantée, 30 s entre deux soins).</p><p>Pièges (<b>/nexus traps</b>, payés par la banque, consommés à la fin de la prochaine horde) : "
         + "Brasier (250), Toile (200), Foudre (350), Brume toxique (300), Golem gardien (500).</p>"));
      b.append(gs("raids", "⚔ Les raids", "<p>Dans <b>/ah</b>, le crâne de Wither : choisissez une horde (ou composez-la, 60 monstres max) et une cible ; la banque de guilde paie. "
         + "Officiers et chef seulement. <b>Un défenseur doit être connecté et sur ses terres.</b> La cible est ensuite protégée 30 minutes (2 minutes pour une guilde en hégémonie, trop riche).</p>"
         + "<p>Si le Nexus tombe : territoire et banque passent au vainqueur, les vaincus choisissent de le rejoindre ou de partir.</p>"));
      b.append(gs("aide", "🆘 Appel à l'aide", "<p>Pendant une attaque, tapez <b>/aide</b> : vos alliés (alliance ou vassalité) sont prévenus. Ceux qui viennent sur vos terres avant la fin de la horde "
         + "reçoivent <b>250 pièces</b> chacun si elle est repoussée (6 alliés au plus). Un appel toutes les 10 minutes.</p>"));
      b.append(gs("deguisement", "🎭 Déguisement", "<p><b>/deguisement &lt;guilde&gt;</b> (750 pièces, 5 minutes, une fois toutes les 30 minutes) : vous apparaissez sous le nom d'un membre de cette guilde, "
         + "avec son étiquette, et votre nom au-dessus de la tête est masqué. Ses pièges et son golem vous ignorent, mais ses protections de territoire restent actives. "
         + "Infliger des dégâts (ou frapper le Nexus) vous démasque, et la guilde est prévenue. <b>/deguisement off</b> pour l'enlever.</p>"));
      b.append(gs("ages", "⏳ Les Âges", "<p>Les membres donnent des ressources dans <b>/age</b>. 1 Rustique · 2 Métallurgie (Nether) · 3 Alchimie (End) · 4 Guerre Totale "
         + "(armures en diamant, villageois, TNT pour tous) · 5 Mythique (Netherite pour tous, terres illimitées) · 6 Suprématie (œuf de dragon et titre).</p>"
         + "<p>Les quantités augmentent avec le nombre maximal de membres atteint pendant l'Âge.</p>"));
      b.append(gs("argent", "💰 Argent et contrats", "<p>Primes de monstres (de 1,50 à 750 pièces), 3 contrats par jour (<b>/contrat</b>, +400 pièces pour les trois), Hôtel des Ventes "
         + "(<b>/ah sell &lt;prix&gt;</b>), événements. Tuer un joueur d'une autre guilde rapporte 100 pièces + 1 % de sa fortune.</p>"
         + "<p><b>Contrat de guilde</b> (dans <b>/contrat</b>) : chaque jour, trois objectifs réservés chacun à un métier (un Nain mine, un Forgeron fond…). "
         + "Seuls les membres du bon métier les font avancer : 200 pièces par objectif et 750 de plus pour les trois, versées à la banque de guilde.</p>"));
      b.append(gs("saisons", "🍂 Saisons naturelles", "<p>Le monde change de saison toutes les deux semaines (<b>/climat</b>) :</p><ul><li><b>🌸 Printemps</b> : cultures plus rapides (+50 %).</li>"
         + "<li><b>☀ Été</b> : +20 % d'expérience.</li><li><b>🍂 Automne</b> : une chance sur deux d'une récolte bonus.</li>"
         + "<li><b>❄ Hiver</b> : cultures deux fois plus lentes, primes de monstres +25 %.</li></ul><p>Pensez à faire des réserves avant l'hiver.</p>"));
      b.append(gs("diplomatie", "🤝 Diplomatie", "<p><b>/traite</b> : <b>alliance</b> (pas de raid, trains gratuits, pas de dégâts entre alliés), <b>pacte</b> de non-agression (7 jours), "
         + "<b>vassalité</b> (alliance + 5 % de la banque du vassal chaque jour au suzerain). Rompre un traité est une trahison : indemnité (500 pièces max) et 30 minutes sans pouvoir attaquer.</p>"));
      b.append(gs("train", "🚆 Le train", "<p>Près d'une gare (panneau <b>[Gare]</b>), <b>/station</b> téléporte vers une autre gare reliée par des rails continus. "
         + "Prix : 1 pièce pour la gare d'arrivée + les péages des guildes traversées. <b>/rail</b> règle le péage et les relations (normale, alliée, bannie). "
         + "Construire une gare : Âge 2, 500 pièces, 6 gares par guilde.</p>"));
      b.append(gs("vie", "🎉 Vie du serveur", "<p>Un événement tous les 1 à 2 jours : Lune de sang, pluie de météorites, marché noir, boss mondial. "
         + "Chaque mois, <b>/classement</b> désigne la guilde championne (5 000 pièces + 500 par membre). <b>/journal</b> résume les derniers grands faits, "
         + "<b>/succes</b> liste vos succès (les plus rares sont exposés au musée du site).</p>"));
      b.append(gs("commandes", "⌨ Commandes", "<table class=\"cmd\"><tr><td>/race</td><td>Métier</td></tr><tr><td>/guild</td><td>Guilde, terres, Nexus, banque</td></tr>"
         + "<tr><td>/age</td><td>Âges et dons</td></tr><tr><td>/ah</td><td>Hôtel des Ventes et raids</td></tr><tr><td>/contrat</td><td>Contrats du jour et de guilde</td></tr>"
         + "<tr><td>/traite</td><td>Traités</td></tr><tr><td>/station · /rail</td><td>Train</td></tr><tr><td>/aide</td><td>Appel à l'aide</td></tr>"
         + "<tr><td>/deguisement</td><td>Déguisement</td></tr><tr><td>/climat</td><td>Saison naturelle</td></tr><tr><td>/journal</td><td>Journal de guerre</td></tr>"
         + "<tr><td>/succes</td><td>Succès</td></tr><tr><td>/classement · /histoire</td><td>Saison et mémorial</td></tr><tr><td>/money · /baltop · /pay</td><td>Argent</td></tr>"
         + "<tr><td>/hud · /guide · /english</td><td>Tableau de bord, livre, anglais</td></tr></table>"));
      return b.append("</div>").toString();
   }

   // ------------------------------------------------------------------ JSON

   static String journalJson(WebExtra x) {
      StringBuilder b = new StringBuilder("{\"entries\":[");
      int n = 0;
      for (WebExtra.JournalView e : x.journal()) {
         if (n >= 50) {
            break;
         }

         b.append(n++ > 0 ? "," : "").append("{\"ts\":").append(e.ts()).append(",\"kind\":").append(json(e.kind())).append(",\"text\":").append(json(e.text())).append("}");
      }

      return b.append("]}").toString();
   }

   static String raidsJson(WebExtra x) {
      StringBuilder b = new StringBuilder("{\"raids\":[");
      int n = 0;
      for (WebExtra.RaidView r : x.raids()) {
         b.append(n++ > 0 ? "," : "").append("{\"target\":").append(json(r.targetName())).append(",\"attacker\":").append(json(r.attackerName())).append(",\"type\":")
            .append(json(r.type())).append(",\"mobsLeft\":").append(r.mobsLeft()).append(",\"mobsTotal\":").append(r.mobsTotal()).append(",\"startedAt\":").append(r.startedAt())
            .append(",\"defenders\":").append(r.defenders());
         if (r.nexusPercent() != null) {
            b.append(",\"nexusPercent\":").append(r.nexusPercent());
         }

         b.append("}");
      }

      return b.append("]}").toString();
   }

   // ------------------------------------------------------------------ script (mises à jour en direct, filtres, recherche)

   /** Empreinte du script : l'adresse change à chaque mise à jour du plugin, donc aucun navigateur ni cache intermédiaire ne sert un ancien script. */
   static final String JS_VERSION = Integer.toHexString(ExtraPages.APP_JS.hashCode());

   static final String APP_JS = """
      (function () {
        'use strict';
        try {
          var storedLang = localStorage.getItem('lang');
          if (storedLang === 'en' && !/[?&]lang=/.test(location.search)) { location.replace(location.pathname + '?lang=en' + location.hash); return; }
        } catch (e) {}
        document.addEventListener('click', function (ev) {
          var a = ev.target.closest ? ev.target.closest('a[data-lang]') : null;
          if (a) { try { localStorage.setItem('lang', a.getAttribute('data-lang')); } catch (e) {} }
        });
        var filter = '';
        function applyFilter() {
          document.querySelectorAll('.fb').forEach(function (b) { b.classList.toggle('on', (b.getAttribute('data-f') || '') === filter); });
          document.querySelectorAll('.ev[data-k]').forEach(function (e) { e.hidden = filter !== '' && e.getAttribute('data-k') !== filter; });
          document.querySelectorAll('[data-day]').forEach(function (d) {
            var n = d.nextElementSibling, any = false;
            while (n && !n.hasAttribute('data-day')) { if (!n.hidden) { any = true; } n = n.nextElementSibling; }
            d.hidden = !any;
          });
        }
        document.addEventListener('click', function (ev) {
          var b = ev.target.closest ? ev.target.closest('.fb') : null;
          if (b) { filter = b.getAttribute('data-f') || ''; applyFilter(); }
        });
        var q = document.getElementById('q');
        if (q) {
          q.addEventListener('input', function () {
            var v = q.value.trim().toLowerCase(), shown = 0;
            document.querySelectorAll('[data-s]').forEach(function (s) {
              var ok = !v || s.textContent.toLowerCase().indexOf(v) >= 0;
              s.hidden = !ok; if (ok) { shown++; }
            });
            var none = document.getElementById('none'); if (none) { none.hidden = shown > 0; }
          });
        }
        var fr = { off: {}, open: {} };
        function applyFr() {
          document.querySelectorAll('[data-js]').forEach(function (e) { e.hidden = false; });
          document.querySelectorAll('.gk').forEach(function (b) { b.classList.toggle('on', !fr.off[b.getAttribute('data-gid')]); });
          document.querySelectorAll('.fg, .fd').forEach(function (e) {
            var id = e.getAttribute('data-gid');
            e.style.display = fr.off[id] ? 'none' : '';
            if (e.tagName === 'DETAILS') { e.open = !!fr.open[id]; }
          });
        }
        document.addEventListener('click', function (ev) {
          var t = ev.target.closest ? ev.target : null;
          if (!t) { return; }
          var gk = t.closest('.gk');
          if (gk) { var id = gk.getAttribute('data-gid'); fr.off[id] = !fr.off[id]; applyFr(); return; }
          var all = t.closest('[data-all]');
          if (all) {
            var hide = all.getAttribute('data-all') === 'hide';
            document.querySelectorAll('.gk').forEach(function (b) { fr.off[b.getAttribute('data-gid')] = hide; });
            applyFr();
          }
        });
        document.addEventListener('toggle', function (ev) {
          var d = ev.target;
          if (d && d.classList && d.classList.contains('fd')) { fr.open[d.getAttribute('data-gid')] = d.open; }
        }, true);
        applyFr();
        var net = { z: 1, x: 0, y: 0, focus: '', off: {}, moved: false };
        function applyNet() {
          document.querySelectorAll('[data-js]').forEach(function (e) { e.hidden = false; });
          var g = document.getElementById('netg');
          if (!g) { return; }
          g.setAttribute('transform', 'translate(' + net.x + ' ' + net.y + ') scale(' + net.z + ')');
          var sel = document.getElementById('netfocus');
          if (sel) { sel.value = net.focus; }
          document.querySelectorAll('.nk').forEach(function (b) { b.classList.toggle('on', !net.off[b.getAttribute('data-nk')]); });
          var linked = {};
          document.querySelectorAll('.net .edge').forEach(function (l) {
            var k = l.getAttribute('data-k'), a = l.getAttribute('data-a'), b = l.getAttribute('data-b');
            var hide = !!net.off[k];
            l.style.display = hide ? 'none' : '';
            var rel = !net.focus || a === net.focus || b === net.focus;
            l.style.opacity = rel ? '' : '0.07';
            if (!hide && net.focus && rel) { linked[a] = true; linked[b] = true; }
          });
          document.querySelectorAll('.net .node').forEach(function (n) {
            var id = n.getAttribute('data-id');
            n.style.opacity = (!net.focus || id === net.focus || linked[id]) ? '' : '0.25';
          });
        }
        function zoomTo(nz, cx, cy) {
          nz = Math.max(0.5, Math.min(5, nz));
          net.x = cx - (cx - net.x) * nz / net.z;
          net.y = cy - (cy - net.y) * nz / net.z;
          net.z = nz;
          applyNet();
        }
        document.addEventListener('click', function (ev) {
          var t = ev.target.closest ? ev.target : null;
          if (!t) { return; }
          var nk = t.closest('.nk');
          if (nk) { var k = nk.getAttribute('data-nk'); net.off[k] = !net.off[k]; applyNet(); return; }
          var z = t.closest('[data-z]');
          if (z) {
            var m = z.getAttribute('data-z');
            if (m === 'reset') { net.z = 1; net.x = 0; net.y = 0; applyNet(); } else { zoomTo(net.z * (m === 'in' ? 1.3 : 1 / 1.3), 220, 215); }
            return;
          }
          if (net.moved && t.closest('.net')) { ev.preventDefault(); net.moved = false; }
        }, true);
        document.addEventListener('change', function (ev) {
          if (ev.target && ev.target.id === 'netfocus') { net.focus = ev.target.value; applyNet(); }
        });
        document.addEventListener('wheel', function (ev) {
          var svg = ev.target.closest ? ev.target.closest('.net') : null;
          if (!svg) { return; }
          ev.preventDefault();
          var r = svg.getBoundingClientRect(), k = 440 / r.width;
          zoomTo(net.z * (ev.deltaY < 0 ? 1.15 : 1 / 1.15), (ev.clientX - r.left) * k, (ev.clientY - r.top) * k);
        }, { passive: false });
        var drag = null;
        document.addEventListener('pointerdown', function (ev) {
          var svg = ev.target.closest ? ev.target.closest('.net') : null;
          if (!svg) { return; }
          drag = { px: ev.clientX, py: ev.clientY, x: net.x, y: net.y, k: 440 / svg.getBoundingClientRect().width };
          net.moved = false;
        });
        document.addEventListener('pointermove', function (ev) {
          if (!drag) { return; }
          var dx = ev.clientX - drag.px, dy = ev.clientY - drag.py;
          if (Math.abs(dx) + Math.abs(dy) > 4) { net.moved = true; }
          if (net.moved) { net.x = drag.x + dx * drag.k; net.y = drag.y + dy * drag.k; applyNet(); }
        });
        document.addEventListener('pointerup', function () { drag = null; });
        applyNet();
        var live = parseInt(document.body.getAttribute('data-live') || '0', 10);
        var main = document.querySelector('main');
        if (!live || !main || !window.fetch || !window.DOMParser) { return; }
        var stamp = document.getElementById('stamp');
        function refresh() {
          if (document.hidden) { return; }
          fetch(location.pathname + location.search, { cache: 'no-store', credentials: 'omit' }).then(function (r) { return r.ok ? r.text() : null; }).then(function (html) {
            if (!html) { return; }
            var next = new DOMParser().parseFromString(html, 'text/html').querySelector('main');
            if (!next) { return; }
            var y = window.scrollY;
            main.innerHTML = next.innerHTML;
            window.scrollTo(0, y);
            applyFilter();
            applyNet();
            applyFr();
            stamp = document.getElementById('stamp');
            if (stamp) { stamp.classList.add('flash'); setTimeout(function () { stamp && stamp.classList.remove('flash'); }, 900); }
          }).catch(function () {});
        }
        setInterval(refresh, Math.max(5, live) * 1000);
        document.addEventListener('visibilitychange', function () { if (!document.hidden) { refresh(); } });
      })();
      """;
}
