package fr.esicraft.game;

import fr.esicraft.EsicraftPlugin;
import fr.esicraft.guild.Guild;
import fr.esicraft.guild.GuildRole;
import fr.esicraft.util.ChatUtil;
import fr.esicraft.util.ChunkKey;
import java.time.DayOfWeek;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import net.kyori.adventure.title.Title;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.jetbrains.annotations.NotNull;

/**
 * Guerres formelles : une guilde plante un Étendard de Siège à la frontière d'une guilde rivale avec 24 h de préavis et fixe un créneau
 * (ex. samedi 21 h). Pendant le créneau, ses membres peuvent démolir le Nexus ennemi à coups de pioche ou d'arme lourde (dégâts majorés, même
 * sans défenseur), sans acheter de horde. Victoire si le Nexus tombe ou si elle lui inflige au moins {@code siege.win-damage-percent} % de ses PV max.
 */
public class SiegeManager implements CommandExecutor, TabCompleter, Listener {
   public enum State { PENDING, ACTIVE }

   public static final class Siege {
      final String id;
      final String attackerId;
      final String defenderId;
      final String world;
      final int bx;
      final int by;
      final int bz;
      final long start;
      final long end;
      final double fee;
      State state;
      long damage;
      boolean reminded;

      Siege(String id, String attackerId, String defenderId, String world, int bx, int by, int bz, long start, long end, double fee, State state, long damage) {
         this.id = id;
         this.attackerId = attackerId;
         this.defenderId = defenderId;
         this.world = world;
         this.bx = bx;
         this.by = by;
         this.bz = bz;
         this.start = start;
         this.end = end;
         this.fee = fee;
         this.state = state;
         this.damage = damage;
      }

      public String attackerId() {
         return this.attackerId;
      }

      public String defenderId() {
         return this.defenderId;
      }

      public long start() {
         return this.start;
      }

      public long end() {
         return this.end;
      }

      public State state() {
         return this.state;
      }

      public long damage() {
         return this.damage;
      }
   }

   private static final DateTimeFormatter FORMAT = DateTimeFormatter.ofPattern("EEEE d MMMM 'à' HH'h'mm", Locale.FRENCH);

   private final EsicraftPlugin plugin;
   private final Map<String, Siege> sieges = new ConcurrentHashMap<>();
   private final Map<String, Long> lastEnded = new ConcurrentHashMap<>();
   private final Map<java.util.UUID, Long> lastPickaxe = new ConcurrentHashMap<>();

   public SiegeManager(EsicraftPlugin plugin) {
      this.plugin = plugin;
   }

   public void init() {
      var db = this.plugin.getDataManager().getStorage();
      db.exec("CREATE TABLE IF NOT EXISTS sieges (id VARCHAR(40) PRIMARY KEY, attacker_id VARCHAR(64), defender_id VARCHAR(64), world VARCHAR(64), bx INT, by INT, bz INT, start BIGINT, end_at BIGINT, fee REAL, state VARCHAR(10), damage BIGINT);");
      db.exec("CREATE TABLE IF NOT EXISTS siege_history (ts BIGINT, attacker_id VARCHAR(64), attacker_name VARCHAR(64), defender_id VARCHAR(64), defender_name VARCHAR(64), winner VARCHAR(10), damage BIGINT);");
      for (Object[] row : db.select("SELECT id, attacker_id, defender_id, world, bx, by, bz, start, end_at, fee, state, damage FROM sieges;", 12)) {
         try {
            Siege s = new Siege(String.valueOf(row[0]), String.valueOf(row[1]), String.valueOf(row[2]), String.valueOf(row[3]), ((Number)row[4]).intValue(), ((Number)row[5]).intValue(),
               ((Number)row[6]).intValue(), ((Number)row[7]).longValue(), ((Number)row[8]).longValue(), ((Number)row[9]).doubleValue(), State.valueOf(String.valueOf(row[10])), ((Number)row[11]).longValue());
            this.sieges.put(s.id, s);
         } catch (RuntimeException ignored) {
         }
      }

      Bukkit.getScheduler().runTaskTimer(this.plugin, this::tick, 200L, 400L);
   }

   public void clear() {
      for (Siege s : this.sieges.values()) {
         this.removeBanner(s);
      }

      this.sieges.clear();
      this.lastEnded.clear();
   }

   private void persist(Siege s) {
      Runnable w = () -> this.plugin.getDataManager().getStorage().exec(
         "INSERT INTO sieges (id, attacker_id, defender_id, world, bx, by, bz, start, end_at, fee, state, damage) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?) "
            + "ON CONFLICT(id) DO UPDATE SET state = excluded.state, damage = excluded.damage;",
         s.id, s.attackerId, s.defenderId, s.world, s.bx, s.by, s.bz, s.start, s.end, s.fee, s.state.name(), s.damage);
      if (this.plugin.isEnabled()) {
         Bukkit.getScheduler().runTaskAsynchronously(this.plugin, w);
      } else {
         w.run();
      }
   }

   private void delete(Siege s) {
      Runnable w = () -> this.plugin.getDataManager().getStorage().exec("DELETE FROM sieges WHERE id = ?;", s.id);
      if (this.plugin.isEnabled()) {
         Bukkit.getScheduler().runTaskAsynchronously(this.plugin, w);
      } else {
         w.run();
      }
   }

   public List<Siege> all() {
      return List.copyOf(this.sieges.values());
   }

   // ------------------------------------------------------------------ requêtes

   /** Siège en cours (créneau ouvert) du {@code attacker} contre le {@code defender}, ou null. */
   public Siege activeBetween(Guild attacker, Guild defender) {
      if (attacker == null || defender == null) {
         return null;
      }

      for (Siege s : this.sieges.values()) {
         if (s.state == State.ACTIVE && s.attackerId.equals(attacker.getId()) && s.defenderId.equals(defender.getId())) {
            return s;
         }
      }

      return null;
   }

   /** La guilde est-elle engagée dans un siège déclaré ou en cours ? */
   public boolean isEngaged(Guild guild) {
      return this.pendingOrActiveOf(guild) != null;
   }

   public boolean isBesieged(Guild defender) {
      for (Siege s : this.sieges.values()) {
         if (s.state == State.ACTIVE && s.defenderId.equals(defender.getId())) {
            return true;
         }
      }

      return false;
   }

   private Siege pendingOrActiveOf(Guild g) {
      for (Siege s : this.sieges.values()) {
         if (s.attackerId.equals(g.getId()) || s.defenderId.equals(g.getId())) {
            return s;
         }
      }

      return null;
   }

   public double damageMultiplier() {
      return Math.max(1.0, this.plugin.getConfig().getDouble("siege.damage-multiplier", 2.0));
   }

   public boolean isSiegeAttacker(Player player, Guild defender) {
      return this.activeBetween(this.plugin.getGuildManager().getGuildByPlayer(player.getUniqueId()), defender) != null;
   }

   public double multiplierFor(Player player, Guild defender) {
      return this.isSiegeAttacker(player, defender) ? this.damageMultiplier() : 1.0;
   }

   public void addDamage(Guild attacker, Guild defender, int dmg) {
      Siege s = this.activeBetween(attacker, defender);
      if (s != null) {
         s.damage += dmg;
      }
   }

   // ------------------------------------------------------------------ pioche

   private static int pickaxeDamage(Material m) {
      return switch (m) {
         case NETHERITE_PICKAXE -> 22;
         case DIAMOND_PICKAXE -> 18;
         case IRON_PICKAXE -> 12;
         case STONE_PICKAXE -> 8;
         case GOLDEN_PICKAXE, WOODEN_PICKAXE -> 5;
         default -> 0;
      };
   }

   /** Vrai si le coup de pioche est un fait de siège (le bloc du Nexus n'est alors pas détruit, c'est son total de PV qui baisse). */
   public boolean pickaxeAttack(Player player, Guild defender) {
      Guild mine = this.plugin.getGuildManager().getGuildByPlayer(player.getUniqueId());
      Siege s = this.activeBetween(mine, defender);
      int base = pickaxeDamage(player.getInventory().getItemInMainHand().getType());
      if (s == null || base <= 0 || !defender.hasNexus() || !this.plugin.isNexusDestructionEnabled()) {
         return false;
      }

      if (this.plugin.getTruce().denyHostile(player)) {
         return true;
      }

      long now = System.currentTimeMillis();
      Long last = this.lastPickaxe.get(player.getUniqueId());
      if (last == null || now - last >= 500L) {
         this.lastPickaxe.put(player.getUniqueId(), now);
         this.plugin.getDisguiseManager().reveal(player, true);
         int dmg = (int)Math.round(base * this.damageMultiplier());
         this.plugin.getNexusManager().damageNexus(defender, dmg, player.getName(), mine, player);
         ChatUtil.sendActionbar(player, "&c⛏ Siège : &e" + dmg + " dégâts &cau Nexus ennemi à coups de pioche !");
      }

      return true;
   }

   // ------------------------------------------------------------------ déclaration

   private static DayOfWeek parseDay(String s) {
      return switch (s.toLowerCase(Locale.ROOT)) {
         case "lundi", "monday", "lun" -> DayOfWeek.MONDAY;
         case "mardi", "tuesday", "mar" -> DayOfWeek.TUESDAY;
         case "mercredi", "wednesday", "mer" -> DayOfWeek.WEDNESDAY;
         case "jeudi", "thursday", "jeu" -> DayOfWeek.THURSDAY;
         case "vendredi", "friday", "ven" -> DayOfWeek.FRIDAY;
         case "samedi", "saturday", "sam" -> DayOfWeek.SATURDAY;
         case "dimanche", "sunday", "dim" -> DayOfWeek.SUNDAY;
         default -> null;
      };
   }

   private static String when(long ms) {
      return LocalDateTime.ofInstant(Instant.ofEpochMilli(ms), ZoneId.systemDefault()).format(FORMAT);
   }

   private void declare(Player player, String[] args) {
      Guild mine = this.plugin.getGuildManager().getGuildByPlayer(player.getUniqueId());
      if (mine == null || !mine.hasRoleAtLeast(player.getUniqueId(), GuildRole.OFFICER)) {
         ChatUtil.sendMessage(player, "&c❌ Réservé aux officiers d'une guilde.");
         return;
      }

      if (args.length < 4) {
         ChatUtil.sendMessage(player, "&cUsage : &e/siege declarer <guilde> <jour> <heure> &7(ex. /siege declarer Loups samedi 21)");
         return;
      }

      // Le nom de la guilde cible peut contenir des espaces : le jour et l'heure sont les deux derniers mots.
      Guild target = this.plugin.getGuildManager().getGuildByName(String.join(" ", java.util.Arrays.copyOfRange(args, 1, args.length - 2)));
      if (target == null || target.getId().equals(mine.getId())) {
         ChatUtil.sendMessage(player, "&c❌ Guilde cible introuvable.");
         return;
      }

      DayOfWeek day = parseDay(args[args.length - 2]);
      int hour;
      try {
         hour = Integer.parseInt(args[args.length - 1].toLowerCase(Locale.ROOT).replace("h", "").split(":")[0]);
      } catch (NumberFormatException e) {
         hour = -1;
      }

      if (day == null || hour < 0 || hour > 23) {
         ChatUtil.sendMessage(player, "&c❌ Jour (lundi…dimanche) ou heure (0-23) invalide.");
         return;
      }

      if (!mine.hasNexus() || !mine.getNexus().isAlive() || !target.hasNexus() || !target.getNexus().isAlive()) {
         ChatUtil.sendMessage(player, "&c❌ Les deux guildes doivent posséder un Nexus actif.");
         return;
      }

      if (this.plugin.getTreatyManager().between(mine.getId(), target.getId()) != null) {
         ChatUtil.sendMessage(player, "&c❌ Un traité vous lie à cette guilde : rompez-le avant de lui déclarer la guerre.");
         return;
      }

      if (this.pendingOrActiveOf(mine) != null || this.pendingOrActiveOf(target) != null) {
         ChatUtil.sendMessage(player, "&c❌ L'une des deux guildes est déjà engagée dans un siège.");
         return;
      }

      long cooldownMs = this.plugin.getConfig().getLong("siege.pair-cooldown-hours", 72L) * 3600000L;
      Long ended = this.lastEnded.get(mine.getId() + ">" + target.getId());
      if (ended != null && System.currentTimeMillis() - ended < cooldownMs && !this.plugin.bypassCooldowns(player)) {
         ChatUtil.sendMessage(player, "&c❌ Un siège vous a déjà opposés récemment.");
         return;
      }

      // Étendard planté sur la frontière : hors du territoire cible, au contact d'un de ses chunks.
      Location here = player.getLocation();
      ChunkKey chunk = new ChunkKey(here.getWorld().getName(), here.getBlockX() >> 4, here.getBlockZ() >> 4);
      Guild occupant = this.plugin.getGuildManager().getGuildByChunk(chunk);
      boolean border = false;
      for (int dx = -1; dx <= 1 && !border; dx++) {
         for (int dz = -1; dz <= 1 && !border; dz++) {
            if (dx != 0 || dz != 0) {
               Guild n = this.plugin.getGuildManager().getGuildByChunk(new ChunkKey(chunk.getWorldName(), chunk.getX() + dx, chunk.getZ() + dz));
               border = n != null && n.getId().equals(target.getId());
            }
         }
      }

      if (!border || occupant != null && occupant.getId().equals(target.getId())) {
         ChatUtil.sendMessage(player, "&c❌ Plantez l'Étendard de Siège sur la frontière de &6" + target.getName() + " &c: dans un chunk voisin du sien, mais pas dedans.");
         return;
      }

      if (occupant != null && !occupant.getId().equals(mine.getId())) {
         ChatUtil.sendMessage(player, "&c❌ Cet emplacement appartient à une guilde tierce : plantez l'étendard en terrain libre ou sur votre territoire.");
         return;
      }

      Block banner = here.getBlock();
      if (!banner.getType().isAir() || !banner.getRelative(org.bukkit.block.BlockFace.DOWN).getType().isSolid()) {
         ChatUtil.sendMessage(player, "&c❌ L'étendard se plante dans un espace libre, sur un sol solide (aucun bloc existant n'est remplacé).");
         return;
      }

      LocalDateTime now = LocalDateTime.now();
      LocalDateTime when = now.toLocalDate().atTime(hour, 0);
      while (when.getDayOfWeek() != day || when.isBefore(now.plusHours(Math.max(1, this.plugin.getConfig().getLong("siege.notice-hours", 24L))))) {
         when = when.plusDays(1);
      }

      long durationMs = Math.max(10L, this.plugin.getConfig().getLong("siege.duration-minutes", 60L)) * 60000L;
      LocalDateTime endAt = when.plusMinutes(durationMs / 60000L);
      if (this.plugin.getTruce().overlapsTruce(when, endAt)) {
         ChatUtil.sendMessage(player, "&c❌ Ce créneau chevauche la trêve hebdomadaire : choisissez une autre heure.");
         return;
      }

      double fee = Math.max(0.0, this.plugin.getConfig().getDouble("siege.declaration-fee", 500.0));
      if (fee > 0.0 && (!this.plugin.getGuildManager().officerMaySpend(mine, player, fee) || !mine.withdrawBank(fee))) {
         ChatUtil.sendMessage(player, "&c❌ La banque de votre guilde doit contenir &6" + (long)fee + " pièces &c(frais de déclaration de guerre).");
         return;
      }

      banner.setType(Material.RED_BANNER);
      long start = when.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli();
      Siege s = new Siege(java.util.UUID.randomUUID().toString(), mine.getId(), target.getId(), banner.getWorld().getName(), banner.getX(), banner.getY(), banner.getZ(), start, start + durationMs, fee, State.PENDING, 0L);
      this.sieges.put(s.id, s);
      this.persist(s);
      this.plugin.getDataManager().saveGuild(mine);
      ChatUtil.broadcast("&4&l⚔ DÉCLARATION DE GUERRE ! &c" + mine.getName() + " &7déclare le siège de &6" + target.getName() + " &7pour le &e" + when(start) + " &7(durée " + (durationMs / 60000L) + " min). Son Étendard flotte sur la frontière !");
      ChatUtil.broadcastGuild(target, "&c⚔ Vous êtes assiégés ! Détruisez l'Étendard rouge planté à la frontière (" + banner.getX() + ", " + banner.getY() + ", " + banner.getZ() + ") avant le début du siège pour l'annuler.");
      this.plugin.getChronicle().log(fr.esicraft.life.Chronicle.Kind.RAID, mine.getName() + " déclare le siège de " + target.getName() + " pour le " + when(start) + ".", mine.getId(), target.getId());
   }

   private void cancelByAttacker(Player player) {
      Guild mine = this.plugin.getGuildManager().getGuildByPlayer(player.getUniqueId());
      Siege s = mine == null ? null : this.pendingOrActiveOf(mine);
      if (s == null || !s.attackerId.equals(mine.getId()) || s.state != State.PENDING || !mine.hasRoleAtLeast(player.getUniqueId(), GuildRole.OFFICER)) {
         ChatUtil.sendMessage(player, "&c❌ Aucun siège déclaré par votre guilde à annuler.");
         return;
      }

      this.finish(s, "ATTACKER_CANCEL");
   }

   private void list(Player player) {
      ChatUtil.sendMessage(player, "&6⚔ Sièges déclarés :");
      boolean any = false;
      for (Siege s : this.sieges.values()) {
         Guild a = this.plugin.getGuildManager().getGuildById(s.attackerId);
         Guild d = this.plugin.getGuildManager().getGuildById(s.defenderId);
         any = true;
         ChatUtil.sendRaw(player, " &8• &c" + (a != null ? a.getName() : "?") + " &7→ &a" + (d != null ? d.getName() : "?") + " &7: " + (s.state == State.ACTIVE ? "&c&lEN COURS" : "&e" + when(s.start)));
      }

      if (!any) {
         ChatUtil.sendRaw(player, " &7Aucun siège.");
      }
   }

   public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
      if (!(sender instanceof Player player)) {
         sender.sendMessage(ChatUtil.color("&cSeul un joueur peut exécuter cette commande."));
         return true;
      }

      String sub = args.length == 0 ? "liste" : args[0].toLowerCase(Locale.ROOT);
      switch (sub) {
         case "declarer", "déclarer" -> this.declare(player, args);
         case "annuler" -> this.cancelByAttacker(player);
         default -> this.list(player);
      }

      return true;
   }

   public List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command command, @NotNull String alias, @NotNull String[] args) {
      List<String> out = new ArrayList<>();
      if (args.length == 1) {
         out.addAll(List.of("declarer", "liste", "annuler"));
      } else if (args.length == 2 && args[0].equalsIgnoreCase("declarer")) {
         for (Guild g : this.plugin.getGuildManager().getAllGuilds()) {
            out.add(g.getName());
         }
      } else if (args.length == 3) {
         out.addAll(List.of("lundi", "mardi", "mercredi", "jeudi", "vendredi", "samedi", "dimanche"));
      }

      return out;
   }

   // ------------------------------------------------------------------ cycle de vie

   private void tick() {
      long now = System.currentTimeMillis();
      for (Siege s : List.copyOf(this.sieges.values())) {
         Guild a = this.plugin.getGuildManager().getGuildById(s.attackerId);
         Guild d = this.plugin.getGuildManager().getGuildById(s.defenderId);
         if (a == null || d == null || !d.hasNexus()) {
            this.finish(s, "VOID");
            continue;
         }

         if (s.state == State.PENDING) {
            if (!s.reminded && s.start - now < 3600000L) {
               s.reminded = true;
               ChatUtil.broadcast("&c⚔ Dans moins d'une heure : siège de &6" + d.getName() + " &cpar &6" + a.getName() + " &c!");
            }

            if (now >= s.start && !this.plugin.getTruce().isTruce()) {
               s.state = State.ACTIVE;
               this.persist(s);
               ChatUtil.broadcast("&4&l⚔ LE SIÈGE COMMENCE ! &c" + a.getName() + " &7assiège &6" + d.getName() + "&7 pendant " + (s.end - s.start) / 60000L + " minutes : Nexus attaquable à la pioche et à l'arme lourde !");
               for (Player p : Bukkit.getOnlinePlayers()) {
                  Guild g = this.plugin.getGuildManager().getGuildByPlayer(p.getUniqueId());
                  if (g != null && (g.getId().equals(a.getId()) || g.getId().equals(d.getId()))) {
                     p.showTitle(Title.title(ChatUtil.component("&4&l⚔ SIÈGE ⚔"), ChatUtil.component("&c" + a.getName() + " &7contre &a" + d.getName())));
                     ChatUtil.playSound(p, Sound.EVENT_RAID_HORN, 3.0F, 1.0F);
                  }
               }
            }
         } else if (now >= s.end) {
            this.finish(s, "TIMEOUT");
         }
      }
   }

   private void removeBanner(Siege s) {
      World w = Bukkit.getWorld(s.world);
      if (w != null) {
         Block b = w.getBlockAt(s.bx, s.by, s.bz);
         if (b.getType() == Material.RED_BANNER) {
            b.setType(Material.AIR);
         }
      }
   }

   /** Conclusion d'un siège : raison TIMEOUT (verdict selon les dégâts), NEXUS (Nexus détruit), BANNER (étendard détruit), ATTACKER_CANCEL ou VOID. */
   private void finish(Siege s, String reason) {
      if (this.sieges.remove(s.id) == null) {
         return;
      }

      this.removeBanner(s);
      this.delete(s);
      Guild a = this.plugin.getGuildManager().getGuildById(s.attackerId);
      Guild d = this.plugin.getGuildManager().getGuildById(s.defenderId);
      if (a == null || d == null) {
         return;
      }

      this.lastEnded.put(s.attackerId + ">" + s.defenderId, System.currentTimeMillis());
      String winner;
      switch (reason) {
         case "NEXUS" -> winner = "ATTACKER";
         case "BANNER" -> {
            winner = "DEFENDER";
            d.depositBank(s.fee);
            this.plugin.getDataManager().saveGuild(d);
            ChatUtil.broadcast("&a🏳 L'Étendard de siège de &c" + a.getName() + " &aa été abattu : &6" + d.getName() + " &aévite la guerre et empoche les frais de déclaration.");
         }
         case "ATTACKER_CANCEL" -> {
            winner = "NONE";
            a.depositBank(s.fee / 2.0);
            this.plugin.getDataManager().saveGuild(a);
            ChatUtil.broadcast("&7🏳 &c" + a.getName() + " &7renonce à son siège contre &6" + d.getName() + "&7.");
         }
         case "TIMEOUT" -> {
            double needed = d.getNexus() != null ? d.getNexus().getMaxHealth() * this.plugin.getConfig().getDouble("siege.win-damage-percent", 40.0) / 100.0 : 0.0;
            winner = s.damage >= needed ? "ATTACKER" : "DEFENDER";
            if (winner.equals("DEFENDER")) {
               d.depositBank(s.fee);
               this.plugin.getDataManager().saveGuild(d);
               ChatUtil.broadcast("&a🛡 Le siège est terminé : &6" + d.getName() + " &atient bon ! &c" + a.getName() + " n'a infligé que " + s.damage + " dégâts (objectif : " + (long)needed + ").");
            }
         }
         default -> winner = "NONE";
      }

      if (winner.equals("ATTACKER")) {
         this.plugin.getBourse().shock(a.getId(), 0.04, "siège gagné");
         this.plugin.getBourse().shock(d.getId(), -0.04, "siège perdu");
      } else if (winner.equals("DEFENDER")) {
         this.plugin.getBourse().shock(d.getId(), 0.03, "siège repoussé");
         this.plugin.getBourse().shock(a.getId(), -0.03, "siège raté");
      }

      if (winner.equals("ATTACKER")) {
         double cap = Math.max(0.0, this.plugin.getConfig().getDouble("siege.loot-cap", 3000.0));
         double loot = Math.min(cap, d.getBankBalance() * Math.max(0.0, this.plugin.getConfig().getDouble("siege.loot-percent", 10.0)) / 100.0);
         if (loot > 0.0 && d.withdrawBank(loot)) {
            a.depositBank(loot + s.fee);
         } else {
            a.depositBank(s.fee);
         }

         this.plugin.getDataManager().saveGuild(a);
         this.plugin.getDataManager().saveGuild(d);
         ChatUtil.broadcast("&4&l⚔ VICTOIRE DE SIÈGE ! &c" + a.getName() + " &7remporte la guerre contre &6" + d.getName() + " &7(" + s.damage + " dégâts infligés au Nexus) et pille " + (long)loot + " pièces de sa banque.");
         this.plugin.getAchievements().grantGuild(a, fr.esicraft.life.Achievement.SIEGE_VICTOR, false);
         this.plugin.getSeasonManager().addHistory(fr.esicraft.life.Chronicle.Kind.RAID, "⚔ " + a.getName() + " remporte le siège contre " + d.getName() + ".", a.getId(), d.getId());
      }

      long ts = System.currentTimeMillis();
      long dmg = s.damage;
      String an = a.getName();
      String dn = d.getName();
      Bukkit.getScheduler().runTaskAsynchronously(this.plugin, () -> this.plugin.getDataManager().getStorage().exec(
         "INSERT INTO siege_history (ts, attacker_id, attacker_name, defender_id, defender_name, winner, damage) VALUES (?, ?, ?, ?, ?, ?, ?);", ts, s.attackerId, an, s.defenderId, dn, winner, dmg));
   }

   /** Le Nexus assiégé est tombé : victoire immédiate de l'assaillant. */
   public void onNexusDestroyed(Guild defender, Guild attacker) {
      Siege s = this.activeBetween(attacker, defender);
      if (s != null) {
         this.finish(s, "NEXUS");
      }
   }

   @EventHandler(priority = EventPriority.HIGHEST)
   public void onBannerBreak(BlockBreakEvent event) {
      Block b = event.getBlock();
      if (b.getType() != Material.RED_BANNER) {
         return;
      }

      for (Siege s : List.copyOf(this.sieges.values())) {
         if (s.bx == b.getX() && s.by == b.getY() && s.bz == b.getZ() && s.world.equals(b.getWorld().getName())) {
            Guild mine = this.plugin.getGuildManager().getGuildByPlayer(event.getPlayer().getUniqueId());
            event.setDropItems(false);
            if (s.state == State.PENDING && mine != null && mine.getId().equals(s.defenderId)) {
               this.finish(s, "BANNER");
            } else if (s.state == State.PENDING && mine != null && mine.getId().equals(s.attackerId)) {
               this.finish(s, "ATTACKER_CANCEL");
            } else {
               event.setCancelled(true);
               ChatUtil.sendActionbar(event.getPlayer(), "&c🚩 Cet Étendard de siège ne peut pas être détruit maintenant.");
            }

            return;
         }
      }
   }
}
