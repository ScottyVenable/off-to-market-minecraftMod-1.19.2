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
 * Sent from client when the player clicks "Collect" on an arrived buy order
 * in the Orders tab of the Trading Post.
 */
public class CollectOrderPacket {

    private final BlockPos pos;
    private final UUID orderId;

    public CollectOrderPacket(BlockPos pos, UUID orderId) {
        this.pos = pos;
        this.orderId = orderId;
    }

    public static void encode(CollectOrderPacket msg, FriendlyByteBuf buf) {
        buf.writeBlockPos(msg.pos);
        buf.writeUUID(msg.orderId);
    }

    public static CollectOrderPacket decode(FriendlyByteBuf buf) {
        return new CollectOrderPacket(buf.readBlockPos(), buf.readUUID());
    }

    public static void handle(CollectOrderPacket msg, Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() -> {
            ServerPlayer player = ctx.get().getSender();
            if (player == null) return;

            BlockEntity be = player.level.getBlockEntity(msg.pos);
            if (!(be instanceof TradingPostBlockEntity tradingPost)) return;

            if (player.distanceToSqr(msg.pos.getX() + 0.5, msg.pos.getY() + 0.5,
                    msg.pos.getZ() + 0.5) > 64.0) return;

            tradingPost.collectBuyOrder(msg.orderId, player);
            player.inventoryMenu.broadcastChanges();
        });
        ctx.get().setPacketHandled(true);
    }
}
