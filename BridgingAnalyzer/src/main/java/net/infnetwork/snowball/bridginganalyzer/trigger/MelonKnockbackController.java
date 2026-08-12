package net.infnetwork.snowball.bridginganalyzer.trigger;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.BlockFace;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.player.PlayerChangedWorldEvent;
import org.bukkit.event.player.PlayerGameModeChangeEvent;
import org.bukkit.event.player.PlayerKickEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.player.PlayerTeleportEvent;
import org.bukkit.scheduler.BukkitTask;
import org.bukkit.util.Vector;
import net.infnetwork.snowball.bridginganalyzer.BridgingAnalyzer;
import net.infnetwork.snowball.bridginganalyzer.recovery.PlayerRecoveryService;

/** Owns the complete lifecycle and debounce state of the melon knockback trigger. */
public final class MelonKnockbackController implements Listener {
    private static final long KNOCKBACK_DELAY_TICKS = 7L;
    private static final long REARM_LOCK_TICKS = KNOCKBACK_DELAY_TICKS + 20L;

    private final BridgingAnalyzer plugin;
    private final PlayerRecoveryService recoveryService;
    private final MelonTriggerGate gate = new MelonTriggerGate(REARM_LOCK_TICKS);
    private final Map<UUID, BukkitTask> pendingTasks = new HashMap<>();
    private final Map<UUID, Long> teleportTicks = new HashMap<>();

    public MelonKnockbackController(BridgingAnalyzer plugin, PlayerRecoveryService recoveryService) {
        this.plugin = plugin;
        this.recoveryService = recoveryService;
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onMove(PlayerMoveEvent event) {
        if (event instanceof PlayerTeleportEvent
                || event.getFrom().getBlock().equals(event.getTo().getBlock())) {
            return;
        }

        Player player = event.getPlayer();
        UUID playerId = player.getUniqueId();
        long currentTick = plugin.getServer().getCurrentTick();
        Long teleportTick = teleportTicks.get(playerId);
        if (teleportTick != null) {
            if (currentTick <= teleportTick) {
                return;
            }
            teleportTicks.remove(playerId);
        }
        boolean onMelon = player.getGameMode() == GameMode.SURVIVAL
                && event.getTo().getBlock().getRelative(BlockFace.DOWN).getType() == Material.MELON;
        long token = gate.update(playerId, onMelon, currentTick);
        if (token == MelonTriggerGate.NO_ACTIVATION) {
            return;
        }

        Vector velocity = createKnockback(player.getLocation());
        BukkitTask task = plugin.getServer().getScheduler().runTaskLater(plugin,
                () -> applyKnockback(playerId, token, velocity), KNOCKBACK_DELAY_TICKS);
        pendingTasks.put(playerId, task);
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onTeleport(PlayerTeleportEvent event) {
        teleportTicks.put(event.getPlayer().getUniqueId(), (long) plugin.getServer().getCurrentTick());
        clear(event.getPlayer());
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        forget(event.getPlayer());
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onKick(PlayerKickEvent event) {
        forget(event.getPlayer());
    }

    @EventHandler
    public void onDeath(PlayerDeathEvent event) {
        forget(event.getEntity());
    }

    @EventHandler
    public void onChangedWorld(PlayerChangedWorldEvent event) {
        clear(event.getPlayer());
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onGameModeChange(PlayerGameModeChangeEvent event) {
        // Preserve a teleport marker written earlier in the same nested event chain.
        clear(event.getPlayer());
    }

    public void shutdown() {
        for (BukkitTask task : pendingTasks.values()) {
            task.cancel();
        }
        pendingTasks.clear();
        teleportTicks.clear();
        gate.clear();
    }

    private void applyKnockback(UUID playerId, long token, Vector velocity) {
        pendingTasks.remove(playerId);
        if (!gate.isCurrent(playerId, token)) {
            return;
        }
        Player player = plugin.getServer().getPlayer(playerId);
        if (player == null
                || !player.isOnline()
                || !player.isValid()
                || player.isDead()
                || player.getGameMode() != GameMode.SURVIVAL
                || recoveryService.isRecovering(player)) {
            return;
        }
        // Do not clear noDamageTicks or call damage(0): those shared flags caused re-entry.
        player.setVelocity(velocity);
    }

    private void clear(Player player) {
        UUID playerId = player.getUniqueId();
        BukkitTask task = pendingTasks.remove(playerId);
        if (task != null) {
            task.cancel();
        }
        gate.remove(playerId);
    }

    private void forget(Player player) {
        clear(player);
        teleportTicks.remove(player.getUniqueId());
    }

    private static Vector createKnockback(Location location) {
        double angle = Math.toRadians(location.getYaw() + 90.0 + Math.random() * 30.0 - 15.0);
        Vector attackDirection = new Vector(Math.cos(angle), 0.0, Math.sin(angle)).normalize();
        return attackDirection.multiply(-1.25).setY(0.45);
    }
}
