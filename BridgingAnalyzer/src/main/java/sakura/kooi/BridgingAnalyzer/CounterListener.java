/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  org.bukkit.Bukkit
 *  org.bukkit.GameMode
 *  org.bukkit.Material
 *  org.bukkit.event.EventHandler
 *  org.bukkit.event.Listener
 *  org.bukkit.event.block.Action
 *  org.bukkit.event.block.BlockBreakEvent
 *  org.bukkit.event.block.BlockFromToEvent
 *  org.bukkit.event.block.BlockPlaceEvent
 *  org.bukkit.event.player.PlayerBucketEmptyEvent
 *  org.bukkit.event.player.PlayerInteractEvent
 *  org.bukkit.event.player.PlayerMoveEvent
 *  org.bukkit.inventory.ItemStack
 *  org.bukkit.plugin.Plugin
 */
package sakura.kooi.BridgingAnalyzer;

import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.Material;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockFromToEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.player.PlayerBucketEmptyEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.event.player.PlayerRespawnEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.Plugin;
import sakura.kooi.BridgingAnalyzer.BridgingAnalyzer;
import sakura.kooi.BridgingAnalyzer.Counter;
import sakura.kooi.BridgingAnalyzer.utils.ActionBarUtils;
import sakura.kooi.BridgingAnalyzer.utils.TitleUtils;

public class CounterListener
implements Listener {
    @EventHandler
    public void onBreakBlock(BlockBreakEvent e) {
        if (e.getPlayer() != null && !BridgingAnalyzer.isPlacedByPlayer(e.getBlock())) {
            if (e.getPlayer().getGameMode() == GameMode.CREATIVE) {
                return;
            }
            e.setCancelled(true);
        }
    }

    @EventHandler
    public void onClick(PlayerInteractEvent e) {
        if (e.getAction().toString().contains("CLICK")) {
            if (e.getAction() == Action.LEFT_CLICK_BLOCK && e.isCancelled()) {
                return;
            }
            Counter c = BridgingAnalyzer.getCounter(e.getPlayer());
            c.countCPS();
            if (!c.isSpeedCountEnabled()) {
                return;
            }
            ActionBarUtils.sendActionBar(e.getPlayer(), "\u00a7c\u00a7l\u6700\u5927CPS - " + c.getMaxCPS() + " \u00a7d\u00a7l\u5f53\u524dCPS - " + c.getCPS() + " \u00a7a\u00a7l| \u00a7c\u00a7l\u6700\u8fdc\u8ddd\u79bb - " + c.getMaxBridgeLength() + " \u00a7d\u00a7l\u5f53\u524d\u8ddd\u79bb - " + c.getBridgeLength());
        }
    }

    @EventHandler
    public void onFallDown(PlayerMoveEvent e) {
        if (e.getTo().getY() < 0.0) {
            Counter c = BridgingAnalyzer.getCounter(e.getPlayer());
            if (c.isSpeedCountEnabled()) {
                TitleUtils.sendTitle(e.getPlayer(), "", "\u00a7cMax - " + c.getMaxBridgeSpeed() + " block/s", 1, 40, 1);
            }
            c.reset();
            BridgingAnalyzer.teleportCheckPoint(e.getPlayer());
        }
    }

    /*
     * \u4e0a\u6e38\u6ca1\u6709\u5b9e\u73b0\u8fd9\u4e2a \u2014\u2014 \u6b7b\u4ea1\u540e\u8d70\u539f\u7248\u91cd\u751f\u903b\u8f91,\u56de\u4e16\u754c\u51fa\u751f\u70b9\u800c\u4e0d\u662f\u68c0\u67e5\u70b9\u3002
     * \u7ec3\u4e60\u670d\u91cc\u6b7b\u4ea1\u672c\u5c31\u5c11\u89c1(\u6240\u6709\u4f24\u5bb3\u90fd\u88ab onDamage \u6e05\u96f6\u4e86),\u4f46\u4e00\u65e6\u53d1\u751f
     * \u5e94\u8be5\u56de\u5230\u6700\u540e\u8e29\u8fc7\u7684\u7eff\u5b9d\u77f3\u5757,\u4e0e\u6389\u865a\u7a7a\u7684\u884c\u4e3a\u4fdd\u6301\u4e00\u81f4\u3002
     */
    @EventHandler
    public void onRespawn(PlayerRespawnEvent e) {
        e.setRespawnLocation(BridgingAnalyzer.getCounter(e.getPlayer()).getCheckPoint());
    }

    @EventHandler
    public void onLiqudFlow(BlockFromToEvent e) {
        e.setCancelled(true);
    }

    @EventHandler
    public void onPlaceBlock(BlockPlaceEvent e) {
        if (e.isCancelled()) {
            return;
        }
        if (e.getPlayer() != null) {
            if (e.getPlayer().getGameMode() == GameMode.CREATIVE) {
                return;
            }
            Counter c = BridgingAnalyzer.getCounter(e.getPlayer());
            c.countBridge(e.getBlock());
            if (c.isSpeedCountEnabled()) {
                TitleUtils.sendTitle(e.getPlayer(), "", "\u00a7b" + c.getBridgeSpeed() + " block/s", 1, 40, 1);
            }
            Bukkit.getScheduler().runTaskLater((Plugin)BridgingAnalyzer.getInstance(), () -> e.getPlayer().getInventory().addItem(new ItemStack[]{new ItemStack(e.getPlayer().getInventory().getItemInMainHand().getType(), 1)}), 1L);
        }
    }

    @EventHandler
    public void onPlaceLiqud(PlayerBucketEmptyEvent e) {
        if (e.isCancelled()) {
            return;
        }
        if (e.getPlayer() != null) {
            if (e.getPlayer().getGameMode() == GameMode.CREATIVE) {
                return;
            }
            Counter c = BridgingAnalyzer.getCounter(e.getPlayer());
            c.addLogBlock(e.getBlockClicked().getRelative(e.getBlockFace()));
            Bukkit.getScheduler().runTaskLater((Plugin)BridgingAnalyzer.getInstance(), () -> e.getPlayer().getInventory().remove(Material.BUCKET), 1L);
        }
    }
}

