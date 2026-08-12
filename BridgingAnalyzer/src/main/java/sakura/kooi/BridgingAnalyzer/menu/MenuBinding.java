package sakura.kooi.BridgingAnalyzer.menu;

import java.util.Objects;
import java.util.Set;

record MenuBinding(MenuAction action, Set<MenuButton> buttons, boolean closeAfter) {
    MenuBinding {
        Objects.requireNonNull(action, "action");
        buttons = Set.copyOf(buttons);
        if (buttons.isEmpty()) {
            throw new IllegalArgumentException("菜单动作至少需要一种点击方式");
        }
    }

    static MenuBinding both(MenuAction action, boolean closeAfter) {
        return new MenuBinding(action, Set.of(MenuButton.LEFT, MenuButton.RIGHT), closeAfter);
    }

    static MenuBinding left(MenuAction action, boolean closeAfter) {
        return new MenuBinding(action, Set.of(MenuButton.LEFT), closeAfter);
    }

    boolean accepts(MenuButton button) {
        return buttons.contains(button);
    }
}
