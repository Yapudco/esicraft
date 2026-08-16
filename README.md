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
   * **Bonus** : Effets **Force I** et **Résistance I** permanents dans les claims, **-30% de réduction sur l'achat de TOUTES les hordes de mercenaires**, **+30% de dégâts d'attaque** contre les monstres et hordes ennemies.
   * **Contraintes** : Max 1 Nain (Mineur) et 1 Sylvain (Fermier), minerais et récoltes réduits de 25% dans les claims.
2. ⛏️ **Industrie Souterraine (Minage / Production)** :
   * **Bonus** : Effet **Haste II** permanent dans les claims sous $Y \le 30$, **drops de minerais bruts multipliés par x1.5**, coût des pièges de Nexus et Golems réduit de **-50%**.
   * **Contraintes** : Max 1 Rôdeur (combattant à distance), vulnérabilité aux attaques magiques (+20% de dégâts subis).
3. 🌾 **Enclave Champêtre (Ressources & Santé)** :
   * **Bonus** : Effets **Régénération I** et **Saturation** permanents dans les claims, **double récolte agricole**, **double butin sur les animaux**, **récolte doublée sur le bois et les pierres naturelles** (Andésite, Diorite, Granite, Blackstone, Calcite), reproduction animale accélérée (x3).
   * **Contraintes** : Interdiction des armures en Netherite, aucun Ingénieur (pas de TNT), max 1 Forgeron.
4. 💰 **Syndicat Marchand (Commerce & Économie)** :
   * **Bonus** : **+20% de primes** sur l'élimination des monstres, **10% des bénéfices de chaque monstre éliminé automatiquement transférés à la banque de guilde**, bonus de gain de +25% à l'Hôtel des Ventes.
   * **Contraintes** : **Hordes de mercenaires 30% plus chères (+30% sur tous les contrats)**, aucun effet passif de combat, PV max du Nexus réduits de 20%.
5. 🔮 **Conclave Arcanique (Magie & Runes)** :
   * **Bonus** : **+25% de PV max sur le Cœur de Guilde (Nexus)**, **coûts en XP des enchantements et réparations à l'enclume réduits de -50%**, **potions consommées et jetées automatiquement du Tier Supérieur** (+1 niveau d'effet), **enchantements surpuissants débloqués (Niveau IV devient V, et Niveau V devient VI)**, le Nexus tire automatiquement un **rayon d'énergie magique** sur l'assaillant le plus proche (inflige **1 cœur de dégâts par seconde** soit 2.0 PV), dégâts magiques subis réduits de **50%**.
   * **Contraintes** : Coût d'achat des claims augmenté de +50%, max 1 Forgeron et 1 Nain, **perte immédiate des enchantements de niveau VI / V boostés si la guilde change de spécialisation**.

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
* En jetant une **Pomme Dorée** sur le bloc central du Nexus, vous lui rendez instantanément **+500 PV** (hors période de raid actif).

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
> **Restrictions Dimensionnelles & Commerce Villageois par Âge** :
> * 🔥 **Nether** : L'accès au Nether (portails, allumage et téléportations) est **strictement bloqué jusqu'à l'Âge 2 (Métallurgie)**.
> * 🌌 **L'End** : L'accès à l'End (portails, yeux de l'ender et téléportations) est **strictement bloqué jusqu'à l'Âge 3 (Alchimie)**.
> * 🛡️ **Commerce Villageois Forgerons** : Les échanges avec les villageois **Armurier** (*Armorer*), **Forgeron d'armes** (*Weaponsmith*) et **Forgeron d'outils** (*Toolsmith*) sont **bloqués jusqu'à l'Âge 3 (Alchimie)** pour valoriser l'artisanat des joueurs.

1. **Âge Rustique (Âge 1 — Départ)** :
   * *Technologies* : Outils en bois et pierre uniquement (16 claims max). **Dimensions Nether & End verrouillées**. Outils en fer réservés aux spécialistes (Nain, Forgeron, Sylvain).
2. **Âge de la Métallurgie (Âge 2)** :
   * *Déblocages* : **Accès au Nether débloqué** 🔥, outils en fer universels, boucliers et extension à **32 claims max**. **Les crafts en diamant sont réservés aux métiers spécialisés** (Nain: pioche/pelle, Forgeron: armures/épée/hache, Sylvain: houe).
   * *Objectifs Requis (Quotas réduits de 50%)* :
     * ⛏ 1 250 Lingots de Cuivre | 750 Fer Brut | 1 000 Bûches de Chêne
     * 🌾 **1 500 Blé** | 🥕 **750 Carottes** *(Alimentation & maraîchage)*
3. **Âge de l'Alchimie (Âge 3)** :
   * *Déblocages* : **Accès à l'End débloqué** 🌌, **démocratisation complète du diamant pour tous**, armures complètes en fer pour tous et extension à **64 claims max**. **Les crafts en Netherite deviennent accessibles exclusivement aux métiers spécialisés** (Nain: pioche/pelle, Forgeron: armures/épée/hache, Sylvain: houe).
   * *Objectifs Requis* :
     * ⛏ 3 000 Lingots de Fer | 1 000 Lingots d'Or | 500 Verrues du Nether
     * 🥔 **5 000 Pommes de Terre** | 🎋 **5 000 Cannes à Sucre** | 🎃 **800 Citrouilles**
4. **Âge de Guerre Totale (Âge 4)** :
   * *Déblocages* : **TNT et mécanismes offensifs (canons à TNT, brèches, pistons, dispensers, redstone avancée) démocratisés pour tous**, alambics et potions T2 universels, arcs et arbalètes pour tous, et extension à **128 claims max**.
   * *Objectifs Requis* :
     * ⛏ **500 Diamants** | **1 500 Obsidienne** | **500 Perles de l'End**
     * ✨ **2 500 Carottes Dorées** | 🌾 **800 Bottes de Foin** | 🍯 **500 Bouteilles de Miel**
     * 🔴 **3 500 Poudre de Redstone** | ⚙ **250 Répéteurs** | 🧱 **200 Pistons**
     * 💣 **1 000 Poudres à Canon** | 🧨 **100 Blocs de TNT**
   * *🌟 **Bonus de Belligérance pour les Joueurs ayant atteint l'Âge 4** :*
     * ⚡ **Mobilité de Guerre** : Effet permanent **Vitesse I (Speed I)**.
     * ⚔ **Combativité** : **+10% de dégâts infligés** (mêlée et tir à distance) et **+25% d'XP supplémentaire**.
     * 🛡 **Blindage de Tranchée** : **-20% de dégâts subis face aux explosions** (TNT et creepers).
5. **Âge Mythique & Apex (Âge 5 — Âge Final)** :
   * *Déblocages* : **Démocratisation complète de la Netherite pour tous**, balises et **territoires illimités**.
   * *Objectifs Requis* :
     * ⛏ 150 Fragments de Netherite | 5 Étoiles du Nether | 64 Souffles de Dragon
     * 🍏 **5 Pommes Dorées Enchantées** | 🌸 **1 000 Fruits de Chorus**
     * 🌌 **64 Carapaces de Shulker** | 🪐 **2 500 Pierres de l'End** | 🟣 **500 Blocs de Purpur**
     * 🔴 **500 Blocs de Redstone** | 🎛 **200 Comparateurs** | 👁 **200 Observateurs**
   * *🌟 **Bénédictions & Bonus Apex pour les Joueurs ayant atteint l'Âge 5** :*
     * 👑 **Aura Souveraine Permanente** : Effets constants **Hâte I** (minage fluide), **Vitesse I** et **Résistance I**.
     * ⚔ **Puissance Martiale & Savoir** : **+15% de dégâts bruts infligés** au combat et **+50% d'XP supplémentaire** sur toutes les sources.
     * 🏛 **Empire Sans Limites** : Revendications de chunks **100% illimitées** et **accès total à tous les crafts en Netherite** pour tous les membres de la guilde.

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
