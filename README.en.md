# ⚔️ OFFICIAL PLAYER GUIDE — ESICRAFT SERVER

🇫🇷 [Version française](README.md)

Welcome to **Esicraft**, a strategic hardcore survival server built around guilds, professions, a dynamic economy and territorial wars!

This guide covers every feature, mechanic, profession and command on the server so you can build your empire and crush your rivals.

> [!NOTE]
> The in-game interface is in French. This guide gives English names, with the in-game French name in parentheses where it helps (e.g. *Nain*, *Âge*, `/traite`). Commands are typed exactly as written.

---

## 📑 Table of Contents
1. [🎭 The 6 Professions (Races)](#-1-the-6-professions-races)
2. [🏛️ Guilds & Territories](#️-2-guilds--territories)
3. [🌟 The 5 Guild Specializations](#-3-the-5-guild-specializations)
4. [💎 The Base Core: the Nexus & Traps](#-4-the-base-core-the-nexus--traps)
5. [⚔️ Black Hordes & Siege Raids](#️-5-black-hordes--siege-raids)
6. [💰 Economy, Auction House & PvP Bounty](#-6-economy-auction-house--pvp-bounty)
7. [⏳ Technological Ages](#-7-technological-ages)
8. [🎉 Contracts, Events, Seasons, Railways, Treaties & Website](#-8-contracts-events-seasons-railways-treaties--website)
9. [📋 Full Command List](#-9-full-command-list)

---

## 🎭 1. The 6 Professions (Races)

> [!IMPORTANT]
> **Profession required**: a player who has not chosen a profession yet (`/race`) **cannot break any block**. Picking a race is mandatory as soon as you arrive if you want to mine, harvest and build.

As soon as you join, choose your profession with `/race`. Each profession has exclusive crafting rights, unique bonuses and balanced drawbacks:

| Profession | Role | Exclusive Crafts & Uses | Passive & Active Bonuses | Restrictions & Drawbacks |
| :--- | :--- | :--- | :--- | :--- |
| **⛏ Miner (Dwarf / *Nain*)** | Mining & geology | • Iron pickaxe (Age 1)<br>• Diamond pickaxe & shovel (Age 2)<br>• Netherite pickaxe & shovel (Ages 3-4) | • Permanent **Haste II** and **Night Vision** below $Y \le 0$<br>• **+15% chance to double** every ore mined | • **Slowness I** in open air and full sunlight ($Y > 60$)<br>• Cannot craft metal weapons or armor |
| **🌿 Sylvan (Elf / *Sylvain*)** | Farming, breeding & nature | • Iron hoe (Age 1), diamond (Age 2), Netherite (Ages 3-4)<br>• Bone meal use (Ages 1-2)<br>• Animal breeding (Ages 1-2)<br>• Crafted golden apples | • Permanent **Regeneration I aura** for themselves and guild allies within 4 blocks<br>• **Instant right-click harvest** of mature crops (with automatic replanting) | • **-20% melee damage** dealt |
| **🔨 Blacksmith (*Forgeron*)** | Heavy forging, armor & metal | • Full iron armor, swords & shields (Age 1)<br>• Diamond armor, sword & axe (Age 2)<br>• Netherite armor, sword & axe (Ages 3-4)<br>• Anvils, grindstones & blast furnaces | • Permanent **Fire Resistance**<br>• **+35% chance to double** an ingot taken from a furnace/blast furnace<br>• **-50% XP cost** at the anvil | • **+25% hunger drain** during physical effort |
| **🏹 Ranger (*Rôdeur*)** | Ranged hunting & exploration | • **Only profession able to use and craft Bows and Crossbows** up to and including Age 3 (unlocked for everyone at Age 4)<br>• Crafted arrows and spectral arrows | • **Every arrow shot becomes a Poison arrow (Poison I)**<br>• **+25% ranged damage** on all projectiles<br>• Permanent **Speed I** in natural biomes (plains, forests, jungles, taigas) | • **Slowness I** while wearing a heavy chestplate (diamond or Netherite) |
| **🧪 Arcanist (*Arcaniste*)** | Magic, alchemy & enchanting | • **Only profession able to enchant items** (enchanting table & enchanted books on an anvil)<br>• **Only profession able to use and throw Splash Potions**<br>• Exclusive use and crafting of Brewing Stands up to Age 3 (Alchemy)<br>• Crafted golden apples | • **+30% XP** from every source<br>• **+25% duration** on potions drunk<br>• **-20% magic damage taken** (harming potions, poison, wither, dragon's breath) | • **+15% physical damage taken** in close combat (melee fragility) |
| **⚙ Engineer (*Ingénieur*)** | Automation, redstone & machines | • Pistons, sticky pistons, hoppers, observers, repeaters, comparators, dispensers, droppers & **TNT** (Ages 1-3) | • **-40% explosion damage taken** (TNT, creepers, beds)<br>• Exclusive mastery of TNT and automation | • **-20% melee damage** dealt |

> [!TIP]
> **Profession commands**:
> * `/race`: opens the profession selection / retraining menu (first choice free + 3 free retrainings, then 10,000 coins, with a 24 h delay between two changes).
> * `/race show [player]`: opens the **interactive Profession Directory** showing the head, profession, guild, technological Age, balance and stats of every online player.

---

## 🏛️ 2. Guilds & Territories

Found a guild with your allies to claim land and build your fortress.

### 👑 Guild Hierarchy
* **Guild Leader (`LEADER`)**: full control over the guild, appoints officers (`/guild promote`), transfers leadership (`/guild transfer`) or disbands the guild (`/guild disband`).
* **Officer (`OFFICER`)**: can claim/unclaim territory, place the Nexus, buy traps and **hire / launch mercenary hordes**.
* **Member / Recruit (`MEMBER`)**: takes part in bank deposits, Age donations and collective defense.
* **Member limit**: **10 players maximum** per guild (`guilds.max-members-default`). A player can only belong to **one guild**; invitations expire after **5 minutes**.
* **Composition**: specialization constraints (forbidden/limited professions, Legion: at least 50% Rangers/Blacksmiths) are checked **every time someone joins** (invitation, allegiance after an annexation) and on every profession change.
* **Disbanding**: `/guild disband` is refused during a raid or within **10 minutes** of an attack on the Nexus (you can't dodge the War Treasury).

### 🗺️ Territorial Expansion & Claims
* **Contiguity required**: the first chunk is free. Every following chunk **must be adjacent** to land your guild already owns. Conversely, `/guild unclaim` is refused if it **splits the territory into several pieces** or targets the Nexus chunk.
* **Capacity unlocked by Age**:
  * **Age 1 (Rustic)**: **16 chunks max**
  * **Age 2 (Metallurgy)**: **32 chunks max**
  * **Age 3 (Alchemy)**: **64 chunks max**
  * **Age 4 (Total War)**: **128 chunks max**
  * **Age 5 (Mythic & Apex)**: **Unlimited**
* **Gentle progressive pricing**:
  * Each new chunk costs $\text{Price} = 100 + (N \times 25\text{ coins})$ where $N$ is the number of chunks already claimed (e.g. 100 coins, then 125, 150, 175, 200...).
  * The price is **taken directly from the Guild Bank**.

### 🧱 Protection & Security in Enemy Territory
* **Full block protection**: you cannot break or place blocks on another guild's territory.
* **Container protection**: only guild members can open the chests, barrels, shulkers, furnaces and hoppers inside their claims.
* **Combat restrictions in enemy land**:
  * ❌ **No TNT placement**.
  * ❌ **No TNT minecarts** (placement, dispenser launch, entering on rails and explosion are blocked).
  * ❌ **No Ender Pearls** (throwing and teleporting into an enemy claim are blocked).
  * ❌ **No End Crystals** (placement and explosion are blocked).
  * ❌ **Chorus Fruit** (teleporting into an enemy claim), **pistons**, **lava / water / fire** coming from outside, **Wither / Enderman / Ravager** block destruction and **projectiles** hitting animals, item frames and armor stands: all blocked inside claims (except during a raid on that guild).
  * ⚠️ **Vulnerable territory**: after its Nexus is destroyed without an annexation, a guild's territory loses its block/explosion protection for **30 minutes**.

---

## 🌟 3. The 5 Guild Specializations

Pick your guild's collective specialization from the `/guild spec` menu:

1. ⚔️ **Warrior Legion (PvP / Conquest)**:
   * **Bonuses**: permanent **Strength I** and **Resistance I** inside claims, **-30% on ALL mercenary hordes**, **+30% attack damage** against monsters and enemy hordes.
   * **Constraints**: max 1 Dwarf (Miner) and 1 Sylvan (Farmer), ore and crop yields reduced by 25% inside claims.
2. ⛏️ **Underground Industry (Mining / Production)**:
   * **Bonuses**: permanent **Haste II** inside claims below $Y \le 30$, **raw ore drops ×1.5**, Nexus traps and Golems **-50%**.
   * **Constraints**: max 1 Ranger (ranged fighter), weak to magic attacks (+20% damage taken), **level II potions forbidden**.
3. 🌾 **Rural Enclave (Resources & Health)**:
   * **Bonuses**: **+2 hearts of base max health (+4.0 HP for every member)**, permanent **Regeneration I** and **Saturation** inside claims, **double crop harvests**, **double animal loot**, **double yield on wood and natural stone** (Andesite, Diorite, Granite, Blackstone, Calcite), faster animal breeding (×3).
   * **Constraints**: no Netherite armor (removed automatically), no Engineer (no TNT), max 1 Blacksmith.
4. 💰 **Merchant Syndicate (Trade & Economy)**:
   * **Bonuses**: **exclusive right to trade with Villagers and Wandering Traders during Ages 1 to 3** (other guilds unlock villager trading at Age 4!), **+20% bounties** on monster kills, **10% of every monster bounty goes to the guild bank** (taken from the player's share), and **0% tax** at the Auction House.
   * **Constraints**: **no Arcanist allowed in the guild**, **mercenary hordes cost 30% more**, no passive combat effects, Nexus max HP reduced by 20%.
5. 🔮 **Arcane Conclave (Magic & Runes)**:
   * **Bonuses**: **+25% max HP on the Guild Core (Nexus)**, **-50% XP cost for enchanting and anvil repairs**, **potions drunk or thrown are automatically one tier higher** (+1 effect level), **overpowered enchantments unlocked (level IV becomes V, level V becomes VI)**, the Nexus automatically fires a **magic energy beam** at the nearest attacker (1 heart = 2.0 HP of damage every 2 seconds), **50% less magic damage** taken.
   * **Constraints**: **-2 hearts of base max health (-4.0 HP for every member)**, claims cost 50% more, max 1 Blacksmith and 1 Dwarf, **boosted level VI / V enchantments are lost immediately if the guild changes specialization**.

> [!IMPORTANT]
> **Specialization change rule**: the first choice is immediate. After that, a guild can only change specialization **when a NEW player** (who has never belonged to the guild before) joins! This rewards active recruiting and prevents permanent opportunistic switching.

---

## 💎 4. The Base Core: the Nexus & Traps

Every guild has a **Nexus** (base core) that it must place inside its territory with `/guild nexus place`.

### 🛡️ Nexus Safety Perimeter (16 blocks)
* **Placement check**: the server scans the whole **16-block radius** around the Nexus before allowing it. Any **water**, **lava** or **obsidian** in that area **blocks the placement**.
* **No fluids at all (lava & water)**: you cannot place water or lava, empty a bucket, dispense a fluid or let one flow within **16 blocks of the Nexus**.
* **No obsidian**: placing obsidian or crying obsidian is strictly blocked within the **16-block radius**, to keep sieges fast and dynamic.

### 💖 Healing the Nexus
* By throwing a **Golden Apple** onto the Nexus's central block (or right-clicking it), **guild members** instantly restore **+500 HP** (**+2,500 HP** for an Enchanted Golden Apple), except during an active raid.
* **30-second cooldown** between two heals (`guilds.nexus.heal-cooldown-seconds`).
* A Nexus **cannot be placed again** while the guild already has one (no free reset to 100% HP).
* **Purchased traps are saved** and survive restarts. The **Guardian Golem** now attacks intruders; traps and the arcane beam only target non-members **standing inside the claim**.

### 💣 The 5 Defensive Traps (`/nexus traps`)
* 🔥 **Infernal Blaze (250 coins)**: sets enemies within 8 blocks on fire with continuous burns.
* 🕸️ **Binding Web (200 coins)**: gives Slowness III and Blindness to attackers approaching the Nexus.
* ⚡ **Retaliation Lightning (350 coins)**: violently strikes anyone who hits the Nexus (+15 damage).
* 🧪 **Toxic Mist (300 coins)**: releases a poison cloud (Poison II) that chokes raiders and attacking monsters.
* 🤖 **Guardian Automaton (500 coins)**: summons a loyal armored iron golem that fiercely defends the Nexus.
* *Note: traps are paid from the Guild Bank first, stay armed for the next horde, and are 50% cheaper for the Underground Industry.*

---

## ⚔️ 5. Black Hordes & Siege Raids

Hire armies of mercenary monsters to besiege and destroy a rival guild's Nexus!

### 🎯 Access & Funding
* **Access**: open the Auction House with `/ah` and click the **Wither Skull** (slot 49).
* **Required rank**: only **Officers** and the **Leader** can set up and launch hordes.
* **Funding**: the cost is **100% paid from the Guild Bank** (`/guild bank`).
* **Defender present**: a horde can only be launched if at least one member of the target guild is online **inside their own claims**.

### 📦 Horde Types
1. **Preset contracts**: armored zombies, incendiary skeletons, venomous spiders, Pillager Siege (1 Ravager in 6 and 1 Vindicator in 3, the rest Pillagers)...
2. **Custom Horde Builder**: build your own army unit by unit from 8 monster classes (up to **60 monsters**, `raids.max-custom-horde-units`, depending on your guild bank funds).

### 💥 Special Invasion Options
* 💣 **Explosive Sappers**: monsters blocked by trenches or walls explode to open a breach and **instantly evaporate every water and lava source within 5 blocks**.
* 🔥 **Fireproofing (Lava & Fire Resistance)**: your monsters get permanent fire resistance and cross enemy lava moats without taking damage.

### ⭐ Economic Hegemony (Bounty on Wealthy Guilds)
To prevent crushing financial monopolies and keep faction wars alive:
* **Hegemony status triggers automatically** if the target guild meets at least one of these conditions:
  1. **At least 50% of its members** are in the server's **Top 5 richest players** (`/baltop`).
  2. **Its Guild Bank holds more than 30%** of the server's total money supply.
* **Devastating effects against a Hegemonic guild**:
  * 📉 **Horde cost halved (-50%!)**: every horde (preset or custom) launched against this guild costs **half price**.
  * ⏱ **Raid cooldown reduced to 2 minutes**: the post-raid immunity of this guild lasts **only 2 minutes** instead of 30.
  * 🛡 **Badge in the target picker**: guilds with this status show a gold star `⭐ HÉGÉMONIE ÉCONOMIQUE` in the raid menu.

### 🏴 Territory Annexation & War Treasury
* If a guild's Nexus is destroyed during a raid:
  * 🗺️ **Territory transfer**: the defeated guild's land is **instantly annexed** by the winning guild, **up to the claim cap of its Age** (extra chunks go back to the wild).
  * 💰 **Guild Bank merge (War Treasury)**: the defeated guild's entire treasury is **automatically transferred into the winner's bank**.
  * 🤝 **Allegiance choice for the defeated**:
    * `/guild annex join`: join the winning guild as a member.
    * `/guild annex decline`: refuse and become a free player with no guild.

---

## 💰 6. Economy, Auction House & PvP Bounty

### 🪙 Earning Coins & Hunting Bounties
Every monster killed pays a bounty straight to your account, based on how dangerous it is:

| Category | Monsters | Bounty per Kill |
| :--- | :--- | :--- |
| 🐉 **Legendary Bosses** | • **Ender Dragon**<br>• **Warden**<br>• **Wither**<br>• **Elder Guardian** | **750.00 coins**<br>**500.00 coins**<br>**350.00 coins**<br>**150.00 coins** |
| 👺 **Mini-Bosses & Sieges** | • **Ravager**<br>• **Evoker**<br>• **Piglin Brute** | **75.00 coins**<br>**40.00 coins**<br>**25.00 coins** |
| 🔥 **Nether & End (Dangerous)** | • **Ghast** & **Shulker**<br>• **Wither Skeleton** & **Guardian**<br>• **Blaze** 🔥 & **Hoglin / Zoglin** | **20.00 coins**<br>**15.00 coins**<br>**12.00 coins** |
| 🏹 **Overworld Elite** | • **Vindicator**<br>• **Witch**<br>• **Enderman**<br>• **Pillager** | **15.00 coins**<br>**12.00 coins**<br>**10.00 coins**<br>**8.00 coins** |
| 🧟 **Common Monsters** | • **Creeper** & **Phantom**<br>• **Piglin** & **Zombified Piglin**<br>• **Spiders (Normal / Cave)**<br>• **Skeletons & Strays**<br>• **Zombies (Drowned, Husks, Villagers)** | **5.00 coins**<br>**4.00 coins**<br>**3.00 - 3.50 coins**<br>**3.00 coins**<br>**2.50 coins** |
| 🐛 **Small Monsters** | • **Slimes & Magma Cubes**<br>• **Vex**<br>• **Silverfish & Endermites** | **2.00 coins**<br>**2.50 coins**<br>**1.50 coins** |

*Note: members of a **Merchant Syndicate** guild get a passive **+20% bonus** on every monster bounty. Repetitive mob farms are detected and pay less (anti-farm).*

1. **Auction House (`/ah`)**: sell your resources and crafted gear with `/ah sell <price>`.
2. **Nexus defense**: killing the monsters of an attacking horde yields valuable loot.

### ⚔️ PvP Kill Bounty (Outside Your Guild)
When you kill an enemy player:
* **The winner (killer)** receives exactly what the victim loses: **100 coins + 1% of the victim's fortune**.
* **The victim** loses:
  * **If they have more than 100 coins**: **100 coins + 1% of their fortune**.
  * **If they have 100 coins or less**: **all of their money** (balance drops to `0.00`).
* No bounty if the same pair of players fought less than 2 minutes ago (anti-farm).
* *(Inactive between members of the same guild, and between allied guilds, to protect allies.)*

---

## ⏳ 7. Technological Ages

The server progresses through **6 major technological Ages**. To move your guild to the next Age and unlock higher technologies and dimensions, members must pool their efforts and donate **ores and farm produce** through `/age`. Quotas scale with the **peak member count** reached during the current Age.

> [!CAUTION]
> **Dimension & villager trading restrictions by Age**:
> * 🔥 **Nether**: access to the Nether (portals, lighting and teleports) is **strictly blocked until Age 2 (Metallurgy)**.
> * 🌌 **The End**: access to the End (portals, eyes of ender and teleports) is **strictly blocked until Age 3 (Alchemy)**.
> * 🛡️ **Villager trading**: trading with villagers and wandering traders is **reserved for the Merchant Syndicate during Ages 1 to 3**, then **opened to every guild from Age 4 (Total War)**.

1. **Rustic Age (Age 1 — Start)**:
   * *Technologies*: wood and stone tools only (16 claims max). **Nether & End locked**. Iron tools reserved for specialists (Dwarf, Blacksmith, Sylvan).
2. **Age of Metallurgy (Age 2)**:
   * *Unlocks*: **Nether access** 🔥, iron tools for everyone, shields, and expansion to **32 claims max**. **Diamond crafts are reserved for specialized professions** (Dwarf: pickaxe/shovel, Blacksmith: armor/sword/axe, Sylvan: hoe).
   * *Required (quotas reduced by 50%)*:
     * ⛏ **750 Copper Ingots** | 🐮 **256 Leather** | 750 Raw Iron | 1,000 Oak Logs
     * 🌾 **1,500 Wheat** | 🥕 **750 Carrots** *(food & market gardening)*
3. **Age of Alchemy (Age 3)**:
   * *Unlocks*: **End access** 🌌, **diamond for everyone**, full iron armor for everyone, and expansion to **64 claims max**. **Netherite crafts become available only to specialized professions** (Dwarf: pickaxe/shovel, Blacksmith: armor/sword/axe, Sylvan: hoe).
   * *Required*:
     * ⛏ 3,000 Iron Ingots | 1,000 Gold Ingots | 500 Nether Wart
     * 🥔 **5,000 Potatoes** | 🎋 **5,000 Sugar Cane** | 🪶 **3,000 Feathers** | 🎃 **800 Pumpkins**
4. **Age of Total War (Age 4)**:
   * *Unlocks*: **villager trading for everyone**, **TNT and offensive machinery (TNT cannons, breaches, pistons, dispensers, advanced redstone) for everyone**, brewing stands and tier II potions for everyone, bows and crossbows for everyone, and expansion to **128 claims max**.
   * *Required*:
     * ⛏ **500 Diamonds** | **1,500 Obsidian** | **500 Ender Pearls** | 🪽 **1 pair of Elytra**
     * ✨ **2,500 Golden Carrots** | 🌾 **800 Hay Bales** | 🍯 **500 Honey Bottles**
     * 🔴 **3,500 Redstone Dust** | ⚙ **250 Repeaters** | 🧱 **200 Pistons**
     * 💣 **1,000 Gunpowder** | 🧨 **100 TNT**
   * *🌟 **Warfare bonuses for players who reached Age 4**:*
     * ⚡ **War mobility**: permanent **Speed I**.
     * ⚔ **Combativeness**: **+10% damage dealt** (melee and ranged) and **+25% extra XP**.
     * 🛡 **Trench plating**: **-20% explosion damage taken** (TNT and creepers).
5. **Mythic & Apex Age (Age 5)**:
   * *Unlocks*: **Netherite for everyone**, beacons and **unlimited territory**.
   * *Required*:
     * ⛏ 150 Netherite Scrap | 5 Nether Stars
     * 🍏 **5 Enchanted Golden Apples** | 🌸 **1,000 Chorus Fruit**
     * 🌌 **64 Shulker Shells** | 🪐 **2,500 End Stone** | 🟣 **500 Purpur Blocks**
     * 🔴 **500 Redstone Blocks** | 🎛 **200 Comparators** | 👁 **200 Observers**
   * *🌟 **Apex blessings for players who reached Age 5**:*
     * 👑 **Permanent Sovereign Aura**: constant **Haste I**, **Speed I** and **Resistance I**.
     * ⚔ **Martial power & knowledge**: **+15% raw damage dealt** in combat and **+50% extra XP** from every source.
     * 🏛 **Limitless empire**: **100% unlimited** chunk claims and **full access to every Netherite craft** for all guild members.
6. 👑 **Age of Supremacy (Age 6 — The Ultimate, Sacred Age)**:
   * *Description*: the ultimate, divine peak of civilization on the server, demanding the rarest relics and masterpieces in the universe.
   * *Required*:
     * 🎶 **Every Music Disc**: 13, Cat, Blocks, Chirp, Far, Mall, Mellohi, Stal, Strad, Ward, 11, Wait, Otherside, 5, Pigstep, Relic.
     * 💀 **Every Mob Head**: Wither Skeleton Skull, Zombie Head, Skeleton Skull, Ender Dragon Head, Creeper Head, Piglin Head.
     * 📯 **Every Goat Horn (8 horns)**.
     * ⭐ **64 Nether Stars**.
     * 🌾 **1 Netherite Hoe**, enchanted with **Mending + Unbreaking III + Fortune III**.
     * 🛡 **Every Smithing Template**: Coast, Dune, Eye, Host, Raiser, Rib, Sentry, Shaper, Silence, Snout, Spire, Tide, Vex, Ward, Wayfinder, Wild, Netherite Upgrade.
     * 🏺 **Every Pottery Sherd (20)**: Angler, Archer, Arms Up, Blade, Brewer, Burn, Danger, Explorer, Friend, Heart, Heartbreak, Howl, Miner, Mourner, Plenty, Prize, Sheaf, Shelter, Skull, Snort.
     * 🥚 **The Ender Dragon Egg** (`1 Dragon Egg`).
     * 🌿 **A Dead Bush** (`1 Dead Bush`).
   * *🌟 **Supreme rewards & titles for conquering Age 6**:*
     * 🥚 **Ancestral relic**: **1× Ender Dragon Egg** for every member.
     * 💎 **Treasure**: **1× Block of Netherite** for every member.
     * 💬 **Dynamic chat title based on play time**:
       * If your current session lasts **less than 1 hour**: glory prefix **`[Giga Tchad]`** in chat!
       * If your current session lasts **more than 1 hour**: prestige prefix **`[Gros Nerd]`** in chat!

---

## 🎉 8. Contracts, Events, Seasons, Railways, Treaties & Website

### 📜 Daily Contracts (`/contrat`)
Every day (reset at midnight, Paris time), each player with a profession gets **3 contracts**: one specific to their profession (Miner: ores, Sylvan: crops, Blacksmith: smelted ingots, Ranger: ranged kills, Arcanist: enchantments, Engineer: redstone components), a hunting contract and a mining/harvesting contract. Each contract pays coins, and **completing all 3 gives +400 coins**. Blocks placed by players, repetitive mob farms and redstone place/break loops don't count.

### 🌍 Server Events (every 45 to 90 min, `events.*` in `config.yml`)
* 🌑 **Blood Moon** (10 min): more monsters at night, **hunting bounties ×2**.
* ☄ **Meteor Shower** (3 min): ore blocks (up to ancient debris) crash down near players, outside guild territories.
* 🧙 **Black Market** (5 min): a wandering merchant appears (approximate coordinates announced) and trades rare items for diamonds — open to everyone, whatever their Age.
* ☠ **World Boss** (15 min): the *Lord of Ashes* (800 HP). **3,000 coins** shared according to damage dealt; the top fighter wins a Nether Star and 8 diamonds.
* Admins: `/evenement <lune|meteores|marchand|boss|stop|status>`.

### 🏆 Season & Memorial
* `/classement`: monthly guild ranking (territory, Age, wealth, members, raids repelled, conquests). The champion receives **5,000 coins** in its bank, **500 per member** and the 👑 crown in chat.
* `/histoire`: the last 10 notable events (conquests, repelled raids, bosses, champions, treaties).

### 🚆 Inter-Guild Railway Network (`/station`, `/rail`)
* **Travel (`/station`)**: within 8 blocks of a `[Gare]` sign, `/station` opens a menu of the stations reachable **through a continuous rail path**. The network is a **graph**: junctions, crossings and rails placed side by side are all connected (so a line can go "both left and right"). Crossed chunks are loaded on demand; the search is spread over several ticks (network limited to `rail.search-max-rails` rails).
* **Fare**: station fee (`rail.base-fare`, 1 coin, paid to the guild owning the arrival station) + **a toll for every guild whose territory the trip crosses** (paid to its bank). Your own guild doesn't charge you; **allied** guilds travel for free.
* **Cooperation & rivalry (`/rail`, officers)**: set your toll (0 to `rail.max-toll`) and, for each other guild, a **Normal / Allied (free) / Banned** relation. A banned guild can no longer cross your land: **the lines through it are cut** for them (the search goes around the area, or the station becomes unreachable).
* **Build a station**: an officer places a sign with `[Gare]` on line 1 and the name on line 2, within 2 blocks of a rail (Age 2 minimum, `rail.station-creation-cost` from the guild bank, 6 stations max per guild, not inside another guild's territory). Breaking the sign removes the station (only officers of the owning guild).

### 📜 Guild Treaties (`/traite`)
Only **officers** can act from this menu (every member can read it with `/traite liste`). A proposal is sent to the other guild (valid for `treaties.proposal-minutes`, 10 min), which accepts it (left click) or refuses it (right click).
* **🤝 Alliance** (unlimited): no raids between the two guilds, **free rail travel** (relation locked in `/rail`) and **no damage between allied players** (`treaties.protect-allies-pvp`), including harmful splash and lingering potions.
* **📜 Non-Aggression Pact** (`treaties.pact-days`, 7 days): no raids between the two guilds for the duration of the pact.
* **👑 Vassalage**: the vassal pays `treaties.vassal-tribute-percent` (5%) of its bank to its overlord every day; same benefits as an alliance. A guild can only be the vassal of one other guild. The overlord can free its vassal at no cost.
* **💔 Betrayal**: breaking an alliance, an ongoing pact or leaving your overlord costs an **indemnity** (`treaties.break-indemnity`, 500 coins max, capped at the bank balance) paid to the former partner and announced to the whole server, and you cannot attack that guild for `treaties.break-grace-minutes` (30 min).
* Treaties are stored in the database (`treaties`, `treaty_grace` tables), written into the season memorial, and removed if a guild disappears. The "defender present" raid rule is unchanged.

### 🌐 Public Website (`web.*` in `config.yml`)
The plugin runs a **read-only website**, open to everyone: `http://<server-ip>/` (port `web.port`, **80** by default).
* **Pages**: home (online players, season, event, top guilds, recent history), `/guildes` (ranking), `/guilde/<id>` (members, treaties, stats), `/joueurs` (online + richest players) and `/api/stats.json` (JSON).
* **Design**: automatic dark/light theme, rounded cards, colored avatars, medals and score bars, history timeline, blurred sticky header, fully responsive (mobile included). No external resources and no scripts.
* **Security**: HTTP server built into the JDK (no dependency), GET/HEAD only, escaped text, CSP / anti-iframe / no-referrer headers. Pages are **rendered once per snapshot** (every `web.refresh-seconds`) and served from memory, with a **per-IP request limit** (`web.rate-limit`, 429 response), a bounded queue and timeouts: a flood can neither fill the memory nor slow down the Minecraft server. **No coordinates** (Nexus, bases, claims) and no IP addresses are ever shown, and *vanished* players are hidden; Nexus HP is hidden by default (`web.show-nexus-health`), banks and balances are configurable (`web.show-guild-banks`, `web.show-balances`). Behind a reverse proxy every visitor shares the proxy's IP: raise `web.rate-limit.requests` in that case.
* **Port 80 on Linux**: requires root or `setcap 'cap_net_bind_service=+ep' $(readlink -f $(which java))`; otherwise set `web.port: 8080`. If the port can't be opened, the plugin keeps running normally and says so in the console. Remember to open the port on your router / firewall.

### 📊 Dashboard (`/hud`)
A sidebar shows your guild, your Age, Nexus HP, raid/immunity status, treaties, contracts, balance and the current event. `/hud` toggles it on or off (setting saved).

---

## 📋 9. Full Command List

### 🎭 Professions & Races
* `/race`: open the profession selection menu.
* `/race show [player]`: profession directory of online players.
* `/guide`: get / open the official interactive guide book.

### 🏛️ Guild Management
* `/guild create <name> <tag>`: found a new guild.
* `/guild invite <player>`: invite a player to the guild *(Officer/Leader)*.
* `/guild join <name>`: accept a guild invitation.
* `/guild leave`: leave your current guild.
* `/guild kick <player>`: kick a player *(Officer/Leader)*.
* `/guild promote <player>`: promote a player to Officer or demote them *(Leader)*.
* `/guild demote <player>`: demote an officer to regular member *(Leader)*.
* `/guild transfer <player>`: hand guild leadership to a member *(Leader)*.
* `/guild disband`: disband the guild *(Leader)*.
* `/guild info [name]`: show a guild's stats, claims, bank and status.

### 🗺️ Territory & Defense
* `/guild claim`: claim the current chunk for your guild *(Officer/Leader)*.
* `/guild claim show`: toggle the highlight of your territory borders.
* `/guild unclaim`: give up the current chunk *(Officer/Leader)*.
* `/guild spec`: open the guild specialization menu.
* `/guild nexus place`: place the guild Nexus *(Officer/Leader)*.
* `/nexus traps`: buy and arm the Nexus's defensive traps.
* `/guild annex <join|decline>`: accept or refuse to join the winning guild after a defeat.

### 💰 Economy & Market
* `/guild bank <amount>`: deposit money into the guild bank.
* `/pay <player> <amount>`: instantly send coins to another player.
* `/money`: check your personal balance. *(The `/money get|set|top|rectify…` subcommands are admin-only.)*
* `/baltop`: show the server's richest players.
* `/ah`: open the Auction House and the Black Horde market.
* `/ah sell <price>`: list the item in your hand on the Auction House (**10 active listings max** per player, `market.max-listings-per-player`).
* Inside `/ah`: **pagination** (◀ ▶ arrows), and **clicking your own listing cancels it** and gives the item back.
* `/age`: open the Age progression menu and donate resources.

### 🎉 Gameplay & Diplomacy
* `/traite [liste]`: guild treaties (alliance, pact, vassalage).
* `/station`, `/rail`: travel between stations / manage your guild's tolls, relations and stations.
* `/contrat`, `/classement`, `/histoire`, `/hud`: daily contracts, season ranking, memorial, dashboard.

### 🌐 Navigation & Server
* `/hub` (or `/lobby`): reconnect directly to the network's main Hub (shortcut for `/server hub`).

---

## 🛡️ Integrity & Anti-Cheat (technical summary)
* **Mining bonuses** (Dwarf, Industry, Enclave): computed **after** claim protection, never on a block **placed by a player** (stored in the chunk, persistent and tracked through pistons).
* **Age quotas**: indexed on the **peak member count** of the current Age (kicking members before donating no longer lowers anything).
* **Profession change**: 3 free retrainings then paid ones, with a **24 h delay** between two changes (`races.change-cooldown-hours`).
* **Hordes**: cap of **60 monsters** for a custom horde (`raids.max-custom-horde-units`); the bank is only charged once the target is validated; orphaned raid mobs are removed on restart.
* **Enchantments**: levels above the vanilla maximum are brought back to normal outside the Arcane Conclave (click, pickup, slot change, login).
* **Brewing stand**: hopper feeding follows the same restriction as opening it by hand.
* **Saving**: player account writes are serialized (no money lost between an Auction House sale and a disconnection).

## ✅ Recent Implementation Details
* **Arcanist**: +15% melee damage taken (direct attacks only).
* **Blacksmith**: anvils, grindstones and blast furnaces can only be **crafted** by Blacksmiths up to Age 3 (using them stays free: Arcanists need the anvil for their books).
* **Rural Enclave**: faster animal breeding (parent cooldown and baby growth divided by 3); Netherite armor is removed automatically (equipping, login, periodic check).
* **Underground Industry**: drinking or throwing a level II potion (`STRONG_*` or amplifier ≥ 1) is refused.
* **Pillager Siege**: 1 Ravager in 6 and 1 Vindicator in 3, the rest Pillagers.
* **Merchant Syndicate**: the 10% dividend is now **taken from** the player's bounty (they get 90%) and sent to the bank: no more money creation.
* **Raid cooldowns**: stored in the database (`raid_cooldowns` table, schema v6) and kept after a restart.
* **Active raids**: by design, a running raid doesn't survive a restart (its monsters are removed), but the post-raid immunity is kept.
* **Performance**: the periodic save of players and guilds runs in **a single** SQLite transaction.
* **Treaties**: new `fr.esicraft.treaty` package (manager, menus, allied PvP protection), wired into both raid entry points and `RailManager.getRelation`.
* **HUD optimization**: the sidebar is no longer rebuilt every 2 s; it only updates when its content changes, and only outdated lines are removed (fewer scoreboard packets).
* **Website**: new `fr.esicraft.web` package (`WebServer`, `WebSnapshot`, `HtmlPages`), started/stopped with the plugin.
* **Security (25/10 audit)**: atomic treaty break (two officers clicking at the same time no longer pay two indemnities), cancelling a proposal is officer-only, allied protection extended to harmful splash and lingering potions.
