package net.infnetwork.snowball.bridginganalyzer;

import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.event.player.PlayerToggleSneakEvent;
import org.bukkit.plugin.Plugin;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import net.infnetwork.snowball.bridginganalyzer.BridgingAnalyzer;
import net.infnetwork.snowball.bridginganalyzer.Counter;
import net.infnetwork.snowball.bridginganalyzer.block.PlacementLedger;
import net.infnetwork.snowball.bridginganalyzer.block.PracticeBlockRegistry;
import net.infnetwork.snowball.bridginganalyzer.utils.FireworkUtils;
import org.bukkit.Particle;
import net.infnetwork.snowball.bridginganalyzer.utils.ParticleRing;
import net.infnetwork.snowball.bridginganalyzer.utils.TeleportRingEffect;
import net.infnetwork.snowball.bridginganalyzer.utils.TitleUtils;

public class TriggerBlockListener
implements Listener {
    private static final int MAX_TRIGGER_CLEANUP_RETRIES = 5;

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void antiTriggerBlockCover(BlockPlaceEvent e) {
        if (e.getPlayer() != null) {
            if (e.getPlayer().getGameMode() == GameMode.CREATIVE) {
                return;
            }
            if (this.isTriggerBlock(e.getBlock().getRelative(BlockFace.DOWN)) || this.isTriggerBlock(e.getBlock().getRelative(BlockFace.DOWN, 2))) {
                PracticeBlockRegistry registry = BridgingAnalyzer.practiceBlocks();
                if (registry == null) {
                    return;
                }
                Block block = e.getBlock();
                java.util.UUID ownerId = e.getPlayer().getUniqueId();
                PlacementLedger.Entry<PracticeBlockRegistry.BlockKey, java.util.UUID, Material> placement =
                        registry.snapshot(ownerId).stream()
                                .filter(entry -> entry.key().equals(
                                        PracticeBlockRegistry.BlockKey.of(block)))
                                .findFirst()
                                .orElse(null);
                if (placement == null) {
                    // CounterListener is registered before this listener at MONITOR.
                    // If another extension changes that ordering, leaving the block for
                    // normal attempt cleanup is safer than scheduling a raw location delete.
                    return;
                }
                this.scheduleTriggerCleanup(registry, placement,
                        BridgingAnalyzer.getCounter(e.getPlayer()), 100L, 0);
            }
        }
    }

    private void scheduleTriggerCleanup(
            PracticeBlockRegistry registry,
            PlacementLedger.Entry<PracticeBlockRegistry.BlockKey, java.util.UUID, Material> placement,
            Counter counter,
            long delay,
            int retryCount) {
        BridgingAnalyzer plugin = BridgingAnalyzer.getInstance();
        if (plugin == null || !plugin.isEnabled() || BridgingAnalyzer.isShuttingDown()) {
            return;
        }
        try {
            Bukkit.getScheduler().runTaskLater((Plugin)plugin, () -> {
                // A reload creates a new registry. Never let a task holding an old token
                // mutate the new runtime, even though Bukkit normally cancels such tasks.
                if (BridgingAnalyzer.practiceBlocks() != registry) {
                    return;
                }
                PracticeBlockRegistry.DeleteResult result;
                try {
                    result = registry.delete(placement);
                } catch (RuntimeException exception) {
                    plugin.getLogger().warning("触发方块上方的练习方块清理失败，将重试: "
                            + exception.getMessage());
                    result = PracticeBlockRegistry.DeleteResult.RETRY;
                }
                if (result == PracticeBlockRegistry.DeleteResult.RETRY) {
                    if (retryCount < MAX_TRIGGER_CLEANUP_RETRIES) {
                        long retryDelay = Math.min(80L, 5L << retryCount);
                        this.scheduleTriggerCleanup(
                                registry, placement, counter, retryDelay, retryCount + 1);
                    }
                    return;
                }
                if (result != PracticeBlockRegistry.DeleteResult.STALE) {
                    // STALE can mean a newer generation now occupies the same location;
                    // removing by Block equality in that case would erase its local mirror.
                    Block block = placement.key().resolve();
                    if (block != null) {
                        counter.removeBlockRecordLocally(block);
                    }
                }
            }, delay);
        } catch (RuntimeException exception) {
            plugin.getLogger().warning("无法排队触发方块上方的练习方块清理: "
                    + exception.getMessage());
        }
    }

    private boolean isTriggerBlock(Block b) {
        if (b.getType() == Material.EMERALD_BLOCK) {
            return true;
        }
        if (b.getType() == Material.REDSTONE_BLOCK) {
            return true;
        }
        if (b.getType() == Material.LAPIS_BLOCK) {
            return true;
        }
        return b.getType() == Material.BEACON;
    }

    @EventHandler
    public void triggerCheckPointBlock(PlayerMoveEvent e) {
        if (e.getFrom().getBlock().equals(e.getTo().getBlock())) {
            return;
        }
        if (e.getPlayer().getNoDamageTicks() != 0) {
            return;
        }
        if (e.getPlayer().getGameMode() == GameMode.SPECTATOR) {
            return;
        }
        if (e.getTo().getBlock().getRelative(BlockFace.DOWN).getType() == Material.EMERALD_BLOCK) {
            e.getPlayer().setNoDamageTicks(40);
            Location spawnLoc = e.getTo().getBlock().getLocation().add(0.5, 1.0, 0.5);
            spawnLoc.setYaw(e.getPlayer().getLocation().getYaw());
            spawnLoc.setPitch(e.getPlayer().getLocation().getPitch());
            Counter c = BridgingAnalyzer.getCounter(e.getPlayer());
            c.setCheckPoint(spawnLoc, e.getPlayer());
            new ParticleRing(e.getTo().getBlock().getLocation().add(0.5, 1.5, 0.5), Particle.CLOUD, 1L){

                @Override
                public void onFinish() {
                }
            };
            TitleUtils.sendTitle(e.getPlayer(), "", "\u00a7a\u4f20\u9001\u70b9\u5df2\u8bbe\u7f6e", 5, 10, 5);
            e.getPlayer().getWorld().playSound(e.getTo(), Sound.ENTITY_EXPERIENCE_ORB_PICKUP, 1.0f, 1.0f);
        }
    }

    @EventHandler
    public void triggerEndPointBlock(final PlayerMoveEvent e) {
        if (e.getFrom().getBlock().equals(e.getTo().getBlock())) {
            return;
        }
        if (e.getPlayer().getNoDamageTicks() != 0) {
            return;
        }
        if (e.getPlayer().getGameMode() == GameMode.SPECTATOR) {
            return;
        }
        if (e.getTo().getBlock().getRelative(BlockFace.DOWN).getType() == Material.REDSTONE_BLOCK) {
            e.getPlayer().setNoDamageTicks(40);
            new ParticleRing(e.getTo().getBlock().getLocation().add(0.5, 0.1, 0.5), Particle.WITCH, 20L){

                @Override
                public void onFinish() {
                    FireworkUtils.shootFirework(e.getPlayer());
                }
            };
            BridgingAnalyzer.getCounter(e.getPlayer()).vectoryBreakBlock(e.getPlayer());
            TitleUtils.sendTitle(e.getPlayer(), "\u00a76\u00a7lVICTORY", "", 5, 20, 5);
            e.getPlayer().getWorld().playSound(e.getTo(), Sound.ENTITY_PLAYER_LEVELUP, 1.0f, 1.0f);
        }
    }

    @EventHandler
    public void triggerSpawnPointBlock(final PlayerMoveEvent e) {
        if (e.getFrom().getBlock().equals(e.getTo().getBlock())) {
            return;
        }
        if (e.getPlayer().getNoDamageTicks() != 0) {
            return;
        }
        if (e.getPlayer().getGameMode() == GameMode.SPECTATOR) {
            return;
        }
        if (e.getTo().getBlock().getRelative(BlockFace.DOWN).getType() == Material.LAPIS_BLOCK) {
            e.getPlayer().setNoDamageTicks(40);
            Counter c = BridgingAnalyzer.getCounter(e.getPlayer());
            c.setCheckPoint(Bukkit.getWorld((String)"world").getSpawnLocation().add(0.5, 1.0, 0.5), e.getPlayer());
            c.resetMax();
            new ParticleRing(e.getTo().getBlock().getLocation().add(0.5, 1.5, 0.5), Particle.FIREWORK, 35L){

                @Override
                public void onFinish() {
                    BridgingAnalyzer.teleportCheckPoint(e.getPlayer());
                    BridgingAnalyzer.clearEffect(e.getPlayer());
                    if (!e.getPlayer().isOp()) {
                        e.getPlayer().getInventory().setHelmet(null);
                        e.getPlayer().getInventory().setChestplate(null);
                        e.getPlayer().getInventory().setLeggings(null);
                        e.getPlayer().getInventory().setBoots(null);
                    }
                }
            };
            TitleUtils.sendTitle(e.getPlayer(), "", "\u00a7b\u6b63\u5728\u8fd4\u56de\u51fa\u751f\u70b9...", 5, 25, 5);
            e.getPlayer().getWorld().playSound(e.getTo(), Sound.ENTITY_EXPERIENCE_ORB_PICKUP, 1.0f, 1.0f);
        }
    }

    @EventHandler
    public void triggerSpeedPlate(PlayerMoveEvent e) {
        if (e.getFrom().getBlock().equals(e.getTo().getBlock())) {
            return;
        }
        if (e.getPlayer().getNoDamageTicks() != 0) {
            return;
        }
        if (e.getPlayer().getGameMode() == GameMode.SPECTATOR) {
            return;
        }
        if (e.getTo().getBlock().getType() == Material.LIGHT_WEIGHTED_PRESSURE_PLATE) {
            e.getPlayer().setNoDamageTicks(20);
            e.getPlayer().addPotionEffect(new PotionEffect(PotionEffectType.SPEED, 100, 2), true);
        }
    }

    @EventHandler
    public void triggerTeleportBlock(final PlayerMoveEvent e) {
        if (e.getFrom().getBlock().equals(e.getTo().getBlock())) {
            return;
        }
        if (e.getPlayer().getNoDamageTicks() != 0) {
            return;
        }
        if (e.getPlayer().getGameMode() == GameMode.SPECTATOR) {
            return;
        }
        if (e.getTo().getBlock().getRelative(BlockFace.DOWN).getType() == Material.BEACON) {
            e.getPlayer().setNoDamageTicks(20);
            Block to = e.getTo().getBlock();
            while (isPassThrough(to.getType()) && to.getY() < to.getWorld().getMaxHeight() - 1) {
                to = to.getRelative(BlockFace.UP);
            }
            if (to.getType() == Material.BEACON) {
                e.getPlayer().setNoDamageTicks(50);
                final Block teleportTarget = to;
                new TeleportRingEffect(e.getTo().getBlock().getLocation().add(0.5, 0.0, 0.5), teleportTarget.getLocation().add(0.5, 1.0, 0.5), 1L, 0, 40){

                    @Override
                    public void onFinish() {
                        Location loc = teleportTarget.getLocation().add(0.5, 1.5, 0.5);
                        loc.setYaw(e.getPlayer().getLocation().getYaw());
                        loc.setPitch(e.getPlayer().getLocation().getPitch());
                        e.getPlayer().teleport(loc);
                    }
                };
                e.getPlayer().getWorld().playSound(e.getTo(), Sound.ENTITY_ENDERMAN_TELEPORT, 1.0f, 1.0f);
            }
        }
    }

    @EventHandler
    public void triggerTeleportBlock(final PlayerToggleSneakEvent e) {
        if (e.isSneaking()) {
            return;
        }
        if (e.getPlayer().getNoDamageTicks() != 0) {
            return;
        }
        if (e.getPlayer().getGameMode() == GameMode.SPECTATOR) {
            return;
        }
        if (e.getPlayer().getLocation().getBlock().getRelative(BlockFace.DOWN).getType() == Material.BEACON) {
            e.getPlayer().setNoDamageTicks(20);
            Block to = e.getPlayer().getLocation().getBlock().getRelative(BlockFace.DOWN, 2);
            while (isPassThrough(to.getType()) && to.getY() > to.getWorld().getMinHeight()) {
                to = to.getRelative(BlockFace.DOWN);
            }
            if (to.getType() == Material.BEACON) {
                e.getPlayer().setNoDamageTicks(50);
                final Block teleportTarget = to;
                new TeleportRingEffect(e.getPlayer().getLocation().getBlock().getLocation().add(0.5, 0.0, 0.5), teleportTarget.getLocation().add(0.5, 1.0, 0.5), 1L, 10, 40){

                    @Override
                    public void onFinish() {
                        Location loc = teleportTarget.getLocation().add(0.5, 1.5, 0.5);
                        loc.setYaw(e.getPlayer().getLocation().getYaw());
                        loc.setPitch(e.getPlayer().getLocation().getPitch());
                        e.getPlayer().teleport(loc);
                    }
                };
                e.getPlayer().getWorld().playSound(e.getPlayer().getLocation(), Sound.ENTITY_ENDERMAN_TELEPORT, 1.0f, 1.0f);
            }
        }
    }

    private static boolean isPassThrough(org.bukkit.Material type) {
        if (type == org.bukkit.Material.AIR) {
            return true;
        }
        if (type.name().endsWith("GLASS_PANE")) {
            return true;
        }
        return org.bukkit.Tag.ALL_SIGNS.isTagged(type);
    }
}

