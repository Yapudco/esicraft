package fr.esicraft.life;

import fr.esicraft.EsicraftPlugin;
import fr.esicraft.fun.ContractManager;
import fr.esicraft.fun.ContractType;
import fr.esicraft.guild.Guild;
import fr.esicraft.race.Race;
import fr.esicraft.util.ChatUtil;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import org.bukkit.Bukkit;
import org.bukkit.Sound;
import org.bukkit.entity.Player;

/**
 * Contrat de guilde quotidien : trois objectifs, chacun réservé à un métier (un Nain mine, un Forgeron fond, un Ingénieur pose
 * de la redstone…). Seuls les membres du bon métier font avancer leur objectif : une guilde variée peut tout remplir.
 * Récompense versée à la banque de guilde : {@value #PART_REWARD} par objectif, {@value #ALL_REWARD} de plus pour les trois.
 */
public class GuildContractManager {
   public static final int PARTS = 3;
   public static final double PART_REWARD = 200.0;
   public static final double ALL_REWARD = 750.0;
   private static final Race[] JOBS = {Race.DWARF, Race.SYLVAN, Race.BLACKSMITH, Race.RANGER, Race.ARCANIST, Race.ENGINEER};

   public record Part(Race race, ContractType type, int target) {
   }

   public static final class Day {
      final String day;
      final List<Part> parts;
      final int[] progress = new int[PARTS];
      final boolean[] done = new boolean[PARTS];
      final Set<UUID> contributors = ConcurrentHashMap.newKeySet();
      volatile boolean dirty;

      Day(String day, List<Part> parts) {
         this.day = day;
         this.parts = parts;
      }

      public List<Part> parts() {
         return this.parts;
      }

      public int progress(int i) {
         return this.progress[i];
      }

      public boolean done(int i) {
         return this.done[i];
      }

      public boolean allDone() {
         for (boolean d : this.done) {
            if (!d) {
               return false;
            }
         }

         return true;
      }
   }

   private final EsicraftPlugin plugin;
   private final Map<String, Day> days = new ConcurrentHashMap<>();

   public GuildContractManager(EsicraftPlugin plugin) {
      this.plugin = plugin;
   }

   public void init() {
      this.plugin.getDataManager().getStorage().exec(
         "CREATE TABLE IF NOT EXISTS guild_contract (guild_id VARCHAR(36), day VARCHAR(10), idx INT, progress INT, done INT DEFAULT 0, PRIMARY KEY (guild_id, day, idx));");
      Bukkit.getScheduler().runTaskTimerAsynchronously(this.plugin, this::flush, 600L, 600L);
   }

   static ContractType typeOf(Race race) {
      return switch (race) {
         case DWARF -> ContractType.MINE_ORE;
         case SYLVAN -> ContractType.HARVEST;
         case BLACKSMITH -> ContractType.SMELT;
         case RANGER -> ContractType.KILL_RANGED;
         case ARCANIST -> ContractType.ENCHANT;
         case ENGINEER -> ContractType.BUILD_REDSTONE;
         default -> ContractType.KILL;
      };
   }

   private List<Part> generate(Guild guild, String day) {
      Random rng = new Random(guild.getId().hashCode() * 31L ^ day.hashCode());
      List<Race> jobs = new ArrayList<>(List.of(JOBS));
      Collections.shuffle(jobs, rng);
      double scale = 1.0 + 0.15 * Math.max(0, guild.getMembers().size() - 1);
      List<Part> parts = new ArrayList<>();
      for (int i = 0; i < PARTS; i++) {
         ContractType type = typeOf(jobs.get(i));
         parts.add(new Part(jobs.get(i), type, (int)Math.max(1L, Math.round(type.getMaxTarget() * 1.5 * scale))));
      }

      return parts;
   }

   public Day get(Guild guild) {
      String day = ContractManager.today();
      Day state = this.days.get(guild.getId());
      if (state == null || !state.day.equals(day)) {
         if (state != null && state.dirty) {
            this.persist(guild.getId(), state);
         }

         state = new Day(day, this.generate(guild, day));
         for (Object[] r : this.plugin.getDataManager().getStorage().select(
            "SELECT idx, progress, done FROM guild_contract WHERE guild_id = ? AND day = ?;", 3, guild.getId(), day)) {
            int idx = ((Number)r[0]).intValue();
            if (idx >= 0 && idx < PARTS) {
               state.progress[idx] = ((Number)r[1]).intValue();
               state.done[idx] = ((Number)r[2]).intValue() == 1;
            }
         }

         this.days.put(guild.getId(), state);
      }

      return state;
   }

   /** Appelé par les contrats individuels : la même action fait avancer l'objectif de guilde du métier du joueur. */
   public void addProgress(Player player, Race race, ContractType type, int amount) {
      Guild guild = this.plugin.getGuildManager().getGuildByPlayer(player.getUniqueId());
      if (guild == null || amount <= 0 || race == null || race == Race.NONE) {
         return;
      }

      Day state = this.get(guild);
      for (int i = 0; i < PARTS; i++) {
         Part part = state.parts.get(i);
         if (state.done[i] || part.race() != race || part.type() != type) {
            continue;
         }

         state.progress[i] = Math.min(part.target(), state.progress[i] + amount);
         state.contributors.add(player.getUniqueId());
         state.dirty = true;
         if (state.progress[i] >= part.target()) {
            this.completePart(guild, state, i);
         }
      }
   }

   private void completePart(Guild guild, Day state, int i) {
      state.done[i] = true;
      Part part = state.parts.get(i);
      double mult = this.plugin.getSynergy().contractMultiplier(guild);
      guild.depositBank(PART_REWARD * mult);
      ChatUtil.broadcastGuild(guild, "&a🧩 Contrat de guilde : objectif &f" + this.label(part) + " &aaccompli ! &6+" + Math.round(PART_REWARD * mult) + " pièces &7(banque)");
      if (state.allDone()) {
         guild.depositBank(ALL_REWARD * mult);
         ChatUtil.broadcastGuild(guild, "&6&l🧩 Contrat de guilde terminé ! &e+" + Math.round(ALL_REWARD * mult) + " pièces &7versées à la banque de guilde.");
         for (UUID uuid : state.contributors) {
            Player p = Bukkit.getPlayer(uuid);
            if (p != null) {
               ChatUtil.playSound(p, Sound.UI_TOAST_CHALLENGE_COMPLETE, 1.0F, 1.0F);
            }

            this.plugin.getAchievements().grant(uuid, Achievement.TEAMWORK);
         }

         this.plugin.getChronicle().log(Chronicle.Kind.CONTRACT, guild.getName() + " remplit son contrat de guilde du jour.", guild.getId(), null);
      }

      this.plugin.getDataManager().saveGuild(guild);
      this.persist(guild.getId(), state);
   }

   public String label(Part part) {
      return part.race().getDisplayName() + " &7: &f" + part.type().describe(part.target());
   }

   /** Lignes affichées dans /contrat. */
   public List<String> lines(Guild guild) {
      Day state = this.get(guild);
      Set<Race> jobs = this.plugin.getSynergy().jobs(guild);
      List<String> lines = new ArrayList<>();
      double mult = this.plugin.getSynergy().contractMultiplier(guild);
      lines.add("  &b&l🧩 CONTRAT DE GUILDE &7(" + Math.round(PART_REWARD * mult) + " par objectif, +" + Math.round(ALL_REWARD * mult) + " pour les trois, versés à la banque)");
      for (int i = 0; i < PARTS; i++) {
         Part part = state.parts.get(i);
         String status = state.done[i] ? "&a✔" : "&e" + state.progress[i] + "/" + part.target();
         String missing = !state.done[i] && !jobs.contains(part.race()) ? " &c(aucun membre de ce métier !)" : "";
         lines.add("   " + status + " &7" + this.label(part) + missing);
      }

      return lines;
   }

   private void persist(String guildId, Day state) {
      state.dirty = false;
      for (int i = 0; i < PARTS; i++) {
         this.plugin.getDataManager().getStorage().exec(
            "INSERT INTO guild_contract (guild_id, day, idx, progress, done) VALUES (?, ?, ?, ?, ?) "
               + "ON CONFLICT(guild_id, day, idx) DO UPDATE SET progress = excluded.progress, done = excluded.done;",
            guildId, state.day, i, state.progress[i], state.done[i] ? 1 : 0);
      }
   }

   public void flush() {
      for (Map.Entry<String, Day> e : this.days.entrySet()) {
         if (e.getValue().dirty) {
            this.persist(e.getKey(), e.getValue());
         }
      }

      this.plugin.getDataManager().getStorage().exec("DELETE FROM guild_contract WHERE day < ?;", java.time.LocalDate.now(java.time.ZoneId.of("Europe/Paris")).minusDays(3).toString());
   }

   public void clear() {
      this.days.clear();
      this.plugin.getDataManager().getStorage().exec("DELETE FROM guild_contract;");
   }
}
