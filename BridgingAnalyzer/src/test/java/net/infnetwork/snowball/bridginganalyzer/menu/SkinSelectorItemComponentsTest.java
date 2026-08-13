package net.infnetwork.snowball.bridginganalyzer.menu;

import static org.junit.jupiter.api.Assertions.assertEquals;

import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import org.junit.jupiter.api.Test;

class SkinSelectorItemComponentsTest {
    @Test
    void usesNativeComponentsForTheNewMenuItem() {
        assertEquals(
                "方块皮肤选择器",
                PlainTextComponentSerializer.plainText().serialize(
                        SkinSelectorItemComponents.displayName()));
        assertEquals(NamedTextColor.GOLD, SkinSelectorItemComponents.displayName().color());
        assertEquals(
                TextDecoration.State.FALSE,
                SkinSelectorItemComponents.displayName().decoration(TextDecoration.ITALIC));
        assertEquals(
                NamedTextColor.YELLOW,
                SkinSelectorItemComponents.lore().getLast().color());
    }
}
