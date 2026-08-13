package net.infnetwork.snowball.blocklv.core;

/** Canonical, overflow-safe experience threshold used by every level operation. */
public final class LevelThresholds {
    public static final long FIRST_SATURATED_LEVEL = 63_000L;

    private LevelThresholds() {
    }

    public static long threshold(long level) {
        if (level < 0L) {
            throw new IllegalArgumentException("level must not be negative");
        }
        if (level >= FIRST_SATURATED_LEVEL) {
            return Long.MAX_VALUE;
        }

        int exponent = (int) (level / 1000L);
        long exponential = 1L << exponent;
        return level * 20L + (level / 3L) * 2L + exponential;
    }
}
