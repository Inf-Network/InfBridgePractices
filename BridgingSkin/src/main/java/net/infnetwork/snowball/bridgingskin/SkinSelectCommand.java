package net.infnetwork.snowball.bridgingskin;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import net.infnetwork.snowball.bridgingskin.data.PlayerSkin;

public final class SkinSelectCommand implements CommandExecutor {
    private static final int INVENTORY_SIZE = 54;

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player player)) {
            NetworkMessages.send(sender, "&c仅玩家可以执行.");
            return true;
        }
        openPage(player, 0);
        return true;
    }

    public static void openPage(Player player, int requestedPage) {
        try {
            SkinService service = BridgingSkin.getSkinService();
            PlayerSkin skin = service.getOrCreate(player);
            List<Material> materials = new ArrayList<>(service.ownedMaterials(player));
            materials.sort(Comparator.comparing(Material::name));

            int totalPages = Math.max(1,
                    (materials.size() + SkinSelectHolder.CONTENT_SIZE - 1)
                            / SkinSelectHolder.CONTENT_SIZE);
            int page = Math.max(0, Math.min(requestedPage, totalPages - 1));
            int fromIndex = page * SkinSelectHolder.CONTENT_SIZE;
            int toIndex = Math.min(fromIndex + SkinSelectHolder.CONTENT_SIZE, materials.size());

            Map<Integer, Material> slots = new LinkedHashMap<>();
            for (int index = fromIndex; index < toIndex; index++) {
                slots.put(index - fromIndex, materials.get(index));
            }

            SkinSelectHolder holder = new SkinSelectHolder(
                    player.getUniqueId(), page, totalPages, slots);
            Inventory inventory = Bukkit.createInventory(
                    holder,
                    INVENTORY_SIZE,
                    Component.empty()
                            .append(Component.text(
                                    "皮肤库存 ", NamedTextColor.GOLD, TextDecoration.BOLD))
                            .append(Component.text(
                                    "(" + (page + 1) + "/" + totalPages + ")",
                                    NamedTextColor.DARK_GRAY)));
            holder.attach(inventory);

            String currentMaterial = skin.currentSkin == null ? "" : skin.currentSkin.material;
            slots.forEach((slot, material) -> inventory.setItem(
                    slot, skinItem(material, material.name().equals(currentMaterial))));
            decorateNavigation(inventory, page, totalPages);
            player.openInventory(inventory);
        } catch (RuntimeException exception) {
            BridgingSkin.getInstance().getLogger().severe(
                    "无法打开 " + player.getName() + " 的皮肤菜单: " + exception.getMessage());
            NetworkMessages.send(player, "&c皮肤数据读取失败，请稍后重试.");
        }
    }

    private static ItemStack skinItem(Material material, boolean selected) {
        ItemStack item = new ItemStack(material, 64);
        ItemMeta meta = item.getItemMeta();
        Component name = selected
                ? itemComponent(Component.empty()
                        .append(Component.text(
                                "当前皮肤 ", NamedTextColor.GREEN, TextDecoration.BOLD))
                        .append(Component.text(material.name(), NamedTextColor.WHITE)))
                : itemComponent(Component.text(material.name(), NamedTextColor.YELLOW));
        meta.displayName(name);
        meta.lore(List.of(itemComponent(Component.text(
                selected ? "正在使用" : "点击选择此皮肤",
                selected ? NamedTextColor.GREEN : NamedTextColor.GRAY))));
        meta.setEnchantmentGlintOverride(selected);
        item.setItemMeta(meta);
        return item;
    }

    static void decorateNavigation(Inventory inventory, int page, int totalPages) {
        ItemStack filler = namedItem(Material.GRAY_STAINED_GLASS_PANE, Component.empty());
        for (int slot = SkinSelectHolder.CONTENT_SIZE; slot < INVENTORY_SIZE; slot++) {
            inventory.setItem(slot, filler);
        }
        if (page > 0) {
            inventory.setItem(SkinSelectHolder.PREVIOUS_SLOT,
                    namedItem(Material.ARROW,
                            Component.text("上一页", NamedTextColor.YELLOW)));
        }
        inventory.setItem(SkinSelectHolder.PAGE_SLOT,
                namedItem(Material.PAPER, Component.text(
                        "第 " + (page + 1) + " / " + totalPages + " 页",
                        NamedTextColor.AQUA)));
        if (page + 1 < totalPages) {
            inventory.setItem(SkinSelectHolder.NEXT_SLOT,
                    namedItem(Material.ARROW,
                            Component.text("下一页", NamedTextColor.YELLOW)));
        }
    }

    static ItemStack namedItem(Material material, Component name) {
        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();
        meta.displayName(itemComponent(name));
        item.setItemMeta(meta);
        return item;
    }

    static Component itemComponent(Component component) {
        return component;
    }
}
