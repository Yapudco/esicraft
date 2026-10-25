package fr.esicraft.util;

import java.util.Objects;
import org.bukkit.Chunk;

public final class ChunkKey {
   private final String worldName;
   private final int x;
   private final int z;
   private final long packedKey;

   public ChunkKey(String worldName, int x, int z) {
      this.worldName = worldName;
      this.x = x;
      this.z = z;
      this.packedKey = pack(x, z);
   }

   public static ChunkKey from(Chunk chunk) {
      return new ChunkKey(chunk.getWorld().getName(), chunk.getX(), chunk.getZ());
   }

   public static ChunkKey fromString(String str) {
      String[] parts = str.split(";");
      if (parts.length == 3) {
         return new ChunkKey(parts[0], Integer.parseInt(parts[1]), Integer.parseInt(parts[2]));
      } else {
         throw new IllegalArgumentException("Format invalide pour ChunkKey: " + str);
      }
   }

   public static long pack(int x, int z) {
      return (long)x << 32 | (long)z & 4294967295L;
   }

   public String getWorldName() {
      return this.worldName;
   }

   public int getX() {
      return this.x;
   }

   public int getZ() {
      return this.z;
   }

   public long getPackedKey() {
      return this.packedKey;
   }

   @Override
   public String toString() {
      return this.worldName + ";" + this.x + ";" + this.z;
   }

   @Override
   public boolean equals(Object o) {
      if (this == o) {
         return true;
      } else if (o != null && this.getClass() == o.getClass()) {
         ChunkKey chunkKey = (ChunkKey)o;
         return this.x == chunkKey.x && this.z == chunkKey.z && Objects.equals(this.worldName, chunkKey.worldName);
      } else {
         return false;
      }
   }

   @Override
   public int hashCode() {
      return Objects.hash(this.worldName, this.x, this.z);
   }
}
