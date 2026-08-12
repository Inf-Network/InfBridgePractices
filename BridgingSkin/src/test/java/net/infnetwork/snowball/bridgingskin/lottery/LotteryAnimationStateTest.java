package net.infnetwork.snowball.bridgingskin.lottery;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.logging.Logger;
import org.bukkit.Material;
import org.junit.jupiter.api.Test;

class LotteryAnimationStateTest {
    @Test
    void advancesPrizeRowLeftOneSlotInsteadOfReplacingTheWholeRow() {
        LotteryAnimationState state = state();
        List<Material> before = state.previewSnapshot();

        state.advance();

        for (int column = 0; column < LotteryAnimationState.ROW_WIDTH - 1; column++) {
            assertEquals(before.get(column + 1), state.preview(column));
        }
    }

    @Test
    void glassBandsMoveInOppositeDirections() {
        LotteryAnimationState state = state();
        List<Material> topBefore = row(state, true);
        List<Material> bottomBefore = row(state, false);

        state.advance();

        for (int column = 0; column < LotteryAnimationState.ROW_WIDTH - 1; column++) {
            assertEquals(topBefore.get(column + 1), state.top(column), "顶部应向左移动");
        }
        for (int column = 1; column < LotteryAnimationState.ROW_WIDTH; column++) {
            assertEquals(bottomBefore.get(column - 1), state.bottom(column), "底部应向右移动");
        }
    }

    private static LotteryAnimationState state() {
        PrizePool pool = new PrizePool(
                List.of("STONE", "GRANITE", "DIORITE", "ANDESITE"),
                Logger.getAnonymousLogger());
        return new LotteryAnimationState(pool, new Random(17));
    }

    private static List<Material> row(LotteryAnimationState state, boolean top) {
        List<Material> materials = new ArrayList<>();
        for (int column = 0; column < LotteryAnimationState.ROW_WIDTH; column++) {
            materials.add(top ? state.top(column) : state.bottom(column));
        }
        return materials;
    }
}
