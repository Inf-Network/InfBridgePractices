package net.infnetwork.snowball.bridginganalyzer.block;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import org.junit.jupiter.api.Test;

class PlacementLedgerTest {
    @Test
    void tracksExpectedStateAndReturnsOwnerAndGlobalSnapshots() {
        PlacementLedger<String, String, String> ledger = new PlacementLedger<>();

        PlacementLedger.Token<String, String> first = ledger.track("0,64,0", "alice", "sandstone");
        PlacementLedger.Token<String, String> second = ledger.track("1,64,0", "bob", "wool");

        assertEquals(
                new PlacementLedger.Entry<>("0,64,0", "alice", first.generation(), "sandstone"),
                ledger.current("0,64,0").orElseThrow());
        assertEquals(List.of(ledger.current("0,64,0").orElseThrow()), ledger.snapshot("alice"));
        assertEquals(
                List.of(
                        ledger.current("0,64,0").orElseThrow(),
                        ledger.current("1,64,0").orElseThrow()),
                ledger.all());
        assertTrue(ledger.isCurrent(first));
        assertTrue(ledger.isCurrent(second));
    }

    @Test
    void retrackingKeyAtomicallyTransfersOwnershipAndInvalidatesOldGeneration() {
        PlacementLedger<String, String, String> ledger = new PlacementLedger<>();
        PlacementLedger.Token<String, String> alice = ledger.track("same", "alice", "sandstone");

        PlacementLedger.Token<String, String> bob = ledger.track("same", "bob", "stone");

        assertFalse(ledger.isCurrent(alice));
        assertTrue(ledger.isCurrent(bob));
        assertTrue(ledger.snapshot("alice").isEmpty());
        assertEquals(List.of(ledger.current("same").orElseThrow()), ledger.snapshot("bob"));
        assertEquals(1, ledger.size());
        assertEquals(1, ledger.ownerCount());
    }

    @Test
    void retrackingBySameOwnerStillCreatesANewGeneration() {
        PlacementLedger<String, String, String> ledger = new PlacementLedger<>();
        PlacementLedger.Token<String, String> old = ledger.track("same", "alice", "sandstone");

        PlacementLedger.Token<String, String> current = ledger.track("same", "alice", "stone");

        assertFalse(ledger.isCurrent(old));
        assertTrue(ledger.isCurrent(current));
        assertEquals("stone", ledger.current("same").orElseThrow().expectedState());
        assertEquals(1, ledger.snapshot("alice").size());
    }

    @Test
    void staleTokenCannotUpdateExpectedState() {
        PlacementLedger<String, String, String> ledger = new PlacementLedger<>();
        PlacementLedger.Token<String, String> stale = ledger.track("same", "alice", "sandstone");
        PlacementLedger.Token<String, String> current = ledger.track("same", "alice", "stone");

        assertTrue(ledger.updateExpected(stale, "wool").isEmpty());
        PlacementLedger.Entry<String, String, String> updated =
                ledger.updateExpected(current, "glass").orElseThrow();

        assertEquals("glass", updated.expectedState());
        assertEquals("glass", ledger.current("same").orElseThrow().expectedState());
        assertTrue(ledger.isCurrent(current));
    }

    @Test
    void naturalMaterialTransitionsStayInTheSameCleanupGeneration() {
        PlacementLedger<String, String, String> ledger = new PlacementLedger<>();
        PlacementLedger.Token<String, String> copper =
                ledger.track("bridge", "alice", "copper_block");

        PlacementLedger.Entry<String, String, String> exposed =
                ledger.updateExpected(copper, "exposed_copper").orElseThrow();
        PlacementLedger.Entry<String, String, String> weathered =
                ledger.updateExpected(exposed.token(), "weathered_copper").orElseThrow();

        assertEquals(copper.generation(), weathered.generation());
        assertEquals("weathered_copper", ledger.current("bridge").orElseThrow().expectedState());
        assertEquals(weathered, ledger.removeIfCurrent(weathered.token()).orElseThrow());
        assertTrue(ledger.isEmpty());
    }

    @Test
    void staleTokenCannotRemoveNewGeneration() {
        PlacementLedger<String, String, String> ledger = new PlacementLedger<>();
        PlacementLedger.Token<String, String> stale = ledger.track("same", "alice", "sandstone");
        PlacementLedger.Token<String, String> current = ledger.track("same", "bob", "stone");

        assertTrue(ledger.removeIfCurrent(stale).isEmpty());
        assertTrue(ledger.isCurrent(current));
        assertEquals(
                ledger.current("same").orElseThrow(),
                ledger.removeIfCurrent(current).orElseThrow());
        assertTrue(ledger.isEmpty());
        assertEquals(0, ledger.ownerCount());
    }

    @Test
    void forgetReturnsRemovedEntryAndCleansEmptyOwner() {
        PlacementLedger<String, String, String> ledger = new PlacementLedger<>();
        PlacementLedger.Token<String, String> token = ledger.track("key", "alice", "sandstone");

        PlacementLedger.Entry<String, String, String> removed = ledger.forget("key").orElseThrow();

        assertEquals(token, removed.token());
        assertEquals("sandstone", removed.expectedState());
        assertTrue(ledger.current("key").isEmpty());
        assertTrue(ledger.snapshot("alice").isEmpty());
        assertEquals(0, ledger.ownerCount());
        assertTrue(ledger.forget("key").isEmpty());
    }

    @Test
    void snapshotsCannotBeMutatedAndDoNotChangeRetroactively() {
        PlacementLedger<String, String, String> ledger = new PlacementLedger<>();
        PlacementLedger.Token<String, String> token = ledger.track("key", "alice", "sandstone");
        List<PlacementLedger.Entry<String, String, String>> ownerSnapshot = ledger.snapshot("alice");
        List<PlacementLedger.Entry<String, String, String>> allSnapshot = ledger.all();

        ledger.updateExpected(token, "stone");

        assertEquals("sandstone", ownerSnapshot.getFirst().expectedState());
        assertEquals("sandstone", allSnapshot.getFirst().expectedState());
        assertThrows(UnsupportedOperationException.class, () -> ownerSnapshot.clear());
        assertThrows(UnsupportedOperationException.class, () -> allSnapshot.clear());
    }

    @Test
    void clearRemovesEntriesAndOwnerBuckets() {
        PlacementLedger<String, String, String> ledger = new PlacementLedger<>();
        ledger.track("one", "alice", "sandstone");
        ledger.track("two", "bob", "stone");

        ledger.clear();

        assertTrue(ledger.all().isEmpty());
        assertTrue(ledger.isEmpty());
        assertEquals(0, ledger.ownerCount());
    }
}
