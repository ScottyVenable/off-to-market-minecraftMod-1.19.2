package com.offtomarket.mod.network;

import com.offtomarket.mod.block.entity.TradingLedgerBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

/**
 * Sent from client to withdraw an item from a Trading Bin slot back into the player's inventory.
 */
public class WithdrawBinItemPacket {
    private final BlockPos pos;
    private final int slot;
    private final boolean toContainer;

    public WithdrawBinItemPacket(BlockPos pos, int slot, boolean toContainer) {
        this.pos = pos;
        this.slot = slot;
        this.toContainer = toContainer;
    }

    /** Backward-compat constructor — withdraws to player inventory. */
    public WithdrawBinItemPacket(BlockPos pos, int slot) {
        this(pos, slot, false);
    }

    public static void encode(WithdrawBinItemPacket msg, FriendlyByteBuf buf) {
        buf.writeBlockPos(msg.pos);
        buf.writeInt(msg.slot);
        buf.writeBoolean(msg.toContainer);
    }

    public static WithdrawBinItemPacket decode(FriendlyByteBuf buf) {
        return new WithdrawBinItemPacket(buf.readBlockPos(), buf.readInt(), buf.readBoolean());
    }

    public static void handle(WithdrawBinItemPacket msg, Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() -> {
            ServerPlayer player = ctx.get().getSender();
            if (player != null) {
                BlockEntity be = player.level.getBlockEntity(msg.pos);
                if (be instanceof TradingLedgerBlockEntity tbbe) {
                    if (msg.slot >= 0 && msg.slot < TradingLedgerBlockEntity.BIN_SIZE) {
                        ItemStack stack = tbbe.getItem(msg.slot);
                        if (!stack.isEmpty()) {
                            ItemStack toGive = stack.copy();
                            tbbe.setItem(msg.slot, ItemStack.EMPTY);
                            tbbe.setChanged();
                            boolean placed = false;
                            if (msg.toContainer) {
                                // Try to place into an adjacent container block entity
                                for (net.minecraft.core.Direction dir : net.minecraft.core.Direction.values()) {
                                    net.minecraft.world.level.block.entity.BlockEntity adj =
                                            player.level.getBlockEntity(be.getBlockPos().relative(dir));
                                    if (adj instanceof net.minecraft.world.Container adjCont
                                            && !(adj instanceof TradingLedgerBlockEntity)) {
                                        for (int i = 0; i < adjCont.getContainerSize(); i++) {
                                            ItemStack slot2 = adjCont.getItem(i);
                                            if (slot2.isEmpty()) {
                                                adjCont.setItem(i, toGive);
                                                adjCont.setChanged();
                                                placed = true;
                                                break;
                                            } else if (net.minecraft.world.item.ItemStack.isSameItemSameTags(slot2, toGive)
                                                    && slot2.getCount() < slot2.getMaxStackSize()) {
                                                int room = slot2.getMaxStackSize() - slot2.getCount();
                                                int take = Math.min(room, toGive.getCount());
                                                slot2.grow(take);
                                                toGive.shrink(take);
                                                adjCont.setChanged();
                                                if (toGive.isEmpty()) { placed = true; break; }
                                            }
                                        }
                                        if (placed) break;
                                    }
                                }
                            }
                            if (!placed) {
                                // Fall back to player inventory
                                if (!player.getInventory().add(toGive)) {
                                    player.drop(toGive, false);
                                }
                            }
                        }
                    }
                }
            }
        });
        ctx.get().setPacketHandled(true);
    }
}
