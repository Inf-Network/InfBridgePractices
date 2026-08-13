package net.infnetwork.snowball.blocklv.commands;

final class LevelAdjustment {
    private LevelAdjustment() {
    }

    static Result add(long currentLevel, long currentExperience, long delta) {
        if (delta <= 0L) {
            return Result.rejected(
                    Status.NON_POSITIVE_DELTA, currentLevel, currentExperience);
        }
        if (currentLevel < 0L) {
            return Result.rejected(
                    Status.INVALID_CURRENT_LEVEL, currentLevel, currentExperience);
        }
        if (currentExperience < 0L
                || currentExperience
                        > ExperienceAdjustment.maximumExperienceForLevel(currentLevel)) {
            return Result.rejected(
                    Status.INVALID_CURRENT_EXPERIENCE, currentLevel, currentExperience);
        }

        long updatedLevel;
        try {
            updatedLevel = Math.addExact(currentLevel, delta);
        } catch (ArithmeticException overflow) {
            return Result.rejected(Status.OVERFLOW, currentLevel, currentExperience);
        }
        return new Result(
                Status.APPLIED,
                currentLevel,
                updatedLevel,
                currentExperience,
                currentExperience);
    }

    static Result subtract(long currentLevel, long currentExperience, long delta) {
        if (delta <= 0L) {
            return Result.rejected(
                    Status.NON_POSITIVE_DELTA, currentLevel, currentExperience);
        }
        if (currentLevel < 0L) {
            return Result.rejected(
                    Status.INVALID_CURRENT_LEVEL, currentLevel, currentExperience);
        }
        if (currentExperience < 0L
                || currentExperience
                        > ExperienceAdjustment.maximumExperienceForLevel(currentLevel)) {
            return Result.rejected(
                    Status.INVALID_CURRENT_EXPERIENCE, currentLevel, currentExperience);
        }

        long updatedLevel = delta >= currentLevel ? 0L : currentLevel - delta;
        long updatedExperience = Math.min(
                currentExperience,
                ExperienceAdjustment.maximumExperienceForLevel(updatedLevel));
        return new Result(
                Status.APPLIED,
                currentLevel,
                updatedLevel,
                currentExperience,
                updatedExperience);
    }

    enum Status {
        APPLIED,
        NON_POSITIVE_DELTA,
        INVALID_CURRENT_LEVEL,
        INVALID_CURRENT_EXPERIENCE,
        OVERFLOW
    }

    record Result(
            Status status,
            long previousLevel,
            long level,
            long previousExperience,
            long experience) {
        static Result rejected(Status status, long level, long experience) {
            return new Result(status, level, level, experience, experience);
        }
    }
}
