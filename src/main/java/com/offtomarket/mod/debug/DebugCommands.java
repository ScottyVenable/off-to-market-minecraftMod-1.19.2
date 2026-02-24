package com.offtomarket.mod.debug;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.DoubleArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.offtomarket.mod.config.ModConfig;
import com.offtomarket.mod.data.PriceCalculator;
import net.minecraft.ChatFormatting;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

/**
 * In-game debug commands under /otm.
 * Requires OP level 2 (cheats enabled).
 */
public class DebugCommands {

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(Commands.literal("otm")
                .requires(src -> src.hasPermission(2))

                // /otm debug - toggle debug mode
                .then(Commands.literal("debug")
                        .executes(ctx -> {
                            DebugConfig.DEBUG_ENABLED = !DebugConfig.DEBUG_ENABLED;
                            ctx.getSource().sendSuccess(Component.literal(
                                    "Debug mode: " + (DebugConfig.DEBUG_ENABLED ? "ON" : "OFF"))
                                    .withStyle(DebugConfig.DEBUG_ENABLED ? ChatFormatting.GREEN : ChatFormatting.RED), true);
                            return 1;
                        })

                        // /otm debug hud
                        .then(Commands.literal("hud")
                                .executes(ctx -> {
                                    DebugConfig.SHOW_DEBUG_HUD = !DebugConfig.SHOW_DEBUG_HUD;
                                    ctx.getSource().sendSuccess(Component.literal(
                                            "Debug HUD: " + (DebugConfig.SHOW_DEBUG_HUD ? "ON" : "OFF"))
                                            .withStyle(ChatFormatting.YELLOW), true);
                                    return 1;
                                }))

                        // /otm debug verbose
                        .then(Commands.literal("verbose")
                                .executes(ctx -> {
                                    DebugConfig.VERBOSE_LOGGING = !DebugConfig.VERBOSE_LOGGING;
                                    ctx.getSource().sendSuccess(Component.literal(
                                            "Verbose logging: " + (DebugConfig.VERBOSE_LOGGING ? "ON" : "OFF"))
                                            .withStyle(ChatFormatting.YELLOW), true);
                                    return 1;
                                }))

                        // /otm debug apply
                        .then(Commands.literal("apply")
                                .executes(ctx -> {
                                    DebugConfig.applyOverridesToConfig();
                                    ctx.getSource().sendSuccess(Component.literal(
                                            "Debug overrides applied to ModConfig!")
                                            .withStyle(ChatFormatting.GREEN), true);
                                    return 1;
                                }))

                        // /otm debug reset
                        .then(Commands.literal("reset")
                                .executes(ctx -> {
                                    DebugConfig.resetOverrides();
                                    ctx.getSource().sendSuccess(Component.literal(
                                            "All debug overrides reset to defaults.")
                                            .withStyle(ChatFormatting.YELLOW), true);
                                    return 1;
                                }))
                )

                // /otm grant coins <amount>
                .then(Commands.literal("grant")
                        .then(Commands.literal("coins")
                                .then(Commands.argument("amount", IntegerArgumentType.integer(1))
                                        .executes(ctx -> {
                                            int amount = IntegerArgumentType.getInteger(ctx, "amount");
                                            DebugConfig.GRANT_COINS = amount;
                                            ctx.getSource().sendSuccess(Component.literal(
                                                    "Granting " + amount + " CP on next tick...")
                                                    .withStyle(ChatFormatting.GOLD), true);
                                            return 1;
                                        })))

                        // /otm grant xp <amount>
                        .then(Commands.literal("xp")
                                .then(Commands.argument("amount", IntegerArgumentType.integer(1))
                                        .executes(ctx -> {
                                            int amount = IntegerArgumentType.getInteger(ctx, "amount");
                                            DebugConfig.GRANT_XP = amount;
                                            ctx.getSource().sendSuccess(Component.literal(
                                                    "Granting " + amount + " XP on next tick...")
                                                    .withStyle(ChatFormatting.GREEN), true);
                                            return 1;
                                        })))
                )

                // /otm setlevel <level>
                .then(Commands.literal("setlevel")
                        .then(Commands.argument("level", IntegerArgumentType.integer(1, 20))
                                .executes(ctx -> {
                                    int lvl = IntegerArgumentType.getInteger(ctx, "level");
                                    DebugConfig.SET_TRADER_LEVEL = lvl;
                                    ctx.getSource().sendSuccess(Component.literal(
                                            "Setting trader level to " + lvl + " on next tick...")
                                            .withStyle(ChatFormatting.AQUA), true);
                                    return 1;
                                })))

                // /otm deliver
                .then(Commands.literal("deliver")
                        .executes(ctx -> {
                            DebugConfig.INSTANT_DELIVERY = true;
                            ctx.getSource().sendSuccess(Component.literal(
                                    "Forcing instant delivery of all in-transit shipments...")
                                    .withStyle(ChatFormatting.GREEN), true);
                            return 1;
                        }))

                // /otm sell
                .then(Commands.literal("sell")
                        .executes(ctx -> {
                            DebugConfig.INSTANT_SELL = true;
                            ctx.getSource().sendSuccess(Component.literal(
                                    "Forcing instant sale of all at-market items...")
                                    .withStyle(ChatFormatting.GOLD), true);
                            return 1;
                        }))

                // /otm set <key> <value> — override a config value at runtime
                .then(Commands.literal("set")
                        .then(Commands.argument("key", StringArgumentType.word())
                                .then(Commands.argument("value", DoubleArgumentType.doubleArg())
                                        .executes(ctx -> {
                                            String key = StringArgumentType.getString(ctx, "key");
                                            double val = DoubleArgumentType.getDouble(ctx, "value");
                                            String result = applySet(key, val);
                                            ctx.getSource().sendSuccess(Component.literal(result)
                                                    .withStyle(ChatFormatting.YELLOW), true);
                                            return 1;
                                        }))))

                // /otm status
                .then(Commands.literal("status")
                        .executes(ctx -> {
                            CommandSourceStack src = ctx.getSource();
                            src.sendSuccess(Component.literal("=== Off to Market Debug Status ===")
                                    .withStyle(ChatFormatting.GOLD), false);
                            src.sendSuccess(Component.literal("Debug: " + (DebugConfig.DEBUG_ENABLED ? "ON" : "OFF")), false);
                            src.sendSuccess(Component.literal("TPS: " + DebugConfig.WATCH_SERVER_TPS), false);
                            src.sendSuccess(Component.literal("Game Time: " + DebugConfig.WATCH_GAME_TIME), false);
                            src.sendSuccess(Component.literal("Trader Lvl: " + DebugConfig.WATCH_TRADER_LEVEL
                                    + " | XP: " + DebugConfig.WATCH_TRADER_XP), false);
                            src.sendSuccess(Component.literal("Pending Coins: " + DebugConfig.WATCH_PENDING_COINS + " CP"), false);
                            src.sendSuccess(Component.literal("Active Shipments: " + DebugConfig.WATCH_ACTIVE_SHIPMENTS), false);
                            src.sendSuccess(Component.literal("Last Event: " + DebugConfig.WATCH_LAST_EVENT), false);
                            return 1;
                        }))

                // /otm settings — view and modify mod settings
                .then(Commands.literal("settings")
                        .executes(ctx -> {
                            CommandSourceStack src = ctx.getSource();
                            src.sendSuccess(Component.literal("=== Off to Market Settings ===")
                                    .withStyle(ChatFormatting.GOLD), false);
                            src.sendSuccess(Component.literal("Gold Only Mode: " +
                                    (DebugConfig.isGoldOnlyMode() ? "ON" : "OFF"))
                                    .withStyle(DebugConfig.isGoldOnlyMode() ? ChatFormatting.GREEN : ChatFormatting.RED), false);
                            src.sendSuccess(Component.literal("Pickup Delay: " + DebugConfig.getPickupDelay() + " ticks"), false);
                            src.sendSuccess(Component.literal("Ticks/Distance: " + DebugConfig.getTicksPerDistance()), false);
                            src.sendSuccess(Component.literal("Sale Interval: " + DebugConfig.getSaleCheckInterval() + " ticks"), false);
                            src.sendSuccess(Component.literal("Sale Chance: " + String.format("%.0f%%", DebugConfig.getBaseSaleChance() * 100)), false);
                            src.sendSuccess(Component.literal("Max Market Time: " + DebugConfig.getMaxMarketTime() + " ticks"), false);
                            src.sendSuccess(Component.literal("Need Bonus: " + String.format("%.1fx", DebugConfig.getNeedBonus()) + " (legacy)"), false);
                            src.sendSuccess(Component.literal("Surplus Penalty: " + String.format("%.1fx", DebugConfig.getSurplusPenalty()) + " (legacy)"), false);
                            src.sendSuccess(Component.literal("NeedLevel System: ACTIVE (6-tier graduated)"), false);
                            src.sendSuccess(Component.literal("Overprice Threshold: " + String.format("%.1fx", DebugConfig.getOverpriceThreshold())), false);
                            src.sendSuccess(Component.literal("Bin Search Radius: " + DebugConfig.getBinSearchRadius() + " blocks"), false);
                            src.sendSuccess(Component.literal("XP/Sale: " + DebugConfig.getXpPerSale()), false);
                            src.sendSuccess(Component.literal("Base XP to Level: " + DebugConfig.getBaseXpToLevel()), false);
                            src.sendSuccess(Component.literal("Max Trader Level: " + DebugConfig.getMaxTraderLevel()), false);
                            return 1;
                        })

                        // /otm settings goldonly — toggle gold only mode
                        .then(Commands.literal("goldonly")
                                .executes(ctx -> {
                                    boolean current = DebugConfig.isGoldOnlyMode();
                                    DebugConfig.OVERRIDE_GOLD_ONLY_MODE = current ? 0 : 1;
                                    ModConfig.goldOnlyMode = !current;
                                    boolean newVal = !current;
                                    ctx.getSource().sendSuccess(Component.literal(
                                            "Gold Only Mode: " + (newVal ? "ON" : "OFF"))
                                            .withStyle(newVal ? ChatFormatting.GREEN : ChatFormatting.RED), true);
                                    return 1;
                                }))

                        // /otm settings set <key> <value>
                        .then(Commands.literal("set")
                                .then(Commands.argument("key", StringArgumentType.word())
                                        .then(Commands.argument("value", DoubleArgumentType.doubleArg())
                                                .executes(ctx -> {
                                                    String key = StringArgumentType.getString(ctx, "key");
                                                    double val = DoubleArgumentType.getDouble(ctx, "value");
                                                    String result = applySettingChange(key, val);
                                                    ctx.getSource().sendSuccess(Component.literal(result)
                                                            .withStyle(ChatFormatting.GREEN), true);
                                                    return 1;
                                                }))))

                        // /otm settings reset
                        .then(Commands.literal("reset")
                                .executes(ctx -> {
                                    DebugConfig.resetOverrides();
                                    ctx.getSource().sendSuccess(Component.literal(
                                            "All settings reset to config defaults.")
                                            .withStyle(ChatFormatting.YELLOW), true);
                                    return 1;
                                }))
                )

                // /otm balancetest — verify price ranges by tier
                .then(Commands.literal("balancetest")
                        .executes(ctx -> {
                            runBalanceTest(ctx.getSource());
                            return 1;
                        }))
        );
    }

    private static String applySet(String key, double val) {
        return switch (key.toLowerCase()) {
            case "pickupdelay" -> { DebugConfig.OVERRIDE_PICKUP_DELAY = (int) val; yield "pickupDelay → " + (int) val; }
            case "ticksperdistance" -> { DebugConfig.OVERRIDE_TICKS_PER_DISTANCE = (int) val; yield "ticksPerDistance → " + (int) val; }
            case "saleinterval" -> { DebugConfig.OVERRIDE_SALE_CHECK_INTERVAL = (int) val; yield "saleCheckInterval → " + (int) val; }
            case "salechance" -> { DebugConfig.OVERRIDE_BASE_SALE_CHANCE = val; yield "baseSaleChance → " + val; }
            case "needbonus" -> { DebugConfig.OVERRIDE_NEED_BONUS = val; yield "needBonus → " + val; }
            case "surpluspenalty" -> { DebugConfig.OVERRIDE_SURPLUS_PENALTY = val; yield "surplusPenalty → " + val; }
            case "overprice" -> { DebugConfig.OVERRIDE_OVERPRICE_THRESHOLD = val; yield "overpriceThreshold → " + val; }
            case "xppersale" -> { DebugConfig.OVERRIDE_XP_PER_SALE = (int) val; yield "xpPerSale → " + (int) val; }
            case "baselevel" -> { DebugConfig.OVERRIDE_BASE_XP_TO_LEVEL = (int) val; yield "baseXpToLevel → " + (int) val; }
            case "maxlevel" -> { DebugConfig.OVERRIDE_MAX_TRADER_LEVEL = (int) val; yield "maxTraderLevel → " + (int) val; }
            case "searchradius" -> { DebugConfig.OVERRIDE_BIN_SEARCH_RADIUS = (int) val; yield "binSearchRadius → " + (int) val; }
            case "goldonly" -> { DebugConfig.OVERRIDE_GOLD_ONLY_MODE = (int) val != 0 ? 1 : 0; ModConfig.goldOnlyMode = (int) val != 0; yield "goldOnlyMode → " + ((int) val != 0 ? "ON" : "OFF"); }
            default -> "Unknown key: " + key + ". Valid: pickupDelay, ticksPerDistance, saleInterval, saleChance, needBonus, surplusPenalty, overprice, xpPerSale, baseLevel, maxLevel, searchRadius, goldonly";
        };
    }

    private static String applySettingChange(String key, double val) {
        return switch (key.toLowerCase()) {
            case "pickupdelay" -> { ModConfig.pickupDelayTicks = (int) val; DebugConfig.OVERRIDE_PICKUP_DELAY = (int) val; yield "Pickup Delay set to " + (int) val + " ticks"; }
            case "ticksperdistance" -> { ModConfig.ticksPerDistance = (int) val; DebugConfig.OVERRIDE_TICKS_PER_DISTANCE = (int) val; yield "Ticks/Distance set to " + (int) val; }
            case "saleinterval" -> { ModConfig.saleCheckInterval = (int) val; DebugConfig.OVERRIDE_SALE_CHECK_INTERVAL = (int) val; yield "Sale Interval set to " + (int) val + " ticks"; }
            case "salechance" -> { ModConfig.baseSaleChance = val; DebugConfig.OVERRIDE_BASE_SALE_CHANCE = val; yield "Sale Chance set to " + String.format("%.0f%%", val * 100); }
            case "maxmarkettime" -> { ModConfig.maxMarketTimeTicks = (int) val; DebugConfig.OVERRIDE_MAX_MARKET_TIME = (int) val; yield "Max Market Time set to " + (int) val + " ticks"; }
            case "needbonus" -> { ModConfig.needBonus = val; DebugConfig.OVERRIDE_NEED_BONUS = val; yield "Need Bonus set to " + String.format("%.1fx", val); }
            case "surpluspenalty" -> { ModConfig.surplusPenalty = val; DebugConfig.OVERRIDE_SURPLUS_PENALTY = val; yield "Surplus Penalty set to " + String.format("%.1fx", val); }
            case "overprice" -> { ModConfig.overpriceThreshold = val; DebugConfig.OVERRIDE_OVERPRICE_THRESHOLD = val; yield "Overprice Threshold set to " + String.format("%.1fx", val); }
            case "xppersale" -> { ModConfig.xpPerSale = (int) val; DebugConfig.OVERRIDE_XP_PER_SALE = (int) val; yield "XP/Sale set to " + (int) val; }
            case "baselevel" -> { ModConfig.baseXpToLevel = (int) val; DebugConfig.OVERRIDE_BASE_XP_TO_LEVEL = (int) val; yield "Base XP to Level set to " + (int) val; }
            case "maxlevel" -> { ModConfig.maxTraderLevel = (int) val; DebugConfig.OVERRIDE_MAX_TRADER_LEVEL = (int) val; yield "Max Trader Level set to " + (int) val; }
            case "searchradius" -> { ModConfig.binSearchRadius = (int) val; DebugConfig.OVERRIDE_BIN_SEARCH_RADIUS = (int) val; yield "Bin Search Radius set to " + (int) val + " blocks"; }
            default -> "Unknown setting: " + key + ". Valid: pickupDelay, ticksPerDistance, saleInterval, saleChance, maxMarketTime, needBonus, surplusPenalty, overprice, xpPerSale, baseLevel, maxLevel, searchRadius";
        };
    }

    // ==================== Balance Testing ====================

    /**
     * /otm balancetest — Checks representative items from each tier against expected CP ranges.
     * Reports PASS/FAIL for each tier with actual vs expected values.
     */
    private static void runBalanceTest(CommandSourceStack src) {
        src.sendSuccess(Component.literal("=== Balance Test: Price Ranges ===")
                .withStyle(ChatFormatting.GOLD), false);

        int pass = 0, fail = 0;

        // Early-game: 1–10 CP
        pass += testItem(src, Items.STICK, 1, 10);
        pass += testItem(src, Items.WOODEN_SWORD, 1, 10);
        pass += testItem(src, Items.WOODEN_PICKAXE, 1, 10);
        pass += testItem(src, Items.BREAD, 1, 10);
        pass += testItem(src, Items.APPLE, 1, 10);
        pass += testItem(src, Items.COBBLESTONE, 1, 10);

        // Mid-game: 15–60 CP
        pass += testItem(src, Items.IRON_SWORD, 15, 60);
        pass += testItem(src, Items.IRON_PICKAXE, 15, 60);
        pass += testItem(src, Items.IRON_CHESTPLATE, 15, 120);
        pass += testItem(src, Items.BOW, 5, 30);
        pass += testItem(src, Items.GOLD_INGOT, 30, 100);

        // Late-game: 100–500 CP
        pass += testItem(src, Items.DIAMOND, 80, 500);
        pass += testItem(src, Items.DIAMOND_SWORD, 100, 500);
        pass += testItem(src, Items.DIAMOND_PICKAXE, 100, 500);
        pass += testItem(src, Items.DIAMOND_CHESTPLATE, 200, 1000);

        // End-game: 500–3000 CP
        pass += testItem(src, Items.NETHERITE_INGOT, 500, 3000);
        pass += testItem(src, Items.NETHERITE_SWORD, 500, 3000);
        pass += testItem(src, Items.NETHER_STAR, 1000, 5000);
        pass += testItem(src, Items.ELYTRA, 2000, 8000);
        pass += testItem(src, Items.DRAGON_EGG, 3000, 10000);

        fail = 20 - pass; // total test items
        ChatFormatting color = fail == 0 ? ChatFormatting.GREEN : (fail <= 3 ? ChatFormatting.YELLOW : ChatFormatting.RED);
        src.sendSuccess(Component.literal("--- Results: " + pass + " PASS, " + fail + " FAIL ---")
                .withStyle(color), false);
    }

    /**
     * Tests a single item's base price against an expected range.
     * Returns 1 for pass, 0 for fail.
     */
    private static int testItem(CommandSourceStack src, Item item, int minExpected, int maxExpected) {
        ItemStack stack = new ItemStack(item);
        int basePrice = PriceCalculator.getBaseValue(stack);
        int maxPrice = PriceCalculator.getMaxPrice(stack);
        String name = stack.getHoverName().getString();

        boolean inRange = basePrice >= minExpected && basePrice <= maxExpected;
        ChatFormatting fmt = inRange ? ChatFormatting.GREEN : ChatFormatting.RED;
        String result = inRange ? "\u2714" : "\u2718";

        src.sendSuccess(Component.literal(
                result + " " + name + ": " + basePrice + " CP (max " + maxPrice + ") [expect " + minExpected + "-" + maxExpected + "]")
                .withStyle(fmt), false);

        return inRange ? 1 : 0;
    }
}
