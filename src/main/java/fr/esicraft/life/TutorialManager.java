package fr.esicraft.life;

import fr.esicraft.EsicraftPlugin;
import fr.esicraft.data.PlayerData;
import fr.esicraft.race.Race;
import fr.esicraft.util.ChatUtil;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import net.kyori.adventure.bossbar.BossBar;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;

/**
 * Tutoriel guidé en quêtes : une suite d'étapes affichée dans une barre en haut de l'écran (choisir un métier, rejoindre une guilde,
 * accomplir un premier contrat, descendre au monde de ressources, faire un premier échange à l'Hôtel des Ventes). Chaque étape rapporte une petite
 * récompense, une seule fois par compte ; le tutoriel terminé offre un bonus final.
 */
public class TutorialManager implements CommandExecutor, TabCompleter, Listener {
   public enum Step {
      JOB("Choisir un métier", "&e/metier &7— chaque métier a ses talents (mineur, forgeron, rôdeur…)", 100),
      GUILD("Rejoindre ou fonder une guilde", "&e/g join <guilde> &7après une invitation, ou &e/g create <nom> <tag>", 150),
      CONTRACT("Accomplir un premier contrat", "&e/contrat &7— trois petites missions par jour, payées en pièces", 150),
      RESOURCE_WORLD("Descendre au monde de ressources", "&e/ressources &7— minerai frais, remis à neuf chaque semaine", 100),
      MARKET("Faire un premier échange à l'Hôtel des Ventes", "&e/ah &7— acheter, vendre (&e/ah sell <prix>&7) ou passer une commande", 100);

      final String title;
      final String hint;
      final int reward;

      Step(String title, String hint, int reward) {
         this.title = title;
         this.hint = hint;
         this.reward = reward;
      }
   }

   private static final String KV = "tuto:";
   private static final String KV_HIDDEN = "tutohide:";
   private static final int ALL = (1 << Step.values().length) - 1;
   private static final double FINAL_BONUS = 300.0;

   private final EsicraftPlugin plugin;
   private final Map<UUID, Integer> progress = new ConcurrentHashMap<>();
   private final Map<UUID, BossBar> bars = new ConcurrentHashMap<>();
   private final Set<UUID> hidden = ConcurrentHashMap.newKeySet();

   public TutorialManager(EsicraftPlugin plugin) {
      this.plugin = plugin;
   }

   private boolean enabled() {
      return this.plugin.getConfig().getBoolean("tutorial.enabled", true);
   }

   public void init() {
      Bukkit.getScheduler().runTaskTimer(this.plugin, this::scan, 100L, 100L);
      for (Player p : Bukkit.getOnlinePlayers()) {
         this.load(p);
      }
   }

   public void clear() {
      for (Map.Entry<UUID, BossBar> e : this.bars.entrySet()) {
         Player p = Bukkit.getPlayer(e.getKey());
         if (p != null) {
            p.hideBossBar(e.getValue());
         }
      }

      this.bars.clear();
      this.progress.clear();
      this.hidden.clear();
   }

   private void load(Player p) {
      UUID id = p.getUniqueId();
      int mask = 0;
      for (Object[] row : this.plugin.getDataManager().getStorage().select("SELECT v FROM kv WHERE k = ?;", 1, KV + id)) {
         try {
            mask = Integer.parseInt(String.valueOf(row[0]));
         } catch (NumberFormatException ignored) {
         }
      }

      this.progress.put(id, mask);
      if (!this.plugin.getDataManager().getStorage().select("SELECT v FROM kv WHERE k = ?;", 1, KV_HIDDEN + id).isEmpty()) {
         this.hidden.add(id);
      }

      this.refresh(p);
   }

   private void save(UUID id, int mask) {
      this.plugin.getDataManager().getStorage().exec("INSERT INTO kv (k, v) VALUES (?, ?) ON CONFLICT(k) DO UPDATE SET v = excluded.v;", KV + id, String.valueOf(mask));
   }

   public boolean isDone(UUID id, Step step) {
      return (this.progress.getOrDefault(id, 0) & 1 << step.ordinal()) != 0;
   }

   private Step current(UUID id) {
      for (Step s : Step.values()) {
         if (!this.isDone(id, s)) {
            return s;
         }
      }

      return null;
   }

   /** Valide une étape (sans effet si elle l'est déjà ou si le joueur n'est pas chargé). */
   public void complete(Player p, Step step) {
      if (p == null || !this.enabled()) {
         return;
      }

      UUID id = p.getUniqueId();
      Integer mask = this.progress.get(id);
      if (mask == null || (mask & 1 << step.ordinal()) != 0) {
         return;
      }

      int next = mask | 1 << step.ordinal();
      this.progress.put(id, next);
      this.save(id, next);
      PlayerData data = this.plugin.getDataManager().getPlayerData(id);
      if (data != null) {
         data.addCoins(step.reward);
         this.plugin.getDataManager().savePlayer(data);
      }

      ChatUtil.sendMessage(p, "&a&l✔ Quête accomplie : &f" + step.title + " &8» &6+" + step.reward + " pièces");
      ChatUtil.playSound(p, Sound.UI_TOAST_CHALLENGE_COMPLETE, 0.7F, 1.4F);
      if (next == ALL) {
         if (data != null) {
            data.addCoins(FINAL_BONUS);
            this.plugin.getDataManager().savePlayer(data);
         }

         for (ItemStack left : p.getInventory().addItem(new ItemStack(Material.BREAD, 16), new ItemStack(Material.TORCH, 32), new ItemStack(Material.COOKED_BEEF, 8)).values()) {
            p.getWorld().dropItemNaturally(p.getLocation(), left);
         }

         ChatUtil.sendMessage(p, "&6&l★ Tutoriel terminé ! &eVous connaissez l'essentiel d'Esicraft. &6+" + (long)FINAL_BONUS + " pièces &eet un petit panier de provisions. Bonne aventure !");
         ChatUtil.broadcast("&6★ &e" + p.getName() + " &7a terminé le tutoriel : souhaitez-lui la bienvenue !");
      } else {
         Step s = this.current(id);
         if (s != null) {
            ChatUtil.sendMessage(p, "&7Prochaine quête : &e" + s.title + " &8— " + s.hint);
         }
      }

      this.refresh(p);
   }

   /** Barre de progression en haut de l'écran tant que le tutoriel n'est pas fini (masquable). */
   private void refresh(Player p) {
      UUID id = p.getUniqueId();
      Step s = this.current(id);
      BossBar bar = this.bars.get(id);
      if (s == null || this.hidden.contains(id) || !this.enabled()) {
         if (bar != null) {
            p.hideBossBar(bar);
            this.bars.remove(id);
         }

         return;
      }

      int done = Integer.bitCount(this.progress.getOrDefault(id, 0));
      int total = Step.values().length;
      var title = ChatUtil.component("&6📜 Quête " + (done + 1) + "/" + total + " : &e" + s.title + " &7(/tutoriel)");
      float prog = Math.max(0.0F, Math.min(1.0F, (float)done / total));
      if (bar == null) {
         bar = BossBar.bossBar(title, prog, BossBar.Color.YELLOW, BossBar.Overlay.NOTCHED_10);
         this.bars.put(id, bar);
         p.showBossBar(bar);
      } else {
         bar.name(title);
         bar.progress(prog);
      }
   }

   /** Étapes vérifiables par l'état du joueur (métier, guilde) : contrôlées toutes les 5 secondes. */
   private void scan() {
      if (!this.enabled()) {
         return;
      }

      for (Player p : Bukkit.getOnlinePlayers()) {
         UUID id = p.getUniqueId();
         Integer mask = this.progress.get(id);
         if (mask == null || mask == ALL) {
            continue;
         }

         if (!this.isDone(id, Step.JOB) && this.plugin.getRaceManager().getRace(p) != Race.NONE) {
            this.complete(p, Step.JOB);
         }

         if (!this.isDone(id, Step.GUILD) && this.plugin.getGuildManager().getGuildByPlayer(id) != null) {
            this.complete(p, Step.GUILD);
         }

         if (!this.isDone(id, Step.RESOURCE_WORLD) && this.plugin.getResourceWorld().isResourceWorld(p.getWorld())) {
            this.complete(p, Step.RESOURCE_WORLD);
         }
      }
   }

   @EventHandler
   public void onJoin(PlayerJoinEvent event) {
      Player p = event.getPlayer();
      this.load(p);
      Step s = this.current(p.getUniqueId());
      if (s != null && this.enabled() && !this.hidden.contains(p.getUniqueId())) {
         Bukkit.getScheduler().runTaskLater(this.plugin, () -> {
            if (p.isOnline()) {
               ChatUtil.sendMessage(p, "&6📜 Tutoriel : &e" + s.title + " &8— " + s.hint + " &7(&e/tutoriel&7 pour la liste)");
            }
         }, 60L);
      }
   }

   @EventHandler
   public void onQuit(PlayerQuitEvent event) {
      UUID id = event.getPlayer().getUniqueId();
      this.progress.remove(id);
      this.bars.remove(id);
      this.hidden.remove(id);
   }

   // ------------------------------------------------------------------ commande

   public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
      if (!(sender instanceof Player p)) {
         ChatUtil.sendMessage(sender, "&cCommande réservée aux joueurs.");
         return true;
      }

      UUID id = p.getUniqueId();
      String sub = args.length == 0 ? "" : args[0].toLowerCase(Locale.ROOT);
      if (sub.equals("masquer") || sub.equals("cacher") || sub.equals("hide")) {
         this.hidden.add(id);
         this.plugin.getDataManager().getStorage().exec("INSERT INTO kv (k, v) VALUES (?, '1') ON CONFLICT(k) DO UPDATE SET v = excluded.v;", KV_HIDDEN + id);
         this.refresh(p);
         ChatUtil.sendMessage(p, "&7Barre du tutoriel masquée (&e/tutoriel afficher&7 pour la remettre). Les quêtes restent valables.");
         return true;
      }

      if (sub.equals("afficher") || sub.equals("show")) {
         this.hidden.remove(id);
         this.plugin.getDataManager().getStorage().exec("DELETE FROM kv WHERE k = ?;", KV_HIDDEN + id);
         this.refresh(p);
         ChatUtil.sendMessage(p, "&7Barre du tutoriel affichée.");
         return true;
      }

      int done = Integer.bitCount(this.progress.getOrDefault(id, 0));
      ChatUtil.sendMessage(p, "&6&l📜 Tutoriel d'Esicraft &7(" + done + "/" + Step.values().length + ") — chaque quête est récompensée, " + (long)FINAL_BONUS + " pièces de bonus à la fin.");
      for (Step s : Step.values()) {
         boolean ok = this.isDone(id, s);
         ChatUtil.sendRaw(p, ChatUtil.color((ok ? " &a✔ &7&m" : " &e➤ &f") + s.title + (ok ? "" : " &8(&6+" + s.reward + "&8)")));
         if (!ok) {
            ChatUtil.sendRaw(p, ChatUtil.color("     " + s.hint));
         }
      }

      ChatUtil.sendRaw(p, ChatUtil.color("&7Le livre des règles : &e/guide&7. Masquer la barre : &e/tutoriel masquer&7."));
      return true;
   }

   public List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command command, @NotNull String alias, @NotNull String[] args) {
      return args.length == 1 ? List.of("masquer", "afficher") : List.of();
   }
}
