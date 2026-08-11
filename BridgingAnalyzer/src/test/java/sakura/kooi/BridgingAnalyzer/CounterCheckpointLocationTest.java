package sakura.kooi.BridgingAnalyzer;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.bukkit.Location;
import org.junit.jupiter.api.Test;

class CounterCheckpointLocationTest {
    @Test
    void chestLookupUsesSameUnmodifiedCheckpointBaseEveryTime() {
        Location checkpoint = new Location(null, 10.5, 42.0, -3.5);

        Location chestBase = Counter.checkPointLoadoutBase(checkpoint);

        assertEquals(42.0, checkpoint.getY());
        assertEquals(41.0, chestBase.getY());
        assertEquals(checkpoint.getX(), chestBase.getX());
        assertEquals(checkpoint.getZ(), chestBase.getZ());
    }
}
