package com.jsmacrosce.jsmacros.core.event.impl;

import com.jsmacrosce.doclet.DocletCategory;
import com.jsmacrosce.jsmacros.core.event.BaseEvent;
import com.jsmacrosce.jsmacros.core.event.EventFilterer;

import java.util.LinkedList;
import java.util.List;

/**
 * Combines filterers as an OR of AND groups. {@link #and(EventFilterer)} adds to the last group;
 * {@link #or(EventFilterer)} starts a new group. {@link #test(BaseEvent)} evaluates groups in
 * order and stops at the first passing group; within each group, it stops at the first failing
 * filterer.
 *
 * @author aMelonRind
 * @since 1.9.1
 */
@DocletCategory("Event Filterers")
public class FiltererComposed implements EventFilterer.Compound {
    private final LinkedList<List<EventFilterer>> components = new LinkedList<>();

    /**
     * Initializes the first OR group with the given filterer.
     * @param initial first filterer; {@code null} throws {@link IllegalArgumentException}
     */
    public FiltererComposed(EventFilterer initial) {
        or(initial);
    }

    /**
     * Reports whether every component supports the event name, including components in alternative OR groups.
     * @param event event name to check
     * @return {@code true} if every component supports the name; {@code false} at the first unsupported component
     */
    @Override
    public boolean canFilter(String event) {
        for (List<EventFilterer> c : components) {
            for (EventFilterer f : c) {
                if (!f.canFilter(event)) return false;
            }
        }
        return true;
    }

    /**
     * Tests the event against the OR of AND groups, in insertion order. Testing stops at the first passing
     * group, and each group stops at its first failing filterer; stateful filterers are evaluated only when
     * reached by this short-circuit order.
     * @param event event instance to test
     * @return {@code true} if any group passes; {@code false} if all groups fail
     */
    @Override
    public boolean test(BaseEvent event) {
        outer:
        for (List<EventFilterer> c : components) {
            for (EventFilterer f : c) {
                if (!f.test(event)) continue outer;
            }
            return true;
        }
        return false;
    }

    /**
     * Adds a condition to the current AND group.
     * @param filterer condition to add; {@code null} throws {@link IllegalArgumentException}
     * @return self for chaining
     */
    public FiltererComposed and(EventFilterer filterer) {
        if (filterer == null) throw new IllegalArgumentException("filterer cannot be null!");
        if (filterer instanceof FiltererComposed fc) fc.checkCyclicRef(this);

        components.getLast().add(filterer);
        return this;
    }

    /**
     * Starts a new OR group containing this filterer.
     * @param filterer first condition in the new group; {@code null} throws {@link IllegalArgumentException}
     * @return self for chaining
     */
    public FiltererComposed or(EventFilterer filterer) {
        if (filterer == null) throw new IllegalArgumentException("filterer cannot be null!");
        if (filterer instanceof FiltererComposed fc) fc.checkCyclicRef(this);

        List<EventFilterer> list = new LinkedList<>();
        list.add(filterer);
        components.add(list);
        return this;
    }

    /**
     * Checks {@code base} against this filterer and recursively against component filterers implementing
     * {@link Compound}. Custom component hooks determine how their own descendants are checked.
     * @param base compound filterer checked against this filterer and its compound components
     */
    public void checkCyclicRef(Compound base) {
        Compound.super.checkCyclicRef(base);
        for (List<EventFilterer> c : components) {
            for (EventFilterer f : c) {
                if (f instanceof Compound fc) fc.checkCyclicRef(base);
            }
        }
    }

}
