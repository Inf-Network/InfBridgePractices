package net.infnetwork.snowball.blocklv.papi;

import net.infnetwork.snowball.blocklv.BlockLv;
import net.infnetwork.snowball.blocklv.api.LevelComponents;
import net.infnetwork.snowball.blocklv.core.PointManger;
import net.infnetwork.snowball.blocklv.text.LegacyComponentOutput;
import me.clip.placeholderapi.expansion.PlaceholderExpansion;
import org.bukkit.entity.Player;

public class PAPIHooker
extends PlaceholderExpansion {
    public String onPlaceholderRequest(Player player, String name) {
        if (player == null) {
            return "";
        }
        if (PointManger.players.get(player.getUniqueId()) == null) {
            return "Loading...";
        }
        if (name.equals("lv")) {
            return String.valueOf(PointManger.getLv(player.getUniqueId()));
        }
        if (name.equals("level_display")) {
            return LegacyComponentOutput.serialize(
                    LevelComponents.level(PointManger.getLv(player.getUniqueId())));
        }
        if (name.equals("px")) {
            return String.valueOf(PointManger.getPx(player.getUniqueId()));
        }
        if (name.equals("uppx")) {
            return String.valueOf(PointManger.getNextLvPx(PointManger.getLv(player.getUniqueId())) - PointManger.getPx(player.getUniqueId()));
        }
        if (name.equals("prefix")) {
            return LegacyComponentOutput.serialize(
                    LevelComponents.badge(PointManger.getLv(player.getUniqueId())));
        }
        if (name.equals("kill")) {
            return String.valueOf(BlockLv.getInstance().getConfig().getInt("data." + player.getName() + ".kill"));
        }
        return null;
    }

    public String getIdentifier() {
        return "blocklv";
    }

    public String getAuthor() {
        return "luanmenglei";
    }

    public String getVersion() {
        return "1.0";
    }
}
