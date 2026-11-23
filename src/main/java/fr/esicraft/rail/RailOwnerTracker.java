package fr.esicraft.rail;

import fr.esicraft.EsicraftPlugin;
import fr.esicraft.guild.Guild;
import fr.esicraft.treaty.Treaty;
import fr.esicraft.treaty.TreatyType;
import fr.esicraft.util.ChatUtil;
import fr.esicraft.util.ChunkKey;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import org.bukkit.Bukkit;
import org.bukkit.Chunk;
import org.bukkit.NamespacedKey;
import org.bukkit.Tag;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockExplodeEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.entity.EntityExplodeEvent;
import org.bukkit.event.world.ChunkUnloadEvent;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;

/**
 * Propriété des rails : un rail posé par un membre de guilde appartient à sa guilde. Hors de tout territoire (terrain neutre compris),
 * personne d'une autre guilde ne peut le casser à la main : il faut de la TNT (ou une autre explosion) pour saboter une ligne. Les alliés et
 * vassaux (traité) peuvent en revanche modifier la ligne, comme les opérateurs. Sur un territoire de guilde, ce sont les règles de protection
 * du territoire qui s'appliquent déjà.
 * <p>
 * Les propriétaires sont gardés dans le {@link PersistentDataContainer} du chunk (comme les blocs posés par les joueurs) : ils survivent aux
 * redémarrages. Un rail posé avant cette règle, ou par un joueur sans guilde, n'a pas de propriétaire et reste cassable par tous.
 */
public final class RailOwnerTracker implements Listener {
   private record ChunkId(UUID world, int x, int z) {
   }

   private static final class Owners {
      final Map<Integer, String> byCode = new HashMap<>();
      boolean dirty;
   }

   private final EsicraftPlugin plugin;
   private final NamespacedKey key;
   private final Map<ChunkId, Owners> cache = new ConcurrentHashMap<>();

   public RailOwnerTracker(EsicraftPlugin plugin) {
      this.plugin = plugin;
      this.key = new NamespacedKey(plugin, "rail_owners");
   }

   public void init() {
      Bukkit.getScheduler().runTaskTimer(this.plugin, this::flushAll, 6000L, 6000L);
   }

   /** Écrit les propriétaires modifiés dans leurs chunks (arrêt du plugin). */
   public void shutdown() {
      this.flushAll();
   }

   private boolean enabled() {
      return this.plugin.getConfig().getBoolean("rail.protection.enabled", true);
   }

   private static int encode(Block block) {
      return (block.getY() + 2048) << 8 | (block.getX() & 15) << 4 | block.getZ() & 15;
   }

   private Owners owners(Block block) {
      Chunk chunk = block.getChunk();
      ChunkId id = new ChunkId(chunk.getWorld().getUID(), chunk.getX(), chunk.getZ());
      return this.cache.computeIfAbsent(id, k -> this.load(chunk));
   }

   private Owners load(Chunk chunk) {
      Owners owners = new Owners();
      String stored = chunk.getPersistentDataContainer().get(this.key, PersistentDataType.STRING);
      if (stored != null && !stored.isEmpty()) {
         for (String entry : stored.split(";")) {
            int eq = entry.indexOf('=');
            if (eq <= 0) {
               continue;
            }

            try {
               owners.byCode.put(Integer.parseInt(entry.substring(0, eq)), entry.substring(eq + 1));
            } catch (NumberFormatException ignored) {
               // entrée illisible : ignorée
            }
         }
      }

      return owners;
   }

   private void write(Chunk chunk, Owners owners) {
      if (!owners.dirty) {
         return;
      }

      PersistentDataContainer pdc = chunk.getPersistentDataContainer();
      if (owners.byCode.isEmpty()) {
         pdc.remove(this.key);
      } else {
         StringBuilder out = new StringBuilder();
         for (Map.Entry<Integer, String> e : owners.byCode.entrySet()) {
            if (out.length() > 0) {
               out.append(';');
            }

            out.append(e.getKey()).append('=').append(e.getValue());
         }

         pdc.set(this.key, PersistentDataType.STRING, out.toString());
      }

      owners.dirty = false;
   }

   private void flushAll() {
      for (Map.Entry<ChunkId, Owners> e : this.cache.entrySet()) {
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
      Owners owners = this.cache.remove(new ChunkId(chunk.getWorld().getUID(), chunk.getX(), chunk.getZ()));
      if (owners != null) {
         this.write(chunk, owners);
      }
   }

   /** Identifiant de la guilde propriétaire du rail, ou {@code null} s'il n'en a pas. */
   public String ownerOf(Block block) {
      return this.owners(block).byCode.get(encode(block));
   }

   private void setOwner(Block block, String guildId) {
      Owners owners = this.owners(block);
      String previous = guildId == null ? owners.byCode.remove(encode(block)) : owners.byCode.put(encode(block), guildId);
      if (previous == null ? guildId != null : !previous.equals(guildId)) {
         owners.dirty = true;
      }
   }

   // ------------------------------------------------------------------ règles

   @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
   public void onPlace(BlockPlaceEvent event) {
      Block block = event.getBlock();
      if (!Tag.RAILS.isTagged(block.getType())) {
         return;
      }

      Guild guild = this.plugin.getGuildManager().getGuildByPlayer(event.getPlayer().getUniqueId());
      this.setOwner(block, guild != null ? guild.getId() : null);
   }

   private boolean friendly(String guildId, String ownerId) {
      Treaty treaty = this.plugin.getTreatyManager().between(guildId, ownerId);
      return treaty != null && treaty.getType() != TreatyType.PACT;
   }

   @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
   public void onBreak(BlockBreakEvent event) {
      Block block = event.getBlock();
      if (!this.enabled() || !Tag.RAILS.isTagged(block.getType())) {
         return;
      }

      String ownerId = this.ownerOf(block);
      if (ownerId == null) {
         return;
      }

      Guild owner = this.plugin.getGuildManager().getGuildById(ownerId);
      if (owner == null) {
         this.setOwner(block, null);
         return;
      }

      // Sur un territoire de guilde, la protection du territoire décide déjà qui peut casser quoi.
      if (this.plugin.getGuildManager().getGuildByChunk(ChunkKey.from(block.getChunk())) != null) {
         return;
      }

      Player player = event.getPlayer();
      if (player.hasPermission("esicraft.admin")) {
         return;
      }

      Guild guild = this.plugin.getGuildManager().getGuildByPlayer(player.getUniqueId());
      if (guild != null && (guild.getId().equals(ownerId) || this.friendly(guild.getId(), ownerId))) {
         return;
      }

      event.setCancelled(true);
      ChatUtil.sendActionbar(player, "&c❌ Ces rails appartiennent à la guilde &6" + owner.getName() + " &c: seule la TNT peut les saboter !");
   }

   /** Un rail cassé pour de bon n'a plus de propriétaire. */
   @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
   public void onBroken(BlockBreakEvent event) {
      if (Tag.RAILS.isTagged(event.getBlock().getType())) {
         this.setOwner(event.getBlock(), null);
      }
   }

   private void forgetDestroyed(List<Block> blocks) {
      for (Block block : blocks) {
         if (Tag.RAILS.isTagged(block.getType())) {
            this.setOwner(block, null);
         }
      }
   }

   @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
   public void onExplode(EntityExplodeEvent event) {
      this.forgetDestroyed(event.blockList());
   }

   @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
   public void onBlockExplode(BlockExplodeEvent event) {
      this.forgetDestroyed(event.blockList());
   }
}
