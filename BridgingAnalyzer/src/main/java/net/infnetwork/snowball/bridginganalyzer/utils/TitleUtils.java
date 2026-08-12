/*
 * 1.21.11 移植:原版用 NMS 反射拼 PacketPlayOutTitle 发包。Paper 自 1.20.5 起
 * 去掉了 org.bukkit.craftbukkit / net.minecraft.server 的版本号包名,
 * getNMSClass 全部返回 null,每次调用都抛异常。
 *
 * 改用 Paper 自带的 Adventure API。原版把 title 直接拼进 JSON 字符串
 * ("{\"text\":\"" + title + "\"}"),既无法处理 § 颜色码也存在注入问题;
 * 这里用 LegacyComponentSerializer 正确解析 § 前缀的颜色码 —— 插件全部提示
 * 文本都是 § 格式,必须这样才能显示出颜色。
 */
package net.infnetwork.snowball.bridginganalyzer.utils;

import java.time.Duration;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import net.kyori.adventure.title.Title;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

public class TitleUtils {

    /** 一 tick 是 50 毫秒,原版的 fadeIn/stay/fadeOut 单位都是 tick。 */
    private static final long MILLIS_PER_TICK = 50L;

    private static final LegacyComponentSerializer LEGACY =
            LegacyComponentSerializer.legacySection();

    /**
     * 向单个玩家发送标题。
     *
     * @param title    主标题,支持 § 颜色码
     * @param subtitle 副标题,支持 § 颜色码
     * @param fadeIn   淡入时长(tick)
     * @param stay     停留时长(tick)
     * @param fadeOut  淡出时长(tick)
     */
    public static void sendTitle(Player player, String title, String subtitle,
                                 int fadeIn, int stay, int fadeOut) {
        Component titleComponent = LEGACY.deserialize(title == null ? "" : title);
        Component subtitleComponent = LEGACY.deserialize(subtitle == null ? "" : subtitle);

        Title.Times times = Title.Times.times(
                Duration.ofMillis(fadeIn * MILLIS_PER_TICK),
                Duration.ofMillis(stay * MILLIS_PER_TICK),
                Duration.ofMillis(fadeOut * MILLIS_PER_TICK));

        player.showTitle(Title.title(titleComponent, subtitleComponent, times));
    }

    /** 向全服在线玩家广播标题。 */
    public static void boardcastTitle(String title, String subtitle,
                                      int fadeIn, int stay, int fadeOut) {
        for (Player p : Bukkit.getOnlinePlayers()) {
            sendTitle(p, title, subtitle, fadeIn, stay, fadeOut);
        }
    }
}
