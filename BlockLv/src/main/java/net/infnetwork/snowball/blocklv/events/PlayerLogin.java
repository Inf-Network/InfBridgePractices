package net.infnetwork.snowball.blocklv.events;

import java.util.UUID;

import net.infnetwork.snowball.blocklv.BlockLv;
import net.infnetwork.snowball.blocklv.core.PointManger;
import org.bukkit.Bukkit;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.entity.Player;

public class PlayerLogin
implements Listener {
    @EventHandler
    public void onJoin(PlayerJoinEvent e) {
        UUID playerUuid = e.getPlayer().getUniqueId();
        String playerName = e.getPlayer().getName();
        Bukkit.getScheduler().runTask(BlockLv.getInstance(), () -> {
            Player currentPlayer = Bukkit.getPlayer(playerUuid);
            if (currentPlayer == null || !currentPlayer.isOnline()) {
                return;
            }
            PointManger pm = BlockLv.getInstance().getDatabase().get(playerUuid, playerName);
            PointManger.players.put(playerUuid, pm);
        });
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        UUID playerUuid = event.getPlayer().getUniqueId();
        String playerName = event.getPlayer().getName();
        PointManger points = PointManger.players.remove(playerUuid);
        BlockLv.getInstance().getDatabase().set(playerUuid, playerName, points);
    }
}
