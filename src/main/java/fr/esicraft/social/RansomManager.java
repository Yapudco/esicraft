package fr.esicraft.social;

import fr.esicraft.EsicraftPlugin;
import fr.esicraft.data.PlayerData;
import fr.esicraft.guild.Guild;
import fr.esicraft.util.ChatUtil;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
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
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.EntityPickupItemEvent;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryOpenEvent;
import org.bukkit.event.player.PlayerDropItemEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerSwapHandItemsEvent;
import org.bukkit.event.player.PlayerCommandPreprocessEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.player.PlayerTeleportEvent;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.jetbrains.annotations.NotNull;

/**
 * Rançon : un ennemi qui devrait mourir au combat (coup fatal d'un joueur d'une autre guilde) est capturé. Il est immobilisé
 * et invulnérable pendant {@code ransom.seconds} ; son ravisseur peut fixer un prix ({@code /rancon prix}), le libérer ou
 * l'exécuter, et le captif peut payer ({@code /rancon payer}). Sans décision, le sort prévu par {@code ransom.on-timeout}
 * s'applique (par défaut : exécution, comme s'il était mort au combat).
 */
public class RansomManager implements Listener, CommandExecutor, TabCompleter {
   private static final class Captive {
      final UUID victim;
      final UUID captor;
      final String captorGuildId;
      volatile double price;
      final long expiresAt;
      /** Position d'origine et cellule du ravisseur (null sans cellule : le captif reste sur place). */
      org.bukkit.Location origin;
      org.bukkit.Location cell;
      int rescue;
      long lastRescue;

      Captive(UUID victim, UUID captor, String captorGuildId, double price, long expiresAt) {
         this.victim = victim;
         this.captor = captor;
         this.captorGuildId = captorGuildId;
         this.price = price;
         this.expiresAt = expiresAt;
      }
   }

   private final EsicraftPlugin plugin;
   private final Map<UUID, Captive> captives = new ConcurrentHashMap<>();
   private final Map<UUID, Long> immuneUntil = new ConcurrentHashMap<>();
   private final Set<UUID> executing = ConcurrentHashMap.newKeySet();
   /** Cellule de prison de chaque guilde (identifiant de guilde → bloc), déclarée avec /rancon cellule. */
   private final Map<String, org.bukkit.Location> cells = new ConcurrentHashMap<>();
   private boolean internalTp;
   /** « guilde ravisseuse>guilde de la victime » → fin du délai avant une nouvelle capture (contre les complices qui se relaient). */
   private final Map<String, Long> pairCooldown = new ConcurrentHashMap<>();

   public RansomManager(EsicraftPlugin plugin) {
      this.plugin = plugin;
   }

   public void start() {
      var db = this.plugin.getDataManager().getStorage();
      db.exec("CREATE TABLE IF NOT EXISTS ransom_cells (guild_id VARCHAR(64) PRIMARY KEY, world VARCHAR(64), x INT, y INT, z INT);");
      for (Object[] row : db.select("SELECT guild_id, world, x, y, z FROM ransom_cells;", 5)) {
         World w = Bukkit.getWorld(String.valueOf(row[1]));
         if (w != null) {
            this.cells.put(String.valueOf(row[0]), new org.bukkit.Location(w, ((Number)row[2]).intValue(), ((Number)row[3]).intValue(), ((Number)row[4]).intValue()));
         }
      }

      Bukkit.getScheduler().runTaskTimer(this.plugin, this::tick, 20L, 20L);
   }

   public void clear() {
      this.captives.clear();
      this.immuneUntil.clear();
      this.cells.clear();
   }

   private boolean enabled() {
      return this.plugin.getConfig().getBoolean("ransom.enabled", true);
   }

   public boolean isCaptive(UUID uuid) {
      return this.captives.containsKey(uuid);
   }

   // ------------------------------------------------------------------ capture

   private static Player shooter(EntityDamageByEntityEvent event) {
      return fr.esicraft.combat.CombatManager.attackerOf(event.getDamager());
   }

   @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
   public void onLethalHit(EntityDamageByEntityEvent event) {
      if (!this.enabled() || !(event.getEntity() instanceof Player victim)) {
         return;
      }

      Player captor = shooter(event);
      if (captor == null || captor.equals(victim) || this.captives.containsKey(victim.getUniqueId()) || this.executing.contains(victim.getUniqueId())
         || this.plugin.getDuels().inDuelWith(captor, victim)) {
         return;
      }

      if (victim.getHealth() - event.getFinalDamage() > 0.0 || victim.getGameMode() == org.bukkit.GameMode.CREATIVE || victim.getGameMode() == org.bukkit.GameMode.SPECTATOR) {
         return;
      }

      Long immune = this.immuneUntil.get(victim.getUniqueId());
      if (immune != null && immune > System.currentTimeMillis()) {
         return;
      }

      if (victim.getInventory().getItemInMainHand().getType() == Material.TOTEM_OF_UNDYING || victim.getInventory().getItemInOffHand().getType() == Material.TOTEM_OF_UNDYING) {
         return;
      }

      Guild captorGuild = this.plugin.getGuildManager().getGuildByPlayer(captor.getUniqueId());
      Guild victimGuild = this.plugin.getGuildManager().getGuildByPlayer(victim.getUniqueId());
      if (captorGuild == null || victimGuild == null || captorGuild.getId().equals(victimGuild.getId())) {
         return;
      }

      // Pas de capture entre guildes liées par un traité (complices, alliés), ni deux fois de suite entre les mêmes guildes en peu de temps.
      if (this.plugin.getTreatyManager().between(captorGuild.getId(), victimGuild.getId()) != null) {
         return;
      }

      String pair = captorGuild.getId() + ">" + victimGuild.getId();
      Long cooldown = this.pairCooldown.get(pair);
      if (cooldown != null && cooldown > System.currentTimeMillis()) {
         return;
      }

      this.pairCooldown.put(pair, System.currentTimeMillis() + this.plugin.getConfig().getLong("ransom.pair-cooldown-minutes", 10L) * 60000L);
      event.setCancelled(true);
      this.capture(victim, captor, captorGuild, victimGuild);
   }

   private void capture(Player victim, Player captor, Guild captorGuild, Guild victimGuild) {
      PlayerData data = this.plugin.getDataManager().getPlayerData(victim.getUniqueId());
      double coins = data != null ? data.getCoins() : 0.0;
      double min = this.plugin.getConfig().getDouble("ransom.min", 100.0);
      double max = this.plugin.getConfig().getDouble("ransom.max", 2000.0);
      double price = Math.max(min, Math.min(max, Math.floor(coins * this.plugin.getConfig().getDouble("ransom.default-percent", 15.0) / 100.0)));
      long seconds = this.plugin.getConfig().getLong("ransom.seconds", 90L);
      Captive captive = new Captive(victim.getUniqueId(), captor.getUniqueId(), captorGuild.getId(), price, System.currentTimeMillis() + seconds * 1000L);
      org.bukkit.Location cell = this.cells.get(captorGuild.getId());
      if (cell != null && cell.getWorld() != null && captorGuild.isClaimed(fr.esicraft.util.ChunkKey.from(cell.getChunk()))) {
         captive.origin = victim.getLocation().clone();
         captive.cell = cell.clone().add(0.5, 1.0, 0.5);
      }

      this.captives.put(victim.getUniqueId(), captive);
      if (captive.cell != null) {
         this.internalTp = true;
         try {
            victim.teleport(captive.cell);
         } finally {
            this.internalTp = false;
         }

         ChatUtil.sendMessage(victim, "&c⛓ Vous êtes enfermé dans la cellule de &6" + captorGuild.getName() + "&c : vos alliés peuvent vous libérer en agissant sur le bloc de la cellule !");
         ChatUtil.broadcastGuild(victimGuild, "&c⛓ " + victim.getName() + " est enfermé dans la cellule de &6" + captorGuild.getName() + " &c(" + cell.getBlockX() + ", " + cell.getBlockY() + ", " + cell.getBlockZ()
            + ") — interagissez avec le bloc de la cellule " + this.plugin.getConfig().getInt("ransom.cell.rescue-actions", 15) + " fois pour le délivrer !");
      }
      victim.setHealth(Math.min(victim.getMaxHealth(), 4.0));
      victim.setFireTicks(0);
      victim.setGlowing(true);
      victim.addPotionEffect(new PotionEffect(PotionEffectType.SLOWNESS, (int)(seconds * 20L + 40L), 6, false, false, false));
      victim.addPotionEffect(new PotionEffect(PotionEffectType.JUMP_BOOST, (int)(seconds * 20L + 40L), 200, false, false, false));
      String shown = String.format(Locale.ROOT, "%.0f", price);
      ChatUtil.sendMessage(victim, "&c⛓ &lVous êtes capturé par &e" + captor.getName() + " &c! &7Rançon : &6" + shown + " pièces&7. Payez avec &e/rancon payer &7ou attendez votre sort (&e" + seconds + " s&7).");
      ChatUtil.sendMessage(captor, "&a⛓ Vous avez capturé &e" + victim.getName() + " &a! Rançon par défaut : &6" + shown + " pièces&a. &e/rancon prix <montant>&a, &e/rancon liberer &aou &e/rancon executer &a(" + seconds + " s).");
      ChatUtil.broadcastGuild(victimGuild, "&c⛓ " + victim.getName() + " est retenu prisonnier par la guilde &6" + captorGuild.getName() + " &c!");
      ChatUtil.playSound(victim, Sound.BLOCK_CHAIN_PLACE, 1.0F, 0.8F);
      ChatUtil.playSound(captor, Sound.BLOCK_CHAIN_PLACE, 1.0F, 1.2F);
      this.plugin.getChronicle().log(fr.esicraft.life.Chronicle.Kind.RAID, victim.getName() + " (" + victimGuild.getName() + ") est capturé par " + captor.getName() + " (" + captorGuild.getName() + ").", captorGuild.getId(), victimGuild.getId());
   }

   // ------------------------------------------------------------------ captivité

   @EventHandler(priority = EventPriority.HIGHEST)
   public void onDamage(EntityDamageEvent event) {
      if (event.getEntity() instanceof Player p && this.captives.containsKey(p.getUniqueId()) && !this.executing.contains(p.getUniqueId())) {
         event.setCancelled(true);
         p.setFireTicks(0);
      }
   }

   @EventHandler(ignoreCancelled = true)
   public void onMove(PlayerMoveEvent event) {
      if (this.captives.isEmpty() || !this.captives.containsKey(event.getPlayer().getUniqueId()) || event.getTo() == null) {
         return;
      }

      var from = event.getFrom();
      var to = event.getTo();
      if (from.getX() != to.getX() || from.getZ() != to.getZ() || to.getY() > from.getY()) {
         event.setTo(from.clone().setDirection(to.getDirection()));
      }
   }

   private boolean held(org.bukkit.entity.HumanEntity e) {
      return e instanceof Player p && this.captives.containsKey(p.getUniqueId());
   }

   @EventHandler(ignoreCancelled = true)
   public void onDrop(PlayerDropItemEvent event) {
      if (this.captives.containsKey(event.getPlayer().getUniqueId())) {
         event.setCancelled(true);
      }
   }

   @EventHandler(ignoreCancelled = true)
   public void onSwap(PlayerSwapHandItemsEvent event) {
      if (this.captives.containsKey(event.getPlayer().getUniqueId())) {
         event.setCancelled(true);
      }
   }

   @EventHandler(ignoreCancelled = true)
   public void onInteract(PlayerInteractEvent event) {
      if (this.captives.containsKey(event.getPlayer().getUniqueId())) {
         event.setCancelled(true);
      }
   }

   @EventHandler(ignoreCancelled = true)
   public void onClick(InventoryClickEvent event) {
      if (this.held(event.getWhoClicked())) {
         event.setCancelled(true);
      }
   }

   @EventHandler(ignoreCancelled = true)
   public void onOpen(InventoryOpenEvent event) {
      if (this.held(event.getPlayer())) {
         event.setCancelled(true);
      }
   }

   @EventHandler(ignoreCancelled = true)
   public void onPickup(EntityPickupItemEvent event) {
      if (event.getEntity() instanceof Player p && this.captives.containsKey(p.getUniqueId())) {
         event.setCancelled(true);
      }
   }

   @EventHandler(ignoreCancelled = true)
   public void onTeleport(PlayerTeleportEvent event) {
      if (!this.internalTp && this.captives.containsKey(event.getPlayer().getUniqueId())) {
         event.setCancelled(true);
      }
   }

   /** Sauvetage : un allié du captif agit sur le bloc de la cellule (clic) assez de fois pour le délivrer. */
   @EventHandler(priority = EventPriority.HIGHEST)
   public void onCellInteract(PlayerInteractEvent event) {
      if (this.captives.isEmpty() || event.getClickedBlock() == null || event.getAction() == org.bukkit.event.block.Action.PHYSICAL) {
         return;
      }

      org.bukkit.block.Block b = event.getClickedBlock();
      for (Captive c : new ArrayList<>(this.captives.values())) {
         if (c.cell == null || c.cell.getWorld() != b.getWorld() || c.cell.getBlockX() != b.getX() || c.cell.getBlockY() - 1 != b.getY() || c.cell.getBlockZ() != b.getZ()) {
            continue;
         }

         Player rescuer = event.getPlayer();
         if (this.captives.containsKey(rescuer.getUniqueId()) || c.victim.equals(rescuer.getUniqueId())) {
            return;
         }

         Guild mine = this.plugin.getGuildManager().getGuildByPlayer(rescuer.getUniqueId());
         Guild victimGuild = this.plugin.getGuildManager().getGuildByPlayer(c.victim);
         boolean ally = mine != null && victimGuild != null && (mine.getId().equals(victimGuild.getId()) || this.plugin.getTreatyManager().areFriends(mine.getId(), victimGuild.getId()));
         if (!ally) {
            return;
         }

         event.setCancelled(true);
         long now = System.currentTimeMillis();
         if (now - c.lastRescue > 10000L) {
            c.rescue = 0;
         }

         if (now - c.lastRescue < 250L) {
            return;
         }

         c.lastRescue = now;
         c.rescue++;
         int need = Math.max(1, this.plugin.getConfig().getInt("ransom.cell.rescue-actions", 15));
         b.getWorld().playSound(b.getLocation(), Sound.BLOCK_IRON_DOOR_OPEN, 1.0F, 1.5F);
         b.getWorld().spawnParticle(org.bukkit.Particle.CRIT, b.getLocation().add(0.5, 1.2, 0.5), 8, 0.3, 0.3, 0.3, 0.1);
         if (c.rescue >= need) {
            this.free(c, false, "délivré par " + rescuer.getName());
            ChatUtil.broadcast("&a⛓ &e" + rescuer.getName() + " a délivré &e" + Bukkit.getOfflinePlayer(c.victim).getName() + " &ade la cellule !");
         } else {
            ChatUtil.sendActionbar(rescuer, "&a⛓ Évasion : &e" + c.rescue + "&7/&e" + need);
         }

         return;
      }
   }

   private void teleportBack(Captive c, Player victim) {
      if (victim != null && c.origin != null && c.origin.getWorld() != null && !victim.isDead()) {
         this.internalTp = true;
         try {
            victim.teleport(c.origin);
         } finally {
            this.internalTp = false;
         }
      }
   }

   private void setCell(Player player, Guild guild) {
      if (!guild.hasRoleAtLeast(player.getUniqueId(), fr.esicraft.guild.GuildRole.OFFICER)) {
         ChatUtil.sendMessage(player, "&c❌ Réservé aux officiers.");
         return;
      }

      org.bukkit.block.Block target = player.getTargetBlockExact(6);
      if (target == null || target.getType().isAir()) {
         ChatUtil.sendMessage(player, "&c❌ Visez (à moins de 6 blocs) le bloc qui servira de cellule : le prisonnier apparaîtra juste au-dessus.");
         return;
      }

      if (!guild.isClaimed(fr.esicraft.util.ChunkKey.from(target.getChunk()))) {
         ChatUtil.sendMessage(player, "&c❌ Le bloc doit se trouver sur le territoire de votre guilde.");
         return;
      }

      org.bukkit.Location loc = target.getLocation();
      this.cells.put(guild.getId(), loc);
      this.plugin.getDataManager().getStorage().exec("INSERT OR REPLACE INTO ransom_cells (guild_id, world, x, y, z) VALUES (?, ?, ?, ?, ?);",
         guild.getId(), loc.getWorld().getName(), loc.getBlockX(), loc.getBlockY(), loc.getBlockZ());
      ChatUtil.sendMessage(player, "&a⛓ Cellule de prison définie en &e" + loc.getBlockX() + ", " + loc.getBlockY() + ", " + loc.getBlockZ() + "&a : vos captifs y seront enfermés. Veillez à la fermer (barreaux, murs) !");
   }

   @EventHandler(ignoreCancelled = true)
   public void onCommand(PlayerCommandPreprocessEvent event) {
      if (!this.captives.containsKey(event.getPlayer().getUniqueId())) {
         return;
      }

      String cmd = event.getMessage().toLowerCase(Locale.ROOT).split(" ")[0];
      if (!cmd.equals("/rancon") && !cmd.equals("/rançon")) {
         event.setCancelled(true);
         ChatUtil.sendMessage(event.getPlayer(), "&c⛓ Vous êtes captif : seule la commande &e/rancon &cest utilisable.");
      }
   }

   @EventHandler(priority = EventPriority.HIGHEST)
   public void onQuit(PlayerQuitEvent event) {
      Player p = event.getPlayer();
      Captive c = this.captives.get(p.getUniqueId());
      if (c != null) {
         // Fuir en captivité équivaut à mourir : exécution immédiate par le ravisseur.
         this.execute(c, "s'est déconnecté en captivité");
      }

      // Un ravisseur qui part libère ses prisonniers.
      for (Captive other : new ArrayList<>(this.captives.values())) {
         if (other.captor.equals(p.getUniqueId())) {
            this.free(other, false, "son ravisseur a disparu");
         }
      }
   }

   private void tick() {
      long now = System.currentTimeMillis();
      this.immuneUntil.values().removeIf(until -> until < now);
      for (Captive c : new ArrayList<>(this.captives.values())) {
         Player victim = Bukkit.getPlayer(c.victim);
         if (victim == null) {
            this.captives.remove(c.victim);
            continue;
         }

         if (c.expiresAt <= now) {
            if ("free".equalsIgnoreCase(this.plugin.getConfig().getString("ransom.on-timeout", "kill"))) {
               this.free(c, false, "temps écoulé");
            } else {
               this.execute(c, "n'a pas été libéré à temps");
            }
         } else {
            ChatUtil.sendActionbar(victim, "&c⛓ Captif : &e" + ((c.expiresAt - now + 999L) / 1000L) + " s &7- rançon &6" + String.format(Locale.ROOT, "%.0f", c.price) + " pièces");
         }
      }
   }

   private void release(Captive c, Player victim) {
      this.captives.remove(c.victim);
      if (victim != null) {
         victim.setGlowing(false);
         victim.removePotionEffect(PotionEffectType.SLOWNESS);
         victim.removePotionEffect(PotionEffectType.JUMP_BOOST);
      }
   }

   private void free(Captive c, boolean paid, String reason) {
      Player victim = Bukkit.getPlayer(c.victim);
      this.release(c, victim);
      this.teleportBack(c, victim);
      if (paid) {
         this.immuneUntil.put(c.victim, System.currentTimeMillis() + this.plugin.getConfig().getLong("ransom.immunity-minutes", 10L) * 60000L);
      }

      if (victim != null) {
         ChatUtil.sendMessage(victim, paid ? "&a⛓ Vous êtes libre ! &7(rançon payée)" : "&a⛓ Vous êtes libéré. &7(" + reason + ")");
         ChatUtil.playSound(victim, Sound.ENTITY_PLAYER_LEVELUP, 1.0F, 1.0F);
      }
   }

   private void execute(Captive c, String reason) {
      Player victim = Bukkit.getPlayer(c.victim);
      this.release(c, victim);
      if (victim == null) {
         return;
      }

      Player captor = Bukkit.getPlayer(c.captor);
      this.executing.add(c.victim);
      try {
         ChatUtil.sendMessage(victim, "&c⛓ Votre captivité s'achève par l'exécution (" + reason + ").");
         // setHealth(0) ne passe pas par le totem d'immortalité (équipé pendant la captivité) ; le tueur reste crédité de la prime.
         if (captor != null && captor.isOnline()) {
            victim.setKiller(captor);
         }

         victim.setHealth(0.0);
      } finally {
         this.executing.remove(c.victim);
      }
   }

   // ------------------------------------------------------------------ commande

   public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
      if (!(sender instanceof Player player)) {
         sender.sendMessage(ChatUtil.color("&cSeul un joueur peut exécuter cette commande."));
         return true;
      }

      Captive asVictim = this.captives.get(player.getUniqueId());
      List<Captive> mine = new ArrayList<>();
      for (Captive c : this.captives.values()) {
         if (c.captor.equals(player.getUniqueId())) {
            mine.add(c);
         }
      }

      String sub = args.length == 0 ? "" : args[0].toLowerCase(Locale.ROOT);
      // Plusieurs prisonniers : le nom se donne en dernier argument (/rancon prix <montant> <joueur>, /rancon liberer <joueur>).
      String nameArg = sub.equals("prix") ? (args.length > 2 ? args[2] : null) : (args.length > 1 ? args[1] : null);
      Captive asCaptor = null;
      if (nameArg != null) {
         for (Captive c : mine) {
            String n = Bukkit.getOfflinePlayer(c.victim).getName();
            if (n != null && n.equalsIgnoreCase(nameArg)) {
               asCaptor = c;
            }
         }
      } else if (mine.size() == 1) {
         asCaptor = mine.get(0);
      }

      if (asCaptor == null && mine.size() > 1 && (sub.equals("prix") || sub.equals("liberer") || sub.equals("libérer") || sub.equals("executer") || sub.equals("exécuter"))) {
         StringBuilder names = new StringBuilder();
         for (Captive c : mine) {
            names.append(' ').append(Bukkit.getOfflinePlayer(c.victim).getName());
         }

         ChatUtil.sendMessage(player, "&eVous avez plusieurs prisonniers :&f" + names + "&e. Ajoutez le nom du joueur à la commande.");
         return true;
      }

      if (sub.isEmpty()) {
         if (asVictim != null) {
            ChatUtil.sendMessage(player, "&c⛓ Vous êtes captif. Rançon : &6" + String.format(Locale.ROOT, "%.0f", asVictim.price) + " pièces&c. &e/rancon payer");
         } else if (asCaptor != null) {
            ChatUtil.sendMessage(player, "&a⛓ Prisonnier : &e" + Bukkit.getOfflinePlayer(asCaptor.victim).getName() + " &7- rançon &6" + String.format(Locale.ROOT, "%.0f", asCaptor.price)
               + " &7- &e/rancon prix <montant>&7, &e/rancon liberer&7, &e/rancon executer");
         } else {
            ChatUtil.sendMessage(player, "&7Aucun prisonnier. Un ennemi qui devrait mourir au combat est capturé : vous fixez sa rançon, ou vous l'exécutez.");
         }

         return true;
      }

      if (sub.equals("cellule") || sub.equals("cell")) {
         Guild guild = this.plugin.getGuildManager().getGuildByPlayer(player.getUniqueId());
         if (guild == null) {
            ChatUtil.sendMessage(player, "&c❌ Vous n'avez pas de guilde.");
         } else if (args.length > 1 && (args[1].equalsIgnoreCase("retirer") || args[1].equalsIgnoreCase("supprimer"))) {
            this.cells.remove(guild.getId());
            this.plugin.getDataManager().getStorage().exec("DELETE FROM ransom_cells WHERE guild_id = ?;", guild.getId());
            ChatUtil.sendMessage(player, "&7Cellule de prison supprimée : les captifs restent désormais sur place.");
         } else {
            this.setCell(player, guild);
         }

         return true;
      }

      switch (sub) {
         case "payer" -> {
            if (asVictim == null) {
               ChatUtil.sendMessage(player, "&cVous n'êtes pas captif.");
               return true;
            }

            PlayerData data = this.plugin.getDataManager().getPlayerData(player.getUniqueId());
            if (data == null || !data.removeCoins(asVictim.price)) {
               ChatUtil.sendMessage(player, "&c❌ Vous n'avez pas assez de pièces pour payer &6" + String.format(Locale.ROOT, "%.0f", asVictim.price) + "&c.");
               return true;
            }

            this.plugin.getDataManager().savePlayer(data);
            Guild captorGuild = this.plugin.getGuildManager().getGuildById(asVictim.captorGuildId);
            double share = Math.max(0.0, Math.min(1.0, this.plugin.getConfig().getDouble("ransom.captor-share", 0.3)));
            double toCaptor = Math.floor(asVictim.price * share);
            double toGuild = asVictim.price - toCaptor;
            if (captorGuild != null) {
               captorGuild.depositBank(toGuild);
               this.plugin.getDataManager().saveGuild(captorGuild);
            } else {
               toCaptor = asVictim.price;
            }

            this.plugin.getDataManager().creditPlayer(asVictim.captor, toCaptor);
            Player captor = Bukkit.getPlayer(asVictim.captor);
            if (captor != null) {
               ChatUtil.sendMessage(captor, "&a⛓ &e" + player.getName() + " &aa payé sa rançon : &6+" + String.format(Locale.ROOT, "%.0f", toCaptor) + " &apour vous, &6+"
                  + String.format(Locale.ROOT, "%.0f", toGuild) + " &aà la banque de guilde.");
            }

            this.free(asVictim, true, "");
         }
         case "prix" -> {
            if (asCaptor == null) {
               ChatUtil.sendMessage(player, "&cVous n'avez aucun prisonnier.");
               return true;
            }

            double min = this.plugin.getConfig().getDouble("ransom.min", 100.0);
            double max = this.plugin.getConfig().getDouble("ransom.max", 2000.0);
            try {
               double value = Double.parseDouble(args.length > 1 ? args[1] : "");
               if (value < min || value > max) {
                  ChatUtil.sendMessage(player, "&c❌ La rançon doit être entre &e" + (int)min + " &cet &e" + (int)max + "&c.");
                  return true;
               }

               asCaptor.price = Math.floor(value);
            } catch (NumberFormatException e) {
               ChatUtil.sendMessage(player, "&cUsage : &e/rancon prix <montant>");
               return true;
            }

            ChatUtil.sendMessage(player, "&a⛓ Rançon fixée à &6" + String.format(Locale.ROOT, "%.0f", asCaptor.price) + " pièces&a.");
            Player victim = Bukkit.getPlayer(asCaptor.victim);
            if (victim != null) {
               ChatUtil.sendMessage(victim, "&c⛓ Nouvelle rançon : &6" + String.format(Locale.ROOT, "%.0f", asCaptor.price) + " pièces&c. &e/rancon payer");
            }
         }
         case "liberer", "libérer" -> {
            if (asCaptor == null) {
               ChatUtil.sendMessage(player, "&cVous n'avez aucun prisonnier.");
               return true;
            }

            this.free(asCaptor, false, "libéré par son ravisseur");
            ChatUtil.sendMessage(player, "&e⛓ Prisonnier libéré.");
         }
         case "executer", "exécuter" -> {
            if (asCaptor == null) {
               ChatUtil.sendMessage(player, "&cVous n'avez aucun prisonnier.");
               return true;
            }

            this.execute(asCaptor, "exécuté par " + player.getName());
         }
         default -> ChatUtil.sendMessage(player, "&cUsage : &e/rancon [payer|prix <montant>|liberer|executer]");
      }

      return true;
   }

   public List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command command, @NotNull String alias, @NotNull String[] args) {
      List<String> out = new ArrayList<>();
      if (args.length == 1) {
         for (String s : List.of("payer", "prix", "liberer", "executer", "cellule")) {
            if (s.startsWith(args[0].toLowerCase(Locale.ROOT))) {
               out.add(s);
            }
         }
      }

      return out;
   }
}
