package net.infnetwork.snowball.bridginganalyzer.menu;

import java.util.List;
import java.util.Objects;
import org.bukkit.Material;

record MenuEntry(String id, int slot, Material material, String displayName,
                 List<String> lore, String permission, MenuBinding binding) {
    MenuEntry {
        if (id == null || !id.matches("[a-z0-9_-]+")) {
            throw new IllegalArgumentException("菜单项 id 无效: " + id);
        }
        if (slot < 0 || slot >= 54) {
            throw new IllegalArgumentException("菜单槽位越界: " + slot);
        }
        Objects.requireNonNull(material, "material");
        if (material == Material.AIR || material.name().startsWith("LEGACY_")) {
            throw new IllegalArgumentException("菜单图标不是有效物品: " + material);
        }
        if (displayName == null || displayName.isBlank()) {
            throw new IllegalArgumentException("菜单项名称不能为空: " + id);
        }
        lore = List.copyOf(lore);
        permission = permission == null ? "" : permission.strip();
    }
}
