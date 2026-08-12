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
        return new ItemStack(material, 64);
    }
}
