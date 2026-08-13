package net.infnetwork.snowball.blocklv.commands;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.bukkit.command.CommandSender;

public final class NetworkMessages {
    private static final Component PREFIX = Component.empty()
            .append(Component.text("I", NamedTextColor.AQUA))
            .append(Component.text("n", NamedTextColor.YELLOW))
            .append(Component.text("f ", NamedTextColor.RED))
            .append(Component.text("Network ", NamedTextColor.AQUA))
            .append(Component.text(">> ", NamedTextColor.YELLOW));
    private static final LegacyComponentSerializer AMPERSAND =
            LegacyComponentSerializer.legacyAmpersand();

    private NetworkMessages() {
    }

    public static void send(CommandSender sender, String message) {
        sendComponent(sender, AMPERSAND.deserialize(message == null ? "" : message));
    }

    public static void sendComponent(CommandSender sender, Component message) {
        sender.sendMessage(compose(message));
    }

    static Component compose(Component message) {
        return PREFIX.append(message == null ? Component.empty() : message);
    }
}
