package sakura.kooi.BridgingAnalyzer.recovery;

import org.bukkit.Location;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.player.PlayerRespawnEvent;

/** Routes all low-Y and real VOID paths into the same guarded recovery service. */
public final class VoidSafetyListener implements Listener {
    private final PlayerRecoveryService recoveryService;

    public VoidSafetyListener(PlayerRecoveryService recoveryService) {
        this.recoveryService = recoveryService;
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onMove(PlayerMoveEvent event) {
        if (recoveryService.isUnsafe(event.getPlayer())) {
            event.setCancelled(true);
            recoveryService.requestFailureRecovery(event.getPlayer());
        }
    }

    @EventHandler(priority = EventPriority.LOWEST)
    public void onVoidDamage(EntityDamageEvent event) {
        if (event.getCause() != EntityDamageEvent.DamageCause.VOID
                || !(event.getEntity() instanceof org.bukkit.entity.Player player)) {
            return;
        }
        event.setCancelled(true);
        recoveryService.requestFailureRecovery(player);
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onJoin(PlayerJoinEvent event) {
        if (recoveryService.isUnsafe(event.getPlayer())) {
            recoveryService.requestFailureRecovery(event.getPlayer());
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onRespawn(PlayerRespawnEvent event) {
        Location emergencyLocation = recoveryService.consumeEmergencyRespawnLocation(event.getPlayer());
        event.setRespawnLocation(emergencyLocation != null
                ? emergencyLocation
                : recoveryService.recoveryLocation(event.getPlayer()));
        if (emergencyLocation == null
                && event.getRespawnReason() == PlayerRespawnEvent.RespawnReason.DEATH) {
            recoveryService.scheduleRespawnLoadout(event.getPlayer());
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onDeath(PlayerDeathEvent event) {
        recoveryService.cancelPendingRecoveryForDeath(event.getEntity());
        // Keep keys and avoid duplicating the old kit as drops; the selected
        // checkpoint loadout is applied one tick after a normal death respawn.
        event.setKeepInventory(true);
        event.setKeepLevel(true);
        event.setShouldDropExperience(false);
        event.getDrops().clear();
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        recoveryService.forget(event.getPlayer());
    }
}
