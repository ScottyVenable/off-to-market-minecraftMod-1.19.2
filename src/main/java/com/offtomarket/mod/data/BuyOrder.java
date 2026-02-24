package com.offtomarket.mod.data;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.registries.ForgeRegistries;

import java.util.UUID;

/**
 * Represents a purchase order from the market board.
 * Items are purchased and then travel from the town to the player's Trading Post.
 * Once arrived, the player can collect the items.
 */
public class BuyOrder {
    public enum Status {
        /** Items are being shipped from the town. */
        IN_TRANSIT,
        /** Items have arrived and are waiting for collection. */
        ARRIVED
    }

    private final UUID id;
    private final String townId;
    private final ResourceLocation itemId;
    private final String itemDisplayName;
    private final int count;
    private final int totalPaid; // in copper pieces (already deducted from player)
    private final long orderTime;
    private final long arrivalTime;
    private Status status;
    @javax.annotation.Nullable
    private final CompoundTag itemNbt; // optional NBT for special items (enchanted books)

    public BuyOrder(UUID id, String townId, ResourceLocation itemId, String itemDisplayName,
                    int count, int totalPaid, long orderTime, long arrivalTime) {
        this(id, townId, itemId, itemDisplayName, count, totalPaid, orderTime, arrivalTime, null);
    }

    public BuyOrder(UUID id, String townId, ResourceLocation itemId, String itemDisplayName,
                    int count, int totalPaid, long orderTime, long arrivalTime,
                    @javax.annotation.Nullable CompoundTag itemNbt) {
        this.id = id;
        this.townId = townId;
        this.itemId = itemId;
        this.itemDisplayName = itemDisplayName;
        this.count = count;
        this.totalPaid = totalPaid;
        this.orderTime = orderTime;
        this.arrivalTime = arrivalTime;
        this.status = Status.IN_TRANSIT;
        this.itemNbt = itemNbt;
    }

    // ==================== Getters ====================

    public UUID getId() { return id; }
    public String getTownId() { return townId; }
    public ResourceLocation getItemId() { return itemId; }
    public String getItemDisplayName() { return itemDisplayName; }
    public int getCount() { return count; }
    public int getTotalPaid() { return totalPaid; }
    public long getOrderTime() { return orderTime; }
    public long getArrivalTime() { return arrivalTime; }
    public Status getStatus() { return status; }
    @javax.annotation.Nullable
    public CompoundTag getItemNbt() { return itemNbt; }

    public void setStatus(Status status) { this.status = status; }

    /**
     * Get how many ticks until this order arrives.
     */
    public long getTicksUntilArrival(long currentTime) {
        return Math.max(0, arrivalTime - currentTime);
    }

    /**
     * Create an ItemStack for the ordered item.
     */
    public ItemStack createStack() {
        Item item = ForgeRegistries.ITEMS.getValue(itemId);
        if (item == null) return ItemStack.EMPTY;
        ItemStack stack = new ItemStack(item, count);
        if (itemNbt != null) stack.setTag(itemNbt.copy());
        return stack;
    }

    // ==================== NBT ====================

    public CompoundTag save() {
        CompoundTag tag = new CompoundTag();
        tag.putUUID("Id", id);
        tag.putString("Town", townId);
        tag.putString("Item", itemId.toString());
        tag.putString("Name", itemDisplayName);
        tag.putInt("Count", count);
        tag.putInt("Paid", totalPaid);
        tag.putLong("OrderTime", orderTime);
        tag.putLong("Arrival", arrivalTime);
        tag.putString("Status", status.name());
        if (itemNbt != null) tag.put("ItemNbt", itemNbt.copy());
        return tag;
    }

    public static BuyOrder load(CompoundTag tag) {
        BuyOrder order = new BuyOrder(
                tag.getUUID("Id"),
                tag.getString("Town"),
                new ResourceLocation(tag.getString("Item")),
                tag.getString("Name"),
                tag.getInt("Count"),
                tag.getInt("Paid"),
                tag.getLong("OrderTime"),
                tag.getLong("Arrival"),
                tag.contains("ItemNbt") ? tag.getCompound("ItemNbt") : null
        );
        order.status = Status.valueOf(tag.getString("Status"));
        return order;
    }
}
