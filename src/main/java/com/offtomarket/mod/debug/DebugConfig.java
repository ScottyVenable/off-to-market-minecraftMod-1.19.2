package com.offtomarket.mod.debug;

import com.offtomarket.mod.config.ModConfig;

/**
 * Global debug variables – live-editable during a debugger session.
 * <p>
 * All fields are public, non-final, and static so they appear in the
 * debugger's "Statics" / "Globals" view and can be changed at runtime.
 * <p>
 * HOW TO USE:
 * 1. Set a breakpoint anywhere in the mod (e.g. DebugHooks.tick()).
 * 2. When the breakpoint hits, expand DebugConfig in the Variables pane.
 * 3. Edit any value — changes take effect immediately.
 * <p>
 * The "apply" methods push values into the real config / state so you
 * don't have to restart.
 */
public class DebugConfig {

    // ===================== Master Toggle =====================
    /** Master debug mode toggle. Controls HUD overlay, logging, etc. */
    public static boolean DEBUG_ENABLED = false;

    /** Show the debug HUD overlay on screen */
    public static boolean SHOW_DEBUG_HUD = false;

    /** Draw menu layout guides / bounds for UI alignment debugging */
    public static boolean SHOW_UI_BOUNDS = false;

    /** Enable verbose console logging for trade events */
    public static boolean VERBOSE_LOGGING = false;

    // ===================== Timing Overrides =====================
    /** Override: ticks before bin items are picked up (0 = use config) */
    public static int OVERRIDE_PICKUP_DELAY = 0;

    /** Override: travel ticks per distance unit (0 = use config) */
    public static int OVERRIDE_TICKS_PER_DISTANCE = 0;

    /** Override: ticks between sale chance checks (0 = use config) */
    public static int OVERRIDE_SALE_CHECK_INTERVAL = 0;

    /** Override: base sale chance 0.0-1.0 (0 = use config) */
    public static double OVERRIDE_BASE_SALE_CHANCE = 0.0;

    /** Override: max ticks items stay at market (0 = use config) */
    public static int OVERRIDE_MAX_MARKET_TIME = 0;

    /** Override: sale chance escalation multiplier (0 = use config) */
    public static double OVERRIDE_SALE_CHANCE_ESCALATION = 0.0;

    // ===================== Economy Overrides =====================
    /**
     * @deprecated Legacy: no longer used by PriceCalculator.
     * NeedLevel.getPriceMultiplier() handles graduated need/surplus pricing.
     * Kept for backward compat with /otm debug display.
     */
    @Deprecated
    public static double OVERRIDE_NEED_BONUS = 0.0;

    /**
     * @deprecated Legacy: no longer used by PriceCalculator.
     * NeedLevel.getPriceMultiplier() handles graduated need/surplus pricing.
     * Kept for backward compat with /otm debug display.
     */
    @Deprecated
    public static double OVERRIDE_SURPLUS_PENALTY = 0.0;

    /** Override: overprice threshold (0 = use config) */
    public static double OVERRIDE_OVERPRICE_THRESHOLD = 0.0;

    /** Override: XP per sale (0 = use config) */
    public static int OVERRIDE_XP_PER_SALE = 0;

    /** Override: base XP to level up (0 = use config) */
    public static int OVERRIDE_BASE_XP_TO_LEVEL = 0;

    /** Override: max trader level (0 = use config) */
    public static int OVERRIDE_MAX_TRADER_LEVEL = 0;

    /** Override: bin search radius (0 = use config) */
    public static int OVERRIDE_BIN_SEARCH_RADIUS = 0;

    /** Override: gold only mode (-1 = use config, 0 = off, 1 = on) */
    public static int OVERRIDE_GOLD_ONLY_MODE = -1;

    // ===================== Instant Cheats =====================
    /** When set > 0, grants this many coins on next tick then resets to 0 */
    public static int GRANT_COINS = 0;

    /** When set > 0, grants this much trader XP on next tick then resets to 0 */
    public static int GRANT_XP = 0;

    /** When set > 0, sets trader level directly on next tick then resets to 0 */
    public static int SET_TRADER_LEVEL = 0;

    /** When set true, forces instant delivery of all in-transit shipments */
    public static boolean INSTANT_DELIVERY = false;

    /** When set true, forces all at-market items to sell immediately */
    public static boolean INSTANT_SELL = false;

    /** When set true, skip pickup delay (items ship immediately) */
    public static boolean SKIP_PICKUP_DELAY = false;

    /** When set true, allows unlimited market board refreshes (no cooldown) */
    public static boolean UNLIMITED_REFRESHES = false;

    // ===================== State Inspection =====================
    /** Read-only: last tick's active shipment count (updated by DebugHooks) */
    public static int WATCH_ACTIVE_SHIPMENTS = 0;

    /** Read-only: last tick's pending coins (updated by DebugHooks) */
    public static int WATCH_PENDING_COINS = 0;

    /** Read-only: last tick's trader level (updated by DebugHooks) */
    public static int WATCH_TRADER_LEVEL = 0;

    /** Read-only: last tick's trader XP (updated by DebugHooks) */
    public static int WATCH_TRADER_XP = 0;

    /** Read-only: current sale check timer value */
    public static int WATCH_SALE_TIMER = 0;

    /** Read-only: ticks since server start (game time) */
    public static long WATCH_GAME_TIME = 0;

    /** Read-only: server TPS approximation */
    public static double WATCH_SERVER_TPS = 20.0;

    /** Read-only: last error/event message */
    public static String WATCH_LAST_EVENT = "none";

    /** Read-only: number of active quests on the nearest Trading Post */
    public static int WATCH_ACTIVE_QUEST_COUNT = 0;
    /** Read-only: last day quests were refreshed on the nearest Trading Post */
    public static long WATCH_LAST_QUEST_REFRESH_DAY = -1;
    /** Read-only: selected town ID on the nearest Trading Post */
    public static String WATCH_SELECTED_TOWN_ID = "none";
    /** Read-only: balance of the nearest Finance Table (copper pieces) */
    public static int WATCH_FINANCE_TABLE_BALANCE = 0;
    /** Read-only: registry name of the last item evaluated by PriceCalculator */
    public static String WATCH_LAST_PRICE_ITEM = "none";
    /** Read-only: base CP value returned by PriceCalculator for the last item */
    public static int WATCH_LAST_PRICE_VALUE = 0;
    /** Read-only: town ID used in the most recent Quest.generateQuests() call */
    public static String WATCH_QUEST_GEN_TOWN = "none";
    /** Read-only: number of needs found in the most recent Quest.generateQuests() call */
    public static int WATCH_QUEST_GEN_NEEDS = 0;
    /** Read-only: number of surplus items found in the most recent Quest.generateQuests() call */
    public static int WATCH_QUEST_GEN_SURPLUS = 0;

    // ===================== Config Apply =====================

    /**
     * Get the effective value for a timing/economy setting:
     * returns the debug override if set, otherwise the config value.
     */
    public static int getPickupDelay() {
        return OVERRIDE_PICKUP_DELAY > 0 ? OVERRIDE_PICKUP_DELAY : ModConfig.pickupDelayTicks;
    }

    public static int getTicksPerDistance() {
        return OVERRIDE_TICKS_PER_DISTANCE > 0 ? OVERRIDE_TICKS_PER_DISTANCE : ModConfig.ticksPerDistance;
    }

    public static int getSaleCheckInterval() {
        return OVERRIDE_SALE_CHECK_INTERVAL > 0 ? OVERRIDE_SALE_CHECK_INTERVAL : ModConfig.saleCheckInterval;
    }

    public static double getBaseSaleChance() {
        return OVERRIDE_BASE_SALE_CHANCE > 0.0 ? OVERRIDE_BASE_SALE_CHANCE : ModConfig.baseSaleChance;
    }

    public static int getMaxMarketTime() {
        return OVERRIDE_MAX_MARKET_TIME > 0 ? OVERRIDE_MAX_MARKET_TIME : ModConfig.maxMarketTimeTicks;
    }

    public static double getSaleChanceEscalation() {
        return OVERRIDE_SALE_CHANCE_ESCALATION > 0.0 ? OVERRIDE_SALE_CHANCE_ESCALATION : ModConfig.saleChanceEscalation;
    }

    /** @deprecated Legacy: replaced by NeedLevel graduated system. */
    @Deprecated
    public static double getNeedBonus() {
        return OVERRIDE_NEED_BONUS > 0.0 ? OVERRIDE_NEED_BONUS : ModConfig.needBonus;
    }

    /** @deprecated Legacy: replaced by NeedLevel graduated system. */
    @Deprecated
    public static double getSurplusPenalty() {
        return OVERRIDE_SURPLUS_PENALTY > 0.0 ? OVERRIDE_SURPLUS_PENALTY : ModConfig.surplusPenalty;
    }

    public static double getOverpriceThreshold() {
        return OVERRIDE_OVERPRICE_THRESHOLD > 0.0 ? OVERRIDE_OVERPRICE_THRESHOLD : ModConfig.overpriceThreshold;
    }

    public static int getXpPerSale() {
        return OVERRIDE_XP_PER_SALE > 0 ? OVERRIDE_XP_PER_SALE : ModConfig.xpPerSale;
    }

    public static int getBaseXpToLevel() {
        return OVERRIDE_BASE_XP_TO_LEVEL > 0 ? OVERRIDE_BASE_XP_TO_LEVEL : ModConfig.baseXpToLevel;
    }

    public static int getMaxTraderLevel() {
        return OVERRIDE_MAX_TRADER_LEVEL > 0 ? OVERRIDE_MAX_TRADER_LEVEL : ModConfig.maxTraderLevel;
    }

    public static int getBinSearchRadius() {
        return OVERRIDE_BIN_SEARCH_RADIUS > 0 ? OVERRIDE_BIN_SEARCH_RADIUS : ModConfig.binSearchRadius;
    }

    public static boolean isGoldOnlyMode() {
        if (OVERRIDE_GOLD_ONLY_MODE >= 0) return OVERRIDE_GOLD_ONLY_MODE == 1;
        return ModConfig.goldOnlyMode;
    }

    /**
     * Push all non-zero overrides into the live ModConfig fields.
     * Called by /otm debug apply or when you want overrides to persist.
     */
    public static void applyOverridesToConfig() {
        if (OVERRIDE_PICKUP_DELAY > 0)          ModConfig.pickupDelayTicks   = OVERRIDE_PICKUP_DELAY;
        if (OVERRIDE_TICKS_PER_DISTANCE > 0)    ModConfig.ticksPerDistance   = OVERRIDE_TICKS_PER_DISTANCE;
        if (OVERRIDE_SALE_CHECK_INTERVAL > 0)    ModConfig.saleCheckInterval = OVERRIDE_SALE_CHECK_INTERVAL;
        if (OVERRIDE_BASE_SALE_CHANCE > 0.0)     ModConfig.baseSaleChance    = OVERRIDE_BASE_SALE_CHANCE;
        if (OVERRIDE_MAX_MARKET_TIME > 0)             ModConfig.maxMarketTimeTicks = OVERRIDE_MAX_MARKET_TIME;
        if (OVERRIDE_SALE_CHANCE_ESCALATION > 0.0)    ModConfig.saleChanceEscalation = OVERRIDE_SALE_CHANCE_ESCALATION;
        if (OVERRIDE_NEED_BONUS > 0.0)           ModConfig.needBonus         = OVERRIDE_NEED_BONUS;
        if (OVERRIDE_SURPLUS_PENALTY > 0.0)      ModConfig.surplusPenalty    = OVERRIDE_SURPLUS_PENALTY;
        if (OVERRIDE_OVERPRICE_THRESHOLD > 0.0)  ModConfig.overpriceThreshold = OVERRIDE_OVERPRICE_THRESHOLD;
        if (OVERRIDE_XP_PER_SALE > 0)            ModConfig.xpPerSale         = OVERRIDE_XP_PER_SALE;
        if (OVERRIDE_BASE_XP_TO_LEVEL > 0)       ModConfig.baseXpToLevel     = OVERRIDE_BASE_XP_TO_LEVEL;
        if (OVERRIDE_MAX_TRADER_LEVEL > 0)        ModConfig.maxTraderLevel    = OVERRIDE_MAX_TRADER_LEVEL;
        if (OVERRIDE_BIN_SEARCH_RADIUS > 0)      ModConfig.binSearchRadius   = OVERRIDE_BIN_SEARCH_RADIUS;
        if (OVERRIDE_GOLD_ONLY_MODE >= 0)         ModConfig.goldOnlyMode      = OVERRIDE_GOLD_ONLY_MODE == 1;

        WATCH_LAST_EVENT = "Overrides applied to ModConfig";
    }

    /**
     * Reset all overrides back to 0 (use config defaults).
     */
    public static void resetOverrides() {
        OVERRIDE_PICKUP_DELAY = 0;
        OVERRIDE_TICKS_PER_DISTANCE = 0;
        OVERRIDE_SALE_CHECK_INTERVAL = 0;
        OVERRIDE_BASE_SALE_CHANCE = 0.0;
        OVERRIDE_MAX_MARKET_TIME = 0;
        OVERRIDE_SALE_CHANCE_ESCALATION = 0.0;
        OVERRIDE_NEED_BONUS = 0.0;
        OVERRIDE_SURPLUS_PENALTY = 0.0;
        OVERRIDE_OVERPRICE_THRESHOLD = 0.0;
        OVERRIDE_XP_PER_SALE = 0;
        OVERRIDE_BASE_XP_TO_LEVEL = 0;
        OVERRIDE_MAX_TRADER_LEVEL = 0;
        OVERRIDE_BIN_SEARCH_RADIUS = 0;
        OVERRIDE_GOLD_ONLY_MODE = -1;
        GRANT_COINS = 0;
        GRANT_XP = 0;
        SET_TRADER_LEVEL = 0;
        INSTANT_DELIVERY = false;
        INSTANT_SELL = false;
        SKIP_PICKUP_DELAY = false;

        WATCH_LAST_EVENT = "All overrides reset";
    }
}
