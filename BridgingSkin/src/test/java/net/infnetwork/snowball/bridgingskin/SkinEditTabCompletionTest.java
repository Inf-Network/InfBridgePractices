package net.infnetwork.snowball.bridgingskin;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.List;
import org.junit.jupiter.api.Test;

class SkinEditTabCompletionTest {
    @Test
    void hidesEverySuggestionWithoutAdminPermission() {
        assertEquals(List.of(), SkinEditCommand.tabSuggestions(
                false,
                new String[]{""},
                List.of("Snowball_233"),
                List.of("STONE")));
    }

    @Test
    void completesSubcommandsCaseInsensitively() {
        assertEquals(List.of("clear"), SkinEditCommand.tabSuggestions(
                true, new String[]{"C"}, List.of(), List.of()));
        assertEquals(List.of("clear", "edit"), SkinEditCommand.tabSuggestions(
                true, new String[]{""}, List.of(), List.of()));
    }

    @Test
    void completesOnlyOnlinePlayersForEdit() {
        assertEquals(List.of("alex", "Alice"), SkinEditCommand.tabSuggestions(
                true,
                new String[]{"edit", "al"},
                List.of("Snowball_233", "alex", "Alice"),
                List.of("ALABASTER")));
    }

    @Test
    void completesOnlyRestrictedMaterialsForClear() {
        assertEquals(List.of("STONE", "STONE_BRICKS"), SkinEditCommand.tabSuggestions(
                true,
                new String[]{"clear", "sto"},
                List.of("StonePlayer"),
                List.of("STONE_BRICKS", "DIORITE", "STONE")));
        assertEquals(List.of(), SkinEditCommand.tabSuggestions(
                true,
                new String[]{"unknown", ""},
                List.of("Alice"),
                List.of("STONE")));
        assertEquals(List.of(), SkinEditCommand.tabSuggestions(
                true,
                new String[]{"clear", "STONE", "extra"},
                List.of(),
                List.of("STONE")));
    }
}
