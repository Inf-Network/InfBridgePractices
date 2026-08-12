/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  org.bukkit.Color
 *  org.bukkit.FireworkEffect
 *  org.bukkit.FireworkEffect$Type
 *  org.bukkit.entity.EntityType
 *  org.bukkit.entity.Firework
 *  org.bukkit.entity.Player
 *  org.bukkit.inventory.meta.FireworkMeta
 */
package net.infnetwork.snowball.bridginganalyzer.utils;

import java.util.Random;
import org.bukkit.Bukkit;
import org.bukkit.Color;
import org.bukkit.FireworkEffect;
import org.bukkit.GameMode;
import org.bukkit.damage.DamageSource;
import org.bukkit.damage.DamageType;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Firework;
import org.bukkit.entity.Player;
import org.bukkit.inventory.meta.FireworkMeta;
import org.bukkit.plugin.Plugin;
import net.infnetwork.snowball.bridginganalyzer.BridgingAnalyzer;

public class FireworkUtils {
    /** 整蛊触发概率的分母:1/1000 = 0.1%。 */
    private static final int RIGGED_ONE_IN = 1000;
    /** 500 点伤害,满血满甲也一击必杀(玩家上限 20 点)。 */
    private static final double RIGGED_DAMAGE = 500.0;
    /** 等烟花实体飞出去一点再引爆结算,视觉上才像是被烟花炸死的。 */
    private static final long RIGGED_DELAY_TICKS = 2L;

    public static Color getColor(int c) {
        switch (c) {
            default: {
                return Color.AQUA;
            }
            case 2: {
                return Color.BLACK;
            }
            case 3: {
                return Color.BLUE;
            }
            case 4: {
                return Color.FUCHSIA;
            }
            case 5: {
                return Color.GRAY;
            }
            case 6: {
                return Color.GREEN;
            }
            case 7: {
                return Color.LIME;
            }
            case 8: {
                return Color.MAROON;
            }
            case 9: {
                return Color.NAVY;
            }
            case 10: {
                return Color.OLIVE;
            }
            case 11: {
                return Color.ORANGE;
            }
            case 12: {
                return Color.PURPLE;
            }
            case 13: {
                return Color.RED;
            }
            case 14: {
                return Color.SILVER;
            }
            case 15: {
                return Color.TEAL;
            }
            case 16: 
        }
        return Color.WHITE;
    }

    public static void shootFirework(Player player) {
        Firework firework = (Firework)player.getWorld().spawnEntity(player.getLocation(), EntityType.FIREWORK_ROCKET);
        FireworkMeta fm = firework.getFireworkMeta();
        Random r = new Random();
        FireworkEffect.Type type = FireworkEffect.Type.STAR;
        int c1i = r.nextInt(16) + 1;
        int c2i = r.nextInt(16) + 1;
        Color c1 = FireworkUtils.getColor(c1i);
        Color c2 = FireworkUtils.getColor(c2i);
        FireworkEffect effect = FireworkEffect.builder().flicker(true).withColor(c1).withFade(c2).with(type).trail(true).build();
        fm.addEffect(effect);
        fm.setPower(0);
        firework.setFireworkMeta(fm);

        if (r.nextInt(RIGGED_ONE_IN) == 0) {
            FireworkUtils.rig(player, firework);
        }
    }

    /**
     * 整蛊:千分之一概率,这发终点烟花是实弹,当场把人炸死。
     *
     * 时间线(踩红石块那一刻为 tick 0):
     *   tick 0   TriggerBlockListener 设 40 tick 无敌,随后 teleportCheckPoint()
     *            把它覆盖成 5 并把血量拉满、传回传送点
     *   tick 20  ParticleRing 播完,回调发射烟花(即本方法的调用点)
     *   tick 22  实弹结算
     *
     * 三个要点:
     *
     * 1. 打之前显式清无敌帧。到 tick 22 那 5 tick 其实早过期了,但玩家可能在这
     *    22 tick 里因别的原因吃到无敌帧,清一下才稳。
     *
     * 2. 伤害必须归因成 DamageType.FIREWORKS,死亡信息才是原版的
     *    「被烟花火箭炸死了」。用 damage(double, Entity) 那个重载不行 ——
     *    它会被映射成通用的实体攻击,一看就不对劲。
     *
     * 3. 先结算伤害再引爆。detonate() 会移除烟花实体,而 DamageSource 里存着
     *    它的引用,顺序反了就是拿一个已消失的实体当伤害来源。
     */
    private static void rig(Player player, Firework firework) {
        Bukkit.getScheduler().runTaskLater((Plugin)BridgingAnalyzer.getInstance(), () -> {
            if (!player.isOnline() || player.isDead()) {
                return;
            }
            // 创造/旁观打不动,白费力气
            if (player.getGameMode() == GameMode.CREATIVE || player.getGameMode() == GameMode.SPECTATOR) {
                return;
            }
            DamageSource source = DamageSource.builder(DamageType.FIREWORKS)
                    .withDirectEntity((Entity)firework)
                    .withCausingEntity((Entity)firework)
                    .withDamageLocation(player.getLocation())
                    .build();
            player.setNoDamageTicks(0);
            player.damage(RIGGED_DAMAGE, source);
            if (firework.isValid()) {
                firework.detonate();
            }
        }, RIGGED_DELAY_TICKS);
    }
}

