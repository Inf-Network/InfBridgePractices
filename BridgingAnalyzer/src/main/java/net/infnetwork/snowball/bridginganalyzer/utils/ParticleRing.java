/*
 * 1.21.11 移植:原版第二个参数是自研的 ParticleEffects 枚举(基于 NMS 反射发
 * 粒子包)。该枚举在 1.21 上完全失效,已整体删除,改用 Bukkit 原生的
 * org.bukkit.Particle + World#spawnParticle。
 *
 * 原版 display 的最后一个参数是可见半径(64.0),原生 API 无对应项 ——
 * 服务端按玩家的实体追踪距离自行决定可见性,行为等价,故不再传递。
 */
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

    /** 圆环由多少个粒子构成,与原版一致。 */
    private static final int CIRCLE_ELEMENTS = 20;

    /** 圆环半径(格),与原版一致。 */
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
            // 原版:display(0,0,0,0, 1, particle, 64.0) —— 偏移全 0、速度 0、数量 1
            centerLoc.getWorld().spawnParticle(type, particle, 1, 0.0, 0.0, 0.0, 0.0);
        }
        Bukkit.getScheduler().runTaskLater(
                (Plugin) BridgingAnalyzer.getInstance(), this::onFinish, delay);
    }

    /** 光环播完后执行的动作。 */
    public abstract void onFinish();
}
