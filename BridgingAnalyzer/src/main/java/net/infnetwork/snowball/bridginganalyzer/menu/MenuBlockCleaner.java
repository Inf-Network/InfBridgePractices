package net.infnetwork.snowball.bridginganalyzer.menu;

import org.bukkit.entity.Player;

/** Clears the server-wide player-placed practice blocks after payment commits. */
@FunctionalInterface
public interface MenuBlockCleaner {
    boolean clearAll(Player purchaser);
}
