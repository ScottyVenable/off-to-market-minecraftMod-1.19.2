package com.offtomarket.mod.network;

import com.offtomarket.mod.block.entity.TradingPostBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraftforge.network.NetworkEvent;

import java.util.UUID;
import java.util.function.Supplier;

/**
 * Sent from client when the player clicks on a completed (sold) shipment
 * in the Activity tab to collect their coins.
 */
public class CollectShipmentCoinsPacket {

    private final BlockPos pos;
    private final UUID shipmentId;

    public CollectShipmentCoinsPacket(BlockPos pos, UUID shipmentId) {
        this.pos = pos;
        this.shipmentId = shipmentId;
    }

    public static void encode(CollectShipmentCoinsPacket msg, FriendlyByteBuf buf) {
        buf.writeBlockPos(msg.pos);
        buf.writeUUID(msg.shipmentId);
    }

    public static CollectShipmentCoinsPacket decode(FriendlyByteBuf buf) {
        return new CollectShipmentCoinsPacket(buf.readBlockPos(), buf.readUUID());
    }

    public static void handle(CollectShipmentCoinsPacket msg, Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() -> {
            ServerPlayer player = ctx.get().getSender();
            if (player == null) return;

            BlockEntity be = player.level.getBlockEntity(msg.pos);
            if (!(be instanceof TradingPostBlockEntity tradingPost)) return;

            if (player.distanceToSqr(msg.pos.getX() + 0.5, msg.pos.getY() + 0.5,
                    msg.pos.getZ() + 0.5) > 64.0) return;

            tradingPost.collectShipmentCoins(msg.shipmentId, player);
            player.inventoryMenu.broadcastChanges();
        });
        ctx.get().setPacketHandled(true);
    }
}
