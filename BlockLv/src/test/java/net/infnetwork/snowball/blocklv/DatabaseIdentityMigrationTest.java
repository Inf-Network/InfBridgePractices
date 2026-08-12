package net.infnetwork.snowball.blocklv;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.List;
import java.util.Properties;
import java.util.UUID;
import java.util.logging.Logger;

import net.infnetwork.snowball.blocklv.holographicdisplay.Lv;
import net.infnetwork.snowball.blocklv.core.PointManger;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class DatabaseIdentityMigrationTest {

    @TempDir
    Path temporaryDirectory;

    @Test
    void newSchemaUsesUuidAsItsPrimaryKey() throws Exception {
        String url = databaseUrl("new-schema.db");
        open(url).close();
        assertUuidPrimaryKey(url);
    }

    @Test
    void adoptsOneOfflineUuidRowIntoAuthenticatedProfileUuid() throws Exception {
        String url = databaseUrl("offline-to-profile.db");
        UUID offlineUuid = UUID.fromString("0877554e-6f5c-3311-b064-d0ad21caf4e8");
        UUID profileUuid = UUID.randomUUID();

        try (TestDatabase database = open(url)) {
            database.value.set(offlineUuid, "Snowball_233", points(9, 176));

            PointManger migrated = database.value.get(profileUuid, "Snowball_233");

            assertEquals(9, migrated.lv);
            assertEquals(176, migrated.px);
            assertEquals(1, scalarLong(url, "select count(*) from blocklv"));
            assertEquals(0, scalarLong(url,
                    "select count(*) from blocklv where uuid = '" + offlineUuid + "'"));
            assertEquals(1, scalarLong(url,
                    "select count(*) from blocklv where uuid = '" + profileUuid + "'"));
        }
    }

    @Test
    void refusesAmbiguousNameMigration() throws Exception {
        String url = databaseUrl("ambiguous.db");
        UUID profileUuid = UUID.randomUUID();

        try (TestDatabase database = open(url)) {
            database.value.set(profileUuid, "SAMENAME", points(5, 6));
            database.value.set(UUID.randomUUID(), "SameName", points(40, 70));
            database.value.set(offlineUuid("SameName"), "SameName", points(20, 50));
            database.value.set(offlineUuid("samename"), "samename", points(30, 60));

            PointManger result = database.value.get(profileUuid, "SAMENAME");
            database.value.set(profileUuid, "SAMENAME", points(99, 99));

            assertEquals(5, result.lv);
            assertEquals(6, result.px);
            assertEquals(4, scalarLong(url, "select count(*) from blocklv"));
            assertEquals(1, scalarLong(url,
                    "select count(*) from blocklv where uuid = '" + profileUuid
                            + "' and lv = 5 and px = 6"));
        }
    }

    @Test
    void mergesLegacyProgressWhenProfileRowAlreadyExists() throws Exception {
        String url = databaseUrl("existing-profile.db");
        UUID profileUuid = UUID.randomUUID();

        try (TestDatabase database = open(url)) {
            database.value.set(offlineUuid("Snowball_233"),
                    "Snowball_233", points(9, 176));
            database.value.set(profileUuid, "Snowball_233", points(0, 0));

            PointManger result = database.value.get(profileUuid, "Snowball_233");

            assertEquals(9, result.lv);
            assertEquals(176, result.px);
            assertEquals(1, scalarLong(url, "select count(*) from blocklv"));
        }
    }

    @Test
    void keepsStrongerProfileProgressWhenMergingLegacyRow() throws Exception {
        String url = databaseUrl("strong-profile.db");
        UUID profileUuid = UUID.randomUUID();

        try (TestDatabase database = open(url)) {
            database.value.set(offlineUuid("Snowball_233"),
                    "Snowball_233", points(9, 176));
            database.value.set(profileUuid, "Snowball_233", points(12, 4));

            PointManger result = database.value.get(profileUuid, "Snowball_233");

            assertEquals(12, result.lv);
            assertEquals(4, result.px);
            assertEquals(1, scalarLong(url, "select count(*) from blocklv"));
        }
    }

    @Test
    void doesNotStealAnotherProfileUuidThatOnlySharesTheName() throws Exception {
        String url = databaseUrl("foreign-profile.db");
        UUID foreignProfile = UUID.randomUUID();
        UUID currentProfile = UUID.randomUUID();

        try (TestDatabase database = open(url)) {
            database.value.set(foreignProfile, "ReusedName", points(30, 80));

            PointManger result = database.value.get(currentProfile, "ReusedName");
            database.value.set(currentProfile, "ReusedName", points(1, 2));

            assertEquals(0, result.lv);
            assertEquals(0, result.px);
            assertEquals(2, scalarLong(url, "select count(*) from blocklv"));
            assertEquals(1, scalarLong(url,
                    "select count(*) from blocklv where uuid = '" + foreignProfile
                            + "' and lv = 30 and px = 80"));
        }
    }

    @Test
    void migratesNumericIdTableLazilyWithoutDroppingTheBackup() throws Exception {
        String url = databaseUrl("numeric-id.db");
        try (Connection legacy = DriverManager.getConnection(url);
             Statement statement = legacy.createStatement()) {
            statement.execute("create table blocklv ("
                    + "id integer primary key autoincrement, "
                    + "name varchar(100), lv bigint, px bigint)");
            statement.execute("insert into blocklv (name, lv, px) "
                    + "values ('GreenNa', 21, 168)");
        }

        UUID profileUuid = UUID.randomUUID();
        try (TestDatabase database = open(url)) {
            assertEquals("GreenNa", database.value.loadTop().getFirst().name);
            PointManger migrated = database.value.get(profileUuid, "GreenNa");

            assertEquals(21, migrated.lv);
            assertEquals(168, migrated.px);
            assertEquals(1, scalarLong(url,
                    "select count(*) from blocklv where uuid = '" + profileUuid + "'"));
            assertEquals(0, scalarLong(url,
                    "select count(*) from blocklv_legacy_id where lower(name) = 'greenna'"));
            assertTrue(tableExists(url, "blocklv_legacy_id"));
        }
    }

    @Test
    void rebuildingNonUniqueUuidSchemaIsIdempotentAcrossRestart() throws Exception {
        String url = databaseUrl("non-unique-uuid.db");
        UUID oldOfflineUuid = offlineUuid("GreenNa");
        UUID profileUuid = UUID.randomUUID();
        try (Connection legacy = DriverManager.getConnection(url);
             Statement statement = legacy.createStatement()) {
            statement.execute("create table blocklv ("
                    + "id integer primary key autoincrement, uuid varchar(36) unique, "
                    + "name varchar(100), lv bigint, px bigint)");
            statement.execute("insert into blocklv (uuid, name, lv, px) values ('"
                    + oldOfflineUuid + "', 'GreenNa', 21, 168)");
        }

        try (TestDatabase firstStart = open(url)) {
            PointManger migrated = firstStart.value.get(profileUuid, "GreenNa");
            assertEquals(21, migrated.lv);
            firstStart.value.set(profileUuid, "GreenNa", points(25, 7));
        }

        try (TestDatabase secondStart = open(url)) {
            PointManger afterRestart = secondStart.value.get(profileUuid, "GreenNa");
            assertEquals(25, afterRestart.lv);
            assertEquals(7, afterRestart.px);
            assertEquals(1, scalarLong(url, "select count(*) from blocklv"));
        }
        assertUuidPrimaryKey(url);
    }

    @Test
    void mixedSchemaWithUnmappableRowsRollsBackInsteadOfHidingData() throws Exception {
        String url = databaseUrl("unmappable-mixed-schema.db");
        try (Connection legacy = DriverManager.getConnection(url);
             Statement statement = legacy.createStatement()) {
            statement.execute("create table blocklv ("
                    + "id integer primary key autoincrement, uuid varchar(36) unique, "
                    + "name varchar(100), lv bigint, px bigint)");
            statement.execute("insert into blocklv (uuid, name, lv, px) values ('"
                    + offlineUuid("MappedPlayer") + "', 'MappedPlayer', 15, 2)");
            statement.execute("insert into blocklv (uuid, name, lv, px) "
                    + "values (null, 'NeedsManualMapping', 40, 9)");
        }

        assertThrows(SQLException.class, () -> open(url));
        assertEquals(2, scalarLong(url, "select count(*) from blocklv"));
        assertFalse(tableExists(url, "blocklv_legacy_id"));
    }

    @Test
    void rankingUsesLevelThenProgressRatherThanPlayerId() throws Exception {
        String url = databaseUrl("ranking.db");
        try (TestDatabase database = open(url)) {
            database.value.set(UUID.fromString("00000000-0000-0000-0000-000000000001"),
                    "Zulu", points(10, 20));
            database.value.set(UUID.fromString("ffffffff-ffff-ffff-ffff-ffffffffffff"),
                    "Alpha", points(10, 20));
            database.value.set(UUID.randomUUID(), "LowerProgress", points(10, 5));
            database.value.set(UUID.randomUUID(), "HigherLevel", points(11, 0));

            List<Lv> ranking = database.value.loadTop();

            assertEquals(List.of("HigherLevel", "Alpha", "Zulu", "LowerProgress"),
                    ranking.stream().map(entry -> entry.name).toList());
        }
    }

    private TestDatabase open(String url) throws Exception {
        return new TestDatabase(new Database(
                url, new Properties(), Logger.getLogger("BlockLvDatabaseTest")));
    }

    private String databaseUrl(String name) {
        return "jdbc:sqlite:" + temporaryDirectory.resolve(name);
    }

    private static PointManger points(long level, long experience) {
        PointManger points = new PointManger();
        points.lv = level;
        points.px = experience;
        return points;
    }

    private static UUID offlineUuid(String playerName) {
        return UUID.nameUUIDFromBytes(
                ("OfflinePlayer:" + playerName).getBytes(StandardCharsets.UTF_8));
    }

    private static long scalarLong(String url, String sql) throws Exception {
        try (Connection connection = DriverManager.getConnection(url);
             Statement statement = connection.createStatement();
             ResultSet result = statement.executeQuery(sql)) {
            result.next();
            return result.getLong(1);
        }
    }

    private static boolean tableExists(String url, String expectedName) throws Exception {
        try (Connection connection = DriverManager.getConnection(url);
             ResultSet tables = connection.getMetaData().getTables(
                     null, null, "%", new String[]{"TABLE"})) {
            while (tables.next()) {
                if (expectedName.equalsIgnoreCase(tables.getString("TABLE_NAME"))) {
                    return true;
                }
            }
            return false;
        }
    }

    private static void assertUuidPrimaryKey(String url) throws Exception {
        try (Connection inspection = DriverManager.getConnection(url);
             Statement statement = inspection.createStatement();
             ResultSet columns = statement.executeQuery("pragma table_info(blocklv)")) {
            boolean uuidPrimaryKey = false;
            boolean idColumn = false;
            while (columns.next()) {
                String name = columns.getString("name");
                if ("uuid".equalsIgnoreCase(name)) {
                    uuidPrimaryKey = columns.getInt("pk") == 1;
                }
                if ("id".equalsIgnoreCase(name)) {
                    idColumn = true;
                }
            }
            assertTrue(uuidPrimaryKey);
            assertFalse(idColumn);
        }
    }

    private static final class TestDatabase implements AutoCloseable {
        private final Database value;

        private TestDatabase(Database value) {
            this.value = value;
        }

        @Override
        public void close() {
            value.close();
        }
    }
}
