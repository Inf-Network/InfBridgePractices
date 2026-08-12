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

import java.util.Objects;
import java.util.UUID;
import java.util.function.Predicate;
import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.Material;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockFromToEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.player.PlayerBucketEmptyEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.Plugin;
import sakura.kooi.BridgingAnalyzer.BridgingAnalyzer;
import sakura.kooi.BridgingAnalyzer.Counter;
import sakura.kooi.BridgingAnalyzer.utils.ActionBarUtils;
import sakura.kooi.BridgingAnalyzer.utils.TitleUtils;

public class CounterListener
implements Listener {
    private final Predicate<ItemStack> menuItemMatcher;

    /** Compatibility constructor for extensions that instantiate the original listener. */
    public CounterListener() {
        this(item -> false);
    }

    public CounterListener(Predicate<ItemStack> menuItemMatcher) {
        this.menuItemMatcher = Objects.requireNonNull(menuItemMatcher, "menuItemMatcher");
    }

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
        if (this.menuItemMatcher.test(e.getItem())) {
            return;
        }
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
    public void onLiqudFlow(BlockFromToEvent e) {
        e.setCancelled(true);
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onPlaceBlock(BlockPlaceEvent e) {
        if (e.getPlayer() != null) {
            if (e.getPlayer().getGameMode() == GameMode.CREATIVE) {
                return;
            }
            Counter c = BridgingAnalyzer.getCounter(e.getPlayer());
            c.countBridge(e.getBlock());
            if (c.isSpeedCountEnabled()) {
                TitleUtils.sendTitle(e.getPlayer(), "", "\u00a7b" + c.getBridgeSpeed() + " block/s", 1, 40, 1);
            }
            // Snapshot only the placed material. Copying a complete BlockStateMeta
            // item here would duplicate shulker/beehive contents; reading the hand one
            // tick later would instead mint whichever hotbar item was selected next.
            Material placedMaterial = e.getBlockPlaced().getType();
            UUID playerId = e.getPlayer().getUniqueId();
            Bukkit.getScheduler().runTaskLater((Plugin)BridgingAnalyzer.getInstance(), () -> {
                org.bukkit.entity.Player current = Bukkit.getPlayer(playerId);
                if (current != null && current.isOnline()) {
                    current.getInventory().addItem(new ItemStack(placedMaterial, 1));
                }
            }, 1L);
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
