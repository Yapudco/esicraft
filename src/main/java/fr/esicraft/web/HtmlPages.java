package fr.esicraft.web;

import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Locale;

/**
 * Génération HTML / JSON à partir d'un instantané. Tout texte venant du jeu est échappé ({@link #esc}).
 * Aucun script, aucune ressource externe : la page est autonome (CSP {@code default-src 'none'}).
 */
final class HtmlPages {
   private static final DateTimeFormatter TIME = DateTimeFormatter.ofPattern("HH:mm:ss").withZone(ZoneId.of("Europe/Paris"));

   private static final String CSS = """
      :root{--bg:#0b0d13;--bg2:#11141d;--card:rgba(255,255,255,.045);--card2:rgba(255,255,255,.07);--line:rgba(255,255,255,.09);
      --tx:#eef0f6;--mut:#9099b0;--acc:#f7b84b;--acc2:#ff7a59;--ok:#34d399;--shadow:0 10px 30px -12px rgba(0,0,0,.6);color-scheme:dark}
      @media (prefers-color-scheme:light){:root{--bg:#f4f5fa;--bg2:#eceef6;--card:#fff;--card2:#f7f8fc;--line:rgba(20,24,40,.08);
      --tx:#151826;--mut:#5f6880;--acc:#d98a12;--acc2:#e85d3a;--ok:#10a36f;--shadow:0 10px 30px -14px rgba(30,40,80,.25);color-scheme:light}}
      *{box-sizing:border-box}html{-webkit-text-size-adjust:100%}
      body{margin:0;min-height:100vh;background:var(--bg);color:var(--tx);font:15.5px/1.55 ui-sans-serif,system-ui,-apple-system,"Segoe UI",Roboto,sans-serif;
      font-variant-numeric:tabular-nums;background-image:radial-gradient(900px 500px at 10% -10%,rgba(247,184,75,.13),transparent 60%),
      radial-gradient(800px 500px at 100% 0%,rgba(255,122,89,.10),transparent 60%)}
      a{color:inherit;text-decoration:none}
      .w{max-width:1080px;margin:0 auto;padding:0 16px}
      header{position:sticky;top:0;z-index:10;backdrop-filter:blur(14px) saturate(1.4);-webkit-backdrop-filter:blur(14px) saturate(1.4);
      background:color-mix(in srgb,var(--bg) 72%,transparent);border-bottom:1px solid var(--line)}
      nav{display:flex;align-items:center;gap:6px;padding:12px 0;flex-wrap:wrap}
      .brand{display:flex;align-items:center;gap:10px;font-weight:800;font-size:1.1rem;margin-right:auto;letter-spacing:-.01em}
      .logo{width:34px;height:34px;border-radius:11px;display:grid;place-items:center;background:linear-gradient(135deg,var(--acc),var(--acc2));
      color:#1a1206;font-weight:900;box-shadow:0 6px 18px -6px var(--acc2)}
      .nl{padding:8px 14px;border-radius:999px;color:var(--mut);font-weight:600;font-size:.93rem;transition:background .2s,color .2s}
      .nl:hover{background:var(--card2);color:var(--tx)}.nl.on{background:var(--tx);color:var(--bg)}
      main{padding-bottom:40px;animation:in .45s ease both}
      @keyframes in{from{opacity:0;transform:translateY(8px)}to{opacity:1;transform:none}}
      @media (prefers-reduced-motion:reduce){*{animation:none!important;transition:none!important}}
      .hero{margin:28px 0 8px;padding:32px 28px;border-radius:28px;border:1px solid var(--line);position:relative;overflow:hidden;
      background:linear-gradient(135deg,rgba(247,184,75,.16),rgba(255,122,89,.08) 45%,var(--card) 80%);box-shadow:var(--shadow)}
      .hero h1{margin:0 0 6px;font-size:clamp(1.7rem,4.5vw,2.6rem);letter-spacing:-.03em;line-height:1.1}
      .hero p{margin:0;color:var(--mut)}
      .live{display:inline-flex;align-items:center;gap:8px;padding:6px 12px;border-radius:999px;background:var(--card2);border:1px solid var(--line);
      font-size:.85rem;font-weight:600;margin-bottom:14px}
      .dot{width:8px;height:8px;border-radius:50%;background:var(--ok);box-shadow:0 0 0 0 var(--ok);animation:pulse 2s infinite}
      @keyframes pulse{0%{box-shadow:0 0 0 0 rgba(52,211,153,.6)}70%{box-shadow:0 0 0 9px rgba(52,211,153,0)}100%{box-shadow:0 0 0 0 rgba(52,211,153,0)}}
      h2{font-size:1.15rem;margin:34px 0 14px;letter-spacing:-.01em;display:flex;align-items:center;gap:10px}
      h2 small{color:var(--mut);font-weight:500;font-size:.85rem}
      .grid{display:grid;grid-template-columns:repeat(auto-fit,minmax(min(100%,160px),1fr));gap:14px}
      .card{background:var(--card);border:1px solid var(--line);border-radius:22px;padding:18px;box-shadow:var(--shadow)}
      .stat{transition:transform .2s,border-color .2s}.stat:hover{transform:translateY(-3px);border-color:color-mix(in srgb,var(--acc) 45%,var(--line))}
      .stat .i{width:38px;height:38px;border-radius:12px;display:grid;place-items:center;background:var(--card2);font-size:1.15rem;margin-bottom:12px}
      .stat .n{overflow-wrap:anywhere;font-size:1.65rem;font-weight:800;letter-spacing:-.02em;line-height:1.1}.stat .l{color:var(--mut);font-size:.85rem;margin-top:2px}
      .list{display:flex;flex-direction:column;gap:10px}
      .row{display:flex;align-items:center;gap:14px;padding:14px 16px;border-radius:20px;background:var(--card);border:1px solid var(--line);
      transition:transform .2s,background .2s,border-color .2s}
      a.row:hover{transform:translateY(-2px);background:var(--card2);border-color:color-mix(in srgb,var(--acc) 40%,var(--line))}
      .rank{width:34px;flex:none;text-align:center;font-weight:800;color:var(--mut)}
      .rank.m1,.rank.m2,.rank.m3{font-size:1.35rem}
      .av{--h:30;flex:none;width:44px;height:44px;border-radius:14px;display:grid;place-items:center;font-weight:800;color:#fff;font-size:.95rem;
      background:linear-gradient(135deg,hsl(var(--h) 75% 58%),hsl(calc(var(--h) + 40) 70% 42%));box-shadow:0 6px 16px -8px hsl(var(--h) 70% 45%)}
      .av.sm{width:34px;height:34px;border-radius:11px;font-size:.8rem}.av.lg{width:72px;height:72px;border-radius:22px;font-size:1.5rem}
      .grow{flex:1;min-width:0}.t{font-weight:700;white-space:nowrap;overflow:hidden;text-overflow:ellipsis}
      .chips{display:flex;flex-wrap:wrap;gap:6px;margin-top:6px}
      .chip{display:inline-flex;align-items:center;gap:5px;padding:3px 10px;border-radius:999px;background:var(--card2);border:1px solid var(--line);
      font-size:.78rem;color:var(--mut);font-weight:600;white-space:nowrap}
      .chip.acc{color:var(--acc);border-color:color-mix(in srgb,var(--acc) 35%,transparent);background:color-mix(in srgb,var(--acc) 10%,transparent)}
      .chip.ok{color:var(--ok);border-color:color-mix(in srgb,var(--ok) 35%,transparent);background:color-mix(in srgb,var(--ok) 10%,transparent)}
      .score{text-align:right;flex:none;min-width:92px}.score b{font-size:1.15rem}.score .mut{display:block;font-size:.75rem}
      .bar{height:6px;border-radius:999px;background:var(--card2);overflow:hidden;margin-top:8px}
      .bar i{display:block;height:100%;border-radius:999px;background:linear-gradient(90deg,var(--acc),var(--acc2))}
      .people{display:grid;grid-template-columns:repeat(auto-fill,minmax(min(100%,230px),1fr));gap:10px}
      .person{display:flex;align-items:center;gap:12px;padding:12px 14px;border-radius:18px;background:var(--card);border:1px solid var(--line)}
      .person .s{color:var(--mut);font-size:.8rem;white-space:nowrap;overflow:hidden;text-overflow:ellipsis}
      .on-dot{width:9px;height:9px;border-radius:50%;background:var(--ok);flex:none}.off-dot{width:9px;height:9px;border-radius:50%;background:var(--line);flex:none}
      .tl{position:relative;padding-left:22px}.tl:before{content:"";position:absolute;left:6px;top:6px;bottom:6px;width:2px;border-radius:2px;background:var(--line)}
      .ev{position:relative;padding:10px 14px;margin-bottom:8px;border-radius:16px;background:var(--card);border:1px solid var(--line)}
      .ev:before{content:"";position:absolute;left:-21px;top:16px;width:10px;height:10px;border-radius:50%;background:var(--acc);box-shadow:0 0 0 4px var(--bg)}
      .ev time{display:block;color:var(--mut);font-size:.75rem;font-weight:600}
      .gh{display:flex;align-items:center;gap:18px;flex-wrap:wrap}
      .empty{padding:26px;text-align:center;color:var(--mut);border:1px dashed var(--line);border-radius:20px}
      .mut{color:var(--mut)}.money{color:var(--acc);font-weight:700}
      .two{display:grid;grid-template-columns:minmax(0,1.4fr) minmax(0,1fr);gap:22px}@media (max-width:820px){.two{grid-template-columns:minmax(0,1fr)}}
      footer{color:var(--mut);font-size:.8rem;padding:26px 0 34px;text-align:center}
      [hidden]{display:none!important}
      .s{color:var(--mut);font-size:.8rem}
      .chip.bad{color:#f87171;border-color:rgba(248,113,113,.4);background:rgba(248,113,113,.1)}
      .chip.rare{color:#c084fc;border-color:rgba(192,132,252,.45);background:rgba(192,132,252,.12)}
      a.chip:hover{color:var(--tx)}
      .row.alert{margin-top:14px;border-color:rgba(248,113,113,.5);background:linear-gradient(90deg,rgba(248,113,113,.14),var(--card))}
      .card.raid{margin-bottom:12px;border-color:rgba(248,113,113,.4)}.card.raid .bar i{background:linear-gradient(90deg,#f87171,var(--acc))}
      .card.nature{margin-top:14px}.big{font-size:1.9rem;line-height:1}
      .chart{width:100%;height:auto;display:block;margin-top:8px}.chart text{fill:var(--mut);font-size:11px}.chart .gl{stroke:var(--line)}
      .chart .lbl{fill:var(--tx);font-weight:700;font-size:12px}.chart .num{fill:#fff;font-weight:800;font-size:11px}.frise{min-width:700px}
      .legend{display:flex;flex-wrap:wrap;gap:6px;margin-top:10px}.sw{width:10px;height:10px;border-radius:3px;display:inline-block}
      .net{width:100%;max-width:560px;margin:0 auto;display:block;height:auto}.net text{fill:var(--tx);font-size:11px;font-weight:600;paint-order:stroke;stroke:var(--bg);stroke-width:3px;stroke-linejoin:round}.net .tag{fill:#10131a;stroke:none;font-weight:800;font-size:10px}
      .duo{display:grid;grid-template-columns:repeat(2,minmax(0,1fr));gap:16px}@media (max-width:820px){.duo{grid-template-columns:minmax(0,1fr)}}
      .net a:hover circle{stroke:var(--acc)}
      .av.head{background-size:cover;background-position:center;background-repeat:no-repeat;image-rendering:pixelated;background-color:var(--card2)}[hidden]{display:none!important}
      .logoimg{width:36px;height:36px;border-radius:10px;image-rendering:pixelated;display:block}.nl.lang{margin-left:auto;font-weight:800;border:1px solid var(--line)}
      .gk:not(.on){opacity:.4;text-decoration:line-through}.fdl{display:grid;gap:10px}
      .fd summary{cursor:pointer;display:flex;flex-wrap:wrap;align-items:center;gap:8px;list-style:none}.fd summary::-webkit-details-marker{display:none}.fd summary::before{content:'▸';color:var(--mut)}.fd[open] summary::before{content:'▾'}
      .fdb{margin-top:10px}.reqs{display:grid;gap:6px;margin-top:8px}.req{display:grid;grid-template-columns:22px 1fr auto;gap:2px 10px;align-items:center;font-size:.88rem}
      .req .bar{grid-column:1/-1;margin:0}.req.ok .ck{color:var(--ok)}.req.ko .ck{color:#f87171}.req .rv{font-variant-numeric:tabular-nums;color:var(--mut)}
      .net{touch-action:none;cursor:grab;overflow:hidden}.net .node,.net .edge{transition:opacity .2s}
      .nettools{display:flex;flex-wrap:wrap;gap:8px;justify-content:center;align-items:center;margin-bottom:8px}.zb{display:inline-flex;gap:6px}
      .nsel{background:var(--card2);color:var(--tx);border:1px solid var(--line);border-radius:999px;padding:5px 12px;font:inherit;font-size:.85rem}
      button.chip{cursor:pointer;font:inherit;color:var(--tx)}.nk:not(.on){opacity:.4;text-decoration:line-through}
      .scroll{overflow-x:auto}.heat{border-collapse:separate;border-spacing:4px;width:100%}.heat th,.heat td{padding:7px 8px;text-align:center;font-size:.8rem;border-radius:8px;white-space:nowrap}
      .heat th{color:var(--mut);font-weight:700}
      .filters{display:flex;flex-wrap:wrap;gap:6px;margin:6px 0 16px}.fb{cursor:pointer;font:inherit;font-size:.82rem;padding:6px 12px}.fb.on{background:var(--tx);color:var(--bg);border-color:var(--tx)}
      .day{font-weight:700;color:var(--tx);margin:16px 0 8px -22px;text-transform:capitalize;font-size:.9rem}
      .ev[data-k=RAID]:before,.ev[data-k=CONQUEST]:before,.ev[data-k=BETRAYAL]:before{background:#f87171}
      .ev[data-k=DEFENSE]:before,.ev[data-k=HELP]:before{background:var(--ok)}.ev[data-k=TREATY]:before{background:#60a5fa}
      .ev[data-k=ACHIEVEMENT]:before{background:#c084fc}.ev[data-k=NATURE]:before{background:#a3e635}
      .ev .chips{margin-top:6px}
      .achs{display:grid;grid-template-columns:repeat(auto-fill,minmax(min(100%,260px),1fr));gap:10px}
      .ach{display:flex;gap:12px;align-items:flex-start;padding:12px 14px;border-radius:18px;background:var(--card);border:1px solid var(--line)}
      .ach .ic{flex:none;font-size:1.35rem;width:44px;height:44px;border-radius:14px;display:grid;place-items:center;background:var(--card2)}
      .ach.lock{opacity:.5}.ach.lock .ic{filter:grayscale(1)}.ach .bar{margin:8px 0 4px}
      .card.trophy{display:block;transition:transform .2s,border-color .2s}.card.trophy:hover{transform:translateY(-2px);border-color:color-mix(in srgb,var(--acc) 40%,var(--line))}
      .search{width:100%;margin:16px 0 4px;padding:14px 18px;border-radius:18px;border:1px solid var(--line);background:var(--card);color:var(--tx);font:inherit;font-size:1rem;box-shadow:var(--shadow)}
      .search:focus{outline:2px solid color-mix(in srgb,var(--acc) 60%,transparent);outline-offset:2px}
      .guide{display:grid;gap:12px;margin-top:14px}.gs h3{margin:0 0 8px;font-size:1.05rem}.gs p,.gs li{color:var(--mut)}.gs b{color:var(--tx)}.gs p{margin:6px 0}.gs ul,.gs ol{margin:6px 0;padding-left:20px}
      .cmd{width:100%;border-collapse:collapse}.cmd td{padding:7px 4px;border-bottom:1px solid var(--line);font-size:.9rem}.cmd td:first-child{font-weight:700;white-space:nowrap;color:var(--acc);padding-right:14px}
      #stamp{transition:color .3s}#stamp.flash{color:var(--ok)}
      .nl.raidl{color:#f87171}
      @media (max-width:860px){nav{flex-wrap:nowrap;overflow-x:auto;scrollbar-width:none;-webkit-overflow-scrolling:touch}nav::-webkit-scrollbar{display:none}.nl{flex:none}.brand{flex:none}}
      @media (max-width:560px){.hero{padding:24px 20px;border-radius:24px}.score{min-width:64px}.row{padding:12px;gap:10px}nav{flex-wrap:nowrap;gap:2px}.brand .bn{display:none}.nl{padding:7px 11px;font-size:.88rem}.grid{grid-template-columns:repeat(2,minmax(0,1fr));gap:10px}.stat{padding:14px}.stat .n{font-size:1.25rem}.stat .i{width:32px;height:32px;margin-bottom:8px}.rank{width:24px}}
      """;

   private HtmlPages() {
   }

   // ------------------------------------------------------------------ utilitaires

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

   /** Retire les codes couleur Minecraft (&x, §x, y compris hexadécimaux §x§r§r§g§g§b§b). */
   static String plain(String s) {
      return s == null ? "" : s.replaceAll("[&§]#[0-9a-fA-F]{6}", "").replaceAll("[&§][0-9a-fk-orxA-FK-ORX]", "");
   }

   static String money(double v) {
      return String.format(Locale.FRANCE, "%,.0f", v).replace(' ', ' ').replace(' ', ' ');
   }

   /** Teinte stable dérivée d'un texte, pour colorer les avatars. */
   static int hue(String s) {
      return Math.floorMod(s == null ? 0 : s.hashCode() * 31 + 7, 360);
   }

   static String initials(String s, boolean two) {
      String p = plain(s).trim();
      if (p.isEmpty()) {
         return "?";
      }

      int cp = p.codePointAt(0);
      String first = new String(Character.toChars(Character.toUpperCase(cp)));
      int next = p.offsetByCodePoints(0, 1);
      if (two && next < p.length()) {
         return esc(first + p.substring(next, p.offsetByCodePoints(next, 1)).toUpperCase(Locale.ROOT));
      }

      return esc(first);
   }

   static String avatar(String label, String seed, String size) {
      return avatar(label, seed, size, false);
   }

   /** Avatar d'un joueur : sa tête (servie par {@code /head/<pseudo>}, jamais un service externe) ; visage de remplacement tant qu'elle n'est pas prête. */
   static String playerAvatar(String name, String size) {
      return "<span class=\"av head" + (size.isEmpty() ? "" : " " + size) + "\" style=\"background-image:url('/head/" + esc(name) + "')\" aria-hidden=\"true\"></span>";
   }

   static String avatar(String label, String seed, String size, boolean two) {
      return "<span class=\"av" + (size.isEmpty() ? "" : " " + size) + "\" style=\"--h:" + hue(seed) + "\" aria-hidden=\"true\">" + initials(label, two) + "</span>";
   }

   static String medal(int rank) {
      return switch (rank) {
         case 1 -> "<span class=\"rank m1\">🥇</span>";
         case 2 -> "<span class=\"rank m2\">🥈</span>";
         case 3 -> "<span class=\"rank m3\">🥉</span>";
         default -> "<span class=\"rank\">" + rank + "</span>";
      };
   }

   static String stat(String icon, String n, String label) {
      return "<div class=\"card stat\"><div class=\"i\">" + icon + "</div><div class=\"n\">" + esc(n) + "</div><div class=\"l\">" + esc(label) + "</div></div>";
   }

   static String empty(String text) {
      return "<div class=\"empty\">" + esc(text) + "</div>";
   }

   // ------------------------------------------------------------------ gabarit

   static String page(String serverName, String active, String title, String body, WebSnapshot snap) {
      return page(serverName, active, title, body, snap, 15, false);
   }

   /** {@code live} : secondes entre deux rafraîchissements automatiques (0 = aucun). */
   static String page(String serverName, String active, String title, String body, WebSnapshot snap, int live, boolean raidActive) {
      String time = snap.generatedAt() == 0L ? "en cours de chargement…" : "mises à jour à " + TIME.format(Instant.ofEpochMilli(snap.generatedAt()));
      return "<!doctype html><html lang=\"fr\"><head><meta charset=\"utf-8\"><meta name=\"viewport\" content=\"width=device-width,initial-scale=1\">"
         + "<meta name=\"color-scheme\" content=\"dark light\"><meta name=\"description\" content=\"Guildes, joueurs et saison du serveur " + esc(serverName) + "\">"
         + "<title>" + esc(title) + " · " + esc(serverName) + "</title><style>" + CSS + "</style><link rel=\"icon\" type=\"image/png\" href=\"/assets/logo.png\"><script src=\"/assets/app.js?v=" + ExtraPages.JS_VERSION + "\" defer></script></head><body data-live=\"" + live + "\">"
         + "<header><div class=\"w\"><nav><a class=\"brand\" href=\"/\"><img class=\"logoimg\" src=\"/assets/logo.png\" alt=\"\" width=\"36\" height=\"36\"><span class=\"bn\">" + esc(serverName) + "</span></a>"
         + navLink("/", "Accueil", "home", active) + navLink("/guildes", "Guildes", "guildes", active) + navLink("/joueurs", "Joueurs", "joueurs", active)
         + (raidActive ? "<a class=\"nl raidl" + ("raids".equals(active) ? " on" : "") + "\" href=\"/raids\">⚔ Raids</a>" : "")
         + navLink("/journal", "Journal", "journal", active) + navLink("/diplomatie", "Diplomatie", "diplomatie", active) + navLink("/musee", "Musée", "musee", active)
         + navLink("/ages", "Âges", "ages", active) + navLink("/guide", "Guide", "guide", active)
         + "<!--LANG--></nav></div></header><main class=\"w\">" + body
         + "<footer><span id=\"stamp\">Données " + esc(time) + "</span>" + (live > 0 ? " · mise à jour automatique" : "")
         + " · <a class=\"money\" href=\"/api/stats.json\">API JSON</a></footer></main></body></html>";
   }

   private static String navLink(String href, String label, String key, String active) {
      return "<a class=\"nl" + (key.equals(active) ? " on" : "") + "\" href=\"" + href + "\">" + label + "</a>";
   }

   // ------------------------------------------------------------------ composants

   static String guildRows(List<WebSnapshot.GuildView> guilds, boolean detailed) {
      if (guilds.isEmpty()) {
         return empty("Aucune guilde pour le moment.");
      }

      long top = Math.max(1L, guilds.get(0).score());
      StringBuilder b = new StringBuilder("<div class=\"list\">");
      int rank = 1;
      for (WebSnapshot.GuildView g : guilds) {
         int pct = (int)Math.max(3L, Math.min(100L, g.score() * 100L / top));
         b.append("<a class=\"row\" href=\"/guilde/").append(esc(g.id())).append("\">").append(medal(rank++)).append(avatar(g.tag(), g.id(), "", true))
            .append("<div class=\"grow\"><div class=\"t\">").append(esc(g.name())).append(g.champion() ? " 👑" : "").append("</div><div class=\"chips\">")
            .append("<span class=\"chip acc\">").append(esc(g.tag())).append("</span><span class=\"chip\">").append(esc(g.age())).append("</span>")
            .append("<span class=\"chip\">👥 ").append(g.members().size()).append("</span><span class=\"chip\">🗺️ ").append(g.claims()).append("</span>");
         if (detailed) {
            b.append("<span class=\"chip\">").append(esc(g.specialization())).append("</span>");
            if (!g.treaties().isEmpty()) {
               b.append("<span class=\"chip\">📜 ").append(g.treaties().size()).append("</span>");
            }
         }

         b.append("</div><div class=\"bar\"><i style=\"width:").append(pct).append("%\"></i></div></div>")
            .append("<div class=\"score\"><b>").append(money(g.score())).append("</b><span class=\"mut\">points</span></div></a>");
      }

      return b.append("</div>").toString();
   }

   static String onlineGrid(WebSnapshot s) {
      if (s.online().isEmpty()) {
         return empty("Personne n'est connecté pour le moment.");
      }

      StringBuilder b = new StringBuilder("<div class=\"people\">");
      for (WebSnapshot.OnlineView p : s.online()) {
         b.append("<a class=\"person row\" href=\"/joueur/" + esc(p.name().toLowerCase(Locale.ROOT)) + "\">")
            .append(playerAvatar(p.name(), "sm")).append("<div class=\"grow\"><div class=\"t\">").append(esc(p.name())).append("</div><div class=\"s\">")
            .append(esc(p.race())).append(" · ").append(p.guildName() != null ? esc(p.guildName()) : "sans guilde").append("</div></div>");
         if (p.coins() != null) {
            b.append("<span class=\"money\">").append(money(p.coins())).append("</span>");
         } else {
            b.append("<span class=\"on-dot\"></span>");
         }

         b.append("</a>");
      }

      return b.append("</div>").toString();
   }

   static String timeline(List<String> history) {
      if (history.isEmpty()) {
         return empty("Rien à signaler pour le moment.");
      }

      StringBuilder b = new StringBuilder("<div class=\"tl\">");
      for (String raw : history) {
         String line = plain(raw);
         int sep = line.indexOf(" » ");
         String when = sep > 0 ? line.substring(0, sep) : "";
         String what = sep > 0 ? line.substring(sep + 3) : line;
         b.append("<div class=\"ev\"><time>").append(esc(when)).append("</time>").append(esc(what)).append("</div>");
      }

      return b.append("</div>").toString();
   }

   // ------------------------------------------------------------------ pages

   static String home(String serverName, WebSnapshot s, WebExtra x) {
      StringBuilder b = new StringBuilder("<section class=\"hero\"><span class=\"live\"><span class=\"dot\"></span>")
         .append(s.onlineCount()).append(s.onlineCount() > 1 ? " joueurs" : " joueur").append(" en ligne</span><h1>")
         .append(esc(serverName)).append("</h1><p>Saison ").append(esc(s.season())).append(" · ").append(s.daysLeft()).append(" jours restants")
         .append(s.event() != null ? " · " + esc(plain(s.event())) : "").append("</p></section>").append(ExtraPages.homeBanner(x)).append(ExtraPages.natureCard(x));
      b.append("<div class=\"grid\" style=\"margin-top:16px\">")
         .append(stat("🟢", s.onlineCount() + " / " + s.maxPlayers(), "Joueurs en ligne"))
         .append(stat("🏰", String.valueOf(s.guilds().size()), "Guildes"))
         .append(stat("🗺️", String.valueOf(s.guilds().stream().mapToInt(WebSnapshot.GuildView::claims).sum()), "Chunks revendiqués"))
         .append(stat("🏆", s.guilds().isEmpty() ? "-" : s.guilds().get(0).name(), "Guilde en tête"))
         .append("</div>");
      b.append("<div class=\"two\"><div><h2>🏆 Top guildes <small><a href=\"/guildes\">tout voir →</a></small></h2>")
         .append(guildRows(s.guilds().stream().limit(5).toList(), false)).append("<p><a class=\"nl\" href=\"/evolution\">📈 Courbes d'évolution →</a></p></div><div><h2>📰 Journal de guerre <small><a href=\"/journal\">tout voir →</a></small></h2>")
         .append(x.journal().isEmpty() ? timeline(s.history()) : ExtraPages.journalList(s, x.journal().stream().limit(8).toList(), s.generatedAt(), false)).append("</div></div>");
      b.append("<h2>🟢 En ligne</h2>").append(onlineGrid(s));
      List<WebExtra.ProfileView> honorable = x.profiles().stream().filter(p -> p.reputation() > 0).sorted((a, c) -> Integer.compare(c.reputation(), a.reputation())).limit(10).toList();
      if (!honorable.isEmpty()) {
         int topRep = honorable.get(0).reputation();
         b.append("<h2>❤ Les plus honorables <small>/merci &lt;joueur&gt;</small></h2><div class=\"list\">");
         int hr = 1;
         for (WebExtra.ProfileView p : honorable) {
            b.append("<a class=\"row\" href=\"").append(ExtraPages.profileLink(p.name())).append("\">").append(medal(hr++)).append(playerAvatar(p.name(), "sm")).append("<div class=\"grow\"><div class=\"t\">")
               .append(esc(p.name())).append("</div><div class=\"bar\"><i style=\"width:").append(Math.max(3, p.reputation() * 100 / topRep)).append("%\"></i></div></div><div class=\"score\"><b>")
               .append(p.reputation()).append("</b><span class=\"mut\">honneur</span></div></a>");
         }

         b.append("</div>");
      }

      return b.toString();
   }

   static String guilds(WebSnapshot s) {
      return "<section class=\"hero\"><h1>Guildes</h1><p>Classement de la saison " + esc(s.season()) + " : territoire, Âge, richesse, raids repoussés et conquêtes.</p></section>"
         + "<h2>🏰 Classement <small>" + s.guilds().size() + " guilde" + (s.guilds().size() > 1 ? "s" : "") + " · <a href=\"/evolution\">📈 courbes</a> · <a href=\"/diplomatie\">🕸 diplomatie</a></small></h2>" + guildRows(s.guilds(), true);
   }

   static String players(WebSnapshot s, WebExtra x) {
      StringBuilder b = new StringBuilder("<section class=\"hero\"><span class=\"live\"><span class=\"dot\"></span>")
         .append(s.onlineCount()).append(" / ").append(s.maxPlayers()).append(" connectés</span><h1>Joueurs</h1><p>Qui joue en ce moment, et qui a le plus de pièces.</p></section>");
      b.append("<h2>🟢 En ligne</h2>").append(onlineGrid(s));
      if (!s.rich().isEmpty()) {
         double top = Math.max(1.0, s.rich().get(0).coins());
         b.append("<h2>💰 Les plus riches</h2><div class=\"list\">");
         int rank = 1;
         for (WebSnapshot.RichView r : s.rich()) {
            int pct = (int)Math.max(3L, Math.min(100L, Math.round(r.coins() * 100.0 / top)));
            b.append("<a class=\"row\" href=\"/joueur/").append(esc(r.name().toLowerCase(Locale.ROOT))).append("\">").append(medal(rank++)).append(playerAvatar(r.name(), "sm")).append("<div class=\"grow\"><div class=\"t\">")
               .append(esc(r.name())).append("</div><div class=\"bar\"><i style=\"width:").append(pct).append("%\"></i></div></div><div class=\"score\"><b class=\"money\">")
               .append(money(r.coins())).append("</b><span class=\"mut\">pièces</span></div></a>");
         }

         b.append("</div>");
      }

      return b.append(ExtraPages.directory(x)).toString();
   }

   static String guild(WebSnapshot.GuildView g) {
      StringBuilder b = new StringBuilder("<section class=\"hero\"><div class=\"gh\">").append(avatar(g.tag(), g.id(), "lg", true)).append("<div><h1>")
         .append(esc(g.name())).append(g.champion() ? " 👑" : "").append("</h1><div class=\"chips\"><span class=\"chip acc\">").append(esc(g.tag()))
         .append("</span><span class=\"chip\">").append(esc(g.age())).append("</span><span class=\"chip\">").append(esc(g.specialization())).append("</span>");
      long online = g.members().stream().filter(WebSnapshot.MemberView::online).count();
      if (online > 0) {
         b.append("<span class=\"chip ok\">● ").append(online).append(" en ligne</span>");
      }

      b.append("</div></div></div></section><div class=\"grid\" style=\"margin-top:16px\">")
         .append(stat("🏆", money(g.score()), "Score de saison"))
         .append(stat("👥", String.valueOf(g.members().size()), "Membres"))
         .append(stat("🗺️", String.valueOf(g.claims()), "Chunks revendiqués"));
      if (g.bank() != null) {
         b.append(stat("💰", money(g.bank()), "Banque"));
      }

      b.append(stat("🛡️", String.valueOf(g.repelled()), "Raids repoussés")).append(stat("⚔️", String.valueOf(g.conquests()), "Conquêtes"));
      if (g.nexusPercent() != null) {
         b.append(stat("💎", g.nexusPercent() + " %", "Nexus"));
      }

      b.append("</div>");
      if (!g.treaties().isEmpty()) {
         b.append("<h2>📜 Traités</h2><div class=\"chips\">");
         for (String t : g.treaties()) {
            b.append("<span class=\"chip acc\" style=\"font-size:.85rem;padding:6px 14px\">").append(esc(t)).append("</span>");
         }

         b.append("</div>");
      }

      b.append("<h2>👥 Membres <small>").append(g.members().size()).append("</small></h2><div class=\"people\">");
      for (WebSnapshot.MemberView m : g.members()) {
         b.append("<a class=\"person row\" href=\"/joueur/").append(esc(m.name().toLowerCase(Locale.ROOT))).append("\">").append(playerAvatar(m.name(), "sm")).append("<div class=\"grow\"><div class=\"t\">").append(esc(m.name()))
            .append("</div><div class=\"s\">").append(esc(m.role())).append(m.race() != null ? " · " + esc(m.race()) : "").append("</div></div>")
            .append(m.online() ? "<span class=\"on-dot\" title=\"En ligne\"></span>" : "<span class=\"off-dot\" title=\"Hors ligne\"></span>").append("</a>");
      }

      return b.append("</div>").toString();
   }

   static String notFound() {
      return "<section class=\"hero\" style=\"text-align:center\"><h1>404</h1><p>Cette page n'existe pas (ou plus).</p>"
         + "<p style=\"margin-top:18px\"><a class=\"nl on\" href=\"/\">Retour à l'accueil</a></p></section>";
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
         } else if (c < 0x20 || c == ' ' || c == ' ' || c == '<' || c == '>') {
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
