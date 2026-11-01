package com.offtomarket.mod.test;

import com.offtomarket.mod.data.TownDepotManager;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

public class TownDepotManagerTest {

    @Test
    public void testAssignAndUnassignContainer() {
        TownDepotManager manager = new TownDepotManager();
        BlockPos pos = new BlockPos(100, 64, 200);

        assertNull(manager.getAssignedTown(pos));
        assertFalse(manager.isAssignedToTown(pos, "saltmarsh"));

        manager.assignContainer(pos, "saltmarsh");
        assertEquals("saltmarsh", manager.getAssignedTown(pos));
        assertTrue(manager.isAssignedToTown(pos, "saltmarsh"));
        assertTrue(manager.isAssignedToTown(pos, "SALTMARSH")); // Case-insensitive
        assertFalse(manager.isAssignedToTown(pos, "oakhaven"));

        manager.unassignContainer(pos);
        assertNull(manager.getAssignedTown(pos));
        assertFalse(manager.isAssignedToTown(pos, "saltmarsh"));
    }

    @Test
    public void testNbtPersistence() {
        TownDepotManager original = new TownDepotManager();
        BlockPos pos1 = new BlockPos(10, 70, -50);
        BlockPos pos2 = new BlockPos(11, 70, -50);

        original.assignContainer(pos1, "saltmarsh");
        original.assignContainer(pos2, "oakhaven");

        CompoundTag tag = original.save();

        TownDepotManager loaded = TownDepotManager.createFromTag(tag);
        assertEquals("saltmarsh", loaded.getAssignedTown(pos1));
        assertEquals("oakhaven", loaded.getAssignedTown(pos2));
        assertEquals(2, loaded.getAllAssignedContainers().size());
    }
}
