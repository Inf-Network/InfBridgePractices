/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  org.bukkit.Bukkit
 *  org.bukkit.entity.Player
 *  org.bukkit.plugin.Plugin
 *  org.bukkit.scheduler.BukkitTask
 */
package sakura.kooi.CpsCounter;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;
import org.bukkit.scheduler.BukkitTask;
import sakura.kooi.CpsCounter.ActionBarUtils;
import sakura.kooi.CpsCounter.Counter;
import sakura.kooi.CpsCounter.CpsCounter;

public class MonitorThread
implements Runnable {
    private final Player player;
    private final Player target;
    private final Counter counter;
    private BukkitTask task;

    public MonitorThread(Player player, Player target) {
        this.player = player;
        this.target = target;
        this.counter = CpsCounter.getCounter(target);
    }

    @Override
    public void run() {
        if (!this.player.isOnline()) {
            CpsCounter.stopMoniting(this.player);
            return;
        }
        if (!this.target.isOnline()) {
            CpsCounter.stopMoniting(this.player);
            return;
        }
        ActionBarUtils.sendActionBar(this.player, "\u00a74\u00a7l\u73a9\u5bb6 \u00a7a" + this.target.getName() + " \u00a76\u00a7l | \u00a74\u00a7lCPS \u00a7c\u00a7l" + (this.counter.getLastClickMs() > 1000L ? 0 : this.counter.getCPS()) + "\u00a74 / \u00a71\u00a7l" + this.counter.getMaxCPS() + "\u00a76\u00a7l | \u00a74\u00a7lLCS \u00a7c\u00a7l" + (this.counter.getLeftLastClickMs() > 1000L ? 0 : this.counter.getLeftCPS()) + "\u00a74 / \u00a71\u00a7l" + this.counter.getLeftMaxCPS() + "\u00a76\u00a7l | \u00a74\u00a7lRCS \u00a7c\u00a7l" + (this.counter.getRightLastClickMs() > 1000L ? 0 : this.counter.getRightCPS()) + "\u00a74 / \u00a71\u00a7l" + this.counter.getRightMaxCPS());
    }

    public void stopMonitor() {
        this.task.cancel();
    }

    public void startMonitor() {
        this.task = Bukkit.getScheduler().runTaskTimer((Plugin)CpsCounter.getInstance(), (Runnable)this, 10L, 10L);
    }
}

