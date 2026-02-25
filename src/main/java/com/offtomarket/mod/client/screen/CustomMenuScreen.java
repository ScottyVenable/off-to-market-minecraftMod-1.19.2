package com.offtomarket.mod.client.screen;

import com.mojang.blaze3d.vertex.PoseStack;
import com.offtomarket.mod.content.CustomMenuDefinition;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraftforge.registries.ForgeRegistries;

import java.util.ArrayList;
import java.util.List;

/**
 * Reusable data-driven shop/info screen rendered from a CustomMenuDefinition.
 * Open in-game with: /otm menu open your-menu-id
 * No Container or Menu class needed - purely visual display.
 * Title, subtitle, and entries all come from the JSON definition.
 */
public class CustomMenuScreen extends Screen {

    // ── Layout constants ──────────────────────────────────────────────────
    private static final int W           = 280;
    private static final int H           = 210;
    private static final int HEADER_H    = 28;   // title + subtitle area
    private static final int ROW_H       = 24;   // height of each row
    private static final int VISIBLE     = 7;    // rows visible at once
    private static final int LIST_X_PAD  = 6;    // horizontal padding inside list
    private static final int ICON_SIZE   = 16;

    // ── State ─────────────────────────────────────────────────────────────
    private final CustomMenuDefinition def;

    /** Flat row list: String = category divider label, Entry = item row. */
    private final List<Object> rows = new ArrayList<>();

    private int leftPos;
    private int topPos;
    private int scrollOffset = 0;

    // ── Construction ──────────────────────────────────────────────────────

    public CustomMenuScreen(CustomMenuDefinition def) {
        super(Component.literal(def.title != null ? def.title : "Menu"));
        this.def = def;
        buildRows();
    }

    private void buildRows() {
        rows.clear();
        if (def.entries == null) return;
        String lastCategory = null;
        for (CustomMenuDefinition.Entry e : def.entries) {
            if (e == null) continue;
            // Insert category divider when the category changes
            String cat = (e.category != null && !e.category.isBlank()) ? e.category.trim() : null;
            if (cat != null && !cat.equals(lastCategory)) {
                rows.add(cat);   // String marker = divider row
                lastCategory = cat;
            }
            rows.add(e);
        }
    }

    // ── Lifecycle ─────────────────────────────────────────────────────────

    @Override
    protected void init() {
        super.init();
        this.leftPos = (this.width - W) / 2;
        this.topPos  = (this.height - H) / 2;
        scrollOffset = Math.min(scrollOffset, maxScroll());

        // Close button top-right (1.19.2 constructor style)
        this.addRenderableWidget(new Button(leftPos + W - 20, topPos + 4, 16, 16,
                Component.literal("X"), b -> this.onClose()));
    }

    @Override
    public boolean isPauseScreen() { return false; }

    // ── Rendering ─────────────────────────────────────────────────────────

    @Override
    public void render(PoseStack ps, int mx, int my, float pt) {
        this.renderBackground(ps);
        drawWindow(ps);
        drawHeader(ps);
        drawList(ps, mx, my);
        drawFooter(ps);
        super.render(ps, mx, my, pt); // renders buttons
    }

    // ── Window shell ──────────────────────────────────────────────────────

    private void drawWindow(PoseStack ps) {
        OtmGuiTheme.drawPanel(ps, leftPos, topPos, W, H);
    }

    // ── Header ────────────────────────────────────────────────────────────

    private void drawHeader(PoseStack ps) {
        int x = leftPos;
        int y = topPos;

        // Inset header bar
        OtmGuiTheme.drawInsetPanel(ps, x + 4, y + 4, W - 28, HEADER_H - 2);

        // Title
        String title = def.title != null ? def.title : "Menu";
        font.draw(ps, title, x + 8, y + 8, OtmGuiTheme.TEXT_TITLE);

        // Subtitle
        if (def.subtitle != null && !def.subtitle.isBlank()) {
            font.draw(ps, def.subtitle, x + 8, y + 18, OtmGuiTheme.TEXT_MUTED);
        }

        // Divider below header
        OtmGuiTheme.drawDividerH(ps, x + 4, x + W - 4, y + HEADER_H + 2);
    }

    // ── Entry list ────────────────────────────────────────────────────────

    private int listTop() { return topPos + HEADER_H + 6; }

    private void drawList(PoseStack ps, int mx, int my) {
        int listY = listTop();
        int x     = leftPos + LIST_X_PAD;
        int availW = W - LIST_X_PAD * 2;

        for (int i = 0; i < VISIBLE && (i + scrollOffset) < rows.size(); i++) {
            int rowY = listY + i * ROW_H;
            Object row = rows.get(i + scrollOffset);

            boolean hovered = mx >= leftPos && mx < leftPos + W
                           && my >= rowY    && my < rowY + ROW_H;

            if (row instanceof String divLabel) {
                drawCategoryRow(ps, x, rowY, availW, (String) divLabel);
            } else if (row instanceof CustomMenuDefinition.Entry entry) {
                drawEntryRow(ps, x, rowY, availW, entry, hovered);
            }
        }
    }

    private void drawCategoryRow(PoseStack ps, int x, int y, int w, String label) {
        // Subtle category divider
        net.minecraft.client.gui.GuiComponent.fill(ps, x, y + 1, x + w, y + ROW_H - 2, OtmGuiTheme.INSET_FILL);
        OtmGuiTheme.drawDividerH(ps, x, x + w, y + 1);
        OtmGuiTheme.drawDividerH(ps, x, x + w, y + ROW_H - 2);
        int textX = x + w / 2 - font.width(label) / 2;
        font.draw(ps, label, textX, y + 8, OtmGuiTheme.TEXT_COL_HEADER);
    }

    private void drawEntryRow(PoseStack ps, int x, int y, int w, CustomMenuDefinition.Entry entry, boolean hovered) {
        // Hover highlight
        if (hovered) {
            net.minecraft.client.gui.GuiComponent.fill(ps,
                    x, y + 1, x + w, y + ROW_H - 1, 0x22FFFFFF);
        }

        // Item icon slot
        OtmGuiTheme.drawSlot(ps, x + 1, y + (ROW_H - ICON_SIZE) / 2);
        ItemStack stack = resolveItemStack(entry.item);
        this.minecraft.getItemRenderer().renderGuiItem(stack, x + 2, y + (ROW_H - ICON_SIZE) / 2);
        this.minecraft.getItemRenderer().renderGuiItemDecorations(
                font, stack, x + 2, y + (ROW_H - ICON_SIZE) / 2);

        // Label
        int textX = x + ICON_SIZE + 8;
        String label = (entry.label != null && !entry.label.isBlank())
                ? entry.label
                : stack.getHoverName().getString();

        // Price badge width for truncation
        String priceText = (entry.price > 0) ? formatPrice(entry.price) : "";
        int priceW = priceText.isEmpty() ? 0 : (font.width(priceText) + 6);
        int maxLabelW = w - ICON_SIZE - 10 - priceW;

        font.draw(ps, OtmGuiTheme.truncate(font, label, maxLabelW), textX, y + 4, OtmGuiTheme.TEXT_NAME);

        // Description
        if (entry.description != null && !entry.description.isBlank()) {
            String desc = OtmGuiTheme.truncate(font, entry.description, w - ICON_SIZE - 10);
            font.draw(ps, desc, textX, y + 14, OtmGuiTheme.TEXT_MUTED);
        }

        // Price badge (right-aligned)
        if (entry.price > 0) {
            int px = x + w - priceW;
            font.draw(ps, priceText, px, y + 8, OtmGuiTheme.TEXT_PAYOUT);
        } else if (entry.price == 0 && !priceText.isEmpty()) {
            font.draw(ps, "Info", x + w - font.width("Info") - 2, y + 8, OtmGuiTheme.TEXT_MUTED);
        }

        // Stock indicator (small, after price)
        if (entry.stock >= 0) {
            String stockStr = "x" + entry.stock;
            font.draw(ps, stockStr, x + w - font.width(stockStr) - 2, y + 16,
                    entry.stock == 0 ? OtmGuiTheme.STATUS_WARN : OtmGuiTheme.TEXT_MUTED);
        }

        // Row separator
        OtmGuiTheme.drawDividerH(ps, x, x + w, y + ROW_H - 1);
    }

    // ── Footer ────────────────────────────────────────────────────────────

    private void drawFooter(PoseStack ps) {
        int y = topPos + H - 14;
        OtmGuiTheme.drawDividerH(ps, leftPos + 4, leftPos + W - 4, y - 2);

        // Scroll hint / page info
        if (rows.size() > VISIBLE) {
            String scrollText = (scrollOffset + 1) + " – "
                    + Math.min(scrollOffset + VISIBLE, rows.size())
                    + " of " + rows.size()
                    + "  (scroll to move)";
            font.draw(ps, scrollText, leftPos + 8, y + 2, OtmGuiTheme.TEXT_SCROLL);
        } else {
            // Show menu id as a subtle hint
            String hint = "/otm menu open " + (def.id != null ? def.id : "...");
            font.draw(ps, hint, leftPos + 8, y + 2, OtmGuiTheme.TEXT_MUTED);
        }
    }

    // ── Scroll ────────────────────────────────────────────────────────────

    @Override
    public boolean mouseScrolled(double mx, double my, double delta) {
        int newOffset = scrollOffset - (int) Math.signum(delta);
        scrollOffset = Math.max(0, Math.min(newOffset, maxScroll()));
        return true;
    }

    private int maxScroll() {
        return Math.max(0, rows.size() - VISIBLE);
    }

    // ── Helpers ──────────────────────────────────────────────────────────

    private static ItemStack resolveItemStack(String itemId) {
        if (itemId == null || itemId.isBlank()) return new ItemStack(Items.BARRIER);
        try {
            String trimmed = itemId.trim();
            ResourceLocation rl = ResourceLocation.tryParse(trimmed);
            if (rl == null) return new ItemStack(Items.BARRIER);
            var item = ForgeRegistries.ITEMS.getValue(rl);
            return item != null ? new ItemStack(item) : new ItemStack(Items.BARRIER);
        } catch (Exception e) {
            return new ItemStack(Items.BARRIER);
        }
    }

    private static String formatPrice(int priceCP) {
        if (priceCP <= 0) return "";
        if (priceCP < 10) return priceCP + " cp";
        if (priceCP < 100) return priceCP + " cp";
        int silver = priceCP / 10;
        int copper = priceCP % 10;
        if (copper == 0) return silver + " sp";
        return silver + " sp " + copper + " cp";
    }
}
