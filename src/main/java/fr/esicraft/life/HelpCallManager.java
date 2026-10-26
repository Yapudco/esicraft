package fr.esicraft.life;

import fr.esicraft.EsicraftPlugin;
import fr.esicraft.guild.Guild;
import fr.esicraft.util.ChatUtil;
import fr.esicraft.util.ChunkKey;
import fr.esicraft.util.Lang;
import java.time.Duration;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import net.kyori.adventure.title.Title;
import net.kyori.adventure.title.Title.Times;
import org.bukkit.Bukkit;
import org.bukkit.Sound;
import org.bukkit.entity.Player;

/**
 * Appel à l'aide : une guilde attaquée prévient ses alliés (alliance ou vassalité). Les alliés présents sur ses terres pendant
 * la bataille sont récompensés si la horde est repoussée ({@code life.help.reward} pièces chacun, {@value #MAX_HELPERS} au plus).
 */
public class HelpCallManager {
   private static final int MAX_HELPERS = 6;
   private static final long CALL_COOLDOWN_MS = 10L * 60000L;
   private static final long CALL_LIFETIME_MS = 15L * 60000L;

   private static final class Call {
      final long startedAt = System.currentTimeMillis();
      final Set<UUID> helpers = ConcurrentHashMap.newKeySet();
   }

   private final EsicraftPlugin plugin;
   private final Map<String, Call> calls = new ConcurrentHashMap<>();
   private final Map<String, Long> lastCall = new ConcurrentHashMap<>();

   public void clearAll() {
      this.calls.clear();
      this.lastCall.clear();
   }

   public HelpCallManager(EsicraftPlugin plugin) {
      this.plugin = plugin;
   }

   public void init() {
      Bukkit.getScheduler().runTaskTimer(this.plugin, this::track, 100L, 100L);
   }

   private boolean inDanger(Guild guild) {
      return this.plugin.getRaidManager().isGuildUnderRaid(guild.getId()) || this.plugin.getNexusManager().wasNexusRecentlyAttacked(guild, 120000L);
   }

   /** /aide : renvoie un message d'erreur, ou null si l'appel est parti. */
   public String call(Player caller) {
      Guild guild = this.plugin.getGuildManager().getGuildByPlayer(caller.getUniqueId());
      if (guild == null) {
         return "&cVous devez appartenir à une guilde pour appeler à l'aide.";
      }

      if (!guild.hasNexus() || !this.inDanger(guild)) {
         return "&cVotre guilde n'est pas attaquée : l'appel à l'aide n'est possible que pendant un raid ou une attaque du Nexus.";
      }

      long now = System.currentTimeMillis();
      Long last = this.lastCall.get(guild.getId());
      if (last != null && now - last < CALL_COOLDOWN_MS && !this.plugin.bypassCooldowns(caller)) {
         return "&cVotre guilde a déjà appelé à l'aide il y a moins de 10 minutes.";
      }

      List<Player> allies = new ArrayList<>();
      for (Player p : Bukkit.getOnlinePlayers()) {
         Guild other = this.plugin.getGuildManager().getGuildByPlayer(p.getUniqueId());
         if (other != null && !other.getId().equals(guild.getId()) && this.plugin.getTreatyManager().areFriends(guild.getId(), other.getId())) {
            allies.add(p);
         }
      }

      if (allies.isEmpty()) {
         return "&cAucun allié connecté : signez une alliance (&e/traite&c) pour pouvoir appeler à l'aide.";
      }

      this.lastCall.put(guild.getId(), now);
      this.calls.put(guild.getId(), new Call());
      int pct = (int)Math.round(guild.getNexus().getHealthPercentage());
      long reward = this.reward();
      for (Player ally : allies) {
         ChatUtil.sendMessage(ally, "&c&l🆘 APPEL À L'AIDE ! &e" + caller.getName() + " &7(&6" + guild.getName() + "&7) &cdemande des renforts : son Nexus est attaqué (&e"
            + pct + " %&c). &7Rejoignez ses terres : &6+" + reward + " pièces &7si la horde est repoussée.");
         ally.showTitle(Title.title(ChatUtil.component(Lang.tr(ally, "&c&l🆘 APPEL À L'AIDE")), ChatUtil.component("&e" + guild.getName()),
            Times.times(Duration.ofMillis(300L), Duration.ofMillis(3000L), Duration.ofMillis(800L))));
         ChatUtil.playSound(ally, Sound.EVENT_RAID_HORN, 1.0F, 1.4F);
      }

      ChatUtil.broadcastGuild(guild, "&a🆘 " + caller.getName() + " a appelé vos alliés à l'aide (" + allies.size() + " joueur" + (allies.size() > 1 ? "s" : "") + " prévenu"
         + (allies.size() > 1 ? "s" : "") + ").");
      return null;
   }

   private long reward() {
      return Math.max(0L, this.plugin.getConfig().getLong("life.help.reward", 250L));
   }

   /** Toutes les 5 s : note les alliés présents sur les terres de la guilde en danger ; expire les vieux appels. */
   private void track() {
      long now = System.currentTimeMillis();
      for (Map.Entry<String, Call> e : this.calls.entrySet()) {
         Guild guild = this.plugin.getGuildManager().getGuildById(e.getKey());
         if (guild == null || now - e.getValue().startedAt > CALL_LIFETIME_MS && !this.plugin.getRaidManager().isGuildUnderRaid(e.getKey())) {
            this.calls.remove(e.getKey());
            continue;
         }

         for (Player p : Bukkit.getOnlinePlayers()) {
            Guild other = this.plugin.getGuildManager().getGuildByPlayer(p.getUniqueId());
            if (other != null && !other.getId().equals(guild.getId()) && this.plugin.getTreatyManager().areFriends(guild.getId(), other.getId())
               && guild.isClaimed(ChunkKey.from(p.getLocation().getChunk())) && e.getValue().helpers.add(p.getUniqueId())) {
               ChatUtil.sendActionbar(p, "&a🆘 Vous êtes arrivé en renfort chez &e" + guild.getName() + " &a!");
            }
         }
      }
   }

   /** Appelé quand une horde est repoussée. */
   public void onRepelled(Guild guild) {
      Call call = this.calls.remove(guild.getId());
      if (call == null || call.helpers.isEmpty()) {
         return;
      }

      long reward = this.reward();
      Set<String> names = new LinkedHashSet<>();
      int n = 0;
      for (UUID uuid : call.helpers) {
         if (n++ >= MAX_HELPERS) {
            break;
         }

         // Récompense TRANSFÉRÉE depuis la banque de la guilde défendue (et non créée) : plus de planche à billets entre guildes complices.
         long paid = (long)Math.min((double)reward, Math.floor(guild.getBankBalance()));
         if (paid > 0L && guild.withdrawBank((double)paid)) {
            this.plugin.getDataManager().creditPlayer(uuid, (double)paid);
         } else {
            paid = 0L;
         }

         this.plugin.getAchievements().grant(uuid, Achievement.SAVIOR);
         this.plugin.getAchievements().addStat(uuid, AchievementManager.STAT_HELPS, 1L);
         Player p = Bukkit.getPlayer(uuid);
         if (p != null) {
            names.add(p.getName());
            ChatUtil.sendMessage(p, "&a🆘 Merci pour votre aide ! La horde a été repoussée" + (paid > 0L ? " : &6+" + paid + " pièces &a(versées par la banque de la guilde)&a." : "&a."));
         }
      }

      if (!names.isEmpty()) {
         this.plugin.getChronicle().log(Chronicle.Kind.HELP, String.join(", ", names) + (names.size() > 1 ? " ont" : " a") + " répondu à l'appel de " + guild.getName() + ".", guild.getId(), null);
      }
   }

   public void cancel(String guildId) {
      this.calls.remove(guildId);
   }
}
