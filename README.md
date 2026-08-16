# ⚔️ GUIDE OFFICIEL DES JOUEURS — SERVEUR ESICRAFT

Bienvenue sur **Esicraft**, un serveur de survie hardcore stratégique orienté guildes, métiers, économie dynamique et guerres de territoires !

Ce guide rassemble l'intégralité des fonctionnalités, mécaniques, métiers et commandes du serveur pour vous permettre de bâtir votre empire et dominer vos rivaux.

---

## 📑 Sommaire
1. [🎭 Les 6 Métiers (Races)](#-1-les-6-métiers-races)
2. [🏛️ Les Guildes & Territoires](#-2-les-guildes--territoires)
3. [🌟 Les 5 Spécialisations de Guilde](#-3-les-5-spécialisations-de-guilde)
4. [💎 Le Cœur de Base : Le Nexus & Pièges](#-4-le-cœur-de-base--le-nexus--pièges)
5. [⚔️ Les Hordes Noires & Raids de Siège](#-5-les-hordes-noires--raids-de-siège)
6. [💰 Économie, Hôtel des Ventes & Prime PvP](#-6-économie-hôtel-des-ventes--prime-pvp)
7. [⏳ Progression des Âges Technologiques](#-7-progression-des-âges-technologiques)
8. [📋 Liste Complète des Commandes](#-8-liste-complète-des-commandes)

---

## 🎭 1. Les 6 Métiers (Races)

> [!IMPORTANT]
> **Obligation de Métier** : Un joueur qui n'a pas encore choisi de métier (`/race`) **ne peut casser aucun bloc**. Le choix d'une race est obligatoire dès votre arrivée pour commencer à miner, récolter et bâtir.

Dès votre arrivée, choisissez votre métier avec la commande `/race`. Chaque métier possède des autorisations de craft exclusives, des bonus uniques et des contreparties équilibrées :

| Métier | Rôle & Spécialité | Bonus Principaux | Restrictions & Malus |
| :--- | :--- | :--- | :--- |
| **⛏ Mineur (Nain)** | Extraction minière & géologie | • Seul métier capable de fabriquer les pioches en fer/diamant.<br>• Effet **Haste II permanent** sous la couche $Y \le 0$.<br>• **+15% de chance de doubler** les minerais extraits. | Vitesse de déplacement réduite en plein air. |
| **🌿 Sylvain (Elfe)** | Agriculture, élevage & nature | • Seul métier à fabriquer des houes & utiliser la poudre d'os.<br>• Récolte rapide des cultures & reproduction animale accélérée.<br>• Potions agricoles exclusives. | -20% de dégâts au corps-à-corps. |
| **🔨 Forgeron** | Forge lourde, armures & métal | • Seul métier capable de crafter les armures, boucliers et épées.<br>• **+35% de lingots supplémentaires** lors de la fonte des minerais.<br>• **-50% de coût en XP** sur l'enclume. | Consommation de faim augmentée de +25% lors des efforts physiques. |
| **🏹 Rôdeur** | Chasse à distance & exploration | • Seul métier capable de fabriquer les arcs, arbalètes et flèches.<br>• **+25% de dégâts à distance**.<br>• Vitesse de déplacement accrue en forêt et jungle. | Ne peut pas porter d'armure lourde intégrale. |
| **🧪 Arcaniste** | Magie, alchimie & enchantements | • Seul métier à fabriquer et utiliser les alambics et tables d'enchantement.<br>• **+30% de gain d'XP** sur toutes les actions.<br>• Durée des potions consommées prolongée de **+25%**.<br>• **-20% de dégâts magiques subis** (potions, poison, wither). | Résistance physique réduite en mêlée. |
| **⚙ Ingénieur** | Automatisation, redstone & engins | • Seul métier à fabriquer les pistons, distributeurs, entonnoirs et rails.<br>• Maîtrise de la TNT et des engins mécaniques.<br>• **-40% de dégâts subis face aux explosions**. | -20% de dégâts au corps-à-corps. |

> [!TIP]
> **Reconversion de Métier** : La première sélection de métier est **100% gratuite**. Vous bénéficiez ensuite de **3 reconversions de métier gratuites**. Une fois ces 3 changements gratuits épuisés, toute reconversion ultérieure coûte **10 000 pièces**.

---

## 🏛️ 2. Les Guildes & Territoires

Fondez une guilde avec vos alliés pour revendiquer des terres et bâtir votre forteresse.

### 👑 Hiérarchie de Guilde
* **Chef de Guilde (`LEADER`)** : Contrôle absolu sur la guilde, nomination des officiers (`/guild promote`), transfert de leadership (`/guild transfer`) ou dissolution (`/guild disband`).
* **Officier (`OFFICER`)** : Peut claim/unclaim des territoires, poser le Nexus, acheter des pièges et **recruter / lancer des hordes de mercenaires**.
* **Membre / Recrue (`MEMBER`)** : Participe aux dépôts en banque, aux dons d'Âge et à la défense collective.

### 🗺️ Expansion Territoriale & Claims
* **Contiguïté Obligatoire** : Le premier chunk est libre. Tous les chunks suivants doivent **obligatoirement être collés (adjacents)** à un territoire déjà possédé par votre guilde.
* **Capacité Débloquée par Âge** :
  * **Âge 1 (Rustique)** : **16 chunks max**
  * **Âge 2 (Métallurgie)** : **32 chunks max**
  * **Âge 3 (Alchimie)** : **64 chunks max**
  * **Âge 4 (Guerre Totale)** : **128 chunks max**
  * **Âge 5 (Mythique & Apex)** : **Illimité**
* **Tarification Progressive Douce** :
  * Chaque nouveau chunk coûte : $\text{Prix} = 100 + (N \times 25\text{ pièces})$ où $N$ est le nombre de claims déjà possédés (ex: 100 pièces, puis 125, 150, 175, 200...).
  * Le prix est **directement prélevé sur la Banque de Guilde**.

### 🧱 Protections & Sécurité en Territoire Ennemi
* **Protection totale des blocs** : Impossible de casser ou poser des blocs sur le territoire d'une autre guilde.
* **Protection des conteneurs** : Seuls les membres de la guilde peuvent ouvrir les coffres, barils, shulkers, fours et entonnoirs situés dans leurs claims.
* **Restrictions de combat en zone ennemie** :
  * ❌ **Pose de TNT interdite**.
  * ❌ **Utilisation d'Ender Pearls interdite** (lancement et téléportation vers un claim adverse bloqués).
  * ❌ **Cristaux de l'End interdits** (pose et explosion bloquées).

---

## 🌟 3. Les 5 Spécialisations de Guilde

Choisissez la spécialisation collective de votre guilde via le menu `/guild spec` :

1. ⚔️ **Légion Guerrière (PvP / Conquête)** :
   * **Bonus** : +30% de dégâts d'attaque contre les monstres et lors des sièges.
   * **Contrepartie** : Coût d'achat des territoires augmenté de +20%.
2. ⛏️ **Industrie Souterraine (Minage / Production)** :
   * **Bonus** : Coût des pièges de Nexus réduit de **-50%**, rendement de minage accru.
   * **Contrepartie** : +20% de dégâts subis face aux attaques magiques.
3. 🌾 **Enclave Champêtre (Ressources & Santé)** :
   * **Bonus** : Régénération de santé passive des membres dans leurs claims, double récolte agricole.
   * **Contrepartie** : PV max du Nexus réduits de 10%.
4. 💰 **Syndicat Marchand (Commerce & Hordes)** :
   * **Bonus** : **-30% de réduction sur l'achat de TOUTES les hordes de mercenaires** et taxe réduite à l'Hôtel des Ventes.
   * **Contrepartie** : Puissance offensive des membres légèrement diminuée.
5. 🔮 **Conclave Arcanique (Magie & Runes)** :
   * **Bonus** : Durée des pièges et des effets alchimiques amplifiée, résistance accrue.
   * **Contrepartie** : Coût d'achat des claims augmenté de +50%.

> [!IMPORTANT]
> **Règle de Changement de Spécialisation** : Le premier choix de spécialisation est immédiat. Par la suite, une guilde peut changer de spécialisation **uniquement lorsqu'un NOUVEAU joueur** (qui n'a jamais appartenu à la guilde auparavant) rejoint ses rangs ! Cela valorise le recrutement actif et empêche le changement opportuniste permanent.

---

## 💎 4. Le Cœur de Base : Le Nexus & Pièges

Chaque guilde possède un **Nexus** (Cœur de base) qu'elle doit placer dans ses territoires avec `/guild nexus place`.

### 🛡️ Règles du Chunk de Nexus
* **Interdiction Totale de la Lave** : Impossible de vider un seau de lave, d'éjecter de la lave par dispenser ou de faire couler de la lave dans le chunk du Nexus.
* **Interdiction de l'Obsidienne** : La pose d'obsidienne ou d'obsidienne pleureuse est bloquée dans tout le chunk pour préserver un gameplay fluide et dynamique lors des assauts.

### 💖 Soin du Nexus
* En jetant une **Pomme Dorée** sur le bloc central du Nexus, vous lui rendez instantanément **+500 PV** (hors période de raid actif).

### 💣 Les 5 Pièges de Défense (`/nexus traps`)
* 🔥 **Brasier Infernal** : Enflamme instantanément tous les assaillants à proximité.
* 🕸️ **Toile d'Entrave** : Ralentit considérablement les ennemis s'approchant du cœur.
* ⚡ **Foudre de Riposte** : Frappe les envahisseurs de violents éclairs.
* 🧪 **Brume Toxique** : Inflige poison et faiblesse aux assaillants.
* 🤖 **Golem Gardien** : Invoque un puissant golem de fer pour défendre le Nexus.
* *Note : Les pièges sont à usage unique par raid et se réarment après chaque invasion.*

---

## ⚔️ 5. Les Hordes Noires & Raids de Siège

Commanditez des armées de monstres mercenaires pour assiéger et détruire le Nexus d'une guilde rivale !

### 🎯 Accès & Financement
* **Accès** : Rendez-vous à l'Hôtel des Ventes avec `/ah` et cliquez sur le **Crâne de Wither** (slot 49).
* **Rangs Requis** : Seuls les **Officiers** et le **Chef** peuvent configurer et lancer des hordes.
* **Financement** : Le coût est **100% débité de la Banque de Guilde** (`/guild bank`).

### 📦 Types de Hordes
1. **Contrats Prédéfinis** : Zombies cuirassés, Squelettes incendiaires, Araignées venimeuses, Siège de Pillards & Ravageurs...
2. **Créateur de Horde Sur-Mesure** : Composez vous-même votre armée unité par unité parmi 8 classes de monstres (**quantité de monstres 100% illimitée**, selon vos fonds en banque de guilde !).

### 💥 Options Spéciales d'Invasion
* 💣 **Sapeurs Explosifs** : Les monstres bloqués par des tranchées ou des murs explosent pour créer une brèche et **vaporisent instantanément toute source d'eau et de lave dans un rayon de 5 blocs**.
* 🔥 **Ignifugation (Résistance Lave & Feu)** : Vos monstres reçoivent un effet de résistance au feu permanent et traversent les douves de lave adverses sans aucun dégât.

### ⭐ Hégémonie Économique (Prime sur les Guildes Fortunées)
Pour empêcher les monopoles financiers écrasants et dynamiser les guerres de factions :
* **Déclenchement automatique du Statut d'Hégémonie** si la guilde cible remplit au moins une de ces deux conditions :
  1. **Au moins 50% de ses membres** figurent dans le **Top 5 des joueurs les plus riches** du serveur (`/baltop`).
  2. **Sa Banque de Guilde détient plus de 30%** de la masse monétaire totale du serveur.
* **Effets Dévastateurs contre la Guilde Hégémonique** :
  * 📉 **Coût des Hordes divisé par 2 (-50% !)** : Toutes les hordes (prédéfinies ou personnalisées) lancées contre cette guilde coûtent **moitié prix**.
  * ⏱ **Cooldown de Raid réduit à 2 minutes** : L'immunité de protection post-raid contre cette guilde dure **seulement 2 minutes** au lieu de 30 minutes.
  * 🛡 **Badge dans le sélecteur de cibles** : Les guildes sous ce statut sont marquées d'une étoile dorée `⭐ HÉGÉMONIE ÉCONOMIQUE` dans le menu des raids.

### 🏴 Annexion de Territoires & Trésor de Guerre
* Si le Nexus d'une guilde est détruit lors d'un raid :
  * 🗺️ **Transfert Territorial** : Tous les territoires de la guilde vaincue sont **instantanément annexés** par la guilde victorieuse.
  * 💰 **Fusion des Banques de Guilde (Trésor de Guerre)** : L'intégralité de la trésorerie de la guilde vaincue est **automatiquement transférée et fusionnée dans la Banque de la guilde vainqueur**.
  * 🤝 **Choix d'Allégeance des Vaincus** :
    * `/guild annex join` : Rejoindre la guilde victorieuse en tant que membre.
    * `/guild annex decline` : Refuser et redevenir joueur libre sans guilde.

---

## 💰 6. Économie, Hôtel des Ventes & Prime PvP

### 🪙 Gagner des Pièces & Tableau des Primes de Chasse
Chaque monstre éliminé rapporte une prime directement créditée sur votre compte, indexée sur sa dangerosité :

| Catégorie | Monstres | Prime par Élimination |
| :--- | :--- | :--- |
| 🐉 **Boss Légendaires** | • **Ender Dragon**<br>• **Warden (Gardien Antique)**<br>• **Wither Boss**<br>• **Grand Gardien (Elder Guardian)** | **750.00 pièces**<br>**500.00 pièces**<br>**350.00 pièces**<br>**150.00 pièces** |
| 👺 **Mini-Boss & Sièges** | • **Ravageur (Ravager)**<br>• **Évocateur (Evoker)**<br>• **Piglin Brute** | **75.00 pièces**<br>**40.00 pièces**<br>**25.00 pièces** |
| 🔥 **Nether & End (Dangereux)** | • **Ghast** & **Shulker**<br>• **Wither Squelette** & **Gardien**<br>• **Blaze** 🔥 & **Hoglin / Zoglin** | **20.00 pièces**<br>**15.00 pièces**<br>**12.00 pièces** |
| 🏹 **Élite Overworld** | • **Vindicateur**<br>• **Sorcière (Witch)**<br>• **Enderman**<br>• **Pillard (Pillager)** | **15.00 pièces**<br>**12.00 pièces**<br>**10.00 pièces**<br>**8.00 pièces** |
| 🧟 **Monstres Communs** | • **Creeper** & **Phantom**<br>• **Piglin** & **Cochon Zombie**<br>• **Araignées (Normales / Cavernes)**<br>• **Squelettes & Strays**<br>• **Zombies (Noyés, Husks, Villageois)** | **5.00 pièces**<br>**4.00 pièces**<br>**3.00 - 3.50 pièces**<br>**3.00 pièces**<br>**2.50 pièces** |
| 🐛 **Petits Monstres** | • **Slimes & Magma Cubes**<br>• **Vex**<br>• **Silverfish & Endermites** | **2.00 pièces**<br>**2.50 pièces**<br>**1.50 pièce** |

*Note : Les membres d'une guilde orientée **Syndicat Marchand** bénéficient d'un bonus passif de **+20% de primes supplémentaires** sur tous les monstres.*

1. **Hôtel des Ventes (`/ah`)** : Vendez vos ressources et équipements craftés avec `/ah sell <prix>`.
2. **Défense de Nexus** : Éliminer les monstres d'une horde assaillante rapporte de précieux butins.

### ⚔️ Prime de Kill PvP (Hors Guilde)
Lors de l'élimination d'un joueur adverse :
* **Le Vainqueur (Killer)** reçoit : **+1 000 pièces + 1% de la fortune** de la victime.
* **La Victime** perd :
  * **Si elle a plus de 1 000 pièces** : Elle perd **1 000 pièces + 1% de sa fortune**.
  * **Si elle a moins de 1 000 pièces** : **Elle perd l'intégralité de son argent** (solde retombant à `0.00 pièce`).
* *(Règle inactive entre membres d'une même guilde pour protéger les alliés).*

---

## ⏳ 7. Progression des Âges Technologiques

Le serveur progresse à travers **5 Âges technologiques majeurs**. Pour faire franchir un Âge à votre guilde et débloquer les technologies supérieures et les dimensions, les membres doivent unir leurs forces et faire des dons de **minerais et de récoltes agricoles** via `/age` :

> [!CAUTION]
> **Restrictions Dimensionnelles par Âge** :
> * 🔥 **Nether** : L'accès au Nether (portails, allumage et téléportations) est **strictement bloqué jusqu'à l'Âge 2 (Métallurgie)**.
> * 🌌 **L'End** : L'accès à l'End (portails, yeux de l'ender et téléportations) est **strictement bloqué jusqu'à l'Âge 3 (Alchimie)**.

1. **Âge Rustique (Âge 1 — Départ)** :
   * *Technologies* : Outils en bois et pierre uniquement (16 claims max). **Dimensions Nether & End verrouillées**.
2. **Âge de la Métallurgie (Âge 2)** :
   * *Déblocages* : **Accès au Nether débloqué** 🔥, outils en fer universels, boucliers et extension à **32 claims max**.
   * *Objectifs Requis (Quotas réduits de 50%)* :
     * ⛏ 1 250 Lingots de Cuivre | 750 Fer Brut | 1 000 Bûches de Chêne
     * 🌾 **1 500 Blé** | 🥕 **750 Carottes** *(Alimentation & maraîchage)*
3. **Âge de l'Alchimie (Âge 3)** :
   * *Déblocages* : **Accès à l'End débloqué** 🌌, armures complètes en fer, alambics, potions T1 et extension à **64 claims max**.
   * *Objectifs Requis* :
     * ⛏ 3 000 Lingots de Fer | 1 000 Lingots d'Or | 500 Verrues du Nether
     * 🥔 **2 500 Pommes de Terre** | 🎋 **2 000 Cannes à Sucre** | 🎃 **800 Citrouilles**
4. **Âge de Guerre Totale (Âge 4)** :
   * *Déblocages* : Équipements complets en diamant, potions T2 et extension à **128 claims max**.
   * *Objectifs Requis* :
     * ⛏ 800 Diamants | 1 000 Obsidienne | 400 Perles de l'End
     * ✨ **500 Carottes Dorées** | 🌾 **600 Bottes de Foin** | 🍯 **400 Bouteilles de Miel**
5. **Âge Mythique & Apex (Âge 5)** :
   * *Déblocages* : Netherite universelle, balises et **territoires illimités**.
   * *Objectifs Requis* :
     * ⛏ 150 Fragments de Netherite | 5 Étoiles du Nether | 64 Souffles de Dragon
     * 🍏 **5 Pommes Dorées Enchantées** | 🌸 **1 000 Fruits de Chorus** *(Flore mystique)*

---

## 📋 8. Liste Complète des Commandes

### 🎭 Métiers & Races
* `/race` : Ouvrir le menu de sélection des métiers.
* `/guide` : Recevoir / ouvrir le livre guide interactif officiel.

### 🏛️ Gestion de Guilde
* `/guild create <nom> <tag>` : Fonder une nouvelle guilde.
* `/guild invite <joueur>` : Inviter un joueur dans la guilde *(Officier/Chef)*.
* `/guild join <nom>` : Accepter une invitation de guilde.
* `/guild leave` : Quitter sa guilde actuelle.
* `/guild kick <joueur>` : Expulser un joueur *(Officier/Chef)*.
* `/guild promote <joueur>` : Promouvoir un joueur au rang d'Officier ou le rétrograder *(Chef)*.
* `/guild demote <joueur>` : Rétrograder un officier au rang de membre simple *(Chef)*.
* `/guild transfer <joueur>` : Transférer la direction de la guilde à un membre *(Chef)*.
* `/guild disband` : Dissoudre la guilde *(Chef)*.
* `/guild info [nom]` : Afficher les statistiques, claims, banque et statut d'une guilde.

### 🗺️ Territoires & Défenses
* `/guild claim` : Revendiquer le chunk actuel pour votre guilde *(Officier/Chef)*.
* `/guild claim show` : Activer/désactiver la surbrillance des bordures de vos territoires.
* `/guild unclaim` : Abandonner le chunk actuel *(Officier/Chef)*.
* `/guild spec` : Ouvrir le menu des spécialisations de guilde.
* `/guild nexus place` : Poser le Nexus de guilde *(Officier/Chef)*.
* `/nexus traps` : Acheter et armer les pièges défensifs du Nexus.
* `/guild annex <join|decline>` : Accepter ou refuser de rejoindre la guilde victorieuse après une défaite.

### 💰 Économie & Marché
* `/guild bank <montant>` : Déposer de l'argent dans la banque de guilde.
* `/pay <joueur> <montant>` : Transférer instantanément des pièces à un autre joueur.
* `/money` : Consulter son solde de pièces personnel.
* `/baltop` : Afficher le classement des joueurs les plus riches du serveur.
* `/ah` : Ouvrir l'Hôtel des Ventes et le marché des Hordes Noires.
* `/ah sell <prix>` : Mettre en vente l'objet tenu en main à l'Hôtel des Ventes.
* `/age` : Ouvrir le menu de progression des Âges et déposer des ressources.
