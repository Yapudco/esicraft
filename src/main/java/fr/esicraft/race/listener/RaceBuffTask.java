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
   private static final PotionEffect DWARF_HASTE = new PotionEffect(PotionEffectType.HASTE, 220, 1, false, false, true);
   private static final PotionEffect DWARF_NIGHT_VISION = new PotionEffect(PotionEffectType.NIGHT_VISION, 240, 0, false, false, false);
   private static final PotionEffect DWARF_SLOWNESS = new PotionEffect(PotionEffectType.SLOWNESS, 60, 0, false, false, false);
   private static final PotionEffect RANGER_SPEED = new PotionEffect(PotionEffectType.SPEED, 220, 0, false, false, false);
   private static final PotionEffect RANGER_SLOWNESS = new PotionEffect(PotionEffectType.SLOWNESS, 60, 0, false, false, true);
   private static final PotionEffect BLACKSMITH_FIRE_RES = new PotionEffect(PotionEffectType.FIRE_RESISTANCE, 220, 0, false, false, false);
   private static final PotionEffect SYLVAN_REGEN = new PotionEffect(PotionEffectType.REGENERATION, 220, 0, false, false, true);
   private static final PotionEffect SPEC_WARLORD_STRENGTH = new PotionEffect(PotionEffectType.STRENGTH, 220, 0, false, false, true);
   private static final PotionEffect SPEC_WARLORD_RESIST = new PotionEffect(PotionEffectType.RESISTANCE, 220, 0, false, false, true);
   private static final PotionEffect SPEC_INDUSTRY_HASTE = new PotionEffect(PotionEffectType.HASTE, 220, 1, false, false, true);
   private static final PotionEffect SPEC_DRUID_REGEN = new PotionEffect(PotionEffectType.REGENERATION, 220, 0, false, false, true);
   private static final PotionEffect SPEC_DRUID_SATURATION = new PotionEffect(PotionEffectType.SATURATION, 220, 0, false, false, true);
   private static final PotionEffect AGE_SPEED = new PotionEffect(PotionEffectType.SPEED, 220, 0, false, false, true);
   private static final PotionEffect AGE_HASTE = new PotionEffect(PotionEffectType.HASTE, 220, 0, false, false, true);
   private static final PotionEffect AGE_RESIST = new PotionEffect(PotionEffectType.RESISTANCE, 220, 0, false, false, true);

   private final SpecializationRulesListener specializationRules;

   private int runs;
   /** Joueurs auxquels les bonus aquatiques des Atlantes ont été appliqués (pour les retirer hors de l'eau). */
   private final java.util.Set<java.util.UUID> atlanteans = java.util.concurrent.ConcurrentHashMap.newKeySet();

   private static boolean inWater(Player p) {
      return p.isInWater() || p.getEyeLocation().getBlock().getType() == Material.WATER;
   }

   private void clearAtlantean(Player p) {
      for (PotionEffectType t : new PotionEffectType[]{PotionEffectType.WATER_BREATHING, PotionEffectType.NIGHT_VISION, PotionEffectType.DOLPHINS_GRACE, PotionEffectType.RESISTANCE}) {
         p.removePotionEffect(t);
      }

      var attr = p.getAttribute(org.bukkit.attribute.Attribute.WATER_MOVEMENT_EFFICIENCY);
      if (attr != null) {
         attr.setBaseValue(0.0);
      }
   }

   /** Atlantes : tous les atouts sous-marins dans l'eau (respiration, vision nocturne, agilité aquatique, dauphin, résistance) et Lenteur sur la terre ferme. */
   private void atlantean(Player p, Guild guild) {
      this.atlanteans.add(p.getUniqueId());
      boolean wetNexus = guild.hasNexus() && fr.esicraft.nexus.NexusManager.isSubmerged(guild.getNexus().getLocation());
      var attr = p.getAttribute(org.bukkit.attribute.Attribute.WATER_MOVEMENT_EFFICIENCY);
      if (wetNexus && inWater(p)) {
         buff(p, new PotionEffect(PotionEffectType.WATER_BREATHING, 240, 0, false, false, false));
         buff(p, new PotionEffect(PotionEffectType.NIGHT_VISION, 260, 0, false, false, false));
         buff(p, new PotionEffect(PotionEffectType.DOLPHINS_GRACE, 240, 0, false, false, true));
         buff(p, new PotionEffect(PotionEffectType.RESISTANCE, 240, 0, false, false, true));
         p.removePotionEffect(PotionEffectType.SLOWNESS);
         if (attr != null) {
            attr.setBaseValue(1.0);
         }
      } else {
         this.clearAtlantean(p);
         buff(p, new PotionEffect(PotionEffectType.SLOWNESS, 60, 0, false, false, false));
      }
   }

   /**
    * Applique un effet seulement s'il manque ou s'il va s'épuiser : plus de paquet toutes les 2 secondes, et la durée dépasse 10 s,
    * seuil sous lequel le client fait clignoter l'icône de l'effet.
    */
   private static void buff(Player player, PotionEffect effect) {
      PotionEffect current = player.getPotionEffect(effect.getType());
      if (current != null && current.getAmplifier() >= effect.getAmplifier() && current.getDuration() > (effect.getDuration() > 100 ? 100 : 40)) {
         return;
      }

      player.addPotionEffect(effect);
   }

   public RaceBuffTask(EsicraftPlugin plugin, SpecializationRulesListener specializationRules) {
      this.plugin = plugin;
      this.specializationRules = specializationRules;
   }

   public void run() {
      // La restriction du Netherite est déjà appliquée par écouteur : ce filet de sécurité ne passe qu'une fois sur cinq.
      boolean enforce = ++this.runs % 5 == 0;
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
               if (enforce) {
                  this.specializationRules.enforceNoNetherite(player);
               }
               Race race = this.plugin.getRaceManager().getRace(player);
               loc = player.getLocation();
               y = loc.getBlockY();
               switch (race) {
                  case DWARF:
                     if (y <= 0) {
                        buff(player, DWARF_HASTE);
                        buff(player, DWARF_NIGHT_VISION);
                     } else if (y > 60 && loc.getWorld() != null && loc.getWorld().isClearWeather() && loc.getBlock().getLightFromSky() >= 14) {
                        buff(player, DWARF_SLOWNESS);
                     }
                     break label65;
                  case RANGER:
                     Biome biome = loc.getBlock().getBiome();
                     String biomeName = biome.name();
                     if (biomeName.contains("PLAINS") || biomeName.contains("FOREST") || biomeName.contains("JUNGLE") || biomeName.contains("TAIGA")) {
                        buff(player, RANGER_SPEED);
                     }

                     ItemStack chest = player.getInventory().getChestplate();
                     if (chest != null && (chest.getType() == Material.DIAMOND_CHESTPLATE || chest.getType() == Material.NETHERITE_CHESTPLATE)) {
                        buff(player, RANGER_SLOWNESS);
                     }
                     break label65;
                  case BLACKSMITH:
                     buff(player, BLACKSMITH_FIRE_RES);
                     break label65;
                  case SYLVAN:
                     buff(player, SYLVAN_REGEN);
                     if (loc.getWorld() != null) {
                        Guild sylvanGuild = this.plugin.getGuildManager().getGuildByPlayer(player.getUniqueId());
                        for (Player nearby : loc.getWorld().getNearbyPlayers(loc, 4.0)) {
                           if (nearby.equals(player)) continue;
                           Guild nearbyGuild = this.plugin.getGuildManager().getGuildByPlayer(nearby.getUniqueId());
                           boolean isAlly = sylvanGuild != null && nearbyGuild != null && sylvanGuild.getId().equals(nearbyGuild.getId());
                           if (isAlly) {
                              buff(nearby, SYLVAN_REGEN);
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
            buff(player, RANGER_SPEED);
         }

         if (playerGuild != null && playerGuild.getSpecialization() == GuildSpecialization.ATLANTEAN) {
            this.atlantean(player, playerGuild);
         } else if (this.atlanteans.remove(player.getUniqueId())) {
            this.clearAtlantean(player);
         }

         if (chunkGuild != null && chunkGuild.isMember(player.getUniqueId())) {
            GuildSpecialization spec = chunkGuild.getSpecialization();
            switch (spec) {
               case WARLORD:
                  buff(player, SPEC_WARLORD_STRENGTH);
                  buff(player, SPEC_WARLORD_RESIST);
                  break;
               case UNDERGROUND_INDUSTRY:
                  if (y <= 30) {
                     buff(player, SPEC_INDUSTRY_HASTE);
                  }
                  break;
               case DRUIDIC_ENCLAVE:
                  buff(player, SPEC_DRUID_REGEN);
                  buff(player, SPEC_DRUID_SATURATION);
            }
         }

         AgeType playerAge = this.plugin.getAgeManager().getPlayerAge(player);
         if (playerAge == AgeType.TOTAL_WAR) {
            buff(player, AGE_SPEED);
         } else if (playerAge == AgeType.MYTHIC || playerAge == AgeType.SUPREMACY) {
            buff(player, AGE_HASTE);
            buff(player, AGE_SPEED);
            buff(player, AGE_RESIST);
         }
      }
   }
}
