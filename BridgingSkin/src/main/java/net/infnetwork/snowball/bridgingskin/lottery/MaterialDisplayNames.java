package net.infnetwork.snowball.bridgingskin.lottery;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.TranslatableComponent;
import net.kyori.adventure.text.format.TextColor;
import org.bukkit.Material;

final class MaterialDisplayNames {
    private MaterialDisplayNames() {
    }

    static TranslatableComponent translated(Material material, TextColor color) {
        return Component.translatable(blockTranslationKey(material), color);
    }

    static String blockTranslationKey(Material material) {
        return "block."
                + material.getKey().getNamespace()
                + "."
                + material.getKey().getKey();
    }
}
