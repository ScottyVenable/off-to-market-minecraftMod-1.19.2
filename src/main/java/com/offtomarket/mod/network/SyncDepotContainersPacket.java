package com.offtomarket.mod.network;

import com.offtomarket.mod.client.render.ClientDepotTracker;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.network.NetworkEvent;

import java.util.HashMap;
import java.util.Map;
import java.util.function.Supplier;

public class SyncDepotContainersPacket {

    private final Map<BlockPos, String> assignments;

    public SyncDepotContainersPacket(Map<BlockPos, String> assignments) {
        this.assignments = assignments != null ? assignments : new HashMap<>();
    }

    public static void encode(SyncDepotContainersPacket msg, FriendlyByteBuf buf) {
        buf.writeInt(msg.assignments.size());
        for (Map.Entry<BlockPos, String> entry : msg.assignments.entrySet()) {
            buf.writeBlockPos(entry.getKey());
            buf.writeUtf(entry.getValue(), 64);
        }
    }

    public static SyncDepotContainersPacket decode(FriendlyByteBuf buf) {
        int size = buf.readInt();
        Map<BlockPos, String> map = new HashMap<>(size);
        for (int i = 0; i < size; i++) {
            BlockPos pos = buf.readBlockPos();
            String townId = buf.readUtf(64);
            map.put(pos, townId);
        }
        return new SyncDepotContainersPacket(map);
    }

    public static void handle(SyncDepotContainersPacket msg, Supplier<NetworkEvent.Context> ctxSupplier) {
        NetworkEvent.Context ctx = ctxSupplier.get();
        ctx.enqueueWork(() -> {
            ClientDepotTracker.update(msg.assignments);
        });
        ctx.setPacketHandled(true);
    }
}
