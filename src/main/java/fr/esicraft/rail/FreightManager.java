package fr.esicraft.rail;

import fr.esicraft.EsicraftPlugin;
import fr.esicraft.age.AgeType;
import fr.esicraft.data.PlayerData;
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
import org.bukkit.inventory.InventoryHolder;
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
      boolean watched;

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

   /** Chargement en cours d'un joueur : les objets de tous les coffres déjà remplis, en attente d'envoi. */
   private static final class Pending {
      final RailSearch.Reached trip;
      ItemStack[] cargo = new ItemStack[0];
      /** Coffres ouverts (vides ou non) et coffres contenant des objets. */
      int opened;
      int filled;
      long expires;

      Pending(RailSearch.Reached trip, long expires) {
         this.trip = trip;
         this.expires = expires;
      }

      void add(ItemStack[] chest) {
         List<ItemStack> all = new ArrayList<>(java.util.Arrays.asList(this.cargo));
         boolean any = false;
         for (ItemStack it : chest) {
            if (it != null && !it.getType().isAir()) {
               all.add(it);
               any = true;
            }
         }

         this.cargo = all.toArray(new ItemStack[0]);
         if (any) {
            this.filled++;
         }
      }
   }

   /** Coffre de chargement : ce n'est volontairement PAS un BaseGUIHolder (le listener global annulerait tous les clics et rien ne pourrait y être déposé). */
   static final class LoadHolder implements InventoryHolder {
      RailSearch.Reached trip;
      boolean done;

      @Override
      public Inventory getInventory() {
         return null;
      }
   }

   static final class BoxHolder implements InventoryHolder {
      String stationId;

      @Override
      public Inventory getInventory() {
         return null;
      }
   }

   private final EsicraftPlugin plugin;
   private final RailManager rails;
   private final Map<String, Convoy> convoys = new ConcurrentHashMap<>();
   private final Map<UUID, Convoy> byCart = new ConcurrentHashMap<>();
   private final Map<UUID, Pending> pending = new ConcurrentHashMap<>();
   /** Boîte de livraison d'une gare : plusieurs coffres (pages) de 54 emplacements, visibles de tous les joueurs présents à la gare. */
   private static final class Box {
      final String stationId;
      final List<Inventory> pages = new ArrayList<>();

      Box(String stationId) {
         this.stationId = stationId;
      }
   }

   /** Une seule boîte par gare en mémoire : tout le monde partage les mêmes inventaires, donc rien ne se duplique. */
   private final Map<String, Box> boxes = new ConcurrentHashMap<>();
   /** Toutes les écritures passent par ce fil unique : leur ordre est garanti. */
   private final java.util.concurrent.ExecutorService io = java.util.concurrent.Executors.newSingleThreadExecutor(r -> {
      Thread t = new Thread(r, "esicraft-freight-io");
      t.setDaemon(true);
      return t;
   });
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
      try {
         this.io.execute(r);
      } catch (java.util.concurrent.RejectedExecutionException e) {
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

      for (Box b : this.boxes.values()) {
         this.saveBox(b);
      }

      this.io.shutdown();
      try {
         this.io.awaitTermination(10L, java.util.concurrent.TimeUnit.SECONDS);
      } catch (InterruptedException ignored) {
         Thread.currentThread().interrupt();
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
      this.boxes.clear();
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

      Pending old = this.pending.remove(player.getUniqueId());
      if (old != null) {
         this.giveBack(player, old.cargo);
      }

      Pending fresh = new Pending(trip, System.currentTimeMillis() + 180000L);
      this.pending.put(player.getUniqueId(), fresh);
      ChatUtil.sendMessage(player, "&6🚂 Votre guilde peut envoyer jusqu'à &e" + this.maxChests(guild) + " double(s) coffre(s) &6par convoi (Âge " + guild.getCurrentAge().getLevel() + "). Placez les objets, puis fermez le coffre.");
      this.openChest(player, fresh);
   }

   /** Nombre de doubles coffres d'un convoi selon l'Âge de la guilde (2 : 1, 3 : 2, 4 : 3, 5 : 5, 6 : 10). */
   public int maxChests(Guild guild) {
      List<Integer> table = this.plugin.getConfig().getIntegerList("rail.freight.chests-by-age");
      if (table.size() < 5) {
         table = List.of(1, 2, 3, 5, 10);
      }

      int index = Math.max(0, Math.min(table.size() - 1, guild.getCurrentAge().getLevel() - 2));
      return Math.max(1, table.get(index));
   }

   private void openChest(Player player, Pending p) {
      p.opened++;
      p.expires = System.currentTimeMillis() + 180000L;
      LoadHolder holder = new LoadHolder();
      holder.trip = p.trip;
      Inventory inv = Bukkit.createInventory(holder, 54, ChatUtil.component("&8&l🚂 Fret vers " + p.trip.station().getName() + " &7(coffre " + p.opened + ")"));
      player.openInventory(inv);
   }

   /** /station fret ajouter : ouvre un double coffre supplémentaire dans la limite de l'Âge. */
   public void addChest(Player player) {
      Pending p = this.pending.get(player.getUniqueId());
      Guild guild = this.plugin.getGuildManager().getGuildByPlayer(player.getUniqueId());
      if (p == null || guild == null || p.expires < System.currentTimeMillis()) {
         ChatUtil.sendMessage(player, "&c❌ Aucun chargement en cours (Maj + clic sur une destination dans /station).");
         return;
      }

      if (p.opened >= this.maxChests(guild)) {
         ChatUtil.sendMessage(player, "&c❌ Limite atteinte : votre Âge autorise " + this.maxChests(guild) + " double(s) coffre(s) par convoi.");
         return;
      }

      this.openChest(player, p);
   }

   @EventHandler
   public void onClose(InventoryCloseEvent event) {
      if (!(event.getPlayer() instanceof Player player)) {
         return;
      }

      if (event.getInventory().getHolder() instanceof LoadHolder holder && !holder.done) {
         holder.done = true;
         Pending p = this.pending.get(player.getUniqueId());
         Guild guild = this.plugin.getGuildManager().getGuildByPlayer(player.getUniqueId());
         if (p == null || guild == null) {
            this.giveBack(player, event.getInventory().getContents());
            return;
         }

         p.add(event.getInventory().getContents());
         p.expires = System.currentTimeMillis() + 180000L;
         if (p.filled == 0) {
            this.pending.remove(player.getUniqueId());
            return;
         }

         RailManager.Quote q = this.rails.quote(player, holder.trip);
         double fee = q == null ? 0.0 : q.fare() * this.plugin.getConfig().getDouble("rail.freight.fare-multiplier", 5.0) * p.filled;
         Component line = Component.text("🚂 " + p.filled + " coffre(s) vers " + holder.trip.station().getName() + " — frais : " + String.format(Locale.ROOT, "%.2f", fee) + " pièces. ", NamedTextColor.GOLD);
         if (p.opened < this.maxChests(guild)) {
            line = line.append(Component.text("[+ COFFRE " + p.opened + "/" + this.maxChests(guild) + "]", NamedTextColor.AQUA).clickEvent(ClickEvent.runCommand("/station fret ajouter"))).append(Component.text("  "));
         }

         player.sendMessage(line
            .append(Component.text("[ENVOYER]", NamedTextColor.GREEN).clickEvent(ClickEvent.runCommand("/station fret confirmer")))
            .append(Component.text("  "))
            .append(Component.text("[ANNULER]", NamedTextColor.RED).clickEvent(ClickEvent.runCommand("/station fret annuler"))));
      } else if (event.getInventory().getHolder() instanceof BoxHolder box) {
         Box b = this.boxes.get(box.stationId);
         if (b != null) {
            this.saveBox(b);
         }
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
      double fee = q == null ? 0.0 : q.fare() * this.plugin.getConfig().getDouble("rail.freight.fare-multiplier", 5.0) * Math.max(1, p.filled);
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

   private static final String PUBLIC = "*";
   private static final int MAX_PAGES = 30;

   private void saveBox(Box box) {
      List<ItemStack> all = new ArrayList<>();
      for (Inventory page : box.pages) {
         for (ItemStack it : page.getContents()) {
            if (it != null && !it.getType().isAir()) {
               all.add(it);
            }
         }
      }

      String data = encodeItems(all.toArray(new ItemStack[0]));
      String station = box.stationId;
      this.async(() -> this.plugin.getDataManager().getStorage().exec(
         "INSERT INTO freight_boxes (guild_id, station_id, cargo) VALUES (?, ?, ?) ON CONFLICT(guild_id, station_id) DO UPDATE SET cargo = excluded.cargo;", PUBLIC, station, data));
   }

   /** Contenu de la gare (toutes les anciennes lignes par guilde sont fusionnées en une seule boîte publique). */
   private List<ItemStack> readBox(String stationId) {
      var db = this.plugin.getDataManager().getStorage();
      List<ItemStack> all = new ArrayList<>();
      var rows = db.select("SELECT guild_id, cargo FROM freight_boxes WHERE station_id = ?;", 2, stationId);
      boolean migrate = rows.size() > 1 || !rows.isEmpty() && !PUBLIC.equals(String.valueOf(rows.get(0)[0]));
      for (Object[] row : rows) {
         try {
            for (ItemStack it : decodeItems(String.valueOf(row[1]))) {
               if (it != null && !it.getType().isAir()) {
                  all.add(it);
               }
            }
         } catch (RuntimeException ignored) {
         }
      }

      if (migrate) {
         String data = encodeItems(all.toArray(new ItemStack[0]));
         db.runInTransaction(() -> {
            db.exec("DELETE FROM freight_boxes WHERE station_id = ?;", stationId);
            db.exec("INSERT INTO freight_boxes (guild_id, station_id, cargo) VALUES (?, ?, ?);", PUBLIC, stationId, data);
         });
      }

      return all;
   }

   private Inventory newPage(Box box) {
      BoxHolder holder = new BoxHolder();
      holder.stationId = box.stationId;
      Station st = this.rails.stationById(box.stationId);
      Inventory page = Bukkit.createInventory(holder, 54, ChatUtil.component("&8&l📦 Livraisons — " + (st != null ? st.getName() : "gare") + " &7(coffre " + (box.pages.size() + 1) + ")"));
      box.pages.add(page);
      return page;
   }

   /** Range un objet dans la boîte (nouveaux coffres au besoin) ; renvoie ce qui ne tient plus (au-delà de 30 coffres). */
   private ItemStack addToBox(Box box, ItemStack item) {
      ItemStack rest = item;
      for (Inventory page : box.pages) {
         java.util.Collection<ItemStack> left = page.addItem(rest).values();
         if (left.isEmpty()) {
            return null;
         }

         rest = left.iterator().next();
      }

      while (box.pages.size() < MAX_PAGES) {
         java.util.Collection<ItemStack> left = this.newPage(box).addItem(rest).values();
         if (left.isEmpty()) {
            return null;
         }

         rest = left.iterator().next();
      }

      return rest;
   }

   /** Donne la boîte partagée de la gare (chargée en tâche de fond à la première utilisation) à l'action, sur le thread principal. */
   private void withBox(String stationId, java.util.function.Consumer<Box> action) {
      Box ready = this.boxes.get(stationId);
      if (ready != null) {
         action.accept(ready);
         return;
      }

      this.async(() -> {
         List<ItemStack> items = this.readBox(stationId);
         Bukkit.getScheduler().runTask(this.plugin, () -> {
            Box box = this.boxes.computeIfAbsent(stationId, k -> {
               Box created = new Box(k);
               this.newPage(created);
               for (ItemStack it : items) {
                  this.addToBox(created, it);
               }

               return created;
            });
            action.accept(box);
         });
      });
   }

   /** /station fret recuperer [coffre] : tout joueur présent à la gare peut prendre les marchandises livrées (le fret sert aussi d'échange entre guildes). */
   public void openBox(Player player, int pageNumber) {
      Station station = this.rails.nearestStation(player.getLocation(), 8.0);
      if (station == null) {
         ChatUtil.sendMessage(player, "&c🚉 Tenez-vous dans une gare (à moins de 8 blocs d'un panneau [Gare]) pour récupérer les livraisons.");
         return;
      }

      this.withBox(station.getId(), box -> {
         if (!player.isOnline()) {
            return;
         }

         int index = Math.max(0, Math.min(box.pages.size() - 1, pageNumber - 1));
         player.openInventory(box.pages.get(index));
         if (box.pages.size() > 1) {
            ChatUtil.sendMessage(player, "&6📦 Coffre &e" + (index + 1) + "&6/&e" + box.pages.size() + "&6 — les autres : &e/station fret recuperer <numéro>&6.");
         }
      });
   }

   /** Supprime la ligne du convoi (et enregistre la boîte du même coup, dans une seule transaction : un plantage ne livre jamais deux fois). */
   private void finishRow(String convoyId, Box box) {
      String station = box == null ? null : box.stationId;
      String data = null;
      if (box != null) {
         List<ItemStack> all = new ArrayList<>();
         for (Inventory page : box.pages) {
            for (ItemStack it : page.getContents()) {
               if (it != null && !it.getType().isAir()) {
                  all.add(it);
               }
            }
         }

         data = encodeItems(all.toArray(new ItemStack[0]));
      }

      String boxData = data;
      this.async(() -> {
         var db = this.plugin.getDataManager().getStorage();
         db.runInTransaction(() -> {
            if (boxData != null) {
               db.exec("INSERT INTO freight_boxes (guild_id, station_id, cargo) VALUES (?, ?, ?) ON CONFLICT(guild_id, station_id) DO UPDATE SET cargo = excluded.cargo;", PUBLIC, station, boxData);
            }

            db.exec("DELETE FROM freight_convoys WHERE id = ?;", convoyId);
         });
      });
   }

   private void deliver(Convoy c, boolean silent) {
      this.convoys.remove(c.id);
      this.despawn(c);
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

      int donatedFinal = donated;
      boolean hasLeftovers = !leftovers.isEmpty();
      // Si la gare d'arrivée a disparu, le fret retourne à la gare de départ.
      Station target = dest != null ? dest : this.rails.stationById(c.fromStation);
      if (target != null && hasLeftovers) {
         World w = Bukkit.getWorld(target.getWorld());
         this.withBox(target.getId(), box -> {
            for (ItemStack it : leftovers) {
               ItemStack overflow = this.addToBox(box, it);
               if (overflow != null && w != null) {
                  w.dropItemNaturally(new Location(w, target.getRailX() + 0.5, target.getRailY() + 1.0, target.getRailZ() + 0.5), overflow);
               }
            }

            this.finishRow(c.id, box);
         });
      } else {
         this.finishRow(c.id, null);
      }

      String where = target != null ? target.getName() : "destination";
      if (guild != null && !silent) {
         ChatUtil.broadcastGuild(guild, "&a🚂 Le convoi de fret est arrivé à &e" + where + "&a !"
            + (donatedFinal > 0 ? " &7" + donatedFinal + " objet(s) ont été offerts automatiquement à la progression d'Âge." : ""));
         this.plugin.getAchievements().grantGuild(guild, fr.esicraft.life.Achievement.FREIGHT_RUNNER, true);
      }

      if (hasLeftovers && target != null) {
         Guild stationGuild = this.rails.guildOf(target);
         String sender = guild != null ? guild.getName() : c.senderName;
         String msg = "&6📦 Un convoi de fret de &e" + sender + " &6est arrivé à la gare &e" + target.getName() + " &6: &e/station fret recuperer &6(ouvert à tous les joueurs présents à la gare).";
         if (stationGuild != null) {
            ChatUtil.broadcastGuild(stationGuild, msg);
         }

         if (guild != null && (stationGuild == null || !stationGuild.getId().equals(guild.getId()))) {
            ChatUtil.broadcastGuild(guild, "&7📦 Les marchandises sont à la disposition de tout joueur présent à la gare &e" + target.getName() + "&7.");
         }
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
         if (this.tickCounter % 5 == 0 || c.cart == null) {
            // Les joueurs proches ne sont recherchés que toutes les 10 ticks : le coût ne dépend plus de convois × joueurs à chaque demi-seconde.
            boolean near = false;
            for (Player p : world.getPlayers()) {
               if (p.getLocation().distanceSquared(loc) <= 96.0 * 96.0) {
                  near = true;
                  break;
               }
            }

            c.watched = near;
         }

         if (!c.watched || !world.isChunkLoaded(loc.getBlockX() >> 4, loc.getBlockZ() >> 4)) {
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

      if (this.plugin.getTruce().denyHostile(attacker)) {
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
      this.finishRow(c.id, null);
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
