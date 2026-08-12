package net.infnetwork.snowball.bridgingskin.crate;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.bukkit.configuration.file.FileConfiguration;

/** Reads and writes the crate configuration, including the legacy single-crate format. */
final class LotteryCrateConfig {
    static final String CRATES_PATH = "lottery.crates";
    static final String LEGACY_CRATE_PATH = "lottery.crate";

    private LotteryCrateConfig() {
    }

    static LoadResult load(FileConfiguration config) {
        Set<LotteryCrate> crates = new LinkedHashSet<>();
        boolean changed = false;

        if (config.contains(CRATES_PATH)) {
            List<Map<?, ?>> configured = new ArrayList<>(config.getMapList(CRATES_PATH));
            for (Map<?, ?> entry : configured) {
                LotteryCrate crate = parse(entry);
                if (crate == null || !crates.add(crate)) {
                    changed = true;
                }
            }
        }

        if (config.contains(LEGACY_CRATE_PATH)) {
            LotteryCrate legacy = readLegacy(config);
            if (legacy != null) {
                crates.add(legacy);
            }
            // The legacy section is removed even when it duplicates an entry in the new list.
            changed = true;
        }

        return new LoadResult(List.copyOf(crates), changed);
    }

    static void write(FileConfiguration config, List<LotteryCrate> crates) {
        List<Map<String, Object>> serialized = crates.stream().map(LotteryCrateConfig::serialize).toList();
        config.set(CRATES_PATH, serialized);
        config.set(LEGACY_CRATE_PATH, null);
    }

    private static LotteryCrate readLegacy(FileConfiguration config) {
        String world = normalizedWorld(config.getString(LEGACY_CRATE_PATH + ".world"));
        if (world == null
                || !config.contains(LEGACY_CRATE_PATH + ".x")
                || !config.contains(LEGACY_CRATE_PATH + ".y")
                || !config.contains(LEGACY_CRATE_PATH + ".z")) {
            return null;
        }
        return new LotteryCrate(world,
                config.getInt(LEGACY_CRATE_PATH + ".x"),
                config.getInt(LEGACY_CRATE_PATH + ".y"),
                config.getInt(LEGACY_CRATE_PATH + ".z"));
    }

    private static LotteryCrate parse(Map<?, ?> entry) {
        String world = normalizedWorld(entry.get("world"));
        Integer x = integer(entry.get("x"));
        Integer y = integer(entry.get("y"));
        Integer z = integer(entry.get("z"));
        if (world == null || x == null || y == null || z == null) {
            return null;
        }
        return new LotteryCrate(world, x, y, z);
    }

    private static Map<String, Object> serialize(LotteryCrate crate) {
        Map<String, Object> entry = new LinkedHashMap<>();
        entry.put("world", crate.worldName());
        entry.put("x", crate.x());
        entry.put("y", crate.y());
        entry.put("z", crate.z());
        return entry;
    }

    private static String normalizedWorld(Object value) {
        if (!(value instanceof String world)) {
            return null;
        }
        String normalized = world.trim();
        return normalized.isEmpty() ? null : normalized;
    }

    private static Integer integer(Object value) {
        if (value instanceof Integer integer) {
            return integer;
        }
        if (value instanceof Number number) {
            long integer = number.longValue();
            return integer < Integer.MIN_VALUE || integer > Integer.MAX_VALUE ? null : (int) integer;
        }
        if (value instanceof String string) {
            try {
                return Integer.valueOf(string.trim());
            } catch (NumberFormatException ignored) {
                return null;
            }
        }
        return null;
    }

    record LoadResult(List<LotteryCrate> crates, boolean changed) {
    }
}
