package fr.esicraft.web;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;
import fr.esicraft.EsicraftPlugin;
import fr.esicraft.data.PlayerData;
import fr.esicraft.guild.Guild;
import fr.esicraft.guild.GuildRole;
import fr.esicraft.treaty.Treaty;
import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;
import org.bukkit.metadata.MetadataValue;
import org.bukkit.scheduler.BukkitTask;

/**
 * Site web public en lecture seule (serveur HTTP intégré au JDK, aucune dépendance).
 * <ul>
 *   <li>Les pages sont rendues <b>une seule fois par instantané</b> (toutes les {@code web.refresh-seconds}) puis servies depuis la mémoire :
 *       une rafale de requêtes ne coûte presque rien et ne touche jamais au monde du jeu.</li>
 *   <li>Limite de requêtes par adresse IP, file d'attente bornée, délais d'expiration, GET/HEAD uniquement, en-têtes de sécurité.</li>
 *   <li>Aucune coordonnée (Nexus, bases, claims) ni adresse IP n'est exposée ; les joueurs « vanish » sont masqués.</li>
 * </ul>
 */
public class WebServer {
   private static final String HTML = "text/html; charset=utf-8";
   private static final String JSON = "application/json; charset=utf-8";
   private static final int MAX_PATH = 128;

   private record Page(int status, String type, byte[] body) {
   }

   private final EsicraftPlugin plugin;
   private volatile Map<String, Page> pages = Map.of();
   private volatile Page notFound = new Page(404, "text/plain; charset=utf-8", "Introuvable".getBytes(StandardCharsets.UTF_8));
   private final Map<String, long[]> hits = new ConcurrentHashMap<>();
   private volatile long lastErrorLog;
   private HttpServer server;
   private ThreadPoolExecutor pool;
   private BukkitTask refreshTask;
   private int rateLimit;
   private long rateWindowMs;

   public WebServer(EsicraftPlugin plugin) {
      this.plugin = plugin;
   }

   public void start() {
      if (!this.plugin.getConfig().getBoolean("web.enabled", true)) {
         return;
      }

      int port = this.plugin.getConfig().getInt("web.port", 80);
      String bind = this.plugin.getConfig().getString("web.bind", "0.0.0.0");
      this.rateLimit = Math.max(5, this.plugin.getConfig().getInt("web.rate-limit.requests", 60));
      this.rateWindowMs = Math.max(1L, this.plugin.getConfig().getLong("web.rate-limit.window-seconds", 10L)) * 1000L;
      // Délais d'expiration du serveur HTTP du JDK (lus une seule fois par la JVM : on ne remplace pas un réglage existant).
      setIfAbsent("sun.net.httpserver.maxReqTime", "10");
      setIfAbsent("sun.net.httpserver.maxRspTime", "30");
      setIfAbsent("sun.net.httpserver.maxReqHeaders", "64");
      this.render(WebSnapshot.empty());

      try {
         this.server = HttpServer.create(new InetSocketAddress(bind, port), 64);
         // 4 threads, file bornée ; si elle déborde, le thread d'acceptation traite lui-même (contre-pression, pas de croissance mémoire).
         this.pool = new ThreadPoolExecutor(2, 4, 30L, TimeUnit.SECONDS, new ArrayBlockingQueue<>(128), r -> {
            Thread t = new Thread(r, "esicraft-web");
            t.setDaemon(true);
            return t;
         }, new ThreadPoolExecutor.CallerRunsPolicy());
         this.server.setExecutor(this.pool);
         this.server.createContext("/", this::handle);
         this.server.start();
         this.plugin.getLogger().info("Site web démarré sur http://" + ("0.0.0.0".equals(bind) ? "<ip-du-serveur>" : bind) + (port == 80 ? "" : ":" + port) + "/");
      } catch (IOException | RuntimeException e) {
         this.server = null;
         if (this.pool != null) {
            this.pool.shutdownNow();
         }

         this.plugin.getLogger().log(Level.WARNING, "Impossible de démarrer le site web sur le port " + port + " : " + e.getMessage()
            + (port < 1024 ? " (sous Linux, un port < 1024 demande root ou 'setcap cap_net_bind_service' ; sinon changez web.port dans config.yml)" : ""));
         return;
      }

      long period = Math.max(5L, this.plugin.getConfig().getLong("web.refresh-seconds", 15L)) * 20L;
      this.refreshTask = Bukkit.getScheduler().runTaskTimer(this.plugin, this::capture, 40L, period);
   }

   private static void setIfAbsent(String key, String value) {
      if (System.getProperty(key) == null) {
         System.setProperty(key, value);
      }
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

   // ------------------------------------------------------------------ capture (thread principal) puis rendu (asynchrone)

   /** Joueur masqué par un plugin de vanish (convention « vanished » partagée par SuperVanish, Essentials, CMI…). */
   private static boolean isVanished(Player player) {
      for (MetadataValue value : player.getMetadata("vanished")) {
         if (value.asBoolean()) {
            return true;
         }
      }

      return false;
   }

   private void capture() {
      boolean showBank = this.plugin.getConfig().getBoolean("web.show-guild-banks", true);
      boolean showCoins = this.plugin.getConfig().getBoolean("web.show-balances", true);
      boolean showNexus = this.plugin.getConfig().getBoolean("web.show-nexus-health", false);
      var season = this.plugin.getSeasonManager();
      List<WebSnapshot.GuildView> guilds = new ArrayList<>();
      for (var entry : season.ranking()) {
         Guild g = entry.guild();
         List<WebSnapshot.MemberView> members = new ArrayList<>();
         for (Map.Entry<UUID, GuildRole> m : g.getMembers().entrySet()) {
            OfflinePlayer op = Bukkit.getOfflinePlayer(m.getKey());
            Player online = op.getPlayer();
            boolean visible = online != null && !isVanished(online);
            PlayerData pd = this.plugin.getDataManager().getPlayerData(m.getKey());
            members.add(new WebSnapshot.MemberView(op.getName() != null ? op.getName() : "?", HtmlPages.plain(m.getValue().getDisplayName()),
               pd != null && pd.getRace() != null ? HtmlPages.plain(pd.getRace().getDisplayName()) : null, visible));
         }

         members.sort((a, b) -> a.online() != b.online() ? (a.online() ? -1 : 1) : a.name().compareToIgnoreCase(b.name()));
         List<String> treaties = new ArrayList<>();
         for (Treaty t : this.plugin.getTreatyManager().treatiesOf(g.getId())) {
            Guild other = this.plugin.getGuildManager().getGuildById(t.other(g.getId()));
            treaties.add(t.getType().getLabel() + " avec " + (other != null ? HtmlPages.plain(other.getName()) : "?"));
         }

         guilds.add(new WebSnapshot.GuildView(g.getId(), HtmlPages.plain(g.getName()), HtmlPages.plain(g.getTag()),
            HtmlPages.plain(g.getSpecialization().getDisplayName()), HtmlPages.plain(g.getCurrentAge().getDisplayName()), g.getCurrentAge().getLevel(),
            members, g.getClaimedChunks().size(), showBank ? g.getBankBalance() : null, entry.score(), season.getRepelled(g.getId()),
            season.getConquests(g.getId()), showNexus && g.hasNexus() ? (int)Math.round(g.getNexus().getHealthPercentage()) : null, treaties,
            season.isChampion(g.getId())));
      }

      List<WebSnapshot.OnlineView> online = new ArrayList<>();
      for (Player p : Bukkit.getOnlinePlayers()) {
         if (isVanished(p)) {
            continue;
         }

         Guild g = this.plugin.getGuildManager().getGuildByPlayer(p.getUniqueId());
         PlayerData pd = this.plugin.getDataManager().getPlayerData(p.getUniqueId());
         online.add(new WebSnapshot.OnlineView(p.getName(), pd != null && pd.getRace() != null ? HtmlPages.plain(pd.getRace().getDisplayName()) : "Sans métier",
            g != null ? g.getId() : null, g != null ? HtmlPages.plain(g.getName()) : null, HtmlPages.plain(this.plugin.getAgeManager().getPlayerAge(p).getDisplayName()),
            showCoins && pd != null ? pd.getCoins() : null));
      }

      online.sort((a, b) -> a.name().compareToIgnoreCase(b.name()));
      String event = this.plugin.getEventManager().getActive() != null ? this.plugin.getEventManager().getActive().getLabel() : null;
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

            List<String> history = new ArrayList<>(this.plugin.getSeasonManager().getHistory(12));
            this.render(new WebSnapshot(System.currentTimeMillis(), seasonName, daysLeft, event, online.size(), max, guilds, online, rich, history));
         } catch (RuntimeException e) {
            this.plugin.getLogger().log(Level.WARNING, "Site web : échec du rafraîchissement", e);
         }
      });
      this.purgeHits();
   }

   /** Rend toutes les pages une fois pour toutes ; les requêtes ne font ensuite qu'une recherche dans une table. */
   private void render(WebSnapshot s) {
      String name = HtmlPages.plain(this.plugin.getConfig().getString("web.server-name", "Esicraft"));
      Map<String, Page> map = new HashMap<>();
      map.put("/", html(200, HtmlPages.page(name, "home", "Accueil", HtmlPages.home(name, s), s)));
      map.put("/guildes", html(200, HtmlPages.page(name, "guildes", "Guildes", HtmlPages.guilds(s), s)));
      map.put("/joueurs", html(200, HtmlPages.page(name, "joueurs", "Joueurs", HtmlPages.players(s), s)));
      map.put("/api/stats.json", new Page(200, JSON, HtmlPages.statsJson(s).getBytes(StandardCharsets.UTF_8)));
      for (WebSnapshot.GuildView g : s.guilds()) {
         map.put("/guilde/" + g.id(), html(200, HtmlPages.page(name, "guildes", g.name(), HtmlPages.guild(g), s)));
      }

      this.notFound = html(404, HtmlPages.page(name, "", "Introuvable", HtmlPages.notFound(), s));
      this.pages = Map.copyOf(map);
   }

   private static Page html(int status, String body) {
      return new Page(status, HTML, body.getBytes(StandardCharsets.UTF_8));
   }

   // ------------------------------------------------------------------ limite de débit

   private boolean allow(String ip) {
      long now = System.currentTimeMillis();
      long[] w = this.hits.computeIfAbsent(ip, k -> new long[]{now, 0L});
      synchronized (w) {
         if (now - w[0] >= this.rateWindowMs) {
            w[0] = now;
            w[1] = 0L;
         }

         return ++w[1] <= this.rateLimit;
      }
   }

   private void purgeHits() {
      long now = System.currentTimeMillis();
      this.hits.entrySet().removeIf(e -> now - e.getValue()[0] >= this.rateWindowMs);
      if (this.hits.size() > 50000) {
         this.hits.clear();
      }
   }

   // ------------------------------------------------------------------ requêtes

   private void handle(HttpExchange ex) throws IOException {
      try {
         String method = ex.getRequestMethod();
         InetSocketAddress remote = ex.getRemoteAddress();
         String ip = remote != null && remote.getAddress() != null ? remote.getAddress().getHostAddress() : "?";
         if (!this.allow(ip)) {
            ex.getResponseHeaders().set("Retry-After", String.valueOf(this.rateWindowMs / 1000L));
            this.send(ex, new Page(429, "text/plain; charset=utf-8", "Trop de requêtes, réessayez dans quelques secondes.".getBytes(StandardCharsets.UTF_8)), false);
            return;
         }

         if (!method.equals("GET") && !method.equals("HEAD")) {
            ex.getResponseHeaders().set("Allow", "GET, HEAD");
            this.send(ex, new Page(405, "text/plain; charset=utf-8", "Méthode non autorisée".getBytes(StandardCharsets.UTF_8)), false);
            return;
         }

         String path = ex.getRequestURI().getRawPath();
         if (path == null || path.length() > MAX_PATH) {
            this.send(ex, this.notFound, method.equals("HEAD"));
            return;
         }

         if (path.length() > 1 && path.endsWith("/")) {
            path = path.substring(0, path.length() - 1);
         }

         Page page = this.pages.get(path);
         if (page != null && page.type().equals(JSON)) {
            ex.getResponseHeaders().set("Access-Control-Allow-Origin", "*");
         }

         this.send(ex, page != null ? page : this.notFound, method.equals("HEAD"));
      } catch (IOException e) {
         // client parti en cours de route : rien à faire
      } catch (RuntimeException e) {
         long now = System.currentTimeMillis();
         if (now - this.lastErrorLog > 60000L) {
            this.lastErrorLog = now;
            this.plugin.getLogger().log(Level.WARNING, "Site web : erreur de requête", e);
         }
      } finally {
         ex.close();
      }
   }

   private void send(HttpExchange ex, Page page, boolean head) throws IOException {
      var h = ex.getResponseHeaders();
      h.set("Content-Type", page.type());
      h.set("X-Content-Type-Options", "nosniff");
      h.set("X-Frame-Options", "DENY");
      h.set("Referrer-Policy", "no-referrer");
      h.set("Permissions-Policy", "camera=(), microphone=(), geolocation=(), interest-cohort=()");
      h.set("Cross-Origin-Opener-Policy", "same-origin");
      h.set("Content-Security-Policy", "default-src 'none'; style-src 'unsafe-inline'; img-src 'self' data:; base-uri 'none'; form-action 'none'; frame-ancestors 'none'");
      h.set("Cache-Control", page.status() == 200 ? "public, max-age=10" : "no-store");
      if (head) {
         ex.sendResponseHeaders(page.status(), -1);
         return;
      }

      ex.sendResponseHeaders(page.status(), page.body().length);
      try (OutputStream os = ex.getResponseBody()) {
         os.write(page.body());
      }
   }
}
