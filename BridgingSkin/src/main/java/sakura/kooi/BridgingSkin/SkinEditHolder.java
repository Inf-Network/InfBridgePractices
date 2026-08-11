/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  org.bukkit.inventory.Inventory
 *  org.bukkit.inventory.InventoryHolder
 */
package sakura.kooi.BridgingSkin;

import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import sakura.kooi.BridgingSkin.data.PlayerSkin;

public class SkinEditHolder
implements InventoryHolder {
    private final PlayerSkin skins;
    private Inventory inv;

    public SkinEditHolder(PlayerSkin skins) {
        this.skins = skins;
    }

    public Inventory getInventory() {
        return this.inv;
    }

    public PlayerSkin getSkins() {
        return this.skins;
    }

    public void setInv(Inventory inv) {
        this.inv = inv;
    }
}

