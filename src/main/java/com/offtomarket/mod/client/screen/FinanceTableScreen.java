package com.offtomarket.mod.client.screen;

import com.mojang.blaze3d.vertex.PoseStack;
import com.offtomarket.mod.block.entity.FinanceTableBlockEntity;
import com.offtomarket.mod.debug.DebugConfig;
import com.offtomarket.mod.menu.FinanceTableMenu;
import com.offtomarket.mod.network.DepositCoinsPacket;
import com.offtomarket.mod.network.ModNetwork;
import com.offtomarket.mod.network.WithdrawCoinsPacket;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;

/**
 * Screen for the Finance Table. Shows the deposited coin balance and provides
 * Deposit All and Withdraw All buttons. No block-entity item slots — all
 * operations are handled server-side via network packets.
 */
public class FinanceTableScreen extends AbstractContainerScreen<FinanceTableMenu> {

    public FinanceTableScreen(FinanceTableMenu menu, Inventory inv, Component title) {
        super(menu, inv, title);
        this.imageWidth  = 176;
        this.imageHeight = 145;
        this.titleLabelX = 8;
        this.titleLabelY = 6;
        // Push inventory label out of the auto-draw path — we draw it manually
        this.inventoryLabelY = this.imageHeight + 100;
    }

    @Override
    protected void init() {
        super.init();

        // Deposit All button
        this.addRenderableWidget(new Button(
                this.leftPos + 8, this.topPos + 44, 78, 20,
                Component.literal("Deposit All"),
                btn -> {
                    FinanceTableBlockEntity be = menu.getBlockEntity();
                    if (be != null) {
                        ModNetwork.CHANNEL.sendToServer(new DepositCoinsPacket(be.getBlockPos()));
                    }
                }));

        // Withdraw All button
        this.addRenderableWidget(new Button(
                this.leftPos + 90, this.topPos + 44, 78, 20,
                Component.literal("Withdraw All"),
                btn -> {
                    FinanceTableBlockEntity be = menu.getBlockEntity();
                    if (be != null) {
                        ModNetwork.CHANNEL.sendToServer(new WithdrawCoinsPacket(be.getBlockPos()));
                    }
                }));
    }

    @Override
    protected void renderBg(PoseStack poseStack, float partialTick, int mouseX, int mouseY) {
        int x = this.leftPos;
        int y = this.topPos;

        // Main outer panel
        OtmGuiTheme.drawPanel(poseStack, x, y, this.imageWidth, this.imageHeight);

        // Title inset
        OtmGuiTheme.drawInsetPanel(poseStack, x + 4, y + 3, this.imageWidth - 8, 14);

        // Balance display inset
        OtmGuiTheme.drawInsetPanel(poseStack, x + 4, y + 20, this.imageWidth - 8, 20);

        // Player inventory inset (3 rows)
        OtmGuiTheme.drawInsetPanel(poseStack, x + 4, y + 60, this.imageWidth - 8, 58);

        // Hotbar inset
        OtmGuiTheme.drawInsetPanel(poseStack, x + 4, y + 118, this.imageWidth - 8, 24);

        // Individual slot backgrounds — player inventory
        for (int row = 0; row < 3; row++) {
            for (int col = 0; col < 9; col++) {
                OtmGuiTheme.drawSlot(poseStack, x + 8 + col * 18, y + FinanceTableMenu.PLAYER_INV_Y + row * 18);
            }
        }
        // Hotbar slot backgrounds
        for (int col = 0; col < 9; col++) {
            OtmGuiTheme.drawSlot(poseStack, x + 8 + col * 18, y + FinanceTableMenu.HOTBAR_Y);
        }
    }

    @Override
    protected void renderLabels(PoseStack poseStack, int mouseX, int mouseY) {
        // Title
        this.font.draw(poseStack, this.title, this.titleLabelX, this.titleLabelY, OtmGuiTheme.TEXT_TITLE);

        // Inventory label above player inventory
        this.font.draw(poseStack, this.playerInventoryTitle,
                this.titleLabelX, FinanceTableMenu.PLAYER_INV_Y - 10, OtmGuiTheme.TEXT_MUTED);

        // Balance display inside the balance inset panel
        FinanceTableBlockEntity be = menu.getBlockEntity();
        int total = (be != null) ? be.getBalance() : 0;

        String balanceText;
        if (DebugConfig.isGoldOnlyMode()) {
            int gp = total / 100;
            balanceText = "Balance: \u00A7e" + gp + "g\u00A7r";
        } else {
            int gp = total / 100;
            int sp = (total % 100) / 10;
            int cp = total % 10;
            StringBuilder sb = new StringBuilder("Balance: ");
            if (gp > 0) sb.append("\u00A7e").append(gp).append("g\u00A7r ");
            if (sp > 0) sb.append("\u00A77").append(sp).append("s\u00A7r ");
            if (cp > 0 || total == 0) sb.append("\u00A76").append(cp).append("c\u00A7r");
            balanceText = sb.toString().trim();
        }
        this.font.draw(poseStack, balanceText, 8, 26, OtmGuiTheme.TEXT_TITLE);
    }

    @Override
    public void render(PoseStack poseStack, int mouseX, int mouseY, float partialTick) {
        this.renderBackground(poseStack);
        super.render(poseStack, mouseX, mouseY, partialTick);
        this.renderTooltip(poseStack, mouseX, mouseY);
    }
}
