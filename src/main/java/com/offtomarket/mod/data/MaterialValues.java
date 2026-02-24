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
        // ─── Junk / ultra-common (2 CP) ───
        put(Items.DIRT,                2);
        put(Items.COBBLESTONE,         2);
        put(Items.STONE,               2);
        put(Items.GRAVEL,              2);
        put(Items.SAND,                2);
        put(Items.RED_SAND,            2);
        put(Items.NETHERRACK,          2);
        put(Items.COBBLED_DEEPSLATE,   2);
        put(Items.DEEPSLATE,           2);
        put(Items.ANDESITE,            2);
        put(Items.DIORITE,             2);
        put(Items.GRANITE,             2);
        put(Items.TUFF,                2);
        put(Items.CALCITE,             2);
        put(Items.CLAY_BALL,           2);
        put(Items.FLINT,               2);
        put(Items.SNOWBALL,            2);
        put(Items.ROTTEN_FLESH,        2);
        put(Items.STICK,               2);
        put(Items.FEATHER,             2);
        put(Items.PAPER,               2);
        put(Items.GLASS_BOTTLE,        2);
        put(Items.KELP,                2);
        put(Items.SUGAR_CANE,          2);
        put(Items.SUGAR,               2);
        put(Items.MELON_SLICE,         2);
        put(Items.BOWL,                2);

        // ─── Planks (2 CP each — logs give 4) ───
        put(Items.OAK_PLANKS,          2);
        put(Items.SPRUCE_PLANKS,       2);
        put(Items.BIRCH_PLANKS,        2);
        put(Items.DARK_OAK_PLANKS,     2);
        put(Items.JUNGLE_PLANKS,       2);
        put(Items.ACACIA_PLANKS,       2);
        put(Items.MANGROVE_PLANKS,     2);
        put(Items.CRIMSON_PLANKS,      2);
        put(Items.WARPED_PLANKS,       2);

        // ─── Logs (5 CP each) ───
        put(Items.OAK_LOG,             5);
        put(Items.SPRUCE_LOG,          5);
        put(Items.BIRCH_LOG,           5);
        put(Items.DARK_OAK_LOG,        5);
        put(Items.JUNGLE_LOG,          5);
        put(Items.ACACIA_LOG,          5);
        put(Items.MANGROVE_LOG,        5);
        put(Items.CRIMSON_STEM,        5);
        put(Items.WARPED_STEM,         5);

        // ─── Common drops & farming (5–8 CP) ───
        put(Items.WHEAT,               5);
        put(Items.POTATO,              5);
        put(Items.CARROT,              5);
        put(Items.BEETROOT,            5);
        put(Items.APPLE,               8);
        put(Items.SWEET_BERRIES,       5);
        put(Items.PUMPKIN,             8);
        put(Items.MELON,               8);
        put(Items.EGG,                 5);
        put(Items.GLASS,               5);
        put(Items.INK_SAC,             8);
        put(Items.BONE_MEAL,           2);

        // ─── Raw meat (8 CP) ───
        put(Items.BEEF,                8);
        put(Items.PORKCHOP,            8);
        put(Items.CHICKEN,             8);
        put(Items.MUTTON,              8);
        put(Items.COD,                 8);
        put(Items.SALMON,              8);
        put(Items.RABBIT,              8);
        put(Items.TROPICAL_FISH,       8);
        put(Items.PUFFERFISH,          20);

        // ─── Cooked meat (raw value + 50% cooking bonus) ───
        put(Items.COOKED_BEEF,         13);
        put(Items.COOKED_PORKCHOP,     13);
        put(Items.COOKED_CHICKEN,      13);
        put(Items.COOKED_MUTTON,       13);
        put(Items.COOKED_COD,          13);
        put(Items.COOKED_SALMON,       13);
        put(Items.COOKED_RABBIT,       13);
        put(Items.BAKED_POTATO,        8);
        put(Items.DRIED_KELP,          5);

        // ─── Prepared food (ingredient-based) ───
        put(Items.BREAD,               16);  // 3 wheat
        put(Items.COOKIE,              8);   // wheat + cocoa
        put(Items.CAKE,                48);  // 3 wheat + 3 milk + 2 sugar + 1 egg
        put(Items.PUMPKIN_PIE,         20);  // pumpkin + sugar + egg
        put(Items.MUSHROOM_STEW,       16);  // 2 mushroom + bowl
        put(Items.RABBIT_STEW,         32);  // rabbit + carrot + potato + mushroom + bowl
        put(Items.GOLDEN_CARROT,       80);  // carrot + 8 gold nuggets
        put(Items.GOLDEN_APPLE,        320); // apple + 8 gold ingots
        put(Items.HONEY_BOTTLE,        26);

        // ─── Mob drops (common) ───
        put(Items.BONE,                8);
        put(Items.STRING,              8);
        put(Items.GUNPOWDER,           13);
        put(Items.SPIDER_EYE,          8);
        put(Items.SLIME_BALL,          20);
        put(Items.LEATHER,             13);
        put(Items.RABBIT_HIDE,         5);
        put(Items.RABBIT_FOOT,         65);
        put(Items.HONEYCOMB,           20);

        // ─── Mob drops (rare / nether) ───
        put(Items.BLAZE_ROD,           105);
        put(Items.BLAZE_POWDER,        65);  // half a rod + brewing utility
        put(Items.GHAST_TEAR,          130);
        put(Items.MAGMA_CREAM,         40);
        put(Items.ENDER_PEARL,         105);
        put(Items.PHANTOM_MEMBRANE,    52);
        put(Items.SHULKER_SHELL,       260);
        put(Items.WITHER_SKELETON_SKULL, 520);
        put(Items.DRAGON_BREATH,       260);
        put(Items.SCUTE,               40);

        // ─── Fuels & common ores ───
        put(Items.COAL,                13);
        put(Items.CHARCOAL,            13);
        put(Items.RAW_COPPER,          13);
        put(Items.RAW_IRON,            26);
        put(Items.RAW_GOLD,            80);

        // ─── Ingots ───
        put(Items.COPPER_INGOT,        20);
        put(Items.IRON_INGOT,          40);
        put(Items.GOLD_INGOT,          130);
        put(Items.IRON_NUGGET,         5);   // 1/9 of ingot
        put(Items.GOLD_NUGGET,         16);  // 1/9 of ingot

        // ─── Gems & precious ───
        put(Items.DIAMOND,             260);
        put(Items.EMERALD,             210);
        put(Items.AMETHYST_SHARD,      32);
        put(Items.NETHERITE_SCRAP,     650);
        put(Items.NETHERITE_INGOT,     2100); // 4 scrap + 4 gold ingots

        // ─── Redstone & crafting ───
        put(Items.REDSTONE,            20);
        put(Items.LAPIS_LAZULI,        32);
        put(Items.QUARTZ,              26);
        put(Items.GLOWSTONE_DUST,      20);
        put(Items.PRISMARINE_SHARD,    20);
        put(Items.PRISMARINE_CRYSTALS, 26);
        put(Items.NAUTILUS_SHELL,      160);
        put(Items.HEART_OF_THE_SEA,    1050);
        put(Items.NETHER_WART,         20);
        put(Items.FERMENTED_SPIDER_EYE, 26);

        // ─── Books & paper ───
        put(Items.BOOK,                10);  // 3 paper + 1 leather
        put(Items.WRITABLE_BOOK,       13);  // book + ink + feather

        // ─── Decorative ───
        put(Items.GLOW_INK_SAC,        20);
        put(Items.CANDLE,              8);
        put(Items.TORCH,               5);  // stick + coal

        // ─── Blocks of material (9× ingot) ───
        put(Items.IRON_BLOCK,          360);
        put(Items.GOLD_BLOCK,          1170);
        put(Items.DIAMOND_BLOCK,       2340);
        put(Items.EMERALD_BLOCK,       1890);
        put(Items.NETHERITE_BLOCK,     18900);
        put(Items.COPPER_BLOCK,        180);
        put(Items.LAPIS_BLOCK,         288);
        put(Items.REDSTONE_BLOCK,      180);
        put(Items.COAL_BLOCK,          120);

        // ─── Special / treasure items (not craftable — purely value-set) ───
        put(Items.BEACON,              6500);
        put(Items.NETHER_STAR,         3900);
        put(Items.ELYTRA,              7800);
        put(Items.TOTEM_OF_UNDYING,    2600);
        put(Items.EXPERIENCE_BOTTLE,   130);
        put(Items.SADDLE,              160);
        put(Items.NAME_TAG,            130);
        put(Items.TRIDENT,             1050);
        put(Items.CONDUIT,             1560);
        put(Items.END_CRYSTAL,         520);
        put(Items.DRAGON_EGG,          13000);
        put(Items.LODESTONE,           780);
        put(Items.RESPAWN_ANCHOR,      520);
        put(Items.ENCHANTED_GOLDEN_APPLE, 6500);

        // ─── Music discs ───
        put(Items.MUSIC_DISC_13,       160);
        put(Items.MUSIC_DISC_CAT,      160);
        put(Items.MUSIC_DISC_BLOCKS,   160);
        put(Items.MUSIC_DISC_CHIRP,    160);
        put(Items.MUSIC_DISC_FAR,      160);
        put(Items.MUSIC_DISC_MALL,     160);
        put(Items.MUSIC_DISC_MELLOHI,  160);
        put(Items.MUSIC_DISC_STAL,     160);
        put(Items.MUSIC_DISC_STRAD,    160);
        put(Items.MUSIC_DISC_WARD,     160);
        put(Items.MUSIC_DISC_11,       160);
        put(Items.MUSIC_DISC_WAIT,     160);
        put(Items.MUSIC_DISC_OTHERSIDE, 260);
        put(Items.MUSIC_DISC_PIGSTEP, 400);

        // ─── Dyes (8 CP each) ───
        put(Items.WHITE_DYE,           8);
        put(Items.RED_DYE,             8);
        put(Items.BLUE_DYE,            8);
        put(Items.GREEN_DYE,           8);
        put(Items.BLACK_DYE,           8);
        put(Items.YELLOW_DYE,          8);
        put(Items.BROWN_DYE,           8);
        put(Items.ORANGE_DYE,          8);
        put(Items.PINK_DYE,            8);
        put(Items.PURPLE_DYE,          8);
        put(Items.CYAN_DYE,            8);
        put(Items.LIGHT_BLUE_DYE,      8);
        put(Items.LIGHT_GRAY_DYE,      8);
        put(Items.LIME_DYE,            8);
        put(Items.MAGENTA_DYE,         8);
        put(Items.COCOA_BEANS,         8);

        // ─── Misc brewing ───
        put(Items.BREWING_STAND,       80);
        put(Items.POTION,              5);  // uncraftable water bottle base
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
        if (stack.is(Tags.Items.INGOTS))         return 40;  // iron ingot baseline
        if (stack.is(Tags.Items.GEMS))           return 160; // diamond-class
        if (stack.is(Tags.Items.ORES))           return 32;  // raw ore
        if (stack.is(Tags.Items.RAW_MATERIALS))  return 26;  // raw materials
        if (stack.is(Tags.Items.NUGGETS))        return 8;   // nugget
        if (stack.is(Tags.Items.DUSTS))          return 20;  // redstone-class
        if (stack.is(Tags.Items.STORAGE_BLOCKS)) return 260; // block of material
        if (stack.is(ItemTags.LOGS))             return 5;
        if (stack.is(ItemTags.PLANKS))           return 2;
        if (stack.is(ItemTags.WOOL))             return 8;
        if (stack.is(ItemTags.FISHES))           return 8;
        if (stack.is(ItemTags.FLOWERS))          return 5;
        if (stack.is(ItemTags.SAPLINGS))         return 5;
        if (stack.is(ItemTags.SAND))             return 2;
        if (stack.is(ItemTags.COALS))            return 13;
        if (stack.is(ItemTags.MUSIC_DISCS))      return 160;
        return 0;
    }
}
