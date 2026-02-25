package com.offtomarket.mod.menu;

import com.offtomarket.mod.block.entity.MarketBoardBlockEntity;
import com.offtomarket.mod.registry.ModMenuTypes;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;

/**
 * Container/Menu for the Market Board.
 * This is a read-only view of available market listings from towns.
 * Player inventory is included for buying items with coins.
 */
public class MarketBoardMenu extends AbstractContainerMenu {
    private final MarketBoardBlockEntity blockEntity;

    // Client-side constructor
    public MarketBoardMenu(int containerId, Inventory inv) {
        this(containerId, inv, null);
    }

    public MarketBoardMenu(int containerId, Inventory inv, MarketBoardBlockEntity be) {
        super(ModMenuTypes.MARKET_BOARD.get(), containerId);
        this.blockEntity = be;
        // No player inventory slots — Market Board is read-only; coins are read directly from Inventory.
    }

    public MarketBoardBlockEntity getBlockEntity() { return blockEntity; }

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        return ItemStack.EMPTY;
    }

    @Override
    public boolean stillValid(Player player) {
        if (blockEntity == null) return true;
        return player.distanceToSqr(
                blockEntity.getBlockPos().getX() + 0.5,
                blockEntity.getBlockPos().getY() + 0.5,
                blockEntity.getBlockPos().getZ() + 0.5) <= 64.0;
    }
}
