package com.offtomarket.mod.network;

import com.offtomarket.mod.block.entity.TradingLedgerBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraftforge.network.NetworkEvent;

import java.util.UUID;
import java.util.function.Supplier;

public class DispatchCustomOrderPacket {

    private final BlockPos ledgerPos;
    private final UUID orderId;

    public DispatchCustomOrderPacket(BlockPos ledgerPos, UUID orderId) {
        this.ledgerPos = ledgerPos;
        this.orderId = orderId;
    }

    public static void encode(DispatchCustomOrderPacket msg, FriendlyByteBuf buf) {
        buf.writeBlockPos(msg.ledgerPos);
        buf.writeUUID(msg.orderId);
    }

    public static DispatchCustomOrderPacket decode(FriendlyByteBuf buf) {
        BlockPos pos = buf.readBlockPos();
        UUID id = buf.readUUID();
        return new DispatchCustomOrderPacket(pos, id);
    }

    public static void handle(DispatchCustomOrderPacket msg, Supplier<NetworkEvent.Context> ctxSupplier) {
        NetworkEvent.Context ctx = ctxSupplier.get();
        ctx.enqueueWork(() -> {
            ServerPlayer player = ctx.getSender();
            if (player == null || player.level == null) return;

            BlockEntity be = player.level.getBlockEntity(msg.ledgerPos);
            if (be instanceof TradingLedgerBlockEntity ledger) {
                ledger.dispatchCustomOrder(msg.orderId);
            }
        });
        ctx.setPacketHandled(true);
    }
}
