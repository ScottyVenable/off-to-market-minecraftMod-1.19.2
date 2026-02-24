package com.offtomarket.mod.debug;

import com.mojang.blaze3d.vertex.PoseStack;
import com.offtomarket.mod.client.screen.CoinRenderer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiComponent;
import net.minecraftforge.client.event.RenderGuiOverlayEvent;
import net.minecraftforge.client.gui.overlay.VanillaGuiOverlay;
import net.minecraftforge.eventbus.api.SubscribeEvent;

/**
 * Debug HUD overlay rendered in the top-left corner.
 * Shows live state from DebugConfig watch variables.
 * Toggle with /otm debug hud.
 */
public class DebugOverlay {

    @SubscribeEvent
    public void onRenderOverlay(RenderGuiOverlayEvent.Post event) {
        if (!DebugConfig.SHOW_DEBUG_HUD) return;
        if (event.getOverlay() != VanillaGuiOverlay.DEBUG_TEXT.type()) return;

        Minecraft mc = Minecraft.getInstance();
        Font font = mc.font;
        PoseStack ps = event.getPoseStack();

        int x = 4;
        int y = 4;
        int lineH = 10;
        int bgColor = 0x90000000;

        // Background panel
        int panelW = 180;
        int panelH = 10 * lineH + 6;
        GuiComponent.fill(ps, x - 2, y - 2, x + panelW, y + panelH, bgColor);

        // Header
        font.drawShadow(ps, "§6=== OTM Debug ===", x, y, 0xFFD700);
        y += lineH;

        // TPS
        double tps = DebugConfig.WATCH_SERVER_TPS;
        int tpsColor = tps >= 19 ? 0x55FF55 : (tps >= 15 ? 0xFFFF55 : 0xFF5555);
        font.drawShadow(ps, "TPS: " + String.format("%.1f", tps), x, y, tpsColor);
        y += lineH;

        // Game time
        long gt = DebugConfig.WATCH_GAME_TIME;
        long secs = (gt / 20) % 60;
        long mins = (gt / 1200) % 60;
        long hrs = gt / 72000;
        font.drawShadow(ps, "Time: " + hrs + "h " + mins + "m " + secs + "s (t" + gt + ")", x, y, 0xCCCCCC);
        y += lineH;

        // Trader info
        font.drawShadow(ps, "Trader Lvl: " + DebugConfig.WATCH_TRADER_LEVEL, x, y, 0xFFD700);
        font.drawShadow(ps, "XP: " + DebugConfig.WATCH_TRADER_XP, x + 90, y, 0x55FF55);
        y += lineH;

        // Coins with sprites
        font.drawShadow(ps, "Coins: ", x, y, 0xFFD700);
        CoinRenderer.renderCoinValue(ps, font, x + font.width("Coins: "), y, DebugConfig.WATCH_PENDING_COINS);
        y += lineH;

        // Shipments
        font.drawShadow(ps, "Shipments: " + DebugConfig.WATCH_ACTIVE_SHIPMENTS, x, y, 0xAAAAAA);
        y += lineH;

        // Sale timer
        font.drawShadow(ps, "Sale Timer: " + DebugConfig.WATCH_SALE_TIMER, x, y, 0xAAAAAA);
        y += lineH;

        // Active overrides count
        int overrides = countActiveOverrides();
        int oColor = overrides > 0 ? 0xFFFF55 : 0x666666;
        font.drawShadow(ps, "Overrides: " + overrides, x, y, oColor);
        y += lineH;

        // Cheats status
        StringBuilder cheats = new StringBuilder();
        if (DebugConfig.SKIP_PICKUP_DELAY) cheats.append("NoPick ");
        if (DebugConfig.INSTANT_DELIVERY) cheats.append("InstDlv ");
        if (DebugConfig.INSTANT_SELL) cheats.append("InstSell ");
        font.drawShadow(ps, "Cheats: " + (cheats.length() > 0 ? cheats.toString() : "none"), x, y, 0xFF5555);
        y += lineH;

        // Last event
        String lastEvent = DebugConfig.WATCH_LAST_EVENT;
        if (lastEvent.length() > 28) lastEvent = lastEvent.substring(0, 27) + "..";
        font.drawShadow(ps, "Last: " + lastEvent, x, y, 0x888888);
    }

    private static int countActiveOverrides() {
        int count = 0;
        if (DebugConfig.OVERRIDE_PICKUP_DELAY > 0) count++;
        if (DebugConfig.OVERRIDE_TICKS_PER_DISTANCE > 0) count++;
        if (DebugConfig.OVERRIDE_SALE_CHECK_INTERVAL > 0) count++;
        if (DebugConfig.OVERRIDE_BASE_SALE_CHANCE > 0) count++;
        if (DebugConfig.OVERRIDE_NEED_BONUS > 0) count++;
        if (DebugConfig.OVERRIDE_SURPLUS_PENALTY > 0) count++;
        if (DebugConfig.OVERRIDE_OVERPRICE_THRESHOLD > 0) count++;
        if (DebugConfig.OVERRIDE_XP_PER_SALE > 0) count++;
        if (DebugConfig.OVERRIDE_BASE_XP_TO_LEVEL > 0) count++;
        if (DebugConfig.OVERRIDE_MAX_TRADER_LEVEL > 0) count++;
        if (DebugConfig.OVERRIDE_BIN_SEARCH_RADIUS > 0) count++;
        return count;
    }
}
