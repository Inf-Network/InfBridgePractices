/*
 * Decompiled with CFR 0.152.
 *
 * 1.21.11 移植:原版是 new Sandstone(SandstoneType.SMOOTH).toItemStack(64)。
 * org.bukkit.material.Sandstone / SandstoneType 属于 1.13 之前的 MaterialData 体系,
 * 在 1.21 上还能跑,但每次发放搭路方块都会触发 Bukkit 的旧 ID 兼容层,日志里报
 * "Initializing Legacy Material Support. Unless you have legacy plugins and/or data this is a bug!"。
 *
 * 1.8 的「平滑砂岩」(SANDSTONE:2)扁平化后即 CUT_SANDSTONE
 * —— 已用升级后的 world/region 方块调色板核对过。
 * 注意 1.21 里另有一个 SMOOTH_SANDSTONE,那是 1.13 新增的熔炼产物,材质不同,不是这个。
 */
package sakura.kooi.BridgingAnalyzer;

import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import sakura.kooi.BridgingAnalyzer.api.BlockSkinProvider;

public class DefaultBlockSkinProvider
implements BlockSkinProvider {
    @Override
    public ItemStack provide(Player player) {
        return new ItemStack(Material.CUT_SANDSTONE, 64);
    }
}
