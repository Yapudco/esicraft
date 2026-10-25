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

   private static final String[] INDEX_EN = {
      "Getting started", "Jobs", "Guilds & land", "Specializations", "Nexus & traps", "Raids", "The Ages", "Money", "Treaties", "Trains", "Events", "Commands"
   };

   private static Component indexPage(boolean english) {
      Component page = ChatUtil.component("&4&l== ESICRAFT ==&r\n");
      for (int idx = 0; idx < INDEX.length; idx++) {
         String[] entry = INDEX[idx];
         int target = Integer.parseInt(entry[2]);
         Component line = ChatUtil.component(entry[1] + "› " + (english ? INDEX_EN[idx] : entry[0]) + "\n")
            .clickEvent(ClickEvent.changePage(target))
            .hoverEvent(HoverEvent.showText(ChatUtil.component((english ? "&7Go to page " : "&7Aller à la page ") + target)));
         page = page.append(line);
      }
      return page;
   }

   private static List<String> pages(boolean english) {
      if (english) {
         return pagesEn();
      }

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
      pages.add(p("&4&lCOMMANDES (2/2)&r", "&1/traite &1/station", "&1/rail", "&1/money &1/baltop", "&1/pay <joueur> <$>", "&1/classement", "&1/histoire", "&1/hud", "&1/hub", "&1/guide", "&1/english"));
      pages.add(p("&4&lCOMMANDES (3/3)&r", "&1/contrat&0 : contrats", "&0du jour et de guilde", "&1/aide&0 : appeler ses", "&0alliés pendant un raid", "&1/deguisement <guilde>", "&0: espionner (750 p.)", "&1/climat&0 : saison", "&0naturelle", "&1/journal&0 : journal", "&0de guerre", "&1/succes&0 : vos succès"));
      // 31 : conseils
      pages.add(p("&4&lCONSEILS&r", "&0• Un raid exige un", "&0  défenseur en ligne", "&0  chez la cible : reste", "&0  connecté pour te", "&0  défendre.", "&0• Soigne ton Nexus", "&0  avant une attaque.", "&0• Un traité protège", "&0  des raids.", "&0• Trop riche = cible.", "&2Bon jeu !"));
      return pages;
   }


   private static List<String> pagesEn() {
      List<String> pages = new ArrayList<>();
      pages.add(p("&4&lGETTING STARTED (1/2)&r", "&01. &1/race&0 : pick", "&0your job. Without", "&0one you cannot break", "&0any block.", "&02. Join a guild", "&0(&1/guild join <name>&0)", "&0or create yours:", "&1/guild create", "&1<name> <tag>", "&0(500 coins)."));
      pages.add(p("&4&lGETTING STARTED (2/2)&r", "&03. &1/guild claim&0,", "&0then &1/guild nexus", "&1place&0.", "&04. Earn coins:", "&0monsters, &1/contrat&0,", "&1/ah&0.", "&05. Donate resources", "&0in &1/age&0 to advance.", "&8/hud : dashboard", "&8/guide : this book"));
      pages.add(p("&4&lJOBS (1/3)&r", "&8&lDwarf&0 (miner)", "&0Haste II + night vision", "&0below layer 0, 15%", "&0double ore.", "&c-&0 Slow in sunlight", "&2&lSylvan&0 (farmer)", "&0Regen I (self + guild", "&0within 4 blocks), right", "&0click harvest.", "&c-&0 -20% melee damage"));
      pages.add(p("&4&lJOBS (2/3)&r", "&4&lBlacksmith", "&0Only one to forge", "&0weapons, armor, anvils.", "&0Fire resistant, 35%", "&0double ingots, anvil", "&0half price.", "&c-&0 Hunger +25%", "&3&lRanger&0 (archer)", "&0Poison arrows, +25%", "&0ranged, fast in", "&0forest/plains.", "&c-&0 Slow in diamond chest"));
      pages.add(p("&4&lJOBS (3/3)&r", "&5&lArcanist&0 (mage)", "&0Enchanting and potions.", "&0+30% XP, potions +25%", "&0longer, -20% magic", "&0damage.", "&c-&0 +15% melee damage", "&1&lEngineer", "&0Redstone, pistons, TNT.", "&0-40% explosion damage.", "&c-&0 -20% melee damage"));
      pages.add(p("&4&lCHANGING JOB&r", "&1/race&0 opens the menu.", "&0The first pick + 3", "&0changes are free,", "&0then 10,000 coins.", "&024 h between changes.", "&4Exclusivities&0: an item", "&0reserved to one job", "&0becomes free later", "&0(iron Age 2, diamond", "&0Age 3, Netherite Age 5)."));
      pages.add(p("&4&lGUILDS&r", "&0Max 10 players, one", "&0guild each.", "&6Leader&0: everything,", "&0names officers, &1/guild", "&1transfer&0, &1disband&0.", "&6Officer&0: invites,", "&0kicks, claims, Nexus,", "&0traps, raids, treaties,", "&0stations.", "&6Member&0: bank, gifts,", "&0defence."));
      pages.add(p("&4&lTERRITORY&r", "&1/guild claim&0 takes", "&0the chunk you stand in.", "&0• All chunks must", "&0touch each other", "&0• 100 coins, then +25", "&0per chunk", "&0• Max: 16 (Age 1), 32,", "&064, 128, then unlimited", "&0(Age 5)", "&1/guild unclaim", "&1/guild claim show"));
      pages.add(p("&4&lPROTECTION&r", "&0On another clan's land:", "&0• no breaking/placing", "&0• chests, furnaces,", "&0  shulkers locked", "&0• TNT, pearls, pistons,", "&0  outside lava/water", "&0  forbidden", "&0Protections drop during", "&0a raid, and 30 min", "&0after the Nexus falls."));
      pages.add(p("&4&lSPECIALIZATIONS&r", "&0Leader: &1/guild spec", "&c&lWarrior Legion", "&0Strength I + Resist. I,", "&0raids -30%.", "&0At least 50% Ranger/", "&0Blacksmith.", "&8&lUnderground Industry", "&0Haste II below Y=30,", "&0ores x1.5, traps -50%.", "&0Max 1 Ranger."));
      pages.add(p("&4&lSPECIALIZATIONS&r", "&2&lPastoral Enclave", "&0+2 hearts, Regen I,", "&0harvests x2. No", "&0Engineer.", "&6&lMerchant Syndicate", "&0Villagers before Age 4,", "&0bounties +20%, no AH", "&0tax. No Arcanist.", "&0Nexus -20% health."));
      pages.add(p("&4&lSPECIALIZATIONS&r", "&5&lArcane Conclave", "&0Nexus +25%, boosted", "&0enchants (IV becomes V),", "&0magic Nexus beam.", "&0Land +50% price.", "&3&lLone Ranger", "&0Alone, Speed I,", "&0Netherite from Age 3.", "&0No chests in claims.", "&8Changing spec: only when", "&8a new player arrives."));
      pages.add(p("&4&lTHE NEXUS&r", "&0The guild's heart:", "&1/guild nexus place", "&0(only once).", "&4If it falls, you lose", "&4everything.", "&016-block zone: no water,", "&0lava or obsidian.", "&2Heal&0: throw a golden", "&0apple on it (+500 HP,", "&0+2500 if enchanted),", "&030 s between heals."));
      pages.add(p("&4&lTRAPS&r &1/nexus traps", "&0Paid by the bank,", "&0used up at the end of", "&0the next raid.", "&0They target intruders,", "&0never your allies.", "&cBlaze&0 250: fire", "&7Web&0 200: slowness", "&eLightning&0 350: 4 hearts", "&2Fumes&0 300: poison", "&8Golem&0 500: guardian"));
      pages.add(p("&4&lRAIDS (1/3)&r", "&0Pay for a monster", "&0horde to destroy a", "&0rival's Nexus.", "&01. &1/ah&0, Wither skull", "&02. Ready-made or custom", "&0horde (60 max)", "&03. Pick the target", "&0The bank pays.", "&0Options: sappers,", "&0fireproofing."));
      pages.add(p("&4&lRAIDS (2/3)&r", "&0• Leader or officer", "&0• A defender must be", "&0  online on their land", "&0• Target protected 30", "&0  min after a raid", "&0• No raid against a", "&0  treaty partner", "&0• Disbanding blocked", "&0  10 min after a raid"));
      pages.add(p("&4&lRAIDS (3/3)&r", "&0Nexus destroyed:", "&0• land annexed", "&0• bank transferred", "&0• the losers choose:", "&1/guild annex join&0 or", "&1decline", "&6&lHegemony&0: a guild", "&0too rich = raids -50%,", "&0protection of only", "&02 min."));
      pages.add(p("&4&lTHE AGES (1/3)&r", "&1/age&0: members donate", "&0resources.", "&01 Rustic: start", "&02 Metallurgy: Nether,", "&0  iron for all", "&03 Alchemy: End,", "&0  diamond, iron armor", "&04 Total War:", "&0  diamond armor,", "&0  villagers, TNT"));
      pages.add(p("&4&lTHE AGES (2/3)&r", "&04: player bonuses", "&0Speed I, +10% damage,", "&0+25% XP.", "&05 Mythic: Netherite", "&0and beacons for all,", "&0unlimited land,", "&0Haste, Resist., +50% XP.", "&06 Supremacy: dragon", "&0egg + Netherite block", "&0per member, chat title."));
      pages.add(p("&4&lDONATIONS&r", "&0The amounts in the", "&1/age&0 menu depend on", "&0the number of members:", "&0x1.3 for 2 players,", "&0x2.2 for 5, x4.7 for 10.", "&0The maximum members", "&0during the Age count:", "&0kicking before donating", "&0is pointless.", "&0Age 6: fixed amounts."));
      pages.add(p("&4&lEARNING COINS&r", "&0• Monsters: 1.5 to", "&0  750 (Ender Dragon)", "&0• &1/contrat&0: 3 a day,", "&0  +400 if all 3", "&0• &1/ah sell <price>", "&0  (10 listings max)", "&0• Events, world boss", "&8Repetitive farms pay", "&8less."));
      pages.add(p("&4&lPVP & SPENDING&r", "&0Killing a player from", "&0another clan: 100 coins", "&0+ 1% of their wealth.", "&0No bounty within 2 min", "&0of a previous fight.", "&0Spend: bank", "&0(&1/guild bank&0), &1/pay", "&0trains, &1/ah&0, changing", "&0job.", "&1/money&0, &1/baltop"));
      pages.add(p("&4&lTREATIES&r &1/traite", "&0Offer valid 10 min.", "&6Alliance&0: no raids,", "&0free trains, no damage", "&0between allies.", "&6Pact&0: 7 days, no raid.", "&6Vassalage&0: alliance +", "&05% of the bank per day", "&0to the suzerain.", "&4Breaking = betrayal:", "&0indemnity (500 max)."));
      pages.add(p("&4&lTRAINS&r", "&0Sign &1[Gare]&0 (line 1)", "&0+ name (line 2) near a", "&0rail. Age 2, 500 coins,", "&06 stations max.", "&1/station&0 near a", "&0station: pick where to go.", "&0Continuous rails needed.", "&0Price: 1 coin + tolls", "&0of guilds crossed.", "&1/rail&0: toll, relations", "&0Normal/Allied/Banned."));
      pages.add(p("&4&lEVENTS&r", "&0One every 1 to 2 days:", "&cBlood Moon&0: more", "&0monsters, bounties x2", "&7Meteors&0: ore falling", "&0from the sky", "&5Black Market&0: rare", "&0items for diamonds", "&4World Boss&0: Lord of", "&0Ashes, 3000 coins", "&0shared"));
      pages.add(p("&4&lSEASONS & SITE&r", "&1/classement&0: each", "&0month, the best guild", "&0wins 5000 + 500 per", "&0member and a crown.", "&1/histoire&0: great", "&0moments.", "&1/hud&0: dashboard.", "&0Public website:", "&1http://<server ip>", "&0ranking, players."));
      pages.add(p("&4&lCOMMANDS (1/2)&r", "&1/race &1/guild &1/age", "&1/guild claim", "&1/guild spec", "&1/guild bank <$>", "&1/guild nexus place", "&1/nexus traps", "&1/guild annex", "&1/ah &0(+ raids)", "&1/ah sell <price>", "&1/contrat"));
      pages.add(p("&4&lCOMMANDS (2/2)&r", "&1/traite &1/station", "&1/rail", "&1/money &1/baltop", "&1/pay <player> <$>", "&1/classement", "&1/histoire", "&1/hud", "&1/hub", "&1/guide", "&1/english &1/francais"));
      pages.add(p("&4&lCOMMANDS (3/3)&r", "&1/contrat&0: daily and", "&0guild contracts", "&1/aide&0: call your allies", "&0during a raid", "&1/deguisement <guild>", "&0: spy (750 coins)", "&1/climat&0: natural", "&0season", "&1/journal&0: war", "&0journal", "&1/succes&0: achievements"));
      pages.add(p("&4&lTIPS&r", "&0• A raid needs a", "&0  defender online at the", "&0  target: stay connected", "&0  to defend yourself.", "&0• Heal your Nexus", "&0  before an attack.", "&0• A treaty protects", "&0  from raids.", "&0• Too rich = target.", "&2Have fun!"));
      return pages;
   }

   public static ItemStack createGuideBook() {
      return createGuideBook(false);
   }

   public static ItemStack createGuideBook(boolean english) {
      ItemStack book = new ItemStack(Material.WRITTEN_BOOK);
      BookMeta meta = (BookMeta)book.getItemMeta();
      if (meta != null) {
         meta.title(ChatUtil.component(english ? "&4Esicraft Guide" : "&4Guide Esicraft"));
         meta.author(ChatUtil.component("L'Ordre d'Esicraft"));
         meta.addPages(new Component[]{indexPage(english)});
         for (String page : pages(english)) {
            meta.addPages(new Component[]{ChatUtil.component(page)});
         }
         book.setItemMeta(meta);
      }
      return book;
   }
}
