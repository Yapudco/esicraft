package fr.esicraft.game;

import fr.esicraft.EsicraftPlugin;
import fr.esicraft.data.PlayerData;
import fr.esicraft.gui.holder.BaseGUIHolder;
import fr.esicraft.guild.Guild;
import fr.esicraft.guild.GuildRole;
import fr.esicraft.guild.GuildSpecialization;
import fr.esicraft.util.ChatUtil;
import fr.esicraft.util.ChunkKey;
import fr.esicraft.util.ItemBuilder;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ThreadLocalRandom;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.World;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.bukkit.entity.Projectile;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.PotionSplashEvent;
import org.bukkit.event.entity.ProjectileLaunchEvent;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;

/**
 * Tavernes : le Syndicat Marchand et l'Enclave Champêtre peuvent déclarer certains chunks de leur territoire (jamais celui du Nexus) zones neutres.
 * Le PvP, les armes et les projectiles y sont désactivés ; on y joue à pile ou face contre la banque de la guilde propriétaire.
 */
public class TavernManager implements CommandExecutor, TabCompleter, Listener {
   private final EsicraftPlugin plugin;
   private final Map<ChunkKey, String> taverns = new ConcurrentHashMap<>();
   private final Map<java.util.UUID, ChunkKey> lastChunk = new ConcurrentHashMap<>();
   private TavernGames games;

   public TavernManager(EsicraftPlugin plugin) {
      this.plugin = plugin;
   }

   public void init() {
      this.games = new TavernGames(this.plugin);
      Bukkit.getPluginManager().registerEvents(this.games, this.plugin);
      var db = this.plugin.getDataManager().getStorage();
      db.exec("CREATE TABLE IF NOT EXISTS taverns (world VARCHAR(64), cx INT, cz INT, guild_id VARCHAR(64), PRIMARY KEY (world, cx, cz));");
      for (Object[] row : db.select("SELECT world, cx, cz, guild_id FROM taverns;", 4)) {
         this.taverns.put(new ChunkKey(String.valueOf(row[0]), ((Number)row[1]).intValue(), ((Number)row[2]).intValue()), String.valueOf(row[3]));
      }
   }

   public void clear() {
      this.taverns.clear();
      this.lastChunk.clear();
   }

   /** Guilde propriétaire de la taverne de ce chunk (null si ce n'en est pas une, ou si le claim a été perdu). */
   public String tavernOwner(ChunkKey key) {
      String owner = this.taverns.get(key);
      if (owner == null) {
         return null;
      }

      Guild claimant = this.plugin.getGuildManager().getGuildByChunk(key);
      if (claimant == null || !claimant.getId().equals(owner)) {
         this.taverns.remove(key);
         this.plugin.getDataManager().getStorage().exec("DELETE FROM taverns WHERE world = ? AND cx = ? AND cz = ?;", key.getWorldName(), key.getX(), key.getZ());
         return null;
      }

      return owner;
   }

   public boolean isTavern(org.bukkit.Location loc) {
      return loc.getWorld() != null && !this.taverns.isEmpty() && this.tavernOwner(new ChunkKey(loc.getWorld().getName(), loc.getBlockX() >> 4, loc.getBlockZ() >> 4)) != null;
   }

   public List<ChunkKey> chunksOf(String guildId) {
      List<ChunkKey> out = new ArrayList<>();
      for (Map.Entry<ChunkKey, String> e : this.taverns.entrySet()) {
         if (e.getValue().equals(guildId)) {
            out.add(e.getKey());
         }
      }

      return out;
   }

   // ------------------------------------------------------------------ commande

   public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
      if (!(sender instanceof Player player)) {
         sender.sendMessage(ChatUtil.color("&cSeul un joueur peut exécuter cette commande."));
         return true;
      }

      String sub = args.length == 0 ? "jeu" : args[0].toLowerCase(Locale.ROOT);
      ChunkKey here = new ChunkKey(player.getWorld().getName(), player.getLocation().getBlockX() >> 4, player.getLocation().getBlockZ() >> 4);
      switch (sub) {
         case "declarer", "déclarer", "creer" -> this.declare(player, here);
         case "retirer", "supprimer" -> this.remove(player, here);
         default -> {
            String owner = this.tavernOwner(here);
            if (owner == null) {
               ChatUtil.sendMessage(player, "&6🍺 Taverne : le Syndicat Marchand et l'Enclave Champêtre peuvent déclarer un chunk de leur territoire zone neutre avec &e/taverne declarer&6. Vous n'êtes dans aucune taverne ici.");
            } else {
               String problem = this.problem(here);
               Guild house = this.plugin.getGuildManager().getGuildById(owner);
               if (problem != null) {
                  ChatUtil.sendMessage(player, "&c🍺 Cette taverne n'est pas en état d'ouvrir ses jeux : " + problem);
               } else if (house != null) {
                  this.games.openMenu(player, house);
               }
            }
         }
      }

      return true;
   }

   private void declare(Player player, ChunkKey here) {
      Guild guild = this.plugin.getGuildManager().getGuildByPlayer(player.getUniqueId());
      if (guild == null || !guild.hasRoleAtLeast(player.getUniqueId(), GuildRole.OFFICER)) {
         ChatUtil.sendMessage(player, "&c❌ Réservé aux officiers d'une guilde.");
         return;
      }

      GuildSpecialization spec = guild.getSpecialization();
      if (spec != GuildSpecialization.MERCHANT_SYNDICATE && spec != GuildSpecialization.DRUIDIC_ENCLAVE) {
         ChatUtil.sendMessage(player, "&c❌ Seuls le Syndicat Marchand et l'Enclave Champêtre peuvent ouvrir des tavernes.");
         return;
      }

      if (!guild.isClaimed(here)) {
         ChatUtil.sendMessage(player, "&c❌ Ce chunk n'appartient pas à votre guilde.");
         return;
      }

      if (this.plugin.getNexusManager().isNexusChunk(here)) {
         ChatUtil.sendMessage(player, "&c❌ Le chunk du Nexus ne peut pas devenir une taverne.");
         return;
      }

      if (this.taverns.containsKey(here)) {
         ChatUtil.sendMessage(player, "&c❌ C'est déjà une taverne.");
         return;
      }

      int max = Math.max(1, this.plugin.getConfig().getInt("taverns.max-per-guild", 2));
      if (this.chunksOf(guild.getId()).size() >= max) {
         ChatUtil.sendMessage(player, "&c❌ Votre guilde possède déjà " + max + " taverne(s).");
         return;
      }

      String problem = this.problem(here);
      if (problem != null) {
         ChatUtil.sendMessage(player, "&c❌ Ce chunk ne ressemble pas encore à une taverne : " + problem);
         return;
      }

      this.taverns.put(here, guild.getId());
      this.plugin.getDataManager().getStorage().exec("INSERT OR REPLACE INTO taverns (world, cx, cz, guild_id) VALUES (?, ?, ?, ?);", here.getWorldName(), here.getX(), here.getZ(), guild.getId());
      this.plugin.getAchievements().grant(player.getUniqueId(), fr.esicraft.life.Achievement.CHAMPION_TAVERN);
      ChatUtil.broadcastGuild(guild, "&6🍺 Une taverne (zone neutre) a été déclarée en &e" + here.getX() + "," + here.getZ() + "&6 : PvP et armes interdits, pile ou face avec &e/taverne&6 !");
   }

   /**
    * Une taverne a besoin, dans son chunk, d'un coffre vide, d'un tonneau (barrel) et d'un toit au-dessus de l'un et de l'autre.
    * Renvoie ce qui manque, ou null si tout est en ordre.
    */
   private String problem(ChunkKey key) {
      World world = Bukkit.getWorld(key.getWorldName());
      if (world == null || !world.isChunkLoaded(key.getX(), key.getZ())) {
         return "chunk non chargé.";
      }

      boolean barrel = false;
      boolean roofedBarrel = false;
      boolean emptyChest = false;
      boolean roofedChest = false;
      for (org.bukkit.block.BlockState state : world.getChunkAt(key.getX(), key.getZ()).getTileEntities()) {
         if (state instanceof org.bukkit.block.Barrel) {
            barrel = true;
            roofedBarrel |= roofed(state.getBlock());
         } else if (state instanceof org.bukkit.block.Chest chest && chest.getBlockInventory().isEmpty()) {
            emptyChest = true;
            roofedChest |= roofed(state.getBlock());
         }
      }

      if (!emptyChest) {
         return "il faut un coffre vide dans le chunk.";
      }

      if (!barrel) {
         return "il faut un tonneau (barrel) dans le chunk.";
      }

      if (!roofedChest || !roofedBarrel) {
         return "le coffre et le tonneau doivent être couverts par un toit.";
      }

      return null;
   }

   /** Un bloc plein au-dessus (jusqu'à 12 blocs) fait office de toit. */
   private static boolean roofed(org.bukkit.block.Block b) {
      for (int dy = 1; dy <= 12 && b.getY() + dy < b.getWorld().getMaxHeight(); dy++) {
         if (b.getRelative(0, dy, 0).getType().isOccluding()) {
            return true;
         }
      }

      return false;
   }

   private void remove(Player player, ChunkKey here) {
      String owner = this.tavernOwner(here);
      Guild guild = this.plugin.getGuildManager().getGuildByPlayer(player.getUniqueId());
      if (owner == null || guild == null || !guild.getId().equals(owner) || !guild.hasRoleAtLeast(player.getUniqueId(), GuildRole.OFFICER)) {
         ChatUtil.sendMessage(player, "&c❌ Vous devez être officier de la guilde propriétaire, dans la taverne.");
         return;
      }

      this.taverns.remove(here);
      this.plugin.getDataManager().getStorage().exec("DELETE FROM taverns WHERE world = ? AND cx = ? AND cz = ?;", here.getWorldName(), here.getX(), here.getZ());
      ChatUtil.sendMessage(player, "&7La taverne a été retirée.");
   }

   public List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command command, @NotNull String alias, @NotNull String[] args) {
      return args.length == 1 ? List.of("declarer", "retirer", "jeu") : List.of();
   }

   // ------------------------------------------------------------------ interdiction du combat

   @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
   public void onDamage(EntityDamageByEntityEvent event) {
      if (this.taverns.isEmpty() || !(event.getEntity() instanceof Player victim)) {
         return;
      }

      Player attacker = fr.esicraft.combat.CombatManager.attackerOf(event.getDamager());
      if (attacker != null && !attacker.equals(victim) && (this.isTavern(victim.getLocation()) || this.isTavern(attacker.getLocation()))) {
         event.setCancelled(true);
         ChatUtil.sendActionbar(attacker, "&6🍺 Zone neutre : ici, on ne se bat pas.");
      }
   }

   @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
   public void onLaunch(ProjectileLaunchEvent event) {
      Projectile proj = event.getEntity();
      if (!this.taverns.isEmpty() && proj.getShooter() instanceof Player shooter && this.isTavern(shooter.getLocation())) {
         event.setCancelled(true);
         ChatUtil.sendActionbar(shooter, "&6🍺 Zone neutre : les armes de tir sont interdites.");
      }
   }

   @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
   public void onSplash(PotionSplashEvent event) {
      if (!this.taverns.isEmpty() && event.getPotion().getShooter() instanceof Player thrower && this.isTavern(thrower.getLocation())) {
         event.setCancelled(true);
      }
   }

   @EventHandler(ignoreCancelled = true)
   public void onMove(PlayerMoveEvent event) {
      if (this.taverns.isEmpty() || event.getTo() == null || event.getFrom().getBlockX() >> 4 == event.getTo().getBlockX() >> 4 && event.getFrom().getBlockZ() >> 4 == event.getTo().getBlockZ() >> 4) {
         return;
      }

      Player p = event.getPlayer();
      boolean was = this.isTavern(event.getFrom());
      boolean now = this.isTavern(event.getTo());
      if (now && !was) {
         ChatUtil.sendActionbar(p, "&6🍺 Vous entrez dans une taverne — zone neutre, &e/taverne &6pour jouer à pile ou face.");
      } else if (was && !now) {
         ChatUtil.sendActionbar(p, "&7Vous quittez la taverne.");
      }
   }
}
