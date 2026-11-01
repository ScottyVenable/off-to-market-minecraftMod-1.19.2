package com.offtomarket.mod.data;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.resources.ResourceLocation;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

/**
 * Represents a player-defined custom trade order (trade goal).
 * Players can set target items and quantities for a town,
 * track progress as containers fill, and dispatch manually or automatically.
 */
public class CustomTradeOrder {

    private final UUID id;
    private final String townId;
    private final List<OrderItem> items = new ArrayList<>();
    private boolean autoDispatch;
    private final long createdTime;

    public static class OrderItem {
        private final ResourceLocation itemId;
        private final String displayName;
        private final int targetQuantity;
        private int currentFilled;

        public OrderItem(ResourceLocation itemId, String displayName, int targetQuantity, int currentFilled) {
            this.itemId = itemId;
            this.displayName = displayName;
            this.targetQuantity = Math.max(1, targetQuantity);
            this.currentFilled = Math.max(0, currentFilled);
        }

        public ResourceLocation getItemId() { return itemId; }
        public String getDisplayName() { return displayName; }
        public int getTargetQuantity() { return targetQuantity; }
        public int getCurrentFilled() { return currentFilled; }
        public void setCurrentFilled(int count) { this.currentFilled = Math.max(0, count); }
        public boolean isFulfilled() { return currentFilled >= targetQuantity; }

        public float getProgress() {
            return (float) Math.min(currentFilled, targetQuantity) / (float) targetQuantity;
        }

        public CompoundTag save() {
            CompoundTag tag = new CompoundTag();
            tag.putString("Item", itemId.toString());
            tag.putString("Name", displayName);
            tag.putInt("Target", targetQuantity);
            tag.putInt("Filled", currentFilled);
            return tag;
        }

        public static OrderItem load(CompoundTag tag) {
            return new OrderItem(
                    new ResourceLocation(tag.getString("Item")),
                    tag.getString("Name"),
                    tag.getInt("Target"),
                    tag.getInt("Filled")
            );
        }
    }

    public CustomTradeOrder(UUID id, String townId, List<OrderItem> items, boolean autoDispatch, long createdTime) {
        this.id = id != null ? id : UUID.randomUUID();
        this.townId = townId != null ? townId : "";
        if (items != null) this.items.addAll(items);
        this.autoDispatch = autoDispatch;
        this.createdTime = createdTime;
    }

    public UUID getId() { return id; }
    public String getTownId() { return townId; }
    public List<OrderItem> getItems() { return Collections.unmodifiableList(items); }
    public boolean isAutoDispatch() { return autoDispatch; }
    public void setAutoDispatch(boolean autoDispatch) { this.autoDispatch = autoDispatch; }
    public long getCreatedTime() { return createdTime; }

    public boolean isAllFulfilled() {
        if (items.isEmpty()) return false;
        for (OrderItem item : items) {
            if (!item.isFulfilled()) return false;
        }
        return true;
    }

    public float getOverallProgress() {
        if (items.isEmpty()) return 0.0f;
        int totalTarget = 0;
        int totalFilled = 0;
        for (OrderItem item : items) {
            totalTarget += item.getTargetQuantity();
            totalFilled += Math.min(item.getCurrentFilled(), item.getTargetQuantity());
        }
        return totalTarget > 0 ? (float) totalFilled / (float) totalTarget : 0.0f;
    }

    public CompoundTag save() {
        CompoundTag tag = new CompoundTag();
        tag.putUUID("Id", id);
        tag.putString("Town", townId);
        tag.putBoolean("AutoDispatch", autoDispatch);
        tag.putLong("Created", createdTime);
        ListTag list = new ListTag();
        for (OrderItem item : items) {
            list.add(item.save());
        }
        tag.put("Items", list);
        return tag;
    }

    public static CustomTradeOrder load(CompoundTag tag) {
        UUID id = tag.hasUUID("Id") ? tag.getUUID("Id") : UUID.randomUUID();
        String townId = tag.getString("Town");
        boolean autoDispatch = tag.getBoolean("AutoDispatch");
        long created = tag.getLong("Created");
        List<OrderItem> items = new ArrayList<>();
        if (tag.contains("Items")) {
            ListTag list = tag.getList("Items", Tag.TAG_COMPOUND);
            for (int i = 0; i < list.size(); i++) {
                items.add(OrderItem.load(list.getCompound(i)));
            }
        }
        return new CustomTradeOrder(id, townId, items, autoDispatch, created);
    }
}
