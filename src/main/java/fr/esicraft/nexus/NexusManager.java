package fr.esicraft.nexus;

import fr.esicraft.EsicraftPlugin;
import fr.esicraft.guild.Guild;
import fr.esicraft.guild.GuildSpecialization;
import fr.esicraft.util.ChatUtil;
import fr.esicraft.util.ChunkKey;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ThreadLocalRandom;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.data.BlockData;
import org.bukkit.block.data.Waterlogged;
import org.bukkit.entity.ArmorStand;
import org.bukkit.entity.Damageable;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Item;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Monster;
import org.bukkit.entity.Player;
import org.bukkit.entity.Wolf;
import org.bukkit.inventory.ItemStack;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitRunnable;

public class NexusManager {
   private final EsicraftPlugin plugin;
   private final Map<String, ArmorStand> holograms = new ConcurrentHashMap<>();
   private final Map<String, Guild> nexusLocationIndex = new ConcurrentHashMap<>();
   private final Set<String> activeNexusGuilds = ConcurrentHashMap.newKeySet();
   /** Chunk du Nexus → guilde (recherche en O(1), sans charger de chunk). */
   private final Map<ChunkKey, String> nexusChunks = new ConcurrentHashMap<>();

   public NexusManager(EsicraftPlugin plugin) {
      this.plugin = plugin;
      this.startNexusTask();
   }

   public static String locKey(Location loc) {
      return loc != null && loc.getWorld() != null ? loc.getWorld().getName() + ":" + loc.getBlockX() + ":" + loc.getBlockY() + ":" + loc.getBlockZ() : null;
   }

   public void indexGuildNexus(Guild guild) {
      if (guild != null && guild.hasNexus() && guild.getNexus().getLocation() != null) {
         String key = locKey(guild.getNexus().getLocation());
         if (key != null) {
            this.nexusLocationIndex.put(key, guild);
            this.activeNexusGuilds.add(guild.getId());
            Location nl = guild.getNexus().getLocation();
            this.nexusChunks.put(new ChunkKey(nl.getWorld().getName(), nl.getBlockX() >> 4, nl.getBlockZ() >> 4), guild.getId());
         }
      }
   }

   public void unindexGuildNexus(Guild guild) {
      if (guild != null) {
         this.activeNexusGuilds.remove(guild.getId());
         if (guild.hasNexus() && guild.getNexus().getLocation() != null) {
            String key = locKey(guild.getNexus().getLocation());
            if (key != null) {
               this.nexusLocationIndex.remove(key);
            }

            Location nl = guild.getNexus().getLocation();
            if (nl.getWorld() != null) {
               this.nexusChunks.remove(new ChunkKey(nl.getWorld().getName(), nl.getBlockX() >> 4, nl.getBlockZ() >> 4));
            }
         }
      }
   }

   public Guild getGuildByNexusLocation(Location loc) {
      if (loc == null) {
         return null;
      } else {
         String key = locKey(loc);
         return key == null ? null : this.nexusLocationIndex.get(key);
      }
   }

   public boolean isNearNexus(Location loc, double radius) {
      if (loc != null && loc.getWorld() != null) {
         double radiusSq = radius * radius;

         for (Guild g : this.nexusLocationIndex.values()) {
            if (g.hasNexus() && g.getNexus().getLocation() != null && g.getNexus().isAlive()) {
               Location nLoc = g.getNexus().getLocation();
               if (nLoc.getWorld() != null && nLoc.getWorld().equals(loc.getWorld()) && nLoc.distanceSquared(loc) <= radiusSq) {
                  return true;
               }
            }
         }

         return false;
      } else {
         return false;
      }
   }

   public boolean isNexusChunk(Location loc) {
      return loc != null && loc.getWorld() != null ? this.isNearNexus(loc, 16.0) : false;
   }

   public boolean isNexusChunk(ChunkKey key) {
      String guildId = key == null ? null : this.nexusChunks.get(key);
      if (guildId == null) {
         return false;
      }

      Guild g = this.plugin.getGuildManager().getGuildById(guildId);
      return g != null && g.hasNexus() && g.getNexus().isAlive();
   }

   public boolean placeNexus(Guild guild, Location location, Player player) {
      if (guild != null && location != null) {
         if (guild.hasNexus()) {
            ChatUtil.sendMessage(player, "&c❌ Votre guilde possède déjà un Nexus ! Il ne peut pas être reposé.");
            return false;
         }

         ChunkKey chunkKey = new ChunkKey(location.getWorld().getName(), location.getBlockX() >> 4, location.getBlockZ() >> 4);
         if (!guild.isClaimed(chunkKey)) {
            ChatUtil.sendMessage(player, "&c❌ Le Nexus ne peut être posé que dans un chunk revendiqué (claim) par votre guilde !");
            ChatUtil.sendMessage(player, "&7Faites &e/guild claim &7pour revendiquer ce chunk d'abord.");
            return false;
         } else {
            if (location.getWorld().getEnvironment() != World.Environment.NORMAL) {
               ChatUtil.sendMessage(player, "&c❌ Le Nexus ne peut être posé que dans l'Overworld (pas dans le Nether ni dans l'End).");
               return false;
            }

            World world = location.getWorld();
            int cx = location.getBlockX();
            int cy = location.getBlockY();
            int cz = location.getBlockZ();
            int radius = 16;
            int radiusSq = radius * radius;

            for (int dx = -radius; dx <= radius; dx++) {
               for (int dy = -radius; dy <= radius; dy++) {
                  for (int dz = -radius; dz <= radius; dz++) {
                     if (dx * dx + dy * dy + dz * dz <= radiusSq) {
                        int targetY = cy + dy;
                        if (targetY >= world.getMinHeight() && targetY < world.getMaxHeight()) {
                           Block block = world.getBlockAt(cx + dx, targetY, cz + dz);
                           Material type = block.getType();
                           boolean isForbidden = type == Material.OBSIDIAN
                              || type == Material.CRYING_OBSIDIAN
                              || type == Material.LAVA
                              || type == Material.WATER
                              || type == Material.BUBBLE_COLUMN;
                           if (!isForbidden) {
                              BlockData var19 = block.getBlockData();
                              if (var19 instanceof Waterlogged) {
                                 Waterlogged waterlogged = (Waterlogged)var19;
                                 if (waterlogged.isWaterlogged()) {
                                    isForbidden = true;
                                 }
                              }
                           }

                           if (isForbidden) {
                              String forbiddenName = type != Material.OBSIDIAN && type != Material.CRYING_OBSIDIAN
                                 ? (type == Material.LAVA ? "Lave" : "Eau")
                                 : "Obsidienne";
                              ChatUtil.sendMessage(player, "&c❌ Pose du Nexus refusée !");
                              ChatUtil.sendMessage(
                                 player,
                                 "&cPrésence de &e"
                                    + forbiddenName
                                    + " &cdans le périmètre de 16 blocs du Nexus (en X: "
                                    + block.getX()
                                    + ", Y: "
                                    + block.getY()
                                    + ", Z: "
                                    + block.getZ()
                                    + ") !"
                              );
                              ChatUtil.sendMessage(
                                 player, "&7Vous devez retirer toute &elave&7, &eeau &7et &eobsidienne &7à moins de 16 blocs avant de pouvoir poser le Nexus."
                              );
                              ChatUtil.playSound(player, Sound.ENTITY_VILLAGER_NO, 1.0F, 1.0F);
                              return false;
                           }
                        }
                     }
                  }
               }
            }

            int maxHealth = this.plugin.getConfig().getInt("guilds.nexus.base-health", 5000);
            if (guild.getSpecialization() == GuildSpecialization.MERCHANT_SYNDICATE) {
               maxHealth = (int)((double)maxHealth * 0.8);
            } else if (guild.getSpecialization() == GuildSpecialization.ARCANE_CONCLAVE) {
               maxHealth = (int)((double)maxHealth * 1.25);
            } else if (guild.getSpecialization() == GuildSpecialization.LONE_RANGER) {
               maxHealth = 2500;
            }

            Nexus nexus = new Nexus(guild.getId(), location, maxHealth);
            guild.setNexus(nexus);
            this.indexGuildNexus(guild);
            location.getBlock().setType(Material.CRYING_OBSIDIAN);
            this.updateHologram(guild);
            this.plugin.getDataManager().saveGuild(guild);
            ChatUtil.sendMessage(player, "&a✔ Cœur de Guilde (Nexus) placé avec succès dans votre claim !");
            this.plugin.getAchievements().grant(player.getUniqueId(), fr.esicraft.life.Achievement.NEXUS);
            ChatUtil.sendMessage(player, "&7Le Nexus est maintenant protégé. Vos membres peuvent désormais progresser en toute sécurité.");
            return true;
         }
      } else {
         return false;
      }
   }

   public void removeNexus(Guild guild) {
      if (guild != null) {
         this.unindexGuildNexus(guild);
         this.removeHologram(guild.getId());
         if (guild.getNexus() != null && guild.getNexus().getLocation() != null) {
            Location loc = guild.getNexus().getLocation();
            if (loc.getBlock().getType() == Material.CRYING_OBSIDIAN || loc.getBlock().getType() == Material.BEACON) {
               loc.getBlock().setType(Material.AIR);
            }
         }

         guild.setNexus(null);
         this.plugin.getDataManager().saveGuild(guild);
      }
   }

   public void updateHologram(Guild guild) {
      if (guild != null && guild.hasNexus()) {
         Nexus nexus = guild.getNexus();
         Location loc = nexus.getLocation();
         if (loc != null && loc.getWorld() != null) {
            Location holoLoc = loc.clone().add(0.5, 1.5, 0.5);
            ArmorStand stand = this.holograms.get(guild.getId());
            if (stand == null || !stand.isValid()) {
               for (Entity nearby : loc.getWorld().getNearbyEntities(holoLoc, 2.0, 2.0, 2.0)) {
                  if (nearby instanceof ArmorStand) {
                     ArmorStand as = (ArmorStand)nearby;
                     if (as.getScoreboardTags().contains("esicraft_nexus_hologram")
                        || as.customName() != null && PlainTextComponentSerializer.plainText().serialize(as.customName()).contains("[Nexus:")) {
                        as.remove();
                     }
                  }
               }

               stand = (ArmorStand)loc.getWorld().spawn(holoLoc, ArmorStand.class, asx -> {
                  asx.setVisible(false);
                  asx.setGravity(false);
                  asx.setCustomNameVisible(true);
                  asx.setMarker(true);
                  asx.setPersistent(false);
                  asx.addScoreboardTag("esicraft_nexus_hologram");
                  asx.addScoreboardTag("nexus_guild_" + guild.getId());
               });
               this.holograms.put(guild.getId(), stand);
            }

            String healthBar = this.getHealthBar(nexus.getCurrentHealth(), nexus.getMaxHealth());
            stand.customName(
               ChatUtil.component("&6&l[Nexus: " + guild.getName() + "] &e" + nexus.getCurrentHealth() + "/" + nexus.getMaxHealth() + " PV " + healthBar)
            );
         }
      }
   }

   public void removeHologram(String guildId) {
      if (guildId != null) {
         ArmorStand stand = this.holograms.remove(guildId);
         if (stand != null && stand.isValid()) {
            stand.remove();
         } else {
            for (World world : Bukkit.getWorlds()) {
               for (ArmorStand as : world.getEntitiesByClass(ArmorStand.class)) {
                  if (as.getScoreboardTags().contains("nexus_guild_" + guildId)) {
                     as.remove();
                  }
               }
            }
         }
      }
   }

   public void removeAllHolograms() {
      for (ArmorStand stand : this.holograms.values()) {
         if (stand != null && stand.isValid()) {
            stand.remove();
         }
      }

      this.holograms.clear();

      for (World world : Bukkit.getWorlds()) {
         for (ArmorStand as : world.getEntitiesByClass(ArmorStand.class)) {
            if (as.getScoreboardTags().contains("esicraft_nexus_hologram")
               || as.customName() != null && PlainTextComponentSerializer.plainText().serialize(as.customName()).contains("[Nexus:")) {
               as.remove();
            }
         }
      }
   }

   public void clearIndex() {
      this.nexusChunks.clear();
      this.nexusLocationIndex.clear();
      this.activeNexusGuilds.clear();
   }

   private String getHealthBar(int current, int max) {
      double pct = (double)current / (double)max;
      int bars = 10;
      int greenBars = (int)(pct * (double)bars);
      return "&a" + "|".repeat(Math.max(0, greenBars)) + "&c" + "|".repeat(Math.max(0, bars - greenBars));
   }

   public void damageNexus(Guild guild, int damage, String attackerName) {
      Player exact = attackerName != null ? Bukkit.getPlayerExact(attackerName) : null;
      Guild attackerGuild = exact != null ? this.plugin.getGuildManager().getGuildByPlayer(exact.getUniqueId()) : null;
      this.damageNexus(guild, damage, attackerName, attackerGuild, exact);
   }

   /** attackerGuild / attackerPlayer sont résolus par l'appelant (jamais par recherche de nom ambiguë). */
   public void damageNexus(Guild guild, int damage, String attackerName, Guild attackerGuild, Player attackerPlayer) {
      if (guild != null && guild.hasNexus()) {
         Nexus nexus = guild.getNexus();
         this.lastNexusDamage.put(guild.getId(), System.currentTimeMillis());
         nexus.damage(damage);
         this.updateHologram(guild);
         Location loc = nexus.getLocation();
         if (loc != null && loc.getWorld() != null) {
            loc.getWorld().spawnParticle(Particle.CRIT, loc.clone().add(0.5, 0.5, 0.5), 15, 0.3, 0.3, 0.3, 0.1);
            loc.getWorld().playSound(loc, Sound.BLOCK_ANVIL_LAND, 1.0F, 0.6F);
            if (nexus.hasTrap(NexusTrapType.LIGHTNING_STRIKE) && attackerPlayer != null && attackerPlayer.isOnline() && this.lightningReady(attackerPlayer)) {
               {
                  loc.getWorld().strikeLightningEffect(attackerPlayer.getLocation());
                  attackerPlayer.damage(LIGHTNING_DAMAGE);
                  ChatUtil.sendMessage(attackerPlayer, "&e⚡ &lRIPOSTE DU NEXUS : &cVous avez été foudroyé pour avoir attaqué le Cœur de Guilde !");
               }
            }

            if (guild.getSpecialization() == GuildSpecialization.DRUIDIC_ENCLAVE) {
               List<Wolf> guardBeasts = new ArrayList<>();
               for (Entity near : loc.getWorld().getNearbyEntities(loc, 15.0, 15.0, 15.0)) {
                  if (near instanceof Wolf w && w.getScoreboardTags().contains("druidic_guard_beast")) {
                     guardBeasts.add(w);
                  }
               }

               // Les bêtes déjà présentes se jettent sur l'assaillant.
               if (attackerPlayer != null && attackerPlayer.isOnline()) {
                  for (Wolf w : guardBeasts) {
                     w.setAngry(true);
                     w.setTarget(attackerPlayer);
                  }
               }

               if (guardBeasts.size() < 3) {
                  Location beastLoc = loc.clone()
                     .add((double)ThreadLocalRandom.current().nextInt(-2, 3), 0.0, (double)ThreadLocalRandom.current().nextInt(-2, 3));
                  loc.getWorld().spawn(beastLoc, Wolf.class, wolf -> {
                     wolf.customName(ChatUtil.component("&a&l[Bête de Garde : " + guild.getName() + "]"));
                     wolf.setCustomNameVisible(true);
                     wolf.setAngry(true);
                     wolf.addScoreboardTag("druidic_guard_beast");
                     wolf.setRemoveWhenFarAway(false);
                     if (attackerPlayer != null && attackerPlayer.isOnline()) {
                        wolf.setTarget(attackerPlayer);
                     }
                     wolf.addPotionEffect(new PotionEffect(PotionEffectType.STRENGTH, 1200, 1));
                     wolf.addPotionEffect(new PotionEffect(PotionEffectType.SPEED, 1200, 1));
                  });
                  loc.getWorld().playSound(loc, Sound.ENTITY_WOLF_GROWL, 1.2F, 0.8F);
               }
            }

            if (guild.getSpecialization() == GuildSpecialization.WARLORD) {
               loc.getWorld().playSound(loc, Sound.EVENT_RAID_HORN, 2.0F, 1.0F);

               for (Player defender : loc.getWorld().getPlayers()) {
                  if (guild.isMember(defender.getUniqueId()) && defender.getLocation().distanceSquared(loc) <= 900.0) {
                     defender.addPotionEffect(new PotionEffect(PotionEffectType.STRENGTH, 200, 1));
                     defender.addPotionEffect(new PotionEffect(PotionEffectType.RESISTANCE, 200, 1));
                     ChatUtil.sendActionbar(defender, "&c⚔️ Ralliement de Guerre : Force II & Résistance II activées !");
                  }
               }
            }
         }

         for (Player member : Bukkit.getOnlinePlayers()) {
            if (guild.isMember(member.getUniqueId())) {
               ChatUtil.sendActionbar(
                  member,
                  "&c\ud83d\udea8 ALERTE : Votre Nexus est attaqué par &e"
                     + attackerName
                     + " &c! ("
                     + nexus.getCurrentHealth()
                     + "/"
                     + nexus.getMaxHealth()
                     + " PV)"
               );
               ChatUtil.playSound(member, Sound.BLOCK_NOTE_BLOCK_PLING, 1.0F, 0.5F);
            }
         }

         if (!nexus.isAlive()) {
            this.onNexusDestroyed(guild, attackerName, attackerGuild);
         }
      }
   }

   /** Dégâts de la Foudre de Riposte (4 cœurs) et délai minimal entre deux coups de foudre sur un même joueur. */
   private static final double LIGHTNING_DAMAGE = 8.0;
   private static final long LIGHTNING_COOLDOWN_MS = 5000L;
   private final Map<UUID, Long> lastLightning = new java.util.concurrent.ConcurrentHashMap<>();

   private boolean lightningReady(Player attacker) {
      long now = System.currentTimeMillis();
      Long last = this.lastLightning.get(attacker.getUniqueId());
      if (last != null && now - last < LIGHTNING_COOLDOWN_MS) {
         return false;
      }

      this.lastLightning.put(attacker.getUniqueId(), now);
      if (this.lastLightning.size() > 500) {
         this.lastLightning.entrySet().removeIf(e -> now - e.getValue() > LIGHTNING_COOLDOWN_MS);
      }

      return true;
   }

   private boolean isEnemy(Guild guild, Entity entity) {
      if (entity instanceof Monster) {
         return true;
      } else if (entity instanceof Player player) {
         // Seuls les joueurs en mode survie / aventure, non-membres, non-alliés et présents dans le claim sont visés (pas les simples passants)
         org.bukkit.GameMode mode = player.getGameMode();
         if ((mode == org.bukkit.GameMode.CREATIVE || mode == org.bukkit.GameMode.SPECTATOR) || guild.isMember(player.getUniqueId())) {
            return false;
         }

         if (this.plugin.getDisguiseManager().isDisguisedAs(player.getUniqueId(), guild.getId())) {
            return false;
         }

         Guild other = this.plugin.getGuildManager().getGuildByPlayer(player.getUniqueId());
         if (other != null && this.plugin.getTreatyManager().between(guild.getId(), other.getId()) != null) {
            return false;
         }

         return guild.isClaimed(ChunkKey.from(player.getLocation().getChunk()));
      } else {
         return false;
      }
   }

   public void onNexusDestroyed(Guild guild, String killer) {
      this.onNexusDestroyed(guild, killer, null);
   }

   public void onNexusDestroyed(Guild guild, String killer, Guild attackerGuild) {
      this.unindexGuildNexus(guild);
      Nexus nexus = guild.getNexus();
      if (nexus != null && nexus.getLocation() != null) {
         Location loc = nexus.getLocation();

         try {
            loc.getWorld().spawnParticle(Particle.EXPLOSION, loc.clone().add(0.5, 0.5, 0.5), 5);
         } catch (Exception var6) {
         }

         loc.getWorld().playSound(loc, Sound.ENTITY_GENERIC_EXPLODE, 2.0F, 0.8F);
      }

      Guild victorGuild = attackerGuild;
      if (victorGuild == null) {
         Player killerPlayer = killer != null ? Bukkit.getPlayerExact(killer) : null;
         if (killerPlayer != null) {
            victorGuild = this.plugin.getGuildManager().getGuildByPlayer(killerPlayer.getUniqueId());
         }
      }

      this.plugin.getHelpCalls().cancel(guild.getId());
      this.plugin.getChronicle().raid(victorGuild != null ? victorGuild.getId() : null, guild.getId(), "destroyed");
      if (victorGuild != null && !victorGuild.getId().equals(guild.getId())) {
         this.plugin.getSeasonManager().recordConquest(victorGuild, guild);
         this.plugin.getGuildManager().registerAnnexation(guild, victorGuild);
      } else {
         guild.setVulnerableUntil(System.currentTimeMillis() + 1800000L);
         this.plugin.getDataManager().saveGuild(guild);
         ChatUtil.broadcast("&4&l\ud83d\udca5 LE NEXUS DE LA GUILDE &c" + guild.getName() + " &4&lA ÉTÉ DÉTRUIT PAR " + killer + " !");
         ChatUtil.broadcast("&eLeur territoire est désormais &cVULNÉRABLE &ependant 30 minutes !");
         this.plugin.getChronicle().log(fr.esicraft.life.Chronicle.Kind.CONQUEST, "Le Nexus de " + guild.getName() + " a été détruit par " + killer + " : son territoire est vulnérable.", guild.getId(), null);
      }
   }

   private static final long GUARDIAN_RESPAWN_MS = 300000L;
   private final Map<String, Long> guardianMissingSince = new java.util.concurrent.ConcurrentHashMap<>();

   public static String guardianTag(Guild guild) {
      return "esicraft_guardian_" + guild.getId();
   }

   /** Invoque (ou ré-invoque) le Golem Gardien d'une guilde au pied du Nexus. */
   public void spawnGuardian(Guild guild, Location nexusLoc) {
      if (nexusLoc == null || nexusLoc.getWorld() == null) {
         return;
      }

      Location spawnLoc = nexusLoc.clone().add(1.0, 0.0, 1.0);
      String tag = guardianTag(guild);
      spawnLoc.getWorld().spawn(spawnLoc, org.bukkit.entity.IronGolem.class, g -> {
         g.customName(ChatUtil.component("&6&l[Gardien : " + guild.getName() + "]"));
         g.setCustomNameVisible(true);
         g.setPlayerCreated(false);
         g.setRemoveWhenFarAway(false);
         g.addScoreboardTag(tag);
      });
   }

   /** Supprime tous les golems gardiens d'une guilde (piège consommé ou guilde détruite). */
   public void removeGuardians(Guild guild, Location nexusLoc) {
      this.guardianMissingSince.remove(guild.getId());
      if (nexusLoc == null || nexusLoc.getWorld() == null) {
         return;
      }

      String tag = guardianTag(guild);
      for (Entity e : nexusLoc.getWorld().getNearbyEntities(nexusLoc, 64.0, 64.0, 64.0)) {
         if (e.getScoreboardTags().contains(tag)) {
            e.remove();
         }
      }
   }

   /** Un golem tué est remplacé 5 minutes plus tard tant que le piège est armé. */
   private void maintainGuardian(Guild guild, Location nexusLoc, Collection<Entity> nearby) {
      String tag = guardianTag(guild);
      for (Entity e : nearby) {
         if (e.getScoreboardTags().contains(tag)) {
            this.guardianMissingSince.remove(guild.getId());
            return;
         }
      }

      // Pas de golem à proximité : peut-être parti au loin, on élargit la recherche avant de conclure qu'il est mort.
      for (Entity e : nexusLoc.getWorld().getNearbyEntities(nexusLoc, 64.0, 64.0, 64.0)) {
         if (e.getScoreboardTags().contains(tag)) {
            this.guardianMissingSince.remove(guild.getId());
            return;
         }
      }

      long now = System.currentTimeMillis();
      Long since = this.guardianMissingSince.putIfAbsent(guild.getId(), now);
      if (since != null && now - since >= GUARDIAN_RESPAWN_MS) {
         this.guardianMissingSince.remove(guild.getId());
         this.spawnGuardian(guild, nexusLoc);
         ChatUtil.broadcastGuild(guild, "&6🤖 Votre Automate Gardien a été reconstruit au pied du Nexus.");
      }
   }

   private final Map<String, Long> lastNexusDamage = new java.util.concurrent.ConcurrentHashMap<>();

   /** Vrai si le Nexus a subi des dégâts dans la fenêtre donnée (anti « dissolution pour refuser le butin »). */
   public boolean wasNexusRecentlyAttacked(Guild guild, long windowMs) {
      Long last = guild != null ? this.lastNexusDamage.get(guild.getId()) : null;
      return last != null && System.currentTimeMillis() - last < windowMs;
   }

   private final Map<String, Long> lastPlayerHeal = new java.util.concurrent.ConcurrentHashMap<>();

   private boolean isHealReady(Guild guild) {
      long cooldownMs = this.plugin.getConfig().getLong("guilds.nexus.heal-cooldown-seconds", 30L) * 1000L;
      Long last = this.lastPlayerHeal.get(guild.getId());
      return last == null || System.currentTimeMillis() - last >= cooldownMs;
   }

   private boolean isHealOnCooldown(Guild guild, Player player) {
      long cooldownMs = this.plugin.getConfig().getLong("guilds.nexus.heal-cooldown-seconds", 30L) * 1000L;
      Long last = this.lastPlayerHeal.get(guild.getId());
      if (last != null && System.currentTimeMillis() - last < cooldownMs) {
         long remaining = (cooldownMs - (System.currentTimeMillis() - last) + 999L) / 1000L;
         ChatUtil.sendMessage(player, "&c❌ Le Nexus vient d'être soigné : réessayez dans &e" + remaining + "s&c.");
         ChatUtil.playSound(player, Sound.ENTITY_VILLAGER_NO, 1.0F, 1.0F);
         return true;
      }

      return false;
   }

   public boolean healWithGoldenApple(Guild guild, Player player, ItemStack item) {
      if (guild != null && guild.hasNexus()) {
         Nexus nexus = guild.getNexus();
         if (this.plugin.getRaidManager().isGuildUnderRaid(guild.getId())) {
            if (player != null) {
               ChatUtil.sendMessage(player, "&c❌ Impossible de soigner le Nexus : une invasion de monstres est en cours !");
               ChatUtil.playSound(player, Sound.ENTITY_VILLAGER_NO, 1.0F, 1.0F);
            }

            return false;
         } else if (this.wasNexusRecentlyAttacked(guild, this.plugin.getConfig().getLong("nexus.heal-block-seconds", 120L) * 1000L)) {
            // Pas de soin pendant ni juste après une attaque de joueurs (2 500 PV toutes les 30 s annuleraient n'importe quel assaut).
            if (player != null) {
               ChatUtil.sendMessage(player, "&c❌ Le Nexus a été attaqué il y a moins de &e" + this.plugin.getConfig().getLong("nexus.heal-block-seconds", 120L) + " s&c : impossible de le soigner maintenant.");
               ChatUtil.playSound(player, Sound.ENTITY_VILLAGER_NO, 1.0F, 1.0F);
            }

            return false;
         } else if (player != null ? this.isHealOnCooldown(guild, player) : !this.isHealReady(guild)) {
            return false;
         } else if (nexus.getCurrentHealth() >= nexus.getMaxHealth()) {
            if (player != null) {
               ChatUtil.sendMessage(player, "&eLe Nexus a déjà sa santé maximale (&a" + nexus.getMaxHealth() + "/" + nexus.getMaxHealth() + " PV&e) !");
            }

            return false;
         } else {
            int healAmount = 500;
            if (item != null && item.getType() == Material.ENCHANTED_GOLDEN_APPLE) {
               healAmount = 2500;
            }

            nexus.heal(healAmount);
            this.lastPlayerHeal.put(guild.getId(), System.currentTimeMillis());

            this.updateHologram(guild);
            this.plugin.getDataManager().saveGuild(guild);
            Location loc = nexus.getLocation();
            if (loc != null && loc.getWorld() != null) {
               try {
                  loc.getWorld().spawnParticle(Particle.HEART, loc.clone().add(0.5, 1.2, 0.5), 10, 0.4, 0.4, 0.4, 0.1);
                  loc.getWorld().spawnParticle(Particle.TOTEM_OF_UNDYING, loc.clone().add(0.5, 1.0, 0.5), 20, 0.3, 0.5, 0.3, 0.2);
               } catch (Exception var8) {
               }

               loc.getWorld().playSound(loc, Sound.ENTITY_PLAYER_LEVELUP, 1.0F, 1.2F);
            }

            if (player != null) {
               ChatUtil.sendActionbar(
                  player, "&a✨ + " + healAmount + " PV restaurés au Nexus ! (&e" + nexus.getCurrentHealth() + "/" + nexus.getMaxHealth() + " PV&a)"
               );
               ChatUtil.sendMessage(player, "&aVous avez restauré &e" + healAmount + " PV &aau Nexus grâce à une Pomme Dorée !");
            }

            return true;
         }
      } else {
         return false;
      }
   }

   private void startNexusTask() {
      (new BukkitRunnable() {
            private int unplacedCheckCounter = 0;

            public void run() {
               long now = System.currentTimeMillis();
               this.unplacedCheckCounter++;
               if (this.unplacedCheckCounter >= 15) {
                  this.unplacedCheckCounter = 0;
                  List<Guild> guildsToDisband = new ArrayList<>();

                  for (Guild guild : NexusManager.this.plugin.getGuildManager().getAllGuilds()) {
                     if (!guild.hasNexus()) {
                        long ageMs = now - guild.getCreatedAt();
                        if (ageMs > 3600000L) {
                           guildsToDisband.add(guild);
                        }
                     }
                  }

                  for (Guild g : guildsToDisband) {
                     ChatUtil.broadcast(
                        "&4&l[Guildes] &cLa guilde &6"
                           + g.getName()
                           + " &ca été automatiquement dissoute : aucun Nexus n'a été placé dans l'heure suivant sa création !"
                     );
                     NexusManager.this.plugin.getGuildManager().disbandGuild(g);
                  }
               }

               for (String guildId : NexusManager.this.activeNexusGuilds) {
                  Guild guildx = NexusManager.this.plugin.getGuildManager().getGuildById(guildId);
                  if (guildx != null && guildx.hasNexus()) {
                     Nexus nexus = guildx.getNexus();
                     Location loc = nexus.getLocation();
                     if (loc != null && loc.getWorld() != null && loc.getWorld().isChunkLoaded(loc.getBlockX() >> 4, loc.getBlockZ() >> 4)) {
                        if (nexus.isAlive()) {
                           ArmorStand holo = NexusManager.this.holograms.get(guildId);
                           if (holo == null || !holo.isValid()) {
                              NexusManager.this.updateHologram(guildx);
                           }
                        }

                        try {
                           loc.getWorld().spawnParticle(Particle.PORTAL, loc.clone().add(0.5, 0.8, 0.5), 6, 0.2, 0.2, 0.2, 0.05);
                        } catch (Exception var23) {
                        }

                        if (nexus.isAlive()) {
                           Collection<Entity> nearbyEntities = loc.getWorld().getNearbyEntities(loc, 18.0, 18.0, 18.0);
                           if (!nearbyEntities.isEmpty()) {
                              boolean hasArcane = guildx.getSpecialization() == GuildSpecialization.ARCANE_CONCLAVE;
                              boolean hasFlameTrap = nexus.hasTrap(NexusTrapType.INFERNAL_FLAMES);
                              boolean hasWebTrap = nexus.hasTrap(NexusTrapType.SLOWDOWN_WEB);
                              boolean hasPoisonTrap = nexus.hasTrap(NexusTrapType.POISON_MIST);
                              if (hasFlameTrap) {
                                 try {
                                    loc.getWorld().spawnParticle(Particle.FLAME, loc.clone().add(0.5, 0.2, 0.5), 8, 1.5, 0.1, 1.5, 0.02);
                                 } catch (Exception var21) {
                                 }
                              }

                              if (hasPoisonTrap) {
                                 try {
                                    loc.getWorld().spawnParticle(Particle.CAMPFIRE_COSY_SMOKE, loc.clone().add(0.5, 0.5, 0.5), 10, 2.0, 0.2, 2.0, 0.01);
                                 } catch (Exception var22) {
                                 }
                              }

                              if (hasArcane) {
                                 LivingEntity closestAttacker = null;
                                 double closestDistSq = Double.MAX_VALUE;

                                 for (Entity entity : nearbyEntities) {
                                    if (entity instanceof LivingEntity) {
                                       LivingEntity living = (LivingEntity)entity;
                                       if (!living.isDead() && NexusManager.this.isEnemy(guildx, entity)) {
                                          double dSq = entity.getLocation().distanceSquared(loc);
                                          if (dSq < closestDistSq) {
                                             closestDistSq = dSq;
                                             closestAttacker = living;
                                          }
                                       }
                                    }
                                 }

                                 if (closestAttacker != null) {
                                    closestAttacker.damage(2.0);

                                    try {
                                       loc.getWorld().spawnParticle(Particle.PORTAL, closestAttacker.getLocation().add(0.0, 1.0, 0.0), 15, 0.3, 0.3, 0.3, 0.05);
                                    } catch (Exception var26) {
                                    }

                                    loc.getWorld().playSound(loc, Sound.ENTITY_ILLUSIONER_CAST_SPELL, 0.8F, 1.4F);
                                 }
                              }

                              if (nexus.hasTrap(NexusTrapType.GUARDIAN_GOLEM)) {
                                 NexusManager.this.maintainGuardian(guildx, loc, nearbyEntities);
                              }

                              for (Entity entityx : nearbyEntities) {
                                 double distSq = entityx.getLocation().distanceSquared(loc);
                                 if (entityx instanceof org.bukkit.entity.IronGolem golem && golem.getScoreboardTags().contains("esicraft_guardian_" + guildx.getId())) {
                                    LivingEntity bestTarget = null;
                                    double bestDist = 256.0;

                                    for (Entity candidate : nearbyEntities) {
                                       if (candidate instanceof LivingEntity candLiving && !candLiving.isDead() && NexusManager.this.isEnemy(guildx, candidate)) {
                                          double cd = candidate.getLocation().distanceSquared(golem.getLocation());
                                          if (cd < bestDist) {
                                             bestDist = cd;
                                             bestTarget = candLiving;
                                          }
                                       }
                                    }

                                    if (bestTarget != null) {
                                       golem.setTarget(bestTarget);
                                    }
                                 }

                                 if (entityx instanceof Wolf beast && beast.getScoreboardTags().contains("druidic_guard_beast")) {
                                    LivingEntity cur = beast.getTarget();
                                    if (cur == null || cur.isDead() || !NexusManager.this.isEnemy(guildx, cur)) {
                                       LivingEntity best = null;
                                       double bd = 400.0;
                                       for (Entity candidate : nearbyEntities) {
                                          if (candidate instanceof LivingEntity cl && !cl.isDead() && !(candidate instanceof Wolf) && NexusManager.this.isEnemy(guildx, candidate)) {
                                             double cd = candidate.getLocation().distanceSquared(beast.getLocation());
                                             if (cd < bd) {
                                                bd = cd;
                                                best = cl;
                                             }
                                          }
                                       }

                                       if (best != null) {
                                          beast.setAngry(true);
                                          beast.setTarget(best);
                                       }
                                    }
                                 }

                                 if (NexusManager.this.isEnemy(guildx, entityx)) {
                                    if (hasFlameTrap && distSq <= 64.0) {
                                       entityx.setFireTicks(60);
                                       if (entityx instanceof Damageable d) {
                                          d.damage(2.0);
                                       }

                                       try {
                                          loc.getWorld().spawnParticle(Particle.LAVA, entityx.getLocation().add(0.0, 1.0, 0.0), 5);
                                       } catch (Exception var25) {
                                       }
                                    }

                                    if (hasWebTrap && distSq <= 100.0 && entityx instanceof LivingEntity le) {
                                       le.addPotionEffect(new PotionEffect(PotionEffectType.SLOWNESS, 60, 1, false, false, false));

                                       try {
                                          loc.getWorld().spawnParticle(Particle.SNOWFLAKE, le.getLocation().add(0.0, 1.0, 0.0), 5);
                                       } catch (Exception var24) {
                                       }
                                    }

                                    if (hasPoisonTrap && distSq <= 64.0 && entityx instanceof LivingEntity le) {
                                       le.addPotionEffect(new PotionEffect(PotionEffectType.POISON, 100, 0, false, false, true));
                                    }
                                 }

                                 if (!NexusManager.this.plugin.getRaidManager().isGuildUnderRaid(guildx.getId())
                                    && NexusManager.this.isHealReady(guildx)
                                    && nexus.getCurrentHealth() < nexus.getMaxHealth()
                                    && distSq <= 4.0
                                    && entityx instanceof Item droppedItem
                                    && droppedItem.getThrower() != null
                                    && guildx.isMember(droppedItem.getThrower())) {
                                    ItemStack itemStack = droppedItem.getItemStack();
                                    if (itemStack.getType() == Material.GOLDEN_APPLE || itemStack.getType() == Material.ENCHANTED_GOLDEN_APPLE) {
                                       if (itemStack.getAmount() > 1) {
                                          itemStack.setAmount(itemStack.getAmount() - 1);
                                          droppedItem.setItemStack(itemStack);
                                       } else {
                                          droppedItem.remove();
                                       }

                                       NexusManager.this.healWithGoldenApple(guildx, null, itemStack);
                                    }
                                 }
                              }
                           }
                        }
                     }
                  }
               }
            }
         })
         .runTaskTimer(this.plugin, 40L, 40L);
   }
}
