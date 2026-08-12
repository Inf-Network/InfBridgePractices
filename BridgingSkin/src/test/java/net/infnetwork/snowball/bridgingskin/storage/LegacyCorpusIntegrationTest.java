package net.infnetwork.snowball.bridgingskin.storage;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.google.gson.GsonBuilder;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Properties;
import java.util.logging.Logger;
import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/** Optional deployment-corpus verification; enabled with BRIDGING_SKIN_CORPUS. */
class LegacyCorpusIntegrationTest {
    @TempDir
    Path temporaryDirectory;

    @Test
    void importsTheCompleteFrozenCorpusAtomicallyAndIdempotently() throws Exception {
        String configured = System.getenv("BRIDGING_SKIN_CORPUS");
        Assumptions.assumeTrue(configured != null && !configured.isBlank());
        Path corpus = Path.of(configured);
        long expected;
        try (var files = Files.list(corpus)) {
            expected = files.filter(path -> path.getFileName().toString()
                    .toLowerCase(java.util.Locale.ROOT).endsWith(".json")).count();
        }
        assertTrue(expected > 0);

        try (JdbcSkinRepository repository = new JdbcSkinRepository(
                "jdbc:sqlite:" + temporaryDirectory.resolve("corpus.db"),
                new Properties(), Logger.getAnonymousLogger())) {
            LegacyJsonMigrator migrator = new LegacyJsonMigrator(
                    repository, new GsonBuilder().create(), Logger.getAnonymousLogger());
            assertEquals(expected, migrator.migrateIfNeeded(corpus.toFile()));
            assertEquals(0, migrator.migrateIfNeeded(corpus.toFile()));
            assertTrue(repository.loadAll().isEmpty(),
                    "legacy offline UUIDs must stay out of the current profile table");
        }
    }
}
