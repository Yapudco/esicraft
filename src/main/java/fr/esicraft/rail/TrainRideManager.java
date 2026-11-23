package fr.esicraft.rail;

import fr.esicraft.EsicraftPlugin;
import fr.esicraft.util.ChatUtil;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Sound;
import org.bukkit.World;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.entity.minecart.RideableMinecart;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.vehicle.VehicleDamageEvent;
import org.bukkit.event.vehicle.VehicleDestroyEvent;
import org.bukkit.event.vehicle.VehicleEnterEvent;
import org.bukkit.scheduler.BukkitTask;
import org.bukkit.util.Vector;

/**
 * Trajet de train des voyageurs : plus de téléportation instantanée. Le voyageur et un wagonnet sont déplacés à chaque tick le long du tracé
 * réel des rails (celui trouvé par {@link RailSearch}), à vitesse constante : plus la ligne est longue, plus le trajet dure
 * ({@code rail.ride.blocks-per-second}). Pendant le trajet le voyageur est invulnérable et ne peut pas frapper ; arriver à destination (ou
 * se déconnecter, ou arrêter le serveur) le dépose à la gare d'arrivée.
 */
public class TrainRideManager implements Listener {
   static final String TAG = "esicraft_train_ride";

   private static final class Ride {
      final UUID playerId;
      final World world;
      final long[] path;
      final long started;
      final long duration;
      final Location arrival;
      final Consumer<Boolean> onArrive;
      final boolean wasInvulnerable;
      RideableMinecart cart;

      Ride(UUID playerId, World world, long[] path, long started, long duration, Location arrival, Consumer<Boolean> onArrive, boolean wasInvulnerable) {
         this.playerId = playerId;
         this.world = world;
         this.path = path;
         this.started = started;
         this.duration = duration;
         this.arrival = arrival;
         this.onArrive = onArrive;
         this.wasInvulnerable = wasInvulnerable;
      }
   }

   private final EsicraftPlugin plugin;
   private final Map<UUID, Ride> rides = new ConcurrentHashMap<>();
   private BukkitTask task;
   private int tickCounter;
   private boolean internalTp;

   public TrainRideManager(EsicraftPlugin plugin) {
      this.plugin = plugin;
   }

   public void init() {
      this.task = Bukkit.getScheduler().runTaskTimer(this.plugin, this::tick, 1L, 1L);
   }

   /** Arrêt du serveur : chaque voyageur est déposé à sa gare d'arrivée (sinon il resterait au milieu de la ligne). */
   public void shutdown() {
      if (this.task != null) {
         this.task.cancel();
         this.task = null;
      }

      for (Ride ride : List.copyOf(this.rides.values())) {
         this.finish(ride, Bukkit.getPlayer(ride.playerId));
      }
   }

   public boolean isRiding(UUID playerId) {
      return this.rides.containsKey(playerId);
   }

   /** Vrai pendant une téléportation imposée par le trajet : elle n'est pas une fuite de combat (voir {@code CombatManager}). */
   public boolean isInternalTeleport() {
      return this.internalTp;
   }

   private boolean teleportInternal(Player player, Location loc) {
      this.internalTp = true;
      try {
         return player.teleport(loc);
      } finally {
         this.internalTp = false;
      }
   }

   /** Durée d'un trajet de {@code steps} rails : proportionnelle à la distance, bornée par {@code rail.ride.min-seconds} et {@code max-seconds}. */
   public long durationMs(int steps) {
      double speed = Math.max(1.0, this.plugin.getConfig().getDouble("rail.ride.blocks-per-second", 30.0));
      double min = Math.max(1.0, this.plugin.getConfig().getDouble("rail.ride.min-seconds", 5.0));
      double max = Math.max(min, this.plugin.getConfig().getDouble("rail.ride.max-seconds", 600.0));
      double seconds = Math.max(min, Math.min(max, steps / speed));
      return (long)(seconds * 1000.0);
   }

   /**
    * Embarque le voyageur. {@code onArrive} reçoit {@code true} une fois le voyageur déposé à destination, {@code false} si le trajet a échoué
    * (mort, téléportation impossible) : l'appelant rembourse alors le billet.
    */
   public void start(Player player, long[] path, String destinationName, Location arrival, Consumer<Boolean> onArrive) {
      if (player.isInsideVehicle()) {
         player.leaveVehicle();
      }

      long duration = this.durationMs(Math.max(0, path.length - 1));
      Ride ride = new Ride(player.getUniqueId(), player.getWorld(), path, System.currentTimeMillis(), duration, arrival, onArrive, player.isInvulnerable());
      player.setInvulnerable(true);
      this.rides.put(player.getUniqueId(), ride);
      ChatUtil.sendMessage(player, "&a🚆 En voiture pour &e" + destinationName + " &a! Durée du trajet : &e" + fr.esicraft.treaty.TreatyManager.formatDuration(duration)
         + "&a. Vous ne pouvez ni frapper ni être blessé pendant le voyage.");
      ChatUtil.playSound(player, Sound.ENTITY_MINECART_RIDING, 1.0F, 1.2F);
   }

   private Location positionOf(Ride ride, double t) {
      int n = ride.path.length;
      double f = Math.max(0.0, Math.min(1.0, t)) * (n - 1);
      int i = Math.min(n - 2, (int)f);
      double frac = f - i;
      double x1 = RailSearch.unpackX(ride.path[i]) + 0.5;
      double y1 = RailSearch.unpackY(ride.path[i]) + 0.1;
      double z1 = RailSearch.unpackZ(ride.path[i]) + 0.5;
      double x2 = RailSearch.unpackX(ride.path[i + 1]) + 0.5;
      double y2 = RailSearch.unpackY(ride.path[i + 1]) + 0.1;
      double z2 = RailSearch.unpackZ(ride.path[i + 1]) + 0.5;
      Location loc = new Location(ride.world, x1 + (x2 - x1) * frac, y1 + (y2 - y1) * frac, z1 + (z2 - z1) * frac);
      loc.setYaw((float)Math.toDegrees(Math.atan2(-(x2 - x1), z2 - z1)));
      return loc;
   }

   private void tick() {
      this.tickCounter++;
      long now = System.currentTimeMillis();
      for (Ride ride : List.copyOf(this.rides.values())) {
         Player player = Bukkit.getPlayer(ride.playerId);
         if (player == null || !player.isOnline() || player.isDead()) {
            this.abort(ride, player);
            continue;
         }

         if (ride.path.length < 2 || now - ride.started >= ride.duration) {
            this.finish(ride, player);
            continue;
         }

         double t = (double)(now - ride.started) / ride.duration;
         Location loc = this.positionOf(ride, t);
         if (this.tickCounter % 10 == 0) {
            // Charge à l'avance les chunks des 3 prochaines secondes pour que le wagon ne saccade pas sur un chargement de disque.
            Location ahead = this.positionOf(ride, Math.min(1.0, t + 3000.0 / ride.duration));
            ride.world.getChunkAtAsync(ahead.getBlockX() >> 4, ahead.getBlockZ() >> 4, false);
         }

         if (ride.cart == null || !ride.cart.isValid()) {
            ride.cart = this.spawn(ride.world, loc);
         } else {
            ride.cart.teleport(loc);
         }

         Location rider = loc.clone().add(0.0, 0.35, 0.0);
         rider.setYaw(player.getLocation().getYaw());
         rider.setPitch(player.getLocation().getPitch());
         this.teleportInternal(player, rider);
         player.setFallDistance(0.0F);
         if (this.tickCounter % 20 == 0) {
            long left = Math.max(0L, ride.duration - (now - ride.started));
            ChatUtil.sendActionbar(player, "&e🚆 En route… arrivée dans &6" + (left / 1000L + 1L) + " s");
            ChatUtil.playSound(player, Sound.ENTITY_MINECART_RIDING, 0.6F, 1.0F);
         }
      }
   }

   private RideableMinecart spawn(World world, Location loc) {
      return world.spawn(loc, RideableMinecart.class, cart -> {
         cart.addScoreboardTag(TAG);
         cart.setPersistent(false);
         cart.setGravity(false);
         cart.setSilent(true);
         cart.setInvulnerable(true);
         cart.setSlowWhenEmpty(false);
         cart.setMaxSpeed(0.0);
         cart.setDerailedVelocityMod(new Vector(0, 0, 0));
         cart.setFlyingVelocityMod(new Vector(0, 0, 0));
      });
   }

   private void cleanup(Ride ride, Player player) {
      this.rides.remove(ride.playerId);
      if (ride.cart != null && ride.cart.isValid()) {
         ride.cart.remove();
      }

      ride.cart = null;
      if (player != null) {
         player.setInvulnerable(ride.wasInvulnerable);
      }
   }

   /** Dépose le voyageur à la gare d'arrivée (arrivée normale, déconnexion ou arrêt du serveur). */
   private void finish(Ride ride, Player player) {
      this.cleanup(ride, player);
      if (player == null) {
         ride.onArrive.accept(false);
         return;
      }

      Location arrival = ride.arrival.clone();
      arrival.setYaw(player.getLocation().getYaw());
      arrival.setPitch(player.getLocation().getPitch());
      ride.onArrive.accept(this.teleportInternal(player, arrival));
   }

   /** Trajet interrompu (mort, joueur disparu) : pas d'arrivée, le billet est remboursé par l'appelant. */
   private void abort(Ride ride, Player player) {
      this.cleanup(ride, player);
      ride.onArrive.accept(false);
   }

   // ------------------------------------------------------------------ règles pendant le trajet

   @EventHandler
   public void onQuit(PlayerQuitEvent event) {
      Ride ride = this.rides.get(event.getPlayer().getUniqueId());
      if (ride != null) {
         this.finish(ride, event.getPlayer());
      }
   }

   @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
   public void onAttack(EntityDamageByEntityEvent event) {
      if (event.getDamager() instanceof Player player && this.rides.containsKey(player.getUniqueId())) {
         event.setCancelled(true);
      }
   }

   private static boolean isRideCart(Entity entity) {
      return entity != null && entity.getScoreboardTags().contains(TAG);
   }

   @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
   public void onEnter(VehicleEnterEvent event) {
      if (isRideCart(event.getVehicle())) {
         event.setCancelled(true);
      }
   }

   @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
   public void onDamage(VehicleDamageEvent event) {
      if (isRideCart(event.getVehicle())) {
         event.setCancelled(true);
      }
   }

   @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
   public void onDestroy(VehicleDestroyEvent event) {
      if (isRideCart(event.getVehicle())) {
         event.setCancelled(true);
      }
   }
}
