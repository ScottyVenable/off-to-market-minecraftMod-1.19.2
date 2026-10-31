package com.offtomarket.mod.test;

import com.offtomarket.mod.data.DiplomatRequest;
import com.offtomarket.mod.data.NeedLevel;
import com.offtomarket.mod.data.TownData;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.*;

import static org.junit.jupiter.api.Assertions.*;

public class DiplomatRequestTest {

    @Test
    @DisplayName("DiplomatRequest correctly serializes and deserializes itemNbt")
    void testDiplomatRequestNbtRoundTrip() {
        UUID id = UUID.randomUUID();
        CompoundTag itemTag = new CompoundTag();
        itemTag.putString("CustomEnchantment", "minecraft:mending");
        itemTag.putInt("Level", 1);

        DiplomatRequest original = new DiplomatRequest(
                id,
                "botany_bay",
                new ResourceLocation("minecraft:enchanted_book"),
                "Enchanted Book (Mending)",
                1,
                5000L,
                itemTag
        );
        original.setTimings(6000L, 18000L, 18300L, 19300L);
        original.setPricing(350, 150);
        original.setSupplyScore(85);

        CompoundTag saved = original.save();
        assertNotNull(saved);
        assertTrue(saved.contains("ItemNbt"));

        DiplomatRequest loaded = DiplomatRequest.load(saved);
        assertNotNull(loaded);
        assertEquals(id, loaded.getId());
        assertEquals("botany_bay", loaded.getTownId());
        assertEquals("minecraft:enchanted_book", loaded.getRequestedItemId().toString());
        assertEquals(350, loaded.getProposedPrice());
        assertEquals(150, loaded.getDiplomatPremium());
        assertEquals(85, loaded.getSupplyScore());
        assertNotNull(loaded.getItemNbt());
        assertEquals("minecraft:mending", loaded.getItemNbt().getString("CustomEnchantment"));
        assertEquals(1, loaded.getItemNbt().getInt("Level"));
    }

    @Test
    @DisplayName("Supply score calculation reflects surplus, specialty, and need status")
    void testSupplyScore() {
        Set<ResourceLocation> specialties = new HashSet<>(List.of(new ResourceLocation("minecraft:potion")));
        Set<ResourceLocation> needs = new HashSet<>(List.of(new ResourceLocation("minecraft:nether_wart")));
        Set<ResourceLocation> surplus = new HashSet<>(List.of(new ResourceLocation("minecraft:glass_bottle")));

        TownData town = new TownData(
                "alchemist_lab",
                "Alchemist Lab",
                "Potion makers.",
                2,
                TownData.TownType.TOWN,
                needs,
                surplus,
                specialties,
                1,
                new HashMap<>(),
                new HashMap<>(),
                new HashMap<>(),
                new HashMap<>()
        );

        // Surplus items -> easy to supply (95)
        assertEquals(95, DiplomatRequest.getSupplyScore(town, new ResourceLocation("minecraft:glass_bottle")));

        // Specialty items -> readily available (80)
        assertEquals(80, DiplomatRequest.getSupplyScore(town, new ResourceLocation("minecraft:potion")));

        // Needs -> hard to source (25)
        assertEquals(25, DiplomatRequest.getSupplyScore(town, new ResourceLocation("minecraft:nether_wart")));

        // Neutral -> trade network default (45)
        assertEquals(45, DiplomatRequest.getSupplyScore(town, new ResourceLocation("minecraft:apple")));
    }
}
