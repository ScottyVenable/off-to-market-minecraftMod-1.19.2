package com.offtomarket.mod.test;

import com.offtomarket.mod.block.entity.FinanceTableBlockEntity.TransactionRecord;
import net.minecraft.nbt.CompoundTag;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

public class FinanceTableTransactionTest {

    @Test
    @DisplayName("TransactionRecord correctly serializes to and deserializes from NBT")
    void testTransactionRecordNbtRoundTrip() {
        TransactionRecord original = new TransactionRecord(
                123456L,
                "Deposit",
                250,
                1500,
                "Deposit All"
        );

        CompoundTag tag = original.save();
        assertNotNull(tag);
        assertEquals(123456L, tag.getLong("Time"));
        assertEquals("Deposit", tag.getString("Type"));
        assertEquals(250, tag.getInt("Amount"));
        assertEquals(1500, tag.getInt("Bal"));
        assertEquals("Deposit All", tag.getString("Details"));

        TransactionRecord loaded = TransactionRecord.load(tag);
        assertNotNull(loaded);
        assertEquals(original.gameTime, loaded.gameTime);
        assertEquals(original.type, loaded.type);
        assertEquals(original.amount, loaded.amount);
        assertEquals(original.resultingBalance, loaded.resultingBalance);
        assertEquals(original.details, loaded.details);
    }

    @Test
    @DisplayName("TransactionRecord handles negative amounts for withdrawals and purchases")
    void testNegativeAmountTransaction() {
        TransactionRecord withdrawal = new TransactionRecord(
                2000L,
                "Withdrawal",
                -450,
                1050,
                "4g 5s 0c"
        );

        CompoundTag tag = withdrawal.save();
        TransactionRecord loaded = TransactionRecord.load(tag);

        assertEquals(-450, loaded.amount);
        assertEquals(1050, loaded.resultingBalance);
        assertEquals("Withdrawal", loaded.type);
    }
}
