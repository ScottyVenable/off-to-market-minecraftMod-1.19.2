package com.offtomarket.mod.data;

/**
 * Macroeconomic market climate that affects global trade dynamics across all towns.
 * Shifts periodically during daily market refresh cycles.
 */
public enum MarketClimate {
    STABLE("Stable", "Balanced markets across the realm", "\u2696", 1.0, 1.0, 0x88CC88),
    PROSPERITY("Prosperity", "+15% shipment payout value", "\u2726", 1.15, 1.0, 0xFFD700),
    INFLATION("High Volume", "+10% payout, +10% market prices", "\u25B2", 1.10, 1.10, 0xFFAA44),
    SCARCITY("Scarcity", "+25% payout on raw materials & food", "\u26A0", 1.25, 1.15, 0xFF6644),
    BUYERS_MARKET("Buyer's Market", "-15% discount on market purchases", "\u25BC", 0.95, 0.85, 0x66AAFF);

    private final String displayName;
    private final String description;
    private final String icon;
    private final double payoutMultiplier;
    private final double purchaseCostMultiplier;
    private final int color;

    MarketClimate(String displayName, String description, String icon,
                  double payoutMultiplier, double purchaseCostMultiplier, int color) {
        this.displayName = displayName;
        this.description = description;
        this.icon = icon;
        this.payoutMultiplier = payoutMultiplier;
        this.purchaseCostMultiplier = purchaseCostMultiplier;
        this.color = color;
    }

    public String getDisplayName() { return displayName; }
    public String getDescription() { return description; }
    public String getIcon() { return icon; }
    public double getPayoutMultiplier() { return payoutMultiplier; }
    public double getPurchaseCostMultiplier() { return purchaseCostMultiplier; }
    public int getColor() { return color; }

    public static MarketClimate fromName(String name) {
        if (name == null || name.isBlank()) return STABLE;
        try {
            return valueOf(name.toUpperCase());
        } catch (IllegalArgumentException e) {
            return STABLE;
        }
    }
}
