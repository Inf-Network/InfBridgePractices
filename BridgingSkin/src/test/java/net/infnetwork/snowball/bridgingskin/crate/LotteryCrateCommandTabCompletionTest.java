package net.infnetwork.snowball.bridgingskin.crate;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.List;
import org.junit.jupiter.api.Test;

class LotteryCrateCommandTabCompletionTest {
    @Test
    void hidesActionsWithoutPermission() {
        assertEquals(List.of(), LotteryCrateCommand.tabSuggestions(
                false, true, new String[]{""}));
    }

    @Test
    void playerReceivesAllContextualActions() {
        assertEquals(List.of("set"), LotteryCrateCommand.tabSuggestions(
                true, true, new String[]{"s"}));
        assertEquals(List.of("clear", "clearall"), LotteryCrateCommand.tabSuggestions(
                true, true, new String[]{"cl"}));
    }

    @Test
    void consoleDoesNotReceiveSelectionOnlyActions() {
        assertEquals(List.of(), LotteryCrateCommand.tabSuggestions(
                true, false, new String[]{"set"}));
        assertEquals(List.of("info"), LotteryCrateCommand.tabSuggestions(
                true, false, new String[]{"i"}));
    }

    @Test
    void extraArgumentsHaveNoSuggestions() {
        assertEquals(List.of(), LotteryCrateCommand.tabSuggestions(
                true, true, new String[]{"set", "extra"}));
    }
}
