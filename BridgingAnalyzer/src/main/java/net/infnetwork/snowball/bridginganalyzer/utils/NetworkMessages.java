package net.infnetwork.snowball.bridginganalyzer.utils;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.bukkit.command.CommandSender;

public final class NetworkMessages {
    public static final String PREFIX_PATTERN = "&bI&en&cf &bNetwork &e>> ";
    private static final LegacyComponentSerializer AMPERSAND =
            LegacyComponentSerializer.legacyAmpersand();
    private static final LegacyComponentSerializer SECTION =
            LegacyComponentSerializer.legacySection();

    private NetworkMessages() {
    }

    public static String format(String message) {
        return SECTION.serialize(component(message));
    }

    public static void send(CommandSender sender, String message) {
        sender.sendMessage(component(message));
    }

    public static void send(CommandSender sender, String... messages) {
        for (String message : messages) {
            send(sender, message);
        }
    }

    private static Component component(String message) {
        return AMPERSAND.deserialize(PREFIX_PATTERN + message);
    }
}
