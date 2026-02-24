package com.offtomarket.mod.data;

import net.minecraft.nbt.CompoundTag;

/**
 * Represents a hired worker at the Trading Post.
 * Workers provide passive bonuses to trading and level up over time.
 * 
 * Two types:
 * - NEGOTIATOR: Negotiates better sale/buy prices, success scales with level.
 *   Per-trip payment deducted from earnings. Levels up per successful negotiation.
 * - TRADING_CART: Reduces delivery time for shipments. Levels up per completed trip.
 *   Per-trip payment deducted from earnings.
 */
public class Worker {
    public enum WorkerType {
        NEGOTIATOR("Negotiator", "Negotiates better prices with towns."),
        TRADING_CART("Trading Cart", "Decreases delivery time for shipments.");

        private final String displayName;
        private final String description;

        WorkerType(String displayName, String description) {
            this.displayName = displayName;
            this.description = description;
        }

        public String getDisplayName() { return displayName; }
        public String getDescription() { return description; }
    }

    private final WorkerType type;
    private int level;
    private int xp;
    private int totalTrips;     // lifetime trips completed
    private boolean hired;      // whether this worker is currently hired

    public Worker(WorkerType type) {
        this.type = type;
        this.level = 1;
        this.xp = 0;
        this.totalTrips = 0;
        this.hired = false;
    }

    // ==================== Getters ====================

    public WorkerType getType() { return type; }
    public int getLevel() { return level; }
    public int getXp() { return xp; }
    public int getTotalTrips() { return totalTrips; }
    public boolean isHired() { return hired; }

    public void setHired(boolean hired) { this.hired = hired; }

    // ==================== Level System ====================

    /**
     * XP needed for next level. Increases by 20 per level.
     */
    public int getXpForNextLevel() {
        return 20 + (level - 1) * 20; // 20, 40, 60, 80...
    }

    /**
     * Max level for workers.
     */
    public static final int MAX_LEVEL = 10;

    /**
     * Add XP and handle level ups.
     */
    public void addXp(int amount) {
        if (level >= MAX_LEVEL) return;
        xp += amount;
        while (xp >= getXpForNextLevel() && level < MAX_LEVEL) {
            xp -= getXpForNextLevel();
            level++;
        }
    }

    /**
     * Record a completed trip.
     */
    public void completedTrip() {
        totalTrips++;
        addXp(1);
    }

    // ==================== Bonuses ====================

    /**
     * Get the hire cost to initially hire this worker (one-time, in CP).
     */
    public int getHireCost() {
        return switch (type) {
            case NEGOTIATOR -> 500;   // 5 gold
            case TRADING_CART -> 300;  // 3 gold
        };
    }

    /**
     * Per-trip cost deducted from shipment earnings (in CP).
     * Stays constant per type.
     */
    public int getPerTripCost() {
        return switch (type) {
            case NEGOTIATOR -> 10 + level * 2;    // 12-30 CP per trip
            case TRADING_CART -> 5 + level;        // 6-15 CP per trip
        };
    }

    /**
     * Negotiator: percentage bonus to sale prices.
     * Ranges from 5% at level 1 to 25% at level 10.
     */
    public double getNegotiationBonus() {
        if (type != WorkerType.NEGOTIATOR) return 0.0;
        return 0.05 + (level - 1) * 0.022; // ~5% to ~25%
    }

    /**
     * Trading Cart: percentage reduction in travel time.
     * Ranges from 10% at level 1 to 40% at level 10.
     */
    public double getSpeedBonus() {
        if (type != WorkerType.TRADING_CART) return 0.0;
        return 0.10 + (level - 1) * 0.033; // ~10% to ~40%
    }

    /**
     * Get a display string for the current bonus.
     */
    public String getBonusDisplay() {
        return switch (type) {
            case NEGOTIATOR -> "+" + (int)(getNegotiationBonus() * 100) + "% sale price";
            case TRADING_CART -> "-" + (int)(getSpeedBonus() * 100) + "% travel time";
        };
    }

    // ==================== NBT ====================

    public CompoundTag save() {
        CompoundTag tag = new CompoundTag();
        tag.putString("Type", type.name());
        tag.putInt("Level", level);
        tag.putInt("Xp", xp);
        tag.putInt("Trips", totalTrips);
        tag.putBoolean("Hired", hired);
        return tag;
    }

    public static Worker load(CompoundTag tag) {
        WorkerType type = WorkerType.valueOf(tag.getString("Type"));
        Worker worker = new Worker(type);
        worker.level = Math.max(1, tag.getInt("Level"));
        worker.xp = tag.getInt("Xp");
        worker.totalTrips = tag.getInt("Trips");
        worker.hired = tag.getBoolean("Hired");
        return worker;
    }
}
