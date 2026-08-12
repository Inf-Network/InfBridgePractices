package sakura.kooi.BridgingAnalyzer.menuentry;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import java.util.function.Consumer;
import java.util.logging.Level;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.HandlerList;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.event.player.PlayerChangedWorldEvent;
import org.bukkit.event.player.PlayerDropItemEvent;
import org.bukkit.event.player.PlayerGameModeChangeEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerKickEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.player.PlayerRespawnEvent;
import org.bukkit.event.player.PlayerSwapHandItemsEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitTask;
import sakura.kooi.BridgingAnalyzer.utils.NetworkMessages;

/**
 * Owns the fixed hotbar entry for the native practice menu.
 *
 * <p>The item is identified only by a plugin PDC key. A normal nether star is
 * therefore never captured, deleted or made immovable. Slot {@value #MENU_SLOT}
 * is reserved without destroying its previous contents: an existing item is
 * moved to another storage slot first, and a completely full inventory is left
 * untouched.</p>
 */
public final class MenuEntryService implements Listener, AutoCloseable {
    public static final int MENU_SLOT = 8;
    public static final String ENTRY_PERMISSION = "bridginganalyzer.menu.item";

    private static final int STORAGE_SIZE = 36;
    private static final long RECONCILE_PERIOD_TICKS = 20L;

    private final JavaPlugin plugin;
    private final Consumer<Player> mainMenuOpener;
    private final String entryPermission;
    private final NamespacedKey menuEntryKey;
    private final Map<UUID, BukkitTask> pendingEnsures = new HashMap<>();
    private final Set<UUID> warnedFullInventories = new HashSet<>();
    private final BukkitTask reconcileTask;
    private boolean closed;

    public MenuEntryService(JavaPlugin plugin, Consumer<Player> mainMenuOpener) {
        this(plugin, mainMenuOpener, ENTRY_PERMISSION);
    }

    public MenuEntryService(JavaPlugin plugin, Consumer<Player> mainMenuOpener,
                            String entryPermission) {
        this.plugin = Objects.requireNonNull(plugin, "plugin");
        this.mainMenuOpener = Objects.requireNonNull(mainMenuOpener, "mainMenuOpener");
        this.entryPermission = Objects.requireNonNull(entryPermission, "entryPermission").strip();
        if (this.entryPermission.isEmpty()) {
            throw new IllegalArgumentException("菜单入口权限不能为空");
        }
        this.menuEntryKey = new NamespacedKey(plugin, "main_menu_entry");
        this.reconcileTask = plugin.getServer().getScheduler().runTaskTimer(
                plugin, this::reconcileOnlinePlayers,
                RECONCILE_PERIOD_TICKS, RECONCILE_PERIOD_TICKS);
    }

    /**
     * Put exactly one canonical entry in hotbar slot 8 when the player may use it.
     *
     * @return true when slot 8 contains the menu entry after this call
     */
    public boolean ensure(Player player) {
        Objects.requireNonNull(player, "player");
        if (closed) {
            return false;
        }
        UUID playerId = player.getUniqueId();
        PlayerInventory inventory = player.getInventory();

        if (!player.hasPermission(entryPermission)) {
            removeAllMenuEntries(inventory);
            warnedFullInventories.remove(playerId);
            return false;
        }

        ItemStack reservedSlot = inventory.getItem(MENU_SLOT);
        ItemStack canonicalEntry = createMenuItem();
        if (isMenuItem(reservedSlot)) {
            removeDuplicateMenuEntries(inventory);
            if (reservedSlot.getAmount() != 1 || !reservedSlot.isSimilar(canonicalEntry)) {
                inventory.setItem(MENU_SLOT, canonicalEntry);
            }
            warnedFullInventories.remove(playerId);
            return true;
        }
        if (!isMenuItem(reservedSlot) && !isEmpty(reservedSlot)) {
            int relocationSlot = findRelocationSlot(inventory);
            if (relocationSlot < 0) {
                // Removing system-owned duplicates outside storage is safe, but never
                // overwrite or drop the player's item merely to make room for the entry.
                removeMenuEntriesOutsideStorage(inventory);
                if (warnedFullInventories.add(playerId)) {
                    NetworkMessages.send(player,
                            "&e快捷菜单入口无法固定到第 9 格：背包已满，请先腾出一个空位。");
                }
                return false;
            }

            ItemStack displaced = reservedSlot.clone();
            removeAllMenuEntries(inventory);
            inventory.setItem(relocationSlot, displaced);
        } else {
            removeAllMenuEntries(inventory);
        }

        inventory.setItem(MENU_SLOT, canonicalEntry);
        warnedFullInventories.remove(playerId);
        return true;
    }

    /** Return whether an item is this plugin's menu entry, independent of its display text. */
    public boolean isMenuItem(ItemStack item) {
        if (isEmpty(item) || !item.hasItemMeta()) {
            return false;
        }
        Byte marker = item.getItemMeta().getPersistentDataContainer()
                .get(menuEntryKey, PersistentDataType.BYTE);
        return marker != null && marker == (byte) 1;
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onJoin(PlayerJoinEvent event) {
        scheduleEnsure(event.getPlayer());
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onRespawn(PlayerRespawnEvent event) {
        // BridgingAnalyzer restores a death loadout on the following tick. This task
        // is queued after that listener and performs a final idempotent reservation.
        scheduleEnsure(event.getPlayer());
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onChangedWorld(PlayerChangedWorldEvent event) {
        scheduleEnsure(event.getPlayer());
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onGameModeChange(PlayerGameModeChangeEvent event) {
        scheduleEnsure(event.getPlayer());
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = false)
    public void onInteract(PlayerInteractEvent event) {
        if (event.getHand() != EquipmentSlot.HAND
                || !event.getAction().isRightClick()
                || !isMenuItem(event.getItem())) {
            return;
        }

        event.setCancelled(true);
        event.setUseInteractedBlock(Event.Result.DENY);
        event.setUseItemInHand(Event.Result.DENY);
        Player player = event.getPlayer();
        if (!player.hasPermission(entryPermission)) {
            ensure(player);
            return;
        }
        try {
            mainMenuOpener.accept(player);
        } catch (RuntimeException exception) {
            plugin.getLogger().log(Level.SEVERE,
                    "无法为 " + player.getName() + " 打开搭路练习菜单", exception);
            NetworkMessages.send(player, "&c菜单暂时无法打开，请稍后重试。");
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = false)
    public void onDrop(PlayerDropItemEvent event) {
        if (!isMenuItem(event.getItemDrop().getItemStack())) {
            return;
        }
        event.setCancelled(true);
        scheduleEnsure(event.getPlayer());
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = false)
    public void onInventoryClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player player)) {
            return;
        }

        boolean touchesEntry = isMenuItem(event.getCurrentItem())
                || isMenuItem(event.getCursor())
                || event.getHotbarButton() == MENU_SLOT
                && isMenuItem(player.getInventory().getItem(MENU_SLOT));
        if (!touchesEntry) {
            return;
        }

        event.setCancelled(true);
        scheduleEnsure(player);
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = false)
    public void onInventoryDrag(InventoryDragEvent event) {
        if (!(event.getWhoClicked() instanceof Player player)) {
            return;
        }

        boolean touchesEntry = isMenuItem(event.getOldCursor())
                || event.getNewItems().values().stream().anyMatch(this::isMenuItem)
                || event.getRawSlots().stream().anyMatch(rawSlot ->
                        rawSlot >= event.getView().getTopInventory().getSize()
                                && event.getView().convertSlot(rawSlot) == MENU_SLOT);
        if (!touchesEntry) {
            return;
        }

        event.setCancelled(true);
        scheduleEnsure(player);
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = false)
    public void onSwapHands(PlayerSwapHandItemsEvent event) {
        if (!isMenuItem(event.getMainHandItem()) && !isMenuItem(event.getOffHandItem())) {
            return;
        }
        event.setCancelled(true);
        scheduleEnsure(event.getPlayer());
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onQuit(PlayerQuitEvent event) {
        forget(event.getPlayer());
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onKick(PlayerKickEvent event) {
        forget(event.getPlayer());
    }

    /** Cancel owned scheduler work. Inventory entries deliberately survive reloads/restarts. */
    @Override
    public void close() {
        if (closed) {
            return;
        }
        closed = true;
        reconcileTask.cancel();
        for (BukkitTask task : pendingEnsures.values()) {
            task.cancel();
        }
        pendingEnsures.clear();
        warnedFullInventories.clear();
        HandlerList.unregisterAll(this);
    }

    private ItemStack createMenuItem() {
        ItemStack item = new ItemStack(Material.NETHER_STAR, 1);
        ItemMeta meta = item.getItemMeta();
        meta.displayName(Component.text("搭路练习菜单", NamedTextColor.AQUA)
                .decorate(TextDecoration.BOLD)
                .decoration(TextDecoration.ITALIC, false));
        meta.lore(List.of(Component.text("右键打开菜单", NamedTextColor.YELLOW)
                .decoration(TextDecoration.ITALIC, false)));
        meta.getPersistentDataContainer().set(menuEntryKey, PersistentDataType.BYTE, (byte) 1);
        item.setItemMeta(meta);
        return item;
    }

    private int findRelocationSlot(PlayerInventory inventory) {
        // Prefer the main inventory so reserving the menu slot does not disturb the
        // usual practice-item order in hotbar slots 0-7.
        for (int slot = 9; slot < STORAGE_SIZE; slot++) {
            ItemStack candidate = inventory.getItem(slot);
            if (isEmpty(candidate) || isMenuItem(candidate)) {
                return slot;
            }
        }
        for (int slot = 0; slot < MENU_SLOT; slot++) {
            ItemStack candidate = inventory.getItem(slot);
            if (isEmpty(candidate) || isMenuItem(candidate)) {
                return slot;
            }
        }
        return -1;
    }

    private void removeAllMenuEntries(PlayerInventory inventory) {
        ItemStack[] contents = inventory.getContents();
        for (int slot = 0; slot < contents.length; slot++) {
            if (isMenuItem(contents[slot])) {
                inventory.setItem(slot, null);
            }
        }
    }

    private void removeDuplicateMenuEntries(PlayerInventory inventory) {
        ItemStack[] contents = inventory.getContents();
        for (int slot = 0; slot < contents.length; slot++) {
            if (slot != MENU_SLOT && isMenuItem(contents[slot])) {
                inventory.setItem(slot, null);
            }
        }
    }

    private void removeMenuEntriesOutsideStorage(PlayerInventory inventory) {
        ItemStack[] contents = inventory.getContents();
        for (int slot = STORAGE_SIZE; slot < contents.length; slot++) {
            if (isMenuItem(contents[slot])) {
                inventory.setItem(slot, null);
            }
        }
    }

    private void scheduleEnsure(Player player) {
        if (closed) {
            return;
        }
        UUID playerId = player.getUniqueId();
        BukkitTask previous = pendingEnsures.remove(playerId);
        if (previous != null) {
            previous.cancel();
        }
        BukkitTask task = plugin.getServer().getScheduler().runTask(plugin, () -> {
            pendingEnsures.remove(playerId);
            Player current = plugin.getServer().getPlayer(playerId);
            if (current != null && current.isOnline() && !current.isDead()) {
                ensure(current);
            }
        });
        pendingEnsures.put(playerId, task);
    }

    private void reconcileOnlinePlayers() {
        if (closed) {
            return;
        }
        for (Player player : plugin.getServer().getOnlinePlayers()) {
            if (!player.isDead()) {
                ensure(player);
            }
        }
    }

    private void forget(Player player) {
        UUID playerId = player.getUniqueId();
        BukkitTask task = pendingEnsures.remove(playerId);
        if (task != null) {
            task.cancel();
        }
        warnedFullInventories.remove(playerId);
    }

    private static boolean isEmpty(ItemStack item) {
        return item == null || item.getType().isAir();
    }
}
