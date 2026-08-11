/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  org.bukkit.Material
 *  org.bukkit.command.CommandSender
 */
package sakura.kooi.BridgingSkin;

import java.io.File;
import java.util.ArrayList;
import org.bukkit.Material;
import org.bukkit.command.CommandSender;
import sakura.kooi.BridgingSkin.BridgingSkin;
import sakura.kooi.BridgingSkin.data.PlayerSkin;
import sakura.kooi.BridgingSkin.data.SkinSet;

public class ClearThread
implements Runnable {
    private final CommandSender sender;
    private final Material material;

    @Override
    public void run() {
        int total = 0;
        int cleared = 0;
        for (File file2 : BridgingSkin.rootDir.listFiles(file -> {
            if (file.getName().contains("-")) {
                return false;
            }
            return file.getName().endsWith(".json");
        })) {
            boolean edited = false;
            try {
                PlayerSkin skin = BridgingSkin.loadSkin(file2);
                // \u539f\u7248\u8fd9\u91cc\u6309 material + data \u4e24\u7ea7\u5339\u914d(data \u4e3a -1 \u65f6\u4e0d\u9650)\u3002
                // \u6241\u5e73\u5316\u540e data \u5df2\u5220,\u53ea\u6309 material \u5339\u914d\u3002
                if (skin.currentSkin.material.equals(this.material.name())) {
                    skin.currentSkin = new SkinSet();
                    edited = true;
                }
                ArrayList<SkinSet> remove = new ArrayList<SkinSet>();
                for (SkinSet entry : skin.allSkin) {
                    if (!entry.material.equals(this.material.name())) continue;
                    remove.add(entry);
                    edited = true;
                }
                skin.allSkin.removeAll(remove);
                ++total;
                if (!edited) continue;
                BridgingSkin.saveSkin(skin);
                BridgingSkin.skins.remove(skin.player);
                ++cleared;
                this.sender.sendMessage("&bI&en&cf &eBridge &7\u00bb \u00a7e\u6210\u529f\u6e05\u7406\u73a9\u5bb6 " + skin.player + " \u7684\u76ae\u80a4\u5e93\u5b58");
            }
            catch (Exception e) {
                e.printStackTrace();
                this.sender.sendMessage("&bI&en&cf &eBridge &7\u00bb \u00a7c\u5904\u7406\u6587\u4ef6 " + file2.getName() + " \u65f6\u51fa\u9519");
            }
        }
        this.sender.sendMessage("&bI&en&cf &eBridge &7\u00bb \u00a7b\u76ae\u80a4\u6e05\u7406\u5b8c\u6bd5 \u5171\u626b\u63cf " + total + " \u73a9\u5bb6 / \u6e05\u7406 " + cleared + " \u73a9\u5bb6");
    }

    public ClearThread(CommandSender sender, Material material) {
        this.sender = sender;
        this.material = material;
    }
}

