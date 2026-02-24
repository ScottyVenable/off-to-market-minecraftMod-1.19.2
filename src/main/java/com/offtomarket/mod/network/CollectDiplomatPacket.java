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
 * Sent when player collects an arrived diplomat request.
 */
public class CollectDiplomatPacket {

    private final BlockPos pos;
    private final UUID requestId;

    public CollectDiplomatPacket(BlockPos pos, UUID requestId) {
        this.pos = pos;
        this.requestId = requestId;
    }

    public static void encode(CollectDiplomatPacket msg, FriendlyByteBuf buf) {
        buf.writeBlockPos(msg.pos);
        buf.writeUUID(msg.requestId);
    }

    public static CollectDiplomatPacket decode(FriendlyByteBuf buf) {
        return new CollectDiplomatPacket(buf.readBlockPos(), buf.readUUID());
    }

    public static void handle(CollectDiplomatPacket msg, Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() -> {
            ServerPlayer player = ctx.get().getSender();
            if (player == null) return;

            BlockEntity be = player.level.getBlockEntity(msg.pos);
            if (!(be instanceof TradingPostBlockEntity tradingPost)) return;

            if (player.distanceToSqr(msg.pos.getX() + 0.5, msg.pos.getY() + 0.5,
                    msg.pos.getZ() + 0.5) > 64.0) return;

            tradingPost.collectDiplomatRequest(msg.requestId, player);
            player.inventoryMenu.broadcastChanges();
        });
        ctx.get().setPacketHandled(true);
    }
}
