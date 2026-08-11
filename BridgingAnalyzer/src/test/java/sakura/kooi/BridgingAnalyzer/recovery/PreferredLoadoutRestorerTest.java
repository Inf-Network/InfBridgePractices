package sakura.kooi.BridgingAnalyzer.recovery;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.Test;

class PreferredLoadoutRestorerTest {
    @Test
    void checkpointChestSuppressesDefaultBlockLoadout() {
        AtomicInteger defaults = new AtomicInteger();

        PreferredLoadoutRestorer.restore(() -> true, defaults::incrementAndGet, ignored -> { });

        assertEquals(0, defaults.get());
    }

    @Test
    void missingCheckpointChestUsesDefaultBlockLoadout() {
        AtomicInteger defaults = new AtomicInteger();

        PreferredLoadoutRestorer.restore(() -> false, defaults::incrementAndGet, ignored -> { });

        assertEquals(1, defaults.get());
    }

    @Test
    void missingCheckpointChestReplacesExistingItemsWithDefaultBlockStack() {
        List<String> inventory = new ArrayList<>(List.of("sword", "food"));

        PreferredLoadoutRestorer.restore(
                () -> false,
                () -> {
                    inventory.clear();
                    inventory.add("64 practice blocks");
                },
                ignored -> { });

        assertEquals(List.of("64 practice blocks"), inventory);
    }

    @Test
    void brokenCheckpointChestFallsBackToDefaultBlockLoadout() {
        AtomicInteger defaults = new AtomicInteger();
        AtomicInteger failures = new AtomicInteger();

        PreferredLoadoutRestorer.restore(
                () -> { throw new IllegalStateException("broken chest"); },
                defaults::incrementAndGet,
                ignored -> failures.incrementAndGet());

        assertEquals(1, failures.get());
        assertEquals(1, defaults.get());
    }
}
