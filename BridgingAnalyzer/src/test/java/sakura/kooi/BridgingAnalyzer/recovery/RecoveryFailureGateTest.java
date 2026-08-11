package sakura.kooi.BridgingAnalyzer.recovery;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.UUID;
import org.junit.jupiter.api.Test;

class RecoveryFailureGateTest {
    @Test
    void opensTerminalFallbackAfterThreeConsecutiveFailures() {
        RecoveryFailureGate gate = new RecoveryFailureGate(3);
        UUID player = UUID.randomUUID();

        assertFalse(gate.recordFailure(player));
        assertFalse(gate.recordFailure(player));
        assertTrue(gate.recordFailure(player));
    }

    @Test
    void successfulRecoveryResetsFailureCount() {
        RecoveryFailureGate gate = new RecoveryFailureGate(3);
        UUID player = UUID.randomUUID();

        assertFalse(gate.recordFailure(player));
        assertFalse(gate.recordFailure(player));
        gate.recovered(player);
        assertFalse(gate.recordFailure(player));
        assertFalse(gate.recordFailure(player));
        assertTrue(gate.recordFailure(player));
    }
}
