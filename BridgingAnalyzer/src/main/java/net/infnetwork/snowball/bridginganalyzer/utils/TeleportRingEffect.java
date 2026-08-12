package net.infnetwork.snowball.bridginganalyzer.utils;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.scheduler.BukkitTask;
import net.infnetwork.snowball.bridginganalyzer.BridgingAnalyzer;
import org.bukkit.Particle;

public abstract class TeleportRingEffect
implements Runnable {
    int circleElements = 40;
    double radius = 1.0;
    private int round;
    private Particle type = Particle.WITCH;
    private Location loc;
    private Location target;
    private BukkitTask task;
    private int dela;
    private int cr = 0;
    private int de;

    private TeleportRingEffect(Location centerLoc, Location target, int dela, int round) {
        this.round = round;
        this.target = target;
        this.loc = centerLoc;
        this.dela = dela;
    }

    /**
     * Compatibility constructor retained for extensions that override {@link #onFinish()}.
     * Bukkit only queues the runnable for a later server tick and never runs it inline.
     */
    @SuppressWarnings("this-escape")
    public TeleportRingEffect(Location centerLoc, Location target, long delay, int dela,
                              int round) {
        this(centerLoc, target, dela, round);
        this.start(delay);
    }

    /** Creates and starts an effect only after its state is fully initialized. */
    public static void play(Location centerLoc, Location target, long delay, int dela,
                            int round, Runnable onFinish) {
        TeleportRingEffect effect = new CallbackEffect(
                centerLoc, target, dela, round, onFinish);
        effect.start(delay);
    }

    private void start(long delay) {
        this.task = Bukkit.getScheduler().runTaskTimer(
                BridgingAnalyzer.getInstance(), this, delay, delay);
    }

    public abstract void onFinish();

    @Override
    public void run() {
        ++this.cr;
        if (this.cr > this.round) {
            ++this.de;
            if (this.de > this.dela) {
                this.task.cancel();
                this.onFinish();
            }
            return;
        }
        Location centerLoc = this.loc.clone();
        Location targetLoc = this.target.clone();
        for (int i = 0; i < this.circleElements; ++i) {
            double alpha = 360.0 / (double)this.circleElements * (double)i;
            double x = this.radius * Math.sin(Math.toRadians(alpha));
            double z = this.radius * Math.cos(Math.toRadians(alpha));
            Location particle = new Location(centerLoc.getWorld(), centerLoc.getX() + x, centerLoc.getY(), centerLoc.getZ() + z);
            particle.getWorld().spawnParticle(this.type, particle, 1, 0.0, 0.0, 0.0, 0.0);
            Location particlet = new Location(targetLoc.getWorld(), targetLoc.getX() + x, targetLoc.getY(), targetLoc.getZ() + z);
            particlet.getWorld().spawnParticle(this.type, particlet, 1, 0.0, 0.0, 0.0, 0.0);
        }
    }

    private static final class CallbackEffect extends TeleportRingEffect {
        private final Runnable callback;

        private CallbackEffect(Location centerLoc, Location target, int dela, int round,
                               Runnable callback) {
            super(centerLoc, target, dela, round);
            this.callback = callback;
        }

        @Override
        public void onFinish() {
            this.callback.run();
        }
    }
}
