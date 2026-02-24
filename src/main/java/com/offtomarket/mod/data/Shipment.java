package com.offtomarket.mod.data;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.registries.ForgeRegistries;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Represents items that have been shipped to a market town.
 * Tracks: what was shipped, which town, when it departs, when it arrives,
 * current status, and how long items have been listed at the market.
 */
public class Shipment {
    public enum Status {
        /** Items are in transit to the market. */
        IN_TRANSIT,
        /** Items have arrived at the market and are listed for sale. */
        AT_MARKET,
        /** Items have been sold. Coins are in transit back. */
        SOLD,
        /** Return requested: items are being shipped back. */
        RETURNING,
        /** Coins have arrived back at the Trading Post (after SOLD). */
        COMPLETED,
        /** Cancelled items have arrived back (click to collect). */
        RETURNED
    }

    private final UUID id;
    private final String townId;
    private final List<ShipmentItem> items;
    private final long departureTime;    // game time when shipped
    private final long arrivalTime;      // game time when it arrives at market
    private long marketListedTime;       // game time when listed at market
    private long soldTime;               // game time when sold
    private long returnArrivalTime;      // game time when items return
    private Status status;
    private int totalEarnings;           // in copper pieces, if sold

    public Shipment(UUID id, String townId, List<ShipmentItem> items,
                    long departureTime, long arrivalTime) {
        this.id = id;
        this.townId = townId;
        this.items = items;
        this.departureTime = departureTime;
        this.arrivalTime = arrivalTime;
        this.status = Status.IN_TRANSIT;
        this.totalEarnings = 0;
    }

    public UUID getId() { return id; }
    public String getTownId() { return townId; }
    public List<ShipmentItem> getItems() { return items; }
    public long getDepartureTime() { return departureTime; }
    public long getArrivalTime() { return arrivalTime; }
    public long getMarketListedTime() { return marketListedTime; }
    public long getSoldTime() { return soldTime; }
    public long getReturnArrivalTime() { return returnArrivalTime; }
    public Status getStatus() { return status; }
    public int getTotalEarnings() { return totalEarnings; }

    public void setStatus(Status status) { this.status = status; }
    public void setMarketListedTime(long time) { this.marketListedTime = time; }
    public void setSoldTime(long time) { this.soldTime = time; }
    public void setReturnArrivalTime(long time) { this.returnArrivalTime = time; }
    public void setTotalEarnings(int earnings) { this.totalEarnings = earnings; }

    /**
     * Get how long until arrival at market (in ticks).
     */
    public long getTicksUntilArrival(long currentTime) {
        return Math.max(0, arrivalTime - currentTime);
    }

    /**
     * Get how long items have been at the market (in ticks).
     */
    public long getTimeAtMarket(long currentTime) {
        if (status == Status.AT_MARKET && marketListedTime > 0) {
            return currentTime - marketListedTime;
        }
        return 0;
    }

    public CompoundTag save() {
        CompoundTag tag = new CompoundTag();
        tag.putUUID("Id", id);
        tag.putString("Town", townId);
        tag.putLong("Departure", departureTime);
        tag.putLong("Arrival", arrivalTime);
        tag.putLong("Listed", marketListedTime);
        tag.putLong("Sold", soldTime);
        tag.putLong("ReturnArr", returnArrivalTime);
        tag.putString("Status", status.name());
        tag.putInt("Earnings", totalEarnings);

        ListTag itemList = new ListTag();
        for (ShipmentItem item : items) {
            itemList.add(item.save());
        }
        tag.put("Items", itemList);

        return tag;
    }

    public static Shipment load(CompoundTag tag) {
        UUID id = tag.getUUID("Id");
        String townId = tag.getString("Town");
        long departure = tag.getLong("Departure");
        long arrival = tag.getLong("Arrival");

        List<ShipmentItem> items = new ArrayList<>();
        ListTag itemList = tag.getList("Items", Tag.TAG_COMPOUND);
        for (int i = 0; i < itemList.size(); i++) {
            items.add(ShipmentItem.load(itemList.getCompound(i)));
        }

        Shipment shipment = new Shipment(id, townId, items, departure, arrival);
        shipment.status = Status.valueOf(tag.getString("Status"));
        shipment.marketListedTime = tag.getLong("Listed");
        shipment.soldTime = tag.getLong("Sold");
        shipment.returnArrivalTime = tag.getLong("ReturnArr");
        shipment.totalEarnings = tag.getInt("Earnings");

        return shipment;
    }

    /**
     * Represents a single item/stack in a shipment with its listing price.
     */
    public static class ShipmentItem {
        private final ResourceLocation itemId;
        private final int count;
        private int pricePerItem; // in copper pieces (mutable for price adjustments)
        private final String displayName;
        private boolean sold;

        public ShipmentItem(ResourceLocation itemId, int count, int pricePerItem, String displayName) {
            this.itemId = itemId;
            this.count = count;
            this.pricePerItem = pricePerItem;
            this.displayName = displayName;
            this.sold = false;
        }

        public ResourceLocation getItemId() { return itemId; }
        public int getCount() { return count; }
        public int getPricePerItem() { return pricePerItem; }
        public void setPricePerItem(int price) { this.pricePerItem = Math.max(1, price); }
        public int getTotalPrice() { return pricePerItem * count; }
        public String getDisplayName() { return displayName; }
        public boolean isSold() { return sold; }
        public void setSold(boolean sold) { this.sold = sold; }

        public Item getItem() {
            return ForgeRegistries.ITEMS.getValue(itemId);
        }

        public ItemStack createStack() {
            Item item = getItem();
            return item != null ? new ItemStack(item, count) : ItemStack.EMPTY;
        }

        public CompoundTag save() {
            CompoundTag tag = new CompoundTag();
            tag.putString("ItemId", itemId.toString());
            tag.putInt("Count", count);
            tag.putInt("Price", pricePerItem);
            tag.putString("Name", displayName);
            tag.putBoolean("Sold", sold);
            return tag;
        }

        public static ShipmentItem load(CompoundTag tag) {
            ShipmentItem item = new ShipmentItem(
                    new ResourceLocation(tag.getString("ItemId")),
                    tag.getInt("Count"),
                    tag.getInt("Price"),
                    tag.getString("Name")
            );
            item.sold = tag.getBoolean("Sold");
            return item;
        }
    }
}
