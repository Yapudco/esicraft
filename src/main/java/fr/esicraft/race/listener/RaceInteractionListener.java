package fr.esicraft.race.listener;

import fr.esicraft.EsicraftPlugin;
import fr.esicraft.age.AgeType;
import fr.esicraft.data.PlayerData;
import fr.esicraft.guild.Guild;
import fr.esicraft.guild.GuildSpecialization;
import fr.esicraft.race.Race;
import fr.esicraft.util.ChatUtil;
import fr.esicraft.util.ChunkKey;
import java.util.ArrayList;
import java.util.Collection;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.ThreadLocalRandom;
import org.bukkit.GameMode;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.block.Block;
import org.bukkit.block.data.Ageable;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.AbstractVillager;
import org.bukkit.entity.Animals;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Monster;
import org.bukkit.entity.Player;
import org.bukkit.entity.ThrownPotion;
import org.bukkit.entity.Villager;
import org.bukkit.entity.WanderingTrader;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.block.BlockPistonExtendEvent;
import org.bukkit.event.block.BlockPistonRetractEvent;
import org.bukkit.event.enchantment.EnchantItemEvent;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.event.entity.ProjectileLaunchEvent;
import org.bukkit.event.inventory.FurnaceExtractEvent;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryOpenEvent;
import org.bukkit.event.inventory.InventoryType;
import org.bukkit.event.player.PlayerInteractEntityEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.AnvilInventory;
import org.bukkit.inventory.ItemStack;

public class RaceInteractionListener implements Listener {
   private final EsicraftPlugin plugin;
   private final fr.esicraft.util.PlacedBlockTracker placedTracker;
   private static final EnumSet<Material> BREEDING_FOODS = EnumSet.of(
      Material.WHEAT,
      Material.CARROT,
      Material.POTATO,
      Material.BEETROOT,
      Material.WHEAT_SEEDS,
      Material.PUMPKIN_SEEDS,
      Material.MELON_SEEDS,
      Material.BEETROOT_SEEDS,
      Material.GOLDEN_CARROT,
      Material.GOLDEN_APPLE,
      Material.BAMBOO
   );
   private static final EnumSet<Material> ORES = EnumSet.noneOf(Material.class);
   private static final EnumSet<Material> DRUIDIC_STONES = EnumSet.of(
      Material.ANDESITE,
      Material.POLISHED_ANDESITE,
      Material.DIORITE,
      Material.POLISHED_DIORITE,
      Material.GRANITE,
      Material.POLISHED_GRANITE,
      Material.BLACKSTONE,
      Material.POLISHED_BLACKSTONE,
      Material.CALCITE
   );

   public RaceInteractionListener(EsicraftPlugin plugin) {
      this.placedTracker = new fr.esicraft.util.PlacedBlockTracker(plugin);
      this.plugin = plugin;
   }

   @EventHandler(
      priority = EventPriority.HIGH,
      ignoreCancelled = true
   )
   public void onPlayerInteract(PlayerInteractEvent event) {
      Player player = event.getPlayer();
      ItemStack item = event.getItem();
      Block block = event.getClickedBlock();
      Race race = this.plugin.getRaceManager().getRace(player);
      AgeType age = this.plugin.getAgeManager().getPlayerAge(player);
      if (event.getAction() == Action.RIGHT_CLICK_BLOCK
         && block != null
         && block.getType() == Material.ENCHANTING_TABLE
         && !this.canPlayerEnchant(player, race, age)) {
         event.setCancelled(true);
         ChatUtil.sendActionbar(
            player, "&c❌ L'utilisation de la Table d'Enchantement est réservée aux Arcanistes (ou Rôdeur Solitaire Âge 3+) jusqu'à l'Âge 4 !"
         );
         ChatUtil.playSound(player, Sound.ENTITY_VILLAGER_NO, 1.0F, 1.0F);
      } else if (event.getAction() == Action.RIGHT_CLICK_BLOCK
         && block != null
         && block.getType() == Material.BREWING_STAND
         && race != Race.ARCANIST
         && age.getLevel() < 3) {
         event.setCancelled(true);
         ChatUtil.sendActionbar(player, "&c❌ L'utilisation de l'Alambic est réservée aux Arcanistes jusqu'à l'Âge 3 (Alchimie) !");
         ChatUtil.playSound(player, Sound.ENTITY_VILLAGER_NO, 1.0F, 1.0F);
      } else if (item != null
         && (item.getType() == Material.SPLASH_POTION || item.getType() == Material.LINGERING_POTION)
         && (event.getAction() == Action.RIGHT_CLICK_AIR || event.getAction() == Action.RIGHT_CLICK_BLOCK)
         && race != Race.ARCANIST) {
         event.setCancelled(true);
         ChatUtil.sendActionbar(player, "&c❌ Seuls les Arcanistes peuvent lancer des potions jetables (Splash Potions) !");
         ChatUtil.playSound(player, Sound.ENTITY_VILLAGER_NO, 1.0F, 1.0F);
      } else if (item != null
         && (item.getType() == Material.BOW || item.getType() == Material.CROSSBOW)
         && (event.getAction() == Action.RIGHT_CLICK_AIR || event.getAction() == Action.RIGHT_CLICK_BLOCK)
         && race != Race.RANGER
         && age.getLevel() <= 3) {
         event.setCancelled(true);
         ChatUtil.sendActionbar(player, "&c❌ L'utilisation de l'arc est réservée aux Rôdeurs jusqu'à l'Âge 4 (Guerre Totale) !");
         ChatUtil.playSound(player, Sound.ENTITY_VILLAGER_NO, 1.0F, 1.0F);
      } else if (event.getAction() == Action.RIGHT_CLICK_BLOCK
         && item != null
         && item.getType() == Material.BONE_MEAL
         && race != Race.SYLVAN
         && age.getLevel() < 3) {
         event.setCancelled(true);
         ChatUtil.sendActionbar(player, "&c❌ Seuls les Sylvains (ou Âge de guilde 3+) peuvent utiliser la Bone Meal !");
      } else {
         if (event.getAction() == Action.RIGHT_CLICK_BLOCK
            && block != null
            && race == Race.SYLVAN
            && block.getBlockData() instanceof Ageable ageable
            && ageable.getAge() >= ageable.getMaximumAge()) {
            Guild claimOwner = this.plugin.getGuildManager().getGuildByChunk(ChunkKey.from(block.getChunk()));
            if (claimOwner != null && !claimOwner.isMember(player.getUniqueId()) && !player.hasPermission("esicraft.admin")) {
               event.setCancelled(true);
               ChatUtil.sendActionbar(player, "&c❌ Territoire protégé de la guilde &6" + claimOwner.getName());
               return;
            }

            Collection<ItemStack> drops = block.getDrops(player.getInventory().getItemInMainHand());
            Guild guild = this.plugin.getGuildManager().getGuildByPlayer(player.getUniqueId());
            boolean isDruidic = guild != null && guild.getSpecialization() == GuildSpecialization.DRUIDIC_ENCLAVE;

            for (ItemStack drop : drops) {
               block.getWorld().dropItemNaturally(block.getLocation(), drop);
               if (isDruidic) {
                  block.getWorld().dropItemNaturally(block.getLocation(), drop.clone());
               }
            }

            if (isDruidic) {
               ChatUtil.sendActionbar(player, "&a\ud83c\udf43 Récolte d'Enclave : Butin agricole doublé !");
               block.getWorld().spawnParticle(Particle.HAPPY_VILLAGER, block.getLocation().add(0.5, 0.5, 0.5), 6, 0.2, 0.2, 0.2, 0.05);
            }

            ageable.setAge(0);
            block.setBlockData(ageable);
            this.plugin.getContractManager().addProgress(player, fr.esicraft.fun.ContractType.HARVEST, 1);
            ChatUtil.playSound(player, Sound.BLOCK_CROP_BREAK, 1.0F, 1.2F);
            event.setCancelled(true);
         }
      }
   }

   @EventHandler(
      priority = EventPriority.HIGH,
      ignoreCancelled = true
   )
   public void onEnchantItem(EnchantItemEvent event) {
      Player player = event.getEnchanter();
      Race race = this.plugin.getRaceManager().getRace(player);
      AgeType age = this.plugin.getAgeManager().getPlayerAge(player);
      if (!this.canPlayerEnchant(player, race, age)) {
         event.setCancelled(true);
         ChatUtil.sendActionbar(player, "&c❌ L'enchantement est réservé aux Arcanistes (ou Rôdeur Solitaire Âge 3+) jusqu'à l'Âge 4 !");
         ChatUtil.playSound(player, Sound.ENTITY_VILLAGER_NO, 1.0F, 1.0F);
      } else {
         Guild guild = this.plugin.getGuildManager().getGuildByPlayer(player.getUniqueId());
         if (guild != null && guild.getSpecialization() == GuildSpecialization.ARCANE_CONCLAVE) {
            int cost = event.getExpLevelCost();
            event.setExpLevelCost(Math.max(1, (int)Math.round((double)cost * 0.5)));
            Map<Enchantment, Integer> toAdd = event.getEnchantsToAdd();

            for (Entry<Enchantment, Integer> entry : new HashMap<>(toAdd).entrySet()) {
               int lvl = entry.getValue();
               if (lvl == 4) {
                  toAdd.put(entry.getKey(), 5);
               } else if (lvl == 5) {
                  toAdd.put(entry.getKey(), 6);
               }
            }

            ChatUtil.sendActionbar(player, "&d\ud83d\udd2e Conclave Arcanique : -50% Coût XP & Enchantements renforcés (IV→V, V→VI) !");
         }
      }
   }

   @EventHandler(
      priority = EventPriority.HIGH,
      ignoreCancelled = true
   )
   public void onInventoryOpen(InventoryOpenEvent event) {
      if (event.getPlayer() instanceof Player player) {
         AgeType age = this.plugin.getAgeManager().getPlayerAge(player);
         Race race = this.plugin.getRaceManager().getRace(player);
         if (event.getInventory().getType() == InventoryType.MERCHANT) {
            Guild guild = this.plugin.getGuildManager().getGuildByPlayer(player.getUniqueId());
            boolean isMerchantSyndicate = guild != null && guild.getSpecialization() == GuildSpecialization.MERCHANT_SYNDICATE;
            boolean canTrade = isMerchantSyndicate || age.getLevel() >= 4;
            if (!canTrade) {
               event.setCancelled(true);
               ChatUtil.sendMessage(
                  player,
                  "&c❌ Le commerce avec les villageois est réservé aux membres du &eSyndicat Marchand &cou aux guildes ayant atteint l'&6Âge 4 (Guerre Totale)&c !"
               );
               ChatUtil.playSound(player, Sound.ENTITY_VILLAGER_NO, 1.0F, 1.0F);
            }
         } else if (event.getInventory().getType() == InventoryType.ENCHANTING && !this.canPlayerEnchant(player, race, age)) {
            event.setCancelled(true);
            ChatUtil.sendMessage(
               player, "&c❌ L'utilisation de la Table d'Enchantement est réservée aux Arcanistes (ou Rôdeur Solitaire Âge 3+) jusqu'à l'Âge 4 !"
            );
            ChatUtil.playSound(player, Sound.ENTITY_VILLAGER_NO, 1.0F, 1.0F);
         }
      }
   }

   @EventHandler(
      priority = EventPriority.HIGH,
      ignoreCancelled = true
   )
   public void onAnvilClick(InventoryClickEvent event) {
      if (event.getInventory().getType() == InventoryType.ENCHANTING && event.getWhoClicked() instanceof Player player) {
         Race race = this.plugin.getRaceManager().getRace(player);
         AgeType age = this.plugin.getAgeManager().getPlayerAge(player);
         if (!this.canPlayerEnchant(player, race, age)) {
            event.setCancelled(true);
            player.closeInventory();
            ChatUtil.sendActionbar(
               player, "&c❌ L'utilisation de la Table d'Enchantement est réservée aux Arcanistes (ou Rôdeur Solitaire Âge 3+) jusqu'à l'Âge 4 !"
            );
            ChatUtil.playSound(player, Sound.ENTITY_VILLAGER_NO, 1.0F, 1.0F);
            return;
         }
      }

      if (event.getInventory() instanceof AnvilInventory anvilInv && event.getRawSlot() == 2 && event.getWhoClicked() instanceof Player playerx) {
         Race race = this.plugin.getRaceManager().getRace(playerx);
         AgeType age = this.plugin.getAgeManager().getPlayerAge(playerx);
         if (!this.canPlayerEnchant(playerx, race, age)) {
            ItemStack firstItem = anvilInv.getItem(0);
            ItemStack secondItem = anvilInv.getItem(1);
            ItemStack result = event.getCurrentItem();
            boolean involvesEnchantedBook = firstItem != null && firstItem.getType() == Material.ENCHANTED_BOOK
               || secondItem != null && secondItem.getType() == Material.ENCHANTED_BOOK
               || result != null && result.getType() == Material.ENCHANTED_BOOK;
            if (involvesEnchantedBook) {
               event.setCancelled(true);
               ChatUtil.sendActionbar(playerx, "&c❌ La forge de livres enchantés est réservée aux Arcanistes (ou Rôdeur Solitaire Âge 3+) jusqu'à l'Âge 4 !");
               ChatUtil.sendMessage(
                  playerx,
                  "&c❌ Seuls les Arcanistes (ou Rôdeur Solitaire Âge 3+) peuvent combiner et forger des livres enchantés sur l'enclume avant l'Âge 4 !"
               );
               ChatUtil.playSound(playerx, Sound.ENTITY_VILLAGER_NO, 1.0F, 1.0F);
            }
         }
      }
   }

   private boolean canPlayerEnchant(Player player, Race race, AgeType age) {
      if (race == Race.ARCANIST) {
         return true;
      } else {
         return age != null && age.getLevel() >= 4 ? true : age != null && age.getLevel() >= 3 && this.isLoneRanger(player);
      }
   }

   private boolean isLoneRanger(Player player) {
      if (player == null) {
         return false;
      } else {
         Race race = this.plugin.getRaceManager().getRace(player);
         if (race != Race.RANGER) {
            return false;
         } else {
            Guild guild = this.plugin.getGuildManager().getGuildByPlayer(player.getUniqueId());
            return guild != null && guild.getSpecialization() == GuildSpecialization.LONE_RANGER;
         }
      }
   }

   @EventHandler(
      priority = EventPriority.HIGH,
      ignoreCancelled = true
   )
   public void onProjectileLaunch(ProjectileLaunchEvent event) {
      if (event.getEntity() instanceof ThrownPotion && event.getEntity().getShooter() instanceof Player player) {
         Race race = this.plugin.getRaceManager().getRace(player);
         if (race != Race.ARCANIST) {
            event.setCancelled(true);
            ChatUtil.sendActionbar(player, "&c❌ Seuls les Arcanistes peuvent lancer des potions jetables (Splash Potions) !");
            ChatUtil.playSound(player, Sound.ENTITY_VILLAGER_NO, 1.0F, 1.0F);
         }
      }
   }

   @EventHandler(
      priority = EventPriority.HIGH,
      ignoreCancelled = true
   )
   public void onPlayerInteractEntity(PlayerInteractEntityEvent event) {
      Player player = event.getPlayer();
      AgeType age = this.plugin.getAgeManager().getPlayerAge(player);
      if (event.getRightClicked() instanceof AbstractVillager
         || event.getRightClicked() instanceof Villager
         || event.getRightClicked() instanceof WanderingTrader) {
         if (event.getRightClicked().getScoreboardTags().contains(fr.esicraft.fun.EventManager.TRADER_TAG)) {
            return; // Marchand du Marché Noir : ouvert à tous
         }

         Guild guild = this.plugin.getGuildManager().getGuildByPlayer(player.getUniqueId());
         boolean isMerchantSyndicate = guild != null && guild.getSpecialization() == GuildSpecialization.MERCHANT_SYNDICATE;
         boolean canTrade = isMerchantSyndicate || age.getLevel() >= 4;
         if (!canTrade) {
            event.setCancelled(true);
            ChatUtil.sendMessage(
               player,
               "&c❌ Le commerce avec les villageois est réservé aux membres du &eSyndicat Marchand &cou aux guildes ayant atteint l'&6Âge 4 (Guerre Totale)&c !"
            );
            ChatUtil.playSound(player, Sound.ENTITY_VILLAGER_NO, 1.0F, 1.0F);
            return;
         }
      }

      if (event.getRightClicked() instanceof Animals) {
         ItemStack inHand = player.getInventory().getItemInMainHand();
         Race race = this.plugin.getRaceManager().getRace(player);
         if (this.isBreedingFood(inHand.getType()) && race != Race.SYLVAN && age.getLevel() < 3) {
            event.setCancelled(true);
            ChatUtil.sendActionbar(player, "&c❌ Seuls les Sylvains (ou Âge de guilde 3+) peuvent reproduire les animaux !");
         }
      }
   }

   @EventHandler(
      priority = EventPriority.MONITOR,
      ignoreCancelled = true
   )
   public void onBlockPlace(BlockPlaceEvent event) {
      Block block = event.getBlock();
      if (this.isOre(block.getType()) || this.isDruidicBonusBlock(block)) {
         this.placedTracker.mark(block);
      }
   }

   @EventHandler(
      priority = EventPriority.LOW,
      ignoreCancelled = true
   )
   public void onBlockBreakRaceCheck(BlockBreakEvent event) {
      Player player = event.getPlayer();
      Race race = this.plugin.getRaceManager().getRace(player);
      if (player.getGameMode() != GameMode.CREATIVE && (race == null || race == Race.NONE)) {
         event.setCancelled(true);
         ChatUtil.sendActionbar(player, "&c❌ Vous devez d'abord choisir un métier avec &e/race &cpour pouvoir miner et casser des blocs !");
         ChatUtil.playSound(player, Sound.ENTITY_VILLAGER_NO, 0.8F, 1.0F);
      }
   }

   // Les bonus de drop sont calculés APRÈS les protections (claims en HIGH), sinon un bloc protégé
   // (cancel tardif) laisserait quand même tomber les drops bonus.
   // Alambic : l'alimentation automatique par entonnoir ne doit pas contourner la restriction (Arcaniste / Âge 3)
   @EventHandler(
      priority = EventPriority.HIGH,
      ignoreCancelled = true
   )
   public void onHopperToBrewingStand(org.bukkit.event.inventory.InventoryMoveItemEvent event) {
      if (event.getDestination().getType() == InventoryType.BREWING && event.getDestination().getLocation() != null) {
         Block stand = event.getDestination().getLocation().getBlock();
         Guild owner = this.plugin.getGuildManager().getGuildByChunk(ChunkKey.from(stand.getChunk()));
         boolean allowed = false;
         if (owner != null) {
            if (owner.getCurrentAge().getLevel() >= 3) {
               allowed = true;
            } else {
               for (java.util.UUID memberUuid : owner.getMembers().keySet()) {
                  if (this.plugin.getRaceManager().getRace(memberUuid) == Race.ARCANIST) {
                     allowed = true;
                     break;
                  }
               }
            }
         }

         if (!allowed) {
            event.setCancelled(true);
         }
      }
   }

   @EventHandler(
      priority = EventPriority.MONITOR,
      ignoreCancelled = true
   )
   public void onPistonExtend(BlockPistonExtendEvent event) {
      this.shiftPlacedMarks(event.getBlocks(), event.getDirection());
   }

   @EventHandler(
      priority = EventPriority.MONITOR,
      ignoreCancelled = true
   )
   public void onPistonRetract(BlockPistonRetractEvent event) {
      this.shiftPlacedMarks(event.getBlocks(), event.getDirection());
   }

   /** Un piston déplace le bloc : la marque « posé par un joueur » doit le suivre. */
   private void shiftPlacedMarks(List<Block> moved, org.bukkit.block.BlockFace direction) {
      List<Block> destinations = new java.util.ArrayList<>();

      for (Block b : moved) {
         if (this.placedTracker.unmark(b)) {
            destinations.add(b.getRelative(direction));
         }
      }

      for (Block dest : destinations) {
         this.placedTracker.mark(dest);
      }
   }

   @EventHandler(
      priority = EventPriority.HIGHEST,
      ignoreCancelled = true
   )
   public void onBlockBreak(BlockBreakEvent event) {
      Player player = event.getPlayer();
      Race race = this.plugin.getRaceManager().getRace(player);
      if (player.getGameMode() == GameMode.CREATIVE || race != null && race != Race.NONE) {
         Block block = event.getBlock();
         boolean isPlayerPlaced = this.placedTracker.isPlaced(block);
         if (isPlayerPlaced) {
            this.placedTracker.unmark(block);
         }

         Material blockMat = block.getType();
         boolean isAnOre = this.isOre(blockMat);
         boolean isADruidicBlock = this.isDruidicBonusBlock(block);
         if (!isPlayerPlaced && (isAnOre || isADruidicBlock)) {
            ItemStack tool = player.getInventory().getItemInMainHand();
            Collection<ItemStack> baseDrops = block.getDrops(tool, player);
            if (!baseDrops.isEmpty()) {
               Guild guild = this.plugin.getGuildManager().getGuildByPlayer(player.getUniqueId());
               GuildSpecialization spec = guild != null ? guild.getSpecialization() : GuildSpecialization.NONE;
               boolean isIndustry = spec == GuildSpecialization.UNDERGROUND_INDUSTRY;
               boolean isDwarf = race == Race.DWARF;
               boolean isWarlord = spec == GuildSpecialization.WARLORD;
               boolean isDruidic = spec == GuildSpecialization.DRUIDIC_ENCLAVE;
               if (isWarlord && ThreadLocalRandom.current().nextDouble() < 0.25) {
                  event.setDropItems(false);
                  ChatUtil.sendActionbar(player, "&c⚔️ Malus Légion Guerrière : Récoltes et minerais réduits de 25% !");
               } else if (isDruidic && isADruidicBlock) {
                  for (ItemStack drop : baseDrops) {
                     block.getWorld().dropItemNaturally(block.getLocation(), drop.clone());
                  }

                  String category = this.isCrop(block) ? "Agricole" : (this.isWood(blockMat) ? "Bois" : "Pierre Naturelle");
                  ChatUtil.sendActionbar(player, "&a\ud83c\udf43 Récolte d'Enclave : Butin " + category + " doublé !");
                  block.getWorld().spawnParticle(Particle.HAPPY_VILLAGER, block.getLocation().add(0.5, 0.5, 0.5), 8, 0.2, 0.2, 0.2, 0.05);
                  ChatUtil.playSound(player, Sound.ENTITY_EXPERIENCE_ORB_PICKUP, 0.6F, 1.4F);
               } else if (isIndustry || isDwarf) {
                  for (ItemStack drop : baseDrops) {
                     int extraAmount = 0;
                     if (isIndustry) {
                        int base = drop.getAmount();
                        int half = base / 2;
                        if (base % 2 != 0 && ThreadLocalRandom.current().nextDouble() < 0.5) {
                           half++;
                        }

                        if (half == 0 && ThreadLocalRandom.current().nextDouble() < 0.5) {
                           half = 1;
                        }

                        extraAmount += half;
                     }

                     if (isDwarf && ThreadLocalRandom.current().nextDouble() < 0.15) {
                        extraAmount += drop.getAmount();
                     }

                     if (extraAmount > 0) {
                        ItemStack bonusDrop = drop.clone();
                        bonusDrop.setAmount(extraAmount);
                        block.getWorld().dropItemNaturally(block.getLocation(), bonusDrop);
                        String msg = isIndustry && isDwarf
                           ? "&6⛏️ Bonus Industrie (x1.5) & Nain !"
                           : (
                              isIndustry
                                 ? "&6⛏️ Bonus Industrie Souterraine : &a+" + extraAmount + " " + drop.getType().name() + " (x1.5) !"
                                 : "&6⛏️ Bonus Nain : Minerais doublés !"
                           );
                        ChatUtil.sendActionbar(player, msg);
                        block.getWorld().spawnParticle(Particle.CRIT, block.getLocation().add(0.5, 0.5, 0.5), 8, 0.2, 0.2, 0.2, 0.05);
                        ChatUtil.playSound(player, Sound.ENTITY_EXPERIENCE_ORB_PICKUP, 0.6F, 1.6F);
                     }
                  }
               }
            }
         }
      }
   }

   @EventHandler(
      priority = EventPriority.HIGH,
      ignoreCancelled = true
   )
   public void onFurnaceExtract(FurnaceExtractEvent event) {
      Player player = event.getPlayer();
      Race race = this.plugin.getRaceManager().getRace(player);
      if (race == Race.BLACKSMITH && event.getItemAmount() > 0) {
         int amount = event.getItemAmount();
         int bonusCount = 0;

         for (int i = 0; i < amount; i++) {
            if (ThreadLocalRandom.current().nextDouble() < 0.35) {
               bonusCount++;
            }
         }

         if (bonusCount > 0) {
            Material itemType = event.getItemType();
            ItemStack bonus = new ItemStack(itemType, bonusCount);
            player.getInventory().addItem(new ItemStack[]{bonus});
            ChatUtil.sendActionbar(player, "&c\ud83d\udd28 Maîtrise de la Forge : +" + bonusCount + " " + itemType.name() + " gratuit(s) !");
            ChatUtil.playSound(player, Sound.BLOCK_ANVIL_USE, 0.5F, 1.5F);
         }
      }
   }

   /** Seuls les animaux d'élevage voient leur butin doublé : jamais les bêtes de bât (âne, mulet, lama, cheval) ni celles qui portent un objet (renard, loup). */
   private static final java.util.Set<org.bukkit.entity.EntityType> DOUBLED_LOOT = java.util.EnumSet.of(org.bukkit.entity.EntityType.COW, org.bukkit.entity.EntityType.MOOSHROOM,
      org.bukkit.entity.EntityType.PIG, org.bukkit.entity.EntityType.SHEEP, org.bukkit.entity.EntityType.CHICKEN, org.bukkit.entity.EntityType.RABBIT);

   @EventHandler(
      priority = EventPriority.MONITOR,
      ignoreCancelled = true
   )
   public void onEntityDeath(EntityDeathEvent event) {
      LivingEntity entity = event.getEntity();
      Player killer = entity.getKiller();
      if (killer != null) {
         Guild guild = this.plugin.getGuildManager().getGuildByPlayer(killer.getUniqueId());
         GuildSpecialization spec = guild != null ? guild.getSpecialization() : GuildSpecialization.NONE;
         if (entity instanceof Animals && spec == GuildSpecialization.DRUIDIC_ENCLAVE && DOUBLED_LOOT.contains(entity.getType())) {
            List<ItemStack> extraDrops = new ArrayList<>();

            for (ItemStack drop : event.getDrops()) {
               extraDrops.add(drop.clone());
            }

            event.getDrops().addAll(extraDrops);
            ChatUtil.sendActionbar(killer, "&a\ud83c\udf43 Élevage d'Enclave : Butin animal doublé !");
            killer.getWorld().spawnParticle(Particle.HAPPY_VILLAGER, entity.getLocation().add(0.0, 0.5, 0.0), 8, 0.3, 0.3, 0.3, 0.05);
         } else {
            org.bukkit.event.entity.CreatureSpawnEvent.SpawnReason reason = entity.getEntitySpawnReason();
            boolean naturalSpawn = switch (reason) {
               case NATURAL, CHUNK_GEN, JOCKEY, REINFORCEMENTS, PATROL, VILLAGE_INVASION, TRAP, LIGHTNING, SILVERFISH_BLOCK -> true;
               default -> false;
            };
            if (!naturalSpawn || entity.getScoreboardTags().contains("esicraft_raid_mob")) {
               return;
            }

            this.plugin.getAntiFarmManager().registerKill(killer.getUniqueId(), entity.getType());
            double baseBounty = this.getMobBounty(entity);
            if (baseBounty > 0.0) {
               double antiFarmMultiplier = this.plugin.getAntiFarmManager().getMultiplier(killer.getUniqueId(), entity.getType());
               double finalBounty = baseBounty * antiFarmMultiplier;
               finalBounty *= this.plugin.getEventManager().getBountyMultiplier();
               finalBounty *= this.plugin.getNatureSeasons().bountyMultiplier();
               finalBounty *= this.plugin.getSynergy().bountyMultiplier(guild);
               finalBounty *= this.plugin.getBalance().catchUpMultiplier(guild);
               finalBounty = this.plugin.getAntiFarmManager().capHourlyEarning(killer.getUniqueId(), finalBounty);
               if (spec == GuildSpecialization.MERCHANT_SYNDICATE) {
                  finalBounty *= 1.2;
               }

               PlayerData data = this.plugin.getDataManager().getPlayerData(killer.getUniqueId());
               if (data != null) {
                  boolean shareWithGuild = guild != null && spec == GuildSpecialization.MERCHANT_SYNDICATE;
                  // 10 % de la prime sont TRANSFÉRÉS à la banque de guilde (et non créés) : le joueur touche 90 %.
                  double guildShare = shareWithGuild ? finalBounty * 0.1 : 0.0;
                  data.addCoins(finalBounty - guildShare);
                  String antiFarmNote = antiFarmMultiplier < 0.7 ? " &8[&c-Farm x" + String.format("%.2f", antiFarmMultiplier) + "&8]" : "";
                  if (shareWithGuild) {
                     guild.depositBank(guildShare);
                     ChatUtil.sendActionbar(
                        killer,
                        "&a+ "
                           + String.format("%.2f", finalBounty)
                           + " pièces &7("
                           + entity.getType().name()
                           + ") &e[+ "
                           + String.format("%.2f", guildShare)
                           + " guilde]"
                           + antiFarmNote
                     );
                  } else {
                     ChatUtil.sendActionbar(killer, "&a+ " + String.format("%.2f", finalBounty) + " pièces &7(" + entity.getType().name() + ")" + antiFarmNote);
                  }
               }
            }
         }
      }
   }

   private double getMobBounty(LivingEntity entity) {
      if (entity == null) {
         return 0.0;
      } else {
         EntityType type = entity.getType();

         return switch (type) {
            case ENDER_DRAGON -> 750.0;
            case WARDEN -> 500.0;
            case WITHER -> 350.0;
            case ELDER_GUARDIAN -> 150.0;
            case GIANT -> 100.0;
            case RAVAGER -> 75.0;
            case EVOKER -> 40.0;
            case PIGLIN_BRUTE -> 25.0;
            case GHAST -> 20.0;
            case SHULKER -> 20.0;
            case WITHER_SKELETON -> 15.0;
            case BLAZE -> 12.0;
            case HOGLIN, ZOGLIN -> 12.0;
            case GUARDIAN -> 15.0;
            case VINDICATOR -> 15.0;
            case WITCH -> 12.0;
            case ENDERMAN -> 10.0;
            case PILLAGER -> 8.0;
            case CREEPER -> 5.0;
            case PHANTOM -> 5.0;
            case PIGLIN, ZOMBIFIED_PIGLIN -> 4.0;
            case CAVE_SPIDER -> 3.5;
            case SPIDER -> 3.0;
            case SKELETON, STRAY -> 3.0;
            case ZOMBIE, DROWNED, HUSK, ZOMBIE_VILLAGER -> 2.5;
            case SLIME, MAGMA_CUBE -> 2.0;
            case VEX -> 2.5;
            case SILVERFISH, ENDERMITE -> 1.5;
            default -> entity instanceof Monster ? 2.5 : 0.0;
         };
      }
   }

   private boolean isBreedingFood(Material mat) {
      return BREEDING_FOODS.contains(mat);
   }

   private boolean isOre(Material mat) {
      return ORES.contains(mat);
   }

   private boolean isDruidicStone(Material mat) {
      return DRUIDIC_STONES.contains(mat);
   }

   private boolean isWood(Material mat) {
      String name = mat.name();
      return name.endsWith("_LOG") || name.endsWith("_WOOD") || name.endsWith("_STEM") || name.endsWith("_HYPHAE");
   }

   private boolean isCrop(Block block) {
      Material mat = block.getType();
      return block.getBlockData() instanceof Ageable ageable
         ? ageable.getAge() >= ageable.getMaximumAge()
         : mat == Material.PUMPKIN
            || mat == Material.MELON
            || mat == Material.SUGAR_CANE
            || mat == Material.CACTUS
            || mat == Material.BAMBOO
            || mat == Material.SWEET_BERRY_BUSH;
   }

   private boolean isDruidicBonusBlock(Block block) {
      if (block == null) {
         return false;
      } else {
         Material mat = block.getType();
         return this.isCrop(block) || this.isWood(mat) || this.isDruidicStone(mat);
      }
   }

   static {
      for (Material m : Material.values()) {
         if (m.isBlock()) {
            String name = m.name();
            if (name.contains("_ORE")
               || m == Material.ANCIENT_DEBRIS
               || m == Material.RAW_IRON_BLOCK
               || m == Material.RAW_COPPER_BLOCK
               || m == Material.RAW_GOLD_BLOCK) {
               ORES.add(m);
            }
         }
      }
   }
}
