package com.offtomarket.mod.data;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraftforge.common.capabilities.ForgeCapabilities;

import java.util.*;

/**
 * Manages physical container (chests, barrels, shulker boxes, hoppers) designations for specific towns.
 * Allows clustering contiguous touching containers into a single Town Loading Dock / Trade Depot.
 */
public class TownDepotManager {

    private final Map<BlockPos, String> assignedContainers = new HashMap<>();

    public void assignContainer(BlockPos pos, String townId) {
        if (townId == null || townId.isBlank()) {
            assignedContainers.remove(pos);
        } else {
            assignedContainers.put(pos.immutable(), townId);
        }
    }

    public void unassignContainer(BlockPos pos) {
        assignedContainers.remove(pos);
    }

    public String getAssignedTown(BlockPos pos) {
        return assignedContainers.get(pos);
    }

    public boolean isAssignedToTown(BlockPos pos, String townId) {
        String assigned = assignedContainers.get(pos);
        return assigned != null && assigned.equalsIgnoreCase(townId);
    }

    public boolean isAssignedToOtherTown(BlockPos pos, String townId) {
        String assigned = assignedContainers.get(pos);
        return assigned != null && !assigned.equalsIgnoreCase(townId);
    }

    public Map<BlockPos, String> getAllAssignedContainers() {
        return Collections.unmodifiableMap(assignedContainers);
    }

    public Set<BlockPos> getContainersForTown(String townId) {
        if (townId == null) return Collections.emptySet();
        Set<BlockPos> set = new HashSet<>();
        for (Map.Entry<BlockPos, String> entry : assignedContainers.entrySet()) {
            if (townId.equalsIgnoreCase(entry.getValue())) {
                set.add(entry.getKey());
            }
        }
        return set;
    }

    /**
     * Flood-fills and links all contiguous touching containers to the given town.
     * Starts at startPos, expanding to any touching block entity that possesses an item handler.
     * Returns the set of all newly linked container positions.
     */
    public Set<BlockPos> linkContiguousCluster(Level level, BlockPos startPos, String townId, int maxCluster) {
        Set<BlockPos> cluster = new HashSet<>();
        Queue<BlockPos> queue = new ArrayDeque<>();
        queue.add(startPos.immutable());
        cluster.add(startPos.immutable());

        while (!queue.isEmpty() && cluster.size() < maxCluster) {
            BlockPos current = queue.poll();
            for (Direction dir : Direction.values()) {
                BlockPos neighbor = current.relative(dir);
                if (cluster.contains(neighbor)) continue;

                BlockEntity be = level.getBlockEntity(neighbor);
                if (be != null && be.getCapability(ForgeCapabilities.ITEM_HANDLER).isPresent()) {
                    cluster.add(neighbor.immutable());
                    queue.add(neighbor.immutable());
                }
            }
        }

        for (BlockPos pos : cluster) {
            assignContainer(pos, townId);
        }
        return cluster;
    }

    /**
     * Unassigns an entire touching cluster of designated containers.
     */
    public Set<BlockPos> unassignCluster(Level level, BlockPos startPos, int maxCluster) {
        Set<BlockPos> cluster = new HashSet<>();
        Queue<BlockPos> queue = new ArrayDeque<>();
        queue.add(startPos.immutable());
        cluster.add(startPos.immutable());

        while (!queue.isEmpty() && cluster.size() < maxCluster) {
            BlockPos current = queue.poll();
            for (Direction dir : Direction.values()) {
                BlockPos neighbor = current.relative(dir);
                if (cluster.contains(neighbor)) continue;

                if (assignedContainers.containsKey(neighbor)) {
                    cluster.add(neighbor.immutable());
                    queue.add(neighbor.immutable());
                }
            }
        }

        for (BlockPos pos : cluster) {
            assignedContainers.remove(pos);
        }
        return cluster;
    }

    public CompoundTag save() {
        CompoundTag tag = new CompoundTag();
        ListTag list = new ListTag();
        for (Map.Entry<BlockPos, String> entry : assignedContainers.entrySet()) {
            CompoundTag eTag = new CompoundTag();
            eTag.putInt("X", entry.getKey().getX());
            eTag.putInt("Y", entry.getKey().getY());
            eTag.putInt("Z", entry.getKey().getZ());
            eTag.putString("Town", entry.getValue());
            list.add(eTag);
        }
        tag.put("AssignedContainers", list);
        return tag;
    }

    public void load(CompoundTag tag) {
        assignedContainers.clear();
        if (tag.contains("AssignedContainers")) {
            ListTag list = tag.getList("AssignedContainers", Tag.TAG_COMPOUND);
            for (int i = 0; i < list.size(); i++) {
                CompoundTag eTag = list.getCompound(i);
                BlockPos pos = new BlockPos(eTag.getInt("X"), eTag.getInt("Y"), eTag.getInt("Z"));
                assignedContainers.put(pos, eTag.getString("Town"));
            }
        }
    }

    public static TownDepotManager createFromTag(CompoundTag tag) {
        TownDepotManager manager = new TownDepotManager();
        if (tag != null) manager.load(tag);
        return manager;
    }
}
