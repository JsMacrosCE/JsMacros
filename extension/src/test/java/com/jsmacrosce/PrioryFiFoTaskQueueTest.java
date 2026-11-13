package com.jsmacrosce;

import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

/**
 * Checks the queue contract the script scheduler relies on: additions do not preempt the current
 * head, and after it is removed the lowest integer priority value runs first while entries sharing
 * a priority keep their insertion order.
 */
class PrioryFiFoTaskQueueTest {

    @Test
    void pollsByPriorityThenInsertionOrder() {
        PrioryFiFoTaskQueue<String> queue = new PrioryFiFoTaskQueue<>(entry -> Integer.parseInt(entry.substring(1, 2)));
        // The first entry becomes the current head; later additions wait for their turn.
        queue.add("p3main");
        queue.add("p1a");
        queue.add("p2");
        queue.add("p1b");
        queue.add("p0");

        assertEquals("p3main", queue.peek());
        List<String> order = new ArrayList<>();
        String next;
        while ((next = queue.poll()) != null) {
            order.add(next);
        }

        assertEquals(List.of("p3main", "p0", "p1a", "p1b", "p2"), order);
        assertNull(queue.peek());
    }
}
