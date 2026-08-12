package net.infnetwork.snowball.bridginganalyzer.commands;

import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.ArmorStand;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import net.kyori.adventure.text.Component;
import net.infnetwork.snowball.bridginganalyzer.utils.NetworkMessages;

public class VillagerSpawnPointCommand
implements CommandExecutor {
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player)) {
            NetworkMessages.send(sender, "&c\u4ec5\u73a9\u5bb6\u53ef\u4ee5\u6267\u884c.");
            return true;
        }
        if (!sender.hasPermission("bridginganalyzer.admin")) {
            NetworkMessages.send(sender, "&c\u4f60\u6ca1\u6709\u6743\u9650\u6267\u884c\u6b64\u547d\u4ee4.");
            return true;
        }
        Player player = (Player)sender;
        Location loc = player.getLocation().getBlock().getLocation().add(0.5, -1.0, 0.5);
        ArmorStand stand = (ArmorStand)player.getWorld().spawnEntity(loc, EntityType.ARMOR_STAND);
        stand.setSmall(true);
        stand.setGravity(false);
        stand.setVisible(false);
        stand.getEquipment().setHelmet(new ItemStack(Material.REDSTONE_BLOCK, 1));
        stand.setMarker(true);
        stand.customName(Component.text("VillagerSpawnPoint"));
        NetworkMessages.send(player, "&a\u6751\u6c11\u5237\u65b0\u70b9\u5df2\u8bbe\u7f6e.");
        return true;
    }
}
