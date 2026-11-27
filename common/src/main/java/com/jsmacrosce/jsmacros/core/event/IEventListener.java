package com.jsmacrosce.jsmacros.core.event;

import com.jsmacrosce.jsmacros.core.language.EventContainer;

/**
 * A listener invoked when a registered event is dispatched. Script subscriptions and profile
 * macro triggers use this interface. Dispatch waits on the returned container only when the
 * listener requests joined handling and the event permits joining; otherwise it is a read-only
 * delivery that dispatch does not wait for. A joined dispatch waits until the container is
 * released, subject to the configured lock watchdog. A {@code null} container is never awaited.
 * <p>
 * Only a joined delivery may cancel or change the event while its lock is held. A listener that
 * does not request joining receives a read-only delivery and must treat the event as read-only;
 * see {@link BaseEvent#canModify()}.
 */
public interface IEventListener {
    /**
     * Listener whose {@link #trigger(BaseEvent)} always returns {@code null}.
     */
    IEventListener NULL = event -> null;

    /**
     * Whether this listener requests joined handling. A joined delivery waits for the listener's
     * event lock before the raiser continues, which lets the listener cancel or change the event.
     * The request is honoured only when the event permits joining.
     *
     * @return {@code true} to request joined handling when the event permits it
     * @since 2.2.0
     */
    default boolean joinRequested() {
        return joined();
    }

    /**
     * Handles one event occurrence. When dispatch joins this listener, it waits on the returned
     * container; {@code null} means dispatch does not wait.
     *
     * @param event the live event instance being dispatched.
     * @return the container dispatch should wait on, or {@code null} if none
     */
    EventContainer<?> trigger(BaseEvent event);

    /**
     * Turns this listener off; the default implementation does nothing.
     *
     * @since 1.8.4
     */
    default void off() {}

    /**
     * Whether this listener requests waiting on a joinable event.
     *
     * @return {@code true} to request waiting when the event permits joining
     * @deprecated Removal in 3.0.0: Use {@link #joinRequested()} instead.
     */
    @Deprecated
    default boolean joined() {
        return false;
    }

}
