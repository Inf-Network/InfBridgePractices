package com.luanmenglei.lv.commands;

import java.util.Locale;

import com.luanmenglei.lv.BlockLv;
import com.luanmenglei.lv.core.PointManger;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class MainCommands implements CommandExecutor {
    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (args.length == 0) {
            sendHelp(sender);
            return true;
        }

        return switch (args[0].toLowerCase(Locale.ROOT)) {
            case "add" -> handleAdd(sender, args);
            case "clear" -> handleClear(sender, args);
            case "setrank" -> handleSetRank(sender, args);
            case "refresh" -> handleRefresh(sender, args);
            default -> {
                NetworkMessages.send(sender, "&c未知子命令：&f" + args[0]);
                sendHelp(sender);
                yield true;
            }
        };
    }

    private boolean handleAdd(CommandSender sender, String[] args) {
        if (!sender.hasPermission("blocklv.add")) {
            NetworkMessages.send(sender, "&c你没有权限增加玩家经验。");
            return true;
        }
        if (args.length != 3) {
            NetworkMessages.send(sender, "&c用法：&e/blocklv add <玩家> <经验>");
            return true;
        }

        long experience;
        try {
            experience = Long.parseLong(args[2]);
        } catch (NumberFormatException ignored) {
            NetworkMessages.send(sender, "&c经验值必须是整数：&f" + args[2]);
            return true;
        }

        Player player = Bukkit.getPlayerExact(args[1]);
        if (player == null || !player.isOnline()) {
            NetworkMessages.send(sender, "&c玩家不在线或不存在：&f" + args[1]);
            return true;
        }
        if (PointManger.players.get(player.getUniqueId()) == null) {
            NetworkMessages.send(sender, "&c玩家等级数据尚未加载，请稍后重试：&f" + player.getName());
            return true;
        }

        PointManger.addPx(experience, player.getUniqueId());
        NetworkMessages.send(sender, "&a已为玩家 &f" + player.getName()
                + " &a增加 &f" + experience + " &a点搭路经验。");
        return true;
    }

    private boolean handleClear(CommandSender sender, String[] args) {
        if (!sender.hasPermission("blocklv.clear")) {
            NetworkMessages.send(sender, "&c你没有权限清空玩家等级。");
            return true;
        }
        if (args.length != 2) {
            NetworkMessages.send(sender, "&c用法：&e/blocklv clear <玩家>");
            return true;
        }

        Player player = Bukkit.getPlayerExact(args[1]);
        if (player == null || !player.isOnline()) {
            NetworkMessages.send(sender, "&c玩家不在线或不存在：&f" + args[1]);
            return true;
        }
        PointManger points = PointManger.players.get(player.getUniqueId());
        if (points == null) {
            NetworkMessages.send(sender, "&c玩家等级数据尚未加载，请稍后重试：&f" + player.getName());
            return true;
        }

        points.lv = 0L;
        points.px = 0L;
        PointManger.refreshExp(player.getUniqueId());
        NetworkMessages.send(sender, "&a已清空玩家 &f" + player.getName() + " &a的搭路等级与经验。");
        return true;
    }

    private boolean handleSetRank(CommandSender sender, String[] args) {
        if (!sender.hasPermission("blocklv.setrank")) {
            NetworkMessages.send(sender, "&c\u4f60\u6ca1\u6709\u6743\u9650\u8bbe\u7f6e\u6392\u884c\u699c\u4f4d\u7f6e\u3002");
            return true;
        }
        if (args.length != 1) {
            NetworkMessages.send(sender, "&c用法：&e/blocklv setrank");
            return true;
        }
        if (!(sender instanceof Player player)) {
            NetworkMessages.send(sender, "&c只有玩家可以设置排行榜位置。");
            return true;
        }

        Location location = player.getLocation();
        BlockLv.getInstance().getConfig().set("rank.x", (int) location.getX());
        BlockLv.getInstance().getConfig().set("rank.y", (int) location.getY());
        BlockLv.getInstance().getConfig().set("rank.z", (int) location.getZ());
        BlockLv.getInstance().getConfig().set("rank.world", location.getWorld().getName());
        BlockLv.getInstance().saveConfig();
        NetworkMessages.send(sender, "&a排行榜位置已设置。");
        return true;
    }

    private boolean handleRefresh(CommandSender sender, String[] args) {
        if (!sender.hasPermission("blocklv.refresh")) {
            NetworkMessages.send(sender, "&c你没有权限刷新排行榜。");
            return true;
        }
        if (args.length != 1) {
            NetworkMessages.send(sender, "&c用法：&e/blocklv refresh");
            return true;
        }

        BlockLv.refreshRank();
        NetworkMessages.send(sender, "&a已请求刷新排行榜。");
        return true;
    }

    private void sendHelp(CommandSender sender) {
        NetworkMessages.send(sender, "&fBlockLv 命令帮助：");
        NetworkMessages.send(sender, "&e/blocklv add <玩家> <经验> &7- 增加玩家经验");
        NetworkMessages.send(sender, "&e/blocklv clear <玩家> &7- 清空玩家等级与经验");
        NetworkMessages.send(sender, "&e/blocklv setrank &7- 设置排行榜位置");
        NetworkMessages.send(sender, "&e/blocklv refresh &7- 刷新排行榜");
    }
}
