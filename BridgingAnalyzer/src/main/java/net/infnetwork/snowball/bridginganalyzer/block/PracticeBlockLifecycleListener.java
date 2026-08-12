package net.infnetwork.snowball.bridginganalyzer.block;

import java.util.Objects;
import net.infnetwork.snowball.bridginganalyzer.BridgingAnalyzer;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.block.BlockBurnEvent;
import org.bukkit.event.block.BlockExplodeEvent;
import org.bukkit.event.block.BlockFadeEvent;
import org.bukkit.event.block.BlockFertilizeEvent;
import org.bukkit.event.block.BlockFormEvent;
import org.bukkit.event.block.BlockGrowEvent;
import org.bukkit.event.block.BlockPhysicsEvent;
import org.bukkit.event.block.BlockPistonExtendEvent;
import org.bukkit.event.block.BlockPistonRetractEvent;
import org.bukkit.event.block.BlockSpreadEvent;
import org.bukkit.event.block.CauldronLevelChangeEvent;
import org.bukkit.event.block.LeavesDecayEvent;
import org.bukkit.event.block.SpongeAbsorbEvent;
import org.bukkit.event.entity.EntityChangeBlockEvent;
import org.bukkit.event.entity.EntityExplodeEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.world.StructureGrowEvent;
import org.bukkit.event.world.WorldUnloadEvent;
import org.bukkit.plugin.Plugin;

public final class PracticeBlockLifecycleListener implements Listener {
    private final PracticeBlockRegistry registry;

    public PracticeBlockLifecycleListener(PracticeBlockRegistry registry) {
        this.registry = Objects.requireNonNull(registry, "registry");
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onBlockExplode(BlockExplodeEvent event) {
        event.blockList().forEach(BridgingAnalyzer::forgetPracticeBlock);
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onEntityExplode(EntityExplodeEvent event) {
        event.blockList().forEach(BridgingAnalyzer::forgetPracticeBlock);
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onBurn(BlockBurnEvent event) {
        BridgingAnalyzer.forgetPracticeBlock(event.getBlock());
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onFade(BlockFadeEvent event) {
        acceptTransition(event.getBlock(), event.getNewState().getType());
    }

    /*
     * Bukkit's form/grow/spread event classes each expose their own HandlerList,
     * so all three handlers are needed even though the Java classes inherit from
     * one another. Paper 1.21.11 routes copper weathering through BlockFormEvent.
     */
    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onForm(BlockFormEvent event) {
        acceptTransition(event.getBlock(), event.getNewState().getType());
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onGrow(BlockGrowEvent event) {
        acceptTransition(event.getBlock(), event.getNewState().getType());
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onSpread(BlockSpreadEvent event) {
        acceptTransition(event.getBlock(), event.getNewState().getType());
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onCauldronChange(CauldronLevelChangeEvent event) {
        acceptTransition(event.getBlock(), event.getNewState().getType());
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onSpongeAbsorb(SpongeAbsorbEvent event) {
        if (event.getBlock().getType() == Material.SPONGE) {
            acceptTransition(event.getBlock(), Material.WET_SPONGE);
        }
    }

    /** Multi-block growth is not allowed to turn one owned sapling into an unowned tree. */
    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onStructureGrow(StructureGrowEvent event) {
        if (registry.isTracked(event.getLocation().getBlock())
                || event.getBlocks().stream().anyMatch(state -> registry.isTracked(state.getBlock()))) {
            event.setCancelled(true);
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onFertilize(BlockFertilizeEvent event) {
        if (registry.isTracked(event.getBlock())
                || event.getBlocks().stream().anyMatch(state -> registry.isTracked(state.getBlock()))) {
            event.setCancelled(true);
        }
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onLeavesDecay(LeavesDecayEvent event) {
        BridgingAnalyzer.forgetPracticeBlock(event.getBlock());
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onPistonExtend(BlockPistonExtendEvent event) {
        if (containsTrackedBlock(event.getBlocks())) {
            event.setCancelled(true);
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onPistonRetract(BlockPistonRetractEvent event) {
        if (containsTrackedBlock(event.getBlocks())) {
            event.setCancelled(true);
        }
    }

    /**
     * Gravity blocks schedule their fall directly after placement, so cancelling
     * physics alone is insufficient. Cancelling the origin change prevents Paper
     * from replacing the registered block with air and spawning a falling entity
     * whose destination would no longer be registered.
     */
    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onTrackedEntityChange(EntityChangeBlockEvent event) {
        if (registry.isTracked(event.getBlock())) {
            event.setCancelled(true);
        }
    }

    /**
     * Axe stripping, shovel paths, farmland, waxing/scraping copper, carved
     * pumpkins and potted blocks all mutate Material through PlayerInteractEvent.
     * Capture the generation now and reconcile after vanilla applies the action.
     */
    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onPlayerBlockMutation(PlayerInteractEvent event) {
        if (event.getAction() != Action.RIGHT_CLICK_BLOCK || event.getClickedBlock() == null) {
            return;
        }
        registry.capture(event.getClickedBlock())
                .flatMap(registry::expectStateRefresh)
                .ifPresent(this::scheduleExpectedRefresh);
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onPhysics(BlockPhysicsEvent event) {
        Block block = event.getBlock();
        if (PracticeBlockRegistry.shouldFreezeGravity(
                block.getType().hasGravity(), registry.isTracked(block))) {
            event.setCancelled(true);
        }
    }

    /** Never let an unloaded world carry temporary blocks beyond the in-memory ledger. */
    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onWorldUnload(WorldUnloadEvent event) {
        int retries = registry.cleanupWorldNow(event.getWorld().getUID());
        if (retries > 0) {
            event.setCancelled(true);
            BridgingAnalyzer.getInstance().getLogger().warning(
                    "世界 " + event.getWorld().getName() + " 中仍有 " + retries
                            + " 个练习方块无法清理，已取消卸载");
        }
    }

    private boolean containsTrackedBlock(Iterable<Block> blocks) {
        for (Block block : blocks) {
            if (registry.isTracked(block)) {
                return true;
            }
        }
        return false;
    }

    private void acceptTransition(Block block, Material nextMaterial) {
        registry.acceptNaturalTransition(block, nextMaterial)
                .ifPresent(this::scheduleExpectedRefresh);
    }

    private void scheduleExpectedRefresh(
            PlacementLedger.Entry<PracticeBlockRegistry.BlockKey, java.util.UUID, Material> entry) {
        BridgingAnalyzer plugin = BridgingAnalyzer.getInstance();
        if (plugin == null || !plugin.isEnabled()) {
            return;
        }
        try {
            Bukkit.getScheduler().runTaskLater(
                    (Plugin) plugin, () -> registry.refreshExpectedState(entry), 1L);
        } catch (RuntimeException exception) {
            plugin.getLogger().warning("无法核对练习方块状态变化: " + exception.getMessage());
        }
    }
}
