package sakura.kooi.BridgingSkin.lottery;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Random;
import java.util.Set;
import java.util.logging.Logger;
import org.bukkit.Material;

public final class PrizePool {
    private final List<Material> materials;

    public PrizePool(List<String> configuredMaterials, Logger logger) {
        LinkedHashSet<Material> accepted = new LinkedHashSet<>();
        for (String configured : configuredMaterials) {
            Material material = Material.matchMaterial(configured.toUpperCase(Locale.ROOT));
            if (!FullBlockPrizeCatalog.isEligible(material)) {
                logger.warning("BridgingSkin 已忽略非完整或冲突的奖池方块: " + configured);
                continue;
            }
            accepted.add(material);
        }
        if (accepted.isEmpty()) {
            accepted.addAll(FullBlockPrizeCatalog.defaults());
            logger.warning("BridgingSkin 奖池配置为空或无效，已使用内置完整方块奖池");
        }
        this.materials = List.copyOf(accepted);
    }

    public List<Material> draw(int count, Set<Material> alreadyOwned, Random random) {
        if (count <= 0) {
            throw new IllegalArgumentException("抽奖次数必须为正数");
        }
        List<Material> unowned = new ArrayList<>();
        for (Material material : materials) {
            if (!alreadyOwned.contains(material)) {
                unowned.add(material);
            }
        }
        if (unowned.size() < count) {
            return List.of();
        }
        Collections.shuffle(unowned, random);
        return List.copyOf(unowned.subList(0, count));
    }

    public Material preview(Random random) {
        return materials.get(random.nextInt(materials.size()));
    }

    public List<Material> materials() {
        return materials;
    }

    public int remaining(Set<Material> alreadyOwned) {
        int remaining = 0;
        for (Material material : materials) {
            if (!alreadyOwned.contains(material)) {
                remaining++;
            }
        }
        return remaining;
    }
}
