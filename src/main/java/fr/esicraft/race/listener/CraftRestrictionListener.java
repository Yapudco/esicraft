package fr.esicraft.race.listener;

import fr.esicraft.EsicraftPlugin;
import fr.esicraft.age.AgeType;
import fr.esicraft.guild.Guild;
import fr.esicraft.guild.GuildSpecialization;
import fr.esicraft.race.Race;
import fr.esicraft.util.ChatUtil;
import java.util.EnumSet;
import org.bukkit.Material;
import org.bukkit.entity.HumanEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.CraftItemEvent;
import org.bukkit.event.inventory.PrepareItemCraftEvent;
import org.bukkit.event.inventory.PrepareSmithingEvent;
import org.bukkit.event.inventory.SmithItemEvent;
import org.bukkit.inventory.ItemStack;

public class CraftRestrictionListener implements Listener {
   private final EsicraftPlugin plugin;
   private static final EnumSet<Material> BASIC_MATERIALS = EnumSet.noneOf(Material.class);
   private static final EnumSet<Material> IRON_TOOLS = EnumSet.of(
      Material.IRON_PICKAXE, Material.IRON_AXE, Material.IRON_SHOVEL, Material.IRON_HOE, Material.BUCKET, Material.SHEARS
   );
   private static final EnumSet<Material> IRON_COMBAT_GEAR = EnumSet.of(
      Material.IRON_HELMET, Material.IRON_CHESTPLATE, Material.IRON_LEGGINGS, Material.IRON_BOOTS, Material.IRON_SWORD, Material.SHIELD
   );
   private static final EnumSet<Material> DIAMOND_GEAR = EnumSet.noneOf(Material.class);
   private static final EnumSet<Material> NETHERITE_GEAR = EnumSet.noneOf(Material.class);
   private static final EnumSet<Material> ARMOR_MATERIALS = EnumSet.noneOf(Material.class);
   private static final EnumSet<Material> PICKAXE_MATERIALS = EnumSet.noneOf(Material.class);
   private static final EnumSet<Material> FORGE_STATIONS = EnumSet.of(Material.ANVIL, Material.GRINDSTONE, Material.BLAST_FURNACE);
   private static final EnumSet<Material> ADVANCED_REDSTONE = EnumSet.of(
      Material.PISTON,
      Material.STICKY_PISTON,
      Material.HOPPER,
      Material.OBSERVER,
      Material.REPEATER,
      Material.COMPARATOR,
      Material.DISPENSER,
      Material.DROPPER,
      Material.TNT
   );

   public CraftRestrictionListener(EsicraftPlugin plugin) {
      this.plugin = plugin;
   }

   @EventHandler(
      priority = EventPriority.HIGHEST
   )
   public void onPrepareCraft(PrepareItemCraftEvent event) {
      ItemStack result = event.getInventory().getResult();
      if (result != null && !result.getType().isAir()) {
         if ((event.getViewers().isEmpty() ? null : (HumanEntity)event.getViewers().get(0)) instanceof Player player) {
            Material mat = result.getType();
            Race race = this.plugin.getRaceManager().getRace(player);
            AgeType age = this.plugin.getAgeManager().getPlayerAge(player);
            if (!this.canCraft(mat, player, race, age)) {
               event.getInventory().setResult(new ItemStack(Material.AIR));
            }
         }
      }
   }

   @EventHandler(
      priority = EventPriority.HIGHEST
   )
   public void onCraft(CraftItemEvent event) {
      if (event.getWhoClicked() instanceof Player player) {
         ItemStack result = event.getRecipe().getResult();
         Material mat = result.getType();
         Race race = this.plugin.getRaceManager().getRace(player);
         AgeType age = this.plugin.getAgeManager().getPlayerAge(player);
         if (!this.canCraft(mat, player, race, age)) {
            event.setCancelled(true);
            String requiredInfo = this.getRequiredInfo(mat);
            ChatUtil.sendActionbar(player, "&c❌ Craft verrouillé ! &7" + requiredInfo);
            ChatUtil.sendMessage(player, "&cVous n'avez pas la classe ou l'Âge requis pour fabriquer cet objet ! (&7" + requiredInfo + "&c)");
         }
      }
   }

   @EventHandler(
      priority = EventPriority.HIGHEST
   )
   public void onPrepareSmithing(PrepareSmithingEvent event) {
      ItemStack result = event.getResult();
      if (result != null && !result.getType().isAir()) {
         if ((event.getViewers().isEmpty() ? null : (HumanEntity)event.getViewers().get(0)) instanceof Player player) {
            Material mat = result.getType();
            Race race = this.plugin.getRaceManager().getRace(player);
            AgeType age = this.plugin.getAgeManager().getPlayerAge(player);
            if (!this.canCraft(mat, player, race, age)) {
               event.setResult(new ItemStack(Material.AIR));
            }
         }
      }
   }

   @EventHandler(
      priority = EventPriority.HIGHEST
   )
   public void onSmithItem(SmithItemEvent event) {
      if (event.getWhoClicked() instanceof Player player) {
         ItemStack result = event.getCurrentItem();
         if (result != null && !result.getType().isAir()) {
            Material mat = result.getType();
            Race race = this.plugin.getRaceManager().getRace(player);
            AgeType age = this.plugin.getAgeManager().getPlayerAge(player);
            if (!this.canCraft(mat, player, race, age)) {
               event.setCancelled(true);
               String requiredInfo = this.getRequiredInfo(mat);
               ChatUtil.sendActionbar(player, "&c❌ Forge verrouillée ! &7" + requiredInfo);
               ChatUtil.sendMessage(player, "&cVous n'avez pas la classe ou l'Âge requis pour forger cet objet ! (&7" + requiredInfo + "&c)");
            }
         }
      }
   }

   /** Bloc Crafter (1.21) : pas de joueur, donc l'Âge de la guilde qui possède le territoire (Âge 1 hors claim) décide, sans bonus de métier. */
   @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
   public void onCrafter(org.bukkit.event.block.CrafterCraftEvent event) {
      ItemStack result = event.getResult();
      if (result == null || result.getType().isAir()) {
         return;
      }

      org.bukkit.block.Block b = event.getBlock();
      Guild owner = this.plugin.getGuildManager().getGuildByChunk(new fr.esicraft.util.ChunkKey(b.getWorld().getName(), b.getX() >> 4, b.getZ() >> 4));
      AgeType age = owner != null ? owner.getCurrentAge() : AgeType.RUSTIC;
      if (!this.canCraft(result.getType(), null, Race.NONE, age)) {
         event.setCancelled(true);
      }
   }

   private boolean usageLocked(Player player, ItemStack item) {
      if (item == null || item.getType().isAir() || player.hasPermission("esicraft.admin") || !this.plugin.getConfig().getBoolean("restrictions.block-use", true)) {
         return false;
      }

      Material m = item.getType();
      boolean gear = this.isDiamondGear(m) || this.isNetheriteGear(m) || this.isIronCombatGear(m) || this.isIronTool(m) || m == Material.BOW || m == Material.CROSSBOW;
      return gear && !this.canCraft(m, player, this.plugin.getRaceManager().getRace(player), this.plugin.getAgeManager().getPlayerAge(player));
   }

   /** Acheter l'équipement interdit (marché, villageois, coffres) ne suffit plus : on ne frappe ni ne mine avec tant que l'Âge ou le métier manque. */
   @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
   public void onUseWeapon(org.bukkit.event.entity.EntityDamageByEntityEvent event) {
      if (event.getDamager() instanceof Player player && this.usageLocked(player, player.getInventory().getItemInMainHand())) {
         event.setCancelled(true);
         ChatUtil.sendActionbar(player, "&c❌ Cet équipement est verrouillé : &7" + this.getRequiredInfo(player.getInventory().getItemInMainHand().getType()));
      }
   }

   @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
   public void onUseTool(org.bukkit.event.block.BlockBreakEvent event) {
      Player player = event.getPlayer();
      if (this.usageLocked(player, player.getInventory().getItemInMainHand())) {
         event.setCancelled(true);
         ChatUtil.sendActionbar(player, "&c❌ Cet outil est verrouillé : &7" + this.getRequiredInfo(player.getInventory().getItemInMainHand().getType()));
      }
   }

   public boolean canCraft(Material mat, Player player, Race race, AgeType age) {
      int ageLevel = age.getLevel();
      if (this.isBasicMaterial(mat)) {
         return true;
      } else if (this.isIronTool(mat)) {
         if (ageLevel >= 2) {
            return true;
         } else if (race == Race.DWARF && mat == Material.IRON_PICKAXE) {
            return true;
         } else {
            return race == Race.SYLVAN && mat == Material.IRON_HOE ? true : race == Race.BLACKSMITH;
         }
      } else if (this.isIronCombatGear(mat)) {
         if (mat == Material.SHIELD && ageLevel >= 2) {
            return true;
         } else {
            return ageLevel >= 3 ? true : race == Race.BLACKSMITH;
         }
      } else if (this.isDiamondGear(mat)) {
         if (this.isArmor(mat)) {
            return ageLevel >= 4 ? true : ageLevel >= 2 && race == Race.BLACKSMITH;
         } else if (ageLevel >= 3) {
            return true;
         } else {
            if (ageLevel >= 2) {
               if (race == Race.DWARF && (this.isPickaxe(mat) || mat == Material.DIAMOND_SHOVEL)) {
                  return true;
               }

               if (race == Race.BLACKSMITH && (mat == Material.DIAMOND_SWORD || mat == Material.DIAMOND_AXE)) {
                  return true;
               }

               if (race == Race.SYLVAN && mat == Material.DIAMOND_HOE) {
                  return true;
               }
            }

            return false;
         }
      } else if (this.isNetheriteGear(mat)) {
         if (ageLevel >= 5) {
            return true;
         } else {
            if (ageLevel >= 3) {
               if (race == Race.DWARF && (this.isPickaxe(mat) || mat == Material.NETHERITE_SHOVEL)) {
                  return true;
               }

               if (race == Race.BLACKSMITH && (this.isArmor(mat) || mat == Material.NETHERITE_SWORD || mat == Material.NETHERITE_AXE)) {
                  return true;
               }

               if (race == Race.SYLVAN && mat == Material.NETHERITE_HOE) {
                  return true;
               }

               if (this.isLoneRanger(player) && this.isArmor(mat)) {
                  return true;
               }
            }

            return false;
         }
      } else if (mat != Material.BOW && mat != Material.ARROW && mat != Material.CROSSBOW && mat != Material.SPECTRAL_ARROW) {
         if (mat == Material.ENCHANTING_TABLE) {
            if (ageLevel >= 4) {
               return true;
            } else {
               return ageLevel >= 3 && this.isLoneRanger(player) ? true : race == Race.ARCANIST;
            }
         } else if (mat == Material.BREWING_STAND) {
            return ageLevel >= 3 ? true : race == Race.ARCANIST;
         } else if (FORGE_STATIONS.contains(mat)) {
            return ageLevel >= 3 ? true : race == Race.BLACKSMITH;
         } else if (mat == Material.GOLDEN_APPLE) {
            return ageLevel >= 4 ? true : race == Race.ARCANIST || race == Race.SYLVAN;
         } else if (!this.isAdvancedRedstone(mat)) {
            return true;
         } else {
            return ageLevel >= 4 ? true : race == Race.ENGINEER;
         }
      } else {
         return ageLevel >= 4 ? true : race == Race.RANGER;
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

   private String getRequiredInfo(Material mat) {
      if (this.isIronTool(mat)) {
         return "Requis: Nain / Forgeron ou Âge 2 (Métallurgie)";
      } else if (this.isIronCombatGear(mat)) {
         return "Requis: Forgeron ou Âge 3 (Alchimie)";
      } else if (this.isDiamondGear(mat) && this.isArmor(mat)) {
         return "Requis: Forgeron (Âge 2+) ou Âge 4 (Guerre Totale)";
      } else if (this.isDiamondGear(mat)) {
         return "Requis: Métiers spécialisés (Âge 2) ou Âge 3 (Alchimie)";
      } else if (this.isNetheriteGear(mat)) {
         return "Requis: Spécialistes / Rôdeur Solitaire (Âge 3+) ou Âge 5";
      } else if (mat == Material.ENCHANTING_TABLE) {
         return "Requis: Arcaniste, Rôdeur Solitaire (Âge 3) ou Âge 4";
      } else if (mat == Material.BREWING_STAND) {
         return "Requis: Arcaniste ou Âge 3 (Alchimie)";
      } else if (FORGE_STATIONS.contains(mat)) {
         return "Requis: Forgeron ou Âge 3 (Alchimie)";
      } else {
         return this.isAdvancedRedstone(mat) ? "Requis: Ingénieur ou Âge 4" : "Niveau technologique insuffisant";
      }
   }

   private boolean isBasicMaterial(Material mat) {
      return BASIC_MATERIALS.contains(mat);
   }

   private boolean isIronTool(Material mat) {
      return IRON_TOOLS.contains(mat);
   }

   private boolean isIronCombatGear(Material mat) {
      return IRON_COMBAT_GEAR.contains(mat);
   }

   private boolean isDiamondGear(Material mat) {
      return DIAMOND_GEAR.contains(mat);
   }

   private boolean isNetheriteGear(Material mat) {
      return NETHERITE_GEAR.contains(mat);
   }

   private boolean isArmor(Material mat) {
      return ARMOR_MATERIALS.contains(mat);
   }

   private boolean isPickaxe(Material mat) {
      return PICKAXE_MATERIALS.contains(mat);
   }

   private boolean isAdvancedRedstone(Material mat) {
      return ADVANCED_REDSTONE.contains(mat);
   }

   static {
      for (Material m : Material.values()) {
         String name = m.name();
         if (name.startsWith("WOODEN_")
            || name.startsWith("STONE_")
            || name.startsWith("LEATHER_")
            || m == Material.TORCH
            || m == Material.CAMPFIRE
            || m == Material.CRAFTING_TABLE
            || m == Material.FURNACE
            || m == Material.CHEST
            || m == Material.STICK
            || m == Material.BOWL
            || m == Material.BREAD) {
            BASIC_MATERIALS.add(m);
         }

         if (name.startsWith("DIAMOND_")
            && (name.endsWith("_HELMET") || name.endsWith("_CHESTPLATE") || name.endsWith("_LEGGINGS") || name.endsWith("_BOOTS") || name.endsWith("_SWORD")
               || name.endsWith("_AXE") || name.endsWith("_PICKAXE") || name.endsWith("_SHOVEL") || name.endsWith("_HOE"))) {
            // Seul l'équipement est restreint : le diamant, les blocs de diamant et le minerai restent utilisables (nécessaires à l'Âge 3).
            DIAMOND_GEAR.add(m);
         }

         if (name.startsWith("NETHERITE_")
            && (
               name.endsWith("_HELMET")
                  || name.endsWith("_CHESTPLATE")
                  || name.endsWith("_LEGGINGS")
                  || name.endsWith("_BOOTS")
                  || name.endsWith("_SWORD")
                  || name.endsWith("_AXE")
                  || name.endsWith("_PICKAXE")
                  || name.endsWith("_SHOVEL")
                  || name.endsWith("_HOE")
            )) {
            // Seul l'équipement est restreint : lingots, blocs, débris et templates restent craftables (nécessaires à l'Âge 5).
            NETHERITE_GEAR.add(m);
         }

         if (name.endsWith("_HELMET") || name.endsWith("_CHESTPLATE") || name.endsWith("_LEGGINGS") || name.endsWith("_BOOTS")) {
            ARMOR_MATERIALS.add(m);
         }

         if (name.endsWith("_PICKAXE")) {
            PICKAXE_MATERIALS.add(m);
         }
      }
   }
}
