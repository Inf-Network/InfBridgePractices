package net.infnetwork.snowball.bridginganalyzer;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import java.util.function.Predicate;
import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.block.BlockState;
import org.bukkit.block.data.BlockData;
import org.bukkit.block.data.Waterlogged;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockFromToEvent;
import org.bukkit.event.block.BlockMultiPlaceEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.player.PlayerBucketEmptyEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.server.PluginDisableEvent;
import org.bukkit.event.world.WorldUnloadEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.Plugin;
import net.infnetwork.snowball.bridginganalyzer.BridgingAnalyzer;
import net.infnetwork.snowball.bridginganalyzer.Counter;
import net.infnetwork.snowball.bridginganalyzer.block.PracticeBlockRegistry;
import net.infnetwork.snowball.bridginganalyzer.utils.ActionBarUtils;
import net.infnetwork.snowball.bridginganalyzer.utils.TitleUtils;

public class CounterListener
implements Listener {
    private final Predicate<ItemStack> menuItemMatcher;
    /** Bucket contents are installed after the Bukkit event returns. Keep at most one pending write per location. */
    private final Map<Block, PendingBucketPlacement> pendingBucketPlacements = new HashMap<>();

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

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onSuccessfulBreak(BlockBreakEvent event) {
        BridgingAnalyzer.forgetPracticeBlock(event.getBlock());
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
        if (!e.canBuild()) {
            return;
        }
        if (e.getPlayer() != null) {
            List<Block> placedBlocks = placedBlocks(e);
            placedBlocks.forEach(this::supersedePendingBucketPlacement);
            if (!PracticeBlockRegistry.shouldTrack(e.getPlayer().getGameMode())) {
                // A creative/admin replacement is permanent and must invalidate an
                // older survival generation at the same coordinates.
                placedBlocks.forEach(BridgingAnalyzer::forgetPracticeBlock);
                return;
            }
            Counter c = BridgingAnalyzer.getCounter(e.getPlayer());
            Block primary = e.getBlockPlaced();
            c.countBridge(primary);
            for (Block placedBlock : placedBlocks) {
                if (!placedBlock.equals(primary)) {
                    // A bed, door, or other multi-place item produces one action but
                    // several world blocks. Count the action once and own every block.
                    c.addLogBlock(placedBlock);
                }
            }
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

    /**
     * Waterlogging changes an existing block's data rather than placing a disposable
     * liquid block. The registry intentionally owns placed blocks, not mutations of
     * permanent map blocks, so survival waterlogging is rejected before it can occur.
     */
    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void guardUnsafeBucketPlacement(PlayerBucketEmptyEvent e) {
        if (!PracticeBlockRegistry.shouldTrack(e.getPlayer().getGameMode())) {
            return;
        }
        Block affected = affectedBucketBlock(e);
        Material expected = bucketContents(e.getBucket());
        if (expected == null
                || isUnsafeBucketTarget(affected)
                || hasReactiveFluidNeighbor(affected, expected)) {
            e.setCancelled(true);
        }
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onPlaceLiqud(PlayerBucketEmptyEvent e) {
        Block affected = affectedBucketBlock(e);
        supersedePendingBucketPlacement(affected);
        if (!PracticeBlockRegistry.shouldTrack(e.getPlayer().getGameMode())) {
            // Creative bucket changes are permanent, including waterlogging. They
            // invalidate an older temporary generation at the actual affected block.
            BridgingAnalyzer.forgetPracticeBlock(affected);
            return;
        }

        Material expectedMaterial = bucketContents(e.getBucket());
        if (expectedMaterial == null) {
            // The HIGHEST guard rejects unknown buckets. This is defensive against a
            // non-conforming listener uncancelling the event at MONITOR priority.
            return;
        }

        Counter counter = BridgingAnalyzer.getCounter(e.getPlayer());
        UUID playerId = e.getPlayer().getUniqueId();
        PendingBucketPlacement pending = new PendingBucketPlacement(
                affected, affected.getBlockData().clone(), playerId, counter);
        counter.getAllBlocks().add(affected);
        pendingBucketPlacements.put(affected, pending);
        try {
            Bukkit.getScheduler().runTaskLater(
                    (Plugin)BridgingAnalyzer.getInstance(), () -> settleBucketPlacement(pending), 1L);
        } catch (RuntimeException schedulingFailure) {
            pendingBucketPlacements.remove(affected, pending);
            counter.getAllBlocks().remove(affected);
            // A placement that cannot be registered on the following tick must not
            // be allowed to become an unowned permanent liquid source.
            e.setCancelled(true);
            BridgingAnalyzer.getInstance().getLogger().warning(
                    "无法登记玩家桶放置，已取消本次操作: " + schedulingFailure.getMessage());
        }
    }

    /** PluginDisableEvent is fired before onDisable and scheduler cancellation on Paper. */
    @EventHandler(priority = EventPriority.MONITOR)
    public void onPluginDisable(PluginDisableEvent event) {
        if (event.getPlugin() != BridgingAnalyzer.getInstance()) {
            return;
        }
        for (PendingBucketPlacement pending : new ArrayList<>(pendingBucketPlacements.values())) {
            try {
                removeExpectedBucketContents(pending);
                pending.counter().getAllBlocks().remove(pending.block());
                pendingBucketPlacements.remove(pending.block(), pending);
            } catch (RuntimeException exception) {
                BridgingAnalyzer.getInstance().getLogger().warning(
                        "停服前无法回滚桶放置: " + exception.getMessage());
            }
        }
    }

    /** Roll back not-yet-registered liquids before an instance world is unloaded. */
    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onWorldUnload(WorldUnloadEvent event) {
        boolean failed = false;
        for (PendingBucketPlacement pending : new ArrayList<>(pendingBucketPlacements.values())) {
            if (!pending.block().getWorld().equals(event.getWorld())) {
                continue;
            }
            try {
                removeExpectedBucketContents(pending);
                pending.counter().getAllBlocks().remove(pending.block());
                pendingBucketPlacements.remove(pending.block(), pending);
            } catch (RuntimeException exception) {
                failed = true;
                BridgingAnalyzer.getInstance().getLogger().warning(
                        "世界卸载前无法回滚桶放置: " + exception.getMessage());
            }
        }
        if (failed) {
            event.setCancelled(true);
        }
    }

    private void settleBucketPlacement(PendingBucketPlacement pending) {
        if (!pendingBucketPlacements.remove(pending.block(), pending)) {
            return;
        }

        // reset(), resetImmediately(), quit cleanup, and victory cleanup all clear
        // Counter#allBlock. The temporary marker makes that lifecycle visible without
        // allowing a delayed bucket task to resurrect ownership after cleanup.
        if (!pending.counter().getAllBlocks().remove(pending.block())) {
            removeExpectedBucketContents(pending);
            return;
        }
        // WATER/LAVA may synchronously form stone, cobblestone or obsidian before
        // this task runs. The resulting block is still part of this survival
        // placement and must remain owned. A no-op placement is not adopted.
        if (pending.block().getBlockData().equals(pending.previousState())) {
            return;
        }

        pending.counter().addLogBlock(pending.block());
        org.bukkit.entity.Player current = Bukkit.getPlayer(pending.playerId());
        if (current != null && current.isOnline() && !current.isDead()) {
            current.getInventory().remove(Material.BUCKET);
        }
    }

    private void supersedePendingBucketPlacement(Block block) {
        PendingBucketPlacement superseded = pendingBucketPlacements.remove(block);
        if (superseded != null) {
            superseded.counter().getAllBlocks().remove(block);
        }
    }

    private void removeExpectedBucketContents(PendingBucketPlacement pending) {
        BlockData current = pending.block().getBlockData();
        if (!current.equals(pending.previousState())) {
            pending.block().setBlockData(pending.previousState().clone(), false);
        }
    }

    private static List<Block> placedBlocks(BlockPlaceEvent event) {
        LinkedHashSet<Block> blocks = new LinkedHashSet<>();
        blocks.add(event.getBlockPlaced());
        if (event instanceof BlockMultiPlaceEvent multiPlaceEvent) {
            for (BlockState replacedState : multiPlaceEvent.getReplacedBlockStates()) {
                blocks.add(replacedState.getBlock());
            }
        }
        return List.copyOf(blocks);
    }

    private static Block affectedBucketBlock(PlayerBucketEmptyEvent event) {
        // Paper's server path always supplies getBlock(). The fallback only supports
        // extensions that still construct the event through its deprecated overload.
        return event.getBlock() != null ? event.getBlock() : event.getBlockClicked();
    }

    static Material bucketContents(Material bucket) {
        return switch (bucket) {
            case WATER_BUCKET -> Material.WATER;
            case LAVA_BUCKET -> Material.LAVA;
            case POWDER_SNOW_BUCKET -> Material.POWDER_SNOW;
            default -> null;
        };
    }

    static boolean isUnsafeBucketTarget(Block block) {
        return isUnsafeBucketTarget(
                block.getType(), block.getBlockData() instanceof Waterlogged);
    }

    static boolean isUnsafeBucketTarget(Material material, boolean waterlogged) {
        return waterlogged
                || (material != Material.AIR
                        && material != Material.CAVE_AIR
                        && material != Material.VOID_AIR);
    }

    private static boolean hasReactiveFluidNeighbor(Block affected, Material placedMaterial) {
        for (BlockFace face : new BlockFace[]{
                BlockFace.UP, BlockFace.DOWN, BlockFace.NORTH,
                BlockFace.SOUTH, BlockFace.EAST, BlockFace.WEST}) {
            if (reactiveFluids(placedMaterial, affected.getRelative(face).getType())) {
                return true;
            }
        }
        return false;
    }

    static boolean reactiveFluids(Material placedMaterial, Material neighboringMaterial) {
        return (placedMaterial == Material.WATER && neighboringMaterial == Material.LAVA)
                || (placedMaterial == Material.LAVA && neighboringMaterial == Material.WATER);
    }

    private record PendingBucketPlacement(
            Block block,
            BlockData previousState,
            UUID playerId,
            Counter counter) {
        private PendingBucketPlacement {
            Objects.requireNonNull(block, "block");
            Objects.requireNonNull(previousState, "previousState");
            Objects.requireNonNull(playerId, "playerId");
            Objects.requireNonNull(counter, "counter");
        }
    }
}
