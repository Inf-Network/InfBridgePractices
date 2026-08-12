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
package net.infnetwork.snowball.cpscounter;

import java.util.HashMap;
import java.util.Map;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import net.infnetwork.snowball.cpscounter.Counter;
import net.infnetwork.snowball.cpscounter.CpsCounter;

public class CPSCommand
implements CommandExecutor {
    private Map<CommandSender, Long> executeTime = new HashMap<CommandSender, Long>();

    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (args.length == 0) {
            NetworkMessages.send(sender, "&e/cps <\u73a9\u5bb6>           \u67e5\u770b\u73a9\u5bb6\u7684CPS\u503c");
            NetworkMessages.send(sender, "&e/cps #mon <\u73a9\u5bb6>  \u76d1\u89c6\u73a9\u5bb6\u7684CPS\u503c");
        } else {
            String player = args[0];
            if (!sender.hasPermission("cpscounter.bypasslimit") && this.executeTime.get(sender) != null && System.currentTimeMillis() - this.executeTime.get(sender) < 1000L) {
                NetworkMessages.send(sender, "&c\u6307\u4ee4\u8f93\u5165\u7684\u592a\u9891\u7e41\u4e86.");
                return true;
            }
            if (args[0].equalsIgnoreCase("#mon")) {
                if (!(sender instanceof Player)) {
                    NetworkMessages.send(sender, "&c\u9519\u8bef: \u60a8\u5fc5\u987b\u767b\u5165\u6e38\u620f\u624d\u80fd\u4f7f\u7528CPS\u76d1\u89c6\u6a21\u5f0f.");
                    return true;
                }
                if (!sender.hasPermission("cpscounter.cps")) {
                    NetworkMessages.send(sender, "&c\u4f60\u6ca1\u6709\u6743\u9650\u6267\u884c\u6b64\u547d\u4ee4");
                    return true;
                }
                if (CpsCounter.isMoniting((Player)sender)) {
                    CpsCounter.stopMoniting((Player)sender);
                    NetworkMessages.send(sender, "&a\u76d1\u89c6\u5df2\u505c\u6b62");
                    return true;
                }
                if (args.length != 2) {
                    NetworkMessages.send(sender, "&e/cps #mon <\u73a9\u5bb6>  \u76d1\u89c6\u73a9\u5bb6\u7684CPS\u503c");
                    return true;
                }
                player = args[1];
                Player p = this.getPlayer(sender, player);
                if (p == null) {
                    return true;
                }
                CpsCounter.startMonitor((Player)sender, p);
                NetworkMessages.send(sender, "&a\u5f00\u59cb\u76d1\u89c6\u73a9\u5bb6 " + p.getName());
                return true;
            }
            if (args[0].equalsIgnoreCase("#silent")) {
                if (!(sender instanceof Player)) {
                    NetworkMessages.send(sender, "&c\u9519\u8bef: \u60a8\u5fc5\u987b\u767b\u5165\u6e38\u620f\u624d\u80fd\u5207\u6362CPS\u81ea\u52a8\u8b66\u544a.");
                    return true;
                }
                if (!sender.hasPermission("cpscounter.cps")) {
                    NetworkMessages.send(sender, "&c\u4f60\u6ca1\u6709\u6743\u9650\u6267\u884c\u6b64\u547d\u4ee4");
                    return true;
                }
                if (CpsCounter.switchSilent((Player)sender)) {
                    NetworkMessages.send(sender, "&cCPS\u81ea\u52a8\u8b66\u544a\u5df2\u5bf9\u60a8\u5173\u95ed");
                    return true;
                }
                NetworkMessages.send(sender, "&aCPS\u81ea\u52a8\u8b66\u544a\u5df2\u5f00\u542f");
                return true;
            }
            Player p = this.getPlayer(sender, player);
            if (p == null) {
                return true;
            }
            Counter counter = CpsCounter.getCounter(p);
            NetworkMessages.send(sender, "&b\u73a9\u5bb6 &a" + p.getName()
                    + "&6 | &bCPS &e" + (counter.getLastClickMs() > 1000L ? 0 : counter.getCPS())
                    + "&b / &d" + counter.getMaxCPS()
                    + "&6 | &bLCS &e" + (counter.getLeftLastClickMs() > 1000L ? 0 : counter.getLeftCPS())
                    + "&b / &d" + counter.getLeftMaxCPS()
                    + "&6 | &bRCS &e" + (counter.getRightLastClickMs() > 1000L ? 0 : counter.getRightCPS())
                    + "&b / &d" + counter.getRightMaxCPS());
            this.executeTime.put(sender, System.currentTimeMillis());
        }
        return true;
    }

    private Player getPlayer(CommandSender sender, String player) {
        OfflinePlayer offp = Bukkit.getOfflinePlayer((String)player);
        if (offp == null) {
            NetworkMessages.send(sender, "&c\u9519\u8bef: \u73a9\u5bb6 " + player + " \u4e0d\u5b58\u5728.");
            return null;
        }
        if (!offp.isOnline()) {
            NetworkMessages.send(sender, "&c\u9519\u8bef: \u73a9\u5bb6 " + offp.getName() + " \u4e0d\u5728\u7ebf.");
            return null;
        }
        return offp.getPlayer();
    }
}
