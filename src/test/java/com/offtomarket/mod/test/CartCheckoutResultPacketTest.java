package com.offtomarket.mod.test;

import com.offtomarket.mod.network.CartCheckoutResultPacket;
import net.minecraft.core.BlockPos;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

public class CartCheckoutResultPacketTest {

    @Test
    public void testPacketState() {
        BlockPos pos = new BlockPos(100, 64, -200);
        CartCheckoutResultPacket failPacket = new CartCheckoutResultPacket(pos, false, "Not enough coins for this purchase!");

        assertEquals(pos, failPacket.getMarketBoardPos());
        assertFalse(failPacket.isSuccess());
        assertEquals("Not enough coins for this purchase!", failPacket.getMessage());

        CartCheckoutResultPacket successPacket = new CartCheckoutResultPacket(pos, true, "Order placed! 2 shipment(s) dispatched.");
        assertTrue(successPacket.isSuccess());
        assertEquals("Order placed! 2 shipment(s) dispatched.", successPacket.getMessage());
    }
}
