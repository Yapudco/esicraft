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
   private final Map<UUID, Map<String, Long>> pendingInvites = new ConcurrentHashMap<>();
   private static final long INVITE_TTL_MS = 300000L;
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
               if (this.plugin.getCombatManager().denySpending(player)) {
                  return false;
               } else if (data.getCoins() < cost) {
                  ChatUtil.sendMessage(player, "&cVous n'avez pas assez d'argent pour fonder une guilde (&e" + cost + " pièces &crequises).");
                  return false;
               } else {
                  data.removeCoins(cost);
                  this.plugin.getDataManager().savePlayer(data);
                  String guildId = UUID.randomUUID().toString();
                  Guild guild = new Guild(guildId, name, tag, player.getUniqueId());
                  data.setGuildId(guildId);
                  data.setGuildRole(GuildRole.LEADER);
                  this.registerGuild(guild);
                  this.plugin.getDataManager().saveGuild(guild);
                  this.plugin.getDataManager().savePlayer(data);
                  this.updatePlayerTab(player);
                  ChatUtil.broadcast("&6&l[Guildes] &e" + player.getName() + " &aa fondé la guilde &6" + name + " &8[&e" + tag + "&8] &a!");
                  this.plugin.getChronicle().log(fr.esicraft.life.Chronicle.Kind.GUILD, player.getName() + " fonde la guilde " + name + " [" + tag + "].", guild.getId(), null);
                  this.plugin.getChronicle().ageReached(guild, 1);
                  this.plugin.getAchievements().grant(player.getUniqueId(), fr.esicraft.life.Achievement.FOUNDER);
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

   /** Vérifie qu'un joueur peut dissoudre sa guilde (pas en plein raid / attaque de Nexus). */
   public boolean canDisbandNow(Guild guild, Player player) {
      if (guild != null && this.plugin.getLoans().hasDebt(guild.getId())) {
         if (player != null) {
            ChatUtil.sendMessage(player, "&c❌ Impossible de dissoudre la guilde : elle a une dette en cours (&e/banque dettes&c).");
            ChatUtil.playSound(player, Sound.ENTITY_VILLAGER_NO, 1.0F, 1.0F);
         }

         return false;
      }

      if (guild != null && guild.hasNexus()) {
         if (this.plugin.getRaidManager().isGuildUnderRaid(guild.getId()) || this.plugin.getNexusManager().wasNexusRecentlyAttacked(guild, 600000L)) {
            if (player != null) {
               ChatUtil.sendMessage(player, "&c❌ Impossible de quitter ou de dissoudre la guilde : votre Nexus est attaqué ou a été attaqué il y a moins de 10 minutes !");
               ChatUtil.playSound(player, Sound.ENTITY_VILLAGER_NO, 1.0F, 1.0F);
            }

            return false;
         }
      }

      return true;
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
         this.plugin.getBourse().onGuildDisbanded(guild.getId());
         this.plugin.getNexusTech().onGuildDisbanded(guild.getId());
         for (UUID memberUuid : guild.getMembers().keySet()) {
            Player online = Bukkit.getPlayer(memberUuid);
            if (online != null) {
               Bukkit.getScheduler().runTask(this.plugin, () -> this.plugin.getAgeDimension().checkAndExpelIfUnderage(online));
            }
         }
         this.updateAllPlayerTabs();
         ChatUtil.broadcast("&c&l[Guildes] &7La guilde &c" + guild.getName() + " &7a été dissoute.");
         this.plugin.getChronicle().log(fr.esicraft.life.Chronicle.Kind.GUILD, "La guilde " + guild.getName() + " est dissoute.", guild.getId(), null);
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
               if (!this.canDisbandNow(guild, player)) {
                  return false;
               }

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
         } else if (!this.canDisbandNow(guild, player)) {
            // Pas de départ en plein raid : sinon on vide la guilde pour priver l'adversaire de l'annexion.
            return false;
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
            this.plugin.getAgeDimension().checkAndExpelIfUnderage(player);
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
            } else if (!this.canDisbandNow(guild, officer)) {
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
                  this.plugin.getAgeDimension().checkAndExpelIfUnderage(target.getPlayer());
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
            PlayerData newLeaderData = this.plugin.getDataManager().getPlayerData(newLeader.getUniqueId());
            if (newLeaderData != null) {
               newLeaderData.setGuildRole(GuildRole.LEADER);
               this.plugin.getDataManager().savePlayer(newLeaderData);
            }

            PlayerData oldLeaderData = this.plugin.getDataManager().getPlayerData(leader.getUniqueId());
            if (oldLeaderData != null) {
               oldLeaderData.setGuildRole(GuildRole.OFFICER);
               this.plugin.getDataManager().savePlayer(oldLeaderData);
            }

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

         cost *= this.plugin.getSynergy().claimCostMultiplier(guild);
         if (this.plugin.getCourt() != null && this.plugin.getCourt().is(fr.esicraft.game.CourtManager.Rule.BUILD_BONUS)) {
            cost *= 0.75;
         }

         return cost;
      }
   }

   /** Un chunk d'une autre guilde (sans traité avec la nôtre) se trouve-t-il dans le tampon de {@code claims.enemy-buffer-chunks} chunks ? Évite l'encerclement. */
   private boolean hasEnemyNeighbor(Guild guild, ChunkKey key) {
      int buffer = Math.max(0, this.plugin.getConfig().getInt("claims.enemy-buffer-chunks", 1));
      for (int dx = -buffer; dx <= buffer; dx++) {
         for (int dz = -buffer; dz <= buffer; dz++) {
            Guild other = this.chunkClaims.get(new ChunkKey(key.getWorldName(), key.getX() + dx, key.getZ() + dz));
            if (other != null && !other.getId().equals(guild.getId()) && this.plugin.getTreatyManager().between(guild.getId(), other.getId()) == null) {
               return true;
            }
         }
      }

      return false;
   }

   private final java.util.Map<String, Double> officerSpend = new java.util.concurrent.ConcurrentHashMap<>();

   /** Plafond quotidien de dépenses (claims, pièges) d'un officier ; le chef de guilde n'a pas de limite. */
   public boolean officerMaySpend(Guild guild, Player player, double amount) {
      if (guild.getLeaderUuid().equals(player.getUniqueId())) {
         return true;
      }

      double cap = this.plugin.getConfig().getDouble("guilds.officer-daily-spend-cap", 3000.0);
      if (cap <= 0.0) {
         return true;
      }

      long day = java.time.LocalDate.now().toEpochDay();
      String key = guild.getId() + "|" + player.getUniqueId() + "|" + day;
      synchronized (this.officerSpend) {
         this.officerSpend.keySet().removeIf(k -> !k.endsWith("|" + day));
         double current = this.officerSpend.getOrDefault(key, 0.0);
         if (current + amount > cap) {
            return false;
         }

         this.officerSpend.put(key, current + amount);
         return true;
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

            // Adjacence orthogonale uniquement (pas de « serpents » en diagonale) pour les NOUVEAUX claims.
            for (int dx = -1; dx <= 1; dx++) {
               for (int dz = -1; dz <= 1; dz++) {
                  if ((dx == 0) != (dz == 0)) {
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
         } else if (this.hasEnemyNeighbor(guild, chunkKey)) {
            ChatUtil.sendMessage(player, "&c❌ Trop près d'un territoire ennemi : laissez au moins &e" + this.plugin.getConfig().getInt("claims.enemy-buffer-chunks", 1)
               + " chunk &cde distance (sauf avec une guilde liée par un traité).");
            ChatUtil.playSound(player, Sound.ENTITY_VILLAGER_NO, 1.0F, 1.0F);
            return false;
         } else if (this.plugin.getLoans().hasOverdue(guild.getId())) {
            ChatUtil.sendMessage(player, "&c❌ Votre guilde a une dette impayée : plus aucune revendication tant qu'elle n'est pas remboursée (&e/banque dettes&c).");
            ChatUtil.playSound(player, Sound.ENTITY_VILLAGER_NO, 1.0F, 1.0F);
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
               } else if (!this.officerMaySpend(guild, player, claimCost)) {
                  ChatUtil.sendMessage(player, "&c❌ Plafond quotidien de dépenses des officiers atteint (&e" + this.plugin.getConfig().getDouble("guilds.officer-daily-spend-cap", 3000.0)
                     + " pièces&c) : seul le chef de guilde peut dépenser davantage aujourd'hui.");
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
                  this.plugin.getAchievements().grant(player.getUniqueId(), fr.esicraft.life.Achievement.FIRST_CLAIM);
                  this.grantClaimAchievements(guild);
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

   /** Succès de superficie : 16, 32, 64 … 2048 chunks, accordés à tous les membres de la guilde. */
   private void grantClaimAchievements(Guild guild) {
      int size = guild.getClaimedChunks().size();
      for (fr.esicraft.life.Achievement a : fr.esicraft.life.Achievement.values()) {
         if (a.name().startsWith("CLAIMS_")) {
            try {
               if (size >= Integer.parseInt(a.name().substring(7))) {
                  this.plugin.getAchievements().grantGuild(guild, a, false);
               }
            } catch (NumberFormatException ignored) {
            }
         }
      }
   }

   /** Vrai si le territoire de la guilde reste d'un seul tenant (voisinage 8, comme la règle d'adjacence) sans ce chunk. */
   private boolean remainsConnectedWithout(Guild guild, ChunkKey removed) {
      Set<ChunkKey> remaining = new HashSet<>(guild.getClaimedChunks());
      remaining.remove(removed);
      if (remaining.size() <= 1) {
         return true;
      } else {
         java.util.ArrayDeque<ChunkKey> queue = new java.util.ArrayDeque<>();
         Set<ChunkKey> seen = new HashSet<>();
         ChunkKey start = remaining.iterator().next();
         queue.add(start);
         seen.add(start);

         while (!queue.isEmpty()) {
            ChunkKey current = queue.poll();

            for (int dx = -1; dx <= 1; dx++) {
               for (int dz = -1; dz <= 1; dz++) {
                  if (dx != 0 || dz != 0) {
                     ChunkKey neighbor = new ChunkKey(current.getWorldName(), current.getX() + dx, current.getZ() + dz);
                     if (remaining.contains(neighbor) && seen.add(neighbor)) {
                        queue.add(neighbor);
                     }
                  }
               }
            }
         }

         return seen.size() == remaining.size();
      }
   }

   /**
    * Saisie d'un chunk pour dette impayée : retire un chunk qui n'abrite pas le Nexus et dont la perte ne coupe pas le territoire
    * (de préférence en périphérie). Renvoie le chunk retiré, ou null si aucun ne peut l'être.
    */
   public ChunkKey seizeOneChunk(Guild guild) {
      if (guild == null || guild.getClaimedChunks().size() <= 1) {
         return null;
      }

      ChunkKey nexusChunk = guild.hasNexus() && guild.getNexus().getLocation() != null ? ChunkKey.from(guild.getNexus().getLocation().getChunk()) : null;
      ChunkKey best = null;
      int bestNeighbors = Integer.MAX_VALUE;
      for (ChunkKey ck : guild.getClaimedChunks()) {
         if (ck.equals(nexusChunk) || !this.remainsConnectedWithout(guild, ck)) {
            continue;
         }

         int neighbors = 0;
         for (int dx = -1; dx <= 1; dx++) {
            for (int dz = -1; dz <= 1; dz++) {
               if ((dx != 0 || dz != 0) && guild.isClaimed(new ChunkKey(ck.getWorldName(), ck.getX() + dx, ck.getZ() + dz))) {
                  neighbors++;
               }
            }
         }

         if (neighbors < bestNeighbors) {
            bestNeighbors = neighbors;
            best = ck;
         }
      }

      if (best != null) {
         guild.removeClaim(best);
         this.chunkClaims.remove(best);
         this.plugin.getDataManager().saveGuild(guild);
      }

      return best;
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
                  ChatUtil.sendMessage(player, "&7Un Nexus ne peut pas être déplacé : le chunk qui l'abrite ne peut pas être abandonné.");
                  return false;
               }
            }

            if (!this.remainsConnectedWithout(guild, chunkKey)) {
               ChatUtil.sendMessage(player, "&c❌ Impossible d'abandonner ce chunk : il couperait votre territoire en plusieurs morceaux non contigus !");
               ChatUtil.playSound(player, Sound.ENTITY_VILLAGER_NO, 1.0F, 1.0F);
               return false;
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

   private long specCooldownLeftMs(Guild guild) {
      long days = this.plugin.getConfig().getLong("guilds.spec-change-cooldown-days", 7L);
      Long changed = this.specChangedAt.get(guild.getId());
      return days <= 0L || changed == null ? 0L : Math.max(0L, changed + days * 86400000L - System.currentTimeMillis());
   }

   public boolean setSpecialization(Guild guild, GuildSpecialization spec, Player player) {
      if (guild != null && spec != null && guild.getSpecialization() != GuildSpecialization.NONE && guild.getSpecialization() != spec) {
         long left = this.specCooldownLeftMs(guild);
         if (left > 0L && !this.plugin.bypassCooldowns(player)) {
            ChatUtil.sendMessage(player, "&c❌ La guilde a changé de spécialisation récemment : encore &e" + fr.esicraft.treaty.TreatyManager.formatDuration(left) + "&c d'attente.");
            ChatUtil.playSound(player, Sound.ENTITY_VILLAGER_NO, 1.0F, 1.0F);
            return false;
         }
      }

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
               if (spec == GuildSpecialization.ATLANTEAN && guild.hasNexus() && !fr.esicraft.nexus.NexusManager.isSubmerged(guild.getNexus().getLocation())) {
                  ChatUtil.sendMessage(player, "&c❌ Les Atlantes exigent un Nexus immergé dans l'eau : le vôtre est à sec.");
                  return false;
               }

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
               String cdKey = "speccd:" + guild.getId();
               this.specChangedAt.put(guild.getId(), System.currentTimeMillis());
               Bukkit.getScheduler().runTaskAsynchronously(this.plugin, () -> this.plugin.getDataManager().getStorage()
                  .exec("INSERT INTO kv (k, v) VALUES (?, ?) ON CONFLICT(k) DO UPDATE SET v = excluded.v;", cdKey, String.valueOf(System.currentTimeMillis())));
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
            } else if (!this.canJoinComposition(guild, target.getUniqueId())) {
               if (sender != null) {
                  ChatUtil.sendMessage(
                     sender, "&c❌ L'arrivée de " + target.getName() + " (métier " + targetRace.getDisplayName() + ") est impossible : guilde pleine (" + this.getMaxMembers() + " max) ou contraintes de " + guild.getSpecialization().getDisplayName() + "&c !"
                  );
               }

               return false;
            } else {
               this.pendingInvites.computeIfAbsent(target.getUniqueId(), k -> new ConcurrentHashMap<>()).put(guild.getId(), System.currentTimeMillis() + INVITE_TTL_MS);
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

   public int getMaxMembers() {
      return Math.max(1, this.plugin.getConfig().getInt("guilds.max-members-default", 10));
   }

   /** Vérifie que l'arrivée d'un joueur (de ce métier) respecte la spécialisation de la guilde. */
   public boolean canJoinComposition(Guild guild, UUID joiningUuid) {
      if (guild == null) {
         return false;
      } else if (!guild.isMember(joiningUuid) && guild.getMembers().size() >= this.getMaxMembers()) {
         return false;
      } else if (guild.getSpecialization() == GuildSpecialization.NONE) {
         return true;
      } else {
         Map<Race, Integer> counts = new EnumMap<>(Race.class);

         for (UUID memberUuid : guild.getMembers().keySet()) {
            Race r = this.plugin.getRaceManager().getRace(memberUuid);
            counts.merge(r, 1, Integer::sum);
         }

         if (!guild.isMember(joiningUuid)) {
            counts.merge(this.plugin.getRaceManager().getRace(joiningUuid), 1, Integer::sum);
         }

         int total = guild.isMember(joiningUuid) ? guild.getMembers().size() : guild.getMembers().size() + 1;
         return guild.getSpecialization().validateComposition(counts, total);
      }
   }

   public boolean hasInvite(Player player, Guild guild) {
      Map<String, Long> invites = this.pendingInvites.get(player.getUniqueId());
      if (invites == null) {
         return false;
      }

      Long expiry = invites.get(guild.getId());
      if (expiry == null) {
         return false;
      }

      if (System.currentTimeMillis() > expiry) {
         invites.remove(guild.getId());
         return false;
      }

      return true;
   }

   public void acceptInvite(Player player, Guild guild) {
      if (guild != null) {
         if (this.getGuildByPlayer(player.getUniqueId()) != null) {
            ChatUtil.sendMessage(player, "&c❌ Vous êtes déjà dans une guilde ! Quittez-la d'abord avec &e/guild leave&c.");
            ChatUtil.playSound(player, Sound.ENTITY_VILLAGER_NO, 1.0F, 1.0F);
         } else if (!this.hasInvite(player, guild)) {
            ChatUtil.sendMessage(player, "&c❌ Cette invitation a expiré ou n'existe pas.");
            ChatUtil.playSound(player, Sound.ENTITY_VILLAGER_NO, 1.0F, 1.0F);
         } else if (guild.getSpecialization() == GuildSpecialization.LONE_RANGER) {
            ChatUtil.sendMessage(player, "&c❌ Cette guilde est sous la spécialisation Rôdeur Solitaire et ne peut accueillir aucun autre membre !");
            ChatUtil.playSound(player, Sound.ENTITY_VILLAGER_NO, 1.0F, 1.0F);
         } else {
            Race playerRace = this.plugin.getRaceManager().getRace(player);
            if (guild.getSpecialization() == GuildSpecialization.MERCHANT_SYNDICATE && playerRace == Race.ARCANIST) {
               ChatUtil.sendMessage(player, "&c❌ Vous êtes Arcaniste : le Syndicat Marchand interdit formellement les Arcanistes dans ses rangs !");
               ChatUtil.playSound(player, Sound.ENTITY_VILLAGER_NO, 1.0F, 1.0F);
            } else if (!this.canJoinComposition(guild, player.getUniqueId())) {
               ChatUtil.sendMessage(player, "&c❌ Impossible de rejoindre : guilde pleine (" + this.getMaxMembers() + " membres max) ou métier incompatible avec " + guild.getSpecialization().getDisplayName() + "&c !");
               ChatUtil.playSound(player, Sound.ENTITY_VILLAGER_NO, 1.0F, 1.0F);
            } else {
               this.pendingInvites.remove(player.getUniqueId());
               PlayerData data = this.plugin.getDataManager().getPlayerData(player.getUniqueId());
               if (data != null) {
                  data.setGuildId(guild.getId());
                  data.setGuildRole(GuildRole.RECRUIT);
                  data.setHasJoinedGuildBefore(true);
                  guild.addMember(player.getUniqueId(), GuildRole.RECRUIT);
                  this.playerGuildMap.put(player.getUniqueId(), guild.getId());
                  if (!guild.isHistoricalMember(player.getUniqueId())) {
                     guild.addHistoricalMember(player.getUniqueId());
                     long hours = player.getStatistic(org.bukkit.Statistic.PLAY_ONE_MINUTE) / 72000L;
                     long minHours = this.plugin.getConfig().getLong("guilds.spec-change-min-hours", 2L);
                     if (hours >= minHours) {
                        guild.setCanChangeSpecialization(true);
                        ChatUtil.sendMessage(player, "&a✨ Votre arrivée en tant que nouveau membre débloque un changement de spécialisation pour la guilde !");
                     } else {
                        ChatUtil.sendMessage(player, "&7Un nouveau membre ne débloque un changement de spécialisation qu'après &e" + minHours + " h &7de jeu.");
                     }
                  }

                  this.plugin.getDataManager().saveGuild(guild);
                  this.plugin.getDataManager().savePlayer(data);
                  this.updatePlayerTab(player);
                  this.plugin.getRaceManager().applyRaceAttributes(player, this.plugin.getRaceManager().getRace(player));
                  ChatUtil.sendMessage(player, "&aVous avez rejoint la guilde &6" + guild.getName() + " &a!");
                  this.plugin.getAchievements().grant(player.getUniqueId(), fr.esicraft.life.Achievement.RECRUIT);
               }
            }
         }
      }
   }

   public void registerAnnexation(Guild defeatedGuild, Guild victorGuild) {
      if (defeatedGuild != null && victorGuild != null) {
         Set<ChunkKey> chunksToTransfer = new HashSet<>();
         int maxVictorClaims = this.getMaxClaims(victorGuild);

         for (ChunkKey ck : new HashSet<>(defeatedGuild.getClaimedChunks())) {
            defeatedGuild.removeClaim(ck);
            this.chunkClaims.remove(ck);
            // Le plafond de claims lié à l'Âge s'applique aussi aux annexions (les chunks en trop redeviennent sauvages)
            if (maxVictorClaims == Integer.MAX_VALUE || victorGuild.getClaimedChunks().size() < maxVictorClaims) {
               victorGuild.addClaim(ck);
               this.chunkClaims.put(ck, victorGuild);
               chunksToTransfer.add(ck);
            }
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
         } else if (this.getGuildByPlayer(player.getUniqueId()) != null) {
            ChatUtil.sendMessage(player, "&cVous êtes déjà dans une guilde : l'offre d'annexion n'est plus valable.");
            return false;
         } else {
            Guild victorGuild = this.getGuildById(victorGuildId);
            if (victorGuild == null) {
               ChatUtil.sendMessage(player, "&cLa guilde victorieuse n'existe plus.");
               return false;
            } else if (victorGuild.getSpecialization() == GuildSpecialization.LONE_RANGER || !this.canJoinComposition(victorGuild, player.getUniqueId())) {
               ChatUtil.sendMessage(player, "&c❌ Votre métier ne respecte pas les contraintes de la guilde victorieuse : vous restez sans guilde.");
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

   /** À la connexion : aligne les données joueur sur l'état réel des guildes (expulsion/dissolution/annexion hors-ligne). */
   public void reconcilePlayerGuild(Player player, PlayerData data) {
      if (player != null && data != null) {
         UUID uuid = player.getUniqueId();
         String stored = data.getGuildId();
         Guild actual = stored != null ? this.guildsById.get(stored) : null;
         if (actual == null || !actual.isMember(uuid)) {
            actual = null;

            for (Guild candidate : this.guildsById.values()) {
               if (candidate.isMember(uuid)) {
                  actual = candidate;
                  break;
               }
            }
         }

         if (actual == null) {
            this.playerGuildMap.remove(uuid);
            if (stored != null) {
               data.setGuildId(null);
               data.setGuildRole(null);
               data.setHasJoinedGuildBefore(true);
               this.plugin.getDataManager().savePlayer(data);
            }
         } else {
            this.playerGuildMap.put(uuid, actual.getId());
            if (!actual.getId().equals(stored)) {
               data.setGuildId(actual.getId());
               data.setGuildRole(actual.getRole(uuid));
               this.plugin.getDataManager().savePlayer(data);
            }
         }
      }
   }

   public void updatePlayerTab(Player player) {
      if (player != null && player.isOnline() && this.plugin.getDisguiseManager().get(player.getUniqueId()) == null) {
         Guild guild = this.getGuildByPlayer(player.getUniqueId());
         if (guild != null) {
            player.playerListName(ChatUtil.component("&8[&6" + guild.getTag() + "&8] &f" + player.getName() + this.plugin.getReputation().tabSuffix(player.getUniqueId())));
         } else {
            player.playerListName(ChatUtil.component("&8[&7Sans Guilde&8] &f" + player.getName() + this.plugin.getReputation().tabSuffix(player.getUniqueId())));
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

   private final java.util.Map<String, Boolean> hegemonyCache = new java.util.concurrent.ConcurrentHashMap<>();
   private final java.util.Map<String, Long> specChangedAt = new java.util.concurrent.ConcurrentHashMap<>();

   /** À appeler au démarrage : charge les délais de spécialisation et lance le recalcul périodique (asynchrone) de l'hégémonie. */
   public void initCaches() {
      for (Object[] row : this.plugin.getDataManager().getStorage().select("SELECT k, v FROM kv WHERE k LIKE 'speccd:%';", 2)) {
         try {
            this.specChangedAt.put(((String)row[0]).substring(7), Long.parseLong((String)row[1]));
         } catch (NumberFormatException ignored) {
         }
      }

      Bukkit.getScheduler().runTaskTimerAsynchronously(this.plugin, () -> {
         for (Guild g : this.getAllGuilds()) {
            this.hegemonyCache.put(g.getId(), this.computeEconomicHegemony(g));
         }

         this.hegemonyCache.keySet().removeIf(id -> this.getGuildById(id) == null);
         long nowMs = System.currentTimeMillis();
         this.pendingInvites.values().forEach(m -> m.values().removeIf(expiry -> expiry < nowMs));
         this.pendingInvites.values().removeIf(Map::isEmpty);
      }, 200L, 600L);
   }

   /**
    * Statut d'hégémonie : lu en mémoire (recalculé toutes les 30 s en tâche de fond). Le calcul (classement, comptes des membres)
    * lit la base de données, il n'est donc jamais fait pendant un tick du serveur, sauf pour une guilde jamais calculée.
    */
   public boolean isEconomicHegemony(Guild targetGuild) {
      if (targetGuild == null) {
         return false;
      }

      Boolean cached = this.hegemonyCache.get(targetGuild.getId());
      if (cached != null) {
         return cached;
      }

      boolean result = this.computeEconomicHegemony(targetGuild);
      this.hegemonyCache.put(targetGuild.getId(), result);
      return result;
   }

   private boolean computeEconomicHegemony(Guild targetGuild) {
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

         // Richesse de la guilde = sa banque + les pièces de tous ses membres : vider la banque dans les poches (ou l'inverse) ne change rien.
         double totalServerWealth = this.plugin.getDataManager().getTotalServerWealth();
         if (totalServerWealth > 0.0) {
            double guildWealth = targetGuild.getBankBalance();
            for (UUID memberUuid : members) {
               guildWealth += this.plugin.getDataManager().getPlayerCoins(memberUuid);
            }

            if (guildWealth / totalServerWealth >= 0.3) {
               return true;
            }
         }

         return false;
      }
   }
}
