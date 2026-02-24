package com.offtomarket.mod.data;

import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.tags.ItemTags;
import net.minecraftforge.common.ForgeHooks;
import net.minecraftforge.common.Tags;
import net.minecraftforge.registries.ForgeRegistries;

import java.util.IdentityHashMap;
import java.util.Map;

/**
 * Single source of truth for raw material prices (in Copper Pieces).
 * <p>
 * Every raw material, mob drop, farming output, and crafting intermediate
 * is defined here with a canonical CP value. {@link PriceCalculator} reads
 * from this class instead of maintaining its own price tables.
 * <p>
 * Resolution order for {@link #getValue(Item)}:
 * <ol>
 *   <li>Explicit override map (hardcoded vanilla prices)</li>
 *   <li>Fuel-time floor ({@code max(1, burnTime / 400)})</li>
 *   <li>Forge tag heuristic (ingots, gems, ores, nuggets, dusts)</li>
 *   <li>Default fallback: 1 CP</li>
 * </ol>
 */
public final class MaterialValues {

    private MaterialValues() {} // utility class

    // ===================== Explicit Override Map =====================

    private static final Map<Item, Integer> VALUES = new IdentityHashMap<>(256);

    static {
        // ─── Junk / ultra-common (1 CP) ───
        put(Items.DIRT,                1);
        put(Items.COBBLESTONE,         1);
        put(Items.STONE,               1);
        put(Items.GRAVEL,              1);
        put(Items.SAND,                1);
        put(Items.RED_SAND,            1);
        put(Items.NETHERRACK,          1);
        put(Items.COBBLED_DEEPSLATE,   1);
        put(Items.DEEPSLATE,           1);
        put(Items.ANDESITE,            1);
        put(Items.DIORITE,             1);
        put(Items.GRANITE,             1);
        put(Items.TUFF,                1);
        put(Items.CALCITE,             1);
        put(Items.CLAY_BALL,           1);
        put(Items.FLINT,               1);
        put(Items.SNOWBALL,            1);
        put(Items.ROTTEN_FLESH,        1);
        put(Items.STICK,               1);
        put(Items.FEATHER,             1);
        put(Items.PAPER,               1);
        put(Items.GLASS_BOTTLE,        1);
        put(Items.KELP,                1);
        put(Items.SUGAR_CANE,          1);
        put(Items.SUGAR,               1);
        put(Items.MELON_SLICE,         1);
        put(Items.BOWL,                1);

        // ─── Planks (1 CP each — logs give 4) ───
        put(Items.OAK_PLANKS,          1);
        put(Items.SPRUCE_PLANKS,       1);
        put(Items.BIRCH_PLANKS,        1);
        put(Items.DARK_OAK_PLANKS,     1);
        put(Items.JUNGLE_PLANKS,       1);
        put(Items.ACACIA_PLANKS,       1);
        put(Items.MANGROVE_PLANKS,     1);
        put(Items.CRIMSON_PLANKS,      1);
        put(Items.WARPED_PLANKS,       1);

        // ─── Logs (2 CP each) ───
        put(Items.OAK_LOG,             2);
        put(Items.SPRUCE_LOG,          2);
        put(Items.BIRCH_LOG,           2);
        put(Items.DARK_OAK_LOG,        2);
        put(Items.JUNGLE_LOG,          2);
        put(Items.ACACIA_LOG,          2);
        put(Items.MANGROVE_LOG,        2);
        put(Items.CRIMSON_STEM,        2);
        put(Items.WARPED_STEM,         2);

        // ─── Common drops & farming (2–3 CP) ───
        put(Items.WHEAT,               2);
        put(Items.POTATO,              2);
        put(Items.CARROT,              2);
        put(Items.BEETROOT,            2);
        put(Items.APPLE,               3);
        put(Items.SWEET_BERRIES,       2);
        put(Items.PUMPKIN,             3);
        put(Items.MELON,               3);
        put(Items.EGG,                 2);
        put(Items.GLASS,               2);
        put(Items.INK_SAC,             3);
        put(Items.BONE_MEAL,           1);

        // ─── Raw meat (3 CP) ───
        put(Items.BEEF,                3);
        put(Items.PORKCHOP,            3);
        put(Items.CHICKEN,             3);
        put(Items.MUTTON,              3);
        put(Items.COD,                 3);
        put(Items.SALMON,              3);
        put(Items.RABBIT,              3);
        put(Items.TROPICAL_FISH,       3);
        put(Items.PUFFERFISH,          8);

        // ─── Cooked meat (raw value + 50% cooking bonus) ───
        put(Items.COOKED_BEEF,         5);
        put(Items.COOKED_PORKCHOP,     5);
        put(Items.COOKED_CHICKEN,      5);
        put(Items.COOKED_MUTTON,       5);
        put(Items.COOKED_COD,          5);
        put(Items.COOKED_SALMON,       5);
        put(Items.COOKED_RABBIT,       5);
        put(Items.BAKED_POTATO,        3);
        put(Items.DRIED_KELP,          2);

        // ─── Prepared food (ingredient-based) ───
        put(Items.BREAD,               6);   // 3 wheat
        put(Items.COOKIE,              3);   // wheat + cocoa
        put(Items.CAKE,                18);  // 3 wheat + 3 milk + 2 sugar + 1 egg
        put(Items.PUMPKIN_PIE,         8);   // pumpkin + sugar + egg
        put(Items.MUSHROOM_STEW,       6);   // 2 mushroom + bowl
        put(Items.RABBIT_STEW,         12);  // rabbit + carrot + potato + mushroom + bowl
        put(Items.GOLDEN_CARROT,       30);  // carrot + 8 gold nuggets
        put(Items.GOLDEN_APPLE,        120); // apple + 8 gold ingots
        put(Items.HONEY_BOTTLE,        10);

        // ─── Mob drops (common) ───
        put(Items.BONE,                3);
        put(Items.STRING,              3);
        put(Items.GUNPOWDER,           5);
        put(Items.SPIDER_EYE,          3);
        put(Items.SLIME_BALL,          8);
        put(Items.LEATHER,             5);
        put(Items.RABBIT_HIDE,         2);
        put(Items.RABBIT_FOOT,         25);
        put(Items.HONEYCOMB,           8);

        // ─── Mob drops (rare / nether) ───
        put(Items.BLAZE_ROD,           40);
        put(Items.BLAZE_POWDER,        25);  // half a rod + brewing utility
        put(Items.GHAST_TEAR,          50);
        put(Items.MAGMA_CREAM,         15);
        put(Items.ENDER_PEARL,         40);
        put(Items.PHANTOM_MEMBRANE,    20);
        put(Items.SHULKER_SHELL,       100);
        put(Items.WITHER_SKELETON_SKULL, 200);
        put(Items.DRAGON_BREATH,       100);
        put(Items.SCUTE,               15);

        // ─── Fuels & common ores ───
        put(Items.COAL,                5);
        put(Items.CHARCOAL,            5);
        put(Items.RAW_COPPER,          5);
        put(Items.RAW_IRON,            10);
        put(Items.RAW_GOLD,            30);

        // ─── Ingots ───
        put(Items.COPPER_INGOT,        8);
        put(Items.IRON_INGOT,          15);
        put(Items.GOLD_INGOT,          50);
        put(Items.IRON_NUGGET,         2);   // 1/9 of ingot
        put(Items.GOLD_NUGGET,         6);   // 1/9 of ingot

        // ─── Gems & precious ───
        put(Items.DIAMOND,             100);
        put(Items.EMERALD,             80);
        put(Items.AMETHYST_SHARD,      12);
        put(Items.NETHERITE_SCRAP,     250);
        put(Items.NETHERITE_INGOT,     800); // 4 scrap + 4 gold ingots

        // ─── Redstone & crafting ───
        put(Items.REDSTONE,            8);
        put(Items.LAPIS_LAZULI,        12);
        put(Items.QUARTZ,              10);
        put(Items.GLOWSTONE_DUST,      8);
        put(Items.PRISMARINE_SHARD,    8);
        put(Items.PRISMARINE_CRYSTALS, 10);
        put(Items.NAUTILUS_SHELL,      60);
        put(Items.HEART_OF_THE_SEA,    400);
        put(Items.NETHER_WART,         8);
        put(Items.FERMENTED_SPIDER_EYE, 10);

        // ─── Books & paper ───
        put(Items.BOOK,                4);  // 3 paper + 1 leather
        put(Items.WRITABLE_BOOK,       5);  // book + ink + feather

        // ─── Decorative ───
        put(Items.GLOW_INK_SAC,        8);
        put(Items.CANDLE,              3);
        put(Items.TORCH,               2);  // stick + coal

        // ─── Blocks of material (9× ingot) ───
        put(Items.IRON_BLOCK,          135);
        put(Items.GOLD_BLOCK,          450);
        put(Items.DIAMOND_BLOCK,       900);
        put(Items.EMERALD_BLOCK,       720);
        put(Items.NETHERITE_BLOCK,     7200);
        put(Items.COPPER_BLOCK,        72);
        put(Items.LAPIS_BLOCK,         108);
        put(Items.REDSTONE_BLOCK,      72);
        put(Items.COAL_BLOCK,          45);

        // ─── Special / treasure items (not craftable — purely value-set) ───
        put(Items.BEACON,              2500);
        put(Items.NETHER_STAR,         1500);
        put(Items.ELYTRA,              3000);
        put(Items.TOTEM_OF_UNDYING,    1000);
        put(Items.EXPERIENCE_BOTTLE,   50);
        put(Items.SADDLE,              60);
        put(Items.NAME_TAG,            50);
        put(Items.TRIDENT,             400);
        put(Items.CONDUIT,             600);
        put(Items.END_CRYSTAL,         200);
        put(Items.DRAGON_EGG,          5000);
        put(Items.LODESTONE,           300);
        put(Items.RESPAWN_ANCHOR,      200);
        put(Items.ENCHANTED_GOLDEN_APPLE, 2500);

        // ─── Music discs ───
        put(Items.MUSIC_DISC_13,       60);
        put(Items.MUSIC_DISC_CAT,      60);
        put(Items.MUSIC_DISC_BLOCKS,   60);
        put(Items.MUSIC_DISC_CHIRP,    60);
        put(Items.MUSIC_DISC_FAR,      60);
        put(Items.MUSIC_DISC_MALL,     60);
        put(Items.MUSIC_DISC_MELLOHI,  60);
        put(Items.MUSIC_DISC_STAL,     60);
        put(Items.MUSIC_DISC_STRAD,    60);
        put(Items.MUSIC_DISC_WARD,     60);
        put(Items.MUSIC_DISC_11,       60);
        put(Items.MUSIC_DISC_WAIT,     60);
        put(Items.MUSIC_DISC_OTHERSIDE, 100);
        put(Items.MUSIC_DISC_PIGSTEP, 150);

        // ─── Dyes (3 CP each) ───
        put(Items.WHITE_DYE,           3);
        put(Items.RED_DYE,             3);
        put(Items.BLUE_DYE,            3);
        put(Items.GREEN_DYE,           3);
        put(Items.BLACK_DYE,           3);
        put(Items.YELLOW_DYE,          3);
        put(Items.BROWN_DYE,           3);
        put(Items.ORANGE_DYE,          3);
        put(Items.PINK_DYE,            3);
        put(Items.PURPLE_DYE,          3);
        put(Items.CYAN_DYE,            3);
        put(Items.LIGHT_BLUE_DYE,      3);
        put(Items.LIGHT_GRAY_DYE,      3);
        put(Items.LIME_DYE,            3);
        put(Items.MAGENTA_DYE,         3);
        put(Items.COCOA_BEANS,         3);

        // ─── Misc brewing ───
        put(Items.BREWING_STAND,       30);
        put(Items.POTION,              2);  // uncraftable water bottle base
    }

    private static void put(Item item, int copperPieces) {
        VALUES.put(item, copperPieces);
    }

    // ===================== Public API =====================

    /**
     * Get the base value of a raw material in Copper Pieces.
     * <p>
     * Resolution order:
     * <ol>
     *   <li>Explicit override map</li>
     *   <li>Fuel-time floor (burnTime / 400, min 1)</li>
     *   <li>Forge tag heuristic</li>
     *   <li>Default: 1 CP</li>
     * </ol>
     */
    public static int getValue(Item item) {
        // 1. Explicit override
        Integer override = VALUES.get(item);
        if (override != null) return override;

        ItemStack stack = new ItemStack(item);

        // 2. Fuel-time floor
        int burnTime = ForgeHooks.getBurnTime(stack, null);
        if (burnTime > 0) {
            return Math.max(1, burnTime / 400);
        }

        // 3. Tag-based heuristic
        int tagValue = getTagValue(stack);
        if (tagValue > 0) return tagValue;

        // 4. Default
        return 1;
    }

    /**
     * Convenience: get the total cost of {@code count} units of an item.
     */
    public static int getIngredientCost(Item item, int count) {
        return getValue(item) * count;
    }

    /**
     * Check whether this item has an explicit value defined.
     */
    public static boolean hasExplicitValue(Item item) {
        return VALUES.containsKey(item);
    }

    // ===================== Tag Heuristic =====================

    /**
     * Estimates a value based on Forge item tags.
     * Returns 0 if no tag matches.
     */
    private static int getTagValue(ItemStack stack) {
        if (stack.is(Tags.Items.INGOTS))         return 15;  // iron ingot baseline
        if (stack.is(Tags.Items.GEMS))           return 60;  // diamond-class
        if (stack.is(Tags.Items.ORES))           return 12;  // raw ore
        if (stack.is(Tags.Items.RAW_MATERIALS))  return 10;  // raw materials
        if (stack.is(Tags.Items.NUGGETS))        return 3;   // nugget
        if (stack.is(Tags.Items.DUSTS))          return 8;   // redstone-class
        if (stack.is(Tags.Items.STORAGE_BLOCKS)) return 100; // block of material
        if (stack.is(ItemTags.LOGS))             return 2;
        if (stack.is(ItemTags.PLANKS))           return 1;
        if (stack.is(ItemTags.WOOL))             return 3;
        if (stack.is(ItemTags.FISHES))           return 3;
        if (stack.is(ItemTags.FLOWERS))          return 2;
        if (stack.is(ItemTags.SAPLINGS))         return 2;
        if (stack.is(ItemTags.SAND))             return 1;
        if (stack.is(ItemTags.COALS))            return 5;
        if (stack.is(ItemTags.MUSIC_DISCS))      return 60;
        return 0;
    }
}
