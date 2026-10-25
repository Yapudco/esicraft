package fr.esicraft.economy.listener;

import fr.esicraft.EsicraftPlugin;
import fr.esicraft.data.PlayerData;
import fr.esicraft.guild.Guild;
import fr.esicraft.util.ChatUtil;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.PlayerDeathEvent;

public class PlayerKillEconomyListener implements Listener {
   private final EsicraftPlugin plugin;
   private final Map<String, Long> lastPvPKillTimes = new ConcurrentHashMap<>();
   private static final long KILL_COOLDOWN_MS = 120000L;

   public PlayerKillEconomyListener(EsicraftPlugin plugin) {
      this.plugin = plugin;
   }

   @EventHandler(
      priority = EventPriority.HIGH
   )
   public void onPlayerDeath(PlayerDeathEvent event) {
      Player victim = event.getEntity();
      Player killer = victim.getKiller();
      if (killer != null && !killer.getUniqueId().equals(victim.getUniqueId())) {
         Guild victimGuild = this.plugin.getGuildManager().getGuildByPlayer(victim.getUniqueId());
         Guild killerGuild = this.plugin.getGuildManager().getGuildByPlayer(killer.getUniqueId());
         if (victimGuild == null || killerGuild == null || !victimGuild.getId().equals(killerGuild.getId())) {
            UUID u1 = killer.getUniqueId();
            UUID u2 = victim.getUniqueId();
            String pairKey = u1.compareTo(u2) < 0 ? u1 + ":" + u2 : u2 + ":" + u1;
            long now = System.currentTimeMillis();
            if (this.lastPvPKillTimes.size() > 50) {
               this.lastPvPKillTimes.entrySet().removeIf(entry -> now - entry.getValue() > 240000L);
            }

            Long lastKill = this.lastPvPKillTimes.get(pairKey);
            if (lastKill != null && now - lastKill < 120000L) {
               ChatUtil.sendMessage(killer, "&c⚠️ Anti-Farm : Vous avez combattu &e" + victim.getName() + " &ctrop récemment pour obtenir une nouvelle prime.");
            } else {
               this.lastPvPKillTimes.put(pairKey, now);
               PlayerData victimData = this.plugin.getDataManager().getPlayerData(victim.getUniqueId());
               PlayerData killerData = this.plugin.getDataManager().getPlayerData(killer.getUniqueId());
               if (victimData != null && killerData != null) {
                  double victimFortune = Math.max(0.0, victimData.getCoins());
                  if (victimFortune <= 0.0) {
                     ChatUtil.sendMessage(killer, "&e⚔️ Vous avez éliminé &c" + victim.getName() + " &emais sa bourse était vide ! Aucune prime récoltée.");
                     ChatUtil.sendMessage(victim, "&cVous avez été éliminé par &e" + killer.getName() + " &c(votre bourse était déjà vide).");
                     return;
                  }

                  double onePercent = victimFortune * 0.01;
                  double baseBounty = 100.0;
                  boolean totalBankrupt = false;
                  double actualLoss;
                  if (victimFortune <= baseBounty) {
                     actualLoss = victimFortune;
                     totalBankrupt = true;
                  } else {
                     actualLoss = Math.min(victimFortune, baseBounty + onePercent);
                  }

                  double totalBounty = actualLoss;
                  victimData.removeCoins(actualLoss);
                  killerData.addCoins(totalBounty);
                  this.plugin.getDataManager().savePlayer(victimData);
                  this.plugin.getDataManager().savePlayer(killerData);
                  ChatUtil.sendRaw(killer, "&8&m----------------------------------------");
                  ChatUtil.sendRaw(killer, "  &a&l⚔️ PRIME DE VICTOIRE PvP !");
                  ChatUtil.sendRaw(killer, "  &7Victime : &e" + victim.getName() + (victimGuild != null ? " &8[&6" + victimGuild.getTag() + "&8]" : ""));
                  if (totalBankrupt) {
                     ChatUtil.sendRaw(killer, "  &7Pillage intégral : &6+" + String.format("%.2f", totalBounty) + " pièces &7(Victime fauchée)");
                  } else {
                     ChatUtil.sendRaw(killer, "  &7Prime de base : &6+100.00 pièces");
                     ChatUtil.sendRaw(killer, "  &7Pillage (1% fortune) : &6+" + String.format("%.2f", onePercent) + " pièces");
                  }
                  ChatUtil.sendRaw(killer, "  &a&lTotal crédité : &e+" + String.format("%.2f", totalBounty) + " pièces &a!");
                  ChatUtil.sendRaw(killer, "&8&m----------------------------------------");
                  ChatUtil.sendActionbar(killer, "&a⚔️ +" + String.format("%.2f", totalBounty) + " pièces (Kill: " + victim.getName() + ")");
                  ChatUtil.playSound(killer, Sound.ENTITY_PLAYER_LEVELUP, 1.0F, 1.5F);
                  ChatUtil.sendRaw(victim, "&8&m----------------------------------------");
                  ChatUtil.sendRaw(victim, "  &c&l☠️ DÉFAITE AU COMBAT PvP !");
                  ChatUtil.sendRaw(victim, "  &7Vainqueur : &e" + killer.getName() + (killerGuild != null ? " &8[&6" + killerGuild.getTag() + "&8]" : ""));
                  if (totalBankrupt) {
                     ChatUtil.sendRaw(victim, "  &c&l⚠️ Solde inférieur à 100 pièces : Vous avez perdu TOUT votre argent !");
                     ChatUtil.sendRaw(
                        victim, "  &c• Montant total perdu : &c-" + String.format("%.2f", actualLoss) + " pièces &7(Nouveau solde : &e0.00 pièces&7)"
                     );
                  } else {
                     ChatUtil.sendRaw(victim, "  &7Pénalité de mort : &c-100.00 pièces");
                     ChatUtil.sendRaw(victim, "  &7Pillage (1% fortune) : &c-" + String.format("%.2f", onePercent) + " pièces");
                     ChatUtil.sendRaw(
                        victim,
                        "  &c&lTotal débité : &c-"
                           + String.format("%.2f", actualLoss)
                           + " pièces &7(Nouveau solde : &e"
                           + String.format("%.2f", victimData.getCoins())
                           + " pièces&7)"
                     );
                  }

                  ChatUtil.sendRaw(victim, "&8&m----------------------------------------");
                  ChatUtil.sendActionbar(victim, "&c☠️ -" + String.format("%.2f", actualLoss) + " pièces (Tué par " + killer.getName() + ")");
                  ChatUtil.playSound(victim, Sound.ENTITY_WITHER_DEATH, 0.6F, 0.9F);
               }
            }
         }
      }
   }
}
