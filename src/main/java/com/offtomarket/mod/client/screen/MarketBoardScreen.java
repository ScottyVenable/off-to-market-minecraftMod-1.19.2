package com.offtomarket.mod.client.screen;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;
import com.offtomarket.mod.OffToMarket;
import com.offtomarket.mod.block.FinanceTableBlock;
import com.offtomarket.mod.block.entity.FinanceTableBlockEntity;
import com.offtomarket.mod.block.entity.MarketBoardBlockEntity;
import com.offtomarket.mod.data.MarketListing;
import com.offtomarket.mod.data.NeedLevel;
import com.offtomarket.mod.data.TownData;
import com.offtomarket.mod.data.TownRegistry;
import com.offtomarket.mod.debug.DebugConfig;
import com.offtomarket.mod.item.CoinItem;
import com.offtomarket.mod.menu.MarketBoardMenu;
import com.offtomarket.mod.network.CartCheckoutPacket;
import com.offtomarket.mod.network.ModNetwork;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.ChatFormatting;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.network.PacketDistributor;
import net.minecraftforge.registries.ForgeRegistries;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import net.minecraft.core.BlockPos;

public class MarketBoardScreen extends AbstractContainerScreen<MarketBoardMenu> {
    private static final ResourceLocation TEXTURE =
            new ResourceLocation(OffToMarket.MODID, "textures/gui/market_board.png");

    private int scrollOffset = 0;
    private static final int VISIBLE_LISTINGS = 14;

    /** Persists cart entries across screen close/reopen (client-side, keyed by block position). */
    private static final Map<BlockPos, List<int[]>> SAVED_CARTS = new HashMap<>();

    /** Row index (0-based within visible rows) the mouse is currently hovering, or -1. */
    private int hoveredRow = -1;

    /** Sort state */
    private enum SortMode { NONE, NAME, TOWN, QTY, PRICE }
    private SortMode sortMode = SortMode.NONE;
    private boolean sortAscending = true;

    private final Inventory playerInv;

    // ==================== Cart System ====================

    /** Entry in the shopping cart. */
    private static class CartEntry {
        final int listingIndex;   // index into MarketBoardBlockEntity.getListings()
        int quantity;
        final String itemName;
        final String townId;
        final int pricePerItem;
        final int maxCount;

        CartEntry(int listingIndex, int quantity, String itemName, String townId,
                  int pricePerItem, int maxCount) {
            this.listingIndex = listingIndex;
            this.quantity = quantity;
            this.itemName = itemName;
            this.townId = townId;
            this.pricePerItem = pricePerItem;
            this.maxCount = maxCount;
        }

        int totalCost() { return pricePerItem * quantity; }
    }

    private final List<CartEntry> cart = new ArrayList<>();
    private boolean showingCart = false;
    private int cartScrollOffset = 0;
    private static final int VISIBLE_CART_ROWS = 13;
    private int hoveredCartRow = -1;

    // Quantity selection overlay
    private int selectedListingIndex = -1;  // -1 = no overlay shown
    private int selectedQuantity = 1;
    private int selectedMaxQty = 1;
    private String selectedItemName = "";
    private int selectedPricePerItem = 0;

    // Overlay geometry (relative to leftPos/topPos)
    private static final int OVL_X = 92;
    private static final int OVL_Y = 30;
    private static final int OVL_W = 200;
    private static final int OVL_H = 96;

    // ==================== Buttons ====================

    private Button scrollUpBtn, scrollDownBtn;
    private Button checkoutBtn, clearCartBtn;
    private Button cartScrollUpBtn, cartScrollDownBtn;

    // Quantity overlay buttons (only visible during selection)
    private Button qtyMinusBtn, qtyPlusBtn, qtyMinus10Btn, qtyPlus10Btn;
    private Button addToCartBtn, cancelQtyBtn;

    public MarketBoardScreen(MarketBoardMenu menu, Inventory inv, Component title) {
        super(menu, inv, title);
        this.imageWidth = 384;
        this.imageHeight = 222;
        this.playerInv = inv;
    }

    @Override
    protected void init() {
        super.init();
        int x = this.leftPos;
        int y = this.topPos;

        // ==== Listing view buttons ====

        scrollUpBtn = addRenderableWidget(new Button(x + 366, y + 20, 14, 14,
                Component.literal("^"), btn -> {
            if (scrollOffset > 0) scrollOffset--;
        }));

        scrollDownBtn = addRenderableWidget(new Button(x + 366, y + 183, 14, 14,
                Component.literal("v"), btn -> scrollOffset++));

        // Cart toggle and refresh button replaced by manual drawing and click detection

        // ==== Cart view buttons ====

        checkoutBtn = addRenderableWidget(new Button(x + 8, y + 188, 170, 14,
                Component.literal("Checkout"), btn -> doCheckout()));

        clearCartBtn = addRenderableWidget(new Button(x + 192, y + 188, 170, 14,
                Component.literal("Clear Cart"), btn -> {
            cart.clear();
            showingCart = false;
            updateButtonVisibility();
        }));

        cartScrollUpBtn = addRenderableWidget(new Button(x + 366, y + 20, 14, 14,
                Component.literal("^"), btn -> {
            if (cartScrollOffset > 0) cartScrollOffset--;
        }));

        cartScrollDownBtn = addRenderableWidget(new Button(x + 366, y + 183, 14, 14,
                Component.literal("v"), btn -> cartScrollOffset++));

        // ==== Quantity overlay buttons ====

        int overlayX = x + OVL_X;
        int overlayY = y + OVL_Y;

        qtyMinus10Btn = addRenderableWidget(new Button(overlayX + 12, overlayY + 48, 34, 18,
                Component.literal("-10"), btn -> {
            selectedQuantity = Math.max(1, selectedQuantity - 10);
        }));

        qtyMinusBtn = addRenderableWidget(new Button(overlayX + 50, overlayY + 48, 34, 18,
                Component.literal("-1"), btn -> {
            if (selectedQuantity > 1) selectedQuantity--;
        }));

        qtyPlusBtn = addRenderableWidget(new Button(overlayX + 116, overlayY + 48, 34, 18,
                Component.literal("+1"), btn -> {
            if (selectedQuantity < selectedMaxQty) selectedQuantity++;
        }));

        qtyPlus10Btn = addRenderableWidget(new Button(overlayX + 154, overlayY + 48, 34, 18,
                Component.literal("+10"), btn -> {
            selectedQuantity = Math.min(selectedMaxQty, selectedQuantity + 10);
        }));

        addToCartBtn = addRenderableWidget(new Button(overlayX + 12, overlayY + 72, 84, 18,
                Component.literal("Add to Cart"), btn -> {
            if (selectedListingIndex >= 0) {
                addToCart(selectedListingIndex, selectedQuantity);
                selectedListingIndex = -1;
                updateButtonVisibility();
            }
        }));

        cancelQtyBtn = addRenderableWidget(new Button(overlayX + 104, overlayY + 72, 84, 18,
                Component.literal("Cancel"), btn -> {
            selectedListingIndex = -1;
            updateButtonVisibility();
        }));

        // Restore previously saved cart for this market board
        MarketBoardBlockEntity be = menu.getBlockEntity();
        if (be != null) {
            List<int[]> saved = SAVED_CARTS.get(be.getBlockPos());
            if (saved != null && !saved.isEmpty()) {
                List<MarketListing> listings = be.getListings();
                for (int[] entry : saved) {
                    int li = entry[0], qty = entry[1];
                    if (li >= 0 && li < listings.size()) {
                        MarketListing ml = listings.get(li);
                        cart.add(new CartEntry(li, qty, ml.getItemDisplayName(),
                                ml.getTownId(), ml.getPricePerItem(), ml.getCount()));
                    }
                }
            }
        }

        updateButtonVisibility();
    }

    @Override
    public void onClose() {
        // Persist the current cart so it survives screen close/reopen
        MarketBoardBlockEntity be = menu.getBlockEntity();
        if (be != null) {
            if (cart.isEmpty()) {
                SAVED_CARTS.remove(be.getBlockPos());
            } else {
                List<int[]> toSave = new ArrayList<>();
                for (CartEntry e : cart) toSave.add(new int[]{e.listingIndex, e.quantity});
                SAVED_CARTS.put(be.getBlockPos(), toSave);
            }
        }
        super.onClose();
    }

    private void updateButtonVisibility() {
        boolean listing = !showingCart && selectedListingIndex < 0;
        boolean cartView = showingCart && selectedListingIndex < 0;
        boolean qtyOverlay = selectedListingIndex >= 0;

        scrollUpBtn.visible = listing;
        scrollDownBtn.visible = listing;
        // refreshBtn removed — market board auto-refreshes; timer shown in title bar

        // cartToggleBtn not a widget; visibility tracked via selectedListingIndex check in mouseClicked

        checkoutBtn.visible = cartView && !cart.isEmpty();
        clearCartBtn.visible = cartView && !cart.isEmpty();
        cartScrollUpBtn.visible = cartView;
        cartScrollDownBtn.visible = cartView;

        qtyMinus10Btn.visible = qtyOverlay;
        qtyMinusBtn.visible = qtyOverlay;
        qtyPlusBtn.visible = qtyOverlay;
        qtyPlus10Btn.visible = qtyOverlay;
        addToCartBtn.visible = qtyOverlay;
        cancelQtyBtn.visible = qtyOverlay;
    }

    @Override
    protected void containerTick() {
        super.containerTick();
    }

    // ==================== Cart Logic ====================

    private void addToCart(int listingIndex, int quantity) {
        // Check if already in cart for this listing
        for (CartEntry entry : cart) {
            if (entry.listingIndex == listingIndex) {
                entry.quantity = Math.min(entry.maxCount, entry.quantity + quantity);
                return;
            }
        }

        MarketBoardBlockEntity be = menu.getBlockEntity();
        if (be == null) return;
        List<MarketListing> listings = be.getListings();
        if (listingIndex >= listings.size()) return;

        MarketListing listing = listings.get(listingIndex);
        cart.add(new CartEntry(listingIndex, quantity, listing.getItemDisplayName(),
                listing.getTownId(), listing.getPricePerItem(), listing.getCount()));
    }

    private int getCartTotal() {
        int total = 0;
        for (CartEntry entry : cart) {
            total += entry.totalCost();
        }
        return total;
    }

    private void doCheckout() {
        if (cart.isEmpty()) return;
        MarketBoardBlockEntity be = menu.getBlockEntity();
        if (be == null) return;

        int balance = getPlayerCoinBalance();
        int total = getCartTotal();
        if (balance < total) return;

        // Build packet entries
        List<CartCheckoutPacket.CartEntry> entries = new ArrayList<>();
        for (CartEntry ce : cart) {
            entries.add(new CartCheckoutPacket.CartEntry(ce.listingIndex, ce.quantity));
        }

        ModNetwork.CHANNEL.send(PacketDistributor.SERVER.noArg(),
                new CartCheckoutPacket(be.getBlockPos(), entries));

        cart.clear();
        SAVED_CARTS.remove(be.getBlockPos()); // cart fulfilled, remove persistence
        showingCart = false;
        updateButtonVisibility();
    }

    // ==================== Coin Helpers ====================

    private int getPlayerCoinBalance() {
        int total = 0;
        for (int i = 0; i < playerInv.getContainerSize(); i++) {
            ItemStack stack = playerInv.getItem(i);
            if (stack.getItem() instanceof CoinItem coin) {
                total += coin.getValue() * stack.getCount();
            }
        }
        // Include coin bag contents
        for (int i = 0; i < playerInv.getContainerSize(); i++) {
            ItemStack stack = playerInv.getItem(i);
            if (stack.getItem() instanceof com.offtomarket.mod.item.CoinBagItem) {
                net.minecraft.nbt.CompoundTag tag = stack.getTag();
                if (tag != null && tag.contains("CoinBag")) {
                    net.minecraft.nbt.CompoundTag bagTag = tag.getCompound("CoinBag");
                    total += bagTag.getInt("Gold") * 100
                           + bagTag.getInt("Silver") * 10
                           + bagTag.getInt("Copper");
                }
            }
        }
        // Include coins stored in nearby Finance Tables (within 16 blocks)
        if (minecraft != null && minecraft.level != null && minecraft.player != null) {
            BlockPos playerPos = minecraft.player.blockPosition();
            int r = 16;
            for (int dx = -r; dx <= r; dx++) {
                for (int dy = -3; dy <= 3; dy++) {
                    for (int dz = -r; dz <= r; dz++) {
                        BlockPos p = playerPos.offset(dx, dy, dz);
                        if (minecraft.level.getBlockState(p).getBlock() instanceof FinanceTableBlock) {
                            net.minecraft.world.level.block.entity.BlockEntity fbe =
                                    minecraft.level.getBlockEntity(p);
                            if (fbe instanceof FinanceTableBlockEntity ftbe) {
                                total += ftbe.getTotalCoinValue();
                            }
                        }
                    }
                }
            }
        }
        return total;
    }

    // ==================== Sort Helpers ====================

    private List<Integer> getSortedIndices(List<MarketListing> listings) {
        List<Integer> indices = new ArrayList<>();
        for (int i = 0; i < listings.size(); i++) indices.add(i);
        if (sortMode == SortMode.NONE) return indices;

        indices.sort((a, b) -> {
            int cmp = switch (sortMode) {
                case NAME -> listings.get(a).getItemDisplayName().compareToIgnoreCase(
                        listings.get(b).getItemDisplayName());
                case TOWN -> {
                    TownData townA = TownRegistry.getTown(listings.get(a).getTownId());
                    TownData townB = TownRegistry.getTown(listings.get(b).getTownId());
                    String nameA = townA != null ? townA.getDisplayName() : listings.get(a).getTownId();
                    String nameB = townB != null ? townB.getDisplayName() : listings.get(b).getTownId();
                    yield nameA.compareToIgnoreCase(nameB);
                }
                case QTY -> Integer.compare(listings.get(a).getCount(), listings.get(b).getCount());
                case PRICE -> Integer.compare(listings.get(a).getPricePerItem(), listings.get(b).getPricePerItem());
                default -> 0;
            };
            return sortAscending ? cmp : -cmp;
        });
        return indices;
    }

    private void drawSortHeader(PoseStack ps, String label, int x, int y, SortMode mode) {
        boolean active = sortMode == mode;
        this.font.draw(ps, OtmGuiTheme.sortLabel(label, active, sortAscending), x, y, OtmGuiTheme.sortColor(active));
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

    // ==================== Drawing Helpers ====================

    private void drawPanel(PoseStack ps, int x, int y, int w, int h) {
        OtmGuiTheme.drawPanel(ps, x, y, w, h);
    }

    private void drawInsetPanel(PoseStack ps, int x, int y, int w, int h) {
        OtmGuiTheme.drawInsetPanel(ps, x, y, w, h);
    }

    // ==================== Render Background ====================

    @Override
    protected void renderBg(PoseStack poseStack, float partialTick, int mouseX, int mouseY) {
        RenderSystem.setShader(GameRenderer::getPositionTexShader);
        RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
        RenderSystem.setShaderTexture(0, TEXTURE);

        int x = this.leftPos;
        int y = this.topPos;

        // Main background
        drawPanel(poseStack, x, y, 384, 222);

        // Title bar
        drawInsetPanel(poseStack, x + 4, y + 3, 376, 14);

        // Cart toggle tab (right side of title bar, Trading Post style)
        if (selectedListingIndex < 0) {
            int tx = x + 312;
            int ty = y + 3;
            if (showingCart) {
                fill(poseStack, tx, ty, tx + 54, ty + 15, 0xFF8B7355);
                fill(poseStack, tx + 1, ty + 1, tx + 53, ty + 15, 0xFF3E3226);
            } else {
                fill(poseStack, tx, ty + 2, tx + 54, ty + 14, 0xFF1A1209);
                fill(poseStack, tx + 1, ty + 3, tx + 53, ty + 13, 0xFF2A1F14);
            }
        }

        // Extended content area (listings / cart) — no inventory below
        drawInsetPanel(poseStack, x + 4, y + 19, 360, 169);

        // Coin balance strip at the bottom
        drawInsetPanel(poseStack, x + 4, y + 200, 376, 14);

        if (showingCart) {
            renderCartBg(poseStack, x, y, mouseX, mouseY);
        } else {
            renderListingBg(poseStack, x, y, mouseX, mouseY);
        }
    }

    private void renderListingBg(PoseStack poseStack, int x, int y, int mouseX, int mouseY) {
        // Table header row
        fill(poseStack, x + 5, y + 20, x + 363, y + 30, 0xFF2C2318);

        // Compute hover row
        hoveredRow = -1;
        if (selectedListingIndex < 0) {
            int relMouseX = mouseX - x;
            int relMouseY = mouseY - y;
            if (relMouseX >= 5 && relMouseX <= 363 && relMouseY >= 31 && relMouseY < 31 + VISIBLE_LISTINGS * 11) {
                hoveredRow = (relMouseY - 31) / 11;
            }
        }

        // Alternating rows with hover
        MarketBoardBlockEntity be = menu.getBlockEntity();
        List<MarketListing> listings = be != null ? be.getListings() : List.of();
        List<Integer> sorted = getSortedIndices(listings);

        for (int i = 0; i < VISIBLE_LISTINGS; i++) {
            int rowY = y + 31 + i * 11;
            int sortedIdx = scrollOffset + i;
            boolean validListing = sortedIdx < sorted.size();
            boolean isHovered = (i == hoveredRow && validListing);

            if (isHovered) {
                fill(poseStack, x + 5, rowY, x + 363, rowY + 11, 0xFF5A4A30);
                fill(poseStack, x + 5, rowY, x + 6, rowY + 11, 0xFFFFD700);
            } else {
                int rowColor = (i % 2 == 0) ? 0xFF3E3226 : 0xFF453929;
                fill(poseStack, x + 5, rowY, x + 363, rowY + 11, rowColor);
            }
        }

        // Sort column underline
        if (sortMode != SortMode.NONE) {
            int hlX1 = x + switch (sortMode) {
                case NAME -> 5; case TOWN -> 131; case QTY -> 233; case PRICE -> 259; default -> 5;
            };
            int hlX2 = x + switch (sortMode) {
                case NAME -> 130; case TOWN -> 232; case QTY -> 258; case PRICE -> 340; default -> 130;
            };
            fill(poseStack, hlX1, y + 29, hlX2, y + 30, 0xFFFFD700);
        }

        // Item icons in listing rows
        for (int i = 0; i < VISIBLE_LISTINGS; i++) {
            int sortedIdx = scrollOffset + i;
            if (sortedIdx < sorted.size()) {
                int actualIdx = sorted.get(sortedIdx);
                MarketListing listing = listings.get(actualIdx);
                Item item = ForgeRegistries.ITEMS.getValue(listing.getItemId());
                if (item != null) {
                    ItemStack renderStack = listing.createItemStack(1);
                    int iconX = x + 7;
                    int iconY = y + 32 + i * 11;
                    PoseStack modelView = RenderSystem.getModelViewStack();
                    modelView.pushPose();
                    modelView.scale(0.5f, 0.5f, 1.0f);
                    RenderSystem.applyModelViewMatrix();
                    this.itemRenderer.renderGuiItem(renderStack, iconX * 2, iconY * 2);
                    modelView.popPose();
                    RenderSystem.applyModelViewMatrix();
                }
            }
        }
    }

    private void renderCartBg(PoseStack poseStack, int x, int y, int mouseX, int mouseY) {
        // Cart header
        fill(poseStack, x + 5, y + 20, x + 363, y + 30, 0xFF2C2318);

        hoveredCartRow = -1;
        int relMouseX = mouseX - x;
        int relMouseY = mouseY - y;
        if (relMouseX >= 5 && relMouseX <= 363 && relMouseY >= 31 && relMouseY < 31 + VISIBLE_CART_ROWS * 11) {
            hoveredCartRow = (relMouseY - 31) / 11;
        }

        for (int i = 0; i < VISIBLE_CART_ROWS; i++) {
            int rowY = y + 31 + i * 11;
            int cartIdx = cartScrollOffset + i;
            boolean valid = cartIdx < cart.size();
            boolean isHovered = (i == hoveredCartRow && valid);

            if (isHovered) {
                fill(poseStack, x + 5, rowY, x + 363, rowY + 11, 0xFF5A4A30);
                fill(poseStack, x + 5, rowY, x + 6, rowY + 11, 0xFFFF4444);
            } else {
                int rowColor = (i % 2 == 0) ? 0xFF3E3226 : 0xFF453929;
                fill(poseStack, x + 5, rowY, x + 363, rowY + 11, rowColor);
            }
        }
    }

    private void renderQtyOverlayBg(PoseStack poseStack, int x, int y) {
        // Qty overlay dims the content area only
        fill(poseStack, x + 4, y + 19, x + 380, y + 192, 0xFF000000);

        // Overlay panel — centered in the content area
        int ox = x + OVL_X;
        int oy = y + OVL_Y;
        drawPanel(poseStack, ox, oy, OVL_W, OVL_H);
        drawInsetPanel(poseStack, ox + 4, oy + 4, OVL_W - 8, OVL_H - 8);
    }

    // ==================== Render Labels ====================

    @Override
    protected void renderLabels(PoseStack poseStack, int mouseX, int mouseY) {
        // Title
        if (showingCart) {
            drawCenteredString(poseStack, this.font, "Shopping Cart", 96, 6, 0xFFD700);
        } else {
            drawCenteredString(poseStack, this.font, "Market Board", 96, 6, 0xFFD700);
        }

        // Cart toggle tab label
        if (selectedListingIndex < 0) {
            String cartLabel = "Cart (" + cart.size() + ")";
            int cartColor = showingCart ? 0xFFD700 : 0x888888;
            drawCenteredString(poseStack, this.font, cartLabel, 339, showingCart ? 6 : 7, cartColor);
        }

        // Refresh countdown timer (shown in title bar when market is refreshing)
        if (!showingCart) {
            MarketBoardBlockEntity rbe = menu.getBlockEntity();
            if (rbe != null && rbe.getRefreshCooldown() > 0 && !DebugConfig.UNLIMITED_REFRESHES) {
                int remainTicks = rbe.getRefreshCooldown();
                int totalSeconds = remainTicks / 20;
                int minutes = totalSeconds / 60;
                int seconds = totalSeconds % 60;
                String countdownStr = "Next: " + String.format("%d:%02d", minutes, seconds);
                this.font.draw(poseStack, countdownStr, 190, 6, 0xFF8888);
            }
        }

        if (showingCart) {
            renderCartLabels(poseStack);
        } else {
            renderListingLabels(poseStack);
        }

        // Coin balance strip label
        int balance = getPlayerCoinBalance();
        this.font.draw(poseStack, "\u00A77Coin Balance: " + formatCoinText(balance), 7, 203, 0xFFDD88);
    }

    private void renderListingLabels(PoseStack poseStack) {
        MarketBoardBlockEntity be = menu.getBlockEntity();
        if (be == null) return;

        List<MarketListing> listings = be.getListings();
        int maxScroll = Math.max(0, listings.size() - VISIBLE_LISTINGS);
        scrollOffset = Math.min(scrollOffset, maxScroll);

        if (listings.isEmpty()) {
            this.font.draw(poseStack, "No listings available.", 10, 50, 0x888888);
            this.font.draw(poseStack, "Market data will load automatically.", 10, 62, 0x888888);
            return;
        }

        // Sortable headers
        drawSortHeader(poseStack, "Item", 17, 22, SortMode.NAME);
        drawSortHeader(poseStack, "Town", 132, 22, SortMode.TOWN);
        drawSortHeader(poseStack, "Qty", 234, 22, SortMode.QTY);
        drawSortHeader(poseStack, "Price Ea.", 260, 22, SortMode.PRICE);

        int balance = getPlayerCoinBalance();
        int yOff = 33;
        int displayed = 0;
        List<Integer> sorted = getSortedIndices(listings);

        for (int d = scrollOffset; d < sorted.size() && displayed < VISIBLE_LISTINGS; d++, displayed++) {
            int actualIdx = sorted.get(d);
            MarketListing listing = listings.get(actualIdx);
            TownData town = TownRegistry.getTown(listing.getTownId());
            String townName = town != null ? town.getDisplayName() : listing.getTownId();

            int priceEach = listing.getPricePerItem();
            boolean canAfford = balance >= priceEach;

            // Check if already in cart
            boolean inCart = isInCart(actualIdx);

            String itemName = listing.getItemDisplayName();
            if (this.font.width(itemName) > 120) {
                while (this.font.width(itemName + "..") > 120 && itemName.length() > 3) {
                    itemName = itemName.substring(0, itemName.length() - 1);
                }
                itemName += "..";
            }
            if (this.font.width(townName) > 84) {
                while (this.font.width(townName + "..") > 84 && townName.length() > 3) {
                    townName = townName.substring(0, townName.length() - 1);
                }
                townName += "..";
            }

            int nameColor = canAfford ? 0xFFFFFF : 0xFF6666;
            // Sale items render in gold
            if (listing.isOnSale()) {
                nameColor = canAfford ? 0xFFD700 : 0xCC8844;
            }
            this.font.draw(poseStack, itemName, 17, yOff, nameColor);

            // Need level indicator dot before town name
            Item listingItem = ForgeRegistries.ITEMS.getValue(listing.getItemId());
            if (town != null && listingItem != null) {
                NeedLevel need = town.getNeedLevel(listingItem);
                int dotColor = 0xFF000000 | need.getColor();
                this.font.draw(poseStack, "\u25CF", 132, yOff, dotColor); // filled circle

                // Supply trend arrow
                String itemKey = listing.getItemId().toString();
                TownData.SupplyTrend trend = town.getTrend(itemKey);
                if (trend == TownData.SupplyTrend.FALLING) {
                    // Supply falling = demand rising → green up arrow
                    this.font.draw(poseStack, "\u25B2", 136, yOff, 0xFF44CC44);
                } else if (trend == TownData.SupplyTrend.RISING) {
                    // Supply rising = demand falling → red down arrow
                    this.font.draw(poseStack, "\u25BC", 136, yOff, 0xFFCC4444);
                }
            }

            this.font.draw(poseStack, townName, 144, yOff, 0xAAAAAA);
            this.font.draw(poseStack, String.valueOf(listing.getCount()), 234, yOff, 0xCCCCCC);
            CoinRenderer.renderCompactCoinValue(poseStack, this.font, 260, yOff, priceEach);

            // Cart status / Add button
            boolean isHovered = (displayed == hoveredRow);
            if (inCart) {
                this.font.draw(poseStack, "\u2714", 345, yOff, 0x44CC44);
            } else if (canAfford) {
                int col = isHovered ? 0x55FF55 : 0x44CC44;
                this.font.draw(poseStack, "[+]", 342, yOff, col);
            } else {
                this.font.draw(poseStack, "[+]", 342, yOff, 0x664444);
            }

            yOff += 11;
        }

        // Count + scroll indicator
        String countText = listings.size() + " listings";
        if (listings.size() > VISIBLE_LISTINGS) {
            countText += "  (" + (scrollOffset + 1) + "-" +
                    Math.min(scrollOffset + VISIBLE_LISTINGS, listings.size()) + ")";
        }
        this.font.draw(poseStack, countText, 8, 188, 0x666666);

        // Cart summary
        if (!cart.isEmpty()) {
            String cartSummary = "Cart: " + formatCoinText(getCartTotal());
            int cartW = this.font.width(cartSummary);
            this.font.draw(poseStack, cartSummary, 358 - cartW, 188, 0xFFCC44);
        }
    }

    private void renderCartLabels(PoseStack poseStack) {
        if (cart.isEmpty()) {
            this.font.draw(poseStack, "Your cart is empty.", 10, 50, 0x888888);
            this.font.draw(poseStack, "Browse the market to add items.", 10, 62, 0x888888);
            return;
        }

        // Cart headers
        this.font.draw(poseStack, "Item", 8, 22, 0xFFD700);
        this.font.draw(poseStack, "Qty", 190, 22, 0xFFD700);
        this.font.draw(poseStack, "Cost", 224, 22, 0xFFD700);
        this.font.draw(poseStack, "Remove", 310, 22, 0xFFD700);

        int maxScroll = Math.max(0, cart.size() - VISIBLE_CART_ROWS);
        cartScrollOffset = Math.min(cartScrollOffset, maxScroll);

        int yOff = 33;
        int displayed = 0;
        for (int i = cartScrollOffset; i < cart.size() && displayed < VISIBLE_CART_ROWS; i++, displayed++) {
            CartEntry entry = cart.get(i);

            String itemName = entry.itemName;
            if (this.font.width(itemName) > 175) {
                while (this.font.width(itemName + "..") > 175 && itemName.length() > 3) {
                    itemName = itemName.substring(0, itemName.length() - 1);
                }
                itemName += "..";
            }

            this.font.draw(poseStack, itemName, 8, yOff, 0xFFFFFF);
            this.font.draw(poseStack, "x" + entry.quantity, 190, yOff, 0xCCCCCC);
            CoinRenderer.renderCompactCoinValue(poseStack, this.font, 224, yOff, entry.totalCost());

            boolean isHovered = (displayed == hoveredCartRow);
            int removeColor = isHovered ? 0xFF4444 : 0xCC4444;
            this.font.draw(poseStack, "[\u2718]", 324, yOff, removeColor);

            yOff += 11;
        }

        // Total + balance
        int total = getCartTotal();
        int balance = getPlayerCoinBalance();
        boolean canAfford = balance >= total;

        this.font.draw(poseStack, "Total:", 8, 108, 0xFFD700);
        CoinRenderer.renderCoinValue(poseStack, this.font, 50, 108, total);

        String balStr = "Balance: " + formatCoinText(balance);
        int balColor = canAfford ? 0x55FF55 : 0xFF4444;
        int balW = this.font.width(balStr);
        this.font.draw(poseStack, balStr, 358 - balW, 108, balColor);
    }

    private void renderQtyOverlayLabels(PoseStack poseStack) {
        int cx = OVL_X + OVL_W / 2;  // center X in gui-local coords
        int oy = OVL_Y + 8;

        // Item name
        String name = selectedItemName;
        if (this.font.width(name) > OVL_W - 24) {
            while (this.font.width(name + "..") > OVL_W - 24 && name.length() > 3) {
                name = name.substring(0, name.length() - 1);
            }
            name += "..";
        }
        drawCenteredString(poseStack, this.font, name, cx, oy, 0xFFFFFF);

        // Quantity display
        String qtyStr = "Qty: " + selectedQuantity + " / " + selectedMaxQty;
        drawCenteredString(poseStack, this.font, qtyStr, cx, oy + 14, 0xFFD700);

        // Cost preview
        int cost = selectedQuantity * selectedPricePerItem;
        String costStr = "Cost: " + formatCoinText(cost);
        drawCenteredString(poseStack, this.font, costStr, cx, oy + 26, 0xFFCC44);
    }

    // ==================== Utilities ====================

    private boolean isInCart(int listingIndex) {
        for (CartEntry entry : cart) {
            if (entry.listingIndex == listingIndex) return true;
        }
        return false;
    }

    // ==================== Render & Tooltips ====================

    @Override
    public void render(PoseStack poseStack, int mouseX, int mouseY, float partialTick) {
        renderBackground(poseStack);
        super.render(poseStack, mouseX, mouseY, partialTick);

        // Render quantity overlay AFTER super.render() so it draws on top of all item icons
        if (selectedListingIndex >= 0) {
            renderQtyOverlayBg(poseStack, this.leftPos, this.topPos);
            // renderLabels is pre-translated by (leftPos, topPos); replicate that here
            poseStack.pushPose();
            poseStack.translate(this.leftPos, this.topPos, 0);
            renderQtyOverlayLabels(poseStack);
            poseStack.popPose();
        }

        renderTooltip(poseStack, mouseX, mouseY);

        // Listing tooltip on hover (not showing cart or overlay)
        if (!showingCart && selectedListingIndex < 0 && hoveredRow >= 0) {
            MarketBoardBlockEntity be = menu.getBlockEntity();
            if (be != null) {
                List<MarketListing> listings = be.getListings();
                List<Integer> sorted = getSortedIndices(listings);
                int sortedIdx = scrollOffset + hoveredRow;
                if (sortedIdx >= 0 && sortedIdx < sorted.size()) {
                    int actualIdx = sorted.get(sortedIdx);
                    MarketListing listing = listings.get(actualIdx);
                    renderListingTooltip(poseStack, listing, mouseX, mouseY);
                }
            }
        }

        // Cart row tooltip
        if (showingCart && hoveredCartRow >= 0) {
            int cartIdx = cartScrollOffset + hoveredCartRow;
            if (cartIdx < cart.size()) {
                CartEntry entry = cart.get(cartIdx);
                renderCartEntryTooltip(poseStack, entry, mouseX, mouseY);
            }
        }
    }

    private void renderListingTooltip(PoseStack ps, MarketListing listing, int mouseX, int mouseY) {
        List<Component> tooltip = new ArrayList<>();

        tooltip.add(Component.literal(listing.getItemDisplayName())
                .withStyle(ChatFormatting.WHITE, ChatFormatting.BOLD));

        if (listing.isOnSale()) {
            tooltip.add(Component.literal("\u2605 SALE! " + listing.getSaleDiscount() + "% OFF \u2605")
                    .withStyle(ChatFormatting.GOLD, ChatFormatting.BOLD));
        }

        tooltip.add(Component.literal("Price: " + formatCoinText(listing.getPricePerItem()) + " each")
                .withStyle(ChatFormatting.GRAY));
        tooltip.add(Component.literal("Qty: " + listing.getCount() + "  |  Total: " + formatCoinText(listing.getTotalPrice()))
                .withStyle(ChatFormatting.GRAY));

        int balance = getPlayerCoinBalance();
        int priceEach = listing.getPricePerItem();
        if (balance >= priceEach) {
            int canBuy = Math.min(balance / priceEach, listing.getCount());
            tooltip.add(Component.literal("\u2714 You can afford " + canBuy + " (of " + listing.getCount() + ")")
                    .withStyle(ChatFormatting.GREEN));
        } else {
            tooltip.add(Component.literal("\u2718 Need " + formatCoinText(priceEach - balance) + " more for 1")
                    .withStyle(ChatFormatting.RED));
        }

        tooltip.add(Component.literal(""));
        tooltip.add(Component.literal("Click to select quantity & add to cart")
                .withStyle(ChatFormatting.YELLOW));

        TownData town = TownRegistry.getTown(listing.getTownId());
        if (town != null) {
            tooltip.add(Component.literal(town.getDisplayName() + " (" + town.getType().getDisplayName() + ")")
                    .withStyle(ChatFormatting.GOLD));
            tooltip.add(Component.literal("Distance: " + town.getDistance())
                    .withStyle(ChatFormatting.GRAY));

            // Show need level for this item in the town
            net.minecraft.world.item.Item tooltipItem = ForgeRegistries.ITEMS.getValue(listing.getItemId());
            if (tooltipItem != null) {
                NeedLevel need = town.getNeedLevel(tooltipItem);
                ChatFormatting needColor = switch (need) {
                    case DESPERATE -> ChatFormatting.RED;
                    case HIGH_NEED -> ChatFormatting.GOLD;
                    case MODERATE_NEED -> ChatFormatting.YELLOW;
                    case BALANCED -> ChatFormatting.GREEN;
                    case SURPLUS -> ChatFormatting.AQUA;
                    case OVERSATURATED -> ChatFormatting.GRAY;
                };
                String arrow = need.isInDemand() ? "\u25B2" : need.isOversupplied() ? "\u25BC" : "\u25CF";
                tooltip.add(Component.literal(arrow + " Demand: " + need.getDisplayName() + " (" + String.format("%.0f", need.getPriceMultiplier() * 100) + "%)")
                        .withStyle(needColor));

                // Supply trend
                String itemKey = listing.getItemId().toString();
                TownData.SupplyTrend trend = town.getTrend(itemKey);
                if (trend == TownData.SupplyTrend.FALLING) {
                    tooltip.add(Component.literal("\u25B2 Demand trending UP")
                            .withStyle(ChatFormatting.GREEN));
                } else if (trend == TownData.SupplyTrend.RISING) {
                    tooltip.add(Component.literal("\u25BC Demand trending DOWN")
                            .withStyle(ChatFormatting.RED));
                } else {
                    tooltip.add(Component.literal("\u25CF Demand stable")
                            .withStyle(ChatFormatting.DARK_GRAY));
                }
            }
        }

        renderComponentTooltip(ps, tooltip, mouseX, mouseY);
    }

    private void renderCartEntryTooltip(PoseStack ps, CartEntry entry, int mouseX, int mouseY) {
        List<Component> tooltip = new ArrayList<>();

        tooltip.add(Component.literal(entry.itemName)
                .withStyle(ChatFormatting.WHITE, ChatFormatting.BOLD));
        tooltip.add(Component.literal("Qty: " + entry.quantity + "  |  Price: " + formatCoinText(entry.pricePerItem) + " ea")
                .withStyle(ChatFormatting.GRAY));
        tooltip.add(Component.literal("Subtotal: " + formatCoinText(entry.totalCost()))
                .withStyle(ChatFormatting.GOLD));

        TownData town = TownRegistry.getTown(entry.townId);
        if (town != null) {
            tooltip.add(Component.literal("From: " + town.getDisplayName())
                    .withStyle(ChatFormatting.GRAY));
        }

        tooltip.add(Component.literal(""));
        tooltip.add(Component.literal("Click to remove from cart")
                .withStyle(ChatFormatting.RED));

        renderComponentTooltip(ps, tooltip, mouseX, mouseY);
    }

    // ==================== Mouse Handling ====================

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (button == 0) {
            int x = this.leftPos;
            int y = this.topPos;
            double relX = mouseX - x;
            double relY = mouseY - y;

            // Cart toggle tab click (right side of title bar)
            if (selectedListingIndex < 0 && relX >= 312 && relX < 366 && relY >= 3 && relY < 17) {
                showingCart = !showingCart;
                selectedListingIndex = -1;
                updateButtonVisibility();
                return true;
            }

            // If quantity overlay is showing, don't allow other clicks
            // (buttons handle overlay interaction)
            if (selectedListingIndex >= 0) {
                return super.mouseClicked(mouseX, mouseY, button);
            }

            if (showingCart) {
                // Cart view: click to remove entry
                if (hoveredCartRow >= 0) {
                    int cartIdx = cartScrollOffset + hoveredCartRow;
                    if (cartIdx < cart.size()) {
                        cart.remove(cartIdx);
                        updateButtonVisibility();
                        return true;
                    }
                }
            } else {
                // Listing view: header click for sorting
                if (relY >= 20 && relY <= 30 && relX >= 5 && relX <= 363) {
                    SortMode clickedMode = SortMode.NONE;
                    if (relX < 131) clickedMode = SortMode.NAME;
                    else if (relX < 233) clickedMode = SortMode.TOWN;
                    else if (relX < 259) clickedMode = SortMode.QTY;
                    else if (relX < 341) clickedMode = SortMode.PRICE;

                    if (clickedMode != SortMode.NONE) {
                        if (sortMode == clickedMode) {
                            sortAscending = !sortAscending;
                        } else {
                            sortMode = clickedMode;
                            sortAscending = true;
                        }
                        scrollOffset = 0;
                        return true;
                    }
                }

                // Listing row click: open quantity selector
                MarketBoardBlockEntity be = menu.getBlockEntity();
                if (be != null) {
                    List<MarketListing> listings = be.getListings();
                    List<Integer> sorted = getSortedIndices(listings);

                    if (relX >= 5 && relX <= 363 && relY >= 31 && relY < 31 + VISIBLE_LISTINGS * 11) {
                        int row = (int) ((relY - 31) / 11);
                        int sortedIdx = scrollOffset + row;

                        if (sortedIdx >= 0 && sortedIdx < sorted.size()) {
                            int actualIdx = sorted.get(sortedIdx);
                            MarketListing listing = listings.get(actualIdx);
                            int balance = getPlayerCoinBalance();

                            if (balance >= listing.getPricePerItem()) {
                                // Open quantity overlay
                                selectedListingIndex = actualIdx;
                                selectedMaxQty = Math.min(listing.getCount(),
                                        balance / listing.getPricePerItem());
                                selectedQuantity = 1;
                                selectedItemName = listing.getItemDisplayName();
                                selectedPricePerItem = listing.getPricePerItem();
                                updateButtonVisibility();
                                return true;
                            }
                        }
                    }
                }
            }
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double delta) {
        if (selectedListingIndex >= 0) return true; // block scrolling during overlay

        if (showingCart) {
            int maxScroll = Math.max(0, cart.size() - VISIBLE_CART_ROWS);
            if (delta > 0 && cartScrollOffset > 0) {
                cartScrollOffset--;
                return true;
            } else if (delta < 0 && cartScrollOffset < maxScroll) {
                cartScrollOffset++;
                return true;
            }
        } else {
            MarketBoardBlockEntity be = menu.getBlockEntity();
            if (be != null) {
                int maxScroll = Math.max(0, be.getListings().size() - VISIBLE_LISTINGS);
                if (delta > 0 && scrollOffset > 0) {
                    scrollOffset--;
                    return true;
                } else if (delta < 0 && scrollOffset < maxScroll) {
                    scrollOffset++;
                    return true;
                }
            }
        }
        return super.mouseScrolled(mouseX, mouseY, delta);
    }
}
