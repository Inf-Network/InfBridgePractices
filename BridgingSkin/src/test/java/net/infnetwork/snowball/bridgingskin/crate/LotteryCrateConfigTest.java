package net.infnetwork.snowball.bridgingskin.crate;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import java.util.Map;
import org.bukkit.configuration.file.YamlConfiguration;
import org.junit.jupiter.api.Test;

class LotteryCrateConfigTest {
    @Test
    void migratesLegacySingleCrateToList() throws Exception {
        YamlConfiguration config = new YamlConfiguration();
        config.set("lottery.crate.world", "world");
        config.set("lottery.crate.x", 12);
        config.set("lottery.crate.y", 64);
        config.set("lottery.crate.z", -5);

        LotteryCrateConfig.LoadResult loaded = LotteryCrateConfig.load(config);

        assertTrue(loaded.changed());
        assertEquals(List.of(new LotteryCrate("world", 12, 64, -5)), loaded.crates());

        LotteryCrateConfig.write(config, loaded.crates());
        YamlConfiguration reloaded = new YamlConfiguration();
        reloaded.loadFromString(config.saveToString());

        assertFalse(reloaded.contains("lottery.crate"));
        assertEquals(loaded.crates(), LotteryCrateConfig.load(reloaded).crates());
    }

    @Test
    void mergesLegacyCrateWithoutDuplicatingExistingListEntry() {
        YamlConfiguration config = new YamlConfiguration();
        config.set("lottery.crates", List.of(
                Map.of("world", "world", "x", 1, "y", 2, "z", 3),
                Map.of("world", "world_nether", "x", -4, "y", 70, "z", 8)));
        config.set("lottery.crate.world", "world");
        config.set("lottery.crate.x", 1);
        config.set("lottery.crate.y", 2);
        config.set("lottery.crate.z", 3);

        LotteryCrateConfig.LoadResult loaded = LotteryCrateConfig.load(config);

        assertTrue(loaded.changed());
        assertEquals(List.of(
                new LotteryCrate("world", 1, 2, 3),
                new LotteryCrate("world_nether", -4, 70, 8)), loaded.crates());
    }

    @Test
    void ignoresMalformedAndDuplicateListEntries() {
        YamlConfiguration config = new YamlConfiguration();
        config.set("lottery.crates", List.of(
                Map.of("world", "world", "x", 1, "y", 2, "z", 3),
                Map.of("world", "world", "x", 1, "y", 2, "z", 3),
                Map.of("world", "", "x", 5, "y", 6, "z", 7),
                Map.of("world", "world", "x", "not-a-number", "y", 6, "z", 7)));

        LotteryCrateConfig.LoadResult loaded = LotteryCrateConfig.load(config);

        assertTrue(loaded.changed());
        assertEquals(List.of(new LotteryCrate("world", 1, 2, 3)), loaded.crates());
    }
}
