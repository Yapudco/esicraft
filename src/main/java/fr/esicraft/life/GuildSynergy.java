package fr.esicraft.life;

import fr.esicraft.EsicraftPlugin;
import fr.esicraft.data.PlayerData;
import fr.esicraft.guild.Guild;
import fr.esicraft.race.Race;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Bonus de diversité : plus une guilde réunit de métiers différents, plus elle est efficace.
 * <ul>
 *   <li>3 métiers : primes de monstres +10 %</li>
 *   <li>4 métiers : nouvelles terres 10 % moins chères</li>
 *   <li>5 métiers : pièges du Nexus 15 % moins chers</li>
 *   <li>6 métiers : récompense des contrats de guilde ×1,5</li>
 * </ul>
 * Les métiers des membres hors ligne viennent d'un cache chargé depuis la base puis tenu à jour.
 */
public class GuildSynergy {
   public static final int TIER_BOUNTY = 3;
   public static final int TIER_CLAIMS = 4;
   public static final int TIER_TRAPS = 5;
   public static final int TIER_CONTRACTS = 6;

   private final EsicraftPlugin plugin;
   private final Map<UUID, Race> raceCache = new ConcurrentHashMap<>();

   public GuildSynergy(EsicraftPlugin plugin) {
      this.plugin = plugin;
   }

   public void init() {
      for (Object[] r : this.plugin.getDataManager().getStorage().select("SELECT uuid, race FROM players WHERE guild_id IS NOT NULL;", 2)) {
         try {
            this.raceCache.put(UUID.fromString((String)r[0]), Race.valueOf((String)r[1]));
         } catch (RuntimeException ignored) {
         }
      }
   }

   public Race raceOf(UUID uuid) {
      PlayerData data = this.plugin.getDataManager().getPlayerData(uuid);
      if (data != null && data.getRace() != null) {
         this.raceCache.put(uuid, data.getRace());
         return data.getRace();
      }

      return this.raceCache.getOrDefault(uuid, Race.NONE);
   }

   public Set<Race> jobs(Guild guild) {
      Set<Race> set = EnumSet.noneOf(Race.class);
      if (guild != null) {
         for (UUID member : guild.getMembers().keySet()) {
            Race r = this.raceOf(member);
            if (r != null && r != Race.NONE) {
               set.add(r);
            }
         }
      }

      return set;
   }

   public int diversity(Guild guild) {
      return this.jobs(guild).size();
   }

   public double bountyMultiplier(Guild guild) {
      return guild != null && this.diversity(guild) >= TIER_BOUNTY ? 1.1 : 1.0;
   }

   public double claimCostMultiplier(Guild guild) {
      return guild != null && this.diversity(guild) >= TIER_CLAIMS ? 0.9 : 1.0;
   }

   public double trapCostMultiplier(Guild guild) {
      return guild != null && this.diversity(guild) >= TIER_TRAPS ? 0.85 : 1.0;
   }

   public double contractMultiplier(Guild guild) {
      return guild != null && this.diversity(guild) >= TIER_CONTRACTS ? 1.5 : 1.0;
   }

   /** Lignes affichées dans /guild info et sur le site. */
   public List<String> describe(Guild guild) {
      int d = this.diversity(guild);
      List<String> lines = new ArrayList<>();
      lines.add("&7Diversité : &e" + d + "/6 métiers");
      lines.add((d >= TIER_BOUNTY ? "&a✔" : "&8✖") + " &73 métiers : primes de monstres +10 %");
      lines.add((d >= TIER_CLAIMS ? "&a✔" : "&8✖") + " &74 métiers : terres -10 %");
      lines.add((d >= TIER_TRAPS ? "&a✔" : "&8✖") + " &75 métiers : pièges du Nexus -15 %");
      lines.add((d >= TIER_CONTRACTS ? "&a✔" : "&8✖") + " &76 métiers : contrats de guilde ×1,5");
      return lines;
   }
}
