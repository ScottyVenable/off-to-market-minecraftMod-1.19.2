package com.offtomarket.mod.network;

import com.offtomarket.mod.block.entity.TradingPostBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraftforge.network.NetworkEvent;

import java.util.List;
import java.util.function.Supplier;

/**
 * Sent from client when player clicks "Collect Coins" in the Trading Post.
 */
public class CollectCoinsPacket {
    private final BlockPos pos;

    public CollectCoinsPacket(BlockPos pos) {
        this.pos = pos;
    }

    public static void encode(CollectCoinsPacket msg, FriendlyByteBuf buf) {
        buf.writeBlockPos(msg.pos);
    }

    public static CollectCoinsPacket decode(FriendlyByteBuf buf) {
        return new CollectCoinsPacket(buf.readBlockPos());
    }

    public static void handle(CollectCoinsPacket msg, Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() -> {
            ServerPlayer player = ctx.get().getSender();
            if (player == null) return;
            
            // Distance check
            if (player.distanceToSqr(msg.pos.getX() + 0.5, msg.pos.getY() + 0.5,
                    msg.pos.getZ() + 0.5) > 64.0) return;
            
            BlockEntity be = player.level.getBlockEntity(msg.pos);
            if (be instanceof TradingPostBlockEntity tpbe) {
                List<ItemStack> coins = tpbe.collectCoins();
                for (ItemStack coin : coins) {
                    if (!player.getInventory().add(coin)) {
                        player.drop(coin, false);
                    }
                }
                if (!coins.isEmpty()) {
                    player.displayClientMessage(
                            net.minecraft.network.chat.Component.literal("Coins collected!")
                                    .withStyle(net.minecraft.ChatFormatting.GOLD), true);
                    com.offtomarket.mod.util.SoundHelper.playCoinCollect(player.level, player);
                } else {
                    player.displayClientMessage(
                            net.minecraft.network.chat.Component.literal("No coins to collect!")
                                    .withStyle(net.minecraft.ChatFormatting.GRAY), true);
                }
            }
        });
        ctx.get().setPacketHandled(true);
    }
}
