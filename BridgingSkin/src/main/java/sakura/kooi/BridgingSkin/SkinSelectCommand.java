/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  org.bukkit.Bukkit
 *  org.bukkit.Material
 *  org.bukkit.command.Command
 *  org.bukkit.command.CommandExecutor
 *  org.bukkit.command.CommandSender
 *  org.bukkit.entity.Player
 *  org.bukkit.inventory.Inventory
 *  org.bukkit.inventory.ItemStack
 *  org.bukkit.inventory.meta.ItemMeta
 */
package sakura.kooi.BridgingSkin;

import java.util.ArrayList;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import sakura.kooi.BridgingSkin.BridgingSkin;
import sakura.kooi.BridgingSkin.IllegalMaterial;
import sakura.kooi.BridgingSkin.data.SkinSet;

public class SkinSelectCommand
implements CommandExecutor {
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        Material material;
        if (!(sender instanceof Player)) {
            sender.sendMessage("&bI&en&cf &eBridge &7\u00bb \u00a7c\u4ec5\u73a9\u5bb6\u53ef\u4ee5\u6267\u884c.");
            return true;
        }
        Player p = (Player)sender;
        SkinSelectHolder holder = new SkinSelectHolder();
        Inventory inv = Bukkit.createInventory((InventoryHolder)holder, (int)54, (String)"\u00a76\u00a7l\u76ae\u80a4\u5e93\u5b58");
        holder.setInv(inv);
        ArrayList<SkinSet> illegalSkins = new ArrayList<SkinSet>();
        for (SkinSet skin : BridgingSkin.getSkin((String)p.getName(), (String)p.getUniqueId().toString()).allSkin) {
            material = Material.getMaterial((String)skin.material);
            if (material == null) {
                illegalSkins.add(skin);
                continue;
            }
            if (!IllegalMaterial.isIllegal(material)) continue;
            illegalSkins.add(skin);
        }
        if (!illegalSkins.isEmpty()) {
            BridgingSkin.getSkin((String)p.getName(), (String)p.getUniqueId().toString()).allSkin.removeAll(illegalSkins);
            sender.sendMessage("&bI&en&cf &eBridge &7\u00bb \u00a7c\u5728\u4f60\u7684\u76ae\u80a4\u5e93\u5b58\u53d1\u73b0\u4e86\u4e00\u4e9b\u65e0\u6548\u7269\u54c1, \u5df2\u81ea\u52a8\u5220\u9664.");
        }
        for (SkinSet skin : BridgingSkin.getSkin((String)p.getName(), (String)p.getUniqueId().toString()).allSkin) {
            ItemStack stack;
            material = Material.getMaterial((String)skin.material);
            if (material == null) {
                material = Material.BARRIER;
                stack = new ItemStack(material, 64);
                ItemMeta meta = stack.getItemMeta();
                meta.setDisplayName("\u00a7c\u65e0\u6548\u76ae\u80a4 \u6570\u636e\u9519\u8bef");
                stack.setItemMeta(meta);
            } else {
                stack = new ItemStack(material, 64);
            }
            inv.addItem(new ItemStack[]{stack});
        }
        p.openInventory(inv);
        return true;
    }
}

