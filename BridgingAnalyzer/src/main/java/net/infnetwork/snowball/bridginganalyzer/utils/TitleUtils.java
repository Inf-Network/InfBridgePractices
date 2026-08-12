package net.infnetwork.snowball.bridginganalyzer.utils;

import java.time.Duration;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import net.kyori.adventure.title.Title;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

public class TitleUtils {

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

    public static void boardcastTitle(String title, String subtitle,
                                      int fadeIn, int stay, int fadeOut) {
        for (Player p : Bukkit.getOnlinePlayers()) {
            sendTitle(p, title, subtitle, fadeIn, stay, fadeOut);
        }
    }
}
