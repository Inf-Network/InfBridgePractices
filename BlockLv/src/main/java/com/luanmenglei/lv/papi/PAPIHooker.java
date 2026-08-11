/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  me.clip.placeholderapi.expansion.PlaceholderExpansion
 *  org.bukkit.entity.Player
 */
package com.luanmenglei.lv.papi;

import com.luanmenglei.lv.BlockLv;
import com.luanmenglei.lv.core.PointManger;
import me.clip.placeholderapi.expansion.PlaceholderExpansion;
import org.bukkit.entity.Player;

public class PAPIHooker
extends PlaceholderExpansion {
    public String onPlaceholderRequest(Player player, String name) {
        if (player == null) {
            return "";
        }
        if (PointManger.players.get(player.getUniqueId()) == null) {
            return "Loading...";
        }
        if (name.equals("lv")) {
            return String.valueOf(PointManger.getLv(player.getUniqueId()));
        }
        if (name.equals("px")) {
            return String.valueOf(PointManger.getPx(player.getUniqueId()));
        }
        if (name.equals("uppx")) {
            return String.valueOf(PointManger.getNextLvPx(PointManger.getLv(player.getUniqueId())) - PointManger.getPx(player.getUniqueId()));
        }
        if (name.equals("prefix")) {
            long lv = PointManger.getLv(player.getUniqueId());
            String str = String.valueOf(lv);
            if (lv < 10L) {
                return "\u00a78[\u00a78" + str + "\u00a78\u272b]";
            }
            if (lv < 100L) {
                return "\u00a77[" + str + "\u272b]";
            }
            if (lv < 200L) {
                return "\u00a7r[" + str + "\u272b]";
            }
            if (lv < 300L) {
                return "\u00a76[" + str + "\u272b]";
            }
            if (lv < 400L) {
                return "\u00a72[" + str + "\u272b]";
            }
            if (lv < 500L) {
                return "\u00a7e[" + str + "\u272b]";
            }
            if (lv < 600L) {
                return "\u00a73[" + str + "\u272b]";
            }
            if (lv < 700L) {
                return "\u00a74[" + str + "\u272b]";
            }
            if (lv < 800L) {
                return "\u00a78[" + str + "\u272b]";
            }
            if (lv < 900L) {
                return "\u00a7a[" + str + "\u272b]";
            }
            return "\u00a7c[" + str + "\u272b]";
        }
        if (name.equals("kill")) {
            return String.valueOf(BlockLv.getInstance().getConfig().getInt("data." + player.getName() + ".kill"));
        }
        return null;
    }

    public String getIdentifier() {
        return "blocklv";
    }

    public String getAuthor() {
        return "luanmenglei";
    }

    public String getVersion() {
        return "1.0";
    }
}

