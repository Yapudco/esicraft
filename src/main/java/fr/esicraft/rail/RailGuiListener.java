package fr.esicraft.rail;

import fr.esicraft.EsicraftPlugin;
import fr.esicraft.guild.Guild;
import fr.esicraft.guild.GuildRole;
import fr.esicraft.util.ChatUtil;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.InventoryHolder;

/** Clics dans les menus /station (voyage) et /rail (gestion). Les clics sont déjà annulés par le listener GUI global. */
public class RailGuiListener implements Listener {
   private final EsicraftPlugin plugin;

   public RailGuiListener(EsicraftPlugin plugin) {
      this.plugin = plugin;
   }

   @EventHandler(priority = EventPriority.HIGHEST)
   public void onClick(InventoryClickEvent event) {
      if (!(event.getWhoClicked() instanceof Player player)) {
         return;
      }

      InventoryHolder holder = event.getInventory().getHolder();
      if (holder instanceof RailGui.TravelHolder travel) {
         event.setCancelled(true);
         this.handleTravel(player, travel, event.getRawSlot(), event);
      } else if (holder instanceof RailGui.ManageHolder manage) {
         event.setCancelled(true);
         this.handleManage(player, manage, event.getRawSlot(), event.isShiftClick());
      }
   }

   private void handleTravel(Player player, RailGui.TravelHolder holder, int slot, InventoryClickEvent event) {
      if (slot < 0 || slot >= 54 || event.getClickedInventory() != event.getView().getTopInventory()) {
         return;
      }

      if (slot == 45 && holder.page > 0) {
         holder.page--;
         RailGui.renderTravel(this.plugin, player, holder);
      } else if (slot == 53 && (holder.page + 1) * RailGui.PAGE_SIZE < holder.trips.size()) {
         holder.page++;
         RailGui.renderTravel(this.plugin, player, holder);
      } else if (slot < RailGui.PAGE_SIZE) {
         int index = holder.page * RailGui.PAGE_SIZE + slot;
         if (index < holder.trips.size()) {
            RailSearch.Reached trip = holder.trips.get(index);
            player.closeInventory();
            this.plugin.getRailManager().travel(player, trip);
         }
      }
   }

   private void handleManage(Player player, RailGui.ManageHolder holder, int slot, boolean shift) {
      Guild guild = this.plugin.getGuildManager().getGuildById(holder.guildId);
      if (guild == null || !guild.hasRoleAtLeast(player.getUniqueId(), GuildRole.OFFICER)) {
         player.closeInventory();
         ChatUtil.sendMessage(player, "&cRéservé aux officiers de la guilde.");
         return;
      }

      RailManager rails = this.plugin.getRailManager();
      if (holder.kind == RailGui.Kind.MAIN) {
         double delta = switch (slot) {
            case 10 -> -10.0;
            case 11 -> -1.0;
            case 15 -> 1.0;
            case 16 -> 10.0;
            default -> 0.0;
         };
         if (delta != 0.0) {
            rails.setToll(guild.getId(), rails.getToll(guild.getId()) + delta);
            ChatUtil.playSound(player, Sound.UI_BUTTON_CLICK, 0.6F, 1.2F);
            RailGui.openMain(this.plugin, player, guild);
         } else if (slot == 31) {
            RailGui.openRelations(this.plugin, player, guild, 0);
         } else if (shift && holder.stationSlots.size() > 0) {
            int[] slots = {28, 29, 30, 32, 33, 34, 37, 38, 39, 40, 41, 42, 43};
            for (int i = 0; i < holder.stationSlots.size() && i < slots.length; i++) {
               if (slots[i] == slot) {
                  Station s = holder.stationSlots.get(i);
                  rails.removeStation(s);
                  ChatUtil.sendMessage(player, "&e🚉 Gare &6" + s.getName() + " &edémantelée (pensez à retirer le panneau).");
                  RailGui.openMain(this.plugin, player, guild);
                  break;
               }
            }
         }
      } else {
         if (slot == 49) {
            RailGui.openMain(this.plugin, player, guild);
         } else if (slot == 45 && holder.page > 0) {
            RailGui.openRelations(this.plugin, player, guild, holder.page - 1);
         } else if (slot == 53 && (holder.page + 1) * RailGui.PAGE_SIZE < this.plugin.getGuildManager().getAllGuilds().size() - 1) {
            RailGui.openRelations(this.plugin, player, guild, holder.page + 1);
         } else if (slot >= 0 && slot < holder.guildSlots.size()) {
            Guild other = holder.guildSlots.get(slot);
            if (rails.isTreatyLocked(guild.getId(), other.getId())) {
               ChatUtil.sendMessage(player, "&c❌ Un traité impose le passage gratuit à &6" + other.getName() + "&c : rompez-le pour changer la relation.");
               return;
            }

            Relation next = rails.getRelation(guild.getId(), other.getId()).next();
            rails.setRelation(guild.getId(), other.getId(), next);
            ChatUtil.playSound(player, Sound.UI_BUTTON_CLICK, 0.6F, 1.0F);
            ChatUtil.broadcastGuild(guild, "&b🚆 " + player.getName() + " a défini la relation ferroviaire avec &f" + other.getName() + " &b: " + next.getLabel());
            RailGui.openRelations(this.plugin, player, guild, holder.page);
         }
      }
   }
}
