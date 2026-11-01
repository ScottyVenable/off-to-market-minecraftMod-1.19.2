# Changelog

All notable changes to **Off to Market - Trading Deluxe** are documented in this file.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.0.0/),
and this project adheres to [Semantic Versioning](https://semver.org/spec/v2.0.0.html).

---

## [0.6.7] - 2026-11-01

### Fixed
- **[BUG] Cart Checkout Retention on Failure**:
  - Fixed a critical bug where attempting to checkout a cart on the Market Board without sufficient coins wiped out the cart completely.
  - Added server-to-client confirmation packet (`CartCheckoutResultPacket`). The client now retains the cart with all items, quantities, and prices completely intact whenever a purchase fails or cannot be made.
  - Added client-side coin balance pre-check with audio feedback (`VILLAGER_NO`) and red action bar notice when funds are lacking.
  - Added dynamic button labels and affordability state (`"Checkout"`, `"Can't Afford"`, `"Checking out..."`).
- **[BUG] Trade Manifest Quantity Input Field**:
  - Fixed a bug where players were unable to type custom numeric values into the Qty field in the Trading Ledger when creating a Trade Manifest.
  - Implemented `charTyped()` delegation in `TradingLedgerScreen` to ensure character strokes are properly delivered to focused text boxes.
  - Enhanced mouse click detection to reliably focus and activate the Qty input.
  - Widened the Qty input box from 26px to 34px so 4-digit numbers (up to 9999) fit without clipping.

### Changed & Improved
- **[ECONOMY] Item Valuation & Dynamic Recipe Cost Scaling**:
  - Added `Items.BAMBOO` to `MaterialValues` (2 CP) and `ITEM_OVERRIDES` (`TIER_JUNK`), correcting its raw material pricing (previously sold for 6 CP).
  - Enforced economic minimum floors for all weapons and tools:
    - Swords: minimum 18 CP base fair value (preventing weapons from selling for 4 CP).
    - Pickaxes / Axes: minimum 15 CP.
    - Shovels / Hoes: minimum 12 CP.
    - Tier floors enforced: Stone (24 CP), Iron (65 CP), Gold (90 CP), Diamond (220 CP), Netherite (1200 CP).
  - Enforced armor piece minimum floors (Chest: 32 CP, Legs: 28 CP, Helm: 22 CP, Boots: 16 CP; scaled with tier).
  - Implemented **Dynamic Recipe Cost Floor**: Integrated with Forge `RecipeManager` to calculate ingredient cost sums. A crafted item's base fair market value will never sell below the sum of its raw ingredient base costs + a 20% crafting labor margin.
  - Enhanced modded tier evaluation to account for harvest level, durability, attack damage bonuses, and dimensional materials.

---

## [0.6.6] - 2026-11-01

### Added
- **Town Action Bar**: Added 3-button interactive bar in Trading Post: `[ Contact ]`, `[ Details ]`, and `[ Trade ]`.
- **Town Dossier Modal**: Comprehensive full-screen deep dive displaying distance, caravan travel time, standing level, tariff perks, lore, and live supply/demand.
- **Courier Correspondence**: Send courier inquiries and diplomatic correspondence directly to towns.
- **Documentation**: Comprehensive `README.md` revamp with architecture, recipes, and feature guides.

---

## [0.6.5] - 2026-11-01

### Added
- **Quick Quota Buttons**: Added `[+1]`, `[+16]`, `[+64]`, and `[Max]` buttons to Trading Ledger.
- **In-World Depot Billboards**: Floating 3D billboard badges (`[ 🚩 <Town> Depot ]`) above assigned depot containers.
- **Interactive Deal Sorting**: Added `SortMode.DEAL` to the Market Board.

### Fixed
- Rebalanced Market Board columns to fix severe Deal tab text crunching and clipping.
- Fixed Town detail panel divider clipping through `Neutral` standing badge.
- Replaced surrogate emojis with standard Unicode glyphs to eliminate tofu boxes in vanilla font.
- Adjusted description mud text color to warm parchment cream (`0xCCBBAA`).

---

## [0.6.4] - 2026-10-31

### Added
- **Custom Trade Manifests**: Create custom trade orders with specific goods, town targets, and quantities.
- **Depot Container Tool**: Link touching containers and assign dedicated town depots.
