package net.infnetwork.snowball.bridgingskin;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.logging.Logger;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import net.infnetwork.snowball.bridgingskin.data.PlayerSkin;
import net.infnetwork.snowball.bridgingskin.data.SkinDataSanitizer;
import net.infnetwork.snowball.bridgingskin.data.SkinSet;
import net.infnetwork.snowball.bridgingskin.storage.JdbcSkinRepository;

/** UUID-keyed, write-through skin service with detached database commits. */
public final class SkinService {
    private final JdbcSkinRepository repository;
    private final Logger logger;
    private final Map<UUID, PlayerSkin> byUuid = new LinkedHashMap<>();
    private final Map<String, LinkedHashSet<UUID>> byNormalizedName = new HashMap<>();
    private final Map<UUID, String> indexedDisplayNames = new HashMap<>();
    private final Map<UUID, String> persistedFingerprints = new HashMap<>();
    private final Set<UUID> resolvedIdentities = new LinkedHashSet<>();
    private final HashMap<String, PlayerSkin> compatibilityNameCache;

    public SkinService(
            JdbcSkinRepository repository,
            Logger logger,
            HashMap<String, PlayerSkin> compatibilityNameCache
    ) {
        this.repository = java.util.Objects.requireNonNull(repository, "repository");
        this.logger = java.util.Objects.requireNonNull(logger, "logger");
        this.compatibilityNameCache = java.util.Objects.requireNonNull(
                compatibilityNameCache, "compatibilityNameCache");
        repository.loadAll().forEach((uuid, skin) -> putCache(uuid, skin, true));
    }

    public synchronized PlayerSkin getOrCreate(Player player) {
        return getOrCreate(player.getUniqueId(), player.getName());
    }

    public synchronized PlayerSkin getOrCreate(UUID authenticatedUuid, String playerName) {
        SkinDataSanitizer.validatePlayerName(playerName);
        PlayerSkin cached = byUuid.get(authenticatedUuid);
        if (cached != null && resolvedIdentities.contains(authenticatedUuid)
                && cached.player.equals(playerName)) {
            return cached;
        }

        JdbcSkinRepository.IdentityLoad identity = repository.loadIdentity(
                authenticatedUuid, playerName);
        if (identity.retiredUuid() != null) {
            removeCache(identity.retiredUuid());
            resolvedIdentities.remove(identity.retiredUuid());
        }
        removeCache(authenticatedUuid);
        putCache(authenticatedUuid, identity.skin(), true);
        resolvedIdentities.add(authenticatedUuid);
        logger.fine(() -> "BridgingSkin 身份已就绪: " + playerName + " / " + authenticatedUuid);
        return byUuid.get(authenticatedUuid);
    }

    /** Name is display metadata, so ambiguous normalized names always fail closed. */
    public synchronized PlayerSkin findByName(String playerName) {
        if (playerName == null) {
            return null;
        }
        LinkedHashSet<UUID> ids = byNormalizedName.get(normalize(playerName));
        if (ids == null || ids.size() != 1) {
            return null;
        }
        return byUuid.get(ids.iterator().next());
    }

    /** Saves a detached copy first and replaces the live cache only after commit succeeds. */
    public synchronized void save(PlayerSkin rawSkin) {
        PlayerSkin candidate = SkinDataSanitizer.sanitize(rawSkin);
        UUID uuid = UUID.fromString(candidate.uuid);
        String fingerprint = fingerprint(candidate);
        if (fingerprint.equals(persistedFingerprints.get(uuid))) {
            return;
        }
        PlayerSkin persisted = repository.save(candidate);
        replaceCache(uuid, persisted, true);
    }

    /** Grants the entire reward list in one repository transaction. */
    public synchronized List<UnlockResult> unlock(Player player, List<Material> rewards) {
        PlayerSkin current = getOrCreate(player);
        PlayerSkin candidate = detachedCopy(current);
        List<UnlockResult> results = new ArrayList<>(rewards.size());
        for (Material material : rewards) {
            String valid = SkinDataSanitizer.requireMaterialName(
                    java.util.Objects.requireNonNull(material, "reward material").name());
            boolean added = candidate.allSkin.add(new SkinSet(valid));
            results.add(new UnlockResult(material, added));
        }
        if (results.stream().noneMatch(UnlockResult::newlyUnlocked)) {
            return List.copyOf(results);
        }
        PlayerSkin persisted = repository.save(candidate);
        replaceCache(player.getUniqueId(), persisted, true);
        return List.copyOf(results);
    }

    public synchronized boolean select(Player player, Material material) {
        PlayerSkin current = getOrCreate(player);
        SkinSet selected = new SkinSet(SkinDataSanitizer.requireMaterialName(material.name()));
        if (!current.allSkin.contains(selected)) {
            return false;
        }
        if (current.currentSkin.equals(selected)) {
            return true;
        }
        PlayerSkin candidate = detachedCopy(current);
        candidate.currentSkin = selected;
        PlayerSkin persisted = repository.save(candidate);
        replaceCache(player.getUniqueId(), persisted, true);
        return true;
    }

    public synchronized Set<Material> ownedMaterials(Player player) {
        PlayerSkin skin = getOrCreate(player);
        LinkedHashSet<Material> materials = new LinkedHashSet<>();
        for (SkinSet entry : skin.allSkin) {
            Material material = Material.matchMaterial(entry.material);
            if (material != null) {
                materials.add(material);
            }
        }
        return Collections.unmodifiableSet(materials);
    }

    public synchronized void clearMaterialGlobally(Material material) {
        if (material == Material.CUT_SANDSTONE) {
            throw new IllegalArgumentException("不能清除默认皮肤 CUT_SANDSTONE");
        }
        repository.clearMaterialGlobally(
                material.name(), SkinDataSanitizer.DEFAULT_MATERIAL);
        List<Map.Entry<UUID, PlayerSkin>> snapshots = new ArrayList<>(byUuid.entrySet());
        for (Map.Entry<UUID, PlayerSkin> entry : snapshots) {
            PlayerSkin candidate = detachedCopy(entry.getValue());
            candidate.allSkin.remove(new SkinSet(material.name()));
            if (candidate.currentSkin.material.equals(material.name())) {
                candidate.currentSkin = new SkinSet(SkinDataSanitizer.DEFAULT_MATERIAL);
            }
            replaceCache(entry.getKey(), SkinDataSanitizer.sanitize(candidate), true);
        }
    }

    /** Defensive snapshots for the legacy periodic compatibility flush. */
    public synchronized Collection<PlayerSkin> allLoaded() {
        List<PlayerSkin> snapshots = new ArrayList<>(byUuid.size());
        for (PlayerSkin skin : byUuid.values()) {
            snapshots.add(detachedCopy(skin));
        }
        return List.copyOf(snapshots);
    }

    private void replaceCache(UUID uuid, PlayerSkin skin, boolean persisted) {
        removeCache(uuid);
        putCache(uuid, skin, persisted);
    }

    private void putCache(UUID uuid, PlayerSkin rawSkin, boolean persisted) {
        PlayerSkin skin = SkinDataSanitizer.requirePersistable(rawSkin);
        byUuid.put(uuid, skin);
        compatibilityNameCache.put(skin.player, skin);
        indexedDisplayNames.put(uuid, skin.player);
        byNormalizedName.computeIfAbsent(normalize(skin.player), ignored -> new LinkedHashSet<>())
                .add(uuid);
        if (persisted) {
            persistedFingerprints.put(uuid, fingerprint(skin));
        }
    }

    private void removeCache(UUID uuid) {
        PlayerSkin removed = byUuid.remove(uuid);
        if (removed == null) {
            return;
        }
        String indexedName = indexedDisplayNames.remove(uuid);
        if (indexedName == null) {
            indexedName = removed.player;
        }
        compatibilityNameCache.remove(indexedName, removed);
        String normalized = normalize(indexedName);
        LinkedHashSet<UUID> ids = byNormalizedName.get(normalized);
        if (ids != null) {
            ids.remove(uuid);
            if (ids.isEmpty()) {
                byNormalizedName.remove(normalized);
            }
        }
        persistedFingerprints.remove(uuid);
    }

    private static PlayerSkin detachedCopy(PlayerSkin skin) {
        PlayerSkin sanitized = SkinDataSanitizer.sanitize(skin);
        LinkedHashSet<SkinSet> owned = new LinkedHashSet<>();
        for (SkinSet entry : sanitized.allSkin) {
            owned.add(new SkinSet(entry.material));
        }
        return new PlayerSkin(sanitized.player, sanitized.uuid,
                new SkinSet(sanitized.currentSkin.material), owned);
    }

    private static String fingerprint(PlayerSkin skin) {
        StringBuilder value = new StringBuilder(skin.player)
                .append('\u0000').append(skin.currentSkin.material);
        for (SkinSet entry : skin.allSkin) {
            value.append('\u0000').append(entry.material);
        }
        return value.toString();
    }

    private static String normalize(String name) {
        return name.toLowerCase(Locale.ROOT);
    }

    public record UnlockResult(Material material, boolean newlyUnlocked) {
    }
}
