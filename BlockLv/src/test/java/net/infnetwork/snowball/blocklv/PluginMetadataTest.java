package net.infnetwork.snowball.blocklv;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import org.bukkit.configuration.file.YamlConfiguration;
import org.junit.jupiter.api.Test;

class PluginMetadataTest {
    @Test
    void declaresLevelAdjustmentPermissionsAndCommandUsage() throws Exception {
        try (InputStream stream = getClass().getClassLoader().getResourceAsStream("plugin.yml")) {
            assertNotNull(stream);
            YamlConfiguration plugin = YamlConfiguration.loadConfiguration(
                    new InputStreamReader(stream, StandardCharsets.UTF_8));

            assertEquals("op", plugin.getString("permissions.blocklv.addlevel.default"));
            assertEquals("op", plugin.getString("permissions.blocklv.decrease.default"));
            assertEquals("op", plugin.getString("permissions.blocklv.decreaselevel.default"));
            assertTrue(plugin.getString("commands.blocklv.usage", "").contains("addlevel"));
            assertTrue(plugin.getString("commands.blocklv.usage", "").contains("decrease"));
            assertTrue(plugin.getString("commands.blocklv.usage", "").contains("decreaselevel"));
        }
    }
}
