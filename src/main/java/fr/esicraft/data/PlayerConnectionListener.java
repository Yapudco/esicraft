package fr.esicraft.data;

import fr.esicraft.EsicraftPlugin;
import fr.esicraft.age.AgeType;
import fr.esicraft.race.Race;
import fr.esicraft.util.BookUtil;
import fr.esicraft.util.ChatUtil;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.AsyncPlayerPreLoginEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.player.AsyncPlayerPreLoginEvent.Result;
import org.bukkit.inventory.ItemStack;

public class PlayerConnectionListener implements Listener {
   private final EsicraftPlugin plugin;

   public PlayerConnectionListener(EsicraftPlugin plugin) {
      this.plugin = plugin;
   }

   // MONITOR : les refus des autres plugins (ban, liste blanche) sont déjà connus ; un compte préchargé d'une connexion avortée est oublié.
   @EventHandler(
      priority = EventPriority.MONITOR
   )
   public void onAsyncPreLogin(AsyncPlayerPreLoginEvent event) {
      if (event.getLoginResult() == Result.ALLOWED) {
         java.util.UUID uuid = event.getUniqueId();
         this.plugin.getDataManager().preloadPlayer(uuid, event.getName());
         org.bukkit.Bukkit.getScheduler().runTaskLater(this.plugin, () -> this.plugin.getDataManager().discardPreloaded(uuid), 600L);
      }
   }

   @EventHandler(
      priority = EventPriority.LOWEST
   )
   public void onPlayerJoin(PlayerJoinEvent event) {
      Player player = event.getPlayer();
      PlayerData data = this.plugin.getDataManager().loadPlayer(player);
      if (data == null) {
         player.kick(ChatUtil.component("&cVos données n'ont pas pu être chargées. Réessayez dans un instant."));
         return;
      }

      data.setSessionLoginTime(System.currentTimeMillis());
      this.plugin.getSkins().remember(player);
      if (this.plugin.getWebServer() != null) {
         this.plugin.getWebServer().rememberSkin(player);
      }

      this.plugin.getGuildManager().reconcilePlayerGuild(player, data);
      this.plugin.getRaceManager().applyRaceAttributes(player, data.getRace());
      this.plugin.getGuildManager().updatePlayerTab(player);
      if (!player.hasPlayedBefore()) {
         player.getInventory().addItem(new ItemStack[]{BookUtil.createGuideBook(fr.esicraft.util.Lang.isEnglish(player))});
         ChatUtil.sendMessage(player, "&6Un livre explicatif des règles &e\"Guide Esicraft\" &6a été ajouté à votre inventaire !");
         ChatUtil.sendMessage(player, "&7(Vous pourrez toujours en réclamer un nouveau avec &e/guide&7)");
      }

      AgeType age = this.plugin.getAgeManager().getPlayerAge(player);
      ChatUtil.sendRaw(player, "&8&m--------------------------------------------------");
      ChatUtil.sendRaw(player, " &6&lBienvenue sur Esicraft !");
      ChatUtil.sendRaw(player, " &7Classe actuelle : &e" + data.getRace().getDisplayName());
      ChatUtil.sendRaw(player, " &7Âge de Guilde : &6" + age.getDisplayName() + " &7(Niveau " + age.getLevel() + ")");
      ChatUtil.sendRaw(player, " &7Solde personnel : &a" + String.format("%.2f", data.getCoins()) + " pièces");
      if (data.getRace() == Race.NONE) {
         ChatUtil.sendRaw(player, " &c⚠ Vous n'avez pas encore choisi de classe ! Faites &e/race &cpour débuter.");
      }

      ChatUtil.sendRaw(player, "&8&m--------------------------------------------------");
   }

   @EventHandler(
      priority = EventPriority.MONITOR
   )
   public void onPlayerQuit(PlayerQuitEvent event) {
      fr.esicraft.gui.CustomHordeGUI.clearAll(event.getPlayer().getUniqueId());
      this.plugin.getDataManager().unloadPlayer(event.getPlayer().getUniqueId());
      this.plugin.getClaimVisualizerManager().stopVisualizer(event.getPlayer());
   }
}
