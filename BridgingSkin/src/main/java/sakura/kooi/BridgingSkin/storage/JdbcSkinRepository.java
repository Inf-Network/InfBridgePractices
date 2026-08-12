package sakura.kooi.BridgingSkin.storage;

import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Properties;
import java.util.UUID;
import java.util.logging.Logger;
import sakura.kooi.BridgingSkin.data.PlayerSkin;
import sakura.kooi.BridgingSkin.data.SkinDataSanitizer;
import sakura.kooi.BridgingSkin.data.SkinSet;

/**
 * Portable SQLite/PostgreSQL repository.
 *
 * <p>Legacy JSON identities deliberately live in separate staging tables. A
 * legacy offline UUID becomes current data only when an authenticated Bukkit
 * UUID claims its uniquely matching name in one transaction.</p>
 */
public final class JdbcSkinRepository implements AutoCloseable {
    private static final String PROFILES = "bridging_skin_profiles";
    private static final String OWNED = "bridging_skin_owned";
    private static final String LEGACY_PROFILES = "bridging_skin_legacy_profiles";
    private static final String LEGACY_OWNED = "bridging_skin_legacy_owned";
    private static final String IMPORT_RUNS = "bridging_skin_import_runs";
    private static final String META = "bridging_skin_meta";
    private static final String SCHEMA_VERSION = "2";

    private final Connection connection;
    private final Logger logger;
    private final boolean postgresql;

    public JdbcSkinRepository(String jdbcUrl, Properties properties, Logger logger) {
        this.logger = Objects.requireNonNull(logger, "logger");
        Objects.requireNonNull(jdbcUrl, "jdbcUrl");
        try {
            connection = DriverManager.getConnection(jdbcUrl,
                    properties == null ? new Properties() : properties);
            DatabaseMetaData metadata = connection.getMetaData();
            postgresql = metadata.getDatabaseProductName()
                    .toLowerCase(Locale.ROOT).contains("postgresql");
            configureConnection(jdbcUrl);
            initializeSchema();
        } catch (SQLException exception) {
            throw new SkinStorageException("无法连接或初始化 BridgingSkin 数据库", exception);
        }
    }

    private void configureConnection(String jdbcUrl) throws SQLException {
        if (!jdbcUrl.startsWith("jdbc:sqlite:")) {
            return;
        }
        try (Statement statement = connection.createStatement()) {
            statement.execute("pragma foreign_keys = on");
            statement.execute("pragma busy_timeout = 5000");
            statement.execute("pragma journal_mode = wal");
        }
    }

    private void initializeSchema() throws SQLException {
        try (Statement statement = connection.createStatement()) {
            statement.execute("create table if not exists " + PROFILES + " ("
                    + "profile_uuid varchar(36) not null, "
                    + "last_name varchar(16) not null, "
                    + "normalized_name varchar(16) not null, "
                    + "selected_material varchar(64) not null, "
                    + "revision bigint not null, "
                    + "updated_at bigint not null, "
                    + "constraint bridging_skin_profiles_pk primary key (profile_uuid))");
            statement.execute("create index if not exists bridging_skin_profiles_name_idx on "
                    + PROFILES + " (normalized_name)");
            statement.execute("create table if not exists " + OWNED + " ("
                    + "profile_uuid varchar(36) not null, "
                    + "material varchar(64) not null, "
                    + "sort_order integer not null, "
                    + "unlocked_at bigint not null, "
                    + "constraint bridging_skin_owned_pk primary key (profile_uuid, material), "
                    + "constraint bridging_skin_owned_order_uq unique (profile_uuid, sort_order), "
                    + "constraint bridging_skin_owned_profile_fk foreign key (profile_uuid) "
                    + "references " + PROFILES + " (profile_uuid) on delete cascade)");

            statement.execute("create table if not exists " + LEGACY_PROFILES + " ("
                    + "legacy_uuid varchar(36) not null, "
                    + "legacy_name varchar(16) not null, "
                    + "normalized_name varchar(16) not null, "
                    + "selected_material varchar(64) not null, "
                    + "source_file varchar(255) not null, "
                    + "source_sha256 varchar(64) not null, "
                    + "import_manifest varchar(64) not null, "
                    + "claimed_profile_uuid varchar(36), "
                    + "claimed_at bigint, "
                    + "imported_at bigint not null, "
                    + "constraint bridging_skin_legacy_profiles_pk primary key (legacy_uuid), "
                    + "constraint bridging_skin_legacy_source_uq unique (source_file), "
                    + "constraint bridging_skin_legacy_claim_uq unique (claimed_profile_uuid))");
            statement.execute("create index if not exists bridging_skin_legacy_name_idx on "
                    + LEGACY_PROFILES + " (normalized_name)");
            statement.execute("create table if not exists " + LEGACY_OWNED + " ("
                    + "legacy_uuid varchar(36) not null, "
                    + "material varchar(64) not null, "
                    + "sort_order integer not null, "
                    + "constraint bridging_skin_legacy_owned_pk primary key (legacy_uuid, material), "
                    + "constraint bridging_skin_legacy_owned_order_uq unique (legacy_uuid, sort_order), "
                    + "constraint bridging_skin_legacy_owned_profile_fk foreign key (legacy_uuid) "
                    + "references " + LEGACY_PROFILES + " (legacy_uuid) on delete cascade)");
            statement.execute("create table if not exists " + IMPORT_RUNS + " ("
                    + "manifest_sha256 varchar(64) not null, "
                    + "file_count integer not null, "
                    + "owned_count integer not null, "
                    + "completed_at bigint not null, "
                    + "constraint bridging_skin_import_runs_pk primary key (manifest_sha256))");
            statement.execute("create table if not exists " + META + " ("
                    + "meta_key varchar(64) not null, "
                    + "meta_value varchar(255) not null, "
                    + "constraint bridging_skin_meta_pk primary key (meta_key))");
        }
        try (PreparedStatement statement = connection.prepareStatement(
                "insert into " + META + " (meta_key, meta_value) values (?,?) "
                        + "on conflict (meta_key) do nothing")) {
            statement.setString(1, "schema_version");
            statement.setString(2, SCHEMA_VERSION);
            statement.executeUpdate();
        }
        try (PreparedStatement statement = connection.prepareStatement(
                "select meta_value from " + META + " where meta_key = ?")) {
            statement.setString(1, "schema_version");
            try (ResultSet result = statement.executeQuery()) {
                if (!result.next() || !SCHEMA_VERSION.equals(result.getString(1))) {
                    throw new SQLException("不支持的 BridgingSkin schema 版本");
                }
            }
        }
    }

    /**
     * Imports the frozen JSON snapshot to staging. The completion row and all
     * staging rows share one transaction; source files are never changed.
     */
    public synchronized boolean importLegacyJson(
            List<LegacySkinRecord> records,
            String manifestSha256
    ) {
        Objects.requireNonNull(records, "records");
        requireSha256(manifestSha256, "manifest");
        return inTransaction("导入旧 JSON 皮肤", () -> {
            ImportRun existing = findImportRun(manifestSha256);
            int ownedCount = records.stream().mapToInt(record -> record.skin().allSkin.size()).sum();
            if (existing != null) {
                if (existing.fileCount() != records.size()
                        || existing.ownedCount() != ownedCount
                        || !sameImportedSources(records, manifestSha256)) {
                    throw new SQLException("迁移清单标记存在，但 staging 数据与源文件不一致");
                }
                return false;
            }
            if (countRows(IMPORT_RUNS) != 0L || countRows(LEGACY_PROFILES) != 0L
                    || countRows(LEGACY_OWNED) != 0L) {
                throw new SQLException("检测到另一份或不完整的旧皮肤迁移，拒绝覆盖");
            }

            long now = System.currentTimeMillis();
            for (LegacySkinRecord record : records) {
                PlayerSkin skin = SkinDataSanitizer.requirePersistable(record.skin());
                insertLegacyProfile(record, skin, manifestSha256, now);
                insertLegacyOwned(skin);
            }
            try (PreparedStatement statement = connection.prepareStatement(
                    "insert into " + IMPORT_RUNS
                            + " (manifest_sha256, file_count, owned_count, completed_at) "
                            + "values (?,?,?,?)")) {
                statement.setString(1, manifestSha256);
                statement.setInt(2, records.size());
                statement.setInt(3, ownedCount);
                statement.setLong(4, now);
                statement.executeUpdate();
            }
            return true;
        });
    }

    private void insertLegacyProfile(
            LegacySkinRecord record,
            PlayerSkin skin,
            String manifest,
            long now
    ) throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement(
                "insert into " + LEGACY_PROFILES + " (legacy_uuid, legacy_name, "
                        + "normalized_name, selected_material, source_file, source_sha256, "
                        + "import_manifest, claimed_profile_uuid, claimed_at, imported_at) "
                        + "values (?,?,?,?,?,?,?,null,null,?)")) {
            statement.setString(1, skin.uuid);
            statement.setString(2, skin.player);
            statement.setString(3, normalizeName(skin.player));
            statement.setString(4, skin.currentSkin.material);
            statement.setString(5, record.sourceFile());
            statement.setString(6, record.sourceSha256());
            statement.setString(7, manifest);
            statement.setLong(8, now);
            statement.executeUpdate();
        }
    }

    private void insertLegacyOwned(PlayerSkin skin) throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement(
                "insert into " + LEGACY_OWNED + " (legacy_uuid, material, sort_order) "
                        + "values (?,?,?)")) {
            int order = 0;
            for (SkinSet entry : skin.allSkin) {
                statement.setString(1, skin.uuid);
                statement.setString(2, entry.material);
                statement.setInt(3, order++);
                statement.addBatch();
            }
            statement.executeBatch();
        }
    }

    private boolean sameImportedSources(
            List<LegacySkinRecord> records,
            String manifest
    ) throws SQLException {
        Map<String, String> expected = new LinkedHashMap<>();
        for (LegacySkinRecord record : records) {
            expected.put(record.sourceFile(), record.sourceSha256());
        }
        Map<String, String> stored = new LinkedHashMap<>();
        try (PreparedStatement statement = connection.prepareStatement(
                "select source_file, source_sha256 from " + LEGACY_PROFILES
                        + " where import_manifest = ?")) {
            statement.setString(1, manifest);
            try (ResultSet result = statement.executeQuery()) {
                while (result.next()) {
                    stored.put(result.getString(1), result.getString(2));
                }
            }
        }
        return expected.equals(stored);
    }

    private ImportRun findImportRun(String manifest) throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement(
                "select file_count, owned_count from " + IMPORT_RUNS
                        + " where manifest_sha256 = ?")) {
            statement.setString(1, manifest);
            try (ResultSet result = statement.executeQuery()) {
                return result.next() ? new ImportRun(result.getInt(1), result.getInt(2)) : null;
            }
        }
    }

    public synchronized Map<UUID, PlayerSkin> loadAll() {
        try {
            LinkedHashMap<UUID, PlayerSkin> result = new LinkedHashMap<>();
            List<UUID> uuids = new ArrayList<>();
            try (Statement statement = connection.createStatement();
                 ResultSet rows = statement.executeQuery(
                         "select profile_uuid from " + PROFILES
                                 + " order by normalized_name, profile_uuid")) {
                while (rows.next()) {
                    uuids.add(UUID.fromString(rows.getString(1)));
                }
            }
            for (UUID uuid : uuids) {
                StoredProfile profile = loadCurrent(uuid, false);
                if (profile != null) {
                    result.put(uuid, profile.skin());
                }
            }
            return result;
        } catch (SQLException | RuntimeException exception) {
            throw new SkinStorageException("加载当前皮肤数据库失败", exception);
        }
    }

    /** Loads an authenticated UUID and atomically adopts its uniquely matching legacy row. */
    public synchronized PlayerSkin loadOrAdopt(UUID authenticatedUuid, String playerName) {
        return loadIdentity(authenticatedUuid, playerName).skin();
    }

    /**
     * Loads an identity and reports the exact obsolete offline UUID removed in the
     * same transaction, if any. Callers use that UUID to evict compatibility
     * caches without guessing from the current capitalization of the player name.
     */
    public synchronized IdentityLoad loadIdentity(UUID authenticatedUuid, String playerName) {
        Objects.requireNonNull(authenticatedUuid, "authenticatedUuid");
        SkinDataSanitizer.validatePlayerName(playerName);
        return inTransaction("加载/认领玩家皮肤", () -> {
            StoredProfile direct = loadCurrent(authenticatedUuid, true);
            if (direct != null) {
                // Once a UUID has a current profile it is authoritative. Looking at
                // name-based legacy rows here would make a rename steal another
                // player's unclaimed data or resurrect an administrator-removed skin.
                return authoritativeIdentity(authenticatedUuid, playerName, direct);
            }

            String normalizedName = normalizeName(playerName);
            List<LegacyProfile> candidates = loadLegacyByName(normalizedName, true);
            if (candidates.size() > 1) {
                throw new SkinIdentityConflictException(
                        "玩家名 " + playerName + " 对应多条旧皮肤记录");
            }

            // Another Paper instance may have created/migrated this UUID while
            // this transaction waited for a name/legacy row lock. Re-read it
            // before any name-based adoption so stale state can never overwrite it.
            StoredProfile concurrentlyCreated = loadCurrent(authenticatedUuid, true);
            if (concurrentlyCreated != null) {
                return authoritativeIdentity(
                        authenticatedUuid, playerName, concurrentlyCreated);
            }
            if (candidates.isEmpty()) {
                return adoptCurrentOfflineProfile(
                        authenticatedUuid, playerName, normalizedName);
            }

            LegacyProfile legacy = candidates.getFirst();
            if (!SkinDataSanitizer.isVerifiedOfflineIdentity(
                    legacy.legacyUuid(), legacy.skin().player)) {
                throw new SkinIdentityConflictException(
                        "旧皮肤 UUID 无法验证: " + legacy.skin().player);
            }

            UUID previousClaim = legacy.claimedProfileUuid();
            StoredProfile previous = null;
            if (previousClaim != null && !previousClaim.equals(authenticatedUuid)) {
                previous = loadCurrent(previousClaim, true);
                if (previous == null) {
                    throw new SkinIdentityConflictException(
                            "旧皮肤当前认领记录缺失");
                }
                // Offline UUID generation is case-sensitive. Validate against
                // the current profile's own spelling, not the original JSON
                // spelling, so CaseName -> casename -> UniversalAuth remains safe.
                if (!SkinDataSanitizer.isVerifiedOfflineIdentity(
                        previousClaim, previous.skin().player)) {
                    // A permanent account already owned this historical name. A new
                    // account reusing the name starts clean and must not steal it.
                    return createDefaultIdentity(authenticatedUuid, playerName);
                }
                if (!normalizeName(previous.skin().player)
                        .equals(legacy.normalizedName())) {
                    throw new SkinIdentityConflictException(
                            "旧离线 UUID 的当前玩家名与历史记录不匹配");
                }
            }

            StoredProfile merged = mergeProfiles(
                    authenticatedUuid, playerName, null, previous, legacy);
            if (!insertExactIfAbsent(
                    merged.skin(), merged.revision(), System.currentTimeMillis())) {
                StoredProfile raced = loadCurrent(authenticatedUuid, true);
                if (raced == null) {
                    throw new SQLException("UUID 档案并发创建后无法重新读取: "
                            + authenticatedUuid);
                }
                return authoritativeIdentity(authenticatedUuid, playerName, raced);
            }
            claimLegacy(legacy.legacyUuid(), previousClaim, authenticatedUuid);
            UUID retiredUuid = null;
            if (previous != null && !previousClaim.equals(authenticatedUuid)) {
                deleteCurrent(previousClaim);
                retiredUuid = previousClaim;
                logger.info("BridgingSkin 已把旧离线 UUID " + previousClaim
                        + " 安全升级为账号 UUID " + authenticatedUuid);
            }
            return new IdentityLoad(merged.skin(), retiredUuid);
        });
    }

    /**
     * Upgrades profiles created by v4 while the server still used Bukkit offline
     * UUIDs. Such records have no JSON staging row, so they must be discovered in
     * the current table by their verified OfflinePlayer identity.
     */
    private IdentityLoad adoptCurrentOfflineProfile(
            UUID authenticatedUuid,
            String playerName,
            String normalizedName
    ) throws SQLException {
        List<CurrentIdentity> sameName = loadCurrentByName(normalizedName, true);
        List<CurrentIdentity> verifiedOffline = sameName.stream()
                .filter(identity -> SkinDataSanitizer.isVerifiedOfflineIdentity(
                        identity.uuid(), identity.profile().skin().player))
                .toList();

        if (verifiedOffline.size() > 1
                || (!verifiedOffline.isEmpty() && sameName.size() != 1)) {
            throw new SkinIdentityConflictException(
                    "玩家名 " + playerName + " 的离线 UUID 升级存在歧义");
        }
        if (verifiedOffline.isEmpty()) {
            // Any existing permanent UUID belongs to a previous owner of the
            // display name. Names are metadata, so the new account starts clean.
            return createDefaultIdentity(authenticatedUuid, playerName);
        }

        CurrentIdentity previous = verifiedOffline.getFirst();
        PlayerSkin migrated = SkinDataSanitizer.copyWithIdentity(
                previous.profile().skin(), playerName, authenticatedUuid);
        if (!insertExactIfAbsent(
                migrated, previous.profile().revision(), System.currentTimeMillis())) {
            StoredProfile raced = loadCurrent(authenticatedUuid, true);
            if (raced == null) {
                throw new SQLException("UUID 档案并发创建后无法重新读取: "
                        + authenticatedUuid);
            }
            return authoritativeIdentity(authenticatedUuid, playerName, raced);
        }
        deleteCurrent(previous.uuid());
        logger.info("BridgingSkin 已把无 JSON staging 的离线 UUID " + previous.uuid()
                + " 安全升级为账号 UUID " + authenticatedUuid);
        return new IdentityLoad(migrated, previous.uuid());
    }

    private IdentityLoad createDefaultIdentity(UUID uuid, String playerName)
            throws SQLException {
        PlayerSkin created = new PlayerSkin(playerName, uuid.toString());
        PlayerSkin persisted = SkinDataSanitizer.requirePersistable(created);
        if (insertExactIfAbsent(persisted, 0L, System.currentTimeMillis())) {
            return new IdentityLoad(persisted, null);
        }

        StoredProfile raced = loadCurrent(uuid, true);
        if (raced == null) {
            throw new SQLException("UUID 档案并发创建后无法重新读取: " + uuid);
        }
        return authoritativeIdentity(uuid, playerName, raced);
    }

    private IdentityLoad authoritativeIdentity(
            UUID uuid,
            String playerName,
            StoredProfile stored
    ) throws SQLException {
        PlayerSkin resolved = stored.skin();
        if (!resolved.player.equals(playerName)) {
            updateCurrentName(uuid, playerName);
            resolved = SkinDataSanitizer.copyWithIdentity(resolved, playerName, uuid);
        }
        return new IdentityLoad(resolved, null);
    }

    private StoredProfile mergeProfiles(
            UUID targetUuid,
            String targetName,
            StoredProfile direct,
            StoredProfile previous,
            LegacyProfile legacy
    ) {
        LinkedHashSet<String> materials = new LinkedHashSet<>();
        addMaterials(materials, legacy.skin());
        if (previous != null) {
            addMaterials(materials, previous.skin());
        }
        if (direct != null) {
            addMaterials(materials, direct.skin());
        }

        String selected;
        if (isMeaningfullyModified(direct)) {
            selected = direct.skin().currentSkin.material;
        } else if (isMeaningfullyModified(previous)) {
            selected = previous.skin().currentSkin.material;
        } else {
            selected = legacy.skin().currentSkin.material;
        }
        if (!materials.contains(selected)) {
            selected = SkinDataSanitizer.DEFAULT_MATERIAL;
        }
        long revision = Math.max(direct == null ? 0L : direct.revision(),
                previous == null ? 0L : previous.revision());
        PlayerSkin merged = SkinDataSanitizer.create(
                targetName, targetUuid, selected, materials);
        return new StoredProfile(merged, revision);
    }

    private static void addMaterials(LinkedHashSet<String> target, PlayerSkin skin) {
        for (SkinSet entry : skin.allSkin) {
            target.add(entry.material);
        }
    }

    private static boolean isMeaningfullyModified(StoredProfile profile) {
        if (profile == null) {
            return false;
        }
        PlayerSkin skin = profile.skin();
        return profile.revision() > 0L
                || skin.allSkin.size() > 1
                || !SkinDataSanitizer.DEFAULT_MATERIAL.equals(skin.currentSkin.material);
    }

    private void claimLegacy(UUID legacyUuid, UUID previousClaim, UUID newClaim)
            throws SQLException {
        String sql;
        if (previousClaim == null) {
            sql = "update " + LEGACY_PROFILES
                    + " set claimed_profile_uuid = ?, claimed_at = ? "
                    + "where legacy_uuid = ? and claimed_profile_uuid is null";
        } else {
            sql = "update " + LEGACY_PROFILES
                    + " set claimed_profile_uuid = ?, claimed_at = ? "
                    + "where legacy_uuid = ? and claimed_profile_uuid = ?";
        }
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, newClaim.toString());
            statement.setLong(2, System.currentTimeMillis());
            statement.setString(3, legacyUuid.toString());
            if (previousClaim != null) {
                statement.setString(4, previousClaim.toString());
            }
            if (statement.executeUpdate() != 1) {
                throw new SkinIdentityConflictException("旧皮肤认领状态发生并发变化");
            }
        }
    }

    public synchronized PlayerSkin save(PlayerSkin rawSkin) {
        PlayerSkin candidate = SkinDataSanitizer.sanitize(rawSkin);
        UUID uuid = UUID.fromString(candidate.uuid);
        return inTransaction("保存玩家皮肤", () -> {
            StoredProfile current = loadCurrent(uuid, true);
            if (current != null && sameSkin(current.skin(), candidate)) {
                return current.skin();
            }
            long revision = current == null ? 1L : current.revision() + 1L;
            saveExact(candidate, revision, System.currentTimeMillis());
            return candidate;
        });
    }

    public synchronized void clearMaterialGlobally(String material, String fallbackMaterial) {
        String target = SkinDataSanitizer.requireMaterialName(material);
        String fallback = SkinDataSanitizer.requireMaterialName(fallbackMaterial);
        if (target.equals(SkinDataSanitizer.DEFAULT_MATERIAL)) {
            throw new IllegalArgumentException("不能清除默认皮肤 " + target);
        }
        inTransaction("全局清除皮肤", () -> {
            try (PreparedStatement statement = connection.prepareStatement(
                    "delete from " + OWNED + " where material = ?")) {
                statement.setString(1, target);
                statement.executeUpdate();
            }
            try (PreparedStatement statement = connection.prepareStatement(
                    "update " + PROFILES + " set selected_material = ?, "
                            + "revision = revision + 1, updated_at = ? "
                            + "where selected_material = ?")) {
                statement.setString(1, fallback);
                statement.setLong(2, System.currentTimeMillis());
                statement.setString(3, target);
                statement.executeUpdate();
            }
            // Staging is working migration state, while the original JSON remains the
            // immutable audit backup. Clearing here prevents a future claim reintroducing it.
            try (PreparedStatement statement = connection.prepareStatement(
                    "delete from " + LEGACY_OWNED + " where material = ?")) {
                statement.setString(1, target);
                statement.executeUpdate();
            }
            try (PreparedStatement statement = connection.prepareStatement(
                    "update " + LEGACY_PROFILES + " set selected_material = ? "
                            + "where selected_material = ?")) {
                statement.setString(1, fallback);
                statement.setString(2, target);
                statement.executeUpdate();
            }
            return null;
        });
    }

    private StoredProfile loadCurrent(UUID uuid, boolean lock) throws SQLException {
        String sql = "select last_name, selected_material, revision from " + PROFILES
                + " where profile_uuid = ?" + lockSuffix(lock);
        String name;
        String selected;
        long revision;
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, uuid.toString());
            try (ResultSet result = statement.executeQuery()) {
                if (!result.next()) {
                    return null;
                }
                name = result.getString(1);
                selected = result.getString(2);
                revision = result.getLong(3);
            }
        }
        LinkedHashSet<String> materials = new LinkedHashSet<>();
        try (PreparedStatement statement = connection.prepareStatement(
                "select material from " + OWNED
                        + " where profile_uuid = ? order by sort_order")) {
            statement.setString(1, uuid.toString());
            try (ResultSet result = statement.executeQuery()) {
                while (result.next()) {
                    materials.add(result.getString(1));
                }
            }
        }
        PlayerSkin skin = SkinDataSanitizer.create(name, uuid, selected, materials);
        return new StoredProfile(skin, revision);
    }

    private List<CurrentIdentity> loadCurrentByName(String normalizedName, boolean lock)
            throws SQLException {
        List<UUID> uuids = new ArrayList<>();
        String sql = "select profile_uuid from " + PROFILES
                + " where normalized_name = ? order by profile_uuid" + lockSuffix(lock);
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, normalizedName);
            try (ResultSet result = statement.executeQuery()) {
                while (result.next()) {
                    uuids.add(UUID.fromString(result.getString(1)));
                }
            }
        }

        List<CurrentIdentity> identities = new ArrayList<>(uuids.size());
        for (UUID uuid : uuids) {
            StoredProfile profile = loadCurrent(uuid, lock);
            if (profile != null) {
                identities.add(new CurrentIdentity(uuid, profile));
            }
        }
        return identities;
    }

    private List<LegacyProfile> loadLegacyByName(String normalizedName, boolean lock)
            throws SQLException {
        String sql = "select legacy_uuid, legacy_name, normalized_name, selected_material, "
                + "claimed_profile_uuid from " + LEGACY_PROFILES
                + " where normalized_name = ? order by legacy_uuid" + lockSuffix(lock);
        List<LegacyProfile> profiles = new ArrayList<>();
        List<LegacyHeader> headers = new ArrayList<>();
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, normalizedName);
            try (ResultSet result = statement.executeQuery()) {
                while (result.next()) {
                    UUID legacyUuid = UUID.fromString(result.getString(1));
                    String name = result.getString(2);
                    String normalized = result.getString(3);
                    String selected = result.getString(4);
                    String rawClaim = result.getString(5);
                    headers.add(new LegacyHeader(legacyUuid, name, normalized, selected,
                            rawClaim == null ? null : UUID.fromString(rawClaim)));
                }
            }
        }
        for (LegacyHeader header : headers) {
            LinkedHashSet<String> materials = loadLegacyMaterials(header.legacyUuid());
            PlayerSkin skin = SkinDataSanitizer.create(
                    header.name(), header.legacyUuid(), header.selected(), materials);
            profiles.add(new LegacyProfile(header.legacyUuid(), header.normalizedName(),
                    skin, header.claimedProfileUuid()));
        }
        return profiles;
    }

    private LinkedHashSet<String> loadLegacyMaterials(UUID legacyUuid) throws SQLException {
        LinkedHashSet<String> materials = new LinkedHashSet<>();
        try (PreparedStatement statement = connection.prepareStatement(
                "select material from " + LEGACY_OWNED
                        + " where legacy_uuid = ? order by sort_order")) {
            statement.setString(1, legacyUuid.toString());
            try (ResultSet result = statement.executeQuery()) {
                while (result.next()) {
                    materials.add(result.getString(1));
                }
            }
        }
        return materials;
    }

    private String lockSuffix(boolean lock) {
        return lock && postgresql ? " for update" : "";
    }

    private void saveExact(PlayerSkin skin, long revision, long now) throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement(
                "insert into " + PROFILES + " (profile_uuid, last_name, normalized_name, "
                        + "selected_material, revision, updated_at) values (?,?,?,?,?,?) "
                        + "on conflict (profile_uuid) do update set "
                        + "last_name = excluded.last_name, "
                        + "normalized_name = excluded.normalized_name, "
                        + "selected_material = excluded.selected_material, "
                        + "revision = excluded.revision, updated_at = excluded.updated_at")) {
            statement.setString(1, skin.uuid);
            statement.setString(2, skin.player);
            statement.setString(3, normalizeName(skin.player));
            statement.setString(4, skin.currentSkin.material);
            statement.setLong(5, revision);
            statement.setLong(6, now);
            statement.executeUpdate();
        }
        try (PreparedStatement statement = connection.prepareStatement(
                "delete from " + OWNED + " where profile_uuid = ?")) {
            statement.setString(1, skin.uuid);
            statement.executeUpdate();
        }
        try (PreparedStatement statement = connection.prepareStatement(
                "insert into " + OWNED
                        + " (profile_uuid, material, sort_order, unlocked_at) values (?,?,?,?)")) {
            int order = 0;
            for (SkinSet entry : skin.allSkin) {
                statement.setString(1, skin.uuid);
                statement.setString(2, entry.material);
                statement.setInt(3, order++);
                statement.setLong(4, now);
                statement.addBatch();
            }
            statement.executeBatch();
        }
    }

    private boolean insertExactIfAbsent(PlayerSkin skin, long revision, long now)
            throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement(
                "insert into " + PROFILES + " (profile_uuid, last_name, normalized_name, "
                        + "selected_material, revision, updated_at) values (?,?,?,?,?,?) "
                        + "on conflict (profile_uuid) do nothing")) {
            statement.setString(1, skin.uuid);
            statement.setString(2, skin.player);
            statement.setString(3, normalizeName(skin.player));
            statement.setString(4, skin.currentSkin.material);
            statement.setLong(5, revision);
            statement.setLong(6, now);
            if (statement.executeUpdate() != 1) {
                return false;
            }
        }
        try (PreparedStatement statement = connection.prepareStatement(
                "insert into " + OWNED
                        + " (profile_uuid, material, sort_order, unlocked_at) values (?,?,?,?)")) {
            int order = 0;
            for (SkinSet entry : skin.allSkin) {
                statement.setString(1, skin.uuid);
                statement.setString(2, entry.material);
                statement.setInt(3, order++);
                statement.setLong(4, now);
                statement.addBatch();
            }
            statement.executeBatch();
        }
        return true;
    }

    private void updateCurrentName(UUID uuid, String newName) throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement(
                "update " + PROFILES + " set last_name = ?, normalized_name = ?, updated_at = ? "
                        + "where profile_uuid = ?")) {
            statement.setString(1, newName);
            statement.setString(2, normalizeName(newName));
            statement.setLong(3, System.currentTimeMillis());
            statement.setString(4, uuid.toString());
            statement.executeUpdate();
        }
    }

    private void deleteCurrent(UUID uuid) throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement(
                "delete from " + PROFILES + " where profile_uuid = ?")) {
            statement.setString(1, uuid.toString());
            statement.executeUpdate();
        }
    }

    private static boolean sameSkin(PlayerSkin left, PlayerSkin right) {
        if (!left.player.equals(right.player)
                || !left.currentSkin.material.equals(right.currentSkin.material)
                || left.allSkin.size() != right.allSkin.size()) {
            return false;
        }
        var leftIterator = left.allSkin.iterator();
        var rightIterator = right.allSkin.iterator();
        while (leftIterator.hasNext()) {
            if (!leftIterator.next().material.equals(rightIterator.next().material)) {
                return false;
            }
        }
        return true;
    }

    private long countRows(String table) throws SQLException {
        try (Statement statement = connection.createStatement();
             ResultSet result = statement.executeQuery("select count(*) from " + table)) {
            result.next();
            return result.getLong(1);
        }
    }

    private <T> T inTransaction(String operation, SqlWork<T> work) {
        boolean previousAutoCommit;
        try {
            previousAutoCommit = connection.getAutoCommit();
            connection.setAutoCommit(false);
        } catch (SQLException exception) {
            throw new SkinStorageException(operation + "：无法开始事务", exception);
        }
        try {
            T value = work.run();
            connection.commit();
            return value;
        } catch (SQLException | RuntimeException exception) {
            rollbackQuietly();
            if (exception instanceof SkinIdentityConflictException conflict) {
                throw conflict;
            }
            throw new SkinStorageException(operation + "失败，事务已回滚", exception);
        } catch (Error error) {
            rollbackQuietly();
            throw error;
        } finally {
            restoreAutoCommit(previousAutoCommit);
        }
    }

    private void rollbackQuietly() {
        try {
            connection.rollback();
        } catch (SQLException exception) {
            logger.severe("回滚 BridgingSkin 数据库失败: " + exception.getMessage());
        }
    }

    private void restoreAutoCommit(boolean value) {
        try {
            connection.setAutoCommit(value);
        } catch (SQLException exception) {
            // Never turn a committed purchase into an apparent failure (and a
            // Vault refund) merely because connection cleanup failed afterward.
            // The next operation sets its transaction mode explicitly and will
            // surface a genuinely unusable connection.
            logger.severe("恢复数据库自动提交模式失败: " + exception.getMessage());
        }
    }

    public static String normalizeName(String name) {
        return name.toLowerCase(Locale.ROOT);
    }

    private static void requireSha256(String value, String label) {
        if (value == null || !value.matches("[0-9a-f]{64}")) {
            throw new IllegalArgumentException(label + " 不是小写 SHA-256");
        }
    }

    @Override
    public synchronized void close() {
        try {
            connection.close();
        } catch (SQLException exception) {
            throw new SkinStorageException("关闭 BridgingSkin 数据库失败", exception);
        }
    }

    private record StoredProfile(PlayerSkin skin, long revision) {
    }

    private record CurrentIdentity(UUID uuid, StoredProfile profile) {
    }

    public record IdentityLoad(PlayerSkin skin, UUID retiredUuid) {
        public IdentityLoad {
            Objects.requireNonNull(skin, "skin");
        }
    }

    private record LegacyProfile(
            UUID legacyUuid,
            String normalizedName,
            PlayerSkin skin,
            UUID claimedProfileUuid
    ) {
    }

    private record LegacyHeader(
            UUID legacyUuid,
            String name,
            String normalizedName,
            String selected,
            UUID claimedProfileUuid
    ) {
    }

    private record ImportRun(int fileCount, int ownedCount) {
    }

    @FunctionalInterface
    private interface SqlWork<T> {
        T run() throws SQLException;
    }
}
