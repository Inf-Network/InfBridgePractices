/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  org.bukkit.Bukkit
 *  org.bukkit.OfflinePlayer
 *  org.bukkit.command.Command
 *  org.bukkit.command.CommandExecutor
 *  org.bukkit.command.CommandSender
 *  org.bukkit.entity.Player
 */
package sakura.kooi.CpsCounter;

import java.util.HashMap;
import java.util.Map;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import sakura.kooi.CpsCounter.Counter;
import sakura.kooi.CpsCounter.CpsCounter;

public class CPSCommand
implements CommandExecutor {
    private Map<CommandSender, Long> executeTime = new HashMap<CommandSender, Long>();

    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (args.length == 0) {
            sender.sendMessage("\u00a7bI\u00a7en\u00a7cf \u00a7bBridge \u00a77\u00bb \u00a7e/cps <\u73a9\u5bb6>           \u67e5\u770b\u73a9\u5bb6\u7684CPS\u503c");
            sender.sendMessage("\u00a7bI\u00a7en\u00a7cf \u00a7bBridge \u00a77\u00bb \u00a7e/cps #mon <\u73a9\u5bb6>  \u76d1\u89c6\u73a9\u5bb6\u7684CPS\u503c");
        } else {
            String player = args[0];
            if (!sender.hasPermission("cpscounter.bypasslimit") && this.executeTime.get(sender) != null && System.currentTimeMillis() - this.executeTime.get(sender) < 1000L) {
                sender.sendMessage("\u00a7bI\u00a7en\u00a7cf \u00a7bBridge \u00a77\u00bb \u00a7c\u6307\u4ee4\u8f93\u5165\u7684\u592a\u9891\u7e41\u4e86.");
                return true;
            }
            if (args[0].equalsIgnoreCase("#mon")) {
                if (!(sender instanceof Player)) {
                    sender.sendMessage("\u00a7bI\u00a7en\u00a7cf \u00a7bBridge \u00a77\u00bb \u00a7c\u9519\u8bef: \u60a8\u5fc5\u987b\u767b\u5165\u6e38\u620f\u624d\u80fd\u4f7f\u7528CPS\u76d1\u89c6\u6a21\u5f0f.");
                    return true;
                }
                if (!sender.hasPermission("cpscounter.cps")) {
                    sender.sendMessage("\u00a7bI\u00a7en\u00a7cf \u00a7bBridge \u00a77\u00bb \u00a7c\u4f60\u6ca1\u6709\u6743\u9650\u6267\u884c\u6b64\u547d\u4ee4");
                    return true;
                }
                if (CpsCounter.isMoniting((Player)sender)) {
                    CpsCounter.stopMoniting((Player)sender);
                    sender.sendMessage("\u00a7bI\u00a7en\u00a7cf \u00a7bBridge \u00a77\u00bb \u00a7a\u76d1\u89c6\u5df2\u505c\u6b62");
                    return true;
                }
                if (args.length != 2) {
                    sender.sendMessage("\u00a7bI\u00a7en\u00a7cf \u00a7bBridge \u00a77\u00bb \u00a7e/cps #mon <\u73a9\u5bb6>  \u76d1\u89c6\u73a9\u5bb6\u7684CPS\u503c");
                    return true;
                }
                player = args[1];
                Player p = this.getPlayer(sender, player);
                if (p == null) {
                    return true;
                }
                CpsCounter.startMonitor((Player)sender, p);
                sender.sendMessage("\u00a7bI\u00a7en\u00a7cf \u00a7bBridge \u00a77\u00bb \u00a7a\u5f00\u59cb\u76d1\u89c6\u73a9\u5bb6 " + p.getName());
                return true;
            }
            if (args[0].equalsIgnoreCase("#silent")) {
                if (!(sender instanceof Player)) {
                    return true;
                }
                if (!sender.hasPermission("cpscounter.cps")) {
                    sender.sendMessage("\u00a7bI\u00a7en\u00a7cf \u00a7bBridge \u00a77\u00bb \u00a7c\u4f60\u6ca1\u6709\u6743\u9650\u6267\u884c\u6b64\u547d\u4ee4");
                    return true;
                }
                if (CpsCounter.switchSilent((Player)sender)) {
                    sender.sendMessage("\u00a7bI\u00a7en\u00a7cf \u00a7bBridge \u00a77\u00bb \u00a7cCPS\u81ea\u52a8\u8b66\u544a\u5df2\u5bf9\u60a8\u5173\u95ed");
                    return true;
                }
                sender.sendMessage("\u00a7bI\u00a7en\u00a7cf \u00a7bBridge \u00a77\u00bb \u00a7aCPS\u81ea\u52a8\u8b66\u544a\u5df2\u5f00\u542f");
                return true;
            }
            Player p = this.getPlayer(sender, player);
            if (p == null) {
                return true;
            }
            Counter counter = CpsCounter.getCounter(p);
            sender.sendMessage("\u00a7bI\u00a7en\u00a7cf \u00a7bBridge \u00a77\u00bb \u00a7b\u73a9\u5bb6 \u00a7a" + p.getName() + "\u00a76 | \u00a7bCPS \u00a7e" + (counter.getLastClickMs() > 1000L ? 0 : counter.getCPS()) + "\u00a7b / \u00a7d" + counter.getMaxCPS() + "\u00a76 | \u00a7bLCS \u00a7e" + (counter.getLeftLastClickMs() > 1000L ? 0 : counter.getLeftCPS()) + "\u00a7b / \u00a7d" + counter.getLeftMaxCPS() + "\u00a76 | \u00a7bRCS \u00a7e" + (counter.getRightLastClickMs() > 1000L ? 0 : counter.getRightCPS()) + "\u00a7b / \u00a7d" + counter.getRightMaxCPS());
            this.executeTime.put(sender, System.currentTimeMillis());
        }
        return true;
    }

    private Player getPlayer(CommandSender sender, String player) {
        OfflinePlayer offp = Bukkit.getOfflinePlayer((String)player);
        if (offp == null) {
            sender.sendMessage("\u00a7bI\u00a7en\u00a7cf \u00a7bBridge \u00a77\u00bb \u00a7c\u9519\u8bef: \u73a9\u5bb6 " + player + " \u4e0d\u5b58\u5728.");
            return null;
        }
        if (!offp.isOnline()) {
            sender.sendMessage("\u00a7bI\u00a7en\u00a7cf \u00a7bBridge \u00a77\u00bb \u00a7c\u9519\u8bef: \u73a9\u5bb6 " + offp.getName() + " \u4e0d\u5728\u7ebf.");
            return null;
        }
        return offp.getPlayer();
    }
}

