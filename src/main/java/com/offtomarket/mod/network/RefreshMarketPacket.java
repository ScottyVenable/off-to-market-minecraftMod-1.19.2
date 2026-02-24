package com.offtomarket.mod.network;

import com.offtomarket.mod.block.entity.MarketBoardBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

/**
 * Sent from client when the player clicks "Refresh" on the Market Board.
 * Triggers server-side listing regeneration and syncs results to client.
 */
public class RefreshMarketPacket {
    private final BlockPos pos;

    public RefreshMarketPacket(BlockPos pos) {
        this.pos = pos;
    }

    public static void encode(RefreshMarketPacket msg, FriendlyByteBuf buf) {
        buf.writeBlockPos(msg.pos);
    }

    public static RefreshMarketPacket decode(FriendlyByteBuf buf) {
        return new RefreshMarketPacket(buf.readBlockPos());
    }

    public static void handle(RefreshMarketPacket msg, Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() -> {
            ServerPlayer player = ctx.get().getSender();
            if (player != null) {
                BlockEntity be = player.level.getBlockEntity(msg.pos);
                if (be instanceof MarketBoardBlockEntity mbbe) {
                    mbbe.refreshListings();
                }
            }
        });
        ctx.get().setPacketHandled(true);
    }
}
