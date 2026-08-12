package sakura.kooi.BridgingSkin.crate;

import org.bukkit.block.Block;
import org.bukkit.entity.Player;

@FunctionalInterface
public interface SelectionResolver {
    Block resolveSingleBlock(Player player) throws CrateSelectionException;
}
