package sakura.kooi.BridgingSkin.lottery;

import org.bukkit.entity.Player;

/** Small boundary around Vault so lottery rules remain testable. */
public interface EconomyGateway {
    Payment withdraw(Player player, double amount);

    Refund refund(Player player, double amount);

    record Payment(boolean successful, double debitedAmount, String message) {
    }

    record Refund(boolean successful, String message) {
    }
}
