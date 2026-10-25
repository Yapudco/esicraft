package fr.esicraft.gui;

import fr.esicraft.EsicraftPlugin;
import fr.esicraft.age.AgeType;
import fr.esicraft.data.PlayerData;
import fr.esicraft.gui.holder.AgeProgressHolder;
import fr.esicraft.gui.holder.AnnexationChoiceHolder;
import fr.esicraft.gui.holder.BaseGUIHolder;
import fr.esicraft.gui.holder.CustomHordeHolder;
import fr.esicraft.gui.holder.GuildMainHolder;
import fr.esicraft.gui.holder.GuildSpecHolder;
import fr.esicraft.gui.holder.MarketMainHolder;
import fr.esicraft.gui.holder.MercenaryHordeHolder;
import fr.esicraft.gui.holder.NexusTrapHolder;
import fr.esicraft.gui.holder.RaceSelectHolder;
import fr.esicraft.gui.holder.RaceShowHolder;
import fr.esicraft.gui.holder.RaidTargetHolder;
import fr.esicraft.guild.Guild;
import fr.esicraft.guild.GuildRole;
import fr.esicraft.guild.GuildSpecialization;
import fr.esicraft.nexus.Nexus;
import fr.esicraft.nexus.NexusTrapType;
import fr.esicraft.race.Race;
import fr.esicraft.raid.CustomHordeUnit;
import fr.esicraft.raid.RaidType;
import fr.esicraft.util.ChatUtil;
import java.util.Map;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.entity.IronGolem;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;
import org.bukkit.persistence.PersistentDataType;

public class GUIListener implements Listener {
   private final EsicraftPlugin plugin;

   public GUIListener(EsicraftPlugin plugin) {
      this.plugin = plugin;
   }

   @EventHandler(
      priority = EventPriority.HIGH
   )
   public void onInventoryDrag(InventoryDragEvent event) {
      if (event.getInventory().getHolder() instanceof BaseGUIHolder) {
         event.setCancelled(true);
      }
   }

   @EventHandler(
      priority = EventPriority.HIGH
   )
   public void onInventoryClick(InventoryClickEvent event) {
      if (event.getWhoClicked() instanceof Player player) {
         InventoryHolder holder = event.getInventory().getHolder();
         if (holder instanceof BaseGUIHolder) {
            event.setCancelled(true);
            int slot = event.getRawSlot();
            if (slot >= 0 && slot < event.getInventory().getSize()) {
               if (holder instanceof RaceSelectHolder) {
                  ItemStack clicked = event.getCurrentItem();
                  if (clicked != null && !clicked.getType().isAir()) {
                     Race targetRace = switch (slot) {
                        case 19 -> Race.DWARF;
                        default -> null;
                        case 21 -> Race.SYLVAN;
                        case 23 -> Race.BLACKSMITH;
                        case 25 -> Race.RANGER;
                        case 29 -> Race.ARCANIST;
                        case 33 -> Race.ENGINEER;
                     };
                     if (targetRace != null && this.plugin.getRaceManager().setRace(player, targetRace)) {
                        player.closeInventory();
                     }
                  }
               } else if (holder instanceof GuildMainHolder) {
                  if (slot == 20) {
                     GuildSpecGUI.open(player, this.plugin);
                  } else if (slot == 22) {
                     NexusTrapGUI.open(player, this.plugin);
                  } else if (slot == 31) {
                     this.plugin.getClaimVisualizerManager().toggleVisualizer(player);
                     player.closeInventory();
                  }
               } else if (holder instanceof GuildSpecHolder) {
                  if (slot == 36) {
                     GuildMainGUI.open(player, this.plugin);
                  } else {
                     GuildSpecialization targetSpec = switch (slot) {
                        case 19 -> GuildSpecialization.WARLORD;
                        case 20 -> GuildSpecialization.UNDERGROUND_INDUSTRY;
                        case 21 -> GuildSpecialization.DRUIDIC_ENCLAVE;
                        case 22 -> GuildSpecialization.MERCHANT_SYNDICATE;
                        case 23 -> GuildSpecialization.ARCANE_CONCLAVE;
                        case 24 -> GuildSpecialization.LONE_RANGER;
                        default -> null;
                     };
                     if (targetSpec != null) {
                        Guild guild = this.plugin.getGuildManager().getGuildByPlayer(player.getUniqueId());
                        if (guild != null) {
                           if (!guild.getLeaderUuid().equals(player.getUniqueId()) && !guild.hasRoleAtLeast(player.getUniqueId(), GuildRole.OFFICER)) {
                              ChatUtil.sendMessage(player, "&cSeuls les chefs et officiers peuvent modifier la spécialisation.");
                           } else {
                              this.plugin.getGuildManager().setSpecialization(guild, targetSpec, player);
                              player.closeInventory();
                           }
                        }
                     }
                  }
               } else if (holder instanceof AgeProgressHolder) {
                  ItemStack clicked = event.getCurrentItem();
                  if (clicked != null && !clicked.getType().isAir()) {
                     Guild guild = this.plugin.getGuildManager().getGuildByPlayer(player.getUniqueId());
                     if (guild != null) {
                        AgeType nextAge = guild.getCurrentAge().getNextAge();
                        if (nextAge != null) {
                           Material clickedMat = clicked.getType();
                           if (this.plugin.getAgeManager().getRequirementsForGuild(guild, nextAge).containsKey(clickedMat)) {
                              boolean deposited = false;

                              for (ItemStack invItem : player.getInventory().getContents()) {
                                 if (invItem != null && invItem.getType() == clickedMat && this.plugin.getAgeManager().depositItemForGuild(player, invItem)) {
                                    deposited = true;
                                    break;
                                 }
                              }

                              if (deposited) {
                                 AgeProgressGUI.open(player, this.plugin);
                              } else {
                                 ChatUtil.sendMessage(
                                    player, "&cVous n'avez pas de &e" + AgeProgressGUI.formatMaterialName(clickedMat) + " &cdans votre inventaire à déposer."
                                 );
                                 ChatUtil.playSound(player, Sound.ENTITY_VILLAGER_NO, 1.0F, 1.0F);
                              }
                           }
                        }
                     }
                  }
               } else if (holder instanceof MarketMainHolder marketHolder) {
                  if (slot == 49) {
                     MercenaryHordeGUI.open(player, this.plugin);
                  } else if (slot == 45 && event.getCurrentItem() != null && event.getCurrentItem().getType() == Material.ARROW) {
                     MarketMainGUI.open(player, this.plugin, marketHolder.getPage() - 1);
                  } else if (slot == 53 && event.getCurrentItem() != null && event.getCurrentItem().getType() == Material.ARROW) {
                     MarketMainGUI.open(player, this.plugin, marketHolder.getPage() + 1);
                  } else {
                     ItemStack clicked = event.getCurrentItem();
                     if (clicked != null && clicked.hasItemMeta()) {
                        String listingId = (String)clicked.getItemMeta().getPersistentDataContainer().get(MarketMainGUI.LISTING_KEY, PersistentDataType.STRING);
                        if (listingId != null) {
                           this.plugin.getMarketManager().buyOrCancelListing(player, listingId);
                           MarketMainGUI.open(player, this.plugin, marketHolder.getPage());
                        }
                     }
                  }
               } else if (holder instanceof MercenaryHordeHolder) {
                  if (slot == 45) {
                     MarketMainGUI.open(player, this.plugin);
                  } else if (slot == 40) {
                     CustomHordeGUI.open(player, this.plugin);
                  } else if (slot == 48) {
                     CustomHordeGUI.toggleBreachers(player.getUniqueId());
                     ChatUtil.playSound(player, Sound.UI_BUTTON_CLICK, 0.8F, 1.2F);
                     MercenaryHordeGUI.open(player, this.plugin);
                  } else if (slot == 50) {
                     CustomHordeGUI.toggleLavaResistance(player.getUniqueId());
                     ChatUtil.playSound(player, Sound.UI_BUTTON_CLICK, 0.8F, 1.4F);
                     MercenaryHordeGUI.open(player, this.plugin);
                  } else {
                     int idx = slot - 19;
                     RaidType[] types = RaidType.values();
                     if (idx >= 0 && idx < types.length) {
                        MercenaryHordeGUI.openTargetSelector(player, types[idx], this.plugin);
                     }
                  }
               } else if (holder instanceof CustomHordeHolder) {
                  if (slot == 45) {
                     MercenaryHordeGUI.open(player, this.plugin);
                  } else if (slot == 47) {
                     CustomHordeGUI.clearDraft(player.getUniqueId());
                     ChatUtil.playSound(player, Sound.BLOCK_LAVA_EXTINGUISH, 1.0F, 1.0F);
                     CustomHordeGUI.open(player, this.plugin);
                  } else if (slot == 49) {
                     CustomHordeGUI.toggleBreachers(player.getUniqueId());
                     ChatUtil.playSound(player, Sound.UI_BUTTON_CLICK, 0.8F, 1.2F);
                     CustomHordeGUI.open(player, this.plugin);
                  } else if (slot == 50) {
                     CustomHordeGUI.toggleLavaResistance(player.getUniqueId());
                     ChatUtil.playSound(player, Sound.UI_BUTTON_CLICK, 0.8F, 1.4F);
                     CustomHordeGUI.open(player, this.plugin);
                  } else {
                     Map<CustomHordeUnit, Integer> draft = CustomHordeGUI.getDraft(player.getUniqueId());
                     int totalUnits = CustomHordeGUI.getTotalMobCount(draft);
                     if (slot != 53) {
                        int unitIdx = slot - 19;
                        CustomHordeUnit[] units = CustomHordeUnit.values();
                        if (unitIdx >= 0 && unitIdx < units.length) {
                           CustomHordeUnit unit = units[unitIdx];
                           int current;
                           synchronized (draft) {
                              current = draft.getOrDefault(unit, 0);
                           }

                           int delta = event.isShiftClick() ? 5 : 1;
                           if (event.isLeftClick()) {
                              synchronized (draft) {
                                 draft.put(unit, current + delta);
                              }

                              ChatUtil.playSound(player, Sound.UI_BUTTON_CLICK, 0.8F, 1.4F);
                           } else if (event.isRightClick()) {
                              int nextVal = Math.max(0, current - delta);
                              synchronized (draft) {
                                 if (nextVal == 0) {
                                    draft.remove(unit);
                                 } else {
                                    draft.put(unit, nextVal);
                                 }
                              }

                              ChatUtil.playSound(player, Sound.UI_BUTTON_CLICK, 0.8F, 0.9F);
                           }

                           CustomHordeGUI.open(player, this.plugin);
                        }
                     } else if (totalUnits <= 0) {
                        ChatUtil.sendMessage(player, "&cVeuillez ajouter au moins une unité à votre composition de horde !");
                        ChatUtil.playSound(player, Sound.ENTITY_VILLAGER_NO, 1.0F, 1.0F);
                     } else {
                        MercenaryHordeGUI.openTargetSelector(player, null, this.plugin);
                     }
                  }
               } else if (holder instanceof RaidTargetHolder targetHolder) {
                  if (slot == 45) {
                     if (targetHolder.isCustom()) {
                        CustomHordeGUI.open(player, this.plugin);
                     } else {
                        MercenaryHordeGUI.open(player, this.plugin);
                     }
                  } else {
                     ItemStack clicked = event.getCurrentItem();
                     if (clicked != null && clicked.hasItemMeta()) {
                        String targetGuildId = (String)clicked.getItemMeta()
                           .getPersistentDataContainer()
                           .get(MercenaryHordeGUI.TARGET_GUILD_KEY, PersistentDataType.STRING);
                        if (targetGuildId != null) {
                           Guild targetGuild = this.plugin.getGuildManager().getGuildById(targetGuildId);
                           if (targetGuild != null) {
                              Guild attacker = this.plugin.getGuildManager().getGuildByPlayer(player.getUniqueId());
                              boolean breachers = CustomHordeGUI.isBreachersEnabled(player.getUniqueId());
                              boolean lavaResist = CustomHordeGUI.isLavaResistanceEnabled(player.getUniqueId());
                              if (targetHolder.isCustom()) {
                                 Map<CustomHordeUnit, Integer> comp = CustomHordeGUI.getDraft(player.getUniqueId());
                                 if (this.plugin.getRaidManager().launchCustomRaid(attacker, targetGuild, comp, breachers, lavaResist, player)) {
                                    CustomHordeGUI.clearDraft(player.getUniqueId());
                                    player.closeInventory();
                                 }
                              } else {
                                 RaidType type = targetHolder.getRaidType();
                                 if (type != null && this.plugin.getRaidManager().launchRaid(attacker, targetGuild, type, breachers, lavaResist, player)) {
                                    player.closeInventory();
                                 }
                              }
                           }
                        }
                     }
                  }
               } else if (!(holder instanceof AnnexationChoiceHolder)) {
                  if (holder instanceof NexusTrapHolder) {
                     Guild guild = this.plugin.getGuildManager().getGuildByPlayer(player.getUniqueId());
                     if (guild == null || !guild.hasNexus()) {
                        return;
                     }

                     if (!guild.hasRoleAtLeast(player.getUniqueId(), GuildRole.OFFICER)) {
                        ChatUtil.sendMessage(player, "&cSeuls les officiers et chefs peuvent acheter des pièges de Nexus.");
                        ChatUtil.playSound(player, Sound.ENTITY_VILLAGER_NO, 1.0F, 1.0F);
                        return;
                     }

                     Nexus nexus = guild.getNexus();

                     NexusTrapType targetTrap = switch (slot) {
                        case 11 -> NexusTrapType.INFERNAL_FLAMES;
                        case 12 -> NexusTrapType.SLOWDOWN_WEB;
                        case 13 -> NexusTrapType.LIGHTNING_STRIKE;
                        case 14 -> NexusTrapType.POISON_MIST;
                        case 15 -> NexusTrapType.GUARDIAN_GOLEM;
                        default -> null;
                     };
                     if (targetTrap != null) {
                        if (nexus.hasTrap(targetTrap)) {
                           ChatUtil.sendMessage(player, "&aCe piège est déjà armé pour la prochaine horde !");
                           ChatUtil.playSound(player, Sound.ENTITY_VILLAGER_YES, 1.0F, 1.0F);
                           return;
                        }

                        double cost = targetTrap.getBaseCost();
                        if (guild.getSpecialization() == GuildSpecialization.UNDERGROUND_INDUSTRY) {
                           cost *= 0.5;
                        }

                        PlayerData pData = this.plugin.getDataManager().getPlayerData(player.getUniqueId());
                        boolean paid = false;
                        if (guild.withdrawBank(cost)) {
                           paid = true;
                           ChatUtil.sendMessage(player, "&aCoût de &6" + String.format("%.2f", cost) + " pièces &adébité de la banque de guilde.");
                        } else {
                           if (pData == null || !(pData.getCoins() >= cost)) {
                              ChatUtil.sendMessage(
                                 player,
                                 "&cSolde insuffisant dans la banque de guilde et sur votre compte (&6" + String.format("%.2f", cost) + " pièces requises&c)."
                              );
                              ChatUtil.playSound(player, Sound.ENTITY_VILLAGER_NO, 1.0F, 1.0F);
                              return;
                           }

                           pData.removeCoins(cost);
                           this.plugin.getDataManager().savePlayer(pData);
                           paid = true;
                           ChatUtil.sendMessage(player, "&aCoût de &6" + String.format("%.2f", cost) + " pièces &adébité de votre solde personnel.");
                        }

                        if (paid) {
                           nexus.addTrap(targetTrap);
                           if (targetTrap == NexusTrapType.GUARDIAN_GOLEM && nexus.getLocation() != null && nexus.getLocation().getWorld() != null) {
                              this.plugin.getNexusManager().removeGuardians(guild, nexus.getLocation());
                              this.plugin.getNexusManager().spawnGuardian(guild, nexus.getLocation());
                           }

                           this.plugin.getDataManager().saveGuild(guild);
                           ChatUtil.playSound(player, Sound.ENTITY_PLAYER_LEVELUP, 1.0F, 1.0F);
                           ChatUtil.sendActionbar(player, "&a✨ Piège " + targetTrap.getDisplayName() + " &aarmé pour la prochaine horde !");
                           NexusTrapGUI.open(player, this.plugin);
                        }
                     }
                  }

                  if (holder instanceof RaceShowHolder showHolder) {
                     if (slot == 45) {
                        if (showHolder.getPage() > 1) {
                           RaceShowGUI.open(player, null, showHolder.getPage() - 1, this.plugin);
                        }
                     } else if (slot == 49) {
                        player.closeInventory();
                     } else if (slot == 53) {
                        RaceShowGUI.open(player, null, showHolder.getPage() + 1, this.plugin);
                     }
                  }
               } else if (slot == 11) {
                  this.plugin.getGuildManager().acceptAnnexation(player);
                  player.closeInventory();
               } else if (slot == 15) {
                  this.plugin.getGuildManager().declineAnnexation(player);
                  player.closeInventory();
               }
            }
         }
      }
   }
}
