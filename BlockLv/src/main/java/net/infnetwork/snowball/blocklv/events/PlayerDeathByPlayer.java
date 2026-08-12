package net.infnetwork.snowball.blocklv.events;

import net.infnetwork.snowball.blocklv.BlockLv;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.scheduler.BukkitRunnable;

public class PlayerDeathByPlayer
implements Listener {
    @EventHandler
    public void onDeath(EntityDamageByEntityEvent e) {
        if (e.isCancelled()) {
            return;
        }
        if (e.getDamager() instanceof Player damager && e.getEntity() instanceof Player p) {
            BlockLv.getInstance().killPlayer.put(p, damager);
            new BukkitRunnable(){

                @Override
                public void run() {
                    BlockLv.getInstance().killPlayer.remove(p);
                }
            }.runTaskLater(BlockLv.getInstance(), 120L);
        }
    }
}
