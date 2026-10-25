package fr.esicraft.web;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;
import fr.esicraft.EsicraftPlugin;
import fr.esicraft.data.PlayerData;
import fr.esicraft.guild.Guild;
import fr.esicraft.treaty.Treaty;
import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.logging.Level;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitTask;

/**
 * Site web public en lecture seule (serveur HTTP intégré au JDK, aucune dépendance).
 * Les pages sont générées à partir d'un instantané rafraîchi périodiquement : aucun accès au monde depuis les threads HTTP.
 * Aucune coordonnée (Nexus, bases, claims) ni adresse IP n'est jamais exposée.
 */
public class WebServer {
   private final EsicraftPlugin plugin;
   private volatile WebSnapshot snapshot = WebSnapshot.empty();
   private HttpServer server;
   private ExecutorService pool;
   private BukkitTask refreshTask;

   public WebServer(EsicraftPlugin plugin) {
      this.plugin = plugin;
   }

   public void start() {
      if (!this.plugin.getConfig().getBoolean("web.enabled", true)) {
         return;
      }

      int port = this.plugin.getConfig().getInt("web.port", 80);
      String bind = this.plugin.getConfig().getString("web.bind", "0.0.0.0");
      try {
         System.setProperty("sun.net.httpserver.maxReqTime", "10");
         System.setProperty("sun.net.httpserver.maxRspTime", "30");
         this.server = HttpServer.create(new InetSocketAddress(bind, port), 64);
         this.pool = Executors.newFixedThreadPool(4, r -> {
            Thread t = new Thread(r, "esicraft-web");
            t.setDaemon(true);
            return t;
         });
         this.server.setExecutor(this.pool);
         this.server.createContext("/", this::handle);
         this.server.start();
         this.plugin.getLogger().info("Site web démarré sur http://" + ("0.0.0.0".equals(bind) ? "<ip-du-serveur>" : bind) + (port == 80 ? "" : ":" + port) + "/");
      } catch (IOException | RuntimeException e) {
         this.server = null;
         this.plugin.getLogger().log(Level.WARNING, "Impossible de démarrer le site web sur le port " + port + " : " + e.getMessage()
            + (port < 1024 ? " (sous Linux, un port < 1024 demande root ou 'setcap cap_net_bind_service'; sinon changez web.port dans config.yml)" : ""));
         return;
      }

      long period = Math.max(5L, this.plugin.getConfig().getLong("web.refresh-seconds", 15L)) * 20L;
      this.refreshTask = Bukkit.getScheduler().runTaskTimer(this.plugin, this::capture, 40L, period);
   }

   public void stop() {
      if (this.refreshTask != null) {
         this.refreshTask.cancel();
      }

      if (this.server != null) {
         this.server.stop(0);
      }

      if (this.pool != null) {
         this.pool.shutdownNow();
      }
   }

   // ------------------------------------------------------------------ capture (thread principal puis asynchrone)

   private void capture() {
      boolean showBank = this.plugin.getConfig().getBoolean("web.show-guild-banks", true);
      boolean showCoins = this.plugin.getConfig().getBoolean("web.show-balances", true);
      boolean showNexus = this.plugin.getConfig().getBoolean("web.show-nexus-health", false);
      var season = this.plugin.getSeasonManager();
      List<WebSnapshot.GuildView> guilds = new ArrayList<>();
      for (var entry : season.ranking()) {
         Guild g = entry.guild();
         List<WebSnapshot.MemberView> members = new ArrayList<>();
         for (Map.Entry<UUID, fr.esicraft.guild.GuildRole> m : g.getMembers().entrySet()) {
            OfflinePlayer op = Bukkit.getOfflinePlayer(m.getKey());
            PlayerData pd = this.plugin.getDataManager().getPlayerData(m.getKey());
            String name = op.getName() != null ? op.getName() : "?";
            members.add(new WebSnapshot.MemberView(name, HtmlPages.plain(m.getValue().getDisplayName()),
               pd != null && pd.getRace() != null ? HtmlPages.plain(pd.getRace().getDisplayName()) : null, op.isOnline()));
         }

         members.sort((a, b) -> Boolean.compare(b.online(), a.online()));
         List<String> treaties = new ArrayList<>();
         for (Treaty t : this.plugin.getTreatyManager().treatiesOf(g.getId())) {
            Guild other = this.plugin.getGuildManager().getGuildById(t.other(g.getId()));
            treaties.add(t.getType().getLabel() + " avec " + (other != null ? other.getName() : "?"));
         }

         guilds.add(new WebSnapshot.GuildView(g.getId(), g.getName(), g.getTag(), HtmlPages.plain(g.getSpecialization().getDisplayName()),
            HtmlPages.plain(g.getCurrentAge().getDisplayName()), g.getCurrentAge().getLevel(), members, g.getClaimedChunks().size(),
            showBank ? g.getBankBalance() : null, entry.score(), season.getRepelled(g.getId()), season.getConquests(g.getId()),
            showNexus && g.hasNexus() ? (int)Math.round(g.getNexus().getHealthPercentage()) : null, treaties, season.isChampion(g.getId())));
      }

      List<WebSnapshot.OnlineView> online = new ArrayList<>();
      for (Player p : Bukkit.getOnlinePlayers()) {
         Guild g = this.plugin.getGuildManager().getGuildByPlayer(p.getUniqueId());
         PlayerData pd = this.plugin.getDataManager().getPlayerData(p.getUniqueId());
         online.add(new WebSnapshot.OnlineView(p.getName(), pd != null && pd.getRace() != null ? HtmlPages.plain(pd.getRace().getDisplayName()) : "-",
            g != null ? g.getId() : null, g != null ? g.getName() : null, HtmlPages.plain(this.plugin.getAgeManager().getPlayerAge(p).getDisplayName()),
            showCoins && pd != null ? pd.getCoins() : null));
      }

      online.sort((a, b) -> a.name().compareToIgnoreCase(b.name()));
      String event = this.plugin.getEventManager().getActive() != null ? this.plugin.getEventManager().getActive().getLabel() : null;
      int count = online.size();
      int max = Bukkit.getMaxPlayers();
      String seasonName = fr.esicraft.fun.SeasonManager.currentSeason();
      long daysLeft = fr.esicraft.fun.SeasonManager.daysLeft();
      Bukkit.getScheduler().runTaskAsynchronously(this.plugin, () -> {
         try {
            List<WebSnapshot.RichView> rich = new ArrayList<>();
            if (showCoins) {
               for (Map.Entry<String, Double> e : this.plugin.getDataManager().getTopBalances(10)) {
                  rich.add(new WebSnapshot.RichView(e.getKey(), e.getValue()));
               }
            }

            List<String> history = new ArrayList<>(this.plugin.getSeasonManager().getHistory(15));
            this.snapshot = new WebSnapshot(System.currentTimeMillis(), seasonName, daysLeft, event, count, max, guilds, online, rich, history);
         } catch (RuntimeException e) {
            this.plugin.getLogger().log(Level.WARNING, "Site web : échec du rafraîchissement", e);
         }
      });
   }

   // ------------------------------------------------------------------ requêtes

   private void handle(HttpExchange ex) throws IOException {
      try {
         String method = ex.getRequestMethod();
         if (!method.equals("GET") && !method.equals("HEAD")) {
            ex.getResponseHeaders().set("Allow", "GET, HEAD");
            this.send(ex, 405, "text/plain; charset=utf-8", "Méthode non autorisée", false);
            return;
         }

         WebSnapshot s = this.snapshot;
         String name = this.plugin.getConfig().getString("web.server-name", "Esicraft");
         String path = ex.getRequestURI().getPath();
         if (path.length() > 1 && path.endsWith("/")) {
            path = path.substring(0, path.length() - 1);
         }

         boolean head = method.equals("HEAD");
         if (path.equals("/")) {
            this.send(ex, 200, "text/html; charset=utf-8", HtmlPages.page(name, "Accueil", HtmlPages.home(name, s), s), head);
         } else if (path.equals("/guildes")) {
            this.send(ex, 200, "text/html; charset=utf-8", HtmlPages.page(name, "Guildes", HtmlPages.guilds(s), s), head);
         } else if (path.equals("/joueurs")) {
            this.send(ex, 200, "text/html; charset=utf-8", HtmlPages.page(name, "Joueurs", HtmlPages.players(s), s), head);
         } else if (path.startsWith("/guilde/")) {
            WebSnapshot.GuildView g = s.guild(path.substring("/guilde/".length()));
            if (g == null) {
               this.send(ex, 404, "text/html; charset=utf-8", HtmlPages.page(name, "Introuvable", HtmlPages.notFound(), s), head);
            } else {
               this.send(ex, 200, "text/html; charset=utf-8", HtmlPages.page(name, g.name(), HtmlPages.guild(g), s), head);
            }
         } else if (path.equals("/api/stats.json")) {
            ex.getResponseHeaders().set("Access-Control-Allow-Origin", "*");
            this.send(ex, 200, "application/json; charset=utf-8", HtmlPages.statsJson(s), head);
         } else {
            this.send(ex, 404, "text/html; charset=utf-8", HtmlPages.page(name, "Introuvable", HtmlPages.notFound(), s), head);
         }
      } catch (RuntimeException e) {
         this.plugin.getLogger().log(Level.WARNING, "Site web : erreur de requête", e);
         this.send(ex, 500, "text/plain; charset=utf-8", "Erreur interne", false);
      } finally {
         ex.close();
      }
   }

   private void send(HttpExchange ex, int status, String type, String body, boolean head) throws IOException {
      byte[] bytes = body.getBytes(StandardCharsets.UTF_8);
      var h = ex.getResponseHeaders();
      h.set("Content-Type", type);
      h.set("X-Content-Type-Options", "nosniff");
      h.set("X-Frame-Options", "DENY");
      h.set("Referrer-Policy", "no-referrer");
      h.set("Content-Security-Policy", "default-src 'none'; style-src 'unsafe-inline'; base-uri 'none'; form-action 'none'");
      h.set("Cache-Control", "public, max-age=10");
      ex.sendResponseHeaders(status, head ? -1 : bytes.length);
      if (!head) {
         try (OutputStream os = ex.getResponseBody()) {
            os.write(bytes);
         }
      }
   }
}
