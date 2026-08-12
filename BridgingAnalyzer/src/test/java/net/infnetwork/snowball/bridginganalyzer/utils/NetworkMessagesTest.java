package net.infnetwork.snowball.bridginganalyzer.utils;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

class NetworkMessagesTest {
    @Test
    void legacyFormatRetainsTheConfiguredColoredPrefix() {
        assertEquals(
                "§bI§en§cf §bNetwork §e>> §c错误",
                NetworkMessages.format("&c错误"));
    }
}
