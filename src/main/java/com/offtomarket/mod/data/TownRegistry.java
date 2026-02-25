package com.offtomarket.mod.data;

import net.minecraft.resources.ResourceLocation;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Default town definitions. These are the fictional towns/villages available
 * for trade. Each has unique needs, surplus goods, specialty products,
 * distance, and minimum trader level requirements.
 */
public class TownRegistry {
    private static final Map<String, TownData> TOWNS = new LinkedHashMap<>();

    // ------------------------------------------------------------------
    // List caches: populated lazily, never invalidated (towns are static
    // after mod-load and dynamic towns are populated once at startup).
    // ------------------------------------------------------------------
    private static volatile Collection<TownData> cachedAllTowns = null;
    private static final ConcurrentHashMap<Integer, List<TownData>> cachedAvailableTowns = new ConcurrentHashMap<>();

    /** Invalidate list caches (call if dynamic towns change after startup). */
    public static void invalidateTownCaches() {
        cachedAllTowns = null;
        cachedAvailableTowns.clear();
    }

    static {
        // Close farming village - Level 1
        register(new TownData(
                "greenhollow",
                "Greenhollow Village",
                "A peaceful farming hamlet nestled in rolling hills. Known for wheat and produce.",
                1, TownData.TownType.VILLAGE,

            // NEEDS (items this town wants to buy, pays more for)
                setOf("minecraft:iron_ingot", "minecraft:iron_hoe", "minecraft:golden_hoe",
                      "minecraft:diamond_hoe", "minecraft:string", "minecraft:bucket",
                      "minecraft:shears", "minecraft:bone_meal", "minecraft:water_bucket",
                      "minecraft:composter"),

            // SURPLUS ITEMS (items this town has excess of, pays less for)
                setOf("minecraft:wheat", "minecraft:carrot", "minecraft:potato",
                      "minecraft:beetroot", "minecraft:melon_slice", "minecraft:pumpkin",
                      "minecraft:hay_block", "minecraft:wheat_seeds"),
            // SPECIALTY ITEMS (items this town sells)
                setOf("minecraft:wheat", "minecraft:bread", "minecraft:carrot",
                      "minecraft:potato", "minecraft:pumpkin_pie", "minecraft:leather",
                      "minecraft:beef", "minecraft:chicken", "minecraft:porkchop",
                      "minecraft:egg", "minecraft:beetroot", "minecraft:melon_slice",
                      "minecraft:apple", "minecraft:sweet_berries", "minecraft:sugar_cane",
                      "minecraft:mutton", "minecraft:rabbit", "minecraft:honey_bottle",
                      "minecraft:honeycomb", "minecraft:hay_block", "minecraft:milk_bucket",
                      "minecraft:cookie", "minecraft:cake", "minecraft:pumpkin",
                      "minecraft:mushroom_stew", "minecraft:rabbit_stew", "minecraft:feather",
                      "minecraft:white_wool", "minecraft:brown_wool", "minecraft:lead"),
                1
        ));

        // Nearby mining village - Level 1
        register(new TownData(
                "irondeep",
                "Irondeep Settlement",
                "A hardy mining community at the base of craggy mountains. Rich in ores.",
                2, TownData.TownType.VILLAGE,
            // NEEDS ITEMS (this town wants to buy, pays more for)
                setOf("minecraft:oak_planks", "minecraft:torch", "minecraft:bread",
                      "minecraft:cooked_beef", "minecraft:leather_boots", "minecraft:lantern",
                      "minecraft:ladder", "minecraft:rail", "minecraft:minecart"),

            // SURPLUS ITEMS (items this town has excess of, pays less for)
                setOf("minecraft:iron_ingot", "minecraft:coal", "minecraft:cobblestone",
                      "minecraft:raw_iron", "minecraft:raw_copper", "minecraft:gravel",
                      "minecraft:flint", "minecraft:andesite", "minecraft:diorite"),

            // SPECIALTY ITEMS (items this town sells)
                setOf("minecraft:iron_ingot", "minecraft:coal", "minecraft:copper_ingot",
                      "minecraft:stone_pickaxe", "minecraft:iron_pickaxe",
                      "minecraft:iron_ore", "minecraft:gold_ore",
                      "minecraft:raw_iron", "minecraft:raw_copper", "minecraft:raw_gold",
                      "minecraft:lapis_lazuli", "minecraft:redstone", "minecraft:cobblestone",
                      "minecraft:stone", "minecraft:deepslate", "minecraft:tuff",
                      "minecraft:flint", "minecraft:iron_nugget", "minecraft:gold_nugget",
                      "minecraft:iron_shovel", "minecraft:stone_sword", "minecraft:rail",
                      "minecraft:minecart", "minecraft:torch", "minecraft:lantern",
                      "minecraft:chain", "minecraft:iron_bars", "minecraft:iron_door",
                      "minecraft:amethyst_shard", "minecraft:dripstone_block"),
                1
        ));

        // Mid-distance fishing town - Level 1
        register(new TownData(
                  "saltmere",
                  "Saltmere Harbor",
                  "A bustling fishing town on the coast. Fresh catch daily.",
                  3, TownData.TownType.TOWN,
                  
                  // NEEDS ITEMS (this town wants to buy, pays more for)
                  setOf("minecraft:oak_planks", "minecraft:iron_ingot", "minecraft:bread",
                      "minecraft:white_wool", "minecraft:lantern", "minecraft:lead",
                      "minecraft:barrel", "minecraft:tripwire_hook"),
                  
                  // SURPLUS ITEMS (items this town has excess of, pays less for)
                  setOf("minecraft:cod", "minecraft:salmon", "minecraft:tropical_fish",
                      "minecraft:kelp", "minecraft:prismarine_shard", "minecraft:pufferfish",
                      "minecraft:seagrass"),

                  // SPECIALTY ITEMS (items this town sells)
                  setOf("minecraft:cod", "minecraft:salmon", "minecraft:nautilus_shell",
                      "minecraft:fishing_rod", "minecraft:prismarine_shard", "minecraft:string",
                      "minecraft:cooked_cod", "minecraft:cooked_salmon", "minecraft:tropical_fish",
                      "minecraft:pufferfish", "minecraft:kelp", "minecraft:dried_kelp",
                      "minecraft:dried_kelp_block", "minecraft:sea_lantern",
                      "minecraft:prismarine", "minecraft:dark_prismarine",
                      "minecraft:turtle_egg", "minecraft:scute",
                      "minecraft:ink_sac", "minecraft:glow_ink_sac",
                      "minecraft:heart_of_the_sea", "minecraft:boat",
                      "minecraft:lily_pad", "minecraft:sponge", "minecraft:trident"),
                1
        ));

        // Mid-distance lumber town - Level 2
        register(new TownData(
                "timberwatch",
                "Timberwatch",
                "A frontier logging town surrounded by ancient forests. Expert woodworkers.",
                4, TownData.TownType.TOWN,
                  // NEEDS ITEMS (this town wants to buy, pays more for)
                setOf("minecraft:iron_axe", "minecraft:diamond_axe", "minecraft:bread",
                      "minecraft:cooked_porkchop", "minecraft:iron_ingot",
                      "minecraft:shears", "minecraft:flint_and_steel", "minecraft:torch"),

                  // SURPLUS ITEMS (items this town has excess of, pays less for)
                setOf("minecraft:oak_log", "minecraft:spruce_log", "minecraft:birch_log",
                      "minecraft:oak_planks", "minecraft:stick", "minecraft:jungle_log",
                      "minecraft:acacia_log", "minecraft:charcoal"),

                  // SPECIALTY ITEMS (items this town sells)
                setOf("minecraft:oak_log", "minecraft:spruce_log", "minecraft:dark_oak_log",
                      "minecraft:bow", "minecraft:shield", "minecraft:bookshelf",
                      "minecraft:birch_log", "minecraft:jungle_log", "minecraft:acacia_log",
                      "minecraft:mangrove_log", "minecraft:cherry_log",
                      "minecraft:oak_planks", "minecraft:spruce_planks", "minecraft:dark_oak_planks",
                      "minecraft:stick", "minecraft:crafting_table", "minecraft:chest",
                      "minecraft:barrel", "minecraft:ladder", "minecraft:fence",
                      "minecraft:oak_door", "minecraft:oak_boat",
                      "minecraft:charcoal", "minecraft:campfire",
                      "minecraft:stripped_oak_log", "minecraft:stripped_spruce_log",
                      "minecraft:bamboo", "minecraft:scaffolding",
                      "minecraft:note_block", "minecraft:jukebox",
                      "minecraft:arrow", "minecraft:crossbow", "minecraft:composter"),
                2
        ));

        // Far trading town - Level 2
        register(new TownData(
                "crossroads",
                "Crossroads Market",
                "A major trade hub where many roads converge. Everything has a price.",
                5, TownData.TownType.TOWN,
                  // NEEDS ITEMS (this town wants to buy, pays more for)
                setOf("minecraft:diamond", "minecraft:emerald", "minecraft:gold_ingot",
                      "minecraft:ender_pearl", "minecraft:blaze_rod",
                      "minecraft:lapis_lazuli", "minecraft:amethyst_shard"),
                  // SURPLUS ITEMS (items this town has excess of, pays less for)
                setOf("minecraft:apple", "minecraft:arrow", "minecraft:glass",
                      "minecraft:map", "minecraft:paper", "minecraft:compass"),
                  // SPECIALTY ITEMS (items this town sells)
                setOf("minecraft:emerald", "minecraft:saddle", "minecraft:name_tag",
                      "minecraft:diamond", "minecraft:enchanted_book",
                      "minecraft:gold_ingot", "minecraft:clock", "minecraft:compass",
                      "minecraft:map", "minecraft:spyglass", "minecraft:bell",
                      "minecraft:ender_pearl", "minecraft:ender_chest",
                      "minecraft:golden_apple", "minecraft:glass_bottle",
                      "minecraft:flower_banner_pattern", "minecraft:globe_banner_pattern",
                      "minecraft:painting", "minecraft:item_frame",
                      "minecraft:armor_stand", "minecraft:firework_rocket",
                      "minecraft:music_disc_cat", "minecraft:music_disc_blocks",
                      "minecraft:tnt", "minecraft:slime_ball", "minecraft:lead",
                      "minecraft:goat_horn", "minecraft:recovery_compass"),
                2
        ));

        // Distant fortress city - Level 3
        register(new TownData(
                "basaltkeep",
                "Basaltkeep Fortress",
                "An imposing fortress city of blackstone and iron. Skilled armorers and weaponsmiths.",
                7, TownData.TownType.CITY,
                  // NEEDS ITEMS (this town wants to buy, pays more for)
                setOf("minecraft:diamond", "minecraft:leather", "minecraft:bread",
                      "minecraft:golden_apple", "minecraft:obsidian",
                      "minecraft:netherite_scrap", "minecraft:blaze_powder",
                      "minecraft:phantom_membrane"),
                  // SURPLUS ITEMS (items this town has excess of, pays less for)
                setOf("minecraft:iron_sword", "minecraft:iron_chestplate",
                      "minecraft:iron_helmet", "minecraft:chainmail_chestplate",
                      "minecraft:arrow", "minecraft:shield", "minecraft:iron_nugget"),
                      // SPECIALTY ITEMS (items this town sells)
                setOf("minecraft:iron_sword", "minecraft:iron_chestplate",
                      "minecraft:chainmail_leggings", "minecraft:crossbow",
                      "minecraft:shield", "minecraft:anvil",
                      "minecraft:iron_helmet", "minecraft:iron_leggings",
                      "minecraft:iron_boots", "minecraft:iron_axe",
                      "minecraft:chainmail_helmet", "minecraft:chainmail_chestplate",
                      "minecraft:chainmail_boots",
                      "minecraft:diamond_sword", "minecraft:diamond_helmet",
                      "minecraft:diamond_chestplate", "minecraft:diamond_leggings",
                      "minecraft:diamond_boots",
                      "minecraft:spectral_arrow", "minecraft:tipped_arrow",
                      "minecraft:netherite_scrap",
                      "minecraft:obsidian", "minecraft:crying_obsidian",
                      "minecraft:blackstone", "minecraft:polished_blackstone_bricks",
                      "minecraft:lodestone", "minecraft:respawn_anchor",
                      "minecraft:fire_charge", "minecraft:blaze_rod",
                      "minecraft:golden_sword", "minecraft:golden_chestplate"),
                3
        ));

        // Very far enchanting city - Level 4
        register(new TownData(
                "arcaneveil",
                "Arcaneveil Spire",
                "A mysterious city of scholars and enchanters. Knowledge is currency here.",
                8, TownData.TownType.CITY,
                // NEEDS ITEMS (this town wants to buy, pays more for)
                setOf("minecraft:lapis_lazuli", "minecraft:book", "minecraft:blaze_rod",
                      "minecraft:ender_pearl", "minecraft:experience_bottle",
                      "minecraft:ghast_tear", "minecraft:nether_wart",
                      "minecraft:amethyst_shard", "minecraft:echo_shard"),
                  // SURPLUS ITEMS (items this town has excess of, pays less for)
                setOf("minecraft:enchanted_book", "minecraft:glass_bottle",
                      "minecraft:potion", "minecraft:redstone",
                      "minecraft:glowstone_dust", "minecraft:paper"),
                  // SPECIALTY ITEMS (items this town sells)
                setOf("minecraft:enchanted_book", "minecraft:experience_bottle",
                      "minecraft:ender_eye", "minecraft:brewing_stand",
                      "minecraft:bookshelf", "minecraft:redstone", "minecraft:book",
                      "minecraft:lapis_lazuli", "minecraft:glowstone",
                      "minecraft:glowstone_dust", "minecraft:redstone_block",
                      "minecraft:enchanting_table", "minecraft:end_crystal",
                      "minecraft:potion", "minecraft:splash_potion",
                      "minecraft:blaze_powder", "minecraft:fermented_spider_eye",
                      "minecraft:magma_cream", "minecraft:ghast_tear",
                      "minecraft:rabbit_foot", "minecraft:phantom_membrane",
                      "minecraft:dragon_breath", "minecraft:nether_wart",
                      "minecraft:soul_lantern", "minecraft:soul_torch",
                      "minecraft:amethyst_shard", "minecraft:amethyst_block",
                      "minecraft:sculk_sensor", "minecraft:echo_shard",
                      "minecraft:conduit", "minecraft:writable_book",
                      "minecraft:candle", "minecraft:shroomlight"),
                4
        ));

        // Farthest exotic city - Level 5
        register(new TownData(
                "goldspire",
                "Goldspire Capital",
                "The legendary capital of trade. Only the most accomplished merchants reach its golden gates.",
                10, TownData.TownType.CITY,
                setOf("minecraft:netherite_ingot", "minecraft:diamond_block",
                      "minecraft:emerald_block", "minecraft:beacon",
                      "minecraft:elytra", "minecraft:totem_of_undying",
                      "minecraft:nether_star", "minecraft:dragon_egg",
                      "minecraft:heart_of_the_sea"),
                setOf(), // no surplus
                setOf("minecraft:netherite_scrap", "minecraft:diamond_block",
                      "minecraft:beacon", "minecraft:totem_of_undying",
                      "minecraft:elytra", "minecraft:nether_star",
                      "minecraft:netherite_ingot", "minecraft:netherite_sword",
                      "minecraft:netherite_pickaxe", "minecraft:netherite_axe",
                      "minecraft:netherite_chestplate", "minecraft:netherite_helmet",
                      "minecraft:netherite_leggings", "minecraft:netherite_boots",
                      "minecraft:emerald_block", "minecraft:gold_block",
                      "minecraft:enchanted_golden_apple",
                      "minecraft:dragon_breath", "minecraft:end_crystal",
                      "minecraft:shulker_box", "minecraft:shulker_shell",
                      "minecraft:chorus_fruit", "minecraft:popped_chorus_fruit",
                      "minecraft:end_rod", "minecraft:purpur_block",
                      "minecraft:ender_chest", "minecraft:lodestone",
                      "minecraft:respawn_anchor", "minecraft:conduit",
                      "minecraft:music_disc_pigstep", "minecraft:music_disc_otherside",
                      "minecraft:wither_skeleton_skull", "minecraft:skeleton_skull",
                      "minecraft:creeper_head", "minecraft:piglin_head"),
                5
        ));

        // Peenam Animal Market - Specialized animal trader - Level 1
        register(new TownData(
                "peenam",
                "Peenam Animal Market",
                "A lively marketplace famous for trading livestock and exotic creatures. If it walks, flies, or swims, Peenam has it.",
                2, TownData.TownType.VILLAGE,
                // NEEDS (items this town wants to buy - animal products)
                setOf("minecraft:wheat", "minecraft:carrot", "minecraft:apple",
                      "minecraft:golden_carrot", "minecraft:golden_apple",
                      "minecraft:hay_block", "minecraft:lead", "minecraft:saddle",
                      "minecraft:name_tag", "minecraft:bucket"),
                // SURPLUS (has plenty of animal products)
                setOf("minecraft:leather", "minecraft:feather", "minecraft:egg",
                      "minecraft:white_wool", "minecraft:beef", "minecraft:porkchop",
                      "minecraft:chicken", "minecraft:mutton", "minecraft:rabbit",
                      "minecraft:rabbit_hide", "minecraft:rabbit_foot"),
                // SPECIALTY ITEMS (sells animal products AND special animal slips)
                setOf("minecraft:leather", "minecraft:feather", "minecraft:egg",
                      "minecraft:white_wool", "minecraft:brown_wool", "minecraft:black_wool",
                      "minecraft:beef", "minecraft:porkchop", "minecraft:chicken",
                      "minecraft:mutton", "minecraft:rabbit", "minecraft:rabbit_hide",
                      "minecraft:rabbit_foot", "minecraft:honey_bottle", "minecraft:honeycomb",
                      "minecraft:milk_bucket", "minecraft:saddle", "minecraft:lead",
                      "minecraft:name_tag", "minecraft:horse_armor_iron",
                      "minecraft:horse_armor_golden", "minecraft:horse_armor_diamond"),
                1
        ));

        // ==================================================================
        // Town Specializations — Granular NeedLevel Overrides
        // ==================================================================
        // These overlay the binary needs/surplus sets with graduated demand
        // levels, creating richer market dynamics per town.

        // ── Greenhollow (farming village) ──
        // Desperate for tools they can't craft; oversaturated on produce
        setNeed("greenhollow", "minecraft:iron_ingot", NeedLevel.DESPERATE);
        setNeed("greenhollow", "minecraft:iron_hoe", NeedLevel.DESPERATE);
        setNeed("greenhollow", "minecraft:shears", NeedLevel.MODERATE_NEED);
        setNeed("greenhollow", "minecraft:bucket", NeedLevel.MODERATE_NEED);
        setNeed("greenhollow", "minecraft:wheat", NeedLevel.OVERSATURATED);
        setNeed("greenhollow", "minecraft:carrot", NeedLevel.OVERSATURATED);
        setNeed("greenhollow", "minecraft:potato", NeedLevel.OVERSATURATED);
        setNeed("greenhollow", "minecraft:hay_block", NeedLevel.OVERSATURATED);
        setNeed("greenhollow", "minecraft:melon_slice", NeedLevel.SURPLUS);
        setNeed("greenhollow", "minecraft:pumpkin", NeedLevel.SURPLUS);

        // ── Irondeep (mining settlement) ──
        // Desperate for food and wood; oversaturated on ores and stone
        setNeed("irondeep", "minecraft:bread", NeedLevel.DESPERATE);
        setNeed("irondeep", "minecraft:cooked_beef", NeedLevel.DESPERATE);
        setNeed("irondeep", "minecraft:oak_planks", NeedLevel.DESPERATE);
        setNeed("irondeep", "minecraft:torch", NeedLevel.MODERATE_NEED);
        setNeed("irondeep", "minecraft:lantern", NeedLevel.MODERATE_NEED);
        setNeed("irondeep", "minecraft:iron_ingot", NeedLevel.OVERSATURATED);
        setNeed("irondeep", "minecraft:coal", NeedLevel.OVERSATURATED);
        setNeed("irondeep", "minecraft:cobblestone", NeedLevel.OVERSATURATED);
        setNeed("irondeep", "minecraft:raw_iron", NeedLevel.SURPLUS);
        setNeed("irondeep", "minecraft:raw_copper", NeedLevel.SURPLUS);

        // ── Saltmere (fishing harbor) ──
        // Desperate for building materials; oversaturated on seafood
        setNeed("saltmere", "minecraft:oak_planks", NeedLevel.DESPERATE);
        setNeed("saltmere", "minecraft:iron_ingot", NeedLevel.MODERATE_NEED);
        setNeed("saltmere", "minecraft:lead", NeedLevel.MODERATE_NEED);
        setNeed("saltmere", "minecraft:cod", NeedLevel.OVERSATURATED);
        setNeed("saltmere", "minecraft:salmon", NeedLevel.OVERSATURATED);
        setNeed("saltmere", "minecraft:tropical_fish", NeedLevel.OVERSATURATED);
        setNeed("saltmere", "minecraft:kelp", NeedLevel.SURPLUS);
        setNeed("saltmere", "minecraft:pufferfish", NeedLevel.SURPLUS);

        // ── Timberwatch (lumber town) ──
        // Desperate for good axes and food; oversaturated on wood
        setNeed("timberwatch", "minecraft:iron_axe", NeedLevel.DESPERATE);
        setNeed("timberwatch", "minecraft:diamond_axe", NeedLevel.DESPERATE);
        setNeed("timberwatch", "minecraft:cooked_porkchop", NeedLevel.MODERATE_NEED);
        setNeed("timberwatch", "minecraft:bread", NeedLevel.MODERATE_NEED);
        setNeed("timberwatch", "minecraft:oak_log", NeedLevel.OVERSATURATED);
        setNeed("timberwatch", "minecraft:spruce_log", NeedLevel.OVERSATURATED);
        setNeed("timberwatch", "minecraft:birch_log", NeedLevel.OVERSATURATED);
        setNeed("timberwatch", "minecraft:oak_planks", NeedLevel.OVERSATURATED);
        setNeed("timberwatch", "minecraft:stick", NeedLevel.OVERSATURATED);
        setNeed("timberwatch", "minecraft:charcoal", NeedLevel.SURPLUS);

        // ── Crossroads (trade hub) ──
        // Moderate demand for valuables; surplus of common trade goods
        setNeed("crossroads", "minecraft:diamond", NeedLevel.MODERATE_NEED);
        setNeed("crossroads", "minecraft:emerald", NeedLevel.MODERATE_NEED);
        setNeed("crossroads", "minecraft:ender_pearl", NeedLevel.MODERATE_NEED);
        setNeed("crossroads", "minecraft:apple", NeedLevel.OVERSATURATED);
        setNeed("crossroads", "minecraft:arrow", NeedLevel.SURPLUS);
        setNeed("crossroads", "minecraft:glass", NeedLevel.SURPLUS);
        setNeed("crossroads", "minecraft:paper", NeedLevel.SURPLUS);

        // ── Basaltkeep (fortress city) ──
        // Desperate for rare combat materials; surplus of iron equipment
        setNeed("basaltkeep", "minecraft:diamond", NeedLevel.DESPERATE);
        setNeed("basaltkeep", "minecraft:netherite_scrap", NeedLevel.DESPERATE);
        setNeed("basaltkeep", "minecraft:obsidian", NeedLevel.MODERATE_NEED);
        setNeed("basaltkeep", "minecraft:leather", NeedLevel.MODERATE_NEED);
        setNeed("basaltkeep", "minecraft:iron_sword", NeedLevel.OVERSATURATED);
        setNeed("basaltkeep", "minecraft:iron_chestplate", NeedLevel.OVERSATURATED);
        setNeed("basaltkeep", "minecraft:chainmail_chestplate", NeedLevel.SURPLUS);
        setNeed("basaltkeep", "minecraft:arrow", NeedLevel.SURPLUS);
        setNeed("basaltkeep", "minecraft:shield", NeedLevel.SURPLUS);

        // ── Arcaneveil (enchanting city) ──
        // Desperate for rare reagents; surplus of potions and enchanted books
        setNeed("arcaneveil", "minecraft:blaze_rod", NeedLevel.DESPERATE);
        setNeed("arcaneveil", "minecraft:ghast_tear", NeedLevel.DESPERATE);
        setNeed("arcaneveil", "minecraft:echo_shard", NeedLevel.DESPERATE);
        setNeed("arcaneveil", "minecraft:lapis_lazuli", NeedLevel.MODERATE_NEED);
        setNeed("arcaneveil", "minecraft:ender_pearl", NeedLevel.MODERATE_NEED);
        setNeed("arcaneveil", "minecraft:nether_wart", NeedLevel.MODERATE_NEED);
        setNeed("arcaneveil", "minecraft:enchanted_book", NeedLevel.OVERSATURATED);
        setNeed("arcaneveil", "minecraft:glass_bottle", NeedLevel.OVERSATURATED);
        setNeed("arcaneveil", "minecraft:potion", NeedLevel.SURPLUS);
        setNeed("arcaneveil", "minecraft:redstone", NeedLevel.SURPLUS);

        // ── Goldspire (legendary capital) ──
        // Desperate for endgame treasures; nothing oversaturated (exclusive market)
        setNeed("goldspire", "minecraft:nether_star", NeedLevel.DESPERATE);
        setNeed("goldspire", "minecraft:dragon_egg", NeedLevel.DESPERATE);
        setNeed("goldspire", "minecraft:elytra", NeedLevel.DESPERATE);
        setNeed("goldspire", "minecraft:beacon", NeedLevel.DESPERATE);
        setNeed("goldspire", "minecraft:netherite_ingot", NeedLevel.HIGH_NEED);
        setNeed("goldspire", "minecraft:diamond_block", NeedLevel.HIGH_NEED);
        setNeed("goldspire", "minecraft:emerald_block", NeedLevel.MODERATE_NEED);
        setNeed("goldspire", "minecraft:totem_of_undying", NeedLevel.MODERATE_NEED);

        // ── Peenam (animal market) ──
        // Desperate for animal feed and equipment; surplus of animal products
        setNeed("peenam", "minecraft:wheat", NeedLevel.DESPERATE);
        setNeed("peenam", "minecraft:hay_block", NeedLevel.DESPERATE);
        setNeed("peenam", "minecraft:golden_carrot", NeedLevel.HIGH_NEED);
        setNeed("peenam", "minecraft:lead", NeedLevel.MODERATE_NEED);
        setNeed("peenam", "minecraft:saddle", NeedLevel.MODERATE_NEED);
        setNeed("peenam", "minecraft:name_tag", NeedLevel.MODERATE_NEED);
        setNeed("peenam", "minecraft:leather", NeedLevel.OVERSATURATED);
        setNeed("peenam", "minecraft:feather", NeedLevel.OVERSATURATED);
        setNeed("peenam", "minecraft:egg", NeedLevel.OVERSATURATED);
        setNeed("peenam", "minecraft:white_wool", NeedLevel.OVERSATURATED);
        setNeed("peenam", "minecraft:beef", NeedLevel.SURPLUS);
        setNeed("peenam", "minecraft:porkchop", NeedLevel.SURPLUS);
        setNeed("peenam", "minecraft:chicken", NeedLevel.SURPLUS);
    }

    private static void register(TownData town) {
        TOWNS.put(town.getId(), town);
    }

    /**
     * Set a specific NeedLevel override for an item in a town.
     * Use this to override the default binary HIGH_NEED/SURPLUS
     * with more granular levels like DESPERATE or MODERATE_NEED.
     */
    private static void setNeed(String townId, String itemId, NeedLevel level) {
        TownData town = TOWNS.get(townId);
        if (town != null) {
            town.setNeedLevel(itemId, level);
        }
    }

    private static Set<ResourceLocation> setOf(String... items) {
        Set<ResourceLocation> set = new HashSet<>();
        for (String item : items) {
            set.add(new ResourceLocation(item));
        }
        return set;
    }

    public static TownData getTown(String id) {
        // Check static towns first
        TownData town = TOWNS.get(id);
        if (town != null) return town;
        
        // Check dynamic towns from mods
        return ModCompatibility.getDynamicTowns().get(id);
    }

    public static Collection<TownData> getAllTowns() {
        Collection<TownData> cached = cachedAllTowns;
        if (cached == null) {
            // Combine static and dynamic towns
            List<TownData> allTowns = new ArrayList<>(TOWNS.values());
            allTowns.addAll(ModCompatibility.getDynamicTowns().values());
            cached = Collections.unmodifiableCollection(allTowns);
            cachedAllTowns = cached;
        }
        return cached;
    }

    /**
     * Get towns available for a given trader level.
     */
    public static List<TownData> getAvailableTowns(int traderLevel) {
        return cachedAvailableTowns.computeIfAbsent(traderLevel, level -> {
            List<TownData> available = new ArrayList<>();
            // Add static towns
            for (TownData town : TOWNS.values()) {
                if (town.getMinTraderLevel() <= level) {
                    available.add(town);
                }
            }
            // Add dynamic mod towns
            for (TownData town : ModCompatibility.getDynamicTowns().values()) {
                if (town.getMinTraderLevel() <= level) {
                    available.add(town);
                }
            }
            return Collections.unmodifiableList(available);
        });
    }

    /**
     * Get towns within a distance range.
     */
    public static List<TownData> getTownsInRange(int minDist, int maxDist, int traderLevel) {
        List<TownData> towns = new ArrayList<>();
        // Add static towns
        for (TownData town : TOWNS.values()) {
            if (town.getDistance() >= minDist && town.getDistance() <= maxDist
                    && town.getMinTraderLevel() <= traderLevel) {
                towns.add(town);
            }
        }
        // Add dynamic mod towns
        for (TownData town : ModCompatibility.getDynamicTowns().values()) {
            if (town.getDistance() >= minDist && town.getDistance() <= maxDist
                    && town.getMinTraderLevel() <= traderLevel) {
                towns.add(town);
            }
        }
        return towns;
    }
}
