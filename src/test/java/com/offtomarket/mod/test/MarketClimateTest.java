package com.offtomarket.mod.test;

import com.offtomarket.mod.data.MarketClimate;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

public class MarketClimateTest {

    @Test
    public void testMarketClimateValues() {
        assertNotNull(MarketClimate.STABLE);
        assertEquals(1.0, MarketClimate.STABLE.getPayoutMultiplier(), 0.001);
        assertEquals(1.0, MarketClimate.STABLE.getPurchaseCostMultiplier(), 0.001);

        assertTrue(MarketClimate.PROSPERITY.getPayoutMultiplier() > 1.0);
        assertEquals(1.15, MarketClimate.PROSPERITY.getPayoutMultiplier(), 0.001);

        assertTrue(MarketClimate.INFLATION.getPayoutMultiplier() > 1.0);
        assertTrue(MarketClimate.INFLATION.getPurchaseCostMultiplier() > 1.0);

        assertTrue(MarketClimate.SCARCITY.getPayoutMultiplier() > 1.0);

        assertTrue(MarketClimate.BUYERS_MARKET.getPurchaseCostMultiplier() < 1.0);
        assertEquals(0.85, MarketClimate.BUYERS_MARKET.getPurchaseCostMultiplier(), 0.001);
    }

    @Test
    public void testFromNameFallback() {
        assertEquals(MarketClimate.STABLE, MarketClimate.fromName(null));
        assertEquals(MarketClimate.STABLE, MarketClimate.fromName(""));
        assertEquals(MarketClimate.STABLE, MarketClimate.fromName("UNKNOWN_CLIMATE"));
        assertEquals(MarketClimate.PROSPERITY, MarketClimate.fromName("PROSPERITY"));
        assertEquals(MarketClimate.BUYERS_MARKET, MarketClimate.fromName("buyers_market"));
    }

    @Test
    public void testIconsAndColors() {
        for (MarketClimate c : MarketClimate.values()) {
            assertNotNull(c.getDisplayName());
            assertNotNull(c.getDescription());
            assertNotNull(c.getIcon());
            assertFalse(c.getIcon().isEmpty());
            assertTrue(c.getColor() > 0);
        }
    }
}
