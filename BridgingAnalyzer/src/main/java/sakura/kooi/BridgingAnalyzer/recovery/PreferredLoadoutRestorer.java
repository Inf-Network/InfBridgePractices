package sakura.kooi.BridgingAnalyzer.recovery;

import java.util.function.BooleanSupplier;
import java.util.function.Consumer;

/** Applies exactly one loadout: checkpoint chest when present, otherwise the default kit. */
final class PreferredLoadoutRestorer {
    private PreferredLoadoutRestorer() {
    }

    static void restore(BooleanSupplier checkpointLoadout, Runnable defaultLoadout,
                        Consumer<RuntimeException> checkpointFailure) {
        boolean restoredFromCheckpoint = false;
        try {
            restoredFromCheckpoint = checkpointLoadout.getAsBoolean();
        } catch (RuntimeException ex) {
            checkpointFailure.accept(ex);
        }
        if (!restoredFromCheckpoint) {
            defaultLoadout.run();
        }
    }
}
