package net.infnetwork.snowball.bridginganalyzer.menu;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.bukkit.Material;
import org.bukkit.configuration.file.YamlConfiguration;
import org.junit.jupiter.api.Test;

class WarpMenuSettingsTest {
    @Test
    void bundledConfigurationReproducesSpawnAndNinePaidWarps() {
        MenuSettings settings = bundledSettings();
        Map<Integer, MenuEntry> bySlot = new HashMap<>();
        settings.warpEntries().forEach(entry -> bySlot.put(entry.slot(), entry));

        assertEquals(54, WarpMenuLayout.SIZE);
        assertEquals("&a&l快捷传送", settings.warpTitle());
        assertEquals("bridginganalyzer.menu.main", settings.mainPermission());
        assertEquals("bridginganalyzer.menu.warp", settings.warpPermission());
        assertEquals("bridginganalyzer.menu.item", settings.itemPermission());
        assertEquals(500.0D, settings.clearBlockCost());
        assertEquals(10, settings.warpEntries().size());
        assertEquals(Material.BEACON, bySlot.get(11).material());
        assertEquals(new MenuAction.PlayerCommand("spawn"), bySlot.get(11).binding().action());

        assertPaidWarp(bySlot.get(12), Material.SANDSTONE, "warp ceda {player}");
        assertPaidWarp(bySlot.get(13), Material.LIGHT_BLUE_TERRACOTTA, "warp qiangda {player}");
        assertPaidWarp(bySlot.get(14), Material.MELON, "warp susheng {player}");
        assertPaidWarp(bySlot.get(15), Material.SPRUCE_LOG, "warp sujiang {player}");
        assertPaidWarp(bySlot.get(20), Material.TNT, "warp TNT {player}");
        assertPaidWarp(bySlot.get(21), Material.RED_SANDSTONE, "warp youxian {player}");
        assertPaidWarp(bySlot.get(22), Material.WATER_BUCKET, "warp jiangluozijiu {player}");
        assertPaidWarp(bySlot.get(23), Material.STICK, "warp jituixunlian {player}");
        assertPaidWarp(bySlot.get(24), Material.DIAMOND_PICKAXE, "warp zhanqiao {player}");

        MenuEntry back = WarpMenuLayout.entries(settings).stream()
                .filter(entry -> entry.slot() == 49)
                .findFirst()
                .orElseThrow();
        assertEquals("back", back.id());
        assertEquals(new MenuAction.Open(MenuAction.Screen.MAIN), back.binding().action());
    }

    @Test
    void everyWarpPriceCanBeConfiguredIndependently() {
        YamlConfiguration config = bundledConfiguration();
        List<String> ids = List.of(
                "spawn", "ceda", "qiangda", "susheng", "sujiang",
                "tnt", "youxian", "jiangluozijiu", "jituixunlian", "zhanqiao");
        Map<String, Double> expected = new HashMap<>();
        for (int index = 0; index < ids.size(); index++) {
            double cost = index + 0.25D;
            String id = ids.get(index);
            config.set("warp.entries." + id + ".cost", cost);
            expected.put(id, cost);
        }

        MenuSettings settings = MenuSettings.load(config);
        for (MenuEntry entry : settings.warpEntries()) {
            MenuAction.Paid paid = assertInstanceOf(MenuAction.Paid.class,
                    entry.binding().action(), entry.id());
            assertEquals(expected.get(entry.id()), paid.cost(), entry.id());
            assertTrue(entry.lore().stream().anyMatch(line -> line.contains("{cost}")),
                    entry.id());
            assertEquals(MenuPrice.format(expected.get(entry.id())),
                    MenuPrice.format(paid.cost()), entry.id());
        }
    }

    @Test
    void missingMainPriceKeepsTheLegacyFiveHundredDefault() {
        YamlConfiguration config = bundledConfiguration();
        config.set("main.entries.clearblock.cost", null);

        assertEquals(500.0D, MenuSettings.load(config).clearBlockCost());
    }

    @Test
    void nonNumericAndUnsafePricesFailFastInsteadOfBecomingFree() {
        List<Object> invalid = List.of(
                "100", true, -1.0D, Double.NaN,
                Double.POSITIVE_INFINITY, Double.NEGATIVE_INFINITY);
        for (Object value : invalid) {
            YamlConfiguration mainConfig = bundledConfiguration();
            mainConfig.set("main.entries.clearblock.cost", value);
            IllegalArgumentException mainFailure = assertThrows(
                    IllegalArgumentException.class, () -> MenuSettings.load(mainConfig));
            assertTrue(mainFailure.getMessage().contains("main.entries.clearblock.cost"));

            YamlConfiguration warpConfig = bundledConfiguration();
            warpConfig.set("warp.entries.spawn.cost", value);
            IllegalArgumentException warpFailure = assertThrows(
                    IllegalArgumentException.class, () -> MenuSettings.load(warpConfig));
            assertTrue(warpFailure.getMessage().contains("warp.entries.spawn.cost"));
        }
    }

    @Test
    void futureDestinationsMayUseRemainingFiftyFourSlotSpace() {
        MenuEntry extension = new MenuEntry("future", 53, Material.ENDER_PEARL, "Future",
                List.of(), "", MenuBinding.left(new MenuAction.PlayerCommand("future"), false));

        assertEquals(List.of(extension), MenuSettings.validateWarpEntries(List.of(extension)));
    }

    @Test
    void profileAndReturnSlotsRemainReserved() {
        MenuEntry profileCollision = new MenuEntry("collision", 4, Material.STONE, "Collision",
                List.of(), "", MenuBinding.left(new MenuAction.PlayerCommand("noop"), false));
        MenuEntry returnCollision = new MenuEntry("collision2", 49, Material.STONE, "Collision",
                List.of(), "", MenuBinding.left(new MenuAction.PlayerCommand("noop"), false));

        assertThrows(IllegalArgumentException.class,
                () -> MenuSettings.validateWarpEntries(List.of(profileCollision)));
        assertThrows(IllegalArgumentException.class,
                () -> MenuSettings.validateWarpEntries(List.of(returnCollision)));
    }

    private static void assertPaidWarp(MenuEntry entry, Material icon, String command) {
        assertEquals(icon, entry.material());
        assertTrue(entry.binding().accepts(MenuButton.LEFT));
        MenuAction.Paid paid = assertInstanceOf(MenuAction.Paid.class, entry.binding().action());
        assertEquals(100.0D, paid.cost());
        MenuAction.ConsoleCommand action = assertInstanceOf(
                MenuAction.ConsoleCommand.class, paid.action());
        assertEquals(command, action.command());
        assertTrue(entry.lore().contains("&f花费: &6{cost}硬币"));
    }

    private static MenuSettings bundledSettings() {
        return MenuSettings.load(bundledConfiguration());
    }

    private static YamlConfiguration bundledConfiguration() {
        InputStream input = WarpMenuSettingsTest.class.getClassLoader()
                .getResourceAsStream("menus.yml");
        if (input == null) {
            throw new AssertionError("menus.yml is missing");
        }
        try (InputStreamReader reader = new InputStreamReader(input, StandardCharsets.UTF_8)) {
            return YamlConfiguration.loadConfiguration(reader);
        } catch (Exception exception) {
            throw new AssertionError(exception);
        }
    }
}
