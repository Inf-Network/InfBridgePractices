package net.infnetwork.snowball.bridgingskin.storage;

import java.io.File;
import java.util.Locale;
import java.util.Properties;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.plugin.java.JavaPlugin;

public final class SkinDatabaseFactory {
    private SkinDatabaseFactory() {
    }

    public static JdbcSkinRepository open(JavaPlugin plugin) {
        FileConfiguration config = plugin.getConfig();
        String type = config.getString("database.type", "sqlite")
                .trim().toLowerCase(Locale.ROOT);
        Properties properties = new Properties();
        String jdbcUrl;
        switch (type) {
            case "sqlite" -> {
                File databaseFile = new File(plugin.getDataFolder(), "skins.db");
                jdbcUrl = "jdbc:sqlite:" + databaseFile.getPath();
            }
            case "postgresql", "postgres", "pgsql" -> {
                String host = required(config, "database.postgresql.host");
                String database = required(config, "database.postgresql.database");
                String user = required(config, "database.postgresql.user");
                String password = required(config, "database.postgresql.password");
                int port = config.getInt("database.postgresql.port", 5432);
                jdbcUrl = "jdbc:postgresql://" + host + ":" + port + "/" + database;
                properties.setProperty("user", user);
                properties.setProperty("password", password);
                properties.setProperty("connectTimeout", "10");
                properties.setProperty("socketTimeout", "15");
                properties.setProperty("ApplicationName", "BridgingSkin");
            }
            default -> throw new SkinStorageException(
                    "不支持的 BridgingSkin 数据库类型: " + type);
        }
        plugin.getLogger().info("BridgingSkin 正在连接 " + type + " 数据库");
        return new JdbcSkinRepository(jdbcUrl, properties, plugin.getLogger());
    }

    private static String required(FileConfiguration config, String path) {
        String value = config.getString(path);
        if (value == null || value.isBlank() || value.equals("CHANGE_ME")) {
            throw new SkinStorageException("数据库配置 " + path + " 尚未填写");
        }
        return value.trim();
    }
}
