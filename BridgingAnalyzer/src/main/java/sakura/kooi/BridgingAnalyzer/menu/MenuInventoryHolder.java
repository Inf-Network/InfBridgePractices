package sakura.kooi.BridgingAnalyzer.menu;

import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.jetbrains.annotations.NotNull;

/** Per-open inventory identity; it deliberately retains UUIDs rather than Player wrappers. */
final class MenuInventoryHolder implements InventoryHolder {
    private final MenuAction.Screen screen;
    private final UUID owner;
    private final UUID sessionId = UUID.randomUUID();
    private final Map<Integer, MenuEntry> actions;
    private Inventory inventory;

    MenuInventoryHolder(MenuAction.Screen screen, UUID owner, Map<Integer, MenuEntry> actions) {
        this.screen = screen;
        this.owner = owner;
        this.actions = Map.copyOf(actions);
    }

    MenuAction.Screen screen() {
        return screen;
    }

    UUID owner() {
        return owner;
    }

    UUID sessionId() {
        return sessionId;
    }

    Optional<MenuEntry> action(int rawSlot, MenuButton button) {
        MenuEntry entry = actions.get(rawSlot);
        return entry != null && entry.binding().accepts(button)
                ? Optional.of(entry)
                : Optional.empty();
    }

    void bind(Inventory inventory) {
        if (this.inventory != null) {
            throw new IllegalStateException("菜单容器已绑定");
        }
        this.inventory = inventory;
    }

    boolean isBoundTo(Inventory inventory) {
        return this.inventory == inventory;
    }

    @Override
    public @NotNull Inventory getInventory() {
        if (inventory == null) {
            throw new IllegalStateException("菜单容器尚未绑定");
        }
        return inventory;
    }
}
