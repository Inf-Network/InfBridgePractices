package sakura.kooi.BridgingSkin.crate;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.block.Block;

public record LotteryCrate(String worldName, int x, int y, int z) {
    public boolean matches(Block block) {
        return block != null
                && block.getWorld().getName().equals(worldName)
                && block.getX() == x && block.getY() == y && block.getZ() == z;
    }

    public Location location() {
        World world = Bukkit.getWorld(worldName);
        return world == null ? null : new Location(world, x, y, z);
    }

    public String display() {
        return worldName + " (" + x + ", " + y + ", " + z + ")";
    }
}
