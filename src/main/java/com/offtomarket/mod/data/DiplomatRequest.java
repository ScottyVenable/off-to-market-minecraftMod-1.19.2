package com.offtomarket.mod.data;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.registries.ForgeRegistries;

import java.util.UUID;

/**
 * Represents a Trade Diplomat request — the player requests specific items from a town.
 * This is a "reverse quest": the player pays a premium to have a town supply specific items.
 * 
 * Requirements:
 * - The requested item must be within the town's specialty or surplus domain.
 * - Costs more than normal market purchases (diplomat premium).
 * - Items take travel time to arrive based on town distance.
 * 
 * Stages:
 * 1. TRAVELING_TO - Diplomat is traveling to the town
 * 2. DISCUSSING - Diplomat is negotiating price (pauses here for player accept/decline)
 * 3. WAITING_FOR_GOODS - Town is preparing the goods
 * 4. TRAVELING_BACK - Diplomat is returning with goods
 * 5. ARRIVED - Ready for collection
 */
public class DiplomatRequest {
    public enum Status {
        /** Diplomat is traveling to the town. */
        TRAVELING_TO,
        /** Diplomat is discussing/negotiating - pauses for player to accept/decline the proposed price. */
        DISCUSSING,
        /** Town is preparing the goods after player accepted. */
        WAITING_FOR_GOODS,
        /** Diplomat is traveling back with the goods. */
        TRAVELING_BACK,
        /** Items have arrived and are waiting for collection. */
        ARRIVED,
        /** Request was declined or failed. */
        FAILED
    }

    private final UUID id;
    private final String townId;
    private final ResourceLocation requestedItemId;
    private final String itemDisplayName;
    private final int requestedCount;
    private final long requestTime;
    
    // Timing milestones (set during creation/progression)
    private long travelToEndTime;    // When TRAVELING_TO ends
    private long discussingEndTime;  // When DISCUSSING would auto-expire (if not accepted)
    private long waitingEndTime;     // When WAITING_FOR_GOODS ends
    private long returnEndTime;      // When TRAVELING_BACK ends (arrival)
    
    // Pricing - set after negotiation
    private int proposedPrice;       // Price proposed by the town (player must accept/decline)
    private int diplomatPremium;     // Premium portion of the cost
    private int finalCost;           // Actual cost if accepted
    
    private Status status;

    public DiplomatRequest(UUID id, String townId, ResourceLocation requestedItemId,
                           String itemDisplayName, int requestedCount, long requestTime) {
        this.id = id;
        this.townId = townId;
        this.requestedItemId = requestedItemId;
        this.itemDisplayName = itemDisplayName;
        this.requestedCount = requestedCount;
        this.requestTime = requestTime;
        this.status = Status.TRAVELING_TO;
        this.proposedPrice = 0;
        this.diplomatPremium = 0;
        this.finalCost = 0;
    }

    // ==================== Getters ====================

    public UUID getId() { return id; }
    public String getTownId() { return townId; }
    public ResourceLocation getRequestedItemId() { return requestedItemId; }
    public String getItemDisplayName() { return itemDisplayName; }
    public int getRequestedCount() { return requestedCount; }
    public long getRequestTime() { return requestTime; }
    public Status getStatus() { return status; }
    
    public long getTravelToEndTime() { return travelToEndTime; }
    public long getDiscussingEndTime() { return discussingEndTime; }
    public long getWaitingEndTime() { return waitingEndTime; }
    public long getReturnEndTime() { return returnEndTime; }
    
    public int getProposedPrice() { return proposedPrice; }
    public int getDiplomatPremium() { return diplomatPremium; }
    public int getFinalCost() { return finalCost; }

    // ==================== Setters ====================

    public void setStatus(Status status) { this.status = status; }
    
    public void setTimings(long travelToEnd, long discussingEnd, long waitingEnd, long returnEnd) {
        this.travelToEndTime = travelToEnd;
        this.discussingEndTime = discussingEnd;
        this.waitingEndTime = waitingEnd;
        this.returnEndTime = returnEnd;
    }
    
    public void setPricing(int proposedPrice, int diplomatPremium) {
        this.proposedPrice = proposedPrice;
        this.diplomatPremium = diplomatPremium;
        this.finalCost = proposedPrice;
    }

    /**
     * Get remaining ticks until the current stage ends.
     */
    public long getTicksRemaining(long currentTime) {
        return switch (status) {
            case TRAVELING_TO -> Math.max(0, travelToEndTime - currentTime);
            case DISCUSSING -> Math.max(0, discussingEndTime - currentTime);
            case WAITING_FOR_GOODS -> Math.max(0, waitingEndTime - currentTime);
            case TRAVELING_BACK -> Math.max(0, returnEndTime - currentTime);
            default -> 0;
        };
    }
    
    /**
     * Get progress fraction (0-1) for the current stage.
     */
    public float getStageProgress(long currentTime) {
        long start, end;
        switch (status) {
            case TRAVELING_TO -> {
                start = requestTime;
                end = travelToEndTime;
            }
            case WAITING_FOR_GOODS -> {
                start = discussingEndTime; // After discussing
                end = waitingEndTime;
            }
            case TRAVELING_BACK -> {
                start = waitingEndTime;
                end = returnEndTime;
            }
            default -> {
                return 0;
            }
        }
        if (end <= start) return 1.0f;
        long elapsed = currentTime - start;
        return Math.min(1.0f, Math.max(0, (float) elapsed / (end - start)));
    }

    /**
     * Calculate the diplomat premium multiplier.
     * Base premium is 50-100% depending on town type.
     * Additional random variance of ±20% is applied during negotiation.
     */
    public static double getDiplomatPremiumMultiplier(TownData town) {
        return switch (town.getType()) {
            case VILLAGE -> 1.5;  // 50% premium
            case TOWN -> 1.65;   // 65% premium
            case CITY -> 1.8;    // 80% premium
            case MARKET -> 1.7;  // 70% premium - good trade hub
            case OUTPOST -> 1.4; // 40% premium - remote location
        };
    }
    
    /**
     * Calculate a negotiated price with random variance.
     * Returns a price that varies ±20% from the base premium price.
     */
    public static int calculateNegotiatedPrice(int basePrice, TownData town, java.util.Random random) {
        double baseMult = getDiplomatPremiumMultiplier(town);
        // Random variance: 0.8 to 1.2 of the premium multiplier
        double variance = 0.8 + random.nextDouble() * 0.4;
        double finalMult = 1.0 + (baseMult - 1.0) * variance;
        return (int) (basePrice * finalMult);
    }

    /**
     * Check if a town can supply the requested item (must be in surplus or specialty).
     */
    public static boolean canTownSupply(TownData town, ResourceLocation itemId) {
        return town.getSurplus().contains(itemId) || town.getSpecialtyItems().contains(itemId);
    }

    /**
     * Create an ItemStack for the requested item.
     */
    public ItemStack createStack() {
        Item item = ForgeRegistries.ITEMS.getValue(requestedItemId);
        if (item == null) return ItemStack.EMPTY;
        return new ItemStack(item, requestedCount);
    }

    // ==================== NBT ====================

    public CompoundTag save() {
        CompoundTag tag = new CompoundTag();
        tag.putUUID("Id", id);
        tag.putString("Town", townId);
        tag.putString("Item", requestedItemId.toString());
        tag.putString("Name", itemDisplayName);
        tag.putInt("Count", requestedCount);
        tag.putLong("RequestTime", requestTime);
        tag.putLong("TravelToEnd", travelToEndTime);
        tag.putLong("DiscussingEnd", discussingEndTime);
        tag.putLong("WaitingEnd", waitingEndTime);
        tag.putLong("ReturnEnd", returnEndTime);
        tag.putInt("ProposedPrice", proposedPrice);
        tag.putInt("Premium", diplomatPremium);
        tag.putInt("FinalCost", finalCost);
        tag.putString("Status", status.name());
        return tag;
    }

    public static DiplomatRequest load(CompoundTag tag) {
        DiplomatRequest req = new DiplomatRequest(
                tag.getUUID("Id"),
                tag.getString("Town"),
                new ResourceLocation(tag.getString("Item")),
                tag.getString("Name"),
                tag.getInt("Count"),
                tag.getLong("RequestTime")
        );
        req.travelToEndTime = tag.getLong("TravelToEnd");
        req.discussingEndTime = tag.getLong("DiscussingEnd");
        req.waitingEndTime = tag.getLong("WaitingEnd");
        req.returnEndTime = tag.getLong("ReturnEnd");
        req.proposedPrice = tag.getInt("ProposedPrice");
        req.diplomatPremium = tag.getInt("Premium");
        req.finalCost = tag.getInt("FinalCost");
        req.status = Status.valueOf(tag.getString("Status"));
        return req;
    }
}
