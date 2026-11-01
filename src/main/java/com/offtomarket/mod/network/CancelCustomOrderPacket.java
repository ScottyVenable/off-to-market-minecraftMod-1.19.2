package com.offtomarket.mod.network;

import com.offtomarket.mod.block.entity.TradingLedgerBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraftforge.network.NetworkEvent;

import java.util.UUID;
import java.util.function.Supplier;

public class CancelCustomOrderPacket {

    private final BlockPos ledgerPos;
    private final UUID orderId;

    public CancelCustomOrderPacket(BlockPos ledgerPos, UUID orderId) {
        this.ledgerPos = ledgerPos;
        this.orderId = orderId;
    }

    public static void encode(CancelCustomOrderPacket msg, FriendlyByteBuf buf) {
        buf.writeBlockPos(msg.ledgerPos);
        buf.writeUUID(msg.orderId);
    }

    public static CancelCustomOrderPacket decode(FriendlyByteBuf buf) {
        BlockPos pos = buf.readBlockPos();
        UUID id = buf.readUUID();
        return new CancelCustomOrderPacket(pos, id);
    }

    public static void handle(CancelCustomOrderPacket msg, Supplier<NetworkEvent.Context> ctxSupplier) {
        NetworkEvent.Context ctx = ctxSupplier.get();
        ctx.enqueueWork(() -> {
            ServerPlayer player = ctx.getSender();
            if (player == null || player.level == null) return;

            BlockEntity be = player.level.getBlockEntity(msg.ledgerPos);
            if (be instanceof TradingLedgerBlockEntity ledger) {
                ledger.removeCustomOrder(msg.orderId);
            }
        });
        ctx.setPacketHandled(true);
    }
}
