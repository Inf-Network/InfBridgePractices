package net.infnetwork.snowball.bridgingskin.storage;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Path;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.UUID;
import java.util.logging.Logger;
import org.bukkit.Material;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import net.infnetwork.snowball.bridgingskin.SkinService;
import net.infnetwork.snowball.bridgingskin.data.PlayerSkin;
import net.infnetwork.snowball.bridgingskin.data.SkinDataSanitizer;
import net.infnetwork.snowball.bridgingskin.data.SkinSet;

class JdbcSkinRepositoryTest {
    private static final String HASH = "a".repeat(64);
    private static final String MANIFEST = "b".repeat(64);

    @TempDir
    Path temporaryDirectory;

    @Test
    void importsToStagingThenClaimsByAuthenticatedUuidIdempotently() {
        try (JdbcSkinRepository repository = repository("claim.db")) {
            PlayerSkin legacy = legacy("Snowball_233", "DIAMOND_BLOCK");
            LegacySkinRecord record = new LegacySkinRecord(
                    "Snowball_233.json", HASH, legacy);

            assertTrue(repository.importLegacyJson(List.of(record), MANIFEST));
            assertFalse(repository.importLegacyJson(List.of(record), MANIFEST));
            assertTrue(repository.loadAll().isEmpty(), "legacy UUID must not enter current table");

            UUID profileUuid = UUID.randomUUID();
            PlayerSkin claimed = repository.loadOrAdopt(profileUuid, "Snowball_233");
            assertEquals(profileUuid.toString(), claimed.uuid);
            assertEquals("DIAMOND_BLOCK", claimed.currentSkin.material);
            assertEquals(List.of("CUT_SANDSTONE", "DIAMOND_BLOCK"), materials(claimed));

            PlayerSkin again = repository.loadOrAdopt(profileUuid, "Snowball_233");
            assertEquals(materials(claimed), materials(again));
            assertEquals(Set.of(profileUuid), repository.loadAll().keySet());
        }
    }

    @Test
    void upgradesAClaimedOfflineProfileToUniversalAuthUuid() {
        try (JdbcSkinRepository repository = repository("upgrade.db")) {
            PlayerSkin legacy = legacy("Snowball_233", "DIAMOND_BLOCK");
            repository.importLegacyJson(List.of(new LegacySkinRecord(
                    "Snowball_233.json", HASH, legacy)), MANIFEST);

            UUID offlineUuid = SkinDataSanitizer.offlineUuid("Snowball_233");
            PlayerSkin offline = repository.loadOrAdopt(offlineUuid, "Snowball_233");
            offline.allSkin.add(new SkinSet("GOLD_BLOCK"));
            offline.currentSkin = new SkinSet("GOLD_BLOCK");
            repository.save(offline);

            UUID profileUuid = UUID.randomUUID();
            PlayerSkin upgraded = repository.loadOrAdopt(profileUuid, "Snowball_233");
            assertEquals(profileUuid.toString(), upgraded.uuid);
            assertEquals("GOLD_BLOCK", upgraded.currentSkin.material);
            assertEquals(List.of("CUT_SANDSTONE", "DIAMOND_BLOCK", "GOLD_BLOCK"),
                    materials(upgraded));
            assertEquals(Set.of(profileUuid), repository.loadAll().keySet());
        }
    }

    @Test
    void upgradesCurrentOfflineProfileWithoutLegacyStaging() {
        try (JdbcSkinRepository repository = repository("current-offline-upgrade.db")) {
            String offlineName = "BeforeAuth";
            UUID offlineUuid = SkinDataSanitizer.offlineUuid(offlineName);
            LinkedHashSet<SkinSet> owned = new LinkedHashSet<>();
            owned.add(new SkinSet("CUT_SANDSTONE"));
            owned.add(new SkinSet("DIAMOND_BLOCK"));
            owned.add(new SkinSet("GOLD_BLOCK"));
            repository.save(new PlayerSkin(
                    offlineName, offlineUuid.toString(), new SkinSet("GOLD_BLOCK"), owned));

            UUID profileUuid = UUID.randomUUID();
            JdbcSkinRepository.IdentityLoad upgraded = repository.loadIdentity(
                    profileUuid, "beforeauth");

            assertEquals(offlineUuid, upgraded.retiredUuid());
            assertEquals(profileUuid.toString(), upgraded.skin().uuid);
            assertEquals("GOLD_BLOCK", upgraded.skin().currentSkin.material);
            assertEquals(List.of("CUT_SANDSTONE", "DIAMOND_BLOCK", "GOLD_BLOCK"),
                    materials(upgraded.skin()));
            assertEquals(Set.of(profileUuid), repository.loadAll().keySet());
        }
    }

    @Test
    void failedUnlockDoesNotPolluteLiveServiceCache() {
        JdbcSkinRepository repository = repository("rollback.db");
        SkinService service = new SkinService(
                repository, Logger.getAnonymousLogger(), new HashMap<>());
        UUID uuid = UUID.randomUUID();
        service.getOrCreate(uuid, "SafePlayer");
        repository.close();

        PlayerSkin detachedPlayer = service.findByName("SafePlayer");
        assertThrows(SkinStorageException.class,
                () -> service.unlock(new TestPlayerIdentity(uuid, "SafePlayer").asPlayer(),
                        List.of(Material.DIAMOND_BLOCK)));
        assertFalse(detachedPlayer.allSkin.contains(new SkinSet("DIAMOND_BLOCK")));
        assertEquals(List.of("CUT_SANDSTONE"), materials(service.findByName("SafePlayer")));
    }

    @Test
    void nameLookupFailsClosedWhenTwoCurrentUuidsShareAName() {
        try (JdbcSkinRepository repository = repository("names.db")) {
            repository.save(new PlayerSkin("SameName", UUID.randomUUID().toString()));
            repository.save(new PlayerSkin("samename", UUID.randomUUID().toString()));
            SkinService service = new SkinService(
                    repository, Logger.getAnonymousLogger(), new HashMap<>());

            assertNull(service.findByName("SameName"));
            assertNull(service.findByName("SAMENAME"));
        }
    }

    @Test
    void globalClearAlsoPreventsUnclaimedLegacyFromReappearing() {
        try (JdbcSkinRepository repository = repository("clear.db")) {
            PlayerSkin legacy = legacy("OldPlayer", "DIAMOND_BLOCK");
            repository.importLegacyJson(List.of(new LegacySkinRecord(
                    "OldPlayer.json", HASH, legacy)), MANIFEST);
            repository.clearMaterialGlobally("DIAMOND_BLOCK", "CUT_SANDSTONE");

            PlayerSkin claimed = repository.loadOrAdopt(UUID.randomUUID(), "OldPlayer");
            assertEquals("CUT_SANDSTONE", claimed.currentSkin.material);
            assertEquals(List.of("CUT_SANDSTONE"), materials(claimed));
            assertThrows(IllegalArgumentException.class,
                    () -> repository.clearMaterialGlobally(
                            "CUT_SANDSTONE", "CUT_SANDSTONE"));
        }
    }

    @Test
    void importFailureRollsBackEveryStagingRowAndCompletionMarker() {
        try (JdbcSkinRepository repository = repository("import-rollback.db")) {
            LegacySkinRecord first = new LegacySkinRecord(
                    "Duplicate.json", HASH, legacy("FirstPlayer", "DIAMOND_BLOCK"));
            LegacySkinRecord conflicting = new LegacySkinRecord(
                    "Duplicate.json", "c".repeat(64), legacy("SecondPlayer", "GOLD_BLOCK"));
            assertThrows(SkinStorageException.class,
                    () -> repository.importLegacyJson(
                            List.of(first, conflicting), "d".repeat(64)));

            assertTrue(repository.importLegacyJson(List.of(first), "e".repeat(64)));
            PlayerSkin claimed = repository.loadOrAdopt(UUID.randomUUID(), "FirstPlayer");
            assertEquals("DIAMOND_BLOCK", claimed.currentSkin.material);
        }
    }

    @Test
    void reusedNameCannotStealLegacyClaimedByPermanentAccountUuid() {
        try (JdbcSkinRepository repository = repository("claim-conflict.db")) {
            PlayerSkin legacy = legacy("OwnedName", "DIAMOND_BLOCK");
            repository.importLegacyJson(List.of(new LegacySkinRecord(
                    "OwnedName.json", HASH, legacy)), MANIFEST);
            UUID owner = UUID.randomUUID();
            repository.loadOrAdopt(owner, "OwnedName");

            UUID newOwnerOfReusedName = UUID.randomUUID();
            PlayerSkin fresh = repository.loadOrAdopt(newOwnerOfReusedName, "OwnedName");
            assertEquals(List.of("CUT_SANDSTONE"), materials(fresh));
            assertEquals(Set.of(owner, newOwnerOfReusedName), repository.loadAll().keySet());
        }
    }

    @Test
    void reusedOldNameStartsDefaultAfterPermanentOwnerRenames() {
        try (JdbcSkinRepository repository = repository("renamed-claim-reuse.db")) {
            repository.importLegacyJson(List.of(new LegacySkinRecord(
                    "OldName.json", HASH, legacy("OldName", "DIAMOND_BLOCK"))), MANIFEST);
            UUID originalOwner = UUID.randomUUID();
            repository.loadOrAdopt(originalOwner, "OldName");
            repository.loadOrAdopt(originalOwner, "NewName");

            UUID newOwner = UUID.randomUUID();
            PlayerSkin fresh = repository.loadOrAdopt(newOwner, "OldName");

            assertEquals(List.of("CUT_SANDSTONE"), materials(fresh));
            assertEquals(List.of("CUT_SANDSTONE", "DIAMOND_BLOCK"),
                    materials(repository.loadAll().get(originalOwner)));
            assertEquals(Set.of(originalOwner, newOwner), repository.loadAll().keySet());
        }
    }

    @Test
    void existingUuidNeverReclaimsLegacyOrResurrectsRemovedSkin() {
        try (JdbcSkinRepository repository = repository("direct-authority.db")) {
            PlayerSkin legacy = legacy("StableUser", "DIAMOND_BLOCK");
            repository.importLegacyJson(List.of(new LegacySkinRecord(
                    "StableUser.json", HASH, legacy)), MANIFEST);
            UUID profileUuid = UUID.randomUUID();
            PlayerSkin claimed = repository.loadOrAdopt(profileUuid, "StableUser");
            claimed.allSkin.remove(new SkinSet("DIAMOND_BLOCK"));
            claimed.currentSkin = new SkinSet("CUT_SANDSTONE");
            repository.save(claimed);

            PlayerSkin relogged = repository.loadOrAdopt(profileUuid, "StableUser");
            assertEquals(List.of("CUT_SANDSTONE"), materials(relogged));
        }
    }

    @Test
    void existingUuidRenameCannotClaimAnotherNamesLegacySkin() {
        try (JdbcSkinRepository repository = repository("rename-authority.db")) {
            PlayerSkin legacy = legacy("HistoricalName", "DIAMOND_BLOCK");
            repository.importLegacyJson(List.of(new LegacySkinRecord(
                    "HistoricalName.json", HASH, legacy)), MANIFEST);
            UUID establishedUuid = UUID.randomUUID();
            repository.loadOrAdopt(establishedUuid, "CurrentName");

            PlayerSkin renamed = repository.loadOrAdopt(establishedUuid, "HistoricalName");
            assertEquals(List.of("CUT_SANDSTONE"), materials(renamed));

            UUID rightfulFirstClaim = UUID.randomUUID();
            PlayerSkin claimed = repository.loadOrAdopt(rightfulFirstClaim, "HistoricalName");
            assertEquals(List.of("CUT_SANDSTONE", "DIAMOND_BLOCK"), materials(claimed));
        }
    }

    @Test
    void caseChangedUniversalAuthUpgradeCannotResurrectOfflineCache() {
        try (JdbcSkinRepository repository = repository("case-upgrade.db")) {
            String legacyName = "CasePlayer";
            PlayerSkin legacy = legacy(legacyName, "DIAMOND_BLOCK");
            repository.importLegacyJson(List.of(new LegacySkinRecord(
                    legacyName + ".json", HASH, legacy)), MANIFEST);
            UUID offlineUuid = SkinDataSanitizer.offlineUuid(legacyName);
            repository.loadOrAdopt(offlineUuid, legacyName);

            SkinService service = new SkinService(
                    repository, Logger.getAnonymousLogger(), new HashMap<>());
            UUID profileUuid = UUID.randomUUID();
            service.getOrCreate(profileUuid, "caseplayer");
            assertEquals(1, service.allLoaded().size());
            assertEquals(profileUuid.toString(), service.findByName("CASEPLAYER").uuid);

            for (PlayerSkin snapshot : service.allLoaded()) {
                service.save(snapshot);
            }
            assertEquals(Set.of(profileUuid), repository.loadAll().keySet());
        }
    }

    @Test
    void legacySurvivesOfflineCaseChangeThenUniversalAuthUpgrade() {
        try (JdbcSkinRepository repository = repository("three-stage-case-upgrade.db")) {
            String originalName = "CasePlayer";
            repository.importLegacyJson(List.of(new LegacySkinRecord(
                    originalName + ".json", HASH,
                    legacy(originalName, "DIAMOND_BLOCK"))), MANIFEST);

            UUID firstOffline = SkinDataSanitizer.offlineUuid(originalName);
            repository.loadOrAdopt(firstOffline, originalName);

            String changedCase = "caseplayer";
            UUID secondOffline = SkinDataSanitizer.offlineUuid(changedCase);
            PlayerSkin caseMigrated = repository.loadOrAdopt(secondOffline, changedCase);
            caseMigrated.allSkin.add(new SkinSet("GOLD_BLOCK"));
            caseMigrated.currentSkin = new SkinSet("GOLD_BLOCK");
            repository.save(caseMigrated);

            UUID profileUuid = UUID.randomUUID();
            PlayerSkin authenticated = repository.loadOrAdopt(profileUuid, changedCase);
            assertEquals("GOLD_BLOCK", authenticated.currentSkin.material);
            assertEquals(List.of("CUT_SANDSTONE", "DIAMOND_BLOCK", "GOLD_BLOCK"),
                    materials(authenticated));
            assertEquals(Set.of(profileUuid), repository.loadAll().keySet());
        }
    }

    private JdbcSkinRepository repository(String fileName) {
        return new JdbcSkinRepository(
                "jdbc:sqlite:" + temporaryDirectory.resolve(fileName),
                new Properties(), Logger.getAnonymousLogger());
    }

    private static PlayerSkin legacy(String name, String selected) {
        LinkedHashSet<SkinSet> owned = new LinkedHashSet<>();
        owned.add(new SkinSet("CUT_SANDSTONE"));
        owned.add(new SkinSet(selected));
        return new PlayerSkin(name,
                SkinDataSanitizer.offlineUuid(name).toString(),
                new SkinSet(selected), owned);
    }

    private static List<String> materials(PlayerSkin skin) {
        return skin.allSkin.stream().map(entry -> entry.material).toList();
    }

    private record TestPlayerIdentity(UUID uuid, String name) {
        org.bukkit.entity.Player asPlayer() {
            return (org.bukkit.entity.Player) java.lang.reflect.Proxy.newProxyInstance(
                    getClass().getClassLoader(),
                    new Class<?>[]{org.bukkit.entity.Player.class},
                    (proxy, method, args) -> switch (method.getName()) {
                        case "getUniqueId" -> uuid;
                        case "getName" -> name;
                        case "hashCode" -> uuid.hashCode();
                        case "equals" -> proxy == args[0];
                        case "toString" -> "TestPlayer[" + name + ']';
                        default -> throw new UnsupportedOperationException(method.getName());
                    });
        }
    }
}
