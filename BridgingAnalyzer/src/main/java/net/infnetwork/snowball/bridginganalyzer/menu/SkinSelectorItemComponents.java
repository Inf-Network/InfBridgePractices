package net.infnetwork.snowball.bridginganalyzer.menu;

import java.util.List;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;

final class SkinSelectorItemComponents {
    private SkinSelectorItemComponents() {
    }

    static Component displayName() {
        return itemText(Component.text("方块皮肤选择器", NamedTextColor.GOLD));
    }

    static List<Component> lore() {
        return List.of(
                itemText(Component.text("外观", NamedTextColor.DARK_GRAY)),
                itemText(Component.text("查看并切换已解锁的", NamedTextColor.GRAY)),
                itemText(Component.text("搭路方块皮肤", NamedTextColor.GRAY)),
                itemText(Component.empty()),
                itemText(Component.text("点击打开!", NamedTextColor.YELLOW)));
    }

    private static Component itemText(Component component) {
        return component.decoration(TextDecoration.ITALIC, false);
    }
}
