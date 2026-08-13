package net.infnetwork.snowball.blocklv.commands;

import java.util.List;
import java.util.Locale;

import net.infnetwork.snowball.blocklv.BlockLv;
import net.infnetwork.snowball.blocklv.api.LevelComponents;
import net.infnetwork.snowball.blocklv.core.PointManger;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabExecutor;
import org.bukkit.entity.Player;

public class MainCommands implements TabExecutor {
    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (args.length == 0) {
            sendHelp(sender);
            return true;
        }

        return switch (args[0].toLowerCase(Locale.ROOT)) {
            case "add" -> handleAdd(sender, args);
            case "addlevel" -> handleAddLevel(sender, args);
            case "decrease" -> handleDecrease(sender, args);
            case "decreaselevel" -> handleDecreaseLevel(sender, args);
            case "clear" -> handleClear(sender, args);
            case "setrank" -> handleSetRank(sender, args);
            case "refresh" -> handleRefresh(sender, args);
            default -> {
                send(sender, Component.text("未知子命令：", NamedTextColor.RED)
                        .append(Component.text(args[0], NamedTextColor.WHITE)));
                sendHelp(sender);
                yield true;
            }
        };
    }

    private boolean handleAdd(CommandSender sender, String[] args) {
        if (!sender.hasPermission("blocklv.add")) {
            send(sender, Component.text("你没有权限增加玩家经验。", NamedTextColor.RED));
            return true;
        }
        if (args.length != 3) {
            sendUsage(sender, "/blocklv add <玩家> <经验>");
            return true;
        }

        long experience;
        try {
            experience = Long.parseLong(args[2]);
        } catch (NumberFormatException ignored) {
            send(sender, Component.text("增加经验必须是正整数：", NamedTextColor.RED)
                    .append(Component.text(args[2], NamedTextColor.WHITE)));
            return true;
        }
        if (experience <= 0L) {
            send(sender, Component.text("增加经验必须是大于 0 的整数。", NamedTextColor.RED));
            return true;
        }

        Player player = onlinePlayer(sender, args[1]);
        if (player == null) {
            return true;
        }
        PointManger points = PointManger.players.get(player.getUniqueId());
        if (points == null) {
            sendDataNotLoaded(sender, player);
            return true;
        }
        ExperienceAdjustment.Result adjustment =
                ExperienceAdjustment.add(points.lv, points.px, experience);
        if (adjustment.status() != ExperienceAdjustment.Status.APPLIED) {
            sendExperienceAdjustmentError(
                    sender, adjustment.status(), points.px, true);
            return true;
        }

        points.lv = adjustment.level();
        points.px = adjustment.experience();
        if (!BlockLv.getInstance().getDatabase().save(
                player.getUniqueId(), player.getName(), points)) {
            points.lv = adjustment.previousLevel();
            points.px = adjustment.previousExperience();
            PointManger.refreshExp(player.getUniqueId());
            send(sender, Component.text("经验保存失败，本次调整已撤销。", NamedTextColor.RED));
            return true;
        }

        PointManger.refreshExp(player.getUniqueId());
        if (adjustment.level() > adjustment.previousLevel()) {
            PointManger.upLevel(adjustment.level(), player);
        }
        send(sender, Component.text("已为玩家 ", NamedTextColor.GREEN)
                .append(Component.text(player.getName(), NamedTextColor.WHITE))
                .append(Component.text(" 增加 ", NamedTextColor.GREEN))
                .append(Component.text(experience, NamedTextColor.WHITE))
                .append(Component.text(" 点搭路经验。", NamedTextColor.GREEN)));
        return true;
    }

    private boolean handleDecrease(CommandSender sender, String[] args) {
        if (!sender.hasPermission("blocklv.decrease")) {
            send(sender, Component.text("你没有权限减少玩家经验。", NamedTextColor.RED));
            return true;
        }
        if (args.length != 3) {
            sendUsage(sender, "/blocklv decrease <玩家> <减少经验>");
            return true;
        }

        long amount;
        try {
            amount = Long.parseLong(args[2]);
        } catch (NumberFormatException ignored) {
            send(sender, Component.text("减少经验必须是正整数：", NamedTextColor.RED)
                    .append(Component.text(args[2], NamedTextColor.WHITE)));
            return true;
        }

        Player player = onlinePlayer(sender, args[1]);
        if (player == null) {
            return true;
        }
        PointManger points = PointManger.players.get(player.getUniqueId());
        if (points == null) {
            sendDataNotLoaded(sender, player);
            return true;
        }

        ExperienceAdjustment.Result adjustment =
                ExperienceAdjustment.subtract(points.lv, points.px, amount);
        if (adjustment.status() != ExperienceAdjustment.Status.APPLIED) {
            sendExperienceAdjustmentError(sender, adjustment.status(), points.px, false);
            return true;
        }

        points.lv = adjustment.level();
        points.px = adjustment.experience();
        if (!BlockLv.getInstance().getDatabase().save(
                player.getUniqueId(), player.getName(), points)) {
            points.lv = adjustment.previousLevel();
            points.px = adjustment.previousExperience();
            PointManger.refreshExp(player.getUniqueId());
            send(sender, Component.text("经验保存失败，本次调整已撤销。", NamedTextColor.RED));
            return true;
        }

        PointManger.refreshExp(player.getUniqueId());
        send(sender, Component.text("已将玩家 ", NamedTextColor.GREEN)
                .append(Component.text(player.getName(), NamedTextColor.WHITE))
                .append(Component.text(" 的搭路进度从 ", NamedTextColor.GREEN))
                .append(LevelComponents.level(adjustment.previousLevel()))
                .append(Component.text(" / ", NamedTextColor.GRAY))
                .append(Component.text(adjustment.previousExperience(), NamedTextColor.WHITE))
                .append(Component.text(" 经验减少至 ", NamedTextColor.GREEN))
                .append(LevelComponents.level(adjustment.level()))
                .append(Component.text(" / ", NamedTextColor.GRAY))
                .append(Component.text(adjustment.experience(), NamedTextColor.WHITE))
                .append(Component.text(" 经验。", NamedTextColor.GREEN)));
        if (!sender.equals(player)) {
            send(player, Component.text("你的搭路进度已由管理员减少至 ", NamedTextColor.GREEN)
                    .append(LevelComponents.level(adjustment.level()))
                    .append(Component.text(" / ", NamedTextColor.GRAY))
                    .append(Component.text(adjustment.experience(), NamedTextColor.WHITE))
                    .append(Component.text(" 经验。", NamedTextColor.GREEN)));
        }
        return true;
    }

    private boolean handleAddLevel(CommandSender sender, String[] args) {
        if (!sender.hasPermission("blocklv.addlevel")) {
            send(sender, Component.text("你没有权限增加玩家等级。", NamedTextColor.RED));
            return true;
        }
        if (args.length != 3) {
            sendUsage(sender, "/blocklv addlevel <玩家> <增加等级>");
            return true;
        }

        long delta;
        try {
            delta = Long.parseLong(args[2]);
        } catch (NumberFormatException ignored) {
            send(sender, Component.text("增加等级必须是正整数：", NamedTextColor.RED)
                    .append(Component.text(args[2], NamedTextColor.WHITE)));
            return true;
        }

        Player player = onlinePlayer(sender, args[1]);
        if (player == null) {
            return true;
        }
        PointManger points = PointManger.players.get(player.getUniqueId());
        if (points == null) {
            sendDataNotLoaded(sender, player);
            return true;
        }

        LevelAdjustment.Result adjustment = LevelAdjustment.add(points.lv, points.px, delta);
        if (adjustment.status() != LevelAdjustment.Status.APPLIED) {
            sendLevelAdjustmentError(sender, adjustment.status(), points.lv, true);
            return true;
        }

        points.lv = adjustment.level();
        points.px = adjustment.experience();
        if (!BlockLv.getInstance().getDatabase().save(
                player.getUniqueId(), player.getName(), points)) {
            points.lv = adjustment.previousLevel();
            points.px = adjustment.previousExperience();
            PointManger.refreshExp(player.getUniqueId());
            send(sender, Component.text("等级保存失败，本次调整已撤销。", NamedTextColor.RED));
            return true;
        }

        PointManger.refreshExp(player.getUniqueId());
        send(sender, Component.text("已将玩家 ", NamedTextColor.GREEN)
                .append(Component.text(player.getName(), NamedTextColor.WHITE))
                .append(Component.text(" 的搭路等级从 ", NamedTextColor.GREEN))
                .append(LevelComponents.level(adjustment.previousLevel()))
                .append(Component.text(" 增加至 ", NamedTextColor.GREEN))
                .append(LevelComponents.level(adjustment.level()))
                .append(Component.text(
                        "（增加 " + delta + " 级）。", NamedTextColor.GREEN)));
        if (!sender.equals(player)) {
            send(player, Component.text("你的搭路等级已由管理员增加至 ", NamedTextColor.GREEN)
                    .append(LevelComponents.level(adjustment.level()))
                    .append(Component.text("。", NamedTextColor.GREEN)));
        }
        return true;
    }

    private boolean handleDecreaseLevel(CommandSender sender, String[] args) {
        if (!sender.hasPermission("blocklv.decreaselevel")) {
            send(sender, Component.text("你没有权限减少玩家等级。", NamedTextColor.RED));
            return true;
        }
        if (args.length != 3) {
            sendUsage(sender, "/blocklv decreaselevel <玩家> <减少等级>");
            return true;
        }

        long delta;
        try {
            delta = Long.parseLong(args[2]);
        } catch (NumberFormatException ignored) {
            send(sender, Component.text("减少等级必须是正整数：", NamedTextColor.RED)
                    .append(Component.text(args[2], NamedTextColor.WHITE)));
            return true;
        }

        Player player = onlinePlayer(sender, args[1]);
        if (player == null) {
            return true;
        }
        PointManger points = PointManger.players.get(player.getUniqueId());
        if (points == null) {
            sendDataNotLoaded(sender, player);
            return true;
        }

        LevelAdjustment.Result adjustment =
                LevelAdjustment.subtract(points.lv, points.px, delta);
        if (adjustment.status() != LevelAdjustment.Status.APPLIED) {
            sendLevelAdjustmentError(sender, adjustment.status(), points.lv, false);
            return true;
        }

        points.lv = adjustment.level();
        points.px = adjustment.experience();
        if (!BlockLv.getInstance().getDatabase().save(
                player.getUniqueId(), player.getName(), points)) {
            points.lv = adjustment.previousLevel();
            points.px = adjustment.previousExperience();
            PointManger.refreshExp(player.getUniqueId());
            send(sender, Component.text("等级保存失败，本次调整已撤销。", NamedTextColor.RED));
            return true;
        }

        PointManger.refreshExp(player.getUniqueId());
        send(sender, Component.text("已将玩家 ", NamedTextColor.GREEN)
                .append(Component.text(player.getName(), NamedTextColor.WHITE))
                .append(Component.text(" 的搭路等级从 ", NamedTextColor.GREEN))
                .append(LevelComponents.level(adjustment.previousLevel()))
                .append(Component.text(" 减少至 ", NamedTextColor.GREEN))
                .append(LevelComponents.level(adjustment.level()))
                .append(Component.text(
                        "（请求减少 " + delta + " 级）。", NamedTextColor.GREEN)));
        if (!sender.equals(player)) {
            send(player, Component.text("你的搭路等级已由管理员减少至 ", NamedTextColor.GREEN)
                    .append(LevelComponents.level(adjustment.level()))
                    .append(Component.text("。", NamedTextColor.GREEN)));
        }
        return true;
    }

    private boolean handleClear(CommandSender sender, String[] args) {
        if (!sender.hasPermission("blocklv.clear")) {
            send(sender, Component.text("你没有权限清空玩家等级。", NamedTextColor.RED));
            return true;
        }
        if (args.length != 2) {
            sendUsage(sender, "/blocklv clear <玩家>");
            return true;
        }

        Player player = onlinePlayer(sender, args[1]);
        if (player == null) {
            return true;
        }
        PointManger points = PointManger.players.get(player.getUniqueId());
        if (points == null) {
            sendDataNotLoaded(sender, player);
            return true;
        }

        points.lv = 0L;
        points.px = 0L;
        PointManger.refreshExp(player.getUniqueId());
        send(sender, Component.text("已清空玩家 ", NamedTextColor.GREEN)
                .append(Component.text(player.getName(), NamedTextColor.WHITE))
                .append(Component.text(" 的搭路等级与经验。", NamedTextColor.GREEN)));
        return true;
    }

    private boolean handleSetRank(CommandSender sender, String[] args) {
        if (!sender.hasPermission("blocklv.setrank")) {
            send(sender, Component.text("你没有权限设置排行榜位置。", NamedTextColor.RED));
            return true;
        }
        if (args.length != 1) {
            sendUsage(sender, "/blocklv setrank");
            return true;
        }
        if (!(sender instanceof Player player)) {
            send(sender, Component.text("只有玩家可以设置排行榜位置。", NamedTextColor.RED));
            return true;
        }

        Location location = player.getLocation();
        BlockLv.getInstance().getConfig().set("rank.x", (int) location.getX());
        BlockLv.getInstance().getConfig().set("rank.y", (int) location.getY());
        BlockLv.getInstance().getConfig().set("rank.z", (int) location.getZ());
        BlockLv.getInstance().getConfig().set("rank.world", location.getWorld().getName());
        BlockLv.getInstance().saveConfig();
        send(sender, Component.text("排行榜位置已设置。", NamedTextColor.GREEN));
        return true;
    }

    private boolean handleRefresh(CommandSender sender, String[] args) {
        if (!sender.hasPermission("blocklv.refresh")) {
            send(sender, Component.text("你没有权限刷新排行榜。", NamedTextColor.RED));
            return true;
        }
        if (args.length != 1) {
            sendUsage(sender, "/blocklv refresh");
            return true;
        }

        BlockLv.refreshRank();
        send(sender, Component.text("已请求刷新排行榜。", NamedTextColor.GREEN));
        return true;
    }

    private void sendHelp(CommandSender sender) {
        send(sender, Component.text("BlockLv 命令帮助：", NamedTextColor.WHITE));
        sendHelpLineIfPermitted(
                sender, "blocklv.add", "/blocklv add <玩家> <经验>", "增加玩家经验");
        sendHelpLineIfPermitted(sender, "blocklv.addlevel",
                "/blocklv addlevel <玩家> <增加等级>", "增加玩家等级并立即保存");
        sendHelpLineIfPermitted(sender, "blocklv.decrease",
                "/blocklv decrease <玩家> <减少经验>", "扣除完整进度，可跨级回退");
        sendHelpLineIfPermitted(sender, "blocklv.decreaselevel",
                "/blocklv decreaselevel <玩家> <减少等级>", "减少玩家等级并立即保存");
        sendHelpLineIfPermitted(sender, "blocklv.clear",
                "/blocklv clear <玩家>", "清空玩家等级与经验");
        sendHelpLineIfPermitted(sender, "blocklv.setrank",
                "/blocklv setrank", "设置排行榜位置");
        sendHelpLineIfPermitted(sender, "blocklv.refresh",
                "/blocklv refresh", "刷新排行榜");
    }

    private static Player onlinePlayer(CommandSender sender, String name) {
        Player player = Bukkit.getPlayerExact(name);
        if (player == null || !player.isOnline()) {
            send(sender, Component.text("玩家不在线或不存在：", NamedTextColor.RED)
                    .append(Component.text(name, NamedTextColor.WHITE)));
            return null;
        }
        return player;
    }

    private static void sendDataNotLoaded(CommandSender sender, Player player) {
        send(sender, Component.text("玩家等级数据尚未加载，请稍后重试：", NamedTextColor.RED)
                .append(Component.text(player.getName(), NamedTextColor.WHITE)));
    }

    private static void sendLevelAdjustmentError(
            CommandSender sender,
            LevelAdjustment.Status status,
            long currentLevel,
            boolean adding) {
        Component message = switch (status) {
            case NON_POSITIVE_DELTA ->
                    Component.text(
                            (adding ? "增加" : "减少") + "等级必须是大于 0 的整数。",
                            NamedTextColor.RED);
            case INVALID_CURRENT_LEVEL -> Component.text("玩家当前等级数据无效：", NamedTextColor.RED)
                    .append(Component.text(currentLevel, NamedTextColor.WHITE));
            case INVALID_CURRENT_EXPERIENCE ->
                    Component.text("玩家当前经验数据无效，无法调整等级。", NamedTextColor.RED);
            case OVERFLOW -> Component.text("增加后的等级超出整数范围。", NamedTextColor.RED);
            case APPLIED -> throw new IllegalArgumentException("成功结果不能作为错误发送");
        };
        send(sender, message);
    }

    private static void sendExperienceAdjustmentError(
            CommandSender sender,
            ExperienceAdjustment.Status status,
            long currentExperience,
            boolean adding) {
        Component message = switch (status) {
            case NON_POSITIVE_AMOUNT ->
                    Component.text(
                            (adding ? "增加" : "减少") + "经验必须是大于 0 的整数。",
                            NamedTextColor.RED);
            case INVALID_CURRENT_LEVEL ->
                    Component.text(
                            "玩家当前等级数据无效，无法"
                                    + (adding ? "增加" : "减少") + "经验。",
                            NamedTextColor.RED);
            case INVALID_CURRENT_EXPERIENCE ->
                    Component.text("玩家当前经验数据无效：", NamedTextColor.RED)
                            .append(Component.text(currentExperience, NamedTextColor.WHITE));
            case OVERFLOW ->
                    Component.text("增加后的搭路进度超出可保存范围。", NamedTextColor.RED);
            case APPLIED -> throw new IllegalArgumentException("成功结果不能作为错误发送");
        };
        send(sender, message);
    }

    @Override
    public List<String> onTabComplete(
            CommandSender sender,
            Command command,
            String alias,
            String[] args) {
        List<String> onlineNames = Bukkit.getOnlinePlayers().stream()
                .filter(player -> !(sender instanceof Player viewer) || viewer.canSee(player))
                .map(Player::getName)
                .toList();
        return BlockLvTabCompletion.complete(
                args, sender::hasPermission, sender instanceof Player, onlineNames);
    }

    private static void sendUsage(CommandSender sender, String usage) {
        send(sender, Component.text("用法：", NamedTextColor.RED)
                .append(Component.text(usage, NamedTextColor.YELLOW)));
    }

    private static void sendHelpLine(CommandSender sender, String command, String description) {
        send(sender, Component.text(command, NamedTextColor.YELLOW)
                .append(Component.text(" - " + description, NamedTextColor.GRAY)));
    }

    private static void sendHelpLineIfPermitted(
            CommandSender sender,
            String permission,
            String command,
            String description) {
        if (sender.hasPermission(permission)) {
            sendHelpLine(sender, command, description);
        }
    }

    private static void send(CommandSender sender, Component message) {
        NetworkMessages.sendComponent(sender, message);
    }
}
