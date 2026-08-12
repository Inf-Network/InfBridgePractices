package sakura.kooi.BridgingSkin.lottery;

import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.jetbrains.annotations.NotNull;
import java.util.UUID;

final class LotteryInventoryHolder implements InventoryHolder {
    enum Screen {
        MENU,
        ANIMATION,
        RESULT
    }

    private final Screen screen;
    private final UUID owner;
    private final UUID sessionId;
    private Inventory inventory;

    LotteryInventoryHolder(Screen screen, UUID owner) {
        this.screen = screen;
        this.owner = owner;
        this.sessionId = UUID.randomUUID();
    }

    Screen screen() {
        return screen;
    }

    UUID owner() {
        return owner;
    }

    UUID sessionId() {
        return sessionId;
    }

    void bind(Inventory inventory) {
        if (this.inventory != null) {
            throw new IllegalStateException("抽奖容器已绑定");
        }
        this.inventory = inventory;
    }

    @Override
    public @NotNull Inventory getInventory() {
        if (inventory == null) {
            throw new IllegalStateException("抽奖容器尚未绑定");
        }
        return inventory;
    }
}
