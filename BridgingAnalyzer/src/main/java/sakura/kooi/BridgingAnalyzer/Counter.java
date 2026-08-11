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
    private Player player;
    private boolean speedCountEnabled = true;
    private boolean PvPEnabled = false;
    private boolean highlightEnabled = true;
    private boolean standBridgeMarkerEnabled = false;

    public Counter(Player p) {
        this.player = p;
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

    public void setCheckPoint(Location loc) {
        // 必须 clone。Location#add 是就地修改的,原版直接拿传进来的 loc 去 add(0,-1,0)
        // 找脚下的箱子,结果把刚存好的 checkPoint 一起改低了一格 ——
        // 之后每次回检查点都会落到绿宝石块自己所在的那一格里。
        this.checkPoint = loc.clone();
        Block target = loc.clone().add(0.0, -1.0, 0.0).getBlock().getRelative(BlockFace.DOWN, 3);
        if (target.getType() == Material.CHEST) {
            BridgingAnalyzer.clearInventory(this.player);
            Chest chest = (Chest)target.getState();
            for (ItemStack stack : chest.getBlockInventory().getContents()) {
                if (stack == null) continue;
                Utils.addItem(this.player.getInventory(), stack.clone());
            }
            this.player.getWorld().playSound(this.player.getLocation(), Sound.ENTITY_ITEM_PICKUP, 1.0f, 1.0f);
        }
    }

    /** 当前检查点(绿宝石块设的传送点;从未设过则是世界出生点)。返回副本,防止调用方就地改坏。 */
    public Location getCheckPoint() {
        return this.checkPoint.clone();
    }

    public void teleportCheckPoint() {        this.player.teleport(this.checkPoint);
        Block target = this.checkPoint.getBlock().getRelative(BlockFace.DOWN, 3);
        if (target.getType() == Material.CHEST) {
            BridgingAnalyzer.clearInventory(this.player);
            Chest chest = (Chest)target.getState();
            for (ItemStack stack : chest.getBlockInventory().getContents()) {
                if (stack == null) continue;
                Utils.addItem(this.player.getInventory(), stack.clone());
            }
            this.player.getWorld().playSound(this.player.getLocation(), Sound.ENTITY_ITEM_PICKUP, 1.0f, 1.0f);
        }
    }

    public void vectoryBreakBlock() {
        this.counterCPS.clear();
        this.counterBridge.clear();
        this.currentLength = 0;
        for (Block b : this.allBlock) {
            if (b.getType() == Material.AIR) continue;
            b.setType(Material.SEA_LANTERN);
        }
        BridgingAnalyzer.teleportCheckPoint(this.player);
        this.breakBlock();
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

