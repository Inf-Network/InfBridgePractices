package net.infnetwork.snowball.blocklv.core;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.Test;

class LevelThresholdsTest {
    @Test
    void preservesTheExistingFormulaAtNormalLevels() {
        assertEquals(1L, LevelThresholds.threshold(0L));
        assertEquals(21L, LevelThresholds.threshold(1L));
        assertEquals(497L, LevelThresholds.threshold(24L));
        assertEquals(20_668L, LevelThresholds.threshold(1_000L));
    }

    @Test
    void saturatesBeforeTheExponentialTermWouldOverflow() {
        assertEquals(
                4_611_686_018_428_669_236L,
                LevelThresholds.threshold(62_000L));
        assertEquals(Long.MAX_VALUE, LevelThresholds.threshold(63_000L));
        assertEquals(Long.MAX_VALUE, LevelThresholds.threshold(Long.MAX_VALUE));
    }

    @Test
    void rejectsNegativeLevels() {
        assertThrows(IllegalArgumentException.class, () -> LevelThresholds.threshold(-1L));
    }
}
