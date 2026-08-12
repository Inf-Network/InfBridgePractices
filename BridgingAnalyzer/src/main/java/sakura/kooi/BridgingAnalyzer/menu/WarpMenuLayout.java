package sakura.kooi.BridgingAnalyzer.menu;

import java.util.ArrayList;
import java.util.List;
import org.bukkit.Material;

/** 54-slot, configuration-driven replacement for the legacy warp.yml. */
final class WarpMenuLayout {
    static final int SIZE = 54;
    static final int RETURN_SLOT = 49;

    private WarpMenuLayout() {
    }

    static List<MenuEntry> entries(MenuSettings settings) {
        List<MenuEntry> entries = new ArrayList<>(settings.warpEntries().size() + 2);
        entries.add(MainMenuLayout.profile());
        entries.addAll(settings.warpEntries());
        entries.add(new MenuEntry("back", RETURN_SLOT, Material.FEATHER, "&e返回主菜单",
                List.of(), "", MenuBinding.left(
                        new MenuAction.Open(MenuAction.Screen.MAIN), false)));
        return List.copyOf(entries);
    }
}
