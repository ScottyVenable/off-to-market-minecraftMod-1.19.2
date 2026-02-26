package com.offtomarket.mod.network;

import com.offtomarket.mod.block.entity.FinanceTableBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

/**
 * Sent from client when the player clicks "Withdraw All" on the Finance Table.
 * The server converts the full stored balance to coin item stacks and gives
 * them to the player (any overflow is dropped at the player's feet).
 */
public class WithdrawCoinsPacket {

    private final BlockPos pos;

    public WithdrawCoinsPacket(BlockPos pos) {
        this.pos = pos;
    }

    public static void encode(WithdrawCoinsPacket msg, FriendlyByteBuf buf) {
        buf.writeBlockPos(msg.pos);
    }

    public static WithdrawCoinsPacket decode(FriendlyByteBuf buf) {
        return new WithdrawCoinsPacket(buf.readBlockPos());
    }

    public static void handle(WithdrawCoinsPacket msg, Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() -> {
            ServerPlayer player = ctx.get().getSender();
            if (player == null) return;

            // Distance check
            if (player.distanceToSqr(msg.pos.getX() + 0.5,
                    msg.pos.getY() + 0.5, msg.pos.getZ() + 0.5) > 64.0) return;

            BlockEntity be = player.level.getBlockEntity(msg.pos);
            if (be instanceof FinanceTableBlockEntity ftbe) {
                if (ftbe.getBalance() > 0) {
                    ftbe.withdraw(player);
                    player.displayClientMessage(
                            net.minecraft.network.chat.Component.literal("Coins withdrawn!")
                                    .withStyle(net.minecraft.ChatFormatting.GOLD), true);
                } else {
                    player.displayClientMessage(
                            net.minecraft.network.chat.Component.literal("No balance to withdraw.")
                                    .withStyle(net.minecraft.ChatFormatting.GRAY), true);
                }
            }
        });
        ctx.get().setPacketHandled(true);
    }
}
