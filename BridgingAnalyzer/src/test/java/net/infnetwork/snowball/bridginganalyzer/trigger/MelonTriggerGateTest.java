package net.infnetwork.snowball.bridginganalyzer.trigger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.UUID;
import org.junit.jupiter.api.Test;

class MelonTriggerGateTest {
    private static final long LOCK_TICKS = 27L;

    @Test
    void firesOnceForContinuousContactAcrossMelonBlocks() {
        MelonTriggerGate gate = new MelonTriggerGate(LOCK_TICKS);
        UUID player = UUID.randomUUID();

        long token = gate.update(player, true, 100L);

        assertNotEquals(MelonTriggerGate.NO_ACTIVATION, token);
        assertEquals(MelonTriggerGate.NO_ACTIVATION, gate.update(player, true, 101L));
        assertEquals(MelonTriggerGate.NO_ACTIVATION, gate.update(player, true, 120L));
        assertTrue(gate.isCurrent(player, token));
    }

    @Test
    void requiresExitAndExpiredLockBeforeRearming() {
        MelonTriggerGate gate = new MelonTriggerGate(LOCK_TICKS);
        UUID player = UUID.randomUUID();

        long first = gate.update(player, true, 100L);
        gate.update(player, false, 105L);
        assertEquals(MelonTriggerGate.NO_ACTIVATION, gate.update(player, true, 110L));

        gate.update(player, false, 120L);
        long second = gate.update(player, true, 127L);
        assertNotEquals(MelonTriggerGate.NO_ACTIVATION, second);
        assertNotEquals(first, second);
    }

    @Test
    void removingPlayerInvalidatesPendingToken() {
        MelonTriggerGate gate = new MelonTriggerGate(LOCK_TICKS);
        UUID player = UUID.randomUUID();
        long token = gate.update(player, true, 100L);

        gate.remove(player);

        assertFalse(gate.isCurrent(player, token));
    }
}
