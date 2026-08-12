package com.luanmenglei.lv.commands;

import org.bukkit.ChatColor;
import org.bukkit.command.CommandSender;

/** Formats BlockLv command replies with the Inf Network prefix. */
public final class NetworkMessages {
    static final String PREFIX = "&bI&en&cf &bNetwork &e>> ";

    private NetworkMessages() {
    }

    public static void send(CommandSender sender, String message) {
        sender.sendMessage(format(message));
    }

    static String format(String message) {
        return ChatColor.translateAlternateColorCodes('&', PREFIX + message);
    }
}
