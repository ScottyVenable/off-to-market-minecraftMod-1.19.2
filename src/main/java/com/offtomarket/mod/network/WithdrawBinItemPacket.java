package com.offtomarket.mod.network;

import com.offtomarket.mod.block.entity.TradingBinBlockEntity;
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

    public WithdrawBinItemPacket(BlockPos pos, int slot) {
        this.pos = pos;
        this.slot = slot;
    }

    public static void encode(WithdrawBinItemPacket msg, FriendlyByteBuf buf) {
        buf.writeBlockPos(msg.pos);
        buf.writeInt(msg.slot);
    }

    public static WithdrawBinItemPacket decode(FriendlyByteBuf buf) {
        return new WithdrawBinItemPacket(buf.readBlockPos(), buf.readInt());
    }

    public static void handle(WithdrawBinItemPacket msg, Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() -> {
            ServerPlayer player = ctx.get().getSender();
            if (player != null) {
                BlockEntity be = player.level.getBlockEntity(msg.pos);
                if (be instanceof TradingBinBlockEntity tbbe) {
                    if (msg.slot >= 0 && msg.slot < TradingBinBlockEntity.BIN_SIZE) {
                        ItemStack stack = tbbe.getItem(msg.slot);
                        if (!stack.isEmpty()) {
                            ItemStack toGive = stack.copy();
                            tbbe.setItem(msg.slot, ItemStack.EMPTY);
                            tbbe.setChanged();
                            if (!player.getInventory().add(toGive)) {
                                player.drop(toGive, false);
                            }
                        }
                    }
                }
            }
        });
        ctx.get().setPacketHandled(true);
    }
}
