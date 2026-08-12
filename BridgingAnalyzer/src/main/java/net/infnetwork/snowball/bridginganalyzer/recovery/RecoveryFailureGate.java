package net.infnetwork.snowball.bridginganalyzer.recovery;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/** Counts consecutive position-recovery failures and opens a terminal fallback gate. */
final class RecoveryFailureGate {
    private final int maxAttempts;
    private final Map<UUID, Integer> attempts = new HashMap<>();

    RecoveryFailureGate(int maxAttempts) {
        if (maxAttempts < 1) {
            throw new IllegalArgumentException("maxAttempts must be positive");
        }
        this.maxAttempts = maxAttempts;
    }

    boolean recordFailure(UUID playerId) {
        int failures = attempts.merge(playerId, 1, Integer::sum);
        if (failures < maxAttempts) {
            return false;
        }
        attempts.remove(playerId);
        return true;
    }

    void recovered(UUID playerId) {
        attempts.remove(playerId);
    }

    void clear() {
        attempts.clear();
    }
}
