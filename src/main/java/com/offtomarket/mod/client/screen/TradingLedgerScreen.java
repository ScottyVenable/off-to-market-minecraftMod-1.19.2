package com.offtomarket.mod.client.screen;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;
import com.offtomarket.mod.block.entity.TradingLedgerBlockEntity;
import com.offtomarket.mod.data.PriceCalculator;
import com.offtomarket.mod.menu.TradingLedgerMenu;
import com.offtomarket.mod.network.ModNetwork;
import com.offtomarket.mod.network.SetPricePacket;
import com.offtomarket.mod.network.UpgradeCaravanWeightPacket;
import com.offtomarket.mod.network.UpdateBinSettingsPacket;
import com.offtomarket.mod.network.WithdrawBinItemPacket;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.network.PacketDistributor;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * Trading Bin GUI — management-only interface.
 * Left panel: searchable, scrollable list of bin contents.
 * Right panel: Price Book / Fees tabs.
 * Items are auto-filled from adjacent container blocks (chest, barrel, etc.) every ~3 s.
 */
public class TradingLedgerScreen extends AbstractContainerScreen<TradingLedgerMenu> {

    /** Active tab: 0 = Bin (price book), 1 = Fees (modifiers & settings), 2 = History (past orders), 3 = Income. */
    private int activeTab = 0;
    private Button binTabButton;
    private Button configTabButton;
    private Button historyTabButton;
    private Button incomeTabButton;
    private int historyScrollOffset = 0;
    private int incomeScrollOffset = 0;

    // ---- Bin tab widgets ----
    private EditBox priceInput;
    private Button setButton;
    private Button withdrawButton;

    // ---- Search & list ----
    private EditBox searchInput;
    private int selectedSlot = -1;
    private ItemStack lastSelectedItem = ItemStack.EMPTY;
    private final List<Integer> filteredSlots = new ArrayList<>();
    private int listScrollOffset = 0;

    // ---- List layout ----
    private static final int LIST_LEFT = 6;
    private static final int LIST_TOP = 36;
    private static final int LIST_WIDTH = 166;
    private static final int ROW_HEIGHT = 14;
    private static final int MAX_VISIBLE = 12;

    // ---- Config tab widgets ----
    private EditBox taxInput;
    private EditBox markupInput;
    private Button autoPriceModeButton;
    private Button applySettingsBtn;
    private Button upgradeCaravanBtn;
    // ---- Right-click context menu ----
    /** The bin slot index for the active context menu, or -1 if none. */
    private int contextMenuSlot = -1;
    private int contextMenuX;
    private int contextMenuY;

    public TradingLedgerScreen(TradingLedgerMenu menu, Inventory inv, Component title) {
        super(menu, inv, title);
        this.imageWidth = 384;
        this.imageHeight = 230;
    }

    // ==================== Initialization ====================

    @Override
    protected void init() {
        super.init();
        int x = this.leftPos;
        int y = this.topPos;

        // ---- Tab buttons (right panel tab bar — 4 equal tabs at 49px each) ----
        binTabButton = addRenderableWidget(new Button(x + 182, y + 4, 49, 12,
                Component.literal("\u00A7e\u00A7lBin"), btn -> {
            activeTab = 0; historyScrollOffset = 0; incomeScrollOffset = 0; updateTabVisibility(); updateTabLabels();
        }));
        configTabButton = addRenderableWidget(new Button(x + 232, y + 4, 49, 12,
                Component.literal("\u00A77Fees"), btn -> {
            activeTab = 1; historyScrollOffset = 0; incomeScrollOffset = 0; updateTabVisibility(); updateTabLabels();
        }));
        historyTabButton = addRenderableWidget(new Button(x + 282, y + 4, 49, 12,
                Component.literal("\u00A77History"), btn -> {
            activeTab = 2; historyScrollOffset = 0; incomeScrollOffset = 0; updateTabVisibility(); updateTabLabels();
        }));
        incomeTabButton = addRenderableWidget(new Button(x + 332, y + 4, 49, 12,
                Component.literal("\u00A77Income"), btn -> {
            activeTab = 3; historyScrollOffset = 0; incomeScrollOffset = 0; updateTabVisibility(); updateTabLabels();
        }));

        // ---- Search input (left panel, below title) ----
        searchInput = new EditBox(this.font, x + LIST_LEFT, y + 20, LIST_WIDTH, 12,
                Component.literal("Search"));
        searchInput.setMaxLength(20);
        searchInput.setResponder(text -> {
            updateFilteredSlots();
            if (selectedSlot >= 0 && !filteredSlots.contains(selectedSlot)) {
                selectedSlot = -1;
                lastSelectedItem = ItemStack.EMPTY;
                updateTabVisibility();
            }
        });
        this.addWidget(searchInput);

        // ---- Bin tab widgets (right panel) ----
        priceInput = new EditBox(this.font, x + 184, y + 102, 108, 14,
                Component.literal("Price"));
        priceInput.setMaxLength(6);
        priceInput.setFilter(s -> s.isEmpty() || s.matches("\\d+"));
        this.addWidget(priceInput);

        setButton = addRenderableWidget(new Button(x + 296, y + 102, 78, 14,
                Component.literal("Set Price"), btn -> {
            if (!priceInput.getValue().isEmpty()) {
                TradingLedgerBlockEntity be = menu.getBlockEntity();
                if (be != null && selectedSlot >= 0) {
                    int price = Integer.parseInt(priceInput.getValue());
                    ModNetwork.CHANNEL.send(PacketDistributor.SERVER.noArg(),
                            new SetPricePacket(be.getBlockPos(), selectedSlot, price));
                }
            }
        }));

        withdrawButton = addRenderableWidget(new Button(x + 184, y + 210, 80, 14,
                Component.literal("Withdraw"), btn -> {
            TradingLedgerBlockEntity be = menu.getBlockEntity();
            if (be != null && selectedSlot >= 0 && selectedSlot < TradingLedgerBlockEntity.BIN_SIZE) {
                ModNetwork.CHANNEL.send(PacketDistributor.SERVER.noArg(),
                        new WithdrawBinItemPacket(be.getBlockPos(), selectedSlot));
                selectedSlot = -1;
                lastSelectedItem = ItemStack.EMPTY;
                updateTabVisibility();
            }
        }));

        // ---- Fees tab widgets ----
        TradingLedgerBlockEntity be = menu.getBlockEntity();

        taxInput = new EditBox(this.font, x + 260, y + 32, 42, 12,
                Component.literal("Tax"));
        taxInput.setMaxLength(3);
        taxInput.setFilter(s -> s.isEmpty() || s.matches("\\d+"));
        taxInput.setValue(be != null ? String.valueOf(be.getCraftingTaxPercent()) : "15");
        this.addWidget(taxInput);

        markupInput = new EditBox(this.font, x + 260, y + 46, 42, 12,
                Component.literal("Markup"));
        markupInput.setMaxLength(3);
        markupInput.setFilter(s -> s.isEmpty() || s.matches("\\d+"));
        markupInput.setValue(be != null ? String.valueOf(be.getMinMarkupPercent()) : "0");
        this.addWidget(markupInput);

        String modeLabel = be != null ? be.getAutoPriceMode().getDisplayName() : "Auto Fair";
        autoPriceModeButton = addRenderableWidget(new Button(x + 184, y + 64, 106, 14,
                Component.literal(modeLabel), btn -> {
            TradingLedgerBlockEntity blockEntity = menu.getBlockEntity();
            if (blockEntity != null) {
                blockEntity.cycleAutoPriceMode();
                btn.setMessage(Component.literal(blockEntity.getAutoPriceMode().getDisplayName()));
            }
        }));

        applySettingsBtn = addRenderableWidget(new Button(x + 294, y + 64, 80, 14,
                Component.literal("Apply"), btn -> sendSettingsToServer()));

        upgradeCaravanBtn = addRenderableWidget(new Button(x + 184, y + 213, 190, 14,
            Component.literal("Upgrade Caravan"), btn -> {
            TradingLedgerBlockEntity blockEntity = menu.getBlockEntity();
            if (blockEntity != null) {
            ModNetwork.CHANNEL.send(PacketDistributor.SERVER.noArg(),
                new UpgradeCaravanWeightPacket(blockEntity.getBlockPos()));
            }
        }));

        updateFilteredSlots();
        updateTabVisibility();
    }

    // ==================== List & Selection Helpers ====================

    /** Rebuild the list of bin slot indices that match the current search filter. */
    private void updateFilteredSlots() {
        filteredSlots.clear();
        TradingLedgerBlockEntity be = menu.getBlockEntity();
        if (be == null) return;
        String filter = searchInput != null ? searchInput.getValue().toLowerCase(Locale.ROOT) : "";
        for (int i = 0; i < TradingLedgerBlockEntity.BIN_SIZE; i++) {
            ItemStack stack = be.getItem(i);
            if (!stack.isEmpty()) {
                if (filter.isEmpty() ||
                    stack.getHoverName().getString().toLowerCase(Locale.ROOT).contains(filter)) {
                    filteredSlots.add(i);
                }
            }
        }
        int maxScroll = Math.max(0, filteredSlots.size() - MAX_VISIBLE);
        listScrollOffset = Math.max(0, Math.min(listScrollOffset, maxScroll));
    }

    /** Get the currently selected item for pricing. */
    private ItemStack getSelectedItem() {
        TradingLedgerBlockEntity be = menu.getBlockEntity();
        if (be == null || selectedSlot < 0 || selectedSlot >= TradingLedgerBlockEntity.BIN_SIZE) {
            return ItemStack.EMPTY;
        }
        return be.getItem(selectedSlot);
    }

    /** Update tab button labels to show active state. */
    private void updateTabLabels() {
        binTabButton.setMessage(Component.literal(activeTab == 0 ? "\u00A7e\u00A7lBin" : "\u00A77Bin"));
        configTabButton.setMessage(Component.literal(activeTab == 1 ? "\u00A7e\u00A7lFees" : "\u00A77Fees"));
        historyTabButton.setMessage(Component.literal(activeTab == 2 ? "\u00A7e\u00A7lHistory" : "\u00A77History"));
        if (incomeTabButton != null)
            incomeTabButton.setMessage(Component.literal(activeTab == 3 ? "\u00A7e\u00A7lIncome" : "\u00A77Income"));
    }

    /** Toggle widget visibility based on active tab and selection state. */
    private void updateTabVisibility() {
        ItemStack selected = getSelectedItem();
        boolean hasItem = !selected.isEmpty();
        boolean binActive = (activeTab == 0);

        priceInput.visible = binActive && hasItem;
        setButton.visible = binActive && hasItem;
        withdrawButton.visible = binActive && hasItem;

        boolean configActive = (activeTab == 1);
        taxInput.visible = configActive;
        markupInput.visible = configActive;
        autoPriceModeButton.visible = configActive;
        applySettingsBtn.visible = configActive;
        upgradeCaravanBtn.visible = configActive;
        // History tab (activeTab == 2) and Income tab (activeTab == 3) have no widgets — labels only
    }

    /** Send current fee/modifier settings to the server. */
    private void sendSettingsToServer() {
        TradingLedgerBlockEntity be = menu.getBlockEntity();
        if (be == null) return;
        int tax = 15, markup = 0;
        try { tax = Integer.parseInt(taxInput.getValue()); } catch (NumberFormatException ignored) {}
        try { markup = Integer.parseInt(markupInput.getValue()); } catch (NumberFormatException ignored) {}
        // Enabled flags are always true — modifiers auto-apply based on item properties
        ModNetwork.CHANNEL.send(PacketDistributor.SERVER.noArg(),
                new UpdateBinSettingsPacket(be.getBlockPos(), tax, markup,
                        be.getAutoPriceMode().ordinal(),
                        true, be.getEnchantedMarkupPercent(),
                        true, be.getUsedDiscountPercent(),
                        true, be.getDamagedDiscountPercent(),
                        true, be.getRareMarkupPercent()));
    }

    // ==================== Container Tick ====================

    @Override
    public void containerTick() {
        super.containerTick();
        if (searchInput != null) searchInput.tick();
        if (priceInput != null) priceInput.tick();
        if (taxInput != null) taxInput.tick();
        if (markupInput != null) markupInput.tick();

        updateFilteredSlots();

        // Deselect if item was withdrawn or removed
        if (selectedSlot >= 0) {
            TradingLedgerBlockEntity be = menu.getBlockEntity();
            if (be == null || be.getItem(selectedSlot).isEmpty()) {
                selectedSlot = -1;
                lastSelectedItem = ItemStack.EMPTY;
                updateTabVisibility();
            }
        }
    }

    // ==================== Drawing Helpers ====================

    /** Dark wood outer panel (matches Trading Post style). */
    private void drawPanel(PoseStack ps, int x, int y, int w, int h) {
        OtmGuiTheme.drawPanel(ps, x, y, w, h);
    }

    /** Dark inset sub-panel. */
    private void drawInsetPanel(PoseStack ps, int x, int y, int w, int h) {
        OtmGuiTheme.drawInsetPanel(ps, x, y, w, h);
    }

    /** Horizontal divider line across the right panel content area. */
    private void drawDivider(PoseStack ps, int x, int y) {
        OtmGuiTheme.drawDividerH(ps, x + 183, x + 379, y);
    }

    // ==================== Render Background ====================

    @Override
    protected void renderBg(PoseStack poseStack, float partialTick, int mouseX, int mouseY) {
        RenderSystem.setShader(GameRenderer::getPositionTexShader);
        RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);

        int x = this.leftPos;
        int y = this.topPos;

        // ---- Left panel: dark wood (replaces vanilla dispenser texture) ----
        drawPanel(poseStack, x, y, 178, 230);
        drawInsetPanel(poseStack, x + 3, y + 18, 172, 14);     // search bar area
        drawInsetPanel(poseStack, x + 3, y + 34, 172, 172);    // list area (12 rows)

        // ---- Right panel: dark wood ----
        drawPanel(poseStack, x + 178, y, 206, 230);
        drawInsetPanel(poseStack, x + 181, y + 3, 200, 14);    // tab bar
        drawInsetPanel(poseStack, x + 181, y + 19, 200, 207);  // content area

        updateTabVisibility();

        // Search input
        searchInput.render(poseStack, mouseX, mouseY, partialTick);

        // ---- List rows (backgrounds and text) ----
        TradingLedgerBlockEntity be = menu.getBlockEntity();
        if (be != null) {
            int listX = x + LIST_LEFT;
            int listY = y + LIST_TOP;

            int visibleCount = Math.min(MAX_VISIBLE, Math.max(0, filteredSlots.size() - listScrollOffset));
            for (int i = 0; i < visibleCount; i++) {
                int slot = filteredSlots.get(listScrollOffset + i);
                ItemStack stack = be.getItem(slot);
                if (stack.isEmpty()) continue;

                int rowY = listY + i * ROW_HEIGHT;
                boolean isSelected = (slot == selectedSlot);
                boolean isHovered = mouseX >= listX && mouseX < listX + LIST_WIDTH
                        && mouseY >= rowY && mouseY < rowY + ROW_HEIGHT;

                // Row background highlight
                if (isSelected) {
                    fill(poseStack, listX, rowY, listX + LIST_WIDTH, rowY + ROW_HEIGHT, 0x80FFD700);
                } else if (isHovered) {
                    fill(poseStack, listX, rowY, listX + LIST_WIDTH, rowY + ROW_HEIGHT, 0x40FFFFFF);
                }

                // Subtle separator line between rows
                if (i > 0) {
                    fill(poseStack, listX + 1, rowY, listX + LIST_WIDTH - 1, rowY + 1, 0x20FFFFFF);
                }

                // Item name (offset for icon rendered later in render())
                String name = stack.getHoverName().getString();
                int maxNameW = 88;
                if (this.font.width(name) > maxNameW) {
                    while (this.font.width(name + "..") > maxNameW && name.length() > 3)
                        name = name.substring(0, name.length() - 1);
                    name += "..";
                }
                this.font.draw(poseStack, name, listX + 16, rowY + 3,
                        isSelected ? 0xFFD700 : 0xCCCCCC);

                // Price (right-aligned, using CoinRenderer)
                int price = be.getEffectivePriceForSlot(slot);
                if (price <= 0) price = PriceCalculator.getBaseValue(stack);
                int priceW = CoinRenderer.getCompactCoinValueWidth(this.font, price);
                CoinRenderer.renderCompactCoinValue(poseStack, this.font,
                        listX + LIST_WIDTH - priceW - 2, rowY + 3, price);
            }

            // Scroll indicators
            int maxScroll = Math.max(0, filteredSlots.size() - MAX_VISIBLE);
            if (maxScroll > 0) {
                if (listScrollOffset > 0) {
                    this.font.draw(poseStack, "\u25B2", listX + LIST_WIDTH - 8, listY - 10, 0x999999);
                }
                if (listScrollOffset < maxScroll) {
                    this.font.draw(poseStack, "\u25BC", listX + LIST_WIDTH - 8,
                            listY + MAX_VISIBLE * ROW_HEIGHT + 1, 0x999999);
                }
            }

            // Empty state message
            if (filteredSlots.isEmpty()) {
                if (searchInput != null && !searchInput.getValue().isEmpty()) {
                    drawCenteredString(poseStack, this.font, "No matches", x + 89, y + 110, 0x666666);
                } else {
                    drawCenteredString(poseStack, this.font, "Bin is empty", x + 89, y + 104, 0x666666);
                    drawCenteredString(poseStack, this.font, "Place a chest or barrel", x + 89, y + 120, 0x555555);
                    drawCenteredString(poseStack, this.font, "next to the bin to", x + 89, y + 132, 0x555555);
                    drawCenteredString(poseStack, this.font, "auto-fill it.", x + 89, y + 144, 0x555555);
                }
            }
        }

        // ---- Right panel tab content ----
        if (activeTab == 1) {
            // Fees tab
            drawDivider(poseStack, x, y + 62);
            drawDivider(poseStack, x, y + 80);
            drawDivider(poseStack, x, y + 130);
            taxInput.render(poseStack, mouseX, mouseY, partialTick);
            markupInput.render(poseStack, mouseX, mouseY, partialTick);
        } else if (activeTab == 2) {
            // History tab — header divider only; content drawn in renderHistoryLabels
            drawDivider(poseStack, x, y + 30);
        } else if (activeTab == 3) {
            // Income tab — header divider only; content drawn in renderIncomeLabels
            drawDivider(poseStack, x, y + 30);
        } else {
            // Bin tab
            drawDivider(poseStack, x, y + 54);
            drawDivider(poseStack, x, y + 86);
            if (priceInput.visible) {
                priceInput.render(poseStack, mouseX, mouseY, partialTick);
            }
        }
    }

    // ==================== Render Labels ====================

    @Override
    protected void renderLabels(PoseStack poseStack, int mouseX, int mouseY) {
        // Left panel title
        this.font.draw(poseStack, "\u00A7e\u00A7lTrading Bin", 8, 6, 0xFFD700);

        // Item count indicator
        this.font.draw(poseStack, "\u00A77" + filteredSlots.size() + "/" +
                TradingLedgerBlockEntity.BIN_SIZE, 130, 6, 0x888888);

        TradingLedgerBlockEntity be = menu.getBlockEntity();
        if (be != null) {
            String payout = "Payout: " + formatCoinText(be.getTotalProposedPayout());
            this.font.draw(poseStack, payout, 8, 220, 0xCCAA66);

            int currentWeight = be.getCurrentCaravanWeight();
            int capacity = be.getCaravanWeightCapacity();
            int weightColor = currentWeight >= capacity ? 0xFF5555 : 0x88CC88;
            this.font.draw(poseStack, "Wt " + currentWeight + "/" + capacity, 112, 220, weightColor);
        }

        if (activeTab == 1) {
            renderConfigLabels(poseStack);
        } else if (activeTab == 2) {
            renderHistoryLabels(poseStack);
        } else if (activeTab == 3) {
            renderIncomeLabels(poseStack);
        } else {
            renderPriceBookLabels(poseStack);
        }
        // No inventory label — player inventory is not displayed
    }

    private void renderConfigLabels(PoseStack poseStack) {
        // Header
        drawCenteredString(poseStack, this.font, "\u00A7lFees", 280, 21, 0xFFD700);

        // Fee setting labels
        this.font.draw(poseStack, "Trading Tax:", 184, 35, 0xCCCCCC);
        this.font.draw(poseStack, "%", 304, 35, 0x999999);
        this.font.draw(poseStack, "Min Markup:", 184, 49, 0xCCCCCC);
        this.font.draw(poseStack, "%", 304, 49, 0x999999);

        TradingLedgerBlockEntity cbe = menu.getBlockEntity();
        if (cbe == null) return;

        if (upgradeCaravanBtn != null) {
            upgradeCaravanBtn.setMessage(Component.literal(
                "Upgrade Caravan (" + formatCoinText(cbe.getNextCaravanUpgradeCost()) + ")"));
        }

        // Modifier section header with item context
        ItemStack inspectStack = getSelectedItem();
        boolean hasItem = !inspectStack.isEmpty();

        this.font.draw(poseStack, "Modifiers", 184, 83, 0xBBAAAA);
        if (hasItem) {
            String itemName = inspectStack.getHoverName().getString();
            int maxNameW = 130;
            if (this.font.width(itemName) > maxNameW) {
                while (this.font.width(itemName + "..") > maxNameW && itemName.length() > 3)
                    itemName = itemName.substring(0, itemName.length() - 1);
                itemName += "..";
            }
            this.font.draw(poseStack, "\u00A77(" + itemName + ")", 235, 83, 0x777777);
        }

        // Auto-applied price modifiers (always active — no toggles needed)
        boolean enchApplicable = hasItem && TradingLedgerBlockEntity.isEnchantmentApplicable(inspectStack);
        boolean usedApplicable = hasItem && TradingLedgerBlockEntity.isDurabilityApplicable(inspectStack)
                && !TradingLedgerBlockEntity.isHeavilyDamaged(inspectStack);
        boolean dmgdApplicable = hasItem && TradingLedgerBlockEntity.isDurabilityApplicable(inspectStack)
                && TradingLedgerBlockEntity.isHeavilyDamaged(inspectStack);
        boolean rareApplicable = hasItem && TradingLedgerBlockEntity.isRarityApplicable(inspectStack);

        int modX = 184;
        int modY = 94;
        // Show modifier label + percent; highlight green if it applies to current item, gray otherwise
        this.font.draw(poseStack,
                (enchApplicable ? "\u00A7a" : "\u00A78") + "Ench +" + cbe.getEnchantedMarkupPercent() + "%",
                modX, modY, 0xFFFFFF);
        this.font.draw(poseStack,
                (usedApplicable ? "\u00A7e" : "\u00A78") + "Used -" + cbe.getUsedDiscountPercent() + "%",
                modX, modY + 11, 0xFFFFFF);
        this.font.draw(poseStack,
                (dmgdApplicable ? "\u00A7c" : "\u00A78") + "Dmgd -" + cbe.getDamagedDiscountPercent() + "%",
                modX, modY + 22, 0xFFFFFF);
        this.font.draw(poseStack,
                (rareApplicable ? "\u00A79" : "\u00A78") + "Rare +" + cbe.getRareMarkupPercent() + "%",
                modX, modY + 33, 0xFFFFFF);
        // Auto-apply status indicators
        if (hasItem) {
            int indX = 305;
            this.font.draw(poseStack, enchApplicable ? "\u00A7a\u2713" : "\u00A78–", indX, modY, 0xFFFFFF);
            this.font.draw(poseStack, usedApplicable ? "\u00A7a\u2713" : "\u00A78–", indX, modY + 11, 0xFFFFFF);
            this.font.draw(poseStack, dmgdApplicable ? "\u00A7a\u2713" : "\u00A78–", indX, modY + 22, 0xFFFFFF);
            this.font.draw(poseStack, rareApplicable ? "\u00A7a\u2713" : "\u00A78–", indX, modY + 33, 0xFFFFFF);
        }

        // Summary: price breakdown for selected item
        if (hasItem) {
            int baseValue = PriceCalculator.getBaseValue(inspectStack);
            int modifiedPrice = cbe.getEffectivePrice(inspectStack, baseValue);
            int maxPrice = PriceCalculator.getMaxPrice(inspectStack);
            String est = TradingLedgerBlockEntity.getEstimatedMarketTime(
                    modifiedPrice, baseValue, maxPrice);

            this.font.draw(poseStack, "Base: " + formatCoinText(baseValue), 184, 134, 0xAAAAAA);
            this.font.draw(poseStack, "\u00A76\u2192 " + formatCoinText(modifiedPrice), 264, 134, 0xFFAA00);
            this.font.draw(poseStack, "Sells: " + est, 184, 143, 0x888888);
        } else {
            this.font.draw(poseStack, "Select an item to preview modifiers.", 184, 134, 0x666666);
        }

        // Caravan Weight display removed — not shown in Fees panel
    }

    private void renderHistoryLabels(PoseStack poseStack) {
        drawCenteredString(poseStack, this.font, "\u00A7lPast Orders", 280, 21, 0xFFD700);

        TradingLedgerBlockEntity be = menu.getBlockEntity();
        if (be == null) return;

        java.util.List<TradingLedgerBlockEntity.LedgerShipmentRecord> history = be.getShipmentHistory();

        if (history.isEmpty()) {
            this.font.draw(poseStack, "No shipments yet.", 184, 42, 0x666666);
            this.font.draw(poseStack, "Ship items from the Trading Post", 184, 54, 0x555555);
            this.font.draw(poseStack, "to record orders here.", 184, 66, 0x555555);
            return;
        }

        this.font.draw(poseStack, "Town", 184, 34, 0xBBAAAA);
        this.font.draw(poseStack, "Items", 256, 34, 0xBBAAAA);
        this.font.draw(poseStack, "Value", 296, 34, 0xBBAAAA);
        this.font.draw(poseStack, "When", 348, 34, 0xBBAAAA);

        int rowH = 12;
        int startY = 40;
        int bottomY = 220;
        int visibleRows = (bottomY - startY) / rowH;  // ~15
        int maxScroll = Math.max(0, history.size() - visibleRows);
        historyScrollOffset = Math.min(historyScrollOffset, maxScroll);

        long nowTicks = this.minecraft != null && this.minecraft.level != null
                ? this.minecraft.level.getGameTime() : 0;

        for (int i = 0; i < visibleRows && (historyScrollOffset + i) < history.size(); i++) {
            TradingLedgerBlockEntity.LedgerShipmentRecord rec = history.get(historyScrollOffset + i);
            int ry = startY + i * rowH;

            String tName = rec.townDisplayName;
            while (this.font.width(tName) > 65 && tName.length() > 3)
                tName = tName.substring(0, tName.length() - 1);
            if (!tName.equals(rec.townDisplayName)) tName += "..";
            this.font.draw(poseStack, tName, 184, ry, 0xCCAA66);

            this.font.draw(poseStack, rec.totalItems + "x", 256, ry, 0xAAAAAA);
            this.font.draw(poseStack, formatCoinText(rec.totalValue), 296, ry, 0x88CC88);

            long deltaTicks = nowTicks - rec.gameTime;
            String when;
            if (deltaTicks < 20 * 60) when = "< 1 min";
            else if (deltaTicks < 20 * 3600) when = (deltaTicks / (20 * 60)) + " min";
            else if (deltaTicks < 20 * 86400L) when = (deltaTicks / (20 * 3600)) + " hr";
            else when = (deltaTicks / (20 * 86400L)) + " day" + (deltaTicks >= 20 * 86400L * 2 ? "s" : "");
            this.font.draw(poseStack, when, 378 - this.font.width(when), ry, 0x666666);
        }

        if (historyScrollOffset > 0) this.font.draw(poseStack, "\u25B2", 370, startY, 0x6B5A3E);
        if (historyScrollOffset < maxScroll) this.font.draw(poseStack, "\u25BC", 370, bottomY - rowH + 2, 0x6B5A3E);
    }

    private void renderIncomeLabels(PoseStack poseStack) {
        drawCenteredString(poseStack, this.font, "\u00A7lIncome Summary", 280, 21, 0xFFD700);

        TradingLedgerBlockEntity be = menu.getBlockEntity();
        if (be == null) return;

        java.util.List<TradingLedgerBlockEntity.LedgerShipmentRecord> history = be.getShipmentHistory();

        if (history.isEmpty()) {
            this.font.draw(poseStack, "No income data yet.", 184, 42, 0x666666);
            this.font.draw(poseStack, "Complete shipments to see", 184, 54, 0x555555);
            this.font.draw(poseStack, "income totals here.", 184, 66, 0x555555);
            return;
        }

        // Aggregate totals
        long totalRevenue = 0;
        int totalItems = 0;
        java.util.Map<String, Long> revenueByTown = new java.util.LinkedHashMap<>();
        java.util.Map<String, Integer> itemsByTown = new java.util.LinkedHashMap<>();
        for (TradingLedgerBlockEntity.LedgerShipmentRecord rec : history) {
            totalRevenue += rec.totalValue;
            totalItems += rec.totalItems;
            revenueByTown.merge(rec.townDisplayName, (long) rec.totalValue, Long::sum);
            itemsByTown.merge(rec.townDisplayName, rec.totalItems, Integer::sum);
        }

        // Summary row
        int sy = 34;
        this.font.draw(poseStack, "Total Revenue:", 184, sy, 0xBBAAAA);
        this.font.draw(poseStack, formatCoinText((int) Math.min(totalRevenue, Integer.MAX_VALUE)), 276, sy, 0x88FF88);
        this.font.draw(poseStack, "Items: " + totalItems, 184, sy + 11, 0x888888);
        this.font.draw(poseStack, "Shipments: " + history.size(), 184, sy + 22, 0x888888);

        // Column headers for by-town breakdown
        int rowH = 11;
        int startY = sy + 36;
        int bottomY = 220;
        int visibleRows = (bottomY - startY) / rowH;
        java.util.List<java.util.Map.Entry<String, Long>> townEntries =
                new java.util.ArrayList<>(revenueByTown.entrySet());
        townEntries.sort((a, b) -> Long.compare(b.getValue(), a.getValue())); // sort by revenue desc
        int maxScroll = Math.max(0, townEntries.size() - visibleRows);
        incomeScrollOffset = Math.min(incomeScrollOffset, maxScroll);

        this.font.draw(poseStack, "Town", 184, startY - 9, 0xBBAAAA);
        this.font.draw(poseStack, "Revenue", 284, startY - 9, 0xBBAAAA);
        this.font.draw(poseStack, "Items", 350, startY - 9, 0xBBAAAA);

        for (int i = 0; i < visibleRows && (incomeScrollOffset + i) < townEntries.size(); i++) {
            java.util.Map.Entry<String, Long> entry = townEntries.get(incomeScrollOffset + i);
            int ry = startY + i * rowH;
            String tName = entry.getKey();
            while (this.font.width(tName) > 88 && tName.length() > 3)
                tName = tName.substring(0, tName.length() - 1);
            if (!tName.equals(entry.getKey())) tName += "..";
            this.font.draw(poseStack, tName, 184, ry, 0xCCAA66);
            this.font.draw(poseStack, formatCoinText((int) Math.min(entry.getValue(), Integer.MAX_VALUE)), 284, ry, 0xAACCAA);
            Integer items = itemsByTown.get(entry.getKey());
            this.font.draw(poseStack, items != null ? String.valueOf(items) : "-", 350, ry, 0x888888);
        }

        if (incomeScrollOffset > 0) this.font.draw(poseStack, "\u25B2", 370, startY, 0x6B5A3E);
        if (incomeScrollOffset < maxScroll) this.font.draw(poseStack, "\u25BC", 370, bottomY - rowH, 0x6B5A3E);
    }

    private void renderPriceBookLabels(PoseStack poseStack) {
        // Header
        drawCenteredString(poseStack, this.font, "\u00A7lPrice Book", 280, 21, 0xFFD700);

        TradingLedgerBlockEntity be = menu.getBlockEntity();
        if (be == null) return;

        ItemStack selectedStack = getSelectedItem();

        // Auto-populate price input when the selected item changes
        if (!ItemStack.matches(selectedStack, lastSelectedItem)) {
            lastSelectedItem = selectedStack.copy();
            if (!selectedStack.isEmpty()) {
                int price = be.getSetPrice(selectedSlot);
                if (price <= 0) price = be.getRememberedPrice(selectedStack);
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

    private void renderItemPriceInfo(PoseStack poseStack, TradingLedgerBlockEntity be, ItemStack stack) {
        // Item name (next to the icon rendered in render())
        String itemName = stack.getHoverName().getString();
        int maxNameW = 168;
        if (this.font.width(itemName) > maxNameW) {
            while (this.font.width(itemName + "..") > maxNameW && itemName.length() > 3) {
                itemName = itemName.substring(0, itemName.length() - 1);
            }
            itemName += "..";
        }
        this.font.draw(poseStack, itemName, 204, 37, 0xFFD700);

        // ---- Price breakdown (y=58 to y=86) ----
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

        int price = 0;
        if (!priceInput.getValue().isEmpty()) {
            try { price = Integer.parseInt(priceInput.getValue()); } catch (NumberFormatException ignored) {}
        }
        if (price <= 0) return;

        int effectivePrice = be.getEffectivePrice(stack, price);

        // ---- Results: price bar (y=120+) ----
        PriceCalculator.PriceRating rating = PriceCalculator.getPriceRating(
            effectivePrice, bd.materialCost(), bd.maxPrice());

        int barX = 184;
        int barY = 120;
        int barW = 190;
        int barH = 5;
        fill(poseStack, barX, barY, barX + barW, barY + barH, 0xFF1A1209);
        fill(poseStack, barX + 1, barY + 1, barX + barW - 1, barY + barH - 1, 0xFF2A2A2A);
        if (bd.maxPrice() > 0) {
            float ratio = Math.min(1.0f, (float) effectivePrice / bd.maxPrice());
            int fillW = Math.max(1, (int) ((barW - 2) * ratio));
            fill(poseStack, barX + 1, barY + 1, barX + 1 + fillW, barY + barH - 1,
                    rating.getColor() | 0xFF000000);
        }
        // Base value marker (white tick)
        if (bd.maxPrice() > 0 && bd.materialCost() > 0) {
            float baseFrac = Math.min(1.0f, (float) bd.materialCost() / bd.maxPrice());
            int baseMarkerX = barX + 1 + (int) ((barW - 2) * baseFrac);
            fill(poseStack, baseMarkerX, barY - 1, baseMarkerX + 1, barY + barH + 1, 0xAAFFFFFF);
        }

        this.font.draw(poseStack, rating.getLabel(), 184, 128, rating.getColor());
        this.font.draw(poseStack, "Final: " + formatCoinText(effectivePrice), 266, 128, 0xBEA876);

        String est = TradingLedgerBlockEntity.getEstimatedMarketTime(
            effectivePrice, bd.materialCost(), bd.maxPrice());
        this.font.draw(poseStack, "Sells: " + est, 184, 140, 0x888888);
    }

    private void renderEmptyHint(PoseStack poseStack) {
        drawCenteredString(poseStack, this.font, "Select an item from", 280, 58, 0x888888);
        drawCenteredString(poseStack, this.font, "the list to view and", 280, 70, 0x888888);
        drawCenteredString(poseStack, this.font, "set its price.", 280, 82, 0x888888);
    }

    // ==================== Mouse Handling ====================

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        // Right-click list row → open context menu for withdraw options
        if (button == 1) {
            int x = this.leftPos;
            int y = this.topPos;
            int listX = x + LIST_LEFT;
            int listY = y + LIST_TOP;
            int visCount = Math.min(MAX_VISIBLE, Math.max(0, filteredSlots.size() - listScrollOffset));
            for (int i = 0; i < visCount; i++) {
                int slot = filteredSlots.get(listScrollOffset + i);
                int rowY = listY + i * ROW_HEIGHT;
                if (mouseX >= listX && mouseX < listX + LIST_WIDTH
                        && mouseY >= rowY && mouseY < rowY + ROW_HEIGHT) {
                    TradingLedgerBlockEntity be = menu.getBlockEntity();
                    if (be != null && !be.getItem(slot).isEmpty()) {
                        contextMenuSlot = slot;
                        contextMenuX = (int) mouseX;
                        contextMenuY = (int) mouseY;
                        return true;
                    }
                }
            }
            // Click outside context menu → dismiss
            contextMenuSlot = -1;
        }

        // Dismiss context menu on any left-click (unless handled below)
        if (button == 0 && contextMenuSlot >= 0) {
            int cmX = contextMenuX;
            int cmY = contextMenuY;
            // Option 0: To Inventory  (y offset 0..10)
            if (mouseX >= cmX && mouseX < cmX + 80 && mouseY >= cmY && mouseY < cmY + 10) {
                TradingLedgerBlockEntity be = menu.getBlockEntity();
                if (be != null) {
                    ModNetwork.CHANNEL.send(PacketDistributor.SERVER.noArg(),
                            new com.offtomarket.mod.network.WithdrawBinItemPacket(
                                    be.getBlockPos(), contextMenuSlot, false));
                }
                contextMenuSlot = -1;
                return true;
            }
            // Option 1: To Container  (y offset 12..22)
            if (mouseX >= cmX && mouseX < cmX + 80 && mouseY >= cmY + 12 && mouseY < cmY + 22) {
                TradingLedgerBlockEntity be = menu.getBlockEntity();
                if (be != null) {
                    ModNetwork.CHANNEL.send(PacketDistributor.SERVER.noArg(),
                            new com.offtomarket.mod.network.WithdrawBinItemPacket(
                                    be.getBlockPos(), contextMenuSlot, true));
                }
                contextMenuSlot = -1;
                return true;
            }
            contextMenuSlot = -1;
        }

        // List row clicks (any tab — list is always visible on the left)
        if (button == 0) {
            int x = this.leftPos;
            int y = this.topPos;
            int listX = x + LIST_LEFT;
            int listY = y + LIST_TOP;

            int visibleCount = Math.min(MAX_VISIBLE, Math.max(0, filteredSlots.size() - listScrollOffset));
            for (int i = 0; i < visibleCount; i++) {
                int slot = filteredSlots.get(listScrollOffset + i);
                int rowY = listY + i * ROW_HEIGHT;

                if (mouseX >= listX && mouseX < listX + LIST_WIDTH
                        && mouseY >= rowY && mouseY < rowY + ROW_HEIGHT) {
                    TradingLedgerBlockEntity be = menu.getBlockEntity();
                    if (be != null && !be.getItem(slot).isEmpty()) {
                        if (selectedSlot == slot) {
                            // Toggle deselect
                            selectedSlot = -1;
                            lastSelectedItem = ItemStack.EMPTY;
                        } else {
                            selectedSlot = slot;
                            lastSelectedItem = ItemStack.EMPTY; // force price input update
                        }
                        updateTabVisibility();
                        return true;
                    }
                }
            }
        }

        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double delta) {
        int x = this.leftPos;
        int y = this.topPos;
        int listX = x + LIST_LEFT;
        int listY = y + LIST_TOP;
        int listBottom = listY + MAX_VISIBLE * ROW_HEIGHT;

        if (mouseX >= listX && mouseX < listX + LIST_WIDTH && mouseY >= listY && mouseY < listBottom) {
            int maxScroll = Math.max(0, filteredSlots.size() - MAX_VISIBLE);
            if (delta > 0 && listScrollOffset > 0) {
                listScrollOffset--;
                return true;
            }
            if (delta < 0 && listScrollOffset < maxScroll) {
                listScrollOffset++;
                return true;
            }
        }

        // History tab right-panel scroll
        if (activeTab == 2 && mouseX >= x + 183 && mouseX < x + 383) {
            TradingLedgerBlockEntity be = menu.getBlockEntity();
            if (be != null) {
                int histVisible = (220 - 40) / 12;
                int maxHistScroll = Math.max(0, be.getShipmentHistory().size() - histVisible);
                if (delta > 0 && historyScrollOffset > 0) { historyScrollOffset--; return true; }
                if (delta < 0 && historyScrollOffset < maxHistScroll) { historyScrollOffset++; return true; }
            }
        }

        // Income tab right-panel scroll
        if (activeTab == 3 && mouseX >= x + 183 && mouseX < x + 383) {
            TradingLedgerBlockEntity be = menu.getBlockEntity();
            if (be != null) {
                int incomeSY = 34 + 36;
                int incomeVisible = (220 - incomeSY) / 11;
                int maxIncomeScroll = Math.max(0, (int) be.getShipmentHistory().stream()
                        .map(r -> r.townDisplayName).distinct().count() - incomeVisible);
                if (delta > 0 && incomeScrollOffset > 0) { incomeScrollOffset--; return true; }
                if (delta < 0 && incomeScrollOffset < maxIncomeScroll) { incomeScrollOffset++; return true; }
            }
        }

        return super.mouseScrolled(mouseX, mouseY, delta);
    }

    // ==================== Keyboard Handling ====================

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        // Enter in price input → set price
        if (keyCode == 257 && priceInput.isFocused() && !priceInput.getValue().isEmpty()) {
            TradingLedgerBlockEntity be = menu.getBlockEntity();
            if (be != null && selectedSlot >= 0) {
                int price = Integer.parseInt(priceInput.getValue());
                ModNetwork.CHANNEL.send(PacketDistributor.SERVER.noArg(),
                        new SetPricePacket(be.getBlockPos(), selectedSlot, price));
            }
            return true;
        }

        // Enter in fee inputs → apply settings and unfocus
        if (keyCode == 257 && (taxInput.isFocused() || markupInput.isFocused())) {
            sendSettingsToServer();
            taxInput.setFocus(false);
            markupInput.setFocus(false);
            return true;
        }

        // Prevent inventory close / intercept keys while typing in any EditBox
        if (searchInput.isFocused() || priceInput.isFocused()
                || taxInput.isFocused() || markupInput.isFocused()) {
            if (keyCode == 256) { // Escape: unfocus all inputs
                searchInput.setFocus(false);
                priceInput.setFocus(false);
                taxInput.setFocus(false);
                markupInput.setFocus(false);
                return true;
            }
            // Delegate to focused widget and consume the event
            if (searchInput.isFocused()) return searchInput.keyPressed(keyCode, scanCode, modifiers) || true;
            if (priceInput.isFocused()) return priceInput.keyPressed(keyCode, scanCode, modifiers) || true;
            if (taxInput.isFocused()) return taxInput.keyPressed(keyCode, scanCode, modifiers) || true;
            if (markupInput.isFocused()) return markupInput.keyPressed(keyCode, scanCode, modifiers) || true;
        }

        return super.keyPressed(keyCode, scanCode, modifiers);
    }

    // ==================== Render Overlays ====================

    @Override
    public void render(PoseStack poseStack, int mouseX, int mouseY, float partialTick) {
        renderBackground(poseStack);
        super.render(poseStack, mouseX, mouseY, partialTick);

        // Render item icons in the list (after bg, on top of row backgrounds)
        TradingLedgerBlockEntity be = menu.getBlockEntity();
        if (be != null) {
            int x = this.leftPos;
            int y = this.topPos;

            int visibleCount = Math.min(MAX_VISIBLE, Math.max(0, filteredSlots.size() - listScrollOffset));
            for (int i = 0; i < visibleCount; i++) {
                int slot = filteredSlots.get(listScrollOffset + i);
                ItemStack stack = be.getItem(slot);
                if (stack.isEmpty()) continue;

                int iconX = x + LIST_LEFT + 1;
                int iconY = y + LIST_TOP + i * ROW_HEIGHT + 1;

                // Render 12x12 item icon (scaled from 16x16)
                RenderSystem.getModelViewStack().pushPose();
                RenderSystem.getModelViewStack().translate(iconX, iconY, 100);
                RenderSystem.getModelViewStack().scale(0.75f, 0.75f, 1.0f);
                RenderSystem.applyModelViewMatrix();
                this.itemRenderer.renderAndDecorateItem(stack, 0, 0);
                this.itemRenderer.renderGuiItemDecorations(this.font, stack, 0, 0, null);
                RenderSystem.getModelViewStack().popPose();
                RenderSystem.applyModelViewMatrix();
            }

            // Render selected item icon in right panel (bin tab, next to item name)
            if (activeTab == 0) {
                ItemStack selected = getSelectedItem();
                if (!selected.isEmpty()) {
                    this.itemRenderer.renderAndDecorateItem(selected, x + 184, y + 34);
                }
            }
        }

        // Tooltip handling: list item hover takes priority
        boolean showedListTooltip = false;
        if (be != null) {
            int visibleCount = Math.min(MAX_VISIBLE, Math.max(0, filteredSlots.size() - listScrollOffset));
            for (int i = 0; i < visibleCount; i++) {
                int slot = filteredSlots.get(listScrollOffset + i);
                int rowY = this.topPos + LIST_TOP + i * ROW_HEIGHT;

                if (mouseX >= this.leftPos + LIST_LEFT
                        && mouseX < this.leftPos + LIST_LEFT + LIST_WIDTH
                        && mouseY >= rowY && mouseY < rowY + ROW_HEIGHT) {
                    ItemStack stack = be.getItem(slot);
                    if (!stack.isEmpty()) {
                        this.renderTooltip(poseStack, stack, mouseX, mouseY);
                        showedListTooltip = true;
                    }
                    break;
                }
            }
        }

        if (!showedListTooltip) {
            this.renderTooltip(poseStack, mouseX, mouseY);
        }

        // Draw right-click context menu overlay
        if (contextMenuSlot >= 0 && be != null) {
            int cmX = contextMenuX;
            int cmY = contextMenuY;
            boolean isVirtual = be.isVirtualSlot(contextMenuSlot);
            String opt1Label = isVirtual ? "Remove from Ledger" : "To Container";
            // Panel background
            fill(poseStack, cmX - 1, cmY - 1, cmX + 82, cmY + 24, 0xFF000000);
            fill(poseStack, cmX, cmY, cmX + 81, cmY + 23, 0xFF2A1E0E);
            // Hover row highlights
            boolean hoverInv  = mouseX >= cmX && mouseX < cmX + 81 && mouseY >= cmY       && mouseY < cmY + 11;
            boolean hoverCont = mouseX >= cmX && mouseX < cmX + 81 && mouseY >= cmY + 12  && mouseY < cmY + 23;
            if (hoverInv)  fill(poseStack, cmX, cmY,      cmX + 81, cmY + 11, 0xFF3D2E18);
            if (hoverCont) fill(poseStack, cmX, cmY + 12, cmX + 81, cmY + 23, 0xFF3D2E18);
            // Divider
            fill(poseStack, cmX + 1, cmY + 11, cmX + 80, cmY + 12, 0xFF4A3820);
            // Option labels (gold on hover)
            this.font.draw(poseStack, "To Inventory", cmX + 3, cmY + 2,  hoverInv  ? 0xFFD700 : 0xFFFFFF);
            this.font.draw(poseStack, opt1Label,       cmX + 3, cmY + 14, hoverCont ? 0xFFD700 : 0xAAAAAA);
        }
    }

    // ==================== Utility Methods ====================

    /**
     * Format copper pieces as a coin string with color codes (e.g., "§e1g§r §72s§r §63c§r").
     */
    private static String formatCoinText(int copper) {
        int g = copper / 100;
        int s = (copper % 100) / 10;
        int c = copper % 10;

        StringBuilder sb = new StringBuilder();
        if (g > 0) sb.append("\u00A7e").append(g).append("g\u00A7r ");
        if (s > 0) sb.append("\u00A77").append(s).append("s\u00A7r ");
        if (c > 0 || sb.length() == 0) sb.append("\u00A76").append(c).append("c\u00A7r");
        return sb.toString().trim();
    }

    /**
     * Draw a custom checkbox with label, styled for the dark panel.
     */
}
