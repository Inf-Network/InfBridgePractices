package net.infnetwork.snowball.bridginganalyzer.utils;

import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.bukkit.entity.Player;

public class ActionBarUtils {

    private static final LegacyComponentSerializer LEGACY =
            LegacyComponentSerializer.legacySection();

    /**
     * 在玩家物品栏上方的 ActionBar 区域显示一行文本。
     *
     * @param message 文本内容,支持 § 颜色码
     */
    public static void sendActionBar(Player player, String message) {
        player.sendActionBar(LEGACY.deserialize(message == null ? "" : message));
    }
}
