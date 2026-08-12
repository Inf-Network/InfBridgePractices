package net.infnetwork.snowball.bridginganalyzer.menu;

import org.bukkit.entity.Player;

/** Economy boundary used by menu purchases; the menu module has no Vault dependency. */
public interface MenuEconomy {
    Payment withdraw(Player player, double amount);

    Refund refund(Player player, double amount);

    record Payment(boolean successful, double debitedAmount, String message) {
    }

    record Refund(boolean successful, String message) {
    }
}
