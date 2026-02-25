package com.offtomarket.mod.client.screen;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;
import com.offtomarket.mod.block.entity.FinanceTableBlockEntity;
import com.offtomarket.mod.debug.DebugConfig;
import com.offtomarket.mod.item.CoinItem;
import com.offtomarket.mod.menu.FinanceTableMenu;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;

public class FinanceTableScreen extends AbstractContainerScreen<FinanceTableMenu> {

    public FinanceTableScreen(FinanceTableMenu menu, Inventory inv, Component title) {
        super(menu, inv, title);
        this.imageWidth = 176;
        this.imageHeight = 168;
        // Move vanilla label positions to match OtmGuiTheme layout
        this.inventoryLabelY = this.imageHeight - 94;
        this.titleLabelX = 8;
        this.titleLabelY = 5;
    }

    @Override
    protected void renderBg(PoseStack poseStack, float partialTick, int mouseX, int mouseY) {
        RenderSystem.setShader(GameRenderer::getPositionTexShader);
        RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);

        int x = this.leftPos;
        int y = this.topPos;

        // Main outer panel
        OtmGuiTheme.drawPanel(poseStack, x, y, this.imageWidth, this.imageHeight);

        // Title inset
        OtmGuiTheme.drawInsetPanel(poseStack, x + 4, y + 3, this.imageWidth - 8, 14);

        // Table slot area inset (3 rows x 9 cols)
        OtmGuiTheme.drawInsetPanel(poseStack, x + 4, y + 18, this.imageWidth - 8, 56);

        // Player inventory inset
        OtmGuiTheme.drawInsetPanel(poseStack, x + 4, y + 78, this.imageWidth - 8, 56);

        // Hotbar inset
        OtmGuiTheme.drawInsetPanel(poseStack, x + 4, y + 138, this.imageWidth - 8, 24);

        // Draw individual slot backgrounds
        for (int row = 0; row < 3; row++) {
            for (int col = 0; col < 9; col++) {
                OtmGuiTheme.drawSlot(poseStack, x + 8 + col * 18, y + 18 + row * 18);
            }
        }
        for (int row = 0; row < 3; row++) {
            for (int col = 0; col < 9; col++) {
                OtmGuiTheme.drawSlot(poseStack, x + 8 + col * 18, y + 84 + row * 18);
            }
        }
        for (int col = 0; col < 9; col++) {
            OtmGuiTheme.drawSlot(poseStack, x + 8 + col * 18, y + 142);
        }
    }

    @Override
    protected void renderLabels(PoseStack poseStack, int mouseX, int mouseY) {
        // Title
        this.font.draw(poseStack, this.title, this.titleLabelX, this.titleLabelY, 0xFFD700);

        // Inventory label
        this.font.draw(poseStack, this.playerInventoryTitle,
                this.titleLabelX, this.inventoryLabelY, 0x888888);

        // Coin value stored
        FinanceTableBlockEntity be = menu.getBlockEntity();
        if (be != null) {
            int total = be.getTotalCoinValue();
            if (DebugConfig.isGoldOnlyMode()) {
                int gp = total / 100;
                this.font.draw(poseStack, "Stored: \u00A7e" + gp + "g\u00A7r", 8, this.imageHeight - 82, 0xCCAA66);
            } else {
                int gp = total / 100;
                int sp = (total % 100) / 10;
                int cp = total % 10;
                StringBuilder sb = new StringBuilder("Stored: ");
                if (gp > 0) sb.append("\u00A7e").append(gp).append("g\u00A7r ");
                if (sp > 0) sb.append("\u00A77").append(sp).append("s\u00A7r ");
                if (cp > 0 || total == 0) sb.append("\u00A76").append(cp).append("c\u00A7r");
                this.font.draw(poseStack, sb.toString(), 8, this.imageHeight - 82, 0xCCAA66);
            }
        }
    }

    @Override
    public void render(PoseStack poseStack, int mouseX, int mouseY, float partialTick) {
        this.renderBackground(poseStack);
        super.render(poseStack, mouseX, mouseY, partialTick);
        this.renderTooltip(poseStack, mouseX, mouseY);
    }
}
