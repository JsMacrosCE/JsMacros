package com.jsmacrosce.jsmacros.core.event;

import com.jsmacrosce.jsmacros.core.language.EventContainer;

/**
 * A listener invoked when a registered event is dispatched. Script subscriptions and profile
 * macro triggers use this interface. Dispatch waits on the returned container only when joining is
 * requested (or the event is cancellable) and the event is joinable; otherwise listener work may
 * continue after dispatch returns. A joined dispatch waits until the container is released, subject
 * to the configured lock watchdog. A {@code null} container is never awaited.
 */
public interface IEventListener {
    /**
     * Listener whose {@link #trigger(BaseEvent)} always returns {@code null}.
     */
    IEventListener NULL = event -> null;

    /**
     * Whether this listener requests waiting on a joinable, non-cancellable event. Cancellable
     * events request joined dispatch regardless of this value.
     *
     * @return {@code true} to request waiting when the event permits joining
     */
    default boolean joined() {
        return false;
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

}
