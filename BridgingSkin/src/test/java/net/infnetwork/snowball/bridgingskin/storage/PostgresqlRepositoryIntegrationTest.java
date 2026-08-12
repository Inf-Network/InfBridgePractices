package net.infnetwork.snowball.bridgingskin.storage;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Properties;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.logging.Logger;
import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.Test;
import net.infnetwork.snowball.bridgingskin.data.PlayerSkin;
import net.infnetwork.snowball.bridgingskin.data.SkinDataSanitizer;
import net.infnetwork.snowball.bridgingskin.data.SkinSet;

/** Optional real-PostgreSQL contract test; enabled with BRIDGING_SKIN_PG_URL. */
class PostgresqlRepositoryIntegrationTest {
    @Test
    void portableSchemaImportClaimAndSaveWorkOnPostgresql() {
        String url = System.getenv("BRIDGING_SKIN_PG_URL");
        Assumptions.assumeTrue(url != null && !url.isBlank());
        String name = "PgPlayer";
        UUID legacyUuid = SkinDataSanitizer.offlineUuid(name);
        LinkedHashSet<SkinSet> owned = new LinkedHashSet<>();
        owned.add(new SkinSet("CUT_SANDSTONE"));
        owned.add(new SkinSet("DIAMOND_BLOCK"));
        PlayerSkin legacy = new PlayerSkin(
                name, legacyUuid.toString(), new SkinSet("DIAMOND_BLOCK"), owned);

        try (JdbcSkinRepository repository = new JdbcSkinRepository(
                url, new Properties(), Logger.getAnonymousLogger())) {
            assertTrue(repository.importLegacyJson(List.of(new LegacySkinRecord(
                    name + ".json", "a".repeat(64), legacy)), "b".repeat(64)));
            UUID profileUuid = UUID.randomUUID();
            PlayerSkin claimed = repository.loadOrAdopt(profileUuid, name);
            assertEquals("DIAMOND_BLOCK", claimed.currentSkin.material);
            claimed.allSkin.add(new SkinSet("GOLD_BLOCK"));
            repository.save(claimed);
            assertEquals(List.of("CUT_SANDSTONE", "DIAMOND_BLOCK", "GOLD_BLOCK"),
                    repository.loadAll().get(profileUuid).allSkin.stream()
                            .map(entry -> entry.material).toList());
        }
    }

    @Test
    void concurrentInstancesCannotOverwriteOfflineMigrationWithDefault() throws Exception {
        String url = System.getenv("BRIDGING_SKIN_PG_URL");
        Assumptions.assumeTrue(url != null && !url.isBlank());
        String name = "PgRacePlayer";
        UUID offlineUuid = SkinDataSanitizer.offlineUuid(name);
        UUID profileUuid = UUID.randomUUID();
        LinkedHashSet<SkinSet> owned = new LinkedHashSet<>();
        owned.add(new SkinSet("CUT_SANDSTONE"));
        owned.add(new SkinSet("DIAMOND_BLOCK"));
        owned.add(new SkinSet("GOLD_BLOCK"));
        try (JdbcSkinRepository bootstrap = new JdbcSkinRepository(
                url, new Properties(), Logger.getAnonymousLogger())) {
            bootstrap.save(new PlayerSkin(
                    name, offlineUuid.toString(), new SkinSet("GOLD_BLOCK"), owned));
        }

        try (JdbcSkinRepository first = new JdbcSkinRepository(
                     url, new Properties(), Logger.getAnonymousLogger());
             JdbcSkinRepository second = new JdbcSkinRepository(
                     url, new Properties(), Logger.getAnonymousLogger());
             var executor = Executors.newFixedThreadPool(2)) {
            CountDownLatch start = new CountDownLatch(1);
            Future<PlayerSkin> firstResult = executor.submit(() -> {
                start.await();
                return first.loadOrAdopt(profileUuid, name);
            });
            Future<PlayerSkin> secondResult = executor.submit(() -> {
                start.await();
                return second.loadOrAdopt(profileUuid, name);
            });
            start.countDown();

            assertEquals("GOLD_BLOCK", firstResult.get().currentSkin.material);
            assertEquals("GOLD_BLOCK", secondResult.get().currentSkin.material);
        }

        try (JdbcSkinRepository verification = new JdbcSkinRepository(
                url, new Properties(), Logger.getAnonymousLogger())) {
            assertEquals(List.of("CUT_SANDSTONE", "DIAMOND_BLOCK", "GOLD_BLOCK"),
                    verification.loadAll().get(profileUuid).allSkin.stream()
                            .map(entry -> entry.material).toList());
            assertTrue(!verification.loadAll().containsKey(offlineUuid));
        }
    }
}
