package net.infnetwork.snowball.bridginganalyzer.utils;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import org.bukkit.Nameable;

/** Small bridge for comparisons against names stored as Adventure components. */
public final class ComponentText {
    private static final PlainTextComponentSerializer PLAIN_TEXT =
            PlainTextComponentSerializer.plainText();

    private ComponentText() {
    }

    public static String plain(Component component) {
        return component == null ? "" : PLAIN_TEXT.serialize(component);
    }

    public static String customName(Nameable nameable) {
        return plain(nameable.customName());
    }

    public static boolean customNameContains(Nameable nameable, String expected) {
        Component name = nameable.customName();
        return name != null && PLAIN_TEXT.serialize(name).contains(expected);
    }
}
