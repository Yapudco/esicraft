package fr.esicraft.treaty;

import fr.esicraft.EsicraftPlugin;
import fr.esicraft.guild.Guild;
import fr.esicraft.util.ChatUtil;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;

/** Clics dans les menus /traite. L'annulation des clics est assurée par le listener GUI global. */
public class TreatyGuiListener implements Listener {
   private final EsicraftPlugin plugin;

   public TreatyGuiListener(EsicraftPlugin plugin) {
      this.plugin = plugin;
   }

   @EventHandler(priority = EventPriority.HIGHEST)
   public void onClick(InventoryClickEvent event) {
      if (!(event.getWhoClicked() instanceof Player player) || !(event.getInventory().getHolder() instanceof TreatyGui.Holder holder)) {
         return;
      }

      event.setCancelled(true);
      if (event.getClickedInventory() != event.getView().getTopInventory()) {
         return;
      }

      Guild guild = this.plugin.getGuildManager().getGuildById(holder.guildId);
      if (guild == null) {
         player.closeInventory();
         return;
      }

      int slot = event.getRawSlot();
      switch (holder.kind) {
         case MAIN -> this.handleMain(player, guild, holder, slot, event.isShiftClick(), event.isRightClick());
         case PICK_GUILD -> this.handlePickGuild(player, guild, holder, slot);
         case PICK_TYPE -> this.handlePickType(player, guild, holder, slot);
      }
   }

   private void handleMain(Player player, Guild guild, TreatyGui.Holder holder, int slot, boolean shift, boolean right) {
      TreatyManager tm = this.plugin.getTreatyManager();
      String error = null;
      if (slot == 49) {
         TreatyGui.openPickGuild(this.plugin, player, guild, 0);
         return;
      }

      if (slot >= 0 && slot < holder.treatySlots.size() && shift) {
         error = tm.breakTreaty(holder.treatySlots.get(slot), guild, player);
      } else if (slot >= 27 && slot - 27 < holder.receivedSlots.size()) {
         Proposal p = holder.receivedSlots.get(slot - 27);
         error = right ? tm.refuse(guild, p, player) : tm.accept(guild, p, player);
      } else if (slot >= 36 && slot - 36 < holder.sentSlots.size()) {
         if (!guild.hasRoleAtLeast(player.getUniqueId(), fr.esicraft.guild.GuildRole.OFFICER)) {
            error = "Réservé aux officiers et au chef de guilde.";
         } else {
            tm.cancel(holder.sentSlots.get(slot - 36));
         }
      } else {
         return;
      }

      if (error != null) {
         ChatUtil.sendMessage(player, "&c❌ " + error);
         ChatUtil.playSound(player, Sound.ENTITY_VILLAGER_NO, 1.0F, 1.0F);
      } else {
         ChatUtil.playSound(player, Sound.UI_BUTTON_CLICK, 0.6F, 1.2F);
      }

      TreatyGui.openMain(this.plugin, player, guild);
   }

   private void handlePickGuild(Player player, Guild guild, TreatyGui.Holder holder, int slot) {
      if (slot == 49) {
         TreatyGui.openMain(this.plugin, player, guild);
      } else if (slot == 45 && holder.page > 0) {
         TreatyGui.openPickGuild(this.plugin, player, guild, holder.page - 1);
      } else if (slot == 53) {
         TreatyGui.openPickGuild(this.plugin, player, guild, holder.page + 1);
      } else if (slot >= 0 && slot < holder.guildSlots.size()) {
         TreatyGui.openPickType(this.plugin, player, guild, holder.guildSlots.get(slot));
      }
   }

   private void handlePickType(Player player, Guild guild, TreatyGui.Holder holder, int slot) {
      Guild target = this.plugin.getGuildManager().getGuildById(holder.targetId);
      if (slot == 22 || target == null) {
         TreatyGui.openMain(this.plugin, player, guild);
         return;
      }

      TreatyType type;
      String suzerain = null;
      switch (slot) {
         case 10 -> type = TreatyType.ALLIANCE;
         case 12 -> type = TreatyType.PACT;
         case 14 -> {
            type = TreatyType.VASSALAGE;
            suzerain = guild.getId();
         }
         case 16 -> {
            type = TreatyType.VASSALAGE;
            suzerain = target.getId();
         }
         default -> {
            return;
         }
      }

      String error = this.plugin.getTreatyManager().propose(guild, target, type, suzerain, player);
      if (error != null) {
         ChatUtil.sendMessage(player, "&c❌ " + error);
         ChatUtil.playSound(player, Sound.ENTITY_VILLAGER_NO, 1.0F, 1.0F);
      } else {
         ChatUtil.sendMessage(player, "&a📜 Proposition de " + type.getLabel() + " envoyée à &6" + target.getName() + "&a.");
         ChatUtil.playSound(player, Sound.ENTITY_PLAYER_LEVELUP, 0.6F, 1.4F);
      }

      TreatyGui.openMain(this.plugin, player, guild);
   }
}
