package com.offtomarket.mod.content;

import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Holds all loaded custom menu definitions, keyed by their {@code id}.
 *
 * Populated at startup by {@link CustomMenuLoader#loadAll()}.
 * Query with {@link #get(String)} in the /otm menu open command handler.
 */
public final class CustomMenuRegistry {

    private static final Map<String, CustomMenuDefinition> MENUS = new LinkedHashMap<>();

    private CustomMenuRegistry() {}

    /** Register a loaded menu definition. Overwrites any existing entry with the same id. */
    public static void register(CustomMenuDefinition def) {
        MENUS.put(def.id, def);
    }

    /**
     * Look up a menu by ID (case-insensitive).
     *
     * @param id the menu's unique ID
     * @return the definition, or {@code null} if not found
     */
    public static CustomMenuDefinition get(String id) {
        return MENUS.get(id == null ? null : id.toLowerCase());
    }

    /** All registered custom menu IDs (for command auto-complete). */
    public static Collection<String> getAllIds() {
        return Collections.unmodifiableSet(MENUS.keySet());
    }

    /** The full registry map (unmodifiable). */
    public static Collection<CustomMenuDefinition> getAll() {
        return Collections.unmodifiableCollection(MENUS.values());
    }
}
