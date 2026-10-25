package fr.esicraft.race.listener;

import fr.esicraft.EsicraftPlugin;
import fr.esicraft.age.AgeType;
import fr.esicraft.guild.Guild;
import fr.esicraft.guild.GuildSpecialization;
import fr.esicraft.guild.listener.SpecializationRulesListener;
import fr.esicraft.race.Race;
import fr.esicraft.util.ChunkKey;
import java.util.Iterator;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Biome;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitRunnable;

public class RaceBuffTask extends BukkitRunnable {
   private final EsicraftPlugin plugin;
   private static final PotionEffect DWARF_HASTE = new PotionEffect(PotionEffectType.FAST_DIGGING, 60, 1, false, false, true);
   private static final PotionEffect DWARF_NIGHT_VISION = new PotionEffect(PotionEffectType.NIGHT_VISION, 240, 0, false, false, false);
   private static final PotionEffect DWARF_SLOWNESS = new PotionEffect(PotionEffectType.SLOW, 60, 0, false, false, false);
   private static final PotionEffect RANGER_SPEED = new PotionEffect(PotionEffectType.SPEED, 60, 0, false, false, false);
   private static final PotionEffect RANGER_SLOWNESS = new PotionEffect(PotionEffectType.SLOW, 60, 0, false, false, true);
   private static final PotionEffect BLACKSMITH_FIRE_RES = new PotionEffect(PotionEffectType.FIRE_RESISTANCE, 100, 0, false, false, false);
   private static final PotionEffect SYLVAN_REGEN = new PotionEffect(PotionEffectType.REGENERATION, 60, 0, false, false, true);
   private static final PotionEffect SPEC_WARLORD_STRENGTH = new PotionEffect(PotionEffectType.INCREASE_DAMAGE, 60, 0, false, false, true);
   private static final PotionEffect SPEC_WARLORD_RESIST = new PotionEffect(PotionEffectType.DAMAGE_RESISTANCE, 60, 0, false, false, true);
   private static final PotionEffect SPEC_INDUSTRY_HASTE = new PotionEffect(PotionEffectType.FAST_DIGGING, 60, 1, false, false, true);
   private static final PotionEffect SPEC_DRUID_REGEN = new PotionEffect(PotionEffectType.REGENERATION, 60, 0, false, false, true);
   private static final PotionEffect SPEC_DRUID_SATURATION = new PotionEffect(PotionEffectType.SATURATION, 60, 0, false, false, true);
   private static final PotionEffect AGE_SPEED = new PotionEffect(PotionEffectType.SPEED, 60, 0, false, false, true);
   private static final PotionEffect AGE_HASTE = new PotionEffect(PotionEffectType.FAST_DIGGING, 60, 0, false, false, true);
   private static final PotionEffect AGE_RESIST = new PotionEffect(PotionEffectType.DAMAGE_RESISTANCE, 60, 0, false, false, true);

   private final SpecializationRulesListener specializationRules;

   public RaceBuffTask(EsicraftPlugin plugin, SpecializationRulesListener specializationRules) {
      this.plugin = plugin;
      this.specializationRules = specializationRules;
   }

   public void run() {
      Iterator var1 = Bukkit.getOnlinePlayers().iterator();

      while (true) {
         Player player;
         Location loc;
         int y;
         label65:
         while (true) {
            if (!var1.hasNext()) {
               return;
            }

            player = (Player)var1.next();
            if (player.isValid() && player.getLocation().getWorld() != null) {
               this.specializationRules.enforceNoNetherite(player);
               Race race = this.plugin.getRaceManager().getRace(player);
               loc = player.getLocation();
               y = loc.getBlockY();
               switch (race) {
                  case DWARF:
                     if (y <= 0) {
                        player.addPotionEffect(DWARF_HASTE);
                        player.addPotionEffect(DWARF_NIGHT_VISION);
                     } else if (y > 60 && loc.getWorld() != null && loc.getWorld().isClearWeather() && loc.getBlock().getLightFromSky() >= 14) {
                        player.addPotionEffect(DWARF_SLOWNESS);
                     }
                     break label65;
                  case RANGER:
                     Biome biome = loc.getBlock().getBiome();
                     String biomeName = biome.name();
                     if (biomeName.contains("PLAINS") || biomeName.contains("FOREST") || biomeName.contains("JUNGLE") || biomeName.contains("TAIGA")) {
                        player.addPotionEffect(RANGER_SPEED);
                     }

                     ItemStack chest = player.getInventory().getChestplate();
                     if (chest != null && (chest.getType() == Material.DIAMOND_CHESTPLATE || chest.getType() == Material.NETHERITE_CHESTPLATE)) {
                        player.addPotionEffect(RANGER_SLOWNESS);
                     }
                     break label65;
                  case BLACKSMITH:
                     player.addPotionEffect(BLACKSMITH_FIRE_RES);
                     break label65;
                  case SYLVAN:
                     player.addPotionEffect(SYLVAN_REGEN);
                     if (loc.getWorld() != null) {
                        Guild sylvanGuild = this.plugin.getGuildManager().getGuildByPlayer(player.getUniqueId());
                        for (Player nearby : loc.getWorld().getNearbyPlayers(loc, 4.0)) {
                           if (nearby.equals(player)) continue;
                           Guild nearbyGuild = this.plugin.getGuildManager().getGuildByPlayer(nearby.getUniqueId());
                           boolean isAlly = sylvanGuild != null && nearbyGuild != null && sylvanGuild.getId().equals(nearbyGuild.getId());
                           if (isAlly) {
                              nearby.addPotionEffect(SYLVAN_REGEN);
                           }
                        }
                     }
                     break label65;
                  case ENGINEER:
                  default:
                     break label65;
               }
            }
         }

         ChunkKey currentChunk = new ChunkKey(loc.getWorld().getName(), loc.getBlockX() >> 4, loc.getBlockZ() >> 4);
         Guild chunkGuild = this.plugin.getGuildManager().getGuildByChunk(currentChunk);
         Guild playerGuild = this.plugin.getGuildManager().getGuildByPlayer(player.getUniqueId());
         if (playerGuild != null && playerGuild.getSpecialization() == GuildSpecialization.LONE_RANGER) {
            player.addPotionEffect(RANGER_SPEED);
         }

         if (chunkGuild != null && chunkGuild.isMember(player.getUniqueId())) {
            GuildSpecialization spec = chunkGuild.getSpecialization();
            switch (spec) {
               case WARLORD:
                  player.addPotionEffect(SPEC_WARLORD_STRENGTH);
                  player.addPotionEffect(SPEC_WARLORD_RESIST);
                  break;
               case UNDERGROUND_INDUSTRY:
                  if (y <= 30) {
                     player.addPotionEffect(SPEC_INDUSTRY_HASTE);
                  }
                  break;
               case DRUIDIC_ENCLAVE:
                  player.addPotionEffect(SPEC_DRUID_REGEN);
                  player.addPotionEffect(SPEC_DRUID_SATURATION);
            }
         }

         AgeType playerAge = this.plugin.getAgeManager().getPlayerAge(player);
         if (playerAge == AgeType.TOTAL_WAR) {
            player.addPotionEffect(AGE_SPEED);
         } else if (playerAge == AgeType.MYTHIC || playerAge == AgeType.SUPREMACY) {
            player.addPotionEffect(AGE_HASTE);
            player.addPotionEffect(AGE_SPEED);
            player.addPotionEffect(AGE_RESIST);
         }
      }
   }
}
