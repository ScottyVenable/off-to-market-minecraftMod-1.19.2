package com.offtomarket.mod.test;

import com.google.gson.Gson;
import com.offtomarket.mod.content.TownDefinition;
import com.offtomarket.mod.data.NeedLevel;
import com.offtomarket.mod.data.TownData;
import net.minecraft.resources.ResourceLocation;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.*;

import static org.junit.jupiter.api.Assertions.*;

public class TownDataTest {

    @Test
    @DisplayName("Needs and surplus sets must be mutable to allow dynamic mod compatibility additions")
    void testNeedsAndSurplusAreMutable() {
        Set<ResourceLocation> needs = new LinkedHashSet<>();
        needs.add(new ResourceLocation("minecraft:iron_ingot"));

        Set<ResourceLocation> surplus = new LinkedHashSet<>();
        surplus.add(new ResourceLocation("minecraft:wheat"));

        TownData town = new TownData(
                "test_town",
                "Test Town",
                "A bustling farming settlement.",
                3,
                TownData.TownType.TOWN,
                needs,
                surplus,
                Collections.emptySet(),
                1,
                new HashMap<>(),
                new HashMap<>(),
                new HashMap<>(),
                new HashMap<>()
        );

        // Verify no UnsupportedOperationException is thrown when dynamically adding needs/surplus
        assertDoesNotThrow(() -> {
            town.getNeeds().add(new ResourceLocation("farmersdelight:onion"));
            town.getSurplus().add(new ResourceLocation("farmersdelight:rice"));
        });

        assertTrue(town.getNeeds().contains(new ResourceLocation("farmersdelight:onion")));
        assertTrue(town.getSurplus().contains(new ResourceLocation("farmersdelight:rice")));
    }

    @Test
    @DisplayName("TownDefinition correctly parses JSON with explicit needs, surplus, and needLevels")
    void testTownDefinitionParsing() {
        String json = """
                {
                  "id": "botany_bay",
                  "displayName": "Beau's Botany Shop",
                  "description": "Exotic plants and herbs.",
                  "distance": 4,
                  "type": "MARKET",
                  "minTraderLevel": 2,
                  "sells": ["minecraft:poppy", "minecraft:dandelion"],
                  "needs": ["minecraft:bone_meal", "farmersdelight:rich_soil"],
                  "surplus": ["minecraft:oak_sapling"],
                  "needLevels": {
                    "minecraft:bone_meal": "DESPERATE",
                    "minecraft:water_bucket": "HIGH_NEED"
                  }
                }
                """;

        Gson gson = new Gson();
        TownDefinition def = gson.fromJson(json, TownDefinition.class);

        assertNotNull(def);
        assertEquals("botany_bay", def.id);
        assertEquals("Beau's Botany Shop", def.displayName);
        assertEquals(4, def.distance);
        assertEquals("MARKET", def.type);
        assertEquals(2, def.minTraderLevel);
        assertEquals(2, def.sells.size());
        assertEquals(2, def.needs.size());
        assertEquals(1, def.surplus.size());
        assertEquals(2, def.needLevels.size());
        assertEquals("DESPERATE", def.needLevels.get("minecraft:bone_meal"));
    }

    @Test
    @DisplayName("TownData need level hierarchy prioritizes explicit needLevels")
    void testNeedLevelPriority() {
        Map<String, NeedLevel> needLevels = new HashMap<>();
        needLevels.put("minecraft:emerald", NeedLevel.DESPERATE);
        needLevels.put("minecraft:gold_ingot", NeedLevel.HIGH_NEED);

        Set<ResourceLocation> needs = new LinkedHashSet<>();
        needs.add(new ResourceLocation("minecraft:iron_ingot"));

        Set<ResourceLocation> surplus = new LinkedHashSet<>();
        surplus.add(new ResourceLocation("minecraft:copper_ingot"));

        TownData town = new TownData(
                "miner_haven",
                "Miner's Haven",
                "Mountain outpost.",
                5,
                TownData.TownType.OUTPOST,
                needs,
                surplus,
                Collections.emptySet(),
                1,
                needLevels,
                new HashMap<>(),
                new HashMap<>(),
                new HashMap<>()
        );

        // Explicit needLevels map
        assertEquals(NeedLevel.DESPERATE, town.getNeedLevel("minecraft:emerald"));
        assertEquals(NeedLevel.HIGH_NEED, town.getNeedLevel("minecraft:gold_ingot"));

        // Fallback to legacy sets
        assertEquals(NeedLevel.HIGH_NEED, town.getNeedLevel("minecraft:iron_ingot"));
        assertEquals(NeedLevel.SURPLUS, town.getNeedLevel("minecraft:copper_ingot"));

        // Unlisted item defaults to BALANCED
        assertEquals(NeedLevel.BALANCED, town.getNeedLevel("minecraft:stone"));
    }
}
