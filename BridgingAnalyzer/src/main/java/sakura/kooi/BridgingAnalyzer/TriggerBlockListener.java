/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  org.bukkit.Bukkit
 *  org.bukkit.GameMode
 *  org.bukkit.Location
 *  org.bukkit.Material
 *  org.bukkit.Sound
 *  org.bukkit.block.Block
 *  org.bukkit.block.BlockFace
 *  org.bukkit.entity.Player
 *  org.bukkit.event.EventHandler
 *  org.bukkit.event.Listener
 *  org.bukkit.event.block.BlockPlaceEvent
 *  org.bukkit.event.player.PlayerMoveEvent
 *  org.bukkit.event.player.PlayerToggleSneakEvent
 *  org.bukkit.plugin.Plugin
 *  org.bukkit.potion.PotionEffect
 *  org.bukkit.potion.PotionEffectType
 *  org.bukkit.util.Vector
 */
package sakura.kooi.BridgingAnalyzer;

import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.event.player.PlayerToggleSneakEvent;
import org.bukkit.plugin.Plugin;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import sakura.kooi.BridgingAnalyzer.BridgingAnalyzer;
import sakura.kooi.BridgingAnalyzer.Counter;
import sakura.kooi.BridgingAnalyzer.utils.FireworkUtils;
import org.bukkit.Particle;
import sakura.kooi.BridgingAnalyzer.utils.ParticleRing;
import sakura.kooi.BridgingAnalyzer.utils.TeleportRingEffect;
import sakura.kooi.BridgingAnalyzer.utils.TitleUtils;
import sakura.kooi.BridgingAnalyzer.utils.Utils;

public class TriggerBlockListener
implements Listener {
    @EventHandler
    public void antiTriggerBlockCover(BlockPlaceEvent e) {
        if (e.getPlayer() != null) {
            if (e.getPlayer().getGameMode() == GameMode.CREATIVE) {
                return;
            }
            if (this.isTriggerBlock(e.getBlock().getRelative(BlockFace.DOWN)) || this.isTriggerBlock(e.getBlock().getRelative(BlockFace.DOWN, 2))) {
                Bukkit.getScheduler().runTaskLater((Plugin)BridgingAnalyzer.getInstance(), () -> {
                    Utils.breakBlock(e.getBlock());
                    BridgingAnalyzer.getCounter(e.getPlayer()).removeBlockRecord(e.getBlock());
                }, 100L);
            }
        }
    }

    private boolean isTriggerBlock(Block b) {
        if (b.getType() == Material.EMERALD_BLOCK) {
            return true;
        }
        if (b.getType() == Material.REDSTONE_BLOCK) {
            return true;
        }
        if (b.getType() == Material.LAPIS_BLOCK) {
            return true;
        }
        return b.getType() == Material.BEACON;
    }

    @EventHandler
    public void triggerCheckPointBlock(PlayerMoveEvent e) {
        if (e.getFrom().getBlock().equals(e.getTo().getBlock())) {
            return;
        }
        if (e.getPlayer().getNoDamageTicks() != 0) {
            return;
        }
        if (e.getPlayer().getGameMode() == GameMode.SPECTATOR) {
            return;
        }
        if (e.getTo().getBlock().getRelative(BlockFace.DOWN).getType() == Material.EMERALD_BLOCK) {
            e.getPlayer().setNoDamageTicks(40);
            Location spawnLoc = e.getTo().getBlock().getLocation().add(0.5, 1.0, 0.5);
            spawnLoc.setYaw(e.getPlayer().getLocation().getYaw());
            spawnLoc.setPitch(e.getPlayer().getLocation().getPitch());
            Counter c = BridgingAnalyzer.getCounter(e.getPlayer());
            c.setCheckPoint(spawnLoc, e.getPlayer());
            new ParticleRing(e.getTo().getBlock().getLocation().add(0.5, 1.5, 0.5), Particle.CLOUD, 1L){

                @Override
                public void onFinish() {
                }
            };
            TitleUtils.sendTitle(e.getPlayer(), "", "\u00a7a\u4f20\u9001\u70b9\u5df2\u8bbe\u7f6e", 5, 10, 5);
            e.getPlayer().getWorld().playSound(e.getTo(), Sound.ENTITY_EXPERIENCE_ORB_PICKUP, 1.0f, 1.0f);
        }
    }

    @EventHandler
    public void triggerEndPointBlock(final PlayerMoveEvent e) {
        if (e.getFrom().getBlock().equals(e.getTo().getBlock())) {
            return;
        }
        if (e.getPlayer().getNoDamageTicks() != 0) {
            return;
        }
        if (e.getPlayer().getGameMode() == GameMode.SPECTATOR) {
            return;
        }
        if (e.getTo().getBlock().getRelative(BlockFace.DOWN).getType() == Material.REDSTONE_BLOCK) {
            e.getPlayer().setNoDamageTicks(40);
            new ParticleRing(e.getTo().getBlock().getLocation().add(0.5, 0.1, 0.5), Particle.WITCH, 20L){

                @Override
                public void onFinish() {
                    FireworkUtils.shootFirework(e.getPlayer());
                }
            };
            BridgingAnalyzer.getCounter(e.getPlayer()).vectoryBreakBlock(e.getPlayer());
            TitleUtils.sendTitle(e.getPlayer(), "\u00a76\u00a7lVICTORY", "", 5, 20, 5);
            e.getPlayer().getWorld().playSound(e.getTo(), Sound.ENTITY_PLAYER_LEVELUP, 1.0f, 1.0f);
        }
    }

    @EventHandler
    public void triggerSpawnPointBlock(final PlayerMoveEvent e) {
        if (e.getFrom().getBlock().equals(e.getTo().getBlock())) {
            return;
        }
        if (e.getPlayer().getNoDamageTicks() != 0) {
            return;
        }
        if (e.getPlayer().getGameMode() == GameMode.SPECTATOR) {
            return;
        }
        if (e.getTo().getBlock().getRelative(BlockFace.DOWN).getType() == Material.LAPIS_BLOCK) {
            e.getPlayer().setNoDamageTicks(40);
            Counter c = BridgingAnalyzer.getCounter(e.getPlayer());
            c.setCheckPoint(Bukkit.getWorld((String)"world").getSpawnLocation().add(0.5, 1.0, 0.5), e.getPlayer());
            c.resetMax();
            new ParticleRing(e.getTo().getBlock().getLocation().add(0.5, 1.5, 0.5), Particle.FIREWORK, 35L){

                @Override
                public void onFinish() {
                    BridgingAnalyzer.teleportCheckPoint(e.getPlayer());
                    BridgingAnalyzer.clearEffect(e.getPlayer());
                    if (!e.getPlayer().isOp()) {
                        e.getPlayer().getInventory().setHelmet(null);
                        e.getPlayer().getInventory().setChestplate(null);
                        e.getPlayer().getInventory().setLeggings(null);
                        e.getPlayer().getInventory().setBoots(null);
                    }
                }
            };
            TitleUtils.sendTitle(e.getPlayer(), "", "\u00a7b\u6b63\u5728\u8fd4\u56de\u51fa\u751f\u70b9...", 5, 25, 5);
            e.getPlayer().getWorld().playSound(e.getTo(), Sound.ENTITY_EXPERIENCE_ORB_PICKUP, 1.0f, 1.0f);
        }
    }

    @EventHandler
    public void triggerSpeedPlate(PlayerMoveEvent e) {
        if (e.getFrom().getBlock().equals(e.getTo().getBlock())) {
            return;
        }
        if (e.getPlayer().getNoDamageTicks() != 0) {
            return;
        }
        if (e.getPlayer().getGameMode() == GameMode.SPECTATOR) {
            return;
        }
        if (e.getTo().getBlock().getType() == Material.LIGHT_WEIGHTED_PRESSURE_PLATE) {
            e.getPlayer().setNoDamageTicks(20);
            e.getPlayer().addPotionEffect(new PotionEffect(PotionEffectType.SPEED, 100, 2), true);
        }
    }

    @EventHandler
    public void triggerTeleportBlock(final PlayerMoveEvent e) {
        if (e.getFrom().getBlock().equals(e.getTo().getBlock())) {
            return;
        }
        if (e.getPlayer().getNoDamageTicks() != 0) {
            return;
        }
        if (e.getPlayer().getGameMode() == GameMode.SPECTATOR) {
            return;
        }
        if (e.getTo().getBlock().getRelative(BlockFace.DOWN).getType() == Material.BEACON) {
            e.getPlayer().setNoDamageTicks(20);
            Block to = e.getTo().getBlock();
            while (isPassThrough(to.getType()) && to.getY() < to.getWorld().getMaxHeight() - 1) {
                to = to.getRelative(BlockFace.UP);
            }
            if (to.getType() == Material.BEACON) {
                e.getPlayer().setNoDamageTicks(50);
                final Block teleportTarget = to;
                new TeleportRingEffect(e.getTo().getBlock().getLocation().add(0.5, 0.0, 0.5), teleportTarget.getLocation().add(0.5, 1.0, 0.5), 1L, 0, 40){

                    @Override
                    public void onFinish() {
                        Location loc = teleportTarget.getLocation().add(0.5, 1.5, 0.5);
                        loc.setYaw(e.getPlayer().getLocation().getYaw());
                        loc.setPitch(e.getPlayer().getLocation().getPitch());
                        e.getPlayer().teleport(loc);
                    }
                };
                e.getPlayer().getWorld().playSound(e.getTo(), Sound.ENTITY_ENDERMAN_TELEPORT, 1.0f, 1.0f);
            }
        }
    }

    @EventHandler
    public void triggerTeleportBlock(final PlayerToggleSneakEvent e) {
        if (e.isSneaking()) {
            return;
        }
        if (e.getPlayer().getNoDamageTicks() != 0) {
            return;
        }
        if (e.getPlayer().getGameMode() == GameMode.SPECTATOR) {
            return;
        }
        if (e.getPlayer().getLocation().getBlock().getRelative(BlockFace.DOWN).getType() == Material.BEACON) {
            e.getPlayer().setNoDamageTicks(20);
            Block to = e.getPlayer().getLocation().getBlock().getRelative(BlockFace.DOWN, 2);
            while (isPassThrough(to.getType()) && to.getY() > to.getWorld().getMinHeight()) {
                to = to.getRelative(BlockFace.DOWN);
            }
            if (to.getType() == Material.BEACON) {
                e.getPlayer().setNoDamageTicks(50);
                final Block teleportTarget = to;
                new TeleportRingEffect(e.getPlayer().getLocation().getBlock().getLocation().add(0.5, 0.0, 0.5), teleportTarget.getLocation().add(0.5, 1.0, 0.5), 1L, 10, 40){

                    @Override
                    public void onFinish() {
                        Location loc = teleportTarget.getLocation().add(0.5, 1.5, 0.5);
                        loc.setYaw(e.getPlayer().getLocation().getYaw());
                        loc.setPitch(e.getPlayer().getLocation().getPitch());
                        e.getPlayer().teleport(loc);
                    }
                };
                e.getPlayer().getWorld().playSound(e.getPlayer().getLocation(), Sound.ENTITY_ENDERMAN_TELEPORT, 1.0f, 1.0f);
            }
        }
    }

    /**
     * 判断方块能否被信标传送穿透。
     *
     * 1.8 时代这里是三个常量的直接比较(AIR / STAINED_GLASS_PANE / WALL_SIGN /
     * SIGN_POST)。1.13 扁平化后玻璃板拆成 16 色、告示牌按木种与朝向拆成几十种,
     * 只能改成按类型族判断。
     *
     * @param type 待判断的方块类型
     * @return 可穿透返回 true
     */
    private static boolean isPassThrough(org.bukkit.Material type) {
        if (type == org.bukkit.Material.AIR) {
            return true;
        }
        // 覆盖 16 色染色玻璃板与无色玻璃板
        if (type.name().endsWith("GLASS_PANE")) {
            return true;
        }
        // Tag.ALL_SIGNS 同时涵盖立式、墙上与悬挂告示牌的全部木种
        return org.bukkit.Tag.ALL_SIGNS.isTagged(type);
    }
}

