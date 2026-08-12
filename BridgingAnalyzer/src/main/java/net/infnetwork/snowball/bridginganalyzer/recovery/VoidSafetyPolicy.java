package net.infnetwork.snowball.bridginganalyzer.recovery;

/** Pure failure-plane policy, kept separate so boundary behavior is unit-testable. */
public final class VoidSafetyPolicy {
    public static final double DEFAULT_FAILURE_HEIGHT = 0.0;

    private final double failureHeight;

    public VoidSafetyPolicy(double failureHeight) {
        if (!Double.isFinite(failureHeight)) {
            throw new IllegalArgumentException("failureHeight must be finite");
        }
        this.failureHeight = failureHeight;
    }

    public boolean isUnsafe(double y) {
        return !Double.isFinite(y) || y < failureHeight;
    }

    public double failureHeight() {
        return failureHeight;
    }
}
