# ⚔️ Esicraft — Guide du joueur

🇬🇧 [English version](README.en.md) · 📝 [Historique des changements](CHANGELOG.md)

**Esicraft** est un serveur Minecraft de survie entre amis, tourné vers la stratégie. Chaque joueur choisit un **métier**, rejoint une **guilde**, revendique des **terres** et protège le **Nexus**, le cœur de sa base. Les guildes progressent ensemble à travers **6 Âges**, commercent, signent des **traités**… ou s'envoient des **hordes de monstres** pour détruire le Nexus d'en face.

---

## 📑 Sommaire

1. [Bien démarrer en 5 étapes](#-1-bien-démarrer-en-5-étapes)
2. [Les métiers](#-2-les-métiers)
3. [Les guildes et leur territoire](#️-3-les-guildes-et-leur-territoire)
4. [Les spécialisations de guilde](#-4-les-spécialisations-de-guilde)
5. [Le Nexus et ses pièges](#-5-le-nexus-et-ses-pièges)
6. [Les raids (hordes de monstres)](#️-6-les-raids-hordes-de-monstres)
7. [Les Âges](#-7-les-âges)
8. [L'argent](#-8-largent)
9. [Diplomatie et transports](#-9-diplomatie-et-transports)
10. [La vie du serveur](#-10-la-vie-du-serveur)
11. [Toutes les commandes](#-11-toutes-les-commandes)
12. [Pour les administrateurs](#️-12-pour-les-administrateurs)

---

## 🚀 1. Bien démarrer en 5 étapes

1. **Choisis ton métier** avec `/race`. Tant que tu n'en as pas, tu ne peux casser aucun bloc.
2. **Rejoins une guilde** (accepte une invitation avec `/guild join <nom>`) ou **crée la tienne** avec `/guild create <nom> <tag>` (coût : 500 pièces).
3. **Revendique ton premier terrain** avec `/guild claim`, puis **pose le Nexus** avec `/guild nexus place`.
4. **Gagne de l'argent** : tue des monstres, remplis tes contrats du jour (`/contrat`), vends à l'Hôtel des Ventes (`/ah`).
5. **Fais avancer ta guilde** en donnant des ressources dans `/age` pour débloquer de nouveaux équipements et dimensions.

> [!TIP]
> Tape `/guide` pour recevoir le livre de règles en jeu (30 pages : sommaire cliquable, métiers, guildes, Nexus, raids, Âges, argent, traités, train, événements, commandes), et `/hud` pour afficher ou masquer le tableau de bord à droite de l'écran.

---

## 🎭 2. Les métiers

Chaque métier a des **objets qu'il est seul à pouvoir fabriquer**, des **avantages** et un **défaut**. Ces exclusivités disparaissent au fil des Âges : un équipement réservé à un métier devient accessible à tous plus tard.

| Métier | Spécialité | Avantages | Défaut |
| :--- | :--- | :--- | :--- |
| ⛏ **Nain** (mineur) | Pioches et pelles | Hâte II et Vision nocturne sous la couche 0 · 15 % de chances de doubler un minerai | Lenteur I en plein soleil (au-dessus de la couche 60) · ne fabrique ni armes ni armures en métal |
| 🌿 **Sylvain** (fermier) | Houes, poudre d'os, élevage, pommes dorées | Régénération I pour lui et les membres de sa guilde à moins de 4 blocs · récolte d'un clic droit avec replantation automatique | -20 % de dégâts au corps à corps |
| 🔨 **Forgeron** | Armures, épées, haches, boucliers, enclumes, meules, hauts-fourneaux | Résistance au feu permanente · 35 % de chances de doubler un lingot au four · enclume 2× moins chère en XP | Il a faim 25 % plus vite |
| 🏹 **Rôdeur** (archer) | Arcs, arbalètes et flèches (jusqu'à l'Âge 3) | Ses flèches empoisonnent · +25 % de dégâts à distance · Vitesse I dans les plaines, forêts, jungles et taïgas | Lenteur I avec un plastron en diamant ou Netherite |
| 🧪 **Arcaniste** (mage) | Enchantements, potions jetables, alambics (jusqu'à l'Âge 3) | +30 % d'XP · potions qui durent 25 % plus longtemps · -20 % de dégâts magiques subis | +15 % de dégâts subis au corps à corps |
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

**Changer de métier** : `/race` ouvre le menu. Le premier choix et les 3 changements suivants sont gratuits, ensuite chaque changement coûte 10 000 pièces. Il faut attendre 24 h entre deux changements.

`/race show` affiche l'annuaire des joueurs connectés (métier, guilde, Âge, argent).

---

## 🏛️ 3. Les guildes et leur territoire

### Les rôles

| Rôle | Ce qu'il peut faire |
| :--- | :--- |
| 👑 **Chef** | Tout, plus : nommer des officiers, céder la guilde (`/guild transfer`), la dissoudre (`/guild disband`) |
| 🛡 **Officier** | Inviter, expulser, revendiquer des terres, poser le Nexus, acheter des pièges, lancer des raids, signer des traités, gérer les gares |
| 🙂 **Membre** | Déposer de l'argent dans la banque, donner des ressources pour l'Âge, défendre |

- **10 joueurs maximum** par guilde, et on ne peut être que dans **une seule guilde**.
- Les invitations expirent après **5 minutes**.
- On ne peut pas dissoudre sa guilde pendant un raid, ni dans les 10 minutes qui suivent une attaque du Nexus.

### Revendiquer des terres

Le territoire se compte en **chunks** (carrés de 16 × 16 blocs). Avec `/guild claim`, un officier revendique le chunk où il se trouve.

- **Tous les chunks doivent se toucher.** Seul le premier peut être placé n'importe où. On ne peut pas non plus abandonner un chunk (`/guild unclaim`) si ça coupe le territoire en deux, ni celui du Nexus.
- **Prix** : payé par la banque de guilde. Le premier coûte 100 pièces, puis chaque nouveau chunk coûte 25 pièces de plus que le précédent (100, 125, 150, 175…).
- **Taille maximale selon l'Âge** :

| Âge | 1 | 2 | 3 | 4 | 5 et 6 |
| :--- | :--- | :--- | :--- | :--- | :--- |
| Chunks max | 16 | 32 | 64 | 128 | illimité |

`/guild claim show` affiche les bordures de ton territoire.

### Ce qui est protégé chez une guilde

Sur les terres d'une autre guilde, tu ne peux **ni casser ni poser de blocs**, ni ouvrir ses coffres, fours, barils, shulkers ou entonnoirs.

Y sont aussi interdits : la **TNT** (y compris en wagonnet), les **perles de l'Ender** et le **fruit de Chorus** pour s'y téléporter, les **cristaux de l'End**, les **pistons**, la **lave, l'eau et le feu** qui viennent de l'extérieur, la destruction de blocs par les Withers, Endermen et Ravageurs, et les **projectiles** sur les animaux, cadres et supports d'armure.

Ces protections sautent seulement pendant un raid contre la guilde, et pendant **30 minutes** après la destruction de son Nexus s'il n'y a pas d'annexion.

---

## 🌟 4. Les spécialisations de guilde

Le chef choisit une spécialisation dans `/guild spec`. Elle donne des bonus à toute la guilde, mais impose des règles sur sa composition, vérifiées à chaque arrivée d'un joueur et à chaque changement de métier.

| Spécialisation | Bonus | Contraintes |
| :--- | :--- | :--- |
| ⚔️ **Légion Guerrière** (combat) | Force I et Résistance I sur vos terres · raids 30 % moins chers · +30 % de dégâts contre les monstres | Au moins 50 % de Rôdeurs et Forgerons · 1 Nain et 1 Sylvain maximum · -25 % de minerais et récoltes sur vos terres |
| ⛏️ **Industrie Souterraine** (minage) | Hâte II sur vos terres sous la couche 30 · minerais bruts ×1,5 · pièges du Nexus 50 % moins chers | 1 Rôdeur maximum · potions de niveau II interdites · +20 % de dégâts magiques subis |
| 🌾 **Enclave Champêtre** (nature) | +2 cœurs de vie · Régénération I et Saturation sur vos terres · récoltes, butin des animaux, bois et pierres ×2 · animaux qui se reproduisent 3× plus vite | Aucun Ingénieur · 1 Forgeron maximum · armures en Netherite interdites |
| 💰 **Syndicat Marchand** (commerce) | Seule guilde à pouvoir commercer avec les villageois avant l'Âge 4 · +20 % sur les primes de monstres (dont 10 % versés à la banque) · aucune taxe à l'Hôtel des Ventes | Aucun Arcaniste · raids 30 % plus chers · pas de bonus de combat · Nexus -20 % de vie |
| 🔮 **Conclave Arcanique** (magie) | Nexus +25 % de vie · enchantements et enclume 2× moins chers en XP · potions d'un niveau au-dessus · enchantements boostés (IV devient V, V devient VI) · le Nexus tire un rayon sur l'attaquant le plus proche (1 cœur toutes les 2 s) · -50 % de dégâts magiques | -2 cœurs de vie · terres 50 % plus chères · 1 Forgeron et 1 Nain maximum · les enchantements boostés sont perdus si on change de spécialisation |
| 🏹 **Rôdeur Solitaire** (seul) | Vitesse I permanente · armures en Netherite et enchantements dès l'Âge 3 | Un seul membre, Rôdeur, qui n'a jamais été dans une autre guilde · aucun coffre sur ses terres · Nexus limité à 2 500 PV |

> [!IMPORTANT]
> Le premier choix est immédiat. Ensuite, on ne peut changer de spécialisation **que lorsqu'un nouveau joueur** (jamais passé par la guilde) la rejoint.

---

## 💎 5. Le Nexus et ses pièges

Le **Nexus** est le cœur de ta guilde. On le pose dans son territoire avec `/guild nexus place` (une seule fois : impossible de le reposer pour remettre sa vie à 100 %). **S'il est détruit, ta guilde perd tout** (voir [les raids](#️-6-les-raids-hordes-de-monstres)).

**Zone de sécurité de 16 blocs** autour du Nexus : pas d'eau, de lave ni d'obsidienne (y compris pleureuse). Le Nexus ne peut pas être posé s'il y en a déjà, et on ne peut pas en ajouter ensuite. Ça empêche les douves et les murs incassables.

**Le soigner** : un membre jette une **pomme dorée** dessus (ou fait un clic droit avec) : +500 PV, ou +2 500 PV avec une pomme dorée enchantée. Il faut attendre 30 secondes entre deux soins, et c'est impossible pendant un raid.

### Les pièges (`/nexus traps`)

Payés par la banque de guilde, ils restent armés (même après un redémarrage) jusqu'à la **fin de la prochaine horde** : il faut ensuite les racheter. Ils visent les monstres et les **intrus** présents sur vos terres, jamais les membres, les alliés (traité), ni les joueurs en créatif ou spectateur.

| Piège | Prix | Effet |
| :--- | :--- | :--- |
| 🔥 Brasier Infernal | 250 | À moins de 8 blocs : 1 cœur de dégâts toutes les 2 s et brûlure |
| 🕸️ Toile d'Entravement | 200 | Lenteur II à moins de 10 blocs |
| ⚡ Foudre de Riposte | 350 | Foudroie quiconque frappe le Nexus : 4 cœurs, au plus un éclair toutes les 5 s par joueur |
| 🧪 Brume Toxique | 300 | Poison I à moins de 8 blocs (le poison ne tue jamais, il affaiblit) |
| 🤖 Automate Gardien | 500 | Un golem de fer attaque les ennemis ; il est reconstruit 5 min après sa mort |

---

## ⚔️ 6. Les raids (hordes de monstres)

Un raid, c'est **payer une armée de monstres** pour qu'elle attaque le Nexus d'une autre guilde.

### Lancer un raid

1. Ouvre `/ah` et clique sur le **crâne de Wither** (en bas du menu).
2. Choisis une **horde toute faite** (zombies cuirassés, squelettes incendiaires, araignées, pillards et ravageurs) ou **compose la tienne** (jusqu'à 60 monstres).
3. Choisis la guilde cible. Le prix est payé par **ta banque de guilde**.

Options possibles : des **sapeurs** qui font exploser les murs et assèchent l'eau et la lave autour d'eux, et l'**ignifugation** pour que tes monstres traversent la lave.

### Conditions

- Seuls les **officiers et le chef** peuvent lancer un raid.
- **Au moins un défenseur doit être connecté et sur ses terres**, sinon le raid est refusé.
- Après un raid, la guilde visée est **protégée 30 minutes**.
- Impossible d'attaquer une guilde avec qui on a un **traité** (voir [Diplomatie](#-9-diplomatie-et-transports)).

### Si le Nexus tombe

- Le **territoire** du perdant passe au gagnant (dans la limite de son Âge).
- Toute la **banque** du perdant passe au gagnant.
- Les membres du perdant choisissent : rejoindre le vainqueur (`/guild annex join`) ou partir (`/guild annex decline`).

### ⭐ Les guildes trop riches sont des cibles

Une guilde passe en **Hégémonie économique** si au moins la moitié de ses membres sont dans le top 5 des plus riches (`/baltop`), ou si sa banque détient plus de 30 % de tout l'argent du serveur. Contre elle, les raids coûtent **2× moins cher** et sa protection après un raid ne dure que **2 minutes**. Elle est marquée d'une ⭐ dans le menu des cibles.

---

## ⏳ 7. Les Âges

Ta guilde commence à l'**Âge 1** et peut monter jusqu'à l'**Âge 6**. Pour passer à l'Âge suivant, les membres **donnent des ressources** dans le menu `/age`. Chaque Âge débloque des équipements et agrandit le territoire possible.

| Âge | Ce qu'il débloque |
| :--- | :--- |
| 1. **Rustique** (départ) | Bois et pierre. Le fer est réservé aux métiers spécialisés. Nether et End fermés. |
| 2. **Métallurgie** | **Le Nether**, les outils en fer et les boucliers pour tous. Le diamant est réservé aux métiers spécialisés. |
| 3. **Alchimie** | **L'End**, les outils en diamant et l'armure en fer pour tous. La Netherite est réservée aux métiers spécialisés. |
| 4. **Guerre Totale** | Pour tous : armures en diamant, commerce avec les villageois, TNT, redstone, pistons, alambics, arcs et arbalètes. **Bonus des joueurs** : Vitesse I, +10 % de dégâts, +25 % d'XP, -20 % de dégâts d'explosion. |
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

## 💰 8. L'argent

### Gagner des pièces

- **Tuer des monstres** : chaque monstre rapporte une prime, de 1,50 pièce (poisson d'argent) à 750 pièces (Ender Dragon). Les fermes à monstres répétitives rapportent moins.
- **Contrats du jour** (`/contrat`) : 3 missions par jour, dont une liée à ton métier. +400 pièces si tu fais les trois.
- **Hôtel des Ventes** (`/ah`) : vends l'objet que tu tiens avec `/ah sell <prix>` (10 annonces maximum). Clique sur ta propre annonce pour la retirer.
- **Événements** et **boss mondial** (voir [La vie du serveur](#-10-la-vie-du-serveur)).

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

### Tuer un joueur

Quand tu tues un joueur d'une autre guilde, tu lui **prends 100 pièces + 1 % de sa fortune** (ou tout ce qu'il a s'il a 100 pièces ou moins). Pas de prime si vous vous êtes déjà affrontés il y a moins de 2 minutes, ni entre membres d'une même guilde.

### Dépenser

Banque de guilde (`/guild bank <montant>`) pour les terres, les pièges et les raids · virements (`/pay`) · achats à l'Hôtel des Ventes · trajets en train · changements de métier.

---

## 🤝 9. Diplomatie et transports

### Les traités (`/traite`)

Un officier propose un traité à une autre guilde, qui a 10 minutes pour accepter (clic gauche) ou refuser (clic droit) dans son menu `/traite`.

| Traité | Durée | Effets |
| :--- | :--- | :--- |
| 🤝 **Alliance** | Illimitée | Pas de raid entre vous · trains gratuits chez l'autre · vous ne pouvez pas vous blesser (coups, flèches, potions) |
| 📜 **Pacte de non-agression** | 7 jours | Pas de raid entre vous |
| 👑 **Vassalité** | Illimitée | Comme une alliance, mais le vassal verse chaque jour 5 % de sa banque à son suzerain |

**Rompre un traité, c'est une trahison** : ta guilde verse jusqu'à 500 pièces d'indemnité à l'autre, tout le serveur est prévenu, et tu ne peux pas l'attaquer pendant 30 minutes. Seule exception : un suzerain peut libérer son vassal gratuitement.

### Le train (`/station`, `/rail`)

Le réseau ferroviaire permet de **se téléporter de gare en gare**, à condition qu'une **ligne de rails continue** les relie (embranchements et croisements compris).

- **Voyager** : près d'une gare (panneau `[Gare]`), tape `/station` et choisis ta destination.
- **Prix** : 1 pièce pour la guilde de la gare d'arrivée, plus le **péage** de chaque guilde dont tu traverses les terres. Ta propre guilde et ses alliés ne font pas payer.
- **Gérer son réseau** (`/rail`, officiers) : fixer son péage, et pour chaque autre guilde choisir **Normale**, **Alliée** (gratuit) ou **Bannie** (les lignes qui passent chez vous lui sont coupées).
- **Construire une gare** : un officier pose un panneau avec `[Gare]` sur la 1re ligne et le nom de la gare sur la 2e, à moins de 2 blocs d'un rail. Il faut être à l'Âge 2, payer 500 pièces avec la banque, et pas plus de 6 gares par guilde.

---

## 🎉 10. La vie du serveur

### Événements (un tous les 1 à 2 jours)

| Événement | Durée | Ce qui se passe |
| :--- | :--- | :--- |
| 🌑 Lune de sang | 10 min | Plus de monstres, primes ×2 |
| ☄ Pluie de météorites | 3 min | Des blocs de minerai tombent du ciel près des joueurs |
| 🧙 Marché noir | 5 min | Un marchand vend des objets rares contre des diamants, à tout le monde |
| ☠ Boss mondial | 15 min | Le *Seigneur des Cendres* : 3 000 pièces partagées selon les dégâts, bonus pour le meilleur |

### Saisons

Chaque mois, `/classement` classe les guildes (territoire, Âge, richesse, membres, raids repoussés, conquêtes). La guilde championne gagne **5 000 pièces**, **500 par membre** et une couronne 👑 dans le tchat. `/histoire` raconte les derniers grands moments du serveur.

### Le site web

Le serveur a un site public : **`http://<adresse-du-serveur>/`**. Il montre les joueurs connectés, le classement des guildes, la fiche de chaque guilde (membres, traités, statistiques) et les plus riches. Il n'affiche **aucune coordonnée** : rien qui aide à trouver une base.

---

## 📋 11. Toutes les commandes

| Commande | À quoi elle sert |
| :--- | :--- |
| `/race` · `/race show` | Choisir son métier · voir les joueurs connectés |
| `/guide` | Livre de règles en jeu (s'ouvre directement, sommaire cliquable) |
| `/guild create <nom> <tag>` | Créer une guilde |
| `/guild invite <joueur>` · `/guild join <nom>` | Inviter · accepter une invitation |
| `/guild leave` · `/guild kick <joueur>` | Partir · expulser |
| `/guild promote <joueur>` · `/guild demote <joueur>` | Nommer ou retirer un officier (chef) |
| `/guild transfer <joueur>` · `/guild disband` | Céder la guilde · la dissoudre (chef) |
| `/guild info [nom]` | Fiche d'une guilde |
| `/guild claim` · `/guild unclaim` · `/guild claim show` | Prendre · abandonner un chunk · voir les bordures |
| `/guild spec` | Choisir la spécialisation |
| `/guild nexus place` · `/nexus traps` | Poser le Nexus · acheter des pièges |
| `/guild annex <join\|decline>` | Après une défaite : rejoindre le vainqueur ou partir |
| `/guild bank <montant>` | Déposer dans la banque de guilde |
| `/age` | Progression des Âges et dons |
| `/money` · `/baltop` · `/pay <joueur> <montant>` | Son argent · les plus riches · envoyer des pièces |
| `/ah` · `/ah sell <prix>` | Hôtel des Ventes et raids · vendre l'objet en main |
| `/contrat` | Contrats du jour |
| `/traite` · `/traite liste` | Traités entre guildes |
| `/station` · `/rail` | Prendre le train · gérer son réseau (officiers) |
| `/classement` · `/histoire` | Classement de la saison · grands moments |
| `/hud` | Afficher ou masquer le tableau de bord |
| `/hub` | Retourner au hub |

---

## 🛠️ 12. Pour les administrateurs

<details>
<summary><b>Installation, configuration et commandes d'administration</b></summary>

### Installation

- **Serveur** : Paper 1.20.4, Java 17 ou plus.
- **Compiler** : `.\mvnw.cmd package` sous Windows, `./mvnw package` sous Linux/macOS. Le plugin est créé dans `target/`. Chaque modification poussée sur GitHub est aussi compilée automatiquement (GitHub Actions).
- **Installer** : copier le `.jar` dans le dossier `plugins/` du serveur et redémarrer. Les données sont stockées dans une base SQLite dans le dossier du plugin.

### Réglages principaux (`config.yml`)

| Section | Contenu |
| :--- | :--- |
| `guilds` | Prix de création et des terres, membres max, vie et soin du Nexus |
| `ages` | Ressources demandées pour chaque Âge |
| `raids` | Protection après un raid, taille max des hordes sur mesure |
| `races` | Délai entre deux changements de métier |
| `market` | Taxe et annonces max à l'Hôtel des Ventes |
| `events` | Fréquence des événements |
| `rail` | Prix des trajets et des gares, péage max, Âge minimum |
| `treaties` | Durée des pactes, tribut, indemnité, préavis, protection entre alliés |
| `web` | Site web : port, nom affiché, informations visibles, anti-flood |

### Le site web

- Activé par défaut sur le **port 80** (`web.port`). Sous Linux, ce port demande les droits root ou `setcap 'cap_net_bind_service=+ep' $(readlink -f $(which java))` ; sinon, utiliser 8080. Si le port est refusé, le plugin démarre quand même et le signale dans la console.
- Penser à **ouvrir le port** sur la box et le pare-feu.
- Lecture seule et sans coordonnées. Les PV du Nexus sont masqués par défaut (`web.show-nexus-health`). Banques et soldes peuvent être cachés (`web.show-guild-banks`, `web.show-balances`). Les joueurs en vanish n'apparaissent pas.
- Protection anti-flood : 60 requêtes par 10 secondes et par adresse IP (`web.rate-limit`). Derrière un proxy inverse, augmenter cette limite.
- Données JSON : `/api/stats.json`.

### Commandes d'administration (permission `esicraft.admin`, opérateurs par défaut)

| Commande | Usage |
| :--- | :--- |
| `/esicraftadmin setage\|spawnraid\|givecoins\|forcenexus` | Changer l'Âge d'une guilde, lancer un raid, donner des pièces, forcer un Nexus |
| `/money get\|set\|give\|take\|rectify\|top` | Gérer l'argent des joueurs |
| `/evenement <lune\|meteores\|marchand\|boss\|stop\|status>` | Lancer ou arrêter un événement |
| `/pvp [on\|off\|toggle]` | Activer ou couper les raids et la destruction des Nexus |
| `/game reset` | Remettre tout le jeu à zéro |
| `/race set <joueur> <race>` | Changer le métier d'un joueur |

### Anti-triche intégré

- Les bonus de minage ne s'appliquent jamais sur un bloc posé par un joueur, ni sur les terres d'une autre guilde.
- Les quantités demandées pour les Âges se basent sur le maximum de membres atteint : expulser des joueurs avant de donner ne sert à rien.
- Les enchantements au-delà du niveau normal sont retirés hors du Conclave Arcanique.
- Taille des hordes limitée, banque débitée seulement une fois la cible validée, monstres de raid supprimés au redémarrage.
- Écritures d'argent sérialisées (pas de perte entre une vente et une déconnexion).

</details>
