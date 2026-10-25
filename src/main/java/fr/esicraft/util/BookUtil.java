package fr.esicraft.util;

import java.util.ArrayList;
import java.util.List;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.event.ClickEvent;
import net.kyori.adventure.text.event.HoverEvent;
import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.BookMeta;

/** Livre de règles remis par /guide : sommaire cliquable puis un chapitre par grande fonctionnalité. */
public final class BookUtil {
   private BookUtil() {
   }

   /** Entrées du sommaire : titre, couleur, numéro de page (à garder synchronisé avec {@link #pages()}). */
   private static final String[][] INDEX = {
      {"Démarrer", "&2", "2"},
      {"Les métiers", "&1", "4"},
      {"Guildes & terres", "&4", "8"},
      {"Spécialisations", "&5", "11"},
      {"Nexus & pièges", "&3", "14"},
      {"Raids", "&c", "16"},
      {"Les Âges", "&6", "19"},
      {"Argent", "&2", "22"},
      {"Traités", "&1", "24"},
      {"Train", "&8", "25"},
      {"Événements", "&5", "26"},
      {"Commandes", "&4", "28"}
   };

   private static String p(String... lines) {
      return String.join("\n", lines);
   }

   private static Component indexPage() {
      Component page = ChatUtil.component("&4&l== ESICRAFT ==&r\n");
      for (String[] entry : INDEX) {
         int target = Integer.parseInt(entry[2]);
         Component line = ChatUtil.component(entry[1] + "› " + entry[0] + "\n")
            .clickEvent(ClickEvent.changePage(target))
            .hoverEvent(HoverEvent.showText(ChatUtil.component("&7Aller à la page " + target)));
         page = page.append(line);
      }
      return page;
   }

   private static List<String> pages() {
      List<String> pages = new ArrayList<>();
      // 2-3 : démarrage
      pages.add(p("&4&lDÉMARRER (1/2)&r", "&01. &1/race&0 : choisis", "&0ton métier. Sans", "&0métier, tu ne peux", "&0casser aucun bloc.", "&02. Rejoins une", "&0guilde (&1/guild join", "&1<nom>&0) ou crée la", "&0tienne : &1/guild", "&1create <nom> <tag>", "&0(500 pièces)."));
      pages.add(p("&4&lDÉMARRER (2/2)&r", "&03. &1/guild claim&0,", "&0puis &1/guild nexus", "&1place&0.", "&04. Gagne des pièces :", "&0monstres, &1/contrat&0,", "&1/ah&0.", "&05. Donne des", "&0ressources dans", "&1/age&0 pour évoluer.", "&8/hud : tableau de bord", "&8/guide : ce livre"));
      // 4-7 : métiers
      pages.add(p("&4&lMÉTIERS (1/3)&r", "&8&lNain&0 (mineur)", "&0Hâte II + vision de", "&0nuit sous la couche 0,", "&015% de minerai x2.", "&c-&0 Lent en plein soleil", "&2&lSylvain&0 (fermier)", "&0Régén. I (lui + guilde", "&0à 4 blocs), récolte au", "&0clic droit.", "&c-&0 -20% dégâts mêlée"));
      pages.add(p("&4&lMÉTIERS (2/3)&r", "&4&lForgeron", "&0Seul à forger armes,", "&0armures, enclumes.", "&0Résiste au feu, 35%", "&0de lingots x2, enclume", "&0moitié prix.", "&c-&0 Faim +25%", "&3&lRôdeur&0 (archer)", "&0Flèches empoisonnées,", "&0+25% à distance, vite", "&0en forêt/plaine.", "&c-&0 Lent en plastron dia"));
      pages.add(p("&4&lMÉTIERS (3/3)&r", "&5&lArcaniste&0 (mage)", "&0Enchantements et", "&0potions. +30% XP,", "&0potions +25% durée,", "&0-20% dégâts magie.", "&c-&0 +15% dégâts mêlée", "&1&lIngénieur", "&0Redstone, pistons et", "&0TNT. -40% dégâts", "&0d'explosion.", "&c-&0 -20% dégâts mêlée"));
      pages.add(p("&4&lCHANGER DE MÉTIER&r", "&1/race&0 ouvre le menu.", "&0Le 1er choix + 3", "&0changements : gratuits,", "&0puis 10 000 pièces.", "&024 h entre deux.", "&4Exclusivités&0 : un objet", "&0réservé à un métier", "&0devient libre plus", "&0tard (fer Âge 2,", "&0diamant Âge 3,", "&0Netherite Âge 5)."));
      // 8-10 : guildes
      pages.add(p("&4&lGUILDES&r", "&0Max 10 joueurs,", "&0une seule guilde.", "&6Chef&0 : tout, nomme les", "&0officiers, &1/guild", "&1transfer&0, &1disband&0.", "&6Officier&0 : invite,", "&0expulse, claims, Nexus,", "&0pièges, raids, traités,", "&0gares.", "&6Membre&0 : banque, dons,", "&0défense."));
      pages.add(p("&4&lTERRITOIRE&r", "&1/guild claim&0 prend", "&0le chunk où tu es.", "&0• Tous les chunks", "&0doivent se toucher", "&0• 100 pièces, puis", "&0+25 par chunk", "&0• Max : 16 (Âge 1),", "&032, 64, 128, puis", "&0illimité (Âge 5)", "&1/guild unclaim", "&1/guild claim show"));
      pages.add(p("&4&lPROTECTIONS&r", "&0Chez un autre clan :", "&0• ni casser ni poser", "&0• coffres, fours,", "&0  shulkers fermés", "&0• TNT, perles, pistons,", "&0  lave/eau/feu extérieurs", "&0  interdits", "&0Les protections", "&0sautent pendant un", "&0raid, et 30 min après", "&0la chute du Nexus."));
      // 11-13 : spécialisations
      pages.add(p("&4&lSPÉCIALISATIONS&r", "&0Chef : &1/guild spec", "&c&lLégion Guerrière", "&0Force I + Résist. I,", "&0raids -30%.", "&0Mini 50% Rôdeur/", "&0Forgeron.", "&8&lIndustrie Souterr.", "&0Hâte II sous Y=30,", "&0minerais x1,5, pièges", "&0-50%. Max 1 Rôdeur."));
      pages.add(p("&4&lSPÉCIALISATIONS&r", "&2&lEnclave Champêtre", "&0+2 cœurs, Régén. I,", "&0récoltes x2. Pas", "&0d'Ingénieur.", "&6&lSyndicat Marchand", "&0Villageois avant l'Âge", "&04, primes +20%, pas", "&0de taxe à l'AH.", "&0Pas d'Arcaniste.", "&0Nexus -20% de vie."));
      pages.add(p("&4&lSPÉCIALISATIONS&r", "&5&lConclave Arcanique", "&0Nexus +25%, enchants", "&0boostés (IV devient V),", "&0rayon magique du Nexus.", "&0Terres +50% prix.", "&3&lRôdeur Solitaire", "&0Seul, Vitesse I,", "&0Netherite dès l'Âge 3.", "&0Aucun coffre en claim.", "&8Changer de spé : seulement", "&8à l'arrivée d'un nouveau."));
      // 14-15 : Nexus
      pages.add(p("&4&lLE NEXUS&r", "&0Cœur de la guilde :", "&1/guild nexus place", "&0(une seule fois).", "&4S'il tombe, tu perds", "&4tout.", "&0Zone de 16 blocs : ni", "&0eau, lave ni obsidienne.", "&2Soin&0 : jette une pomme", "&0dorée dessus (+500 PV,", "&0+2500 si enchantée),", "&030 s entre deux soins."));
      pages.add(p("&4&lPIÈGES&r &1/nexus traps", "&0Payés par la banque,", "&0consommés à la fin de", "&0la prochaine horde.", "&0Ils visent les intrus,", "&0jamais tes alliés.", "&cBrasier&0 250 : feu", "&7Toile&0 200 : lenteur", "&eFoudre&0 350 : 4 cœurs", "&2Brume&0 300 : poison", "&8Golem&0 500 : gardien"));
      // 16-18 : raids
      pages.add(p("&4&lRAIDS (1/3)&r", "&0Paye une horde de", "&0monstres pour détruire", "&0le Nexus d'un rival.", "&01. &1/ah&0, crâne de", "&0Wither", "&02. Horde prête ou", "&0sur mesure (60 max)", "&03. Choisis la cible", "&0La banque paie.", "&0Options : sapeurs,", "&0ignifugation."));
      pages.add(p("&4&lRAIDS (2/3)&r", "&0• Chef ou officier", "&0• Un défenseur doit", "&0  être en ligne sur", "&0  ses terres", "&0• Cible protégée 30", "&0  min après un raid", "&0• Pas de raid contre", "&0  un traité", "&0• Dissolution bloquée", "&0  10 min après un raid"));
      pages.add(p("&4&lRAIDS (3/3)&r", "&0Nexus détruit :", "&0• terres annexées", "&0• banque transférée", "&0• les perdants", "&0  choisissent :", "&1/guild annex join&0 ou", "&1decline", "&6&lHégémonie&0 : guilde", "&0trop riche = raids", "&0-50%, protection", "&0de 2 min seulement."));
      // 19-21 : Âges
      pages.add(p("&4&lLES ÂGES (1/3)&r", "&1/age&0 : les membres", "&0donnent des ressources.", "&01 Rustique : départ", "&02 Métallurgie : Nether,", "&0  fer pour tous", "&03 Alchimie : End,", "&0  diamant, fer armure", "&04 Guerre Totale :", "&0  armures diamant,", "&0  villageois, TNT"));
      pages.add(p("&4&lLES ÂGES (2/3)&r", "&04 : bonus joueurs", "&0Vitesse I, +10% dégâts,", "&0+25% XP.", "&05 Mythique : Netherite", "&0et balises pour tous,", "&0terres illimitées,", "&0Hâte, Résist., +50% XP.", "&06 Suprématie : œuf de", "&0dragon + bloc de", "&0Netherite par membre,", "&0titre au tchat."));
      pages.add(p("&4&lDONS&r", "&0Les quantités du menu", "&0&1/age&0 dépendent du", "&0nombre de membres :", "&0x1,3 à 2 joueurs,", "&0x2,2 à 5, x4,7 à 10.", "&0On compte le maximum", "&0de membres de l'Âge :", "&0expulser avant de", "&0donner ne sert à rien.", "&0Âge 6 : quantités fixes."));
      // 22-23 : argent
      pages.add(p("&4&lGAGNER DES PIÈCES&r", "&0• Monstres : de 1,5 à", "&0  750 (Ender Dragon)", "&0• &1/contrat&0 : 3 par", "&0  jour, +400 si les 3", "&0• &1/ah sell <prix>", "&0  (10 annonces max)", "&0• Événements, boss", "&0  mondial", "&8Les fermes répétitives", "&8rapportent moins."));
      pages.add(p("&4&lPVP & DÉPENSES&r", "&0Tuer un joueur d'un", "&0autre clan : 100 pièces", "&0+ 1% de sa fortune.", "&0Pas de prime sous 2", "&0min d'intervalle.", "&0Dépenser : banque", "&0(&1/guild bank&0), &1/pay", "&0trains, &1/ah&0, changer", "&0de métier.", "&1/money&0, &1/baltop"));
      // 24 : traités
      pages.add(p("&4&lTRAITÉS&r &1/traite", "&0Offre valable 10 min.", "&6Alliance&0 : pas de raid,", "&0trains gratuits, pas de", "&0dégâts entre alliés.", "&6Pacte&0 : 7 jours sans", "&0raid.", "&6Vassalité&0 : alliance +", "&05% de banque/jour au", "&0suzerain.", "&4Rompre = trahison :", "&0indemnité (500 max)."));
      // 25 : train
      pages.add(p("&4&lTRAIN&r", "&0Panneau &1[Gare]&0 (ligne", "&01) + nom (ligne 2) près", "&0d'un rail. Âge 2, 500", "&0pièces, 6 gares max.", "&1/station&0 près d'une", "&0gare : choisis où aller.", "&0Rails continus requis.", "&0Prix : 1 pièce + péages", "&0des guildes traversées.", "&1/rail&0 : péage, relations", "&0Normale/Alliée/Bannie."));
      // 26-27 : vie du serveur
      pages.add(p("&4&lÉVÉNEMENTS&r", "&0Un tous les 1 à 2 jours :", "&cLune de sang&0 : plus de", "&0monstres, primes x2", "&7Météorites&0 : minerais", "&0qui tombent du ciel", "&5Marché noir&0 : objets", "&0rares contre diamants", "&4Boss mondial&0 : Seigneur", "&0des Cendres, 3000", "&0pièces partagées"));
      pages.add(p("&4&lSAISONS & SITE&r", "&1/classement&0 : chaque", "&0mois, la meilleure", "&0guilde gagne 5000 +", "&0500 par membre et une", "&0couronne.", "&1/histoire&0 : grands", "&0moments.", "&1/hud&0 : tableau de bord.", "&0Site web public :", "&1http://<ip du serveur>", "&0classement, joueurs."));
      // 28-29 : commandes
      pages.add(p("&4&lCOMMANDES (1/2)&r", "&1/race &1/guild &1/age", "&1/guild claim", "&1/guild spec", "&1/guild bank <$>", "&1/guild nexus place", "&1/nexus traps", "&1/guild annex", "&1/ah &0(+ raids)", "&1/ah sell <prix>", "&1/contrat"));
      pages.add(p("&4&lCOMMANDES (2/2)&r", "&1/traite &1/station", "&1/rail", "&1/money &1/baltop", "&1/pay <joueur> <$>", "&1/classement", "&1/histoire", "&1/hud", "&1/hub", "&1/guide"));
      // 30 : conseils
      pages.add(p("&4&lCONSEILS&r", "&0• Un raid exige un", "&0  défenseur en ligne", "&0  chez la cible : reste", "&0  connecté pour te", "&0  défendre.", "&0• Soigne ton Nexus", "&0  avant une attaque.", "&0• Un traité protège", "&0  des raids.", "&0• Trop riche = cible.", "&2Bon jeu !"));
      return pages;
   }

   public static ItemStack createGuideBook() {
      ItemStack book = new ItemStack(Material.WRITTEN_BOOK);
      BookMeta meta = (BookMeta)book.getItemMeta();
      if (meta != null) {
         meta.title(ChatUtil.component("&4Guide Esicraft"));
         meta.author(ChatUtil.component("L'Ordre d'Esicraft"));
         meta.addPages(new Component[]{indexPage()});
         for (String page : pages()) {
            meta.addPages(new Component[]{ChatUtil.component(page)});
         }
         book.setItemMeta(meta);
      }
      return book;
   }
}
