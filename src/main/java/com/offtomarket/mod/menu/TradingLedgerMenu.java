package com.offtomarket.mod.menu;

import com.offtomarket.mod.block.entity.TradingLedgerBlockEntity;
import com.offtomarket.mod.registry.ModMenuTypes;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;

/**
 * Container/Menu for the Trading Bin.
 * All slots are positioned off-screen; the GUI uses a custom list view.
 * Shift-click still moves items between bin and player inventory.
 */
public class TradingLedgerMenu extends AbstractContainerMenu {
    private final TradingLedgerBlockEntity blockEntity;

    // Client-side constructor
    public TradingLedgerMenu(int containerId, Inventory inv) {
        this(containerId, inv, null);
    }

    public TradingLedgerMenu(int containerId, Inventory inv, TradingLedgerBlockEntity be) {
        super(ModMenuTypes.TRADING_LEDGER.get(), containerId);
        this.blockEntity = be;

        net.minecraft.world.Container container = be != null ? be : new SimpleContainer(TradingLedgerBlockEntity.BIN_SIZE);

        // Bin slots (off-screen — rendered as a custom list in the screen)
        for (int i = 0; i < TradingLedgerBlockEntity.BIN_SIZE; i++) {
            this.addSlot(new Slot(container, i, -9999, -9999));
        }

        // Player inventory (off-screen — not visible in the management GUI)
        for (int row = 0; row < 3; row++) {
            for (int col = 0; col < 9; col++) {
                this.addSlot(new Slot(inv, col + row * 9 + 9, -9999, -9999));
            }
        }

        // Player hotbar (off-screen)
        for (int col = 0; col < 9; col++) {
            this.addSlot(new Slot(inv, col, -9999, -9999));
        }
    }

    public TradingLedgerBlockEntity getBlockEntity() { return blockEntity; }

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        ItemStack result = ItemStack.EMPTY;
        Slot slot = this.slots.get(index);

        if (slot != null && slot.hasItem()) {
            ItemStack slotStack = slot.getItem();
            result = slotStack.copy();

            int binEnd = TradingLedgerBlockEntity.BIN_SIZE;      // 9
            int playerStart = binEnd;                          // 9
            int playerEnd = playerStart + 36;                  // 45

            if (index < binEnd) {
                // Moving from bin to player inventory
                if (!this.moveItemStackTo(slotStack, playerStart, playerEnd, true)) {
                    return ItemStack.EMPTY;
                }
            } else {
                // Moving from player inventory to bin
                if (!this.moveItemStackTo(slotStack, 0, binEnd, false)) {
                    return ItemStack.EMPTY;
                }
            }

            if (slotStack.isEmpty()) {
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
