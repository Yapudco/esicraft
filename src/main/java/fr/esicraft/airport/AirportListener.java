package fr.esicraft.airport;

import fr.esicraft.EsicraftPlugin;
import fr.esicraft.util.ChatUtil;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityToggleGlideEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.Damageable;
import org.bukkit.inventory.meta.ItemMeta;

/**
 * Aéroports : l'élytre ne s'active qu'au décollage depuis une piste, et seulement à partir d'un certain Âge.
 * <ul>
 *   <li>Une <b>piste</b> est une bande d'au moins 32 × 3 blocs de béton noir, au même niveau (dans un sens ou dans l'autre).</li>
 *   <li>Planer ne peut commencer qu'au-dessus d'une piste : on saute depuis la piste puis on déploie l'élytre. Partout ailleurs (falaise,
 *       chute, bateau, montagne), l'élytre refuse de s'ouvrir.</li>
 *   <li>Se poser en dehors d'une piste abîme l'élytre.</li>
 *   <li>Le vol est fermé avant l'Âge 4 ({@code airports.min-age}), sauf dans l'End et le monde de ressources.</li>
 * </ul>
 * Aucune piste n'est à déclarer : elle est reconnue à la volée quand un joueur tente de décoller.
 */
public class AirportListener implements Listener {
   /** Au-dessus de la piste, le joueur est en l'air après son saut : on cherche la piste jusqu'à 4 blocs sous ses pieds. */
   private static final int MAX_HEIGHT_ABOVE_RUNWAY = 4;
   /** Distance maximale explorée de part et d'autre du joueur le long d'une rangée de béton. */
   private static final int SCAN_RADIUS = 80;

   private final EsicraftPlugin plugin;

   public AirportListener(EsicraftPlugin plugin) {
      this.plugin = plugin;
   }

   private boolean enabled() {
      return this.plugin.getConfig().getBoolean("airports.enabled", true);
   }

   /** L'End et le monde de ressources ne sont pas concernés (réglable). */
   private boolean isFreeWorld(World world) {
      if (world.getEnvironment() == World.Environment.THE_END && this.plugin.getConfig().getBoolean("airports.free-in-end", true)) {
         return true;
      }

      return this.plugin.getConfig().getBoolean("airports.free-in-resource-world", true)
         && this.plugin.getResourceWorld() != null
         && this.plugin.getResourceWorld().isResourceWorld(world);
   }

   @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
   public void onGlide(EntityToggleGlideEvent event) {
      if (!(event.getEntity() instanceof Player player) || !this.enabled() || this.isFreeWorld(player.getWorld())) {
         return;
      }

      if (event.isGliding()) {
         int minAge = Math.max(1, this.plugin.getConfig().getInt("airports.min-age", 4));
         if (this.plugin.getAgeManager().getPlayerAge(player).getLevel() < minAge) {
            event.setCancelled(true);
            ChatUtil.sendActionbar(player, "&c❌ Le vol en élytre est réservé aux guildes de l'Âge " + minAge + " (ou plus) !");
            ChatUtil.playSound(player, Sound.ENTITY_VILLAGER_NO, 1.0F, 1.0F);
            return;
         }

         if (!this.isOnRunway(player.getLocation())) {
            event.setCancelled(true);
            ChatUtil.sendActionbar(player, "&c❌ Décollage impossible : l'élytre ne s'ouvre que depuis une piste d'aéroport (au moins "
               + this.length() + " × " + this.width() + " blocs de béton noir) !");
            ChatUtil.playSound(player, Sound.ENTITY_VILLAGER_NO, 1.0F, 1.0F);
         }

         return;
      }

      this.checkLanding(player);
   }

   /** Atterrissage hors piste : l'élytre encaisse la secousse. */
   @SuppressWarnings("deprecation")
   private void checkLanding(Player player) {
      int damage = this.plugin.getConfig().getInt("airports.bad-landing-damage", 100);
      ItemStack chest = player.getInventory().getChestplate();
      if (damage <= 0 || chest == null || chest.getType() != Material.ELYTRA || !player.isOnGround() || this.isOnRunway(player.getLocation())) {
         return;
      }

      ItemMeta meta = chest.getItemMeta();
      if (!(meta instanceof Damageable damageable)) {
         return;
      }

      int total = damageable.getDamage() + damage;
      if (total >= Material.ELYTRA.getMaxDurability()) {
         player.getInventory().setChestplate(null);
      } else {
         damageable.setDamage(total);
         chest.setItemMeta(damageable);
         player.getInventory().setChestplate(chest);
      }

      ChatUtil.sendActionbar(player, "&c💥 Atterrissage hors piste : votre élytre est endommagée ! Posez-vous sur une piste d'aéroport.");
      ChatUtil.playSound(player, Sound.ENTITY_VILLAGER_NO, 1.0F, 0.6F);
   }

   private int length() {
      return Math.max(1, this.plugin.getConfig().getInt("airports.length", 32));
   }

   private int width() {
      return Math.max(1, this.plugin.getConfig().getInt("airports.width", 3));
   }

   /** Vrai si le bloc plein sous le joueur (à 4 blocs au plus : on décolle en sautant) fait partie d'une piste valide. */
   public boolean isOnRunway(Location loc) {
      World world = loc.getWorld();
      if (world == null) {
         return false;
      }

      int x = loc.getBlockX();
      int z = loc.getBlockZ();
      int y = loc.getBlockY() - 1;
      for (int i = 0; i <= MAX_HEIGHT_ABOVE_RUNWAY; i++, y--) {
         if (!world.isChunkLoaded(x >> 4, z >> 4) || y < world.getMinHeight()) {
            return false;
         }

         Material type = world.getBlockAt(x, y, z).getType();
         if (type.isAir()) {
            continue;
         }

         return type == Material.BLACK_CONCRETE && (this.hasStrip(world, x, y, z, true) || this.hasStrip(world, x, y, z, false));
      }

      return false;
   }

   private static boolean isBlack(World world, int x, int y, int z) {
      return world.isChunkLoaded(x >> 4, z >> 4) && world.getBlockAt(x, y, z).getType() == Material.BLACK_CONCRETE;
   }

   /**
    * Cherche une bande de béton noir de {@code length} × {@code width} blocs (dans le sens demandé) qui contient le bloc donné.
    * Pour chaque rangée voisine on mesure le tronçon de béton continu autour du joueur ; la bande existe si {@code width} rangées
    * consécutives, dont celle du joueur, partagent un tronçon commun d'au moins {@code length} blocs.
    */
   private boolean hasStrip(World world, int x, int y, int z, boolean alongX) {
      int length = this.length();
      int width = this.width();
      int a0 = alongX ? x : z;
      int b0 = alongX ? z : x;
      int rows = 2 * width - 1;
      int[] left = new int[rows];
      int[] right = new int[rows];
      boolean[] present = new boolean[rows];
      for (int r = 0; r < rows; r++) {
         int b = b0 - (width - 1) + r;
         if (!black(world, alongX, a0, b, y)) {
            continue;
         }

         present[r] = true;
         int l = a0;
         while (a0 - l < SCAN_RADIUS && black(world, alongX, l - 1, b, y)) {
            l--;
         }

         int rr = a0;
         while (rr - a0 < SCAN_RADIUS && black(world, alongX, rr + 1, b, y)) {
            rr++;
         }

         left[r] = l;
         right[r] = rr;
      }

      // Fenêtres de {@code width} rangées consécutives qui contiennent la rangée du joueur (indice width - 1).
      for (int start = 0; start <= width - 1; start++) {
         int lo = Integer.MIN_VALUE;
         int hi = Integer.MAX_VALUE;
         boolean ok = true;
         for (int r = start; r < start + width; r++) {
            if (!present[r]) {
               ok = false;
               break;
            }

            lo = Math.max(lo, left[r]);
            hi = Math.min(hi, right[r]);
         }

         if (ok && hi - lo + 1 >= length) {
            return true;
         }
      }

      return false;
   }

   /** Coordonnées (a le long de la bande, b en travers) converties en bloc réel au niveau {@code y}. */
   private static boolean black(World world, boolean alongX, int a, int b, int y) {
      return alongX ? isBlack(world, a, y, b) : isBlack(world, b, y, a);
   }
}
