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
    /** Checkbox pixel size for modifier toggles. */
    private static final int CB_SIZE = 8;

    public TradingBinScreen(TradingBinMenu menu, Inventory inv, Component title) {
        super(menu, inv, title);
        this.imageWidth = 384;
        this.imageHeight = 166;
    }

    // ==================== Initialization ====================

    @Override
    protected void init() {
        super.init();
        int x = this.leftPos;
        int y = this.topPos;

        // ---- Tab buttons inside right-panel tab bar ----
        binTabButton = addRenderableWidget(new Button(x + 183, y + 4, 96, 12,
                Component.literal("\u00A7e\u00A7lBin"), btn -> {
            activeTab = 0; updateTabVisibility(); updateTabLabels();
        }));
        configTabButton = addRenderableWidget(new Button(x + 283, y + 4, 96, 12,
                Component.literal("\u00A77Config"), btn -> {
            activeTab = 1; updateTabVisibility(); updateTabLabels();
        }));

        // ---- Bin tab widgets ----

        // Price input field (inside the right panel)
        this.priceInput = new EditBox(this.font, x + 184, y + 102, 108, 14,
                Component.literal("Price"));
        this.priceInput.setMaxLength(6);
        this.priceInput.setFilter(s -> s.isEmpty() || s.matches("\\d+"));
        this.addWidget(this.priceInput);

        // Set Price button (adjacent to price input)
        setButton = addRenderableWidget(new Button(x + 296, y + 102, 78, 14,
                Component.literal("Set Price"), btn -> {
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
        taxInput = new EditBox(this.font, x + 258, y + 96, 42, 12,
                Component.literal("Tax"));
        taxInput.setMaxLength(3);
        taxInput.setFilter(s -> s.isEmpty() || s.matches("\\d+"));
        taxInput.setValue(be != null ? String.valueOf(be.getCraftingTaxPercent()) : "15");
        this.addWidget(taxInput);

        // Min Markup % input
        markupInput = new EditBox(this.font, x + 258, y + 110, 42, 12,
                Component.literal("Markup"));
        markupInput.setMaxLength(3);
        markupInput.setFilter(s -> s.isEmpty() || s.matches("\\d+"));
        markupInput.setValue(be != null ? String.valueOf(be.getMinMarkupPercent()) : "0");
        this.addWidget(markupInput);

        // Auto-Price Mode cycling button
        String modeLabel = be != null ? be.getAutoPriceMode().getDisplayName() : "Auto Fair";
        autoPriceModeButton = addRenderableWidget(new Button(x + 186, y + 126, 106, 14,
                Component.literal(modeLabel), btn -> {
            TradingBinBlockEntity blockEntity = menu.getBlockEntity();
            if (blockEntity != null) {
                blockEntity.cycleAutoPriceMode();
                btn.setMessage(Component.literal(blockEntity.getAutoPriceMode().getDisplayName()));
            }
        }));

        // Apply Settings button
        applySettingsBtn = addRenderableWidget(new Button(x + 296, y + 126, 78, 14,
                Component.literal("Apply"), btn -> {
            sendSettingsToServer();
        }));

        updateTabVisibility();
    }

    // ==================== Item & Tab Helpers ====================

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

    /** Dark wood outer panel (matches Trading Post style). */
    private void drawPanel(PoseStack ps, int x, int y, int w, int h) {
        fill(ps, x, y, x + w, y + h, 0xFF1A1209);
        fill(ps, x + 1, y + 1, x + w - 1, y + 2, 0xFF8B7355);
        fill(ps, x + 1, y + 1, x + 2, y + h - 1, 0xFF8B7355);
        fill(ps, x + 1, y + h - 2, x + w - 1, y + h - 1, 0xFF2A1F14);
        fill(ps, x + w - 2, y + 1, x + w - 1, y + h - 1, 0xFF2A1F14);
        fill(ps, x + 2, y + 2, x + w - 2, y + h - 2, 0xFF5C4A32);
    }

    /** Dark inset sub-panel. */
    private void drawInsetPanel(PoseStack ps, int x, int y, int w, int h) {
        fill(ps, x, y, x + w, y + h, 0xFF2A1F14);
        fill(ps, x + 1, y + 1, x + w - 1, y + h - 1, 0xFF3E3226);
    }

    /** Inspection slot with dark wood styling. */
    private void drawInspectionSlot(PoseStack ps, int x, int y) {
        fill(ps, x - 2, y - 2, x + 18, y + 18, 0xFF1A1209);
        fill(ps, x - 1, y - 1, x + 17, y + 17, 0xFF2A1F14);
        fill(ps, x, y, x + 16, y + 16, 0xFF3E3226);
        // Top-left highlight edges
        fill(ps, x, y, x + 16, y + 1, 0xFF4A3D2B);
        fill(ps, x, y, x + 1, y + 16, 0xFF4A3D2B);
    }

    /** Horizontal divider line across the right panel content area. */
    private void drawDivider(PoseStack ps, int x, int y) {
        fill(ps, x + 183, y, x + 379, y + 1, 0xFF2A1F14);
    }

    // ==================== Render Background ====================

    @Override
    protected void renderBg(PoseStack poseStack, float partialTick, int mouseX, int mouseY) {
        RenderSystem.setShader(GameRenderer::getPositionTexShader);
        RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
        RenderSystem.setShaderTexture(0, TEXTURE);

        int x = this.leftPos;
        int y = this.topPos;

        // Blit the main dispenser-based texture (176x166) for left side
        this.blit(poseStack, x, y, 0, 0, 176, 166);

        // ---- Right panel: dark wood style ----
        drawPanel(poseStack, x + 178, y, 206, 166);
        drawInsetPanel(poseStack, x + 181, y + 3, 200, 14);    // tab bar
        drawInsetPanel(poseStack, x + 181, y + 19, 200, 145);  // content area

        // Update widget visibility each frame (inspection item may change)
        updateTabVisibility();

        if (activeTab == 1) {
            // ---- Config tab background ----
            drawDivider(poseStack, x, y + 92);  // between checkboxes and inputs
            taxInput.render(poseStack, mouseX, mouseY, partialTick);
            markupInput.render(poseStack, mouseX, mouseY, partialTick);
        } else {
            // ---- Bin tab background ----
            drawInspectionSlot(poseStack, x + 184, y + 34);

            // Section dividers
            drawDivider(poseStack, x, y + 54);   // below item info
            drawDivider(poseStack, x, y + 86);   // before set price section

            // Render price input field
            if (priceInput.visible) {
                this.priceInput.render(poseStack, mouseX, mouseY, partialTick);
            }
        }
    }

    // ==================== Render Labels ====================

    @Override
    protected void renderLabels(PoseStack poseStack, int mouseX, int mouseY) {
        // Title (left side, vanilla style)
        this.font.draw(poseStack, this.title, 8, 6, 0x404040);

        if (activeTab == 1) {
            renderConfigLabels(poseStack);
        } else {
            renderPriceBookLabels(poseStack);
        }

        // Inventory label (left side)
        this.font.draw(poseStack, this.playerInventoryTitle, 8, 73, 0x404040);
    }

    private void renderConfigLabels(PoseStack poseStack) {
        // Header
        drawCenteredString(poseStack, this.font, "\u00A7lConfig", 280, 21, 0xFFD700);

        this.font.draw(poseStack, "Price Modifiers", 186, 34, 0xBBAAAA);

        TradingBinBlockEntity cbe = menu.getBlockEntity();
        if (cbe != null) {
            int cbX = 186;
            drawCheckbox(poseStack, cbX, 46, cbe.isEnchantedMarkupEnabled(),
                    "Ench +" + cbe.getEnchantedMarkupPercent() + "%", 0xFF66DD66);
            drawCheckbox(poseStack, cbX, 58, cbe.isUsedDiscountEnabled(),
                    "Used -" + cbe.getUsedDiscountPercent() + "%", 0xFFCC9955);
            drawCheckbox(poseStack, cbX, 70, cbe.isDamagedDiscountEnabled(),
                    "Dmgd -" + cbe.getDamagedDiscountPercent() + "%", 0xFFDD5555);
            drawCheckbox(poseStack, cbX, 82, cbe.isRareMarkupEnabled(),
                    "Rare +" + cbe.getRareMarkupPercent() + "%", 0xFF8888FF);
        }

        this.font.draw(poseStack, "Tax %:", 186, 98, 0xCCCCCC);
        this.font.draw(poseStack, "Markup %:", 186, 112, 0xCCCCCC);

        // Estimated market time (if an item is present)
        if (cbe != null) {
            ItemStack inspectStack = getSelectedItem();
            if (!inspectStack.isEmpty()) {
                int baseValue = PriceCalculator.getBaseValue(inspectStack);
                int modifiedPrice = cbe.applyPriceModifiers(inspectStack, baseValue);
                int maxPrice = PriceCalculator.getMaxPrice(inspectStack);
                String est = TradingBinBlockEntity.getEstimatedMarketTime(
                        modifiedPrice, baseValue, maxPrice);
                this.font.draw(poseStack, "Est: " + est, 186, 144, 0x888888);
            }
        }
    }

    private void renderPriceBookLabels(PoseStack poseStack) {
        // Header
        drawCenteredString(poseStack, this.font, "\u00A7lPrice Book", 280, 21, 0xFFD700);

        TradingBinBlockEntity be = menu.getBlockEntity();
        if (be == null) return;

        ItemStack selectedStack = getSelectedItem();

        // Auto-populate price input when the selected item changes
        if (!ItemStack.matches(selectedStack, lastInspectionItem)) {
            lastInspectionItem = selectedStack.copy();
            if (!selectedStack.isEmpty()) {
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
            renderItemPriceInfo(poseStack, be, selectedStack);
        } else {
            renderEmptyHint(poseStack);
        }
    }

    private void renderItemPriceInfo(PoseStack poseStack, TradingBinBlockEntity be, ItemStack stack) {
        // ---- Item info section (above first divider y=54) ----

        // Item name (to the right of the inspection slot)
        String itemName = stack.getHoverName().getString();
        int maxNameW = 168;
        if (this.font.width(itemName) > maxNameW) {
            while (this.font.width(itemName + "..") > maxNameW && itemName.length() > 3) {
                itemName = itemName.substring(0, itemName.length() - 1);
            }
            itemName += "..";
        }
        this.font.draw(poseStack, itemName, 204, 37, 0xFFD700);

        // ---- Price breakdown section (y=58 to y=86) ----

        int taxPercent = be.getCraftingTaxPercent();
        PriceCalculator.PriceBreakdown bd = PriceCalculator.getBreakdown(stack, null, taxPercent);

        this.font.draw(poseStack, "Base: " + formatCoinText(bd.materialCost()), 184, 58, 0xAAAAAA);

        int nextY = 68;
        if (bd.hasTax()) {
            this.font.draw(poseStack, "+Tax: " + formatCoinText(bd.craftingTax()), 184, 68, 0xAAAAAA);
            nextY = 78;
        }
        this.font.draw(poseStack, "Max:  " + formatCoinText(bd.maxPrice()), 184, nextY, 0x888888);

        // ---- Set price section (y=90+) ----

        this.font.draw(poseStack, "Set Price:", 184, 90, 0xCCCCCC);

        // Parse current price from input
        int price = 0;
        if (!priceInput.getValue().isEmpty()) {
            try { price = Integer.parseInt(priceInput.getValue()); } catch (NumberFormatException ignored) {}
        }

        if (price <= 0) return;

        // ---- Results section (y=120+) ----

        PriceCalculator.PriceRating rating = PriceCalculator.getPriceRating(
                price, bd.materialCost(), bd.maxPrice());

        // Price position bar
        int barX = 184;
        int barY = 120;
        int barW = 190;
        int barH = 5;
        // Bar border
        fill(poseStack, barX, barY, barX + barW, barY + barH, 0xFF1A1209);
        // Bar background
        fill(poseStack, barX + 1, barY + 1, barX + barW - 1, barY + barH - 1, 0xFF2A2A2A);
        // Filled portion colored by rating
        if (bd.maxPrice() > 0) {
            float ratio = Math.min(1.0f, (float) price / bd.maxPrice());
            int fillW = Math.max(1, (int) ((barW - 2) * ratio));
            fill(poseStack, barX + 1, barY + 1, barX + 1 + fillW, barY + barH - 1,
                    rating.getColor() | 0xFF000000);
        }
        // Base value marker (white tick on bar)
        if (bd.maxPrice() > 0 && bd.materialCost() > 0) {
            float baseFrac = Math.min(1.0f, (float) bd.materialCost() / bd.maxPrice());
            int baseMarkerX = barX + 1 + (int) ((barW - 2) * baseFrac);
            fill(poseStack, baseMarkerX, barY - 1, baseMarkerX + 1, barY + barH + 1, 0xAAFFFFFF);
        }

        // Rating label
        this.font.draw(poseStack, rating.getLabel(), 184, 128, rating.getColor());

        // Estimated sell speed
        String est = TradingBinBlockEntity.getEstimatedMarketTime(
                price, bd.materialCost(), bd.maxPrice());
        this.font.draw(poseStack, "Sells: " + est, 184, 140, 0x888888);
    }

    private void renderEmptyHint(PoseStack poseStack) {
        drawCenteredString(poseStack, this.font, "Place an item in the", 280, 58, 0x888888);
        drawCenteredString(poseStack, this.font, "slot above, or click", 280, 70, 0x888888);
        drawCenteredString(poseStack, this.font, "a bin item to view", 280, 82, 0x888888);
        drawCenteredString(poseStack, this.font, "and set its price.", 280, 94, 0x888888);
    }

    // ==================== Mouse Handling ====================

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        // Config tab: check modifier checkbox clicks
        if (button == 0 && activeTab == 1) {
            TradingBinBlockEntity be = menu.getBlockEntity();
            if (be != null) {
                int x = this.leftPos;
                int y = this.topPos;
                int cbX = x + 186;
                int[] cbYs = getCheckboxYPositions();

                for (int i = 0; i < cbYs.length; i++) {
                    int cbY = y + cbYs[i];
                    if (mouseX >= cbX && mouseX < cbX + CB_SIZE + 80 &&
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
                                // Second click on same slot: deselect
                                selectedSlot = -1;
                                lastInspectionItem = ItemStack.EMPTY;
                                updateTabVisibility();
                                break; // let super handle the pickup
                            }
                            selectedSlot = i;
                            lastInspectionItem = ItemStack.EMPTY; // Force price input update
                            updateTabVisibility();
                            return true;
                        }
                    }
                }

                // Check inspection slot (at 184, 34)
                int inspectX = x + 184;
                int inspectY = y + 34;
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

    // ==================== Keyboard Handling ====================

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
        // Enter in tax/markup inputs triggers Apply
        if (keyCode == 257 && (taxInput.isFocused() || markupInput.isFocused())) {
            sendSettingsToServer();
            return true;
        }
        return super.keyPressed(keyCode, scanCode, modifiers);
    }

    // ==================== Render Overlays ====================

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

            // Selection highlight on selected bin slot (yellow border)
            if (selectedSlot >= 0 && selectedSlot < TradingBinBlockEntity.BIN_SIZE) {
                int slotX = x + 62 + (selectedSlot % 3) * 18 - 1;
                int slotY = y + 17 + (selectedSlot / 3) * 18 - 1;
                fill(poseStack, slotX, slotY, slotX + 18, slotY + 1, 0xFFFFFF00);       // Top
                fill(poseStack, slotX, slotY + 17, slotX + 18, slotY + 18, 0xFFFFFF00); // Bottom
                fill(poseStack, slotX, slotY, slotX + 1, slotY + 18, 0xFFFFFF00);       // Left
                fill(poseStack, slotX + 17, slotY, slotX + 18, slotY + 18, 0xFFFFFF00); // Right
            }

            // Selection highlight on inspection slot (gold border)
            if (selectedSlot == TradingBinBlockEntity.INSPECT_SLOT) {
                int slotX = x + 184 - 2;
                int slotY = y + 34 - 2;
                fill(poseStack, slotX, slotY, slotX + 20, slotY + 1, 0xFFFFD700);       // Top
                fill(poseStack, slotX, slotY + 19, slotX + 20, slotY + 20, 0xFFFFD700); // Bottom
                fill(poseStack, slotX, slotY, slotX + 1, slotY + 20, 0xFFFFD700);       // Left
                fill(poseStack, slotX + 19, slotY, slotX + 20, slotY + 20, 0xFFFFD700); // Right
            }

            // Price overlays on each bin item
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

    // ==================== Utility Methods ====================

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
     * Draw a custom checkbox with label, styled for the dark panel.
     */
    private void drawCheckbox(PoseStack ps, int x, int y, boolean checked, String label, int color) {
        // Outer border
        fill(ps, x, y, x + CB_SIZE, y + CB_SIZE, 0xFF1A1209);
        // Inner fill: dark green when checked, dark neutral when unchecked
        fill(ps, x + 1, y + 1, x + CB_SIZE - 1, y + CB_SIZE - 1, checked ? 0xFF336633 : 0xFF3E3226);
        // Bright green indicator dot when checked
        if (checked) {
            fill(ps, x + 2, y + 2, x + CB_SIZE - 2, y + CB_SIZE - 2, 0xFF55FF55);
        }
        // Label text
        this.font.draw(ps, label, x + CB_SIZE + 3, y, color);
    }

    /**
     * Y positions of modifier checkboxes for hit testing.
     * Returns: [enchantedY, usedY, damagedY, rareY].
     */
    private static int[] getCheckboxYPositions() {
        return new int[]{46, 58, 70, 82};
    }
}
