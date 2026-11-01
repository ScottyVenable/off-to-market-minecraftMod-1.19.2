package com.offtomarket.mod.network;

import com.offtomarket.mod.block.entity.MailboxBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraftforge.network.NetworkEvent;

import java.util.UUID;
import java.util.function.Supplier;

public class ReplyMailNotePacket {
    private final BlockPos pos;
    private final UUID noteId;
    private final int choiceIndex;

    public ReplyMailNotePacket(BlockPos pos, UUID noteId, int choiceIndex) {
        this.pos = pos;
        this.noteId = noteId;
        this.choiceIndex = choiceIndex;
    }

    public static void encode(ReplyMailNotePacket msg, FriendlyByteBuf buf) {
        buf.writeBlockPos(msg.pos);
        buf.writeUUID(msg.noteId);
        buf.writeInt(msg.choiceIndex);
    }

    public static ReplyMailNotePacket decode(FriendlyByteBuf buf) {
        return new ReplyMailNotePacket(buf.readBlockPos(), buf.readUUID(), buf.readInt());
    }

    public static void handle(ReplyMailNotePacket msg, Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() -> {
            ServerPlayer player = ctx.get().getSender();
            if (player != null) {
                BlockEntity be = player.level.getBlockEntity(msg.pos);
                if (be instanceof MailboxBlockEntity mailbox) {
                    mailbox.replyToNote(msg.noteId, msg.choiceIndex, player);
                }
            }
        });
        ctx.get().setPacketHandled(true);
    }
}
