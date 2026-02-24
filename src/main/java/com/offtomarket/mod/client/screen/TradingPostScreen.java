package com.offtomarket.mod.client.screen;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;
import com.offtomarket.mod.OffToMarket;
import com.offtomarket.mod.block.entity.TradingPostBlockEntity;
import com.offtomarket.mod.data.*;
import com.offtomarket.mod.debug.DebugConfig;
import com.offtomarket.mod.menu.TradingPostMenu;
import com.offtomarket.mod.network.*;
import com.offtomarket.mod.util.SoundHelper;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.network.PacketDistributor;
import net.minecraftforge.registries.ForgeRegistries;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public class TradingPostScreen extends AbstractContainerScreen<TradingPostMenu> {
    private static final ResourceLocation TEXTURE =
            new ResourceLocation(OffToMarket.MODID, "textures/gui/trading_post.png");

    // ==================== Tab System ====================

    private enum Tab { TRADE, ACTIVITY, TOWNS, QUESTS, WORKERS, DIPLOMAT }
    private Tab currentTab = Tab.TRADE;
    private static final String[] TAB_NAMES = {"Trade", "Activity", "Towns", "Quests", "Workers", "Requests"};

    // Tab layout (6 tabs across 248px — 40px each with 1px gap)
    private static final int TAB_Y = 18;
    private static final int TAB_H = 14;
    private static final int TAB_W = 40;
    private static final int TAB_COUNT = 6;
    private static final int[] TAB_X = {8, 49, 90, 131, 172, 213};

    private static final int CONTENT_TOP = 32;
    private static final int CONTENT_H = 112;

    // ==================== State ====================

    private int selectedTownIndex = 0;
    private int activityScrollOffset = 0;
    private boolean showingEconomyDashboard = false;
    private int townViewPage = 0;
    private int townContentScroll = 0; // scroll offset for town needs/surplus content
    private int questScrollOffset = 0;
    private int diplomatScrollOffset = 0;
    private static final int VISIBLE_ACTIVITY = 7;
    private static final int VISIBLE_QUESTS = 6;
    private static final int VISIBLE_DIPLOMAT = 6;

    // Hover tracking
    private int hoveredActivityRow = -1;
    private int hoveredQuestRow = -1;
    private int hoveredDiplomatRow = -1;

    // Diplomat selection from Towns tab
    private boolean selectingDiplomatItem = false;
    private int hoveredSurplusRow = -1;
    private List<net.minecraft.resources.ResourceLocation> currentSurplusList = new ArrayList<>();
    private int diplomatQuantity = 1; // quantity for diplomat requests

    // Shipment cancel confirmation
    private UUID confirmCancelShipmentId = null; // shipment ID awaiting cancel confirmation

    // ==================== Buttons ====================

    // Trade tab
    private Button prevTownBtn, nextTownBtn, sendBtn, collectBtn;
    // Activity tab (shipments + orders)
    private Button activityScrollUpBtn, activityScrollDownBtn;
    private Button economyToggleBtn;
    // Towns tab
    private Button prevTownPageBtn, nextTownPageBtn;
    private Button sendDiplomatBtn, cancelDiplomatBtn;
    // Quests tab
    private Button questScrollUpBtn, questScrollDownBtn;
    // Workers tab
    private Button hireNegotiatorBtn, hireCartBtn;
    // Diplomat tab
    private Button diplomatScrollUpBtn, diplomatScrollDownBtn;

    public TradingPostScreen(TradingPostMenu menu, Inventory inv, Component title) {
        super(menu, inv, title);
        this.imageWidth = 256;
        this.imageHeight = 230;
    }

    @Override
    protected void init() {
        super.init();
        int x = this.leftPos;
        int y = this.topPos;

        // ===== Bug fix: sync selectedTownIndex from block entity =====
        TradingPostBlockEntity be = menu.getBlockEntity();
        if (be != null) {
            String selectedId = be.getSelectedTownId();
            List<TownData> towns = getAvailableTowns();
            selectedTownIndex = 0;
            for (int i = 0; i < towns.size(); i++) {
                if (towns.get(i).getId().equals(selectedId)) {
                    selectedTownIndex = i;
                    break;
                }
            }
        }

        // ==== Trade tab buttons ====

        prevTownBtn = addRenderableWidget(new Button(x + 7, y + 38, 14, 14,
                Component.literal("<"), btn -> {
            List<TownData> towns = getAvailableTowns();
            if (!towns.isEmpty()) {
                selectedTownIndex = (selectedTownIndex - 1 + towns.size()) % towns.size();
                selectTown(towns.get(selectedTownIndex));
            }
        }));

        nextTownBtn = addRenderableWidget(new Button(x + 235, y + 38, 14, 14,
                Component.literal(">"), btn -> {
            List<TownData> towns = getAvailableTowns();
            if (!towns.isEmpty()) {
                selectedTownIndex = (selectedTownIndex + 1) % towns.size();
                selectTown(towns.get(selectedTownIndex));
            }
        }));

        sendBtn = addRenderableWidget(new Button(x + 8, y + 80, 120, 16,
                Component.literal("Send to Market"), btn -> {
            TradingPostBlockEntity tbe = menu.getBlockEntity();
            if (tbe != null) {
                ModNetwork.CHANNEL.send(PacketDistributor.SERVER.noArg(),
                        new SendShipmentPacket(tbe.getBlockPos()));
            }
        }));

        collectBtn = addRenderableWidget(new Button(x + 130, y + 80, 118, 16,
                Component.literal("Collect Coins"), btn -> {
            TradingPostBlockEntity tbe = menu.getBlockEntity();
            if (tbe != null) {
                ModNetwork.CHANNEL.send(PacketDistributor.SERVER.noArg(),
                        new CollectCoinsPacket(tbe.getBlockPos()));
            }
        }));

        // ==== Activity tab buttons (unified shipments + orders) ====

        activityScrollUpBtn = addRenderableWidget(new Button(x + 236, y + 47, 14, 14,
                Component.literal("\u25B2"), btn -> {
            if (activityScrollOffset > 0) activityScrollOffset--;
        }));

        activityScrollDownBtn = addRenderableWidget(new Button(x + 236, y + 127, 14, 14,
                Component.literal("\u25BC"), btn -> activityScrollOffset++));

        economyToggleBtn = addRenderableWidget(new Button(x + 175, y + 33, 60, 12,
                Component.literal("\uD83D\uDCCA Stats"), btn -> {
            showingEconomyDashboard = !showingEconomyDashboard;
            btn.setMessage(Component.literal(showingEconomyDashboard ? "\u25C0 Activity" : "\uD83D\uDCCA Stats"));
            updateButtonVisibility();
        }));

        // ==== Towns tab buttons ====

        prevTownPageBtn = addRenderableWidget(new Button(x + 8, y + 128, 55, 14,
                Component.literal("\u25C0 Prev"), btn -> {
            if (townViewPage > 0) { townViewPage--; townContentScroll = 0; }
        }));

        nextTownPageBtn = addRenderableWidget(new Button(x + 68, y + 128, 55, 14,
                Component.literal("Next \u25B6"), btn -> {
            Collection<TownData> allTowns = TownRegistry.getAllTowns();
            if (townViewPage < allTowns.size() - 1) { townViewPage++; townContentScroll = 0; }
        }));

        // Send Request button — appears on Towns tab left page, below buttons
        sendDiplomatBtn = addRenderableWidget(new Button(x + 8, y + 112, 115, 14,
                Component.literal("\u270D Request Item"), btn -> {
            selectingDiplomatItem = true;
            hoveredSurplusRow = -1;
            townContentScroll = 0;
            diplomatQuantity = 1; // reset quantity when starting selection
            // Build the surplus list for the currently viewed town
            List<TownData> allTowns = new ArrayList<>(TownRegistry.getAllTowns());
            if (townViewPage < allTowns.size()) {
                currentSurplusList = new ArrayList<>(allTowns.get(townViewPage).getSurplus());
            } else {
                currentSurplusList = new ArrayList<>();
            }
            updateButtonVisibility();
        }));

        // Cancel diplomat selection
        cancelDiplomatBtn = addRenderableWidget(new Button(x + 8, y + 128, 80, 14,
                Component.literal("\u2718 Cancel"), btn -> {
            selectingDiplomatItem = false;
            hoveredSurplusRow = -1;
            updateButtonVisibility();
        }));

        // ==== Quests tab buttons ====

        questScrollUpBtn = addRenderableWidget(new Button(x + 236, y + 47, 14, 14,
                Component.literal("\u25B2"), btn -> {
            if (questScrollOffset > 0) questScrollOffset--;
        }));

        questScrollDownBtn = addRenderableWidget(new Button(x + 236, y + 127, 14, 14,
                Component.literal("\u25BC"), btn -> questScrollOffset++));

        // ==== Workers tab buttons ====

        hireNegotiatorBtn = addRenderableWidget(new Button(x + 8, y + 82, 112, 14,
                Component.literal("Hire Negotiator"), btn -> {
            TradingPostBlockEntity tbe = menu.getBlockEntity();
            if (tbe != null) {
                ModNetwork.CHANNEL.send(PacketDistributor.SERVER.noArg(),
                        new HireWorkerPacket(tbe.getBlockPos(),
                                com.offtomarket.mod.data.Worker.WorkerType.NEGOTIATOR));
            }
        }));

        hireCartBtn = addRenderableWidget(new Button(x + 130, y + 82, 118, 14,
                Component.literal("Hire Trading Cart"), btn -> {
            TradingPostBlockEntity tbe = menu.getBlockEntity();
            if (tbe != null) {
                ModNetwork.CHANNEL.send(PacketDistributor.SERVER.noArg(),
                        new HireWorkerPacket(tbe.getBlockPos(),
                                com.offtomarket.mod.data.Worker.WorkerType.TRADING_CART));
            }
        }));

        // ==== Diplomat tab buttons ====

        diplomatScrollUpBtn = addRenderableWidget(new Button(x + 236, y + 47, 14, 14,
                Component.literal("\u25B2"), btn -> {
            if (diplomatScrollOffset > 0) diplomatScrollOffset--;
        }));

        diplomatScrollDownBtn = addRenderableWidget(new Button(x + 236, y + 127, 14, 14,
                Component.literal("\u25BC"), btn -> diplomatScrollOffset++));

        updateButtonVisibility();
    }

    private void switchTab(Tab tab) {
        if (currentTab != tab) {
            SoundHelper.playTabSwitch();
        }
        currentTab = tab;
        selectingDiplomatItem = false;
        hoveredSurplusRow = -1;
        showingEconomyDashboard = false;
        if (economyToggleBtn != null) {
            economyToggleBtn.setMessage(Component.literal("\uD83D\uDCCA Stats"));
        }
        updateButtonVisibility();
    }

    private void updateButtonVisibility() {
        boolean trade = currentTab == Tab.TRADE;
        boolean activity = currentTab == Tab.ACTIVITY;
        boolean towns = currentTab == Tab.TOWNS;
        boolean quests = currentTab == Tab.QUESTS;
        boolean workers = currentTab == Tab.WORKERS;
        boolean diplomat = currentTab == Tab.DIPLOMAT;

        prevTownBtn.visible = trade;
        nextTownBtn.visible = trade;
        sendBtn.visible = trade;
        collectBtn.visible = trade;

        activityScrollUpBtn.visible = activity && !showingEconomyDashboard;
        activityScrollDownBtn.visible = activity && !showingEconomyDashboard;
        economyToggleBtn.visible = activity;

        // Hide coin exchange slots (no longer have a coins tab)
        updateCoinSlotPositions(false);

        prevTownPageBtn.visible = towns && !selectingDiplomatItem;
        nextTownPageBtn.visible = towns && !selectingDiplomatItem;

        // Send Diplomat button: visible on Towns tab (not already selecting), when town is unlocked
        boolean townUnlocked = false;
        if (towns) {
            List<TownData> allTowns = new ArrayList<>(TownRegistry.getAllTowns());
            if (townViewPage < allTowns.size()) {
                townUnlocked = allTowns.get(townViewPage).getMinTraderLevel() <= menu.getTraderLevel();
            }
        }
        sendDiplomatBtn.visible = towns && !selectingDiplomatItem && townUnlocked;
        cancelDiplomatBtn.visible = towns && selectingDiplomatItem;

        questScrollUpBtn.visible = quests;
        questScrollDownBtn.visible = quests;

        TradingPostBlockEntity be = menu.getBlockEntity();
        hireNegotiatorBtn.visible = workers && (be == null || !be.getNegotiator().isHired());
        hireCartBtn.visible = workers && (be == null || !be.getTradingCart().isHired());

        diplomatScrollUpBtn.visible = diplomat;
        diplomatScrollDownBtn.visible = diplomat;
    }

    private List<TownData> getAvailableTowns() {
        int level = menu.getTraderLevel();
        return TownRegistry.getAvailableTowns(Math.max(1, level));
    }

    private void selectTown(TownData town) {
        TradingPostBlockEntity be = menu.getBlockEntity();
        if (be != null) {
            ModNetwork.CHANNEL.send(PacketDistributor.SERVER.noArg(),
                    new SelectTownPacket(be.getBlockPos(), town.getId()));
        }
    }

    // ==================== Drawing Helpers ====================

    private void drawPanel(PoseStack ps, int x, int y, int w, int h) {
        fill(ps, x, y, x + w, y + h, 0xFF1A1209);
        fill(ps, x + 1, y + 1, x + w - 1, y + 2, 0xFF8B7355);
        fill(ps, x + 1, y + 1, x + 2, y + h - 1, 0xFF8B7355);
        fill(ps, x + 1, y + h - 2, x + w - 1, y + h - 1, 0xFF2A1F14);
        fill(ps, x + w - 2, y + 1, x + w - 1, y + h - 1, 0xFF2A1F14);
        fill(ps, x + 2, y + 2, x + w - 2, y + h - 2, 0xFF5C4A32);
    }

    private void drawInsetPanel(PoseStack ps, int x, int y, int w, int h) {
        fill(ps, x, y, x + w, y + h, 0xFF2A1F14);
        fill(ps, x + 1, y + 1, x + w - 1, y + h - 1, 0xFF3E3226);
    }

    private void drawSlot(PoseStack ps, int x, int y) {
        fill(ps, x - 1, y - 1, x + 17, y + 17, 0xFF373737);
        fill(ps, x, y, x + 16, y + 16, 0xFF8B8B8B);
    }

    // ==================== Render Background ====================

    @Override
    protected void renderBg(PoseStack poseStack, float partialTick, int mouseX, int mouseY) {
        RenderSystem.setShader(GameRenderer::getPositionTexShader);
        RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
        RenderSystem.setShaderTexture(0, TEXTURE);

        int x = this.leftPos;
        int y = this.topPos;

        // Main background panel
        drawPanel(poseStack, x, y, 256, 230);

        // Title bar
        drawInsetPanel(poseStack, x + 4, y + 3, 248, 14);

        // Content area
        drawInsetPanel(poseStack, x + 4, y + CONTENT_TOP, 248, CONTENT_H);

        // Tab bar (8 tabs)
        for (int i = 0; i < TAB_COUNT; i++) {
            boolean selected = currentTab.ordinal() == i;
            int tx = x + TAB_X[i];
            int ty = y + TAB_Y;

            if (selected) {
                fill(poseStack, tx, ty, tx + TAB_W, ty + TAB_H + 1, 0xFF8B7355);
                fill(poseStack, tx + 1, ty + 1, tx + TAB_W - 1, ty + TAB_H + 1, 0xFF3E3226);
            } else {
                fill(poseStack, tx, ty + 2, tx + TAB_W, ty + TAB_H, 0xFF1A1209);
                fill(poseStack, tx + 1, ty + 3, tx + TAB_W - 1, ty + TAB_H - 1, 0xFF2A1F14);
            }
        }

        // Per-tab content backgrounds
        switch (currentTab) {
            case TRADE -> renderTradeBg(poseStack, x, y, mouseX, mouseY);
            case ACTIVITY -> renderActivityBg(poseStack, x, y, mouseX, mouseY);
            case TOWNS -> {
                renderTownsBg(poseStack, x, y);
                updateTownsDiplomatHover(x, y, mouseX, mouseY);
            }
            case QUESTS -> renderQuestsBg(poseStack, x, y, mouseX, mouseY);
            case WORKERS -> renderWorkersBg(poseStack, x, y);
            case DIPLOMAT -> renderDiplomatBg(poseStack, x, y, mouseX, mouseY);
        }

        // Divider above inventory
        fill(poseStack, x + 4, y + 145, x + 252, y + 146, 0xFF3B2E1E);

        // Player inventory slot backgrounds
        for (int row = 0; row < 3; row++) {
            for (int col = 0; col < 9; col++) {
                drawSlot(poseStack, x + 48 + col * 18, y + 148 + row * 18);
            }
        }
        for (int col = 0; col < 9; col++) {
            drawSlot(poseStack, x + 48 + col * 18, y + 206);
        }
    }

    private void renderTradeBg(PoseStack ps, int x, int y, int mouseX, int mouseY) {
        // Town selector sub-panel
        drawInsetPanel(ps, x + 6, y + 35, 244, 22);
        // Stats sub-panel
        drawInsetPanel(ps, x + 6, y + 60, 244, 14);

        // XP progress bar
        int level = menu.getTraderLevel();
        int xp = menu.getTraderXp();
        int xpNeeded = DebugConfig.getBaseXpToLevel() * Math.max(1, level);
        float xpFraction = Math.min(1.0f, xp / (float) xpNeeded);
        int barX = x + 74;
        int barY = y + 63;
        int barW = 80;
        fill(ps, barX - 1, barY - 1, barX + barW + 1, barY + 9, 0xFF1A1209);
        fill(ps, barX, barY, barX + barW, barY + 8, 0xFF2A2A2A);
        if (xpFraction > 0) {
            int fillW = Math.max(1, (int) (barW * xpFraction));
            fill(ps, barX, barY, barX + fillW, barY + 8, 0xFF55CC55);
        }
    }

    // ==================== Activity Tab (Unified Shipments + Orders) ====================

    /** Tracks what type each visible activity row is: true = shipment, false = order */
    private boolean[] activityRowIsShipment = new boolean[VISIBLE_ACTIVITY];
    /** Maps visible row index to underlying data index */
    private int[] activityRowDataIndex = new int[VISIBLE_ACTIVITY];

    private void renderActivityBg(PoseStack ps, int x, int y, int mouseX, int mouseY) {
        if (showingEconomyDashboard) {
            return; // Dashboard draws its own content
        }

        // Header row background
        fill(ps, x + 5, y + 46, x + 233, y + 56, 0xFF2C2318);

        // Activity rows with hover
        hoveredActivityRow = -1;
        TradingPostBlockEntity be = menu.getBlockEntity();
        int totalShipments = be != null ? be.getActiveShipments().size() : 0;
        int totalOrders = be != null ? be.getActiveBuyOrders().size() : 0;
        int totalActivity = totalShipments + totalOrders;

        for (int i = 0; i < VISIBLE_ACTIVITY; i++) {
            int rowY = y + 57 + i * 12;
            int actIdx = activityScrollOffset + i;
            boolean validRow = actIdx < totalActivity;

            boolean isHovered = false;
            if (validRow && mouseX >= x + 5 && mouseX <= x + 233
                    && mouseY >= rowY && mouseY < rowY + 12) {
                hoveredActivityRow = i;
                isHovered = true;
            }

            if (isHovered) {
                fill(ps, x + 5, rowY, x + 233, rowY + 12, 0xFF5A4A30);
                fill(ps, x + 5, rowY, x + 6, rowY + 12, 0xFFFFD700);
            } else {
                int rowColor = (i % 2 == 0) ? 0xFF4A3D2B : 0xFF3E3226;
                fill(ps, x + 5, rowY, x + 233, rowY + 12, rowColor);
            }
        }
    }

    private void renderTownsBg(PoseStack ps, int x, int y) {
        // Book-style page panel
        // Left page (parchment)
        fill(ps, x + 6, y + 35, x + 126, y + 140, 0xFFD8C8A0);
        fill(ps, x + 7, y + 36, x + 125, y + 139, 0xFFF0E6C8);
        // Center spine
        fill(ps, x + 126, y + 35, x + 130, y + 140, 0xFF6B5A3E);
        // Right page (parchment)
        fill(ps, x + 130, y + 35, x + 250, y + 140, 0xFFD8C8A0);
        fill(ps, x + 131, y + 36, x + 249, y + 139, 0xFFF0E6C8);
    }

    /**
     * Compute hover row for diplomat item selection on the Towns tab right page.
     * Called from renderBg to update hoveredSurplusRow.
     */
    private void updateTownsDiplomatHover(int x, int y, int mouseX, int mouseY) {
        hoveredSurplusRow = -1;
        if (!selectingDiplomatItem || currentSurplusList.isEmpty()) return;

        int rightPageLeft = x + 131;
        int rightPageRight = x + 249;
        int firstRowY = y + 72; // drawY=72 in local coords
        int rowHeight = 9;
        int visibleItems = 7;
        int itemCount = Math.min(visibleItems, currentSurplusList.size() - townContentScroll);

        if (mouseX >= rightPageLeft && mouseX <= rightPageRight
                && mouseY >= firstRowY && mouseY < firstRowY + itemCount * rowHeight) {
            hoveredSurplusRow = (mouseY - firstRowY) / rowHeight;
        }
    }

    private void renderQuestsBg(PoseStack ps, int x, int y, int mouseX, int mouseY) {
        // Header row
        fill(ps, x + 5, y + 46, x + 233, y + 56, 0xFF2C2318);

        // Quest rows with hover
        hoveredQuestRow = -1;
        TradingPostBlockEntity be = menu.getBlockEntity();
        int totalQuests = be != null ? be.getActiveQuests().size() : 0;

        for (int i = 0; i < VISIBLE_QUESTS; i++) {
            int rowY = y + 57 + i * 14;
            int qIdx = questScrollOffset + i;
            boolean validRow = qIdx < totalQuests;

            boolean isHovered = false;
            if (validRow && mouseX >= x + 5 && mouseX <= x + 233
                    && mouseY >= rowY && mouseY < rowY + 14) {
                hoveredQuestRow = i;
                isHovered = true;
            }

            if (isHovered) {
                fill(ps, x + 5, rowY, x + 233, rowY + 14, 0xFF5A4A30);
                fill(ps, x + 5, rowY, x + 6, rowY + 14, 0xFFFFD700);
            } else {
                int rowColor = (i % 2 == 0) ? 0xFF4A3D2B : 0xFF3E3226;
                fill(ps, x + 5, rowY, x + 233, rowY + 14, rowColor);
            }
        }
    }

    private void renderWorkersBg(PoseStack ps, int x, int y) {
        // Two side-by-side worker panels
        drawInsetPanel(ps, x + 6, y + 35, 118, 106);  // Left: Negotiator
        drawInsetPanel(ps, x + 132, y + 35, 118, 106); // Right: Trading Cart
    }

    private void renderDiplomatBg(PoseStack ps, int x, int y, int mouseX, int mouseY) {
        // Header row
        fill(ps, x + 5, y + 46, x + 233, y + 56, 0xFF2C2318);

        // Diplomat request rows with hover
        hoveredDiplomatRow = -1;
        TradingPostBlockEntity be = menu.getBlockEntity();
        int totalReqs = be != null ? be.getActiveDiplomatRequests().size() : 0;

        for (int i = 0; i < VISIBLE_DIPLOMAT; i++) {
            int rowY = y + 57 + i * 14;
            int dIdx = diplomatScrollOffset + i;
            boolean validRow = dIdx < totalReqs;

            boolean isHovered = false;
            if (validRow && mouseX >= x + 5 && mouseX <= x + 233
                    && mouseY >= rowY && mouseY < rowY + 14) {
                hoveredDiplomatRow = i;
                isHovered = true;
            }

            if (isHovered) {
                fill(ps, x + 5, rowY, x + 233, rowY + 14, 0xFF5A4A30);
                fill(ps, x + 5, rowY, x + 6, rowY + 14, 0xFFFFD700);
            } else {
                int rowColor = (i % 2 == 0) ? 0xFF4A3D2B : 0xFF3E3226;
                fill(ps, x + 5, rowY, x + 233, rowY + 14, rowColor);
            }
        }
    }

    // ==================== Render Labels ====================

    @Override
    protected void renderLabels(PoseStack poseStack, int mouseX, int mouseY) {
        // Title
        drawCenteredString(poseStack, this.font, "Trading Post", 128, 6, 0xFFD700);

        // Tab labels (8 tabs)
        for (int i = 0; i < TAB_COUNT; i++) {
            boolean selected = currentTab.ordinal() == i;
            int color = selected ? 0xFFD700 : 0x888888;
            int textY = selected ? TAB_Y + 3 : TAB_Y + 4;
            drawCenteredString(poseStack, this.font, TAB_NAMES[i],
                    TAB_X[i] + TAB_W / 2, textY, color);
        }

        // Per-tab content
        switch (currentTab) {
            case TRADE -> renderTradeLabels(poseStack);
            case ACTIVITY -> renderActivityLabels(poseStack);
            case TOWNS -> renderTownsLabels(poseStack);
            case QUESTS -> renderQuestsLabels(poseStack);
            case WORKERS -> renderWorkersLabels(poseStack);
            case DIPLOMAT -> renderDiplomatLabels(poseStack);
        }

        // Inventory label
        this.font.draw(poseStack, this.playerInventoryTitle, 48, 138, 0x404040);
    }

    // ==================== Trade Tab ====================

    private void renderTradeLabels(PoseStack poseStack) {
        List<TownData> towns = getAvailableTowns();
        if (!towns.isEmpty() && selectedTownIndex < towns.size()) {
            TownData town = towns.get(selectedTownIndex);

            // Town name centered
            String townName = town.getDisplayName();
            int nameW = this.font.width(townName);
            this.font.draw(poseStack, townName, (256 - nameW) / 2.0f, 38, 0xFFFFFF);

            // Type + distance
            String typeStr = town.getType().getDisplayName() + "  |  Dist: " + town.getDistance();
            int typeW = this.font.width(typeStr);
            this.font.draw(poseStack, typeStr, (256 - typeW) / 2.0f, 48, 0xAAAAAA);
        } else {
            drawCenteredString(poseStack, this.font, "No towns available", 128, 42, 0x888888);
        }

        // Stats row
        int level = menu.getTraderLevel();
        int xp = menu.getTraderXp();
        int xpNeeded = DebugConfig.getBaseXpToLevel() * Math.max(1, level);
        this.font.draw(poseStack, "Lvl " + level, 10, 63, 0xFFD700);
        String xpText = xp + " / " + xpNeeded;
        int xpW = this.font.width(xpText);
        this.font.draw(poseStack, xpText, 74 + (80 - xpW) / 2.0f, 63, 0xCCCCCC);

        int coins = menu.getPendingCoins();
        int coinW = CoinRenderer.getCoinValueWidth(this.font, coins);
        int labelW = this.font.width("Coins: ");
        this.font.draw(poseStack, "Coins: ", 246 - coinW - labelW, 63, 0xFFD700);
        CoinRenderer.renderCoinValue(poseStack, this.font, 246 - coinW, 63, coins);

        // Shipment summary on Trade tab
        TradingPostBlockEntity be = menu.getBlockEntity();
        if (be != null) {
            int activeCount = be.getActiveShipments().size();
            this.font.draw(poseStack, "Shipments: " + activeCount + " active", 8, 102, 0xAAAAAA);
            if (activeCount > 0) {
                this.font.draw(poseStack, "(See Ships tab for details)", 8, 114, 0x666666);
            }
        }
    }

    // ==================== Activity Tab (Unified Shipments + Orders) ====================

    private void renderActivityLabels(PoseStack poseStack) {
        if (showingEconomyDashboard) {
            renderEconomyDashboard(poseStack);
            return;
        }

        this.font.draw(poseStack, "Activity", 8, 36, 0xFFD700);

        TradingPostBlockEntity be = menu.getBlockEntity();
        if (be == null) return;

        List<Shipment> shipments = be.getActiveShipments();
        List<BuyOrder> orders = be.getActiveBuyOrders();
        int totalActivity = shipments.size() + orders.size();

        if (totalActivity == 0) {
            this.font.draw(poseStack, "No active shipments or orders.", 10, 60, 0x888888);
            this.font.draw(poseStack, "Send items from Trade tab, or", 10, 74, 0x888888);
            this.font.draw(poseStack, "order from a Market Board.", 10, 88, 0x888888);
            return;
        }

        int maxScroll = Math.max(0, totalActivity - VISIBLE_ACTIVITY);
        activityScrollOffset = Math.min(activityScrollOffset, maxScroll);

        // Count indicator
        if (totalActivity > VISIBLE_ACTIVITY) {
            String scrollInfo = (activityScrollOffset + 1) + "-" +
                    Math.min(activityScrollOffset + VISIBLE_ACTIVITY, totalActivity) +
                    " of " + totalActivity;
            int scrollW = this.font.width(scrollInfo);
            this.font.draw(poseStack, scrollInfo, 230 - scrollW, 36, 0x666666);
        }

        // Column headers
        this.font.draw(poseStack, "Type", 8, 48, 0xFFD700);
        this.font.draw(poseStack, "Details", 40, 48, 0xFFD700);
        this.font.draw(poseStack, "Status", 170, 48, 0xFFD700);

        int yOff = 58;
        int displayed = 0;

        // Combined iteration: shipments first, then orders
        for (int i = activityScrollOffset; displayed < VISIBLE_ACTIVITY && i < totalActivity; i++, displayed++) {
            boolean isShipment = i < shipments.size();
            activityRowIsShipment[displayed] = isShipment;

            if (isShipment) {
                int shipIdx = i;
                activityRowDataIndex[displayed] = shipIdx;
                Shipment s = shipments.get(shipIdx);
                renderActivityShipmentRow(poseStack, s, be, yOff);
            } else {
                int orderIdx = i - shipments.size();
                activityRowDataIndex[displayed] = orderIdx;
                BuyOrder order = orders.get(orderIdx);
                renderActivityOrderRow(poseStack, order, be, yOff);
            }

            yOff += 12;
        }
    }

    private void renderActivityShipmentRow(PoseStack ps, Shipment s, TradingPostBlockEntity be, int yOff) {
        // Check if this shipment is pending cancel confirmation
        boolean isConfirmingCancel = s.getId().equals(confirmCancelShipmentId);

        // Type indicator: "OUT" for outgoing shipments
        this.font.draw(ps, "\u2191OUT", 8, yOff, 0xFFAA44);

        TownData town = TownRegistry.getTown(s.getTownId());
        String dest = town != null ? town.getDisplayName() : s.getTownId();

        String status;
        int statusColor;
        
        if (isConfirmingCancel) {
            // Show cancel confirmation prompt
            status = "\u2718 Click to CANCEL";
            statusColor = 0xFF5555;
        } else {
            switch (s.getStatus()) {
                case IN_TRANSIT -> {
                    long ticks = s.getTicksUntilArrival(be.getLevel().getGameTime());
                    status = "Transit (" + ticksToTime(ticks) + ")";
                    statusColor = 0x88BBFF;
                }
                case AT_MARKET -> {
                    long elapsed = s.getTimeAtMarket(be.getLevel().getGameTime());
                    int maxTime = DebugConfig.getMaxMarketTime();
                    long remaining = Math.max(0, maxTime - elapsed);
                    status = "Market (" + ticksToTime(remaining) + ")";
                    statusColor = 0xFFCC44;
                }
                case SOLD -> {
                    status = "SOLD!";
                    statusColor = 0x55FF55;
                }
                case RETURNING -> {
                    long currentTime = be.getLevel().getGameTime();
                    long remaining = Math.max(0, s.getReturnArrivalTime() - currentTime);
                    status = "Returning (" + ticksToTime(remaining) + ")";
                    statusColor = 0xFFAA88;
                }
                case COMPLETED -> {
                    status = "\u2714 Collect $";
                    statusColor = 0x55FF55;
                }
                case RETURNED -> {
                    status = "\u2714 Collect Items";
                    statusColor = 0xFFDD55;
                }
                default -> {
                    status = "Done";
                    statusColor = 0xAAAAAA;
                }
            }
        }

        // Truncate destination
        String destDisplay = dest;
        int statusW = this.font.width(status);
        int available = 170 - 42 - 4;
        if (this.font.width(destDisplay) > available) {
            while (this.font.width(destDisplay + "..") > available && destDisplay.length() > 3) {
                destDisplay = destDisplay.substring(0, destDisplay.length() - 1);
            }
            destDisplay += "..";
        }

        this.font.draw(ps, destDisplay, 40, yOff, 0xCCCCCC);
        this.font.draw(ps, status, 230 - statusW, yOff, statusColor);

        // Progress bar for IN_TRANSIT shipments
        if (s.getStatus() == Shipment.Status.IN_TRANSIT) {
            long totalTravel = s.getArrivalTime() - s.getDepartureTime();
            long elapsed = be.getLevel().getGameTime() - s.getDepartureTime();
            float progress = totalTravel > 0 ? Math.min(1.0f, (float) elapsed / totalTravel) : 1.0f;
            int barX = 40;
            int barY2 = yOff + 9;
            int barW = 188;
            int barH = 2;
            fill(ps, barX, barY2, barX + barW, barY2 + barH, 0xFF2A2A2A);
            if (progress > 0) {
                int fillW = Math.max(1, (int) (barW * progress));
                fill(ps, barX, barY2, barX + fillW, barY2 + barH, 0xFF88BBFF);
            }
        }
    }

    private void renderActivityOrderRow(PoseStack ps, BuyOrder order, TradingPostBlockEntity be, int yOff) {
        // Type indicator: "IN" for incoming orders
        this.font.draw(ps, "\u2193IN", 8, yOff, 0x44AAFF);

        TownData town = TownRegistry.getTown(order.getTownId());
        String itemName = order.getItemDisplayName();
        if (order.getCount() > 1) itemName += " x" + order.getCount();

        String status;
        int statusColor;
        if (order.getStatus() == BuyOrder.Status.IN_TRANSIT) {
            long ticks = order.getTicksUntilArrival(be.getLevel().getGameTime());
            status = ticksToTime(ticks);
            statusColor = 0x88BBFF;
        } else {
            status = "\u2714 Collect";
            statusColor = 0x55FF55;
        }

        // Truncate item name
        int statusW = this.font.width(status);
        int available = 170 - 42 - 4;
        if (this.font.width(itemName) > available) {
            while (this.font.width(itemName + "..") > available && itemName.length() > 3) {
                itemName = itemName.substring(0, itemName.length() - 1);
            }
            itemName += "..";
        }

        this.font.draw(ps, itemName, 40, yOff, 0xCCCCCC);
        this.font.draw(ps, status, 230 - statusW, yOff, statusColor);

        // Progress bar for IN_TRANSIT orders
        if (order.getStatus() == BuyOrder.Status.IN_TRANSIT) {
            long totalTravel = order.getArrivalTime() - order.getOrderTime();
            long elapsed = be.getLevel().getGameTime() - order.getOrderTime();
            float progress = totalTravel > 0 ? Math.min(1.0f, (float) elapsed / totalTravel) : 1.0f;
            int barX = 40;
            int barY2 = yOff + 9;
            int barW = 188;
            int barH = 2;
            fill(ps, barX, barY2, barX + barW, barY2 + barH, 0xFF2A2A2A);
            if (progress > 0) {
                int fillW = Math.max(1, (int) (barW * progress));
                fill(ps, barX, barY2, barX + fillW, barY2 + barH, 0xFF88BBFF);
            }
        }
    }

    // ==================== Economy Dashboard ====================

    private void renderEconomyDashboard(PoseStack ps) {
        this.font.draw(ps, "Economy Overview", 8, 36, 0xFFD700);

        TradingPostBlockEntity be = menu.getBlockEntity();
        if (be == null) {
            this.font.draw(ps, "No data available.", 10, 50, 0x888888);
            return;
        }

        long lifetime = be.getLifetimeEarnings();
        int totalShips = be.getTotalShipmentsSent();
        long avgEarnings = totalShips > 0 ? lifetime / totalShips : 0;

        // Summary stats
        int drawY = 48;
        this.font.draw(ps, "Total Earnings:", 8, drawY, 0xBBBBBB);
        CoinRenderer.renderCoinValue(ps, this.font, 95, drawY, (int) Math.min(lifetime, Integer.MAX_VALUE));
        drawY += 10;
        this.font.draw(ps, "Shipments:", 8, drawY, 0xBBBBBB);
        this.font.draw(ps, String.valueOf(totalShips), 95, drawY, 0xFFFFFF);
        drawY += 10;
        this.font.draw(ps, "Avg/Ship:", 8, drawY, 0xBBBBBB);
        CoinRenderer.renderCoinValue(ps, this.font, 95, drawY, (int) Math.min(avgEarnings, Integer.MAX_VALUE));

        // Top items (right column)
        Map<String, Long> itemEarnings = be.getEarningsByItem();
        if (!itemEarnings.isEmpty()) {
            this.font.draw(ps, "Top Items", 140, 48, 0xFFD700);
            List<Map.Entry<String, Long>> sorted = itemEarnings.entrySet().stream()
                    .sorted(Comparator.<Map.Entry<String, Long>, Long>comparing(Map.Entry::getValue).reversed())
                    .limit(5)
                    .toList();
            int itemY = 58;
            for (Map.Entry<String, Long> entry : sorted) {
                String name = entry.getKey();
                if (this.font.width(name) > 60) {
                    while (this.font.width(name + "..") > 60 && name.length() > 2) {
                        name = name.substring(0, name.length() - 1);
                    }
                    name += "..";
                }
                this.font.draw(ps, name, 140, itemY, 0xCCCCCC);
                CoinRenderer.renderCoinValue(ps, this.font, 205, itemY, (int) Math.min(entry.getValue(), Integer.MAX_VALUE));
                itemY += 9;
            }
        }

        // Best towns (bottom section)
        Map<String, Long> townEarnings = be.getEarningsByTown();
        if (!townEarnings.isEmpty()) {
            drawY += 14;
            this.font.draw(ps, "Best Towns", 8, drawY, 0xFFD700);
            drawY += 10;
            List<Map.Entry<String, Long>> sortedTowns = townEarnings.entrySet().stream()
                    .sorted(Comparator.<Map.Entry<String, Long>, Long>comparing(Map.Entry::getValue).reversed())
                    .limit(4)
                    .toList();
            for (Map.Entry<String, Long> entry : sortedTowns) {
                TownData town = TownRegistry.getTown(entry.getKey());
                String townName = town != null ? town.getDisplayName() : entry.getKey();
                if (this.font.width(townName) > 70) {
                    while (this.font.width(townName + "..") > 70 && townName.length() > 2) {
                        townName = townName.substring(0, townName.length() - 1);
                    }
                    townName += "..";
                }
                this.font.draw(ps, townName, 10, drawY, 0xCCCCCC);
                CoinRenderer.renderCoinValue(ps, this.font, 85, drawY, (int) Math.min(entry.getValue(), Integer.MAX_VALUE));

                // Show shipment count from history for this town
                if (totalShips > 0) {
                    long townAvg = entry.getValue() / Math.max(1, countShipmentsForTown(be, entry.getKey()));
                    this.font.draw(ps, "avg:", 140, drawY, 0x888888);
                    CoinRenderer.renderCoinValue(ps, this.font, 160, drawY, (int) Math.min(townAvg, Integer.MAX_VALUE));
                }
                drawY += 9;
            }
        }

        // Hint
        if (lifetime == 0) {
            this.font.draw(ps, "Send shipments to see", 10, 80, 0x888888);
            this.font.draw(ps, "your economy stats here!", 10, 90, 0x888888);
        }
    }

    private int countShipmentsForTown(TradingPostBlockEntity be, String townId) {
        TownData td = TownRegistry.getTown(townId);
        String displayName = td != null ? td.getDisplayName() : townId;
        int count = 0;
        for (net.minecraft.nbt.CompoundTag record : be.getShipmentHistory()) {
            if (displayName.equals(record.getString("Town"))) {
                count++;
            }
        }
        return Math.max(1, count);
    }

    // ==================== Towns Tab ====================

    private void renderTownsLabels(PoseStack poseStack) {
        List<TownData> allTowns = new ArrayList<>(TownRegistry.getAllTowns());
        if (allTowns.isEmpty()) return;

        townViewPage = Math.min(townViewPage, allTowns.size() - 1);
        TownData town = allTowns.get(townViewPage);
        int traderLevel = menu.getTraderLevel();
        boolean unlocked = town.getMinTraderLevel() <= traderLevel;

        TradingPostBlockEntity be = menu.getBlockEntity();

        // LEFT PAGE: town info
        int leftX = 12;
        int rightX = 136;

        // Page indicator (top of left page)
        String pageInfo = (townViewPage + 1) + " / " + allTowns.size();
        this.font.draw(poseStack, pageInfo, leftX, 38, 0x6B5A3E);

        // Town name
        if (!unlocked) {
            this.font.draw(poseStack, "\u2716 " + town.getDisplayName(), leftX, 50, 0x664444);
        } else {
            this.font.draw(poseStack, town.getDisplayName(), leftX, 50, 0x3B2A14);
        }

        // Type + distance
        String meta = town.getType().getDisplayName() + " | Dist " + town.getDistance();
        this.font.draw(poseStack, meta, leftX, 62, 0x6B5A3E);

        // Min level
        String lvlReq = "Requires Lvl " + town.getMinTraderLevel();
        int lvlColor = unlocked ? 0x3B7A3B : 0x8B2222;
        this.font.draw(poseStack, lvlReq, leftX, 72, lvlColor);
        
        // Reputation with this town (on separate line)
        if (be != null && unlocked) {
            int rep = be.getReputation(town.getId());
            String repLevel = TradingPostBlockEntity.getReputationLevel(rep);
            int repColor = TradingPostBlockEntity.getReputationColor(rep);
            this.font.draw(poseStack, "Rep: " + repLevel + " (" + rep + ")", leftX, 82, repColor);
        }

        // Description (wrap 2 lines to fit)
        List<FormattedCharSequence> descLines = this.font.split(
                Component.literal(town.getDescription()), 108);
        for (int i = 0; i < Math.min(descLines.size(), 2); i++) {
            this.font.draw(poseStack, descLines.get(i), leftX, 94 + i * 10, 0x5C4A32);
        }

        // Demand level
        if (be != null && !selectingDiplomatItem) {
            double demand = be.getDemandTracker().getTownDemandLevel(town.getId());
            int pct = (int) (demand * 100);
            String demandStr = "Demand: " + pct + "%";
            int demandColor = pct >= 80 ? 0x3B7A3B : (pct >= 50 ? 0x8B7355 : 0x8B2222);
            // Draw on left page, at bottom (but above buttons)
        }

        // RIGHT PAGE: changes based on diplomat selection mode
        if (selectingDiplomatItem) {
            renderDiplomatSelectionPage(poseStack, rightX, town);
        } else {
        // Normal mode: needs & surplus with NeedLevel indicators (scrollable)
        // Build a list of all lines, then render visible ones based on scroll
        List<String> rightLines = new ArrayList<>();
        List<Integer> rightColors = new ArrayList<>();

        // Group items by NeedLevel for a clearer display
        // Desperate & High Need items first (things the town wants)
        List<String[]> desperateItems = new ArrayList<>();
        List<String[]> highNeedItems = new ArrayList<>();
        List<String[]> moderateItems = new ArrayList<>();
        List<String[]> surplusItems = new ArrayList<>();
        List<String[]> oversatItems = new ArrayList<>();

        // Check all needs items for their actual NeedLevel
        for (ResourceLocation rl : town.getNeeds()) {
            Item item = ForgeRegistries.ITEMS.getValue(rl);
            if (item == null) continue;
            NeedLevel level = town.getNeedLevel(item);
            String iname = new ItemStack(item).getHoverName().getString();
            if (this.font.width(iname) > 86) {
                while (this.font.width(iname + "..") > 86 && iname.length() > 3)
                    iname = iname.substring(0, iname.length() - 1);
                iname += "..";
            }
            switch (level) {
                case DESPERATE -> desperateItems.add(new String[]{iname, level.getDisplayName()});
                case HIGH_NEED -> highNeedItems.add(new String[]{iname, level.getDisplayName()});
                case MODERATE_NEED -> moderateItems.add(new String[]{iname, level.getDisplayName()});
                default -> highNeedItems.add(new String[]{iname, "Needed"}); // legacy fallback
            }
        }

        // Check all surplus items for their actual NeedLevel
        for (ResourceLocation rl : town.getSurplus()) {
            Item item = ForgeRegistries.ITEMS.getValue(rl);
            if (item == null) continue;
            NeedLevel level = town.getNeedLevel(item);
            String iname = new ItemStack(item).getHoverName().getString();
            if (this.font.width(iname) > 86) {
                while (this.font.width(iname + "..") > 86 && iname.length() > 3)
                    iname = iname.substring(0, iname.length() - 1);
                iname += "..";
            }
            switch (level) {
                case OVERSATURATED -> oversatItems.add(new String[]{iname, level.getDisplayName()});
                case SURPLUS -> surplusItems.add(new String[]{iname, level.getDisplayName()});
                default -> surplusItems.add(new String[]{iname, "Surplus"}); // legacy fallback
            }
        }

        // Render grouped by urgency
        if (!desperateItems.isEmpty()) {
            rightLines.add("\u2757 Desperate:");
            rightColors.add(NeedLevel.DESPERATE.getColor());
            for (String[] entry : desperateItems) {
                rightLines.add("  \u2022 " + entry[0]);
                rightColors.add(NeedLevel.DESPERATE.getColor());
            }
        }

        if (!highNeedItems.isEmpty()) {
            if (!rightLines.isEmpty()) { rightLines.add(""); rightColors.add(0); }
            rightLines.add("\u26A0 High Need:");
            rightColors.add(NeedLevel.HIGH_NEED.getColor());
            for (String[] entry : highNeedItems) {
                rightLines.add("  \u2022 " + entry[0]);
                rightColors.add(NeedLevel.HIGH_NEED.getColor());
            }
        }

        if (!moderateItems.isEmpty()) {
            if (!rightLines.isEmpty()) { rightLines.add(""); rightColors.add(0); }
            rightLines.add("Moderate:");
            rightColors.add(NeedLevel.MODERATE_NEED.getColor());
            for (String[] entry : moderateItems) {
                rightLines.add("  \u2022 " + entry[0]);
                rightColors.add(NeedLevel.MODERATE_NEED.getColor());
            }
        }

        if (!surplusItems.isEmpty() || !oversatItems.isEmpty()) {
            if (!rightLines.isEmpty()) { rightLines.add(""); rightColors.add(0); }
            rightLines.add("\u25BC Surplus:");
            rightColors.add(NeedLevel.SURPLUS.getColor());
            for (String[] entry : surplusItems) {
                rightLines.add("  \u2022 " + entry[0]);
                rightColors.add(NeedLevel.SURPLUS.getColor());
            }
            for (String[] entry : oversatItems) {
                rightLines.add("  \u2022 " + entry[0] + " \u2716");
                rightColors.add(NeedLevel.OVERSATURATED.getColor());
            }
        }

        int visibleLines = 9; // ~82px at 9px each, leaving room at bottom
        int maxScrollR = Math.max(0, rightLines.size() - visibleLines);
        townContentScroll = Math.min(townContentScroll, maxScrollR);

        int drawY = 50; // Start below the parchment header area
        for (int li = townContentScroll; li < rightLines.size() && (li - townContentScroll) < visibleLines; li++) {
            this.font.draw(poseStack, rightLines.get(li), rightX, drawY, rightColors.get(li));
            drawY += 9;
        }

        // Scroll indicators
        if (townContentScroll > 0) {
            this.font.draw(poseStack, "\u25B2", rightX + 104, 50, 0x6B5A3E);
        }
        if (townContentScroll < maxScrollR) {
            this.font.draw(poseStack, "\u25BC", rightX + 104, 125, 0x6B5A3E);
        }
        } // close else (normal right page mode)
    }

    /**
     * Renders the right page in diplomat selection mode — showing clickable surplus items.
     */
    private void renderDiplomatSelectionPage(PoseStack poseStack, int rightX, TownData town) {
        this.font.draw(poseStack, "\u270D Request Item", rightX, 50, 0xFFD700);
        this.font.draw(poseStack, "from " + town.getDisplayName(), rightX, 60, 0x6B5A3E);
        
        // Quantity selector
        String qtyLabel = "Qty: [-] " + diplomatQuantity + " [+]";
        this.font.draw(poseStack, qtyLabel, rightX, 134, 0xAABBFF);

        if (currentSurplusList.isEmpty()) {
            this.font.draw(poseStack, "No surplus items", rightX, 76, 0x888888);
            this.font.draw(poseStack, "available.", rightX, 86, 0x888888);
            return;
        }

        int visibleItems = 6; // reduced by 1 to make room for quantity selector
        int maxScroll = Math.max(0, currentSurplusList.size() - visibleItems);
        townContentScroll = Math.min(townContentScroll, maxScroll);

        int drawY = 72;
        int displayed = 0;
        for (int i = townContentScroll; i < currentSurplusList.size() && displayed < visibleItems; i++, displayed++) {
            ResourceLocation rl = currentSurplusList.get(i);
            Item item = ForgeRegistries.ITEMS.getValue(rl);
            if (item == null) continue;

            String iname = new ItemStack(item).getHoverName().getString();
            if (this.font.width(iname) > 86) {
                while (this.font.width(iname + "..") > 86 && iname.length() > 3)
                    iname = iname.substring(0, iname.length() - 1);
                iname += "..";
            }

            boolean isHovered = (displayed == hoveredSurplusRow);
            int color = isHovered ? 0x55FF55 : 0xBBBBBB;
            String prefix = isHovered ? "\u25B6 " : "  \u2022 ";
            this.font.draw(poseStack, prefix + iname, rightX, drawY, color);
            drawY += 9;
        }

        // Scroll indicators
        if (townContentScroll > 0) {
            this.font.draw(poseStack, "\u25B2", rightX + 104, 72, 0x6B5A3E);
        }
        if (townContentScroll < maxScroll) {
            this.font.draw(poseStack, "\u25BC", rightX + 104, 118, 0x6B5A3E);
        }
    }

    // ==================== Quests Tab ====================

    private void renderQuestsLabels(PoseStack poseStack) {
        this.font.draw(poseStack, "Trade Quests", 8, 36, 0xFFD700);

        TradingPostBlockEntity be = menu.getBlockEntity();
        if (be == null) return;

        List<Quest> quests = be.getActiveQuests();
        int maxScroll = Math.max(0, quests.size() - VISIBLE_QUESTS);
        questScrollOffset = Math.min(questScrollOffset, maxScroll);

        if (quests.isEmpty()) {
            this.font.draw(poseStack, "No quests available.", 10, 60, 0x888888);
            this.font.draw(poseStack, "Quests refresh each dawn.", 10, 72, 0x888888);
            return;
        }

        // Count indicator
        if (quests.size() > VISIBLE_QUESTS) {
            String scrollInfo = (questScrollOffset + 1) + "-" +
                    Math.min(questScrollOffset + VISIBLE_QUESTS, quests.size()) +
                    " of " + quests.size();
            int scrollW = this.font.width(scrollInfo);
            this.font.draw(poseStack, scrollInfo, 230 - scrollW, 36, 0x666666);
        }

        // Column headers
        this.font.draw(poseStack, "Type", 8, 48, 0xFFD700);
        this.font.draw(poseStack, "Item", 45, 48, 0xFFD700);
        this.font.draw(poseStack, "Qty", 115, 48, 0xFFD700);
        this.font.draw(poseStack, "Reward", 145, 48, 0xFFD700);
        this.font.draw(poseStack, "Status", 195, 48, 0xFFD700);

        int yOff = 59;
        int displayed = 0;
        for (int i = questScrollOffset; i < quests.size() && displayed < VISIBLE_QUESTS; i++, displayed++) {
            Quest quest = quests.get(i);
            
            // Quest type badge (colored)
            String typeLabel = switch (quest.getQuestType()) {
                case STANDARD -> "STD";
                case BULK -> "BULK";
                case RUSH -> "RUSH";
                case SPECIALTY -> "SPEC";
                case CHARITY -> "AID";
            };
            int typeColor = quest.getQuestType().getColor();
            this.font.draw(poseStack, typeLabel, 8, yOff, typeColor);

            // Item name (truncated)
            String itemName = quest.getItemDisplayName();
            if (this.font.width(itemName) > 65) {
                while (this.font.width(itemName + "..") > 65 && itemName.length() > 3) {
                    itemName = itemName.substring(0, itemName.length() - 1);
                }
                itemName += "..";
            }

            // Quantity: delivered/required
            String qty = quest.getDeliveredCount() + "/" + quest.getRequiredCount();

            // Reward text (coins only - reputation shown in tooltip)
            String reward = formatCoinText(quest.getRewardCoins());

            // Status text + color
            String status;
            int statusColor;
            switch (quest.getStatus()) {
                case AVAILABLE -> {
                    status = "[Accept]";
                    statusColor = 0x88BBFF;
                }
                case ACCEPTED -> {
                    long ticks = quest.getTicksRemaining(be.getLevel().getGameTime());
                    if (quest.getRemainingCount() == 0) {
                        status = "\u2714";
                        statusColor = 0x55FF55;
                    } else {
                        status = ticksToTime(ticks);
                        statusColor = 0xFFCC44;
                    }
                }
                case COMPLETED -> {
                    status = "\u2714";
                    statusColor = 0x55FF55;
                }
                default -> {
                    status = "Exp";
                    statusColor = 0x884444;
                }
            }

            this.font.draw(poseStack, itemName, 45, yOff, 0xCCCCCC);
            this.font.draw(poseStack, qty, 115, yOff, 0xAAAAAA);
            this.font.draw(poseStack, reward, 145, yOff, 0xFFD700);
            this.font.draw(poseStack, status, 195, yOff, statusColor);

            yOff += 14;
        }
    }

    // ==================== Workers Tab ====================

    private void renderWorkersLabels(PoseStack poseStack) {
        this.font.draw(poseStack, "Workers", 8, 36, 0xFFD700);

        TradingPostBlockEntity be = menu.getBlockEntity();
        if (be == null) return;

        // Left panel: Negotiator
        renderWorkerPanel(poseStack, 10, 38, be.getNegotiator());

        // Right panel: Trading Cart
        renderWorkerPanel(poseStack, 136, 38, be.getTradingCart());
    }

    private void renderWorkerPanel(PoseStack poseStack, int px, int py, com.offtomarket.mod.data.Worker worker) {
        // Worker type name
        this.font.draw(poseStack, worker.getType().getDisplayName(), px, py, 0xFFD700);

        if (!worker.isHired()) {
            // Show description and hire cost
            List<FormattedCharSequence> descLines = this.font.split(
                    Component.literal(worker.getType().getDescription()), 110);
            for (int i = 0; i < descLines.size(); i++) {
                this.font.draw(poseStack, descLines.get(i), px, py + 12 + i * 10, 0x888888);
            }
            this.font.draw(poseStack, "Cost: " + formatCoinText(worker.getHireCost()), px, py + 38, 0xFFAA44);
            return;
        }

        // Hired — show stats
        this.font.draw(poseStack, "Level " + worker.getLevel()
                + (worker.getLevel() >= com.offtomarket.mod.data.Worker.MAX_LEVEL ? " (MAX)" : ""), px, py + 12, 0xCCCCCC);

        // XP bar
        if (worker.getLevel() < com.offtomarket.mod.data.Worker.MAX_LEVEL) {
            int xpNeeded = worker.getXpForNextLevel();
            int barX = px;
            int barY = py + 23;
            int barW = 100;
            fill(poseStack, barX - 1, barY - 1, barX + barW + 1, barY + 7, 0xFF1A1209);
            fill(poseStack, barX, barY, barX + barW, barY + 6, 0xFF2A2A2A);
            float frac = Math.min(1.0f, worker.getXp() / (float) xpNeeded);
            if (frac > 0) {
                int fillW = Math.max(1, (int) (barW * frac));
                fill(poseStack, barX, barY, barX + fillW, barY + 6, 0xFF55CC55);
            }
            String xpText = worker.getXp() + "/" + xpNeeded;
            int xpW = this.font.width(xpText);
            this.font.draw(poseStack, xpText, px + (barW - xpW) / 2, barY - 1, 0xCCCCCC);
        }

        // Bonus
        this.font.draw(poseStack, worker.getBonusDisplay(), px, py + 34, 0x88CC88);

        // Trip cost
        this.font.draw(poseStack, "Cost/trip: " + formatCoinText(worker.getPerTripCost()), px, py + 46, 0xAAAAAA);

        // Total trips
        this.font.draw(poseStack, "Trips: " + worker.getTotalTrips(), px, py + 58, 0x888888);
    }

    // ==================== Diplomat Tab ====================

    private void renderDiplomatLabels(PoseStack poseStack) {
        this.font.draw(poseStack, "Trade Requests", 8, 36, 0xFFD700);

        TradingPostBlockEntity be = menu.getBlockEntity();
        if (be == null) return;

        List<DiplomatRequest> requests = be.getActiveDiplomatRequests();
        int maxScroll = Math.max(0, requests.size() - VISIBLE_DIPLOMAT);
        diplomatScrollOffset = Math.min(diplomatScrollOffset, maxScroll);

        if (requests.isEmpty()) {
            this.font.draw(poseStack, "No active requests.", 10, 60, 0x888888);
            this.font.draw(poseStack, "Request items from the", 10, 72, 0x888888);
            this.font.draw(poseStack, "Towns tab.", 10, 84, 0x888888);
            return;
        }

        // Count indicator
        if (requests.size() > VISIBLE_DIPLOMAT) {
            String scrollInfo = (diplomatScrollOffset + 1) + "-" +
                    Math.min(diplomatScrollOffset + VISIBLE_DIPLOMAT, requests.size()) +
                    " of " + requests.size();
            int scrollW = this.font.width(scrollInfo);
            this.font.draw(poseStack, scrollInfo, 230 - scrollW, 36, 0x666666);
        }

        // Column headers
        this.font.draw(poseStack, "Item", 8, 48, 0xFFD700);
        this.font.draw(poseStack, "Town", 80, 48, 0xFFD700);
        this.font.draw(poseStack, "Status", 140, 48, 0xFFD700);

        int yOff = 59;
        int displayed = 0;
        for (int i = diplomatScrollOffset; i < requests.size() && displayed < VISIBLE_DIPLOMAT; i++, displayed++) {
            DiplomatRequest req = requests.get(i);

            // Item name with count
            String itemName = req.getItemDisplayName();
            if (req.getRequestedCount() > 1) itemName += " x" + req.getRequestedCount();
            if (this.font.width(itemName) > 68) {
                while (this.font.width(itemName + "..") > 68 && itemName.length() > 3) {
                    itemName = itemName.substring(0, itemName.length() - 1);
                }
                itemName += "..";
            }

            // Town name (truncated)
            TownData town = TownRegistry.getTown(req.getTownId());
            String townName = town != null ? town.getDisplayName() : req.getTownId();
            if (this.font.width(townName) > 55) {
                while (this.font.width(townName + "..") > 55 && townName.length() > 3) {
                    townName = townName.substring(0, townName.length() - 1);
                }
                townName += "..";
            }

            // Status text based on new stages
            String status;
            int statusColor;
            float progress = 0;
            boolean showProgress = false;
            long gameTime = be.getLevel() != null ? be.getLevel().getGameTime() : 0;
            
            switch (req.getStatus()) {
                case TRAVELING_TO -> {
                    long ticks = req.getTicksRemaining(gameTime);
                    status = "Sending: " + ticksToTime(ticks);
                    statusColor = 0x88BBFF;
                    progress = req.getStageProgress(gameTime);
                    showProgress = true;
                }
                case DISCUSSING -> {
                    status = formatCoinText(req.getProposedPrice());
                    statusColor = 0xFFCC44;
                }
                case WAITING_FOR_GOODS -> {
                    long ticks = req.getTicksRemaining(gameTime);
                    status = "Preparing: " + ticksToTime(ticks);
                    statusColor = 0xAABBFF;
                    progress = req.getStageProgress(gameTime);
                    showProgress = true;
                }
                case TRAVELING_BACK -> {
                    long ticks = req.getTicksRemaining(gameTime);
                    status = "Returning: " + ticksToTime(ticks);
                    statusColor = 0x88DDFF;
                    progress = req.getStageProgress(gameTime);
                    showProgress = true;
                }
                case ARRIVED -> {
                    status = "\u2714 Collect";
                    statusColor = 0x55FF55;
                }
                default -> {
                    status = "Failed";
                    statusColor = 0xFF4444;
                }
            }

            this.font.draw(poseStack, itemName, 8, yOff, 0xCCCCCC);
            this.font.draw(poseStack, townName, 80, yOff, 0xAAAAAA);
            
            // For DISCUSSING status, show price and Accept/Decline buttons
            if (req.getStatus() == DiplomatRequest.Status.DISCUSSING) {
                this.font.draw(poseStack, status, 140, yOff, statusColor);
                // Accept button (green checkmark)
                this.font.draw(poseStack, "\u2714", 200, yOff, 0x55FF55);
                // Decline button (red X)
                this.font.draw(poseStack, "\u2718", 215, yOff, 0xFF5555);
            } else if (showProgress) {
                // Draw progress bar for traveling/waiting stages
                int barX = 140;
                int barY = yOff;
                int barW = 60;
                int barH = 8;
                // Background
                fill(poseStack, barX, barY, barX + barW, barY + barH, 0xFF333333);
                // Progress fill
                int fillW = (int)(barW * progress);
                fill(poseStack, barX, barY, barX + fillW, barY + barH, statusColor | 0xFF000000);
                // Border
                hLine(poseStack, barX, barX + barW - 1, barY, 0xFF555555);
                hLine(poseStack, barX, barX + barW - 1, barY + barH - 1, 0xFF555555);
                vLine(poseStack, barX, barY, barY + barH - 1, 0xFF555555);
                vLine(poseStack, barX + barW - 1, barY, barY + barH - 1, 0xFF555555);
                // Time text (right-aligned after bar)
                String timeStr = ticksToTime(req.getTicksRemaining(gameTime));
                int timeW = this.font.width(timeStr);
                this.font.draw(poseStack, timeStr, 228 - timeW, yOff, 0x888888);
            } else {
                int statusW = this.font.width(status);
                this.font.draw(poseStack, status, 230 - statusW, yOff, statusColor);
            }

            yOff += 14;
        }
    }

    // ==================== Utilities ====================

    private String ticksToTime(long ticks) {
        long seconds = ticks / 20;
        long minutes = seconds / 60;
        seconds %= 60;
        if (minutes > 0) return minutes + "m " + seconds + "s";
        return seconds + "s";
    }

    private String formatCoinText(int copperPieces) {
        if (DebugConfig.isGoldOnlyMode()) {
            int gp = Math.max(1, (copperPieces + 99) / 100);
            return gp + "g";
        }
        int gp = copperPieces / 100;
        int sp = (copperPieces % 100) / 10;
        int cp = copperPieces % 10;
        StringBuilder sb = new StringBuilder();
        if (gp > 0) sb.append(gp).append("g");
        if (sp > 0) { if (sb.length() > 0) sb.append(" "); sb.append(sp).append("s"); }
        if (cp > 0 || sb.length() == 0) { if (sb.length() > 0) sb.append(" "); sb.append(cp).append("c"); }
        return sb.toString();
    }

    /**
     * Move the 3 coin exchange slots on/off screen based on the active tab.
     */
    private void updateCoinSlotPositions(boolean visible) {
        menu.setCoinSlotsActive(visible);
    }

    // ==================== Render & Tooltips ====================

    @Override
    public void render(PoseStack poseStack, int mouseX, int mouseY, float partialTick) {
        renderBackground(poseStack);
        super.render(poseStack, mouseX, mouseY, partialTick);
        renderTooltip(poseStack, mouseX, mouseY);

        // Activity tooltip on hover (Activity tab)
        if (currentTab == Tab.ACTIVITY && hoveredActivityRow >= 0) {
            renderActivityTooltip(poseStack, mouseX, mouseY);
        }

        // Quest tooltip on hover (Quests tab)
        if (currentTab == Tab.QUESTS && hoveredQuestRow >= 0) {
            renderQuestTooltip(poseStack, mouseX, mouseY);
        }

        // Diplomat tooltip on hover (Diplomat tab)
        if (currentTab == Tab.DIPLOMAT && hoveredDiplomatRow >= 0) {
            renderDiplomatTooltip(poseStack, mouseX, mouseY);
        }
    }

    private void renderActivityTooltip(PoseStack ps, int mouseX, int mouseY) {
        if (hoveredActivityRow < 0 || hoveredActivityRow >= VISIBLE_ACTIVITY) return;
        TradingPostBlockEntity be = menu.getBlockEntity();
        if (be == null) return;

        boolean isShipment = activityRowIsShipment[hoveredActivityRow];
        int dataIdx = activityRowDataIndex[hoveredActivityRow];

        if (isShipment) {
            List<Shipment> shipments = be.getActiveShipments();
            if (dataIdx >= shipments.size()) return;
            Shipment s = shipments.get(dataIdx);
            renderShipmentTooltipContent(ps, s, mouseX, mouseY);
        } else {
            List<BuyOrder> orders = be.getActiveBuyOrders();
            if (dataIdx >= orders.size()) return;
            BuyOrder order = orders.get(dataIdx);
            renderOrderTooltipContent(ps, order, be, mouseX, mouseY);
        }
    }

    private void renderShipmentTooltipContent(PoseStack ps, Shipment s, int mouseX, int mouseY) {
        List<Component> tooltip = new ArrayList<>();
        TownData town = TownRegistry.getTown(s.getTownId());
        tooltip.add(Component.literal("\u2191 Shipment to " + (town != null ? town.getDisplayName() : s.getTownId()))
                .withStyle(ChatFormatting.GOLD));
        tooltip.add(Component.literal(""));

        for (Shipment.ShipmentItem item : s.getItems()) {
            String icon = item.isSold() ? "\u2714 " : "\u2022 ";
            ChatFormatting color = item.isSold() ? ChatFormatting.GREEN : ChatFormatting.GRAY;
            tooltip.add(Component.literal(icon + item.getDisplayName() + " x" + item.getCount())
                    .withStyle(color));
        }

        if (s.getTotalEarnings() > 0) {
            tooltip.add(Component.literal(""));
            tooltip.add(Component.literal("Earnings: " + formatCoinText(s.getTotalEarnings()))
                    .withStyle(ChatFormatting.GOLD));
        }

        renderComponentTooltip(ps, tooltip, mouseX, mouseY);
    }

    private void renderOrderTooltipContent(PoseStack ps, BuyOrder order, TradingPostBlockEntity be, int mouseX, int mouseY) {
        List<Component> tooltip = new ArrayList<>();
        TownData town = TownRegistry.getTown(order.getTownId());

        tooltip.add(Component.literal("\u2193 " + order.getItemDisplayName() + " x" + order.getCount())
                .withStyle(ChatFormatting.WHITE, ChatFormatting.BOLD));
        tooltip.add(Component.literal("From: " + (town != null ? town.getDisplayName() : order.getTownId()))
                .withStyle(ChatFormatting.GRAY));
        tooltip.add(Component.literal("Paid: " + formatCoinText(order.getTotalPaid()))
                .withStyle(ChatFormatting.GOLD));

        if (order.getStatus() == BuyOrder.Status.IN_TRANSIT) {
            long ticks = order.getTicksUntilArrival(be.getLevel().getGameTime());
            tooltip.add(Component.literal("Arrives in: " + ticksToTime(ticks))
                    .withStyle(ChatFormatting.AQUA));
        } else {
            tooltip.add(Component.literal("\u2714 Click to collect!")
                    .withStyle(ChatFormatting.GREEN));
        }

        renderComponentTooltip(ps, tooltip, mouseX, mouseY);
    }

    private void renderQuestTooltip(PoseStack ps, int mouseX, int mouseY) {
        TradingPostBlockEntity be = menu.getBlockEntity();
        if (be == null) return;
        List<Quest> quests = be.getActiveQuests();
        int idx = questScrollOffset + hoveredQuestRow;
        if (idx >= quests.size()) return;

        Quest quest = quests.get(idx);
        List<Component> tooltip = new ArrayList<>();
        TownData town = TownRegistry.getTown(quest.getTownId());
        
        // Quest type header with color
        ChatFormatting typeFormat = switch (quest.getQuestType()) {
            case RUSH -> ChatFormatting.GOLD;
            case BULK -> ChatFormatting.AQUA;
            case SPECIALTY -> ChatFormatting.LIGHT_PURPLE;
            case CHARITY -> ChatFormatting.GREEN;
            default -> ChatFormatting.WHITE;
        };
        tooltip.add(Component.literal("[" + quest.getQuestType().getDisplayName() + "] ")
                .withStyle(typeFormat)
                .append(Component.literal(quest.getItemDisplayName() + " x" + quest.getRequiredCount())
                        .withStyle(ChatFormatting.WHITE, ChatFormatting.BOLD)));
        
        // Quest description (if present)
        if (quest.getQuestDescription() != null && !quest.getQuestDescription().isEmpty()) {
            tooltip.add(Component.literal("\"" + quest.getQuestDescription() + "\"")
                    .withStyle(ChatFormatting.ITALIC, ChatFormatting.GRAY));
        }
        
        tooltip.add(Component.literal("From: " + (town != null ? town.getDisplayName() : quest.getTownId()))
                .withStyle(ChatFormatting.GRAY));
        tooltip.add(Component.literal(""));

        // Rewards with reputation
        tooltip.add(Component.literal("Rewards:")
                .withStyle(ChatFormatting.GOLD));
        tooltip.add(Component.literal("  \u2022 " + formatCoinText(quest.getRewardCoins()) + " coins")
                .withStyle(ChatFormatting.YELLOW));
        tooltip.add(Component.literal("  \u2022 " + quest.getRewardXp() + " Trader XP")
                .withStyle(ChatFormatting.AQUA));
        tooltip.add(Component.literal("  \u2022 +" + quest.getRewardReputation() + " Reputation")
                .withStyle(ChatFormatting.GREEN));

        if (quest.getStatus() == Quest.Status.ACCEPTED) {
            tooltip.add(Component.literal(""));
            tooltip.add(Component.literal("Progress: " + quest.getDeliveredCount()
                    + " / " + quest.getRequiredCount())
                    .withStyle(ChatFormatting.AQUA));
            long ticks = quest.getTicksRemaining(be.getLevel().getGameTime());
            tooltip.add(Component.literal("Time left: " + ticksToTime(ticks))
                    .withStyle(ChatFormatting.YELLOW));
            if (quest.getRemainingCount() == 0) {
                tooltip.add(Component.literal("\u2714 Click to complete!")
                        .withStyle(ChatFormatting.GREEN, ChatFormatting.BOLD));
            } else {
                tooltip.add(Component.literal("Click to deliver items from inventory")
                        .withStyle(ChatFormatting.GRAY));
            }
        } else if (quest.getStatus() == Quest.Status.AVAILABLE) {
            tooltip.add(Component.literal(""));
            long ticks = quest.getTicksRemaining(be.getLevel().getGameTime());
            tooltip.add(Component.literal("Expires in: " + ticksToTime(ticks))
                    .withStyle(ChatFormatting.YELLOW));
            tooltip.add(Component.literal("Click to accept!")
                    .withStyle(ChatFormatting.AQUA, ChatFormatting.BOLD));
        }

        renderComponentTooltip(ps, tooltip, mouseX, mouseY);
    }

    private void renderDiplomatTooltip(PoseStack ps, int mouseX, int mouseY) {
        TradingPostBlockEntity be = menu.getBlockEntity();
        if (be == null) return;
        List<DiplomatRequest> requests = be.getActiveDiplomatRequests();
        int idx = diplomatScrollOffset + hoveredDiplomatRow;
        if (idx >= requests.size()) return;

        DiplomatRequest req = requests.get(idx);
        List<Component> tooltip = new ArrayList<>();
        TownData town = TownRegistry.getTown(req.getTownId());

        tooltip.add(Component.literal(req.getItemDisplayName() + " x" + req.getRequestedCount())
                .withStyle(ChatFormatting.WHITE, ChatFormatting.BOLD));
        tooltip.add(Component.literal("From: " + (town != null ? town.getDisplayName() : req.getTownId()))
                .withStyle(ChatFormatting.GRAY));
        
        long gameTime = be.getLevel() != null ? be.getLevel().getGameTime() : 0;

        switch (req.getStatus()) {
            case TRAVELING_TO -> {
                tooltip.add(Component.literal("Request being sent...")
                        .withStyle(ChatFormatting.AQUA));
                tooltip.add(Component.literal("Arrives in: " + ticksToTime(req.getTicksRemaining(gameTime)))
                        .withStyle(ChatFormatting.GRAY));
            }
            case DISCUSSING -> {
                tooltip.add(Component.literal("Proposed Price: " + formatCoinText(req.getProposedPrice()))
                        .withStyle(ChatFormatting.GOLD));
                tooltip.add(Component.literal("(Includes +" + formatCoinText(req.getDiplomatPremium()) + " premium)")
                        .withStyle(ChatFormatting.DARK_GRAY));
                tooltip.add(Component.literal("\u2714 Click checkmark to accept")
                        .withStyle(ChatFormatting.GREEN));
                tooltip.add(Component.literal("\u2718 Click X to decline")
                        .withStyle(ChatFormatting.RED));
            }
            case WAITING_FOR_GOODS -> {
                tooltip.add(Component.literal("Paid: " + formatCoinText(req.getFinalCost()))
                        .withStyle(ChatFormatting.GOLD));
                tooltip.add(Component.literal("Preparing goods: " + ticksToTime(req.getTicksRemaining(gameTime)))
                        .withStyle(ChatFormatting.YELLOW));
            }
            case TRAVELING_BACK -> {
                tooltip.add(Component.literal("Paid: " + formatCoinText(req.getFinalCost()))
                        .withStyle(ChatFormatting.GOLD));
                tooltip.add(Component.literal("Returning with goods: " + ticksToTime(req.getTicksRemaining(gameTime)))
                        .withStyle(ChatFormatting.AQUA));
            }
            case ARRIVED -> {
                tooltip.add(Component.literal("Paid: " + formatCoinText(req.getFinalCost()))
                        .withStyle(ChatFormatting.GOLD));
                tooltip.add(Component.literal("\u2714 Click to collect!")
                        .withStyle(ChatFormatting.GREEN));
            }
            case FAILED -> tooltip.add(Component.literal("\u2716 Request failed or declined")
                    .withStyle(ChatFormatting.RED));
        }

        renderComponentTooltip(ps, tooltip, mouseX, mouseY);
    }

    // ==================== Mouse Handling ====================

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        int x = this.leftPos;
        int y = this.topPos;

        // Tab click detection (6 tabs)
        for (int i = 0; i < TAB_COUNT; i++) {
            int tx = x + TAB_X[i];
            int ty = y + TAB_Y;
            if (mouseX >= tx && mouseX < tx + TAB_W
                    && mouseY >= ty && mouseY < ty + TAB_H) {
                switchTab(Tab.values()[i]);
                return true;
            }
        }

        // Activity tab: click to collect arrived orders or completed shipments
        // Right-click on IN_TRANSIT/AT_MARKET to show cancel confirmation
        if (currentTab == Tab.ACTIVITY && hoveredActivityRow >= 0) {
            TradingPostBlockEntity be = menu.getBlockEntity();
            if (be != null && hoveredActivityRow < VISIBLE_ACTIVITY) {
                boolean isShipment = activityRowIsShipment[hoveredActivityRow];
                int dataIdx = activityRowDataIndex[hoveredActivityRow];
                if (isShipment) {
                    List<Shipment> shipments = be.getActiveShipments();
                    if (dataIdx < shipments.size()) {
                        Shipment shipment = shipments.get(dataIdx);
                        Shipment.Status status = shipment.getStatus();
                        
                        if (button == 1) {
                            // Right-click: toggle cancel confirmation for cancellable shipments
                            if (status == Shipment.Status.IN_TRANSIT || status == Shipment.Status.AT_MARKET) {
                                if (shipment.getId().equals(confirmCancelShipmentId)) {
                                    confirmCancelShipmentId = null; // Toggle off
                                } else {
                                    confirmCancelShipmentId = shipment.getId(); // Show confirm
                                    SoundHelper.playUIClick();
                                }
                                return true;
                            }
                        } else if (button == 0) {
                            // Left-click actions
                            if (shipment.getId().equals(confirmCancelShipmentId)) {
                                // Confirming cancel
                                ModNetwork.CHANNEL.send(PacketDistributor.SERVER.noArg(),
                                        new CancelShipmentPacket(be.getBlockPos(), shipment.getId()));
                                confirmCancelShipmentId = null;
                                SoundHelper.playUIClick();
                                return true;
                            } else if (status == Shipment.Status.COMPLETED) {
                                // Collect coins from sold shipment
                                ModNetwork.CHANNEL.send(PacketDistributor.SERVER.noArg(),
                                        new CollectShipmentCoinsPacket(be.getBlockPos(), shipment.getId()));
                                SoundHelper.playUIClick();
                                return true;
                            } else if (status == Shipment.Status.RETURNED) {
                                // Collect items from cancelled shipment
                                ModNetwork.CHANNEL.send(PacketDistributor.SERVER.noArg(),
                                        new CollectReturnedItemsPacket(be.getBlockPos(), shipment.getId()));
                                SoundHelper.playUIClick();
                                return true;
                            }
                        }
                    }
                } else {
                    // It's an order - check if collectible (left-click only)
                    if (button == 0) {
                        List<BuyOrder> orders = be.getActiveBuyOrders();
                        if (dataIdx < orders.size()) {
                            BuyOrder order = orders.get(dataIdx);
                            if (order.getStatus() == BuyOrder.Status.ARRIVED) {
                                ModNetwork.CHANNEL.send(PacketDistributor.SERVER.noArg(),
                                        new CollectOrderPacket(be.getBlockPos(), order.getId()));
                                SoundHelper.playUIClick();
                                return true;
                            }
                        }
                    }
                }
            }
            // Clear cancel confirmation if clicked elsewhere in activity tab
            if (button == 0 && confirmCancelShipmentId != null) {
                confirmCancelShipmentId = null;
            }
        } else {
            // Clear cancel confirmation when switching away from activity tab
            confirmCancelShipmentId = null;
        }

        // Quests tab: click to accept or deliver quests
        if (currentTab == Tab.QUESTS && hoveredQuestRow >= 0) {
            TradingPostBlockEntity be = menu.getBlockEntity();
            if (be != null) {
                List<Quest> quests = be.getActiveQuests();
                int idx = questScrollOffset + hoveredQuestRow;
                if (idx < quests.size()) {
                    Quest quest = quests.get(idx);
                    if (quest.getStatus() == Quest.Status.AVAILABLE) {
                        ModNetwork.CHANNEL.send(PacketDistributor.SERVER.noArg(),
                                new AcceptQuestPacket(be.getBlockPos(), quest.getId()));
                        SoundHelper.playUIClick();
                        return true;
                    } else if (quest.getStatus() == Quest.Status.ACCEPTED) {
                        ModNetwork.CHANNEL.send(PacketDistributor.SERVER.noArg(),
                                new DeliverQuestPacket(be.getBlockPos(), quest.getId()));
                        SoundHelper.playUIClick();
                        return true;
                    }
                }
            }
        }

        // Diplomat tab: click to collect arrived requests OR accept/decline proposals
        if (currentTab == Tab.DIPLOMAT && hoveredDiplomatRow >= 0) {
            TradingPostBlockEntity be = menu.getBlockEntity();
            if (be != null) {
                List<DiplomatRequest> requests = be.getActiveDiplomatRequests();
                int idx = diplomatScrollOffset + hoveredDiplomatRow;
                if (idx < requests.size()) {
                    DiplomatRequest req = requests.get(idx);
                    if (req.getStatus() == DiplomatRequest.Status.ARRIVED) {
                        ModNetwork.CHANNEL.send(PacketDistributor.SERVER.noArg(),
                                new CollectDiplomatPacket(be.getBlockPos(), req.getId()));
                        return true;
                    } else if (req.getStatus() == DiplomatRequest.Status.DISCUSSING) {
                        // Check if clicking accept (checkmark at x=200) or decline (X at x=215)
                        double localX = mouseX - this.leftPos;
                        if (localX >= 198 && localX <= 210) {
                            // Accept button clicked
                            ModNetwork.CHANNEL.send(PacketDistributor.SERVER.noArg(),
                                    new AcceptDiplomatPacket(be.getBlockPos(), req.getId()));
                            return true;
                        } else if (localX >= 213 && localX <= 225) {
                            // Decline button clicked
                            ModNetwork.CHANNEL.send(PacketDistributor.SERVER.noArg(),
                                    new DeclineDiplomatPacket(be.getBlockPos(), req.getId()));
                            return true;
                        }
                    }
                }
            }
        }

        // Towns tab: diplomat item selection — click a surplus item to send diplomat
        if (currentTab == Tab.TOWNS && selectingDiplomatItem && hoveredSurplusRow >= 0) {
            int idx = townContentScroll + hoveredSurplusRow;
            if (idx < currentSurplusList.size()) {
                TradingPostBlockEntity be = menu.getBlockEntity();
                if (be != null) {
                    List<TownData> allTowns = new ArrayList<>(TownRegistry.getAllTowns());
                    if (townViewPage < allTowns.size()) {
                        TownData town = allTowns.get(townViewPage);
                        ResourceLocation itemId = currentSurplusList.get(idx);
                        ModNetwork.CHANNEL.send(PacketDistributor.SERVER.noArg(),
                                new SendDiplomatPacket(be.getBlockPos(), town.getId(), itemId, diplomatQuantity));
                        selectingDiplomatItem = false;
                        hoveredSurplusRow = -1;
                        updateButtonVisibility();
                        return true;
                    }
                }
            }
        }
        
        // Towns tab: diplomat quantity +/- buttons
        if (currentTab == Tab.TOWNS && selectingDiplomatItem) {
            double localX = mouseX - this.leftPos;
            double localY = mouseY - this.topPos;
            // Quantity row is at rightX (128) + 5px for "Qty: " offset, y=134
            // "Qty: [-] X [+]" — [-] at ~30px, [+] at ~50px from rightX
            if (localY >= 132 && localY <= 144) {
                // [-] button area (around "[-]" text)
                if (localX >= 153 && localX <= 168) {
                    if (diplomatQuantity > 1) diplomatQuantity--;
                    return true;
                }
                // [+] button area (around "[+]" text)
                if (localX >= 178 && localX <= 193) {
                    if (diplomatQuantity < 64) diplomatQuantity++;
                    return true;
                }
            }
        }

        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double delta) {
        if (currentTab == Tab.ACTIVITY && !showingEconomyDashboard) {
            TradingPostBlockEntity be = menu.getBlockEntity();
            if (be != null) {
                int totalActivity = be.getActiveShipments().size() + be.getActiveBuyOrders().size();
                int maxScroll = Math.max(0, totalActivity - VISIBLE_ACTIVITY);
                if (delta > 0 && activityScrollOffset > 0) {
                    activityScrollOffset--;
                    return true;
                } else if (delta < 0 && activityScrollOffset < maxScroll) {
                    activityScrollOffset++;
                    return true;
                }
            }
        } else if (currentTab == Tab.TOWNS) {
            // Right page area (x >= leftPos + 130): scroll content
            if (mouseX >= this.leftPos + 130) {
                if (delta > 0 && townContentScroll > 0) {
                    townContentScroll--;
                    return true;
                } else if (delta < 0) {
                    townContentScroll++;
                    return true;
                }
            } else {
                // Left page area: page between towns
                List<TownData> allTowns = new ArrayList<>(TownRegistry.getAllTowns());
                int maxPage = Math.max(0, allTowns.size() - 1);
                if (delta > 0 && townViewPage > 0) {
                    townViewPage--;
                    townContentScroll = 0;
                    return true;
                } else if (delta < 0 && townViewPage < maxPage) {
                    townViewPage++;
                    townContentScroll = 0;
                    return true;
                }
            }
        } else if (currentTab == Tab.QUESTS) {
            TradingPostBlockEntity be = menu.getBlockEntity();
            if (be != null) {
                int maxScroll = Math.max(0, be.getActiveQuests().size() - VISIBLE_QUESTS);
                if (delta > 0 && questScrollOffset > 0) {
                    questScrollOffset--;
                    return true;
                } else if (delta < 0 && questScrollOffset < maxScroll) {
                    questScrollOffset++;
                    return true;
                }
            }
        } else if (currentTab == Tab.DIPLOMAT) {
            TradingPostBlockEntity be = menu.getBlockEntity();
            if (be != null) {
                int maxScroll = Math.max(0, be.getActiveDiplomatRequests().size() - VISIBLE_DIPLOMAT);
                if (delta > 0 && diplomatScrollOffset > 0) {
                    diplomatScrollOffset--;
                    return true;
                } else if (delta < 0 && diplomatScrollOffset < maxScroll) {
                    diplomatScrollOffset++;
                    return true;
                }
            }
        }
        return super.mouseScrolled(mouseX, mouseY, delta);
    }
}
