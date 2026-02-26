package com.offtomarket.mod.data;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;

import java.util.ArrayList;
import java.util.List;

/**
 * A snapshot of a town's stock taken by a Stock Scout worker.
 *
 * Detail levels (controlled by scout level):
 *   Basic  (Lv 1-2): totalUniqueItems, totalStock, stockHealth
 *   Good   (Lv 3-5): + top items with quantities
 *   Great  (Lv 6-8): + prices revealed, sale flags
 *   Full   (Lv 9+):  + best-deal markers, rare stock alerts
 *
 * Reports become stale over time. freshness is measured as
 * game ticks since scoutedAt.
 */
public class ScoutReport {

    /** How many game ticks before a report is considered stale (2 in-game days). */
    public static final long STALE_THRESHOLD = 24000 * 2;

    private final String townId;
    private long scoutedAt;           // game time when the scout returned
    private int totalUniqueItems;     // number of distinct items in stock
    private int totalStock;           // sum of all item quantities
    private StockHealth stockHealth;  // overall stock level
    private final List<ReportEntry> entries = new ArrayList<>();

    // Scout metadata at time of report
    private int scoutLevel;           // level of the scout when report was generated
    private boolean pricesRevealed;   // whether prices are included
    private boolean bestDealsRevealed;// whether best-deal flags are included

    public ScoutReport(String townId) {
        this.townId = townId;
    }

    // ==================== Stock Health ====================

    public enum StockHealth {
        EMPTY("Empty", 0xFF4444),
        LOW("Low Stock", 0xFFAA44),
        MODERATE("Moderate", 0xCCAA44),
        WELL_STOCKED("Well Stocked", 0x88CC88),
        ABUNDANT("Abundant", 0x44CC44);

        private final String displayName;
        private final int color;

        StockHealth(String displayName, int color) {
            this.displayName = displayName;
            this.color = color;
        }

        public String getDisplayName() { return displayName; }
        public int getColor() { return color; }

        public static StockHealth fromStock(int totalStock, int uniqueItems) {
            if (totalStock == 0) return EMPTY;
            double avgPerItem = uniqueItems > 0 ? (double) totalStock / uniqueItems : 0;
            if (avgPerItem < 3) return LOW;
            if (avgPerItem < 8) return MODERATE;
            if (avgPerItem < 20) return WELL_STOCKED;
            return ABUNDANT;
        }
    }

    // ==================== Report Entry ====================

    /**
     * A single item entry in a scout report.
     */
    public static class ReportEntry {
        private final String itemName;
        private final String itemId;
        private int quantity;
        private int price;        // 0 if prices not revealed
        private boolean onSale;
        private boolean bestDeal; // flagged by Master Appraiser perk

        public ReportEntry(String itemName, String itemId, int quantity, int price,
                           boolean onSale, boolean bestDeal) {
            this.itemName = itemName;
            this.itemId = itemId;
            this.quantity = quantity;
            this.price = price;
            this.onSale = onSale;
            this.bestDeal = bestDeal;
        }

        public String getItemName() { return itemName; }
        public String getItemId() { return itemId; }
        public int getQuantity() { return quantity; }
        public int getPrice() { return price; }
        public boolean isOnSale() { return onSale; }
        public boolean isBestDeal() { return bestDeal; }

        public CompoundTag save() {
            CompoundTag tag = new CompoundTag();
            tag.putString("Name", itemName);
            tag.putString("Id", itemId);
            tag.putInt("Qty", quantity);
            tag.putInt("Price", price);
            tag.putBoolean("Sale", onSale);
            tag.putBoolean("Best", bestDeal);
            return tag;
        }

        public static ReportEntry load(CompoundTag tag) {
            return new ReportEntry(
                    tag.getString("Name"),
                    tag.getString("Id"),
                    tag.getInt("Qty"),
                    tag.getInt("Price"),
                    tag.getBoolean("Sale"),
                    tag.getBoolean("Best")
            );
        }
    }

    // ==================== Getters ====================

    public String getTownId() { return townId; }
    public long getScoutedAt() { return scoutedAt; }
    public int getTotalUniqueItems() { return totalUniqueItems; }
    public int getTotalStock() { return totalStock; }
    public StockHealth getStockHealth() { return stockHealth; }
    public List<ReportEntry> getEntries() { return entries; }
    public int getScoutLevel() { return scoutLevel; }
    public boolean arePricesRevealed() { return pricesRevealed; }
    public boolean areBestDealsRevealed() { return bestDealsRevealed; }

    // ==================== Setters (used during report generation) ====================

    public void setScoutedAt(long scoutedAt) { this.scoutedAt = scoutedAt; }
    public void setTotalUniqueItems(int total) { this.totalUniqueItems = total; }
    public void setTotalStock(int total) { this.totalStock = total; }
    public void setStockHealth(StockHealth health) { this.stockHealth = health; }
    public void setScoutLevel(int level) { this.scoutLevel = level; }
    public void setPricesRevealed(boolean revealed) { this.pricesRevealed = revealed; }
    public void setBestDealsRevealed(boolean revealed) { this.bestDealsRevealed = revealed; }

    /**
     * Check if this report is stale (older than STALE_THRESHOLD ticks).
     */
    public boolean isStale(long currentGameTime) {
        return (currentGameTime - scoutedAt) > STALE_THRESHOLD;
    }

    /**
     * Get a freshness label for display.
     */
    public String getFreshnessLabel(long currentGameTime) {
        long age = currentGameTime - scoutedAt;
        if (age < 1200) return "Just now";       // < 1 minute
        if (age < 6000) return "Recent";          // < 5 minutes
        if (age < 12000) return "Earlier today";  // < 10 minutes
        if (age < 24000) return "This morning";   // < 1 day
        if (age < STALE_THRESHOLD) return "Yesterday";
        return "Outdated";
    }

    /**
     * Get color for freshness display.
     */
    public int getFreshnessColor(long currentGameTime) {
        long age = currentGameTime - scoutedAt;
        if (age < 6000) return 0x88CC88;   // green
        if (age < 12000) return 0xCCAA44;  // gold
        if (age < 24000) return 0xFFAA44;  // orange
        return 0xFF6666;                    // red
    }

    // ==================== NBT ====================

    public CompoundTag save() {
        CompoundTag tag = new CompoundTag();
        tag.putString("Town", townId);
        tag.putLong("At", scoutedAt);
        tag.putInt("Unique", totalUniqueItems);
        tag.putInt("Total", totalStock);
        tag.putString("Health", stockHealth != null ? stockHealth.name() : "EMPTY");
        tag.putInt("ScoutLvl", scoutLevel);
        tag.putBoolean("Prices", pricesRevealed);
        tag.putBoolean("BestDeals", bestDealsRevealed);

        ListTag entryList = new ListTag();
        for (ReportEntry entry : entries) {
            entryList.add(entry.save());
        }
        tag.put("Entries", entryList);
        return tag;
    }

    public static ScoutReport load(CompoundTag tag) {
        ScoutReport report = new ScoutReport(tag.getString("Town"));
        report.scoutedAt = tag.getLong("At");
        report.totalUniqueItems = tag.getInt("Unique");
        report.totalStock = tag.getInt("Total");
        try {
            report.stockHealth = StockHealth.valueOf(tag.getString("Health"));
        } catch (IllegalArgumentException e) {
            report.stockHealth = StockHealth.EMPTY;
        }
        report.scoutLevel = tag.getInt("ScoutLvl");
        report.pricesRevealed = tag.getBoolean("Prices");
        report.bestDealsRevealed = tag.getBoolean("BestDeals");

        ListTag entryList = tag.getList("Entries", Tag.TAG_COMPOUND);
        for (int i = 0; i < entryList.size(); i++) {
            report.entries.add(ReportEntry.load(entryList.getCompound(i)));
        }
        return report;
    }
}
