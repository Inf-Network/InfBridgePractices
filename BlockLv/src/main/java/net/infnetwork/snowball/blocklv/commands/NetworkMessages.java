package net.infnetwork.snowball.blocklv.commands;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.bukkit.command.CommandSender;

public final class NetworkMessages {
    static final String PREFIX = "&bI&en&cf &bNetwork &e>> ";
    private static final LegacyComponentSerializer AMPERSAND =
            LegacyComponentSerializer.legacyAmpersand();
    private static final LegacyComponentSerializer SECTION =
            LegacyComponentSerializer.legacySection();

    private NetworkMessages() {
    }

    public static void send(CommandSender sender, String message) {
        sender.sendMessage(component(message));
    }

    static String format(String message) {
        return SECTION.serialize(component(message));
    }

    private static Component component(String message) {
        return AMPERSAND.deserialize(PREFIX + (message == null ? "" : message));
    }
}
