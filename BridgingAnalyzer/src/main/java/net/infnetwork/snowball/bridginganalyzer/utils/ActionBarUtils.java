/*
 * 1.21.11 移植:原版用 NMS 反射拼 PacketPlayOutChat(type=2 即 ActionBar)发包。
 * Paper 自 1.20.5 起去掉了包名里的版本号,反射初始化必然失败。
 *
 * Bukkit 从 1.11 起就有 Player#sendActionBar,Paper 更提供了接受 Component 的
 * 重载。这里用 Adventure 版本,并用 LegacyComponentSerializer 解析 § 颜色码 ——
 * 插件的提示文本都是 § 格式。
 */
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
