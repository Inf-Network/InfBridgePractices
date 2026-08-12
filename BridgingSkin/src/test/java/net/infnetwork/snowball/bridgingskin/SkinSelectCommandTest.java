package net.infnetwork.snowball.bridgingskin;

import static org.junit.jupiter.api.Assertions.assertEquals;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.TextDecoration;
import org.junit.jupiter.api.Test;

class SkinSelectCommandTest {
    @Test
    void itemComponentPreservesUnspecifiedItalicDecoration() {
        Component component = Component.text("皮肤");

        Component result = SkinSelectCommand.itemComponent(component);

        assertEquals(TextDecoration.State.NOT_SET,
                result.decoration(TextDecoration.ITALIC));
    }
}
