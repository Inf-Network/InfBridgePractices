/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  org.bukkit.Bukkit
 *  org.bukkit.command.Command
 *  org.bukkit.command.CommandExecutor
 *  org.bukkit.command.CommandSender
 *  org.bukkit.entity.ArmorStand
 *  org.bukkit.entity.Entity
 *  org.bukkit.entity.Player
 */
package sakura.kooi.BridgingAnalyzer.commands;

import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.ArmorStand;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import sakura.kooi.BridgingAnalyzer.BridgingAnalyzer;
import sakura.kooi.BridgingAnalyzer.Counter;
import sakura.kooi.BridgingAnalyzer.utils.NetworkMessages;
import sakura.kooi.BridgingAnalyzer.utils.TitleUtils;

public class BridgeCommand
implements CommandExecutor {
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player)) {
            NetworkMessages.send(sender, "&c\u6b64\u547d\u4ee4\u7528\u4e8e\u914d\u7f6e\u63d2\u4ef6\u53c2\u6570, \u4ec5\u73a9\u5bb6\u53ef\u4ee5\u6267\u884c.");
            return true;
        }
        if (args.length != 1) {
            NetworkMessages.send(sender,
                    "&b&lBridgingAnalyzer",
                    "&e/bridge highlight    &a\u542f\u7528/\u7981\u7528\u4fa7\u642d\u8f85\u52a9\u6307\u793a",
                    "&e/bridge pvp          &a\u542f\u7528/\u7981\u7528\u4f24\u5bb3\u5c4f\u853d",
                    "&e/bridge speed        &a\u542f\u7528/\u7981\u7528\u642d\u8def\u901f\u5ea6\u7edf\u8ba1",
                    "&e/bridge stand        &a\u542f\u7528/\u7981\u7528\u8d70\u642d\u4f4d\u7f6e\u6307\u793a",
                    "&e/bridge reset        &a\u91cd\u7f6e\u51fa\u751f\u70b9",
                    "&e/bridge remove       &a\u5220\u9664\u6700\u8fd1\u7684\u4e00\u4e2a\u9776\u5b50",
                    "&d\u6240\u914d\u7f6e\u7684\u53c2\u6570\u4ec5\u5bf9\u60a8\u6709\u6548, \u5176\u4ed6\u73a9\u5bb6\u4e0d\u53d7\u5f71\u54cd",
                    "&bhttps://github.com/SakuraKoi/BridgingAnalyzer");
            return true;
        }
        Counter counter = BridgingAnalyzer.getCounter((Player)sender);
        switch (args[0].toLowerCase()) {
            case "highlight": {
                counter.setHighlightEnabled(!counter.isHighlightEnabled());
                NetworkMessages.send(sender, "&a\u4fa7\u642d\u8f85\u52a9\u6307\u793a\u5df2" + (counter.isHighlightEnabled() ? "\u5f00\u542f" : "\u5173\u95ed"));
                break;
            }
            case "pvp": {
                counter.setPvPEnabled(!counter.isPvPEnabled());
                NetworkMessages.send(sender, "&aPvP\u5df2" + (counter.isPvPEnabled() ? "\u5f00\u542f" : "\u5173\u95ed"));
                break;
            }
            case "speed": {
                counter.setSpeedCountEnabled(!counter.isSpeedCountEnabled());
                NetworkMessages.send(sender, "&a\u642d\u8def\u901f\u5ea6\u7edf\u8ba1\u5df2" + (counter.isSpeedCountEnabled() ? "\u5f00\u542f" : "\u5173\u95ed"));
                break;
            }
            case "stand": {
                counter.setStandBridgeMarkerEnabled(!counter.isStandBridgeMarkerEnabled());
                NetworkMessages.send(sender, "&a\u8d70\u642d\u4f4d\u7f6e\u6307\u793a\u5df2" + (counter.isStandBridgeMarkerEnabled() ? "\u5f00\u542f" : "\u5173\u95ed"));
                break;
            }
            case "reset": {
                counter.setCheckPoint(Bukkit.getWorld((String)"world").getSpawnLocation().add(0.5, 1.0, 0.5), (Player)sender);
                NetworkMessages.send(sender, "&a\u51fa\u751f\u70b9\u5df2\u91cd\u7f6e.");
                break;
            }
            case "remove": {
                if (!sender.hasPermission("bridge.remove")) {
                    NetworkMessages.send(sender, "&c\u4f60\u6ca1\u6709\u6743\u9650\u4f7f\u7528\u6b64\u529f\u80fd.");
                    return true;
                }
                ArmorStand nearest = null;
                Player player = (Player)sender;
                for (Entity entity : player.getWorld().getEntities()) {
                    if (!(entity instanceof ArmorStand) || entity.getCustomName() == null || !entity.getCustomName().contains("VillagerSpawnPoint") || nearest != null && !(entity.getLocation().distance(player.getLocation()) < nearest.getLocation().distance(player.getLocation()))) continue;
                    nearest = (ArmorStand)entity;
                }
                if (nearest != null) {
                    nearest.remove();
                    TitleUtils.sendTitle(player, "", "\u00a7a\u6751\u6c11\u5237\u65b0\u70b9\u5df2\u79fb\u9664", 10, 20, 10);
                    NetworkMessages.send(sender, "&a\u6751\u6c11\u5237\u65b0\u70b9\u5df2\u79fb\u9664.");
                } else {
                    NetworkMessages.send(sender, "&c\u4f60\u7684\u9644\u8fd1\u6ca1\u6709\u9776\u5b50.");
                }
                break;
            }
            default: {
                NetworkMessages.send(sender, "&c\u5c1d\u8bd5\u5207\u6362\u7684\u529f\u80fd " + args[0] + " \u4e0d\u5b58\u5728.");
            }
        }
        return true;
    }
}
