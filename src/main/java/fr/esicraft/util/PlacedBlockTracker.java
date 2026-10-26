package fr.esicraft.util;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import org.bukkit.Bukkit;
import org.bukkit.Chunk;
import org.bukkit.NamespacedKey;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.world.ChunkUnloadEvent;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.Plugin;

/**
 * Mémorise les blocs posés par les joueurs (minerais, bois, etc.) dans le PersistentDataContainer du chunk.
 * Contrairement aux métadonnées Bukkit, cette information survit aux redémarrages et aux déchargements de chunks.
 * <p>
 * Les marques d'un chunk sont gardées en mémoire dans un ensemble (lecture et écriture en O(1), sans recopier un tableau à chaque pose ou
 * casse) ; elles sont écrites dans le chunk quand il se décharge, toutes les 5 minutes et à l'arrêt. Une instance unique est partagée
 * ({@link #shared(Plugin)}) pour que contrats et métiers voient les mêmes marques.
 */
public final class PlacedBlockTracker implements Listener {
   private static PlacedBlockTracker instance;

   private record ChunkId(UUID world, int x, int z) {
   }

   private static final class Marks {
      final Set<Integer> codes = new HashSet<>();
      boolean dirty;
   }

   private final NamespacedKey key;
   private final Map<ChunkId, Marks> cache = new ConcurrentHashMap<>();

   private PlacedBlockTracker(Plugin plugin) {
      this.key = new NamespacedKey(plugin, "placed_blocks");
   }

   /** Instance unique ; la première demande enregistre l'écouteur de déchargement et la sauvegarde périodique. */
   public static synchronized PlacedBlockTracker shared(Plugin plugin) {
      if (instance == null) {
         instance = new PlacedBlockTracker(plugin);
         Bukkit.getPluginManager().registerEvents(instance, plugin);
         Bukkit.getScheduler().runTaskTimer(plugin, instance::flushAll, 6000L, 6000L);
      }

      return instance;
   }

   /** Écrit toutes les marques modifiées dans leurs chunks (arrêt du plugin). */
   public static synchronized void flushShared() {
      if (instance != null) {
         instance.flushAll();
      }
   }

   private static int encode(Block block) {
      return (block.getY() + 2048) << 8 | (block.getX() & 15) << 4 | block.getZ() & 15;
   }

   private Marks marks(Block block) {
      Chunk chunk = block.getChunk();
      ChunkId id = new ChunkId(chunk.getWorld().getUID(), chunk.getX(), chunk.getZ());
      return this.cache.computeIfAbsent(id, k -> this.load(chunk));
   }

   private Marks load(Chunk chunk) {
      Marks marks = new Marks();
      long[] stored = chunk.getPersistentDataContainer().get(this.key, PersistentDataType.LONG_ARRAY);
      if (stored != null) {
         for (long v : stored) {
            marks.codes.add((int)v);
         }
      }

      return marks;
   }

   private void write(Chunk chunk, Marks marks) {
      if (!marks.dirty) {
         return;
      }

      PersistentDataContainer pdc = chunk.getPersistentDataContainer();
      if (marks.codes.isEmpty()) {
         pdc.remove(this.key);
      } else {
         long[] out = new long[marks.codes.size()];
         int i = 0;
         for (int code : marks.codes) {
            out[i++] = code;
         }

         pdc.set(this.key, PersistentDataType.LONG_ARRAY, out);
      }

      marks.dirty = false;
   }

   private void flushAll() {
      for (Map.Entry<ChunkId, Marks> e : this.cache.entrySet()) {
         if (e.getValue().dirty) {
            World world = Bukkit.getWorld(e.getKey().world());
            if (world != null && world.isChunkLoaded(e.getKey().x(), e.getKey().z())) {
               this.write(world.getChunkAt(e.getKey().x(), e.getKey().z()), e.getValue());
            }
         }
      }
   }

   @EventHandler
   public void onUnload(ChunkUnloadEvent event) {
      Chunk chunk = event.getChunk();
      Marks marks = this.cache.remove(new ChunkId(chunk.getWorld().getUID(), chunk.getX(), chunk.getZ()));
      if (marks != null) {
         this.write(chunk, marks);
      }
   }

   public boolean isPlaced(Block block) {
      return this.marks(block).codes.contains(encode(block));
   }

   public void mark(Block block) {
      Marks marks = this.marks(block);
      if (marks.codes.add(encode(block))) {
         marks.dirty = true;
      }
   }

   /** @return true si le bloc était marqué (et ne l'est plus). */
   public boolean unmark(Block block) {
      Marks marks = this.marks(block);
      if (marks.codes.remove(encode(block))) {
         marks.dirty = true;
         return true;
      }

      return false;
   }
}
