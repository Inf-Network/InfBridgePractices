package net.infnetwork.snowball.blocklv.commands;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.Test;

class BlockLvTabCompletionTest {
    private static final List<String> PLAYERS = List.of("Snowball_233", "GreenNa");

    @Test
    void filtersSubcommandsByPermissionAndPrefix() {
        Set<String> permissions = Set.of("blocklv.add", "blocklv.decreaselevel");

        assertEquals(
                List.of("add"),
                BlockLvTabCompletion.complete(
                        new String[]{"a"}, permissions::contains, true, PLAYERS));
        assertEquals(
                List.of("decreaselevel"),
                BlockLvTabCompletion.complete(
                        new String[]{"decrease"}, permissions::contains, true, PLAYERS));
    }

    @Test
    void completesOnlinePlayersAndCommonPositiveAmounts() {
        assertEquals(
                List.of("Snowball_233"),
                BlockLvTabCompletion.complete(
                        new String[]{"decrease", "s"}, permission -> true, true, PLAYERS));
        assertEquals(
                List.of("1", "10", "100", "1000"),
                BlockLvTabCompletion.complete(
                        new String[]{"decreaselevel", "Snowball_233", "1"},
                        permission -> true,
                        true,
                        PLAYERS));
    }

    @Test
    void suppressesArgumentsForDeniedUnknownAndCompletedCommands() {
        assertEquals(
                List.of(),
                BlockLvTabCompletion.complete(
                        new String[]{"clear", ""}, permission -> false, true, PLAYERS));
        assertEquals(
                List.of(),
                BlockLvTabCompletion.complete(
                        new String[]{"refresh", ""}, permission -> true, true, PLAYERS));
        assertEquals(
                List.of(),
                BlockLvTabCompletion.complete(
                        new String[]{"unknown", ""}, permission -> true, true, PLAYERS));
        assertEquals(
                List.of(),
                BlockLvTabCompletion.complete(
                        new String[]{"set"}, permission -> true, false, PLAYERS));
    }
}
