package com.offtomarket.mod.client.screen;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;
import com.offtomarket.mod.OffToMarket;
import com.offtomarket.mod.debug.DebugConfig;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiComponent;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.resources.ResourceLocation;

/**
 * Utility for rendering coin values with inline coin sprites.
 * Breaks a copper-piece total into GP/SP/CP and renders each denomination
 * as a colored number followed by a small coin icon.
 */
public class CoinRenderer extends GuiComponent {
    private static final ResourceLocation GOLD_ICON =
            new ResourceLocation(OffToMarket.MODID, "textures/item/gold_coin.png");
    private static final ResourceLocation SILVER_ICON =
            new ResourceLocation(OffToMarket.MODID, "textures/item/silver_coin.png");
    private static final ResourceLocation COPPER_ICON =
            new ResourceLocation(OffToMarket.MODID, "textures/item/copper_coin.png");

    private static final int GOLD_COLOR   = 0xFFD700;
    private static final int SILVER_COLOR = 0xC0C0C0;
    private static final int COPPER_COLOR = 0xCD7F32;

    // Normal size (for detail displays, stats bars)
    private static final int ICON_SIZE = 8;
    private static final int GAP = 2;

    // Compact size (for slot overlays, table cells)
    private static final int ICON_SMALL = 8;
    private static final int GAP_SMALL = 2;

    /**
     * Render a coin value with inline coin sprites at normal size (8x8 icons).
     * @return total pixel width drawn
     */
    public static int renderCoinValue(PoseStack ps, Font font, int x, int y, int copperPieces) {
        return renderInternal(ps, font, x, y, copperPieces, ICON_SIZE, GAP);
    }

    /**
     * Render a compact coin value with smaller sprites (6x6 icons).
     * Suitable for slot overlays and table cells.
     * @return total pixel width drawn
     */
    public static int renderCompactCoinValue(PoseStack ps, Font font, int x, int y, int copperPieces) {
        return renderInternal(ps, font, x, y, copperPieces, ICON_SMALL, GAP_SMALL);
    }

    /**
     * Calculate the pixel width a coin value would take at normal size.
     */
    public static int getCoinValueWidth(Font font, int copperPieces) {
        return calcWidth(font, copperPieces, ICON_SIZE, GAP);
    }

    /**
     * Calculate the pixel width a compact coin value would take.
     */
    public static int getCompactCoinValueWidth(Font font, int copperPieces) {
        return calcWidth(font, copperPieces, ICON_SMALL, GAP_SMALL);
    }

    /**
     * Render only the highest non-zero denomination with a coin sprite.
     * Ideal for slot overlays where space is very limited.
     * Shows e.g. "4" + gold icon for 453 CP.
     * @return total pixel width drawn
     */
    public static int renderPrimaryValue(PoseStack ps, Font font, int x, int y, int copperPieces) {
        int gp = copperPieces / 100;
        int sp = (copperPieces % 100) / 10;
        int cp = copperPieces % 10;
        int startX = x;

        if (gp > 0) {
            x = drawDenomination(ps, font, x, y, gp, GOLD_ICON, GOLD_COLOR, ICON_SMALL);
        } else if (sp > 0) {
            x = drawDenomination(ps, font, x, y, sp, SILVER_ICON, SILVER_COLOR, ICON_SMALL);
        } else {
            x = drawDenomination(ps, font, x, y, cp, COPPER_ICON, COPPER_COLOR, ICON_SMALL);
        }

        return x - startX;
    }

    /**
     * Calculate pixel width of a primary denomination rendering.
     */
    public static int getPrimaryValueWidth(Font font, int copperPieces) {
        int gp = copperPieces / 100;
        int sp = (copperPieces % 100) / 10;
        int cp = copperPieces % 10;

        int amount;
        if (gp > 0) amount = gp;
        else if (sp > 0) amount = sp;
        else amount = cp;

        return font.width(String.valueOf(amount)) + 1 + ICON_SMALL;
    }

    // --- Internal ---

    private static int renderInternal(PoseStack ps, Font font, int x, int y, int copperPieces,
                                       int iconSize, int gap) {
        // Gold Only mode: show everything as gold pieces (rounded up)
        if (DebugConfig.isGoldOnlyMode()) {
            int goldAmount = Math.max(1, (copperPieces + 99) / 100);
            return drawDenomination(ps, font, x, y, goldAmount, GOLD_ICON, GOLD_COLOR, iconSize) - x;
        }

        int gp = copperPieces / 100;
        int sp = (copperPieces % 100) / 10;
        int cp = copperPieces % 10;
        int startX = x;
        boolean any = false;

        if (gp > 0) {
            x = drawDenomination(ps, font, x, y, gp, GOLD_ICON, GOLD_COLOR, iconSize);
            any = true;
        }
        if (sp > 0) {
            if (any) x += gap;
            x = drawDenomination(ps, font, x, y, sp, SILVER_ICON, SILVER_COLOR, iconSize);
            any = true;
        }
        if (cp > 0 || !any) {
            if (any) x += gap;
            x = drawDenomination(ps, font, x, y, cp, COPPER_ICON, COPPER_COLOR, iconSize);
        }

        return x - startX;
    }

    private static int calcWidth(Font font, int copperPieces, int iconSize, int gap) {
        // Gold Only mode
        if (DebugConfig.isGoldOnlyMode()) {
            int goldAmount = Math.max(1, (copperPieces + 99) / 100);
            return font.width(String.valueOf(goldAmount)) + 1 + iconSize;
        }

        int gp = copperPieces / 100;
        int sp = (copperPieces % 100) / 10;
        int cp = copperPieces % 10;
        int width = 0;
        int parts = 0;

        if (gp > 0) { width += font.width(String.valueOf(gp)) + 1 + iconSize; parts++; }
        if (sp > 0) { width += font.width(String.valueOf(sp)) + 1 + iconSize; parts++; }
        if (cp > 0 || parts == 0) { width += font.width(String.valueOf(cp)) + 1 + iconSize; parts++; }

        if (parts > 1) width += (parts - 1) * gap;
        return width;
    }

    private static int drawDenomination(PoseStack ps, Font font, int x, int y, int amount,
                                         ResourceLocation icon, int color, int iconSize) {
        String num = String.valueOf(amount);
        font.draw(ps, num, (float) x, (float) y, color);
        x += font.width(num) + 1;

        RenderSystem.setShader(GameRenderer::getPositionTexShader);
        RenderSystem.setShaderColor(1.0f, 1.0f, 1.0f, 1.0f);
        RenderSystem.setShaderTexture(0, icon);
        RenderSystem.enableBlend();
        blit(ps, x, y, iconSize, iconSize, 0.0f, 0.0f, 16, 16, 16, 16);
        RenderSystem.disableBlend();

        return x + iconSize;
    }
}
