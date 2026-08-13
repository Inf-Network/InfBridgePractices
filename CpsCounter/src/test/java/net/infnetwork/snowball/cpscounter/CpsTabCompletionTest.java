package net.infnetwork.snowball.cpscounter;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.List;
import org.junit.jupiter.api.Test;

class CpsTabCompletionTest {
    private static final List<String> PLAYERS = List.of("Snowball_233", "GreenNa");

    @Test
    void completesVisiblePlayersForPublicQueries() {
        assertEquals(
                List.of("Snowball_233"),
                CpsTabCompletion.complete(new String[]{"s"}, false, true, PLAYERS));
    }

    @Test
    void exposesManagementModesOnlyWithPermission() {
        assertEquals(
                List.of(),
                CpsTabCompletion.complete(new String[]{"#"}, false, true, PLAYERS));
        assertEquals(
                List.of("#mon", "#silent"),
                CpsTabCompletion.complete(new String[]{"#"}, true, true, PLAYERS));
        assertEquals(
                List.of("GreenNa"),
                CpsTabCompletion.complete(new String[]{"#mon", "g"}, true, true, PLAYERS));
        assertEquals(
                List.of(),
                CpsTabCompletion.complete(new String[]{"#"}, true, false, PLAYERS));
    }

    @Test
    void suppressesUnexpectedExtraArguments() {
        assertEquals(
                List.of(),
                CpsTabCompletion.complete(
                        new String[]{"#silent", ""}, true, true, PLAYERS));
        assertEquals(
                List.of(),
                CpsTabCompletion.complete(
                        new String[]{"Snowball_233", ""}, true, true, PLAYERS));
    }
}
