package fr.esicraft.util;

import java.util.Arrays;
import org.bukkit.Chunk;
import org.bukkit.NamespacedKey;
import org.bukkit.block.Block;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.Plugin;

/**
 * Mémorise les blocs posés par les joueurs (minerais, bois, etc.) dans le PersistentDataContainer du chunk.
 * Contrairement aux métadonnées Bukkit, cette information survit aux redémarrages et aux déchargements de chunks.
 */
public final class PlacedBlockTracker {
   private final NamespacedKey key;

   public PlacedBlockTracker(Plugin plugin) {
      this.key = new NamespacedKey(plugin, "placed_blocks");
   }

   private static long encode(Block block) {
      long y = (long)(block.getY() + 2048);
      return y << 8 | (long)((block.getX() & 15) << 4) | (long)(block.getZ() & 15);
   }

   private long[] read(PersistentDataContainer pdc) {
      long[] arr = pdc.get(this.key, PersistentDataType.LONG_ARRAY);
      return arr != null ? arr : new long[0];
   }

   public boolean isPlaced(Block block) {
      long code = encode(block);

      for (long v : this.read(block.getChunk().getPersistentDataContainer())) {
         if (v == code) {
            return true;
         }
      }

      return false;
   }

   public void mark(Block block) {
      Chunk chunk = block.getChunk();
      PersistentDataContainer pdc = chunk.getPersistentDataContainer();
      long code = encode(block);
      long[] arr = this.read(pdc);

      for (long v : arr) {
         if (v == code) {
            return;
         }
      }

      long[] next = Arrays.copyOf(arr, arr.length + 1);
      next[arr.length] = code;
      pdc.set(this.key, PersistentDataType.LONG_ARRAY, next);
   }

   /** @return true si le bloc était marqué (et ne l'est plus). */
   public boolean unmark(Block block) {
      Chunk chunk = block.getChunk();
      PersistentDataContainer pdc = chunk.getPersistentDataContainer();
      long code = encode(block);
      long[] arr = this.read(pdc);
      int idx = -1;

      for (int i = 0; i < arr.length; i++) {
         if (arr[i] == code) {
            idx = i;
            break;
         }
      }

      if (idx < 0) {
         return false;
      } else {
         if (arr.length == 1) {
            pdc.remove(this.key);
         } else {
            long[] next = new long[arr.length - 1];
            System.arraycopy(arr, 0, next, 0, idx);
            System.arraycopy(arr, idx + 1, next, idx, arr.length - idx - 1);
            pdc.set(this.key, PersistentDataType.LONG_ARRAY, next);
         }

         return true;
      }
   }
}
