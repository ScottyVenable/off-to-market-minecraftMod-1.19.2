package com.offtomarket.mod.network;

import com.offtomarket.mod.block.entity.TradingPostBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

/**
 * Sent from client when a player selects a different town in the Trading Post UI.
 */
public class SelectTownPacket {
    private final BlockPos pos;
    private final String townId;

    public SelectTownPacket(BlockPos pos, String townId) {
        this.pos = pos;
        this.townId = townId;
    }

    public static void encode(SelectTownPacket msg, FriendlyByteBuf buf) {
        buf.writeBlockPos(msg.pos);
        buf.writeUtf(msg.townId);
    }

    public static SelectTownPacket decode(FriendlyByteBuf buf) {
        return new SelectTownPacket(buf.readBlockPos(), buf.readUtf());
    }

    public static void handle(SelectTownPacket msg, Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() -> {
            ServerPlayer player = ctx.get().getSender();
            if (player != null) {
                BlockEntity be = player.level.getBlockEntity(msg.pos);
                if (be instanceof TradingPostBlockEntity tpbe) {
                    tpbe.setSelectedTownId(msg.townId);
                }
            }
        });
        ctx.get().setPacketHandled(true);
    }
}
