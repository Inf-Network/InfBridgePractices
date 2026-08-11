package sakura.kooi.BridgingAnalyzer.targets;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class PracticeTargetListenerTest {
    @Test
    void evenMinimalComboDamageIsReplacedWithLethalDamage() {
        double lethalDamage = PracticeTargetListener.lethalDamage(1.0, 0.0);

        assertEquals(2.0, lethalDamage);
        assertTrue(lethalDamage > 1.0);
        assertTrue(lethalDamage > 0.01);
    }

    @Test
    void absorptionCannotPreventTargetKill() {
        assertTrue(PracticeTargetListener.lethalDamage(1.0, 40.0) > 41.0);
    }
}
