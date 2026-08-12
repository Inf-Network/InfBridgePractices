package net.infnetwork.snowball.blocklv.events;

import net.infnetwork.snowball.blocklv.core.PointManger;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockPlaceEvent;

public class BlockPlace
implements Listener {
    @EventHandler
    public void onPlace(BlockPlaceEvent e) {
        if (e.isCancelled()) {
            return;
        }
        PointManger.addPx(1L, e.getPlayer().getUniqueId());
    }
}
