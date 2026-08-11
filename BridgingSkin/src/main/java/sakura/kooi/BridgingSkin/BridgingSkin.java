/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  org.bukkit.Bukkit
 *  org.bukkit.Material
 *  org.bukkit.command.CommandExecutor
 *  org.bukkit.entity.Player
 *  org.bukkit.event.EventHandler
 *  org.bukkit.event.Listener
 *  org.bukkit.event.inventory.InventoryClickEvent
 *  org.bukkit.event.inventory.InventoryMoveItemEvent
 *  org.bukkit.event.player.PlayerDropItemEvent
 *  org.bukkit.event.player.PlayerInteractEvent
 *  org.bukkit.event.player.PlayerQuitEvent
 *  org.bukkit.inventory.ItemStack
 *  org.bukkit.plugin.Plugin
 *  org.bukkit.plugin.java.JavaPlugin
 *  sakura.kooi.BridgingAnalyzer.api.BlockSkinProvider
 *  sakura.kooi.BridgingAnalyzer.api.BridgingAnalyzerAPI
 *  sakura.kooi.BridgingAnalyzer.utils.Metrics
 */
package sakura.kooi.BridgingSkin;

import java.io.File;
import java.io.FileReader;
import java.io.Reader;
import java.util.HashMap;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.command.CommandExecutor;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryMoveItemEvent;
import org.bukkit.event.player.PlayerDropItemEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.java.JavaPlugin;
import sakura.kooi.BridgingAnalyzer.api.BlockSkinProvider;
import sakura.kooi.BridgingAnalyzer.api.BridgingAnalyzerAPI;
import sakura.kooi.BridgingSkin.FileUtils;
import sakura.kooi.BridgingSkin.SkinEditCommand;
import sakura.kooi.BridgingSkin.SkinEditListener;
import sakura.kooi.BridgingSkin.SkinProvider;
import sakura.kooi.BridgingSkin.SkinSelectCommand;
import sakura.kooi.BridgingSkin.data.PlayerSkin;
import sakura.kooi.BridgingSkin.data.SkinSet;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

public class BridgingSkin
extends JavaPlugin
implements Listener {
    private static BridgingSkin instance;
    private static final Gson gson;
    public static HashMap<String, PlayerSkin> skins;
    protected static File rootDir;

    public static PlayerSkin getSkin(String player, String uuid) {
        if (!skins.containsKey(player)) {
            File playerfile = new File(rootDir, player + ".json");
            PlayerSkin skin = BridgingSkin.loadSkin(playerfile);
            if (skin == null) {
                skin = new PlayerSkin(player, uuid);
            }
            skins.put(player, skin);
            return skin;
        }
        PlayerSkin skin = skins.get(player);
        return skin;
    }

    /*
     * CFR 把原版的 try-with-resources 反编译成了一坨 Collections.singletonList(reader).get(0)
     * 判空 + 标签跳转的结构,且没接住 FileReader 构造与 close() 抛的受检异常,根本编译不过。
     * 这里还原成它本来的样子。
     */
    protected static PlayerSkin loadSkin(File file) {
        if (!file.exists()) {
            return null;
        }
        try (FileReader reader = new FileReader(file)) {
            return gson.fromJson((Reader)reader, PlayerSkin.class);
        }
        catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    public void onEnable() {
        instance = this;
        // 移植时移除 bStats 统计:它依赖 BridgingAnalyzer 的 Metrics 类,
        // 而后者因依赖 org.json.simple(现代服务端已移除)已在移植中删除。
        rootDir = new File("plugins" + File.separator + "BridgingSkin" + File.separator + "skins");
        if (!rootDir.exists()) {
            rootDir.mkdirs();
        }
        skins = new HashMap();
        BridgingAnalyzerAPI.setBlockSkinProvider((BlockSkinProvider)new SkinProvider());
        this.getCommand("bskin").setExecutor((CommandExecutor)new SkinSelectCommand());
        this.getCommand("bskin-edit").setExecutor((CommandExecutor)new SkinEditCommand());
        Bukkit.getPluginManager().registerEvents((Listener)this, (Plugin)this);
        Bukkit.getPluginManager().registerEvents((Listener)new SkinEditListener(), (Plugin)this);
        Bukkit.getScheduler().runTaskTimer((Plugin)this, this::saveData, 6000L, 6000L);
    }

    public void onDisable() {
        this.saveData();
    }

    public static PlayerSkin getSkin(String player) {
        if (!skins.containsKey(player)) {
            File playerfile = new File(rootDir, player + ".json");
            PlayerSkin skin = BridgingSkin.loadSkin(playerfile);
            return skin;
        }
        PlayerSkin skin = skins.get(player);
        return skin;
    }

    public static void saveSkin(PlayerSkin skin) {
        if (skin == null) {
            return;
        }
        try {
            String json = gson.toJson(skin);
            FileUtils.writeFile(new File(rootDir, skin.player + ".json"), json);
            File uuidFile = new File(rootDir, skin.uuid + ".json");
            if (uuidFile.exists()) {
                uuidFile.delete();
            }
        }
        catch (Exception ex) {
            ex.printStackTrace();
        }
    }

    private void saveData() {
        rootDir = new File("plugins" + File.separator + "BridgingSkin" + File.separator + "skins");
        if (!rootDir.exists()) {
            rootDir.mkdirs();
        }
        for (PlayerSkin skin : skins.values()) {
            try {
                String json = gson.toJson(skin);
                FileUtils.writeFile(new File(rootDir, skin.player + ".json"), json);
                File uuidFile = new File(rootDir, skin.uuid + ".json");
                if (!uuidFile.exists()) continue;
                uuidFile.delete();
            }
            catch (Exception exception) {}
        }
    }

    @EventHandler
    public void onLeave(PlayerQuitEvent e) {
        PlayerSkin skin = skins.get(e.getPlayer().getName());
        if (skin == null) {
            return;
        }
        try {
            String json = gson.toJson(skin);
            FileUtils.writeFile(new File(rootDir, skin.player + ".json"), json);
            File uuidFile = new File(rootDir, skin.uuid + ".json");
            if (uuidFile.exists()) {
                uuidFile.delete();
            }
        }
        catch (Exception ex) {
            ex.printStackTrace();
            return;
        }
        skins.remove(e.getPlayer().getName());
    }

    @EventHandler
    public void onDrop(PlayerDropItemEvent e) {
        ItemStack item = e.getItemDrop().getItemStack();
        if (!item.hasItemMeta()) {
            return;
        }
        if (!item.getItemMeta().hasLore()) {
            return;
        }
        if (!item.getItemMeta().getLore().contains("\u00a76\u76ae\u80a4\u65b9\u5757")) {
            return;
        }
        e.setCancelled(true);
    }

    /*
     * 1.21.11 \u79fb\u690d:\u539f\u7248\u9760 Inventory#getTitle() \u6bd4\u5bf9\u300c\u00a76\u00a7l\u76ae\u80a4\u5e93\u5b58\u300d\u6765\u8ba4\u83dc\u5355,
     * \u8be5\u65b9\u6cd5 1.14 \u8d77\u5df2\u4ece Inventory \u79fb\u9664\u3002\u6539\u6210\u8ba4 SkinSelectHolder \u7c7b\u578b\u3002
     */
    @EventHandler
    public void onInventoryClick(InventoryClickEvent e) {
        if (e.isCancelled()) {
            return;
        }
        if (!(e.getInventory().getHolder() instanceof SkinSelectHolder)) {
            return;
        }
        // \u76ae\u80a4\u5e93\u5b58\u5f00\u7740\u7684\u65f6\u5019,\u8fde\u73a9\u5bb6\u81ea\u5df1\u80cc\u5305\u91cc\u7684\u632a\u52a8\u4e5f\u4e00\u5e76\u7981\u6389(\u4e0e\u539f\u7248\u4e00\u81f4)
        e.setCancelled(true);
        if (e.getClickedInventory() != e.getInventory()) {
            return;
        }
        Player player = (Player)e.getView().getPlayer();
        ItemStack item = e.getCurrentItem();
        if (item == null || item.getType() == Material.AIR) {
            return;
        }
        if (item.getType() == Material.BARRIER) {
            return;
        }
        BridgingSkin.getSkin((String)player.getName(), (String)player.getUniqueId().toString()).currentSkin = new SkinSet(item.getType().name());
        player.closeInventory();
        BridgingAnalyzerAPI.refreshItem((Player)player);
        player.sendMessage("&bI&en&cf &eBridge &7\u00bb \u00a7a\u4f60\u7684\u642d\u8def\u76ae\u80a4\u5df2\u66f4\u6362");
    }

    @EventHandler
    public void onMove(InventoryMoveItemEvent e) {
        if (e.isCancelled()) {
            return;
        }
        if (e.getSource().getHolder() instanceof SkinSelectHolder || e.getDestination().getHolder() instanceof SkinSelectHolder) {
            e.setCancelled(true);
        }
    }

    @EventHandler
    public void onSetSkin(PlayerInteractEvent e) {
        // 1.9 \u8d77 PlayerInteractEvent \u4e3b\u624b/\u526f\u624b\u5404\u89e6\u53d1\u4e00\u6b21,\u4e0d\u62e6\u4f1a\u91cd\u590d\u52a0\u76ae\u80a4
        if (e.getHand() != EquipmentSlot.HAND) {
            return;
        }
        ItemStack item = e.getPlayer().getInventory().getItemInMainHand();
        if (item.getType() == Material.AIR) {
            return;
        }
        if (!item.hasItemMeta()) {
            return;
        }
        if (!item.getItemMeta().hasLore()) {
            return;
        }
        if (!item.getItemMeta().getLore().contains("\u00a76\u76ae\u80a4\u65b9\u5757")) {
            return;
        }
        e.setCancelled(true);
        PlayerSkin skin = BridgingSkin.getSkin(e.getPlayer().getName(), e.getPlayer().getUniqueId().toString());
        skin.allSkin.add(new SkinSet(item.getType().name()));
        BridgingSkin.saveSkin(skin);
        e.getPlayer().getInventory().setItemInMainHand(null);
        e.getPlayer().sendMessage("&bI&en&cf &eBridge &7\u00bb \u00a7a\u6b64\u65b9\u5757\u5df2\u6dfb\u52a0\u5230\u4f60\u7684\u642d\u8def\u76ae\u80a4\u5e93\u5b58! \u8f93\u5165/bskin\u5207\u6362\u76ae\u80a4");
    }

    public static BridgingSkin getInstance() {
        return instance;
    }

    static {
        gson = new GsonBuilder().setPrettyPrinting().create();
    }
}

