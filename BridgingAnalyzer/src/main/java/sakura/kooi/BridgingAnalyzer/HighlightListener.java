/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  org.bukkit.Location
 *  org.bukkit.Material
 *  org.bukkit.block.Block
 *  org.bukkit.block.BlockFace
 *  org.bukkit.entity.Player
 *  org.bukkit.event.EventHandler
 *  org.bukkit.event.Listener
 *  org.bukkit.event.player.PlayerMoveEvent
 */
package sakura.kooi.BridgingAnalyzer;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import sakura.kooi.BridgingAnalyzer.BridgingAnalyzer;
import org.bukkit.Particle;

public class HighlightListener
implements Listener {
    private final Map<UUID, Block> highlightHistory = new HashMap<>();

    private Block getRelativeBrick(Block b) {
        Block relative = b.getRelative(BlockFace.EAST);
        if (relative.getType() == Material.STONE_BRICKS) {
            return relative;
        }
        relative = b.getRelative(BlockFace.WEST);
        if (relative.getType() == Material.STONE_BRICKS) {
            return relative;
        }
        relative = b.getRelative(BlockFace.SOUTH);
        if (relative.getType() == Material.STONE_BRICKS) {
            return relative;
        }
        relative = b.getRelative(BlockFace.NORTH);
        if (relative.getType() == Material.STONE_BRICKS) {
            return relative;
        }
        return null;
    }

    @EventHandler
    public void onFallDown(PlayerMoveEvent e) {
        Block historyBlock;
        if (e.getTo().getY() < 0.0 && (historyBlock = this.highlightHistory.get(e.getPlayer().getUniqueId())) != null) {
            e.getPlayer().sendBlockChange(historyBlock.getLocation(), historyBlock.getType(), historyBlock.getData());
        }
    }

    @EventHandler
    public void onMove(PlayerMoveEvent e) {
        Block target;
        if (!BridgingAnalyzer.getCounter(e.getPlayer()).isHighlightEnabled()) {
            return;
        }
        if (!e.getFrom().getBlock().equals(e.getTo().getBlock()) && (target = this.getRelativeBrick(this.roundLocation(e.getTo().clone().add(0.0, -1.0, 0.0)).getBlock())) != null) {
            Block historyBlock = this.highlightHistory.get(e.getPlayer().getUniqueId());
            if (historyBlock != null) {
                e.getPlayer().sendBlockChange(historyBlock.getLocation(), historyBlock.getType(), historyBlock.getData());
            }
            e.getPlayer().sendBlockChange(target.getLocation(), Material.SNOW_BLOCK, (byte)0);
            this.highlightHistory.put(e.getPlayer().getUniqueId(), target);
        }
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        this.highlightHistory.remove(event.getPlayer().getUniqueId());
    }

    @EventHandler
    public void onStandBridgeMove(PlayerMoveEvent e) {
        if (!BridgingAnalyzer.getCounter(e.getPlayer()).isStandBridgeMarkerEnabled()) {
            return;
        }
        Location markerLoc = e.getTo().clone().add(0.08, 0.0, 0.08);
        markerLoc.getWorld().spawnParticle(Particle.MYCELIUM, markerLoc, 5, 0.0, 0.0, 0.0, 0.0);
    }

    private Location roundLocation(Location location) {
        Location loc = location.clone();
        loc.setX((double)Math.round(loc.getX()));
        loc.setZ((double)Math.round(loc.getZ()));
        return loc;
    }
}
