package net.infnetwork.snowball.blocklv.commands;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

class LevelAdjustmentTest {
    @Test
    void appliesPositiveChangesAndPreservesExperience() {
        LevelAdjustment.Result result = LevelAdjustment.add(100L, 50L, 25L);

        assertEquals(LevelAdjustment.Status.APPLIED, result.status());
        assertEquals(100L, result.previousLevel());
        assertEquals(125L, result.level());
        assertEquals(50L, result.previousExperience());
        assertEquals(50L, result.experience());
    }

    @Test
    void rejectsNonPositiveDeltaInvalidCurrentLevelAndLongOverflow() {
        assertEquals(
                LevelAdjustment.Status.NON_POSITIVE_DELTA,
                LevelAdjustment.add(10L, 0L, 0L).status());
        assertEquals(
                LevelAdjustment.Status.NON_POSITIVE_DELTA,
                LevelAdjustment.add(10L, 0L, -11L).status());
        assertEquals(
                LevelAdjustment.Status.INVALID_CURRENT_LEVEL,
                LevelAdjustment.add(-1L, 0L, 1L).status());
        assertEquals(
                LevelAdjustment.Status.OVERFLOW,
                LevelAdjustment.add(Long.MAX_VALUE, 0L, 1L).status());
        assertEquals(
                LevelAdjustment.Status.INVALID_CURRENT_EXPERIENCE,
                LevelAdjustment.add(10L, -1L, 1L).status());
    }

    @Test
    void supportsLevelsBeyondTheMinecraftHudIntegerRange() {
        long expected = (long) Integer.MAX_VALUE + 1_000L;

        LevelAdjustment.Result result =
                LevelAdjustment.add(Integer.MAX_VALUE, 1L, 1_000L);

        assertEquals(LevelAdjustment.Status.APPLIED, result.status());
        assertEquals(expected, result.level());
        assertEquals(1L, result.experience());
    }

    @Test
    void subtractsLevelsWithoutChangingExperienceAndClampsAtZero() {
        LevelAdjustment.Result reduced = LevelAdjustment.subtract(25L, 71L, 5L);
        LevelAdjustment.Result clamped = LevelAdjustment.subtract(25L, 71L, 100L);

        assertEquals(LevelAdjustment.Status.APPLIED, reduced.status());
        assertEquals(20L, reduced.level());
        assertEquals(71L, reduced.experience());
        assertEquals(0L, clamped.level());
        assertEquals(0L, clamped.experience());
    }

    @Test
    void rejectsInvalidLevelSubtractions() {
        assertEquals(
                LevelAdjustment.Status.NON_POSITIVE_DELTA,
                LevelAdjustment.subtract(10L, 3L, 0L).status());
        assertEquals(
                LevelAdjustment.Status.NON_POSITIVE_DELTA,
                LevelAdjustment.subtract(10L, 3L, -1L).status());
        assertEquals(
                LevelAdjustment.Status.INVALID_CURRENT_LEVEL,
                LevelAdjustment.subtract(-1L, 3L, 1L).status());
        assertEquals(
                LevelAdjustment.Status.INVALID_CURRENT_EXPERIENCE,
                LevelAdjustment.subtract(10L, -1L, 1L).status());
    }

    @Test
    void clampsExperienceToTheNewLevelsValidRange() {
        LevelAdjustment.Result result = LevelAdjustment.subtract(25L, 500L, 24L);

        assertEquals(LevelAdjustment.Status.APPLIED, result.status());
        assertEquals(1L, result.level());
        assertEquals(20L, result.experience());
    }
}
