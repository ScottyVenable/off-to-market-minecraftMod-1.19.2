package com.offtomarket.mod.menu;

import com.offtomarket.mod.block.entity.FinanceTableBlockEntity;
import com.offtomarket.mod.registry.ModMenuTypes;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;

/**
 * Menu for the Finance Table. Provides 27 coin storage slots and a full
 * player inventory for convenient coin transfers.
 */
public class FinanceTableMenu extends AbstractContainerMenu {

    private final FinanceTableBlockEntity blockEntity;

    // Client-side constructor (block entity resolved via menu type factory)
    public FinanceTableMenu(int containerId, Inventory inv) {
        this(containerId, inv, null);
    }

    public FinanceTableMenu(int containerId, Inventory inv, FinanceTableBlockEntity be) {
        super(ModMenuTypes.FINANCE_TABLE.get(), containerId);
        this.blockEntity = be;

        int rows = 3;
        int cols = 9;

        // Finance Table slots (3 rows x 9 = 27 slots)
        for (int row = 0; row < rows; row++) {
            for (int col = 0; col < cols; col++) {
                int slotIndex = col + row * cols;
                int x = 8 + col * 18;
                int y = 18 + row * 18;
                if (be != null) {
                    this.addSlot(new Slot(be, slotIndex, x, y));
                } else {
                    this.addSlot(new Slot(new net.minecraft.world.SimpleContainer(27), slotIndex, x, y));
                }
            }
        }

        // Player inventory (3 rows)
        for (int row = 0; row < 3; row++) {
            for (int col = 0; col < 9; col++) {
                this.addSlot(new Slot(inv, col + row * 9 + 9, 8 + col * 18, 84 + row * 18));
            }
        }

        // Hotbar
        for (int col = 0; col < 9; col++) {
            this.addSlot(new Slot(inv, col, 8 + col * 18, 142));
        }
    }

    public FinanceTableBlockEntity getBlockEntity() { return blockEntity; }

    // Slot indices
    private static final int TABLE_SLOT_START = 0;
    private static final int TABLE_SLOT_END = 26;
    private static final int PLAYER_INV_START = 27;
    private static final int PLAYER_INV_END = 62;

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        ItemStack result = ItemStack.EMPTY;
        Slot slot = this.slots.get(index);
        if (slot.hasItem()) {
            ItemStack stack = slot.getItem();
            result = stack.copy();
            if (index <= TABLE_SLOT_END) {
                // Move from table -> player inventory
                if (!this.moveItemStackTo(stack, PLAYER_INV_START, PLAYER_INV_END + 1, true)) {
                    return ItemStack.EMPTY;
                }
            } else {
                // Move from player inventory -> table
                if (!this.moveItemStackTo(stack, TABLE_SLOT_START, TABLE_SLOT_END + 1, false)) {
                    return ItemStack.EMPTY;
                }
            }
            if (stack.isEmpty()) {
                slot.set(ItemStack.EMPTY);
            } else {
                slot.setChanged();
            }
        }
        return result;
    }

    @Override
    public boolean stillValid(Player player) {
        if (blockEntity == null) return true;
        return blockEntity.stillValid(player);
    }
}
