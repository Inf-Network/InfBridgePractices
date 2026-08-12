package sakura.kooi.BridgingAnalyzer.menu;

import java.io.File;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.logging.Level;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.TextDecoration;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.bukkit.Material;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.SkullMeta;
import org.bukkit.plugin.java.JavaPlugin;
import sakura.kooi.BridgingAnalyzer.utils.NetworkMessages;

/** Native, holder-backed implementation of the audited /cd and /warpbridge menus. */
public final class MenuSubsystem implements Listener, AutoCloseable {
    private static final LegacyComponentSerializer LEGACY =
            LegacyComponentSerializer.legacyAmpersand();

    private final JavaPlugin plugin;
    private final MenuDependencies dependencies;
    private final MenuSettings settings;
    private final Set<UUID> pendingActions = new HashSet<>();
    private boolean closed;

    private MenuSubsystem(JavaPlugin plugin, MenuDependencies dependencies, MenuSettings settings) {
        this.plugin = plugin;
        this.dependencies = dependencies;
        this.settings = settings;
    }

    /** Copies menus.yml on first use, validates it and constructs the listener. */
    public static MenuSubsystem load(JavaPlugin plugin, MenuDependencies dependencies) {
        File file = new File(plugin.getDataFolder(), "menus.yml");
        if (!file.isFile()) {
            plugin.saveResource("menus.yml", false);
        }
        MenuSettings settings = MenuSettings.load(YamlConfiguration.loadConfiguration(file));
        return new MenuSubsystem(plugin, dependencies, settings);
    }

    public boolean openMain(Player player) {
        return open(player, MenuAction.Screen.MAIN);
    }

    public boolean openWarp(Player player) {
        return open(player, MenuAction.Screen.WARP);
    }

    public String itemPermission() {
        return settings.itemPermission();
    }

    private boolean open(Player player, MenuAction.Screen screen) {
        if (closed || !player.isOnline()) {
            return false;
        }
        String permission = screen == MenuAction.Screen.MAIN
                ? settings.mainPermission()
                : settings.warpPermission();
        if (!hasPermission(player, permission)) {
            send(player, "&c你没有权限打开此菜单");
            return false;
        }

        List<MenuEntry> entries = screen == MenuAction.Screen.MAIN
                ? MainMenuLayout.entries()
                : WarpMenuLayout.entries(settings);
        int size = screen == MenuAction.Screen.MAIN ? MainMenuLayout.SIZE : WarpMenuLayout.SIZE;
        String title = screen == MenuAction.Screen.MAIN ? MainMenuLayout.TITLE : settings.warpTitle();
        MenuInventoryHolder holder = new MenuInventoryHolder(
                screen, player.getUniqueId(), actionEntries(entries));
        Inventory inventory = plugin.getServer().createInventory(holder, size, component(title));
        holder.bind(inventory);

        MenuProfileProvider.ProfileSnapshot profile = readProfile(player);
        for (MenuEntry entry : entries) {
            inventory.setItem(entry.slot(), render(entry, player, profile));
        }
        player.openInventory(inventory);
        return true;
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = false)
    public void onInventoryClick(InventoryClickEvent event) {
        Inventory top = event.getView().getTopInventory();
        if (!(top.getHolder() instanceof MenuInventoryHolder holder)) {
            return;
        }

        // Cancel every interaction while a native menu is open: shift-clicks from the
        // player inventory, hotbar swaps, collect-to-cursor and drop clicks included.
        event.setCancelled(true);
        if (!(event.getWhoClicked() instanceof Player player)
                || !holder.owner().equals(player.getUniqueId())
                || !holder.isBoundTo(top)
                || event.getRawSlot() < 0
                || event.getRawSlot() >= top.getSize()) {
            return;
        }

        MenuButton button = button(event.getClick());
        if (button == null) {
            return;
        }
        MenuEntry entry = holder.action(event.getRawSlot(), button).orElse(null);
        if (entry == null || !pendingActions.add(holder.owner())) {
            return;
        }

        try {
            plugin.getServer().getScheduler().runTask(plugin,
                    () -> executeDeferred(holder.owner(), holder.sessionId(), holder.screen(), entry));
        } catch (RuntimeException exception) {
            pendingActions.remove(holder.owner());
            plugin.getLogger().log(Level.SEVERE, "无法排队执行菜单动作 " + entry.id(), exception);
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = false)
    public void onInventoryDrag(InventoryDragEvent event) {
        if (event.getView().getTopInventory().getHolder() instanceof MenuInventoryHolder) {
            event.setCancelled(true);
        }
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        pendingActions.remove(event.getPlayer().getUniqueId());
    }

    private void executeDeferred(UUID playerId, UUID sessionId,
                                 MenuAction.Screen screen, MenuEntry entry) {
        try {
            if (closed) {
                return;
            }
            Player player = plugin.getServer().getPlayer(playerId);
            if (player == null || !player.isOnline() || !isCurrent(player, sessionId)) {
                return;
            }
            String screenPermission = screen == MenuAction.Screen.MAIN
                    ? settings.mainPermission()
                    : settings.warpPermission();
            if (!hasPermission(player, screenPermission)
                    || !hasPermission(player, entry.permission())) {
                send(player, "&c你没有权限使用此功能");
                return;
            }

            boolean successful;
            try {
                successful = execute(player, entry.binding().action());
            } catch (RuntimeException exception) {
                successful = false;
                plugin.getLogger().log(Level.SEVERE,
                        "执行 " + player.getName() + " 的菜单动作 " + entry.id() + " 时出错", exception);
                send(player, "&c操作执行失败，请联系管理员");
            }
            if (!successful && !(entry.binding().action() instanceof MenuAction.Paid)) {
                send(player, "&c操作未能完成");
            }
            if (entry.binding().closeAfter() && player.isOnline()) {
                player.closeInventory();
            }
        } finally {
            pendingActions.remove(playerId);
        }
    }

    private boolean execute(Player player, MenuAction action) {
        if (action instanceof MenuAction.Open open) {
            return open.screen() == MenuAction.Screen.MAIN ? openMain(player) : openWarp(player);
        }
        if (action instanceof MenuAction.PlayerCommand command) {
            return dependencies.commands().player(player,
                    expandCommand(command.command(), player));
        }
        if (action instanceof MenuAction.ConsoleCommand command) {
            return dependencies.commands().console(expandCommand(command.command(), player));
        }
        if (action instanceof MenuAction.Paid paid) {
            return executePaid(player, paid);
        }
        if (action instanceof MenuAction.Connect connect) {
            send(player, "&7正在连接到 主大厅...");
            return dependencies.serverConnector().connect(player, connect.serverName());
        }
        if (action == MenuAction.ClearAll.INSTANCE) {
            send(player, "&7正在清理所有方块...");
            boolean cleared = dependencies.blockCleaner().clearAll(player);
            if (cleared) {
                send(player, "&a清理成功!");
            }
            return cleared;
        }
        if (action == MenuAction.Close.INSTANCE) {
            player.closeInventory();
            return true;
        }
        throw new IllegalStateException("未知菜单动作: " + action);
    }

    private boolean executePaid(Player player, MenuAction.Paid paid) {
        MenuEconomy.Payment payment;
        try {
            payment = dependencies.economy().withdraw(player, paid.cost());
        } catch (RuntimeException exception) {
            plugin.getLogger().log(Level.SEVERE, "菜单扣款异常: " + player.getName(), exception);
            send(player, "&c经济服务暂时不可用");
            closeOnDeny(player, paid);
            return false;
        }
        MenuPaymentPolicy.Decision decision = MenuPaymentPolicy.assess(paid.cost(), payment);
        if (decision.outcome() == MenuPaymentPolicy.Outcome.FAILED) {
            refundIfPositive(player, decision.refundAmount(), "扣款失败但产生了部分扣款");
            send(player, "&c你没有足够的硬币!");
            closeOnDeny(player, paid);
            return false;
        }
        if (decision.outcome() == MenuPaymentPolicy.Outcome.AMOUNT_MISMATCH) {
            refundIfPositive(player, decision.refundAmount(), "扣款金额异常");
            send(player, "&c扣款金额异常，本次操作已取消");
            closeOnDeny(player, paid);
            return false;
        }
        double debited = payment.debitedAmount();

        try {
            if (execute(player, paid.action())) {
                return true;
            }
        } catch (RuntimeException exception) {
            refund(player, debited, "付费菜单动作异常");
            throw exception;
        }
        refund(player, debited, "付费菜单动作失败");
        send(player, "&c操作失败，已尝试退款");
        return false;
    }

    private void closeOnDeny(Player player, MenuAction.Paid paid) {
        if (paid.closeOnDeny() && player.isOnline()) {
            player.closeInventory();
        }
    }

    private void refundIfPositive(Player player, double amount, String reason) {
        if (Double.isFinite(amount) && amount > 0.0D) {
            refund(player, amount, reason);
        }
    }

    private void refund(Player player, double amount, String reason) {
        try {
            MenuEconomy.Refund refund = dependencies.economy().refund(player, amount);
            if (refund == null || !refund.successful()) {
                plugin.getLogger().severe(reason + ": 给 " + player.getName() + " 退款 " + amount
                        + " 失败: " + (refund == null ? "无返回值" : safe(refund.message())));
                send(player, "&c自动退款失败，请立即联系管理员");
            }
        } catch (RuntimeException exception) {
            plugin.getLogger().log(Level.SEVERE,
                    reason + ": 给 " + player.getName() + " 退款 " + amount + " 时出错", exception);
            send(player, "&c自动退款失败，请立即联系管理员");
        }
    }

    private boolean isCurrent(Player player, UUID sessionId) {
        Inventory top = player.getOpenInventory().getTopInventory();
        return top.getHolder() instanceof MenuInventoryHolder current
                && current.owner().equals(player.getUniqueId())
                && current.sessionId().equals(sessionId)
                && current.isBoundTo(top);
    }

    private boolean hasPermission(Player player, String permission) {
        if (permission == null || permission.isBlank()) {
            return true;
        }
        try {
            return dependencies.permissions().has(player, permission);
        } catch (RuntimeException exception) {
            plugin.getLogger().log(Level.WARNING,
                    "检查 " + player.getName() + " 的菜单权限 " + permission + " 时出错", exception);
            return false;
        }
    }

    private MenuProfileProvider.ProfileSnapshot readProfile(Player player) {
        try {
            MenuProfileProvider.ProfileSnapshot profile = dependencies.profiles().profile(player);
            return profile == null ? MenuProfileProvider.ProfileSnapshot.unknown() : profile;
        } catch (RuntimeException exception) {
            plugin.getLogger().log(Level.WARNING, "读取 " + player.getName() + " 的菜单资料失败", exception);
            return MenuProfileProvider.ProfileSnapshot.unknown();
        }
    }

    private ItemStack render(MenuEntry entry, Player player,
                             MenuProfileProvider.ProfileSnapshot profile) {
        ItemStack stack = new ItemStack(entry.material());
        ItemMeta meta = stack.getItemMeta();
        if (meta == null) {
            throw new IllegalArgumentException("菜单项 " + entry.id() + " 的材质没有物品元数据");
        }
        if (meta instanceof SkullMeta skull && entry.material() == Material.PLAYER_HEAD) {
            skull.setOwningPlayer(player);
        }

        Map<String, String> placeholders = new HashMap<>();
        placeholders.put("{group}", profile.group());
        placeholders.put("{level}", profile.level());
        placeholders.put("{balance}", profile.balance());
        placeholders.put("{player}", player.getName());
        double cost = cost(entry.binding());
        placeholders.put("{cost}", formatMoney(cost));
        meta.displayName(component(expand(entry.displayName(), placeholders)));
        meta.lore(entry.lore().stream()
                .map(line -> component(expand(line, placeholders)))
                .toList());
        stack.setItemMeta(meta);
        return stack;
    }

    private static Map<Integer, MenuEntry> actionEntries(List<MenuEntry> entries) {
        Map<Integer, MenuEntry> actions = new HashMap<>();
        for (MenuEntry entry : entries) {
            if (entry.binding() != null) {
                MenuEntry previous = actions.put(entry.slot(), entry);
                if (previous != null) {
                    throw new IllegalArgumentException("菜单槽位重复: " + entry.slot());
                }
            }
        }
        return actions;
    }

    private static MenuButton button(ClickType click) {
        return switch (click) {
            case LEFT -> MenuButton.LEFT;
            case RIGHT -> MenuButton.RIGHT;
            default -> null;
        };
    }

    private static double cost(MenuBinding binding) {
        return binding != null && binding.action() instanceof MenuAction.Paid paid
                ? paid.cost()
                : 0.0D;
    }

    private static String expandCommand(String command, Player player) {
        return command.replace("{player}", player.getName());
    }

    private static String expand(String source, Map<String, String> placeholders) {
        String expanded = source;
        for (Map.Entry<String, String> placeholder : placeholders.entrySet()) {
            expanded = expanded.replace(placeholder.getKey(), placeholder.getValue());
        }
        return expanded;
    }

    private static String formatMoney(double amount) {
        return amount == Math.rint(amount) ? Long.toString((long) amount) : Double.toString(amount);
    }

    private static Component component(String legacy) {
        return LEGACY.deserialize(legacy.replace('§', '&'))
                .decoration(TextDecoration.ITALIC, false);
    }

    private static void send(Player player, String message) {
        NetworkMessages.send(player, message);
    }

    private static String safe(String message) {
        return message == null || message.isBlank() ? "未提供原因" : message;
    }

    @Override
    public void close() {
        closed = true;
        pendingActions.clear();
        for (Player player : plugin.getServer().getOnlinePlayers()) {
            if (player.getOpenInventory().getTopInventory().getHolder()
                    instanceof MenuInventoryHolder) {
                player.closeInventory();
            }
        }
    }
}
