package net.infnetwork.snowball.blocklv.core;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import net.infnetwork.snowball.blocklv.api.LevelComponents;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.Bukkit;
import org.bukkit.Color;
import org.bukkit.FireworkEffect;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Firework;
import org.bukkit.entity.Player;
import org.bukkit.inventory.meta.FireworkMeta;

public class PointManger {
    public static Map<UUID, PointManger> players = new HashMap<UUID, PointManger>();
    public long lv;
    public long px;

    public static long getNextLvPx(long lv) {
        return LevelThresholds.threshold(lv);
    }

    public static void upLevel(long lv, Player p) {
        p.sendMessage(Component.text("\u642d\u8def\u7b49\u7ea7 >> \u5347\u7ea7\u4e86\uff0c\u76ee\u524d\u60a8\u7684\u7b49\u7ea7\u4e3a",
                        NamedTextColor.AQUA, TextDecoration.BOLD)
                .append(LevelComponents.badge(lv)));
        if (lv >= 5L && lv % 10L != 0L) {
            return;
        }
        Firework fw = (Firework)p.getLocation().getWorld().spawnEntity(p.getLocation(), EntityType.FIREWORK_ROCKET);
        FireworkMeta fwm = fw.getFireworkMeta();
        fwm.setPower(2);
        fwm.addEffect(FireworkEffect.builder().withColor(Color.LIME).flicker(true).build());
        fw.setFireworkMeta(fwm);
    }

    public static void addPx(long addpx, UUID name) {
        PointManger points = PointManger.players.get(name);
        if (points == null || addpx <= 0L || points.lv < 0L || points.px < 0L) {
            return;
        }
        if (points.lv == 0L) {
            points.lv = 1L;
        }
        long maximumExperience = getNextLvPx(points.lv) - 1L;
        if (points.px > maximumExperience) {
            return;
        }

        long remaining = addpx;
        while (remaining > 0L) {
            long threshold = getNextLvPx(points.lv);
            long needed = threshold - points.px;
            if (remaining < needed) {
                points.px += remaining;
                remaining = 0L;
                continue;
            }

            remaining -= needed;
            points.px = 0L;
            if (points.lv == Long.MAX_VALUE) {
                points.px = threshold - 1L;
                break;
            }
            points.lv++;
            Player player = Bukkit.getServer().getPlayer(name);
            if (player != null) {
                PointManger.upLevel(points.lv, player);
            }
        }
        PointManger.refreshExp(name);
    }

    public static void refreshExp(UUID name) {
        Player player = Bukkit.getPlayer(name);
        if (player != null) {
            long level = Math.max(0L, PointManger.getLv(name));
            long threshold = Math.max(1L, PointManger.getNextLvPx(level));
            long experience = Math.max(0L, PointManger.getPx(name));
            float progress = (float) Math.min(
                    1.0D, ((double) experience + 1.0D) / (double) threshold);
            int visibleLevel = level > Integer.MAX_VALUE
                    ? Integer.MAX_VALUE
                    : (int) level;
            player.setExp(progress);
            player.setLevel(visibleLevel);
        }
    }

    public static long getPx(UUID name) {
        return PointManger.players.get(name).px;
    }

    public static long getLv(UUID name) {
        return PointManger.players.get(name).lv;
    }
}
