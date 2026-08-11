/*
 * 1.21.11 移植:原版依赖 HolographicDisplays 的 HologramsAPI。
 * 该插件已停更,本服换成了 DecentHolograms,两者 API 完全不同
 * (DecentHolograms 没有提供 HD 的兼容层,只有一次性的数据转换工具)。
 *
 * 改用 DHAPI:
 *   - 全息以名字为标识注册,重启后仍可按名字取回,不需要自己持有引用
 *   - 整行列表一次性 setHologramLines 替换,不再逐行 insert
 */
package com.luanmenglei.lv.HolographicDisplay;

import java.util.ArrayList;
import java.util.List;

import com.luanmenglei.lv.BlockLv;
import eu.decentsoftware.holograms.api.DHAPI;
import eu.decentsoftware.holograms.api.holograms.Hologram;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;

public class DisPlay {

    /** DecentHolograms 里这个全息的唯一名字。 */
    private static final String HOLOGRAM_NAME = "blocklv_rank";

    /** 排行榜数据,由 Database#refreshTop 填充。 */
    public static List<Lv> tops = new ArrayList<>();

    /**
     * 从配置读取一个坐标。
     *
     * @param path 配置节点前缀,当前只用到 "rank"
     * @return 世界不存在或未配置时返回 null
     */
    public static Location loadLocation(String path) {
        int x = BlockLv.getInstance().getConfig().getInt(path + ".x");
        int y = BlockLv.getInstance().getConfig().getInt(path + ".y");
        int z = BlockLv.getInstance().getConfig().getInt(path + ".z");
        String worldname = BlockLv.getInstance().getConfig().getString(path + ".world");
        if (worldname == null) {
            return null;
        }
        World w = Bukkit.getWorld(worldname);
        if (w == null) {
            return null;
        }
        return new Location(w, x, y, z);
    }

    /**
     * 重建排行榜全息的内容。必须在主线程调用。
     *
     * 文本格式与原版逐字一致:标题 + 空行 + 前十名,前三名分别是金/蓝/绿。
     */
    public static void refreshHologrphic() {
        Location loc = loadLocation("rank");
        if (loc == null) {
            return;
        }

        List<String> lines = new ArrayList<>();
        lines.add("&bI&en&cf &eBridge &aTop 10".replace('&', '§'));
        lines.add("§r§r");

        for (int i = 1; i <= 10 && i <= tops.size(); ++i) {
            Lv entry = tops.get(i - 1);
            if (entry == null) {
                break;
            }
            String color = switch (i) {
                case 1 -> "§6";
                case 2 -> "§b";
                case 3 -> "§a";
                default -> "§7";
            };
            lines.add(color + i + ". " + entry.name + " " + entry.lv + "✫");
        }

        Hologram holo = DHAPI.getHologram(HOLOGRAM_NAME);
        if (holo == null) {
            DHAPI.createHologram(HOLOGRAM_NAME, loc, lines);
        } else {
            DHAPI.setHologramLines(holo, lines);
        }
    }

    /** 关服时移除全息,避免下次启动重复创建。 */
    public static void remove() {
        Hologram holo = DHAPI.getHologram(HOLOGRAM_NAME);
        if (holo != null) {
            DHAPI.removeHologram(HOLOGRAM_NAME);
        }
    }
}
