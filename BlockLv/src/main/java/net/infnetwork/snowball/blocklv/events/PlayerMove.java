package net.infnetwork.snowball.blocklv.events;

import net.infnetwork.snowball.blocklv.BlockLv;
import net.infnetwork.snowball.blocklv.core.PointManger;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerMoveEvent;

public class PlayerMove
implements Listener {
    @EventHandler
    public void onMove(PlayerMoveEvent e) {
        if (e.getTo().getY() <= 1.0 && BlockLv.getInstance().killPlayer.get(e.getPlayer()) != null) {
            Player attacker = BlockLv.getInstance().killPlayer.get(e.getPlayer());
            Component announcement = Component.text(attacker.getName() + " ", NamedTextColor.DARK_GREEN)
                    .append(Component.text("\u628a", NamedTextColor.GRAY))
                    .append(Component.text(" " + e.getPlayer().getName() + " ", NamedTextColor.RED))
                    .append(Component.text("\u63a8\u4e0b\u4e86\u865a\u7a7a.", NamedTextColor.GRAY));
            for (Player p : Bukkit.getOnlinePlayers()) {
                p.sendMessage(announcement);
            }
            attacker.sendMessage(Component.text("+10 \u642d\u8def\u7ecf\u9a8c (\u51fb\u6740\u5956\u52b1)", NamedTextColor.GOLD));
            PointManger.addPx(10L, attacker.getUniqueId());
            BlockLv.getInstance().killPlayer.remove(e.getPlayer());
        }
    }
}
