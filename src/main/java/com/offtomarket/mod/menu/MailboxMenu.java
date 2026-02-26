package com.offtomarket.mod.menu;

import com.offtomarket.mod.block.entity.MailboxBlockEntity;
import com.offtomarket.mod.registry.ModMenuTypes;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;

/**
 * Container/Menu for the Mailbox.
 * No item slots — the Mailbox is purely a data-driven UI for reading/deleting notes.
 * All note data is accessed through the block entity reference.
 */
public class MailboxMenu extends AbstractContainerMenu {
    private final MailboxBlockEntity blockEntity;

    // Client-side constructor
    public MailboxMenu(int containerId, Inventory inv) {
        this(containerId, inv, null);
    }

    public MailboxMenu(int containerId, Inventory inv, MailboxBlockEntity be) {
        super(ModMenuTypes.MAILBOX.get(), containerId);
        this.blockEntity = be;
    }

    public MailboxBlockEntity getBlockEntity() { return blockEntity; }

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
