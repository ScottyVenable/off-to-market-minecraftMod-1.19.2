package com.offtomarket.mod.data;

import java.util.Collections;
import java.util.List;

/**
 * An immutable letter that a town NPC may send to the player when a specific
 * game event occurs at that town.
 *
 * Letters are defined per-town inside the town's JSON file under the
 * {@code "letters"} object, keyed by event type:
 *
 * <pre>
 * "letters": {
 *   "shipment_received": [ { "sender": "Pip", "subject": "Thanks!", "body": "..." } ],
 *   "quest_complete":    [ { "sender": "Pip", "subject": "Well done!", "body": "..." } ]
 * }
 * </pre>
 *
 * Supported event keys (used as keys in the {@code "letters"} map):
 * <ul>
 *   <li>{@code shipment_received}   - A shipment the player sent arrived at the town.</li>
 *   <li>{@code shipment_returned}   - Items were returned to the player from this town.</li>
 *   <li>{@code quest_complete}      - The player completed a delivery or specialty quest for this town.</li>
 *   <li>{@code quest_expired}       - A quest from this town expired without completion.</li>
 *   <li>{@code first_trade}         - The player's very first trade with this town.</li>
 *   <li>{@code reputation_increase} - Player gained reputation with this town.</li>
 *   <li>{@code reputation_decrease} - Player lost reputation with this town.</li>
 *   <li>{@code buy_order_fulfilled} - Player fulfilled a buy order from this town's Market Board listing.</li>
 *   <li>{@code market_refresh}      - Daily dawn market refresh at this town.</li>
 *   <li>{@code diplomat_proposed}   - A diplomat from this town arrived with a proposal.</li>
 *   <li>{@code diplomat_accepted}   - The player accepted a diplomat proposal from this town.</li>
 *   <li>{@code level_up}            - The player's trader level increased.</li>
 * </ul>
 *
 * When multiple letters exist for the same event, one is chosen randomly
 * weighted by {@link #getChanceWeight()}.  Eligibility is filtered by
 * {@link #getMinReputation()} and {@link #getMaxReputation()} before the roll.
 */
public class TownLetter {

    /** Display name of the NPC sending the letter (e.g. "Beau", "Innkeeper"). */
    private final String sender;

    /** Subject line shown in the mailbox list. */
    private final String subject;

    /** Full body text of the letter. May use supported placeholders if the
     *  calling system supports them — e.g. {@code {player}}, {@code {town}},
     *  {@code {item}}, {@code {coins}}. */
    private final String body;

    /**
     * Minimum reputation the player must have with this town for this letter
     * to be eligible. Default: {@code Integer.MIN_VALUE} (always eligible).
     */
    private final int minReputation;

    /**
     * Maximum reputation threshold (inclusive) for this letter to be eligible.
     * Lets you write letters that only appear early in a relationship, before
     * the player becomes well-known.  Default: {@code Integer.MAX_VALUE}.
     */
    private final int maxReputation;

    /**
     * Relative weight used when randomly picking one letter from multiple
     * eligible ones for the same event.  Higher = more likely.  Default: 1.
     */
    private final int chanceWeight;

    /**
     * Optional metadata tags for custom game-logic filtering.
     * E.g. {@code ["has_completed_main_quest", "botanist_unlocked"]}.
     * The mod does not interpret these automatically — they are available for
     * future conditional-trigger systems.
     */
    private final List<String> tags;

    public TownLetter(String sender, String subject, String body,
                      int minReputation, int maxReputation, int chanceWeight,
                      List<String> tags) {
        this.sender       = sender != null ? sender : "Unknown";
        this.subject      = subject != null ? subject : "";
        this.body         = body != null ? body : "";
        this.minReputation = minReputation;
        this.maxReputation = maxReputation;
        this.chanceWeight = Math.max(1, chanceWeight);
        this.tags         = tags != null ? Collections.unmodifiableList(tags) : Collections.emptyList();
    }

    public String getSender()       { return sender; }
    public String getSubject()      { return subject; }
    public String getBody()         { return body; }
    public int    getMinReputation(){ return minReputation; }
    public int    getMaxReputation(){ return maxReputation; }
    public int    getChanceWeight() { return chanceWeight; }
    public List<String> getTags()   { return tags; }

    /**
     * Whether this letter is eligible to be sent given the player's current
     * reputation with the town.
     */
    public boolean isEligible(int playerReputation) {
        return playerReputation >= minReputation && playerReputation <= maxReputation;
    }

    @Override
    public String toString() {
        return "TownLetter{event=?, sender='" + sender + "', subject='" + subject + "'}";
    }
}
