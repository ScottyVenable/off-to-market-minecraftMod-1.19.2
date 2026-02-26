# Debug Watch Expressions — Off to Market

---

## Global Static Watches (Recommended — work from ANY breakpoint)

These fields in `DebugConfig` are updated automatically every server tick and whenever
price/quest logic fires. Add them once and they always show current values, regardless of
which class or method you are paused in.

In the WATCH panel, add each as a static field expression:

```
com.offtomarket.mod.debug.DebugConfig.WATCH_TRADER_LEVEL
com.offtomarket.mod.debug.DebugConfig.WATCH_TRADER_XP
com.offtomarket.mod.debug.DebugConfig.WATCH_PENDING_COINS
com.offtomarket.mod.debug.DebugConfig.WATCH_ACTIVE_SHIPMENTS
com.offtomarket.mod.debug.DebugConfig.WATCH_SALE_TIMER
com.offtomarket.mod.debug.DebugConfig.WATCH_GAME_TIME
com.offtomarket.mod.debug.DebugConfig.WATCH_SERVER_TPS
com.offtomarket.mod.debug.DebugConfig.WATCH_LAST_EVENT
com.offtomarket.mod.debug.DebugConfig.WATCH_ACTIVE_QUEST_COUNT
com.offtomarket.mod.debug.DebugConfig.WATCH_LAST_QUEST_REFRESH_DAY
com.offtomarket.mod.debug.DebugConfig.WATCH_SELECTED_TOWN_ID
com.offtomarket.mod.debug.DebugConfig.WATCH_FINANCE_TABLE_BALANCE
com.offtomarket.mod.debug.DebugConfig.WATCH_LAST_PRICE_ITEM
com.offtomarket.mod.debug.DebugConfig.WATCH_LAST_PRICE_VALUE
com.offtomarket.mod.debug.DebugConfig.WATCH_QUEST_GEN_TOWN
com.offtomarket.mod.debug.DebugConfig.WATCH_QUEST_GEN_NEEDS
com.offtomarket.mod.debug.DebugConfig.WATCH_QUEST_GEN_SURPLUS
```

**Update sources:**
| Field | Updated by |
|-------|-----------|
| `WATCH_TRADER_LEVEL/XP/PENDING_COINS/ACTIVE_SHIPMENTS/SALE_TIMER` | `DebugHooks.onServerTick()` each tick |
| `WATCH_GAME_TIME/SERVER_TPS` | `DebugHooks.onServerTick()` each tick |
| `WATCH_ACTIVE_QUEST_COUNT/LAST_QUEST_REFRESH_DAY/SELECTED_TOWN_ID` | `DebugHooks.onServerTick()` each tick |
| `WATCH_FINANCE_TABLE_BALANCE` | `DebugHooks.onServerTick()` each tick (when Finance Table is nearby) |
| `WATCH_LAST_PRICE_ITEM/VALUE` | `PriceCalculator.getBaseValue()` — inline, on every price lookup |
| `WATCH_QUEST_GEN_TOWN/NEEDS/SURPLUS` | `Quest.generateQuests()` — inline, each time quests are generated |

> Tip: In VS Code the statics panel may not auto-refresh. Step once (F10) after pausing
> to force a variable pane refresh.

---

Add these to the **WATCH** panel in VS Code (`Ctrl+Shift+D` → WATCH section → click `+`).
Context-bound expressions below only evaluate meaningfully when paused at a breakpoint in the relevant context.

---

## Trading Post Block Entity

**When paused inside an instance method** (most methods) — use `this`:
```
this.traderLevel
this.traderXP
this.pendingCoins
this.selectedTownId
this.activeQuests.size()
this.shipments.size()
this.activeQuests
this.shipments
this.workers
this.diplomat
this.lastQuestRefreshDay
this.worldPosition
```

**When paused inside `serverTick()`** (static method) — use `be`:
```
be.traderLevel
be.traderXP
be.pendingCoins
be.selectedTownId
be.activeQuests.size()
be.shipments.size()
be.activeQuests
be.shipments
be.lastQuestRefreshDay
```

---

## Shipment Debugging
*Useful when paused inside `processMarketSales()` or `serverTick()`*

```
shipment.getTownId()
shipment.getItems().size()
shipment.getCoinsEarned()
shipment.isComplete()
shipment.getDaysInTransit()
```

---

## Quest Generation
*Useful when paused inside `Quest.generateQuests()`*

```
town.getId()
town.getDisplayName()
town.getType()
town.getMinTraderLevel()
town.getNeeds().size()
town.getNeedLevels()
town.getNeedLevels().size()
needs
surplus
needs.size()
surplus.size()
quests.size()
maxQuests
```

---

## Quest State
*Useful when paused in quest accept/deliver logic*

```
quest.getTitle()
quest.getStatus()
quest.getTargetItem()
quest.getRequiredAmount()
quest.getDeliveredAmount()
quest.getCoinReward()
quest.getXpReward()
quest.getQuestType()
```

---

## Price Calculator
*Useful when paused inside `PriceCalculator.getBaseValue()` or `classifyByClass()`*

```
stack.getItem().getRegistryName()
stack.getItem().getClass().getSimpleName()
stack.getRarity()
stack.getItem().getFoodProperties()
result
tier
baseValue
```

---

## Town Registry & Loading
*Useful when paused inside `TownLoader`, `TownRegistry`, or `ModCompatibility`*

```
TownRegistry.getAllTowns().size()
town.getId()
town.getDisplayName()
town.getMinTraderLevel()
town.getNeedLevels().isEmpty()
town.getSpecialtyItems().size()
```

---

## Finance Table
*Useful when paused inside `FinanceTableBlockEntity`, `DepositCoinsPacket`, or `WithdrawCoinsPacket`*

```
ftbe.getBalance()
ftbe.balance
ftbe.getBlockPos()
```

---

## Market Board / NPC Listings
*Useful when paused inside `MarketListing` or `TradingPostScreen`*

```
listing.getItem().getRegistryName()
listing.getPrice()
listing.getNeedLevel()
listing.getStock()
```

---

## Network Packets
*Useful when paused in any packet `handle()` method*

```
msg.pos
player.distanceToSqr(msg.pos.getX() + 0.5, msg.pos.getY() + 0.5, msg.pos.getZ() + 0.5)
ctx.get().getSender().getScoreboardName()
```

---

## General Player State
*Useful anywhere a `Player` or `ServerPlayer` is in scope*

```
player.getInventory().getContainerSize()
player.getInventory().countItem(net.minecraft.world.item.Items.GOLD_INGOT)
player.level.dayTime
player.level.isClientSide()
player.getScoreboardName()
player.blockPosition()
```

---

## Supply & Demand
*Useful when paused inside `SupplyDemandManager`*

```
townId
currentLevel
currentLevel.name()
currentLevel.isInDemand()
currentLevel.isOversupplied()
```

---

## Tips

- Right-click any variable in the **Variables** panel while paused → **Add to Watch** (fastest method)
- Expressions evaluate in the context of the **currently selected stack frame** — switch frames in the call stack to change scope
- You can call methods in watch expressions: `town.getNeedLevels().entrySet()` works fine
- Cast when needed: `((com.offtomarket.mod.block.entity.TradingPostBlockEntity) be).traderLevel`
