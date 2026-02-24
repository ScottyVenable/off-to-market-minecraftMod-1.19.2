package com.offtomarket.mod.client.screen;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;
import com.offtomarket.mod.menu.CoinBagMenu;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;

/**
 * Screen for the Coin Bag UI. Shows 3 coin slots (gold, silver, copper)
 * with labels and a total value display.
 */
public class CoinBagScreen extends AbstractContainerScreen<CoinBagMenu> {
    private static final ResourceLocation TEXTURE =
            new ResourceLocation("minecraft", "textures/gui/container/generic_54.png");

    public CoinBagScreen(CoinBagMenu menu, Inventory inv, Component title) {
        super(menu, inv, title);
        this.imageWidth = 176;
        this.imageHeight = 166;
    }

    @Override
    protected void renderBg(PoseStack poseStack, float partialTick, int mouseX, int mouseY) {
        RenderSystem.setShader(GameRenderer::getPositionTexShader);
        RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);

        int x = this.leftPos;
        int y = this.topPos;

        // Draw a custom background using fills (themed like the trading UI)
        // Main background
        fill(poseStack, x, y, x + imageWidth, y + imageHeight, 0xFF2A2018);

        // Border
        fill(poseStack, x + 1, y + 1, x + imageWidth - 1, y + imageHeight - 1, 0xFF3D3226);

        // Title bar
        fill(poseStack, x + 4, y + 4, x + imageWidth - 4, y + 18, 0xFF4A3D2B);

        // Coin slots area
        fill(poseStack, x + 4, y + 22, x + imageWidth - 4, y + 70, 0xFF4A3D2B);

        // Draw slot backgrounds (18x18 each at slots positions)
        int[] slotX = {44, 80, 116};
        for (int sx : slotX) {
            fill(poseStack, x + sx - 1, y + 34, x + sx + 17, y + 52, 0xFF1A1410);
            fill(poseStack, x + sx, y + 35, x + sx + 16, y + 51, 0xFF2A2018);
        }

        // Total value bar
        fill(poseStack, x + 4, y + 58, x + imageWidth - 4, y + 70, 0xFF5A4A30);

        // Player inventory background
        fill(poseStack, x + 4, y + 74, x + imageWidth - 4, y + imageHeight - 4, 0xFF4A3D2B);

        // Player inventory slot backgrounds
        for (int row = 0; row < 3; row++) {
            for (int col = 0; col < 9; col++) {
                int sx = x + 8 + col * 18;
                int sy = y + 84 + row * 18;
                fill(poseStack, sx - 1, sy - 1, sx + 17, sy + 17, 0xFF1A1410);
                fill(poseStack, sx, sy, sx + 16, sy + 16, 0xFF2A2018);
            }
        }
        // Hotbar slot backgrounds
        for (int col = 0; col < 9; col++) {
            int sx = x + 8 + col * 18;
            int sy = y + 142;
            fill(poseStack, sx - 1, sy - 1, sx + 17, sy + 17, 0xFF1A1410);
            fill(poseStack, sx, sy, sx + 16, sy + 16, 0xFF2A2018);
        }
    }

    @Override
    protected void renderLabels(PoseStack poseStack, int mouseX, int mouseY) {
        // Title
        drawCenteredString(poseStack, this.font, "Coin Bag", this.imageWidth / 2, 6, 0xFFD700);

        // Slot labels
        drawCenteredString(poseStack, this.font, "Gold", 52, 24, 0xFFD700);
        drawCenteredString(poseStack, this.font, "Silver", 88, 24, 0xC0C0C0);
        drawCenteredString(poseStack, this.font, "Copper", 124, 24, 0xCD7F32);

        // Total value
        int total = menu.getTotalValue();
        String totalText = "Total: " + total + " CP";
        if (total >= 100) {
            int gp = total / 100;
            int sp = (total % 100) / 10;
            int cp = total % 10;
            StringBuilder sb = new StringBuilder("Total: ");
            if (gp > 0) sb.append(gp).append("g ");
            if (sp > 0) sb.append(sp).append("s ");
            if (cp > 0) sb.append(cp).append("c");
            totalText = sb.toString().trim() + " (" + total + " CP)";
        }
        drawCenteredString(poseStack, this.font, totalText, this.imageWidth / 2, 61, 0xFFD700);

        // Inventory label
        this.font.draw(poseStack, this.playerInventoryTitle, 8, 74, 0x888888);
    }

    @Override
    public void render(PoseStack poseStack, int mouseX, int mouseY, float partialTick) {
        renderBackground(poseStack);
        super.render(poseStack, mouseX, mouseY, partialTick);
        renderTooltip(poseStack, mouseX, mouseY);
    }
}
