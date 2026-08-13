package net.infnetwork.snowball.blocklv.text;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;

public final class LegacyComponentOutput {
    private static final LegacyComponentSerializer SERIALIZER =
            LegacyComponentSerializer.builder()
                    .character(LegacyComponentSerializer.SECTION_CHAR)
                    .hexColors()
                    .useUnusualXRepeatedCharacterHexFormat()
                    .build();

    private LegacyComponentOutput() {
    }

    public static String serialize(Component component) {
        return SERIALIZER.serialize(component);
    }
}
