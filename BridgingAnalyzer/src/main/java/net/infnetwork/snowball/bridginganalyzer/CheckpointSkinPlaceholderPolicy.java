package net.infnetwork.snowball.bridginganalyzer;

import java.util.Objects;
import java.util.function.Supplier;
import org.bukkit.Material;

/** Stateful one-kit policy that resolves the skin placeholder at most once. */
final class CheckpointSkinPlaceholderPolicy {
    private static final Material PLACEHOLDER = Material.SANDSTONE;

    private final Supplier<Material> skinSupplier;
    private boolean attempted;
    private Material selectedSkin;
    private RuntimeException failure;

    CheckpointSkinPlaceholderPolicy(Supplier<Material> skinSupplier) {
        this.skinSupplier = Objects.requireNonNull(skinSupplier, "skinSupplier");
    }

    Material resolve(Material original) {
        Objects.requireNonNull(original, "original");
        if (original != PLACEHOLDER) {
            return original;
        }
        if (!attempted) {
            attempted = true;
            try {
                Material supplied = skinSupplier.get();
                // Compare constants here instead of Material#isAir(): the latter needs
                // Paper's runtime registry and makes this pure policy impossible to test
                // outside a running server.
                if (supplied == null || supplied == Material.AIR
                        || supplied == Material.CAVE_AIR || supplied == Material.VOID_AIR) {
                    throw new IllegalStateException("方块皮肤提供器返回了无效方块物品");
                }
                selectedSkin = supplied;
            } catch (RuntimeException exception) {
                failure = exception;
            }
        }
        return selectedSkin == null ? original : selectedSkin;
    }

    RuntimeException failure() {
        return failure;
    }
}
