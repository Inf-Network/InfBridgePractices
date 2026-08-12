package sakura.kooi.BridgingSkin.crate;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.lang.reflect.Proxy;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.configuration.file.YamlConfiguration;
import org.junit.jupiter.api.Test;

class LotteryCrateServiceTest {
    @Test
    void constructorMigratesAndSavesLegacyConfiguration() {
        YamlConfiguration config = new YamlConfiguration();
        config.set("lottery.crate.world", "world");
        config.set("lottery.crate.x", 10);
        config.set("lottery.crate.y", 64);
        config.set("lottery.crate.z", -2);
        AtomicInteger saves = new AtomicInteger();

        LotteryCrateService service = new LotteryCrateService(
                config, saves::incrementAndGet, player -> null);

        assertEquals(1, saves.get());
        assertEquals(List.of(new LotteryCrate("world", 10, 64, -2)), service.crates());
        assertFalse(config.contains("lottery.crate"));
        assertEquals(1, config.getMapList("lottery.crates").size());
    }

    @Test
    void addsMultipleCratesIdempotentlyAndMatchesEveryRegisteredLocation() throws Exception {
        YamlConfiguration config = new YamlConfiguration();
        AtomicInteger saves = new AtomicInteger();
        AtomicReference<Block> selection = new AtomicReference<>();
        LotteryCrateService service = new LotteryCrateService(
                config, saves::incrementAndGet, player -> selection.get());
        Block first = block("world", 1, 64, 2, Material.ENDER_CHEST);
        Block second = block("world_nether", -3, 70, 4, Material.ENDER_CHEST);

        selection.set(first);
        assertTrue(service.registerSelectedWithStatus(null).added());
        assertFalse(service.registerSelectedWithStatus(null).added());
        selection.set(second);
        assertTrue(service.registerSelectedWithStatus(null).added());

        assertEquals(2, saves.get());
        assertEquals(List.of(
                new LotteryCrate("world", 1, 64, 2),
                new LotteryCrate("world_nether", -3, 70, 4)), service.crates());
        assertEquals(service.crates().getFirst(), service.crate().orElseThrow());
        assertTrue(service.matches(first));
        assertTrue(service.matches(second));
        assertFalse(service.matches(block("world", 9, 64, 2, Material.ENDER_CHEST)));
        assertFalse(service.matches(null));
        assertThrows(UnsupportedOperationException.class, () -> service.crates().clear());
    }

    @Test
    void removesOnlySelectedRegistrationEvenWhenBlockTypeChanged() throws Exception {
        YamlConfiguration config = new YamlConfiguration();
        config.set("lottery.crates", List.of(
                java.util.Map.of("world", "world", "x", 1, "y", 64, "z", 2),
                java.util.Map.of("world", "world", "x", 3, "y", 64, "z", 4)));
        AtomicInteger saves = new AtomicInteger();
        Block changedBlock = block("world", 1, 64, 2, Material.STONE);
        LotteryCrateService service = new LotteryCrateService(
                config, saves::incrementAndGet, player -> changedBlock);

        assertEquals(new LotteryCrate("world", 1, 64, 2),
                service.removeSelected(null).orElseThrow());

        assertEquals(1, saves.get());
        assertEquals(List.of(new LotteryCrate("world", 3, 64, 4)), service.crates());
        assertFalse(service.matches(changedBlock));
    }

    @Test
    void rejectsSelectedBlockThatIsNotAnEnderChest() {
        AtomicInteger saves = new AtomicInteger();
        LotteryCrateService service = new LotteryCrateService(
                new YamlConfiguration(), saves::incrementAndGet,
                player -> block("world", 1, 64, 2, Material.CHEST));

        CrateSelectionException exception = assertThrows(
                CrateSelectionException.class, () -> service.registerSelected(null));

        assertTrue(exception.getMessage().contains("末影箱"));
        assertEquals(0, saves.get());
        assertTrue(service.crates().isEmpty());
    }

    private static Block block(String worldName, int x, int y, int z, Material material) {
        World world = (World) Proxy.newProxyInstance(
                World.class.getClassLoader(), new Class<?>[] {World.class},
                (proxy, method, args) -> switch (method.getName()) {
                    case "getName" -> worldName;
                    case "toString" -> worldName;
                    default -> throw new UnsupportedOperationException(method.getName());
                });
        return (Block) Proxy.newProxyInstance(
                Block.class.getClassLoader(), new Class<?>[] {Block.class},
                (proxy, method, args) -> switch (method.getName()) {
                    case "getWorld" -> world;
                    case "getX" -> x;
                    case "getY" -> y;
                    case "getZ" -> z;
                    case "getType" -> material;
                    case "toString" -> worldName + " (" + x + ", " + y + ", " + z + ")";
                    default -> throw new UnsupportedOperationException(method.getName());
                });
    }
}
