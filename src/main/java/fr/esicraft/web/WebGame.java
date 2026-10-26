package fr.esicraft.web;

import fr.esicraft.EsicraftPlugin;
import fr.esicraft.game.CourtManager;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Locale;

/** Pages du site pour les nouveautés de jeu : élections en ligne, Panthéon (boss, sièges, Tribunal) et leur script. */
final class WebGame {
   private static final DateTimeFormatter DAY = DateTimeFormatter.ofPattern("d MMM yyyy", Locale.FRENCH).withZone(ZoneId.systemDefault());

   private WebGame() {
   }

   static String votes(boolean enabled) {
      if (!enabled) {
         return "<section class=\"hero\"><h1>🗳 Élections de la semaine</h1><p>Les élections du site sont désactivées sur ce serveur.</p></section>";
      }

      return "<section class=\"hero\"><h1>🗳 Élections de la semaine</h1><p>Chaque semaine, la communauté élit le plus débile, le plus gros, le plus drôle et le plus chiant. "
         + "Revendiquez votre pseudo avec un code à taper dans le chat du jeu, puis votez : votre navigateur vous reconnaît ensuite grâce à un cookie.</p></section>"
         + "<div id=\"va\" class=\"card\" style=\"margin-top:16px\"><p class=\"empty\">Chargement des élections… (JavaScript requis)</p></div>"
         + "<script src=\"/assets/votes.js\" defer></script>";
   }

   private static String cell(Object o) {
      return HtmlPages.esc(String.valueOf(o));
   }

   /** Page de la bourse : cours, variation sur 24 h, mini-graphique et facteurs de valeur de chaque guilde cotée. */
   static String bourse(EsicraftPlugin plugin) {
      StringBuilder b = new StringBuilder("<section class=\"hero\"><h1>📈 La bourse des guildes</h1><p>Le cours de chaque action suit la valeur de la guilde (richesse, territoire, effectif, "
         + "réputation de ses membres, résultats de la saison, force du Nexus, diversité des métiers, Âge), l'humeur du marché, les événements (raids, sièges) et les ordres des investisseurs.</p></section>");
      var quotes = plugin.getBourse().quotes();
      double mood = plugin.getBourse().moodValue();
      b.append("<p style=\"margin:14px 0\">Humeur du marché : <b>").append(mood > 0.05 ? "optimiste" : mood < -0.05 ? "pessimiste" : "neutre").append("</b> (")
         .append(String.format(Locale.ROOT, "%+.1f", mood * 100)).append(" %)</p>");
      if (quotes.isEmpty()) {
         return b.append("<div class=\"card\"><p class=\"empty\">Aucune guilde n'est cotée pour l'instant (Âge 4 requis, <b>/bourse ouvrir</b> en jeu).</p></div>").toString();
      }

      b.append("<div class=\"card\"><table class=\"tbl\"><thead><tr><th>Guilde</th><th>Cours</th><th>24 h</th><th>Évolution</th><th>Âge</th><th>Réputation moy.</th><th>Nexus</th><th>Parts libres</th></tr></thead><tbody>");
      for (var q : quotes) {
         double ch = q.change24();
         b.append("<tr><td>").append(cell(q.guildName())).append("</td><td><b>").append(String.format(Locale.ROOT, "%.2f", q.price())).append("</b></td><td style=\"color:")
            .append(ch > 0.0005 ? "#2e9e5b" : ch < -0.0005 ? "#d04a4a" : "inherit").append("\">").append(ch > 0.0005 ? "▲ " : ch < -0.0005 ? "▼ " : "■ ")
            .append(String.format(Locale.ROOT, "%+.1f", ch * 100)).append(" %</td><td>").append(spark(q.spark(), ch)).append("</td>");
         var f = q.fund();
         if (f != null) {
            b.append("<td>").append(f.age()).append("</td><td>❤ ").append(String.format(Locale.ROOT, "%.1f", f.avgRep())).append("</td><td>niv. ").append(f.techLevel()).append(" · ")
               .append((int)Math.round(f.nexusPct() * 100)).append(" %</td>");
         } else {
            b.append("<td>—</td><td>—</td><td>—</td>");
         }

         b.append("<td>").append(q.free()).append(" / ").append(q.total()).append("</td></tr>");
      }

      return b.append("</tbody></table></div>").toString();
   }

   private static String spark(List<Double> pts, double change) {
      if (pts.size() < 2) {
         return "—";
      }

      double min = Double.MAX_VALUE;
      double max = -Double.MAX_VALUE;
      for (double p : pts) {
         min = Math.min(min, p);
         max = Math.max(max, p);
      }

      double span = Math.max(0.0001, max - min);
      StringBuilder pl = new StringBuilder();
      for (int i = 0; i < pts.size(); i++) {
         pl.append(String.format(Locale.ROOT, "%.1f,%.1f ", i * 100.0 / (pts.size() - 1), 22.0 - (pts.get(i) - min) / span * 20.0));
      }

      return "<svg viewBox=\"0 0 100 24\" width=\"110\" height=\"26\" role=\"img\" aria-label=\"évolution du cours\"><polyline fill=\"none\" stroke=\""
         + (change >= 0 ? "#2e9e5b" : "#d04a4a") + "\" stroke-width=\"2\" points=\"" + pl + "\"/></svg>";
   }

   static String pantheon(EsicraftPlugin plugin) {
      var db = plugin.getDataManager().getStorage();
      StringBuilder b = new StringBuilder("<section class=\"hero\"><h1>🏛 Panthéon</h1><p>Les exploits gravés dans la pierre : boss terrassés, guerres formelles et verdicts du Tribunal.</p></section>");
      CourtManager.Rule rule = plugin.getCourt().getActive();
      b.append("<h2 style=\"margin:22px 0 10px\">⚖ Tribunal des guildes</h2><div class=\"card\"><p>")
         .append(rule == null ? "Aucune règle en vigueur. Le scrutin a lieu chaque lundi (<b>/vote</b> en jeu)."
            : "Règle de la semaine : <b>" + cell(rule.label()) + "</b> — " + cell(rule.effect())).append("</p></div>");
      b.append("<h2 style=\"margin:22px 0 10px\">☠ Boss terrassés</h2>");
      List<Object[]> wins = db.select("SELECT ts, boss, heroic, guild_name, top_player, fighters FROM boss_wins ORDER BY ts DESC LIMIT 20;", 6);
      if (wins.isEmpty()) {
         b.append("<div class=\"card\"><p class=\"empty\">Aucun boss n'a encore été terrassé.</p></div>");
      } else {
         b.append("<div class=\"card\"><table class=\"tbl\"><thead><tr><th>Date</th><th>Boss</th><th>Meilleur combattant</th><th>Guilde</th><th>Combattants</th></tr></thead><tbody>");
         for (Object[] r : wins) {
            boolean heroic = ((Number)r[2]).intValue() == 1;
            b.append("<tr><td>").append(DAY.format(Instant.ofEpochMilli(((Number)r[0]).longValue()))).append("</td><td>").append(cell(r[1])).append(heroic ? " ★ héroïque" : "")
               .append("</td><td>").append(cell(r[4])).append("</td><td>").append(String.valueOf(r[3]).isEmpty() ? "—" : cell(r[3])).append("</td><td>").append(cell(r[5])).append("</td></tr>");
         }

         b.append("</tbody></table></div>");
      }

      b.append("<h2 style=\"margin:22px 0 10px\">⚔ Guerres formelles</h2>");
      List<Object[]> sieges = db.select("SELECT ts, attacker_name, defender_name, winner, damage FROM siege_history ORDER BY ts DESC LIMIT 15;", 5);
      if (sieges.isEmpty()) {
         b.append("<div class=\"card\"><p class=\"empty\">Aucun siège n'a encore eu lieu.</p></div>");
      } else {
         b.append("<div class=\"card\"><table class=\"tbl\"><thead><tr><th>Date</th><th>Assaillant</th><th>Défenseur</th><th>Vainqueur</th><th>Dégâts au Nexus</th></tr></thead><tbody>");
         for (Object[] r : sieges) {
            String w = String.valueOf(r[3]);
            String winner = w.equals("ATTACKER") ? "Assaillant" : w.equals("DEFENDER") ? "Défenseur" : "Aucun";
            b.append("<tr><td>").append(DAY.format(Instant.ofEpochMilli(((Number)r[0]).longValue()))).append("</td><td>").append(cell(r[1])).append("</td><td>").append(cell(r[2]))
               .append("</td><td>").append(winner).append("</td><td>").append(cell(r[4])).append("</td></tr>");
         }

         b.append("</tbody></table></div>");
      }

      b.append("<h2 style=\"margin:22px 0 10px\">🗳 Titres de la semaine dernière</h2>");
      List<Object[]> titles = db.select("SELECT t.cat, t.winner_name, t.votes, t.week FROM web_titles t WHERE t.week = (SELECT MAX(week) FROM web_titles) ORDER BY t.cat;", 4);
      if (titles.isEmpty()) {
         b.append("<div class=\"card\"><p class=\"empty\">Les premiers titres seront proclamés à la fin de la semaine (<a href=\"/votes\">voter</a>).</p></div>");
      } else {
         b.append("<div class=\"card\"><ul>");
         for (Object[] r : titles) {
            b.append("<li>").append(cell(WebVotes.labelOf(String.valueOf(r[0])))).append(" : <b>").append(cell(r[1])).append("</b> (").append(cell(r[2])).append(" vote(s))</li>");
         }

         b.append("</ul></div>");
      }

      return b.toString();
   }

   static final String VOTES_JS = """
      (function () {
        'use strict';
        var root = document.getElementById('va');
        if (!root) { return; }
        var en = /[?&]lang=en/.test(location.search);
        var T = en ? {
          loading: 'Loading…', claimTitle: 'Claim your username', claimHelp: 'Enter your Minecraft username. You will get a code to type in the game chat — that links this browser to your account.',
          username: 'Minecraft username', claim: 'Claim', code: 'Type this code in the Minecraft chat (within 10 minutes):', waiting: 'Waiting for you to type the code in the game…',
          hello: 'Signed in as', logout: 'Sign out', vote: 'Vote', pick: '— choose a player —', mine: 'Your vote:', nobody: 'No vote yet.', titles: 'Last week\\'s titles',
          error: 'Something went wrong.', votes: 'vote(s)', saved: 'Vote saved!', confirm: 'Now confirm the link in the game chat ([CONFIRMER]).'
        } : {
          loading: 'Chargement…', claimTitle: 'Revendiquez votre pseudo', claimHelp: 'Saisissez votre pseudo Minecraft. Un code vous sera donné : tapez-le dans le chat du jeu pour lier ce navigateur à votre compte.',
          username: 'Pseudo Minecraft', claim: 'Revendiquer', code: 'Tapez ce code dans le chat de Minecraft (dans les 10 minutes) :', waiting: 'En attente du code dans le jeu…',
          hello: 'Connecté en tant que', logout: 'Se déconnecter', vote: 'Voter', pick: '— choisir un joueur —', mine: 'Votre vote :', nobody: 'Aucun vote pour le moment.', titles: 'Titres de la semaine dernière',
          error: 'Une erreur est survenue.', votes: 'vote(s)', saved: 'Vote enregistré !', confirm: 'Confirmez maintenant la liaison dans le chat du jeu ([CONFIRMER]).'
        };
        function el(tag, cls, text) { var e = document.createElement(tag); if (cls) { e.className = cls; } if (text != null) { e.textContent = text; } return e; }
        function api(path, body) {
          var opt = { method: body ? 'POST' : 'GET', credentials: 'same-origin', headers: { 'X-Esicraft': '1', 'Content-Type': 'application/json' } };
          if (body) { opt.body = JSON.stringify(body); }
          return fetch(path, opt).then(function (r) { return r.json().then(function (j) { return { ok: r.ok, j: j }; }); });
        }
        var poll = null;
        function clear() { while (root.firstChild) { root.removeChild(root.firstChild); } }
        function load() {
          api('/api/votes').then(function (r) { render(r.j); }).catch(function () { clear(); root.appendChild(el('p', 'empty', T.error)); });
        }
        function claimBox(prefill) {
          var box = el('div', null);
          box.appendChild(el('h2', null, T.claimTitle));
          box.appendChild(el('p', null, T.claimHelp));
          var input = el('input', 'search'); input.type = 'text'; input.maxLength = 16; input.placeholder = T.username; input.value = prefill || '';
          input.setAttribute('aria-label', T.username);
          var btn = el('button', 'fb on', T.claim); btn.type = 'button';
          var out = el('div', null); out.style.marginTop = '12px';
          btn.addEventListener('click', function () {
            api('/api/claim/start', { name: input.value.trim() }).then(function (r) {
              while (out.firstChild) { out.removeChild(out.firstChild); }
              if (!r.ok) { out.appendChild(el('p', 'empty', r.j.error || T.error)); return; }
              out.appendChild(el('p', null, T.code));
              var code = el('div', 'chip acc', r.j.code); code.style.fontSize = '1.6rem'; code.style.letterSpacing = '.15em';
              out.appendChild(code);
              out.appendChild(el('p', 'empty', T.waiting));
              if (poll) { clearInterval(poll); }
              poll = setInterval(function () {
                api('/api/claim/status').then(function (s) {
                  if (s.j.linked) { clearInterval(poll); poll = null; load(); }
                  else if (s.j.confirm) { while (out.firstChild) { out.removeChild(out.firstChild); } out.appendChild(el('p', null, T.confirm)); }
                });
              }, 3000);
            });
          });
          box.appendChild(input); box.appendChild(document.createTextNode(' ')); box.appendChild(btn); box.appendChild(out);
          return box;
        }
        function render(d) {
          clear();
          var head = el('div', null);
          if (d.player) {
            head.appendChild(el('p', null, T.hello + ' ' + d.player + ' · '));
            var lo = el('button', 'fb', T.logout); lo.type = 'button';
            lo.addEventListener('click', function () { api('/api/logout', {}).then(load); });
            head.lastChild.appendChild(lo);
          }
          root.appendChild(head);
          if (!d.player) {
            var m = /[?&]claim=([A-Za-z0-9_]{3,16})/.exec(location.search);
            root.appendChild(claimBox(m ? m[1] : ''));
          }
          d.categories.forEach(function (c) {
            var sec = el('div', null); sec.style.marginTop = '22px';
            sec.appendChild(el('h2', null, c.label));
            if (c.top.length === 0) { sec.appendChild(el('p', 'empty', T.nobody)); }
            else {
              var ol = el('ol', null);
              c.top.forEach(function (t) { ol.appendChild(el('li', null, t.name + ' — ' + t.votes + ' ' + T.votes)); });
              sec.appendChild(ol);
            }
            if (d.player) {
              var sel = el('select', 'search'); sel.setAttribute('aria-label', c.label);
              var first = el('option', null, T.pick); first.value = ''; sel.appendChild(first);
              d.candidates.forEach(function (n) { if (n.toLowerCase() !== d.player.toLowerCase()) { var o = el('option', null, n); o.value = n; sel.appendChild(o); } });
              if (c.mine) { sel.value = c.mine; }
              var b = el('button', 'fb on', T.vote); b.type = 'button';
              b.addEventListener('click', function () {
                if (!sel.value) { return; }
                api('/api/vote', { cat: c.key, target: sel.value }).then(function (r) { if (r.ok) { render(r.j); } else { alert(r.j.error || T.error); } });
              });
              sec.appendChild(sel); sec.appendChild(document.createTextNode(' ')); sec.appendChild(b);
              if (c.mine) { sec.appendChild(el('p', 'empty', T.mine + ' ' + c.mine)); }
            }
            root.appendChild(sec);
          });
          if (d.titles && d.titles.length) {
            var ts = el('div', null); ts.style.marginTop = '26px';
            ts.appendChild(el('h2', null, T.titles));
            var ul = el('ul', null);
            d.titles.forEach(function (t) { ul.appendChild(el('li', null, t.label + ' : ' + t.name + ' (' + t.votes + ')')); });
            ts.appendChild(ul); root.appendChild(ts);
          }
        }
        root.appendChild(el('p', 'empty', T.loading));
        load();
      })();
      """;
}
