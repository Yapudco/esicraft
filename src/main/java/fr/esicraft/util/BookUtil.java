package fr.esicraft.util;

import net.kyori.adventure.text.Component;
import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.BookMeta;

public final class BookUtil {
   private BookUtil() {
   }

   public static ItemStack createGuideBook() {
      ItemStack book = new ItemStack(Material.WRITTEN_BOOK);
      BookMeta meta = (BookMeta)book.getItemMeta();
      if (meta != null) {
         meta.title(ChatUtil.component("&4Guide Esicraft"));
         meta.author(ChatUtil.component("L'Ordre d'Esicraft"));
         meta.addPages(
            new Component[]{
               ChatUtil.component(
                  "&4&l=== ESICRAFT ===&r\n\n&0Guide officiel du\n&0serveur survie RPG\n&0et PvP de factions.\n\n&11. Métiers & Classes\n&42. Guildes & Claims\n&53. Spécialisations\n&24. Nexus & Pièges\n&65. Hordes & Raids\n&36. Économie & Âges\n\n&8Tournez la page ➔"
               )
            }
         );
         meta.addPages(
            new Component[]{
               ChatUtil.component(
                  "&4&lMÉTIERS (1/3)&r\n\n&8⛏ &lMineur (Nain) :&r\n&0• Pioches/Pelles dia\n&0• Haste 2 sous Y=0\n&0• +15% double minage\n&c- Pas d'armure fer\n\n&2\ud83c\udf3f &lSylvain (Elfe) :&r\n&0• Houes/Cultures\n&0• Aura Régen (4b)\n&0• Récolte clic-droit\n&c- -20% dégâts mêlée"
               )
            }
         );
         meta.addPages(
            new Component[]{
               ChatUtil.component(
                  "&4&lMÉTIERS (2/3)&r\n\n&4\ud83d\udd28 &lForgeron :&r\n&0• Armures/Épées fer\n&0• Armures dia (A2)\n&0• Résistance Feu\n&0• +35% lingots fondus\n&0• -50% coût enclume\n\n&3\ud83c\udff9 &lRôdeur :&r\n&0• Arcs/Arbalètes A1\n&0• Flèches de Poison\n&0• +25% tir distance\n&0• Vitesse 1 en forêt"
               )
            }
         );
         meta.addPages(
            new Component[]{
               ChatUtil.component(
                  "&4&lMÉTIERS (3/3)&r\n\n&5\ud83e\uddea &lArcaniste :&r\n&0• Enchanteur exclusif\n&0• Splash Potions\n&0• +30% gain XP\n&0• -20% dégâts magie\n\n&1⚙ &lIngénieur :&r\n&0• Redstone avancée\n&0• Poseur de TNT (A1)\n&0• -40% dégâts explos\n\n&8Choix : &1/race"
               )
            }
         );
         meta.addPages(
            new Component[]{
               ChatUtil.component(
                  "&4&lGUILDES & CLAIMS&r\n\n&0Fondez votre clan :\n&1/guild create <n> <t>\n\n&4Règles des Claims :&r\n&0• Contigus obligat.\n&0• Coffres protégés\n&0• 16 claims à l'Âge 1\n&0• 32 à l'Âge 2\n&0• 64 à l'Âge 3\n&0• 128 à l'Âge 4\n&0• Illimité à l'Âge 5"
               )
            }
         );
         meta.addPages(
            new Component[]{
               ChatUtil.component(
                  "&4&lBANQUE & RANGS&r\n\n&1• Banque commune :&r\n&1/guild bank <montant>\n&0Finance les claims,\n&0pièges et hordes.\n\n&4• Rangs & Ordres :&r\n&0• Chef (Fondateur)\n&0• Officier (Gestion)\n&0• Membre (Soldat)\n\n&1/guild promote <j>"
               )
            }
         );
         meta.addPages(
            new Component[]{
               ChatUtil.component(
                  "&4&lSPÉCIALISATIONS 1&r\n\n&0Menu : &1/guild spec\n\n&4⚔ Légion Guerrière :&r\n&0• Force 1 & Résist 1\n&0• -30% coût hordes\n\n&8⛏ Industrie Souter. :&r\n&0• Haste 2 sous Y=30\n&0• Minerais x1.5\n\n&2\ud83c\udf3e Enclave Champêtre :&r\n&0• +2 cœurs santé\n&0• Régen 1 & Saturation"
               )
            }
         );
         meta.addPages(
            new Component[]{
               ChatUtil.component(
                  "&4&lSPÉCIALISATIONS 2&r\n\n&3\ud83d\udcb0 Syndicat Marchand :&r\n&0• Villageois (A1-A3)\n&0• +20% primes monstres\n&c- 0 Arcaniste permis\n\n&5\ud83d\udd2e Conclave Arcanique :&r\n&0• Enchants Niveau 6 !\n&0• Rayon magique Nexus\n\n&2\ud83c\udff9 Rôdeur Solitaire :&r\n&0• Solo (Vitesse 1)\n&0• Netherite & Ench A3\n&c- 0 coffre en claim"
               )
            }
         );
         meta.addPages(
            new Component[]{
               ChatUtil.component(
                  "&4&lLE NEXUS DE BASE&r\n\n&0Cœur vital du clan :\n&1/guild nexus place\n\n&4• Sécurité (16 blocs) :&r\n&0Lave, eau & obsi\n&0strictement interdits\n&0dans le périmètre.\n\n&2• Soin du Nexus :&r\n&0Jetez 1 Pomme Dorée\n&0pour rendre +500 PV."
               )
            }
         );
         meta.addPages(
            new Component[]{
               ChatUtil.component(
                  "&4&lPIÈGES DE NEXUS&r\n\n&0Menu : &1/nexus traps\n\n&4\ud83d\udd25 Brasier Infernal\n&0Enflamme les rivaux.\n\n&8\ud83d\udd78️ Toile d'Entrave\n&0Ralentit & aveugle.\n\n&3⚡ Foudre de Riposte\n&0+15 dégâts au frappeur\n\n&2\ud83e\uddea Brume Toxique\n&8\ud83e\udd16 Golem Gardien"
               )
            }
         );
         meta.addPages(
            new Component[]{
               ChatUtil.component(
                  "&4&lHORDES NOIRES&r\n\n&0Menu : &1/ah &0(crâne)\n\n&0Assiégez les rivaux !\n&0• Payé par la banque\n&0• Hordes sur-mesure\n&0• Sapeurs briseurs\n&0• Anti-lave/feu\n\n&4⭐ Hégémonie :&r\n&0Hordes à -50% contre\n&0les clans trop riches."
               )
            }
         );
         meta.addPages(
            new Component[]{
               ChatUtil.component(
                  "&4&lANNEXION & GUERRE&r\n\n&0Si le Nexus ennemi\n&0est anéanti en raid :\n\n&4• Territoires :&r\n&0Tous annexés par le\n&0clan vainqueur !\n\n&6• Trésor de Guerre :&r\n&0La banque adverse est\n&0100% transférée !\n\n&0Les vaincus peuvent\n&0rejoindre le vainqueur"
               )
            }
         );
         meta.addPages(
            new Component[]{
               ChatUtil.component(
                  "&4&lÉCONOMIE & PRIMES&r\n\n&2• Monstres éliminés :&r\n&0Rapporte des pièces\n&0selon le danger.\n\n&4• Prime de Kill PvP :&r\n&0Tuer un rival donne :\n&6+50 pièces &0et\n&6+10% de sa fortune &0!\n\n&1• Hôtel des Ventes :&r\n&1/ah &0ou &1/ah sell <p>"
               )
            }
         );
         meta.addPages(
            new Component[]{
               ChatUtil.component(
                  "&4&lÂGES DU SERVEUR 1&r\n\n&0Menu : &1/age\n\n&81. Rustique :&r &0Départ\n&72. Métallurgie :&r\n&0• Accès au Nether \ud83d\udd25\n&0• Outils fer pour tous\n\n&53. Alchimie :&r\n&0• Accès à l'End \ud83c\udf0c\n&0• Outils dia pour tous\n&0• Potions de base"
               )
            }
         );
         meta.addPages(
            new Component[]{
               ChatUtil.component(
                  "&4&lÂGES DU SERVEUR 2&r\n\n&34. Guerre Totale :&r\n&0• Armures diamant\n&0• Enchants universels\n&0• Commerce villageois\n&0• TNT & Sièges\n\n&45. Mythique & Apex :&r\n&0• Netherite universelle\n&0• Claims illimités\n\n&66. Suprématie (Fin)"
               )
            }
         );
         meta.addPages(
            new Component[]{
               ChatUtil.component(
                  "&4&lÉCHELLE DES DONS&r\n\n&0Quotas de base = 1j.\n\n&1• Membres 2 à 5 :&r\n&0+30% d'items par\n&0joueur supplémentaire.\n\n&4• Membres 6+ :&r\n&0+50% par joueur sup.\n\n&6\ud83d\udc51 Exception Âge 6 :&r\n&0Quotas de reliques\n&0100% fixes !"
               )
            }
         );
         meta.addPages(
            new Component[]{
               ChatUtil.component(
                  "&4&lCOMMANDES CLÉS&r\n\n&1/race &0: Choisir métier\n&1/guild &0: Menu de clan\n&1/guild claim &0: Territ.\n&1/guild spec &0: Bonus\n&1/guild bank &0: Coffre\n&1/nexus traps &0: Pièges\n&1/age &0: Progrès Âges\n&1/ah &0: Marché & Hordes\n&1/money &0: Votre solde\n&1/baltop &0: Richesse\n&1/hub &0: Retour au Hub"
               )
            }
         );
         book.setItemMeta(meta);
      }

      return book;
   }
}
