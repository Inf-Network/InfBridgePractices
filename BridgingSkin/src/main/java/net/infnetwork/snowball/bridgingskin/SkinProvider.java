/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  org.bukkit.Material
 *  org.bukkit.entity.Player
 *  org.bukkit.inventory.ItemStack
 *  net.infnetwork.snowball.bridginganalyzer.api.BlockSkinProvider
 */
package net.infnetwork.snowball.bridgingskin;

import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import net.infnetwork.snowball.bridginganalyzer.api.BlockSkinProvider;
import net.infnetwork.snowball.bridgingskin.BridgingSkin;
import net.infnetwork.snowball.bridgingskin.IllegalMaterial;
import net.infnetwork.snowball.bridgingskin.data.SkinSet;

public class SkinProvider
implements BlockSkinProvider {
    public ItemStack provide(Player player) {
        SkinSet skin = BridgingSkin.getSkin((String)player.getName(), (String)player.getUniqueId().toString()).currentSkin;
        Material material = Material.getMaterial((String)skin.material);
        if (material == null || !material.isBlock() || !material.isItem()
                || IllegalMaterial.isIllegal(material)) {
            material = Material.CUT_SANDSTONE;
        }
        // 原版此处还有一句 "material 是 SANDSTONE 就把 data 强制成 2",
        // 那是 1.8 用来把普通砂岩纠正成平滑砂岩的;扁平化后两者已是不同 Material,不再需要。
        return new ItemStack(material, 64);
    }
}
