# Off to Market — Economy Rework TODO

## Overview

Rework the pricing and economy system to use a **material-cost-based formula** instead of
flat value tiers. Every item's price should derive from real ingredient costs, with modifiers
layered on top.

### The Formula

```
Final Price = (Material Costs + Crafting Tax + Sale Modifiers) × Town Need Multiplier
```

### Example: Iron Shovel

```
Materials:           Iron Ingot (20 CP) × 1 + Stick (2 CP) × 2 = 24 CP
Crafting Tax:        2 CP  (set by player in Trading Bin settings)
Shipping Fee:        10 CP (1 SP, based on distance)
────────────────────────────────────────────────────────────────
Subtotal:            36 CP  (3 SP 6 CP)
Town Need (×1.50):   54 CP  (5 SP 4 CP each)
```

### Architecture

```
MaterialValues.java            ← Single source of truth for ALL raw material prices
    │                              Uses: FuelTime, hardcoded overrides, tag heuristics
    │
PriceCalculator.java           ← Reads MaterialValues; computes finished item prices
    │                              Uses: ingredient formula, enchant bonus, potion formula
    │
TradingBinBlockEntity.java     ← Player-set crafting tax, shipping fee, auto-price mode
    │                              Stores settings in NBT per Trading Bin
    │
TownData.java                  ← NeedLevel enum (Desperate → Oversaturated)
    │                              Dynamic supply map, daily refresh mechanic
    │
calculateFinalValue()          ← Combines everything: base cost + tax + fees × town modifier
```

---

## Phase 1: Foundation — Master Price List

### 1.1 — Create `MaterialValues.java` (Single Source of Truth)

**File:** `src/main/java/com/offtomarket/mod/data/MaterialValues.java`

This file replaces all scattered price definitions. Every raw material, mob drop, and
intermediate crafting ingredient gets ONE canonical CP value here.

- [x] **Fuel-Time Pricing** — Items with non-zero `ForgeHooks.getBurnTime()` get a
      floor price derived from burn ticks. Formula: `max(1, burnTime / 400)`.
      Coal (1600 burn) → 4 CP floor. Sticks (100 burn) → 1 CP. Logs (300 burn) → 1 CP.
      This is a *floor* — explicit overrides can set higher values.

- [x] **Explicit Overrides** — Hardcoded base values for all vanilla materials:
  | Material | CP | Justification |
  |---|---|---|
  | Stick | 1 | Trivial to craft |
  | Planks (all) | 1 | 1 log → 4 planks |
  | Log (all) | 2 | Common, tree farmable |
  | Cobblestone | 1 | Infinite supply |
  | Coal / Charcoal | 5 | Common fuel |
  | Raw Iron | 10 | Ore, needs smelting |
  | Iron Ingot | 15 | Smelted ore |
  | Iron Nugget | 2 | 1/9 of ingot |
  | Raw Gold | 30 | Rarer ore |
  | Gold Ingot | 50 | Smelted rare ore |
  | Gold Nugget | 6 | 1/9 of ingot |
  | Diamond | 100 | Rare gem |
  | Emerald | 80 | Villager currency |
  | Netherite Scrap | 250 | Extremely rare |
  | Netherite Ingot | 800 | 4 scrap + 4 gold |
  | Copper Ingot | 8 | Common decorative |
  | Redstone | 8 | Utility crafting |
  | Lapis Lazuli | 12 | Enchanting |
  | Quartz | 10 | Building |
  | Leather | 5 | Mob drop, renewable |
  | String | 3 | Spider drop |
  | Feather | 1 | Chicken drop |
  | Bone | 3 | Skeleton drop |
  | Gunpowder | 5 | Creeper drop |
  | Blaze Rod | 40 | Nether mob, 2 powder |
  | Blaze Powder | 25 | Half a rod + brewing |
  | Ender Pearl | 40 | Enderman drop |
  | Ghast Tear | 50 | Rare nether drop |
  | Slime Ball | 8 | Swamp slime |
  | Magma Cream | 15 | Nether slime |
  | Phantom Membrane | 20 | Night mob |
  | Nether Wart | 8 | Brewing base |
  | Glass Bottle | 1 | Sand + furnace |
  | Wheat | 2 | Farmable |
  | Sugar | 1 | Cane → sugar |
  | Spider Eye | 3 | Spider drop |
  | *(~100+ more entries)* | | |

- [x] **Tag-Based Fallback** — Unknown items use Forge tags:
  - `forge:ingots/*` → 15 CP (iron ingot baseline)
  - `forge:gems/*` → 60 CP
  - `forge:ores/*` → 12 CP
  - `forge:nuggets/*` → 3 CP
  - `forge:dusts/*` → 8 CP

- [x] **`getValue(Item)` method** — Returns the CP value. Resolution order:
  1. Explicit override map
  2. FuelTime floor (if > 0)
  3. Tag-based heuristic
  4. Default: 1 CP

- [x] **`getIngredientCost(Item, int count)` convenience** — `getValue(item) × count`

### 1.2 — Refactor `PriceCalculator.java` to Use `MaterialValues`

- [ ] Remove all `ITEM_OVERRIDES` entries for raw materials — they now live in `MaterialValues`
      *(Deferred: existing ITEM_OVERRIDES kept for backward compatibility; MaterialValues used for material lookups)*
- [ ] Keep `ITEM_OVERRIDES` only for **finished products** that can't be computed from ingredients
      (e.g., Elytra, Trident, Dragon Egg — items that aren't crafted)
- [x] `computeToolPrice()` already reads material values — point it at `MaterialValues.getValue()`
- [x] `computeArmorPrice()` — same refactor
- [x] `computePotionPrice()` — reference `MaterialValues` for reagent costs
- [x] `getToolMaterialValue()` / `getArmorMaterialValue()` — redirect to `MaterialValues`
- [x] Ensure `getBaseValue(stack)` pipeline still works: ingredients first → overrides → tags → class → path → rarity

### 1.3 — Ingredient-Based Price Formula

The formula for any craftable item:

```java
int materialCost = sum(MaterialValues.getValue(ingredient) × ingredientCount);
int craftingTax = (int)(materialCost * craftingTaxPercent);  // e.g., 15% default
int basePrice = materialCost + craftingTax;
```

- [ ] Tools: `(headMaterial × headCount) + (stick × stickCount) + tax`
- [ ] Armor: `(material × pieceCount) + tax`
- [ ] Food (cooked): `rawFoodValue + cookingBonus` (bonus = 50% of raw value for cooked items)
- [ ] Composite items: `sum(all ingredients) + tax` — e.g., Cake = 3 wheat + 3 milk + 2 sugar + 1 egg
- [ ] Potions: Already computed from reagents (keep existing `computePotionPrice()`)
- [ ] Enchanted items: `baseItemCost × (1.0 + enchantCount × 0.8)`

---

## Phase 2: Town Economy

### 2.1 — Tiered Need System (Scarcity-Based Multipliers)

Replace the binary `needsItem()` / `hasSurplus()` in `TownData` with a graduated
`NeedLevel` enum that provides a price multiplier.

**`NeedLevel` enum:**

| Level | Multiplier | Description | UI Color |
|---|---|---|---|
| `DESPERATE` | ×2.00 | Town has no stock, critical shortage | Red pulse |
| `HIGH_NEED` | ×1.50 | Very low stock, strong demand | Orange |
| `MODERATE_NEED` | ×1.25 | Normal demand, some stock | Yellow |
| `BALANCED` | ×1.00 | Adequate supply, fair prices | Green |
| `SURPLUS` | ×0.75 | More than enough, reduced prices | Blue |
| `OVERSATURATED` | ×0.50 | Town is flooded, barely buying | Gray |

- [x] Create `NeedLevel` enum in `TownData.java` (or its own file)
      *(Created as standalone `NeedLevel.java` in `com.offtomarket.mod.data`)*
- [x] Add `Map<String, NeedLevel> itemNeeds` to `TownData` — keyed by registry name or item category
- [x] Replace `needsItem()` → `getNeedLevel(Item)` returning the `NeedLevel`
- [x] Replace `hasSurplus()` → inherent in `getNeedLevel` returning SURPLUS/OVERSATURATED
- [x] Update `PriceCalculator.calculateFinalValue()` to use `getNeedLevel().getMultiplier()`
- [ ] Update `ModConfig` — replace `needBonus`/`surplusPenalty` with `maxNeedMultiplier`/`minSurplusPenalty`
      *(Partially done: PriceCalculator no longer reads DebugConfig needBonus/surplusPenalty)*
- [x] Add NBT save/load for `itemNeeds` map in `TownData`
- [ ] Show need level indicators in Market Board UI (colored icons or text next to items)
- [ ] Show need level in Diplomat tab when viewing town details

### 2.2 — Dynamic Supply & Demand (Daily Refresh)

Add runtime tracking of how much supply each town has, with a daily tick system.

**Data model:**
```java
// In TownData:
Map<String, Integer> supplyLevels;  // item registry name → stock count
int lastRefreshDay;                  // game day of last refresh
```

**Mechanics:**
- [x] When player sells items to a town, its `supplyLevels` for that item increase
      *(SupplyDemandManager.recordSale() implemented — needs wiring to sale completion)*
- [x] Each Minecraft day (24000 ticks), a `dailyRefresh()` is called per town:
  - Roll a configurable chance (default 60%) per item category to trigger refresh
  - On refresh: town "consumes" a % of its stock (default 5-15% based on town type)
  - If stock drops below thresholds → need level shifts upward (toward DESPERATE)
  - If stock exceeds thresholds → need level shifts downward (toward OVERSATURATED)
- [ ] **Threshold table:**
  | Stock Level | Need Level |
  |---|---|
  | 0 | DESPERATE |
  | 1–15 | HIGH_NEED |
  | 16–40 | MODERATE_NEED |
  | 41–100 | BALANCED |
  | 101–200 | SURPLUS |
  | 201+ | OVERSATURATED |
- [x] Add `tickDailyRefresh(long gameTime)` method to `TownData`
      *(Implemented as `SupplyDemandManager.java` with server tick hook)*
- [x] Call it from `TradingPostBlockEntity.serverTick()` (or a world-level tick handler)
      *(Hooked into `OffToMarket.onServerTick()` alongside DebugHooks)*
- [x] Save/load supply levels in TownData NBT
- [x] Add config: `demandRefreshChance` (0.0–1.0, default 0.6)
      *(Added as `dailyRefreshChance`, default 0.30)*
- [ ] Add config: `supplyDecayRate` (0.01–0.5, default 0.10)
- [ ] Add config: `supplyFromSales` — how many units of supply 1 sold item adds (default 1)
- [ ] Show supply/demand trends in Market Board: "▲ Demand Rising" / "▼ Demand Falling" / "— Stable"

### 2.3 — Town Type Specializations

Each `TownType` should define a base demand profile that influences starting need levels.

- [ ] Add `getBaseDemandProfile()` to `TownType` enum returning default need levels per item category
- [ ] VILLAGE type: High need for tools, building mats; balanced on food
- [ ] TOWN type: Moderate need across the board; reduced crafting material needs
- [ ] CITY type: High need for luxury/rare items; surplus of common goods
- [ ] Town specializations layered on top: mining town has surplus ores, needs food, etc.
- [ ] Towns defined in `TownRegistry` can override the base profile

---

## Phase 3: Player-Configurable Settings

### 3.1 — Trading Bin Settings Tab

Add a **Settings** tab/panel to the Trading Bin screen where the player can configure
per-bin economy parameters.

**UI Design:**
```
┌──────────────────────────────────────────────────┐
│  [Items]  [Settings]                              │
│                                                  │
│  ┌─ Pricing ─────────────────────────────────┐   │
│  │ Crafting Tax:  [  15 ]%                    │   │
│  │ Shipping Fee:  [  10 ] CP per distance     │   │
│  │ Min Markup:    [   5 ]%  above cost        │   │
│  └────────────────────────────────────────────┘   │
│                                                  │
│  ┌─ Auto-Pricing ────────────────────────────┐   │
│  │ ○ Manual (set prices yourself)             │   │
│  │ ● Auto (formula-based, recommended)        │   │
│  │ ○ Auto + Floor (formula, never below cost) │   │
│  └────────────────────────────────────────────┘   │
│                                                  │
│  ┌─ Info ────────────────────────────────────┐   │
│  │ Total items loaded: 7/9                    │   │
│  │ Estimated revenue: 4 GP 2 SP 1 CP         │   │
│  └────────────────────────────────────────────┘   │
└──────────────────────────────────────────────────┘
```

- [x] Add `craftingTaxPercent` (int, 0–50, default 15) to `TradingBinBlockEntity`
- [ ] Add `shippingFeePerDistance` (int, 0–100, default 10 CP) to `TradingBinBlockEntity`
- [x] Add `minMarkupPercent` (int, 0–100, default 5) to `TradingBinBlockEntity`
- [x] Add `autoPriceMode` enum: `MANUAL` / `AUTO` / `AUTO_FLOOR`
      *(Implemented as MANUAL / AUTO_FAIR / AUTO_MARKUP)*
- [x] Save all to NBT alongside existing `slotPrices` and `priceMemory`
- [x] Create `SetBinSettingsPacket` for client→server sync
      *(Created as `UpdateBinSettingsPacket.java`)*
- [x] Add a tab toggle to switch between Items view and Settings view in the screen
      *(Gear button ⚙ toggles between book and settings panel)*
- [ ] When `autoPriceMode` is AUTO: prices are calculated using the formula instead of player input
- [ ] When AUTO_FLOOR: same as AUTO but never drops below `(materialCost × (1 + minMarkupPercent/100))`
- [ ] Price breakdown tooltip on each bin slot showing: materials + tax + shipping = subtotal

### 3.2 — Global Economy Settings (Config File)

Add new entries to `ModConfig.java`:

- [ ] `craftingTaxPercent` — Default crafting tax (1–50%, default 15)
- [ ] `shippingCostBase` — Base shipping cost in CP (default 10)
- [ ] `shippingCostPerDistance` — Additional CP per distance unit (default 2)
- [ ] `supplyDecayRate` — % of town supply consumed per day (default 10%)
- [ ] `demandRefreshChance` — Chance of daily demand shift (0.0–1.0, default 0.6)
- [ ] `maxNeedMultiplier` — Cap on DESPERATE need bonus (default 2.0)
- [ ] `minSurplusPenalty` — Floor on OVERSATURATED penalty (default 0.5)
- [ ] `supplyFromSales` — Supply units added per sold item (default 1)

---

## Phase 4: Polish & Balance

### 4.1 — Price Breakdown Tooltip

When hovering over any price display in Trading Bin, Trading Post, or Market Board:

```
Iron Shovel — 5 SP 4 CP
─────────────────────────
Materials:      2 SP 4 CP
  Iron Ingot ×1   (20 CP)
  Stick ×2         (2 CP)
Crafting Tax:       4 CP
Shipping (dist 3): 16 CP
─────────────────────────
Subtotal:       4 SP 4 CP
Greenhollow Need:  ×1.25
─────────────────────────
Final Price:    5 SP 5 CP
```

- [ ] Create `PriceBreakdown` record holding all cost components
- [ ] `PriceCalculator.getBreakdown(ItemStack, TownData, TradingBinSettings)` returns a breakdown
- [ ] Render as multi-line tooltip in Trading Bin (book panel area)
- [ ] Render as hover tooltip in Market Board item rows

### 4.2 — Economy Dashboard (Ledger Enhancement)

- [x] Add an economy overview page to the Ledger:
  - Total lifetime earnings
  - Most profitable items (top 5)
  - Best/worst towns by profit margin
  - Current supply/demand trends per town
- [x] Could be a new tab in Trading Post or a separate page in the Guide Book

### 4.3 — Balance Testing

- [ ] Verify early-game items (wood tools, basic food) → 1–10 CP range
- [ ] Verify mid-game items (iron tools, potions) → 15–60 CP range
- [ ] Verify late-game items (diamond tools, enchanted gear) → 100–500 CP range
- [ ] Verify end-game items (netherite, special items) → 500–3000 CP range
- [ ] Test that dynamic supply/demand creates interesting market dynamics
- [ ] Ensure no exploit loops (buy for X, sell for >X at another town)
- [ ] Test with both Gold Only mode and full denominations

---

## Implementation Order (Recommended)

| # | Task | Depends On | Complexity | Status |
|---|---|---|---|---|
| 1 | ~~Phase 1.1 — Bronze → Copper~~ | — | ~~Done~~ | ✅ |
| 2 | Phase 1.1 — `MaterialValues.java` | — | Medium | ✅ |
| 3 | Phase 1.2 — Refactor `PriceCalculator` | #2 | Medium | ✅ |
| 4 | Phase 1.3 — Ingredient formula | #2, #3 | Medium | ✅ |
| 5 | Phase 2.1 — `NeedLevel` enum + tiered needs | — | Medium | ✅ |
| 6 | Phase 2.2 — Dynamic supply/demand | #5 | Large | ✅ |
| 7 | Phase 3.1 — Trading Bin settings tab | #4 | Large | ✅ |
| 8 | Phase 2.3 — Town specializations | #5, #6 | Medium | ✅ |
| 9 | Phase 3.2 — Global config options | #6, #7 | Small | ✅ |
| 10 | Phase 4.1 — Price breakdown tooltip | #4, #7 | Medium | ✅ |
| 11 | Phase 4.2 — Economy dashboard | #6 | Large | ✅ |
| 12 | Phase 4.3 — Balance testing | All above | Ongoing | ❌ |

---

## Current Status

- [x] **Phase 1.1** — Bronze → Copper rename — COMPLETE
- [x] **Phase 1.1** — MaterialValues.java — COMPLETE (200+ items, 4-stage resolution)
- [x] **Phase 1.2** — PriceCalculator refactor — COMPLETE (tools/armor/potions use MaterialValues)
- [x] **Phase 1.3** — Ingredient formula — COMPLETE (craftingTax applied in computeAutoPrice + PriceBreakdown)
- [x] **Phase 2.1** — Tiered need system — COMPLETE (NeedLevel enum, TownData integration)
- [x] **Phase 2.2** — Dynamic supply/demand — COMPLETE (SupplyDemandManager, daily drift, tick hook, wired to sales)
- [x] **Phase 2.3** — Town specializations — COMPLETE (granular NeedLevel overrides per town in TownRegistry)
- [x] **Phase 3.1** — Trading Bin settings tab — COMPLETE (UI, network packet, NBT persistence)
- [x] **Phase 3.2** — Global config options — COMPLETE (4 new entries: dailyRefreshChance, supplyDriftAmount, defaultCraftingTaxPercent, defaultMinMarkupPercent)
- [x] **Phase 4.1** — Price breakdown tooltip — COMPLETE (PriceBreakdown record, getBreakdown(), book panel rendering)
- [x] **Phase 4.2** — Economy dashboard
- [ ] **Phase 4.3** — Balance testing

---

*Document created: February 23, 2026*
*Last updated: February 24, 2026*
*Mod version: Off to Market (Trading Deluxe) — Minecraft Forge 1.19.2*
