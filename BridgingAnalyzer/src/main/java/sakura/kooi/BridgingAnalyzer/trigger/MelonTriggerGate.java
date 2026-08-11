package sakura.kooi.BridgingAnalyzer.trigger;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Per-player rising-edge gate for melon surfaces.
 *
 * <p>A player must leave the surface and wait for the lock to expire before a
 * later entry can activate it again. Damage invulnerability is intentionally
 * not part of this state machine.</p>
 */
public final class MelonTriggerGate {
    public static final long NO_ACTIVATION = -1L;

    private final long lockTicks;
    private final Map<UUID, State> states = new HashMap<>();
    private long nextToken = 1L;

    public MelonTriggerGate(long lockTicks) {
        if (lockTicks < 0L) {
            throw new IllegalArgumentException("lockTicks must not be negative");
        }
        this.lockTicks = lockTicks;
    }

    public long update(UUID playerId, boolean onMelon, long currentTick) {
        State state = states.computeIfAbsent(playerId, ignored -> new State());
        if (!onMelon) {
            state.onMelon = false;
            return NO_ACTIVATION;
        }
        if (state.onMelon) {
            return NO_ACTIVATION;
        }

        state.onMelon = true;
        if (currentTick < state.lockedUntilTick) {
            return NO_ACTIVATION;
        }

        state.lockedUntilTick = currentTick + lockTicks;
        state.token = nextToken++;
        return state.token;
    }

    public boolean isCurrent(UUID playerId, long token) {
        State state = states.get(playerId);
        return state != null && state.token == token;
    }

    public void remove(UUID playerId) {
        states.remove(playerId);
    }

    public void clear() {
        states.clear();
    }

    private static final class State {
        private boolean onMelon;
        private long lockedUntilTick;
        private long token;
    }
}
