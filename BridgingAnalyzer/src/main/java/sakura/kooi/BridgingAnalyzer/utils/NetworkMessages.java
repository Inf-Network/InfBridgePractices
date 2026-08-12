package sakura.kooi.BridgingAnalyzer.utils;

import org.bukkit.ChatColor;
import org.bukkit.command.CommandSender;

/** Shared formatting for all command feedback in the Inf Network plugins. */
public final class NetworkMessages {
    public static final String PREFIX_PATTERN = "&bI&en&cf &bNetwork &e>> ";

    private NetworkMessages() {
    }

    public static String format(String message) {
        return ChatColor.translateAlternateColorCodes('&', PREFIX_PATTERN + message);
    }

    public static void send(CommandSender sender, String message) {
        sender.sendMessage(format(message));
    }

    public static void send(CommandSender sender, String... messages) {
        for (String message : messages) {
            send(sender, message);
        }
    }
}
