package net.infnetwork.snowball.bridgingskin.crate;

import com.sk89q.worldedit.IncompleteRegionException;
import com.sk89q.worldedit.LocalSession;
import com.sk89q.worldedit.WorldEdit;
import com.sk89q.worldedit.bukkit.BukkitAdapter;
import com.sk89q.worldedit.math.BlockVector3;
import com.sk89q.worldedit.regions.Region;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;

/** Reads FAWE selections through its WorldEdit-compatible public API. */
public final class WorldEditSelectionResolver implements SelectionResolver {
    @Override
    public Block resolveSingleBlock(Player player) throws CrateSelectionException {
        LocalSession session = WorldEdit.getInstance().getSessionManager().get(BukkitAdapter.adapt(player));
        final Region selection;
        try {
            selection = session.getSelection(BukkitAdapter.adapt(player.getWorld()));
        } catch (IncompleteRegionException exception) {
            throw new CrateSelectionException("请先用 WorldEdit/FAWE 选中一个方块", exception);
        }
        if (!selection.getMinimumPoint().equals(selection.getMaximumPoint())) {
            throw new CrateSelectionException("选区必须正好只有一个方块，当前为 " + selection.getVolume() + " 个");
        }
        BlockVector3 position = selection.getMinimumPoint();
        return player.getWorld().getBlockAt(position.x(), position.y(), position.z());
    }
}
