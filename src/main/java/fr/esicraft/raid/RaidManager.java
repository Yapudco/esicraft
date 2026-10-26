package fr.esicraft.raid;

import fr.esicraft.EsicraftPlugin;
import fr.esicraft.gui.CustomHordeGUI;
import fr.esicraft.guild.Guild;
import fr.esicraft.guild.GuildRole;
import fr.esicraft.guild.GuildSpecialization;
import fr.esicraft.util.ChatUtil;
import fr.esicraft.util.Lang;
import fr.esicraft.util.ChunkKey;
import java.time.Duration;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.Map.Entry;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ThreadLocalRandom;
import net.kyori.adventure.title.Title;
import net.kyori.adventure.title.Title.Times;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.entity.Creeper;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Mob;
import org.bukkit.entity.Pillager;
import org.bukkit.entity.Player;
import org.bukkit.entity.Projectile;
import org.bukkit.entity.Skeleton;
import org.bukkit.entity.Vindicator;
import org.bukkit.entity.WitherSkeleton;
import org.bukkit.entity.Zombie;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.event.entity.EntityExplodeEvent;
import org.bukkit.event.entity.EntityTargetLivingEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent.DamageCause;
import org.bukkit.inventory.ItemStack;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitRunnable;

public class RaidManager implements Listener {
   private final EsicraftPlugin plugin;
   private final Map<String, ActiveRaid> activeRaids = new ConcurrentHashMap<>();
   private final Set<String> guildIdsUnderRaid = ConcurrentHashMap.newKeySet();
   private final Map<UUID, ActiveRaid> mobRaidIndex = new ConcurrentHashMap<>();
   private final Map<String, Long> guildRaidCooldowns = new ConcurrentHashMap<>();
   private final Map<UUID, Location> lastMobPositions = new ConcurrentHashMap<>();
   private final Map<UUID, Integer> mobStuckCounters = new ConcurrentHashMap<>();
   private final Map<UUID, Location> mobLastKnown = new ConcurrentHashMap<>();
   /** « attaquant>cible|jour » → victoires créditées au classement : les auto-raids entre complices ne rapportent qu'un nombre limité de points. */
   private final Map<String, Integer> repelCredits = new ConcurrentHashMap<>();
   private final Map<UUID, Integer> mobMisses = new ConcurrentHashMap<>();

   public RaidManager(EsicraftPlugin plugin) {
      this.plugin = plugin;
      this.startRaidAITask();
   }

   // Après un redémarrage, les raids (en mémoire) n'existent plus : on supprime les mobs de raid orphelins restés dans le monde.
   @EventHandler
   public void onEntitiesLoad(org.bukkit.event.world.EntitiesLoadEvent event) {
      for (Entity entity : event.getEntities()) {
         if (entity.getScoreboardTags().contains("esicraft_raid_mob") && !this.mobRaidIndex.containsKey(entity.getUniqueId())) {
            entity.remove();
         }
      }
   }

   private boolean adminLaunch = false;

   /** Raid forcé par un administrateur : gratuit, sans guilde attaquante. */
   public boolean launchAdminRaid(Guild targetGuild, RaidType raidType) {
      this.adminLaunch = true;

      boolean var3;
      try {
         var3 = this.launchRaid(null, targetGuild, raidType, false, false, null);
      } finally {
         this.adminLaunch = false;
      }

      return var3;
   }

   public boolean isGuildUnderRaid(String guildId) {
      return guildId != null && this.guildIdsUnderRaid.contains(guildId);
   }

   private boolean isDefenderInClaim(Guild guild) {
      if (guild == null) {
         return false;
      } else {
         for (UUID memberUuid : guild.getMembers().keySet()) {
            Player member = Bukkit.getPlayer(memberUuid);
            if (member != null && member.isOnline()) {
               ChunkKey pChunk = ChunkKey.from(member.getLocation().getChunk());
               if (guild.isClaimed(pChunk)) {
                  return true;
               }
            }
         }

         return false;
      }
   }

   public boolean launchRaid(Guild attackerGuild, Guild targetGuild, RaidType raidType, Player buyer) {
      return this.launchRaid(attackerGuild, targetGuild, raidType, false, false, buyer);
   }

   public boolean launchRaid(Guild attackerGuild, Guild targetGuild, RaidType raidType, boolean explosiveBreachers, Player buyer) {
      return this.launchRaid(attackerGuild, targetGuild, raidType, explosiveBreachers, false, buyer);
   }

   /** Siège de Pillards : 1 Ravageur sur 6, 1 Vindicateur sur 3, le reste en Pillards. */
   private EntityType spawnTypeFor(RaidType raidType, int index) {
      if (raidType == RaidType.PILLAGER_SIEGE) {
         if (index % 6 == 0) {
            return EntityType.RAVAGER;
         }

         if (index % 3 == 1) {
            return EntityType.VINDICATOR;
         }
      }

      return raidType.getPrimaryMobType();
   }

   public boolean launchCustomRaid(Guild attackerGuild, Guild targetGuild, Map<CustomHordeUnit, Integer> composition, Player buyer) {
      return this.launchCustomRaid(attackerGuild, targetGuild, composition, false, false, buyer);
   }

   public boolean launchCustomRaid(Guild attackerGuild, Guild targetGuild, Map<CustomHordeUnit, Integer> composition, boolean explosiveBreachers, Player buyer) {
      return this.launchCustomRaid(attackerGuild, targetGuild, composition, explosiveBreachers, false, buyer);
   }

   public boolean launchRaid(Guild attackerGuild, Guild targetGuild, RaidType raidType, boolean explosiveBreachers, boolean lavaImmunity, Player buyer) {
      if (targetGuild != null && targetGuild.hasNexus() && targetGuild.getNexus().isAlive()) {
         if (!this.plugin.isNexusDestructionEnabled()) {
            if (buyer != null) {
               ChatUtil.sendMessage(buyer, "&cLes raids et la destruction de Nexus sont temporairement désactivés (/pvp).");
            }

            return false;
         } else if (!this.adminLaunch && this.plugin.getTruce().isTruce() && !this.plugin.bypassCooldowns(buyer)) {
            if (buyer != null) {
               ChatUtil.sendMessage(buyer, "&a🕊 Trêve : aucune horde ne peut être lancée pour l'instant.");
            }

            return false;
         } else if (!this.isDefenderInClaim(targetGuild)) {
            if (buyer != null) {
               ChatUtil.sendMessage(buyer, "&c❌ Impossible de lancer cette horde : aucun membre de la guilde ciblée n'est présent sur son territoire claimé !");
               ChatUtil.playSound(buyer, Sound.ENTITY_VILLAGER_NO, 1.0F, 1.0F);
            }

            return false;
         } else if (this.plugin.getTreatyManager().blocksRaid(attackerGuild, targetGuild, buyer)) {
            return false;
         } else if (this.isGuildUnderRaid(targetGuild.getId())) {
            if (buyer != null) {
               ChatUtil.sendMessage(buyer, "&cUne horde de monstres attaque déjà le Nexus de cette guilde !");
            }

            return false;
         } else {
            boolean isHegemony = this.plugin.getGuildManager().isEconomicHegemony(targetGuild);
            long now = System.currentTimeMillis();
            long cooldownMs = isHegemony ? 120000L : (long)(this.plugin.getConfig().getInt("raids.cooldown-minutes-between-raids", 30) * 60) * 1000L;
            Long lastRaid = this.guildRaidCooldowns.get(targetGuild.getId());
            if (lastRaid != null && now - lastRaid < cooldownMs && !this.plugin.bypassCooldowns(buyer)) {
               long remainingMs = cooldownMs - (now - lastRaid);
               long remainingMin = remainingMs / 60000L;
               long remainingSec = remainingMs % 60000L / 1000L;
               String timeStr = remainingMin > 0L ? remainingMin + " min " + remainingSec + " s" : remainingSec + " secondes";
               if (buyer != null) {
                  ChatUtil.sendMessage(buyer, "&cCette guilde a récemment subi un raid. Protégée encore &e" + timeStr + "&c.");
               }

               return false;
            } else if (attackerGuild == null && !this.adminLaunch) {
               if (buyer != null) {
                  ChatUtil.sendMessage(buyer, "&c❌ Vous devez faire partie d'une guilde pour lancer une horde de mercenaires !");
                  ChatUtil.playSound(buyer, Sound.ENTITY_VILLAGER_NO, 1.0F, 1.0F);
               }

               return false;
            } else if (attackerGuild != null && attackerGuild.getId().equals(targetGuild.getId())) {
               if (buyer != null) {
                  ChatUtil.sendMessage(buyer, "&c❌ Vous ne pouvez pas lancer de raid mercenaire contre votre propre guilde !");
                  ChatUtil.playSound(buyer, Sound.ENTITY_VILLAGER_NO, 1.0F, 1.0F);
               }

               return false;
            } else if (buyer != null && attackerGuild != null && !attackerGuild.hasRoleAtLeast(buyer.getUniqueId(), GuildRole.OFFICER)) {
               ChatUtil.sendMessage(buyer, "&c❌ Seuls les Officiers et le Chef de guilde peuvent lancer des hordes de mercenaires !");
               ChatUtil.playSound(buyer, Sound.ENTITY_VILLAGER_NO, 1.0F, 1.0F);
               return false;
            } else {
               GuildSpecialization spec = attackerGuild != null ? attackerGuild.getSpecialization() : GuildSpecialization.NONE;
               double specMultiplier = spec == GuildSpecialization.WARLORD ? 0.7 : (spec == GuildSpecialization.MERCHANT_SYNDICATE ? 1.3 : 1.0);
               double price = raidType.getBasePrice() * specMultiplier;
               if (explosiveBreachers) {
                  price += 2500.0 * specMultiplier;
               }

               if (lavaImmunity) {
                  price += 1500.0 * specMultiplier;
               }

               if (isHegemony) {
                  price *= 0.5;
               }

               price *= this.plugin.getBalance().raidPriceFactor(attackerGuild, targetGuild);
               if (attackerGuild == null) {
                  price = 0.0;
               }

               if (attackerGuild != null && attackerGuild.getBankBalance() < price) {
                  if (buyer != null) {
                     ChatUtil.sendMessage(
                        buyer,
                        "&c❌ La banque de votre guilde ne possède pas assez de fonds (&e"
                           + String.format("%.2f", price)
                           + " pièces &crequises, Solde : &6"
                           + String.format("%.2f", attackerGuild.getBankBalance())
                           + "&c)."
                     );
                     ChatUtil.sendMessage(buyer, "&7Déposez de l'argent dans la banque de guilde avec &e/guild bank <montant>&7.");
                     ChatUtil.playSound(buyer, Sound.ENTITY_VILLAGER_NO, 1.0F, 1.0F);
                  }

                  return false;
               } else {
                  Location preCheckLoc = targetGuild.getNexus().getLocation();
                  if (preCheckLoc == null || preCheckLoc.getWorld() == null) {
                     if (buyer != null) {
                        ChatUtil.sendMessage(buyer, "&c❌ Le monde du Nexus ciblé est introuvable : aucun fonds n'a été prélevé.");
                     }

                     return false;
                  }

                  if (attackerGuild != null) {
                     attackerGuild.withdrawBank(price);
                     this.plugin.getDataManager().saveGuild(attackerGuild);
                  }

                  if (buyer != null) {
                     String discountTag = isHegemony ? " &6[Cible Hégémonique : -50%]" : "";
                     ChatUtil.sendMessage(
                        buyer,
                        "&a✔ &e" + String.format("%.2f", price) + " pièces &aont été prélevées de la banque de guilde pour financer l'invasion !" + discountTag
                     );
                  }

                  this.setCooldown(targetGuild.getId(), now);
                  Location nexusLoc = targetGuild.getNexus().getLocation();
                  World world = nexusLoc.getWorld();
                  if (world == null) {
                     return false;
                  } else {
                     String raidId = UUID.randomUUID().toString();
                     String attackerName = attackerGuild != null ? attackerGuild.getName() : "Administration";
                     String attackerGuildId = attackerGuild != null ? attackerGuild.getId() : null;
                     ActiveRaid raid = new ActiveRaid(
                        raidId, attackerGuildId, attackerName, targetGuild.getId(), raidType, explosiveBreachers, lavaImmunity, nexusLoc
                     );
                     this.activeRaids.put(raidId, raid);
                     this.guildIdsUnderRaid.add(targetGuild.getId());
                     int count = raidType.getMobCount();

                     for (int i = 0; i < count; i++) {
                        double angle = (Math.PI * 2) / (double)count * (double)i;
                        double distance = ThreadLocalRandom.current().nextDouble(28.0, 42.0);
                        double x = nexusLoc.getX() + distance * Math.cos(angle);
                        double z = nexusLoc.getZ() + distance * Math.sin(angle);
                        Location spawnLoc = this.safeSpawn(world, nexusLoc, x, z);
                        EntityType spawnType = this.spawnTypeFor(raidType, i);
                        Entity entity = world.spawnEntity(spawnLoc, spawnType);
                        if (entity instanceof LivingEntity) {
                           LivingEntity living = (LivingEntity)entity;
                           String tag = "";
                           if (explosiveBreachers) {
                              tag = tag + " &c[Sapeur]";
                           }

                           if (lavaImmunity) {
                              tag = tag + " &6[Ignifugé]";
                           }

                           if (tag.isEmpty()) {
                              tag = " &c[Attaquant]";
                           }

                           living.customName(ChatUtil.component(raidType.getDisplayName() + tag));
                           living.setCustomNameVisible(true);
                           living.setRemoveWhenFarAway(false);
                           living.addScoreboardTag("esicraft_raid_mob");
                           this.equipMob(living, spawnType);
                           if (lavaImmunity) {
                              living.addPotionEffect(new PotionEffect(PotionEffectType.FIRE_RESISTANCE, 24000, 1, false, false));
                           }

                           raid.addMob(living.getUniqueId(), raidType.getDamageToNexus());
                           this.mobRaidIndex.put(living.getUniqueId(), raid);
                        }
                     }

                     ChatUtil.broadcast("&4&l⚔️ UNE HORDE DE MONSTRES A ÉTÉ LANCEE SUR LA GUILDE &c" + targetGuild.getName() + " &4&l!");
                     this.onRaidLaunched(attackerGuild, targetGuild, buyer, fr.esicraft.util.ChatUtil.color(raidType.getDisplayName()));
                     String details = "&7Type d'invasion : " + raidType.getDisplayName() + " &7par &e" + attackerName;
                     if (explosiveBreachers) {
                        details = details + " &c&l[SAPEURS EXPLOSIFS]";
                     }

                     if (lavaImmunity) {
                        details = details + " &6&l[RÉSISTANCE LAVE]";
                     }

                     ChatUtil.broadcast(details);
                     if (isHegemony) {
                        ChatUtil.broadcast(
                           "&6&l⭐ PRIME HÉGÉMONIE FINANCIÈRE : &eLa guilde &c"
                              + targetGuild.getName()
                              + " &ea été ciblée pour sa richesse (Coût divisé par 2 & Cooldown de 2 min) !"
                        );
                     }

                     this.notifyDefenders(targetGuild);
                     return true;
                  }
               }
            }
         }
      } else {
         if (buyer != null) {
            ChatUtil.sendMessage(buyer, "&cCette guilde n'a pas de Nexus actif à attaquer !");
         }

         return false;
      }
   }

   public boolean launchCustomRaid(
      Guild attackerGuild, Guild targetGuild, Map<CustomHordeUnit, Integer> composition, boolean explosiveBreachers, boolean lavaImmunity, Player buyer
   ) {
      if (attackerGuild == null) {
         if (buyer != null) {
            ChatUtil.sendMessage(buyer, "&c❌ Vous devez faire partie d'une guilde pour lancer une horde de mercenaires !");
            ChatUtil.playSound(buyer, Sound.ENTITY_VILLAGER_NO, 1.0F, 1.0F);
         }

         return false;
      } else if (targetGuild != null && attackerGuild.getId().equals(targetGuild.getId())) {
         if (buyer != null) {
            ChatUtil.sendMessage(buyer, "&c❌ Vous ne pouvez pas lancer de raid mercenaire contre votre propre guilde !");
            ChatUtil.playSound(buyer, Sound.ENTITY_VILLAGER_NO, 1.0F, 1.0F);
         }

         return false;
      } else if (buyer != null && !attackerGuild.hasRoleAtLeast(buyer.getUniqueId(), GuildRole.OFFICER)) {
         ChatUtil.sendMessage(buyer, "&c❌ Seuls les Officiers et le Chef de guilde peuvent lancer des hordes de mercenaires !");
         ChatUtil.playSound(buyer, Sound.ENTITY_VILLAGER_NO, 1.0F, 1.0F);
         return false;
      } else if (targetGuild != null && targetGuild.hasNexus() && targetGuild.getNexus().isAlive()) {
         if (!this.plugin.isNexusDestructionEnabled()) {
            if (buyer != null) {
               ChatUtil.sendMessage(buyer, "&cLes raids et la destruction de Nexus sont temporairement désactivés (/pvp).");
            }

            return false;
         } else if (composition != null && !composition.isEmpty() && composition.values().stream().mapToInt(Integer::intValue).sum() != 0) {
            int maxUnits = this.plugin.getConfig().getInt("raids.max-custom-horde-units", 60);
            long unitsTotal = 0L;
            boolean invalidAmount = false;

            for (int amt : composition.values()) {
               if (amt < 0) {
                  invalidAmount = true;
               }

               unitsTotal += (long)amt;
            }

            if (invalidAmount || unitsTotal > (long)maxUnits) {
               if (buyer != null) {
                  ChatUtil.sendMessage(buyer, "&c❌ Une horde personnalisée ne peut pas dépasser &e" + maxUnits + " &cmonstres au total (et les quantités doivent être positives).");
                  ChatUtil.playSound(buyer, Sound.ENTITY_VILLAGER_NO, 1.0F, 1.0F);
               }

               return false;
            }

            if (!this.isDefenderInClaim(targetGuild)) {
               if (buyer != null) {
                  ChatUtil.sendMessage(
                     buyer, "&c❌ Impossible de lancer cette horde : aucun membre de la guilde ciblée n'est présent sur son territoire claimé !"
                  );
                  ChatUtil.playSound(buyer, Sound.ENTITY_VILLAGER_NO, 1.0F, 1.0F);
               }

               return false;
            } else if (this.plugin.getTreatyManager().blocksRaid(attackerGuild, targetGuild, buyer)) {
            return false;
         } else if (this.isGuildUnderRaid(targetGuild.getId())) {
               if (buyer != null) {
                  ChatUtil.sendMessage(buyer, "&cUne horde de monstres attaque déjà le Nexus de cette guilde !");
               }

               return false;
            } else {
               boolean isHegemony = this.plugin.getGuildManager().isEconomicHegemony(targetGuild);
               long now = System.currentTimeMillis();
               long cooldownMs = isHegemony ? 120000L : (long)(this.plugin.getConfig().getInt("raids.cooldown-minutes-between-raids", 30) * 60) * 1000L;
               Long lastRaid = this.guildRaidCooldowns.get(targetGuild.getId());
               if (lastRaid != null && now - lastRaid < cooldownMs && !this.plugin.bypassCooldowns(buyer)) {
                  long remainingMs = cooldownMs - (now - lastRaid);
                  long remainingMin = remainingMs / 60000L;
                  long remainingSec = remainingMs % 60000L / 1000L;
                  String timeStr = remainingMin > 0L ? remainingMin + " min " + remainingSec + " s" : remainingSec + " secondes";
                  if (buyer != null) {
                     ChatUtil.sendMessage(buyer, "&cCette guilde a récemment subi un raid. Protégée encore &e" + timeStr + "&c.");
                  }

                  return false;
               } else {
                  GuildSpecialization spec = attackerGuild.getSpecialization();
                  double totalPrice = CustomHordeGUI.getTotalCost(composition, spec, explosiveBreachers, lavaImmunity);
                  if (isHegemony) {
                     totalPrice *= 0.5;
                  }

                  totalPrice *= this.plugin.getBalance().raidPriceFactor(attackerGuild, targetGuild);

                  if (attackerGuild.getBankBalance() < totalPrice) {
                     if (buyer != null) {
                        ChatUtil.sendMessage(
                           buyer,
                           "&c❌ La banque de votre guilde ne possède pas assez de fonds (&e"
                              + String.format("%.2f", totalPrice)
                              + " pièces &crequises, Solde : &6"
                              + String.format("%.2f", attackerGuild.getBankBalance())
                              + "&c)."
                        );
                        ChatUtil.sendMessage(buyer, "&7Déposez de l'argent dans la banque de guilde avec &e/guild bank <montant>&7.");
                        ChatUtil.playSound(buyer, Sound.ENTITY_VILLAGER_NO, 1.0F, 1.0F);
                     }

                     return false;
                  } else {
                     Location preCheckLoc = targetGuild.getNexus().getLocation();
                     if (preCheckLoc == null || preCheckLoc.getWorld() == null) {
                        if (buyer != null) {
                           ChatUtil.sendMessage(buyer, "&c❌ Le monde du Nexus ciblé est introuvable : aucun fonds n'a été prélevé.");
                        }

                        return false;
                     }

                     attackerGuild.withdrawBank(totalPrice);
                     this.plugin.getDataManager().saveGuild(attackerGuild);
                     if (buyer != null) {
                        String discountTag = isHegemony ? " &6[Cible Hégémonique : -50%]" : "";
                        ChatUtil.sendMessage(
                           buyer,
                           "&a✔ &e"
                              + String.format("%.2f", totalPrice)
                              + " pièces &aont été prélevées de la banque de guilde pour financer l'invasion personnalisée !"
                              + discountTag
                        );
                     }

                     this.setCooldown(targetGuild.getId(), now);
                     Location nexusLoc = targetGuild.getNexus().getLocation();
                     World world = nexusLoc.getWorld();
                     if (world == null) {
                        return false;
                     } else {
                        String raidId = UUID.randomUUID().toString();
                        String attackerName = attackerGuild.getName();
                        String attackerGuildId = attackerGuild.getId();
                        ActiveRaid raid = new ActiveRaid(
                           raidId,
                           attackerGuildId,
                           attackerName,
                           targetGuild.getId(),
                           null,
                           "&4⚔️ Horde Personnalisée Sur-Mesure",
                           explosiveBreachers,
                           lavaImmunity,
                           nexusLoc
                        );
                        this.activeRaids.put(raidId, raid);
                        this.guildIdsUnderRaid.add(targetGuild.getId());
                        int totalUnits = composition.values().stream().mapToInt(Integer::intValue).sum();
                        int currentUnitIndex = 0;

                        for (Entry<CustomHordeUnit, Integer> entry : composition.entrySet()) {
                           CustomHordeUnit unit = entry.getKey();
                           int amount = entry.getValue();

                           for (int i = 0; i < amount; i++) {
                              double angle = (Math.PI * 2) / (double)Math.max(1, totalUnits) * (double)currentUnitIndex;
                              double distance = ThreadLocalRandom.current().nextDouble(28.0, 42.0);
                              double x = nexusLoc.getX() + distance * Math.cos(angle);
                              double z = nexusLoc.getZ() + distance * Math.sin(angle);
                              Location spawnLoc = this.safeSpawn(world, nexusLoc, x, z);
                              if (world.spawnEntity(spawnLoc, unit.getEntityType()) instanceof LivingEntity living) {
                                 String tag = "";
                                 if (explosiveBreachers) {
                                    tag = tag + " &c[Sapeur]";
                                 }

                                 if (lavaImmunity) {
                                    tag = tag + " &6[Ignifugé]";
                                 }

                                 if (tag.isEmpty()) {
                                    tag = " &c[Attaquant]";
                                 }

                                 living.customName(ChatUtil.component(unit.getDisplayName() + tag));
                                 living.setCustomNameVisible(true);
                                 living.setRemoveWhenFarAway(false);
                           living.addScoreboardTag("esicraft_raid_mob");
                                 this.equipMob(living, unit.getEntityType());
                                 if (lavaImmunity) {
                                    living.addPotionEffect(new PotionEffect(PotionEffectType.FIRE_RESISTANCE, 24000, 1, false, false));
                                 }

                                 raid.addMob(living.getUniqueId(), unit.getDamageToNexus());
                                 this.mobRaidIndex.put(living.getUniqueId(), raid);
                              }

                              currentUnitIndex++;
                           }
                        }

                        ChatUtil.broadcast("&4&l⚔️ UNE HORDE PERSONNALISÉE SUR-MESURE A ÉTÉ LANCEE SUR LA GUILDE &c" + targetGuild.getName() + " &4&l!");
                        this.onRaidLaunched(attackerGuild, targetGuild, buyer, "horde personnalisée");
                        String details = "&7Commanditaire : &e" + attackerName + " &7| Total : &c" + totalUnits + " monstres d'élite";
                        if (explosiveBreachers) {
                           details = details + " &c&l[SAPEURS EXPLOSIFS]";
                        }

                        if (lavaImmunity) {
                           details = details + " &6&l[RÉSISTANCE LAVE]";
                        }

                        ChatUtil.broadcast(details);
                        if (isHegemony) {
                           ChatUtil.broadcast(
                              "&6&l⭐ PRIME HÉGÉMONIE FINANCIÈRE : &eLa guilde &c"
                                 + targetGuild.getName()
                                 + " &ea été ciblée pour sa richesse (Coût divisé par 2 & Cooldown de 2 min) !"
                           );
                        }

                        this.notifyDefenders(targetGuild);
                        return true;
                     }
                  }
               }
            }
         } else {
            if (buyer != null) {
               ChatUtil.sendMessage(buyer, "&cVotre composition de horde est vide.");
            }

            return false;
         }
      } else {
         if (buyer != null) {
            ChatUtil.sendMessage(buyer, "&cCette guilde n'a pas de Nexus actif à attaquer !");
         }

         return false;
      }
   }

   /** Journal de guerre, rivalités et succès au lancement d'une horde. */
   private void onRaidLaunched(Guild attackerGuild, Guild targetGuild, Player buyer, String hordeName) {
      String attacker = attackerGuild != null ? attackerGuild.getName() : "L'administration";
      this.plugin.getChronicle().log(fr.esicraft.life.Chronicle.Kind.RAID, attacker + " lance une horde (" + hordeName.replaceAll("§.", "") + ") sur " + targetGuild.getName() + ".",
         attackerGuild != null ? attackerGuild.getId() : null, targetGuild.getId());
      this.plugin.getChronicle().raid(attackerGuild != null ? attackerGuild.getId() : null, targetGuild.getId(), "launched");
      this.plugin.getBetting().openRaid(attackerGuild, targetGuild);
      if (buyer != null) {
         this.plugin.getAchievements().grant(buyer.getUniqueId(), fr.esicraft.life.Achievement.RAIDER);
         this.plugin.getAchievements().addStat(buyer.getUniqueId(), fr.esicraft.life.AchievementManager.STAT_RAIDS, 1L);
      }
   }

   /** Raids en cours (lecture seule), pour le site web. */
   public ActiveRaid raidAgainst(String targetGuildId) {
      for (ActiveRaid r : this.activeRaids.values()) {
         if (r.getTargetGuildId().equals(targetGuildId)) {
            return r;
         }
      }

      return null;
   }

   public java.util.Collection<ActiveRaid> getActiveRaids() {
      return java.util.List.copyOf(this.activeRaids.values());
   }

   private void notifyDefenders(Guild targetGuild) {
      for (Player p : Bukkit.getOnlinePlayers()) {
         if (targetGuild.isMember(p.getUniqueId())) {
            p.showTitle(
               Title.title(
                  ChatUtil.component(Lang.tr(p, "&4&lINVASION ENNEMIE !")),
                  ChatUtil.component(Lang.tr(p, "&cUne horde attaque votre Nexus !")),
                  Times.times(Duration.ofMillis(500L), Duration.ofMillis(4000L), Duration.ofMillis(1000L))
               )
            );
            ChatUtil.playSound(p, Sound.EVENT_RAID_HORN, 2.0F, 1.0F);
         }
      }
   }

   private void equipMob(LivingEntity living, EntityType type) {
      if (living instanceof Zombie zombie) {
         zombie.getEquipment().setHelmet(new ItemStack(Material.IRON_HELMET));
         zombie.getEquipment().setChestplate(new ItemStack(Material.IRON_CHESTPLATE));
         zombie.getEquipment().setItemInMainHand(new ItemStack(Material.IRON_SWORD));
      } else if (living instanceof Skeleton skeleton) {
         skeleton.getEquipment().setHelmet(new ItemStack(Material.CHAINMAIL_HELMET));
         skeleton.getEquipment().setItemInMainHand(new ItemStack(Material.BOW));
      } else if (living instanceof Pillager pillager) {
         pillager.getEquipment().setItemInMainHand(new ItemStack(Material.CROSSBOW));
      } else if (living instanceof Vindicator vindicator) {
         vindicator.getEquipment().setItemInMainHand(new ItemStack(Material.IRON_AXE));
      } else if (living instanceof WitherSkeleton witherSkeleton) {
         witherSkeleton.getEquipment().setItemInMainHand(new ItemStack(Material.STONE_SWORD));
      } else if (living instanceof Creeper creeper) {
         creeper.addPotionEffect(new PotionEffect(PotionEffectType.SPEED, 2400, 1));
      }
   }

   private void startRaidAITask() {
      (new BukkitRunnable() {
            private int aiTick = 0;

            public void run() {
               this.aiTick++;
               long maxRaidMs = RaidManager.this.plugin.getConfig().getLong("raids.max-minutes", 25L) * 60000L;
               for (ActiveRaid raid : RaidManager.this.activeRaids.values()) {
                  if (System.currentTimeMillis() - raid.getStartTime() > maxRaidMs) {
                     // Un raid ne dure pas indéfiniment (mob piégé) : il s'achève sans victoire ni défaite, la guilde n'est plus « en raid ».
                     RaidManager.this.finishRaid(raid, false);
                     continue;
                  }

                  if (raid.isFinished()) {
                     RaidManager.this.finishRaid(raid, true);
                  } else {
                     Guild targetGuild = RaidManager.this.plugin.getGuildManager().getGuildById(raid.getTargetGuildId());
                     if (targetGuild != null && targetGuild.hasNexus()) {
                        Location nexusLoc = targetGuild.getNexus().getLocation();

                        for (UUID mobUuid : raid.getAliveMobUuids()) {
                           if (Bukkit.getEntity(mobUuid) == null) {
                              // Chunk déchargé (défenseurs partis) : on recharge au lieu de compter le mob comme mort ; au bout de 6 échecs, il est « évaporé » (pas de victoire offerte).
                              Location known = RaidManager.this.mobLastKnown.get(mobUuid);
                              int misses = RaidManager.this.mobMisses.merge(mobUuid, 1, Integer::sum);
                              if (known != null && known.getWorld() != null && misses <= 6) {
                                 known.getWorld().getChunkAtAsync(known);
                                 continue;
                              }

                              raid.markVanished();
                           }

                           if (Bukkit.getEntity(mobUuid) instanceof Mob mob && !mob.isDead()) {
                              RaidManager.this.mobMisses.remove(mobUuid);
                              RaidManager.this.mobLastKnown.put(mobUuid, mob.getLocation());
                              // Chemin recalculé toutes les 6 s, ou tout de suite si le mob n'en a plus (jusqu'à 60 mobs : le calcul de chemin est coûteux).
                              if (this.aiTick % 3 == 0 || mob.getPathfinder().getCurrentPath() == null) {
                                 mob.getPathfinder().moveTo(nexusLoc, 1.25);
                              }
                              if (raid.hasLavaImmunity() && mob.getLocation().getBlock().getType() == Material.LAVA) {
                                 mob.setVelocity(mob.getVelocity().setY(0.12));
                              }

                              if (raid.hasExplosiveBreachers()) {
                                 Location lastLoc = RaidManager.this.lastMobPositions.get(mobUuid);
                                 Location currentLoc = mob.getLocation();
                                 if (lastLoc != null && lastLoc.getWorld() != null && lastLoc.getWorld().equals(currentLoc.getWorld())) {
                                    if (lastLoc.distanceSquared(currentLoc) < 0.49) {
                                       int stuck = RaidManager.this.mobStuckCounters.getOrDefault(mobUuid, 0) + 1;
                                       RaidManager.this.mobStuckCounters.put(mobUuid, stuck);
                                       if (stuck >= 3) {
                                          currentLoc.getWorld().createExplosion(currentLoc, 3.0F, false, true);
                                          RaidManager.this.cleanFluidsAround(currentLoc, 5);
                                          currentLoc.getWorld().spawnParticle(Particle.EXPLOSION, currentLoc.clone().add(0.0, 1.0, 0.0), 4);
                                          currentLoc.getWorld().playSound(currentLoc, Sound.ENTITY_GENERIC_EXPLODE, 2.0F, 0.8F);
                                          mob.remove();
                                          raid.removeMob(mobUuid);
                                          RaidManager.this.mobRaidIndex.remove(mobUuid);
                                          RaidManager.this.mobStuckCounters.remove(mobUuid);
                                          RaidManager.this.lastMobPositions.remove(mobUuid);
                                          ChatUtil.broadcastGuild(targetGuild, 
                                             "&c\ud83d\udca3 &lSAPEUR DÉTONATEUR : &7Un monstre bloqué a explosé pour ouvrir une brèche et vaporiser l'eau/lave !"
                                          );
                                          continue;
                                       }
                                    } else {
                                       RaidManager.this.mobStuckCounters.put(mobUuid, 0);
                                    }
                                 }

                                 RaidManager.this.lastMobPositions.put(mobUuid, currentLoc);
                              }

                              if (mob.getLocation().distanceSquared(nexusLoc) <= 12.25) {
                                 int damage = raid.getMobDamage(mobUuid);
                                 RaidManager.this.plugin.getNexusManager().damageNexus(targetGuild, damage, raid.getAttackerGuildName(), RaidManager.this.plugin.getGuildManager().getGuildById(raid.getAttackerGuildId()), null);
                                 mob.getWorld().spawnParticle(Particle.SWEEP_ATTACK, nexusLoc.clone().add(0.5, 0.5, 0.5), 5);
                                 mob.remove();
                                 raid.removeMob(mobUuid);
                                 RaidManager.this.mobRaidIndex.remove(mobUuid);
                                 RaidManager.this.mobStuckCounters.remove(mobUuid);
                                 RaidManager.this.lastMobPositions.remove(mobUuid);
                              }
                              continue;
                           }

                           raid.removeMob(mobUuid);
                           RaidManager.this.mobRaidIndex.remove(mobUuid);
                           RaidManager.this.mobStuckCounters.remove(mobUuid);
                           RaidManager.this.lastMobPositions.remove(mobUuid);
                        }

                        if (raid.isFinished()) {
                           RaidManager.this.finishRaid(raid, true);
                        }
                     } else {
                        RaidManager.this.plugin.getBalance().recordRaidLoss(targetGuild, RaidManager.this.plugin.getGuildManager().getGuildById(raid.getAttackerGuildId()));
                        RaidManager.this.finishRaid(raid, false);
                     }
                  }
               }
            }
         })
         .runTaskTimer(this.plugin, 40L, 40L);
   }

   /** Point d'apparition sur un sol solide ; sinon (vide, îlot, End) à côté du Nexus, jamais dans le vide. */
   private Location safeSpawn(World world, Location nexusLoc, double x, double z) {
      int y = world.getHighestBlockYAt((int)x, (int)z);
      Material ground = world.getBlockAt((int)Math.floor(x), y, (int)Math.floor(z)).getType();
      if (y > world.getMinHeight() && ground.isSolid() && !ground.name().contains("LEAVES")) {
         return new Location(world, x, (double)(y + 1), z);
      }

      return nexusLoc.clone().add(ThreadLocalRandom.current().nextDouble(-3.0, 3.0), 2.0, ThreadLocalRandom.current().nextDouble(-3.0, 3.0));
   }

   public void cleanFluidsAround(Location center, int radius) {
      if (center != null && center.getWorld() != null) {
         World world = center.getWorld();
         int cx = center.getBlockX();
         int cy = center.getBlockY();
         int cz = center.getBlockZ();
         int removed = 0;

         for (int x = -radius; x <= radius; x++) {
            for (int y = -radius; y <= radius; y++) {
               for (int z = -radius; z <= radius; z++) {
                  if (x * x + y * y + z * z <= radius * radius) {
                     Block block = world.getBlockAt(cx + x, cy + y, cz + z);
                     Material mat = block.getType();
                     if (mat == Material.WATER || mat == Material.LAVA || mat == Material.BUBBLE_COLUMN) {
                        block.setType(Material.AIR);
                        removed++;
                     }
                  }
               }
            }
         }

         if (removed > 0) {
            world.spawnParticle(Particle.LARGE_SMOKE, center.clone().add(0.0, 1.0, 0.0), 25, 1.0, 1.0, 1.0, 0.05);
            world.playSound(center, Sound.BLOCK_LAVA_EXTINGUISH, 1.5F, 0.8F);
         }
      }
   }

   @EventHandler(
      priority = EventPriority.HIGH,
      ignoreCancelled = true
   )
   public void onRaidEntityExplode(EntityExplodeEvent event) {
      if (event.getEntity() != null && this.mobRaidIndex.containsKey(event.getEntity().getUniqueId())) {
         this.cleanFluidsAround(event.getLocation(), 5);
      }
   }

   @EventHandler(
      priority = EventPriority.HIGH,
      ignoreCancelled = true
   )
   public void onLavaOrFireDamage(EntityDamageEvent event) {
      if (event.getEntity() instanceof LivingEntity living) {
         ActiveRaid raid = this.mobRaidIndex.get(living.getUniqueId());
         if (raid != null && raid.hasLavaImmunity()) {
            DamageCause cause = event.getCause();
            if (cause == DamageCause.LAVA || cause == DamageCause.FIRE || cause == DamageCause.FIRE_TICK || cause == DamageCause.HOT_FLOOR) {
               event.setCancelled(true);
               living.setFireTicks(0);
            }
         }
      }
   }

   @EventHandler(
      priority = EventPriority.HIGH
   )
   public void onEntityTarget(EntityTargetLivingEntityEvent event) {
      if (event.getEntity() instanceof Mob mob) {
         ActiveRaid raid = this.mobRaidIndex.get(mob.getUniqueId());
         if (raid != null) {
            if (event.getTarget() instanceof Player targetPlayer) {
               String attackerGuildId = raid.getAttackerGuildId();
               if (attackerGuildId != null) {
                  Guild attackerGuild = this.plugin.getGuildManager().getGuildById(attackerGuildId);
                  if (attackerGuild != null && attackerGuild.isMember(targetPlayer.getUniqueId())) {
                     event.setCancelled(true);
                  }
               }
            }
         }
      }
   }

   @EventHandler(
      priority = EventPriority.HIGH
   )
   public void onEntityDamage(EntityDamageByEntityEvent event) {
      Entity damager = event.getDamager();
      if (damager instanceof Projectile projectile && projectile.getShooter() instanceof Entity shooter) {
         damager = shooter;
      }

      ActiveRaid raid = this.mobRaidIndex.get(damager.getUniqueId());
      if (raid != null) {
         if (event.getEntity() instanceof Player victimPlayer) {
            String attackerGuildId = raid.getAttackerGuildId();
            if (attackerGuildId != null) {
               Guild attackerGuild = this.plugin.getGuildManager().getGuildById(attackerGuildId);
               if (attackerGuild != null && attackerGuild.isMember(victimPlayer.getUniqueId())) {
                  event.setCancelled(true);
               }
            }
         }
      }
   }

   @EventHandler
   public void onMobDeath(EntityDeathEvent event) {
      ActiveRaid raid = this.mobRaidIndex.remove(event.getEntity().getUniqueId());
      if (raid != null) {
         raid.removeMob(event.getEntity().getUniqueId());
         this.mobStuckCounters.remove(event.getEntity().getUniqueId());
         this.lastMobPositions.remove(event.getEntity().getUniqueId());
         if (raid.isFinished()) {
            this.finishRaid(raid, true);
         }
      }
   }

   private void finishRaid(ActiveRaid raid, boolean defended) {
      // Pas de victoire si une bonne part de la horde s'est volatilisée sans combat (chunk déchargé, vide, mob piégé).
      defended = defended && raid != null && raid.getVanished() * 4 <= Math.max(1, raid.getTotalMobs());
      if (raid != null) {
         this.plugin.getBetting().settleRaid(raid.getTargetGuildId(), defended);
      }

      if (raid != null) {
         this.activeRaids.remove(raid.getId());
         this.guildIdsUnderRaid.remove(raid.getTargetGuildId());

         for (UUID mobUuid : raid.getAliveMobUuids()) {
            Entity entity = Bukkit.getEntity(mobUuid);
            if (entity != null && !entity.isDead()) {
               entity.remove();
            }

            this.mobRaidIndex.remove(mobUuid);
            this.mobStuckCounters.remove(mobUuid);
            this.lastMobPositions.remove(mobUuid);
         }

         Guild targetGuild = this.plugin.getGuildManager().getGuildById(raid.getTargetGuildId());
         if (targetGuild != null && targetGuild.hasNexus()) {
            // Les défenses de l'arbre technologique sont permanentes : elles ne sont plus consommées par la bataille.
            this.plugin.getDataManager().saveGuild(targetGuild);
            if (defended) {
               String pairKey = raid.getAttackerGuildId() + ">" + targetGuild.getId() + "|" + java.time.LocalDate.now().toEpochDay();
               int credited = this.repelCredits.merge(pairKey, 1, Integer::sum);
               // Une horde minuscule (complice qui lance un zombie) ne rapporte ni points de saison, ni XP de Nexus, ni succès.
               boolean worthy = raid.getTotalMobs() >= this.plugin.getConfig().getInt("raids.min-mobs-for-rewards", 12)
                  && credited <= this.plugin.getConfig().getInt("raids.max-credited-repels-per-pair-per-day", 2);
               if (worthy) {
                  this.plugin.getSeasonManager().recordRepelled(targetGuild);
                  this.plugin.getNexusTech().onRaidRepelled(targetGuild);
               }

               if (this.repelCredits.size() > 500) {
                  this.repelCredits.keySet().removeIf(k -> !k.endsWith("|" + java.time.LocalDate.now().toEpochDay()));
               }

               this.plugin.getChronicle().raid(raid.getAttackerGuildId(), targetGuild.getId(), "repelled");
               this.plugin.getHelpCalls().onRepelled(targetGuild);
               for (UUID memberUuid : targetGuild.getMembers().keySet()) {
                  Player defender = Bukkit.getPlayer(memberUuid);
                  if (worthy && defender != null && targetGuild.isClaimed(ChunkKey.from(defender.getLocation().getChunk()))) {
                     this.plugin.getAchievements().grant(memberUuid, fr.esicraft.life.Achievement.DEFENDER);
                     this.plugin.getAchievements().addStat(memberUuid, fr.esicraft.life.AchievementManager.STAT_DEFENSES, 1L);
                  }
               }

               ChatUtil.broadcast("&a&l\ud83d\udee1️ La guilde &e" + targetGuild.getName() + " &aa repoussé avec succès l'invasion de monstres !");
            }

            for (UUID memberUuid : targetGuild.getMembers().keySet()) {
               Player p = Bukkit.getPlayer(memberUuid);
               if (p != null && p.isOnline()) {
                  ChatUtil.playSound(p, Sound.UI_TOAST_CHALLENGE_COMPLETE, 1.0F, 1.0F);
               }
            }
         }
      }
   }

   /** Temps d'immunité post-raid restant (ms) pour cette guilde, 0 si elle peut être attaquée. */
   public long getCooldownRemainingMs(Guild guild) {
      Long lastRaid = this.guildRaidCooldowns.get(guild.getId());
      if (lastRaid == null) {
         return 0L;
      }

      long cooldownMs = this.plugin.getGuildManager().isEconomicHegemony(guild)
         ? 120000L
         : (long)(this.plugin.getConfig().getInt("raids.cooldown-minutes-between-raids", 30) * 60) * 1000L;
      return Math.max(0L, cooldownMs - (System.currentTimeMillis() - lastRaid));
   }

   /** Charge les cooldowns sauvegardés (appelé après l'ouverture de la base) : l'immunité post-raid survit aux redémarrages. */
   public void loadCooldowns() {
      this.guildRaidCooldowns.putAll(this.plugin.getDataManager().getStorage().loadRaidCooldowns());
   }

   private void setCooldown(String guildId, long timestamp) {
      this.guildRaidCooldowns.put(guildId, timestamp);
      org.bukkit.Bukkit.getScheduler().runTaskAsynchronously(this.plugin, () -> this.plugin.getDataManager().getStorage().saveRaidCooldown(guildId, timestamp));
   }

   public void resetAllCooldowns() {
      this.guildRaidCooldowns.clear();
      org.bukkit.Bukkit.getScheduler().runTaskAsynchronously(this.plugin, () -> this.plugin.getDataManager().getStorage().clearRaidCooldowns());
   }

   public void resetCooldown(String guildId) {
      if (guildId != null) {
         this.guildRaidCooldowns.remove(guildId);
         org.bukkit.Bukkit.getScheduler().runTaskAsynchronously(this.plugin, () -> this.plugin.getDataManager().getStorage().deleteRaidCooldown(guildId));
      }
   }

   public void stopAllRaids() {
      for (ActiveRaid raid : this.activeRaids.values()) {
         this.plugin.getBetting().cancelRaid(raid.getTargetGuildId());
         this.finishRaid(raid, false);
      }

      this.activeRaids.clear();
      this.guildIdsUnderRaid.clear();
   }

   public boolean stopRaid(String guildId) {
      boolean stopped = false;
      Iterator<Entry<String, ActiveRaid>> it = this.activeRaids.entrySet().iterator();

      while (it.hasNext()) {
         ActiveRaid raid = it.next().getValue();
         if (raid.getTargetGuildId().equalsIgnoreCase(guildId) || raid.getAttackerGuildId() != null && raid.getAttackerGuildId().equalsIgnoreCase(guildId)) {
            this.plugin.getBetting().cancelRaid(raid.getTargetGuildId());
            this.finishRaid(raid, false);
            it.remove();
            stopped = true;
         }
      }

      this.resetCooldown(guildId);
      return stopped;
   }
}
