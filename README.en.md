# ⚔️ Esicraft — Player Guide

🇫🇷 [Version française](README.md) · 📝 [Changelog (in French)](CHANGELOG.md)

**Esicraft** is a strategy-focused survival Minecraft server for a group of friends. Each player picks a **profession**, joins a **guild**, claims **land** and protects the **Nexus**, the heart of their base. Guilds progress together through **6 Ages**, trade, sign **treaties**… or send **monster hordes** to destroy each other's Nexus.

> [!NOTE]
> The game itself is in French. This guide gives the French in-game name in *italics* where it helps you find things in menus. Commands are typed exactly as written.

---

## 📑 Contents

1. [Getting started in 5 steps](#-1-getting-started-in-5-steps)
2. [Professions](#-2-professions)
3. [Guilds and their land](#️-3-guilds-and-their-land)
4. [Guild specializations](#-4-guild-specializations)
5. [The Nexus and its traps](#-5-the-nexus-and-its-traps)
6. [Raids (monster hordes)](#️-6-raids-monster-hordes)
7. [Ages](#-7-ages)
8. [Money](#-8-money)
9. [Diplomacy and travel](#-9-diplomacy-and-travel)
10. [Server life](#-10-server-life)
11. [All commands](#-11-all-commands)
12. [For administrators](#️-12-for-administrators)

---

## 🚀 1. Getting started in 5 steps

1. **Pick your profession** with `/race`. Until you do, you can't break any block.
2. **Join a guild** (accept an invitation with `/guild join <name>`) or **create your own** with `/guild create <name> <tag>` (costs 500 coins).
3. **Claim your first piece of land** with `/guild claim`, then **place the Nexus** with `/guild nexus place`.
4. **Earn money**: kill monsters, complete your daily contracts (`/contrat`), sell at the Auction House (`/ah`).
5. **Level up your guild** by donating resources in `/age` to unlock new gear and dimensions.

> [!TIP]
> Type `/guide` to get the in-game rule book (30 pages: clickable table of contents, jobs, guilds, Nexus, raids, Ages, money, treaties, trains, events, commands), and `/hud` to show or hide the dashboard on the right of your screen.

---

## 🎭 2. Professions

Each profession has **items only it can craft**, some **strengths** and one **weakness**. These exclusives fade over the Ages: gear reserved for one profession becomes available to everyone later.

| Profession | Specialty | Strengths | Weakness |
| :--- | :--- | :--- | :--- |
| ⛏ **Dwarf** (*Nain*, miner) | Pickaxes and shovels | Haste II and Night Vision below layer 0 · 15% chance to double an ore | Slowness I in full sunlight (above layer 60) · can't craft metal weapons or armor |
| 🌿 **Sylvan** (*Sylvain*, farmer) | Hoes, bone meal, breeding, golden apples | Regeneration I for themselves and guildmates within 4 blocks · right-click harvest with automatic replanting | -20% melee damage |
| 🔨 **Blacksmith** (*Forgeron*) | Armor, swords, axes, shields, anvils, grindstones, blast furnaces | Permanent Fire Resistance · 35% chance to double an ingot from a furnace · anvil costs half the XP | Gets hungry 25% faster |
| 🏹 **Ranger** (*Rôdeur*, archer) | Bows, crossbows and arrows (until Age 3) | Arrows poison their target · +25% ranged damage · Speed I in plains, forests, jungles and taigas | Slowness I when wearing a diamond or Netherite chestplate |
| 🧪 **Arcanist** (*Arcaniste*, mage) | Enchanting, splash potions, brewing stands (until Age 3) | +30% XP · potions last 25% longer · -20% magic damage taken | +15% melee damage taken |
| ⚙ **Engineer** (*Ingénieur*) | Redstone, pistons, hoppers, dispensers and **TNT** (until Age 3) | -40% explosion damage taken | -20% melee damage |

<details>
<summary><b>Which tools and armor for which profession, Age by Age?</b></summary>

| Profession | Age 1 | Age 2 | Ages 3-4 |
| :--- | :--- | :--- | :--- |
| Dwarf | Iron pickaxe | Diamond pickaxe and shovel | Netherite pickaxe and shovel |
| Blacksmith | Iron armor, sword and shield | Diamond armor, sword and axe | Netherite armor, sword and axe |
| Sylvan | Iron hoe | Diamond hoe | Netherite hoe |

From Age 5, Netherite is available to everyone.
</details>

**Changing profession**: `/race` opens the menu. Your first choice and the next 3 changes are free; after that each change costs 10,000 coins. You must wait 24 h between two changes.

`/race show` lists online players (profession, guild, Age, money).

---

## 🏛️ 3. Guilds and their land

### Roles

| Role | What they can do |
| :--- | :--- |
| 👑 **Leader** | Everything, plus: appoint officers, hand over the guild (`/guild transfer`), disband it (`/guild disband`) |
| 🛡 **Officer** | Invite, kick, claim land, place the Nexus, buy traps, launch raids, sign treaties, manage stations |
| 🙂 **Member** | Deposit money in the bank, donate resources for the Age, defend |

- **10 players maximum** per guild, and you can only be in **one guild**.
- Invitations expire after **5 minutes**.
- You can't disband your guild during a raid, or within 10 minutes of an attack on the Nexus.

### Claiming land

Land is counted in **chunks** (16 × 16 block squares). With `/guild claim`, an officer claims the chunk they're standing in.

- **All chunks must touch** along a **side** (not diagonally: no more "snakes"). Only the first one can go anywhere. Only the **leader** can give up a chunk (`/guild unclaim`), and not if that would split the territory in two, or if it holds the Nexus.
- **Anti-encirclement buffer**: a new chunk must stay at least **1 chunk** away from the territory of a guild with no treaty with yours.
- **Officer cap**: an officer spends at most **3,000 coins per day** from the bank (claims, traps); the leader has no limit.
- **Price**: paid from the guild bank. The first chunk costs 100 coins, then each new chunk costs 25 more than the previous one (100, 125, 150, 175…).
- **Maximum size by Age**:

| Age | 1 | 2 | 3 | 4 | 5 and 6 |
| :--- | :--- | :--- | :--- | :--- | :--- |
| Max chunks | 16 | 32 | 64 | 128 | unlimited |

`/guild claim show` displays your territory borders.

### What's protected on a guild's land

On another guild's land you **can't break or place blocks**, or open its chests, furnaces, barrels, shulkers or hoppers.

Also forbidden there: **TNT** (including TNT minecarts), **Ender Pearls** and **Chorus Fruit** to teleport in, **End Crystals**, **pistons**, **lava, water and fire** coming from outside, block destruction by Withers, Endermen and Ravagers, and **projectiles** hitting animals, item frames and armor stands.

**Lava and water**: a liquid never crosses from one territory to another (even during a raid), dispensers cannot pour any into a neighbour's land, and a **lava** bucket cannot be emptied within 5 blocks of a player who is neither in your guild nor your ally.

**Protected entities**: item frames, armor stands, minecarts (chest ones too), horses, donkeys, animals and leashes can't be hit, ridden or opened by a stranger. Explosions damage no block of a territory, and dispensers can't pour buckets, fire charges or TNT in from outside.

**Siege zone**: so that a Nexus stays reachable, an attacker can break blocks (except chests and containers) within **4 blocks** of a living Nexus, and members can't place a solid block within **2 blocks** of it (no walled-in Nexus).

The other protections only drop during a raid against that guild, and for **30 minutes** after its Nexus is destroyed if there's no annexation.

### 🧩 The diversity bonus

The more **different professions** your guild brings together, the stronger it gets (shown in `/guild info` and on the website):

| Different professions | Bonus |
| :--- | :--- |
| 3 | Monster bounties +10 % |
| 4 | New land 10 % cheaper |
| 5 | Nexus traps 15 % cheaper |
| 6 | Guild contract rewards ×1.5 |

---

## 🌟 4. Guild specializations

The leader picks a specialization in `/guild spec`. It gives bonuses to the whole guild but imposes rules on its makeup, checked whenever a player joins and whenever someone changes profession.

| Specialization | Bonuses | Constraints |
| :--- | :--- | :--- |
| ⚔️ **Warrior Legion** (*Légion Guerrière*, combat) | Strength I and Resistance I on your land · raids 30% cheaper · +30% damage against monsters | At least 50% Rangers and Blacksmiths · max 1 Dwarf and 1 Sylvan · -25% ores and crops on your land |
| ⛏️ **Underground Industry** (*Industrie Souterraine*, mining) | Haste II on your land below layer 30 · raw ores ×1.5 · Nexus traps 50% cheaper | Max 1 Ranger · level II potions forbidden · +20% magic damage taken |
| 🌾 **Rural Enclave** (*Enclave Champêtre*, nature) | +2 hearts of health · Regeneration I and Saturation on your land · crops, animal loot, wood and stone ×2 · animals breed 3× faster | No Engineer · max 1 Blacksmith · Netherite armor forbidden |
| 💰 **Merchant Syndicate** (*Syndicat Marchand*, trade) | Only guild allowed to trade with villagers before Age 4 · +20% on monster bounties (10% of which goes to the bank) · no Auction House tax | No Arcanist · raids 30% more expensive · no combat bonuses · Nexus -20% health |
| 🔮 **Arcane Conclave** (*Conclave Arcanique*, magic) | Nexus +25% health · enchanting and anvil cost half the XP · potions one level higher · boosted enchantments (IV becomes V, V becomes VI) · the Nexus fires a beam at the nearest attacker (1 heart every 2 s) · -50% magic damage | -2 hearts of health · land 50% more expensive · max 1 Blacksmith and 1 Dwarf · boosted enchantments are lost if you change specialization |
| 🏹 **Lone Ranger** (*Rôdeur Solitaire*, solo) | Permanent Speed I · Netherite armor and enchanting from Age 3 | A single member, a Ranger who has never been in another guild · no chests on their land · Nexus capped at 2,500 HP |
| 🔱 **Atlantean** (*Atlante*, ocean) | **While in water**: water breathing, night vision, aquatic agility, dolphin's grace and Resistance | **Nexus must be submerged** (at least one water block against it; water around it is then allowed) · **Slowness out of the water** |

> [!IMPORTANT]
> The first choice is immediate. After that, a guild can only change specialization **when a new player** (who has never been in the guild) joins.

---

## 💎 5. The Nexus and its defenses

The **Nexus** is the heart of your guild. You place it on your land with `/guild nexus place` (only once: you can't place it again to reset its health to 100%). **If it's destroyed, your guild loses everything** (see [raids](#️-6-raids-monster-hordes)).

**16-block safety zone** around the Nexus: no water, lava or obsidian (crying obsidian included). The Nexus can't be placed if there's already some, and none can be added later. This prevents moats and unbreakable walls.

**Health points**: the Nexus's HP is shown above it (hologram), including after a server restart or a reconnection.

**Who is spared**: the guardian golem and traps never target guild members (the golem doesn't turn on them even if they hit it) or members of a guild bound by a treaty (alliance, non-aggression pact or vassalage).

**The Nexus must be visible**: a hit only counts if the attacker can see it (range and line of sight are checked). The Nexus only exists in the Overworld.

**Healing it**: a member throws a **golden apple** on it (or right-clicks it with one): +500 HP, or +2,500 HP with an enchanted golden apple. You must wait 30 seconds between two heals, and it's impossible during a raid or within 2 minutes of a player attack.

### The Nexus tech tree (`/nexus`)

The Nexus gains **experience** for every horde repelled (+200) and when you **sacrifice resources** to it (`/nexus sacrifice`, item in hand, within 10 blocks: coal, iron, gold, diamond, emerald, netherite, ender pearl, blaze rod…; capped at 3,000 XP a day). It climbs from level 1 to level 6 (0, 300, 900, 2,000, 4,500, 9,000 XP).

Instead of buying disposable traps, officers **unlock** defenses in `/nexus` (the tech tree): you need the required level, the "parent" defense already unlocked, **items to sacrifice** taken from your inventory and, for the "living" defenses, to have **brought the matching mob into the Nexus chunk and killed it there**. An unlocked defense is **permanent** (hordes no longer consume it); Underground Industry pays half the items. They target monsters and **intruders**, never members, treaty allies or players in creative/spectator mode.

| Defense | Lvl | Requirements | Effect |
| :--- | :---: | :--- | :--- |
| 🔥 Infernal Blaze | 1 | 64 coal, 16 flint | Within 8 blocks: 1 heart of damage every 2 s plus burning |
| 🕸️ Binding Web | 1 | 128 string, 16 slime balls | Slowness II within 10 blocks |
| ⚡ Retaliation Lightning | 2 | Blaze · 96 copper, 64 redstone | Strikes anyone who hits the Nexus (4 hearts, one bolt every 5 s per player) |
| 🤖 Guardian Automaton | 2 | **Kill an iron golem in the chunk** · 8 iron blocks, 32 redstone | An iron golem attacks enemies; rebuilt 5 min after it dies |
| 🧪 Toxic Mist | 2 | Web · 48 spider eyes, 16 slime | Poison I within 8 blocks (never kills) |
| 🔥 Blaze Shot | 3 | Blaze · **kill a blaze** · 24 blaze rods, 32 gold | Up to 3 fireballs every 5 s |
| 🐡 Guardian Shot | 3 | Lightning · **kill a guardian** · 64 shards + 32 crystals | Water beam (damage + slowness) every 4 s |
| 🐝 Bee Swarm | 3 | Mist · **kill a bee** · 32 honeycombs, 8 honey bottles | Up to 6 enraged bees stinging enemies |
| 🪄 Evoker Shot | 4 | Golem · **kill an evoker** · 64 emeralds, 1 totem | Magic fangs: 6 area damage every 9 s |
| 🟪 Shulker Shot | 4 | Guardian · **kill a shulker** · 8 shells, 16 pearls | Homing projectile + levitation every 6 s |
| 👻 Ghast Shot | 5 | Blaze · **kill a ghast** · 8 tears, 128 gunpowder | Area fireball (8 damage, breaks no blocks) every 14 s |
| 👁 Warden Shot | 5 | Evoker · **kill a warden** · 8 echo shards, 4 catalysts | Sonic boom: 10 damage and knockback every 15 s |
| 🌊 Elder's Curse | 6 | Guardian · **kill an elder guardian** · 128 prismarine bricks, 8 nautilus shells | Mining Fatigue III and damage to all nearby enemies every 25 s |

Unlocking everything earns the *Archi-technician* achievement. **Tiny hordes earn nothing**: repelling a horde of fewer than 12 monsters gives no Nexus XP, season points or achievement. During a raid, only the horde's monsters and TNT lit by the attacking guild destroy the base (not a third party). The Nexus must stay **reachable**: it can't be placed under more than 3 solid blocks of rock (sieges only break within 4 blocks of it). Settings: `nexus.tech.*` (cost, damage, XP).

### 🎰 Nexus Russian roulette (`/nexus gamble`)

The **guild leader**, facing the Nexus (within 6 blocks), can try an all-in gamble (retype to confirm within 15 s, once per **72 h**, impossible during a raid): **50%** the Nexus gains **+1,000 max HP** (cumulative bonus capped at **3,000**) and **2,000 coins** go to the bank; **50%** it loses **1,500 HP** (it always survives at 1 HP) and **500 max HP** of the roulette bonus, the bank loses **1,000 coins**, and a **mini-horde of pillagers** appears with no warning at its foot. *All or nothing* achievement on a win.

---

## ⚔️ 6. Raids (monster hordes)

A raid means **paying for an army of monsters** to attack another guild's Nexus.

### Launching a raid

1. Open `/ah` and click the **Wither skull** (at the bottom of the menu).
2. Pick a **ready-made horde** (armored zombies, incendiary skeletons, spiders, pillagers and ravagers) or **build your own** (up to 60 monsters).
3. Pick the target guild. The price is paid from **your guild bank**.

Options: **sappers** that blow up walls and dry out the water and lava around them, and **fireproofing** so your monsters can cross lava.

### Conditions

- Only **officers and the leader** can launch a raid.
- **With no defender** online and on their land, a raid is still possible but the Nexus only takes **25%** of the damage.
- **Maximum duration**: 25 minutes; after that the raid ends with no win or loss (a trapped monster no longer makes a guild untouchable). If a large part of the horde vanishes without fighting (unloaded chunk…), the win isn't credited.
- After a raid, the target guild is **protected for 30 minutes**.
- You can't attack a guild you have a **treaty** with (see [Diplomacy](#-9-diplomacy-and-travel)).

### If the Nexus falls

- The loser's **land** goes to the winner (within the limit of the winner's Age).
- The loser's whole **bank** goes to the winner.
- The loser's members choose: join the winner (`/guild annex join`) or leave (`/guild annex decline`).

### ⭐ Guilds that get too rich become targets

A guild gets **Economic Hegemony** status if at least half of its members are in the top 5 richest players (`/baltop`), or if its **bank plus its members' coins** hold more than 30% of all the money on the server. Raids against it cost **half price** and its post-raid protection only lasts **2 minutes**. It's marked with a ⭐ in the target menu.

---

## ⏳ 7. Ages

Your guild starts at **Age 1** and can go up to **Age 6**. To reach the next Age, members **donate resources** in the `/age` menu. Each Age unlocks gear and raises how much land you can hold.

| Age | What it unlocks |
| :--- | :--- |
| 1. **Rustic** (start) | Wood and stone. Iron is reserved for specialized professions. Nether and End closed. |
| 2. **Metallurgy** | **The Nether**, iron tools and shields for everyone. Diamond is reserved for specialized professions. |
| 3. **Alchemy** | **The End**, diamond tools and iron armor for everyone. Netherite is reserved for specialized professions. |
| 4. **Total War** | For everyone: diamond armor, villager trading, TNT, redstone, pistons, brewing stands, bows and crossbows. **Player bonuses**: Speed I, +10% damage, +25% XP, -20% explosion damage. |
| 5. **Mythic** | Netherite and beacons for everyone, unlimited land. **Player bonuses**: Haste I, Speed I, Resistance I, +15% damage, +50% XP. |
| 6. **Supremacy** | The ultimate Age. **Reward**: a dragon egg and a block of Netherite for every member, plus a chat title: **[Giga Tchad]** if your session is under an hour, **[Gros Nerd]** beyond that. |

> [!NOTE]
> The amounts below are for a **one-player guild**. They grow with the number of members (×1.3 with 2 players, ×2.2 with 5, ×4.7 with 10), based on the **highest member count** reached during the Age. The `/age` menu always shows the exact amounts for your guild.

<details>
<summary><b>Resources to donate for each Age</b></summary>

- **Age 2**: 750 copper ingots, 256 leather, 750 raw iron, 1,000 oak logs, 1,500 wheat, 750 carrots.
- **Age 3**: 3,500 iron ingots, 1,500 gold ingots, 1,500 lapis lazuli, 1,000 quartz, 750 nether wart, 150 blaze rods, 150 magma cream, 15 ghast tears, 750 glowstone dust, 3,500 feathers, 6,000 potatoes, 6,000 sugar cane, 1,000 pumpkins, 2,500 melon slices, 1,500 beetroots.
- **Age 4**: 500 diamonds, 150 iron blocks, 100 gold blocks, 1,000 obsidian, 200 crying obsidian, 500 ender pearls, 1 pair of elytra, 2,500 golden carrots, 150 golden apples, 1,200 hay bales, 800 honey bottles, 800 slimeballs, 6,000 redstone dust, 400 repeaters, 350 pistons, 200 sticky pistons, 150 dispensers, 2,500 gunpowder, 250 TNT, 750 firework rockets, 32 anvils.
- **Age 5**: 16 netherite ingots, 5 nether stars, 4 beacons, 8 totems of undying, 16 echo shards, 16 sculk catalysts, 16 sculk shriekers and 64 sculk sensors, 64 shulker shells, 2,500 end stone, 1,200 purpur blocks, 2,500 chorus fruit and 1,000 popped chorus fruit, 1,000 redstone blocks, 400 comparators, 400 observers, 250 hoppers, 150 targets, 150 emerald blocks.
- **Age 6** (fixed amounts): every music disc, every mob head, all 8 goat horns, 64 nether stars, a Netherite hoe enchanted with Mending + Unbreaking III + Fortune III, every smithing template, all 20 pottery sherds, the dragon egg and a dead bush.

</details>

---

## 💰 8. Money

### Earning coins

- **Killing monsters**: every monster pays a bounty, from 1.50 coins (silverfish) to 750 coins (Ender Dragon). Repetitive mob farms pay less.
- **Daily contracts** (`/contrat`): 3 missions a day, one of them tied to your profession. +400 coins if you complete all three.
- **Auction House** (`/ah`): sell the item in your hand with `/ah sell <price>` (10 listings max). Click your own listing to take it back.
- **Events** and the **world boss** (see [Server life](#-10-server-life)).

<details>
<summary><b>Monster bounty table</b></summary>

| Category | Monsters and bounty |
| :--- | :--- |
| 🐉 Bosses | Ender Dragon 750 · Warden 500 · Wither 350 · Elder Guardian 150 |
| 👺 Mini-bosses | Ravager 75 · Evoker 40 · Piglin Brute 25 |
| 🔥 Nether and End | Ghast and Shulker 20 · Wither Skeleton and Guardian 15 · Blaze, Hoglin and Zoglin 12 |
| 🏹 Elite | Vindicator 15 · Witch 12 · Enderman 10 · Pillager 8 |
| 🧟 Common | Creeper and Phantom 5 · Piglin and Zombified Piglin 4 · Spiders 3 to 3.50 · Skeletons 3 · Zombies 2.50 |
| 🐛 Small | Vex 2.50 · Slimes and Magma Cubes 2 · Silverfish and Endermites 1.50 |

The Merchant Syndicate earns 20% more.
</details>

### The guild contract

In `/contrat`, below your personal contracts: every day your guild gets **three objectives, each reserved for one profession** (for example: a Dwarf mines, a Blacksmith smelts ingots, an Engineer places redstone). Only members with the right profession advance their objective. Each objective pays **200 coins** into the guild bank, and all three together **750 more**. Objectives grow with the size of the guild.

### Killing a player

When you kill a player from another guild, you **take 100 coins + 1% of their fortune** from them (or everything they have if they have 100 coins or less). No bounty if you already fought each other less than 2 minutes ago, or between members of the same guild.

### Spending

Guild bank (`/guild bank <amount>`) for land, traps and raids · transfers (`/pay`) · Auction House purchases · train trips · profession changes.

---

## 🤝 9. Diplomacy and travel

### Treaties (`/traite`)

An officer proposes a treaty to another guild, which has 10 minutes to accept (left click) or refuse (right click) in its own `/traite` menu.

| Treaty | Duration | Effects |
| :--- | :--- | :--- |
| 🤝 **Alliance** | Unlimited | No raids between you · free train travel on each other's land · you can't hurt each other (hits, arrows, potions) |
| 📜 **Non-aggression pact** | 7 days | No raids between you · **hitting a player of the other guild or their Nexus breaks the pact** (betrayal + rivalry) |
| 👑 **Vassalage** | Unlimited | Like an alliance, but the vassal pays 5% of its bank to its overlord every day |

**Breaking a treaty is a betrayal**: your guild pays up to 500 coins of compensation to the other one, the whole server is told, and you can't attack them for 30 minutes. One exception: an overlord can free its vassal for free.

**Easing a rivalry**: it fades on its own over time (the weight of raids, betrayals and kills is halved every 7 days, adjustable via `rivalry.half-life-days`) and **vanishes as soon as the two guilds sign a treaty** (alliance, pact or vassalage).

**Aggression under a treaty**: if a player attacks the Nexus of a guild bound by a treaty (pact, alliance or vassalage, overlord against vassal included), or hits a member of a guild under a pact (melee, arrow), the treaty is broken automatically. It counts as a betrayal (compensation, 30-minute notice, server announcement) and a **rivalry** opens between the two guilds (shown on the website, diplomacy page).

### The train (`/station`, `/rail`)

The rail network lets you **teleport from station to station**, as long as a **continuous rail line** connects them (junctions and crossings included).

- **Travel**: near a station (a `[Gare]` sign), type `/station` and pick your destination.
- **Price**: 1 coin for the guild owning the arrival station, plus the **toll** of every guild whose land you cross. Your own guild and its allies don't charge you.
- **Managing your network** (`/rail`, officers): set your toll (**up to 10 coins for every specialization, up to 100 for the Merchant Syndicate**), and for each other guild choose **Normal**, **Allied** (free) or **Banned** (lines through your land are cut for them).
- **📦 Freight train** (Age 2 minimum, officers): **Shift + click** a destination in the `/station` menu to open a **double chest (54 slots)**. Close it, then click **[+ COFFRE]** to load another or **[ENVOYER]** in chat (fee: arrival fare ×5 per filled chest). The number of double chests per convoy depends on the guild's Age: **Age 2: 1 · Age 3: 2 · Age 4: 3 · Age 5: 5 · Age 6: 10**: a **chest minecart** leaves on its own and follows the rails, **slowly** (**4 min** under 1,000 blocks, **8 min** up to 3,000, **12 min** up to 5,000, **15 min** up to 10,000, **20 min** beyond). It isn't a real minecart: its position is computed from the clock, it is only visible near players and survives restarts. On arrival, items useful for the next Age are **donated automatically** if the station is yours; the rest goes into the **arrival station's box, open to every player present** (`/station fret recuperer [chest number]`, up to 30 chests): freight also works as an **exchange between guilds** (the station's guild is told of the arrival). Two convoys per guild at most.
- **🏹 Attacking a convoy**: when a convoy crosses the territory of a guild with **no treaty** with the sender, its members hit it (**25 hits**) to **loot** it: the loot spills on the rails. Both guilds are warned at each border crossed. *Convoy looter* and *Freight king* achievements.
- **Building a station**: an officer places a sign with `[Gare]` on line 1 and the station name on line 2, within 2 blocks of a rail. Requires Age 2, 500 coins from the bank, and no more than 6 stations per guild.

---

## 🎉 10. Server life

### Events (one every 1 to 2 days)

| Event | Duration | What happens |
| :--- | :--- | :--- |
| 🌑 Blood Moon | 10 min | More monsters, bounties ×2 |
| ☄ Meteor Shower | 3 min | Ore blocks fall from the sky near players |
| 🧙 Black Market | 5 min | A merchant offers 3 randomly picked rare items, **32 to 128 diamonds** each. **Each offer is locked after a single purchase** and each player can only buy **one thing** |
| ☠ World Boss | 15 min | A **different boss every week** (see below): 1,000 coins shared by damage dealt (at least 10% of the damage, 400 coins max per player), and for the top fighter 3 diamonds + a 20% chance of a Nether Star |

### ☠ The world boss in detail

- **One boss a week**: *Lord of Ashes* (forge, +25% damage from **Dwarves**), *Forest Guardian* (woodland beast, +25% from **Rangers**) or *Ice Queen* (+25% from **Blacksmiths**).
- **Countdown**: **2 minutes** before it wakes, a full-screen title and a **horn** warn everyone; the message gives the **distance and direction** from your position, not a rough area. Reminder 30 s before.
- **Scaled difficulty**: HP = **300 per player** within 150 blocks when it wakes (400 to 1,000 HP), damage 8 + 2 per player.
- **Three phases** (the bar changes color): at **75%** it summons **spectral guards**, at **50%** it casts a **shockwave** (an 11-block circle that throws you), at **25%** it **enrages** (faster, stronger, more frequent attacks).
- **Telegraphed attacks**: before every area hit, a **particle circle** appears on the ground for **3 seconds**: get out!
- **Heroic mode**: after the server's first victory, the boss is heroic one time in two (HP ×1.5, damage ×1.3, prize pool ×1.5, more diamonds); *Heroic legend* achievement.
- **Guild bonus**: the top fighter's guild receives **500 coins** in its bank, and its name is engraved in the website's **Pantheon**.
- Settings: `events.boss.*`.

### ⚖ Guild Court (`/vote`)

Every **Monday**, guild members vote with `/vote` (clickable list) on a **rule for the following week** (Tuesday to Monday). Each established guild (active Nexus, at least 2 members and 3 days old: `court.*`) weighs one vote (its choice = the option most voted by its members); ties are broken by number of voters. Rules: **Toll tax** (rail tolls ×2), **Building bonus** (claims -25%), **Free market** (no Auction House tax), **Open hunt** (monster bounties +25%), **Extended truce** (see below). The verdict is announced and written in the journal.

### 🕊 Weekly truce

On **Friday and Saturday at 9 pm**, **one hour with no PvP** (hits, arrows, harmful potions) for everyone, **and no attack on a Nexus, no horde launched, no convoy looted**; honor duels are exempt. A siege whose time falls during a truce waits for its end. Adjustable in `truce.*` (days, time, length). With the "Extended truce" rule it also starts on Thursday and lasts twice as long.

### Seasons

Every month, `/classement` ranks the guilds (land, Age, wealth, members, raids repelled, conquests). The champion guild wins **5,000 coins**, **500 per member** and a 👑 crown in chat. `/histoire` tells the latest big moments on the server.

### 🍂 Natural seasons (`/climat`)

The world changes season **every two weeks** (configurable):

| Season | Effect |
| :--- | :--- |
| 🌸 Spring | Crops grow faster (+50 %) |
| ☀ Summer | +20 % experience |
| 🍂 Autumn | One chance in two of a bonus crop |
| ❄ Winter | Crops grow half as fast, monster bounties +25 % |

### 🆘 Calling for help (`/aide`)

During a raid or an attack on your Nexus, `/aide` warns your **allies** (alliance or vassalage). Each ally who reaches your land before the horde ends earns **250 coins** if it is repelled (6 allies max). One call every 10 minutes.

### 🎭 Disguise (`/deguisement`)

For **750 coins** you change **skin and name** for **5 minutes** (the skin is refreshed for everyone, and restored at the end):

- `/deguisement <guild>`: pose as a member of that guild (skin of one of its members, made-up pseudonym, guild tag in chat and the list). Its traps and golem ignore you.
- `/deguisement <Name>`: take the skin and name of a Minecraft account **outside the server** (`/deguisement Notch`). Names of players on the server are **refused** (impersonation: announcements, journal and commands would be hijacked): to imitate one of ours, use their guild. Impossible if that player is online.
- `/deguisement off`: remove the disguise (original name and skin restored).

**Offline-mode server behind Velocity or BungeeCord**: it works. The skins of the server's own players are the ones the proxy forwards at login (enable **modern** forwarding: `player-info-forwarding-mode = "modern"` in Velocity and `velocity-support` in Paper), and other accounts are looked up directly with Mojang (the server must be able to reach `api.mojang.com` and `sessionserver.mojang.com`).

Land protections stay active. **Dealing damage or hitting the Nexus unmasks you**, and the imitated guild is warned. One disguise every 30 minutes; coins are only charged if the skin was actually found. Death messages never reveal your real name.

### ✨ Cosmetics (`/cosmetique`)

Three slots, one cosmetic equipped in each:

- **Auras**: particles around you.
- **Trails**: particles behind your steps.
- **Victory effects**: a burst of particles when you kill a player from another guild.

You get them in three ways:

- **Achievements**: each achievement unlocks its own aura (26).
- **Quests**: gameplay goals that unlock an aura, trail or effect: daily contracts completed (10, 50), monsters slain (250, 1,000), train trips (20, 25), calls for help (5, 10), hordes repelled (5, 10, 20), hordes launched (3), players killed (5, 25, 100).
- **The server shop**: 15 cosmetics for **3,000 to 15,000 coins** (personal coins, paid once: click to buy, click again to confirm). Not possible during combat.

The `/cosmetique` menu has one tab per slot; locked cosmetics say how to get them (quest progress, price). `/cosmetique off` removes everything. There are also secrets to find…

### 📰 War journal and achievements

*New hard achievements*: **land area** (16, 32, 64, 128, 256, 512, 1,024 and 2,048 claimed chunks), Heroic legend, Weapons master (10 duels), All or nothing, Archi-technician, Siege master, Shareholder, Freight king, Convoy looter, Tavern keeper.

`/journal` sums up the latest big events (raids, conquests, treaties, betrayals, Ages, events). `/succes` lists your **26 achievements** (first profession, conqueror, traitor, master spy, four seasons…) and the share of players who have them. An achievement held by fewer than 10 % of players is **rare** and announced in the journal.

### ⚔ PvP combat

Hitting a player from another guild (melee, arrow, TNT, wolf, harmful potion…) or being hit puts you "in combat" for 15 seconds. During that time **`/hub`, `/station`, `/pay`, `/ah`, `/banque`, `/guild bank|home|leave`, `/home`, `/tpa`… are blocked**, command teleports too, and **logging out is punished by death** (kicks included; only a server shutdown is excused). Spending or transferring coins is also impossible (purchases, `/pay`, bank, profession, disguise); only paying a ransom is allowed.

### 🏦 Guild bank: loans and shared chest (`/banque`)

Between two guilds **bound by a treaty** (alliance, pact or vassalage):

- `/banque pret <guild> <amount>`: an officer offers a loan (100 to 20,000 coins). The other guild accepts with `/banque accepter <guild>` (or `refuser`). The coins move from one bank to the other; the borrowing guild must give back the amount **+ 10% interest** within **5 days**.
- `/banque dettes` lists loans · `/banque rembourser <guild> [amount]` repays (in several payments if needed) · `/banque pardonner <guild>` forgives a debt.
- `/banque coffre <guild>` opens a 54-slot **shared chest** used by both guilds (available as long as a treaty binds them).
- **An unpaid debt weighs on the land**: once the deadline has passed, the guild can no longer claim chunks, and **one chunk is seized every 24 h** (never the Nexus chunk, never one that would split the territory). Each seized chunk wipes 300 coins of debt.

### ❤ Reputation (`/merci <player>`)

Thank a helpful player: they gain **1 honor point**. You can thank the same person **once a day**. Points show in the **tab list** (❤), on the **website profile** and in the "Most honorable" ranking on the Players page. `/merci` alone shows your reputation and the top 3.

### 🎓 Mentoring (`/parrain <player>`)

An advanced player (profession chosen, in a guild, 5 h played) can guide a **recruit** (joined less than 14 days ago, 5 recruits at most). The recruit accepts with `/parrain accepter`. The mentor is rewarded when the recruit reaches milestones: **first profession** (+200 coins, +50 for the recruit) and **first territory** of their guild (+300, +100 for the recruit). `/parrain` shows progress.

### ⛓ Ransom

A blow that would **kill** a player from another guild **captures** them instead: they are immobilized and invulnerable for 90 seconds. Their captor chooses:

- `/rancon prix <amount>` sets the ransom (100 to 2,000 coins; by default 15% of the captive's fortune);
- the captive pays with `/rancon payer`: 30% to the captor, 70% to their guild's bank;
- `/rancon liberer` frees them for nothing, `/rancon executer` kills them (usual kill bounty).

With no decision, they are executed. Logging out while captive counts as dying. After release, they can't be captured again for 10 minutes.

**Prison cell**: an officer aims at a block on their territory and types `/rancon cellule` (`/rancon cellule retirer`). The captive is then **teleported** there during the ransom (and brought back to their capture point afterwards). Their allies (same guild or treaty) can **free** them by acting 15 times on the cell block (right or left click); remember to close the cell!

### 🗡 Honor duels and bets (`/duel`, `/pari`)

- `/duel <player> [stake]` challenges a player (optional coin stake, `/duel accepter` or `/duel refuser`). Both are **teleported to a lit neutral arena** above the world (one platform per duel, 6 at a time, built only if the area is empty); their **inventory, health and position are saved** then returned at the end; they fight with the **same supplied kit** (full iron armor, iron sword and axe, shield, bow, 16 arrows, 64 steaks, 1 instant health potion) (even after a crash, when they reconnect).
- **Nobody dies**: the deciding blow ends the fight. The winner takes the opponent's stake. A duel **triggers no rivalry, betrayal, capture or combat tag**, and is not stopped by the truce. 5 minutes at most (draw after that, stakes returned); quitting = losing.
- `/pari <side> <amount>`: at the start of a **raid** or a **duel**, **spectators** (not in the guilds involved / not the duelists) have **2 minutes** to bet (10 to 1,000 coins) on the `attaquante`/`defensive` guild (or its name) or on a duelist. The winning side shares the pot minus **5%** kept by the server; with no opposing bets, or if the fight is cancelled, everything is refunded. **Anti-cheat**: members of the guilds involved **and guilds bound to them by a treaty** are excluded, as is anyone whose **IP address** matches an online participant, and accounts under 3 days old.

### 🍺 Taverns (`/taverne`)

The **Merchant Syndicate** and the **Druidic Enclave** can declare up to 2 chunks of their territory (never the Nexus chunk) as **neutral zones** with `/taverne declarer` (`/taverne retirer`). Inside, **PvP, ranged weapons and offensive potions are disabled**. `/taverne` opens a **coin flip** (click interface): bet 10 to 1,000 coins against the **owning guild's bank** (47% chance to win, stake doubled); the bank must be able to cover the bet. *Tavern keeper* achievement.

### 📈 The stock exchange (`/bourse`)

From **Age 4**, the leader can **open the guild's capital** (`/bourse ouvrir`, 1,000 shares). **Outside** players buy shares (`/bourse acheter <guild> <shares>`, price = 10 + equity ÷ 1,000; 100 shares per player, 49% of the capital at most) and sell them back to the guild at 90% of the price, **after holding them 24 h** and never for more than 1.5 times what they paid (the exchange can't be used to drain the bank). Every 6 hours, **20% of the guild's profit** (rise of its **equity**: bank minus net debts, so a received loan is never profit) is paid to shareholders **pro rata to their shares**. `/bourse` lists listed guilds, `/bourse portefeuille` your shares. A disbanded guild leaves the exchange: its shares become worthless. *Shareholder* achievement.

### ⚔ Formal wars: sieges (`/siege`)

An officer plants a **Siege Banner** on a rival guild's **border** (in a chunk next to theirs, on free ground or their own land, in an empty space on solid ground) with `/siege declarer <guild> <day> <hour>` (e.g. `samedi 21`): **24 hours' notice**, 500 coins fee from the bank, a one-hour slot that can't overlap the truce, impossible under a treaty. The target can **cancel the siege by destroying the banner** before the time (and keeps the fee). During the slot, members of the besieging guild can **tear down the enemy Nexus with pickaxes or heavy weapons**: damage ×2, even with no defender present, without buying a horde. **Victory** if the Nexus falls or 40% of its max HP is dealt: 10% of the loser's bank (3,000 max) is looted. Otherwise the defender keeps the fee. `/siege liste` shows sieges, `/siege annuler` gives up (half the fee refunded). History in the website Pantheon. *Siege master* achievement.

### 🔱 Atlanteans

The **Atlantean** specialization (see the table above) requires a Nexus **in the water**: place it against a water block. Water is allowed all around it (placing, buckets, flowing) in the Nexus chunk; lava and obsidian stay forbidden. Members get all the underwater perks **while in water** and suffer **Slowness I on dry land**.

### 🗳 Website elections and Pantheon

Every week the community elects on the website (`/votes`) the most **clueless**, the **biggest**, the **funniest** and the most **annoying** player. To vote: click "Revendiquer ce pseudo" (on your profile or the votes page); the site shows a **code** (`ESI-XXXX`) to **type in the game chat** with that account (the message isn't broadcast); the game then asks for a **confirmation** (`[CONFIRMER]` / `[REFUSER]`, with the requester's approximate address: never confirm a request you didn't make). A **cookie** (random token, HttpOnly, 90 days) then recognizes your browser. One vote per category, changeable, never for yourself; accounts under 2 days old are excluded, and **only one account per connection and category**. `/webvote optout` removes you from the elections; admins can disable them or rename the categories (`web.votes.*`). The week's winners are announced in chat and shown in the **Pantheon** (`/pantheon`), along with slain bosses, sieges and the Court rule.

### ⚖ Balance: revenge and catch-up

- **Revenge**: the guild that loses a raid gets **24 h of hordes at -50%** against its victor.
- **Catch-up**: guilds **below the season median** earn up to **+25%** on monster bounties (at least 4 guilds). The bonus shrinks as they catch up and vanishes at the median.

### Playing in English

`/english` translates the game **for you only**: messages, action bar, menus, dashboard and the `/guide` book (`/francais` goes back). Your choice is remembered. Guild and player names are not translated. Not translated yet: the event boss bar, chat, the war journal entries and the website.

### The website

The server has a public website: **`http://<server-address>:8080/`**, which updates itself (every 30 seconds, adjustable with `web.refresh-seconds`). It shows **no coordinates**: nothing that helps find a base. The website is in French.

| Page | Content |
| :--- | :--- |
| Home (*Accueil*) | Online players, top guilds, natural season, ongoing raids, recent journal |
| Guilds · guild page | Ranking; members, treaties, diversity, trophies, growth curves, Ages reached, highlights |
| Players · profile | Online, richest, directory; each player's profile (profession, guild, stats, achievements) |
| Raids | Ongoing hordes: target, attacker, monsters left, defenders on site, duration |
| Journal | The full war journal, filterable by category |
| Diplomacy (*Diplomatie*) | Network of alliances, pacts, vassalages and rivalries, rivalry heat map |
| Museum (*Musée*) | Guild trophies, every achievement with its rarity, latest unlocks |
| Ages (*Âges*) | Timeline on a real time axis (guilds can be hidden one by one) and each guild's requirements for the next Age |
| Pantheon (*Panthéon*) | Slain bosses (with the guild engraved), sieges, Court rule, week's titles |
| Votes | Weekly elections (claim your username with a code typed in chat) |
| Guide | All the rules, with a search box |

Data is also available as JSON: `/api/stats.json`, `/api/journal.json`, `/api/raids.json`.

---

## 📋 11. All commands

| Command | What it does |
| :--- | :--- |
| `/race` · `/race show` | Pick your profession · see online players |
| `/guide` | In-game rule book (opens right away, clickable contents) |
| `/guild create <name> <tag>` | Create a guild |
| `/guild invite <player>` · `/guild join <name>` | Invite · accept an invitation |
| `/guild leave` · `/guild kick <player>` | Leave · kick |
| `/guild promote <player>` · `/guild demote <player>` | Appoint or remove an officer (leader) |
| `/guild transfer <player>` · `/guild disband` | Hand over the guild · disband it (leader) |
| `/guild info [name]` | Guild details |
| `/guild claim` · `/guild unclaim` · `/guild claim show` | Claim · give up a chunk · show borders |
| `/guild spec` | Pick the specialization |
| `/guild nexus place` · `/nexus` | Place the Nexus · defense tech tree |
| `/nexus sacrifice` · `/nexus gamble` | Sacrifice a resource to the Nexus · Russian roulette (leader) |
| `/guild annex <join\|decline>` | After a defeat: join the winner or leave |
| `/guild bank <amount>` | Deposit into the guild bank |
| `/age` | Age progression and donations |
| `/money` · `/baltop` · `/pay <player> <amount>` | Your money · richest players · send coins |
| `/ah` · `/ah sell <price>` | Auction House and raids · sell the item in your hand |
| `/contrat` | Daily contracts and guild contract |
| `/aide` | Call your allies for help during an attack |
| `/deguisement <guild or Name>` · `/deguisement off` | Change skin and name (guild or any Minecraft account) · remove the disguise |
| `/climat` | Current natural season |
| `/journal` · `/succes [player]` | War journal · achievements |
| `/traite` · `/traite liste` | Guild treaties |
| `/station` · `/rail` | Take the train · manage your network (officers) |
| `/classement` · `/histoire` | Season ranking · big moments |
| `/english` · `/francais` | Switch the game to English (menus, messages, dashboard, rule book) · back to French |
| `/hud` | Show or hide the dashboard |
| `/banque` | Loans between guilds, debts, shared chest |
| `/merci <player>` | Give an honor point (once a day per person) |
| `/parrain <player>` | Mentor a recruit (`accepter` / `refuser` to answer) |
| `/rancon` | Manage a prisoner: `prix`, `payer`, `liberer`, `executer` |
| `/cosmetique` | Cosmetics menu (auras, trails, victory effects): earn them or buy them |
| `/hub` | Go back to the hub (blocked during PvP combat) |
| `/vote [number]` | Guild Court: vote the week's rule (Mondays) |
| `/duel <player> [stake]` · `/duel accepter\|refuser` | Honor duel in a neutral arena |
| `/pari <side> <amount>` | Bet on a raid or a duel (spectators) |
| `/taverne [declarer\|retirer]` | Neutral zone and coin flip |
| `/bourse [ouvrir\|acheter\|vendre\|portefeuille]` | Guild shares (Age 4) |
| `/siege [declarer <guild> <day> <hour>\|liste\|annuler]` | Formal war |
| `/rancon cellule` | Designate your guild's prison cell |
| `/station fret [confirmer\|annuler\|recuperer]` | Freight train |

---

## 🛠️ 12. For administrators

<details>
<summary><b>Installation, configuration and admin commands</b></summary>

### Installation

- **Server**: Paper 26.2, Java 25.
- **Build**: `.\mvnw.cmd package` on Windows, `./mvnw package` on Linux/macOS. The plugin is created in `target/`. Every change pushed to GitHub is also built automatically (GitHub Actions).
- **Install**: copy the `.jar` into the server's `plugins/` folder and restart. Data is stored in an SQLite database in the plugin folder.

### Main settings (`config.yml`)

| Section | Contents |
| :--- | :--- |
| `guilds` | Creation and land prices, max members, Nexus health and healing |
| `ages` | Resources required for each Age |
| `raids` | Post-raid protection, max size of custom hordes |
| `races` | Delay between two profession changes |
| `market` | Auction House tax and max listings |
| `events` | Event frequency |
| `rail` | Trip and station prices, max toll, minimum Age |
| `treaties` | Pact duration, tribute, compensation, grace period, ally protection |
| `web` | Website: port, displayed name, visible information, anti-flood |
| `life` | Natural seasons (length), disguise (price, duration, cooldown), call-for-help reward |

### The website

- Enabled by default on **port 8080** (`web.port`). On Linux this port needs root or `setcap 'cap_net_bind_service=+ep' $(readlink -f $(which java))`; otherwise use 8080. If the port is refused, the plugin still starts and says so in the console.
- Remember to **open the port** on your router and firewall.
- Read-only, with no coordinates. Nexus HP is hidden by default (`web.show-nexus-health`). Banks and balances can be hidden (`web.show-guild-banks`, `web.show-balances`). Vanished players don't appear.
- Anti-flood: 60 requests per 10 seconds per IP address (`web.rate-limit`). Behind a reverse proxy, raise this limit.
- JSON data: `/api/stats.json`.

### Admin commands (permission `esicraft.admin`, operators by default)

| Command | Use |
| :--- | :--- |
| `/esicraftadmin setage\|spawnraid\|givecoins\|forcenexus` | Change a guild's Age, launch a raid, give coins, force a Nexus |
| `/money get\|set\|give\|take\|rectify\|top` | Manage player money |
| `/evenement <lune\|meteores\|marchand\|boss\|stop\|status>` | Start or stop an event |
| `/pvp [on\|off\|toggle]` | Turn raids and Nexus destruction on or off |
| `/game reset` | Reset **everything**: guilds, Nexus, fortunes, achievements, statistics, contracts, reputation, mentoring, cosmetics, journal, curves, Ages, season, loans and the website (only language preferences are kept) |
| `/race set <player> <race>` | Change a player's profession |

### Operators and cooldowns

**Operators** (`op`) are not subject to **any cooldown**: disguise (30 min), call for help (10 min), a guild's post-raid protection, Nexus healing (30 s and the block after an attack), profession change (24 h), specialization change (7 days), train line search, `/merci` (once a day per person) and the PvP bounty anti-farm delay. Setting: `admin.ops-bypass-cooldowns` (`true` by default).

### Built-in anti-cheat

- Mining bonuses never apply to a block placed by a player, or on another guild's land.
- Age requirements are based on the highest member count reached: kicking players before donating doesn't help.
- Enchantments above their normal maximum are removed outside the Arcane Conclave.
- Horde size is capped, the bank is only charged once the target is confirmed, and raid monsters are removed on restart.
- Money writes are serialized (nothing lost between a sale and a disconnection).

</details>
