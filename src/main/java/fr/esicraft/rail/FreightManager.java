package fr.esicraft.rail;

import fr.esicraft.EsicraftPlugin;
import fr.esicraft.age.AgeType;
import fr.esicraft.data.PlayerData;
import fr.esicraft.gui.holder.BaseGUIHolder;
import fr.esicraft.guild.Guild;
import fr.esicraft.guild.GuildRole;
import fr.esicraft.util.ChatUtil;
import fr.esicraft.util.ChunkKey;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.event.ClickEvent;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Sound;
import org.bukkit.World;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.entity.minecart.StorageMinecart;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.player.PlayerInteractEntityEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.vehicle.VehicleDamageEvent;
import org.bukkit.event.vehicle.VehicleDestroyEvent;
import org.bukkit.event.vehicle.VehicleEnterEvent;
import org.bukkit.event.world.EntitiesLoadEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;

/**
 * Trains de fret : une guilde (à partir de l'Âge 2) charge un wagonnet à coffre (54 emplacements) qui voyage tout seul d'une gare à l'autre en suivant
 * le tracé des rails, lentement (4 à 20 minutes selon la distance). Ce n'est pas un vrai wagonnet : sa position est calculée à partir de l'heure, il ne
 * devient visible que près des joueurs. Une guilde sans traité avec l'expéditeur peut l'intercepter et le piller quand il traverse son territoire.
 */
public class FreightManager implements Listener {
   private static final String TAG = "esicraft_freight";

   static final class Convoy {
      final String id;
      final String guildId;
      final UUID sender;
      final String senderName;
      final String fromStation;
      final String toStation;
      final String world;
      final long started;
      final long duration;
      final long[] path;
      ItemStack[] cargo;
      int hitsLeft;
      StorageMinecart cart;
      String lastOwner = "";
      long lastHit;

      Convoy(String id, String guildId, UUID sender, String senderName, String fromStation, String toStation, String world, long started, long duration, long[] path, ItemStack[] cargo, int hits) {
         this.id = id;
         this.guildId = guildId;
         this.sender = sender;
         this.senderName = senderName;
         this.fromStation = fromStation;
         this.toStation = toStation;
         this.world = world;
         this.started = started;
         this.duration = duration;
         this.path = path;
         this.cargo = cargo;
         this.hitsLeft = hits;
      }
   }

   private static final class Pending {
      final RailSearch.Reached trip;
      final ItemStack[] cargo;
      final long expires;

      Pending(RailSearch.Reached trip, ItemStack[] cargo, long expires) {
         this.trip = trip;
         this.cargo = cargo;
         this.expires = expires;
      }
   }

   static final class LoadHolder extends BaseGUIHolder {
      RailSearch.Reached trip;
      boolean done;
   }

   static final class BoxHolder extends BaseGUIHolder {
      String guildId;
      String stationId;
   }

   private final EsicraftPlugin plugin;
   private final RailManager rails;
   private final Map<String, Convoy> convoys = new ConcurrentHashMap<>();
   private final Map<UUID, Convoy> byCart = new ConcurrentHashMap<>();
   private final Map<UUID, Pending> pending = new ConcurrentHashMap<>();
   private int tickCounter;

   public FreightManager(EsicraftPlugin plugin, RailManager rails) {
      this.plugin = plugin;
      this.rails = rails;
   }

   // ------------------------------------------------------------------ persistance

   private static String encodeItems(ItemStack[] items) {
      return Base64.getEncoder().encodeToString(ItemStack.serializeItemsAsBytes(items));
   }

   private static ItemStack[] decodeItems(String data) {
      if (data == null || data.isEmpty()) {
         return new ItemStack[0];
      }

      return ItemStack.deserializeItemsFromBytes(Base64.getDecoder().decode(data));
   }

   private static String encodePath(long[] path) {
      ByteBuffer buf = ByteBuffer.allocate(path.length * 8);
      for (long p : path) {
         buf.putLong(p);
      }

      return Base64.getEncoder().encodeToString(buf.array());
   }

   private static long[] decodePath(String data) {
      ByteBuffer buf = ByteBuffer.wrap(Base64.getDecoder().decode(data));
      long[] path = new long[buf.remaining() / 8];
      for (int i = 0; i < path.length; i++) {
         path[i] = buf.getLong();
      }

      return path;
   }

   private void async(Runnable r) {
      if (this.plugin.isEnabled()) {
         Bukkit.getScheduler().runTaskAsynchronously(this.plugin, r);
      } else {
         r.run();
      }
   }

   public void init() {
      var db = this.plugin.getDataManager().getStorage();
      db.exec("CREATE TABLE IF NOT EXISTS freight_convoys (id VARCHAR(40) PRIMARY KEY, guild_id VARCHAR(64), sender VARCHAR(36), sender_name VARCHAR(40), from_station VARCHAR(40), to_station VARCHAR(40), world VARCHAR(64), started BIGINT, duration BIGINT, path TEXT, cargo TEXT);");
      db.exec("CREATE TABLE IF NOT EXISTS freight_boxes (guild_id VARCHAR(64), station_id VARCHAR(40), cargo TEXT, PRIMARY KEY (guild_id, station_id));");
      for (Object[] row : db.select("SELECT id, guild_id, sender, sender_name, from_station, to_station, world, started, duration, path, cargo FROM freight_convoys;", 11)) {
         try {
            Convoy c = new Convoy(String.valueOf(row[0]), String.valueOf(row[1]), UUID.fromString(String.valueOf(row[2])), String.valueOf(row[3]), String.valueOf(row[4]), String.valueOf(row[5]),
               String.valueOf(row[6]), ((Number)row[7]).longValue(), ((Number)row[8]).longValue(), decodePath(String.valueOf(row[9])), decodeItems(String.valueOf(row[10])), this.hitsToLoot());
            this.convoys.put(c.id, c);
         } catch (RuntimeException e) {
            this.plugin.getLogger().warning("Convoi illisible ignoré : " + e.getMessage());
         }
      }

      Bukkit.getScheduler().runTaskTimer(this.plugin, this::tick, 40L, 2L);
   }

   public void shutdown() {
      for (Convoy c : this.convoys.values()) {
         this.despawn(c);
      }

      for (Map.Entry<UUID, Pending> e : this.pending.entrySet()) {
         Player p = Bukkit.getPlayer(e.getKey());
         if (p != null) {
            this.giveBack(p, e.getValue().cargo);
         }
      }

      this.pending.clear();
   }

   /** /game reset : plus aucun convoi (les tables sont vidées par le reset). */
   public void clear() {
      for (Convoy c : this.convoys.values()) {
         this.despawn(c);
      }

      this.convoys.clear();
      this.byCart.clear();
      this.pending.clear();
   }

   public java.util.Collection<Convoy> convoys() {
      return List.copyOf(this.convoys.values());
   }

   // ------------------------------------------------------------------ configuration

   private int hitsToLoot() {
      return Math.max(1, this.plugin.getConfig().getInt("rail.freight.hits-to-loot", 25));
   }

   private long durationFor(double distance) {
      List<Integer> minutes = this.plugin.getConfig().getIntegerList("rail.freight.minutes");
      List<Integer> limits = this.plugin.getConfig().getIntegerList("rail.freight.distance-limits");
      if (minutes.size() != limits.size() + 1) {
         minutes = List.of(4, 8, 12, 15, 20);
         limits = List.of(1000, 3000, 5000, 10000);
      }

      for (int i = 0; i < limits.size(); i++) {
         if (distance <= limits.get(i)) {
            return minutes.get(i) * 60000L;
         }
      }

      return minutes.get(minutes.size() - 1) * 60000L;
   }

   // ------------------------------------------------------------------ chargement

   /** Maj + clic sur une destination du menu /station : ouvre le coffre de chargement du convoi. */
   public void openLoading(Player player, RailSearch.Result result, RailSearch.Reached trip) {
      Guild guild = this.plugin.getGuildManager().getGuildByPlayer(player.getUniqueId());
      if (guild == null) {
         ChatUtil.sendMessage(player, "&c❌ Seule une guilde peut expédier du fret.");
         return;
      }

      int minAge = this.plugin.getConfig().getInt("rail.freight.min-age", 2);
      if (guild.getCurrentAge().getLevel() < minAge) {
         ChatUtil.sendMessage(player, "&c❌ Le transport d'objets par wagonnet exige l'" + AgeType.values()[Math.max(0, minAge - 1)].getDisplayName() + "&c.");
         return;
      }

      if (!guild.hasRoleAtLeast(player.getUniqueId(), GuildRole.OFFICER)) {
         ChatUtil.sendMessage(player, "&c❌ Seuls les officiers peuvent expédier un convoi de fret.");
         return;
      }

      int max = Math.max(1, this.plugin.getConfig().getInt("rail.freight.max-per-guild", 2));
      long active = this.convoys.values().stream().filter(c -> c.guildId.equals(guild.getId())).count();
      if (active >= max) {
         ChatUtil.sendMessage(player, "&c❌ Votre guilde a déjà " + max + " convoi(s) en route.");
         return;
      }

      if (trip.path() == null || trip.path().length < 2) {
         ChatUtil.sendMessage(player, "&c❌ Itinéraire introuvable pour ce convoi.");
         return;
      }

      LoadHolder holder = new LoadHolder();
      holder.trip = trip;
      Inventory inv = Bukkit.createInventory(holder, 54, ChatUtil.component("&8&l🚂 Fret vers " + trip.station().getName()));
      player.openInventory(inv);
      ChatUtil.sendMessage(player, "&6🚂 Placez les objets à expédier (jusqu'à 54 emplacements), puis fermez le coffre pour confirmer.");
   }

   @EventHandler
   public void onClose(InventoryCloseEvent event) {
      if (!(event.getPlayer() instanceof Player player)) {
         return;
      }

      if (event.getInventory().getHolder() instanceof LoadHolder holder && !holder.done) {
         holder.done = true;
         ItemStack[] cargo = event.getInventory().getContents();
         boolean empty = true;
         for (ItemStack it : cargo) {
            if (it != null && !it.getType().isAir()) {
               empty = false;
            }
         }

         if (empty) {
            return;
         }

         this.pending.put(player.getUniqueId(), new Pending(holder.trip, cargo, System.currentTimeMillis() + 60000L));
         RailManager.Quote q = this.rails.quote(player, holder.trip);
         double fee = q == null ? 0.0 : q.fare() * this.plugin.getConfig().getDouble("rail.freight.fare-multiplier", 5.0);
         player.sendMessage(Component.text("🚂 Envoi vers " + holder.trip.station().getName() + " — frais : " + String.format(Locale.ROOT, "%.2f", fee) + " pièces. ", NamedTextColor.GOLD)
            .append(Component.text("[ENVOYER]", NamedTextColor.GREEN).clickEvent(ClickEvent.runCommand("/station fret confirmer")))
            .append(Component.text("  "))
            .append(Component.text("[ANNULER]", NamedTextColor.RED).clickEvent(ClickEvent.runCommand("/station fret annuler"))));
      } else if (event.getInventory().getHolder() instanceof BoxHolder box) {
         this.saveBox(box.guildId, box.stationId, event.getInventory().getContents());
      }
   }

   private void giveBack(Player p, ItemStack[] cargo) {
      for (ItemStack it : cargo) {
         if (it == null || it.getType().isAir()) {
            continue;
         }

         for (ItemStack left : p.getInventory().addItem(it).values()) {
            p.getWorld().dropItemNaturally(p.getLocation(), left);
         }
      }
   }

   public void confirm(Player player) {
      Pending p = this.pending.remove(player.getUniqueId());
      if (p == null || p.expires < System.currentTimeMillis()) {
         if (p != null) {
            this.giveBack(player, p.cargo);
         }

         ChatUtil.sendMessage(player, "&c❌ Aucun envoi en attente (ou il a expiré : vos objets vous ont été rendus).");
         return;
      }

      Guild guild = this.plugin.getGuildManager().getGuildByPlayer(player.getUniqueId());
      Station origin = this.rails.nearestStation(player.getLocation(), 12.0);
      Station dest = p.trip.station();
      if (guild == null || origin == null || this.rails.stationById(dest.getId()) == null) {
         this.giveBack(player, p.cargo);
         ChatUtil.sendMessage(player, "&c❌ Envoi impossible (restez près de votre gare de départ). Objets rendus.");
         return;
      }

      RailManager.Quote q = this.rails.quote(player, p.trip);
      double fee = q == null ? 0.0 : q.fare() * this.plugin.getConfig().getDouble("rail.freight.fare-multiplier", 5.0);
      if (q == null) {
         this.giveBack(player, p.cargo);
         ChatUtil.sendMessage(player, "&c❌ Une guilde traversée vous interdit désormais le passage. Objets rendus.");
         return;
      }

      PlayerData data = this.plugin.getDataManager().getPlayerData(player.getUniqueId());
      if (data == null || !data.removeCoins(fee)) {
         this.giveBack(player, p.cargo);
         ChatUtil.sendMessage(player, "&c❌ Il vous faut &6" + String.format(Locale.ROOT, "%.2f", fee) + " pièces. Objets rendus.");
         return;
      }

      this.plugin.getDataManager().savePlayer(data);
      Guild destGuild = this.rails.guildOf(dest);
      if (destGuild != null && fee > 0.0) {
         destGuild.depositBank(fee);
         this.plugin.getDataManager().saveGuild(destGuild);
      }

      double dx = dest.getRailX() - origin.getRailX();
      double dz = dest.getRailZ() - origin.getRailZ();
      long duration = this.durationFor(Math.sqrt(dx * dx + dz * dz));
      Convoy c = new Convoy(UUID.randomUUID().toString(), guild.getId(), player.getUniqueId(), player.getName(), origin.getId(), dest.getId(), origin.getWorld(), System.currentTimeMillis(), duration, p.trip.path(), p.cargo, this.hitsToLoot());
      this.convoys.put(c.id, c);
      String cargo = encodeItems(c.cargo);
      String path = encodePath(c.path);
      this.async(() -> this.plugin.getDataManager().getStorage().exec(
         "INSERT INTO freight_convoys (id, guild_id, sender, sender_name, from_station, to_station, world, started, duration, path, cargo) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?);",
         c.id, c.guildId, c.sender.toString(), c.senderName, c.fromStation, c.toStation, c.world, c.started, c.duration, path, cargo));
      ChatUtil.broadcastGuild(guild, "&6🚂 Convoi de fret parti de &e" + origin.getName() + " &6vers &e" + dest.getName() + " &6— arrivée dans environ &e" + (duration / 60000L) + " minutes&6. Protégez la ligne !");
      ChatUtil.playSound(player, Sound.ENTITY_MINECART_INSIDE, 1.0F, 1.0F);
   }

   public void cancel(Player player) {
      Pending p = this.pending.remove(player.getUniqueId());
      if (p == null) {
         ChatUtil.sendMessage(player, "&cAucun envoi en attente.");
         return;
      }

      this.giveBack(player, p.cargo);
      ChatUtil.sendMessage(player, "&7Envoi annulé : vos objets vous ont été rendus.");
   }

   @EventHandler
   public void onQuit(PlayerQuitEvent event) {
      Pending p = this.pending.remove(event.getPlayer().getUniqueId());
      if (p != null) {
         this.giveBack(event.getPlayer(), p.cargo);
      }
   }

   // ------------------------------------------------------------------ livraisons

   private void saveBox(String guildId, String stationId, ItemStack[] items) {
      String data = encodeItems(items);
      this.async(() -> this.plugin.getDataManager().getStorage().exec(
         "INSERT INTO freight_boxes (guild_id, station_id, cargo) VALUES (?, ?, ?) ON CONFLICT(guild_id, station_id) DO UPDATE SET cargo = excluded.cargo;", guildId, stationId, data));
   }

   private ItemStack[] readBox(String guildId, String stationId) {
      for (Object[] row : this.plugin.getDataManager().getStorage().select("SELECT cargo FROM freight_boxes WHERE guild_id = ? AND station_id = ?;", 1, guildId, stationId)) {
         try {
            return decodeItems(String.valueOf(row[0]));
         } catch (RuntimeException ignored) {
         }
      }

      return new ItemStack[0];
   }

   /** /station fret recuperer : ouvre les marchandises livrées à la gare où se tient l'officier. */
   public void openBox(Player player) {
      Guild guild = this.plugin.getGuildManager().getGuildByPlayer(player.getUniqueId());
      Station station = this.rails.nearestStation(player.getLocation(), 8.0);
      if (guild == null || station == null) {
         ChatUtil.sendMessage(player, "&c🚉 Tenez-vous dans une gare (à moins de 8 blocs d'un panneau [Gare]) pour récupérer vos livraisons.");
         return;
      }

      if (!guild.hasRoleAtLeast(player.getUniqueId(), GuildRole.OFFICER)) {
         ChatUtil.sendMessage(player, "&c❌ Réservé aux officiers.");
         return;
      }

      BoxHolder holder = new BoxHolder();
      holder.guildId = guild.getId();
      holder.stationId = station.getId();
      Inventory inv = Bukkit.createInventory(holder, 54, ChatUtil.component("&8&l📦 Livraisons — " + station.getName()));
      for (ItemStack it : this.readBox(guild.getId(), station.getId())) {
         if (it != null) {
            inv.addItem(it);
         }
      }

      player.openInventory(inv);
   }

   private void deliver(Convoy c, boolean silent) {
      this.convoys.remove(c.id);
      this.despawn(c);
      this.async(() -> this.plugin.getDataManager().getStorage().exec("DELETE FROM freight_convoys WHERE id = ?;", c.id));
      Station dest = this.rails.stationById(c.toStation);
      Guild guild = this.plugin.getGuildManager().getGuildById(c.guildId);
      List<ItemStack> leftovers = new ArrayList<>();
      int donated = 0;
      for (ItemStack it : c.cargo) {
         if (it == null || it.getType().isAir()) {
            continue;
         }

         ItemStack rest = it.clone();
         if (guild != null && dest != null && dest.getGuildId().equals(c.guildId)) {
            int before = rest.getAmount();
            rest = this.plugin.getAgeManager().donateFreight(guild, rest);
            donated += before - (rest == null ? 0 : rest.getAmount());
         }

         if (rest != null && rest.getAmount() > 0) {
            leftovers.add(rest);
         }
      }

      if (dest != null && !leftovers.isEmpty()) {
         List<ItemStack> all = new ArrayList<>(java.util.Arrays.asList(this.readBox(c.guildId, dest.getId())));
         all.removeIf(java.util.Objects::isNull);
         Inventory tmp = Bukkit.createInventory(null, 54);
         for (ItemStack it : all) {
            tmp.addItem(it);
         }

         World w = Bukkit.getWorld(dest.getWorld());
         for (ItemStack it : leftovers) {
            for (ItemStack overflow : tmp.addItem(it).values()) {
               if (w != null) {
                  w.dropItemNaturally(new Location(w, dest.getRailX() + 0.5, dest.getRailY() + 1.0, dest.getRailZ() + 0.5), overflow);
               }
            }
         }

         this.saveBox(c.guildId, dest.getId(), tmp.getContents());
      }

      if (guild != null && !silent) {
         ChatUtil.broadcastGuild(guild, "&a🚂 Le convoi de fret est arrivé à &e" + (dest != null ? dest.getName() : "destination") + "&a !"
            + (donated > 0 ? " &7" + donated + " objet(s) ont été offerts automatiquement à la progression d'Âge." : "") + (leftovers.isEmpty() ? "" : " &7Récupérez le reste avec &e/station fret recuperer&7 dans cette gare."));
         this.plugin.getAchievements().grantGuild(guild, fr.esicraft.life.Achievement.FREIGHT_RUNNER, true);
      }
   }

   // ------------------------------------------------------------------ déplacement virtuel

   private Location positionOf(Convoy c, double t, World world) {
      int n = c.path.length;
      double f = Math.max(0.0, Math.min(1.0, t)) * (n - 1);
      int i = Math.min(n - 2, (int)f);
      double frac = f - i;
      double x1 = RailSearch.unpackX(c.path[i]) + 0.5;
      double y1 = RailSearch.unpackY(c.path[i]) + 0.1;
      double z1 = RailSearch.unpackZ(c.path[i]) + 0.5;
      double x2 = RailSearch.unpackX(c.path[i + 1]) + 0.5;
      double y2 = RailSearch.unpackY(c.path[i + 1]) + 0.1;
      double z2 = RailSearch.unpackZ(c.path[i + 1]) + 0.5;
      Location loc = new Location(world, x1 + (x2 - x1) * frac, y1 + (y2 - y1) * frac, z1 + (z2 - z1) * frac);
      loc.setYaw((float)Math.toDegrees(Math.atan2(-(x2 - x1), z2 - z1)));
      return loc;
   }

   private void tick() {
      this.tickCounter++;
      long now = System.currentTimeMillis();
      this.pending.entrySet().removeIf(e -> {
         if (e.getValue().expires < now) {
            Player p = Bukkit.getPlayer(e.getKey());
            if (p != null) {
               this.giveBack(p, e.getValue().cargo);
               ChatUtil.sendMessage(p, "&7Envoi de fret expiré : vos objets vous ont été rendus.");
            }

            return true;
         }

         return false;
      });
      for (Convoy c : List.copyOf(this.convoys.values())) {
         World world = Bukkit.getWorld(c.world);
         if (world == null || c.path.length < 2) {
            continue;
         }

         double t = (double)(now - c.started) / c.duration;
         if (t >= 1.0) {
            this.deliver(c, false);
            continue;
         }

         Location loc = this.positionOf(c, t, world);
         boolean watched = false;
         for (Player p : world.getPlayers()) {
            if (p.getLocation().distanceSquared(loc) <= 96.0 * 96.0) {
               watched = true;
               break;
            }
         }

         if (!watched || !world.isChunkLoaded(loc.getBlockX() >> 4, loc.getBlockZ() >> 4)) {
            this.despawn(c);
         } else if (c.cart == null || !c.cart.isValid()) {
            this.spawn(c, loc);
         } else {
            c.cart.teleport(loc);
         }

         if (this.tickCounter % 10 == 0) {
            this.watchTerritory(c, loc);
         }
      }
   }

   private void spawn(Convoy c, Location loc) {
      Guild g = this.plugin.getGuildManager().getGuildById(c.guildId);
      c.cart = loc.getWorld().spawn(loc, StorageMinecart.class, cart -> {
         cart.addScoreboardTag(TAG);
         cart.setPersistent(false);
         cart.setGravity(false);
         cart.setSlowWhenEmpty(false);
         cart.setMaxSpeed(0.0);
         cart.setDerailedVelocityMod(new org.bukkit.util.Vector(0, 0, 0));
         cart.setFlyingVelocityMod(new org.bukkit.util.Vector(0, 0, 0));
         cart.customName(ChatUtil.component("&6🚂 Fret de " + (g != null ? g.getName() : "?")));
         cart.setCustomNameVisible(true);
      });
      this.byCart.put(c.cart.getUniqueId(), c);
   }

   private void despawn(Convoy c) {
      if (c.cart != null) {
         this.byCart.remove(c.cart.getUniqueId());
         if (c.cart.isValid()) {
            c.cart.remove();
         }

         c.cart = null;
      }
   }

   /** Prévient les deux guildes quand le convoi franchit une frontière. */
   private void watchTerritory(Convoy c, Location loc) {
      Guild owner = this.plugin.getGuildManager().getGuildByChunk(new ChunkKey(loc.getWorld().getName(), loc.getBlockX() >> 4, loc.getBlockZ() >> 4));
      String ownerId = owner == null ? "" : owner.getId();
      if (ownerId.equals(c.lastOwner)) {
         return;
      }

      c.lastOwner = ownerId;
      if (owner == null || owner.getId().equals(c.guildId)) {
         return;
      }

      Guild sender = this.plugin.getGuildManager().getGuildById(c.guildId);
      boolean safe = this.plugin.getTreatyManager().between(owner.getId(), c.guildId) != null;
      if (sender != null) {
         ChatUtil.broadcastGuild(sender, safe ? "&7🚂 Votre convoi traverse le territoire de &e" + owner.getName() + " &7(traité : passage sûr)."
            : "&c⚠ Votre convoi entre sur le territoire de &e" + owner.getName() + " &c— sans traité, il peut être pillé !");
      }

      if (!safe) {
         ChatUtil.broadcastGuild(owner, "&6🏹 Un convoi de fret de &e" + (sender != null ? sender.getName() : "?") + " &6traverse votre territoire en " + loc.getBlockX() + ", " + loc.getBlockZ()
            + " ! Frappez-le " + this.hitsToLoot() + " fois pour le piller.");
      }
   }

   // ------------------------------------------------------------------ interception

   private Convoy convoyOf(Entity e) {
      return e == null ? null : this.byCart.get(e.getUniqueId());
   }

   private void hit(Player attacker, Convoy c) {
      long now = System.currentTimeMillis();
      if (now - c.lastHit < 150L) {
         return;
      }

      Guild mine = this.plugin.getGuildManager().getGuildByPlayer(attacker.getUniqueId());
      if (mine == null || mine.getId().equals(c.guildId)) {
         ChatUtil.sendActionbar(attacker, "&7🚂 Ce wagon appartient à une guilde qui n'est pas votre ennemie.");
         return;
      }

      Location loc = c.cart.getLocation();
      Guild owner = this.plugin.getGuildManager().getGuildByChunk(new ChunkKey(loc.getWorld().getName(), loc.getBlockX() >> 4, loc.getBlockZ() >> 4));
      if (owner == null || !owner.getId().equals(mine.getId())) {
         ChatUtil.sendActionbar(attacker, "&c🚂 Vous ne pouvez piller un convoi que sur le territoire de votre propre guilde.");
         return;
      }

      if (this.plugin.getTreatyManager().between(mine.getId(), c.guildId) != null) {
         ChatUtil.sendActionbar(attacker, "&c🚂 Un traité vous lie à cette guilde : le convoi est intouchable.");
         return;
      }

      c.lastHit = now;
      c.hitsLeft--;
      c.cart.getWorld().playSound(loc, Sound.ENTITY_ITEM_BREAK, 1.0F, 0.7F);
      c.cart.getWorld().spawnParticle(org.bukkit.Particle.CRIT, loc.clone().add(0, 0.8, 0), 10, 0.3, 0.3, 0.3, 0.1);
      if (c.hitsLeft > 0) {
         ChatUtil.sendActionbar(attacker, "&6🏹 Convoi attaqué : encore &e" + c.hitsLeft + " &6coups avant le pillage !");
         return;
      }

      this.loot(c, attacker, mine, loc);
   }

   private void loot(Convoy c, Player attacker, Guild looters, Location loc) {
      this.convoys.remove(c.id);
      this.despawn(c);
      this.async(() -> this.plugin.getDataManager().getStorage().exec("DELETE FROM freight_convoys WHERE id = ?;", c.id));
      for (ItemStack it : c.cargo) {
         if (it != null && !it.getType().isAir()) {
            loc.getWorld().dropItemNaturally(loc, it);
         }
      }

      loc.getWorld().spawnParticle(org.bukkit.Particle.EXPLOSION, loc, 3, 0.5, 0.5, 0.5, 0.0);
      loc.getWorld().playSound(loc, Sound.ENTITY_GENERIC_EXPLODE, 2.0F, 1.0F);
      Guild sender = this.plugin.getGuildManager().getGuildById(c.guildId);
      ChatUtil.broadcast("&c🏹 Le convoi de fret de &e" + (sender != null ? sender.getName() : "?") + " &ca été pillé par &e" + looters.getName() + " &c! Le butin est éparpillé sur les rails.");
      this.plugin.getAchievements().grant(attacker.getUniqueId(), fr.esicraft.life.Achievement.CONVOY_RAIDER);
      this.plugin.getChronicle().log(fr.esicraft.life.Chronicle.Kind.RAID, looters.getName() + " pille le convoi de fret de " + (sender != null ? sender.getName() : "?") + ".", looters.getId(), c.guildId);
   }

   @EventHandler(priority = EventPriority.HIGHEST)
   public void onVehicleDamage(VehicleDamageEvent event) {
      Convoy c = this.convoyOf(event.getVehicle());
      if (c != null) {
         event.setCancelled(true);
         if (event.getAttacker() instanceof Player p) {
            this.hit(p, c);
         }
      }
   }

   @EventHandler(priority = EventPriority.HIGHEST)
   public void onDamage(EntityDamageEvent event) {
      Convoy c = this.convoyOf(event.getEntity());
      if (c != null) {
         event.setCancelled(true);
         if (event instanceof org.bukkit.event.entity.EntityDamageByEntityEvent byEntity) {
            Player p = fr.esicraft.combat.CombatManager.attackerOf(byEntity.getDamager());
            if (p != null) {
               this.hit(p, c);
            }
         }
      }
   }

   @EventHandler(priority = EventPriority.HIGHEST)
   public void onDestroy(VehicleDestroyEvent event) {
      if (this.convoyOf(event.getVehicle()) != null) {
         event.setCancelled(true);
      }
   }

   @EventHandler(priority = EventPriority.HIGHEST)
   public void onEnter(VehicleEnterEvent event) {
      if (this.convoyOf(event.getVehicle()) != null) {
         event.setCancelled(true);
      }
   }

   @EventHandler(priority = EventPriority.HIGHEST)
   public void onInteract(PlayerInteractEntityEvent event) {
      Convoy c = this.convoyOf(event.getRightClicked());
      if (c != null) {
         event.setCancelled(true);
         this.hit(event.getPlayer(), c);
      }
   }

   /** Supprime les wagons de fret orphelins laissés par un arrêt brutal. */
   @EventHandler
   public void onEntitiesLoad(EntitiesLoadEvent event) {
      for (Entity e : event.getEntities()) {
         if (e.getScoreboardTags().contains(TAG) && !this.byCart.containsKey(e.getUniqueId())) {
            e.remove();
         }
      }
   }
}
