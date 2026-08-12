package sakura.kooi.BridgingSkin.lottery;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.HashSet;
import java.util.List;
import java.util.Random;
import java.util.Set;
import java.util.logging.Logger;
import org.bukkit.Material;
import org.junit.jupiter.api.Test;

class PrizePoolTest {
    @Test
    void drawsOnlyUniqueUnownedMaterials() {
        PrizePool pool = new PrizePool(List.of("STONE", "GRANITE", "DIORITE"), Logger.getAnonymousLogger());
        List<Material> draw = pool.draw(2, Set.of(Material.STONE), new Random(7));

        assertEquals(2, draw.size());
        assertEquals(2, new HashSet<>(draw).size());
        assertFalse(draw.contains(Material.STONE));
    }

    @Test
    void refusesBatchWhenNotEnoughUnownedMaterialsRemain() {
        PrizePool pool = new PrizePool(List.of("STONE", "GRANITE"), Logger.getAnonymousLogger());
        assertTrue(pool.draw(2, Set.of(Material.STONE), new Random(1)).isEmpty());
        assertEquals(1, pool.remaining(Set.of(Material.STONE)));
    }

    @Test
    void catalogNeverContainsSeaLanternOrIllegalAndEveryEntryIsSolidFullBlockWhitelist() {
        assertFalse(FullBlockPrizeCatalog.defaults().contains(Material.SEA_LANTERN));
        assertTrue(FullBlockPrizeCatalog.defaults().size() > 200);
        for (Material material : FullBlockPrizeCatalog.defaults()) {
            assertTrue(FullBlockPrizeCatalog.isEligible(material), material.name());
        }
    }
}
