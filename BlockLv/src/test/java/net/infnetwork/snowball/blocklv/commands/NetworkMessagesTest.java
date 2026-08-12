package net.infnetwork.snowball.blocklv.commands;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

class NetworkMessagesTest {
    @Test
    void formatsExactNetworkPrefixAndMessageColors() {
        assertEquals("§bI§en§cf §bNetwork §e>> §a完成",
                NetworkMessages.format("&a完成"));
    }
}
