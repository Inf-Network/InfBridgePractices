/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  org.bukkit.Location
 *  org.bukkit.Material
 *  org.bukkit.command.Command
 *  org.bukkit.command.CommandExecutor
 *  org.bukkit.command.CommandSender
 *  org.bukkit.entity.ArmorStand
 *  org.bukkit.entity.EntityType
 *  org.bukkit.entity.Player
 *  org.bukkit.inventory.ItemStack
 */
package sakura.kooi.BridgingAnalyzer.commands;

import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.ArmorStand;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

public class VillagerSpawnPointCommand
implements CommandExecutor {
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player)) {
            return true;
        }
        if (!sender.hasPermission("bridginganalyzer.admin")) {
            return true;
        }
        Player player = (Player)sender;
        Location loc = player.getLocation().getBlock().getLocation().add(0.5, -1.0, 0.5);
        ArmorStand stand = (ArmorStand)player.getWorld().spawnEntity(loc, EntityType.ARMOR_STAND);
        stand.setSmall(true);
        stand.setGravity(false);
        stand.setVisible(false);
        stand.setHelmet(new ItemStack(Material.REDSTONE_BLOCK, 1));
        stand.setMarker(true);
        stand.setCustomName("VillagerSpawnPoint");
        player.sendMessage("\u00a7b\u00a7l\u642d\u8def\u7ec3\u4e60 \u00a77>> \u00a7a\u6751\u6c11\u5237\u65b0\u70b9\u5df2\u8bbe\u7f6e");
        return true;
    }
}

