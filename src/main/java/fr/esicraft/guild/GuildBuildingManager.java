package fr.esicraft.guild;

import fr.esicraft.EsicraftPlugin;
import fr.esicraft.util.ChatUtil;
import fr.esicraft.util.ChunkKey;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ThreadLocalRandom;
import org.bukkit.Bukkit;
import org.bukkit.ChunkSnapshot;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.Tag;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.BlockState;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.AbstractHorse;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.enchantment.EnchantItemEvent;
import org.bukkit.event.inventory.PrepareAnvilEvent;
import org.bukkit.event.player.PlayerExpChangeEvent;
import org.bukkit.event.player.PlayerItemDamageEvent;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.jetbrains.annotations.NotNull;

/**
 * Bâtiments de guilde : comme la taverne, un bâtiment est un chunk du territoire (jamais celui du Nexus) qui contient certains blocs.
 * Chaque guilde peut en avoir un de chaque type ; tant qu'il reste conforme, il donne un bonus à ses membres.
 * <ul>
 *    <li><b>Forge</b> : enclume, haut fourneau et table de forgeron, sous un toit. Outils et armures des membres s'usent moins ; les enclumes de la forge
 *    coûtent moitié moins d'expérience.</li>
 *    <li><b>Bibliothèque</b> : table d'enchantement sous un toit, pupitre et 15 bibliothèques. Les membres gagnent plus d'expérience ; enchanter dans la
 *    bibliothèque rend un niveau.</li>
 *    <li><b>Écurie</b> : 6 bottes de foin et 8 clôtures. Les montures des membres galopent plus vite et se soignent.</li>
 * </ul>
 */
public class GuildBuildingManager implements CommandExecutor, TabCompleter, Listener {
   public enum Type {
      FORGE("Forge", "forge", "⚒", "enclume, haut fourneau et table de forgeron (haut fourneau sous un toit)",
         "outils et armures s'usent 20 % moins vite ; enclumes de la forge à moitié prix en expérience"),
      LIBRARY("Bibliothèque", "bibliotheque", "📚", "table d'enchantement sous un toit, pupitre et 15 bibliothèques",
         "+15 % d'expérience pour les membres ; enchanter dans la bibliothèque rend 1 niveau"),
      STABLE("Écurie", "ecurie", "🐎", "6 bottes de foin et 8 clôtures (ou portillons)",
         "les montures des membres reçoivent Vitesse et Régénération");

      final String label;
      final String key;
      final String icon;
      final String needs;
      final String bonus;

      Type(String label, String key, String icon, String needs, String bonus) {
         this.label = label;
         this.key = key;
         this.icon = icon;
         this.needs = needs;
         this.bonus = bonus;
      }

      public String label() {
         return this.label;
      }

      static Type parse(String s) {
         String k = s.toLowerCase(Locale.ROOT).replace("é", "e").replace("è", "e").replace("è", "e");
         return switch (k) {
            case "forge" -> FORGE;
            case "bibliotheque", "biblio", "library" -> LIBRARY;
            case "ecurie", "stable", "ecuries" -> STABLE;
            default -> null;
         };
      }
   }

   private static final class Building {
      final String guildId;
      final Type type;
      boolean active = true;
      long checkedAt;

      Building(String guildId, Type type) {
         this.guildId = guildId;
         this.type = type;
      }
   }

   private static final long RECHECK_MS = 5 * 60_000L;
   private final EsicraftPlugin plugin;
   private final Map<ChunkKey, Building> buildings = new ConcurrentHashMap<>();

   public GuildBuildingManager(EsicraftPlugin plugin) {
      this.plugin = plugin;
   }

   public void init() {
      var db = this.plugin.getDataManager().getStorage();
      db.exec("CREATE TABLE IF NOT EXISTS guild_buildings (world VARCHAR(64), cx INT, cz INT, guild_id VARCHAR(64), type VARCHAR(16), PRIMARY KEY (world, cx, cz));");
      for (Object[] row : db.select("SELECT world, cx, cz, guild_id, type FROM guild_buildings;", 5)) {
         try {
            this.buildings.put(new ChunkKey(String.valueOf(row[0]), ((Number)row[1]).intValue(), ((Number)row[2]).intValue()), new Building(String.valueOf(row[3]), Type.valueOf(String.valueOf(row[4]))));
         } catch (IllegalArgumentException ignored) {
         }
      }

      Bukkit.getScheduler().runTaskTimer(this.plugin, this::mountTick, 100L, 100L);
   }

   public void clear() {
      this.buildings.clear();
   }

   public boolean isBuilding(ChunkKey key) {
      return this.buildings.containsKey(key);
   }

   private void delete(ChunkKey key) {
      this.buildings.remove(key);
      this.plugin.getDataManager().getStorage().exec("DELETE FROM guild_buildings WHERE world = ? AND cx = ? AND cz = ?;", key.getWorldName(), key.getX(), key.getZ());
   }

   /** Bâtiment valide (claim toujours détenu par la guilde) ; supprime ceux dont le territoire a été perdu. */
   private Building owned(ChunkKey key) {
      Building b = this.buildings.get(key);
      if (b == null) {
         return null;
      }

      Guild claimant = this.plugin.getGuildManager().getGuildByChunk(key);
      if (claimant == null || !claimant.getId().equals(b.guildId)) {
         this.delete(key);
         return null;
      }

      return b;
   }

   private ChunkKey find(String guildId, Type type) {
      for (Map.Entry<ChunkKey, Building> e : this.buildings.entrySet()) {
         if (e.getValue().guildId.equals(guildId) && e.getValue().type == type) {
            return e.getKey();
         }
      }

      return null;
   }

   /** Vrai si la guilde possède ce bâtiment et qu'il est toujours conforme (revérifié toutes les 5 minutes quand son chunk est chargé). */
   public boolean isActive(Guild guild, Type type) {
      if (guild == null || this.buildings.isEmpty()) {
         return false;
      }

      ChunkKey key = this.find(guild.getId(), type);
      Building b = key != null ? this.owned(key) : null;
      if (b == null) {
         return false;
      }

      long now = System.currentTimeMillis();
      if (now - b.checkedAt > RECHECK_MS) {
         World w = Bukkit.getWorld(key.getWorldName());
         if (w != null && w.isChunkLoaded(key.getX(), key.getZ())) {
            b.checkedAt = now;
            boolean was = b.active;
            b.active = this.problem(key, type) == null;
            if (was && !b.active) {
               ChatUtil.broadcastGuild(guild, "&c" + type.icon + " Votre " + type.label + " n'est plus conforme : son bonus est suspendu (&e/batiment&c pour voir ce qui manque).");
            } else if (!was && b.active) {
               ChatUtil.broadcastGuild(guild, "&a" + type.icon + " Votre " + type.label + " est de nouveau conforme : son bonus est rétabli.");
            }
         }
      }

      return b.active;
   }

   private boolean memberHas(Player p, Type type) {
      return this.isActive(this.plugin.getGuildManager().getGuildByPlayer(p.getUniqueId()), type);
   }

   /** Le joueur se trouve-t-il dans le bâtiment de ce type de sa propre guilde ? */
   private boolean insideOwn(Player p, Block where, Type type) {
      ChunkKey key = ChunkKey.from(where.getChunk());
      Building b = this.owned(key);
      Guild g = this.plugin.getGuildManager().getGuildByPlayer(p.getUniqueId());
      return b != null && b.type == type && g != null && g.getId().equals(b.guildId) && this.isActive(g, type);
   }

   // ------------------------------------------------------------------ conformité

   /** Ce qui manque au chunk pour abriter ce bâtiment, ou null s'il est conforme. */
   private String problem(ChunkKey key, Type type) {
      World world = Bukkit.getWorld(key.getWorldName());
      if (world == null || !world.isChunkLoaded(key.getX(), key.getZ())) {
         return "chunk non chargé.";
      }

      var chunk = world.getChunkAt(key.getX(), key.getZ());
      boolean blast = false;
      boolean roofedBlast = false;
      boolean enchant = false;
      boolean roofedEnchant = false;
      boolean lectern = false;
      for (BlockState state : chunk.getTileEntities()) {
         Material m = state.getType();
         if (m == Material.BLAST_FURNACE) {
            blast = true;
            roofedBlast |= roofed(state.getBlock());
         } else if (m == Material.ENCHANTING_TABLE) {
            enchant = true;
            roofedEnchant |= roofed(state.getBlock());
         } else if (m == Material.LECTERN) {
            lectern = true;
         }
      }

      int anvils = 0;
      int smithing = 0;
      int shelves = 0;
      int hay = 0;
      int fences = 0;
      ChunkSnapshot snap = chunk.getChunkSnapshot(false, false, false);
      int min = world.getMinHeight();
      int max = world.getMaxHeight();
      for (int x = 0; x < 16; x++) {
         for (int z = 0; z < 16; z++) {
            int top = Math.min(max - 1, snap.getHighestBlockYAt(x, z) + 1);
            for (int y = min; y <= top; y++) {
               Material m = snap.getBlockType(x, y, z);
               if (m == Material.AIR || m == Material.STONE || m == Material.DEEPSLATE || m == Material.DIRT || m == Material.WATER) {
                  continue;
               }

               if (m == Material.ANVIL || m == Material.CHIPPED_ANVIL || m == Material.DAMAGED_ANVIL) {
                  anvils++;
               } else if (m == Material.SMITHING_TABLE) {
                  smithing++;
               } else if (m == Material.BOOKSHELF || m == Material.CHISELED_BOOKSHELF) {
                  shelves++;
               } else if (m == Material.HAY_BLOCK) {
                  hay++;
               } else if (Tag.FENCES.isTagged(m) || Tag.FENCE_GATES.isTagged(m)) {
                  fences++;
               }
            }
         }
      }

      return switch (type) {
         case FORGE -> anvils == 0 ? "il faut une enclume." : !blast ? "il faut un haut fourneau." : smithing == 0 ? "il faut une table de forgeron."
            : !roofedBlast ? "le haut fourneau doit être sous un toit." : null;
         case LIBRARY -> !enchant ? "il faut une table d'enchantement." : !roofedEnchant ? "la table d'enchantement doit être sous un toit." : !lectern ? "il faut un pupitre."
            : shelves < 15 ? "il faut 15 bibliothèques (" + shelves + " trouvées)." : null;
         case STABLE -> hay < 6 ? "il faut 6 bottes de foin (" + hay + " trouvées)." : fences < 8 ? "il faut 8 clôtures ou portillons (" + fences + " trouvés)." : null;
      };
   }

   private static boolean roofed(Block b) {
      for (int dy = 1; dy <= 12 && b.getY() + dy < b.getWorld().getMaxHeight(); dy++) {
         if (b.getRelative(0, dy, 0).getType().isOccluding()) {
            return true;
         }
      }

      return false;
   }

   // ------------------------------------------------------------------ commande

   public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
      if (!(sender instanceof Player player)) {
         ChatUtil.sendMessage(sender, "&cSeul un joueur peut exécuter cette commande.");
         return true;
      }

      ChunkKey here = ChunkKey.from(player.getLocation().getChunk());
      String sub = args.length == 0 ? "liste" : args[0].toLowerCase(Locale.ROOT);
      switch (sub) {
         case "declarer", "déclarer", "construire", "creer" -> {
            Type type = args.length >= 2 ? Type.parse(args[1]) : null;
            if (type == null) {
               ChatUtil.sendMessage(player, "&cUtilisation : &e/batiment declarer <forge|bibliotheque|ecurie>");
            } else {
               this.declare(player, here, type);
            }
         }
         case "retirer", "supprimer" -> this.remove(player, here);
         default -> this.list(player);
      }

      return true;
   }

   private void list(Player player) {
      Guild guild = this.plugin.getGuildManager().getGuildByPlayer(player.getUniqueId());
      ChatUtil.sendMessage(player, "&6&l🏛 Bâtiments de guilde &7— un chunk de votre territoire (hors Nexus) qui contient les bons blocs. Un de chaque par guilde.");
      for (Type t : Type.values()) {
         String status;
         ChunkKey key = guild != null ? this.find(guild.getId(), t) : null;
         if (key == null || this.owned(key) == null) {
            status = "&8(non construit)";
         } else {
            Building b = this.buildings.get(key);
            this.isActive(guild, t);
            String problem = this.problem(key, t);
            status = (b != null && b.active && problem == null ? "&a✔ actif" : "&c✖ suspendu" + (problem != null && !problem.startsWith("chunk") ? " : " + problem : ""))
               + " &8(chunk " + key.getX() + ", " + key.getZ() + ")";
         }

         ChatUtil.sendRaw(player, ChatUtil.color(" &e" + t.icon + " " + t.label + " " + status));
         ChatUtil.sendRaw(player, ChatUtil.color("   &7Blocs : &f" + t.needs));
         ChatUtil.sendRaw(player, ChatUtil.color("   &7Bonus : &a" + t.bonus));
      }

      ChatUtil.sendRaw(player, ChatUtil.color("&7Officiers : &e/batiment declarer <forge|bibliotheque|ecurie> &7dans le chunk, &e/batiment retirer &7pour le libérer."));
   }

   private void declare(Player player, ChunkKey here, Type type) {
      Guild guild = this.plugin.getGuildManager().getGuildByPlayer(player.getUniqueId());
      if (guild == null || !guild.hasRoleAtLeast(player.getUniqueId(), GuildRole.OFFICER)) {
         ChatUtil.sendMessage(player, "&c❌ Réservé aux officiers d'une guilde.");
         return;
      }

      if (!guild.isClaimed(here)) {
         ChatUtil.sendMessage(player, "&c❌ Ce chunk n'appartient pas à votre guilde.");
         return;
      }

      if (this.plugin.getNexusManager().isNexusChunk(here)) {
         ChatUtil.sendMessage(player, "&c❌ Le chunk du Nexus ne peut pas accueillir de bâtiment.");
         return;
      }

      if (this.plugin.getTavern().tavernOwner(here) != null) {
         ChatUtil.sendMessage(player, "&c❌ Ce chunk est déjà une taverne.");
         return;
      }

      if (this.owned(here) != null) {
         ChatUtil.sendMessage(player, "&c❌ Ce chunk abrite déjà un bâtiment (&e/batiment retirer&c d'abord).");
         return;
      }

      ChunkKey existing = this.find(guild.getId(), type);
      if (existing != null && this.owned(existing) != null) {
         ChatUtil.sendMessage(player, "&c❌ Votre guilde a déjà une " + type.label + " (chunk " + existing.getX() + ", " + existing.getZ() + ").");
         return;
      }

      String problem = this.problem(here, type);
      if (problem != null) {
         ChatUtil.sendMessage(player, "&c❌ Ce chunk ne fait pas encore une " + type.label + " : " + problem);
         return;
      }

      Building b = new Building(guild.getId(), type);
      b.checkedAt = System.currentTimeMillis();
      this.buildings.put(here, b);
      this.plugin.getDataManager().getStorage().exec("INSERT OR REPLACE INTO guild_buildings (world, cx, cz, guild_id, type) VALUES (?, ?, ?, ?, ?);",
         here.getWorldName(), here.getX(), here.getZ(), guild.getId(), type.name());
      ChatUtil.playSound(player, Sound.BLOCK_ANVIL_USE, 0.8F, 1.2F);
      ChatUtil.broadcastGuild(guild, "&6" + type.icon + " " + player.getName() + " a inauguré la &e" + type.label + "&6 de la guilde ! &7Bonus : &a" + type.bonus + "&7.");
   }

   private void remove(Player player, ChunkKey here) {
      Building b = this.owned(here);
      Guild guild = this.plugin.getGuildManager().getGuildByPlayer(player.getUniqueId());
      if (b == null || guild == null || !guild.getId().equals(b.guildId) || !guild.hasRoleAtLeast(player.getUniqueId(), GuildRole.OFFICER)) {
         ChatUtil.sendMessage(player, "&c❌ Vous devez être officier de la guilde propriétaire, dans le chunk du bâtiment.");
         return;
      }

      this.delete(here);
      ChatUtil.sendMessage(player, "&7La " + b.type.label + " a été retirée.");
   }

   public List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command command, @NotNull String alias, @NotNull String[] args) {
      if (args.length == 1) {
         return List.of("liste", "declarer", "retirer");
      }

      return args.length == 2 && args[0].toLowerCase(Locale.ROOT).startsWith("decl") ? List.of("forge", "bibliotheque", "ecurie") : List.of();
   }

   // ------------------------------------------------------------------ bonus

   /** Forge : 20 % de chances qu'un outil ou une armure d'un membre ne perde pas de durabilité. */
   @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
   public void onItemDamage(PlayerItemDamageEvent event) {
      if (!this.buildings.isEmpty() && ThreadLocalRandom.current().nextDouble() < 0.20 && this.memberHas(event.getPlayer(), Type.FORGE)) {
         event.setCancelled(true);
      }
   }

   /** Forge : les enclumes du chunk de la forge coûtent moitié moins d'expérience aux membres. */
   @EventHandler(priority = EventPriority.HIGH)
   public void onAnvil(PrepareAnvilEvent event) {
      if (this.buildings.isEmpty() || !(event.getView().getPlayer() instanceof Player p) || event.getInventory().getLocation() == null) {
         return;
      }

      if (this.insideOwn(p, event.getInventory().getLocation().getBlock(), Type.FORGE)) {
         int cost = event.getView().getRepairCost();
         if (cost > 1) {
            event.getView().setRepairCost(Math.max(1, cost / 2));
         }
      }
   }

   /** Bibliothèque : +15 % d'expérience (arrondi aléatoire pour que les petits orbes en profitent aussi). */
   @EventHandler(priority = EventPriority.HIGH)
   public void onExp(PlayerExpChangeEvent event) {
      if (this.buildings.isEmpty() || event.getAmount() <= 0 || !this.memberHas(event.getPlayer(), Type.LIBRARY)) {
         return;
      }

      double boosted = event.getAmount() * 1.15;
      int whole = (int)boosted;
      if (ThreadLocalRandom.current().nextDouble() < boosted - whole) {
         whole++;
      }

      event.setAmount(whole);
   }

   /** Bibliothèque : enchanter sur la table de la bibliothèque rend un niveau. */
   @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
   public void onEnchant(EnchantItemEvent event) {
      Player p = event.getEnchanter();
      if (!this.buildings.isEmpty() && this.insideOwn(p, event.getEnchantBlock(), Type.LIBRARY)) {
         UUID id = p.getUniqueId();
         Bukkit.getScheduler().runTask(this.plugin, () -> {
            Player online = Bukkit.getPlayer(id);
            if (online != null) {
               online.giveExpLevels(1);
               ChatUtil.sendActionbar(online, "&b📚 La bibliothèque vous rend un niveau.");
            }
         });
      }
   }

   /** Écurie : Vitesse et Régénération pour les montures des membres. */
   private void mountTick() {
      if (this.buildings.isEmpty()) {
         return;
      }

      for (Player p : Bukkit.getOnlinePlayers()) {
         Entity mount = p.getVehicle();
         if (mount instanceof LivingEntity living && !(mount instanceof Player) && (mount instanceof AbstractHorse || mount.getType().name().equals("PIG") || mount.getType().name().equals("STRIDER")
            || mount.getType().name().equals("CAMEL")) && this.memberHas(p, Type.STABLE)) {
            living.addPotionEffect(new PotionEffect(PotionEffectType.SPEED, 140, 0, true, false));
            living.addPotionEffect(new PotionEffect(PotionEffectType.REGENERATION, 140, 0, true, false));
         }
      }
   }

   /** Liste lisible des bâtiments d'une guilde (pour /g info, le site…). */
   public List<String> describe(Guild guild) {
      List<String> out = new ArrayList<>();
      for (Type t : Type.values()) {
         if (this.isActive(guild, t)) {
            out.add(t.icon + " " + t.label);
         }
      }

      return out;
   }
}
