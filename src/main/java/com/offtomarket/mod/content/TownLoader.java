package com.offtomarket.mod.content;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.offtomarket.mod.OffToMarket;
import com.offtomarket.mod.data.NeedLevel;
import com.offtomarket.mod.data.TownData;
import com.offtomarket.mod.data.TownLetter;
import com.offtomarket.mod.data.TownRegistry;
import net.minecraft.resources.ResourceLocation;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.*;

/**
 * Loads town definitions from JSON files at mod startup.
 *
 * Towns live in: src/main/resources/data/offtomarket/towns/
 *   _index.json        — lists every town filename to load
 *   _template.json     — blank template; copy and rename to add a new town
 *   greenhollow.json   — example existing town
 *   mytown.json        — any new file you add (register its name in _index.json)
 *
 * To add a new town:
 *   1. Copy _template.json, rename it to your-town-id.json
 *   2. Fill in the fields (see _template.json for docs)
 *   3. Add "your-town-id.json" to the "towns" array in _index.json
 *   4. Recompile (or hot-swap with Ctrl+Shift+B)
 */
public class TownLoader {

    private static final Gson GSON = new GsonBuilder().create();
    private static final String INDEX_PATH  = "/data/offtomarket/towns/_index.json";
    private static final String TOWNS_ROOT  = "/data/offtomarket/towns/";

    /** Load every town listed in _index.json and register it with TownRegistry. */
    public static void loadAll() {
        List<String> files = loadIndex();
        if (files.isEmpty()) {
            OffToMarket.LOGGER.warn("[TownLoader] _index.json is empty or not found — no JSON towns loaded!");
            return;
        }

        int loaded = 0;
        for (String fileName : files) {
            if (fileName.startsWith("_") || fileName.isBlank()) continue; // skip templates
            try {
                TownData town = loadTownFile(TOWNS_ROOT + fileName);
                if (town != null) {
                    TownRegistry.register(town);
                    loaded++;
                }
            } catch (Exception e) {
                OffToMarket.LOGGER.error("[TownLoader] Failed to load town file '{}': {}", fileName, e.getMessage());
            }
        }
        OffToMarket.LOGGER.info("[TownLoader] Loaded {} town(s) from JSON definitions.", loaded);
    }

    // -----------------------------------------------------------------------

    private static List<String> loadIndex() {
        try (InputStream is = TownLoader.class.getResourceAsStream(INDEX_PATH)) {
            if (is == null) {
                OffToMarket.LOGGER.warn("[TownLoader] Town index not found at {}", INDEX_PATH);
                return Collections.emptyList();
            }
            TownIndex idx = GSON.fromJson(new InputStreamReader(is, StandardCharsets.UTF_8), TownIndex.class);
            return (idx != null && idx.towns != null) ? idx.towns : Collections.emptyList();
        } catch (Exception e) {
            OffToMarket.LOGGER.error("[TownLoader] Failed to read town index", e);
            return Collections.emptyList();
        }
    }

    private static TownData loadTownFile(String path) throws Exception {
        try (InputStream is = TownLoader.class.getResourceAsStream(path)) {
            if (is == null) {
                OffToMarket.LOGGER.warn("[TownLoader] Town file not found at classpath: {}", path);
                return null;
            }
            TownDefinition def = GSON.fromJson(new InputStreamReader(is, StandardCharsets.UTF_8), TownDefinition.class);
            return buildTownData(def, path);
        }
    }

    private static TownData buildTownData(TownDefinition def, String srcPath) {
        if (def == null || def.id == null || def.id.isBlank()) {
            OffToMarket.LOGGER.warn("[TownLoader] Skipping {} — missing or empty 'id' field.", srcPath);
            return null;
        }

        // Parse type
        TownData.TownType type = TownData.TownType.TOWN;
        if (def.type != null && !def.type.isBlank()) {
            try {
                type = TownData.TownType.valueOf(def.type.toUpperCase(Locale.ROOT));
            } catch (IllegalArgumentException e) {
                OffToMarket.LOGGER.warn("[TownLoader] Unknown type '{}' for town '{}' — defaulting to TOWN.", def.type, def.id);
            }
        }

        // Build specialty items set
        Set<ResourceLocation> specialtyItems = new LinkedHashSet<>();
        if (def.sells != null) {
            for (String s : def.sells) {
                if (s != null && !s.isBlank()) {
                    specialtyItems.add(new ResourceLocation(s.trim()));
                }
            }
        }

        // Build needLevels map
        Map<String, NeedLevel> needLevels = new LinkedHashMap<>();
        if (def.needLevels != null) {
            for (Map.Entry<String, String> e : def.needLevels.entrySet()) {
                if (e.getKey() == null || e.getValue() == null) continue;
                if (e.getKey().startsWith("_")) continue; // skip _comment_ and similar doc keys
                try {
                    needLevels.put(e.getKey().trim(), NeedLevel.valueOf(e.getValue().toUpperCase(Locale.ROOT)));
                } catch (IllegalArgumentException ex) {
                    OffToMarket.LOGGER.warn("[TownLoader] Unknown NeedLevel '{}' for item '{}' in town '{}' — skipped.",
                            e.getValue(), e.getKey(), def.id);
                }
            }
        }

        // Build letters map
        Map<String, List<TownLetter>> letters = new LinkedHashMap<>();
        if (def.letters != null) {
            for (Map.Entry<String, List<TownDefinition.LetterDef>> entry : def.letters.entrySet()) {
                String eventKey = entry.getKey();
                if (eventKey == null || eventKey.startsWith("_") || entry.getValue() == null) continue;
                List<TownLetter> parsed = new ArrayList<>();
                for (TownDefinition.LetterDef ld : entry.getValue()) {
                    if (ld == null) continue;
                    parsed.add(new TownLetter(
                            ld.sender,
                            ld.subject,
                            ld.body,
                            ld.minReputation == 0 ? Integer.MIN_VALUE : ld.minReputation,
                            ld.maxReputation == 0 ? Integer.MAX_VALUE : ld.maxReputation,
                            ld.chanceWeight,
                            ld.tags
                    ));
                }
                if (!parsed.isEmpty()) letters.put(eventKey, parsed);
            }
        }

        return new TownData(
                def.id.trim(),
                def.displayName != null ? def.displayName : def.id,
                def.description != null ? def.description : "",
                Math.min(10, Math.max(1, def.distance)),
                type,
                Collections.emptySet(),   // legacy needs set — now expressed via needLevels
                Collections.emptySet(),   // legacy surplus set — now expressed via needLevels
                specialtyItems,
                Math.max(1, def.minTraderLevel),
                needLevels,
                new HashMap<>(),   // supplyLevels  (runtime, not persisted in JSON)
                new HashMap<>(),   // previousSupplyLevels (runtime)
                letters
        );
    }

    // -----------------------------------------------------------------------
    // Internal index POJO

    private static class TownIndex {
        List<String> towns;
    }
}
