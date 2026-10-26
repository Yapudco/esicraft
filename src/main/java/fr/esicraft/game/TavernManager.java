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
   private static final int[] BET_SLOTS = {10, 11, 12, 13, 14};
   private static final int[] BET_VALUES = {10, 50, 100, 500, 1000};
   private static final int SIDE_HEADS = 20;
   private static final int SIDE_TAILS = 24;
   private static final int COIN_SLOT = 16;

   static final class TavernHolder extends BaseGUIHolder {
      String guildId;
      int betIndex = 1;
      boolean busy;
   }

   private final EsicraftPlugin plugin;
   private final Map<ChunkKey, String> taverns = new ConcurrentHashMap<>();
   private final Map<java.util.UUID, ChunkKey> lastChunk = new ConcurrentHashMap<>();

   public TavernManager(EsicraftPlugin plugin) {
      this.plugin = plugin;
   }

   public void init() {
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
               this.openGame(player, owner);
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

      this.taverns.put(here, guild.getId());
      this.plugin.getDataManager().getStorage().exec("INSERT OR REPLACE INTO taverns (world, cx, cz, guild_id) VALUES (?, ?, ?, ?);", here.getWorldName(), here.getX(), here.getZ(), guild.getId());
      ChatUtil.broadcastGuild(guild, "&6🍺 Une taverne (zone neutre) a été déclarée en &e" + here.getX() + "," + here.getZ() + "&6 : PvP et armes interdits, pile ou face avec &e/taverne&6 !");
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

   // ------------------------------------------------------------------ pile ou face

   private void openGame(Player player, String ownerId) {
      Guild owner = this.plugin.getGuildManager().getGuildById(ownerId);
      if (owner == null) {
         return;
      }

      TavernHolder holder = new TavernHolder();
      holder.guildId = ownerId;
      Inventory inv = Bukkit.createInventory(holder, 27, ChatUtil.component("&8&l🍺 Pile ou face — taverne de " + owner.getName()));
      this.render(inv, holder, owner);
      player.openInventory(inv);
   }

   private void render(Inventory inv, TavernHolder holder, Guild owner) {
      ItemStack glass = new ItemBuilder(Material.GRAY_STAINED_GLASS_PANE).name(" ").build();
      for (int i = 0; i < 27; i++) {
         inv.setItem(i, glass);
      }

      double winChance = Math.max(0.05, Math.min(0.95, this.plugin.getConfig().getDouble("taverns.coinflip.win-chance", 0.47)));
      inv.setItem(4, new ItemBuilder(Material.BARREL).name("&6&lLa banque de la taverne").lore("&7Propriétaire : &e" + owner.getName(), "&7Fonds : &6" + (long)owner.getBankBalance() + " pièces",
         "&7Chances de gagner : &e" + (int)Math.round(winChance * 100) + " %", "&7Gain : &amise doublée").build());
      for (int i = 0; i < BET_SLOTS.length; i++) {
         boolean selected = i == holder.betIndex;
         inv.setItem(BET_SLOTS[i], new ItemBuilder(selected ? Material.GOLD_BLOCK : Material.GOLD_NUGGET).name((selected ? "&a▶ " : "&e") + BET_VALUES[i] + " pièces").lore("&7Cliquez pour miser ce montant.").build());
      }

      inv.setItem(COIN_SLOT, new ItemBuilder(Material.GOLD_INGOT).name("&6La pièce").lore("&7Choisissez un côté pour lancer.").build());
      inv.setItem(SIDE_HEADS, new ItemBuilder(Material.PLAYER_HEAD).name("&e&lPILE").lore("&7Misez sur pile.").build());
      inv.setItem(SIDE_TAILS, new ItemBuilder(Material.IRON_INGOT).name("&b&lFACE").lore("&7Misez sur face.").build());
   }

   @EventHandler(priority = EventPriority.HIGHEST)
   public void onClick(InventoryClickEvent event) {
      if (!(event.getWhoClicked() instanceof Player player) || !(event.getInventory().getHolder() instanceof TavernHolder holder)) {
         return;
      }

      event.setCancelled(true);
      if (event.getClickedInventory() != event.getView().getTopInventory() || holder.busy) {
         return;
      }

      Guild owner = this.plugin.getGuildManager().getGuildById(holder.guildId);
      if (owner == null || !this.isTavern(player.getLocation())) {
         player.closeInventory();
         return;
      }

      int slot = event.getRawSlot();
      for (int i = 0; i < BET_SLOTS.length; i++) {
         if (slot == BET_SLOTS[i]) {
            holder.betIndex = i;
            this.render(event.getInventory(), holder, owner);
            ChatUtil.playSound(player, Sound.UI_BUTTON_CLICK, 1.0F, 1.2F);
            return;
         }
      }

      if (slot == SIDE_HEADS || slot == SIDE_TAILS) {
         this.flip(player, holder, owner, slot == SIDE_HEADS, event.getInventory());
      }
   }

   private void flip(Player player, TavernHolder holder, Guild owner, boolean heads, Inventory inv) {
      int bet = BET_VALUES[holder.betIndex];
      if (this.plugin.getCombatManager().denySpending(player)) {
         return;
      }

      if (owner.getBankBalance() < bet) {
         ChatUtil.sendMessage(player, "&c❌ La banque de la taverne est trop pauvre pour couvrir cette mise.");
         return;
      }

      PlayerData data = this.plugin.getDataManager().getPlayerData(player.getUniqueId());
      if (data == null || !data.removeCoins(bet)) {
         ChatUtil.sendMessage(player, "&c❌ Vous n'avez pas assez de pièces.");
         return;
      }

      this.plugin.getDataManager().savePlayer(data);
      holder.busy = true;
      double winChance = Math.max(0.05, Math.min(0.95, this.plugin.getConfig().getDouble("taverns.coinflip.win-chance", 0.47)));
      boolean win = ThreadLocalRandom.current().nextDouble() < winChance;
      java.util.UUID id = player.getUniqueId();
      int[] frame = {0};
      Bukkit.getScheduler().runTaskTimer(this.plugin, task -> {
         frame[0]++;
         if (frame[0] <= 10) {
            inv.setItem(COIN_SLOT, new ItemBuilder(frame[0] % 2 == 0 ? Material.GOLD_INGOT : Material.IRON_INGOT).name("&6La pièce tourne…").build());
            Player p = Bukkit.getPlayer(id);
            if (p != null) {
               ChatUtil.playSound(p, Sound.BLOCK_NOTE_BLOCK_CHIME, 0.6F, 0.8F + frame[0] * 0.08F);
            }

            return;
         }

         task.cancel();
         holder.busy = false;
         boolean landedHeads = win == heads;
         inv.setItem(COIN_SLOT, new ItemBuilder(landedHeads ? Material.GOLD_INGOT : Material.IRON_INGOT).name(landedHeads ? "&e&lPILE !" : "&b&lFACE !").build());
         if (win) {
            boolean paid = owner.withdrawBank(bet);
            this.plugin.getDataManager().creditPlayer(id, paid ? bet * 2.0 : bet);
         } else {
            owner.depositBank(bet);
         }

         this.plugin.getDataManager().saveGuild(owner);
         Player p = Bukkit.getPlayer(id);
         if (p != null) {
            ChatUtil.sendMessage(p, win ? "&a🍺 " + (landedHeads ? "Pile" : "Face") + " ! Vous gagnez &6" + bet + " pièces&a." : "&c🍺 " + (landedHeads ? "Pile" : "Face") + "… la taverne empoche vos &6" + bet + " pièces&c.");
            ChatUtil.playSound(p, win ? Sound.ENTITY_PLAYER_LEVELUP : Sound.ENTITY_VILLAGER_NO, 1.0F, win ? 1.5F : 1.0F);
         }

         if (inv.getViewers().size() > 0) {
            this.render(inv, holder, owner);
            inv.setItem(COIN_SLOT, new ItemBuilder(landedHeads ? Material.GOLD_INGOT : Material.IRON_INGOT).name(landedHeads ? "&e&lPILE !" : "&b&lFACE !").build());
         }
      }, 4L, 4L);
   }
}
