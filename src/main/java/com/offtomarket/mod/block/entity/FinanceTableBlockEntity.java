package com.offtomarket.mod.block.entity;

import com.offtomarket.mod.item.CoinItem;
import com.offtomarket.mod.menu.FinanceTableMenu;
import com.offtomarket.mod.registry.ModBlockEntities;
import com.offtomarket.mod.registry.ModItems;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

import java.util.ArrayList;
import java.util.List;

/**
 * The Finance Table block entity stores a coin balance (in copper pieces).
 * Players can deposit all coins from their inventory into the bank balance,
 * or withdraw the full balance as coin items. The block does not act as a
 * container — the balance is a single integer persisted to NBT.
 */
public class FinanceTableBlockEntity extends BlockEntity implements MenuProvider {

    /** Current balance stored in this Finance Table, in copper pieces. */
    private int balance = 0;

    public FinanceTableBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.FINANCE_TABLE.get(), pos, state);
    }

    // ==================== Balance API ====================

    public int getBalance() { return balance; }

    public void setBalance(int value) {
        this.balance = Math.max(0, value);
        setChanged();
    }

    public void addToBalance(int amount) {
        this.balance += amount;
        setChanged();
    }

    // ==================== Deposit / Withdraw ====================

    /**
     * Scans the player's full inventory for coin items, removes them, and
     * adds their total CP value to this table's balance. Syncs to clients.
     */
    public void deposit(Player player) {
        int total = 0;
        Inventory inv = player.getInventory();
        for (int i = 0; i < inv.getContainerSize(); i++) {
            ItemStack stack = inv.getItem(i);
            if (!stack.isEmpty() && stack.getItem() instanceof CoinItem coin) {
                total += coin.getValue() * stack.getCount();
                inv.setItem(i, ItemStack.EMPTY);
            }
        }
        balance += total;
        setChanged();
        syncToClient();
    }

    /**
     * Converts the full balance to coin item stacks and gives them to the
     * player (excess dropped at the player's feet). Sets balance to zero.
     * Syncs to clients.
     */
    public void withdraw(Player player) {
        if (balance <= 0) return;
        List<ItemStack> coins = buildCoinStacks(balance);
        balance = 0;
        setChanged();
        syncToClient();
        for (ItemStack stack : coins) {
            if (!player.getInventory().add(stack)) {
                player.drop(stack, false);
            }
        }
    }

    /**
     * Converts a CP value into a list of coin ItemStacks (gold first, then
     * silver, then copper), each capped at a stack of 64.
     */
    public static List<ItemStack> buildCoinStacks(int cp) {
        List<ItemStack> coins = new ArrayList<>();
        int remaining = cp;
        int gp = remaining / 100; remaining %= 100;
        int sp = remaining / 10;  remaining %= 10;
        int copper = remaining;
        while (gp > 0)     { int n = Math.min(gp, 64);     coins.add(new ItemStack(ModItems.GOLD_COIN.get(), n));   gp -= n; }
        while (sp > 0)     { int n = Math.min(sp, 64);     coins.add(new ItemStack(ModItems.SILVER_COIN.get(), n)); sp -= n; }
        while (copper > 0) { int n = Math.min(copper, 64); coins.add(new ItemStack(ModItems.COPPER_COIN.get(), n)); copper -= n; }
        return coins;
    }

    // ==================== Validity ====================

    public boolean stillValid(Player player) {
        return player.distanceToSqr(
                worldPosition.getX() + 0.5,
                worldPosition.getY() + 0.5,
                worldPosition.getZ() + 0.5) <= 64.0;
    }

    // ==================== MenuProvider ====================

    @Override
    public Component getDisplayName() {
        return Component.translatable("block.offtomarket.finance_table");
    }

    @Override
    public AbstractContainerMenu createMenu(int containerId, Inventory inv, Player player) {
        return new FinanceTableMenu(containerId, inv, this);
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

    // ==================== NBT ====================

    @Override
    protected void saveAdditional(CompoundTag tag) {
        super.saveAdditional(tag);
        tag.putInt("Balance", balance);
    }

    @Override
    public void load(CompoundTag tag) {
        super.load(tag);
        balance = tag.getInt("Balance");
    }
}
