package fr.esicraft.chat;

import fr.esicraft.EsicraftPlugin;
import fr.esicraft.age.AgeType;
import fr.esicraft.data.PlayerData;
import fr.esicraft.guild.Guild;
import fr.esicraft.util.ChatUtil;
import io.papermc.paper.chat.ChatRenderer;
import io.papermc.paper.event.player.AsyncChatEvent;
import net.kyori.adventure.audience.Audience;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;

public class ChatFormatListener implements Listener, ChatRenderer {
   private final EsicraftPlugin plugin;

   public ChatFormatListener(EsicraftPlugin plugin) {
      this.plugin = plugin;
   }

   @EventHandler(
      priority = EventPriority.HIGHEST,
      ignoreCancelled = true
   )
   public void onAsyncChat(AsyncChatEvent event) {
      event.renderer(this);
   }

   public Component render(Player source, Component sourceDisplayName, Component message, Audience viewer) {
      Guild guild = this.plugin.getGuildManager().getGuildByPlayer(source.getUniqueId());
      StringBuilder prefixBuilder = new StringBuilder();
      fr.esicraft.life.DisguiseManager.Disguise disguise = this.plugin.getDisguiseManager().get(source.getUniqueId());
      if (disguise != null) {
         prefixBuilder.append(disguise.hasGuild() ? "&8[&6" + disguise.tag() + "&8] " : "&8[&7Sans Guilde&8] ");
      } else if (guild != null) {
         prefixBuilder.append(this.plugin.getSeasonManager().isChampion(guild.getId()) ? "&6👑 " : "").append("&8[&6").append(guild.getTag()).append("&8] ");
      } else {
         prefixBuilder.append("&8[&7Sans Guilde&8] ");
      }

      AgeType age = this.plugin.getAgeManager().getPlayerAge(source);
      if (age == AgeType.SUPREMACY && disguise == null) {
         PlayerData data = this.plugin.getDataManager().getPlayerData(source.getUniqueId());
         long sessionDurationMs = data != null ? System.currentTimeMillis() - data.getSessionLoginTime() : 0L;
         if (sessionDurationMs > 3600000L) {
            prefixBuilder.append("&5[&dGros Nerd&5] ");
         } else {
            prefixBuilder.append("&3[&bGiga Tchad&3] ");
         }
      }

      return ChatUtil.component(prefixBuilder.toString())
         .append(sourceDisplayName.color(NamedTextColor.WHITE))
         .append(ChatUtil.component(" &8» &f"))
         .append(message);
   }
}
