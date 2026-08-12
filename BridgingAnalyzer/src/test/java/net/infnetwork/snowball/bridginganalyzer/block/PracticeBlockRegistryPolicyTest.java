package net.infnetwork.snowball.bridginganalyzer.block;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.bukkit.GameMode;
import org.junit.jupiter.api.Test;

class PracticeBlockRegistryPolicyTest {
    @Test
    void onlyCreativePlacementsArePermanent() {
        assertTrue(PracticeBlockRegistry.shouldTrack(GameMode.SURVIVAL));
        assertTrue(PracticeBlockRegistry.shouldTrack(GameMode.ADVENTURE));
        assertTrue(PracticeBlockRegistry.shouldTrack(GameMode.SPECTATOR));
        assertFalse(PracticeBlockRegistry.shouldTrack(GameMode.CREATIVE));
    }

    @Test
    void freezesOnlyTrackedGravityMaterials() {
        assertTrue(PracticeBlockRegistry.shouldFreezeGravity(true, true));
        assertFalse(PracticeBlockRegistry.shouldFreezeGravity(true, false));
        assertFalse(PracticeBlockRegistry.shouldFreezeGravity(false, true));
        assertFalse(PracticeBlockRegistry.shouldFreezeGravity(false, false));
    }
}
