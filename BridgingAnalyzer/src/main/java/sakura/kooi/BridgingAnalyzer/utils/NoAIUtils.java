/*
 * 1.21.11 移植:原版用 NMS 反射(CraftEntity -> getHandle -> NBTTagCompound
 * 写 NoAI 标志)。Paper 自 1.20.5 起去掉了 org.bukkit.craftbukkit 的版本号包名,
 * 靠 Bukkit.getServer().getClass().getPackage().getName().split(".")[3] 拼出来的
 * 包名不再存在,clsCraftEntity 恒为 null。
 *
 * 而且原版静态块有 bug:catch 里置 works=false 之后,末尾又无条件 works=true,
 * 于是反射初始化失败也照样往下走,每次调用都抛 NPE。
 *
 * Bukkit 从 1.9 起提供 LivingEntity#setAI,语义与原版写 NoAI 标志一致,
 * 直接用原生 API,反射整套删除。
 */
package sakura.kooi.BridgingAnalyzer.utils;

import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;

public class NoAIUtils {

    /**
     * 开关实体的 AI。
     *
     * @param bukkitEntity 目标实体。非 LivingEntity 时静默忽略 —— 原版基于 NBT
     *                     的实现同样只对生物有意义
     * @param hasAI        true 保留 AI,false 关闭
     */
    public static void setAI(Entity bukkitEntity, boolean hasAI) {
        if (bukkitEntity instanceof LivingEntity living) {
            living.setAI(hasAI);
        }
    }
}
