package net.infnetwork.snowball.bridgingskin;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.TranslatableComponent;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.Material;

final class SkinItemComponents {
    private SkinItemComponents() {
    }

    static TranslatableComponent materialName(Material material, TextColor color) {
        return Component.translatable(blockTranslationKey(material), color);
    }

    static Component adminCatalogName(Material material, boolean owned) {
        Component status = owned
                ? Component.text("已拥有", NamedTextColor.GREEN, TextDecoration.BOLD)
                : Component.text("未拥有", NamedTextColor.GRAY);
        return Component.empty()
                .append(status)
                .append(Component.space())
                .append(materialName(material, NamedTextColor.WHITE));
    }

    static String blockTranslationKey(Material material) {
        return "block."
                + material.getKey().getNamespace()
                + "."
                + material.getKey().getKey();
    }
}
