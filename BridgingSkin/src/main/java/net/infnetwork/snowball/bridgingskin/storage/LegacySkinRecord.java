package net.infnetwork.snowball.bridgingskin.storage;

import java.util.Objects;
import net.infnetwork.snowball.bridgingskin.data.PlayerSkin;
import net.infnetwork.snowball.bridgingskin.data.SkinDataSanitizer;

/** Immutable import unit. Accessors return defensive skin copies. */
public final class LegacySkinRecord {
    private final String sourceFile;
    private final String sourceSha256;
    private final PlayerSkin skin;

    public LegacySkinRecord(String sourceFile, String sourceSha256, PlayerSkin skin) {
        this.sourceFile = requireText(sourceFile, "sourceFile");
        this.sourceSha256 = requireSha256(sourceSha256);
        this.skin = SkinDataSanitizer.requirePersistable(skin);
    }

    public String sourceFile() {
        return sourceFile;
    }

    public String sourceSha256() {
        return sourceSha256;
    }

    public PlayerSkin skin() {
        return SkinDataSanitizer.copy(skin);
    }

    private static String requireText(String value, String field) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(field + " 不能为空");
        }
        return value;
    }

    private static String requireSha256(String value) {
        Objects.requireNonNull(value, "sourceSha256");
        if (!value.matches("[0-9a-f]{64}")) {
            throw new IllegalArgumentException("sourceSha256 不是小写 SHA-256");
        }
        return value;
    }
}
