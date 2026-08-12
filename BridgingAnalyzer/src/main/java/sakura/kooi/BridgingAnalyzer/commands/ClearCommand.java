/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  org.bukkit.Bukkit
 *  org.bukkit.Material
 *  org.bukkit.OfflinePlayer
 *  org.bukkit.block.Block
 *  org.bukkit.command.Command
 *  org.bukkit.command.CommandExecutor
 *  org.bukkit.command.CommandSender
 *  org.bukkit.entity.Player
 */
package sakura.kooi.BridgingAnalyzer.commands;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.OfflinePlayer;
import org.bukkit.block.Block;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import sakura.kooi.BridgingAnalyzer.BridgingAnalyzer;
import sakura.kooi.BridgingAnalyzer.Counter;
import sakura.kooi.BridgingAnalyzer.utils.NetworkMessages;

public class ClearCommand
implements CommandExecutor {
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (sender.hasPermission("bridginganalyzer.clear")) {
            if (args.length == 0) {
                NetworkMessages.send(sender, "&c\u6b63\u5728\u6e05\u9664\u6240\u6709\u5df2\u653e\u7f6e\u65b9\u5757....");
                for (Counter c : BridgingAnalyzer.getCounterSessions()) {
                    c.instantBreakBlock();
                }
                for (Block b : Counter.scheduledBreakBlocks) {
                    b.setType(Material.AIR);
                }
                NetworkMessages.send(sender, "&a\u65b9\u5757\u6e05\u9664\u5b8c\u6bd5.");
            } else {
                String player = args[0];
                OfflinePlayer offp = Bukkit.getOfflinePlayer((String)player);
                if (offp == null) {
                    NetworkMessages.send(sender, "&c\u9519\u8bef: \u73a9\u5bb6 " + player + " \u4e0d\u5b58\u5728.");
                    return true;
                }
                if (!offp.isOnline()) {
                    NetworkMessages.send(sender, "&c\u9519\u8bef: \u73a9\u5bb6 " + offp.getName() + " \u4e0d\u5728\u7ebf.");
                    return true;
                }
                Player p = offp.getPlayer();
                BridgingAnalyzer.getCounter(p).instantBreakBlock();
                NetworkMessages.send(sender, "&a\u5df2\u6e05\u9664\u73a9\u5bb6 " + p.getName() + " \u653e\u7f6e\u7684\u65b9\u5757.");
            }
        } else {
            NetworkMessages.send(sender, "&c\u4f60\u6ca1\u6709\u6743\u9650\u6267\u884c\u6b64\u547d\u4ee4.");
        }
        return true;
    }
}
