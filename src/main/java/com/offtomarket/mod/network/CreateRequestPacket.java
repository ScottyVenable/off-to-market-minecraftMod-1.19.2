package com.offtomarket.mod.network;

import com.offtomarket.mod.block.entity.TradingPostBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraftforge.network.NetworkEvent;
import net.minecraftforge.registries.ForgeRegistries;

import java.util.function.Supplier;

/**
 * Sent when a player creates a new item request from the Requests tab.
 * The server auto-selects the best town to fulfill the request.
 */
public class CreateRequestPacket {

    private final BlockPos pos;
    private final ResourceLocation itemId;
    private final int count;

    public CreateRequestPacket(BlockPos pos, ResourceLocation itemId, int count) {
        this.pos = pos;
        this.itemId = itemId;
        this.count = count;
    }

    public static void encode(CreateRequestPacket msg, FriendlyByteBuf buf) {
        buf.writeBlockPos(msg.pos);
        buf.writeResourceLocation(msg.itemId);
        buf.writeInt(msg.count);
    }

    public static CreateRequestPacket decode(FriendlyByteBuf buf) {
        return new CreateRequestPacket(
                buf.readBlockPos(),
                buf.readResourceLocation(),
                buf.readInt()
        );
    }

    public static void handle(CreateRequestPacket msg, Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() -> {
            ServerPlayer player = ctx.get().getSender();
            if (player == null) return;

            BlockEntity be = player.level.getBlockEntity(msg.pos);
            if (!(be instanceof TradingPostBlockEntity tradingPost)) return;

            if (player.distanceToSqr(msg.pos.getX() + 0.5, msg.pos.getY() + 0.5,
                    msg.pos.getZ() + 0.5) > 64.0) return;

            // Server-side validation: reject invalid counts
            if (msg.count <= 0 || msg.count > 64) return;

            // Server-side validation: reject unregistered items
            if (ForgeRegistries.ITEMS.getValue(msg.itemId) == null
                    || ForgeRegistries.ITEMS.getValue(msg.itemId) == net.minecraft.world.item.Items.AIR) return;

            tradingPost.createRequest(player, msg.itemId, msg.count);
            player.inventoryMenu.broadcastChanges();
        });
        ctx.get().setPacketHandled(true);
    }
}
