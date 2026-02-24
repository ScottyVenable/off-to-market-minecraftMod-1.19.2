# Off to Market (Trading Deluxe) - Changelog

## Version 0.1.4 — Dynamic Mod Compatibility (Current Release)

### New Features

#### Dynamic Mod Integration
- **Automatic Item Discovery** - Scans all loaded mods at runtime for tradeable items
- **Intelligent Categorization** - Uses Forge Tags API to classify items (ingots, gems, foods, weapons, armor, magic, etc.)
- **Auto-Generated Markets** - New themed towns appear based on installed mods

#### Supported Mod Themes
When these mods are detected, specialized markets are automatically created:
- Farmer's Delight → Harvest Valley Market
- Alex's Mobs → Wanderer's Outpost
- Twilight Forest → Twilight Market
- Blue Skies → Sky Haven Trading Post
- The Aether → Aether Outpost
- Ars Nouveau → Arcane Emporium
- Iron's Spellbooks → Spellforge Market
- Apotheosis → Enchanter's Bazaar
- Aquaculture → Fisherman's Wharf
- MineColonies → Colonial Trading Co.
- Vinery → Vineyard Market
- Cataclysm → Dungeon Outpost
- Simply Swords → Bladeworks Market
- Supplementaries → Artisan's Market
- Quark → Oddities Emporium
- Oh The Biomes You'll Go → Explorer's Trading Post
- And more...

#### Animal Trading System
- **Animal Trade Slip** - New item for trading livestock
- **Peenam Animal Market** - Specialized town for buying animals
- Supports: Cows, Pigs, Sheep, Chickens, Horses, Donkeys, Llamas, and more

#### New Town Types
- **MARKET** - Trade hub with good variety
- **OUTPOST** - Remote trading post

### Bug Fixes
- Trading Bin now uses click-to-select for setting prices
- Yellow highlight shows selected slot in Trading Bin UI
- Price book correctly displays selected item information

### Technical
- Quest/Diplomat reward multipliers adjusted for new town types

---

## Version 0.1.3 — Major Feature Update

### New Features

#### Quests System
- Towns now generate trade quests based on their needs
- Accept quests from the new **Quests tab** in the Trading Post
- Deliver requested items to earn bonus coins and trader XP
- Quests refresh each dawn and expire after 2-4 Minecraft days
- Quest rewards scale by town type (Village 1x, Town 1.3x, City 1.6x)
- Up to 5 active quests at a time

#### Workers System
- Hire workers from the new **Workers tab** in the Trading Post
- **Negotiator**: Negotiates better sale prices (5-25% bonus, scales with level)
  - Hire cost: 500 CP (5 gold)
  - Per-trip cost: 12-30 CP (scales with level)
  - Levels up with each completed trip (max level 10)
- **Trading Cart**: Reduces delivery time for shipments (10-40% reduction, scales with level)
  - Hire cost: 300 CP (3 gold)
  - Per-trip cost: 6-15 CP (scales with level)
  - Levels up with each completed trip (max level 10)
- Workers show level, XP progress bar, bonus stats, and trip count

#### Trade Diplomat System
- Send diplomats to request specific items from towns
- Items must be within the town's surplus or specialty domain
- Diplomat premium pricing by town type (Village +50%, Town +65%, City +80%)
- Diplomat requests go through 3 phases: Negotiating → In Transit → Arrived
- Travel time is 1.5x normal, reduced by Trading Cart bonus
- View and collect requests from the new **Diplomat tab**

### UI Improvements

#### Tab System Expansion
- Trading Post now has 8 tabs: Trade, Ships, Coins, Orders, Towns, Quests, Work, Diplo
- Tabs are 30px wide each, fitting cleanly across the 248px content area
- Each tab has hover tooltips for detailed information

#### Quantity Selector Fix
- Fixed z-order rendering of the quantity selector overlay on the Market Board
- Quantity selector now renders properly above item icons and store content

#### Guidebook Layout Fix
- Guidebook text now renders in a single full-width column instead of two narrow columns
- Text is easier to read with proper width utilization

### Networking
- Added 5 new server-bound packets for quest, worker, and diplomat interactions
- AcceptQuestPacket, DeliverQuestPacket, HireWorkerPacket, SendDiplomatPacket, CollectDiplomatPacket

### Technical
- 3 new data classes: Quest, Worker, DiplomatRequest with full NBT serialization
- TradingPostBlockEntity extended with quest management, worker bonuses, and diplomat request processing
- Server tick processing for diplomat status transitions, quest expiry checks, and dawn-based quest refresh

---

## Version 0.1.2 — Pricing & Economy Update

### Ingredient-Based Pricing Algorithm
- Item prices now calculated based on crafting recipe ingredient costs
- Recursive ingredient resolution with cycle detection
- Fallback tiered pricing for items without recipes
- Price modifiers for specialty items, enchanted items, tools, armor, and potions
- Dynamic pricing influenced by town demand levels

### Market Board Enhancements
- Purchase items from town surplus and specialty catalogs
- Buy order system with tracking and collection
- Scrollable item listings with hover tooltips

### Coin System
- Three-tier currency: Copper (1 CP), Silver (10 CP), Gold (100 CP)
- Coin exchange tab for converting between tiers
- Coin Bag item for portable coin storage

---

## Version 0.1.1 — Initial Release

### Core Features
- Trading Post block for sending shipments to towns
- Multiple town types (Village, Town, City) with unique needs, surplus, and specialties
- Trader leveling system with XP progression
- Shipment tracking with travel time, market time, and return phases
- Dynamic demand tracking affecting sale prices
- Market Board for browsing and purchasing town goods
- Guide Book with mod documentation
- Trading Bin for quick item selling
- Debug configuration system for testing
