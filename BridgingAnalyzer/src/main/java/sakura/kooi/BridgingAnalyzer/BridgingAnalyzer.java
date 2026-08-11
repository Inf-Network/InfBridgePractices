/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  org.bukkit.Bukkit
 *  org.bukkit.GameMode
 *  org.bukkit.Material
 *  org.bukkit.block.Block
 *  org.bukkit.command.CommandExecutor
 *  org.bukkit.entity.ArmorStand
 *  org.bukkit.entity.Entity
 *  org.bukkit.entity.EntityType
 *  org.bukkit.entity.Player
 *  org.bukkit.entity.Projectile
 *  org.bukkit.entity.Villager
 *  org.bukkit.entity.Villager$Profession
 *  org.bukkit.event.EventHandler
 *  org.bukkit.event.Listener
 *  org.bukkit.event.entity.EntityDamageByEntityEvent
 *  org.bukkit.event.entity.EntityDamageEvent
 *  org.bukkit.event.entity.FoodLevelChangeEvent
 *  org.bukkit.event.player.PlayerArmorStandManipulateEvent
 *  org.bukkit.event.player.PlayerDropItemEvent
 *  org.bukkit.event.player.PlayerInteractAtEntityEvent
 *  org.bukkit.event.player.PlayerInteractEntityEvent
 *  org.bukkit.event.player.PlayerJoinEvent
 *  org.bukkit.event.player.PlayerQuitEvent
 *  org.bukkit.event.weather.WeatherChangeEvent
 *  org.bukkit.inventory.ItemStack
 *  org.bukkit.inventory.PlayerInventory
 *  org.bukkit.material.MaterialData
 *  org.bukkit.plugin.Plugin
 *  org.bukkit.plugin.PluginManager
 *  org.bukkit.plugin.java.JavaPlugin
 *  org.bukkit.potion.PotionEffect
 *  org.bukkit.potion.PotionEffectType
 */
package sakura.kooi.BridgingAnalyzer;

import java.util.HashMap;
import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.command.CommandExecutor;
import org.bukkit.entity.ArmorStand;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.entity.Projectile;
import org.bukkit.entity.Villager;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.FoodLevelChangeEvent;
import org.bukkit.event.player.PlayerArmorStandManipulateEvent;
import org.bukkit.event.player.PlayerDropItemEvent;
import org.bukkit.event.player.PlayerInteractAtEntityEvent;
import org.bukkit.event.player.PlayerInteractEntityEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.weather.WeatherChangeEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;
import org.bukkit.material.MaterialData;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.PluginManager;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import sakura.kooi.BridgingAnalyzer.Counter;
import sakura.kooi.BridgingAnalyzer.CounterListener;
import sakura.kooi.BridgingAnalyzer.DefaultBlockSkinProvider;
import sakura.kooi.BridgingAnalyzer.HighlightListener;
import sakura.kooi.BridgingAnalyzer.TriggerBlockListener;
import sakura.kooi.BridgingAnalyzer.api.BlockSkinProvider;
import sakura.kooi.BridgingAnalyzer.commands.BridgeCommand;
import sakura.kooi.BridgingAnalyzer.commands.ClearCommand;
import sakura.kooi.BridgingAnalyzer.commands.SaveWorldCommand;
import sakura.kooi.BridgingAnalyzer.commands.StuckCommand;
import sakura.kooi.BridgingAnalyzer.commands.VillagerSpawnPointCommand;
import sakura.kooi.BridgingAnalyzer.utils.NoAIUtils;
import sakura.kooi.BridgingAnalyzer.utils.TitleUtils;
import sakura.kooi.BridgingAnalyzer.utils.Utils;

public class BridgingAnalyzer
extends JavaPlugin
implements Listener {
    private static BridgingAnalyzer instance;
    private static HashMap<Player, Counter> counters;
    private static HashMap<Block, MaterialData> placedBlocks;
    private static BlockSkinProvider blockSkinProvider;

    public static void clearEffect(Player player) {
        for (PotionEffect eff : player.getActivePotionEffects()) {
            if (eff.getType() == PotionEffectType.INVISIBILITY && player.isOp()) continue;
            player.removePotionEffect(eff.getType());
        }
    }

    public static void clearInventory(Player p) {
        PlayerInventory inv = p.getInventory();
        for (int i = 0; i < inv.getSize(); ++i) {
            ItemStack item = inv.getItem(i);
            if (item != null && item.getItemMeta() != null && item.getItemMeta().getDisplayName() != null && item.getItemMeta().getDisplayName().contains("Key")) continue;
            inv.setItem(i, null);
        }
    }

    public static Counter getCounter(Player p) {
        Counter c = counters.get(p);
        if (c == null) {
            c = new Counter(p);
            counters.put(p, c);
        }
        return c;
    }

    public static void spawnVillager() {
        for (Entity en : Bukkit.getWorld((String)"world").getEntities()) {
            if (en.getType() != EntityType.VILLAGER || !"\u9776\u5b50".equals(en.getCustomName())) continue;
            en.remove();
        }
        for (ArmorStand stand : Bukkit.getWorld((String)"world").getEntitiesByClass(ArmorStand.class)) {
            if (stand.getCustomName() == null || !stand.getCustomName().contains("VillagerSpawnPoint")) continue;
            Villager vi = (Villager)stand.getWorld().spawnEntity(stand.getLocation().add(0.0, 1.0, 0.0), EntityType.VILLAGER);
            vi.addPotionEffect(new PotionEffect(PotionEffectType.SLOWNESS, 32766, 254, false, false), true);
            vi.setProfession(Villager.Profession.LIBRARIAN);
            vi.setMaxHealth(1.0);
            vi.setHealth(1.0);
            vi.setCustomName("\u9776\u5b50");
            vi.setCustomNameVisible(false);
            NoAIUtils.setAI((Entity)vi, false);
        }
    }

    /*
     * 原版顺序是「清背包 → 发方块 → 回血 → 传送」。传送排在最后,
     * 意味着前面任何一步抛异常,玩家就永远留在原地 —— 掉进虚空时就是
     * 反复触发、反复清背包、却始终出不来。
     *
     * 发方块这一步现在走的是 BridgingSkin 的 SkinProvider(读玩家皮肤 json),
     * 比原来的固定砂岩多了几条出错路径,更不该挡在传送前面。
     *
     * 改成传送优先:先把人拉回来,再做背包与状态这些收尾。
     */
    public static void teleportCheckPoint(Player p) {
        p.setFallDistance(0.0f);
        BridgingAnalyzer.getCounter(p).teleportCheckPoint();
        p.setGameMode(GameMode.SURVIVAL);
        p.setFoodLevel(20);
        p.setHealth(20.0);
        p.setNoDamageTicks(5);
        BridgingAnalyzer.clearInventory(p);
        p.getInventory().addItem(new ItemStack[]{blockSkinProvider.provide(p)});
    }

    public static void refreshItem(Player p) {
        BridgingAnalyzer.clearInventory(p);
        p.getInventory().addItem(new ItemStack[]{blockSkinProvider.provide(p)});
    }

    public static boolean isPlacedByPlayer(Block b) {
        if (BridgingAnalyzer.getPlacedBlocks().containsKey(b)) {
            return BridgingAnalyzer.getPlacedBlocks().get(b).equals((Object)b.getState().getData());
        }
        return false;
    }

    @EventHandler
    public void interactAtEntity(PlayerInteractAtEntityEvent e) {
        if (e.getPlayer().getGameMode() == GameMode.CREATIVE && e.getPlayer().hasPermission("bridge.remove") && e.getRightClicked().getCustomName().contains("VillagerSpawnPoint") && e.getRightClicked().getType() == EntityType.ARMOR_STAND) {
            e.setCancelled(true);
            e.getRightClicked().remove();
            TitleUtils.sendTitle(e.getPlayer(), "", "\u00a7a\u6751\u6c11\u5237\u65b0\u70b9\u5df2\u79fb\u9664", 10, 20, 10);
        }
    }

    @EventHandler
    public void antiArmorSTack(PlayerArmorStandManipulateEvent event) {
        event.setCancelled(true);
    }

    @EventHandler
    public void disableVillagerShop(PlayerInteractEntityEvent e) {
        if (e.getRightClicked().getType() == EntityType.VILLAGER) {
            e.setCancelled(true);
        }
    }

    @EventHandler
    public void disableWeather(WeatherChangeEvent e) {
        e.setCancelled(true);
    }

    @EventHandler
    public void logoutBreak(PlayerQuitEvent e) {
        BridgingAnalyzer.getCounter(e.getPlayer()).instantBreakBlock();
        Bukkit.getConsoleSender().sendMessage("\u00a7bBridgingAnalyzer \u00a77>> \u00a7a\u73a9\u5bb6 " + e.getPlayer().getName() + " \u79bb\u7ebf, \u5df2\u6e05\u9664\u5176\u653e\u7f6e\u7684\u65b9\u5757.");
    }

    @EventHandler
    public void noHunger(FoodLevelChangeEvent e) {
        e.setFoodLevel(20);
    }

    @EventHandler
    public void onDamage(EntityDamageEvent e) {
        if (e.getEntity().getType() == EntityType.PLAYER) {
            Counter c = BridgingAnalyzer.getCounter((Player)e.getEntity());
            if (e.getFinalDamage() > 20.0) {
                c.reset();
                BridgingAnalyzer.teleportCheckPoint((Player)e.getEntity());
                TitleUtils.sendTitle((Player)e.getEntity(), "", "\u00a74\u81f4\u547d\u4f24\u5bb3 - " + Utils.formatDouble(e.getFinalDamage() / 2.0) + " \u2764", 10, 20, 10);
                e.setDamage(0.0);
            } else if (e.getFinalDamage() > 10.0) {
                TitleUtils.sendTitle((Player)e.getEntity(), "", "\u00a7c\u4e25\u91cd\u4f24\u5bb3 - " + Utils.formatDouble(e.getFinalDamage() / 2.0) + " \u2764", 10, 20, 10);
            }
            e.setDamage(0.0);
        }
    }

    public void onDisable() {
        Bukkit.getConsoleSender().sendMessage("\u00a7bBridgingAnalyzer \u00a77>> \u00a7c\u6b63\u5728\u6e05\u9664\u6240\u6709\u5df2\u653e\u7f6e\u65b9\u5757....");
        for (Counter c : counters.values()) {
            c.instantBreakBlock();
        }
        counters.clear();
        for (Block b : Counter.scheduledBreakBlocks) {
            b.setType(Material.AIR);
        }
        Counter.scheduledBreakBlocks.clear();
        Bukkit.getConsoleSender().sendMessage("\u00a7bBridgingAnalyzer \u00a77>> \u00a7a\u65b9\u5757\u6e05\u9664\u5b8c\u6bd5.");
    }

    public void onEnable() {
        instance = this;
        // 移植时移除了 bStats 统计(原 utils/Metrics.java)。
        // 它依赖 org.json.simple —— 该库在 Spigot 1.8 内置、现代服务端已移除;
        // 而这只是给上游作者看的匿名用量统计,对玩法零贡献,Paper 自身也已内置 bStats。
        blockSkinProvider = new DefaultBlockSkinProvider();
        PluginManager pluginManager = Bukkit.getPluginManager();
        pluginManager.registerEvents((Listener)this, (Plugin)this);
        pluginManager.registerEvents((Listener)new CounterListener(), (Plugin)this);
        pluginManager.registerEvents((Listener)new HighlightListener(), (Plugin)this);
        pluginManager.registerEvents((Listener)new TriggerBlockListener(), (Plugin)this);
        this.getCommand("bridge").setExecutor((CommandExecutor)new BridgeCommand());
        this.getCommand("clearblock").setExecutor((CommandExecutor)new ClearCommand());
        this.getCommand("bsaveworld").setExecutor((CommandExecutor)new SaveWorldCommand());
        this.getCommand("imstuck").setExecutor((CommandExecutor)new StuckCommand());
        this.getCommand("genvillager").setExecutor((CommandExecutor)new VillagerSpawnPointCommand());
        BridgingAnalyzer.spawnVillager();
        Bukkit.getScheduler().runTaskTimer((Plugin)this, () -> {
            if (Bukkit.getOnlinePlayers().isEmpty()) {
                return;
            }
            BridgingAnalyzer.spawnVillager();
        }, 300L, 300L);
        Bukkit.getConsoleSender().sendMessage(new String[]{"\u00a7bBridgingAnalyzer \u00a77>> \u00a7f----------------------------------------------------------------", "\u00a7bBridgingAnalyzer \u00a77>> \u00a7a\u642d\u8def\u7ec3\u4e60 \u5df2\u52a0\u8f7d \u00a7bBy.SakuraKooi", "\u00a7bBridgingAnalyzer \u00a77>> \u00a7chttps://github.com/SakuraKoi/BridgingAnalyzer/", "\u00a7bBridgingAnalyzer \u00a77>> \u00a7f----------------------------------------------------------------", "\u00a7bBridgingAnalyzer \u00a77>> \u00a7e\u8e29\u5728 \u00a7a\u7eff\u5b9d\u77f3\u5757 \u00a7e\u4e0a\u53ef\u4ee5\u8bbe\u7f6e\u4f20\u9001\u70b9", "\u00a7bBridgingAnalyzer \u00a77>> \u00a7e\u8e29\u5728 \u00a7c\u7ea2\u77f3\u5757 \u00a7e\u4e0a\u53ef\u4ee5\u56de\u5230\u4f20\u9001\u70b9", "\u00a7bBridgingAnalyzer \u00a77>> \u00a7e\u8e29\u5728 \u00a7b\u9752\u91d1\u77f3\u5757 \u00a7e\u4e0a\u53ef\u4ee5\u56de\u5230\u51fa\u751f\u70b9", "\u00a7bBridgingAnalyzer \u00a77>> \u00a7e\u4f7f\u7528 \u00a7a/genvillager \u00a7e\u53ef\u5728\u7ad9\u7acb\u4f4d\u7f6e\u521b\u5efa\u6751\u6c11\u5237\u65b0\u70b9", "\u00a7bBridgingAnalyzer \u00a77>> \u00a7c\u6389\u5165\u865a\u7a7a\u4f1a\u81ea\u52a8\u56de\u5230 \u00a7a\u4f20\u9001\u70b9 \u00a7c\u5e76\u91cd\u7f6e\u5730\u56fe", "\u00a7bBridgingAnalyzer \u00a77>> \u00a7c\u6ce8\u610f: \u521b\u9020\u6a21\u5f0f\u653e\u7f6e\u7684\u65b9\u5757\u4e0d\u4f1a\u88ab\u91cd\u7f6e, \u8bf7\u5728\u751f\u5b58\u6a21\u5f0f\u4e0b\u7ec3\u4e60", "\u00a7bBridgingAnalyzer \u00a77>> \u00a7f----------------------------------------------------------------"});
    }

    @EventHandler
    public void onJoin(PlayerJoinEvent e) {
        if (e.getPlayer().hasPermission("bridginganalyzer.noclear")) {
            return;
        }
        BridgingAnalyzer.teleportCheckPoint(e.getPlayer());
    }

    @EventHandler
    public void onDropItem(PlayerDropItemEvent e) {
        if (e.getPlayer().hasPermission("bridginganalyzer.noclear")) {
            return;
        }
        if (e.getItemDrop().getItemStack().getType() == Material.GOLDEN_PICKAXE) {
            e.getItemDrop().remove();
        }
    }

    @EventHandler
    public void onPvP(EntityDamageByEntityEvent e) {
        if (e.isCancelled()) {
            return;
        }
        if (e.getEntity() == null) {
            return;
        }
        if (e.getDamager() == null) {
            return;
        }
        if (e.getEntity().getType() == EntityType.PLAYER) {
            Projectile proj;
            if (e.getDamager().getType() == EntityType.PLAYER) {
                int state = this.onPvPDamage((Player)e.getEntity(), (Player)e.getDamager());
                if (state == -1) {
                    e.setCancelled(true);
                } else if (state == 1) {
                    e.setCancelled(true);
                    BridgingAnalyzer.getCounter((Player)e.getDamager()).setPvPEnabled(true);
                    TitleUtils.sendTitle((Player)e.getDamager(), "", "\u00a7c\u6ce8\u610f: \u00a7aPvP\u5df2\u5f00\u542f", 10, 20, 10);
                    ((Player)e.getEntity()).damage(0.0);
                    ((Player)e.getEntity()).setNoDamageTicks(10);
                    ((Player)e.getDamager()).setNoDamageTicks(10);
                }
            } else if (e.getDamager() instanceof Projectile && (proj = (Projectile)e.getDamager()).getShooter() instanceof Player) {
                int state = this.onPvPDamage((Player)e.getEntity(), (Player)proj.getShooter());
                if (state == -1) {
                    e.setCancelled(true);
                } else if (state == 1) {
                    e.setCancelled(true);
                    BridgingAnalyzer.getCounter((Player)proj.getShooter()).setPvPEnabled(true);
                    TitleUtils.sendTitle((Player)proj.getShooter(), "", "\u00a7c\u6ce8\u610f: \u00a7aPvP\u5df2\u5f00\u542f", 10, 20, 10);
                    ((Player)e.getEntity()).damage(0.0);
                    ((Player)e.getEntity()).setNoDamageTicks(10);
                    ((Player)proj.getShooter()).setNoDamageTicks(10);
                }
            }
        }
    }

    private int onPvPDamage(Player player, Player damager) {
        if (!BridgingAnalyzer.getCounter(player).isPvPEnabled()) {
            return -1;
        }
        if (!BridgingAnalyzer.getCounter(damager).isPvPEnabled()) {
            return 1;
        }
        return 0;
    }

    public static BridgingAnalyzer getInstance() {
        return instance;
    }

    public static HashMap<Player, Counter> getCounters() {
        return counters;
    }

    public static HashMap<Block, MaterialData> getPlacedBlocks() {
        return placedBlocks;
    }

    public static void setBlockSkinProvider(BlockSkinProvider blockSkinProvider) {
        BridgingAnalyzer.blockSkinProvider = blockSkinProvider;
    }

    static {
        counters = new HashMap();
        placedBlocks = new HashMap();
    }
}

