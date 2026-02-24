package com.offtomarket.mod.data;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;

import java.util.HashMap;
import java.util.Map;

/**
 * Tracks supply history per town to model dynamic demand.
 * As the player oversupplies a town with specific items, demand drops
 * and the town pays less / buys less eagerly. Demand recovers over time.
 *
 * Each unit shipped reduces demand by 2%, down to a minimum of 50%.
 * Every ~10 minutes of game time, all supply counts decay by half.
 */
public class DemandTracker {
    // townId -> itemId -> supply count
    private final Map<String, Map<String, Integer>> supplyMap = new HashMap<>();

    // Decay timer
    private int decayTimer = 0;
    private static final int DECAY_INTERVAL = 12000; // ~10 minutes at 20 TPS
    private static final double DECAY_FACTOR = 0.5;  // halve each decay
    private static final double DEMAND_DROP_PER_UNIT = 0.02; // 2% per unit shipped
    private static final double MIN_DEMAND = 0.5; // minimum 50% of base value

    /**
     * Record that items were shipped to a town.
     */
    public void recordSupply(String townId, String itemId, int count) {
        supplyMap.computeIfAbsent(townId, k -> new HashMap<>())
                .merge(itemId, count, Integer::sum);
    }

    /**
     * Get the demand multiplier for an item at a town.
     * Returns 1.0 (full demand) down to MIN_DEMAND (oversupplied).
     */
    public double getDemandMultiplier(String townId, String itemId) {
        Map<String, Integer> townSupply = supplyMap.get(townId);
        if (townSupply == null) return 1.0;
        int supply = townSupply.getOrDefault(itemId, 0);
        return Math.max(MIN_DEMAND, 1.0 - supply * DEMAND_DROP_PER_UNIT);
    }

    /**
     * Get the overall demand level for a town (average across all items).
     * Returns 1.0 (full demand) down to MIN_DEMAND.
     */
    public double getTownDemandLevel(String townId) {
        Map<String, Integer> townSupply = supplyMap.get(townId);
        if (townSupply == null || townSupply.isEmpty()) return 1.0;
        double total = 0;
        for (int supply : townSupply.values()) {
            total += Math.max(MIN_DEMAND, 1.0 - supply * DEMAND_DROP_PER_UNIT);
        }
        return total / townSupply.size();
    }

    /**
     * Tick the decay timer. Should be called every server tick.
     * Returns true if decay was applied this tick.
     */
    public boolean tick() {
        decayTimer++;
        if (decayTimer >= DECAY_INTERVAL) {
            decayTimer = 0;
            decay();
            return true;
        }
        return false;
    }

    /**
     * Decay all supply counts by DECAY_FACTOR.
     */
    private void decay() {
        for (Map<String, Integer> townSupply : supplyMap.values()) {
            townSupply.replaceAll((item, count) -> Math.max(0, (int) (count * DECAY_FACTOR)));
            townSupply.values().removeIf(c -> c <= 0);
        }
        supplyMap.values().removeIf(Map::isEmpty);
    }

    /**
     * Get all supply data for a town (for display purposes).
     */
    public Map<String, Integer> getTownSupply(String townId) {
        return supplyMap.getOrDefault(townId, Map.of());
    }

    // ==================== NBT ====================

    public CompoundTag save() {
        CompoundTag tag = new CompoundTag();
        tag.putInt("DecayTimer", decayTimer);

        ListTag townList = new ListTag();
        for (Map.Entry<String, Map<String, Integer>> townEntry : supplyMap.entrySet()) {
            CompoundTag townTag = new CompoundTag();
            townTag.putString("Town", townEntry.getKey());

            ListTag items = new ListTag();
            for (Map.Entry<String, Integer> itemEntry : townEntry.getValue().entrySet()) {
                CompoundTag itemTag = new CompoundTag();
                itemTag.putString("Item", itemEntry.getKey());
                itemTag.putInt("Count", itemEntry.getValue());
                items.add(itemTag);
            }
            townTag.put("Items", items);
            townList.add(townTag);
        }
        tag.put("Supply", townList);

        return tag;
    }

    public void load(CompoundTag tag) {
        decayTimer = tag.getInt("DecayTimer");
        supplyMap.clear();

        ListTag townList = tag.getList("Supply", Tag.TAG_COMPOUND);
        for (int i = 0; i < townList.size(); i++) {
            CompoundTag townTag = townList.getCompound(i);
            String townId = townTag.getString("Town");
            Map<String, Integer> items = new HashMap<>();

            ListTag itemList = townTag.getList("Items", Tag.TAG_COMPOUND);
            for (int j = 0; j < itemList.size(); j++) {
                CompoundTag itemTag = itemList.getCompound(j);
                items.put(itemTag.getString("Item"), itemTag.getInt("Count"));
            }

            supplyMap.put(townId, items);
        }
    }
}
