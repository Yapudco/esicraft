# ⚔️ Esicraft — Guide du joueur

🇬🇧 [English version](README.en.md) · 📝 [Historique des changements](CHANGELOG.md)

**Esicraft** est un serveur Minecraft de survie entre amis, tourné vers la stratégie.

- Tu choisis un **métier** (mineur, forgeron, archer…).
- Tu rejoins une **guilde** (10 joueurs maximum) et vous revendiquez des **terres**.
- Au cœur de votre base, vous posez le **Nexus** : s'il tombe, vous perdez tout.
- Ensemble, vous franchissez **6 Âges** pour débloquer le fer, le Nether, l'End, la Netherite…
- Vous commerçez, signez des **traités**… ou **payez des hordes de monstres** pour détruire le Nexus d'en face.

> [!TIP]
> **Tu débutes ?** Va directement à la partie [1. Bien commencer](#1-bien-commencer) : elle te guide pas à pas, de ta première connexion à ta première semaine.

---

## Sommaire

1. [Bien commencer](#1-bien-commencer) — le tutoriel pas à pas
2. [Référence des commandes](#2-référence-des-commandes)
3. [Les métiers](#3-les-métiers)
4. [Les guildes et leur territoire](#4-les-guildes-et-leur-territoire)
5. [Les spécialisations de guilde](#5-les-spécialisations-de-guilde)
6. [Le Nexus](#6-le-nexus)
7. [Raids, sièges et combat entre joueurs](#7-raids-sièges-et-combat-entre-joueurs)
8. [Les Âges](#8-les-âges)
9. [L'argent, le marché et la bourse](#9-largent-le-marché-et-la-bourse)
10. [Diplomatie et transports](#10-diplomatie-et-transports)
11. [La vie du serveur](#11-la-vie-du-serveur)
12. [Le site web](#12-le-site-web)
13. [Pour les administrateurs](#13-pour-les-administrateurs)

---

## 1. Bien commencer

### 1.1 Les cinq notions à retenir

| Notion | En une phrase |
| :--- | :--- |
| 🎭 **Métier** | Ta classe. Elle te donne des talents, un défaut, et le droit de fabriquer certains objets. **Sans métier, tu ne peux casser aucun bloc.** |
| 🏛️ **Guilde** | Ton équipe (10 joueurs max). Elle possède une **banque**, des **terres** et un **Nexus**. |
| 💎 **Nexus** | Le cœur de ta base. Les ennemis veulent le détruire, toi tu veux le protéger. |
| ⏳ **Âge** | Le niveau de progression de la guilde (1 à 6). On le fait monter en **donnant des ressources** dans `/age`. |
| 🪙 **Pièces** | La monnaie du serveur. Tu en as **100** au départ. Elles servent à tout : terres, marché, trains, raids… |

### 1.2 Étape 1 : tes cinq premières minutes

À ta toute première connexion :

1. **`/guide`** : reçois le livre de règles (sommaire cliquable). Le lire rapporte 50 pièces au tutoriel.
2. **`/race`** : choisis ton **métier** (voir le tableau ci-dessous). Le premier choix est gratuit, et les 3 changements suivants aussi.
3. **`/hud`** : affiche ou masque le tableau de bord à droite de l'écran (argent, guilde, Âge…).
4. **`/tutoriel`** : une barre en haut de l'écran t'indique la quête en cours (voir [1.7](#17-le-tutoriel-intégré-9-quêtes)). `/tutoriel masquer` la cache.

> [!IMPORTANT]
> Tant que tu n'as pas choisi de métier, tout bloc que tu casses est annulé avec un message : **`/race` en premier !**

**Quel métier choisir ?**

| Si tu aimes… | Prends | Pourquoi |
| :--- | :--- | :--- |
| Miner, creuser | ⛏ **Nain** | Hâte II et vision nocturne sous la couche 0, 15 % de chances de doubler un minerai. Lent en plein soleil. |
| Cultiver, élever, soigner | 🌿 **Sylvain** | Régénération pour toi et ta guilde autour de toi, récolte et replantation d'un clic droit. Un peu fragile au corps à corps. |
| Fabriquer armes et armures | 🔨 **Forgeron** | Résistance au feu, lingots doublés au four, enclume moins chère. Il a faim plus vite. |
| Tirer à l'arc | 🏹 **Rôdeur** | Flèches empoisonnées, +25 % de dégâts à distance, rapide dehors. |
| Enchanter, brasser | 🧪 **Arcaniste** | +30 % d'XP, potions plus longues. Plus vulnérable au corps à corps. |
| Redstone et explosifs | ⚙ **Ingénieur** | -40 % de dégâts d'explosion, redstone et TNT. Faible au corps à corps. |

> [!TIP]
> Une guilde est **plus forte quand elle réunit des métiers différents** (jusqu'à 6, avec de vrais bonus : voir [Le bonus de diversité](#le-bonus-de-diversité)). Si tu rejoins des amis, demande-leur quels métiers manquent. Tous les détails sont dans [Les métiers](#3-les-métiers).

### 1.3 Étape 2 : gagner tes premières pièces

Tu pars avec **100 pièces**. Pour fonder une guilde, il en faut davantage (voir ci-dessous). Les sources les plus simples :

- **Tuer des monstres** : chaque monstre rapporte une prime (de 1,50 à 750 pièces).
- **`/contrat`** : 3 missions par jour, dont une liée à ton métier. **+400 pièces** si tu fais les trois.
- **Le tutoriel** : 925 pièces en tout, plus 400 de bonus final.
- **`/ressources`** : un monde minier remis à neuf chaque semaine, pour miner du minerai frais (ne laisse rien de précieux en bas, le PvP y est libre).
- **`/ah sell <prix>`** : vends l'objet que tu tiens en main à l'Hôtel des Ventes.

### 1.4 Étape 3 : rejoindre ou fonder une guilde

Deux chemins. **Le premier est le plus simple pour débuter.**

#### Option A : rejoindre une guilde (conseillé)

1. Demande à un **officier ou au chef** de taper `/guild invite <ton pseudo>`.
2. Tu as **5 minutes** pour accepter : `/guild join <nom de la guilde>`.
3. C'est tout : tu profites tout de suite de la banque, des terres et de la protection du Nexus de ta guilde.

#### Option B : fonder ta guilde

> [!WARNING]
> **Prépare-toi avant de taper la commande.** Dès que la guilde existe, tu as **1 heure** pour revendiquer un chunk **et** poser le Nexus. Sans Nexus au bout d'une heure, **la guilde est dissoute automatiquement**, et tes 500 pièces sont perdues.

Il te faut au minimum **600 pièces** : 500 pour la création, puis au moins 100 à déposer dans la banque pour payer le premier chunk.

| Étape | Commande | Détails |
| :--- | :--- | :--- |
| 1. Choisir l'endroit | : | Place-toi dans le chunk (carré de 16 × 16 blocs) où tu veux ta base, **dans l'Overworld**. Le Nexus ne sera posé qu'une seule fois. |
| 2. Créer | `/guild create <nom> <tag>` | Nom de 3 à 24 caractères (lettres, chiffres, `-`, `_`), tag de 2 à 6 caractères. Coût : **500 pièces**. |
| 3. Alimenter la banque | `/guild bank 100` | La banque de guilde paie les terres. Le premier chunk coûte 100 pièces. |
| 4. Revendiquer | `/guild claim` | Revendique le chunk où tu te trouves. |
| 5. Poser le Nexus | `/guild nexus place` | Dans ton territoire. Aucune eau, lave ni obsidienne dans les **16 blocs** autour, et pas plus de 3 blocs de roche au-dessus. **Impossible de le reposer ensuite.** |
| 6. Contrôler | `/guild claim show` | Affiche pendant une minute les bords des territoires (vert : le tien, rouge : les autres). |

Ensuite, tu peux inviter tes amis avec `/guild invite <joueur>`.

### 1.5 Étape 4 : ta première journée

Une fois dans une guilde, voici une routine efficace :

1. **Chaque jour** : fais tes `/contrat` (3 missions perso + 3 objectifs de guilde, un par métier).
2. **Alimente la banque** : `/guild bank <montant>`. Elle sert aux terres, aux hordes et aux sièges.
3. **Fais monter l'Âge** : ouvre `/age` pour voir les ressources demandées, puis donne-les (menu, ou objet en main + `/age deposit`). Les quantités s'adaptent au nombre de membres.
4. **Agrandis le territoire** : `/guild claim` sur un chunk **qui touche par un côté** (pas en diagonale) ceux que tu as déjà. Chaque chunk coûte 25 pièces de plus que le précédent.
5. **Commerce** : `/ah` ouvre l'Hôtel des Ventes (annonces, commandes d'achat).
6. **Descends miner** : `/ressources`, puis `/ressources quitter` pour remonter.

### 1.6 Étape 5 : ta première semaine

| Objectif | Comment |
| :--- | :--- |
| **Recruter** | Invite des amis aux métiers différents : 3 métiers = primes de monstres +10 %, jusqu'à 6 métiers = contrats ×1,5. |
| **Choisir une spécialisation** | Un officier ouvre `/guild spec` (combat, minage, nature, commerce, magie, solitaire, océan). Le premier choix est immédiat ; lis bien les contraintes de composition. |
| **Défendre le Nexus** | Les officiers débloquent des défenses permanentes dans `/nexus` (brasier, toile, foudre, golem…). Sacrifie des ressources avec `/nexus sacrifice`. Soigne-le avec des pommes dorées. |
| **Monter à l'Âge 2** | Débloque le **Nether**, les outils en fer pour tous, et la possibilité de construire des gares. |
| **Se protéger** | Signe un traité (`/traite`) avec une guilde voisine : alliance, pacte de non-agression ou vassalité. Les guildes liées ne peuvent pas se raider. |
| **Attention aux voisins** | Pas de territoire à moins de 1 chunk d'une guilde sans traité. Les guildes très riches sont des cibles (voir [Hégémonie économique](#les-guildes-trop-riches-sont-des-cibles)). |
| **Découvrir la vie du serveur** | Événements, boss mondial, tavernes, bourse, train : tout est en [partie 11](#11-la-vie-du-serveur). |

### 1.7 Le tutoriel intégré (9 quêtes)

`/tutoriel` te guide avec une barre en haut de l'écran. Chaque quête rapporte des pièces, **une seule fois par compte**. Une fois les 9 quêtes terminées, tu reçois **400 pièces de bonus** et un panier de provisions.

| # | Quête | Comment | Récompense |
| :-: | :--- | :--- | :-: |
| 1 | Lire le livre des règles | `/guide` | 50 |
| 2 | Choisir un métier | `/race` | 100 |
| 3 | Rejoindre ou fonder une guilde | `/guild join <guilde>` ou `/guild create <nom> <tag>` | 150 |
| 4 | Accomplir un premier contrat | `/contrat` | 150 |
| 5 | Déposer 100 pièces dans la banque | `/guild bank 100` | 100 |
| 6 | Faire un don pour l'Âge | `/age` | 100 |
| 7 | Entrer dans une taverne | zone neutre du Syndicat Marchand ou de l'Enclave Champêtre | 75 |
| 8 | Descendre au monde de ressources | `/ressources` | 100 |
| 9 | Faire un premier échange à l'Hôtel des Ventes | `/ah`, `/ah sell <prix>` ou une commande d'achat | 100 |

### 1.8 Les pièges à éviter

- 🚫 **Pas de métier = pas de minage.** `/race` d'abord.
- ⏱ **Fonder une guilde déclenche un compte à rebours d'1 heure** (claim + Nexus), sinon dissolution.
- 💎 **Le Nexus ne se repose pas** (sauf déplacement offert après un changement de spécialisation : `/nexus move`). S'il est détruit, la guilde perd son territoire, sa banque et ses traités.
- ⚔️ **Combat PvP = 15 secondes de blocage** : `/hub`, `/ah`, `/pay`, `/banque`, `/station`, `/home`, `/tpa`… sont refusés, les coffres sont fermés, et **se déconnecter en combat est puni de mort**.
- ⛏ **Ne laisse rien de précieux dans le monde de ressources** : le PvP y est libre et il est remis à neuf chaque semaine.
- 🔇 **Les messages privés sont désactivés** (`/msg`, `/tell`, `/w`, `/r`…). Parle dans le chat public ou celui de ta guilde.
- 🧱 **Chez les autres, tu ne casses ni ne poses rien**, et tu n'ouvres aucun coffre, four ou entonnoir.
- 🛠 **Certains équipements sont verrouillés par métier ou par Âge** : un objet « interdit » ne peut ni frapper ni miner, même trouvé ou acheté.
- 🪂 **L'élytre ne s'ouvre que depuis une piste d'aéroport** (au moins 32 × 3 blocs de béton noir) et à partir de l'Âge 4 : pour voyager, construis des routes, monte à cheval ou prends le train (qui n'est plus instantané).

### 1.9 Le rythme du serveur

| Quand | Quoi |
| :--- | :--- |
| Tous les jours | 3 contrats personnels + 3 objectifs de guilde · marchand itinérant chez une guilde tirée au sort (vers 14 h, 6 h de séjour) |
| Tous les 1 à 2 jours | Un événement : Lune de sang, météorites, marché noir ou boss mondial |
| **Lundi** | Vote du Tribunal des guildes (`/vote`) · remise à neuf du monde de ressources (5 h) |
| **Vendredi et samedi, 21 h** | Trêve : 1 heure sans PvP, sans raid ni attaque de Nexus |
| Toutes les 2 semaines | Changement de saison naturelle (`/climat`) |
| Chaque mois | Classement des guildes (`/classement`) et récompense de la championne |

### 1.10 Aide-mémoire : les 12 commandes du débutant

```text
/guide              Le livre de règles
/race               Choisir son métier
/tutoriel           Les 9 quêtes de départ
/contrat            Les missions du jour
/guild              Menu de la guilde (ou aide si tu n'en as pas)
/guild join <nom>   Accepter une invitation
/guild bank <n>     Déposer dans la banque de guilde
/guild claim        Revendiquer le chunk où tu es
/age                Progression des Âges et dons
/ah                 Hôtel des Ventes
/ressources         Monde minier
/hud                Tableau de bord
```

---

## 2. Référence des commandes

> [!NOTE]
> Les commandes admin sont en [partie 13](#13-pour-les-administrateurs). Les alias sont regroupés à la fin de cette partie. Une commande sans argument ouvre en général son menu.

### Compte, aide et interface

| Commande | À quoi elle sert |
| :--- | :--- |
| `/esicraft` | Présentation du plugin et liste des commandes |
| `/guide` | Livre de règles en jeu (30 pages, sommaire cliquable) |
| `/tutoriel [masquer\|afficher]` | Quêtes du tutoriel guidé |
| `/hud` | Afficher ou masquer le tableau de bord latéral |
| `/english [on\|off]` · `/francais` | Passer le jeu en anglais (menus, messages, tableau de bord, livre) · revenir au français |
| `/hub` | Retourner au hub (serveur proxy ; bloqué en combat) |
| `/webvote [confirmer\|refuser\|optout\|optin]` | Élections du site : lier son navigateur, ne plus être candidat |

### Métier

| Commande | À quoi elle sert |
| :--- | :--- |
| `/race` | Ouvrir le menu de choix du métier |
| `/race show [joueur]` | Annuaire des joueurs connectés (métier, guilde, Âge, argent), ou fiche d'un joueur |

### Guilde et territoire

| Commande | Qui | À quoi elle sert |
| :--- | :--- | :--- |
| `/guild` | Membre | Menu de la guilde (aide si tu n'en as pas) |
| `/guild create <nom> <tag>` | Tout joueur | Fonder une guilde (500 pièces) |
| `/guild invite <joueur>` | Officier | Inviter un joueur (valable 5 min) |
| `/guild join <nom>` | Tout joueur | Accepter une invitation |
| `/guild leave` | Membre | Quitter la guilde |
| `/guild kick <joueur>` | Officier | Expulser un membre |
| `/guild promote <joueur>` · `/guild demote <joueur>` | Chef | Nommer ou retirer un officier |
| `/guild transfer <joueur>` | Chef | Céder la guilde |
| `/guild disband` | Chef | Dissoudre la guilde |
| `/guild info [nom]` | Tous | Fiche d'une guilde (niveau de diversité inclus) |
| `/guild claim` | Officier | Revendiquer le chunk où tu te trouves |
| `/guild claim show` | Tous | Voir les bordures des territoires pendant 1 minute |
| `/guild unclaim` | Chef | Abandonner le chunk où tu te trouves |
| `/guild spec` | Officier | Choisir la spécialisation |
| `/guild bank` | Membre | Voir le solde de la banque |
| `/guild bank <montant>` | Membre | Y déposer des pièces |
| `/guild nexus` | Membre | État du Nexus |
| `/guild nexus place` | Officier | Poser le Nexus (une seule fois) |
| `/guild annex join` · `/guild annex decline` | Membre | Après une défaite : rejoindre le vainqueur ou partir |
| `/batiment [liste\|declarer <forge\|bibliotheque\|ecurie>\|retirer]` | Officier | Bâtiments de guilde et leurs bonus |
| `/taverne [declarer\|retirer]` | Officier | Déclarer une zone neutre, ou ouvrir le menu des jeux |

### Nexus

| Commande | Qui | À quoi elle sert |
| :--- | :--- | :--- |
| `/nexus` | Membre | Arbre technologique des défenses |
| `/nexus info` | Membre | Vie, niveau, XP et défenses débloquées |
| `/nexus sacrifice` | Membre | Sacrifier l'objet en main au Nexus (à moins de 10 blocs) |
| `/nexus move` | Officier | Déplacer le Nexus (droit offert après un changement de spécialisation) |
| `/nexus gamble` | Chef | Roulette russe du Nexus |

### Âges

| Commande | À quoi elle sert |
| :--- | :--- |
| `/age` | Menu des Âges : ressources demandées et dons |
| `/age deposit` | Donner l'objet tenu en main à la progression de la guilde |

### Argent et marché

| Commande | À quoi elle sert |
| :--- | :--- |
| `/money` | Voir son solde |
| `/baltop` | Classement des joueurs les plus riches |
| `/pay <joueur> <montant>` | Envoyer des pièces |
| `/ah` | Hôtel des Ventes (annonces, commandes d'achat, hordes mercenaires) |
| `/ah sell <prix>` | Vendre l'objet tenu en main (1 à 1 000 000 pièces, 10 annonces max) |
| `/ah commande [matériau] <quantité> <prix unitaire>` | Passer une commande d'achat (somme bloquée) |
| `/ah commandes` | Livrer, retirer ou annuler des commandes |
| `/bourse` · `/bourse liste` | Cours des actions de guilde |
| `/bourse ouvrir` · `/bourse fermer` | Ouvrir ou fermer le capital de sa guilde (chef, Âge 4) |
| `/bourse acheter <guilde> <parts>` · `/bourse vendre <guilde> <parts>` | Acheter ou revendre des parts |
| `/bourse info <guilde>` · `/bourse portefeuille` | Détail de la valeur d'une guilde · tes actions |
| `/banque` | Menu de la banque : prêts, dettes, coffres communs |
| `/banque pret <guilde> <montant> [taux]` | Proposer un prêt (officier) |
| `/banque accepter <guilde>` · `/banque refuser` | Répondre à une offre de prêt |
| `/banque dettes` · `/banque rembourser <guilde> [montant]` | Lister ses dettes · rembourser |
| `/banque pardonner <guilde>` | Effacer une dette (chef) |
| `/banque coffre <guilde>` | Coffre commun de 54 cases avec une guilde partenaire |
| `/marchand` | Où se trouve le marchand itinérant du jour |
| `/ressources [aller\|quitter\|info]` | Descendre au monde de ressources · remonter · date de la remise à neuf |

### Raids, sièges et combat

| Commande | À quoi elle sert |
| :--- | :--- |
| `/ah` puis le crâne de Wither | Acheter une horde pour attaquer une autre guilde (officiers) |
| `/siege declarer <guilde> <jour> <heure>` | Déclarer un siège (24 h de préavis), ex. `/siege declarer MaCible samedi 21` |
| `/siege liste` · `/siege annuler` | Voir les sièges · y renoncer |
| `/aide` | Appeler ses alliés à l'aide pendant une attaque |
| `/duel <joueur> [mise]` · `/duel accepter` · `/duel refuser` | Duel d'honneur dans une arène neutre |
| `/pari <camp> <montant>` | Parier sur un raid ou un duel (spectateurs) |
| `/rancon prix <montant>` · `/rancon liberer` · `/rancon executer` | Décisions du ravisseur |
| `/rancon payer` | Le captif paie sa rançon |
| `/rancon cellule` · `/rancon cellule retirer` | Désigner ou retirer la cellule de prison (officier) |

### Diplomatie et transports

| Commande | À quoi elle sert |
| :--- | :--- |
| `/traite` · `/traite liste` | Menu des traités (proposer, accepter, refuser, rompre) |
| `/station` | Voyager de gare en gare en wagonnet (le trajet dure selon la distance ; Maj + clic sur une destination : train de fret) |
| `/station fret confirmer\|annuler\|ajouter\|recuperer [n°]` | Gérer un convoi de fret et récupérer l'arrivage |
| `/rail` | Gérer son réseau : péage, relations avec les autres guildes (officiers) |

### Social, succès et vie du serveur

| Commande | À quoi elle sert |
| :--- | :--- |
| `/contrat` | Contrats du jour et contrat de guilde |
| `/merci <joueur>` | Offrir un point d'honneur (`/merci` seul : ta réputation et le top 3) |
| `/parrain <joueur>` · `/parrain accepter\|refuser` | Parrainer une recrue · répondre à une proposition |
| `/vote [numéro]` | Tribunal des guildes : voter la règle de la semaine (le lundi) |
| `/classement` | Classement de la saison des guildes |
| `/histoire` | Les grands moments du serveur |
| `/journal` | Journal de guerre (derniers grands faits) |
| `/succes [joueur]` | Tes succès (ou ceux d'un joueur) |
| `/climat` | Saison naturelle en cours et ses effets |
| `/cosmetique` · `/cosmetique off` | Auras, traînées, effets de victoire · tout retirer |
| `/deguisement <guilde\|Pseudo>` · `/deguisement off` | Changer de skin et de pseudo pendant 5 minutes (750 pièces) |

### Alias

| Commande | Alias |
| :--- | :--- |
| `/esicraft` | `/esi` |
| `/race` | `/metier`, `/classe` |
| `/guild` | `/g`, `/guilde`, `/faction` |
| `/age` | `/ages`, `/era` |
| `/ah` | `/hdv`, `/market`, `/marche` |
| `/money` | `/solde` |
| `/baltop` | `/balancetop`, `/topmoney`, `/richest` |
| `/pay` | `/payer`, `/virement`, `/sendmoney` |
| `/guide` | `/livre`, `/rules`, `/regles` |
| `/hub` | `/lobby`, `/mainhub` |
| `/contrat` | `/contrats`, `/quetes`, `/daily` |
| `/classement` | `/saison`, `/topguildes` |
| `/histoire` | `/memorial` |
| `/journal` | `/chronique`, `/gazette` |
| `/succes` | `/achievements`, `/hautsfaits` |
| `/climat` | `/nature`, `/meteo` |
| `/aide` | `/sos`, `/renfort` |
| `/deguisement` | `/masque`, `/disguise` |
| `/station` | `/gares`, `/train`, `/metro` |
| `/rail` | `/reseau`, `/gare` |
| `/traite` | `/traites`, `/treaty` |
| `/merci` | `/thanks`, `/honneur` |
| `/parrain` | `/mentor`, `/parrainage` |
| `/rancon` | `/ransom` |
| `/banque` | `/bank`, `/pret` |
| `/vote` | `/tribunal` |
| `/pari` | `/bet` |
| `/bourse` | `/actions` |
| `/taverne` | `/tavern` |
| `/cosmetique` | `/cosmetic`, `/cosmetics` |
| `/ressources` | `/ressource`, `/minage`, `/rw`, `/resources` |
| `/marchand` | `/merchant`, `/marchanditinerant` |
| `/batiment` | `/batiments`, `/building`, `/buildings` |
| `/tutoriel` | `/tuto`, `/tutorial` |
| `/english` | `/en`, `/anglais` |
| `/francais` | `/fr`, `/french` |

---

## 3. Les métiers

Chaque métier a des **objets qu'il est seul à pouvoir fabriquer**, des **avantages** et un **défaut**. Ces exclusivités disparaissent au fil des Âges : un équipement réservé à un métier devient accessible à tous plus tard.

| Métier | Spécialité | Avantages | Défaut |
| :--- | :--- | :--- | :--- |
| ⛏ **Nain** (mineur) | Pioches et pelles | Hâte II et Vision nocturne sous la couche 0 · 15 % de chances de doubler un minerai | Lenteur I en plein soleil (au-dessus de la couche 60) · ne fabrique ni armes ni armures en métal |
| 🌿 **Sylvain** (fermier) | Houes, poudre d'os, élevage, pommes dorées | Régénération I pour lui et les membres de sa guilde à moins de 4 blocs · récolte d'un clic droit avec replantation automatique | -20 % de dégâts au corps à corps |
| 🔨 **Forgeron** | Armures, épées, haches, boucliers, enclumes, meules, hauts-fourneaux | Résistance au feu permanente · 35 % de chances de doubler un lingot au four · enclume 2× moins chère en XP | Il a faim 25 % plus vite |
| 🏹 **Rôdeur** (archer) | Arcs, arbalètes et flèches (jusqu'à l'Âge 3) | Ses flèches empoisonnent · +25 % de dégâts à distance · Vitesse I dans les plaines, forêts, jungles et taïgas | Lenteur I avec un plastron en diamant ou Netherite |
| 🧪 **Arcaniste** (mage) | Enchantements, potions jetables, alambics (jusqu'à l'Âge 3) | +30 % d'XP · potions (bues ou jetées) qui durent 25 % plus longtemps · -20 % de dégâts magiques subis | +15 % de dégâts subis au corps à corps |
| ⚙ **Ingénieur** | Redstone, pistons, entonnoirs, distributeurs et **TNT** (jusqu'à l'Âge 3) | -40 % de dégâts d'explosion subis | -20 % de dégâts au corps à corps |

<details>
<summary><b>Quels outils et armures pour quel métier, Âge par Âge ?</b></summary>

| Métier | Âge 1 | Âge 2 | Âges 3-4 |
| :--- | :--- | :--- | :--- |
| Nain | Pioche en fer | Pioche et pelle en diamant | Pioche et pelle en Netherite |
| Forgeron | Armure, épée et bouclier en fer | Armure, épée et hache en diamant | Armure, épée et hache en Netherite |
| Sylvain | Houe en fer | Houe en diamant | Houe en Netherite |

À l'Âge 5, la Netherite est accessible à tout le monde.
</details>

**Changer de métier** : `/race` ouvre le menu. Le premier choix et les 3 changements suivants sont gratuits, ensuite chaque changement coûte 10 000 pièces. Il faut attendre 24 h entre deux changements. Les opérateurs n'ont aucun délai.

`/race show` affiche l'annuaire des joueurs connectés (métier, guilde, Âge, argent).

---

## 4. Les guildes et leur territoire

### Les rôles

| Rôle | Ce qu'il peut faire |
| :--- | :--- |
| 👑 **Chef** | Tout, plus : nommer des officiers, céder la guilde (`/guild transfer`), la dissoudre (`/guild disband`), abandonner un chunk |
| 🛡 **Officier** | Inviter, expulser, revendiquer des terres, poser le Nexus, choisir la spécialisation, débloquer des défenses, lancer des raids, signer des traités, gérer les gares |
| 🙂 **Membre** | Déposer de l'argent dans la banque, donner des ressources pour l'Âge, défendre |

- **10 joueurs maximum** par guilde, et on ne peut être que dans **une seule guilde**.
- Les invitations expirent après **5 minutes**.
- On ne peut pas dissoudre sa guilde pendant un raid, ni dans les 10 minutes qui suivent une attaque du Nexus.
- **Plafond des officiers** : un officier dépense au plus **3 000 pièces par jour** de la banque (claims, pièges, **hordes**, **sièges**, **prêts**) ; le chef n'a pas de limite.

### Revendiquer des terres

Le territoire se compte en **chunks** (carrés de 16 × 16 blocs). Avec `/guild claim`, un officier revendique le chunk où il se trouve.

- **Tous les chunks doivent se toucher** par un **côté** (pas en diagonale : plus de « serpents »). Seul le premier peut être placé n'importe où. Seul le **chef** peut abandonner un chunk (`/guild unclaim`), et pas si ça coupe le territoire en deux, ni celui du Nexus.
- **Tampon anti-encerclement** : un nouveau chunk doit rester à au moins **1 chunk** du territoire d'une guilde sans traité avec la tienne.
- **Prix** : payé par la banque de guilde. Le premier coûte 100 pièces, puis chaque nouveau chunk coûte 25 pièces de plus que le précédent (100, 125, 150, 175…).
- **Taille maximale selon l'Âge** :

| Âge | 1 | 2 | 3 | 4 | 5 et 6 |
| :--- | :--- | :--- | :--- | :--- | :--- |
| Chunks max | 16 | 32 | 64 | 128 | illimité |

- On ne peut rien revendiquer dans le monde de ressources, et plus rien tant qu'une **dette est impayée**.

`/guild claim show` affiche pendant une minute les **bords extérieurs** des territoires dans un rayon de 2 chunks (vert : le tien, rouge : les autres).

### Ce qui est protégé chez une guilde

Sur les terres d'une autre guilde, tu ne peux **ni casser ni poser de blocs**, ni ouvrir ses coffres, fours, barils, shulkers ou entonnoirs.

Y sont aussi interdits : la **TNT** (y compris en wagonnet), les **perles de l'Ender** et le **fruit de Chorus** pour s'y téléporter, les **cristaux de l'End**, les **pistons**, la **lave, l'eau et le feu** qui viennent de l'extérieur, la destruction de blocs par les Withers, Endermen et Ravageurs, les **projectiles** sur les animaux, cadres et supports d'armure ou sur les blocs (pots décorés et leur contenu, fleurs de chorus, stalactites), et la **canne à pêche** pour tirer hors du territoire ses animaux, villageois, supports d'armure, wagonnets ou objets au sol.

**Lave et eau** : un liquide ne passe jamais d'un territoire à un autre (même pendant un raid), les distributeurs ne peuvent pas en verser chez le voisin, et on ne peut pas vider un seau de **lave** à moins de 5 blocs d'un joueur qui n'est ni de sa guilde ni son allié.

**Entités protégées** : cadres, supports d'armure, minecarts (même à coffre), chevaux, ânes, animaux et laisses ne peuvent être ni frappés, ni montés, ni ouverts par un étranger. Les explosions n'abîment aucun bloc d'un territoire, et les distributeurs ne peuvent y vider ni seau, ni boule de feu, ni TNT venus de l'extérieur.

**Zone de siège** : pour qu'un Nexus reste atteignable, un assaillant peut casser les blocs (hors coffres et conteneurs) à moins de **4 blocs** d'un Nexus vivant, et les membres ne peuvent pas poser de bloc plein à moins de **2 blocs** de lui (pas de Nexus muré).

Les autres protections sautent seulement pendant un raid contre la guilde, et pendant **30 minutes** après la destruction de son Nexus s'il n'y a pas d'annexion.

### Le bonus de diversité

Plus ta guilde réunit de **métiers différents**, plus elle est forte (niveau visible dans `/guild info` et sur le site) :

| Métiers différents | Bonus |
| :--- | :--- |
| 3 | Primes de monstres +10 % |
| 4 | Nouvelles terres 10 % moins chères |
| 5 | Pièges du Nexus 15 % moins chers |
| 6 | Récompenses des contrats de guilde ×1,5 |

### Bâtiments de guilde (`/batiment`)

Comme la taverne, un bâtiment est un **chunk de son territoire** (jamais celui du Nexus, ni une taverne) qui contient les bons blocs. Un officier le déclare avec `/batiment declarer <forge|bibliotheque|ecurie>` (`/batiment retirer` pour le libérer) ; chaque guilde peut avoir **un bâtiment de chaque type**. Le bâtiment est revérifié régulièrement : s'il n'est plus conforme, son bonus est suspendu jusqu'à ce qu'on le répare. `/batiment` liste l'état des trois.

| Bâtiment | Blocs requis dans le chunk | Bonus pour les membres |
| :--- | :--- | :--- |
| ⚒ **Forge** | Une enclume, un haut fourneau (sous un toit) et une table de forgeron | Outils et armures s'usent **20 % moins vite** ; les enclumes de la forge coûtent **moitié moins** d'expérience |
| 📚 **Bibliothèque** | Une table d'enchantement (sous un toit), un pupitre et **15 bibliothèques** | **+15 % d'expérience** ; enchanter sur la table de la bibliothèque **rend 1 niveau** |
| 🐎 **Écurie** | **6 bottes de foin** et **8 clôtures** (ou portillons) | Les montures (chevaux, ânes, mules, chameaux, cochons, arpenteurs) reçoivent **Vitesse II et Régénération** (sans écurie, chevaux, ânes, mules, lamas et chameaux ont déjà Vitesse I) |

---

## 5. Les spécialisations de guilde

Un officier ou le chef choisit une spécialisation dans `/guild spec`. Elle donne des bonus à toute la guilde, mais impose des règles sur sa composition, vérifiées à chaque arrivée d'un joueur et à chaque changement de métier.

| Spécialisation | Bonus | Contraintes |
| :--- | :--- | :--- |
| ⚔️ **Légion Guerrière** (combat) | Force I et Résistance I sur vos terres · raids 30 % moins chers · +30 % de dégâts contre les monstres | Au moins 50 % de Rôdeurs et Forgerons · 1 Nain et 1 Sylvain maximum · -25 % de minerais et de récoltes |
| ⛏️ **Industrie Souterraine** (minage) | Hâte II sur vos terres sous la couche 30 · minerais bruts ×1,5 · pièges du Nexus 50 % moins chers | 1 Rôdeur maximum · potions de niveau II interdites · +20 % de dégâts magiques subis |
| 🌾 **Enclave Champêtre** (nature) | +2 cœurs de vie · Régénération I et Saturation sur vos terres · récoltes, butin des animaux, bois et pierres ×2 · animaux qui se reproduisent 3× plus vite | Aucun Ingénieur · 1 Forgeron maximum · armures en Netherite interdites |
| 💰 **Syndicat Marchand** (commerce) | Seule guilde à pouvoir commercer avec les villageois avant l'Âge 4 · +20 % sur les primes de monstres (dont 10 % versés à la banque) · aucune taxe à l'Hôtel des Ventes | Aucun Arcaniste · raids 30 % plus chers · pas de bonus de combat · Nexus -20 % de vie |
| 🔮 **Conclave Arcanique** (magie) | Nexus +25 % de vie · enchantements et enclume 2× moins chers en XP · potions d'un niveau au-dessus · enchantements boostés (IV devient V, V devient VI) · le Nexus tire un rayon sur l'attaquant le plus proche (1 cœur toutes les 2 s) · -50 % de dégâts magiques | -2 cœurs de vie · terres 50 % plus chères · 1 Forgeron et 1 Nain maximum · les enchantements boostés sont perdus si on change de spécialisation |
| 🏹 **Rôdeur Solitaire** (seul) | Vitesse I permanente · armures en Netherite et enchantements dès l'Âge 3 | Un seul membre, Rôdeur, qui n'a jamais été dans une autre guilde · aucun coffre sur ses terres · Nexus limité à 2 500 PV |
| 🔱 **Atlante** (océan) | **Dans l'eau** : respiration aquatique, vision nocturne, agilité aquatique, grâce du dauphin et Résistance | **Nexus obligatoirement immergé** (au moins un bloc d'eau contre lui) · **Lenteur hors de l'eau** |

> [!IMPORTANT]
> Le premier choix est immédiat. Ensuite, on ne peut changer de spécialisation **que lorsqu'un nouveau joueur** (jamais passé par la guilde) la rejoint avec **au moins 2 h de jeu** (ou lorsqu'un membre d'une guilde vaincue lui prête allégeance), au plus une fois tous les 7 jours, et jamais pendant un raid, un siège ou dans les 10 minutes qui suivent une attaque du Nexus. Chaque changement offre le droit de [déplacer le Nexus](#déplacer-le-nexus-nexus-move).

**Atlantes** : posez le Nexus contre un bloc d'eau. L'eau est alors autorisée tout autour (pose, seaux, écoulement) dans le chunk du Nexus ; la lave et l'obsidienne restent interdites. Les membres ont tous les avantages sous-marins **dans l'eau** et subissent **Lenteur I sur la terre ferme**.

---

## 6. Le Nexus

Le **Nexus** est le cœur de ta guilde. On le pose dans son territoire avec `/guild nexus place` (une seule fois : impossible de le reposer pour remettre sa vie à 100 %). **S'il est détruit, ta guilde perd tout** (voir [Si le Nexus tombe](#si-le-nexus-tombe)).

### Règles de pose

- **Dans l'Overworld uniquement**, dans un chunk revendiqué de ta guilde.
- **Zone de sécurité de 16 blocs** : pas d'eau, de lave ni d'obsidienne (y compris pleureuse), et on ne peut pas en ajouter ensuite. Ça empêche les douves et les murs incassables.
- **Atteignable** : on ne peut pas le poser sous plus de 3 blocs pleins de roche (le siège ne casse qu'à 4 blocs de lui).
- **Le Nexus doit être visible** : un coup n'est compté que si l'assaillant le voit (portée et ligne de vue vérifiées).

### Vie, soins et protection

- Les PV du Nexus s'affichent au-dessus de lui (hologramme), y compris après un redémarrage ou une reconnexion.
- **Le soigner** : un membre jette une **pomme dorée** dessus (ou fait un clic droit avec) : +500 PV, ou +2 500 PV avec une pomme dorée enchantée. 30 secondes entre deux soins, et c'est impossible pendant un raid ou dans les 2 minutes qui suivent une attaque de joueurs.
- **Qui est épargné** : le golem gardien et les pièges ne visent jamais les membres de la guilde (le golem ne se retourne pas contre eux même s'ils le frappent) ni ceux d'une guilde liée par un traité (alliance, pacte de non-agression ou vassalité).

### L'arbre technologique du Nexus (`/nexus`)

Le Nexus gagne de l'**expérience** à chaque horde repoussée (+200) et quand on lui **sacrifie des ressources** (`/nexus sacrifice`, objet en main, à moins de 10 blocs : charbon, fer, or, diamant, émeraude, netherite, perle d'Ender, bâton de blaze… ; plafond de 3 000 XP par jour). Il monte ainsi du niveau 1 au niveau 6 (0, 300, 900, 2 000, 4 500, 9 000 XP).

Au lieu d'acheter des pièges jetables, les officiers **débloquent** les défenses dans `/nexus` : il faut le niveau requis, la défense « parente » déjà débloquée, des **objets à sacrifier** pris dans l'inventaire, et, pour les défenses « vivantes », avoir **ramené puis tué le mob correspondant dans le chunk du Nexus**. Une défense débloquée est **permanente** (elle n'est plus consommée par les hordes) ; l'Industrie Souterraine paie moitié moins d'objets. Elles visent les monstres et les **intrus**, jamais les membres, les alliés (traité) ni les joueurs en créatif ou spectateur.

| Défense | Niv. | Prérequis | Effet |
| :--- | :---: | :--- | :--- |
| 🔥 Brasier Infernal | 1 | 64 charbon, 16 silex | À moins de 8 blocs : 1 cœur de dégâts toutes les 2 s et brûlure |
| 🕸️ Toile d'Entravement | 1 | 128 ficelles, 16 boules de slime | Lenteur II à moins de 10 blocs |
| ⚡ Foudre de Riposte | 2 | Brasier · 96 cuivre, 64 redstone | Foudroie quiconque frappe le Nexus (4 cœurs, un éclair toutes les 5 s par joueur) |
| 🤖 Automate Gardien | 2 | **Tuer un golem de fer dans le chunk** · 8 blocs de fer, 32 redstone | Un golem de fer attaque les ennemis ; reconstruit 5 min après sa mort |
| 🧪 Brume Toxique | 2 | Toile · 48 yeux d'araignée, 16 slime | Poison I à moins de 8 blocs (il ne tue jamais) |
| 🔥 Tir de Blaze | 3 | Brasier · **tuer un blaze** · 24 bâtons de blaze, 32 or | Jusqu'à 3 boules de feu toutes les 5 s |
| 🐡 Tir de Gardien | 3 | Foudre · **tuer un gardien** · 64 éclats + 32 cristaux de prismarine | Rayon aquatique (dégâts + lenteur) toutes les 4 s |
| 🐝 Essaim d'abeilles | 3 | Brume · **tuer une abeille** · 32 rayons de miel, 8 fioles de miel | Jusqu'à 6 abeilles enragées qui piquent les ennemis |
| 🪄 Tir d'Évocateur | 4 | Golem · **tuer un évocateur** · 64 émeraudes, 1 totem | Crocs magiques : 6 dégâts de zone toutes les 9 s |
| 🟪 Tir de Shulker | 4 | Gardien · **tuer un shulker** · 8 carapaces, 16 perles | Projectile guidé + lévitation toutes les 6 s |
| 👻 Tir de Ghast | 5 | Blaze · **tuer un ghast** · 8 larmes, 128 poudres à canon | Boule de feu de zone (8 dégâts, sans casser de blocs) toutes les 14 s |
| 👁 Tir du Warden | 5 | Évocateur · **tuer un warden** · 8 éclats d'écho, 4 catalyseurs | Onde sonique : 10 dégâts et repoussée toutes les 15 s |
| 🌊 Malédiction de l'Ancien | 6 | Gardien · **tuer un gardien ancien** · 128 briques de prismarine, 8 coquilles de nautile | Fatigue de minage III et dégâts à tous les ennemis proches toutes les 25 s |

Tout débloquer rapporte le succès *Archi-technicien*. Réglages : `nexus.tech.*` (coût, dégâts, XP).

### Déplacer le Nexus (`/nexus move`)

Chaque **changement de spécialisation** offre à la guilde le droit de **déplacer son Nexus une fois**, dans les 7 jours : un officier se tient à l'endroit voulu (dans le territoire de la guilde) et tape `/nexus move`. Les règles de pose s'appliquent (zone de sécurité, Atlantes dans l'eau, Nexus atteignable). La vie et les défenses sont conservées ; impossible pendant un raid, un siège ou dans les 5 minutes qui suivent une attaque.

### Roulette russe du Nexus (`/nexus gamble`)

Le **chef de guilde**, face au Nexus (moins de 6 blocs), peut tenter un coup de poker (confirmation à retaper dans les 15 s, une fois par **72 h**, impossible pendant un raid). La banque doit contenir la **mise de 2 000 pièces**, retenue dès la confirmation :

- **50 % (pile)** : le Nexus gagne **+1 000 PV max** et la banque récupère sa mise **plus 2 000 pièces**.
- **50 % (face)** : il perd **1 000 PV max** et **1 500 PV** (il survit toujours à 1 PV, PV max jamais sous 1 000), la mise est perdue, et une **mini-horde de pillards** surgit sans préavis au pied du Nexus.

Succès *Tout pour le tout* en cas de victoire.

---

## 7. Raids, sièges et combat entre joueurs

### Lancer un raid

Un raid, c'est **payer une armée de monstres** pour qu'elle attaque le Nexus d'une autre guilde.

1. Ouvre `/ah` et clique sur le **crâne de Wither** (en bas du menu).
2. Choisis une **horde toute faite** (zombies cuirassés, squelettes incendiaires, araignées, pillards et ravageurs) ou **compose la tienne** (jusqu'à 60 monstres).
3. Choisis la guilde cible. Le prix est payé par **ta banque de guilde**.

| Horde | Prix | Monstres |
| :--- | :-: | :-: |
| 🧟 Zombies cuirassés | 2 500 | 24 |
| 🏹 Squelettes incendiaires | 3 500 | 20 |
| 🕷️ Araignées toxiques | 3 000 | 30 |
| 👺 Pillards et ravageurs | 7 500 | 15 |

Options possibles : des **sapeurs** qui font exploser les murs et assèchent l'eau et la lave autour d'eux, et l'**ignifugation** pour que tes monstres traversent la lave.

### Conditions

- Seuls les **officiers et le chef** peuvent lancer un raid.
- **Sans défenseur** connecté et sur ses terres, un raid est encore possible mais le Nexus n'encaisse que **25 %** des dégâts.
- **Durée maximale** : 25 minutes ; au-delà, le raid s'achève sans victoire ni défaite. Si une part importante de la horde se volatilise sans combat (chunk déchargé…), la victoire n'est pas créditée.
- Après un raid, la guilde visée est **protégée 30 minutes**.
- **Délai de 30 minutes** entre deux raids lancés par une même guilde.
- Impossible d'attaquer une guilde avec qui on a un **traité** (voir [Diplomatie](#10-diplomatie-et-transports)), ni pendant la **trêve** du vendredi et du samedi.
- Pendant un raid, seuls les monstres de la horde et le TNT allumé par la guilde attaquante détruisent la base (pas un tiers).
- **Les hordes minuscules ne rapportent rien** : repousser une horde de moins de 12 monstres ne donne ni XP de Nexus, ni points de saison, ni succès, ni prime de renfort (`/aide`) ; on ne peut pas parier dessus et elle ne compte pas pour la quête « Lancer 3 hordes ».

### Si le Nexus tombe

- Le vainqueur est la guilde qui a infligé **le plus de dégâts** au Nexus dans les 10 dernières minutes (pas celle qui porte le dernier coup).
- Sans guilde victorieuse (Nexus détruit par un joueur sans guilde ou par une horde de l'administration), le territoire est **vulnérable 30 minutes**, puis le Nexus **renaît** avec un quart de ses PV : il peut de nouveau être soigné, raidé et assiégé.
- Le **territoire** du perdant passe au gagnant (dans la limite de son Âge).
- Toute la **banque** du perdant passe au gagnant, avec ses **dettes et ses créances** (`/banque dettes`) ; si le perdant était coté en bourse, ses actionnaires sont d'abord remboursés.
- Ses traités, gares et péages disparaissent.
- Les membres du perdant choisissent : rejoindre le vainqueur (`/guild annex join`) ou partir (`/guild annex decline`).
- La conquête ne rapporte des **points de saison** et le succès *Conquérant* que si la guilde vaincue était **établie** (au moins 2 membres et 3 jours d'existence).

### Les guildes trop riches sont des cibles

Une guilde passe en **Hégémonie économique** si au moins la moitié de ses membres sont dans le top 5 des plus riches (`/baltop`), ou si sa **banque plus les pièces de ses membres** détiennent plus de 30 % de tout l'argent du serveur. Contre elle, les raids coûtent **2× moins cher** et sa protection après un raid ne dure que **2 minutes**. Elle est marquée d'une ⭐ dans le menu des cibles.

### Équilibre : revanche et rattrapage

- **Revanche** : la guilde qui perd un raid obtient **24 h de hordes à -50 %** contre son vainqueur.
- **Rattrapage** : les guildes **sous la médiane** de la saison touchent jusqu'à **+25 %** sur les primes de monstres (4 guildes au moins). Le bonus diminue à mesure qu'elles rattrapent et disparaît à la médiane.

### Guerres formelles : les sièges (`/siege`)

Un officier plante un **Étendard de Siège** sur la **frontière** d'une guilde rivale (dans un chunk voisin du sien, en terrain libre ou sur son propre territoire, dans un espace vide posé sur un sol solide) avec `/siege declarer <guilde> <jour> <heure>` (ex. `samedi 21`).

- **24 heures de préavis**, 500 pièces de frais pris dans la banque, créneau d'une heure qui ne peut pas chevaucher la trêve, impossible sous traité.
- La cible peut **annuler le siège en détruisant l'Étendard** avant l'heure dite (elle empoche les frais).
- Pendant le créneau, les membres de la guilde assiégeante peuvent **démolir le Nexus ennemi à la pioche ou à l'arme lourde** : dégâts ×2, même sans défenseur présent, sans acheter de horde.
- **Victoire** si le Nexus tombe ou si 40 % de ses PV max sont infligés : 10 % de la banque du vaincu (3 000 maximum) sont pillés. Sinon le défenseur garde les frais.
- Une guilde ne mène qu'**un siège à la fois** mais peut en subir jusqu'à 3 ; si la cible disparaît avant le siège, les frais sont rendus. `/siege liste` montre les sièges, `/siege annuler` renonce (moitié des frais rendue).

Historique au Panthéon du site. Succès *Maître du siège*.

### Combat entre joueurs (PvP)

Frapper un joueur d'une autre guilde (coup, flèche, TNT, loup, potion nocive…) ou être frappé met « en combat » 15 secondes. Pendant ce temps :

- **`/hub`, `/station`, `/pay`, `/ah`, `/banque`, `/guild bank|home|leave`, `/home`, `/tpa`… sont bloqués**, les téléportations par commande aussi ;
- **se déconnecter est puni de mort** (kick compris ; seul l'arrêt du serveur est excusé) ;
- dépenser ou transférer ses pièces est impossible (achats, `/pay`, banque, métier, déguisement) ; seul le paiement d'une rançon reste permis ;
- **les coffres sont fermés** (coffre, tonneau, shulker, coffre de l'Ender, wagonnet, âne à coffre), et on ne peut ni donner d'objets à l'Âge de sa guilde, ni les sacrifier au Nexus ;
- le butin d'un joueur qui meurt revient à son tueur.

**Tuer un joueur** d'une autre guilde : tu lui **prends 100 pièces + 1 % de sa fortune** (ou tout ce qu'il a s'il a 100 pièces ou moins). Pas de prime si vous vous êtes déjà affrontés il y a moins de 2 minutes, ni entre membres d'une même guilde.

### Rançon (`/rancon`)

Un coup qui devrait **tuer** un joueur d'une autre guilde le **capture** à la place : il est immobilisé et invulnérable pendant 90 secondes. Son ravisseur choisit :

- `/rancon prix <montant>` fixe la rançon (100 à 2 000 pièces ; par défaut 15 % de la fortune du captif) ;
- le captif paie avec `/rancon payer` : 30 % pour le ravisseur, 70 % pour la banque de sa guilde ;
- `/rancon liberer` le libère gratuitement, `/rancon executer` le tue (prime de mort habituelle).

Sans décision, il est exécuté. Se déconnecter en captivité équivaut à mourir. Après un paiement de rançon, il ne peut pas être recapturé pendant 10 minutes.

**Cellule de prison** : un officier vise un bloc de son territoire et tape `/rancon cellule` (`/rancon cellule retirer`). Le captif y est alors **téléporté** pendant la rançon (puis ramené à son point de capture). Ses alliés (même guilde ou traité) peuvent le **délivrer** en agissant 15 fois sur le bloc de la cellule (clic droit ou gauche) ; pensez à fermer la cellule !

### Duels d'honneur et paris (`/duel`, `/pari`)

- `/duel <joueur> [mise]` provoque un joueur (mise en pièces optionnelle, `/duel accepter` ou `/duel refuser`). Les deux sont **téléportés dans une arène neutre** éclairée au-dessus du monde (une plateforme par duel, 6 en même temps, **indestructible** et réparée avant chaque duel) ; leur **inventaire, leur vie et leur position sont sauvegardés** puis rendus à la fin (même après un plantage, à la reconnexion). Ils combattent avec le **même équipement fourni** (armure en fer complète, épée et hache en fer, bouclier, arc, 16 flèches, 64 steaks, 1 potion de soin instantané).
- **Personne ne meurt** : le coup décisif met fin au combat. Le vainqueur remporte la mise de l'adversaire. Un duel **ne déclenche ni rivalité, ni trahison, ni capture, ni marque de combat**, et n'est pas stoppé par la trêve. Maximum 5 minutes (match nul au-delà, mises rendues) ; abandonner = perdre. Succès *Maître d'armes* (10 duels).
- `/pari <camp> <montant>` : au début d'un **raid** ou d'un **duel**, les **spectateurs** (hors des guildes concernées / hors duellistes) ont **2 minutes** pour parier (10 à 1 000 pièces **au total** par pari) sur la guilde `attaquante`/`defensive` (ou son nom) ou sur un duelliste. Le camp vainqueur se partage la cagnotte moins **5 %** prélevés par le serveur ; sans pari adverse, ou si le combat est annulé, tout est remboursé. **Anti-triche** : sont écartés les membres des guildes concernées **et celles qui leur sont liées par un traité**, tout joueur dont l'**adresse IP** est celle d'un participant en ligne, et les comptes de moins de 3 jours.

### L'appel à l'aide (`/aide`)

Pendant un raid ou une attaque de ton Nexus, `/aide` prévient tes **alliés** (alliance ou vassalité). Chaque allié qui rejoint tes terres avant la fin de la horde gagne **250 pièces** (prises dans la banque de ta guilde) si elle est repoussée (6 alliés au plus), à condition que ce soit une vraie horde (au moins 12 monstres, deux victoires comptées par jour contre le même attaquant). Un appel toutes les 10 minutes.

---

## 8. Les Âges

Ta guilde commence à l'**Âge 1** et peut monter jusqu'à l'**Âge 6**. Pour passer à l'Âge suivant, les membres **donnent des ressources** dans le menu `/age` (ou objet en main avec `/age deposit`). Chaque Âge débloque des équipements et agrandit le territoire possible.

| Âge | Ce qu'il débloque |
| :--- | :--- |
| 1. **Rustique** (départ) | Bois et pierre. Le fer est réservé aux métiers spécialisés. Nether et End fermés. |
| 2. **Métallurgie** | **Le Nether**, les outils en fer et les boucliers pour tous. Le diamant est réservé aux métiers spécialisés. Gares et fret. |
| 3. **Alchimie** | **L'End**, les outils en diamant et l'armure en fer pour tous. La Netherite est réservée aux métiers spécialisés. |
| 4. **Guerre Totale** | Pour tous : armures en diamant, commerce avec les villageois, TNT, redstone, pistons, alambics, potions jetables, arcs et arbalètes, **vol en élytre** (depuis une piste d'aéroport). Bourse des guildes. **Bonus des joueurs** : Vitesse I, +10 % de dégâts, +25 % d'XP, -20 % de dégâts d'explosion. |
| 5. **Mythique** | La Netherite et les balises pour tous, territoire illimité. **Bonus des joueurs** : Hâte I, Vitesse I, Résistance I, +15 % de dégâts, +50 % d'XP. |
| 6. **Suprématie** | L'Âge ultime. **Récompense** : un œuf de dragon et un bloc de Netherite pour chaque membre, et un titre dans le tchat : **[Giga Tchad]** si ta session dure moins d'une heure, **[Gros Nerd]** au-delà. |

> [!NOTE]
> Les quantités ci-dessous sont celles d'une **guilde d'un seul joueur**. Elles augmentent avec le nombre de membres (×1,3 à 2 joueurs, ×2,2 à 5, ×4,7 à 10), en comptant le **maximum de membres** atteint pendant l'Âge. Le menu `/age` affiche toujours les quantités exactes pour ta guilde.

<details>
<summary><b>Ressources à donner pour chaque Âge</b></summary>

- **Âge 2** : 750 lingots de cuivre, 256 cuirs, 750 fer brut, 1 000 bûches de chêne, 1 500 blé, 750 carottes.
- **Âge 3** : 3 500 lingots de fer, 1 500 lingots d'or, 1 500 lapis-lazuli, 1 000 quartz, 750 verrues du Nether, 150 bâtons de Blaze, 150 crèmes de magma, 15 larmes de Ghast, 750 poudres lumineuses, 3 500 plumes, 6 000 pommes de terre, 6 000 cannes à sucre, 1 000 citrouilles, 2 500 tranches de pastèque, 1 500 betteraves.
- **Âge 4** : 500 diamants, 150 blocs de fer, 100 blocs d'or, 1 000 obsidiennes, 200 obsidiennes pleureuses, 500 perles de l'Ender, 1 paire d'élytres, 2 500 carottes dorées, 150 pommes dorées, 1 200 bottes de foin, 800 fioles de miel, 800 boules de slime, 6 000 poudres de redstone, 400 répéteurs, 350 pistons, 200 pistons collants, 150 distributeurs, 2 500 poudres à canon, 250 TNT, 750 fusées, 32 enclumes.
- **Âge 5** : 16 lingots de Netherite, 5 étoiles du Nether, 4 balises, 8 totems d'immortalité, 16 éclats d'écho, 16 catalyseurs, 16 hurleurs et 64 capteurs de sculk, 64 carapaces de shulker, 2 500 pierres de l'End, 1 200 blocs de purpur, 2 500 fruits de Chorus et 1 000 fruits de Chorus éclatés, 1 000 blocs de redstone, 400 comparateurs, 400 observateurs, 250 entonnoirs, 150 cibles, 150 blocs d'émeraude.
- **Âge 6** (quantités fixes) : tous les disques de musique, toutes les têtes de monstres, les 8 cornes de chèvre, 64 étoiles du Nether, une houe en Netherite enchantée Raccommodage + Solidité III + Fortune III, tous les modèles de forge, les 20 tessons de poterie, l'œuf de dragon et un buisson mort.

</details>

---

## 9. L'argent, le marché et la bourse

### Gagner des pièces

- **Tuer des monstres** : chaque monstre rapporte une prime, de 1,50 pièce (poisson d'argent) à 750 pièces (Ender Dragon). Les fermes à monstres répétitives rapportent moins, et un plafond horaire s'applique.
- **Contrats du jour** (`/contrat`) : 3 missions par jour, dont une liée à ton métier. +400 pièces si tu fais les trois. Les monstres de générateur (spawner), d'œuf ou de commande ne comptent pas pour les contrats « tuer ».
- **Hôtel des Ventes** (`/ah`) : vends et achète entre joueurs.
- **Événements** et **boss mondial** (voir [La vie du serveur](#11-la-vie-du-serveur)).
- **Tuer un joueur** d'une autre guilde, ou capturer un joueur contre rançon.

<details>
<summary><b>Tableau des primes de monstres</b></summary>

| Catégorie | Monstres et prime |
| :--- | :--- |
| 🐉 Boss | Ender Dragon 750 · Warden 500 · Wither 350 · Grand Gardien 150 |
| 👺 Mini-boss | Ravageur 75 · Évocateur 40 · Piglin barbare 25 |
| 🔥 Nether et End | Ghast et Shulker 20 · Wither squelette et Gardien 15 · Blaze, Hoglin et Zoglin 12 |
| 🏹 Élite | Vindicateur 15 · Sorcière 12 · Enderman 10 · Pillard 8 |
| 🧟 Communs | Creeper et Phantom 5 · Piglin et Piglin zombifié 4 · Araignées 3 à 3,50 · Squelettes 3 · Zombies 2,50 |
| 🐛 Petits | Vex 2,50 · Slimes et cubes de magma 2 · Poissons d'argent et Endermites 1,50 |

Le Syndicat Marchand touche 20 % de plus.
</details>

### Le contrat de guilde

Dans `/contrat`, sous tes contrats personnels : chaque jour, ta guilde reçoit **trois objectifs, chacun réservé à un métier** (par exemple : un Nain mine, un Forgeron fond des lingots, un Ingénieur pose de la redstone). Seuls les membres du bon métier font avancer leur objectif. Chaque objectif rapporte **200 pièces** à la banque de guilde, et les trois ensemble **750 de plus**. Les objectifs grandissent avec la taille de la guilde, et ne rapportent qu'aux guildes d'au moins 3 membres.

### Dépenser

Banque de guilde (`/guild bank <montant>`) pour les terres, les pièges et les raids · virements (`/pay`) · achats à l'Hôtel des Ventes · trajets en train · changements de métier · cosmétiques · déguisement.

### L'Hôtel des Ventes (`/ah`)

- **Vendre** : tiens l'objet en main et tape `/ah sell <prix>` (10 annonces maximum, prix de 1 à 1 000 000). Une taxe de **5 %** est prélevée sur chaque vente (aucune pour le Syndicat Marchand). Clique sur ta propre annonce pour la retirer.
- **Raids** : le crâne de Wither en bas du menu ouvre le menu des hordes (voir [partie 7](#7-raids-sièges-et-combat-entre-joueurs)).

#### Commandes d'achat

En plus des annonces, l'HDV accepte des **commandes d'achat** : « j'achète 64 diamants à 30 pièces l'unité ».

- **Passer une commande** : tenez l'objet voulu en main et tapez `/ah commande <quantité> <prix unitaire>`, ou `/ah commande <matériau> <quantité> <prix unitaire>` (nom anglais, ex. `diamond`). La somme totale est **bloquée** aussitôt. 5 commandes ouvertes au plus, 2 304 objets au plus par commande.
- **Livrer** : `/ah commandes` (ou le livre dans `/ah`) liste les commandes, les mieux payées d'abord. Un clic livre tout ce que vous avez (dans la limite de la commande) et vous **payez aussitôt**, taxe du marché déduite. Seuls les objets « nus » comptent (sans nom, enchantement ni usure).
- **Récupérer** : sur sa propre commande, clic gauche pour retirer les objets livrés, clic droit pour **annuler** (la partie non livrée est remboursée). Les objets livrés attendent tant que vous ne les avez pas retirés, même hors connexion.

### Banque de guilde : prêts et coffre commun (`/banque`)

Entre deux guildes **liées par un traité** (alliance, pacte ou vassalité) :

- **`/banque` ouvre le menu de la banque** : offres reçues (clic gauche pour accepter, clic droit pour refuser), dettes (clic gauche pour tout rembourser, clic droit pour rembourser 1 000 pièces), créances (Maj + clic droit pour effacer une dette, chef seulement), offres envoyées (clic pour les retirer), et accès aux coffres communs.
- **Proposer un prêt** (officiers) : bouton « Proposer un prêt », choix de la guilde partenaire, puis du **montant** et du **taux d'intérêt**, réglable de **0 à 50 %** (10 % par défaut). En commande : `/banque pret <guilde> <montant> [taux]`. Un prêt est limité à 1 500 pièces par chunk revendiqué de l'emprunteur (100 à 20 000 pièces).
- L'offre reste valable 10 minutes. Une fois acceptée (menu ou `/banque accepter <guilde>`), les pièces passent d'une banque à l'autre ; la guilde emprunteuse doit rendre le montant **plus les intérêts au taux convenu** sous **5 jours**.
- `/banque dettes` liste les prêts · `/banque rembourser <guilde> [montant]` rembourse (en plusieurs fois possible) · `/banque pardonner <guilde>` efface une dette (**chef seulement**). Un prêt proposé par un officier compte dans son plafond de 3 000 pièces par jour.
- `/banque coffre <guilde>` ouvre un **coffre commun** de 54 cases partagé par les deux guildes (accessible tant qu'un traité les lie).
- **Une dette impayée pèse sur les territoires** : dès l'échéance dépassée, la guilde ne peut plus revendiquer de chunk, et **un chunk lui est saisi toutes les 24 h** (jamais celui du Nexus, jamais un chunk qui couperait le territoire). Chaque chunk saisi efface 300 pièces de dette.

### La bourse (`/bourse`)

À partir de l'**Âge 4**, le chef peut **ouvrir le capital** de sa guilde (`/bourse ouvrir`, 1 000 parts). Le **cours** d'une action n'est pas fixe : c'est un vrai marché, mis à jour toutes les 10 minutes.

**Ce qui fait la valeur d'une guilde** (valeur fondamentale, détaillée par `/bourse info <guilde>`) :

- sa **richesse** (capitaux propres : banque moins dettes nettes), son **territoire** (chunks) et son **effectif** ;
- la **réputation de ses membres** (points d'honneur `/merci`) ;
- l'**Âge** de la guilde, ses **résultats de la saison** et la **diversité** de ses métiers ;
- la force du **Nexus** : niveau, défenses débloquées et vie restante (un Nexus détruit fait s'effondrer la valeur) ;
- les **impayés** (une dette en retard pénalise de 15 %).

**Comment le cours bouge** : il se rapproche de la valeur fondamentale (15 % de l'écart à chaque mise à jour) avec un **bruit aléatoire** et l'**humeur générale du marché** (optimiste ou pessimiste, qui tire tous les cours dans le même sens). Des **chocs** immédiats suivent les événements : horde repoussée +2 %, siège gagné +4 % / perdu −4 %, siège repoussé +3 % / raté −3 %, boss terrassé +3 %, **Nexus détruit −35 %**. Enfin, **vos ordres comptent** : acheter 10 % du capital relève le cours de 8 %, vendre le fait baisser.

**Acheter et vendre** : les joueurs **extérieurs** achètent des parts (`/bourse acheter <guilde> <parts>`, 100 par joueur, 49 % du capital au plus) et les revendent à la guilde à 95 % du cours, **après 24 h de détention** et sans jamais gagner plus de 3 fois le prix payé. Toutes les 6 heures, **20 % du bénéfice** de la guilde (hausse de ses capitaux propres) est réparti sur les 1 000 parts : les actionnaires touchent la part de leurs actions, la guilde garde celle des parts qu'elle n'a pas vendues. `/bourse` affiche les cours (▲▼ sur 24 h), `/bourse portefeuille` vos actions ; la page **Bourse** du site montre cours, variation et mini-graphiques.

Une guilde **dissoute ou annexée** quitte la cote : avant de disparaître, sa banque **rachète les parts** du public, au plus au prix payé (et à la valeur de marché), au prorata si elle ne suffit pas. Succès *Actionnaire*. Tous les facteurs se règlent dans `bourse.*`.

### Le marchand itinérant de guilde (`/marchand`)

Chaque jour (vers 14 h), un **marchand rare** s'installe pendant 6 heures près du Nexus d'une **guilde tirée au sort** (Nexus posé, au moins 4 territoires, un membre en ligne ; jamais deux jours de suite la même). Tout le serveur est prévenu et **tout le monde peut venir le voir** : c'est l'occasion de traverser des terres étrangères, de négocier un passage… ou de tendre une embuscade.

- Il vend 5 objets tirés au sort parmi : livres Raccommodage, Solidité III, Efficacité IV, Fortune III, trident, cœur de la mer, gabarit d'amélioration en netherite, œuf de renifleur, selle, étiquettes, fioles d'expérience, débris antiques, carapaces de shulker, pommes dorées. Paiement en **émeraudes** ou **diamants**, 3 exemplaires par offre, **2 achats par joueur et par jour**.
- Il n'exige aucun Âge : tout le monde peut commercer avec lui.
- La guilde hôte touche **75 pièces** en banque à chaque achat d'un visiteur étranger : elle a intérêt à bien recevoir.
- `/marchand` donne sa position et l'heure de son départ. S'il perd le terrain qui l'accueille, il s'en va.

### Le monde de ressources (`/ressources`)

Un **monde minier séparé**, remis à neuf **chaque semaine** (le lundi à 5 h par défaut). On y trouve du **minerai frais** et des terres sauvages, ce qui évite de transformer le monde principal en gruyère. **Aucun territoire** ne peut y être revendiqué : le **PvP y est libre** partout.

- `/ressources` : descendre (point d'arrivée aléatoire et sûr dans une bordure de 6 000 blocs ; pas en combat, en duel ni en captivité ; 5 minutes entre deux descentes).
- `/ressources quitter` : remonter là d'où l'on venait (ou à son lit, ou au point d'apparition).
- `/ressources info` : date de la prochaine remise à zéro.

Les joueurs présents sont prévenus 60, 30, 10, 5 et 1 minute avant la remise à zéro, puis renvoyés à la surface. Un joueur déconnecté dans l'ancien monde revient au point d'apparition à sa reconnexion. Les portails n'y fonctionnent pas. **Ne laissez rien de précieux en bas !**

---

## 10. Diplomatie et transports

### Les traités (`/traite`)

Un officier propose un traité à une autre guilde, qui a 10 minutes pour accepter (clic gauche) ou refuser (clic droit) dans son menu `/traite`.

| Traité | Durée | Effets |
| :--- | :--- | :--- |
| 🤝 **Alliance** | Illimitée | Pas de raid entre vous · trains gratuits chez l'autre · vous ne pouvez pas vous blesser (coups, flèches, potions) |
| 📜 **Pacte de non-agression** | 7 jours | Pas de raid entre vous · **frapper un joueur de l'autre guilde ou son Nexus rompt le pacte** (trahison + rivalité) |
| 👑 **Vassalité** | Illimitée | Comme une alliance, mais le vassal verse chaque jour 5 % de sa banque à son suzerain |

**Rompre un traité, c'est une trahison** : ta guilde verse jusqu'à 500 pièces d'indemnité à l'autre, tout le serveur est prévenu, et tu ne peux pas l'attaquer pendant 30 minutes. Seule exception : un suzerain peut libérer son vassal gratuitement.

**Agression sous traité** : si un joueur attaque le Nexus d'une guilde liée par un traité (pacte, alliance ou vassalité, y compris le suzerain contre son vassal), ou frappe un membre d'une guilde sous pacte (coup, flèche), le traité est rompu automatiquement. C'est une trahison (indemnité, préavis de 30 minutes, annonce au serveur) et une **rivalité** s'ouvre entre les deux guildes (visible sur le site, page diplomatie).

**Apaiser une rivalité** : elle s'estompe seule avec le temps (le poids des raids, trahisons et morts est divisé par deux tous les 7 jours) et **disparaît dès que les deux guildes signent un traité**.

### Le train (`/station`, `/rail`)

Le réseau ferroviaire relie les gares, à condition qu'une **ligne de rails continue** les relie (embranchements et croisements compris). Le trajet **n'est pas instantané** : tu voyages dans un wagonnet qui suit le tracé réel des rails à grande vitesse (30 blocs par seconde par défaut, `rail.ride.*`), donc **plus la ligne est longue, plus le trajet dure** (5 secondes au minimum, 10 minutes au maximum). Pendant le voyage tu ne peux ni frapper ni être blessé, et tu es déposé à la gare d'arrivée (même si tu te déconnectes).

- **Voyager** : près d'une gare (panneau `[Gare]`), tape `/station` et choisis ta destination.
- **Prix** : 1 pièce pour la guilde de la gare d'arrivée, plus le **péage** de chaque guilde dont tu traverses les terres. Ta propre guilde et ses alliés ne font pas payer.
- **Construire une gare** : un officier pose un panneau avec `[Gare]` sur la 1re ligne et le nom de la gare sur la 2e, à moins de 2 blocs d'un rail. Il faut être à l'Âge 2, payer 500 pièces avec la banque (dans la limite du plafond quotidien des officiers), et pas plus de 6 gares par guilde.
- **Gérer son réseau** (`/rail`, officiers) : fixer son péage (**toutes les spécialisations jusqu'à 10 pièces, le Syndicat Marchand jusqu'à 100**), et pour chaque autre guilde choisir **Normale**, **Alliée** (gratuit) ou **Bannie** (les lignes qui passent chez vous lui sont coupées).
- **Sabotage** : hors de tout territoire (terrain neutre compris), les rails posés par une guilde ne se cassent pas à la main depuis une autre guilde : il faut de la **TNT** (ou une autre explosion). Les alliés et vassaux peuvent modifier la ligne. Sur un territoire de guilde, la protection du territoire s'applique déjà. Les rails posés par un joueur sans guilde, ou avant cette règle, restent cassables par tous (`rail.protection.enabled`).

#### 📦 Train de fret

Réservé aux officiers, **Âge 2 minimum**. **Maj + clic** sur une destination du menu `/station` ouvre un **double coffre (54 emplacements)**. Fermez-le, puis cliquez sur **[+ COFFRE]** pour en charger un autre ou sur **[ENVOYER]** dans le chat (frais : tarif d'arrivée ×5 par coffre rempli).

| Âge de la guilde | 2 | 3 | 4 | 5 | 6 |
| :--- | :-: | :-: | :-: | :-: | :-: |
| Doubles coffres par convoi | 1 | 2 | 3 | 5 | 10 |

Un **wagonnet à coffre** part tout seul et suit le tracé des rails, **lentement** : **4 min** sous 1 000 blocs, **8 min** jusqu'à 3 000, **12 min** jusqu'à 5 000, **15 min** jusqu'à 10 000, **20 min** au-delà. Ce n'est pas un vrai wagonnet : sa position est calculée à partir de l'heure, il n'est visible que près des joueurs et survit aux redémarrages.

À l'arrivée, les objets utiles à l'Âge suivant sont **donnés automatiquement** si la gare est à vous ; le reste est déposé dans la **boîte de la gare d'arrivée, ouverte à tous les joueurs présents** (`/station fret recuperer [numéro de coffre]`, jusqu'à 30 coffres) : le fret sert aussi d'**échange entre guildes** (la guilde de la gare est prévenue de l'arrivée). Deux convois par guilde au plus.

**🏹 Attaquer un convoi** : quand un convoi traverse le territoire d'une guilde **sans traité** avec l'expéditeur, ses membres le frappent (**25 coups**) pour le **piller** : le butin tombe sur les rails. Les deux guildes sont prévenues à chaque frontière franchie. Succès *Pilleur de convois* et *Roi du fret*.

### ✈️ Aéroports et élytres

L'élytre n'est plus un moyen de traverser la carte à volonté : pour planer, il faut **partir d'une piste d'aéroport**.

- **Âge 4 minimum** : tant que ta guilde n'a pas atteint l'Âge 4 (`airports.min-age`), l'élytre refuse de s'ouvrir.
- **Construire une piste** : une bande d'**au moins 32 × 3 blocs de béton noir**, au même niveau, dans le sens que tu veux. Aucun panneau ni commande : la piste est reconnue d'elle-même quand quelqu'un tente de décoller. N'importe qui peut en construire, n'importe où (territoire de guilde ou terrain neutre).
- **Décoller** : saute depuis la piste, puis ouvre l'élytre (double saut) à moins de 4 blocs au-dessus d'elle. Partout ailleurs (falaise, chute, montagne, bateau), l'élytre ne s'ouvre pas.
- **Se poser** : sur une piste. Un atterrissage hors piste endommage l'élytre (100 points de durabilité, `airports.bad-landing-damage`) ; il faut ensuite rejoindre une piste à pied, à cheval ou en train pour redécoller.
- **Exceptions** : l'**End** et le **monde de ressources** ne sont pas concernés (`airports.free-in-end`, `airports.free-in-resource-world`). Les opérateurs ne sont pas dispensés.

#### 🐎 Chevaux

Pour que la route et le cheval valent mieux que l'élytre, toute monture de selle (cheval, âne, mulet, lama, chameau) reçoit **Vitesse I** sous son cavalier (`mounts.speed-amplifier`). Les membres d'une guilde qui a une **Écurie** montent encore plus vite (**Vitesse II**, `mounts.stable-speed-amplifier`) et leurs montures se soignent.

---

## 11. La vie du serveur

### Événements (un tous les 1 à 2 jours)

| Événement | Durée | Ce qui se passe |
| :--- | :--- | :--- |
| 🌑 Lune de sang | 10 min | Plus de monstres, primes ×2 |
| ☄ Pluie de météorites | 3 min | Des blocs de minerai tombent du ciel près des joueurs (sur le terrain naturel, jamais sur une construction) |
| 🧙 Marché noir | 5 min | Un marchand propose 3 objets rares tirés au hasard, de **32 à 128 diamants** chacun. **Chaque offre est verrouillée après un seul achat** et chaque joueur ne peut acheter qu'**une seule chose** |
| ☠ Boss mondial | 15 min | Un **boss différent chaque semaine** (voir ci-dessous) : 1 000 pièces partagées selon les dégâts (au moins 10 % des dégâts, 400 pièces max par joueur), et pour le meilleur 3 diamants + 20 % de chance d'une Étoile du Nether |

### ☠ Le boss mondial en détail

- **Un boss par semaine** : *Seigneur des Cendres* (forge, +25 % de dégâts des **Nains**), *Gardien de la Forêt* (bête sylvestre, +25 % des **Rôdeurs**) ou *Reine des Glaces* (+25 % des **Forgerons**).
- **Compte à rebours** : **2 minutes** avant l'éveil, un titre plein écran et un **cor** préviennent tout le monde ; le message donne la **distance et la direction** depuis votre position. Rappel 30 s avant.
- **Difficulté adaptée** : PV = **300 par joueur** à moins de 150 blocs au moment de l'éveil (400 à 1 000 PV), dégâts de 8 + 2 par joueur.
- **Trois phases** (barre de couleur différente) : à **75 %** il invoque des **gardes spectraux**, à **50 %** il lance une **onde de choc** (cercle de 11 blocs qui projette), à **25 %** il **s'enrage** (plus rapide, plus fort, attaques plus fréquentes).
- **Attaques télégraphiées** : avant chaque coup de zone, un **cercle de particules** apparaît au sol pendant **3 secondes** : sortez-en !
- **Mode héroïque** : dès la première victoire du serveur, le boss est héroïque une fois sur deux (PV ×1,5, dégâts ×1,3, cagnotte ×1,5, plus de diamants) ; succès *Légende héroïque*.
- **Bonus de guilde** : la guilde du meilleur combattant reçoit **500 pièces** dans sa banque, et son nom est gravé au **Panthéon** du site.

### ⚖ Tribunal des guildes (`/vote`)

Chaque **lundi**, les membres de guilde votent avec `/vote` (liste cliquable) une **règle pour la semaine suivante** (du mardi au lundi). Chaque guilde établie (Nexus actif, 2 membres et 3 jours d'existence au moins) pèse une voix (son choix = l'option la plus votée par ses membres), l'égalité se départage au nombre de votants. Le verdict est annoncé et inscrit au journal.

| Règle | Effet |
| :--- | :--- |
| Taxe de passage | Péages ferroviaires ×2 |
| Bonus de construction | Terres -25 % |
| Marché libre | Aucune taxe à l'Hôtel des Ventes |
| Chasse ouverte | Primes de monstres +25 % |
| Trêve prolongée | La trêve commence aussi le jeudi et dure deux fois plus longtemps |

### 🕊 Trêve hebdomadaire

Le **vendredi et le samedi à 21 h**, **une heure sans PvP** (coups, flèches, potions néfastes) pour tout le monde, **et aucune attaque contre un Nexus, aucune horde lancée, aucun convoi pillé** ; les duels d'honneur en sont exemptés. Un siège dont l'heure tombe pendant une trêve attend sa fin.

### Saisons de guildes

Chaque mois, `/classement` classe les guildes (territoire, Âge, richesse, membres, raids repoussés, conquêtes). La guilde championne gagne **5 000 pièces**, **500 par membre présent depuis au moins 7 jours** et une couronne 👑 dans le tchat. `/histoire` raconte les derniers grands moments du serveur.

### 🍂 Les saisons naturelles (`/climat`)

Le monde change de saison **toutes les deux semaines** :

| Saison | Effet |
| :--- | :--- |
| 🌸 Printemps | Les cultures poussent plus vite (+50 %) |
| ☀ Été | +20 % d'expérience |
| 🍂 Automne | Une chance sur deux d'avoir une récolte bonus |
| ❄ Hiver | Les cultures poussent deux fois moins vite, primes de monstres +25 % |

### 🍺 Tavernes (`/taverne`)

Le **Syndicat Marchand** et l'**Enclave Champêtre** peuvent déclarer jusqu'à 2 chunks de leur territoire (jamais celui du Nexus) **zones neutres** avec `/taverne declarer` (`/taverne retirer`). Le chunk doit contenir **un coffre vide, un tonneau (barrel) et un toit** au-dessus de l'un et de l'autre. Dedans, le **PvP, les armes de tir et les potions offensives sont désactivés**, sauf entre deux joueurs **déjà en combat** : une taverne n'est pas un refuge pour fuir un affrontement.

`/taverne` ouvre le menu des jeux, tous joués contre la **banque de la guilde propriétaire** (la mise maximale s'adapte à ce que la banque peut payer ; 1 000 pièces au plus) :

| Jeu | Règle |
| :--- | :--- |
| 🪙 **Pile ou face** | 47 % de chances de doubler sa mise |
| 🍻 **Le bar** | Un verre (50 pièces, ou 5 / 10 d'un coup) donne un tirage : **0,0001 %** pomme dorée enchantée, **1 %** pomme dorée, **12 %** 8 steaks, **12 %** 16 pommes de terre cuites, **50 %** une *pinte de Kastell rouge* (nausée, 30 s), le reste une soupe suspecte aléatoire |
| 🎡 **Roulette** | Européenne (0 à 36) : rouge/noir, pair/impair, manque/passe ×2, douzaines ×3, numéro plein ×36 ; le 0 fait gagner la maison |
| 🎰 **Machine à sous** | Trois rouleaux : 7-7-7 ×50, diamants ×20, cloches ×10, citrons ×6, cerises ×4 ; deux cerises ×1,5 ; une cerise ×0,5 |
| 🎲 **Dés** | Deux dés contre deux dés de la maison : total plus haut ×2, égalité : moitié de la mise rendue |
| 💪 **Bras de fer** | Jeu d'adresse contre le champion de la taverne : cliquez sur le poing dès qu'il apparaît (1,3 s au début, 0,6 s au plus court). Un clic réussi le fait plier, un clic raté ou trop lent vous fait reculer, et il pousse aussi de lui-même, de plus en plus souvent. Attention aux **feintes** (bloc rouge ✋) : il ne faut pas les toucher, sinon vous reculez de deux crans. Victoire ×1,8 ; 6 parties par jour |
| 💣 **Greed** | Un multiplicateur part de ×1,00 et grimpe de plus en plus vite : cliquez sur **ENCAISSER** pour empocher mise × multiplicateur avant qu'il **explose**, sinon la mise est perdue. Le point d'explosion est tiré au hasard (4 % des parties explosent d'entrée) ; plafond ×20, encaissé automatiquement. Fermer le menu en pleine partie, c'est abandonner la mise |
| 🐖 **Course de cochons** | Quatre cochons (Rosette, Jambonneau, Truffe, Saucisse) : pariez sur le gagnant, ×3,6 |

Les gains viennent de la banque de la taverne, les pertes y entrent. **Tout le monde peut jouer**, y compris les membres de la guilde propriétaire contre leur propre maison. Succès *Tavernier*.

### ❤ Réputation (`/merci <joueur>`)

Remercie un joueur serviable : il gagne **1 point d'honneur**. Tu peux remercier la même personne **une fois par jour**, **5 personnes par jour au plus**, avec **60 secondes** entre deux remerciements ; un joueur ne peut en recevoir que **10 par jour**. Pas de remerciement entre membres d'une **même guilde** ni entre joueurs qui jouent (ou ont joué ces 30 derniers jours) depuis la **même connexion**, et le compte doit avoir **2 jours** (la réputation pèse sur le cours en bourse). Les points s'affichent dans le **tab** (❤), sur le **profil du site** et dans le classement « Les plus honorables ». `/merci` seul montre ta réputation et le top 3.

### 🎓 Parrainage (`/parrain <joueur>`)

Un joueur avancé (métier choisi, en guilde, 15 h de jeu) peut guider une **recrue** (arrivée depuis moins de 14 jours, 5 recrues au plus). La recrue accepte avec `/parrain accepter`. Le parrain est récompensé quand elle atteint des jalons : **premier métier** (+200 pièces, +50 pour la recrue) et **premier territoire** de sa guilde (+300, +100 pour la recrue). Parrain et recrue ne doivent jamais avoir joué depuis la même connexion. `/parrain` montre l'avancement.

### 🎭 Le déguisement (`/deguisement`)

Pour **750 pièces**, tu changes de **skin et de pseudo** pendant **5 minutes** :

- `/deguisement <guilde>` : tu te fais passer pour un membre de cette guilde (skin d'un de ses membres, pseudonyme inventé, étiquette de guilde dans le tchat et la liste). Ses pièges et son golem t'ignorent.
- `/deguisement <Pseudo>` : tu prends le skin et le pseudo d'un compte Minecraft **étranger au serveur** (`/deguisement Notch`). Les pseudos des joueurs du serveur sont **refusés** (usurpation) : pour imiter quelqu'un de chez nous, utilise sa guilde. Impossible si ce joueur est connecté.
- `/deguisement off` : retire le déguisement (nom et skin d'origine rendus).

Les protections de territoire restent actives. **Infliger des dégâts ou frapper le Nexus te démasque**, et la guilde imitée est prévenue. Un déguisement toutes les 30 minutes ; les pièces ne sont débitées que si le skin a bien été trouvé. Les messages de mort ne révèlent pas ton vrai nom.

### ✨ Les cosmétiques (`/cosmetique`)

Trois emplacements, un cosmétique équipé dans chacun :

- **Auras** : des particules autour de toi.
- **Traînées** : des particules derrière tes pas.
- **Effets de victoire** : une gerbe de particules quand tu tues un joueur d'une autre guilde.

On les obtient de trois façons :

- **Les succès** : chaque succès débloque son propre aura (26).
- **Les quêtes** : des objectifs de jeu qui débloquent aura, traînée ou effet : contrats du jour accomplis (10, 50), monstres éliminés (250, 1 000), trajets en train (20, 25), appels à l'aide (5, 10), hordes repoussées (5, 10, 20), hordes lancées (3), joueurs tués (5, 25, 100).
- **La boutique du serveur** : 15 cosmétiques de **3 000 à 15 000 pièces** (pièces personnelles, débitées une fois : clic pour acheter, deuxième clic pour confirmer). Impossible en plein combat.

Le menu `/cosmetique` a un onglet par emplacement ; les cosmétiques verrouillés indiquent comment les obtenir. `/cosmetique off` retire tout. Il y a aussi des secrets à découvrir…

### 📰 Journal de guerre et succès

`/journal` résume les derniers grands faits (raids, conquêtes, traités, trahisons, Âges, événements). `/succes` liste tes **26 succès** (premier métier, conquérant, traître, maître espion, quatre saisons, superficie de 16 à 2 048 chunks…) et le pourcentage de joueurs qui les ont. Un succès obtenu par moins de 10 % des joueurs est **rare** et annoncé dans le journal.

### Le jeu en anglais

`/english` traduit le jeu **pour toi seul** : messages, barre d'action, menus, tableau de bord et livre `/guide` (réponse `/francais` pour revenir). Le choix est mémorisé. Les noms de guildes et de joueurs ne sont pas traduits. Pas encore traduits : la barre de boss des événements, le tchat et les entrées du journal de guerre.

### Pas de messages privés

Les messages privés sont **désactivés** : `/msg`, `/tell`, `/w`, `/r` et leurs équivalents (y compris `minecraft:msg` ou ceux d'Essentials) sont refusés et n'apparaissent plus dans l'auto-complétion. On parle dans le chat public ou dans celui de sa guilde.

---

## 12. Le site web

Le serveur a un site public : **`http://<adresse-du-serveur>:8080/`**, qui se met à jour tout seul (toutes les 30 secondes). Il n'affiche **aucune coordonnée** : rien qui aide à trouver une base.

| Page | Contenu |
| :--- | :--- |
| Accueil | Joueurs en ligne, top guildes, saison naturelle, raids en cours, journal récent |
| Guildes · fiche de guilde | Classement ; membres, traités, diversité, trophées, courbes d'évolution, Âges franchis, faits marquants |
| Joueurs · profil | En ligne, plus riches, annuaire ; profil de chaque joueur (métier, guilde, statistiques, succès, réputation) |
| Raids | Les hordes en cours : cible, attaquant, monstres restants, durée |
| Journal | Le journal de guerre complet, filtrable par catégorie |
| Diplomatie | Réseau des alliances, pactes, vassalités et rivalités, carte des rivalités |
| Musée | Trophées des guildes, tous les succès avec leur rareté, derniers débloqués |
| Âges | Frise sur un vrai axe de temps et prérequis du prochain Âge de chaque guilde |
| Bourse | Cours des actions des guildes, variation sur 24 h, mini-graphiques |
| Panthéon | Boss terrassés (avec la guilde gravée), sièges, règle du Tribunal, titres de la semaine |
| Votes | Élections hebdomadaires |
| Guide | Toutes les règles, avec une recherche |

Le site existe aussi **en anglais** : bouton **EN** / **FR** en haut à droite, ou `?lang=en` à la fin de l'adresse. Les données sont aussi en JSON : `/api/stats.json`, `/api/journal.json`, `/api/raids.json`.

### 🗳 Élections du site

Chaque semaine, la communauté élit sur le site (`/votes`) le plus **débile**, le plus **gros**, le plus **drôle** et le plus **chiant**.

1. Sur le site, clique « Revendiquer ce pseudo » (sur ton profil ou sur la page des votes) : un **code** (`ESI-XXXX`) s'affiche.
2. **Tape ce code dans le chat du jeu** avec ce compte (le message n'est pas diffusé).
3. Le jeu demande une **confirmation** (`[CONFIRMER]` / `[REFUSER]`, avec l'adresse approximative du demandeur : ne confirme jamais une demande que tu n'as pas faite).
4. Un **cookie** (90 jours) reconnaît ensuite ton navigateur.

Un vote par catégorie, modifiable, jamais pour soi ; comptes de moins de 2 jours exclus, et **un seul compte par connexion et par catégorie**. `/webvote optout` te retire des élections. Les vainqueurs de la semaine sont proclamés dans le chat et affichés au **Panthéon**.

---

## 13. Pour les administrateurs

<details>
<summary><b>Installation, configuration et commandes d'administration</b></summary>

### Prérequis et installation

- **Serveur** : Paper 26.2, **Java 25**. Aucune dépendance à un autre plugin (ni Vault, ni PlaceholderAPI) : l'économie est interne.
- **Compiler** : `./mvnw package` sous Linux/macOS, `.\mvnw.cmd package` sous Windows. Le plugin est créé dans `target/` (`esicraft-plugin-<version>.jar`). Chaque modification poussée sur GitHub est aussi compilée automatiquement (GitHub Actions, le `.jar` est disponible en artefact).
- **Installer** : copier le `.jar` dans le dossier `plugins/` du serveur et redémarrer. Le fichier `config.yml` est créé dans le dossier du plugin.
- **Stockage** : base SQLite dans le dossier du plugin (`storage.type`, `SQLITE` ou `JSON`), sauvegardée toutes les 180 s.
- **`/hub`** envoie le joueur vers le serveur `hub` d'un proxy (Velocity ou BungeeCord).
- **Déguisements derrière Velocity** : activer le transfert d'informations **moderne** (`player-info-forwarding-mode = "modern"` dans Velocity et `velocity-support` dans Paper). Le serveur doit pouvoir joindre `api.mojang.com` et `sessionserver.mojang.com`.

### Permissions

| Permission | Par défaut | Rôle |
| :--- | :--- | :--- |
| `esicraft.user` | tous | Fonctionnalités de jeu |
| `esicraft.admin` | opérateurs | Commandes d'administration |

### Commandes d'administration

| Commande | Usage |
| :--- | :--- |
| `/esicraft reload` | Recharger `config.yml` et la configuration des Âges |
| `/esicraftadmin setage <guilde> <niveau>` | Changer l'Âge d'une guilde |
| `/esicraftadmin spawnraid <guilde> <type>` | Lancer un raid contre une guilde |
| `/esicraftadmin givecoins <joueur> <montant>` | Donner des pièces |
| `/esicraftadmin forcenexus <guilde>` | Réparer un Nexus |
| `/money get\|set\|give\|take <joueur> [montant]` | Gérer l'argent des joueurs |
| `/money top` | Classement des plus riches |
| `/money rectify <facteur>` · `rectify status <joueur>` · `rectify reset [joueur\|all]` | Facteur anti-farm des primes de monstres |
| `/race set <joueur> <race>` | Changer le métier d'un joueur (`DWARF`, `SYLVAN`, `BLACKSMITH`, `RANGER`, `ARCANIST`, `ENGINEER`, `NONE`) |
| `/evenement <lune\|meteores\|marchand\|boss\|stop\|status>` | Lancer ou arrêter un événement |
| `/marchand forcer` · `/marchand partir` | Faire venir ou partir le marchand itinérant |
| `/guild cooldown reset [guilde]` | Réinitialiser les délais de hordes et arrêter les raids (une guilde ou toutes) |
| `/pvp [on\|off\|toggle]` | Activer ou couper la destruction des Nexus |
| `/game reset` puis `/game reset confirm` | Remettre **tout** le jeu à zéro : guildes, Nexus, fortunes (100 pièces par joueur), succès, statistiques, contrats, réputation, parrainages, cosmétiques, journal, courbes, Âges, saison, prêts et site web (seules les préférences de langue sont gardées) |

### Réglages principaux (`config.yml`)

| Section | Contenu |
| :--- | :--- |
| `guilds` | Prix de création et des terres, membres max, vie et soin du Nexus, plafond des officiers |
| `ages` | Ressources demandées pour chaque Âge |
| `raids` | Prix et taille des hordes, durée max, délai entre raids, plafond des hordes sur mesure |
| `nexus` | Rayon de siège, dégâts sans défenseur, déplacement, roulette russe, arbre technologique (`nexus.tech.*`) |
| `siege` | Préavis, durée, frais, multiplicateur de dégâts, butin |
| `races` | Délai entre deux changements de métier |
| `market` | Taxe et annonces max à l'Hôtel des Ventes, commandes d'achat |
| `economy` · `catchup` · `revenge` | Plafond de primes par heure, rattrapage, revanche |
| `events` | Fréquence des événements, boss mondial (`events.boss.*`), marché noir |
| `truce` | Jours, heure et durée de la trêve |
| `court` | Conditions de vote du Tribunal |
| `duels` · `betting` | Mises, durée, arène, paris |
| `taverns` | Mise max et réglage de chaque jeu |
| `resource-world` | Nom, bordure, jour et heure de remise à neuf |
| `guild-merchant` | Heure, durée, offres, commission |
| `rail` | Prix des trajets et des gares, péage max, Âge minimum, fret, vitesse du train (`rail.ride.*`), protection des rails (`rail.protection.*`) |
| `airports` | Aéroports : Âge minimum, taille d'une piste, dégâts d'un atterrissage hors piste, zones libres (End, monde de ressources) |
| `mounts` | Vitesse des chevaux et des montures d'écurie |
| `treaties` · `rivalry` | Durée des pactes, tribut, indemnité, préavis, protection entre alliés, demi-vie des rivalités |
| `loans` · `ransom` | Taux, délais, saisies · durée et montants des rançons |
| `bourse` | Cours, volatilité, dividendes, pondération de la valeur (`bourse.weights.*`) |
| `reputation` · `mentoring` · `cosmetics` | Limites de `/merci`, récompenses du parrainage, prix des cosmétiques |
| `combat` | Durée du marquage PvP, commandes bloquées |
| `chat` | Commandes de messages privés bloquées |
| `anti-alt` | Détection des comptes d'une même connexion |
| `web` | Site web : port, nom affiché, informations visibles, anti-flood, élections |
| `life` | Saisons naturelles, déguisement, appel à l'aide |
| `tutorial` | Activation du tutoriel |
| `admin` | Opérateurs et délais d'attente |

### Le site web (côté serveur)

- Activé par défaut sur le **port 8080** (`web.port`). Sous Linux, un port inférieur à 1024 demande les droits root ou `setcap 'cap_net_bind_service=+ep' $(readlink -f $(which java))` ; sinon, utiliser 8080. Si le port est refusé, le plugin démarre quand même et le signale dans la console.
- Penser à **ouvrir le port** sur la box et le pare-feu.
- Lecture seule et sans coordonnées. Les PV du Nexus sont masqués par défaut (`web.show-nexus-health`), ainsi que le nombre de défenseurs d'un raid (`web.show-raid-defenders`). Banques et soldes peuvent être cachés (`web.show-guild-banks`, `web.show-balances`). Les joueurs en vanish n'apparaissent pas.
- Protection anti-flood : 60 requêtes par 10 secondes et par adresse IP (`web.rate-limit`). Derrière un proxy inverse, activer `web.trust-proxy-header` et augmenter cette limite.
- Les élections peuvent être désactivées ou leurs catégories renommées (`web.votes.*`).

### Opérateurs et délais d'attente

Les **opérateurs** (`op`) ne sont soumis à **aucun délai d'attente** : déguisement (30 min), appel à l'aide (10 min), protection d'une guilde après un raid, soin du Nexus (30 s et blocage après une attaque), changement de métier (24 h), changement de spécialisation (7 jours), recherche de lignes de train, et délai anti-farm des primes PvP. Réglage : `admin.ops-bypass-cooldowns` (`true` par défaut).

### Anti-triche intégré

- Les bonus de minage ne s'appliquent jamais sur un bloc posé par un joueur, ni sur les terres d'une autre guilde.
- Les quantités demandées pour les Âges se basent sur le maximum de membres atteint : expulser des joueurs avant de donner ne sert à rien.
- Les enchantements au-delà du niveau normal sont retirés hors du Conclave Arcanique.
- Taille des hordes limitée, banque débitée seulement une fois la cible validée, monstres de raid supprimés au redémarrage.
- Écritures d'argent sérialisées (pas de perte entre une vente et une déconnexion).
- Détection des comptes multiples (parrainage, `/merci`, paris, statistiques PvP) : `anti-alt.*`.

</details>
