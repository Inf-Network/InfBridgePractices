package net.infnetwork.snowball.bridgingskin.lottery;

import java.util.Arrays;
import java.util.List;
import java.util.Random;
import org.bukkit.Material;

final class LotteryAnimationState {
    static final int ROW_WIDTH = 9;

    private static final List<Material> GLASS_PALETTE = List.of(
            Material.RED_STAINED_GLASS_PANE,
            Material.ORANGE_STAINED_GLASS_PANE,
            Material.YELLOW_STAINED_GLASS_PANE,
            Material.LIME_STAINED_GLASS_PANE,
            Material.GREEN_STAINED_GLASS_PANE,
            Material.CYAN_STAINED_GLASS_PANE,
            Material.LIGHT_BLUE_STAINED_GLASS_PANE,
            Material.BLUE_STAINED_GLASS_PANE,
            Material.PURPLE_STAINED_GLASS_PANE,
            Material.MAGENTA_STAINED_GLASS_PANE,
            Material.PINK_STAINED_GLASS_PANE,
            Material.WHITE_STAINED_GLASS_PANE);

    private final PrizePool prizePool;
    private final Random random;
    private final Material[] previews = new Material[ROW_WIDTH];
    private int bandOffset;

    LotteryAnimationState(PrizePool prizePool, Random random) {
        this.prizePool = prizePool;
        this.random = random;
        for (int column = 0; column < ROW_WIDTH; column++) {
            previews[column] = prizePool.preview(random);
        }
    }

    void advance() {
        System.arraycopy(previews, 1, previews, 0, ROW_WIDTH - 1);
        previews[ROW_WIDTH - 1] = prizePool.preview(random);
        bandOffset = Math.floorMod(bandOffset + 1, GLASS_PALETTE.size());
    }

    Material top(int column) {
        checkColumn(column);
        return GLASS_PALETTE.get(Math.floorMod(column + bandOffset, GLASS_PALETTE.size()));
    }

    Material preview(int column) {
        checkColumn(column);
        return previews[column];
    }

    Material bottom(int column) {
        checkColumn(column);
        return GLASS_PALETTE.get(Math.floorMod(column - bandOffset, GLASS_PALETTE.size()));
    }

    List<Material> previewSnapshot() {
        return List.copyOf(Arrays.asList(previews.clone()));
    }

    private static void checkColumn(int column) {
        if (column < 0 || column >= ROW_WIDTH) {
            throw new IndexOutOfBoundsException("动画列超出范围: " + column);
        }
    }
}
