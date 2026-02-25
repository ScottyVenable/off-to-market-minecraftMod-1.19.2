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
 * needLevels (map of item -> DESPERATE/HIGH_NEED/MODERATE_NEED/BALANCED/SURPLUS/OVERSATURATED),
 * letters (map of event -> list of letter definitions).
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

    /** Minimum trader level before this town unlocks (1-5). */
    public int minTraderLevel = 1;

    /**
     * Items this town sells on the Market Board.
     * Use full registry names: {@code "minecraft:wheat"}, {@code "farmersdelight:rice"}, etc.
     */
    public List<String> sells;

    /**
     * Graduated demand level per item.
     * Key   - item registry name, e.g. {@code "minecraft:iron_ingot"}
     * Value - one of: {@code DESPERATE, HIGH_NEED, MODERATE_NEED, BALANCED, SURPLUS, OVERSATURATED}
     * Items not listed default to {@code BALANCED}.
     * This map replaces the old binary needs/surplus sets and supports fine-grained pricing.
     */
    public Map<String, String> needLevels;

    /**
     * Letters this town may send to the player when specific events occur.
     *
     * Key   - event type string. Supported events:
     *   {@code shipment_received}   - A shipment arrived at this town.
     *   {@code shipment_returned}   - Items were returned to the player.
     *   {@code quest_complete}      - A quest for this town was completed.
     *   {@code quest_expired}       - A quest expired without completion.
     *   {@code first_trade}         - Very first trade with this town.
     *   {@code reputation_increase} - Player gained reputation here.
     *   {@code reputation_decrease} - Player lost reputation here.
     *   {@code buy_order_fulfilled} - A buy order from this town was fulfilled.
     *   {@code market_refresh}      - Daily market refresh at dawn.
     *   {@code diplomat_proposed}   - Diplomat arrived with a proposal.
     *   {@code diplomat_accepted}   - Player accepted a diplomat proposal.
     *   {@code level_up}            - Player's trader level increased.
     * Value - list of candidate LetterDef objects. When an event fires, one
     *         eligible letter is chosen at random (weighted by chanceWeight).
     */
    public Map<String, List<LetterDef>> letters;

    // -----------------------------------------------------------------------

    /**
     * Raw POJO for a single letter entry inside the JSON.
     * Mirrors the fields of TownLetter exactly so GSON can deserialise it.
     */
    public static class LetterDef {

        /** Display name of the NPC sending the letter. */
        public String sender = "Townsperson";

        /** Subject line shown in the mailbox list. */
        public String subject = "";

        /**
         * Full body text.  Supports placeholders if the calling code expands them:
         * {player}, {town}, {item}, {coins}, {reputation}.
         */
        public String body = "";

        /**
         * Minimum reputation required for this letter to be eligible.
         * Default: no minimum (Integer.MIN_VALUE).
         */
        public int minReputation = Integer.MIN_VALUE;

        /**
         * Maximum reputation cap for eligibility.
         * Lets you write early-relationship letters that stop appearing
         * once the player is well-known.
         * Default: no cap (Integer.MAX_VALUE).
         */
        public int maxReputation = Integer.MAX_VALUE;

        /**
         * Relative weight when picking randomly among multiple eligible letters
         * for the same event.  Higher = more likely.  Default: 1.
         */
        public int chanceWeight = 1;

        /**
         * Optional metadata tags for future conditional-trigger systems.
         * E.g. {@code ["high_rep_only", "holiday"]}. Not used by base mod logic.
         */
        public List<String> tags;
    }
}
