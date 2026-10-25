package fr.esicraft.treaty;

import fr.esicraft.EsicraftPlugin;
import fr.esicraft.guild.Guild;
import fr.esicraft.guild.GuildRole;
import fr.esicraft.rail.Relation;
import fr.esicraft.util.ChatUtil;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

/**
 * Traités entre guildes : alliance, pacte de non-agression, vassalité.
 * <ul>
 *   <li>Tant qu'un traité lie deux guildes, aucune ne peut lancer de horde contre l'autre.</li>
 *   <li>Alliance et vassalité : passage ferroviaire gratuit et pas de dégâts entre joueurs.</li>
 *   <li>Rompre un traité (hors libération d'un vassal par son suzerain) coûte une indemnité à l'autre guilde et
 *       interdit à la guilde traître de l'attaquer pendant un préavis.</li>
 * </ul>
 */
public class TreatyManager {
   private static final long DAY_MS = 86400000L;

   private final EsicraftPlugin plugin;
   private final Map<String, Treaty> treaties = new ConcurrentHashMap<>();
   private final Map<String, Treaty> byPair = new ConcurrentHashMap<>();
   private final List<Proposal> proposals = new CopyOnWriteArrayList<>();
   /** Préavis : clé « traître>victime » → fin du préavis (le traître ne peut pas attaquer la victime). */
   private final Map<String, Long> grace = new ConcurrentHashMap<>();

   public TreatyManager(EsicraftPlugin plugin) {
      this.plugin = plugin;
   }

   // ------------------------------------------------------------------ persistance et cycle de vie

   public void load() {
      for (Object[] row : this.plugin.getDataManager().getStorage().select(
         "SELECT id, a_id, b_id, type, suzerain_id, created_at, expires_at, last_tribute FROM treaties;", 8)) {
         try {
            Treaty t = new Treaty((String)row[0], (String)row[1], (String)row[2], TreatyType.valueOf((String)row[3]), (String)row[4],
               ((Number)row[5]).longValue(), ((Number)row[6]).longValue(), ((Number)row[7]).longValue());
            this.index(t);
         } catch (IllegalArgumentException ignored) {
         }
      }

      long now = System.currentTimeMillis();
      for (Object[] row : this.plugin.getDataManager().getStorage().select("SELECT breaker_id, victim_id, until FROM treaty_grace;", 3)) {
         long until = ((Number)row[2]).longValue();
         if (until > now) {
            this.grace.put(row[0] + ">" + row[1], until);
         }
      }

      this.plugin.getLogger().info("Traités : " + this.treaties.size() + " traité(s) en vigueur.");
      Bukkit.getScheduler().runTaskTimer(this.plugin, this::tick, 1200L, 1200L);
   }

   public void clearAll() {
      this.treaties.clear();
      this.byPair.clear();
      this.proposals.clear();
      this.grace.clear();
   }

   private static String pairKey(String a, String b) {
      return a.compareTo(b) < 0 ? a + "|" + b : b + "|" + a;
   }

   private void index(Treaty t) {
      this.treaties.put(t.getId(), t);
      this.byPair.put(pairKey(t.getAId(), t.getBId()), t);
   }

   private void unindex(Treaty t) {
      this.treaties.remove(t.getId());
      this.byPair.remove(pairKey(t.getAId(), t.getBId()));
      this.async(() -> this.plugin.getDataManager().getStorage().exec("DELETE FROM treaties WHERE id = ?;", t.getId()));
      this.plugin.getRailManager().invalidateCache();
   }

   private void async(Runnable r) {
      if (this.plugin.isEnabled()) {
         Bukkit.getScheduler().runTaskAsynchronously(this.plugin, r);
      } else {
         r.run();
      }
   }

   private boolean enabled() {
      return this.plugin.getConfig().getBoolean("treaties.enabled", true);
   }

   /** Vérification périodique (1 min) : propositions expirées, pactes échus, guildes disparues, tributs de vassalité. */
   private void tick() {
      long now = System.currentTimeMillis();
      this.proposals.removeIf(p -> p.expiresAt() <= now);
      Iterator<Map.Entry<String, Long>> graceIt = this.grace.entrySet().iterator();
      while (graceIt.hasNext()) {
         Map.Entry<String, Long> e = graceIt.next();
         if (e.getValue() <= now) {
            graceIt.remove();
            String[] ids = e.getKey().split(">");
            this.async(() -> this.plugin.getDataManager().getStorage().exec("DELETE FROM treaty_grace WHERE breaker_id = ? AND victim_id = ?;", ids[0], ids[1]));
         }
      }

      for (Treaty t : new ArrayList<>(this.treaties.values())) {
         Guild a = this.plugin.getGuildManager().getGuildById(t.getAId());
         Guild b = this.plugin.getGuildManager().getGuildById(t.getBId());
         if (a == null || b == null) {
            this.unindex(t);
         } else if (t.getExpiresAt() > 0L && now >= t.getExpiresAt()) {
            this.unindex(t);
            String msg = "&e📜 Le " + t.getType().getLabel().toLowerCase(Locale.ROOT) + " entre &6" + a.getName() + " &eet &6" + b.getName() + " &es'est achevé.";
            ChatUtil.broadcastGuild(a, msg);
            ChatUtil.broadcastGuild(b, msg);
            this.plugin.getSeasonManager().addHistory("📜 Fin du " + t.getType().getLabel().toLowerCase(Locale.ROOT) + " entre " + a.getName() + " et " + b.getName() + ".");
         } else if (t.getType() == TreatyType.VASSALAGE && now - t.getLastTribute() >= DAY_MS) {
            this.collectTribute(t, a, b, now);
         }
      }
   }

   private void collectTribute(Treaty t, Guild a, Guild b, long now) {
      Guild suzerain = t.isSuzerain(a.getId()) ? a : b;
      Guild vassal = suzerain == a ? b : a;
      double percent = this.plugin.getConfig().getDouble("treaties.vassal-tribute-percent", 5.0);
      double amount = Math.floor(vassal.getBankBalance() * percent) / 100.0;
      t.setLastTribute(now);
      this.async(() -> this.plugin.getDataManager().getStorage().exec("UPDATE treaties SET last_tribute = ? WHERE id = ?;", now, t.getId()));
      if (amount >= 1.0 && vassal.withdrawBank(amount)) {
         suzerain.depositBank(amount);
         this.plugin.getDataManager().saveGuild(vassal);
         this.plugin.getDataManager().saveGuild(suzerain);
         String shown = String.format(Locale.ROOT, "%.2f", amount);
         ChatUtil.broadcastGuild(vassal, "&6👑 Tribut quotidien versé à &e" + suzerain.getName() + " &6: &c-" + shown + " pièces");
         ChatUtil.broadcastGuild(suzerain, "&6👑 Tribut de &e" + vassal.getName() + " &6: &a+" + shown + " pièces");
      }
   }

   // ------------------------------------------------------------------ requêtes

   public Treaty between(String guildA, String guildB) {
      return guildA == null || guildB == null ? null : this.byPair.get(pairKey(guildA, guildB));
   }

   public List<Treaty> treatiesOf(String guildId) {
      List<Treaty> list = new ArrayList<>();
      for (Treaty t : this.treaties.values()) {
         if (t.involves(guildId)) {
            list.add(t);
         }
      }

      list.sort((x, y) -> Long.compare(x.getCreatedAt(), y.getCreatedAt()));
      return list;
   }

   public List<Proposal> receivedBy(String guildId) {
      List<Proposal> list = new ArrayList<>();
      for (Proposal p : this.proposals) {
         if (p.toId().equals(guildId) && p.expiresAt() > System.currentTimeMillis()) {
            list.add(p);
         }
      }

      return list;
   }

   public List<Proposal> sentBy(String guildId) {
      List<Proposal> list = new ArrayList<>();
      for (Proposal p : this.proposals) {
         if (p.fromId().equals(guildId) && p.expiresAt() > System.currentTimeMillis()) {
            list.add(p);
         }
      }

      return list;
   }

   public boolean areFriends(String guildA, String guildB) {
      Treaty t = this.between(guildA, guildB);
      return t != null && t.getType().isFriendly();
   }

   /** Relation imposée par un traité pour le passage ferroviaire (null si aucun traité amical). */
   public Relation railOverride(String ownerGuildId, String otherGuildId) {
      return this.areFriends(ownerGuildId, otherGuildId) ? Relation.FREE : null;
   }

   public static String formatDuration(long ms) {
      long minutes = Math.max(0L, ms / 60000L);
      if (minutes >= 1440L) {
         return (minutes / 1440L) + " j " + ((minutes % 1440L) / 60L) + " h";
      }

      if (minutes >= 60L) {
         return (minutes / 60L) + " h " + (minutes % 60L) + " min";
      }

      return minutes + " min";
   }

   public double indemnity(Guild breaker) {
      return Math.min(this.plugin.getConfig().getDouble("treaties.break-indemnity", 500.0), breaker.getBankBalance());
   }

   /** Vrai si le geste de rompre ce traité par cette guilde est une « trahison » (indemnité + préavis). */
   public boolean isBetrayal(Treaty t, Guild breaker) {
      return switch (t.getType()) {
         case ALLIANCE -> true;
         case PACT -> t.getExpiresAt() <= 0L || System.currentTimeMillis() < t.getExpiresAt();
         case VASSALAGE -> !t.isSuzerain(breaker.getId());
      };
   }

   // ------------------------------------------------------------------ raids

   /** Vrai (avec message) si un traité ou un préavis interdit à l'attaquant de lancer une horde sur la cible. */
   public boolean blocksRaid(Guild attacker, Guild target, Player buyer) {
      if (!this.enabled() || attacker == null || target == null) {
         return false;
      }

      Treaty t = this.between(attacker.getId(), target.getId());
      if (t != null) {
         if (buyer != null) {
            ChatUtil.sendMessage(buyer, "&c❌ Un &e" + t.getType().getLabel() + " &cvous lie à &6" + target.getName() + "&c : rompez-le d'abord (&e/traite&c).");
         }

         return true;
      }

      Long until = this.grace.get(attacker.getId() + ">" + target.getId());
      if (until != null && until > System.currentTimeMillis()) {
         if (buyer != null) {
            ChatUtil.sendMessage(buyer, "&c❌ Vous avez rompu un traité avec &6" + target.getName() + "&c : préavis de &e" + formatDuration(until - System.currentTimeMillis()) + " &cavant de pouvoir l'attaquer.");
         }

         return true;
      }

      return false;
   }

   // ------------------------------------------------------------------ propositions

   private boolean isOfficer(Guild guild, Player actor) {
      return guild != null && actor != null && guild.hasRoleAtLeast(actor.getUniqueId(), GuildRole.OFFICER);
   }

   /** Renvoie un message d'erreur, ou null si la proposition a été envoyée. */
   public String propose(Guild from, Guild to, TreatyType type, String suzerainId, Player actor) {
      if (!this.enabled()) {
         return "Les traités sont désactivés.";
      }

      if (!this.isOfficer(from, actor)) {
         return "Réservé aux officiers et au chef de guilde.";
      }

      if (to == null || from.getId().equals(to.getId())) {
         return "Guilde cible invalide.";
      }

      if (this.between(from.getId(), to.getId()) != null) {
         return "Un traité lie déjà vos guildes : rompez-le d'abord.";
      }

      for (Proposal p : this.proposals) {
         if (pairKey(p.fromId(), p.toId()).equals(pairKey(from.getId(), to.getId())) && p.expiresAt() > System.currentTimeMillis()) {
            return "Une proposition est déjà en attente entre vos deux guildes.";
         }
      }

      if (type == TreatyType.VASSALAGE) {
         if (suzerainId == null || !suzerainId.equals(from.getId()) && !suzerainId.equals(to.getId())) {
            return "Suzerain invalide.";
         }

         String vassalId = suzerainId.equals(from.getId()) ? to.getId() : from.getId();
         if (this.isVassal(vassalId)) {
            return "Cette guilde est déjà vassale d'une autre guilde.";
         }
      }

      long expires = System.currentTimeMillis() + this.plugin.getConfig().getLong("treaties.proposal-minutes", 10L) * 60000L;
      this.proposals.add(new Proposal(from.getId(), to.getId(), type, type == TreatyType.VASSALAGE ? suzerainId : null, expires));
      ChatUtil.broadcastGuild(to, "&d📜 &6" + from.getName() + " &dvous propose un &e" + type.getLabel() + (type == TreatyType.VASSALAGE ? this.vassalSuffix(suzerainId, from, to) : "")
         + " &d! Ouvrez &e/traite &dpour répondre (&7" + this.plugin.getConfig().getLong("treaties.proposal-minutes", 10L) + " min&d).");
      return null;
   }

   private String vassalSuffix(String suzerainId, Guild from, Guild to) {
      return suzerainId.equals(from.getId()) ? " &d(" + from.getName() + " suzerain, vous vassal)" : " &d(vous suzerain, " + from.getName() + " vassal)";
   }

   public boolean isVassal(String guildId) {
      for (Treaty t : this.treaties.values()) {
         if (t.getType() == TreatyType.VASSALAGE && t.involves(guildId) && !t.isSuzerain(guildId)) {
            return true;
         }
      }

      return false;
   }

   public Proposal findProposal(String fromId, String toId) {
      for (Proposal p : this.proposals) {
         if (p.fromId().equals(fromId) && p.toId().equals(toId) && p.expiresAt() > System.currentTimeMillis()) {
            return p;
         }
      }

      return null;
   }

   /** Accepte une proposition reçue. Renvoie un message d'erreur ou null. */
   public String accept(Guild target, Proposal p, Player actor) {
      if (!this.isOfficer(target, actor)) {
         return "Réservé aux officiers et au chef de guilde.";
      }

      Guild from = this.plugin.getGuildManager().getGuildById(p.fromId());
      if (from == null || !this.proposals.remove(p)) {
         return "Cette proposition n'est plus valable.";
      }

      if (this.between(from.getId(), target.getId()) != null) {
         return "Un traité lie déjà vos guildes.";
      }

      if (p.type() == TreatyType.VASSALAGE && this.isVassal(p.suzerainId().equals(from.getId()) ? target.getId() : from.getId())) {
         return "Le vassal désigné est déjà vassal d'une autre guilde.";
      }

      long now = System.currentTimeMillis();
      long expires = p.type() == TreatyType.PACT ? now + this.plugin.getConfig().getLong("treaties.pact-days", 7L) * DAY_MS : 0L;
      Treaty t = new Treaty(UUID.randomUUID().toString(), from.getId(), target.getId(), p.type(), p.suzerainId(), now, expires, now);
      this.index(t);
      this.async(() -> this.plugin.getDataManager().getStorage().exec(
         "INSERT INTO treaties (id, a_id, b_id, type, suzerain_id, created_at, expires_at, last_tribute) VALUES (?, ?, ?, ?, ?, ?, ?, ?);",
         t.getId(), t.getAId(), t.getBId(), t.getType().name(), t.getSuzerainId(), t.getCreatedAt(), t.getExpiresAt(), t.getLastTribute()));
      if (p.type().isFriendly()) {
         // Le traité remplace toute relation ferroviaire manuelle entre les deux guildes.
         this.plugin.getRailManager().setRelation(from.getId(), target.getId(), Relation.NORMAL);
         this.plugin.getRailManager().setRelation(target.getId(), from.getId(), Relation.NORMAL);
      }

      this.plugin.getRailManager().invalidateCache();
      String text = switch (p.type()) {
         case ALLIANCE -> "🤝 ALLIANCE conclue entre &6" + from.getName() + " &eet &6" + target.getName() + " &e!";
         case PACT -> "📜 PACTE DE NON-AGRESSION entre &6" + from.getName() + " &eet &6" + target.getName() + " &e(" + this.plugin.getConfig().getLong("treaties.pact-days", 7L) + " jours) !";
         case VASSALAGE -> "👑 VASSALITÉ : &6" + (p.suzerainId().equals(from.getId()) ? target.getName() : from.getName()) + " &eprête allégeance à &6"
            + (p.suzerainId().equals(from.getId()) ? from.getName() : target.getName()) + " &e!";
      };
      ChatUtil.broadcast("&e&l" + text);
      this.plugin.getSeasonManager().addHistory(fr.esicraft.life.Chronicle.Kind.TREATY, ChatUtil.color(text).replaceAll("§.", ""), from.getId(), target.getId());
      this.plugin.getAchievements().grantGuild(from, fr.esicraft.life.Achievement.DIPLOMAT, true);
      this.plugin.getAchievements().grantGuild(target, fr.esicraft.life.Achievement.DIPLOMAT, true);
      return null;
   }

   public String refuse(Guild target, Proposal p, Player actor) {
      if (!this.isOfficer(target, actor)) {
         return "Réservé aux officiers et au chef de guilde.";
      }

      if (!this.proposals.remove(p)) {
         return "Cette proposition n'est plus valable.";
      }

      Guild from = this.plugin.getGuildManager().getGuildById(p.fromId());
      if (from != null) {
         ChatUtil.broadcastGuild(from, "&c📜 " + target.getName() + " a refusé votre proposition de " + p.type().getLabel() + ".");
      }

      return null;
   }

   /** Retire une proposition envoyée. */
   public boolean cancel(Proposal p) {
      return this.proposals.remove(p);
   }

   // ------------------------------------------------------------------ rupture

   public String breakTreaty(Treaty t, Guild breaker, Player actor) {
      if (!this.isOfficer(breaker, actor) || !t.involves(breaker.getId())) {
         return "Réservé aux officiers et au chef de guilde concernés.";
      }

      // Retrait atomique : deux officiers qui cliquent en même temps (menus ouverts sur un traité déjà rompu)
      // ne doivent pas payer deux indemnités ni rompre un traité qui n'existe plus.
      if (this.treaties.remove(t.getId()) == null) {
         return "Ce traité n'existe plus.";
      }

      Guild other = this.plugin.getGuildManager().getGuildById(t.other(breaker.getId()));
      boolean betrayal = this.isBetrayal(t, breaker);
      double paid = 0.0;
      this.unindex(t);
      if (other != null && betrayal) {
         paid = this.indemnity(breaker);
         if (paid > 0.0 && breaker.withdrawBank(paid)) {
            other.depositBank(paid);
            this.plugin.getDataManager().saveGuild(breaker);
            this.plugin.getDataManager().saveGuild(other);
         } else {
            paid = 0.0;
         }

         long until = System.currentTimeMillis() + this.plugin.getConfig().getLong("treaties.break-grace-minutes", 30L) * 60000L;
         this.grace.put(breaker.getId() + ">" + other.getId(), until);
         String breakerId = breaker.getId();
         String victimId = other.getId();
         this.async(() -> this.plugin.getDataManager().getStorage().exec(
            "INSERT INTO treaty_grace (breaker_id, victim_id, until) VALUES (?, ?, ?) ON CONFLICT(breaker_id, victim_id) DO UPDATE SET until = excluded.until;",
            breakerId, victimId, until));
      }

      String otherName = other != null ? other.getName() : "?";
      if (betrayal) {
         ChatUtil.broadcast("&4&l💔 TRAHISON ! &c" + breaker.getName() + " &7rompt son &e" + t.getType().getLabel() + " &7avec &6" + otherName
            + (paid > 0.0 ? " &7et lui verse &6" + String.format(Locale.ROOT, "%.0f", paid) + " pièces &7d'indemnité." : "."));
         this.plugin.getSeasonManager().addHistory(fr.esicraft.life.Chronicle.Kind.BETRAYAL, "💔 " + breaker.getName() + " rompt son " + t.getType().getLabel().toLowerCase(Locale.ROOT) + " avec " + otherName + ".",
            breaker.getId(), other != null ? other.getId() : null);
         if (actor != null) {
            this.plugin.getAchievements().grant(actor.getUniqueId(), fr.esicraft.life.Achievement.TRAITOR);
         }
      } else {
         ChatUtil.broadcast("&e👑 " + breaker.getName() + " libère son vassal &6" + otherName + "&e : la vassalité prend fin.");
         this.plugin.getSeasonManager().addHistory(fr.esicraft.life.Chronicle.Kind.TREATY, "👑 " + breaker.getName() + " libère son vassal " + otherName + ".", breaker.getId(),
            other != null ? other.getId() : null);
      }

      return null;
   }
}
