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
 * Sent from client to adjust the price of a specific item in an AT_MARKET shipment.
 * Allows the player to change prices on the fly to improve sale chances.
 */
public class AdjustShipmentPricePacket {

    private final BlockPos pos;
    private final UUID shipmentId;
    private final int itemIndex;
    private final int newPrice;

    public AdjustShipmentPricePacket(BlockPos pos, UUID shipmentId, int itemIndex, int newPrice) {
        this.pos = pos;
        this.shipmentId = shipmentId;
        this.itemIndex = itemIndex;
        this.newPrice = newPrice;
    }

    public static void encode(AdjustShipmentPricePacket msg, FriendlyByteBuf buf) {
        buf.writeBlockPos(msg.pos);
        buf.writeUUID(msg.shipmentId);
        buf.writeInt(msg.itemIndex);
        buf.writeInt(msg.newPrice);
    }

    public static AdjustShipmentPricePacket decode(FriendlyByteBuf buf) {
        return new AdjustShipmentPricePacket(
                buf.readBlockPos(), buf.readUUID(), buf.readInt(), buf.readInt());
    }

    public static void handle(AdjustShipmentPricePacket msg, Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() -> {
            ServerPlayer player = ctx.get().getSender();
            if (player == null) return;

            BlockEntity be = player.level.getBlockEntity(msg.pos);
            if (!(be instanceof TradingPostBlockEntity tradingPost)) return;

            if (player.distanceToSqr(msg.pos.getX() + 0.5, msg.pos.getY() + 0.5,
                    msg.pos.getZ() + 0.5) > 64.0) return;

            tradingPost.adjustShipmentPrice(msg.shipmentId, msg.itemIndex, msg.newPrice);
        });
        ctx.get().setPacketHandled(true);
    }
}
