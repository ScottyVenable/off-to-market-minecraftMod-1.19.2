package com.offtomarket.mod.content;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.offtomarket.mod.OffToMarket;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.List;
import java.util.Locale;

/**
 * Loads custom menu definitions from JSON files at mod startup.
 *
 * Menus live in:  src/main/resources/data/offtomarket/custom_menus/
 *   _index.json        — lists every menu filename to load
 *   _template.json     — blank template; copy and rename to create a new menu
 *   example_*.json     — working example menus
 *
 * To create a new custom menu:
 *   1. Copy _template.json, rename it to your-menu-id.json
 *   2. Fill in the fields (see _template.json for full docs)
 *   3. Add "your-menu-id.json" to the "menus" array in _index.json
 *   4. Recompile, then test with:  /otm menu open your-menu-id
 */
public class CustomMenuLoader {

    private static final Gson GSON = new GsonBuilder().create();
    private static final String INDEX_PATH = "/data/offtomarket/custom_menus/_index.json";
    private static final String MENUS_ROOT = "/data/offtomarket/custom_menus/";

    /** Load every menu listed in _index.json and register it with CustomMenuRegistry. */
    public static void loadAll() {
        List<String> files = loadIndex();
        if (files.isEmpty()) {
            OffToMarket.LOGGER.info("[CustomMenuLoader] No custom menus listed in _index.json.");
            return;
        }

        int loaded = 0;
        for (String fileName : files) {
            if (fileName.startsWith("_") || fileName.isBlank()) continue;
            try {
                CustomMenuDefinition def = loadMenuFile(MENUS_ROOT + fileName);
                if (def != null) {
                    CustomMenuRegistry.register(def);
                    loaded++;
                }
            } catch (Exception e) {
                OffToMarket.LOGGER.error("[CustomMenuLoader] Failed to load menu '{}': {}", fileName, e.getMessage());
            }
        }
        OffToMarket.LOGGER.info("[CustomMenuLoader] Loaded {} custom menu(s).", loaded);
    }

    // -----------------------------------------------------------------------

    private static List<String> loadIndex() {
        try (InputStream is = CustomMenuLoader.class.getResourceAsStream(INDEX_PATH)) {
            if (is == null) {
                OffToMarket.LOGGER.debug("[CustomMenuLoader] No custom_menus/_index.json found.");
                return Collections.emptyList();
            }
            MenuIndex idx = GSON.fromJson(new InputStreamReader(is, StandardCharsets.UTF_8), MenuIndex.class);
            return (idx != null && idx.menus != null) ? idx.menus : Collections.emptyList();
        } catch (Exception e) {
            OffToMarket.LOGGER.error("[CustomMenuLoader] Could not read menu index", e);
            return Collections.emptyList();
        }
    }

    private static CustomMenuDefinition loadMenuFile(String path) throws Exception {
        try (InputStream is = CustomMenuLoader.class.getResourceAsStream(path)) {
            if (is == null) {
                OffToMarket.LOGGER.warn("[CustomMenuLoader] Menu file not found: {}", path);
                return null;
            }
            CustomMenuDefinition def = GSON.fromJson(
                    new InputStreamReader(is, StandardCharsets.UTF_8), CustomMenuDefinition.class);
            if (def == null || def.id == null || def.id.isBlank()) {
                OffToMarket.LOGGER.warn("[CustomMenuLoader] Menu at {} has no 'id' — skipped.", path);
                return null;
            }
            def.id = def.id.trim().toLowerCase(Locale.ROOT);
            return def;
        }
    }

    // Internal index POJO
    private static class MenuIndex {
        List<String> menus;
    }
}
