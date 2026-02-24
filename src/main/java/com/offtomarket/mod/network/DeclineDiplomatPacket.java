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
 * Sent when player declines a diplomat's proposed price.
 */
public class DeclineDiplomatPacket {

    private final BlockPos pos;
    private final UUID requestId;

    public DeclineDiplomatPacket(BlockPos pos, UUID requestId) {
        this.pos = pos;
        this.requestId = requestId;
    }

    public static void encode(DeclineDiplomatPacket msg, FriendlyByteBuf buf) {
        buf.writeBlockPos(msg.pos);
        buf.writeUUID(msg.requestId);
    }

    public static DeclineDiplomatPacket decode(FriendlyByteBuf buf) {
        return new DeclineDiplomatPacket(buf.readBlockPos(), buf.readUUID());
    }

    public static void handle(DeclineDiplomatPacket msg, Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() -> {
            ServerPlayer player = ctx.get().getSender();
            if (player == null) return;

            BlockEntity be = player.level.getBlockEntity(msg.pos);
            if (!(be instanceof TradingPostBlockEntity tradingPost)) return;

            if (player.distanceToSqr(msg.pos.getX() + 0.5, msg.pos.getY() + 0.5,
                    msg.pos.getZ() + 0.5) > 64.0) return;

            tradingPost.declineDiplomatProposal(msg.requestId);
        });
        ctx.get().setPacketHandled(true);
    }
}
