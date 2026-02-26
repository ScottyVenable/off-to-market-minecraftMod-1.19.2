package com.offtomarket.mod.client.screen;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiComponent;

/**
 * Central UI theme for all Off to Market screens.
 *
 * All color constants, panel-drawing helpers, and text utilities live here so
 * new screens can achieve a consistent look with a single import.
 *
 * <pre>
 * Example usage in a new screen:
 *
 *   // Background
 *   OtmGuiTheme.drawPanel(poseStack, x, y, imageWidth, imageHeight);
 *   OtmGuiTheme.drawInsetPanel(poseStack, x + 4, y + 3, imageWidth - 8, 14);
 *
 *   // Labels
 *   font.draw(poseStack, "My Section", 8, 6, OtmGuiTheme.TEXT_TITLE);
 *   font.draw(poseStack, "Value:", 8, 20, OtmGuiTheme.TEXT_LABEL);
 *   font.draw(poseStack, someValue, 60, 20, OtmGuiTheme.TEXT_VALUE);
 *
 *   // Column headers with sort arrow
 *   font.draw(poseStack, OtmGuiTheme.sortLabel("Name", isNameSort, ascending),
 *             8, 32, OtmGuiTheme.sortColor(isNameSort));
 * </pre>
 */
public final class OtmGuiTheme {

    private OtmGuiTheme() {}

    // =========================================================================
    // Panel structure colours
    // =========================================================================

    /** Outermost 1-pixel shadow / border (near-black brown). */
    public static final int PANEL_SHADOW  = 0xFF1A1209;

    /** Top- and left-edge highlight bevel (lighter wood grain). */
    public static final int PANEL_BEVEL   = 0xFF8B7355;

    /** Bottom- and right-edge dark bevel; also used for inset borders and
     *  thin divider lines. */
    public static final int PANEL_SHADE   = 0xFF2A1F14;

    /** Main panel body fill (medium warm wood). */
    public static final int PANEL_FILL    = 0xFF5C4A32;

    /** Inset sub-panel fill (dark wood — recessed feel). */
    public static final int INSET_FILL    = 0xFF3E3226;

    /** Row separator / thematic divider line (slightly lighter than PANEL_SHADE). */
    public static final int SEPARATOR     = 0xFF4A3828;

    // =========================================================================
    // Text colours
    // =========================================================================

    /** Section and panel titles — gold. */
    public static final int TEXT_TITLE        = 0xFFD700;

    /** Column sub-headers — amber, slightly muted. */
    public static final int TEXT_COL_HEADER   = 0xCCAA44;

    /** Stat / field labels — blue-grey; stands out on dark wood. */
    public static final int TEXT_LABEL        = 0x88BBDD;

    /** Data values — white. */
    public static final int TEXT_VALUE        = 0xFFFFFF;

    /** Entity, town, and item names — warm cream. */
    public static final int TEXT_NAME         = 0xEEDDCC;

    /** Disabled, secondary, or hint text — neutral grey. */
    public static final int TEXT_MUTED        = 0x888888;

    /** Scroll position / pagination hints — muted blue. */
    public static final int TEXT_SCROLL       = 0x557799;

    /** Earnings / payout numbers — warm gold (lighter than TITLE). */
    public static final int TEXT_PAYOUT       = 0xCCAA66;

    /** Sort header when it is NOT the active sort column (same as TITLE). */
    public static final int TEXT_SORT_IDLE    = 0xFFD700;

    /** Sort header when it IS the active sort column — bright yellow. */
    public static final int TEXT_SORT_ACTIVE  = 0xFFFF88;

    /** Inventory section label — same as TEXT_MUTED. */
    public static final int TEXT_INVENTORY    = 0x888888;

    // =========================================================================
    // Status / feedback colours
    // =========================================================================

    /** Positive state — bright green. */
    public static final int STATUS_OK           = 0x55FF55;

    /** Negative state / over-capacity / error — red. */
    public static final int STATUS_WARN         = 0xFF5555;

    /** Weight / capacity within limits — soft green. */
    public static final int STATUS_CAP_OK       = 0x88CC88;

    /** Cannot-afford balance warning — bright red. */
    public static final int STATUS_BALANCE_WARN = 0xFF4444;

    // =========================================================================
    // Sort-arrow string constants
    // =========================================================================

    /** Ascending sort arrow appended to column headers. */
    public static final String ARROW_UP   = " \u25B2";

    /** Descending sort arrow appended to column headers. */
    public static final String ARROW_DOWN = " \u25BC";

    // =========================================================================
    // Panel drawing helpers
    // =========================================================================

    /**
     * Draws the standard outer panel: dark shadow border, bevel edges, and
     * warm-wood body fill.  Used as the outermost container for every screen.
     */
    public static void drawPanel(PoseStack ps, int x, int y, int w, int h) {
        GuiComponent.fill(ps, x,         y,         x + w,         y + h,     PANEL_SHADOW);
        GuiComponent.fill(ps, x + 1,     y + 1,     x + w - 1,     y + 2,     PANEL_BEVEL);
        GuiComponent.fill(ps, x + 1,     y + 1,     x + 2,         y + h - 1, PANEL_BEVEL);
        GuiComponent.fill(ps, x + 1,     y + h - 2, x + w - 1,     y + h - 1, PANEL_SHADE);
        GuiComponent.fill(ps, x + w - 2, y + 1,     x + w - 1,     y + h - 1, PANEL_SHADE);
        GuiComponent.fill(ps, x + 2,     y + 2,     x + w - 2,     y + h - 2, PANEL_FILL);
    }

    /**
     * Draws a recessed inset sub-panel (dark border + very dark fill).
     * Used for tab content areas, stat strips, list panes, etc.
     */
    public static void drawInsetPanel(PoseStack ps, int x, int y, int w, int h) {
        GuiComponent.fill(ps, x,     y,     x + w,     y + h,     PANEL_SHADE);
        GuiComponent.fill(ps, x + 1, y + 1, x + w - 1, y + h - 1, INSET_FILL);
    }

    /**
     * Draws a standard 18 × 18 item slot.
     *
     * @param x left edge of the inner 16 × 16 icon area (i.e. slot origin).
     * @param y top edge of the inner 16 × 16 icon area.
     */
    public static void drawSlot(PoseStack ps, int x, int y) {
        GuiComponent.fill(ps, x - 1, y - 1, x + 17, y + 17, 0xFF373737);
        GuiComponent.fill(ps, x,     y,     x + 16, y + 16, 0xFF8B8B8B);
    }

    /**
     * Draws a 1-pixel horizontal divider line using {@link #SEPARATOR}.
     *
     * @param x1 left edge (absolute screen / panel-relative coords)
     * @param x2 right edge (exclusive)
     * @param y  y-coordinate of the pixel row
     */
    public static void drawDividerH(PoseStack ps, int x1, int x2, int y) {
        GuiComponent.fill(ps, x1, y, x2, y + 1, SEPARATOR);
    }

    /**
     * Draws a 1-pixel vertical divider line using {@link #SEPARATOR}.
     *
     * @param x  x-coordinate of the pixel column
     * @param y1 top edge
     * @param y2 bottom edge (exclusive)
     */
    public static void drawDividerV(PoseStack ps, int x, int y1, int y2) {
        GuiComponent.fill(ps, x, y1, x + 1, y2, SEPARATOR);
    }

    // =========================================================================
    // Text helpers
    // =========================================================================

    /**
     * Draws a section/panel title in {@link #TEXT_TITLE} (gold).
     */
    public static void drawTitle(Font font, PoseStack ps, String text, int x, int y) {
        font.draw(ps, text, x, y, TEXT_TITLE);
    }

    /**
     * Draws a field label in {@link #TEXT_LABEL} (blue-grey).
     */
    public static void drawLabel(Font font, PoseStack ps, String text, int x, int y) {
        font.draw(ps, text, x, y, TEXT_LABEL);
    }

    /**
     * Returns the sort column header string, appending an arrow when active.
     *
     * @param label     base column label (e.g. "Name")
     * @param active    {@code true} if this column is the current sort key
     * @param ascending {@code true} = ascending; only meaningful when active
     * @return label possibly suffixed with {@link #ARROW_UP} or {@link #ARROW_DOWN}
     */
    public static String sortLabel(String label, boolean active, boolean ascending) {
        if (!active) return label;
        return label + (ascending ? ARROW_UP : ARROW_DOWN);
    }

    /**
     * Returns the text colour to use for a sort column header.
     *
     * @param active {@code true} if this column is the current sort key
     * @return {@link #TEXT_SORT_ACTIVE} or {@link #TEXT_SORT_IDLE}
     */
    public static int sortColor(boolean active) {
        return active ? TEXT_SORT_ACTIVE : TEXT_SORT_IDLE;
    }

    /**
     * Truncates {@code text} so that {@code font.width(text + suffix) <= maxWidth},
     * appending {@code suffix} when truncation occurs.
     *
     * @param font     the font to measure with
     * @param text     original text
     * @param maxWidth maximum allowed pixel width
     * @param suffix   appended when truncation is needed (typically "..")
     * @return original text if it fits, otherwise a shortened version + suffix
     */
    public static String truncate(Font font, String text, int maxWidth, String suffix) {
        if (font.width(text) <= maxWidth) return text;
        while (font.width(text + suffix) > maxWidth && text.length() > 1)
            text = text.substring(0, text.length() - 1);
        return text + suffix;
    }

    /**
     * Convenience overload of {@link #truncate(Font, String, int, String)}
     * that uses {@code ".."} as the suffix.
     */
    public static String truncate(Font font, String text, int maxWidth) {
        return truncate(font, text, maxWidth, "..");
    }
}
