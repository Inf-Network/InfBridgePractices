package net.infnetwork.snowball.cpscounter;

import org.bukkit.ChatColor;
import org.bukkit.command.CommandSender;

/** Formats command replies with the shared Inf Network chat prefix. */
public final class NetworkMessages {
    private static final String PREFIX = colorize("&bI&en&cf &bNetwork &e>> ");

    private NetworkMessages() {
    }

    public static void send(CommandSender sender, String message) {
        sender.sendMessage(PREFIX + colorize(message));
    }

    static String colorize(String message) {
        return ChatColor.translateAlternateColorCodes('&', message == null ? "" : message);
    }
}
