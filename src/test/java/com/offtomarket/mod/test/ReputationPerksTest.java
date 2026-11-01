package com.offtomarket.mod.test;

import com.offtomarket.mod.block.entity.TradingPostBlockEntity;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

public class ReputationPerksTest {

    @Test
    public void testReputationTiers() {
        assertEquals("Exalted", TradingPostBlockEntity.getReputationLevel(1000));
        assertEquals("Exalted", TradingPostBlockEntity.getReputationLevel(600));
        assertEquals("Honored", TradingPostBlockEntity.getReputationLevel(599));
        assertEquals("Honored", TradingPostBlockEntity.getReputationLevel(300));
        assertEquals("Friendly", TradingPostBlockEntity.getReputationLevel(299));
        assertEquals("Friendly", TradingPostBlockEntity.getReputationLevel(100));
        assertEquals("Neutral", TradingPostBlockEntity.getReputationLevel(99));
        assertEquals("Neutral", TradingPostBlockEntity.getReputationLevel(0));
        assertEquals("Neutral", TradingPostBlockEntity.getReputationLevel(-99));
        assertEquals("Distrusted", TradingPostBlockEntity.getReputationLevel(-100));
        assertEquals("Distrusted", TradingPostBlockEntity.getReputationLevel(-299));
        assertEquals("Unfriendly", TradingPostBlockEntity.getReputationLevel(-300));
        assertEquals("Unfriendly", TradingPostBlockEntity.getReputationLevel(-599));
        assertEquals("Hostile", TradingPostBlockEntity.getReputationLevel(-600));
        assertEquals("Hostile", TradingPostBlockEntity.getReputationLevel(-1000));
    }

    @Test
    public void testPartnershipPerkDescriptions() {
        assertTrue(TradingPostBlockEntity.getPartnershipPerkDescription(700).contains("+20%"));
        assertTrue(TradingPostBlockEntity.getPartnershipPerkDescription(400).contains("+15%"));
        assertTrue(TradingPostBlockEntity.getPartnershipPerkDescription(150).contains("+10%"));
        assertEquals("Standard Trade Terms", TradingPostBlockEntity.getPartnershipPerkDescription(0));
        assertTrue(TradingPostBlockEntity.getPartnershipPerkDescription(-150).contains("-10%"));
        assertTrue(TradingPostBlockEntity.getPartnershipPerkDescription(-450).contains("-20%"));
        assertTrue(TradingPostBlockEntity.getPartnershipPerkDescription(-800).contains("-35%"));
    }
}
