package net.infnetwork.snowball.blocklv.events;

import net.infnetwork.snowball.blocklv.BlockLv;
import net.infnetwork.snowball.blocklv.core.PointManger;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerMoveEvent;

public class PlayerMove
implements Listener {
    @EventHandler
    public void onMove(PlayerMoveEvent e) {
        if (e.getTo().getY() <= 1.0 && BlockLv.getInstance().killPlayer.get(e.getPlayer()) != null) {
            for (Player p : Bukkit.getOnlinePlayers()) {
                p.sendMessage(ChatColor.translateAlternateColorCodes((char)'&', (String)("&2" + BlockLv.getInstance().killPlayer.get(e.getPlayer()).getName() + " &7\u628a&c " + e.getPlayer().getName() + " &7\u63a8\u4e0b\u4e86\u865a\u7a7a.")));
            }
            BlockLv.getInstance().killPlayer.get(e.getPlayer()).sendMessage(ChatColor.translateAlternateColorCodes((char)'&', (String)"&6+10 \u642d\u8def\u7ecf\u9a8c (\u51fb\u6740\u5956\u52b1)"));
            PointManger.addPx(10L, BlockLv.getInstance().killPlayer.get(e.getPlayer()).getUniqueId());
            BlockLv.getInstance().killPlayer.remove(e.getPlayer());
        }
    }
}
