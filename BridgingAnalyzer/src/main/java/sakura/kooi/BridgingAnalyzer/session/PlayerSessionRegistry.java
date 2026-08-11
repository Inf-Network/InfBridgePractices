package sakura.kooi.BridgingAnalyzer.session;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import org.bukkit.entity.Player;
import sakura.kooi.BridgingAnalyzer.Counter;

/**
 * Stores per-player practice state by UUID.
 *
 * <p>Never key sessions by {@link Player}: Paper compares player wrappers by UUID,
 * so a reconnect can find a session that still points at an old, offline entity.</p>
 */
public final class PlayerSessionRegistry {
    private final Map<UUID, Counter> sessions = new HashMap<>();

    public Counter get(Player player) {
        return sessions.computeIfAbsent(player.getUniqueId(), Counter::new);
    }

    public Counter getIfPresent(Player player) {
        return sessions.get(player.getUniqueId());
    }

    public Counter remove(Player player) {
        return sessions.remove(player.getUniqueId());
    }

    public Collection<Counter> values() {
        return sessions.values();
    }

    public void clear() {
        sessions.clear();
    }
}
