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
   /** « guilde ravisseuse>guilde de la victime » → fin du délai avant une nouvelle capture (contre les complices qui se relaient). */
   private final Map<String, Long> pairCooldown = new ConcurrentHashMap<>();

   public RansomManager(EsicraftPlugin plugin) {
      this.plugin = plugin;
   }

   public void start() {
      Bukkit.getScheduler().runTaskTimer(this.plugin, this::tick, 20L, 20L);
   }

   public void clear() {
      this.captives.clear();
      this.immuneUntil.clear();
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
      if (captor == null || captor.equals(victim) || this.captives.containsKey(victim.getUniqueId()) || this.executing.contains(victim.getUniqueId())) {
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
      this.captives.put(victim.getUniqueId(), new Captive(victim.getUniqueId(), captor.getUniqueId(), captorGuild.getId(), price, System.currentTimeMillis() + seconds * 1000L));
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
      if (this.captives.containsKey(event.getPlayer().getUniqueId())) {
         event.setCancelled(true);
      }
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
         for (String s : List.of("payer", "prix", "liberer", "executer")) {
            if (s.startsWith(args[0].toLowerCase(Locale.ROOT))) {
               out.add(s);
            }
         }
      }

      return out;
   }
}
