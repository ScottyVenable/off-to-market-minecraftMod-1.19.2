package com.offtomarket.mod.content;

import java.util.List;
import java.util.Map;

/**
 * POJO that mirrors the JSON structure of a town definition file.
 * One JSON file = one town.
 *
 * Create a new file in data/offtomarket/towns/ and add its filename
 * to _index.json to register it automatically.
 *
 * Fields: id, displayName, description, distance (1-10), type
 * (VILLAGE/TOWN/CITY/MARKET/OUTPOST), minTraderLevel (1-5), sells (list),
 * needLevels (map of item -> DESPERATE/HIGH_NEED/MODERATE_NEED/BALANCED/SURPLUS/OVERSATURATED).
 */
public class TownDefinition {

    /** Unique ID used in code and for Save/NBT (e.g. {@code "greenhollow"}). */
    public String id;

    /** Player-facing name shown in the Market Board UI. */
    public String displayName;

    /** Short flavour text shown in the town listing. */
    public String description;

    /**
     * Abstract travel distance from 1 (close) to 10 (far).
     * Higher values grant a distance premium on sale prices.
     */
    public int distance = 1;

    /**
     * Town type. One of: {@code VILLAGE, TOWN, CITY, MARKET, OUTPOST}.
     * Controls the base price bias in market listings.
     */
    public String type = "TOWN";

    /** Minimum trader level before this town unlocks (1–5). */
    public int minTraderLevel = 1;

    /**
     * Items this town sells on the Market Board.
     * Use full registry names: {@code "minecraft:wheat"}, {@code "farmersdelight:rice"}, etc.
     */
    public List<String> sells;

    /**
     * Graduated demand level per item.
     * <ul>
     *   <li>Key   — item registry name, e.g. {@code "minecraft:iron_ingot"}
     *   <li>Value — one of: {@code DESPERATE, HIGH_NEED, MODERATE_NEED, BALANCED, SURPLUS, OVERSATURATED}
     * </ul>
     * Items not listed default to {@code BALANCED}.
     * This map replaces the old binary needs/surplus sets and supports fine-grained pricing.
     */
    public Map<String, String> needLevels;
}
