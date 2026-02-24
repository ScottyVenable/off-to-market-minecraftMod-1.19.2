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
 * Sent from client when the player clicks to collect returned items from a cancelled shipment.
 */
public class CollectReturnedItemsPacket {

    private final BlockPos pos;
    private final UUID shipmentId;

    public CollectReturnedItemsPacket(BlockPos pos, UUID shipmentId) {
        this.pos = pos;
        this.shipmentId = shipmentId;
    }

    public static void encode(CollectReturnedItemsPacket msg, FriendlyByteBuf buf) {
        buf.writeBlockPos(msg.pos);
        buf.writeUUID(msg.shipmentId);
    }

    public static CollectReturnedItemsPacket decode(FriendlyByteBuf buf) {
        return new CollectReturnedItemsPacket(buf.readBlockPos(), buf.readUUID());
    }

    public static void handle(CollectReturnedItemsPacket msg, Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() -> {
            ServerPlayer player = ctx.get().getSender();
            if (player == null) return;

            BlockEntity be = player.level.getBlockEntity(msg.pos);
            if (!(be instanceof TradingPostBlockEntity tradingPost)) return;

            if (player.distanceToSqr(msg.pos.getX() + 0.5, msg.pos.getY() + 0.5,
                    msg.pos.getZ() + 0.5) > 64.0) return;

            tradingPost.collectReturnedItems(msg.shipmentId, player);
            player.inventoryMenu.broadcastChanges();
        });
        ctx.get().setPacketHandled(true);
    }
}
