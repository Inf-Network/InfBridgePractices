package net.infnetwork.snowball.bridginganalyzer.commands;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Set;
import org.bukkit.configuration.file.YamlConfiguration;
import org.junit.jupiter.api.Test;

class AnalyzerTabCompleterTest {
    @Test
    void bridgeCompletesPublicSubcommandsWithoutLeakingAdminRemove() {
        assertEquals(
                List.of("highlight", "pvp", "reset", "speed", "stand"),
                complete("bridge", "", true, false, false, List.of()));
        assertEquals(
                List.of(),
                complete("bridge", "rem", true, false, false, List.of()));
        assertEquals(
                List.of("remove"),
                complete("bridge", "REM", true, false, true, List.of()));
    }

    @Test
    void bridgeHelpAlsoHidesTheAdministrativeRemoveHint() {
        assertFalse(BridgeCommand.helpLines(false).stream()
                .anyMatch(line -> line.contains("/bridge remove")));
        assertTrue(BridgeCommand.helpLines(true).stream()
                .anyMatch(line -> line.contains("/bridge remove")));
    }

    @Test
    void bridgeDoesNotSuggestPlayerOnlyOptionsToConsoleOrAfterFirstArgument() {
        assertEquals(
                List.of(),
                complete("bridge", "", false, false, true, List.of()));
        assertEquals(
                List.of(),
                AnalyzerTabCompleter.complete(
                        "bridge", new String[]{"speed", ""}, true, false, true, List.of()));
    }

    @Test
    void clearBlockCompletesOnlyOnlineCandidatesForAuthorizedSenders() {
        List<String> players = List.of("Zulu", "snowball_233", "SnowFox");

        assertEquals(
                List.of(),
                complete("clearblock", "s", true, false, false, players));
        assertEquals(
                List.of("snowball_233", "SnowFox"),
                complete("clearblock", "sNo", true, true, false, players));
        assertEquals(
                List.of("Zulu"),
                complete("CLEARBLOCK", "z", false, true, false, players));
    }

    @Test
    void commandsWithoutArgumentsReturnAnExplicitEmptyList() {
        for (String command : Set.of(
                "imstuck", "genvillager", "bsaveworld", "cd", "warpbridge")) {
            assertEquals(
                    List.of(),
                    complete(command, "", true, true, true, List.of("Player")),
                    command);
        }
        assertEquals(
                List.of(),
                complete("unknown", "", true, true, true, List.of("Player")));
    }

    @Test
    void supportedCommandsExactlyMatchPluginMetadata() throws Exception {
        try (InputStream stream = getClass().getClassLoader().getResourceAsStream("plugin.yml")) {
            if (stream == null) {
                throw new AssertionError("plugin.yml is missing");
            }
            YamlConfiguration plugin = YamlConfiguration.loadConfiguration(
                    new InputStreamReader(stream, StandardCharsets.UTF_8));

            assertEquals(
                    plugin.getConfigurationSection("commands").getKeys(false),
                    AnalyzerTabCompleter.supportedCommandNames());
        }
    }

    private static List<String> complete(
            String command,
            String fragment,
            boolean playerSender,
            boolean canClear,
            boolean canRemove,
            List<String> onlinePlayers
    ) {
        return AnalyzerTabCompleter.complete(
                command,
                new String[]{fragment},
                playerSender,
                canClear,
                canRemove,
                onlinePlayers);
    }
}
