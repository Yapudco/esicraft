package fr.esicraft.gui;

import fr.esicraft.EsicraftPlugin;
import fr.esicraft.age.AgeType;
import fr.esicraft.data.PlayerData;
import fr.esicraft.gui.holder.RaceShowHolder;
import fr.esicraft.guild.Guild;
import fr.esicraft.race.Race;
import fr.esicraft.util.ChatUtil;
import fr.esicraft.util.ItemBuilder;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.attribute.Attribute;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;

public class RaceShowGUI {
   private static final int PAGE_SIZE = 28;

   public static void open(Player viewer, EsicraftPlugin plugin) {
      open(viewer, null, 1, plugin);
   }

   public static void open(Player viewer, Player highlightedTarget, EsicraftPlugin plugin) {
      open(viewer, highlightedTarget, 1, plugin);
   }

   public static void open(Player viewer, Player highlightedTarget, int page, EsicraftPlugin plugin) {
      List<Player> onlinePlayers = new ArrayList<>(Bukkit.getOnlinePlayers());
      onlinePlayers.sort(Comparator.comparing(Player::getName, String.CASE_INSENSITIVE_ORDER));
      if (highlightedTarget != null && onlinePlayers.contains(highlightedTarget)) {
         onlinePlayers.remove(highlightedTarget);
         onlinePlayers.add(0, highlightedTarget);
      }

      int totalPages = Math.max(1, (int)Math.ceil((double)onlinePlayers.size() / 28.0));
      page = Math.max(1, Math.min(page, totalPages));
      Inventory inv = Bukkit.createInventory(
         new RaceShowHolder(page), 54, ChatUtil.component("&8» &6Métiers & Joueurs en ligne &7(Page " + page + "/" + totalPages + ")")
      );
      ItemStack filler = new ItemBuilder(Material.GRAY_STAINED_GLASS_PANE).name(" ").build();

      for (int i = 0; i < 54; i++) {
         inv.setItem(i, filler);
      }

      Map<Race, Integer> countByRace = new EnumMap<>(Race.class);

      for (Player p : onlinePlayers) {
         Race r = plugin.getRaceManager().getRace(p);
         countByRace.put(r, countByRace.getOrDefault(r, 0) + 1);
      }

      List<String> statsLore = new ArrayList<>();
      statsLore.add("&7Joueurs connectés : &e" + onlinePlayers.size());
      statsLore.add("");
      statsLore.add("&6&lRépartition des Métiers :");

      for (Race r : Race.values()) {
         if (r != Race.NONE) {
            int count = countByRace.getOrDefault(r, 0);
            statsLore.add(" &8• &f" + r.getDisplayName() + " &7: &e" + count);
         }
      }

      int noneCount = countByRace.getOrDefault(Race.NONE, 0);
      if (noneCount > 0) {
         statsLore.add(" &8• &7Sans Métier : &c" + noneCount);
      }

      inv.setItem(4, new ItemBuilder(Material.PLAYER_HEAD).name("&6&l\ud83d\udcca Annuaire des Joueurs & Métiers").lore(statsLore).build());
      int[] playerSlots = new int[]{10, 11, 12, 13, 14, 15, 16, 19, 20, 21, 22, 23, 24, 25, 28, 29, 30, 31, 32, 33, 34, 37, 38, 39, 40, 41, 42, 43};
      int startIndex = (page - 1) * 28;
      int endIndex = Math.min(startIndex + 28, onlinePlayers.size());

      for (int i = startIndex; i < endIndex; i++) {
         Player target = onlinePlayers.get(i);
         int slot = playerSlots[i - startIndex];
         Race race = plugin.getRaceManager().getRace(target);
         Guild guild = plugin.getGuildManager().getGuildByPlayer(target.getUniqueId());
         AgeType age = plugin.getAgeManager().getPlayerAge(target);
         PlayerData data = plugin.getDataManager().getPlayerData(target.getUniqueId());
         double coins = data != null ? data.getCoins() : 0.0;
         boolean isViewer = target.getUniqueId().equals(viewer.getUniqueId());
         boolean isHighlighted = highlightedTarget != null && target.getUniqueId().equals(highlightedTarget.getUniqueId());
         List<String> lore = new ArrayList<>();
         lore.add("&7━━━━━━━━━━━━━━━━━━━━━━━━━━━");
         lore.add("&e\ud83c\udfad Métier : &f" + race.getDisplayName());
         lore.add("&7&o" + race.getDescription());
         lore.add("");
         if (guild != null) {
            lore.add("&e\ud83c\udfdb Guilde : &6" + guild.getName() + " &7[" + guild.getTag() + "]");
            lore.add("&e\ud83c\udf96 Rôle : &f" + guild.getRole(target.getUniqueId()).name());
         } else {
            lore.add("&e\ud83c\udfdb Guilde : &7Aucune");
         }

         lore.add("&e⏳ Âge : &f" + age.getDisplayName());
         lore.add("&e\ud83d\udcb0 Solde : &6" + String.format("%.2f", coins) + " pièces");
         lore.add("");
         double maxHealth = target.getAttribute(Attribute.GENERIC_MAX_HEALTH) != null ? target.getAttribute(Attribute.GENERIC_MAX_HEALTH).getValue() : 20.0;
         lore.add("&c❤ Santé : &f" + String.format("%.1f", target.getHealth()) + " / " + String.format("%.1f", maxHealth) + " HP");
         lore.add("&a✨ Niveau XP : &f" + target.getLevel());
         lore.add("&b\ud83d\udcf6 Ping : &f" + target.getPing() + " ms");
         lore.add("&d\ud83c\udf0d Monde : &f" + target.getWorld().getName());
         lore.add("&7━━━━━━━━━━━━━━━━━━━━━━━━━━━");
         String title = (isHighlighted ? "&6&l⭐ " : "&e&l") + target.getName() + (isViewer ? " &a(Vous)" : "");
         ItemBuilder headBuilder = new ItemBuilder(Material.PLAYER_HEAD).skullOwner(target).name(title).lore(lore);
         if (isHighlighted || isViewer) {
            headBuilder.glow();
         }

         inv.setItem(slot, headBuilder.build());
      }

      if (page > 1) {
         inv.setItem(45, new ItemBuilder(Material.ARROW).name("&e« Page Précédente &7(" + (page - 1) + "/" + totalPages + ")").build());
      }

      inv.setItem(49, new ItemBuilder(Material.BARRIER).name("&cFermer le menu").build());
      if (page < totalPages) {
         inv.setItem(53, new ItemBuilder(Material.ARROW).name("&ePage Suivante » &7(" + (page + 1) + "/" + totalPages + ")").build());
      }

      viewer.openInventory(inv);
      ChatUtil.playSound(viewer, Sound.BLOCK_CHEST_OPEN, 1.0F, 1.2F);
   }
}
