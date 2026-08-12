package sakura.kooi.BridgingSkin.lottery;

import net.milkbowl.vault.economy.Economy;
import net.milkbowl.vault.economy.EconomyResponse;
import org.bukkit.Server;
import org.bukkit.entity.Player;
import org.bukkit.plugin.RegisteredServiceProvider;

/** Uses only Vault's standard Economy service; the concrete economy provider is replaceable. */
public final class VaultEconomyGateway implements EconomyGateway {
    private final Server server;

    public VaultEconomyGateway(Server server) {
        this.server = server;
    }

    @Override
    public Payment withdraw(Player player, double amount) {
        Economy economy = economy();
        if (economy == null) {
            return new Payment(false, 0.0D, "未找到 Vault 经济服务");
        }
        if (!economy.has(player, amount)) {
            return new Payment(false, 0.0D, "余额不足，需要 " + economy.format(amount));
        }
        EconomyResponse response = economy.withdrawPlayer(player, amount);
        return new Payment(response.transactionSuccess(), Math.max(0.0D, response.amount), response.errorMessage);
    }

    @Override
    public Refund refund(Player player, double amount) {
        if (amount <= 0.0D) {
            return new Refund(true, "");
        }
        Economy economy = economy();
        if (economy == null) {
            return new Refund(false, "退款时 Vault 经济服务不可用");
        }
        EconomyResponse response = economy.depositPlayer(player, amount);
        return new Refund(response.transactionSuccess(), response.errorMessage);
    }

    private Economy economy() {
        RegisteredServiceProvider<Economy> registration =
                server.getServicesManager().getRegistration(Economy.class);
        return registration == null ? null : registration.getProvider();
    }
}
