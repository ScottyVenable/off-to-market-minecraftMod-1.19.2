package com.offtomarket.mod.network;

import com.offtomarket.mod.block.entity.TradingPostBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

/**
 * Sent by the client when the player dispatches the Stock Scout to a town.
 * The server validates and performs the dispatch.
 */
public class DispatchScoutPacket {

    private final BlockPos pos;
    private final String townId;

    public DispatchScoutPacket(BlockPos pos, String townId) {
        this.pos = pos;
        this.townId = townId;
    }

    public static void encode(DispatchScoutPacket msg, FriendlyByteBuf buf) {
        buf.writeBlockPos(msg.pos);
        buf.writeUtf(msg.townId);
    }

    public static DispatchScoutPacket decode(FriendlyByteBuf buf) {
        return new DispatchScoutPacket(buf.readBlockPos(), buf.readUtf());
    }

    public static void handle(DispatchScoutPacket msg, Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() -> {
            ServerPlayer player = ctx.get().getSender();
            if (player == null) return;

            BlockEntity be = player.level.getBlockEntity(msg.pos);
            if (!(be instanceof TradingPostBlockEntity tradingPost)) return;

            // Range check
            if (player.distanceToSqr(msg.pos.getX() + 0.5, msg.pos.getY() + 0.5,
                    msg.pos.getZ() + 0.5) > 64.0) return;

            tradingPost.dispatchScout(msg.townId, player);
            player.inventoryMenu.broadcastChanges();
        });
        ctx.get().setPacketHandled(true);
    }
}
