package net.infnetwork.snowball.cpscounter;

import java.util.ArrayList;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

public class Counter {
    private static final LegacyComponentSerializer LEGACY =
            LegacyComponentSerializer.legacySection();

    private final ArrayList<Long> counterRightCPS = new ArrayList<>();
    private final ArrayList<Long> counterLeftCPS = new ArrayList<>();
    private int maxRightCPS = 0;
    private int maxLeftCPS = 0;
    private int maxCPS = 0;
    private final Player player;
    long timeout = -1L;

    public Counter(Player p) {
        this.player = p;
    }

    public void countRightCPS() {
        this.counterRightCPS.add(System.currentTimeMillis());
        this.removeRightCPSTimeout();
        if (this.counterRightCPS.size() > this.maxRightCPS) {
            this.maxRightCPS = this.counterRightCPS.size();
        }
        if (this.getLeftCPS() + this.getRightCPS() > this.maxCPS) {
            this.maxCPS = this.getLeftCPS() + this.getRightCPS();
        }
        if (this.counterRightCPS.size() > 18 && !this.player.hasPermission("cpscounter.bypass")) {
            this.warnOP();
        }
    }

    public void countLeftCPS() {
        this.counterLeftCPS.add(System.currentTimeMillis());
        this.removeLeftCPSTimeout();
        if (this.counterLeftCPS.size() > this.maxLeftCPS) {
            this.maxLeftCPS = this.counterLeftCPS.size();
        }
        if (this.getLeftCPS() + this.getRightCPS() > this.maxCPS) {
            this.maxCPS = this.getLeftCPS() + this.getRightCPS();
        }
        if (this.counterLeftCPS.size() > 18 && !this.player.hasPermission("cpscounter.bypass")) {
            this.warnOP();
        }
    }

    public int getRightCPS() {
        return this.counterRightCPS.size();
    }

    public int getLeftCPS() {
        return this.counterLeftCPS.size();
    }

    public int getCPS() {
        return this.getRightCPS() + this.getLeftCPS();
    }

    public int getRightMaxCPS() {
        return this.maxRightCPS;
    }

    public int getLeftMaxCPS() {
        return this.maxLeftCPS;
    }

    public int getMaxCPS() {
        return this.maxCPS;
    }

    public long getRightLastClickMs() {
        if (this.counterRightCPS.isEmpty()) {
            return -1L;
        }
        return System.currentTimeMillis() - this.counterRightCPS.get(this.counterRightCPS.size() - 1);
    }

    public long getLeftLastClickMs() {
        if (this.counterLeftCPS.isEmpty()) {
            return -1L;
        }
        return System.currentTimeMillis() - this.counterLeftCPS.get(this.counterLeftCPS.size() - 1);
    }

    public long getLastClickMs() {
        return Math.min(this.getRightLastClickMs(), this.getLeftLastClickMs());
    }

    private void removeRightCPSTimeout() {
        while (!this.counterRightCPS.isEmpty() && System.currentTimeMillis() - this.counterRightCPS.get(0) > 1000L) {
            this.counterRightCPS.remove(0);
        }
    }

    private void removeLeftCPSTimeout() {
        while (!this.counterLeftCPS.isEmpty() && System.currentTimeMillis() - this.counterLeftCPS.get(0) > 1000L) {
            this.counterLeftCPS.remove(0);
        }
    }

    private void warnOP() {
        Bukkit.getScheduler().runTaskLater(CpsCounter.getInstance(), () -> {
            if (!this.player.isOnline()) {
                return;
            }
            if (System.currentTimeMillis() > this.timeout) {
                this.timeout = System.currentTimeMillis() + 5000L;
                for (Player op : Bukkit.getOnlinePlayers()) {
                    if (!op.hasPermission("cpscounter.monitor")) continue;
                    if (CpsCounter.isSilent(op)) {
                        return;
                    }
                    op.sendMessage(LEGACY.deserialize("\u00a7b[CPS] \u00a7c\u73a9\u5bb6 " + this.player.getName() + " \u5efa\u8bae\u68c0\u67e5\u662f\u5426\u8fde\u70b9\u00a7b | \u00a7cCPS \u00a7e" + this.getCPS() + "\u00a7c / \u00a7d" + this.getMaxCPS() + "\u00a7b | \u00a7cRCS \u00a7e" + this.getRightCPS() + "\u00a7c / \u00a7d" + this.getRightMaxCPS() + "\u00a7b | \u00a7cLCS \u00a7e" + this.getLeftCPS() + "\u00a7c / \u00a7d" + this.getLeftMaxCPS()));
                }
            }
        }, 7L);
    }
}
