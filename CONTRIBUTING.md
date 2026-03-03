# Contributing to Off to Market — Trading Deluxe

Welcome to the **Off to Market** contributor guide! Whether you are joining the development team, designing content, or submitting ideas, this document gives you everything you need to understand what the mod is, how it is built, and how you can contribute.

---

## Table of Contents

1. [What Is Off to Market?](#1-what-is-off-to-market)
2. [Design Vision and Philosophy](#2-design-vision-and-philosophy)
3. [Repository Layout](#3-repository-layout)
4. [Architecture Overview](#4-architecture-overview)
5. [Key Systems Explained](#5-key-systems-explained)
6. [Setting Up Your Development Environment](#6-setting-up-your-development-environment)
7. [How to Contribute](#7-how-to-contribute)
   - [Code Contributions](#71-code-contributions)
   - [Data-Driven Content (Towns and Custom Menus)](#72-data-driven-content-towns-and-custom-menus)
   - [Art and Textures](#73-art-and-textures)
   - [Ideas and Design Feedback](#74-ideas-and-design-feedback)
8. [Coding Conventions](#8-coding-conventions)
9. [Branching and Versioning Strategy](#9-branching-and-versioning-strategy)
10. [Pull Request Process](#10-pull-request-process)

---

## 1. What Is Off to Market?

**Off to Market — Trading Deluxe** is a Minecraft Forge mod for version 1.19.2 that adds a full merchant and trading economy to your world. The core fantasy is: *you are a traveling merchant building a trading empire.*

Players craft Trading Posts, establish trade routes with fictional towns, ship goods to distant markets, manage reputation with those towns, complete delivery quests, and hire specialist workers. The economy uses a three-tier coin currency (Copper, Silver, Gold) and prices fluctuate dynamically based on supply and demand.

The mod is designed to feel immersive and rewarding at every stage of progression — from a humble village trader hawking crops at Level 1 all the way to a high-level merchant negotiating premium deals at Goldspire Capital.

**Key pillars of the experience:**

- Every item in the game (including items from other installed mods) has a sensible price.
- Towns feel alive: they stock goods, run low on supplies, and send letters to the player.
- Progression is meaningful: higher trader levels unlock better towns, workers, and diplomacy.
- The mod should be easy to extend with new towns, menus, and integrations without touching Java code.

---

## 2. Design Vision and Philosophy

- **Data over code** — New towns, custom shop menus, and loot tables are defined in JSON files. Only low-level mechanics require Java.
- **One authoritative owner per piece of state** — The `TradingPostBlockEntity` owns all Trading Post state. Nothing is split across the Block or Menu class.
- **Backward compatibility matters** — Players should never lose items or progress across mod updates. Every data format change must come with a migration path.
- **Economy balance is intentional** — Price multipliers, supply/demand drift, and progression gates exist to prevent exploits. Do not adjust them lightly.
- **Mod compatibility is automatic** — `ModCompatibility` discovers items from other loaded mods at runtime and generates themed towns. New mods should be supported without hardcoding where possible.

---

## 3. Repository Layout

```
off-to-market-minecraftMod-1.19.2/
├── src/
│   └── main/
│       ├── java/com/offtomarket/mod/    ← All Java source code
│       │   ├── block/                  ← Block classes (placement, use(), drops)
│       │   │   └── entity/             ← BlockEntity classes (game logic, NBT)
│       │   ├── client/
│       │   │   └── screen/             ← Client-side GUI screens
│       │   ├── config/                 ← Forge config (ModConfig, DebugConfig)
│       │   ├── content/                ← Data loaders (TownLoader, CustomMenuLoader)
│       │   ├── data/                   ← Pure game-logic data classes
│       │   ├── debug/                  ← /otm debug commands (DebugCommands)
│       │   ├── event/                  ← Forge event handlers
│       │   ├── item/                   ← Item classes
│       │   ├── menu/                   ← Container/Menu classes (slot wiring)
│       │   ├── network/                ← Network packets (client <-> server)
│       │   ├── registry/               ← Deferred register helpers
│       │   └── util/                   ← Shared utilities
│       └── resources/
│           ├── assets/offtomarket/     ← Client assets (textures, models, lang)
│           └── data/offtomarket/       ← Server data (towns, recipes, loot tables, menus)
│               ├── towns/              ← JSON town definitions + _index.json
│               └── custom_menus/       ← JSON custom shop menus + _index.json
├── gradle.properties                   ← mod_version lives here
├── CHANGELOG.md
├── README.md
├── TEXTURE_GUIDE.md
└── TODO.md
```

---

## 4. Architecture Overview

The mod follows the standard Minecraft Forge block/block-entity/menu/screen pattern. The key rule is: **all state lives in the BlockEntity, never in the Block or Menu.**

```
TradingPostBlock          — handles placement, right-click to open GUI
  └─ TradingPostBlockEntity  — ALL logic: shipments, XP, coins, quests,
                               workers, diplomacy, supply/demand, NBT save/load
       └─ TradingPostMenu    — wires inventory slots to the BlockEntity
            └─ TradingPostScreen  — client-side rendering only; sends packets for actions

data/                     — pure logic, no Minecraft lifecycle coupling
  PriceCalculator         — 6-tier pricing pipeline for any ItemStack
  TownRegistry            — static + JSON-loaded town map
  MarketListing           — NPC town Market Board listing generation
  ModCompatibility        — runtime item discovery + dynamic town generation
  NeedLevel               — 6-tier demand enum (DESPERATE → OVERSATURATED)
  SupplyDemandManager     — server-tick demand drift
  TownInventory / TownInventoryManager — persistent per-town stock with dynamic pricing

content/                  — data-driven content loaded from JSON at startup
  TownLoader / TownDefinition    — reads data/offtomarket/towns/*.json
  CustomMenuLoader / CustomMenuRegistry — reads data/offtomarket/custom_menus/*.json

network/                  — Forge SimpleChannel packets
  ModNetwork              — registers all packets
                            client→server: PLAY_TO_SERVER direction
                            server→client: PLAY_TO_CLIENT direction

client/screen/
  OtmGuiTheme             — single source of truth for all UI colors, draw helpers,
                            slot drawing, dividers, and text truncation
```

---

## 5. Key Systems Explained

### 5.1 Economy and Currency

The economy uses three coin denominations stored as item counts in the player's inventory or in a Finance Table:

| Coin | Registry ID | Value |
|------|------------|-------|
| Copper Piece | `offtomarket:copper_coin` | 1 CP |
| Silver Piece | `offtomarket:silver_coin` | 10 CP |
| Gold Piece | `offtomarket:gold_coin` | 100 CP |

All internal calculations use **Copper Pieces (CP)** as the base unit. The `PriceCalculator` returns CP values.

### 5.2 Price Calculator

`PriceCalculator` determines the base value of any `ItemStack` in this fixed order — **do not skip or reorder steps**:

1. **Ingredient-based** — tool/armor tier + repair material cost × crafting premium
2. **Exact item overrides** — hardcoded vanilla specials (e.g., elytra, nether star)
3. **Forge tag rules** — `forge:ingots/iron`, `forge:gems/diamond`, etc. (works for modded items automatically)
4. **`classifyByClass()`** — `instanceof` checks including food via `FoodProperties`
5. **`classifyByPath()`** — registry-name keyword heuristics for remaining modded items
6. **Rarity fallback** — uses item rarity enum as a last resort

Results are **cached** in `BASE_TIER_CACHE` keyed by `Item` type (not stack). Enchanted books bypass the cache and are special-cased before it.

### 5.3 Town System

Towns are defined in JSON files (`data/offtomarket/towns/*.json`) and loaded at startup by `TownLoader`. Each town has:

- A unique `id`, display name, and description
- An abstract `distance` (1–10) used for travel time and price premiums
- A `type` (VILLAGE, TOWN, CITY, MARKET, OUTPOST) affecting price bias
- A `minTraderLevel` that gates access until the player reaches that level
- A `sells` list of item registry IDs
- A `needLevels` map of item → `NeedLevel` demand (see below)
- Optional `letters` that NPCs send to players on events

`TownRegistry` holds both statically registered towns and JSON-loaded ones. JSON entries override same-ID hardcoded entries on load, so data-driven content always wins.

### 5.4 Supply and Demand (NeedLevel)

`NeedLevel` is a 6-tier enum that drives both player-sale multipliers and NPC listing prices:

| Level | Player sale bonus | NPC buy price |
|-------|-----------------|--------------|
| DESPERATE | 1.75x | town will not sell this item |
| HIGH_NEED | 1.40x | town will not sell this item |
| MODERATE_NEED | 1.25x | 1.30x base (25% chance of listing) |
| BALANCED | 1.00x | 1.00x base |
| SURPLUS | 0.80x | 0.85x base |
| OVERSATURATED | 0.60x | 0.70x base |

`SupplyDemandManager` runs on the server tick to drift these values over time, simulating a living market. Never use the old binary `needs`/`surplus` sets — always use `needLevels` map with `NeedLevel` values.

`TownInventory` tracks persistent per-town stock. Prices within town inventory fluctuate further based on purchase frequency (demand surcharge: +3% per buy, capped at 2x) and scarcity (stock below 50% or 25% thresholds).

### 5.5 Quests

Quests are generated per-town and managed inside `TradingPostBlockEntity`. Types include:

- Standard delivery
- Bulk order
- Rush delivery (time-sensitive)
- Specialty request
- Charity mission (high reputation, lower pay)

Completing quests grants XP and coins. Failing (expiring) quests may affect town reputation in future updates.

### 5.6 Workers

Workers are hireable NPCs that provide passive trade bonuses. There are four types:

| Worker | Bonus |
|--------|-------|
| Negotiator | Increases sale prices at markets |
| Trading Cart | Reduces caravan travel time |
| Bookkeeper | Reduces worker operating costs |
| Stock Scout | Scouts towns and returns stock reports |

Worker state (level, XP, hire status, mission) is stored inside `TradingPostBlockEntity`. Hire costs and bonuses are configured via `ModConfig[workers]`.

### 5.7 Networking

All client-to-server actions are sent as packets via `ModNetwork` (a Forge `SimpleChannel`). Every user action (send shipment, accept quest, hire worker, etc.) has its own packet class in `network/`.

Pattern for new packets:
1. Create `network/MyPacket.java` with static `encode`, `decode`, and `handle` methods.
2. Register in `ModNetwork.register()` with direction `PLAY_TO_SERVER` or `PLAY_TO_CLIENT`.
3. Server→Client packets **must** use `DistExecutor.unsafeRunWhenOn(Dist.CLIENT, ...)` inside `handle()`.
4. Use `ctx.get().enqueueWork(() -> ...)` for all world interactions.

### 5.8 UI / Screens

All screens are client-only and extend Minecraft's `AbstractContainerScreen` or `Screen`. They render using `OtmGuiTheme`, the single source of all UI colors, `drawPanel`, `drawInsetPanel`, `drawSlot`, `drawDividerH/V`, and text truncation helpers. Import `OtmGuiTheme` into every new screen — do not hardcode colors inline.

Button construction uses the 1.19.2 API: `new Button(x, y, w, h, label, handler)`. The `Button.builder(...).build()` API does not exist on this version.

### 5.9 Configuration

All runtime configuration lives in `config/offtomarket-common.toml`, loaded by `ModConfig`. Always read values via `DebugConfig.get*()` methods at runtime, not `ModConfig.*` directly. `DebugConfig` wraps `ModConfig` and allows runtime overrides for testing (e.g., `instantTravel`, `freePurchases`).

### 5.10 Mod Compatibility

`ModCompatibility` runs at startup, discovers item registries from all loaded mods, and categorizes items by type. When recognized mods are present, it generates themed towns (e.g., Farmer's Delight → Harvest Valley Market). This all happens automatically — new mod themes should be added to `ModCompatibility` rather than hardcoded elsewhere.

---

## 6. Setting Up Your Development Environment

### Prerequisites

- **Java 17** (required by Minecraft Forge 1.19.2)
- **Gradle** (the wrapper `gradlew` is included — no separate install needed)
- **Git**
- An IDE: **IntelliJ IDEA** (recommended) or **VS Code** with the Java extension pack

### First-Time Setup

```bash
# Clone the repository
git clone https://github.com/ScottyVenable/off-to-market-minecraftMod-1.19.2.git
cd off-to-market-minecraftMod-1.19.2

# Generate IDE run configs and decompile Minecraft
./gradlew genIntellijRuns   # IntelliJ IDEA
# or
./gradlew genVSCodeRuns     # VS Code
```

Import the project as a **Gradle project** in your IDE.

### Build Commands

| Command | What it does |
|---------|-------------|
| `./gradlew classes` | Fast recompile — use for hot-swap during development |
| `./gradlew build` | Full build, produces `build/libs/offtomarket-*.jar` |
| `./gradlew runClient` | Launch the Minecraft client with the mod loaded |
| `./gradlew runServer` | Launch a local dedicated server |

### Hot-Swap Workflow (Recommended for Iteration)

1. Start the game via the **"Debug: BootstrapLauncher"** run config in your IDE.
2. Make a code change.
3. Run `./gradlew classes` (or press Ctrl+Shift+B in VS Code).
4. Trigger hot-swap in the debug panel.
5. No restart needed for most logic changes.

### In-Game Debug Commands

All require OP level 2:

```
/otm debug          — toggle debug overlay
/otm status         — print block entity state to chat
/otm grant coins <n> — add n copper pieces to your wallet
/otm balancetest    — run economy sanity checks
```

---

## 7. How to Contribute

### 7.1 Code Contributions

1. **Pick up an issue** from the issue tracker or coordinate with the project lead before starting significant work.
2. **Branch from the current active minor branch** (e.g., `v0.6`) — never work directly on `main` or `dev`.
3. **Keep changes focused** — one feature or fix per pull request makes review faster.
4. **Run the game and test your changes** before opening a PR. Use the debug commands and `instantTravel`/`freePurchases` config options to speed up testing.
5. **Bump the version** in `gradle.properties` (`mod_version`) for every meaningful change and append a matching entry to `CHANGELOG.md`.

Key entry points for common tasks:

| Task | Where to start |
|------|---------------|
| New block | `block/` + `block/entity/` + register in `registry/` |
| New item | `item/` + register in `registry/` |
| New network action | `network/` + register in `ModNetwork` |
| New GUI screen | `client/screen/` + `menu/` for container-based screens |
| New `/otm` command | `debug/DebugCommands.java` |
| New config option | `config/ModConfig.java` + read via `DebugConfig` |
| New pricing rule | `data/PriceCalculator.java` (follow the 6-step pipeline) |
| New mod compat theme | `data/ModCompatibility.java` |

### 7.2 Data-Driven Content (Towns and Custom Menus)

No Java changes are needed to add new towns or custom shop menus.

#### Adding a New Town

1. Copy `src/main/resources/data/offtomarket/towns/_template.json` to a new file, e.g., `mytown.json`.
2. Fill in all fields:
   - `id` — unique snake_case identifier (e.g., `"harbor_rest"`)
   - `displayName` — shown in-game
   - `description` — flavour text for the Market Board
   - `distance` — 1 (nearby) to 10 (very far); affects travel time and price premiums
   - `type` — one of `VILLAGE`, `TOWN`, `CITY`, `MARKET`, `OUTPOST`
   - `minTraderLevel` — 1–5; gates the town until the player reaches this level
   - `sells` — list of item registry IDs the town stocks
   - `needLevels` — map of item registry ID → `NeedLevel` demand value
   - `letters` (optional) — NPC letters sent on game events
3. Add the filename (e.g., `"mytown.json"`) to `towns/_index.json`.
4. Launch the game and verify the town appears in the Trading Post's Towns tab.

Available `NeedLevel` values: `DESPERATE`, `HIGH_NEED`, `MODERATE_NEED`, `BALANCED`, `SURPLUS`, `OVERSATURATED`.

#### Adding a New Custom Menu Screen

1. Copy `src/main/resources/data/offtomarket/custom_menus/_template.json` to a new file.
2. Fill in the `entries[]` array with `{ item, label, description, price, stock, category }` objects.
3. Add the filename to `custom_menus/_index.json`.
4. Test in-game: `/otm menu open <your-menu-id>`.

#### Notes for Content Designers

- JSON `needLevels` maps may include `"_comment": "..."` entries for documentation — `TownLoader` skips all keys starting with `_`.
- Use full registry IDs: `"minecraft:iron_ingot"`, `"minecraft:diamond"`, `"farmersdelight:rice"`, etc.
- Towns with `minTraderLevel: 3` or higher should generally have richer inventories and higher-value specialty goods to justify the progression gate.
- Avoid giving towns `DESPERATE` demand for items they also `sells` — that is both inconsistent lore-wise and an economy exploit.

### 7.3 Art and Textures

All textures go under `src/main/resources/assets/offtomarket/textures/`. See **TEXTURE_GUIDE.md** for a full table of every required file, its dimensions, and visual guidelines.

Quick reference:

| Category | Size | Location |
|----------|------|----------|
| Block textures | 16×16 px | `textures/block/` |
| Item textures | 16×16 px | `textures/item/` |
| GUI backgrounds | 256×256 px | `textures/gui/` |

Recommended tools: [Aseprite](https://www.aseprite.org/), [Piskel](https://www.piskelapp.com/), or [Pixilart](https://www.pixilart.com/).

The mod uses a consistent **medieval trading** palette — warm wood browns, parchment tones, and metallic coin colors. Refer to the color palette section in TEXTURE_GUIDE.md before starting new artwork.

When submitting texture work, include a screenshot of the texture rendered in-game so reviewers can assess it in context.

### 7.4 Ideas and Design Feedback

If you have an idea for a new feature, economy mechanic, town, or quest type, open a **GitHub issue** with the label `idea` or `design`. Good idea submissions include:

- **What** the feature is (one sentence)
- **Why** it fits the mod's trading-empire fantasy
- **How** a player would interact with it
- Any **balance concerns** you have already identified

Check the **TODO.md** and open issues before submitting — the idea may already be planned or in progress.

---

## 8. Coding Conventions

- **Package**: all classes live under `com.offtomarket.mod`.
- **State ownership**: all game state for a block goes in the `BlockEntity`. Never put mutable state in the `Block` or `Menu`.
- **Config access**: always read config values via `DebugConfig.get*()`, not `ModConfig.*` directly.
- **NeedLevel**: always use `needLevels` map with `NeedLevel` values. Never use the old binary `needs`/`surplus` sets.
- **ResourceLocation**: use `ResourceLocation.tryParse(str)` for untrusted strings; use `new ResourceLocation(namespace, path)` for known-safe inputs.
- **Button API**: use `new Button(x, y, w, h, label, handler)` — the `Button.builder(...).build()` API does not exist on 1.19.2.
- **UI colors**: import `OtmGuiTheme` into every new screen; never hardcode hex colors inline.
- **Logging**: use a class-level `static final Logger LOGGER` and wrap non-trivial logic in try/catch blocks in tick methods and packet handlers. Server tick crashes must be caught and logged rather than crashing the game.
- **Javadoc**: avoid `{@code}` or `{@link}` tags in Javadoc that contains dashes, arrows, or HTML list tags — the project's compile chain can flag these incorrectly. Use plain text.
- **Version bump**: bump `mod_version` in `gradle.properties` and add a `CHANGELOG.md` entry with every meaningful commit.

---

## 9. Branching and Versioning Strategy

The project uses **semantic versioning**: `MAJOR.MINOR.PATCH` (e.g., `0.6.1`).

### Branch Layout

| Branch | Purpose |
|--------|---------|
| `main` | Stable, release-ready code |
| `dev` | Integration branch for tested but unreleased work |
| `v0.X` | Active development branch for minor version X |

### Patch Workflow (e.g., 0.6.1 → 0.6.2)

- Work directly on the active minor branch (e.g., `v0.6`).
- Bump `mod_version` to `0.6.2` in `gradle.properties`.
- Add a `CHANGELOG.md` entry under the new version header.
- Open a PR from `v0.6` → `dev` (or `main` if release-ready).

### Minor/Major Version Workflow (e.g., starting v0.7)

1. Confirm that the current minor branch (`v0.6`) is fully tested.
2. Open a PR: `v0.6` → `dev` (tested but unreleased) or `v0.6` → `main` (release-ready).
3. After the PR is merged, create a new branch `v0.7` from `main` or `dev`.
4. Update `mod_version` to `0.7.0` and add a `CHANGELOG.md` entry.

---

## 10. Pull Request Process

1. **Branch**: create your branch from the active minor version branch (e.g., `v0.6`), not from `main`.
2. **Title**: use a short, descriptive title: `feat: add Harbor Rest town`, `fix: Trading Ledger item disappearance`, `chore: bump version to 0.6.2`.
3. **Description**: explain what changed and why. Link to the relevant issue if one exists.
4. **Checklist before opening**:
   - [ ] `mod_version` bumped in `gradle.properties`
   - [ ] `CHANGELOG.md` updated
   - [ ] Game tested with the feature/fix exercised in-game
   - [ ] No debug/test-only code left in (e.g., `freePurchases = true` committed)
   - [ ] New textures render correctly in-game (include a screenshot)
5. **Review**: at least the project lead must approve before merging.
6. **Merge**: squash-merge to keep history clean on minor version branches.

---

*Thank you for contributing to Off to Market! If you have questions not covered here, open an issue or reach out to the project lead directly.*
