package net.infnetwork.snowball.blocklv.api;

import java.util.Objects;
import java.util.OptionalLong;
import java.util.UUID;
import net.infnetwork.snowball.blocklv.core.PointManger;

public final class BlockLevelAPI {
    private BlockLevelAPI() {
    }

    public static OptionalLong loadedLevel(UUID playerId) {
        PointManger points = PointManger.players.get(Objects.requireNonNull(playerId, "playerId"));
        return points == null
                ? OptionalLong.empty()
                : OptionalLong.of(Math.max(0L, points.lv));
    }
}
