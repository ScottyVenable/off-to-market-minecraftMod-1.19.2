package com.offtomarket.mod.network;

import com.offtomarket.mod.content.CustomMenuDefinition;
import com.offtomarket.mod.content.CustomMenuRegistry;
import net.minecraft.client.Minecraft;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

/**
 * Server → Client packet that tells the client to open a custom menu screen.
 *
 * Sent by the /otm menu open command on the server.
 * The client looks up the menu ID in CustomMenuRegistry and opens CustomMenuScreen.
 */
public class OpenCustomMenuPacket {

    private final String menuId;

    public OpenCustomMenuPacket(String menuId) {
        this.menuId = menuId;
    }

    public static void encode(OpenCustomMenuPacket msg, FriendlyByteBuf buf) {
        buf.writeUtf(msg.menuId, 128);
    }

    public static OpenCustomMenuPacket decode(FriendlyByteBuf buf) {
        return new OpenCustomMenuPacket(buf.readUtf(128));
    }

    public static void handle(OpenCustomMenuPacket msg, Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() ->
            DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () -> openOnClient(msg.menuId))
        );
        ctx.get().setPacketHandled(true);
    }

    /** Called only on the physical client. */
    private static void openOnClient(String menuId) {
        CustomMenuDefinition def = CustomMenuRegistry.get(menuId);
        if (def == null) {
            Minecraft.getInstance().player.sendSystemMessage(
                net.minecraft.network.chat.Component.literal(
                    "[OTM] No custom menu found with id: " + menuId));
            return;
        }
        Minecraft.getInstance().setScreen(
            new com.offtomarket.mod.client.screen.CustomMenuScreen(def));
    }
}
