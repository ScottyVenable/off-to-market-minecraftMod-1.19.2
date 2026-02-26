package com.offtomarket.mod.network;

import com.offtomarket.mod.block.entity.TradingLedgerBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

/**
 * Sent from client to purchase a caravan weight capacity upgrade for a Trading Bin.
 */
public class UpgradeCaravanWeightPacket {
    private final BlockPos pos;

    public UpgradeCaravanWeightPacket(BlockPos pos) {
        this.pos = pos;
    }

    public static void encode(UpgradeCaravanWeightPacket msg, FriendlyByteBuf buf) {
        buf.writeBlockPos(msg.pos);
    }

    public static UpgradeCaravanWeightPacket decode(FriendlyByteBuf buf) {
        return new UpgradeCaravanWeightPacket(buf.readBlockPos());
    }

    public static void handle(UpgradeCaravanWeightPacket msg, Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() -> {
            ServerPlayer player = ctx.get().getSender();
            if (player == null) return;

            BlockEntity be = player.level.getBlockEntity(msg.pos);
            if (be instanceof TradingLedgerBlockEntity tbbe) {
                tbbe.upgradeCaravanWeight(player);
            }
        });
        ctx.get().setPacketHandled(true);
    }
}
