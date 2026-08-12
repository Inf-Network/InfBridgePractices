package sakura.kooi.BridgingAnalyzer;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.function.Supplier;
import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;

/** Converts detached checkpoint-chest contents into the kit given to a player. */
final class CheckpointLoadoutResolver {
    private static final Material SKIN_PLACEHOLDER = Material.SANDSTONE;

    private CheckpointLoadoutResolver() {
    }

    static Resolution resolve(ItemStack[] source, Supplier<ItemStack> skinSupplier) {
        Objects.requireNonNull(source, "source");
        Objects.requireNonNull(skinSupplier, "skinSupplier");

        List<ItemStack> items = new ArrayList<>();
        CheckpointSkinPlaceholderPolicy placeholderPolicy = new CheckpointSkinPlaceholderPolicy(() -> {
            ItemStack supplied = skinSupplier.get();
            if (supplied == null || supplied.getType().isAir()
                    || !supplied.getType().isBlock() || !supplied.getType().isItem()) {
                throw new IllegalStateException("方块皮肤提供器返回了无效方块物品");
            }
            return supplied.getType();
        });

        for (ItemStack sourceStack : source) {
            if (sourceStack == null || sourceStack.getType().isAir()) {
                continue;
            }

            ItemStack resolved = sourceStack.clone();
            if (sourceStack.getType() == SKIN_PLACEHOLDER) {
                Material selectedSkin = placeholderPolicy.resolve(sourceStack.getType());
                if (selectedSkin != SKIN_PLACEHOLDER) {
                    // Preserve the preset stack's amount and generic item metadata;
                    // only its material acts as the skin placeholder.
                    resolved.setType(selectedSkin);
                }
            }
            items.add(resolved);
        }

        return new Resolution(List.copyOf(items), placeholderPolicy.failure());
    }

    record Resolution(List<ItemStack> items, RuntimeException skinFailure) {
    }
}
