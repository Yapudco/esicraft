package fr.esicraft.rail;

import fr.esicraft.EsicraftPlugin;
import fr.esicraft.guild.Guild;
import fr.esicraft.util.ChunkKey;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Consumer;
import org.bukkit.Bukkit;
import org.bukkit.Tag;
import org.bukkit.World;
import org.bukkit.block.data.BlockData;
import org.bukkit.block.data.Rail;
import org.bukkit.scheduler.BukkitRunnable;

/**
 * Parcours en largeur du réseau de rails à partir d'une gare.
 * <ul>
 *   <li>Deux rails voisins (même niveau, ou reliés par une pente) sont connectés : les embranchements et croisements
 *       forment donc un vrai réseau, pas une simple ligne.</li>
 *   <li>Les chunks non chargés sont chargés à la demande de façon asynchrone (jamais générés) et gardés par un ticket
 *       le temps de la recherche ; le parcours est étalé sur plusieurs ticks pour ne pas faire lagger le serveur.</li>
 *   <li>Les tronçons situés sur le territoire d'une guilde qui bannit la guilde du voyageur sont infranchissables :
 *       la recherche contourne ces zones, ou ne trouve plus la gare.</li>
 * </ul>
 */
public final class RailSearch extends BukkitRunnable {
   private static final int[] DX = {0, 1, 0, -1};
   private static final int[] DZ = {-1, 0, 1, 0};
   private static final Rail.Shape[] ASCENDING = {Rail.Shape.ASCENDING_NORTH, Rail.Shape.ASCENDING_EAST, Rail.Shape.ASCENDING_SOUTH, Rail.Shape.ASCENDING_WEST};
   private static final int NODES_PER_TICK = 1500;
   private static final long TIMEOUT_MS = 45000L;

   public record Reached(Station station, int steps, Set<ChunkKey> chunks) {
   }

   public record Result(Station origin, String travelerGuildId, long finishedAt, List<Reached> reached, boolean truncated, Set<String> blockedBy) {
   }

   private enum ChunkState { LOADED, EMPTY, PENDING }

   private final EsicraftPlugin plugin;
   private final RailManager manager;
   private final World world;
   private final Station origin;
   private final Guild traveler;
   private final Consumer<Result> callback;
   private final Map<Long, Long> parent = new HashMap<>();
   private final ArrayDeque<Long> queue = new ArrayDeque<>();
   private final List<Long> deferred = new ArrayList<>();
   private final Set<Long> pendingChunks = new HashSet<>();
   private final Set<Long> emptyChunks = new HashSet<>();
   private final Set<Long> ticketed = new HashSet<>();
   private final Set<String> blockedBy = new LinkedHashSet<>();
   private final int maxRails;
   private final long deadline = System.currentTimeMillis() + TIMEOUT_MS;
   private boolean truncated;
   private boolean finished;

   public RailSearch(EsicraftPlugin plugin, RailManager manager, World world, Station origin, Guild traveler, Consumer<Result> callback) {
      this.plugin = plugin;
      this.manager = manager;
      this.world = world;
      this.origin = origin;
      this.traveler = traveler;
      this.callback = callback;
      this.maxRails = plugin.getConfig().getInt("rail.search-max-rails", 30000);
   }

   public static long pack(int x, int y, int z) {
      return ((long)(x & 0x3FFFFFF) << 38) | ((long)(z & 0x3FFFFFF) << 12) | (long)((y + 2048) & 0xFFF);
   }

   private static int unpackX(long p) {
      return (int)(p >> 38);
   }

   private static int unpackZ(long p) {
      return (int)((p >> 12) & 0x3FFFFFFL) << 6 >> 6;
   }

   private static int unpackY(long p) {
      return (int)(p & 0xFFFL) - 2048;
   }

   private static long chunkKey(int cx, int cz) {
      return ((long)cx << 32) ^ (cz & 0xFFFFFFFFL);
   }

   public void begin() {
      long start = this.origin.railKey();
      this.parent.put(start, -1L);
      this.queue.add(start);
      this.runTaskTimer(this.plugin, 1L, 1L);
   }

   @Override
   public void run() {
      if (this.finished) {
         return;
      }

      int budget = NODES_PER_TICK;
      while (budget-- > 0 && !this.queue.isEmpty()) {
         long node = this.queue.poll();
         if (!this.expand(node)) {
            this.deferred.add(node);
         }
      }

      if (System.currentTimeMillis() > this.deadline) {
         this.truncated = true;
         this.finish();
      } else if (this.queue.isEmpty() && this.pendingChunks.isEmpty()) {
         this.finish();
      }
   }

   private ChunkState state(int cx, int cz) {
      long key = chunkKey(cx, cz);
      if (this.emptyChunks.contains(key)) {
         return ChunkState.EMPTY;
      }

      if (this.world.isChunkLoaded(cx, cz)) {
         if (this.ticketed.add(key)) {
            this.world.addPluginChunkTicket(cx, cz, this.plugin);
         }

         return ChunkState.LOADED;
      }

      if (this.pendingChunks.add(key)) {
         this.world.getChunkAtAsync(cx, cz, false).thenAccept(chunk -> Bukkit.getScheduler().runTask(this.plugin, () -> {
            this.pendingChunks.remove(key);
            if (chunk == null) {
               this.emptyChunks.add(key);
            } else if (!this.finished && this.ticketed.add(key)) {
               this.world.addPluginChunkTicket(cx, cz, this.plugin);
            }

            this.queue.addAll(this.deferred);
            this.deferred.clear();
         }));
      }

      return ChunkState.PENDING;
   }

   private boolean isRail(int x, int y, int z) {
      return y >= this.world.getMinHeight() && y < this.world.getMaxHeight() && Tag.RAILS.isTagged(this.world.getType(x, y, z));
   }

   private boolean ascendsToward(int x, int y, int z, int dir) {
      BlockData data = this.world.getBlockData(x, y, z);
      return data instanceof Rail rail && rail.getShape() == ASCENDING[dir];
   }

   /** Renvoie faux si le nœud doit être réexaminé plus tard (chunk voisin en cours de chargement). */
   private boolean expand(long node) {
      int x = unpackX(node);
      int y = unpackY(node);
      int z = unpackZ(node);
      boolean needDefer = false;
      for (int d = 0; d < 4; d++) {
         int nx = x + DX[d];
         int nz = z + DZ[d];
         ChunkState st = this.state(nx >> 4, nz >> 4);
         if (st == ChunkState.PENDING) {
            needDefer = true;
            continue;
         }

         if (st == ChunkState.EMPTY) {
            continue;
         }

         if (this.isRail(nx, y, nz)) {
            this.visit(node, nx, y, nz);
         }

         if (this.isRail(nx, y + 1, nz) && this.ascendsToward(x, y, z, d)) {
            this.visit(node, nx, y + 1, nz);
         }

         if (this.isRail(nx, y - 1, nz) && this.ascendsToward(nx, y - 1, nz, (d + 2) % 4)) {
            this.visit(node, nx, y - 1, nz);
         }
      }

      return !needDefer;
   }

   private void visit(long from, int nx, int ny, int nz) {
      long key = pack(nx, ny, nz);
      if (this.parent.containsKey(key)) {
         return;
      }

      if (this.parent.size() >= this.maxRails) {
         this.truncated = true;
         return;
      }

      Guild owner = this.manager.ownerOfChunk(this.world.getName(), nx >> 4, nz >> 4);
      if (owner != null && this.manager.isBanned(owner, this.traveler)) {
         this.blockedBy.add(owner.getName());
         return;
      }

      this.parent.put(key, from);
      this.queue.add(key);
   }

   private void finish() {
      if (this.finished) {
         return;
      }

      this.finished = true;
      this.cancel();
      List<Reached> reached = new ArrayList<>();
      for (Station station : this.manager.stationsInWorld(this.world.getName())) {
         if (station == this.origin || !this.parent.containsKey(station.railKey())) {
            continue;
         }

         Set<ChunkKey> chunks = new HashSet<>();
         int steps = 0;
         long cursor = station.railKey();
         while (cursor != -1L) {
            chunks.add(new ChunkKey(this.world.getName(), unpackX(cursor) >> 4, unpackZ(cursor) >> 4));
            steps++;
            Long next = this.parent.get(cursor);
            cursor = next == null ? -1L : next;
         }

         reached.add(new Reached(station, steps, chunks));
      }

      for (long key : this.ticketed) {
         this.world.removePluginChunkTicket((int)(key >> 32), (int)key, this.plugin);
      }

      this.callback.accept(new Result(this.origin, this.traveler != null ? this.traveler.getId() : null, System.currentTimeMillis(), reached, this.truncated, this.blockedBy));
   }
}
