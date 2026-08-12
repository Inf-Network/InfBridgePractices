package sakura.kooi.BridgingSkin;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
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
import sakura.kooi.BridgingSkin.data.SkinDataSanitizer;
import sakura.kooi.BridgingSkin.data.SkinSet;

/** Administrator commands for the paged, click-to-toggle skin catalog. */
public final class SkinEditCommand implements CommandExecutor {
    private static final int INVENTORY_SIZE = 54;
    private static final List<Material> EDITABLE_CATALOG = buildCatalog();

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player player)) {
            NetworkMessages.send(sender, "&c仅玩家可以执行此命令.");
            return true;
        }
        if (!sender.hasPermission("bridgingSkin.admin")) {
            NetworkMessages.send(sender, "&c你没有权限执行此命令.");
            return true;
        }
        if (args.length == 0) {
            sendUsage(sender);
            return true;
        }

        switch (args[0].toLowerCase(Locale.ROOT)) {
            case "edit" -> {
                if (args.length < 2) {
                    sendUsage(sender);
                    return true;
                }
                try {
                    PlayerSkin target = BridgingSkin.getSkinService().findByName(args[1]);
                    if (target == null) {
                        NetworkMessages.send(sender, "&c玩家 " + args[1] + " 没有皮肤数据或名称不唯一.");
                        return true;
                    }
                    openPage(player, UUID.fromString(target.uuid), target.player, 0);
                } catch (RuntimeException exception) {
                    reportFailure(sender, "读取玩家皮肤失败", exception);
                }
                return true;
            }
            case "clear" -> {
                if (args.length < 2) {
                    sendUsage(sender);
                    return true;
                }
                Material material = Material.matchMaterial(args[1]);
                if (material == null) {
                    NetworkMessages.send(sender,
                            "&cMaterial " + args[1].toUpperCase(Locale.ROOT) + " 不存在");
                    return true;
                }
                if (material == Material.CUT_SANDSTONE) {
                    NetworkMessages.send(sender, "&cCUT_SANDSTONE 是保底皮肤，禁止全局清除.");
                    return true;
                }
                // SkinService owns both the database transaction and cache update. Running this
                // on the server thread avoids racing live GUI/lottery mutations.
                new ClearThread(sender, material).run();
                return true;
            }
            default -> {
                NetworkMessages.send(sender, "&c未知子命令: &f" + args[0]);
                sendUsage(sender);
                return true;
            }
        }
    }

    public static void openPage(
            Player editor,
            UUID targetUuid,
            String targetName,
            int requestedPage
    ) {
        try {
            SkinService service = BridgingSkin.getSkinService();
            PlayerSkin target = service.findByName(targetName);
            if (target == null || !targetUuid.toString().equals(target.uuid)) {
                NetworkMessages.send(editor, "&c目标玩家数据已发生变化，请重新执行命令.");
                editor.closeInventory();
                return;
            }

            Set<Material> owned = materialSet(target.allSkin);
            int totalPages = Math.max(1,
                    (EDITABLE_CATALOG.size() + SkinEditHolder.CONTENT_SIZE - 1)
                            / SkinEditHolder.CONTENT_SIZE);
            int page = Math.max(0, Math.min(requestedPage, totalPages - 1));
            int fromIndex = page * SkinEditHolder.CONTENT_SIZE;
            int toIndex = Math.min(fromIndex + SkinEditHolder.CONTENT_SIZE,
                    EDITABLE_CATALOG.size());

            Map<Integer, Material> slots = new LinkedHashMap<>();
            Map<Integer, Boolean> ownershipBySlot = new LinkedHashMap<>();
            for (int index = fromIndex; index < toIndex; index++) {
                int slot = index - fromIndex;
                Material material = EDITABLE_CATALOG.get(index);
                slots.put(slot, material);
                ownershipBySlot.put(slot, owned.contains(material));
            }

            SkinEditHolder holder = new SkinEditHolder(
                    editor.getUniqueId(), targetUuid, target.player, page, totalPages,
                    slots, ownershipBySlot);
            Inventory inventory = Bukkit.createInventory(
                    holder,
                    INVENTORY_SIZE,
                    "§6§l编辑皮肤: §f" + target.player + " §8(" + (page + 1)
                            + "/" + totalPages + ")");
            holder.attach(inventory);

            String selected = target.currentSkin == null ? "" : target.currentSkin.material;
            slots.forEach((slot, material) -> inventory.setItem(slot,
                    catalogItem(material, owned.contains(material), material.name().equals(selected))));
            decorateNavigation(inventory, page, totalPages);
            editor.openInventory(inventory);
        } catch (RuntimeException exception) {
            reportFailure(editor, "打开皮肤编辑菜单失败", exception);
        }
    }

    private static ItemStack catalogItem(Material material, boolean owned, boolean selected) {
        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();
        String status = owned ? "§a§l已拥有" : "§7未拥有";
        meta.setDisplayName(status + " §f" + material.name());
        if (material == Material.CUT_SANDSTONE) {
            meta.setLore(List.of("§e保底皮肤，不能移除", selected ? "§b当前正在使用" : "§7"));
        } else if (owned) {
            meta.setLore(List.of("§c点击移除此皮肤", selected ? "§b当前正在使用" : "§7"));
        } else {
            meta.setLore(List.of("§a点击添加此皮肤"));
        }
        meta.setEnchantmentGlintOverride(owned);
        item.setItemMeta(meta);
        return item;
    }

    private static void decorateNavigation(Inventory inventory, int page, int totalPages) {
        ItemStack filler = SkinSelectCommand.namedItem(Material.GRAY_STAINED_GLASS_PANE, "§8");
        for (int slot = SkinEditHolder.CONTENT_SIZE; slot < INVENTORY_SIZE; slot++) {
            inventory.setItem(slot, filler);
        }
        if (page > 0) {
            inventory.setItem(SkinEditHolder.PREVIOUS_SLOT,
                    SkinSelectCommand.namedItem(Material.ARROW, "§e上一页"));
        }
        inventory.setItem(SkinEditHolder.PAGE_SLOT,
                SkinSelectCommand.namedItem(
                        Material.PAPER, "§b第 " + (page + 1) + " / " + totalPages + " 页"));
        if (page + 1 < totalPages) {
            inventory.setItem(SkinEditHolder.NEXT_SLOT,
                    SkinSelectCommand.namedItem(Material.ARROW, "§e下一页"));
        }
    }

    static PlayerSkin copyWithChanges(
            PlayerSkin current,
            Material material,
            boolean remove
    ) {
        LinkedHashSet<SkinSet> updated = new LinkedHashSet<>();
        for (SkinSet entry : current.allSkin) {
            updated.add(new SkinSet(entry.material));
        }
        SkinSet changed = new SkinSet(material.name());
        if (remove) {
            updated.remove(changed);
        } else {
            updated.add(changed);
        }

        String selected = current.currentSkin == null
                ? SkinDataSanitizer.DEFAULT_MATERIAL
                : current.currentSkin.material;
        if (remove && material.name().equals(selected)) {
            selected = SkinDataSanitizer.DEFAULT_MATERIAL;
        }
        return new PlayerSkin(
                current.player,
                current.uuid,
                new SkinSet(selected),
                updated);
    }

    static Set<Material> materialSet(Iterable<SkinSet> skins) {
        LinkedHashSet<Material> materials = new LinkedHashSet<>();
        for (SkinSet skin : skins) {
            Material material = skin == null ? null : Material.matchMaterial(skin.material);
            if (material != null) {
                materials.add(material);
            }
        }
        return materials;
    }

    private static List<Material> buildCatalog() {
        List<Material> materials = new ArrayList<>();
        for (Material material : Material.values()) {
            if (!material.isLegacy()
                    && material.isBlock()
                    && material.isItem()
                    && SkinDataSanitizer.validMaterialName(material.name()) != null) {
                materials.add(material);
            }
        }
        materials.sort(Comparator.comparing(Material::name));
        return List.copyOf(materials);
    }

    private static void sendUsage(CommandSender sender) {
        NetworkMessages.send(sender,
                "&e/bskin-edit edit <player>",
                "&e/bskin-edit clear <material>");
    }

    private static void reportFailure(
            CommandSender sender,
            String operation,
            RuntimeException exception
    ) {
        BridgingSkin.getInstance().getLogger().severe(operation + ": " + exception.getMessage());
        NetworkMessages.send(sender, "&c" + operation + "，数据库没有确认本次修改.");
    }
}
