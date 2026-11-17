# ⚔️ Esicraft — Player Guide

🇫🇷 [Version française](README.md) · 📝 [Changelog (in French)](CHANGELOG.md)

**Esicraft** is a strategy-focused survival Minecraft server for a group of friends.

- You pick a **profession** (miner, blacksmith, archer…).
- You join a **guild** (10 players max) and together you claim **land**.
- At the heart of your base you place the **Nexus**: if it falls, you lose everything.
- Together you climb through **6 Ages** to unlock iron, the Nether, the End, Netherite…
- You trade, sign **treaties**… or **pay for monster hordes** to destroy the Nexus across the border.

> [!NOTE]
> Commands are French and typed exactly as written (`/guild`, `/contrat`, `/ah`…). The in-game French names are given in *italics* where it helps you find things in menus. `/english` translates menus and messages for you.

> [!TIP]
> **New here?** Jump straight to [1. Getting started](#1-getting-started): it walks you step by step from your first login to your first week.

---

## Contents

1. [Getting started](#1-getting-started) — the step-by-step tutorial
2. [Command reference](#2-command-reference)
3. [Professions](#3-professions)
4. [Guilds and their land](#4-guilds-and-their-land)
5. [Guild specializations](#5-guild-specializations)
6. [The Nexus](#6-the-nexus)
7. [Raids, sieges and player combat](#7-raids-sieges-and-player-combat)
8. [Ages](#8-ages)
9. [Money, the market and the stock exchange](#9-money-the-market-and-the-stock-exchange)
10. [Diplomacy and travel](#10-diplomacy-and-travel)
11. [Server life](#11-server-life)
12. [The website](#12-the-website)
13. [For administrators](#13-for-administrators)

---

## 1. Getting started

### 1.1 The five ideas to remember

| Idea | In one sentence |
| :--- | :--- |
| 🎭 **Profession** (*métier*) | Your class. It gives you strengths, a weakness, and the right to craft certain items. **Without a profession, you can't break any block.** |
| 🏛️ **Guild** | Your team (10 players max). It owns a **bank**, **land** and a **Nexus**. |
| 💎 **Nexus** | The heart of your base. Enemies want to destroy it, you want to protect it. |
| ⏳ **Age** | The guild's progression level (1 to 6). You raise it by **donating resources** in `/age`. |
| 🪙 **Coins** | The server currency. You start with **100**. They pay for everything: land, market, trains, raids… |

### 1.2 Step 1: your first five minutes

On your very first login:

1. **`/guide`**: get the rule book (clickable table of contents). Reading it earns 50 coins in the tutorial.
2. **`/race`**: pick your **profession** (see the table below). Your first choice is free, and so are the next 3 changes.
3. **`/hud`**: show or hide the dashboard on the right of your screen (money, guild, Age…).
4. **`/tutoriel`**: a bar at the top of the screen shows the current quest (see [1.7](#17-the-built-in-tutorial-9-quests)). `/tutoriel masquer` hides it.

> [!IMPORTANT]
> Until you've picked a profession, every block you break is cancelled with a message: **`/race` first!**

**Which profession should you pick?**

| If you like… | Pick | Why |
| :--- | :--- | :--- |
| Mining, digging | ⛏ **Dwarf** (*Nain*) | Haste II and Night Vision below layer 0, 15% chance to double an ore. Slow in full sunlight. |
| Farming, breeding, healing | 🌿 **Sylvan** (*Sylvain*) | Regeneration for you and your guildmates nearby, right-click harvest and replant. A bit fragile in melee. |
| Crafting weapons and armor | 🔨 **Blacksmith** (*Forgeron*) | Fire Resistance, doubled ingots from furnaces, cheaper anvil. Gets hungry faster. |
| Shooting a bow | 🏹 **Ranger** (*Rôdeur*) | Poisoned arrows, +25% ranged damage, fast outdoors. |
| Enchanting, brewing | 🧪 **Arcanist** (*Arcaniste*) | +30% XP, longer potions. More vulnerable in melee. |
| Redstone and explosives | ⚙ **Engineer** (*Ingénieur*) | -40% explosion damage, redstone and TNT. Weak in melee. |

> [!TIP]
> A guild is **stronger when it brings together different professions** (up to 6, with real bonuses: see [The diversity bonus](#the-diversity-bonus)). If you're joining friends, ask which professions are missing. Full details are in [Professions](#3-professions).

### 1.3 Step 2: earn your first coins

You start with **100 coins**. Founding a guild takes more (see below). The easiest sources:

- **Kill monsters**: every monster pays a bounty (from 1.50 to 750 coins).
- **`/contrat`**: 3 missions a day, one tied to your profession. **+400 coins** if you complete all three.
- **The tutorial**: 925 coins in all, plus a 400-coin final bonus.
- **`/ressources`**: a mining world reset every week, for fresh ore (leave nothing valuable down there, PvP is free).
- **`/ah sell <price>`**: sell the item in your hand at the Auction House.

### 1.4 Step 3: join or found a guild

Two paths. **The first is the easiest way to start.**

#### Option A: join a guild (recommended)

1. Ask an **officer or the leader** to type `/guild invite <your name>`.
2. You have **5 minutes** to accept: `/guild join <guild name>`.
3. That's it: you immediately enjoy your guild's bank, land and Nexus protection.

#### Option B: found your own guild

> [!WARNING]
> **Get ready before typing the command.** As soon as the guild exists, you have **1 hour** to claim a chunk **and** place the Nexus. With no Nexus after one hour, **the guild is automatically disbanded**, and your 500 coins are lost.

You need at least **600 coins**: 500 to create it, then at least 100 to put into the bank to pay for the first chunk.

| Step | Command | Details |
| :--- | :--- | :--- |
| 1. Pick the spot | : | Stand in the chunk (a 16 × 16 block square) where you want your base, **in the Overworld**. The Nexus can only be placed once. |
| 2. Create | `/guild create <name> <tag>` | Name of 3 to 24 characters (letters, digits, `-`, `_`), tag of 2 to 6 characters. Cost: **500 coins**. |
| 3. Fund the bank | `/guild bank 100` | The guild bank pays for land. The first chunk costs 100 coins. |
| 4. Claim | `/guild claim` | Claims the chunk you're standing in. |
| 5. Place the Nexus | `/guild nexus place` | Inside your territory. No water, lava or obsidian within **16 blocks**, and no more than 3 solid blocks of rock above it. **It can't be placed again afterwards.** |
| 6. Check | `/guild claim show` | Shows the territory borders for one minute (green: yours, red: others). |

Then you can invite your friends with `/guild invite <player>`.

### 1.5 Step 4: your first day

Once in a guild, here's an efficient routine:

1. **Every day**: do your `/contrat` (3 personal missions + 3 guild objectives, one per profession).
2. **Feed the bank**: `/guild bank <amount>`. It pays for land, hordes and sieges.
3. **Raise the Age**: open `/age` to see the requested resources, then donate them (menu, or item in hand + `/age deposit`). Amounts scale with the number of members.
4. **Grow your territory**: `/guild claim` on a chunk that **touches by a side** (not diagonally) the ones you already have. Each chunk costs 25 coins more than the previous one.
5. **Trade**: `/ah` opens the Auction House (listings, buy orders).
6. **Go mining**: `/ressources`, then `/ressources quitter` to come back up.

### 1.6 Step 5: your first week

| Goal | How |
| :--- | :--- |
| **Recruit** | Invite friends with different professions: 3 professions = monster bounties +10%, up to 6 professions = contracts ×1.5. |
| **Choose a specialization** | An officer opens `/guild spec` (combat, mining, nature, trade, magic, solo, ocean). The first choice is immediate; read the composition constraints carefully. |
| **Defend the Nexus** | Officers unlock permanent defenses in `/nexus` (blaze, web, lightning, golem…). Sacrifice resources with `/nexus sacrifice`. Heal it with golden apples. |
| **Reach Age 2** | Unlocks the **Nether**, iron tools for everyone, and the ability to build stations. |
| **Protect yourself** | Sign a treaty (`/traite`) with a neighboring guild: alliance, non-aggression pact or vassalage. Guilds bound by a treaty can't raid each other. |
| **Mind your neighbors** | No land within 1 chunk of a guild you have no treaty with. Very rich guilds are targets (see [Economic Hegemony](#rich-guilds-are-targets)). |
| **Discover server life** | Events, world boss, taverns, stock exchange, trains: all in [part 11](#11-server-life). |

### 1.7 The built-in tutorial (9 quests)

`/tutoriel` guides you with a bar at the top of the screen. Each quest pays coins, **once per account**. After all 9 quests you get a **400-coin bonus** and a basket of food.

| # | Quest | How | Reward |
| :-: | :--- | :--- | :-: |
| 1 | Read the rule book | `/guide` | 50 |
| 2 | Choose a profession | `/race` | 100 |
| 3 | Join or found a guild | `/guild join <guild>` or `/guild create <name> <tag>` | 150 |
| 4 | Complete a first contract | `/contrat` | 150 |
| 5 | Deposit 100 coins in the bank | `/guild bank 100` | 100 |
| 6 | Donate for the Age | `/age` | 100 |
| 7 | Step into a tavern | neutral zone of the Merchant Syndicate or the Rural Enclave | 75 |
| 8 | Go down to the resource world | `/ressources` | 100 |
| 9 | Make a first trade at the Auction House | `/ah`, `/ah sell <price>` or a buy order | 100 |

### 1.8 Pitfalls to avoid

- 🚫 **No profession = no mining.** `/race` first.
- ⏱ **Founding a guild starts a 1-hour countdown** (claim + Nexus), otherwise it's disbanded.
- 💎 **The Nexus can't be placed again** (except the move granted after a specialization change: `/nexus move`). If it's destroyed, the guild loses its land, bank and treaties.
- ⚔️ **PvP combat = 15 seconds of lockdown**: `/hub`, `/ah`, `/pay`, `/banque`, `/station`, `/home`, `/tpa`… are refused, chests are closed, and **logging out in combat is punished by death**.
- ⛏ **Leave nothing valuable in the resource world**: PvP is free there and it's reset every week.
- 🔇 **Private messages are disabled** (`/msg`, `/tell`, `/w`, `/r`…). Talk in public chat or your guild's.
- 🧱 **On other people's land you can't break or place anything**, nor open any chest, furnace or hopper.
- 🛠 **Some gear is locked by profession or Age**: a "forbidden" item can neither hit nor mine, even if found or bought.

### 1.9 The server's rhythm

| When | What |
| :--- | :--- |
| Every day | 3 personal contracts + 3 guild objectives · travelling merchant at a randomly drawn guild (around 2 pm, 6-hour stay) |
| Every 1 to 2 days | An event: Blood Moon, meteor shower, black market or world boss |
| **Monday** | Guild Court vote (`/vote`) · resource world reset (5 am) |
| **Friday and Saturday, 9 pm** | Truce: 1 hour with no PvP, no raids and no Nexus attacks |
| Every 2 weeks | Natural season change (`/climat`) |
| Every month | Guild ranking (`/classement`) and reward for the champion |

### 1.10 Cheat sheet: the beginner's 12 commands

```text
/guide              The rule book
/race               Choose your profession
/tutoriel           The 9 starter quests
/contrat            Today's missions
/guild              Guild menu (or help if you have none)
/guild join <name>  Accept an invitation
/guild bank <n>     Deposit into the guild bank
/guild claim        Claim the chunk you're in
/age                Age progress and donations
/ah                 Auction House
/ressources         Mining world
/hud                Dashboard
```

---

## 2. Command reference

> [!NOTE]
> Admin commands are in [part 13](#13-for-administrators). Aliases are grouped at the end of this part. A command with no argument usually opens its menu.

### Account, help and interface

| Command | What it does |
| :--- | :--- |
| `/esicraft` | Plugin overview and command list |
| `/guide` | In-game rule book (30 pages, clickable table of contents) |
| `/tutoriel [masquer\|afficher]` | Guided tutorial quests (hide / show the bar) |
| `/hud` | Show or hide the side dashboard |
| `/english [on\|off]` · `/francais` | Switch the game to English (menus, messages, dashboard, book) · back to French |
| `/hub` | Go back to the hub (proxy server; blocked in combat) |
| `/webvote [confirmer\|refuser\|optout\|optin]` | Website elections: link your browser, stop being a candidate |

### Profession

| Command | What it does |
| :--- | :--- |
| `/race` | Open the profession menu |
| `/race show [player]` | Directory of online players (profession, guild, Age, money), or one player's card |

### Guild and land

| Command | Who | What it does |
| :--- | :--- | :--- |
| `/guild` | Member | Guild menu (help if you have none) |
| `/guild create <name> <tag>` | Any player | Found a guild (500 coins) |
| `/guild invite <player>` | Officer | Invite a player (valid for 5 min) |
| `/guild join <name>` | Any player | Accept an invitation |
| `/guild leave` | Member | Leave the guild |
| `/guild kick <player>` | Officer | Kick a member |
| `/guild promote <player>` · `/guild demote <player>` | Leader | Appoint or remove an officer |
| `/guild transfer <player>` | Leader | Hand the guild over |
| `/guild disband` | Leader | Disband the guild |
| `/guild info [name]` | Everyone | A guild's card (diversity level included) |
| `/guild claim` | Officer | Claim the chunk you're standing in |
| `/guild claim show` | Everyone | See territory borders for 1 minute |
| `/guild unclaim` | Leader | Give up the chunk you're standing in |
| `/guild spec` | Officer | Choose the specialization |
| `/guild bank` | Member | See the bank balance |
| `/guild bank <amount>` | Member | Deposit coins into it |
| `/guild nexus` | Member | Nexus status |
| `/guild nexus place` | Officer | Place the Nexus (only once) |
| `/guild annex join` · `/guild annex decline` | Member | After a defeat: join the winner or leave |
| `/batiment [liste\|declarer <forge\|bibliotheque\|ecurie>\|retirer]` | Officer | Guild buildings and their bonuses |
| `/taverne [declarer\|retirer]` | Officer | Declare a neutral zone, or open the games menu |

### Nexus

| Command | Who | What it does |
| :--- | :--- | :--- |
| `/nexus` | Member | Defense tech tree |
| `/nexus info` | Member | Health, level, XP and unlocked defenses |
| `/nexus sacrifice` | Member | Sacrifice the item in your hand to the Nexus (within 10 blocks) |
| `/nexus move` | Officer | Move the Nexus (right granted after a specialization change) |
| `/nexus gamble` | Leader | Nexus Russian roulette |

### Ages

| Command | What it does |
| :--- | :--- |
| `/age` | Ages menu: requested resources and donations |
| `/age deposit` | Donate the item in your hand to the guild's progress |

### Money and market

| Command | What it does |
| :--- | :--- |
| `/money` | See your balance |
| `/baltop` | Ranking of the richest players |
| `/pay <player> <amount>` | Send coins |
| `/ah` | Auction House (listings, buy orders, mercenary hordes) |
| `/ah sell <price>` | Sell the item in your hand (1 to 1,000,000 coins, 10 listings max) |
| `/ah commande [material] <quantity> <unit price>` | Place a buy order (amount set aside) |
| `/ah commandes` | Deliver to, collect from or cancel orders |
| `/bourse` · `/bourse liste` | Guild share prices |
| `/bourse ouvrir` · `/bourse fermer` | Open or close your guild's capital (leader, Age 4) |
| `/bourse acheter <guild> <shares>` · `/bourse vendre <guild> <shares>` | Buy or sell shares |
| `/bourse info <guild>` · `/bourse portefeuille` | Breakdown of a guild's value · your shares |
| `/banque` | Bank menu: loans, debts, shared chests |
| `/banque pret <guild> <amount> [rate]` | Offer a loan (officer) |
| `/banque accepter <guild>` · `/banque refuser` | Answer a loan offer |
| `/banque dettes` · `/banque rembourser <guild> [amount]` | List your debts · repay |
| `/banque pardonner <guild>` | Forgive a debt (leader) |
| `/banque coffre <guild>` | 54-slot shared chest with a partner guild |
| `/marchand` | Where today's travelling merchant is |
| `/ressources [aller\|quitter\|info]` | Go down to the resource world · come back up · reset date |

### Raids, sieges and combat

| Command | What it does |
| :--- | :--- |
| `/ah` then the Wither skull | Buy a horde to attack another guild (officers) |
| `/siege declarer <guild> <day> <hour>` | Declare a siege (24 h notice), e.g. `/siege declarer TheTarget samedi 21` |
| `/siege liste` · `/siege annuler` | See sieges · give up |
| `/aide` | Call your allies for help during an attack |
| `/duel <player> [stake]` · `/duel accepter` · `/duel refuser` | Honor duel in a neutral arena |
| `/pari <side> <amount>` | Bet on a raid or a duel (spectators) |
| `/rancon prix <amount>` · `/rancon liberer` · `/rancon executer` | The captor's decisions |
| `/rancon payer` | The captive pays their ransom |
| `/rancon cellule` · `/rancon cellule retirer` | Set or remove the prison cell (officer) |

### Diplomacy and travel

| Command | What it does |
| :--- | :--- |
| `/traite` · `/traite liste` | Treaty menu (propose, accept, refuse, break) |
| `/station` | Travel from station to station (Shift + click a destination: freight train) |
| `/station fret confirmer\|annuler\|ajouter\|recuperer [no.]` | Manage a freight convoy and collect arrivals |
| `/rail` | Manage your network: toll, relations with other guilds (officers) |

### Social, achievements and server life

| Command | What it does |
| :--- | :--- |
| `/contrat` | Daily contracts and guild contract |
| `/merci <player>` | Give a point of honor (`/merci` alone: your reputation and the top 3) |
| `/parrain <player>` · `/parrain accepter\|refuser` | Mentor a recruit · answer a proposal |
| `/vote [number]` | Guild Court: vote the week's rule (Mondays) |
| `/classement` | Season ranking of guilds |
| `/histoire` | The server's great moments |
| `/journal` | War journal (latest big events) |
| `/succes [player]` | Your achievements (or a player's) |
| `/climat` | Current natural season and its effects |
| `/cosmetique` · `/cosmetique off` | Auras, trails, victory effects · remove everything |
| `/deguisement <guild\|Name>` · `/deguisement off` | Change skin and name for 5 minutes (750 coins) |

### Aliases

| Command | Aliases |
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

## 3. Professions

Each profession has **items only it can craft**, some **strengths** and one **weakness**. These exclusives fade over the Ages: gear reserved for one profession becomes available to everyone later.

| Profession | Specialty | Strengths | Weakness |
| :--- | :--- | :--- | :--- |
| ⛏ **Dwarf** (*Nain*, miner) | Pickaxes and shovels | Haste II and Night Vision below layer 0 · 15% chance to double an ore | Slowness I in full sunlight (above layer 60) · can't craft metal weapons or armor |
| 🌿 **Sylvan** (*Sylvain*, farmer) | Hoes, bone meal, breeding, golden apples | Regeneration I for themselves and guildmates within 4 blocks · right-click harvest with automatic replanting | -20% melee damage |
| 🔨 **Blacksmith** (*Forgeron*) | Armor, swords, axes, shields, anvils, grindstones, blast furnaces | Permanent Fire Resistance · 35% chance to double an ingot from a furnace · anvil costs half the XP | Gets hungry 25% faster |
| 🏹 **Ranger** (*Rôdeur*, archer) | Bows, crossbows and arrows (until Age 3) | Arrows poison their target · +25% ranged damage · Speed I in plains, forests, jungles and taigas | Slowness I when wearing a diamond or Netherite chestplate |
| 🧪 **Arcanist** (*Arcaniste*, mage) | Enchanting, splash potions, brewing stands (until Age 3) | +30% XP · potions (drunk or thrown) last 25% longer · -20% magic damage taken | +15% melee damage taken |
| ⚙ **Engineer** (*Ingénieur*) | Redstone, pistons, hoppers, dispensers and **TNT** (until Age 3) | -40% explosion damage taken | -20% melee damage |

<details>
<summary><b>Which tools and armor for which profession, Age by Age?</b></summary>

| Profession | Age 1 | Age 2 | Ages 3-4 |
| :--- | :--- | :--- | :--- |
| Dwarf | Iron pickaxe | Diamond pickaxe and shovel | Netherite pickaxe and shovel |
| Blacksmith | Iron armor, sword and shield | Diamond armor, sword and axe | Netherite armor, sword and axe |
| Sylvan | Iron hoe | Diamond hoe | Netherite hoe |

At Age 5, Netherite is available to everyone.
</details>

**Changing profession**: `/race` opens the menu. Your first choice and the next 3 changes are free, after that every change costs 10,000 coins. You must wait 24 h between two changes. Operators have no delay.

`/race show` displays the directory of online players (profession, guild, Age, money).

---

## 4. Guilds and their land

### Roles

| Role | What they can do |
| :--- | :--- |
| 👑 **Leader** (*Chef*) | Everything, plus: appoint officers, hand the guild over (`/guild transfer`), disband it (`/guild disband`), give up a chunk |
| 🛡 **Officer** | Invite, kick, claim land, place the Nexus, choose the specialization, unlock defenses, launch raids, sign treaties, manage stations |
| 🙂 **Member** | Deposit money in the bank, donate resources for the Age, defend |

- **10 players maximum** per guild, and you can only be in **one guild**.
- Invitations expire after **5 minutes**.
- You can't disband your guild during a raid, nor within 10 minutes of an attack on the Nexus.
- **Officer cap**: an officer spends at most **3,000 coins a day** from the bank (claims, traps, **hordes**, **sieges**, **loans**); the leader has no limit.

### Claiming land

Territory is counted in **chunks** (16 × 16 block squares). With `/guild claim`, an officer claims the chunk they're standing in.

- **All chunks must touch** by a **side** (not diagonally: no more "snakes"). Only the first one can be placed anywhere. Only the **leader** can give up a chunk (`/guild unclaim`), and not if it would split the territory in two, nor the Nexus chunk.
- **Anti-encirclement buffer**: a new chunk must stay at least **1 chunk** away from the territory of a guild you have no treaty with.
- **Price**: paid by the guild bank. The first costs 100 coins, then each new chunk costs 25 coins more than the previous one (100, 125, 150, 175…).
- **Maximum size by Age**:

| Age | 1 | 2 | 3 | 4 | 5 and 6 |
| :--- | :--- | :--- | :--- | :--- | :--- |
| Max chunks | 16 | 32 | 64 | 128 | unlimited |

- You can't claim anything in the resource world, nor anything while the guild has an **unpaid debt**.

`/guild claim show` displays for one minute the **outer edges** of territories within 2 chunks (green: yours, red: others).

### What's protected on a guild's land

On another guild's land you can **neither break nor place blocks**, nor open its chests, furnaces, barrels, shulker boxes or hoppers.

Also forbidden there: **TNT** (minecart TNT included), **ender pearls** and **chorus fruit** used to teleport in, **end crystals**, **pistons**, **lava, water and fire** coming from outside, block destruction by Withers, Endermen and Ravagers, **projectiles** at animals, item frames and armor stands or at blocks (decorated pots and their contents, chorus flowers, dripstone), and the **fishing rod** to pull its animals, villagers, armor stands, minecarts or dropped items out of the territory.

**Lava and water**: a liquid never crosses from one territory to another (even during a raid), dispensers can't pour any onto a neighbor, and you can't empty a **lava** bucket within 5 blocks of a player who is neither in your guild nor your ally.

**Protected entities**: item frames, armor stands, minecarts (even chest ones), horses, donkeys, animals and leads can't be hit, ridden or opened by a stranger. Explosions damage no block in a territory, and dispensers can't empty a bucket, fire charge or TNT from outside into it.

**Siege zone**: so that a Nexus stays reachable, an attacker can break blocks (except chests and containers) within **4 blocks** of a living Nexus, and members can't place a solid block within **2 blocks** of it (no walled-in Nexus).

The other protections only drop during a raid against the guild, and for **30 minutes** after its Nexus is destroyed if there's no annexation.

### The diversity bonus

The more **different professions** your guild brings together, the stronger it is (level shown in `/guild info` and on the website):

| Different professions | Bonus |
| :--- | :--- |
| 3 | Monster bounties +10% |
| 4 | New land 10% cheaper |
| 5 | Nexus traps 15% cheaper |
| 6 | Guild contract rewards ×1.5 |

### Guild buildings (`/batiment`)

Like a tavern, a building is a **chunk of your territory** (never the Nexus one, nor a tavern) holding the right blocks. An officer declares it with `/batiment declarer <forge|bibliotheque|ecurie>` (`/batiment retirer` to free it); each guild can have **one building of each type**. The building is rechecked regularly: if it's no longer compliant, its bonus is suspended until it's repaired. `/batiment` lists the state of all three.

| Building | Blocks required in the chunk | Bonus for members |
| :--- | :--- | :--- |
| ⚒ **Forge** | An anvil, a blast furnace (under a roof) and a smithing table | Tools and armor wear out **20% slower**; the forge's anvils cost **half** the experience |
| 📚 **Library** (*Bibliothèque*) | An enchanting table (under a roof), a lectern and **15 bookshelves** | **+15% experience**; enchanting at the library table **refunds 1 level** |
| 🐎 **Stable** (*Écurie*) | **6 hay bales** and **8 fences** (or fence gates) | Mounts (horses, donkeys, mules, camels, pigs, striders) get **Speed and Regeneration** |

---

## 5. Guild specializations

An officer or the leader chooses a specialization in `/guild spec`. It gives bonuses to the whole guild, but imposes rules on its composition, checked whenever a player joins and whenever someone changes profession.

| Specialization | Bonus | Constraints |
| :--- | :--- | :--- |
| ⚔️ **Warrior Legion** (*Légion Guerrière*, combat) | Strength I and Resistance I on your land · raids 30% cheaper · +30% damage against monsters | At least 50% Rangers and Blacksmiths · max 1 Dwarf and 1 Sylvan · -25% ores and crops |
| ⛏️ **Underground Industry** (*Industrie Souterraine*, mining) | Haste II on your land below layer 30 · raw ores ×1.5 · Nexus traps 50% cheaper | Max 1 Ranger · level II potions forbidden · +20% magic damage taken |
| 🌾 **Rural Enclave** (*Enclave Champêtre*, nature) | +2 hearts of health · Regeneration I and Saturation on your land · crops, animal loot, wood and stone ×2 · animals breed 3× faster | No Engineer · max 1 Blacksmith · Netherite armor forbidden |
| 💰 **Merchant Syndicate** (*Syndicat Marchand*, trade) | Only guild allowed to trade with villagers before Age 4 · +20% on monster bounties (10% of which goes to the bank) · no Auction House tax | No Arcanist · raids 30% more expensive · no combat bonuses · Nexus -20% health |
| 🔮 **Arcane Conclave** (*Conclave Arcanique*, magic) | Nexus +25% health · enchanting and anvil cost half the XP · potions one level higher · boosted enchantments (IV becomes V, V becomes VI) · the Nexus fires a beam at the nearest attacker (1 heart every 2 s) · -50% magic damage | -2 hearts of health · land 50% more expensive · max 1 Blacksmith and 1 Dwarf · boosted enchantments are lost if you change specialization |
| 🏹 **Lone Ranger** (*Rôdeur Solitaire*, solo) | Permanent Speed I · Netherite armor and enchanting from Age 3 | A single member, a Ranger who has never been in another guild · no chests on their land · Nexus capped at 2,500 HP |
| 🔱 **Atlantean** (*Atlante*, ocean) | **While in water**: water breathing, night vision, aquatic agility, dolphin's grace and Resistance | **Nexus must be submerged** (at least one water block against it) · **Slowness out of water** |

> [!IMPORTANT]
> The first choice is immediate. After that, a guild can only change specialization **when a new player** (who has never been in the guild) joins with **at least 2 h of playtime** (or when a member of a defeated guild pledges allegiance), at most once every 7 days, and never during a raid, a siege or within 10 minutes of an attack on the Nexus. Each change grants the right to [move the Nexus](#moving-the-nexus-nexus-move).

**Atlanteans**: place the Nexus against a water block. Water is then allowed all around it (placing, buckets, flow) in the Nexus chunk; lava and obsidian remain forbidden. Members get every underwater advantage **in water** and suffer **Slowness I on dry land**.

---

## 6. The Nexus

The **Nexus** is the heart of your guild. You place it on your land with `/guild nexus place` (only once: you can't place it again to reset its health to 100%). **If it's destroyed, your guild loses everything** (see [If the Nexus falls](#if-the-nexus-falls)).

### Placement rules

- **Overworld only**, in one of your guild's claimed chunks.
- **16-block safety zone**: no water, lava or obsidian (crying obsidian included), and none can be added afterwards. This prevents moats and unbreakable walls.
- **Reachable**: it can't be placed under more than 3 solid blocks of rock (a siege only breaks blocks within 4 blocks of it).
- **The Nexus must be visible**: a hit only counts if the attacker can see it (range and line of sight are checked).

### Health, healing and protection

- The Nexus's HP is shown above it (hologram), including after a restart or reconnection.
- **Healing it**: a member throws a **golden apple** on it (or right-clicks it with one): +500 HP, or +2,500 HP with an enchanted golden apple. 30 seconds between two heals, and it's impossible during a raid or within 2 minutes of a player attack.
- **Who is spared**: the guardian golem and traps never target guild members (the golem doesn't turn on them even if they hit it) or members of a guild bound by a treaty (alliance, non-aggression pact or vassalage).

### The Nexus tech tree (`/nexus`)

The Nexus gains **experience** for every horde repelled (+200) and when you **sacrifice resources** to it (`/nexus sacrifice`, item in hand, within 10 blocks: coal, iron, gold, diamond, emerald, netherite, ender pearl, blaze rod…; capped at 3,000 XP a day). It thus rises from level 1 to level 6 (0, 300, 900, 2,000, 4,500, 9,000 XP).

Instead of buying disposable traps, officers **unlock** defenses in `/nexus`: you need the required level, the "parent" defense already unlocked, **items to sacrifice** taken from your inventory and, for the "living" defenses, to have **brought back and killed the matching mob in the Nexus chunk**. An unlocked defense is **permanent** (hordes no longer consume it); Underground Industry pays half the items. They target monsters and **intruders**, never members, allies (treaty) or players in creative or spectator mode.

| Defense | Lvl | Requirements | Effect |
| :--- | :---: | :--- | :--- |
| 🔥 Infernal Blaze (*Brasier Infernal*) | 1 | 64 coal, 16 flint | Within 8 blocks: 1 heart of damage every 2 s plus burning |
| 🕸️ Binding Web (*Toile d'Entravement*) | 1 | 128 string, 16 slime balls | Slowness II within 10 blocks |
| ⚡ Retaliation Lightning (*Foudre de Riposte*) | 2 | Blaze · 96 copper, 64 redstone | Strikes anyone who hits the Nexus (4 hearts, one bolt every 5 s per player) |
| 🤖 Guardian Automaton (*Automate Gardien*) | 2 | **Kill an iron golem in the chunk** · 8 iron blocks, 32 redstone | An iron golem attacks enemies; rebuilt 5 min after it dies |
| 🧪 Toxic Mist (*Brume Toxique*) | 2 | Web · 48 spider eyes, 16 slime | Poison I within 8 blocks (never kills) |
| 🔥 Blaze Shot (*Tir de Blaze*) | 3 | Blaze · **kill a blaze** · 24 blaze rods, 32 gold | Up to 3 fireballs every 5 s |
| 🐡 Guardian Shot (*Tir de Gardien*) | 3 | Lightning · **kill a guardian** · 64 shards + 32 prismarine crystals | Water beam (damage + slowness) every 4 s |
| 🐝 Bee Swarm (*Essaim d'abeilles*) | 3 | Mist · **kill a bee** · 32 honeycombs, 8 honey bottles | Up to 6 enraged bees stinging enemies |
| 🪄 Evoker Shot (*Tir d'Évocateur*) | 4 | Golem · **kill an evoker** · 64 emeralds, 1 totem | Magic fangs: 6 area damage every 9 s |
| 🟪 Shulker Shot (*Tir de Shulker*) | 4 | Guardian · **kill a shulker** · 8 shells, 16 pearls | Homing projectile + levitation every 6 s |
| 👻 Ghast Shot (*Tir de Ghast*) | 5 | Blaze · **kill a ghast** · 8 tears, 128 gunpowder | Area fireball (8 damage, breaks no blocks) every 14 s |
| 👁 Warden Shot (*Tir du Warden*) | 5 | Evoker · **kill a warden** · 8 echo shards, 4 catalysts | Sonic boom: 10 damage and knockback every 15 s |
| 🌊 Elder's Curse (*Malédiction de l'Ancien*) | 6 | Guardian · **kill an elder guardian** · 128 prismarine bricks, 8 nautilus shells | Mining Fatigue III and damage to all nearby enemies every 25 s |

Unlocking everything earns the *Archi-technician* achievement. Settings: `nexus.tech.*` (cost, damage, XP).

### Moving the Nexus (`/nexus move`)

Every **specialization change** gives the guild the right to **move its Nexus once**, within 7 days: an officer stands where they want it (inside the guild's territory) and types `/nexus move`. Placement rules apply (safety zone, Atlanteans in water, reachable Nexus). Health and defenses are kept; impossible during a raid, a siege or within 5 minutes of an attack.

### Nexus Russian roulette (`/nexus gamble`)

The **guild leader**, facing the Nexus (within 6 blocks), can try an all-in gamble (retype to confirm within 15 s, once per **72 h**, impossible during a raid). The bank must hold the **2,000-coin stake**, set aside as soon as you confirm:

- **50% (heads)**: the Nexus gains **+1,000 max HP** and the bank gets its stake back **plus 2,000 coins**.
- **50% (tails)**: it loses **1,000 max HP** and **1,500 HP** (it always survives at 1 HP, max HP never below 1,000), the stake is lost, and a **mini-horde of pillagers** appears with no warning at the foot of the Nexus.

*All or nothing* achievement on a win.

---

## 7. Raids, sieges and player combat

### Launching a raid

A raid means **paying a monster army** to attack another guild's Nexus.

1. Open `/ah` and click the **Wither skull** (at the bottom of the menu).
2. Pick a **ready-made horde** (armored zombies, incendiary skeletons, spiders, pillagers and ravagers) or **build your own** (up to 60 monsters).
3. Pick the target guild. The price is paid by **your guild bank**.

| Horde | Price | Monsters |
| :--- | :-: | :-: |
| 🧟 Armored zombies | 2,500 | 24 |
| 🏹 Incendiary skeletons | 3,500 | 20 |
| 🕷️ Toxic spiders | 3,000 | 30 |
| 👺 Pillagers and ravagers | 7,500 | 15 |

Possible options: **sappers** that blow up walls and dry out water and lava around them, and **fireproofing** so your monsters can cross lava.

### Conditions

- Only **officers and the leader** can launch a raid.
- With **no defender** online and on their land, a raid is still possible but the Nexus only takes **25%** of the damage.
- **Maximum duration**: 25 minutes; beyond that the raid ends with no victory or defeat. If a large part of the horde vanishes without fighting (unloaded chunk…), the victory isn't credited.
- After a raid, the targeted guild is **protected for 30 minutes**.
- **30-minute cooldown** between two raids launched by the same guild.
- You can't attack a guild you have a **treaty** with (see [Diplomacy](#10-diplomacy-and-travel)), nor during the Friday and Saturday **truce**.
- During a raid, only the horde's monsters and TNT lit by the attacking guild destroy the base (not a third party).
- **Tiny hordes earn nothing**: repelling a horde of fewer than 12 monsters gives no Nexus XP, season points, achievement or reinforcement bounty (`/aide`); nobody can bet on it and it doesn't count for the "Launch 3 hordes" quest.

### If the Nexus falls

- The winner is the guild that dealt **the most damage** to the Nexus in the last 10 minutes (not the one that lands the last hit).
- With no winning guild (Nexus destroyed by a player with no guild or by an administration horde), the territory is **vulnerable for 30 minutes**, then the Nexus **respawns** with a quarter of its HP: it can be healed, raided and besieged again.
- The loser's **territory** passes to the winner (within the limit of their Age).
- The loser's whole **bank** passes to the winner, with its **debts and receivables** (`/banque dettes`); if the loser was listed on the stock exchange, its shareholders are repaid first.
- Its treaties, stations and tolls disappear.
- The loser's members choose: join the winner (`/guild annex join`) or leave (`/guild annex decline`).
- A conquest only earns **season points** and the *Conqueror* achievement if the defeated guild was **established** (at least 2 members and 3 days old).

### Rich guilds are targets

A guild gets **Economic Hegemony** status if at least half of its members are in the top 5 richest players (`/baltop`), or if its **bank plus its members' coins** hold more than 30% of all the money on the server. Raids against it cost **half price** and its post-raid protection only lasts **2 minutes**. It's marked with a ⭐ in the target menu.

### Balance: revenge and catch-up

- **Revenge**: the guild that loses a raid gets **24 h of hordes at -50%** against its conqueror.
- **Catch-up**: guilds **below the season median** earn up to **+25%** on monster bounties (at least 4 guilds). The bonus shrinks as they catch up and disappears at the median.

### Formal wars: sieges (`/siege`)

An officer plants a **Siege Banner** on a rival guild's **border** (in a chunk next to theirs, on free ground or their own land, in an empty space on solid ground) with `/siege declarer <guild> <day> <hour>` (e.g. `samedi 21`).

- **24 hours' notice**, 500 coins fee taken from the bank, a one-hour slot that can't overlap the truce, impossible under a treaty.
- The target can **cancel the siege by destroying the banner** before the time (and keeps the fee).
- During the slot, members of the besieging guild can **tear down the enemy Nexus with pickaxes or heavy weapons**: damage ×2, even with no defender present, without buying a horde.
- **Victory** if the Nexus falls or 40% of its max HP is dealt: 10% of the loser's bank (3,000 max) is looted. Otherwise the defender keeps the fee.
- A guild leads only **one siege at a time** but can face up to 3; if the target disappears before the siege, the fee is refunded. `/siege liste` shows sieges, `/siege annuler` gives up (half the fee refunded).

History in the website Pantheon. *Siege master* achievement.

### Player combat (PvP)

Hitting a player from another guild (hit, arrow, TNT, wolf, harmful potion…) or being hit puts you "in combat" for 15 seconds. During that time:

- **`/hub`, `/station`, `/pay`, `/ah`, `/banque`, `/guild bank|home|leave`, `/home`, `/tpa`… are blocked**, and so are command teleports;
- **logging out is punished by death** (kicks included; only a server shutdown is excused);
- spending or transferring your coins is impossible (purchases, `/pay`, bank, profession, disguise); only paying a ransom remains allowed;
- **chests are closed** (chest, barrel, shulker box, ender chest, minecart, donkey with chest), and you can neither donate items to your guild's Age nor sacrifice them to the Nexus;
- the loot of a player who dies goes to their killer.

**Killing a player** from another guild: you take **100 coins + 1% of their fortune** (or everything they have if they have 100 coins or less). No bounty if you already fought each other less than 2 minutes ago, nor between members of the same guild.

### Ransom (`/rancon`)

A blow that would **kill** a player from another guild **captures** them instead: they're immobilized and invulnerable for 90 seconds. Their captor chooses:

- `/rancon prix <amount>` sets the ransom (100 to 2,000 coins; by default 15% of the captive's fortune);
- the captive pays with `/rancon payer`: 30% to the captor, 70% to their guild's bank;
- `/rancon liberer` frees them for free, `/rancon executer` kills them (usual death bounty).

With no decision, they're executed. Logging out while captive counts as dying. After a ransom payment, they can't be recaptured for 10 minutes.

**Prison cell**: an officer aims at a block of their territory and types `/rancon cellule` (`/rancon cellule retirer`). The captive is then **teleported** there during the ransom (then returned to their capture point). Their allies (same guild or treaty) can **rescue** them by acting 15 times on the cell block (right or left click); remember to close the cell!

### Honor duels and bets (`/duel`, `/pari`)

- `/duel <player> [stake]` challenges a player (optional stake in coins, `/duel accepter` or `/duel refuser`). Both are **teleported to a neutral arena** lit above the world (one platform per duel, 6 at a time, **indestructible** and repaired before each duel); their **inventory, health and position are saved** then returned at the end (even after a crash, on reconnection). They fight with the **same supplied gear** (full iron armor, iron sword and axe, shield, bow, 16 arrows, 64 steaks, 1 instant health potion).
- **Nobody dies**: the decisive blow ends the fight. The winner takes the opponent's stake. A duel **triggers no rivalry, betrayal, capture or combat mark**, and isn't stopped by the truce. Maximum 5 minutes (draw beyond that, stakes returned); giving up = losing. *Weapons master* achievement (10 duels).
- `/pari <side> <amount>`: at the start of a **raid** or a **duel**, **spectators** (outside the guilds involved / not the duelists) have **2 minutes** to bet (10 to 1,000 coins **in total** per bet) on the `attaquante`/`defensive` guild (or its name) or on a duelist. The winning side shares the pot minus **5%** taken by the server; with no opposing bet, or if the fight is cancelled, everything is refunded. **Anti-cheat**: members of the guilds involved **and guilds bound to them by a treaty**, any player whose **IP address** matches an online participant's, and accounts under 3 days old are excluded.

### Call for help (`/aide`)

During a raid or an attack on your Nexus, `/aide` alerts your **allies** (alliance or vassalage). Each ally who joins your land before the horde ends earns **250 coins** (taken from your guild's bank) if it's repelled (6 allies at most), provided it's a real horde (at least 12 monsters, two victories counted per day against the same attacker). One call every 10 minutes.

---

## 8. Ages

Your guild starts at **Age 1** and can go up to **Age 6**. To reach the next Age, members **donate resources** in the `/age` menu (or item in hand with `/age deposit`). Each Age unlocks gear and raises how much land you can hold.

| Age | What it unlocks |
| :--- | :--- |
| 1. **Rustic** (*Rustique*, start) | Wood and stone. Iron is reserved for specialized professions. Nether and End closed. |
| 2. **Metallurgy** (*Métallurgie*) | **The Nether**, iron tools and shields for everyone. Diamond is reserved for specialized professions. Stations and freight. |
| 3. **Alchemy** (*Alchimie*) | **The End**, diamond tools and iron armor for everyone. Netherite is reserved for specialized professions. |
| 4. **Total War** (*Guerre Totale*) | For everyone: diamond armor, villager trading, TNT, redstone, pistons, brewing stands, splash potions, bows and crossbows. Guild stock exchange. **Player bonuses**: Speed I, +10% damage, +25% XP, -20% explosion damage. |
| 5. **Mythic** (*Mythique*) | Netherite and beacons for everyone, unlimited land. **Player bonuses**: Haste I, Speed I, Resistance I, +15% damage, +50% XP. |
| 6. **Supremacy** (*Suprématie*) | The ultimate Age. **Reward**: a dragon egg and a block of Netherite for every member, plus a chat title: **[Giga Tchad]** if your session is under an hour, **[Gros Nerd]** beyond that. |

> [!NOTE]
> The amounts below are for a **one-player guild**. They grow with the number of members (×1.3 with 2 players, ×2.2 with 5, ×4.7 with 10), based on the **highest member count** reached during the Age. The `/age` menu always shows the exact amounts for your guild.

<details>
<summary><b>Resources to donate for each Age</b></summary>

- **Age 2**: 750 copper ingots, 256 leather, 750 raw iron, 1,000 oak logs, 1,500 wheat, 750 carrots.
- **Age 3**: 3,500 iron ingots, 1,500 gold ingots, 1,500 lapis lazuli, 1,000 quartz, 750 nether wart, 150 blaze rods, 150 magma cream, 15 ghast tears, 750 glowstone dust, 3,500 feathers, 6,000 potatoes, 6,000 sugar cane, 1,000 pumpkins, 2,500 melon slices, 1,500 beetroot.
- **Age 4**: 500 diamonds, 150 iron blocks, 100 gold blocks, 1,000 obsidian, 200 crying obsidian, 500 ender pearls, 1 pair of elytra, 2,500 golden carrots, 150 golden apples, 1,200 hay bales, 800 honey bottles, 800 slime balls, 6,000 redstone dust, 400 repeaters, 350 pistons, 200 sticky pistons, 150 dispensers, 2,500 gunpowder, 250 TNT, 750 rockets, 32 anvils.
- **Age 5**: 16 Netherite ingots, 5 nether stars, 4 beacons, 8 totems of undying, 16 echo shards, 16 catalysts, 16 shriekers and 64 sculk sensors, 64 shulker shells, 2,500 end stone, 1,200 purpur blocks, 2,500 chorus fruit and 1,000 popped chorus fruit, 1,000 redstone blocks, 400 comparators, 400 observers, 250 hoppers, 150 targets, 150 emerald blocks.
- **Age 6** (fixed amounts): every music disc, every mob head, the 8 goat horns, 64 nether stars, a Netherite hoe enchanted Mending + Unbreaking III + Fortune III, every smithing template, the 20 pottery sherds, the dragon egg and a dead bush.

</details>

---

## 9. Money, the market and the stock exchange

### Earning coins

- **Killing monsters**: every monster pays a bounty, from 1.50 coins (silverfish) to 750 coins (Ender Dragon). Repetitive monster farms pay less, and an hourly cap applies.
- **Daily contracts** (`/contrat`): 3 missions a day, one tied to your profession. +400 coins if you do all three. Monsters from spawners, eggs or commands don't count for "kill" contracts.
- **Auction House** (`/ah`): sell and buy between players.
- **Events** and the **world boss** (see [Server life](#11-server-life)).
- **Killing a player** from another guild, or capturing a player for ransom.

<details>
<summary><b>Monster bounty table</b></summary>

| Category | Monsters and bounty |
| :--- | :--- |
| 🐉 Boss | Ender Dragon 750 · Warden 500 · Wither 350 · Elder Guardian 150 |
| 👺 Mini-boss | Ravager 75 · Evoker 40 · Piglin Brute 25 |
| 🔥 Nether and End | Ghast and Shulker 20 · Wither Skeleton and Guardian 15 · Blaze, Hoglin and Zoglin 12 |
| 🏹 Elite | Vindicator 15 · Witch 12 · Enderman 10 · Pillager 8 |
| 🧟 Common | Creeper and Phantom 5 · Piglin and Zombified Piglin 4 · Spiders 3 to 3.50 · Skeletons 3 · Zombies 2.50 |
| 🐛 Small | Vex 2.50 · Slimes and Magma Cubes 2 · Silverfish and Endermites 1.50 |

The Merchant Syndicate earns 20% more.
</details>

### The guild contract

In `/contrat`, below your personal contracts: every day your guild gets **three objectives, each reserved for one profession** (for example: a Dwarf mines, a Blacksmith smelts ingots, an Engineer places redstone). Only members with the right profession advance their objective. Each objective pays **200 coins** into the guild bank, and all three together **750 more**. Objectives grow with the size of the guild, and only pay guilds of at least 3 members.

### Spending

Guild bank (`/guild bank <amount>`) for land, traps and raids · transfers (`/pay`) · Auction House purchases · train trips · profession changes · cosmetics · disguise.

### The Auction House (`/ah`)

- **Selling**: hold the item and type `/ah sell <price>` (10 listings max, price from 1 to 1,000,000). A **5%** tax is taken on every sale (none for the Merchant Syndicate). Click your own listing to take it back.
- **Raids**: the Wither skull at the bottom of the menu opens the hordes menu (see [part 7](#7-raids-sieges-and-player-combat)).

#### Buy orders

Besides listings, the Auction House takes **buy orders**: "I'm buying 64 diamonds at 30 coins each".

- **Placing an order**: hold the wanted item and type `/ah commande <quantity> <unit price>`, or `/ah commande <material> <quantity> <unit price>` (English name, e.g. `diamond`). The total amount is **set aside** at once. 5 open orders at most, 2,304 items at most per order.
- **Delivering**: `/ah commandes` (or the book in `/ah`) lists the orders, best-paid first. One click delivers everything you have (up to the order's size) and you're **paid at once**, minus the market tax. Only "plain" items count (no name, enchantment or wear).
- **Collecting**: on your own order, left-click to collect delivered items, right-click to **cancel** (the undelivered part is refunded). Delivered items wait until you collect them, even offline.

### Guild bank: loans and shared chest (`/banque`)

Between two guilds **bound by a treaty** (alliance, pact or vassalage):

- **`/banque` opens the bank menu**: received offers (left-click to accept, right-click to refuse), debts (left-click to repay everything, right-click to repay 1,000 coins), receivables (Shift + right-click to erase a debt, leader only), sent offers (click to withdraw them), and access to the shared chests.
- **Offering a loan** (officers): "Offer a loan" button, pick the partner guild, then the **amount** and the **interest rate**, adjustable from **0 to 50%** (10% by default). By command: `/banque pret <guild> <amount> [rate]`. A loan is capped at 1,500 coins per claimed chunk of the borrower (100 to 20,000 coins).
- The offer stays valid for 10 minutes. Once accepted (menu or `/banque accepter <guild>`), the coins move from one bank to the other; the borrowing guild must pay back the amount **plus interest at the agreed rate** within **5 days**.
- `/banque dettes` lists loans · `/banque rembourser <guild> [amount]` repays (in several installments if you like) · `/banque pardonner <guild>` erases a debt (**leader only**). A loan offered by an officer counts toward their 3,000-coin daily cap.
- `/banque coffre <guild>` opens a 54-slot **shared chest** between the two guilds (accessible as long as a treaty binds them).
- **An unpaid debt weighs on territories**: once the deadline passes, the guild can no longer claim chunks, and **one chunk is seized from it every 24 h** (never the Nexus one, never one that would split the territory). Each seized chunk erases 300 coins of debt.

### The stock exchange (`/bourse`)

From **Age 4**, the leader can **open the guild's capital** (`/bourse ouvrir`, 1,000 shares). A share's **price** isn't fixed: it's a real market, updated every 10 minutes.

**What makes a guild valuable** (fundamental value, detailed by `/bourse info <guild>`):

- its **wealth** (equity: bank minus net debts), its **territory** (chunks) and its **headcount**;
- its **members' reputation** (honor points from `/merci`);
- the guild's **Age**, its **season results** and the **diversity** of its professions;
- the strength of the **Nexus**: level, unlocked defenses and remaining health (a destroyed Nexus makes the value collapse);
- **arrears** (an overdue debt costs 15%).

**How the price moves**: it drifts toward the fundamental value (15% of the gap at each update) with **random noise** and the **general market mood** (optimistic or pessimistic, pulling all prices the same way). Immediate **shocks** follow events: horde repelled +2%, siege won +4% / lost −4%, siege repelled +3% / failed −3%, boss slain +3%, **Nexus destroyed −35%**. And finally, **your orders matter**: buying 10% of the capital raises the price by 8%, selling lowers it.

**Buying and selling**: **outside** players buy shares (`/bourse acheter <guild> <shares>`, 100 per player, 49% of the capital at most) and sell them back to the guild at 95% of the price, **after holding them 24 h** and never earning more than 3 times the price paid. Every 6 hours, **20% of the guild's profit** (the rise in its equity) is spread across the 1,000 shares: shareholders receive their shares' portion, the guild keeps the portion of the shares it hasn't sold. `/bourse` shows prices (▲▼ over 24 h), `/bourse portefeuille` your shares; the website's **Stock exchange** page shows prices, change and mini-charts.

A guild that is **disbanded or annexed** leaves the market: before disappearing, its bank **buys back the public's shares**, at most at the price paid (and at market value), pro rata if it can't cover it all. *Shareholder* achievement. All factors are set in `bourse.*`.

### The travelling guild merchant (`/marchand`)

Every day (around 2 pm), a **rare merchant** settles for 6 hours near the Nexus of a **randomly drawn guild** (Nexus placed, at least 4 claims, a member online; never the same guild two days in a row). The whole server is told and **anyone can come and see him**: a chance to cross foreign land, negotiate a passage… or set an ambush.

- He sells 5 items drawn from: Mending, Unbreaking III, Efficiency IV, Fortune III books, trident, heart of the sea, netherite upgrade template, sniffer egg, saddle, name tags, bottles o' enchanting, ancient debris, shulker shells, golden apples. Payment in **emeralds** or **diamonds**, 3 copies per offer, **2 purchases per player per day**.
- He requires no Age: anyone can trade with him.
- The host guild gets **75 coins** in its bank for each purchase by a foreign visitor: it pays to be welcoming.
- `/marchand` shows where he is and when he leaves. If the land hosting him is lost, he leaves.

### The resource world (`/ressources`)

A **separate mining world**, reset **every week** (Monday at 5 am by default). It has **fresh ore** and wild land, so the main world doesn't turn into Swiss cheese. **No territory** can be claimed there: **PvP is free** everywhere.

- `/ressources`: go down (random safe landing spot within a 6,000-block border; not in combat, in a duel or in captivity; 5 minutes between trips).
- `/ressources quitter`: go back to where you came from (or your bed, or spawn).
- `/ressources info`: date of the next reset.

Players inside are warned 60, 30, 10, 5 and 1 minute before the reset, then sent back to the surface. A player who logged out in the old world comes back at spawn when they reconnect. Portals don't work there. **Don't leave anything valuable down there!**

---

## 10. Diplomacy and travel

### Treaties (`/traite`)

An officer proposes a treaty to another guild, which has 10 minutes to accept (left-click) or refuse (right-click) in their `/traite` menu.

| Treaty | Duration | Effects |
| :--- | :--- | :--- |
| 🤝 **Alliance** | Unlimited | No raids between you · free trains at the other's stations · you can't hurt each other (hits, arrows, potions) |
| 📜 **Non-aggression pact** (*Pacte de non-agression*) | 7 days | No raids between you · **hitting a player of the other guild or its Nexus breaks the pact** (betrayal + rivalry) |
| 👑 **Vassalage** (*Vassalité*) | Unlimited | Like an alliance, but the vassal pays 5% of its bank to its overlord every day |

**Breaking a treaty is a betrayal**: your guild pays up to 500 coins in compensation to the other, the whole server is told, and you can't attack them for 30 minutes. Only exception: an overlord can free its vassal for free.

**Aggression under a treaty**: if a player attacks the Nexus of a guild bound by a treaty (pact, alliance or vassalage, including the overlord against its vassal), or hits a member of a guild under a pact (hit, arrow), the treaty is broken automatically. It's a betrayal (compensation, 30-minute notice, server announcement) and a **rivalry** opens between the two guilds (visible on the website, diplomacy page).

**Calming a rivalry**: it fades on its own over time (the weight of raids, betrayals and deaths is halved every 7 days) and **disappears as soon as the two guilds sign a treaty**.

### The train (`/station`, `/rail`)

The rail network lets you **teleport from station to station**, provided a **continuous rail line** connects them (branches and crossings included).

- **Traveling**: near a station (`[Gare]` sign), type `/station` and pick your destination.
- **Price**: 1 coin for the arrival station's guild, plus the **toll** of every guild whose land you cross. Your own guild and its allies don't charge.
- **Building a station**: an officer places a sign with `[Gare]` on the 1st line and the station's name on the 2nd, within 2 blocks of a rail. You need Age 2, pay 500 coins from the bank (within the officers' daily cap), and no more than 6 stations per guild.
- **Managing your network** (`/rail`, officers): set your toll (**up to 10 coins for every specialization, up to 100 for the Merchant Syndicate**), and for each other guild choose **Normal**, **Allied** (free) or **Banned** (lines through your land are cut for them).

#### 📦 Freight train

Officers only, **Age 2 minimum**. **Shift + click** a destination in the `/station` menu to open a **double chest (54 slots)**. Close it, then click **[+ COFFRE]** to load another or **[ENVOYER]** in chat (fee: arrival fare ×5 per filled chest).

| Guild Age | 2 | 3 | 4 | 5 | 6 |
| :--- | :-: | :-: | :-: | :-: | :-: |
| Double chests per convoy | 1 | 2 | 3 | 5 | 10 |

A **chest minecart** leaves on its own and follows the rails, **slowly**: **4 min** under 1,000 blocks, **8 min** up to 3,000, **12 min** up to 5,000, **15 min** up to 10,000, **20 min** beyond. It isn't a real minecart: its position is computed from the clock, it is only visible near players and survives restarts.

On arrival, items useful for the next Age are **donated automatically** if the station is yours; the rest goes into the **arrival station's box, open to every player present** (`/station fret recuperer [chest number]`, up to 30 chests): freight also works as an **exchange between guilds** (the station's guild is told of the arrival). Two convoys per guild at most.

**🏹 Attacking a convoy**: when a convoy crosses the territory of a guild with **no treaty** with the sender, its members hit it (**25 hits**) to **loot** it: the loot spills on the rails. Both guilds are warned at each border crossed. *Convoy looter* and *Freight king* achievements.

---

## 11. Server life

### Events (one every 1 to 2 days)

| Event | Duration | What happens |
| :--- | :--- | :--- |
| 🌑 Blood Moon (*Lune de sang*) | 10 min | More monsters, bounties ×2 |
| ☄ Meteor Shower (*Pluie de météorites*) | 3 min | Ore blocks fall from the sky near players (on natural ground, never on a build) |
| 🧙 Black Market (*Marché noir*) | 5 min | A merchant offers 3 randomly picked rare items, **32 to 128 diamonds** each. **Each offer is locked after a single purchase** and each player can only buy **one thing** |
| ☠ World Boss (*Boss mondial*) | 15 min | A **different boss every week** (see below): 1,000 coins shared by damage dealt (at least 10% of the damage, 400 coins max per player), and for the top fighter 3 diamonds + a 20% chance of a Nether Star |

### ☠ The world boss in detail

- **One boss a week**: *Lord of Ashes* (forge, +25% damage from **Dwarves**), *Forest Guardian* (woodland beast, +25% from **Rangers**) or *Ice Queen* (+25% from **Blacksmiths**).
- **Countdown**: **2 minutes** before it awakens, a full-screen title and a **horn** warn everyone; the message gives the **distance and direction** from your position. Reminder 30 s before.
- **Adapted difficulty**: HP = **300 per player** within 150 blocks at the moment it awakens (400 to 1,000 HP), damage of 8 + 2 per player.
- **Three phases** (different bar color): at **75%** it summons **spectral guards**, at **50%** it casts a **shockwave** (an 11-block circle that knocks back), at **25%** it **enrages** (faster, stronger, more frequent attacks).
- **Telegraphed attacks**: before each area strike, a **particle circle** appears on the ground for **3 seconds**: get out of it!
- **Heroic mode**: after the server's first victory, the boss is heroic one time in two (HP ×1.5, damage ×1.3, pot ×1.5, more diamonds); *Heroic legend* achievement.
- **Guild bonus**: the top fighter's guild receives **500 coins** in its bank, and its name is engraved in the website's **Pantheon**.

### ⚖ Guild Court (`/vote`)

Every **Monday**, guild members vote with `/vote` (clickable list) on a **rule for the following week** (Tuesday to Monday). Each established guild (active Nexus, at least 2 members and 3 days old) weighs one vote (its choice = the option most voted by its members); ties are broken by number of voters. The verdict is announced and written in the journal.

| Rule | Effect |
| :--- | :--- |
| Toll tax | Rail tolls ×2 |
| Building bonus | Land -25% |
| Free market | No Auction House tax |
| Open hunt | Monster bounties +25% |
| Extended truce | The truce also starts on Thursday and lasts twice as long |

### 🕊 Weekly truce

On **Friday and Saturday at 9 pm**, **one hour with no PvP** (hits, arrows, harmful potions) for everyone, **and no attacks on a Nexus, no hordes launched, no convoys looted**; honor duels are exempt. A siege whose time falls during a truce waits for it to end.

### Guild seasons

Every month, `/classement` ranks the guilds (territory, Age, wealth, members, hordes repelled, conquests). The champion guild wins **5,000 coins**, **500 per member present for at least 7 days** and a 👑 crown in chat. `/histoire` tells the server's latest great moments.

### 🍂 Natural seasons (`/climat`)

The world changes season **every two weeks**:

| Season | Effect |
| :--- | :--- |
| 🌸 Spring | Crops grow faster (+50%) |
| ☀ Summer | +20% experience |
| 🍂 Autumn | A one-in-two chance of a bonus harvest |
| ❄ Winter | Crops grow half as fast, monster bounties +25% |

### 🍺 Taverns (`/taverne`)

The **Merchant Syndicate** and the **Rural Enclave** can declare up to 2 chunks of their territory (never the Nexus chunk) as **neutral zones** with `/taverne declarer` (`/taverne retirer`). The chunk must contain **an empty chest, a barrel and a roof** over both. Inside, **PvP, ranged weapons and offensive potions are disabled**, except between two players **already in combat**: a tavern is not a refuge to escape a fight.

`/taverne` opens the games menu, all played against the **owning guild's bank** (the maximum bet adapts to what the bank can pay; 1,000 coins at most):

| Game | Rule |
| :--- | :--- |
| 🪙 **Coin flip** | 47% chance to double your bet |
| 🍻 **The bar** | A drink (50 coins, or 5 / 10 at once) gives one draw: **0.0001%** enchanted golden apple, **1%** golden apple, **12%** 8 steaks, **12%** 16 baked potatoes, **50%** a *Kastell red pint* (nausea, 30 s), the rest a random suspicious stew |
| 🎡 **Roulette** | European (0 to 36): red/black, even/odd, low/high ×2, dozens ×3, straight number ×36; 0 makes the house win |
| 🎰 **Slot machine** | Three reels: 7-7-7 ×50, diamonds ×20, bells ×10, lemons ×6, cherries ×4; two cherries ×1.5; one cherry ×0.5 |
| 🎲 **Dice** | Two dice against the house's two dice: higher total ×2, tie: half the bet back |
| 💪 **Arm wrestling** | Skill game against the tavern champion: click the fist as soon as it appears (1.3 s at first, 0.6 s at the shortest). A good click bends his arm, a missed or slow click pushes you back, and he also pushes on his own, more and more often. Watch out for **feints** (red block ✋): don't touch them, or you lose two notches. Win ×1.8; 6 games a day |
| 💣 **Greed** | A multiplier starts at ×1.00 and climbs faster and faster: click **CASH OUT** to pocket bet × multiplier before it **blows up**, or lose the bet. The blow-up point is random (4% of games blow up right away); capped at ×20, cashed out automatically. Closing the menu mid-game means forfeiting the bet |
| 🐖 **Pig race** | Four pigs (Rosette, Jambonneau, Truffe, Saucisse): bet on the winner, ×3.6 |

Winnings come out of the tavern's bank, losses go into it. **Everyone can play**, including members of the owning guild against their own house. *Tavern keeper* achievement.

### ❤ Reputation (`/merci <player>`)

Thank a helpful player: they gain **1 honor point**. You can thank the same person **once a day**, **5 people a day at most**, with **60 seconds** between two thanks; a player can only receive **10 a day**. No thanks between members of the **same guild**, nor between players who play (or played in the last 30 days) from the **same connection**, and the account must be **2 days old** (reputation weighs on the stock price). Points show in the **tab list** (❤), on the **website profile** and in the "Most honorable" ranking. `/merci` alone shows your reputation and the top 3.

### 🎓 Mentoring (`/parrain <player>`)

An advanced player (profession chosen, in a guild, 15 h of play) can guide a **recruit** (joined less than 14 days ago, 5 recruits at most). The recruit accepts with `/parrain accepter`. The mentor is rewarded when the recruit hits milestones: **first profession** (+200 coins, +50 for the recruit) and **first territory** of their guild (+300, +100 for the recruit). Mentor and recruit must never have played from the same connection. `/parrain` shows progress.

### 🎭 The disguise (`/deguisement`)

For **750 coins**, you change **skin and name** for **5 minutes**:

- `/deguisement <guild>`: you pass yourself off as a member of that guild (skin of one of its members, invented name, guild tag in chat and the player list). Its traps and golem ignore you.
- `/deguisement <Name>`: you take the skin and name of a Minecraft account **foreign to the server** (`/deguisement Notch`). Names of the server's players are **refused** (impersonation): to imitate someone from here, use their guild. Impossible if that player is online.
- `/deguisement off`: removes the disguise (original name and skin restored).

Territory protections stay active. **Dealing damage or hitting the Nexus unmasks you**, and the imitated guild is warned. One disguise every 30 minutes; coins are only charged if the skin was actually found. Death messages don't reveal your real name.

### ✨ Cosmetics (`/cosmetique`)

Three slots, one cosmetic equipped in each:

- **Auras**: particles around you.
- **Trails**: particles behind your steps.
- **Victory effects**: a burst of particles when you kill a player from another guild.

You get them in three ways:

- **Achievements**: each achievement unlocks its own aura (26).
- **Quests**: game goals that unlock an aura, trail or effect: daily contracts completed (10, 50), monsters killed (250, 1,000), train trips (20, 25), calls for help (5, 10), hordes repelled (5, 10, 20), hordes launched (3), players killed (5, 25, 100).
- **The server shop**: 15 cosmetics from **3,000 to 15,000 coins** (personal coins, charged once: click to buy, second click to confirm). Impossible mid-combat.

The `/cosmetique` menu has one tab per slot; locked cosmetics say how to get them. `/cosmetique off` removes everything. There are also secrets to discover…

### 📰 War journal and achievements

`/journal` summarizes the latest big events (raids, conquests, treaties, betrayals, Ages, events). `/succes` lists your **26 achievements** (first profession, conqueror, traitor, master spy, four seasons, land area from 16 to 2,048 chunks…) and the percentage of players who have them. An achievement earned by fewer than 10% of players is **rare** and announced in the journal.

### The game in English

`/english` translates the game **for you alone**: messages, action bar, menus, dashboard and the `/guide` book (reply `/francais` to go back). Your choice is remembered. Guild and player names aren't translated. Not yet translated: the events boss bar, chat and war journal entries.

### No private messages

Private messages are **disabled**: `/msg`, `/tell`, `/w`, `/r` and their equivalents (including `minecraft:msg` or Essentials ones) are refused and no longer appear in auto-complete. Talk in public chat or your guild's.

---

## 12. The website

The server has a public website: **`http://<server-address>:8080/`**, which updates by itself (every 30 seconds). It shows **no coordinates**: nothing that helps find a base.

| Page | Contents |
| :--- | :--- |
| Home | Online players, top guilds, natural season, raids in progress, recent journal |
| Guilds · guild page | Ranking; members, treaties, diversity, trophies, growth curves, Ages reached, highlights |
| Players · profile | Online, richest, directory; each player's profile (profession, guild, statistics, achievements, reputation) |
| Raids | Hordes in progress: target, attacker, monsters left, duration |
| Journal | The full war journal, filterable by category |
| Diplomacy | Network of alliances, pacts, vassalages and rivalries, rivalry map |
| Museum (*Musée*) | Guild trophies, all achievements with their rarity, latest unlocked |
| Ages | Timeline on a real time axis and the requirements for each guild's next Age |
| Stock exchange (*Bourse*) | Guild share prices, 24 h change, mini-charts |
| Pantheon (*Panthéon*) | Slain bosses (with the guild engraved), sieges, Court rule, week's titles |
| Votes | Weekly elections |
| Guide | All the rules, with search |

The site is also available **in English**: **EN** / **FR** button at the top right, or `?lang=en` at the end of the address. Data is also available as JSON: `/api/stats.json`, `/api/journal.json`, `/api/raids.json`.

### 🗳 Website elections

Every week, the community elects on the website (`/votes`) the most **clueless**, the **biggest**, the **funniest** and the most **annoying** player.

1. On the website, click "Revendiquer ce pseudo" (on your profile or the votes page): a **code** (`ESI-XXXX`) appears.
2. **Type that code in the game chat** with that account (the message isn't broadcast).
3. The game asks for a **confirmation** (`[CONFIRMER]` / `[REFUSER]`, with the requester's approximate address: never confirm a request you didn't make).
4. A **cookie** (90 days) then recognizes your browser.

One vote per category, changeable, never for yourself; accounts under 2 days old are excluded, and **only one account per connection and category**. `/webvote optout` removes you from the elections. The week's winners are announced in chat and shown in the **Pantheon**.

---

## 13. For administrators

<details>
<summary><b>Installation, configuration and admin commands</b></summary>

### Requirements and installation

- **Server**: Paper 26.2, **Java 25**. No dependency on any other plugin (no Vault, no PlaceholderAPI): the economy is built in.
- **Build**: `./mvnw package` on Linux/macOS, `.\mvnw.cmd package` on Windows. The plugin is created in `target/` (`esicraft-plugin-<version>.jar`). Every change pushed to GitHub is also built automatically (GitHub Actions, the `.jar` is available as an artifact).
- **Install**: copy the `.jar` into the server's `plugins/` folder and restart. `config.yml` is created in the plugin folder.
- **Storage**: SQLite database in the plugin folder (`storage.type`, `SQLITE` or `JSON`), saved every 180 s.
- **`/hub`** sends the player to the `hub` server of a proxy (Velocity or BungeeCord).
- **Disguises behind Velocity**: enable **modern** info forwarding (`player-info-forwarding-mode = "modern"` in Velocity and `velocity-support` in Paper). The server must be able to reach `api.mojang.com` and `sessionserver.mojang.com`.

### Permissions

| Permission | Default | Role |
| :--- | :--- | :--- |
| `esicraft.user` | everyone | Gameplay features |
| `esicraft.admin` | operators | Admin commands |

### Admin commands

| Command | Use |
| :--- | :--- |
| `/esicraft reload` | Reload `config.yml` and the Ages configuration |
| `/esicraftadmin setage <guild> <level>` | Change a guild's Age |
| `/esicraftadmin spawnraid <guild> <type>` | Launch a raid against a guild |
| `/esicraftadmin givecoins <player> <amount>` | Give coins |
| `/esicraftadmin forcenexus <guild>` | Repair a Nexus |
| `/money get\|set\|give\|take <player> [amount]` | Manage player money |
| `/money top` | Ranking of the richest |
| `/money rectify <factor>` · `rectify status <player>` · `rectify reset [player\|all]` | Anti-farm factor for monster bounties |
| `/race set <player> <race>` | Change a player's profession (`DWARF`, `SYLVAN`, `BLACKSMITH`, `RANGER`, `ARCANIST`, `ENGINEER`, `NONE`) |
| `/evenement <lune\|meteores\|marchand\|boss\|stop\|status>` | Start or stop an event |
| `/marchand forcer` · `/marchand partir` | Make the travelling merchant arrive or leave |
| `/guild cooldown reset [guild]` | Reset horde cooldowns and stop raids (one guild or all) |
| `/pvp [on\|off\|toggle]` | Turn Nexus destruction on or off |
| `/game reset` then `/game reset confirm` | Reset **everything**: guilds, Nexus, fortunes (100 coins per player), achievements, statistics, contracts, reputation, mentoring, cosmetics, journal, curves, Ages, season, loans and the website (only language preferences are kept) |

### Main settings (`config.yml`)

| Section | Contents |
| :--- | :--- |
| `guilds` | Creation and land prices, max members, Nexus health and healing, officer cap |
| `ages` | Resources required for each Age |
| `raids` | Horde prices and sizes, max duration, delay between raids, custom horde cap |
| `nexus` | Siege radius, undefended damage, relocation, Russian roulette, tech tree (`nexus.tech.*`) |
| `siege` | Notice, duration, fee, damage multiplier, loot |
| `races` | Delay between two profession changes |
| `market` | Auction House tax and max listings, buy orders |
| `economy` · `catchup` · `revenge` | Hourly bounty cap, catch-up, revenge |
| `events` | Event frequency, world boss (`events.boss.*`), black market |
| `truce` | Truce days, time and duration |
| `court` | Guild Court voting conditions |
| `duels` · `betting` | Stakes, duration, arena, bets |
| `taverns` | Max bet and each game's settings |
| `resource-world` | Name, border, reset day and hour |
| `guild-merchant` | Time, duration, offers, commission |
| `rail` | Trip and station prices, max toll, minimum Age, freight |
| `treaties` · `rivalry` | Pact duration, tribute, compensation, grace period, ally protection, rivalry half-life |
| `loans` · `ransom` | Rates, deadlines, seizures · ransom duration and amounts |
| `bourse` | Prices, volatility, dividends, value weighting (`bourse.weights.*`) |
| `reputation` · `mentoring` · `cosmetics` | `/merci` limits, mentoring rewards, cosmetic prices |
| `combat` | PvP tag duration, blocked commands |
| `chat` | Blocked private-message commands |
| `anti-alt` | Detection of accounts sharing a connection |
| `web` | Website: port, displayed name, visible information, anti-flood, elections |
| `life` | Natural seasons, disguise, call for help |
| `tutorial` | Tutorial on/off |
| `admin` | Operators and cooldowns |

### The website (server side)

- Enabled by default on **port 8080** (`web.port`). On Linux, a port below 1024 needs root or `setcap 'cap_net_bind_service=+ep' $(readlink -f $(which java))`; otherwise use 8080. If the port is refused, the plugin still starts and reports it in the console.
- Remember to **open the port** on your router and firewall.
- Read-only, with no coordinates. Nexus HP is hidden by default (`web.show-nexus-health`), as is the number of defenders during a raid (`web.show-raid-defenders`). Banks and balances can be hidden (`web.show-guild-banks`, `web.show-balances`). Vanished players don't appear.
- Anti-flood: 60 requests per 10 seconds per IP address (`web.rate-limit`). Behind a reverse proxy, enable `web.trust-proxy-header` and raise this limit.
- Elections can be disabled or their categories renamed (`web.votes.*`).

### Operators and cooldowns

**Operators** (`op`) are not subject to **any cooldown**: disguise (30 min), call for help (10 min), a guild's post-raid protection, Nexus healing (30 s and the block after an attack), profession change (24 h), specialization change (7 days), train line search, and the PvP bounty anti-farm delay. Setting: `admin.ops-bypass-cooldowns` (`true` by default).

### Built-in anti-cheat

- Mining bonuses never apply to a block placed by a player, or on another guild's land.
- Age requirements are based on the highest member count reached: kicking players before donating doesn't help.
- Enchantments above their normal maximum are removed outside the Arcane Conclave.
- Horde size is capped, the bank is only charged once the target is confirmed, and raid monsters are removed on restart.
- Money writes are serialized (nothing lost between a sale and a disconnection).
- Multi-account detection (mentoring, `/merci`, bets, PvP statistics): `anti-alt.*`.

</details>
