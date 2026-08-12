package net.infnetwork.snowball.bridgingskin;

import java.util.Map;
import java.util.UUID;
import org.bukkit.Material;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import net.infnetwork.snowball.bridgingskin.data.PlayerSkin;

/** Slot routing for the paged, click-to-toggle administrator skin catalog. */
public final class SkinEditHolder implements InventoryHolder {
    public static final int CONTENT_SIZE = 45;
    public static final int PREVIOUS_SLOT = 45;
    public static final int PAGE_SLOT = 49;
    public static final int NEXT_SLOT = 53;

    private final UUID editorUuid;
    private final UUID targetUuid;
    private final String targetName;
    private final int page;
    private final int totalPages;
    private final Map<Integer, Material> materialsBySlot;
    private final Map<Integer, Boolean> ownedBySlot;
    private Inventory inventory;
    private boolean actionClaimed;
    private PlayerSkin legacySkins;

    public SkinEditHolder(
            UUID editorUuid,
            UUID targetUuid,
            String targetName,
            int page,
            int totalPages,
            Map<Integer, Material> materialsBySlot,
            Map<Integer, Boolean> ownedBySlot
    ) {
        this.editorUuid = editorUuid;
        this.targetUuid = targetUuid;
        this.targetName = targetName;
        this.page = page;
        this.totalPages = totalPages;
        this.materialsBySlot = Map.copyOf(materialsBySlot);
        this.ownedBySlot = Map.copyOf(ownedBySlot);
    }

    /** @deprecated Binary compatibility for extensions compiled against v3. */
    @Deprecated
    public SkinEditHolder(PlayerSkin skins) {
        this(new UUID(0L, 0L), legacyUuid(skins), skins.player,
                0, 1, Map.of(), Map.of());
        this.legacySkins = skins;
    }

    public UUID editorUuid() {
        return editorUuid;
    }

    public UUID targetUuid() {
        return targetUuid;
    }

    public String targetName() {
        return targetName;
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

    /** Ownership state rendered to the editor; this defines the click's intent. */
    public boolean wasOwnedAtOpen(int rawSlot) {
        return ownedBySlot.getOrDefault(rawSlot, false);
    }

    /** Ensures one rendered ownership state can produce at most one database mutation. */
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
            throw new IllegalStateException("SkinEditHolder 已绑定 Inventory");
        }
        this.inventory = inventory;
    }

    /** @deprecated Legacy holder payload; new menus route by UUID. */
    @Deprecated
    public PlayerSkin getSkins() {
        return legacySkins;
    }

    /** @deprecated Use {@link #attach(Inventory)}. */
    @Deprecated
    public void setInv(Inventory inventory) {
        attach(inventory);
    }

    private static UUID legacyUuid(PlayerSkin skins) {
        try {
            return UUID.fromString(skins.uuid);
        } catch (RuntimeException ignored) {
            return new UUID(0L, 0L);
        }
    }
}
