package com.offtomarket.mod.block.entity;

import com.offtomarket.mod.data.PriceCalculator;
import com.offtomarket.mod.menu.TradingBinMenu;
import com.offtomarket.mod.registry.ModBlockEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.core.NonNullList;
import net.minecraft.nbt.CompoundTag;
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
import java.util.HashMap;
import java.util.Map;

/**
 * The Trading Bin holds items the player wants to sell/ship.
 * Players can set prices per slot. When items are shipped,
 * they're removed and replaced with a shipment note.
 *
 * 9 slots for items, plus the note appears in slot 0 after pickup.
 */
public class TradingBinBlockEntity extends BlockEntity implements Container, MenuProvider {
    public static final int BIN_SIZE = 9;
    public static final int INSPECT_SLOT = 9;
    public static final int TOTAL_SIZE = 10;

    private NonNullList<ItemStack> items = NonNullList.withSize(BIN_SIZE, ItemStack.EMPTY);

    // Inspection slot: holds an item for price checking/setting in the book panel
    private ItemStack inspectionItem = ItemStack.EMPTY;

    // Prices set by the player for each slot (in copper pieces, 0 = use default)
    private final Map<Integer, Integer> slotPrices = new HashMap<>();

    // Remembers the last price set for each item type (persists across shipments)
    private final Map<String, Integer> priceMemory = new HashMap<>();

    // Countdown for items to be "picked up" after send command
    private int pickupTimer = -1;
    private boolean awaitingPickup = false;

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

    private int craftingTaxPercent = 15;   // extra % for crafted items
    private int minMarkupPercent = 0;      // minimum markup % over fair value
    private AutoPriceMode autoPriceMode = AutoPriceMode.AUTO_FAIR;

    public TradingBinBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.TRADING_BIN.get(), pos, state);
    }

    // ==================== Price Management ====================

    public int getSetPrice(int slot) {
        return slotPrices.getOrDefault(slot, 0);
    }

    public void setPrice(int slot, int price) {
        if (slot == INSPECT_SLOT) {
            // Set price for the item TYPE in the inspection slot
            if (!inspectionItem.isEmpty()) {
                String key = getItemKey(inspectionItem);
                if (key != null) {
                    if (price <= 0) {
                        priceMemory.remove(key);
                    } else {
                        priceMemory.put(key, price);
                    }
                    // Propagate to all bin slots with matching items
                    for (int i = 0; i < BIN_SIZE; i++) {
                        if (!items.get(i).isEmpty() && key.equals(getItemKey(items.get(i)))) {
                            if (price <= 0) {
                                slotPrices.remove(i);
                            } else {
                                slotPrices.put(i, price);
                            }
                        }
                    }
                }
            }
        } else {
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
     * Clear all items and leave a shipment note in slot 0.
     */
    public void clearAndLeaveNote(ItemStack note) {
        items.clear();
        items = NonNullList.withSize(BIN_SIZE, ItemStack.EMPTY);
        items.set(0, note);
        slotPrices.clear();
        awaitingPickup = false;
        pickupTimer = -1;
        syncToClient();
    }

    /**
     * Add an item to the first available slot (used for returned items).
     */
    public boolean addItem(ItemStack stack) {
        for (int i = 0; i < BIN_SIZE; i++) {
            if (items.get(i).isEmpty()) {
                items.set(i, stack);
                setChanged();
                return true;
            }
        }
        return false;
    }

    public NonNullList<ItemStack> getItems() {
        return items;
    }

    public static void serverTick(Level level, BlockPos pos, BlockState state,
                                   TradingBinBlockEntity be) {
        if (be.awaitingPickup && be.pickupTimer > 0) {
            be.pickupTimer--;
            if (be.pickupTimer <= 0) {
                // Timer expired - items should have been picked up by the Trading Post
                be.awaitingPickup = false;
                be.setChanged();
            }
        }
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
        if (slot == INSPECT_SLOT) return inspectionItem;
        return items.get(slot);
    }

    @Override
    public ItemStack removeItem(int slot, int amount) {
        if (slot == INSPECT_SLOT) {
            if (inspectionItem.isEmpty()) return ItemStack.EMPTY;
            ItemStack result = inspectionItem.split(amount);
            if (inspectionItem.isEmpty()) inspectionItem = ItemStack.EMPTY;
            setChanged();
            return result;
        }
        ItemStack result = ContainerHelper.removeItem(items, slot, amount);
        if (!result.isEmpty()) setChanged();
        return result;
    }

    @Override
    public ItemStack removeItemNoUpdate(int slot) {
        if (slot == INSPECT_SLOT) {
            ItemStack result = inspectionItem;
            inspectionItem = ItemStack.EMPTY;
            return result;
        }
        return ContainerHelper.takeItem(items, slot);
    }

    @Override
    public void setItem(int slot, ItemStack stack) {
        if (slot == INSPECT_SLOT) {
            inspectionItem = stack;
            setChanged();
            return;
        }
        items.set(slot, stack);
        if (stack.getCount() > getMaxStackSize()) {
            stack.setCount(getMaxStackSize());
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

        if (autoPriceMode == AutoPriceMode.AUTO_FAIR) {
            // Fair value + crafting tax
            int tax = (int) (baseValue * (craftingTaxPercent / 100.0));
            return baseValue + tax;
        } else if (autoPriceMode == AutoPriceMode.AUTO_MARKUP) {
            // Fair value + crafting tax + minimum markup
            int tax = (int) (baseValue * (craftingTaxPercent / 100.0));
            int subtotal = baseValue + tax;
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
        inspectionItem = ItemStack.EMPTY;
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
        return Component.translatable("block.offtomarket.trading_bin");
    }

    @Nullable
    @Override
    public AbstractContainerMenu createMenu(int containerId, Inventory inv, Player player) {
        return new TradingBinMenu(containerId, inv, this);
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

        if (!inspectionItem.isEmpty()) {
            tag.put("InspectionItem", inspectionItem.save(new CompoundTag()));
        }
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

        priceMemory.clear();
        if (tag.contains("PriceMemory")) {
            CompoundTag memoryTag = tag.getCompound("PriceMemory");
            for (String key : memoryTag.getAllKeys()) {
                priceMemory.put(key, memoryTag.getInt(key));
            }
        }

        inspectionItem = tag.contains("InspectionItem")
                ? ItemStack.of(tag.getCompound("InspectionItem"))
                : ItemStack.EMPTY;
    }
}
