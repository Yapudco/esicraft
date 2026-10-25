package fr.esicraft.rail;

import fr.esicraft.EsicraftPlugin;
import fr.esicraft.guild.Guild;
import fr.esicraft.guild.GuildRole;
import fr.esicraft.util.ChatUtil;
import fr.esicraft.util.ChunkKey;
import java.util.Locale;
import java.util.regex.Pattern;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import org.bukkit.Location;
import org.bukkit.Sound;
import org.bukkit.Tag;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.SignChangeEvent;

/** Création d'une gare en posant un panneau « [Gare] / nom », et protection de ce panneau. */
public class RailListener implements Listener {
   private static final Pattern NAME = Pattern.compile("[\\p{L}0-9 '_-]{3,20}");
   private final EsicraftPlugin plugin;

   public RailListener(EsicraftPlugin plugin) {
      this.plugin = plugin;
   }

   private static String plain(Component c) {
      return c == null ? "" : PlainTextComponentSerializer.plainText().serialize(c).trim();
   }

   @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
   public void onSign(SignChangeEvent event) {
      String first = plain(event.line(0)).toLowerCase(Locale.ROOT);
      if (!first.equals("[gare]") && !first.equals("[station]")) {
         return;
      }

      Player player = event.getPlayer();
      if (!this.plugin.getConfig().getBoolean("rail.enabled", true)) {
         this.fail(event, player, "Le réseau ferroviaire est désactivé.");
         return;
      }

      Guild guild = this.plugin.getGuildManager().getGuildByPlayer(player.getUniqueId());
      if (guild == null || !guild.hasRoleAtLeast(player.getUniqueId(), GuildRole.OFFICER)) {
         this.fail(event, player, "Seuls les officiers et le chef d'une guilde peuvent créer une gare.");
         return;
      }

      int minAge = this.plugin.getConfig().getInt("rail.min-age-level", 2);
      if (guild.getCurrentAge().getLevel() < minAge) {
         this.fail(event, player, "Votre guilde doit avoir atteint l'Âge " + minAge + " pour créer des gares.");
         return;
      }

      String name = plain(event.line(1));
      if (!NAME.matcher(name).matches()) {
         this.fail(event, player, "Nom de gare invalide (ligne 2) : 3 à 20 caractères (lettres, chiffres, espace, ' _ -).");
         return;
      }

      if (this.plugin.getRailManager().nameTaken(name)) {
         this.fail(event, player, "Une gare porte déjà ce nom.");
         return;
      }

      Block signBlock = event.getBlock();
      Guild owner = this.plugin.getGuildManager().getGuildByChunk(ChunkKey.from(signBlock.getChunk()));
      if (owner != null && !owner.getId().equals(guild.getId())) {
         this.fail(event, player, "Vous ne pouvez pas créer une gare sur le territoire d'une autre guilde.");
         return;
      }

      int max = this.plugin.getConfig().getInt("rail.max-stations-per-guild", 6);
      if (this.plugin.getRailManager().stationsOf(guild.getId()).size() >= max) {
         this.fail(event, player, "Votre guilde possède déjà le maximum de gares (" + max + ").");
         return;
      }

      Block rail = this.findRail(signBlock);
      if (rail == null) {
         this.fail(event, player, "Aucun rail à proximité : posez le panneau à moins de 2 blocs d'un rail.");
         return;
      }

      double cost = this.plugin.getConfig().getDouble("rail.station-creation-cost", 500.0);
      if (!guild.withdrawBank(cost)) {
         this.fail(event, player, "La banque de guilde doit contenir " + String.format(Locale.ROOT, "%.0f", cost) + " pièces.");
         return;
      }

      this.plugin.getDataManager().saveGuild(guild);
      Location signLoc = signBlock.getLocation();
      this.plugin.getRailManager().createStation(name, guild, signLoc, rail.getLocation());
      event.line(0, ChatUtil.component("&1&l[Gare]"));
      event.line(1, ChatUtil.component("&0" + name));
      event.line(2, ChatUtil.component("&8" + guild.getName()));
      event.line(3, ChatUtil.component("&2/station"));
      ChatUtil.sendMessage(player, "&a🚉 Gare &e" + name + " &acréée ! Les voyageurs ouvrent le menu avec &e/station &aà moins de 8 blocs. Gérez péages et relations avec &e/rail&a.");
      ChatUtil.playSound(player, Sound.BLOCK_NOTE_BLOCK_CHIME, 1.0F, 1.5F);
   }

   private void fail(SignChangeEvent event, Player player, String reason) {
      event.line(0, ChatUtil.component("&4[Gare]"));
      event.line(1, ChatUtil.component("&cRefusé"));
      ChatUtil.sendMessage(player, "&c❌ " + reason);
      ChatUtil.playSound(player, Sound.ENTITY_VILLAGER_NO, 1.0F, 1.0F);
   }

   private Block findRail(Block center) {
      Block best = null;
      double bestDist = Double.MAX_VALUE;
      for (int dx = -2; dx <= 2; dx++) {
         for (int dy = -1; dy <= 1; dy++) {
            for (int dz = -2; dz <= 2; dz++) {
               Block b = center.getRelative(dx, dy, dz);
               if (Tag.RAILS.isTagged(b.getType())) {
                  double d = dx * dx + dy * dy + dz * dz;
                  if (d < bestDist) {
                     bestDist = d;
                     best = b;
                  }
               }
            }
         }
      }

      return best;
   }

   @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
   public void onBreak(BlockBreakEvent event) {
      Station station = this.plugin.getRailManager().stationAtSign(event.getBlock());
      if (station == null) {
         return;
      }

      Player player = event.getPlayer();
      Guild guild = this.plugin.getGuildManager().getGuildByPlayer(player.getUniqueId());
      boolean allowed = player.hasPermission("esicraft.admin")
         || guild == null && this.plugin.getRailManager().guildOf(station) == null
         || guild != null && guild.getId().equals(station.getGuildId()) && guild.hasRoleAtLeast(player.getUniqueId(), GuildRole.OFFICER);
      if (!allowed) {
         event.setCancelled(true);
         ChatUtil.sendActionbar(player, "&c❌ Cette gare appartient à une autre guilde (ou réservée aux officiers).");
         return;
      }

      this.plugin.getRailManager().removeStation(station);
      ChatUtil.sendMessage(player, "&e🚉 La gare &6" + station.getName() + " &eest démantelée.");
   }
}
