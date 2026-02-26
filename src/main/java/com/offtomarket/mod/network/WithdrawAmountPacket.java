package com.offtomarket.mod.network;

import com.offtomarket.mod.block.entity.FinanceTableBlockEntity;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

/**
 * Sent from client when the player clicks "Withdraw" on the Finance Table
 * with specific gold, silver, and copper amounts entered in the input fields.
 * The server validates the balance is sufficient, deducts the total CP,
 * and gives the player coins in the specified denominations.
 */
public class WithdrawAmountPacket {

    private final BlockPos pos;
    private final int gold;
    private final int silver;
    private final int copper;

    public WithdrawAmountPacket(BlockPos pos, int gold, int silver, int copper) {
        this.pos = pos;
        this.gold = gold;
        this.silver = silver;
        this.copper = copper;
    }

    public static void encode(WithdrawAmountPacket msg, FriendlyByteBuf buf) {
        buf.writeBlockPos(msg.pos);
        buf.writeVarInt(msg.gold);
        buf.writeVarInt(msg.silver);
        buf.writeVarInt(msg.copper);
    }

    public static WithdrawAmountPacket decode(FriendlyByteBuf buf) {
        return new WithdrawAmountPacket(
                buf.readBlockPos(), buf.readVarInt(), buf.readVarInt(), buf.readVarInt());
    }

    public static void handle(WithdrawAmountPacket msg, Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() -> {
            ServerPlayer player = ctx.get().getSender();
            if (player == null) return;

            if (player.distanceToSqr(msg.pos.getX() + 0.5,
                    msg.pos.getY() + 0.5, msg.pos.getZ() + 0.5) > 64.0) return;

            // Sanitise inputs
            if (msg.gold < 0 || msg.silver < 0 || msg.copper < 0) return;
            if (msg.gold == 0 && msg.silver == 0 && msg.copper == 0) return;

            BlockEntity be = player.level.getBlockEntity(msg.pos);
            if (be instanceof FinanceTableBlockEntity ftbe) {
                int cpNeeded = msg.gold * 100 + msg.silver * 10 + msg.copper;
                if (ftbe.getBalance() >= cpNeeded) {
                    ftbe.withdrawAmount(player, msg.gold, msg.silver, msg.copper);
                    StringBuilder sb = new StringBuilder("Withdrew: ");
                    if (msg.gold > 0)   sb.append("\u00A7e").append(msg.gold).append("g\u00A7r ");
                    if (msg.silver > 0) sb.append("\u00A77").append(msg.silver).append("s\u00A7r ");
                    if (msg.copper > 0) sb.append("\u00A76").append(msg.copper).append("c\u00A7r");
                    player.displayClientMessage(
                            Component.literal(sb.toString().trim())
                                    .withStyle(ChatFormatting.GREEN), true);
                } else {
                    player.displayClientMessage(
                            Component.literal("Insufficient balance!")
                                    .withStyle(ChatFormatting.RED), true);
                }
            }
        });
        ctx.get().setPacketHandled(true);
    }
}
