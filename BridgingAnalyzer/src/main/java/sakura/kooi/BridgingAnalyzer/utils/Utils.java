/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  org.bukkit.Effect
 *  org.bukkit.Material
 *  org.bukkit.block.Block
 *  org.bukkit.inventory.ItemStack
 *  org.bukkit.inventory.PlayerInventory
 */
package sakura.kooi.BridgingAnalyzer.utils;

import org.bukkit.Effect;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;

public class Utils {
    public static void breakBlock(Block b) {
        if (!b.getChunk().isLoaded()) {
            b.getChunk().load(false);
        }
        b.getWorld().playEffect(b.getLocation(), Effect.STEP_SOUND, (Object)b.getType());
        b.setType(Material.AIR);
    }

    public static double formatDouble(double d) {
        return (double)Math.round(d * 100.0) / 100.0;
    }

    public static void addItem(PlayerInventory inv, ItemStack item) {
        if (Utils.isEmptySlot(item)) {
            return;
        }
        if (item.getType().toString().endsWith("_HELMET")) {
            if (Utils.isEmptySlot(inv.getHelmet())) {
                inv.setHelmet(item);
            }
        } else if (item.getType().toString().endsWith("_CHESTPLATE")) {
            if (Utils.isEmptySlot(inv.getChestplate())) {
                inv.setChestplate(item);
            }
        } else if (item.getType().toString().endsWith("_LEGGINGS")) {
            if (Utils.isEmptySlot(inv.getLeggings())) {
                inv.setLeggings(item);
            }
        } else if (item.getType().toString().endsWith("_BOOTS")) {
            if (Utils.isEmptySlot(inv.getBoots())) {
                inv.setBoots(item);
            }
        } else {
            inv.addItem(new ItemStack[]{item});
        }
    }

    public static boolean isEmptySlot(ItemStack item) {
        if (item == null) {
            return true;
        }
        return item.getType() == Material.AIR;
    }
}

