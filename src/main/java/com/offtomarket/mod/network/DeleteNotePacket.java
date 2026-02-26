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
 * Packet sent from client to server to delete a note.
 */
public class DeleteNotePacket {
    private final BlockPos pos;
    private final UUID noteId;

    public DeleteNotePacket(BlockPos pos, UUID noteId) {
        this.pos = pos;
        this.noteId = noteId;
    }

    public static void encode(DeleteNotePacket msg, FriendlyByteBuf buf) {
        buf.writeBlockPos(msg.pos);
        buf.writeUUID(msg.noteId);
    }

    public static DeleteNotePacket decode(FriendlyByteBuf buf) {
        return new DeleteNotePacket(buf.readBlockPos(), buf.readUUID());
    }

    public static void handle(DeleteNotePacket msg, Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() -> {
            ServerPlayer player = ctx.get().getSender();
            if (player == null) return;
            BlockEntity be = player.level.getBlockEntity(msg.pos);
            if (!(be instanceof MailboxBlockEntity mailbox)) return;
            if (player.distanceToSqr(msg.pos.getX() + 0.5, msg.pos.getY() + 0.5, msg.pos.getZ() + 0.5) > 64.0) return;
            mailbox.deleteNote(msg.noteId);
        });
        ctx.get().setPacketHandled(true);
    }
}
