package com.offtomarket.mod.config;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.offtomarket.mod.OffToMarket;
import com.offtomarket.mod.data.NeedLevel;
import com.offtomarket.mod.data.TownData;
import com.offtomarket.mod.data.TownRegistry;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.fml.loading.FMLPaths;
import net.minecraftforge.registries.ForgeRegistries;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;

/**
 * Handles loading of modded item configurations from JSON files.
 * Allows modpack makers and users to add items from other mods to the trading system.
 * 
 * Config files are loaded from: config/offtomarket/items/
 * 
 * Example JSON format:
 * {
 *   "modId": "farmersdelight",
 *   "townAdditions": {
 *     "greenhollow": {
 *       "needs": ["farmersdelight:tomato", "farmersdelight:onion"],
 *       "surplus": ["farmersdelight:cabbage"],
 *       "specialties": ["farmersdelight:fried_egg", "farmersdelight:mixed_salad"],
 *       "needLevels": {
 *         "farmersdelight:tomato": "HIGH_NEED",
 *         "farmersdelight:cabbage": "SURPLUS"
 *       }
 *     }
 *   },
 *   "globalItems": {
 *     "universalNeeds": ["farmersdelight:cooking_pot"],
 *     "universalSurplus": []
 *   }
 * }
 */
public class ModdedItemConfig {
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final String CONFIG_FOLDER = "offtomarket/items";
    private static final List<String> loadedMods = new ArrayList<>();
    private static boolean initialized = false;

    /**
     * Load all modded item configurations from the config folder.
     * Called during mod initialization.
     */
    public static void loadAllConfigs() {
        if (initialized) return;
        initialized = true;
        
        Path configPath = FMLPaths.CONFIGDIR.get().resolve(CONFIG_FOLDER);
        
        // Create the config folder and example file if it doesn't exist
        try {
            Files.createDirectories(configPath);
            createExampleConfig(configPath);
        } catch (IOException e) {
            OffToMarket.LOGGER.error("Failed to create modded items config folder", e);
            return;
        }
        
        // Load all JSON files in the folder
        File[] files = configPath.toFile().listFiles((dir, name) -> name.endsWith(".json"));
        if (files == null) return;
        
        for (File file : files) {
            try {
                loadConfigFile(file);
            } catch (Exception e) {
                OffToMarket.LOGGER.error("Failed to load modded item config: " + file.getName(), e);
            }
        }
        
        OffToMarket.LOGGER.info("Loaded {} modded item config(s): {}", loadedMods.size(), loadedMods);
    }

    /**
     * Load a single config file.
     */
    private static void loadConfigFile(File file) throws IOException {
        String content = Files.readString(file.toPath(), StandardCharsets.UTF_8);
        JsonObject root = JsonParser.parseString(content).getAsJsonObject();
        
        String modId = root.has("modId") ? root.get("modId").getAsString() : file.getName().replace(".json", "");
        
        // Check if the mod is loaded
        boolean modLoaded = isModLoaded(modId);
        if (!modLoaded && !modId.equals("example") && !modId.equals("minecraft")) {
            OffToMarket.LOGGER.debug("Skipping config for unloaded mod: {}", modId);
            return;
        }
        
        // Process town-specific additions
        if (root.has("townAdditions")) {
            JsonObject townAdditions = root.getAsJsonObject("townAdditions");
            for (String townId : townAdditions.keySet()) {
                processTownAdditions(townId, townAdditions.getAsJsonObject(townId));
            }
        }
        
        // Process global items (added to all towns)
        if (root.has("globalItems")) {
            JsonObject globalItems = root.getAsJsonObject("globalItems");
            processGlobalItems(globalItems);
        }
        
        if (!modId.equals("example")) {
            loadedMods.add(modId);
        }
    }

    /**
     * Process additions for a specific town.
     */
    private static void processTownAdditions(String townId, JsonObject additions) {
        TownData town = TownRegistry.getTown(townId);
        if (town == null) {
            OffToMarket.LOGGER.warn("Unknown town '{}' in modded item config", townId);
            return;
        }
        
        // Add needs
        if (additions.has("needs")) {
            for (JsonElement elem : additions.getAsJsonArray("needs")) {
                String itemId = elem.getAsString();
                if (isItemValid(itemId)) {
                    town.getNeeds().add(new ResourceLocation(itemId));
                }
            }
        }
        
        // Add surplus
        if (additions.has("surplus")) {
            for (JsonElement elem : additions.getAsJsonArray("surplus")) {
                String itemId = elem.getAsString();
                if (isItemValid(itemId)) {
                    town.getSurplus().add(new ResourceLocation(itemId));
                }
            }
        }
        
        // Add specialties (items the town sells)
        if (additions.has("specialties")) {
            for (JsonElement elem : additions.getAsJsonArray("specialties")) {
                String itemId = elem.getAsString();
                if (isItemValid(itemId)) {
                    town.getSpecialtyItems().add(new ResourceLocation(itemId));
                }
            }
        }
        
        // Set specific need levels
        if (additions.has("needLevels")) {
            JsonObject needLevels = additions.getAsJsonObject("needLevels");
            for (String itemId : needLevels.keySet()) {
                String levelName = needLevels.get(itemId).getAsString();
                try {
                    NeedLevel level = NeedLevel.valueOf(levelName);
                    town.setNeedLevel(itemId, level);
                } catch (IllegalArgumentException e) {
                    OffToMarket.LOGGER.warn("Invalid NeedLevel '{}' for item '{}'", levelName, itemId);
                }
            }
        }
    }

    /**
     * Process global items that apply to all towns.
     */
    private static void processGlobalItems(JsonObject globalItems) {
        // Universal needs - add to all towns
        if (globalItems.has("universalNeeds")) {
            for (JsonElement elem : globalItems.getAsJsonArray("universalNeeds")) {
                String itemId = elem.getAsString();
                if (isItemValid(itemId)) {
                    ResourceLocation rl = new ResourceLocation(itemId);
                    for (TownData town : TownRegistry.getAllTowns()) {
                        town.getNeeds().add(rl);
                    }
                }
            }
        }
        
        // Universal surplus - add to all towns
        if (globalItems.has("universalSurplus")) {
            for (JsonElement elem : globalItems.getAsJsonArray("universalSurplus")) {
                String itemId = elem.getAsString();
                if (isItemValid(itemId)) {
                    ResourceLocation rl = new ResourceLocation(itemId);
                    for (TownData town : TownRegistry.getAllTowns()) {
                        town.getSurplus().add(rl);
                    }
                }
            }
        }
    }

    /**
     * Check if an item ID is valid (exists in registry).
     */
    private static boolean isItemValid(String itemId) {
        try {
            ResourceLocation rl = new ResourceLocation(itemId);
            return ForgeRegistries.ITEMS.containsKey(rl);
        } catch (Exception e) {
            return false;
        }
    }

    /**
     * Check if a mod is loaded.
     */
    private static boolean isModLoaded(String modId) {
        return net.minecraftforge.fml.ModList.get().isLoaded(modId);
    }

    /**
     * Create an example config file if none exist.
     */
    private static void createExampleConfig(Path configPath) throws IOException {
        Path exampleFile = configPath.resolve("_example.json");
        if (Files.exists(exampleFile)) return;
        
        JsonObject example = new JsonObject();
        example.addProperty("_comment", "This is an example config. Rename to your mod's ID (e.g., farmersdelight.json) and edit.");
        example.addProperty("modId", "example");
        
        // Town additions example
        JsonObject townAdditions = new JsonObject();
        
        JsonObject greenhollow = new JsonObject();
        JsonArray ghNeeds = new JsonArray();
        ghNeeds.add("modid:example_tool");
        greenhollow.add("needs", ghNeeds);
        
        JsonArray ghSurplus = new JsonArray();
        ghSurplus.add("modid:example_crop");
        greenhollow.add("surplus", ghSurplus);
        
        JsonArray ghSpecialties = new JsonArray();
        ghSpecialties.add("modid:example_food");
        greenhollow.add("specialties", ghSpecialties);
        
        JsonObject ghNeedLevels = new JsonObject();
        ghNeedLevels.addProperty("modid:example_tool", "DESPERATE");
        ghNeedLevels.addProperty("modid:example_crop", "OVERSATURATED");
        greenhollow.add("needLevels", ghNeedLevels);
        
        townAdditions.add("greenhollow", greenhollow);
        example.add("townAdditions", townAdditions);
        
        // Global items example
        JsonObject globalItems = new JsonObject();
        JsonArray universalNeeds = new JsonArray();
        universalNeeds.add("modid:universal_item");
        globalItems.add("universalNeeds", universalNeeds);
        globalItems.add("universalSurplus", new JsonArray());
        example.add("globalItems", globalItems);
        
        // Add available towns list
        JsonArray availableTowns = new JsonArray();
        availableTowns.add("greenhollow");
        availableTowns.add("irondeep");
        availableTowns.add("saltmere");
        availableTowns.add("timberwatch");
        availableTowns.add("crossroads");
        availableTowns.add("basaltkeep");
        availableTowns.add("arcaneveil");
        availableTowns.add("goldspire");
        example.add("_availableTowns", availableTowns);
        
        // Add available need levels
        JsonArray availableLevels = new JsonArray();
        availableLevels.add("DESPERATE");
        availableLevels.add("HIGH_NEED");
        availableLevels.add("MODERATE_NEED");
        availableLevels.add("BALANCED");
        availableLevels.add("SURPLUS");
        availableLevels.add("OVERSATURATED");
        example.add("_availableNeedLevels", availableLevels);
        
        Files.writeString(exampleFile, GSON.toJson(example), StandardCharsets.UTF_8);
    }

    /**
     * Get list of loaded mod configs.
     */
    public static List<String> getLoadedMods() {
        return Collections.unmodifiableList(loadedMods);
    }

    /**
     * API method for other mods to register items programmatically.
     */
    public static void registerTownItem(String townId, String itemId, ItemCategory category, NeedLevel level) {
        TownData town = TownRegistry.getTown(townId);
        if (town == null) {
            OffToMarket.LOGGER.warn("Cannot register item: unknown town '{}'", townId);
            return;
        }
        
        ResourceLocation rl = new ResourceLocation(itemId);
        switch (category) {
            case NEED -> town.getNeeds().add(rl);
            case SURPLUS -> town.getSurplus().add(rl);
            case SPECIALTY -> town.getSpecialtyItems().add(rl);
        }
        
        if (level != null) {
            town.setNeedLevel(itemId, level);
        }
    }

    /**
     * API method to register an item to all towns.
     */
    public static void registerGlobalItem(String itemId, ItemCategory category, NeedLevel level) {
        ResourceLocation rl = new ResourceLocation(itemId);
        for (TownData town : TownRegistry.getAllTowns()) {
            switch (category) {
                case NEED -> town.getNeeds().add(rl);
                case SURPLUS -> town.getSurplus().add(rl);
                case SPECIALTY -> town.getSpecialtyItems().add(rl);
            }
            if (level != null) {
                town.setNeedLevel(itemId, level);
            }
        }
    }

    public enum ItemCategory {
        NEED,       // Items the town wants to buy
        SURPLUS,    // Items the town has excess of
        SPECIALTY   // Items the town sells
    }
}
