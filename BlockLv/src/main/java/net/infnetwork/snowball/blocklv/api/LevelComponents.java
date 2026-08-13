package net.infnetwork.snowball.blocklv.api;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.TextColor;
import net.kyori.adventure.text.format.TextDecoration;

public final class LevelComponents {
    private static final int GRAY = 0xA0A0A0;
    private static final int GREEN = 0x55D68A;
    private static final int BLUE = 0x4DA3FF;
    private static final int PURPLE = 0x8B5CF6;
    private static final int YELLOW = 0xFFD54A;
    private static final int ORANGE = 0xFF6B35;
    private static final TextColor NEUTRAL = TextColor.color(GRAY);

    private LevelComponents() {
    }

    /**
     * Renders the decimal level and its star as one continuous colour unit.
     */
    public static Component level(long level) {
        long normalized = Math.max(0L, level);
        String text = normalized + "✫";
        int start;
        int end;
        if (normalized < 100L) {
            start = GRAY;
            end = GRAY;
        } else if (normalized < 200L) {
            start = GREEN;
            end = GREEN;
        } else if (normalized < 300L) {
            start = BLUE;
            end = PURPLE;
        } else {
            start = YELLOW;
            end = ORANGE;
        }

        Component result = Component.empty();
        int lastIndex = text.length() - 1;
        for (int index = 0; index < text.length(); index++) {
            int color = interpolate(start, end, index, lastIndex);
            result = result.append(Component.text(
                    String.valueOf(text.charAt(index)), TextColor.color(color)));
        }
        return withoutInheritedDecorations(result);
    }

    public static Component badge(long level) {
        return withoutInheritedDecorations(Component.empty()
                .append(Component.text("[", NEUTRAL))
                .append(level(level))
                .append(Component.text("]", NEUTRAL)));
    }

    private static int interpolate(int start, int end, int index, int lastIndex) {
        if (lastIndex <= 0 || start == end) {
            return start;
        }
        double progress = (double) index / (double) lastIndex;
        int red = interpolateChannel(start >> 16, end >> 16, progress);
        int green = interpolateChannel(start >> 8, end >> 8, progress);
        int blue = interpolateChannel(start, end, progress);
        return red << 16 | green << 8 | blue;
    }

    private static int interpolateChannel(int start, int end, double progress) {
        return (int) Math.round((start & 0xFF) + ((end & 0xFF) - (start & 0xFF)) * progress);
    }

    private static Component withoutInheritedDecorations(Component component) {
        return component
                .decoration(TextDecoration.BOLD, false)
                .decoration(TextDecoration.ITALIC, false);
    }
}
