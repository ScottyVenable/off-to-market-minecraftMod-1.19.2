package com.offtomarket.mod.network;

import com.offtomarket.mod.block.entity.TradingLedgerBlockEntity;
import com.offtomarket.mod.data.CustomTradeOrder;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

public class CreateCustomOrderPacket {

    private final BlockPos ledgerPos;
    private final CompoundTag orderTag;

    public CreateCustomOrderPacket(BlockPos ledgerPos, CustomTradeOrder order) {
        this.ledgerPos = ledgerPos;
        this.orderTag = order.save();
    }

    public CreateCustomOrderPacket(BlockPos ledgerPos, CompoundTag orderTag) {
        this.ledgerPos = ledgerPos;
        this.orderTag = orderTag;
    }

    public static void encode(CreateCustomOrderPacket msg, FriendlyByteBuf buf) {
        buf.writeBlockPos(msg.ledgerPos);
        buf.writeNbt(msg.orderTag);
    }

    public static CreateCustomOrderPacket decode(FriendlyByteBuf buf) {
        BlockPos pos = buf.readBlockPos();
        CompoundTag tag = buf.readNbt();
        return new CreateCustomOrderPacket(pos, tag != null ? tag : new CompoundTag());
    }

    public static void handle(CreateCustomOrderPacket msg, Supplier<NetworkEvent.Context> ctxSupplier) {
        NetworkEvent.Context ctx = ctxSupplier.get();
        ctx.enqueueWork(() -> {
            ServerPlayer player = ctx.getSender();
            if (player == null || player.level == null) return;

            BlockEntity be = player.level.getBlockEntity(msg.ledgerPos);
            if (be instanceof TradingLedgerBlockEntity ledger) {
                CustomTradeOrder order = CustomTradeOrder.load(msg.orderTag);
                ledger.addCustomOrder(order);
            }
        });
        ctx.setPacketHandled(true);
    }
}
