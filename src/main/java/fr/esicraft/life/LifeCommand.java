package fr.esicraft.life;

import fr.esicraft.EsicraftPlugin;
import fr.esicraft.guild.Guild;
import fr.esicraft.util.ChatUtil;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabExecutor;
import org.bukkit.entity.Player;

/** /journal, /succes, /climat, /aide et /deguisement. */
public class LifeCommand implements TabExecutor {
   private static final DateTimeFormatter WHEN = DateTimeFormatter.ofPattern("dd/MM HH:mm").withZone(ZoneId.of("Europe/Paris"));
   private static final String LINE = "&8&m--------------------------------------------------";
   private final EsicraftPlugin plugin;

   public LifeCommand(EsicraftPlugin plugin) {
      this.plugin = plugin;
   }

   @Override
   public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
      switch (command.getName().toLowerCase(Locale.ROOT)) {
         case "journal" -> this.journal(sender);
         case "succes" -> this.achievements(sender, args);
         case "climat" -> this.climate(sender);
         case "aide" -> {
            if (sender instanceof Player p) {
               String error = this.plugin.getHelpCalls().call(p);
               if (error != null) {
                  ChatUtil.sendMessage(p, error);
               }
            } else {
               ChatUtil.sendMessage(sender, "&cCommande réservée aux joueurs.");
            }
         }
         case "deguisement" -> this.disguise(sender, args);
         default -> {
            return false;
         }
      }

      return true;
   }

   private void journal(CommandSender sender) {
      Bukkit.getScheduler().runTaskAsynchronously(this.plugin, () -> {
         List<Chronicle.Entry> entries = this.plugin.getChronicle().recent(12);
         Bukkit.getScheduler().runTask(this.plugin, () -> {
            ChatUtil.sendRaw(sender, LINE);
            ChatUtil.sendRaw(sender, "  &6&l📰 JOURNAL DE GUERRE &7(12 derniers faits)");
            if (entries.isEmpty()) {
               ChatUtil.sendRaw(sender, "  &7Rien à signaler… pour l'instant.");
            }

            for (Chronicle.Entry e : entries) {
               ChatUtil.sendRaw(sender, "  &8" + WHEN.format(Instant.ofEpochMilli(e.ts())) + " &f" + e.kind().getIcon() + " &7" + e.text());
            }

            ChatUtil.sendRaw(sender, "  &7Tout le journal est sur le site web du serveur.");
            ChatUtil.sendRaw(sender, LINE);
         });
      });
   }

   private void achievements(CommandSender sender, String[] args) {
      UUID target;
      String name;
      if (args.length > 0) {
         OfflinePlayer op = Bukkit.getOfflinePlayerIfCached(args[0]);
         if (op == null) {
            ChatUtil.sendMessage(sender, "&cJoueur introuvable.");
            return;
         }

         target = op.getUniqueId();
         name = op.getName() != null ? op.getName() : args[0];
      } else if (sender instanceof Player p) {
         target = p.getUniqueId();
         name = p.getName();
      } else {
         ChatUtil.sendMessage(sender, "&cUsage : /succes <joueur>");
         return;
      }

      AchievementManager am = this.plugin.getAchievements();
      Map<Achievement, Long> owned = am.of(target);
      ChatUtil.sendRaw(sender, LINE);
      ChatUtil.sendRaw(sender, "  &6&l🏅 SUCCÈS DE &e" + name + " &7(" + owned.size() + "/" + Achievement.values().length + ")");
      for (Achievement a : Achievement.values()) {
         boolean has = owned.containsKey(a);
         String pct = String.format(Locale.ROOT, "%.0f", am.percent(a));
         ChatUtil.sendRaw(sender, "  " + (has ? "&a✔ " : "&8✖ ") + a.getIcon() + " " + (has ? "&f" : "&7") + a.getTitle() + " &8— &7" + a.getDescription() + " &8("
            + pct + " %" + (am.isRare(a) ? " &d· rare" : "") + "&8)");
      }

      ChatUtil.sendRaw(sender, LINE);
   }

   private void climate(CommandSender sender) {
      NatureSeasonManager nature = this.plugin.getNatureSeasons();
      if (!nature.isEnabled()) {
         ChatUtil.sendMessage(sender, "&7Les saisons naturelles sont désactivées sur ce serveur.");
         return;
      }

      NatureSeason now = nature.current();
      ChatUtil.sendRaw(sender, LINE);
      ChatUtil.sendRaw(sender, "  " + now.getColor() + "&l" + now.display() + " &7— encore " + nature.remainingDays() + " jour" + (nature.remainingDays() > 1 ? "s" : ""));
      for (String effect : now.getEffects()) {
         ChatUtil.sendRaw(sender, "  &7• " + effect);
      }

      ChatUtil.sendRaw(sender, "  &7Ensuite : " + nature.next().getColor() + nature.next().display());
      ChatUtil.sendRaw(sender, LINE);
   }

   private void disguise(CommandSender sender, String[] args) {
      if (!(sender instanceof Player player)) {
         ChatUtil.sendMessage(sender, "&cCommande réservée aux joueurs.");
         return;
      }

      DisguiseManager dm = this.plugin.getDisguiseManager();
      if (args.length == 0) {
         ChatUtil.sendMessage(player, "&7Usage : &e/deguisement <guilde>&7 (se faire passer pour un de ses membres) · &e/deguisement <Pseudo>&7 (skin et pseudo de n'importe quel compte Minecraft) · &e/deguisement off");
         ChatUtil.sendRaw(player, "&7Prix : &6" + this.plugin.getConfig().getLong("life.disguise.cost", 750L) + " pièces &7· durée : &e"
            + this.plugin.getConfig().getLong("life.disguise.duration-seconds", 300L) / 60L + " min &7· infliger des dégâts vous démasque.");
         return;
      }

      if (args[0].equalsIgnoreCase("off")) {
         if (dm.get(player.getUniqueId()) == null) {
            ChatUtil.sendMessage(player, "&7Vous n'êtes pas déguisé.");
         } else {
            dm.reveal(player, false);
         }

         return;
      }

      dm.disguise(player, String.join(" ", args), error -> {
         if (error != null) {
            ChatUtil.sendMessage(player, error);
            return;
         }

         DisguiseManager.Disguise d = dm.get(player.getUniqueId());
         if (d.hasGuild()) {
            ChatUtil.sendMessage(player, "&5🎭 Vous voilà déguisé en &e" + d.fakeName() + " &8[&6" + d.tag() + "&8] &5! Les pièges de &e" + d.guildName()
               + " &5vous ignorent. Attaquer quoi que ce soit vous démasque.");
         } else {
            ChatUtil.sendMessage(player, "&5🎭 Vous voilà déguisé en &e" + d.fakeName() + " &5(skin et pseudo changés) ! Attaquer quoi que ce soit vous démasque.");
         }
      });
   }

   @Override
   public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
      List<String> out = new ArrayList<>();
      if (args.length != 1) {
         return out;
      }

      String prefix = args[0].toLowerCase(Locale.ROOT);
      String name = command.getName().toLowerCase(Locale.ROOT);
      if (name.equals("deguisement")) {
         if ("off".startsWith(prefix)) {
            out.add("off");
         }

         for (Guild g : this.plugin.getGuildManager().getAllGuilds()) {
            if (g.getName().toLowerCase(Locale.ROOT).startsWith(prefix)) {
               out.add(g.getName());
            }
         }
      } else if (name.equals("succes")) {
         for (Player p : Bukkit.getOnlinePlayers()) {
            if (p.getName().toLowerCase(Locale.ROOT).startsWith(prefix)) {
               out.add(p.getName());
            }
         }
      }

      return out;
   }
}
