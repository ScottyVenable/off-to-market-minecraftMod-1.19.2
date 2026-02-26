package com.offtomarket.mod.content;

import java.util.List;

/**
 * POJO that mirrors the JSON structure of a custom menu definition file.
 * One JSON file = one testable screen.
 *
 * Create a file in data/offtomarket/custom_menus/ and list it in _index.json.
 * Then open it in-game with:  /otm menu open your-menu-id
 *
 * Fields:
 *   id          — unique ID used in the /otm menu open command
 *   title       — bold header shown at the top of the screen
 *   subtitle    — smaller text below the title (optional)
 *   entries     — list of rows shown in the scrollable list body
 *
 * Entry fields:
 *   item        — registry name of the icon item, e.g. "minecraft:golden_carrot"
 *   label       — display name (optional; uses item name if omitted)
 *   description — short flavour / info text shown below the label (optional)
 *   price       — price in copper pieces; 0 = info-only row (no price badge)
 *   stock       — available stock count; -1 = unlimited (optional)
 *   category    — if set, a bold divider row with this text appears before the entry (optional)
 */
public class CustomMenuDefinition {

    /** ID used in /otm menu open command. */
    public String id;

    /** Bold title displayed at the top of the screen. */
    public String title;

    /** Subtitle shown in smaller text below the title (optional). */
    public String subtitle;

    /** Rows to display in the scrollable list. */
    public List<Entry> entries;

    // -----------------------------------------------------------------------

    public static class Entry {

        /** Minecraft item registry ID displayed as an icon, e.g. "minecraft:wheat". */
        public String item;

        /**
         * Display label. If null or blank, the item's in-game name is shown automatically.
         */
        public String label;

        /** Short description or flavour text shown below the label. Optional. */
        public String description;

        /**
         * Price in copper pieces (CP).
         * Set to 0 to mark as an info-only entry with no price badge.
         */
        public int price = 0;

        /**
         * Available stock count. -1 = unlimited (default).
         * Displayed in the price badge area.
         */
        public int stock = -1;

        /**
         * If set, a bold divider row with this category heading is inserted
         * above this entry in the list. Useful for grouping related items.
         */
        public String category;
    }
}
