package net.infnetwork.snowball.bridginganalyzer;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.bukkit.Material;
import org.junit.jupiter.api.Test;

class CounterListenerPolicyTest {
    @Test
    void mapsEverySupportedFilledBucketToItsWorldBlock() {
        assertEquals(Material.WATER, CounterListener.bucketContents(Material.WATER_BUCKET));
        assertEquals(Material.LAVA, CounterListener.bucketContents(Material.LAVA_BUCKET));
        assertEquals(
                Material.POWDER_SNOW,
                CounterListener.bucketContents(Material.POWDER_SNOW_BUCKET));
        assertNull(CounterListener.bucketContents(Material.BUCKET));
        assertNull(CounterListener.bucketContents(Material.MILK_BUCKET));
        assertNull(CounterListener.bucketContents(Material.COD_BUCKET));
    }

    @Test
    void rejectsWaterloggingAndCauldronMutationButAllowsAirPlacement() {
        assertTrue(CounterListener.isUnsafeBucketTarget(Material.OAK_SLAB, true));
        assertTrue(CounterListener.isUnsafeBucketTarget(Material.OAK_SLAB, false));
        assertTrue(CounterListener.isUnsafeBucketTarget(Material.WATER, false));
        assertTrue(CounterListener.isUnsafeBucketTarget(Material.LAVA, false));
        assertTrue(CounterListener.isUnsafeBucketTarget(Material.CAULDRON, false));
        assertTrue(CounterListener.isUnsafeBucketTarget(Material.WATER_CAULDRON, false));
        assertFalse(CounterListener.isUnsafeBucketTarget(Material.AIR, false));
        assertFalse(CounterListener.isUnsafeBucketTarget(Material.CAVE_AIR, false));
        assertFalse(CounterListener.isUnsafeBucketTarget(Material.VOID_AIR, false));
    }

    @Test
    void rejectsOnlyWaterLavaPairsThatCanGenerateUntrackedNeighborBlocks() {
        assertTrue(CounterListener.reactiveFluids(Material.WATER, Material.LAVA));
        assertTrue(CounterListener.reactiveFluids(Material.LAVA, Material.WATER));
        assertFalse(CounterListener.reactiveFluids(Material.WATER, Material.WATER));
        assertFalse(CounterListener.reactiveFluids(Material.POWDER_SNOW, Material.LAVA));
    }
}
