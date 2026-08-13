package net.infnetwork.snowball.blocklv.commands;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

class ExperienceAdjustmentTest {
    @Test
    void addsExperienceAcrossLevelsWithoutIntermediateOverflow() {
        ExperienceAdjustment.Result first = ExperienceAdjustment.add(0L, 0L, 1L);
        ExperienceAdjustment.Result levelUp = ExperienceAdjustment.add(1L, 20L, 1L);
        ExperienceAdjustment.Result saturatedThreshold = ExperienceAdjustment.add(
                63_000L, Long.MAX_VALUE - 1L, 10L);

        assertEquals(1L, first.level());
        assertEquals(1L, first.experience());
        assertEquals(2L, levelUp.level());
        assertEquals(0L, levelUp.experience());
        assertEquals(63_001L, saturatedThreshold.level());
        assertEquals(9L, saturatedThreshold.experience());
    }

    @Test
    void rejectsProgressThatCannotBeRepresentedPastTheMaximumLevel() {
        ExperienceAdjustment.Result result = ExperienceAdjustment.add(
                Long.MAX_VALUE, Long.MAX_VALUE - 1L, 1L);

        assertEquals(ExperienceAdjustment.Status.OVERFLOW, result.status());
        assertEquals(Long.MAX_VALUE, result.level());
        assertEquals(Long.MAX_VALUE - 1L, result.experience());
    }

    @Test
    void subtractsCurrentExperienceAndClampsAtZero() {
        ExperienceAdjustment.Result reduced =
                ExperienceAdjustment.subtract(10L, 80L, 30L);
        ExperienceAdjustment.Result clamped =
                ExperienceAdjustment.subtract(10L, 80L, Long.MAX_VALUE);

        assertEquals(ExperienceAdjustment.Status.APPLIED, reduced.status());
        assertEquals(10L, reduced.level());
        assertEquals(80L, reduced.previousExperience());
        assertEquals(50L, reduced.experience());
        assertEquals(0L, clamped.level());
        assertEquals(0L, clamped.experience());
    }

    @Test
    void borrowsFromPreviousLevelWhenCurrentExperienceIsExhausted() {
        ExperienceAdjustment.Result result =
                ExperienceAdjustment.subtract(25L, 0L, 1L);

        assertEquals(ExperienceAdjustment.Status.APPLIED, result.status());
        assertEquals(24L, result.level());
        assertEquals(496L, result.experience());
    }

    @Test
    void rejectsNonPositiveAmountsAndInvalidStoredExperience() {
        assertEquals(
                ExperienceAdjustment.Status.NON_POSITIVE_AMOUNT,
                ExperienceAdjustment.subtract(10L, 80L, 0L).status());
        assertEquals(
                ExperienceAdjustment.Status.NON_POSITIVE_AMOUNT,
                ExperienceAdjustment.subtract(10L, 80L, -1L).status());
        assertEquals(
                ExperienceAdjustment.Status.INVALID_CURRENT_LEVEL,
                ExperienceAdjustment.subtract(-1L, 80L, 1L).status());
        assertEquals(
                ExperienceAdjustment.Status.INVALID_CURRENT_EXPERIENCE,
                ExperienceAdjustment.subtract(10L, -1L, 1L).status());
    }

    @Test
    void computesMaximumStoredExperienceWithoutOverflow() {
        assertEquals(20L, ExperienceAdjustment.maximumExperienceForLevel(1L));
        assertEquals(496L, ExperienceAdjustment.maximumExperienceForLevel(24L));
        assertEquals(
                Long.MAX_VALUE - 1L,
                ExperienceAdjustment.maximumExperienceForLevel(Long.MAX_VALUE));
    }

    @Test
    void matchesTheLevelingFormulaAcrossRepresentativeProgressStates() {
        long[] amounts = {1L, 20L, 21L, 100L, 1_000L, 100_000L};
        for (long level = 1L; level <= 80L; level++) {
            long maximum = ExperienceAdjustment.maximumExperienceForLevel(level);
            long[] experiences = {0L, maximum / 2L, maximum};
            for (long experience : experiences) {
                for (long amount : amounts) {
                    Expected expected = expectedAfterSubtract(level, experience, amount);
                    ExperienceAdjustment.Result actual =
                            ExperienceAdjustment.subtract(level, experience, amount);
                    assertEquals(expected.level(), actual.level());
                    assertEquals(expected.experience(), actual.experience());
                }
            }
        }
    }

    @Test
    void additionMatchesTheLevelingFormulaAcrossRepresentativeProgressStates() {
        long[] amounts = {1L, 20L, 21L, 100L, 1_000L, 100_000L};
        for (long level = 1L; level <= 80L; level++) {
            long maximum = ExperienceAdjustment.maximumExperienceForLevel(level);
            long[] experiences = {0L, maximum / 2L, maximum};
            for (long experience : experiences) {
                for (long amount : amounts) {
                    Expected expected = expectedAfterAdd(level, experience, amount);
                    ExperienceAdjustment.Result actual =
                            ExperienceAdjustment.add(level, experience, amount);
                    assertEquals(expected.level(), actual.level());
                    assertEquals(expected.experience(), actual.experience());
                }
            }
        }
    }

    private static Expected expectedAfterSubtract(
            long currentLevel,
            long currentExperience,
            long amount) {
        long total = currentExperience;
        for (long level = 1L; level < currentLevel; level++) {
            total += ExperienceAdjustment.maximumExperienceForLevel(level) + 1L;
        }
        long remaining = Math.max(0L, total - Math.min(total, amount));
        if (remaining == 0L) {
            return new Expected(0L, 0L);
        }

        long level = 1L;
        while (remaining >= ExperienceAdjustment.maximumExperienceForLevel(level) + 1L) {
            remaining -= ExperienceAdjustment.maximumExperienceForLevel(level) + 1L;
            level++;
        }
        return new Expected(level, remaining);
    }

    private static Expected expectedAfterAdd(
            long currentLevel,
            long currentExperience,
            long amount) {
        long level = currentLevel;
        long experience = currentExperience;
        long remaining = amount;
        while (remaining > 0L) {
            long needed = ExperienceAdjustment.maximumExperienceForLevel(level) + 1L
                    - experience;
            if (remaining < needed) {
                return new Expected(level, experience + remaining);
            }
            remaining -= needed;
            level++;
            experience = 0L;
        }
        return new Expected(level, experience);
    }

    private record Expected(long level, long experience) {
    }
}
