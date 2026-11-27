package com.jsmacrosce.jsmacros.core.event;

import com.jsmacrosce.doclet.DocletCategory;
import com.jsmacrosce.doclet.DocletIgnore;
import com.jsmacrosce.jsmacros.core.Core;
import com.jsmacrosce.jsmacros.core.language.EventContainer;
import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayDeque;
import java.util.Iterator;

/**
 * Base class for event payloads dispatched to registered listeners. By default, the runtime
 * class's {@link Event} annotation supplies its name and cancellable/joinable flags; without an
 * annotation, the simple class name is used and both flags are false. Subclasses may override
 * these methods, as {@link com.jsmacrosce.jsmacros.core.event.impl.EventCustom} does.
 * <p>
 * Dispatch waits for a listener only when that listener requests joined handling and the event
 * permits joining. A joined listener may cancel or change the event while its event lock is held.
 * A listener that did not request joined handling runs after dispatch returns; the game has
 * already read the event by then, so it cannot cancel or change it. Call {@link #canModify()} to
 * check the current delivery before attempting a change.
 * <pre>
 * const listener = JsMacros.on("SendMessage", true, JavaWrapper.methodToJava(event => {
 *   if (event.getMessage() === "/hello") {
 *     // Cancel before the message is sent or added to chat history.
 *     event.cancel();
 *   }
 *   // Remove a one-shot listener using the handle returned by on().
 *   JsMacros.off(listener);
 * }));
 * </pre>
 */
@DocletCategory("Events and Event Handling")
public class BaseEvent {
    /**
     * The core through which this event is dispatched.
     */
    public final Core<?, ?> runner;
    /**
     * whether {@link #cancel()} has been called on this instance. It starts out {@code false} and
     * the game reads it back through {@link #isCanceled()} once the listeners have had their turn.
     */
    protected boolean cancelled;

    private final ThreadLocal<ArrayDeque<Delivery>> delivery = new ThreadLocal<>();
    private final ThreadLocal<ArrayDeque<Thread>> dispatches = new ThreadLocal<>();
    private final Thread creatorThread = Thread.currentThread();
    private volatile boolean hasDispatched;

    private record Delivery(@Nullable EventContainer<?> container, boolean joined) {}

    /**
     * Sets the core through which this event is dispatched.
     *
     * @param runner the core this event is dispatched through
     */
    public BaseEvent(Core<?, ?> runner) {
        this.runner = runner;
    }

    /**
     * whether this event can be cancelled, read from {@link Event#cancellable()} on the subclass.
     * It is the flag {@link #cancel()} checks, so it is also what decides whether a cancel throws
     * rather than sticking. A subclass with no {@code @Event} defaults to {@code false}.
     *
     * @return {@code true} if the event declares itself cancellable.
     */
    public boolean cancellable() {
        Event annotation = this.getClass().getAnnotation(Event.class);
        return annotation != null && annotation.cancellable();
    }

    /**
     * Whether this event permits joining based on its own cancellable or joinable flag. Named-event
     * dispatch may also permit joining based on {@link BaseEventRegistry#joinableEvents}.
     *
     * @return {@code true} if the event is cancellable or declares itself joinable.
     */
    public boolean joinable() {
        Event annotation = this.getClass().getAnnotation(Event.class);
        return cancellable() || annotation != null && annotation.joinable();
    }

    /**
     * Whether the current callback may cancel or change this event. This is {@code true} for a
     * joined listener while its event lock is still held, for the raising thread during dispatch,
     * and for a custom event's creator between dispatches. It is {@code false} for a non-joined
     * listener, after the event lock is released, and for built-in events after dispatch returns.
     *
     * @return {@code true} if the current delivery may cancel or change the event
     * @since 2.2.0
     */
    public final boolean canModify() {
        Delivery current = currentDelivery();
        if (current != null) {
            return current.joined() && current.container() != null && current.container().isLocked();
        }
        if (!hasDispatched) {
            return true;
        }
        ArrayDeque<Thread> stack = dispatches.get();
        if (stack != null && !stack.isEmpty()) {
            return true;
        }
        return creatorThread == Thread.currentThread() && canCreatorModifyAfterDispatch();
    }

    /** Whether the event's creator may change it between dispatches. Custom events override this so
     * a script can reuse one instance; built-in events deny changes after the raiser has returned.
     * @return {@code true} when the creator thread keeps write access between dispatches
     */
    @ApiStatus.Internal
    @DocletIgnore
    protected boolean canCreatorModifyAfterDispatch() {
        return false;
    }

    /** Records the raising thread and starts a dispatch. Internal dispatch hook. */
    @ApiStatus.Internal
    @DocletIgnore
    public final void beginDispatch(Thread thread) {
        hasDispatched = true;
        ArrayDeque<Thread> stack = dispatches.get();
        if (stack == null) {
            stack = new ArrayDeque<>();
            dispatches.set(stack);
        }
        stack.push(thread);
    }

    /** Marks the current thread's dispatch as finished. Internal dispatch hook. */
    @ApiStatus.Internal
    @DocletIgnore
    public final void endDispatch() {
        ArrayDeque<Thread> stack = dispatches.get();
        if (stack == null) {
            return;
        }
        stack.pop();
        if (stack.isEmpty()) {
            dispatches.remove();
        }
    }

    /**
     * Marks the current callback as joined or non-joined for the calling thread. Internal listener hook.
     * @param container the delivery's event lock, or {@code null} when it has no container
     * @param joined whether dispatch waits for this delivery before the raiser continues
     */
    @ApiStatus.Internal
    @DocletIgnore
    public final void beginDelivery(@Nullable EventContainer<?> container, boolean joined) {
        ArrayDeque<Delivery> stack = delivery.get();
        if (stack == null) {
            stack = new ArrayDeque<>();
            delivery.set(stack);
        }
        stack.push(new Delivery(container, joined));
    }

    /** Clears the current thread's innermost delivery marker. Internal listener hook. */
    @ApiStatus.Internal
    @DocletIgnore
    public final void endDelivery() {
        ArrayDeque<Delivery> stack = delivery.get();
        if (stack == null) {
            return;
        }
        stack.pop();
        if (stack.isEmpty()) {
            delivery.remove();
        }
    }

    /** Clears the topmost delivery marker for a specific event lock. Internal listener hook.
     * @param container the delivery container to remove, or {@code null} to remove a marker without a container
     */
    @ApiStatus.Internal
    @DocletIgnore
    public final void endDelivery(@Nullable EventContainer<?> container) {
        ArrayDeque<Delivery> stack = delivery.get();
        if (stack == null) {
            return;
        }
        Iterator<Delivery> iterator = stack.descendingIterator();
        while (iterator.hasNext()) {
            if (iterator.next().container() == container) {
                iterator.remove();
                break;
            }
        }
        if (stack.isEmpty()) {
            delivery.remove();
        }
    }

    @Nullable
    private Delivery currentDelivery() {
        ArrayDeque<Delivery> stack = delivery.get();
        return stack == null ? null : stack.peek();
    }

    /**
     * Marks this event cancelled. This is only valid from a joined listener while its event lock is
     * still held; see {@link #canModify()}. Cancel before releasing the lock so the raising code
     * observes the flag when dispatch returns. Throws {@link UnsupportedOperationException} if this
     * event is not cancellable, and {@link IllegalStateException} if the current delivery cannot
     * change the event.
     */
    public final void cancel() {
        if (!cancellable()) {
            throw new UnsupportedOperationException("Event is not cancellable");
        }
        assertCanModify("cancel");
        cancelled = true;
    }

    /**
     * Fails unless the current delivery may change this event.
     * @param action action name used in the diagnostic message
     */
    @DocletIgnore
    protected final void assertCanModify(String action) {
        if (canModify()) {
            return;
        }
        Delivery current = currentDelivery();
        if (current != null && !current.joined()) {
            throw new IllegalStateException(action + "() requires joined handling. Enable Joined on the trigger or pass true to JsMacros.on(event, true, callback); a non-joined listener receives the event after dispatch and cannot change it.");
        }
        if (current != null) {
            throw new IllegalStateException(action + "() cannot run after the event lock is released. Cancel or change the event before calling context.releaseLock().");
        }
        throw new IllegalStateException(action + "() is only available from a joined listener while the event is being dispatched.");
    }

    /**
     * Whether {@link #cancel()} has been called on this event.
     *
     * @return {@code true} once something has called {@link #cancel()} on this instance.
     */
    public final boolean isCanceled() {
        return cancelled;
    }

    /**
     * The event name from {@link Event#value()}, or the runtime class's simple name if it has no
     * annotation.
     *
     * @return the name from the annotation, for example {@code SendMessage}, or the runtime
     * class's simple name when no annotation is present.
     */
    public String getEventName() {
        Event annotation = this.getClass().getAnnotation(Event.class);
        return annotation == null ? getClass().getSimpleName() : annotation.value();
    }

    /**
     * Dispatches this event through its core's profile. Listener work may outlive this call unless
     * dispatch joins it; custom events dispatch under their custom name.
     */
    public void trigger() {
        runner.profile.triggerEvent(this);
    }

}
