package com.jsmacrosce.jsmacros.client.api.event.impl.player;

import com.jsmacrosce.doclet.DocletCategory;

import com.jsmacrosce.jsmacros.client.JsMacrosClient;
import com.jsmacrosce.jsmacros.core.event.BaseEvent;
import com.jsmacrosce.jsmacros.core.event.Event;

/**
 * Fires when the local player's polled fall-flying state differs from the previous sample.
 * The state is checked once per client tick while a player is present. The previous sample is
 * retained while no player is present, including across disconnects, so a later event may compare
 * different player instances. The event is not cancellable.
 * <pre>
 * JsMacros.on("FallFlying", JavaWrapper.methodToJava(function (event) {
 *   if (event.state) {
 *     Chat.log("Elytra flight started");
 *   } else {
 *     Chat.log("Elytra flight ended");
 *   }
 * }))
 * </pre>
 */
@DocletCategory("Player/Stats")
@Event("FallFlying")
public class EventFallFlying extends BaseEvent {
    /**
     * The newly sampled fall-flying state: {@code true} if fall-flying, {@code false} otherwise.
     */
    public final boolean state;

    /**
     * Initializes the event with the supplied fall-flying state.
     * @param state the supplied fall-flying state
     */
    public EventFallFlying(boolean state) {
        super(JsMacrosClient.clientCore);
        this.state = state;
    }

    /**
     * Returns an event-name and stored-state diagnostic in the format {@code %s:{"state": %s}},
     * with the payload rendered as {@code true} or {@code false}; this is not JSON.
     * @return the event name and stored state in the diagnostic format
     */
    @Override
    public String toString() {
        return String.format("%s:{\"state\": %s}", this.getEventName(), state);
    }

}
