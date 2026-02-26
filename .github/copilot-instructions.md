# Off to Market — AI Agent Instructions

**Forge 1.19.2 / Java 21 Minecraft mod.** Root package: `com.offtomarket.mod`.

---

## Build & Run

```powershell
./gradlew classes          # Quick recompile for hot-swap (Ctrl+Shift+B in VS Code)
./gradlew build            # Full distributable JAR
```

- The game client runs via the "Debug: BootstrapLauncher" launch config.
- Hot-swap: recompile with `./gradlew classes` while the client is running, then trigger a hot-swap in the debug panel — no restart needed for logic changes.
- In-game debug commands: `/otm debug`, `/otm status`, `/otm grant coins <n>`, `/otm balancetest`. All require OP level 2.

---

## Architecture Overview

```
Block (TradingPostBlock)           — placement, use() opens GUI
  └─ BlockEntity (TradingPostBlockEntity) — ALL game logic, NBT persistence, serverTick()
       └─ Menu (TradingPostMenu)   — Container slots wired to BE
            └─ Screen (TradingPostScreen) — client-side rendering only

data/                              — pure logic, no Minecraft lifecycle coupling
  PriceCalculator  — 6-tier pricing pipeline for any ItemStack
  TownRegistry     — static town map + JSON-loaded towns (via TownLoader)
  MarketListing    — NPC town Market Board listing generation
  ModCompatibility — runtime item discovery + dynamic town generation
  NeedLevel        — 6-tier demand enum (DESPERATE → OVERSATURATED)
  SupplyDemandManager — server-tick demand drift

content/                           — data-driven content loaded from JSON at startup
  TownLoader / TownDefinition      — reads data/offtomarket/towns/*.json
  CustomMenuLoader / CustomMenuRegistry — reads data/offtomarket/custom_menus/*.json

network/                           — Forge SimpleChannel packets
  ModNetwork       — registers all packets; client→server use PLAY_TO_SERVER,
                     server→client use PLAY_TO_CLIENT
```

`TradingPostBlockEntity` (~2100 lines) is the primary authoritative state owner for the Trading Post. All shipment, XP, coin, and town logic lives there. Do not split state across the Block or Menu. As the mod grows, additional block entities may be introduced to offload UI-heavy concerns — follow the same contained-state pattern (all state in the BlockEntity, no state in the Block or Menu).

### Other Systems

- **Quest system** (`data/Quest.java`, managed in `TradingPostBlockEntity`) — delivery and specialty quests generated per town; rewards grant XP and coins. Read those files for full implementation details.
- **Worker system** (`data/Worker.java`) — hireable NPCs that provide passive trade bonuses; hire cost and bonuses configured via `ModConfig[workers]`.
- **Diplomat system** (`data/Diplomat.java`) — premium price negotiation unlocked at higher trader levels; state tracked inside `TradingPostBlockEntity`.
- **SupplyDemandManager** (`data/SupplyDemandManager.java`) — runs on server tick to drift `NeedLevel` values for active towns over time, simulating supply and demand.

---

## Economy Model

- **Currency**: Copper Pieces (CP). 1 gold = 100 CP, 1 silver = 10 CP.
- **PriceCalculator** resolves in this order — **do not skip steps**:
  1. Ingredient-based (tool/armor Tier + ArmorMaterial repair ingredient × crafting premium)
  2. Exact item overrides (hardcoded vanilla)
  3. Forge tag rules (works for modded items automatically)
  4. `classifyByClass()` — instanceof checks incl. food via `FoodProperties`
  5. `classifyByPath()` — registry-name keyword heuristics for leftover modded items
  6. Rarity fallback
- **Results are cached** in `BASE_TIER_CACHE` keyed by `Item` type (not stack). Enchanted books are special-cased before the cache.
- `NeedLevel` enum drives both player-sale multipliers and `computeTownListingPrice()` in `MarketListing`.
- **Do not use the old binary `needs`/`surplus` sets** in new code — always use `needLevels` map with `NeedLevel` values.

---

## Adding Content (Data-Driven — No Hardcoding Needed)

### New Town
1. Copy `src/main/resources/data/offtomarket/towns/_template.json` → `mytown.json`
2. Set `id`, `displayName`, `description`, `distance` (1–10), `type`, `minTraderLevel`, `sells[]`, `needLevels{}`
3. Add `"mytown.json"` to `towns/_index.json`

### New Custom Menu Screen
1. Copy `src/main/resources/data/offtomarket/custom_menus/_template.json` → `mymenu.json`
2. Fill `entries[]` with `{ item, label, description, price, stock, category }` fields
3. Add to `custom_menus/_index.json`
4. Test in-game: `/otm menu open mymenu`

### New Town in Code (fallback / dynamic)
Call `TownRegistry.register(TownData)` — public since v0.2. JSON towns override same-ID hardcoded entries on load.

---

## Adding a Network Packet

1. Create `network/MyPacket.java` with `encode`, `decode`, `handle` static methods.
2. Register in `ModNetwork.register()` with direction `PLAY_TO_SERVER` or `PLAY_TO_CLIENT`.
3. Server→Client packets **must** use `DistExecutor.unsafeRunWhenOn(Dist.CLIENT, ...)` inside `handle()`.
4. Use `ctx.get().enqueueWork(() -> ...)` for all world interactions.

---

## Key Conventions

- **Button constructor** is 1.19.2-style: `new Button(x, y, w, h, label, handler)` — the `Button.builder(...).build()` API doesn't exist on 1.19.2.
- **ResourceLocation**: use `ResourceLocation.tryParse(str)` for untrusted strings; `new ResourceLocation(namespace, path)` for known-safe inputs. The single-arg `new ResourceLocation(string)` constructor is deprecated in later versions but works here.
- **Javadoc**: avoid `{@code}` or `{@link}` tags in Javadoc that contains `–`, `→`, or `<ul>` — the project's compile chain flags these incorrectly as "unexpected end of comment". Use plain text.
- **NeedLevel comment keys**: JSON `needLevels` maps may include `"_comment": "..."` entries — `TownLoader` skips all keys starting with `_`.
- **OtmGuiTheme** (`client/screen/OtmGuiTheme.java`) is the single source for all UI colors, `drawPanel`, `drawInsetPanel`, `drawSlot`, `drawDividerH/V`, and text truncation. Import it into every new screen.
- **DebugConfig** wraps `ModConfig` with runtime overrides; always read values via `DebugConfig.get*()` methods in runtime logic, not `ModConfig.*` directly.

---

## README & Documentation Maintenance

`README.md` is the public-facing description and tends to drift out of date as features are added. The agent must:
- Update the version badge in `README.md` whenever `mod_version` in `gradle.properties` changes.
- Verify the towns table, feature list, and roadmap match what is actually implemented.
- Flag or update any stale content found while working on the codebase.
- Treat README accuracy as a first-class task, not an afterthought.

---

## Versioning, Changelogs & Branching

### Version Bumping
On every meaningful edit or commit:
1. Update `mod_version` in `gradle.properties` (e.g. `0.2.5` → `0.2.6` for a patch, `0.2.x` → `0.3.0` for a minor update).
2. Update the version badge in `README.md` to match.
3. Append an entry to `CHANGELOG.md` describing what changed.

### Branch Strategy
- **Minor/major updates** (e.g., v0.4, v0.5, v0.6) get their own branch: `v0.4`, `v0.5`, etc.
- **Patches and tweaks** within a minor version (v0.4.1, v0.4.2, ...) are committed directly to the active minor branch — no new branch per patch.
- Only create a new minor/major branch when the user explicitly requests a new update increment (e.g., moving from v0.4.x work to starting v0.5).

### Starting a New Minor or Major Update
When the user asks to begin a new minor or major version (e.g., asking about v0.6 while on v0.5), the agent must:
1. Ask: "Is the current `<branch>` work fully implemented and tested?"
2. Ask: "Do you want to create a pull request for `<branch>` before starting the new version?"
3. If the answer is yes to a PR:
   - If tested and **release-ready** → PR to `main`.
   - If tested but **not yet ready for release** → PR to `dev`.
   - Do not merge without the user confirming they have tested it.
4. After confirming (or skipping the PR), create the new branch from `main` or `dev` as appropriate.

---

## Key Files

| File | Purpose |
|------|---------|
| `block/entity/TradingPostBlockEntity.java` | Main game state + shipment logic (~2100 lines) |
| `data/PriceCalculator.java` | Item pricing pipeline (~1140 lines) |
| `data/TownRegistry.java` | Static + JSON-loaded town registry |
| `data/MarketListing.java` | NPC listing generation with town-aware pricing |
| `data/ModCompatibility.java` | Runtime mod item discovery + dynamic towns |
| `client/screen/OtmGuiTheme.java` | All UI theme constants and draw helpers |
| `debug/DebugCommands.java` | All `/otm` commands — add new subcommands here |
| `network/ModNetwork.java` | Packet registration — add new packets here |
| `content/TownLoader.java` | JSON → TownData converter |
| `content/CustomMenuLoader.java` | JSON → CustomMenuDefinition converter |
