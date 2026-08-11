/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  org.bukkit.Bukkit
 *  org.bukkit.event.EventHandler
 *  org.bukkit.event.Listener
 *  org.bukkit.event.player.PlayerJoinEvent
 *  org.bukkit.event.player.PlayerQuitEvent
 *  org.bukkit.plugin.Plugin
 */
package com.luanmenglei.lv.events;

import com.luanmenglei.lv.BlockLv;
import com.luanmenglei.lv.core.PointManger;
import org.bukkit.Bukkit;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.plugin.Plugin;

public class PlayerLogin
implements Listener {
    @EventHandler
    public void onJoin(PlayerJoinEvent e) {
        Bukkit.getScheduler().runTask((Plugin)BlockLv.getInstance(), () -> {
            PointManger pm = BlockLv.getInstance().getDatabase().get(e.getPlayer());
            PointManger.players.put(e.getPlayer().getUniqueId(), pm);
        });
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        Bukkit.getScheduler().runTask((Plugin)BlockLv.getInstance(), () -> BlockLv.getInstance().getDatabase().set(event.getPlayer(), PointManger.players.get(event.getPlayer().getUniqueId())));
    }
}

