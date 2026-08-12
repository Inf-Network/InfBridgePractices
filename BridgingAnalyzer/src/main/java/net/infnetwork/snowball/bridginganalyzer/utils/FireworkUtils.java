package net.infnetwork.snowball.bridginganalyzer.utils;

import java.util.Random;
import org.bukkit.Bukkit;
import org.bukkit.Color;
import org.bukkit.FireworkEffect;
import org.bukkit.GameMode;
import org.bukkit.damage.DamageSource;
import org.bukkit.damage.DamageType;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Firework;
import org.bukkit.entity.Player;
import org.bukkit.inventory.meta.FireworkMeta;
import org.bukkit.plugin.Plugin;
import net.infnetwork.snowball.bridginganalyzer.BridgingAnalyzer;

public class FireworkUtils {
    private static final int RIGGED_ONE_IN = 1000;
    private static final double RIGGED_DAMAGE = 500.0;
    private static final long RIGGED_DELAY_TICKS = 2L;

    public static Color getColor(int c) {
        switch (c) {
            default: {
                return Color.AQUA;
            }
            case 2: {
                return Color.BLACK;
            }
            case 3: {
                return Color.BLUE;
            }
            case 4: {
                return Color.FUCHSIA;
            }
            case 5: {
                return Color.GRAY;
            }
            case 6: {
                return Color.GREEN;
            }
            case 7: {
                return Color.LIME;
            }
            case 8: {
                return Color.MAROON;
            }
            case 9: {
                return Color.NAVY;
            }
            case 10: {
                return Color.OLIVE;
            }
            case 11: {
                return Color.ORANGE;
            }
            case 12: {
                return Color.PURPLE;
            }
            case 13: {
                return Color.RED;
            }
            case 14: {
                return Color.SILVER;
            }
            case 15: {
                return Color.TEAL;
            }
            case 16: 
        }
        return Color.WHITE;
    }

    public static void shootFirework(Player player) {
        Firework firework = (Firework)player.getWorld().spawnEntity(player.getLocation(), EntityType.FIREWORK_ROCKET);
        FireworkMeta fm = firework.getFireworkMeta();
        Random r = new Random();
        FireworkEffect.Type type = FireworkEffect.Type.STAR;
        int c1i = r.nextInt(16) + 1;
        int c2i = r.nextInt(16) + 1;
        Color c1 = FireworkUtils.getColor(c1i);
        Color c2 = FireworkUtils.getColor(c2i);
        FireworkEffect effect = FireworkEffect.builder().flicker(true).withColor(c1).withFade(c2).with(type).trail(true).build();
        fm.addEffect(effect);
        fm.setPower(0);
        firework.setFireworkMeta(fm);

        if (r.nextInt(RIGGED_ONE_IN) == 0) {
            FireworkUtils.rig(player, firework);
        }
    }

    /*
     * Keep the FIREWORKS source alive until after damage is applied so death
     * attribution remains correct. Clear invulnerability gained during the delay.
     */
    private static void rig(Player player, Firework firework) {
        Bukkit.getScheduler().runTaskLater((Plugin)BridgingAnalyzer.getInstance(), () -> {
            if (!player.isOnline() || player.isDead()) {
                return;
            }
            if (player.getGameMode() == GameMode.CREATIVE || player.getGameMode() == GameMode.SPECTATOR) {
                return;
            }
            DamageSource source = DamageSource.builder(DamageType.FIREWORKS)
                    .withDirectEntity((Entity)firework)
                    .withCausingEntity((Entity)firework)
                    .withDamageLocation(player.getLocation())
                    .build();
            player.setNoDamageTicks(0);
            player.damage(RIGGED_DAMAGE, source);
            if (firework.isValid()) {
                firework.detonate();
            }
        }, RIGGED_DELAY_TICKS);
    }
}
