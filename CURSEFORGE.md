# Off to Market — Trading Deluxe

> *"May your coffers overflow and your trade routes prosper!"*

> ⚠️ **ALPHA — Work in Progress**
> This mod is in early alpha. Features are incomplete, bugs are expected, and things might be weird (but should not break). Thank you for your patience as development continues!

**Off to Market** transforms Minecraft into a full merchant experience. Build trading posts, establish routes with unique towns, manage your reputation, hire workers, and grow your trading empire — all with a dynamic economy that reacts to supply & demand.

Whether you're running a solo merchant playthrough or loading a 300+ mod modpack, Off to Market fits right in — automatically discovering items from other mods and generating themed markets around them.

---

## Currency System

Forget emeralds. Off to Market introduces a three-tier coin economy:

| Coin | Value |
|------|-------|
| **Copper Coin** | 1 CP — base unit |
| **Silver Coin** | 10 CP |
| **Gold Coin** | 100 CP |
| **Coin Bag** | Portable storage for your coins |

A **Coin Exchange** tab lets you convert between tiers freely. A *Gold Only* mode is also available in the config if you prefer a simpler economy. **IN ALPHA** — subject to change based on player feedback.

---

## Trading Blocks

### Trading Post
Your merchant headquarters. From here you can:
- Browse goods from multiple towns and cities
- Send **shipments** to distant markets via caravan
- Track active shipments through all phases: *In Transit → At Market → Returning*
- Accept **diplomat requests** for premium-priced orders
- Complete **quests** for bonus coins and trader XP
- Hire **workers** to improve profits and reduce travel time
- Monitor your **merchant level** and reputation across towns

### Trading Bin
A wall-mounted storage block for items you want to sell. Set a price per slot and let passing caravans handle the rest.

### Market Board
A wall-mounted board showing:
- Live market prices from nearby towns
- Available town quests
- Your standing and reputation

---

## Towns

Trade with **10+ handcrafted towns**, each with unique needs, surpluses, and specialties. Prices fluctuate dynamically based on what each town needs and has in abundance.

| Town | Type | Specialty |
|------|------|-----------|
| Greenhollow Village | Farming Village | Agricultural products, livestock |
| Irondeep Settlement | Mining Village | Ores, metals, mining supplies |
| Mistwood Village | Forest Village | Lumber, forestry products |
| Stonehaven | Mountain Town | Stone, building materials |
| Silverbrook | River Town | Fish, aquatic goods |
| Goldcrest City | Capital City | Luxury goods, rare items |
| Redforge | Industrial Town | Smithed goods, tools |
| Moonvale | Mystical Town | Enchanting supplies, magic items |
| Sandport | Coastal Town | Trade goods, exotic items |
| Frostholm | Northern Outpost | Cold weather supplies |

---

## Quests

Towns generate trade quests based on their current needs. Head to the **Quests tab** to accept them and deliver on time for bonus rewards and rep.

- **Standard Deliveries** — Bring goods a town is requesting
- **Bulk Orders** — Large quantities for bigger payouts
- **Rush Deliveries** — Time-sensitive for premium bonuses
- **Specialty Requests** — Unique items for rare rewards
- **Charity Missions** — High-reputation quests with modest pay

Up to 5 active quests at a time. Quests refresh each dawn and expire after 2–4 in-game days.

---

## Workers

Hire workers from your Trading Post to improve your operation:

| Worker | Bonus | Hire Cost |
|--------|-------|-----------|
| **Negotiator** | +5–25% sale price | 500 CP |
| **Trading Cart** | –10–40% travel time | 300 CP |

Workers level up through completed trips (max level 10) and display XP progress in the UI.

---

## Diplomats

Send diplomats to towns to **request specific items** at a premium price. Great for sourcing hard-to-find goods on demand.

- Requests go through: *Negotiating → In Transit → Arrived*
- Premium pricing by town tier: Village +50% · Town +65% · City +80%
- Travel time is 1.5× standard (reduced by Trading Cart bonus)

---

## Animal Trading

Trade livestock using **Animal Trade Slips** — redeemable vouchers for live animals. Purchase slips from the specialized **Peenam Animal Market**.

Supported animals: Cows, Pigs, Sheep, Chickens, Horses, Donkeys, Llamas, and more.

---

## Dynamic Mod Compatibility

**Off to Market automatically integrates with your modpack.** When other mods are installed, the system scans them at runtime, categorizes their items (ingots, gems, foods, weapons, armor, magic, etc.), and generates themed towns specifically for those mods.

| Installed Mod | Generated Market |
|---------------|-----------------|
| Farmer's Delight | Harvest Valley Market |
| Alex's Mobs | Wanderer's Outpost |
| Twilight Forest | Twilight Market |
| Blue Skies | Sky Haven Trading Post |
| The Aether | Aether Outpost |
| Ars Nouveau | Arcane Emporium |
| Iron's Spellbooks | Spellforge Market |
| Apotheosis | Enchanter's Bazaar |
| Aquaculture | Fisherman's Wharf |
| MineColonies | Colonial Trading Co. |
| Vinery | Vineyard Market |
| Cataclysm | Dungeon Outpost |
| Simply Swords | Bladeworks Market |
| Supplementaries | Artisan's Market |
| Quark | Oddities Emporium |
| Oh The Biomes You'll Go | Explorer's Trading Post |
| *And more…* | |

> Tested in a **300+ mod** RPG modpack with no conflicts.

---

## Key Items

| Item | Description |
|------|-------------|
| **Trading Post** | Central hub for all trading activities |
| **Trading Bin** | Store items for sale with custom prices |
| **Market Board** | Wall display for prices, quests, and town info |
| **Ledger** | Track your trading history and finances |
| **Guide Book** | In-game documentation and tutorials |
| **Shipment Note** | Proof of sent goods |
| **Animal Trade Slip** | Redeemable voucher for livestock |
| **Coin Bag** | Portable coin storage |

*Crafting recipes viewable in-game with JEI/REI.*

---

## Configuration

Highly configurable via `config/offtomarket-common.toml`. Notable options:

- **Global price multiplier** — Scale all prices up or down
- **Gold Only mode** — Disable silver and copper coins
- **Dynamic towns** — Toggle auto-generated mod-compatible markets
- **Supply & demand** — Tune market dynamics
- **Debug mode** — Instant travel and free purchases for testing

---

## Getting Started

1. Craft a **Trading Post** — your gateway to commerce
2. Craft a **Trading Bin** — stock items you want to sell
3. Earn some coins via initial quests or selling goods
4. Build **reputation** to unlock more towns and better prices
5. Hire **workers**, send **diplomats**, and grow your empire!

---

## Requirements

- **Minecraft:** 1.19.2
- **Mod Loader:** Forge 43.x+
- **Optional:** JEI/REI (for viewing recipes)

---

## Roadmap

This mod is still in active alpha development. Below is a look at what's planned across short, medium, and long term. Priority and timing may shift based on feedback — your comments genuinely influence what gets built next!

> 💡 Have an idea not listed here? Drop it in the comments — great suggestions get added to the list!

---

### 🔧 Near-Term (Alpha Stabilization)

These are the immediate priorities before moving to a beta release.

- **Economy balance pass** — Pricing, quest rewards, worker costs, and diplomat premiums will all get tuned based on real player feedback. Numbers are placeholder-ish right now.
- **Shipment tracking edge cases** — Some edge cases exist where shipments can get stuck or not return properly. These need to be hunted down and squashed.
- **Quest expiry bugs** — Quests occasionally don't expire or refresh correctly at dawn. A full audit of the quest lifecycle is planned.
- **Trading Bin UX** — The current click-to-select price system works but is clunky. Planning a cleaner approach — possibly inline editing or a dedicated price input screen.
- **Guide Book rewrite** — The in-game Guide Book is outdated and doesn't cover all the newer systems (workers, diplomats, animals). A full rewrite is planned with proper diagrams and examples.
- **JEI/REI integration** — Show proper crafting recipes, coin exchange rates, and item categories in JEI/REI overlays.
- **Config validation** — Better error messages when config values are out of range or misconfigured.
- **Crash hardening** — More null checks and graceful fallbacks to prevent crashes in edge cases or unusual modpack environments.

---

### 🏘️ Towns & Economy

The heart of the mod — expect a lot of growth here.

- **Datapack-defined towns** — Create your own towns entirely through datapacks. Define the name, type, specialty items, lore, reputation thresholds, and more — no Java required.
- **Town reputation tiers** — Reputation currently affects prices, but the plan is full tiers (Stranger → Acquaintance → Trusted → Partner → Honored) each unlocking perks: exclusive items, lower fees, access to rare quests, and special diplomat options.
- **Town events** — Random and triggered events that shake up the market: a surplus of iron after a mining boom, a food shortage after a bad harvest, a festival driving up luxury goods prices for a few days.
- **Seasonal market cycles** — Prices and available goods shift by season. Frostholm stocks more firewood in winter. Silverbrook's fish supply peaks in spring. Makes trade feel alive.
- **Town backstory & lore** — Each handcrafted town will get a brief history, notable NPC mentions, and flavor text visible in the trading post UI.
- **More handcrafted towns** — Aiming for 20+ unique towns total, including desert bazaars, underground trading posts, and island markets.
- **Town growth system** — Towns can grow over time if you trade with them consistently. A village becomes a town, a town becomes a city — with new goods and better prices as they grow.
- **Rival merchants** — NPCs that compete with you for good deals, potentially undercutting prices or locking up quests you wanted.
- **Expanded mod-compatible town generation** — More mod themes supported, with richer item selection and better category detection.

---

### 🚢 Trading & Logistics

Making the actual act of trading feel more tangible and exciting.

- **Caravans as world entities** — Instead of invisible shipments, actual caravan entities will travel across the world. You'll be able to see and follow them.
- **Caravan encounters** — Caravans face random events on the road: bandit ambushes (lose a portion of goods), helpful travelers (small bonus), or weather delays. A Caravan Guard worker reduces risk.
- **Trade routes** — Establish permanent routes between your base and a town. Invest resources to upgrade roads for faster travel and less encounter risk.
- **Shipment insurance** — Purchase insurance for high-value shipments. Pay a premium upfront; if the shipment is lost or ambushed, you're reimbursed.
- **Multi-stop routes** — Chain multiple towns into a single caravan run. Higher risk, higher reward.
- **Trade contracts** — Lock in a standing order with a town: deliver X items every Y days in exchange for a guaranteed price and rep bonus.
- **Black market** — High-risk, high-reward trades with shady contacts. Better prices, but chance of confiscation or reputation hit with legitimate towns.

---

### 👷 Workers & Progression

The workforce system has a lot of room to grow.

- **New worker types:**
  - *Accountant* — Reduces the per-trip tax/fee on sales
  - *Scout* — Discovers new towns and reveals town inventory before committing to a shipment
  - *Caravan Guard* — Reduces encounter risk and loss from ambushes
  - *Appraiser* — More accurately values rare and modded items
  - *Courier* — Speeds up small, single-item diplomat requests
- **Worker traits** — Each hired worker has a randomly assigned trait (e.g., "Experienced Traveler" for faster trips, "Haggler" for better prices, "Clumsy" for occasional mishaps). Adds personality and replayability.
- **Worker fatigue** — Workers need rest between trips. Push them too hard and their performance degrades.
- **Merchant guild system** — Join a guild (Traders' Union, Merchants' Circle, Free Traders, etc.) each offering different passive bonuses, exclusive workers, and faction-specific quests. Guilds have opposing interests, so you can't join them all.
- **Merchant leveling expansion** — More levels, meaningful milestone rewards (permanent price bonuses, new town unlocks, exclusive worker hires), and a visible merchant title displayed in the UI.
- **Worker housing block** — A new block where assigned workers "live," providing small passive bonuses even when not on a trip.

---

### 📜 Quests & Diplomacy

More ways to interact with the towns you trade with.

- **Escort quests** — Accompany a merchant NPC from one town to another. Combat encounter along the way; success earns big rep and coin.
- **Timed collection quests** — Gather a specific set of items within a Minecraft day or two. Fail the timer and lose the quest.
- **Town construction quests** — Donate resources to help a town build something (a new market stall, a storage warehouse). Permanent bonus once complete.
- **Reputation recovery quests** — Made a town unhappy? Special quests to earn back their trust.
- **Chained quest lines** — Story-driven sequences across multiple towns with a narrative payoff and a unique reward at the end.
- **Diplomat skill tree** — Diplomats level up with use and can specialize: better prices, access to restricted goods, or faster negotiation times.

---

### 🤝 Multiplayer

The full multiplayer experience is a longer-term goal but absolutely planned.

- **Shared marketplace block** — A new block where players on the same server can post items for sale and browse each other's listings. Think a player-driven auction house.
- **Player-to-player shipments** — Send items directly to another player's Trading Post via caravan (with transit time).
- **Shared town reputation** — Option to pool reputation per server or keep it per-player (configurable by server admin).
- **Trading guilds for players** — Groups of players can form a guild, pooling reputation and splitting quest rewards.
- **Server-wide economy events** — Events that affect all players: a global surplus crashing a commodity's price, or a rare demand spike making one item worth a fortune for a limited time.

---

### 📦 Content & Polish

Filling in the experience and making everything feel complete.

- **Sound design** — Market ambient sounds, coin jingle on sale, caravan departure/arrival sounds, quest complete fanfare.
- **Ledger overhaul** — Charts for profit/loss over time, per-town breakdown, best-selling items, total coins earned lifetime.
- **Expanded animal trading** — More species, baby animal vouchers, exotic/mod-added animals, and a dedicated animal market UI.
- **Collectible currency** — Rare commemorative coins, antique gold pieces, and high-value trade artifacts that serve as both currency and collectibles.
- **Market decorations** — New decorative blocks themed around trading: market stall canopies, hanging lanterns, coin display cases, barrel carts.
- **Achievements/advancements** — A full advancement tree: first sale, first quest, first diplomat, first level-up, and lore-flavored milestone advances.
- **Villager integration** — Optional bridge between Off to Market towns and vanilla villager trading. Reputation with a town could give discounts with nearby vanilla villagers.
- **Visual caravan customization** — Name your caravans, choose their banner colors, unlock cosmetic upgrades as you level up.

---

## 💬 Feedback & Bug Reports

This mod is actively being developed and your feedback genuinely shapes it! If you run into a bug, have a suggestion, or just want to share your experience:

- **Drop a comment** on this page — all feedback is read!
- **Report bugs** in the comments with your Forge version, a description of what happened, and any crash report if applicable
- **Suggest features** — town ideas, new trade mechanics, mod integrations, you name it

The more feedback received, the better this mod gets. Thanks for trying it out during alpha! 🙏

---

**Author:** Scotty Venable
**License:** All Rights Reserved © 2024–2026
