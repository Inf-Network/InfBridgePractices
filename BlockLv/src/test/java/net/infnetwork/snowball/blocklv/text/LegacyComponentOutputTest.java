package net.infnetwork.snowball.blocklv.text;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

import net.infnetwork.snowball.blocklv.api.LevelComponents;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import org.junit.jupiter.api.Test;

class LegacyComponentOutputTest {
    private static final LegacyComponentSerializer LEGACY =
            LegacyComponentSerializer.builder()
                    .character(LegacyComponentSerializer.SECTION_CHAR)
                    .hexColors()
                    .useUnusualXRepeatedCharacterHexFormat()
                    .build();

    @Test
    void serializesOnlyAtLegacyApiBoundaryAndRoundTripsText() {
        String levelOutput = LegacyComponentOutput.serialize(LevelComponents.level(300L));
        String badgeOutput = LegacyComponentOutput.serialize(LevelComponents.badge(300L));
        Component decodedLevel = LEGACY.deserialize(levelOutput);
        Component decodedBadge = LEGACY.deserialize(badgeOutput);

        assertEquals("300✫", PlainTextComponentSerializer.plainText().serialize(decodedLevel));
        assertEquals("[300✫]", PlainTextComponentSerializer.plainText().serialize(decodedBadge));
        assertFalse(levelOutput.contains("<gradient"));
        assertFalse(levelOutput.contains("&"));
        assertFalse(badgeOutput.contains("<gradient"));
        assertFalse(badgeOutput.contains("&"));
    }
}
