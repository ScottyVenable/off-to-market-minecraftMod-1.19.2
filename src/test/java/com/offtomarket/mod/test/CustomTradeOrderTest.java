package com.offtomarket.mod.test;

import com.offtomarket.mod.data.CustomTradeOrder;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

public class CustomTradeOrderTest {

    @Test
    public void testSingleItemOrderProgressAndFulfillment() {
        ResourceLocation bread = new ResourceLocation("minecraft", "bread");
        CustomTradeOrder.OrderItem item = new CustomTradeOrder.OrderItem(bread, "Bread", 15, 0);

        List<CustomTradeOrder.OrderItem> items = new ArrayList<>();
        items.add(item);

        CustomTradeOrder order = new CustomTradeOrder(UUID.randomUUID(), "saltmarsh", items, true, 1000L);

        assertEquals("saltmarsh", order.getTownId());
        assertTrue(order.isAutoDispatch());
        assertFalse(order.isAllFulfilled());
        assertEquals(0.0f, order.getOverallProgress(), 0.001f);

        // Fill partially (5 of 15)
        item.setCurrentFilled(5);
        assertFalse(order.isAllFulfilled());
        assertEquals(5.0f / 15.0f, order.getOverallProgress(), 0.001f);

        // Fill completely (15 of 15)
        item.setCurrentFilled(15);
        assertTrue(order.isAllFulfilled());
        assertEquals(1.0f, order.getOverallProgress(), 0.001f);

        // Overfill (20 of 15)
        item.setCurrentFilled(20);
        assertTrue(order.isAllFulfilled());
        assertEquals(1.0f, order.getOverallProgress(), 0.001f);
    }

    @Test
    public void testMultiItemManifest() {
        ResourceLocation bread = new ResourceLocation("minecraft", "bread");
        ResourceLocation barrel = new ResourceLocation("minecraft", "barrel");

        CustomTradeOrder.OrderItem breadItem = new CustomTradeOrder.OrderItem(bread, "Bread", 15, 15);
        CustomTradeOrder.OrderItem barrelItem = new CustomTradeOrder.OrderItem(barrel, "Barrel", 5, 0);

        List<CustomTradeOrder.OrderItem> items = new ArrayList<>();
        items.add(breadItem);
        items.add(barrelItem);

        CustomTradeOrder order = new CustomTradeOrder(UUID.randomUUID(), "saltmarsh", items, false, 2000L);

        // Bread fulfilled, barrel not fulfilled yet -> overall order not fulfilled
        assertFalse(order.isAllFulfilled());
        // 15 out of 20 items total = 75%
        assertEquals(0.75f, order.getOverallProgress(), 0.001f);

        // Fill barrel
        barrelItem.setCurrentFilled(5);
        assertTrue(order.isAllFulfilled());
        assertEquals(1.0f, order.getOverallProgress(), 0.001f);
    }

    @Test
    public void testNbtSerialization() {
        ResourceLocation wheat = new ResourceLocation("minecraft", "wheat");
        CustomTradeOrder.OrderItem item = new CustomTradeOrder.OrderItem(wheat, "Wheat", 128, 64);

        List<CustomTradeOrder.OrderItem> items = new ArrayList<>();
        items.add(item);

        UUID orderId = UUID.randomUUID();
        CustomTradeOrder original = new CustomTradeOrder(orderId, "oakhaven", items, true, 5000L);

        CompoundTag tag = original.save();
        CustomTradeOrder restored = CustomTradeOrder.load(tag);

        assertEquals(original.getId(), restored.getId());
        assertEquals("oakhaven", restored.getTownId());
        assertTrue(restored.isAutoDispatch());
        assertEquals(5000L, restored.getCreatedTime());
        assertEquals(1, restored.getItems().size());

        CustomTradeOrder.OrderItem restoredItem = restored.getItems().get(0);
        assertEquals(wheat, restoredItem.getItemId());
        assertEquals("Wheat", restoredItem.getDisplayName());
        assertEquals(128, restoredItem.getTargetQuantity());
        assertEquals(64, restoredItem.getCurrentFilled());
    }

    @Test
    public void testRecurringTradeManifestLifecycleAndNbt() {
        ResourceLocation iron = new ResourceLocation("minecraft", "iron_ingot");
        List<CustomTradeOrder.OrderItem> items = List.of(
                new CustomTradeOrder.OrderItem(iron, "Iron Ingot", 32, 32)
        );

        // 3-cycle recurring manifest
        CustomTradeOrder finiteRecur = new CustomTradeOrder(UUID.randomUUID(), "frostpeak", items, true, 100L, 3, 0);
        assertTrue(finiteRecur.isRecurring());
        assertEquals(3, finiteRecur.getRepeatCount());
        assertEquals(0, finiteRecur.getCompletedCycles());
        assertEquals("RECUR 0/3", finiteRecur.getScheduleBadgeText());

        // Cycle 1 completes -> should keep
        assertTrue(finiteRecur.recordCycleAndCheckShouldKeep());
        assertEquals(1, finiteRecur.getCompletedCycles());
        assertEquals("RECUR 1/3", finiteRecur.getScheduleBadgeText());

        // Cycle 2 completes -> should keep
        assertTrue(finiteRecur.recordCycleAndCheckShouldKeep());
        assertEquals(2, finiteRecur.getCompletedCycles());
        assertEquals("RECUR 2/3", finiteRecur.getScheduleBadgeText());

        // Cycle 3 completes -> should finish and be removed
        assertFalse(finiteRecur.recordCycleAndCheckShouldKeep());
        assertEquals(3, finiteRecur.getCompletedCycles());

        // Unlimited recurring manifest (-1)
        CustomTradeOrder infiniteRecur = new CustomTradeOrder(UUID.randomUUID(), "frostpeak", items, true, 200L, -1, 0);
        assertTrue(infiniteRecur.isRecurring());
        assertEquals("RECUR \u221E", infiniteRecur.getScheduleBadgeText());
        for (int i = 0; i < 10; i++) {
            assertTrue(infiniteRecur.recordCycleAndCheckShouldKeep());
        }
        assertEquals(10, infiniteRecur.getCompletedCycles());

        // NBT round-trip preserves repeatCount and completedCycles
        CompoundTag saved = infiniteRecur.save();
        CustomTradeOrder loaded = CustomTradeOrder.load(saved);
        assertEquals(-1, loaded.getRepeatCount());
        assertEquals(10, loaded.getCompletedCycles());
        assertTrue(loaded.isRecurring());
    }

    @Test
    public void testTradeManagerWorkerAndMailReplyNbt() {
        com.offtomarket.mod.data.Worker tm = new com.offtomarket.mod.data.Worker(
                com.offtomarket.mod.data.Worker.WorkerType.TRADE_MANAGER);
        assertEquals("Trade Manager", tm.getType().getDisplayName());
        assertEquals(750, tm.getHireCost());
        assertEquals("Auto-Dispatch", tm.getPerkName(3));
        assertEquals("Vault Sweeper", tm.getPerkName(6));
        assertEquals("Contract Broker", tm.getPerkName(9));

        com.offtomarket.mod.data.MailNote note = new com.offtomarket.mod.data.MailNote(
                com.offtomarket.mod.data.MailNote.NoteType.QUEST_COMPLETED,
                "Order Complete", "Thank you for the shipment!", "Greenhollow", 500L);
        assertFalse(note.isReplied());
        note.markReplied("Persuasion Succeeded! (+12 Rep, +45c Rebate)");
        assertTrue(note.isReplied());
        assertTrue(note.isRead());

        CompoundTag noteTag = note.save();
        com.offtomarket.mod.data.MailNote loadedNote = com.offtomarket.mod.data.MailNote.load(noteTag);
        assertTrue(loadedNote.isReplied());
        assertEquals("Persuasion Succeeded! (+12 Rep, +45c Rebate)", loadedNote.getReplySummary());
    }
}
