/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  org.bukkit.event.EventHandler
 *  org.bukkit.event.Listener
 *  org.bukkit.event.block.BlockPlaceEvent
 */
package com.luanmenglei.lv.events;

import com.luanmenglei.lv.core.PointManger;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockPlaceEvent;

public class BlockPlace
implements Listener {
    @EventHandler
    public void onPlace(BlockPlaceEvent e) {
        if (e.isCancelled()) {
            return;
        }
        PointManger.addPx(1L, e.getPlayer().getUniqueId());
    }
}

