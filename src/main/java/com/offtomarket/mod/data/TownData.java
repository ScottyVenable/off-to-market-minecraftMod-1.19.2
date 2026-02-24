package com.offtomarket.mod.data;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraftforge.registries.ForgeRegistries;

import java.util.*;

/**
 * Represents a fictional trading town/village with specific needs and specialties.
 * <p>
 * Each item category has a {@link NeedLevel} that determines how much the town
 * values that category (DESPERATE → 2.0× down to OVERSATURATED → 0.5×).
 * <p>
 * Supply levels track a numeric count (0–120+) that maps to NeedLevel tiers
 * and can shift dynamically through daily refresh mechanics.
 * <p>
 * Distance affects travel time and sale value.
 */
public class TownData {
    private final String id;
    private final String displayName;
    private final String description;
    private final int distance; // abstract distance (1-10)
    private final TownType type;
    private final Set<ResourceLocation> needs; // items this town wants to buy (higher value) — legacy
    private final Set<ResourceLocation> surplus; // items this town has surplus of (lower value) — legacy
    private final Set<ResourceLocation> specialtyItems; // items this town sells
    private final int minTraderLevel; // minimum trader level to unlock this town

    /** Per-item NeedLevel overrides (keyed by item registry name string). */
    private final Map<String, NeedLevel> needLevels;

    /**
     * Dynamic supply counts per item category (keyed by item registry name string).
     * Maps to NeedLevel via {@link NeedLevel#fromSupplyLevel(int)}.
     */
    private final Map<String, Integer> supplyLevels;

    public TownData(String id, String displayName, String description, int distance,
                    TownType type, Set<ResourceLocation> needs, Set<ResourceLocation> surplus,
                    Set<ResourceLocation> specialtyItems, int minTraderLevel) {
        this(id, displayName, description, distance, type, needs, surplus,
                specialtyItems, minTraderLevel, new HashMap<>(), new HashMap<>());
    }

    public TownData(String id, String displayName, String description, int distance,
                    TownType type, Set<ResourceLocation> needs, Set<ResourceLocation> surplus,
                    Set<ResourceLocation> specialtyItems, int minTraderLevel,
                    Map<String, NeedLevel> needLevels, Map<String, Integer> supplyLevels) {
        this.id = id;
        this.displayName = displayName;
        this.description = description;
        this.distance = distance;
        this.type = type;
        this.needs = needs;
        this.surplus = surplus;
        this.specialtyItems = specialtyItems;
        this.minTraderLevel = minTraderLevel;
        this.needLevels = needLevels;
        this.supplyLevels = supplyLevels;
    }

    public String getId() { return id; }
    public String getDisplayName() { return displayName; }
    public String getDescription() { return description; }
    public int getDistance() { return distance; }
    public TownType getType() { return type; }
    public Set<ResourceLocation> getNeeds() { return needs; }
    public Set<ResourceLocation> getSurplus() { return surplus; }
    public Set<ResourceLocation> getSpecialtyItems() { return specialtyItems; }
    public int getMinTraderLevel() { return minTraderLevel; }

    /**
     * Whether this town needs the given item (pays more for it).
     * Now delegates to the graduated NeedLevel system.
     */
    public boolean needsItem(Item item) {
        NeedLevel level = getNeedLevel(item);
        return level.isInDemand();
    }

    /**
     * Whether this town has surplus of the given item (pays less for it).
     * Now delegates to the graduated NeedLevel system.
     */
    public boolean hasSurplus(Item item) {
        NeedLevel level = getNeedLevel(item);
        return level.isOversupplied();
    }

    /**
     * Get the graduated NeedLevel for a specific item in this town.
     * <p>
     * Resolution order:
     * <ol>
     *   <li>Explicit NeedLevel override (set per item category)</li>
     *   <li>Dynamic supply level → converted via {@link NeedLevel#fromSupplyLevel}</li>
     *   <li>Legacy needs/surplus sets (backward compat)</li>
     *   <li>Default: {@link NeedLevel#BALANCED}</li>
     * </ol>
     */
    public NeedLevel getNeedLevel(Item item) {
        ResourceLocation rl = ForgeRegistries.ITEMS.getKey(item);
        if (rl == null) return NeedLevel.BALANCED;
        String key = rl.toString();

        // 1. Explicit override
        NeedLevel explicit = needLevels.get(key);
        if (explicit != null) return explicit;

        // 2. Dynamic supply level
        Integer supply = supplyLevels.get(key);
        if (supply != null) return NeedLevel.fromSupplyLevel(supply);

        // 3. Legacy needs/surplus sets
        if (needs.contains(rl)) return NeedLevel.HIGH_NEED;
        if (surplus.contains(rl)) return NeedLevel.SURPLUS;

        // 4. Default
        return NeedLevel.BALANCED;
    }

    /**
     * Set an explicit NeedLevel override for an item category.
     */
    public void setNeedLevel(String itemKey, NeedLevel level) {
        needLevels.put(itemKey, level);
    }

    /**
     * Get the current supply level for an item, or -1 if not tracked.
     */
    public int getSupplyLevel(String itemKey) {
        return supplyLevels.getOrDefault(itemKey, -1);
    }

    /**
     * Set the supply level for an item category.
     */
    public void setSupplyLevel(String itemKey, int level) {
        supplyLevels.put(itemKey, Math.max(0, level));
    }

    /**
     * Adjust a supply level by a delta (positive = more supply, negative = less).
     */
    public void adjustSupplyLevel(String itemKey, int delta) {
        int current = supplyLevels.getOrDefault(itemKey, 60); // default BALANCED
        supplyLevels.put(itemKey, Math.max(0, current + delta));
    }

    /**
     * Get the NeedLevel map for UI display.
     */
    public Map<String, NeedLevel> getNeedLevels() { return needLevels; }

    /**
     * Get the supply levels map for persistence.
     */
    public Map<String, Integer> getSupplyLevels() { return supplyLevels; }

    /**
     * Calculate the travel time in ticks based on distance and config.
     */
    public int getTravelTimeTicks(int ticksPerDistance) {
        return distance * ticksPerDistance;
    }

    /**
     * Get the distance-based value multiplier. Farther towns pay more.
     */
    public double getDistanceValueMultiplier() {
        return 1.0 + (distance - 1) * 0.1; // +10% per distance unit over 1
    }

    public CompoundTag save() {
        CompoundTag tag = new CompoundTag();
        tag.putString("Id", id);
        tag.putString("Name", displayName);
        tag.putString("Desc", description);
        tag.putInt("Distance", distance);
        tag.putString("Type", type.name());
        tag.putInt("MinLevel", minTraderLevel);

        ListTag needsList = new ListTag();
        for (ResourceLocation rl : needs) {
            CompoundTag entry = new CompoundTag();
            entry.putString("Item", rl.toString());
            needsList.add(entry);
        }
        tag.put("Needs", needsList);

        ListTag surplusList = new ListTag();
        for (ResourceLocation rl : surplus) {
            CompoundTag entry = new CompoundTag();
            entry.putString("Item", rl.toString());
            surplusList.add(entry);
        }
        tag.put("Surplus", surplusList);

        ListTag specialtyList = new ListTag();
        for (ResourceLocation rl : specialtyItems) {
            CompoundTag entry = new CompoundTag();
            entry.putString("Item", rl.toString());
            specialtyList.add(entry);
        }
        tag.put("Specialties", specialtyList);

        // Save NeedLevel overrides
        if (!needLevels.isEmpty()) {
            CompoundTag needLevelTag = new CompoundTag();
            for (Map.Entry<String, NeedLevel> e : needLevels.entrySet()) {
                needLevelTag.putString(e.getKey(), e.getValue().name());
            }
            tag.put("NeedLevels", needLevelTag);
        }

        // Save dynamic supply levels
        if (!supplyLevels.isEmpty()) {
            CompoundTag supplyTag = new CompoundTag();
            for (Map.Entry<String, Integer> e : supplyLevels.entrySet()) {
                supplyTag.putInt(e.getKey(), e.getValue());
            }
            tag.put("SupplyLevels", supplyTag);
        }

        return tag;
    }

    public static TownData load(CompoundTag tag) {
        String id = tag.getString("Id");
        String name = tag.getString("Name");
        String desc = tag.getString("Desc");
        int distance = tag.getInt("Distance");
        TownType type = TownType.valueOf(tag.getString("Type"));
        int minLevel = tag.getInt("MinLevel");

        Set<ResourceLocation> needs = new HashSet<>();
        ListTag needsList = tag.getList("Needs", Tag.TAG_COMPOUND);
        for (int i = 0; i < needsList.size(); i++) {
            needs.add(new ResourceLocation(needsList.getCompound(i).getString("Item")));
        }

        Set<ResourceLocation> surplus = new HashSet<>();
        ListTag surplusList = tag.getList("Surplus", Tag.TAG_COMPOUND);
        for (int i = 0; i < surplusList.size(); i++) {
            surplus.add(new ResourceLocation(surplusList.getCompound(i).getString("Item")));
        }

        Set<ResourceLocation> specialties = new HashSet<>();
        ListTag specialtyList = tag.getList("Specialties", Tag.TAG_COMPOUND);
        for (int i = 0; i < specialtyList.size(); i++) {
            specialties.add(new ResourceLocation(specialtyList.getCompound(i).getString("Item")));
        }

        // Load NeedLevel overrides
        Map<String, NeedLevel> needLevels = new HashMap<>();
        if (tag.contains("NeedLevels")) {
            CompoundTag needLevelTag = tag.getCompound("NeedLevels");
            for (String key : needLevelTag.getAllKeys()) {
                try {
                    needLevels.put(key, NeedLevel.valueOf(needLevelTag.getString(key)));
                } catch (IllegalArgumentException ignored) {}
            }
        }

        // Load dynamic supply levels
        Map<String, Integer> supplyLevels = new HashMap<>();
        if (tag.contains("SupplyLevels")) {
            CompoundTag supplyTag = tag.getCompound("SupplyLevels");
            for (String key : supplyTag.getAllKeys()) {
                supplyLevels.put(key, supplyTag.getInt(key));
            }
        }

        return new TownData(id, name, desc, distance, type, needs, surplus, specialties,
                minLevel, needLevels, supplyLevels);
    }

    public enum TownType {
        VILLAGE("Village"),
        TOWN("Town"),
        CITY("City"),
        MARKET("Market"),
        OUTPOST("Outpost");

        private final String displayName;
        TownType(String displayName) { this.displayName = displayName; }
        public String getDisplayName() { return displayName; }
    }
}
