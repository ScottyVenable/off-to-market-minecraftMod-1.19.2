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
 * Sent from client when the player clicks "Deposit" on the Finance Table
 * with specific gold, silver, and copper counts entered in the input fields.
 * The server verifies the player's inventory contains the exact coin items,
 * removes them, and adds the total CP to the Finance Table balance.
 */
public class DepositAmountPacket {

    private final BlockPos pos;
    private final int gold;
    private final int silver;
    private final int copper;

    public DepositAmountPacket(BlockPos pos, int gold, int silver, int copper) {
        this.pos = pos;
        this.gold = gold;
        this.silver = silver;
        this.copper = copper;
    }

    public static void encode(DepositAmountPacket msg, FriendlyByteBuf buf) {
        buf.writeBlockPos(msg.pos);
        buf.writeVarInt(msg.gold);
        buf.writeVarInt(msg.silver);
        buf.writeVarInt(msg.copper);
    }

    public static DepositAmountPacket decode(FriendlyByteBuf buf) {
        return new DepositAmountPacket(
                buf.readBlockPos(), buf.readVarInt(), buf.readVarInt(), buf.readVarInt());
    }

    public static void handle(DepositAmountPacket msg, Supplier<NetworkEvent.Context> ctx) {
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
                int deposited = ftbe.depositAmount(player, msg.gold, msg.silver, msg.copper);
                if (deposited > 0) {
                    int gp = deposited / 100;
                    int sp = (deposited % 100) / 10;
                    int cp = deposited % 10;
                    StringBuilder sb = new StringBuilder("Deposited: ");
                    if (gp > 0) sb.append("\u00A7e").append(gp).append("g\u00A7r ");
                    if (sp > 0) sb.append("\u00A77").append(sp).append("s\u00A7r ");
                    if (cp > 0) sb.append("\u00A76").append(cp).append("c\u00A7r");
                    player.displayClientMessage(
                            Component.literal(sb.toString().trim())
                                    .withStyle(ChatFormatting.GREEN), true);
                } else {
                    player.displayClientMessage(
                            Component.literal("Not enough coins in inventory!")
                                    .withStyle(ChatFormatting.RED), true);
                }
            }
        });
        ctx.get().setPacketHandled(true);
    }
}
