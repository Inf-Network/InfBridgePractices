package net.infnetwork.snowball.bridginganalyzer;

import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import net.infnetwork.snowball.bridginganalyzer.api.BlockSkinProvider;

public class DefaultBlockSkinProvider
implements BlockSkinProvider {
    @Override
    public ItemStack provide(Player player) {
        return new ItemStack(Material.CUT_SANDSTONE, 64);
    }
}
