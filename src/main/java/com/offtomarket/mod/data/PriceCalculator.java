package com.offtomarket.mod.data;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.ItemTags;
import net.minecraft.tags.TagKey;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.*;
import net.minecraft.world.item.alchemy.Potion;
import net.minecraft.world.item.alchemy.PotionUtils;
import net.minecraft.world.item.alchemy.Potions;
import net.minecraftforge.common.Tags;
import net.minecraftforge.registries.ForgeRegistries;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.function.Predicate;

/**
 * Comprehensive item pricing and valuation system.
 * <p>
 * Every item is classified into a {@link ValueTier} that determines:
 * <ul>
 *   <li><b>basePrice</b> – The "fair value" in copper pieces (CP).</li>
 *   <li><b>maxPrice</b>  – The absolute price ceiling. If the player sets a price
 *       above this, the item <em>will not sell at all</em>.</li>
 * </ul>
 * The system resolves prices in this order:
 * <ol>
 *   <li>Ingredient-based pricing for tools &amp; armor (recipe cost × crafting premium;
 *       auto-handles modded items via Tier / ArmorMaterial repair ingredients).</li>
 *   <li>Exact item overrides (hardcoded vanilla items).</li>
 *   <li>Tag-based category matching (works with modded items).</li>
 *   <li>Item-class heuristics (instanceof checks).</li>
 *   <li>Registry-path keyword heuristics (catches remaining modded items).</li>
 *   <li>Rarity-based fallback.</li>
 * </ol>
 */
public class PriceCalculator {

    // ===================== Value Tiers =====================

    /**
     * Defines a price band for a category of items.
     * @param basePrice  Fair market value in copper pieces.
     * @param maxPrice   Absolute ceiling – items priced above this WILL NOT sell.
     */
    public record ValueTier(int basePrice, int maxPrice) {
        /** Convenience: maxPrice defaults to 3× basePrice. */
        public ValueTier(int basePrice) { this(basePrice, basePrice * 3); }
    }

    // Pre-defined tiers (base, max)
    public static final ValueTier TIER_JUNK       = new ValueTier(1,   3);     // dirt, cobble, sticks
    public static final ValueTier TIER_BASIC       = new ValueTier(2,   8);     // planks, logs, sand
    public static final ValueTier TIER_COMMON      = new ValueTier(3,   12);    // common food, wool
    public static final ValueTier TIER_USEFUL      = new ValueTier(5,   20);    // coal, raw ores, bread
    public static final ValueTier TIER_CRAFT_MAT   = new ValueTier(8,   30);    // copper, redstone, glowstone
    public static final ValueTier TIER_FOOD_GOOD   = new ValueTier(6,   25);    // cooked meats, cake
    public static final ValueTier TIER_TOOL_STONE  = new ValueTier(8,   30);    // stone tools
    public static final ValueTier TIER_TOOL_IRON   = new ValueTier(25,  80);    // iron tools/armor
    public static final ValueTier TIER_TOOL_GOLD   = new ValueTier(35,  120);   // gold tools/armor
    public static final ValueTier TIER_TOOL_DIAMOND= new ValueTier(80,  300);   // diamond tools/armor
    public static final ValueTier TIER_TOOL_NETHERITE = new ValueTier(500, 1800);// netherite gear
    public static final ValueTier TIER_INGOT_IRON  = new ValueTier(15,  50);    // iron ingot
    public static final ValueTier TIER_INGOT_GOLD  = new ValueTier(50,  160);   // gold ingot
    public static final ValueTier TIER_GEM         = new ValueTier(100, 350);   // diamond, emerald
    public static final ValueTier TIER_PRECIOUS    = new ValueTier(250, 800);   // netherite scrap
    public static final ValueTier TIER_LEGENDARY   = new ValueTier(800, 2500);  // netherite ingot
    public static final ValueTier TIER_TREASURE    = new ValueTier(1500, 5000); // nether star, elytra
    public static final ValueTier TIER_PRICELESS   = new ValueTier(2500, 8000); // beacon, enchanted gapple
    public static final ValueTier TIER_ENCHANTED   = new ValueTier(200, 700);   // enchanted books
    public static final ValueTier TIER_POTION      = new ValueTier(20,  70);    // potions
    public static final ValueTier TIER_RARE_DROP   = new ValueTier(40,  140);   // blaze rod, ender pearl
    public static final ValueTier TIER_MUSIC       = new ValueTier(60,  200);   // music discs
    public static final ValueTier TIER_DECORATION  = new ValueTier(3,   15);    // decorative blocks
    public static final ValueTier TIER_MOB_DROP    = new ValueTier(5,   20);    // bones, string, gunpowder
    public static final ValueTier TIER_SPAWN_EGG   = new ValueTier(150, 500);   // spawn eggs

    // ===================== Exact Item Overrides =====================

    private static final Map<Item, ValueTier> ITEM_OVERRIDES = new LinkedHashMap<>();
    static {
        // ---- Junk / ultra-common ----
        put(Items.DIRT,               TIER_JUNK);
        put(Items.COBBLESTONE,        TIER_JUNK);
        put(Items.STONE,              TIER_JUNK);
        put(Items.GRAVEL,             TIER_JUNK);
        put(Items.SAND,               TIER_JUNK);
        put(Items.RED_SAND,           TIER_JUNK);
        put(Items.NETHERRACK,         TIER_JUNK);
        put(Items.COBBLED_DEEPSLATE,  TIER_JUNK);
        put(Items.DEEPSLATE,          TIER_JUNK);
        put(Items.ANDESITE,           TIER_JUNK);
        put(Items.DIORITE,            TIER_JUNK);
        put(Items.GRANITE,            TIER_JUNK);
        put(Items.TUFF,               TIER_JUNK);
        put(Items.CALCITE,            TIER_JUNK);
        put(Items.CLAY_BALL,          TIER_JUNK);
        put(Items.FLINT,              TIER_JUNK);
        put(Items.SNOWBALL,           TIER_JUNK);
        put(Items.ROTTEN_FLESH,       TIER_JUNK);
        put(Items.STICK,              TIER_JUNK);

        // ---- Basic resources ----
        put(Items.OAK_LOG,            TIER_BASIC);
        put(Items.SPRUCE_LOG,         TIER_BASIC);
        put(Items.BIRCH_LOG,          TIER_BASIC);
        put(Items.DARK_OAK_LOG,       TIER_BASIC);
        put(Items.JUNGLE_LOG,         TIER_BASIC);
        put(Items.ACACIA_LOG,         TIER_BASIC);
        put(Items.MANGROVE_LOG,       TIER_BASIC);
        put(Items.CRIMSON_STEM,       TIER_BASIC);
        put(Items.WARPED_STEM,        TIER_BASIC);
        put(Items.OAK_PLANKS,         TIER_JUNK);
        put(Items.SPRUCE_PLANKS,      TIER_JUNK);
        put(Items.BIRCH_PLANKS,       TIER_JUNK);
        put(Items.DARK_OAK_PLANKS,    TIER_JUNK);
        put(Items.JUNGLE_PLANKS,      TIER_JUNK);
        put(Items.ACACIA_PLANKS,      TIER_JUNK);
        put(Items.MANGROVE_PLANKS,    TIER_JUNK);
        put(Items.GLASS,              TIER_BASIC);
        put(Items.LEATHER,            TIER_USEFUL);
        put(Items.PAPER,              TIER_JUNK);
        put(Items.BOOK,               TIER_COMMON);
        put(Items.FEATHER,            TIER_JUNK);
        put(Items.INK_SAC,            TIER_COMMON);
        put(Items.GLOW_INK_SAC,       TIER_CRAFT_MAT);

        // ---- Common food ----
        put(Items.APPLE,              TIER_COMMON);
        put(Items.SWEET_BERRIES,      TIER_COMMON);
        put(Items.WHEAT,              TIER_COMMON);
        put(Items.POTATO,             TIER_COMMON);
        put(Items.CARROT,             TIER_COMMON);
        put(Items.BEETROOT,           TIER_COMMON);
        put(Items.MELON_SLICE,        TIER_JUNK);
        put(Items.PUMPKIN,            TIER_COMMON);
        put(Items.SUGAR_CANE,         TIER_BASIC);
        put(Items.KELP,               TIER_JUNK);
        put(Items.EGG,                TIER_COMMON);
        put(Items.BEEF,               TIER_COMMON);
        put(Items.PORKCHOP,           TIER_COMMON);
        put(Items.CHICKEN,            TIER_COMMON);
        put(Items.MUTTON,             TIER_COMMON);
        put(Items.COD,                TIER_COMMON);
        put(Items.SALMON,             TIER_COMMON);
        put(Items.RABBIT,             TIER_COMMON);

        // ---- Better food ----
        put(Items.BREAD,              TIER_USEFUL);
        put(Items.COOKED_BEEF,        TIER_FOOD_GOOD);
        put(Items.COOKED_PORKCHOP,    TIER_FOOD_GOOD);
        put(Items.COOKED_CHICKEN,     TIER_USEFUL);
        put(Items.COOKED_MUTTON,      TIER_FOOD_GOOD);
        put(Items.COOKED_COD,         TIER_USEFUL);
        put(Items.COOKED_SALMON,      TIER_FOOD_GOOD);
        put(Items.COOKED_RABBIT,      TIER_FOOD_GOOD);
        put(Items.BAKED_POTATO,       TIER_USEFUL);
        put(Items.PUMPKIN_PIE,        TIER_FOOD_GOOD);
        put(Items.CAKE,               TIER_FOOD_GOOD);
        put(Items.COOKIE,             TIER_COMMON);
        put(Items.MUSHROOM_STEW,      TIER_FOOD_GOOD);
        put(Items.RABBIT_STEW,        new ValueTier(10, 35));
        put(Items.GOLDEN_CARROT,      new ValueTier(30, 100));
        put(Items.GOLDEN_APPLE,       new ValueTier(120, 400));
        put(Items.ENCHANTED_GOLDEN_APPLE, TIER_PRICELESS);

        // ---- Fuel & common ores ----
        put(Items.COAL,               TIER_USEFUL);
        put(Items.CHARCOAL,           TIER_USEFUL);
        put(Items.RAW_COPPER,         TIER_USEFUL);
        put(Items.RAW_IRON,           new ValueTier(10, 35));
        put(Items.RAW_GOLD,           new ValueTier(30, 100));
        put(Items.COPPER_INGOT,       TIER_CRAFT_MAT);
        put(Items.IRON_INGOT,         TIER_INGOT_IRON);
        put(Items.GOLD_INGOT,         TIER_INGOT_GOLD);
        put(Items.IRON_NUGGET,        new ValueTier(2, 8));
        put(Items.GOLD_NUGGET,        new ValueTier(6, 20));
        put(Items.AMETHYST_SHARD,     new ValueTier(12, 40));

        // ---- Redstone & crafting ----
        put(Items.REDSTONE,           TIER_CRAFT_MAT);
        put(Items.LAPIS_LAZULI,       new ValueTier(12, 40));
        put(Items.QUARTZ,             new ValueTier(10, 35));
        put(Items.GLOWSTONE_DUST,     TIER_CRAFT_MAT);
        put(Items.GLOWSTONE,          new ValueTier(30, 100));
        put(Items.PRISMARINE_SHARD,   TIER_CRAFT_MAT);
        put(Items.PRISMARINE_CRYSTALS,new ValueTier(10, 35));
        put(Items.PHANTOM_MEMBRANE,   new ValueTier(20, 70));
        put(Items.NAUTILUS_SHELL,     new ValueTier(60, 200));
        put(Items.HEART_OF_THE_SEA,   new ValueTier(400, 1200));
        put(Items.SLIME_BALL,         TIER_CRAFT_MAT);

        // ---- Gems & precious ----
        put(Items.DIAMOND,            TIER_GEM);
        put(Items.EMERALD,            new ValueTier(80, 280));
        put(Items.NETHERITE_SCRAP,    TIER_PRECIOUS);
        put(Items.NETHERITE_INGOT,    TIER_LEGENDARY);

        // ---- Blocks of material ----
        put(Items.IRON_BLOCK,         new ValueTier(135, 450));
        put(Items.GOLD_BLOCK,         new ValueTier(450, 1400));
        put(Items.DIAMOND_BLOCK,      new ValueTier(900, 3000));
        put(Items.EMERALD_BLOCK,      new ValueTier(720, 2400));
        put(Items.NETHERITE_BLOCK,    new ValueTier(7200, 22000));
        put(Items.COPPER_BLOCK,       new ValueTier(72, 220));
        put(Items.LAPIS_BLOCK,        new ValueTier(108, 350));
        put(Items.REDSTONE_BLOCK,     new ValueTier(72, 220));
        put(Items.COAL_BLOCK,         new ValueTier(36, 120));

        // ---- Mob drops ----
        put(Items.BONE,               TIER_MOB_DROP);
        put(Items.STRING,             TIER_MOB_DROP);
        put(Items.GUNPOWDER,          TIER_MOB_DROP);
        put(Items.SPIDER_EYE,         TIER_MOB_DROP);
        put(Items.ENDER_PEARL,        TIER_RARE_DROP);
        put(Items.BLAZE_ROD,          TIER_RARE_DROP);
        put(Items.BLAZE_POWDER,       new ValueTier(25, 80));
        put(Items.GHAST_TEAR,         new ValueTier(50, 160));
        put(Items.MAGMA_CREAM,        new ValueTier(15, 50));
        put(Items.RABBIT_FOOT,        new ValueTier(25, 80));
        put(Items.SHULKER_SHELL,      new ValueTier(100, 350));
        put(Items.WITHER_SKELETON_SKULL, new ValueTier(200, 650));
        put(Items.DRAGON_BREATH,      new ValueTier(100, 350));
        put(Items.NETHER_WART,        new ValueTier(8, 30));
        put(Items.HONEYCOMB,          TIER_CRAFT_MAT);
        put(Items.HONEY_BOTTLE,       new ValueTier(10, 35));

        // ---- Special / treasure items ----
        put(Items.BEACON,             TIER_PRICELESS);
        put(Items.NETHER_STAR,        TIER_TREASURE);
        put(Items.ELYTRA,             new ValueTier(3000, 9000));
        put(Items.TOTEM_OF_UNDYING,   new ValueTier(1000, 3500));
        put(Items.ENCHANTED_BOOK,     TIER_ENCHANTED);
        put(Items.EXPERIENCE_BOTTLE,  new ValueTier(50, 160));
        put(Items.SADDLE,             new ValueTier(60, 200));
        put(Items.NAME_TAG,           new ValueTier(50, 160));
        put(Items.TRIDENT,            new ValueTier(400, 1400));
        put(Items.CONDUIT,            new ValueTier(600, 2000));
        put(Items.END_CRYSTAL,        new ValueTier(200, 650));
        put(Items.DRAGON_EGG,         new ValueTier(5000, 15000));
        put(Items.LODESTONE,          new ValueTier(300, 1000));
        put(Items.RESPAWN_ANCHOR,     new ValueTier(200, 650));

        // NOTE: Tools and armor are NO LONGER hardcoded here.
        // They are priced dynamically by computeIngredientPrice() which
        // calculates: material value × recipe ingredient count × crafting premium.
        // This automatically supports modded tools/armor via their Tier or ArmorMaterial.

        // ---- Other equipment (non-tiered / non-armor) ----
        put(Items.BOW,                TIER_TOOL_IRON);
        put(Items.CROSSBOW,           new ValueTier(30, 100));
        put(Items.SHIELD,             new ValueTier(20, 65));
        put(Items.FISHING_ROD,        new ValueTier(8, 30));
        put(Items.FLINT_AND_STEEL,    TIER_CRAFT_MAT);
        put(Items.SHEARS,             TIER_CRAFT_MAT);
        put(Items.COMPASS,            new ValueTier(20, 65));
        put(Items.CLOCK,              new ValueTier(25, 80));
        put(Items.SPYGLASS,           new ValueTier(30, 100));
        put(Items.LEAD,               TIER_CRAFT_MAT);

        // ---- Music discs ----
        put(Items.MUSIC_DISC_13,      TIER_MUSIC);
        put(Items.MUSIC_DISC_CAT,     TIER_MUSIC);
        put(Items.MUSIC_DISC_BLOCKS,  TIER_MUSIC);
        put(Items.MUSIC_DISC_CHIRP,   TIER_MUSIC);
        put(Items.MUSIC_DISC_FAR,     TIER_MUSIC);
        put(Items.MUSIC_DISC_MALL,    TIER_MUSIC);
        put(Items.MUSIC_DISC_MELLOHI, TIER_MUSIC);
        put(Items.MUSIC_DISC_STAL,    TIER_MUSIC);
        put(Items.MUSIC_DISC_STRAD,   TIER_MUSIC);
        put(Items.MUSIC_DISC_WARD,    TIER_MUSIC);
        put(Items.MUSIC_DISC_11,      TIER_MUSIC);
        put(Items.MUSIC_DISC_WAIT,    TIER_MUSIC);
        put(Items.MUSIC_DISC_OTHERSIDE, new ValueTier(100, 350));
        put(Items.MUSIC_DISC_PIGSTEP,   new ValueTier(150, 500));

        // ---- Dyes & decorative ----
        put(Items.WHITE_DYE,          TIER_COMMON);
        put(Items.RED_DYE,            TIER_COMMON);
        put(Items.BLUE_DYE,           TIER_COMMON);
        put(Items.GREEN_DYE,          TIER_COMMON);
        put(Items.BLACK_DYE,          TIER_COMMON);
        put(Items.YELLOW_DYE,         TIER_COMMON);
        put(Items.BROWN_DYE,          TIER_COMMON);
        put(Items.ORANGE_DYE,         TIER_COMMON);
        put(Items.PINK_DYE,           TIER_COMMON);
        put(Items.PURPLE_DYE,         TIER_COMMON);
        put(Items.CYAN_DYE,           TIER_COMMON);
        put(Items.LIGHT_BLUE_DYE,     TIER_COMMON);
        put(Items.LIGHT_GRAY_DYE,     TIER_COMMON);
        put(Items.LIME_DYE,           TIER_COMMON);
        put(Items.MAGENTA_DYE,        TIER_COMMON);

        // ---- Brewing ----
        put(Items.GLASS_BOTTLE,       TIER_JUNK);
        put(Items.FERMENTED_SPIDER_EYE, new ValueTier(10, 35));
        put(Items.BREWING_STAND,      new ValueTier(30, 100));
    }

    private static void put(Item item, ValueTier tier) {
        ITEM_OVERRIDES.put(item, tier);
    }

    // ===================== Tag-based Category Rules =====================

    /**
     * Ordered list of tag checks for classifying items by Forge/vanilla tags.
     * First match wins. This is the primary way modded items get priced.
     */
    private static final CategoryRule[] TAG_RULES = {
        // Ores (raw blocks / ore blocks)
        tagRule(Tags.Items.ORES,                   new ValueTier(12, 40)),
        tagRule(Tags.Items.RAW_MATERIALS,           new ValueTier(10, 35)),

        // Ingots
        tagRule(Tags.Items.INGOTS,                 TIER_INGOT_IRON),

        // Gems & dusts
        tagRule(Tags.Items.GEMS,                   new ValueTier(60, 200)),
        tagRule(Tags.Items.DUSTS,                  TIER_CRAFT_MAT),

        // Storage blocks (blocks of ingots/gems)
        tagRule(Tags.Items.STORAGE_BLOCKS,         new ValueTier(100, 350)),

        // Nuggets
        tagRule(Tags.Items.NUGGETS,                new ValueTier(3, 12)),


        // Logs & planks
        tagRule(ItemTags.LOGS,                     TIER_BASIC),
        tagRule(ItemTags.PLANKS,                   TIER_JUNK),

        // Wool & carpets
        tagRule(ItemTags.WOOL,                     TIER_COMMON),
        // Carpets covered by path heuristics

        // Sand
        tagRule(ItemTags.SAND,                     TIER_JUNK),

        // Saplings & flowers
        tagRule(ItemTags.SAPLINGS,                 TIER_BASIC),
        tagRule(ItemTags.FLOWERS,                  TIER_COMMON),

        // Fishes
        tagRule(ItemTags.FISHES,                   TIER_COMMON),

        // Music discs
        tagRule(ItemTags.MUSIC_DISCS,              TIER_MUSIC),

        // Banners
        tagRule(ItemTags.BANNERS,                  TIER_DECORATION),

        // Boats & minecarts
        tagRule(ItemTags.BOATS,                    TIER_CRAFT_MAT),

        // Candles
        tagRule(ItemTags.CANDLES,                  TIER_COMMON),

        // Coals
        tagRule(ItemTags.COALS,                    TIER_USEFUL),
    };

    private static CategoryRule tagRule(TagKey<Item> tag, ValueTier tier) {
        return new CategoryRule(stack -> stack.is(tag), tier);
    }

    private record CategoryRule(Predicate<ItemStack> test, ValueTier tier) {}

    // ===================== Ingredient-Based Pricing (Tools & Armor) =====================

    /** Crafting premium: 15% markup representing crafting labour. */
    private static final double CRAFTING_PREMIUM = 1.15;

    /**
     * Compute an ingredient-based price for tools and armor.
     * Returns null for items that aren't tools or armor.
     * <p>
     * This works for modded items automatically: it reads the Tier or ArmorMaterial
     * repair ingredient and prices it through the standard pipeline (tags, class, path).
     * If no repair ingredient is available, it interpolates from durability stats.
     */
    private static ValueTier computeIngredientPrice(ItemStack stack) {
        Item item = stack.getItem();
        if (item instanceof TieredItem tiered) return computeToolPrice(tiered);
        if (item instanceof ArmorItem armor)   return computeArmorPrice(armor);
        return null;
    }

    /**
     * Price a tool from: material value × recipe ingredient count + crafting premium.
     * Material values are sourced from {@link MaterialValues} (single source of truth).
     * Netherite tools are priced as the diamond version + a netherite ingot upgrade.
     */
    private static ValueTier computeToolPrice(TieredItem tool) {
        Tier tier = tool.getTier();
        int matValue = getToolMaterialValue(tier);
        int stickValue = MaterialValues.getValue(Items.STICK);
        int[] recipe = getToolIngredients(tool); // {materialCount, stickCount}

        int baseCost;
        if (tier == Tiers.NETHERITE) {
            // Netherite = diamond tool + 1 netherite ingot upgrade
            int diamondCost = getToolMaterialValue(Tiers.DIAMOND) * recipe[0]
                    + stickValue * recipe[1];
            baseCost = diamondCost + getToolMaterialValue(Tiers.NETHERITE);
        } else {
            baseCost = matValue * recipe[0] + stickValue * recipe[1];
        }

        int price = Math.max(1, (int) (baseCost * CRAFTING_PREMIUM));
        return new ValueTier(price, price * 3);
    }

    /**
     * Price armor from: material value × piece ingredient count + crafting premium.
     * Netherite armor is priced as the diamond piece + a netherite ingot upgrade.
     */
    private static ValueTier computeArmorPrice(ArmorItem armor) {
        ArmorMaterial mat = armor.getMaterial();
        int matValue = getArmorMaterialValue(mat);
        int count = getArmorIngredientCount(armor.getSlot());

        int baseCost;
        if (mat == ArmorMaterials.NETHERITE) {
            // Netherite = diamond armor piece + 1 netherite ingot
            int diamondCost = getArmorMaterialValue(ArmorMaterials.DIAMOND) * count;
            baseCost = diamondCost + getToolMaterialValue(Tiers.NETHERITE);
        } else {
            baseCost = matValue * count;
        }

        int price = Math.max(1, (int) (baseCost * CRAFTING_PREMIUM));
        return new ValueTier(price, price * 3);
    }

    /**
     * Get the CP value of one unit of the material used by a tool tier.
     * Known vanilla tiers are resolved through {@link MaterialValues}.
     * Modded tiers try the repair ingredient first, then fall back to durability.
     */
    private static int getToolMaterialValue(Tier tier) {
        if (tier == Tiers.WOOD)      return MaterialValues.getValue(Items.OAK_PLANKS);
        if (tier == Tiers.STONE)     return MaterialValues.getValue(Items.COBBLESTONE);
        if (tier == Tiers.IRON)      return MaterialValues.getValue(Items.IRON_INGOT);
        if (tier == Tiers.GOLD)      return MaterialValues.getValue(Items.GOLD_INGOT);
        if (tier == Tiers.DIAMOND)   return MaterialValues.getValue(Items.DIAMOND);
        if (tier == Tiers.NETHERITE) return MaterialValues.getValue(Items.NETHERITE_INGOT);

        // Modded tier: try to price the repair ingredient through MaterialValues
        try {
            ItemStack[] repairItems = tier.getRepairIngredient().getItems();
            if (repairItems.length > 0 && !repairItems[0].isEmpty()) {
                int val = MaterialValues.getValue(repairItems[0].getItem());
                if (val > 1) return val;
            }
        } catch (Exception ignored) {}

        // Fallback: interpolate from durability
        return estimateFromDurability(tier.getUses());
    }

    /**
     * Get the CP value of one unit of an armor material.
     * Known vanilla materials are resolved through {@link MaterialValues}.
     * Modded materials try the repair ingredient, then fall back to durability.
     */
    private static int getArmorMaterialValue(ArmorMaterial mat) {
        if (mat == ArmorMaterials.LEATHER)   return MaterialValues.getValue(Items.LEATHER);
        if (mat == ArmorMaterials.CHAIN)     return MaterialValues.getValue(Items.IRON_INGOT); // chain uses iron in repair
        if (mat == ArmorMaterials.IRON)      return MaterialValues.getValue(Items.IRON_INGOT);
        if (mat == ArmorMaterials.GOLD)      return MaterialValues.getValue(Items.GOLD_INGOT);
        if (mat == ArmorMaterials.DIAMOND)   return MaterialValues.getValue(Items.DIAMOND);
        if (mat == ArmorMaterials.TURTLE)    return MaterialValues.getValue(Items.SCUTE);
        if (mat == ArmorMaterials.NETHERITE) return MaterialValues.getValue(Items.NETHERITE_INGOT);

        // Modded armor: try the repair ingredient through MaterialValues
        try {
            ItemStack[] repairItems = mat.getRepairIngredient().getItems();
            if (repairItems.length > 0 && !repairItems[0].isEmpty()) {
                int val = MaterialValues.getValue(repairItems[0].getItem());
                if (val > 1) return val;
            }
        } catch (Exception ignored) {}

        // Fallback: estimate from chest-slot durability
        int durability = mat.getDurabilityForSlot(EquipmentSlot.CHEST);
        return estimateFromDurability(durability);
    }

    /**
     * Returns {materialCount, stickCount} for crafting a tool.
     * Matches vanilla crafting recipes.
     */
    private static int[] getToolIngredients(TieredItem tool) {
        if (tool instanceof SwordItem)   return new int[]{2, 1};
        if (tool instanceof PickaxeItem) return new int[]{3, 2};
        if (tool instanceof AxeItem)     return new int[]{3, 2};
        if (tool instanceof ShovelItem)  return new int[]{1, 2};
        if (tool instanceof HoeItem)     return new int[]{2, 2};
        return new int[]{2, 1}; // sensible default for unknown tiered items
    }

    /**
     * Returns the number of material pieces in a standard armor recipe.
     */
    private static int getArmorIngredientCount(EquipmentSlot slot) {
        return switch (slot) {
            case HEAD  -> 5;
            case CHEST -> 8;
            case LEGS  -> 7;
            case FEET  -> 4;
            default    -> 5;
        };
    }

    /**
     * Interpolates material value from durability, using vanilla reference points.
     * Wood=59→1, Stone=131→1, Iron=250→15, Diamond=1561→100, Netherite=2031→800.
     */
    private static int estimateFromDurability(int durability) {
        if (durability <= 59)   return 1;
        if (durability <= 250)  return 1  + (int) ((durability -   59.0) / ( 250.0 -   59.0) *  14);
        if (durability <= 1561) return 15 + (int) ((durability -  250.0) / (1561.0 -  250.0) *  85);
        if (durability <= 2031) return 100+ (int) ((durability - 1561.0) / (2031.0 - 1561.0) * 700);
        return 800 + (int) ((durability - 2031.0) / 1000.0 * 400); // beyond netherite
    }

    // ===================== Class-based Heuristics =====================

    /**
     * Classify unknown (modded) items by their Java class.
     * Tools and armor are handled above by {@link #computeIngredientPrice};
     * this covers special item types that don't have ingredient-based pricing.
     */
    private static ValueTier classifyByClass(ItemStack stack) {
        Item item = stack.getItem();

        if (item instanceof BowItem)           return new ValueTier(18, 54);  // 3 sticks + 3 string
        if (item instanceof CrossbowItem)      return new ValueTier(36, 108); // 3 sticks + 2 string + iron + hook
        if (item instanceof ShieldItem)        return new ValueTier(20, 65);
        if (item instanceof TridentItem)       return new ValueTier(400, 1400);
        if (item instanceof PotionItem)        return computePotionPrice(stack);
        if (item instanceof SpawnEggItem)      return TIER_SPAWN_EGG;
        if (item instanceof RecordItem)        return TIER_MUSIC;
        if (item instanceof EnchantedBookItem) return TIER_ENCHANTED;
        if (item instanceof BannerItem)        return TIER_DECORATION;
        if (item instanceof BlockItem)         return null; // fall through to path heuristics

        return null; // unknown
    }

    // ===================== Potion Pricing =====================

    /**
     * Calculate the value of a potion based on its actual ingredients, effect
     * strength, duration, and delivery method.
     * <p>
     * Pricing factors:
     * <ul>
     *   <li><b>Base brewing cost</b> – glass bottle + nether wart + blaze powder fuel</li>
     *   <li><b>Reagent cost</b> – value of the ingredient for the specific effect</li>
     *   <li><b>Amplifier</b> – Level II+ uses glowstone dust (extra cost per level)</li>
     *   <li><b>Duration</b> – Extended potions use redstone (extra cost)</li>
     *   <li><b>Delivery</b> – Splash (+gunpowder), Lingering (+dragon's breath)</li>
     *   <li><b>Labor premium</b> – 15% markup for brewing time and effort</li>
     * </ul>
     */
    private static ValueTier computePotionPrice(ItemStack stack) {
        Item item = stack.getItem();
        Potion potion = PotionUtils.getPotion(stack);

        // Intermediate / base potions with no useful effects
        if (potion == Potions.WATER || potion == Potions.MUNDANE
                || potion == Potions.THICK || potion == Potions.AWKWARD) {
            return new ValueTier(2, 8);
        }

        java.util.List<MobEffectInstance> effects = potion.getEffects();
        if (effects.isEmpty()) return new ValueTier(3, 10);

        // Base brewing cost: glass bottle(1) + nether wart(8) + blaze powder fuel(~1)
        int totalBase = 10;

        // Sum up value from every effect on this potion
        for (MobEffectInstance effect : effects) {
            int reagent = getEffectReagentCost(effect.getEffect());

            // Level II+ potions require glowstone dust (8 CP) per amplifier level
            if (effect.getAmplifier() > 0) {
                reagent += 8 * effect.getAmplifier();
            }

            // Extended-duration potions require redstone (8 CP)
            // Non-instant effects with >4800 ticks (~4 min) are "long" variants
            if (!effect.getEffect().isInstantenous() && effect.getDuration() > 4800) {
                reagent += 8;
            }

            totalBase += reagent;
        }

        // Multi-effect potions (Turtle Master) need extra brewing steps
        if (effects.size() > 1) {
            totalBase += 3;
        }

        // Delivery type costs
        if (item instanceof LingeringPotionItem) {
            // Dragon's Breath (100 CP) + extra brewing step
            totalBase = (int) (totalBase * 2.5) + 80;
        } else if (item instanceof SplashPotionItem) {
            // Gunpowder (5 CP) + extra brewing step
            totalBase = (int) (totalBase * 1.3) + 4;
        }

        // 15% crafting labor premium
        totalBase = (int) (totalBase * 1.15);

        // Max price = 3.5× base
        int maxPrice = (int) (totalBase * 3.5);

        return new ValueTier(Math.max(totalBase, 5), Math.max(maxPrice, 20));
    }

    /**
     * Get the reagent ingredient cost (in CP) for a given mob effect.
     * Values are sourced from {@link MaterialValues} for the actual brewing
     * ingredient used to create each potion effect.
     */
    private static int getEffectReagentCost(MobEffect effect) {
        // Costs derived from MaterialValues for each brewing ingredient
        if (effect == MobEffects.HEAL)              return MaterialValues.getIngredientCost(Items.GOLD_NUGGET, 8) + MaterialValues.getValue(Items.MELON_SLICE); // Glistering Melon
        if (effect == MobEffects.DAMAGE_BOOST)      return MaterialValues.getValue(Items.BLAZE_POWDER);
        if (effect == MobEffects.REGENERATION)       return MaterialValues.getValue(Items.GHAST_TEAR);
        if (effect == MobEffects.FIRE_RESISTANCE)    return MaterialValues.getValue(Items.MAGMA_CREAM);
        if (effect == MobEffects.MOVEMENT_SPEED)     return MaterialValues.getValue(Items.SUGAR);
        if (effect == MobEffects.JUMP)               return MaterialValues.getValue(Items.RABBIT_FOOT);
        if (effect == MobEffects.NIGHT_VISION)       return MaterialValues.getValue(Items.GOLDEN_CARROT);
        if (effect == MobEffects.INVISIBILITY)       return MaterialValues.getValue(Items.GOLDEN_CARROT) + MaterialValues.getValue(Items.FERMENTED_SPIDER_EYE); // 2 steps
        if (effect == MobEffects.WATER_BREATHING)    return MaterialValues.getValue(Items.PUFFERFISH);
        if (effect == MobEffects.SLOW_FALLING)       return MaterialValues.getValue(Items.PHANTOM_MEMBRANE);
        if (effect == MobEffects.POISON)             return MaterialValues.getValue(Items.SPIDER_EYE);
        if (effect == MobEffects.WEAKNESS)           return MaterialValues.getValue(Items.FERMENTED_SPIDER_EYE);
        if (effect == MobEffects.HARM)               return MaterialValues.getValue(Items.FERMENTED_SPIDER_EYE) + 25; // Healing/Poison + Fermented Spider Eye (2 steps)
        if (effect == MobEffects.MOVEMENT_SLOWDOWN)  return MaterialValues.getValue(Items.FERMENTED_SPIDER_EYE); // modifier on Speed
        if (effect == MobEffects.DIG_SLOWDOWN)       return MaterialValues.getValue(Items.FERMENTED_SPIDER_EYE); // Part of Turtle Master
        if (effect == MobEffects.DAMAGE_RESISTANCE)  return MaterialValues.getIngredientCost(Items.SCUTE, 5) / 2; // Turtle Shell (5 scute ÷ 2)
        if (effect == MobEffects.LUCK)               return 20; // Lucky potion (creative/rare)
        if (effect == MobEffects.ABSORPTION)         return 35; // Golden Apple effect (valuable)
        if (effect == MobEffects.SATURATION)         return 15; // Dandelion (suspicious stew)
        return 10; // Unknown / modded effect – reasonable default
    }

    // ===================== Path-based Heuristics =====================

    /**
     * Last-resort classification for items by their registry path.
     * Checks keywords in the path like "ingot", "ore", "sword" etc.
     */
    private static ValueTier classifyByPath(ItemStack stack) {
        ResourceLocation rl = ForgeRegistries.ITEMS.getKey(stack.getItem());
        if (rl == null) return null;
        String path = rl.getPath().toLowerCase();

        // Ingots & metals
        if (path.contains("netherite") && path.contains("ingot")) return TIER_LEGENDARY;
        if (path.contains("ingot"))     return TIER_INGOT_IRON;
        if (path.contains("nugget"))    return new ValueTier(3, 12);

        // Gems
        if (path.contains("diamond") || path.contains("gem") || path.contains("ruby")
                || path.contains("sapphire") || path.contains("amethyst")) return TIER_GEM;
        if (path.contains("emerald"))   return new ValueTier(80, 280);

        // Ores & raw
        if (path.contains("raw_") && (path.contains("iron") || path.contains("gold")
                || path.contains("copper"))) return new ValueTier(10, 35);
        if (path.contains("_ore"))      return new ValueTier(12, 40);

        // Storage blocks of materials
        if (path.contains("_block") && (path.contains("iron") || path.contains("gold")
                || path.contains("diamond") || path.contains("emerald")
                || path.contains("copper") || path.contains("netherite")))
            return new ValueTier(100, 350);

        // Tools & weapons
        if (path.contains("netherite") && (path.contains("sword") || path.contains("pickaxe")
                || path.contains("axe") || path.contains("shovel") || path.contains("hoe")))
            return TIER_TOOL_NETHERITE;
        if (path.contains("diamond") && (path.contains("sword") || path.contains("pickaxe")
                || path.contains("axe") || path.contains("shovel") || path.contains("hoe")))
            return TIER_TOOL_DIAMOND;
        if (path.contains("sword") || path.contains("pickaxe") || path.contains("axe_")
                || path.contains("_axe") || path.contains("shovel") || path.contains("hoe"))
            return TIER_TOOL_IRON;

        // Armor
        if (path.contains("netherite") && (path.contains("helmet") || path.contains("chestplate")
                || path.contains("leggings") || path.contains("boots")))
            return TIER_TOOL_NETHERITE;
        if (path.contains("diamond") && (path.contains("helmet") || path.contains("chestplate")
                || path.contains("leggings") || path.contains("boots")))
            return TIER_TOOL_DIAMOND;
        if (path.contains("helmet") || path.contains("chestplate")
                || path.contains("leggings") || path.contains("boots"))
            return TIER_TOOL_IRON;

        // Food
        if (path.contains("cooked_") || path.contains("baked_") || path.contains("stew")
                || path.contains("soup"))
            return TIER_FOOD_GOOD;
        if (path.contains("raw_") && (path.contains("beef") || path.contains("pork")
                || path.contains("chicken") || path.contains("fish") || path.contains("mutton")
                || path.contains("rabbit") || path.contains("cod") || path.contains("salmon")))
            return TIER_COMMON;

        // Dust, powder, etc.
        if (path.contains("dust") || path.contains("powder"))  return TIER_CRAFT_MAT;
        if (path.contains("shard") || path.contains("crystal")) return TIER_CRAFT_MAT;

        // Spawn eggs
        if (path.contains("spawn_egg")) return TIER_SPAWN_EGG;

        // Planks, logs, wood
        if (path.contains("planks"))    return TIER_JUNK;
        if (path.contains("_log") || path.contains("_stem") || path.contains("_wood"))
            return TIER_BASIC;

        // Stone variants, cobble
        if (path.contains("cobble") || path.contains("stone") || path.contains("deepslate"))
            return TIER_JUNK;

        // Dyes
        if (path.contains("_dye"))      return TIER_COMMON;

        return null;
    }

    // ===================== Rarity Fallback =====================

    private static ValueTier rarityFallback(ItemStack stack) {
        Rarity rarity = stack.getRarity();
        return switch (rarity) {
            case EPIC     -> new ValueTier(81, 270);
            case RARE     -> new ValueTier(27, 90);
            case UNCOMMON -> new ValueTier(9, 30);
            default       -> TIER_COMMON; // COMMON
        };
    }

    // ===================== Public API =====================

    /**
     * Get the full value tier for an item, resolving through the classification pipeline.
     */
    public static ValueTier getValueTier(ItemStack stack) {
        if (stack.isEmpty()) return TIER_JUNK;

        // 0. Ingredient-based pricing for tools & armor (handles vanilla + modded)
        ValueTier tier = computeIngredientPrice(stack);
        if (tier != null) {
            if (stack.isEnchanted()) tier = applyEnchantBonus(tier, stack);
            return tier;
        }

        // 1. Exact item override
        tier = ITEM_OVERRIDES.get(stack.getItem());
        if (tier != null) {
            // Apply enchantment bonus
            if (stack.isEnchanted()) {
                tier = applyEnchantBonus(tier, stack);
            }
            return tier;
        }

        // 2. Tag-based rules
        for (CategoryRule rule : TAG_RULES) {
            if (rule.test().test(stack)) {
                tier = rule.tier();
                if (stack.isEnchanted()) tier = applyEnchantBonus(tier, stack);
                return tier;
            }
        }

        // 3. Class-based heuristics
        tier = classifyByClass(stack);
        if (tier != null) {
            if (stack.isEnchanted()) tier = applyEnchantBonus(tier, stack);
            return tier;
        }

        // 4. Path-based heuristics
        tier = classifyByPath(stack);
        if (tier != null) {
            if (stack.isEnchanted()) tier = applyEnchantBonus(tier, stack);
            return tier;
        }

        // 5. Rarity fallback
        tier = rarityFallback(stack);
        if (stack.isEnchanted()) tier = applyEnchantBonus(tier, stack);
        return tier;
    }

    /** Enchanted items get 3× base and 3× max ceiling. */
    private static ValueTier applyEnchantBonus(ValueTier tier, ItemStack stack) {
        if (!stack.isEnchanted()) return tier;
        int enchantCount = stack.getEnchantmentTags().size();
        double mult = 1.0 + enchantCount * 0.8; // each enchant adds 80% value
        return new ValueTier(
                Math.max(tier.basePrice(), (int)(tier.basePrice() * mult)),
                Math.max(tier.maxPrice(),  (int)(tier.maxPrice()  * mult))
        );
    }

    /**
     * Get the base value of an item in copper pieces.
     */
    public static int getBaseValue(ItemStack stack) {
        return getValueTier(stack).basePrice();
    }

    /**
     * Get the maximum price an item can be listed at and still sell.
     */
    public static int getMaxPrice(ItemStack stack) {
        return getValueTier(stack).maxPrice();
    }

    /**
     * Get the rarity tier (for legacy compat).
     */
    public static int getRarityTier(Rarity rarity) {
        return switch (rarity) {
            case EPIC -> 4;
            case RARE -> 3;
            case UNCOMMON -> 2;
            default -> 1;
        };
    }

    /**
     * Get the rarity display name for an item.
     */
    public static String getRarityName(Rarity rarity) {
        return switch (rarity) {
            case EPIC -> "Epic";
            case RARE -> "Rare";
            case UNCOMMON -> "Uncommon";
            default -> "Common";
        };
    }

    /**
     * Calculate the sale speed multiplier based on how the player's price compares
     * to the fair value and the item's max price ceiling.
     * <p>
     * Returns:
     * <ul>
     *   <li>&gt; 1.0 – underpriced, sells faster</li>
     *   <li>  1.0   – fair price</li>
     *   <li>&lt; 1.0 – overpriced, sells slower</li>
     *   <li>  0.0   – price exceeds max ceiling, WILL NOT SELL</li>
     * </ul>
     */
    public static double getSaleSpeedMultiplier(int playerPrice, int fairValue,
                                                 int maxPrice, double overpriceThreshold) {
        if (fairValue <= 0) return 1.0;

        // Hard ceiling: price above maxPrice = will not sell
        if (playerPrice > maxPrice) return 0.0;

        double ratio = (double) playerPrice / fairValue;

        if (ratio <= 0.75) {
            // Very underpriced - sells much faster (up to 1.5×)
            return 1.5;
        } else if (ratio <= 1.0) {
            // Slightly underpriced - bonus speed
            return 1.0 + (1.0 - ratio) * 2.0;
        } else if (ratio <= overpriceThreshold) {
            // Within reasonable markup - normal speed
            return 1.0;
        } else {
            // Overpriced but below ceiling - scales linearly to 0
            double ceilingRatio = (double) maxPrice / fairValue;
            double overRange = ceilingRatio - overpriceThreshold;
            if (overRange <= 0) return 0.05; // safety
            double overAmount = ratio - overpriceThreshold;
            double speedFactor = 1.0 - (overAmount / overRange);
            return Math.max(0.03, speedFactor * 0.8); // minimum 3% chance while under ceiling
        }
    }

    /**
     * Legacy 3-param overload for backward compat.
     */
    public static double getSaleSpeedMultiplier(int playerPrice, int fairValue,
                                                 double overpriceThreshold) {
        // Estimate maxPrice as 3× fair value if no explicit max is available
        return getSaleSpeedMultiplier(playerPrice, fairValue, fairValue * 3, overpriceThreshold);
    }

    /**
     * Describe how well-priced an item is relative to its fair value and ceiling.
     * Returns a color-coded status for UI display.
     */
    public static PriceRating getPriceRating(int playerPrice, int fairValue, int maxPrice) {
        if (playerPrice > maxPrice) return PriceRating.WILL_NOT_SELL;
        if (playerPrice > fairValue * 2) return PriceRating.OVERPRICED;
        if (playerPrice > fairValue * 1.3) return PriceRating.HIGH;
        if (playerPrice >= fairValue * 0.8) return PriceRating.FAIR;
        return PriceRating.UNDERPRICED;
    }

    public enum PriceRating {
        UNDERPRICED  ("Underpriced",    0x55AAFF),  // blue - sells fast but cheap
        FAIR         ("Fair Price",     0x55FF55),  // green - good balance
        HIGH         ("Slightly High",  0xFFFF55),  // yellow - still sells
        OVERPRICED   ("Overpriced",     0xFF8800),  // orange - sells very slowly
        WILL_NOT_SELL("Won't Sell!",    0xFF5555);  // red - above ceiling

        private final String label;
        private final int color;

        PriceRating(String label, int color) {
            this.label = label;
            this.color = color;
        }

        public String getLabel() { return label; }
        public int getColor() { return color; }
    }

    /**
     * Calculate the final sale value considering town modifiers.
     * <p>
     * Uses the graduated {@link NeedLevel} system:
     * distance multiplier × needLevel multiplier.
     */
    public static int calculateFinalValue(ItemStack stack, int basePrice, TownData town) {
        double multiplier = town.getDistanceValueMultiplier();

        NeedLevel level = town.getNeedLevel(stack.getItem());
        multiplier *= level.getPriceMultiplier();

        return Math.max(1, (int) (basePrice * multiplier));
    }

    /**
     * Calculate the final max price ceiling considering town modifiers.
     * <p>
     * Only demand-side NeedLevels raise the ceiling (surplus doesn't lower it,
     * since max price is already a hard cap).
     */
    public static int calculateFinalMaxPrice(ItemStack stack, int maxPrice, TownData town) {
        double multiplier = town.getDistanceValueMultiplier();
        NeedLevel level = town.getNeedLevel(stack.getItem());
        if (level.isInDemand()) {
            multiplier *= level.getPriceMultiplier();
        }
        return Math.max(1, (int) (maxPrice * multiplier));
    }

    // ===================== Price Breakdown =====================

    /**
     * A detailed breakdown of how a price was computed.
     * Used for tooltip rendering in Trading Bin and Market Board.
     */
    public record PriceBreakdown(
            String itemName,
            int materialCost,       // base fair value from getBaseValue()
            int craftingTax,        // materialCost * taxPercent/100
            int subtotal,           // materialCost + craftingTax
            NeedLevel needLevel,    // town's need level for this item
            double needMultiplier,  // the multiplier from NeedLevel
            double distanceMultiplier, // from town distance
            int finalPrice,         // after all multipliers
            int maxPrice            // ceiling from calculateFinalMaxPrice
    ) {
        /**
         * Format materialCost as a multi-line ingredient list for tooltips.
         * Returns the label for the base value source (e.g., "Materials" for tools,
         * "Base Value" for simple items).
         */
        public String getBaseLabel() {
            return materialCost > 0 ? "Materials" : "Base Value";
        }

        /**
         * Whether we have any tax component.
         */
        public boolean hasTax() {
            return craftingTax > 0;
        }

        /**
         * Whether the town modifier deviates from 1.0×.
         */
        public boolean hasTownModifier() {
            return needLevel != NeedLevel.BALANCED || distanceMultiplier != 1.0;
        }

        /**
         * Total town multiplier (distance × need).
         */
        public double getTotalTownMultiplier() {
            return distanceMultiplier * needMultiplier;
        }
    }

    /**
     * Compute a full price breakdown for an item, given a town and bin settings.
     *
     * @param stack      The item to price.
     * @param town       The destination town (or null for no town modifiers).
     * @param taxPercent Crafting tax percent from the Trading Bin settings (0-100).
     * @return A breakdown of every cost component.
     */
    public static PriceBreakdown getBreakdown(ItemStack stack, TownData town, int taxPercent) {
        String itemName = stack.getHoverName().getString();
        int materialCost = getBaseValue(stack);
        int craftingTax = (int) (materialCost * (taxPercent / 100.0));
        int subtotal = materialCost + craftingTax;

        NeedLevel needLevel = NeedLevel.BALANCED;
        double needMultiplier = 1.0;
        double distanceMultiplier = 1.0;

        if (town != null) {
            needLevel = town.getNeedLevel(stack.getItem());
            needMultiplier = needLevel.getPriceMultiplier();
            distanceMultiplier = town.getDistanceValueMultiplier();
        }

        int finalPrice = Math.max(1, (int) (subtotal * distanceMultiplier * needMultiplier));
        int maxPrice = town != null
                ? calculateFinalMaxPrice(stack, getMaxPrice(stack), town)
                : getMaxPrice(stack);

        return new PriceBreakdown(
                itemName, materialCost, craftingTax, subtotal,
                needLevel, needMultiplier, distanceMultiplier,
                finalPrice, maxPrice
        );
    }
}
