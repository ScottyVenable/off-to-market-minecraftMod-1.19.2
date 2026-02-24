package com.offtomarket.mod.network;

import com.offtomarket.mod.block.entity.TradingPostBlockEntity;
import com.offtomarket.mod.data.Worker;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

/**
 * Sent when player fires (dismisses) a worker.
 * The player receives a partial refund (50% of hire cost).
 */
public class FireWorkerPacket {

    private final BlockPos pos;
    private final String workerType;

    public FireWorkerPacket(BlockPos pos, Worker.WorkerType type) {
        this.pos = pos;
        this.workerType = type.name();
    }

    private FireWorkerPacket(BlockPos pos, String workerType) {
        this.pos = pos;
        this.workerType = workerType;
    }

    public static void encode(FireWorkerPacket msg, FriendlyByteBuf buf) {
        buf.writeBlockPos(msg.pos);
        buf.writeUtf(msg.workerType);
    }

    public static FireWorkerPacket decode(FriendlyByteBuf buf) {
        return new FireWorkerPacket(buf.readBlockPos(), buf.readUtf());
    }

    public static void handle(FireWorkerPacket msg, Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() -> {
            ServerPlayer player = ctx.get().getSender();
            if (player == null) return;

            BlockEntity be = player.level.getBlockEntity(msg.pos);
            if (!(be instanceof TradingPostBlockEntity tradingPost)) return;

            if (player.distanceToSqr(msg.pos.getX() + 0.5, msg.pos.getY() + 0.5,
                    msg.pos.getZ() + 0.5) > 64.0) return;

            try {
                Worker.WorkerType type = Worker.WorkerType.valueOf(msg.workerType);
                tradingPost.fireWorker(type, player);
                player.inventoryMenu.broadcastChanges();
            } catch (IllegalArgumentException ignored) {
                // Invalid worker type
            }
        });
        ctx.get().setPacketHandled(true);
    }
}
