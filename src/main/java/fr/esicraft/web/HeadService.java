package fr.esicraft.web;

import fr.esicraft.EsicraftPlugin;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.InputStream;
import java.net.URI;
import java.net.URL;
import java.net.URLConnection;
import java.util.Collection;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.regex.Pattern;
import java.util.zip.CRC32;
import java.util.zip.Deflater;
import javax.imageio.ImageIO;
import org.bukkit.entity.Player;

/**
 * Têtes de joueurs du site : le skin est récupéré <b>par le serveur</b> (le navigateur n'appelle aucun service externe, la politique de
 * sécurité du site l'interdit), découpé (visage + calque du chapeau), agrandi sans lissage et servi en PNG depuis {@code /head/<pseudo>}.
 * <p>
 * Le skin d'un joueur est mémorisé dès qu'il se connecte (adresse du skin dans son profil, y compris derrière un proxy BungeeCord) : aucun
 * appel à Mojang n'est nécessaire pour les joueurs du serveur. Pour les autres, une recherche Mojang est faite en tâche de fond.
 * Tant que la vraie tête n'est pas prête, ou si elle est introuvable, un petit visage coloré est servi (PNG écrit sans interface graphique).
 */
final class HeadService {
   static final Pattern NAME = Pattern.compile("[A-Za-z0-9_]{1,16}");
   private static final long TTL_MS = 6L * 3600000L;
   private static final long RETRY_MS = 10L * 60000L;

   /** {@code real} : vraie tête (cache long) ; sinon visage de remplacement (cache court). */
   record Head(byte[] png, boolean real) {
   }

   private record Entry(Head head, long at) {
   }

   private final EsicraftPlugin plugin;
   private final Map<String, Entry> cache = new ConcurrentHashMap<>();
   /** Adresse du skin de chaque joueur vu en ligne (pseudo en minuscules). */
   private final Map<String, URL> skinUrls = new ConcurrentHashMap<>();
   private final Set<String> inflight = ConcurrentHashMap.newKeySet();
   private final Set<String> warned = ConcurrentHashMap.newKeySet();
   /** File bornée : une rafale de pseudos inventés ne remplit pas la mémoire (les demandes en trop sont simplement ignorées). */
   private final ExecutorService io = new ThreadPoolExecutor(2, 2, 30L, TimeUnit.SECONDS, new ArrayBlockingQueue<>(64), r -> {
      Thread t = new Thread(r, "esicraft-heads");
      t.setDaemon(true);
      return t;
   });
   private static final int MAX_CACHE = 1500;
   /** Pseudos des joueurs du serveur (en minuscules) : seuls ceux-là déclenchent une recherche Mojang. */
   private volatile Set<String> known = Set.of();

   /** Remplace la liste des joueurs connus du site. */
   void setKnown(Collection<String> names) {
      Set<String> set = ConcurrentHashMap.newKeySet();
      for (String n : names) {
         if (n != null) {
            set.add(n.toLowerCase(Locale.ROOT));
         }
      }

      this.known = set;
   }

   private void trim() {
      if (this.cache.size() > MAX_CACHE) {
         this.cache.entrySet().stream().sorted(java.util.Comparator.comparingLong(e -> e.getValue().at())).limit(this.cache.size() - MAX_CACHE + 200L)
            .map(Map.Entry::getKey).toList().forEach(this.cache::remove);
      }

      if (this.skinUrls.size() > MAX_CACHE * 2) {
         this.skinUrls.keySet().stream().filter(k -> !this.known.contains(k)).limit(500).toList().forEach(this.skinUrls::remove);
      }
   }

   HeadService(EsicraftPlugin plugin) {
      this.plugin = plugin;
   }

   void shutdown() {
      this.io.shutdownNow();
   }

   /** À appeler sur le thread principal quand un joueur est en ligne : retient l'adresse de son skin (sans appel réseau). */
   void remember(Player player) {
      try {
         URL skin = player.getPlayerProfile().getTextures().getSkin();
         if (skin != null) {
            String key = player.getName().toLowerCase(Locale.ROOT);
            URL before = this.skinUrls.put(key, skin);
            if (before == null || !before.equals(skin)) {
               this.cache.remove(key);
            }
         }
      } catch (RuntimeException ignored) {
      }
   }

   /** Tête à servir maintenant ; lance la récupération en tâche de fond si elle manque ou si elle est périmée. */
   Head get(String name) {
      if (!NAME.matcher(name).matches()) {
         return new Head(placeholder(name), false);
      }

      String key = name.toLowerCase(Locale.ROOT);
      if (!this.known.contains(key) && !this.skinUrls.containsKey(key)) {
         // Pseudo inconnu du serveur : aucun appel à Mojang (sinon n'importe qui épuiserait notre quota avec des pseudos inventés).
         return new Head(placeholder(name), false);
      }

      Entry e = this.cache.get(key);
      long now = System.currentTimeMillis();
      if (e == null || now - e.at() > (e.head().real() ? TTL_MS : RETRY_MS)) {
         this.fetch(name, key);
      }

      return e != null ? e.head() : new Head(placeholder(name), false);
   }

   /** Prépare les têtes des joueurs connus (appelé à chaque rafraîchissement du site, sans bloquer). */
   void prefetch(Collection<String> names) {
      int started = 0;
      for (String name : names) {
         if (started >= 60) {
            return;
         }

         if (name != null && NAME.matcher(name).matches()) {
            String key = name.toLowerCase(Locale.ROOT);
            Entry e = this.cache.get(key);
            if (e == null || System.currentTimeMillis() - e.at() > (e.head().real() ? TTL_MS : RETRY_MS)) {
               this.fetch(name, key);
               started++;
            }
         }
      }
   }

   private void warnOnce(String id, String message) {
      if (this.warned.add(id)) {
         this.plugin.getLogger().warning("Têtes du site : " + message);
      }
   }

   private void fetch(String name, String key) {
      if (!this.inflight.add(key)) {
         return;
      }

      try {
         this.io.execute(() -> {
            try {
               byte[] png = this.render(name, key);
               this.cache.put(key, new Entry(png != null ? new Head(png, true) : new Head(placeholder(name), false), System.currentTimeMillis()));
               this.trim();
            } catch (Throwable t) {
               this.cache.put(key, new Entry(new Head(placeholder(name), false), System.currentTimeMillis()));
               this.plugin.getLogger().log(Level.FINE, "Tête de " + name + " indisponible : " + t);
            } finally {
               this.inflight.remove(key);
            }
         });
      } catch (RuntimeException rejected) {
         this.inflight.remove(key);
      }
   }

   /** Télécharge le skin du compte et en tire la tête (64 × 64). Renvoie null si le compte ou son skin est introuvable. */
   private byte[] render(String name, String key) throws Exception {
      URL skin = this.skinUrls.get(key);
      if (skin == null) {
         // Recherche directe auprès de Mojang (indépendante du mode du serveur : hors ligne derrière Velocity ou BungeeCord).
         fr.esicraft.life.SkinLookup.Skin found = this.plugin.getSkins().cachedByName(name);
         try {
            if (found == null) {
               found = this.plugin.getSkins().fetchBlocking(name);
            }
         } catch (java.io.IOException e) {
            this.warnOnce("mojang", "Mojang est injoignable depuis le serveur (" + e.getMessage() + ") : un visage de remplacement est affiché pour les joueurs dont le skin n'a pas été vu en ligne.");
            return null;
         }

         skin = found == null ? null : found.skinUrl();
         if (skin == null) {
            return null;
         }

         this.skinUrls.put(key, skin);
      }

      String host = skin.getHost();
      if (!"https".equals(skin.getProtocol()) && !"http".equals(skin.getProtocol()) || !(host.equals("textures.minecraft.net") || host.endsWith(".minecraft.net"))) {
         return null;
      }

      URLConnection c = URI.create(skin.toString().replace("http://", "https://")).toURL().openConnection();
      c.setConnectTimeout(4000);
      c.setReadTimeout(6000);
      BufferedImage image;
      try (InputStream in = c.getInputStream()) {
         image = ImageIO.read(in);
      } catch (NoClassDefFoundError | UnsatisfiedLinkError missing) {
         this.warnOnce("awt", "ce serveur Java n'a pas le module d'images (java.desktop) : les têtes réelles sont impossibles, un visage de remplacement est affiché.");
         return null;
      }

      return image == null ? null : headFrom(image);
   }

   /** Visage (8 × 8) + calque du chapeau, agrandi ×8 sans lissage, encodé en PNG. */
   static byte[] headFrom(BufferedImage skin) {
      if (skin.getWidth() < 64 || skin.getHeight() < 16) {
         return null;
      }

      int[] out = new int[64 * 64];
      for (int y = 0; y < 8; y++) {
         for (int x = 0; x < 8; x++) {
            int face = skin.getRGB(8 + x, 8 + y);
            int hat = skin.getRGB(40 + x, 8 + y);
            int a = hat >>> 24;
            int mixed;
            if (a == 255) {
               mixed = hat;
            } else if (a == 0) {
               mixed = face | 0xFF000000;
            } else {
               int r = ((hat >> 16 & 255) * a + (face >> 16 & 255) * (255 - a)) / 255;
               int g = ((hat >> 8 & 255) * a + (face >> 8 & 255) * (255 - a)) / 255;
               int b = ((hat & 255) * a + (face & 255) * (255 - a)) / 255;
               mixed = 0xFF000000 | r << 16 | g << 8 | b;
            }

            for (int dy = 0; dy < 8; dy++) {
               for (int dx = 0; dx < 8; dx++) {
                  out[(y * 8 + dy) * 64 + x * 8 + dx] = mixed;
               }
            }
         }
      }

      return encodePng(64, 64, out);
   }

   /** Petit visage pixelisé de couleur dérivée du pseudo (écrit en Java pur : aucune interface graphique ni police nécessaire). */
   static byte[] placeholder(String name) {
      int hue = Math.floorMod(name.toLowerCase(Locale.ROOT).hashCode(), 360);
      int skin = hslToRgb(hue, 0.35, 0.72);
      int hair = shade(skin, 0.45);
      int eye = 0xFF28283C;
      int mouth = 0xFF1C1C28;
      int[] face = new int[64];
      for (int y = 0; y < 8; y++) {
         for (int x = 0; x < 8; x++) {
            face[y * 8 + x] = y < 2 ? hair : skin;
         }
      }

      face[4 * 8 + 2] = eye;
      face[4 * 8 + 5] = eye;
      face[6 * 8 + 3] = mouth;
      face[6 * 8 + 4] = mouth;
      int[] out = new int[64 * 64];
      for (int y = 0; y < 64; y++) {
         for (int x = 0; x < 64; x++) {
            out[y * 64 + x] = face[(y / 8) * 8 + x / 8];
         }
      }

      return encodePng(64, 64, out);
   }

   /** Teinte (0-359), saturation et luminosité (0-1) → couleur opaque, sans java.awt (absent de certains JRE de serveur). */
   private static int hslToRgb(int hue, double s, double l) {
      double c = (1 - Math.abs(2 * l - 1)) * s;
      double hp = hue / 60.0;
      double x = c * (1 - Math.abs(hp % 2 - 1));
      double r = 0;
      double g = 0;
      double b = 0;
      if (hp < 1) {
         r = c;
         g = x;
      } else if (hp < 2) {
         r = x;
         g = c;
      } else if (hp < 3) {
         g = c;
         b = x;
      } else if (hp < 4) {
         g = x;
         b = c;
      } else if (hp < 5) {
         r = x;
         b = c;
      } else {
         r = c;
         b = x;
      }

      double m = l - c / 2;
      return 0xFF000000 | (int)Math.round((r + m) * 255) << 16 | (int)Math.round((g + m) * 255) << 8 | (int)Math.round((b + m) * 255);
   }

   private static int shade(int argb, double factor) {
      int r = (int)((argb >> 16 & 255) * factor);
      int g = (int)((argb >> 8 & 255) * factor);
      int b = (int)((argb & 255) * factor);
      return 0xFF000000 | r << 16 | g << 8 | b;
   }

   /** Encodeur PNG minimal (RGBA 8 bits) : zlib et CRC32 du JDK, aucune dépendance à l'interface graphique. */
   static byte[] encodePng(int w, int h, int[] argb) {
      try {
         ByteArrayOutputStream raw = new ByteArrayOutputStream(h * (w * 4 + 1));
         for (int y = 0; y < h; y++) {
            raw.write(0);
            for (int x = 0; x < w; x++) {
               int c = argb[y * w + x];
               raw.write(c >> 16 & 255);
               raw.write(c >> 8 & 255);
               raw.write(c & 255);
               raw.write(c >>> 24);
            }
         }

         Deflater deflater = new Deflater();
         deflater.setInput(raw.toByteArray());
         deflater.finish();
         ByteArrayOutputStream z = new ByteArrayOutputStream();
         byte[] buf = new byte[4096];
         while (!deflater.finished()) {
            z.write(buf, 0, deflater.deflate(buf));
         }

         deflater.end();
         ByteArrayOutputStream png = new ByteArrayOutputStream();
         png.write(new byte[]{(byte)0x89, 'P', 'N', 'G', 0x0D, 0x0A, 0x1A, 0x0A});
         ByteArrayOutputStream ihdr = new ByteArrayOutputStream();
         DataOutputStream d = new DataOutputStream(ihdr);
         d.writeInt(w);
         d.writeInt(h);
         d.write(new byte[]{8, 6, 0, 0, 0});
         chunk(png, "IHDR", ihdr.toByteArray());
         chunk(png, "IDAT", z.toByteArray());
         chunk(png, "IEND", new byte[0]);
         return png.toByteArray();
      } catch (java.io.IOException e) {
         return new byte[0];
      }
   }

   private static void chunk(ByteArrayOutputStream out, String type, byte[] data) throws java.io.IOException {
      DataOutputStream d = new DataOutputStream(out);
      d.writeInt(data.length);
      byte[] t = type.getBytes(java.nio.charset.StandardCharsets.US_ASCII);
      d.write(t);
      d.write(data);
      CRC32 crc = new CRC32();
      crc.update(t);
      crc.update(data);
      d.writeInt((int)crc.getValue());
   }
}
