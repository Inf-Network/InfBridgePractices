package sakura.kooi.BridgingAnalyzer.targets;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class PracticeTargetServiceTest {
    @Test
    void persistentMarkerIdentifiesNewVillagerTarget() {
        assertTrue(PracticeTargetService.matchesIdentity(
                true, true, null, true, 20.0, false));
    }

    @Test
    void strictFingerprintAdoptsLegacyTarget() {
        assertTrue(PracticeTargetService.matchesIdentity(
                true, false, "靶子", false, 1.0, true));
    }

    @Test
    void renamedOrdinaryVillagerIsNotATarget() {
        assertFalse(PracticeTargetService.matchesIdentity(
                true, false, "靶子", true, 20.0, true));
    }

    @Test
    void markerOnAnotherEntityTypeIsIgnored() {
        assertFalse(PracticeTargetService.matchesIdentity(
                false, true, "靶子", false, 1.0, true));
    }
}
