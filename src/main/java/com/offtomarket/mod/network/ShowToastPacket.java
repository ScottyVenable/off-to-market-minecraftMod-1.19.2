package com.offtomarket.mod.network;

import com.offtomarket.mod.client.NotificationToastRenderer;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

/**
 * Packet sent from server to client to display a notification toast.
 */
public class ShowToastPacket {
    private final String title;
    private final String message;
    private final int typeOrdinal;
    
    public ShowToastPacket(String title, String message, NotificationToastRenderer.ToastType type) {
        this.title = title;
        this.message = message;
        this.typeOrdinal = type.ordinal();
    }
    
    public ShowToastPacket(FriendlyByteBuf buf) {
        this.title = buf.readUtf(128);
        this.message = buf.readUtf(256);
        this.typeOrdinal = buf.readVarInt();
    }
    
    public void encode(FriendlyByteBuf buf) {
        buf.writeUtf(title, 128);
        buf.writeUtf(message, 256);
        buf.writeVarInt(typeOrdinal);
    }
    
    public void handle(Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() -> {
            // This runs on the client
            NotificationToastRenderer.ToastType type = 
                    NotificationToastRenderer.ToastType.values()[typeOrdinal];
            NotificationToastRenderer.addToast(title, message, type);
        });
        ctx.get().setPacketHandled(true);
    }
}
