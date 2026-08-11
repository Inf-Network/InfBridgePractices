package sakura.kooi.BridgingAnalyzer.recovery;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class VoidSafetyPolicyTest {
    private final VoidSafetyPolicy policy = new VoidSafetyPolicy(0.0);

    @Test
    void treatsOnlyCoordinatesBelowFailurePlaneAsUnsafe() {
        assertFalse(policy.isUnsafe(0.0));
        assertFalse(policy.isUnsafe(0.001));
        assertTrue(policy.isUnsafe(-0.001));
        assertTrue(policy.isUnsafe(-4773.704));
    }

    @Test
    void rejectsNonFiniteCoordinates() {
        assertTrue(policy.isUnsafe(Double.NaN));
        assertTrue(policy.isUnsafe(Double.POSITIVE_INFINITY));
        assertTrue(policy.isUnsafe(Double.NEGATIVE_INFINITY));
    }
}
