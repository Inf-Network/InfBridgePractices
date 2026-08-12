package net.infnetwork.snowball.bridginganalyzer.commands;

import org.bukkit.Bukkit;
import org.bukkit.World;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import net.infnetwork.snowball.bridginganalyzer.BridgingAnalyzer;
import net.infnetwork.snowball.bridginganalyzer.utils.NetworkMessages;

public class SaveWorldCommand
implements CommandExecutor {
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (sender.hasPermission("bridginganalyzer.admin")) {
            NetworkMessages.send(sender, "&c\u6b63\u5728\u4fdd\u5b58\u4e16\u754c....");
            BridgingAnalyzer.clearAllPracticeBlocks();
            for (World world : Bukkit.getWorlds()) {
                world.save();
            }
            NetworkMessages.send(sender, "&a\u5730\u56fe\u4fdd\u5b58\u5b8c\u6bd5.");
        } else {
            NetworkMessages.send(sender, "&c\u4f60\u6ca1\u6709\u6743\u9650\u6267\u884c\u6b64\u547d\u4ee4.");
        }
        return true;
    }
}
