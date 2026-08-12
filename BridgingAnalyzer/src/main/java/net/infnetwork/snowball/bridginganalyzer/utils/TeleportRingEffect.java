/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  org.bukkit.Bukkit
 *  org.bukkit.Location
 *  org.bukkit.plugin.Plugin
 *  org.bukkit.scheduler.BukkitTask
 */
package net.infnetwork.snowball.bridginganalyzer.utils;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.plugin.Plugin;
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

    public TeleportRingEffect(Location centerLoc, Location target, long delay, int dela, int round) {
        this.round = round;
        this.target = target;
        this.loc = centerLoc;
        this.dela = dela;
        this.task = Bukkit.getScheduler().runTaskTimer((Plugin)BridgingAnalyzer.getInstance(), (Runnable)this, delay, delay);
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
}

