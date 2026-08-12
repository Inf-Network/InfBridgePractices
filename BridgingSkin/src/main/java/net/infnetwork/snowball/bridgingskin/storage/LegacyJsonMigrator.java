package net.infnetwork.snowball.bridgingskin.storage;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import java.io.File;
import java.io.IOException;
import java.math.BigDecimal;
import java.nio.ByteBuffer;
import java.nio.charset.CharacterCodingException;
import java.nio.charset.CodingErrorAction;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import java.util.logging.Logger;
import java.util.stream.Stream;
import net.infnetwork.snowball.bridgingskin.data.PlayerSkin;
import net.infnetwork.snowball.bridgingskin.data.SkinDataSanitizer;
import net.infnetwork.snowball.bridgingskin.data.SkinSet;

/** One-time, fail-closed import. Source JSON files are never modified or deleted. */
public final class LegacyJsonMigrator {
    private static final Set<String> ROOT_KEYS = Set.of(
            "uuid", "player", "currentSelected", "allSkins");
    private static final Set<String> SKIN_KEYS = Set.of("Material");
    private static final Set<String> SKIN_KEYS_WITH_DATA = Set.of("Material", "Data");
    private static final long MAX_FILE_BYTES = 1024L * 1024L;
    private static final long MAX_TOTAL_BYTES = 64L * 1024L * 1024L;

    private final JdbcSkinRepository repository;
    private final Logger logger;

    /** Gson remains in the signature for source compatibility with the existing bootstrap. */
    public LegacyJsonMigrator(JdbcSkinRepository repository, Gson gson, Logger logger) {
        this.repository = Objects.requireNonNull(repository, "repository");
        Objects.requireNonNull(gson, "gson");
        this.logger = Objects.requireNonNull(logger, "logger");
    }

    /**
     * Reads and validates the complete directory before starting the database transaction.
     * Returns zero when the exact same manifest was already imported.
     */
    public int migrateIfNeeded(File skinDirectory) {
        Path directory = Objects.requireNonNull(skinDirectory, "skinDirectory").toPath();
        if (!Files.isDirectory(directory, LinkOption.NOFOLLOW_LINKS)) {
            throw new SkinStorageException("无法读取旧皮肤目录 " + directory);
        }

        List<Path> files = listJsonFiles(directory);
        List<LegacySkinRecord> records = new ArrayList<>(files.size());
        Set<UUID> uuids = new HashSet<>();
        Set<String> normalizedNames = new HashSet<>();
        Set<String> normalizedFiles = new HashSet<>();
        long totalBytes = 0L;

        for (Path file : files) {
            try {
                if (!Files.isRegularFile(file, LinkOption.NOFOLLOW_LINKS)) {
                    throw new IllegalArgumentException("JSON 不是普通文件（不接受符号链接）");
                }
                long size = Files.size(file);
                if (size > MAX_FILE_BYTES || totalBytes + size > MAX_TOTAL_BYTES) {
                    throw new IllegalArgumentException("JSON 文件过大");
                }
                totalBytes += size;
                byte[] bytes = Files.readAllBytes(file);
                String sourceFile = file.getFileName().toString();
                String expectedName = sourceFile.substring(0, sourceFile.length() - 5);
                PlayerSkin skin = parseStrict(bytes, expectedName);

                UUID uuid = UUID.fromString(skin.uuid);
                if (!uuids.add(uuid)) {
                    throw new IllegalArgumentException("发现重复 UUID: " + uuid);
                }
                if (!normalizedNames.add(normalize(skin.player))) {
                    throw new IllegalArgumentException("发现大小写不敏感的重复玩家名: " + skin.player);
                }
                if (!normalizedFiles.add(normalize(sourceFile))) {
                    throw new IllegalArgumentException("发现大小写不敏感的重复文件名: " + sourceFile);
                }
                records.add(new LegacySkinRecord(sourceFile, sha256Hex(bytes), skin));
            } catch (Exception exception) {
                throw new SkinStorageException(
                        "旧皮肤文件无法安全迁移: " + file.getFileName(), exception);
            }
        }

        String manifest = manifestSha256(records);
        boolean imported = repository.importLegacyJson(records, manifest);
        if (!imported) {
            logger.info("BridgingSkin 旧 JSON 清单已迁移，校验一致；跳过重复导入");
            return 0;
        }
        logger.info("BridgingSkin 已把 " + records.size()
                + " 份 JSON 安全导入 legacy staging；原 JSON 保持原样");
        return records.size();
    }

    private static List<Path> listJsonFiles(Path directory) {
        try (Stream<Path> entries = Files.list(directory)) {
            return entries
                    .filter(path -> path.getFileName().toString()
                            .toLowerCase(Locale.ROOT).endsWith(".json"))
                    .sorted(Comparator
                            .comparing((Path path) -> path.getFileName().toString(),
                                    String.CASE_INSENSITIVE_ORDER)
                            .thenComparing(path -> path.getFileName().toString()))
                    .toList();
        } catch (IOException exception) {
            throw new SkinStorageException("无法枚举旧皮肤目录 " + directory, exception);
        }
    }

    private static PlayerSkin parseStrict(byte[] bytes, String expectedPlayerName) {
        String json = decodeUtf8(bytes);
        JsonElement rootElement = JsonParser.parseString(json);
        if (!rootElement.isJsonObject()) {
            throw new IllegalArgumentException("JSON 根节点必须是对象");
        }
        JsonObject root = rootElement.getAsJsonObject();
        if (!root.keySet().equals(ROOT_KEYS)) {
            throw new IllegalArgumentException("JSON 根字段不匹配: " + root.keySet());
        }

        String uuid = requiredString(root, "uuid");
        String player = requiredString(root, "player");
        String selected = readSkin(root.get("currentSelected"), "currentSelected");
        JsonElement ownedElement = root.get("allSkins");
        if (ownedElement == null || !ownedElement.isJsonArray()) {
            throw new IllegalArgumentException("allSkins 必须是数组");
        }
        JsonArray ownedArray = ownedElement.getAsJsonArray();
        if (ownedArray.isEmpty()) {
            throw new IllegalArgumentException("allSkins 不能为空");
        }

        LinkedHashSet<SkinSet> owned = new LinkedHashSet<>();
        Set<String> materialNames = new HashSet<>();
        for (int index = 0; index < ownedArray.size(); index++) {
            String material = readSkin(ownedArray.get(index), "allSkins[" + index + "]");
            if (!materialNames.add(material)) {
                throw new IllegalArgumentException("allSkins 包含重复方块: " + material);
            }
            owned.add(new SkinSet(material));
        }
        if (!materialNames.contains(selected)) {
            throw new IllegalArgumentException("currentSelected 不在 allSkins 中: " + selected);
        }

        PlayerSkin parsed = new PlayerSkin(player, uuid, new SkinSet(selected), owned);
        return SkinDataSanitizer.requireLegacy(parsed, expectedPlayerName);
    }

    private static String readSkin(JsonElement element, String path) {
        if (element == null || !element.isJsonObject()) {
            throw new IllegalArgumentException(path + " 必须是对象");
        }
        JsonObject object = element.getAsJsonObject();
        if (!object.keySet().equals(SKIN_KEYS)
                && !object.keySet().equals(SKIN_KEYS_WITH_DATA)) {
            throw new IllegalArgumentException(path + " 字段不匹配: " + object.keySet());
        }
        String material = requiredString(object, "Material");
        if (!object.has("Data")) {
            return SkinDataSanitizer.requireMaterialName(material);
        }

        JsonElement dataElement = object.get("Data");
        if (!dataElement.isJsonPrimitive() || !dataElement.getAsJsonPrimitive().isNumber()) {
            throw new IllegalArgumentException(path + ".Data 必须是整数");
        }
        BigDecimal decimal = dataElement.getAsBigDecimal().stripTrailingZeros();
        if (decimal.scale() > 0) {
            throw new IllegalArgumentException(path + ".Data 必须是整数");
        }
        int data = decimal.intValueExact();
        if (material.equalsIgnoreCase("SANDSTONE") && data == 2) {
            return SkinDataSanitizer.DEFAULT_MATERIAL;
        }
        if (data != 0) {
            throw new IllegalArgumentException(
                    path + " 包含无法无损迁移的旧 data 值: " + material + ':' + data);
        }
        return SkinDataSanitizer.requireMaterialName(material);
    }

    private static String requiredString(JsonObject object, String key) {
        JsonElement element = object.get(key);
        if (element == null || !element.isJsonPrimitive()
                || !element.getAsJsonPrimitive().isString()) {
            throw new IllegalArgumentException(key + " 必须是字符串");
        }
        String value = element.getAsString();
        if (value.isBlank()) {
            throw new IllegalArgumentException(key + " 不能为空");
        }
        return value;
    }

    private static String decodeUtf8(byte[] bytes) {
        try {
            return StandardCharsets.UTF_8.newDecoder()
                    .onMalformedInput(CodingErrorAction.REPORT)
                    .onUnmappableCharacter(CodingErrorAction.REPORT)
                    .decode(ByteBuffer.wrap(bytes))
                    .toString();
        } catch (CharacterCodingException exception) {
            throw new IllegalArgumentException("文件不是有效 UTF-8", exception);
        }
    }

    private static String manifestSha256(List<LegacySkinRecord> records) {
        MessageDigest digest = sha256();
        for (LegacySkinRecord record : records) {
            digest.update(record.sourceFile().getBytes(StandardCharsets.UTF_8));
            digest.update((byte) 0);
            digest.update(record.sourceSha256().getBytes(StandardCharsets.US_ASCII));
            digest.update((byte) '\n');
        }
        return hex(digest.digest());
    }

    private static String sha256Hex(byte[] bytes) {
        return hex(sha256().digest(bytes));
    }

    private static MessageDigest sha256() {
        try {
            return MessageDigest.getInstance("SHA-256");
        } catch (NoSuchAlgorithmException impossible) {
            throw new IllegalStateException("JVM 缺少 SHA-256", impossible);
        }
    }

    private static String hex(byte[] bytes) {
        StringBuilder result = new StringBuilder(bytes.length * 2);
        for (byte value : bytes) {
            result.append(Character.forDigit((value >>> 4) & 0xf, 16));
            result.append(Character.forDigit(value & 0xf, 16));
        }
        return result.toString();
    }

    private static String normalize(String value) {
        return value.toLowerCase(Locale.ROOT);
    }
}
