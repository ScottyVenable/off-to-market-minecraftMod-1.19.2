package com.offtomarket.mod.network;

import com.offtomarket.mod.block.entity.FinanceTableBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

/**
 * Sent from client when the player clicks "Deposit All" on the Finance Table.
 * The server scans the player's full inventory for coin items, removes them,
 * and adds their total CP value to the Finance Table's stored balance.
 */
public class DepositCoinsPacket {

    private final BlockPos pos;

    public DepositCoinsPacket(BlockPos pos) {
        this.pos = pos;
    }

    public static void encode(DepositCoinsPacket msg, FriendlyByteBuf buf) {
        buf.writeBlockPos(msg.pos);
    }

    public static DepositCoinsPacket decode(FriendlyByteBuf buf) {
        return new DepositCoinsPacket(buf.readBlockPos());
    }

    public static void handle(DepositCoinsPacket msg, Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() -> {
            ServerPlayer player = ctx.get().getSender();
            if (player == null) return;

            // Distance check
            if (player.distanceToSqr(msg.pos.getX() + 0.5,
                    msg.pos.getY() + 0.5, msg.pos.getZ() + 0.5) > 64.0) return;

            BlockEntity be = player.level.getBlockEntity(msg.pos);
            if (be instanceof FinanceTableBlockEntity ftbe) {
                int before = ftbe.getBalance();
                ftbe.deposit(player);
                int deposited = ftbe.getBalance() - before;
                if (deposited > 0) {
                    int gp = deposited / 100;
                    int sp = (deposited % 100) / 10;
                    int cp = deposited % 10;
                    StringBuilder sb = new StringBuilder("Deposited: ");
                    if (gp > 0) sb.append("\u00A7e").append(gp).append("g\u00A7r ");
                    if (sp > 0) sb.append("\u00A77").append(sp).append("s\u00A7r ");
                    if (cp > 0) sb.append("\u00A76").append(cp).append("c\u00A7r");
                    player.displayClientMessage(
                            net.minecraft.network.chat.Component.literal(sb.toString().trim())
                                    .withStyle(net.minecraft.ChatFormatting.GREEN), true);
                } else {
                    player.displayClientMessage(
                            net.minecraft.network.chat.Component.literal("No coins to deposit.")
                                    .withStyle(net.minecraft.ChatFormatting.GRAY), true);
                }
            }
        });
        ctx.get().setPacketHandled(true);
    }
}
