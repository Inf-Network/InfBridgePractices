package net.infnetwork.snowball.bridgingskin;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.bukkit.command.CommandSender;

public final class NetworkMessages {
    static final String PREFIX = "&bI&en&cf &bNetwork &e>> ";

    private static final LegacyComponentSerializer LEGACY =
            LegacyComponentSerializer.legacyAmpersand();
    private static final Component PREFIX_COMPONENT = LEGACY.deserialize(PREFIX);

    private NetworkMessages() {
    }

    public static void send(CommandSender sender, String... messages) {
        for (String message : messages) {
            sender.sendMessage(PREFIX_COMPONENT.append(LEGACY.deserialize(message)));
        }
    }
}
