/*
 * 1.21.11 移植。相对原版的改动:
 *   - 数据层 MySQLUtil -> Database(SQLite,见该类注释)
 *   - 全息依赖 HolographicDisplays -> DecentHolograms
 *   - 关服时补上数据库连接的释放(原版没有)
 * 业务逻辑、经验公式、消息文案一律未动。
 */
package net.infnetwork.snowball.blocklv;

import java.io.File;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Properties;

import net.infnetwork.snowball.blocklv.holographicdisplay.DisPlay;
import net.infnetwork.snowball.blocklv.commands.MainCommands;
import net.infnetwork.snowball.blocklv.core.PointManger;
import net.infnetwork.snowball.blocklv.events.BlockPlace;
import net.infnetwork.snowball.blocklv.events.PlayerDeathByPlayer;
import net.infnetwork.snowball.blocklv.events.PlayerLogin;
import net.infnetwork.snowball.blocklv.events.PlayerMove;
import net.infnetwork.snowball.blocklv.papi.PAPIHooker;
import me.clip.placeholderapi.PlaceholderAPI;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.java.JavaPlugin;

public class BlockLv extends JavaPlugin {

    /** 排行榜刷新间隔(tick)。1000 tick = 50 秒,与原版一致。 */
    private static final long RANK_REFRESH_INTERVAL = 1000L;

    static BlockLv instance;

    /**
     * 记录"谁把谁打了",用于判定虚空击杀。
     * key 为受害者,value 为攻击者;120 tick 后自动清除(见 PlayerDeathByPlayer)。
     */
    public Map<Player, Player> killPlayer = new HashMap<>();

    Plugin papi;

    /** 全息插件是否可用。不可用时排行榜静默跳过,其余功能照常。 */
    private boolean onEnableHolo;

    private Database database;

    @Override
    public void onEnable() {
        this.saveDefaultConfig();
        setInstance(this);
        this.getLogger().info("BlockLv开始加载....");

        Bukkit.getPluginManager().registerEvents(new BlockPlace(), this);
        Bukkit.getPluginManager().registerEvents(new PlayerMove(), this);
        Bukkit.getPluginManager().registerEvents(new PlayerDeathByPlayer(), this);
        Bukkit.getPluginManager().registerEvents(new PlayerLogin(), this);
        this.getLogger().info("注册事件完毕");

        // 原版检查的是 HolographicDisplays,本服已换成 DecentHolograms
        if (Bukkit.getPluginManager().isPluginEnabled("DecentHolograms")) {
            this.getLogger().info("DecentHolograms正确加载...");
            this.onEnableHolo = true;
        }

        this.papi = Bukkit.getPluginManager().getPlugin("PlaceholderAPI");
        if (this.papi != null) {
            this.getLogger().info("开始链接papi");
            if (new PAPIHooker().register()) {
                this.getLogger().info("成功注册papi变量");
            } else {
                this.getLogger().warning("papi链接失败");
            }
        }

        Bukkit.getPluginCommand("blocklv").setExecutor(new MainCommands());

        // SQLite 驱动由 Paper 自带,PostgreSQL 驱动由 plugin.yml 的 libraries 段下载。
        this.database = this.connectDatabase();
        if (this.database == null) {
            // 连不上就停用,不要带着 null 的 database 继续跑 —— 那会让每次
            // 放方块、每次登录都抛 NPE,而玩家的等级数据一条都存不下来。
            this.getLogger().severe("数据库不可用,BlockLv 已停用。等级数据不会被记录。");
            Bukkit.getPluginManager().disablePlugin(this);
            return;
        }

        // 热重载场景:插件在有玩家在线时被启用,补读一次他们的数据
        for (Player onlinePlayer : Bukkit.getOnlinePlayers()) {
            PointManger.players.put(onlinePlayer.getUniqueId(), this.database.get(onlinePlayer));
        }

        refreshRank();
        Bukkit.getScheduler().runTaskTimerAsynchronously(
                this, BlockLv::refreshRank, 0L, RANK_REFRESH_INTERVAL);
    }

    /**
     * 按 config.yml 的 database.type 建立连接。
     *
     * sqlite     —— 本地开发,零配置。
     * postgresql —— 生产。占位符未替换时直接拒绝启动,而不是拿着 CHANGE_ME
     *               去连一个不存在的库、报一堆看不懂的错。
     *
     * @return 连接失败返回 null,由调用方停用插件
     */
    private Database connectDatabase() {
        String type = this.getConfig().getString("database.type", "sqlite").trim().toLowerCase(Locale.ROOT);
        Properties props = new Properties();
        String url;

        switch (type) {
            case "postgresql": {
                String host = this.getConfig().getString("database.postgresql.host", "");
                int port = this.getConfig().getInt("database.postgresql.port", 5432);
                String db = this.getConfig().getString("database.postgresql.database", "");
                String user = this.getConfig().getString("database.postgresql.user", "");
                String password = this.getConfig().getString("database.postgresql.password", "");
                for (String v : new String[]{host, db, user}) {
                    if (v == null || v.isEmpty() || v.contains("CHANGE_ME")) {
                        this.getLogger().severe("config.yml 里 database.postgresql 还是占位符,请先填真实连接信息。");
                        return null;
                    }
                }
                url = "jdbc:postgresql://" + host + ":" + port + "/" + db;
                props.setProperty("user", user);
                props.setProperty("password", password);
                break;
            }
            case "sqlite": {
                url = "jdbc:sqlite:" + new File(this.getDataFolder(), "blocklv.db").getPath();
                break;
            }
            default: {
                this.getLogger().severe("database.type 只支持 sqlite 或 postgresql,当前值: " + type);
                return null;
            }
        }

        try {
            Database d = new Database(url, props, this.getLogger());
            this.getLogger().info("数据库已连接(" + type + ")");
            return d;
        } catch (SQLException e) {
            this.getLogger().severe("数据库连接失败(" + type + "): " + e.getMessage());
            return null;
        }
    }

    /**
     * 刷新排行榜。查库在异步线程,改全息回主线程 —— Bukkit 的实体操作不是线程安全的。
     */
    public static void refreshRank() {
        if (instance.onEnableHolo && DisPlay.loadLocation("rank") != null) {
            instance.getDatabase().refreshTop();
            Bukkit.getScheduler().runTask(getInstance(), DisPlay::refreshHologrphic);
        }
    }

    @Override
    public void onDisable() {
        this.getLogger().info("disable");
        DisPlay.remove();
        PlaceholderAPI.unregisterPlaceholderHook("blocklv");

        // 数据库连不上时 onEnable 会调 disablePlugin,从而走到这里,此时 database 是 null。
        // 不判空就会在"已经出错"的路径上再抛一个 NPE,把真正的原因淹掉。
        if (this.database == null) {
            return;
        }

        // 先落盘在线玩家的数据,再关连接 —— 顺序颠倒会丢最后一次存档
        for (Player player : Bukkit.getOnlinePlayers()) {
            this.database.set(player.getUniqueId(), player.getName(),
                    PointManger.players.get(player.getUniqueId()));
        }
        this.database.close();
    }

    public static BlockLv getInstance() {
        return instance;
    }

    static void setInstance(BlockLv i) {
        instance = i;
    }

    public Database getDatabase() {
        return this.database;
    }
}
