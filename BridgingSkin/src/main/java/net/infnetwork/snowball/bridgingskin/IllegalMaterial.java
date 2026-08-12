package net.infnetwork.snowball.bridgingskin;

import java.util.EnumSet;
import java.util.Set;
import org.bukkit.Material;

public class IllegalMaterial {
    private static final Set<Material> ILLEGAL_MATERIALS = EnumSet.of(
            Material.REDSTONE_BLOCK,
            Material.PISTON,
            Material.STICKY_PISTON,
            Material.LEVER,
            Material.DISPENSER,
            Material.LAPIS_BLOCK,
            Material.EMERALD_BLOCK,
            Material.BEACON,
            Material.COMPARATOR,
            Material.REPEATER,
            Material.REDSTONE,
            Material.REDSTONE_TORCH,
            Material.STONE_BUTTON,
            Material.OAK_BUTTON,
            Material.HOPPER,
            Material.LIGHT_WEIGHTED_PRESSURE_PLATE,
            Material.HEAVY_WEIGHTED_PRESSURE_PLATE,
            Material.STONE_PRESSURE_PLATE,
            Material.OAK_PRESSURE_PLATE,
            Material.DAYLIGHT_DETECTOR,
            Material.DROPPER,
            Material.SLIME_BLOCK,
            Material.ANVIL,
            Material.GRAVEL,
            Material.MELON,
            Material.SEA_LANTERN);

    public static boolean isIllegal(Material skin) {
        return ILLEGAL_MATERIALS.contains(skin);
    }
}
