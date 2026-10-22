package com.jsmacrosce.jsmacros.core.event;

import com.jsmacrosce.doclet.DocletCategory;
import com.jsmacrosce.jsmacros.core.Core;

/**
 * Base class for event payloads dispatched to registered listeners. By default, the runtime
 * class's {@link Event} annotation supplies its name and cancellable/joinable flags; without an
 * annotation, the simple class name is used and both flags are false. Subclasses may override
 * these methods, as {@link com.jsmacrosce.jsmacros.core.event.impl.EventCustom} does.
 * <p>
 * Dispatch waits for cancellable events, or for listeners requesting a join when joining is
 * permitted. Otherwise listener work may continue after {@link #trigger()} returns, so changes to the
 * event may be too late for the code that raised it. See {@link IEventListener#joined()} for the
 * listener-side contract.
 * <pre>
 * const listener = JsMacros.on("SendMessage", JavaWrapper.methodToJava(event => {
 *   if (event.message === "/hello") {
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
     * Marks this event cancelled. Dispatch waits for cancellable events; cancel before releasing
     * the event lock for the raising code to observe the flag when dispatch returns. Throws
     * {@link UnsupportedOperationException} if this event is not cancellable.
     */
    public final void cancel() {
        if (cancellable()) {
            cancelled = true;
        } else {
            throw new UnsupportedOperationException("Event is not cancellable");
        }
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
