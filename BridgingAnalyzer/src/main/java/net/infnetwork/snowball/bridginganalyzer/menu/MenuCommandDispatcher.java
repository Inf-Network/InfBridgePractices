package net.infnetwork.snowball.bridginganalyzer.menu;

import org.bukkit.entity.Player;

/** Dispatches fixed, audited commands without coupling the menu to command implementations. */
public interface MenuCommandDispatcher {
    boolean player(Player player, String command);

    boolean console(String command);
}
