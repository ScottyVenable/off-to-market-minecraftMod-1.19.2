package com.offtomarket.mod.client.screen;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;
import com.offtomarket.mod.OffToMarket;
import com.offtomarket.mod.block.entity.TradingBinBlockEntity;
import com.offtomarket.mod.data.PriceCalculator;
import com.offtomarket.mod.menu.TradingBinMenu;
import com.offtomarket.mod.network.ModNetwork;
import com.offtomarket.mod.network.SetPricePacket;
import com.offtomarket.mod.network.UpdateBinSettingsPacket;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.network.PacketDistributor;

public class TradingBinScreen extends AbstractContainerScreen<TradingBinMenu> {
    private static final ResourceLocation TEXTURE =
            new ResourceLocation(OffToMarket.MODID, "textures/gui/trading_bin.png");

    private EditBox priceInput;
    private Button setButton;

    /** Track what was in the inspection slot last frame to auto-populate price input. */
    private ItemStack lastInspectionItem = ItemStack.EMPTY;

    /** Whether the settings panel is showing instead of the price book. */
    private boolean showingSettings = false;

    /** 
     * Currently selected slot for pricing (0-8 = bin slots, 9 = inspect slot).
     * -1 means no selection (defaults to inspect slot behavior).
     */
    private int selectedSlot = -1;

    /** Settings panel widgets. */
    private Button gearButton;
    private EditBox taxInput;
    private EditBox markupInput;
    private Button autoPriceModeButton;
    private Button applySettingsBtn;

    public TradingBinScreen(TradingBinMenu menu, Inventory inv, Component title) {
        super(menu, inv, title);
        this.imageWidth = 256;
        this.imageHeight = 166;
    }

    @Override
    protected void init() {
        super.init();
        int x = this.leftPos;
        int y = this.topPos;

        // Gear button (top-right of book panel) to toggle settings
        gearButton = addRenderableWidget(new Button(x + 241, y + 7, 14, 14,
                Component.literal("\u2699"), btn -> {
            showingSettings = !showingSettings;
            updateSettingsVisibility();
        }));

        // Price input field (inside the book panel)
        this.priceInput = new EditBox(this.font, x + 188, y + 76, 56, 12,
                Component.literal("Price"));
        this.priceInput.setMaxLength(6);
        this.priceInput.setFilter(s -> s.isEmpty() || s.matches("\\d+"));
        this.addWidget(this.priceInput);

        // Set Price button (inside the book panel)
        setButton = addRenderableWidget(new Button(x + 188, y + 90, 56, 14,
                Component.literal("Set"), btn -> {
            if (!priceInput.getValue().isEmpty()) {
                TradingBinBlockEntity be = menu.getBlockEntity();
                if (be != null) {
                    int price = Integer.parseInt(priceInput.getValue());
                    // Use selected slot, or INSPECT_SLOT if nothing selected
                    int targetSlot = selectedSlot >= 0 ? selectedSlot : TradingBinBlockEntity.INSPECT_SLOT;
                    ModNetwork.CHANNEL.send(PacketDistributor.SERVER.noArg(),
                            new SetPricePacket(be.getBlockPos(), targetSlot, price));
                }
            }
        }));

        // ---- Settings panel widgets ----
        TradingBinBlockEntity be = menu.getBlockEntity();

        // Crafting Tax % input
        taxInput = new EditBox(this.font, x + 218, y + 40, 30, 12,
                Component.literal("Tax"));
        taxInput.setMaxLength(3);
        taxInput.setFilter(s -> s.isEmpty() || s.matches("\\d+"));
        taxInput.setValue(be != null ? String.valueOf(be.getCraftingTaxPercent()) : "15");
        this.addWidget(taxInput);

        // Min Markup % input
        markupInput = new EditBox(this.font, x + 218, y + 62, 30, 12,
                Component.literal("Markup"));
        markupInput.setMaxLength(3);
        markupInput.setFilter(s -> s.isEmpty() || s.matches("\\d+"));
        markupInput.setValue(be != null ? String.valueOf(be.getMinMarkupPercent()) : "0");
        this.addWidget(markupInput);

        // Auto-Price Mode cycling button
        String modeLabel = be != null ? be.getAutoPriceMode().getDisplayName() : "Auto Fair";
        autoPriceModeButton = addRenderableWidget(new Button(x + 188, y + 82, 64, 14,
                Component.literal(modeLabel), btn -> {
            TradingBinBlockEntity blockEntity = menu.getBlockEntity();
            if (blockEntity != null) {
                blockEntity.cycleAutoPriceMode();
                btn.setMessage(Component.literal(blockEntity.getAutoPriceMode().getDisplayName()));
            }
        }));

        // Apply Settings button
        applySettingsBtn = addRenderableWidget(new Button(x + 188, y + 100, 64, 14,
                Component.literal("Apply"), btn -> {
            sendSettingsToServer();
        }));

        updateSettingsVisibility();
    }

    /**
     * Get the currently selected item for pricing.
     * Returns the item in the selected slot, or the inspection slot if nothing selected.
     */
    private ItemStack getSelectedItem() {
        TradingBinBlockEntity be = menu.getBlockEntity();
        if (be == null) return ItemStack.EMPTY;
        
        if (selectedSlot >= 0 && selectedSlot < TradingBinBlockEntity.BIN_SIZE) {
            return be.getItem(selectedSlot);
        } else if (selectedSlot == TradingBinBlockEntity.INSPECT_SLOT) {
            return be.getItem(TradingBinBlockEntity.INSPECT_SLOT);
        }
        // Default to inspection slot
        return be.getItem(TradingBinBlockEntity.INSPECT_SLOT);
    }

    /**
     * Toggle visibility of book vs settings panel widgets.
     * Also considers whether an item is selected for price book widgets.
     */
    private void updateSettingsVisibility() {
        ItemStack selectedItem = getSelectedItem();
        boolean hasSelectedItem = !selectedItem.isEmpty();

        // Price book widgets: visible only when NOT in settings AND an item is selected
        boolean priceWidgetsVisible = !showingSettings && hasSelectedItem;
        priceInput.visible = priceWidgetsVisible;
        setButton.visible = priceWidgetsVisible;

        // Settings widgets
        taxInput.visible = showingSettings;
        markupInput.visible = showingSettings;
        autoPriceModeButton.visible = showingSettings;
        applySettingsBtn.visible = showingSettings;
    }

    /**
     * Send current settings to the server.
     */
    private void sendSettingsToServer() {
        TradingBinBlockEntity be = menu.getBlockEntity();
        if (be == null) return;

        int tax = 15;
        int markup = 0;
        try { tax = Integer.parseInt(taxInput.getValue()); } catch (NumberFormatException ignored) {}
        try { markup = Integer.parseInt(markupInput.getValue()); } catch (NumberFormatException ignored) {}

        ModNetwork.CHANNEL.send(PacketDistributor.SERVER.noArg(),
                new UpdateBinSettingsPacket(be.getBlockPos(), tax, markup,
                        be.getAutoPriceMode().ordinal()));
    }

    // ==================== Drawing Helpers ====================

    private void drawSlot(PoseStack ps, int x, int y) {
        fill(ps, x - 1, y - 1, x + 17, y + 17, 0xFF373737);
        fill(ps, x, y, x + 16, y + 16, 0xFF8B8B8B);
    }

    private void drawBookPanel(PoseStack ps, int bx, int by) {
        int bw = 76, bh = 116;
        // Outer leather cover
        fill(ps, bx, by, bx + bw, by + bh, 0xFF3B2A18);
        // Spine (left edge)
        fill(ps, bx, by, bx + 3, by + bh, 0xFF2A1C10);
        // Inner leather border
        fill(ps, bx + 4, by + 3, bx + bw - 3, by + bh - 3, 0xFF6B4A35);
        // Stitch line
        fill(ps, bx + 6, by + 5, bx + bw - 5, by + bh - 5, 0xFF8B7355);
        // Parchment interior
        fill(ps, bx + 7, by + 6, bx + bw - 6, by + bh - 6, 0xFFE8D5B5);

        // Stitch dots along the border
        for (int i = by + 8; i < by + bh - 8; i += 6) {
            fill(ps, bx + 5, i, bx + 6, i + 2, 0xFFAA8866);
            fill(ps, bx + bw - 5, i, bx + bw - 4, i + 2, 0xFFAA8866);
        }
        for (int i = bx + 10; i < bx + bw - 10; i += 6) {
            fill(ps, i, by + 4, i + 2, by + 5, 0xFFAA8866);
            fill(ps, i, by + bh - 5, i + 2, by + bh - 4, 0xFFAA8866);
        }
    }

    @Override
    protected void renderBg(PoseStack poseStack, float partialTick, int mouseX, int mouseY) {
        RenderSystem.setShader(GameRenderer::getPositionTexShader);
        RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
        RenderSystem.setShaderTexture(0, TEXTURE);

        int x = this.leftPos;
        int y = this.topPos;

        // Blit the main dispenser-based texture (176x166)
        this.blit(poseStack, x, y, 0, 0, 176, 166);

        // Draw the book panel to the right
        drawBookPanel(poseStack, x + 180, y + 6);

        // Update widget visibility each frame (inspection item may change)
        updateSettingsVisibility();

        if (showingSettings) {
            // ---- Settings panel rendering ----
            taxInput.render(poseStack, mouseX, mouseY, partialTick);
            markupInput.render(poseStack, mouseX, mouseY, partialTick);
        } else {
            // ---- Price book rendering ----
            // Draw slot background in the book for the inspection slot
            drawSlot(poseStack, x + 209, y + 24);

            // Render price input field (only when an item is being inspected)
            if (priceInput.visible) {
                this.priceInput.render(poseStack, mouseX, mouseY, partialTick);
            }
        }
    }

    @Override
    protected void renderLabels(PoseStack poseStack, int mouseX, int mouseY) {
        // Title
        this.font.draw(poseStack, this.title, 8, 6, 0x404040);

        if (showingSettings) {
            // ---- Settings panel labels ----
            drawCenteredString(poseStack, this.font, "Settings", 217, 10, 0xFF5C3D28);

            this.font.draw(poseStack, "Tax %:", 190, 42, 0xFF3B2A18);
            this.font.draw(poseStack, "Markup %:", 190, 64, 0xFF3B2A18);
            this.font.draw(poseStack, "Auto Price:", 190, 76, 0xFF6B4A35);
        } else {
            // ---- Price book labels ----
            // Book header
            drawCenteredString(poseStack, this.font, "Price Book", 217, 10, 0xFF5C3D28);

        TradingBinBlockEntity be = menu.getBlockEntity();
        if (be != null) {
            ItemStack selectedStack = getSelectedItem();

            // Auto-populate price input when selected item changes
            if (!ItemStack.matches(selectedStack, lastInspectionItem)) {
                lastInspectionItem = selectedStack.copy();
                if (!selectedStack.isEmpty()) {
                    // If bin slot is selected, get its current price; otherwise use remembered
                    int price;
                    if (selectedSlot >= 0 && selectedSlot < TradingBinBlockEntity.BIN_SIZE) {
                        price = be.getSetPrice(selectedSlot);
                        if (price <= 0) price = be.getRememberedPrice(selectedStack);
                    } else {
                        price = be.getRememberedPrice(selectedStack);
                    }
                    int displayPrice = price > 0 ? price : PriceCalculator.getBaseValue(selectedStack);
                    priceInput.setValue(String.valueOf(displayPrice));
                } else {
                    priceInput.setValue("");
                }
            }

            if (!selectedStack.isEmpty()) {
                // Item name (single line, truncated to fit book width)
                String itemName = selectedStack.getHoverName().getString();
                if (this.font.width(itemName) > 58) {
                    while (this.font.width(itemName + "..") > 58 && itemName.length() > 3) {
                        itemName = itemName.substring(0, itemName.length() - 1);
                    }
                    itemName += "..";
                }
                this.font.draw(poseStack, itemName, 190, 42, 0xFF3B2A18);

                int price = 0;
                if (!priceInput.getValue().isEmpty()) {
                    try { price = Integer.parseInt(priceInput.getValue()); } catch (NumberFormatException ignored) {}
                }

                int taxPercent = be.getCraftingTaxPercent();
                PriceCalculator.PriceBreakdown bd = PriceCalculator.getBreakdown(
                        selectedStack, null, taxPercent);

                // Base material value (text format)
                this.font.draw(poseStack, "Base: " + formatCoinText(bd.materialCost()), 190, 54, 0xFF6B4A35);

                // Crafting tax (if any)
                if (bd.hasTax()) {
                    this.font.draw(poseStack, "+Tax: " + formatCoinText(bd.craftingTax()), 190, 64, 0xFF6B4A35);
                    this.font.draw(poseStack, "Max: " + formatCoinText(bd.maxPrice()), 190, 74, 0xFF888888);
                } else {
                    // No tax — show max on the second line
                    this.font.draw(poseStack, "Max: " + formatCoinText(bd.maxPrice()), 190, 64, 0xFF888888);
                }

                // Rating (if price is set)
                if (price > 0) {
                    PriceCalculator.PriceRating rating = PriceCalculator.getPriceRating(
                            price, bd.materialCost(), bd.maxPrice());
                    this.font.draw(poseStack, rating.getLabel(), 190, 106, rating.getColor());
                }
            } else {
                // No item selected: hint text
                this.font.draw(poseStack, "Click an", 190, 44, 0xFF8B7355);
                this.font.draw(poseStack, "item in", 190, 54, 0xFF8B7355);
                this.font.draw(poseStack, "the bin to", 190, 64, 0xFF8B7355);
                this.font.draw(poseStack, "set price.", 190, 74, 0xFF8B7355);
            }
        }
        } // close else (price book panel)

        // Inventory label
        this.font.draw(poseStack, this.playerInventoryTitle, 8, 73, 0x404040);
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        // Check if clicked on a bin slot (left click)
        if (button == 0 && !showingSettings) {
            TradingBinBlockEntity be = menu.getBlockEntity();
            if (be != null) {
                int x = this.leftPos;
                int y = this.topPos;
                
                // Check bin slots (3x3 grid starting at 62,17)
                for (int i = 0; i < TradingBinBlockEntity.BIN_SIZE; i++) {
                    int slotX = x + 62 + (i % 3) * 18;
                    int slotY = y + 17 + (i / 3) * 18;
                    
                    if (mouseX >= slotX && mouseX < slotX + 16 &&
                        mouseY >= slotY && mouseY < slotY + 16) {
                        ItemStack stack = be.getItem(i);
                        if (!stack.isEmpty()) {
                            selectedSlot = i;
                            lastInspectionItem = ItemStack.EMPTY; // Force price input update
                            updateSettingsVisibility();
                            return true;
                        }
                    }
                }
                
                // Check inspection slot (at 209,24)
                int inspectX = x + 209;
                int inspectY = y + 24;
                if (mouseX >= inspectX && mouseX < inspectX + 16 &&
                    mouseY >= inspectY && mouseY < inspectY + 16) {
                    selectedSlot = TradingBinBlockEntity.INSPECT_SLOT;
                    lastInspectionItem = ItemStack.EMPTY; // Force price input update
                    updateSettingsVisibility();
                    return true;
                }
            }
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        // Enter key in price input triggers the Set action
        if (keyCode == 257 && priceInput.isFocused() && !priceInput.getValue().isEmpty()) {
            TradingBinBlockEntity be = menu.getBlockEntity();
            if (be != null) {
                int price = Integer.parseInt(priceInput.getValue());
                int targetSlot = selectedSlot >= 0 ? selectedSlot : TradingBinBlockEntity.INSPECT_SLOT;
                ModNetwork.CHANNEL.send(PacketDistributor.SERVER.noArg(),
                        new SetPricePacket(be.getBlockPos(), targetSlot, price));
            }
            return true;
        }
        // Enter key in settings inputs triggers Apply
        if (keyCode == 257 && (taxInput.isFocused() || markupInput.isFocused())) {
            sendSettingsToServer();
            return true;
        }
        return super.keyPressed(keyCode, scanCode, modifiers);
    }

    @Override
    public void render(PoseStack poseStack, int mouseX, int mouseY, float partialTick) {
        renderBackground(poseStack);
        super.render(poseStack, mouseX, mouseY, partialTick);

        // Render price overlays on bin items
        TradingBinBlockEntity be = menu.getBlockEntity();
        if (be != null) {
            poseStack.pushPose();
            poseStack.translate(0, 0, 300);

            int x = this.leftPos;
            int y = this.topPos;

            // Draw selection highlight on selected bin slot
            if (selectedSlot >= 0 && selectedSlot < TradingBinBlockEntity.BIN_SIZE) {
                int slotX = x + 62 + (selectedSlot % 3) * 18 - 1;
                int slotY = y + 17 + (selectedSlot / 3) * 18 - 1;
                // Yellow highlight border
                fill(poseStack, slotX, slotY, slotX + 18, slotY + 1, 0xFFFFFF00);      // Top
                fill(poseStack, slotX, slotY + 17, slotX + 18, slotY + 18, 0xFFFFFF00); // Bottom
                fill(poseStack, slotX, slotY, slotX + 1, slotY + 18, 0xFFFFFF00);      // Left
                fill(poseStack, slotX + 17, slotY, slotX + 18, slotY + 18, 0xFFFFFF00); // Right
            }

            for (int i = 0; i < TradingBinBlockEntity.BIN_SIZE; i++) {
                ItemStack stack = be.getItem(i);
                if (!stack.isEmpty()) {
                    int price = be.getSetPrice(i);
                    if (price <= 0) price = PriceCalculator.getBaseValue(stack);

                    int fairValue = PriceCalculator.getBaseValue(stack);
                    int maxPrice = PriceCalculator.getMaxPrice(stack);
                    PriceCalculator.PriceRating rating = PriceCalculator.getPriceRating(price, fairValue, maxPrice);
                    int bgColor = switch (rating) {
                        case WILL_NOT_SELL -> 0xBBAA0000;
                        case OVERPRICED    -> 0xBB884400;
                        default            -> 0xBB000000;
                    };

                    int totalW = CoinRenderer.getPrimaryValueWidth(this.font, price);
                    int px = x + 62 + (i % 3) * 18 + 16 - totalW;
                    int py = y + 17 + (i / 3) * 18 + 9;
                    fill(poseStack, px - 1, py - 1, px + totalW + 1, py + 9, bgColor);
                    CoinRenderer.renderPrimaryValue(poseStack, this.font, px, py, price);
                }
            }

            poseStack.popPose();
        }

        renderTooltip(poseStack, mouseX, mouseY);
    }

    /**
     * Format copper pieces as a coin string (e.g., "1g 5s 3c").
     */
    private static String formatCoinText(int copper) {
        int g = copper / 100;
        int s = (copper % 100) / 10;
        int c = copper % 10;

        StringBuilder sb = new StringBuilder();
        if (g > 0) sb.append(g).append("g ");
        if (s > 0) sb.append(s).append("s ");
        if (c > 0 || sb.length() == 0) sb.append(c).append("c");
        return sb.toString().trim();
    }
}