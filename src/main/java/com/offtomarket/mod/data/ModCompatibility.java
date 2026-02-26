package com.offtomarket.mod.data;

import com.offtomarket.mod.OffToMarket;
import com.offtomarket.mod.config.ModConfig;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.ItemTags;
import net.minecraft.tags.TagKey;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.food.FoodProperties;
import net.minecraft.world.item.*;
import net.minecraftforge.common.Tags;
import net.minecraftforge.fml.ModList;
import net.minecraftforge.registries.ForgeRegistries;

import java.util.*;
import java.util.function.Predicate;

/**
 * Dynamic mod compatibility system.
 * Automatically discovers items from other mods and categorizes them for trading.
 * Also generates dynamic towns based on which mods are loaded.
 */
public class ModCompatibility {

    // ==================== Item Categories ====================

    public enum ItemCategory {
        // Resources
        INGOTS("ingots", "Metal ingots and bars"),
        GEMS("gems", "Precious gems and crystals"),
        ORES("ores", "Raw ores and mining materials"),
        DUSTS("dusts", "Powders and dusts"),
        NUGGETS("nuggets", "Nuggets and fragments"),
        
        // Food & Agriculture
        RAW_FOOD("raw_food", "Raw meats and produce"),
        COOKED_FOOD("cooked_food", "Cooked foods"),
        CROPS("crops", "Crops and seeds"),
        
        // Combat & Tools
        SWORDS("swords", "Swords and blades"),
        AXES("axes", "Axes"),
        PICKAXES("pickaxes", "Pickaxes"),
        SHOVELS("shovels", "Shovels"),
        HOES("hoes", "Hoes"),
        BOWS("bows", "Bows and crossbows"),
        
        // Armor
        HELMETS("helmets", "Helmets and headgear"),
        CHESTPLATES("chestplates", "Chestplates and body armor"),
        LEGGINGS("leggings", "Leggings"),
        BOOTS("boots", "Boots and footwear"),
        
        // Magic & Potions
        POTIONS("potions", "Potions and elixirs"),
        SPELL_ITEMS("spell_items", "Magical items and scrolls"),
        ENCHANTED_BOOKS("enchanted_books", "Enchanted books"),
        
        // Building & Decoration
        BUILDING_BLOCKS("building_blocks", "Building blocks"),
        DECORATION("decoration", "Decorative items"),
        FURNITURE("furniture", "Furniture and furnishings"),
        LIGHTS("lights", "Light sources"),
        
        // Mob Drops
        MOB_DROPS("mob_drops", "Monster drops"),
        ANIMAL_DROPS("animal_drops", "Animal products"),
        
        // Miscellaneous
        SPAWN_EGGS("spawn_eggs", "Spawn eggs"),
        MUSIC_DISCS("music_discs", "Music discs"),
        DYES("dyes", "Dyes and colorants"),
        MISC("misc", "Miscellaneous items");

        private final String id;
        private final String description;

        ItemCategory(String id, String description) {
            this.id = id;
            this.description = description;
        }

        public String getId() { return id; }
        public String getDescription() { return description; }
    }

    // ==================== Mod Theme Definitions ====================

    /**
     * Defines a mod theme that can generate a town.
     */
    public record ModTheme(
        String modId,
        String townId,
        String townName,
        String description,
        TownData.TownType type,
        int distance,
        int requiredLevel,
        List<String> tagPrefixes,      // Item tag prefixes to match (e.g., "farmersdelight:")
        List<ItemCategory> buyCategories,  // Categories this town wants to buy
        List<ItemCategory> sellCategories  // Categories this town sells
    ) {}

    // Known mod themes for auto-generation
    private static final List<ModTheme> MOD_THEMES = new ArrayList<>();

    static {
        // Farmer's Delight - Food & Cooking
        MOD_THEMES.add(new ModTheme(
            "farmersdelight", "farmersdelight_market", "Gourmet Market",
            "A bustling market specializing in fine cuisine and cooking supplies from across the land.",
            TownData.TownType.MARKET, 2, 1,
            List.of("farmersdelight:"),
            List.of(ItemCategory.RAW_FOOD, ItemCategory.CROPS),
            List.of(ItemCategory.COOKED_FOOD, ItemCategory.RAW_FOOD)
        ));

        // Alex's Mobs - Exotic Animals
        MOD_THEMES.add(new ModTheme(
            "alexsmobs", "alexsmobs_menagerie", "Exotic Menagerie",
            "A collection of rare creatures and animal products from distant lands.",
            TownData.TownType.MARKET, 3, 2,
            List.of("alexsmobs:"),
            List.of(ItemCategory.ANIMAL_DROPS, ItemCategory.RAW_FOOD),
            List.of(ItemCategory.MOB_DROPS, ItemCategory.ANIMAL_DROPS, ItemCategory.SPAWN_EGGS)
        ));

        // Iron's Spellbooks - Magic
        MOD_THEMES.add(new ModTheme(
            "irons_spellbooks", "irons_arcane", "Arcane Emporium",
            "A mysterious shop dealing in magical artifacts and spell components.",
            TownData.TownType.MARKET, 4, 3,
            List.of("irons_spellbooks:"),
            List.of(ItemCategory.GEMS, ItemCategory.MOB_DROPS),
            List.of(ItemCategory.SPELL_ITEMS, ItemCategory.POTIONS)
        ));

        // Twilight Forest
        MOD_THEMES.add(new ModTheme(
            "twilightforest", "twilight_outpost", "Twilight Outpost",
            "A trading post on the edge of the mysterious Twilight Forest.",
            TownData.TownType.OUTPOST, 5, 2,
            List.of("twilightforest:"),
            List.of(ItemCategory.INGOTS, ItemCategory.COOKED_FOOD),
            List.of(ItemCategory.GEMS, ItemCategory.MOB_DROPS, ItemCategory.DECORATION)
        ));

        // Blue Skies
        MOD_THEMES.add(new ModTheme(
            "blue_skies", "skylands_trading", "Skylands Trading Co.",
            "Merchants from the floating islands above, dealing in rare sky materials.",
            TownData.TownType.MARKET, 5, 3,
            List.of("blue_skies:"),
            List.of(ItemCategory.INGOTS, ItemCategory.CROPS),
            List.of(ItemCategory.ORES, ItemCategory.GEMS, ItemCategory.MOB_DROPS)
        ));

        // Aether
        MOD_THEMES.add(new ModTheme(
            "aether", "aether_haven", "Aether Haven",
            "A celestial trading post dealing in heavenly goods from the Aether.",
            TownData.TownType.CITY, 6, 3,
            List.of("aether:"),
            List.of(ItemCategory.INGOTS, ItemCategory.RAW_FOOD),
            List.of(ItemCategory.GEMS, ItemCategory.ORES, ItemCategory.COOKED_FOOD)
        ));

        // Ars Nouveau - Magic
        MOD_THEMES.add(new ModTheme(
            "ars_nouveau", "ars_academy", "Arcane Academy",
            "A prestigious academy of magic, trading in mystical reagents and artifacts.",
            TownData.TownType.CITY, 4, 2,
            List.of("ars_nouveau:", "ars_elemental:"),
            List.of(ItemCategory.GEMS, ItemCategory.CROPS),
            List.of(ItemCategory.SPELL_ITEMS, ItemCategory.POTIONS, ItemCategory.DECORATION)
        ));

        // Apotheosis - Enchanting
        MOD_THEMES.add(new ModTheme(
            "apotheosis", "apotheosis_sanctum", "Enchanter's Sanctum",
            "A haven for enchanters, dealing in rare gems and magical components.",
            TownData.TownType.MARKET, 3, 2,
            List.of("apotheosis:"),
            List.of(ItemCategory.GEMS, ItemCategory.ENCHANTED_BOOKS),
            List.of(ItemCategory.GEMS, ItemCategory.ENCHANTED_BOOKS)
        ));

        // Aquaculture - Fishing
        MOD_THEMES.add(new ModTheme(
            "aquaculture", "aquaculture_docks", "Fisher's Docks",
            "A seaside market specializing in exotic fish and fishing equipment.",
            TownData.TownType.TOWN, 2, 1,
            List.of("aquaculture:"),
            List.of(ItemCategory.INGOTS, ItemCategory.CROPS),
            List.of(ItemCategory.RAW_FOOD, ItemCategory.COOKED_FOOD)
        ));

        // Minecolonies
        MOD_THEMES.add(new ModTheme(
            "minecolonies", "colony_exchange", "Colony Exchange",
            "A trade hub connecting various colonies and settlements.",
            TownData.TownType.CITY, 3, 2,
            List.of("minecolonies:"),
            List.of(ItemCategory.RAW_FOOD, ItemCategory.BUILDING_BLOCKS),
            List.of(ItemCategory.BUILDING_BLOCKS, ItemCategory.DECORATION)
        ));

        // Let's Do series (Vinery, Bakery)
        MOD_THEMES.add(new ModTheme(
            "vinery", "vinery_vineyard", "Vineyard Estate",
            "A refined establishment dealing in fine wines and gourmet produce.",
            TownData.TownType.VILLAGE, 3, 2,
            List.of("vinery:", "bakery:"),
            List.of(ItemCategory.CROPS, ItemCategory.RAW_FOOD),
            List.of(ItemCategory.COOKED_FOOD, ItemCategory.CROPS)
        ));

        // Cataclysm
        MOD_THEMES.add(new ModTheme(
            "cataclysm", "cataclysm_armory", "Cataclysm Armory",
            "A dangerous outpost trading in powerful weapons and rare boss drops.",
            TownData.TownType.OUTPOST, 6, 4,
            List.of("cataclysm:"),
            List.of(ItemCategory.INGOTS, ItemCategory.GEMS),
            List.of(ItemCategory.SWORDS, ItemCategory.MOB_DROPS)
        ));

        // Simply Swords
        MOD_THEMES.add(new ModTheme(
            "simplyswords", "simplyswords_forge", "Blademaster's Forge",
            "A renowned forge specializing in exotic and magical weaponry.",
            TownData.TownType.MARKET, 4, 3,
            List.of("simplyswords:"),
            List.of(ItemCategory.INGOTS, ItemCategory.GEMS),
            List.of(ItemCategory.SWORDS)
        ));

        // Supplementaries
        MOD_THEMES.add(new ModTheme(
            "supplementaries", "supplementaries_bazaar", "Curiosity Bazaar",
            "A quirky shop full of useful gadgets and decorative oddities.",
            TownData.TownType.MARKET, 2, 1,
            List.of("supplementaries:"),
            List.of(ItemCategory.INGOTS, ItemCategory.CROPS),
            List.of(ItemCategory.DECORATION, ItemCategory.FURNITURE, ItemCategory.LIGHTS)
        ));

        // Quark
        MOD_THEMES.add(new ModTheme(
            "quark", "quark_emporium", "Quality Emporium",
            "A well-stocked shop with quality goods for the discerning buyer.",
            TownData.TownType.MARKET, 2, 1,
            List.of("quark:"),
            List.of(ItemCategory.INGOTS, ItemCategory.CROPS),
            List.of(ItemCategory.BUILDING_BLOCKS, ItemCategory.DECORATION)
        ));

        // Relics
        MOD_THEMES.add(new ModTheme(
            "relics", "relics_curator", "Relic Curator",
            "A collector of ancient artifacts and powerful relics.",
            TownData.TownType.MARKET, 5, 3,
            List.of("relics:"),
            List.of(ItemCategory.GEMS, ItemCategory.MOB_DROPS),
            List.of(ItemCategory.MISC)
        ));

        // Decorative Blocks / Fantasy Furniture
        MOD_THEMES.add(new ModTheme(
            "fantasyfurniture", "fantasy_furnishings", "Fantasy Furnishings",
            "A specialty shop for elegant furniture and home décor.",
            TownData.TownType.MARKET, 3, 2,
            List.of("fantasyfurniture:", "decorative_blocks:"),
            List.of(ItemCategory.BUILDING_BLOCKS, ItemCategory.INGOTS),
            List.of(ItemCategory.FURNITURE, ItemCategory.DECORATION)
        ));

        // Enigmatic Legacy
        MOD_THEMES.add(new ModTheme(
            "enigmaticlegacy", "enigmatic_vault", "Enigmatic Vault",
            "A mysterious vault containing powerful and ancient artifacts.",
            TownData.TownType.MARKET, 5, 4,
            List.of("enigmaticlegacy:"),
            List.of(ItemCategory.GEMS, ItemCategory.INGOTS),
            List.of(ItemCategory.MISC)
        ));

        // Oh The Biomes You'll Go
        MOD_THEMES.add(new ModTheme(
            "byg", "byg_explorers", "Explorer's Guild",
            "A guild of explorers trading in exotic woods and rare biome materials.",
            TownData.TownType.TOWN, 4, 2,
            List.of("byg:"),
            List.of(ItemCategory.INGOTS, ItemCategory.COOKED_FOOD),
            List.of(ItemCategory.BUILDING_BLOCKS, ItemCategory.CROPS, ItemCategory.DECORATION)
        ));
    }

    // ==================== Cached Data ====================

    private static Map<String, Set<Item>> itemsByMod = null;
    private static Map<ItemCategory, Set<Item>> itemsByCategory = null;
    private static Map<String, TownData> dynamicTowns = null;
    private static boolean initialized = false;

    // ==================== Initialization ====================

    /**
     * Initialize the mod compatibility system.
     * Call this after registries are frozen (e.g., in FMLCommonSetupEvent).
     */
    public static void initialize() {
        if (initialized) return;
        
        itemsByMod = new HashMap<>();
        itemsByCategory = new EnumMap<>(ItemCategory.class);
        dynamicTowns = new LinkedHashMap<>();

        // Initialize category maps
        for (ItemCategory cat : ItemCategory.values()) {
            itemsByCategory.put(cat, new HashSet<>());
        }

        // Scan all items in the registry
        int modItemCount = 0;
        Set<String> discoveredMods = new HashSet<>();
        
        for (Item item : ForgeRegistries.ITEMS) {
            ResourceLocation rl = ForgeRegistries.ITEMS.getKey(item);
            if (rl == null) continue;

            String modId = rl.getNamespace();
            
            // Skip minecraft items (already handled)
            if (modId.equals("minecraft")) continue;

            // Add to mod map
            itemsByMod.computeIfAbsent(modId, k -> new HashSet<>()).add(item);
            discoveredMods.add(modId);
            modItemCount++;

            // Categorize item (wrapped in try-catch so one bad modded item can't crash everything)
            try {
                categorizeItem(item, rl);
            } catch (Exception e) {
                OffToMarket.LOGGER.warn("[ModCompat] Failed to categorize item {}: {}", rl, e.getMessage());
                continue;
            }
            
            // Log if enabled
            if (ModConfig.logDiscoveredItems) {
                OffToMarket.LOGGER.info("[ModCompat] Discovered: {} (category: {})", 
                    rl, getCategoryFor(item));
            }
        }

        // Log summary
        OffToMarket.LOGGER.info("[ModCompat] Discovered {} items from {} mods", modItemCount, discoveredMods.size());
        
        // Generate dynamic towns for loaded mods (if enabled)
        if (ModConfig.enableDynamicTowns) {
            generateDynamicTowns();
            OffToMarket.LOGGER.info("[ModCompat] Generated {} dynamic mod towns", dynamicTowns.size());
        } else {
            OffToMarket.LOGGER.info("[ModCompat] Dynamic towns disabled in config");
        }

        initialized = true;
    }
    
    /**
     * Get the primary category for an item.
     */
    private static String getCategoryFor(Item item) {
        for (Map.Entry<ItemCategory, Set<Item>> entry : itemsByCategory.entrySet()) {
            if (entry.getValue().contains(item)) {
                return entry.getKey().id;
            }
        }
        return "uncategorized";
    }

    // ==================== Item Categorization ====================

    private static void categorizeItem(Item item, ResourceLocation rl) {
        String path = rl.getPath().toLowerCase();
        ItemStack stack = new ItemStack(item);

        // Check tags first (most reliable for mod compat)
        if (isTagged(stack, Tags.Items.INGOTS)) {
            itemsByCategory.get(ItemCategory.INGOTS).add(item);
        }
        if (isTagged(stack, Tags.Items.GEMS)) {
            itemsByCategory.get(ItemCategory.GEMS).add(item);
        }
        if (isTagged(stack, Tags.Items.ORES)) {
            itemsByCategory.get(ItemCategory.ORES).add(item);
        }
        if (isTagged(stack, Tags.Items.DUSTS)) {
            itemsByCategory.get(ItemCategory.DUSTS).add(item);
        }
        if (isTagged(stack, Tags.Items.NUGGETS)) {
            itemsByCategory.get(ItemCategory.NUGGETS).add(item);
        }
        if (isTagged(stack, Tags.Items.DYES)) {
            itemsByCategory.get(ItemCategory.DYES).add(item);
        }

        // Check item type
        if (item instanceof SwordItem) {
            itemsByCategory.get(ItemCategory.SWORDS).add(item);
        } else if (item instanceof AxeItem) {
            itemsByCategory.get(ItemCategory.AXES).add(item);
        } else if (item instanceof PickaxeItem) {
            itemsByCategory.get(ItemCategory.PICKAXES).add(item);
        } else if (item instanceof ShovelItem) {
            itemsByCategory.get(ItemCategory.SHOVELS).add(item);
        } else if (item instanceof HoeItem) {
            itemsByCategory.get(ItemCategory.HOES).add(item);
        } else if (item instanceof BowItem || item instanceof CrossbowItem) {
            itemsByCategory.get(ItemCategory.BOWS).add(item);
        } else if (item instanceof ArmorItem armor) {
            // Get equipment slot from a stack of the item (may be null for some modded armor)
            EquipmentSlot slot = armor.getEquipmentSlot(new ItemStack(armor));
            if (slot != null) {
                switch (slot) {
                    case HEAD -> itemsByCategory.get(ItemCategory.HELMETS).add(item);
                    case CHEST -> itemsByCategory.get(ItemCategory.CHESTPLATES).add(item);
                    case LEGS -> itemsByCategory.get(ItemCategory.LEGGINGS).add(item);
                    case FEET -> itemsByCategory.get(ItemCategory.BOOTS).add(item);
                    default -> {} // Ignore non-standard slots
                }
            }
        } else if (item instanceof PotionItem || item instanceof TippedArrowItem) {
            itemsByCategory.get(ItemCategory.POTIONS).add(item);
        } else if (item instanceof EnchantedBookItem) {
            itemsByCategory.get(ItemCategory.ENCHANTED_BOOKS).add(item);
        } else if (item instanceof SpawnEggItem) {
            itemsByCategory.get(ItemCategory.SPAWN_EGGS).add(item);
        } else if (item instanceof RecordItem) {
            itemsByCategory.get(ItemCategory.MUSIC_DISCS).add(item);
        } else if (item instanceof BlockItem) {
            // Check for decoration/building blocks
            if (path.contains("brick") || path.contains("stone") || path.contains("wood") ||
                path.contains("plank") || path.contains("slab") || path.contains("stair") ||
                path.contains("wall") || path.contains("fence") || path.contains("pillar")) {
                itemsByCategory.get(ItemCategory.BUILDING_BLOCKS).add(item);
            } else if (path.contains("lamp") || path.contains("lantern") || path.contains("light") ||
                       path.contains("candle") || path.contains("torch")) {
                itemsByCategory.get(ItemCategory.LIGHTS).add(item);
            } else if (path.contains("chair") || path.contains("table") || path.contains("bench") ||
                       path.contains("shelf") || path.contains("cabinet") || path.contains("bed") ||
                       path.contains("desk") || path.contains("stool")) {
                itemsByCategory.get(ItemCategory.FURNITURE).add(item);
            } else if (path.contains("flower") || path.contains("pot") || path.contains("banner") ||
                       path.contains("carpet") || path.contains("painting") || path.contains("statue")) {
                itemsByCategory.get(ItemCategory.DECORATION).add(item);
            }
        }

        // Food check
        FoodProperties food = item.getFoodProperties();
        if (food != null) {
            // Check if it's cooked (higher nutrition usually means cooked)
            if (path.contains("cooked") || path.contains("baked") || path.contains("fried") ||
                path.contains("roast") || path.contains("grilled") || food.getNutrition() >= 6) {
                itemsByCategory.get(ItemCategory.COOKED_FOOD).add(item);
            } else {
                itemsByCategory.get(ItemCategory.RAW_FOOD).add(item);
            }
        }

        // Path-based categorization for remaining items
        if (path.contains("seed") || path.contains("sapling")) {
            itemsByCategory.get(ItemCategory.CROPS).add(item);
        }
        if (path.contains("spell") || path.contains("scroll") || path.contains("wand") ||
            path.contains("staff") || path.contains("tome") || path.contains("grimoire")) {
            itemsByCategory.get(ItemCategory.SPELL_ITEMS).add(item);
        }
        if (path.contains("hide") || path.contains("pelt") || path.contains("fur") ||
            path.contains("feather") || path.contains("scale") || path.contains("horn") ||
            path.contains("fang") || path.contains("claw") || path.contains("talon")) {
            itemsByCategory.get(ItemCategory.ANIMAL_DROPS).add(item);
        }
        if (path.contains("bone") || path.contains("skull") || path.contains("heart") ||
            path.contains("eye") || path.contains("essence") || path.contains("soul") ||
            path.contains("tooth") || path.contains("slime")) {
            itemsByCategory.get(ItemCategory.MOB_DROPS).add(item);
        }
    }

    private static boolean isTagged(ItemStack stack, TagKey<Item> tag) {
        return stack.is(tag);
    }

    // ==================== Dynamic Town Generation ====================

    private static void generateDynamicTowns() {
        for (ModTheme theme : MOD_THEMES) {
            if (ModList.get().isLoaded(theme.modId())) {
                TownData town = createTownFromTheme(theme);
                if (town != null) {
                    dynamicTowns.put(town.getId(), town);
                }
            }
        }
    }

    private static TownData createTownFromTheme(ModTheme theme) {
        Set<Item> modItems = itemsByMod.get(theme.modId());
        if (modItems == null || modItems.isEmpty()) return null;

        int maxItems = ModConfig.maxItemsPerCategory;

        // Build needs (items town wants to buy)
        Set<ResourceLocation> needs = new HashSet<>();
        for (ItemCategory cat : theme.buyCategories()) {
            Set<Item> catItems = itemsByCategory.get(cat);
            if (catItems != null) {
                int countInCategory = 0;
                for (Item item : catItems) {
                    ResourceLocation rl = ForgeRegistries.ITEMS.getKey(item);
                    if (rl != null) {
                        needs.add(rl);
                        countInCategory++;
                        if (countInCategory >= maxItems / 3) break; // Limit per category
                    }
                }
            }
        }
        // Add some vanilla staples as needs
        needs.add(new ResourceLocation("minecraft", "iron_ingot"));
        needs.add(new ResourceLocation("minecraft", "gold_ingot"));
        needs.add(new ResourceLocation("minecraft", "emerald"));
        needs.add(new ResourceLocation("minecraft", "diamond"));

        // Build surplus (items town has too much of, pays less)
        Set<ResourceLocation> surplus = new HashSet<>();
        for (Item item : modItems) {
            ResourceLocation rl = ForgeRegistries.ITEMS.getKey(item);
            if (rl != null) {
                surplus.add(rl);
                if (surplus.size() >= maxItems / 5) break;
            }
        }

        // Build specialty (items town sells)
        Set<ResourceLocation> specialty = new HashSet<>();
        // First add mod-specific items
        for (Item item : modItems) {
            ResourceLocation rl = ForgeRegistries.ITEMS.getKey(item);
            if (rl != null) {
                specialty.add(rl);
                if (specialty.size() >= maxItems / 2) break;
            }
        }
        // Then add from sell categories (from any mod)
        for (ItemCategory cat : theme.sellCategories()) {
            Set<Item> catItems = itemsByCategory.get(cat);
            if (catItems != null) {
                for (Item item : catItems) {
                    ResourceLocation rl = ForgeRegistries.ITEMS.getKey(item);
                    if (rl != null && !specialty.contains(rl)) {
                        specialty.add(rl);
                        if (specialty.size() >= maxItems) break;
                    }
                }
            }
        }

        return new TownData(
            theme.townId(),
            theme.townName(),
            theme.description(),
            theme.distance(),
            theme.type(),
            needs,
            surplus,
            specialty,
            theme.requiredLevel()
        );
    }

    // ==================== Public API ====================

    /**
     * Get all dynamically generated towns.
     */
    public static Map<String, TownData> getDynamicTowns() {
        if (!initialized) initialize();
        return Collections.unmodifiableMap(dynamicTowns);
    }

    /**
     * Get items from a specific mod.
     */
    public static Set<Item> getItemsFromMod(String modId) {
        if (!initialized) initialize();
        return itemsByMod.getOrDefault(modId, Collections.emptySet());
    }

    /**
     * Get items in a specific category.
     */
    public static Set<Item> getItemsByCategory(ItemCategory category) {
        if (!initialized) initialize();
        return itemsByCategory.getOrDefault(category, Collections.emptySet());
    }

    /**
     * Check if a mod is loaded.
     */
    public static boolean isModLoaded(String modId) {
        return ModList.get().isLoaded(modId);
    }

    /**
     * Get all loaded mod IDs that have items.
     */
    public static Set<String> getLoadedModsWithItems() {
        if (!initialized) initialize();
        return Collections.unmodifiableSet(itemsByMod.keySet());
    }

    /**
     * Categorize a single item and return its categories.
     */
    public static Set<ItemCategory> getCategoriesForItem(Item item) {
        if (!initialized) initialize();
        
        Set<ItemCategory> result = EnumSet.noneOf(ItemCategory.class);
        for (Map.Entry<ItemCategory, Set<Item>> entry : itemsByCategory.entrySet()) {
            if (entry.getValue().contains(item)) {
                result.add(entry.getKey());
            }
        }
        return result;
    }

    /**
     * Get a random selection of items from a mod for market listings.
     */
    public static List<Item> getRandomItemsFromMod(String modId, int count, Random random) {
        Set<Item> modItems = getItemsFromMod(modId);
        if (modItems.isEmpty()) return Collections.emptyList();

        List<Item> list = new ArrayList<>(modItems);
        Collections.shuffle(list, random);
        return list.subList(0, Math.min(count, list.size()));
    }

    /**
     * Get a random selection of items from a category.
     */
    public static List<Item> getRandomItemsFromCategory(ItemCategory category, int count, Random random) {
        Set<Item> catItems = getItemsByCategory(category);
        if (catItems.isEmpty()) return Collections.emptyList();

        List<Item> list = new ArrayList<>(catItems);
        Collections.shuffle(list, random);
        return list.subList(0, Math.min(count, list.size()));
    }
}
