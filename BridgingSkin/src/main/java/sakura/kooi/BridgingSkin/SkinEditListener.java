/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  org.bukkit.event.EventHandler
 *  org.bukkit.event.Listener
 *  org.bukkit.event.inventory.InventoryCloseEvent
 *  org.bukkit.inventory.ItemStack
 */
package sakura.kooi.BridgingSkin;

import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.inventory.ItemStack;
import sakura.kooi.BridgingSkin.BridgingSkin;
import sakura.kooi.BridgingSkin.SkinEditHolder;
import sakura.kooi.BridgingSkin.data.SkinSet;

public class SkinEditListener
implements Listener {
    @EventHandler
    public void onInventoryClose(InventoryCloseEvent e) {
        if (e.getInventory().getHolder() instanceof SkinEditHolder) {
            SkinEditHolder holder = (SkinEditHolder)e.getInventory().getHolder();
            holder.getSkins().allSkin.clear();
            for (ItemStack item : e.getInventory().getContents()) {
                if (item == null) continue;
                holder.getSkins().allSkin.add(new SkinSet(item.getType().name()));
            }
            holder.getSkins().currentSkin = new SkinSet();
            BridgingSkin.saveSkin(holder.getSkins());
        }
    }
}

