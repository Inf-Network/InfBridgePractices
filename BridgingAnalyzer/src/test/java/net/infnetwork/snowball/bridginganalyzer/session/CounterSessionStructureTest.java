package net.infnetwork.snowball.bridginganalyzer.session;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import java.lang.reflect.Field;
import java.util.Arrays;
import java.util.HashMap;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.junit.jupiter.api.Test;
import net.infnetwork.snowball.bridginganalyzer.BridgingAnalyzer;
import net.infnetwork.snowball.bridginganalyzer.Counter;

class CounterSessionStructureTest {
    @Test
    void counterMustNeverRetainPlayerEntityAcrossReconnects() {
        boolean retainsPlayer = Arrays.stream(Counter.class.getDeclaredFields())
                .map(Field::getType)
                .anyMatch(Player.class::isAssignableFrom);

        assertFalse(retainsPlayer,
                "Counter retaining Player recreates the reconnect-to-old-entity void lock");
    }

    @Test
    void legacyPublicDescriptorsRemainBinaryCompatible() throws ReflectiveOperationException {
        assertNotNull(Counter.class.getDeclaredConstructor(Player.class));
        assertNotNull(Counter.class.getDeclaredMethod("setCheckPoint", Location.class));
        assertNotNull(Counter.class.getDeclaredMethod("teleportCheckPoint"));
        assertNotNull(Counter.class.getDeclaredMethod("vectoryBreakBlock"));
        assertEquals(HashMap.class,
                BridgingAnalyzer.class.getDeclaredMethod("getCounters").getReturnType());
    }
}
