package fr.esicraft.util;

import fr.esicraft.EsicraftPlugin;
import java.net.Inet6Address;
import java.net.InetAddress;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.util.HexFormat;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import org.bukkit.entity.Player;

/**
 * Carnet des connexions : retient, pour chaque compte, les adresses utilisées ces 30 derniers jours (empreintes salées, jamais l'adresse en clair).
 * Sert à repérer les comptes multiples d'une même machine même quand l'un des deux est hors ligne (parrainage, remerciements, statistiques de
 * combat, paris) : avant, la vérification « même adresse » ne voyait que deux joueurs connectés au même moment.
 * Une adresse partagée par beaucoup de comptes (proxy sans transfert d'adresse, réseau d'école…) est ignorée pour ne pas bloquer tout le monde.
 */
public class AddressBook {
   private static final long KEEP_MS = 30L * 86400000L;
   private final EsicraftPlugin plugin;
   private final Map<UUID, Set<String>> byPlayer = new ConcurrentHashMap<>();
   private final Map<String, Set<UUID>> byAddress = new ConcurrentHashMap<>();
   private volatile String salt = "";

   public AddressBook(EsicraftPlugin plugin) {
      this.plugin = plugin;
   }

   public void init() {
      var db = this.plugin.getDataManager().getStorage();
      db.exec("CREATE TABLE IF NOT EXISTS player_addresses (uuid VARCHAR(36), addr VARCHAR(64), last_seen BIGINT, PRIMARY KEY (uuid, addr));");
      var rows = db.select("SELECT v FROM kv WHERE k = 'address_salt';", 1);
      if (rows.isEmpty()) {
         byte[] raw = new byte[16];
         new SecureRandom().nextBytes(raw);
         this.salt = HexFormat.of().formatHex(raw);
         db.exec("INSERT INTO kv (k, v) VALUES ('address_salt', ?) ON CONFLICT(k) DO NOTHING;", this.salt);
      } else {
         this.salt = String.valueOf(rows.get(0)[0]);
      }

      db.exec("DELETE FROM player_addresses WHERE last_seen < ?;", System.currentTimeMillis() - KEEP_MS);
      for (Object[] row : db.select("SELECT uuid, addr FROM player_addresses;", 2)) {
         try {
            this.index(UUID.fromString(String.valueOf(row[0])), String.valueOf(row[1]));
         } catch (IllegalArgumentException ignored) {
         }
      }

      // Joueurs déjà connectés (rechargement du plugin).
      for (Player p : org.bukkit.Bukkit.getOnlinePlayers()) {
         if (p.getAddress() != null) {
            this.record(p.getUniqueId(), p.getAddress().getAddress());
         }
      }
   }

   private void index(UUID uuid, String fp) {
      this.byPlayer.computeIfAbsent(uuid, k -> ConcurrentHashMap.newKeySet()).add(fp);
      this.byAddress.computeIfAbsent(fp, k -> ConcurrentHashMap.newKeySet()).add(uuid);
   }

   public void clear() {
      this.byPlayer.clear();
      this.byAddress.clear();
   }

   private boolean enabled() {
      return this.plugin.getConfig().getBoolean("anti-alt.enabled", true);
   }

   /** Une adresse vue sur plus de comptes que ce seuil est partagée (proxy, réseau public) : elle ne relie personne. */
   private boolean shared(String fp) {
      Set<UUID> users = this.byAddress.get(fp);
      return users != null && users.size() > Math.max(2, this.plugin.getConfig().getInt("anti-alt.max-accounts-per-address", 6));
   }

   /** Empreinte d'une adresse : IPv4 entière, IPv6 ramenée à son préfixe /64 (une box attribue tout un /64 à un foyer). */
   private String fingerprint(InetAddress address) {
      byte[] raw = address.getAddress();
      if (address instanceof Inet6Address && raw.length == 16) {
         raw = java.util.Arrays.copyOf(raw, 8);
      }

      try {
         MessageDigest md = MessageDigest.getInstance("SHA-256");
         md.update(this.salt.getBytes(StandardCharsets.UTF_8));
         md.update(raw);
         return HexFormat.of().formatHex(md.digest(), 0, 12);
      } catch (java.security.NoSuchAlgorithmException e) {
         return HexFormat.of().formatHex(raw);
      }
   }

   /** Appelé à la pré-connexion (thread asynchrone) : note l'adresse du compte. */
   public void record(UUID uuid, InetAddress address) {
      if (uuid == null || address == null || address.isLoopbackAddress()) {
         return;
      }

      String fp = this.fingerprint(address);
      this.index(uuid, fp);
      this.plugin.getDataManager().getStorage().exec(
         "INSERT INTO player_addresses (uuid, addr, last_seen) VALUES (?, ?, ?) ON CONFLICT(uuid, addr) DO UPDATE SET last_seen = excluded.last_seen;",
         uuid.toString(), fp, System.currentTimeMillis());
   }

   /** Vrai si les deux comptes ont utilisé la même adresse (non partagée) ces 30 derniers jours. */
   public boolean linked(UUID a, UUID b) {
      if (a == null || b == null || !this.enabled()) {
         return false;
      }

      if (a.equals(b)) {
         return true;
      }

      Set<String> x = this.byPlayer.get(a);
      Set<String> y = this.byPlayer.get(b);
      if (x == null || y == null) {
         return false;
      }

      for (String fp : x) {
         if (y.contains(fp) && !this.shared(fp)) {
            return true;
         }
      }

      return false;
   }
}
