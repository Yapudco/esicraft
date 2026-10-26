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

> [!IMPORTANT]
> The first choice is immediate. After that, a guild can only change specialization **when a new player** (who has never been in the guild) joins.

---

## 💎 5. The Nexus and its traps

The **Nexus** is the heart of your guild. You place it on your land with `/guild nexus place` (only once: you can't place it again to reset its health to 100%). **If it's destroyed, your guild loses everything** (see [raids](#️-6-raids-monster-hordes)).

**16-block safety zone** around the Nexus: no water, lava or obsidian (crying obsidian included). The Nexus can't be placed if there's already some, and none can be added later. This prevents moats and unbreakable walls.

**Health points**: the Nexus's HP is shown above it (hologram), including after a server restart or a reconnection.

**Who is spared**: the guardian golem and traps never target guild members or members of a guild bound by a treaty (alliance, non-aggression pact or vassalage).

**The Nexus must be visible**: a hit only counts if the attacker can see it (range and line of sight are checked). The Nexus only exists in the Overworld.

**Healing it**: a member throws a **golden apple** on it (or right-clicks it with one): +500 HP, or +2,500 HP with an enchanted golden apple. You must wait 30 seconds between two heals, and it's impossible during a raid or within 2 minutes of a player attack.

### Traps (`/nexus traps`)

Paid from the guild bank, they stay armed (even after a restart) until the **end of the next horde**; you then have to buy them again. They target monsters and **intruders** standing on your land, never members, treaty allies, or players in creative/spectator mode.

| Trap | Price | Effect |
| :--- | :--- | :--- |
| 🔥 Infernal Blaze | 250 | Within 8 blocks: 1 heart of damage every 2 s plus burning |
| 🕸️ Binding Web | 200 | Slowness II within 10 blocks |
| ⚡ Retaliation Lightning | 350 | Strikes anyone who hits the Nexus: 4 hearts, at most one bolt every 5 s per player |
| 🧪 Toxic Mist | 300 | Poison I within 8 blocks (poison never kills, it only weakens) |
| 🤖 Guardian Automaton | 500 | An iron golem attacks enemies; rebuilt 5 min after it dies |

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
- **Managing your network** (`/rail`, officers): set your toll (**Merchant Syndicate guilds only**; other guilds never charge), and for each other guild choose **Normal**, **Allied** (free) or **Banned** (lines through your land are cut for them).
- **Building a station**: an officer places a sign with `[Gare]` on line 1 and the station name on line 2, within 2 blocks of a rail. Requires Age 2, 500 coins from the bank, and no more than 6 stations per guild.

---

## 🎉 10. Server life

### Events (one every 1 to 2 days)

| Event | Duration | What happens |
| :--- | :--- | :--- |
| 🌑 Blood Moon | 10 min | More monsters, bounties ×2 |
| ☄ Meteor Shower | 3 min | Ore blocks fall from the sky near players |
| 🧙 Black Market | 5 min | A merchant sells rare items for diamonds, to everyone |
| ☠ World Boss | 15 min | The *Lord of Ashes*: 3,000 coins shared by damage dealt, bonus for the top fighter |

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
- `/deguisement <Name>`: take the skin and name of **any Minecraft account**, even one that isn't on the server (`/deguisement aypierre_`). If that player belongs to another guild on the server, you also borrow its guild identity. Not possible while that player is online.
- `/deguisement off`: remove the disguise (original name and skin restored).

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

### ⚖ Balance: revenge and catch-up

- **Revenge**: the guild that loses a raid gets **24 h of hordes at -50%** against its victor.
- **Catch-up**: guilds **below the season median** earn up to **+25%** on monster bounties (at least 4 guilds). The bonus shrinks as they catch up and vanishes at the median.

### Playing in English

`/english` translates the game **for you only**: messages, action bar, menus, dashboard and the `/guide` book (`/francais` goes back). Your choice is remembered. Guild and player names are not translated. Not translated yet: the event boss bar, chat, the war journal entries and the website.

### The website

The server has a public website: **`http://<server-address>/`**, which updates itself (every 15 seconds). It shows **no coordinates**: nothing that helps find a base. The website is in French.

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
| `/guild nexus place` · `/nexus traps` | Place the Nexus · buy traps |
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

- Enabled by default on **port 80** (`web.port`). On Linux this port needs root or `setcap 'cap_net_bind_service=+ep' $(readlink -f $(which java))`; otherwise use 8080. If the port is refused, the plugin still starts and says so in the console.
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
| `/game reset` | Reset the whole game |
| `/race set <player> <race>` | Change a player's profession |

### Built-in anti-cheat

- Mining bonuses never apply to a block placed by a player, or on another guild's land.
- Age requirements are based on the highest member count reached: kicking players before donating doesn't help.
- Enchantments above their normal maximum are removed outside the Arcane Conclave.
- Horde size is capped, the bank is only charged once the target is confirmed, and raid monsters are removed on restart.
- Money writes are serialized (nothing lost between a sale and a disconnection).

</details>
