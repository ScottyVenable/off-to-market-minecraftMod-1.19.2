package com.offtomarket.mod.network;

import com.offtomarket.mod.block.entity.MailboxBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

/**
 * Packet sent from client to server to delete all read notes.
 */
public class DeleteAllReadNotesPacket {
    private final BlockPos pos;

    public DeleteAllReadNotesPacket(BlockPos pos) {
        this.pos = pos;
    }

    public static void encode(DeleteAllReadNotesPacket msg, FriendlyByteBuf buf) {
        buf.writeBlockPos(msg.pos);
    }

    public static DeleteAllReadNotesPacket decode(FriendlyByteBuf buf) {
        return new DeleteAllReadNotesPacket(buf.readBlockPos());
    }

    public static void handle(DeleteAllReadNotesPacket msg, Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() -> {
            ServerPlayer player = ctx.get().getSender();
            if (player == null) return;
            BlockEntity be = player.level.getBlockEntity(msg.pos);
            if (!(be instanceof MailboxBlockEntity mailbox)) return;
            if (player.distanceToSqr(msg.pos.getX() + 0.5, msg.pos.getY() + 0.5, msg.pos.getZ() + 0.5) > 64.0) return;
            mailbox.deleteAllRead();
        });
        ctx.get().setPacketHandled(true);
    }
}
