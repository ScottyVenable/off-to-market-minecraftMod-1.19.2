package com.offtomarket.mod.network;

import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

/**
 * Packet sent from server to client with the result of a cart checkout attempt.
 * <p>
 * If checkout succeeded, the client clears the cart and removes persisted cart state.
 * If checkout failed, the client preserves the cart intact so the player does not
 * lose their staged items and quantities.
 */
public class CartCheckoutResultPacket {
    private final BlockPos marketBoardPos;
    private final boolean success;
    private final String message;

    public CartCheckoutResultPacket(BlockPos marketBoardPos, boolean success, String message) {
        this.marketBoardPos = marketBoardPos;
        this.success = success;
        this.message = message != null ? message : "";
    }

    public CartCheckoutResultPacket(FriendlyByteBuf buf) {
        this.marketBoardPos = buf.readBlockPos();
        this.success = buf.readBoolean();
        this.message = buf.readUtf(256);
    }

    public void encode(FriendlyByteBuf buf) {
        buf.writeBlockPos(marketBoardPos);
        buf.writeBoolean(success);
        buf.writeUtf(message, 256);
    }

    public void handle(Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() -> {
            DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () ->
                    com.offtomarket.mod.client.screen.MarketBoardScreen.handleCheckoutResult(
                            marketBoardPos, success, message));
        });
        ctx.get().setPacketHandled(true);
    }

    public BlockPos getMarketBoardPos() {
        return marketBoardPos;
    }

    public boolean isSuccess() {
        return success;
    }

    public String getMessage() {
        return message;
    }
}
