package com.offtomarket.mod.network;

import com.offtomarket.mod.block.entity.TradingLedgerBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

/**
 * Sent from client to set the price of an item in a specific Trading Bin slot.
 */
public class SetPricePacket {
    private final BlockPos pos;
    private final int slot;
    private final int price;

    public SetPricePacket(BlockPos pos, int slot, int price) {
        this.pos = pos;
        this.slot = slot;
        this.price = price;
    }

    public static void encode(SetPricePacket msg, FriendlyByteBuf buf) {
        buf.writeBlockPos(msg.pos);
        buf.writeInt(msg.slot);
        buf.writeInt(msg.price);
    }

    public static SetPricePacket decode(FriendlyByteBuf buf) {
        return new SetPricePacket(buf.readBlockPos(), buf.readInt(), buf.readInt());
    }

    public static void handle(SetPricePacket msg, Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() -> {
            ServerPlayer player = ctx.get().getSender();
            if (player != null) {
                BlockEntity be = player.level.getBlockEntity(msg.pos);
                if (be instanceof TradingLedgerBlockEntity tbbe) {
                    tbbe.setPrice(msg.slot, msg.price);
                }
            }
        });
        ctx.get().setPacketHandled(true);
    }
}
