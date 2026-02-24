package com.offtomarket.mod.data;

import com.offtomarket.mod.config.ModConfig;
import net.minecraft.nbt.CompoundTag;

/**
 * Represents a hired worker at the Trading Post.
 * Workers provide passive bonuses to trading and level up over time.
 * 
 * Three types:
 * - NEGOTIATOR: Negotiates better sale/buy prices, success scales with level.
 *   Per-trip payment deducted from earnings. Levels up per successful negotiation.
 * - TRADING_CART: Reduces delivery time for shipments. Levels up per completed trip.
 *   Per-trip payment deducted from earnings.
 * - BOOKKEEPER: Manages trade finances, reducing per-trip costs of all workers.
 *   Levels up per completed trip. Provides overhead reduction scaling with level.
 */
public class Worker {
    public enum WorkerType {
        NEGOTIATOR("Negotiator", "Negotiates better prices with towns, boosting sale earnings."),
        TRADING_CART("Trading Cart", "A faster cart that reduces delivery time for all shipments."),
        BOOKKEEPER("Bookkeeper", "Manages the ledger, reducing worker overhead costs per trip.");

        private final String displayName;
        private final String description;
        private final String symbol; // icon character for UI

        WorkerType(String displayName, String description) {
            this.displayName = displayName;
            this.description = description;
            this.symbol = switch (this.ordinal()) {
                case 0 -> "\u2696"; // ⚖ scales for negotiator
                case 1 -> "\u2708"; // ✈ for speed/cart
                default -> "\u270E"; // ✎ for bookkeeper
            };
        }

        public String getDisplayName() { return displayName; }
        public String getDescription() { return description; }
        public String getSymbol() { return symbol; }
    }

    private final WorkerType type;
    private int level;
    private int xp;
    private int totalTrips;          // lifetime trips completed
    private boolean hired;           // whether this worker is currently hired
    private long lifetimeBonusValue; // lifetime bonus value in CP (earnings gained, time saved, costs saved)

    public Worker(WorkerType type) {
        this.type = type;
        this.level = 1;
        this.xp = 0;
        this.totalTrips = 0;
        this.hired = false;
        this.lifetimeBonusValue = 0;
    }

    // ==================== Getters ====================

    public WorkerType getType() { return type; }
    public int getLevel() { return level; }
    public int getXp() { return xp; }
    public int getTotalTrips() { return totalTrips; }
    public boolean isHired() { return hired; }
    public long getLifetimeBonusValue() { return lifetimeBonusValue; }

    public void setHired(boolean hired) { this.hired = hired; }
    public void addLifetimeBonusValue(long amount) { this.lifetimeBonusValue += amount; }

    // ==================== Level System ====================

    /**
     * XP needed for next level. Increases by 20 per level.
     */
    public int getXpForNextLevel() {
        return 20 + (level - 1) * 20; // 20, 40, 60, 80...
    }

    /**
     * Max level for workers — read from config.
     */
    public static int getMaxLevel() {
        return ModConfig.maxWorkerLevel;
    }

    /** @deprecated Use getMaxLevel() instead — left for backwards compat. */
    @Deprecated
    public static final int MAX_LEVEL = 10;

    /**
     * Add XP and handle level ups. Returns number of levels gained.
     */
    public int addXp(int amount) {
        int maxLvl = getMaxLevel();
        if (level >= maxLvl) return 0;
        xp += amount;
        int levelsGained = 0;
        while (xp >= getXpForNextLevel() && level < maxLvl) {
            xp -= getXpForNextLevel();
            level++;
            levelsGained++;
        }
        if (level >= maxLvl) xp = 0; // cap XP at max
        return levelsGained;
    }

    /**
     * Record a completed trip.
     */
    public void completedTrip() {
        totalTrips++;
        addXp(1);
    }

    // ==================== Level Titles ====================

    /**
     * Get the title for the current level.
     */
    public String getLevelTitle() {
        return getLevelTitle(level);
    }

    /**
     * Get title for a given level.
     */
    public static String getLevelTitle(int lvl) {
        if (lvl <= 2) return "Novice";
        if (lvl <= 4) return "Apprentice";
        if (lvl <= 6) return "Journeyman";
        if (lvl <= 8) return "Expert";
        if (lvl == 9) return "Master";
        return "Grandmaster";
    }

    /**
     * Get color for the level title.
     */
    public int getLevelTitleColor() {
        return getLevelTitleColor(level);
    }

    public static int getLevelTitleColor(int lvl) {
        if (lvl <= 2) return 0xAAAAAA;  // gray
        if (lvl <= 4) return 0x88CC88;  // green
        if (lvl <= 6) return 0x88BBFF;  // blue
        if (lvl <= 8) return 0xDD88FF;  // purple
        if (lvl == 9) return 0xFFAA44;  // orange
        return 0xFFD700;                // gold
    }

    // ==================== Perks ====================

    /**
     * Get the perk name unlocked at a milestone level for this worker type.
     * Milestones are at levels 3, 6, and 9.
     */
    public String getPerkName(int milestone) {
        return switch (type) {
            case NEGOTIATOR -> switch (milestone) {
                case 3 -> "Bulk Pricing";
                case 6 -> "Silver Tongue";
                case 9 -> "Trade Mastery";
                default -> null;
            };
            case TRADING_CART -> switch (milestone) {
                case 3 -> "Quick Loading";
                case 6 -> "Shortcut Finder";
                case 9 -> "Express Routes";
                default -> null;
            };
            case BOOKKEEPER -> switch (milestone) {
                case 3 -> "Penny Pincher";
                case 6 -> "Market Analyst";
                case 9 -> "Financial Advisor";
                default -> null;
            };
        };
    }

    /**
     * Get a short description for a perk.
     */
    public String getPerkDescription(int milestone) {
        return switch (type) {
            case NEGOTIATOR -> switch (milestone) {
                case 3 -> "Negotiation bonus is more consistent";
                case 6 -> "Bonus also improves buy order prices";
                case 9 -> "Rare chance of double negotiation bonus";
                default -> "";
            };
            case TRADING_CART -> switch (milestone) {
                case 3 -> "Shipments depart 10% faster";
                case 6 -> "Speed bonus increased by 5%";
                case 9 -> "Rare chance of instant delivery";
                default -> "";
            };
            case BOOKKEEPER -> switch (milestone) {
                case 3 -> "Cost reduction applies to own wages";
                case 6 -> "Adds +1 CP flat bonus per trip";
                case 9 -> "Additional 5% cost reduction";
                default -> "";
            };
        };
    }

    /**
     * Check if a perk milestone has been reached.
     */
    public boolean hasPerk(int milestone) {
        return level >= milestone;
    }

    // ==================== Bonuses ====================

    /**
     * Get the hire cost to initially hire this worker (one-time, in CP).
     */
    public int getHireCost() {
        return switch (type) {
            case NEGOTIATOR -> ModConfig.negotiatorHireCost;
            case TRADING_CART -> ModConfig.cartHireCost;
            case BOOKKEEPER -> ModConfig.bookkeeperHireCost;
        };
    }

    /**
     * Get partial refund when firing a worker (50% of hire cost).
     */
    public int getFireRefund() {
        return getHireCost() / 2;
    }

    /**
     * Per-trip cost deducted from shipment earnings (in CP).
     * Scales with level.
     */
    public int getPerTripCost() {
        return switch (type) {
            case NEGOTIATOR -> 26 + level * 5;     // 31-76 CP per trip
            case TRADING_CART -> 13 + level * 3;    // 16-43 CP per trip
            case BOOKKEEPER -> 15 + level * 3;      // 18-45 CP per trip
        };
    }

    /**
     * Negotiator: percentage bonus to sale prices.
     * Ranges from 5% at level 1 to max bonus at max level.
     */
    public double getNegotiationBonus() {
        if (type != WorkerType.NEGOTIATOR) return 0.0;
        double maxBonus = ModConfig.negotiatorMaxBonus;
        int maxLvl = getMaxLevel();
        double baseBonus = maxBonus * 0.2; // 20% of max at level 1
        double perLevel = (maxBonus - baseBonus) / Math.max(1, maxLvl - 1);
        return baseBonus + (level - 1) * perLevel;
    }

    /**
     * Trading Cart: percentage reduction in travel time.
     * Ranges from ~10% at level 1 to max bonus at max level.
     */
    public double getSpeedBonus() {
        if (type != WorkerType.TRADING_CART) return 0.0;
        double maxBonus = ModConfig.cartMaxSpeedBonus;
        int maxLvl = getMaxLevel();
        double baseBonus = maxBonus * 0.25; // 25% of max at level 1
        double perLevel = (maxBonus - baseBonus) / Math.max(1, maxLvl - 1);
        return baseBonus + (level - 1) * perLevel;
    }

    /**
     * Bookkeeper: percentage reduction in per-trip worker costs.
     * Ranges from ~8% at level 1 to max bonus at max level.
     */
    public double getCostReductionBonus() {
        if (type != WorkerType.BOOKKEEPER) return 0.0;
        double maxBonus = ModConfig.bookkeeperMaxCostReduction;
        int maxLvl = getMaxLevel();
        double baseBonus = maxBonus * 0.23; // ~23% of max at level 1
        double perLevel = (maxBonus - baseBonus) / Math.max(1, maxLvl - 1);
        double bonus = baseBonus + (level - 1) * perLevel;
        // Level 9 perk: additional 5% cost reduction
        if (hasPerk(9)) bonus += 0.05;
        return Math.min(bonus, 0.95); // cap at 95%
    }

    /**
     * Get a display string for the current bonus.
     */
    public String getBonusDisplay() {
        return switch (type) {
            case NEGOTIATOR -> "+" + (int)(getNegotiationBonus() * 100) + "% sale price";
            case TRADING_CART -> "-" + (int)(getSpeedBonus() * 100) + "% travel time";
            case BOOKKEEPER -> "-" + (int)(getCostReductionBonus() * 100) + "% worker costs";
        };
    }

    /**
     * Get a preview of the next level's bonus.
     */
    public String getNextLevelBonusPreview() {
        if (level >= getMaxLevel()) return "MAX";
        // Temporarily compute next level's bonus
        int origLevel = this.level;
        this.level = origLevel + 1;
        String preview = getBonusDisplay();
        this.level = origLevel;
        return preview;
    }

    /**
     * Get lifetime bonus display text.
     */
    public String getLifetimeBonusDisplay() {
        return switch (type) {
            case NEGOTIATOR -> "Total extra earned";
            case TRADING_CART -> "Total time saved";
            case BOOKKEEPER -> "Total costs saved";
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
        tag.putLong("LifetimeBonus", lifetimeBonusValue);
        return tag;
    }

    public static Worker load(CompoundTag tag) {
        WorkerType type;
        try {
            type = WorkerType.valueOf(tag.getString("Type"));
        } catch (IllegalArgumentException e) {
            type = WorkerType.NEGOTIATOR; // fallback for invalid types
        }
        Worker worker = new Worker(type);
        worker.level = Math.max(1, tag.getInt("Level"));
        worker.xp = tag.getInt("Xp");
        worker.totalTrips = tag.getInt("Trips");
        worker.hired = tag.getBoolean("Hired");
        worker.lifetimeBonusValue = tag.getLong("LifetimeBonus");
        return worker;
    }
}
