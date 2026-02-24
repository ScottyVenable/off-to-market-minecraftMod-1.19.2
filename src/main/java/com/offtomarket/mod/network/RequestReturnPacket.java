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
 * Sent from client when player requests to return a shipment from the market.
 */
public class RequestReturnPacket {
    private final BlockPos pos;
    private final UUID shipmentId;

    public RequestReturnPacket(BlockPos pos, UUID shipmentId) {
        this.pos = pos;
        this.shipmentId = shipmentId;
    }

    public static void encode(RequestReturnPacket msg, FriendlyByteBuf buf) {
        buf.writeBlockPos(msg.pos);
        buf.writeUUID(msg.shipmentId);
    }

    public static RequestReturnPacket decode(FriendlyByteBuf buf) {
        return new RequestReturnPacket(buf.readBlockPos(), buf.readUUID());
    }

    public static void handle(RequestReturnPacket msg, Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() -> {
            ServerPlayer player = ctx.get().getSender();
            if (player != null) {
                BlockEntity be = player.level.getBlockEntity(msg.pos);
                if (be instanceof TradingPostBlockEntity tpbe) {
                    boolean success = tpbe.requestReturn(msg.shipmentId);
                    if (success) {
                        player.displayClientMessage(
                                net.minecraft.network.chat.Component.literal("Return requested! Items are being shipped back.")
                                        .withStyle(net.minecraft.ChatFormatting.YELLOW), true);
                    }
                }
            }
        });
        ctx.get().setPacketHandled(true);
    }
}
