# Off to Market (Trading Deluxe) - Changelog

## Version 0.4.9 — Expanded In-Game Commands

### New Command — /otm help
- Added `/otm help` to print a full, categorized list of all `/otm` commands with short descriptions.
- Added `/otm help <topic>` for detailed help per category: `debug`, `grant`, `settings`, `town`, `price`, `menu`.

### New Command — /otm town
- Added `/otm town list` to display a formatted table of all registered towns (ID, name, type, distance, min level).
- Added `/otm town info <id>` to show full details for a specific town: description, need levels (color-coded), and specialty/sell items.

### New Command — /otm price
- Added `/otm price` to look up the base and max CP price of the item in the player's main hand.
- Output includes coin breakdown (gold/silver/copper) and item rarity.

### New Command — /otm debug uibounds
- Added `/otm debug uibounds` to toggle the UI bounding box overlay (maps to `DebugConfig.SHOW_UI_BOUNDS`).

---

## Version 0.4.8 — UI Polish, Bug Fixes & Master Ledger

### Bug Fix — Send to Market Button Always Active
- `sendBtn.active` is now forced to `true` every tick in `containerTick()`, removing the unintended gray-out.

### Bug Fix — Trading Ledger Virtual Items Dropped on Block Break
- Fixed a critical bug where breaking a Trading Ledger block would drop items that belonged to adjacent containers (virtual slots) rather than protecting them.
- Root cause: `slotToVirtualSource` is populated by the server tick scan and not persisted to NBT. If broken before the first scan tick after world load, all items dropped.
- Fix: `isVirtualSlot()` now falls back to `clientVirtualSlots` (which IS loaded from NBT) on the server side.

### New Item — Master Ledger (renamed from Ledger)
- The **Ledger** item is now called the **Master Ledger**.
- Right-clicking opens a new aggregated overview screen showing all nearby Trading Ledger bins (within 64 blocks): position, item count, and payout per bin.
- Total items and payout are summarised at the bottom.
- A **Send to Market** button automatically finds the nearest Trading Post and triggers shipment from there.
- Shift+use still opens the Coin Exchange screen.

### Market Board — Auto-Refresh (no manual Refresh button)
- The manual "Refresh" button has been removed. The Market Board now **auto-refreshes listings** every 5 minutes (6000 ticks) without any player interaction.
- On first placement (or if listings are empty after world load), listings generate immediately.
- A **countdown timer** ("Next: M:SS") is shown in the title bar so players know when the next refresh will occur.

### Market Board — Cart Toggle Tab Style
- The "Cart (n)" button in the title bar now uses the same tab style as the Trading Post tabs (gold border when active, dark when inactive).
- Click detection is handled manually; no vanilla Button widget is rendered.

### Market Board — Pagination Label Repositioned
- The "X listings (1-14)" count label and the cart total summary were moved from **y=120** (mid-listing) to **y=188** (below the last listing row), so they no longer overlap the listing table.

### Trading Ledger — Tabs Restyled (Trading Post Style)
- The four tab buttons (Bin, Fees, History, Income) now use the same fill()-based rendering as the Trading Post tabs: gold border + dark body when selected; dark border + darker body when unselected.
- Tab label text renders in gold (selected) or gray (unselected) directly via `renderLabels()`.
- Vanilla Button widgets for tabs have been removed; click detection is handled in `mouseClicked()`.

### Trading Ledger — Renamed "Trading Bin" → "Trading Ledger" in UI
- The left-panel title label no longer shows "Trading Bin"; it now correctly reads **"Trading Ledger"** in gold.

### Trading Ledger — Fees Tab Modifier Layout Improved
- Modifier rows (Ench/Used/Dmgd/Rare) now use **14px vertical spacing** instead of 11px, giving the section more breathing room.
- The divider separating the modifier section from the summary moved down to y+136.
- The per-item summary and "Select an item to preview modifiers" hint are now drawn at y+140/150, reducing dead space.

## Version 0.4.6 — UI Polish, Finance Table & Economy Fixes

### Bug Fix — "Send to Market" Button
- Fixed a bug where the **"Send to Market" button remained grayed out** even when items were present in the Trading Ledger bin.
- Root cause: `sendBtn.active` was only updated on user actions, not every tick. Added a `containerTick()` override that reads `menu.hasBinItems()` on each tick.

### Blocks — Axe Tool Preference
- All mod blocks (`trading_post`, `trading_ledger`, `market_board`, `mailbox`, `finance_table`) now correctly require and prefer an **axe** to break.
- Added `data/minecraft/tags/blocks/mineable/axe.json` with all 5 blocks.
- Added `.requiresCorrectToolForDrops()` to all block registrations.

### Animal Trade Slip Pricing
- **Animal Trade Slip base values now reflect the animal type** instead of defaulting to 8c (TIER_COMMON).
- `PriceCalculator.getBaseValue()` checks for `TAG_ANIMAL_TYPE` NBT and delegates to `AnimalTradeSlipItem.getBaseValue()`.
- Unfilled slips return 40c. Filled slips return 50c (Chicken) to 2000c (Allay) based on animal.

### Trading Ledger — Income Tab & Taller Window
- Window height increased from **166px to 230px** (matches Mailbox), giving more room for bin lists and content.
- **MAX_VISIBLE bin rows increased from 8 to 12.**
- Added a **4th "Income" tab** that aggregates all past shipments: total revenue, total items shipped, and per-town revenue breakdown (scrollable, sorted by revenue).
- Tabs resized to 4 × 49px to accommodate the new tab.
- Bottom widgets (Withdraw button, Upgrade Caravan) repositioned to match new height.

### Market Board — Cart Persistence
- **Shopping cart is no longer lost when closing the Market Board.** Cart is saved to a static per-`BlockPos` map on `onClose()` and restored in `init()`, persisting across GUI close/reopen within the same session.
- Cart is cleared after a successful Checkout, as expected.

### Market Board — UI Overhaul
- **Player inventory view removed** from the Market Board screen.
- Freed space used for more listings: **VISIBLE_LISTINGS increased from 8 to 14**, cart rows from 7 to 13.
- **Coin Balance strip** added at the bottom of the screen (shows inventory coins + coin bags + nearby Finance Tables).
- Checkout and scroll buttons repositioned to match the extended listing area.

### New Block — Finance Table
- Added the **Finance Table** block: a 27-slot coin storage vault (3 rows × 9 columns).
- Coins dropped on block break; shift-click transfers between table and inventory.
- `FinanceTableBlockEntity.getTotalCoinValue()` sums all stored CoinItem stacks.
- The Market Board **coin balance strip** scans nearby Finance Tables (within 16 blocks) and includes their stored value.
- Mineable with an axe; drops stored contents when broken.

### Trading Post — List Expansion & UI Cleanup
- All scrollable lists in the Trading Post now use the full **230px window height**.
- **Activity tab**: visible rows increased from 7 to **13** (12px rows); detail bar repositioned to bottom of list area.
- **Quests tab**: visible rows increased from 6 to **11** (14px rows).
- **Requests tab**: visible rows increased from 6 to **11** (14px rows).
- **Towns tab**: visible town list rows increased from 8 to **15**; left/right panels and center spine extended to fill height.
- **Workers tab**: left list panel and right detail panel extended to 186px; Hire/Dismiss buttons repositioned to bottom of panel.

---

## Version 0.4.5 — Virtual Container Sync & Past Orders History

### Trading Ledger — Virtual Container Reading
- **Adjacent containers are now read virtually** — items in chests/barrels next to the Trading Ledger appear in the ledger bin as snapshots; they are not physically moved until shipped.
- `slotToVirtualSource` tracks each virtual slot back to its source container and slot index.
- NBT syncs a `VirtualSlots` int array to the client for correct display.
- Context menu shows **"Remove from Ledger"** instead of "To Container" for virtual items (the item stays in its source container).
- "To Inventory" on a virtual item withdraws it from the source container directly.
- Re-pull suppression (`suppressContainerSync`) prevents an item just moved to a container from immediately re-appearing as a virtual snapshot.

### Trading Ledger — Past Orders Tab
- **New "History" tab** (third tab, 68px wide) on the Trading Ledger screen.
- Each completed shipment records: destination town, total items shipped, total coin value, and game timestamp.
- Up to **20 past orders** are retained and persisted in NBT.
- History list is scrollable (mouse-wheel + scroll arrows) with time-ago display (< 1 min / N min / N hr / N days).
- Column headers: Town | Items | Value | When.

### Trading Post — Send to Market
- `sendItemsToMarket()` now drains source containers for all virtual slots before clearing the ledger.
- Calls `recordShipment()` on the ledger block entity to log each completed shipment.

---

## Version 0.4.4 — UI Overhaul & Economy Polish

### Trading Post — Inventory Removed
- **Player inventory no longer shown** in the Trading Post screen; the GUI uses the full 193px content height for trade content.
- Overview panel expanded to show more at-a-glance info.

### Trading Post — Economy Tab
- **Vertical income tables** — Best Towns and Top Items each use the full panel width in a stacked layout.
- **g/s/c text labels** instead of coin icons for all coin values (e.g. `1g 2s 3c`).

### Trading Post — Towns Tab
- **Right panel is now fully scrollable** — Description text word-wraps and scrolls; specialty items, needs, and surplus entries are all accessible via mouse-wheel.
- **Expanded panels** — Both left and right panels extended to 158px height with improved separator spacing.
- Scroll arrows appear when content overflows the visible area.

### Trading Ledger — Fees Tab
- **Modifier text no longer clips** into the Upgrade Caravan button; layout tightened with better separator and text positions.

### Trading Ledger — Context Menu
- **Hover highlight** on "To Inventory" / "To Container" rows: background tint + gold text on hover.

### Send to Market
- **Send to Market button grays out** when the connected Trading Ledger is empty or no Ledger is nearby.

---

## Version 0.4.3 — Trading Bin Renamed to Trading Ledger

### Block Rename
- **Trading Bin → Trading Ledger** — The storage block for items awaiting shipment has been renamed. Registry ID, item, lang keys, loot table, recipe, and all code references updated.
- **Directional placement** — The Trading Ledger now has a FACING property (north/south/east/west) and faces the player on placement, matching the Trading Post behavior.
- **New directional model** — Block now uses four distinct textures: `trading_ledger_front`, `trading_ledger_back`, `trading_ledger_side`, and `trading_ledger_top`. The blockstates JSON handles rotation so the front always faces the player.

---

## Version 0.4.2 — UI Polish, Reputation Overhaul & Trading Bin Improvements

### Reputation System
- **Expanded rep range** — Reputation now ranges from -1000 to +1000 (was 0–200), supporting negative standing with towns.
- **7 reputation tiers** — Hostile, Unfriendly, Distrusted, Neutral, Friendly, Honored, Exalted (was 5 tiers).
- **Scaled rep rewards** — Quest reputation rewards scaled 5x (15–125 range); each sale now grants 5 rep per item sold (was 1).
- **Bidirectional rep bar** — Reputation bar on the Towns tab now starts in the center for Neutral and fills left (negative) or right (positive).
- **Town name colors** — Unselected town names in the left-page list are now tinted by that town's reputation color.

### Market Board Fix
- **No more "Uncraftable" items** — Potions, splash/lingering potions, and tipped arrows in town specialties now generate with valid random NBT (e.g., `{Potion: "minecraft:swiftness"}`). Items that still render as "Uncraftable" are skipped entirely.

### Trading Bin — Fees Tab Improvements
- **Auto-applied modifiers** — Enchantment markup, rarity markup, used discount, and damaged discount are now always auto-applied based on item properties. Checkboxes removed; colored indicators show applicability for the selected item.
- **Apply button now auto-prices** — Pressing Apply in the Fees tab now also fills any unpriced slots with their calculated fair value.
- **Enter key unfocuses inputs** — Pressing Enter in the Trading Tax or Min Markup fields now commits the value and unfocuses the input.
- **Caravan Weight removed from Fees** — Weight display moved to caravan-specific UI; no longer shown in Fees panel.
- **Scroll clipping fixed** — Max visible rows reduced from 9 to 8 so the bottom item never overlaps the Payout text.

### Trading Bin — Right-Click Withdraw Menu
- **Context menu on right-click** — Right-clicking a row in the Trading Bin list opens a small overlay with two options: "To Inventory" and "To Container".
- **To Container** — Attempts to push the item into an adjacent container block entity; falls back to player inventory if no adjacent container has room.

### Trading Post — Non-Trade Tabs
- **Inventory hidden on non-Trade tabs** — Player inventory slots and label only render on the Trade tab, giving other tabs the full content height.
- **Expanded content area** — Non-Trade tabs now use 193px of vertical height (was 112px shared with inventory).
- **Income tab unified** — Best Towns and Top Items sections are now stacked in a single full-height scrollable panel instead of two side-by-side panels.

### Build
- **JAR naming** — Build output now named `offtomarket-vX.Y.Z-Forge-1.19.2.jar`.

---

## Version 0.4.1 — Developer Documentation & README Refresh

- Updated `.github/copilot-instructions.md`: expanded architecture notes, added Other Systems section (Quest, Worker, Diplomat, SupplyDemandManager), added README Maintenance and Versioning/Branch Strategy sections.
- Refreshed `README.md`: corrected version badge, replaced stale towns table with the actual 9 JSON-loaded towns, marked JSON town system as complete on roadmap.

## Version 0.4.0 — Trading Bin Capacity, Fees, and Caravan Weight

### Universal Fees & Pricing
- **Unified Fees Application** — Trading tax, markup, and item modifiers now run through one universal effective-price pipeline for bin list display, price preview, and shipment dispatch.
- **Fees Tab Price Updating Fixed** — Changing values in Fees now immediately affects displayed final pricing behavior and total payout calculations.

### Bin Storage & Scrolling
- **Expanded Bin Capacity** — Trading Bin storage increased from 9 slots to **200 slots**.
- **Larger Slot Stack Cap** — Bin slots now support up to **200 items per slot**.
- **Scrollable Item List** — Bin item area now supports mouse-wheel scrolling with up/down indicators for large inventories.

### Payout Visibility
- **Live Total Proposed Payout** — Added a running payout total for the entire bin that updates as prices, fees, and modifiers change.

### Deposit & Adjacent Container Support
- **Tool Deposit Support** — Right-click deposit now uses partial insertion logic and no longer fails hard on non-full insertion cases.
- **Touching Container Aggregation** — Any container touching the Trading Bin is scanned and its contents are pulled into bin inventory when the bin is used.

### Caravan Weight System
- **Weight Capacity Cap** — Added a caravan weight limit for the bin based on item weight.
- **Upgradeable Capacity** — Added caravan weight upgrades (purchased with coins) from the Fees tab.
- **Weight HUD** — Added current/maximum caravan weight display in the bin UI.

### Network & Backend
- **New Packet** — Added `UpgradeCaravanWeightPacket` and registered it in `ModNetwork`.
- **Shipment Pricing Sync** — Trading Post shipment creation now uses the Trading Bin effective price calculation.

## Version 0.3.1 — Trading Post UX Checkpoint

- Added Activity/Quests/Requests sorting controls.
- Added Activity row details-on-click behavior.
- Added dedicated Income tab with economy and worker wage visibility.
- Added request flow/status polish, quantity entry, and UI debug bounds toggle.

## Version 0.2.5 — Trading Bin Overhaul & Market Improvements

### Trading Bin Complete Redesign
- **List View Interface** — Replaced 3×3 grid + inspection slot with a searchable list view on the left panel.
- **Searchable Inventory** — Type to filter bin contents by item name; filtered count displayed.
- **Custom List Rendering** — Each row shows 12×12 item icon, truncated item name, and coin price (using CoinRenderer for colored denominations).
- **Right-Click Deposit** — Players now deposit items by right-clicking the Trading Bin block while holding items; no drag-and-drop needed.
- **Withdraw Button** — Click a list item to select, then click Withdraw to take it back into player inventory via WithdrawBinItemPacket.
- **Inspection Slot Removed** — Eliminated the dedicated inspection slot; pricing now done entirely through list selection.
- **Player Inventory Removed** — Trading Bin menu no longer displays player inventory; all slots positioned off-screen.
- **Persistent Price Book Tab** — Price setting UI remains in the right Bin tab with price input and auto-population when items are selected.
- **Modifiers Tab Unchanged** — Fees tab continues to show crafting tax, min markup, auto-price mode, and per-item modifier toggles.

### Market Board Improvements
- **Town Column Repositioned** — Shifted left by 12px (dot x=132, arrow x=136, name x=144) with 84px truncation for better spacing.
- **Refresh Cooldown System** — 5-minute cooldown between market refreshes (configurable via MarketBoardBlockEntity.REFRESH_COOLDOWN_TICKS).
- **Countdown Display** — Refresh button shows M:SS countdown during cooldown; button disabled until refresh is available.
- **Cheats Bypass** — New DebugConfig.UNLIMITED_REFRESHES flag allows unlimited refreshes when enabled.
- **Server-Side Ticker** — Added serverTick() to MarketBoardBlockEntity for countdown decrement; registered via getTicker() in MarketBoardBlock.

### Bug Fixes
- **Air Item Prevention** — Added guards at 4 points to prevent Items.AIR from being sold:
  - TradingBinBlockEntity.addItem() — Guards against empty/air items in deposits
  - TradingBinBlockEntity.setPrice() — Enhanced container methods with proper bounds checking
  - MarketListing.generateListings() — Prevents air items from entering market listings
  - Shipment.ShipmentItem.createStack() — Validates item isn't air before creating stacks
  - TradingPostBlockEntity.sendItemsToMarket() — Guards against air in shipment processing
- **Workers Tab Rendering** — Fixed text overlap by repositioning worker labels from rowY=6 to rowY=38+i×32 with compressed detail rendering.

### Reputation & Experience Balancing
- **Worker XP Increase** — Increased from 1 XP per trip to 3 XP per trip (Worker.completedTrip()).
- **Sales-Based Reputation** — Trading Posts now grant reputation on regular sales (in addition to quests):
  - Amount = number of items sold in the shipment (SoldCount)
  - Applied on SOLD→COMPLETED state transition in TradingPostBlockEntity
  - Eliminates the slow reputation grind when relying only on quests

### Coin Text Coloring
- **Colored Denominations** — Coin amounts now render with color codes across all screens:
  - Gold: §e (yellow/gold)
  - Silver: §7 (light gray)
  - Copper: §6 (dark orange)
- **Affected Screens** — TradingBinScreen, TradingPostScreen, MarketBoardScreen (all use updated formatCoinText())
- **Escape Sequences** — Uses \u00A7e, \u00A77, \u00A76, \u00A7r for robust rendering

### Network Changes
- **WithdrawBinItemPacket** — New packet for withdrawing items from Trading Bin to player inventory with validation.
- **ModNetwork Registry** — Registered new packet in ModNetwork.register().

### Technical Details
- **TradingBinMenu**: Simplified to 9 bin slots + 36 player slots, all positioned off-screen; handles shift-click moves programmatically.
- **TradingBinScreen**: Complete rewrite (∼1200 lines); features list filtering, item selection, price book management, modifier toggles.
- **TradingBinBlockEntity**: Added air guards in critical methods; container methods now include proper bounds checking.
- **TradingBinBlock**: Updated use() to deposit held items before opening GUI; cleaned up onRemove() (removed inspection slot handling).
- **MarketBoardBlockEntity**: Added cooldown timer system with canRefresh(), getRefreshCooldown(), and serverTick() methods.
- **MarketBoardBlock**: Registered ticker via getTicker() for server-side tick processing.

## Version 0.2.4 — Workers System Expansion

### New Worker: Bookkeeper
- **Third Worker Type** — The Bookkeeper manages trade finances, reducing overhead costs for all hired workers per trip.
- **Cost Reduction Scaling** — Starts at ∼8% reduction, scaling up to 35% (configurable) as the Bookkeeper levels up.
- **Config Options** — bookkeeperHireCost (default 1040 CP) and bookkeeperMaxCostReduction (default 35%) added to mod config.

### Worker Level Titles & Perks
- **Level Titles** — Workers now display ranked titles: Novice (1-2), Apprentice (3-4), Journeyman (5-6), Expert (7-8), Master (9), Grandmaster (10).
- **Title Colors** — Each title tier has a distinct color from gray to gold.
- **Perk Milestones** — Workers unlock named perks at levels 3, 6, and 9:
  - Negotiator: Bulk Pricing → Silver Tongue → Trade Mastery
  - Trading Cart: Quick Loading → Shortcut Finder → Express Routes
  - Bookkeeper: Penny Pincher → Market Analyst → Financial Advisor
- **Perk Effects** — Bookkeeper's Penny Pincher (Lv3) lets their own overhead costs be reduced; Financial Advisor (Lv9) adds +5% cost reduction.

### Worker Dismissal
- **Fire Workers** — Players can now dismiss hired workers via the ✖ Dismiss button.
- **Partial Refund** — Firing returns 50% of the original hire cost.
- **FireWorkerPacket** — New network packet handles dismissal with server-side distance validation.

### Workers Tab UI Redesign
- **List + Detail Layout** — Workers tab now shows a clickable worker list on the left and a detailed info panel on the right.
- **Interactive Worker List** — Click to select a worker; hover highlighting and gold accent for the selected row.
- **Status Indicators** — Green dot for hired workers, gray dot for unhired.
- **Rich Detail Panel** — Shows worker name, title, level with XP bar (sheen effect), current bonus, cost/trip, trip count, and perk status.
- **Perk Display** — Perks shown with ✔ (unlocked, green) or • (locked, gray) indicators.
- **Next Level Preview** — Shows what bonus the worker will have at the next level.
- **Lifetime Stats** — Tracks and displays cumulative earnings (Negotiator), time saved (Trading Cart), or overhead saved (Bookkeeper).
- **Hire/Fire Buttons** — Context-sensitive buttons appear in the detail panel based on worker hire status.

### Worker System Improvements
- **Config Integration** — All worker bonuses, hire costs, and level caps now read from mod config instead of hardcoded values.
- **Lifetime Bonus Tracking** — Each worker tracks their cumulative contribution (saved to NBT).
- **XP Return Value** — addXp() now returns levels gained for triggering level-up logic.
- **Robust NBT Loading** — Worker deserialization wrapped in try/catch for resilience against invalid data.

## Version 0.2.0 — Economy Overhaul & Requests System

### Economy Rebalance
- **Material-Cost-Based Pricing** — Item prices now derive from actual crafting ingredient costs with recursive resolution, replacing the old flat value tiers.
- **PriceCalculator Overhaul** — Complete rewrite of the pricing engine using MaterialValues.java as the single source of truth for raw material prices.
- **Crafting Tax & Shipping Fees** — Players can set crafting tax and shipping fees per Trading Bin through the new Config tab.
- **Town Need Multipliers** — Dynamic price scaling based on town demand levels (Desperate → Oversaturated).

### Trading Bin Price Config Tab
- **New Config Tab** — Trading Bins now have a Bin/Config tab system for managing price modifiers.
- **8 Modifier Fields** — Crafting Tax, Shipping Fee, Tool Modifier, Armor Modifier, Weapon Modifier, Food Modifier, Potion Modifier, and Enchanted Modifier.
- **Checkbox Toggles** — Enable/disable individual modifiers with visual checkboxes.
- **Live Preview** — Price calculations update in real-time as settings are adjusted.
- **Per-Bin Settings** — Each Trading Bin stores its own config in NBT via the extended UpdateBinSettingsPacket (now 12 fields).

### Shipment & Order Improvements
- **Order Cancel & Return** — Players can cancel active shipments; unsold items are returned instead of force-sold.
- **Auto-Return After Market Time** — Unsold items automatically return after max market time (8 minutes) instead of being force-sold at a loss.
- **Partial Sales** — Items can sell partially at market; earnings are tracked per-item with the new ShipmentItem.pricePerItem field.
- **Price Adjustment** — AdjustShipmentPricePacket allows in-flight price changes on active shipments.
- **Earnings in Returns** — Activity tab shows partial sale earnings alongside returned items.

### Towns Tab Redesign
- **List + Detail View** — Towns tab now shows a scrollable town list on the left page and detailed town info on the right page.
- **Interactive Town List** — Hover highlighting and click-to-select for browsing towns.
- **Scrollable Navigation** — Mouse wheel scrolling through the town list when there are more towns than visible rows.
- **Town Detail Panel** — Right page shows town name, type, distance, reputation, and key trade info for the selected town.
- **Diplomat Selection Removed** — Item selection for diplomat requests has been moved to the Requests tab (see below).

### Requests System Overhaul (formerly Diplomat)
The old diplomat item selection (limited to town surplus) has been completely replaced with a universal item request system.

#### Backend
- **Request Any Item** — Players can now request ANY item in any quantity, not just items from a town's surplus.
- **Supply Scoring** — DiplomatRequest.getSupplyScore() evaluates each town's ability to fulfill a request (surplus=95, specialty=80, neutral=45, needs=25).
- **Auto-Town Selection** — findBestTownForItem() automatically picks the best town based on supply score.
- **Fulfillment Chance** — Towns have a percentage chance to successfully fulfill requests based on their supply score tier (95%/85%/60%/35%).
- **Score-Based Premium** — Price premium scales with how difficult the request is for the selected town (1.0x–1.8x).
- **CreateRequestPacket** — New network packet for submitting item requests from the UI.

#### UI (Requests Tab)
- **Item Search** — Type to search/filter through all registered items using the new EditBox search field.
- **Scrollable Results** — Browse filtered items in a 6-row list with mouse wheel scrolling.
- **Confirmation View** — After selecting an item, see the item name, quantity selector ([-] / [+]), estimated cost, best town, and fulfillment chance percentage (color-coded green/yellow/red).
- **New Request Button** — Click + New Request to enter creation mode; Send/Cancel buttons for confirmation.
- **Keyboard Support** — Full keyboard input for search, Escape to exit creation mode, prevents accidental screen close while typing.

### Travel Time for Quest Rewards
- **DELIVERING Status** — Quests now have a new DELIVERING phase after all items are delivered: rewards travel back from the town before being collected.
- **Travel Time Calculation** — Reward travel time is based on town distance × trading cart speed multiplier, matching the shipment travel system.
- **Automatic Reward Payout** — When the travel timer expires, rewards (bonus coins, trader XP, reputation) are automatically paid out with a notification.
- **UI Indicators** — Quests in DELIVERING status show a ⇨ Xm Ys countdown in the status column with an orange color.
- **Tooltip Details** — Hovering DELIVERING quests shows Rewards en route with remaining time and Rewards will arrive automatically.
- **Backward Compatibility** — Legacy save data with DELIVERING quests that lack arrival time are auto-completed on load.

### Technical Changes
- Quest.java: Added DELIVERING status enum, rewardArrivalTime field with NBT save/load, getRewardTicksRemaining(), deliver() now transitions to DELIVERING instead of COMPLETED
- TradingPostBlockEntity.java: Added formatTravelTime() helper, DELIVERING→COMPLETED tick processing with reward payout, modified deliverQuestItems() for deferred rewards
- TradingPostScreen.java: Requests creation UI (∼300 lines), Towns tab cleanup (∼100 lines removed), quest DELIVERING status rendering and tooltips
- DiplomatRequest.java: Added supplyScore field, getSupplyScore(), getScoreBasedPremium(), getFulfillmentChance()
- CreateRequestPacket.java: New network packet for item request submission
- ModNetwork.java: Registered CreateRequestPacket
- UpdateBinSettingsPacket: Extended from 4 to 12 fields for Trading Bin config

---

## Version 0.1.5 — Market Intelligence

### UX Improvements

#### Trading Bin
- **Click-to-Pickup Fix** — Clicking an already-selected Trading Bin slot now picks up the item instead of re-selecting it. First click selects a slot for pricing; clicking the same slot again deselects and lets you pick up the item normally.

### New Features

#### Need Level Indicators (Market Board)
- **Colored Demand Dots** — Each listing row now shows a colored dot (●) before the town name indicating the item's demand level in that town (red = desperate, gold = high need, yellow = moderate, green = balanced, aqua = surplus, gray = oversaturated).
- **Demand Tooltip Info** — Hovering over a listing now shows the demand level name, price multiplier percentage, and a trend arrow in the tooltip.

#### Supply/Demand Trend Arrows (Market Board)
- **Trend Tracking** — The economy system now tracks previous supply levels across daily refreshes to compute trends.
- **Visual Trend Arrows** — Listing rows show a green ▲ when demand is rising (supply falling) or a red ▼ when demand is dropping (supply rising) next to the need level dot.
- **Trend Tooltips** — Hovering over a listing shows Demand trending UP/DOWN or Demand stable in the tooltip.
- **Persistent Trend Data** — Previous supply levels are saved/loaded with town data so trends survive server restarts.

#### Economy Dashboard Enhancements
- **Market Trends Section** — The Stats panel on the Activity tab now includes a Market Trends section showing per-town summaries of how many items have rising vs. falling demand, with color-coded arrows and counts.

### Technical Changes
- Added previousSupplyLevels field and SupplyTrend enum to TownData
- Added snapshotSupplyLevels() and getTrend() methods to TownData
- SupplyDemandManager.refreshTown() now snapshots supply levels before applying daily drift
- Town data NBT save/load updated to persist previous supply levels (PrevSupplyLevels tag)
- Market Board listing layout adjusted: need dot at x=84, trend arrow at x=88, town name at x=96

---

## Version 0.1.4 — Dynamic Mod Compatibility

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
- Accept quests from the new Quests tab in the Trading Post
- Deliver requested items to earn bonus coins and trader XP
- Quests refresh each dawn and expire after 2-4 Minecraft days
- Quest rewards scale by town type (Village 1x, Town 1.3x, City 1.6x)
- Up to 5 active quests at a time

#### Workers System
- Hire workers from the new Workers tab in the Trading Post
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
- View and collect requests from the new Diplomat tab

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

