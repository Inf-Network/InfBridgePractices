package net.infnetwork.snowball.cpscounter;

import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

public class ActionBarUtils {

    private static final LegacyComponentSerializer LEGACY =
            LegacyComponentSerializer.legacySection();

    /** Sends an action-bar message using legacy {@code §} formatting; null renders empty. */
    public static void sendActionBar(Player player, String message) {
        player.sendActionBar(LEGACY.deserialize(message == null ? "" : message));
    }

    public static void sendActionBarToAllPlayers(String message) {
        for (Player p : Bukkit.getOnlinePlayers()) {
            sendActionBar(p, message);
        }
    }
}
