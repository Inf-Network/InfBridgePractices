package net.infnetwork.snowball.bridgingskin;

import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import net.infnetwork.snowball.bridginganalyzer.api.BlockSkinProvider;
import net.infnetwork.snowball.bridgingskin.data.SkinSet;

public class SkinProvider implements BlockSkinProvider {
    @Override
    public ItemStack provide(Player player) {
        SkinSet skin = BridgingSkin.getSkinService().getOrCreate(player).currentSkin;
        Material material = Material.getMaterial(skin.material);
        if (material == null || !material.isBlock() || !material.isItem()
                || IllegalMaterial.isIllegal(material)) {
            material = Material.CUT_SANDSTONE;
        }
        return new ItemStack(material, 64);
    }
}
