package com.offtomarket.mod.client.screen;

import com.mojang.blaze3d.vertex.PoseStack;
import com.offtomarket.mod.block.entity.FinanceTableBlockEntity;
import com.offtomarket.mod.debug.DebugConfig;
import com.offtomarket.mod.menu.FinanceTableMenu;
import com.offtomarket.mod.network.DepositAmountPacket;
import com.offtomarket.mod.network.DepositCoinsPacket;
import com.offtomarket.mod.network.ModNetwork;
import com.offtomarket.mod.network.WithdrawAmountPacket;
import com.offtomarket.mod.network.WithdrawCoinsPacket;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;

/**
 * Screen for the Finance Table. Shows the deposited coin balance and provides
 * gold/silver/copper input fields for precise withdraw/deposit operations,
 * plus Deposit All and Withdraw All convenience buttons.
 */
public class FinanceTableScreen extends AbstractContainerScreen<FinanceTableMenu> {

    private EditBox goldInput;
    private EditBox silverInput;
    private EditBox copperInput;

    public FinanceTableScreen(FinanceTableMenu menu, Inventory inv, Component title) {
        super(menu, inv, title);
        this.imageWidth  = 176;
        this.imageHeight = 178;
        this.titleLabelX = 8;
        this.titleLabelY = 6;
        // Push inventory label out of the auto-draw path -- we draw it manually
        this.inventoryLabelY = this.imageHeight + 100;
    }

    @Override
    protected void init() {
        super.init();
        int x = this.leftPos;
        int y = this.topPos;

        // ---- Coin amount input fields (y+38 row) ----

        goldInput = new EditBox(this.font, x + 18, y + 39, 32, 12,
                Component.literal("Gold"));
        goldInput.setMaxLength(4);
        goldInput.setFilter(s -> s.isEmpty() || s.matches("\\d+"));
        goldInput.setValue("0");
        this.addWidget(goldInput);

        silverInput = new EditBox(this.font, x + 72, y + 39, 32, 12,
                Component.literal("Silver"));
        silverInput.setMaxLength(4);
        silverInput.setFilter(s -> s.isEmpty() || s.matches("\\d+"));
        silverInput.setValue("0");
        this.addWidget(silverInput);

        copperInput = new EditBox(this.font, x + 126, y + 39, 32, 12,
                Component.literal("Copper"));
        copperInput.setMaxLength(4);
        copperInput.setFilter(s -> s.isEmpty() || s.matches("\\d+"));
        copperInput.setValue("0");
        this.addWidget(copperInput);

        // ---- Withdraw / Deposit amount buttons (y+55) ----

        this.addRenderableWidget(new Button(
                x + 8, y + 55, 78, 16,
                Component.literal("Withdraw"),
                btn -> {
                    FinanceTableBlockEntity be = menu.getBlockEntity();
                    if (be != null) {
                        int g = parseInput(goldInput);
                        int s = parseInput(silverInput);
                        int c = parseInput(copperInput);
                        if (g > 0 || s > 0 || c > 0) {
                            ModNetwork.CHANNEL.sendToServer(
                                    new WithdrawAmountPacket(be.getBlockPos(), g, s, c));
                        }
                    }
                }));

        this.addRenderableWidget(new Button(
                x + 90, y + 55, 78, 16,
                Component.literal("Deposit"),
                btn -> {
                    FinanceTableBlockEntity be = menu.getBlockEntity();
                    if (be != null) {
                        int g = parseInput(goldInput);
                        int s = parseInput(silverInput);
                        int c = parseInput(copperInput);
                        if (g > 0 || s > 0 || c > 0) {
                            ModNetwork.CHANNEL.sendToServer(
                                    new DepositAmountPacket(be.getBlockPos(), g, s, c));
                        }
                    }
                }));

        // ---- Withdraw All / Deposit All convenience buttons (y+73) ----

        this.addRenderableWidget(new Button(
                x + 8, y + 73, 78, 16,
                Component.literal("Withdraw All"),
                btn -> {
                    FinanceTableBlockEntity be = menu.getBlockEntity();
                    if (be != null) {
                        ModNetwork.CHANNEL.sendToServer(new WithdrawCoinsPacket(be.getBlockPos()));
                    }
                }));

        this.addRenderableWidget(new Button(
                x + 90, y + 73, 78, 16,
                Component.literal("Deposit All"),
                btn -> {
                    FinanceTableBlockEntity be = menu.getBlockEntity();
                    if (be != null) {
                        ModNetwork.CHANNEL.sendToServer(new DepositCoinsPacket(be.getBlockPos()));
                    }
                }));
    }

    /** Parses an EditBox value to an int, returning 0 for empty/invalid. */
    private static int parseInput(EditBox box) {
        String text = box.getValue().trim();
        if (text.isEmpty()) return 0;
        try {
            return Math.max(0, Integer.parseInt(text));
        } catch (NumberFormatException e) {
            return 0;
        }
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
        OtmGuiTheme.drawInsetPanel(poseStack, x + 4, y + 19, this.imageWidth - 8, 16);

        // Coin input row inset
        OtmGuiTheme.drawInsetPanel(poseStack, x + 4, y + 37, this.imageWidth - 8, 16);

        // Player inventory inset (3 rows)
        OtmGuiTheme.drawInsetPanel(poseStack, x + 4, y + 92, this.imageWidth - 8, 58);

        // Hotbar inset
        OtmGuiTheme.drawInsetPanel(poseStack, x + 4, y + 150, this.imageWidth - 8, 24);

        // Individual slot backgrounds -- player inventory
        for (int row = 0; row < 3; row++) {
            for (int col = 0; col < 9; col++) {
                OtmGuiTheme.drawSlot(poseStack, x + 8 + col * 18, y + FinanceTableMenu.PLAYER_INV_Y + row * 18);
            }
        }
        // Hotbar slot backgrounds
        for (int col = 0; col < 9; col++) {
            OtmGuiTheme.drawSlot(poseStack, x + 8 + col * 18, y + FinanceTableMenu.HOTBAR_Y);
        }

        // Coin denomination labels next to input fields
        this.font.draw(poseStack, "\u00A7eG:", x + 8, y + 41, OtmGuiTheme.TEXT_VALUE);
        this.font.draw(poseStack, "\u00A77S:", x + 62, y + 41, OtmGuiTheme.TEXT_VALUE);
        this.font.draw(poseStack, "\u00A76C:", x + 116, y + 41, OtmGuiTheme.TEXT_VALUE);

        // Render the EditBox widgets
        goldInput.render(poseStack, mouseX, mouseY, partialTick);
        silverInput.render(poseStack, mouseX, mouseY, partialTick);
        copperInput.render(poseStack, mouseX, mouseY, partialTick);
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
        this.font.draw(poseStack, balanceText, 8, 23, OtmGuiTheme.TEXT_TITLE);
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        // Prevent inventory close while typing in any EditBox
        if (goldInput.isFocused() || silverInput.isFocused() || copperInput.isFocused()) {
            if (keyCode == 256) { // Escape: unfocus all inputs
                goldInput.setFocus(false);
                silverInput.setFocus(false);
                copperInput.setFocus(false);
                return true;
            }
            // Tab to cycle between inputs
            if (keyCode == 258) {
                if (goldInput.isFocused()) {
                    goldInput.setFocus(false);
                    silverInput.setFocus(true);
                } else if (silverInput.isFocused()) {
                    silverInput.setFocus(false);
                    copperInput.setFocus(true);
                } else {
                    copperInput.setFocus(false);
                    goldInput.setFocus(true);
                }
                return true;
            }
            // Delegate to focused widget and consume the event
            if (goldInput.isFocused())   return goldInput.keyPressed(keyCode, scanCode, modifiers) || true;
            if (silverInput.isFocused()) return silverInput.keyPressed(keyCode, scanCode, modifiers) || true;
            if (copperInput.isFocused()) return copperInput.keyPressed(keyCode, scanCode, modifiers) || true;
        }
        return super.keyPressed(keyCode, scanCode, modifiers);
    }

    @Override
    public boolean charTyped(char codePoint, int modifiers) {
        if (goldInput.isFocused())   return goldInput.charTyped(codePoint, modifiers);
        if (silverInput.isFocused()) return silverInput.charTyped(codePoint, modifiers);
        if (copperInput.isFocused()) return copperInput.charTyped(codePoint, modifiers);
        return super.charTyped(codePoint, modifiers);
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        // Let EditBoxes handle their own clicks
        if (goldInput.mouseClicked(mouseX, mouseY, button)) {
            goldInput.setFocus(true);
            silverInput.setFocus(false);
            copperInput.setFocus(false);
            return true;
        }
        if (silverInput.mouseClicked(mouseX, mouseY, button)) {
            goldInput.setFocus(false);
            silverInput.setFocus(true);
            copperInput.setFocus(false);
            return true;
        }
        if (copperInput.mouseClicked(mouseX, mouseY, button)) {
            goldInput.setFocus(false);
            silverInput.setFocus(false);
            copperInput.setFocus(true);
            return true;
        }
        // Unfocus all when clicking elsewhere
        goldInput.setFocus(false);
        silverInput.setFocus(false);
        copperInput.setFocus(false);
        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public void render(PoseStack poseStack, int mouseX, int mouseY, float partialTick) {
        this.renderBackground(poseStack);
        super.render(poseStack, mouseX, mouseY, partialTick);
        this.renderTooltip(poseStack, mouseX, mouseY);
    }
}
