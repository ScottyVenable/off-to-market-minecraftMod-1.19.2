package com.offtomarket.mod.item;

/**
 * Currency denominations.
 * 1 GP = 10 SP = 100 CP.
 */
public enum CoinType {
    GOLD("gold", 100),
    SILVER("silver", 10),
    COPPER("copper", 1);

    private final String name;
    private final int value; // value in copper pieces

    CoinType(String name, int value) {
        this.name = name;
        this.value = value;
    }

    public String getName() { return name; }
    public int getValue() { return value; }

    /**
     * Convert a total value in copper pieces to a readable currency string.
     * e.g., 253 CP → "2 GP 5 SP 3 CP"
     */
    public static String formatValue(int copperPieces) {
        // Check gold-only mode via config directly to avoid circular deps
        if (com.offtomarket.mod.config.ModConfig.goldOnlyMode) {
            int gp = Math.max(1, (copperPieces + 99) / 100);
            return gp + " GP";
        }
        int gp = copperPieces / 100;
        int remainder = copperPieces % 100;
        int sp = remainder / 10;
        int cp = remainder % 10;

        StringBuilder sb = new StringBuilder();
        if (gp > 0) sb.append(gp).append(" GP ");
        if (sp > 0) sb.append(sp).append(" SP ");
        if (cp > 0 || sb.length() == 0) sb.append(cp).append(" CP");
        return sb.toString().trim();
    }
}
