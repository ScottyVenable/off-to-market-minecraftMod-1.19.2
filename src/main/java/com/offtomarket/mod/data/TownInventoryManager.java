package com.offtomarket.mod.data;

import com.offtomarket.mod.data.TownInventory.StockSlot;
import com.offtomarket.mod.item.AnimalTradeSlipItem;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.resources.ResourceLocation;

import java.util.*;

/**
 * Global manager for all town inventories.
 *
 * Handles:
 *  - Per-town persistent inventory of sellable items
 *  - Daily restocking with production chain bonuses
 *  - Black market random appearance with rare items
 *  - Price decay (buyCount reduction) on restock
 *  - Purchase tracking to drive dynamic pricing
 *
 * Inventory data is persisted via TradingData (world-level SavedData).
 * All Trading Posts share the same town inventories.
 */
public class TownInventoryManager {

    private static final Map<String, TownInventory> inventories = new LinkedHashMap<>();

    // Black market state
    private static boolean blackMarketActive = false;
    private static long blackMarketEndDay = -1;
    private static final String BLACK_MARKET_ID = "blackmarket";

    // Config: chance per dawn for black market to appear when not active
    private static final double BLACK_MARKET_CHANCE = 0.08; // 8%
    // Config: how many days the black market lasts
    private static final int BLACK_MARKET_MIN_DAYS = 2;
    private static final int BLACK_MARKET_MAX_DAYS = 3;

    /** Whether town inventories have been initialized this server session. */
    private static boolean initialized = false;

    private static final Random RANDOM = new Random();

    // ==================== Initialization ====================

    /**
     * Initialize town inventories for all registered towns.
     * Called once per server session when a Trading Post first ticks.
     * If inventories were loaded from save data, this is a no-op.
     */
    public static void initializeIfNeeded() {
        if (initialized) return;
        initialized = true;

        // Create inventories for any towns that don't already have one
        // (existing inventories from save data are preserved)
        for (TownData town : TownRegistry.getAllTowns()) {
            if (!inventories.containsKey(town.getId())) {
                TownInventory inv = new TownInventory(town.getId());
                inventories.put(town.getId(), inv);
                // Initial stock generation
                generateInitialStock(inv, town);
            }
        }
    }

    /**
     * Generate initial stock for a new (never-before-seen) town inventory.
     * Uses the town's specialty items to populate the inventory.
     */
    private static void generateInitialStock(TownInventory inv, TownData town) {
        List<ResourceLocation> specialties = new ArrayList<>(town.getSpecialtyItems());
        Collections.shuffle(specialties, RANDOM);

        // Stock 60-80% of specialty items initially
        int count = Math.max(4, (int) (specialties.size() * (0.6 + RANDOM.nextDouble() * 0.2)));
        count = Math.min(count, specialties.size());

        for (int i = 0; i < count; i++) {
            ResourceLocation itemId = specialties.get(i);
            net.minecraft.world.item.Item item =
                    net.minecraftforge.registries.ForgeRegistries.ITEMS.getValue(itemId);
            if (item == null || item == net.minecraft.world.item.Items.AIR) continue;

            // Skip items the town desperately needs
            NeedLevel need = town.getNeedLevel(item);
            if (need == NeedLevel.DESPERATE || need == NeedLevel.HIGH_NEED) continue;

            addItemToInventory(inv, town, itemId, item, need, true);
        }

        // Special Peenam animal market initial stock
        if ("peenam".equals(town.getId())) {
            generateAnimalStock(inv, town, true);
        }
    }

    // ==================== Restocking ====================

    /**
     * Restock all town inventories. Called once per dawn.
     * Adds new stock (does NOT clear), applies production chain bonuses,
     * decays buy counts, rolls for sales, and handles the black market.
     */
    public static void restockAll(long currentDay) {
        initializeIfNeeded();

        for (TownData town : TownRegistry.getAllTowns()) {
            TownInventory inv = inventories.computeIfAbsent(town.getId(), TownInventory::new);

            // Only restock if a day has passed since last restock
            if (inv.getLastRestockDay() >= currentDay) continue;
            inv.setLastRestockDay(currentDay);

            restockTown(inv, town);
        }

        // Black market logic
        handleBlackMarket(currentDay);
    }

    /**
     * Restock a single town's inventory.
     */
    private static void restockTown(TownInventory inv, TownData town) {
        // Decay buy counts by 50% to gradually normalize prices
        for (StockSlot slot : inv.getSlots()) {
            slot.setBuyCount(slot.getBuyCount() / 2);
        }

        // Calculate production chain multiplier
        double productionMult = calculateProductionMultiplier(town);

        List<ResourceLocation> specialties = new ArrayList<>(town.getSpecialtyItems());
        Collections.shuffle(specialties, RANDOM);

        // Restock existing items and add some new ones
        int restockCount = 4 + RANDOM.nextInt(6); // 4-9 items restocked
        int restocked = 0;

        for (ResourceLocation itemId : specialties) {
            if (restocked >= restockCount) break;

            net.minecraft.world.item.Item item =
                    net.minecraftforge.registries.ForgeRegistries.ITEMS.getValue(itemId);
            if (item == null || item == net.minecraft.world.item.Items.AIR) continue;

            NeedLevel need = town.getNeedLevel(item);
            if (need == NeedLevel.DESPERATE || need == NeedLevel.HIGH_NEED) continue;
            if (need == NeedLevel.MODERATE_NEED && RANDOM.nextFloat() > 0.25f) continue;

            addItemToInventory(inv, town, itemId, item, need, false);
            restocked++;
        }

        // Apply production chain bonus: chance for extra specialty items
        if (productionMult > 1.0) {
            int bonusItems = (int) ((productionMult - 1.0) * 4); // 0-2 extra items at 1.5x
            for (int i = 0; i < bonusItems && !specialties.isEmpty(); i++) {
                ResourceLocation bonusItemId = specialties.get(RANDOM.nextInt(specialties.size()));
                net.minecraft.world.item.Item bonusItem =
                        net.minecraftforge.registries.ForgeRegistries.ITEMS.getValue(bonusItemId);
                if (bonusItem == null || bonusItem == net.minecraft.world.item.Items.AIR) continue;
                NeedLevel bonusNeed = town.getNeedLevel(bonusItem);
                if (bonusNeed == NeedLevel.DESPERATE || bonusNeed == NeedLevel.HIGH_NEED) continue;
                addItemToInventory(inv, town, bonusItemId, bonusItem, bonusNeed, false);
            }
        }

        // Restock Peenam animals
        if ("peenam".equals(town.getId())) {
            generateAnimalStock(inv, town, false);
        }

        // Re-roll sales: clear old sales, 20% chance per slot
        for (StockSlot slot : inv.getSlots()) {
            if (RANDOM.nextFloat() < 0.20f) {
                slot.setOnSale(true);
                slot.setSaleDiscount(10 + RANDOM.nextInt(21)); // 10-30%
            } else {
                slot.setOnSale(false);
                slot.setSaleDiscount(0);
            }
        }

        // Cap total slots to prevent unbounded growth
        while (inv.getSlots().size() > 30) {
            // Remove the slot with lowest quantity (least relevant stock)
            inv.getSlots().stream()
                    .min(Comparator.comparingInt(StockSlot::getQuantity))
                    .ifPresent(s -> inv.removeSlot(s.getStockKey()));
        }
    }

    /**
     * Add or restock an item in a town's inventory.
     *
     * For normal items: adds quantity to existing slot or creates a new one.
     * For enchanted books/potions: creates new unique slots.
     */
    private static void addItemToInventory(TownInventory inv, TownData town,
                                           ResourceLocation itemId,
                                           net.minecraft.world.item.Item item,
                                           NeedLevel need, boolean isInitial) {
        boolean isModerateNeed = need == NeedLevel.MODERATE_NEED;

        // Enchanted books get random enchantments — each is a unique slot
        if (item instanceof net.minecraft.world.item.EnchantedBookItem) {
            MarketListing.EnchantBookData bookData = MarketListing.generateRandomEnchantedBook(RANDOM);
            if (bookData.nbt != null) {
                String key = StockSlot.makeStockKey(itemId, bookData.nbt);
                // Don't duplicate existing enchanted book slots
                if (inv.findSlot(key) == null) {
                    inv.getSlots().add(new StockSlot(key, itemId, bookData.displayName,
                            1, 1, bookData.nbt));
                }
            }
            return;
        }

        // Potions get random potion types — each type is a unique slot
        if (item instanceof net.minecraft.world.item.PotionItem) {
            CompoundTag potionNbt = MarketListing.generateRandomPotionNbt(RANDOM);
            String key = StockSlot.makeStockKey(itemId, potionNbt);
            net.minecraft.world.item.ItemStack tempStack = new net.minecraft.world.item.ItemStack(item);
            tempStack.setTag(potionNbt.copy());
            String displayName = tempStack.getHoverName().getString();
            if (displayName.contains("Uncraftable")) return;

            StockSlot existing = inv.findSlot(key);
            int maxQty = isModerateNeed ? 3 : 6;
            if (existing != null) {
                int addQty = 1 + RANDOM.nextInt(3); // 1-3
                existing.setQuantity(Math.min(existing.getMaxQuantity(),
                        existing.getQuantity() + addQty));
            } else {
                int qty = isModerateNeed ? 1 + RANDOM.nextInt(2) : 1 + RANDOM.nextInt(maxQty);
                inv.getSlots().add(new StockSlot(key, itemId, displayName,
                        qty, maxQty, potionNbt));
            }
            return;
        }

        // Tipped arrows get random potion effects
        if (item instanceof net.minecraft.world.item.TippedArrowItem) {
            CompoundTag arrowNbt = MarketListing.generateRandomPotionNbt(RANDOM);
            String key = StockSlot.makeStockKey(itemId, arrowNbt);
            net.minecraft.world.item.ItemStack tempStack = new net.minecraft.world.item.ItemStack(item);
            tempStack.setTag(arrowNbt.copy());
            String displayName = tempStack.getHoverName().getString();
            if (displayName.contains("Uncraftable")) return;

            StockSlot existing = inv.findSlot(key);
            int maxQty = isModerateNeed ? 5 : 12;
            if (existing != null) {
                int addQty = 2 + RANDOM.nextInt(4); // 2-5
                existing.setQuantity(Math.min(existing.getMaxQuantity(),
                        existing.getQuantity() + addQty));
            } else {
                int qty = isModerateNeed ? 2 + RANDOM.nextInt(3) : 2 + RANDOM.nextInt(maxQty - 1);
                inv.getSlots().add(new StockSlot(key, itemId, displayName,
                        qty, maxQty, arrowNbt));
            }
            return;
        }

        // Normal items: single slot per item type, stackable quantity
        String key = StockSlot.makeStockKey(itemId, null);
        String displayName = new net.minecraft.world.item.ItemStack(item).getHoverName().getString();
        if (displayName.contains("Uncraftable")) return;

        int maxQty = isModerateNeed ? 5 : 16;
        StockSlot existing = inv.findSlot(key);
        if (existing != null) {
            // Restock: add some quantity up to max
            int addQty = isModerateNeed
                    ? 1 + RANDOM.nextInt(2) // 1-2 for moderate need
                    : 2 + RANDOM.nextInt(8); // 2-9 for normal
            existing.setQuantity(Math.min(existing.getMaxQuantity(),
                    existing.getQuantity() + addQty));
        } else {
            int qty;
            if (isInitial) {
                qty = isModerateNeed ? 1 + RANDOM.nextInt(3) : 4 + RANDOM.nextInt(13); // 4-16
            } else {
                qty = isModerateNeed ? 1 + RANDOM.nextInt(3) : 1 + RANDOM.nextInt(16);
            }
            inv.getSlots().add(new StockSlot(key, itemId, displayName,
                    qty, maxQty, null));
        }
    }

    /**
     * Generate animal trade slip stock for Peenam.
     */
    private static void generateAnimalStock(TownInventory inv, TownData town, boolean isInitial) {
        List<String> commonAnimals = new ArrayList<>(List.of(
                "minecraft:chicken", "minecraft:pig", "minecraft:sheep", "minecraft:cow",
                "minecraft:rabbit", "minecraft:goat", "minecraft:horse", "minecraft:donkey",
                "minecraft:llama", "minecraft:cat", "minecraft:wolf", "minecraft:parrot",
                "minecraft:bee", "minecraft:fox", "minecraft:axolotl", "minecraft:turtle",
                "minecraft:frog"
        ));
        List<String> rareAnimals = List.of(
                "minecraft:mooshroom", "minecraft:panda", "minecraft:polar_bear",
                "minecraft:mule", "minecraft:camel", "minecraft:allay", "minecraft:sniffer"
        );

        ResourceLocation slipItemId = new ResourceLocation("offtomarket", "animal_trade_slip");

        // Stock 3-6 common animals
        Collections.shuffle(commonAnimals, RANDOM);
        int animalCount = 3 + RANDOM.nextInt(4);
        for (int i = 0; i < Math.min(animalCount, commonAnimals.size()); i++) {
            String animalType = commonAnimals.get(i);
            CompoundTag nbt = new CompoundTag();
            nbt.putBoolean(AnimalTradeSlipItem.TAG_IS_FILLED, true);
            nbt.putString(AnimalTradeSlipItem.TAG_ANIMAL_TYPE, animalType);
            nbt.putInt(AnimalTradeSlipItem.TAG_BASE_VALUE, AnimalTradeSlipItem.getBaseValue(animalType));

            String key = StockSlot.makeStockKey(slipItemId, nbt);
            String displayName = "Animal Slip - " + AnimalTradeSlipItem.getAnimalDisplayName(animalType);

            StockSlot existing = inv.findSlot(key);
            if (existing != null) {
                existing.setQuantity(Math.min(3, existing.getQuantity() + 1));
            } else {
                inv.getSlots().add(new StockSlot(key, slipItemId, displayName,
                        1, 3, nbt));
            }
        }

        // 30% chance for a rare animal
        if (RANDOM.nextFloat() < 0.30f && !rareAnimals.isEmpty()) {
            String rareAnimal = rareAnimals.get(RANDOM.nextInt(rareAnimals.size()));
            CompoundTag nbt = new CompoundTag();
            nbt.putBoolean(AnimalTradeSlipItem.TAG_IS_FILLED, true);
            nbt.putString(AnimalTradeSlipItem.TAG_ANIMAL_TYPE, rareAnimal);
            nbt.putInt(AnimalTradeSlipItem.TAG_BASE_VALUE, AnimalTradeSlipItem.getBaseValue(rareAnimal));

            String key = StockSlot.makeStockKey(slipItemId, nbt);
            String displayName = "Animal Slip - " + AnimalTradeSlipItem.getAnimalDisplayName(rareAnimal) + " (Rare!)";

            if (inv.findSlot(key) == null) {
                inv.getSlots().add(new StockSlot(key, slipItemId, displayName,
                        1, 1, nbt));
            }
        }
    }

    // ==================== Production Chains ====================

    /**
     * Calculate a production multiplier for a town based on how well-supplied
     * its catalyst/needed items are.
     *
     * When players sell tools and resources that a town needs, that town
     * becomes more productive and restocks specialty items at a higher rate.
     *
     * Examples:
     *  - Mining town with high pickaxe supply -> more ores in restock
     *  - Farm town with high hoe/seed supply  -> more crops in restock
     *  - Lumber town with high axe supply     -> more logs in restock
     *
     * Returns: multiplier from 0.5 (poorly supplied) to 1.5 (well supplied).
     * Default is 1.0 (balanced).
     */
    private static double calculateProductionMultiplier(TownData town) {
        Set<ResourceLocation> needs = town.getNeeds();
        if (needs == null || needs.isEmpty()) return 1.0;

        int totalSupply = 0;
        int trackedItems = 0;

        for (ResourceLocation needItem : needs) {
            int supply = town.getSupplyLevel(needItem.toString());
            if (supply >= 0) { // -1 means untracked
                totalSupply += supply;
                trackedItems++;
            }
        }

        if (trackedItems == 0) return 1.0;

        double avgSupply = (double) totalSupply / trackedItems;

        // Map average supply to multiplier:
        // < 30: 0.5x (poorly supplied)
        // 30-50: 0.8x
        // 50-70: 1.0x (balanced, near equilibrium of 60)
        // 70-90: 1.3x
        // > 90: 1.5x (well supplied)
        if (avgSupply > 90) return 1.5;
        if (avgSupply > 70) return 1.3;
        if (avgSupply > 50) return 1.0;
        if (avgSupply > 30) return 0.8;
        return 0.5;
    }

    // ==================== Black Market ====================

    /**
     * Handle black market appearance/disappearance at dawn.
     */
    private static void handleBlackMarket(long currentDay) {
        if (blackMarketActive) {
            if (currentDay >= blackMarketEndDay) {
                // Black market disappears
                blackMarketActive = false;
                inventories.remove(BLACK_MARKET_ID);
            }
            return;
        }

        // Chance to appear
        if (RANDOM.nextDouble() < BLACK_MARKET_CHANCE) {
            blackMarketActive = true;
            int duration = BLACK_MARKET_MIN_DAYS + RANDOM.nextInt(BLACK_MARKET_MAX_DAYS - BLACK_MARKET_MIN_DAYS + 1);
            blackMarketEndDay = currentDay + duration;

            TownInventory bm = new TownInventory(BLACK_MARKET_ID);
            bm.setLastRestockDay(currentDay);
            generateBlackMarketStock(bm);
            inventories.put(BLACK_MARKET_ID, bm);
        }
    }

    /**
     * Generate rare/powerful items for the black market.
     * All items are at 250-350% of base price (handled by the pricing system
     * since black market distance is 10 and all items are at balanced need).
     */
    private static void generateBlackMarketStock(TownInventory bm) {
        // Rare items pool
        List<String> rareItems = new ArrayList<>(List.of(
                "minecraft:elytra",
                "minecraft:totem_of_undying",
                "minecraft:netherite_ingot",
                "minecraft:netherite_sword",
                "minecraft:netherite_pickaxe",
                "minecraft:netherite_axe",
                "minecraft:netherite_chestplate",
                "minecraft:netherite_helmet",
                "minecraft:netherite_leggings",
                "minecraft:netherite_boots",
                "minecraft:enchanted_golden_apple",
                "minecraft:beacon",
                "minecraft:nether_star",
                "minecraft:trident",
                "minecraft:heart_of_the_sea",
                "minecraft:shulker_box",
                "minecraft:end_crystal",
                "minecraft:wither_skeleton_skull",
                "minecraft:dragon_breath",
                "minecraft:conduit"
        ));

        Collections.shuffle(rareItems, RANDOM);
        int itemCount = 3 + RANDOM.nextInt(4); // 3-6 items

        for (int i = 0; i < Math.min(itemCount, rareItems.size()); i++) {
            ResourceLocation itemId = new ResourceLocation(rareItems.get(i));
            net.minecraft.world.item.Item item =
                    net.minecraftforge.registries.ForgeRegistries.ITEMS.getValue(itemId);
            if (item == null || item == net.minecraft.world.item.Items.AIR) continue;

            String displayName = new net.minecraft.world.item.ItemStack(item).getHoverName().getString();
            String key = StockSlot.makeStockKey(itemId, null);
            int qty = 1 + RANDOM.nextInt(2); // 1-2

            bm.getSlots().add(new StockSlot(key, itemId, displayName, qty, qty, null));
        }

        // Always include 1-2 high-level enchanted books
        for (int i = 0; i < 1 + RANDOM.nextInt(2); i++) {
            MarketListing.EnchantBookData book = MarketListing.generateRandomEnchantedBook(RANDOM);
            if (book.nbt != null) {
                ResourceLocation bookId = new ResourceLocation("minecraft", "enchanted_book");
                String key = StockSlot.makeStockKey(bookId, book.nbt);
                if (bm.findSlot(key) == null) {
                    bm.getSlots().add(new StockSlot(key, bookId, book.displayName,
                            1, 1, book.nbt));
                }
            }
        }
    }

    // ==================== Public API ====================

    /**
     * Get a town's inventory (or null if not yet initialized).
     */
    @javax.annotation.Nullable
    public static TownInventory getInventory(String townId) {
        return inventories.get(townId);
    }

    /**
     * Record a purchase from a town's inventory.
     * Decreases stock and tracks demand.
     *
     * @return true if the purchase was valid
     */
    public static boolean recordPurchase(String townId, String stockKey, int quantity) {
        TownInventory inv = inventories.get(townId);
        if (inv == null) return false;
        boolean result = inv.recordPurchase(stockKey, quantity);
        // Also update global supply/demand tracking
        if (result) {
            TownData town = TownRegistry.getTown(townId);
            if (town != null) {
                // Extract the item portion of the stock key (before any # qualifier)
                String itemKey = stockKey.contains("#") ? stockKey.substring(0, stockKey.indexOf('#')) : stockKey;
                SupplyDemandManager.recordPurchase(town, itemKey, quantity);
            }
        }
        return result;
    }

    /**
     * Whether the black market is currently active.
     */
    public static boolean isBlackMarketActive() {
        return blackMarketActive;
    }

    /**
     * Get the day the black market closes (or -1 if inactive).
     */
    public static long getBlackMarketEndDay() {
        return blackMarketActive ? blackMarketEndDay : -1;
    }

    /**
     * Get the black market's item listings (empty list if not active).
     */
    public static List<MarketListing> getBlackMarketListings(long gameTime) {
        if (!blackMarketActive) return Collections.emptyList();
        TownInventory bm = inventories.get(BLACK_MARKET_ID);
        if (bm == null) return Collections.emptyList();

        // Black market uses a dummy TownData for pricing (distance 10, OUTPOST type)
        // We compute prices manually with a 2.5x base multiplier
        List<MarketListing> listings = new ArrayList<>();
        for (StockSlot slot : bm.getSlots()) {
            if (slot.getQuantity() <= 0) continue;
            net.minecraft.world.item.ItemStack sample = slot.createSampleStack();
            int basePrice = PriceCalculator.getBaseValue(sample);
            // Black market premium: 2.5x to 3.5x base price
            double premium = 2.5 + RANDOM.nextDouble();
            int price = Math.max(1, (int) Math.round(basePrice * premium));

            listings.add(new MarketListing(
                    BLACK_MARKET_ID, slot.getItemId(), slot.getDisplayName(),
                    slot.getQuantity(), price, gameTime,
                    false, 0, slot.getItemNbt()
            ));
        }
        return listings;
    }

    public static String getBlackMarketId() {
        return BLACK_MARKET_ID;
    }

    // ==================== Persistence ====================

    /**
     * Save all town inventories to a CompoundTag.
     */
    public static CompoundTag save() {
        CompoundTag tag = new CompoundTag();
        ListTag invList = new ListTag();
        for (TownInventory inv : inventories.values()) {
            invList.add(inv.save());
        }
        tag.put("Inventories", invList);
        tag.putBoolean("BlackMarketActive", blackMarketActive);
        tag.putLong("BlackMarketEnd", blackMarketEndDay);
        return tag;
    }

    /**
     * Load town inventories from a CompoundTag.
     */
    public static void load(CompoundTag tag) {
        inventories.clear();
        if (tag.contains("Inventories")) {
            ListTag invList = tag.getList("Inventories", Tag.TAG_COMPOUND);
            for (int i = 0; i < invList.size(); i++) {
                TownInventory inv = TownInventory.load(invList.getCompound(i));
                inventories.put(inv.getTownId(), inv);
            }
        }
        blackMarketActive = tag.getBoolean("BlackMarketActive");
        blackMarketEndDay = tag.getLong("BlackMarketEnd");
        initialized = !inventories.isEmpty(); // mark initialized if data was loaded
    }

    /**
     * Reset all state (called on server stop/start).
     */
    public static void reset() {
        inventories.clear();
        blackMarketActive = false;
        blackMarketEndDay = -1;
        initialized = false;
    }
}
