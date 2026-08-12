package sakura.kooi.BridgingSkin.lottery;

import java.util.List;
import java.util.Set;
import org.bukkit.Material;
import org.bukkit.entity.Player;

/**
 * Durable skin ownership boundary supplied by the plugin's storage layer.
 * Implementations must grant the whole batch atomically and return only after persistence succeeds.
 */
public interface RewardAuthorizer {
    Set<Material> ownedBy(Player player) throws RewardAuthorizationException;

    void grantAtomically(Player player, List<Material> rewards) throws RewardAuthorizationException;
}
