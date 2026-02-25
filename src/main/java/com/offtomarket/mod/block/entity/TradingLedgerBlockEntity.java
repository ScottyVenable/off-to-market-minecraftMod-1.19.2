package com.offtomarket.mod.block.entity;

import com.offtomarket.mod.data.PriceCalculator;
import com.offtomarket.mod.menu.TradingLedgerMenu;
import com.offtomarket.mod.registry.ModBlockEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.NonNullList;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.world.Container;
import net.minecraft.world.ContainerHelper;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.registries.ForgeRegistries;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * The Trading Bin holds items the player wants to sell/ship.
 * Players can set prices per slot. When items are shipped,
 * they're removed and replaced with a shipment note.
 *
 * 9 slots for items, plus the note appears in slot 0 after pickup.
 */
public class TradingLedgerBlockEntity extends BlockEntity implements Container, MenuProvider {
    public static final int BIN_SIZE = 200;
    public static final int MAX_STACK_PER_SLOT = 200;
    public static final int BASE_CARAVAN_WEIGHT_CAPACITY = 800;
    public static final int CARAVAN_WEIGHT_PER_UPGRADE = 200;
    /** @deprecated Inspection slot removed — pricing now done via list selection. */
    @Deprecated public static final int INSPECT_SLOT = -1;
    public static final int TOTAL_SIZE = BIN_SIZE;

    private NonNullList<ItemStack> items = NonNullList.withSize(BIN_SIZE, ItemStack.EMPTY);

    // Prices set by the player for each slot (in copper pieces, 0 = use default)
    private final Map<Integer, Integer> slotPrices = new HashMap<>();

    // Remembers the last price set for each item type (persists across shipments)
    private final Map<String, Integer> priceMemory = new HashMap<>();

    // Countdown for items to be "picked up" after send command
    private int pickupTimer = -1;
    private boolean awaitingPickup = false;

    // Adjacent-container auto-sync
    /** How often (in ticks) the bin pulls items from neighbouring containers (~3 s). */
    public static final int SYNC_INTERVAL_TICKS = 60;
    /** Counts down to the next auto-sync; starts at 0 so the first sync happens quickly. */
    private int syncCooldown = 0;
    /**
     * Containers suppressed from auto-sync after a "To Container" withdrawal.
     * Key = container BlockPos, Value = game tick when suppression expires.
     * Not persisted — intentional; re-sync on reload is fine.
     */
    private final Map<BlockPos, Long> withdrawSuppressedPos = new HashMap<>();

    // ==================== Settings ====================

    /** Auto-price mode for newly placed items. */
    public enum AutoPriceMode {
        MANUAL("Manual"),        // Player sets all prices
        AUTO_FAIR("Auto Fair"),  // Auto-set to fair value
        AUTO_MARKUP("Auto+Markup"); // Auto-set to fair value + markup

        private final String displayName;
        AutoPriceMode(String displayName) { this.displayName = displayName; }
        public String getDisplayName() { return displayName; }
    }

    /**
     * Records the origin of a "virtual" ledger slot — an item that lives in
     * an adjacent container and is shown read-only in the ledger until shipped.
     */
    public static final class VirtualSource {
        public final BlockPos sourcePos;
        public final int sourceSlot;

        public VirtualSource(BlockPos sourcePos, int sourceSlot) {
            this.sourcePos = sourcePos;
            this.sourceSlot = sourceSlot;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (!(o instanceof VirtualSource vs)) return false;
            return sourceSlot == vs.sourceSlot && sourcePos.equals(vs.sourcePos);
        }

        @Override
        public int hashCode() {
            return 31 * sourcePos.hashCode() + sourceSlot;
        }
    }

    /**
     * Maps ledger slot index to its source container location for virtual (read-only) slots.
     * Not persisted — rebuilt on next auto-sync tick after world load.
     */
    private final Map<Integer, VirtualSource> slotToVirtualSource = new HashMap<>();

    /**
     * Client-side mirror of virtual slot indices, populated via NBT sync.
     * On the server, query slotToVirtualSource directly.
     */
    private final Set<Integer> clientVirtualSlots = new HashSet<>();

    // ==================== Shipment History ====================

    /** Records a single shipping event for the Past Orders tab. */
    public static final class LedgerShipmentRecord {
        public final long gameTime;
        public final String townDisplayName;
        public final int totalItems;
        public final int totalValue; // copper pieces

        public LedgerShipmentRecord(long gameTime, String townDisplayName, int totalItems, int totalValue) {
            this.gameTime = gameTime;
            this.townDisplayName = townDisplayName;
            this.totalItems = totalItems;
            this.totalValue = totalValue;
        }

        public CompoundTag save() {
            CompoundTag tag = new CompoundTag();
            tag.putLong("GameTime", gameTime);
            tag.putString("Town", townDisplayName);
            tag.putInt("Items", totalItems);
            tag.putInt("Value", totalValue);
            return tag;
        }

        public static LedgerShipmentRecord load(CompoundTag tag) {
            return new LedgerShipmentRecord(
                    tag.getLong("GameTime"),
                    tag.getString("Town"),
                    tag.getInt("Items"),
                    tag.getInt("Value"));
        }
    }

    /** Max entries kept in the history list. */
    private static final int MAX_HISTORY_ENTRIES = 20;
    private final List<LedgerShipmentRecord> shipmentHistory = new ArrayList<>();

    /**
     * Record a shipment event. Call from TradingPostBlockEntity before clearing the bin.
     */
    public void recordShipment(long gameTime, String townDisplayName, int totalItems, int totalValue) {
        shipmentHistory.add(0, new LedgerShipmentRecord(gameTime, townDisplayName, totalItems, totalValue));
        if (shipmentHistory.size() > MAX_HISTORY_ENTRIES) {
            shipmentHistory.subList(MAX_HISTORY_ENTRIES, shipmentHistory.size()).clear();
        }
        setChanged();
        syncToClient();
    }

    public List<LedgerShipmentRecord> getShipmentHistory() {
        return java.util.Collections.unmodifiableList(shipmentHistory);
    }

    private int craftingTaxPercent = 15;   // extra % for crafted items
    private int minMarkupPercent = 0;      // minimum markup % over fair value
    private AutoPriceMode autoPriceMode = AutoPriceMode.AUTO_FAIR;

    // ==================== Price Modifiers ====================
    /** Whether to apply enchantment markup when pricing enchanted items. */
    private boolean enchantedMarkupEnabled = true;
    /** Percent markup for enchanted items (default 50%). */
    private int enchantedMarkupPercent = 50;

    /** Whether to apply a discount for used (partially damaged) items. */
    private boolean usedDiscountEnabled = true;
    /** Percent discount for used items (default 20%). */
    private int usedDiscountPercent = 20;

    /** Whether to apply a larger discount for heavily damaged items. */
    private boolean damagedDiscountEnabled = true;
    /** Percent discount for damaged items below 50% durability (default 40%). */
    private int damagedDiscountPercent = 40;

    /** Whether to apply a rarity markup. */
    private boolean rareMarkupEnabled = true;
    /** Percent markup for rare+ rarity items (default 30%). */
    private int rareMarkupPercent = 30;
    /** Caravan weight upgrade level purchased from Fees tab. */
    private int caravanWeightUpgradeLevel = 0;

    public TradingLedgerBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.TRADING_LEDGER.get(), pos, state);
    }

    // ==================== Price Management ====================

    public int getSetPrice(int slot) {
        return slotPrices.getOrDefault(slot, 0);
    }

    public void setPrice(int slot, int price) {
        if (slot >= 0 && slot < BIN_SIZE) {
            if (price <= 0) {
                slotPrices.remove(slot);
            } else {
                slotPrices.put(slot, price);
                // Remember this price for the item type
                ItemStack stack = getItem(slot);
                if (!stack.isEmpty()) {
                    String itemKey = getItemKey(stack);
                    if (itemKey != null) {
                        priceMemory.put(itemKey, price);
                    }
                }
            }
        }
        syncToClient();
    }

    /**
     * Get the remembered price for an item type, or 0 if none.
     */
    public int getRememberedPrice(ItemStack stack) {
        if (stack.isEmpty()) return 0;
        String key = getItemKey(stack);
        return key != null ? priceMemory.getOrDefault(key, 0) : 0;
    }

    /**
     * Get a stable string key for an item (registry name).
     */
    private static String getItemKey(ItemStack stack) {
        var rl = ForgeRegistries.ITEMS.getKey(stack.getItem());
        return rl != null ? rl.toString() : null;
    }

    // ==================== Settings Accessors ====================

    public int getCraftingTaxPercent() { return craftingTaxPercent; }
    public void setCraftingTaxPercent(int percent) {
        this.craftingTaxPercent = Math.max(0, Math.min(100, percent));
        syncToClient();
    }

    public int getMinMarkupPercent() { return minMarkupPercent; }
    public void setMinMarkupPercent(int percent) {
        this.minMarkupPercent = Math.max(0, Math.min(200, percent));
        syncToClient();
    }

    public AutoPriceMode getAutoPriceMode() { return autoPriceMode; }
    public void setAutoPriceMode(AutoPriceMode mode) {
        this.autoPriceMode = mode;
        syncToClient();
    }

    // ==================== Price Modifier Accessors ====================

    public boolean isEnchantedMarkupEnabled() { return enchantedMarkupEnabled; }
    public void setEnchantedMarkupEnabled(boolean enabled) { this.enchantedMarkupEnabled = enabled; syncToClient(); }
    public int getEnchantedMarkupPercent() { return enchantedMarkupPercent; }
    public void setEnchantedMarkupPercent(int percent) { this.enchantedMarkupPercent = Math.max(0, Math.min(200, percent)); syncToClient(); }

    public boolean isUsedDiscountEnabled() { return usedDiscountEnabled; }
    public void setUsedDiscountEnabled(boolean enabled) { this.usedDiscountEnabled = enabled; syncToClient(); }
    public int getUsedDiscountPercent() { return usedDiscountPercent; }
    public void setUsedDiscountPercent(int percent) { this.usedDiscountPercent = Math.max(0, Math.min(100, percent)); syncToClient(); }

    public boolean isDamagedDiscountEnabled() { return damagedDiscountEnabled; }
    public void setDamagedDiscountEnabled(boolean enabled) { this.damagedDiscountEnabled = enabled; syncToClient(); }
    public int getDamagedDiscountPercent() { return damagedDiscountPercent; }
    public void setDamagedDiscountPercent(int percent) { this.damagedDiscountPercent = Math.max(0, Math.min(100, percent)); syncToClient(); }

    public boolean isRareMarkupEnabled() { return rareMarkupEnabled; }
    public void setRareMarkupEnabled(boolean enabled) { this.rareMarkupEnabled = enabled; syncToClient(); }
    public int getRareMarkupPercent() { return rareMarkupPercent; }
    public void setRareMarkupPercent(int percent) { this.rareMarkupPercent = Math.max(0, Math.min(200, percent)); syncToClient(); }
    public int getCaravanWeightUpgradeLevel() { return caravanWeightUpgradeLevel; }

    public int getCaravanWeightCapacity() {
        return BASE_CARAVAN_WEIGHT_CAPACITY + caravanWeightUpgradeLevel * CARAVAN_WEIGHT_PER_UPGRADE;
    }

    public int getNextCaravanUpgradeCost() {
        return 250 + (caravanWeightUpgradeLevel * 175);
    }

    public int getCurrentCaravanWeight() {
        int weight = 0;
        for (int i = 0; i < BIN_SIZE; i++) {
            ItemStack stack = items.get(i);
            if (!stack.isEmpty()) {
                weight += stack.getCount() * getItemWeightPerUnit(stack);
            }
        }
        return weight;
    }

    public int getRemainingCaravanWeight() {
        return Math.max(0, getCaravanWeightCapacity() - getCurrentCaravanWeight());
    }

    public static int getItemWeightPerUnit(ItemStack stack) {
        if (stack.isEmpty()) return 1;
        int base = PriceCalculator.getBaseValue(stack);
        return Math.max(1, base / 50);
    }

    public boolean upgradeCaravanWeight(Player player) {
        int cost = getNextCaravanUpgradeCost();
        if (!TradingPostBlockEntity.hasEnoughCoins(player, cost)) {
            return false;
        }
        TradingPostBlockEntity.deductCoins(player, cost);
        caravanWeightUpgradeLevel++;
        syncToClient();
        return true;
    }

    // ==================== Modifier Validation ====================

    /**
     * Check if the enchantment modifier is applicable to this item.
     * Only returns true if the item is actually enchanted.
     */
    public static boolean isEnchantmentApplicable(ItemStack stack) {
        return !stack.isEmpty() && stack.isEnchanted();
    }

    /**
     * Check if the rarity modifier is applicable to this item.
     * Only RARE and EPIC trigger the rarity markup (UNCOMMON does not).
     */
    public static boolean isRarityApplicable(ItemStack stack) {
        if (stack.isEmpty()) return false;
        net.minecraft.world.item.Rarity rarity = stack.getRarity();
        return rarity == net.minecraft.world.item.Rarity.RARE || rarity == net.minecraft.world.item.Rarity.EPIC;
    }

    /**
     * Check if the damaged/used discount might apply (item is damageable and currently damaged).
     */
    public static boolean isDurabilityApplicable(ItemStack stack) {
        return !stack.isEmpty() && stack.isDamageableItem() && stack.isDamaged();
    }

    /**
     * Check if the item is heavily damaged (&lt;50% durability).
     */
    public static boolean isHeavilyDamaged(ItemStack stack) {
        if (!isDurabilityApplicable(stack)) return false;
        double ratio = 1.0 - (double) stack.getDamageValue() / stack.getMaxDamage();
        return ratio < 0.5;
    }

    /**
     * Calculate the modified price for an item based on enabled price modifiers.
     * Applies enchanted markup, used/damaged discounts, and rarity markup.
     *
     * @param stack the item to price
     * @param basePrice the base fair value in CP
     * @return the modified price in CP
     */
    public int applyPriceModifiers(ItemStack stack, int basePrice) {
        if (stack.isEmpty() || basePrice <= 0) return basePrice;
        double price = basePrice;

        // Enchantment markup — auto-apply when item is actually enchanted
        if (isEnchantmentApplicable(stack)) {
            price *= (1.0 + enchantedMarkupPercent / 100.0);
        }

        // Rarity markup — auto-apply when RARE or EPIC
        if (isRarityApplicable(stack)) {
            price *= (1.0 + rareMarkupPercent / 100.0);
        }

        // Durability-based discounts — auto-apply for damageable, damaged items
        if (isDurabilityApplicable(stack)) {
            if (isHeavilyDamaged(stack)) {
                // Heavily damaged: apply damaged discount
                price *= (1.0 - damagedDiscountPercent / 100.0);
            } else {
                // Partially used: apply used discount
                price *= (1.0 - usedDiscountPercent / 100.0);
            }
        }

        return Math.max(1, (int) price);
    }

    /**
     * Auto-price all occupied slots using PriceCalculator base value.
     * Slots with a manually set price (rawPrice > 0) are left unchanged.
     * Called when the player presses Apply in the Trading Bin settings panel.
     */
    public void applyAutoPricingToAllSlots() {
        boolean changed = false;
        for (int i = 0; i < BIN_SIZE; i++) {
            ItemStack stack = getItem(i);
            if (!stack.isEmpty() && getRawPriceForSlot(i) <= 0) {
                int fair = PriceCalculator.getBaseValue(stack);
                if (fair > 0) {
                    slotPrices.put(i, fair);
                    changed = true;
                }
            }
        }
        if (changed) {
            setChanged();
        }
    }

    /**
     * Price currently configured in the slot (0 means default/fair value).
     */
    public int getRawPriceForSlot(int slot) {
        return slotPrices.getOrDefault(slot, 0);
    }

    /**
     * Universal final pricing pipeline used by list display and shipment dispatch.
     */
    public int getEffectivePrice(ItemStack stack, int rawPrice) {
        if (stack.isEmpty()) return 0;
        int base = rawPrice > 0 ? rawPrice : PriceCalculator.getBaseValue(stack);
        if (base <= 0) base = 1;

        int modifiedValue = applyPriceModifiers(stack, base);
        int tax = (int) (modifiedValue * (craftingTaxPercent / 100.0));
        int subtotal = modifiedValue + tax;
        int markup = (int) (subtotal * (minMarkupPercent / 100.0));
        return Math.max(1, subtotal + markup);
    }

    public int getEffectivePriceForSlot(int slot) {
        ItemStack stack = getItem(slot);
        if (stack.isEmpty()) return 0;
        return getEffectivePrice(stack, getRawPriceForSlot(slot));
    }

    public int getTotalProposedPayout() {
        long total = 0;
        for (int i = 0; i < BIN_SIZE; i++) {
            ItemStack stack = getItem(i);
            if (stack.isEmpty()) continue;
            total += (long) getEffectivePriceForSlot(i) * stack.getCount();
        }
        return (int) Math.min(Integer.MAX_VALUE, total);
    }

    /**
     * Get the estimated market time label for a given price.
     * Based on how the price compares to fair value.
     */
    public static String getEstimatedMarketTime(int price, int fairValue, int maxPrice) {
        if (price <= 0 || fairValue <= 0) return "—";
        double ratio = (double) price / fairValue;
        if (ratio <= 0.7) return "§aInstant";
        if (ratio <= 1.0) return "§a< 1 min";
        if (ratio <= 1.3) return "§e1-3 min";
        if (ratio <= 1.6) return "§63-5 min";
        if (ratio <= 2.0) return "§c5-8 min";
        if (price > maxPrice) return "§4Won't sell";
        return "§c6+ min";
    }

    /**
     * Cycle to the next auto-price mode.
     */
    public void cycleAutoPriceMode() {
        AutoPriceMode[] modes = AutoPriceMode.values();
        this.autoPriceMode = modes[(this.autoPriceMode.ordinal() + 1) % modes.length];
        syncToClient();
    }

    // ==================== Pickup/Shipping ====================

    public void startPickupCountdown(int ticks) {
        this.pickupTimer = ticks;
        this.awaitingPickup = true;
        setChanged();
    }

    public boolean isAwaitingPickup() {
        return awaitingPickup;
    }

    public int getPickupTimer() {
        return pickupTimer;
    }

    /**
     * Clear all items and optionally leave a note in slot 0.
     */
    public void clearAndLeaveNote(ItemStack note) {
        items.clear();
        items = NonNullList.withSize(BIN_SIZE, ItemStack.EMPTY);
        if (note != null && !note.isEmpty()) {
            items.set(0, note);
        }
        slotPrices.clear();
        slotToVirtualSource.clear();
        awaitingPickup = false;
        pickupTimer = -1;
        syncToClient();
    }

    /**
     * Add as many items as possible into the bin, respecting stack and caravan weight caps.
     * @return amount inserted
     */
    public int addItemAmount(ItemStack stack) {
        if (stack.isEmpty() || stack.getItem() == net.minecraft.world.item.Items.AIR) return 0;

        int weightPerUnit = getItemWeightPerUnit(stack);
        int maxByWeight = getRemainingCaravanWeight() / weightPerUnit;
        if (maxByWeight <= 0) return 0;

        int toInsert = Math.min(stack.getCount(), maxByWeight);
        int inserted = 0;

        // 1) Merge into existing compatible stacks
        for (int i = 0; i < BIN_SIZE && inserted < toInsert; i++) {
            ItemStack existing = items.get(i);
            if (existing.isEmpty()) continue;
            if (!ItemStack.isSameItemSameTags(existing, stack)) continue;

            int room = Math.max(0, MAX_STACK_PER_SLOT - existing.getCount());
            if (room <= 0) continue;

            int add = Math.min(room, toInsert - inserted);
            existing.grow(add);
            inserted += add;
        }

        // 2) Fill empty slots
        for (int i = 0; i < BIN_SIZE && inserted < toInsert; i++) {
            ItemStack existing = items.get(i);
            if (!existing.isEmpty()) continue;

            int add = Math.min(MAX_STACK_PER_SLOT, toInsert - inserted);
            ItemStack placed = stack.copy();
            placed.setCount(add);
            items.set(i, placed);

            // Auto-apply / remember default pricing on newly created slot
            if (getSetPrice(i) <= 0) {
                int autoPrice = computeAutoPrice(placed);
                if (autoPrice > 0) {
                    slotPrices.put(i, autoPrice);
                } else {
                    int remembered = getRememberedPrice(placed);
                    if (remembered > 0) {
                        slotPrices.put(i, remembered);
                    }
                }
            }
            inserted += add;
        }

        if (inserted > 0) {
            setChanged();
            syncToClient();
        }
        return inserted;
    }

    /**
     * Add an item stack and return true only if the entire stack was inserted.
     */
    public boolean addItem(ItemStack stack) {
        return addItemAmount(stack) >= stack.getCount();
    }

    public NonNullList<ItemStack> getItems() {
        return items;
    }

    public static void serverTick(Level level, BlockPos pos, BlockState state,
                                   TradingLedgerBlockEntity be) {
        if (be.awaitingPickup && be.pickupTimer > 0) {
            be.pickupTimer--;
            if (be.pickupTimer <= 0) {
                // Timer expired - items should have been picked up by the Trading Post
                be.awaitingPickup = false;
                be.setChanged();
            }
        }

        // Periodically pull items from adjacent containers into the bin
        be.syncCooldown--;
        if (be.syncCooldown <= 0) {
            be.syncCooldown = SYNC_INTERVAL_TICKS;
            be.syncFromAdjacentContainers();
        }
    }

    /**
     * Scan all six neighbouring positions and snapshot every item from any found
     * Container block entity (excluding other Trading Ledgers) as virtual slots.
     * Items are NOT physically moved — they stay in the source container and are
     * only removed when the shipment is sent or the player withdraws them.
     * Skips containers that were recently used as "To Container" withdrawal targets.
     */
    private void syncFromAdjacentContainers() {
        Level level = this.getLevel();
        if (level == null || level.isClientSide()) return;
        BlockPos pos = this.getBlockPos();
        long now = level.getGameTime();

        // Clean up expired suppression entries
        withdrawSuppressedPos.entrySet().removeIf(e -> e.getValue() <= now);

        // Track which ledger slots were refreshed this cycle
        List<Integer> updatedLedgerSlots = new ArrayList<>();
        boolean changed = false;

        for (Direction dir : Direction.values()) {
            BlockPos adjPos = pos.relative(dir);
            BlockEntity adjBe = level.getBlockEntity(adjPos);
            if (!(adjBe instanceof Container container)) continue;
            if (adjBe instanceof TradingLedgerBlockEntity) continue;

            // Skip containers suppressed by recent "To Container" withdrawal
            if (withdrawSuppressedPos.getOrDefault(adjPos, 0L) > now) continue;

            for (int cSlot = 0; cSlot < container.getContainerSize(); cSlot++) {
                ItemStack stack = container.getItem(cSlot);
                if (stack.isEmpty()) continue;

                // Find existing virtual ledger slot for this (sourcePos, sourceSlot) pair
                Integer existingLedgerSlot = findVirtualLedgerSlot(adjPos, cSlot);

                if (existingLedgerSlot != null) {
                    // Refresh snapshot — count or NBT may have changed in source container
                    items.set(existingLedgerSlot, stack.copy());
                    updatedLedgerSlots.add(existingLedgerSlot);
                    changed = true; // always sync to keep client display current
                } else {
                    // Allocate a free ledger slot for this new virtual item
                    int freeSlot = findFreeLedgerSlot();
                    if (freeSlot < 0) continue; // ledger display full

                    ItemStack snapshot = stack.copy();
                    items.set(freeSlot, snapshot);
                    slotToVirtualSource.put(freeSlot, new VirtualSource(adjPos, cSlot));

                    // Auto-price the new virtual slot
                    if (getSetPrice(freeSlot) <= 0) {
                        int autoPrice = computeAutoPrice(snapshot);
                        if (autoPrice > 0) {
                            slotPrices.put(freeSlot, autoPrice);
                        } else {
                            int remembered = getRememberedPrice(snapshot);
                            if (remembered > 0) slotPrices.put(freeSlot, remembered);
                        }
                    }
                    updatedLedgerSlots.add(freeSlot);
                    changed = true;
                }
            }
        }

        // Remove stale virtual slots whose source container slot is now empty/moved
        List<Integer> staleSlots = new ArrayList<>();
        for (Map.Entry<Integer, VirtualSource> entry : slotToVirtualSource.entrySet()) {
            if (!updatedLedgerSlots.contains(entry.getKey())) {
                staleSlots.add(entry.getKey());
            }
        }
        for (int staleSlot : staleSlots) {
            items.set(staleSlot, ItemStack.EMPTY);
            slotPrices.remove(staleSlot);
            slotToVirtualSource.remove(staleSlot);
            changed = true;
        }

        if (changed) {
            setChanged();
            syncToClient();
        }
    }

    /** Returns the VirtualSource for this ledger slot, or null if it is a real (owned) slot. */
    @Nullable
    public VirtualSource getVirtualSource(int slot) {
        return slotToVirtualSource.get(slot);
    }

    /** Returns true if this slot contains a snapshot of an item owned by an adjacent container. */
    public boolean isVirtualSlot(int slot) {
        Level lvl = getLevel();
        if (lvl != null && lvl.isClientSide()) {
            // Client side: use the set synced from server via NBT
            return clientVirtualSlots.contains(slot);
        }
        // Server: check live tick-populated map first, then fall back to the NBT-loaded set
        // (clientVirtualSlots is populated from NBT in load() on both sides, so it is reliable
        // during block break even before the first server-tick scan has run).
        return slotToVirtualSource.containsKey(slot) || clientVirtualSlots.contains(slot);
    }

    /**
     * Find the ledger slot index already assigned to the given (sourcePos, sourceContainerSlot),
     * or null if no assignment exists yet.
     */
    @Nullable
    private Integer findVirtualLedgerSlot(BlockPos sourcePos, int sourceContainerSlot) {
        for (Map.Entry<Integer, VirtualSource> e : slotToVirtualSource.entrySet()) {
            VirtualSource vs = e.getValue();
            if (vs.sourceSlot == sourceContainerSlot && vs.sourcePos.equals(sourcePos)) {
                return e.getKey();
            }
        }
        return null;
    }

    /** Find the first items[] slot that is completely empty (not occupied by real or virtual item). */
    private int findFreeLedgerSlot() {
        for (int i = 0; i < BIN_SIZE; i++) {
            if (items.get(i).isEmpty()) return i;
        }
        return -1;
    }

    /**
     * Suppresses auto-syncing from the given adjacent container for the specified
     * number of ticks. Call this after a "To Container" action so the item
     * isn't immediately re-imported as a virtual slot.
     */
    public void suppressContainerSync(BlockPos containerPos, int ticks) {
        Level level = getLevel();
        if (level == null) return;
        withdrawSuppressedPos.put(containerPos, level.getGameTime() + ticks);
    }

    /**
     * Remove the virtual tracking for a slot without touching the source container.
     * Used when the item is left in the source container intentionally (e.g. "Stop Tracking").
     */
    public void removeVirtualTracking(int slot) {
        if (!isVirtualSlot(slot)) return;
        // Get source pos for suppression before clearing
        VirtualSource vs = slotToVirtualSource.get(slot);
        items.set(slot, ItemStack.EMPTY);
        slotPrices.remove(slot);
        slotToVirtualSource.remove(slot);
        if (vs != null) {
            // Suppress re-syncing this source container for several cycles
            suppressContainerSync(vs.sourcePos, 5 * SYNC_INTERVAL_TICKS);
        }
        setChanged();
        syncToClient();
    }

    // ==================== Container ====================

    @Override
    public int getContainerSize() {
        return TOTAL_SIZE;
    }

    @Override
    public boolean isEmpty() {
        for (int i = 0; i < BIN_SIZE; i++) {
            if (!items.get(i).isEmpty()) return false;
        }
        return true;
    }

    @Override
    public ItemStack getItem(int slot) {
        if (slot < 0 || slot >= BIN_SIZE) return ItemStack.EMPTY;
        return items.get(slot);
    }

    @Override
    public ItemStack removeItem(int slot, int amount) {
        if (slot < 0 || slot >= BIN_SIZE) return ItemStack.EMPTY;
        ItemStack result = ContainerHelper.removeItem(items, slot, amount);
        if (!result.isEmpty()) setChanged();
        return result;
    }

    @Override
    public ItemStack removeItemNoUpdate(int slot) {
        if (slot < 0 || slot >= BIN_SIZE) return ItemStack.EMPTY;
        return ContainerHelper.takeItem(items, slot);
    }

    @Override
    public void setItem(int slot, ItemStack stack) {
        if (slot < 0 || slot >= BIN_SIZE) return;
        items.set(slot, stack);
        if (stack.getCount() > getMaxStackSize()) {
            stack.setCount(getMaxStackSize());
        }
        // Remove virtual tracking when a slot is cleared
        if (stack.isEmpty()) {
            slotToVirtualSource.remove(slot);
        }
        // Auto-apply price based on AutoPriceMode
        if (!stack.isEmpty() && getSetPrice(slot) <= 0) {
            int autoPrice = computeAutoPrice(stack);
            if (autoPrice > 0) {
                slotPrices.put(slot, autoPrice);
            } else {
                // Fallback: try remembered price
                int remembered = getRememberedPrice(stack);
                if (remembered > 0) {
                    slotPrices.put(slot, remembered);
                }
            }
        }
        setChanged();
    }

    @Override
    public int getMaxStackSize() {
        return MAX_STACK_PER_SLOT;
    }

    /**
     * Compute the auto-set price for an item based on the current AutoPriceMode.
     * Returns 0 if mode is MANUAL (no auto-pricing).
     */
    private int computeAutoPrice(ItemStack stack) {
        if (autoPriceMode == AutoPriceMode.MANUAL) {
            return 0; // Manual mode — no auto-pricing
        }
        int baseValue = PriceCalculator.getBaseValue(stack);
        if (baseValue <= 0) baseValue = 1;

        // Apply item-specific modifiers (enchantment markup, durability discount, etc.)
        int modifiedValue = applyPriceModifiers(stack, baseValue);

        if (autoPriceMode == AutoPriceMode.AUTO_FAIR) {
            // Modified value + crafting tax
            int tax = (int) (modifiedValue * (craftingTaxPercent / 100.0));
            return modifiedValue + tax;
        } else if (autoPriceMode == AutoPriceMode.AUTO_MARKUP) {
            // Modified value + crafting tax + minimum markup
            int tax = (int) (modifiedValue * (craftingTaxPercent / 100.0));
            int subtotal = modifiedValue + tax;
            int markup = (int) (subtotal * (minMarkupPercent / 100.0));
            return subtotal + markup;
        }
        return 0;
    }

    @Override
    public boolean stillValid(Player player) {
        if (this.level.getBlockEntity(this.worldPosition) != this) return false;
        return player.distanceToSqr(this.worldPosition.getX() + 0.5,
                this.worldPosition.getY() + 0.5,
                this.worldPosition.getZ() + 0.5) <= 64.0;
    }

    @Override
    public void clearContent() {
        items.clear();
    }

    // ==================== Client Sync ====================

    public void syncToClient() {
        setChanged();
        if (level != null && !level.isClientSide()) {
            level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), 3);
        }
    }

    @Override
    public CompoundTag getUpdateTag() {
        CompoundTag tag = super.getUpdateTag();
        saveAdditional(tag);
        return tag;
    }

    @Override
    public Packet<ClientGamePacketListener> getUpdatePacket() {
        return ClientboundBlockEntityDataPacket.create(this);
    }

    // ==================== Menu Provider ====================

    @Override
    public Component getDisplayName() {
        return Component.translatable("block.offtomarket.trading_ledger");
    }

    @Nullable
    @Override
    public AbstractContainerMenu createMenu(int containerId, Inventory inv, Player player) {
        return new TradingLedgerMenu(containerId, inv, this);
    }

    // ==================== NBT ====================

    @Override
    protected void saveAdditional(CompoundTag tag) {
        super.saveAdditional(tag);
        ContainerHelper.saveAllItems(tag, items);

        CompoundTag pricesTag = new CompoundTag();
        for (Map.Entry<Integer, Integer> entry : slotPrices.entrySet()) {
            pricesTag.putInt("Slot" + entry.getKey(), entry.getValue());
        }
        tag.put("Prices", pricesTag);

        CompoundTag memoryTag = new CompoundTag();
        for (Map.Entry<String, Integer> entry : priceMemory.entrySet()) {
            memoryTag.putInt(entry.getKey(), entry.getValue());
        }
        tag.put("PriceMemory", memoryTag);

        tag.putInt("PickupTimer", pickupTimer);
        tag.putBoolean("AwaitingPickup", awaitingPickup);

        // Settings
        tag.putInt("CraftingTaxPercent", craftingTaxPercent);
        tag.putInt("MinMarkupPercent", minMarkupPercent);
        tag.putString("AutoPriceMode", autoPriceMode.name());

        // Price modifiers
        tag.putBoolean("EnchantedMarkupEnabled", enchantedMarkupEnabled);
        tag.putInt("EnchantedMarkupPercent", enchantedMarkupPercent);
        tag.putBoolean("UsedDiscountEnabled", usedDiscountEnabled);
        tag.putInt("UsedDiscountPercent", usedDiscountPercent);
        tag.putBoolean("DamagedDiscountEnabled", damagedDiscountEnabled);
        tag.putInt("DamagedDiscountPercent", damagedDiscountPercent);
        tag.putBoolean("RareMarkupEnabled", rareMarkupEnabled);
        tag.putInt("RareMarkupPercent", rareMarkupPercent);
        tag.putInt("CaravanWeightUpgradeLevel", caravanWeightUpgradeLevel);

        // Virtual slot indices for client-side display
        int[] virtualIndices = new int[slotToVirtualSource.size()];
        int vi = 0;
        for (int idx : slotToVirtualSource.keySet()) virtualIndices[vi++] = idx;
        tag.putIntArray("VirtualSlots", virtualIndices);

        // Shipment history (most recent first)
        ListTag historyTag = new ListTag();
        for (LedgerShipmentRecord rec : shipmentHistory) historyTag.add(rec.save());
        tag.put("ShipmentHistory", historyTag);

    }

    @Override
    public void load(CompoundTag tag) {
        super.load(tag);
        items = NonNullList.withSize(BIN_SIZE, ItemStack.EMPTY);
        ContainerHelper.loadAllItems(tag, items);

        slotPrices.clear();
        if (tag.contains("Prices")) {
            CompoundTag pricesTag = tag.getCompound("Prices");
            for (int i = 0; i < BIN_SIZE; i++) {
                String key = "Slot" + i;
                if (pricesTag.contains(key)) {
                    slotPrices.put(i, pricesTag.getInt(key));
                }
            }
        }
        pickupTimer = tag.getInt("PickupTimer");
        awaitingPickup = tag.getBoolean("AwaitingPickup");

        // Settings
        craftingTaxPercent = tag.contains("CraftingTaxPercent") ? tag.getInt("CraftingTaxPercent") : 15;
        minMarkupPercent = tag.contains("MinMarkupPercent") ? tag.getInt("MinMarkupPercent") : 0;
        if (tag.contains("AutoPriceMode")) {
            try { autoPriceMode = AutoPriceMode.valueOf(tag.getString("AutoPriceMode")); }
            catch (IllegalArgumentException e) { autoPriceMode = AutoPriceMode.AUTO_FAIR; }
        }

        // Price modifiers
        enchantedMarkupEnabled = !tag.contains("EnchantedMarkupEnabled") || tag.getBoolean("EnchantedMarkupEnabled");
        enchantedMarkupPercent = tag.contains("EnchantedMarkupPercent") ? tag.getInt("EnchantedMarkupPercent") : 50;
        usedDiscountEnabled = !tag.contains("UsedDiscountEnabled") || tag.getBoolean("UsedDiscountEnabled");
        usedDiscountPercent = tag.contains("UsedDiscountPercent") ? tag.getInt("UsedDiscountPercent") : 20;
        damagedDiscountEnabled = !tag.contains("DamagedDiscountEnabled") || tag.getBoolean("DamagedDiscountEnabled");
        damagedDiscountPercent = tag.contains("DamagedDiscountPercent") ? tag.getInt("DamagedDiscountPercent") : 40;
        rareMarkupEnabled = !tag.contains("RareMarkupEnabled") || tag.getBoolean("RareMarkupEnabled");
        rareMarkupPercent = tag.contains("RareMarkupPercent") ? tag.getInt("RareMarkupPercent") : 30;
        caravanWeightUpgradeLevel = Math.max(0, tag.getInt("CaravanWeightUpgradeLevel"));

        priceMemory.clear();
        if (tag.contains("PriceMemory")) {
            CompoundTag memoryTag = tag.getCompound("PriceMemory");
            for (String key : memoryTag.getAllKeys()) {
                priceMemory.put(key, memoryTag.getInt(key));
            }
        }

        // Client-side virtual slot index mirror
        clientVirtualSlots.clear();
        if (tag.contains("VirtualSlots")) {
            for (int idx : tag.getIntArray("VirtualSlots")) clientVirtualSlots.add(idx);
        }

        // Shipment history
        shipmentHistory.clear();
        if (tag.contains("ShipmentHistory")) {
            ListTag histList = tag.getList("ShipmentHistory", 10); // 10 = CompoundTag type
            for (int i = 0; i < histList.size(); i++) {
                shipmentHistory.add(LedgerShipmentRecord.load(histList.getCompound(i)));
            }
        }

    }
}
