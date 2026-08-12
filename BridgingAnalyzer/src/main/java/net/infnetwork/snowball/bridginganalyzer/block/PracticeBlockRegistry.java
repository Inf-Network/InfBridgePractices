package net.infnetwork.snowball.bridginganalyzer.block;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import net.infnetwork.snowball.bridginganalyzer.BridgingAnalyzer;
import net.infnetwork.snowball.bridginganalyzer.Counter;
import org.bukkit.Bukkit;
import org.bukkit.Effect;
import org.bukkit.GameMode;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;

/**
 * Authoritative ownership registry for temporary practice blocks.
 *
 * <p>The legacy plugin stored raw {@link Block} instances in each counter. That
 * allowed an old delayed cleanup to delete a newer creative/admin block placed at
 * the same coordinates. This registry assigns every successful non-creative
 * placement a generation token and only deletes the generation it captured.</p>
 */
public final class PracticeBlockRegistry {
    private final BridgingAnalyzer plugin;
    private final PlacementLedger<BlockKey, UUID, Material> ledger = new PlacementLedger<>();
    private final Set<PlacementLedger.Token<BlockKey, UUID>> pendingStateRefreshes =
            new HashSet<>();

    public PracticeBlockRegistry(BridgingAnalyzer plugin) {
        this.plugin = Objects.requireNonNull(plugin, "plugin");
    }

    /** Only creative placement is permanent. OP status is deliberately irrelevant. */
    public static boolean shouldTrack(GameMode gameMode) {
        return gameMode != GameMode.CREATIVE;
    }

    public PlacementLedger.Entry<BlockKey, UUID, Material> track(UUID ownerId, Block block) {
        Objects.requireNonNull(ownerId, "ownerId");
        Objects.requireNonNull(block, "block");
        clearPendingForKey(BlockKey.of(block));
        PlacementLedger.Token<BlockKey, UUID> token = ledger.track(
                BlockKey.of(block), ownerId, block.getType());
        BridgingAnalyzer.getPlacedBlocks().put(block, block.getState().getData());
        return ledger.current(token.key()).orElseThrow();
    }

    /** Invalidates any temporary generation at this location without changing the world. */
    public Optional<PlacementLedger.Entry<BlockKey, UUID, Material>> forget(Block block) {
        Objects.requireNonNull(block, "block");
        Optional<PlacementLedger.Entry<BlockKey, UUID, Material>> removed =
                ledger.forget(BlockKey.of(block));
        clearPendingForKey(BlockKey.of(block));
        if (removed.isPresent()) {
            BridgingAnalyzer.getPlacedBlocks().remove(block);
            Counter.scheduledBreakBlocks.remove(block);
        }
        return removed;
    }

    public boolean isTracked(Block block) {
        Optional<PlacementLedger.Entry<BlockKey, UUID, Material>> current =
                ledger.current(BlockKey.of(block));
        return current.isPresent()
                && (block.getType() == current.get().expectedState()
                        || pendingStateRefreshes.contains(current.get().token()));
    }

    public boolean isOwnedBy(UUID ownerId, Block block) {
        return ledger.current(BlockKey.of(block))
                .map(entry -> entry.owner().equals(ownerId))
                .orElse(false);
    }

    /** Captures the current generation for a later, token-checked operation. */
    public Optional<PlacementLedger.Entry<BlockKey, UUID, Material>> capture(Block block) {
        Objects.requireNonNull(block, "block");
        return ledger.current(BlockKey.of(block)).filter(entry ->
                block.getType() == entry.expectedState()
                        || pendingStateRefreshes.contains(entry.token()));
    }

    /**
     * Keeps the current generation attached to a block that is about to change
     * material through a cancellable world event (for example copper oxidation).
     *
     * <p>The live material must still match the generation's expected material.
     * This prevents an old survival generation from adopting a creative/admin
     * replacement. The ledger then performs a second generation-token check
     * before committing the transition.</p>
     *
     * @return the updated entry, or empty when the location is untracked, was
     *     replaced, or changed generation while the event was being handled
     */
    public Optional<PlacementLedger.Entry<BlockKey, UUID, Material>> acceptNaturalTransition(
            Block block, Material nextMaterial) {
        Objects.requireNonNull(block, "block");
        Objects.requireNonNull(nextMaterial, "nextMaterial");

        Optional<PlacementLedger.Entry<BlockKey, UUID, Material>> current =
                ledger.current(BlockKey.of(block));
        if (current.isEmpty() || block.getType() != current.get().expectedState()) {
            return Optional.empty();
        }
        Optional<PlacementLedger.Entry<BlockKey, UUID, Material>> updated =
                ledger.updateExpected(current.get().token(), nextMaterial);
        updated.ifPresent(entry -> pendingStateRefreshes.add(entry.token()));
        return updated;
    }

    /** Marks a player-driven mutation whose final Material is applied after its event. */
    public Optional<PlacementLedger.Entry<BlockKey, UUID, Material>> expectStateRefresh(
            PlacementLedger.Entry<BlockKey, UUID, Material> captured) {
        Objects.requireNonNull(captured, "captured");
        if (!ledger.isCurrent(captured.token())) {
            return Optional.empty();
        }
        pendingStateRefreshes.add(captured.token());
        return Optional.of(captured);
    }

    /**
     * Reconciles one captured generation with the material now present in the world.
     * Used one tick after player/natural mutations so a later event cancellation
     * cannot leave the ledger expecting a material that was never applied.
     */
    public Optional<PlacementLedger.Entry<BlockKey, UUID, Material>> refreshExpectedState(
            PlacementLedger.Entry<BlockKey, UUID, Material> captured) {
        Objects.requireNonNull(captured, "captured");
        try {
            if (!ledger.isCurrent(captured.token())) {
                return Optional.empty();
            }
            Block block = captured.key().resolve();
            if (block == null) {
                return Optional.empty();
            }
            if (block.getType() == Material.AIR) {
                retire(captured, block);
                return Optional.empty();
            }
            Optional<PlacementLedger.Entry<BlockKey, UUID, Material>> updated =
                    ledger.updateExpected(captured.token(), block.getType());
            updated.ifPresent(ignored ->
                    BridgingAnalyzer.getPlacedBlocks().put(block, block.getState().getData()));
            return updated;
        } finally {
            pendingStateRefreshes.remove(captured.token());
        }
    }

    static boolean shouldFreezeGravity(boolean gravityMaterial, boolean tracked) {
        return tracked && gravityMaterial;
    }

    public List<PlacementLedger.Entry<BlockKey, UUID, Material>> snapshot(UUID ownerId) {
        return ledger.snapshot(ownerId);
    }

    public List<PlacementLedger.Entry<BlockKey, UUID, Material>> snapshotAll() {
        return ledger.all();
    }

    /** Changes the current owner's bridge to the victory material without touching stale entries. */
    public List<PlacementLedger.Entry<BlockKey, UUID, Material>> prepareVictory(UUID ownerId) {
        List<PlacementLedger.Entry<BlockKey, UUID, Material>> prepared = new ArrayList<>();
        for (PlacementLedger.Entry<BlockKey, UUID, Material> entry : ledger.snapshot(ownerId)) {
            if (!ledger.isCurrent(entry.token())) {
                continue;
            }
            Block block = entry.key().resolve();
            if (block == null) {
                prepared.add(entry);
                continue;
            }
            if (block.getType() == Material.AIR) {
                retire(entry, block);
                continue;
            }
            if (block.getType() != entry.expectedState()
                    && !pendingStateRefreshes.contains(entry.token())) {
                // An administrator or another plugin replaced this location. It is
                // no longer the temporary block represented by this generation.
                retire(entry, block);
                continue;
            }
            try {
                block.setType(Material.SEA_LANTERN, false);
                Optional<PlacementLedger.Entry<BlockKey, UUID, Material>> updated =
                        ledger.updateExpected(entry.token(), Material.SEA_LANTERN);
                updated.ifPresent(value -> {
                    BridgingAnalyzer.getPlacedBlocks().put(block, block.getState().getData());
                    prepared.add(value);
                });
            } catch (RuntimeException exception) {
                plugin.getLogger().warning("无法显示通关方块 " + entry.key() + ": "
                        + exception.getMessage() + "；仍会继续清理");
                prepared.add(entry);
            }
        }
        return List.copyOf(prepared);
    }

    /**
     * Deletes one captured generation. Tracking is retired only after deletion
     * succeeds; failures remain current so a later tick can retry.
     */
    public DeleteResult delete(PlacementLedger.Entry<BlockKey, UUID, Material> entry) {
        Optional<PlacementLedger.Entry<BlockKey, UUID, Material>> current =
                ledger.current(entry.key());
        if (current.isEmpty() || !ledger.isCurrent(entry.token())) {
            removeScheduledMirror(entry.key());
            return DeleteResult.STALE;
        }
        // Natural transitions (for example copper oxidation) intentionally retain
        // the same generation while updating its expected material. Delayed cleanup
        // tasks therefore use the current entry, never the old snapshot they captured.
        PlacementLedger.Entry<BlockKey, UUID, Material> liveEntry = current.get();
        Block block = liveEntry.key().resolve();
        if (block == null) {
            return DeleteResult.RETRY;
        }
        if (block.getType() == Material.AIR) {
            retire(liveEntry, block);
            return DeleteResult.ALREADY_GONE;
        }
        if (block.getType() != liveEntry.expectedState()
                && !pendingStateRefreshes.contains(liveEntry.token())) {
            retire(liveEntry, block);
            return DeleteResult.REPLACED;
        }

        Material removedMaterial = block.getType();
        try {
            if (!block.getChunk().isLoaded() && !block.getChunk().load(false)) {
                return DeleteResult.RETRY;
            }
            block.setType(Material.AIR, false);
            if (block.getType() != Material.AIR) {
                return DeleteResult.RETRY;
            }
        } catch (RuntimeException exception) {
            plugin.getLogger().warning("清理练习方块 " + liveEntry.key() + " 失败，将重试: "
                    + exception.getMessage());
            return DeleteResult.RETRY;
        }

        retire(liveEntry, block);
        try {
            block.getWorld().playEffect(block.getLocation(), Effect.STEP_SOUND, removedMaterial);
        } catch (RuntimeException exception) {
            // Sound/particle failure must never turn a successful physical deletion
            // back into an untracked leftover.
            plugin.getLogger().fine("无法播放方块清理效果: " + exception.getMessage());
        }
        return DeleteResult.DELETED;
    }

    public int cleanupNow(UUID ownerId) {
        int retries = 0;
        for (PlacementLedger.Entry<BlockKey, UUID, Material> entry : ledger.snapshot(ownerId)) {
            if (delete(entry) == DeleteResult.RETRY) {
                retries++;
            }
        }
        return retries;
    }

    public int cleanupAllNow() {
        int retries = 0;
        for (PlacementLedger.Entry<BlockKey, UUID, Material> entry : ledger.all()) {
            if (delete(entry) == DeleteResult.RETRY) {
                retries++;
            }
        }
        return retries;
    }

    public int cleanupWorldNow(UUID worldId) {
        Objects.requireNonNull(worldId, "worldId");
        int retries = 0;
        for (PlacementLedger.Entry<BlockKey, UUID, Material> entry : ledger.all()) {
            if (entry.key().worldId().equals(worldId) && delete(entry) == DeleteResult.RETRY) {
                retries++;
            }
        }
        return retries;
    }

    public boolean hasEntries(UUID ownerId) {
        return !ledger.snapshot(ownerId).isEmpty();
    }

    public void clearTracking() {
        ledger.clear();
        pendingStateRefreshes.clear();
    }

    private void retire(PlacementLedger.Entry<BlockKey, UUID, Material> entry, Block block) {
        if (ledger.removeIfCurrent(entry.token()).isPresent()) {
            pendingStateRefreshes.remove(entry.token());
            BridgingAnalyzer.getPlacedBlocks().remove(block);
            Counter.scheduledBreakBlocks.remove(block);
        }
    }

    private void clearPendingForKey(BlockKey key) {
        pendingStateRefreshes.removeIf(token -> token.key().equals(key));
    }

    private void removeScheduledMirror(BlockKey key) {
        Block block = key.resolve();
        if (block != null) {
            Counter.scheduledBreakBlocks.remove(block);
        }
    }

    public enum DeleteResult {
        DELETED,
        ALREADY_GONE,
        REPLACED,
        STALE,
        RETRY
    }

    /** Stable location key; it never retains a World or Player wrapper. */
    public record BlockKey(UUID worldId, int x, int y, int z) {
        public BlockKey {
            Objects.requireNonNull(worldId, "worldId");
        }

        public static BlockKey of(Block block) {
            return new BlockKey(block.getWorld().getUID(), block.getX(), block.getY(), block.getZ());
        }

        public Block resolve() {
            World world = Bukkit.getWorld(worldId);
            return world == null ? null : world.getBlockAt(x, y, z);
        }

        @Override
        public String toString() {
            return worldId + "@" + x + "," + y + "," + z;
        }
    }
}
