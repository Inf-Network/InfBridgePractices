package net.infnetwork.snowball.bridginganalyzer.commands;

import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import net.infnetwork.snowball.bridginganalyzer.BridgingAnalyzer;
import net.infnetwork.snowball.bridginganalyzer.utils.NetworkMessages;

public class StuckCommand
implements CommandExecutor {
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player)) {
            NetworkMessages.send(sender, "&c\u4ec5\u73a9\u5bb6\u53ef\u4ee5\u6267\u884c.");
            return true;
        }
        Player p = (Player)sender;
        if (p.getLocation().getBlock().getRelative(BlockFace.DOWN).getType() == Material.EMERALD_BLOCK) {
            for (int x = -3; x < 3; ++x) {
                for (int y = -3; y < 3; ++y) {
                    for (int z = -3; z < 3; ++z) {
                        Block b = p.getLocation().add((double)x, (double)y, (double)z).getBlock();
                        if (!BridgingAnalyzer.isPlacedByPlayer(b)) continue;
                        b.setType(Material.AIR, false);
                        BridgingAnalyzer.forgetPracticeBlock(b);
                    }
                }
            }
            NetworkMessages.send(sender, "&a\u4f60\u5468\u56f4\u7684\u65b9\u5757\u5df2\u88ab\u6e05\u9664.");
        } else {
            if (p.getInventory().contains(Material.GOLDEN_PICKAXE)) {
                NetworkMessages.send(sender, "&a\u4f60\u80cc\u5305\u91cc\u6709\u7a3f\u5b50, \u81ea\u5df1\u6316\u5f00=-=");
            }
            ItemStack pickaxe = new ItemStack(Material.GOLDEN_PICKAXE, 1);
            pickaxe.setDurability((short)27);
            p.getInventory().addItem(new ItemStack[]{pickaxe});
            NetworkMessages.send(sender, "&a\u4f60\u4f3c\u4e4e\u4e0d\u5728\u51fa\u751f\u70b9, \u7ed9\u4f60\u4e00\u628a\u7a3f\u5b50, \u88ab\u5361\u4f4f\u8bf7\u81ea\u884c\u6316\u5f00.");
        }
        return true;
    }
}
