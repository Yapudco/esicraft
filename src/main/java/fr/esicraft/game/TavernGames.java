package fr.esicraft.game;

import fr.esicraft.EsicraftPlugin;
import fr.esicraft.data.PlayerData;
import fr.esicraft.gui.holder.BaseGUIHolder;
import fr.esicraft.guild.Guild;
import fr.esicraft.util.ChatUtil;
import fr.esicraft.util.ItemBuilder;
import java.util.ArrayList;
import java.util.List;
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
 * roulette européenne, machine à sous, dés, bras de fer et course de cochons. La maison ne paie jamais plus que ce que sa banque peut couvrir : la mise maximale s'adapte.
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

   static final class Dice extends GameHolder {
      Dice(String g) {
         super(g);
      }
   }

   /** Bras de fer contre le champion de la taverne : cliquer à temps sur le poing qui apparaît. */
   static final class Arm extends GameHolder {
      int pos = 4;
      int round;
      int target = -1;
      int deadline;
      int wait;
      int stake;
      boolean running;
      /** Le poing affiché est une feinte du champion : il ne faut PAS cliquer dessus. */
      boolean trap;

      Arm(String g) {
         super(g);
      }
   }

   /** Jeu du « greed » : un multiplicateur grimpe, il faut encaisser avant qu'il n'explose. */
   static final class Greed extends GameHolder {
      boolean running;
      double mult = 1.0;
      double crash;
      double cap;
      int stake;

      Greed(String g) {
         super(g);
      }
   }

   static final class Pigs extends GameHolder {
      Pigs(String g) {
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

      // Les membres de la guilde propriétaire peuvent jouer contre leur propre maison (choix de conception : la taverne est aussi un lieu de vie
      // pour la guilde). L'avantage de la maison et la mise maximale limitent ce que la banque peut perdre.
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
      inv.setItem(26, new ItemBuilder(Material.TNT).name("&c&lGreed").lore("&7Un multiplicateur grimpe : &a×1,00, ×1,50, ×3…", "&7Encaissez avant qu'il n'explose,", "&7sinon la mise est perdue !",
         "&7Jusqu'à &a×" + this.greedCap()).build());
      inv.setItem(20, new ItemBuilder(Material.WHITE_CONCRETE).name("&f&lDés").lore("&7Deux dés contre deux dés de la maison.", "&7Plus haut : mise doublée · égalité : moitié rendue.").build());
      inv.setItem(22, new ItemBuilder(Material.IRON_INGOT).name("&6&lBras de fer").lore("&7Affrontez le champion de la taverne :", "&7cliquez sur le poing dès qu'il apparaît !", "&7Victoire : &a×" + ARM_MULT + " &7(" + this.armLimit() + " parties par jour).").build());
      inv.setItem(24, new ItemBuilder(Material.PORKCHOP).name("&d&lCourse de cochons").lore("&7Quatre cochons, un seul vainqueur.", "&7Pariez sur le bon : &a×" + PIG_MULT + " &7la mise.").build());
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
               // Joueur parti : le verre est remboursé PAR LA MAISON (qui a encaissé le prix). Avant, le remboursement était créé de rien
               // pendant que la banque gardait la commande : commander puis se déconnecter faisait apparaître de l'argent à chaque fois.
               double refund = Math.min(this.barPrice(), owner.getBankBalance());
               if (refund > 0.0 && owner.withdrawBank(refund)) {
                  this.plugin.getDataManager().creditPlayer(id, refund);
                  this.plugin.getDataManager().saveGuild(owner);
               }
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

   // ------------------------------------------------------------------ dés

   private void openDice(Player player, Guild owner) {
      Dice h = new Dice(owner.getId());
      Inventory inv = Bukkit.createInventory(h, 27, ChatUtil.component("&8&l🎲 Dés — " + owner.getName()));
      this.renderDice(inv, h, owner);
      player.openInventory(inv);
   }

   private ItemStack die(int value, boolean house) {
      return new ItemBuilder(house ? Material.RED_CONCRETE : Material.WHITE_CONCRETE).amount(Math.max(1, value)).name((house ? "&c" : "&f") + (value > 0 ? "Dé : " + value : "?")).build();
   }

   private void renderDice(Inventory inv, Dice h, Guild owner) {
      this.fill(inv);
      inv.setItem(4, new ItemBuilder(Material.BARREL).name("&f&lDés").lore(this.houseLine(owner), "&7Vous lancez deux dés, la maison aussi.", "&7Total plus haut : &amise doublée",
         "&7Égalité : &emoitié de la mise rendue", "&7Plus bas : &cperdu").build());
      this.chips(inv, h);
      inv.setItem(19, new ItemBuilder(Material.PLAYER_HEAD).name("&fVos dés").build());
      inv.setItem(20, this.die(0, false));
      inv.setItem(21, this.die(0, false));
      inv.setItem(22, new ItemBuilder(Material.LIME_CONCRETE).name("&a&lLANCER").lore("&7Mise : &6" + BETS[h.bet] + " pièces").build());
      inv.setItem(23, this.die(0, true));
      inv.setItem(24, this.die(0, true));
      inv.setItem(25, new ItemBuilder(Material.BARREL).name("&cLa maison").build());
      inv.setItem(18, this.back());
   }

   private void rollDice(Player player, Dice h, Guild owner, Inventory inv) {
      int stake = BETS[h.bet];
      if (!this.take(player, owner, stake, 2.0)) {
         return;
      }

      h.busy = true;
      UUID id = player.getUniqueId();
      this.playing.add(id);
      ThreadLocalRandom r = ThreadLocalRandom.current();
      int[] mine = {r.nextInt(1, 7), r.nextInt(1, 7)};
      int[] house = {r.nextInt(1, 7), r.nextInt(1, 7)};
      int[] frame = {0};
      Bukkit.getScheduler().runTaskTimer(this.plugin, task -> {
         ThreadLocalRandom rr = ThreadLocalRandom.current();
         frame[0]++;
         if (frame[0] <= 10) {
            boolean stopMine = frame[0] > 6;
            inv.setItem(20, this.die(stopMine ? mine[0] : rr.nextInt(1, 7), false));
            inv.setItem(21, this.die(stopMine ? mine[1] : rr.nextInt(1, 7), false));
            inv.setItem(23, this.die(rr.nextInt(1, 7), true));
            inv.setItem(24, this.die(rr.nextInt(1, 7), true));
            this.tick(Bukkit.getPlayer(id), Sound.BLOCK_WOOD_HIT, 0.8F + frame[0] * 0.05F);
            return;
         }

         task.cancel();
         h.busy = false;
         this.playing.remove(id);
         int a = mine[0] + mine[1];
         int b = house[0] + house[1];
         double mult = a > b ? 2.0 : a == b ? 0.5 : 0.0;
         double payout = this.settle(owner, id, stake, mult);
         this.note(id, a > b ? "&a🎲 " + a + " contre " + b + " : vous gagnez &6" + (long)(payout - stake) + " pièces&a !"
            : a == b ? "&e🎲 Égalité (" + a + ") : la moitié de votre mise vous est rendue." : "&c🎲 " + a + " contre " + b + " : la maison empoche vos &6" + stake + " pièces&c.", a > b);
         if (!inv.getViewers().isEmpty()) {
            inv.setItem(20, this.die(mine[0], false));
            inv.setItem(21, this.die(mine[1], false));
            inv.setItem(23, this.die(house[0], true));
            inv.setItem(24, this.die(house[1], true));
         }
      }, 3L, 3L);
   }

   // ------------------------------------------------------------------ bras de fer

   private static final double ARM_MULT = 1.8;
   private static final int[] ARM_GRID = {27, 28, 29, 30, 31, 32, 33, 34, 35, 36, 37, 38, 39, 40, 41, 42, 43, 44};
   private final java.util.Map<UUID, Integer> armToday = new ConcurrentHashMap<>();
   private String armDay = "";

   /** Temps (en ticks) pour cliquer sur le poing au premier tour ; il raccourcit d'un tick par tour jusqu'au minimum. */
   private int armStartWindow() {
      return Math.max(10, this.plugin.getConfig().getInt("taverns.arm-wrestling.start-window-ticks", 26));
   }

   /** Chance de base que le champion pousse avant chaque poing (+4 points par tour, plafonnée). */
   private double armPushBase() {
      return Math.max(0.0, Math.min(1.0, this.plugin.getConfig().getDouble("taverns.arm-wrestling.champion-push", 0.40)));
   }

   private double armPushMax() {
      return Math.max(this.armPushBase(), Math.min(1.0, this.plugin.getConfig().getDouble("taverns.arm-wrestling.champion-push-max", 0.75)));
   }

   /** Part des poings qui sont des feintes (à ne pas toucher), à partir du 2e tour. */
   private double armTrapChance() {
      return Math.max(0.0, Math.min(0.8, this.plugin.getConfig().getDouble("taverns.arm-wrestling.feint-chance", 0.25)));
   }

   private int armMinWindow() {
      return Math.max(6, Math.min(this.armStartWindow(), this.plugin.getConfig().getInt("taverns.arm-wrestling.min-window-ticks", 12)));
   }

   private int armLimit() {
      return Math.max(1, this.plugin.getConfig().getInt("taverns.arm-wrestling.matches-per-day", 6));
   }

   private void openArm(Player player, Guild owner) {
      Arm h = new Arm(owner.getId());
      Inventory inv = Bukkit.createInventory(h, 54, ChatUtil.component("&8&l💪 Bras de fer — " + owner.getName()));
      this.renderArm(inv, h, owner);
      player.openInventory(inv);
   }

   private void renderArm(Inventory inv, Arm h, Guild owner) {
      this.fill(inv);
      inv.setItem(4, new ItemBuilder(Material.BARREL).name("&6&lBras de fer").lore(this.houseLine(owner), "&7Cliquez sur le &e✊ poing &7dès qu'il apparaît", "&7dans la grille : chaque clic réussi fait",
         "&7plier le champion. Un clic raté ou trop lent", "&7vous fait reculer, et il pousse de plus en plus fort.", "&7Méfiez-vous des &4✋ feintes &7: n'y touchez pas !","&7Victoire : &a×" + ARM_MULT + " &7la mise").build());
      this.chips(inv, h);
      inv.setItem(16, new ItemBuilder(Material.IRON_INGOT).name("&a&lAFFRONTER LE CHAMPION").lore("&7Mise : &6" + BETS[h.bet] + " pièces").build());
      this.renderRope(inv, h);
      inv.setItem(45, this.back());
   }

   /** La corde : à gauche le champion, à droite vous. Le marqueur avance vers la droite quand vous gagnez du terrain. */
   private void renderRope(Inventory inv, Arm h) {
      for (int i = 0; i <= 8; i++) {
         Material m = i == h.pos ? Material.IRON_BLOCK : i < h.pos ? Material.LIME_STAINED_GLASS_PANE : Material.RED_STAINED_GLASS_PANE;
         inv.setItem(18 + i, new ItemBuilder(m).name(i == 0 ? "&cLe champion" : i == 8 ? "&aVous" : i == h.pos ? "&f&lLes bras" : " ").build());
      }
   }

   private void startArm(Player player, Arm h, Guild owner, Inventory inv) {
      String today = java.time.LocalDate.now().toString();
      if (!today.equals(this.armDay)) {
         this.armDay = today;
         this.armToday.clear();
      }

      int played = this.armToday.getOrDefault(player.getUniqueId(), 0);
      if (played >= this.armLimit()) {
         ChatUtil.sendMessage(player, "&c💪 Le champion refuse : vous avez déjà joué " + this.armLimit() + " parties aujourd'hui. Revenez demain !");
         return;
      }

      int stake = BETS[h.bet];
      if (!this.take(player, owner, stake, ARM_MULT)) {
         return;
      }

      this.armToday.put(player.getUniqueId(), played + 1);
      h.stake = stake;
      h.pos = 4;
      h.round = 0;
      h.target = -1;
      h.wait = 40;
      h.running = true;
      UUID id = player.getUniqueId();
      this.playing.add(id);
      for (int s : ARM_GRID) {
         inv.setItem(s, new ItemBuilder(Material.BLACK_STAINED_GLASS_PANE).name(" ").build());
      }

      inv.setItem(16, new ItemBuilder(Material.CLOCK).name("&e&lPrêt…").build());
      this.renderRope(inv, h);
      ChatUtil.playSound(player, Sound.ENTITY_IRON_GOLEM_ATTACK, 1.0F, 1.0F);
      Bukkit.getScheduler().runTaskTimer(this.plugin, task -> {
         Player p = Bukkit.getPlayer(id);
         if (!h.running) {
            task.cancel();
            return;
         }

         if (p == null || inv.getViewers().isEmpty()) {
            task.cancel();
            this.endArm(id, h, owner, inv, false, "&c💪 Vous avez lâché le bras du champion : la mise est perdue.");
            return;
         }

         if (h.target < 0) {
            if (--h.wait > 0) {
               return;
            }

            h.round++;
            // Le champion pousse de lui-même de temps en temps, de plus en plus souvent.
            if (ThreadLocalRandom.current().nextDouble() < Math.min(this.armPushMax(), this.armPushBase() + h.round * 0.04)) {
               h.pos--;
               this.renderRope(inv, h);
               this.tick(p, Sound.ENTITY_IRON_GOLEM_HURT, 0.8F);
               if (h.pos <= 0) {
                  task.cancel();
                  this.endArm(id, h, owner, inv, false, "&c💪 Le champion vous plaque le bras sur la table ! La maison garde vos &6" + h.stake + " pièces&c.");
                  return;
               }
            }

            h.target = ARM_GRID[ThreadLocalRandom.current().nextInt(ARM_GRID.length)];
            h.deadline = Math.max(this.armMinWindow(), this.armStartWindow() - h.round);
            h.trap = h.round > 1 && ThreadLocalRandom.current().nextDouble() < this.armTrapChance();
            inv.setItem(h.target, h.trap ? new ItemBuilder(Material.REDSTONE_BLOCK).name("&4&l✋ FEINTE ! &cNe touchez pas").build()
               : new ItemBuilder(Material.IRON_INGOT).name("&e&l✊ POUSSEZ !").build());
            return;
         }

         if (--h.deadline <= 0) {
            inv.setItem(h.target, new ItemBuilder(Material.BLACK_STAINED_GLASS_PANE).name(" ").build());
            h.target = -1;
            h.wait = ThreadLocalRandom.current().nextInt(15, 31);
            if (h.trap) {
               // Feinte ignorée : c'est bien joué, rien ne bouge.
               h.trap = false;
               return;
            }

            h.pos--;
            this.renderRope(inv, h);
            this.tick(p, Sound.BLOCK_NOTE_BLOCK_BASS, 0.6F);
            if (h.pos <= 0) {
               task.cancel();
               this.endArm(id, h, owner, inv, false, "&c💪 Trop lent ! Le champion l'emporte et la maison garde vos &6" + h.stake + " pièces&c.");
            }
         }
      }, 1L, 1L);
   }

   private void armClick(Player player, Arm h, int slot, Inventory inv) {
      if (h.target < 0 || slot < 27 || slot > 44) {
         return;
      }

      boolean hit = slot == h.target && !h.trap;
      if (slot == h.target && h.trap) {
         // Tomber dans la feinte coûte deux crans.
         h.pos--;
         ChatUtil.sendActionbar(player, "&c✋ C'était une feinte !");
      }

      h.trap = false;
      inv.setItem(h.target, new ItemBuilder(Material.BLACK_STAINED_GLASS_PANE).name(" ").build());
      h.target = -1;
      h.wait = ThreadLocalRandom.current().nextInt(15, 31);
      h.pos += hit ? 1 : -1;
      this.renderRope(inv, h);
      this.tick(player, hit ? Sound.ENTITY_PLAYER_ATTACK_STRONG : Sound.BLOCK_NOTE_BLOCK_BASS, hit ? 1.2F : 0.6F);
      Guild owner = this.plugin.getGuildManager().getGuildById(h.guildId);
      if (owner == null) {
         h.running = false;
         this.playing.remove(player.getUniqueId());
         return;
      }

      if (h.pos >= 8) {
         this.endArm(player.getUniqueId(), h, owner, inv, true, null);
      } else if (h.pos <= 0) {
         this.endArm(player.getUniqueId(), h, owner, inv, false, "&c💪 Raté ! Le champion vous plaque le bras sur la table. La maison garde vos &6" + h.stake + " pièces&c.");
      }
   }

   private void endArm(UUID id, Arm h, Guild owner, Inventory inv, boolean win, String loseMessage) {
      if (!h.running) {
         return;
      }

      h.running = false;
      this.playing.remove(id);
      double payout = this.settle(owner, id, h.stake, win ? ARM_MULT : 0.0);
      if (win) {
         this.note(id, "&a💪 Vous faites plier le champion de " + owner.getName() + " ! &6+" + (long)(payout - h.stake) + " pièces&a.", true);
         Player p = Bukkit.getPlayer(id);
         if (p != null && h.stake >= 500) {
            ChatUtil.broadcast("&6💪 &e" + p.getName() + " &6a battu le champion de bras de fer de la taverne de " + owner.getName() + " !");
         }
      } else {
         this.note(id, loseMessage, false);
      }

      if (!inv.getViewers().isEmpty()) {
         for (int s : ARM_GRID) {
            inv.setItem(s, this.glass());
         }

         inv.setItem(16, new ItemBuilder(Material.IRON_INGOT).name("&a&lREVANCHE").lore("&7Mise : &6" + BETS[h.bet] + " pièces").build());
      }
   }

   // ------------------------------------------------------------------ course de cochons

   private static final double PIG_MULT = 3.6;
   private static final String[] PIG_NAMES = {"&dRosette", "&6Jambonneau", "&8Truffe", "&cSaucisse"};
   private static final Material[] PIG_LANES = {Material.PINK_STAINED_GLASS_PANE, Material.ORANGE_STAINED_GLASS_PANE, Material.BROWN_STAINED_GLASS_PANE, Material.RED_STAINED_GLASS_PANE};

   private void openPigs(Player player, Guild owner) {
      Pigs h = new Pigs(owner.getId());
      Inventory inv = Bukkit.createInventory(h, 54, ChatUtil.component("&8&l🐖 Course de cochons — " + owner.getName()));
      this.renderPigs(inv, h, owner);
      player.openInventory(inv);
   }

   private void renderPigs(Inventory inv, Pigs h, Guild owner) {
      this.fill(inv);
      inv.setItem(4, new ItemBuilder(Material.BARREL).name("&d&lCourse de cochons").lore(this.houseLine(owner), "&7Choisissez votre mise, puis cliquez sur", "&7le cochon qui va gagner (colonne de gauche).",
         "&7Gain : &a×" + PIG_MULT + " &7la mise").build());
      this.chips(inv, h);
      inv.setItem(0, this.back());
      this.renderTrack(inv, new int[]{0, 0, 0, 0});
      for (int lane = 0; lane < 4; lane++) {
         inv.setItem(18 + lane * 9, new ItemBuilder(Material.PORKCHOP).name("&e&lParier sur " + PIG_NAMES[lane]).lore("&7Mise : &6" + BETS[h.bet] + " pièces", "&7Gain : &a×" + PIG_MULT).build());
      }
   }

   private void renderTrack(Inventory inv, int[] pos) {
      for (int lane = 0; lane < 4; lane++) {
         for (int col = 1; col <= 8; col++) {
            int slot = 18 + lane * 9 + col;
            boolean pig = Math.min(7, pos[lane]) == col - 1;
            inv.setItem(slot, pig ? new ItemBuilder(Material.PIG_SPAWN_EGG).name(PIG_NAMES[lane]).build()
               : new ItemBuilder(col == 8 ? Material.WHITE_STAINED_GLASS_PANE : PIG_LANES[lane]).name(col == 8 ? "&f🏁 Arrivée" : " ").build());
         }
      }
   }

   private void racePigs(Player player, Pigs h, Guild owner, int choice, Inventory inv) {
      if (choice < 0 || choice > 3) {
         return;
      }

      int stake = BETS[h.bet];
      if (!this.take(player, owner, stake, PIG_MULT)) {
         return;
      }

      h.busy = true;
      UUID id = player.getUniqueId();
      this.playing.add(id);
      int[] pos = {0, 0, 0, 0};
      this.renderTrack(inv, pos);
      ChatUtil.sendMessage(player, "&d🐖 Vous pariez &6" + stake + " pièces &dsur " + PIG_NAMES[choice] + "&d. Partez !");
      Bukkit.getScheduler().runTaskTimer(this.plugin, task -> {
         ThreadLocalRandom r = ThreadLocalRandom.current();
         List<Integer> finished = new ArrayList<>();
         for (int lane = 0; lane < 4; lane++) {
            double roll = r.nextDouble();
            pos[lane] += roll < 0.08 ? 2 : roll < 0.55 ? 1 : 0;
            if (pos[lane] >= 7) {
               finished.add(lane);
            }
         }

         if (!inv.getViewers().isEmpty()) {
            this.renderTrack(inv, pos);
         }

         this.tick(Bukkit.getPlayer(id), Sound.ENTITY_PIG_AMBIENT, 0.8F + r.nextFloat() * 0.6F);
         if (finished.isEmpty()) {
            return;
         }

         task.cancel();
         h.busy = false;
         this.playing.remove(id);
         int winner = finished.get(r.nextInt(finished.size()));
         boolean win = winner == choice;
         double payout = this.settle(owner, id, stake, win ? PIG_MULT : 0.0);
         this.note(id, win ? "&a🐖 " + PIG_NAMES[winner] + " &al'emporte ! Vous gagnez &6" + (long)(payout - stake) + " pièces&a."
            : "&c🐖 " + PIG_NAMES[winner] + " &cfranchit la ligne la première… La maison garde vos &6" + stake + " pièces&c.", win);
      }, 10L, 6L);
   }

   // ------------------------------------------------------------------ greed (multiplicateur qui explose)

   private double greedCap() {
      return Math.max(2.0, this.plugin.getConfig().getDouble("taverns.greed.max-multiplier", 20.0));
   }

   private double greedEdge() {
      return Math.max(0.0, Math.min(0.2, this.plugin.getConfig().getDouble("taverns.greed.house-edge", 0.04)));
   }

   private void openGreed(Player player, Guild owner) {
      Greed h = new Greed(owner.getId());
      Inventory inv = Bukkit.createInventory(h, 45, ChatUtil.component("&8&l💣 Greed — " + owner.getName()));
      this.renderGreed(inv, h, owner);
      player.openInventory(inv);
   }

   private void renderGreed(Inventory inv, Greed h, Guild owner) {
      this.fill(inv);
      inv.setItem(4, new ItemBuilder(Material.BARREL).name("&c&lGreed").lore(this.houseLine(owner), "&7Lancez la partie : le multiplicateur part de", "&a×1,00 &7et grimpe de plus en plus vite.",
         "&7Cliquez sur &aENCAISSER &7pour empocher", "&7mise × multiplicateur… avant l'explosion !", "&cS'il explose avant, la mise est perdue.", "&8Fermer le menu en cours de partie = abandon.",
         "&7Plafond : &a×" + this.greedCap() + " &7(encaissé automatiquement)").build());
      this.chips(inv, h);
      inv.setItem(16, new ItemBuilder(Material.TNT).name("&a&lLANCER").lore("&7Mise : &6" + BETS[h.bet] + " pièces").build());
      inv.setItem(0, this.back());
      this.renderGreedBar(inv, 1.0, false);
   }

   /** Rangée de verre qui se remplit au fil du multiplicateur (échelle logarithmique jusqu'au plafond). */
   private void renderGreedBar(Inventory inv, double mult, boolean boom) {
      double fill = Math.log(mult) / Math.log(this.greedCap());
      int lit = (int)Math.round(Math.max(0.0, Math.min(1.0, fill)) * 9);
      for (int i = 0; i < 9; i++) {
         Material m = boom ? Material.RED_STAINED_GLASS_PANE : i < lit ? (i < 3 ? Material.LIME_STAINED_GLASS_PANE : i < 6 ? Material.YELLOW_STAINED_GLASS_PANE : Material.ORANGE_STAINED_GLASS_PANE)
            : Material.BLACK_STAINED_GLASS_PANE;
         inv.setItem(18 + i, new ItemBuilder(m).name(boom ? "&4&lBOUM !" : String.format(java.util.Locale.ROOT, "&e×%.2f", mult)).build());
      }
   }

   private void startGreed(Player player, Greed h, Guild owner, Inventory inv) {
      int stake = BETS[h.bet];
      // La banque doit au moins couvrir un ×2 ; au-delà, le plafond de la partie s'adapte à ce qu'elle peut payer.
      if (!this.take(player, owner, stake, 2.0)) {
         return;
      }

      h.stake = stake;
      h.cap = Math.max(1.01, Math.min(this.greedCap(), 1.0 + owner.getBankBalance() / stake));
      // Point d'explosion : P(explosion après m) = (1 - avantage) / m, donc espérance (1 - avantage) quel que soit le moment où l'on encaisse.
      double u = ThreadLocalRandom.current().nextDouble();
      h.crash = (1.0 - this.greedEdge()) / Math.max(1e-9, 1.0 - u);
      h.mult = 1.0;
      h.running = true;
      UUID id = player.getUniqueId();
      this.playing.add(id);
      ItemStack cash = new ItemBuilder(Material.EMERALD_BLOCK).name("&a&lENCAISSER").lore("&7Cliquez n'importe où ici !").build();
      for (int s = 27; s <= 44; s++) {
         inv.setItem(s, cash);
      }

      ChatUtil.playSound(player, Sound.ENTITY_TNT_PRIMED, 0.8F, 1.2F);
      Bukkit.getScheduler().runTaskTimer(this.plugin, task -> {
         if (!h.running) {
            task.cancel();
            return;
         }

         Player p = Bukkit.getPlayer(id);
         if (p == null || inv.getViewers().isEmpty()) {
            task.cancel();
            h.running = false;
            this.playing.remove(id);
            this.settle(owner, id, h.stake, 0.0);
            this.note(id, "&c💣 Vous avez quitté la table en pleine partie : la mise de &6" + h.stake + " pièces &cest perdue.", false);
            return;
         }

         double next = Math.floor(h.mult * 1.012 * 100.0 + 1.0) / 100.0;
         if (next >= h.crash) {
            task.cancel();
            h.running = false;
            this.playing.remove(id);
            this.settle(owner, id, h.stake, 0.0);
            this.renderGreedBar(inv, h.mult, true);
            inv.setItem(16, new ItemBuilder(Material.TNT).name("&a&lREJOUER").lore("&7Mise : &6" + BETS[h.bet] + " pièces").build());
            for (int s = 27; s <= 44; s++) {
               inv.setItem(s, this.glass());
            }

            ChatUtil.playSound(p, Sound.ENTITY_GENERIC_EXPLODE, 0.7F, 1.0F);
            this.note(id, String.format(java.util.Locale.ROOT, "&c💣 BOUM à ×%.2f ! La maison garde vos &6%d pièces&c.", Math.max(1.0, h.crash), h.stake), false);
            return;
         }

         h.mult = Math.min(next, h.cap);
         this.renderGreedBar(inv, h.mult, false);
         inv.setItem(16, new ItemBuilder(Material.GOLD_BLOCK).name(String.format(java.util.Locale.ROOT, "&e&l×%.2f", h.mult))
            .lore(String.format(java.util.Locale.ROOT, "&7Gain si vous encaissez : &6%d pièces", (long)Math.floor(h.stake * h.mult))).build());
         if ((int)(h.mult * 100) % 10 == 0) {
            this.tick(p, Sound.BLOCK_NOTE_BLOCK_PLING, (float)Math.min(2.0, 0.5 + h.mult / 4.0));
         }

         if (h.mult >= h.cap) {
            task.cancel();
            this.cashOut(id, h, owner, inv);
         }
      }, 10L, 2L);
   }

   private void cashOut(UUID id, Greed h, Guild owner, Inventory inv) {
      if (!h.running) {
         return;
      }

      h.running = false;
      this.playing.remove(id);
      double payout = this.settle(owner, id, h.stake, h.mult);
      this.note(id, String.format(java.util.Locale.ROOT, "&a💣 Encaissé à ×%.2f : &6+%d pièces &a(gain net %d).", h.mult, (long)payout, (long)(payout - h.stake)), payout > h.stake);
      Player p = Bukkit.getPlayer(id);
      if (p != null && h.mult >= 10.0) {
         ChatUtil.broadcast(String.format(java.util.Locale.ROOT, "&c💣 &e%s &7a encaissé un &a×%.2f &7au Greed de la taverne de %s !", p.getName(), h.mult, owner.getName()));
      }

      if (!inv.getViewers().isEmpty()) {
         for (int s = 27; s <= 44; s++) {
            inv.setItem(s, this.glass());
         }

         inv.setItem(16, new ItemBuilder(Material.TNT).name("&a&lREJOUER").lore("&7Mise : &6" + BETS[h.bet] + " pièces").build());
      }
   }

   private static int backSlot(GameHolder h) {
      return h instanceof Pigs || h instanceof Greed ? 0 : h instanceof Roulette || h instanceof Arm ? 45 : 18;
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
            case 20 -> this.openDice(player, owner);
            case 22 -> this.openArm(player, owner);
            case 24 -> this.openPigs(player, owner);
            case 26 -> this.openGreed(player, owner);
            default -> {
            }
         }

         return;
      }

      if (holder instanceof Greed g && g.running) {
         if (slot >= 27 && slot <= 44) {
            this.cashOut(player.getUniqueId(), g, owner, inv);
         }

         return;
      }

      if (holder instanceof Arm arm && arm.running) {
         this.armClick(player, arm, slot, inv);
         return;
      }

      if (slot == backSlot(holder)) {
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
            } else if (holder instanceof Dice d) {
               this.renderDice(inv, d, owner);
            } else if (holder instanceof Arm a) {
               this.renderArm(inv, a, owner);
            } else if (holder instanceof Pigs pg) {
               this.renderPigs(inv, pg, owner);
            } else if (holder instanceof Greed gr) {
               this.renderGreed(inv, gr, owner);
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
      } else if (holder instanceof Dice d && slot == 22) {
         this.rollDice(player, d, owner, inv);
      } else if (holder instanceof Arm a && slot == 16) {
         this.startArm(player, a, owner, inv);
      } else if (holder instanceof Greed g && slot == 16) {
         this.startGreed(player, g, owner, inv);
      } else if (holder instanceof Pigs pg && (slot == 18 || slot == 27 || slot == 36 || slot == 45)) {
         this.racePigs(player, pg, owner, (slot - 18) / 9, inv);
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
