package sakura.kooi.BridgingAnalyzer.menu;

import org.bukkit.entity.Player;

/** Proxy/server-transfer boundary, normally backed by a BungeeCord Connect plugin message. */
@FunctionalInterface
public interface MenuServerConnector {
    boolean connect(Player player, String serverName);
}
