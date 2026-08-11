/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  org.bukkit.Material
 */
package sakura.kooi.BridgingSkin;

import java.util.ArrayList;
import org.bukkit.Material;

public class IllegalMaterial {
    private static final ArrayList<Material> illegalMaterial = new ArrayList();

    public static boolean isIllegal(Material skin) {
        return illegalMaterial.contains(skin);
    }

    static {
        illegalMaterial.add(Material.REDSTONE_BLOCK);
        illegalMaterial.add(Material.PISTON);
        illegalMaterial.add(Material.STICKY_PISTON);
        illegalMaterial.add(Material.LEVER);
        illegalMaterial.add(Material.DISPENSER);
        illegalMaterial.add(Material.LAPIS_BLOCK);
        illegalMaterial.add(Material.EMERALD_BLOCK);
        illegalMaterial.add(Material.BEACON);
        illegalMaterial.add(Material.COMPARATOR);
        illegalMaterial.add(Material.REPEATER);
        illegalMaterial.add(Material.REDSTONE);
        illegalMaterial.add(Material.REDSTONE_TORCH);
        illegalMaterial.add(Material.STONE_BUTTON);
        illegalMaterial.add(Material.OAK_BUTTON);
        illegalMaterial.add(Material.HOPPER);
        illegalMaterial.add(Material.LIGHT_WEIGHTED_PRESSURE_PLATE);
        illegalMaterial.add(Material.HEAVY_WEIGHTED_PRESSURE_PLATE);
        illegalMaterial.add(Material.STONE_PRESSURE_PLATE);
        illegalMaterial.add(Material.OAK_PRESSURE_PLATE);
        illegalMaterial.add(Material.DAYLIGHT_DETECTOR);
        illegalMaterial.add(Material.DROPPER);
        illegalMaterial.add(Material.SLIME_BLOCK);
        illegalMaterial.add(Material.ANVIL);
        illegalMaterial.add(Material.GRAVEL);
    }
}

