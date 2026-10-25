package fr.esicraft.race.listener;

import fr.esicraft.EsicraftPlugin;
import fr.esicraft.age.AgeType;
import fr.esicraft.guild.Guild;
import fr.esicraft.guild.GuildSpecialization;
import fr.esicraft.race.Race;
import fr.esicraft.util.ChatUtil;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.ThreadLocalRandom;
import org.bukkit.Bukkit;
import org.bukkit.Color;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.AreaEffectCloud;
import org.bukkit.entity.Arrow;
import org.bukkit.entity.DragonFireball;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.EvokerFangs;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Monster;
import org.bukkit.entity.Player;
import org.bukkit.entity.Projectile;
import org.bukkit.entity.ShulkerBullet;
import org.bukkit.entity.ThrownPotion;
import org.bukkit.entity.WitherSkull;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.EntityShootBowEvent;
import org.bukkit.event.entity.FoodLevelChangeEvent;
import org.bukkit.event.entity.PotionSplashEvent;
import org.bukkit.event.entity.EntityDamageEvent.DamageCause;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.PrepareAnvilEvent;
import org.bukkit.event.player.PlayerExpChangeEvent;
import org.bukkit.event.player.PlayerItemConsumeEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.inventory.AnvilInventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.EnchantmentStorageMeta;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.PotionMeta;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

public class RaceCombatListener implements Listener {
   private final EsicraftPlugin plugin;

   public RaceCombatListener(EsicraftPlugin plugin) {
      this.plugin = plugin;
   }

   @EventHandler(
      priority = EventPriority.HIGH,
      ignoreCancelled = true
   )
   public void onEntityDamageByEntity(EntityDamageByEntityEvent event) {
      if (event.getDamager() instanceof Player attacker) {
         Race race = this.plugin.getRaceManager().getRace(attacker);
         if (race == Race.SYLVAN) {
            event.setDamage(event.getDamage() * 0.8);
         }

         if (race == Race.ENGINEER) {
            event.setDamage(event.getDamage() * 0.8);
         }

         Guild guild = this.plugin.getGuildManager().getGuildByPlayer(attacker.getUniqueId());
         if (guild != null && guild.getSpecialization() == GuildSpecialization.WARLORD && event.getEntity() instanceof Monster) {
            event.setDamage(event.getDamage() * 1.3);
         }

         AgeType attackerAge = this.plugin.getAgeManager().getPlayerAge(attacker);
         if (attackerAge == AgeType.TOTAL_WAR) {
            event.setDamage(event.getDamage() * 1.1);
         } else if (attackerAge == AgeType.MYTHIC || attackerAge == AgeType.SUPREMACY) {
            event.setDamage(event.getDamage() * 1.15);
         }
      }

      if (event.getDamager() instanceof Projectile projectile && projectile.getShooter() instanceof Player shooter) {
         Race racex = this.plugin.getRaceManager().getRace(shooter);
         AgeType shooterAge = this.plugin.getAgeManager().getPlayerAge(shooter);
         if (racex == Race.RANGER) {
            event.setDamage(event.getDamage() * 1.25);
            if (event.getEntity() instanceof LivingEntity livingVictim) {
               livingVictim.addPotionEffect(new PotionEffect(PotionEffectType.POISON, 100, 0, false, true, true));
            }
         }

         if (shooterAge == AgeType.TOTAL_WAR) {
            event.setDamage(event.getDamage() * 1.1);
         } else if (shooterAge == AgeType.MYTHIC || shooterAge == AgeType.SUPREMACY) {
            event.setDamage(event.getDamage() * 1.15);
         }
      }

      if (event.getEntity() instanceof Player victim) {
         Guild guildx = this.plugin.getGuildManager().getGuildByPlayer(victim.getUniqueId());
         if (guildx != null && guildx.getSpecialization() == GuildSpecialization.UNDERGROUND_INDUSTRY) {
            Entity damager = event.getDamager();
            if (damager instanceof Monster
               && (
                  damager.getType() == EntityType.BLAZE
                     || damager.getType() == EntityType.GHAST
                     || damager.getType() == EntityType.WITCH
                     || damager.getType() == EntityType.WITHER_SKELETON
                     || damager.getType() == EntityType.SHULKER
                     || damager.getType() == EntityType.EVOKER
                     || damager.getType() == EntityType.VEX
               )) {
               event.setDamage(event.getDamage() * 1.2);
            }
         }

         if (guildx != null && guildx.getSpecialization() == GuildSpecialization.ARCANE_CONCLAVE) {
            Entity damager = event.getDamager();
            if (damager instanceof ThrownPotion
               || damager instanceof AreaEffectCloud
               || damager instanceof EvokerFangs
               || damager instanceof WitherSkull
               || damager instanceof DragonFireball
               || damager instanceof ShulkerBullet) {
               event.setDamage(event.getDamage() * 0.5);
            }
         }
      }
   }

   @EventHandler(
      priority = EventPriority.HIGH,
      ignoreCancelled = true
   )
   public void onShootBow(EntityShootBowEvent event) {
      if (event.getEntity() instanceof Player player) {
         Race var7 = this.plugin.getRaceManager().getRace(player);
         AgeType age = this.plugin.getAgeManager().getPlayerAge(player);
         if (var7 != Race.RANGER && age.getLevel() <= 3) {
            event.setCancelled(true);
            ChatUtil.sendActionbar(player, "&c❌ L'utilisation de l'arc est réservée aux Rôdeurs jusqu'à l'Âge 4 (Guerre Totale) !");
            ChatUtil.playSound(player, Sound.ENTITY_VILLAGER_NO, 1.0F, 1.0F);
         } else {
            if (var7 == Race.RANGER && event.getProjectile() instanceof Arrow arrow) {
               arrow.addCustomEffect(new PotionEffect(PotionEffectType.POISON, 100, 0, false, true, true), true);
               arrow.setColor(Color.GREEN);
            }
         }
      }
   }

   @EventHandler(
      priority = EventPriority.HIGH,
      ignoreCancelled = true
   )
   public void onEntityDamage(EntityDamageEvent event) {
      if (event.getEntity() instanceof Player player) {
         Race var6 = this.plugin.getRaceManager().getRace(player);
         AgeType age = this.plugin.getAgeManager().getPlayerAge(player);
         if (var6 == Race.ENGINEER) {
            if (event.getCause() == DamageCause.BLOCK_EXPLOSION || event.getCause() == DamageCause.ENTITY_EXPLOSION) {
               event.setDamage(event.getDamage() * 0.6);
               ChatUtil.sendActionbar(player, "&6\ud83d\udee1️ Blindage d'Ingénieur : -40% dégâts d'explosion !");
            }
         } else if ((age == AgeType.TOTAL_WAR || age == AgeType.MYTHIC || age == AgeType.SUPREMACY)
            && (event.getCause() == DamageCause.BLOCK_EXPLOSION || event.getCause() == DamageCause.ENTITY_EXPLOSION)) {
            event.setDamage(event.getDamage() * 0.8);
         }

         if (var6 == Race.ARCANIST) {
            DamageCause cause = event.getCause();
            if (cause == DamageCause.MAGIC || cause == DamageCause.POISON || cause == DamageCause.WITHER || cause == DamageCause.DRAGON_BREATH) {
               event.setDamage(event.getDamage() * 0.8);
            }
         }
      }
   }

   @EventHandler(
      priority = EventPriority.NORMAL,
      ignoreCancelled = true
   )
   public void onPlayerExpChange(PlayerExpChangeEvent event) {
      Player player = event.getPlayer();
      Race race = this.plugin.getRaceManager().getRace(player);
      AgeType age = this.plugin.getAgeManager().getPlayerAge(player);
      if (event.getAmount() > 0) {
         double multiplier = 1.0;
         if (race == Race.ARCANIST) {
            multiplier += 0.3;
         }

         if (age == AgeType.TOTAL_WAR) {
            multiplier += 0.25;
         }

         if (age == AgeType.MYTHIC || age == AgeType.SUPREMACY) {
            multiplier += 0.5;
         }

         if (multiplier > 1.0) {
            int bonus = (int)Math.round((double)event.getAmount() * multiplier);
            event.setAmount(bonus);
         }
      }
   }

   @EventHandler(
      priority = EventPriority.NORMAL,
      ignoreCancelled = true
   )
   public void onPotionConsume(PlayerItemConsumeEvent event) {
      Player player = event.getPlayer();
      Race race = this.plugin.getRaceManager().getRace(player);
      Guild guild = this.plugin.getGuildManager().getGuildByPlayer(player.getUniqueId());
      boolean isArcaneConclave = guild != null && guild.getSpecialization() == GuildSpecialization.ARCANE_CONCLAVE;
      boolean isArcanist = race == Race.ARCANIST;
      if (isArcanist || isArcaneConclave) {
         ItemStack item = event.getItem();
         if (item.getType() == Material.POTION && item.getItemMeta() instanceof PotionMeta meta) {
            double durationMultiplier = isArcanist ? 1.25 : 1.0;
            int ampBoost = isArcaneConclave ? 1 : 0;
            List<PotionEffect> effectsToApply = new ArrayList<>();
            if (meta.getBasePotionType() != null) {
               for (PotionEffect effect : meta.getBasePotionType().getPotionEffects()) {
                  int extendedDuration = (int)((double)effect.getDuration() * durationMultiplier);
                  int upgradedAmp = effect.getAmplifier() + ampBoost;
                  effectsToApply.add(
                     new PotionEffect(effect.getType(), extendedDuration, upgradedAmp, effect.isAmbient(), effect.hasParticles(), effect.hasIcon())
                  );
               }
            }

            if (meta.hasCustomEffects()) {
               for (PotionEffect effect : meta.getCustomEffects()) {
                  int extendedDuration = (int)((double)effect.getDuration() * durationMultiplier);
                  int upgradedAmp = effect.getAmplifier() + ampBoost;
                  effectsToApply.add(
                     new PotionEffect(effect.getType(), extendedDuration, upgradedAmp, effect.isAmbient(), effect.hasParticles(), effect.hasIcon())
                  );
               }
            }

            if (!effectsToApply.isEmpty()) {
               Bukkit.getScheduler().runTask(this.plugin, () -> {
                  for (PotionEffect pe : effectsToApply) {
                     player.addPotionEffect(pe);
                  }
               });
               if (isArcaneConclave && isArcanist) {
                  ChatUtil.sendActionbar(player, "&d\ud83d\udd2e Magie Arcanique : Potion Tier Supérieur (+1) & Durée +25% !");
               } else if (isArcaneConclave) {
                  ChatUtil.sendActionbar(player, "&d\ud83d\udd2e Conclave Arcanique : Potion améliorée au Tier Supérieur (+1) !");
               } else {
                  ChatUtil.sendActionbar(player, "&d✨ Alchimie Arcanique : Durée des potions +25% !");
               }
            }
         }
      }
   }

   @EventHandler(
      priority = EventPriority.HIGH,
      ignoreCancelled = true
   )
   public void onPotionSplash(PotionSplashEvent event) {
      if (event.getPotion().getShooter() instanceof Player shooter) {
         Guild guild = this.plugin.getGuildManager().getGuildByPlayer(shooter.getUniqueId());
         if (guild != null && guild.getSpecialization() == GuildSpecialization.ARCANE_CONCLAVE) {
            for (LivingEntity affected : event.getAffectedEntities()) {
               double intensity = event.getIntensity(affected);

               for (PotionEffect pe : event.getPotion().getEffects()) {
                  int dur = Math.max(20, (int)((double)pe.getDuration() * intensity));
                  int amp = pe.getAmplifier() + 1;
                  affected.addPotionEffect(new PotionEffect(pe.getType(), dur, amp, pe.isAmbient(), pe.hasParticles(), pe.hasIcon()));
               }
            }
         }
      }
   }

   @EventHandler(
      priority = EventPriority.NORMAL,
      ignoreCancelled = true
   )
   public void onFoodLevelChange(FoodLevelChangeEvent event) {
      if (event.getEntity() instanceof Player player) {
         Race race = this.plugin.getRaceManager().getRace(player);
         if (race == Race.BLACKSMITH && event.getFoodLevel() < player.getFoodLevel() && ThreadLocalRandom.current().nextDouble() < 0.25) {
            event.setFoodLevel(Math.max(0, event.getFoodLevel() - 1));
         }
      }
   }

   @EventHandler(
      priority = EventPriority.HIGH
   )
   public void onPrepareAnvil(PrepareAnvilEvent event) {
      if (event.getView().getPlayer() instanceof Player player) {
         Race var15 = this.plugin.getRaceManager().getRace(player);
         AnvilInventory inv = event.getInventory();
         Guild guild = this.plugin.getGuildManager().getGuildByPlayer(player.getUniqueId());
         boolean isArcaneConclave = guild != null && guild.getSpecialization() == GuildSpecialization.ARCANE_CONCLAVE;
         ItemStack secondItem = inv.getItem(1);
         if (secondItem != null && secondItem.getType() == Material.ENCHANTED_BOOK && var15 != Race.ARCANIST) {
            event.setResult(null);
         } else {
            if (var15 == Race.BLACKSMITH || isArcaneConclave) {
               int cost = inv.getRepairCost();
               if (cost > 1) {
                  inv.setRepairCost(Math.max(1, cost / 2));
               }
            }

            if (isArcaneConclave && event.getResult() != null) {
               ItemStack result = event.getResult().clone();
               if (result.hasItemMeta()) {
                  ItemMeta meta = result.getItemMeta();
                  boolean modified = false;
                  if (meta instanceof EnchantmentStorageMeta bookMeta) {
                     for (Entry<Enchantment, Integer> entry : new HashMap<>(bookMeta.getStoredEnchants()).entrySet()) {
                        int lvl = entry.getValue();
                        if (lvl == 4) {
                           bookMeta.addStoredEnchant(entry.getKey(), 5, true);
                           modified = true;
                        } else if (lvl == 5) {
                           bookMeta.addStoredEnchant(entry.getKey(), 6, true);
                           modified = true;
                        }
                     }
                  } else {
                     for (Entry<Enchantment, Integer> entryx : new HashMap<>(meta.getEnchants()).entrySet()) {
                        int lvl = entryx.getValue();
                        if (lvl == 4) {
                           meta.addEnchant(entryx.getKey(), 5, true);
                           modified = true;
                        } else if (lvl == 5) {
                           meta.addEnchant(entryx.getKey(), 6, true);
                           modified = true;
                        }
                     }
                  }

                  if (modified) {
                     result.setItemMeta(meta);
                     event.setResult(result);
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
   public void onAnvilClick(InventoryClickEvent event) {
      if (event.getInventory() instanceof AnvilInventory anvilInv && event.getRawSlot() == 2 && event.getWhoClicked() instanceof Player player) {
         Race race = this.plugin.getRaceManager().getRace(player);
         ItemStack secondItem = anvilInv.getItem(1);
         if (secondItem != null && secondItem.getType() == Material.ENCHANTED_BOOK && race != Race.ARCANIST) {
            event.setCancelled(true);
            ChatUtil.sendActionbar(player, "&c❌ Seuls les Arcanistes peuvent appliquer des livres enchantés !");
            ChatUtil.playSound(player, Sound.ENTITY_VILLAGER_NO, 1.0F, 1.0F);
         }
      }

      if (event.getWhoClicked() instanceof Player playerx) {
         Guild guild = this.plugin.getGuildManager().getGuildByPlayer(playerx.getUniqueId());
         boolean isArcane = guild != null && guild.getSpecialization() == GuildSpecialization.ARCANE_CONCLAVE;
         if (!isArcane) {
            sanitizeEnchantments(event.getCurrentItem(), false);
            sanitizeEnchantments(event.getCursor(), false);
         }
      }
   }

   @EventHandler
   public void onPlayerJoin(PlayerJoinEvent event) {
      Player player = event.getPlayer();
      Guild guild = this.plugin.getGuildManager().getGuildByPlayer(player.getUniqueId());
      boolean isArcane = guild != null && guild.getSpecialization() == GuildSpecialization.ARCANE_CONCLAVE;
      if (!isArcane) {
         sanitizePlayerEnchantments(player, false);
      }
   }

   public static boolean sanitizeEnchantments(ItemStack item, boolean allowArcaneBonus) {
      if (item != null && item.getType() != Material.AIR && item.hasItemMeta()) {
         ItemMeta meta = item.getItemMeta();
         boolean modified = false;
         if (meta instanceof EnchantmentStorageMeta bookMeta) {
            Map<Enchantment, Integer> stored = new HashMap<>(bookMeta.getStoredEnchants());

            for (Entry<Enchantment, Integer> entry : stored.entrySet()) {
               Enchantment ench = entry.getKey();
               int lvl = entry.getValue();
               if (!allowArcaneBonus && lvl > ench.getMaxLevel()) {
                  bookMeta.addStoredEnchant(ench, ench.getMaxLevel(), true);
                  modified = true;
               }
            }
         } else {
            Map<Enchantment, Integer> enchants = new HashMap<>(meta.getEnchants());

            for (Entry<Enchantment, Integer> entryx : enchants.entrySet()) {
               Enchantment ench = entryx.getKey();
               int lvl = entryx.getValue();
               if (!allowArcaneBonus && lvl > ench.getMaxLevel()) {
                  meta.addEnchant(ench, ench.getMaxLevel(), true);
                  modified = true;
               }
            }
         }

         if (modified) {
            item.setItemMeta(meta);
         }

         return modified;
      } else {
         return false;
      }
   }

   public static void sanitizePlayerEnchantments(Player player, boolean allowArcaneBonus) {
      if (player != null) {
         boolean changed = false;

         for (ItemStack item : player.getInventory().getContents()) {
            if (sanitizeEnchantments(item, allowArcaneBonus)) {
               changed = true;
            }
         }

         for (ItemStack itemx : player.getInventory().getArmorContents()) {
            if (sanitizeEnchantments(itemx, allowArcaneBonus)) {
               changed = true;
            }
         }

         for (ItemStack itemxx : player.getEnderChest().getContents()) {
            if (sanitizeEnchantments(itemxx, allowArcaneBonus)) {
               changed = true;
            }
         }

         if (changed) {
            player.updateInventory();
            ChatUtil.sendMessage(player, "&e⚠️ Vos enchantements ont été ramenés à leur niveau standard (spécialisation non-arcanique).");
         }
      }
   }
}
