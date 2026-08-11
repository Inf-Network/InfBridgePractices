/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  org.bukkit.Bukkit
 *  org.bukkit.World
 *  org.bukkit.block.Block
 *  org.bukkit.command.Command
 *  org.bukkit.command.CommandExecutor
 *  org.bukkit.command.CommandSender
 */
package sakura.kooi.BridgingAnalyzer.commands;

import org.bukkit.Bukkit;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import sakura.kooi.BridgingAnalyzer.BridgingAnalyzer;
import sakura.kooi.BridgingAnalyzer.Counter;
import sakura.kooi.BridgingAnalyzer.utils.Utils;

public class SaveWorldCommand
implements CommandExecutor {
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (sender.hasPermission("bridginganalyzer.admin")) {
            sender.sendMessage("\u00a7b\u00a7l\u642d\u8def\u7ec3\u4e60 \u00a77>> \u00a7c\u6b63\u5728\u4fdd\u5b58\u4e16\u754c....");
            for (Counter c : BridgingAnalyzer.getCounterSessions()) {
                c.instantBreakBlock();
            }
            for (Block b : Counter.scheduledBreakBlocks) {
                Utils.breakBlock(b);
            }
            for (World world : Bukkit.getWorlds()) {
                world.save();
            }
            sender.sendMessage("\u00a7b\u00a7l\u642d\u8def\u7ec3\u4e60 \u00a77>> \u00a7a\u5730\u56fe\u4fdd\u5b58\u5b8c\u6bd5.");
        }
        return true;
    }
}
