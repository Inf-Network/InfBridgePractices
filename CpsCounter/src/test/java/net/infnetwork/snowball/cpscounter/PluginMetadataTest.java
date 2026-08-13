package net.infnetwork.snowball.cpscounter;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import org.bukkit.configuration.file.YamlConfiguration;
import org.junit.jupiter.api.Test;

class PluginMetadataTest {
    @Test
    void declaresEveryPermissionUsedByTheCpsRuntime() throws Exception {
        try (InputStream stream = getClass().getClassLoader().getResourceAsStream("plugin.yml")) {
            assertNotNull(stream);
            YamlConfiguration plugin = YamlConfiguration.loadConfiguration(
                    new InputStreamReader(stream, StandardCharsets.UTF_8));

            assertEquals("op", plugin.getString("permissions.cpscounter.cps.default"));
            assertEquals("op", plugin.getString("permissions.cpscounter.bypass.default"));
            assertEquals("op", plugin.getString("permissions.cpscounter.bypasslimit.default"));
            assertEquals("op", plugin.getString("permissions.cpscounter.monitor.default"));
        }
    }
}
