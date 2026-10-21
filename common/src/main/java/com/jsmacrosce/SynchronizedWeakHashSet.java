package com.jsmacrosce;

import com.google.common.collect.ImmutableSet;

import java.io.Serializable;
import java.util.AbstractSet;
import java.util.Iterator;
import java.util.Map;
import java.util.WeakHashMap;

/**
 * A set backed by weak keys; each of its own operations is synchronized, but this does not make
 * inherited or compound operations atomic. Iterators are detached snapshots that retain strong
 * references to their elements. A null key is accepted by the set methods, but while one is
 * present, {@link #iterator()} throws {@link NullPointerException}.
 * <pre>
 * const Set = Java.type("com.jsmacrosce.SynchronizedWeakHashSet");
 * const values = new Set();
 * const JavaObject = Java.type("java.lang.Object");
 * const item = new JavaObject();
 * values.add(item);
 * const snapshot = values.iterator();
 * values.clear();
 * snapshot.hasNext(); // still true; the snapshot retains item
 * </pre>
 * @param <E> the element type
 */
public class SynchronizedWeakHashSet<E> extends AbstractSet<E> implements Serializable {

    private final Map<E, Boolean> map;

    /** Initializes an empty set backed by a weak-key map. */
    public SynchronizedWeakHashSet() {
        map = new WeakHashMap<>();
    }

    /**
     * Returns the current number of entries; weakly reachable keys may disappear.
     * @return the current entry count
     */
    @Override
    public synchronized int size() {
        return map.size();
    }

    /**
     * Returns whether an equal key is present. A null key can be queried.
     * @param o the key to test
     * @return {@code true} if an equal key is present
     */
    @Override
    public synchronized boolean contains(Object o) {
        return map.containsKey(o);
    }

    /**
     * Adds a key if absent; keys are held weakly.
     * @param o the element to add
     * @return {@code true} if the element was previously absent
     */
    @Override
    public synchronized boolean add(E o) {
        return map.putIfAbsent(o, Boolean.TRUE) == null;
    }

    /**
     * Removes a matching key if present.
     * @param o the key to remove
     * @return {@code true} if an entry existed
     */
    @Override
    public synchronized boolean remove(Object o) {
        return map.remove(o) != null;
    }

    /** Removes all entries. */
    @Override
    public synchronized void clear() {
        map.clear();
    }

    /**
     * Returns a detached snapshot of the current keys; its iterator does not support removal.
     * A present null key causes snapshot creation to throw {@link NullPointerException}.
     * @return an iterator over the snapshot, which strongly retains its elements
     */
    @Override
    public synchronized Iterator<E> iterator() {
        return ImmutableSet.copyOf(map.keySet()).iterator();
    }

}
