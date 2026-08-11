/*
 * 搭路等级数据的持久层。
 *
 * 支持两种后端,由 config.yml 的 database.type 决定:
 *   sqlite      —— 本地开发用,零配置,数据落在插件目录的 blocklv.db。
 *                  驱动由 Paper 自带(libraries/org/xerial/sqlite-jdbc)。
 *   postgresql  —— 生产用(Ubuntu 主服)。驱动由 plugin.yml 的 libraries 段
 *                  在启动时下载,Paper 不自带 PG 驱动。
 *
 * 原版是 MySQLUtil,硬编码连接 jdbc:mysql://localhost:3306/blocklv。
 *
 * 所有 SQL 都写成两边通吃的形式,没有方言分支:
 *   - 标识符一律不加引号的小写。原版用的反引号是 MySQL/SQLite 方言,PostgreSQL 不认。
 *   - upsert 用 INSERT ... ON CONFLICT ... DO UPDATE。
 *     PostgreSQL 9.5+ 与 SQLite 3.24+ 语法完全一致
 *     (Paper 自带的 sqlite-jdbc 是 3.49,远超要求)。
 *     原版是 delete + insert 两条语句,非原子,中途失败会留下空记录。
 *   - varchar / char / bigint / limit 都是标准 SQL,两边通用。
 */
package com.luanmenglei.lv;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Properties;

import com.luanmenglei.lv.HolographicDisplay.DisPlay;
import com.luanmenglei.lv.HolographicDisplay.Lv;
import com.luanmenglei.lv.core.PointManger;
import org.bukkit.entity.Player;

/**
 * 每个玩家一行,以 UUID 为主键;name 冗余存一份,只为排行榜显示用,
 * 避免为了显示名字去查 Mojang API。
 */
public class Database {

    /** 排行榜取前几名,与原版一致。 */
    private static final int TOP_SIZE = 10;

    private Connection connection;

    /**
     * @param jdbcUrl 形如 jdbc:sqlite:plugins/BlockLv/blocklv.db
     *                或   jdbc:postgresql://host:5432/dbname
     * @param props   用户名密码等;SQLite 传空即可
     */
    public Database(String jdbcUrl, Properties props) throws SQLException {
        this.connection = DriverManager.getConnection(jdbcUrl, props);
        this.init();
    }

    /** 建表。DDL 用标准 SQL,PostgreSQL 与 SQLite 都能吃。 */
    private void init() throws SQLException {
        try (Statement st = this.connection.createStatement()) {
            st.execute("create table if not exists blocklv ("
                    + "uuid varchar(40) not null, "
                    + "name varchar(100), "
                    + "lv bigint, "
                    + "px bigint, "
                    + "primary key (uuid))");
        }
    }

    /** 写入(或覆盖)一名玩家的等级数据。 */
    public void set(Player p, PointManger pm) {
        if (pm == null) {
            return;
        }
        try (PreparedStatement ps = this.connection.prepareStatement(
                "insert into blocklv (uuid, name, lv, px) values (?,?,?,?) "
                        + "on conflict (uuid) do update set "
                        + "name = excluded.name, lv = excluded.lv, px = excluded.px")) {
            ps.setString(1, p.getUniqueId().toString());
            ps.setString(2, p.getName());
            ps.setLong(3, pm.lv);
            ps.setLong(4, pm.px);
            ps.execute();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    /**
     * 读取一名玩家的等级数据,无记录时返回全 0 的新对象。
     *
     * 修正了原版的一个 bug:原代码是
     *     if (rs.next()) pm.lv = ...;
     *     if (rs.next()) pm.px = ...;
     * 第二个 rs.next() 会前进到"下一行",而按 uuid 查询只有一行,于是 px
     * 永远读不到,玩家每次登录经验都被清零。这里改为一次 next() 读两列。
     */
    public PointManger get(Player p) {
        PointManger pm = new PointManger();
        pm.lv = 0L;
        pm.px = 0L;
        try (PreparedStatement ps = this.connection.prepareStatement(
                "select lv, px from blocklv where uuid = ?")) {
            ps.setString(1, p.getUniqueId().toString());
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    pm.lv = rs.getLong("lv");
                    pm.px = rs.getLong("px");
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return pm;
    }

    /**
     * 刷新排行榜前十。
     *
     * 原版是把全表读进内存再做 10 轮线性扫描找最大值(O(10n));这里交给
     * SQL 排序 + limit,数据量大时差别明显,结果完全一致。
     */
    public void refreshTop() {
        List<Lv> tops = new ArrayList<>();
        try (PreparedStatement ps = this.connection.prepareStatement(
                "select lv, name from blocklv order by lv desc limit " + TOP_SIZE)) {
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    tops.add(new Lv((int) rs.getLong("lv"), rs.getString("name")));
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        tops.sort(Comparator.comparingInt((Lv l) -> l.lv).reversed());
        DisPlay.tops = tops;
    }

    /** 关服时释放连接。原版没有这一步,连接一直挂到进程退出。 */
    public void close() {
        try {
            if (this.connection != null && !this.connection.isClosed()) {
                this.connection.close();
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }
}
