package com.offtomarket.mod.network;

import com.offtomarket.mod.block.entity.TradingPostBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

/**
 * Sent when player sends a diplomat to request specific items from a town.
 */
public class SendDiplomatPacket {

    private final BlockPos pos;
    private final String townId;
    private final ResourceLocation itemId;
    private final int count;

    public SendDiplomatPacket(BlockPos pos, String townId, ResourceLocation itemId, int count) {
        this.pos = pos;
        this.townId = townId;
        this.itemId = itemId;
        this.count = count;
    }

    public static void encode(SendDiplomatPacket msg, FriendlyByteBuf buf) {
        buf.writeBlockPos(msg.pos);
        buf.writeUtf(msg.townId);
        buf.writeResourceLocation(msg.itemId);
        buf.writeInt(msg.count);
    }

    public static SendDiplomatPacket decode(FriendlyByteBuf buf) {
        return new SendDiplomatPacket(
                buf.readBlockPos(),
                buf.readUtf(),
                buf.readResourceLocation(),
                buf.readInt()
        );
    }

    public static void handle(SendDiplomatPacket msg, Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() -> {
            ServerPlayer player = ctx.get().getSender();
            if (player == null) return;

            BlockEntity be = player.level.getBlockEntity(msg.pos);
            if (!(be instanceof TradingPostBlockEntity tradingPost)) return;

            if (player.distanceToSqr(msg.pos.getX() + 0.5, msg.pos.getY() + 0.5,
                    msg.pos.getZ() + 0.5) > 64.0) return;

            tradingPost.sendDiplomat(player, msg.townId, msg.itemId, msg.count);
            player.inventoryMenu.broadcastChanges();
        });
        ctx.get().setPacketHandled(true);
    }
}
