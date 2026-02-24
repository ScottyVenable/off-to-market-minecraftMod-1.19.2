package com.offtomarket.mod.network;

import com.offtomarket.mod.menu.TradingPostMenu;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

/**
 * Sent from client when the player clicks Convert Up/Down in the Coins tab.
 * Converts all coins in the 3 exchange slots to the next tier up or down.
 */
public class CoinSlotConvertPacket {
    private final boolean convertUp;

    public CoinSlotConvertPacket(boolean convertUp) {
        this.convertUp = convertUp;
    }

    public static void encode(CoinSlotConvertPacket msg, FriendlyByteBuf buf) {
        buf.writeBoolean(msg.convertUp);
    }

    public static CoinSlotConvertPacket decode(FriendlyByteBuf buf) {
        return new CoinSlotConvertPacket(buf.readBoolean());
    }

    public static void handle(CoinSlotConvertPacket msg, Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() -> {
            ServerPlayer player = ctx.get().getSender();
            if (player == null) return;

            if (player.containerMenu instanceof TradingPostMenu menu) {
                menu.convertCoinSlots(msg.convertUp, player);
            }
        });
        ctx.get().setPacketHandled(true);
    }
}
