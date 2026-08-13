package net.infnetwork.snowball.blocklv.commands;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.List;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import org.junit.jupiter.api.Test;

class NetworkMessagesTest {
    @Test
    void composesExactNativeNetworkPrefixAndMessageColors() {
        Component formatted = NetworkMessages.compose(
                Component.text("完成", NamedTextColor.GREEN));

        assertEquals(
                "Inf Network >> 完成",
                PlainTextComponentSerializer.plainText().serialize(formatted));
        assertEquals(
                List.of(
                        NamedTextColor.AQUA,
                        NamedTextColor.YELLOW,
                        NamedTextColor.RED,
                        NamedTextColor.AQUA,
                        NamedTextColor.YELLOW,
                        NamedTextColor.GREEN),
                formatted.children().stream().map(Component::color).toList());
    }
}
