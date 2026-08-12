package net.infnetwork.snowball.blocklv.holographicdisplay;

import java.util.ArrayList;
import java.util.List;

import net.infnetwork.snowball.blocklv.BlockLv;
import eu.decentsoftware.holograms.api.DHAPI;
import eu.decentsoftware.holograms.api.holograms.Hologram;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;

public class DisPlay {

    private static final String HOLOGRAM_NAME = "blocklv_rank";

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

    /** Rebuilds the leaderboard hologram and must run on the Bukkit main thread. */
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

    public static void remove() {
        Hologram holo = DHAPI.getHologram(HOLOGRAM_NAME);
        if (holo != null) {
            DHAPI.removeHologram(HOLOGRAM_NAME);
        }
    }
}
