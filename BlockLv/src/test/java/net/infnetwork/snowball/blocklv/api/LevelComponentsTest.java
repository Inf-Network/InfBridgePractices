package net.infnetwork.snowball.blocklv.api;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

import java.util.Arrays;
import java.util.List;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.TextColor;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import org.junit.jupiter.api.Test;

class LevelComponentsTest {
    private static final PlainTextComponentSerializer PLAIN =
            PlainTextComponentSerializer.plainText();

    @Test
    void usesExactSolidColorsAtTierBoundaries() {
        assertColors(-1L, 0xA0A0A0, 0xA0A0A0);
        assertColors(0L, 0xA0A0A0, 0xA0A0A0);
        assertColors(99L, 0xA0A0A0, 0xA0A0A0, 0xA0A0A0);
        assertColors(100L, 0x55D68A, 0x55D68A, 0x55D68A, 0x55D68A);
        assertColors(199L, 0x55D68A, 0x55D68A, 0x55D68A, 0x55D68A);
    }

    @Test
    void interpolatesBluePurpleAndYellowOrangeAcrossDigits() {
        assertColors(200L, 0x4DA3FF, 0x628BFC, 0x7674F9, 0x8B5CF6);
        assertColors(299L, 0x4DA3FF, 0x628BFC, 0x7674F9, 0x8B5CF6);
        assertColors(300L, 0xFFD54A, 0xFFB243, 0xFF8E3C, 0xFF6B35);
        assertColors(1000L, 0xFFD54A, 0xFFBB45, 0xFFA040, 0xFF863A, 0xFF6B35);
        assertColors(1001L, 0xFFD54A, 0xFFBB45, 0xFFA040, 0xFF863A, 0xFF6B35);
    }

    @Test
    void badgeKeepsNeutralFrameAndDoesNotContainFormattingMarkup() {
        Component badge = LevelComponents.badge(666L);

        assertEquals("[666✫]", PLAIN.serialize(badge));
        assertEquals(TextColor.color(0xA0A0A0), badge.children().get(0).color());
        assertEquals(LevelComponents.level(666L), badge.children().get(1));
        assertEquals(TextColor.color(0xA0A0A0), badge.children().get(2).color());
        assertFalse(PLAIN.serialize(badge).contains("<"));
        assertFalse(PLAIN.serialize(badge).contains("&"));
    }

    @Test
    void keepsTheStarAsTheGradientEndpointForEveryLongLevel() {
        Component display = LevelComponents.level(Long.MAX_VALUE);

        assertEquals(TextColor.color(0xFFD54A), display.children().getFirst().color());
        assertEquals(TextColor.color(0xFF6B35), display.children().getLast().color());
        assertEquals(Long.MAX_VALUE + "✫", PLAIN.serialize(display));
    }

    private static void assertColors(long level, int... expected) {
        Component display = LevelComponents.level(level);
        List<Integer> actual = display.children().stream()
                .map(Component::color)
                .map(TextColor::value)
                .toList();
        assertEquals(Arrays.stream(expected).boxed().toList(), actual);
    }
}
