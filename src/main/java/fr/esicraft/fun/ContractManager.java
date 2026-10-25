package fr.esicraft.fun;

import fr.esicraft.EsicraftPlugin;
import fr.esicraft.data.PlayerData;
import fr.esicraft.race.Race;
import fr.esicraft.util.ChatUtil;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.HashSet;
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
 * Contrats quotidiens : 3 missions par joueur et par jour (une propre au métier, une de chasse, une de récolte/minage).
 * Les contrats sont générés de façon déterministe à partir de l'UUID et de la date : seule la progression est stockée.
 */
public class ContractManager {
   public static final int CONTRACTS_PER_DAY = 3;
   private static final double ALL_DONE_BONUS = 400.0;
   private static final ZoneId ZONE = ZoneId.of("Europe/Paris");

   public record Contract(ContractType type, int target, double reward) {
   }

   public static final class DayState {
      final String day;
      final List<Contract> contracts;
      final int[] progress = new int[CONTRACTS_PER_DAY];
      final boolean[] claimed = new boolean[CONTRACTS_PER_DAY];
      final Set<Long> countedPositions = new HashSet<>();
      boolean dirty;

      DayState(String day, List<Contract> contracts) {
         this.day = day;
         this.contracts = contracts;
      }

      public List<Contract> getContracts() {
         return this.contracts;
      }

      public int getProgress(int idx) {
         return this.progress[idx];
      }

      public boolean isClaimed(int idx) {
         return this.claimed[idx];
      }

      public int completedCount() {
         int n = 0;
         for (boolean c : this.claimed) {
            if (c) {
               n++;
            }
         }

         return n;
      }
   }

   private final EsicraftPlugin plugin;
   private final Map<UUID, DayState> states = new ConcurrentHashMap<>();

   public ContractManager(EsicraftPlugin plugin) {
      this.plugin = plugin;
      // Sauvegarde différée de la progression (toutes les 30 s) : évite une écriture SQL par bloc cassé.
      Bukkit.getScheduler().runTaskTimerAsynchronously(plugin, this::flush, 600L, 600L);
   }

   public static String today() {
      return LocalDate.now(ZONE).toString();
   }

   private static ContractType raceType(Race race) {
      return switch (race) {
         case DWARF -> ContractType.MINE_ORE;
         case SYLVAN -> ContractType.HARVEST;
         case BLACKSMITH -> ContractType.SMELT;
         case RANGER -> ContractType.KILL_RANGED;
         case ARCANIST -> ContractType.ENCHANT;
         case ENGINEER -> ContractType.BUILD_REDSTONE;
         default -> null;
      };
   }

   private List<Contract> generate(UUID uuid, Race race, String day) {
      Random rng = new Random(uuid.getMostSignificantBits() ^ uuid.getLeastSignificantBits() ^ ((long)day.hashCode() * 31L));
      List<ContractType> types = new ArrayList<>();
      ContractType own = raceType(race);
      types.add(own != null ? own : ContractType.MINE_ORE);
      types.add(ContractType.KILL);
      ContractType third = rng.nextBoolean() ? ContractType.MINE_ORE : ContractType.HARVEST;
      if (types.contains(third)) {
         third = third == ContractType.MINE_ORE ? ContractType.HARVEST : ContractType.MINE_ORE;
      }

      types.add(third);
      List<Contract> contracts = new ArrayList<>();
      for (ContractType type : types) {
         int span = type.getMaxTarget() - type.getMinTarget() + 1;
         int target = type.getMinTarget() + rng.nextInt(span);
         contracts.add(new Contract(type, target, Math.round(target * type.getPerUnit())));
      }

      return contracts;
   }

   public DayState getState(Player player) {
      String day = today();
      DayState state = this.states.get(player.getUniqueId());
      if (state == null || !state.day.equals(day)) {
         if (state != null && state.dirty) {
            this.persist(player.getUniqueId(), state);
         }

         state = new DayState(day, this.generate(player.getUniqueId(), this.plugin.getRaceManager().getRace(player), day));
         for (Object[] row : this.plugin.getDataManager().getStorage().select(
            "SELECT idx, progress, claimed FROM contract_progress WHERE uuid = ? AND day = ?;", 3, player.getUniqueId().toString(), day)) {
            int idx = ((Number)row[0]).intValue();
            if (idx >= 0 && idx < CONTRACTS_PER_DAY) {
               state.progress[idx] = ((Number)row[1]).intValue();
               state.claimed[idx] = ((Number)row[2]).intValue() == 1;
            }
         }

         this.states.put(player.getUniqueId(), state);
      }

      return state;
   }

   /** Ajoute de la progression à tous les contrats du joueur correspondant à ce type. */
   public void addProgress(Player player, ContractType type, int amount) {
      if (amount <= 0 || this.plugin.getRaceManager().getRace(player) == Race.NONE) {
         return;
      }

      this.plugin.getGuildContracts().addProgress(player, this.plugin.getRaceManager().getRace(player), type, amount);
      DayState state = this.getState(player);
      for (int i = 0; i < CONTRACTS_PER_DAY; i++) {
         Contract contract = state.contracts.get(i);
         if (contract.type() != type || state.claimed[i]) {
            continue;
         }

         state.progress[i] = Math.min(contract.target(), state.progress[i] + amount);
         state.dirty = true;
         if (state.progress[i] >= contract.target()) {
            this.complete(player, state, i);
         } else if (state.progress[i] % 10 == 0) {
            ChatUtil.sendActionbar(player, "&e📜 " + contract.type().describe(contract.target()) + " &7(" + state.progress[i] + "/" + contract.target() + ")");
         }
      }
   }

   /** Évite de compter plusieurs fois la même position (pose/ramassage en boucle). Renvoie vrai si la position est nouvelle. */
   public boolean markPositionOnce(Player player, long packedPosition) {
      return this.getState(player).countedPositions.add(packedPosition);
   }

   private void complete(Player player, DayState state, int idx) {
      state.claimed[idx] = true;
      Contract contract = state.contracts.get(idx);
      PlayerData data = this.plugin.getDataManager().getPlayerData(player.getUniqueId());
      if (data != null) {
         data.addCoins(contract.reward());
      }

      ChatUtil.sendMessage(player, "&a✔ Contrat accompli : &f" + contract.type().describe(contract.target()) + " &8» &6+" + String.format("%.0f", contract.reward()) + " pièces");
      ChatUtil.playSound(player, Sound.ENTITY_PLAYER_LEVELUP, 1.0F, 1.2F);
      this.plugin.getAchievements().addStat(player.getUniqueId(), fr.esicraft.life.AchievementManager.STAT_CONTRACTS, 1L);
      if (state.completedCount() == CONTRACTS_PER_DAY) {
         if (data != null) {
            data.addCoins(ALL_DONE_BONUS);
         }

         ChatUtil.sendMessage(player, "&6&l★ Tous les contrats du jour sont accomplis ! &e+" + String.format("%.0f", ALL_DONE_BONUS) + " pièces de bonus.");
         this.plugin.getAchievements().grant(player.getUniqueId(), fr.esicraft.life.Achievement.CONTRACTOR);
         ChatUtil.playSound(player, Sound.UI_TOAST_CHALLENGE_COMPLETE, 1.0F, 1.0F);
      }

      this.persist(player.getUniqueId(), state);
   }

   private void persist(UUID uuid, DayState state) {
      state.dirty = false;
      for (int i = 0; i < CONTRACTS_PER_DAY; i++) {
         this.plugin.getDataManager().getStorage().exec(
            "INSERT INTO contract_progress (uuid, day, idx, progress, claimed) VALUES (?, ?, ?, ?, ?) "
               + "ON CONFLICT(uuid, day, idx) DO UPDATE SET progress = excluded.progress, claimed = excluded.claimed;",
            uuid.toString(), state.day, i, state.progress[i], state.claimed[i] ? 1 : 0);
      }
   }

   /** Écrit les états modifiés (appelable depuis un thread asynchrone ou à l'arrêt). */
   public void flush() {
      for (Map.Entry<UUID, DayState> entry : this.states.entrySet()) {
         if (entry.getValue().dirty) {
            this.persist(entry.getKey(), entry.getValue());
         }
      }

      // Purge des anciens contrats (plus de 3 jours) pour garder la base légère.
      this.plugin.getDataManager().getStorage().exec("DELETE FROM contract_progress WHERE day < ?;", LocalDate.now(ZONE).minusDays(3).toString());
   }

   public void onQuit(UUID uuid) {
      DayState state = this.states.remove(uuid);
      if (state != null && state.dirty) {
         this.persist(uuid, state);
      }
   }
}
