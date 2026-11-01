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
}
