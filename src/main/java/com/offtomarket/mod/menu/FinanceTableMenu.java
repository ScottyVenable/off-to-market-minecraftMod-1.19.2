package com.offtomarket.mod.menu;

import com.offtomarket.mod.block.entity.FinanceTableBlockEntity;
import com.offtomarket.mod.registry.ModMenuTypes;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;

/**
 * Menu for the Finance Table. Exposes only the player inventory — no block
 * entity item slots. The balance (coin value) is read directly from the
 * synced block entity on the client side.
 */
public class FinanceTableMenu extends AbstractContainerMenu {

    private final FinanceTableBlockEntity blockEntity;

    // Slot layout constants (relative to screen left/top)
    public static final int PLAYER_INV_Y = 64;
    public static final int HOTBAR_Y     = 122;

    // Client-side constructor (block entity resolved via menu type factory)
    public FinanceTableMenu(int containerId, Inventory inv) {
        this(containerId, inv, null);
    }

    public FinanceTableMenu(int containerId, Inventory inv, FinanceTableBlockEntity be) {
        super(ModMenuTypes.FINANCE_TABLE.get(), containerId);
        this.blockEntity = be;

        // Player inventory (3 rows x 9 cols)
        for (int row = 0; row < 3; row++) {
            for (int col = 0; col < 9; col++) {
                this.addSlot(new Slot(inv, col + row * 9 + 9, 8 + col * 18, PLAYER_INV_Y + row * 18));
            }
        }

        // Hotbar
        for (int col = 0; col < 9; col++) {
            this.addSlot(new Slot(inv, col, 8 + col * 18, HOTBAR_Y));
        }
    }

    public FinanceTableBlockEntity getBlockEntity() { return blockEntity; }

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        // No block slots to shift-click into or out of
        return ItemStack.EMPTY;
    }

    @Override
    public boolean stillValid(Player player) {
        if (blockEntity == null) return true;
        return blockEntity.stillValid(player);
    }
}
