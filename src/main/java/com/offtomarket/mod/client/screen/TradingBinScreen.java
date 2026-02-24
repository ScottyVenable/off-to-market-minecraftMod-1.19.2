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

    /** Active tab: 0 = Bin (price book), 1 = Config (modifiers & settings). */
    private int activeTab = 0;
    private Button binTabButton;
    private Button configTabButton;

    // ---- Bin tab widgets ----
    private EditBox priceInput;
    private Button setButton;

    /** Track what was in the inspection slot last frame to auto-populate price input. */
    private ItemStack lastInspectionItem = ItemStack.EMPTY;

    /** 
     * Currently selected slot for pricing (0-8 = bin slots, 9 = inspect slot).
     * -1 means no selection (defaults to inspect slot behavior).
     */
    private int selectedSlot = -1;

    // ---- Config tab widgets ----
    private EditBox taxInput;
    private EditBox markupInput;
    private Button autoPriceModeButton;
    private Button applySettingsBtn;
    /** Checkbox size for modifier toggles (rendered manually). */
    private static final int CB_SIZE = 8;

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

        // ---- Tab buttons at top of book panel ----
        binTabButton = addRenderableWidget(new Button(x + 182, y + 8, 32, 12,
                Component.literal("\u00A7e\u00A7lBin"), btn -> {
            activeTab = 0; updateTabVisibility(); updateTabLabels();
        }));
        configTabButton = addRenderableWidget(new Button(x + 216, y + 8, 34, 12,
                Component.literal("\u00A77Config"), btn -> {
            activeTab = 1; updateTabVisibility(); updateTabLabels();
        }));

        // ---- Bin tab widgets ----

        // Price input field (inside the book panel)
        this.priceInput = new EditBox(this.font, x + 188, y + 76, 56, 12,
                Component.literal("Price"));
        this.priceInput.setMaxLength(6);
        this.priceInput.setFilter(s -> s.isEmpty() || s.matches("\\d+"));
        this.addWidget(this.priceInput);

        // Set Price button
        setButton = addRenderableWidget(new Button(x + 188, y + 90, 56, 14,
                Component.literal("Set"), btn -> {
            if (!priceInput.getValue().isEmpty()) {
                TradingBinBlockEntity be = menu.getBlockEntity();
                if (be != null) {
                    int price = Integer.parseInt(priceInput.getValue());
                    int targetSlot = selectedSlot >= 0 ? selectedSlot : TradingBinBlockEntity.INSPECT_SLOT;
                    ModNetwork.CHANNEL.send(PacketDistributor.SERVER.noArg(),
                            new SetPricePacket(be.getBlockPos(), targetSlot, price));
                }
            }
        }));

        // ---- Config tab widgets (settings) ----
        TradingBinBlockEntity be = menu.getBlockEntity();

        // Crafting Tax % input
        taxInput = new EditBox(this.font, x + 224, y + 84, 24, 10,
                Component.literal("Tax"));
        taxInput.setMaxLength(3);
        taxInput.setFilter(s -> s.isEmpty() || s.matches("\\d+"));
        taxInput.setValue(be != null ? String.valueOf(be.getCraftingTaxPercent()) : "15");
        this.addWidget(taxInput);

        // Min Markup % input
        markupInput = new EditBox(this.font, x + 224, y + 96, 24, 10,
                Component.literal("Markup"));
        markupInput.setMaxLength(3);
        markupInput.setFilter(s -> s.isEmpty() || s.matches("\\d+"));
        markupInput.setValue(be != null ? String.valueOf(be.getMinMarkupPercent()) : "0");
        this.addWidget(markupInput);

        // Auto-Price Mode cycling button
        String modeLabel = be != null ? be.getAutoPriceMode().getDisplayName() : "Auto Fair";
        autoPriceModeButton = addRenderableWidget(new Button(x + 187, y + 108, 62, 12,
                Component.literal(modeLabel), btn -> {
            TradingBinBlockEntity blockEntity = menu.getBlockEntity();
            if (blockEntity != null) {
                blockEntity.cycleAutoPriceMode();
                btn.setMessage(Component.literal(blockEntity.getAutoPriceMode().getDisplayName()));
            }
        }));

        // Apply Settings button
        applySettingsBtn = addRenderableWidget(new Button(x + 187, y + 122, 62, 12,
                Component.literal("Apply"), btn -> {
            sendSettingsToServer();
        }));

        updateTabVisibility();
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
     * Update tab button labels to show active tab.
     */
    private void updateTabLabels() {
        binTabButton.setMessage(Component.literal(activeTab == 0 ? "\u00A7e\u00A7lBin" : "\u00A77Bin"));
        configTabButton.setMessage(Component.literal(activeTab == 1 ? "\u00A7e\u00A7lConfig" : "\u00A77Config"));
    }

    /**
     * Toggle visibility of widgets based on active tab.
     * Also considers whether an item is selected for price book widgets.
     */
    private void updateTabVisibility() {
        ItemStack selectedItem = getSelectedItem();
        boolean hasSelectedItem = !selectedItem.isEmpty();

        // Bin tab widgets: visible only on Bin tab AND an item is selected
        boolean binActive = (activeTab == 0);
        priceInput.visible = binActive && hasSelectedItem;
        setButton.visible = binActive && hasSelectedItem;

        // Config tab widgets
        boolean configActive = (activeTab == 1);
        taxInput.visible = configActive;
        markupInput.visible = configActive;
        autoPriceModeButton.visible = configActive;
        applySettingsBtn.visible = configActive;
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
                        be.getAutoPriceMode().ordinal(),
                        be.isEnchantedMarkupEnabled(), be.getEnchantedMarkupPercent(),
                        be.isUsedDiscountEnabled(), be.getUsedDiscountPercent(),
                        be.isDamagedDiscountEnabled(), be.getDamagedDiscountPercent(),
                        be.isRareMarkupEnabled(), be.getRareMarkupPercent()));
    }

    // ==================== Drawing Helpers ====================

    private void drawSlot(PoseStack ps, int x, int y) {
        fill(ps, x - 1, y - 1, x + 17, y + 17, 0xFF373737);
        fill(ps, x, y, x + 16, y + 16, 0xFF8B8B8B);
    }

    private void drawBookPanel(PoseStack ps, int bx, int by) {
        int bw = 76, bh = 134;
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
        updateTabVisibility();

        if (activeTab == 1) {
            // ---- Config tab rendering ----
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

        if (activeTab == 1) {
            // ---- Config tab labels ----
            drawCenteredString(poseStack, this.font, "Config", 217, 22, 0xFF5C3D28);

            // Draw modifier checkboxes
            TradingBinBlockEntity cbe = menu.getBlockEntity();
            if (cbe != null) {
                int cbX = 187;
                drawCheckbox(poseStack, cbX, 42, cbe.isEnchantedMarkupEnabled(),
                        "Ench +" + cbe.getEnchantedMarkupPercent() + "%", 0xFF3B7A3B);
                drawCheckbox(poseStack, cbX, 52, cbe.isUsedDiscountEnabled(),
                        "Used -" + cbe.getUsedDiscountPercent() + "%", 0xFF8B4513);
                drawCheckbox(poseStack, cbX, 62, cbe.isDamagedDiscountEnabled(),
                        "Dmgd -" + cbe.getDamagedDiscountPercent() + "%", 0xFFAA0000);
                drawCheckbox(poseStack, cbX, 72, cbe.isRareMarkupEnabled(),
                        "Rare +" + cbe.getRareMarkupPercent() + "%", 0xFF5555FF);
            }

            this.font.draw(poseStack, "Tax%:", 189, 86, 0xFF3B2A18);
            this.font.draw(poseStack, "Mkp%:", 189, 98, 0xFF3B2A18);

            // Show estimated market time if an item is in the inspection slot
            if (cbe != null) {
                ItemStack inspectStack = getSelectedItem();
                if (!inspectStack.isEmpty()) {
                    int baseValue = PriceCalculator.getBaseValue(inspectStack);
                    int modifiedPrice = cbe.applyPriceModifiers(inspectStack, baseValue);
                    int maxPrice = PriceCalculator.getMaxPrice(inspectStack);
                    String est = TradingBinBlockEntity.getEstimatedMarketTime(
                            modifiedPrice, baseValue, maxPrice);
                    this.font.draw(poseStack, "Est: " + est, 189, 136, 0xFF6B4A35);
                }
            }
        } else {
            // ---- Price book labels ----
            // Book header
            drawCenteredString(poseStack, this.font, "Price Book", 217, 22, 0xFF5C3D28);

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
        // Config tab: check modifier checkbox clicks
        if (button == 0 && activeTab == 1) {
            TradingBinBlockEntity be = menu.getBlockEntity();
            if (be != null) {
                int x = this.leftPos;
                int y = this.topPos;
                int cbX = x + 187;
                int[] cbYs = getCheckboxYPositions();

                for (int i = 0; i < cbYs.length; i++) {
                    int cbY = y + cbYs[i];
                    if (mouseX >= cbX && mouseX < cbX + CB_SIZE + 50 &&
                        mouseY >= cbY && mouseY < cbY + CB_SIZE) {
                        switch (i) {
                            case 0 -> be.setEnchantedMarkupEnabled(!be.isEnchantedMarkupEnabled());
                            case 1 -> be.setUsedDiscountEnabled(!be.isUsedDiscountEnabled());
                            case 2 -> be.setDamagedDiscountEnabled(!be.isDamagedDiscountEnabled());
                            case 3 -> be.setRareMarkupEnabled(!be.isRareMarkupEnabled());
                        }
                        return true;
                    }
                }
            }
        }

        // Bin tab: check if clicked on a bin slot (left click)
        if (button == 0 && activeTab == 0) {
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
                            if (selectedSlot == i) {
                                // Second click on already-selected slot: deselect
                                selectedSlot = -1;
                                lastInspectionItem = ItemStack.EMPTY;
                                updateTabVisibility();
                                break; // let super.mouseClicked handle the pickup
                            }
                            selectedSlot = i;
                            lastInspectionItem = ItemStack.EMPTY; // Force price input update
                            updateTabVisibility();
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
                    updateTabVisibility();
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

    /**
     * Draw a manually rendered checkbox with label text.
     * Used in the Config tab for modifier toggles.
     *
     * @param ps      pose stack
     * @param x       checkbox X (label-space, relative to leftPos)
     * @param y       checkbox Y (label-space, relative to topPos)
     * @param checked whether the checkbox is checked
     * @param label   text label beside the checkbox
     * @param color   text color for the label
     */
    private void drawCheckbox(PoseStack ps, int x, int y, boolean checked, String label, int color) {
        // Outer border
        fill(ps, x, y, x + CB_SIZE, y + CB_SIZE, 0xFF555555);
        // Inner background
        fill(ps, x + 1, y + 1, x + CB_SIZE - 1, y + CB_SIZE - 1, checked ? 0xFF336633 : 0xFFAAAAAA);
        // Check mark
        if (checked) {
            this.font.draw(ps, "\u00A72x", x + 1, y, 0xFFFFFFFF);
        }
        // Label
        this.font.draw(ps, label, x + CB_SIZE + 2, y, color);
    }

    /**
     * Get the Y positions of modifier checkboxes for hit testing.
     * Returns int array: [enchantedY, usedY, damagedY, rareY].
     */
    private static int[] getCheckboxYPositions() {
        return new int[]{42, 52, 62, 72};
    }
}