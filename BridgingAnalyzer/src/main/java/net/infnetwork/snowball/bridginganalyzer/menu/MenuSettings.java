package net.infnetwork.snowball.bridginganalyzer.menu;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;

record MenuSettings(String mainPermission, String warpPermission, String itemPermission, String warpTitle,
                    List<MenuEntry> warpEntries) {
    private static final int WARP_SIZE = 54;
    private static final Set<Integer> RESERVED_WARP_SLOTS = Set.of(4, 49);

    MenuSettings {
        mainPermission = normalizePermission(mainPermission, "bridginganalyzer.menu.main");
        warpPermission = normalizePermission(warpPermission, "bridginganalyzer.menu.warp");
        itemPermission = normalizePermission(itemPermission, "bridginganalyzer.menu.item");
        warpTitle = warpTitle == null || warpTitle.isBlank() ? "&a&l快捷传送" : warpTitle;
        warpEntries = validateWarpEntries(warpEntries);
    }

    static MenuSettings load(FileConfiguration config) {
        ConfigurationSection entries = config.getConfigurationSection("warp.entries");
        if (entries == null) {
            throw new IllegalArgumentException("menus.yml 缺少 warp.entries");
        }

        List<MenuEntry> parsed = new ArrayList<>();
        for (String id : entries.getKeys(false)) {
            ConfigurationSection entry = entries.getConfigurationSection(id);
            if (entry == null) {
                throw new IllegalArgumentException("warp.entries." + id + " 必须是配置节");
            }
            parsed.add(parseWarpEntry(id, entry));
        }
        return new MenuSettings(
                config.getString("permissions.main", "bridginganalyzer.menu.main"),
                config.getString("permissions.warp", "bridginganalyzer.menu.warp"),
                config.getString("permissions.item", "bridginganalyzer.menu.item"),
                config.getString("warp.title", "&a&l快捷传送"),
                parsed);
    }

    static List<MenuEntry> validateWarpEntries(List<MenuEntry> entries) {
        List<MenuEntry> copy = List.copyOf(entries);
        Set<String> ids = new HashSet<>();
        Set<Integer> slots = new HashSet<>(RESERVED_WARP_SLOTS);
        for (MenuEntry entry : copy) {
            if (entry.slot() >= WARP_SIZE) {
                throw new IllegalArgumentException("warp 菜单槽位越界: " + entry.slot());
            }
            if (!ids.add(entry.id())) {
                throw new IllegalArgumentException("warp 菜单项 id 重复: " + entry.id());
            }
            if (!slots.add(entry.slot())) {
                throw new IllegalArgumentException("warp 菜单槽位重复或占用保留位: " + entry.slot());
            }
        }
        return copy;
    }

    private static MenuEntry parseWarpEntry(String id, ConfigurationSection config) {
        int slot = config.getInt("slot", -1);
        Material material = Material.matchMaterial(config.getString("material", ""));
        if (material == null) {
            throw new IllegalArgumentException("warp.entries." + id + ".material 无效");
        }
        String name = config.getString("display-name", "");
        double cost = config.getDouble("cost", 0.0D);
        if (!Double.isFinite(cost) || cost < 0.0D) {
            throw new IllegalArgumentException("warp.entries." + id + ".cost 必须是非负有限数");
        }

        List<String> lore = config.getStringList("lore");
        if (lore.isEmpty()) {
            lore = defaultWarpLore(cost);
        }
        String permission = config.getString("permission", "");
        String type = config.getString("action.type", "").strip().toLowerCase(Locale.ROOT);
        String command = config.getString("action.command", "");
        MenuAction action = switch (type) {
            case "player-command" -> new MenuAction.PlayerCommand(command);
            case "console-command" -> new MenuAction.ConsoleCommand(command);
            default -> throw new IllegalArgumentException(
                    "warp.entries." + id + ".action.type 仅支持 player-command/console-command");
        };
        if (cost > 0.0D) {
            action = new MenuAction.Paid(cost, action,
                    config.getBoolean("close-on-deny", true));
        }
        return new MenuEntry(id, slot, material, name, lore, permission,
                MenuBinding.left(action, config.getBoolean("close-after", false)));
    }

    private static List<String> defaultWarpLore(double cost) {
        if (cost <= 0.0D) {
            return List.of("&8地标", "", "&e点击传送!");
        }
        return List.of("&8地标", "", "&f花费: &6{cost}硬币", "&e点击传送!");
    }

    private static String normalizePermission(String permission, String fallback) {
        return permission == null || permission.isBlank() ? fallback : permission.strip();
    }
}
