package fr.esicraft.web;

import com.destroystokyo.paper.profile.PlayerProfile;
import fr.esicraft.EsicraftPlugin;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.net.URI;
import java.net.URL;
import java.net.URLConnection;
import java.util.Collection;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.logging.Level;
import java.util.regex.Pattern;
import javax.imageio.ImageIO;

/**
 * Têtes de joueurs du site : le skin est récupéré <b>par le serveur</b> (le navigateur n'appelle aucun service externe, la politique de
 * sécurité du site l'interdit), découpé (visage + calque du chapeau), agrandi sans lissage et servi en PNG depuis {@code /head/<pseudo>}.
 * Tant que la vraie tête n'est pas prête, ou si le compte est introuvable, un petit visage de remplacement coloré est servi.
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
   private final Set<String> inflight = ConcurrentHashMap.newKeySet();
   private final ExecutorService io = Executors.newFixedThreadPool(2, r -> {
      Thread t = new Thread(r, "esicraft-heads");
      t.setDaemon(true);
      return t;
   });

   HeadService(EsicraftPlugin plugin) {
      this.plugin = plugin;
   }

   void shutdown() {
      this.io.shutdownNow();
   }

   /** Tête à servir maintenant ; lance la récupération en tâche de fond si elle manque ou si elle est périmée. */
   Head get(String name) {
      String key = name.toLowerCase(java.util.Locale.ROOT);
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
         if (started >= 40) {
            return;
         }

         if (name != null && NAME.matcher(name).matches()) {
            Entry e = this.cache.get(name.toLowerCase(java.util.Locale.ROOT));
            if (e == null || System.currentTimeMillis() - e.at() > (e.head().real() ? TTL_MS : RETRY_MS)) {
               this.fetch(name, name.toLowerCase(java.util.Locale.ROOT));
               started++;
            }
         }
      }
   }

   private void fetch(String name, String key) {
      if (!this.inflight.add(key)) {
         return;
      }

      try {
         this.io.execute(() -> {
            try {
               byte[] png = this.render(name);
               this.cache.put(key, new Entry(png != null ? new Head(png, true) : new Head(placeholder(name), false), System.currentTimeMillis()));
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
   private byte[] render(String name) throws Exception {
      PlayerProfile profile = org.bukkit.Bukkit.createProfile(name);
      if (!profile.complete(true) || profile.getTextures().getSkin() == null) {
         return null;
      }

      URL url = profile.getTextures().getSkin();
      String host = url.getHost();
      if (!"https".equals(url.getProtocol()) || !(host.equals("textures.minecraft.net") || host.endsWith(".minecraft.net"))) {
         return null;
      }

      URLConnection c = URI.create(url.toString()).toURL().openConnection();
      c.setConnectTimeout(4000);
      c.setReadTimeout(6000);
      BufferedImage skin;
      try (InputStream in = c.getInputStream()) {
         skin = ImageIO.read(in);
      }

      if (skin == null || skin.getWidth() < 64 || skin.getHeight() < 16) {
         return null;
      }

      BufferedImage out = new BufferedImage(64, 64, BufferedImage.TYPE_INT_ARGB);
      Graphics2D g = out.createGraphics();
      try {
         g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_NEAREST_NEIGHBOR);
         g.drawImage(skin.getSubimage(8, 8, 8, 8), 0, 0, 64, 64, null);
         g.drawImage(skin.getSubimage(40, 8, 8, 8), 0, 0, 64, 64, null);
      } finally {
         g.dispose();
      }

      ByteArrayOutputStream bytes = new ByteArrayOutputStream();
      ImageIO.write(out, "png", bytes);
      return bytes.toByteArray();
   }

   /** Petit visage pixelisé de couleur dérivée du pseudo (aucune police nécessaire, donc fiable sur un serveur sans interface graphique). */
   static byte[] placeholder(String name) {
      try {
         int hue = Math.floorMod(name.toLowerCase(java.util.Locale.ROOT).hashCode(), 360);
         Color skin = Color.getHSBColor(hue / 360f, 0.35f, 0.85f);
         BufferedImage img = new BufferedImage(8, 8, BufferedImage.TYPE_INT_ARGB);
         Color hair = skin.darker().darker();
         for (int y = 0; y < 8; y++) {
            for (int x = 0; x < 8; x++) {
               img.setRGB(x, y, (y < 2 ? hair : skin).getRGB());
            }
         }

         Color dark = new Color(40, 40, 60);
         img.setRGB(2, 4, dark.getRGB());
         img.setRGB(5, 4, dark.getRGB());
         img.setRGB(3, 6, dark.darker().getRGB());
         img.setRGB(4, 6, dark.darker().getRGB());
         BufferedImage big = new BufferedImage(64, 64, BufferedImage.TYPE_INT_ARGB);
         Graphics2D g = big.createGraphics();
         try {
            g.drawImage(img, 0, 0, 64, 64, null);
         } finally {
            g.dispose();
         }

         ByteArrayOutputStream bytes = new ByteArrayOutputStream();
         ImageIO.write(big, "png", bytes);
         return bytes.toByteArray();
      } catch (Throwable t) {
         return new byte[0];
      }
   }
}
