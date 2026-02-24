package com.offtomarket.mod.network;

import com.offtomarket.mod.block.entity.TradingBinBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

/**
 * Sent from client to update Trading Bin settings
 * (crafting tax, min markup, auto-price mode).
 */
public class UpdateBinSettingsPacket {
    private final BlockPos pos;
    private final int craftingTaxPercent;
    private final int minMarkupPercent;
    private final int autoPriceModeOrdinal;

    public UpdateBinSettingsPacket(BlockPos pos, int craftingTaxPercent,
                                   int minMarkupPercent, int autoPriceModeOrdinal) {
        this.pos = pos;
        this.craftingTaxPercent = craftingTaxPercent;
        this.minMarkupPercent = minMarkupPercent;
        this.autoPriceModeOrdinal = autoPriceModeOrdinal;
    }

    public static void encode(UpdateBinSettingsPacket msg, FriendlyByteBuf buf) {
        buf.writeBlockPos(msg.pos);
        buf.writeInt(msg.craftingTaxPercent);
        buf.writeInt(msg.minMarkupPercent);
        buf.writeInt(msg.autoPriceModeOrdinal);
    }

    public static UpdateBinSettingsPacket decode(FriendlyByteBuf buf) {
        return new UpdateBinSettingsPacket(
                buf.readBlockPos(), buf.readInt(), buf.readInt(), buf.readInt());
    }

    public static void handle(UpdateBinSettingsPacket msg, Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() -> {
            ServerPlayer player = ctx.get().getSender();
            if (player != null) {
                BlockEntity be = player.level.getBlockEntity(msg.pos);
                if (be instanceof TradingBinBlockEntity tbbe) {
                    tbbe.setCraftingTaxPercent(msg.craftingTaxPercent);
                    tbbe.setMinMarkupPercent(msg.minMarkupPercent);
                    TradingBinBlockEntity.AutoPriceMode[] modes =
                            TradingBinBlockEntity.AutoPriceMode.values();
                    if (msg.autoPriceModeOrdinal >= 0 && msg.autoPriceModeOrdinal < modes.length) {
                        tbbe.setAutoPriceMode(modes[msg.autoPriceModeOrdinal]);
                    }
                }
            }
        });
        ctx.get().setPacketHandled(true);
    }
}
