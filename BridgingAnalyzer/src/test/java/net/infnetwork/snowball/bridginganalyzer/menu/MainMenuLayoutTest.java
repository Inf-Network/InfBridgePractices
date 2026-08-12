package net.infnetwork.snowball.bridginganalyzer.menu;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.bukkit.Material;
import org.junit.jupiter.api.Test;

class MainMenuLayoutTest {
    @Test
    void reproducesAuditedThirtySixSlotLayoutAndActions() {
        List<MenuEntry> entries = MainMenuLayout.entries();
        Map<Integer, MenuEntry> bySlot = bySlot(entries);

        assertEquals(36, MainMenuLayout.SIZE);
        assertEquals("&e&l搭路练习 &a&l可视化菜单", MainMenuLayout.TITLE);
        assertEquals(Map.of(
                4, "profile",
                12, "pickaxe",
                13, "warp",
                14, "clearblock",
                20, "pvp",
                21, "highlight",
                22, "stand",
                23, "speed",
                24, "lobby",
                31, "close"), slotIds(bySlot));

        assertEquals(Material.PLAYER_HEAD, bySlot.get(4).material());
        assertEquals(Material.GOLDEN_PICKAXE, bySlot.get(12).material());
        assertPlayerCommand(bySlot.get(12), "imstuck", true);
        assertInstanceOf(MenuAction.Open.class, bySlot.get(13).binding().action());

        MenuAction.Paid clear = assertInstanceOf(
                MenuAction.Paid.class, bySlot.get(14).binding().action());
        assertEquals(500.0D, clear.cost());
        assertEquals(MenuAction.ClearAll.INSTANCE, clear.action());
        assertTrue(bySlot.get(14).binding().closeAfter());

        assertPlayerCommand(bySlot.get(20), "bridge pvp", true);
        assertPlayerCommand(bySlot.get(21), "bridge highlight", true);
        assertPlayerCommand(bySlot.get(22), "bridge stand", true);
        assertPlayerCommand(bySlot.get(23), "bridge speed", true);
        assertEquals(new MenuAction.Connect("lobby_1"), bySlot.get(24).binding().action());
        assertEquals(MenuAction.Close.INSTANCE, bySlot.get(31).binding().action());
    }

    @Test
    void mainActionsAcceptBothAuditedMouseButtons() {
        for (MenuEntry entry : MainMenuLayout.entries()) {
            if (entry.binding() == null) {
                continue;
            }
            assertTrue(entry.binding().accepts(MenuButton.LEFT), entry.id());
            assertTrue(entry.binding().accepts(MenuButton.RIGHT), entry.id());
        }
    }

    private static void assertPlayerCommand(MenuEntry entry, String command, boolean closes) {
        MenuAction.PlayerCommand action = assertInstanceOf(
                MenuAction.PlayerCommand.class, entry.binding().action());
        assertEquals(command, action.command());
        assertEquals(closes, entry.binding().closeAfter());
    }

    private static Map<Integer, MenuEntry> bySlot(List<MenuEntry> entries) {
        Map<Integer, MenuEntry> bySlot = new HashMap<>();
        for (MenuEntry entry : entries) {
            assertEquals(null, bySlot.put(entry.slot(), entry), "duplicate slot " + entry.slot());
        }
        return bySlot;
    }

    private static Map<Integer, String> slotIds(Map<Integer, MenuEntry> entries) {
        Map<Integer, String> ids = new HashMap<>();
        entries.forEach((slot, entry) -> ids.put(slot, entry.id()));
        return ids;
    }
}
