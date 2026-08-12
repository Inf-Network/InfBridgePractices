package net.infnetwork.snowball.bridgingskin;

import java.util.LinkedHashSet;
import java.util.UUID;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.event.inventory.InventoryMoveItemEvent;
import org.bukkit.inventory.Inventory;
import net.infnetwork.snowball.bridginganalyzer.api.BridgingAnalyzerAPI;
import net.infnetwork.snowball.bridgingskin.data.PlayerSkin;
import net.infnetwork.snowball.bridgingskin.data.SkinSet;

/** Owns all interactions for the player selector and administrator catalog. */
public final class SkinEditListener implements Listener {
    /**
     * LOWEST is intentional: the legacy listener on BridgingSkin runs at NORMAL and returns
     * when the event is already cancelled. This prevents it from treating navigation arrows
     * as skins while the main class is being migrated to SkinService.
     */
    @EventHandler(priority = EventPriority.LOWEST)
    public void onInventoryClick(InventoryClickEvent event) {
        Inventory top = event.getView().getTopInventory();
        Object holder = top.getHolder();
        if (!(holder instanceof SkinSelectHolder) && !(holder instanceof SkinEditHolder)) {
            return;
        }

        // Cancel bottom-inventory moves, shift-click, hotbar swaps, collect-to-cursor and all
        // creative variants as well. Menu actions are routed only by the holder's slot map.
        event.setCancelled(true);
        if (!(event.getWhoClicked() instanceof Player player)) {
            return;
        }
        int rawSlot = event.getRawSlot();
        if (rawSlot < 0 || rawSlot >= top.getSize()) {
            return;
        }
        if (event.getClick() != ClickType.LEFT) {
            return;
        }

        if (holder instanceof SkinSelectHolder selectHolder) {
            handleSelection(player, rawSlot, selectHolder);
        } else {
            handleAdminEdit(player, rawSlot, (SkinEditHolder) holder);
        }
    }

    @EventHandler(priority = EventPriority.LOWEST)
    public void onInventoryDrag(InventoryDragEvent event) {
        Object holder = event.getView().getTopInventory().getHolder();
        if (holder instanceof SkinSelectHolder || holder instanceof SkinEditHolder) {
            event.setCancelled(true);
        }
    }

    /** Keep later plugins from re-enabling movement after the LOWEST router cancelled it. */
    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = false)
    public void enforceClickCancellation(InventoryClickEvent event) {
        Object holder = event.getView().getTopInventory().getHolder();
        if (holder instanceof SkinSelectHolder || holder instanceof SkinEditHolder) {
            event.setCancelled(true);
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = false)
    public void enforceDragCancellation(InventoryDragEvent event) {
        Object holder = event.getView().getTopInventory().getHolder();
        if (holder instanceof SkinSelectHolder || holder instanceof SkinEditHolder) {
            event.setCancelled(true);
        }
    }

    @EventHandler(priority = EventPriority.LOWEST)
    public void onInventoryMove(InventoryMoveItemEvent event) {
        if (event.getSource().getHolder() instanceof SkinSelectHolder
                || event.getDestination().getHolder() instanceof SkinSelectHolder
                || event.getSource().getHolder() instanceof SkinEditHolder
                || event.getDestination().getHolder() instanceof SkinEditHolder) {
            event.setCancelled(true);
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = false)
    public void enforceMoveCancellation(InventoryMoveItemEvent event) {
        if (event.getSource().getHolder() instanceof SkinSelectHolder
                || event.getDestination().getHolder() instanceof SkinSelectHolder
                || event.getSource().getHolder() instanceof SkinEditHolder
                || event.getDestination().getHolder() instanceof SkinEditHolder) {
            event.setCancelled(true);
        }
    }

    private void handleSelection(Player player, int rawSlot, SkinSelectHolder holder) {
        if (!player.getUniqueId().equals(holder.playerUuid())) {
            return;
        }
        if (rawSlot == SkinSelectHolder.PREVIOUS_SLOT && holder.page() > 0) {
            if (holder.claimAction()) {
                scheduleWhileViewing(player, holder, () -> {
                    holder.releaseAction();
                    SkinSelectCommand.openPage(player, holder.page() - 1);
                });
            }
            return;
        }
        if (rawSlot == SkinSelectHolder.NEXT_SLOT
                && holder.page() + 1 < holder.totalPages()) {
            if (holder.claimAction()) {
                scheduleWhileViewing(player, holder, () -> {
                    holder.releaseAction();
                    SkinSelectCommand.openPage(player, holder.page() + 1);
                });
            }
            return;
        }

        Material selected = holder.materialAt(rawSlot);
        if (selected == null) {
            return;
        }
        if (!holder.claimAction()) {
            return;
        }
        try {
            if (!persistSelection(player, selected)) {
                NetworkMessages.send(player, "&c此皮肤已不在你的库存中，菜单已刷新.");
                scheduleWhileViewing(player, holder, () -> {
                    holder.releaseAction();
                    SkinSelectCommand.openPage(player, holder.page());
                });
                return;
            }
        } catch (RuntimeException exception) {
            holder.releaseAction();
            reportDatabaseFailure(player, "保存皮肤选择失败", exception);
            return;
        }

        BridgingSkin.getInstance().getServer().getScheduler().runTask(
                BridgingSkin.getInstance(),
                () -> finishSelection(player, holder));
    }

    private void handleAdminEdit(Player editor, int rawSlot, SkinEditHolder holder) {
        if (!editor.getUniqueId().equals(holder.editorUuid())
                || !editor.hasPermission("bridgingSkin.admin")) {
            return;
        }
        if (rawSlot == SkinEditHolder.PREVIOUS_SLOT && holder.page() > 0) {
            if (holder.claimAction()) {
                scheduleWhileViewing(editor, holder, () -> {
                    holder.releaseAction();
                    SkinEditCommand.openPage(
                            editor, holder.targetUuid(), holder.targetName(), holder.page() - 1);
                });
            }
            return;
        }
        if (rawSlot == SkinEditHolder.NEXT_SLOT
                && holder.page() + 1 < holder.totalPages()) {
            if (holder.claimAction()) {
                scheduleWhileViewing(editor, holder, () -> {
                    holder.releaseAction();
                    SkinEditCommand.openPage(
                            editor, holder.targetUuid(), holder.targetName(), holder.page() + 1);
                });
            }
            return;
        }

        Material material = holder.materialAt(rawSlot);
        if (material == null) {
            return;
        }
        boolean remove = holder.wasOwnedAtOpen(rawSlot);
        if (remove && material == Material.CUT_SANDSTONE) {
            NetworkMessages.send(editor, "&cCUT_SANDSTONE 是保底皮肤，不能移除.");
            return;
        }
        if (!holder.claimAction()) {
            return;
        }

        try {
            SkinService service = BridgingSkin.getSkinService();
            synchronized (service) {
                PlayerSkin latest = service.findByName(holder.targetName());
                if (!isSameTarget(latest, holder.targetUuid())) {
                    NetworkMessages.send(editor, "&c目标玩家数据已发生变化，请重新执行命令.");
                    scheduleWhileViewing(editor, holder, editor::closeInventory);
                    return;
                }
                // The click intent comes from the rendered snapshot. Adding an already-owned
                // skin is harmless, so a lottery unlock that occurred while this GUI was open
                // can never be accidentally interpreted as a removal.
                PlayerSkin edited = SkinEditCommand.copyWithChanges(latest, material, remove);
                service.save(edited);
            }
        } catch (RuntimeException exception) {
            holder.releaseAction();
            reportDatabaseFailure(editor, "保存管理员皮肤修改失败", exception);
            return;
        }

        NetworkMessages.send(editor,
                remove
                        ? "&e已移除 " + holder.targetName() + " 的皮肤 " + material.name()
                        : "&a已添加 " + holder.targetName() + " 的皮肤 " + material.name());
        scheduleWhileViewing(editor, holder, () -> {
            holder.releaseAction();
            SkinEditCommand.openPage(
                    editor, holder.targetUuid(), holder.targetName(), holder.page());
        });
    }

    /** Build a detached candidate so a failed repository save cannot mutate the live cache. */
    private static boolean persistSelection(Player player, Material selected) {
        SkinService service = BridgingSkin.getSkinService();
        synchronized (service) {
            PlayerSkin latest = service.getOrCreate(player);
            SkinSet selectedSkin = new SkinSet(selected.name());
            if (!latest.allSkin.contains(selectedSkin)) {
                return false;
            }
            LinkedHashSet<SkinSet> copiedSkins = new LinkedHashSet<>();
            for (SkinSet entry : latest.allSkin) {
                copiedSkins.add(new SkinSet(entry.material));
            }
            PlayerSkin candidate = new PlayerSkin(
                    latest.player,
                    latest.uuid,
                    selectedSkin,
                    copiedSkins);
            service.save(candidate);
            return true;
        }
    }

    private static void finishSelection(Player player, SkinSelectHolder holder) {
        if (!player.isOnline()) {
            return;
        }
        if (player.getOpenInventory().getTopInventory().getHolder() == holder) {
            player.closeInventory();
        }
        try {
            BridgingAnalyzerAPI.refreshItem(player);
            NetworkMessages.send(player, "&a你的搭路皮肤已更换");
        } catch (RuntimeException exception) {
            BridgingSkin.getInstance().getLogger().warning(
                    "已保存 " + player.getName() + " 的皮肤，但刷新练习物品失败: "
                            + exception.getMessage());
            NetworkMessages.send(player, "&e皮肤已保存，但练习物品刷新失败；下次重生会自动生效.");
        }
    }

    private static void scheduleWhileViewing(Player player, Object expectedHolder, Runnable action) {
        BridgingSkin.getInstance().getServer().getScheduler().runTask(
                BridgingSkin.getInstance(),
                () -> {
                    if (player.isOnline()
                            && player.getOpenInventory().getTopInventory().getHolder()
                            == expectedHolder) {
                        action.run();
                    }
                });
    }

    private static boolean isSameTarget(PlayerSkin skin, UUID expectedUuid) {
        if (skin == null || skin.uuid == null) {
            return false;
        }
        try {
            return expectedUuid.equals(UUID.fromString(skin.uuid));
        } catch (IllegalArgumentException exception) {
            return false;
        }
    }

    private static void reportDatabaseFailure(
            Player player,
            String operation,
            RuntimeException exception
    ) {
        BridgingSkin.getInstance().getLogger().severe(
                operation + " (" + player.getName() + "): " + exception.getMessage());
        NetworkMessages.send(player, "&c" + operation + "，数据库没有确认本次修改.");
    }
}
