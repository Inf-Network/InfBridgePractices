package net.infnetwork.snowball.bridgingskin.lottery;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import java.util.UUID;
import java.util.logging.Level;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockExplodeEvent;
import org.bukkit.event.block.BlockPistonExtendEvent;
import org.bukkit.event.block.BlockPistonRetractEvent;
import org.bukkit.event.entity.EntityExplodeEvent;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.player.PlayerTeleportEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scheduler.BukkitTask;
import net.infnetwork.snowball.bridgingskin.NetworkMessages;
import net.infnetwork.snowball.bridgingskin.crate.LotteryCrateService;

/** Owns the purchase state machine and all lottery inventories/animations. */
public final class LotteryManager implements Listener, AutoCloseable {
    private static final int SINGLE_SLOT = 11;
    private static final int TEN_SLOT = 15;
    private static final int[] RESULT_SLOTS = {13, 10, 11, 12, 13, 14, 15, 16, 21, 22, 23};

    private final JavaPlugin plugin;
    private final LotteryCrateService crates;
    private final PrizePool prizePool;
    private final LotterySettings settings;
    private final EconomyGateway economy;
    private final RewardAuthorizer authorizer;
    private final Random random;
    private final Set<UUID> activeDraws = new HashSet<>();
    private final Map<UUID, BukkitTask> animationTasks = new HashMap<>();
    private final Map<UUID, Location> menuCrateLocations = new HashMap<>();
    private boolean closed;

    public LotteryManager(JavaPlugin plugin, LotteryCrateService crates, PrizePool prizePool,
            LotterySettings settings, EconomyGateway economy, RewardAuthorizer authorizer) {
        this(plugin, crates, prizePool, settings, economy, authorizer, new Random());
    }

    LotteryManager(JavaPlugin plugin, LotteryCrateService crates, PrizePool prizePool,
            LotterySettings settings, EconomyGateway economy, RewardAuthorizer authorizer, Random random) {
        this.plugin = plugin;
        this.crates = crates;
        this.prizePool = prizePool;
        this.settings = settings;
        this.economy = economy;
        this.authorizer = authorizer;
        this.random = random;
    }

    public void open(Player player) {
        Location defaultCrate = crates.crate().map(crate -> crate.location()).orElse(null);
        open(player, defaultCrate);
    }

    private void open(Player player, Location crateLocation) {
        if (closed) {
            return;
        }
        Set<Material> owned;
        try {
            owned = Set.copyOf(authorizer.ownedBy(player));
        } catch (RewardAuthorizationException exception) {
            storageError(player, "读取皮肤失败", exception);
            return;
        }
        int remaining = prizePool.remaining(owned);
        LotteryInventoryHolder holder = new LotteryInventoryHolder(
                LotteryInventoryHolder.Screen.MENU, player.getUniqueId());
        Inventory inventory = Bukkit.createInventory(holder, 27, Component.text("方块皮肤抽奖", NamedTextColor.DARK_AQUA));
        holder.bind(inventory);
        inventory.setItem(SINGLE_SLOT, item(Material.ENDER_CHEST, "单次抽奖", NamedTextColor.AQUA,
                List.of("价格: " + settings.singleCost(), "剩余未拥有: " + remaining)));
        inventory.setItem(TEN_SLOT, item(Material.CHEST, "十连抽奖", NamedTextColor.GOLD,
                List.of("价格: " + settings.tenCost(), "不足 10 种时请使用单抽")));
        player.openInventory(inventory);
        // Opening a new inventory synchronously closes the previous one. Store the
        // origin afterwards so an old MENU close event cannot erase this session.
        rememberMenuCrate(player.getUniqueId(), crateLocation);
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = false)
    public void onCrateUse(PlayerInteractEvent event) {
        Block clicked = event.getClickedBlock();
        if (clicked == null || !event.getAction().isRightClick() || !crates.matches(clicked)
                || clicked.getType() != Material.ENDER_CHEST) {
            return;
        }
        // Explicit DENY keeps the physical ender chest closed even if protection plugins already cancelled use.
        event.setCancelled(true);
        event.setUseInteractedBlock(Event.Result.DENY);
        event.setUseItemInHand(Event.Result.DENY);
        if (event.getHand() == EquipmentSlot.HAND) {
            open(event.getPlayer(), clicked.getLocation());
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = false)
    public void onInventoryClick(InventoryClickEvent event) {
        if (!(event.getView().getTopInventory().getHolder() instanceof LotteryInventoryHolder holder)) {
            return;
        }
        event.setCancelled(true);
        if (holder.screen() != LotteryInventoryHolder.Screen.MENU
                || !(event.getWhoClicked() instanceof Player player)
                || !holder.owner().equals(player.getUniqueId())
                || event.getClick() != ClickType.LEFT
                || event.getRawSlot() < 0
                || event.getRawSlot() >= event.getView().getTopInventory().getSize()) {
            return;
        }
        if (event.getRawSlot() == SINGLE_SLOT) {
            purchase(player, 1, settings.singleCost());
        } else if (event.getRawSlot() == TEN_SLOT) {
            purchase(player, 10, settings.tenCost());
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = false)
    public void onInventoryDrag(InventoryDragEvent event) {
        if (event.getView().getTopInventory().getHolder() instanceof LotteryInventoryHolder) {
            event.setCancelled(true);
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = false)
    public void onBreak(BlockBreakEvent event) {
        if (crates.matches(event.getBlock())) {
            event.setCancelled(true);
            NetworkMessages.send(event.getPlayer(), "&c抽奖箱受到保护，请先选中它并用 /bskin-crate remove 移除注册");
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = false)
    public void onBlockExplode(BlockExplodeEvent event) {
        event.blockList().removeIf(crates::matches);
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = false)
    public void onEntityExplode(EntityExplodeEvent event) {
        event.blockList().removeIf(crates::matches);
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = false)
    public void onPistonExtend(BlockPistonExtendEvent event) {
        if (event.getBlocks().stream().anyMatch(crates::matches)) {
            event.setCancelled(true);
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = false)
    public void onPistonRetract(BlockPistonRetractEvent event) {
        if (event.getBlocks().stream().anyMatch(crates::matches)) {
            event.setCancelled(true);
        }
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        menuCrateLocations.remove(event.getPlayer().getUniqueId());
        cancelAnimation(event.getPlayer().getUniqueId(), false);
    }

    @EventHandler
    public void onTeleport(PlayerTeleportEvent event) {
        menuCrateLocations.remove(event.getPlayer().getUniqueId());
        cancelAnimation(event.getPlayer().getUniqueId(), true);
    }

    @EventHandler
    public void onDeath(PlayerDeathEvent event) {
        menuCrateLocations.remove(event.getEntity().getUniqueId());
        cancelAnimation(event.getEntity().getUniqueId(), true);
    }

    @EventHandler
    public void onInventoryClose(InventoryCloseEvent event) {
        if (!(event.getInventory().getHolder() instanceof LotteryInventoryHolder holder)
                || !holder.owner().equals(event.getPlayer().getUniqueId())) {
            return;
        }
        if (holder.screen() == LotteryInventoryHolder.Screen.MENU) {
            menuCrateLocations.remove(holder.owner());
        } else if (holder.screen() == LotteryInventoryHolder.Screen.ANIMATION) {
            cancelAnimation(holder.owner(), true);
        }
    }

    private void purchase(Player player, int count, double cost) {
        UUID playerId = player.getUniqueId();
        if (!activeDraws.add(playerId)) {
            NetworkMessages.send(player, "&e你的上一轮抽奖动画还没有结束");
            return;
        }
        try {
            Set<Material> owned = Set.copyOf(authorizer.ownedBy(player));
            int remaining = prizePool.remaining(owned);
            if (remaining < count) {
                if (remaining == 0) {
                    NetworkMessages.send(player, "&a你已经收集了奖池里的全部方块皮肤");
                } else if (count == 10) {
                    NetworkMessages.send(player, "&e只剩 " + remaining + " 种未拥有皮肤，请改用单次抽奖");
                }
                activeDraws.remove(playerId);
                return;
            }
            List<Material> rewards = prizePool.draw(count, owned, random);
            EconomyGateway.Payment payment = economy.withdraw(player, cost);
            if (!payment.successful()) {
                refundPartialDebit(player, payment.debitedAmount(), "扣款失败");
                NetworkMessages.send(player, "&c抽奖扣款失败: " + safeMessage(payment.message()));
                activeDraws.remove(playerId);
                return;
            }
            if (Math.abs(payment.debitedAmount() - cost) > 0.000_001D) {
                refundPartialDebit(player, payment.debitedAmount(), "扣款金额异常");
                NetworkMessages.send(player, "&c经济服务返回的扣款金额异常，本次抽奖已取消并退款");
                activeDraws.remove(playerId);
                return;
            }
            try {
                // This call is the commit point. The animation below is presentation only.
                authorizer.grantAtomically(player, rewards);
            } catch (RewardAuthorizationException | RuntimeException exception) {
                refundFull(player, payment.debitedAmount(), "皮肤授权失败");
                storageError(player, "皮肤授权失败，已尝试全额退款", exception);
                activeDraws.remove(playerId);
                return;
            }
            try {
                Location crateLocation = menuCrateLocations.remove(playerId);
                beginAnimation(player, rewards, crateLocation);
            } catch (RuntimeException exception) {
                // Authorization already committed: never refund and never imply that rewards were lost.
                activeDraws.remove(playerId);
                plugin.getLogger().log(Level.SEVERE, "抽奖奖励已授权，但动画启动失败: " + player.getName(), exception);
                showResults(player, rewards);
            }
        } catch (RewardAuthorizationException exception) {
            activeDraws.remove(playerId);
            storageError(player, "读取皮肤失败", exception);
        } catch (RuntimeException exception) {
            activeDraws.remove(playerId);
            plugin.getLogger().log(Level.SEVERE, "处理 " + player.getName() + " 的抽奖时出现异常", exception);
            NetworkMessages.send(player, "&c抽奖暂时不可用，请联系管理员检查日志");
        }
    }

    private void beginAnimation(Player player, List<Material> rewards, Location crateLocation) {
        LotteryInventoryHolder holder = new LotteryInventoryHolder(
                LotteryInventoryHolder.Screen.ANIMATION, player.getUniqueId());
        Inventory inventory = Bukkit.createInventory(holder, 27, Component.text("正在开启...", NamedTextColor.LIGHT_PURPLE));
        holder.bind(inventory);
        LotteryAnimationState animation = new LotteryAnimationState(prizePool, random);
        renderAnimation(inventory, animation);
        player.openInventory(inventory);
        int frames = Math.max(1, settings.animationDurationTicks() / settings.animationIntervalTicks());
        BukkitTask task = new BukkitRunnable() {
            // The fully initialized inventory displayed above is the first frame.
            private int frame = 1;

            @Override
            public void run() {
                if (!player.isOnline() || closed || !isCurrentAnimation(player, holder)) {
                    cancelAnimation(player.getUniqueId(), player.isOnline() && !closed);
                    return;
                }
                if (frame++ >= frames) {
                    animationTasks.remove(player.getUniqueId());
                    activeDraws.remove(player.getUniqueId());
                    showResults(player, rewards);
                    cancel();
                    return;
                }
                animation.advance();
                renderAnimation(inventory, animation);
                player.playSound(player.getLocation(), Sound.BLOCK_NOTE_BLOCK_HAT, 0.55F,
                        0.8F + Math.min(frame, 20) * 0.04F);
                spawnCrateParticle(crateLocation);
            }
        }.runTaskTimer(plugin, settings.animationIntervalTicks(), settings.animationIntervalTicks());
        animationTasks.put(player.getUniqueId(), task);
    }

    private void renderAnimation(Inventory inventory, LotteryAnimationState animation) {
        for (int column = 0; column < LotteryAnimationState.ROW_WIDTH; column++) {
            inventory.setItem(column,
                    item(animation.top(column), " ", NamedTextColor.GRAY, List.of()));

            Material preview = animation.preview(column);
            inventory.setItem(9 + column, item(preview,
                    MaterialDisplayNames.translated(preview, NamedTextColor.GRAY), List.of()));

            inventory.setItem(18 + column,
                    item(animation.bottom(column), " ", NamedTextColor.GRAY, List.of()));
        }
        // Slot 13 remains the selector, but its material moves with the preview
        // row instead of being independently randomized on every frame.
        Material center = animation.preview(4);
        inventory.setItem(13, item(center, "?", NamedTextColor.LIGHT_PURPLE, List.of("奖励已安全存入数据库")));
    }

    private void showResults(Player player, List<Material> rewards) {
        LotteryInventoryHolder holder = new LotteryInventoryHolder(
                LotteryInventoryHolder.Screen.RESULT, player.getUniqueId());
        Inventory inventory = Bukkit.createInventory(holder, 27, Component.text("抽奖结果", NamedTextColor.GOLD));
        holder.bind(inventory);
        int offset = rewards.size() == 1 ? 0 : 1;
        for (int index = 0; index < rewards.size(); index++) {
            Material material = rewards.get(index);
            inventory.setItem(RESULT_SLOTS[index + offset],
                    item(material, MaterialDisplayNames.translated(material, NamedTextColor.GREEN),
                            List.of("新皮肤已解锁")));
        }
        player.openInventory(inventory);
        player.playSound(player.getLocation(), Sound.UI_TOAST_CHALLENGE_COMPLETE, 0.8F, 1.2F);
        NetworkMessages.send(player, "&a抽奖完成，已解锁 " + rewards.size() + " 个新方块皮肤");
    }

    private void spawnCrateParticle(Location crateLocation) {
        if (crateLocation == null || crateLocation.getWorld() == null
                || !crateLocation.getWorld().isChunkLoaded(
                        crateLocation.getBlockX() >> 4, crateLocation.getBlockZ() >> 4)) {
            return;
        }
        crateLocation.getWorld().spawnParticle(Particle.PORTAL,
                crateLocation.clone().add(0.5, 1.0, 0.5),
                12, 0.3, 0.4, 0.3, 0.05);
    }

    private void rememberMenuCrate(UUID playerId, Location crateLocation) {
        if (crateLocation == null || crateLocation.getWorld() == null) {
            menuCrateLocations.remove(playerId);
        } else {
            menuCrateLocations.put(playerId, crateLocation.clone());
        }
    }

    private void refundPartialDebit(Player player, double amount, String reason) {
        if (amount > 0.0D) {
            refund(player, amount, reason);
        }
    }

    private void refundFull(Player player, double amount, String reason) {
        refund(player, amount, reason);
    }

    private void refund(Player player, double amount, String reason) {
        EconomyGateway.Refund refund = economy.refund(player, amount);
        if (!refund.successful()) {
            plugin.getLogger().severe(reason + "后给 " + player.getName() + " 退款 " + amount
                    + " 失败: " + safeMessage(refund.message()));
            NetworkMessages.send(player, "&c自动退款失败，请立即联系管理员；应退金额: " + amount);
        }
    }

    private void storageError(Player player, String action, Exception exception) {
        plugin.getLogger().log(Level.SEVERE, action + ": " + player.getName(), exception);
        NetworkMessages.send(player, "&c" + action + "，本次抽奖未完成");
    }

    private boolean isCurrentAnimation(Player player, LotteryInventoryHolder expected) {
        if (!(player.getOpenInventory().getTopInventory().getHolder() instanceof LotteryInventoryHolder current)) {
            return false;
        }
        return current.screen() == LotteryInventoryHolder.Screen.ANIMATION
                && current.owner().equals(player.getUniqueId())
                && current.sessionId().equals(expected.sessionId());
    }

    private void cancelAnimation(UUID playerId, boolean notify) {
        BukkitTask task = animationTasks.remove(playerId);
        if (task != null) {
            task.cancel();
            if (notify) {
                Player player = Bukkit.getPlayer(playerId);
                if (player != null) {
                    NetworkMessages.send(player, "&e抽奖展示已关闭；奖励在扣款成功后已经安全解锁");
                }
            }
        }
        activeDraws.remove(playerId);
    }

    @Override
    public void close() {
        closed = true;
        for (BukkitTask task : new ArrayList<>(animationTasks.values())) {
            task.cancel();
        }
        animationTasks.clear();
        activeDraws.clear();
        menuCrateLocations.clear();
        // Plugin disable/reload does not guarantee that custom inventories close.
        // Leaving one open after listeners unregister would let players take the
        // real preview/result ItemStacks from an orphaned GUI.
        for (Player player : Bukkit.getOnlinePlayers()) {
            if (player.getOpenInventory().getTopInventory().getHolder()
                    instanceof LotteryInventoryHolder) {
                player.closeInventory();
            }
        }
    }

    private static ItemStack item(Material material, String name, NamedTextColor color, List<String> lore) {
        return item(material, Component.text(name, color), lore);
    }

    private static ItemStack item(Material material, Component name, List<String> lore) {
        ItemStack stack = new ItemStack(material);
        ItemMeta meta = stack.getItemMeta();
        meta.displayName(name);
        meta.lore(lore.stream().map(line -> Component.text(line, NamedTextColor.GRAY)).toList());
        stack.setItemMeta(meta);
        return stack;
    }

    private static String safeMessage(String message) {
        return message == null || message.isBlank() ? "经济服务未提供详细原因" : message;
    }
}
