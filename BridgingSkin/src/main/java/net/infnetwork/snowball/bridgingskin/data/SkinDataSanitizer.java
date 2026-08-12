package net.infnetwork.snowball.bridgingskin.data;

import java.nio.charset.StandardCharsets;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.UUID;
import java.util.regex.Pattern;
import org.bukkit.Material;
import net.infnetwork.snowball.bridgingskin.IllegalMaterial;

/**
 * Validation and defensive-copy boundary for skin data.
 *
 * <p>Runtime callers historically mutate {@link PlayerSkin}'s public fields. The storage layer
 * must therefore never retain or write a caller-owned instance directly.</p>
 */
public final class SkinDataSanitizer {
    public static final String DEFAULT_MATERIAL = "CUT_SANDSTONE";

    private static final Pattern PLAYER_NAME = Pattern.compile("[A-Za-z0-9_]{1,16}");

    private SkinDataSanitizer() {
    }

    /**
     * Produces a persistable defensive copy for the compatibility API.
     *
     * <p>Invalid material entries are discarded, while the default and selected materials are
     * guaranteed to be owned. Identity errors remain fatal.</p>
     */
    public static PlayerSkin sanitize(PlayerSkin source) {
        requireIdentity(source);
        UUID uuid = parseCanonicalUuid(source.uuid);
        LinkedHashSet<String> materials = new LinkedHashSet<>();
        materials.add(DEFAULT_MATERIAL);
        if (source.allSkin != null) {
            for (SkinSet skin : source.allSkin) {
                String material = validMaterialName(skin == null ? null : skin.material);
                if (material != null) {
                    materials.add(material);
                }
            }
        }

        String selected = source.currentSkin == null
                ? null
                : validMaterialName(source.currentSkin.material);
        if (selected == null) {
            selected = DEFAULT_MATERIAL;
        }
        materials.add(selected);
        return create(source.player, uuid, selected, materials);
    }

    /** Validates a row read from the database without silently repairing corruption. */
    public static PlayerSkin requirePersistable(PlayerSkin source) {
        requireIdentity(source);
        UUID uuid = parseCanonicalUuid(source.uuid);
        if (source.currentSkin == null || source.allSkin == null || source.allSkin.isEmpty()) {
            throw new IllegalArgumentException("皮肤记录缺少当前皮肤或库存");
        }

        LinkedHashSet<String> materials = strictMaterials(source.allSkin);
        String selected = requireMaterialName(source.currentSkin.material);
        if (!materials.contains(selected)) {
            throw new IllegalArgumentException("当前皮肤不在玩家库存中: " + selected);
        }
        if (!materials.contains(DEFAULT_MATERIAL)) {
            throw new IllegalArgumentException("皮肤库存缺少默认方块 " + DEFAULT_MATERIAL);
        }
        return create(source.player, uuid, selected, materials);
    }

    /**
     * Strictly validates one legacy JSON identity. No material is dropped or added.
     */
    public static PlayerSkin requireLegacy(
            PlayerSkin source,
            String expectedPlayerName
    ) {
        PlayerSkin copy = requirePersistable(source);
        if (!copy.player.equals(expectedPlayerName)) {
            throw new IllegalArgumentException("文件名与 JSON 玩家名不一致: "
                    + expectedPlayerName + " != " + copy.player);
        }
        UUID uuid = UUID.fromString(copy.uuid);
        UUID expected = offlineUuid(copy.player);
        if (!uuid.equals(expected)) {
            throw new IllegalArgumentException("旧 UUID 与 OfflinePlayer 身份不一致: "
                    + copy.player + " / " + uuid);
        }
        return copy;
    }

    public static PlayerSkin copy(PlayerSkin source) {
        return requirePersistable(source);
    }

    public static PlayerSkin copyWithIdentity(
            PlayerSkin source,
            String playerName,
            UUID playerUuid
    ) {
        validatePlayerName(playerName);
        if (playerUuid == null) {
            throw new IllegalArgumentException("玩家 UUID 不能为空");
        }
        PlayerSkin sanitized = source == null
                ? new PlayerSkin(playerName, playerUuid.toString())
                : sanitize(source);
        LinkedHashSet<String> materials = new LinkedHashSet<>();
        for (SkinSet skin : sanitized.allSkin) {
            materials.add(skin.material);
        }
        return create(playerName, playerUuid, sanitized.currentSkin.material, materials);
    }

    public static PlayerSkin create(
            String playerName,
            UUID playerUuid,
            String selectedMaterial,
            Iterable<String> orderedMaterials
    ) {
        validatePlayerName(playerName);
        if (playerUuid == null) {
            throw new IllegalArgumentException("玩家 UUID 不能为空");
        }
        String selected = requireMaterialName(selectedMaterial);
        LinkedHashSet<SkinSet> skins = new LinkedHashSet<>();
        boolean selectedOwned = false;
        boolean defaultOwned = false;
        for (String rawMaterial : orderedMaterials) {
            String material = requireMaterialName(rawMaterial);
            if (!skins.add(new SkinSet(material))) {
                throw new IllegalArgumentException("皮肤库存包含重复方块: " + material);
            }
            selectedOwned |= material.equals(selected);
            defaultOwned |= material.equals(DEFAULT_MATERIAL);
        }
        if (!selectedOwned) {
            throw new IllegalArgumentException("当前皮肤不在玩家库存中: " + selected);
        }
        if (!defaultOwned) {
            throw new IllegalArgumentException("皮肤库存缺少默认方块 " + DEFAULT_MATERIAL);
        }
        return new PlayerSkin(playerName, playerUuid.toString(), new SkinSet(selected), skins);
    }

    public static List<String> orderedMaterialNames(PlayerSkin skin) {
        PlayerSkin copy = requirePersistable(skin);
        return copy.allSkin.stream().map(entry -> entry.material).toList();
    }

    public static String validMaterialName(String rawMaterial) {
        if (rawMaterial == null || rawMaterial.isBlank()) {
            return null;
        }
        Material material = Material.matchMaterial(rawMaterial.trim().toUpperCase(Locale.ROOT));
        // Material#isBlock/isItem are backed by Paper's live registry in 1.21.11.
        // Persistence validation must also run in offline migration tooling, so
        // the gameplay entry points enforce block shape while this boundary
        // validates the stable enum identity and conflict blacklist.
        if (material == null || material == Material.AIR || material.isLegacy()
                || IllegalMaterial.isIllegal(material)) {
            return null;
        }
        return material.name();
    }

    public static String requireMaterialName(String rawMaterial) {
        String material = validMaterialName(rawMaterial);
        if (material == null) {
            throw new IllegalArgumentException("无效的皮肤方块: " + rawMaterial);
        }
        return material;
    }

    public static void validatePlayerName(String playerName) {
        if (playerName == null || !PLAYER_NAME.matcher(playerName).matches()) {
            throw new IllegalArgumentException("无效的 Minecraft 玩家名: " + playerName);
        }
    }

    public static UUID parseCanonicalUuid(String rawUuid) {
        if (rawUuid == null) {
            throw new IllegalArgumentException("玩家 UUID 不能为空");
        }
        UUID uuid = UUID.fromString(rawUuid);
        if (!uuid.toString().equalsIgnoreCase(rawUuid)) {
            throw new IllegalArgumentException("玩家 UUID 不是标准格式: " + rawUuid);
        }
        return uuid;
    }

    public static UUID offlineUuid(String playerName) {
        validatePlayerName(playerName);
        return UUID.nameUUIDFromBytes(
                ("OfflinePlayer:" + playerName).getBytes(StandardCharsets.UTF_8));
    }

    public static boolean isVerifiedOfflineIdentity(UUID uuid, String playerName) {
        if (uuid == null || playerName == null) {
            return false;
        }
        try {
            return uuid.equals(offlineUuid(playerName));
        } catch (IllegalArgumentException ignored) {
            return false;
        }
    }

    private static void requireIdentity(PlayerSkin source) {
        if (source == null) {
            throw new IllegalArgumentException("皮肤记录不能为空");
        }
        validatePlayerName(source.player);
        parseCanonicalUuid(source.uuid);
    }

    private static LinkedHashSet<String> strictMaterials(Iterable<SkinSet> skins) {
        LinkedHashSet<String> materials = new LinkedHashSet<>();
        for (SkinSet skin : skins) {
            if (skin == null) {
                throw new IllegalArgumentException("皮肤库存包含空条目");
            }
            String material = requireMaterialName(skin.material);
            if (!materials.add(material)) {
                throw new IllegalArgumentException("皮肤库存包含重复方块: " + material);
            }
        }
        return materials;
    }
}
