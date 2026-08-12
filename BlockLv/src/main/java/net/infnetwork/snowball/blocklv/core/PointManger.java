package net.infnetwork.snowball.blocklv.core;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
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
        return (long)((double)(lv * 20L) + Math.pow(2.0, lv / 1000L) + (double)(lv / 3L * 2L));
    }

    public static void upLevel(long lv, Player p) {
        p.sendMessage(Component.text("\u642d\u8def\u7b49\u7ea7 >> \u5347\u7ea7\u4e86\uff0c\u76ee\u524d\u60a8\u7684\u7b49\u7ea7\u4e3a",
                        NamedTextColor.AQUA, TextDecoration.BOLD)
                .append(Component.text(lv, NamedTextColor.GREEN)
                        .decoration(TextDecoration.BOLD, false)));
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
        if (PointManger.players.get(name).lv == 0L) {
            PointManger.players.get(name).lv = 1L;
        }
        long px = PointManger.players.get(name).px;
        PointManger.players.get(name).px = px += addpx;
        long lv = PointManger.players.get(name).lv;
        while (px >= PointManger.getNextLvPx(lv)) {
            // Deduct the current level's threshold before incrementing the level.
            PointManger.players.get(name).px = px -= PointManger.getNextLvPx(lv);
            PointManger.upLevel(lv + 1L, Bukkit.getServer().getPlayer(name));
            PointManger.players.get(name).lv = ++lv;
        }
        PointManger.refreshExp(name);
    }

    public static void refreshExp(UUID name) {
        Player player = Bukkit.getPlayer(name);
        if (player != null) {
            player.setExp((float)(PointManger.getPx(name) + 1L) * 1.0f / (float)PointManger.getNextLvPx(PointManger.getLv(name)));
            player.setLevel((int)PointManger.getLv(name));
        }
    }

    public static long getPx(UUID name) {
        return PointManger.players.get(name).px;
    }

    public static long getLv(UUID name) {
        return PointManger.players.get(name).lv;
    }
}
