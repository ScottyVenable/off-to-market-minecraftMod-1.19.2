package com.offtomarket.mod.data;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.resources.ResourceLocation;

import javax.annotation.Nullable;
import java.util.*;

/**
 * Represents a town's persistent inventory of items available for sale.
 *
 * Each town maintains its own stock of items that depletes when players buy
 * and restocks periodically (daily at dawn). Prices fluctuate based on
 * purchase frequency, and production chain bonuses can increase restock
 * amounts when catalysts (tools/resources the town needs) are well-supplied.
 *
 * The inventory is stored as a list of StockSlot entries. Normal items
 * (iron ingots, wheat, etc.) have a single slot with variable quantity.
 * Unique-NBT items like enchanted books and potions each get their own
 * slot with quantity 1.
 */
public class TownInventory {

    private final String townId;
    private final List<StockSlot> slots = new ArrayList<>();
    private long lastRestockDay = -1;

    public TownInventory(String townId) {
        this.townId = townId;
    }

    public String getTownId() { return townId; }
    public List<StockSlot> getSlots() { return slots; }
    public long getLastRestockDay() { return lastRestockDay; }
    public void setLastRestockDay(long day) { this.lastRestockDay = day; }

    /**
     * Find a stock slot by its unique key.
     * Returns null if no matching slot exists.
     */
    @Nullable
    public StockSlot findSlot(String stockKey) {
        for (StockSlot slot : slots) {
            if (slot.getStockKey().equals(stockKey)) return slot;
        }
        return null;
    }

    /**
     * Remove a stock slot by key (when quantity reaches 0).
     */
    public boolean removeSlot(String stockKey) {
        return slots.removeIf(s -> s.getStockKey().equals(stockKey));
    }

    /**
     * Record a purchase: decrease stock, increase buy count.
     * Returns true if the purchase was valid (slot existed and had stock).
     */
    public boolean recordPurchase(String stockKey, int quantity) {
        StockSlot slot = findSlot(stockKey);
        if (slot == null || slot.quantity < quantity) return false;
        slot.quantity -= quantity;
        slot.buyCount += quantity;
        if (slot.quantity <= 0) {
            removeSlot(stockKey);
        }
        return true;
    }

    /**
     * Convert the current inventory to MarketListing objects for display.
     * Each slot becomes one listing. The price is computed dynamically
     * using PriceCalculator + town modifiers + demand surcharges.
     */
    public List<MarketListing> toListings(TownData town, long gameTime) {
        List<MarketListing> listings = new ArrayList<>();
        for (StockSlot slot : slots) {
            if (slot.quantity <= 0) continue;

            int price = computeSlotPrice(town, slot);

            // Apply sale discount if on sale
            boolean onSale = slot.onSale;
            int discount = slot.saleDiscount;
            if (onSale && discount > 0) {
                int basePrice = PriceCalculator.getBaseValue(slot.createSampleStack());
                int minSalePrice = Math.max(1, (int) Math.floor(basePrice * 0.5));
                price = Math.max(minSalePrice, (int) (price * (1.0 - discount / 100.0)));
            }

            listings.add(new MarketListing(
                    townId, slot.itemId, slot.displayName,
                    slot.quantity, price, gameTime,
                    onSale, discount, slot.itemNbt
            ));
        }
        return listings;
    }

    /**
     * Compute the dynamic price for a stock slot.
     *
     * Factors: base price, distance premium, town type bias, need level bias,
     * demand surcharge (from buy count), stock scarcity multiplier, random band.
     */
    private static int computeSlotPrice(TownData town, StockSlot slot) {
        net.minecraft.world.item.ItemStack sampleStack = slot.createSampleStack();
        int basePrice = PriceCalculator.getBaseValue(sampleStack);
        if (basePrice <= 0) basePrice = 1;

        net.minecraft.world.item.Item item = sampleStack.getItem();
        NeedLevel need = town.getNeedLevel(item);

        // Distance premium: +0% to +27% across distance 1..10
        double distancePremium = Math.max(0, town.getDistance() - 1) * 0.03;

        // Town role bias
        double typeBias = switch (town.getType()) {
            case VILLAGE -> -0.04;
            case TOWN -> 0.00;
            case MARKET -> 0.03;
            case OUTPOST -> 0.07;
            case CITY -> 0.10;
        };

        // Need level bias (same anti-arbitrage values as MarketListing)
        double needBias = switch (need) {
            case DESPERATE -> 1.80;
            case HIGH_NEED -> 1.55;
            case MODERATE_NEED -> 1.30;
            case BALANCED -> 1.00;
            case SURPLUS -> 0.85;
            case OVERSATURATED -> 0.70;
        };

        // Demand surcharge: each purchase since last restock raises price 3%
        double demandSurcharge = 1.0 + slot.buyCount * 0.03;
        demandSurcharge = Math.min(demandSurcharge, 2.0); // cap at 2x

        // Stock scarcity: low stock → higher prices
        double scarcityMult = 1.0;
        if (slot.maxQuantity > 0) {
            double stockRatio = (double) slot.quantity / slot.maxQuantity;
            if (stockRatio < 0.25) scarcityMult = 1.15;
            else if (stockRatio < 0.50) scarcityMult = 1.05;
        }

        double adjusted = basePrice * (1.0 + distancePremium + typeBias)
                * needBias * demandSurcharge * scarcityMult;
        int computed = Math.max(1, (int) Math.round(adjusted));

        // Floor by need level
        double floorFactor = switch (need) {
            case DESPERATE -> 1.20;
            case HIGH_NEED -> 1.10;
            case MODERATE_NEED -> 0.95;
            case BALANCED -> 0.80;
            case SURPLUS -> 0.65;
            case OVERSATURATED -> 0.50;
        };
        int floor = Math.max(1, (int) Math.floor(basePrice * floorFactor));

        return Math.max(floor, computed);
    }

    // ==================== NBT Persistence ====================

    public CompoundTag save() {
        CompoundTag tag = new CompoundTag();
        tag.putString("TownId", townId);
        tag.putLong("LastRestock", lastRestockDay);
        ListTag slotList = new ListTag();
        for (StockSlot slot : slots) {
            slotList.add(slot.save());
        }
        tag.put("Slots", slotList);
        return tag;
    }

    public static TownInventory load(CompoundTag tag) {
        TownInventory inv = new TownInventory(tag.getString("TownId"));
        inv.lastRestockDay = tag.getLong("LastRestock");
        ListTag slotList = tag.getList("Slots", Tag.TAG_COMPOUND);
        for (int i = 0; i < slotList.size(); i++) {
            inv.slots.add(StockSlot.load(slotList.getCompound(i)));
        }
        return inv;
    }

    // ==================== StockSlot ====================

    /**
     * A single entry in a town's inventory, representing a specific item
     * (or a specific variant like a particular enchanted book) with a
     * current quantity, max restock cap, and dynamic pricing state.
     */
    public static class StockSlot {
        private final String stockKey;
        private final ResourceLocation itemId;
        private String displayName;
        private int quantity;
        private int maxQuantity;
        private int buyCount;
        private boolean onSale;
        private int saleDiscount;
        @Nullable
        private CompoundTag itemNbt;

        public StockSlot(String stockKey, ResourceLocation itemId, String displayName,
                         int quantity, int maxQuantity, @Nullable CompoundTag itemNbt) {
            this.stockKey = stockKey;
            this.itemId = itemId;
            this.displayName = displayName;
            this.quantity = quantity;
            this.maxQuantity = maxQuantity;
            this.buyCount = 0;
            this.onSale = false;
            this.saleDiscount = 0;
            this.itemNbt = itemNbt;
        }

        // Getters
        public String getStockKey() { return stockKey; }
        public ResourceLocation getItemId() { return itemId; }
        public String getDisplayName() { return displayName; }
        public int getQuantity() { return quantity; }
        public int getMaxQuantity() { return maxQuantity; }
        public int getBuyCount() { return buyCount; }
        public boolean isOnSale() { return onSale; }
        public int getSaleDiscount() { return saleDiscount; }
        @Nullable
        public CompoundTag getItemNbt() { return itemNbt; }

        // Setters
        public void setQuantity(int qty) { this.quantity = Math.max(0, qty); }
        public void setMaxQuantity(int max) { this.maxQuantity = max; }
        public void setBuyCount(int count) { this.buyCount = count; }
        public void setOnSale(boolean onSale) { this.onSale = onSale; }
        public void setSaleDiscount(int discount) { this.saleDiscount = discount; }

        /**
         * Create a sample ItemStack for price calculation (quantity 1).
         * Applies NBT if present (for enchanted books, potions, etc.).
         */
        public net.minecraft.world.item.ItemStack createSampleStack() {
            net.minecraft.world.item.Item item =
                    net.minecraftforge.registries.ForgeRegistries.ITEMS.getValue(itemId);
            if (item == null) return net.minecraft.world.item.ItemStack.EMPTY;
            net.minecraft.world.item.ItemStack stack = new net.minecraft.world.item.ItemStack(item, 1);
            if (itemNbt != null) stack.setTag(itemNbt.copy());
            return stack;
        }

        // ==================== Stock Key Generation ====================

        /**
         * Generate a unique stock key for an item.
         *
         * Normal items: just the registry name ("minecraft:iron_ingot").
         * Enchanted books: includes enchantment and level ("minecraft:enchanted_book#sharpness_5").
         * Potions / tipped arrows: includes potion type ("minecraft:potion#night_vision").
         * Animal slips: includes animal type ("offtomarket:animal_trade_slip#minecraft:cow").
         */
        public static String makeStockKey(ResourceLocation itemId, @Nullable CompoundTag nbt) {
            if (nbt == null) return itemId.toString();

            // Enchanted books
            if (nbt.contains("StoredEnchantments")) {
                ListTag enchants = nbt.getList("StoredEnchantments", Tag.TAG_COMPOUND);
                if (!enchants.isEmpty()) {
                    CompoundTag first = enchants.getCompound(0);
                    return itemId + "#" + first.getString("id") + "_" + first.getInt("lvl");
                }
            }
            // Potions and tipped arrows
            if (nbt.contains("Potion")) {
                return itemId + "#" + nbt.getString("Potion");
            }
            // Animal trade slips
            if (nbt.contains("AnimalType")) {
                return itemId + "#" + nbt.getString("AnimalType");
            }
            return itemId.toString();
        }

        // ==================== NBT ====================

        public CompoundTag save() {
            CompoundTag tag = new CompoundTag();
            tag.putString("Key", stockKey);
            tag.putString("Item", itemId.toString());
            tag.putString("Name", displayName);
            tag.putInt("Qty", quantity);
            tag.putInt("Max", maxQuantity);
            tag.putInt("Buys", buyCount);
            tag.putBoolean("Sale", onSale);
            tag.putInt("Disc", saleDiscount);
            if (itemNbt != null) tag.put("Nbt", itemNbt.copy());
            return tag;
        }

        public static StockSlot load(CompoundTag tag) {
            StockSlot slot = new StockSlot(
                    tag.getString("Key"),
                    new ResourceLocation(tag.getString("Item")),
                    tag.getString("Name"),
                    tag.getInt("Qty"),
                    tag.getInt("Max"),
                    tag.contains("Nbt") ? tag.getCompound("Nbt") : null
            );
            slot.buyCount = tag.getInt("Buys");
            slot.onSale = tag.getBoolean("Sale");
            slot.saleDiscount = tag.getInt("Disc");
            return slot;
        }
    }
}
