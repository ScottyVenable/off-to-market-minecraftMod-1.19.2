package com.offtomarket.mod.config;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.offtomarket.mod.OffToMarket;
import com.offtomarket.mod.data.PriceCalculator.ValueTier;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraftforge.fml.loading.FMLPaths;
import net.minecraftforge.registries.ForgeRegistries;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

/**
 * Loads custom item price overrides from config/offtomarket/custom_prices.json.
 * Allows pack makers and server owners to set custom prices for any item without modifying code.
 */
public class CustomPriceConfig {

    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final Map<Item, ValueTier> CUSTOM_PRICES = new HashMap<>();
    private static boolean initialized = false;

    public static void loadAll() {
        CUSTOM_PRICES.clear();
        initialized = true;

        Path configFolder = FMLPaths.CONFIGDIR.get().resolve("offtomarket");
        Path priceFile = configFolder.resolve("custom_prices.json");

        try {
            Files.createDirectories(configFolder);
            if (!Files.exists(priceFile)) {
                createExampleFile(priceFile);
                return;
            }
        } catch (IOException e) {
            OffToMarket.LOGGER.error("Failed to initialize custom prices config directory", e);
            return;
        }

        try {
            String content = Files.readString(priceFile, StandardCharsets.UTF_8);
            JsonObject root = JsonParser.parseString(content).getAsJsonObject();
            for (Map.Entry<String, JsonElement> entry : root.entrySet()) {
                String itemId = entry.getKey();
                if (itemId.startsWith("_")) continue; // Skip comments / metadata keys

                ResourceLocation rl = ResourceLocation.tryParse(itemId);
                if (rl == null) continue;
                if (!ForgeRegistries.ITEMS.containsKey(rl)) continue;
                Item item = ForgeRegistries.ITEMS.getValue(rl);
                if (item == null) continue;

                int basePrice = 0;
                int maxPrice = 0;
                if (entry.getValue().isJsonPrimitive()) {
                    basePrice = entry.getValue().getAsInt();
                    maxPrice = basePrice * 3;
                } else if (entry.getValue().isJsonObject()) {
                    JsonObject obj = entry.getValue().getAsJsonObject();
                    basePrice = obj.has("basePrice") ? obj.get("basePrice").getAsInt() : 0;
                    maxPrice = obj.has("maxPrice") ? obj.get("maxPrice").getAsInt() : basePrice * 3;
                }

                if (basePrice > 0) {
                    CUSTOM_PRICES.put(item, new ValueTier(basePrice, Math.max(basePrice, maxPrice)));
                }
            }
            OffToMarket.LOGGER.info("Loaded {} custom price overrides from config/offtomarket/custom_prices.json", CUSTOM_PRICES.size());
        } catch (Exception e) {
            OffToMarket.LOGGER.error("Failed to parse config/offtomarket/custom_prices.json", e);
        }
    }

    public static ValueTier getPriceOverride(Item item) {
        return CUSTOM_PRICES.get(item);
    }

    public static Map<Item, ValueTier> getAllOverrides() {
        return Collections.unmodifiableMap(CUSTOM_PRICES);
    }

    private static void createExampleFile(Path file) throws IOException {
        JsonObject example = new JsonObject();
        example.addProperty("_comment", "Custom price overrides in Copper Pieces (CP). 10 CP = 1 Silver, 100 CP = 1 Gold. Format: 'modid:item_name': basePrice OR {'basePrice': 100, 'maxPrice': 300}");
        example.addProperty("minecraft:nether_star", 5000);
        Files.writeString(file, GSON.toJson(example), StandardCharsets.UTF_8);
    }
}
