package net.infnetwork.snowball.bridginganalyzer;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.UUID;
import org.bukkit.Bukkit;
import org.bukkit.Sound;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.block.Chest;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.Plugin;
import org.bukkit.scheduler.BukkitTask;
import net.infnetwork.snowball.bridginganalyzer.BridgingAnalyzer;
import net.infnetwork.snowball.bridginganalyzer.block.PlacementLedger;
import net.infnetwork.snowball.bridginganalyzer.block.PracticeBlockRegistry;
import net.infnetwork.snowball.bridginganalyzer.utils.Utils;

public class Counter {
    public static HashSet<Block> scheduledBreakBlocks = new HashSet();
    private static final int MAX_BREAK_RETRIES = 8;
    private static final long MAX_BREAK_RETRY_DELAY_TICKS = 100L;
    private ArrayList<Long> counterCPS = new ArrayList();
    private int maxCPS = 0;
    private ArrayList<Long> counterBridge = new ArrayList();
    private double maxBridge = 0.0;
    private int currentLength = 0;
    private int maxLength = 0;
    private ArrayList<Block> allBlock = new ArrayList();
    private Block lastBlock;
    private Location checkPoint = Bukkit.getWorld((String)"world").getSpawnLocation().add(0.5, 1.0, 0.5);
    private boolean speedCountEnabled = true;
    private boolean PvPEnabled = false;
    private boolean highlightEnabled = true;
    private boolean standBridgeMarkerEnabled = false;
    private final UUID ownerId;

    public Counter() {
        // Preserve the public constructor without falling back to raw Block cleanup.
        // A synthetic owner still receives generation tokens, so a delayed legacy
        // cleanup cannot delete a creative replacement at the same coordinates.
        this(UUID.randomUUID());
    }

    public Counter(UUID ownerId) {
        this.ownerId = ownerId;
    }

    /**
     * Compatibility constructor for extensions compiled against older releases.
     * Only the UUID is retained; the reconnect-sensitive Player wrapper is not.
     */
    @Deprecated
    public Counter(Player player) {
        this(player.getUniqueId());
    }

    public void addLogBlock(Block block) {
        this.recordBlock(block);
    }

    public void breakBlock() {
        new BreakRunnable(new ArrayList<Block>(this.allBlock));
        this.allBlock.clear();
    }

    public void countBridge(Block block) {
        this.recordBlock(block);
        if (this.lastBlock != null && this.lastBlock.getY() + 1 != block.getY()) {
            this.counterBridge.add(System.currentTimeMillis());
            ++this.currentLength;
            if (this.currentLength > this.maxLength) {
                this.maxLength = this.currentLength;
            }
        }
        this.lastBlock = block;
        this.removeBridgeTimeout();
        this.getBridgeSpeed();
    }

    public void countCPS() {
        this.counterCPS.add(System.currentTimeMillis());
        this.removeCPSTimeout();
        if (this.counterCPS.size() > this.maxCPS) {
            this.maxCPS = this.counterCPS.size();
        }
    }

    public ArrayList<Block> getAllBlocks() {
        return this.allBlock;
    }

    public int getBridgeLength() {
        return this.currentLength;
    }

    public double getBridgeSpeed() {
        double result;
        if (this.counterBridge.isEmpty()) {
            result = 0.0;
        } else {
            long peri = this.counterBridge.get(this.counterBridge.size() - 1) - this.counterBridge.get(0);
            if (peri > 1000L) {
                result = (double)this.counterBridge.size() / ((double)peri / 1000.0);
                if (result > this.maxBridge) {
                    this.maxBridge = Utils.formatDouble(result);
                }
            } else {
                result = this.counterBridge.size();
            }
        }
        return Utils.formatDouble(result);
    }

    public int getCPS() {
        return this.counterCPS.size();
    }

    public int getMaxBridgeLength() {
        return this.maxLength;
    }

    public double getMaxBridgeSpeed() {
        return this.maxBridge;
    }

    public int getMaxCPS() {
        return this.maxCPS;
    }

    public void instantBreakBlock() {
        PracticeBlockRegistry registry = BridgingAnalyzer.practiceBlocks();
        if (this.ownerId != null && registry != null) {
            int retries = registry.cleanupNow(this.ownerId);
            BridgingAnalyzer plugin = BridgingAnalyzer.getInstance();
            if (retries > 0 && plugin != null && plugin.isEnabled()
                    && !BridgingAnalyzer.isShuttingDown()) {
                // Keep retrying on the main thread. The generation tokens ensure this
                // task can never delete a newer creative/player replacement.
                new BreakRunnable(new ArrayList<Block>(this.allBlock));
            }
            this.allBlock.clear();
            return;
        }

        for (Block b : new ArrayList<>(this.allBlock)) {
            try {
                Utils.breakBlock(b);
                this.allBlock.remove(b);
                BridgingAnalyzer.getPlacedBlocks().remove(b);
            } catch (RuntimeException exception) {
                BridgingAnalyzer.getInstance().getLogger().warning(
                        "清理旧版练习方块失败，将保留记录: " + exception.getMessage());
            }
        }
    }

    public void removeBlockRecord(Block b) {
        BridgingAnalyzer.forgetPracticeBlock(b);
    }

    /** Internal compatibility-list cleanup; the generation ledger remains authoritative. */
    void removeBlockRecordLocally(Block block) {
        this.allBlock.removeIf(block::equals);
    }

    private void removeBridgeTimeout() {
        while (!this.counterBridge.isEmpty() && System.currentTimeMillis() - this.counterBridge.get(0) > 3000L) {
            this.counterBridge.remove(0);
        }
    }

    private void removeCPSTimeout() {
        while (!this.counterCPS.isEmpty() && System.currentTimeMillis() - this.counterCPS.get(0) > 1000L) {
            this.counterCPS.remove(0);
        }
    }

    public void reset() {
        this.counterCPS.clear();
        this.maxCPS = 0;
        this.counterBridge.clear();
        this.maxBridge = 0.0;
        this.currentLength = 0;
        this.lastBlock = null;
        this.breakBlock();
    }

    /** A real death has no victory animation, so remove the owner's blocks immediately. */
    public void resetImmediately() {
        this.counterCPS.clear();
        this.maxCPS = 0;
        this.counterBridge.clear();
        this.maxBridge = 0.0;
        this.currentLength = 0;
        this.lastBlock = null;
        this.instantBreakBlock();
    }

    public void resetMax() {
        this.maxCPS = 0;
        this.maxLength = 0;
    }

    public void resetMaxLength() {
        this.maxLength = 0;
    }

    public void setCheckPoint(Location loc, Player player) {
        // Location#add mutates its receiver, so the stored checkpoint must be detached.
        this.checkPoint = loc.clone();
        // 箱子是显式检查点套装（包括空箱子）；没有箱子时必须立刻恢复
        // 出生时的默认练习方块，不能把玩家踩绿宝石前的物品原样留下。
        BridgingAnalyzer.restorePreferredCheckPointLoadout(player, this);
    }

    /** @deprecated Use {@link #setCheckPoint(Location, Player)} with the current player. */
    @Deprecated
    public void setCheckPoint(Location loc) {
        Player player = currentPlayer();
        if (player == null) {
            this.checkPoint = loc.clone();
            return;
        }
        setCheckPoint(loc, player);
    }

    /** 当前检查点(绿宝石块设的传送点;从未设过则是世界出生点)。返回副本,防止调用方就地改坏。 */
    public Location getCheckPoint() {
        return this.checkPoint.clone();
    }

    /**
     * Restore the chest loadout belonging to this checkpoint.
     *
     * @return true when the checkpoint has a chest, including an intentionally empty chest
     */
    public boolean restoreCheckPointLoadout(Player player) {
        Block target = getCheckPointLoadoutBlock();
        if (target.getType() != Material.CHEST) {
            return false;
        }
        Chest chest = (Chest)target.getState();
        // Resolve the complete kit before clearing anything. Plain SANDSTONE in a
        // preset chest is a placeholder for the player's selected block skin.
        // The chest itself is never modified.
        CheckpointLoadoutResolver.Resolution loadout = CheckpointLoadoutResolver.resolve(
                chest.getBlockInventory().getContents(),
                () -> BridgingAnalyzer.resolvePracticeBlocks(player));
        BridgingAnalyzer.clearInventory(player);
        for (ItemStack stack : loadout.items()) {
            Utils.addItem(player.getInventory(), stack);
        }
        if (loadout.skinFailure() != null) {
            BridgingAnalyzer.getInstance().getLogger().warning(
                    "无法读取 " + player.getName() + " 的当前方块皮肤，检查点套装已保留原砂岩: "
                            + loadout.skinFailure().getMessage());
        }
        try {
            player.getWorld().playSound(player.getLocation(), Sound.ENTITY_ITEM_PICKUP, 1.0f, 1.0f);
        } catch (RuntimeException ex) {
            // Sound is cosmetic; it must not turn a successfully restored (possibly empty)
            // chest kit into the default kit.
            BridgingAnalyzer.getInstance().getLogger().warning(
                    "播放检查点套装音效失败: " + ex.getMessage());
        }
        return true;
    }

    /** Compatibility facade retained for code compiled against the first modular release. */
    public void restoreCheckPointItems(Player player) {
        restoreCheckPointLoadout(player);
    }

    private Block getCheckPointLoadoutBlock() {
        return checkPointLoadoutBase(this.checkPoint)
                .getBlock().getRelative(BlockFace.DOWN, 3);
    }

    static Location checkPointLoadoutBase(Location checkPoint) {
        return checkPoint.clone().add(0.0, -1.0, 0.0);
    }

    /** @deprecated Use {@link BridgingAnalyzer#teleportCheckPoint(Player)}. */
    @Deprecated
    public void teleportCheckPoint() {
        Player player = currentPlayer();
        if (player != null) {
            BridgingAnalyzer.teleportCheckPoint(player);
        }
    }

    public void vectoryBreakBlock(Player player) {
        try {
            prepareVictoryBlocks();
            BridgingAnalyzer.teleportCheckPoint(player);
        } finally {
            // Victory rendering, inventory and teleport providers must never be able
            // to prevent cleanup.
            this.breakBlock();
        }
    }

    private void prepareVictoryBlocks() {
        this.counterCPS.clear();
        this.counterBridge.clear();
        this.currentLength = 0;
        PracticeBlockRegistry registry = BridgingAnalyzer.practiceBlocks();
        if (this.ownerId != null && registry != null) {
            registry.prepareVictory(this.ownerId);
            return;
        }
        for (Block b : this.allBlock) {
            if (b.getType() == Material.AIR) continue;
            b.setType(Material.SEA_LANTERN);
        }
    }

    /** @deprecated Use {@link #vectoryBreakBlock(Player)} with the current player. */
    @Deprecated
    public void vectoryBreakBlock() {
        Player player = currentPlayer();
        try {
            prepareVictoryBlocks();
            if (player != null) {
                BridgingAnalyzer.teleportCheckPoint(player);
            }
        } finally {
            this.breakBlock();
        }
    }

    private Player currentPlayer() {
        return this.ownerId == null ? null : Bukkit.getPlayer(this.ownerId);
    }

    private void recordBlock(Block block) {
        if (this.ownerId != null && BridgingAnalyzer.practiceBlocks() != null) {
            BridgingAnalyzer.trackPracticeBlock(this.ownerId, block);
        } else {
            BridgingAnalyzer.getPlacedBlocks().put(block, block.getState().getData());
        }
        this.allBlock.add(block);
    }

    public boolean isSpeedCountEnabled() {
        return this.speedCountEnabled;
    }

    public void setSpeedCountEnabled(boolean speedCountEnabled) {
        this.speedCountEnabled = speedCountEnabled;
    }

    public boolean isPvPEnabled() {
        return this.PvPEnabled;
    }

    public void setPvPEnabled(boolean PvPEnabled) {
        this.PvPEnabled = PvPEnabled;
    }

    public boolean isHighlightEnabled() {
        return this.highlightEnabled;
    }

    public void setHighlightEnabled(boolean highlightEnabled) {
        this.highlightEnabled = highlightEnabled;
    }

    public boolean isStandBridgeMarkerEnabled() {
        return this.standBridgeMarkerEnabled;
    }

    public void setStandBridgeMarkerEnabled(boolean standBridgeMarkerEnabled) {
        this.standBridgeMarkerEnabled = standBridgeMarkerEnabled;
    }

    public class BreakRunnable
    implements Runnable {
        BukkitTask task;
        ArrayList<Block> blocks = new ArrayList();
        ArrayList<PlacementLedger.Entry<PracticeBlockRegistry.BlockKey, UUID, Material>> placements =
                new ArrayList<>();
        private final long normalDelayTicks;
        private int retryCount;
        private boolean stopped;

        public BreakRunnable(ArrayList<Block> allBlocks) {
            PracticeBlockRegistry registry = BridgingAnalyzer.practiceBlocks();
            if (ownerId != null && registry != null) {
                this.placements.addAll(registry.snapshot(ownerId));
                for (PlacementLedger.Entry<PracticeBlockRegistry.BlockKey, UUID, Material> entry
                        : this.placements) {
                    Block block = entry.key().resolve();
                    if (block != null) {
                        scheduledBreakBlocks.add(block);
                    }
                }
            } else {
                this.blocks.addAll(allBlocks);
                scheduledBreakBlocks.addAll(this.blocks);
            }
            int size = this.placements.isEmpty() ? this.blocks.size() : this.placements.size();
            if (size == 0) {
                this.normalDelayTicks = 1L;
                return;
            }
            int tick = 1 + 60 / size;
            if (tick > 3) {
                tick = 3;
            }
            this.normalDelayTicks = tick;
            this.scheduleNext(10L);
        }

        @Override
        public void run() {
            this.task = null;
            if (this.stopped) {
                return;
            }
            PracticeBlockRegistry registry = BridgingAnalyzer.practiceBlocks();
            if (!this.placements.isEmpty()) {
                if (registry == null) {
                    this.stop();
                    return;
                }
                while (!this.placements.isEmpty()) {
                    PlacementLedger.Entry<PracticeBlockRegistry.BlockKey, UUID, Material> entry =
                            this.placements.get(0);
                    PracticeBlockRegistry.DeleteResult result;
                    try {
                        result = registry.delete(entry);
                    } catch (RuntimeException exception) {
                        BridgingAnalyzer.getInstance().getLogger().warning(
                                "清理练习方块失败，将重试: " + exception.getMessage());
                        result = PracticeBlockRegistry.DeleteResult.RETRY;
                    }
                    if (result == PracticeBlockRegistry.DeleteResult.RETRY) {
                        this.placements.remove(0);
                        this.placements.add(entry);
                        this.retryOrStop("练习方块 " + entry.key());
                        return;
                    }
                    this.placements.remove(0);
                    this.retryCount = 0;
                    if (result == PracticeBlockRegistry.DeleteResult.DELETED) {
                        this.scheduleNext(this.normalDelayTicks);
                        return;
                    }
                }
                this.stop();
                return;
            }

            while (!this.blocks.isEmpty()) {
                Block block = this.blocks.get(0);
                Material blockType;
                try {
                    blockType = block.getType();
                } catch (RuntimeException exception) {
                    this.blocks.remove(0);
                    this.blocks.add(block);
                    BridgingAnalyzer.getInstance().getLogger().warning(
                            "读取旧版练习方块失败，将重试: " + exception.getMessage());
                    this.retryOrStop("旧版练习方块");
                    return;
                }
                if (blockType == Material.AIR) {
                    this.blocks.remove(0);
                    scheduledBreakBlocks.remove(block);
                    BridgingAnalyzer.getPlacedBlocks().remove(block);
                    continue;
                }
                try {
                    Utils.breakBlock(block);
                } catch (RuntimeException exception) {
                    // Delete first, retire tracking second. Rotate the failed block so
                    // later blocks still make progress on subsequent task runs.
                    this.blocks.remove(0);
                    this.blocks.add(block);
                    BridgingAnalyzer.getInstance().getLogger().warning(
                            "清理旧版练习方块失败，将重试: " + exception.getMessage());
                    this.retryOrStop("旧版练习方块 " + block.getLocation());
                    return;
                }
                this.blocks.remove(0);
                scheduledBreakBlocks.remove(block);
                BridgingAnalyzer.getPlacedBlocks().remove(block);
                this.retryCount = 0;
                this.scheduleNext(this.normalDelayTicks);
                return;
            }
            this.stop();
        }

        private void retryOrStop(String description) {
            ++this.retryCount;
            if (this.retryCount > MAX_BREAK_RETRIES) {
                BridgingAnalyzer plugin = BridgingAnalyzer.getInstance();
                if (plugin != null) {
                    plugin.getLogger().warning(description + " 连续清理失败，已停止本轮重试；"
                            + "记录会保留到下次重置或全局清理");
                }
                this.stop();
                return;
            }
            long multiplier = 1L << Math.min(this.retryCount - 1, 6);
            this.scheduleNext(Math.min(
                    MAX_BREAK_RETRY_DELAY_TICKS, this.normalDelayTicks * multiplier));
        }

        private void scheduleNext(long delayTicks) {
            BridgingAnalyzer plugin = BridgingAnalyzer.getInstance();
            if (this.stopped || plugin == null || !plugin.isEnabled()
                    || BridgingAnalyzer.isShuttingDown()) {
                this.stop();
                return;
            }
            try {
                this.task = Bukkit.getScheduler().runTaskLater(
                        (Plugin)plugin, (Runnable)this, Math.max(1L, delayTicks));
            } catch (RuntimeException exception) {
                plugin.getLogger().warning("无法排队练习方块清理任务: " + exception.getMessage());
                this.stop();
            }
        }

        private void stop() {
            this.stopped = true;
            if (this.task != null) {
                try {
                    this.task.cancel();
                } catch (RuntimeException ignored) {
                    // A completed or server-shutdown task may no longer be cancellable.
                }
                this.task = null;
            }
        }
    }
}

