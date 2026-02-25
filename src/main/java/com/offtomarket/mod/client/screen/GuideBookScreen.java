package com.offtomarket.mod.client.screen;

import com.mojang.blaze3d.vertex.PoseStack;
import com.offtomarket.mod.debug.DebugConfig;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.FormattedCharSequence;

import java.util.ArrayList;
import java.util.List;

/**
 * Book-style guide screen for "Off to the Market Guide".
 * Uses the vanilla book texture and renders pages of text
 * with guide content, tips, and a settings page.
 */
public class GuideBookScreen extends Screen {
    private static final ResourceLocation BOOK_TEXTURE =
            new ResourceLocation("minecraft", "textures/gui/book.png");

    private static final int BOOK_W = 192;  // vanilla book sprite width (from 256x256 atlas)
    private static final int BOOK_H = 192;  // vanilla book sprite height
    private static final int TEXT_X = 18;    // left margin for text
    private static final int TEXT_W = 156;   // full single-column width across the book
    private static final int TEXT_Y = 18;    // top margin for text
    private static final int LINE_H = 10;    // line height

    private int currentPage = 0;
    private Button prevBtn, nextBtn;

    // Each "spread" is a pair of pages (left + right), stored as string arrays
    private final List<PageSpread> pages = new ArrayList<>();

    public GuideBookScreen() {
        super(Component.literal("Off to the Market Guide"));
        buildPages();
    }

    private void buildPages() {
        // Page 0: Title
        pages.add(new PageSpread(new String[]{
                "",
                "",
                "    Off to the Market Guide",
                "",
                "       ~~~~~~~~~~",
                "",
                "    A Trader's Handbook",
                "",
                "",
                "        by the Guild",
                "        of Merchants"
        }));

        // Page 1: Welcome + Getting Started
        pages.add(new PageSpread(new String[]{
                "Welcome, Trader!",
                "~~~~~~~~~~",
                "",
                "This guide will help you master",
                "trading across the land.",
                "",
                "Getting Started:",
                "1. Place a Trading Post block.",
                "2. Place a Trading Ledger within 5 blocks.",
                "3. Stock the Ledger and set prices.",
                "4. Open the Post and pick a town.",
                "5. Click Send to dispatch!",
                "6. Wait for items to sell,",
                "   then collect coins."
        }));

        // Page 2: Towns & Trading Tips
        pages.add(new PageSpread(new String[]{
                "Towns & Demand",
                "~~~~~~~~~~",
                "",
                "Each town has needs and surplus.",
                "Sell what towns NEED for bonus!",
                "Avoid surplus - you'll get less.",
                "",
                "Trading Tips:",
                "Check the Market Board for prices.",
                "Higher trader levels unlock towns.",
                "Use the Ledger to trade remotely!"
        }));

        // Page 3: Coins & Market Board
        pages.add(new PageSpread(new String[]{
                "Currency",
                "~~~~~~~~~~",
                "",
                "Gold = 100 CP  |  Silver = 10 CP",
                "Copper = 1 CP (CP = Copper Pieces)",
                "",
                "Use the Coins tab to exchange coins.",
                "",
                "Market Board",
                "~~~~~~~~~~",
                "Shows what towns buy and sell.",
                "SALE items are shown in gold text.",
                "Add items to your cart and checkout."
        }));

        // Page 4: Shipments & Orders
        pages.add(new PageSpread(new String[]{
                "Shipments",
                "~~~~~~~~~~",
                "",
                "After sending goods, check the",
                "Shipments tab for delivery status.",
                "Items sell based on demand/pricing.",
                "",
                "Buy Orders",
                "~~~~~~~~~~",
                "Buy items from the Market Board.",
                "Orders arrive over time.",
                "Collect from the Orders tab!"
        }));

        // Page 5: Settings
        pages.add(new PageSpread(new String[]{
                "Settings",
                "~~~~~~~~~~",
                "",
                "Use /otm settings to view and",
                "change settings.",
                "",
                "Commands:",
                "  /otm settings - View all",
                "  /otm settings set <key> <value>",
                "",
                "Gold Only Mode:",
                "  /otm settings goldonly",
                "  Currently: " + (DebugConfig.isGoldOnlyMode() ? "ON" : "OFF")
        }));
    }

    @Override
    protected void init() {
        super.init();
        int bx = (this.width - BOOK_W) / 2;
        int by = (this.height - BOOK_H) / 2;

        prevBtn = addRenderableWidget(new Button(bx + 18, by + 164, 23, 13,
                Component.literal("<"), btn -> {
            if (currentPage > 0) currentPage--;
            updateButtons();
        }));

        nextBtn = addRenderableWidget(new Button(bx + 150, by + 164, 23, 13,
                Component.literal(">"), btn -> {
            if (currentPage < pages.size() - 1) currentPage++;
            updateButtons();
        }));

        updateButtons();
    }

    private void updateButtons() {
        prevBtn.visible = currentPage > 0;
        nextBtn.visible = currentPage < pages.size() - 1;
    }

    @Override
    public void render(PoseStack poseStack, int mouseX, int mouseY, float partialTick) {
        renderBackground(poseStack);

        int bx = (this.width - BOOK_W) / 2;
        int by = (this.height - BOOK_H) / 2;

        // Draw book background
        com.mojang.blaze3d.systems.RenderSystem.setShaderTexture(0, BOOK_TEXTURE);
        blit(poseStack, bx, by, 0, 0, BOOK_W, BOOK_H);

        // Render page content
        if (currentPage >= 0 && currentPage < pages.size()) {
            PageSpread spread = pages.get(currentPage);

            // Single-column text rendering
            int ly = by + TEXT_Y;
            for (String line : spread.lines) {
                if (line.startsWith("~~~")) {
                    // Decorative line
                    this.font.draw(poseStack, line, bx + TEXT_X, ly, 0x8B7355);
                } else {
                    // Wrap long lines across full book width
                    List<FormattedCharSequence> wrapped = this.font.split(
                            Component.literal(line), TEXT_W);
                    for (FormattedCharSequence wl : wrapped) {
                        this.font.draw(poseStack, wl, bx + TEXT_X, ly, 0x3B2A14);
                        ly += LINE_H;
                    }
                    continue;
                }
                ly += LINE_H;
            }
        }

        // Page number
        String pageNum = (currentPage + 1) + " / " + pages.size();
        int pw = this.font.width(pageNum);
        this.font.draw(poseStack, pageNum, bx + (BOOK_W - pw) / 2, by + BOOK_H - 18, 0x999999);

        super.render(poseStack, mouseX, mouseY, partialTick);
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    /**
     * A single page of text in the book.
     */
    private static class PageSpread {
        final String[] lines;

        PageSpread(String[] lines) {
            this.lines = lines;
        }
    }
}
