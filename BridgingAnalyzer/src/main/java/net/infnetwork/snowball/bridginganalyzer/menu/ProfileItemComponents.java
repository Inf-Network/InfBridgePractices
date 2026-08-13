package net.infnetwork.snowball.bridginganalyzer.menu;

import java.util.List;
import net.infnetwork.snowball.blocklv.api.LevelComponents;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextColor;
import net.kyori.adventure.text.format.TextDecoration;

final class ProfileItemComponents {
    private ProfileItemComponents() {
    }

    static Component displayName() {
        return itemText(Component.text(
                "个人信息", NamedTextColor.YELLOW, TextDecoration.BOLD));
    }

    static List<Component> lore(MenuProfileProvider.ProfileSnapshot profile) {
        return List.of(
                itemText(Component.empty()),
                itemText(Component.text("身份: ", NamedTextColor.GRAY)
                        .append(Component.text(profile.group(), NamedTextColor.GRAY))),
                itemText(Component.text("等级: ", NamedTextColor.GRAY)
                        .append(level(profile.level()))),
                itemText(Component.text("硬币: ", NamedTextColor.GRAY)
                        .append(Component.text(profile.balance(), NamedTextColor.GOLD))),
                itemText(Component.empty()),
                itemText(Component.empty()
                        .append(Component.text("I", NamedTextColor.AQUA))
                        .append(Component.text("n", NamedTextColor.YELLOW))
                        .append(Component.text("f ", NamedTextColor.RED))
                        .append(Component.text("Network ", NamedTextColor.AQUA))
                        .append(Component.text(
                                "搭路练习", NamedTextColor.GREEN, TextDecoration.BOLD))));
    }

    private static Component level(String rawLevel) {
        try {
            return LevelComponents.badge(Long.parseLong(rawLevel));
        } catch (NumberFormatException invalidLevel) {
            return Component.text(rawLevel, TextColor.color(0xA0A0A0));
        }
    }

    private static Component itemText(Component component) {
        return component.decoration(TextDecoration.ITALIC, false);
    }
}
