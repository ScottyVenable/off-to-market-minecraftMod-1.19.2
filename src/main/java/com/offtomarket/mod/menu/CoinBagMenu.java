package com.offtomarket.mod.menu;

import com.offtomarket.mod.item.CoinItem;
import com.offtomarket.mod.item.CoinType;
import com.offtomarket.mod.registry.ModItems;
import com.offtomarket.mod.registry.ModMenuTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.world.Container;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;

/**
 * Menu for the Coin Bag. Has 3 filtered slots:
 * - Slot 0: Gold coins only
 * - Slot 1: Silver coins only
 * - Slot 2: Copper coins only
 * Each slot supports up to 999 coins (overriding the normal 64 limit).
 * Contents are persisted to the Coin Bag item's NBT.
 */
public class CoinBagMenu extends AbstractContainerMenu {
    private final SimpleContainer coinStorage;
    private final ItemStack bagStack;
    private final int bagSlotIndex; // which inventory slot holds the bag

    // Slot indices in this menu
    public static final int GOLD_SLOT = 0;
    public static final int SILVER_SLOT = 1;
    public static final int COPPER_SLOT = 2;
    private static final int BAG_SLOTS = 3;
    private static final int INV_START = BAG_SLOTS;
    private static final int INV_END = INV_START + 36;
    private static final int MAX_COIN_STACK = 999;

    // Client constructor
    public CoinBagMenu(int containerId, Inventory inv) {
        this(containerId, inv, ItemStack.EMPTY, -1);
    }

    public CoinBagMenu(int containerId, Inventory inv, ItemStack bagStack, int bagSlotIndex) {
        super(ModMenuTypes.COIN_BAG.get(), containerId);
        this.bagStack = bagStack;
        this.bagSlotIndex = bagSlotIndex;

        // Create storage with max stack size override
        this.coinStorage = new SimpleContainer(3) {
            @Override
            public int getMaxStackSize() {
                return MAX_COIN_STACK;
            }
        };

        // Load from bag NBT
        loadFromBag();

        // Coin bag slots (centered in a small GUI)
        this.addSlot(new CoinFilterSlot(coinStorage, GOLD_SLOT, 44, 35, CoinType.GOLD));
        this.addSlot(new CoinFilterSlot(coinStorage, SILVER_SLOT, 80, 35, CoinType.SILVER));
        this.addSlot(new CoinFilterSlot(coinStorage, COPPER_SLOT, 116, 35, CoinType.COPPER));

        // Player inventory (3 rows of 9)
        for (int row = 0; row < 3; row++) {
            for (int col = 0; col < 9; col++) {
                this.addSlot(new Slot(inv, col + row * 9 + 9, 8 + col * 18, 84 + row * 18));
            }
        }
        // Player hotbar
        for (int col = 0; col < 9; col++) {
            this.addSlot(new Slot(inv, col, 8 + col * 18, 142));
        }
    }

    private void loadFromBag() {
        if (bagStack.isEmpty()) return;
        CompoundTag tag = bagStack.getTag();
        if (tag == null || !tag.contains("CoinBag")) return;
        CompoundTag bagTag = tag.getCompound("CoinBag");

        if (bagTag.contains("Gold")) {
            coinStorage.setItem(GOLD_SLOT,
                    new ItemStack(ModItems.GOLD_COIN.get(), bagTag.getInt("Gold")));
        }
        if (bagTag.contains("Silver")) {
            coinStorage.setItem(SILVER_SLOT,
                    new ItemStack(ModItems.SILVER_COIN.get(), bagTag.getInt("Silver")));
        }
        if (bagTag.contains("Copper")) {
            coinStorage.setItem(COPPER_SLOT,
                    new ItemStack(ModItems.COPPER_COIN.get(), bagTag.getInt("Copper")));
        }
    }

    private void saveToBag() {
        if (bagStack.isEmpty()) return;
        CompoundTag tag = bagStack.getOrCreateTag();
        CompoundTag bagTag = new CompoundTag();

        ItemStack gold = coinStorage.getItem(GOLD_SLOT);
        ItemStack silver = coinStorage.getItem(SILVER_SLOT);
        ItemStack copper = coinStorage.getItem(COPPER_SLOT);

        if (!gold.isEmpty()) bagTag.putInt("Gold", gold.getCount());
        if (!silver.isEmpty()) bagTag.putInt("Silver", silver.getCount());
        if (!copper.isEmpty()) bagTag.putInt("Copper", copper.getCount());

        tag.put("CoinBag", bagTag);
    }

    public int getTotalValue() {
        int total = 0;
        ItemStack gold = coinStorage.getItem(GOLD_SLOT);
        ItemStack silver = coinStorage.getItem(SILVER_SLOT);
        ItemStack copper = coinStorage.getItem(COPPER_SLOT);
        if (!gold.isEmpty()) total += gold.getCount() * 100;
        if (!silver.isEmpty()) total += silver.getCount() * 10;
        if (!copper.isEmpty()) total += copper.getCount();
        return total;
    }

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        Slot slot = this.slots.get(index);
        if (slot == null || !slot.hasItem()) return ItemStack.EMPTY;

        ItemStack current = slot.getItem();
        ItemStack copy = current.copy();

        if (index < BAG_SLOTS) {
            // Moving from bag to player inventory
            if (!this.moveItemStackTo(current, INV_START, INV_END, true)) {
                return ItemStack.EMPTY;
            }
        } else {
            // Moving from player inventory to bag - find the right slot
            if (current.getItem() instanceof CoinItem coin) {
                int targetSlot = switch (coin.getCoinType()) {
                    case GOLD -> GOLD_SLOT;
                    case SILVER -> SILVER_SLOT;
                    case COPPER -> COPPER_SLOT;
                };
                if (!this.moveItemStackTo(current, targetSlot, targetSlot + 1, false)) {
                    return ItemStack.EMPTY;
                }
            } else {
                return ItemStack.EMPTY; // Not a coin
            }
        }

        if (current.isEmpty()) {
            slot.set(ItemStack.EMPTY);
        } else {
            slot.setChanged();
        }

        return copy;
    }

    @Override
    public void removed(Player player) {
        super.removed(player);
        saveToBag();
    }

    @Override
    public boolean stillValid(Player player) {
        // Bag must still be in the player's inventory
        if (bagSlotIndex < 0) return true;
        return player.getInventory().getItem(bagSlotIndex) == bagStack;
    }

    /**
     * A slot that only accepts a specific CoinType and supports large stacks.
     */
    private static class CoinFilterSlot extends Slot {
        private final CoinType acceptedType;

        public CoinFilterSlot(Container container, int index, int x, int y, CoinType type) {
            super(container, index, x, y);
            this.acceptedType = type;
        }

        @Override
        public boolean mayPlace(ItemStack stack) {
            return stack.getItem() instanceof CoinItem coin
                    && coin.getCoinType() == acceptedType;
        }

        @Override
        public int getMaxStackSize() {
            return MAX_COIN_STACK;
        }

        @Override
        public int getMaxStackSize(ItemStack stack) {
            return MAX_COIN_STACK;
        }
    }
}
