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
 * (crafting tax, min markup, auto-price mode, price modifiers).
 */
public class UpdateBinSettingsPacket {
    private final BlockPos pos;
    private final int craftingTaxPercent;
    private final int minMarkupPercent;
    private final int autoPriceModeOrdinal;
    // Price modifiers
    private final boolean enchantedMarkupEnabled;
    private final int enchantedMarkupPercent;
    private final boolean usedDiscountEnabled;
    private final int usedDiscountPercent;
    private final boolean damagedDiscountEnabled;
    private final int damagedDiscountPercent;
    private final boolean rareMarkupEnabled;
    private final int rareMarkupPercent;

    public UpdateBinSettingsPacket(BlockPos pos, int craftingTaxPercent,
                                   int minMarkupPercent, int autoPriceModeOrdinal,
                                   boolean enchantedMarkupEnabled, int enchantedMarkupPercent,
                                   boolean usedDiscountEnabled, int usedDiscountPercent,
                                   boolean damagedDiscountEnabled, int damagedDiscountPercent,
                                   boolean rareMarkupEnabled, int rareMarkupPercent) {
        this.pos = pos;
        this.craftingTaxPercent = craftingTaxPercent;
        this.minMarkupPercent = minMarkupPercent;
        this.autoPriceModeOrdinal = autoPriceModeOrdinal;
        this.enchantedMarkupEnabled = enchantedMarkupEnabled;
        this.enchantedMarkupPercent = enchantedMarkupPercent;
        this.usedDiscountEnabled = usedDiscountEnabled;
        this.usedDiscountPercent = usedDiscountPercent;
        this.damagedDiscountEnabled = damagedDiscountEnabled;
        this.damagedDiscountPercent = damagedDiscountPercent;
        this.rareMarkupEnabled = rareMarkupEnabled;
        this.rareMarkupPercent = rareMarkupPercent;
    }

    public static void encode(UpdateBinSettingsPacket msg, FriendlyByteBuf buf) {
        buf.writeBlockPos(msg.pos);
        buf.writeInt(msg.craftingTaxPercent);
        buf.writeInt(msg.minMarkupPercent);
        buf.writeInt(msg.autoPriceModeOrdinal);
        buf.writeBoolean(msg.enchantedMarkupEnabled);
        buf.writeInt(msg.enchantedMarkupPercent);
        buf.writeBoolean(msg.usedDiscountEnabled);
        buf.writeInt(msg.usedDiscountPercent);
        buf.writeBoolean(msg.damagedDiscountEnabled);
        buf.writeInt(msg.damagedDiscountPercent);
        buf.writeBoolean(msg.rareMarkupEnabled);
        buf.writeInt(msg.rareMarkupPercent);
    }

    public static UpdateBinSettingsPacket decode(FriendlyByteBuf buf) {
        return new UpdateBinSettingsPacket(
                buf.readBlockPos(), buf.readInt(), buf.readInt(), buf.readInt(),
                buf.readBoolean(), buf.readInt(),
                buf.readBoolean(), buf.readInt(),
                buf.readBoolean(), buf.readInt(),
                buf.readBoolean(), buf.readInt());
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
                    // Price modifiers
                    tbbe.setEnchantedMarkupEnabled(msg.enchantedMarkupEnabled);
                    tbbe.setEnchantedMarkupPercent(msg.enchantedMarkupPercent);
                    tbbe.setUsedDiscountEnabled(msg.usedDiscountEnabled);
                    tbbe.setUsedDiscountPercent(msg.usedDiscountPercent);
                    tbbe.setDamagedDiscountEnabled(msg.damagedDiscountEnabled);
                    tbbe.setDamagedDiscountPercent(msg.damagedDiscountPercent);
                    tbbe.setRareMarkupEnabled(msg.rareMarkupEnabled);
                    tbbe.setRareMarkupPercent(msg.rareMarkupPercent);
                }
            }
        });
        ctx.get().setPacketHandled(true);
    }
}
