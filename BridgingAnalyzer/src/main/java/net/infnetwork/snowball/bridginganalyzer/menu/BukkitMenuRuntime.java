package net.infnetwork.snowball.bridginganalyzer.menu;

import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.util.Locale;
import java.util.Objects;
import java.util.logging.Level;
import net.milkbowl.vault.economy.Economy;
import net.milkbowl.vault.economy.EconomyResponse;
import net.milkbowl.vault.permission.Permission;
import org.bukkit.Server;
import org.bukkit.entity.Player;
import org.bukkit.plugin.RegisteredServiceProvider;
import org.bukkit.plugin.java.JavaPlugin;

/** Bukkit, Vault and proxy adapters kept outside the inventory/menu state machine. */
public final class BukkitMenuRuntime implements AutoCloseable {
    private static final String BUNGEE_CHANNEL = "BungeeCord";

    private final JavaPlugin plugin;
    private final MenuDependencies dependencies;
    private boolean closed;

    public BukkitMenuRuntime(JavaPlugin plugin, MenuBlockCleaner blockCleaner) {
        this.plugin = Objects.requireNonNull(plugin, "plugin");
        Objects.requireNonNull(blockCleaner, "blockCleaner");

        VaultServices vault = new VaultServices(plugin.getServer());
        MenuCommandDispatcher commands = new MenuCommandDispatcher() {
            @Override
            public boolean player(Player player, String command) {
                return player.performCommand(normalizeCommand(command));
            }

            @Override
            public boolean console(String command) {
                return plugin.getServer().dispatchCommand(
                        plugin.getServer().getConsoleSender(), normalizeCommand(command));
            }
        };

        this.dependencies = new MenuDependencies(
                vault,
                vault,
                Player::hasPermission,
                commands,
                blockCleaner,
                this::connect);
        plugin.getServer().getMessenger().registerOutgoingPluginChannel(plugin, BUNGEE_CHANNEL);
    }

    public MenuDependencies dependencies() {
        return dependencies;
    }

    @Override
    public void close() {
        if (closed) {
            return;
        }
        closed = true;
        plugin.getServer().getMessenger().unregisterOutgoingPluginChannel(plugin, BUNGEE_CHANNEL);
    }

    private boolean connect(Player player, String serverName) {
        if (closed || !player.isOnline() || serverName == null
                || !serverName.matches("[A-Za-z0-9_.-]{1,64}")) {
            return false;
        }
        try (ByteArrayOutputStream bytes = new ByteArrayOutputStream();
             DataOutputStream output = new DataOutputStream(bytes)) {
            output.writeUTF("Connect");
            output.writeUTF(serverName);
            player.sendPluginMessage(plugin, BUNGEE_CHANNEL, bytes.toByteArray());
            return true;
        } catch (IOException | RuntimeException exception) {
            plugin.getLogger().log(Level.WARNING,
                    "无法把 " + player.getName() + " 连接到 " + serverName, exception);
            return false;
        }
    }

    private static String normalizeCommand(String command) {
        String normalized = Objects.requireNonNull(command, "command").trim();
        while (normalized.startsWith("/")) {
            normalized = normalized.substring(1);
        }
        if (normalized.isBlank() || normalized.indexOf('\n') >= 0 || normalized.indexOf('\r') >= 0) {
            throw new IllegalArgumentException("无效命令");
        }
        return normalized;
    }

    private static final class VaultServices implements MenuEconomy, MenuProfileProvider {
        private static final double PAYMENT_EPSILON = 0.000_001D;

        private final Server server;

        private VaultServices(Server server) {
            this.server = server;
        }

        @Override
        public Payment withdraw(Player player, double amount) {
            if (!Double.isFinite(amount) || amount < 0.0D) {
                return new Payment(false, 0.0D, "无效的扣款金额");
            }
            if (amount == 0.0D) {
                return new Payment(true, 0.0D, "");
            }
            Economy economy = economy();
            if (economy == null) {
                return new Payment(false, 0.0D, "Vault 经济服务不可用");
            }
            if (!economy.has(player, amount)) {
                return new Payment(false, 0.0D, "余额不足，需要 " + economy.format(amount));
            }
            EconomyResponse response = economy.withdrawPlayer(player, amount);
            double debited = Math.max(0.0D, response.amount);
            boolean exact = Math.abs(debited - amount) <= PAYMENT_EPSILON;
            boolean successful = response.transactionSuccess() && exact;
            String message = successful ? "" : response.errorMessage;
            if (response.transactionSuccess() && !exact) {
                message = "经济插件返回的实际扣款金额不正确";
            }
            return new Payment(successful, debited, message == null ? "" : message);
        }

        @Override
        public Refund refund(Player player, double amount) {
            if (amount <= 0.0D) {
                return new Refund(true, "");
            }
            Economy economy = economy();
            if (economy == null) {
                return new Refund(false, "Vault 经济服务不可用");
            }
            EconomyResponse response = economy.depositPlayer(player, amount);
            return new Refund(response.transactionSuccess(),
                    response.errorMessage == null ? "" : response.errorMessage);
        }

        @Override
        public ProfileSnapshot profile(Player player) {
            Economy economy = economy();
            Permission permission = permission();
            String group = permission == null ? "玩家" : permission.getPrimaryGroup(player);
            String balance = economy == null
                    ? "不可用"
                    : economy.format(economy.getBalance(player));
            return new ProfileSnapshot(group, bridgeLevel(player.getLevel()), balance);
        }

        private Economy economy() {
            RegisteredServiceProvider<Economy> registration =
                    server.getServicesManager().getRegistration(Economy.class);
            return registration == null ? null : registration.getProvider();
        }

        private Permission permission() {
            RegisteredServiceProvider<Permission> registration =
                    server.getServicesManager().getRegistration(Permission.class);
            return registration == null ? null : registration.getProvider();
        }

        private static String bridgeLevel(int level) {
            return String.format(Locale.ROOT, "[%d✫]", Math.max(0, level));
        }
    }
}
