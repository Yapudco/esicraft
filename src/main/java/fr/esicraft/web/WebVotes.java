package fr.esicraft.web;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.sun.net.httpserver.HttpExchange;
import fr.esicraft.EsicraftPlugin;
import fr.esicraft.util.ChatUtil;
import io.papermc.paper.event.player.AsyncChatEvent;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.LocalDate;
import java.time.temporal.IsoFields;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.event.ClickEvent;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerQuitEvent;
import org.jetbrains.annotations.NotNull;

/**
 * Élections hebdomadaires en ligne (désactivables, catégories réglables dans {@code web.votes.*}). Pour voter, un visiteur revendique son pseudo :
 * le site lui donne un code à taper dans le chat du jeu, <b>le joueur doit ensuite confirmer</b> (avec l'adresse approximative du demandeur, contre
 * l'hameçonnage), et le navigateur est reconnu par un cookie (jeton aléatoire, HttpOnly). Un joueur peut refuser d'être candidat ({@code /webvote optout}).
 */
public class WebVotes implements Listener, CommandExecutor {
   public record Resp(int status, String json, String setCookie) {
   }

   private record Pending(String token, UUID uuid, String name, long expires, String ipHint) {
   }

   public static final String COOKIE = "esi_sid";
   private static final String[][] DEFAULT_CATEGORIES = {
      {"debile", "🤪 Le joueur le plus débile"}, {"gros", "🍔 Le joueur le plus gros"}, {"drole", "😂 Le joueur le plus drôle"}, {"chiant", "😤 Le joueur le plus chiant"}};
   private static volatile String[][] categories = DEFAULT_CATEGORIES;
   private static final String CODE_CHARS = "ABCDEFGHJKMNPQRSTUVWXYZ23456789";
   private static final SecureRandom RANDOM = new SecureRandom();
   private static final Gson GSON = new Gson();

   private final EsicraftPlugin plugin;
   private final Map<String, UUID> sessions = new ConcurrentHashMap<>();
   private final Map<String, Pending> pendingByCode = new ConcurrentHashMap<>();
   private final Map<String, String> codeByToken = new ConcurrentHashMap<>();
   /** Code tapé en jeu, en attente de la confirmation du joueur (joueur → demande). */
   private final Map<UUID, Pending> awaitingConfirm = new ConcurrentHashMap<>();
   private final Map<String, long[]> claimRate = new ConcurrentHashMap<>();
   private final java.util.Set<UUID> optedOut = ConcurrentHashMap.newKeySet();
   /** Instantané partagé (classements, candidats, titres) gardé quelques secondes : l'API n'est pas authentifiée et ne doit pas multiplier les requêtes. */
   private volatile List<Map<String, Object>> cachedCats;
   private volatile long cachedCatsAt;
   private volatile List<String> cachedCandidates;
   private volatile long cachedCandidatesAt;

   public WebVotes(EsicraftPlugin plugin) {
      this.plugin = plugin;
   }

   public boolean enabled() {
      return this.plugin.getConfig().getBoolean("web.votes.enabled", true);
   }

   public void init() {
      var db = this.plugin.getDataManager().getStorage();
      db.exec("CREATE TABLE IF NOT EXISTS web_sessions (token VARCHAR(64) PRIMARY KEY, uuid VARCHAR(36), created BIGINT);");
      db.exec("CREATE TABLE IF NOT EXISTS web_ballots (week VARCHAR(10), cat VARCHAR(16), voter VARCHAR(36), target VARCHAR(36), iphash VARCHAR(20), PRIMARY KEY (week, cat, voter));");
      db.exec("CREATE INDEX IF NOT EXISTS idx_ballots_target ON web_ballots (week, cat, target);");
      db.exec("DROP TABLE IF EXISTS web_votes;");
      db.exec("CREATE TABLE IF NOT EXISTS web_titles (week VARCHAR(10), cat VARCHAR(16), winner VARCHAR(36), winner_name VARCHAR(40), votes INT, PRIMARY KEY (week, cat));");
      db.exec("CREATE TABLE IF NOT EXISTS web_optout (uuid VARCHAR(36) PRIMARY KEY);");
      long ttl = this.sessionTtlMs();
      db.exec("DELETE FROM web_sessions WHERE created < ?;", System.currentTimeMillis() - ttl);
      for (Object[] row : db.select("SELECT token, uuid FROM web_sessions;", 2)) {
         try {
            this.sessions.put(String.valueOf(row[0]), UUID.fromString(String.valueOf(row[1])));
         } catch (IllegalArgumentException ignored) {
         }
      }

      for (Object[] row : db.select("SELECT uuid FROM web_optout;", 1)) {
         try {
            this.optedOut.add(UUID.fromString(String.valueOf(row[0])));
         } catch (IllegalArgumentException ignored) {
         }
      }

      List<String[]> cats = new ArrayList<>();
      for (String s : this.plugin.getConfig().getStringList("web.votes.categories")) {
         String[] parts = s.split("\\|", 2);
         if (parts.length == 2 && parts[0].matches("[a-z0-9]{1,16}") && !parts[1].isBlank()) {
            cats.add(new String[]{parts[0], parts[1].trim()});
         }
      }

      categories = cats.isEmpty() ? DEFAULT_CATEGORIES : cats.toArray(new String[0][]);
      Bukkit.getPluginManager().registerEvents(this, this.plugin);
      Bukkit.getScheduler().runTaskTimerAsynchronously(this.plugin, this::rollover, 600L, 6000L);
   }

   public void clear() {
      this.pendingByCode.clear();
      this.codeByToken.clear();
      this.awaitingConfirm.clear();
      this.cachedCats = null;
   }

   private long sessionTtlMs() {
      return Math.max(1L, this.plugin.getConfig().getLong("web.votes.session-days", 90L)) * 86400000L;
   }

   private static String week(LocalDate d) {
      return d.get(IsoFields.WEEK_BASED_YEAR) + "-W" + String.format("%02d", d.get(IsoFields.WEEK_OF_WEEK_BASED_YEAR));
   }

   private static String currentWeek() {
      return week(LocalDate.now());
   }

   public static String labelOf(String cat) {
      for (String[] c : categories) {
         if (c[0].equals(cat)) {
            return c[1];
         }
      }

      return cat;
   }

   // ------------------------------------------------------------------ revendication du pseudo : code dans le chat, puis confirmation en jeu

   @EventHandler(priority = EventPriority.LOWEST)
   public void onChat(AsyncChatEvent event) {
      if (this.pendingByCode.isEmpty()) {
         return;
      }

      String text = PlainTextComponentSerializer.plainText().serialize(event.message()).trim().toUpperCase(Locale.ROOT);
      Pending p = this.pendingByCode.get(text);
      if (p == null) {
         return;
      }

      if (p.expires() < System.currentTimeMillis()) {
         this.pendingByCode.remove(text);
         this.codeByToken.remove(p.token(), text);
         return;
      }

      if (!p.uuid().equals(event.getPlayer().getUniqueId())) {
         return;
      }

      // Le code ne s'affiche pas dans le chat public ; le joueur doit encore confirmer en connaissance de cause.
      event.setCancelled(true);
      this.pendingByCode.remove(text);
      this.codeByToken.remove(p.token(), text);
      this.awaitingConfirm.put(p.uuid(), p);
      Player player = event.getPlayer();
      Bukkit.getScheduler().runTask(this.plugin, () -> {
         ChatUtil.sendMessage(player, "&6🗳 Une demande de liaison du site web a été faite pour votre compte depuis &e" + p.ipHint() + "&6. &cSi vous ne l'avez PAS faite vous-même (quelqu'un vous a demandé de taper un code), refusez !");
         player.sendMessage(Component.text("   ", NamedTextColor.GRAY)
            .append(Component.text("[CONFIRMER]", NamedTextColor.GREEN).clickEvent(ClickEvent.runCommand("/webvote confirmer")))
            .append(Component.text("   "))
            .append(Component.text("[REFUSER]", NamedTextColor.RED).clickEvent(ClickEvent.runCommand("/webvote refuser"))));
      });
   }

   @EventHandler
   public void onQuit(PlayerQuitEvent event) {
      this.awaitingConfirm.remove(event.getPlayer().getUniqueId());
   }

   public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
      if (!(sender instanceof Player player)) {
         sender.sendMessage(ChatUtil.color("&cSeul un joueur peut exécuter cette commande."));
         return true;
      }

      String sub = args.length == 0 ? "" : args[0].toLowerCase(Locale.ROOT);
      switch (sub) {
         case "confirmer", "confirm" -> {
            Pending p = this.awaitingConfirm.remove(player.getUniqueId());
            if (p == null || p.expires() + 600000L < System.currentTimeMillis()) {
               ChatUtil.sendMessage(player, "&cAucune demande de liaison en attente.");
               return true;
            }

            this.sessions.put(p.token(), p.uuid());
            Bukkit.getScheduler().runTaskAsynchronously(this.plugin, () -> this.plugin.getDataManager().getStorage().exec(
               "INSERT OR REPLACE INTO web_sessions (token, uuid, created) VALUES (?, ?, ?);", p.token(), p.uuid().toString(), System.currentTimeMillis()));
            ChatUtil.sendMessage(player, "&a✔ Votre navigateur est lié à votre compte &e" + player.getName() + "&a : vous pouvez voter sur le site !");
         }
         case "refuser", "deny" -> {
            ChatUtil.sendMessage(player, this.awaitingConfirm.remove(player.getUniqueId()) != null ? "&7Demande de liaison refusée." : "&cAucune demande en attente.");
         }
         case "optout" -> this.setOptOut(player, true);
         case "optin" -> this.setOptOut(player, false);
         default -> ChatUtil.sendMessage(player, "&6🗳 &e/webvote optout &7: ne plus apparaître dans les élections du site · &e/webvote optin &7: y revenir.");
      }

      return true;
   }

   private void setOptOut(Player player, boolean out) {
      UUID id = player.getUniqueId();
      if (out) {
         this.optedOut.add(id);
      } else {
         this.optedOut.remove(id);
      }

      this.cachedCandidates = null;
      this.cachedCats = null;
      Bukkit.getScheduler().runTaskAsynchronously(this.plugin, () -> {
         var db = this.plugin.getDataManager().getStorage();
         if (out) {
            db.exec("INSERT OR IGNORE INTO web_optout (uuid) VALUES (?);", id.toString());
         } else {
            db.exec("DELETE FROM web_optout WHERE uuid = ?;", id.toString());
         }
      });
      ChatUtil.sendMessage(player, out ? "&7Vous n'apparaissez plus dans les élections du site (ni comme candidat, ni dans les classements)." : "&aVous pouvez de nouveau être élu(e) sur le site.");
   }

   // ------------------------------------------------------------------ API HTTP

   private static String newToken() {
      byte[] raw = new byte[24];
      RANDOM.nextBytes(raw);
      StringBuilder sb = new StringBuilder();
      for (byte b : raw) {
         sb.append(String.format("%02x", b));
      }

      return sb.toString();
   }

   private static String tokenOf(HttpExchange ex) {
      List<String> cookies = ex.getRequestHeaders().get("Cookie");
      if (cookies == null) {
         return null;
      }

      for (String header : cookies) {
         for (String part : header.split(";")) {
            String s = part.trim();
            if (s.startsWith(COOKIE + "=")) {
               String v = s.substring(COOKIE.length() + 1);
               return v.matches("[0-9a-f]{48}") ? v : null;
            }
         }
      }

      return null;
   }

   private String cookieHeader(String token, long maxAgeSeconds, boolean https) {
      return COOKIE + "=" + token + "; Path=/; Max-Age=" + maxAgeSeconds + "; HttpOnly; SameSite=Lax" + (https ? "; Secure" : "");
   }

   private static Resp json(int status, Object body) {
      return new Resp(status, GSON.toJson(body), null);
   }

   private static Resp error(int status, String message) {
      Map<String, Object> m = new LinkedHashMap<>();
      m.put("error", message);
      return json(status, m);
   }

   private boolean claimAllowed(String ip) {
      long now = System.currentTimeMillis();
      long[] w = this.claimRate.computeIfAbsent(ip, k -> new long[]{now, 0L});
      synchronized (w) {
         if (now - w[0] > 60000L) {
            w[0] = now;
            w[1] = 0L;
         }

         return ++w[1] <= 8L;
      }
   }

   private static String hashIp(String ip) {
      try {
         byte[] d = MessageDigest.getInstance("SHA-256").digest(("esicraft-votes|" + ip).getBytes(StandardCharsets.UTF_8));
         StringBuilder sb = new StringBuilder();
         for (int i = 0; i < 8; i++) {
            sb.append(String.format("%02x", d[i]));
         }

         return sb.toString();
      } catch (NoSuchAlgorithmException e) {
         return Integer.toHexString(ip.hashCode());
      }
   }

   /** Adresse approximative affichée au joueur qui confirme (les derniers octets sont masqués). */
   private static String maskIp(String clientKey) {
      if (clientKey.startsWith("v6:")) {
         return clientKey.substring(3, Math.min(clientKey.length(), 11)) + ":…";
      }

      String[] p = clientKey.split("\\.");
      return p.length == 4 ? p[0] + "." + p[1] + ".x.x" : "adresse inconnue";
   }

   private String nameOf(UUID uuid) {
      var rows = this.plugin.getDataManager().getStorage().select("SELECT name FROM players WHERE uuid = ?;", 1, uuid.toString());
      return rows.isEmpty() ? "?" : String.valueOf(rows.get(0)[0]);
   }

   private Object[] findPlayer(String name) {
      if (name == null || !name.matches("[A-Za-z0-9_]{3,16}")) {
         return null;
      }

      var rows = this.plugin.getDataManager().getStorage().select("SELECT uuid, name FROM players WHERE lower(name) = lower(?);", 2, name);
      return rows.isEmpty() ? null : rows.get(0);
   }

   /** Traite /api/me, /api/votes, /api/claim/*, /api/vote et /api/logout ; renvoie null si le chemin n'est pas un chemin d'élection. */
   public Resp handle(HttpExchange ex, String method, String path, String ip, boolean trustedProxy) throws IOException {
      boolean https = trustedProxy && "https".equalsIgnoreCase(ex.getRequestHeaders().getFirst("X-Forwarded-Proto"));
      if (!this.enabled()) {
         return error(403, "Les élections sont désactivées.");
      }

      String token = tokenOf(ex);
      UUID me = token == null ? null : this.sessions.get(token);
      boolean post = method.equals("POST");
      if (post) {
         // Protection CSRF : requête fabriquée par le site lui-même (en-tête personnalisé, donc soumis à CORS) et origine cohérente.
         String origin = ex.getRequestHeaders().getFirst("Origin");
         String host = ex.getRequestHeaders().getFirst("Host");
         if (!"1".equals(ex.getRequestHeaders().getFirst("X-Esicraft")) || origin != null && host != null && !origin.endsWith("://" + host)) {
            return error(403, "Requête refusée.");
         }
      }

      switch (path) {
         case "/api/me" -> {
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("player", me == null ? null : this.nameOf(me));
            return json(200, m);
         }
         case "/api/votes" -> {
            return json(200, this.snapshot(me));
         }
         case "/api/claim/status" -> {
            Map<String, Object> m = new LinkedHashMap<>();
            if (me != null) {
               m.put("linked", true);
               m.put("player", this.nameOf(me));
            } else {
               String code = token == null ? null : this.codeByToken.get(token);
               Pending p = code == null ? null : this.pendingByCode.get(code);
               if (p != null && p.expires() > System.currentTimeMillis()) {
                  m.put("pending", true);
                  m.put("code", code);
                  m.put("player", p.name());
                  m.put("seconds", (p.expires() - System.currentTimeMillis()) / 1000L);
               } else if (token != null && this.awaitingConfirm.values().stream().anyMatch(a -> a.token().equals(token))) {
                  m.put("confirm", true);
               }
            }

            return json(200, m);
         }
         case "/api/claim/start" -> {
            if (!post) {
               return error(405, "POST requis.");
            }

            if (!this.claimAllowed(ip)) {
               return error(429, "Trop de tentatives, patientez une minute.");
            }

            JsonObject body = this.readBody(ex);
            Object[] player = body == null ? null : this.findPlayer(body.has("name") ? body.get("name").getAsString() : null);
            if (player == null) {
               return error(404, "Pseudo inconnu (le joueur doit s'être connecté au moins une fois).");
            }

            String t = token != null ? token : newToken();
            String old = this.codeByToken.remove(t);
            if (old != null) {
               this.pendingByCode.remove(old);
            }

            String code = "ESI-" + randomCode(4);
            long expires = System.currentTimeMillis() + 10L * 60000L;
            this.pendingByCode.put(code, new Pending(t, UUID.fromString(String.valueOf(player[0])), String.valueOf(player[1]), expires, maskIp(ip)));
            this.codeByToken.put(t, code);
            if (this.pendingByCode.size() > 2000) {
               long now = System.currentTimeMillis();
               this.pendingByCode.entrySet().removeIf(e -> e.getValue().expires() < now);
            }

            Map<String, Object> m = new LinkedHashMap<>();
            m.put("code", code);
            m.put("player", String.valueOf(player[1]));
            m.put("seconds", 600);
            return new Resp(200, GSON.toJson(m), this.cookieHeader(t, this.sessionTtlMs() / 1000L, https));
         }
         case "/api/vote" -> {
            if (!post) {
               return error(405, "POST requis.");
            }

            if (me == null) {
               return error(401, "Revendiquez d'abord votre pseudo.");
            }

            JsonObject body = this.readBody(ex);
            String cat = body != null && body.has("cat") ? body.get("cat").getAsString() : "";
            Object[] target = body == null ? null : this.findPlayer(body.has("target") ? body.get("target").getAsString() : null);
            boolean validCat = false;
            for (String[] c : categories) {
               validCat |= c[0].equals(cat);
            }

            if (!validCat || target == null) {
               return error(400, "Vote invalide.");
            }

            UUID targetId = UUID.fromString(String.valueOf(target[0]));
            if (targetId.equals(me)) {
               return error(400, "On ne vote pas pour soi-même !");
            }

            if (this.optedOut.contains(targetId)) {
               return error(400, "Ce joueur ne souhaite pas apparaître dans les élections.");
            }

            long minAge = this.plugin.getConfig().getLong("web.votes.min-account-days", 2L) * 86400000L;
            long first = Bukkit.getOfflinePlayer(me).getFirstPlayed();
            if (first > 0L && System.currentTimeMillis() - first < minAge) {
               return error(403, "Votre compte est trop récent pour voter.");
            }

            String week = currentWeek();
            String iphash = hashIp(ip);
            var db = this.plugin.getDataManager().getStorage();
            // Un seul compte par connexion et par catégorie : empêche de bourrer l'urne avec des comptes jetables.
            if (!db.select("SELECT 1 FROM web_ballots WHERE week = ? AND cat = ? AND iphash = ? AND voter <> ? LIMIT 1;", 1, week, cat, iphash, me.toString()).isEmpty()) {
               return error(403, "Un autre compte a déjà voté dans cette catégorie depuis cette connexion.");
            }

            db.exec("INSERT OR REPLACE INTO web_ballots (week, cat, voter, target, iphash) VALUES (?, ?, ?, ?, ?);", week, cat, me.toString(), targetId.toString(), iphash);
            this.cachedCats = null;
            return json(200, this.snapshot(me));
         }
         case "/api/logout" -> {
            if (!post) {
               return error(405, "POST requis.");
            }

            if (token != null) {
               this.sessions.remove(token);
               this.plugin.getDataManager().getStorage().exec("DELETE FROM web_sessions WHERE token = ?;", token);
            }

            return new Resp(200, "{\"ok\":true}", this.cookieHeader("deleted", 0L, https));
         }
         default -> {
            return null;
         }
      }
   }

   private static String randomCode(int n) {
      StringBuilder sb = new StringBuilder();
      for (int i = 0; i < n; i++) {
         sb.append(CODE_CHARS.charAt(RANDOM.nextInt(CODE_CHARS.length())));
      }

      return sb.toString();
   }

   private JsonObject readBody(HttpExchange ex) throws IOException {
      byte[] raw = ex.getRequestBody().readNBytes(2049);
      if (raw.length > 2048) {
         return null;
      }

      try {
         var el = JsonParser.parseString(new String(raw, StandardCharsets.UTF_8));
         return el.isJsonObject() ? el.getAsJsonObject() : null;
      } catch (RuntimeException e) {
         return null;
      }
   }

   // ------------------------------------------------------------------ données

   private List<Map<String, Object>> sharedCategories() {
      long now = System.currentTimeMillis();
      List<Map<String, Object>> cats = this.cachedCats;
      if (cats != null && now - this.cachedCatsAt < 8000L) {
         return cats;
      }

      var db = this.plugin.getDataManager().getStorage();
      String week = currentWeek();
      List<Map<String, Object>> built = new ArrayList<>();
      for (String[] c : categories) {
         Map<String, Object> cat = new LinkedHashMap<>();
         cat.put("key", c[0]);
         cat.put("label", c[1]);
         List<Map<String, Object>> top = new ArrayList<>();
         for (Object[] row : db.select("SELECT p.name, p.uuid, COUNT(*) AS n FROM web_ballots v JOIN players p ON p.uuid = v.target WHERE v.week = ? AND v.cat = ? GROUP BY v.target ORDER BY n DESC, p.name LIMIT 8;", 3, week, c[0])) {
            if (!this.optedOut.contains(UUID.fromString(String.valueOf(row[1]))) && top.size() < 5) {
               Map<String, Object> t = new LinkedHashMap<>();
               t.put("name", String.valueOf(row[0]));
               t.put("votes", ((Number)row[2]).intValue());
               top.add(t);
            }
         }

         cat.put("top", top);
         built.add(cat);
      }

      this.cachedCats = built;
      this.cachedCatsAt = now;
      return built;
   }

   private List<String> candidates() {
      long now = System.currentTimeMillis();
      List<String> names = this.cachedCandidates;
      if (names != null && now - this.cachedCandidatesAt < 60000L) {
         return names;
      }

      List<String> list = new ArrayList<>();
      for (Object[] row : this.plugin.getDataManager().getStorage().select("SELECT name, uuid FROM players WHERE name IS NOT NULL ORDER BY lower(name) LIMIT 700;", 2)) {
         try {
            if (!this.optedOut.contains(UUID.fromString(String.valueOf(row[1]))) && list.size() < 600) {
               list.add(String.valueOf(row[0]));
            }
         } catch (IllegalArgumentException ignored) {
         }
      }

      this.cachedCandidates = list;
      this.cachedCandidatesAt = now;
      return list;
   }

   private Map<String, Object> snapshot(UUID me) {
      String week = currentWeek();
      Map<String, Object> out = new LinkedHashMap<>();
      out.put("week", week);
      out.put("player", me == null ? null : this.nameOf(me));
      Map<String, String> mine = new HashMap<>();
      if (me != null) {
         for (Object[] row : this.plugin.getDataManager().getStorage().select("SELECT v.cat, p.name FROM web_ballots v JOIN players p ON p.uuid = v.target WHERE v.week = ? AND v.voter = ?;", 2, week, me.toString())) {
            mine.put(String.valueOf(row[0]), String.valueOf(row[1]));
         }
      }

      List<Map<String, Object>> cats = new ArrayList<>();
      for (Map<String, Object> shared : this.sharedCategories()) {
         Map<String, Object> cat = new LinkedHashMap<>(shared);
         cat.put("mine", mine.get(String.valueOf(shared.get("key"))));
         cats.add(cat);
      }

      out.put("categories", cats);
      out.put("candidates", this.candidates());
      out.put("titles", this.lastTitles());
      return out;
   }

   /** Titres de la dernière semaine dépouillée. */
   public List<Map<String, Object>> lastTitles() {
      var db = this.plugin.getDataManager().getStorage();
      List<Map<String, Object>> list = new ArrayList<>();
      var weeks = db.select("SELECT week FROM web_titles ORDER BY week DESC LIMIT 1;", 1);
      if (weeks.isEmpty()) {
         return list;
      }

      String w = String.valueOf(weeks.get(0)[0]);
      for (Object[] row : db.select("SELECT cat, winner_name, votes FROM web_titles WHERE week = ?;", 3, w)) {
         Map<String, Object> m = new LinkedHashMap<>();
         m.put("week", w);
         m.put("label", labelOf(String.valueOf(row[0])));
         m.put("name", String.valueOf(row[1]));
         m.put("votes", ((Number)row[2]).intValue());
         list.add(m);
      }

      return list;
   }

   /** Dépouille les semaines terminées : proclame et grave les vainqueurs. */
   private void rollover() {
      try {
         var db = this.plugin.getDataManager().getStorage();
         String current = currentWeek();
         var weeks = db.select("SELECT DISTINCT week FROM web_ballots WHERE week < ? ORDER BY week;", 1, current);
         for (Object[] w : weeks) {
            String week = String.valueOf(w[0]);
            if (!db.select("SELECT 1 FROM web_titles WHERE week = ? LIMIT 1;", 1, week).isEmpty()) {
               continue;
            }

            List<String> lines = new ArrayList<>();
            for (String[] c : categories) {
               for (Object[] r : db.select("SELECT p.uuid, p.name, COUNT(*) AS n FROM web_ballots v JOIN players p ON p.uuid = v.target WHERE v.week = ? AND v.cat = ? GROUP BY v.target ORDER BY n DESC, p.name LIMIT 5;", 3, week, c[0])) {
                  if (this.optedOut.contains(UUID.fromString(String.valueOf(r[0])))) {
                     continue;
                  }

                  db.exec("INSERT OR REPLACE INTO web_titles (week, cat, winner, winner_name, votes) VALUES (?, ?, ?, ?, ?);", week, c[0], String.valueOf(r[0]), String.valueOf(r[1]), ((Number)r[2]).intValue());
                  lines.add(c[1] + " : &e" + r[1] + " &7(" + r[2] + " vote(s))");
                  break;
               }
            }

            if (!lines.isEmpty()) {
               Bukkit.getScheduler().runTask(this.plugin, () -> {
                  ChatUtil.broadcast("&6🗳 &e&lÉlections de la semaine &7(" + week + ")");
                  for (String l : lines) {
                     ChatUtil.broadcast("  &7" + l);
                  }

                  this.plugin.getSeasonManager().addHistory(fr.esicraft.life.Chronicle.Kind.EVENT, "🗳 Élections hebdomadaires du site : " + String.join(" · ", lines).replaceAll("&.", "") + ".", null, null);
               });
            }
         }
      } catch (RuntimeException e) {
         this.plugin.getLogger().warning("Dépouillement des élections impossible : " + e.getMessage());
      }
   }
}
