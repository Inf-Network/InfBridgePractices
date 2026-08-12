/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  org.bukkit.Bukkit
 *  org.bukkit.Location
 *  org.bukkit.Material
 *  org.bukkit.block.Block
 *  org.bukkit.block.BlockFace
 *  org.bukkit.block.Chest
 *  org.bukkit.entity.Player
 *  org.bukkit.inventory.ItemStack
 *  org.bukkit.plugin.Plugin
 *  org.bukkit.scheduler.BukkitTask
 */
package sakura.kooi.BridgingAnalyzer;

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
import sakura.kooi.BridgingAnalyzer.BridgingAnalyzer;
import sakura.kooi.BridgingAnalyzer.utils.Utils;

public class Counter {
    public static HashSet<Block> scheduledBreakBlocks = new HashSet();
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
        this.ownerId = null;
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
        this.allBlock.add(block);
        BridgingAnalyzer.getPlacedBlocks().put(block, block.getState().getData());
    }

    public void breakBlock() {
        scheduledBreakBlocks.addAll(this.allBlock);
        new BreakRunnable(new ArrayList<Block>(this.allBlock));
        this.allBlock.clear();
    }

    public void countBridge(Block block) {
        this.allBlock.add(block);
        BridgingAnalyzer.getPlacedBlocks().put(block, block.getState().getData());
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
        for (Block b : this.allBlock) {
            Utils.breakBlock(b);
            BridgingAnalyzer.getPlacedBlocks().remove(b);
        }
        this.allBlock.clear();
    }

    public void removeBlockRecord(Block b) {
        this.allBlock.remove(b);
        BridgingAnalyzer.getPlacedBlocks().remove(b);
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
        this.breakBlock();
    }

    public void resetMax() {
        this.maxCPS = 0;
        this.maxLength = 0;
    }

    public void resetMaxLength() {
        this.maxLength = 0;
    }

    public void setCheckPoint(Location loc, Player player) {
        // 必须 clone。Location#add 是就地修改的,原版直接拿传进来的 loc 去 add(0,-1,0)
        // 找脚下的箱子,结果把刚存好的 checkPoint 一起改低了一格 ——
        // 之后每次回检查点都会落到绿宝石块自己所在的那一格里。
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
        // Keep this identical to the lookup used when the emerald checkpoint is set.
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
        prepareVictoryBlocks();
        BridgingAnalyzer.teleportCheckPoint(player);
        this.breakBlock();
    }

    private void prepareVictoryBlocks() {
        this.counterCPS.clear();
        this.counterBridge.clear();
        this.currentLength = 0;
        for (Block b : this.allBlock) {
            if (b.getType() == Material.AIR) continue;
            b.setType(Material.SEA_LANTERN);
        }
    }

    /** @deprecated Use {@link #vectoryBreakBlock(Player)} with the current player. */
    @Deprecated
    public void vectoryBreakBlock() {
        Player player = currentPlayer();
        prepareVictoryBlocks();
        if (player != null) {
            BridgingAnalyzer.teleportCheckPoint(player);
        }
        this.breakBlock();
    }

    private Player currentPlayer() {
        return this.ownerId == null ? null : Bukkit.getPlayer(this.ownerId);
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

        public BreakRunnable(ArrayList<Block> allBlocks) {
            this.blocks.addAll(allBlocks);
            scheduledBreakBlocks.addAll(this.blocks);
            if (this.blocks.isEmpty()) {
                return;
            }
            int tick = 1 + 60 / this.blocks.size();
            if (tick > 3) {
                tick = 3;
            }
            this.task = Bukkit.getScheduler().runTaskTimer((Plugin)BridgingAnalyzer.getInstance(), (Runnable)this, 10L, (long)tick);
        }

        @Override
        public void run() {
            if (!this.blocks.isEmpty()) {
                Block b = null;
                while (!(this.blocks.isEmpty() || b != null && b.getType() != Material.AIR)) {
                    b = this.blocks.get(0);
                    scheduledBreakBlocks.remove(b);
                    this.blocks.remove(0);
                    BridgingAnalyzer.getPlacedBlocks().remove(b);
                }
                if (b != null) {
                    Utils.breakBlock(b);
                    BridgingAnalyzer.getPlacedBlocks().remove(b);
                }
            } else {
                this.task.cancel();
            }
        }
    }
}

