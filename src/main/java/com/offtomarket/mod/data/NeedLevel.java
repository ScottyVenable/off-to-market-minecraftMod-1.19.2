package com.offtomarket.mod.data;

/**
 * Represents how badly a town needs a particular category of items.
 * <p>
 * Replaces the old binary needs/surplus system with a graduated 6-tier
 * scale that affects both price multipliers and sale speed.
 * <p>
 * <b>Tiers (from most demand to least):</b>
 * <pre>
 *   DESPERATE       → 2.00× price  (crisis-level shortage)
 *   HIGH_NEED       → 1.50× price  (significant demand)
 *   MODERATE_NEED   → 1.25× price  (noticeable demand)
 *   BALANCED        → 1.00× price  (fair market equilibrium)
 *   SURPLUS         → 0.75× price  (oversupplied, lower prices)
 *   OVERSATURATED   → 0.50× price  (flooded, hard to sell at all)
 * </pre>
 */
public enum NeedLevel {

    DESPERATE      ("Desperate",       2.00, 0xFF5555, "Crisis-level shortage"),
    HIGH_NEED      ("High Need",       1.50, 0xFF8800, "Significant demand"),
    MODERATE_NEED  ("Moderate Need",   1.25, 0xFFFF55, "Noticeable demand"),
    BALANCED       ("Balanced",        1.00, 0x55FF55, "Fair market equilibrium"),
    SURPLUS        ("Surplus",         0.75, 0x55AAFF, "Oversupplied"),
    OVERSATURATED  ("Oversaturated",   0.50, 0xAAAAAA, "Market is flooded");

    private final String displayName;
    private final double priceMultiplier;
    private final int color;
    private final String description;

    NeedLevel(String displayName, double priceMultiplier, int color, String description) {
        this.displayName = displayName;
        this.priceMultiplier = priceMultiplier;
        this.color = color;
        this.description = description;
    }

    /** Display name for UI rendering (e.g., "High Need"). */
    public String getDisplayName() { return displayName; }

    /** Price multiplier applied to items in this need category. */
    public double getPriceMultiplier() { return priceMultiplier; }

    /** Color for UI rendering (0xRRGGBB). */
    public int getColor() { return color; }

    /** Short description for tooltips. */
    public String getDescription() { return description; }

    /**
     * Determine the NeedLevel from a numeric supply value.
     * <p>
     * Supply thresholds:
     * <ul>
     *   <li>0–19: DESPERATE (nearly out of stock)</li>
     *   <li>20–39: HIGH_NEED</li>
     *   <li>40–59: MODERATE_NEED</li>
     *   <li>60–79: BALANCED</li>
     *   <li>80–99: SURPLUS</li>
     *   <li>100+: OVERSATURATED</li>
     * </ul>
     *
     * @param supplyLevel The town's current supply count for an item category.
     * @return The corresponding NeedLevel.
     */
    public static NeedLevel fromSupplyLevel(int supplyLevel) {
        if (supplyLevel < 20) return DESPERATE;
        if (supplyLevel < 40) return HIGH_NEED;
        if (supplyLevel < 60) return MODERATE_NEED;
        if (supplyLevel < 80) return BALANCED;
        if (supplyLevel < 100) return SURPLUS;
        return OVERSATURATED;
    }

    /**
     * Shift this NeedLevel up (more need) by the given number of steps.
     * Clamps to DESPERATE.
     */
    public NeedLevel increaseNeed(int steps) {
        int idx = Math.max(0, this.ordinal() - steps);
        return values()[idx];
    }

    /**
     * Shift this NeedLevel down (less need) by the given number of steps.
     * Clamps to OVERSATURATED.
     */
    public NeedLevel decreaseNeed(int steps) {
        int idx = Math.min(values().length - 1, this.ordinal() + steps);
        return values()[idx];
    }

    /**
     * Whether this level represents active demand (items sell faster).
     */
    public boolean isInDemand() {
        return this.ordinal() < BALANCED.ordinal();
    }

    /**
     * Whether this level represents excess supply (items sell slower).
     */
    public boolean isOversupplied() {
        return this.ordinal() > BALANCED.ordinal();
    }
}
