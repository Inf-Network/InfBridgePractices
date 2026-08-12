package net.infnetwork.snowball.bridginganalyzer.utils;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.plugin.Plugin;
import net.infnetwork.snowball.bridginganalyzer.BridgingAnalyzer;

/**
 * 在指定位置画一圈粒子,延迟若干 tick 后回调 {@link #onFinish()}。
 *
 * 用于踩下功能方块后的视觉反馈:先出现光环,等光环播完再执行实际动作
 * (设置出生点 / 传送 / 完成练习)。
 */
public abstract class ParticleRing {

    private static final int CIRCLE_ELEMENTS = 20;

    private static final double RADIUS = 1.0;

    /**
     * @param centerLoc 圆心
     * @param type      粒子类型
     * @param delay     延迟多少 tick 后回调 onFinish
     */
    public ParticleRing(Location centerLoc, Particle type, long delay) {
        for (int i = 0; i < CIRCLE_ELEMENTS; ++i) {
            double alpha = 360.0 / (double) CIRCLE_ELEMENTS * (double) i;
            double x = RADIUS * Math.sin(Math.toRadians(alpha));
            double z = RADIUS * Math.cos(Math.toRadians(alpha));
            Location particle = new Location(centerLoc.getWorld(),
                    centerLoc.getX() + x, centerLoc.getY(), centerLoc.getZ() + z);
            centerLoc.getWorld().spawnParticle(type, particle, 1, 0.0, 0.0, 0.0, 0.0);
        }
        Bukkit.getScheduler().runTaskLater(
                (Plugin) BridgingAnalyzer.getInstance(), this::onFinish, delay);
    }

    public abstract void onFinish();
}
