package net.infnetwork.snowball.bridginganalyzer.recovery;

import java.util.HashSet;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.event.player.PlayerTeleportEvent;
import org.bukkit.scheduler.BukkitTask;
import org.bukkit.util.Vector;
import net.infnetwork.snowball.bridginganalyzer.BridgingAnalyzer;
import net.infnetwork.snowball.bridginganalyzer.Counter;
import net.infnetwork.snowball.bridginganalyzer.utils.TitleUtils;

/**
 * Single, idempotent entry point for checkpoint recovery.
 *
 * <p>Teleporting the current Player happens before inventory/skin work. A broken
 * skin provider therefore cannot leave someone in the void.</p>
 */
public final class PlayerRecoveryService {
    private static final int MAX_TELEPORT_FAILURES = 3;

    private final BridgingAnalyzer plugin;
    private final VoidSafetyPolicy safetyPolicy;
    private final Set<UUID> queuedRecoveries = new HashSet<>();
    private final Map<UUID, BukkitTask> recoveryTasks = new HashMap<>();
    private final Map<UUID, BukkitTask> respawnLoadoutTasks = new HashMap<>();
    private final RecoveryFailureGate failureGate = new RecoveryFailureGate(MAX_TELEPORT_FAILURES);
    private final Map<UUID, Location> emergencyRespawns = new HashMap<>();
    private BukkitTask watchdogTask;

    public PlayerRecoveryService(BridgingAnalyzer plugin, VoidSafetyPolicy safetyPolicy) {
        this.plugin = plugin;
        this.safetyPolicy = safetyPolicy;
    }

    public void start() {
        watchdogTask = plugin.getServer().getScheduler().runTaskTimer(plugin, () -> {
            for (Player player : plugin.getServer().getOnlinePlayers()) {
                try {
                    boolean unsafe = isUnsafe(player);
                    if (unsafe && !player.isDead()) {
                        requestFailureRecovery(player);
                    } else if (!unsafe) {
                        failureGate.recovered(player.getUniqueId());
                    }
                } catch (RuntimeException ex) {
                    plugin.getLogger().severe("虚空安全巡检处理玩家 " + player.getName()
                            + " 时失败,下个 tick 会继续重试: " + ex.getMessage());
                }
            }
        }, 1L, 1L);
    }

    public void stop() {
        if (watchdogTask != null) {
            watchdogTask.cancel();
            watchdogTask = null;
        }
        for (BukkitTask task : recoveryTasks.values()) {
            task.cancel();
        }
        recoveryTasks.clear();
        for (BukkitTask task : respawnLoadoutTasks.values()) {
            task.cancel();
        }
        respawnLoadoutTasks.clear();
        queuedRecoveries.clear();
        failureGate.clear();
        emergencyRespawns.clear();
    }

    public boolean isUnsafe(Player player) {
        return safetyPolicy.isUnsafe(player.getLocation().getY());
    }

    public boolean isRecovering(Player player) {
        return queuedRecoveries.contains(player.getUniqueId());
    }

    public boolean isEmergencyRespawn(Player player) {
        return emergencyRespawns.containsKey(player.getUniqueId());
    }

    public Location consumeEmergencyRespawnLocation(Player player) {
        Location location = emergencyRespawns.remove(player.getUniqueId());
        return location == null ? null : location.clone();
    }

    public void forget(Player player) {
        UUID playerId = player.getUniqueId();
        BukkitTask task = recoveryTasks.remove(playerId);
        if (task != null) {
            task.cancel();
        }
        BukkitTask respawnTask = respawnLoadoutTasks.remove(playerId);
        if (respawnTask != null) {
            respawnTask.cancel();
        }
        queuedRecoveries.remove(playerId);
        failureGate.recovered(playerId);
        emergencyRespawns.remove(playerId);
    }

    /** A real death supersedes any fatal-damage/void teleport queued earlier in that tick. */
    public void cancelPendingRecoveryForDeath(Player player) {
        UUID playerId = player.getUniqueId();
        BukkitTask task = recoveryTasks.remove(playerId);
        if (task != null) {
            task.cancel();
        }
        BukkitTask respawnTask = respawnLoadoutTasks.remove(playerId);
        if (respawnTask != null) {
            respawnTask.cancel();
        }
        queuedRecoveries.remove(playerId);
        failureGate.recovered(playerId);
    }

    public Location recoveryLocation(Player player) {
        return safeTarget(BridgingAnalyzer.getCounter(player).getCheckPoint(), player.getWorld());
    }

    /** Restore inventory after Bukkit has finished applying the death respawn. */
    public void scheduleRespawnLoadout(Player player) {
        UUID playerId = player.getUniqueId();
        BukkitTask previousTask = respawnLoadoutTasks.remove(playerId);
        if (previousTask != null) {
            previousTask.cancel();
        }
        try {
            BukkitTask task = plugin.getServer().getScheduler().runTask(plugin, () -> {
                respawnLoadoutTasks.remove(playerId);
                Player currentPlayer = plugin.getServer().getPlayer(playerId);
                if (currentPlayer == null || !currentPlayer.isOnline() || currentPlayer.isDead()) {
                    return;
                }
                Counter counter = null;
                try {
                    counter = BridgingAnalyzer.getCounter(currentPlayer);
                } catch (RuntimeException ex) {
                    plugin.getLogger().warning("重生后无法读取 " + currentPlayer.getName()
                            + " 的检查点套装: " + ex.getMessage());
                }
                restorePreferredLoadout(currentPlayer, counter);
            });
            respawnLoadoutTasks.put(playerId, task);
        } catch (RuntimeException ex) {
            plugin.getLogger().severe("无法排队恢复 " + player.getName()
                    + " 的重生套装: " + ex.getMessage());
        }
    }

    /** Queue one recovery for move, damage and watchdog events that may occur in the same tick. */
    public boolean requestFailureRecovery(Player player) {
        UUID playerId = player.getUniqueId();
        if (!queuedRecoveries.add(playerId)) {
            return false;
        }

        try {
            // Resolve the Player again when the task runs. A reconnect must never make a
            // delayed rescue operate on the old CraftPlayer wrapper.
            BukkitTask task = plugin.getServer().getScheduler()
                    .runTask(plugin, () -> runQueuedRecovery(playerId));
            recoveryTasks.put(playerId, task);
        } catch (RuntimeException schedulingFailure) {
            queuedRecoveries.remove(playerId);
            plugin.getLogger().severe("无法排队救援玩家 " + player.getName()
                    + ",正在立即重试: " + schedulingFailure.getMessage());
            Player currentPlayer = plugin.getServer().getPlayer(playerId);
            if (currentPlayer != null && !currentPlayer.isDead()) {
                try {
                    if (recoverNow(currentPlayer)) {
                        failureGate.recovered(playerId);
                    } else if (plugin.isEnabled()) {
                        handleFailedRecovery(currentPlayer);
                    }
                } catch (RuntimeException immediateFailure) {
                    plugin.getLogger().severe("立即救援玩家 " + player.getName()
                            + " 也失败,安全巡检将继续重试: " + immediateFailure.getMessage());
                }
            }
            return false;
        }
        return true;
    }

    private void runQueuedRecovery(UUID playerId) {
        recoveryTasks.remove(playerId);
        try {
            Player player = plugin.getServer().getPlayer(playerId);
            if (player == null || !player.isOnline() || player.isDead()) {
                return;
            }
            if (recoverNow(player)) {
                failureGate.recovered(playerId);
                finishFailureRecovery(player);
            } else {
                handleFailedRecovery(player);
            }
        } catch (RuntimeException ex) {
            plugin.getLogger().severe("救援玩家 " + playerId + " 时发生异常,安全巡检将继续重试: "
                    + ex.getMessage());
        } finally {
            // Never schedule guard cleanup: scheduler shutdown is itself an exception path.
            queuedRecoveries.remove(playerId);
        }
    }

    private void handleFailedRecovery(Player player) {
        UUID playerId = player.getUniqueId();
        if (!failureGate.recordFailure(playerId)) {
            return;
        }

        Location spawn = safeSpawn(player.getWorld(), player.getLocation());
        try {
            player.setRespawnLocation(spawn, true);
        } catch (RuntimeException ex) {
            plugin.getLogger().warning("无法预设 " + player.getName()
                    + " 的紧急重生点: " + ex.getMessage());
        }

        emergencyRespawns.put(playerId, spawn.clone());
        try {
            plugin.getLogger().severe("玩家 " + player.getName() + " 连续 "
                    + MAX_TELEPORT_FAILURES + " 次传送失败,正在保留物品并强制安全重生");
            player.setHealth(0.0);
            plugin.getServer().getScheduler().runTask(plugin, () -> {
                Player currentPlayer = plugin.getServer().getPlayer(playerId);
                if (currentPlayer != null && currentPlayer.isDead()) {
                    currentPlayer.spigot().respawn();
                }
            });
        } catch (RuntimeException ex) {
            emergencyRespawns.remove(playerId);
            plugin.getLogger().severe("无法强制重生玩家 " + player.getName()
                    + ",安全巡检将继续重试: " + ex.getMessage());
        }
    }

    private void finishFailureRecovery(Player player) {
        Counter counter;
        try {
            counter = BridgingAnalyzer.getCounter(player);
        } catch (RuntimeException ex) {
            plugin.getLogger().warning("玩家已获救,但无法读取练习统计: " + ex.getMessage());
            return;
        }
        try {
            if (counter.isSpeedCountEnabled()) {
                TitleUtils.sendTitle(player, "", "§cMax - " + counter.getMaxBridgeSpeed()
                        + " block/s", 1, 40, 1);
            }
        } catch (RuntimeException ex) {
            plugin.getLogger().warning("玩家已获救,但无法显示速度统计: " + ex.getMessage());
        }
        try {
            counter.reset();
        } catch (RuntimeException ex) {
            plugin.getLogger().warning("玩家已获救,但无法重置练习方块: " + ex.getMessage());
        }
    }

    /** Immediate checkpoint teleport used by joins, commands and victory handling. */
    public boolean recoverNow(Player player) {
        if (!player.isOnline() || player.isDead()) {
            return false;
        }

        Counter counter = null;
        Location checkpoint;
        try {
            counter = BridgingAnalyzer.getCounter(player);
            checkpoint = safeTarget(counter.getCheckPoint(), player.getWorld());
        } catch (RuntimeException ex) {
            checkpoint = safeSpawn(player.getWorld(), player.getLocation());
            plugin.getLogger().warning("无法读取 " + player.getName()
                    + " 的检查点,改用世界出生点: " + ex.getMessage());
        }

        boolean teleported = tryTeleport(player, checkpoint) && isSafeAfterTeleport(player);
        if (!teleported) {
            Location fallback = safeSpawn(player.getWorld(), player.getLocation());
            plugin.getLogger().warning("检查点传送被取消,正在把 " + player.getName() + " 送回世界出生点");
            teleported = tryTeleport(player, fallback) && isSafeAfterTeleport(player);
        }
        if (!teleported) {
            plugin.getLogger().severe("无法救援玩家 " + player.getName() + ",两次传送均被取消");
            return false;
        }

        runSafely(player, "停止下落", () -> stabilize(player));
        runSafely(player, "恢复生存模式", () -> player.setGameMode(GameMode.SURVIVAL));
        runSafely(player, "恢复饱食度", () -> player.setFoodLevel(20));
        runSafely(player, "恢复生命值", () -> player.setHealth(Math.min(20.0, player.getMaxHealth())));
        runSafely(player, "设置救援保护", () -> player.setNoDamageTicks(Math.max(player.getNoDamageTicks(), 20)));

        Counter resolvedCounter = counter;
        restorePreferredLoadout(player, resolvedCounter);
        return true;
    }

    /** Restore exactly one loadout: checkpoint chest when present, otherwise the default kit. */
    public void restorePreferredLoadout(Player player, Counter counter) {
        try {
            PreferredLoadoutRestorer.restore(
                    () -> counter != null && counter.restoreCheckPointLoadout(player),
                    () -> runSafely(player, "恢复默认练习物品",
                            () -> BridgingAnalyzer.restorePracticeLoadout(player)),
                    ex -> plugin.getLogger().warning("玩家 " + player.getName()
                            + " 的检查点套装恢复失败,改用默认练习方块: " + ex.getMessage()));
        } finally {
            // The fixed entry is an inventory invariant, including empty checkpoint
            // chests and provider failures that leave the previous loadout untouched.
            BridgingAnalyzer.ensureMenuEntry(player);
        }
    }

    private boolean tryTeleport(Player player, Location target) {
        try {
            return player.teleport(target, PlayerTeleportEvent.TeleportCause.PLUGIN);
        } catch (RuntimeException ex) {
            plugin.getLogger().warning("传送 " + player.getName() + " 时发生异常: " + ex.getMessage());
            return false;
        }
    }

    private boolean isSafeAfterTeleport(Player player) {
        try {
            return !isUnsafe(player);
        } catch (RuntimeException ex) {
            plugin.getLogger().warning("无法验证 " + player.getName()
                    + " 的传送落点: " + ex.getMessage());
            return false;
        }
    }

    private void runSafely(Player player, String operation, Runnable action) {
        try {
            action.run();
        } catch (RuntimeException ex) {
            plugin.getLogger().warning("玩家 " + player.getName() + " 已获救,但无法"
                    + operation + ": " + ex.getMessage());
        }
    }

    private Location safeTarget(Location checkpoint, World fallbackWorld) {
        if (checkpoint == null || checkpoint.getWorld() == null
                || !Double.isFinite(checkpoint.getX())
                || !Double.isFinite(checkpoint.getY())
                || !Double.isFinite(checkpoint.getZ())
                || safetyPolicy.isUnsafe(checkpoint.getY())) {
            return safeSpawn(fallbackWorld, checkpoint);
        }
        return checkpoint.clone();
    }

    private Location safeSpawn(World fallbackWorld, Location orientation) {
        Location spawn = fallbackWorld.getSpawnLocation().clone().add(0.5, 1.0, 0.5);
        if (safetyPolicy.isUnsafe(spawn.getY())) {
            double surfaceY = fallbackWorld.getHighestBlockYAt(spawn.getBlockX(), spawn.getBlockZ()) + 1.0;
            spawn.setY(Math.max(1.0, surfaceY));
            if (safetyPolicy.isUnsafe(spawn.getY())) {
                spawn.setY(fallbackWorld.getMaxHeight() - 2.0);
            }
        }
        if (orientation != null) {
            if (Float.isFinite(orientation.getYaw())) {
                spawn.setYaw(orientation.getYaw());
            }
            if (Float.isFinite(orientation.getPitch())) {
                spawn.setPitch(orientation.getPitch());
            }
        }
        return spawn;
    }

    private static void stabilize(Player player) {
        player.setVelocity(new Vector(0.0, 0.0, 0.0));
        player.setFallDistance(0.0f);
    }
}
