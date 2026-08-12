package net.infnetwork.snowball.bridginganalyzer;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.HashMap;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class PlacedBlockSnapshotPolicyTest {
    private static final String KEY = "world:1:2:3";
    private static final String LEGACY_STATE = "sandstone:0";

    private final Map<String, String> legacy = new HashMap<>();

    @BeforeEach
    void rememberInternalSnapshot() {
        legacy.put(KEY, LEGACY_STATE);
    }

    @Test
    void legacyRemoveInvalidatesTheInternalSnapshot() {
        legacy.remove(KEY);

        assertFalse(matches(LEGACY_STATE));
    }

    @Test
    void legacyClearInvalidatesTheInternalSnapshot() {
        legacy.clear();

        assertFalse(matches(LEGACY_STATE));
    }

    @Test
    void legacyReplacementMustMatchTheLiveLegacyState() {
        legacy.put(KEY, "sandstone:1");

        assertFalse(matches(LEGACY_STATE));
    }

    @Test
    void matchingLegacyStateRemainsValid() {
        assertTrue(matches(LEGACY_STATE));
    }

    @Test
    void legacyPutCanAcceptTheCurrentLiveState() {
        legacy.clear();
        legacy.put(KEY, LEGACY_STATE);

        assertTrue(matches(LEGACY_STATE));
    }

    private boolean matches(String currentLegacyState) {
        return PlacedBlockSnapshotPolicy.matches(legacy, KEY, currentLegacyState);
    }
}
