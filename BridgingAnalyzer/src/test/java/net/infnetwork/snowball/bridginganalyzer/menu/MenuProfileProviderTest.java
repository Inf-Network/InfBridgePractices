package net.infnetwork.snowball.bridginganalyzer.menu;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

class MenuProfileProviderTest {
    @Test
    void preservesThreeStringRecordContract() {
        MenuProfileProvider.ProfileSnapshot profile =
                new MenuProfileProvider.ProfileSnapshot("玩家", "12", "100");

        assertEquals("12", profile.level());
        assertEquals("玩家", profile.group());
        assertEquals("100", profile.balance());
    }
}
