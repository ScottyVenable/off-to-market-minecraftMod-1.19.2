package com.offtomarket.mod.config;

import net.minecraftforge.common.ForgeConfigSpec;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.config.ModConfigEvent;
import com.offtomarket.mod.OffToMarket;

@Mod.EventBusSubscriber(modid = OffToMarket.MODID, bus = Mod.EventBusSubscriber.Bus.MOD)
public class ModConfig {
    private static final ForgeConfigSpec.Builder BUILDER = new ForgeConfigSpec.Builder();

    // ==================== TIMING ====================
    static {
        BUILDER.comment("Timing Settings - Control how long things take").push("timing");
    }

    private static final ForgeConfigSpec.IntValue PICKUP_DELAY_TICKS = BUILDER
            .comment("Ticks before items are picked up from the Trading Bin after sending (20 ticks = 1 second)")
            .defineInRange("pickupDelayTicks", 600, 100, 24000);

    private static final ForgeConfigSpec.IntValue TICKS_PER_DISTANCE = BUILDER
            .comment("Travel ticks per distance unit (1200 = 1 minute per distance)")
            .defineInRange("ticksPerDistance", 1200, 200, 24000);

    private static final ForgeConfigSpec.IntValue SALE_CHECK_INTERVAL = BUILDER
            .comment("Ticks between sale chance checks for market listings")
            .defineInRange("saleCheckInterval", 100, 20, 2400);

    private static final ForgeConfigSpec.IntValue MAX_MARKET_TIME_TICKS = BUILDER
            .comment("Maximum ticks items stay at market before auto-cancel returns unsold items (9600 = 8 minutes)")
            .defineInRange("maxMarketTimeTicks", 9600, 600, 72000);

    static { BUILDER.pop(); }

    // ==================== PRICING ====================
    static {
        BUILDER.comment("Pricing Settings - Control how prices are calculated").push("pricing");
    }

    private static final ForgeConfigSpec.DoubleValue BASE_SALE_CHANCE = BUILDER
            .comment("Base chance (0-1) an item sells each check interval")
            .defineInRange("baseSaleChance", 0.2, 0.01, 1.0);

    private static final ForgeConfigSpec.DoubleValue SALE_CHANCE_ESCALATION = BUILDER
            .comment("Multiplier applied to sale chance as items approach max market time")
            .defineInRange("saleChanceEscalation", 2.5, 1.0, 10.0);

    private static final ForgeConfigSpec.DoubleValue NEED_BONUS = BUILDER
            .comment("Price multiplier bonus for items a town needs (1.5 = 50% bonus)")
            .defineInRange("needBonus", 1.5, 1.0, 5.0);

    private static final ForgeConfigSpec.DoubleValue SURPLUS_PENALTY = BUILDER
            .comment("Price multiplier penalty for items a town has surplus of (0.7 = 30% less)")
            .defineInRange("surplusPenalty", 0.7, 0.1, 1.0);

    private static final ForgeConfigSpec.DoubleValue OVERPRICE_THRESHOLD = BUILDER
            .comment("If player price exceeds fair value * this, sell speed decreases")
            .defineInRange("overpriceThreshold", 1.5, 1.0, 5.0);

    private static final ForgeConfigSpec.DoubleValue GLOBAL_PRICE_MULTIPLIER = BUILDER
            .comment("Global multiplier for all prices (2.0 = everything costs/sells for double)")
            .defineInRange("globalPriceMultiplier", 1.0, 0.1, 10.0);

    static { BUILDER.pop(); }

    // ==================== CURRENCY ====================
    static {
        BUILDER.comment("Currency Settings - Control the coin system").push("currency");
    }

    private static final ForgeConfigSpec.BooleanValue GOLD_ONLY_MODE = BUILDER
            .comment("Gold Only mode: disables silver and copper coins. All prices are in gold pieces.")
            .define("goldOnlyMode", false);

    private static final ForgeConfigSpec.IntValue COPPER_PER_SILVER = BUILDER
            .comment("How many copper coins equal one silver coin")
            .defineInRange("copperPerSilver", 10, 1, 100);

    private static final ForgeConfigSpec.IntValue SILVER_PER_GOLD = BUILDER
            .comment("How many silver coins equal one gold coin")
            .defineInRange("silverPerGold", 10, 1, 100);

    static { BUILDER.pop(); }

    // ==================== LEVELING ====================
    static {
        BUILDER.comment("Leveling Settings - Control trader progression").push("leveling");
    }

    private static final ForgeConfigSpec.IntValue XP_PER_SALE = BUILDER
            .comment("Trader XP gained per successful sale")
            .defineInRange("xpPerSale", 10, 1, 1000);

    private static final ForgeConfigSpec.IntValue BASE_XP_TO_LEVEL = BUILDER
            .comment("Base XP needed for first level up (scales per level)")
            .defineInRange("baseXpToLevel", 100, 10, 10000);

    private static final ForgeConfigSpec.IntValue MAX_TRADER_LEVEL = BUILDER
            .comment("Maximum trader level")
            .defineInRange("maxTraderLevel", 5, 1, 20);

    private static final ForgeConfigSpec.DoubleValue XP_LEVEL_SCALING = BUILDER
            .comment("XP requirement multiplier per level (1.5 = 50% more XP needed each level)")
            .defineInRange("xpLevelScaling", 1.5, 1.0, 5.0);

    static { BUILDER.pop(); }

    // ==================== QUESTS ====================
    static {
        BUILDER.comment("Quest Settings - Control the quest system").push("quests");
    }

    private static final ForgeConfigSpec.IntValue MAX_ACTIVE_QUESTS = BUILDER
            .comment("Maximum number of active quests at once")
            .defineInRange("maxActiveQuests", 5, 1, 20);

    private static final ForgeConfigSpec.IntValue QUEST_REFRESH_HOUR = BUILDER
            .comment("Minecraft hour (0-23) when new quests are generated")
            .defineInRange("questRefreshHour", 6, 0, 23);

    private static final ForgeConfigSpec.DoubleValue QUEST_REWARD_MULTIPLIER = BUILDER
            .comment("Multiplier for all quest rewards")
            .defineInRange("questRewardMultiplier", 1.0, 0.1, 10.0);

    private static final ForgeConfigSpec.IntValue QUEST_EXPIRY_DAYS = BUILDER
            .comment("Minecraft days until quests expire")
            .defineInRange("questExpiryDays", 3, 1, 14);

    static { BUILDER.pop(); }

    // ==================== WORKERS ====================
    static {
        BUILDER.comment("Worker Settings - Control hired workers").push("workers");
    }

    private static final ForgeConfigSpec.IntValue NEGOTIATOR_HIRE_COST = BUILDER
            .comment("Cost in copper to hire a Negotiator")
            .defineInRange("negotiatorHireCost", 1300, 0, 50000);

    private static final ForgeConfigSpec.IntValue CART_HIRE_COST = BUILDER
            .comment("Cost in copper to hire a Trading Cart")
            .defineInRange("cartHireCost", 780, 0, 50000);

    private static final ForgeConfigSpec.IntValue MAX_WORKER_LEVEL = BUILDER
            .comment("Maximum level workers can reach")
            .defineInRange("maxWorkerLevel", 10, 1, 50);

    private static final ForgeConfigSpec.DoubleValue NEGOTIATOR_MAX_BONUS = BUILDER
            .comment("Maximum price bonus from a max-level Negotiator (0.25 = 25%)")
            .defineInRange("negotiatorMaxBonus", 0.25, 0.0, 1.0);

    private static final ForgeConfigSpec.DoubleValue CART_MAX_SPEED_BONUS = BUILDER
            .comment("Maximum travel time reduction from a max-level Cart (0.4 = 40% faster)")
            .defineInRange("cartMaxSpeedBonus", 0.4, 0.0, 0.9);

    private static final ForgeConfigSpec.IntValue BOOKKEEPER_HIRE_COST = BUILDER
            .comment("Cost in copper to hire a Bookkeeper")
            .defineInRange("bookkeeperHireCost", 1040, 0, 50000);

    private static final ForgeConfigSpec.DoubleValue BOOKKEEPER_MAX_COST_REDUCTION = BUILDER
            .comment("Maximum worker cost reduction from a max-level Bookkeeper (0.35 = 35%)")
            .defineInRange("bookkeeperMaxCostReduction", 0.35, 0.0, 0.95);

    static { BUILDER.pop(); }

    // ==================== DIPLOMATS ====================
    static {
        BUILDER.comment("Diplomat Settings - Control the diplomat request system").push("diplomats");
    }

    private static final ForgeConfigSpec.DoubleValue DIPLOMAT_BASE_PREMIUM = BUILDER
            .comment("Base premium multiplier for diplomat requests (1.5 = 50% more)")
            .defineInRange("diplomatBasePremium", 1.5, 1.0, 5.0);

    private static final ForgeConfigSpec.DoubleValue DIPLOMAT_TRAVEL_MULTIPLIER = BUILDER
            .comment("Travel time multiplier for diplomat requests (1.5 = 50% longer)")
            .defineInRange("diplomatTravelMultiplier", 1.5, 1.0, 5.0);

    static { BUILDER.pop(); }

    // ==================== ANIMALS ====================
    static {
        BUILDER.comment("Animal Trading Settings - Control the animal trade slip system").push("animals");
    }

    private static final ForgeConfigSpec.BooleanValue ENABLE_ANIMAL_TRADING = BUILDER
            .comment("Enable the animal trade slip system")
            .define("enableAnimalTrading", true);

    private static final ForgeConfigSpec.DoubleValue ANIMAL_PRICE_MULTIPLIER = BUILDER
            .comment("Multiplier for all animal prices")
            .defineInRange("animalPriceMultiplier", 1.0, 0.1, 10.0);

    private static final ForgeConfigSpec.BooleanValue SPAWN_TAMED_ANIMALS = BUILDER
            .comment("Whether animals from trade slips spawn already tamed (if tameable)")
            .define("spawnTamedAnimals", true);

    static { BUILDER.pop(); }

    // ==================== MOD COMPATIBILITY ====================
    static {
        BUILDER.comment("Mod Compatibility Settings - Control integration with other mods").push("modCompat");
    }

    private static final ForgeConfigSpec.BooleanValue ENABLE_DYNAMIC_TOWNS = BUILDER
            .comment("Enable auto-generated towns based on installed mods")
            .define("enableDynamicTowns", true);

    private static final ForgeConfigSpec.BooleanValue LOG_DISCOVERED_ITEMS = BUILDER
            .comment("Log discovered mod items to console (useful for debugging)")
            .define("logDiscoveredItems", false);

    private static final ForgeConfigSpec.IntValue MAX_ITEMS_PER_CATEGORY = BUILDER
            .comment("Maximum items to add per category to dynamic towns")
            .defineInRange("maxItemsPerCategory", 50, 10, 500);

    static { BUILDER.pop(); }

    // ==================== SUPPLY & DEMAND ====================
    static {
        BUILDER.comment("Supply & Demand Settings - Control market dynamics").push("supplyDemand");
    }

    private static final ForgeConfigSpec.DoubleValue DAILY_REFRESH_CHANCE = BUILDER
            .comment("Chance per item per in-game day that supply drifts toward equilibrium")
            .defineInRange("dailyRefreshChance", 0.30, 0.0, 1.0);

    private static final ForgeConfigSpec.IntValue SUPPLY_DRIFT_AMOUNT = BUILDER
            .comment("How many supply units drift per daily refresh tick")
            .defineInRange("supplyDriftAmount", 5, 1, 50);

    static { BUILDER.pop(); }

    // ==================== TRADING BIN ====================
    static {
        BUILDER.comment("Trading Bin Settings - Control bin behavior").push("tradingLedger");
    }

    private static final ForgeConfigSpec.IntValue BIN_SEARCH_RADIUS = BUILDER
            .comment("Radius in blocks to search for a Trading Bin from a Trading Post")
            .defineInRange("binSearchRadius", 5, 1, 16);

    private static final ForgeConfigSpec.IntValue DEFAULT_CRAFTING_TAX = BUILDER
            .comment("Default crafting tax percentage for new Trading Bins")
            .defineInRange("defaultCraftingTaxPercent", 15, 0, 100);

    private static final ForgeConfigSpec.IntValue DEFAULT_MIN_MARKUP = BUILDER
            .comment("Default minimum markup percentage for new Trading Bins")
            .defineInRange("defaultMinMarkupPercent", 0, 0, 200);

    static { BUILDER.pop(); }

    // ==================== DEBUG ====================
    static {
        BUILDER.comment("Debug Settings - For testing and troubleshooting").push("debug");
    }

    private static final ForgeConfigSpec.BooleanValue DEBUG_MODE = BUILDER
            .comment("Enable debug mode (extra logging and /offtomarket debug commands)")
            .define("debugMode", false);

    private static final ForgeConfigSpec.BooleanValue INSTANT_TRAVEL = BUILDER
            .comment("Debug: Make all shipments arrive instantly")
            .define("instantTravel", false);

    private static final ForgeConfigSpec.BooleanValue FREE_PURCHASES = BUILDER
            .comment("Debug: All purchases are free")
            .define("freePurchases", false);

    static { BUILDER.pop(); }

    public static final ForgeConfigSpec SPEC = BUILDER.build();

    // ==================== RUNTIME VALUES ====================
    // Timing
    public static int pickupDelayTicks = 600;
    public static int ticksPerDistance = 1200;
    public static int saleCheckInterval = 100;
    public static int maxMarketTimeTicks = 9600;

    // Pricing
    public static double baseSaleChance = 0.2;
    public static double saleChanceEscalation = 2.5;
    public static double needBonus = 1.5;
    public static double surplusPenalty = 0.7;
    public static double overpriceThreshold = 1.5;
    public static double globalPriceMultiplier = 1.0;

    // Currency
    public static boolean goldOnlyMode = false;
    public static int copperPerSilver = 10;
    public static int silverPerGold = 10;

    // Leveling
    public static int xpPerSale = 10;
    public static int baseXpToLevel = 100;
    public static int maxTraderLevel = 5;
    public static double xpLevelScaling = 1.5;

    // Quests
    public static int maxActiveQuests = 5;
    public static int questRefreshHour = 6;
    public static double questRewardMultiplier = 1.0;
    public static int questExpiryDays = 3;

    // Workers
    public static int negotiatorHireCost = 1300;
    public static int cartHireCost = 780;
    public static int maxWorkerLevel = 10;
    public static double negotiatorMaxBonus = 0.25;
    public static double cartMaxSpeedBonus = 0.4;
    public static int bookkeeperHireCost = 1040;
    public static double bookkeeperMaxCostReduction = 0.35;

    // Diplomats
    public static double diplomatBasePremium = 1.5;
    public static double diplomatTravelMultiplier = 1.5;

    // Animals
    public static boolean enableAnimalTrading = true;
    public static double animalPriceMultiplier = 1.0;
    public static boolean spawnTamedAnimals = true;

    // Mod Compatibility
    public static boolean enableDynamicTowns = true;
    public static boolean logDiscoveredItems = false;
    public static int maxItemsPerCategory = 50;

    // Supply & Demand
    public static double dailyRefreshChance = 0.30;
    public static int supplyDriftAmount = 5;

    // Trading Bin
    public static int binSearchRadius = 5;
    public static int defaultCraftingTaxPercent = 15;
    public static int defaultMinMarkupPercent = 0;

    // Debug
    public static boolean debugMode = false;
    public static boolean instantTravel = false;
    public static boolean freePurchases = false;

    @SubscribeEvent
    static void onLoad(final ModConfigEvent event) {
        // Timing
        pickupDelayTicks = PICKUP_DELAY_TICKS.get();
        ticksPerDistance = TICKS_PER_DISTANCE.get();
        saleCheckInterval = SALE_CHECK_INTERVAL.get();
        maxMarketTimeTicks = MAX_MARKET_TIME_TICKS.get();

        // Pricing
        baseSaleChance = BASE_SALE_CHANCE.get();
        saleChanceEscalation = SALE_CHANCE_ESCALATION.get();
        needBonus = NEED_BONUS.get();
        surplusPenalty = SURPLUS_PENALTY.get();
        overpriceThreshold = OVERPRICE_THRESHOLD.get();
        globalPriceMultiplier = GLOBAL_PRICE_MULTIPLIER.get();

        // Currency
        goldOnlyMode = GOLD_ONLY_MODE.get();
        copperPerSilver = COPPER_PER_SILVER.get();
        silverPerGold = SILVER_PER_GOLD.get();

        // Leveling
        xpPerSale = XP_PER_SALE.get();
        baseXpToLevel = BASE_XP_TO_LEVEL.get();
        maxTraderLevel = MAX_TRADER_LEVEL.get();
        xpLevelScaling = XP_LEVEL_SCALING.get();

        // Quests
        maxActiveQuests = MAX_ACTIVE_QUESTS.get();
        questRefreshHour = QUEST_REFRESH_HOUR.get();
        questRewardMultiplier = QUEST_REWARD_MULTIPLIER.get();
        questExpiryDays = QUEST_EXPIRY_DAYS.get();

        // Workers
        negotiatorHireCost = NEGOTIATOR_HIRE_COST.get();
        cartHireCost = CART_HIRE_COST.get();
        maxWorkerLevel = MAX_WORKER_LEVEL.get();
        negotiatorMaxBonus = NEGOTIATOR_MAX_BONUS.get();
        cartMaxSpeedBonus = CART_MAX_SPEED_BONUS.get();
        bookkeeperHireCost = BOOKKEEPER_HIRE_COST.get();
        bookkeeperMaxCostReduction = BOOKKEEPER_MAX_COST_REDUCTION.get();

        // Diplomats
        diplomatBasePremium = DIPLOMAT_BASE_PREMIUM.get();
        diplomatTravelMultiplier = DIPLOMAT_TRAVEL_MULTIPLIER.get();

        // Animals
        enableAnimalTrading = ENABLE_ANIMAL_TRADING.get();
        animalPriceMultiplier = ANIMAL_PRICE_MULTIPLIER.get();
        spawnTamedAnimals = SPAWN_TAMED_ANIMALS.get();

        // Mod Compatibility
        enableDynamicTowns = ENABLE_DYNAMIC_TOWNS.get();
        logDiscoveredItems = LOG_DISCOVERED_ITEMS.get();
        maxItemsPerCategory = MAX_ITEMS_PER_CATEGORY.get();

        // Supply & Demand
        dailyRefreshChance = DAILY_REFRESH_CHANCE.get();
        supplyDriftAmount = SUPPLY_DRIFT_AMOUNT.get();

        // Trading Bin
        binSearchRadius = BIN_SEARCH_RADIUS.get();
        defaultCraftingTaxPercent = DEFAULT_CRAFTING_TAX.get();
        defaultMinMarkupPercent = DEFAULT_MIN_MARKUP.get();

        // Debug
        debugMode = DEBUG_MODE.get();
        instantTravel = INSTANT_TRAVEL.get();
        freePurchases = FREE_PURCHASES.get();

        // Push supply config to SupplyDemandManager
        com.offtomarket.mod.data.SupplyDemandManager.setDailyRefreshChance(dailyRefreshChance);
        com.offtomarket.mod.data.SupplyDemandManager.setDriftAmount(supplyDriftAmount);
    }
}
