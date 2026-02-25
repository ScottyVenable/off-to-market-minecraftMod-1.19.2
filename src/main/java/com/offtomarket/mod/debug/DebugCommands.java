package com.offtomarket.mod.debug;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.DoubleArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.offtomarket.mod.config.ModConfig;
import com.offtomarket.mod.content.CustomMenuRegistry;
import com.offtomarket.mod.data.PriceCalculator;
import com.offtomarket.mod.network.ModNetwork;
import com.offtomarket.mod.network.OpenCustomMenuPacket;
import net.minecraft.ChatFormatting;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraftforge.network.PacketDistributor;
import com.offtomarket.mod.data.TownRegistry;
import com.offtomarket.mod.data.TownData;
import com.offtomarket.mod.data.NeedLevel;

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

                        // /otm debug uibounds
                        .then(Commands.literal("uibounds")
                                .executes(ctx -> {
                                    DebugConfig.SHOW_UI_BOUNDS = !DebugConfig.SHOW_UI_BOUNDS;
                                    ctx.getSource().sendSuccess(Component.literal(
                                            "UI Bounds: " + (DebugConfig.SHOW_UI_BOUNDS ? "ON" : "OFF"))
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

                // /otm help [topic]
                .then(Commands.literal("help")
                        .executes(ctx -> {
                            printHelp(ctx.getSource());
                            return 1;
                        })
                        .then(Commands.argument("topic", StringArgumentType.word())
                                .executes(ctx -> {
                                    printHelpTopic(ctx.getSource(), StringArgumentType.getString(ctx, "topic"));
                                    return 1;
                                })))

                // /otm town list | info <id>
                .then(Commands.literal("town")
                        .then(Commands.literal("list")
                                .executes(ctx -> {
                                    printTownList(ctx.getSource());
                                    return 1;
                                }))
                        .then(Commands.literal("info")
                                .then(Commands.argument("id", StringArgumentType.word())
                                        .executes(ctx -> {
                                            printTownInfo(ctx.getSource(), StringArgumentType.getString(ctx, "id"));
                                            return 1;
                                        }))))

                // /otm price — show price of held item
                .then(Commands.literal("price")
                        .executes(ctx -> {
                            if (!(ctx.getSource().getEntity() instanceof ServerPlayer player)) {
                                ctx.getSource().sendFailure(Component.literal("[OTM] Must be a player."));
                                return 0;
                            }
                            ItemStack held = player.getMainHandItem();
                            if (held.isEmpty()) {
                                ctx.getSource().sendFailure(Component.literal("[OTM] Hold an item in your main hand to check its price."));
                                return 0;
                            }
                            int base = PriceCalculator.getBaseValue(held);
                            int max  = PriceCalculator.getMaxPrice(held);
                            String name = held.getHoverName().getString();
                            ctx.getSource().sendSuccess(Component.literal(
                                    "=== Price: " + name + " ===").withStyle(ChatFormatting.GOLD), false);
                            ctx.getSource().sendSuccess(Component.literal(
                                    "  Base: " + base + " CP  |  Max: " + max + " CP")
                                    .withStyle(ChatFormatting.YELLOW), false);
                            ctx.getSource().sendSuccess(Component.literal(
                                    "  Breakdown: " + (base / 100) + "g " + ((base % 100) / 10) + "s " + (base % 10) + "c  (base)")
                                    .withStyle(ChatFormatting.WHITE), false);
                            ctx.getSource().sendSuccess(Component.literal(
                                    "  Rarity: " + PriceCalculator.getRarityName(held.getRarity()))
                                    .withStyle(ChatFormatting.AQUA), false);
                            return 1;
                        }))

                // /otm menu open <id> — open a custom menu screen on the requesting player's client
                .then(Commands.literal("menu")
                        .then(Commands.literal("open")
                                .then(Commands.argument("menuId", StringArgumentType.word())
                                        .executes(ctx -> {
                                            String menuId = StringArgumentType.getString(ctx, "menuId");
                                            if (CustomMenuRegistry.get(menuId) == null) {
                                                ctx.getSource().sendFailure(Component.literal(
                                                    "[OTM] Unknown menu id '" + menuId + "'. Available: "
                                                    + CustomMenuRegistry.getAllIds()));
                                                return 0;
                                            }
                                            if (!(ctx.getSource().getEntity() instanceof ServerPlayer player)) {
                                                ctx.getSource().sendFailure(Component.literal(
                                                    "[OTM] Must be a player."));
                                                return 0;
                                            }
                                            ModNetwork.CHANNEL.send(
                                                PacketDistributor.PLAYER.with(() -> player),
                                                new OpenCustomMenuPacket(menuId));
                                            ctx.getSource().sendSuccess(Component.literal(
                                                "Opening menu '" + menuId + "' ...")
                                                .withStyle(ChatFormatting.GREEN), false);
                                            return 1;
                                        }))))
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

    // ==================== Help ====================

    private static void printHelp(CommandSourceStack src) {
        src.sendSuccess(Component.literal("=== /otm Commands (requires OP level 2) ===")
                .withStyle(ChatFormatting.GOLD), false);

        src.sendSuccess(Component.literal("[General]").withStyle(ChatFormatting.YELLOW), false);
        src.sendSuccess(Component.literal("  /otm help [topic]       - This help list. Topics: debug, grant, settings, town, price, menu"), false);
        src.sendSuccess(Component.literal("  /otm status             - Show live debug watch values (coins, XP, shipments)"), false);
        src.sendSuccess(Component.literal("  /otm balancetest        - Run item price tier accuracy test"), false);

        src.sendSuccess(Component.literal("[Grant & Trader]").withStyle(ChatFormatting.YELLOW), false);
        src.sendSuccess(Component.literal("  /otm grant coins <n>    - Give yourself n copper pieces (100 CP = 1g)"), false);
        src.sendSuccess(Component.literal("  /otm grant xp <n>       - Give yourself n trader XP"), false);
        src.sendSuccess(Component.literal("  /otm setlevel <1-20>    - Set your trader level immediately"), false);

        src.sendSuccess(Component.literal("[Simulation]").withStyle(ChatFormatting.YELLOW), false);
        src.sendSuccess(Component.literal("  /otm deliver            - Instantly deliver all in-transit shipments"), false);
        src.sendSuccess(Component.literal("  /otm sell               - Instantly sell all items currently on the market"), false);

        src.sendSuccess(Component.literal("[Debug Flags]").withStyle(ChatFormatting.YELLOW), false);
        src.sendSuccess(Component.literal("  /otm debug              - Toggle debug mode"), false);
        src.sendSuccess(Component.literal("  /otm debug hud          - Toggle debug HUD overlay"), false);
        src.sendSuccess(Component.literal("  /otm debug verbose      - Toggle verbose server logging"), false);
        src.sendSuccess(Component.literal("  /otm debug uibounds     - Toggle UI bounding box overlay"), false);
        src.sendSuccess(Component.literal("  /otm debug apply        - Push current debug overrides into ModConfig"), false);
        src.sendSuccess(Component.literal("  /otm debug reset        - Clear all overrides back to config defaults"), false);

        src.sendSuccess(Component.literal("[Settings]").withStyle(ChatFormatting.YELLOW), false);
        src.sendSuccess(Component.literal("  /otm settings           - View all current mod settings"), false);
        src.sendSuccess(Component.literal("  /otm settings goldonly  - Toggle gold-only payout mode"), false);
        src.sendSuccess(Component.literal("  /otm settings set <key> <val>  - Change a setting at runtime"), false);
        src.sendSuccess(Component.literal("  /otm settings reset     - Reset all settings to config file defaults"), false);
        src.sendSuccess(Component.literal("  Use /otm help settings for valid setting keys.").withStyle(ChatFormatting.GRAY), false);

        src.sendSuccess(Component.literal("[Towns]").withStyle(ChatFormatting.YELLOW), false);
        src.sendSuccess(Component.literal("  /otm town list          - List all registered towns"), false);
        src.sendSuccess(Component.literal("  /otm town info <id>     - Show detailed info for a specific town"), false);

        src.sendSuccess(Component.literal("[Pricing]").withStyle(ChatFormatting.YELLOW), false);
        src.sendSuccess(Component.literal("  /otm price              - Show base/max price of your held item"), false);

        src.sendSuccess(Component.literal("[Menus]").withStyle(ChatFormatting.YELLOW), false);
        src.sendSuccess(Component.literal("  /otm menu open <id>     - Open a custom menu screen by ID"), false);

        src.sendSuccess(Component.literal("[Advanced]").withStyle(ChatFormatting.YELLOW), false);
        src.sendSuccess(Component.literal("  /otm set <key> <val>    - Override a config value at runtime (not saved)"), false);
    }

    private static void printHelpTopic(CommandSourceStack src, String topic) {
        switch (topic.toLowerCase()) {
            case "debug" -> {
                src.sendSuccess(Component.literal("=== Debug Commands ===").withStyle(ChatFormatting.GOLD), false);
                src.sendSuccess(Component.literal("/otm debug              - Toggle debug mode on/off"), false);
                src.sendSuccess(Component.literal("/otm debug hud          - Toggle debug HUD overlay on screen"), false);
                src.sendSuccess(Component.literal("/otm debug verbose      - Extra logging to server console"), false);
                src.sendSuccess(Component.literal("/otm debug uibounds     - Show UI element bounding boxes (client)"), false);
                src.sendSuccess(Component.literal("/otm debug apply        - Push debug overrides into active ModConfig"), false);
                src.sendSuccess(Component.literal("/otm debug reset        - Clear all overrides back to config defaults"), false);
            }
            case "grant" -> {
                src.sendSuccess(Component.literal("=== Grant & Trader Commands ===").withStyle(ChatFormatting.GOLD), false);
                src.sendSuccess(Component.literal("/otm grant coins <n>    - Give n copper pieces (100=1g, 10=1s, 1=1c)"), false);
                src.sendSuccess(Component.literal("/otm grant xp <n>       - Give n trader XP toward the next level"), false);
                src.sendSuccess(Component.literal("/otm setlevel <1-20>    - Directly set your trader level"), false);
                src.sendSuccess(Component.literal("Example: /otm grant coins 500  (= 5g)").withStyle(ChatFormatting.GRAY), false);
            }
            case "settings" -> {
                src.sendSuccess(Component.literal("=== Settings Keys ===").withStyle(ChatFormatting.GOLD), false);
                src.sendSuccess(Component.literal("Use with: /otm settings set <key> <value>").withStyle(ChatFormatting.GRAY), false);
                src.sendSuccess(Component.literal("  pickupDelay      - Ticks before delivered items are collected"), false);
                src.sendSuccess(Component.literal("  ticksPerDistance - Travel ticks per distance unit"), false);
                src.sendSuccess(Component.literal("  saleInterval     - Ticks between sale attempt checks"), false);
                src.sendSuccess(Component.literal("  saleChance       - Base sale probability 0.0-1.0 (0.25 = 25%)"), false);
                src.sendSuccess(Component.literal("  maxMarketTime    - Max ticks an item sits on the market"), false);
                src.sendSuccess(Component.literal("  needBonus        - Sale multiplier when town needs the item (legacy)"), false);
                src.sendSuccess(Component.literal("  surplusPenalty   - Sale multiplier when town has surplus (legacy)"), false);
                src.sendSuccess(Component.literal("  overprice        - Price ratio above which sale chance drops sharply"), false);
                src.sendSuccess(Component.literal("  xpPerSale        - XP awarded per successful sale"), false);
                src.sendSuccess(Component.literal("  baseLevel        - Base XP required to level up"), false);
                src.sendSuccess(Component.literal("  maxLevel         - Maximum trader level cap"), false);
                src.sendSuccess(Component.literal("  searchRadius     - Blocks to search for a Trading Ledger"), false);
            }
            case "town" -> {
                src.sendSuccess(Component.literal("=== Town Commands ===").withStyle(ChatFormatting.GOLD), false);
                src.sendSuccess(Component.literal("/otm town list          - Shows all towns: id, name, type, distance, min level"), false);
                src.sendSuccess(Component.literal("/otm town info <id>     - Full details: need levels, specialty items, description"), false);
                src.sendSuccess(Component.literal("Example: /otm town info ironhaven").withStyle(ChatFormatting.GRAY), false);
                src.sendSuccess(Component.literal("Use /otm town list to see valid town IDs.").withStyle(ChatFormatting.GRAY), false);
            }
            case "price" -> {
                src.sendSuccess(Component.literal("=== Price Command ===").withStyle(ChatFormatting.GOLD), false);
                src.sendSuccess(Component.literal("/otm price              - Prices the item in your main hand"), false);
                src.sendSuccess(Component.literal("Shows: base sell price, max price cap, and rarity.").withStyle(ChatFormatting.GRAY), false);
                src.sendSuccess(Component.literal("Currency: Copper Pieces. 100 CP = 1 Gold, 10 CP = 1 Silver.").withStyle(ChatFormatting.GRAY), false);
                src.sendSuccess(Component.literal("Base = default price with no modifiers applied.").withStyle(ChatFormatting.GRAY), false);
                src.sendSuccess(Component.literal("Max  = ceiling before the overprice penalty activates.").withStyle(ChatFormatting.GRAY), false);
            }
            case "menu" -> {
                src.sendSuccess(Component.literal("=== Menu Commands ===").withStyle(ChatFormatting.GOLD), false);
                src.sendSuccess(Component.literal("/otm menu open <id>     - Open a custom menu screen on your client"), false);
                src.sendSuccess(Component.literal("Available menus: " + CustomMenuRegistry.getAllIds()).withStyle(ChatFormatting.GRAY), false);
                src.sendSuccess(Component.literal("Custom menus are defined in data/offtomarket/custom_menus/*.json").withStyle(ChatFormatting.GRAY), false);
            }
            default ->
                src.sendFailure(Component.literal(
                        "Unknown topic '" + topic + "'. Valid topics: debug, grant, settings, town, price, menu"));
        }
    }

    // ==================== Town Commands ====================

    private static void printTownList(CommandSourceStack src) {
        var towns = TownRegistry.getAllTowns();
        src.sendSuccess(Component.literal("=== Registered Towns (" + towns.size() + ") ===")
                .withStyle(ChatFormatting.GOLD), false);
        src.sendSuccess(Component.literal(
                String.format("  %-18s %-22s %-12s %4s  %s", "ID", "Name", "Type", "Dist", "MinLvl"))
                .withStyle(ChatFormatting.GRAY), false);
        for (TownData town : towns) {
            ChatFormatting color = town.getMinTraderLevel() <= 1 ? ChatFormatting.WHITE : ChatFormatting.AQUA;
            src.sendSuccess(Component.literal(
                    String.format("  %-18s %-22s %-12s %4d   %d",
                            town.getId(),
                            town.getDisplayName(),
                            town.getType().name().toLowerCase(),
                            town.getDistance(),
                            town.getMinTraderLevel()))
                    .withStyle(color), false);
        }
        src.sendSuccess(Component.literal("Use /otm town info <id> for full details.").withStyle(ChatFormatting.GRAY), false);
    }

    private static void printTownInfo(CommandSourceStack src, String id) {
        TownData town = TownRegistry.getTown(id);
        if (town == null) {
            src.sendFailure(Component.literal(
                    "Unknown town '" + id + "'. Use /otm town list to see valid IDs."));
            return;
        }
        src.sendSuccess(Component.literal("=== " + town.getDisplayName() + " ===")
                .withStyle(ChatFormatting.GOLD), false);
        src.sendSuccess(Component.literal("  ID: " + town.getId()), false);
        src.sendSuccess(Component.literal("  Type: " + town.getType().name().toLowerCase()
                + "  |  Distance: " + town.getDistance()
                + "  |  Min Level: " + town.getMinTraderLevel()), false);
        if (town.getDescription() != null && !town.getDescription().isEmpty()) {
            src.sendSuccess(Component.literal("  \"" + town.getDescription() + "\"")
                    .withStyle(ChatFormatting.ITALIC), false);
        }
        if (!town.getNeedLevels().isEmpty()) {
            src.sendSuccess(Component.literal("  Need Levels (" + town.getNeedLevels().size() + "):")
                    .withStyle(ChatFormatting.YELLOW), false);
            for (var entry : town.getNeedLevels().entrySet()) {
                NeedLevel lvl = entry.getValue();
                ChatFormatting fmt = switch (lvl) {
                    case DESPERATE, HIGH_NEED -> ChatFormatting.GREEN;
                    case MODERATE_NEED, BALANCED -> ChatFormatting.WHITE;
                    case SURPLUS, OVERSATURATED -> ChatFormatting.RED;
                };
                src.sendSuccess(Component.literal("    " + entry.getKey() + "  ->  " + lvl.name())
                        .withStyle(fmt), false);
            }
        }
        if (!town.getSpecialtyItems().isEmpty()) {
            src.sendSuccess(Component.literal("  Sells (specialty items):")
                    .withStyle(ChatFormatting.AQUA), false);
            for (var rl : town.getSpecialtyItems()) {
                src.sendSuccess(Component.literal("    " + rl), false);
            }
        }
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
