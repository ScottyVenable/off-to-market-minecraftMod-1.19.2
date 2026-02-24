package com.offtomarket.mod.network;

import com.offtomarket.mod.block.entity.TradingPostBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

/**
 * Sent from client to server when the player clicks "Send to Market" in the Trading Post.
 */
public class SendShipmentPacket {
    private final BlockPos pos;

    public SendShipmentPacket(BlockPos pos) {
        this.pos = pos;
    }

    public static void encode(SendShipmentPacket msg, FriendlyByteBuf buf) {
        buf.writeBlockPos(msg.pos);
    }

    public static SendShipmentPacket decode(FriendlyByteBuf buf) {
        return new SendShipmentPacket(buf.readBlockPos());
    }

    public static void handle(SendShipmentPacket msg, Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() -> {
            ServerPlayer player = ctx.get().getSender();
            if (player == null) return;
            
            // Distance check
            if (player.distanceToSqr(msg.pos.getX() + 0.5, msg.pos.getY() + 0.5,
                    msg.pos.getZ() + 0.5) > 64.0) return;
            
            BlockEntity be = player.level.getBlockEntity(msg.pos);
            if (be instanceof TradingPostBlockEntity tpbe) {
                boolean success = tpbe.sendItemsToMarket(player.level, msg.pos);
                if (success) {
                    player.displayClientMessage(
                            net.minecraft.network.chat.Component.literal("Items dispatched to market!")
                                    .withStyle(net.minecraft.ChatFormatting.GREEN), true);
                    com.offtomarket.mod.util.SoundHelper.playShipmentSend(player.level, msg.pos);
                } else {
                    player.displayClientMessage(
                            net.minecraft.network.chat.Component.literal("No items to send or no Trading Bin found nearby!")
                                    .withStyle(net.minecraft.ChatFormatting.RED), true);
                }
            }
        });
        ctx.get().setPacketHandled(true);
    }
}
