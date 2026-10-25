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

| Métier | Rôle & Spécialité | Crafts & Usages Exclusifs | Bonus Passifs & Actifs | Restrictions & Malus |
| :--- | :--- | :--- | :--- | :--- |
| **⛏ Mineur (Nain)** | Extraction minière & géologie | • Pioche en fer (Âge 1)<br>• Pioche & Pelle diamant (Âge 2)<br>• Pioche & Pelle Netherite (Âges 3-4) | • **Haste II** et **Vision Nocturne** permanents sous $Y \le 0$<br>• **+15% de chance de doubler** tous les minerais extraits | • **Lenteur I** en plein air et plein soleil ($Y > 60$)<br>• Incapable de fabriquer des armes ou armures métalliques |
| **🌿 Sylvain (Elfe)** | Agriculture, élevage & nature | • Houe en fer (Âge 1), diamant (Âge 2), Netherite (Âges 3-4)<br>• Utilisation de la Poudre d'os (Âges 1-2)<br>• Reproduction animale (Âges 1-2)<br>• Pommes Dorées artisanales | • **Aura de Régénération I permanente** pour soi et tous les joueurs à moins de 4 blocs<br>• **Récolte instantanée automatique au clic-droit** sur les cultures à maturité (avec replantage automatique) | • **-20% de dégâts** infligés au corps-à-corps |
| **🔨 Forgeron** | Forge lourde, armures & métal | • Armures fer complètes, épées & boucliers (Âge 1)<br>• Armures, épée & hache diamant (Âge 2)<br>• Armures, épée & hache Netherite (Âges 3-4)<br>• Enclumes, meules & hauts-fourneaux | • **Résistance au Feu** permanente<br>• **+35% de chance de doubler** un lingot extrait d'un four/haut-fourneau<br>• **-50% de coût en XP** sur l'enclume | • **Consommation de faim augmentée de +25%** lors des efforts physiques |
| **🏹 Rôdeur** | Chasse à distance & exploration | • **Seul métier capable d'utiliser et fabriquer des Arcs et Arbalètes** jusqu'à l'Âge 3 (compris, débloqué à l'Âge 4)<br>• Flèches et flèches spectrales artisanales | • **Toutes les flèches tirées deviennent des Flèches de Poison (Poison I)**<br>• **+25% de dégâts à distance** sur tous les projectiles<br>• **Vitesse I (Speed I)** permanente dans les biomes naturels (plaines, forêts, jungles, taïgas) | • **Pénalité de lenteur (Slowness I)** si un plastron lourd (diamant ou Netherite) est équipé |
| **🧪 Arcaniste** | Magie, alchimie & enchantements | • **Seul métier capable d'enchanter des objets** (Table d'enchantement & Livres enchantés sur enclume)<br>• **Seul métier capable d'utiliser et lancer des Potions Jetables (Splash Potions)**<br>• Utilisation et craft d'Alambics exclusifs jusqu'à l'Âge 3 (Alchimie)<br>• Pommes Dorées artisanales | • **+30% d'XP reçue** sur toutes les actions<br>• **Durée des potions consommées prolongée de +25%**<br>• **-20% de dégâts magiques subis** (potions de dégâts, poison, wither, souffle de dragon) | • **+15% de dégâts physiques subis** en combat rapproché (fragilité en mêlée) |
| **⚙ Ingénieur** | Automatisation, redstone & engins | • Pistons, pistons collants, entonnoirs (hoppers), observateurs, répéteurs, comparateurs, distributeurs, droppers & **TNT** (Âges 1-3) | • **-40% de dégâts subis face aux explosions** (TNT, creepers, lits)<br>• Maîtrise exclusive de la TNT et de l'automatisation | • **-20% de dégâts** infligés au corps-à-corps |

> [!TIP]
> **Commandes de Métier** :
> * `/race` : Ouvre le menu de sélection ou de reconversion de métier (1ère sélection gratuite + 3 reconversions offertes, puis 10 000 pièces).
> * `/race show [joueur]` : Ouvre l'**Annuaire interactif des Métiers** affichant la tête, le métier, la guilde, l'âge technologique, le solde et les statistiques de tous les joueurs connectés.

---

## 🏛️ 2. Les Guildes & Territoires

Fondez une guilde avec vos alliés pour revendiquer des terres et bâtir votre forteresse.

### 👑 Hiérarchie de Guilde
* **Chef de Guilde (`LEADER`)** : Contrôle absolu sur la guilde, nomination des officiers (`/guild promote`), transfert de leadership (`/guild transfer`) ou dissolution (`/guild disband`).
* **Officier (`OFFICER`)** : Peut claim/unclaim des territoires, poser le Nexus, acheter des pièges et **recruter / lancer des hordes de mercenaires**.
* **Membre / Recrue (`MEMBER`)** : Participe aux dépôts en banque, aux dons d'Âge et à la défense collective.
* **Limite de membres** : **10 joueurs maximum** par guilde (`guilds.max-members-default`). Un joueur ne peut appartenir qu'à **une seule guilde** ; les invitations expirent au bout de **5 minutes**.
* **Composition** : les contraintes de spécialisation (métiers interdits/limités, Légion : min. 50 % de Rôdeurs/Forgerons) sont vérifiées à **chaque arrivée** (invitation, allégeance après annexion) et à chaque changement de métier.
* **Dissolution** : `/guild disband` est refusé pendant un raid ou dans les **10 minutes** qui suivent une attaque du Nexus (impossible d'esquiver le Trésor de Guerre).

### 🗺️ Expansion Territoriale & Claims
* **Contiguïté Obligatoire** : Le premier chunk est libre. Tous les chunks suivants doivent **obligatoirement être collés (adjacents)** à un territoire déjà possédé par votre guilde. À l'inverse, `/guild unclaim` est refusé s'il **coupe le territoire en plusieurs morceaux** ou s'il vise le chunk du Nexus.
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
  * ❌ **Minecarts avec TNT interdits** (pose, tir via distributeur, pénétration sur rails et explosion bloqués).
  * ❌ **Utilisation d'Ender Pearls interdite** (lancement et téléportation vers un claim adverse bloqués).
  * ❌ **Cristaux de l'End interdits** (pose et explosion bloquées).
  * ❌ **Fruit de Chorus** (téléportation vers un claim adverse bloquée), **pistons**, **lave / eau / feu** venant de l'extérieur, **Wither / Enderman / Ravageur** (destruction de blocs) et **projectiles** sur les animaux, cadres et armor stands : tous bloqués dans les claims (sauf pendant un raid sur la guilde).
  * ⚠️ **Territoire vulnérable** : après la destruction de son Nexus sans annexion, le territoire d'une guilde perd sa protection des blocs/explosions pendant **30 minutes**.

---

## 🌟 3. Les 5 Spécialisations de Guilde

Choisissez la spécialisation collective de votre guilde via le menu `/guild spec` :

1. ⚔️ **Légion Guerrière (PvP / Conquête)** :
   * **Bonus** : Effets **Force I** et **Résistance I** permanents dans les claims, **-30% de réduction sur l'achat de TOUTES les hordes de mercenaires**, **+30% de dégâts d'attaque** contre les monstres et hordes ennemies.
   * **Contraintes** : Max 1 Nain (Mineur) et 1 Sylvain (Fermier), minerais et récoltes réduits de 25% dans les claims.
2. ⛏️ **Industrie Souterraine (Minage / Production)** :
   * **Bonus** : Effet **Haste II** permanent dans les claims sous $Y \le 30$, **drops de minerais bruts multipliés par x1.5**, coût des pièges de Nexus et Golems réduit de **-50%**.
   * **Contraintes** : Max 1 Rôdeur (combattant à distance), vulnérabilité aux attaques magiques (+20% de dégâts subis).
3. 🌾 **Enclave Champêtre (Ressources & Santé)** :
   * **Bonus** : **Santé maximale de base augmentée de +2 cœurs (+4.0 PV pour tous les membres)**, effets **Régénération I** et **Saturation** permanents dans les claims, **double récolte agricole**, **double butin sur les animaux**, **récolte doublée sur le bois et les pierres naturelles** (Andésite, Diorite, Granite, Blackstone, Calcite), reproduction animale accélérée (x3).
   * **Contraintes** : Interdiction des armures en Netherite, aucun Ingénieur (pas de TNT), max 1 Forgeron.
4. 💰 **Syndicat Marchand (Commerce & Économie)** :
   * **Bonus** : **Privilège exclusif de commerce avec les Villageois et Marchands ambulants dès les Âges 1 à 3** (le commerce avec les villageois est débloqué pour les autres guildes à partir de l'Âge 4 !), **+20% de primes** sur l'élimination des monstres, **10% des bénéfices de chaque monstre éliminé automatiquement transférés à la banque de guilde**, bonus de gain de +25% à l'Hôtel des Ventes.
   * **Contraintes** : **Aucun Arcaniste autorisé dans la guilde (0 Arcaniste)**, **Hordes de mercenaires 30% plus chères (+30% sur tous les contrats)**, aucun effet passif de combat, PV max du Nexus réduits de 20%.
5. 🔮 **Conclave Arcanique (Magie & Runes)** :
   * **Bonus** : **+25% de PV max sur le Cœur de Guilde (Nexus)**, **coûts en XP des enchantements et réparations à l'enclume réduits de -50%**, **potions consommées et jetées automatiquement du Tier Supérieur** (+1 niveau d'effet), **enchantements surpuissants débloqués (Niveau IV devient V, et Niveau V devient VI)**, le Nexus tire automatiquement un **rayon d'énergie magique** sur l'assaillant le plus proche (inflige **1 cœur de dégâts par seconde** soit 2.0 PV), dégâts magiques subis réduits de **50%**.
   * **Contraintes** : **Santé maximale de base réduite de 2 cœurs (-4.0 PV pour tous les membres)**, coût d'achat des claims augmenté de +50%, max 1 Forgeron et 1 Nain, **perte immédiate des enchantements de niveau VI / V boostés si la guilde change de spécialisation**.

> [!IMPORTANT]
> **Règle de Changement de Spécialisation** : Le premier choix de spécialisation est immédiat. Par la suite, une guilde peut changer de spécialisation **uniquement lorsqu'un NOUVEAU joueur** (qui n'a jamais appartenu à la guilde auparavant) rejoint ses rangs ! Cela valorise le recrutement actif et empêche le changement opportuniste permanent.

---

## 💎 4. Le Cœur de Base : Le Nexus & Pièges

Chaque guilde possède un **Nexus** (Cœur de base) qu'elle doit placer dans ses territoires avec `/guild nexus place`.

### 🛡️ Règles du Périmètre de Sécurité du Nexus (16 Blocs)
* **Vérification à la Pose** : Le serveur analyse l'intégralité du **rayon de 16 blocs** autour du Nexus avant d'autoriser sa pose. La présence d'**eau**, de **lave** ou d'**obsidienne** dans ce périmètre entraîne un **refus immédiat de la pose du Nexus**.
* **Interdiction Totale des Fluides (Lave & Eau)** : Impossible de poser de l'eau ou de la lave, de vider un seau, d'éjecter un fluide par dispenser ou de faire couler un fluide à moins de **16 blocs du Nexus**.
* **Interdiction de l'Obsidienne** : La pose d'obsidienne ou d'obsidienne pleureuse est strictement bloquée dans le **rayon de 16 blocs** pour préserver un gameplay fluide et dynamique lors des assauts.

### 💖 Soin du Nexus
* En jetant une **Pomme Dorée** sur le bloc central du Nexus (ou clic-droit dessus), les **membres de la guilde** lui rendent instantanément **+500 PV** (**+2 500 PV** pour une Pomme d'Or enchantée), hors période de raid actif.
* **Délai de 30 secondes** entre deux soins (`guilds.nexus.heal-cooldown-seconds`).
* Un Nexus **ne peut pas être reposé** tant que la guilde en possède un (pas de remise à 100 % de PV).
* Les **pièges achetés sont sauvegardés** : ils survivent aux redémarrages. Le **Golem Gardien** attaque désormais les intrus ; les pièges et le rayon arcanique ne visent que les non-membres **présents dans le claim**.

### 💣 Les 5 Pièges de Défense (`/nexus traps`)
* 🔥 **Brasier Infernal (250 pièces)** : Enflamme et inflige des brûlures continues aux ennemis à moins de 8 blocs.
* 🕸️ **Toile d'Entravement (200 pièces)** : Inflige Lenteur III et Cécité aux assaillants approchant du Nexus.
* ⚡ **Foudre de Riposte (350 pièces)** : Foudroie violemment quiconque frappe le Nexus (+15 dégâts réels).
* 🧪 **Brume Toxique (300 pièces)** : Émet un nuage de poison (Poison II) asphyxiant les pillards et monstres assaillants.
* 🤖 **Automate Gardien (500 pièces)** : Invoque un Golem de fer blindé loyal défendant férocement le Nexus.
* *Note : Les pièges sont financés en priorité par la Banque de Guilde, restent armés pour la prochaine horde et bénéficient d'une réduction de -50% pour l'Industrie Souterraine.*

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
  * 🗺️ **Transfert Territorial** : Les territoires de la guilde vaincue sont **instantanément annexés** par la guilde victorieuse, **dans la limite du plafond de claims de son Âge** (les chunks en trop redeviennent sauvages).
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
* **Le Vainqueur (Killer)** reçoit : **+100 pièces + 1% de la fortune** de la victime.
* **La Victime** perd :
  * **Si elle a plus de 100 pièces** : Elle perd **100 pièces + 1% de sa fortune**.
  * **Si elle a moins de 100 pièces** : **Elle perd l'intégralité de son argent** (solde retombant à `0.00 pièce`).
* *(Règle inactive entre membres d'une même guilde pour protéger les alliés).*

---

## ⏳ 7. Progression des Âges Technologiques

Le serveur progresse à travers **6 Âges technologiques majeurs**. Pour faire franchir un Âge à votre guilde et débloquer les technologies supérieures et les dimensions, les membres doivent unir leurs forces et faire des dons de **minerais et de récoltes agricoles** via `/age` :

> [!CAUTION]
> **Restrictions Dimensionnelles & Commerce Villageois par Âge** :
> * 🔥 **Nether** : L'accès au Nether (portails, allumage et téléportations) est **strictement bloqué jusqu'à l'Âge 2 (Métallurgie)**.
> * 🌌 **L'End** : L'accès à l'End (portails, yeux de l'ender et téléportations) est **strictement bloqué jusqu'à l'Âge 3 (Alchimie)**.
> * 🛡️ **Commerce Villageois** : Les échanges avec les villageois et marchands ambulants sont **réservés au Syndicat Marchand pendant les Âges 1 à 3**, puis **démocratisés pour toutes les guildes à partir de l'Âge 4 (Guerre Totale)**.

1. **Âge Rustique (Âge 1 — Départ)** :
   * *Technologies* : Outils en bois et pierre uniquement (16 claims max). **Dimensions Nether & End verrouillées**. Outils en fer réservés aux spécialistes (Nain, Forgeron, Sylvain).
2. **Âge de la Métallurgie (Âge 2)** :
   * *Déblocages* : **Accès au Nether débloqué** 🔥, outils en fer universels, boucliers et extension à **32 claims max**. **Les crafts en diamant sont réservés aux métiers spécialisés** (Nain: pioche/pelle, Forgeron: armures/épée/hache, Sylvain: houe).
   * *Objectifs Requis (Quotas réduits de 50%)* :
     * ⛏ **750 Lingots de Cuivre** | 🐮 **256 Cuirs** | 750 Fer Brut | 1 000 Bûches de Chêne
     * 🌾 **1 500 Blé** | 🥕 **750 Carottes** *(Alimentation & maraîchage)*
3. **Âge de l'Alchimie (Âge 3)** :
   * *Déblocages* : **Accès à l'End débloqué** 🌌, **démocratisation complète du diamant pour tous**, armures complètes en fer pour tous et extension à **64 claims max**. **Les crafts en Netherite deviennent accessibles exclusivement aux métiers spécialisés** (Nain: pioche/pelle, Forgeron: armures/épée/hache, Sylvain: houe).
   * *Objectifs Requis* :
     * ⛏ 3 000 Lingots de Fer | 1 000 Lingots d'Or | 500 Verrues du Nether
     * 🥔 **5 000 Pommes de Terre** | 🎋 **5 000 Cannes à Sucre** | 🪶 **3 000 Plumes** | 🎃 **800 Citrouilles**
4. **Âge de Guerre Totale (Âge 4)** :
   * *Déblocages* : **Commerce avec les villageois démocratisé pour tous**, **TNT et mécanismes offensifs (canons à TNT, brèches, pistons, dispensers, redstone avancée) démocratisés pour tous**, alambics et potions T2 universels, arcs et arbalètes pour tous, et extension à **128 claims max**.
   * *Objectifs Requis* :
     * ⛏ **500 Diamants** | **1 500 Obsidienne** | **500 Perles de l'End** | 🪽 **1 Paire d'Élytres**
     * ✨ **2 500 Carottes Dorées** | 🌾 **800 Bottes de Foin** | 🍯 **500 Bouteilles de Miel**
     * 🔴 **3 500 Poudre de Redstone** | ⚙ **250 Répéteurs** | 🧱 **200 Pistons**
     * 💣 **1 000 Poudres à Canon** | 🧨 **100 Blocs de TNT**
   * *🌟 **Bonus de Belligérance pour les Joueurs ayant atteint l'Âge 4** :*
     * ⚡ **Mobilité de Guerre** : Effet permanent **Vitesse I (Speed I)**.
     * ⚔ **Combativité** : **+10% de dégâts infligés** (mêlée et tir à distance) et **+25% d'XP supplémentaire**.
     * 🛡 **Blindage de Tranchée** : **-20% de dégâts subis face aux explosions** (TNT et creepers).
5. **Âge Mythique & Apex (Âge 5)** :
   * *Déblocages* : **Démocratisation complète de la Netherite pour tous**, balises et **territoires illimités**.
   * *Objectifs Requis* :
     * ⛏ 150 Fragments de Netherite | 5 Étoiles du Nether
     * 🍏 **5 Pommes Dorées Enchantées** | 🌸 **1 000 Fruits de Chorus**
     * 🌌 **64 Carapaces de Shulker** | 🪐 **2 500 Pierres de l'End** | 🟣 **500 Blocs de Purpur**
     * 🔴 **500 Blocs de Redstone** | 🎛 **200 Comparateurs** | 👁 **200 Observateurs**
   * *🌟 **Bénédictions & Bonus Apex pour les Joueurs ayant atteint l'Âge 5** :*
     * 👑 **Aura Souveraine Permanente** : Effets constants **Hâte I** (minage fluide), **Vitesse I** et **Résistance I**.
     * ⚔ **Puissance Martiale & Savoir** : **+15% de dégâts bruts infligés** au combat et **+50% d'XP supplémentaire** sur toutes les sources.
     * 🏛 **Empire Sans Limites** : Revendications de chunks **100% illimitées** et **accès total à tous les crafts en Netherite** pour tous les membres de la guilde.
6. 👑 **Âge de Suprématie (Âge 6 — Âge Ultime & Sacré)** :
   * *Description* : L'apogée ultime et divine de la civilisation sur le serveur, exigeant les reliques et chefs-d'œuvre les plus rares de l'univers.
   * *Objectifs Requis* :
     * 🎶 **Tous les Disques de Musique (CDs)** : 13, Cat, Blocks, Chirp, Far, Mall, Mellohi, Stal, Strad, Ward, 11, Wait, Otherside, 5, Pigstep, Relic.
     * 💀 **Toutes les Têtes de Monstres** : Crâne de Wither Squelette, Tête de Zombie, Crâne de Squelette, Tête d'Ender Dragon, Tête de Creeper, Tête de Piglin.
     * 📯 **Toutes les Cornes de Chèvre (8 Cornes)**.
     * ⭐ **64 Étoiles du Nether**.
     * 🌾 **1 Houe en Netherite** obligatoirement enchantée **Mending (Raccommodage) + Unbreaking III (Solidité 3) + Fortune III (Fortune 3)**.
     * 🛡 **Tous les Modèles de Forge (Trim Templates)** : Coast, Dune, Eye, Host, Raiser, Rib, Sentry, Shaper, Silence, Snout, Spire, Tide, Vex, Ward, Wayfinder, Wild, Netherite Upgrade.
     * 🏺 **Tous les Tessons de Poterie (20 Pottery Sherds)** : Angler, Archer, Arms Up, Blade, Brewer, Burn, Danger, Explorer, Friend, Heart, Heartbreak, Howl, Miner, Mourner, Plenty, Prize, Sheaf, Shelter, Skull, Snort.
     * 🥚 **L'Œuf de l'Ender Dragon** (`1 Dragon Egg`).
     * 🌿 **Un Buisson Mort** (`1 Dead Bush`).
   * *🌟 **Récompenses & Titres Suprêmes pour avoir conquis l'Âge 6** :*
     * 🥚 **Relique Ancestrale** : **1x Œuf d'Ender Dragon** offert à tous les membres.
     * ⚙ **Pouvoir Divin** : **1x Bloc de Commande (Command Block)** offert à tous les membres.
     * 💬 **Titre & Préfixe de Tchat Dynamique selon le Temps de Jeu** :
       * Si votre session active de connexion dure **moins d'1 heure (< 1h)** : Préfixe de gloire **`[Giga Tchad]`** dans le tchat !
       * Si votre session active de connexion dure **plus d'1 heure (> 1h)** : Préfixe de prestige **`[Gros Nerd]`** dans le tchat !

---

## 🎉 8bis. Contrats, Événements & Saisons

### 📜 Contrats quotidiens (`/contrat`)
Chaque jour (renouvellement à minuit, heure de Paris), chaque joueur ayant un métier reçoit **3 contrats** : un propre à son métier (Mineur : minerais, Sylvain : récoltes, Forgeron : lingots fondus, Rôdeur : kills à distance, Arcaniste : enchantements, Ingénieur : composants de redstone), un contrat de chasse et un contrat de minage/récolte. Chaque contrat rapporte des pièces, et les **3 accomplis donnent +400 pièces**. Les blocs posés par les joueurs, les fermes à mobs répétitives et les poses/reprises de redstone en boucle ne comptent pas.

### 🌍 Événements serveur (toutes les 45 à 90 min, `events.*` dans `config.yml`)
* 🌑 **Lune de Sang** (10 min) : plus de monstres la nuit, **primes de chasse x2**.
* ☄ **Pluie de Météorites** (3 min) : des blocs de minerai (jusqu'aux débris antiques) s'écrasent près des joueurs, hors des territoires de guilde.
* 🧙 **Marché Noir** (5 min) : un marchand ambulant apparaît (coordonnées approximatives annoncées) et échange des objets rares contre des diamants — ouvert à tous, quel que soit l'Âge.
* ☠ **Boss Mondial** (15 min) : le *Seigneur des Cendres* (800 PV). **3 000 pièces** partagées selon les dégâts infligés ; le meilleur combattant gagne une Étoile du Nether et 8 diamants.
* Admins : `/evenement <lune|meteores|marchand|boss|stop|status>`.

### 🏆 Saison & mémorial
* `/classement` : classement mensuel des guildes (territoire, Âge, richesse, membres, raids repoussés, conquêtes). Le champion reçoit **5 000 pièces** en banque, **500 par membre** et la couronne 👑 dans le tchat.
* `/histoire` : les 10 derniers événements marquants (conquêtes, raids repoussés, boss, champions).

### 🚆 Réseau ferroviaire inter-guildes (`/station`, `/rail`)
* **Voyager (`/station`)** : à moins de 8 blocs d'un panneau `[Gare]`, `/station` ouvre un menu des gares atteignables **par un chemin de rails continu**. Le réseau est un **graphe** : embranchements, croisements et rails collés les uns aux autres sont reliés (une ligne peut donc aller « à droite et à gauche »). Les chunks traversés sont chargés à la demande ; la recherche est étalée sur plusieurs ticks (réseau limité à `rail.search-max-rails` rails).
* **Prix d'un trajet** : tarif de gare (`rail.base-fare`, 1 pièce, versé à la guilde de la gare d'arrivée) + **péage de chaque guilde dont le territoire est traversé** (versé à sa banque). Votre propre guilde ne se fait pas payer ; les guildes **alliées** passent gratuitement.
* **Coopération & rivalités (`/rail`, officiers)** : fixez votre péage (0 à `rail.max-toll`) et, pour chaque autre guilde, une relation **Normale / Alliée (gratuit) / Bannie**. Une guilde bannie ne peut plus franchir vos terres : **les lignes qui les traversent sont coupées** pour elle (la recherche contourne la zone, ou la gare devient inaccessible).
* **Créer une gare** : un officier pose un panneau avec `[Gare]` en ligne 1 et le nom en ligne 2, à moins de 2 blocs d'un rail (Âge 2 minimum, `rail.station-creation-cost` en banque de guilde, 6 gares max par guilde, hors territoire d'une autre guilde). Casser le panneau supprime la gare (officiers de la guilde propriétaire seulement).

### 📜 Traités entre guildes (`/traite`)
Menu réservé aux **officiers** pour agir (tous les membres peuvent consulter avec `/traite liste`). Une proposition est envoyée à l'autre guilde (valable `treaties.proposal-minutes`, 10 min) qui l'accepte (clic gauche) ou la refuse (clic droit).
* **🤝 Alliance** (illimitée) : aucun raid entre les deux guildes, **passage ferroviaire gratuit** (relation verrouillée dans `/rail`) et **pas de dégâts entre joueurs alliés** (`treaties.protect-allies-pvp`).
* **📜 Pacte de non-agression** (`treaties.pact-days`, 7 jours) : aucun raid entre les deux guildes pendant la durée du pacte.
* **👑 Vassalité** : le vassal verse chaque jour `treaties.vassal-tribute-percent` (5 %) de sa banque au suzerain ; mêmes avantages qu'une alliance. Une guilde ne peut être vassale que d'une seule autre. Le suzerain peut libérer son vassal sans pénalité.
* **💔 Trahison** : rompre une alliance, un pacte en cours ou son suzerain coûte une **indemnité** (`treaties.break-indemnity`, 500 pièces max, plafonnée à la banque) versée à l'ex-partenaire, annoncée à tout le serveur, et interdit d'attaquer cette guilde pendant `treaties.break-grace-minutes` (30 min).
* Les traités sont sauvegardés en base (tables `treaties`, `treaty_grace`), inscrits au mémorial de saison, et supprimés si une guilde disparaît. La règle du « défenseur présent » pour les raids reste inchangée.

### 🌐 Site web public (`web.*` dans `config.yml`)
Le plugin lance un **site web en lecture seule**, accessible à tous : `http://<ip-du-serveur>/` (port `web.port`, **80** par défaut).
* **Pages** : accueil (joueurs en ligne, saison, événement, top guildes, histoire récente), `/guildes` (classement), `/guilde/<id>` (membres, traités, stats), `/joueurs` (en ligne + plus riches) et `/api/stats.json` (JSON).
* **Design** : thème sombre/clair automatique, cartes arrondies, avatars colorés, médailles et barres de score, frise de l'histoire, en-tête flouté, entièrement responsive (mobile compris). Aucune ressource externe ni script.
* **Sécurité** : serveur HTTP intégré au JDK (aucune dépendance), GET/HEAD uniquement, texte échappé, en-têtes CSP / anti-iframe / no-referrer. Les pages sont **rendues une fois par instantané** (toutes les `web.refresh-seconds`) puis servies depuis la mémoire, avec une **limite de requêtes par IP** (`web.rate-limit`, réponse 429), une file d'attente bornée et des délais d'expiration : un flood ne peut ni saturer la mémoire ni ralentir le serveur Minecraft. **Aucune coordonnée** (Nexus, bases, claims) ni IP n'est exposée, les joueurs en *vanish* sont masqués ; les PV du Nexus sont masqués par défaut (`web.show-nexus-health`), banques et soldes réglables (`web.show-guild-banks`, `web.show-balances`). Derrière un proxy inverse, tous les visiteurs partagent l'IP du proxy : augmentez alors `web.rate-limit.requests`.
* **Port 80 sous Linux** : demande root ou `setcap 'cap_net_bind_service=+ep' $(readlink -f $(which java))` ; sinon mettez `web.port: 8080`. En cas d'échec, le plugin continue normalement et l'indique dans la console. Pensez à ouvrir le port sur votre box / pare-feu.

### 📊 Tableau de bord (`/hud`)
Un tableau latéral affiche votre guilde, votre Âge, les PV du Nexus, l'état du raid/immunité, vos contrats, votre solde et l'événement en cours. `/hud` l'active ou le masque (réglage conservé).

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
* `/money` : Consulter son solde de pièces personnel. *(Les sous-commandes `/money get|set|top|rectify…` restent réservées aux administrateurs.)*
* `/baltop` : Afficher le classement des joueurs les plus riches du serveur.
* `/ah` : Ouvrir l'Hôtel des Ventes et le marché des Hordes Noires.
* `/ah sell <prix>` : Mettre en vente l'objet tenu en main à l'Hôtel des Ventes (**10 annonces actives max** par joueur, `market.max-listings-per-player`).
* Dans `/ah` : **pagination** (flèches ◀ ▶), et un **clic sur sa propre annonce l'annule** et rend l'objet.
* `/age` : Ouvrir le menu de progression des Âges et déposer des ressources.
* `/traite [liste]` : traités entre guildes (alliance, pacte, vassalité).
* `/station`, `/rail` : voyager entre gares / gérer péages, relations et gares de sa guilde.
* `/contrat`, `/classement`, `/histoire`, `/hud` : contrats du jour, classement de saison, mémorial, tableau de bord.

### 🌐 Navigation & Serveur
* `/hub` (ou `/lobby`) : Se reconnecter directement au Hub principal du réseau (raccourci vers `/server hub`).

---

## 🛡️ Intégrité & Anti-Triche (résumé technique)
* **Bonus de minage** (Nain, Industrie, Enclave) : calculés **après** les protections de claim, jamais sur un bloc **posé par un joueur** (mémorisé dans le chunk, persistant et suivi par les pistons).
* **Quotas d'Âge** : indexés sur le **pic de membres** de l'Âge en cours (sortir ses membres avant de déposer ne réduit plus rien).
* **Changement de métier** : 3 reconversions gratuites puis payantes, avec un **délai de 24 h** entre deux changements (`races.change-cooldown-hours`).
* **Hordes** : plafond de **60 monstres** pour une horde sur-mesure (`raids.max-custom-horde-units`) ; la banque n'est débitée qu'une fois la cible validée ; les mobs de raid orphelins sont supprimés au redémarrage.
* **Enchantements** : les niveaux au-delà du maximum vanilla sont ramenés à la normale hors Conclave Arcanique (clic, ramassage, changement de slot, connexion).
* **Alambic** : l'alimentation par entonnoir est soumise à la même restriction que l'ouverture manuelle.
* **Sauvegarde** : les écritures de comptes joueurs sont sérialisées (pas de perte d'argent entre une vente HDV et une déconnexion).

## ✅ Détails d'implémentation récents
* **Arcaniste** : +15 % de dégâts subis en mêlée (attaques directes uniquement).
* **Forgeron** : enclumes, meules et hauts-fourneaux ne sont **craftables** que par les Forgerons jusqu'à l'Âge 3 (l'usage reste libre : les Arcanistes ont besoin de l'enclume pour leurs livres).
* **Enclave Champêtre** : reproduction animale accélérée (cooldown des parents et croissance des bébés divisés par 3) ; les armures en Netherite sont retirées automatiquement (équipement, connexion, vérification périodique).
* **Industrie Souterraine** : boire ou lancer une potion de niveau 2 (`STRONG_*` ou effet d'amplificateur ≥ 1) est refusé.
* **Siège de Pillards** : 1 Ravageur sur 6 et 1 Vindicateur sur 3, le reste en Pillards.
* **Syndicat Marchand** : les 10 % de dividende sont désormais **prélevés** sur la prime du joueur (il touche 90 %) et transférés à la banque : plus de création de monnaie.
* **Cooldowns de raid** : persistés en base (table `raid_cooldowns`, schéma v6) et conservés après un redémarrage.
* **Raids actifs** : par conception, un raid en cours ne survit pas à un redémarrage (ses monstres sont supprimés), mais l'immunité post-raid, elle, est conservée.
* **Performances** : la sauvegarde périodique des joueurs et guildes s'exécute en **une seule transaction** SQLite.
* **Traités** : nouveau paquet `fr.esicraft.treaty` (gestionnaire, menus, protection PvP alliée), branché dans les deux points d'entrée des raids et dans `RailManager.getRelation`.
* **Optimisation HUD** : le tableau latéral n'est plus reconstruit toutes les 2 s ; il n'est mis à jour que si son contenu a changé et seules les lignes obsolètes sont retirées (moins de paquets scoreboard).
* **Site web** : nouveau paquet `fr.esicraft.web` (`WebServer`, `WebSnapshot`, `HtmlPages`), démarré/arrêté avec le plugin.
* **Sécurité (audit du 25/10)** : rupture de traité atomique (deux officiers qui cliquent en même temps ne paient plus deux indemnités), annulation d'une proposition réservée aux officiers, protection alliée étendue aux potions nocives jetées et persistantes.
