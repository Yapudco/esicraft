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

- **Tous les chunks doivent se toucher** par un **côté** (pas en diagonale : plus de « serpents »). Seul le premier peut être placé n'importe où. Seul le **chef** peut abandonner un chunk (`/guild unclaim`), et pas si ça coupe le territoire en deux, ni celui du Nexus.
- **Tampon anti-encerclement** : un nouveau chunk doit rester à au moins **1 chunk** du territoire d'une guilde sans traité avec la tienne.
- **Plafond des officiers** : un officier dépense au plus **3 000 pièces par jour** de la banque (claims, pièges) ; le chef n'a pas de limite.
- **Prix** : payé par la banque de guilde. Le premier coûte 100 pièces, puis chaque nouveau chunk coûte 25 pièces de plus que le précédent (100, 125, 150, 175…).
- **Taille maximale selon l'Âge** :

| Âge | 1 | 2 | 3 | 4 | 5 et 6 |
| :--- | :--- | :--- | :--- | :--- | :--- |
| Chunks max | 16 | 32 | 64 | 128 | illimité |

`/guild claim show` affiche pendant une minute les **bords extérieurs** des territoires dans un rayon de 2 chunks (vert : le tien, rouge : les autres).

### Ce qui est protégé chez une guilde

Sur les terres d'une autre guilde, tu ne peux **ni casser ni poser de blocs**, ni ouvrir ses coffres, fours, barils, shulkers ou entonnoirs.

Y sont aussi interdits : la **TNT** (y compris en wagonnet), les **perles de l'Ender** et le **fruit de Chorus** pour s'y téléporter, les **cristaux de l'End**, les **pistons**, la **lave, l'eau et le feu** qui viennent de l'extérieur, la destruction de blocs par les Withers, Endermen et Ravageurs, et les **projectiles** sur les animaux, cadres et supports d'armure.

**Lave et eau** : un liquide ne passe jamais d'un territoire à un autre (même pendant un raid), les distributeurs ne peuvent pas en verser chez le voisin, et on ne peut pas vider un seau de **lave** à moins de 5 blocs d'un joueur qui n'est ni de sa guilde ni son allié.

**Entités protégées** : cadres, supports d'armure, minecarts (même à coffre), chevaux, ânes, animaux et laisses ne peuvent être ni frappés, ni montés, ni ouverts par un étranger. Les explosions n'abîment aucun bloc d'un territoire, et les distributeurs ne peuvent y vider ni seau, ni boule de feu, ni TNT venus de l'extérieur.

**Zone de siège** : pour qu'un Nexus reste atteignable, un assaillant peut casser les blocs (hors coffres et conteneurs) à moins de **4 blocs** d'un Nexus vivant, et les membres ne peuvent pas poser de bloc plein à moins de **2 blocs** de lui (pas de Nexus muré).

Les autres protections sautent seulement pendant un raid contre la guilde, et pendant **30 minutes** après la destruction de son Nexus s'il n'y a pas d'annexion.

### 🧩 Le bonus de diversité

Plus ta guilde réunit de **métiers différents**, plus elle est forte (niveau visible dans `/guild info` et sur le site) :

| Métiers différents | Bonus |
| :--- | :--- |
| 3 | Primes de monstres +10 % |
| 4 | Nouvelles terres 10 % moins chères |
| 5 | Pièges du Nexus 15 % moins chers |
| 6 | Récompenses des contrats de guilde ×1,5 |

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
| 🔱 **Atlante** (océan) | **Dans l'eau** : respiration aquatique, vision nocturne, agilité aquatique, grâce du dauphin et Résistance | **Nexus obligatoirement immergé** (au moins un bloc d'eau contre lui ; l'eau est alors autorisée autour) · **Lenteur hors de l'eau** |

> [!IMPORTANT]
> Le premier choix est immédiat. Ensuite, on ne peut changer de spécialisation **que lorsqu'un nouveau joueur** (jamais passé par la guilde) la rejoint.

---

## 💎 5. Le Nexus et ses défenses

Le **Nexus** est le cœur de ta guilde. On le pose dans son territoire avec `/guild nexus place` (une seule fois : impossible de le reposer pour remettre sa vie à 100 %). **S'il est détruit, ta guilde perd tout** (voir [les raids](#️-6-les-raids-hordes-de-monstres)).

**Zone de sécurité de 16 blocs** autour du Nexus : pas d'eau, de lave ni d'obsidienne (y compris pleureuse). Le Nexus ne peut pas être posé s'il y en a déjà, et on ne peut pas en ajouter ensuite. Ça empêche les douves et les murs incassables.

**Points de vie** : les PV du Nexus s'affichent au-dessus de lui (hologramme), y compris après un redémarrage du serveur ou une reconnexion.

**Qui est épargné** : le golem gardien et les pièges ne visent jamais les membres de la guilde (le golem ne se retourne pas contre eux même s'ils le frappent) ni ceux d'une guilde liée par un traité (alliance, pacte de non-agression ou vassalité).

**Le Nexus doit être visible** : un coup n'est compté que si l'assaillant le voit (portée et ligne de vue vérifiées). Le Nexus n'existe que dans l'Overworld.

**Le soigner** : un membre jette une **pomme dorée** dessus (ou fait un clic droit avec) : +500 PV, ou +2 500 PV avec une pomme dorée enchantée. Il faut attendre 30 secondes entre deux soins, et c'est impossible pendant un raid ou dans les 2 minutes qui suivent une attaque de joueurs.

### L'arbre technologique du Nexus (`/nexus`)

Le Nexus gagne de l'**expérience** à chaque horde repoussée (+200) et quand on lui **sacrifie des ressources** (`/nexus sacrifice`, objet en main, à moins de 10 blocs : charbon, fer, or, diamant, émeraude, netherite, perle d'Ender, bâton de blaze… ; plafond de 3 000 XP par jour). Il monte ainsi du niveau 1 au niveau 6 (0, 300, 900, 2 000, 4 500, 9 000 XP).

Au lieu d'acheter des pièges jetables, les officiers **débloquent** les défenses dans `/nexus` (l'arbre technologique) : il faut le niveau requis, la défense « parente » déjà débloquée, des **objets à sacrifier** pris dans l'inventaire, et, pour les défenses « vivantes », avoir **ramené puis tué le mob correspondant dans le chunk du Nexus**. Une défense débloquée est **permanente** (elle n'est plus consommée par les hordes) ; l'Industrie Souterraine paie moitié moins d'objets. Elles visent les monstres et les **intrus**, jamais les membres, les alliés (traité) ni les joueurs en créatif ou spectateur.

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

Tout débloquer rapporte le succès *Archi-technicien*. Les **hordes minuscules ne rapportent rien** : repousser une horde de moins de 12 monstres ne donne ni XP de Nexus, ni points de saison, ni succès. Pendant un raid, seuls les monstres de la horde et le TNT allumé par la guilde attaquante détruisent la base (pas un tiers). Le Nexus doit rester **atteignable** : on ne peut pas le poser sous plus de 3 blocs pleins de roche (le siège ne casse qu'à 4 blocs de lui). Réglages : `nexus.tech.*` (coût, dégâts, XP).

### 🚚 Déplacer le Nexus (`/nexus move`)

Chaque **changement de spécialisation** offre à la guilde le droit de **déplacer son Nexus une fois**, dans les 7 jours : un officier se tient à l'endroit voulu (dans le territoire de la guilde) et tape `/nexus move`. Les règles de pose s'appliquent (zone de sécurité, Atlantes dans l'eau, Nexus atteignable). La vie et les défenses sont conservées ; impossible pendant un raid, un siège ou dans les 5 minutes qui suivent une attaque.

### 🎰 Roulette russe du Nexus (`/nexus gamble`)

Le **chef de guilde**, face au Nexus (moins de 6 blocs), peut tenter un coup de poker (confirmation à retaper dans les 15 s, une fois par **72 h**, impossible pendant un raid) : **50 %** (pile) le Nexus gagne **+1 000 PV max** et **2 000 pièces** vont à la banque ; **50 %** (face) il perd **1 000 PV max** et **1 500 PV** (il survit toujours à 1 PV, PV max jamais sous 1 000), la banque perd **2 000 pièces** (dans la limite de son solde), et une **mini-horde de pillards** surgit sans préavis au pied du Nexus. Succès *Tout pour le tout* en cas de victoire.

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
- **Sans défenseur** connecté et sur ses terres, un raid est encore possible mais le Nexus n'encaisse que **25 %** des dégâts.
- **Durée maximale** : 25 minutes ; au-delà, le raid s'achève sans victoire ni défaite (un monstre piégé ne rend plus une guilde intouchable). Si une part importante de la horde se volatilise sans combat (chunk déchargé…), la victoire n'est pas créditée.
- Après un raid, la guilde visée est **protégée 30 minutes**.
- Impossible d'attaquer une guilde avec qui on a un **traité** (voir [Diplomatie](#-9-diplomatie-et-transports)).

### Si le Nexus tombe

- Le **territoire** du perdant passe au gagnant (dans la limite de son Âge).
- Toute la **banque** du perdant passe au gagnant.
- Les membres du perdant choisissent : rejoindre le vainqueur (`/guild annex join`) ou partir (`/guild annex decline`).

### ⭐ Les guildes trop riches sont des cibles

Une guilde passe en **Hégémonie économique** si au moins la moitié de ses membres sont dans le top 5 des plus riches (`/baltop`), ou si sa **banque plus les pièces de ses membres** détiennent plus de 30 % de tout l'argent du serveur. Contre elle, les raids coûtent **2× moins cher** et sa protection après un raid ne dure que **2 minutes**. Elle est marquée d'une ⭐ dans le menu des cibles.

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

### Le contrat de guilde

Dans `/contrat`, sous tes contrats personnels : chaque jour, ta guilde reçoit **trois objectifs, chacun réservé à un métier** (par exemple : un Nain mine, un Forgeron fond des lingots, un Ingénieur pose de la redstone). Seuls les membres du bon métier font avancer leur objectif. Chaque objectif rapporte **200 pièces** à la banque de guilde, et les trois ensemble **750 de plus**. Les objectifs grandissent avec la taille de la guilde.

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
| 📜 **Pacte de non-agression** | 7 jours | Pas de raid entre vous · **frapper un joueur de l'autre guilde ou son Nexus rompt le pacte** (trahison + rivalité) |
| 👑 **Vassalité** | Illimitée | Comme une alliance, mais le vassal verse chaque jour 5 % de sa banque à son suzerain |

**Rompre un traité, c'est une trahison** : ta guilde verse jusqu'à 500 pièces d'indemnité à l'autre, tout le serveur est prévenu, et tu ne peux pas l'attaquer pendant 30 minutes. Seule exception : un suzerain peut libérer son vassal gratuitement.

**Apaiser une rivalité** : elle s'estompe seule avec le temps (le poids des raids, trahisons et morts est divisé par deux tous les 7 jours, réglable dans `rivalry.half-life-days`) et **disparaît dès que les deux guildes signent un traité** (alliance, pacte ou vassalité).

**Agression sous traité** : si un joueur attaque le Nexus d'une guilde liée par un traité (pacte, alliance ou vassalité, y compris le suzerain contre son vassal), ou frappe un membre d'une guilde sous pacte (coup, flèche), le traité est rompu automatiquement. C'est une trahison (indemnité, préavis de 30 minutes, annonce au serveur) et une **rivalité** s'ouvre entre les deux guildes (visible sur le site, page diplomatie).

### Le train (`/station`, `/rail`)

Le réseau ferroviaire permet de **se téléporter de gare en gare**, à condition qu'une **ligne de rails continue** les relie (embranchements et croisements compris).

- **Voyager** : près d'une gare (panneau `[Gare]`), tape `/station` et choisis ta destination.
- **Prix** : 1 pièce pour la guilde de la gare d'arrivée, plus le **péage** de chaque guilde dont tu traverses les terres. Ta propre guilde et ses alliés ne font pas payer.
- **Gérer son réseau** (`/rail`, officiers) : fixer son péage (**toutes les spécialisations jusqu'à 10 pièces, le Syndicat Marchand jusqu'à 100**), et pour chaque autre guilde choisir **Normale**, **Alliée** (gratuit) ou **Bannie** (les lignes qui passent chez vous lui sont coupées).
- **📦 Train de fret** (Âge 2 minimum, officiers) : **Maj + clic** sur une destination du menu `/station` ouvre un **double coffre (54 emplacements)**. Fermez-le, puis cliquez sur **[+ COFFRE]** pour en charger un autre ou sur **[ENVOYER]** dans le chat (frais : tarif d'arrivée ×5 par coffre rempli). Le nombre de doubles coffres par convoi dépend de l'Âge de la guilde : **Âge 2 : 1 · Âge 3 : 2 · Âge 4 : 3 · Âge 5 : 5 · Âge 6 : 10** : un **wagonnet à coffre** part tout seul et suit le tracé des rails, **lentement** (**4 min** sous 1 000 blocs, **8 min** jusqu'à 3 000, **12 min** jusqu'à 5 000, **15 min** jusqu'à 10 000, **20 min** au-delà). Ce n'est pas un vrai wagonnet : sa position est calculée à partir de l'heure, il n'est visible que près des joueurs et survit aux redémarrages. À l'arrivée, les objets utiles à l'Âge suivant sont **donnés automatiquement** si la gare est à vous ; le reste est déposé dans la **boîte de la gare d'arrivée, ouverte à tous les joueurs présents** (`/station fret recuperer [numéro de coffre]`, jusqu'à 30 coffres) : le fret sert aussi d'**échange entre guildes** (la guilde de la gare est prévenue de l'arrivée). Deux convois par guilde au plus.
- **🏹 Attaquer un convoi** : quand un convoi traverse le territoire d'une guilde **sans traité** avec l'expéditeur, ses membres le frappent (**25 coups**) pour le **piller** : le butin tombe sur les rails. Les deux guildes sont prévenues à chaque frontière franchie. Succès *Pilleur de convois* et *Roi du fret*.
- **Construire une gare** : un officier pose un panneau avec `[Gare]` sur la 1re ligne et le nom de la gare sur la 2e, à moins de 2 blocs d'un rail. Il faut être à l'Âge 2, payer 500 pièces avec la banque, et pas plus de 6 gares par guilde.

---

## 🎉 10. La vie du serveur

### Événements (un tous les 1 à 2 jours)

| Événement | Durée | Ce qui se passe |
| :--- | :--- | :--- |
| 🌑 Lune de sang | 10 min | Plus de monstres, primes ×2 |
| ☄ Pluie de météorites | 3 min | Des blocs de minerai tombent du ciel près des joueurs |
| 🧙 Marché noir | 5 min | Un marchand propose 3 objets rares tirés au hasard, de **32 à 128 diamants** chacun. **Chaque offre est verrouillée après un seul achat** et chaque joueur ne peut acheter qu'**une seule chose** |
| ☠ Boss mondial | 15 min | Un **boss différent chaque semaine** (voir ci-dessous) : 1 000 pièces partagées selon les dégâts (au moins 10 % des dégâts, 400 pièces max par joueur), et pour le meilleur 3 diamants + 20 % de chance d'une Étoile du Nether |

### ☠ Le boss mondial en détail

- **Un boss par semaine** : *Seigneur des Cendres* (forge, +25 % de dégâts des **Nains**), *Gardien de la Forêt* (bête sylvestre, +25 % des **Rôdeurs**) ou *Reine des Glaces* (+25 % des **Forgerons**).
- **Compte à rebours** : **2 minutes** avant l'éveil, un titre plein écran et un **cor** préviennent tout le monde ; le message donne la **distance et la direction** depuis votre position, pas une zone approximative. Rappel 30 s avant.
- **Difficulté adaptée** : PV = **300 par joueur** à moins de 150 blocs au moment de l'éveil (400 à 1 000 PV), dégâts de 8 + 2 par joueur.
- **Trois phases** (barre de couleur différente) : à **75 %** il invoque des **gardes spectraux**, à **50 %** il lance une **onde de choc** (cercle de 11 blocs qui projette), à **25 %** il **s'enrage** (plus rapide, plus fort, attaques plus fréquentes).
- **Attaques télégraphiées** : avant chaque coup de zone, un **cercle de particules** apparaît au sol pendant **3 secondes** : sortez-en !
- **Mode héroïque** : dès la première victoire du serveur, le boss est héroïque une fois sur deux (PV ×1,5, dégâts ×1,3, cagnotte ×1,5, plus de diamants) ; succès *Légende héroïque*.
- **Bonus de guilde** : la guilde du meilleur combattant reçoit **500 pièces** dans sa banque, et son nom est gravé au **Panthéon** du site.
- Réglages : `events.boss.*`.

### ⚖ Tribunal des guildes (`/vote`)

Chaque **lundi**, les membres de guilde votent avec `/vote` (liste cliquable) une **règle pour la semaine suivante** (du mardi au lundi). Chaque guilde établie (Nexus actif, 2 membres et 3 jours d'existence au moins : `court.*`) pèse une voix (son choix = l'option la plus votée par ses membres), l'égalité se départage au nombre de votants. Règles : **Taxe de passage** (péages ferroviaires ×2), **Bonus de construction** (claims -25 %), **Marché libre** (aucune taxe à l'Hôtel des Ventes), **Chasse ouverte** (primes de monstres +25 %), **Trêve prolongée** (voir ci-dessous). Le verdict est annoncé et inscrit au journal.

### 🕊 Trêve hebdomadaire

Le **vendredi et le samedi à 21 h**, **une heure sans PvP** (coups, flèches, potions néfastes) pour tout le monde, **et aucune attaque contre un Nexus, aucune horde lancée, aucun convoi pillé** ; les duels d'honneur en sont exemptés. Un siège dont l'heure tombe pendant une trêve attend sa fin. Réglable dans `truce.*` (jours, heure, durée). Avec la règle « Trêve prolongée », elle commence aussi le jeudi et dure deux fois plus longtemps.

### Saisons

Chaque mois, `/classement` classe les guildes (territoire, Âge, richesse, membres, raids repoussés, conquêtes). La guilde championne gagne **5 000 pièces**, **500 par membre** et une couronne 👑 dans le tchat. `/histoire` raconte les derniers grands moments du serveur.

### 🍂 Les saisons naturelles (`/climat`)

Le monde change de saison **toutes les deux semaines** (réglable) :

| Saison | Effet |
| :--- | :--- |
| 🌸 Printemps | Les cultures poussent plus vite (+50 %) |
| ☀ Été | +20 % d'expérience |
| 🍂 Automne | Une chance sur deux d'avoir une récolte bonus |
| ❄ Hiver | Les cultures poussent deux fois moins vite, primes de monstres +25 % |

### 🆘 L'appel à l'aide (`/aide`)

Pendant un raid ou une attaque de ton Nexus, `/aide` prévient tes **alliés** (alliance ou vassalité). Chaque allié qui rejoint tes terres avant la fin de la horde gagne **250 pièces** si elle est repoussée (6 alliés au plus). Un appel toutes les 10 minutes.

### 🎭 Le déguisement (`/deguisement`)

Pour **750 pièces**, tu changes de **skin et de pseudo** pendant **5 minutes** (le skin est rafraîchi chez tous les joueurs, et rendu à la fin) :

- `/deguisement <guilde>` : tu te fais passer pour un membre de cette guilde (skin d'un de ses membres, pseudonyme inventé, étiquette de guilde dans le tchat et la liste). Ses pièges et son golem t'ignorent.
- `/deguisement <Pseudo>` : tu prends le skin et le pseudo d'un compte Minecraft **étranger au serveur** (`/deguisement Notch`). Les pseudos des joueurs du serveur sont **refusés** (usurpation : annonces, journal et commandes seraient détournés) : pour imiter quelqu'un de chez nous, utilise sa guilde. Impossible si ce joueur est connecté.
- `/deguisement off` : retire le déguisement (nom et skin d'origine rendus).

**Serveur hors ligne derrière Velocity ou BungeeCord** : ça fonctionne. Les skins des joueurs du serveur sont ceux que le proxy transmet à la connexion (activez le transfert d'informations **moderne** : `player-info-forwarding-mode = "modern"` dans Velocity et `velocity-support` dans Paper), et les autres comptes sont cherchés directement auprès de Mojang (le serveur doit pouvoir joindre `api.mojang.com` et `sessionserver.mojang.com`).

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

Le menu `/cosmetique` a un onglet par emplacement ; les cosmétiques verrouillés indiquent comment les obtenir (progression de la quête, prix). `/cosmetique off` retire tout. Il y a aussi des secrets à découvrir…

### 📰 Journal de guerre et succès

*Nouveaux succès difficiles* : **superficie** (16, 32, 64, 128, 256, 512, 1 024 et 2 048 chunks revendiqués), Légende héroïque, Maître d'armes (10 duels), Tout pour le tout, Archi-technicien, Maître du siège, Actionnaire, Roi du fret, Pilleur de convois, Tavernier.

`/journal` résume les derniers grands faits (raids, conquêtes, traités, trahisons, Âges, événements). `/succes` liste tes **26 succès** (premier métier, conquérant, traître, maître espion, quatre saisons…) et le pourcentage de joueurs qui les ont. Un succès obtenu par moins de 10 % des joueurs est **rare** et annoncé dans le journal.

### ⚔ Combat PvP

Frapper un joueur d'une autre guilde (coup, flèche, TNT, loup, potion nocive…) ou être frappé met « en combat » 15 secondes. Pendant ce temps : **`/hub`, `/station`, `/pay`, `/ah`, `/banque`, `/guild bank|home|leave`, `/home`, `/tpa`… sont bloqués**, les téléportations par commande aussi, et **se déconnecter est puni de mort** (kick compris ; seul l'arrêt du serveur est excusé). Dépenser ou transférer ses pièces est aussi impossible (achats, `/pay`, banque, métier, déguisement) ; seul le paiement d'une rançon reste permis.

### 🏦 Banque de guilde : prêts et coffre commun (`/banque`)

Entre deux guildes **liées par un traité** (alliance, pacte ou vassalité) :

- `/banque pret <guilde> <montant>` : un officier propose un prêt (de 100 à 20 000 pièces). L'autre guilde l'accepte avec `/banque accepter <guilde>` (ou `refuser`). Les pièces passent d'une banque à l'autre ; la guilde emprunteuse doit rendre le montant **+ 10 % d'intérêts** sous **5 jours**.
- `/banque dettes` liste les prêts · `/banque rembourser <guilde> [montant]` rembourse (en plusieurs fois possible) · `/banque pardonner <guilde>` efface une dette.
- `/banque coffre <guilde>` ouvre un **coffre commun** de 54 cases partagé par les deux guildes (accessible tant qu'un traité les lie).
- **Une dette impayée pèse sur les territoires** : dès l'échéance dépassée, la guilde ne peut plus revendiquer de chunk, et **un chunk lui est saisi toutes les 24 h** (jamais celui du Nexus, jamais un chunk qui couperait le territoire). Chaque chunk saisi efface 300 pièces de dette.

### ❤ Réputation (`/merci <joueur>`)

Remercie un joueur serviable : il gagne **1 point d'honneur**. Tu peux remercier la même personne **une fois par jour**. Les points s'affichent dans le **tab** (❤), sur le **profil du site** et dans le classement « Les plus honorables » de la page Joueurs. `/merci` seul montre ta réputation et le top 3.

### 🎓 Parrainage (`/parrain <joueur>`)

Un joueur avancé (métier choisi, en guilde, 5 h de jeu) peut guider une **recrue** (arrivée depuis moins de 14 jours, 5 recrues au plus). La recrue accepte avec `/parrain accepter`. Le parrain est récompensé quand elle atteint des jalons : **premier métier** (+200 pièces, +50 pour la recrue) et **premier territoire** de sa guilde (+300, +100 pour la recrue). `/parrain` montre l'avancement.

### ⛓ Rançon

Un coup qui devrait **tuer** un joueur d'une autre guilde le **capture** à la place : il est immobilisé et invulnérable pendant 90 secondes. Son ravisseur choisit :

- `/rancon prix <montant>` fixe la rançon (100 à 2 000 pièces ; par défaut 15 % de la fortune du captif) ;
- le captif paie avec `/rancon payer` : 30 % pour le ravisseur, 70 % pour la banque de sa guilde ;
- `/rancon liberer` le libère gratuitement, `/rancon executer` le tue (prime de mort habituelle).

Sans décision, il est exécuté. Se déconnecter en captivité équivaut à mourir. Après une libération, il ne peut pas être recapturé pendant 10 minutes.

**Cellule de prison** : un officier vise un bloc de son territoire et tape `/rancon cellule` (`/rancon cellule retirer`). Le captif y est alors **téléporté** pendant la rançon (puis ramené à son point de capture). Ses alliés (même guilde ou traité) peuvent le **délivrer** en agissant 15 fois sur le bloc de la cellule (clic droit ou gauche) ; pensez à fermer la cellule !

### 🗡 Duels d'honneur et paris (`/duel`, `/pari`)

- `/duel <joueur> [mise]` provoque un joueur (mise en pièces optionnelle, `/duel accepter` ou `/duel refuser`). Les deux sont **téléportés dans une arène neutre** éclairée au-dessus du monde (une plateforme par duel, 6 en même temps, construite seulement si la zone est vide) ; leur **inventaire, leur vie et leur position sont sauvegardés** puis rendus à la fin ; ils combattent avec le **même équipement fourni** (armure en fer complète, épée et hache en fer, bouclier, arc, 16 flèches, 64 steaks, 1 potion de soin instantané) (même après un plantage, à la reconnexion).
- **Personne ne meurt** : le coup décisif met fin au combat. Le vainqueur remporte la mise de l'adversaire. Un duel **ne déclenche ni rivalité, ni trahison, ni capture, ni marque de combat**, et n'est pas stoppé par la trêve. Maximum 5 minutes (match nul au-delà, mises rendues) ; abandonner = perdre.
- `/pari <camp> <montant>` : au début d'un **raid** ou d'un **duel**, les **spectateurs** (hors des guildes concernées / hors duellistes) ont **2 minutes** pour parier (10 à 1 000 pièces) sur la guilde `attaquante`/`defensive` (ou son nom) ou sur un duelliste. Le camp vainqueur se partage la cagnotte moins **5 %** prélevés par le serveur ; sans pari adverse, ou si le combat est annulé, tout est remboursé. **Anti-triche** : sont écartés les membres des guildes concernées **et celles qui leur sont liées par un traité**, tout joueur dont l'**adresse IP** est celle d'un participant en ligne, et les comptes de moins de 3 jours.

### 🍺 Tavernes (`/taverne`)

Le **Syndicat Marchand** et l'**Enclave Champêtre** peuvent déclarer jusqu'à 2 chunks de leur territoire (jamais celui du Nexus) **zones neutres** avec `/taverne declarer` (`/taverne retirer`). Dedans, le **PvP, les armes de tir et les potions offensives sont désactivés**. `/taverne` ouvre un **pile ou face** (interface à cliquer) : on mise 10 à 1 000 pièces contre la **banque de la guilde propriétaire** (47 % de chances de gagner, mise doublée) ; la banque doit pouvoir couvrir la mise. Succès *Tavernier*.

### 📈 La bourse (`/bourse`)

À partir de l'**Âge 4**, le chef peut **ouvrir le capital** de sa guilde (`/bourse ouvrir`, 1 000 parts). Les joueurs **extérieurs** achètent des parts (`/bourse acheter <guilde> <parts>`, cours = 10 + capitaux propres ÷ 1 000 ; 100 parts par joueur, 49 % du capital au plus) et les revendent à la guilde à 90 % du cours, **après 24 h de détention** et sans jamais gagner plus de 1,5 fois le prix payé (la bourse ne sert pas à vider la banque). Toutes les 6 heures, **20 % du bénéfice** de la guilde (hausse de ses **capitaux propres** : banque moins dettes nettes, donc un prêt reçu n'est jamais un bénéfice) est versé aux actionnaires **au prorata de leurs parts**. `/bourse` liste les guildes cotées, `/bourse portefeuille` vos actions. Une guilde dissoute disparaît de la cote : ses actions ne valent plus rien. Succès *Actionnaire*.

### ⚔ Guerres formelles : les sièges (`/siege`)

Un officier plante un **Étendard de Siège** sur la **frontière** d'une guilde rivale (dans un chunk voisin du sien, en terrain libre ou sur son propre territoire, dans un espace vide posé sur un sol solide) avec `/siege declarer <guilde> <jour> <heure>` (ex. `samedi 21`) : **24 heures de préavis**, 500 pièces de frais pris dans la banque, créneau d'une heure qui ne peut pas chevaucher la trêve, impossible sous traité. La cible peut **annuler le siège en détruisant l'Étendard** avant l'heure dite (elle empoche les frais). Pendant le créneau, les membres de la guilde assiégeante peuvent **démolir le Nexus ennemi à la pioche ou à l'arme lourde** : dégâts ×2, même sans défenseur présent, sans acheter de horde. **Victoire** si le Nexus tombe ou si 40 % de ses PV max sont infligés : 10 % de la banque du vaincu (3 000 maximum) sont pillés. Sinon le défenseur garde les frais. `/siege liste` montre les sièges, `/siege annuler` renonce (moitié des frais rendue). Historique au Panthéon du site. Succès *Maître du siège*.

### 🔱 Atlantes

La spécialisation **Atlante** (voir la table ci-dessus) exige un Nexus **dans l'eau** : posez-le contre un bloc d'eau. L'eau est autorisée tout autour (pose, seaux, écoulement) dans le chunk du Nexus ; la lave et l'obsidienne restent interdites. Les membres ont tous les avantages sous-marins **dans l'eau** et subissent **Lenteur I sur la terre ferme**.

### 🗳 Élections du site et Panthéon

Chaque semaine, la communauté élit sur le site (`/votes`) le plus **débile**, le plus **gros**, le plus **drôle** et le plus **chiant**. Pour voter : cliquez « Revendiquer ce pseudo » (sur votre profil ou sur la page des votes), le site affiche un **code** (`ESI-XXXX`) à **taper dans le chat du jeu** avec ce compte (le message n'est pas diffusé) ; le jeu demande alors une **confirmation** (`[CONFIRMER]` / `[REFUSER]`, avec l'adresse approximative du demandeur : ne confirmez jamais une demande que vous n'avez pas faite). Un **cookie** (jeton aléatoire, HttpOnly, 90 jours) reconnaît ensuite votre navigateur. Un vote par catégorie, modifiable, jamais pour soi ; comptes de moins de 2 jours exclus, et **un seul compte par connexion et par catégorie**. `/webvote optout` vous retire des élections ; les admins peuvent les désactiver ou renommer les catégories (`web.votes.*`). Les vainqueurs de la semaine sont proclamés dans le chat et affichés au **Panthéon** (`/pantheon`), avec les boss terrassés, les sièges et la règle du Tribunal.

### ⚖ Équilibre : revanche et rattrapage

- **Revanche** : la guilde qui perd un raid obtient **24 h de hordes à -50 %** contre son vainqueur.
- **Rattrapage** : les guildes **sous la médiane** de la saison touchent jusqu'à **+25 %** sur les primes de monstres (4 guildes au moins). Le bonus diminue à mesure qu'elles rattrapent et disparaît à la médiane.

### Le jeu en anglais

`/english` traduit le jeu **pour toi seul** : messages, barre d'action, menus, tableau de bord et livre `/guide` (réponse `/francais` pour revenir). Le choix est mémorisé. Les noms de guildes et de joueurs ne sont pas traduits. Pas encore traduits : la barre de boss des événements, le tchat, les entrées du journal de guerre et le site web.

### Le site web

Le serveur a un site public : **`http://<adresse-du-serveur>:8080/`**, qui se met à jour tout seul (toutes les 30 secondes, réglable avec `web.refresh-seconds`). Il n'affiche **aucune coordonnée** : rien qui aide à trouver une base.

| Page | Contenu |
| :--- | :--- |
| Accueil | Joueurs en ligne, top guildes, saison naturelle, raids en cours, journal récent |
| Guildes · fiche de guilde | Classement ; membres, traités, diversité, trophées, courbes d'évolution, Âges franchis, faits marquants |
| Joueurs · profil | En ligne, plus riches, annuaire ; profil de chaque joueur (métier, guilde, statistiques, succès) |
| Raids | Les hordes en cours : cible, attaquant, monstres restants, défenseurs sur place, durée |
| Journal | Le journal de guerre complet, filtrable par catégorie |
| Diplomatie | Réseau des alliances, pactes, vassalités et rivalités, carte des rivalités |
| Musée | Trophées des guildes, tous les succès avec leur rareté, derniers débloqués |
| Âges | Frise sur un vrai axe de temps (guildes masquables une à une) et prérequis du prochain Âge de chaque guilde |
| Panthéon | Boss terrassés (avec la guilde gravée), sièges, règle du Tribunal, titres de la semaine |
| Votes | Élections hebdomadaires (revendication du pseudo par code dans le chat) |
| Guide | Toutes les règles, avec une recherche |

Le logo du serveur s'affiche dans l'en-tête. Le site existe aussi **en anglais** : bouton **EN** / **FR** en haut à droite, ou `?lang=en` à la fin de l'adresse (le choix est retenu par le navigateur). Il réutilise le dictionnaire de `/english` (`lang/en.tsv`). Les profils affichent la **réputation** ❤, et chaque joueur y est représenté par **sa tête** (récupérée et mise en cache par le serveur).

Les données sont aussi en JSON : `/api/stats.json`, `/api/journal.json`, `/api/raids.json`.

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
| `/guild nexus place` · `/nexus` | Poser le Nexus · arbre technologique des défenses |
| `/nexus sacrifice` · `/nexus gamble` | Sacrifier une ressource au Nexus · roulette russe (chef) |
| `/guild annex <join\|decline>` | Après une défaite : rejoindre le vainqueur ou partir |
| `/guild bank <montant>` | Déposer dans la banque de guilde |
| `/age` | Progression des Âges et dons |
| `/money` · `/baltop` · `/pay <joueur> <montant>` | Son argent · les plus riches · envoyer des pièces |
| `/ah` · `/ah sell <prix>` | Hôtel des Ventes et raids · vendre l'objet en main |
| `/contrat` | Contrats du jour et contrat de guilde |
| `/aide` | Appeler ses alliés à l'aide pendant une attaque |
| `/deguisement <guilde ou Pseudo>` · `/deguisement off` | Changer de skin et de pseudo (guilde ou n'importe quel compte Minecraft) · retirer le déguisement |
| `/climat` | Saison naturelle en cours |
| `/journal` · `/succes [joueur]` | Journal de guerre · succès |
| `/traite` · `/traite liste` | Traités entre guildes |
| `/station` · `/rail` | Prendre le train · gérer son réseau (officiers) |
| `/classement` · `/histoire` | Classement de la saison · grands moments |
| `/hud` | Afficher ou masquer le tableau de bord |
| `/english` · `/francais` | Passer le jeu en anglais (menus, messages, tableau de bord, livre) · revenir au français |
| `/banque` | Prêts entre guildes, dettes, coffre commun |
| `/merci <joueur>` | Offrir un point d'honneur (une fois par jour et par personne) |
| `/parrain <joueur>` | Parrainer une recrue (`accepter` / `refuser` pour répondre) |
| `/rancon` | Gérer un prisonnier : `prix`, `payer`, `liberer`, `executer` |
| `/cosmetique` | Menu des cosmétiques (auras, traînées, effets de victoire) : à gagner ou à acheter |
| `/hub` | Retourner au hub (bloqué pendant un combat PvP) |
| `/vote [numéro]` | Tribunal des guildes : voter la règle de la semaine (le lundi) |
| `/duel <joueur> [mise]` · `/duel accepter\|refuser` | Duel d'honneur dans une arène neutre |
| `/pari <camp> <montant>` | Parier sur un raid ou un duel (spectateurs) |
| `/taverne [declarer\|retirer]` | Zone neutre et pile ou face |
| `/bourse [ouvrir\|acheter\|vendre\|portefeuille]` | Actions de guilde (Âge 4) |
| `/siege [declarer <guilde> <jour> <heure>\|liste\|annuler]` | Guerre formelle |
| `/rancon cellule` | Désigner la cellule de prison de sa guilde |
| `/station fret [confirmer\|annuler\|recuperer]` | Train de fret |

---

## 🛠️ 12. Pour les administrateurs

<details>
<summary><b>Installation, configuration et commandes d'administration</b></summary>

### Installation

- **Serveur** : Paper 26.2, Java 25.
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
| `life` | Saisons naturelles (durée), déguisement (prix, durée, délai), récompense de l'appel à l'aide |

### Le site web

- Activé par défaut sur le **port 8080** (`web.port`). Sous Linux, ce port demande les droits root ou `setcap 'cap_net_bind_service=+ep' $(readlink -f $(which java))` ; sinon, utiliser 8080. Si le port est refusé, le plugin démarre quand même et le signale dans la console.
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
| `/game reset` | Remettre **tout** le jeu à zéro : guildes, Nexus, fortunes, succès, statistiques, contrats, réputation, parrainages, cosmétiques, journal, courbes, Âges, saison, prêts et site web (seules les préférences de langue sont gardées) |
| `/race set <joueur> <race>` | Changer le métier d'un joueur |

### Opérateurs et délais d'attente

Les **opérateurs** (`op`) ne sont soumis à **aucun délai d'attente** : déguisement (30 min), appel à l'aide (10 min), protection d'une guilde après un raid, soin du Nexus (30 s et blocage après une attaque), changement de métier (24 h), changement de spécialisation (7 jours), recherche de lignes de train, `/merci` (une fois par jour et par personne) et délai anti-farm des primes PvP. Réglage : `admin.ops-bypass-cooldowns` (`true` par défaut).

### Anti-triche intégré

- Les bonus de minage ne s'appliquent jamais sur un bloc posé par un joueur, ni sur les terres d'une autre guilde.
- Les quantités demandées pour les Âges se basent sur le maximum de membres atteint : expulser des joueurs avant de donner ne sert à rien.
- Les enchantements au-delà du niveau normal sont retirés hors du Conclave Arcanique.
- Taille des hordes limitée, banque débitée seulement une fois la cible validée, monstres de raid supprimés au redémarrage.
- Écritures d'argent sérialisées (pas de perte entre une vente et une déconnexion).

</details>
