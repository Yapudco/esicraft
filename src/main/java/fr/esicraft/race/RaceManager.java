package fr.esicraft.race;

import fr.esicraft.EsicraftPlugin;
import fr.esicraft.data.PlayerData;
import fr.esicraft.guild.Guild;
import fr.esicraft.guild.GuildSpecialization;
import fr.esicraft.util.ChatUtil;
import java.util.EnumMap;
import java.util.Map;
import java.util.UUID;
import org.bukkit.Sound;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeInstance;
import org.bukkit.entity.Player;

public class RaceManager {
   private final EsicraftPlugin plugin;

   public RaceManager(EsicraftPlugin plugin) {
      this.plugin = plugin;
   }

   public Race getRace(Player player) {
      if (player == null) {
         return Race.NONE;
      } else {
         PlayerData data = this.plugin.getDataManager().getPlayerData(player.getUniqueId());
         return data != null ? data.getRace() : Race.NONE;
      }
   }

   public Race getRace(UUID uuid) {
      PlayerData data = this.plugin.getDataManager().getPlayerData(uuid);
      return data != null ? data.getRace() : Race.NONE;
   }

   public boolean setRace(Player player, Race newRace) {
      return this.setRace(player, newRace, false);
   }

   public boolean setRace(Player player, Race newRace, boolean forcedByAdmin) {
      if (player != null && newRace != null) {
         PlayerData data = this.plugin.getDataManager().getPlayerData(player.getUniqueId());
         if (data == null) {
            return false;
         } else {
            Race previousRace = data.getRace();
            if (previousRace == newRace) {
               ChatUtil.sendMessage(player, "&cVous appartenez déjà à la classe &e" + newRace.getDisplayName() + " &c!");
               return false;
            } else {
               Guild guild = this.plugin.getGuildManager().getGuildByPlayer(player.getUniqueId());
               if (!forcedByAdmin && guild != null && guild.getSpecialization() != GuildSpecialization.NONE) {
                  Map<Race, Integer> simulatedCounts = new EnumMap<>(Race.class);

                  for (UUID memberUuid : guild.getMembers().keySet()) {
                     Race r = memberUuid.equals(player.getUniqueId()) ? newRace : this.getRace(memberUuid);
                     simulatedCounts.put(r, simulatedCounts.getOrDefault(r, 0) + 1);
                  }

                  if (!guild.getSpecialization().validateComposition(simulatedCounts, guild.getMembers().size())) {
                     if (guild.getSpecialization() == GuildSpecialization.MERCHANT_SYNDICATE && newRace == Race.ARCANIST) {
                        ChatUtil.sendMessage(player, "&c❌ Votre guilde (Syndicat Marchand) interdit formellement les Arcanistes dans ses rangs !");
                     } else {
                        ChatUtil.sendMessage(
                           player, "&c❌ Votre guilde (" + guild.getSpecialization().getDisplayName() + "&c) n'autorise pas cette composition de métier !"
                        );
                     }

                     ChatUtil.playSound(player, Sound.ENTITY_VILLAGER_NO, 1.0F, 1.0F);
                     return false;
                  }
               }

               double changeCost = 10000.0;
               if (!forcedByAdmin) {
                  long cooldownMs = this.plugin.getConfig().getLong("races.change-cooldown-hours", 24L) * 3600000L;
                  long sinceLast = System.currentTimeMillis() - data.getLastRaceChangeTimestamp();
                  if (previousRace != Race.NONE && data.getLastRaceChangeTimestamp() > 0L && sinceLast < cooldownMs && !this.plugin.bypassCooldowns(player)) {
                     long remainingMin = (cooldownMs - sinceLast + 59999L) / 60000L;
                     ChatUtil.sendMessage(
                        player, "&c❌ Reconversion impossible pour le moment : prochain changement de métier dans &e" + remainingMin / 60L + "h " + remainingMin % 60L + "min&c."
                     );
                     ChatUtil.playSound(player, Sound.ENTITY_VILLAGER_NO, 1.0F, 1.0F);
                     return false;
                  }

                  if (previousRace != Race.NONE) {
                     int currentChanges = data.getRaceChangesCount();
                     if (currentChanges < 3) {
                        data.incrementRaceChangesCount();
                        int remainingFree = 3 - data.getRaceChangesCount();
                        ChatUtil.sendMessage(
                           player, "&a✨ Reconversion de métier gratuite appliquée ! (&e" + remainingFree + " &achangement(s) gratuit(s) restant(s))."
                        );
                     } else {
                        if (data.getCoins() < changeCost) {
                           ChatUtil.sendMessage(player, "&cSolde insuffisant pour changer de métier !");
                           ChatUtil.sendMessage(
                              player,
                              "&cVous avez utilisé vos &e3 changements gratuits&c. La reconversion coûte désormais &6"
                                 + String.format("%.0f", changeCost)
                                 + " pièces &c(Votre solde : &e"
                                 + String.format("%.2f", data.getCoins())
                                 + "&c)."
                           );
                           ChatUtil.playSound(player, Sound.ENTITY_VILLAGER_NO, 1.0F, 1.0F);
                           return false;
                        }

                        if (this.plugin.getCombatManager().denySpending(player)) {
                           return false;
                        }

                        data.removeCoins(changeCost);
                        this.plugin.getDataManager().savePlayer(data);
                        data.incrementRaceChangesCount();
                        ChatUtil.sendMessage(player, "&6" + String.format("%.0f", changeCost) + " pièces &aont été débitées pour votre reconversion.");
                     }
                  } else {
                     ChatUtil.sendMessage(player, "&a✨ Première sélection de métier gratuite ! (Vous disposez de 3 changements gratuits supplémentaires).");
                  }
               }

               data.setRace(newRace);
               this.plugin.getDataManager().savePlayer(data);
               this.applyRaceAttributes(player, newRace);
               ChatUtil.sendMessage(player, "&aVous êtes désormais : &e" + newRace.getDisplayName() + " &a!");
               this.plugin.getAchievements().grant(player.getUniqueId(), fr.esicraft.life.Achievement.FIRST_JOB);
               ChatUtil.playSound(player, Sound.UI_TOAST_CHALLENGE_COMPLETE, 1.0F, 1.2F);
               return true;
            }
         }
      } else {
         return false;
      }
   }

   public void applyRaceAttributes(Player player, Race race) {
      AttributeInstance maxHealth = player.getAttribute(Attribute.MAX_HEALTH);
      if (maxHealth != null) {
         double baseHealth = race != null ? race.getBaseHealth() : 20.0;
         Guild guild = this.plugin.getGuildManager().getGuildByPlayer(player.getUniqueId());
         if (guild != null) {
            GuildSpecialization spec = guild.getSpecialization();
            if (spec == GuildSpecialization.ARCANE_CONCLAVE) {
               baseHealth -= 4.0;
            } else if (spec == GuildSpecialization.DRUIDIC_ENCLAVE) {
               baseHealth += 4.0;
            }
         }

         double targetHealth = Math.max(2.0, baseHealth);
         maxHealth.setBaseValue(targetHealth);
         if (player.getHealth() > targetHealth) {
            player.setHealth(targetHealth);
         }
      }
   }
}
