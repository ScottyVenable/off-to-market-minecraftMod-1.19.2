package com.offtomarket.mod.menu;

import com.offtomarket.mod.block.entity.TradingBinBlockEntity;
import com.offtomarket.mod.registry.ModMenuTypes;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;

/**
 * Container/Menu for the Trading Bin.
 * Shows 9 item slots for placing items to sell, plus the player inventory.
 */
public class TradingBinMenu extends AbstractContainerMenu {
    private final TradingBinBlockEntity blockEntity;

    // Client-side constructor
    public TradingBinMenu(int containerId, Inventory inv) {
        this(containerId, inv, null);
    }

    public TradingBinMenu(int containerId, Inventory inv, TradingBinBlockEntity be) {
        super(ModMenuTypes.TRADING_BIN.get(), containerId);
        this.blockEntity = be;

        // Trading Bin slots (3x3 grid)
        // Use a dummy container on client side so slot count matches server
        net.minecraft.world.Container container = be != null ? be : new SimpleContainer(TradingBinBlockEntity.TOTAL_SIZE);
        for (int row = 0; row < 3; row++) {
            for (int col = 0; col < 3; col++) {
                this.addSlot(new Slot(container, col + row * 3, 62 + col * 18, 17 + row * 18));
            }
        }

        // Inspection slot (in right panel)
        this.addSlot(new Slot(container, TradingBinBlockEntity.INSPECT_SLOT, 184, 34) {
            @Override
            public int getMaxStackSize() { return 64; }
        });

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

    public TradingBinBlockEntity getBlockEntity() { return blockEntity; }

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        ItemStack result = ItemStack.EMPTY;
        Slot slot = this.slots.get(index);

        if (slot != null && slot.hasItem()) {
            ItemStack slotStack = slot.getItem();
            result = slotStack.copy();

            int binSlots = TradingBinBlockEntity.BIN_SIZE;      // 9
            int inspectSlot = TradingBinBlockEntity.INSPECT_SLOT; // 9 in container, 9 in menu
            int playerStart = inspectSlot + 1;                    // 10
            int playerEnd = playerStart + 36;                     // 46

            if (index < binSlots) {
                // Moving from bin to player inventory
                if (!this.moveItemStackTo(slotStack, playerStart, playerEnd, true)) {
                    return ItemStack.EMPTY;
                }
            } else if (index == inspectSlot) {
                // Moving from inspection to player inventory
                if (!this.moveItemStackTo(slotStack, playerStart, playerEnd, true)) {
                    return ItemStack.EMPTY;
                }
            } else {
                // Moving from player inventory to bin (NOT to inspection slot)
                if (!this.moveItemStackTo(slotStack, 0, binSlots, false)) {
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
