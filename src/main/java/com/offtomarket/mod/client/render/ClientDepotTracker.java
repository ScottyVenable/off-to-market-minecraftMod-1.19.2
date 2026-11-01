package com.offtomarket.mod.client.render;

import net.minecraft.core.BlockPos;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

/**
 * Client-side tracker of container-to-town designations,
 * populated by SyncDepotContainersPacket and block entity updates.
 * Used by RouteMarkerRenderer for in-world outlines.
 */
public class ClientDepotTracker {

    private static final Map<BlockPos, String> assignedDepots = new HashMap<>();

    public static synchronized void update(Map<BlockPos, String> newAssignments) {
        assignedDepots.clear();
        if (newAssignments != null) {
            assignedDepots.putAll(newAssignments);
        }
    }

    public static synchronized void set(BlockPos pos, String townId) {
        if (townId == null || townId.isEmpty()) {
            assignedDepots.remove(pos);
        } else {
            assignedDepots.put(pos.immutable(), townId);
        }
    }

    public static synchronized Map<BlockPos, String> getAssignedDepots() {
        return Collections.unmodifiableMap(assignedDepots);
    }

    public static synchronized String getTown(BlockPos pos) {
        return assignedDepots.get(pos);
    }
}
