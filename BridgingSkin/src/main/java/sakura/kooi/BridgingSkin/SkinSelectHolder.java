package sakura.kooi.BridgingSkin;

import java.util.Map;
import java.util.UUID;
import org.bukkit.Material;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;

/** Immutable identity and slot routing for one page of the player skin menu. */
public final class SkinSelectHolder implements InventoryHolder {
    public static final int CONTENT_SIZE = 45;
    public static final int PREVIOUS_SLOT = 45;
    public static final int PAGE_SLOT = 49;
    public static final int NEXT_SLOT = 53;

    private final UUID playerUuid;
    private final int page;
    private final int totalPages;
    private final Map<Integer, Material> materialsBySlot;
    private Inventory inventory;
    private boolean actionClaimed;

    /** @deprecated Binary compatibility for extensions compiled against v3. */
    @Deprecated
    public SkinSelectHolder() {
        this(new UUID(0L, 0L), 0, 1, Map.of());
    }

    public SkinSelectHolder(
            UUID playerUuid,
            int page,
            int totalPages,
            Map<Integer, Material> materialsBySlot
    ) {
        this.playerUuid = playerUuid;
        this.page = page;
        this.totalPages = totalPages;
        this.materialsBySlot = Map.copyOf(materialsBySlot);
    }

    public UUID playerUuid() {
        return playerUuid;
    }

    public int page() {
        return page;
    }

    public int totalPages() {
        return totalPages;
    }

    public Material materialAt(int rawSlot) {
        return materialsBySlot.get(rawSlot);
    }

    /** Ensures packet bursts cannot execute two actions before the next GUI opens/closes. */
    public boolean claimAction() {
        if (actionClaimed) {
            return false;
        }
        actionClaimed = true;
        return true;
    }

    public void releaseAction() {
        actionClaimed = false;
    }

    @Override
    public Inventory getInventory() {
        return inventory;
    }

    public void attach(Inventory inventory) {
        if (this.inventory != null) {
            throw new IllegalStateException("SkinSelectHolder 已绑定 Inventory");
        }
        this.inventory = inventory;
    }

    /** @deprecated Use {@link #attach(Inventory)}. */
    @Deprecated
    public void setInv(Inventory inventory) {
        attach(inventory);
    }
}
