package net.infnetwork.snowball.blocklv.holographicdisplay;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

class LvTest {
    @Test
    void preservesLongDatabaseLevelsWithoutBreakingLegacyIntField() {
        long level = (long) Integer.MAX_VALUE + 1000L;
        Lv entry = new Lv(level, "player");

        assertEquals(level, entry.level());
        assertEquals(Integer.MAX_VALUE, entry.lv);
    }

    @Test
    void legacyMutableIntFieldRemainsAuthoritativeForLegacyInstances() {
        Lv entry = new Lv();
        entry.lv = 1234;

        assertEquals(1234L, entry.level());
    }

    @Test
    void changingLegacyFieldOverridesDatabaseConstructorMirror() {
        Lv entry = new Lv(1234L, "player");
        entry.lv = 42;

        assertEquals(42L, entry.level());
    }
}
