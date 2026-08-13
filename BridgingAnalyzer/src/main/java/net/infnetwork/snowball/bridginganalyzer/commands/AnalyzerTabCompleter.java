package net.infnetwork.snowball.bridginganalyzer.commands;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.Set;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.PluginCommand;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

/** Context-aware completion shared by every command registered by BridgingAnalyzer. */
public final class AnalyzerTabCompleter implements TabCompleter {
    private static final String CLEAR_PERMISSION = "bridginganalyzer.clear";
    private static final String REMOVE_PERMISSION = "bridge.remove";
    private static final List<String> BRIDGE_SUBCOMMANDS =
            List.of("highlight", "pvp", "speed", "stand", "reset");
    private static final Set<String> SUPPORTED_COMMANDS = Set.of(
            "bridge",
            "clearblock",
            "imstuck",
            "genvillager",
            "bsaveworld",
            "cd",
            "warpbridge");

    /** Installs one non-null completer so Bukkit never falls back to unrelated player names. */
    public static void install(JavaPlugin plugin) {
        Objects.requireNonNull(plugin, "plugin");
        AnalyzerTabCompleter completer = new AnalyzerTabCompleter();
        for (String commandName : SUPPORTED_COMMANDS) {
            PluginCommand command = Objects.requireNonNull(
                    plugin.getCommand(commandName),
                    "plugin.yml 缺少命令 " + commandName);
            command.setTabCompleter(completer);
        }
    }

    /** Exposed as an immutable view for plugin.yml parity tests. */
    public static Set<String> supportedCommandNames() {
        return SUPPORTED_COMMANDS;
    }

    @Override
    public List<String> onTabComplete(
            CommandSender sender,
            Command command,
            String alias,
            String[] args
    ) {
        String commandName = command.getName().toLowerCase(Locale.ROOT);
        boolean playerSender = sender instanceof Player;
        boolean canClear = sender.hasPermission(CLEAR_PERMISSION);
        boolean canRemove = sender.hasPermission(REMOVE_PERMISSION);
        List<String> onlinePlayers = commandName.equals("clearblock")
                && canClear
                && args.length == 1
                ? visibleOnlinePlayerNames(sender)
                : List.of();
        return complete(
                commandName, args, playerSender, canClear, canRemove, onlinePlayers);
    }

    static List<String> complete(
            String commandName,
            String[] args,
            boolean playerSender,
            boolean canClear,
            boolean canRemove,
            List<String> onlinePlayers
    ) {
        if (args.length != 1) {
            return List.of();
        }
        String normalizedCommand = commandName.toLowerCase(Locale.ROOT);
        return switch (normalizedCommand) {
            case "bridge" -> playerSender
                    ? partial(args[0], bridgeSubcommands(canRemove))
                    : List.of();
            case "clearblock" -> canClear
                    ? partial(args[0], onlinePlayers)
                    : List.of();
            default -> List.of();
        };
    }

    private static List<String> bridgeSubcommands(boolean canRemove) {
        if (!canRemove) {
            return BRIDGE_SUBCOMMANDS;
        }
        List<String> commands = new ArrayList<>(BRIDGE_SUBCOMMANDS);
        commands.add("remove");
        return commands;
    }

    private static List<String> visibleOnlinePlayerNames(CommandSender sender) {
        Player viewer = sender instanceof Player player ? player : null;
        return sender.getServer().getOnlinePlayers().stream()
                .filter(candidate -> viewer == null || viewer.canSee(candidate))
                .map(Player::getName)
                .sorted(String.CASE_INSENSITIVE_ORDER.thenComparing(Comparator.naturalOrder()))
                .toList();
    }

    private static List<String> partial(String fragment, List<String> candidates) {
        List<String> matches = candidates.stream()
                .filter(candidate -> startsWithIgnoreCase(candidate, fragment))
                .sorted(String.CASE_INSENSITIVE_ORDER.thenComparing(Comparator.naturalOrder()))
                .toList();
        return List.copyOf(matches);
    }

    private static boolean startsWithIgnoreCase(String candidate, String fragment) {
        return fragment.length() <= candidate.length()
                && candidate.regionMatches(true, 0, fragment, 0, fragment.length());
    }
}
