package fr.esicraft.guild.listener;

import fr.esicraft.EsicraftPlugin;
import fr.esicraft.guild.Guild;
import fr.esicraft.guild.GuildSpecialization;
import fr.esicraft.util.ChatUtil;
import com.destroystokyo.paper.event.player.PlayerArmorChangeEvent;
import java.util.Map;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.entity.Ageable;
import org.bukkit.entity.Animals;
import org.bukkit.entity.Player;
import org.bukkit.entity.ThrownPotion;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityBreedEvent;
import org.bukkit.event.entity.ProjectileLaunchEvent;
import org.bukkit.event.player.PlayerItemConsumeEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.PotionMeta;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionType;

/**
 * Contraintes et bonus de spécialisation qui n'appartiennent à aucun autre listener :
 * <ul>
 *   <li>Enclave Champêtre : reproduction animale accélérée (x3) et armures en Netherite interdites ;</li>
 *   <li>Industrie Souterraine : potions de niveau 2 interdites.</li>
 * </ul>
 */
public class SpecializationRulesListener implements Listener {
   private static final EquipmentSlot[] ARMOR_SLOTS = {EquipmentSlot.HEAD, EquipmentSlot.CHEST, EquipmentSlot.LEGS, EquipmentSlot.FEET};
   /** Cooldown vanilla de reproduction : 6000 ticks ; l'Enclave le divise par 3. */
   private static final int ENCLAVE_BREED_COOLDOWN_TICKS = 2000;
   /** Croissance vanilla d'un bébé : 24000 ticks ; l'Enclave la divise par 3. */
   private static final int ENCLAVE_BABY_AGE_TICKS = -8000;

   private final EsicraftPlugin plugin;

   public SpecializationRulesListener(EsicraftPlugin plugin) {
      this.plugin = plugin;
   }

   private GuildSpecialization specOf(Player player) {
      Guild guild = this.plugin.getGuildManager().getGuildByPlayer(player.getUniqueId());
      return guild != null ? guild.getSpecialization() : GuildSpecialization.NONE;
   }

   // ---------------------------------------------------------------- Enclave : reproduction x3

   @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
   public void onBreed(EntityBreedEvent event) {
      if (!(event.getBreeder() instanceof Player breeder) || this.specOf(breeder) != GuildSpecialization.DRUIDIC_ENCLAVE) {
         return;
      }

      if (event.getEntity() instanceof Ageable baby) {
         baby.setAge(ENCLAVE_BABY_AGE_TICKS);
      }

      Bukkit.getScheduler().runTask(this.plugin, () -> {
         for (Object parent : new Object[]{event.getMother(), event.getFather()}) {
            if (parent instanceof Animals animal && animal.isValid()) {
               animal.setAge(-ENCLAVE_BREED_COOLDOWN_TICKS);
            }
         }
      });
   }

   // ---------------------------------------------------------------- Enclave : pas de Netherite

   private static boolean isNetheriteArmor(ItemStack item) {
      return item != null && item.getType().name().startsWith("NETHERITE_") && isArmorName(item.getType().name());
   }

   private static boolean isArmorName(String name) {
      return name.endsWith("_HELMET") || name.endsWith("_CHESTPLATE") || name.endsWith("_LEGGINGS") || name.endsWith("_BOOTS");
   }

   /** Retire toute pièce de Netherite portée par un membre d'Enclave Champêtre (rendue à l'inventaire, sinon lâchée au sol). */
   public void enforceNoNetherite(Player player) {
      if (this.specOf(player) != GuildSpecialization.DRUIDIC_ENCLAVE) {
         return;
      }

      boolean removed = false;
      for (EquipmentSlot slot : ARMOR_SLOTS) {
         ItemStack worn = player.getInventory().getItem(slot);
         if (isNetheriteArmor(worn)) {
            player.getInventory().setItem(slot, null);
            Map<Integer, ItemStack> leftovers = player.getInventory().addItem(worn);
            for (ItemStack left : leftovers.values()) {
               player.getWorld().dropItemNaturally(player.getLocation(), left);
            }

            removed = true;
         }
      }

      if (removed) {
         ChatUtil.sendMessage(player, "&c❌ Les armures en Netherite sont interdites dans une &aEnclave Champêtre &c!");
         ChatUtil.playSound(player, Sound.ITEM_ARMOR_EQUIP_NETHERITE, 0.6F, 0.6F);
      }
   }

   @EventHandler(priority = EventPriority.MONITOR)
   public void onArmorChange(PlayerArmorChangeEvent event) {
      if (isNetheriteArmor(event.getNewItem())) {
         Player player = event.getPlayer();
         Bukkit.getScheduler().runTask(this.plugin, () -> this.enforceNoNetherite(player));
      }
   }

   @EventHandler(priority = EventPriority.MONITOR)
   public void onJoin(PlayerJoinEvent event) {
      Player player = event.getPlayer();
      Bukkit.getScheduler().runTaskLater(this.plugin, () -> this.enforceNoNetherite(player), 20L);
   }

   // ---------------------------------------------------------------- Industrie : pas de potions de niveau 2

   private static boolean isLevelTwo(PotionMeta meta) {
      if (meta == null) {
         return false;
      }

      PotionType base = meta.getBasePotionType();
      if (base != null && base.name().startsWith("STRONG_")) {
         return true;
      }

      if (meta.hasCustomEffects()) {
         for (PotionEffect effect : meta.getCustomEffects()) {
            if (effect.getAmplifier() >= 1) {
               return true;
            }
         }
      }

      return false;
   }

   @EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true)
   public void onDrink(PlayerItemConsumeEvent event) {
      ItemStack item = event.getItem();
      if (item.getType() == Material.POTION
         && item.getItemMeta() instanceof PotionMeta meta
         && isLevelTwo(meta)
         && this.specOf(event.getPlayer()) == GuildSpecialization.UNDERGROUND_INDUSTRY) {
         event.setCancelled(true);
         this.deny(event.getPlayer());
      }
   }

   @EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true)
   public void onThrow(ProjectileLaunchEvent event) {
      if (event.getEntity() instanceof ThrownPotion potion
         && potion.getShooter() instanceof Player shooter
         && potion.getItem().getItemMeta() instanceof PotionMeta meta
         && isLevelTwo(meta)
         && this.specOf(shooter) == GuildSpecialization.UNDERGROUND_INDUSTRY) {
         event.setCancelled(true);
         this.deny(shooter);
      }
   }

   private void deny(Player player) {
      ChatUtil.sendActionbar(player, "&c❌ Les potions de niveau 2 sont interdites dans l'&6Industrie Souterraine &c!");
      ChatUtil.playSound(player, Sound.ENTITY_VILLAGER_NO, 1.0F, 1.0F);
   }
}
