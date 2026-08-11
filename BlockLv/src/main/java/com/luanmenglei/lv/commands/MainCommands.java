/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  org.bukkit.Bukkit
 *  org.bukkit.Location
 *  org.bukkit.command.Command
 *  org.bukkit.command.CommandExecutor
 *  org.bukkit.command.CommandSender
 *  org.bukkit.entity.Player
 */
package com.luanmenglei.lv.commands;

import com.luanmenglei.lv.BlockLv;
import com.luanmenglei.lv.core.PointManger;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class MainCommands
implements CommandExecutor {
    public boolean onCommand(CommandSender sender, Command cmd, String lable, String[] args) {
        if (args.length == 3 && args[0].equals("add") && sender.hasPermission("blocklv.add")) {
            long px;
            try {
                px = Long.parseLong(args[2]);
            }
            catch (NumberFormatException ignored) {
                return true;
            }
            Player player = Bukkit.getPlayer((String)args[1]);
            PointManger.addPx(px, player.getUniqueId());
            sender.sendMessage("\u642d\u8def\u7b49\u7ea7 > \u5df2\u7ecf\u7ed9\u73a9\u5bb6" + args[1] + "\u589e\u52a0" + args[2] + "\u7684\u7ecf\u9a8c");
        } else if (args.length == 2 && args[0].equals("clear") && sender.hasPermission("blocklv.clear")) {
            Player player = Bukkit.getPlayer((String)args[1]);
            PointManger.players.get((Object)player.getUniqueId()).lv = 0L;
            PointManger.players.get((Object)player.getUniqueId()).px = 0L;
            PointManger.refreshExp(player.getUniqueId());
            sender.sendMessage("\u6e05\u7a7a\u5b8c\u6210");
        } else if (args.length == 1 && args[0].equals("setrank")) {
            if (sender instanceof Player) {
                Location loc = ((Player)sender).getLocation();
                BlockLv.getInstance().getConfig().set("rank.x", (Object)((int)loc.getX()));
                BlockLv.getInstance().getConfig().set("rank.y", (Object)((int)loc.getY()));
                BlockLv.getInstance().getConfig().set("rank.z", (Object)((int)loc.getZ()));
                BlockLv.getInstance().getConfig().set("rank.world", (Object)loc.getWorld().getName());
                BlockLv.getInstance().saveConfig();
                sender.sendMessage("success!");
            } else {
                sender.sendMessage("only player can use this command");
            }
        }
        if (args.length == 1 && args[0].equalsIgnoreCase("refresh") && sender.hasPermission("blocklv.refresh")) {
            BlockLv.refreshRank();
        }
        return true;
    }
}

