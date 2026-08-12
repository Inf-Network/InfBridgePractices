package sakura.kooi.BridgingSkin.storage;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.google.gson.GsonBuilder;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Properties;
import java.util.UUID;
import java.util.logging.Logger;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import sakura.kooi.BridgingSkin.data.SkinDataSanitizer;

class LegacyJsonMigratorTest {
    @TempDir
    Path temporaryDirectory;

    @Test
    void invalidFileRollsBackAndCanBeCorrectedBeforeRetry() throws Exception {
        Path skins = Files.createDirectory(temporaryDirectory.resolve("skins"));
        UUID legacyUuid = SkinDataSanitizer.offlineUuid("CorrectName");
        Files.writeString(skins.resolve("WrongName.json"), json(legacyUuid, "CorrectName"),
                StandardCharsets.UTF_8);

        try (JdbcSkinRepository repository = repository()) {
            LegacyJsonMigrator migrator = new LegacyJsonMigrator(
                    repository, new GsonBuilder().create(), Logger.getAnonymousLogger());
            assertThrows(SkinStorageException.class,
                    () -> migrator.migrateIfNeeded(skins.toFile()));

            Files.delete(skins.resolve("WrongName.json"));
            Files.writeString(skins.resolve("CorrectName.json"),
                    json(legacyUuid, "CorrectName"), StandardCharsets.UTF_8);
            assertEquals(1, migrator.migrateIfNeeded(skins.toFile()));
            assertEquals(0, migrator.migrateIfNeeded(skins.toFile()));

            UUID profileUuid = UUID.randomUUID();
            assertEquals("DIAMOND_BLOCK",
                    repository.loadOrAdopt(profileUuid, "CorrectName")
                            .currentSkin.material);
        }
    }

    private JdbcSkinRepository repository() {
        return new JdbcSkinRepository(
                "jdbc:sqlite:" + temporaryDirectory.resolve("skins.db"),
                new Properties(), Logger.getAnonymousLogger());
    }

    private static String json(UUID uuid, String name) {
        return """
                {
                  "uuid": "%s",
                  "player": "%s",
                  "currentSelected": {"Material": "DIAMOND_BLOCK"},
                  "allSkins": [
                    {"Material": "CUT_SANDSTONE"},
                    {"Material": "DIAMOND_BLOCK"}
                  ]
                }
                """.formatted(uuid, name);
    }
}
