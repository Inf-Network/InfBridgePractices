package net.infnetwork.snowball.blocklv.commands;

import java.math.BigInteger;
import net.infnetwork.snowball.blocklv.core.LevelThresholds;

final class ExperienceAdjustment {
    private static final BigInteger LONG_MAX = BigInteger.valueOf(Long.MAX_VALUE);

    private ExperienceAdjustment() {
    }

    static Result add(long currentLevel, long currentExperience, long amount) {
        Status invalid = validate(currentLevel, currentExperience, amount);
        if (invalid != null) {
            return Result.rejected(invalid, currentLevel, currentExperience);
        }

        long baseLevel = currentLevel == 0L ? 1L : currentLevel;
        BigInteger available = BigInteger.valueOf(currentExperience)
                .add(BigInteger.valueOf(amount));
        long maximumCrossings = Long.MAX_VALUE - baseLevel;
        long low = 0L;
        long high = maximumCrossings;
        BigInteger comparisonCap = available.add(BigInteger.ONE);
        while (low < high) {
            long distance = high - low;
            long middle = low + (distance >>> 1) + (distance & 1L);
            BigInteger required = thresholdRange(
                    baseLevel, baseLevel + middle - 1L, comparisonCap);
            if (required.compareTo(available) <= 0) {
                low = middle;
            } else {
                high = middle - 1L;
            }
        }

        long updatedLevel = baseLevel + low;
        BigInteger crossed = low == 0L
                ? BigInteger.ZERO
                : thresholdRange(baseLevel, updatedLevel - 1L, comparisonCap);
        BigInteger updatedExperience = available.subtract(crossed);
        long maximum = maximumExperienceForLevel(updatedLevel);
        if (updatedExperience.compareTo(BigInteger.valueOf(maximum)) > 0) {
            return Result.rejected(Status.OVERFLOW, currentLevel, currentExperience);
        }
        return new Result(
                Status.APPLIED,
                currentLevel,
                updatedLevel,
                currentExperience,
                updatedExperience.longValueExact());
    }

    static Result subtract(long currentLevel, long currentExperience, long amount) {
        Status invalid = validate(currentLevel, currentExperience, amount);
        if (invalid != null) {
            return Result.rejected(invalid, currentLevel, currentExperience);
        }
        if (amount < currentExperience) {
            return new Result(
                    Status.APPLIED,
                    currentLevel,
                    currentLevel,
                    currentExperience,
                    currentExperience - amount);
        }
        if (amount == currentExperience) {
            long updatedLevel = currentLevel == 1L ? 0L : currentLevel;
            return new Result(
                    Status.APPLIED,
                    currentLevel,
                    updatedLevel,
                    currentExperience,
                    0L);
        }
        if (currentLevel <= 1L) {
            return new Result(
                    Status.APPLIED, currentLevel, 0L, currentExperience, 0L);
        }

        long remaining = amount - currentExperience;
        BigInteger required = BigInteger.valueOf(remaining);
        BigInteger allPreviousLevels = thresholdRange(
                1L, currentLevel - 1L, required.add(BigInteger.ONE));
        if (allPreviousLevels.compareTo(required) <= 0) {
            return new Result(
                    Status.APPLIED, currentLevel, 0L, currentExperience, 0L);
        }

        long low = 1L;
        long high = currentLevel - 1L;
        while (low < high) {
            long middle = low + ((high - low) >>> 1);
            long firstLevel = currentLevel - middle;
            BigInteger crossed = thresholdRange(
                    firstLevel, currentLevel - 1L, required);
            if (crossed.compareTo(required) >= 0) {
                high = middle;
            } else {
                low = middle + 1L;
            }
        }

        long updatedLevel = currentLevel - low;
        BigInteger exactCap = required.add(LONG_MAX).add(BigInteger.ONE);
        BigInteger crossed = thresholdRange(
                updatedLevel, currentLevel - 1L, exactCap);
        BigInteger updatedExperience = crossed.subtract(required);
        long storedExperience = updatedExperience.compareTo(LONG_MAX) > 0
                ? Long.MAX_VALUE
                : updatedExperience.longValueExact();
        return new Result(
                Status.APPLIED,
                currentLevel,
                updatedLevel,
                currentExperience,
                storedExperience);
    }

    static long maximumExperienceForLevel(long level) {
        if (level <= 0L) {
            return 0L;
        }
        BigInteger cap = LONG_MAX.add(BigInteger.ONE);
        BigInteger threshold = thresholdRange(level, level, cap);
        if (threshold.compareTo(LONG_MAX) > 0) {
            return Long.MAX_VALUE;
        }
        return Math.max(0L, threshold.longValueExact() - 1L);
    }

    private static Status validate(long level, long experience, long amount) {
        if (amount <= 0L) {
            return Status.NON_POSITIVE_AMOUNT;
        }
        if (level < 0L) {
            return Status.INVALID_CURRENT_LEVEL;
        }
        if (experience < 0L || experience > maximumExperienceForLevel(level)) {
            return Status.INVALID_CURRENT_EXPERIENCE;
        }
        return null;
    }

    private static BigInteger thresholdRange(
            long firstLevel,
            long lastLevel,
            BigInteger cap) {
        if (firstLevel > lastLevel) {
            return BigInteger.ZERO;
        }
        BigInteger result = BigInteger.ZERO;
        if (firstLevel < LevelThresholds.FIRST_SATURATED_LEVEL) {
            long exactLast = Math.min(
                    lastLevel, LevelThresholds.FIRST_SATURATED_LEVEL - 1L);
            result = exactThresholdRange(firstLevel, exactLast, cap);
        }
        if (lastLevel >= LevelThresholds.FIRST_SATURATED_LEVEL && result.compareTo(cap) < 0) {
            long saturatedFirst = Math.max(
                    firstLevel, LevelThresholds.FIRST_SATURATED_LEVEL);
            BigInteger count = BigInteger.valueOf(lastLevel - saturatedFirst + 1L);
            result = capped(result.add(count.multiply(LONG_MAX)), cap);
        }
        return result;
    }

    private static BigInteger exactThresholdRange(
            long firstLevel,
            long lastLevel,
            BigInteger cap) {
        BigInteger result = capped(
                integerRange(firstLevel, lastLevel).multiply(BigInteger.valueOf(20L)),
                cap);
        result = capped(result.add(
                floorThirdRange(firstLevel, lastLevel).multiply(BigInteger.TWO)), cap);
        result = capped(result.add(exponentialRange(firstLevel, lastLevel, cap)), cap);
        return result;
    }

    private static BigInteger integerRange(long first, long last) {
        BigInteger count = BigInteger.valueOf(last - first + 1L);
        BigInteger endpoints = BigInteger.valueOf(first).add(BigInteger.valueOf(last));
        return count.multiply(endpoints).divide(BigInteger.TWO);
    }

    private static BigInteger floorThirdRange(long first, long last) {
        return floorThirdPrefix(last).subtract(floorThirdPrefix(first - 1L));
    }

    private static BigInteger floorThirdPrefix(long value) {
        if (value <= 0L) {
            return BigInteger.ZERO;
        }
        long quotient = value / 3L;
        long remainder = value % 3L;
        BigInteger q = BigInteger.valueOf(quotient);
        BigInteger completeGroups = q.multiply(q.subtract(BigInteger.ONE))
                .multiply(BigInteger.valueOf(3L))
                .divide(BigInteger.TWO);
        return completeGroups.add(q.multiply(BigInteger.valueOf(remainder + 1L)));
    }

    private static BigInteger exponentialRange(
            long first,
            long last,
            BigInteger cap) {
        long firstGroup = first / 1000L;
        long lastGroup = last / 1000L;
        if (firstGroup == lastGroup) {
            return powerContribution(firstGroup, last - first + 1L, cap);
        }

        long firstCount = 1000L - (first % 1000L);
        long lastCount = (last % 1000L) + 1L;
        BigInteger result = powerContribution(firstGroup, firstCount, cap);
        result = capped(result.add(powerContribution(lastGroup, lastCount, cap)), cap);

        long fullFirst = firstGroup + 1L;
        long fullLast = lastGroup - 1L;
        if (fullFirst <= fullLast) {
            if (fullLast >= 64L) {
                return cap;
            }
            BigInteger powers = BigInteger.ONE.shiftLeft((int) (fullLast + 1L))
                    .subtract(BigInteger.ONE.shiftLeft((int) fullFirst));
            result = capped(result.add(powers.multiply(BigInteger.valueOf(1000L))), cap);
        }
        return result;
    }

    private static BigInteger powerContribution(
            long exponent,
            long count,
            BigInteger cap) {
        if (count <= 0L) {
            return BigInteger.ZERO;
        }
        if (exponent >= 64L) {
            return cap;
        }
        BigInteger contribution = BigInteger.ONE.shiftLeft((int) exponent)
                .multiply(BigInteger.valueOf(count));
        return capped(contribution, cap);
    }

    private static BigInteger capped(BigInteger value, BigInteger cap) {
        return value.compareTo(cap) >= 0 ? cap : value;
    }

    enum Status {
        APPLIED,
        NON_POSITIVE_AMOUNT,
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
