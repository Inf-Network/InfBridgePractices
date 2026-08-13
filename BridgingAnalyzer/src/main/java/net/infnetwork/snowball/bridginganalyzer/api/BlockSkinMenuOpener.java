package net.infnetwork.snowball.bridginganalyzer.api;

import org.bukkit.entity.Player;

@FunctionalInterface
public interface BlockSkinMenuOpener {
    boolean open(Player player);
}
