package com.offtomarket.mod.content;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.offtomarket.mod.OffToMarket;
import com.offtomarket.mod.data.NeedLevel;
import com.offtomarket.mod.data.TownData;
import com.offtomarket.mod.data.TownLetter;
import com.offtomarket.mod.data.TownRegistry;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.fml.loading.FMLPaths;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.stream.Stream;

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

    /** Load every town listed in _index.json and any custom JSON towns in config/offtomarket/towns/. */
    public static void loadAll() {
        List<String> files = loadIndex();
        int loaded = 0;
        if (files.isEmpty()) {
            OffToMarket.LOGGER.warn("[TownLoader] _index.json is empty or not found — no classpath towns loaded!");
        } else {
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
        }

        // Also load custom towns placed in config/offtomarket/towns/
        try {
            Path customTownsDir = FMLPaths.CONFIGDIR.get().resolve("offtomarket").resolve("towns");
            if (Files.exists(customTownsDir) && Files.isDirectory(customTownsDir)) {
                try (Stream<Path> stream = Files.list(customTownsDir)) {
                    for (Path p : stream.filter(p -> p.getFileName().toString().endsWith(".json")).toList()) {
                        String fileName = p.getFileName().toString();
                        if (fileName.startsWith("_")) continue;
                        try (Reader reader = Files.newBufferedReader(p, StandardCharsets.UTF_8)) {
                            TownDefinition def = GSON.fromJson(reader, TownDefinition.class);
                            TownData customTown = buildTownData(def, p.toString());
                            if (customTown != null) {
                                TownRegistry.register(customTown);
                                loaded++;
                                OffToMarket.LOGGER.info("[TownLoader] Loaded custom town '{}' from config: {}", customTown.getId(), fileName);
                            }
                        } catch (Exception ex) {
                            OffToMarket.LOGGER.error("[TownLoader] Failed to load custom town file '{}': {}", fileName, ex.getMessage());
                        }
                    }
                }
            }
        } catch (Exception e) {
            OffToMarket.LOGGER.warn("[TownLoader] Could not scan config towns directory: {}", e.getMessage());
        }

        OffToMarket.LOGGER.info("[TownLoader] Loaded {} total town(s) from JSON definitions.", loaded);
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

        // Build mutable needs and surplus sets (supports dynamic additions & modded compatibility)
        Set<ResourceLocation> needsSet = new LinkedHashSet<>();
        Set<ResourceLocation> surplusSet = new LinkedHashSet<>();

        // Populate from explicit JSON needs / surplus arrays if defined
        if (def.needs != null) {
            for (String n : def.needs) {
                if (n != null && !n.isBlank()) needsSet.add(new ResourceLocation(n.trim()));
            }
        }
        if (def.surplus != null) {
            for (String s : def.surplus) {
                if (s != null && !s.isBlank()) surplusSet.add(new ResourceLocation(s.trim()));
            }
        }

        // Also mirror graduated needLevels into needs/surplus sets
        for (Map.Entry<String, NeedLevel> entry : needLevels.entrySet()) {
            ResourceLocation rl = new ResourceLocation(entry.getKey());
            if (entry.getValue() == NeedLevel.DESPERATE || entry.getValue() == NeedLevel.HIGH_NEED) {
                needsSet.add(rl);
            } else if (entry.getValue() == NeedLevel.SURPLUS || entry.getValue() == NeedLevel.OVERSATURATED) {
                surplusSet.add(rl);
            }
        }

        return new TownData(
                def.id.trim(),
                def.displayName != null ? def.displayName : def.id,
                def.description != null ? def.description : "",
                Math.min(10, Math.max(1, def.distance)),
                type,
                needsSet,
                surplusSet,
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
