package sakura.kooi.BridgingSkin;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import sakura.kooi.BridgingSkin.data.PlayerSkin;

/** Opens the UUID-bound, paged player skin selector. */
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
                    "§6§l皮肤库存 §8(" + (page + 1) + "/" + totalPages + ")");
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
        meta.setDisplayName((selected ? "§a§l当前皮肤 §f" : "§e") + material.name());
        meta.setLore(List.of(selected ? "§a正在使用" : "§7点击选择此皮肤"));
        meta.setEnchantmentGlintOverride(selected);
        item.setItemMeta(meta);
        return item;
    }

    static void decorateNavigation(Inventory inventory, int page, int totalPages) {
        ItemStack filler = namedItem(Material.GRAY_STAINED_GLASS_PANE, "§8");
        for (int slot = SkinSelectHolder.CONTENT_SIZE; slot < INVENTORY_SIZE; slot++) {
            inventory.setItem(slot, filler);
        }
        if (page > 0) {
            inventory.setItem(SkinSelectHolder.PREVIOUS_SLOT,
                    namedItem(Material.ARROW, "§e上一页"));
        }
        inventory.setItem(SkinSelectHolder.PAGE_SLOT,
                namedItem(Material.PAPER, "§b第 " + (page + 1) + " / " + totalPages + " 页"));
        if (page + 1 < totalPages) {
            inventory.setItem(SkinSelectHolder.NEXT_SLOT,
                    namedItem(Material.ARROW, "§e下一页"));
        }
    }

    static ItemStack namedItem(Material material, String name) {
        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();
        meta.setDisplayName(name);
        item.setItemMeta(meta);
        return item;
    }
}
