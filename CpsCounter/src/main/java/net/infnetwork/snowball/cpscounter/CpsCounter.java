package net.infnetwork.snowball.cpscounter;

import java.util.HashMap;
import java.util.HashSet;
import java.util.UUID;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.plugin.java.JavaPlugin;

public class CpsCounter
extends JavaPlugin
implements Listener {
    private static CpsCounter instance;
    private final HashMap<UUID, Counter> counter = new HashMap<>();
    private final HashMap<UUID, MonitorThread> monitors = new HashMap<>();
    private final HashSet<Player> silentPlayer = new HashSet<>();

    public static Counter getCounter(Player p) {
        Counter c = CpsCounter.instance.counter.get(p.getUniqueId());
        if (c == null) {
            c = new Counter(p);
            CpsCounter.instance.counter.put(p.getUniqueId(), c);
        }
        return c;
    }

    public void onEnable() {
        instance = this;
        Bukkit.getPluginManager().registerEvents(this, this);
        CPSCommand cpsCommand = new CPSCommand();
        this.getCommand("cps").setExecutor(cpsCommand);
        this.getCommand("cps").setTabCompleter(cpsCommand);
    }

    @EventHandler
    public void onClick(PlayerInteractEvent e) {
        if (e.getAction().toString().startsWith("LEFT_CLICK_")) {
            if (e.useInteractedBlock() == Event.Result.DENY) {
                return;
            }
            Counter c = CpsCounter.getCounter(e.getPlayer());
            c.countLeftCPS();
        } else if (e.getAction().toString().startsWith("RIGHT_CLICK_")) {
            Counter c = CpsCounter.getCounter(e.getPlayer());
            c.countRightCPS();
        }
    }

    @EventHandler
    public void onLogout(PlayerQuitEvent e) {
        this.counter.remove(e.getPlayer().getUniqueId());
    }

    public static void startMonitor(Player player, Player target) {
        if (CpsCounter.instance.monitors.containsKey(player.getUniqueId())) {
            CpsCounter.instance.monitors.get(player.getUniqueId()).stopMonitor();
        }
        MonitorThread mon = new MonitorThread(player, target);
        CpsCounter.instance.monitors.put(player.getUniqueId(), mon);
        mon.startMonitor();
    }

    public static boolean isMoniting(Player player) {
        return CpsCounter.instance.monitors.containsKey(player.getUniqueId());
    }

    public static void stopMoniting(Player player) {
        if (CpsCounter.instance.monitors.containsKey(player.getUniqueId())) {
            CpsCounter.instance.monitors.get(player.getUniqueId()).stopMonitor();
            CpsCounter.instance.monitors.remove(player.getUniqueId());
        }
    }

    public static boolean isSilent(Player player) {
        return CpsCounter.instance.silentPlayer.contains(player);
    }

    public static boolean switchSilent(Player player) {
        if (CpsCounter.instance.silentPlayer.contains(player)) {
            CpsCounter.instance.silentPlayer.remove(player);
            return false;
        }
        CpsCounter.instance.silentPlayer.add(player);
        return true;
    }

    public static CpsCounter getInstance() {
        return instance;
    }
}
