package sakura.kooi.BridgingAnalyzer.menu;

import org.bukkit.entity.Player;

/** Permission boundary shared by command, NPC and in-menu entry points. */
@FunctionalInterface
public interface MenuPermissionChecker {
    boolean has(Player player, String permission);
}
