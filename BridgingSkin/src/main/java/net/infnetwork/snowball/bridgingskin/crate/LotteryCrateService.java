package net.infnetwork.snowball.bridgingskin.crate;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

public final class LotteryCrateService {
    private final FileConfiguration config;
    private final Runnable saveConfig;
    private final SelectionResolver selectionResolver;
    private final List<LotteryCrate> crates;

    public LotteryCrateService(JavaPlugin plugin, SelectionResolver selectionResolver) {
        this(plugin.getConfig(), plugin::saveConfig, selectionResolver);
    }

    LotteryCrateService(FileConfiguration config, Runnable saveConfig,
            SelectionResolver selectionResolver) {
        this.config = Objects.requireNonNull(config, "config");
        this.saveConfig = Objects.requireNonNull(saveConfig, "saveConfig");
        this.selectionResolver = Objects.requireNonNull(selectionResolver, "selectionResolver");
        LotteryCrateConfig.LoadResult loaded = LotteryCrateConfig.load(config);
        this.crates = new ArrayList<>(loaded.crates());
        if (loaded.changed()) {
            persist();
        }
    }

    /**
     * Registers the selected crate and keeps the original return type for API compatibility.
     */
    public LotteryCrate registerSelected(Player player) throws CrateSelectionException {
        return registerSelectedWithStatus(player).crate();
    }

    public Registration registerSelectedWithStatus(Player player) throws CrateSelectionException {
        Block block = selectionResolver.resolveSingleBlock(player);
        if (block.getType() != Material.ENDER_CHEST) {
            throw new CrateSelectionException("选中的方块必须是末影箱，当前是 " + block.getType().name());
        }
        LotteryCrate selected = new LotteryCrate(
                block.getWorld().getName(), block.getX(), block.getY(), block.getZ());
        if (crates.contains(selected)) {
            return new Registration(selected, false);
        }
        crates.add(selected);
        persist();
        return new Registration(selected, true);
    }

    /**
     * Removes the registered crate at the player's current single-block selection.
     * The block does not have to still be an ender chest, so an externally changed crate can be unregistered.
     */
    public Optional<LotteryCrate> removeSelected(Player player) throws CrateSelectionException {
        Block block = selectionResolver.resolveSingleBlock(player);
        LotteryCrate selected = new LotteryCrate(
                block.getWorld().getName(), block.getX(), block.getY(), block.getZ());
        if (!crates.remove(selected)) {
            return Optional.empty();
        }
        persist();
        return Optional.of(selected);
    }

    /**
     * Retains the old all-clear behavior for callers compiled against the single-crate service.
     */
    public void clear() {
        clearAll();
    }

    public int clearAll() {
        int removed = crates.size();
        if (removed == 0) {
            return 0;
        }
        crates.clear();
        persist();
        return removed;
    }

    /**
     * Returns the first registered crate for compatibility with the old single-crate API.
     */
    public Optional<LotteryCrate> crate() {
        return crates.stream().findFirst();
    }

    public List<LotteryCrate> crates() {
        return List.copyOf(crates);
    }

    public boolean matches(Block block) {
        return block != null && crates.stream().anyMatch(crate -> crate.matches(block));
    }

    private void persist() {
        LotteryCrateConfig.write(config, crates);
        saveConfig.run();
    }

    public record Registration(LotteryCrate crate, boolean added) {
    }
}
