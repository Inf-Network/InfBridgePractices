/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  org.bukkit.command.CommandSender
 */
package sakura.kooi.BridgingAnalyzer.utils;

import org.bukkit.command.CommandSender;

public class SendMessageUtils {
    public static void sendMessage(CommandSender sender, String ... message) {
        sender.sendMessage(message);
    }
}

