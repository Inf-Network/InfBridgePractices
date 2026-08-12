package net.infnetwork.snowball.bridginganalyzer;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

import java.util.concurrent.atomic.AtomicInteger;
import org.bukkit.Material;
import org.junit.jupiter.api.Test;

class CheckpointSkinPlaceholderPolicyTest {
    @Test
    void replacesExactSandstoneWithSelectedSkin() {
        CheckpointSkinPlaceholderPolicy policy = new CheckpointSkinPlaceholderPolicy(
                () -> Material.STRIPPED_MANGROVE_WOOD);

        assertEquals(Material.STRIPPED_MANGROVE_WOOD, policy.resolve(Material.SANDSTONE));
        assertNull(policy.failure());
    }

    @Test
    void resolvesSkinOnlyOnceForMultiplePlaceholders() {
        AtomicInteger calls = new AtomicInteger();
        CheckpointSkinPlaceholderPolicy policy = new CheckpointSkinPlaceholderPolicy(() -> {
            calls.incrementAndGet();
            return Material.DIAMOND_BLOCK;
        });

        assertEquals(Material.DIAMOND_BLOCK, policy.resolve(Material.SANDSTONE));
        assertEquals(Material.DIAMOND_BLOCK, policy.resolve(Material.SANDSTONE));
        assertEquals(Material.DIAMOND_BLOCK, policy.resolve(Material.SANDSTONE));
        assertEquals(1, calls.get());
    }

    @Test
    void doesNotConsultSkinProviderForOtherSandstoneVariantsOrItems() {
        AtomicInteger calls = new AtomicInteger();
        CheckpointSkinPlaceholderPolicy policy = new CheckpointSkinPlaceholderPolicy(() -> {
            calls.incrementAndGet();
            return Material.DIAMOND_BLOCK;
        });

        assertEquals(Material.CUT_SANDSTONE, policy.resolve(Material.CUT_SANDSTONE));
        assertEquals(Material.SMOOTH_SANDSTONE, policy.resolve(Material.SMOOTH_SANDSTONE));
        assertEquals(Material.CHISELED_SANDSTONE, policy.resolve(Material.CHISELED_SANDSTONE));
        assertEquals(Material.STICK, policy.resolve(Material.STICK));
        assertEquals(0, calls.get());
        assertNull(policy.failure());
    }

    @Test
    void brokenSkinProviderKeepsOriginalSandstone() {
        CheckpointSkinPlaceholderPolicy policy = new CheckpointSkinPlaceholderPolicy(() -> {
            throw new IllegalStateException("skin database unavailable");
        });

        assertEquals(Material.SANDSTONE, policy.resolve(Material.SANDSTONE));
        assertEquals(Material.SANDSTONE, policy.resolve(Material.SANDSTONE));
        assertNotNull(policy.failure());
        assertEquals("skin database unavailable", policy.failure().getMessage());
    }

    @Test
    void nullOrAirSkinKeepsOriginalSandstone() {
        CheckpointSkinPlaceholderPolicy nullPolicy = new CheckpointSkinPlaceholderPolicy(() -> null);
        CheckpointSkinPlaceholderPolicy airPolicy = new CheckpointSkinPlaceholderPolicy(() -> Material.AIR);

        assertEquals(Material.SANDSTONE, nullPolicy.resolve(Material.SANDSTONE));
        assertEquals(Material.SANDSTONE, airPolicy.resolve(Material.SANDSTONE));
        assertNotNull(nullPolicy.failure());
        assertNotNull(airPolicy.failure());
    }
}
