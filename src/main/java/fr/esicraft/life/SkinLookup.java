package fr.esicraft.life;

import com.destroystokyo.paper.profile.ProfileProperty;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import fr.esicraft.EsicraftPlugin;
import java.io.IOException;
import java.net.URI;
import java.net.URL;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Base64;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import org.bukkit.entity.Player;

/**
 * Skins des joueurs, <b>indépendants du mode du serveur</b> : un serveur en mode hors ligne derrière un proxy (Velocity, BungeeCord)
 * ne sait pas interroger Mojang par lui-même. Ce service
 * <ul>
 *   <li>retient le skin que le proxy transmet à chaque connexion (propriété « textures » du profil, signature comprise) : aucun appel réseau
 *       pour les joueurs du serveur ;</li>
 *   <li>interroge directement l'API de Mojang (nom → identifiant → profil signé) pour tout autre compte, en tâche de fond, avec un cache.</li>
 * </ul>
 */
public final class SkinLookup {
   /** Skin d'un compte : {@code value} (JSON encodé en base 64) et {@code signature} sont la propriété « textures » du profil. */
   public record Skin(String name, String value, String signature) {
      /** Adresse de l'image du skin (dans le JSON encodé), ou null. */
      public URL skinUrl() {
         try {
            JsonObject json = JsonParser.parseString(new String(Base64.getDecoder().decode(this.value), StandardCharsets.UTF_8)).getAsJsonObject();
            return URI.create(json.getAsJsonObject("textures").getAsJsonObject("SKIN").get("url").getAsString()).toURL();
         } catch (Exception e) {
            return null;
         }
      }
   }

   private record Cached(Skin skin, long at, boolean failedNetwork) {
   }

   private static final long OK_TTL_MS = 6L * 3600000L;
   private static final long MISSING_TTL_MS = 5L * 60000L;
   private static final long NETWORK_TTL_MS = 30000L;

   private final EsicraftPlugin plugin;
   private final Map<UUID, Skin> byUuid = new ConcurrentHashMap<>();
   private final Map<String, Cached> byName = new ConcurrentHashMap<>();
   private final HttpClient http = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(5)).followRedirects(HttpClient.Redirect.NORMAL).build();
   private final ExecutorService io = Executors.newFixedThreadPool(2, r -> {
      Thread t = new Thread(r, "esicraft-skins");
      t.setDaemon(true);
      return t;
   });

   public SkinLookup(EsicraftPlugin plugin) {
      this.plugin = plugin;
   }

   public void shutdown() {
      this.io.shutdownNow();
   }

   /** À appeler sur le thread principal quand un joueur est en ligne : retient le skin transmis par le proxy (jamais un skin de déguisement). */
   public void remember(Player player) {
      if (this.plugin.getDisguiseManager() != null && this.plugin.getDisguiseManager().get(player.getUniqueId()) != null) {
         return;
      }

      try {
         for (ProfileProperty p : player.getPlayerProfile().getProperties()) {
            if (p.getName().equals("textures") && p.getValue() != null && !p.getValue().isEmpty()) {
               Skin skin = new Skin(player.getName(), p.getValue(), p.getSignature());
               this.byUuid.put(player.getUniqueId(), skin);
               this.byName.put(player.getName().toLowerCase(Locale.ROOT), new Cached(skin, System.currentTimeMillis(), false));
               return;
            }
         }
      } catch (RuntimeException ignored) {
      }
   }

   public Skin cached(UUID uuid) {
      return uuid == null ? null : this.byUuid.get(uuid);
   }

   /** Skin déjà connu (joueur du serveur ou recherche récente réussie), sans appel réseau. */
   public Skin cachedByName(String name) {
      Cached c = this.byName.get(name.toLowerCase(Locale.ROOT));
      return c != null && c.skin() != null && System.currentTimeMillis() - c.at() < OK_TTL_MS ? c.skin() : null;
   }

   /** Recherche en tâche de fond ; le résultat est null si le compte n'existe pas, et la recherche échoue (IOException) si Mojang est injoignable. */
   public CompletableFuture<Skin> lookup(String name) {
      Skin known = this.cachedByName(name);
      if (known != null) {
         return CompletableFuture.completedFuture(known);
      }

      CompletableFuture<Skin> future = new CompletableFuture<>();
      try {
         this.io.execute(() -> {
            try {
               future.complete(this.fetchBlocking(name));
            } catch (Throwable t) {
               future.completeExceptionally(t);
            }
         });
      } catch (RuntimeException rejected) {
         future.completeExceptionally(rejected);
      }

      return future;
   }

   /** Bloquant : à n'appeler que hors du thread principal. */
   public Skin fetchBlocking(String name) throws IOException {
      String key = name.toLowerCase(Locale.ROOT);
      Cached c = this.byName.get(key);
      long now = System.currentTimeMillis();
      if (c != null) {
         if (c.skin() != null && now - c.at() < OK_TTL_MS) {
            return c.skin();
         }

         if (c.skin() == null && now - c.at() < (c.failedNetwork() ? NETWORK_TTL_MS : MISSING_TTL_MS)) {
            if (c.failedNetwork()) {
               throw new IOException("Mojang injoignable (nouvelle tentative dans quelques secondes)");
            }

            return null;
         }
      }

      try {
         JsonObject profile = this.getJson("https://api.mojang.com/users/profiles/minecraft/" + java.net.URLEncoder.encode(name, StandardCharsets.UTF_8));
         if (profile == null || !profile.has("id")) {
            this.byName.put(key, new Cached(null, now, false));
            return null;
         }

         String id = profile.get("id").getAsString();
         String canonical = profile.has("name") ? profile.get("name").getAsString() : name;
         JsonObject full = this.getJson("https://sessionserver.mojang.com/session/minecraft/profile/" + id + "?unsigned=false");
         Skin skin = null;
         if (full != null && full.has("properties")) {
            JsonArray props = full.getAsJsonArray("properties");
            for (JsonElement e : props) {
               JsonObject p = e.getAsJsonObject();
               if ("textures".equals(p.get("name").getAsString())) {
                  skin = new Skin(canonical, p.get("value").getAsString(), p.has("signature") ? p.get("signature").getAsString() : null);
                  break;
               }
            }
         }

         this.byName.put(key, new Cached(skin, now, false));
         return skin;
      } catch (IOException e) {
         this.byName.put(key, new Cached(null, now, true));
         throw e;
      }
   }

   /** GET JSON ; null si 204 / 404 (compte inexistant) ; IOException si Mojang répond une erreur ou ne répond pas. */
   private JsonObject getJson(String url) throws IOException {
      try {
         HttpResponse<String> response = this.http.send(HttpRequest.newBuilder(URI.create(url)).timeout(Duration.ofSeconds(6)).header("Accept", "application/json").GET().build(),
            HttpResponse.BodyHandlers.ofString());
         int status = response.statusCode();
         if (status == 204 || status == 404) {
            return null;
         }

         if (status != 200) {
            throw new IOException("Mojang a répondu " + status + " (" + url + ")");
         }

         return JsonParser.parseString(response.body()).getAsJsonObject();
      } catch (InterruptedException e) {
         Thread.currentThread().interrupt();
         throw new IOException("Recherche interrompue", e);
      } catch (RuntimeException e) {
         throw new IOException("Réponse illisible de Mojang : " + e.getMessage(), e);
      }
   }
}
