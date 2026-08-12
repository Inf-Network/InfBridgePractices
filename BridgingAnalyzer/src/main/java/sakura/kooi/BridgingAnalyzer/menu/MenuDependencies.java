package sakura.kooi.BridgingAnalyzer.menu;

import java.util.Objects;

/** All external effects required by the native menus. */
public record MenuDependencies(
        MenuEconomy economy,
        MenuProfileProvider profiles,
        MenuPermissionChecker permissions,
        MenuCommandDispatcher commands,
        MenuBlockCleaner blockCleaner,
        MenuServerConnector serverConnector) {
    public MenuDependencies {
        Objects.requireNonNull(economy, "economy");
        Objects.requireNonNull(profiles, "profiles");
        Objects.requireNonNull(permissions, "permissions");
        Objects.requireNonNull(commands, "commands");
        Objects.requireNonNull(blockCleaner, "blockCleaner");
        Objects.requireNonNull(serverConnector, "serverConnector");
    }
}
