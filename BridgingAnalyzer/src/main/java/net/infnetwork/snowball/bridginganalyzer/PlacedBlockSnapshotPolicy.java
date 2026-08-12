package net.infnetwork.snowball.bridginganalyzer;

import java.util.Map;

/** Applies the original mutable-map matching semantics used by legacy extensions. */
final class PlacedBlockSnapshotPolicy {
    private PlacedBlockSnapshotPolicy() {
    }

    static <K, S> boolean matches(
            Map<K, S> legacySnapshots, K key, S currentState) {
        S expected = legacySnapshots.get(key);
        return expected != null && expected.equals(currentState);
    }
}
