package com.offtomarket.mod.network;

import com.offtomarket.mod.block.entity.TradingPostBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraftforge.network.NetworkEvent;

import java.util.UUID;
import java.util.function.Supplier;

/**
 * Sent when player accepts a quest from the Quests tab.
 */
public class AcceptQuestPacket {

    private final BlockPos pos;
    private final UUID questId;

    public AcceptQuestPacket(BlockPos pos, UUID questId) {
        this.pos = pos;
        this.questId = questId;
    }

    public static void encode(AcceptQuestPacket msg, FriendlyByteBuf buf) {
        buf.writeBlockPos(msg.pos);
        buf.writeUUID(msg.questId);
    }

    public static AcceptQuestPacket decode(FriendlyByteBuf buf) {
        return new AcceptQuestPacket(buf.readBlockPos(), buf.readUUID());
    }

    public static void handle(AcceptQuestPacket msg, Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() -> {
            ServerPlayer player = ctx.get().getSender();
            if (player == null) return;

            BlockEntity be = player.level.getBlockEntity(msg.pos);
            if (!(be instanceof TradingPostBlockEntity tradingPost)) return;

            if (player.distanceToSqr(msg.pos.getX() + 0.5, msg.pos.getY() + 0.5,
                    msg.pos.getZ() + 0.5) > 64.0) return;

            boolean success = tradingPost.acceptQuest(msg.questId);
            if (success) {
                player.displayClientMessage(
                        net.minecraft.network.chat.Component.literal("Quest accepted!")
                                .withStyle(net.minecraft.ChatFormatting.GREEN), true);
                com.offtomarket.mod.util.SoundHelper.playQuestAccept(player.level, msg.pos);
            } else {
                player.displayClientMessage(
                        net.minecraft.network.chat.Component.literal("Quest no longer available!")
                                .withStyle(net.minecraft.ChatFormatting.RED), true);
            }
        });
        ctx.get().setPacketHandled(true);
    }
}
