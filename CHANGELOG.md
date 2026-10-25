# 📝 Historique des changements

Les changements récents du plugin, du plus récent au plus ancien. Le guide du joueur est dans le [README](README.md).

## 25/10/2026 (fin de nuit)

### Correctif Enclave Druidique
- Les bêtes de garde attaquent enfin : elles étaient créées « en colère » mais sans cible, donc inoffensives. Elles ciblent maintenant l'assaillant dès qu'elles apparaissent, et les bêtes déjà présentes se retournent contre lui à chaque coup porté au Nexus. Elles choisissent aussi seules l'ennemi le plus proche.

### Combat PvP
- Frapper un joueur d'une autre guilde, ou être frappé, met « en combat » pendant 15 secondes (`combat.seconds`, `combat.enabled`) avec compte à rebours dans la barre d'action.
- `/hub` est bloqué pendant le combat.
- Se déconnecter en combat est puni : le joueur meurt sur place et lâche ses objets, avec une annonce au serveur. Un kick ou un arrêt du serveur n'est pas puni.

### Nexus
- Le golem gardien et les pièges du Nexus (flammes, toile, poison, rayon du Conclave) épargnent désormais aussi les joueurs d'une guilde liée par un pacte de non-agression (comme pour l'alliance et la vassalité). Frapper le Nexus rompt le pacte avant que les défenses ne réagissent.

### Rivalités
- Une rivalité s'estompe avec le temps : raids, trahisons et morts perdent la moitié de leur poids tous les 7 jours (`rivalry.half-life-days`).
- Signer un traité (alliance, pacte, vassalité) efface la rivalité passée entre les deux guildes.
- Les morts PvP sont désormais datées ; celles d'avant cette mise à jour ne comptent plus dans les rivalités.

### Rail
- Seules les guildes **Syndicat Marchand** peuvent prélever un péage sur le rail. Les autres guildes ne font plus payer (avant, toutes appliquaient 2 pièces par défaut) et ne peuvent plus modifier le montant dans `/rail`.

### Pacte de non-agression
- Frapper un joueur de la guilde sous pacte (coup, flèche) ou son Nexus rompt désormais le pacte automatiquement : trahison (indemnité, préavis de 30 minutes, annonce, succès) et **rivalité** déclarée entre les deux guildes.

### Correctifs
- **Nexus** : l'hologramme des PV réapparaît après un redémarrage du serveur ou une reconnexion (il n'était recréé qu'au premier dégât ou soin).
- **Nexus** : après un redémarrage, le Nexus est de nouveau retrouvé à sa position (l'index était vidé au démarrage).

## 25/10/2026 (nuit, suite)

### Nouvelles mécaniques
- **Déguisement** (`/deguisement <guilde>`) : 750 pièces, 5 minutes, toutes les 30 minutes. Faux nom et étiquette dans le tchat et la liste, nom masqué au-dessus de la tête, pièges et golem de la guilde imitée sans effet. Infliger des dégâts ou frapper le Nexus démasque.
- **Saisons naturelles** (`/climat`) : printemps, été, automne, hiver, deux semaines chacune. Effets sur la pousse des cultures, l'expérience, les récoltes et les primes.
- **Bonus de diversité** : 3 à 6 métiers différents dans la guilde donnent des primes, des terres, des pièges et des contrats plus avantageux.
- **Contrat de guilde** (dans `/contrat`) : trois objectifs par jour, chacun réservé à un métier ; récompense versée à la banque.
- **Appel à l'aide** (`/aide`) : les alliés venus en renfort sont payés si la horde est repoussée.
- **Journal de guerre** (`/journal`) et **26 succès** (`/succes`), avec statistiques par joueur.

### Site web
- Nouvelles pages : journal de guerre filtrable, raids en direct, réseau diplomatique et carte des rivalités, musée des trophées et des succès (rares mis en avant), courbes d'évolution, frise des Âges, profil de chaque joueur, guide consultable avec recherche.
- Les pages se mettent à jour toutes seules (script local, autorisé par une politique de sécurité qui n'accepte que les scripts du site).
- Nouvelles API : `/api/journal.json`, `/api/raids.json`.


### Commande `/english`
- Traduit le jeu en anglais pour le joueur seul (messages, barre d'action, titres, menus, tableau de bord, livre `/guide`) ; `/francais` pour revenir. Choix mémorisé en base.
- Fonctionne avec un dictionnaire FR→EN (`lang/en.tsv`, ~1 300 fragments) : les parties dynamiques (noms, nombres) restent telles quelles.
- Pas encore traduits : barre de boss des événements, tchat, site web.

### Compatibilité Paper 26.2
- Le plugin cible maintenant Paper 26.2 et Java 25 (il visait Paper 1.20.4 / Java 17 et ne pouvait pas fonctionner sur un serveur 26.2).
- Constantes d'API renommées (effets de potion, attributs, particules, enchantements, wagonnet TNT).
- Un objet de `config.yml` qui n'existe plus dans cette version est signalé dans la console au lieu d'être ignoré en silence.

### Anti « lava cast » et inondations
- Lave et eau ne passent plus d'un territoire à un autre, y compris pendant un raid (avant, l'exception de raid laissait inonder la cible).
- Un distributeur ne peut plus verser un seau de lave ou d'eau sur le territoire d'une autre guilde.
- Impossible de vider un seau de lave à moins de 5 blocs d'un joueur hors guilde et hors traité.

### Livre `/guide` réécrit
- 30 pages alignées sur le jeu actuel : démarrage, 6 métiers, guildes et terres, 6 spécialisations, Nexus et pièges, raids, Âges, argent, traités, train, événements, saisons, site web, commandes.
- Sommaire cliquable (clic = saut au chapitre) ; le livre s'ouvre directement et tombe au sol si l'inventaire est plein.

## 25/10/2026 (soir)

### Pièges du Nexus rééquilibrés
- **Foudre** : 15 dégâts à *chaque* coup (un joueur mourait en 2 coups) → 4 cœurs, au plus un éclair toutes les 5 s par joueur.
- **Brasier** : 2 cœurs toutes les 2 s + brûlure de 5 s → 1 cœur toutes les 2 s + brûlure de 3 s.
- **Toile** : Lenteur III + Cécité permanentes → Lenteur II seule.
- **Brume** : Poison II → Poison I.
- Les pièges ne visent plus les alliés (traité), ni les joueurs en créatif ou spectateur.
- **Golem Gardien** : reconstruit 5 min après sa mort tant que le piège est armé, et supprimé quand le piège est consommé (il restait avant indéfiniment).
- La documentation indiquait que les pièges restaient armés indéfiniment : ils sont en fait consommés à la fin de la prochaine horde (corrigé dans les README).

### Événements
- Un événement tous les 1 à 2 jours (au lieu de 45 à 90 minutes). L'échéance est conservée en base : un redémarrage ne la remet plus à zéro.

## 25/10/2026

### Documentation
- README réécrit pour être plus simple à lire (français et [anglais](README.en.md)), avec une partie dédiée aux administrateurs.
- Quantités des Âges 3 à 5 corrigées (elles ne correspondaient plus à `config.yml`) et 6ᵉ spécialisation (Rôdeur Solitaire) documentée.
- Textes du menu `/guild spec` corrigés : le Syndicat Marchand n'a pas de taxe à l'Hôtel des Ventes (au lieu de « +25 % ») et le rayon du Conclave frappe toutes les 2 secondes.

### Corrections
- `/money` était réservé aux administrateurs : les joueurs peuvent maintenant voir leur solde (les sous-commandes restent réservées aux administrateurs).

### Site web (PR #6 et #7)
- Site public en lecture seule lancé avec le plugin (port 80 par défaut) : accueil, classement des guildes, fiche de guilde, joueurs, API JSON.
- Design moderne : thème clair/sombre, cartes arrondies, avatars colorés, médailles, responsive.
- Sécurité : pages pré-rendues, limite de requêtes par IP, file d'attente bornée, aucune coordonnée, joueurs en vanish masqués.

### Traités entre guildes (PR #5)
- Alliance, pacte de non-agression et vassalité (`/traite`), avec indemnité et préavis en cas de trahison.
- Pas de raid entre guildes liées, trains gratuits et pas de dégâts entre alliés (potions comprises).
- Correctifs de sécurité : rupture atomique (plus de double indemnité), annulation réservée aux officiers.
- Tableau de bord (`/hud`) optimisé : il n'est mis à jour que si son contenu change.

### Réseau ferroviaire (PR #4)
- Gares (`[Gare]`), voyage instantané le long des rails (`/station`), péages et relations entre guildes (`/rail`).

### Gameplay (PR #3)
- Contrats quotidiens, événements serveur (lune de sang, météorites, marché noir, boss mondial), saisons et mémorial, tableau de bord.

### Fonctionnalités manquantes et sécurité (PR #1 et #2)
- **Arcaniste** : +15 % de dégâts subis en mêlée (attaques directes uniquement).
- **Forgeron** : enclumes, meules et hauts-fourneaux ne sont fabricables que par les Forgerons jusqu'à l'Âge 3 (leur usage reste libre).
- **Enclave Champêtre** : reproduction animale ×3 ; les armures en Netherite sont retirées automatiquement.
- **Industrie Souterraine** : potions de niveau II refusées.
- **Siège de Pillards** : 1 Ravageur sur 6 et 1 Vindicateur sur 3.
- **Syndicat Marchand** : les 10 % de dividende sont prélevés sur la prime du joueur (plus de création d'argent).
- Protections de claim complétées (pistons, fluides, feu, chorus, entités), dupe des bonus de minage corrigé, Nexus impossible à reposer, quotas d'Âge basés sur le maximum de membres, limite de 10 membres, invitations qui expirent.
- Cooldowns de raid et pièges sauvegardés en base (survivent aux redémarrages).
- Sauvegarde périodique en une seule transaction SQLite.
