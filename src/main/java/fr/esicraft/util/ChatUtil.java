package fr.esicraft.util;

import fr.esicraft.guild.Guild;
import java.util.UUID;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.bukkit.Bukkit;
import org.bukkit.Sound;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public final class ChatUtil {
   public static final String PREFIX = "&8[&6&lEsicraft&8] &r";
   private static final LegacyComponentSerializer SERIALIZER = LegacyComponentSerializer.builder()
      .character('&')
      .hexColors()
      .useUnusualXRepeatedCharacterHexFormat()
      .build();

   private ChatUtil() {
   }

   public static String color(String message) {
      return message == null ? "" : LegacyComponentSerializer.legacySection().serialize(SERIALIZER.deserialize(message));
   }

   public static Component component(String message) {
      return message == null ? Component.empty() : SERIALIZER.deserialize(message);
   }

   public static void sendMessage(CommandSender sender, String message) {
      if (sender != null && message != null) {
         sender.sendMessage(component("&8[&6&lEsicraft&8] &r" + message));
      }
   }

   public static void sendRaw(CommandSender sender, String message) {
      if (sender != null && message != null) {
         sender.sendMessage(component(message));
      }
   }

   public static void sendActionbar(Player player, String message) {
      if (player != null && player.isOnline() && message != null) {
         player.sendActionBar(component(message));
      }
   }

   public static void playSound(Player player, Sound sound, float volume, float pitch) {
      if (player != null && player.isOnline()) {
         player.playSound(player.getLocation(), sound, volume, pitch);
      }
   }

   public static void broadcast(String message) {
      Bukkit.broadcast(component("&8[&6&lEsicraft&8] &r" + message));
   }

   public static void broadcastGuild(Guild guild, String message) {
      if (guild != null && message != null) {
         for (UUID memberUuid : guild.getMembers().keySet()) {
            Player p = Bukkit.getPlayer(memberUuid);
            if (p != null && p.isOnline()) {
               sendMessage(p, message);
            }
         }
      }
   }
}
