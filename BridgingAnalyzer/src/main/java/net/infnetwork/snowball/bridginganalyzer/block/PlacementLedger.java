package net.infnetwork.snowball.bridginganalyzer.block;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;

/**
 * Tracks ownership of replaceable placements without depending on Bukkit types.
 *
 * <p>Every call to {@link #track(Object, Object, Object)} creates a new generation.
 * A token from an older generation cannot mutate or remove the newer placement,
 * even when both generations use an equal key and owner. All public operations are
 * synchronized so transferring a key between owners updates both indexes atomically.</p>
 *
 * @param <K> placement key type
 * @param <O> owner type
 * @param <S> expected state type
 */
public final class PlacementLedger<K, O, S> {
    private final Map<K, StoredEntry<O, S>> entriesByKey = new LinkedHashMap<>();
    private final Map<O, Set<K>> keysByOwner = new LinkedHashMap<>();
    private long nextGeneration = 1L;

    /**
     * Tracks a new generation for {@code key}, atomically transferring it from a
     * previous owner when necessary.
     */
    public synchronized Token<K, O> track(K key, O owner, S expectedState) {
        Objects.requireNonNull(key, "key");
        Objects.requireNonNull(owner, "owner");
        Objects.requireNonNull(expectedState, "expectedState");

        StoredEntry<O, S> previous = entriesByKey.get(key);
        if (previous != null && !previous.owner.equals(owner)) {
            removeOwnerKey(previous.owner, key);
        }

        long generation = nextGeneration;
        nextGeneration = Math.incrementExact(nextGeneration);
        entriesByKey.put(key, new StoredEntry<>(owner, generation, expectedState));
        keysByOwner.computeIfAbsent(owner, ignored -> new LinkedHashSet<>()).add(key);
        return new Token<>(key, owner, generation);
    }

    /** Returns an immutable snapshot of the current entry for {@code key}. */
    public synchronized Optional<Entry<K, O, S>> current(K key) {
        Objects.requireNonNull(key, "key");
        StoredEntry<O, S> stored = entriesByKey.get(key);
        return stored == null ? Optional.empty() : Optional.of(snapshot(key, stored));
    }

    /** Returns an immutable snapshot of every current entry owned by {@code owner}. */
    public synchronized List<Entry<K, O, S>> snapshot(O owner) {
        Objects.requireNonNull(owner, "owner");
        Set<K> keys = keysByOwner.get(owner);
        if (keys == null) {
            return List.of();
        }

        List<Entry<K, O, S>> result = new ArrayList<>(keys.size());
        for (K key : keys) {
            StoredEntry<O, S> stored = entriesByKey.get(key);
            if (stored != null && stored.owner.equals(owner)) {
                result.add(snapshot(key, stored));
            }
        }
        return List.copyOf(result);
    }

    /** Returns an immutable snapshot of every current entry in the ledger. */
    public synchronized List<Entry<K, O, S>> all() {
        List<Entry<K, O, S>> result = new ArrayList<>(entriesByKey.size());
        entriesByKey.forEach((key, stored) -> result.add(snapshot(key, stored)));
        return List.copyOf(result);
    }

    /** Returns whether {@code token} still identifies the current key generation. */
    public synchronized boolean isCurrent(Token<K, O> token) {
        Objects.requireNonNull(token, "token");
        StoredEntry<O, S> stored = entriesByKey.get(token.key());
        return stored != null
                && stored.generation == token.generation()
                && stored.owner.equals(token.owner());
    }

    /**
     * Updates the expected state only when {@code token} is current.
     *
     * @return the updated immutable entry, or an empty optional for a stale token
     */
    public synchronized Optional<Entry<K, O, S>> updateExpected(
            Token<K, O> token, S expectedState) {
        Objects.requireNonNull(token, "token");
        Objects.requireNonNull(expectedState, "expectedState");
        if (!isCurrent(token)) {
            return Optional.empty();
        }

        StoredEntry<O, S> updated = new StoredEntry<>(
                token.owner(), token.generation(), expectedState);
        entriesByKey.put(token.key(), updated);
        return Optional.of(snapshot(token.key(), updated));
    }

    /**
     * Removes the entry only when {@code token} is current.
     *
     * @return the removed immutable entry, or an empty optional for a stale token
     */
    public synchronized Optional<Entry<K, O, S>> removeIfCurrent(Token<K, O> token) {
        Objects.requireNonNull(token, "token");
        if (!isCurrent(token)) {
            return Optional.empty();
        }
        return forget(token.key());
    }

    /** Removes the current generation for {@code key}, regardless of its token. */
    public synchronized Optional<Entry<K, O, S>> forget(K key) {
        Objects.requireNonNull(key, "key");
        StoredEntry<O, S> removed = entriesByKey.remove(key);
        if (removed == null) {
            return Optional.empty();
        }
        removeOwnerKey(removed.owner, key);
        return Optional.of(snapshot(key, removed));
    }

    public synchronized int size() {
        return entriesByKey.size();
    }

    /** Exposed for lifecycle checks; owners with no keys are removed immediately. */
    public synchronized int ownerCount() {
        return keysByOwner.size();
    }

    public synchronized boolean isEmpty() {
        return entriesByKey.isEmpty();
    }

    public synchronized void clear() {
        entriesByKey.clear();
        keysByOwner.clear();
    }

    private void removeOwnerKey(O owner, K key) {
        Set<K> keys = keysByOwner.get(owner);
        if (keys == null) {
            return;
        }
        keys.remove(key);
        if (keys.isEmpty()) {
            keysByOwner.remove(owner);
        }
    }

    private Entry<K, O, S> snapshot(K key, StoredEntry<O, S> stored) {
        return new Entry<>(key, stored.owner, stored.generation, stored.expectedState);
    }

    /** Identifies one ownership generation for a key. */
    public record Token<K, O>(K key, O owner, long generation) {
        public Token {
            Objects.requireNonNull(key, "key");
            Objects.requireNonNull(owner, "owner");
            if (generation <= 0L) {
                throw new IllegalArgumentException("generation must be positive");
            }
        }
    }

    /** Immutable view of one tracked placement. */
    public record Entry<K, O, S>(K key, O owner, long generation, S expectedState) {
        public Entry {
            Objects.requireNonNull(key, "key");
            Objects.requireNonNull(owner, "owner");
            Objects.requireNonNull(expectedState, "expectedState");
            if (generation <= 0L) {
                throw new IllegalArgumentException("generation must be positive");
            }
        }

        public Token<K, O> token() {
            return new Token<>(key, owner, generation);
        }
    }

    private record StoredEntry<O, S>(O owner, long generation, S expectedState) {
    }
}
