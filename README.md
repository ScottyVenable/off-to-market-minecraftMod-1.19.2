# Off to Market - Trading Deluxe

<p align="center">
  <img src="https://img.shields.io/badge/Minecraft-1.19.2-green" alt="Minecraft 1.19.2">
  <img src="https://img.shields.io/badge/Forge-43.5.0-orange" alt="Forge 43.5.0">
  <img src="https://img.shields.io/badge/Version-0.4.2-blue" alt="Version 0.4.2">
  <img src="https://img.shields.io/badge/License-All%20Rights%20Reserved-red" alt="License">
</p>

A comprehensive trading and economy mod for Minecraft that brings a complete merchant experience to your world. Build trading posts, establish trade routes with fictional towns, manage your merchant reputation, and grow your trading empire!

## Features

### Currency System
- **Gold Coins** - High value currency (100 copper value)
- **Silver Coins** - Medium value currency (10 copper value)  
- **Copper Coins** - Base currency unit
- **Coin Bag** - Store and manage your currency in one convenient item

### Trading Blocks

#### Trading Post
The central hub for all your trading activities. Use it to:
- Browse items from multiple towns and cities
- Send shipments to distant markets
- Accept diplomat requests for premium prices
- Complete quests for reputation and rewards
- Track your merchant level progression

#### Trading Bin
Your local storage for items to sell:
- Set prices per item slot
- Items are automatically valued when caravans come
- Quick interface for managing inventory

#### Market Board
Wall-mounted board to view:
- Current market prices
- Available quests
- Town information and your standing

### Town System

Trade with 9 unique towns, each with their own personality:

| Town | Type | Specialty | Min. Level |
|------|------|-----------|------------|
| **Greenhollow Village** | Village | Farming, crops, produce | 1 |
| **Irondeep Settlement** | Village | Ores, metals, mining supplies | 1 |
| **Peenam Animal Market** | Village | Livestock, animal trade slips | 1 |
| **Saltmere Harbor** | Town | Fish, coastal goods | 1 |
| **Timberwatch** | Town | Lumber, wood products | 2 |
| **Crossroads Market** | Market | General trade, diverse goods | 2 |
| **Basaltkeep Fortress** | City | Weapons, armor, construction | 3 |
| **Arcaneveil Spire** | City | Enchanting supplies, magic items | 4 |
| **Goldspire Capital** | City | Luxury goods, rare items, endgame trade | 5 |

### Dynamic Mod Compatibility

**Off to Market automatically integrates with your modpack!**

When other mods are installed, the system:
- **Discovers items** from all loaded mods at runtime
- **Categorizes items** by type (ingots, gems, foods, weapons, armor, magic, etc.)
- **Generates new towns** themed around popular mods

#### Supported Mod Themes

When these mods are detected, specialized markets are created:

| Mod | Generated Town |
|-----|---------------|
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
| *And more...* | |

### Trading Features

- **Supply & Demand Pricing** - Prices fluctuate based on what towns need and have surplus of
- **Merchant Reputation** - Build standing with towns for better deals
- **Quests** - Accept delivery quests for bonus rewards
  - Standard deliveries
  - Bulk orders
  - Rush deliveries (time-sensitive)
  - Specialty requests
  - Charity missions (high rep, lower pay)
- **Diplomat System** - Negotiate premium prices for your goods
- **Shipments** - Send goods to distant markets via caravan

### Animal Trading

Trade livestock with the **Animal Trade Slip** system:
- Purchase slips for various animals from the Peenam Animal Market
- Redeem slips to spawn tamed animals
- Buy and sell: Cows, Pigs, Sheep, Chickens, Horses, Donkeys, Llamas, and more!

### Items

| Item | Description |
|------|-------------|
| **Ledger** | Track your trading history and finances |
| **Shipment Note** | Proof of goods sent to market |
| **Guide Book** | In-game documentation and tutorials |
| **Animal Trade Slip** | Redeemable voucher for livestock |

## Getting Started

1. **Craft a Trading Post** - Your gateway to commerce
2. **Craft a Trading Bin** - Store items you want to sell
3. **Earn some coins** - Complete initial quests or sell goods
4. **Build reputation** - Unlock access to more towns and better prices
5. **Expand your empire** - Set up trade routes and grow wealth!

## Crafting Recipes

*Recipes are viewable in-game with JEI/REI*

### Trading Post
```
[Oak Log] [Oak Log] [Oak Log]
[Oak Log] [Emerald] [Oak Log]
[Oak Log] [Oak Log] [Oak Log]
```

### Trading Bin
```
[Oak Planks] [Oak Planks] [Oak Planks]
[Oak Planks] [Chest]      [Oak Planks]
[Oak Planks] [Oak Planks] [Oak Planks]
```

### Market Board
```
[Oak Sign] [Oak Sign] [Oak Sign]
[Oak Sign] [Paper]    [Oak Sign]
[Oak Sign] [Oak Sign] [Oak Sign]
```

### Ledger
```
[Leather] [Ink Sac] [Leather]
[Ink Sac] [Paper]   [Ink Sac]
[Leather] [Ink Sac] [Leather]
```

### Coin Bag
```
[Leather] [String]  [Leather]
[Leather] [       ] [Leather]
[Leather] [Leather] [Leather]
```

### Guide Book
*Shapeless recipe:*
- Book + Emerald

### Animal Trade Slip
*Shapeless recipe (yields 3):*
- 3× Paper + Ink Sac

## Configuration

The mod is highly configurable through the TOML config file at `config/offtomarket-common.toml`.

### Config Sections

| Section | Description |
|---------|-------------|
| `[timing]` | Control travel times, sale intervals |
| `[pricing]` | Adjust price multipliers, sale chances |
| `[currency]` | Coin exchange rates, gold-only mode |
| `[leveling]` | XP and trader progression settings |
| `[quests]` | Quest count, rewards, expiry |
| `[workers]` | Hire costs, worker bonuses |
| `[diplomats]` | Diplomat premiums and timing |
| `[animals]` | Animal trading settings |
| `[modCompat]` | Dynamic mod integration settings |
| `[supplyDemand]` | Market dynamics |
| `[tradingBin]` | Bin search radius, defaults |
| `[debug]` | Debug mode, instant travel, free purchases |

### Key Settings

```toml
[pricing]
# Global multiplier for all prices (2.0 = everything costs/sells for double)
globalPriceMultiplier = 1.0

[currency]
# Gold Only mode: disables silver and copper coins
goldOnlyMode = false

[modCompat]
# Enable auto-generated towns based on installed mods
enableDynamicTowns = true
# Log discovered mod items to console (for debugging)
logDiscoveredItems = false

[debug]
# Make all shipments arrive instantly (for testing)
instantTravel = false
# All purchases are free (for testing)
freePurchases = false
```

## Compatibility

- **Minecraft**: 1.19.2
- **Forge**: 43.x+
- **Modpack Tested**: Cisco's Fantasy Medieval RPG [Ultimate] (300+ mods)

Works alongside other economy mods and integrates automatically with item-adding mods.

## Installation

1. Install Minecraft Forge 1.19.2 (version 43.x or higher)
2. Download `offtomarket-0.4.2.jar`
3. Place in your `mods` folder
4. Launch the game!

## Roadmap

- [x] Custom town creation — data-driven JSON town system (v0.2)
- [ ] Multiplayer trading between players
- [ ] Trading caravans as entities
- [ ] More mod integrations
- [ ] Seasonal market events

## Credits

**Author**: Scotty Venable

## License

All Rights Reserved © 2024-2026

---

*"May your coffers overflow and your trade routes prosper!"*
