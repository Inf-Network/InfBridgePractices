package net.infnetwork.snowball.bridginganalyzer.menu;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.lang.reflect.Field;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.junit.jupiter.api.Test;

class MenuHolderAndActionTest {
    @Test
    void holderRetainsOwnerAndSessionIdsButNeverPlayerWrappers() {
        UUID owner = UUID.randomUUID();
        MenuEntry entry = new MenuEntry("test", 1, Material.STONE, "Test", List.of(), "",
                MenuBinding.left(new MenuAction.PlayerCommand("test"), false));
        MenuInventoryHolder first = new MenuInventoryHolder(
                MenuAction.Screen.MAIN, owner, Map.of(1, entry));
        MenuInventoryHolder second = new MenuInventoryHolder(
                MenuAction.Screen.MAIN, owner, Map.of(1, entry));

        assertEquals(owner, first.owner());
        assertNotEquals(first.sessionId(), second.sessionId());
        assertTrue(first.action(1, MenuButton.LEFT).isPresent());
        assertTrue(first.action(1, MenuButton.RIGHT).isEmpty());
        assertFalse(Arrays.stream(MenuInventoryHolder.class.getDeclaredFields())
                .map(Field::getType)
                .anyMatch(Player.class::isAssignableFrom));
    }

    @Test
    void commandNormalizationRemovesOneLeadingSlashAndRejectsLineInjection() {
        assertEquals("bridge speed", new MenuAction.PlayerCommand(" /bridge speed ").command());
        assertThrows(IllegalArgumentException.class,
                () -> new MenuAction.ConsoleCommand("warp safe\nstop"));
        assertThrows(IllegalArgumentException.class,
                () -> new MenuAction.PlayerCommand("   "));
    }

    @Test
    void paidActionRejectsNonPositiveOrNonFiniteCosts() {
        MenuAction action = new MenuAction.PlayerCommand("spawn");
        assertThrows(IllegalArgumentException.class,
                () -> new MenuAction.Paid(0.0D, action, true));
        assertThrows(IllegalArgumentException.class,
                () -> new MenuAction.Paid(Double.NaN, action, true));
    }
}
