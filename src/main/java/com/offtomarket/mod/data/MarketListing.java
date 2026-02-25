package com.offtomarket.mod.data;

import com.offtomarket.mod.item.AnimalTradeSlipItem;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.enchantment.Enchantment;

import java.util.*;

/**
 * Represents a market listing from a town that appears on the Market Board.
 * These are items that towns are currently selling.
 */
public class MarketListing {
    private final String townId;
    private final ResourceLocation itemId;
    private final String itemDisplayName;
    private final int count;
    private final int pricePerItem; // in copper pieces
    private final long listedTime;  // game time when listed
    private final boolean onSale;   // whether this item is on sale
    private final int saleDiscount; // discount percentage (10-30)
    @javax.annotation.Nullable
    private final CompoundTag itemNbt; // optional NBT for special items (enchanted books)

    public MarketListing(String townId, ResourceLocation itemId, String itemDisplayName,
                         int count, int pricePerItem, long listedTime) {
        this(townId, itemId, itemDisplayName, count, pricePerItem, listedTime, false, 0, null);
    }

    public MarketListing(String townId, ResourceLocation itemId, String itemDisplayName,
                         int count, int pricePerItem, long listedTime, boolean onSale, int saleDiscount) {
        this(townId, itemId, itemDisplayName, count, pricePerItem, listedTime, onSale, saleDiscount, null);
    }

    public MarketListing(String townId, ResourceLocation itemId, String itemDisplayName,
                         int count, int pricePerItem, long listedTime, boolean onSale, int saleDiscount,
                         @javax.annotation.Nullable CompoundTag itemNbt) {
        this.townId = townId;
        this.itemId = itemId;
        this.itemDisplayName = itemDisplayName;
        this.count = count;
        this.pricePerItem = pricePerItem;
        this.listedTime = listedTime;
        this.onSale = onSale;
        this.saleDiscount = saleDiscount;
        this.itemNbt = itemNbt;
    }

    public String getTownId() { return townId; }
    public ResourceLocation getItemId() { return itemId; }
    public String getItemDisplayName() { return itemDisplayName; }
    public int getCount() { return count; }
    public int getPricePerItem() { return pricePerItem; }
    public int getTotalPrice() { return pricePerItem * count; }
    public long getListedTime() { return listedTime; }
    public boolean isOnSale() { return onSale; }
    public int getSaleDiscount() { return saleDiscount; }
    @javax.annotation.Nullable
    public CompoundTag getItemNbt() { return itemNbt; }

    /**
     * Create an ItemStack for this listing (applies NBT if present, e.g. enchanted books).
     */
    public net.minecraft.world.item.ItemStack createItemStack(int quantity) {
        net.minecraft.world.item.Item item = net.minecraftforge.registries.ForgeRegistries.ITEMS.getValue(itemId);
        if (item == null) return net.minecraft.world.item.ItemStack.EMPTY;
        net.minecraft.world.item.ItemStack stack = new net.minecraft.world.item.ItemStack(item, quantity);
        if (itemNbt != null) stack.setTag(itemNbt.copy());
        return stack;
    }

    public CompoundTag save() {
        CompoundTag tag = new CompoundTag();
        tag.putString("Town", townId);
        tag.putString("Item", itemId.toString());
        tag.putString("Name", itemDisplayName);
        tag.putInt("Count", count);
        tag.putInt("Price", pricePerItem);
        tag.putLong("Listed", listedTime);
        tag.putBoolean("OnSale", onSale);
        tag.putInt("Discount", saleDiscount);
        if (itemNbt != null) tag.put("ItemNbt", itemNbt.copy());
        return tag;
    }

    public static MarketListing load(CompoundTag tag) {
        return new MarketListing(
                tag.getString("Town"),
                new ResourceLocation(tag.getString("Item")),
                tag.getString("Name"),
                tag.getInt("Count"),
                tag.getInt("Price"),
                tag.getLong("Listed"),
                tag.getBoolean("OnSale"),
                tag.getInt("Discount"),
                tag.contains("ItemNbt") ? tag.getCompound("ItemNbt") : null
        );
    }

    /**
     * Generate random market listings from a town's specialty items.
     */
    public static List<MarketListing> generateListings(TownData town, long gameTime, Random random) {
        List<MarketListing> listings = new ArrayList<>();
        List<ResourceLocation> specialties = new ArrayList<>(town.getSpecialtyItems());

        int count = 4 + random.nextInt(6); // 4-9 listings per town
        Collections.shuffle(specialties, random);

        // Special handling for Peenam Animal Market - add animal slips
        if ("peenam".equals(town.getId())) {
            listings.addAll(generateAnimalSlipListings(town, gameTime, random));
        }

        for (int i = 0; i < Math.min(count, specialties.size()); i++) {
            ResourceLocation itemId = specialties.get(i);
            net.minecraft.world.item.Item item = net.minecraftforge.registries.ForgeRegistries.ITEMS.getValue(itemId);
            if (item != null && item != net.minecraft.world.item.Items.AIR) {
                int qty = 1 + random.nextInt(16);
                CompoundTag nbt = null;
                String displayName;

                // Special handling for enchanted books - give a random enchantment
                if (item instanceof net.minecraft.world.item.EnchantedBookItem) {
                    qty = 1; // always sell 1 enchanted book at a time
                    var enchantData = generateRandomEnchantedBook(random);
                    nbt = enchantData.nbt;
                    displayName = enchantData.displayName;
                } else {
                    displayName = new net.minecraft.world.item.ItemStack(item).getHoverName().getString();
                }

                int basePrice = PriceCalculator.getBaseValue(new net.minecraft.world.item.ItemStack(item));

                // For enchanted books with NBT, adjust price based on enchantment
                if (nbt != null && item instanceof net.minecraft.world.item.EnchantedBookItem) {
                    net.minecraft.world.item.ItemStack tempStack = new net.minecraft.world.item.ItemStack(item);
                    tempStack.setTag(nbt.copy());
                    basePrice = PriceCalculator.getBaseValue(tempStack);
                }

                // Town-aware pricing (distance/type/need-aware), with a protective floor.
                int price = computeTownListingPrice(town, item, basePrice, random);

                // ~20% chance the item is on sale with 10-30% discount
                boolean sale = random.nextFloat() < 0.2f;
                int discount = 0;
                if (sale) {
                    discount = 10 + random.nextInt(21); // 10 to 30
                    int minSalePrice = Math.max(1, (int) Math.floor(basePrice * 0.5));
                    price = Math.max(minSalePrice, (int) (price * (1.0 - discount / 100.0)));
                }

                listings.add(new MarketListing(
                        town.getId(), itemId, displayName,
                        qty, price, gameTime, sale, discount, nbt
                ));
            }
        }

        return listings;
    }

    /**
     * Compute a town-specific listing price for an item.
     *
     * Factors:
     * - Distance premium (farther towns trend more expensive)
     * - Town type bias (cities/outposts/markets generally pricier than villages)
     * - Need level bias (what the town is short on trends pricier)
     * - Small random band for variety
     *
     * Also applies a floor relative to fair value so listings don't collapse to
     * unrealistically low prices in dynamic/modded towns.
     */
    private static int computeTownListingPrice(TownData town, net.minecraft.world.item.Item item,
                                               int basePrice, Random random) {
        NeedLevel need = town.getNeedLevel(item);

        // Distance premium: +0% to +27% across distance 1..10
        double distancePremium = Math.max(0, town.getDistance() - 1) * 0.03;

        // Town role bias
        double typeBias = switch (town.getType()) {
            case VILLAGE -> -0.04;
            case TOWN -> 0.00;
            case MARKET -> 0.03;
            case OUTPOST -> 0.07;
            case CITY -> 0.10;
        };

        // Keep NPC sell listings moderate compared to full buy-price multipliers.
        double needBias = switch (need) {
            case DESPERATE -> 1.20;
            case HIGH_NEED -> 1.12;
            case MODERATE_NEED -> 1.06;
            case BALANCED -> 1.00;
            case SURPLUS -> 0.92;
            case OVERSATURATED -> 0.85;
        };

        // Small market noise for variety
        double randomBand = 0.90 + random.nextDouble() * 0.20; // 0.90 - 1.10

        double adjusted = basePrice * (1.0 + distancePremium + typeBias) * needBias * randomBand;
        int computed = Math.max(1, (int) Math.round(adjusted));

        // Floor by need level (prevents very low prices on modded catalogues).
        double floorFactor = switch (need) {
            case DESPERATE -> 0.90;
            case HIGH_NEED -> 0.88;
            case MODERATE_NEED -> 0.86;
            case BALANCED -> 0.80;
            case SURPLUS -> 0.72;
            case OVERSATURATED -> 0.65;
        };
        int floor = Math.max(1, (int) Math.floor(basePrice * floorFactor));

        return Math.max(floor, computed);
    }

    /**
     * Generate animal slip listings for Peenam Animal Market.
     */
    private static List<MarketListing> generateAnimalSlipListings(TownData town, long gameTime, Random random) {
        List<MarketListing> animalListings = new ArrayList<>();
        
        // List of animals Peenam sells
        List<String> availableAnimals = new ArrayList<>(List.of(
                "minecraft:chicken", "minecraft:pig", "minecraft:sheep", "minecraft:cow",
                "minecraft:rabbit", "minecraft:goat", "minecraft:horse", "minecraft:donkey",
                "minecraft:llama", "minecraft:cat", "minecraft:wolf", "minecraft:parrot",
                "minecraft:bee", "minecraft:fox", "minecraft:axolotl", "minecraft:turtle",
                "minecraft:frog"
        ));
        
        // Rare animals with lower chance
        List<String> rareAnimals = List.of(
                "minecraft:mooshroom", "minecraft:panda", "minecraft:polar_bear",
                "minecraft:mule", "minecraft:camel", "minecraft:allay", "minecraft:sniffer"
        );
        
        // Shuffle and pick 3-6 common animals
        Collections.shuffle(availableAnimals, random);
        int animalCount = 3 + random.nextInt(4);
        
        ResourceLocation slipItemId = new ResourceLocation("offtomarket", "animal_trade_slip");
        
        for (int i = 0; i < Math.min(animalCount, availableAnimals.size()); i++) {
            String animalType = availableAnimals.get(i);
            int baseValue = AnimalTradeSlipItem.getBaseValue(animalType);
            
            // Peenam sells at markup (1.2x - 1.5x base value)
            int price = (int) (baseValue * (1.2 + random.nextDouble() * 0.3));
            
            // Create NBT for the filled animal slip
            CompoundTag nbt = new CompoundTag();
            nbt.putBoolean(AnimalTradeSlipItem.TAG_IS_FILLED, true);
            nbt.putString(AnimalTradeSlipItem.TAG_ANIMAL_TYPE, animalType);
            nbt.putInt(AnimalTradeSlipItem.TAG_BASE_VALUE, baseValue);
            
            String displayName = "Animal Slip - " + AnimalTradeSlipItem.getAnimalDisplayName(animalType);
            
            // Small chance of sale
            boolean sale = random.nextFloat() < 0.15f;
            int discount = sale ? 10 + random.nextInt(16) : 0;
            if (sale) {
                price = Math.max(1, (int) (price * (1.0 - discount / 100.0)));
            }
            
            animalListings.add(new MarketListing(
                    town.getId(), slipItemId, displayName,
                    1, price, gameTime, sale, discount, nbt
            ));
        }
        
        // 30% chance to have one rare animal
        if (random.nextFloat() < 0.3f && !rareAnimals.isEmpty()) {
            String rareAnimal = rareAnimals.get(random.nextInt(rareAnimals.size()));
            int baseValue = AnimalTradeSlipItem.getBaseValue(rareAnimal);
            
            // Rare animals at premium (1.5x - 2.0x)
            int price = (int) (baseValue * (1.5 + random.nextDouble() * 0.5));
            
            CompoundTag nbt = new CompoundTag();
            nbt.putBoolean(AnimalTradeSlipItem.TAG_IS_FILLED, true);
            nbt.putString(AnimalTradeSlipItem.TAG_ANIMAL_TYPE, rareAnimal);
            nbt.putInt(AnimalTradeSlipItem.TAG_BASE_VALUE, baseValue);
            
            String displayName = "Animal Slip - " + AnimalTradeSlipItem.getAnimalDisplayName(rareAnimal) + " (Rare!)";
            
            animalListings.add(new MarketListing(
                    town.getId(), slipItemId, displayName,
                    1, price, gameTime, false, 0, nbt
            ));
        }
        
        return animalListings;
    }

    /**
     * Data holder for a randomly generated enchanted book.
     */
    private static class EnchantBookData {
        final CompoundTag nbt;
        final String displayName;
        EnchantBookData(CompoundTag nbt, String displayName) {
            this.nbt = nbt;
            this.displayName = displayName;
        }
    }

    /**
     * Generate a random enchanted book with a single enchantment at a random valid level.
     */
    private static EnchantBookData generateRandomEnchantedBook(Random random) {
        // Get all registered enchantments
        List<Enchantment> allEnchants = new ArrayList<>();
        for (Enchantment ench : net.minecraftforge.registries.ForgeRegistries.ENCHANTMENTS) {
            if (ench.isDiscoverable()) {
                allEnchants.add(ench);
            }
        }

        if (allEnchants.isEmpty()) {
            // Fallback: return a plain enchanted book
            return new EnchantBookData(null, "Enchanted Book");
        }

        Enchantment chosen = allEnchants.get(random.nextInt(allEnchants.size()));
        int level = 1 + random.nextInt(chosen.getMaxLevel());

        // Build the ItemStack with the stored enchantment
        net.minecraft.world.item.ItemStack bookStack = new net.minecraft.world.item.ItemStack(
                net.minecraft.world.item.Items.ENCHANTED_BOOK);
        net.minecraft.world.item.EnchantedBookItem.addEnchantment(bookStack,
                new net.minecraft.world.item.enchantment.EnchantmentInstance(chosen, level));

        // Build display name
        String enchName = net.minecraft.network.chat.Component.translatable(
                chosen.getDescriptionId()).getString();
        String displayName = "Enchanted Book: " + enchName;
        if (chosen.getMaxLevel() > 1) {
            displayName += " " + toRoman(level);
        }

        return new EnchantBookData(bookStack.getTag(), displayName);
    }

    private static String toRoman(int num) {
        return switch (num) {
            case 1 -> "I";
            case 2 -> "II";
            case 3 -> "III";
            case 4 -> "IV";
            case 5 -> "V";
            default -> String.valueOf(num);
        };
    }
}
