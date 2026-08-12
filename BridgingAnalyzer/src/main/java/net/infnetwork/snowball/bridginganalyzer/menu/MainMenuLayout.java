package net.infnetwork.snowball.bridginganalyzer.menu;

import java.util.List;
import org.bukkit.Material;

/** Audited replacement for the legacy DeluxeMenus main.yml. */
final class MainMenuLayout {
    static final int SIZE = 36;
    static final String TITLE = "&e&l搭路练习 &a&l可视化菜单";

    private MainMenuLayout() {
    }

    static List<MenuEntry> entries() {
        return List.of(
                profile(),
                new MenuEntry("pickaxe", 12, Material.GOLDEN_PICKAXE, "&6获取稿子",
                        List.of("&8道具", "&7被困住了?", "&7用稿子挖出一片天地", "&e", "&e点击获取稿子!"),
                        "", MenuBinding.both(new MenuAction.PlayerCommand("imstuck"), true)),
                new MenuEntry("warp", 13, Material.COMPASS, "&a快捷传送",
                        List.of("&8传送", "&7选择想要到达的位置直接传送", "&e", "&e点击打开!"),
                        "", MenuBinding.both(new MenuAction.Open(MenuAction.Screen.WARP), false)),
                new MenuEntry("clearblock", 14, Material.TNT, "&c清除所有方块",
                        List.of("&8道具", "&7清理所有方块", "&7推倒一切障碍", "&e",
                                "&f花费: &6500金币", "&e点击购买!"),
                        "", MenuBinding.both(new MenuAction.Paid(
                                500.0D, MenuAction.ClearAll.INSTANCE, true), true)),
                toggle("pvp", 20, Material.IRON_SWORD, "&a开/关PVP模式",
                        List.of("&8战斗", "&7战桥等区域可以开启", "&7PvP模式,与他人切磋", "&e", "&e点击切换!"),
                        "bridge pvp"),
                toggle("highlight", 21, Material.SUGAR, "&a开/关侧搭提示",
                        List.of("&8辅助", "&7在侧搭时给予你适当", "&7位置的提醒,更准确地", "&7放置方块", "&e", "&e点击切换!"),
                        "bridge highlight"),
                toggle("stand", 22, Material.REDSTONE, "&a开/关走搭提示",
                        List.of("&8辅助", "&7在走搭时给予你适当", "&7位置的提醒,更准确地", "&7放置方块", "&e", "&e点击切换!"),
                        "bridge stand"),
                toggle("speed", 23, Material.FEATHER, "&a开/关速度统计",
                        List.of("&8统计", "&7在放置方块时记录速度", "&7帮助你了解自己的情况", "&e", "&e点击切换!"),
                        "bridge speed"),
                new MenuEntry("lobby", 24, Material.BEACON, "&a返回大厅",
                        List.of("&8传送", "&7懒得输入指令了?", "&7点我帮你回到主大厅!", "&e", "&e点击传送!"),
                        "", MenuBinding.both(new MenuAction.Connect("lobby_1"), true)),
                new MenuEntry("close", 31, Material.BARRIER, "&c关闭菜单",
                        List.of(), "", MenuBinding.both(MenuAction.Close.INSTANCE, false))
        );
    }

    static MenuEntry profile() {
        return new MenuEntry("profile", 4, Material.PLAYER_HEAD, "&e&l个人信息",
                List.of("&c", "&7身份: {group}", "&7等级: {level}", "&7硬币: &6{balance}",
                        "&e", "&bI&en&cf &bNetwork &a&l搭路练习"),
                "", null);
    }

    private static MenuEntry toggle(String id, int slot, Material material, String name,
                                    List<String> lore, String command) {
        return new MenuEntry(id, slot, material, name, lore, "",
                MenuBinding.both(new MenuAction.PlayerCommand(command), true));
    }
}
