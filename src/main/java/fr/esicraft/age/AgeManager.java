package fr.esicraft.age;

import fr.esicraft.EsicraftPlugin;
import fr.esicraft.guild.Guild;
import fr.esicraft.util.ChatUtil;
import java.time.Duration;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.Map.Entry;
import net.kyori.adventure.title.Title;
import net.kyori.adventure.title.Title.Times;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

public class AgeManager {
   private final EsicraftPlugin plugin;
   private final Map<AgeType, Map<Material, Integer>> ageRequirements = new EnumMap<>(AgeType.class);

   public AgeManager(EsicraftPlugin plugin) {
      this.plugin = plugin;
      this.loadConfig();
   }

   public void loadConfig() {
      this.ageRequirements.clear();
      ConfigurationSection agesSec = this.plugin.getConfig().getConfigurationSection("ages");
      if (agesSec != null) {
         for (AgeType age : AgeType.values()) {
            String key = "age-" + age.getLevel() + ".requirements";
            ConfigurationSection reqSec = agesSec.getConfigurationSection(key);
            Map<Material, Integer> reqs = new HashMap<>();
            if (reqSec != null) {
               for (String matName : reqSec.getKeys(false)) {
                  Material mat = Material.matchMaterial(matName);
                  if (mat != null) {
                     reqs.put(mat, reqSec.getInt(matName));
                  }
               }
            }

            this.ageRequirements.put(age, reqs);
         }
      }
   }

   public AgeType getPlayerAge(Player player) {
      return player == null ? AgeType.RUSTIC : this.getPlayerAge(player.getUniqueId());
   }

   public AgeType getPlayerAge(UUID playerUuid) {
      Guild guild = this.plugin.getGuildManager().getGuildByPlayer(playerUuid);
      return guild != null ? guild.getCurrentAge() : AgeType.RUSTIC;
   }

   public Map<Material, Integer> getRequirementsForAge(AgeType nextAge) {
      return nextAge == null ? Map.of() : this.ageRequirements.getOrDefault(nextAge, Map.of());
   }

   public Map<Material, Integer> getRequirementsForGuild(Guild guild, AgeType nextAge) {
      if (nextAge == null) {
         return Map.of();
      } else {
         Map<Material, Integer> baseReqs = this.ageRequirements.getOrDefault(nextAge, Map.of());
         if (guild == null) {
            return baseReqs;
         } else if (nextAge == AgeType.SUPREMACY) {
            return baseReqs;
         } else {
            int memberCount = guild.getEffectiveMemberCount();
            if (memberCount <= 1) {
               return baseReqs;
            } else {
               double multiplier = 1.0;
               if (memberCount <= 5) {
                  multiplier += (double)(memberCount - 1) * 0.3;
               } else {
                  multiplier += 1.2 + (double)(memberCount - 5) * 0.5;
               }

               Map<Material, Integer> scaledReqs = new HashMap<>();

               for (Entry<Material, Integer> entry : baseReqs.entrySet()) {
                  Material mat = entry.getKey();
                  int baseAmount = entry.getValue();
                  if (nextAge == AgeType.SUPREMACY && baseAmount == 1) {
                     scaledReqs.put(mat, 1);
                  } else {
                     int scaledAmount;
                     if (baseAmount >= 100) {
                        double raw = (double)baseAmount * multiplier;
                        scaledAmount = (int)(Math.ceil(raw / 100.0) * 100.0);
                     } else {
                        scaledAmount = (int)Math.ceil((double)baseAmount * multiplier);
                     }

                     scaledReqs.put(mat, Math.max(baseAmount, scaledAmount));
                  }
               }

               return scaledReqs;
            }
         }
      }
   }

   public double getGuildMultiplier(Guild guild) {
      if (guild == null) {
         return 1.0;
      } else {
         int memberCount = guild.getEffectiveMemberCount();
         if (memberCount <= 1) {
            return 1.0;
         } else {
            return memberCount <= 5 ? 1.0 + (double)(memberCount - 1) * 0.3 : 2.2 + (double)(memberCount - 5) * 0.5;
         }
      }
   }

   public boolean depositItemForGuild(Player player, ItemStack item) {
      if (player != null && item != null && !item.getType().isAir()) {
         Guild guild = this.plugin.getGuildManager().getGuildByPlayer(player.getUniqueId());
         if (guild == null) {
            ChatUtil.sendMessage(player, "&cVous devez appartenir à une guilde pour faire progresser les Âges !");
            return false;
         } else {
            AgeType currentAge = guild.getCurrentAge();
            AgeType nextAge = currentAge.getNextAge();
            if (nextAge == null) {
               ChatUtil.sendMessage(player, "&cVotre guilde a déjà atteint l'Âge maximal (&e" + currentAge.getDisplayName() + "&c) !");
               return false;
            } else {
               Map<Material, Integer> reqs = this.getRequirementsForGuild(guild, nextAge);
               Material mat = item.getType();
               if (!reqs.containsKey(mat)) {
                  ChatUtil.sendMessage(player, "&cCet item n'est pas requis pour le passage au prochain Âge (&e" + nextAge.getDisplayName() + "&c).");
                  return false;
               } else {
                  if (mat == Material.NETHERITE_HOE && nextAge == AgeType.SUPREMACY) {
                     int mendingLvl = 0;
                     int unbreakingLvl = 0;
                     int fortuneLvl = 0;

                     for (Entry<Enchantment, Integer> entry : item.getEnchantments().entrySet()) {
                        String key = entry.getKey().getKey().getKey().toLowerCase();
                        if (key.contains("mending")) {
                           mendingLvl = entry.getValue();
                        }

                        if (key.contains("unbreaking") || key.contains("durability")) {
                           unbreakingLvl = entry.getValue();
                        }

                        if (key.contains("fortune") || key.contains("loot_bonus_blocks")) {
                           fortuneLvl = entry.getValue();
                        }
                     }

                     boolean hasMending = mendingLvl >= 1;
                     boolean hasUnbreaking3 = unbreakingLvl >= 3;
                     boolean hasFortune3 = fortuneLvl >= 3;
                     if (!hasMending || !hasUnbreaking3 || !hasFortune3) {
                        ChatUtil.sendMessage(player, "&c❌ Cette houe en Netherite doit obligatoirement posséder les 3 enchantements :");
                        ChatUtil.sendMessage(player, " &e• Raccommodage (Mending)");
                        ChatUtil.sendMessage(player, " &e• Solidité III (Unbreaking III)");
                        ChatUtil.sendMessage(player, " &e• Fortune III (Fortune III)");
                        ChatUtil.playSound(player, Sound.ENTITY_VILLAGER_NO, 1.0F, 1.0F);
                        return false;
                     }
                  }

                  int required = reqs.get(mat);
                  int current = guild.getDonatedAmount(mat);
                  if (current >= required) {
                     ChatUtil.sendMessage(player, "&eLe quota pour &6" + mat.name() + " &ea déjà été atteint dans votre guilde !");
                     return false;
                  } else {
                     int needed = required - current;
                     int toDeposit = Math.min(item.getAmount(), needed);
                     item.setAmount(item.getAmount() - toDeposit);
                     guild.addDonation(mat, toDeposit);
                     this.plugin.getDataManager().saveGuild(guild);
                     ChatUtil.sendMessage(player, "&aMerci pour votre don de &e" + toDeposit + "x " + mat.name() + " &apour la progression de votre guilde !");
                     ChatUtil.playSound(player, Sound.ENTITY_PLAYER_LEVELUP, 1.0F, 1.5F);
                     this.checkGuildProgression(guild);
                     return true;
                  }
               }
            }
         }
      } else {
         return false;
      }
   }

   public void checkGuildProgression(Guild guild) {
      if (guild != null) {
         AgeType nextAge = guild.getCurrentAge().getNextAge();
         if (nextAge != null) {
            Map<Material, Integer> reqs = this.getRequirementsForGuild(guild, nextAge);
            boolean complete = true;

            for (Entry<Material, Integer> entry : reqs.entrySet()) {
               int donated = guild.getDonatedAmount(entry.getKey());
               if (donated < entry.getValue()) {
                  complete = false;
                  break;
               }
            }

            if (complete) {
               this.unlockNextAgeForGuild(guild);
            }
         }
      }
   }

   public void unlockNextAgeForGuild(Guild guild) {
      if (guild != null) {
         AgeType next = guild.getCurrentAge().getNextAge();
         if (next != null) {
            guild.setCurrentAge(next);
            guild.clearDonations();
            this.plugin.getDataManager().saveGuild(guild);
            ChatUtil.broadcast("&6&l⚔️ PROGRESSION DE GUILDE : &e" + guild.getName() + " &aest entrée dans l'ère &6" + next.getDisplayName() + " &a!");
            if (next == AgeType.SUPREMACY) {
               ChatUtil.broadcast("&e&l\ud83d\udc51 EXPLOIT LÉGENDAIRE : &6La guilde &e" + guild.getName() + " &6a conquis l'&eÂge de Suprématie &6!");
            }

            for (UUID memberUuid : guild.getMembers().keySet()) {
               Player member = Bukkit.getPlayer(memberUuid);
               if (member != null && member.isOnline()) {
                  member.showTitle(
                     Title.title(
                        ChatUtil.component(next.getDisplayName()),
                        ChatUtil.component("&aNouvel Âge débloqué pour votre guilde !"),
                        Times.times(Duration.ofMillis(500L), Duration.ofMillis(4000L), Duration.ofMillis(1000L))
                     )
                  );
                  ChatUtil.playSound(member, Sound.UI_TOAST_CHALLENGE_COMPLETE, 1.0F, 1.0F);
                  ChatUtil.sendRaw(member, "&8&m--------------------------------------------------");
                  ChatUtil.sendRaw(member, " &6&l✨ VOTRE GUILDE A ATTEINT UN NOUVEL ÂGE ! ✨");
                  ChatUtil.sendRaw(member, "       " + next.getDisplayName());
                  ChatUtil.sendRaw(member, " &7" + next.getDescription());
                  ChatUtil.sendRaw(member, " &aVos membres débloquent de nouvelles technologies et crafts !");
                  if (next == AgeType.SUPREMACY) {
                     ItemStack dragonEgg = new ItemStack(Material.DRAGON_EGG, 1);
                     ItemStack netheriteBlock = new ItemStack(Material.NETHERITE_BLOCK, 1);
                     HashMap<Integer, ItemStack> leftover = member.getInventory().addItem(new ItemStack[]{dragonEgg, netheriteBlock});

                     for (ItemStack drop : leftover.values()) {
                        member.getWorld().dropItemNaturally(member.getLocation(), drop);
                     }

                     ChatUtil.sendRaw(
                        member, " &e&l🎁 RÉCOMPENSES SUPRÊMES : &a1x Œuf de Dragon &7+ &a1x Bloc de Netherite &7ajoutés à votre inventaire !"
                     );
                     ChatUtil.sendRaw(member, " &b&l💬 TITRE DE TCHAT ACTIF : &3[Giga Tchad] &7(Session < 1h) ou &5[Gros Nerd] &7(Session > 1h) !");
                  }

                  ChatUtil.sendRaw(member, "&8&m--------------------------------------------------");
               }
            }
         }
      }
   }
}
