package net.infnetwork.snowball.bridginganalyzer.menu;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.List;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.TextDecoration;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import org.junit.jupiter.api.Test;

class ProfileItemComponentsTest {
    private static final PlainTextComponentSerializer PLAIN =
            PlainTextComponentSerializer.plainText();

    @Test
    void embedsNativeLevelBadgeWithoutParsingFormattingMarkup() {
        MenuProfileProvider.ProfileSnapshot profile =
                new MenuProfileProvider.ProfileSnapshot("玩家", "666", "900");

        List<Component> lore = ProfileItemComponents.lore(profile);

        assertEquals("等级: [666✫]", PLAIN.serialize(lore.get(2)));
        assertEquals("[666✫]", PLAIN.serialize(lore.get(2).children().get(0)));
        assertEquals(TextDecoration.State.FALSE, lore.get(2).decoration(TextDecoration.ITALIC));
        assertEquals(
                TextDecoration.State.FALSE,
                ProfileItemComponents.displayName().decoration(TextDecoration.ITALIC));
    }
}
