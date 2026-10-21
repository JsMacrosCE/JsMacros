package com.jsmacrosce;

import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;
import org.jetbrains.annotations.NotNull;

import java.util.*;
import java.util.function.Function;

/**
 * A queue that deduplicates entries by equality and groups them into integer-priority buckets,
 * preserving insertion order within each bucket. When adding to an empty membership set, the entry
 * becomes the cached head. Removing the head through the queue's head-removal operation selects the
 * next head from the lowest-priority bucket; removing entries by value or clearing the queue does not
 * refresh the cached head. {@code size()} counts occupied priority buckets, not entries.
 * <pre>
 * const Queue = Java.type("com.jsmacrosce.PrioryFiFoTaskQueue");
 * const Integer = Java.type("java.lang.Integer");
 * const queue = new Queue(JavaWrapper.methodToJava(function(value) {
 *   return Integer.valueOf(String(value));
 * }));
 * queue.add("2");
 * queue.add("1");
 * Chat.log(queue.poll());
 * </pre>
 * @param <E> queued entry type
 */
public class PrioryFiFoTaskQueue<E> implements Queue<E> {
    volatile E currentTask;
    final Int2ObjectOpenHashMap<List<E>> tasks = new Int2ObjectOpenHashMap<>();
    final Set<E> taskSet = new HashSet<>();
    final Function<E, Integer> priorityFunction;

    /** Stores the function used to obtain each entry's stable integer priority; it is not evaluated here.
     * @param priorityFunction non-null function providing a stable integer priority per entry
     */
    public PrioryFiFoTaskQueue(Function<E, Integer> priorityFunction) {
        this.priorityFunction = priorityFunction;
    }

    /** Returns the number of priority buckets containing entries, not the number of entries.
     * @return occupied priority bucket count
     */
    @Override
    public synchronized int size() {
        return tasks.size();
    }

    /** Returns whether the priority-bucket map is empty.
     * @return whether the priority-bucket map is empty
     */
    @Override
    public synchronized boolean isEmpty() {
        return tasks.isEmpty();
    }

    /** Checks whether the membership set contains the candidate entry.
     * @param o candidate entry
     * @return whether the entry is present
     */
    @Override
    public synchronized boolean contains(Object o) {
        return taskSet.contains(o);
    }

    /** Returns a live iterator over the membership set; iteration order is unspecified and is not priority order.
     * Removing through the iterator changes only the membership set, not the priority buckets or cached head.
     * @return live membership-set iterator
     */
    @NotNull
    @Override
    public synchronized Iterator<E> iterator() {
        return taskSet.iterator();
    }

    /** Returns an array of membership-set entries in unspecified order.
     * @return membership entries
     */
    @NotNull
    @Override
    public synchronized Object[] toArray() {
        return taskSet.toArray();
    }

    /** Copies membership-set entries into an array, using the supplied array when it is large enough.
     * @param <T> destination array component type
     * @param ts destination array
     * @return membership entries in the supplied array when large enough, or a new array otherwise
     */
    @NotNull
    @Override
    public synchronized <T> T[] toArray(@NotNull T[] ts) {
        return taskSet.toArray(ts);
    }

    /** Adds an entry if no equal entry is already present. The priority function is evaluated for a new entry;
     * adding a lower-priority entry does not replace the cached head while the membership set is nonempty.
     * @param e non-null entry with a stable priority
     * @return {@code true} if added; {@code false} if already present
     */
    @Override
    public synchronized boolean add(E e) {
        boolean wasEmpty = taskSet.isEmpty();
        if (taskSet.add(e)) {
            if (wasEmpty) {
                currentTask = e;
            }
            tasks.computeIfAbsent(priorityFunction.apply(e), k -> new ArrayList<>()).add(e);
            this.notifyAll();
            return true;
        }
        return false;
    }

    /** Removes an entry and its bucket membership using its current priority-function result. This does not
     * refresh the cached head; entries therefore need stable priorities.
     * @param o entry to remove
     * @return {@code true} if present and removed; {@code false} if absent
     */
    @Override
    public synchronized boolean remove(Object o) {
        if (taskSet.remove(o)) {
            int prio = priorityFunction.apply((E) o);
            List<E> list = tasks.get(prio);
            list.remove(o);
            if (list.isEmpty()) {
                tasks.remove(prio);
            }
            return true;
        }
        return false;
    }

    /** Checks whether every supplied entry is in the membership set.
     * @param collection entries to check
     * @return whether all entries are present
     */
    @Override
    public synchronized boolean containsAll(@NotNull Collection<?> collection) {
        return taskSet.containsAll(collection);
    }

    /** Adds entries in collection iteration order; additions completed before a failure are not rolled back.
     * @param collection entries to add
     * @return whether at least one entry was added
     */
    @Override
    public synchronized boolean addAll(@NotNull Collection<? extends E> collection) {
        boolean changed = false;
        for (E e : collection) {
            changed |= add(e);
        }
        return changed;
    }

    /** Removes entries in collection iteration order through value removal; completed removals are not rolled back on failure.
     * The cached head is not refreshed.
     * @param collection entries to remove
     * @return whether any entry was removed, if the operation completes
     */
    @Override
    public synchronized boolean removeAll(@NotNull Collection<?> collection) {
        boolean changed = false;
        for (Object o : collection) {
            changed |= remove(o);
        }
        return changed;
    }

    /** Removes entries absent from the collection while iterating the live membership set; structural removals during
     * iteration can cause {@code ConcurrentModificationException} after partial removal, so complete retention is not guaranteed.
     * The cached head is not refreshed.
     * @param collection entries to keep
     * @return whether any entry was removed, if the operation completes
     */
    @Override
    public synchronized boolean retainAll(@NotNull Collection<?> collection) {
        boolean changed = false;
        for (Object o : taskSet) {
            if (!collection.contains(o)) {
                changed |= remove(o);
            }
        }
        return changed;
    }

    /** Clears the membership set and priority buckets without resetting the cached head or notifying waiters.
     */
    @Override
    public synchronized void clear() {
        taskSet.clear();
        tasks.clear();
    }

    /** Adds an entry using the queue's deduplication and priority behavior.
     * @param e non-null entry with a stable priority
     * @return {@code true} if newly added; {@code false} if an equal entry is already present
     */
    @Override
    public synchronized boolean offer(E e) {
        return add(e);
    }

    /** Removes and returns the cached head, then refreshes it from the lowest-priority bucket when non-null.
     * @return the previous cached head, or {@code null} if the cache was empty; the cached entry may be stale
     */
    @Override
    public synchronized E remove() {
        E e = currentTask;
        if (e != null) {
            remove(e);
            currentTask = getLowestPrioItem();
        }
        return e;
    }

    /** Removes and returns the cached head using the queue's head-removal behavior.
     * @return the previous cached head, or {@code null} if the cache was empty; the cached entry may be stale
     */
    @Override
    public E poll() {
        return remove();
    }

    /** Waits until the membership set is nonempty, then removes and returns the cached head. Wait interruption propagates as
     * {@code InterruptedException}.
     * @return the cached-head removal result, which may be {@code null} or stale
     */
    public synchronized E pollWaiting() throws InterruptedException {
        while (taskSet.isEmpty()) {
            this.wait();
        }
        return remove();
    }

    /** While membership is empty, tracks wall-clock elapsed wait time against the budget, then attempts to remove and return
     * the cached head even if the budget has expired or membership remains empty; this does not guarantee an exact deadline.
     * Nonpositive budgets skip waiting. Wait interruption propagates as {@code InterruptedException}.
     * @param timeout wait budget in milliseconds; nonpositive values skip waiting
     * @return the cached-head removal result, which may be {@code null} or stale
     */
    public synchronized E pollWaiting(long timeout) throws InterruptedException {
        long timeoutLeft = timeout;
        while (taskSet.isEmpty() && timeoutLeft > 0) {
            long start = System.currentTimeMillis();
            this.wait(timeoutLeft);
            timeoutLeft -= System.currentTimeMillis() - start;
        }
        return remove();
    }

    /** Waits once if membership is empty, then returns the cached head without removing it or rechecking membership.
     * Wait interruption propagates as {@code InterruptedException}.
     * @return the cached head, which may be {@code null} or stale
     */
    public synchronized E peekWaiting() throws InterruptedException {
        if (taskSet.isEmpty()) {
            this.wait();
        }
        return currentTask;
    }

    /** Waits once if membership is empty, then returns the cached head without removing it or rechecking membership.
     * Wait interruption propagates as {@code InterruptedException}.
     * @param timeout wait duration in milliseconds; zero waits indefinitely, and a negative value is invalid if waiting occurs
     * @return the cached head, which may be {@code null} or stale
     */
    public synchronized E peekWaiting(long timeout) throws InterruptedException {
        if (taskSet.isEmpty()) {
            this.wait(timeout);
        }
        return currentTask;
    }

    /** Returns the cached head without checking membership or removing it.
     * @return the cached head, or {@code null} if the cache is empty; it may be stale
     */
    @Override
    public synchronized E element() {
        return currentTask;
    }

    /** Returns the cached head without checking membership or removing it.
     * @return the cached head, or {@code null} if the cache is empty; it may be stale
     */
    @Override
    public synchronized E peek() {
        return currentTask;
    }

    private synchronized E getLowestPrioItem() {
        if (tasks.isEmpty()) {
            return null;
        }
        int lowestPrio = Integer.MAX_VALUE;
        for (int prio : tasks.keySet()) {
            if (prio < lowestPrio) {
                lowestPrio = prio;
            }
        }
        return tasks.get(lowestPrio).get(0);
    }

}
