/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  org.bukkit.Bukkit
 *  org.bukkit.Material
 *  org.bukkit.OfflinePlayer
 *  org.bukkit.command.Command
 *  org.bukkit.command.CommandExecutor
 *  org.bukkit.command.CommandSender
 *  org.bukkit.entity.Player
 *  org.bukkit.inventory.Inventory
 *  org.bukkit.inventory.InventoryHolder
 *  org.bukkit.inventory.ItemStack
 *  org.bukkit.plugin.Plugin
 */
package sakura.kooi.BridgingSkin;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.Plugin;
import sakura.kooi.BridgingSkin.BridgingSkin;
import sakura.kooi.BridgingSkin.ClearThread;
import sakura.kooi.BridgingSkin.IllegalMaterial;
import sakura.kooi.BridgingSkin.SkinEditHolder;
import sakura.kooi.BridgingSkin.data.PlayerSkin;
import sakura.kooi.BridgingSkin.data.SkinSet;

public class SkinEditCommand
implements CommandExecutor {
    public boolean onCommand(CommandSender sender, Command arg, String arg2, String[] args) {
        if (sender instanceof Player && sender.hasPermission("bridgingSkin.admin")) {
            if (args.length < 2) {
                sender.sendMessage(new String[]{"&bI&en&cf &eBridge &7\u00bb \u00a7e/bskin-edit edit <player>", "&bI&en&cf &eBridge &7\u00bb \u00a7e/bskin-edit clear <material>"});
                return true;
            }
            switch (args[0]) {
                case "edit": {
                    String player = args[1];
                    OfflinePlayer offp = Bukkit.getOfflinePlayer((String)player);
                    if (offp == null) {
                        sender.sendMessage("&bI&en&cf &eBridge &7\u00bb \u00a7c\u73a9\u5bb6 " + player + " \u4e0d\u5b58\u5728");
                        return true;
                    }
                    PlayerSkin skin = BridgingSkin.getSkin(offp.getName());
                    if (skin == null) {
                        sender.sendMessage("&bI&en&cf &eBridge &7\u00bb \u00a7c\u73a9\u5bb6 " + offp.getName() + " \u6ca1\u6709\u76ae\u80a4");
                        return true;
                    }
                    this.openSkinInventory((Player)sender, skin);
                    return true;
                }
                case "clear": {
                    Material material = Material.getMaterial((String)args[1].toUpperCase());
                    if (material == null) {
                        sender.sendMessage("&bI&en&cf &eBridge &7\u00bb \u00a7cMaterial " + args[1].toUpperCase() + " \u4e0d\u5b58\u5728");
                        return true;
                    }
                    // \u539f\u7248\u8fd9\u91cc\u8fd8\u63a5\u4e00\u4e2a\u53ef\u9009\u7684 [data] \u53c2\u6570(-1 \u8868\u793a\u4e0d\u9650 data)\u3002
                    // \u6241\u5e73\u5316\u540e\u65b9\u5757\u53d8\u4f53\u5404\u81ea\u662f\u72ec\u7acb Material,data \u5df2\u65e0\u610f\u4e49,\u53c2\u6570\u4e00\u5e76\u53bb\u6389\u3002
                    Bukkit.getScheduler().runTaskAsynchronously((Plugin)BridgingSkin.getInstance(), (Runnable)new ClearThread(sender, material));
                    return true;
                }
            }
        }
        return true;
    }

    private void openSkinInventory(Player player, PlayerSkin skins) {
        SkinEditHolder holder = new SkinEditHolder(skins);
        Inventory inv = Bukkit.createInventory((InventoryHolder)holder, (int)54, (String)("\u00a76\u00a7l\u7f16\u8f91\u76ae\u80a4\u5e93\u5b58: \u73a9\u5bb6 " + skins.player));
        holder.setInv(inv);
        for (SkinSet skin : skins.allSkin) {
            Material material = Material.getMaterial((String)skin.material);
            if (material == null || IllegalMaterial.isIllegal(material)) continue;
            ItemStack stack = new ItemStack(material, 1);
            inv.addItem(new ItemStack[]{stack});
        }
        player.openInventory(inv);
    }
}

