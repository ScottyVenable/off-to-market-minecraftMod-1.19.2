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
import net.minecraft.client.gui.components.EditBox;
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

    // Tab layout (6 tabs across 376px — 60px each with 1px gap)
    private static final int TAB_Y = 18;
    private static final int TAB_H = 14;
    private static final int TAB_W = 60;
    private static final int TAB_COUNT = 6;
    private static final int[] TAB_X = {8, 69, 130, 191, 252, 313};

    private static final int CONTENT_TOP = 32;
    private static final int CONTENT_H = 112;

    // ==================== State ====================

    private int selectedTownIndex = 0;
    private int activityScrollOffset = 0;
    private boolean showingEconomyDashboard = false;
    private int townViewPage = 0;
    private int townContentScroll = 0; // scroll offset for town needs/surplus content
    private int townListScroll = 0;    // scroll offset for town list (left page)
    private int questScrollOffset = 0;
    private int diplomatScrollOffset = 0;
    private static final int VISIBLE_ACTIVITY = 7;
    private static final int VISIBLE_QUESTS = 6;
    private static final int VISIBLE_DIPLOMAT = 6;

    // Hover tracking
    private int hoveredActivityRow = -1;
    private int hoveredQuestRow = -1;
    private int hoveredDiplomatRow = -1;

    private int hoveredTownRow = -1; // hover index for town list on left page

    // Request creation mode (Requests tab)
    private boolean creatingRequest = false;
    private EditBox requestSearchBox;
    private List<ResourceLocation> requestFilteredItems = new ArrayList<>();
    private int requestListScroll = 0;
    private int hoveredRequestItem = -1;
    private ResourceLocation requestSelectedItem = null;
    private int requestQuantity = 1;
    private static final int VISIBLE_REQUEST_ITEMS = 6;

    // Shipment cancel confirmation
    private UUID confirmCancelShipmentId = null; // shipment ID awaiting cancel confirmation

    // ==================== Buttons ====================

    // Trade tab
    private Button prevTownBtn, nextTownBtn, sendBtn, collectBtn;
    // Activity tab (shipments + orders)
    private Button activityScrollUpBtn, activityScrollDownBtn;
    private Button economyToggleBtn;
    // Towns tab
    // sendDiplomatBtn/cancelDiplomatBtn removed — request creation moved to Requests tab
    // Quests tab
    private Button questScrollUpBtn, questScrollDownBtn;
    // Workers tab
    private Button hireNegotiatorBtn, hireCartBtn, hireBookkeeperBtn;
    private Button fireWorkerBtn;
    private int selectedWorkerIndex = 0; // 0=Negotiator, 1=Trading Cart, 2=Bookkeeper
    // Diplomat tab
    private Button diplomatScrollUpBtn, diplomatScrollDownBtn;
    private Button newRequestBtn, sendRequestBtn, cancelRequestBtn;

    public TradingPostScreen(TradingPostMenu menu, Inventory inv, Component title) {
        super(menu, inv, title);
        this.imageWidth = 384;
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

        nextTownBtn = addRenderableWidget(new Button(x + 363, y + 38, 14, 14,
                Component.literal(">"), btn -> {
            List<TownData> towns = getAvailableTowns();
            if (!towns.isEmpty()) {
                selectedTownIndex = (selectedTownIndex + 1) % towns.size();
                selectTown(towns.get(selectedTownIndex));
            }
        }));

        sendBtn = addRenderableWidget(new Button(x + 8, y + 80, 182, 16,
                Component.literal("Send to Market"), btn -> {
            TradingPostBlockEntity tbe = menu.getBlockEntity();
            if (tbe != null) {
                ModNetwork.CHANNEL.send(PacketDistributor.SERVER.noArg(),
                        new SendShipmentPacket(tbe.getBlockPos()));
            }
        }));

        collectBtn = addRenderableWidget(new Button(x + 194, y + 80, 182, 16,
                Component.literal("Collect Coins"), btn -> {
            TradingPostBlockEntity tbe = menu.getBlockEntity();
            if (tbe != null) {
                ModNetwork.CHANNEL.send(PacketDistributor.SERVER.noArg(),
                        new CollectCoinsPacket(tbe.getBlockPos()));
            }
        }));

        // ==== Activity tab buttons (unified shipments + orders) ====

        activityScrollUpBtn = addRenderableWidget(new Button(x + 364, y + 47, 14, 14,
                Component.literal("\u25B2"), btn -> {
            if (activityScrollOffset > 0) activityScrollOffset--;
        }));

        activityScrollDownBtn = addRenderableWidget(new Button(x + 364, y + 127, 14, 14,
                Component.literal("\u25BC"), btn -> activityScrollOffset++));

        economyToggleBtn = addRenderableWidget(new Button(x + 303, y + 33, 60, 12,
                Component.literal("\uD83D\uDCCA Stats"), btn -> {
            showingEconomyDashboard = !showingEconomyDashboard;
            btn.setMessage(Component.literal(showingEconomyDashboard ? "\u25C0 Activity" : "\uD83D\uDCCA Stats"));
            updateButtonVisibility();
        }));

        // Towns tab: diplomat selection buttons removed — request creation moved to Requests tab

        // ==== Quests tab buttons ====

        questScrollUpBtn = addRenderableWidget(new Button(x + 364, y + 47, 14, 14,
                Component.literal("\u25B2"), btn -> {
            if (questScrollOffset > 0) questScrollOffset--;
        }));

        questScrollDownBtn = addRenderableWidget(new Button(x + 364, y + 127, 14, 14,
                Component.literal("\u25BC"), btn -> questScrollOffset++));

        // ==== Workers tab buttons ====

        hireNegotiatorBtn = addRenderableWidget(new Button(x + 156, y + 128, 110, 14,
                Component.literal("Hire Negotiator"), btn -> {
            TradingPostBlockEntity tbe = menu.getBlockEntity();
            if (tbe != null) {
                ModNetwork.CHANNEL.send(PacketDistributor.SERVER.noArg(),
                        new HireWorkerPacket(tbe.getBlockPos(),
                                com.offtomarket.mod.data.Worker.WorkerType.NEGOTIATOR));
            }
        }));

        hireCartBtn = addRenderableWidget(new Button(x + 156, y + 128, 110, 14,
                Component.literal("Hire Trading Cart"), btn -> {
            TradingPostBlockEntity tbe = menu.getBlockEntity();
            if (tbe != null) {
                ModNetwork.CHANNEL.send(PacketDistributor.SERVER.noArg(),
                        new HireWorkerPacket(tbe.getBlockPos(),
                                com.offtomarket.mod.data.Worker.WorkerType.TRADING_CART));
            }
        }));

        hireBookkeeperBtn = addRenderableWidget(new Button(x + 156, y + 128, 110, 14,
                Component.literal("Hire Bookkeeper"), btn -> {
            TradingPostBlockEntity tbe = menu.getBlockEntity();
            if (tbe != null) {
                ModNetwork.CHANNEL.send(PacketDistributor.SERVER.noArg(),
                        new HireWorkerPacket(tbe.getBlockPos(),
                                com.offtomarket.mod.data.Worker.WorkerType.BOOKKEEPER));
            }
        }));

        fireWorkerBtn = addRenderableWidget(new Button(x + 272, y + 128, 100, 14,
                Component.literal("\u2716 Dismiss"), btn -> {
            TradingPostBlockEntity tbe = menu.getBlockEntity();
            if (tbe != null) {
                com.offtomarket.mod.data.Worker.WorkerType[] types = com.offtomarket.mod.data.Worker.WorkerType.values();
                if (selectedWorkerIndex >= 0 && selectedWorkerIndex < types.length) {
                    ModNetwork.CHANNEL.send(PacketDistributor.SERVER.noArg(),
                            new com.offtomarket.mod.network.FireWorkerPacket(tbe.getBlockPos(), types[selectedWorkerIndex]));
                }
            }
        }));

        // ==== Diplomat tab buttons ====

        diplomatScrollUpBtn = addRenderableWidget(new Button(x + 364, y + 47, 14, 14,
                Component.literal("\u25B2"), btn -> {
            if (diplomatScrollOffset > 0) diplomatScrollOffset--;
        }));

        diplomatScrollDownBtn = addRenderableWidget(new Button(x + 364, y + 127, 14, 14,
                Component.literal("\u25BC"), btn -> diplomatScrollOffset++));

        // "New Request" button — opens request creation mode on Requests tab
        newRequestBtn = addRenderableWidget(new Button(x + 298, y + 33, 65, 12,
                Component.literal("+ New Request"), btn -> {
            creatingRequest = true;
            requestSelectedItem = null;
            requestQuantity = 1;
            requestListScroll = 0;
            hoveredRequestItem = -1;
            requestSearchBox.setValue("");
            requestSearchBox.setFocus(true);
            updateRequestFilteredItems();
            updateButtonVisibility();
        }));

        // "Send" button — confirms and sends the new request
        sendRequestBtn = addRenderableWidget(new Button(x + 5, y + 127, 80, 12,
                Component.literal("\u2714 Send Request"), btn -> {
            if (requestSelectedItem != null) {
                TradingPostBlockEntity tpbe = menu.getBlockEntity();
                if (tpbe != null) {
                    ModNetwork.CHANNEL.send(PacketDistributor.SERVER.noArg(),
                            new CreateRequestPacket(tpbe.getBlockPos(), requestSelectedItem, requestQuantity));
                    creatingRequest = false;
                    requestSelectedItem = null;
                    updateButtonVisibility();
                }
            }
        }));

        // "Cancel" button — exits request creation mode
        cancelRequestBtn = addRenderableWidget(new Button(x + 90, y + 127, 60, 12,
                Component.literal("\u2718 Cancel"), btn -> {
            creatingRequest = false;
            requestSelectedItem = null;
            requestSearchBox.setValue("");
            updateButtonVisibility();
        }));

        // Search box for request creation (custom wood-themed background drawn in renderDiplomatBg)
        requestSearchBox = new EditBox(this.font, x + 7, y + 50, 256, 10,
                Component.literal("Search items..."));
        requestSearchBox.setMaxLength(50);
        requestSearchBox.setBordered(false);
        requestSearchBox.setTextColor(0xFFEEDDCC);
        requestSearchBox.setVisible(false);
        requestSearchBox.setResponder(text -> updateRequestFilteredItems());
        addRenderableWidget(requestSearchBox);

        updateButtonVisibility();
    }

    private void switchTab(Tab tab) {
        if (currentTab != tab) {
            SoundHelper.playTabSwitch();
        }
        currentTab = tab;
        creatingRequest = false;
        showingEconomyDashboard = false;
        confirmCancelShipmentId = null; // Clear cancel confirmation on tab switch
        if (economyToggleBtn != null) {
            economyToggleBtn.setMessage(Component.literal("\uD83D\uDCCA Stats"));
        }
        updateButtonVisibility();
    }

    private void updateButtonVisibility() {
        boolean trade = currentTab == Tab.TRADE;
        boolean activity = currentTab == Tab.ACTIVITY;
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


        // Towns tab diplomat buttons removed — request creation moved to Requests tab

        questScrollUpBtn.visible = quests;
        questScrollDownBtn.visible = quests;

        TradingPostBlockEntity be = menu.getBlockEntity();
        // Workers tab: show hire button for selected unhired worker, fire button for selected hired worker
        com.offtomarket.mod.data.Worker selectedWorker = null;
        if (be != null) {
            com.offtomarket.mod.data.Worker.WorkerType[] wTypes = com.offtomarket.mod.data.Worker.WorkerType.values();
            if (selectedWorkerIndex >= 0 && selectedWorkerIndex < wTypes.length) {
                selectedWorker = be.getWorker(wTypes[selectedWorkerIndex]);
            }
        }
        boolean selHired = selectedWorker != null && selectedWorker.isHired();
        boolean selNotHired = selectedWorker != null && !selectedWorker.isHired();
        hireNegotiatorBtn.visible = workers && selNotHired && selectedWorkerIndex == 0;
        hireCartBtn.visible = workers && selNotHired && selectedWorkerIndex == 1;
        hireBookkeeperBtn.visible = workers && selNotHired && selectedWorkerIndex == 2;
        fireWorkerBtn.visible = workers && selHired;

        diplomatScrollUpBtn.visible = diplomat && !creatingRequest;
        diplomatScrollDownBtn.visible = diplomat && !creatingRequest;
        newRequestBtn.visible = diplomat && !creatingRequest;
        sendRequestBtn.visible = diplomat && creatingRequest && requestSelectedItem != null;
        cancelRequestBtn.visible = diplomat && creatingRequest;
        if (requestSearchBox != null) {
            requestSearchBox.setVisible(diplomat && creatingRequest);
            requestSearchBox.active = diplomat && creatingRequest;
        }
    }

    /**
     * Update the filtered item list based on the search box text.
     * Searches all registered items by display name.
     */
    private void updateRequestFilteredItems() {
        requestFilteredItems.clear();
        requestListScroll = 0;
        hoveredRequestItem = -1;

        String query = requestSearchBox != null ? requestSearchBox.getValue().toLowerCase().trim() : "";
        if (query.isEmpty()) return; // don't show anything with empty search

        for (Item item : ForgeRegistries.ITEMS) {
            ResourceLocation rl = ForgeRegistries.ITEMS.getKey(item);
            if (rl == null) continue;
            String name = new ItemStack(item).getHoverName().getString().toLowerCase();
            if (name.contains(query)) {
                requestFilteredItems.add(rl);
                if (requestFilteredItems.size() >= 100) break; // cap results
            }
        }
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
        drawPanel(poseStack, x, y, 384, 230);

        // Title bar
        drawInsetPanel(poseStack, x + 4, y + 3, 376, 14);

        // Content area
        drawInsetPanel(poseStack, x + 4, y + CONTENT_TOP, 376, CONTENT_H);

        // Tab bar (6 tabs)
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
            case WORKERS -> renderWorkersBg(poseStack, x, y, mouseX, mouseY);
            case DIPLOMAT -> renderDiplomatBg(poseStack, x, y, mouseX, mouseY);
        }

        // Divider above inventory
        fill(poseStack, x + 4, y + 145, x + 380, y + 146, 0xFF3B2E1E);

        // Player inventory slot backgrounds
        for (int row = 0; row < 3; row++) {
            for (int col = 0; col < 9; col++) {
                drawSlot(poseStack, x + 111 + col * 18, y + 148 + row * 18);
            }
        }
        for (int col = 0; col < 9; col++) {
            drawSlot(poseStack, x + 111 + col * 18, y + 206);
        }
    }

    private void renderTradeBg(PoseStack ps, int x, int y, int mouseX, int mouseY) {
        // Town selector sub-panel
        drawInsetPanel(ps, x + 6, y + 35, 372, 22);
        // Stats sub-panel
        drawInsetPanel(ps, x + 6, y + 60, 372, 14);

        // XP progress bar
        int level = menu.getTraderLevel();
        int xp = menu.getTraderXp();
        int xpNeeded = DebugConfig.getBaseXpToLevel() * Math.max(1, level);
        float xpFraction = Math.min(1.0f, xp / (float) xpNeeded);
        int barX = x + 100;
        int barY = y + 63;
        int barW = 160;
        fill(ps, barX - 1, barY - 1, barX + barW + 1, barY + 9, 0xFF1A1209);
        fill(ps, barX, barY, barX + barW, barY + 8, 0xFF2A2A2A);
        if (xpFraction > 0) {
            int fillW = Math.max(1, (int) (barW * xpFraction));
            fill(ps, barX, barY, barX + fillW, barY + 8, 0xFF55CC55);
            // Highlight sheen on top edge
            fill(ps, barX, barY, barX + fillW, barY + 1, 0xFF77EE77);
        }

        // Overview panel below buttons
        drawInsetPanel(ps, x + 6, y + 98, 372, 44);
        // Header separator
        fill(ps, x + 12, y + 109, x + 372, y + 110, 0xFF4A3D2B);
        // Column divider
        fill(ps, x + 194, y + 111, x + 195, y + 140, 0xFF4A3D2B);
    }

    // ==================== Activity Tab (Unified Shipments + Orders) ====================

    /** Tracks what type each visible activity row is: true = shipment, false = order */
    private boolean[] activityRowIsShipment = new boolean[VISIBLE_ACTIVITY];
    /** Maps visible row index to underlying data index */
    private int[] activityRowDataIndex = new int[VISIBLE_ACTIVITY];

    private void renderActivityBg(PoseStack ps, int x, int y, int mouseX, int mouseY) {
        if (showingEconomyDashboard) {
            hoveredActivityRow = -1; // Reset so stale hover can't trigger click actions
            return; // Dashboard draws its own content
        }

        // Header row background
        fill(ps, x + 5, y + 46, x + 361, y + 56, 0xFF2C2318);

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
            if (validRow && mouseX >= x + 5 && mouseX <= x + 361
                    && mouseY >= rowY && mouseY < rowY + 12) {
                hoveredActivityRow = i;
                isHovered = true;
            }

            if (isHovered) {
                fill(ps, x + 5, rowY, x + 361, rowY + 12, 0xFF5A4A30);
                fill(ps, x + 5, rowY, x + 6, rowY + 12, 0xFFFFD700);
            } else {
                int rowColor = (i % 2 == 0) ? 0xFF4A3D2B : 0xFF3E3226;
                fill(ps, x + 5, rowY, x + 361, rowY + 12, rowColor);
            }
        }
    }

    private void renderTownsBg(PoseStack ps, int x, int y) {
        // Book-style page panel
        // Left page (parchment) — town list
        fill(ps, x + 6, y + 35, x + 186, y + 140, 0xFFD8C8A0);
        fill(ps, x + 7, y + 36, x + 185, y + 139, 0xFFF0E6C8);

        // Header underline
        fill(ps, x + 10, y + 48, x + 182, y + 49, 0xFFBBA878);

        // Town list row backgrounds (selection/hover highlights)
        List<TownData> allTowns = new ArrayList<>(TownRegistry.getAllTowns());
        int visibleTowns = 8;
        int rowH = 11;
        int listStartY = y + 50;
        for (int i = 0; i < visibleTowns; i++) {
            int townIdx = townListScroll + i;
            if (townIdx >= allTowns.size()) break;
            int rowY = listStartY + i * rowH;
            if (townIdx == townViewPage) {
                // Selected — dark bg with gold left accent
                fill(ps, x + 8, rowY, x + 184, rowY + rowH, 0xFF8B7355);
                fill(ps, x + 8, rowY, x + 10, rowY + rowH, 0xFFFFD700);
            } else if (i == hoveredTownRow) {
                // Hovered
                fill(ps, x + 8, rowY, x + 184, rowY + rowH, 0xFFBBA878);
            }
        }

        // Center spine
        fill(ps, x + 186, y + 35, x + 190, y + 140, 0xFF6B5A3E);
        // Right page (parchment) — detail view
        fill(ps, x + 190, y + 35, x + 378, y + 140, 0xFFD8C8A0);
        fill(ps, x + 191, y + 36, x + 377, y + 139, 0xFFF0E6C8);

        // Right page section dividers
        fill(ps, x + 196, y + 57, x + 372, y + 58, 0xFFBBA878); // below name/meta
        fill(ps, x + 196, y + 77, x + 372, y + 78, 0xFFBBA878); // below req/rep

        // Reputation mini-bar background (rendered in Bg so labels draw over it)
        TradingPostBlockEntity be = menu.getBlockEntity();
        if (be != null && !allTowns.isEmpty()) {
            int tvp = Math.min(townViewPage, allTowns.size() - 1);
            TownData town = allTowns.get(tvp);
            boolean unlocked = town.getMinTraderLevel() <= menu.getTraderLevel();
            if (unlocked) {
                int rep = be.getReputation(town.getId());
                int repBarX = x + 260;
                int repBarY = y + 70;
                int repBarW = 60;
                int repBarH = 5;
                // Border + bg
                fill(ps, repBarX - 1, repBarY - 1, repBarX + repBarW + 1, repBarY + repBarH + 1, 0xFFBBA878);
                fill(ps, repBarX, repBarY, repBarX + repBarW, repBarY + repBarH, 0xFFD8C8A0);
                // Fill (clamp at 200 = Exalted)
                int maxRep = 200;
                int fillW = Math.max(0, Math.min(repBarW, (int)(repBarW * (rep / (float) maxRep))));
                if (fillW > 0) {
                    int repColor = TradingPostBlockEntity.getReputationColor(rep);
                    fill(ps, repBarX, repBarY, repBarX + fillW, repBarY + repBarH, repColor | 0xFF000000);
                }
            }
        }
    }

    /**
     * Compute hover row for the Towns tab left-page town list.
     */
    private void updateTownsDiplomatHover(int x, int y, int mouseX, int mouseY) {
        // Town list hover (left page)
        hoveredTownRow = -1;
        {
            int leftPageLeft = x + 8;
            int leftPageRight = x + 184;
            int listStartY = y + 50;
            int rowH = 11;
            int visibleTowns = 8;
            List<TownData> allTowns = new ArrayList<>(TownRegistry.getAllTowns());
            int itemCount = Math.min(visibleTowns, allTowns.size() - townListScroll);
            if (mouseX >= leftPageLeft && mouseX <= leftPageRight
                    && mouseY >= listStartY && mouseY < listStartY + itemCount * rowH) {
                int row = (mouseY - listStartY) / rowH;
                int townIdx = townListScroll + row;
                // Don't hover on the already-selected town
                if (townIdx != townViewPage) {
                    hoveredTownRow = row;
                }
            }
        }
    }

    private void renderQuestsBg(PoseStack ps, int x, int y, int mouseX, int mouseY) {
        // Header row
        fill(ps, x + 5, y + 46, x + 361, y + 56, 0xFF2C2318);

        // Quest rows with hover
        hoveredQuestRow = -1;
        TradingPostBlockEntity be = menu.getBlockEntity();
        int totalQuests = be != null ? be.getActiveQuests().size() : 0;

        for (int i = 0; i < VISIBLE_QUESTS; i++) {
            int rowY = y + 57 + i * 14;
            int qIdx = questScrollOffset + i;
            boolean validRow = qIdx < totalQuests;

            boolean isHovered = false;
            if (validRow && mouseX >= x + 5 && mouseX <= x + 361
                    && mouseY >= rowY && mouseY < rowY + 14) {
                hoveredQuestRow = i;
                isHovered = true;
            }

            if (isHovered) {
                fill(ps, x + 5, rowY, x + 361, rowY + 14, 0xFF5A4A30);
                fill(ps, x + 5, rowY, x + 6, rowY + 14, 0xFFFFD700);
            } else {
                int rowColor = (i % 2 == 0) ? 0xFF4A3D2B : 0xFF3E3226;
                fill(ps, x + 5, rowY, x + 361, rowY + 14, rowColor);
            }
        }
    }

    private int hoveredWorkerRow = -1;

    private void renderWorkersBg(PoseStack ps, int x, int y, int mouseX, int mouseY) {
        // Left panel: worker list (140px wide)
        drawInsetPanel(ps, x + 6, y + 35, 140, 106);

        // Worker list rows (3 workers, 30px each)
        hoveredWorkerRow = -1;
        com.offtomarket.mod.data.Worker.WorkerType[] wTypes = com.offtomarket.mod.data.Worker.WorkerType.values();
        TradingPostBlockEntity be = menu.getBlockEntity();
        for (int i = 0; i < wTypes.length; i++) {
            int rowY = y + 38 + i * 32;
            boolean isSelected = (i == selectedWorkerIndex);
            boolean isHovered = mouseX >= x + 8 && mouseX <= x + 144
                    && mouseY >= rowY && mouseY < rowY + 30;

            if (isHovered) hoveredWorkerRow = i;

            if (isSelected) {
                fill(ps, x + 8, rowY, x + 144, rowY + 30, 0xFF5A4A30);
                fill(ps, x + 8, rowY, x + 10, rowY + 30, 0xFFFFD700); // gold left accent
            } else if (isHovered) {
                fill(ps, x + 8, rowY, x + 144, rowY + 30, 0xFF4A3D2B);
            }

            // Divider between rows
            if (i > 0) {
                fill(ps, x + 12, rowY - 1, x + 140, rowY, 0xFF2A1F14);
            }

            // Small status indicator (colored dot)
            if (be != null) {
                com.offtomarket.mod.data.Worker w = be.getWorker(wTypes[i]);
                int dotColor = w.isHired() ? 0xFF55CC55 : 0xFF666666;
                fill(ps, x + 14, rowY + 12, x + 18, rowY + 16, dotColor);
            }
        }

        // Right panel: worker detail (224px wide)
        drawInsetPanel(ps, x + 152, y + 35, 226, 106);

        // Divider below detail header
        fill(ps, x + 156, y + 55, x + 374, y + 56, 0xFF2A1F14);
    }

    private void renderDiplomatBg(PoseStack ps, int x, int y, int mouseX, int mouseY) {
        if (creatingRequest) {
            // Request creation mode backgrounds
            hoveredDiplomatRow = -1; // Reset so stale hover can't trigger diplomat actions
            hoveredRequestItem = -1;
            if (requestSelectedItem == null) {
                // Wood-themed search box background (replaces default black EditBox border)
                drawInsetPanel(ps, x + 5, y + 48, 260, 14);

                // Search results list background
                for (int i = 0; i < VISIBLE_REQUEST_ITEMS; i++) {
                    int rowY = y + 64 + i * 10;
                    int itemIdx = requestListScroll + i;
                    boolean validRow = itemIdx < requestFilteredItems.size();

                    boolean isHovered = false;
                    if (validRow && mouseX >= x + 5 && mouseX <= x + 270
                            && mouseY >= rowY && mouseY < rowY + 10) {
                        hoveredRequestItem = i;
                        isHovered = true;
                    }

                    if (isHovered) {
                        fill(ps, x + 5, rowY, x + 270, rowY + 10, 0xFF5A4A30);
                        fill(ps, x + 5, rowY, x + 6, rowY + 10, 0xFFFFD700);
                    } else if (validRow) {
                        int rowColor = (i % 2 == 0) ? 0xFF4A3D2B : 0xFF3E3226;
                        fill(ps, x + 5, rowY, x + 270, rowY + 10, rowColor);
                    }
                }
            } else {
                // Selected item confirmation panel
                drawInsetPanel(ps, x + 5, y + 48, 370, 75);
            }
            return;
        }

        // Normal diplomat list — header row
        fill(ps, x + 5, y + 46, x + 361, y + 56, 0xFF2C2318);

        // Diplomat request rows with hover
        hoveredDiplomatRow = -1;
        TradingPostBlockEntity be = menu.getBlockEntity();
        int totalReqs = be != null ? be.getActiveDiplomatRequests().size() : 0;

        for (int i = 0; i < VISIBLE_DIPLOMAT; i++) {
            int rowY = y + 57 + i * 14;
            int dIdx = diplomatScrollOffset + i;
            boolean validRow = dIdx < totalReqs;

            boolean isHovered = false;
            if (validRow && mouseX >= x + 5 && mouseX <= x + 361
                    && mouseY >= rowY && mouseY < rowY + 14) {
                hoveredDiplomatRow = i;
                isHovered = true;
            }

            if (isHovered) {
                fill(ps, x + 5, rowY, x + 361, rowY + 14, 0xFF5A4A30);
                fill(ps, x + 5, rowY, x + 6, rowY + 14, 0xFFFFD700);
            } else {
                int rowColor = (i % 2 == 0) ? 0xFF4A3D2B : 0xFF3E3226;
                fill(ps, x + 5, rowY, x + 361, rowY + 14, rowColor);
            }
        }
    }

    // ==================== Render Labels ====================

    @Override
    protected void renderLabels(PoseStack poseStack, int mouseX, int mouseY) {
        // Title
        drawCenteredString(poseStack, this.font, "Trading Post", 192, 6, 0xFFD700);

        // Tab labels (6 tabs)
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
        this.font.draw(poseStack, this.playerInventoryTitle, 111, 138, 0x404040);
    }

    // ==================== Trade Tab ====================

    private void renderTradeLabels(PoseStack poseStack) {
        TradingPostBlockEntity be = menu.getBlockEntity();
        List<TownData> towns = getAvailableTowns();

        if (!towns.isEmpty() && selectedTownIndex < towns.size()) {
            TownData town = towns.get(selectedTownIndex);

            // Town name centered
            String townName = town.getDisplayName();
            int nameW = this.font.width(townName);
            this.font.draw(poseStack, townName, (384 - nameW) / 2.0f, 38, 0xFFFFFF);

            // Type + distance + reputation
            String baseStr = town.getType().getDisplayName() + "  |  Dist: " + town.getDistance();
            if (be != null) {
                int rep = be.getReputation(town.getId());
                String repLevel = TradingPostBlockEntity.getReputationLevel(rep);
                int repColor = TradingPostBlockEntity.getReputationColor(rep);
                String sep = "  |  ";
                String repStr = "\u2605 " + repLevel;
                int totalW = this.font.width(baseStr + sep + repStr);
                float startX = (384 - totalW) / 2.0f;
                this.font.draw(poseStack, baseStr + sep, startX, 48, 0xAAAAAA);
                this.font.draw(poseStack, repStr, startX + this.font.width(baseStr + sep), 48, repColor);
            } else {
                int typeW = this.font.width(baseStr);
                this.font.draw(poseStack, baseStr, (384 - typeW) / 2.0f, 48, 0xAAAAAA);
            }
        } else {
            drawCenteredString(poseStack, this.font, "No towns available", 192, 42, 0x888888);
        }

        // Stats row
        int level = menu.getTraderLevel();
        int xp = menu.getTraderXp();
        int xpNeeded = DebugConfig.getBaseXpToLevel() * Math.max(1, level);
        this.font.draw(poseStack, "Lvl " + level, 10, 63, 0xFFD700);
        String xpText = xp + " / " + xpNeeded;
        int xpW = this.font.width(xpText);
        this.font.draw(poseStack, xpText, 100 + (160 - xpW) / 2.0f, 63, 0xCCCCCC);

        int coins = menu.getPendingCoins();
        int coinW = CoinRenderer.getCoinValueWidth(this.font, coins);
        int labelW = this.font.width("Coins: ");
        this.font.draw(poseStack, "Coins: ", 374 - coinW - labelW, 63, 0xFFD700);
        CoinRenderer.renderCoinValue(poseStack, this.font, 374 - coinW, 63, coins);

        // Quick Overview panel
        drawCenteredString(poseStack, this.font, "Overview", 192, 100, 0xBEA876);

        if (be != null) {
            int ships = be.getActiveShipments().size();
            int orders = be.getActiveBuyOrders().size();
            int reqs = be.getActiveDiplomatRequests().size();
            int sent = be.getTotalShipmentsSent();
            long earned = be.getLifetimeEarnings();

            // Left column — active counts
            this.font.draw(poseStack, "\u2022 " + ships + " Shipment" + (ships != 1 ? "s" : ""),
                    12, 112, ships > 0 ? 0x88BBFF : 0x666666);
            this.font.draw(poseStack, "\u2022 " + orders + " Order" + (orders != 1 ? "s" : ""),
                    12, 122, orders > 0 ? 0xFFCC44 : 0x666666);
            this.font.draw(poseStack, "\u2022 " + reqs + " Request" + (reqs != 1 ? "s" : ""),
                    12, 132, reqs > 0 ? 0xDD88FF : 0x666666);

            // Right column — lifetime stats
            this.font.draw(poseStack, "Total Sent: " + sent, 200, 112, 0x999999);
            this.font.draw(poseStack, "Towns: " + towns.size(), 200, 122, 0x999999);
            this.font.draw(poseStack, "Earned: " + formatCoinText((int) Math.min(earned, Integer.MAX_VALUE)),
                    200, 132, 0xBEA876);
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
            this.font.draw(poseStack, scrollInfo, 358 - scrollW, 36, 0x666666);
        }

        // Column headers
        this.font.draw(poseStack, "Type", 8, 48, 0xFFD700);
        this.font.draw(poseStack, "Details", 40, 48, 0xFFD700);
        this.font.draw(poseStack, "Status", 260, 48, 0xFFD700);

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
                    // Show sold count if any items have sold
                    int soldCount = 0;
                    int totalCount = s.getItems().size();
                    for (Shipment.ShipmentItem si : s.getItems()) {
                        if (si.isSold()) soldCount++;
                    }
                    if (soldCount > 0) {
                        status = soldCount + "/" + totalCount + " (" + ticksToTime(remaining) + ")";
                    } else {
                        status = "Market (" + ticksToTime(remaining) + ")";
                    }
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
                    if (s.getTotalEarnings() > 0) {
                        status = "\u2714 Collect All";
                    } else {
                        status = "\u2714 Collect Items";
                    }
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
        int available = 260 - 42 - 4;
        if (this.font.width(destDisplay) > available) {
            while (this.font.width(destDisplay + "..") > available && destDisplay.length() > 3) {
                destDisplay = destDisplay.substring(0, destDisplay.length() - 1);
            }
            destDisplay += "..";
        }

        this.font.draw(ps, destDisplay, 40, yOff, 0xCCCCCC);
        this.font.draw(ps, status, 358 - statusW, yOff, statusColor);

        // Progress bar for IN_TRANSIT shipments
        if (s.getStatus() == Shipment.Status.IN_TRANSIT) {
            long totalTravel = s.getArrivalTime() - s.getDepartureTime();
            long elapsed = be.getLevel().getGameTime() - s.getDepartureTime();
            float progress = totalTravel > 0 ? Math.min(1.0f, (float) elapsed / totalTravel) : 1.0f;
            int barX = 40;
            int barY2 = yOff + 9;
            int barW = 316;
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
        int available = 260 - 42 - 4;
        if (this.font.width(itemName) > available) {
            while (this.font.width(itemName + "..") > available && itemName.length() > 3) {
                itemName = itemName.substring(0, itemName.length() - 1);
            }
            itemName += "..";
        }

        this.font.draw(ps, itemName, 40, yOff, 0xCCCCCC);
        this.font.draw(ps, status, 358 - statusW, yOff, statusColor);

        // Progress bar for IN_TRANSIT orders
        if (order.getStatus() == BuyOrder.Status.IN_TRANSIT) {
            long totalTravel = order.getArrivalTime() - order.getOrderTime();
            long elapsed = be.getLevel().getGameTime() - order.getOrderTime();
            float progress = totalTravel > 0 ? Math.min(1.0f, (float) elapsed / totalTravel) : 1.0f;
            int barX = 40;
            int barY2 = yOff + 9;
            int barW = 316;
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
            this.font.draw(ps, "Top Items", 230, 48, 0xFFD700);
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
                this.font.draw(ps, name, 230, itemY, 0xCCCCCC);
                CoinRenderer.renderCoinValue(ps, this.font, 295, itemY, (int) Math.min(entry.getValue(), Integer.MAX_VALUE));
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
                CoinRenderer.renderCoinValue(ps, this.font, 130, drawY, (int) Math.min(entry.getValue(), Integer.MAX_VALUE));

                // Show shipment count from history for this town
                if (totalShips > 0) {
                    long townAvg = entry.getValue() / Math.max(1, countShipmentsForTown(be, entry.getKey()));
                    this.font.draw(ps, "avg:", 190, drawY, 0x888888);
                    CoinRenderer.renderCoinValue(ps, this.font, 215, drawY, (int) Math.min(townAvg, Integer.MAX_VALUE));
                }
                drawY += 9;
            }
        }

        // Supply/Demand Trends section
        Collection<TownData> allTowns = TownRegistry.getAllTowns();
        boolean hasTrends = false;
        for (TownData t : allTowns) {
            if (!t.getSupplyLevels().isEmpty() && !t.getPreviousSupplyLevels().isEmpty()) {
                hasTrends = true;
                break;
            }
        }
        if (hasTrends) {
            drawY += 6;
            this.font.draw(ps, "Market Trends", 8, drawY, 0xFFD700);
            drawY += 10;
            for (TownData t : allTowns) {
                Map<String, Integer> supplies = t.getSupplyLevels();
                if (supplies.isEmpty()) continue;
                // Count rising/falling/stable items
                int rising = 0, falling = 0;
                for (String key : supplies.keySet()) {
                    TownData.SupplyTrend trend = t.getTrend(key);
                    if (trend == TownData.SupplyTrend.RISING) rising++;
                    else if (trend == TownData.SupplyTrend.FALLING) falling++;
                }
                if (rising == 0 && falling == 0) continue; // all stable, skip

                String tName = t.getDisplayName();
                if (this.font.width(tName) > 60) {
                    while (this.font.width(tName + "..") > 60 && tName.length() > 2) {
                        tName = tName.substring(0, tName.length() - 1);
                    }
                    tName += "..";
                }
                this.font.draw(ps, tName, 10, drawY, 0xCCCCCC);
                // Show trend counts
                if (falling > 0) {
                    this.font.draw(ps, "\u25B2" + falling, 130, drawY, 0x44CC44); // demand rising
                }
                if (rising > 0) {
                    this.font.draw(ps, "\u25BC" + rising, 165, drawY, 0xCC4444); // demand falling
                }
                drawY += 9;
                if (drawY > 165) break; // don't overflow
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

        int traderLevel = menu.getTraderLevel();
        TradingPostBlockEntity be = menu.getBlockEntity();

        // Clamp selections
        townViewPage = Math.min(townViewPage, allTowns.size() - 1);
        int maxListScroll = Math.max(0, allTowns.size() - 8);
        townListScroll = Math.min(townListScroll, maxListScroll);

        // ========== LEFT PAGE: Scrollable town list ==========
        int leftX = 12;
        int rightX = 196;

        // Header with count
        this.font.draw(poseStack, "Towns", leftX, 38, 0x3B2A14);
        int unlockCount = 0;
        for (TownData t : allTowns) {
            if (t.getMinTraderLevel() <= traderLevel) unlockCount++;
        }
        String countStr = unlockCount + "/" + allTowns.size();
        int countW = this.font.width(countStr);
        this.font.draw(poseStack, countStr, 180 - countW, 38, 0x8B7355);

        // Town list entries (8 visible rows, 11px each, starting at y=50)
        int visibleTowns = 8;
        int rowH = 11;
        for (int i = 0; i < visibleTowns; i++) {
            int townIdx = townListScroll + i;
            if (townIdx >= allTowns.size()) break;

            TownData t = allTowns.get(townIdx);
            boolean townUnlocked = t.getMinTraderLevel() <= traderLevel;
            boolean isSelected = (townIdx == townViewPage);

            // Distance tag on right side
            String distStr = String.valueOf(t.getDistance());
            int distW = this.font.width(distStr);

            // Truncate name to fit
            String name = t.getDisplayName();
            int maxNameW = 155 - distW;
            if (this.font.width(name) > maxNameW) {
                while (this.font.width(name + "..") > maxNameW && name.length() > 3)
                    name = name.substring(0, name.length() - 1);
                name += "..";
            }

            String prefix = townUnlocked ? "" : "\u2716 ";
            int textColor;
            if (isSelected) {
                textColor = 0xFFF0E6; // bright cream on dark bg
            } else if (!townUnlocked) {
                textColor = 0x664444; // red-gray for locked
            } else {
                textColor = 0x3B2A14; // normal brown
            }

            int drawY = 50 + i * rowH;
            this.font.draw(poseStack, prefix + name, leftX, drawY + 1, textColor);

            // Distance right-aligned in muted color
            int distColor = isSelected ? 0xBBA878 : 0x8B7355;
            this.font.draw(poseStack, distStr, 180 - distW, drawY + 1, distColor);

            // Reputation dot next to name (if unlocked and has rep)
            if (townUnlocked && be != null) {
                int rep = be.getReputation(t.getId());
                if (rep > 0) {
                    int dotColor = TradingPostBlockEntity.getReputationColor(rep);
                    int dotX = leftX + this.font.width(prefix + name) + 2;
                    this.font.draw(poseStack, "\u2022", dotX, drawY + 1, dotColor);
                }
            }
        }

        // Scroll indicators for town list
        if (townListScroll > 0) {
            this.font.draw(poseStack, "\u25B2", 174, 42, 0x6B5A3E);
        }
        if (townListScroll < maxListScroll) {
            this.font.draw(poseStack, "\u25BC", 174, 134, 0x6B5A3E);
        }

        // ========== RIGHT PAGE: Detail view of selected town ==========
        TownData town = allTowns.get(townViewPage);
        boolean unlocked = town.getMinTraderLevel() <= traderLevel;

        // Town name (larger section above first divider, y=38-56)
        if (!unlocked) {
            this.font.draw(poseStack, "\u2716 " + town.getDisplayName(), rightX, 38, 0x664444);
        } else {
            this.font.draw(poseStack, town.getDisplayName(), rightX, 38, 0x3B2A14);
        }

        // Type + distance right of name
        String meta = town.getType().getDisplayName() + " \u00B7 Dist " + town.getDistance();
        this.font.draw(poseStack, meta, rightX, 48, 0x6B5A3E);

        // --- Section below first divider (y=59-76): Level + Reputation ---
        // Level requirement
        String lvlReq = "Req Lvl " + town.getMinTraderLevel();
        int lvlColor = unlocked ? 0x3B7A3B : 0x8B2222;
        this.font.draw(poseStack, lvlReq, rightX, 60, lvlColor);

        // Reputation (text + mini-bar drawn in Bg)
        if (be != null && unlocked) {
            int rep = be.getReputation(town.getId());
            String repLevel = TradingPostBlockEntity.getReputationLevel(rep);
            int repColor = TradingPostBlockEntity.getReputationColor(rep);
            this.font.draw(poseStack, "\u2605 " + repLevel, rightX, 70, repColor);
            // Rep number next to the bar
            this.font.draw(poseStack, String.valueOf(rep), 324, 70, 0x8B7355);
        }

        // --- Section below second divider (y=79+): Description + Specialty + Needs ---
        int sectionY = 80;

        // Description (2 lines max, tight 9px spacing)
        List<FormattedCharSequence> descLines = this.font.split(
                Component.literal(town.getDescription()), 175);
        int descLinesShown = Math.min(descLines.size(), 2);
        for (int i = 0; i < descLinesShown; i++) {
            this.font.draw(poseStack, descLines.get(i), rightX, sectionY + i * 9, 0x5C4A32);
        }
        sectionY += descLinesShown * 9 + 1;

        // Specialty items (gold-highlighted)
        if (!town.getSpecialtyItems().isEmpty()) {
            StringBuilder specStr = new StringBuilder("\u2726 ");
            boolean first = true;
            for (ResourceLocation rl : town.getSpecialtyItems()) {
                Item item = ForgeRegistries.ITEMS.getValue(rl);
                if (item == null) continue;
                String iname = new ItemStack(item).getHoverName().getString();
                if (!first) specStr.append(", ");
                specStr.append(iname);
                first = false;
            }
            String specText = specStr.toString();
            // Truncate if too wide
            if (this.font.width(specText) > 170) {
                while (this.font.width(specText + "..") > 170 && specText.length() > 5)
                    specText = specText.substring(0, specText.length() - 1);
                specText += "..";
            }
            this.font.draw(poseStack, specText, rightX, sectionY, 0xCCAA44);
            sectionY += 9;
        }

        // ---- Needs & Surplus below description ----
        List<String> lines = new ArrayList<>();
        List<Integer> colors = new ArrayList<>();

        List<String[]> desperateItems = new ArrayList<>();
        List<String[]> highNeedItems = new ArrayList<>();
        List<String[]> moderateItems = new ArrayList<>();
        List<String[]> surplusItems = new ArrayList<>();
        List<String[]> oversatItems = new ArrayList<>();

        for (ResourceLocation rl : town.getNeeds()) {
            Item item = ForgeRegistries.ITEMS.getValue(rl);
            if (item == null) continue;
            NeedLevel level = town.getNeedLevel(item);
            String iname = new ItemStack(item).getHoverName().getString();
            if (this.font.width(iname) > 145) {
                while (this.font.width(iname + "..") > 145 && iname.length() > 3)
                    iname = iname.substring(0, iname.length() - 1);
                iname += "..";
            }
            switch (level) {
                case DESPERATE -> desperateItems.add(new String[]{iname});
                case HIGH_NEED -> highNeedItems.add(new String[]{iname});
                case MODERATE_NEED -> moderateItems.add(new String[]{iname});
                default -> highNeedItems.add(new String[]{iname});
            }
        }

        for (ResourceLocation rl : town.getSurplus()) {
            Item item = ForgeRegistries.ITEMS.getValue(rl);
            if (item == null) continue;
            NeedLevel level = town.getNeedLevel(item);
            String iname = new ItemStack(item).getHoverName().getString();
            if (this.font.width(iname) > 145) {
                while (this.font.width(iname + "..") > 145 && iname.length() > 3)
                    iname = iname.substring(0, iname.length() - 1);
                iname += "..";
            }
            switch (level) {
                case OVERSATURATED -> oversatItems.add(new String[]{iname});
                case SURPLUS -> surplusItems.add(new String[]{iname});
                default -> surplusItems.add(new String[]{iname});
            }
        }

        if (!desperateItems.isEmpty()) {
            lines.add("\u2757 Desperate:");
            colors.add(NeedLevel.DESPERATE.getColor());
            for (String[] e : desperateItems) { lines.add("  \u2022 " + e[0]); colors.add(NeedLevel.DESPERATE.getColor()); }
        }
        if (!highNeedItems.isEmpty()) {
            if (!lines.isEmpty()) { lines.add(""); colors.add(0); }
            lines.add("\u26A0 High Need:");
            colors.add(NeedLevel.HIGH_NEED.getColor());
            for (String[] e : highNeedItems) { lines.add("  \u2022 " + e[0]); colors.add(NeedLevel.HIGH_NEED.getColor()); }
        }
        if (!moderateItems.isEmpty()) {
            if (!lines.isEmpty()) { lines.add(""); colors.add(0); }
            lines.add("Moderate:");
            colors.add(NeedLevel.MODERATE_NEED.getColor());
            for (String[] e : moderateItems) { lines.add("  \u2022 " + e[0]); colors.add(NeedLevel.MODERATE_NEED.getColor()); }
        }
        if (!surplusItems.isEmpty() || !oversatItems.isEmpty()) {
            if (!lines.isEmpty()) { lines.add(""); colors.add(0); }
            lines.add("\u25BC Surplus:");
            colors.add(NeedLevel.SURPLUS.getColor());
            for (String[] e : surplusItems) { lines.add("  \u2022 " + e[0]); colors.add(NeedLevel.SURPLUS.getColor()); }
            for (String[] e : oversatItems) { lines.add("  \u2022 " + e[0] + " \u2716"); colors.add(NeedLevel.OVERSATURATED.getColor()); }
        }

        // Scrollable needs/surplus area — dynamic start Y, fit remaining space
        int needsStartY = sectionY;
        int availableH = 138 - needsStartY; // bottom of right page at y~138
        int visibleLines = Math.max(2, availableH / 9);
        int maxScrollR = Math.max(0, lines.size() - visibleLines);
        townContentScroll = Math.min(townContentScroll, maxScrollR);

        int drawY = needsStartY;
        for (int li = townContentScroll; li < lines.size() && (li - townContentScroll) < visibleLines; li++) {
            this.font.draw(poseStack, lines.get(li), rightX, drawY, colors.get(li));
            drawY += 9;
        }

        // Scroll indicators for needs/surplus
        if (townContentScroll > 0) {
            this.font.draw(poseStack, "\u25B2", rightX + 170, needsStartY, 0x6B5A3E);
        }
        if (townContentScroll < maxScrollR) {
            this.font.draw(poseStack, "\u25BC", rightX + 170, needsStartY + (visibleLines - 1) * 9, 0x6B5A3E);
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
            this.font.draw(poseStack, scrollInfo, 358 - scrollW, 36, 0x666666);
        }

        // Column headers
        this.font.draw(poseStack, "Type", 8, 48, 0xFFD700);
        this.font.draw(poseStack, "Item", 60, 48, 0xFFD700);
        this.font.draw(poseStack, "Qty", 170, 48, 0xFFD700);
        this.font.draw(poseStack, "Reward", 210, 48, 0xFFD700);
        this.font.draw(poseStack, "Status", 290, 48, 0xFFD700);

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
            if (this.font.width(itemName) > 100) {
                while (this.font.width(itemName + "..") > 100 && itemName.length() > 3) {
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
                case DELIVERING -> {
                    long remaining = quest.getRewardTicksRemaining(be.getLevel().getGameTime());
                    status = "\u21E8 " + ticksToTime(remaining);
                    statusColor = 0xFFAA44;
                }
                case COMPLETED -> {
                    status = "\u2714 Done";
                    statusColor = 0x55FF55;
                }
                default -> {
                    status = "Exp";
                    statusColor = 0x884444;
                }
            }

            this.font.draw(poseStack, itemName, 60, yOff, 0xCCCCCC);
            this.font.draw(poseStack, qty, 170, yOff, 0xAAAAAA);
            this.font.draw(poseStack, reward, 210, yOff, 0xFFD700);
            this.font.draw(poseStack, status, 290, yOff, statusColor);

            yOff += 14;
        }
    }

    // ==================== Workers Tab ====================

    private void renderWorkersLabels(PoseStack poseStack) {
        TradingPostBlockEntity be = menu.getBlockEntity();
        if (be == null) return;

        // Left panel: Worker list
        com.offtomarket.mod.data.Worker.WorkerType[] wTypes = com.offtomarket.mod.data.Worker.WorkerType.values();
        for (int i = 0; i < wTypes.length; i++) {
            com.offtomarket.mod.data.Worker w = be.getWorker(wTypes[i]);
            int rowY = 38 + i * 32; // match bg rows at y+38+i*32 absolute → label y = 38+i*32
            int textColor = (i == selectedWorkerIndex) ? 0xFFD700 : 0xCCCCCC;

            // Worker symbol + name
            String symbol = w.getType().getSymbol();
            this.font.draw(poseStack, symbol, 22, rowY, textColor);
            this.font.draw(poseStack, w.getType().getDisplayName(), 33, rowY, textColor);

            // Status line
            if (w.isHired()) {
                String title = w.getLevelTitle();
                this.font.draw(poseStack, "Lv" + w.getLevel() + " " + title, 22, rowY + 10, w.getLevelTitleColor());
                // Mini XP bar
                int barX = 22;
                int barY = rowY + 20;
                int barW = 112;
                fill(poseStack, barX, barY, barX + barW, barY + 3, 0xFF2A2A2A);
                if (w.getLevel() < com.offtomarket.mod.data.Worker.getMaxLevel()) {
                    float frac = Math.min(1.0f, w.getXp() / (float) w.getXpForNextLevel());
                    if (frac > 0) {
                        fill(poseStack, barX, barY, barX + Math.max(1, (int)(barW * frac)), barY + 3, 0xFF55CC55);
                    }
                } else {
                    fill(poseStack, barX, barY, barX + barW, barY + 3, 0xFFFFD700); // full gold at max
                }
            } else {
                this.font.draw(poseStack, "Not Hired", 22, rowY + 10, 0x666666);
            }
        }

        // Right panel: Selected worker detail
        if (selectedWorkerIndex < 0 || selectedWorkerIndex >= wTypes.length) return;
        com.offtomarket.mod.data.Worker worker = be.getWorker(wTypes[selectedWorkerIndex]);
        renderWorkerDetail(poseStack, 156, 36, worker);
    }

    private void renderWorkerDetail(PoseStack poseStack, int px, int py, com.offtomarket.mod.data.Worker worker) {
        // Header: Symbol + Name + Title
        String header = worker.getType().getSymbol() + " " + worker.getType().getDisplayName();
        this.font.draw(poseStack, header, px, py, 0xFFD700);

        if (!worker.isHired()) {
            // Not hired: show description and hire cost
            List<FormattedCharSequence> descLines = this.font.split(
                    Component.literal(worker.getType().getDescription()), 210);
            for (int i = 0; i < descLines.size(); i++) {
                this.font.draw(poseStack, descLines.get(i), px, py + 12 + i * 10, 0x999999);
            }
            this.font.draw(poseStack, "Hire Cost: " + formatCoinText(worker.getHireCost()), px, py + 36, 0xFFAA44);

            // Show what this worker does at each level
            this.font.draw(poseStack, "Perks:", px, py + 48, 0x888888);
            int[] milestones = {3, 6, 9};
            for (int i = 0; i < milestones.length; i++) {
                String perkName = worker.getPerkName(milestones[i]);
                if (perkName != null) {
                    this.font.draw(poseStack, "Lv" + milestones[i] + ": " + perkName, px + 4, py + 57 + i * 10, 0x777777);
                }
            }
            return;
        }

        // Hired: Title line
        String titleLine = worker.getLevelTitle();
        int titleW = this.font.width(titleLine);
        this.font.draw(poseStack, titleLine, px + 218 - titleW, py, worker.getLevelTitleColor());

        // Level + XP bar
        int maxLvl = com.offtomarket.mod.data.Worker.getMaxLevel();
        boolean isMax = worker.getLevel() >= maxLvl;
        String lvlText = "Level " + worker.getLevel() + (isMax ? " (MAX)" : "");
        this.font.draw(poseStack, lvlText, px, py + 12, 0xCCCCCC);

        if (!isMax) {
            int xpNeeded = worker.getXpForNextLevel();
            int barX = px;
            int barY = py + 22;
            int barW = 218;
            fill(poseStack, barX - 1, barY - 1, barX + barW + 1, barY + 7, 0xFF1A1209);
            fill(poseStack, barX, barY, barX + barW, barY + 6, 0xFF2A2A2A);
            float frac = Math.min(1.0f, worker.getXp() / (float) xpNeeded);
            if (frac > 0) {
                int fillW = Math.max(1, (int)(barW * frac));
                fill(poseStack, barX, barY, barX + fillW, barY + 6, 0xFF55CC55);
                fill(poseStack, barX, barY, barX + fillW, barY + 1, 0xFF77DD77);
            }
            String xpText = worker.getXp() + "/" + xpNeeded;
            int xpW = this.font.width(xpText);
            this.font.draw(poseStack, xpText, px + (barW - xpW) / 2, barY - 1, 0xCCCCCC);
        } else {
            int barX = px;
            int barY = py + 22;
            int barW = 218;
            fill(poseStack, barX - 1, barY - 1, barX + barW + 1, barY + 7, 0xFF1A1209);
            fill(poseStack, barX, barY, barX + barW, barY + 6, 0xFFCCAA33);
            fill(poseStack, barX, barY, barX + barW, barY + 1, 0xFFFFD700);
            String maxText = "\u2605 GRANDMASTER \u2605";
            if (worker.getLevel() < 10) maxText = "MAX LEVEL";
            int mW = this.font.width(maxText);
            this.font.draw(poseStack, maxText, px + (barW - mW) / 2, barY - 1, 0xFFD700);
        }

        // Current bonus + next level preview
        this.font.draw(poseStack, worker.getBonusDisplay(), px, py + 32, 0x88CC88);
        if (!isMax) {
            String nextPreview = "Next: " + worker.getNextLevelBonusPreview();
            this.font.draw(poseStack, nextPreview, px + 120, py + 32, 0x666666);
        }

        // Stats row
        this.font.draw(poseStack, "Cost/trip: " + formatCoinText(worker.getPerTripCost()), px, py + 42, 0xAAAAAA);
        this.font.draw(poseStack, "Trips: " + worker.getTotalTrips(), px + 120, py + 42, 0x888888);

        // Perks section (compact: 8px per line)
        int perkY = py + 52;
        this.font.draw(poseStack, "Perks:", px, perkY, 0xBBAAAA);
        int[] milestones = {3, 6, 9};
        for (int i = 0; i < milestones.length; i++) {
            String perkName = worker.getPerkName(milestones[i]);
            if (perkName == null) continue;
            boolean unlocked = worker.hasPerk(milestones[i]);
            int perkColor = unlocked ? 0x88CC88 : 0x555555;
            String indicator = unlocked ? "\u2714 " : "\u2022 ";
            this.font.draw(poseStack, indicator + perkName + " (Lv" + milestones[i] + ")",
                    px + 4, perkY + 9 + i * 8, perkColor);
        }

        // Lifetime stats
        if (worker.getLifetimeBonusValue() > 0) {
            String lifetimeLabel = worker.getLifetimeBonusDisplay() + ": ";
            String lifetimeValue;
            if (worker.getType() == com.offtomarket.mod.data.Worker.WorkerType.TRADING_CART) {
                long ticks = worker.getLifetimeBonusValue();
                long seconds = ticks / 20;
                if (seconds >= 60) {
                    lifetimeValue = (seconds / 60) + "m " + (seconds % 60) + "s";
                } else {
                    lifetimeValue = seconds + "s";
                }
            } else {
                lifetimeValue = formatCoinText((int) Math.min(worker.getLifetimeBonusValue(), Integer.MAX_VALUE));
            }
            this.font.draw(poseStack, lifetimeLabel + lifetimeValue, px, perkY + 34, 0x776655);
        }
    }

    // ==================== Diplomat Tab ====================

    private void renderDiplomatLabels(PoseStack poseStack) {
        if (creatingRequest) {
            renderRequestCreationLabels(poseStack);
            return;
        }

        this.font.draw(poseStack, "Trade Requests", 8, 36, 0xFFD700);

        TradingPostBlockEntity be = menu.getBlockEntity();
        if (be == null) return;

        List<DiplomatRequest> requests = be.getActiveDiplomatRequests();
        int maxScroll = Math.max(0, requests.size() - VISIBLE_DIPLOMAT);
        diplomatScrollOffset = Math.min(diplomatScrollOffset, maxScroll);

        if (requests.isEmpty()) {
            this.font.draw(poseStack, "No active requests.", 10, 60, 0x888888);
            this.font.draw(poseStack, "Click '+ New Request' to", 10, 72, 0x888888);
            this.font.draw(poseStack, "request items from towns.", 10, 84, 0x888888);
            return;
        }

        // Count indicator
        if (requests.size() > VISIBLE_DIPLOMAT) {
            String scrollInfo = (diplomatScrollOffset + 1) + "-" +
                    Math.min(diplomatScrollOffset + VISIBLE_DIPLOMAT, requests.size()) +
                    " of " + requests.size();
            int scrollW = this.font.width(scrollInfo);
            this.font.draw(poseStack, scrollInfo, 358 - scrollW, 36, 0x666666);
        }

        // Column headers
        this.font.draw(poseStack, "Item", 8, 48, 0xFFD700);
        this.font.draw(poseStack, "Town", 120, 48, 0xFFD700);
        this.font.draw(poseStack, "Status", 210, 48, 0xFFD700);

        int yOff = 59;
        int displayed = 0;
        for (int i = diplomatScrollOffset; i < requests.size() && displayed < VISIBLE_DIPLOMAT; i++, displayed++) {
            DiplomatRequest req = requests.get(i);

            // Item name with count
            String itemName = req.getItemDisplayName();
            if (req.getRequestedCount() > 1) itemName += " x" + req.getRequestedCount();
            if (this.font.width(itemName) > 105) {
                while (this.font.width(itemName + "..") > 105 && itemName.length() > 3) {
                    itemName = itemName.substring(0, itemName.length() - 1);
                }
                itemName += "..";
            }

            // Town name (truncated)
            TownData town = TownRegistry.getTown(req.getTownId());
            String townName = town != null ? town.getDisplayName() : req.getTownId();
            if (this.font.width(townName) > 85) {
                while (this.font.width(townName + "..") > 85 && townName.length() > 3) {
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
            this.font.draw(poseStack, townName, 120, yOff, 0xAAAAAA);
            
            // For DISCUSSING status, show price and Accept/Decline buttons
            if (req.getStatus() == DiplomatRequest.Status.DISCUSSING) {
                this.font.draw(poseStack, status, 210, yOff, statusColor);
                // Accept button (green checkmark)
                this.font.draw(poseStack, "\u2714", 280, yOff, 0x55FF55);
                // Decline button (red X)
                this.font.draw(poseStack, "\u2718", 300, yOff, 0xFF5555);
            } else if (showProgress) {
                // Draw progress bar for traveling/waiting stages
                int barX = 210;
                int barY = yOff;
                int barW = 100;
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
                this.font.draw(poseStack, timeStr, 348 - timeW, yOff, 0x888888);
            } else {
                int statusW = this.font.width(status);
                this.font.draw(poseStack, status, 358 - statusW, yOff, statusColor);
            }

            yOff += 14;
        }
    }

    /**
     * Render the request creation mode UI.
     * Two sub-states: searching (no item selected) and confirming (item selected).
     */
    private void renderRequestCreationLabels(PoseStack poseStack) {
        this.font.draw(poseStack, "New Request", 8, 36, 0xFFD700);

        if (requestSelectedItem == null) {
            // ---- Searching state: search box + filtered item list ----
            // Search box renders itself as a widget (EditBox)

            // Filtered item list below search box
            int maxScroll = Math.max(0, requestFilteredItems.size() - VISIBLE_REQUEST_ITEMS);
            requestListScroll = Math.min(requestListScroll, maxScroll);

            if (requestFilteredItems.isEmpty() && requestSearchBox != null
                    && !requestSearchBox.getValue().isEmpty()) {
                this.font.draw(poseStack, "No items found.", 8, 66, 0x888888);
            } else if (requestFilteredItems.isEmpty()) {
                this.font.draw(poseStack, "Type to search for items...", 8, 66, 0x888888);
            }

            for (int i = 0; i < VISIBLE_REQUEST_ITEMS; i++) {
                int itemIdx = requestListScroll + i;
                if (itemIdx >= requestFilteredItems.size()) break;

                ResourceLocation rl = requestFilteredItems.get(itemIdx);
                Item item = ForgeRegistries.ITEMS.getValue(rl);
                if (item == null) continue;

                String name = new ItemStack(item).getHoverName().getString();
                if (this.font.width(name) > 260) {
                    while (this.font.width(name + "..") > 260 && name.length() > 3) {
                        name = name.substring(0, name.length() - 1);
                    }
                    name += "..";
                }

                int rowY = 65 + i * 10;
                int color = (hoveredRequestItem == i) ? 0xFFFFDD : 0xCCCCCC;
                this.font.draw(poseStack, name, 8, rowY, color);
            }

            // Scroll indicators
            if (requestListScroll > 0) {
                this.font.draw(poseStack, "\u25B2", 268, 64, 0x888888);
            }
            if (requestListScroll < maxScroll) {
                this.font.draw(poseStack, "\u25BC", 268, 65 + (VISIBLE_REQUEST_ITEMS - 1) * 10, 0x888888);
            }

            // Result count
            if (!requestFilteredItems.isEmpty()) {
                String count = requestFilteredItems.size() + " results";
                int cw = this.font.width(count);
                this.font.draw(poseStack, count, 358 - cw, 36, 0x666666);
            }
        } else {
            // ---- Confirmation state: selected item + quantity + cost estimate ----
            Item item = ForgeRegistries.ITEMS.getValue(requestSelectedItem);
            if (item == null) return;

            String itemName = new ItemStack(item).getHoverName().getString();

            // Item name (large, gold)
            this.font.draw(poseStack, itemName, 10, 52, 0xFFD700);

            // Quantity selector: "Qty:  [-]  N  [+]"
            this.font.draw(poseStack, "Qty:", 10, 68, 0xAAAAAA);
            this.font.draw(poseStack, "[-]", 32, 68, 0xFF8888);
            this.font.draw(poseStack, String.valueOf(requestQuantity), 50, 68, 0xFFFFFF);
            this.font.draw(poseStack, "[+]", 64, 68, 0x88FF88);

            // Estimate cost and find best town (client-side preview)
            int basePrice = PriceCalculator.getBaseValue(new ItemStack(item));
            int traderLevel = menu.getTraderLevel();
            List<TownData> towns = TownRegistry.getAvailableTowns(Math.max(1, traderLevel));

            TownData bestTown = null;
            int bestScore = 0;
            for (TownData t : towns) {
                if (t.getMinTraderLevel() > traderLevel) continue;
                int score = DiplomatRequest.getSupplyScore(t, requestSelectedItem);
                if (score > bestScore || (score == bestScore && bestTown != null
                        && t.getDistance() < bestTown.getDistance())) {
                    bestScore = score;
                    bestTown = t;
                }
            }

            if (bestTown != null) {
                // Town info
                this.font.draw(poseStack, "Best Town: " + bestTown.getDisplayName(), 10, 82, 0xAAAAAA);

                // Estimated cost
                double premium = DiplomatRequest.getScoreBasedPremium(bestScore, bestTown);
                int estCostPerUnit = Math.max(1, (int) (basePrice * premium));
                int estTotal = estCostPerUnit * requestQuantity;
                this.font.draw(poseStack, "Est. Cost: " + formatCoinText(estTotal), 10, 94, 0xCCBB88);
                if (requestQuantity > 1) {
                    this.font.draw(poseStack, "(" + formatCoinText(estCostPerUnit) + " each)", 10, 104, 0x888888);
                }

                // Fulfillment chance
                int chance = (int) (DiplomatRequest.getFulfillmentChance(bestScore) * 100);
                int chanceColor = chance >= 80 ? 0x55FF55 : chance >= 50 ? 0xFFCC44 : 0xFF6644;
                this.font.draw(poseStack, "Fulfillment: " + chance + "%", 10, 114, chanceColor);
            } else {
                this.font.draw(poseStack, "No town available!", 10, 82, 0xFF4444);
            }

            // Hint
            this.font.draw(poseStack, "(Click item name to change)", 280, 52, 0x555555);
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
            return "\u00A7e" + gp + "g\u00A7r";
        }
        int gp = copperPieces / 100;
        int sp = (copperPieces % 100) / 10;
        int cp = copperPieces % 10;
        StringBuilder sb = new StringBuilder();
        if (gp > 0) sb.append("\u00A7e").append(gp).append("g\u00A7r");
        if (sp > 0) { if (sb.length() > 0) sb.append(" "); sb.append("\u00A77").append(sp).append("s\u00A7r"); }
        if (cp > 0 || sb.length() == 0) { if (sb.length() > 0) sb.append(" "); sb.append("\u00A76").append(cp).append("c\u00A7r"); }
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

        // Cost breakdown tooltip on hover (Request creation with item selected)
        if (currentTab == Tab.DIPLOMAT && creatingRequest && requestSelectedItem != null) {
            int costY = topPos + 94;
            int costX = leftPos + 10;
            if (mouseX >= costX && mouseX <= costX + 200 && mouseY >= costY && mouseY < costY + 10) {
                renderCostBreakdownTooltip(poseStack, mouseX, mouseY);
            }
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
        } else if (quest.getStatus() == Quest.Status.DELIVERING) {
            tooltip.add(Component.literal(""));
            long remaining = quest.getRewardTicksRemaining(be.getLevel().getGameTime());
            tooltip.add(Component.literal("\u21E8 Rewards en route: " + ticksToTime(remaining))
                    .withStyle(ChatFormatting.GOLD));
            tooltip.add(Component.literal("Rewards will arrive automatically.")
                    .withStyle(ChatFormatting.GRAY));
        } else if (quest.getStatus() == Quest.Status.COMPLETED) {
            tooltip.add(Component.literal(""));
            tooltip.add(Component.literal("\u2714 Quest completed! Rewards collected.")
                    .withStyle(ChatFormatting.GREEN, ChatFormatting.BOLD));
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

    private void renderCostBreakdownTooltip(PoseStack ps, int mouseX, int mouseY) {
        if (requestSelectedItem == null) return;
        Item item = ForgeRegistries.ITEMS.getValue(requestSelectedItem);
        if (item == null) return;

        int basePrice = PriceCalculator.getBaseValue(new ItemStack(item));
        int traderLevel = menu.getTraderLevel();
        List<TownData> towns = TownRegistry.getAvailableTowns(Math.max(1, traderLevel));

        TownData bestTown = null;
        int bestScore = 0;
        for (TownData t : towns) {
            if (t.getMinTraderLevel() > traderLevel) continue;
            int score = DiplomatRequest.getSupplyScore(t, requestSelectedItem);
            if (score > bestScore || (score == bestScore && bestTown != null
                    && t.getDistance() < bestTown.getDistance())) {
                bestScore = score;
                bestTown = t;
            }
        }
        if (bestTown == null) return;

        double typeMult = DiplomatRequest.getDiplomatPremiumMultiplier(bestTown);
        double difficultyMult;
        String supplyLabel;
        if (bestScore >= 90) {
            difficultyMult = 1.0;
            supplyLabel = "Surplus";
        } else if (bestScore >= 70) {
            difficultyMult = 1.1;
            supplyLabel = "Specialty";
        } else if (bestScore >= 40) {
            difficultyMult = 1.4;
            supplyLabel = "Neutral";
        } else {
            difficultyMult = 1.8;
            supplyLabel = "High Demand";
        }

        double totalMult = typeMult * difficultyMult;
        int estPerUnit = Math.max(1, (int) (basePrice * totalMult));
        int estTotal = estPerUnit * requestQuantity;

        List<Component> tooltip = new ArrayList<>();
        tooltip.add(Component.literal("Cost Breakdown")
                .withStyle(ChatFormatting.GOLD, ChatFormatting.BOLD));
        tooltip.add(Component.literal(""));
        tooltip.add(Component.literal("Base Value: " + formatCoinText(basePrice))
                .withStyle(ChatFormatting.GRAY));
        tooltip.add(Component.literal("Town Type (" + bestTown.getType().getDisplayName() + "): \u00D7"
                + String.format("%.2f", typeMult))
                .withStyle(ChatFormatting.GRAY));
        tooltip.add(Component.literal("Supply (" + supplyLabel + "): \u00D7"
                + String.format("%.1f", difficultyMult))
                .withStyle(ChatFormatting.GRAY));
        tooltip.add(Component.literal(""));
        tooltip.add(Component.literal("Per Unit: " + formatCoinText(estPerUnit))
                .withStyle(ChatFormatting.WHITE));
        if (requestQuantity > 1) {
            tooltip.add(Component.literal("\u00D7 " + requestQuantity + " = " + formatCoinText(estTotal))
                    .withStyle(ChatFormatting.YELLOW));
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

        // Diplomat tab: request creation mode click handling
        if (currentTab == Tab.DIPLOMAT && creatingRequest && button == 0) {
            double localX = mouseX - x;
            double localY = mouseY - y;

            if (requestSelectedItem == null) {
                // Searching state: click on a filtered item to select it
                if (hoveredRequestItem >= 0) {
                    int itemIdx = requestListScroll + hoveredRequestItem;
                    if (itemIdx < requestFilteredItems.size()) {
                        requestSelectedItem = requestFilteredItems.get(itemIdx);
                        requestQuantity = 1;
                        updateButtonVisibility();
                        SoundHelper.playUIClick();
                        return true;
                    }
                }
            } else {
                // Confirmation state: quantity +/- or click item name to change
                // "[-]" at localX ~32-44, localY ~68-78
                if (localY >= 66 && localY <= 78) {
                    if (localX >= 30 && localX <= 46) {
                        // [-] button
                        if (requestQuantity > 1) requestQuantity--;
                        return true;
                    }
                    if (localX >= 62 && localX <= 78) {
                        // [+] button
                        if (requestQuantity < 64) requestQuantity++;
                        return true;
                    }
                }
                // Click on item name area (localY ~50-60) to go back to search
                if (localY >= 50 && localY <= 60 && localX >= 8 && localX <= 358) {
                    requestSelectedItem = null;
                    requestQuantity = 1;
                    if (requestSearchBox != null) requestSearchBox.setFocus(true);
                    updateButtonVisibility();
                    return true;
                }
            }

            // Let the EditBox handle its own clicks
            if (requestSearchBox != null && requestSelectedItem == null) {
                requestSearchBox.mouseClicked(mouseX, mouseY, button);
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
                        if (localX >= 278 && localX <= 290) {
                            // Accept button clicked
                            ModNetwork.CHANNEL.send(PacketDistributor.SERVER.noArg(),
                                    new AcceptDiplomatPacket(be.getBlockPos(), req.getId()));
                            return true;
                        } else if (localX >= 298 && localX <= 310) {
                            // Decline button clicked
                            ModNetwork.CHANNEL.send(PacketDistributor.SERVER.noArg(),
                                    new DeclineDiplomatPacket(be.getBlockPos(), req.getId()));
                            return true;
                        }
                    }
                }
            }
        }

        // Towns tab: click town in list to select it (left page)
        if (currentTab == Tab.TOWNS && hoveredTownRow >= 0) {
            int townIdx = townListScroll + hoveredTownRow;
            List<TownData> allTowns = new ArrayList<>(TownRegistry.getAllTowns());
            if (townIdx < allTowns.size() && townIdx != townViewPage) {
                townViewPage = townIdx;
                townContentScroll = 0; // reset right page scroll
                updateButtonVisibility();
                return true;
            }
        }

        // Workers tab: click worker in list to select it
        if (currentTab == Tab.WORKERS && button == 0) {
            double lx = mouseX - x;
            double ly = mouseY - y;
            // Left list panel: x+6 to x+146, rows at y+38, y+70, y+102 (each 32px)
            if (lx >= 6 && lx <= 146) {
                for (int i = 0; i < 3; i++) {
                    int rowTop = 38 + i * 32;
                    if (ly >= rowTop && ly < rowTop + 32) {
                        if (selectedWorkerIndex != i) {
                            selectedWorkerIndex = i;
                            updateButtonVisibility();
                            SoundHelper.playUIClick();
                        }
                        return true;
                    }
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
            // Right page area (x >= leftPos + 190): scroll needs/surplus content
            if (mouseX >= this.leftPos + 190) {
                if (delta > 0 && townContentScroll > 0) {
                    townContentScroll--;
                    return true;
                } else if (delta < 0) {
                    townContentScroll++;
                    return true;
                }
            } else {
                // Left page area: scroll town list
                List<TownData> allTowns = new ArrayList<>(TownRegistry.getAllTowns());
                int maxScroll = Math.max(0, allTowns.size() - 8);
                if (delta > 0 && townListScroll > 0) {
                    townListScroll--;
                    return true;
                } else if (delta < 0 && townListScroll < maxScroll) {
                    townListScroll++;
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
            if (creatingRequest && requestSelectedItem == null) {
                // Scroll the filtered item list
                int maxScroll = Math.max(0, requestFilteredItems.size() - VISIBLE_REQUEST_ITEMS);
                if (delta > 0 && requestListScroll > 0) {
                    requestListScroll--;
                    return true;
                } else if (delta < 0 && requestListScroll < maxScroll) {
                    requestListScroll++;
                    return true;
                }
            } else if (!creatingRequest) {
                // Scroll the normal diplomat request list
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
        }
        return super.mouseScrolled(mouseX, mouseY, delta);
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (creatingRequest && requestSearchBox != null && requestSelectedItem == null) {
            if (requestSearchBox.keyPressed(keyCode, scanCode, modifiers)) {
                return true;
            }
            // Don't let Escape close the screen if we're in creation mode — exit creation instead
            if (keyCode == 256) { // GLFW_KEY_ESCAPE
                creatingRequest = false;
                requestSelectedItem = null;
                requestSearchBox.setValue("");
                updateButtonVisibility();
                return true;
            }
            // Prevent 'E' (inventory key) from closing screen while typing
            if (requestSearchBox.isFocused()) {
                return true;
            }
        }
        return super.keyPressed(keyCode, scanCode, modifiers);
    }

    @Override
    public boolean charTyped(char codePoint, int modifiers) {
        if (creatingRequest && requestSearchBox != null && requestSelectedItem == null) {
            if (requestSearchBox.charTyped(codePoint, modifiers)) {
                return true;
            }
        }
        return super.charTyped(codePoint, modifiers);
    }
}
