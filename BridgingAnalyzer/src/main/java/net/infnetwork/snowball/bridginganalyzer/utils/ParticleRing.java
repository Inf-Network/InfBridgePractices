package net.infnetwork.snowball.bridginganalyzer.utils;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Particle;
import net.infnetwork.snowball.bridginganalyzer.BridgingAnalyzer;

/**
 * Legacy subclassable particle ring plus a two-phase callback API for internal use.
 */
public abstract class ParticleRing {

    private static final int CIRCLE_ELEMENTS = 20;

    private static final double RADIUS = 1.0;

    /**
     * Compatibility constructor retained for extensions that override {@link #onFinish()}.
     * Bukkit only queues this callback for a later server tick; it is never invoked inline.
     */
    @SuppressWarnings("this-escape")
    public ParticleRing(Location centerLoc, Particle type, long delay) {
        draw(centerLoc, type);
        Bukkit.getScheduler().runTaskLater(
                BridgingAnalyzer.getInstance(), this::onFinish, delay);
    }

    /** Starts the internal callback form without publishing a partially built subclass. */
    public static void play(Location centerLoc, Particle type, long delay, Runnable onFinish) {
        draw(centerLoc, type);
        Bukkit.getScheduler().runTaskLater(
                BridgingAnalyzer.getInstance(), onFinish, delay);
    }

    private static void draw(Location centerLoc, Particle type) {
        for (int i = 0; i < CIRCLE_ELEMENTS; ++i) {
            double alpha = 360.0 / (double) CIRCLE_ELEMENTS * (double) i;
            double x = RADIUS * Math.sin(Math.toRadians(alpha));
            double z = RADIUS * Math.cos(Math.toRadians(alpha));
            Location particle = new Location(centerLoc.getWorld(),
                    centerLoc.getX() + x, centerLoc.getY(), centerLoc.getZ() + z);
            centerLoc.getWorld().spawnParticle(type, particle, 1, 0.0, 0.0, 0.0, 0.0);
        }
    }

    public abstract void onFinish();
}
