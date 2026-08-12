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

    private static final long RANK_REFRESH_INTERVAL = 1000L;

    static BlockLv instance;

    /**
     * 记录"谁把谁打了",用于判定虚空击杀。
     * key 为受害者,value 为攻击者;120 tick 后自动清除(见 PlayerDeathByPlayer)。
     */
    public Map<Player, Player> killPlayer = new HashMap<>();

    Plugin papi;

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
            this.getLogger().severe("数据库不可用,BlockLv 已停用。等级数据不会被记录。");
            Bukkit.getPluginManager().disablePlugin(this);
            return;
        }

        // Reloads can enable the plugin while players are already online.
        for (Player onlinePlayer : Bukkit.getOnlinePlayers()) {
            PointManger.players.put(onlinePlayer.getUniqueId(), this.database.get(onlinePlayer));
        }

        refreshRank();
        Bukkit.getScheduler().runTaskTimerAsynchronously(
                this, BlockLv::refreshRank, 0L, RANK_REFRESH_INTERVAL);
    }

    /** Supports SQLite and PostgreSQL; unresolved PostgreSQL placeholders are rejected. */
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

    /** Queries asynchronously; hologram mutations are dispatched to the Bukkit main thread. */
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

        // disablePlugin may re-enter here before database initialization succeeds.
        if (this.database == null) {
            return;
        }

        // Persist the final online state before closing the connection.
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
