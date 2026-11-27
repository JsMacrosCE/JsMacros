package com.jsmacrosce;

import com.jsmacrosce.jsmacros.core.event.BaseEvent;
import com.jsmacrosce.jsmacros.core.event.Event;
import com.jsmacrosce.jsmacros.core.event.impl.EventCustom;
import com.jsmacrosce.jsmacros.core.language.EventContainer;

import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicBoolean;

/** Checks that only joined, still-locked deliveries may cancel or change an event. */
public final class EventDeliveryTest {
    private static int checks;

    public static void main(String[] args) throws Exception {
        unjoinedDeliveryIsReadOnly();
        joinedDeliveryCanCancel();
        releasedDeliveryIsReadOnly();
        missingContainerIsReadOnly();
        nestedDeliveriesRestoreOuter();
        containerCleanupRemovesMatchingDelivery();
        containerCleanupRemovesNewestMatchingDelivery();
        nestedDispatchesKeepOuterWritable();
        creatorOwnedEventStaysWritable();
        releasedMarkerDoesNotHideCreatorAccess();
        customEventMapEntryUpdatesValue();
        concurrentDispatchesKeepTheirOwnState();
        finishedDispatchIsReadOnly();
        nonCancellableStillRejectsCancel();
        containerJoinFlagRoundTrips();
        System.out.println("EventDelivery: " + checks + " checks passed");
    }

    private static void unjoinedDeliveryIsReadOnly() {
        CancellableEvent event = new CancellableEvent();
        event.beginDispatch(Thread.currentThread());
        event.beginDelivery(lockedContainer(), false);
        check(!event.canModify(), "an unjoined delivery cannot modify the event");
        expectThrows(IllegalStateException.class, event::cancel, "an unjoined delivery cannot cancel");
        expectThrows(IllegalStateException.class, () -> event.setValue("changed"), "an unjoined delivery cannot change a checked field");
        check(!event.isCanceled(), "a rejected cancel leaves the event uncancelled");
        check(event.getValue() == null, "a rejected field change leaves the value unchanged");
        event.endDelivery();
        event.endDispatch();
    }

    private static void joinedDeliveryCanCancel() {
        CancellableEvent event = new CancellableEvent();
        event.beginDispatch(Thread.currentThread());
        event.beginDelivery(lockedContainer(), true);
        check(event.canModify(), "a joined, locked delivery can modify the event");
        event.cancel();
        check(event.isCanceled(), "a joined delivery can cancel");
        event.setValue("changed");
        check("changed".equals(event.getValue()), "a joined delivery can change a checked field");
        event.endDelivery();
        event.endDispatch();
    }

    private static void releasedDeliveryIsReadOnly() {
        CancellableEvent event = new CancellableEvent();
        event.beginDispatch(Thread.currentThread());
        event.endDispatch();
        event.beginDelivery(unlockedContainer(), true);
        check(!event.canModify(), "a joined delivery cannot modify after its lock is released");
        expectThrows(IllegalStateException.class, event::cancel, "a released joined delivery cannot cancel");
        event.endDelivery();
    }

    private static void missingContainerIsReadOnly() {
        CancellableEvent event = new CancellableEvent();
        event.beginDispatch(Thread.currentThread());
        event.beginDelivery(null, true);
        check(!event.canModify(), "a joined delivery without a container cannot modify");
        event.endDelivery();
        event.endDispatch();
    }

    private static void nestedDeliveriesRestoreOuter() {
        CancellableEvent event = new CancellableEvent();
        event.beginDispatch(Thread.currentThread());
        event.beginDelivery(lockedContainer(), false);
        event.beginDelivery(lockedContainer(), true);
        check(event.canModify(), "the innermost joined delivery can modify");
        event.endDelivery();
        check(!event.canModify(), "ending a nested delivery restores the outer read-only delivery");
        event.endDelivery();
        event.endDispatch();
    }

    private static void containerCleanupRemovesMatchingDelivery() {
        CancellableEvent event = new CancellableEvent();
        EventContainer<?> outer = lockedContainer();
        EventContainer<?> inner = lockedContainer();
        event.beginDispatch(Thread.currentThread());
        event.beginDelivery(outer, false);
        event.beginDelivery(inner, true);
        event.endDelivery(outer);
        check(event.canModify(), "removing an outer container marker leaves the inner joined delivery writable");
        event.endDelivery();
        event.endDispatch();
    }

    private static void containerCleanupRemovesNewestMatchingDelivery() {
        CancellableEvent event = new CancellableEvent();
        EventContainer<?> shared = lockedContainer();
        event.beginDispatch(Thread.currentThread());
        event.beginDelivery(shared, false);
        event.beginDelivery(shared, true);
        event.endDelivery(shared);
        check(!event.canModify(), "removing a container marker removes the newest matching marker");
        event.endDelivery();
        event.endDispatch();
    }

    private static void nestedDispatchesKeepOuterWritable() {
        CancellableEvent event = new CancellableEvent();
        event.beginDispatch(Thread.currentThread());
        event.beginDispatch(Thread.currentThread());
        event.endDispatch();
        check(event.canModify(), "the outer same-event dispatch stays writable after the inner ends");
        event.endDispatch();
        check(!event.canModify(), "the event is read-only after the outermost dispatch ends");
    }

    private static void creatorOwnedEventStaysWritable() {
        ReusableEvent event = new ReusableEvent();
        event.beginDispatch(Thread.currentThread());
        event.endDispatch();
        check(event.canModify(), "a creator-owned reusable event stays writable after dispatch");
    }

    private static void releasedMarkerDoesNotHideCreatorAccess() {
        ReusableEvent event = new ReusableEvent();
        event.beginDispatch(Thread.currentThread());
        event.endDispatch();
        event.beginDelivery(unlockedContainer(), true);
        check(event.canModify(), "a released marker left by another thread does not hide creator access");
        event.endDelivery();
    }

    private static void customEventMapEntryUpdatesValue() {
        EventCustom event = new EventCustom(null, "DeliveryTestCustomMap");
        event.beginDispatch(Thread.currentThread());
        event.endDispatch();
        Map<String, Object> args = event.getUnderlyingMap();
        args.put("key", "first");
        Map.Entry<String, Object> entry = args.entrySet().iterator().next();
        check("first".equals(entry.setValue("second")), "entry.setValue returns the previous value");
        check("second".equals(entry.getValue()), "entry.setValue updates the entry value");
        check("second".equals(args.get("key")), "entry.setValue updates the map");
    }

    private static void concurrentDispatchesKeepTheirOwnState() throws InterruptedException {
        CancellableEvent event = new CancellableEvent();
        CountDownLatch started = new CountDownLatch(2);
        CountDownLatch release = new CountDownLatch(1);
        AtomicBoolean firstWritable = new AtomicBoolean();
        AtomicBoolean secondWritable = new AtomicBoolean();
        Thread first = new Thread(() -> {
            event.beginDispatch(Thread.currentThread());
            started.countDown();
            await(release);
            firstWritable.set(event.canModify());
            event.endDispatch();
        });
        Thread second = new Thread(() -> {
            event.beginDispatch(Thread.currentThread());
            started.countDown();
            await(release);
            secondWritable.set(event.canModify());
            event.endDispatch();
        });
        first.start();
        second.start();
        started.await();
        release.countDown();
        first.join();
        second.join();
        check(firstWritable.get(), "the first concurrent raiser can modify during its dispatch");
        check(secondWritable.get(), "the second concurrent raiser can modify during its dispatch");
        check(!event.canModify(), "the event is read-only after concurrent dispatches end");
    }

    private static void await(CountDownLatch latch) {
        try {
            latch.await();
        } catch (InterruptedException interrupted) {
            Thread.currentThread().interrupt();
        }
    }

    private static void finishedDispatchIsReadOnly() {
        CancellableEvent event = new CancellableEvent();
        event.beginDispatch(Thread.currentThread());
        event.endDispatch();
        check(!event.canModify(), "the raising thread cannot modify after dispatch finishes");
        expectThrows(IllegalStateException.class, event::cancel, "the raising thread cannot cancel after dispatch finishes");
    }

    private static void nonCancellableStillRejectsCancel() {
        PlainEvent event = new PlainEvent();
        event.beginDispatch(Thread.currentThread());
        event.beginDelivery(lockedContainer(), true);
        expectThrows(UnsupportedOperationException.class, event::cancel, "a non-cancellable event still rejects cancel");
        event.endDelivery();
        event.endDispatch();
    }

    private static void containerJoinFlagRoundTrips() {
        EventContainer<?> container = lockedContainer();
        check(!container.isJoined(), "a new container is not joined");
        container.setJoined(true);
        check(container.isJoined(), "setJoined updates the container flag");
        check(container.isLocked(), "the test container remains locked");
    }

    private static EventContainer<?> lockedContainer() {
        return new EventContainer<>(null);
    }

    private static EventContainer<?> unlockedContainer() {
        return new EventContainer<>(null) {
            @Override
            public synchronized boolean isLocked() {
                return false;
            }
        };
    }

    private static void expectThrows(Class<? extends Throwable> expected, Runnable action, String description) {
        try {
            action.run();
        } catch (Throwable failure) {
            if (expected.isInstance(failure)) {
                checks++;
                return;
            }
            throw new AssertionError(description + ": expected " + expected.getSimpleName() + " but got " + failure, failure);
        }
        throw new AssertionError(description + ": expected " + expected.getSimpleName() + " but nothing was thrown");
    }

    private static void check(boolean condition, String description) {
        if (!condition) {
            throw new AssertionError(description);
        }
        checks++;
    }

    @Event(value = "DeliveryTestCancellable", cancellable = true)
    private static final class CancellableEvent extends BaseEvent {
        private String value;

        private CancellableEvent() {
            super(null);
        }

        private String getValue() {
            return value;
        }

        private void setValue(String value) {
            assertCanModify("setValue");
            this.value = value;
        }
    }

    @Event("DeliveryTestPlain")
    private static final class PlainEvent extends BaseEvent {
        private PlainEvent() {
            super(null);
        }
    }

    @Event("DeliveryTestReusable")
    private static final class ReusableEvent extends BaseEvent {
        private ReusableEvent() {
            super(null);
        }

        @Override
        protected boolean canCreatorModifyAfterDispatch() {
            return true;
        }
    }
}
