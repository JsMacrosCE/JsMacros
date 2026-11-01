package com.jsmacrosce.jsmacros.client.api.event.impl;

import com.jsmacrosce.doclet.DocletCategory;
import com.jsmacrosce.jsmacros.client.JsMacrosClient;
import com.jsmacrosce.jsmacros.core.event.BaseEvent;
import com.jsmacrosce.jsmacros.core.event.Event;

/**
 * Fires during JVM shutdown from a shutdown hook registered by JsMacros. It has no payload.
 * Listener work may not finish before the process exits. This event does not fire when leaving a
 * world while the game continues running; use
 * {@link com.jsmacrosce.jsmacros.client.api.event.impl.world.EventDisconnect} for that transition.
 * @author Etheradon
 * @since 1.8.4
 */
@DocletCategory("System/Lifecycle")
@Event(value = "QuitGame")
public class EventQuitGame extends BaseEvent {

    /** Initializes the event with the JsMacros client core as its runner; construction does not dispatch it. */
    public EventQuitGame() {
        super(JsMacrosClient.clientCore);
    }

    /** Returns a diagnostic string containing the event name and an empty-brace payload marker, not JSON.
     * @return the event name followed by {@code :{}}
     */
    @Override
    public String toString() {
        return String.format("%s:{}", this.getEventName());
    }

}
