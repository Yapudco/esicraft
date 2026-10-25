package fr.esicraft.guild;

import fr.esicraft.EsicraftPlugin;
import fr.esicraft.age.AgeType;
import fr.esicraft.data.PlayerData;
import fr.esicraft.gui.AnnexationChoiceGUI;
import fr.esicraft.guild.listener.GuildClaimListener;
import fr.esicraft.nexus.Nexus;
import fr.esicraft.race.Race;
import fr.esicraft.race.listener.RaceCombatListener;
import fr.esicraft.util.ChatUtil;
import fr.esicraft.util.ChunkKey;
import java.util.Collection;
import java.util.EnumMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Pattern;
import org.bukkit.Bukkit;
import org.bukkit.Chunk;
import org.bukkit.OfflinePlayer;
import org.bukkit.Sound;
import org.bukkit.block.BlockState;
import org.bukkit.entity.Player;

public class GuildManager {
   private final EsicraftPlugin plugin;
   private final Map<String, Guild> guildsById = new ConcurrentHashMap<>();
   private final Map<String, Guild> guildsByName = new ConcurrentHashMap<>();
   private final Map<ChunkKey, Guild> chunkClaims = new ConcurrentHashMap<>();
   private final Map<UUID, String> playerGuildMap = new ConcurrentHashMap<>();
   private final Map<UUID, Set<String>> pendingInvites = new ConcurrentHashMap<>();
   private static final Pattern GUILD_NAME_PATTERN = Pattern.compile("^[a-zA-Z0-9àâäéèêëïîôùûüÿçÀÂÄÉÈÊËÏÎÔÙÛÜŸÇ_\\-]{3,24}$");
   private static final Pattern GUILD_TAG_PATTERN = Pattern.compile("^[a-zA-Z0-9àâäéèêëïîôùûüÿçÀÂÄÉÈÊËÏÎÔÙÛÜŸÇ_\\-]{2,6}$");
   private final Map<UUID, String> pendingAnnexationChoices = new ConcurrentHashMap<>();

   public GuildManager(EsicraftPlugin plugin) {
      this.plugin = plugin;
   }

   public void registerGuild(Guild guild) {
      this.guildsById.put(guild.getId(), guild);
      this.guildsByName.put(guild.getName().toLowerCase(), guild);

      for (UUID member : guild.getMembers().keySet()) {
         this.playerGuildMap.put(member, guild.getId());
      }

      for (ChunkKey ck : guild.getClaimedChunks()) {
         this.chunkClaims.put(ck, guild);
      }

      if (guild.hasNexus() && this.plugin.getNexusManager() != null) {
         this.plugin.getNexusManager().indexGuildNexus(guild);
      }
   }

   public Collection<Guild> getAllGuilds() {
      return this.guildsById.values();
   }

   public Guild getGuildById(String id) {
      return id == null ? null : this.guildsById.get(id);
   }

   public Guild getGuildByName(String name) {
      return name == null ? null : this.guildsByName.get(name.toLowerCase());
   }

   public Guild getGuildByPlayer(UUID playerUuid) {
      String guildId = this.playerGuildMap.get(playerUuid);
      if (guildId == null) {
         PlayerData data = this.plugin.getDataManager().getPlayerData(playerUuid);
         if (data != null && data.hasGuild()) {
            guildId = data.getGuildId();
            this.playerGuildMap.put(playerUuid, guildId);
         }
      }

      return guildId != null ? this.guildsById.get(guildId) : null;
   }

   public Guild getGuildByChunk(ChunkKey chunkKey) {
      return this.chunkClaims.get(chunkKey);
   }

   public boolean createGuild(Player player, String name, String tag) {
      if (player == null || name == null || tag == null) {
         return false;
      } else if (!GUILD_NAME_PATTERN.matcher(name).matches()) {
         ChatUtil.sendMessage(player, "&cNom de guilde invalide ! 3 à 24 caractères alphanumériques, tirets et underscores uniquement.");
         return false;
      } else if (!GUILD_TAG_PATTERN.matcher(tag).matches()) {
         ChatUtil.sendMessage(player, "&cTag invalide ! 2 à 6 caractères alphanumériques uniquement.");
         return false;
      } else {
         PlayerData data = this.plugin.getDataManager().getPlayerData(player.getUniqueId());
         if (data != null && !data.hasGuild()) {
            if (this.guildsByName.containsKey(name.toLowerCase())) {
               ChatUtil.sendMessage(player, "&cCe nom de guilde est déjà utilisé !");
               return false;
            } else {
               double cost = this.plugin.getConfig().getDouble("guilds.creation-cost", 500.0);
               if (data.getCoins() < cost) {
                  ChatUtil.sendMessage(player, "&cVous n'avez pas assez d'argent pour fonder une guilde (&e" + cost + " pièces &crequises).");
                  return false;
               } else {
                  data.removeCoins(cost);
                  String guildId = UUID.randomUUID().toString();
                  Guild guild = new Guild(guildId, name, tag, player.getUniqueId());
                  data.setGuildId(guildId);
                  data.setGuildRole(GuildRole.LEADER);
                  this.registerGuild(guild);
                  this.plugin.getDataManager().saveGuild(guild);
                  this.plugin.getDataManager().savePlayer(data);
                  this.updatePlayerTab(player);
                  ChatUtil.broadcast("&6&l[Guildes] &e" + player.getName() + " &aa fondé la guilde &6" + name + " &8[&e" + tag + "&8] &a!");
                  ChatUtil.sendMessage(
                     player,
                     "&e⚠ &lIMPORTANT : &eVous avez &61 heure &epour revendiquer un chunk (&e/guild claim&e) et poser votre Nexus (&e/guild nexus place&e) sous peine de dissolution automatique de la guilde !"
                  );
                  return true;
               }
            }
         } else {
            ChatUtil.sendMessage(player, "&cVous appartenez déjà à une guilde !");
            return false;
         }
      }
   }

   public void clearAll() {
      this.guildsById.clear();
      this.guildsByName.clear();
      this.chunkClaims.clear();
      this.playerGuildMap.clear();
      this.pendingInvites.clear();
   }

   public boolean disbandGuild(Guild guild) {
      if (guild == null) {
         return false;
      } else {
         this.plugin.getNexusManager().removeNexus(guild);

         for (UUID member : guild.getMembers().keySet()) {
            this.playerGuildMap.remove(member);
            PlayerData pData = this.plugin.getDataManager().getPlayerData(member);
            if (pData != null) {
               pData.setGuildId(null);
               pData.setGuildRole(null);
               pData.setHasJoinedGuildBefore(true);
               this.plugin.getDataManager().savePlayer(pData);
            }
         }

         for (ChunkKey ck : guild.getClaimedChunks()) {
            this.chunkClaims.remove(ck);
         }

         this.guildsById.remove(guild.getId());
         this.guildsByName.remove(guild.getName().toLowerCase());
         this.plugin.getDataManager().deleteGuild(guild.getId());
         this.updateAllPlayerTabs();
         ChatUtil.broadcast("&c&l[Guildes] &7La guilde &c" + guild.getName() + " &7a été dissoute.");
         return true;
      }
   }

   public boolean leaveGuild(Guild guild, Player player) {
      if (guild != null && player != null) {
         UUID uuid = player.getUniqueId();
         if (!guild.isMember(uuid)) {
            ChatUtil.sendMessage(player, "&cVous ne faites pas partie de cette guilde.");
            return false;
         } else if (guild.getLeaderUuid().equals(uuid)) {
            if (guild.getMembers().size() <= 1) {
               this.disbandGuild(guild);
               ChatUtil.sendMessage(player, "&aVous avez quitté et dissous votre guilde.");
               return true;
            } else {
               ChatUtil.sendMessage(
                  player,
                  "&cVous êtes le chef de la guilde ! Transférez le leadership (&e/guild transfer <joueur>&c) ou dissolvez la guilde (&e/guild disband&c)."
               );
               return false;
            }
         } else {
            guild.removeMember(uuid);
            this.playerGuildMap.remove(uuid);
            PlayerData pData = this.plugin.getDataManager().getPlayerData(uuid);
            if (pData != null) {
               pData.setGuildId(null);
               pData.setGuildRole(null);
               pData.setHasJoinedGuildBefore(true);
               this.plugin.getDataManager().savePlayer(pData);
            }

            this.updatePlayerTab(player);
            this.plugin.getRaceManager().applyRaceAttributes(player, this.plugin.getRaceManager().getRace(player));
            this.plugin.getDataManager().saveGuild(guild);
            ChatUtil.sendMessage(player, "&aVous avez quitté la guilde &6" + guild.getName() + " &aavec succès.");

            for (UUID memberUuid : guild.getMembers().keySet()) {
               Player member = Bukkit.getPlayer(memberUuid);
               if (member != null && member.isOnline()) {
                  ChatUtil.sendMessage(member, "&e" + player.getName() + " &ca quitté la guilde.");
               }
            }

            return true;
         }
      } else {
         return false;
      }
   }

   public boolean kickPlayer(Guild guild, Player officer, OfflinePlayer target) {
      if (guild != null && officer != null && target != null && target.getUniqueId() != null) {
         UUID targetUuid = target.getUniqueId();
         if (!guild.isMember(targetUuid)) {
            ChatUtil.sendMessage(officer, "&cCe joueur ne fait pas partie de votre guilde.");
            return false;
         } else {
            GuildRole officerRole = guild.getRole(officer.getUniqueId());
            GuildRole targetRole = guild.getRole(targetUuid);
            if (!officerRole.isAtLeast(GuildRole.OFFICER)) {
               ChatUtil.sendMessage(officer, "&cSeuls les chefs et officiers peuvent expulser des membres.");
               return false;
            } else if (targetRole.ordinal() >= officerRole.ordinal()) {
               ChatUtil.sendMessage(officer, "&cVous ne pouvez pas expulser un membre ayant un rang égal ou supérieur au vôtre.");
               return false;
            } else {
               guild.removeMember(targetUuid);
               this.playerGuildMap.remove(targetUuid);
               PlayerData pData = this.plugin.getDataManager().getPlayerData(targetUuid);
               if (pData != null) {
                  pData.setGuildId(null);
                  pData.setGuildRole(null);
                  pData.setHasJoinedGuildBefore(true);
                  this.plugin.getDataManager().savePlayer(pData);
               }

               this.plugin.getDataManager().saveGuild(guild);
               if (target.isOnline() && target.getPlayer() != null) {
                  this.updatePlayerTab(target.getPlayer());
                  this.plugin.getRaceManager().applyRaceAttributes(target.getPlayer(), this.plugin.getRaceManager().getRace(target.getPlayer()));
               }

               ChatUtil.sendMessage(officer, "&aVous avez expulsé &e" + (target.getName() != null ? target.getName() : "le joueur") + " &ade la guilde.");
               if (target.isOnline() && target.getPlayer() != null) {
                  ChatUtil.sendMessage(target.getPlayer(), "&cVous avez été expulsé de la guilde &6" + guild.getName() + "&c.");
               }

               return true;
            }
         }
      } else {
         return false;
      }
   }

   public boolean transferLeadership(Guild guild, Player leader, Player newLeader) {
      if (guild != null && leader != null && newLeader != null) {
         if (!guild.getLeaderUuid().equals(leader.getUniqueId())) {
            ChatUtil.sendMessage(leader, "&cSeul le chef actuel peut transférer le leadership.");
            return false;
         } else if (!guild.isMember(newLeader.getUniqueId())) {
            ChatUtil.sendMessage(leader, "&cLe nouveau chef doit déjà être membre de la guilde.");
            return false;
         } else {
            guild.setLeaderUuid(newLeader.getUniqueId());
            guild.addMember(newLeader.getUniqueId(), GuildRole.LEADER);
            guild.addMember(leader.getUniqueId(), GuildRole.OFFICER);
            this.plugin.getDataManager().saveGuild(guild);
            ChatUtil.broadcast(
               "&6&l[Guildes] &e" + leader.getName() + " &aa cédé le leadership de la guilde &6" + guild.getName() + " &aà &e" + newLeader.getName() + " &a!"
            );
            return true;
         }
      } else {
         return false;
      }
   }

   public boolean promoteOrDemotePlayer(Guild guild, Player leader, OfflinePlayer target, GuildRole specificRole) {
      if (guild != null && leader != null && target != null) {
         UUID targetUuid = target.getUniqueId();
         if (!guild.isMember(targetUuid)) {
            ChatUtil.sendMessage(leader, "&cCe joueur ne fait pas partie de votre guilde.");
            return false;
         } else if (guild.getLeaderUuid().equals(targetUuid)) {
            ChatUtil.sendMessage(leader, "&cLe chef de guilde ne peut pas être rétrogradé via cette commande (utilisez &e/guild transfer <joueur>&c).");
            return false;
         } else {
            GuildRole leaderRole = guild.getRole(leader.getUniqueId());
            if (leaderRole != GuildRole.LEADER) {
               ChatUtil.sendMessage(leader, "&cSeul le Chef de guilde peut promouvoir ou rétrograder des membres.");
               return false;
            } else {
               GuildRole currentRole = guild.getRole(targetUuid);
               GuildRole newRole;
               if (specificRole != null) {
                  if (specificRole == GuildRole.LEADER) {
                     ChatUtil.sendMessage(leader, "&cPour céder le rôle de chef, utilisez &e/guild transfer <joueur>&c.");
                     return false;
                  }

                  newRole = specificRole;
               } else if (currentRole == GuildRole.OFFICER) {
                  newRole = GuildRole.MEMBER;
               } else {
                  newRole = GuildRole.OFFICER;
               }

               if (newRole == currentRole) {
                  ChatUtil.sendMessage(
                     leader,
                     "&e" + (target.getName() != null ? target.getName() : "Ce joueur") + " possède déjà le rang " + currentRole.getDisplayName() + "&e."
                  );
                  return false;
               } else {
                  guild.addMember(targetUuid, newRole);
                  PlayerData pData = this.plugin.getDataManager().getPlayerData(targetUuid);
                  if (pData != null) {
                     pData.setGuildRole(newRole);
                     this.plugin.getDataManager().savePlayer(pData);
                  }

                  this.plugin.getDataManager().saveGuild(guild);
                  if (target.isOnline() && target.getPlayer() != null) {
                     this.updatePlayerTab(target.getPlayer());
                     ChatUtil.sendMessage(
                        target.getPlayer(), "&6&l[Guilde] &eVotre rang a été modifié : vous êtes maintenant " + newRole.getDisplayName() + " &e!"
                     );
                     ChatUtil.playSound(target.getPlayer(), Sound.UI_TOAST_CHALLENGE_COMPLETE, 1.0F, 1.2F);
                  }

                  String targetName = target.getName() != null ? target.getName() : "Le joueur";
                  if (newRole == GuildRole.OFFICER) {
                     ChatUtil.broadcastGuild(
                        guild, "&6&l[Guilde] &a⭐ &e" + targetName + " &aa été promu au rang d'&eOfficier &apar le chef &e" + leader.getName() + " &a!"
                     );
                  } else {
                     ChatUtil.broadcastGuild(
                        guild,
                        "&6&l[Guilde] &e"
                           + targetName
                           + " &aa été rétrogradé au rang de simple "
                           + newRole.getDisplayName()
                           + " &epar le chef &e"
                           + leader.getName()
                           + "."
                     );
                  }

                  return true;
               }
            }
         }
      } else {
         return false;
      }
   }

   public double calculateNextClaimCost(Guild guild) {
      if (guild == null) {
         return 100.0;
      } else {
         int currentClaims = guild.getClaimedChunks().size();
         double baseCost = this.plugin.getConfig().getDouble("guilds.claim-cost-base", 100.0);
         double stepIncrease = this.plugin.getConfig().getDouble("guilds.claim-cost-increment", 25.0);
         double quadraticFactor = this.plugin.getConfig().getDouble("guilds.claim-cost-quadratic", 0.0);
         double cost = baseCost + (double)currentClaims * stepIncrease + (double)(currentClaims * currentClaims) * quadraticFactor;
         if (guild.getSpecialization() == GuildSpecialization.ARCANE_CONCLAVE) {
            cost *= 1.5;
         }

         return cost;
      }
   }

   public boolean isAdjacentToGuildClaims(Guild guild, ChunkKey chunkKey) {
      if (guild != null && chunkKey != null) {
         if (guild.getClaimedChunks().isEmpty()) {
            return true;
         } else {
            String world = chunkKey.getWorldName();
            int x = chunkKey.getX();
            int z = chunkKey.getZ();

            for (int dx = -1; dx <= 1; dx++) {
               for (int dz = -1; dz <= 1; dz++) {
                  if (dx != 0 || dz != 0) {
                     ChunkKey neighbor = new ChunkKey(world, x + dx, z + dz);
                     Guild neighborOwner = this.chunkClaims.get(neighbor);
                     if (neighborOwner != null && neighborOwner.getId().equals(guild.getId())) {
                        return true;
                     }
                  }
               }
            }

            return false;
         }
      } else {
         return false;
      }
   }

   public int getMaxClaims(Guild guild) {
      if (guild == null) {
         return 16;
      } else {
         AgeType age = guild.getCurrentAge();
         if (age == null) {
            age = AgeType.RUSTIC;
         }

         return age.getMaxClaims();
      }
   }

   public boolean claimChunk(Guild guild, Player player) {
      if (guild != null && player != null) {
         Chunk chunk = player.getLocation().getChunk();
         ChunkKey chunkKey = ChunkKey.from(chunk);
         Guild existing = this.chunkClaims.get(chunkKey);
         if (existing != null) {
            ChatUtil.sendMessage(player, "&cCe chunk est déjà revendiqué par la guilde &e" + existing.getName() + " &c!");
            return false;
         } else if (!guild.getClaimedChunks().isEmpty() && !this.isAdjacentToGuildClaims(guild, chunkKey)) {
            ChatUtil.sendMessage(
               player, "&c❌ Territoire non contigu : Vos claims doivent obligatoirement être collés (adjacents) à un chunk déjà possédé par votre guilde !"
            );
            ChatUtil.sendMessage(player, "&7(Positionnez-vous sur un chunk limitrophe à vos claims actuels pour l'acheter).");
            ChatUtil.playSound(player, Sound.ENTITY_VILLAGER_NO, 1.0F, 1.0F);
            return false;
         } else {
            if (guild.getSpecialization() == GuildSpecialization.LONE_RANGER) {
               for (BlockState state : chunk.getTileEntities()) {
                  if (GuildClaimListener.isChestMaterial(state.getType())) {
                     ChatUtil.sendMessage(
                        player,
                        "&c❌ Revendication impossible : La spécialisation Rôdeur Solitaire interdit formellement la présence de coffres dans ses claims !"
                     );
                     ChatUtil.sendMessage(
                        player, "&7Veuillez retirer tous les coffres / tonneaux / boîtes shulker présents dans ce chunk avant de le revendiquer."
                     );
                     ChatUtil.playSound(player, Sound.ENTITY_VILLAGER_NO, 1.0F, 1.0F);
                     return false;
                  }
               }
            }

            int maxClaims = this.getMaxClaims(guild);
            int currentClaims = guild.getClaimedChunks().size();
            if (maxClaims != Integer.MAX_VALUE && currentClaims >= maxClaims) {
               ChatUtil.sendMessage(
                  player,
                  "&c❌ Votre guilde a atteint sa limite maximale de claims pour l'"
                     + guild.getCurrentAge().getDisplayName()
                     + " &c(&e"
                     + maxClaims
                     + " max&c) !"
               );
               ChatUtil.sendMessage(player, "&7Passez à l'Âge supérieur avec &e/age &7pour débloquer plus de territoires (16 ➔ 32 ➔ 64 ➔ 128 ➔ Illimité).");
               ChatUtil.playSound(player, Sound.ENTITY_VILLAGER_NO, 1.0F, 1.0F);
               return false;
            } else {
               double claimCost = this.calculateNextClaimCost(guild);
               if (guild.getBankBalance() < claimCost) {
                  ChatUtil.sendMessage(
                     player,
                     "&c❌ La banque de votre guilde ne possède pas assez de fonds pour acheter ce claim n°"
                        + (currentClaims + 1)
                        + " (&e"
                        + String.format("%.2f", claimCost)
                        + " pièces &crequises, Solde : &6"
                        + String.format("%.2f", guild.getBankBalance())
                        + "&c)."
                  );
                  ChatUtil.sendMessage(player, "&7Déposez de l'argent dans la banque de guilde avec &e/guild bank <montant>&7.");
                  ChatUtil.playSound(player, Sound.ENTITY_VILLAGER_NO, 1.0F, 1.0F);
                  return false;
               } else {
                  guild.withdrawBank(claimCost);
                  guild.addClaim(chunkKey);
                  this.chunkClaims.put(chunkKey, guild);
                  this.plugin.getDataManager().saveGuild(guild);
                  String maxStr = maxClaims == Integer.MAX_VALUE ? "Illimité" : String.valueOf(maxClaims);
                  double nextCost = this.calculateNextClaimCost(guild);
                  ChatUtil.sendMessage(player, "&a✔ Chunk revendiqué avec succès pour votre guilde ! (&7X: " + chunk.getX() + ", Z: " + chunk.getZ() + "&a)");
                  ChatUtil.sendMessage(
                     player,
                     "&7• Coût prélevé (Banque) : &6"
                        + String.format("%.2f", claimCost)
                        + " pièces &8| &7Claims totaux : &e"
                        + (currentClaims + 1)
                        + "/"
                        + maxStr
                  );
                  if (maxClaims == Integer.MAX_VALUE || currentClaims + 1 < maxClaims) {
                     ChatUtil.sendMessage(
                        player, "&7• Prix du prochain claim (n°" + (currentClaims + 2) + ") : &6" + String.format("%.2f", nextCost) + " pièces&7."
                     );
                  }

                  ChatUtil.playSound(player, Sound.ENTITY_PLAYER_LEVELUP, 1.0F, 1.2F);
                  return true;
               }
            }
         }
      } else {
         return false;
      }
   }

   public boolean unclaimChunk(Guild guild, Player player) {
      if (guild != null && player != null) {
         Chunk chunk = player.getLocation().getChunk();
         ChunkKey chunkKey = ChunkKey.from(chunk);
         if (!guild.isClaimed(chunkKey)) {
            ChatUtil.sendMessage(player, "&cCe chunk n'appartient pas à votre guilde.");
            return false;
         } else {
            if (guild.hasNexus() && guild.getNexus().getLocation() != null) {
               ChunkKey nexusChunk = ChunkKey.from(guild.getNexus().getLocation().getChunk());
               if (chunkKey.equals(nexusChunk)) {
                  ChatUtil.sendMessage(player, "&c❌ Impossible d'abandonner ce chunk : le Nexus de votre guilde y est implanté !");
                  ChatUtil.sendMessage(player, "&7Déplacez d'abord votre Nexus vers un autre claim sécurisé avec &e/guild nexus place&7.");
                  return false;
               }
            }

            guild.removeClaim(chunkKey);
            this.chunkClaims.remove(chunkKey);
            this.plugin.getDataManager().saveGuild(guild);
            ChatUtil.sendMessage(player, "&eChunk abandonné avec succès.");
            return true;
         }
      } else {
         return false;
      }
   }

   public boolean setSpecialization(Guild guild, GuildSpecialization spec, Player player) {
      if (guild != null && spec != null) {
         if (guild.getSpecialization() != GuildSpecialization.NONE && guild.getSpecialization() != spec && !guild.canChangeSpecialization()) {
            ChatUtil.sendMessage(player, "&c❌ Votre guilde ne peut pas changer de spécialisation pour le moment !");
            ChatUtil.sendMessage(player, "&7Un &enouveau joueur &7(qui n'a jamais appartenu à votre guilde) doit la rejoindre pour débloquer un changement.");
            ChatUtil.playSound(player, Sound.ENTITY_VILLAGER_NO, 1.0F, 1.0F);
            return false;
         } else {
            Map<Race, Integer> raceCounts = new EnumMap<>(Race.class);

            for (UUID memberUuid : guild.getMembers().keySet()) {
               Race r = this.plugin.getRaceManager().getRace(memberUuid);
               raceCounts.put(r, raceCounts.getOrDefault(r, 0) + 1);
            }

            if (!spec.validateComposition(raceCounts, guild.getMembers().size())) {
               ChatUtil.sendMessage(player, "&cLa composition actuelle de votre guilde ne respecte pas les contraintes de cette spécialisation !");
               return false;
            } else {
               if (spec == GuildSpecialization.LONE_RANGER) {
                  if (guild.getMembers().size() != 1) {
                     ChatUtil.sendMessage(player, "&c❌ La spécialisation Rôdeur Solitaire exige d'être strictement le seul membre de la guilde !");
                     return false;
                  }

                  if (this.plugin.getRaceManager().getRace(player.getUniqueId()) != Race.RANGER) {
                     ChatUtil.sendMessage(player, "&c❌ Seul un Rôdeur peut choisir la spécialisation Rôdeur Solitaire !");
                     return false;
                  }

                  PlayerData pData = this.plugin.getDataManager().getPlayerData(player.getUniqueId());
                  if (pData != null && pData.hasJoinedGuildBefore()) {
                     ChatUtil.sendMessage(
                        player, "&c❌ Vous ne pouvez pas adopter la voie du Rôdeur Solitaire car vous avez déjà appartenu à une guilde par le passé !"
                     );
                     return false;
                  }
               }

               GuildSpecialization oldSpec = guild.getSpecialization();
               guild.setSpecialization(spec);
               guild.setCanChangeSpecialization(false);
               if (guild.hasNexus()) {
                  Nexus nexus = guild.getNexus();
                  int baseHealth = this.plugin.getConfig().getInt("guilds.nexus.base-health", 5000);
                  int targetMaxHealth = baseHealth;
                  if (spec == GuildSpecialization.MERCHANT_SYNDICATE) {
                     targetMaxHealth = (int)((double)baseHealth * 0.8);
                  } else if (spec == GuildSpecialization.ARCANE_CONCLAVE) {
                     targetMaxHealth = (int)((double)baseHealth * 1.25);
                  } else if (spec == GuildSpecialization.LONE_RANGER) {
                     targetMaxHealth = 2500;
                  }

                  nexus.setMaxHealth(targetMaxHealth);
                  nexus.setCurrentHealth(Math.min(nexus.getCurrentHealth(), targetMaxHealth));
                  this.plugin.getNexusManager().updateHologram(guild);
               }

               if (oldSpec == GuildSpecialization.ARCANE_CONCLAVE && spec != GuildSpecialization.ARCANE_CONCLAVE) {
                  for (UUID memberUuid : guild.getMembers().keySet()) {
                     Player member = Bukkit.getPlayer(memberUuid);
                     if (member != null && member.isOnline()) {
                        RaceCombatListener.sanitizePlayerEnchantments(member, false);
                     }
                  }
               }

               for (UUID memberUuidx : guild.getMembers().keySet()) {
                  Player member = Bukkit.getPlayer(memberUuidx);
                  if (member != null && member.isOnline()) {
                     this.plugin.getRaceManager().applyRaceAttributes(member, this.plugin.getRaceManager().getRace(member));
                  }
               }

               this.plugin.getDataManager().saveGuild(guild);
               ChatUtil.sendMessage(player, "&aSpécialisation de guilde mise à jour avec succès : " + spec.getDisplayName());
               return true;
            }
         }
      } else {
         return false;
      }
   }

   public boolean invitePlayer(Guild guild, Player target, Player sender) {
      if (guild != null && target != null) {
         if (guild.getSpecialization() == GuildSpecialization.LONE_RANGER) {
            if (sender != null) {
               ChatUtil.sendMessage(sender, "&c❌ La spécialisation Rôdeur Solitaire interdit formellement d'inviter d'autres membres (1 membre max) !");
            }

            return false;
         } else {
            Race targetRace = this.plugin.getRaceManager().getRace(target);
            if (guild.getSpecialization() == GuildSpecialization.MERCHANT_SYNDICATE && targetRace == Race.ARCANIST) {
               if (sender != null) {
                  ChatUtil.sendMessage(
                     sender, "&c❌ " + target.getName() + " est Arcaniste : le Syndicat Marchand interdit formellement les Arcanistes dans ses rangs !"
                  );
               }

               return false;
            } else {
               this.pendingInvites.computeIfAbsent(target.getUniqueId(), k -> new HashSet<>()).add(guild.getId());
               ChatUtil.sendMessage(
                  target,
                  "&aVous avez reçu une invitation à rejoindre la guilde &6"
                     + guild.getName()
                     + " &a! Faites &e/guild join "
                     + guild.getName()
                     + " &apour accepter."
               );
               return true;
            }
         }
      } else {
         return false;
      }
   }

   public void invitePlayer(Guild guild, Player target) {
      this.invitePlayer(guild, target, null);
   }

   public boolean hasInvite(Player player, Guild guild) {
      Set<String> invites = this.pendingInvites.get(player.getUniqueId());
      return invites != null && invites.contains(guild.getId());
   }

   public void acceptInvite(Player player, Guild guild) {
      if (guild != null) {
         if (guild.getSpecialization() == GuildSpecialization.LONE_RANGER) {
            ChatUtil.sendMessage(player, "&c❌ Cette guilde est sous la spécialisation Rôdeur Solitaire et ne peut accueillir aucun autre membre !");
            ChatUtil.playSound(player, Sound.ENTITY_VILLAGER_NO, 1.0F, 1.0F);
         } else {
            Race playerRace = this.plugin.getRaceManager().getRace(player);
            if (guild.getSpecialization() == GuildSpecialization.MERCHANT_SYNDICATE && playerRace == Race.ARCANIST) {
               ChatUtil.sendMessage(player, "&c❌ Vous êtes Arcaniste : le Syndicat Marchand interdit formellement les Arcanistes dans ses rangs !");
               ChatUtil.playSound(player, Sound.ENTITY_VILLAGER_NO, 1.0F, 1.0F);
            } else {
               this.pendingInvites.computeIfAbsent(player.getUniqueId(), k -> new HashSet<>()).remove(guild.getId());
               PlayerData data = this.plugin.getDataManager().getPlayerData(player.getUniqueId());
               if (data != null) {
                  data.setGuildId(guild.getId());
                  data.setGuildRole(GuildRole.RECRUIT);
                  data.setHasJoinedGuildBefore(true);
                  guild.addMember(player.getUniqueId(), GuildRole.RECRUIT);
                  this.playerGuildMap.put(player.getUniqueId(), guild.getId());
                  if (!guild.isHistoricalMember(player.getUniqueId())) {
                     guild.addHistoricalMember(player.getUniqueId());
                     guild.setCanChangeSpecialization(true);
                     ChatUtil.sendMessage(player, "&a✨ Votre arrivée en tant que nouveau membre débloque un changement de spécialisation pour la guilde !");
                  }

                  this.plugin.getDataManager().saveGuild(guild);
                  this.plugin.getDataManager().savePlayer(data);
                  this.updatePlayerTab(player);
                  this.plugin.getRaceManager().applyRaceAttributes(player, this.plugin.getRaceManager().getRace(player));
                  ChatUtil.sendMessage(player, "&aVous avez rejoint la guilde &6" + guild.getName() + " &a!");
               }
            }
         }
      }
   }

   public void registerAnnexation(Guild defeatedGuild, Guild victorGuild) {
      if (defeatedGuild != null && victorGuild != null) {
         Set<ChunkKey> chunksToTransfer = new HashSet<>(defeatedGuild.getClaimedChunks());

         for (ChunkKey ck : chunksToTransfer) {
            defeatedGuild.removeClaim(ck);
            this.chunkClaims.remove(ck);
            victorGuild.addClaim(ck);
            this.chunkClaims.put(ck, victorGuild);
         }

         double transferredBank = defeatedGuild.getBankBalance();
         if (transferredBank > 0.0) {
            victorGuild.depositBank(transferredBank);
            defeatedGuild.withdrawBank(transferredBank);
         }

         this.plugin.getNexusManager().removeNexus(defeatedGuild);

         for (UUID memberUuid : defeatedGuild.getMembers().keySet()) {
            this.playerGuildMap.remove(memberUuid);
            PlayerData pData = this.plugin.getDataManager().getPlayerData(memberUuid);
            if (pData != null) {
               pData.setGuildId(null);
               pData.setGuildRole(null);
               this.plugin.getDataManager().savePlayer(pData);
            }

            this.pendingAnnexationChoices.put(memberUuid, victorGuild.getId());
            Player member = Bukkit.getPlayer(memberUuid);
            if (member != null && member.isOnline()) {
               AnnexationChoiceGUI.open(member, victorGuild, this.plugin);
               ChatUtil.sendMessage(member, "&8&m--------------------------------------------------");
               ChatUtil.sendMessage(member, "&4&l\ud83d\udca5 VOTRE GUILDE A ÉTÉ VAINCUE PAR LA GUILDE &6" + victorGuild.getName() + " &4&l!");
               ChatUtil.sendMessage(member, "&eTous vos territoires ont été &cANNEXÉS &epar les vainqueurs (" + chunksToTransfer.size() + " claims).");
               if (transferredBank > 0.0) {
                  ChatUtil.sendMessage(
                     member, "&6\ud83d\udcb0 Trésorerie de guilde pillée et fusionnée : &e" + String.format("%.2f", transferredBank) + " pièces."
                  );
               }

               ChatUtil.sendMessage(member, "&7Options d'avenir :");
               ChatUtil.sendMessage(member, "&a• Rejoindre le vainqueur : &e/guild annex join");
               ChatUtil.sendMessage(member, "&c• Rester indépendant : &e/guild annex decline");
               ChatUtil.sendMessage(member, "&8&m--------------------------------------------------");
            }
         }

         this.guildsById.remove(defeatedGuild.getId());
         this.guildsByName.remove(defeatedGuild.getName().toLowerCase());
         this.plugin.getDataManager().deleteGuild(defeatedGuild.getId());
         this.plugin.getDataManager().saveGuild(victorGuild);
         this.updateAllPlayerTabs();
         ChatUtil.broadcast(
            "&4&l⚔️ [ANNEXION] &eLa guilde &6"
               + victorGuild.getName()
               + " &aa conquis et annexé tous les territoires de &c"
               + defeatedGuild.getName()
               + " &a("
               + chunksToTransfer.size()
               + " claims) !"
         );
         if (transferredBank > 0.0) {
            ChatUtil.broadcast(
               "&6\ud83d\udcb0 [TRÉSOR DE GUERRE] &eLa totalité de la trésorerie de &c"
                  + defeatedGuild.getName()
                  + " &8(&6"
                  + String.format("%.2f", transferredBank)
                  + " pièces&8) &aa été fusionnée dans la Banque de &6"
                  + victorGuild.getName()
                  + " &a!"
            );
         }
      }
   }

   public boolean acceptAnnexation(Player player) {
      if (player == null) {
         return false;
      } else {
         String victorGuildId = this.pendingAnnexationChoices.remove(player.getUniqueId());
         if (victorGuildId == null) {
            ChatUtil.sendMessage(player, "&cVous n'avez aucune offre d'annexion en attente.");
            return false;
         } else {
            Guild victorGuild = this.getGuildById(victorGuildId);
            if (victorGuild == null) {
               ChatUtil.sendMessage(player, "&cLa guilde victorieuse n'existe plus.");
               return false;
            } else {
               PlayerData pData = this.plugin.getDataManager().getPlayerData(player.getUniqueId());
               if (pData != null) {
                  pData.setGuildId(victorGuild.getId());
                  pData.setGuildRole(GuildRole.MEMBER);
                  victorGuild.addMember(player.getUniqueId(), GuildRole.MEMBER);
                  this.playerGuildMap.put(player.getUniqueId(), victorGuild.getId());
                  if (!victorGuild.isHistoricalMember(player.getUniqueId())) {
                     victorGuild.addHistoricalMember(player.getUniqueId());
                     victorGuild.setCanChangeSpecialization(true);
                  }

                  this.plugin.getDataManager().saveGuild(victorGuild);
                  this.plugin.getDataManager().savePlayer(pData);
                  this.updatePlayerTab(player);
                  ChatUtil.broadcast(
                     "&a&l[Allégeance] &e" + player.getName() + " &aa prêté allégeance et a rejoint la guilde victorieuse &6" + victorGuild.getName() + " &a!"
                  );
                  ChatUtil.playSound(player, Sound.UI_TOAST_CHALLENGE_COMPLETE, 1.0F, 1.0F);
                  return true;
               } else {
                  return false;
               }
            }
         }
      }
   }

   public boolean declineAnnexation(Player player) {
      if (player == null) {
         return false;
      } else {
         String victorGuildId = this.pendingAnnexationChoices.remove(player.getUniqueId());
         if (victorGuildId == null) {
            ChatUtil.sendMessage(player, "&cVous n'avez aucune offre d'annexion en attente.");
            return false;
         } else {
            this.updatePlayerTab(player);
            ChatUtil.sendMessage(player, "&aVous avez refusé de rejoindre la guilde victorieuse. Vous êtes désormais libre et sans guilde.");
            ChatUtil.playSound(player, Sound.ENTITY_EXPERIENCE_ORB_PICKUP, 1.0F, 1.0F);
            return true;
         }
      }
   }

   public void updatePlayerTab(Player player) {
      if (player != null && player.isOnline()) {
         Guild guild = this.getGuildByPlayer(player.getUniqueId());
         if (guild != null) {
            player.playerListName(ChatUtil.component("&8[&6" + guild.getTag() + "&8] &f" + player.getName()));
         } else {
            player.playerListName(ChatUtil.component("&8[&7Sans Guilde&8] &f" + player.getName()));
         }
      }
   }

   public void updateAllPlayerTabs() {
      for (Player p : Bukkit.getOnlinePlayers()) {
         this.updatePlayerTab(p);
      }
   }

   public String getPendingAnnexationVictor(UUID playerUuid) {
      return this.pendingAnnexationChoices.get(playerUuid);
   }

   public boolean isEconomicHegemony(Guild targetGuild) {
      if (targetGuild == null) {
         return false;
      } else {
         Set<UUID> members = targetGuild.getMembers().keySet();
         if (!members.isEmpty()) {
            List<UUID> top5Uuids = this.plugin.getDataManager().getTopPlayerUuids(5);
            int top5Count = 0;

            for (UUID memberUuid : members) {
               if (top5Uuids.contains(memberUuid)) {
                  top5Count++;
               }
            }

            if ((double)top5Count / (double)members.size() >= 0.5) {
               return true;
            }
         }

         double totalServerWealth = this.plugin.getDataManager().getTotalServerWealth();
         if (totalServerWealth > 0.0) {
            double share = targetGuild.getBankBalance() / totalServerWealth;
            if (share >= 0.3) {
               return true;
            }
         }

         return false;
      }
   }
}
