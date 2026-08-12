package sakura.kooi.BridgingSkin.lottery;

import java.util.Objects;
import org.bukkit.block.Block;
import org.bukkit.command.PluginCommand;
import org.bukkit.event.HandlerList;
import org.bukkit.plugin.java.JavaPlugin;
import sakura.kooi.BridgingSkin.crate.CrateSelectionException;
import sakura.kooi.BridgingSkin.crate.LotteryCrateCommand;
import sakura.kooi.BridgingSkin.crate.LotteryCrateService;
import sakura.kooi.BridgingSkin.crate.SelectionResolver;
import sakura.kooi.BridgingSkin.crate.WorldEditSelectionResolver;

/** One-call integration facade for BridgingSkin's main class. */
public final class LotterySubsystem implements AutoCloseable {
    private final LotteryManager manager;
    private final LotteryCrateService crates;

    private LotterySubsystem(LotteryManager manager, LotteryCrateService crates) {
        this.manager = manager;
        this.crates = crates;
    }

    public static LotterySubsystem enable(JavaPlugin plugin, RewardAuthorizer authorizer) {
        plugin.saveDefaultConfig();
        if (plugin.getConfig().getStringList("lottery.prize-pool").isEmpty()) {
            // Materialize the curated list so operators can remove/reorder entries in config.yml.
            plugin.getConfig().set("lottery.prize-pool", FullBlockPrizeCatalog.defaultNames());
            plugin.saveConfig();
        }
        LotterySettings settings = LotterySettings.load(plugin.getConfig());
        PrizePool pool = new PrizePool(settings.configuredMaterials(), plugin.getLogger());
        SelectionResolver resolver = worldEditResolver(plugin);
        LotteryCrateService crates = new LotteryCrateService(plugin, resolver);
        LotteryManager manager = new LotteryManager(plugin, crates, pool, settings,
                new VaultEconomyGateway(plugin.getServer()), authorizer);
        plugin.getServer().getPluginManager().registerEvents(manager, plugin);

        LotteryCrateCommand crateCommand = new LotteryCrateCommand(crates);
        PluginCommand command = Objects.requireNonNull(plugin.getCommand("bskin-crate"),
                "plugin.yml 缺少 bskin-crate 命令");
        command.setExecutor(crateCommand);
        command.setTabCompleter(crateCommand);
        return new LotterySubsystem(manager, crates);
    }

    private static SelectionResolver worldEditResolver(JavaPlugin plugin) {
        boolean worldEditReady = plugin.getServer().getPluginManager().isPluginEnabled("WorldEdit")
                || plugin.getServer().getPluginManager().isPluginEnabled("FastAsyncWorldEdit");
        if (!worldEditReady) {
            return player -> {
                throw new CrateSelectionException("服务端没有加载 WorldEdit/FAWE，无法读取选区");
            };
        }
        return new WorldEditSelectionResolver();
    }

    public LotteryManager manager() {
        return manager;
    }

    public boolean isCrate(Block block) {
        return crates.matches(block);
    }

    @Override
    public void close() {
        manager.close();
        HandlerList.unregisterAll(manager);
    }
}
