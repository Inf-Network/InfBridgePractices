package net.infnetwork.snowball.bridgingskin;

import java.util.Objects;
import org.bukkit.Material;
import org.bukkit.command.CommandSender;

/** Compatibility Runnable that now performs one transactional database-backed clear. */
public final class ClearThread implements Runnable {
    private final CommandSender sender;
    private final Material material;

    public ClearThread(CommandSender sender, Material material) {
        this.sender = Objects.requireNonNull(sender, "sender");
        this.material = Objects.requireNonNull(material, "material");
    }

    @Override
    public void run() {
        if (material == Material.CUT_SANDSTONE) {
            NetworkMessages.send(sender, "&cCUT_SANDSTONE 是保底皮肤，禁止全局清除.");
            return;
        }
        try {
            BridgingSkin.getSkinService().clearMaterialGlobally(material);
            NetworkMessages.send(sender,
                    "&b皮肤清理完成，已从数据库及在线缓存移除 " + material.name());
        } catch (RuntimeException exception) {
            BridgingSkin.getInstance().getLogger().severe(
                    "全局清理皮肤 " + material.name() + " 失败: " + exception.getMessage());
            NetworkMessages.send(sender,
                    "&c皮肤清理失败，数据库事务没有确认本次修改.");
        }
    }
}
