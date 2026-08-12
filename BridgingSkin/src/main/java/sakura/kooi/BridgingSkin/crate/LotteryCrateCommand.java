package sakura.kooi.BridgingSkin.crate;

import java.util.List;
import java.util.Locale;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import sakura.kooi.BridgingSkin.NetworkMessages;

public final class LotteryCrateCommand implements CommandExecutor, TabCompleter {
    private final LotteryCrateService crates;

    public LotteryCrateCommand(LotteryCrateService crates) {
        this.crates = crates;
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command,
            @NotNull String label, @NotNull String[] args) {
        if (!sender.hasPermission("bridgingskin.admin.crate")) {
            NetworkMessages.send(sender, "&c你没有管理抽奖箱的权限");
            return true;
        }
        String action = args.length == 0 ? "info" : args[0].toLowerCase(Locale.ROOT);
        switch (action) {
            case "set" -> set(sender);
            case "remove" -> removeSelected(sender);
            case "clear", "clearall" -> clearAll(sender);
            case "info" -> list(sender);
            case "list" -> list(sender);
            default -> NetworkMessages.send(sender,
                    "&e用法: /" + label + " <set|remove|clear|clearall|info|list>");
        }
        return true;
    }

    private void set(CommandSender sender) {
        if (!(sender instanceof Player player)) {
            NetworkMessages.send(sender, "&c该操作只能由游戏内玩家执行");
            return;
        }
        try {
            LotteryCrateService.Registration result = crates.registerSelectedWithStatus(player);
            if (result.added()) {
                NetworkMessages.send(sender,
                        "&a已将 " + result.crate().display() + " 注册为方块皮肤抽奖箱");
            } else {
                NetworkMessages.send(sender,
                        "&e" + result.crate().display() + " 已经是注册的方块皮肤抽奖箱，无需重复添加");
            }
        } catch (CrateSelectionException exception) {
            NetworkMessages.send(sender, "&c" + exception.getMessage());
        }
    }

    private void removeSelected(CommandSender sender) {
        if (!(sender instanceof Player player)) {
            NetworkMessages.send(sender, "&c该操作只能由游戏内玩家执行");
            return;
        }
        try {
            crates.removeSelected(player).ifPresentOrElse(
                    crate -> NetworkMessages.send(sender, "&a已移除抽奖箱 " + crate.display()),
                    () -> NetworkMessages.send(sender, "&e选中的方块不是已注册的抽奖箱"));
        } catch (CrateSelectionException exception) {
            NetworkMessages.send(sender, "&c" + exception.getMessage());
        }
    }

    private void clearAll(CommandSender sender) {
        int removed = crates.clearAll();
        if (removed == 0) {
            NetworkMessages.send(sender, "&e当前没有已注册的抽奖箱");
        } else {
            NetworkMessages.send(sender, "&a已清除全部 " + removed + " 个方块皮肤抽奖箱");
        }
    }

    private void list(CommandSender sender) {
        List<LotteryCrate> registered = crates.crates();
        if (registered.isEmpty()) {
            NetworkMessages.send(sender, "&e当前还没有注册抽奖箱");
            return;
        }
        NetworkMessages.send(sender, "&e已注册抽奖箱（共 &f" + registered.size() + "&e 个）:");
        for (int index = 0; index < registered.size(); index++) {
            NetworkMessages.send(sender, "&7" + (index + 1) + ". &f" + registered.get(index).display());
        }
    }

    @Override
    public @Nullable List<String> onTabComplete(@NotNull CommandSender sender,
            @NotNull Command command, @NotNull String alias, @NotNull String[] args) {
        if (args.length != 1 || !sender.hasPermission("bridgingskin.admin.crate")) {
            return List.of();
        }
        String prefix = args[0].toLowerCase(Locale.ROOT);
        return List.of("set", "remove", "clear", "clearall", "info", "list").stream()
                .filter(value -> value.startsWith(prefix)).toList();
    }
}
