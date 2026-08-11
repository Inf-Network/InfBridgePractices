package sakura.kooi.BridgingAnalyzer.targets;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import org.bukkit.Location;
import org.bukkit.NamespacedKey;
import org.bukkit.World;
import org.bukkit.entity.ArmorStand;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Villager;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import sakura.kooi.BridgingAnalyzer.BridgingAnalyzer;
import sakura.kooi.BridgingAnalyzer.utils.NoAIUtils;

/** Creates and identifies the disposable villagers used as practice targets. */
public final class PracticeTargetService {
    static final String TARGET_NAME = "靶子";
    private static final String SPAWN_POINT_NAME = "VillagerSpawnPoint";
    private static final double LEGACY_MAX_HEALTH = 1.0;
    private static final double HEALTH_EPSILON = 1.0E-6;
    private static final double SPAWN_MATCH_DISTANCE_SQUARED = 1.0;

    private final BridgingAnalyzer plugin;
    private final NamespacedKey targetKey;

    public PracticeTargetService(BridgingAnalyzer plugin) {
        this.plugin = plugin;
        this.targetKey = new NamespacedKey(plugin, "practice_target");
    }

    /** Rebuild all loaded practice targets from the existing armor-stand spawn points. */
    public void respawnAll() {
        World world = practiceWorld();
        if (world == null) {
            return;
        }

        for (Entity entity : world.getEntities()) {
            if (isPracticeTarget(entity)) {
                entity.remove();
            }
        }
        for (ArmorStand stand : spawnPoints(world)) {
            spawnTarget(stand);
        }
    }

    /**
     * Periodic maintenance only fills missing targets and removes duplicates/orphans.
     * Keeping healthy targets avoids replacing a just-killed entity with a new UUID in the
     * same tick, which previously made a successful hit occasionally look ineffective.
     */
    public void reconcileAll() {
        World world = practiceWorld();
        if (world == null) {
            return;
        }

        List<Villager> targets = new ArrayList<>();
        for (Villager villager : world.getEntitiesByClass(Villager.class)) {
            if (!villager.isDead() && villager.isValid() && isPracticeTarget(villager)) {
                targets.add(villager);
            }
        }

        Set<UUID> retainedTargets = new HashSet<>();
        for (ArmorStand stand : spawnPoints(world)) {
            Location expected = targetLocation(stand);
            Villager existing = nearestUnclaimedTarget(targets, retainedTargets, expected);
            if (existing == null) {
                spawnTarget(stand);
            } else {
                retainedTargets.add(existing.getUniqueId());
            }
        }
        for (Villager target : targets) {
            if (!retainedTargets.contains(target.getUniqueId())) {
                target.remove();
            }
        }
    }

    private World practiceWorld() {
        World world = plugin.getServer().getWorld("world");
        if (world == null) {
            plugin.getLogger().warning("无法刷新练习靶子: world 世界不存在");
            return null;
        }
        return world;
    }

    private List<ArmorStand> spawnPoints(World world) {
        List<ArmorStand> spawnPoints = new ArrayList<>();
        for (ArmorStand stand : world.getEntitiesByClass(ArmorStand.class)) {
            String customName = stand.getCustomName();
            if (customName != null && customName.contains(SPAWN_POINT_NAME)) {
                spawnPoints.add(stand);
            }
        }
        return spawnPoints;
    }

    private Villager nearestUnclaimedTarget(List<Villager> targets, Set<UUID> retained,
                                             Location expected) {
        Villager nearest = null;
        double nearestDistance = SPAWN_MATCH_DISTANCE_SQUARED;
        for (Villager target : targets) {
            if (retained.contains(target.getUniqueId())) {
                continue;
            }
            double distance = target.getLocation().distanceSquared(expected);
            if (distance <= nearestDistance) {
                nearest = target;
                nearestDistance = distance;
            }
        }
        return nearest;
    }

    private void spawnTarget(ArmorStand stand) {
        Villager target = (Villager) stand.getWorld().spawnEntity(
                targetLocation(stand), EntityType.VILLAGER);
        configureTarget(target);
    }

    private Location targetLocation(ArmorStand stand) {
        return stand.getLocation().add(0.0, 1.0, 0.0);
    }

    /**
     * New targets use a PDC marker. The strict legacy fingerprint keeps already-loaded
     * 1-health targets compatible without treating an ordinary renamed villager as a target.
     */
    public boolean isPracticeTarget(Entity entity) {
        if (!(entity instanceof Villager villager)) {
            return false;
        }
        Byte marker = villager.getPersistentDataContainer()
                .get(targetKey, PersistentDataType.BYTE);
        boolean tagged = marker != null && marker == (byte) 1;
        boolean target = matchesIdentity(true, tagged, villager.getCustomName(),
                villager.hasAI(), villager.getMaxHealth(),
                villager.getProfession() == Villager.Profession.LIBRARIAN);
        if (target && !tagged) {
            villager.getPersistentDataContainer().set(targetKey, PersistentDataType.BYTE, (byte) 1);
        }
        return target;
    }

    static boolean matchesIdentity(boolean villagerType, boolean tagged, String customName,
                                   boolean hasAi, double maxHealth, boolean librarian) {
        if (!villagerType) {
            return false;
        }
        if (tagged) {
            return true;
        }
        return TARGET_NAME.equals(customName)
                && !hasAi
                && Math.abs(maxHealth - LEGACY_MAX_HEALTH) <= HEALTH_EPSILON
                && librarian;
    }

    private void configureTarget(Villager target) {
        target.getPersistentDataContainer().set(targetKey, PersistentDataType.BYTE, (byte) 1);
        target.addPotionEffect(new PotionEffect(
                PotionEffectType.SLOWNESS, 32766, 254, false, false), true);
        target.setProfession(Villager.Profession.LIBRARIAN);
        target.setMaxHealth(LEGACY_MAX_HEALTH);
        target.setHealth(LEGACY_MAX_HEALTH);
        target.setCustomName(TARGET_NAME);
        target.setCustomNameVisible(false);
        NoAIUtils.setAI(target, false);
    }
}
