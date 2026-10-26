package fr.esicraft.game;

import fr.esicraft.EsicraftPlugin;
import fr.esicraft.data.PlayerData;
import fr.esicraft.gui.holder.BaseGUIHolder;
import fr.esicraft.guild.Guild;
import fr.esicraft.util.ChatUtil;
import fr.esicraft.util.ItemBuilder;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ThreadLocalRandom;
import org.bukkit.Bukkit;
import org.bukkit.Color;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.PotionMeta;
import org.bukkit.inventory.meta.SuspiciousStewMeta;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

/**
 * Jeux d'une taverne, tous contre la banque de la guilde propriétaire (la « maison ») : pile ou face, tirage du bar (nourriture et pintes),
 * roulette européenne et machine à sous. La maison ne paie jamais plus que ce que sa banque peut couvrir : la mise maximale s'adapte.
 */
public class TavernGames implements Listener {
   private static final int[] BETS = {10, 50, 100, 500, 1000};
   private static final int[] CHIP_SLOTS = {10, 11, 12, 13, 14};
   private static final Set<Integer> RED = Set.of(1, 3, 5, 7, 9, 12, 14, 16, 18, 19, 21, 23, 25, 27, 30, 32, 34, 36);

   /** Support commun : guilde-maison, mise choisie et verrou pendant l'animation. */
   static class GameHolder extends BaseGUIHolder {
      final String guildId;
      int bet = 1;
      boolean busy;

      GameHolder(String guildId) {
         this.guildId = guildId;
      }
   }

   static final class Menu extends GameHolder {
      Menu(String g) {
         super(g);
      }
   }

   static final class Flip extends GameHolder {
      Flip(String g) {
         super(g);
      }
   }

   static final class Bar extends GameHolder {
      Bar(String g) {
         super(g);
      }
   }

   static final class Roulette extends GameHolder {
      int number = 17;

      Roulette(String g) {
         super(g);
      }
   }

   static final class Slots extends GameHolder {
      Slots(String g) {
         super(g);
      }
   }

   private final EsicraftPlugin plugin;
   private final Set<UUID> playing = ConcurrentHashMap.newKeySet();

   public TavernGames(EsicraftPlugin plugin) {
      this.plugin = plugin;
   }

   // ------------------------------------------------------------------ outils

   private double maxBet() {
      return Math.max(10.0, this.plugin.getConfig().getDouble("taverns.max-bet", 1000.0));
   }

   private ItemStack glass() {
      return new ItemBuilder(Material.GRAY_STAINED_GLASS_PANE).name(" ").build();
   }

   private void fill(Inventory inv) {
      ItemStack g = this.glass();
      for (int i = 0; i < inv.getSize(); i++) {
         inv.setItem(i, g);
      }
   }

   private ItemStack back() {
      return new ItemBuilder(Material.ARROW).name("&e◀ Retour au menu de la taverne").build();
   }

   private void chips(Inventory inv, GameHolder h) {
      for (int i = 0; i < BETS.length; i++) {
         boolean sel = i == h.bet;
         inv.setItem(CHIP_SLOTS[i], new ItemBuilder(sel ? Material.GOLD_BLOCK : Material.GOLD_NUGGET).name((sel ? "&a▶ " : "&e") + BETS[i] + " pièces").lore("&7Cliquez pour miser ce montant.").build());
      }
   }

   private String houseLine(Guild owner) {
      return "&7Maison : &e" + owner.getName() + " &7— fonds &6" + (long)owner.getBankBalance() + " pièces";
   }

   /** Débite la mise ; renvoie faux (avec message) si impossible. Vérifie aussi que la banque couvre le gain maximal. */
   private boolean take(Player p, Guild owner, int bet, double maxMultiplier) {
      if (this.plugin.getCombatManager().denySpending(p)) {
         return false;
      }

      if (bet > this.maxBet() || owner.getBankBalance() < bet * (maxMultiplier - 1.0)) {
         ChatUtil.sendMessage(p, "&c❌ La banque de la taverne ne couvre pas le gain maximal de cette mise (choisissez une mise plus petite).");
         return false;
      }

      PlayerData data = this.plugin.getDataManager().getPlayerData(p.getUniqueId());
      if (data == null || !data.removeCoins(bet)) {
         ChatUtil.sendMessage(p, "&c❌ Vous n'avez pas assez de pièces.");
         return false;
      }

      this.plugin.getDataManager().savePlayer(data);
      return true;
   }

   /** Règle la mise : le joueur reçoit {@code bet × mult}, la banque encaisse ou paie la différence (dans la limite de ses fonds). */
   private double settle(Guild owner, UUID player, int bet, double mult) {
      double payout = Math.floor(bet * mult);
      if (payout > bet) {
         double need = payout - bet;
         double avail = Math.min(need, owner.getBankBalance());
         owner.withdrawBank(avail);
         payout = bet + avail;
      } else {
         owner.depositBank(bet - payout);
      }

      if (payout > 0) {
         this.plugin.getDataManager().creditPlayer(player, payout);
      }

      this.plugin.getDataManager().saveGuild(owner);
      return payout;
   }

   private void note(UUID id, String message, boolean good) {
      Player p = Bukkit.getPlayer(id);
      if (p != null) {
         ChatUtil.sendMessage(p, message);
         ChatUtil.playSound(p, good ? Sound.ENTITY_PLAYER_LEVELUP : Sound.ENTITY_VILLAGER_NO, 1.0F, good ? 1.5F : 1.0F);
      }
   }

   private void tick(Player p, Sound s, float pitch) {
      if (p != null) {
         ChatUtil.playSound(p, s, 0.6F, pitch);
      }
   }

   // ------------------------------------------------------------------ menu

   public void openMenu(Player player, Guild owner) {
      Menu holder = new Menu(owner.getId());
      Inventory inv = Bukkit.createInventory(holder, 27, ChatUtil.component("&8&l🍺 Taverne de " + owner.getName()));
      this.fill(inv);
      inv.setItem(4, new ItemBuilder(Material.BARREL).name("&6&lLa taverne").lore(this.houseLine(owner), "&7Tous les jeux se jouent contre la banque", "&7de la guilde propriétaire.").build());
      inv.setItem(10, new ItemBuilder(Material.GOLD_INGOT).name("&e&lPile ou face").lore("&7Doublez votre mise… ou perdez-la.", "&7Chance de gagner : 47 %").build());
      inv.setItem(12, new ItemBuilder(Material.POTION).name("&c&lLe bar : tirage").lore("&7Payez un verre et tentez votre chance :", "&7nourriture, soupes… ou une pinte de", "&4Kastell rouge&7 (nausée !).", "&7Une pomme dorée à 1 % !").build());
      inv.setItem(14, new ItemBuilder(Material.RED_WOOL).name("&4&lRoulette").lore("&7Roulette européenne (0 à 36) :", "&7rouge/noir, pair/impair, douzaines, numéro plein.").build());
      inv.setItem(16, new ItemBuilder(Material.BELL).name("&d&lMachine à sous").lore("&7Trois rouleaux : cerises, citrons, cloches,", "&7diamants et sept. Jusqu'à ×50 !").build());
      player.openInventory(inv);
   }

   // ------------------------------------------------------------------ pile ou face

   private void openFlip(Player player, Guild owner) {
      Flip h = new Flip(owner.getId());
      Inventory inv = Bukkit.createInventory(h, 27, ChatUtil.component("&8&l🪙 Pile ou face — " + owner.getName()));
      this.renderFlip(inv, h, owner);
      player.openInventory(inv);
   }

   private void renderFlip(Inventory inv, Flip h, Guild owner) {
      this.fill(inv);
      double chance = this.flipChance();
      inv.setItem(4, new ItemBuilder(Material.BARREL).name("&6&lPile ou face").lore(this.houseLine(owner), "&7Chances de gagner : &e" + (int)Math.round(chance * 100) + " %", "&7Gain : &amise doublée").build());
      this.chips(inv, h);
      inv.setItem(16, new ItemBuilder(Material.GOLD_INGOT).name("&6La pièce").lore("&7Choisissez un côté pour lancer.").build());
      inv.setItem(20, new ItemBuilder(Material.PLAYER_HEAD).name("&e&lPILE").lore("&7Misez sur pile.").build());
      inv.setItem(24, new ItemBuilder(Material.IRON_INGOT).name("&b&lFACE").lore("&7Misez sur face.").build());
      inv.setItem(18, this.back());
   }

   private double flipChance() {
      return Math.max(0.05, Math.min(0.95, this.plugin.getConfig().getDouble("taverns.coinflip.win-chance", 0.47)));
   }

   private void flip(Player player, Flip h, Guild owner, boolean heads, Inventory inv) {
      int bet = BETS[h.bet];
      if (!this.take(player, owner, bet, 2.0)) {
         return;
      }

      h.busy = true;
      this.playing.add(player.getUniqueId());
      boolean win = ThreadLocalRandom.current().nextDouble() < this.flipChance();
      boolean landedHeads = win == heads;
      UUID id = player.getUniqueId();
      int[] frame = {0};
      Bukkit.getScheduler().runTaskTimer(this.plugin, task -> {
         frame[0]++;
         if (frame[0] <= 10) {
            inv.setItem(16, new ItemBuilder(frame[0] % 2 == 0 ? Material.GOLD_INGOT : Material.IRON_INGOT).name("&6La pièce tourne…").build());
            this.tick(Bukkit.getPlayer(id), Sound.BLOCK_NOTE_BLOCK_CHIME, 0.8F + frame[0] * 0.08F);
            return;
         }

         task.cancel();
         h.busy = false;
         this.playing.remove(id);
         double payout = this.settle(owner, id, bet, win ? 2.0 : 0.0);
         this.note(id, win ? "&a🪙 " + (landedHeads ? "Pile" : "Face") + " ! Vous gagnez &6" + (long)(payout - bet) + " pièces&a." : "&c🪙 " + (landedHeads ? "Pile" : "Face") + "… la taverne empoche vos &6" + bet + " pièces&c.", win);
         if (!inv.getViewers().isEmpty()) {
            this.renderFlip(inv, h, owner);
            inv.setItem(16, new ItemBuilder(landedHeads ? Material.GOLD_INGOT : Material.IRON_INGOT).name(landedHeads ? "&e&lPILE !" : "&b&lFACE !").build());
         }
      }, 4L, 4L);
   }

   // ------------------------------------------------------------------ bar : nourriture et pintes

   private void openBar(Player player, Guild owner) {
      Bar h = new Bar(owner.getId());
      Inventory inv = Bukkit.createInventory(h, 27, ChatUtil.component("&8&l🍻 Le bar — " + owner.getName()));
      this.renderBar(inv, owner);
      player.openInventory(inv);
   }

   private int barPrice() {
      return Math.max(1, this.plugin.getConfig().getInt("taverns.bar.price", 50));
   }

   private void renderBar(Inventory inv, Guild owner) {
      this.fill(inv);
      int price = this.barPrice();
      inv.setItem(4, new ItemBuilder(Material.BARREL).name("&6&lLe bar").lore(this.houseLine(owner), "", "&7Chaque verre : 1 tirage.", "&e1 % &7pomme dorée · &e0,0001 % &7pomme dorée enchantée",
         "&e12 % &78 steaks · &e12 % &716 pommes de terre cuites", "&e50 % &4pinte de Kastell rouge&7 (nausée)", "&7Le reste : une soupe suspecte au hasard").build());
      inv.setItem(11, new ItemBuilder(Material.POTION).name("&e1 verre").lore("&7Prix : &6" + price + " pièces").build());
      inv.setItem(13, new ItemBuilder(Material.POTION).amount(5).name("&e5 verres").lore("&7Prix : &6" + price * 5 + " pièces").build());
      inv.setItem(15, new ItemBuilder(Material.POTION).amount(10).name("&e10 verres").lore("&7Prix : &6" + price * 10 + " pièces").build());
      inv.setItem(22, new ItemBuilder(Material.BARREL).name("&7Votre commande apparaîtra ici").build());
      inv.setItem(18, this.back());
   }

   private ItemStack pinte() {
      ItemStack item = new ItemStack(Material.POTION);
      PotionMeta meta = (PotionMeta)item.getItemMeta();
      meta.displayName(ChatUtil.component("&4&lPinte de Kastell rouge"));
      meta.lore(java.util.List.of(ChatUtil.component("&7Une bière rouge très forte… la tête tourne."), ChatUtil.component("&8Nausée")));
      meta.setColor(Color.fromRGB(139, 0, 0));
      meta.addCustomEffect(new PotionEffect(PotionEffectType.NAUSEA, 20 * 30, 0), true);
      meta.addItemFlags(ItemFlag.HIDE_ADDITIONAL_TOOLTIP);
      item.setItemMeta(meta);
      return item;
   }

   private ItemStack stew() {
      ItemStack item = new ItemStack(Material.SUSPICIOUS_STEW);
      SuspiciousStewMeta meta = (SuspiciousStewMeta)item.getItemMeta();
      Object[][] effects = {{PotionEffectType.SATURATION, 140}, {PotionEffectType.FIRE_RESISTANCE, 100}, {PotionEffectType.REGENERATION, 160}, {PotionEffectType.NIGHT_VISION, 200},
         {PotionEffectType.JUMP_BOOST, 160}, {PotionEffectType.SPEED, 200}, {PotionEffectType.BLINDNESS, 160}, {PotionEffectType.WEAKNESS, 180}, {PotionEffectType.POISON, 100}};
      Object[] e = effects[ThreadLocalRandom.current().nextInt(effects.length)];
      meta.addCustomEffect(new PotionEffect((PotionEffectType)e[0], (Integer)e[1], 0), true);
      item.setItemMeta(meta);
      return item;
   }

   private ItemStack barPrize() {
      double r = ThreadLocalRandom.current().nextDouble();
      double t = 0.000001;
      if (r < t) {
         return new ItemStack(Material.ENCHANTED_GOLDEN_APPLE);
      }

      t += 0.01;
      if (r < t) {
         return new ItemStack(Material.GOLDEN_APPLE);
      }

      t += 0.12;
      if (r < t) {
         return new ItemStack(Material.COOKED_BEEF, 8);
      }

      t += 0.12;
      if (r < t) {
         return new ItemStack(Material.BAKED_POTATO, 16);
      }

      t += 0.50;
      return r < t ? this.pinte() : this.stew();
   }

   private void order(Player player, Guild owner, int count, Inventory inv, Bar h) {
      int total = this.barPrice() * count;
      if (this.plugin.getCombatManager().denySpending(player)) {
         return;
      }

      PlayerData data = this.plugin.getDataManager().getPlayerData(player.getUniqueId());
      if (data == null || !data.removeCoins(total)) {
         ChatUtil.sendMessage(player, "&c❌ Il vous faut &6" + total + " pièces&c.");
         return;
      }

      this.plugin.getDataManager().savePlayer(data);
      owner.depositBank(total);
      this.plugin.getDataManager().saveGuild(owner);
      h.busy = true;
      UUID id = player.getUniqueId();
      this.playing.add(id);
      java.util.List<ItemStack> prizes = new java.util.ArrayList<>();
      for (int i = 0; i < count; i++) {
         prizes.add(this.barPrize());
      }

      Material[] spin = {Material.COOKED_BEEF, Material.BAKED_POTATO, Material.POTION, Material.SUSPICIOUS_STEW, Material.GOLDEN_APPLE, Material.BREAD};
      int[] frame = {0};
      Bukkit.getScheduler().runTaskTimer(this.plugin, task -> {
         frame[0]++;
         if (frame[0] <= 8) {
            inv.setItem(22, new ItemBuilder(spin[ThreadLocalRandom.current().nextInt(spin.length)]).name("&7Le barman sert…").build());
            this.tick(Bukkit.getPlayer(id), Sound.BLOCK_NOTE_BLOCK_HAT, 1.0F);
            return;
         }

         task.cancel();
         h.busy = false;
         this.playing.remove(id);
         Player p = Bukkit.getPlayer(id);
         StringBuilder summary = new StringBuilder();
         for (ItemStack prize : prizes) {
            String label = prize.getAmount() + "× " + (prize.getItemMeta() != null && prize.getItemMeta().hasDisplayName() ? ChatUtil.toLegacy(prize.getItemMeta().displayName()) : prize.getType().name().toLowerCase(java.util.Locale.ROOT).replace('_', ' '));
            summary.append(summary.length() > 0 ? "&7, &f" : "&f").append(label);
            if (p != null) {
               for (ItemStack left : p.getInventory().addItem(prize).values()) {
                  p.getWorld().dropItemNaturally(p.getLocation(), left);
               }
            } else {
               // Joueur parti : le butin est rendu sous forme de pièces (prix payé) pour ne rien perdre.
               this.plugin.getDataManager().creditPlayer(id, this.barPrice());
            }

            if (prize.getType() == Material.GOLDEN_APPLE || prize.getType() == Material.ENCHANTED_GOLDEN_APPLE) {
               ChatUtil.broadcast("&6🍻 &e" + (p != null ? p.getName() : "Un client") + " &6décroche une &e" + (prize.getType() == Material.ENCHANTED_GOLDEN_APPLE ? "POMME DORÉE ENCHANTÉE" : "pomme dorée") + " &6au bar de " + owner.getName() + " !");
            }
         }

         if (!inv.getViewers().isEmpty()) {
            inv.setItem(22, prizes.get(prizes.size() - 1).clone());
         }

         this.note(id, "&6🍻 Votre commande : " + summary, true);
      }, 3L, 3L);
   }

   // ------------------------------------------------------------------ roulette

   private void openRoulette(Player player, Guild owner) {
      Roulette h = new Roulette(owner.getId());
      Inventory inv = Bukkit.createInventory(h, 54, ChatUtil.component("&8&l🎡 Roulette — " + owner.getName()));
      this.renderRoulette(inv, h, owner);
      player.openInventory(inv);
   }

   private void renderRoulette(Inventory inv, Roulette h, Guild owner) {
      this.fill(inv);
      inv.setItem(4, new ItemBuilder(Material.BARREL).name("&6&lRoulette européenne").lore(this.houseLine(owner), "&7Choisissez votre mise, puis un pari :",
         "&7Rouge/Noir, Pair/Impair, Manque/Passe : ×2", "&7Douzaines : ×3 · Numéro plein : ×36", "&7Le 0 fait gagner la maison (sauf pari plein).").build());
      this.chips(inv, h);
      inv.setItem(16, new ItemBuilder(Material.GREEN_WOOL).name("&7La bille attend…").build());
      inv.setItem(19, new ItemBuilder(Material.RED_WOOL).name("&c&lROUGE").lore("&7Gain ×2").build());
      inv.setItem(20, new ItemBuilder(Material.BLACK_WOOL).name("&8&lNOIR").lore("&7Gain ×2").build());
      inv.setItem(21, new ItemBuilder(Material.LIGHT_BLUE_WOOL).name("&b&lPAIR").lore("&7Gain ×2").build());
      inv.setItem(23, new ItemBuilder(Material.ORANGE_WOOL).name("&6&lIMPAIR").lore("&7Gain ×2").build());
      inv.setItem(24, new ItemBuilder(Material.WHITE_WOOL).name("&f&lMANQUE &7(1-18)").lore("&7Gain ×2").build());
      inv.setItem(25, new ItemBuilder(Material.YELLOW_WOOL).name("&e&lPASSE &7(19-36)").lore("&7Gain ×2").build());
      inv.setItem(28, new ItemBuilder(Material.PURPLE_WOOL).name("&5&l1re douzaine &7(1-12)").lore("&7Gain ×3").build());
      inv.setItem(31, new ItemBuilder(Material.MAGENTA_WOOL).name("&d&l2e douzaine &7(13-24)").lore("&7Gain ×3").build());
      inv.setItem(34, new ItemBuilder(Material.PINK_WOOL).name("&d&l3e douzaine &7(25-36)").lore("&7Gain ×3").build());
      inv.setItem(37, new ItemBuilder(Material.RED_STAINED_GLASS_PANE).name("&c-10").build());
      inv.setItem(38, new ItemBuilder(Material.ORANGE_STAINED_GLASS_PANE).name("&6-1").build());
      boolean red = RED.contains(h.number);
      inv.setItem(40, new ItemBuilder(h.number == 0 ? Material.GREEN_CONCRETE : red ? Material.RED_CONCRETE : Material.BLACK_CONCRETE).name("&e&lNuméro plein : " + h.number)
         .lore("&7Cliquez pour miser sur le " + h.number, "&7Gain ×36").build());
      inv.setItem(42, new ItemBuilder(Material.LIME_STAINED_GLASS_PANE).name("&a+1").build());
      inv.setItem(43, new ItemBuilder(Material.GREEN_STAINED_GLASS_PANE).name("&2+10").build());
      inv.setItem(45, this.back());
   }

   private static double rouletteMult(String bet, int n) {
      return switch (bet) {
         case "red" -> n != 0 && RED.contains(n) ? 2.0 : 0.0;
         case "black" -> n != 0 && !RED.contains(n) ? 2.0 : 0.0;
         case "even" -> n != 0 && n % 2 == 0 ? 2.0 : 0.0;
         case "odd" -> n % 2 == 1 ? 2.0 : 0.0;
         case "low" -> n >= 1 && n <= 18 ? 2.0 : 0.0;
         case "high" -> n >= 19 ? 2.0 : 0.0;
         case "d1" -> n >= 1 && n <= 12 ? 3.0 : 0.0;
         case "d2" -> n >= 13 && n <= 24 ? 3.0 : 0.0;
         case "d3" -> n >= 25 ? 3.0 : 0.0;
         default -> bet.startsWith("n") && Integer.parseInt(bet.substring(1)) == n ? 36.0 : 0.0;
      };
   }

   private void spinRoulette(Player player, Roulette h, Guild owner, String bet, double maxMult, Inventory inv) {
      int stake = BETS[h.bet];
      if (!this.take(player, owner, stake, maxMult)) {
         return;
      }

      h.busy = true;
      UUID id = player.getUniqueId();
      this.playing.add(id);
      int result = ThreadLocalRandom.current().nextInt(37);
      int[] frame = {0};
      Bukkit.getScheduler().runTaskTimer(this.plugin, task -> {
         frame[0]++;
         if (frame[0] <= 14) {
            int n = ThreadLocalRandom.current().nextInt(37);
            inv.setItem(16, new ItemBuilder(n == 0 ? Material.GREEN_WOOL : RED.contains(n) ? Material.RED_WOOL : Material.BLACK_WOOL).name("&7" + n).build());
            this.tick(Bukkit.getPlayer(id), Sound.BLOCK_NOTE_BLOCK_BASEDRUM, 0.6F + frame[0] * 0.05F);
            return;
         }

         task.cancel();
         h.busy = false;
         this.playing.remove(id);
         double mult = rouletteMult(bet, result);
         double payout = this.settle(owner, id, stake, mult);
         String colour = result == 0 ? "&a(vert)" : RED.contains(result) ? "&c(rouge)" : "&8(noir)";
         this.note(id, mult > 0 ? "&a🎡 La bille s'arrête sur le &e" + result + " " + colour + " &a! Vous gagnez &6" + (long)(payout - stake) + " pièces&a." : "&c🎡 Le &e" + result + " " + colour + " &c: la maison empoche vos &6" + stake + " pièces&c.", mult > 0);
         if (!inv.getViewers().isEmpty()) {
            this.renderRoulette(inv, h, owner);
            inv.setItem(16, new ItemBuilder(result == 0 ? Material.GREEN_WOOL : RED.contains(result) ? Material.RED_WOOL : Material.BLACK_WOOL).name("&e&l" + result).build());
         }
      }, 3L, 3L);
   }

   // ------------------------------------------------------------------ machine à sous

   private static final Material[] SYMBOLS = {Material.SWEET_BERRIES, Material.YELLOW_DYE, Material.BELL, Material.DIAMOND, Material.GOLD_BLOCK};
   private static final String[] SYMBOL_NAMES = {"&cCerise", "&eCitron", "&6Cloche", "&bDiamant", "&c&lSEPT"};
   private static final int[] WEIGHTS = {30, 25, 20, 15, 10};
   private static final int[] REELS = {11, 13, 15};

   private void openSlots(Player player, Guild owner) {
      Slots h = new Slots(owner.getId());
      Inventory inv = Bukkit.createInventory(h, 27, ChatUtil.component("&8&l🎰 Machine à sous — " + owner.getName()));
      this.renderSlots(inv, h, owner);
      player.openInventory(inv);
   }

   private ItemStack symbol(int i) {
      return new ItemBuilder(SYMBOLS[i]).name(SYMBOL_NAMES[i]).build();
   }

   private void renderSlots(Inventory inv, Slots h, Guild owner) {
      this.fill(inv);
      inv.setItem(4, new ItemBuilder(Material.BARREL).name("&d&lMachine à sous").lore(this.houseLine(owner), "", "&7Trois symboles identiques :", "&c7 7 7 &7×50 · &bdiamants &7×20 · &6cloches &7×10",
         "&ecitrons &7×6 · &ccerises &7×4", "&7Deux cerises : &a×1,5 &7· une cerise : &e×0,5").build());
      this.chips(inv, h);
      for (int slot : REELS) {
         inv.setItem(slot, new ItemBuilder(Material.GRAY_STAINED_GLASS).name("&7?").build());
      }

      inv.setItem(22, new ItemBuilder(Material.LIME_CONCRETE).name("&a&lTIRER LE LEVIER").lore("&7Mise : &6" + BETS[h.bet] + " pièces").build());
      inv.setItem(18, this.back());
   }

   private static int drawSymbol() {
      int r = ThreadLocalRandom.current().nextInt(100);
      int acc = 0;
      for (int i = 0; i < WEIGHTS.length; i++) {
         acc += WEIGHTS[i];
         if (r < acc) {
            return i;
         }
      }

      return 0;
   }

   private static double slotsMult(int[] s) {
      if (s[0] == s[1] && s[1] == s[2]) {
         return switch (s[0]) {
            case 4 -> 50.0;
            case 3 -> 20.0;
            case 2 -> 10.0;
            case 1 -> 6.0;
            default -> 4.0;
         };
      }

      int cherries = 0;
      for (int x : s) {
         if (x == 0) {
            cherries++;
         }
      }

      return cherries == 2 ? 1.5 : cherries == 1 ? 0.5 : 0.0;
   }

   private void spinSlots(Player player, Slots h, Guild owner, Inventory inv) {
      int stake = BETS[h.bet];
      if (!this.take(player, owner, stake, 50.0)) {
         return;
      }

      h.busy = true;
      UUID id = player.getUniqueId();
      this.playing.add(id);
      int[] result = {drawSymbol(), drawSymbol(), drawSymbol()};
      int[] frame = {0};
      Bukkit.getScheduler().runTaskTimer(this.plugin, task -> {
         frame[0]++;
         if (frame[0] <= 15) {
            for (int i = 0; i < 3; i++) {
               inv.setItem(REELS[i], this.symbol(frame[0] >= 8 + i * 3 ? result[i] : ThreadLocalRandom.current().nextInt(SYMBOLS.length)));
            }

            this.tick(Bukkit.getPlayer(id), Sound.BLOCK_NOTE_BLOCK_PLING, 0.5F + (frame[0] % 5) * 0.2F);
            return;
         }

         task.cancel();
         h.busy = false;
         this.playing.remove(id);
         double mult = slotsMult(result);
         double payout = this.settle(owner, id, stake, mult);
         boolean win = payout > stake;
         String line = SYMBOL_NAMES[result[0]] + " &7| " + SYMBOL_NAMES[result[1]] + " &7| " + SYMBOL_NAMES[result[2]];
         this.note(id, "&d🎰 " + line + " &d— " + (win ? "&aGagné ! +" + (long)(payout - stake) + " pièces" : payout == stake ? "&eMise rendue" : payout > 0 ? "&eVous récupérez " + (long)payout + " pièces" : "&cPerdu"), win);
         if (!inv.getViewers().isEmpty()) {
            for (int i = 0; i < 3; i++) {
               inv.setItem(REELS[i], this.symbol(result[i]));
            }

            inv.setItem(22, new ItemBuilder(Material.LIME_CONCRETE).name("&a&lREJOUER").lore("&7Mise : &6" + BETS[h.bet] + " pièces").build());
         }
      }, 3L, 3L);
   }

   // ------------------------------------------------------------------ clics

   @EventHandler(priority = EventPriority.HIGHEST)
   public void onClick(InventoryClickEvent event) {
      if (!(event.getWhoClicked() instanceof Player player) || !(event.getInventory().getHolder() instanceof GameHolder holder)) {
         return;
      }

      event.setCancelled(true);
      if (event.getClickedInventory() != event.getView().getTopInventory() || holder.busy) {
         return;
      }

      Guild owner = this.plugin.getGuildManager().getGuildById(holder.guildId);
      if (owner == null || !this.plugin.getTavern().isTavern(player.getLocation())) {
         player.closeInventory();
         return;
      }

      int slot = event.getRawSlot();
      Inventory inv = event.getInventory();
      if (holder instanceof Menu) {
         switch (slot) {
            case 10 -> this.openFlip(player, owner);
            case 12 -> this.openBar(player, owner);
            case 14 -> this.openRoulette(player, owner);
            case 16 -> this.openSlots(player, owner);
            default -> {
            }
         }

         return;
      }

      if (slot == 18 && !(holder instanceof Roulette) || slot == 45 && holder instanceof Roulette) {
         this.openMenu(player, owner);
         return;
      }

      if (this.playing.contains(player.getUniqueId())) {
         return;
      }

      for (int i = 0; i < CHIP_SLOTS.length; i++) {
         if (slot == CHIP_SLOTS[i] && !(holder instanceof Bar)) {
            holder.bet = i;
            if (holder instanceof Flip f) {
               this.renderFlip(inv, f, owner);
            } else if (holder instanceof Roulette r) {
               this.renderRoulette(inv, r, owner);
            } else if (holder instanceof Slots s) {
               this.renderSlots(inv, s, owner);
            }

            ChatUtil.playSound(player, Sound.UI_BUTTON_CLICK, 1.0F, 1.2F);
            return;
         }
      }

      if (holder instanceof Flip f && (slot == 20 || slot == 24)) {
         this.flip(player, f, owner, slot == 20, inv);
      } else if (holder instanceof Bar b) {
         int count = slot == 11 ? 1 : slot == 13 ? 5 : slot == 15 ? 10 : 0;
         if (count > 0) {
            this.order(player, owner, count, inv, b);
         }
      } else if (holder instanceof Slots s && slot == 22) {
         this.spinSlots(player, s, owner, inv);
      } else if (holder instanceof Roulette r) {
         String bet = switch (slot) {
            case 19 -> "red";
            case 20 -> "black";
            case 21 -> "even";
            case 23 -> "odd";
            case 24 -> "low";
            case 25 -> "high";
            case 28 -> "d1";
            case 31 -> "d2";
            case 34 -> "d3";
            case 40 -> "n" + r.number;
            default -> null;
         };
         if (bet != null) {
            this.spinRoulette(player, r, owner, bet, bet.startsWith("n") ? 36.0 : bet.startsWith("d") ? 3.0 : 2.0, inv);
         } else if (slot == 37 || slot == 38 || slot == 42 || slot == 43) {
            int delta = slot == 37 ? -10 : slot == 38 ? -1 : slot == 42 ? 1 : 10;
            r.number = Math.floorMod(r.number + delta, 37);
            this.renderRoulette(inv, r, owner);
            ChatUtil.playSound(player, Sound.UI_BUTTON_CLICK, 1.0F, 1.0F);
         }
      }
   }
}
