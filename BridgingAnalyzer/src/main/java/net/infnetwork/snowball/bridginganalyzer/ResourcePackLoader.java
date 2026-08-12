/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  org.bukkit.Bukkit
 *  org.bukkit.event.EventHandler
 *  org.bukkit.event.Listener
 *  org.bukkit.event.player.PlayerJoinEvent
 *  org.bukkit.event.player.PlayerResourcePackStatusEvent
 *  org.bukkit.event.player.PlayerResourcePackStatusEvent$Status
 *  org.bukkit.plugin.Plugin
 */
package net.infnetwork.snowball.bridginganalyzer;

import org.bukkit.Bukkit;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerResourcePackStatusEvent;
import org.bukkit.plugin.Plugin;
import net.infnetwork.snowball.bridginganalyzer.BridgingAnalyzer;

public class ResourcePackLoader
implements Listener {
    @EventHandler
    public void onJoin(PlayerJoinEvent e) {
        Bukkit.getScheduler().runTaskLater((Plugin)BridgingAnalyzer.getInstance(), () -> e.getPlayer().setResourcePack("https://raw.githubusercontent.com/SakuraKoi/FileCloud/texture/BridgingHelper.zip"), 10L);
    }

    @EventHandler
    public void onRefuse(PlayerResourcePackStatusEvent e) {
        if (e.getStatus() == PlayerResourcePackStatusEvent.Status.ACCEPTED) {
            e.getPlayer().sendMessage("\u00a7b\u00a7lBridgingAnalyzer \u00a77>> \u00a7e\u6b63\u5728\u4e0b\u8f7d\u8d44\u6e90\u5305, \u8bf7\u7a0d\u5019...");
        } else if (e.getStatus() == PlayerResourcePackStatusEvent.Status.FAILED_DOWNLOAD) {
            e.getPlayer().sendMessage(new String[]{"\u00a7a\u00a7lBridgingAnalyzer \u00a77>> \u00a7c\u8d44\u6e90\u5305\u4e0b\u8f7d\u5931\u8d25, \u8bf7\u91cd\u8fdb\u6e38\u620f\u518d\u8bd5\u4e00\u6b21", "\u00a7a\u00a7lBridgingAnalyzer \u00a77>> \u00a7c\u5982\u679c\u6b64\u60c5\u51b5\u53cd\u590d\u51fa\u73b0, \u8bf7\u8054\u7cfb\u7ba1\u7406\u5458"});
        } else if (e.getStatus() == PlayerResourcePackStatusEvent.Status.SUCCESSFULLY_LOADED) {
            e.getPlayer().sendMessage(new String[]{"", "\u00a7a\u00a7lBridgingAnalyzer \u00a77>> \u00a7b\u6b64\u8d44\u6e90\u5305\u5bf9\u6c99\u77f3\u6dfb\u52a0\u4e86\u6807\u8bb0\u7ebf\u4ee5\u8f85\u52a9\u60a8\u7ec3\u4e60", "\u00a7a\u00a7lBridgingAnalyzer \u00a77>> \u00a7e  \u9876\u7aef\u7684\u7ea2\u84dd\u7ebf\u662f\u8d70\u642d\u8def\u7ebf", "\u00a7a\u00a7lBridgingAnalyzer \u00a77>> \u00a7e  \u4fa7\u9762\u7684\u6307\u793a\u7ebf\u662f\u63a8\u8350\u51c6\u5fc3\u4f4d\u7f6e", "\u00a7a\u00a7lBridgingAnalyzer \u00a77>> \u00a7a\u8f93\u5165 /bridge \u53ef\u4ee5\u5f00\u5173\u4e00\u4e9b\u6709\u7528\u7684\u529f\u80fd", ""});
        }
    }
}

