package com.offtomarket.mod.config;

import net.minecraftforge.common.ForgeConfigSpec;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.config.ModConfigEvent;
import com.offtomarket.mod.OffToMarket;

@Mod.EventBusSubscriber(modid = OffToMarket.MODID, bus = Mod.EventBusSubscriber.Bus.MOD)
public class ModConfig {
    private static final ForgeConfigSpec.Builder BUILDER = new ForgeConfigSpec.Builder();

    // Timing
    private static final ForgeConfigSpec.IntValue PICKUP_DELAY_TICKS = BUILDER
            .comment("Ticks before items are picked up from the Trading Bin after sending (20 ticks = 1 second)")
            .defineInRange("pickupDelayTicks", 600, 100, 24000);

    private static final ForgeConfigSpec.IntValue TICKS_PER_DISTANCE = BUILDER
            .comment("Travel ticks per distance unit (1200 = 1 minute per distance)")
            .defineInRange("ticksPerDistance", 1200, 200, 24000);

    private static final ForgeConfigSpec.IntValue SALE_CHECK_INTERVAL = BUILDER
            .comment("Ticks between sale chance checks for market listings")
            .defineInRange("saleCheckInterval", 100, 20, 2400);

    private static final ForgeConfigSpec.DoubleValue BASE_SALE_CHANCE = BUILDER
            .comment("Base chance (0-1) an item sells each check interval")
            .defineInRange("baseSaleChance", 0.2, 0.01, 1.0);

    private static final ForgeConfigSpec.IntValue MAX_MARKET_TIME_TICKS = BUILDER
            .comment("Maximum ticks items stay at market before auto-selling at 75% price (6000 = 5 minutes)")
            .defineInRange("maxMarketTimeTicks", 6000, 600, 72000);

    private static final ForgeConfigSpec.DoubleValue SALE_CHANCE_ESCALATION = BUILDER
            .comment("Multiplier applied to sale chance as items approach max market time (e.g. 2.0 = double chance at 50% of max time)")
            .defineInRange("saleChanceEscalation", 2.5, 1.0, 10.0);

    // Pricing
    private static final ForgeConfigSpec.DoubleValue NEED_BONUS = BUILDER
            .comment("Price multiplier bonus for items a town needs")
            .defineInRange("needBonus", 1.5, 1.0, 5.0);

    private static final ForgeConfigSpec.DoubleValue SURPLUS_PENALTY = BUILDER
            .comment("Price multiplier penalty for items a town already has surplus of")
            .defineInRange("surplusPenalty", 0.7, 0.1, 1.0);

    private static final ForgeConfigSpec.DoubleValue OVERPRICE_THRESHOLD = BUILDER
            .comment("If player price exceeds fair value * this, sell speed decreases")
            .defineInRange("overpriceThreshold", 1.5, 1.0, 5.0);

    // Trading Bin
    private static final ForgeConfigSpec.IntValue BIN_SEARCH_RADIUS = BUILDER
            .comment("Radius in blocks to search for a Trading Bin from a Trading Post")
            .defineInRange("binSearchRadius", 5, 1, 16);

    // Leveling
    private static final ForgeConfigSpec.IntValue XP_PER_SALE = BUILDER
            .comment("Trader XP gained per successful sale")
            .defineInRange("xpPerSale", 10, 1, 1000);

    private static final ForgeConfigSpec.IntValue BASE_XP_TO_LEVEL = BUILDER
            .comment("Base XP needed for first level up (scales per level)")
            .defineInRange("baseXpToLevel", 100, 10, 10000);

    private static final ForgeConfigSpec.IntValue MAX_TRADER_LEVEL = BUILDER
            .comment("Maximum trader level")
            .defineInRange("maxTraderLevel", 5, 1, 20);

    private static final ForgeConfigSpec.BooleanValue GOLD_ONLY_MODE = BUILDER
                .comment("Gold Only mode: disables silver and copper coins. All prices are in gold pieces.")
            .define("goldOnlyMode", false);

    // Supply & Demand
    private static final ForgeConfigSpec.DoubleValue DAILY_REFRESH_CHANCE = BUILDER
            .comment("Chance per item per in-game day that a town's supply level drifts toward equilibrium (0.0-1.0)")
            .defineInRange("dailyRefreshChance", 0.30, 0.0, 1.0);

    private static final ForgeConfigSpec.IntValue SUPPLY_DRIFT_AMOUNT = BUILDER
            .comment("How many supply units drift per daily refresh tick")
            .defineInRange("supplyDriftAmount", 5, 1, 50);

    // Default Trading Bin settings
    private static final ForgeConfigSpec.IntValue DEFAULT_CRAFTING_TAX = BUILDER
            .comment("Default crafting tax percentage for new Trading Bins")
            .defineInRange("defaultCraftingTaxPercent", 15, 0, 100);

    private static final ForgeConfigSpec.IntValue DEFAULT_MIN_MARKUP = BUILDER
            .comment("Default minimum markup percentage for new Trading Bins")
            .defineInRange("defaultMinMarkupPercent", 0, 0, 200);

    public static final ForgeConfigSpec SPEC = BUILDER.build();

    // Runtime values (defaults match config spec defaults before config loads)
    public static int pickupDelayTicks = 600;
    public static int ticksPerDistance = 1200;
    public static int saleCheckInterval = 100;
    public static double baseSaleChance = 0.2;
    public static int maxMarketTimeTicks = 6000;
    public static double saleChanceEscalation = 2.5;
    public static double needBonus = 1.5;
    public static double surplusPenalty = 0.7;
    public static double overpriceThreshold = 1.5;
    public static int binSearchRadius = 5;
    public static int xpPerSale = 10;
    public static int baseXpToLevel = 100;
    public static int maxTraderLevel = 5;
    public static boolean goldOnlyMode = false;
    public static double dailyRefreshChance = 0.30;
    public static int supplyDriftAmount = 5;
    public static int defaultCraftingTaxPercent = 15;
    public static int defaultMinMarkupPercent = 0;

    @SubscribeEvent
    static void onLoad(final ModConfigEvent event) {
        pickupDelayTicks = PICKUP_DELAY_TICKS.get();
        ticksPerDistance = TICKS_PER_DISTANCE.get();
        saleCheckInterval = SALE_CHECK_INTERVAL.get();
        baseSaleChance = BASE_SALE_CHANCE.get();
        maxMarketTimeTicks = MAX_MARKET_TIME_TICKS.get();
        saleChanceEscalation = SALE_CHANCE_ESCALATION.get();
        needBonus = NEED_BONUS.get();
        surplusPenalty = SURPLUS_PENALTY.get();
        overpriceThreshold = OVERPRICE_THRESHOLD.get();
        binSearchRadius = BIN_SEARCH_RADIUS.get();
        xpPerSale = XP_PER_SALE.get();
        baseXpToLevel = BASE_XP_TO_LEVEL.get();
        maxTraderLevel = MAX_TRADER_LEVEL.get();
        goldOnlyMode = GOLD_ONLY_MODE.get();
        dailyRefreshChance = DAILY_REFRESH_CHANCE.get();
        supplyDriftAmount = SUPPLY_DRIFT_AMOUNT.get();
        defaultCraftingTaxPercent = DEFAULT_CRAFTING_TAX.get();
        defaultMinMarkupPercent = DEFAULT_MIN_MARKUP.get();

        // Push supply config to SupplyDemandManager
        com.offtomarket.mod.data.SupplyDemandManager.setDailyRefreshChance(dailyRefreshChance);
        com.offtomarket.mod.data.SupplyDemandManager.setDriftAmount(supplyDriftAmount);
    }
}
