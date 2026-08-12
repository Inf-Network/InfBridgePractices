/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  org.bukkit.Bukkit
 *  org.bukkit.ChatColor
 *  org.bukkit.Color
 *  org.bukkit.FireworkEffect
 *  org.bukkit.entity.EntityType
 *  org.bukkit.entity.Firework
 *  org.bukkit.entity.Player
 *  org.bukkit.inventory.meta.FireworkMeta
 */
package net.infnetwork.snowball.blocklv.core;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
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
        p.sendMessage(ChatColor.AQUA + ChatColor.translateAlternateColorCodes((char)'&', (String)("&l\u642d\u8def\u7b49\u7ea7 >> \u5347\u7ea7\u4e86\uff0c\u76ee\u524d\u60a8\u7684\u7b49\u7ea7\u4e3a" + ChatColor.GREEN + lv)));
        if (lv >= 5L && lv % 10L != 0L) {
            return;
        }
        Firework fw = (Firework)p.getLocation().getWorld().spawnEntity(p.getLocation(), EntityType.FIREWORK_ROCKET);
        FireworkMeta fwm = fw.getFireworkMeta();
        fwm.setPower(2);
        fwm.addEffect(FireworkEffect.builder().withColor(Color.LIME).flicker(true).build());
        // 原版漏了这一行,构造好的 meta 从未写回实体,烟花其实一直是默认样式
        fw.setFireworkMeta(fwm);
    }

    public static void addPx(long addpx, UUID name) {
        if (PointManger.players.get((Object)name).lv == 0L) {
            PointManger.players.get((Object)name).lv = 1L;
        }
        long px = PointManger.players.get((Object)name).px;
        PointManger.players.get((Object)name).px = px += addpx;
        boolean flag = false;
        long lv = PointManger.players.get((Object)name).lv;
        while (px >= PointManger.getNextLvPx(lv)) {
            // 扣经验必须放在 ++lv 之前。原版是先自增再按「新等级」的门槛扣,
            // 而循环判定用的是「当前等级」的门槛 —— 门槛随等级递增,于是每次升级都多扣一截:
            // 1 级攒满 21 点该升 2 级,却按 2 级的 41 点去扣,px 直接成了 -20。
            // 1.8 的 setExp 不校验取值范围,这个错一直是静默的;1.21 会抛
            // IllegalArgumentException,连带 refreshExp 里的 setLevel 一起崩掉。
            PointManger.players.get((Object)name).px = px -= PointManger.getNextLvPx(lv);
            PointManger.upLevel(lv + 1L, Bukkit.getServer().getPlayer(name));
            flag = true;
            PointManger.players.get((Object)name).lv = ++lv;
        }
        PointManger.refreshExp(name);
    }

    public static void refreshExp(UUID name) {
        if (Bukkit.getPlayer((UUID)name) != null) {
            Bukkit.getPlayer((UUID)name).setExp((float)(PointManger.getPx(name) + 1L) * 1.0f / (float)PointManger.getNextLvPx(PointManger.getLv(name)));
            Bukkit.getPlayer((UUID)name).setLevel((int)PointManger.getLv(name));
        }
    }

    public static long getPx(UUID name) {
        return PointManger.players.get((Object)name).px;
    }

    public static long getLv(UUID name) {
        return PointManger.players.get((Object)name).lv;
    }
}

