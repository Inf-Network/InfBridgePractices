/*
 * BlockLv persistence.
 *
 * The authenticated Bukkit UUID is the only player identity used here. When
 * UniversalAuth is deployed behind Velocity modern forwarding this value is
 * its permanent profileUuid, not the temporary frontUuid or Mojang UUID.
 */
package com.luanmenglei.lv;

import java.nio.charset.StandardCharsets;
import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Properties;
import java.util.Set;
import java.util.UUID;
import java.util.logging.Level;
import java.util.logging.Logger;

import com.luanmenglei.lv.HolographicDisplay.DisPlay;
import com.luanmenglei.lv.HolographicDisplay.Lv;
import com.luanmenglei.lv.core.PointManger;
import org.bukkit.entity.Player;

/**
 * One row per authenticated account. {@code uuid} is the primary key; the
 * latest player name is retained only for display and one-time legacy lookup.
 */
public class Database {

    private static final String TABLE = "blocklv";
    private static final String LEGACY_TABLE = "blocklv_legacy_id";
    private static final int TOP_SIZE = 10;

    private final Connection connection;
    private final Logger logger;
    private final Set<UUID> writeBlocked = new HashSet<>();
    private boolean legacyTableAvailable;

    /** Compatibility constructor retained for existing callers. */
    public Database(String jdbcUrl, Properties props) throws SQLException {
        this(jdbcUrl, props, Logger.getLogger(Database.class.getName()));
    }

    public Database(String jdbcUrl, Properties props, Logger logger) throws SQLException {
        this.connection = DriverManager.getConnection(jdbcUrl, props);
        this.logger = Objects.requireNonNull(logger, "logger");
        try {
            init();
        } catch (SQLException exception) {
            try {
                connection.close();
            } catch (SQLException closeFailure) {
                exception.addSuppressed(closeFailure);
            }
            throw exception;
        }
    }

    /**
     * Create the UUID schema, or preserve an old numeric-id table for lazy
     * migration. Renaming instead of dropping makes the operation recoverable.
     */
    private void init() throws SQLException {
        if (!tableExists(TABLE)) {
            createCurrentTable();
        } else {
            Set<String> columns = columns(TABLE);
            requireLegacyColumns(columns, TABLE);
            if (!columns.contains("uuid") || !hasUuidPrimaryKey(TABLE)) {
                moveLegacyTableAside(columns.contains("uuid"));
            }
        }

        if (tableExists(LEGACY_TABLE)) {
            Set<String> legacyColumns = columns(LEGACY_TABLE);
            requireLegacyColumns(legacyColumns, LEGACY_TABLE);
            // A UUID-shaped backup was imported atomically while the schema was
            // rebuilt. Only a true id/name table needs lazy per-account adoption.
            legacyTableAvailable = !legacyColumns.contains("uuid");
        }
    }

    private void createCurrentTable() throws SQLException {
        try (Statement statement = connection.createStatement()) {
            statement.execute("create table if not exists " + TABLE + " ("
                    + "uuid varchar(36) not null, "
                    + "name varchar(100) not null, "
                    + "lv bigint not null default 0, "
                    + "px bigint not null default 0, "
                    + "constraint blocklv_profile_uuid_primary_key primary key (uuid))");
        }
    }

    private void moveLegacyTableAside(boolean hasUuid) throws SQLException {
        if (tableExists(LEGACY_TABLE)) {
            throw new SQLException("检测到非 UUID 主键的 blocklv 表,但备份表 "
                    + LEGACY_TABLE + " 已存在;为避免覆盖数据,已停止自动迁移");
        }

        boolean previousAutoCommit = connection.getAutoCommit();
        connection.setAutoCommit(false);
        try (Statement statement = connection.createStatement()) {
            statement.execute("alter table " + TABLE + " rename to " + LEGACY_TABLE);
            createCurrentTable();
            if (hasUuid) {
                importValidUuidRows();
            }
            connection.commit();
            logger.info(hasUuid
                    ? "BlockLv 已重建 UUID 主键并保留旧表为 " + LEGACY_TABLE
                    : "BlockLv 已把旧 id 主键表保留为 " + LEGACY_TABLE
                            + ",玩家登录时将按唯一名字迁移到 UUID 主键");
        } catch (SQLException exception) {
            connection.rollback();
            throw exception;
        } finally {
            connection.setAutoCommit(previousAutoCommit);
        }
        legacyTableAvailable = !hasUuid;
    }

    private void requireLegacyColumns(Set<String> columns, String table) throws SQLException {
        for (String required : List.of("name", "lv", "px")) {
            if (!columns.contains(required)) {
                throw new SQLException("表 " + table + " 缺少必需字段 " + required);
            }
        }
    }

    /** Copy valid UUID rows inside the same transaction that rebuilds the table. */
    private void importValidUuidRows() throws SQLException {
        Map<UUID, StoredRow> imported = new HashMap<>();
        try (Statement query = connection.createStatement();
             ResultSet rows = query.executeQuery(
                     "select uuid, name, lv, px from " + LEGACY_TABLE)) {
            while (rows.next()) {
                String rawUuid = rows.getString("uuid");
                String name = rows.getString("name");
                if (rawUuid == null || name == null) {
                    throw new SQLException("BlockLv 混合旧表包含空 UUID 或空玩家名;"
                            + "为避免静默丢失排行榜数据,已回滚自动迁移");
                }
                try {
                    UUID uuid = UUID.fromString(rawUuid);
                    StoredRow candidate = new StoredRow(rawUuid, name,
                            rows.getLong("lv"), rows.getLong("px"));
                    imported.merge(uuid, candidate, Database::stronger);
                } catch (IllegalArgumentException invalidUuid) {
                    throw new SQLException("BlockLv 混合旧表包含无效 UUID " + rawUuid
                            + ";为避免静默丢失排行榜数据,已回滚自动迁移", invalidUuid);
                }
            }
        }
        for (Map.Entry<UUID, StoredRow> entry : imported.entrySet()) {
            StoredRow row = entry.getValue();
            upsert(entry.getKey(), row.name(), row.level(), row.experience());
        }
    }

    /** Compatibility facade; persistence itself no longer depends on Player. */
    public void set(Player player, PointManger points) {
        set(player.getUniqueId(), player.getName(), points);
    }

    /** Write or update one authenticated account. */
    public synchronized void set(UUID playerUuid, String playerName, PointManger points) {
        if (points == null) {
            return;
        }
        if (writeBlocked.contains(playerUuid)) {
            logger.warning("BlockLv 已跳过 " + playerName
                    + " 的保存:本次登录的数据读取或身份迁移未成功");
            return;
        }
        try {
            upsert(playerUuid, playerName, points.lv, points.px);
        } catch (SQLException exception) {
            logger.log(Level.SEVERE, "保存玩家等级失败: " + playerUuid, exception);
        }
    }

    private void upsert(UUID playerUuid, String playerName, long level, long experience)
            throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement(
                "insert into " + TABLE + " (uuid, name, lv, px) values (?,?,?,?) "
                        + "on conflict (uuid) do update set "
                        + "name = excluded.name, lv = excluded.lv, px = excluded.px")) {
            statement.setString(1, playerUuid.toString());
            statement.setString(2, playerName);
            statement.setLong(3, level);
            statement.setLong(4, experience);
            statement.executeUpdate();
        }
    }

    /** Compatibility facade; use the explicit UUID overload internally. */
    public PointManger get(Player player) {
        return get(player.getUniqueId(), player.getName());
    }

    /**
     * Load one authenticated account and atomically adopt its legacy row.
     *
     * <p>Name matching alone is not enough for rows already carrying a UUID:
     * only a UUID that exactly matches Bukkit's old OfflinePlayer formula is
     * eligible. This prevents an unrelated profile UUID with a reused name from
     * being stolen. A pre-existing target row is merged with the legacy row by
     * taking the greater (level, progress) pair, never by adding the values.</p>
     */
    public synchronized PointManger get(UUID playerUuid, String playerName) {
        PointManger empty = points(0L, 0L);
        boolean previousAutoCommit;
        try {
            previousAutoCommit = connection.getAutoCommit();
            connection.setAutoCommit(false);
        } catch (SQLException exception) {
            writeBlocked.add(playerUuid);
            logger.log(Level.SEVERE, "无法开始读取玩家等级: " + playerUuid, exception);
            return empty;
        }

        try {
            StoredRow direct = findByUuid(playerUuid);
            List<StoredRow> offlineCandidates = findRowsByName(TABLE, true, playerName).stream()
                    .filter(row -> !playerUuid.toString().equalsIgnoreCase(row.uuid()))
                    .filter(Database::isVerifiedOfflineRow)
                    .toList();
            List<StoredRow> idCandidates = legacyTableAvailable
                    ? findRowsByName(LEGACY_TABLE, false, playerName)
                    : List.of();

            if (offlineCandidates.size() > 1 || idCandidates.size() > 1) {
                connection.rollback();
                writeBlocked.add(playerUuid);
                logger.warning("BlockLv 已阻止 " + playerName
                        + " 的自动迁移:发现多条无法唯一确认的旧身份记录;本次登录不会覆盖数据库");
                return direct == null
                        ? empty
                        : points(direct.level(), direct.experience());
            }

            StoredRow resolved = direct;
            StoredRow migratedOffline = null;
            boolean migratedNumericId = false;
            if (offlineCandidates.size() == 1) {
                StoredRow offline = offlineCandidates.get(0);
                resolved = stronger(resolved, offline);
                upsert(playerUuid, playerName, resolved.level(), resolved.experience());
                deleteCurrentUuid(offline.uuid());
                migratedOffline = offline;
            }

            if (idCandidates.size() == 1) {
                StoredRow legacy = idCandidates.get(0);
                resolved = stronger(resolved, legacy);
                upsert(playerUuid, playerName, resolved.level(), resolved.experience());
                deleteLegacyName(playerName);
                migratedNumericId = true;
            }

            if (resolved != null) {
                updateDisplayName(playerUuid, playerName);
            }
            connection.commit();
            writeBlocked.remove(playerUuid);
            if (migratedOffline != null) {
                logger.info("BlockLv 已把 " + playerName + " 的数据从旧离线 UUID "
                        + migratedOffline.uuid() + " 迁移到账号 UUID " + playerUuid);
            }
            if (migratedNumericId) {
                logger.info("BlockLv 已把 " + playerName
                        + " 从旧 id 表迁移到账号 UUID " + playerUuid);
            }
            return resolved == null
                    ? empty
                    : points(resolved.level(), resolved.experience());
        } catch (SQLException exception) {
            rollbackQuietly();
            writeBlocked.add(playerUuid);
            logger.log(Level.SEVERE, "读取或迁移玩家等级失败: " + playerUuid, exception);
            return empty;
        } finally {
            try {
                connection.setAutoCommit(previousAutoCommit);
            } catch (SQLException exception) {
                logger.log(Level.SEVERE, "恢复数据库事务模式失败", exception);
            }
        }
    }

    private StoredRow findByUuid(UUID playerUuid) throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement(
                "select uuid, name, coalesce(lv, 0) as lv, coalesce(px, 0) as px "
                        + "from " + TABLE + " where uuid = ?")) {
            statement.setString(1, playerUuid.toString());
            try (ResultSet result = statement.executeQuery()) {
                return result.next()
                        ? new StoredRow(result.getString("uuid"),
                                result.getString("name"), result.getLong("lv"),
                                result.getLong("px"))
                        : null;
            }
        }
    }

    private List<StoredRow> findRowsByName(
            String table,
            boolean tableHasUuid,
            String playerName
    ) throws SQLException {
        String uuidColumn = tableHasUuid ? "uuid" : "null";
        try (PreparedStatement statement = connection.prepareStatement(
                "select " + uuidColumn + " as uuid, name, coalesce(lv, 0) as lv, "
                        + "coalesce(px, 0) as px from " + table
                        + " where lower(name) = lower(?)")) {
            statement.setString(1, playerName);
            try (ResultSet result = statement.executeQuery()) {
                List<StoredRow> rows = new ArrayList<>();
                while (result.next()) {
                    rows.add(new StoredRow(result.getString("uuid"),
                            result.getString("name"), result.getLong("lv"),
                            result.getLong("px")));
                }
                return rows;
            }
        }
    }

    private void deleteCurrentUuid(String oldUuid) throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement(
                "delete from " + TABLE + " where uuid = ?")) {
            statement.setString(1, oldUuid);
            statement.executeUpdate();
        }
    }

    private void updateDisplayName(UUID playerUuid, String playerName) throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement(
                "update " + TABLE + " set name = ? where uuid = ?")) {
            statement.setString(1, playerName);
            statement.setString(2, playerUuid.toString());
            statement.executeUpdate();
        }
    }

    private void deleteLegacyName(String playerName) throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement(
                "delete from " + LEGACY_TABLE + " where lower(name) = lower(?)")) {
            statement.setString(1, playerName);
            statement.executeUpdate();
        }
    }

    private static boolean isVerifiedOfflineRow(StoredRow row) {
        if (row.uuid() == null || row.name() == null) {
            return false;
        }
        try {
            UUID stored = UUID.fromString(row.uuid());
            UUID expected = UUID.nameUUIDFromBytes(
                    ("OfflinePlayer:" + row.name()).getBytes(StandardCharsets.UTF_8));
            return stored.equals(expected);
        } catch (IllegalArgumentException ignored) {
            return false;
        }
    }

    private static StoredRow stronger(StoredRow first, StoredRow second) {
        if (first == null) {
            return second;
        }
        if (second.level() > first.level()
                || second.level() == first.level()
                && second.experience() > first.experience()) {
            return second;
        }
        return first;
    }

    /** Ranking is level first, then progress, then display name; UUID never decides rank. */
    synchronized List<Lv> loadTop() throws SQLException {
        List<Lv> tops = new ArrayList<>();
        String rankingSource = legacyTableAvailable
                ? "(select lv, px, name from " + TABLE
                        + " union all select lv, px, name from " + LEGACY_TABLE
                        + ") as blocklv_rank_rows"
                : TABLE;
        try (PreparedStatement statement = connection.prepareStatement(
                "select coalesce(lv, 0) as lv, name from " + rankingSource
                        + " order by coalesce(lv, 0) desc, coalesce(px, 0) desc, "
                        + "lower(name) asc limit "
                        + TOP_SIZE);
             ResultSet result = statement.executeQuery()) {
            while (result.next()) {
                tops.add(new Lv((int) result.getLong("lv"), result.getString("name")));
            }
        }
        return tops;
    }

    public synchronized void refreshTop() {
        try {
            DisPlay.tops = loadTop();
        } catch (SQLException exception) {
            logger.log(Level.SEVERE, "刷新 BlockLv 排行榜失败", exception);
        }
    }

    public synchronized void close() {
        try {
            if (!connection.isClosed()) {
                connection.close();
            }
        } catch (SQLException exception) {
            logger.log(Level.SEVERE, "关闭 BlockLv 数据库失败", exception);
        }
    }

    private boolean tableExists(String expectedName) throws SQLException {
        return findTable(expectedName) != null;
    }

    private TableRef findTable(String expectedName) throws SQLException {
        DatabaseMetaData metadata = connection.getMetaData();
        String currentSchema = connection.getSchema();
        TableRef fallback = null;
        try (ResultSet tables = metadata.getTables(null, null, "%", new String[]{"TABLE"})) {
            while (tables.next()) {
                if (expectedName.equalsIgnoreCase(tables.getString("TABLE_NAME"))) {
                    TableRef candidate = new TableRef(
                            tables.getString("TABLE_CAT"),
                            tables.getString("TABLE_SCHEM"),
                            tables.getString("TABLE_NAME"));
                    if (currentSchema != null
                            && currentSchema.equalsIgnoreCase(candidate.schema())) {
                        return candidate;
                    }
                    if (fallback == null) {
                        fallback = candidate;
                    }
                }
            }
        }
        return fallback;
    }

    private Set<String> columns(String table) throws SQLException {
        Set<String> names = new HashSet<>();
        try (Statement statement = connection.createStatement();
             ResultSet result = statement.executeQuery("select * from " + table + " where 1 = 0")) {
            for (int index = 1; index <= result.getMetaData().getColumnCount(); index++) {
                names.add(result.getMetaData().getColumnName(index).toLowerCase(Locale.ROOT));
            }
        }
        return names;
    }

    private boolean hasUuidPrimaryKey(String table) throws SQLException {
        DatabaseMetaData metadata = connection.getMetaData();
        TableRef tableRef = findTable(table);
        if (tableRef == null) {
            return false;
        }
        Set<String> primaryColumns = new HashSet<>();
        try (ResultSet keys = metadata.getPrimaryKeys(
                tableRef.catalog(), tableRef.schema(), tableRef.name())) {
            while (keys.next()) {
                primaryColumns.add(keys.getString("COLUMN_NAME").toLowerCase(Locale.ROOT));
            }
        }
        if (primaryColumns.equals(Set.of("uuid"))) {
            return true;
        }
        return false;
    }

    private void rollbackQuietly() {
        try {
            connection.rollback();
        } catch (SQLException rollbackFailure) {
            logger.log(Level.SEVERE, "回滚 BlockLv 数据迁移失败", rollbackFailure);
        }
    }

    private static PointManger points(long level, long experience) {
        PointManger points = new PointManger();
        points.lv = level;
        points.px = experience;
        return points;
    }

    private record StoredRow(String uuid, String name, long level, long experience) {
    }

    private record TableRef(String catalog, String schema, String name) {
    }
}
