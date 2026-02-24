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
 * Sent when player delivers items for an accepted quest.
 */
public class DeliverQuestPacket {

    private final BlockPos pos;
    private final UUID questId;

    public DeliverQuestPacket(BlockPos pos, UUID questId) {
        this.pos = pos;
        this.questId = questId;
    }

    public static void encode(DeliverQuestPacket msg, FriendlyByteBuf buf) {
        buf.writeBlockPos(msg.pos);
        buf.writeUUID(msg.questId);
    }

    public static DeliverQuestPacket decode(FriendlyByteBuf buf) {
        return new DeliverQuestPacket(buf.readBlockPos(), buf.readUUID());
    }

    public static void handle(DeliverQuestPacket msg, Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() -> {
            ServerPlayer player = ctx.get().getSender();
            if (player == null) return;

            BlockEntity be = player.level.getBlockEntity(msg.pos);
            if (!(be instanceof TradingPostBlockEntity tradingPost)) return;

            if (player.distanceToSqr(msg.pos.getX() + 0.5, msg.pos.getY() + 0.5,
                    msg.pos.getZ() + 0.5) > 64.0) return;

            int delivered = tradingPost.deliverQuestItems(msg.questId, player);
            if (delivered > 0) {
                player.displayClientMessage(
                        net.minecraft.network.chat.Component.literal("Delivered " + delivered + " items!")
                                .withStyle(net.minecraft.ChatFormatting.GREEN), true);
            } else {
                player.displayClientMessage(
                        net.minecraft.network.chat.Component.literal("You don't have any required items!")
                                .withStyle(net.minecraft.ChatFormatting.RED), true);
            }
            player.inventoryMenu.broadcastChanges();
        });
        ctx.get().setPacketHandled(true);
    }
}
