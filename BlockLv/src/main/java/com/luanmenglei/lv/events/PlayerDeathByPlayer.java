/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  org.bukkit.entity.Player
 *  org.bukkit.event.EventHandler
 *  org.bukkit.event.Listener
 *  org.bukkit.event.entity.EntityDamageByEntityEvent
 *  org.bukkit.plugin.Plugin
 *  org.bukkit.scheduler.BukkitRunnable
 */
package com.luanmenglei.lv.events;

import com.luanmenglei.lv.BlockLv;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.plugin.Plugin;
import org.bukkit.scheduler.BukkitRunnable;

public class PlayerDeathByPlayer
implements Listener {
    @EventHandler
    public void onDeath(EntityDamageByEntityEvent e) {
        if (e.isCancelled()) {
            return;
        }
        if (e.getDamager() instanceof Player && e.getEntity() instanceof Player) {
            Player damager = (Player)e.getDamager();
            final Player p = (Player)e.getEntity();
            BlockLv.getInstance().killPlayer.put(p, damager);
            new BukkitRunnable(){

                public void run() {
                    BlockLv.getInstance().killPlayer.remove(p);
                }
            }.runTaskLater((Plugin)BlockLv.getInstance(), 120L);
        }
    }
}

