/*
 * 1.21.11 移植新增。
 *
 * 原版靠 Inventory#getTitle() 比对标题字符串来识别「皮肤库存」菜单,
 * 但该方法在 1.14 起已从 Inventory 上移除(标题属于 InventoryView,不属于 Inventory)。
 * 改成给菜单挂一个自定义 InventoryHolder,按类型识别 —— 比字符串比对更可靠,
 * 也不会被玩家用同名箱子骗过。
 *
 * 与 SkinEditHolder 分开:那个是管理员编辑菜单用的,关闭时会用箱子内容覆盖玩家皮肤库存,
 * 选皮肤菜单绝不能走那条路径。
 */
package sakura.kooi.BridgingSkin;

import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;

public class SkinSelectHolder
implements InventoryHolder {
    private Inventory inv;

    public Inventory getInventory() {
        return this.inv;
    }

    public void setInv(Inventory inv) {
        this.inv = inv;
    }
}
