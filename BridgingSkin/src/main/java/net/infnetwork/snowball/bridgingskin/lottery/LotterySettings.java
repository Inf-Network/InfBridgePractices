package net.infnetwork.snowball.bridgingskin.lottery;

import java.util.List;
import org.bukkit.configuration.file.FileConfiguration;

public record LotterySettings(
        double singleCost,
        double tenCost,
        int animationDurationTicks,
        int animationIntervalTicks,
        List<String> configuredMaterials) {

    public LotterySettings {
        if (!Double.isFinite(singleCost) || singleCost < 0.0D) {
            throw new IllegalArgumentException("lottery.single-cost 必须是非负有限数字");
        }
        if (!Double.isFinite(tenCost) || tenCost < 0.0D) {
            throw new IllegalArgumentException("lottery.ten-cost 必须是非负有限数字");
        }
        if (animationDurationTicks < 1 || animationIntervalTicks < 1) {
            throw new IllegalArgumentException("抽奖动画 tick 配置必须大于 0");
        }
        configuredMaterials = List.copyOf(configuredMaterials);
    }

    public static LotterySettings load(FileConfiguration config) {
        return new LotterySettings(
                config.getDouble("lottery.single-cost", 100.0D),
                config.getDouble("lottery.ten-cost", 900.0D),
                config.getInt("lottery.animation.duration-ticks", 50),
                config.getInt("lottery.animation.interval-ticks", 2),
                config.getStringList("lottery.prize-pool"));
    }
}
