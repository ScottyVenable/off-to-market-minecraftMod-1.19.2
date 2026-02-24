package com.offtomarket.mod.network;

import com.offtomarket.mod.block.entity.TradingPostBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

/**
 * Sent from client to update min/max distance settings on the Trading Post.
 */
public class SetDistancePacket {
    private final BlockPos pos;
    private final int minDistance;
    private final int maxDistance;

    public SetDistancePacket(BlockPos pos, int minDistance, int maxDistance) {
        this.pos = pos;
        this.minDistance = minDistance;
        this.maxDistance = maxDistance;
    }

    public static void encode(SetDistancePacket msg, FriendlyByteBuf buf) {
        buf.writeBlockPos(msg.pos);
        buf.writeInt(msg.minDistance);
        buf.writeInt(msg.maxDistance);
    }

    public static SetDistancePacket decode(FriendlyByteBuf buf) {
        return new SetDistancePacket(buf.readBlockPos(), buf.readInt(), buf.readInt());
    }

    public static void handle(SetDistancePacket msg, Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() -> {
            ServerPlayer player = ctx.get().getSender();
            if (player != null) {
                BlockEntity be = player.level.getBlockEntity(msg.pos);
                if (be instanceof TradingPostBlockEntity tpbe) {
                    tpbe.setMinDistance(msg.minDistance);
                    tpbe.setMaxDistance(msg.maxDistance);
                }
            }
        });
        ctx.get().setPacketHandled(true);
    }
}
