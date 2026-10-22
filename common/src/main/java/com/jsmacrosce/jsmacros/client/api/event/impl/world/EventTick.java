package com.jsmacrosce.jsmacros.client.api.event.impl.world;

import com.jsmacrosce.doclet.DocletCategory;
import com.jsmacrosce.jsmacros.client.JsMacrosClient;
import com.jsmacrosce.jsmacros.core.event.BaseEvent;
import com.jsmacrosce.jsmacros.core.event.Event;

/**
 * Fires after the key-macro check, tick synchronizer, and server-list pinger on each client tick.
 * It fires without a player or loaded world and cannot be cancelled.
 * <pre>
 * const listener = JsMacros.on("Tick", JavaWrapper.methodToJava(function () {
 *   // runs on each client tick
 * }));
 * // later
 * JsMacros.off(listener);
 * </pre>
 * @author Wagyourtail
 * @since 1.2.7
 */
@DocletCategory("World")
@Event(value = "Tick", oldName = "TICK")
public class EventTick extends BaseEvent {
    /** Initializes this event with the client core; it does not fire the event. */
    public EventTick() {
        super(JsMacrosClient.clientCore);
    }

    /** Returns the event name followed by {@code :{}}.
     * @return the event name followed by {@code :{}}
     */
    @Override
    public String toString() {
        return String.format("%s:{}", this.getEventName());
    }

}
