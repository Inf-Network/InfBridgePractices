package net.infnetwork.snowball.bridgingskin;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import java.io.File;
import java.io.FileReader;
import java.io.Reader;
import java.util.HashMap;
import java.util.List;
import java.util.Objects;
import java.util.UUID;
import java.util.logging.Level;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.command.CommandExecutor;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryMoveItemEvent;
import org.bukkit.event.player.PlayerDropItemEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerLoginEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.java.JavaPlugin;
import net.infnetwork.snowball.bridginganalyzer.api.BlockSkinProvider;
import net.infnetwork.snowball.bridginganalyzer.api.BridgingAnalyzerAPI;
import net.infnetwork.snowball.bridgingskin.data.PlayerSkin;
import net.infnetwork.snowball.bridgingskin.lottery.LotterySubsystem;
import net.infnetwork.snowball.bridgingskin.lottery.RewardAuthorizationException;
import net.infnetwork.snowball.bridgingskin.lottery.RewardAuthorizer;
import net.infnetwork.snowball.bridgingskin.storage.JdbcSkinRepository;
import net.infnetwork.snowball.bridgingskin.storage.LegacyJsonMigrator;
import net.infnetwork.snowball.bridgingskin.storage.SkinDatabaseFactory;

/**
 * Bootstrap and backwards-compatible public facade for BridgingSkin.
 *
 * <p>The authoritative cache and persistence key are UUID based. The public
 * name map and the old static methods remain because separately compiled
 * extensions used them in previous releases.</p>
 */
public final class BridgingSkin extends JavaPlugin implements Listener {
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();

    private static BridgingSkin instance;
    private static SkinService skinService;

    /** @deprecated read-only compatibility view; UUID storage is authoritative. */
    @Deprecated
    public static HashMap<String, PlayerSkin> skins;

    /** Retained as the immutable source directory for the one-time JSON import. */
    protected static File rootDir;

    private JdbcSkinRepository repository;
    private LotterySubsystem lotterySubsystem;
    private boolean providerInstalled;

    /**
     * @deprecated Use the UUID-aware service internally. Kept for binary API compatibility.
     */
    @Deprecated
    public static PlayerSkin getSkin(String player, String uuid) {
        return requireSkinService().getOrCreate(UUID.fromString(uuid), player);
    }

    /** @deprecated Name lookup is ambiguous after renames; kept for admin compatibility. */
    @Deprecated
    public static PlayerSkin getSkin(String player) {
        return requireSkinService().findByName(player);
    }

    /** @deprecated Mutations should go through SkinService; still performs a durable save. */
    @Deprecated
    public static void saveSkin(PlayerSkin skin) {
        if (skin != null) {
            requireSkinService().save(skin);
        }
    }

    /** Retained for migration tooling and old extensions; runtime writes never target JSON. */
    protected static PlayerSkin loadSkin(File file) {
        if (file == null || !file.isFile()) {
            return null;
        }
        try (FileReader reader = new FileReader(file)) {
            return GSON.fromJson((Reader) reader, PlayerSkin.class);
        } catch (Exception exception) {
            instance.getLogger().log(Level.WARNING,
                    "无法读取旧皮肤文件 " + file.getName(), exception);
            return null;
        }
    }

    @Override
    public void onEnable() {
        instance = this;
        skins = new HashMap<>();
        saveDefaultConfig();
        rootDir = new File(getDataFolder(), "skins");
        if (!rootDir.isDirectory() && !rootDir.mkdirs()) {
            getLogger().severe("无法创建旧皮肤数据目录 " + rootDir);
            getServer().getPluginManager().disablePlugin(this);
            return;
        }

        try {
            repository = SkinDatabaseFactory.open(this);
            int imported = new LegacyJsonMigrator(repository, GSON, getLogger())
                    .migrateIfNeeded(rootDir);
            if (imported > 0) {
                getLogger().info("旧 JSON 迁移验证完成: " + imported + " 个玩家文件");
            }
            skinService = new SkinService(repository, getLogger(), skins);

            Objects.requireNonNull(getCommand("bskin"), "plugin.yml 缺少 bskin")
                    .setExecutor((CommandExecutor) new SkinSelectCommand());
            Objects.requireNonNull(getCommand("bskin-edit"), "plugin.yml 缺少 bskin-edit")
                    .setExecutor((CommandExecutor) new SkinEditCommand());
            Bukkit.getPluginManager().registerEvents(this, this);
            Bukkit.getPluginManager().registerEvents(new SkinEditListener(), this);

            lotterySubsystem = LotterySubsystem.enable(this, lotteryAuthorizer());

            // Compatibility flush for external plugins that still mutate public PlayerSkin fields.
            Bukkit.getScheduler().runTaskTimer(this, this::saveData, 6000L, 6000L);
            for (Player player : Bukkit.getOnlinePlayers()) {
                skinService.getOrCreate(player);
            }
            BridgingAnalyzerAPI.setBlockSkinProvider((BlockSkinProvider) new SkinProvider());
            providerInstalled = true;
        } catch (RuntimeException exception) {
            getLogger().log(Level.SEVERE,
                    "BridgingSkin 启动失败；为防止覆盖旧皮肤数据，插件已停止", exception);
            getServer().getPluginManager().disablePlugin(this);
        }
    }

    @Override
    public void onDisable() {
        for (Player player : Bukkit.getOnlinePlayers()) {
            Object holder = player.getOpenInventory().getTopInventory().getHolder();
            if (holder instanceof SkinSelectHolder || holder instanceof SkinEditHolder) {
                player.closeInventory();
            }
        }
        if (providerInstalled) {
            BridgingAnalyzerAPI.setBlockSkinProvider(
                    player -> new ItemStack(Material.CUT_SANDSTONE, 64));
            providerInstalled = false;
        }
        if (lotterySubsystem != null) {
            lotterySubsystem.close();
            lotterySubsystem = null;
        }
        if (skinService != null) {
            saveData();
        }
        if (repository != null) {
            try {
                repository.close();
            } catch (RuntimeException exception) {
                getLogger().log(Level.SEVERE, "关闭皮肤数据库失败", exception);
            }
            repository = null;
        }
        skinService = null;
        if (skins != null) {
            skins.clear();
        }
    }

    /** Fail the login instead of returning and later persisting a fake default record. */
    @EventHandler(priority = EventPriority.LOWEST)
    public void onLogin(PlayerLoginEvent event) {
        try {
            requireSkinService().getOrCreate(event.getPlayer());
        } catch (RuntimeException exception) {
            getLogger().log(Level.SEVERE,
                    "无法安全加载 " + event.getPlayer().getName() + " 的皮肤", exception);
            event.disallow(PlayerLoginEvent.Result.KICK_OTHER,
                    "§c皮肤数据库暂时不可用，为保护数据已拒绝登录，请稍后重试。");
        }
    }

    @EventHandler
    public void onLeave(PlayerQuitEvent event) {
        PlayerSkin cached = skins.get(event.getPlayer().getName());
        if (cached == null || skinService == null) {
            return;
        }
        try {
            skinService.save(cached);
        } catch (RuntimeException exception) {
            getLogger().log(Level.SEVERE,
                    "退出时保存 " + event.getPlayer().getName() + " 的皮肤失败", exception);
        }
    }

    @EventHandler
    public void onDrop(PlayerDropItemEvent event) {
        ItemStack item = event.getItemDrop().getItemStack();
        if (isLegacySkinToken(item)) {
            event.setCancelled(true);
        }
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = false)
    public void onInventoryClick(InventoryClickEvent event) {
        if (!(event.getView().getTopInventory().getHolder() instanceof SkinSelectHolder)) {
            return;
        }
        // SkinEditListener owns UUID/slot routing at LOWEST. This public legacy
        // handler remains as a final safety net for separately constructed menus.
        event.setCancelled(true);
    }

    @EventHandler
    public void onMove(InventoryMoveItemEvent event) {
        if (event.getSource().getHolder() instanceof SkinSelectHolder
                || event.getDestination().getHolder() instanceof SkinSelectHolder) {
            event.setCancelled(true);
        }
    }

    /** Legacy skin-token redemption remains compatible, but database success comes first. */
    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onSetSkin(PlayerInteractEvent event) {
        if (event.getHand() != EquipmentSlot.HAND
                || (lotterySubsystem != null && lotterySubsystem.isCrate(event.getClickedBlock()))) {
            return;
        }
        ItemStack item = event.getPlayer().getInventory().getItemInMainHand();
        if (!isLegacySkinToken(item)) {
            return;
        }
        Material material = item.getType();
        if (!material.isBlock() || IllegalMaterial.isIllegal(material)) {
            event.setCancelled(true);
            NetworkMessages.send(event.getPlayer(), "&c这个方块不能作为搭路皮肤");
            return;
        }
        event.setCancelled(true);
        try {
            List<SkinService.UnlockResult> results = requireSkinService()
                    .unlock(event.getPlayer(), List.of(material));
            if (results.isEmpty() || !results.getFirst().newlyUnlocked()) {
                NetworkMessages.send(event.getPlayer(), "&e你已经拥有这个方块皮肤");
                return;
            }
            item.subtract(1);
            NetworkMessages.send(event.getPlayer(),
                    "&a此方块已添加到你的搭路皮肤库存，输入 /bskin 切换皮肤");
        } catch (RuntimeException exception) {
            getLogger().log(Level.SEVERE, "兑换皮肤方块失败: " + event.getPlayer().getName(), exception);
            NetworkMessages.send(event.getPlayer(), "&c皮肤保存失败，物品没有被消耗");
        }
    }

    private RewardAuthorizer lotteryAuthorizer() {
        return new RewardAuthorizer() {
            @Override
            public java.util.Set<Material> ownedBy(Player player)
                    throws RewardAuthorizationException {
                try {
                    return requireSkinService().ownedMaterials(player);
                } catch (RuntimeException exception) {
                    throw new RewardAuthorizationException("读取 UUID 皮肤库存失败", exception);
                }
            }

            @Override
            public void grantAtomically(Player player, List<Material> rewards)
                    throws RewardAuthorizationException {
                try {
                    requireSkinService().unlock(player, rewards);
                } catch (RuntimeException exception) {
                    throw new RewardAuthorizationException("写入 UUID 皮肤奖励失败", exception);
                }
            }
        };
    }

    private static boolean isLegacySkinToken(ItemStack item) {
        return item != null && item.getType() != Material.AIR
                && item.hasItemMeta() && item.getItemMeta().hasLore()
                && item.getItemMeta().getLore().contains("§6皮肤方块");
    }

    private void saveData() {
        if (skinService == null) {
            return;
        }
        for (PlayerSkin skin : skinService.allLoaded()) {
            try {
                skinService.save(skin);
            } catch (RuntimeException exception) {
                getLogger().log(Level.SEVERE,
                        "定期保存皮肤失败: " + skin.player + " / " + skin.uuid, exception);
            }
        }
    }

    public static BridgingSkin getInstance() {
        return instance;
    }

    public static SkinService getSkinService() {
        return requireSkinService();
    }

    private static SkinService requireSkinService() {
        if (skinService == null) {
            throw new IllegalStateException("BridgingSkin 数据服务尚未就绪");
        }
        return skinService;
    }
}
