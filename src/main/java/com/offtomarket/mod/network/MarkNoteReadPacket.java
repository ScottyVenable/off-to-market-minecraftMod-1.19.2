package com.offtomarket.mod.network;

import com.offtomarket.mod.block.entity.MailboxBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraftforge.network.NetworkEvent;

import java.util.UUID;
import java.util.function.Supplier;

/**
 * Packet sent from client to server to mark a note as read.
 */
public class MarkNoteReadPacket {
    private final BlockPos pos;
    private final UUID noteId;

    public MarkNoteReadPacket(BlockPos pos, UUID noteId) {
        this.pos = pos;
        this.noteId = noteId;
    }

    public static void encode(MarkNoteReadPacket msg, FriendlyByteBuf buf) {
        buf.writeBlockPos(msg.pos);
        buf.writeUUID(msg.noteId);
    }

    public static MarkNoteReadPacket decode(FriendlyByteBuf buf) {
        return new MarkNoteReadPacket(buf.readBlockPos(), buf.readUUID());
    }

    public static void handle(MarkNoteReadPacket msg, Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() -> {
            ServerPlayer player = ctx.get().getSender();
            if (player == null) return;
            BlockEntity be = player.level.getBlockEntity(msg.pos);
            if (!(be instanceof MailboxBlockEntity mailbox)) return;
            if (player.distanceToSqr(msg.pos.getX() + 0.5, msg.pos.getY() + 0.5, msg.pos.getZ() + 0.5) > 64.0) return;
            mailbox.markRead(msg.noteId);
        });
        ctx.get().setPacketHandled(true);
    }
}
