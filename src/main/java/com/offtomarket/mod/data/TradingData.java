package com.offtomarket.mod.data;

import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.saveddata.SavedData;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

/**
 * World-level saved data that stores the shared trading state.
 * All Trading Posts in a world share the same trader progression,
 * shipments, quests, diplomats, market data, economy stats, and
 * town inventories.
 *
 * Each Trading Post registers/unregisters itself, and any change
 * to the shared state is pushed here so that all posts stay in sync.
 */
public class TradingData extends SavedData {

    private CompoundTag sharedState = new CompoundTag();
    private final Set<BlockPos> registeredPosts = new HashSet<>();
    private long lastTickedGameTime = -1;

    public TradingData() {
    }

    /**
     * Get the TradingData for this server level (creates if absent).
     */
    public static TradingData get(ServerLevel level) {
        return level.getDataStorage().computeIfAbsent(
                TradingData::load, TradingData::new, "offtomarket_trading");
    }

    // ==================== Shared State ====================

    public CompoundTag getSharedState() {
        return sharedState.copy();
    }

    public void setSharedState(CompoundTag tag) {
        this.sharedState = tag.copy();
        setDirty();
    }

    public boolean isEmpty() {
        return sharedState.isEmpty();
    }

    // ==================== Post Registration ====================

    public void register(BlockPos pos) {
        registeredPosts.add(pos.immutable());
        setDirty();
    }

    public void unregister(BlockPos pos) {
        registeredPosts.remove(pos);
        setDirty();
    }

    public Set<BlockPos> getRegisteredPositions() {
        return Collections.unmodifiableSet(registeredPosts);
    }

    // ==================== Tick Guard ====================

    /**
     * Attempt to claim the tick for this game time.
     * Returns true if this call is the first to claim; false if already claimed.
     * Ensures only one Trading Post processes the global tick logic per game tick.
     */
    public boolean tryClaimTick(long gameTime) {
        if (gameTime == lastTickedGameTime) return false;
        lastTickedGameTime = gameTime;
        return true;
    }

    // ==================== Persistence ====================

    @Override
    public CompoundTag save(CompoundTag tag) {
        tag.put("SharedState", sharedState.copy());

        ListTag positionList = new ListTag();
        for (BlockPos pos : registeredPosts) {
            CompoundTag posTag = new CompoundTag();
            posTag.putInt("X", pos.getX());
            posTag.putInt("Y", pos.getY());
            posTag.putInt("Z", pos.getZ());
            positionList.add(posTag);
        }
        tag.put("RegisteredPosts", positionList);

        // Persist town inventories (global, shared across all trading posts)
        tag.put("TownInventories", TownInventoryManager.save());

        return tag;
    }

    public static TradingData load(CompoundTag tag) {
        TradingData data = new TradingData();
        if (tag.contains("SharedState")) {
            data.sharedState = tag.getCompound("SharedState").copy();
        }
        if (tag.contains("RegisteredPosts")) {
            ListTag positions = tag.getList("RegisteredPosts", Tag.TAG_COMPOUND);
            for (int i = 0; i < positions.size(); i++) {
                CompoundTag posTag = positions.getCompound(i);
                data.registeredPosts.add(new BlockPos(
                        posTag.getInt("X"), posTag.getInt("Y"), posTag.getInt("Z")));
            }
        }
        // Restore town inventories
        if (tag.contains("TownInventories")) {
            TownInventoryManager.load(tag.getCompound("TownInventories"));
        }
        return data;
    }
}
