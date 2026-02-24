package com.offtomarket.mod.data;

import com.offtomarket.mod.OffToMarket;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;

import java.util.Map;
import java.util.Random;

/**
 * Manages dynamic supply and demand for all towns.
 * <p>
 * Called once per server tick from the main event loop.
 * At each in-game day boundary, supply levels for every tracked item
 * category drift toward equilibrium (60) with a configurable chance.
 * <p>
 * <b>How it works:</b>
 * <ul>
 *   <li>Every in-game day (24000 ticks), a refresh cycle runs.</li>
 *   <li>For each tracked supply level in each town, there is a configurable
 *       chance ({@code dailyRefreshChance}, default 30%) that the supply
 *       drifts 1 step toward BALANCED (60).</li>
 *   <li>When a player sells items to a town, the supply increases,
 *       potentially pushing the town toward SURPLUS or OVERSATURATED.</li>
 *   <li>This creates a natural ebb and flow: high demand decays over time,
 *       and gluts slowly recover.</li>
 * </ul>
 */
public class SupplyDemandManager {

    private static final int TICKS_PER_DAY = 24000;
    private static final int EQUILIBRIUM = 60; // BALANCED supply level
    private static final Random RANDOM = new Random();

    /** Configurable: chance per item per day that supply drifts toward equilibrium. */
    private static double dailyRefreshChance = 0.30; // 30%

    /** Configurable: how many units supply drifts per refresh. */
    private static int driftAmount = 5;

    /** Track the last day we refreshed to avoid double-processing. */
    private static long lastRefreshDay = -1;

    /**
     * Called every server tick. Checks if a new in-game day has started
     * and triggers supply drift if so.
     */
    public static void onServerTick(MinecraftServer server) {
        ServerLevel overworld = server.overworld();
        if (overworld == null) return;

        long dayTime = overworld.getDayTime();
        long currentDay = dayTime / TICKS_PER_DAY;

        if (currentDay > lastRefreshDay) {
            lastRefreshDay = currentDay;
            refreshAllTowns();
        }
    }

    /**
     * Run the daily supply drift for all registered towns.
     * Each supply level has a random chance to drift toward equilibrium.
     */
    private static void refreshAllTowns() {
        for (TownData town : TownRegistry.getAllTowns()) {
            refreshTown(town);
        }
    }

    /**
     * Drift supply levels for a single town toward BALANCED (60).
     * Snapshots supply levels first so trend arrows can compare before/after.
     */
    private static void refreshTown(TownData town) {
        Map<String, Integer> supplyLevels = town.getSupplyLevels();
        if (supplyLevels.isEmpty()) return;

        // Snapshot before applying drift so we can show trend arrows
        town.snapshotSupplyLevels();

        for (Map.Entry<String, Integer> entry : supplyLevels.entrySet()) {
            if (RANDOM.nextDouble() < dailyRefreshChance) {
                int current = entry.getValue();
                if (current < EQUILIBRIUM) {
                    // Supply is low → drift up (demand eases)
                    entry.setValue(Math.min(EQUILIBRIUM, current + driftAmount));
                } else if (current > EQUILIBRIUM) {
                    // Supply is high → drift down (surplus absorbed)
                    entry.setValue(Math.max(EQUILIBRIUM, current - driftAmount));
                }
                // If already at equilibrium, do nothing
            }
        }
    }

    /**
     * Record that items were sold to a town.
     * Increases the supply level for the item, potentially
     * shifting the town from HIGH_NEED → BALANCED → SURPLUS.
     *
     * @param town     The town the items were sold to.
     * @param itemKey  The item registry name (e.g., "minecraft:iron_ingot").
     * @param quantity How many items were sold.
     */
    public static void recordSale(TownData town, String itemKey, int quantity) {
        town.adjustSupplyLevel(itemKey, quantity);
    }

    /**
     * Record that items were purchased from a town (buy orders).
     * Decreases the supply level, potentially creating demand.
     *
     * @param town     The town the items were purchased from.
     * @param itemKey  The item registry name.
     * @param quantity How many items were purchased.
     */
    public static void recordPurchase(TownData town, String itemKey, int quantity) {
        town.adjustSupplyLevel(itemKey, -quantity);
    }

    // ===================== Config Setters =====================

    public static void setDailyRefreshChance(double chance) {
        dailyRefreshChance = Math.max(0.0, Math.min(1.0, chance));
    }

    public static double getDailyRefreshChance() {
        return dailyRefreshChance;
    }

    public static void setDriftAmount(int amount) {
        driftAmount = Math.max(1, amount);
    }

    public static int getDriftAmount() {
        return driftAmount;
    }

    /**
     * Reset the refresh tracker (called when server starts/stops).
     */
    public static void reset() {
        lastRefreshDay = -1;
    }
}
